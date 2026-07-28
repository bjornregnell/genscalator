//> using file ../project.scala
//> using dep org.scalameta::munit::1.3.3

// Unit tests for the SM208 per-session scoping: SessionStore's PURE core, the `tt session` verb,
// mode's chip routing (session vs machine store), and the statusline session lead. All stores are
// temp-dir overrides — nothing here may touch ~/.claude (the suite runs inside a live session whose
// env carries a REAL session id, which is exactly why every CLI call pins --id / --sessions-root).

class SessionStoreSuite extends munit.FunSuite:
  private val utc = java.time.ZoneId.of("UTC")

  test("defaultName is YYMMDD-HHhMMm, fixed width, no colon") {
    // 2026-07-28 15:42 UTC
    val ms = java.time.ZonedDateTime.of(2026, 7, 28, 15, 42, 7, 0, utc).toInstant.toEpochMilli
    assertEquals(SessionStore.defaultName(ms, utc), "260728-15h42m")
  }

  test("displayName joins timestamp FIRST, human name as suffix") {
    val ms = java.time.ZonedDateTime.of(2026, 7, 28, 15, 42, 0, 0, utc).toInstant.toEpochMilli
    assertEquals(SessionStore.displayName(ms, Some("alpha prep"), utc), "260728-15h42m-alpha prep")
    assertEquals(SessionStore.displayName(ms, None, utc), "260728-15h42m")
  }

  test("validName allows spaces and hyphens, rejects control chars and empties") {
    assert(SessionStore.validName("alpha prep"))
    assert(SessionStore.validName("re-run 2"))
    assert(!SessionStore.validName(""))
    assert(!SessionStore.validName("two\nlines"))
    assert(!SessionStore.validName("tab\there"))
    assert(!SessionStore.validName("x" * 121))
  }

  test("name + started round-trip through the store; started is stamped once") {
    val root = os.temp.dir().toNIO
    SessionStore.writeName(root, "id-1", "alpha", nowMs = 1000L)
    assertEquals(SessionStore.readName(root, "id-1"), Some("alpha"))
    assertEquals(SessionStore.readStarted(root, "id-1"), Some(1000L))
    SessionStore.writeName(root, "id-1", "beta", nowMs = 2000L)
    assertEquals(SessionStore.readName(root, "id-1"), Some("beta"))
    assertEquals(SessionStore.readStarted(root, "id-1"), Some(1000L)) // first stamp survives a rename
    SessionStore.clearName(root, "id-1")
    assertEquals(SessionStore.readName(root, "id-1"), None)
  }

  test("BudgetChips carries the token-budget family and nothing session-shaped") {
    assert(SessionStore.BudgetChips("TokSpend"))
    assert(SessionStore.BudgetChips("TokenSaving"))
    assert(!SessionStore.BudgetChips("Afk"))
    assert(!SessionStore.BudgetChips("RotVigil"))
  }

  test("prune drops only dirs older than the cutoff") {
    val root = os.temp.dir().toNIO
    SessionStore.writeName(root, "old", "x", nowMs = 0L)
    SessionStore.writeName(root, "new", "y", nowMs = 0L)
    val oldDir = SessionStore.dir(root, "old")
    java.nio.file.Files.setLastModifiedTime(oldDir, java.nio.file.attribute.FileTime.fromMillis(0L))
    SessionStore.prune(root, nowMs = 20L * 86400_000L, olderThanDays = 14)
    assert(!java.nio.file.Files.exists(oldDir))
    assert(java.nio.file.Files.exists(SessionStore.dir(root, "new")))
  }

class SessionCliSuite extends munit.FunSuite:
  // Subprocess CLI-contract runs — the same resolution + parity pattern as CliSuite, local because
  // its helpers are private there.
  private lazy val toolsDir: os.Path =
    sys.props.get("tt.tools").map(os.Path(_, os.pwd)).getOrElse:
      Iterator.iterate(os.pwd)(_ / os.up).take(6)
        .find(d => os.exists(d / "tools" / "tt")).map(_ / "tools")
        .getOrElse(throw IllegalStateException(s"cannot locate tools/ (pass -Dtt.tools=<dir>); cwd=${os.pwd}"))
  private lazy val nativeBin: Option[os.Path] =
    sys.props.get("tt.native.bin").map(os.Path(_, os.pwd)).filter(os.exists)
  private val ScalaCli =
    if System.getProperty("os.name", "").toLowerCase.contains("win") then "scala-cli.bat" else "scala-cli"
  private def norm(s: String): String = s.replace("\r\n", "\n").trim
  private def run(tool: String, args: String*): (Int, String, String) = runStdin(tool, "", args*)
  private def runStdin(tool: String, stdinText: String, args: String*): (Int, String, String) =
    val r = nativeBin match
      case Some(bin) =>
        os.proc(bin.toString, tool, args).call(check = false, stdin = stdinText, stdout = os.Pipe, stderr = os.Pipe)
      case None =>
        os.proc(ScalaCli, "run", (toolsDir / s"$tool.scala").toString, "--", args)
          .call(check = false, stdin = stdinText, stdout = os.Pipe, stderr = os.Pipe)
    (r.exitCode, norm(r.out.text()), norm(r.err.text()))

  test("session with --id prints the default name; setting and clearing a name round-trips") {
    val root = os.temp.dir()
    val (c1, out1, _) = run("session", "--sessions-root", root.toString, "--id", "t-1", "--now-ms", "1785339720000")
    assertEquals(c1, 0)
    assert(clue(out1).trim.matches("\\d{6}-\\d{2}h\\d{2}m"), out1)
    val (c2, out2, _) = run("session", "--sessions-root", root.toString, "--id", "t-1", "alpha", "prep")
    assertEquals(c2, 0)
    assert(clue(out2).trim.endsWith("-alpha prep"))
    val (c3, out3, _) = run("session", "--sessions-root", root.toString, "--id", "t-1", "--clear")
    assertEquals(c3, 0)
    assert(!clue(out3).contains("alpha"))
  }

  test("session rejects a control-character name with exit 2 and writes nothing") {
    val root = os.temp.dir()
    val (c1, _, err1) = run("session", "--sessions-root", root.toString, "--id", "t-2", "bad\tname")
    assertEquals(c1, 2)
    assert(clue(err1).contains("invalid name"))
    assert(!os.exists(root / "t-2" / "name"))
  }

  test("mode routes: session chip to the session store, budget chip to the machine store") {
    val root = os.temp.dir()
    val g    = (root / "global").toString
    run("mode", "--global-file", g, "--sessions-root", root.toString, "--id", "s-1", "add", "RotVigil")
    run("mode", "--global-file", g, "--sessions-root", root.toString, "--id", "s-1", "add", "TokSpend")
    val sessionChips = os.read(root / "s-1" / "modes")
    val globalChips  = os.read(os.Path(g))
    assert(clue(sessionChips).contains("RotVigil") && !sessionChips.contains("TokSpend"))
    assert(clue(globalChips).contains("TokSpend") && !globalChips.contains("RotVigil"))
  }

  test("mode list is the union; rm removes from either store; clear keeps budget chips") {
    val root = os.temp.dir()
    val g    = (root / "global").toString
    def m(args: String*)= run("mode", ("--global-file" +: g +: "--sessions-root" +: root.toString +: "--id" +: "s-2" +: args)*)
    m("add", "Afk"); m("add", "TokSpend")
    val (_, listed, _) = m()
    assert(clue(listed).contains("Afk") && listed.contains("TokSpend"))
    m("rm", "Afk")
    assert(!m()._2.contains("Afk"))
    m("add", "Solo"); m("clear")
    val (_, after, _) = m()
    assert(clue(after).contains("TokSpend") && !after.contains("Solo"))
  }

  test("mode chips from another session do NOT leak into this one") {
    val root = os.temp.dir()
    val g    = (root / "global").toString
    run("mode", "--global-file", g, "--sessions-root", root.toString, "--id", "sess-A", "add", "Afk")
    val (_, listedB, _) = run("mode", "--global-file", g, "--sessions-root", root.toString, "--id", "sess-B")
    assert(!clue(listedB).contains("Afk"))
  }

  test("statusline renders the session lead + chips from session and machine stores") {
    val root = os.temp.dir()
    val g    = root / "global"
    os.write(g, "TokSpend\n")
    run("mode", "--global-file", g.toString, "--sessions-root", root.toString, "--id", "sl-1", "add", "RotVigil")
    run("session", "--sessions-root", root.toString, "--id", "sl-1", "--now-ms", "1785339720000", "demo")
    val (code, out, _) = runStdin("statusline", """{"session_id":"sl-1"}""",
      "--mode-line", "--no-status", "--no-tok",
      "--modes-file", g.toString, "--sessions-root", root.toString, "--limits-file", "/nonexistent")
    assertEquals(code, 0)
    assert(clue(out).contains("gs session:"))
    assert(clue(out).contains("demo"))
    assert(clue(out).contains("RotVigil"))
    assert(clue(out).contains("TokSpend"))
  }

  test("statusline without a session_id renders the pre-scoping mode line") {
    val root = os.temp.dir()
    val g    = root / "global"
    os.write(g, "TokSpend\n")
    val (code, out, _) = runStdin("statusline", """{}""",
      "--mode-line", "--no-status", "--no-tok",
      "--modes-file", g.toString, "--sessions-root", root.toString, "--limits-file", "/nonexistent")
    assertEquals(code, 0)
    assert(clue(out).contains("gs mode set"))
    assert(!clue(out).contains("gs session:"))
  }
