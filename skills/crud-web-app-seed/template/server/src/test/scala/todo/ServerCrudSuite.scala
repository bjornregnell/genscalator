package todo.server

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import todo.Todo

// A beginner-friendly END-TO-END test: it starts the REAL server on a free port, then drives the CRUD API over HTTP
// with the JDK's own HttpClient (exactly like the browser client does). This shows how to test a whole web backend
// without a browser. Run with `sbt test`.
class ServerCrudSuite extends munit.FunSuite:

  test("the full todo lifecycle works over HTTP: create, list, toggle, delete") {
    val server = Main.start(0) // port 0 = let the OS pick a free port
    val port = server.getAddress.getPort
    val base = s"http://localhost:$port"
    val http = HttpClient.newHttpClient()

    def call(method: String, path: String, body: String = ""): (Int, String) =
      val req = HttpRequest
        .newBuilder(URI.create(base + path))
        .header("Content-Type", "application/json")
        .method(method, HttpRequest.BodyPublishers.ofString(body))
        .build()
      val res = http.send(req, HttpResponse.BodyHandlers.ofString())
      (res.statusCode, res.body)

    try
      // starts empty
      assertEquals(call("GET", "/api/todos"), (200, "[]"))

      // create one
      val (createCode, createBody) = call("POST", "/api/todos", """{"title":"learn Scala"}""")
      assertEquals(createCode, 201)
      val created = Todo.parse(createBody)
      assertEquals(created.title, "learn Scala")
      assertEquals(created.done, false)

      // it shows up in the list
      assertEquals(Todo.parseList(call("GET", "/api/todos")._2), List(created))

      // toggle it done
      val (toggleCode, toggleBody) = call("PUT", s"/api/todos/${created.id}")
      assertEquals(toggleCode, 200)
      assertEquals(Todo.parse(toggleBody).done, true)

      // delete it
      assertEquals(call("DELETE", s"/api/todos/${created.id}")._1, 204)

      // empty again
      assertEquals(call("GET", "/api/todos")._2, "[]")
    finally server.stop(0)
  }

  // REGRESSION TEST for the client-JS path. This seed served a 404 for /main.js after the Scala 3.9.0 bump:
  // the path named ONE version directory (`client/target/scala-3/...`), but sbt names that directory after the
  // Scala version it resolved, so the linked client sat in `scala-3.9.0/` and the server looked elsewhere. The
  // client had built perfectly well. Nothing tested this path, which is why the defect shipped.
  //
  // The check below finds the linked client by WALKING client/target, deliberately not by re-using the server's
  // own lookup: a test that repeats the code it checks can agree with it and still be wrong together.
  test("GET /main.js serves the linked client whatever the Scala version directory is called") {
    val targetDir = java.nio.file.Path.of("client", "target")
    val linked: Option[java.nio.file.Path] =
      if !java.nio.file.Files.isDirectory(targetDir) then None
      else
        val walk = java.nio.file.Files.walk(targetDir, 3)
        try
          walk.filter(java.nio.file.Files.isRegularFile(_))
            .filter(_.endsWith(java.nio.file.Path.of("todo-client-fastopt", "main.js")))
            .findFirst()
            .map[Option[java.nio.file.Path]](p => Some(p))
            .orElse(None)
        finally walk.close()

    val server = Main.start(0)
    val port = server.getAddress.getPort
    val http = HttpClient.newHttpClient()
    try
      val req = HttpRequest.newBuilder(URI.create(s"http://localhost:$port/main.js")).GET().build()
      val res = http.send(req, HttpResponse.BodyHandlers.ofString())
      linked match
        case Some(p) =>
          // A linked client exists on disk, so the server MUST find it. This is the assertion that fails
          // when the version directory is renamed by a bump.
          assertEquals(clue(res.statusCode), 200, s"a linked client exists at $p but the server did not serve it")
          assert(clue(res.body.length) > 1000, "served /main.js looks too small to be the linked client")
        case None =>
          // No client linked yet (a fresh checkout, or CI without `sbt client/fastLinkJS`): the 404 must still
          // tell the reader what to run.
          assertEquals(clue(res.statusCode), 404)
          assert(clue(res.body).contains("sbt client/fastLinkJS"))
    finally server.stop(0)
  }
