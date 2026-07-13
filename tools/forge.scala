//> using scala 3.8.4
//> using jvm 21
//> using dep com.lihaoyi::requests:0.9.3
//> using dep com.lihaoyi::ujson:4.4.3
//> using dep com.lihaoyi::os-lib:0.11.8

// forge — typed client for a Forgejo/Gitea forge (default: Codeberg). Replaces hand-curling the REST API
// (a dual-use `curl` carrying a token on the command line) with a narrow, effect-declared tool.
//   READ verbs (releases, tags) need NO auth (public repos) → safe to allowlist (`Bash(tt forge releases *)`).
//   The one EFFECTFUL verb (release-create) reads its token ONLY from a fixed set of human-set env vars
//   (GENSCALATOR_CODEBERG_TOKEN, then CODEBERG_TOKEN, then FORGE_TOKEN) — NEVER a flag — so the agent cannot self-authorize (same trust-boundary rule as
//   verify's TT_VERIFY_ALLOW / the configInArgsNotEnv PRD feature). It prints an [audit] line before acting,
//   and is deliberately NOT blanket-allowlistable (creating a release should stay a visible, confirmed op).
//   tt forge whoami   [--url BASE]                          # verify auth: print the token's login (never the token)
//   tt forge releases <owner>/<repo> [--url BASE] [--limit N]
//   tt forge tags     <owner>/<repo> [--url BASE] [--limit N]
//   tt forge release-create <owner>/<repo> <tag> [--name S] [--body S | --body-file F]
//                           [--prerelease] [--draft] [--target COMMITISH] [--url BASE]
//   tt forge release-edit   <owner>/<repo> <tag> [--name S] [--body S | --body-file F] [--prerelease] [--draft] [--url BASE]
//                           # PATCH an existing release (look up by tag); sends ONLY the provided fields
//   BASE defaults to https://codeberg.org
import scala.util.Try

// Helpers (die/token/hostOf/getJson/splitRepo/… and the opts types) scoped in this object so their generic
// names don't collide with other tools when the toolbox compiles together. Only the @main entry is top-level.
object Forge {
  private val DefaultBase = "https://codeberg.org"

  private def die(msg: String): Nothing = { System.err.println(s"forge: $msg"); sys.exit(2) }

  private def forgeUsage(): Nothing = die(
    "usage:\n" +
      "  forge whoami   [--url BASE]                              (verify auth: prints the token's login)\n" +
      "  forge releases <owner>/<repo> [--url BASE] [--limit N]\n" +
      "  forge tags     <owner>/<repo> [--url BASE] [--limit N]\n" +
      "  forge release-create <owner>/<repo> <tag> [--name S] [--body S | --body-file F] [--prerelease] [--draft] [--target C] [--url BASE]\n" +
      "  forge release-edit   <owner>/<repo> <tag> [--name S] [--body S | --body-file F] [--prerelease] [--draft] [--url BASE]\n" +
      "  BASE defaults to https://codeberg.org; release-create/edit read the token from env CODEBERG_TOKEN or FORGE_TOKEN (never a flag)."
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
      |  forge releases <owner>/<repo> [--url BASE] [--limit N]  list releases (READ, no auth)
      |  forge tags     <owner>/<repo> [--url BASE] [--limit N]  list tags     (READ, no auth)
      |  forge release-create <owner>/<repo> <tag> [--name S] [--body S | --body-file F]
      |                       [--prerelease] [--draft] [--target COMMITISH] [--url BASE]
      |  forge release-edit   <owner>/<repo> <tag> [--name S] [--body S | --body-file F]
      |                       [--prerelease] [--draft] [--url BASE]
      |                       (PATCH an existing release; sends ONLY the provided fields)
      |Flags:
      |  --url BASE        forge base URL (default https://codeberg.org)
      |  --limit N         max items for releases/tags (default 50)
      |  --name S          release title (default: the tag)
      |  --body S          release notes inline; --body-file F reads them from a file
      |  --prerelease      mark as prerelease
      |  --draft           mark as draft
      |  --target C        commitish the new tag points at (release-create only)
      |
      |Token: whoami and release-create/edit read the token from env
      |GENSCALATOR_CODEBERG_TOKEN, then CODEBERG_TOKEN, then FORGE_TOKEN — never a flag,
      |and it is only ever sent to a trusted host (codeberg.org; the human may extend
      |the set via env TT_FORGE_HOSTS). Effectful verbs print an [audit] line first.
      |
      |Examples:
      |  tt forge releases bjornregnell/genscalator --limit 5    # latest 5 releases
      |  tt forge tags bjornregnell/genscalator                  # tag list with short SHAs
      |  tt forge release-create bjornregnell/genscalator v0.9.0 --name "v0.9.0: title" \
      |           --body-file NOTES.md --prerelease
      |
      |Full reference: tools/README.md""".stripMargin

  // Token comes ONLY from a FIXED set of human-set env-var names — never a flag, and never an agent-nameable
  // var (an agent-chosen var name + an agent-chosen --url would let it POST an arbitrary secret to an
  // arbitrary host = exfiltration). Fixed names keep the authorization a human boundary.
  private val TokenEnvNames = List("GENSCALATOR_CODEBERG_TOKEN", "CODEBERG_TOKEN", "FORGE_TOKEN")
  private def token: Option[String] =
    TokenEnvNames.iterator.flatMap(sys.env.get).map(_.trim).find(_.nonEmpty)

  // The token may only be sent to a TRUSTED host — so the agent cannot redirect it to an attacker host via
  // --url. Default: codeberg.org. The HUMAN (not a flag) extends the set via env TT_FORGE_HOSTS (comma-sep).
  private def trustedHosts: Set[String] =
    val extra = sys.env.getOrElse("TT_FORGE_HOSTS", "").split(",").iterator.map(_.trim).filter(_.nonEmpty).toSet
    Set("codeberg.org") ++ extra

  private def hostOf(url: String): String =
    Try(Option(java.net.URI(url).getHost)).toOption.flatten.getOrElse("")

  private def splitRepo(s: String): (String, String) =
    s.split("/") match
      case Array(o, r) if o.nonEmpty && r.nonEmpty => (o, r)
      case _                                       => die(s"expected <owner>/<repo>, got '$s'")

  private def apiBase(url: String): String = url.stripSuffix("/") + "/api/v1"

  def dispatch(args: String*): Unit =
    if args.contains("--help") || args.contains("-h") then { println(Help); sys.exit(0) }
    args.toList match
      case "whoami" :: rest         => whoami(rest)
      case "releases" :: rest       => listReleases(rest)
      case "tags" :: rest           => listTags(rest)
      case "release-create" :: rest => releaseCreate(rest)
      case "release-edit" :: rest   => releaseEdit(rest)
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

  private final case class ReadOpts(repo: Option[String], base: String, limit: Int)

  private def parseRead(args: List[String]): ReadOpts =
    @annotation.tailrec
    def go(rest: List[String], o: ReadOpts): ReadOpts =
      rest match
        case Nil                 => o
        case "--url" :: u :: t   => go(t, o.copy(base = u))
        case "--limit" :: n :: t =>
          n.toIntOption match
            case Some(v) if v > 0 => go(t, o.copy(limit = v))
            case _                => die(s"--limit needs a positive integer, got '$n'")
        case flag :: _ if flag.startsWith("--") => die(s"unknown/incomplete flag '$flag'")
        case r :: t if o.repo.isEmpty            => go(t, o.copy(repo = Some(r)))
        case other :: _                          => die(s"unexpected argument '$other'")
    go(args, ReadOpts(None, DefaultBase, 50))

  private def getJson(url: String): ujson.Value =
    val r = Try(requests.get(url, check = false, readTimeout = 30000, connectTimeout = 10000))
      .getOrElse(die(s"request failed: $url"))
    if r.statusCode != 200 then die(s"GET $url -> ${r.statusCode} ${r.statusMessage}")
    Try(ujson.read(r.text())).getOrElse(die(s"unexpected (non-JSON) response from $url"))

  private def listReleases(args: List[String]): Unit =
    val o             = parseRead(args)
    val (owner, repo) = splitRepo(o.repo.getOrElse(forgeUsage()))
    val arr = Try(getJson(s"${apiBase(o.base)}/repos/$owner/$repo/releases?limit=${o.limit}").arr)
      .getOrElse(die("expected a JSON array of releases"))
    if arr.isEmpty then println("(no releases)")
    else
      arr.foreach { rel =>
        val tag   = rel.obj.get("tag_name").map(_.str).getOrElse("?")
        val name  = rel.obj.get("name").map(_.str).getOrElse("")
        val pre   = rel.obj.get("prerelease").exists(_.bool)
        val draft = rel.obj.get("draft").exists(_.bool)
        val pub   = rel.obj.get("published_at").map(_.str).getOrElse("")
        val flags = (if draft then " [draft]" else "") + (if pre then " [prerelease]" else "")
        println(s"$tag\t$pub$flags\t$name")
      }

  private def listTags(args: List[String]): Unit =
    val o             = parseRead(args)
    val (owner, repo) = splitRepo(o.repo.getOrElse(forgeUsage()))
    val arr = Try(getJson(s"${apiBase(o.base)}/repos/$owner/$repo/tags?limit=${o.limit}").arr)
      .getOrElse(die("expected a JSON array of tags"))
    if arr.isEmpty then println("(no tags)")
    else
      arr.foreach { t =>
        val name = t.obj.get("name").map(_.str).getOrElse("?")
        val sha  = t.obj.get("commit").flatMap(_.obj.get("sha")).map(_.str).getOrElse("").take(10)
        println(s"$name\t$sha")
      }

  private final case class CreateOpts(repo: Option[String], tag: Option[String], name: Option[String],
      body: Option[String], bodyFile: Option[String], prerelease: Boolean, draft: Boolean,
      target: Option[String], base: String)

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
        case flag :: _ if flag.startsWith("--") => die(s"unknown/incomplete flag '$flag'")
        case r :: t if o.repo.isEmpty  => go(t, o.copy(repo = Some(r)))
        case tg :: t if o.tag.isEmpty  => go(t, o.copy(tag = Some(tg)))
        case other :: _                => die(s"unexpected argument '$other'")
    val o          = go(args, CreateOpts(None, None, None, None, None, false, false, None, DefaultBase))
    val (owner, repo) = splitRepo(o.repo.getOrElse(forgeUsage()))
    val tag        = o.tag.getOrElse(forgeUsage())
    val tok = token.getOrElse(die(
      s"release-create needs a token — the HUMAN sets one of env ${TokenEnvNames.mkString(", ")}; it is deliberately\n" +
        "  NOT a flag, so the agent cannot self-authorize. Create one at Codeberg → Settings → Applications (write:repository)."))
    val url  = s"${apiBase(o.base)}/repos/$owner/$repo/releases"
    val host = hostOf(url)
    if !trustedHosts.contains(host) then die(
      s"refusing to send the token to untrusted host '$host'. Trusted: ${trustedHosts.toVector.sorted.mkString(", ")}.\n" +
        "  The HUMAN may extend the set via env TT_FORGE_HOSTS (comma-separated) — not a flag.")
    val bodyText = o.bodyFile match
      case Some(f) => Try(os.read(os.Path(f, os.pwd))).getOrElse(die(s"cannot read --body-file '$f'"))
      case None    => o.body.getOrElse("")
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

  private final case class EditOpts(repo: Option[String], tag: Option[String], name: Option[String],
      body: Option[String], bodyFile: Option[String], setPrerelease: Boolean, setDraft: Boolean, base: String)

  // release-edit — PATCH an EXISTING release: look it up by tag (unauth GET), then send ONLY the provided fields
  // (unspecified fields are left unchanged by the forge). Same effectful/token/trusted-host rules as release-create.
  private def releaseEdit(args: List[String]): Unit =
    @annotation.tailrec
    def go(rest: List[String], o: EditOpts): EditOpts =
      rest match
        case Nil                       => o
        case "--name" :: s :: t        => go(t, o.copy(name = Some(s)))
        case "--body" :: s :: t        => go(t, o.copy(body = Some(s)))
        case "--body-file" :: f :: t   => go(t, o.copy(bodyFile = Some(f)))
        case "--prerelease" :: t       => go(t, o.copy(setPrerelease = true))
        case "--draft" :: t            => go(t, o.copy(setDraft = true))
        case "--url" :: u :: t         => go(t, o.copy(base = u))
        case flag :: _ if flag.startsWith("--") => die(s"unknown/incomplete flag '$flag'")
        case r :: t if o.repo.isEmpty  => go(t, o.copy(repo = Some(r)))
        case tg :: t if o.tag.isEmpty  => go(t, o.copy(tag = Some(tg)))
        case other :: _                => die(s"unexpected argument '$other'")
    val o             = go(args, EditOpts(None, None, None, None, None, false, false, DefaultBase))
    val (owner, repo) = splitRepo(o.repo.getOrElse(forgeUsage()))
    val tag           = o.tag.getOrElse(forgeUsage())
    val tok = token.getOrElse(die(
      s"release-edit needs a token — the HUMAN sets one of env ${TokenEnvNames.mkString(", ")} (never a flag)."))
    val host = hostOf(o.base)
    if !trustedHosts.contains(host) then die(
      s"refusing to send the token to untrusted host '$host'. Trusted: ${trustedHosts.toVector.sorted.mkString(", ")}.")
    // build the PATCH payload with ONLY the provided fields (so unspecified fields stay unchanged)
    val payload = ujson.Obj()
    o.bodyFile match
      case Some(f) => payload("body") = Try(os.read(os.Path(f, os.pwd))).getOrElse(die(s"cannot read --body-file '$f'"))
      case None    => o.body.foreach(b => payload("body") = b)
    o.name.foreach(n => payload("name") = n)
    if o.setPrerelease then payload("prerelease") = true
    if o.setDraft then payload("draft") = true
    if payload.obj.isEmpty then die("nothing to edit — provide --body/--body-file, --name, --prerelease, or --draft")
    // look up the release id by tag (unauthenticated GET; getJson dies on non-200)
    val relJson = getJson(s"${apiBase(o.base)}/repos/$owner/$repo/releases/tags/$tag")
    val id      = Try(relJson.obj("id").num.toLong).getOrElse(die(s"no release id found for tag '$tag'"))
    val url     = s"${apiBase(o.base)}/repos/$owner/$repo/releases/$id"
    System.err.println(s"forge: [audit] PATCH $url  tag=$tag fields=${payload.obj.keys.mkString(",")}")
    val r = Try(requests.patch(url, data = ujson.write(payload),
      headers = Map("Content-Type" -> "application/json", "Authorization" -> s"token $tok"),
      check = false, readTimeout = 30000, connectTimeout = 10000)).getOrElse(die("request failed"))
    r.statusCode match
      case 200 =>
        val html = Try(ujson.read(r.text()).obj.get("html_url").map(_.str).getOrElse("")).getOrElse("")
        println(s"edited release $tag  $html")
      case 404 => die(s"release for tag '$tag' not found (404)")
      case c   => die(s"PATCH $url -> $c ${r.statusMessage}\n${r.text().take(500)}")
}

@main def forgeClient(args: String*): Unit = Forge.dispatch(args*)
