//> using file ../project.scala
//> using dep org.scalameta::munit::1.3.4

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

  test("selectOrphans (issue-023): same-dir only, non-empty only, age-capped, newest first, current id excluded") {
    import SessionStore.Orphan
    val now = 1_000_000_000L
    val older   = Orphan("older",   Some("alpha"), Vector("Afk"),  Some(1L), Some("/proj"), now - 1000)
    val newest  = Orphan("newest",  None,          Vector("Solo"), None,     Some("/proj"), now - 500)
    val elsewhere = Orphan("elsewhere", Some("x"), Vector("X"),    None,     Some("/other"), now - 10)
    val preStamp  = Orphan("prestamp",  Some("y"), Vector("Y"),    None,     None,           now - 10) // no cwd recorded -> never matches
    val empty     = Orphan("empty",     None,      Vector.empty,   Some(2L), Some("/proj"),  now - 10)
    val stale     = Orphan("stale",     Some("z"), Vector("Z"),    None,     Some("/proj"),  now - 49L * 3_600_000L)
    val me        = Orphan("me",        Some("m"), Vector("M"),    None,     Some("/proj"),  now)
    val all = Vector(older, newest, elsewhere, preStamp, empty, stale, me)
    val sel = SessionStore.selectOrphans(all, "me", "/proj", now, SessionStore.HintMaxAgeMs)
    assertEquals(sel.map(_.id), Vector("newest", "older"))
    // no age cap (the adopt path): the stale entry qualifies too, still newest-first
    val uncapped = SessionStore.selectOrphans(all, "me", "/proj", now, Long.MaxValue)
    assertEquals(uncapped.map(_.id), Vector("newest", "older", "stale"))
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

  test("mode add is UNIFORMLY session-scoped: even a budget chip goes to the session store") {
    val root = os.temp.dir()
    val g    = (root / "global").toString
    run("mode", "--global-file", g, "--sessions-root", root.toString, "--id", "s-1", "add", "RotVigil")
    run("mode", "--global-file", g, "--sessions-root", root.toString, "--id", "s-1", "add", "TokSpend")
    val sessionChips = os.read(root / "s-1" / "modes")
    assert(clue(sessionChips).contains("RotVigil") && sessionChips.contains("TokSpend"))
    assert(!os.exists(os.Path(g))) // nothing routed to the machine file
  }

  test("mode list unions in legacy global chips; rm removes from either store; clear spares the global file") {
    val root = os.temp.dir()
    val g    = root / "global"
    os.write(g, "LegacyChip\n") // pre-scoping residue / bare-shell declarations
    def m(args: String*)= run("mode", ("--global-file" +: g.toString +: "--sessions-root" +: root.toString +: "--id" +: "s-2" +: args)*)
    m("add", "Afk")
    val (_, listed, _) = m()
    assert(clue(listed).contains("Afk") && listed.contains("LegacyChip"))
    m("rm", "LegacyChip") // rm reaches the global file too
    assert(!m()._2.contains("LegacyChip"))
    os.write.over(g, "LegacyChip\n")
    m("add", "Solo"); m("clear")
    val (_, after, _) = m()
    assert(clue(after).contains("LegacyChip") && !after.contains("Solo"))
  }

  test("mode chips from another session do NOT leak into this one") {
    val root = os.temp.dir()
    val g    = (root / "global").toString
    run("mode", "--global-file", g, "--sessions-root", root.toString, "--id", "sess-A", "add", "Afk")
    val (_, listedB, _) = run("mode", "--global-file", g, "--sessions-root", root.toString, "--id", "sess-B")
    assert(!clue(listedB).contains("Afk"))
  }

  // ---- issue-023: orphaned state after a harness session-id re-mint (adopt verb + hint) ----
  // Fixture pattern: seed state under a FAKE old id via the CLI itself (so `cwd` is stamped the
  // way real writers stamp it), then read/adopt under a NEW id. All runs pin --sessions-root,
  // --id and --cwd, so nothing depends on the live harness session or the test runner's cwd.

  test("session adopt with exactly ONE candidate adopts it; post-re-mint chips survive (union)") {
    val root = os.temp.dir()
    val g    = (root / "global").toString
    run("session", "--sessions-root", root.toString, "--id", "old-1", "--cwd", "/fake/dir", "one")
    run("mode", "--global-file", g, "--sessions-root", root.toString, "--id", "old-1", "--cwd", "/fake/dir", "add", "Afk")
    // the new session already declared a post-re-mint chip; it must SURVIVE adoption (union)
    run("mode", "--global-file", g, "--sessions-root", root.toString, "--id", "new-1", "--cwd", "/fake/dir", "add", "Solo")
    val (code, out, _) = run("session", "--sessions-root", root.toString, "--id", "new-1", "--cwd", "/fake/dir", "adopt")
    assertEquals(code, 0)
    assert(clue(out).contains("adopted:") && out.contains("one") && out.contains("old-1"))
    assertEquals(SessionStore.readName(root.toNIO, "new-1"), Some("one"))
    val chips = os.read(root / "new-1" / "modes")
    assert(clue(chips).contains("Afk") && chips.contains("Solo")) // orphan chip + surviving new chip
  }

  test("session adopt with SEVERAL candidates adopts NOTHING, lists them newest first, requires a choice") {
    val root = os.temp.dir()
    val g    = (root / "global").toString
    def seed(id: String, name: String, chip: String) =
      run("session", "--sessions-root", root.toString, "--id", id, "--cwd", "/fake/dir", name)
      run("mode", "--global-file", g, "--sessions-root", root.toString, "--id", id, "--cwd", "/fake/dir", "add", chip)
    seed("old-1", "one", "Afk")
    seed("old-2", "two", "RotVigil")
    // make old-1 decisively older than old-2 (recent enough to dodge the 14-day GC)
    val hourAgo = System.currentTimeMillis() - 3_600_000L
    for f <- os.list(root / "old-1") do os.mtime.set(f, hourAgo)
    os.mtime.set(root / "old-1", hourAgo)
    val (code, out, err) = run("session", "--sessions-root", root.toString, "--id", "new-1", "--cwd", "/fake/dir", "adopt")
    assertEquals(code, 2)
    assertEquals(clue(out), "") // NOTHING adopted, nothing on stdout
    assert(!os.exists(root / "new-1")) // the new key was not even created — no state written
    assert(clue(err).contains("old-1") && err.contains("old-2") && err.contains("tt session adopt <id>"))
    assert(clue(err).indexOf("old-2") < err.indexOf("old-1")) // listed newest first
  }

  test("session adopt <id> adopts exactly that candidate; an unknown id errors naming the valid ones") {
    val root = os.temp.dir()
    val g    = (root / "global").toString
    def seed(id: String, name: String, chip: String) =
      run("session", "--sessions-root", root.toString, "--id", id, "--cwd", "/fake/dir", name)
      run("mode", "--global-file", g, "--sessions-root", root.toString, "--id", id, "--cwd", "/fake/dir", "add", chip)
    seed("old-1", "one", "Afk")
    seed("old-2", "two", "RotVigil")
    run("mode", "--global-file", g, "--sessions-root", root.toString, "--id", "new-1", "--cwd", "/fake/dir", "add", "Solo")
    // pick the OLDER candidate deliberately: the explicit choice, not recency, decides
    val hourAgo = System.currentTimeMillis() - 3_600_000L
    for f <- os.list(root / "old-1") do os.mtime.set(f, hourAgo)
    os.mtime.set(root / "old-1", hourAgo)
    val (c1, out1, _) = run("session", "--sessions-root", root.toString, "--id", "new-1", "--cwd", "/fake/dir", "adopt", "old-1")
    assertEquals(c1, 0)
    assert(clue(out1).contains("adopted:") && out1.contains("one") && out1.contains("old-1"))
    assertEquals(SessionStore.readName(root.toNIO, "new-1"), Some("one"))
    val chips = os.read(root / "new-1" / "modes")
    assert(clue(chips).contains("Afk") && chips.contains("Solo"))
    val (c2, _, err2) = run("session", "--sessions-root", root.toString, "--id", "new-2", "--cwd", "/fake/dir", "adopt", "no-such")
    assertEquals(c2, 2)
    assert(clue(err2).contains("not an adoptable orphan") && err2.contains("old-2")) // valid ones named
  }

  test("session adopt (any capitalization) with no orphan exits 2 and never NAMES; other-directory state never counts") {
    val root = os.temp.dir()
    // "Adopt" (case-typo) must hit the VERB, not silently name the session during recovery
    val (c1, _, err1) = run("session", "--sessions-root", root.toString, "--id", "lone", "--cwd", "/fake/dir", "Adopt")
    assertEquals(c1, 2)
    assert(clue(err1).contains("no orphaned session state"))
    assert(!os.exists(root / "lone")) // nothing was named/written
    run("session", "--sessions-root", root.toString, "--id", "old-x", "--cwd", "/other/dir", "elsewhere")
    val (c2, _, err2) = run("session", "--sessions-root", root.toString, "--id", "new-x", "--cwd", "/fake/dir", "adopt")
    assertEquals(c2, 2)
    assert(clue(err2).contains("no orphaned session state"))
  }

  test("empty-state reads hint at a recent same-directory orphan on STDERR; stdout is unchanged") {
    val root = os.temp.dir()
    val g    = (root / "global").toString
    run("session", "--sessions-root", root.toString, "--id", "old-h", "--cwd", "/fake/dir", "ghost")
    run("mode", "--global-file", g, "--sessions-root", root.toString, "--id", "old-h", "--cwd", "/fake/dir", "add", "Afk")
    val (c1, out1, err1) = run("mode", "--global-file", g, "--sessions-root", root.toString, "--id", "new-h", "--cwd", "/fake/dir")
    assertEquals(c1, 0)
    assertEquals(clue(out1), "(no active modes)") // stdout byte-identical to the pre-hint contract
    assert(clue(err1).contains("hint:") && err1.contains("tt session adopt") && err1.contains("ghost"))
    val (c2, out2, err2) = run("session", "--sessions-root", root.toString, "--id", "new-h", "--cwd", "/fake/dir")
    assertEquals(c2, 0)
    assert(clue(out2).trim.matches("\\d{6}-\\d{2}h\\d{2}m")) // normal print unchanged
    assert(clue(err2).contains("hint:") && err2.contains("tt session adopt"))
  }

  test("no hint without an orphan, with declared state under the current key, or past the 48h cap") {
    val root = os.temp.dir()
    val g    = (root / "global").toString
    def list(id: String) = run("mode", "--global-file", g, "--sessions-root", root.toString, "--id", id, "--cwd", "/fake/dir")
    assert(!clue(list("solo-1")._3).contains("hint:")) // empty store, nothing to hint at
    run("session", "--sessions-root", root.toString, "--id", "old-h2", "--cwd", "/fake/dir", "ghost")
    run("mode", "--global-file", g, "--sessions-root", root.toString, "--id", "new-h2", "--cwd", "/fake/dir", "add", "Solo")
    assert(!clue(list("new-h2")._3).contains("hint:")) // key not empty: human already re-declaring
    // the 48h cap needs an otherwise-EMPTY root, or the entries above would themselves hint
    val root2 = os.temp.dir()
    run("session", "--sessions-root", root2.toString, "--id", "old-h3", "--cwd", "/fake/dir", "ghost")
    val past = System.currentTimeMillis() - 49L * 3_600_000L // just past the 48h hint cap
    for f <- os.list(root2 / "old-h3") do os.mtime.set(f, past)
    os.mtime.set(root2 / "old-h3", past)
    val (_, _, err3) = run("mode", "--global-file", g, "--sessions-root", root2.toString, "--id", "new-h3", "--cwd", "/fake/dir")
    assert(!clue(err3).contains("hint:")) // orphan too old to hint (adopt still could)
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

  // ---- issue-037: read-shaped words must never SET a name; the setter announces itself ----
  // `tt session list` used to silently rename the live session to "<stamp>-list" (observed live
  // 2026-08-15). Every test here asserts the one property the defect broke: a read leaves the
  // stored name unchanged.

  test("session list is a READ: the roster prints and the stored name is untouched") {
    val root = os.temp.dir()
    run("session", "--sessions-root", root.toString, "--id", "t-37", "--cwd", "/fake/dir", "Golf")
    val (code, out, _) = run("session", "--sessions-root", root.toString, "--id", "t-37", "--cwd", "/fake/dir", "list")
    assertEquals(code, 0)
    assertEquals(SessionStore.readName(root.toNIO, "t-37"), Some("Golf")) // NOT renamed to "list"
    assert(clue(out).contains("Golf") && out.contains("t-37"))
    assert(clue(out).contains("*")) // the current session is starred
  }

  test("session list shows sessions of THIS directory only, and ls is an alias") {
    val root = os.temp.dir()
    run("session", "--sessions-root", root.toString, "--id", "here-1", "--cwd", "/fake/dir", "alpha")
    run("session", "--sessions-root", root.toString, "--id", "there-1", "--cwd", "/other/dir", "beta")
    val (code, out, _) = run("session", "--sessions-root", root.toString, "--id", "me-1", "--cwd", "/fake/dir", "list")
    assertEquals(code, 0)
    assert(clue(out).contains("alpha") && !out.contains("beta"))
    val (c2, out2, _) = run("session", "--sessions-root", root.toString, "--id", "me-1", "--cwd", "/fake/dir", "ls")
    assertEquals(c2, 0)
    assert(clue(out2).contains("alpha") && !out2.contains("beta"))
  }

  test("every reserved read word leaves the stored name unchanged, in any capitalization") {
    val root = os.temp.dir()
    run("session", "--sessions-root", root.toString, "--id", "t-38", "Golf")
    for w <- Seq("list", "ls", "show", "status", "current", "get", "name", "LIST", "Show") do
      val (code, _, _) = run("session", "--sessions-root", root.toString, "--id", "t-38", w)
      assertEquals(code, 0, w)
      assertEquals(SessionStore.readName(root.toNIO, "t-38"), Some("Golf"), w)
  }

  test("a reserved read synonym prints the display name on stdout, with a stderr note") {
    val root = os.temp.dir()
    run("session", "--sessions-root", root.toString, "--id", "t-39", "Golf")
    val (code, out, err) = run("session", "--sessions-root", root.toString, "--id", "t-39", "status")
    assertEquals(code, 0)
    assert(clue(out).trim.endsWith("-Golf")) // a genuine read of the name
    assert(clue(err).contains("reserved READ word"))
  }

  test("setting a name announces the rename on STDERR; stdout stays the bare display name") {
    val root = os.temp.dir()
    val (c1, out1, err1) = run("session", "--sessions-root", root.toString, "--id", "t-40", "Golf")
    assertEquals(c1, 0)
    assert(clue(out1).trim.endsWith("-Golf")) // stdout contract unchanged (byte-stable)
    assert(clue(err1).contains("session: named") && err1.contains("-Golf"))
    val (c2, _, err2) = run("session", "--sessions-root", root.toString, "--id", "t-40", "Hotel")
    assertEquals(c2, 0)
    assert(clue(err2).contains("session: renamed") && err2.contains("-Golf") && err2.contains("-Hotel"))
  }

  test("an exact-lowercase read word with arguments is a usage error that writes nothing") {
    val root = os.temp.dir()
    val (code, _, err) = run("session", "--sessions-root", root.toString, "--id", "t-41", "list", "extra")
    assertEquals(code, 2)
    assert(clue(err).contains("takes no arguments"))
    assert(!os.exists(root / "t-41")) // nothing was named/written
  }

  test("multi-word names starting with a capitalized reserved word still name (cold-start flow intact)") {
    val root = os.temp.dir()
    val (code, out, _) = run("session", "--sessions-root", root.toString, "--id", "t-42", "List", "of", "things")
    assertEquals(code, 0)
    assert(clue(out).trim.endsWith("-List of things"))
  }
