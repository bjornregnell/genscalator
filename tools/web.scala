//> using scala 3.8.4
//> using jvm 21
//> using dep com.lihaoyi::requests:0.9.3

// web — safe, READ-ONLY HTTP for agents. Replaces the dual-use `curl` reflex with a narrow, allowlistable
// tool: GET only (no POST/PUT/upload), NO credential/cookie headers ever, a response size cap, and an
// optional --host allowlist. Because it can ONLY fetch-and-print, `Bash(tt web get *)` is safe to
// blanket-allow where a bare `curl *` allowlist would expose exfiltration (`curl -d @secret …`), RCE
// (`curl … | sh`), and credential-header leaks. (SSRF-read of internal hosts is the residual risk; lock it
// down with --host.)
//   tt web get <url> [--host <host>]... [--max-bytes N] [--status]
//     --host H      restrict to these host(s), repeatable (default: any — GET-only + no-creds already caps risk)
//     --max-bytes N truncate the printed body at N bytes (default 5000000)
//     --status      also print HTTP status + content-type to stderr
import scala.util.{Try, Success, Failure}

private val DefaultMaxBytes = 5_000_000

private def webUsage(): Nothing =
  System.err.println(
    "web: usage: web get <url> [--host <host>]... [--max-bytes N] [--status]\n" +
      "  safe read-only HTTP: GET only, no credential headers, size-capped."
  )
  sys.exit(2)

private final case class WebOpts(url: Option[String], hosts: Vector[String], maxBytes: Int, status: Boolean)

@main def web(args: String*): Unit =
  args.toList match
    case "get" :: rest => webGet(rest)
    case _             => webUsage()

private def webGet(args: List[String]): Unit =
  @annotation.tailrec
  def parse(rest: List[String], o: WebOpts): WebOpts =
    rest match
      case Nil                     => o
      case "--host" :: h :: t      => parse(t, o.copy(hosts = o.hosts :+ h))
      case "--status" :: t         => parse(t, o.copy(status = true))
      case "--max-bytes" :: n :: t =>
        n.toIntOption match
          case Some(v) if v > 0 => parse(t, o.copy(maxBytes = v))
          case _ => System.err.println(s"web: --max-bytes needs a positive integer, got '$n'"); sys.exit(2)
      case flag :: _ if flag.startsWith("--")  => System.err.println(s"web: unknown/incomplete flag '$flag'"); sys.exit(2)
      case u :: t if o.url.isEmpty              => parse(t, o.copy(url = Some(u)))
      case other :: _                           => System.err.println(s"web: unexpected argument '$other'"); sys.exit(2)
  val o   = parse(args, WebOpts(None, Vector.empty, DefaultMaxBytes, false))
  val url = o.url.getOrElse(webUsage())

  val lower = url.toLowerCase
  if !(lower.startsWith("http://") || lower.startsWith("https://")) then
    System.err.println(s"web: only http(s) URLs allowed, got '$url'"); sys.exit(2)

  val host = Try(Option(java.net.URI(url).getHost)).toOption.flatten.getOrElse("")
  if o.hosts.nonEmpty && !o.hosts.contains(host) then
    System.err.println(s"web: host '$host' not in --host allowlist [${o.hosts.mkString(", ")}]"); sys.exit(2)

  // GET only. NO credential/cookie headers are ever attached — this tool cannot carry secrets outward.
  Try(requests.get(url, check = false, readTimeout = 30000, connectTimeout = 10000)) match
    case Failure(e) => System.err.println(s"web: request failed: ${e.getMessage}"); sys.exit(2)
    case Success(r) =>
      if o.status then
        val ct = r.headers.getOrElse("content-type", Nil).mkString(";")
        System.err.println(s"web: ${r.statusCode} ${r.statusMessage}  $ct")
      val bytes = r.bytes
      val out   = if bytes.length > o.maxBytes then bytes.take(o.maxBytes) else bytes
      System.out.write(out); System.out.flush()
      if bytes.length > o.maxBytes then
        System.err.println(s"web: [truncated at ${o.maxBytes} bytes of ${bytes.length}]")
