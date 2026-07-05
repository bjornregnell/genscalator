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
      assert(clue(out).contains("wrote auto sequence diagram"))
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
