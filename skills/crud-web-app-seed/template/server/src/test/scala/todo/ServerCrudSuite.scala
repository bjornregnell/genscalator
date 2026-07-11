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
