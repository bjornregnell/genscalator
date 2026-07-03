//> using scala 3.8.4
//> using jvm 21
//> using dep com.lihaoyi::requests:0.9.3
//> using dep com.lihaoyi::ujson:4.4.3
//> using dep com.lihaoyi::os-lib:0.11.8

// forge — typed client for a Forgejo/Gitea forge (default: Codeberg). Replaces hand-curling the REST API
// (a dual-use `curl` carrying a token on the command line) with a narrow, effect-declared tool.
//   READ verbs (releases, tags) need NO auth (public repos) → safe to allowlist (`Bash(tt forge releases *)`).
//   The one EFFECTFUL verb (release-create) reads its token ONLY from the human-set env var CODEBERG_TOKEN
//   (or FORGE_TOKEN) — NEVER a flag — so the agent cannot self-authorize (same trust-boundary rule as
//   verify's TT_VERIFY_ALLOW / the configInArgsNotEnv PRD feature). It prints an [audit] line before acting,
//   and is deliberately NOT blanket-allowlistable (creating a release should stay a visible, confirmed op).
//   tt forge releases <owner>/<repo> [--url BASE] [--limit N]
//   tt forge tags     <owner>/<repo> [--url BASE] [--limit N]
//   tt forge release-create <owner>/<repo> <tag> [--name S] [--body S | --body-file F]
//                           [--prerelease] [--draft] [--target COMMITISH] [--url BASE]
//   BASE defaults to https://codeberg.org
import scala.util.Try

private val DefaultBase = "https://codeberg.org"

private def die(msg: String): Nothing = { System.err.println(s"forge: $msg"); sys.exit(2) }

private def forgeUsage(): Nothing = die(
  "usage:\n" +
    "  forge releases <owner>/<repo> [--url BASE] [--limit N]\n" +
    "  forge tags     <owner>/<repo> [--url BASE] [--limit N]\n" +
    "  forge release-create <owner>/<repo> <tag> [--name S] [--body S | --body-file F] [--prerelease] [--draft] [--target C] [--url BASE]\n" +
    "  BASE defaults to https://codeberg.org; release-create reads the token from env CODEBERG_TOKEN or FORGE_TOKEN (never a flag)."
)

private def token: Option[String] =
  List("CODEBERG_TOKEN", "FORGE_TOKEN").iterator.flatMap(sys.env.get).map(_.trim).find(_.nonEmpty)

private def splitRepo(s: String): (String, String) =
  s.split("/") match
    case Array(o, r) if o.nonEmpty && r.nonEmpty => (o, r)
    case _                                       => die(s"expected <owner>/<repo>, got '$s'")

private def apiBase(url: String): String = url.stripSuffix("/") + "/api/v1"

@main def forge(args: String*): Unit =
  args.toList match
    case "releases" :: rest       => listReleases(rest)
    case "tags" :: rest           => listTags(rest)
    case "release-create" :: rest => releaseCreate(rest)
    case _                        => forgeUsage()

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
    "release-create needs a token — the HUMAN sets env CODEBERG_TOKEN (or FORGE_TOKEN); it is deliberately\n" +
      "  NOT a flag, so the agent cannot self-authorize. Create one at Codeberg → Settings → Applications (write:repository)."))
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
  val url = s"${apiBase(o.base)}/repos/$owner/$repo/releases"
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
