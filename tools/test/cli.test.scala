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
// `tools/tt`. NO ambient env var. Run from the genscalator root:  scala-cli test tools
//   or from anywhere:  scala-cli test <root>/tools --java-prop tt.tools=<root>/tools

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
  // --- doc ---
  test("doc: prints a doc verbatim by name (tries .txt/.md)") {
    val d = os.temp.dir()
    try
      os.write(d / "hello.txt", "line one\nline two\n")
      val (code, out, _) = run("doc", "--docs", d.toString, "hello")
      assertEquals(code, 0)
      assertEquals(out, "line one\nline two")
    finally os.remove.all(d)
  }
  test("doc: bare (no name) lists md/txt docs, skips others") {
    val d = os.temp.dir()
    try
      os.write(d / "a.md", "x")
      os.write(d / "b.txt", "y")
      os.write(d / "skip.log", "z")
      val (code, out, _) = run("doc", "--docs", d.toString)
      assertEquals(code, 0)
      assert(clue(out).contains("a.md"))
      assert(clue(out).contains("b.txt"))
      assert(!clue(out).contains("skip.log"))
    finally os.remove.all(d)
  }
  test("doc: unknown name exits 2 with a message") {
    val d = os.temp.dir()
    try
      val (code, _, err) = run("doc", "--docs", d.toString, "nope")
      assertEquals(code, 2)
      assert(clue(err).contains("no such doc"))
    finally os.remove.all(d)
  }
  test("doc: rejects a traversal / path name") {
    val d = os.temp.dir()
    try
      val (code, _, err) = run("doc", "--docs", d.toString, "../secret")
      assertEquals(code, 2)
      assert(clue(err).contains("invalid"))
    finally os.remove.all(d)
  }

  // --- mode (declared joint state-of-mind) ---
  test("mode: add (idempotent) then list shows one per line, in order") {
    val d = os.temp.dir()
    try
      val f = (d / "modes").toString
      run("mode", "--file", f, "add", "hot-harvest")
      run("mode", "--file", f, "add", "hot-harvest") // idempotent: no duplicate
      run("mode", "--file", f, "add", "token-spend")
      val (code, out, _) = run("mode", "--file", f)
      assertEquals(code, 0)
      assertEquals(out, "hot-harvest\ntoken-spend")
    finally os.remove.all(d)
  }
  test("mode: rm removes one label; clear empties") {
    val d = os.temp.dir()
    try
      val f = (d / "modes").toString
      run("mode", "--file", f, "add", "a")
      run("mode", "--file", f, "add", "b")
      run("mode", "--file", f, "rm", "a")
      val (_, out1, _) = run("mode", "--file", f)
      assertEquals(out1, "b")
      run("mode", "--file", f, "clear")
      val (_, out2, _) = run("mode", "--file", f)
      assertEquals(out2, "(no active modes)")
    finally os.remove.all(d)
  }
  test("mode: invalid label (spaces) exits 2") {
    val d = os.temp.dir()
    try
      val (code, _, err) = run("mode", "--file", (d / "modes").toString, "add", "has space")
      assertEquals(code, 2)
      assert(clue(err).contains("invalid label"))
    finally os.remove.all(d)
  }

  // --- statusline mode line ---
  test("statusline --mode-line: renders declared modes as a second line") {
    val d = os.temp.dir()
    try
      val f = (d / "modes").toString
      run("mode", "--file", f, "add", "hot-harvest")
      val (code, out, _) = run("statusline", "--no-status", "--mode-line", "--modes-file", f, "{}")
      assertEquals(code, 0)
      assert(clue(out).contains("gs mode set"))
      assert(clue(out).contains("hot-harvest"))
      assert(!clue(out).contains(" & ")) // one mode -> no separator
    finally os.remove.all(d)
  }
  test("statusline: no --mode-line means no mode line") {
    val (code, out, _) = run("statusline", "{}")
    assertEquals(code, 0)
    assert(!clue(out).contains(" & "))
  }

  test("text context: prints a match with N lines of context, excludes beyond N") {
    val f = os.temp(contents = "l1\nl2\nNEEDLE\nl4\nl5\n", suffix = ".txt")
    try
      val (code, out, _) = run("text", "context", f.toString, "NEEDLE", "1")
      assertEquals(code, 0)
      assert(clue(out).contains("NEEDLE"))
      assert(clue(out).contains("l2")) // 1 before
      assert(clue(out).contains("l4")) // 1 after
      assert(!clue(out).contains("l1")) // 2 before — outside N=1
      assert(!clue(out).contains("l5")) // outside
    finally os.remove(f)
  }
  test("text context: separates non-contiguous match groups with --") {
    val f = os.temp(contents = "HIT\nx\nx\nx\nx\nHIT\n", suffix = ".txt")
    try
      val (_, out, _) = run("text", "context", f.toString, "HIT", "1")
      assert(clue(out).contains("--")) // two groups with a gap between
    finally os.remove(f)
  }
  test("text with no args prints usage (does not crash)") {
    val (_, out, _) = run("text")
    assert(clue(out).toLowerCase.contains("count")) // usage lists subcommands
  }
  test("text grepr warns (stderr) on a grep-BRE escaped pipe — the Java-regex silent-empty footgun") {
    val d = os.temp.dir()
    try
      os.write(d / "f.scala", "alpha\nbeta\n")
      val (_, _, err) = run("text", "grepr", d.toString, ".scala", "alpha\\|beta") // grep-BRE `\|`
      assert(clue(err).contains("grep-BRE"))
      assert(clue(err).contains("Java regex"))
    finally os.remove.all(d)
  }
  test("text count reads UTF-8 (Swedish å/ä/ö not mangled) — regression for the latin1 bug") {
    val f = os.temp(contents = "Björn\nRegnell\n", suffix = ".txt")
    try
      val (code, out, _) = run("text", "count", f.toString, "ö")
      assertEquals(code, 0)
      assertEquals(out, "1") // read as latin1 the UTF-8 ö byte-pair would NOT match the char 'ö' → 0
    finally os.remove(f)
  }
  test("text match preserves UTF-8 Swedish characters in its output (no mojibake)") {
    val f = os.temp(contents = "Björn\nabc\n", suffix = ".txt")
    try
      val (_, out, _) = run("text", "match", f.toString, "B")
      assert(clue(out).contains("Björn")) // not "BjÃ¶rn"
    finally os.remove(f)
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

  // --- find (typed safe enumeration, read-half) ---
  private def findFixture(): os.Path =
    val d = os.temp.dir()
    os.write(d / "a.scala", "x")
    os.write(d / "b.scala", "y")
    os.write(d / "c.txt", "z")
    os.makeDir(d / "sub")
    os.write(d / "sub" / "deep.scala", "q")
    d

  test("find --ext + default type f: recurses, counts regular files by extension") {
    val d = findFixture()
    try
      val (_, out, _) = run("find", d.toString, "--ext", ".scala", "--count")
      assert(clue(out).contains("3 matches"))
      assert(!clue(out).contains("a.scala"))            // --count suppresses the path list
      val (_, out2, _) = run("find", d.toString, "--count")
      assert(clue(out2).contains("4 matches"))          // every regular file: a, b, c.txt, sub/deep
    finally os.remove.all(d)
  }
  test("find --name: filters by filename glob") {
    val d = findFixture()
    try
      val (_, out, _) = run("find", d.toString, "--name", "*.scala")
      assert(clue(out).contains("3 matches"))
      assert(clue(out).contains("deep.scala"))
      val (_, out2, _) = run("find", d.toString, "--name", "a.scala", "--count")
      assert(clue(out2).contains("1 matches"))
    finally os.remove.all(d)
  }
  test("find --type d: lists directories including the root") {
    val d = findFixture()
    try
      val (_, out, _) = run("find", d.toString, "--type", "d", "--count")
      assert(clue(out).contains("2 matches"))           // root + sub
    finally os.remove.all(d)
  }
  test("find --max-depth: bounds the descent") {
    val d = findFixture()
    try
      val (_, out, _) = run("find", d.toString, "--ext", ".scala", "--max-depth", "1", "--count")
      assert(clue(out).contains("2 matches"))           // sub/deep.scala (depth 2) is excluded
    finally os.remove.all(d)
  }
  test("find on a nonexistent root: exit 2, no such path") {
    val d = findFixture()
    try
      val (code, _, err) = run("find", (d / "nope").toString)
      assertEquals(code, 2)
      assert(clue(err).contains("no such path"))
    finally os.remove.all(d)
  }
  test("find --help: elaborate help, exit 0") {
    val (code, out, _) = run("find", "--help")
    assertEquals(code, 0)
    assert(clue(out).contains("tt find"))
    assert(clue(out).contains("read-half"))
  }
  test("find skips hidden entries by default; --all includes them") {
    val d = os.temp.dir()
    try
      os.write(d / "visible.scala", "x")
      os.makeDir(d / ".hidden")
      os.write(d / ".hidden" / "buried.scala", "y")
      os.write(d / ".dotfile.scala", "z")
      val (_, out, _) = run("find", d.toString, "--ext", ".scala", "--count")
      assert(clue(out).contains("1 matches"))         // only visible.scala (hidden dir + dotfile skipped)
      val (_, outAll, _) = run("find", d.toString, "--ext", ".scala", "--all", "--count")
      assert(clue(outAll).contains("3 matches"))       // + .hidden/buried.scala + .dotfile.scala
    finally os.remove.all(d)
  }

  // --- skillcheck (SM070: expected-skill manifest from disk; warn on the silent skill outage) ---
  private def skillsFixture(): os.Path =
    val d = os.temp.dir()
    os.makeDir(d / "alpha"); os.write(d / "alpha" / "SKILL.md", "# alpha\n")
    os.makeDir(d / "beta");  os.write(d / "beta" / "SKILL.md", "# beta\n")
    os.makeDir(d / "notaskill"); os.write(d / "notaskill" / "README.md", "no skill here\n")
    d

  test("skillcheck list mode: names the expected skills from disk, excludes dirs without SKILL.md") {
    val d = skillsFixture()
    try
      val (code, out, _) = run("skillcheck", "--skills", d.toString)
      assertEquals(code, 0)
      assert(clue(out).contains("expected genscalator skills"))
      assert(clue(out).contains("alpha"))
      assert(clue(out).contains("beta"))
      assert(!clue(out).contains("notaskill"))            // no SKILL.md → not an expected skill
    finally os.remove.all(d)
  }
  test("skillcheck --active with all expected present: OK, exit 0") {
    val d = skillsFixture()
    try
      val (code, out, _) = run("skillcheck", "--skills", d.toString, "--active", "alpha", "beta")
      assertEquals(code, 0)
      assert(clue(out).contains("OK: all 2 expected"))
    finally os.remove.all(d)
  }
  test("skillcheck --active reports an unexpected active skill as info, still exit 0") {
    val d = skillsFixture()
    try
      val (code, out, _) = run("skillcheck", "--skills", d.toString, "--active", "alpha", "beta", "gamma")
      assertEquals(code, 0)
      assert(clue(out).contains("active but not in the genscalator set"))
      assert(clue(out).contains("gamma"))
    finally os.remove.all(d)
  }
  test("skillcheck --active missing one: WARNING naming the missing skill, exit 1") {
    val d = skillsFixture()
    try
      val (code, _, err) = run("skillcheck", "--skills", d.toString, "--active", "alpha")
      assertEquals(code, 1)                               // the silent-outage signal, made loud
      assert(clue(err).contains("NOT active"))
      assert(clue(err).contains("beta"))
    finally os.remove.all(d)
  }
  test("skillcheck --active with NONE active (the /skills said 'No skills found' case): exit 1, all missing") {
    val d = skillsFixture()
    try
      val (code, _, err) = run("skillcheck", "--skills", d.toString, "--active")
      assertEquals(code, 1)
      assert(clue(err).contains("alpha"))
      assert(clue(err).contains("beta"))
    finally os.remove.all(d)
  }
  test("skillcheck --help: elaborate help, exit 0") {
    val (code, out, _) = run("skillcheck", "--help")
    assertEquals(code, 0)
    assert(clue(out).contains("tt skillcheck"))
    assert(clue(out).contains("phenomenology") || clue(out).contains("cannot FEEL"))
  }

  // --- prd (SM065: read + navigate PRD.md; structural FUTURE summary, no LLM) ---
  private def prdFixture(): os.Path =
    val d = os.temp.dir()
    val f = d / "PRD.md"
    os.write(f,
      """|# PRD
         |
         |## FUTURE
         |
         |### Release v1.0.0 — first
         |
         |* Feature: fooTool has
         |  * Gist: does the foo thing
         |  * Spec: some detail about foo
         |* Feature: fooTool helps Goal: bar
         |
         |* Goal: someGoal has
         |  * Gist: the goal gist
         |
         |### Release v2.0.0 — later
         |
         |* Feature: bazTool has
         |  * Gist: does baz
         |
         |## PAST
         |
         |### IMPLEMENTED
         |* Feature: oldTool has
         |  * Gist: shipped already
         |""".stripMargin)
    f

  test("prd summarize: extracts FUTURE Feature/Goal gists by release, excludes PAST") {
    val f = prdFixture()
    try
      val (code, out, _) = run("prd", "--prd", f.toString, "summarize")
      assertEquals(code, 0)
      assert(clue(out).contains("fooTool — does the foo thing"))
      assert(clue(out).contains("someGoal — the goal gist"))
      assert(clue(out).contains("bazTool — does baz"))
      assert(clue(out).contains("### Release v1.0.0 — first"))
      assert(clue(out).contains("### Release v2.0.0 — later"))
      assert(!clue(out).contains("oldTool"))               // PAST is excluded
      assert(!clue(out).contains("some detail about foo"))  // Spec lines are not gists
    finally os.remove.all(f / os.up)
  }
  test("prd find: case-insensitive line match tagged with the nearest heading") {
    val f = prdFixture()
    try
      val (code, out, _) = run("prd", "--prd", f.toString, "find", "BAZ")
      assertEquals(code, 0)
      assert(clue(out).contains("does baz"))
      assert(clue(out).contains("Release v2.0.0"))          // heading context
    finally os.remove.all(f / os.up)
  }
  test("prd find: no match exits 1") {
    val f = prdFixture()
    try
      val (code, out, _) = run("prd", "--prd", f.toString, "find", "zzznotthere")
      assertEquals(code, 1)
      assert(clue(out).contains("no line matches"))
    finally os.remove.all(f / os.up)
  }
  test("prd show: prints the whole PRD verbatim") {
    val f = prdFixture()
    try
      val (code, out, _) = run("prd", "--prd", f.toString, "show")
      assertEquals(code, 0)
      assert(clue(out).contains("## FUTURE"))
      assert(clue(out).contains("## PAST"))
      assert(clue(out).contains("shipped already"))
    finally os.remove.all(f / os.up)
  }
  test("prd: unknown verb and bare invocation error with exit 2") {
    val f = prdFixture()
    try
      val (c1, _, e1) = run("prd", "--prd", f.toString, "wat")
      assertEquals(c1, 2)
      assert(clue(e1).contains("unknown verb"))
      val (c2, _, e2) = run("prd", "--prd", f.toString)
      assertEquals(c2, 2)
      assert(clue(e2).contains("usage"))
    finally os.remove.all(f / os.up)
  }
  test("prd --help: elaborate help, exit 0") {
    val (code, out, _) = run("prd", "--help")
    assertEquals(code, 0)
    assert(clue(out).contains("tt prd"))
    assert(clue(out).contains("never LLM-generated"))
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

  /** Run a tool subprocess with an explicit cwd (newtool writes tools/<name>.scala RELATIVE to cwd). */
  private def runIn(cwd: os.Path, tool: String, args: String*): (Int, String, String) =
    val r = os.proc("scala-cli", "run", (toolsDir / s"$tool.scala").toString, "--", args)
      .call(cwd = cwd, check = false, stdout = os.Pipe, stderr = os.Pipe)
    (r.exitCode, r.out.text().trim, r.err.text().trim)

  // --- parsereqt (reqT-lang parse + lint over the vendored parser) ---
  // Fixtures use verified reqT-lang syntax; behaviors were confirmed against `tt parsereqt` before encoding here.
  test("parsereqt parse: reports a top-level elem count, exit 0") {
    val f = os.temp(contents = "* Feature: alpha\n* Feature: beta requires Feature: alpha\n", suffix = ".md")
    try
      val (code, out, _) = run("parsereqt", "parse", f.toString)
      assertEquals(code, 0)
      assert(clue(out).contains("reqt parse:"))
      assert(clue(out).contains("top-level elems"))
    finally os.remove(f)
  }
  test("parsereqt lint: a clean model reports 0 fall-throughs") {
    val f = os.temp(contents = "* Feature: alpha\n* Feature: beta requires Feature: alpha\n", suffix = ".md")
    try
      val (code, out, _) = run("parsereqt", "lint", f.toString)
      assertEquals(code, 0)
      assert(clue(out).contains("0 unknown-concept fall-through"))
    finally os.remove(f)
  }
  test("parsereqt lint REGRESSION: a relation under `has` is flagged as LOST to Text") {
    // The silent-data-loss case reqT/reqT-lang#15 targets: a relation keyword written as an indented bullet
    // under a `has` block degrades to a Text attr (the relation is LOST). This locks the lint detection so a
    // future parser/wrapper change can't silently stop catching it. Verified live: `tt parsereqt lint` emits
    // "relation 'requires' LOST to Text" for exactly this input.
    val f = os.temp(contents = "* Feature: alpha has\n    * requires: Feature: beta\n", suffix = ".md")
    try
      val (code, out, _) = run("parsereqt", "lint", f.toString)
      assertEquals(code, 0)
      assert(clue(out).contains("relation 'requires' LOST to Text"))
      assert(clue(out).contains("1 unknown-concept fall-through"))
    finally os.remove(f)
  }
  test("parsereqt lint: a Capitalized typo is flagged as an unknown concept") {
    val f = os.temp(contents = "* Feautre: alpha\n", suffix = ".md")
    try
      val (_, out, _) = run("parsereqt", "lint", f.toString)
      assert(clue(out).contains("unknown concept 'Feautre'"))
    finally os.remove(f)
  }
  test("parsereqt with no args prints usage and exits 2") {
    val (code, out, _) = run("parsereqt")
    assertEquals(code, 2)
    assert(clue(out).toLowerCase.contains("usage"))
  }

  // --- log (build/run log analyzer) ---
  test("log summary: counts errors and warnings with a verdict") {
    val f = os.temp(contents = "[error] boom\n[warn] careful\nall good\n", suffix = ".log")
    try
      val (code, out, _) = run("log", "summary", f.toString)
      assertEquals(code, 0)
      assert(clue(out).contains("1 errors, 1 warnings"))
    finally os.remove(f)
  }
  test("log errors: shows only the error bucket") {
    val f = os.temp(contents = "[error] boom\n[warn] careful\n", suffix = ".log")
    try
      val (_, out, _) = run("log", "errors", f.toString)
      assert(clue(out).contains("=== errors: 1"))
      assert(clue(out).contains("boom"))
    finally os.remove(f)
  }
  test("log --error: a custom pattern adds matches on top of the defaults") {
    val f = os.temp(contents = "kaboom here\nfine line\n", suffix = ".log")
    try
      val (_, out, _) = run("log", "errors", f.toString, "--error", "kaboom")
      assert(clue(out).contains("=== errors: 1"))
      assert(clue(out).contains("kaboom"))
    finally os.remove(f)
  }
  test("log --cap with a non-integer exits 2") {
    val f = os.temp(contents = "[error] x\n", suffix = ".log")
    try
      val (code, _, err) = run("log", "summary", f.toString, "--cap", "notanint")
      assertEquals(code, 2)
      assert(clue(err).toLowerCase.contains("cap"))
    finally os.remove(f)
  }
  test("log with no file argument exits 2") {
    val (code, _, err) = run("log")
    assertEquals(code, 2)
    assert(clue(err).nonEmpty)
  }

  // --- newtool (scaffold generator; writes tools/<name>.scala relative to cwd) ---
  test("newtool: scaffolds a tool from the template into tools/ under cwd") {
    val work = os.temp.dir()
    try
      os.makeDir.all(work / "tools")
      os.write(work / "tools" / "template.scala.txt", "// __NAME__ tool\n@main def __NAME__() = println(\"__NAME__\")\n")
      val (code, out, _) = runIn(work, "newtool", "mytool")
      assertEquals(code, 0)
      val made = work / "tools" / "mytool.scala"
      assert(os.exists(made))
      assert(clue(os.read(made)).contains("mytool"))
      assert(!clue(os.read(made)).contains("__NAME__")) // placeholder substituted
    finally os.remove.all(work)
  }
  test("newtool: refuses to overwrite an existing tool file (exit 1)") {
    val work = os.temp.dir()
    try
      os.makeDir.all(work / "tools")
      os.write(work / "tools" / "template.scala.txt", "// __NAME__\n")
      os.write(work / "tools" / "dup.scala", "// already here\n")
      val (code, _, err) = runIn(work, "newtool", "dup")
      assertEquals(code, 1)
      assert(clue(err).contains("refusing"))
    finally os.remove.all(work)
  }
  test("newtool: rejects a non-identifier tool name") {
    val work = os.temp.dir()
    try
      os.makeDir.all(work / "tools")
      os.write(work / "tools" / "template.scala.txt", "// __NAME__\n")
      val (code, _, err) = runIn(work, "newtool", "9bad")
      assert(clue(code) != 0)
      assert(clue(err).contains("bad tool name"))
    finally os.remove.all(work)
  }

  // --- guardcheck (flags guard-trip / banned-reflex patterns; the "prosthetic perception" of guard feedback) ---
  // Trap inputs are safe here: they are Scala string literals passed via os.proc, never a Bash-tool shell arg.
  test("guardcheck cmd: flags an && command chain (exit 1)") {
    val (code, out, _) = run("guardcheck", "cmd", "git add . && git commit")
    assertEquals(code, 1)
    assert(clue(out).contains("&& command chain"))
  }
  test("guardcheck cmd: a clean git -C command passes (exit 0)") {
    val (code, out, _) = run("guardcheck", "cmd", "git -C /tmp/x status")
    assertEquals(code, 0)
    assert(clue(out).contains("clean"))
  }
  test("guardcheck cmd: flags command substitution") {
    val (code, out, _) = run("guardcheck", "cmd", "x=$(find .)")
    assertEquals(code, 1)
    assert(clue(out).contains("command substitution"))
  }
  test("guardcheck cmd: flags a pipe to head") {
    val (_, out, _) = run("guardcheck", "cmd", "ps aux | head")
    assert(clue(out).contains("pipe to head"))
  }
  test("guardcheck msg: flags a line-leading # (newline-then-#)") {
    val (code, out, _) = run("guardcheck", "msg", "#8017 harvest done")
    assertEquals(code, 1)
    assert(clue(out).contains("line-leading #"))
  }
  test("guardcheck msg: clean prose passes (exit 0)") {
    val (code, out, _) = run("guardcheck", "msg", "harvest turn 8017 done")
    assertEquals(code, 0)
    assert(clue(out).contains("clean"))
  }
  test("guardcheck with no args prints usage and exits 2") {
    val (code, out, _) = run("guardcheck")
    assertEquals(code, 2)
    assert(clue(out).toLowerCase.contains("usage"))
  }

  // --- typo (keyboard-aware typo classifier for the fatigue gauge; tests keyed to BR's real typos) ---
  test("typo adjacent: i and o are Swedish-QWERTY neighbors") {
    val (code, out, _) = run("typo", "adjacent", "i", "o")
    assertEquals(code, 0)
    assertEquals(out, "yes")
  }
  test("typo adjacent: q and p are not neighbors") {
    val (_, out, _) = run("typo", "adjacent", "q", "p")
    assertEquals(out, "no")
  }
  test("typo classify: identofy->identify is an adjacency slip (i->o, BR's real typo)") {
    val (_, out, _) = run("typo", "classify", "identofy", "identify")
    assertEquals(out, "adjacency")
  }
  test("typo classify: tierd->tired is a transposition (BR's real typo)") {
    val (_, out, _) = run("typo", "classify", "tierd", "tired")
    assertEquals(out, "transposition")
  }
  test("typo classify: compedium->compendium is a deletion (BR's real typo)") {
    val (_, out, _) = run("typo", "classify", "compedium", "compendium")
    assertEquals(out, "deletion")
  }
  test("typo with no args prints usage and exits 2") {
    val (code, out, _) = run("typo")
    assertEquals(code, 2)
    assert(clue(out).toLowerCase.contains("classify"))
  }

  // --- htmltext (strip a saved HTML page to readable text) ---
  test("htmltext: strips head/script/tags, decodes entities, keeps body text") {
    val f = os.temp(
      contents = "<html><head><title>t</title><style>.x{}</style></head><body><script>var a=1;</script>" +
        "<h1>Hello</h1><p>World &amp; more</p></body></html>",
      suffix = ".html")
    try
      val (code, out, _) = run("htmltext", f.toString)
      assertEquals(code, 0)
      assert(clue(out).contains("Hello"))
      assert(clue(out).contains("World & more")) // entity decoded
      assert(!clue(out).contains("var a=1")) // script body dropped
      assert(!clue(out).contains(".x{}")) // style dropped
      assert(!clue(out).contains("<h1>")) // tags removed
    finally os.remove(f)
  }
  test("htmltext with no args prints usage and exits 2") {
    val (code, out, _) = run("htmltext")
    assertEquals(code, 2)
    assert(clue(out).toLowerCase.contains("usage"))
  }

  // --- chrono (stopwatch) — fmt is the pure formatter; now/usage are the CLI contract ---
  test("chrono fmt: formats ms as a compact duration") {
    assertEquals(run("chrono", "fmt", "1000")._2, "1.00s")
    assertEquals(run("chrono", "fmt", "45000")._2, "45s")
    assertEquals(run("chrono", "fmt", "65000")._2, "1m 5s")
    assertEquals(run("chrono", "fmt", "78000")._2, "1m 18s")
  }
  test("chrono now: prints a timestamp, exit 0") {
    val (code, out, _) = run("chrono", "now")
    assertEquals(code, 0)
    assert(clue(out).matches("""\d{4}-\d\d-\d\d \d\d:\d\d:\d\d"""))
  }
  test("chrono with no args prints usage and exits 2") {
    val (code, out, _) = run("chrono")
    assertEquals(code, 2)
    assert(clue(out).toLowerCase.contains("usage"))
  }
  test("chrono think: parses a relayed think-time to ms") {
    assertEquals(run("chrono", "think", "30s")._2, "30000")
    assertEquals(run("chrono", "think", "1m18s")._2, "78000")
    assertEquals(run("chrono", "think", "1m")._2, "60000")
    assertEquals(run("chrono", "think", "90")._2, "90000")
  }
  test("chrono report: runs and summarises (exit 0)") {
    val (code, out, _) = run("chrono", "report")
    assertEquals(code, 0)
    assert(clue(out).toLowerCase.contains("report"))
  }

  // --- git (safe git helper) — show: read-only file-content-at-ref extraction ---
  /** Fixture: init a git repo in d with one commit containing name→content. Returns nothing; throws on failure. */
  private def gitFixture(d: os.Path, name: String, content: String): Unit =
    def g(args: String*) = os.proc("git" +: "-C" +: d.toString +: args)
      .call(stdout = os.Pipe, stderr = os.Pipe)
    g("init", "-q")
    g("config", "user.email", "test@example.org")
    g("config", "user.name", "Test Fixture")
    os.write(d / os.SubPath(name), content, createFolders = true)
    g("add", name)
    g("commit", "-q", "-m", "add fixture file")

  test("git show: prints the committed content at HEAD, not the working tree") {
    val d = os.temp.dir()
    try
      gitFixture(d, "f.txt", "hello at ref\n")
      os.write.over(d / "f.txt", "DIRTY working tree\n") // must NOT leak into show output
      val (code, out, _) = run("git", "show", "--repo", d.toString, "--ref", "HEAD", "--path", "f.txt")
      assertEquals(code, 0)
      assertEquals(out, "hello at ref") // run() trims; exactness incl. newline is the --out test below
    finally os.remove.all(d)
  }
  test("git show: --out writes the byte-exact content to the given file") {
    val d = os.temp.dir()
    try
      gitFixture(d, "sub/dir/data.md", "# Title\n\nBjörn läser åäö.\n")
      val outFile = d / "extracted.md"
      val (code, out, _) = run("git", "show", "--repo", d.toString, "--ref", "HEAD",
        "--path", "sub/dir/data.md", "--out", outFile.toString)
      assertEquals(code, 0)
      assert(clue(out).contains("wrote"))
      assertEquals(os.read(outFile), "# Title\n\nBjörn läser åäö.\n") // byte-exact incl. trailing newline + UTF-8
    finally os.remove.all(d)
  }
  test("git show: a bad ref exits non-zero with a clear error (no empty success)") {
    val d = os.temp.dir()
    try
      gitFixture(d, "f.txt", "x\n")
      val (code, out, err) = run("git", "show", "--repo", d.toString, "--ref", "nosuchref", "--path", "f.txt")
      assert(clue(code) != 0)
      assertEquals(out, "") // nothing on stdout — never a partial/empty success
      assert(clue(err).contains("nosuchref"))
    finally os.remove.all(d)
  }
  test("git show: a bad path exits non-zero and does not create the --out file") {
    val d = os.temp.dir()
    try
      gitFixture(d, "f.txt", "x\n")
      val outFile = d / "should-not-exist.txt"
      val (code, _, err) = run("git", "show", "--repo", d.toString, "--ref", "HEAD",
        "--path", "missing.txt", "--out", outFile.toString)
      assert(clue(code) != 0)
      assert(clue(err).contains("missing.txt"))
      assert(!os.exists(outFile))
    finally os.remove.all(d)
  }
  test("git show: missing required flags exit non-zero with guidance") {
    val d = os.temp.dir()
    try
      gitFixture(d, "f.txt", "x\n")
      val (code, _, err) = run("git", "show", "--repo", d.toString, "--ref", "HEAD") // no --path
      assert(clue(code) != 0)
      assert(clue(err).contains("--path"))
    finally os.remove.all(d)
  }
  test("git --help mentions the show subcommand") {
    val (code, out, _) = run("git", "--help")
    assertEquals(code, 0)
    assert(clue(out).contains("show"))
    assert(clue(out).contains("--ref"))
  }

  // --- guardcheck (the prosthetic habit as a tool: cmd/msg checks + the PreToolUse hook mode) ---
  test("guardcheck cmd: a clean typed command exits 0") {
    val (code, out, _) = run("guardcheck", "cmd", "tt git commit --repo /x --message-file /x/m.txt --add /x/f --push")
    assertEquals(code, 0)
    assert(clue(out).toLowerCase.contains("clean"))
  }
  test("guardcheck cmd: flags /dev/stdin commit sink (exit 1)") {
    val (code, out, _) = run("guardcheck", "cmd", "tt git commit --message-file /dev/stdin")
    assertEquals(code, 1)
    assert(clue(out).contains("/dev/stdin"))
  }
  test("guardcheck cmd: flags heredoc and here-string (<<)") {
    assertEquals(run("guardcheck", "cmd", "tt git commit --message-file - <<EOF")._1, 1)
    val (code, out, _) = run("guardcheck", "cmd", "grep foo <<< bar")
    assertEquals(code, 1)
    assert(clue(out.toLowerCase).contains("heredoc"))
  }
  test("guardcheck cmd: flags grep context flags -A/-B/-C") {
    assertEquals(run("guardcheck", "cmd", "grep -nA4 foo file")._1, 1)
    assertEquals(run("guardcheck", "cmd", "grep -B2 foo file")._1, 1)
    assertEquals(run("guardcheck", "cmd", "grep -C3 foo file")._1, 1)
  }
  test("guardcheck hook: HIGH finding → deny decision JSON") {
    val json = """{"tool_name":"Bash","tool_input":{"command":"tt git commit --message-file /dev/stdin"}}"""
    val (code, out, _) = run("guardcheck", "hook", json)
    assertEquals(code, 0)
    assert(clue(out).contains("\"permissionDecision\":\"deny\""))
    assert(clue(out).contains("PreToolUse"))
  }
  test("guardcheck hook: MED-only finding → ask decision JSON") {
    val json = """{"tool_name":"Bash","tool_input":{"command":"grep -A4 foo file"}}"""
    val (_, out, _) = run("guardcheck", "hook", json)
    assert(clue(out).contains("\"permissionDecision\":\"ask\""))
  }
  test("guardcheck hook: a clean command emits nothing (allow)") {
    val json = """{"tool_name":"Bash","tool_input":{"command":"tt chrono now"}}"""
    val (code, out, _) = run("guardcheck", "hook", json)
    assertEquals(code, 0)
    assertEquals(out, "")
  }
  test("guardcheck with no args prints usage and exits 2") {
    val (code, out, _) = run("guardcheck")
    assertEquals(code, 2)
    assert(clue(out).toLowerCase.contains("usage"))
  }

  // --- wr stamp (transcript-timestamp retrofit lookup) ---
  test("wr stamp: finds a match and prints timestamp + type + session + snippet") {
    val d = os.temp.dir()
    try
      os.write(d / "abc12345.jsonl",
        "{\"type\":\"user\",\"timestamp\":\"2026-07-06T16:48:35.656Z\",\"message\":{\"content\":\"giving ok to mv star THAT I REALLY DO NOT WANT\"}}\n" +
        "{\"type\":\"assistant\",\"timestamp\":\"2026-07-06T16:49:00.000Z\",\"message\":{\"content\":\"noted\"}}\n")
      val (code, out, _) = run("wr", "stamp", d.toString, "REALLY DO NOT WANT")
      assertEquals(code, 0)
      assert(clue(out).contains("2026-07-06T16:48:35.656Z"))
      assert(clue(out).contains("[user]"))
      assert(clue(out).contains("abc12345"))
    finally os.remove.all(d)
  }
  test("wr stamp: --user skips non-user entries (exit 1 when only an assistant matches)") {
    val d = os.temp.dir()
    try
      os.write(d / "s.jsonl", "{\"type\":\"assistant\",\"timestamp\":\"2026-07-06T00:00:00Z\",\"message\":{\"content\":\"noted here\"}}\n")
      val (code, out, _) = run("wr", "stamp", d.toString, "noted", "--user")
      assertEquals(code, 1)
      assert(clue(out).toLowerCase.contains("no match"))
    finally os.remove.all(d)
  }
  test("wr stamp: sorts matches earliest-first") {
    val d = os.temp.dir()
    try
      os.write(d / "s.jsonl",
        "{\"type\":\"user\",\"timestamp\":\"2026-07-06T10:00:00Z\",\"message\":{\"content\":\"beta marker\"}}\n" +
        "{\"type\":\"user\",\"timestamp\":\"2026-07-06T09:00:00Z\",\"message\":{\"content\":\"alpha marker\"}}\n")
      val (_, out, _) = run("wr", "stamp", d.toString, "marker")
      assert(clue(out).indexOf("09:00:00") < clue(out).indexOf("10:00:00"))
    finally os.remove.all(d)
  }
  test("wr stamp --human keeps a genuine human-typed string line") {
    val d = os.temp.dir()
    try
      os.write(d / "s.jsonl",
        "{\"type\":\"user\",\"timestamp\":\"2026-07-06T09:00:00Z\",\"message\":{\"role\":\"user\",\"content\":\"UNIQUEPHRASE from BR\"}}\n")
      val (code, out, _) = run("wr", "stamp", d.toString, "UNIQUEPHRASE", "--human")
      assertEquals(code, 0)
      assert(clue(out).contains("2026-07-06T09:00:00Z"))
      assert(clue(out).contains("[user]"))
    finally os.remove.all(d)
  }
  test("wr stamp --human DROPS a tool_result echo (type==user but has toolUseResult) that --user would keep") {
    val d = os.temp.dir()
    try
      os.write(d / "s.jsonl",
        "{\"type\":\"user\",\"timestamp\":\"2026-07-06T09:00:00Z\",\"toolUseResult\":{\"stdout\":\"x\"}," +
          "\"message\":{\"role\":\"user\",\"content\":[{\"type\":\"tool_result\",\"tool_use_id\":\"t1\",\"content\":\"ECHOMARK back\"}]}}\n")
      // --human filters the echo out entirely → no matches (exit 1)
      val (codeH, outH, _) = run("wr", "stamp", d.toString, "ECHOMARK", "--human")
      assertEquals(codeH, 1)
      assert(clue(outH.toLowerCase).contains("no match"))
      // the coarse --user WOULD keep it (the very footgun --human fixes)
      assertEquals(run("wr", "stamp", d.toString, "ECHOMARK", "--user")._1, 0)
    finally os.remove.all(d)
  }
  test("wr stamp --human drops isMeta chrome and <command-name> wrappers") {
    val d = os.temp.dir()
    try
      os.write(d / "s.jsonl",
        "{\"type\":\"user\",\"isMeta\":true,\"timestamp\":\"2026-07-06T09:00:00Z\",\"message\":{\"role\":\"user\",\"content\":\"METAMARK ctx\"}}\n" +
          "{\"type\":\"user\",\"timestamp\":\"2026-07-06T09:01:00Z\",\"message\":{\"role\":\"user\",\"content\":\"<command-name>/context</command-name> CMDMARK\"}}\n")
      assertEquals(run("wr", "stamp", d.toString, "METAMARK", "--human")._1, 1) // meta dropped
      assertEquals(run("wr", "stamp", d.toString, "CMDMARK", "--human")._1, 1)  // command wrapper dropped
    finally os.remove.all(d)
  }
  test("wr stamp --human keeps an array text block (image+text style paste)") {
    val d = os.temp.dir()
    try
      os.write(d / "s.jsonl",
        "{\"type\":\"user\",\"timestamp\":\"2026-07-06T09:00:00Z\",\"message\":{\"role\":\"user\",\"content\":[{\"type\":\"text\",\"text\":\"ARRTEXTMARK here\"}]}}\n")
      val (code, out, _) = run("wr", "stamp", d.toString, "ARRTEXTMARK", "--human")
      assertEquals(code, 0)
      assert(clue(out).contains("[user]"))
    finally os.remove.all(d)
  }
  test("wr with no args prints usage and exits 2") {
    val (code, out, _) = run("wr")
    assertEquals(code, 2)
    assert(clue(out).toLowerCase.contains("usage"))
  }

  // --- svg (textual sequence-diagram spec → self-contained, theme-aware SVG for blogs & reports) ---
  test("svg sequence: renders an <svg> with lifeline labels and a message label to stdout") {
    val f = os.temp(contents = "actor Alice\nactor Bob\nAlice -> Bob: hello\n", suffix = ".txt")
    try
      val (code, out, _) = run("svg", "sequence", f.toString)
      assertEquals(code, 0)
      assert(clue(out).startsWith("<svg"))
      assert(clue(out).contains("Alice"))
      assert(clue(out).contains("Bob"))
      assert(clue(out).contains("hello"))
      assert(clue(out).contains("</svg>"))
    finally os.remove(f)
  }
  test("svg --sequence-diagram alias works and includes the title") {
    val f = os.temp(contents = "title: My Flow\nA -> B: x\n", suffix = ".txt")
    try
      val (code, out, _) = run("svg", "--sequence-diagram", f.toString)
      assertEquals(code, 0)
      assert(clue(out).contains("My Flow"))
      assert(clue(out).contains("class=\"title\""))
    finally os.remove(f)
  }
  test("svg: a return arrow (-->) is dashed and XML metacharacters are escaped") {
    val f = os.temp(contents = "A --> B: a < b & c\n", suffix = ".txt")
    try
      val (_, out, _) = run("svg", "sequence", f.toString)
      assert(clue(out).contains("class=\"ret\"")) // dashed return line
      assert(clue(out).contains("a &lt; b &amp; c")) // escaped, not raw
      assert(!clue(out).contains("a < b & c"))
    finally os.remove(f)
  }
  test("svg: a self-message (A -> A) draws a loop path and does not crash") {
    val f = os.temp(contents = "A -> A: think\n", suffix = ".txt")
    try
      val (code, out, _) = run("svg", "sequence", f.toString)
      assertEquals(code, 0)
      assert(clue(out).contains("think"))
      assert(clue(out).contains("<path")) // the loop
    finally os.remove(f)
  }
  test("svg output is well-formed XML (title/message/note metachars all escaped)") {
    val f = os.temp(contents = "title: T & U\nactor A\nA -> A: x < y\nnote over A: n > m\n", suffix = ".txt")
    try
      val (_, out, _) = run("svg", "sequence", f.toString)
      val doc = javax.xml.parsers.DocumentBuilderFactory.newInstance()
        .newDocumentBuilder()
        .parse(java.io.ByteArrayInputStream(out.getBytes("UTF-8")))
      assertEquals(doc.getDocumentElement.getTagName, "svg") // parses without throwing
    finally os.remove(f)
  }
  test("svg sequence writes to a file when an out path is given") {
    val d = os.temp.dir()
    try
      val in = d / "s.txt"; os.write(in, "A -> B: hi\n")
      val outp = d / "s.svg"
      val (code, out, _) = run("svg", "sequence", in.toString, outp.toString)
      assertEquals(code, 0)
      assert(clue(out).contains("wrote auto/opaque sequence diagram"))
      assert(os.exists(outp))
      assert(clue(os.read(outp)).contains("<svg"))
    finally os.remove.all(d)
  }
  test("svg with no args prints usage and exits 2") {
    val (code, out, _) = run("svg")
    assertEquals(code, 2)
    assert(clue(out).toLowerCase.contains("usage"))
    assert(clue(out).toLowerCase.contains("sequence"))
  }
  test("svg: a message whose text begins with ':' spawns no phantom lifeline (leading-colon regression)") {
    // seqspec bug: `to` was `\S+` (colon is \S), so `A -> B: :Z cue` captured to="B:" — a 3rd, phantom lifeline.
    val d = os.temp.dir()
    try
      val in = d / "s.txt"; os.write(in, "actor A\nactor B\nA -> B: :Z cue tired\n")
      val outp = d / "s.svg"
      val (code, out, _) = run("svg", "sequence", in.toString, outp.toString)
      assertEquals(code, 0)
      assert(clue(out).contains("2 lifelines")) // A and B only, not a phantom "B:"
      assert(clue(os.read(outp)).contains(":Z cue tired")) // the colon-led text still renders
    finally os.remove.all(d)
  }
  test("svg --dark emits a fixed dark palette and no media query") {
    val f = os.temp(contents = "A -> B: x\n", suffix = ".txt")
    try
      val (code, out, _) = run("svg", "sequence", f.toString, "--dark")
      assertEquals(code, 0)
      assert(clue(out).contains("--fg:#e6e6f0")) // dark foreground
      assert(!clue(out).contains("prefers-color-scheme")) // tailored theme has no media query
    finally os.remove(f)
  }
  test("svg --light emits a fixed light palette and no media query") {
    val f = os.temp(contents = "A -> B: x\n", suffix = ".txt")
    try
      val (_, out, _) = run("svg", "sequence", f.toString, "--light")
      assert(clue(out).contains("--fg:#1b1b2b")) // light foreground
      assert(!clue(out).contains("prefers-color-scheme"))
    finally os.remove(f)
  }
  test("svg default theme is auto (adapts via prefers-color-scheme)") {
    val f = os.temp(contents = "A -> B: x\n", suffix = ".txt")
    try
      val (_, out, _) = run("svg", "sequence", f.toString)
      assert(clue(out).contains("prefers-color-scheme")) // media query present
      assert(clue(out).contains("--fg:#1b1b2b")) // with the light base
    finally os.remove(f)
  }
  test("svg default background is opaque (a theme-coloured canvas rect + --bg var)") {
    val f = os.temp(contents = "A -> B: x\n", suffix = ".txt")
    try
      val (code, out, _) = run("svg", "sequence", f.toString)
      assertEquals(code, 0)
      assert(clue(out).contains("class=\"canvas\"")) // background rect present by default
      assert(clue(out).contains("--bg:")) // theme background colour var
    finally os.remove(f)
  }
  test("svg --transparent omits the background rect") {
    val f = os.temp(contents = "A -> B: x\n", suffix = ".txt")
    try
      val (code, out, _) = run("svg", "sequence", f.toString, "--transparent")
      assertEquals(code, 0)
      assert(!clue(out).contains("class=\"canvas\"")) // no background rect
    finally os.remove(f)
  }

  // --- ascii (SAME spec as svg, shared via seqspec.scala → monospace box-drawing art) ---
  test("ascii sequence: renders lifelines, a message label, and box-drawing glyphs") {
    val f = os.temp(contents = "actor Alice\nactor Bob\nAlice -> Bob: hello\n", suffix = ".txt")
    try
      val (code, out, _) = run("ascii", "sequence", f.toString)
      assertEquals(code, 0)
      assert(clue(out).contains("Alice"))
      assert(clue(out).contains("Bob"))
      assert(clue(out).contains("hello"))
      assert(clue(out).contains("│")) // box-drawing lifeline
      assert(clue(out).contains("▶")) // filled arrowhead
    finally os.remove(f)
  }
  test("ascii --pure uses strict 7-bit ASCII (no box-drawing glyphs)") {
    val f = os.temp(contents = "A -> B: x\n", suffix = ".txt")
    try
      val (code, out, _) = run("ascii", "sequence", f.toString, "--pure")
      assertEquals(code, 0)
      assert(clue(out).contains("|")) // ascii lifeline
      assert(clue(out).contains(">")) // ascii arrowhead
      assert(!clue(out).contains("│")) // no unicode lifeline
      assert(!clue(out).contains("▶"))
    finally os.remove(f)
  }
  test("ascii self-message and note render without crashing") {
    val f = os.temp(contents = "actor A\nA -> A: think\nnote over A: hmm\n", suffix = ".txt")
    try
      val (code, out, _) = run("ascii", "sequence", f.toString)
      assertEquals(code, 0)
      assert(clue(out).contains("think"))
      assert(clue(out).contains("hmm"))
    finally os.remove(f)
  }
  test("ascii writes to a file when an out path is given") {
    val d = os.temp.dir()
    try
      val in = d / "s.txt"; os.write(in, "A -> B: hi\n")
      val outp = d / "s.art"
      val (code, out, _) = run("ascii", "sequence", in.toString, outp.toString)
      assertEquals(code, 0)
      assert(clue(out).contains("wrote"))
      assert(os.exists(outp))
    finally os.remove.all(d)
  }
  test("ascii with no args prints usage and exits 2") {
    val (code, out, _) = run("ascii")
    assertEquals(code, 2)
    assert(clue(out).toLowerCase.contains("usage"))
  }

  // --- gvdot (SAME spec → graphviz DOT rendered via `dot`; the DOT-gen path needs no `dot` installed) ---
  test("gvdot sequence: emits valid-looking DOT with headers, lifelines, and a message edge") {
    val f = os.temp(contents = "actor Alice\nactor Bob\nAlice -> Bob: hello\n", suffix = ".txt")
    try
      val (code, out, _) = run("gvdot", "sequence", f.toString)
      assertEquals(code, 0)
      assert(clue(out).contains("digraph seq"))
      assert(clue(out).contains("Alice"))
      assert(clue(out).contains("Bob"))
      assert(clue(out).contains("hello"))
      assert(clue(out).contains("->")) // edges present
    finally os.remove(f)
  }
  test("gvdot: a note renders as a note-shaped node") {
    val f = os.temp(contents = "actor A\nnote over A: hmm\n", suffix = ".txt")
    try
      val (_, out, _) = run("gvdot", "sequence", f.toString)
      assert(clue(out).contains("shape=note"))
      assert(clue(out).contains("hmm"))
    finally os.remove(f)
  }
  test("gvdot escapes double-quotes in labels (valid DOT, no injection)") {
    val f = os.temp(contents = "A -> B: say \"hi\"\n", suffix = ".txt")
    try
      val (_, out, _) = run("gvdot", "sequence", f.toString)
      assert(clue(out).contains("\\\"hi\\\"")) // quotes escaped for DOT
    finally os.remove(f)
  }
  test("gvdot with no args prints usage and exits 2") {
    val (code, out, _) = run("gvdot")
    assertEquals(code, 2)
    assert(clue(out).toLowerCase.contains("usage"))
  }
  /** Is graphviz `dot` on PATH? Probed HERE via os.proc (not imported from the tool) — the suite runs tools as
    * subprocesses and deliberately does not import them, so it can't call `Gvdot.dotAvailable` directly. */
  private lazy val dotAvailable: Boolean =
    try os.proc("dot", "-V").call(check = false, stdout = os.Pipe, stderr = os.Pipe).exitCode == 0
    catch case _: Throwable => false
  test("gvdot RENDER path: with an out path, shells to `dot` and writes a non-empty image (guarded by dot present)") {
    // The effectful driver (spawn `dot`, write a file) — the tool's whole point — had zero coverage; the pure
    // DOT-gen path above needs no `dot`. Guarded by `assume` so the suite still passes where graphviz is absent.
    assume(dotAvailable, "graphviz `dot` not on PATH — skipping the effectful render-path test")
    val d = os.temp.dir()
    try
      val in = d / "s.txt"; os.write(in, "actor Alice\nactor Bob\nAlice -> Bob: hello\n")
      val outp = d / "s.svg" // svg format is textual + self-contained; easy to assert non-empty
      val (code, out, err) = run("gvdot", "sequence", in.toString, outp.toString)
      assertEquals(code, 0, clue(err))
      assert(clue(out).contains("wrote svg via graphviz dot"))
      assert(os.exists(outp))
      assert(clue(os.size(outp)) > 0L)
    finally os.remove.all(d)
  }

  // --- statusline (format the CC statusLine stdin JSON into one line; SM039) ---
  test("statusline: formats model + cost + context + rate limits into one line") {
    val now = 1_000_000_000_000L
    val resetsSec = now / 1000L + 3 * 86400 // 3 days later, in SECONDS
    val json = s"""{"model":{"display_name":"Opus 4.8","id":"opus"},"cost":{"total_cost_usd":12.34},""" +
      s""""context_window":{"used_percentage":41},"rate_limits":{"five_hour":{"used_percentage":30},""" +
      s""""seven_day":{"used_percentage":14,"resets_at":$resetsSec}}}"""
    val (code, out, _) = run("statusline", json, "--now-ms", now.toString)
    assertEquals(code, 0)
    assert(clue(out).contains("genscalator")) // brand prefix
    assert(clue(out).contains("o4.8")) // model label compacted: Opus 4.8 -> o4.8 (lower-case o, no ctx here)
    assert(clue(out).contains("cost $12")) // whole dollars, no cents (12.34 -> 12)
    assert(!clue(out).contains("cost $12.34")) // cents dropped
    assert(clue(out).contains("ctx-fill 41%"))
    assert(clue(out).contains("5h-lim 30%"))
    assert(clue(out).contains("wk-lim 14%"))
    assert(clue(out).contains("reset 3d"))
  }
  test("statusline: missing rate_limits degrades gracefully (shows what's present, no crash)") {
    val (code, out, _) = run("statusline", """{"model":{"id":"haiku"},"cost":{"total_cost_usd":0.5}}""")
    assertEquals(code, 0)
    assert(clue(out).contains("haiku"))
    assert(clue(out).contains("cost $0")) // 0.50 truncates to whole dollars -> $0
    assert(!clue(out).contains("wk")) // no rate_limits → no weekly segment
  }
  test("statusline: empty/invalid JSON prints an empty line at exit 0 (never breaks the prompt)") {
    val (code, out, _) = run("statusline", "not json at all")
    assertEquals(code, 0)
    assertEquals(out, "")
  }
  test("statusline: resets_at given in MILLISECONDS is auto-detected (not multiplied again)") {
    val now = 1_000_000_000_000L
    val resetsMs = now + 2 * 86400_000L // 2 days later, already in MS (> 1e12)
    val json = s"""{"rate_limits":{"seven_day":{"used_percentage":50,"resets_at":$resetsMs}}}"""
    val (_, out, _) = run("statusline", json, "--now-ms", now.toString)
    assert(clue(out).contains("wk-lim 50%"))
    assert(clue(out).contains("reset 2d"))
  }
  test("statusline: five_hour resets_at renders a fine h/m countdown") {
    val now = 1_000_000_000_000L
    val resetSec = now / 1000L + (2 * 3600 + 34 * 60) // 2h34m later, in SECONDS
    val json = s"""{"rate_limits":{"five_hour":{"used_percentage":68,"resets_at":$resetSec}}}"""
    val (_, out, _) = run("statusline", json, "--now-ms", now.toString)
    assert(clue(out).contains("5h-lim 68%"))
    assert(clue(out).contains("reset 2h34m"))
  }
  test("statusline: a usage limit at/above the warn threshold turns BOTH its % and its reset red") {
    val now = 1_000_000_000_000L
    val resetSec = now / 1000L + 3600 // 1h later
    val json = s"""{"rate_limits":{"five_hour":{"used_percentage":85,"resets_at":$resetSec}}}"""
    val (_, out, _) = run("statusline", json, "--now-ms", now.toString)
    assert(clue(out).contains("38;5;203m5h-lim 85%")) // the % is red (>= 80% default warn)
    assert(clue(out).contains("38;5;203mreset"))     // and its reset countdown reddens with it
  }
  test("statusline: --warn makes the threshold configurable (85% stays non-red under --warn 90)") {
    val now = 1_000_000_000_000L
    val resetSec = now / 1000L + 3600
    val json = s"""{"rate_limits":{"five_hour":{"used_percentage":85,"resets_at":$resetSec}}}"""
    val (_, out, _) = run("statusline", json, "--now-ms", now.toString, "--warn", "90")
    assert(clue(out).contains("38;5;245mreset"))  // reset stays dim grey (85% is below the raised 90% threshold)
    assert(!clue(out).contains("38;5;203mreset")) // and is not red
  }
  test("statusline: ctx-fill reds at the dumb-zone threshold (Z, default 30%), oranges at 0.8*Z, green below") {
    val now = 1_000_000_000_000L
    val (_, outHi, _)  = run("statusline", """{"context_window":{"used_percentage":35}}""", "--now-ms", now.toString)
    assert(clue(outHi).contains("38;5;203mctx-fill 35%"))  // 35% >= 30% (Z) -> red (dumb-zone risk)
    val (_, outMid, _) = run("statusline", """{"context_window":{"used_percentage":26}}""", "--now-ms", now.toString)
    assert(clue(outMid).contains("38;5;214mctx-fill 26%")) // 26% >= 24% (0.8*Z compact trigger) -> orange
    val (_, outLo, _)  = run("statusline", """{"context_window":{"used_percentage":15}}""", "--now-ms", now.toString)
    assert(clue(outLo).contains("38;5;114mctx-fill 15%"))  // 15% -> healthy green (well inside the smart zone)
    assert(!clue(outLo).contains("38;5;203mctx-fill"))      // and not red at 90%-style thresholds
  }
  test("statusline: --ctx-warn makes the dumb-zone threshold configurable (35% non-red under --ctx-warn 40)") {
    val now = 1_000_000_000_000L
    val (_, out, _) = run("statusline", """{"context_window":{"used_percentage":35}}""", "--now-ms", now.toString, "--ctx-warn", "40")
    assert(!clue(out).contains("38;5;203mctx-fill")) // 35% < 40% raised threshold -> not red
  }
  test("statusline: ctx-fill past dumb-zone (75%) -> bold bright-red + dumb-zone flag") {
    val (_, out, _) = run("statusline", """{"context_window":{"used_percentage":80}}""", "--now-ms", "1000000000000")
    assert(clue(out).contains("1;38;5;196mctx-fill 80% dumb-zone"))
  }
  test("statusline: ctx-fill past auto-compact (92%) -> bold reverse-red + auto-compact flag (supersedes dumb-zone)") {
    val (_, out, _) = run("statusline", """{"context_window":{"used_percentage":95}}""", "--now-ms", "1000000000000")
    assert(clue(out).contains("7;1;38;5;196mctx-fill 95% auto-compact!"))
    assert(!clue(out).contains("dumb-zone"))
  }
  test("statusline: --dumb-zone / --auto-compact thresholds are configurable") {
    val (_, out, _) = run("statusline", """{"context_window":{"used_percentage":65}}""", "--now-ms", "1000000000000", "--dumb-zone", "60")
    assert(clue(out).contains("ctx-fill 65% dumb-zone"))
  }
  test("statusline: prepends a HH:MM:SS wall clock, and abbreviates the model label") {
    val (_, out, _) = run("statusline", """{"model":{"display_name":"Fable 5 (1M context)"}}""", "--now-ms", "1000000000000")
    assert("""\d\d:\d\d:\d\d""".r.findFirstIn(out).isDefined, clue(out)) // a HH:MM:SS clock is present
    assert(clue(out).contains("f5/1M")) // Fable 5 (1M context) -> f5/1M (compact SM117 form)
  }

  // --- SM117 status-line gauges: pure helpers + transcript parsing + tok/tired? segments ---
  test("statusline SM117 pure helpers: shortModel / formatTokens / tokGauge") {
    import StatuslineTool.*
    assertEquals(shortModel("Opus 4.8 (1M context)"), "o4.8/1M")
    assertEquals(shortModel("Fable 5"), "f5")
    assertEquals(shortModel("Sonnet 5"), "s5")
    assertEquals(shortModel("Haiku 4.5"), "h4.5")
    assertEquals(formatTokens(5008654L), "5.0M")
    assertEquals(formatTokens(178118L), "178k")
    assertEquals(formatTokens(950L), "950")
    assertEquals(formatTokens(1_000_000L), "1.0M")
    assertEquals(tokGauge(1_000_000L, 3_000_000L, 6_000_000L), "38;5;42")  // green base
    assertEquals(tokGauge(4_000_000L, 3_000_000L, 6_000_000L), "38;5;214") // orange past warn
    assertEquals(tokGauge(7_000_000L, 3_000_000L, 6_000_000L), Red)        // red past danger
  }

  test("statusline SM119 sortModes: stable canonical order regardless of +/- add/remove history") {
    import StatuslineTool.*
    // same SET, different insertion orders -> identical render order (the whole point of SM119)
    val a = sortModes(Seq("solo", "afk", "rot-vigil"))
    val b = sortModes(Seq("rot-vigil", "afk", "solo"))
    assertEquals(a, b)
    assertEquals(a, Seq("afk", "solo", "rot-vigil"))                       // session frame first, agent-vigilance after
    assertEquals(sortModes(Seq("zzz", "tok-spend", "aaa")),               // unknowns sort alphabetically AFTER known
                 Seq("tok-spend", "aaa", "zzz"))
    assertEquals(sortModes(Seq("dumb-zone?", "dumb-zone")),               // ?-inferred sorts just after its base (SM118)
                 Seq("dumb-zone", "dumb-zone?"))
  }

  test("statusline SM117 TranscriptStats.of: sums output_tokens (excl sidechain), human string-content chars") {
    val lines = List(
      """{"type":"assistant","isSidechain":false,"message":{"usage":{"output_tokens":100}}}""",
      """{"type":"assistant","isSidechain":true,"message":{"usage":{"output_tokens":50}}}""",  // sidechain: excluded
      """{"type":"user","message":{"role":"user","content":"hello"}}""",                        // 5 human chars
      """{"type":"user","message":{"role":"user","content":[{"type":"tool_result","content":"x"}]}}""", // tool result: excluded
      """{bad json""",                                                                          // skipped
      """{"type":"system","message":{}}"""
    )
    val s = StatuslineTool.TranscriptStats.of(lines)
    assertEquals(s.agentTokens, 100L) // 50 sidechain excluded
    assertEquals(s.humanChars, 5L)    // "hello"; the array-content (tool-result) user excluded
  }

  test("statusline SM117: tok segment appears when transcript_path is present") {
    val tmp = java.nio.file.Files.createTempFile("tt-transcript", ".jsonl")
    java.nio.file.Files.writeString(tmp,
      """{"type":"assistant","isSidechain":false,"message":{"usage":{"output_tokens":1500000}}}""" + "\n")
    val json = s"""{"context_window":{"used_percentage":20},"transcript_path":"${tmp.toString}"}"""
    val (code, out, _) = run("statusline", json, "--now-ms", "1000000000000")
    assertEquals(code, 0)
    assert(clue(out).contains("tok 1.5M"))
    java.nio.file.Files.deleteIfExists(tmp)
  }

  test("statusline SM117: tired? nudge is OPT-IN (off by default, on past --tired-chars)") {
    val tmp = java.nio.file.Files.createTempFile("tt-transcript2", ".jsonl")
    java.nio.file.Files.writeString(tmp,
      """{"type":"user","message":{"content":"aaaaaaaaaa"}}""" + "\n") // 10 human chars
    val json = s"""{"transcript_path":"${tmp.toString}"}"""
    val (_, off, _) = run("statusline", json, "--now-ms", "1000000000000")
    assert(!clue(off).contains("tired?")) // off without a threshold
    val (_, on, _) = run("statusline", json, "--now-ms", "1000000000000", "--tired-chars", "5")
    assert(clue(on).contains("tired?")) // 10 >= 5 -> gentle nudge
    java.nio.file.Files.deleteIfExists(tmp)
  }

  // --- --help across tools (elaborate, human-friendly per-tool help; 2026-07-13) ---
  test("--help prints elaborate help (tagline + Full reference), exits 0, across tools + insertion shapes") {
    // one per batch + each structurally-distinct insertion: block sys.exit (chrono), stdin-precedence
    // (statusline), the `--` caveat (verify), seqspec dep (svg), top-level Help val (text), return 0
    // (harden), expression-style dispatch (wr), quoted-arg-safe contains (guardcheck).
    val tools = List("chrono", "statusline", "verify", "svg", "text", "harden", "wr", "guardcheck")
    for tool <- tools do
      val (code, out, _) = run(tool, "--help")
      assertEquals(code, 0, s"$tool --help should exit 0")
      assert(out.contains(s"tt $tool —"), s"$tool --help missing tagline; got:\n$out")
      assert(out.contains("Full reference:"), s"$tool --help missing 'Full reference:'; got:\n$out")
  }
  test("statusline --help does not block on stdin (help check precedes the stdin read)") {
    val (code, out, _) = run("statusline", "--help") // no stdin provided; must not hang
    assertEquals(code, 0)
    assert(out.contains("statusLine"), clue(out))
  }
  test("-h short form also prints help and exits 0") {
    val (code, out, _) = run("chrono", "-h")
    assertEquals(code, 0)
    assert(out.contains("tt chrono —"), clue(out))
  }

  // --- harden (Layer-1 deterministic secret scanner; SM042) ---
  test("harden egress: flags AWS key + high-entropy assignment + PEM + sensitive filename; gates placeholders; redacts") {
    val d = os.temp.dir()
    try
      os.write(d / "config.txt",
        "aws_key = AKIAIOSFODNN7EXAMPLE\n" +
          "api_key = aB3xK9mZ2pQ7rT5vW8yDdF1gH4jL6nP\n" +
          "-----BEGIN RSA PRIVATE KEY-----\n" +
          "password = your-placeholder-here\n")
      os.write(d / ".netrc", "machine x login y password z\n")
      val (code, out, _) = run("harden", "egress", d.toString)
      assertEquals(code, 1) // candidates found
      assert(clue(out).contains("aws-access-key"))
      assert(clue(out).contains("secret-assignment(api_key)"))
      assert(clue(out).contains("pem-private-key"))
      assert(clue(out).contains("sensitive-filename"))
      assert(clue(out).contains("AKIA")) // redacted prefix shown
      assert(!clue(out).contains("AKIAIOSFODNN7EXAMPLE")) // the RAW secret is NEVER printed
      assert(!clue(out).contains("your-placeholder")) // low-entropy placeholder gated out
    finally os.remove.all(d)
  }
  test("harden egress: a clean dir reports 0 candidates at exit 0 (the word 'password' in prose is not a hit)") {
    val d = os.temp.dir()
    try
      os.write(d / "readme.md", "# hello\njust some prose mentioning the word password in passing\n")
      val (code, out, _) = run("harden", "egress", d.toString)
      assertEquals(code, 0)
      assert(clue(out).toLowerCase.contains("clean"))
    finally os.remove.all(d)
  }
  test("harden with no args prints usage and exits 2") {
    val (code, out, _) = run("harden")
    assertEquals(code, 2)
    assert(clue(out).toLowerCase.contains("harden"))
  }

  // --- forge release-edit (arg contract; the effectful PATCH path needs a live forge, so only arg errors here) ---
  test("forge release-edit without a repo or tag prints usage and exits 2 (before any token/network)") {
    assertEquals(run("forge", "release-edit")._1, 2)            // no repo
    assertEquals(run("forge", "release-edit", "foo/bar")._1, 2) // repo but no tag
  }
