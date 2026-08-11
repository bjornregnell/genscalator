//> using file ../project.scala
//> using dep org.scalameta::munit::1.3.4
//> using dep com.lihaoyi::os-lib:0.11.8

// Tests for the single dispatcher (dispatch.scala). The coverage test is the drift-proof: the verb
// table must match EXACTLY the set of tools/*.scala files with a top-level @main (minus dispatch
// itself), so adding or removing a tool without touching the table fails the suite. The subprocess
// test exercises the real single-entry-point contract the native image will ship with.

class DispatchSuite extends munit.FunSuite:

  // Hans's alpha finding (2026-07-28): the subprocess golden test pays for a COLD scala-cli compile
  // inside munit's default 30s budget — measured 4.2s warm vs 30.94s-and-timeout cold, a 7x margin
  // CI will eventually land on the wrong side of, and the failure reads as a dispatcher bug to
  // whoever hits it. The budget covers the compile, not the dispatcher.
  override def munitTimeout = scala.concurrent.duration.Duration(180, "s")

  // Deliberately restates the locate logic (test independence over DRY, scala-style §5).
  private lazy val toolsDir: os.Path =
    sys.props.get("tt.tools").map(os.Path(_)).filter(d => os.exists(d / "tt")).getOrElse:
      Iterator.iterate(os.pwd)(_ / os.up).take(8)
        .find(d => os.exists(d / "tools" / "tt")).map(_ / "tools")
        .getOrElse(throw IllegalStateException(s"cannot locate tools/ (pass -Dtt.tools=<dir>); cwd=${os.pwd}"))

  test("verb table covers exactly the tool files with a top-level @main") {
    val stems = os.list(toolsDir)
      .filter(p => os.isFile(p) && p.ext == "scala")
      .filter(p => os.read.lines(p).exists(_.startsWith("@main ")))
      .map(_.baseName)
      .filterNot(_ == "dispatch")
      .toSet
    assertEquals(Dispatch.verbs.toSet, stems)
  }

  test("verbs are unique and usage lists every verb") {
    assertEquals(Dispatch.verbs.distinct, Dispatch.verbs)
    Dispatch.verbs.foreach(v => assert(clue(Dispatch.usage).contains(v)))
  }

  test("entryFor: known verb yields an entry, unknown yields none") {
    assert(Dispatch.entryFor("text").isDefined)
    assert(Dispatch.entryFor("no-such-tool").isEmpty)
  }

  // scala-cli.bat on Windows: ProcessBuilder does no PATHEXT resolution (see cli.test.scala).
  private val isWindows = System.getProperty("os.name", "").toLowerCase.contains("win")
  private val ScalaCli  = if isWindows then "scala-cli.bat" else "scala-cli"

  // Parity mode (opt-in), the same contract CliSuite uses: -Dtt.native.bin=<native dispatcher> runs
  // these through the binary that actually SHIPS, so the dispatcher contract is tested end to end
  // rather than through a build tool that happens to spawn it.
  //
  // ⚠ It matters more here than anywhere else in the suite, because a wrapper can silently rewrite the
  // very thing these tests assert. Specimen, the v0.10.2 release run (2026-08-11, windows-x86_64):
  // this test saw exit 1 where the dispatcher had called `sys.exit(2)`, while CliSuite's 29 exit-2
  // assertions passed in the SAME run — because CliSuite goes through the binary and this suite went
  // through `scala-cli.bat`. Exit 0 survived that wrapper; the exact nonzero code did not. The
  // dispatcher's exit branches carry no platform condition, so the wrapper was the only difference.
  private lazy val nativeBin: Option[os.Path] =
    sys.props.get("tt.native.bin").map(os.Path(_, os.pwd)).filter(os.exists)

  // check = false so exit codes are data.
  private def runDispatcher(args: String*): os.CommandResult =
    nativeBin match
      case Some(bin) =>
        os.proc(bin.toString, args.toSeq).call(check = false, stdout = os.Pipe, stderr = os.Pipe)
      case None =>
        os.proc(ScalaCli, "run", toolsDir.toString,
            "--main-class", "dispatchTypedTools", "--", args.toSeq)
          .call(check = false, stdout = os.Pipe, stderr = os.Pipe)

  test("subprocess: help, --help, -h print usage with the full tool list and exit 0 (issue 020)") {
    for helpArg <- Seq("help", "--help", "-h") do
      val r = runDispatcher(helpArg)
      assertEquals(r.exitCode, 0, s"'$helpArg' must exit 0")
      val out = r.out.text()
      assert(clue(out).contains("usage: tt <tool> <args...>"), s"'$helpArg' must print the usage line")
      Dispatch.verbs.foreach(v => assert(clue(out).contains(v), s"'$helpArg' usage must list verb '$v'"))
  }

  test("subprocess: unknown tool still exits 2 with usage on stderr (issue 020)") {
    val r = runDispatcher("no-such-tool")
    // Exit 2 is the contract, asserted exactly wherever the channel preserves it — which is the native
    // binary always, and `scala-cli` everywhere except Windows. Through `scala-cli.bat` the assertion
    // weakens to "failed", because that is all that channel can honestly report (see runDispatcher).
    if nativeBin.isDefined || !isWindows then assertEquals(r.exitCode, 2)
    else assertNotEquals(r.exitCode, 0, "unknown tool must fail")
    val err = r.err.text()
    assert(clue(err).contains("tt: no such tool 'no-such-tool'"))
    assert(clue(err).contains("usage: tt <tool> <args...>"))
  }

  test("subprocess golden: text count through the single entry point") {
    val f = os.temp(contents = "foo\nbar\nfoo baz foo\n", suffix = ".txt")
    try
      // scala-cli.bat on Windows: ProcessBuilder does no PATHEXT resolution (see cli.test.scala).
      val scalaCli = if System.getProperty("os.name", "").toLowerCase.contains("win")
                     then "scala-cli.bat" else "scala-cli"
      val r = os.proc(scalaCli, "run", toolsDir.toString,
          "--main-class", "dispatchTypedTools", "--", "text", "count", f.toString, "foo")
        .call(check = false, stdout = os.Pipe, stderr = os.Pipe)
      assertEquals(r.exitCode, 0)
      assertEquals(r.out.text().replace("\r\n", "\n").trim, "3")
    finally os.remove(f)
  }
