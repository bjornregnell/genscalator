//> using scala 3.8.4
//> using dep org.scalameta::munit::1.3.3

// Unit tests for box.scala's SM181 LOCAL shapes — the PURE core only: /proc stat/meminfo parsing,
// the human formatters, and the DevServer fingerprints. The bloop fingerprint delegates to the
// ONE-home signature BoxStats.isBloopCmdline (bloop-and-not-metals), so the Metals exclusion is
// regression-pinned HERE too: killing the editor's language server is the one over-match that
// genuinely hurts. No process is touched in these tests; the health/kill DRIVERS stay deliberately
// un-run (kill ship/allowlist is BR-gated per the SM181 pin).

class BoxLocalSuite extends munit.FunSuite:

  test("parseStat: comm with spaces/parens parsed via the LAST ')', tick fields land right") {
    val stat = "1234 (Web (Content)) S 1 1234 1234 0 -1 4194560 100 0 0 0 750 250 3 1 20 0 4 0 98765 123456789 5000 18446744073709551615"
    val p = Box.parseStat(stat).get
    assertEquals(p.comm, "Web (Content)")
    assertEquals(p.cpuTicks, 1000L) // utime 750 + stime 250
    assertEquals(p.startTicks, 98765L)
  }

  test("parseStat: garbage and truncated lines yield None, not a crash") {
    assertEquals(Box.parseStat(""), None)
    assertEquals(Box.parseStat("no parens here"), None)
    assertEquals(Box.parseStat("1 (x) S 1 2 3"), None)
  }

  test("parseMeminfo: the four keys picked by name; missing keys default to 0") {
    val lines = Vector(
      "MemTotal:       32000000 kB",
      "MemFree:         1000000 kB",
      "MemAvailable:   12345678 kB",
      "SwapTotal:       2097148 kB",
      "SwapFree:        2000000 kB")
    val m = Box.parseMeminfo(lines)
    assertEquals(m.totalKb, 32000000L)
    assertEquals(m.availKb, 12345678L)
    assertEquals(m.swapTotalKb, 2097148L)
    assertEquals(m.swapFreeKb, 2000000L)
    assertEquals(Box.parseMeminfo(Vector.empty).totalKb, 0L)
  }

  test("fmtKb: G with one decimal, whole M, raw K — decimal DOT regardless of locale") {
    assertEquals(Box.fmtKb(11431936L), "10.9G")
    assertEquals(Box.fmtKb(1048576L), "1.0G")
    assertEquals(Box.fmtKb(524288L), "512M")
    assertEquals(Box.fmtKb(500L), "500K")
  }

  test("fmtElapsed: compact two-unit ages") {
    assertEquals(Box.fmtElapsed(45L), "45s")
    assertEquals(Box.fmtElapsed(125L), "2m5s")
    assertEquals(Box.fmtElapsed(3L * 3600 + 12 * 60), "3h12m")
    assertEquals(Box.fmtElapsed(2L * 86400 + 3 * 3600), "2d3h")
  }

  test("DevServer.parse: exactly the three enum names, case-sensitive, nothing else") {
    assertEquals(Box.DevServer.parse("bloop"), Some(Box.DevServer.Bloop))
    assertEquals(Box.DevServer.parse("sbt"), Some(Box.DevServer.Sbt))
    assertEquals(Box.DevServer.parse("scala-cli"), Some(Box.DevServer.ScalaCli))
    assertEquals(Box.DevServer.parse("Bloop"), None)
    assertEquals(Box.DevServer.parse("java"), None)
    Box.DevServer.values.foreach(t => assertEquals(Box.DevServer.parse(Box.DevServer.name(t)), Some(t)))
  }

  test("bloop fingerprint: a JVM whose cmdline mentions bloop matches (main class NOT required)") {
    import Box.{matchesDevServer, DevServer}
    assert(matchesDevServer(DevServer.Bloop, "/usr/lib/jvm/temurin/bin/java",
      Seq("-Xmx4G", "-cp", "/home/u/.cache/bloop/jars/core.jar", "some.opaque.Main")))
    assert(matchesDevServer(DevServer.Bloop, "java",
      Seq("-cp", "/home/u/.cache/scalacli/local-repo/bloop-rifle.jar", "whatever")))
  }

  test("bloop fingerprint: the METALS exclusion holds (never kill the language server)") {
    import Box.{matchesDevServer, DevServer}
    assert(!matchesDevServer(DevServer.Bloop, "/usr/bin/java",
      Seq("-cp", "/home/u/.cache/metals/metals-core.jar:/home/u/.cache/bloop/rifle.jar", "scala.meta.metals.Main")))
  }

  test("bloop fingerprint: only JVMs can match — a non-java command never does") {
    import Box.{matchesDevServer, DevServer}
    assert(!matchesDevServer(DevServer.Bloop, "scala-cli", Seq("bloop.Server")))
    assert(!matchesDevServer(DevServer.Bloop, "/usr/bin/bash", Seq("-c", "eval bloop stuff")))
  }

  test("sbt fingerprint: launcher jar or boot class — a bare `sbt` word is NOT enough") {
    import Box.{matchesDevServer, DevServer}
    assert(matchesDevServer(DevServer.Sbt, "java", Seq("-jar", "/opt/sbt/bin/sbt-launch.jar")))
    assert(matchesDevServer(DevServer.Sbt, "java", Seq("-cp", "x.jar", "xsbt.boot.Boot")))
    assert(!matchesDevServer(DevServer.Sbt, "java", Seq("kill", "sbt"))) // our own cmdline shape
    assert(!matchesDevServer(DevServer.Sbt, "sbt", Seq()))               // the thin launcher script is not the JVM
  }

  test("scala-cli fingerprint: the binary itself by basename — a `scala-cli` ARGUMENT is not it") {
    import Box.{matchesDevServer, DevServer}
    assert(matchesDevServer(DevServer.ScalaCli, "/usr/local/bin/scala-cli", Seq("run", "x.scala")))
    assert(matchesDevServer(DevServer.ScalaCli, "scala-cli", Seq()))
    assert(!matchesDevServer(DevServer.ScalaCli, "/usr/bin/java", Seq("kill", "scala-cli")))
  }
