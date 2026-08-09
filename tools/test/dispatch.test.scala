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

  // Same invocation shape as the golden test below; check = false so exit codes are data.
  private def runDispatcher(args: String*): os.CommandResult =
    // scala-cli.bat on Windows: ProcessBuilder does no PATHEXT resolution (see cli.test.scala).
    val scalaCli = if System.getProperty("os.name", "").toLowerCase.contains("win")
                   then "scala-cli.bat" else "scala-cli"
    os.proc(scalaCli, "run", toolsDir.toString,
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
    assertEquals(r.exitCode, 2)
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
