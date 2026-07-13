//> using scala 3.8.4
//> using jvm 21
//> using file lib.scala
//> using file mdparse.scala

// md-fmt — markdown-aware line reflow to a target width (SM012 first cut).
// Reflows prose / list-item / blockquote blocks to <= --line-width columns while
// PRESERVING markdown structure: headings, ``` fences, |tables|, --- rules, blank
// lines, blockquote `>` prefixes, list markers + the author's continuation indent.
// Never breaks inside `inline code` or [links](url). Idempotent. Content-preserving:
// an internal guard REFUSES any result that changes the text beyond whitespace + `>`.
//   tt md-fmt <file>                 reflow to stdout at the default width (80)
//   tt md-fmt <file> --line-width N  target N columns
//   tt md-fmt <file> --write         rewrite the file in place (guarded)
// PURE by default (reads, computes, prints); --write is the one explicit, guarded effect.
//
// Helpers live INSIDE `object MdFmt` so top-level names don't collide when the toolbox
// compiles as one unit; only the @main is top-level. See skills/scala-style §1.
// Deferred to later SM012: fuzzy/semantic-line-break modes, the post-edit-hook wiring,
// `:shortcode:` -> emoji. Known edge case (needs author intent, not fixed here): a
// hard-wrapped continuation that begins `+ ` or `## ` re-parses as a bullet/heading.
import agenttools.Lib
import scala.collection.mutable.ArrayBuffer

object MdFmt:
  val DefaultWidth = 80

  /** Display width in code points (so a multi-byte char counts as one column). */
  def cpLen(s: String): Int = s.codePointCount(0, s.length)

  /** Split into words, keeping `code spans` and [text](url) links as single unbreakable tokens. */
  def tokenize(s: String): Vector[String] =
    val toks = ArrayBuffer[String]()
    val n = s.length; var i = 0
    while i < n do
      while i < n && s(i) == ' ' do i += 1
      if i < n then
        val start = i; var done = false
        while i < n && !done do
          val c = s(i)
          if c == ' ' then done = true
          else if c == '`' then
            i += 1
            while i < n && s(i) != '`' do i += 1
            if i < n then i += 1
          else if c == '[' then
            i += 1
            while i < n && s(i) != ']' do i += 1
            if i < n && i + 1 < n && s(i) == ']' && s(i + 1) == '(' then
              i += 2
              while i < n && s(i) != ')' do i += 1
              if i < n then i += 1
          else i += 1
        toks += s.substring(start, i)
    toks.toVector

  /** A token that, alone at column 0 followed by a space, would START a new markdown block
    * (heading / bullet / quote / ordered item). Never let a wrapped line begin with one, or the
    * reflow silently changes the document structure. */
  def isHazard(t: String): Boolean =
    t.matches("#{1,6}") || t == "-" || t == "*" || t == "+" || t == ">" || t.matches("""\d+\.""")

  /** Greedy word-wrap of `text` to width `w`, with a first-line prefix (marker/indent) and a hanging
    * prefix for continuation lines (the author's continuation indent). */
  def wrap(firstPrefix: String, hangPrefix: String, text: String, w: Int): Vector[String] =
    val toks = tokenize(text)
    if toks.isEmpty then return Vector(firstPrefix)
    val res = ArrayBuffer[String]()
    var cur = new StringBuilder(firstPrefix)
    var curLen = cpLen(firstPrefix)
    var atPrefix = true
    for tok <- toks do
      val tLen = cpLen(tok)
      if atPrefix then { cur.append(tok); curLen += tLen; atPrefix = false }
      else if curLen + 1 + tLen <= w then { cur.append(' ').append(tok); curLen += 1 + tLen }
      else if cpLen(hangPrefix) == 0 && isHazard(tok) then
        { cur.append(' ').append(tok); curLen += 1 + tLen } // keep a hazard token off a line-start
      else
        res += cur.toString
        cur = new StringBuilder(hangPrefix); cur.append(tok)
        curLen = cpLen(hangPrefix) + tLen
    res += cur.toString
    res.toVector

  /** Render a parsed block stream back to reflowed markdown at width `w`, joined by '\n' (no trailing '\n').
    * Wrappable blocks (Quote/Para/Item) are re-wrapped; pass-through blocks are emitted verbatim. */
  def renderReflow(blocks: Vector[MdParse.Block], w: Int): String =
    import MdParse.Block.*
    val out = ArrayBuffer[String]()
    blocks.foreach {
      case Blank                             => out += ""
      case Fence(lines)                      => lines.foreach(out += _)
      case Heading(raw)                      => out += raw
      case Rule(raw)                         => out += raw
      case Table(rows)                       => rows.foreach(out += _)
      case Quote(text)                       => wrap("> ", "> ", text, w).foreach(out += _)
      case Para(lead, hang, text)            => wrap(lead, hang, text, w).foreach(out += _)
      case Item(lead, marker, hang, text, _) => wrap(lead + marker, hang, text, w).foreach(out += _)
    }
    out.mkString("\n")

  /** Pure reflow: markdown text -> reflowed markdown at width `w` (no trailing '\n'). Parses via the shared
    * `MdParse` front-end, then re-wraps — one parser, two renderers (this reflow + ssg's HTML). */
  def reflow(input: String, w: Int): String =
    renderReflow(MdParse.parse(input), w)

  /** True iff a and b are identical after removing all whitespace and blockquote `>` markers —
    * i.e. the reflow only moved line breaks / indentation / quote prefixes around, changing no words. */
  def contentPreserved(a: String, b: String): Boolean =
    def norm(s: String) = s.filterNot(c => c.isWhitespace || c == '>')
    norm(a) == norm(b)

  /** Parsed CLI: the positional args, the target width, and whether to write in place. `Left` = a usage
    * error message. Pure (no I/O) so it is unit-testable. */
  case class Opts(positional: List[String], width: Int, write: Boolean)
  def parseArgs(args: List[String]): Either[String, Opts] =
    def loop(rest: List[String], pos: List[String], w: Int, wr: Boolean): Either[String, Opts] =
      rest match
        case Nil                          => Right(Opts(pos.reverse, w, wr))
        case "--write" :: t               => loop(t, pos, w, true)
        case "--line-width" :: n :: t     =>
          n.toIntOption.filter(_ > 0) match
            case Some(v) => loop(t, pos, v, wr)
            case None    => Left(s"--line-width needs a positive integer, got: $n")
        case "--line-width" :: Nil        => Left("--line-width needs a value")
        case flag :: _ if flag.startsWith("--") => Left(s"unknown option: $flag")
        case x :: t                       => loop(t, x :: pos, w, wr)
    loop(args, Nil, DefaultWidth, false)

  private val Help: String =
    """tt md-fmt — markdown-aware line reflow to a target width (pure by default)
      |
      |Re-wraps prose, list items, and blockquotes to a target column width while PRESERVING
      |markdown structure: headings, code fences, tables, --- rules, blank lines, blockquote
      |prefixes, list markers, and the author's continuation indent. Never breaks inside
      |`inline code` or [links](url). Idempotent. A content-preservation guard REFUSES any
      |result (and any --write) that would change the text beyond whitespace and quote
      |markers — so it can only re-flow, never re-word.
      |
      |Usage:
      |  md-fmt <file>                  reflow to stdout at the default width (80)
      |
      |Flags:
      |  --line-width N                 target N columns (default 80)
      |  --write                        rewrite the file in place (content-guarded); without
      |                                 it the tool only prints — writing is the one effect
      |
      |Examples:
      |  tt md-fmt notes/plan.md --line-width 82           # print reflowed at 82 cols
      |  tt md-fmt notes/plan.md --line-width 82 --write   # ... and rewrite the file in place
      |
      |Full reference: tools/README.md""".stripMargin

  private val Usage =
    """md-fmt — markdown-aware line reflow (pure by default)
      |  md-fmt <file>                 reflow to stdout at the default width (80)
      |  md-fmt <file> --line-width N  target N columns
      |  md-fmt <file> --write         rewrite the file in place (content-guarded)""".stripMargin

  def dispatch(args: String*): Unit =
    if args.contains("--help") || args.contains("-h") then { println(Help); sys.exit(0) }
    parseArgs(args.toList) match
      case Left(err) => System.err.println(s"md-fmt: $err"); System.err.println(Usage); sys.exit(2)
      case Right(Opts(file :: Nil, w, write)) =>
        val input = Lib.readUtf8(file)
        val result = reflow(input, w)
        if !contentPreserved(input, result) then
          System.err.println("md-fmt: REFUSING — reflow would change the text beyond whitespace/`>` " +
            "(this is a bug; report the input). No output written.")
          sys.exit(2)
        if write then
          java.nio.file.Files.write(java.nio.file.Path.of(file), (result + "\n").getBytes("UTF-8"))
          System.err.println(s"md-fmt: wrote $file at width $w")
        else
          print(result + "\n")
      case Right(_) => println(Usage); sys.exit(2)

@main def formatMarkdown(args: String*): Unit = MdFmt.dispatch(args*)
