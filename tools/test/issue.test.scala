//> using file ../project.scala
//> using dep org.scalameta::munit::1.3.4

// Unit tests for issue.scala's PURE core — the reqts/issues/README.md rules made executable
// (issue-032). No filesystem is touched: numbering takes NAMES, the status rewrite takes TEXT,
// and close-argument parsing returns Either (bloop-planClean style), so every refusal the driver
// enforces is pinned here without a fixture tree. The specimen strings are copied from real issue
// files (021's trailing-summary line, 003's "open, GATED", 002's "done (2026-07-21)").

class IssueSuite extends munit.FunSuite:

  // --- file-name convention: issue-NNN-short-snake-case-name.md ---

  test("issueNum parses the convention and rejects strays") {
    assertEquals(Issue.issueNum("issue-032-no-typed-verb-for-the-issue-workflow.md"), Some(32))
    assertEquals(Issue.issueNum("issue-000-how-to-make-issues-on-genscalator.md"), Some(0))
    assertEquals(Issue.issueNum("README.md"), None)
    assertEquals(Issue.issueNum("issue-32-two-digits.md"), None)        // NNN is 3 digits minimum
    assertEquals(Issue.issueNum("issue-1000-scheme-survives.md"), Some(1000))
    assertEquals(Issue.issueNum("issue-033-Upper-Case.md"), None)       // lower-case words only
    assertEquals(Issue.issueNum("issue-033-name.txt"), None)
  }

  test("nnn zero-pads to 3 digits and widens past 999") {
    assertEquals(Issue.nnn(0), "000")
    assertEquals(Issue.nnn(39), "039")
    assertEquals(Issue.nnn(1000), "1000")
  }

  // --- next free number: highest across open AND closed plus one, never reused ---

  test("nextNumber takes the max across BOTH directories' names plus one") {
    val open   = Seq("issue-004-a.md", "issue-038-b.md", "README.md")
    val closed = Seq("issue-023-c.md", "issue-002-d.md")
    assertEquals(Issue.nextNumber(open ++ closed), 39)
    // closed/ holding the highest number must win too — the half a contributor forgets
    assertEquals(Issue.nextNumber(Seq("issue-003-x.md") ++ Seq("issue-040-y.md")), 41)
  }

  test("nextNumber on an empty tracker is 000") {
    assertEquals(Issue.nextNumber(Seq("README.md")), 0)
    assertEquals(Issue.nextNumber(Seq.empty), 0)
  }

  // --- preamble parsing: the wrapped block-quote after the heading ---

  private val specimen =
    """# Issue 023: per-session state is orphaned
      |
      |> status: closed 2026-08-13, fixed by `456f038` (v0.10.2) · labels: toolbox, session, mode · summary: backgrounding a session
      |> and returning made the harness mint a NEW id; the real state
      |> sat orphaned under the old key.
      |
      |## Description
      |
      |Body text with a > quote later that must NOT join the preamble.
      |""".stripMargin

  test("preambleOf joins the first contiguous quote block, stripping the markers") {
    val pre = Issue.preambleOf(specimen).get
    assert(pre.startsWith("status: closed 2026-08-13"), pre)
    assert(pre.endsWith("sat orphaned under the old key."), pre)
    assert(!pre.contains("Description"), pre)
  }

  test("preambleOf is None when no block quote exists") {
    assertEquals(Issue.preambleOf("# Heading\n\nJust prose.\n"), None)
  }

  test("status/labels/summary fields come out of a joined preamble") {
    val pre = Issue.preambleOf(specimen).get
    assertEquals(Issue.statusField(pre), Some("closed 2026-08-13, fixed by `456f038` (v0.10.2)"))
    assertEquals(Issue.labelsField(pre), Some("toolbox, session, mode"))
    val s = Issue.summaryField(pre).get
    assert(s.startsWith("backgrounding a session"), s)
    assert(s.endsWith("under the old key."), s) // summary spans the wrapped lines
  }

  test("statusField requires the first segment to BE status (never grabs a later field)") {
    assertEquals(Issue.statusField("labels: a, b · status: open"), None)
  }

  test("statusHead takes the first word: open/parked/closed/done specimens") {
    assertEquals(Issue.statusHead("open"), "open")
    assertEquals(Issue.statusHead("open, GATED"), "open")
    assertEquals(Issue.statusHead("parked"), "parked")
    assertEquals(Issue.statusHead("closed 2026-08-13, fixed by `456f038`"), "closed")
    assertEquals(Issue.statusHead("done (2026-07-21)"), "done")
    assertEquals(Issue.statusHead(""), "")
  }

  test("the closed-state family matches closed/README.md's endings") {
    for s <- Seq("closed", "done", "wontfix", "duplicate") do assert(Issue.closedStates(s), s)
    for s <- Seq("open", "parked", "triage", "") do assert(!Issue.closedStates(s), s)
  }

  // --- the status rewrite: the one edit close performs ---

  test("rewriteStatus swaps ONLY the finer-state field, keeping labels/summary and the body") {
    val out = Issue.rewriteStatus(
      "# T\n\n> status: open · labels: a, b · summary: text\n> wrapped line\n\n## Description\nbody\n",
      "closed 2026-08-16, fixed by `abc1234`").toOption.get
    assert(out.contains("> status: closed 2026-08-16, fixed by `abc1234` · labels: a, b · summary: text"), out)
    assert(out.contains("> wrapped line"), out)
    assert(out.contains("## Description\nbody\n"), out)
    assert(!out.contains("status: open"), out)
  }

  test("rewriteStatus handles a finer state carrying commas (open, GATED)") {
    val out = Issue.rewriteStatus("> status: open, GATED · labels: native · summary: s\n", "closed 2026-08-16, wontfix").toOption.get
    assertEquals(out, "> status: closed 2026-08-16, wontfix · labels: native · summary: s\n")
  }

  test("rewriteStatus without a · replaces the line's remainder") {
    assertEquals(Issue.rewriteStatus("> status: open", "closed 2026-08-16, x"), Right("> status: closed 2026-08-16, x"))
  }

  test("rewriteStatus fails loudly when no status line exists") {
    assert(Issue.rewriteStatus("# T\n\nno preamble\n", "closed").isLeft)
  }

  test("closedStatus renders the house wording") {
    assertEquals(Issue.closedStatus("2026-08-16", "fixed by `456f038`"), "closed 2026-08-16, fixed by `456f038`")
    assertEquals(Issue.closedStatus("2026-08-16", "wontfix"), "closed 2026-08-16, wontfix")
  }

  // --- close-argument parsing: preview by default, exactly one ending, no invented dates ---

  test("close with --fixed-by parses; preview is the DEFAULT") {
    val o = Issue.parseClose(List("23", "--fixed-by", "456f038")).toOption.get
    assertEquals(o.num, 23)
    assertEquals(o.end, "fixed by `456f038`")
    assertEquals(o.yes, false)
    assertEquals(o.date, None)
  }

  test("zero-padded and bare numbers both parse ('032' == '32')") {
    assertEquals(Issue.parseClose(List("032", "--as", "wontfix")).map(_.num), Right(32))
    assertEquals(Issue.parseClose(List("32", "--as", "wontfix")).map(_.num), Right(32))
  }

  test("--yes and --date are accepted in any order") {
    val o = Issue.parseClose(List("--yes", "23", "--date", "2026-08-16", "--fixed-by", "v0.10.2")).toOption.get
    assertEquals(o.yes, true)
    assertEquals(o.date, Some("2026-08-16"))
    assertEquals(o.end, "fixed by `v0.10.2`")
  }

  test("exactly one of --fixed-by / --as: neither, or both, is refused") {
    assert(Issue.parseClose(List("23")).isLeft)
    assert(Issue.parseClose(List("23", "--fixed-by", "abc", "--as", "wontfix")).isLeft)
  }

  test("a malformed date is refused, never reinterpreted") {
    assert(Issue.parseClose(List("23", "--fixed-by", "abc", "--date", "16/8/2026")).isLeft)
    assert(Issue.parseClose(List("23", "--fixed-by", "abc", "--date", "2026-8-16")).isLeft)
  }

  test("a ref with spaces or backticks is refused (it would break the markdown)") {
    assert(Issue.parseClose(List("23", "--fixed-by", "abc def")).isLeft)
    assert(Issue.parseClose(List("23", "--fixed-by", "`abc`")).isLeft)
  }

  test("--as text may not smuggle the preamble's own separators in") {
    assert(Issue.parseClose(List("23", "--as", "wontfix · labels: x")).isLeft)
    assert(Issue.parseClose(List("23", "--as", "with `ticks`")).isLeft)
    assert(Issue.parseClose(List("23", "--as", "  ")).isLeft)
  }

  test("a missing number or a stray argument is refused") {
    assert(Issue.parseClose(List("--fixed-by", "abc")).isLeft)
    assert(Issue.parseClose(List("23", "--fixed-by", "abc", "extra")).isLeft)
    assert(Issue.parseClose(List("-5", "--fixed-by", "abc")).isLeft)
  }

  // --- list-line rendering ---

  test("listLine shows number, finer state, labels, summary") {
    val line = Issue.listLine(Issue.Row(32, "open", "open", "toolbox, reqts", "no typed verb for the issue workflow"))
    assert(line.startsWith("032"), line)
    assert(line.contains("[toolbox, reqts]"), line)
    assert(line.contains("no typed verb"), line)
    assert(!line.contains("⚠"), line)
  }

  test("listLine prefers the finer state word (parked in open/ is fine, no warning)") {
    val line = Issue.listLine(Issue.Row(1, "open", "parked", "refactor", "s"))
    assert(line.contains("parked"), line)
    assert(!line.contains("⚠"), line)
  }

  test("listLine flags a preamble that disagrees with its directory — both directions") {
    assert(Issue.listLine(Issue.Row(9, "open", "closed", "l", "s")).contains("⚠"), "closed-in-open/")
    assert(Issue.listLine(Issue.Row(9, "closed", "open", "l", "s")).contains("⚠"), "open-in-closed/")
    assert(Issue.listLine(Issue.Row(9, "closed", "done", "l", "s")).contains("⚠") == false, "done is a closed state")
    assert(Issue.listLine(Issue.Row(9, "open", "", "l", "s")).contains("no status"), "missing status field")
  }

  test("truncate caps long summaries visibly, leaves short ones alone") {
    assertEquals(Issue.truncate("short"), "short")
    val long = "x" * 100
    val cut = Issue.truncate(long)
    assert(cut.length <= 72, cut.length.toString)
    assert(cut.endsWith("…"), cut)
  }
