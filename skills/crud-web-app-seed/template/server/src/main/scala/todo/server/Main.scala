package todo.server

import com.sun.net.httpserver.{HttpServer, HttpExchange, HttpHandler}
import java.net.InetSocketAddress
import todo.{Todo, Json}

// JDK-only HTTP server (no libraries) for the todo CRUD API + the static client page. In-memory store, thread-safe.
// Direct common style. Reuses the shared `common` datamodel + JSON codec so the wire format matches the client.
object Main:
  private val store = scala.collection.mutable.LinkedHashMap[Int, Todo]()
  private var nextId = 1
  private val lock = new Object

  def list(): List[Todo] = lock.synchronized(store.values.toList)

  def create(title: String): Todo = lock.synchronized:
    val t = Todo(nextId, title, done = false)
    store(nextId) = t
    nextId += 1
    t

  def toggle(id: Int): Option[Todo] = lock.synchronized:
    store.get(id).map: t =>
      val u = t.copy(done = !t.done)
      store(id) = u
      u

  def delete(id: Int): Boolean = lock.synchronized(store.remove(id).isDefined)

  // Start the server and RETURN it, so callers (e.g. the test suite) can `server.stop(0)`.
  def start(port: Int): HttpServer =
    val server = HttpServer.create(InetSocketAddress("localhost", port), 0)
    server.createContext("/api/todos", ApiHandler)
    server.createContext("/", StaticHandler)
    server.setExecutor(null)
    server.start()
    println(s"todo-seed server on http://localhost:${server.getAddress.getPort}")
    server

  private def respond(ex: HttpExchange, code: Int, body: String, ctype: String = "application/json"): Unit =
    val bytes = body.getBytes("UTF-8")
    ex.getResponseHeaders.add("Content-Type", ctype)
    ex.sendResponseHeaders(code, if bytes.isEmpty then -1 else bytes.length.toLong)
    if bytes.nonEmpty then
      val os = ex.getResponseBody
      try os.write(bytes) finally os.close()

  private def body(ex: HttpExchange): String = String(ex.getRequestBody.readAllBytes(), "UTF-8")

  object ApiHandler extends HttpHandler:
    def handle(ex: HttpExchange): Unit =
      val idPart = ex.getRequestURI.getPath.stripPrefix("/api/todos").stripPrefix("/")
      (ex.getRequestMethod, idPart) match
        case ("GET", "")  => respond(ex, 200, Todo.listToJson(list()))
        case ("POST", "") => respond(ex, 201, Todo.toJson(create(Json.parse(body(ex)).obj("title").str)))
        case ("PUT", id) if id.toIntOption.isDefined =>
          toggle(id.toInt) match
            case Some(t) => respond(ex, 200, Todo.toJson(t))
            case None    => respond(ex, 404, """{"error":"not found"}""")
        case ("DELETE", id) if id.toIntOption.isDefined =>
          if delete(id.toInt) then respond(ex, 204, "") else respond(ex, 404, """{"error":"not found"}""")
        case _ => respond(ex, 405, """{"error":"method not allowed"}""")

  // The Scala.js client output, produced by `sbt client/fastLinkJS`. Override with env TODO_CLIENT_JS if your Scala
  // version dir differs. This is the one bit a newcomer may need to adjust; the README says so.
  private val clientJs =
    sys.env.getOrElse("TODO_CLIENT_JS", "client/target/scala-3.9.0-RC1/todo-client-fastopt/main.js")

  object StaticHandler extends HttpHandler:
    def handle(ex: HttpExchange): Unit =
      ex.getRequestURI.getPath match
        case "/main.js" =>
          val f = java.nio.file.Path.of(clientJs)
          if java.nio.file.Files.exists(f) then
            respond(ex, 200, java.nio.file.Files.readString(f), "application/javascript")
          else
            respond(ex, 404, s"// not found: $clientJs — run `sbt client/fastLinkJS` first (or set TODO_CLIENT_JS)",
              "application/javascript")
        case _ =>
          val html =
            """<!doctype html><html><head><meta charset="utf-8"><title>Todo seed</title></head>
              |<body><div id="app"></div><script src="/main.js"></script></body></html>""".stripMargin
          respond(ex, 200, html, "text/html; charset=utf-8")

@main def run(args: String*): Unit =
  todo.server.Main.start(args.headOption.flatMap(_.toIntOption).getOrElse(8080))
