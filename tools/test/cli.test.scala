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
    assert(clue(out).contains("genscalator:")) // brand prefix
    assert(clue(out).contains("O4.8")) // model label abbreviated: Opus 4.8 -> O4.8
    assert(clue(out).contains("cost: $12.34"))
    assert(clue(out).contains("ctx-fill: 41%"))
    assert(clue(out).contains("5h-lim: 30%"))
    assert(clue(out).contains("wk-lim: 14%"))
    assert(clue(out).contains("resets: 3d"))
  }
  test("statusline: missing rate_limits degrades gracefully (shows what's present, no crash)") {
    val (code, out, _) = run("statusline", """{"model":{"id":"haiku"},"cost":{"total_cost_usd":0.5}}""")
    assertEquals(code, 0)
    assert(clue(out).contains("haiku"))
    assert(clue(out).contains("$0.50"))
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
    assert(clue(out).contains("wk-lim: 50%"))
    assert(clue(out).contains("resets: 2d"))
  }
  test("statusline: five_hour resets_at renders a fine h/m countdown") {
    val now = 1_000_000_000_000L
    val resetSec = now / 1000L + (2 * 3600 + 34 * 60) // 2h34m later, in SECONDS
    val json = s"""{"rate_limits":{"five_hour":{"used_percentage":68,"resets_at":$resetSec}}}"""
    val (_, out, _) = run("statusline", json, "--now-ms", now.toString)
    assert(clue(out).contains("5h-lim: 68%"))
    assert(clue(out).contains("resets: 2h34m"))
  }
  test("statusline: prepends a HH:MM:SS wall clock, and abbreviates the model label") {
    val (_, out, _) = run("statusline", """{"model":{"display_name":"Fable 5 (1M context)"}}""", "--now-ms", "1000000000000")
    assert("""\d\d:\d\d:\d\d""".r.findFirstIn(out).isDefined, clue(out)) // a HH:MM:SS clock is present
    assert(clue(out).contains("F5 (1M ctx)")) // Fable 5 (1M context) -> F5 (1M ctx)
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
