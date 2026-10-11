//> using file ../project.scala
//> using dep org.scalameta::munit::1.3.4

// Unit tests for issue.scala's PURE core — the reqts/issues/README.md rules made executable
// (issue-032; identity per issue 073). No filesystem is touched: identity and minting take NAMES
// and a pinned date or clock, the status rewrite takes TEXT,
// and close-argument parsing returns Either (bloop-planClean style), so every refusal the driver
// enforces is pinned here without a fixture tree. The specimen strings are copied from real issue
// files (021's trailing-summary line, 003's "open, GATED", 002's "done (2026-07-21)").

class IssueSuite extends munit.FunSuite:

  // --- legacy file-name convention: issue-NNN-name.md (frozen at 073 by issue 073) ---

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

  // --- issue identity (issue 073): legacy NNN (frozen) and YYMMDD-slug, side by side ---

  import Issue.IssueId.{Dated, Legacy}
  import java.time.{Clock, Instant, LocalDate, ZoneId}

  private val d261011 = LocalDate.of(2026, 10, 11)

  test("issueId parses both forms; a 6-digit date prefix is never read as a legacy number") {
    assertEquals(Issue.issueId("issue-073-issue-identity.md"), Some(Legacy(73)))
    assertEquals(Issue.issueId("issue-261011-issue-identity.md"), Some(Dated(d261011, "issue-identity")))
    assertEquals(Issue.issueNum("issue-261011-issue-identity.md"), None)   // dated issues have no number
    assertEquals(Issue.issueId("issue-261399-bad-date.md"), None)          // month 13 is not a date
    assertEquals(Issue.issueId("issue-261011-one-two-three-four-five.md"), None) // slug is at most 4 words
    assertEquals(Issue.issueId("issue-261011-Upper.md"), None)
    assertEquals(Issue.issueId("README.md"), None)
  }

  test("render is the citation form: zero-padded number, or YYMMDD-slug") {
    assertEquals(Legacy(7).render, "007")
    assertEquals(Dated(d261011, "issue-identity").render, "261011-issue-identity")
    assertEquals(Dated(LocalDate.of(2027, 1, 2), "x").render, "270102-x")
  }

  test("mixed legacy + new: legacy block first (by number), then new by day then slug, = plain file-name sort") {
    val names = Vector(
      "issue-261012-aaa.md", "issue-073-issue-identity.md", "issue-261011-zeta.md", "issue-000-zero.md",
      "issue-270101-next-year.md", "issue-261011-alpha.md", "issue-032-no-typed-verb.md")
    val byOrdering = names.flatMap(n => Issue.issueId(n).map(_ -> n)).sortBy(_._1)(using Issue.idOrdering).map(_._2)
    assertEquals(byOrdering, names.sorted)   // the tool's ordering agrees with `ls`
    assertEquals(byOrdering, Vector(
      "issue-000-zero.md", "issue-032-no-typed-verb.md", "issue-073-issue-identity.md",
      "issue-261011-alpha.md", "issue-261011-zeta.md", "issue-261012-aaa.md", "issue-270101-next-year.md"))
  }

  test("parseId: legacy numbers in any padding, dated IDs, and a bare date refused (not unique)") {
    assertEquals(Issue.parseId("23"), Right(Legacy(23)))
    assertEquals(Issue.parseId("073"), Right(Legacy(73)))
    assertEquals(Issue.parseId("261011-issue-identity"), Right(Dated(d261011, "issue-identity")))
    assert(Issue.parseId("261011").left.exists(_.contains("bare date")))
    assert(Issue.parseId("261399-x").isLeft)
    assert(Issue.parseId("Issue-Identity").isLeft)
  }

  test("validateSlug: 1 to 4 lower-case kebab-case words, nothing else") {
    for ok <- Seq("x", "issue-identity", "a-b-c-d", "utc-2") do assertEquals(Issue.validateSlug(ok), Right(ok))
    for bad <- Seq("", "a-b-c-d-e", "Upper", "snake_case", "-lead", "trail-", "two--dashes", "sp ace", "ä")
    do assert(Issue.validateSlug(bad).isLeft, bad)
  }

  // --- minting: the UTC date, with the clock pinned ---

  test("utcToday is the UTC date even when the clock's zone is not UTC (both sides of midnight)") {
    // 23:30 UTC on the 11th is already the 12th in Stockholm: the ID must say the 11th
    val lateUtc = Clock.fixed(Instant.parse("2026-10-11T23:30:00Z"), ZoneId.of("Europe/Stockholm"))
    assertEquals(Issue.utcToday(lateUtc), d261011)
    // 00:30 UTC on the 12th is still the 11th in Los Angeles: the ID must say the 12th
    val earlyUtc = Clock.fixed(Instant.parse("2026-10-12T00:30:00Z"), ZoneId.of("America/Los_Angeles"))
    assertEquals(Issue.utcToday(earlyUtc), LocalDate.of(2026, 10, 12))
  }

  test("utcToday ignores a non-UTC JVM default zone (the driver passes the system default clock)") {
    val saved = java.util.TimeZone.getDefault
    try
      java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Pacific/Kiritimati")) // UTC+14
      val clock = Clock.fixed(Instant.parse("2026-10-11T12:00:00Z"), ZoneId.systemDefault())
      assertEquals(clock.getZone.getId, "Pacific/Kiritimati")      // the local date there is the 12th
      assertEquals(Issue.utcToday(clock), d261011)
    finally java.util.TimeZone.setDefault(saved)
  }

  test("planNew with a pinned date creates issue-YYMMDD-slug.md in the house format") {
    val (name, text) = Issue.planNew(Seq("issue-073-issue-identity.md"), Seq.empty, "issue-identity",
      Some("make IDs unique"), d261011, d261011).toOption.get
    assertEquals(name, "issue-261011-issue-identity.md")
    assert(text.startsWith("# Issue 261011-issue-identity: make IDs unique\n"), text)
    assert(text.contains("\n> status: open 2026-10-11 · labels: · summary:\n"), text)
    for s <- Seq("## Description", "## How to reproduce it","## Acceptance sketch", "## Discussion") do
      assert(text.contains(s"\n$s\n"), s)
    // the template's preamble reads back through the same parsers list and close use
    val pre = Issue.preambleOf(text).get
    assertEquals(Issue.statusField(pre).map(Issue.statusHead), Some("open"))
    assert(Issue.rewriteStatus(text, "closed 2026-10-12, wontfix").isRight)
  }

  test("planNew without --title writes TODO as the title") {
    val (_, text) = Issue.planNew(Seq.empty, Seq.empty, "x", None, d261011, d261011).toOption.get
    assert(text.startsWith("# Issue 261011-x: TODO\n"), text)
  }

  test("planNew: same day + same slug is refused; same day + a different slug succeeds") {
    val open = Seq("issue-261011-issue-identity.md")
    val dup = Issue.planNew(open, Seq.empty, "issue-identity", None, d261011, d261011)
    assert(dup.left.exists(_.contains("already exists")), dup)
    assertEquals(Issue.planNew(open, Seq.empty, "other-slug", None, d261011, d261011).map(_._1),
      Right("issue-261011-other-slug.md"))
    // same slug on another day is a different ID
    val next = LocalDate.of(2026, 10, 12)
    assertEquals(Issue.planNew(open, Seq.empty, "issue-identity", None, next, next).map(_._1),
      Right("issue-261012-issue-identity.md"))
  }

  test("planNew refuses an ID already in closed/ (IDs are never reused)") {
    val r = Issue.planNew(Seq.empty, Seq("issue-261011-done-thing.md"), "done-thing", None, d261011, d261011)
    assert(r.left.exists(_.contains("closed/")), r)
  }

  test("planNew refuses a bad slug, a bad title, and a date after today") {
    assert(Issue.planNew(Seq.empty, Seq.empty, "Bad_Slug", None, d261011, d261011).isLeft)
    assert(Issue.planNew(Seq.empty, Seq.empty, "a-b-c-d-e", None, d261011, d261011).isLeft)
    assert(Issue.planNew(Seq.empty, Seq.empty, "ok", Some("  "), d261011, d261011).isLeft)
    assert(Issue.planNew(Seq.empty, Seq.empty, "ok", Some("two\nlines"), d261011, d261011).isLeft)
    assert(Issue.planNew(Seq.empty, Seq.empty, "ok", None, LocalDate.of(2026, 10, 12), d261011).isLeft)
    // an earlier day (a draft conceived before minting) is fine
    assert(Issue.planNew(Seq.empty, Seq.empty, "ok", None, LocalDate.of(2026, 10, 1), d261011).isRight)
  }

  test("parseNew: one slug, optional --title/--date/--repo in any order") {
    val o = Issue.parseNew(List("--title", "a title", "my-slug", "--date", "2026-10-01", "--repo", "/r")).toOption.get
    assertEquals(o, Issue.NewOpts("my-slug", Some("a title"), Some(LocalDate.of(2026, 10, 1)), Some("/r")))
    assert(Issue.parseNew(Nil).isLeft)                                      // no slug
    assert(Issue.parseNew(List("a", "b")).isLeft)                           // two slugs
    assert(Issue.parseNew(List("a", "--date", "2026-13-01")).isLeft)        // not a date
    assert(Issue.parseNew(List("a", "--date", "261011")).isLeft)            // wrong format
  }

  test("ageDays: whole days since a dated ID; legacy IDs have no age") {
    assertEquals(Issue.ageDays(Dated(d261011, "x"), LocalDate.of(2026, 10, 21)), Some(10L))
    assertEquals(Issue.ageDays(Dated(d261011, "x"), d261011), Some(0L))
    assertEquals(Issue.ageDays(Legacy(73), d261011), None)
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
    assertEquals(o.id, Legacy(23))
    assertEquals(o.end, "fixed by `456f038`")
    assertEquals(o.yes, false)
    assertEquals(o.date, None)
  }

  test("zero-padded and bare numbers both parse ('032' == '32')") {
    assertEquals(Issue.parseClose(List("032", "--as", "wontfix")).map(_.id), Right(Legacy(32)))
    assertEquals(Issue.parseClose(List("32", "--as", "wontfix")).map(_.id), Right(Legacy(32)))
  }

  test("close takes a dated YYMMDD-slug ID, and refuses a bare date") {
    assertEquals(Issue.parseClose(List("261011-issue-identity", "--as", "wontfix")).map(_.id),
      Right(Dated(d261011, "issue-identity")))
    assert(Issue.parseClose(List("261011", "--as", "wontfix")).left.exists(_.contains("bare date")))
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
    val line = Issue.listLine(Issue.Row(Legacy(32), "open", "open", "toolbox, reqts", "no typed verb for the issue workflow"))
    assert(line.startsWith("032"), line)
    assert(line.contains("[toolbox, reqts]"), line)
    assert(line.contains("no typed verb"), line)
    assert(!line.contains("⚠"), line)
  }

  test("listLine prefers the finer state word (parked in open/ is fine, no warning)") {
    val line = Issue.listLine(Issue.Row(Legacy(1), "open", "parked", "refactor", "s"))
    assert(line.contains("parked"), line)
    assert(!line.contains("⚠"), line)
  }

  test("listLine flags a preamble that disagrees with its directory — both directions") {
    assert(Issue.listLine(Issue.Row(Legacy(9), "open", "closed", "l", "s")).contains("⚠"), "closed-in-open/")
    assert(Issue.listLine(Issue.Row(Legacy(9), "closed", "open", "l", "s")).contains("⚠"), "open-in-closed/")
    assert(Issue.listLine(Issue.Row(Legacy(9), "closed", "done", "l", "s")).contains("⚠") == false, "done is a closed state")
    assert(Issue.listLine(Issue.Row(Legacy(9), "open", "", "l", "s")).contains("no status"), "missing status field")
  }

  test("listLine shows a dated ID with its age in days; a legacy line is unchanged") {
    val line = Issue.listLine(Issue.Row(Dated(d261011, "issue-identity"), "open", "open", "meta", "s", Some(3L)))
    assert(line.startsWith("261011-issue-identity (3d)  open"), line)
    assert(Issue.listLine(Issue.Row(Legacy(2), "closed", "done", "x", "s")).startsWith("002  done"))
  }

  test("truncate caps long summaries visibly, leaves short ones alone") {
    assertEquals(Issue.truncate("short"), "short")
    val long = "x" * 100
    val cut = Issue.truncate(long)
    assert(cut.length <= 72, cut.length.toString)
    assert(cut.endsWith("…"), cut)
  }
