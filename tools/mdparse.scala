// (no version include: mainless helper — inherits it from its includer; see project.scala)

// mdparse — the SHARED markdown block parser for the toolbox (SM019 step 1). It lifts the block
// classification + gathering that used to live inside md-fmt's reflow loop into ONE reusable `parse(md) ->
// Vector[Block]`, so md-fmt (reflow back-end) and ssg (HTML back-end) consume the same front-end: one parser,
// two renderers. PURE, zero-dep, no @main — like lib.scala it sits on the toolbox MAIN scope and is pulled
// into a standalone tool run via `//> using file mdparse.scala`. See skills/scala-style §1.
//
// The Block model stores exactly what the reflow renderer needs (prefixes + joined text for wrappable blocks;
// verbatim lines for pass-through blocks) so that md-fmt's output is byte-identical to before the lift; the
// HTML renderer re-derives the finer structure (heading level, table cells, inline spans) from those fields.
// Scope = the GitHub-flavored-markdown subset we actually use; extend on need.
import scala.collection.mutable.ArrayBuffer

object MdParse:

  /** A parsed markdown block. Wrappable blocks (Quote/Para/Item) carry the prefixes + joined text the reflow
    * renderer needs; pass-through blocks (Fence/Heading/Rule/Table) carry their verbatim source line(s). */
  enum Block:
    case Blank
    case Fence(lines: Vector[String])                                   // ``` ... ``` incl. both fence lines, verbatim
    case Heading(raw: String)                                           // a single `#..` line, verbatim
    case Rule(raw: String)                                              // a single `---` / `***` / `___` line, verbatim
    case Table(rows: Vector[String])                                    // contiguous `|` rows, verbatim
    case Quote(text: String)                                            // joined inner text (the `>` prefixes stripped)
    case Para(lead: String, hang: String, text: String)                 // paragraph: lead indent + continuation indent + joined text
    case Item(lead: String, marker: String, hang: String, text: String, ordered: Boolean) // list item

  /** Separator marking a PARAGRAPH BREAK inside one `Quote` block, written in markdown as a bare `>` line.
    *
    * Quote keeps carrying plain joined text (the reflow renderer wants that), so the break has to live IN the
    * text. This sequence is safe as a marker because every other join in the quote branch is a single space, so
    * no parsed quote can otherwise contain a newline at all. Both consumers know it: `tt md-fmt` re-emits a bare
    * `>` between paragraphs rather than reflowing the break away, and `tt ssg` renders one `<p>` per paragraph
    * inside a single `<blockquote>`. */
  val QuoteParaSep: String = "\n\n"

  /** Display width in code points (a multi-byte char counts as one column). */
  def cpLen(s: String): Int = s.codePointCount(0, s.length)

  // --- line classifiers (a line's block type). Public so the HTML renderer can reuse them. ---
  private val bulletRe = """^(\s*)([-*+]\s+|\d+\.\s+)(.*)$""".r
  def isFence(l: String): Boolean = { val t = l.trim; t.startsWith("```") || t.startsWith("~~~") }
  def isHeading(l: String): Boolean = l.trim.startsWith("#")
  def isTable(l: String): Boolean = l.trim.startsWith("|")
  def isHR(l: String): Boolean = l.trim.matches("(-{3,}|\\*{3,}|_{3,})")
  def isBlank(l: String): Boolean = l.trim.isEmpty
  def isQuote(l: String): Boolean = l.trim.startsWith(">")
  def isSpecial(l: String): Boolean =
    isHeading(l) || isTable(l) || isHR(l) || isBlank(l) || isFence(l) || isQuote(l)

  /** Parse markdown into a flat block stream. Consecutive list items are separate Item blocks (a renderer that
    * wants `<ul>`/`<ol>` grouping folds adjacent Items itself). */
  def parse(md: String): Vector[Block] =
    val lines = md.linesIterator.toVector
    val out = ArrayBuffer[Block]()
    var i = 0
    while i < lines.length do
      val line = lines(i)
      if isFence(line) then
        val buf = ArrayBuffer(line); i += 1
        while i < lines.length && !isFence(lines(i)) do { buf += lines(i); i += 1 }
        if i < lines.length then { buf += lines(i); i += 1 } // the closing fence (if any)
        out += Block.Fence(buf.toVector)
      else if isBlank(line) then { out += Block.Blank; i += 1 }
      else if isHeading(line) then { out += Block.Heading(line); i += 1 }
      else if isHR(line) then { out += Block.Rule(line); i += 1 }
      else if isTable(line) then
        val buf = ArrayBuffer(line); i += 1
        while i < lines.length && isTable(lines(i)) do { buf += lines(i); i += 1 }
        out += Block.Table(buf.toVector)
      else if isQuote(line) then
        // A BARE `>` line is a paragraph break inside the quote (standard markdown); prose lines still join
        // with a space. Two adjacent bare `>` lines collapse to one break rather than an empty paragraph.
        val paras = ArrayBuffer[String]()
        val cur = ArrayBuffer[String]()
        def closePara(): Unit = if cur.nonEmpty then { paras += cur.mkString(" "); cur.clear() }
        while i < lines.length && isQuote(lines(i)) do
          val inner = lines(i).trim.stripPrefix(">").stripPrefix(" ")
          if inner.isBlank then closePara() else cur += inner
          i += 1
        closePara()
        out += Block.Quote(paras.mkString(QuoteParaSep))
      else
        // paragraph or list item: gather this line + its non-special, non-item continuation lines
        val m = bulletRe.findFirstMatchIn(line)
        val (lead, markerOpt, contentFirst) = m match
          case Some(mm) => (mm.group(1), Some(mm.group(2)), mm.group(3))
          case None     => val l = line.takeWhile(_ == ' '); (l, None, line.drop(l.length))
        val contents = ArrayBuffer[String](contentFirst.trim)
        val contIndents = ArrayBuffer[Int]()
        i += 1
        while i < lines.length && !isSpecial(lines(i)) && bulletRe.findFirstMatchIn(lines(i)).isEmpty do
          contIndents += lines(i).takeWhile(_ == ' ').length
          contents += lines(i).trim; i += 1
        val text = contents.mkString(" ")
        markerOpt match
          case Some(marker) =>
            val hang = if contIndents.nonEmpty then " " * contIndents.min else lead + " " * cpLen(marker)
            out += Block.Item(lead, marker, hang, text, ordered = marker.head.isDigit)
          case None =>
            val hang = if contIndents.nonEmpty then " " * contIndents.min else lead
            out += Block.Para(lead, hang, text)
    out.toVector
