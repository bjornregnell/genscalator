//> using scala 3.8.4
//> using dep org.scalameta::munit::1.3.3

// Unit tests for the md-fmt markdown reflow (md-fmt.scala). Like the other pure tools its logic lives in an
// `object MdFmt` (the @main just does I/O), so it sits on the toolbox MAIN scope and is importable in-process
// here — fast + hermetic, no subprocess. Covers the two load-bearing invariants (content-preservation +
// idempotency), structure pass-through, unbreakable code/link tokens, the hazard guard, and CLI parsing.
//   run from the genscalator root:  scala-cli test tools

class MdFmtSuite extends munit.FunSuite:

  // --- fixtures (each exercises a different markdown construct) ---
  val prose =
    "The quick brown fox jumps over the lazy dog and then the dog gets up and runs across the big green " +
    "field while the sun sets slowly behind the distant blue hills far away."
  val nestedList =
    """- first item that is quite long and really should wrap across several lines at a small target width
      |  - a nested item, also long enough that it needs to wrap when the width is deliberately made small
      |- second top-level item, short
      |1. an ordered item that is also long enough to require wrapping at the small test width used below""".stripMargin
  // NB: built without stripMargin on purpose — stripMargin's own delimiter is `|`, which would eat the
  // leading pipe of each table row and corrupt the fixture.
  val table = "| name | value |\n|------|-------|\n| a    | 1     |\n| bb   | 22    |"
  val codeFence =
    """Some intro prose before the fence that is long enough to wrap at a small width for the test here.
      |
      |```scala
      |val x = someVeryLongExpression + thatWouldExceed + theTargetWidth + butMustNotBeTouchedAtAll
      |```
      |
      |Trailing prose after the fence, also long enough to wrap at the small width chosen for the test.""".stripMargin
  val inlineCodeAndLink =
    "Here is a paragraph with `an inline code span that is fairly long` and also " +
    "[a link with visible text](http://example.com/a/very/long/path/that/exceeds/the/width) inline."
  val blockquote =
    """> a quoted paragraph that is long enough that it will need to be wrapped across more than one line
      |> when reflowed at the deliberately small width used in these tests here today""".stripMargin
  val heading = "## A Section Heading That Is Deliberately Longer Than The Small Test Width Used Below"
  val hr = "---"

  val fixtures = List(prose, nestedList, table, codeFence, inlineCodeAndLink, blockquote, heading, hr)

  // --- INVARIANT 1: content preservation (words never change; only breaks/indent/`>` move) ---
  test("content is preserved (identical after stripping whitespace + `>`) across all fixtures") {
    for (f, i) <- fixtures.zipWithIndex; w <- List(20, 40, 80) do
      val out = MdFmt.reflow(f, w)
      assert(clue(MdFmt.contentPreserved(f, out)), s"content changed for fixture #$i at width $w")
  }

  // --- INVARIANT 2: idempotency (reflowing an already-reflowed doc is a no-op) ---
  test("reflow is idempotent across all fixtures") {
    for (f, i) <- fixtures.zipWithIndex; w <- List(20, 40, 80) do
      val once = MdFmt.reflow(f, w)
      assertEquals(MdFmt.reflow(once, w), once, s"not idempotent for fixture #$i at width $w")
  }

  // --- structure pass-through: special lines survive verbatim ---
  test("headings, HR, and table rows pass through unchanged") {
    assertEquals(MdFmt.reflow(heading, 20), heading)
    assertEquals(MdFmt.reflow(hr, 20), hr)
    assertEquals(MdFmt.reflow(table, 8), table) // narrow width must NOT split table rows
  }
  test("code fence contents are never reflowed, even when wider than the target width") {
    val out = MdFmt.reflow(codeFence, 30)
    assert(clue(out).contains(
      "val x = someVeryLongExpression + thatWouldExceed + theTargetWidth + butMustNotBeTouchedAtAll"))
    assert(out.contains("```scala"))
  }

  // --- unbreakable tokens: inline code spans and links never split ---
  test("inline `code` and [links](url) are kept whole even below their own length in width") {
    val out = MdFmt.reflow(inlineCodeAndLink, 20)
    assert(clue(out).contains("`an inline code span that is fairly long`"), "code span was split")
    assert(clue(out).contains("[a link with visible text](http://example.com/a/very/long/path/that/exceeds/the/width)"),
      "link was split")
  }

  // --- width: breakable prose respects the target (short words only, so every line can fit) ---
  test("no reflowed prose line exceeds the target width when all words are short") {
    val shortWords = (1 to 40).map(_ => "word").mkString(" ")
    val w = 20
    for line <- MdFmt.reflow(shortWords, w).linesIterator do
      assert(line.length <= w, s"line exceeds width $w: '$line' (${line.length})")
  }

  // --- hazard guard: wrapping must never create a line that starts a NEW markdown block ---
  test("a wrapped continuation never starts with a bullet/heading/ordered marker") {
    // pure prose whose words include tokens that WOULD start a block if left at column 0
    val hazardy = "alpha beta gamma delta - epsilon zeta # eta theta 1. iota kappa lambda mu nu xi omicron pi"
    for w <- List(8, 12, 16, 24) do
      for line <- MdFmt.reflow(hazardy, w).linesIterator do
        val t = line.stripLeading
        assert(!(t.startsWith("- ") || t.startsWith("* ") || t.startsWith("+ ")),
          s"created a bullet at width $w: '$line'")
        assert(!t.matches("""#{1,6} .*"""), s"created a heading at width $w: '$line'")
        assert(!t.matches("""\d+\. .*"""), s"created an ordered item at width $w: '$line'")
  }

  // --- blockquote: `>` prefix is re-applied to every wrapped line ---
  test("blockquote reflow keeps every output line prefixed with `> `") {
    val out = MdFmt.reflow(blockquote, 20)
    assert(out.linesIterator.forall(_.startsWith("> ")), s"a quote line lost its prefix:\n$out")
    assert(out.linesIterator.size > 1, "expected the quote to wrap into multiple lines at width 20")
  }

  // --- list markers + the author's continuation indent are preserved ---
  test("list markers survive and nested-item continuations stay indented under their marker") {
    val out = MdFmt.reflow(nestedList, 30)
    assert(clue(out).linesIterator.exists(_.startsWith("- first item")), "top bullet marker lost")
    assert(clue(out).linesIterator.exists(_.startsWith("  - a nested item")), "nested bullet marker/indent lost")
    assert(clue(out).linesIterator.exists(_.startsWith("1. an ordered")), "ordered marker lost")
  }

  // --- tokenize: code spans and links are single tokens; plain words split on spaces ---
  test("tokenize keeps `code` and [text](url) as one token each") {
    val toks = MdFmt.tokenize("a `b c` d [e f](http://x/y) g")
    assertEquals(toks, Vector("a", "`b c`", "d", "[e f](http://x/y)", "g"))
  }

  // --- CLI parsing (pure) ---
  test("parseArgs defaults: width 80, no write, single positional") {
    assertEquals(MdFmt.parseArgs(List("f.md")), Right(MdFmt.Opts(List("f.md"), 80, false)))
  }
  test("parseArgs reads --line-width and --write, order-independent") {
    assertEquals(MdFmt.parseArgs(List("f.md", "--line-width", "82")), Right(MdFmt.Opts(List("f.md"), 82, false)))
    assertEquals(MdFmt.parseArgs(List("--write", "f.md")), Right(MdFmt.Opts(List("f.md"), 80, true)))
    assertEquals(MdFmt.parseArgs(List("--line-width", "100", "f.md")), Right(MdFmt.Opts(List("f.md"), 100, false)))
  }
  test("parseArgs rejects a non-positive / non-numeric width and unknown options") {
    assert(MdFmt.parseArgs(List("f.md", "--line-width", "0")).isLeft)
    assert(MdFmt.parseArgs(List("f.md", "--line-width", "x")).isLeft)
    assert(MdFmt.parseArgs(List("f.md", "--line-width")).isLeft)
    assert(MdFmt.parseArgs(List("f.md", "--bogus")).isLeft)
  }
