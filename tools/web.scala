//> using scala 3.8.4
//> using jvm 21
//> using dep com.lihaoyi::requests:0.9.3

// web — safe, READ-ONLY HTTP for agents. Replaces the dual-use `curl` reflex with a narrow, allowlistable
// tool: GET only (no POST/PUT/upload), NO credential/cookie headers ever, a response size cap, and an
// optional --host allowlist. Because it can ONLY fetch-and-print, `Bash(tt web get *)` is safe to
// blanket-allow where a bare `curl *` allowlist would expose exfiltration (`curl -d @secret …`), RCE
// (`curl … | sh`), and credential-header leaks. (SSRF-read of internal hosts is the residual risk; lock it
// down with --host.)
//   tt web get <url> [--host <host>]... [--max-bytes N] [--status] [--trace]
//     --host H      restrict to these host(s), repeatable (default: any — GET-only + no-creds already caps risk)
//     --max-bytes N truncate the printed body at N bytes (default 5000000)
//     --status      also print HTTP status + content-type to stderr
//     --trace       follow redirects HEAD-only, printing each hop's status + Location (the safe `curl -sIL`)
import scala.util.{Try, Success, Failure}

// Helpers scoped in this object so top-level names don't collide with the other tools when the toolbox
// compiles together. Only the @main entry is top-level. See skills/scala-style.
object Web {
  private val DefaultMaxBytes = 5_000_000

  private val Help: String =
    """tt web — safe, read-only HTTP fetch (the audited `curl` replacement)
      |
      |Fetches a URL with GET and prints the response body to stdout. It can ONLY fetch-and-print:
      |no POST/PUT/upload, NO credential/cookie headers ever, and the printed body is size-capped —
      |so it cannot exfiltrate secrets or pipe code to a shell the way a bare `curl` could. The
      |residual risk is only SSRF-read of internal hosts — lock that down with --host.
      |
      |Usage:
      |  web get <url> [flags]      fetch <url> (http/https only) and print the body to stdout
      |
      |Flags:
      |  --host H         restrict fetching to host H; repeatable for several hosts
      |                   (default: any host — GET-only + no-credentials already caps the risk)
      |  --max-bytes N    truncate the printed body at N bytes (default 5000000 = 5 MB)
      |  --status         also print the HTTP status + content-type to stderr
      |  --trace          follow redirects HEAD-only and print each hop's status + Location (like curl -sIL);
      |                   no body, hop-capped, stops before an off-allowlist redirect
      |
      |Examples:
      |  tt web get http://genscalator.ai --trace                 # the redirect chain, hop by hop
      |  tt web get https://codeberg.org/api/v1/repos/o/r/tags --status
      |  tt web get https://example.org/big.json --max-bytes 20000
      |  tt web get https://codeberg.org/x --host codeberg.org    # refuse any other host
      |
      |Full reference: tools/README.md""".stripMargin

  private def webUsage(): Nothing =
    System.err.println(
      "web: usage: web get <url> [--host <host>]... [--max-bytes N] [--status] [--trace]\n" +
        "  safe read-only HTTP: GET only, no credential headers, size-capped."
    )
    sys.exit(2)

  private final case class WebOpts(url: Option[String], hosts: Vector[String], maxBytes: Int, status: Boolean, trace: Boolean)

  def dispatch(args: String*): Unit =
    if args.contains("--help") || args.contains("-h") then { println(Help); sys.exit(0) }
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
        case "--trace" :: t          => parse(t, o.copy(trace = true))
        case "--max-bytes" :: n :: t =>
          n.toIntOption match
            case Some(v) if v > 0 => parse(t, o.copy(maxBytes = v))
            case _ => System.err.println(s"web: --max-bytes needs a positive integer, got '$n'"); sys.exit(2)
        case flag :: _ if flag.startsWith("--")  => System.err.println(s"web: unknown/incomplete flag '$flag'"); sys.exit(2)
        case u :: t if o.url.isEmpty              => parse(t, o.copy(url = Some(u)))
        case other :: _                           => System.err.println(s"web: unexpected argument '$other'"); sys.exit(2)
    val o   = parse(args, WebOpts(None, Vector.empty, DefaultMaxBytes, false, false))
    val url = o.url.getOrElse(webUsage())

    val lower = url.toLowerCase
    if !(lower.startsWith("http://") || lower.startsWith("https://")) then
      System.err.println(s"web: only http(s) URLs allowed, got '$url'"); sys.exit(2)

    val host = Try(Option(java.net.URI(url).getHost)).toOption.flatten.getOrElse("")
    if o.hosts.nonEmpty && !o.hosts.contains(host) then
      System.err.println(s"web: host '$host' not in --host allowlist [${o.hosts.mkString(", ")}]"); sys.exit(2)

    if o.trace then { webTrace(url, o.hosts); return }

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

  /** Follow redirects one hop at a time (the `curl -sIL` equivalent): HEAD each hop, print its status +
    * Location, then follow. Read-only + no credentials like `get`; hop-capped at 10; if --host is set it
    * stops BEFORE fetching an off-allowlist host. No body is printed (headers only). */
  private def webTrace(url0: String, hosts: Vector[String]): Unit =
    val MaxHops = 10
    var url = url0
    var hop = 0
    var go  = true
    while go do
      val host = Try(Option(java.net.URI(url).getHost)).toOption.flatten.getOrElse("")
      if hosts.nonEmpty && !hosts.contains(host) then
        System.err.println(s"web: stop — next host '$host' not in --host allowlist [${hosts.mkString(", ")}]")
        go = false
      else
        Try(requests.head(url, check = false, maxRedirects = 0, readTimeout = 30000, connectTimeout = 10000)) match
          case Failure(e) =>
            System.err.println(s"web: request failed at $url: ${e.getMessage}"); sys.exit(2)
          case Success(r) =>
            val loc    = r.headers.get("location").flatMap(_.headOption)
            val ct     = r.headers.getOrElse("content-type", Nil).mkString(";")
            val locStr = loc.fold("")(l => s"  ->  $l")
            val ctStr  = if ct.nonEmpty then s"   [$ct]" else ""
            println(url)
            println(s"  ${r.statusCode} ${r.statusMessage}$locStr$ctStr")
            loc match
              case Some(l) if r.statusCode >= 300 && r.statusCode < 400 =>
                hop += 1
                if hop > MaxHops then
                  System.err.println(s"web: too many redirects (> $MaxHops), stopping"); go = false
                else url = Try(java.net.URI(url).resolve(l).toString).getOrElse(l)
              case _ => go = false
}

@main def webFetch(args: String*): Unit = Web.dispatch(args*)
