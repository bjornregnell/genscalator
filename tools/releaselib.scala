// (no version include: mainless helper — inherits it from its includer; see project.scala)
//
// ⚠ NO `//> using dep` here, following the toolbox convention: every mainless helper is dep-free and the
// TOOL that includes it declares what the combined build unit needs. NO NEW external dependency enters the
// project through this file — requests, ujson and os-lib are all already used elsewhere, and `forge.scala`
// already declared all three. The one consequence is that `update.scala`, which had only os-lib, must now
// also declare requests + ujson: this file compiles as part of ITS build unit too and references those
// types. An includer that forgets one fails at COMPILE time, loudly, which is why no runtime guard is needed.

// releaselib — the shared release-download client. Fetching a release asset and checking its sha256 is
// needed by TWO tools now: `tt forge release-download` (a human asking for an artifact) and
// `tt update --native` (the toolbox replacing its own binary). This file is where that capability lives so
// that neither tool depends on the other.
//
// ⚠ WHY A THIRD FILE rather than the obvious two alternatives (D7a, decided by BR 2026-07-27):
//   - Promoting a public entry point on `Forge` and calling it from `update` would make one TOOL depend on
//     another's code. The toolbox's dependency graph is deliberately FLAT — tools depend on shared libs,
//     never on each other — and that property is worth more than the file it would save.
//   - Shelling out to `tt forge release-download` would keep the tools decoupled at the cost of a subprocess
//     reach of exactly the kind this project argues against.
//   - Copying the request code into `update` is the SM247 sibling-miss trap: a fix applied to one copy of a
//     duplicated predicate looks complete from inside that file. Rejected outright.
// It cannot live in `lib.scala`, which is deliberately JDK-only so that pure text tools compile fast, while
// download-and-verify needs requests and os-lib. Hence a separate file carrying those deps.
//
// ⚠ THIS FILE CONCENTRATES THE TOOLBOX'S MOST SECURITY-SENSITIVE MACHINERY: credential acquisition, the
// trusted-host guard that stops a token being redirected, and the dialect logic that decides which token
// goes to which host. The properties below are load-bearing, not stylistic. Read the CREDENTIAL HELPERS
// note before changing any of them.
package agenttools

import scala.util.Try

object ReleaseLib:

  /** Which forge dialect a call speaks. Gitea (default, `--url`) sends `Authorization: token` to a
    * trustedHosts host; GitHub (`--gh`) talks to the FIXED api.github.com root; GitLab (`--gl`) is listed
    * for completeness because the release-WRITE verbs support it — the download path deliberately refuses
    * it, since GitLab releases carry LINKS to external artifacts rather than uploaded assets. */
  enum Dialect { case Gitea, GitHub, GitLab }

  // --- pure helpers (no I/O, no credentials) -------------------------------------------------------

  /** Host of a URL, or "" when it does not parse. Used by every trusted-host check, so it must never
    * throw: a malformed URL yields "", which no trusted set contains, so the guard REFUSES. Failing
    * closed is the whole point. */
  def hostOf(url: String): String =
    Try(Option(java.net.URI(url).getHost)).toOption.flatten.getOrElse("")

  /** The Gitea/Forgejo API root for a base URL. */
  def apiBase(url: String): String = url.stripSuffix("/") + "/api/v1"

  /** Split "owner/repo", or None. PURE — the caller supplies the error, so the same predicate serves a
    * tool that dies and a tool that reports. Same reason `Lib.isAbsolutePath` is a string predicate. */
  def ownerRepo(s: String): Option[(String, String)] =
    s.split("/") match
      case Array(o, r) if o.nonEmpty && r.nonEmpty => Some((o, r))
      case _                                       => None

  def assetsOf(rel: ujson.Value): List[ujson.Value] =
    rel.obj.get("assets").toList.flatMap(a => Try(a.arr.toList).getOrElse(Nil))

  /** ONE predicate for "a JSON string field I can live without", because the naive shape
    * `obj.get(k).map(_.str).getOrElse(d)` is WRONG against a forge and reads as right: GitHub sends
    * key-present-with-NULL (every draft's `published_at`, any unnamed release's `name`), so `.get`
    * returns Some(Null), `.str` throws, and `getOrElse` never fires because the key IS there.
    * Found by the release rehearsal 2026-07-27: `tt forge releases` crashed on the first draft it ever
    * saw. Absent and present-but-null must collapse to the same answer, in one place. */
  def strOr(v: Option[ujson.Value], default: String): String =
    v.flatMap(x => Try(x.str).toOption).getOrElse(default)

  def strOrEmpty(v: Option[ujson.Value]): String = strOr(v, "")

  /** SHA-256 of a file as lowercase hex. JDK only, no dependency. */
  def sha256Hex(p: os.Path): String =
    java.security.MessageDigest.getInstance("SHA-256")
      .digest(os.read.bytes(p)).map(b => String.format("%02x", Byte.box(b))).mkString

  // --- the client ----------------------------------------------------------------------------------

  /** A release client that reports failures under the CALLING TOOL's name.
    *
    * Parameterised by `tool` rather than hard-coding "forge" because the same failure now reaches a user
    * through two different verbs, and a message reading `forge: ...` during `tt update --native` would
    * send them to the wrong tool's help. That is the ONLY reason this is a class and not an object.
    */
  final class Client(tool: String):

    def die(msg: String): Nothing = { System.err.println(s"$tool: $msg"); sys.exit(2) }

    def splitRepo(s: String): (String, String) =
      ownerRepo(s).getOrElse(die(s"expected <owner>/<repo>, got '$s'"))

    // Token comes ONLY from a FIXED set of human-set env-var names — never a flag, and never an
    // agent-nameable var (an agent-chosen var name + an agent-chosen --url would let it POST an arbitrary
    // secret to an arbitrary host = exfiltration). Fixed names keep the authorization a human boundary.
    val TokenEnvNames = List("GENSCALATOR_CODEBERG_TOKEN", "CODEBERG_TOKEN", "FORGE_TOKEN")

    def envToken: Option[String] =
      TokenEnvNames.iterator.flatMap(sys.env.get).map(_.trim).find(_.nonEmpty)

    // ==============================================================================================
    // CREDENTIAL HELPERS — one deliberate trust-boundary change, covering every fallback below.
    //
    // ⚠ WHAT CHANGED (BR-authorized 2026-07-25, after the agent flagged the cost). Tokens used to come
    // ONLY from fixed human-set env names, which meant the human's SHELL decided whether a running agent
    // had forge credentials at all. These helpers let the tool obtain one itself, so an agent that can run
    // `tt forge` can now act as the user without the user having exported anything. That is a real
    // widening, written here rather than buried, because the rule it relaxes was previously absolute.
    //
    // WHY IT WAS ACCEPTED. It lets the human keep NO long-lived credential in the environment at all —
    // BR's `.bashrc` now exports neither token. On 2026-07-25 a bare `printenv` put the then-ambient
    // tokens into a durable transcript and forced a rotation of both. Ambient exposure is continuous and
    // passive; a helper call is momentary and reachable only through this one audited path. Ratified as
    // project POLICY 2026-07-27 (momentary-first) — see SECURITY-MODEL.md "Credentials and tokens".
    //
    // WHAT STILL HOLDS, so the widening stays bounded:
    //   - env ALWAYS wins, so existing setups and CI are unaffected;
    //   - the SOURCE is never agent-nameable — fixed env names, a fixed keyring key or one fixed env var
    //     naming it, never a flag. An agent that could choose both the secret's source and the
    //     destination could exfiltrate, which is the original reason for the fixed-names rule;
    //   - each token still only ever travels to its fixed/trusted host, never one derived from --url;
    //   - argv, never a shell;
    //   - NEVER silent, and failure is soft.
    //
    // ⚠ THE WIDENING NOW COVERS `tt update --native` TOO, which is new and worth stating plainly: the
    // self-update path can obtain a credential the same way. It does not need one for a PUBLIC release,
    // and the download proceeds anonymously when no token is obtainable — but the capability is reachable
    // from a second verb as of this refactor, so the audit line matters more, not less.
    //
    // OPEN for the SM073 review: whether these fallbacks should require an explicit human opt-in rather
    // than being the default. Argument for opt-in: the convenience is small and the boundary is not.
    // Argument against: an opt-in nobody sets leaves the ambient token in place, which is the exposure
    // this removes.
    // ==============================================================================================

    /** Run a fixed argv and take its stdout as a token. Shared by every credential-helper fallback so they
      * cannot drift on the properties that make them acceptable: argv (never a shell, so nothing is
      * injectable), soft failure (a missing or locked helper yields None and read verbs keep working), and
      * NEVER SILENT — a line on stderr, so a human reading a transcript can see that the agent obtained a
      * credential rather than being handed one by the shell. */
    def helperToken(label: String, cmd: String*): Option[String] =
      try
        val p   = ProcessBuilder(cmd*).redirectErrorStream(false).start()
        val out = String(p.getInputStream.readAllBytes, "UTF-8").trim
        if p.waitFor() == 0 && out.nonEmpty then
          Console.err.println(s"$tool: no token in env; obtained one from $label")
          Some(out)
        else None
      catch case _: Throwable => None

    /** WHERE the keyring entry lives — a service/account pair. This is NOT a secret, so it is safe to
      * export, which is the whole point: the machine says where to look and nothing stores the token but
      * the OS keyring.
      *
      * ⛔ It must NOT be agent-nameable, and that is a security property rather than a style choice. An
      * agent that could choose BOTH the keyring key and the destination could read any stored secret and
      * ship it to a forge. So the key comes from one fixed env name or the built-in default, exactly as
      * the token env names are a fixed list — never from a flag. */
    def keyringSpec: (String, String) =
      sys.env.get("TT_FORGE_KEYRING").map(_.trim).filter(_.nonEmpty).map(_.split("/", 2)) match
        case Some(Array(service, account)) if service.nonEmpty && account.nonEmpty => (service, account)
        case _                                                                     => ("codeberg", "genscalator-token")

    /** Gitea/Codeberg token from the OS keyring when no env var holds one. Same trust-boundary trade as
      * ghCliToken below, and accepted for the same reason: it lets the human keep NO long-lived credential
      * in the environment of every process, which is the exposure that leaked two tokens on 2026-07-25. */
    lazy val keyringToken: Option[String] =
      val (service, account) = keyringSpec
      helperToken(s"keyring get $service $account", "keyring", "get", service, account)

    def token: Option[String] = envToken.orElse(keyringToken)

    // The token may only be sent to a TRUSTED host — so the agent cannot redirect it to an attacker host
    // via --url. Default: codeberg.org. The HUMAN (not a flag) extends the set via env TT_FORGE_HOSTS.
    def trustedHosts: Set[String] =
      val extra = sys.env.getOrElse("TT_FORGE_HOSTS", "").split(",").iterator.map(_.trim).filter(_.nonEmpty).toSet
      Set("codeberg.org") ++ extra

    /** Refuse to proceed unless `base`'s host is trusted. The ONE definition of this check, because it
      * previously existed in three places (`whoami`, `findHeaders`, `writeHeaders`) and a guard that is
      * written three times is a guard that can be relaxed in two. */
    def requireTrustedHost(base: String, extendHint: Boolean = false): String =
      val host = hostOf(base)
      if !trustedHosts.contains(host) then
        val hint = if extendHint then " (extend via env TT_FORGE_HOSTS)" else ""
        die(s"refusing to send the token to untrusted host '$host'. " +
          s"Trusted: ${trustedHosts.toVector.sorted.mkString(", ")}$hint.")
      host

    // GitHub dialect. `--gh` (or a github.com --url) switches the path shapes to the GitHub REST API,
    // rooted at the FIXED GitHubApi constant below — never derived from --url — so the GitHub token can
    // only ever travel to that one host (same no-redirect rule as trustedHosts for the Gitea token).
    // READ verbs work without any token (60/h anonymous rate limit); `protection` requires one.
    val GitHubApi       = "https://api.github.com"
    val GhTokenEnvNames = List("GENSCALATOR_GITHUB_TOKEN", "GITHUB_TOKEN", "GH_TOKEN")

    def envGhToken: Option[String] =
      GhTokenEnvNames.iterator.flatMap(sys.env.get).map(_.trim).find(_.nonEmpty)

    /** GitHub token from the `gh` CLI when no env var holds one. `gh` is the right helper here rather than
      * the keyring: it already owns the credential, refreshes it, and `gh auth token` needs no key name, so
      * there is nothing for an agent to point elsewhere. See the CREDENTIAL HELPERS note above. */
    lazy val ghCliToken: Option[String] = helperToken("`gh auth token`", "gh", "auth", "token")

    def ghToken: Option[String] = envGhToken.orElse(ghCliToken)

    def isGitHub(base: String): Boolean =
      Set("github.com", "www.github.com", "api.github.com").contains(hostOf(base))

    /** Pair ONLY with GitHubApi-rooted URLs — never a URL derived from --url. */
    def ghHeaders: Map[String, String] =
      Map("Accept" -> "application/vnd.github+json") ++ ghToken.map(t => "Authorization" -> s"Bearer $t")

    // --- HTTP --------------------------------------------------------------------------------------

    /** GET returning None on ANY failure instead of exiting.
      *
      * ⚠ Needed because a `die`-based getter reports a non-200 through `sys.exit` — which NO `Try` can
      * catch. A fallback written as `Try(get(a)).orElse(Try(get(b)))` would therefore terminate the
      * process on a's 404 and never reach b, while LOOKING like a fallback. */
    def getJsonOpt(url: String, headers: Map[String, String] = Map.empty): Option[ujson.Value] =
      Try(requests.get(url, headers = headers, check = false, readTimeout = 30000, connectTimeout = 10000))
        .toOption.filter(_.statusCode == 200).flatMap(r => Try(ujson.read(r.text())).toOption)

    /** The API root for a repo in the given dialect. GitHub is the FIXED constant; Gitea derives from base. */
    def repoRoot(owner: String, repo: String, dialect: Dialect, base: String): String =
      if dialect == Dialect.GitHub then s"$GitHubApi/repos/$owner/$repo"
      else s"${apiBase(base)}/repos/$owner/$repo"

    /** Read headers for a release lookup. A Gitea/Forgejo DRAFT is invisible without auth, so the token is
      * attached when one exists — and the trusted-host guard applies exactly as it does on the write verbs,
      * because this is the same token going over the same wire. */
    def findHeaders(dialect: Dialect, base: String): Map[String, String] =
      if dialect == Dialect.GitHub then ghHeaders
      else
        token match
          case None    => Map.empty
          case Some(t) =>
            requireTrustedHost(base)
            Map("Authorization" -> s"token $t")

    /** Headers for a call that MUST be authenticated (writes, and asset downloads that may hit a draft). */
    def writeHeaders(dialect: Dialect, base: String, verb: String): Map[String, String] =
      if dialect == Dialect.GitHub then
        val tok = ghToken.getOrElse(die(
          s"$verb --gh needs a token — the HUMAN sets one of env ${GhTokenEnvNames.mkString(", ")} (never a flag)."))
        Map("Accept" -> "application/vnd.github+json", "Authorization" -> s"Bearer $tok")
      else
        requireTrustedHost(base)
        val tok = token.getOrElse(die(
          s"$verb needs a token — the HUMAN sets one of env ${TokenEnvNames.mkString(", ")} (never a flag)."))
        Map("Authorization" -> s"token $tok")

    /** Find a release by tag, INCLUDING drafts.
      *
      * Deliberately LISTS and filters instead of going straight to the by-tag endpoint. That endpoint
      * cannot see a draft at all — a draft has no tag yet — so it is blind to exactly the releases these
      * verbs most need to reach: the half-finished one you want to inspect or throw away. The by-tag
      * endpoint is then a FALLBACK, because the two are complementary rather than ranked: listing is the
      * only thing that can see a DRAFT, but it is capped, so a published release older than the newest 100
      * is reachable only by tag. Using either alone loses real cases. */
    def findRelease(owner: String, repo: String, tag: String, dialect: Dialect, base: String): ujson.Value =
      val root    = repoRoot(owner, repo, dialect, base)
      val hdrs    = findHeaders(dialect, base)
      val listUrl = if dialect == Dialect.GitHub then s"$root/releases?per_page=100" else s"$root/releases?limit=100"
      val listed  = getJsonOpt(listUrl, hdrs).toList
        .flatMap(v => Try(v.arr.toList).getOrElse(Nil))
        .find(r => strOrEmpty(r.obj.get("tag_name")) == tag)
      listed.orElse(getJsonOpt(s"$root/releases/tags/$tag", hdrs)).getOrElse(die(
        s"no release for tag '$tag' — absent from the newest 100 (the only view that shows DRAFTS)\n" +
          "  and from the by-tag endpoint (which reaches further back but never shows a draft)."))

    /** The LATEST published release. Both dialects expose the same path, and both EXCLUDE drafts and
      * prereleases from it — which is the wanted behaviour for `tt update --native`: a self-updater must
      * never pull a draft, and the caller has no tag to name.
      *
      * ⚠ It also returns the release's `tag_name`, because the caller needs to TELL the user which version
      * it is about to install. A downloader that cannot name what it downloaded is not auditable. */
    def latestRelease(owner: String, repo: String, dialect: Dialect, base: String): (ujson.Value, String) =
      val root = repoRoot(owner, repo, dialect, base)
      val rel  = getJsonOpt(s"$root/releases/latest", findHeaders(dialect, base)).getOrElse(die(
        s"no published release found for $owner/$repo — the project may have only drafts/prereleases so far."))
      (rel, strOrEmpty(rel.obj.get("tag_name")))

    /** Download the assets of `rel` whose names match `pattern` (a `*`-only glob; None = all) into
      * `outDir`, returning what was written.
      *
      * ⚠ Uses the API asset url with `Accept: application/octet-stream`, NOT `browser_download_url`: the
      * browser URL 404s for a DRAFT's assets because no public release page exists yet, and a draft is the
      * main thing `release-download` is for. */
    def downloadAssets(rel: ujson.Value, pattern: Option[String], outDir: os.Path,
        dialect: Dialect, base: String, verb: String): List[os.Path] =
      val all = assetsOf(rel)
      if all.isEmpty then die("release carries no assets")
      val wanted = pattern match
        case Some(p) => all.filter(a => Lib.globMatches(p, strOrEmpty(a.obj.get("name"))))
        case None    => all
      if wanted.isEmpty then
        die(s"no asset matches --pattern '${pattern.getOrElse("")}' " +
          s"(present: ${all.map(a => strOr(a.obj.get("name"), "?")).mkString(", ")})")
      os.makeDir.all(outDir)
      val hdrs = (if dialect == Dialect.GitHub then ghHeaders else writeHeaders(dialect, base, verb)) ++
        Map("Accept" -> "application/octet-stream")
      wanted.map { a =>
        val name = strOr(a.obj.get("name"), "asset")
        val aUrl = strOrEmpty(a.obj.get("url"))
        if aUrl.isEmpty then die(s"asset '$name' has no api url")
        val r = Try(requests.get(aUrl, headers = hdrs, check = false,
          readTimeout = 300000, connectTimeout = 10000)).getOrElse(die(s"request failed for '$name'"))
        if r.statusCode != 200 then die(s"GET asset '$name' -> ${r.statusCode} ${r.statusMessage}")
        val target = outDir / name
        os.write.over(target, r.bytes)
        println(s"downloaded $name (${r.bytes.length} B) -> $target")
        target
      }

    /** Check each payload against a downloaded sibling `<name>.sha256` — the same digest CI writes and the
      * installer checks. A payload with no sibling prints UNVERIFIED, never "ok": "nothing to check" and
      * "checked and correct" must not render identically, which is the SM241 Class-B rule.
      *
      * Returns the number of payloads that were actually verified against a sibling, so a CALLER that must
      * not proceed unverified (the self-updater) can insist on it rather than re-deriving the count from
      * printed text. `release-download` ignores the return and merely reports. */
    def verifyChecksums(files: List[os.Path]): Int =
      val shaOf    = files.filter(_.last.endsWith(".sha256")).map(p => p.last.stripSuffix(".sha256") -> p).toMap
      val payloads = files.filterNot(_.last.endsWith(".sha256"))
      if payloads.isEmpty then die("--verify: nothing but .sha256 files were downloaded, so there is nothing to verify")
      val outcomes = payloads.map { p =>
        val expected = shaOf.get(p.last).map(f => os.read(f).trim.split("\\s+").head.toLowerCase)
        (p, expected, sha256Hex(p))
      }
      outcomes.foreach {
        case (p, Some(exp), act) if exp == act => println(s"ok         ${p.last}  sha256 $act")
        case (p, Some(exp), act)               => println(s"MISMATCH   ${p.last}\n  expected $exp\n  actual   $act")
        case (p, None, act)                    => println(s"UNVERIFIED ${p.last}  sha256 $act  (no sibling .sha256 downloaded)")
      }
      val bad = outcomes.count((_, exp, act) => exp.exists(_ != act))
      val ok  = outcomes.count((_, exp, act) => exp.contains(act))
      if bad > 0 then die(s"$bad of ${outcomes.size} payload(s) FAILED the checksum — the download is not intact")
      println(s"verified $ok/${outcomes.size} payload(s) against a downloaded .sha256")
      ok
