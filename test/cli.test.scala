//> using scala 3.8.4
//> using dep org.scalameta::munit::1.3.3
//> using dep com.lihaoyi::os-lib:0.11.8

// CLI-CONTRACT tests: run each tool as a SUBPROCESS (`scala-cli run tools/<t>.scala -- <args>`) against
// fixtures generated in a temp dir, and assert exit code + stdout. Deliberately does NOT import the tools —
// that avoids the pre-monolith multi-@main clash AND tests the REAL command-line contract, so this suite
// stays valid across the planned monolith refactor (it is the golden "identical output before/after" net).
//
// Tool dir resolution (cwd-independent, config-in-ARGS not env — PRD: configInArgsNotEnv): a `-Dtt.tools=<dir>`
// property (passed as an explicit `scala-cli --java-prop tt.tools=<dir>` flag), else walk up from the cwd for a
// `tools/tt`. NO ambient env var. Run from the genscalator root:  scala-cli test test
//   or from anywhere:  scala-cli test <root>/test --java-prop tt.tools=<root>/tools

class CliSuite extends munit.FunSuite:

  private lazy val toolsDir: os.Path =
    sys.props.get("tt.tools").map(os.Path(_)).filter(d => os.exists(d / "tt")).getOrElse:
      Iterator.iterate(os.pwd)(_ / os.up).take(8)
        .find(d => os.exists(d / "tools" / "tt")).map(_ / "tools")
        .getOrElse(throw IllegalStateException(s"cannot locate tools/ (pass -Dtt.tools=<dir>); cwd=${os.pwd}"))

  /** Run a tool file as a subprocess; return (exit, stdout, stderr). */
  private def run(tool: String, args: String*): (Int, String, String) =
    val r = os.proc("scala-cli", "run", (toolsDir / s"$tool.scala").toString, "--", args)
      .call(check = false, stdout = os.Pipe, stderr = os.Pipe)
    (r.exitCode, r.out.text().trim, r.err.text().trim)

  // --- text ---
  test("text count: number of regex matches") {
    val f = os.temp(contents = "foo\nbar\nfoo baz foo\n", suffix = ".txt")
    try
      val (code, out, _) = run("text", "count", f.toString, "foo")
      assertEquals(code, 0)
      assertEquals(out, "3")
    finally os.remove(f)
  }
  test("text match: prints matching lines, skips non-matches") {
    val f = os.temp(contents = "alpha\nBETA\ngamma\n", suffix = ".txt")
    try
      val (_, out, _) = run("text", "match", f.toString, "a")
      assert(clue(out).contains("alpha"))
      assert(clue(out).contains("gamma"))
      assert(!clue(out).contains("BETA")) // no lowercase 'a'
    finally os.remove(f)
  }
  test("text cols: extract 1-based fields, tab-joined") {
    val f = os.temp(contents = "a,b,c\n1,2,3\n", suffix = ".csv")
    try
      val (_, out, _) = run("text", "cols", f.toString, ",", "1", "3")
      assert(clue(out).contains("a\tc"))
      assert(clue(out).contains("1\t3"))
    finally os.remove(f)
  }
  test("text grepr: recurse, honour extension filter") {
    val d = os.temp.dir()
    try
      os.write(d / "hit.scala", "val target = 1\n")
      os.write(d / "skip.txt", "target\n") // wrong extension → ignored
      val (_, out, _) = run("text", "grepr", d.toString, ".scala", "target")
      assert(clue(out).contains("hit.scala"))
      assert(!clue(out).contains("skip.txt"))
    finally os.remove.all(d)
  }
  test("text with no args prints usage (does not crash)") {
    val (_, out, _) = run("text")
    assert(clue(out).toLowerCase.contains("count")) // usage lists subcommands
  }

  // --- files ---
  test("files --count: counts files by extension") {
    val d = os.temp.dir()
    try
      os.write(d / "a.scala", "x")
      os.write(d / "b.scala", "y")
      os.write(d / "c.txt", "z")
      val (_, out, _) = run("files", d.toString, ".scala", "--count")
      assert(clue(out).contains("2 files"))
    finally os.remove.all(d)
  }
  test("files with content regex: filters by body match") {
    val d = os.temp.dir()
    try
      os.write(d / "yes.scala", "has NEEDLE here")
      os.write(d / "no.scala", "nothing")
      val (_, out, _) = run("files", d.toString, ".scala", "NEEDLE")
      assert(clue(out).contains("1 files"))
      assert(clue(out).contains("yes.scala"))
    finally os.remove.all(d)
  }

  // --- verify (allowlist safety) ---
  test("verify refuses a non-allowlisted executable with exit 2, never running it") {
    // `ls` is harmless AND not in the builtin allowlist {scala-cli, tt, scalex} → must be rejected pre-exec.
    val (code, _, err) = run("verify", "--", "ls")
    assertEquals(code, 2)
    assert(clue(err).contains("not an allowed executable"))
  }
  test("verify usage error (no command) exits 2") {
    val (code, _, err) = run("verify")
    assertEquals(code, 2)
    assert(clue(err).toLowerCase.contains("usage"))
  }
