//> using scala 3.8.4
//> using dep org.scalameta::munit::1.3.3
//> using dep com.lihaoyi::os-lib:0.11.8

// Unit tests for the serv static-file server (serv.scala). The server loop itself opens a socket and blocks,
// so it is NOT unit-tested here; instead we test the PURE, security-critical helpers that decide what gets
// served: content-type mapping, the path-resolution + traversal guard (resolveSafe), and CLI parsing. These
// sit on the toolbox MAIN scope (the @main does not clash) and run in-process — fast + hermetic.
//   run from the genscalator root:  scala-cli test tools
import java.nio.file.Path

class ServSuite extends munit.FunSuite:

  // --- contentType ---
  test("contentType maps common text types with charset and binaries without") {
    assertEquals(Serv.contentType("page.html"), "text/html; charset=utf-8")
    assertEquals(Serv.contentType("style.CSS"), "text/css; charset=utf-8") // case-insensitive
    assertEquals(Serv.contentType("app.js"), "text/javascript; charset=utf-8")
    assertEquals(Serv.contentType("pic.png"), "image/png")
    assertEquals(Serv.contentType("logo.svg"), "image/svg+xml; charset=utf-8")
    assertEquals(Serv.contentType("data.bin"), "application/octet-stream")
    assertEquals(Serv.contentType("noext"), "application/octet-stream")
  }

  // --- resolveSafe: correct resolution + the traversal guard (the security boundary) ---
  private def withRoot(body: (os.Path, Path) => Unit): Unit =
    val root = os.temp.dir()
    try
      os.write(root / "index.html", "root index")
      os.write(root / "a.txt", "aaa")
      os.makeDir(root / "sub")
      os.write(root / "sub" / "index.html", "sub index")
      body(root, Path.of(root.toString))
    finally os.remove.all(root)

  test("resolveSafe serves a file, maps a directory to its index.html, and strips the query string") {
    withRoot { (osRoot, root) =>
      assertEquals(Serv.resolveSafe(root, "/").map(_.getFileName.toString), Some("index.html"))
      assert(Serv.resolveSafe(root, "/a.txt").exists(_.endsWith("a.txt")))
      assert(Serv.resolveSafe(root, "/sub").exists(_.endsWith(Path.of("sub", "index.html"))))    // dir -> index
      assert(Serv.resolveSafe(root, "/sub/").exists(_.endsWith(Path.of("sub", "index.html"))))
      assert(Serv.resolveSafe(root, "/a.txt?v=9").exists(_.endsWith("a.txt")))                    // query dropped
    }
  }

  test("resolveSafe resolves a not-yet-existing path but keeps it under root (existence is the handler's job)") {
    withRoot { (_, root) =>
      val r = Serv.resolveSafe(root, "/does-not-exist.html")
      assert(r.isDefined)
      assert(r.get.startsWith(root.toAbsolutePath.normalize))
    }
  }

  test("resolveSafe REFUSES path traversal: `..`, encoded `..`, cannot escape root") {
    withRoot { (_, root) =>
      assertEquals(Serv.resolveSafe(root, "/../etc/passwd"), None)
      assertEquals(Serv.resolveSafe(root, "/../../etc/passwd"), None)
      assertEquals(Serv.resolveSafe(root, "/..%2f..%2fetc%2fpasswd"), None) // URL-encoded ../../
      assertEquals(Serv.resolveSafe(root, "/sub/../../secret"), None)
    }
  }

  test("resolveSafe: a leading-slash absolute-looking path stays under root, it does not escape") {
    withRoot { (_, root) =>
      // "/etc/passwd" becomes root/etc/passwd (a 404 later), NOT the system file
      assert(Serv.resolveSafe(root, "/etc/passwd").exists(_.startsWith(root.toAbsolutePath.normalize)))
    }
  }

  // --- CLI parsing ---
  test("parseArgs: dir with default port, explicit --port, and ignored --localhost") {
    assertEquals(Serv.parseArgs(List("site")), Right(("site", 8000)))
    assertEquals(Serv.parseArgs(List("site", "--port", "9001")), Right(("site", 9001)))
    assertEquals(Serv.parseArgs(List("--localhost", "site", "--port", "3000")), Right(("site", 3000)))
  }
  test("parseArgs rejects a bad/missing port, a missing dir, unknown flags, and an extra arg") {
    assert(Serv.parseArgs(List("site", "--port", "0")).isLeft)
    assert(Serv.parseArgs(List("site", "--port", "99999")).isLeft)
    assert(Serv.parseArgs(List("site", "--port", "x")).isLeft)
    assert(Serv.parseArgs(List("site", "--port")).isLeft)
    assert(Serv.parseArgs(List.empty).isLeft)
    assert(Serv.parseArgs(List("site", "--bogus")).isLeft)
    assert(Serv.parseArgs(List("site", "extra")).isLeft)
  }

  // --- end-to-end: a real server on an ephemeral loopback port actually serves + refuses traversal ---
  private def httpGet(url: String): (Int, String) =
    val con = java.net.URI.create(url).toURL.openConnection().asInstanceOf[java.net.HttpURLConnection]
    con.setRequestMethod("GET")
    try
      val code = con.getResponseCode
      val in = if code < 400 then con.getInputStream else con.getErrorStream
      val body = if in == null then "" else String(in.readAllBytes(), "UTF-8")
      (code, body)
    finally con.disconnect()

  test("end-to-end: the running server serves files over loopback and refuses encoded traversal") {
    withRoot { (_, root) =>
      val server = Serv.start(root, 0) // 0 = ephemeral OS-assigned port
      val port = server.getAddress.getPort
      try
        val (rootCode, rootBody) = httpGet(s"http://127.0.0.1:$port/")
        assertEquals(rootCode, 200)
        assert(clue(rootBody).contains("root index"), "index.html not served at /")

        val (dirCode, dirBody) = httpGet(s"http://127.0.0.1:$port/sub/") // dir -> index.html
        assertEquals(dirCode, 200)
        assert(clue(dirBody).contains("sub index"))

        val (missCode, _) = httpGet(s"http://127.0.0.1:$port/nope.html")
        assertEquals(missCode, 404)

        // encoded ../../ must NOT escape root (kept percent-encoded by the client, decoded once server-side)
        val (escCode, _) = httpGet(s"http://127.0.0.1:$port/..%2f..%2fetc%2fpasswd")
        assert(clue(escCode) == 403 || escCode == 404, s"traversal not refused (got $escCode)")
      finally server.stop(0)
    }
  }
