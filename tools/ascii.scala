//> using scala 3.8.4
//> using jvm 21
//> using file seqspec.scala

// ascii — render a sequence-diagram spec (the SAME spec as `tt svg`, shared via seqspec.scala) to a good-looking
// monospace diagram for terminals, PR/commit comments, and plaintext reports. PURE: reads spec, computes text,
// prints (or writes a file).
//   tt ascii sequence <in.txt> [out.txt] [--pure]     (no out → stdout; --pure = strict 7-bit ASCII glyphs)
//   tt ascii --sequence-diagram <in.txt> [out.txt]    (alias for `sequence`)
// Default uses Unicode box-drawing glyphs (│ ─ ┌ ┐ └ ┘ ┬ ┴ ┼ ▶ ◀) for looks; --pure falls back to | - + > < for
// contexts that can't render them. A dashed arrow (a reply/return, `A --> B`) draws as a gapped line + open head.
import java.nio.file.{Files, Path}
import SeqSpec.*

object Ascii:

  /** The glyph set. Default is box-drawing (pretty); `pure` is strict 7-bit ASCII. */
  final case class Glyphs(v: Char, h: Char, tl: Char, tr: Char, bl: Char, br: Char,
                          tdown: Char, tup: Char, cross: Char, aR: Char, aL: Char)
  val Box  = Glyphs('│', '─', '┌', '┐', '└', '┘', '┬', '┴', '┼', '▶', '◀')
  val Pure = Glyphs('|', '-', '+', '+', '+', '+', '+', '+', '+', '>', '<')

  /** Render a parsed diagram to a monospace-art string. */
  def render(d: Diagram, g: Glyphs): String =
    val lls = d.lifelines
    val n = lls.length
    if n == 0 then return "(empty diagram)\n"
    val labels = lls.map(_.label)
    def boxW(l: String): Int = l.length + 4 // "│ label │"
    val maxBox = labels.map(boxW).max
    val maxMsg = d.events.collect { case Event.Msg(_, _, t, _) => t.length }.maxOption.getOrElse(0)
    val pitch  = math.max(math.max(maxBox + 2, maxMsg + 4), 14) // room between adjacent lifelines for a label
    val leftPad = maxBox / 2 + 2
    val idIndex = lls.iterator.map(_.id).zipWithIndex.toMap
    def center(i: Int): Int = leftPad + i * pitch

    // width must fit the packed lifelines, the title, and any right-extending self-message / note label
    val evReach = d.events.map {
      case Event.Msg(f, t, txt, _) if idIndex(f) == idIndex(t) => center(idIndex(f)) + 6 + txt.length
      case Event.Msg(f, t, txt, _) => (center(idIndex(f)) + center(idIndex(t))) / 2 + txt.length / 2 + 1
      case Event.Note(over, txt) =>
        val xs = over.flatMap(idIndex.get).map(center)
        if xs.isEmpty then 0 else (xs.min + xs.max) / 2 + math.max(txt.length + 4, xs.max - xs.min + 6) / 2 + 1
    }.maxOption.getOrElse(0)
    val width = List(center(n - 1) + maxBox / 2 + 2, d.title.map(_.length).getOrElse(0) + 2, evReach).max + 2

    val rows = scala.collection.mutable.ArrayBuffer.empty[Array[Char]]
    def blank(): Array[Char] = Array.fill(width)(' ')
    def put(r: Array[Char], c: Int, ch: Char): Unit = if c >= 0 && c < width then r(c) = ch
    def putStr(r: Array[Char], c: Int, s: String): Unit = s.iterator.zipWithIndex.foreach((ch, k) => put(r, c + k, ch))
    def lifeRow(): Array[Char] = { val r = blank(); for i <- 0 until n do put(r, center(i), g.v); r }
    def hbox(r: Array[Char], left: Int, w: Int, lc: Char, rc: Char, mid: Char, midCol: Int): Unit =
      put(r, left, lc); for k <- 1 until w - 1 do put(r, left + k, g.h); put(r, left + w - 1, rc); put(r, midCol, mid)

    // title
    d.title.foreach { t =>
      val tr = blank(); putStr(tr, (width - t.length) / 2, t); rows += tr; rows += blank()
    }

    // header: a labelled box per lifeline, its bottom connected to the lifeline with `┬`
    val hTop = blank(); val hMid = blank(); val hBot = blank()
    for i <- 0 until n do
      val l = labels(i); val w = boxW(l); val left = center(i) - w / 2
      hbox(hTop, left, w, g.tl, g.tr, g.h, center(i))
      putStr(hMid, left, s"${g.v} $l ${g.v}")
      hbox(hBot, left, w, g.bl, g.br, g.tdown, center(i))
    rows += hTop += hMid += hBot += lifeRow()

    // events
    for ev <- d.events do
      ev match
        case Event.Msg(from, to, text, dashed) =>
          val a = center(idIndex(from)); val b = center(idIndex(to))
          if a == b then // self-message: a small loop to the right
            val r1 = lifeRow(); put(r1, a + 1, g.h); put(r1, a + 2, g.h); put(r1, a + 3, g.tr); putStr(r1, a + 5, text)
            val r2 = lifeRow(); put(r2, a + 1, g.aL); put(r2, a + 2, g.h); put(r2, a + 3, g.br)
            rows += r1 += r2
          else
            val lo = math.min(a, b); val hi = math.max(a, b)
            val lrow = lifeRow(); putStr(lrow, (a + b) / 2 - text.length / 2, text) // label centred above the arrow
            val arow = lifeRow()
            for c <- lo + 1 until hi do put(arow, c, if dashed && ((c - lo) % 2 == 1) then ' ' else g.h)
            for i <- 0 until n do { val c = center(i); if c > lo && c < hi then put(arow, c, g.cross) } // crossings
            put(arow, b, if b > a then g.aR else g.aL) // arrowhead at the target
            rows += lrow += arow
        case Event.Note(over, text) =>
          val xs = over.flatMap(idIndex.get).map(center)
          if xs.nonEmpty then
            val mid = (xs.min + xs.max) / 2
            val w = math.max(text.length + 4, xs.max - xs.min + 6)
            val left = mid - w / 2
            val top = lifeRow(); hbox(top, left, w, g.tl, g.tr, g.h, -1)
            val body = lifeRow()
            val inner = w - 2; val lp = (inner - text.length) / 2
            putStr(body, left, s"${g.v}" + " " * lp + text + " " * (inner - text.length - lp) + s"${g.v}")
            val bot = lifeRow(); hbox(bot, left, w, g.bl, g.br, g.h, -1)
            rows += top += body += bot
      rows += lifeRow() // breathing room between events

    // footer: mirror the header, top border connected UP to the lifeline with `┴`
    val fTop = blank(); val fMid = blank(); val fBot = blank()
    for i <- 0 until n do
      val l = labels(i); val w = boxW(l); val left = center(i) - w / 2
      hbox(fTop, left, w, g.tl, g.tr, g.tup, center(i))
      putStr(fMid, left, s"${g.v} $l ${g.v}")
      hbox(fBot, left, w, g.bl, g.br, g.h, center(i))
    rows += fTop += fMid += fBot

    rows.iterator.map(r => String(r).replaceAll("\\s+$", "")).mkString("\n") + "\n"

  // --- CLI -----------------------------------------------------------------
  private def isSeqMode(m: String): Boolean =
    val s = m.toLowerCase; s == "sequence" || s == "seq" || s == "--sequence-diagram" || s == "-s"

  private val Help: String =
    """tt ascii — sequence-diagram spec → good-looking monospace / box-drawing art
      |
      |The plaintext sibling of `tt svg`: reads the SAME spec (shared grammar) and renders a
      |diagram for terminals, PR/commit comments, and plaintext reports. Pure: reads the spec,
      |computes text, prints it (or writes a file when out is given).
      |
      |Usage:
      |  ascii sequence <in.txt>                  render the spec, art to stdout
      |  ascii sequence <in.txt> <out.txt>        ... write the art to <out.txt> instead
      |  ascii --sequence-diagram <in.txt> [out]  alias for `sequence` (also: seq, -s)
      |
      |Flags:
      |  --pure    strict 7-bit ASCII glyphs (| - + > <) for contexts that cannot render Unicode.
      |            Default: Unicode box-drawing glyphs (│ ─ ┌ ┐ └ ┘ ┬ ┴ ┼ ▶ ◀) for looks.
      |
      |Spec (one statement per line; blank lines and # / // comments are ignored):
      |  title: <text>                    optional diagram title, centred at the top
      |  actor <Id> [as <label>]          declare a lifeline (also: participant); label may be "quoted"
      |  <A> -> <B>: <message>            solid arrow (a call / synchronous message)
      |  <A> --> <B>: <message>           dashed reply — renders as a gapped line + open head
      |  note over <A>[, <B>]: <text>     a note box spanning one or two lifelines
      |Undeclared lifelines are auto-created in first-seen order; A -> A draws a small self-loop.
      |
      |Examples:
      |  tt ascii sequence flow.txt                       # print to the terminal
      |  tt ascii sequence flow.txt flow.txt.art --pure   # write a strict-ASCII file
      |
      |Siblings reading the SAME spec: tt svg (theme-aware SVG), tt gvdot (graphviz image).
      |Full reference: tools/README.md""".stripMargin

  private def usage(): Unit =
    println(
      """usage: ascii sequence <in.txt> [out.txt] [--pure]   render a sequence-diagram spec to monospace art (no out → stdout)
        |       ascii --sequence-diagram <in.txt> [out.txt]   (alias for `sequence`)
        |
        |glyphs: default = Unicode box-drawing (good-looking); --pure = strict 7-bit ASCII (| - + > <)
        |spec lines:  title: <t> | actor <Id> [as <label>] | <A> -> <B>: <msg> | <A> --> <B>: <msg> | note over <A>[,<B>]: <t>""".stripMargin)

  def dispatch(args: List[String]): Unit =
    if args.contains("--help") || args.contains("-h") then { println(Help); sys.exit(0) }
    args match
      case mode :: tail if isSeqMode(mode) =>
        val (flags, pos) = tail.partition(_.startsWith("--"))
        val g = if flags.map(_.toLowerCase).contains("--pure") then Pure else Box
        pos match
          case in :: rest =>
            val art = render(parse(Files.readString(Path.of(in))), g)
            rest.headOption match
              case Some(out) =>
                Files.writeString(Path.of(out), art)
                println(s"ascii: wrote ${art.linesIterator.size}-line sequence diagram to $out")
              case None => print(art)
          case Nil => usage(); sys.exit(2)
      case _ =>
        usage()
        sys.exit(2)

@main def renderAsciiDiagram(args: String*): Unit = Ascii.dispatch(args.toList)
