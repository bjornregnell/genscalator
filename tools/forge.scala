//> using file project.scala
//> using file lib.scala
//> using file releaselib.scala
//> using jvm 21
//> using dep com.lihaoyi::requests:0.9.3
//> using dep com.lihaoyi::ujson:4.4.3
//> using dep com.lihaoyi::os-lib:0.11.8

// forge — typed client for a Forgejo/Gitea forge (default: Codeberg). Replaces hand-curling the REST API
// (a dual-use `curl` carrying a token on the command line) with a narrow, effect-declared tool.
//   READ verbs (releases, tags) need NO auth (public repos) → safe to allowlist (`Bash(tt forge releases *)`).
//   The one EFFECTFUL verb (release-create) reads its token from a fixed set of human-set env vars
//   (GENSCALATOR_CODEBERG_TOKEN, then CODEBERG_TOKEN, then FORGE_TOKEN) and, failing those, from an OS
//   credential helper (`keyring get` for Gitea, `gh auth token` for GitHub) — NEVER a flag, and never a
//   source the AGENT can name, so it still cannot point the tool at an arbitrary secret. The helper
//   fallback is a deliberate 2026-07-25 widening so the human need keep NO token in the environment at
//   all; see the CREDENTIAL HELPERS note below for the full trade. It prints an [audit] line before acting,
//   and is deliberately NOT blanket-allowlistable (creating a release should stay a visible, confirmed op).
//   tt forge whoami   [--url BASE]                          # verify auth: print the token's login (never the token)
//   tt forge releases <owner>/<repo> [--gh | --url BASE] [--limit N]   # lists asset names too
//   tt forge tags     <owner>/<repo> [--gh | --url BASE] [--limit N]
//   tt forge release-create <owner>/<repo> <tag> [--name S] [--body S | --body-file F]
//                           [--prerelease] [--draft] [--target COMMITISH] [--url BASE]
//   tt forge release-edit   <owner>/<repo> <tag> [--name S] [--body S | --body-file F] [--prerelease] [--draft] [--gh | --url BASE]
//                           # PATCH an existing release (draft-visible lookup); sends ONLY the provided fields
//   tt forge release-download <owner>/<repo> <tag> [--gh | --url BASE] [--pattern GLOB] [--dir D] [--verify]
//   tt forge release-upload <owner>/<repo> <tag> <file> [--name N] [--gh | --url BASE]
//   tt forge release-delete <owner>/<repo> <tag> [--gh | --url BASE] [--yes] [--allow-published]
//   READ verbs for issues/PRs/branch protection (both dialects; --gh = GitHub, see GitHubApi below):
//   tt forge issues <owner>/<repo> [--gh | --url BASE] [--state open|closed|all] [--limit N]
//   tt forge prs    <owner>/<repo> [--gh | --url BASE] [--state open|closed|all] [--limit N]
//   tt forge contributors <owner>/<repo> [--gh | --gl | --url BASE] [--limit N]   # GitHub login/contribs/type
//                                                                                 # or GitLab name/email/commits
//   tt forge issue  <owner>/<repo> <n> [--gh | --url BASE]           # body + comments
//   tt forge pr     <owner>/<repo> <n> [--gh | --url BASE]           # merge state + body
//   tt forge protection <owner>/<repo> <branch> [--gh | --url BASE]  # protection rule (needs a token)
//   BASE defaults to https://codeberg.org
import scala.util.Try
import agenttools.{Lib, ReleaseLib}

// Helpers (die/token/hostOf/getJson/splitRepo/… and the opts types) scoped in this object so their generic
// names don't collide with other tools when the toolbox compiles together. Only the @main entry is top-level.
object Forge {
  private val DefaultBase = "https://codeberg.org"

  private def die(msg: String): Nothing = { System.err.println(s"forge: $msg"); sys.exit(2) }

  private def forgeUsage(): Nothing = die(
    "usage:\n" +
      "  forge whoami   [--url BASE]                              (verify auth: prints the token's login)\n" +
      "  forge releases <owner>/<repo> [--gh | --url BASE] [--limit N]\n" +
      "  forge tags     <owner>/<repo> [--gh | --url BASE] [--limit N]\n" +
      "  forge issues <owner>/<repo> [--gh | --url BASE] [--state open|closed|all] [--limit N]\n" +
      "  forge prs    <owner>/<repo> [--gh | --url BASE] [--state open|closed|all] [--limit N]\n" +
      "  forge contributors <owner>/<repo> [--gh | --gl | --url BASE] [--limit N]   (--gh/--gl only)\n" +
      "  forge issue  <owner>/<repo> <n> [--gh | --url BASE]             (body + comments)\n" +
      "  forge pr     <owner>/<repo> <n> [--gh | --url BASE]             (merge state + body)\n" +
      "  forge protection <owner>/<repo> <branch> [--gh | --url BASE]    (needs a token)\n" +
      "  forge release-create <owner>/<repo> <tag> [--gh | --gl | --url BASE] [--name S] [--body S | --body-file F] [--prerelease] [--draft] [--target C]\n" +
      "  forge release-edit   <owner>/<repo> <tag> [--name S] [--body S | --body-file F] [--prerelease] [--draft] [--gh | --url BASE]\n" +
      "  forge release-download <owner>/<repo> <tag> [--gh | --url BASE] [--pattern GLOB] [--dir D] [--verify]   (finds DRAFTS too; --verify checks the .sha256)\n" +
      "  forge release-upload <owner>/<repo> <tag> <file> [--name N] [--gh | --url BASE]   (attach ONE file; refuses duplicate names)\n" +
      "  forge release-delete <owner>/<repo> <tag> [--gh | --url BASE] [--yes] [--allow-published]   (PREVIEWS by default; never deletes the git tag)\n" +
      "  Dialects for release-create: default = Gitea/Forgejo (--url BASE, default https://codeberg.org); --gh = GitHub (fixed api.github.com); --gl = GitLab (--url BASE, default https://gitlab.com).\n" +
      "  Tokens come ONLY from fixed env names (never a flag): Gitea = CODEBERG_TOKEN/FORGE_TOKEN, GitHub = GITHUB_TOKEN/GH_TOKEN, GitLab = GITLAB_TOKEN (GENSCALATOR_-prefixed variants win first)."
  )

  private val Help: String =
    """tt forge — Forgejo/Gitea forge client (default: Codeberg)
      |
      |Talks to a forge's REST API without hand-curling it: list releases and tags
      |(no auth needed on public repos), verify a token, and create or edit releases.
      |The token is read ONLY from human-set env vars — never from a flag.
      |
      |Usage:
      |  forge whoami   [--url BASE]                             verify auth: print the token's
      |                                                          login (never the token itself)
      |  forge releases <owner>/<repo> [--gh | --url BASE] [--limit N]
      |                                                          list releases, with each release's
      |                                                          asset names indented under it (READ)
      |  forge tags     <owner>/<repo> [--gh | --url BASE] [--limit N]  list tags (READ)
      |  forge issues <owner>/<repo> [--gh | --url BASE] [--state S] [--limit N]
      |                                                          list issues   (READ)
      |  forge prs    <owner>/<repo> [--gh | --url BASE] [--state S] [--limit N]
      |                                                          list PRs, head branch in [brackets]
      |  forge contributors <owner>/<repo> [--gh | --gl | --url BASE] [--limit N]
      |                                                          list contributors (READ; --gh/--gl only —
      |                                                          Gitea has no such endpoint)
      |  forge issue  <owner>/<repo> <n> [--gh | --url BASE]     show an issue + comments (READ)
      |  forge pr     <owner>/<repo> <n> [--gh | --url BASE]     show a PR: merge state + body (READ)
      |  forge protection <owner>/<repo> <branch> [--gh | --url BASE]
      |                                                          show the protection rule (token)
      |  forge release-create <owner>/<repo> <tag> [--gh | --gl | --url BASE]
      |                       [--name S] [--body S | --body-file F]
      |                       [--prerelease] [--draft] [--target COMMITISH]
      |                       (create a release; three dialects — see below)
      |  forge release-edit   <owner>/<repo> <tag> [--name S] [--body S | --body-file F]
      |                       [--tag S] [--prerelease] [--draft] [--gh | --url BASE]
      |                       (PATCH an existing release; sends ONLY the provided fields;
      |                        --gh edits on GitHub and finds DRAFTS too; --tag re-points a
      |                        draft's tag, e.g. after the UI reset it to untagged-...)
      |  forge release-upload <owner>/<repo> <tag> <file> [--name N] [--gh | --url BASE]
      |                       (attach ONE file to an existing release, drafts included;
      |                        refuses a duplicate asset name)
      |  forge release-download <owner>/<repo> <tag> [--gh | --url BASE]
      |                       [--pattern GLOB] [--dir D] [--verify]
      |                       (download release assets; finds DRAFTS too, which the tags
      |                        endpoint cannot; --verify checks each payload against its
      |                        downloaded .sha256 and says UNVERIFIED when there is none)
      |  forge release-delete <owner>/<repo> <tag> [--gh | --url BASE]
      |                       [--yes] [--allow-published]
      |                       (DESTRUCTIVE: previews by default like `tt sub`, applies only
      |                        with --yes; a PUBLISHED release additionally needs
      |                        --allow-published; the git TAG is never deleted)
      |Flags:
      |  --url BASE        forge base URL (Gitea default https://codeberg.org; GitLab default https://gitlab.com)
      |  --limit N         max items for releases/tags (default 50)
      |  --name S          release title (default: the tag)
      |  --body S          release notes inline; --body-file F reads them from a file
      |  --prerelease      mark as prerelease (Gitea/GitHub only; GitLab has no such flag)
      |  --draft           mark as draft (Gitea/GitHub only; GitLab has no such flag)
      |  --target C        commitish/ref the new tag points at (release-create only)
      |  --gh              talk to the GitHub API (fixed api.github.com) instead of a Gitea forge
      |  --gl              talk to the GitLab API (--url BASE, default https://gitlab.com)
      |  --state S         open | closed | all for issues/prs (default open)
      |
      |Token: whoami, release-create/edit/upload/delete and protection read the token from env
      |GENSCALATOR_CODEBERG_TOKEN, then CODEBERG_TOKEN, then FORGE_TOKEN — never a flag,
      |and it is only ever sent to a trusted host (codeberg.org; the human may extend
      |the set via env TT_FORGE_HOSTS). Effectful verbs print an [audit] line first.
      |GitHub verbs (--gh) read their token from env GENSCALATOR_GITHUB_TOKEN, GITHUB_TOKEN
      |or GH_TOKEN and only ever send it to api.github.com; reads work anonymously too
      |(60 requests/h), protection requires the token (admin-scoped read). release-create
      |--gh needs it too (Contents: read-and-write, or the classic `repo` scope).
      |GitLab release-create (--gl) reads its token from env GENSCALATOR_GITLAB_TOKEN or
      |GITLAB_TOKEN (scope: api), sends it as PRIVATE-TOKEN, and only ever to a trusted host
      |(gitlab.com; the HUMAN extends the set via env TT_FORGE_GITLAB_HOSTS or TT_FORGE_HOSTS
      |for self-managed instances like git.cs.lth.se).
      |
      |Examples:
      |  tt forge releases bjornregnell/genscalator --limit 5    # latest 5 releases
      |  tt forge tags bjornregnell/genscalator                  # tag list with short SHAs
      |  tt forge release-create bjornregnell/genscalator v0.9.0 --name "v0.9.0: title" \
      |           --body-file NOTES.md --prerelease                # Gitea/Codeberg (default)
      |  tt forge release-create bjornregnell/prontopop v0.1.1 --gh --name "v0.1.1" \
      |           --body-file NOTES.md                             # GitHub
      |  tt forge release-create bjornregnell/prontopop v0.1.1 --gl \
      |           --url https://git.cs.lth.se --name "v0.1.1" --body-file NOTES.md   # GitLab
      |
      |Full reference: tools/README.md""".stripMargin

  // ================================================================================================
  // The release client. Credential acquisition, the trusted-host guard, the dialect logic and the whole
  // download/verify path MOVED to `releaselib.scala` on 2026-07-27 (D7a), because `tt update --native`
  // needs exactly the same machinery and the toolbox's dependency graph is deliberately FLAT — tools
  // depend on shared libs, never on each other.
  //
  // What remains below are FORWARDERS, not second definitions: the same move `globMatches` already made
  // to `Lib`. They exist so this file's ~40 existing call sites keep reading the way they did, while
  // there is exactly ONE definition of each security-relevant rule, in one reviewable place.
  // ================================================================================================
  private val rl = ReleaseLib.Client("forge")

  private def TokenEnvNames   = rl.TokenEnvNames
  private def GhTokenEnvNames = rl.GhTokenEnvNames
  private def token           = rl.token
  private def ghToken         = rl.ghToken
  private def trustedHosts    = rl.trustedHosts
  private def ghHeaders       = rl.ghHeaders
  private def isGitHub(base: String)  = rl.isGitHub(base)
  private def hostOf(url: String)     = ReleaseLib.hostOf(url)
  private def apiBase(url: String)    = ReleaseLib.apiBase(url)
  private def splitRepo(s: String)    = rl.splitRepo(s)

  // The GitHub REST root — a FIXED constant, never derived from --url, so the GitHub token can only ever
  // travel to that one host (the same no-redirect rule trustedHosts enforces for the Gitea token).
  private def GitHubApi = rl.GitHubApi

  // GitLab dialect. Unlike GitHub, GitLab has self-managed instances (git.cs.lth.se, …), so the base URL IS
  // configurable via --url — which means the trusted-host guard matters here exactly as it does for the Gitea
  // token: the token only ever travels to a host in gitlabTrustedHosts. Default: gitlab.com; the HUMAN extends
  // the set via env TT_FORGE_GITLAB_HOSTS (or the shared TT_FORGE_HOSTS) — never a flag. Auth header is
  // `PRIVATE-TOKEN` (not `Authorization: token`); token from fixed human-set env names only.
  private val GlTokenEnvNames = List("GENSCALATOR_GITLAB_TOKEN", "GITLAB_TOKEN")
  private def glToken: Option[String] =
    GlTokenEnvNames.iterator.flatMap(sys.env.get).map(_.trim).find(_.nonEmpty)
  private def gitlabTrustedHosts: Set[String] =
    val extra = List("TT_FORGE_GITLAB_HOSTS", "TT_FORGE_HOSTS").iterator
      .flatMap(n => sys.env.getOrElse(n, "").split(",")).map(_.trim).filter(_.nonEmpty).toSet
    Set("gitlab.com") ++ extra

  private def userLogin(v: ujson.Value): String =
    Try(v.obj("user").obj("login").str).getOrElse("?")

  // one issue/PR per line: number, updated, author, title (tab-separated, like releases/tags)
  private def itemLine(v: ujson.Value): String =
    val num     = Try(v.obj("number").num.toLong).getOrElse(0L)
    val title   = Try(v.obj("title").str).getOrElse("?")
    val updated = Try(v.obj("updated_at").str).getOrElse("")
    s"#$num\t$updated\t${userLogin(v)}\t$title"

  // See ReleaseLib.strOr for WHY this predicate exists — it is the null-vs-absent trap a forge walks into.
  private def strOr(v: Option[ujson.Value], default: String) = ReleaseLib.strOr(v, default)
  private def strOrEmpty(v: Option[ujson.Value])             = ReleaseLib.strOrEmpty(v)

  def dispatch(args: String*): Unit =
    if args.contains("--help") || args.contains("-h") then { println(Help); sys.exit(0) }
    args.toList match
      case "whoami" :: rest         => whoami(rest)
      case "releases" :: rest       => listReleases(rest)
      case "tags" :: rest           => listTags(rest)
      case "issues" :: rest         => listIssues(rest)
      case "prs" :: rest            => listPrs(rest)
      case "contributors" :: rest   => listContributors(rest)
      case "issue" :: rest          => showIssue(rest)
      case "pr" :: rest             => showPr(rest)
      case "protection" :: rest     => showProtection(rest)
      case "release-create" :: rest => releaseCreate(rest)
      case "release-edit" :: rest   => releaseEdit(rest)
      case "release-download" :: rest => releaseDownload(rest)
      case "release-upload" :: rest => releaseUpload(rest)
      case "release-delete" :: rest => releaseDelete(rest)
      case _                        => forgeUsage()

  // whoami — authenticated READ (GET /user) to verify the token inherits + is valid. Prints only the login and
  // which env var supplied the token (NEVER the token). Trusted-host-guarded like release-create.
  private def whoami(args: List[String]): Unit =
    @annotation.tailrec
    def go(rest: List[String], base: String): String =
      rest match
        case Nil                                => base
        case "--url" :: u :: t                  => go(t, u)
        case flag :: _ if flag.startsWith("--") => die(s"unknown/incomplete flag '$flag'")
        case other :: _                         => die(s"unexpected argument '$other'")
    val base = go(args, DefaultBase)
    val tok  = token.getOrElse(die(s"whoami needs a token — the HUMAN sets one of env ${TokenEnvNames.mkString(", ")} (never a flag)."))
    val url  = s"${apiBase(base)}/user"
    val host = hostOf(url)
    if !trustedHosts.contains(host) then die(
      s"refusing to send the token to untrusted host '$host'. Trusted: ${trustedHosts.toVector.sorted.mkString(", ")} (extend via env TT_FORGE_HOSTS).")
    val r = Try(requests.get(url, headers = Map("Authorization" -> s"token $tok"),
      check = false, readTimeout = 30000, connectTimeout = 10000)).getOrElse(die("request failed"))
    r.statusCode match
      case 200 =>
        val login = Try(ujson.read(r.text()).obj.get("login").map(_.str).getOrElse("?")).getOrElse("?")
        val src   = TokenEnvNames.find(n => sys.env.get(n).exists(_.trim.nonEmpty)).getOrElse("?")
        println(s"authenticated as $login on $host (token from env $src)")
      case 401 => die(s"token present but rejected (401) by $host — check the token / its scope")
      case c   => die(s"GET $url -> $c ${r.statusMessage}")

  private final case class ReadOpts(repo: Option[String], base: String, limit: Int, state: String)

  private def parseRead(args: List[String]): ReadOpts =
    @annotation.tailrec
    def go(rest: List[String], o: ReadOpts): ReadOpts =
      rest match
        case Nil                 => o
        case "--url" :: u :: t   => go(t, o.copy(base = u))
        case "--gh" :: t         => go(t, o.copy(base = "https://github.com"))
        case "--state" :: s :: t =>
          if Set("open", "closed", "all").contains(s) then go(t, o.copy(state = s))
          else die(s"--state must be open, closed or all, got '$s'")
        case "--limit" :: n :: t =>
          n.toIntOption match
            case Some(v) if v > 0 => go(t, o.copy(limit = v))
            case _                => die(s"--limit needs a positive integer, got '$n'")
        case flag :: _ if flag.startsWith("--") => die(s"unknown/incomplete flag '$flag'")
        case r :: t if o.repo.isEmpty            => go(t, o.copy(repo = Some(r)))
        case other :: _                          => die(s"unexpected argument '$other'")
    go(args, ReadOpts(None, DefaultBase, 50, "open"))

  private def getJson(url: String, headers: Map[String, String] = Map.empty): ujson.Value =
    val r = Try(requests.get(url, headers = headers, check = false, readTimeout = 30000, connectTimeout = 10000))
      .getOrElse(die(s"request failed: $url"))
    if r.statusCode != 200 then die(s"GET $url -> ${r.statusCode} ${r.statusMessage}")
    Try(ujson.read(r.text())).getOrElse(die(s"unexpected (non-JSON) response from $url"))

  private def listReleases(args: List[String]): Unit =
    val o             = parseRead(args)
    val (owner, repo) = splitRepo(o.repo.getOrElse(forgeUsage()))
    // --gh sets base to github.com, but this verb used to build a Gitea path against it regardless,
    // producing https://github.com/api/v1/... -> 410 Gone. Route on the dialect like every other
    // dual-dialect read verb does. GitHub spells the page size per_page, Gitea limit.
    val gh = isGitHub(o.base)
    val url =
      if gh then s"$GitHubApi/repos/$owner/$repo/releases?per_page=${o.limit}"
      else s"${apiBase(o.base)}/repos/$owner/$repo/releases?limit=${o.limit}"
    val arr = Try(getJson(url, if gh then ghHeaders else Map.empty).arr)
      .getOrElse(die("expected a JSON array of releases"))
    if arr.isEmpty then println("(no releases)")
    else
      arr.foreach { rel =>
        val tag   = strOr(rel.obj.get("tag_name"), "?")
        val name  = strOrEmpty(rel.obj.get("name"))
        val pre   = rel.obj.get("prerelease").exists(_.bool)
        val draft = rel.obj.get("draft").exists(_.bool)
        val pub   = strOrEmpty(rel.obj.get("published_at"))
        val flags = (if draft then " [draft]" else "") + (if pre then " [prerelease]" else "")
        println(s"$tag\t$pub$flags\t$name")
        // Asset names, indented under their release. Both dialects expose `assets`, and this is
        // what makes "which platforms does that release actually ship?" answerable here instead
        // of by hand-curling the API — the gap that sent a caller back to a raw forge client.
        val assets = rel.obj.get("assets").toList.flatMap(a => Try(a.arr.toList).getOrElse(Nil))
        assets.foreach { a =>
          val an = strOr(a.obj.get("name"), "?")
          println(s"\t  $an")
        }
      }

  private def listTags(args: List[String]): Unit =
    val o             = parseRead(args)
    val (owner, repo) = splitRepo(o.repo.getOrElse(forgeUsage()))
    val gh = isGitHub(o.base) // same dialect bug as releases had; see the note there
    val url =
      if gh then s"$GitHubApi/repos/$owner/$repo/tags?per_page=${o.limit}"
      else s"${apiBase(o.base)}/repos/$owner/$repo/tags?limit=${o.limit}"
    val arr = Try(getJson(url, if gh then ghHeaders else Map.empty).arr)
      .getOrElse(die("expected a JSON array of tags"))
    if arr.isEmpty then println("(no tags)")
    else
      arr.foreach { t =>
        val name = strOr(t.obj.get("name"), "?")
        val sha  = Try(t.obj("commit").obj("sha").str).getOrElse("").take(10)
        println(s"$name\t$sha")
      }

  // ---- issue / PR / branch-protection READ verbs (both dialects; --gh = GitHub) ----

  private def listIssues(args: List[String]): Unit =
    val o             = parseRead(args)
    val (owner, repo) = splitRepo(o.repo.getOrElse(forgeUsage()))
    val arr =
      if isGitHub(o.base) then
        // GitHub's /issues endpoint interleaves PRs — drop entries carrying a pull_request key
        Try(getJson(s"$GitHubApi/repos/$owner/$repo/issues?state=${o.state}&per_page=${o.limit}", ghHeaders).arr)
          .getOrElse(die("expected a JSON array of issues")).filterNot(_.obj.contains("pull_request"))
      else
        Try(getJson(s"${apiBase(o.base)}/repos/$owner/$repo/issues?state=${o.state}&type=issues&limit=${o.limit}").arr)
          .getOrElse(die("expected a JSON array of issues"))
    arr.foreach(i => println(itemLine(i)))
    println(s"=== ${arr.size} ${o.state} issues")

  private def listPrs(args: List[String]): Unit =
    val o             = parseRead(args)
    val (owner, repo) = splitRepo(o.repo.getOrElse(forgeUsage()))
    val gh            = isGitHub(o.base)
    val url =
      if gh then s"$GitHubApi/repos/$owner/$repo/pulls?state=${o.state}&per_page=${o.limit}"
      else s"${apiBase(o.base)}/repos/$owner/$repo/pulls?state=${o.state}&limit=${o.limit}"
    val arr = Try(getJson(url, if gh then ghHeaders else Map.empty).arr)
      .getOrElse(die("expected a JSON array of pull requests"))
    arr.foreach { p =>
      val head = Try(p.obj("head").obj("ref").str).getOrElse("?")
      println(s"${itemLine(p)}\t[$head]")
    }
    println(s"=== ${arr.size} ${o.state} PRs")

  // contributors — READ the repo's contributor list (the "who does the forge think contributed" verb; born
  // from the SM217 investigation, where verifying it needed raw `tt web get` on the API). Reuses the SM207
  // --gh/--gl dialect routing. GitHub prints login/contributions/type (type = User|Bot — the thing that
  // answers "why is a bot on the list"); GitLab prints name/email/commits. The Gitea/Forgejo REST API has NO
  // contributors endpoint (verified 2026-07-24: Codeberg returns 404), so the default dialect says so plainly
  // rather than 404-ing cryptically. Anonymous read for GitLab (no token → no trusted-host surface); GitHub
  // sends its token only to the fixed api.github.com root (ghHeaders), same no-redirect rule as the other reads.
  private final case class ContribOpts(repo: Option[String], base: String, limit: Int, dialect: Dialect)

  private def listContributors(args: List[String]): Unit =
    @annotation.tailrec
    def go(rest: List[String], o: ContribOpts): ContribOpts =
      rest match
        case Nil                 => o
        case "--url" :: u :: t   => go(t, o.copy(base = u)) // base only; dialect stays as --gh/--gl set it (default Gitea)
        case "--gh" :: t         => go(t, o.copy(dialect = Dialect.GitHub))
        case "--gl" :: t         => go(t, o.copy(dialect = Dialect.GitLab))
        case "--limit" :: n :: t =>
          n.toIntOption match
            case Some(v) if v > 0 => go(t, o.copy(limit = v))
            case _                => die(s"--limit needs a positive integer, got '$n'")
        case flag :: _ if flag.startsWith("--") => die(s"unknown/incomplete flag '$flag'")
        case r :: t if o.repo.isEmpty            => go(t, o.copy(repo = Some(r)))
        case other :: _                          => die(s"unexpected argument '$other'")
    val o             = go(args, ContribOpts(None, DefaultBase, 50, Dialect.Gitea))
    val (owner, repo) = splitRepo(o.repo.getOrElse(forgeUsage()))
    o.dialect match
      case Dialect.GitHub =>
        val arr = Try(getJson(s"$GitHubApi/repos/$owner/$repo/contributors?per_page=${o.limit}", ghHeaders).arr)
          .getOrElse(die("expected a JSON array of contributors"))
        arr.foreach { c =>
          val login = Try(c.obj("login").str).getOrElse("?")
          val n     = Try(c.obj("contributions").num.toLong).getOrElse(0L)
          val typ   = Try(c.obj("type").str).getOrElse("")
          println(s"$login\t$n\t$typ")
        }
        println(s"=== ${arr.size} contributors")
      case Dialect.GitLab =>
        val base = if o.base == DefaultBase then "https://gitlab.com" else o.base // default gitlab.com, not codeberg
        val proj = s"$owner%2F$repo" // GitLab wants the project path URL-encoded ('/' -> %2F)
        val arr = Try(getJson(s"${base.stripSuffix("/")}/api/v4/projects/$proj/repository/contributors?per_page=${o.limit}").arr)
          .getOrElse(die("expected a JSON array of contributors"))
        arr.foreach { c =>
          val name    = Try(c.obj("name").str).getOrElse("?")
          val email   = Try(c.obj("email").str).getOrElse("")
          val commits = Try(c.obj("commits").num.toLong).getOrElse(0L)
          println(s"$name\t$email\t$commits")
        }
        println(s"=== ${arr.size} contributors")
      case Dialect.Gitea =>
        die("contributors is supported for --gh (GitHub) and --gl (GitLab) only; the Gitea/Forgejo REST API\n" +
          "  has no contributors endpoint (Codeberg returns 404) — read the contributor graph from the web UI.")

  private final case class ItemOpts(repo: Option[String], item: Option[String], base: String)

  private def parseItem(args: List[String]): ItemOpts =
    @annotation.tailrec
    def go(rest: List[String], o: ItemOpts): ItemOpts =
      rest match
        case Nil               => o
        case "--url" :: u :: t => go(t, o.copy(base = u))
        case "--gh" :: t       => go(t, o.copy(base = "https://github.com"))
        case flag :: _ if flag.startsWith("--") => die(s"unknown/incomplete flag '$flag'")
        case r :: t if o.repo.isEmpty => go(t, o.copy(repo = Some(r)))
        case i :: t if o.item.isEmpty => go(t, o.copy(item = Some(i)))
        case other :: _               => die(s"unexpected argument '$other'")
    go(args, ItemOpts(None, None, DefaultBase))

  private def showIssue(args: List[String]): Unit =
    val o             = parseItem(args)
    val (owner, repo) = splitRepo(o.repo.getOrElse(forgeUsage()))
    val n    = o.item.flatMap(_.toIntOption).getOrElse(die("expected an issue number after <owner>/<repo>"))
    val gh   = isGitHub(o.base)
    val root = if gh then s"$GitHubApi/repos/$owner/$repo/issues/$n" else s"${apiBase(o.base)}/repos/$owner/$repo/issues/$n"
    val hdrs = if gh then ghHeaders else Map.empty[String, String]
    val issue = getJson(root, hdrs)
    println(itemLine(issue))
    println(s"state: ${Try(issue.obj("state").str).getOrElse("?")}")
    println("")
    println(strOrEmpty(issue.obj.get("body")))
    val commentsUrl = if gh then s"$root/comments?per_page=100" else s"$root/comments"
    val comments = Try(getJson(commentsUrl, hdrs).arr).getOrElse(die("expected a JSON array of comments"))
    comments.foreach { c =>
      println(s"\n--- comment by ${userLogin(c)} at ${Try(c.obj("created_at").str).getOrElse("?")} ---")
      println(strOrEmpty(c.obj.get("body")))
    }
    println(s"\n=== ${comments.size} comments")

  private def showPr(args: List[String]): Unit =
    val o             = parseItem(args)
    val (owner, repo) = splitRepo(o.repo.getOrElse(forgeUsage()))
    val n   = o.item.flatMap(_.toIntOption).getOrElse(die("expected a PR number after <owner>/<repo>"))
    val gh  = isGitHub(o.base)
    val url = if gh then s"$GitHubApi/repos/$owner/$repo/pulls/$n" else s"${apiBase(o.base)}/repos/$owner/$repo/pulls/$n"
    val pr  = getJson(url, if gh then ghHeaders else Map.empty)
    println(itemLine(pr))
    val baseRef   = Try(pr.obj("base").obj("ref").str).getOrElse("?")
    val headRef   = Try(pr.obj("head").obj("ref").str).getOrElse("?")
    val state     = Try(pr.obj("state").str).getOrElse("?")
    val merged    = Try(pr.obj("merged").bool).getOrElse(false)
    val mergeable = pr.obj.get("mergeable").map(v => Try(v.bool).map(_.toString).getOrElse("computing")).getOrElse("?")
    val mergeState = if gh then s"  merge_state=${Try(pr.obj("mergeable_state").str).getOrElse("?")}" else ""
    println(s"state: $state  merged=$merged  mergeable=$mergeable$mergeState  $headRef -> $baseRef")
    println("")
    println(strOrEmpty(pr.obj.get("body")))

  private def showProtection(args: List[String]): Unit =
    val o             = parseItem(args)
    val (owner, repo) = splitRepo(o.repo.getOrElse(forgeUsage()))
    val branch        = o.item.getOrElse(die("expected a branch name after <owner>/<repo>"))
    if isGitHub(o.base) then
      if ghToken.isEmpty then die(
        s"protection on GitHub needs an admin-read token — the HUMAN sets one of env ${GhTokenEnvNames.mkString(", ")} (never a flag).")
      val p = getJson(s"$GitHubApi/repos/$owner/$repo/branches/$branch/protection", ghHeaders)
      def enabled(key: String): String =
        Try(p.obj(key).obj("enabled").bool).toOption.map(b => if b then "yes" else "no").getOrElse("?")
      p.obj.get("required_status_checks") match
        case Some(c) =>
          val strict = Try(c.obj("strict").bool).getOrElse(false)
          val ctxs   = Try(c.obj("contexts").arr.map(_.str).toList).getOrElse(Nil)
          println(s"required status checks: ${if ctxs.isEmpty then "(none selected)" else ctxs.mkString(", ")}  strict=$strict")
        case None => println("required status checks: NONE")
      println(s"enforce admins: ${enabled("enforce_admins")}")
      println(s"required PR reviews: ${if p.obj.contains("required_pull_request_reviews") then "yes" else "no"}")
      p.obj.get("restrictions") match
        case Some(r) =>
          val users = Try(r.obj("users").arr.map(_.obj("login").str).toList).getOrElse(Nil)
          val teams = Try(r.obj("teams").arr.map(_.obj("slug").str).toList).getOrElse(Nil)
          println(s"push restricted to: users=[${users.mkString(", ")}] teams=[${teams.mkString(", ")}]")
        case None => println("push restrictions: none")
      println(s"force pushes allowed: ${enabled("allow_force_pushes")}")
      println(s"deletions allowed: ${enabled("allow_deletions")}")
    else
      val tok = token.getOrElse(die(
        s"protection needs a token — the HUMAN sets one of env ${TokenEnvNames.mkString(", ")} (never a flag)."))
      val url  = s"${apiBase(o.base)}/repos/$owner/$repo/branch_protections"
      val host = hostOf(url)
      if !trustedHosts.contains(host) then die(
        s"refusing to send the token to untrusted host '$host'. Trusted: ${trustedHosts.toVector.sorted.mkString(", ")}.")
      val r = Try(requests.get(url, headers = Map("Authorization" -> s"token $tok"),
        check = false, readTimeout = 30000, connectTimeout = 10000)).getOrElse(die("request failed"))
      if r.statusCode != 200 then die(s"GET $url -> ${r.statusCode} ${r.statusMessage}")
      val arr  = Try(ujson.read(r.text()).arr).getOrElse(die("expected a JSON array of branch protections"))
      val hits = arr.filter(p =>
        Try(p.obj("branch_name").str).toOption.orElse(Try(p.obj("rule_name").str).toOption).contains(branch))
      if hits.isEmpty then println(s"no branch protection rule matches '$branch' (${arr.size} rules total)")
      else hits.foreach { p =>
        val checksOn = Try(p.obj("enable_status_check").bool).getOrElse(false)
        val ctxs     = Try(p.obj("status_check_contexts").arr.map(_.str).toList).getOrElse(Nil)
        println(s"rule: $branch  status-checks=${if checksOn then ctxs.mkString(", ") else "off"}")
        println(s"push whitelist: ${Try(p.obj("push_whitelist_usernames").arr.map(_.str).toList).getOrElse(Nil).mkString(", ")}")
        println(s"force pushes allowed: ${Try(p.obj("enable_force_push").bool).getOrElse(false)}")
      }

  // Which forge dialect release-create/edit speaks. Gitea (default, --url) posts the Gitea payload with an
  // `Authorization: token` header to a trustedHosts host; GitHub (--gh) posts to the FIXED api.github.com root;
  // GitLab (--gl) posts the /api/v4 payload with a `PRIVATE-TOKEN` header to a gitlabTrustedHosts host.
  //
  // Defined in ReleaseLib because it appears in the shared download signatures; aliased here so every
  // `Dialect.Gitea` in this file still reads the same. ONE enum, two tools.
  private type Dialect = ReleaseLib.Dialect
  private val Dialect  = ReleaseLib.Dialect

  private final case class CreateOpts(repo: Option[String], tag: Option[String], name: Option[String],
      body: Option[String], bodyFile: Option[String], prerelease: Boolean, draft: Boolean,
      target: Option[String], base: String, dialect: Dialect)

  private def releaseCreate(args: List[String]): Unit =
    @annotation.tailrec
    def go(rest: List[String], o: CreateOpts): CreateOpts =
      rest match
        case Nil                       => o
        case "--name" :: s :: t        => go(t, o.copy(name = Some(s)))
        case "--body" :: s :: t        => go(t, o.copy(body = Some(s)))
        case "--body-file" :: f :: t   => go(t, o.copy(bodyFile = Some(f)))
        case "--prerelease" :: t       => go(t, o.copy(prerelease = true))
        case "--draft" :: t            => go(t, o.copy(draft = true))
        case "--target" :: c :: t      => go(t, o.copy(target = Some(c)))
        case "--url" :: u :: t         => go(t, o.copy(base = u))
        case "--gh" :: t               => go(t, o.copy(dialect = Dialect.GitHub))
        case "--gl" :: t               => go(t, o.copy(dialect = Dialect.GitLab))
        case flag :: _ if flag.startsWith("--") => die(s"unknown/incomplete flag '$flag'")
        case r :: t if o.repo.isEmpty  => go(t, o.copy(repo = Some(r)))
        case tg :: t if o.tag.isEmpty  => go(t, o.copy(tag = Some(tg)))
        case other :: _                => die(s"unexpected argument '$other'")
    val o             = go(args, CreateOpts(None, None, None, None, None, false, false, None, DefaultBase, Dialect.Gitea))
    val (owner, repo) = splitRepo(o.repo.getOrElse(forgeUsage()))
    val tag           = o.tag.getOrElse(forgeUsage())
    val bodyText = o.bodyFile match
      case Some(f) => Try(os.read(os.Path(f, os.pwd))).getOrElse(die(s"cannot read --body-file '$f'"))
      case None    => o.body.getOrElse("")
    o.dialect match
      case Dialect.Gitea  => createGitea(owner, repo, tag, bodyText, o)
      case Dialect.GitHub => createGitHub(owner, repo, tag, bodyText, o)
      case Dialect.GitLab => createGitLab(owner, repo, tag, bodyText, o)

  // Gitea/Forgejo dialect (default): POST <base>/api/v1/repos/<o>/<r>/releases, `Authorization: token`, GitHub-shaped payload.
  private def createGitea(owner: String, repo: String, tag: String, bodyText: String, o: CreateOpts): Unit =
    val tok = token.getOrElse(die(
      s"release-create needs a token — the HUMAN sets one of env ${TokenEnvNames.mkString(", ")}; it is deliberately\n" +
        "  NOT a flag, so the agent cannot self-authorize. Create one at Codeberg → Settings → Applications (write:repository)."))
    val url  = s"${apiBase(o.base)}/repos/$owner/$repo/releases"
    val host = hostOf(url)
    if !trustedHosts.contains(host) then die(
      s"refusing to send the token to untrusted host '$host'. Trusted: ${trustedHosts.toVector.sorted.mkString(", ")}.\n" +
        "  The HUMAN may extend the set via env TT_FORGE_HOSTS (comma-separated) — not a flag.")
    val payload = ujson.Obj(
      "tag_name"   -> tag,
      "name"       -> o.name.getOrElse(tag),
      "body"       -> bodyText,
      "prerelease" -> o.prerelease,
      "draft"      -> o.draft
    )
    o.target.foreach(c => payload("target_commitish") = c)
    System.err.println(s"forge: [audit] POST $url  tag=$tag name=${o.name.getOrElse(tag)} prerelease=${o.prerelease} draft=${o.draft}")
    val r = Try(requests.post(url, data = ujson.write(payload),
      headers = Map("Content-Type" -> "application/json", "Authorization" -> s"token $tok"),
      check = false, readTimeout = 30000, connectTimeout = 10000)).getOrElse(die("request failed"))
    r.statusCode match
      case 201 =>
        val html = Try(ujson.read(r.text()).obj.get("html_url").map(_.str).getOrElse("")).getOrElse("")
        println(s"created release $tag  $html")
      case 409 => die(s"a release for tag '$tag' already exists (409)")
      case c   => die(s"POST $url -> $c ${r.statusMessage}\n${r.text().take(500)}")

  // GitHub dialect (--gh): POST to the FIXED api.github.com root (never derived from --url, so the token cannot be
  // redirected — same no-exfiltration rule as the read verbs). Payload is already GitHub-shaped (Gitea copied it).
  private def createGitHub(owner: String, repo: String, tag: String, bodyText: String, o: CreateOpts): Unit =
    if o.base != DefaultBase then die("--gh targets the fixed GitHub API root; drop --url (it is not used with --gh).")
    val tok = ghToken.getOrElse(die(
      s"release-create --gh needs a token — the HUMAN sets one of env ${GhTokenEnvNames.mkString(", ")} (never a flag).\n" +
        "  Create a fine-grained token with Contents: read-and-write (or a classic token with the `repo` scope)."))
    val url     = s"$GitHubApi/repos/$owner/$repo/releases"
    val payload = ujson.Obj(
      "tag_name"   -> tag,
      "name"       -> o.name.getOrElse(tag),
      "body"       -> bodyText,
      "prerelease" -> o.prerelease,
      "draft"      -> o.draft
    )
    o.target.foreach(c => payload("target_commitish") = c)
    System.err.println(s"forge: [audit] POST $url  tag=$tag name=${o.name.getOrElse(tag)} prerelease=${o.prerelease} draft=${o.draft}")
    val r = Try(requests.post(url, data = ujson.write(payload),
      headers = Map("Content-Type" -> "application/json", "Accept" -> "application/vnd.github+json", "Authorization" -> s"Bearer $tok"),
      check = false, readTimeout = 30000, connectTimeout = 10000)).getOrElse(die("request failed"))
    r.statusCode match
      case 201 =>
        val html = Try(ujson.read(r.text()).obj.get("html_url").map(_.str).getOrElse("")).getOrElse("")
        println(s"created release $tag  $html")
      case 422 => die(s"GitHub rejected the release (422) — a release for tag '$tag' may already exist, or the ref is invalid.\n${r.text().take(500)}")
      case c   => die(s"POST $url -> $c ${r.statusMessage}\n${r.text().take(500)}")

  // GitLab dialect (--gl): POST <base>/api/v4/projects/<owner%2Frepo>/releases, `PRIVATE-TOKEN` header. Payload keys
  // differ (description, not body); GitLab has no prerelease/draft concept. Base is configurable (self-managed
  // instances) so the token only travels to a gitlabTrustedHosts host.
  private def createGitLab(owner: String, repo: String, tag: String, bodyText: String, o: CreateOpts): Unit =
    if o.prerelease || o.draft then die(
      "GitLab releases have no prerelease/draft flag — drop --prerelease/--draft when using --gl.")
    val base = if o.base == DefaultBase then "https://gitlab.com" else o.base // default gitlab.com, not codeberg
    val tok  = glToken.getOrElse(die(
      s"release-create --gl needs a token — the HUMAN sets one of env ${GlTokenEnvNames.mkString(", ")} (never a flag).\n" +
        "  Create a personal/project access token with the `api` scope."))
    val host = hostOf(base)
    if !gitlabTrustedHosts.contains(host) then die(
      s"refusing to send the token to untrusted host '$host'. Trusted: ${gitlabTrustedHosts.toVector.sorted.mkString(", ")}.\n" +
        "  The HUMAN may extend the set via env TT_FORGE_GITLAB_HOSTS or TT_FORGE_HOSTS (comma-separated) — not a flag.")
    val proj    = s"$owner%2F$repo" // GitLab wants the project path URL-encoded ('/' -> %2F)
    val url     = s"${base.stripSuffix("/")}/api/v4/projects/$proj/releases"
    val payload = ujson.Obj("tag_name" -> tag, "name" -> o.name.getOrElse(tag), "description" -> bodyText)
    o.target.foreach(c => payload("ref") = c) // GitLab creates the tag from `ref` when it doesn't yet exist
    System.err.println(s"forge: [audit] POST $url  tag=$tag name=${o.name.getOrElse(tag)}")
    val r = Try(requests.post(url, data = ujson.write(payload),
      headers = Map("Content-Type" -> "application/json", "PRIVATE-TOKEN" -> tok),
      check = false, readTimeout = 30000, connectTimeout = 10000)).getOrElse(die("request failed"))
    r.statusCode match
      case 201 =>
        val link = Try(ujson.read(r.text()).obj("_links").obj("self").str).getOrElse("")
        println(s"created release $tag  $link")
      case 409 => die(s"a release for tag '$tag' already exists (409)")
      case c   => die(s"POST $url -> $c ${r.statusMessage}\n${r.text().take(500)}")

  private final case class EditOpts(repo: Option[String], tag: Option[String], name: Option[String],
      body: Option[String], bodyFile: Option[String], setPrerelease: Boolean, setDraft: Boolean, base: String,
      dialect: Dialect, newTag: Option[String])

  /** PURE payload builder for release-edit: ONLY the provided fields, so unspecified fields are left
    * unchanged by the forge. Public so the request shape is unit-testable without a network (SM207).
    * `tagName` exists because a DRAFT's tag is just a text field the GitHub UI can silently reset to an
    * `untagged-...` placeholder on a save with the selector unpicked (it happened to v0.10.0 on
    * 2026-07-28, minutes after a body-only PATCH had matched the tag) — restoring it must be possible
    * without the UI. */
  def editPayload(name: Option[String], body: Option[String],
      setPrerelease: Boolean, setDraft: Boolean, tagName: Option[String] = None): ujson.Obj =
    val payload = ujson.Obj()
    body.foreach(b => payload("body") = b)
    name.foreach(n => payload("name") = n)
    tagName.foreach(t => payload("tag_name") = t)
    if setPrerelease then payload("prerelease") = true
    if setDraft then payload("draft") = true
    payload

  // release-edit — PATCH an EXISTING release: look it up by tag (unauth GET on Gitea; token-visible on
  // GitHub so DRAFTS are found), then send ONLY the provided fields (unspecified fields are left
  // unchanged by the forge). Same effectful/token/trusted-host rules as release-create; the write-auth
  // headers come from ReleaseLib.writeHeaders so the rule has ONE definition. `--gh` added 2026-07-28
  // (SM207's edit half): the immediate need was editing the v0.10.0 DRAFT notes, which had no typed shape.
  private def releaseEdit(args: List[String]): Unit =
    @annotation.tailrec
    def go(rest: List[String], o: EditOpts): EditOpts =
      rest match
        case Nil                       => o
        case "--name" :: s :: t        => go(t, o.copy(name = Some(s)))
        case "--body" :: s :: t        => go(t, o.copy(body = Some(s)))
        case "--body-file" :: f :: t   => go(t, o.copy(bodyFile = Some(f)))
        case "--tag" :: s :: t         => go(t, o.copy(newTag = Some(s)))
        case "--prerelease" :: t       => go(t, o.copy(setPrerelease = true))
        case "--draft" :: t            => go(t, o.copy(setDraft = true))
        case "--url" :: u :: t         => go(t, o.copy(base = u))
        case "--gh" :: t               => go(t, o.copy(dialect = Dialect.GitHub))
        case "--gl" :: _               => die(
          "release-edit is not implemented for GitLab (its release update API differs; use the web UI\n" +
            "  or extend this verb). Stated rather than faked — use --gh or the default Gitea dialect.")
        case flag :: _ if flag.startsWith("--") => die(s"unknown/incomplete flag '$flag'")
        case r :: t if o.repo.isEmpty  => go(t, o.copy(repo = Some(r)))
        case tg :: t if o.tag.isEmpty  => go(t, o.copy(tag = Some(tg)))
        case other :: _                => die(s"unexpected argument '$other'")
    val o             = go(args, EditOpts(None, None, None, None, None, false, false, DefaultBase, Dialect.Gitea, None))
    if o.dialect == Dialect.GitHub && o.base != DefaultBase then
      die("--gh targets the fixed GitHub API root; drop --url (it is not used with --gh).")
    val (owner, repo) = splitRepo(o.repo.getOrElse(forgeUsage()))
    val tag           = o.tag.getOrElse(forgeUsage())
    val bodyText = o.bodyFile match
      case Some(f) => Some(Try(os.read(os.Path(f, os.pwd))).getOrElse(die(s"cannot read --body-file '$f'")))
      case None    => o.body
    val payload = editPayload(o.name, bodyText, o.setPrerelease, o.setDraft, o.newTag)
    if payload.obj.isEmpty then die("nothing to edit — provide --body/--body-file, --name, --tag, --prerelease, or --draft")
    // Look up the release id via findRelease, which LISTS first and so can see a DRAFT. The previous
    // by-tag-only lookup made this verb structurally unable to edit a draft — the state you are most
    // likely to be editing, since a draft is by definition unfinished.
    val relJson = findRelease(owner, repo, tag, o.dialect, o.base)
    val id      = Try(relJson.obj("id").num.toLong).getOrElse(die(s"no release id found for tag '$tag'"))
    val url     = if o.dialect == Dialect.GitHub then s"$GitHubApi/repos/$owner/$repo/releases/$id"
                  else s"${apiBase(o.base)}/repos/$owner/$repo/releases/$id"
    val hdrs    = rl.writeHeaders(o.dialect, o.base, "release-edit") + ("Content-Type" -> "application/json")
    System.err.println(s"forge: [audit] PATCH $url  tag=$tag fields=${payload.obj.keys.mkString(",")}")
    val r = Try(requests.patch(url, data = ujson.write(payload),
      headers = hdrs,
      check = false, readTimeout = 30000, connectTimeout = 10000)).getOrElse(die("request failed"))
    r.statusCode match
      case 200 =>
        val html = Try(ujson.read(r.text()).obj.get("html_url").map(_.str).getOrElse("")).getOrElse("")
        println(s"edited release $tag  $html")
      case 404 => die(s"release for tag '$tag' not found (404)")
      case c   => die(s"PATCH $url -> $c ${r.statusMessage}\n${r.text().take(500)}")

  // ---- release ASSETS: download (+ integrity) and delete -------------------------------------
  //
  // WHY these exist: the release-asset lifecycle was the one part of shipping that had no typed
  // verb, so every "does that release actually carry a binary?" and every cleanup went out as a
  // raw `gh` call the guard cannot inspect. Built 2026-07-27 during the first release rehearsal,
  // where the missing shapes were hit twice within minutes of each other.

  private final case class AssetOpts(repo: Option[String], tag: Option[String], base: String,
      dialect: Dialect, pattern: Option[String], dir: String, verify: Boolean, yes: Boolean,
      allowPublished: Boolean)

  private def parseAsset(args: List[String], verb: String): AssetOpts =
    @annotation.tailrec
    def go(rest: List[String], o: AssetOpts): AssetOpts =
      rest match
        case Nil                        => o
        case "--pattern" :: p :: t      => go(t, o.copy(pattern = Some(p)))
        case "--dir" :: d :: t          => go(t, o.copy(dir = d))
        case "--verify" :: t            => go(t, o.copy(verify = true))
        case "--yes" :: t               => go(t, o.copy(yes = true))
        case "--allow-published" :: t   => go(t, o.copy(allowPublished = true))
        case "--url" :: u :: t          => go(t, o.copy(base = u))
        case "--gh" :: t                => go(t, o.copy(dialect = Dialect.GitHub))
        case "--gl" :: _                => die(
          s"$verb is not implemented for GitLab: GitLab releases carry LINKS to external artifacts\n" +
            "  rather than uploaded assets, so the same flags would mean something different there.\n" +
            "  Stated rather than faked — use --gh or the default Gitea/Forgejo dialect.")
        case flag :: _ if flag.startsWith("--") => die(s"unknown/incomplete flag '$flag'")
        case r :: t if o.repo.isEmpty   => go(t, o.copy(repo = Some(r)))
        case tg :: t if o.tag.isEmpty   => go(t, o.copy(tag = Some(tg)))
        case other :: _                 => die(s"unexpected argument '$other'")
    go(args, AssetOpts(None, None, DefaultBase, Dialect.Gitea, None, ".", false, false, false))

  // ------------------------------------------------------------------------------------------------
  // Release lookup + download: ONE definition, in ReleaseLib, shared with `tt update --native`.
  // Forwarders below keep this file's call sites unchanged. See releaselib.scala for the WHY of each —
  // the draft-vs-by-tag complementarity, the die-cannot-be-caught reason getJsonOpt exists, and the
  // trusted-host guard that is now written once instead of three times.
  // ------------------------------------------------------------------------------------------------
  private def getJsonOpt(url: String, headers: Map[String, String] = Map.empty) = rl.getJsonOpt(url, headers)
  private def findHeaders(dialect: Dialect, base: String)                        = rl.findHeaders(dialect, base)
  private def assetsOf(rel: ujson.Value)                                         = ReleaseLib.assetsOf(rel)
  private def globMatches(glob: String, name: String)                            = Lib.globMatches(glob, name)
  private def sha256Hex(p: os.Path)                                              = ReleaseLib.sha256Hex(p)
  private def writeHeaders(o: AssetOpts, verb: String)                           = rl.writeHeaders(o.dialect, o.base, verb)
  private def verifyChecksums(files: List[os.Path]): Unit                        = rl.verifyChecksums(files)

  private def findRelease(owner: String, repo: String, tag: String, dialect: Dialect, base: String): ujson.Value =
    rl.findRelease(owner, repo, tag, dialect, base)

  private def releaseDownload(args: List[String]): Unit =
    val o             = parseAsset(args, "release-download")
    val (owner, repo) = splitRepo(o.repo.getOrElse(forgeUsage()))
    val tag           = o.tag.getOrElse(forgeUsage())
    val rel           = findRelease(owner, repo, tag, o.dialect, o.base)
    val written       = rl.downloadAssets(rel, o.pattern, os.Path(o.dir, os.pwd), o.dialect, o.base, "release-download")
    if o.verify then verifyChecksums(written)
    else println("(no --verify: bytes downloaded but NOT checked against a .sha256)")

  /** PURE upload-URL builder, public for unit tests (SM207's testable-request-building rule).
    * GitHub uploads go to a SECOND fixed root (uploads.github.com) — fixed for the same reason as
    * GitHubApi: the token must never travel to a host derived from input (not even from the release
    * JSON's own upload_url, which is attacker-influenceable in principle). The asset name is
    * URL-encoded with %20 for spaces (URLEncoder's '+' is form-encoding, not query-encoding). */
  def uploadAssetUrl(isGh: Boolean, apiRoot: String, owner: String, repo: String, id: Long, name: String): String =
    val enc = java.net.URLEncoder.encode(name, "UTF-8").replace("+", "%20")
    if isGh then s"https://uploads.github.com/repos/$owner/$repo/releases/$id/assets?name=$enc"
    else s"$apiRoot/repos/$owner/$repo/releases/$id/assets?name=$enc"

  // release-upload — attach ONE file to an existing release (drafts included, via findRelease).
  // Built 2026-07-28: the v0.10.0 draft needed get-genscalator.sc attached and the only shapes were a
  // raw `gh release upload` or the web UI. GitHub sends the bytes as application/octet-stream to the
  // fixed uploads root; Gitea/Forgejo takes multipart form field `attachment` on the API root.
  private final case class UploadOpts(repo: Option[String], tag: Option[String], file: Option[String],
      name: Option[String], base: String, dialect: Dialect)

  private def releaseUpload(args: List[String]): Unit =
    @annotation.tailrec
    def go(rest: List[String], o: UploadOpts): UploadOpts =
      rest match
        case Nil                       => o
        case "--name" :: n :: t        => go(t, o.copy(name = Some(n)))
        case "--url" :: u :: t         => go(t, o.copy(base = u))
        case "--gh" :: t               => go(t, o.copy(dialect = Dialect.GitHub))
        case "--gl" :: _               => die(
          "release-upload is not implemented for GitLab: GitLab releases carry LINKS to external\n" +
            "  artifacts rather than uploaded assets. Stated rather than faked — use --gh or the default dialect.")
        case flag :: _ if flag.startsWith("--") => die(s"unknown/incomplete flag '$flag'")
        case r :: t if o.repo.isEmpty  => go(t, o.copy(repo = Some(r)))
        case tg :: t if o.tag.isEmpty  => go(t, o.copy(tag = Some(tg)))
        case f :: t if o.file.isEmpty  => go(t, o.copy(file = Some(f)))
        case other :: _                => die(s"unexpected argument '$other'")
    val o             = go(args, UploadOpts(None, None, None, None, DefaultBase, Dialect.Gitea))
    if o.dialect == Dialect.GitHub && o.base != DefaultBase then
      die("--gh targets the fixed GitHub upload root; drop --url (it is not used with --gh).")
    val (owner, repo) = splitRepo(o.repo.getOrElse(forgeUsage()))
    val tag           = o.tag.getOrElse(forgeUsage())
    val path          = os.Path(o.file.getOrElse(forgeUsage()), os.pwd)
    if !os.isFile(path) then die(s"no such file: $path")
    val bytes         = os.read.bytes(path)
    val assetName     = o.name.getOrElse(path.last)
    val rel           = findRelease(owner, repo, tag, o.dialect, o.base)
    val id            = Try(rel.obj("id").num.toLong).getOrElse(die(s"no numeric release id for tag '$tag'"))
    val existing      = assetsOf(rel).map(a => strOr(a.obj.get("name"), "?"))
    if existing.contains(assetName) then die(
      s"an asset named '$assetName' already exists on release '$tag' — forges refuse duplicate names.\n" +
        s"  Delete it first, or upload under another name with --name.")
    val url  = uploadAssetUrl(o.dialect == Dialect.GitHub, apiBase(o.base), owner, repo, id, assetName)
    val hdrs = rl.writeHeaders(o.dialect, o.base, "release-upload")
    System.err.println(s"forge: [audit] POST $url  tag=$tag file=${path.last} bytes=${bytes.length}")
    val r =
      if o.dialect == Dialect.GitHub then
        Try(requests.post(url, data = bytes, headers = hdrs + ("Content-Type" -> "application/octet-stream"),
          check = false, readTimeout = 120000, connectTimeout = 10000)).getOrElse(die("request failed"))
      else
        Try(requests.post(url,
          data = requests.MultiPart(requests.MultiItem("attachment", bytes, assetName)),
          headers = hdrs, check = false, readTimeout = 120000, connectTimeout = 10000))
          .getOrElse(die("request failed"))
    r.statusCode match
      case 201 | 200 =>
        val dl = Try(ujson.read(r.text()).obj.get("browser_download_url").map(_.str).getOrElse("")).getOrElse("")
        println(s"uploaded $assetName (${bytes.length} B) to release $tag  $dl")
      case 422 => die(s"the forge rejected the upload (422) — most often a duplicate asset name.\n${r.text().take(500)}")
      case c   => die(s"POST $url -> $c ${r.statusMessage}\n${r.text().take(500)}")

  private def releaseDelete(args: List[String]): Unit =
    val o             = parseAsset(args, "release-delete")
    val (owner, repo) = splitRepo(o.repo.getOrElse(forgeUsage()))
    val tag           = o.tag.getOrElse(forgeUsage())
    val rel           = findRelease(owner, repo, tag, o.dialect, o.base)
    val id            = Try(rel.obj("id").num.toLong).getOrElse(die(s"no numeric release id for tag '$tag'"))
    val isDraft       = rel.obj.get("draft").exists(_.bool)
    val nAssets       = assetsOf(rel).size
    val what          = s"$tag (id $id, ${if isDraft then "draft" else "PUBLISHED"}, $nAssets asset(s))"
    // PREVIEW BY DEFAULT — the same contract as `tt sub`, and for the same reason: the cheapest way
    // to destroy the wrong release is a mistyped tag, and a preview costs one extra keystroke.
    if !o.yes then
      println(s"would DELETE release $what")
      println("  assets: " + (if nAssets == 0 then "(none)" else assetsOf(rel).map(a => strOr(a.obj.get("name"), "?")).mkString(", ")))
      println("  the git TAG is NOT touched — deleting a release and deleting its tag are different acts")
      // Tell the caller what will ACTUALLY work. Saying "--yes" for a published release would promise
      // something the next step refuses — a preview that misdescribes its own follow-up is the same
      // class of defect as a stale comment (SM243), and it is worse here because the verb is destructive.
      println(
        if isDraft then "  re-run with --yes to apply"
        else "  re-run with --yes --allow-published to apply (a PUBLISHED release needs BOTH flags)")
    else if !isDraft && !o.allowPublished then
      die(s"refusing to delete a PUBLISHED release ('$tag'): its notes are public and its asset URLs\n" +
        "  may already be in someone's install script. Pass --allow-published if that is truly intended.\n" +
        "  A draft needs no such flag, which is the case this verb was built for.")
    else
      val url  = if o.dialect == Dialect.GitHub then s"$GitHubApi/repos/$owner/$repo/releases/$id"
                 else s"${apiBase(o.base)}/repos/$owner/$repo/releases/$id"
      val hdrs = writeHeaders(o, "release-delete")
      System.err.println(s"forge: [audit] DELETE $url  tag=$tag draft=$isDraft assets=$nAssets")
      val r = Try(requests.delete(url, headers = hdrs, check = false,
        readTimeout = 30000, connectTimeout = 10000)).getOrElse(die("request failed"))
      r.statusCode match
        case 204 | 200 =>
          println(s"deleted release $what")
          println("  the git tag, if one existed, is untouched (tag deletion is out of scope here)")
        case 404 => die(s"release id $id not found (404) — it may already be gone")
        case c   => die(s"DELETE $url -> $c ${r.statusMessage}\n${r.text().take(500)}")
}

@main def forgeClient(args: String*): Unit = Forge.dispatch(args*)
