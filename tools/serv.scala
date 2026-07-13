//> using scala 3.8.4
//> using jvm 21

// serv — a tiny local static-file preview server (SM020). EFFECTFUL: opens a socket on LOOPBACK ONLY
// (127.0.0.1) and serves files from a directory until Ctrl-C. Zero external deps (JDK's built-in
// com.sun.net.httpserver). The audited replacement for `python3 -m http.server` when previewing a generated
// site (e.g. `tt ssg` output) before deploy. NEVER binds 0.0.0.0 — loopback only, by design.
//   tt serv <dir> [--port N]      serve <dir> at http://127.0.0.1:N/  (default N=8000)
//   (--localhost is accepted and ignored — the bind is ALWAYS loopback)
//
// Helpers live inside `object Serv` (so top-level names don't collide when the toolbox compiles as one unit);
// only the @main is top-level. The pure, security-critical bits (path resolution + traversal guard,
// content-type) are separate methods so they are unit-testable without opening a socket. See skills/scala-style §1.
import java.nio.file.{Files, Path}
import java.net.InetSocketAddress
import com.sun.net.httpserver.{HttpServer, HttpExchange, HttpHandler}

object Serv:
  val DefaultPort = 8000

  /** Content-Type for a filename by extension. Text types carry `; charset=utf-8`; binaries do not. */
  def contentType(name: String): String =
    val lower = name.toLowerCase
    def txt(t: String) = s"$t; charset=utf-8"
    if lower.endsWith(".html") || lower.endsWith(".htm") then txt("text/html")
    else if lower.endsWith(".css") then txt("text/css")
    else if lower.endsWith(".js") || lower.endsWith(".mjs") then txt("text/javascript")
    else if lower.endsWith(".json") then txt("application/json")
    else if lower.endsWith(".svg") then txt("image/svg+xml")
    else if lower.endsWith(".md") then txt("text/markdown")
    else if lower.endsWith(".txt") then txt("text/plain")
    else if lower.endsWith(".xml") then txt("application/xml")
    else if lower.endsWith(".png") then "image/png"
    else if lower.endsWith(".jpg") || lower.endsWith(".jpeg") then "image/jpeg"
    else if lower.endsWith(".gif") then "image/gif"
    else if lower.endsWith(".ico") then "image/x-icon"
    else if lower.endsWith(".woff2") then "font/woff2"
    else "application/octet-stream"

  /** Resolve a request URL path to a real file UNDER `root`, or None if it would escape root
    * (path-traversal guard) — a directory resolves to its `index.html`. Existence is the caller's concern.
    * PURE apart from a directory probe; unit-testable with a temp dir. The security invariant: the returned
    * path always starts with the normalized absolute root, so `..`/encoded-`..`/leading-`/` cannot escape. */
  def resolveSafe(root: Path, urlPathRaw: String): Option[Path] =
    val rootAbs = root.toAbsolutePath.normalize
    val noQuery = { val q = urlPathRaw.indexOf('?'); if q >= 0 then urlPathRaw.substring(0, q) else urlPathRaw }
    val decoded = java.net.URLDecoder.decode(noQuery, "UTF-8")
    val rel = decoded.stripPrefix("/")
    val base = if rel.isEmpty then rootAbs else rootAbs.resolve(rel).normalize
    val candidate = if Files.isDirectory(base) then base.resolve("index.html").normalize else base
    if candidate.startsWith(rootAbs) then Some(candidate) else None

  private def respond(ex: HttpExchange, code: Int, ctype: String, body: Array[Byte], headOnly: Boolean): Unit =
    ex.getResponseHeaders.set("Content-Type", ctype)
    val len = body.length.toLong
    ex.sendResponseHeaders(code, if headOnly || len == 0 then -1 else len)
    if !headOnly && len > 0 then
      val os = ex.getResponseBody; try os.write(body) finally os.close()

  private def handler(root: Path): HttpHandler = (ex: HttpExchange) =>
    try
      val method = ex.getRequestMethod
      val path = ex.getRequestURI.getRawPath // RAW (undecoded); resolveSafe decodes exactly once (no double-decode)
      if method != "GET" && method != "HEAD" then
        respond(ex, 405, "text/plain; charset=utf-8", "405 method not allowed".getBytes("UTF-8"), false)
      else
        val head = method == "HEAD"
        resolveSafe(root, path) match
          case None =>
            System.err.println(s"serv: 403 $path"); respond(ex, 403, "text/plain; charset=utf-8", "403 forbidden".getBytes("UTF-8"), head)
          case Some(file) if !Files.isRegularFile(file) =>
            System.err.println(s"serv: 404 $path"); respond(ex, 404, "text/plain; charset=utf-8", s"404 not found: $path".getBytes("UTF-8"), head)
          case Some(file) =>
            val bytes = Files.readAllBytes(file)
            System.err.println(s"serv: 200 $path (${bytes.length} B)")
            respond(ex, 200, contentType(file.getFileName.toString), bytes, head)
    catch
      case e: Throwable =>
        try respond(ex, 500, "text/plain; charset=utf-8", s"500 ${e.getMessage}".getBytes("UTF-8"), false) catch case _: Throwable => ()
    finally ex.close()

  /** Create + start a loopback-only HTTP server serving `root`, and return it (NON-blocking; the caller stops
    * it or blocks). `port` 0 asks the OS for an ephemeral port (used by tests). */
  def start(root: Path, port: Int): HttpServer =
    val server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0)
    server.createContext("/", handler(root))
    server.setExecutor(null)
    server.start()
    server

  private val Help: String =
    """tt serv — local static-file preview server (loopback only, zero-dep)
      |
      |Serves a directory at http://127.0.0.1:<port>/ until Ctrl-C — the audited replacement for
      |`python3 -m http.server` when previewing a generated site (e.g. `tt ssg` output) before
      |deploy. Zero external deps: it uses the JDK's built-in HTTP server.
      |
      |Usage:
      |  serv <dir> [--port N]      serve <dir> at http://127.0.0.1:N/  (Ctrl-C to stop)
      |
      |Flags:
      |  --port N       port to listen on, 1..65535 (default 8000)
      |  --localhost    accepted and ignored — the bind is ALWAYS loopback anyway
      |
      |Safety: always binds 127.0.0.1 (never 0.0.0.0 — nothing is exposed off the box). GET and
      |HEAD only. A directory serves its index.html. A path-traversal guard keeps every served
      |path under <dir>: `..`, encoded `..`, and a leading `/` cannot escape (they get 403).
      |
      |Examples:
      |  tt serv site                     # serve ./site at http://127.0.0.1:8000/
      |  tt serv tmp/site --port 8137     # pick a port, then open the printed URL
      |
      |Full reference: tools/README.md""".stripMargin

  private val Usage =
    """serv — local static-file preview server (loopback only, zero-dep)
      |  serv <dir> [--port N]      serve <dir> at http://127.0.0.1:N/  (default N=8000)""".stripMargin

  /** Parse `<dir> [--port N]` (and a tolerated, ignored `--localhost`). Pure + testable. */
  def parseArgs(args: List[String]): Either[String, (String, Int)] =
    def loop(rest: List[String], dir: Option[String], port: Int): Either[String, (String, Int)] =
      rest match
        case Nil                       => dir.map(d => Right((d, port))).getOrElse(Left("missing <dir>"))
        case "--port" :: n :: t        =>
          n.toIntOption.filter(p => p > 0 && p <= 65535) match
            case Some(p) => loop(t, dir, p)
            case None    => Left(s"--port needs an integer 1..65535, got: $n")
        case "--port" :: Nil           => Left("--port needs a value")
        case "--localhost" :: t        => loop(t, dir, port) // accepted + ignored: bind is always loopback
        case flag :: _ if flag.startsWith("--") => Left(s"unknown option: $flag")
        case d :: t                    => if dir.isDefined then Left(s"unexpected extra argument: $d") else loop(t, Some(d), port)
    loop(args, None, DefaultPort)

  def dispatch(args: String*): Unit =
    if args.contains("--help") || args.contains("-h") then { println(Help); sys.exit(0) }
    parseArgs(args.toList) match
      case Left(err) => System.err.println(s"serv: $err"); System.err.println(Usage); sys.exit(2)
      case Right((dir, port)) =>
        val root = Path.of(dir).toAbsolutePath.normalize
        if !Files.isDirectory(root) then
          System.err.println(s"serv: not a directory: $dir (resolved: $root)"); sys.exit(2)
        val server = start(root, port)
        System.err.println(s"serv: serving $root at http://127.0.0.1:${server.getAddress.getPort}/  (loopback only; Ctrl-C to stop)")
        java.util.concurrent.CountDownLatch(1).await() // block until the process is killed

@main def serveStaticFiles(args: String*): Unit = Serv.dispatch(args*)
