//> using scala 3.8.4
//> using jvm 21

// svg — render a small textual diagram spec to a self-contained, theme-aware SVG for blogs & reports.
// Focus mode: --sequence-diagram (aka `sequence`). PURE: reads spec text, computes SVG, prints (or writes a file).
//   tt svg sequence <in.txt> [out.svg]              (no out → prints the SVG to stdout)
//   tt svg --sequence-diagram <in.txt> [out.svg]    (alias for `sequence`)
//
// Why a bespoke spec and NOT reqT-lang: reqT models a SET of entities / relations / attributes — an unordered
// graph. A sequence diagram is fundamentally ORDERED in time (message 1, then 2, then 3) along per-actor
// lifelines; reqT has no native temporal-order concept, so encoding a sequence in it would lose the very thing
// that makes it a sequence. Hence a tiny purpose-built spec, deliberately PlantUML/mermaid-flavoured so it is a
// de-facto standard readers already know. (See research/svg-sequence-diagram-tool.md for the design rationale.)
//
// Spec grammar (one statement per line; blank lines and `#` / `//` comments are ignored):
//   title: <text>                     optional diagram title, centred at the top
//   actor <Id> [as <label>]           declare a lifeline (also: participant); label may be "quoted"
//   participant <Id> [as label]
//   <A> -> <B>: <message>             solid arrow, filled head  (a call / synchronous message)
//   <A> --> <B>: <message>            dashed arrow, open head    (a return / reply / async)
//   note over <A>[, <B>]: <text>      a note box spanning one or two lifelines
// Lifelines that are not pre-declared are auto-created in first-seen order. A self-message (A -> A) draws a loop.
// The SVG is self-contained (inline <style>, no external refs) and theme-aware (light + prefers-color-scheme dark).

import java.nio.file.{Files, Path}

// Helpers live INSIDE this object so top-level names don't collide with the other tools when the whole tools/
// tree compiles as one unit (scala-cli compile tools / the Scala MCP). Only the @main entry is top-level.
object Svg:

  // --- model ---------------------------------------------------------------
  final case class Lifeline(id: String, label: String)
  enum Event:
    case Msg(from: String, to: String, text: String, dashed: Boolean)
    case Note(over: List[String], text: String)
  final case class Diagram(title: Option[String], lifelines: Vector[Lifeline], events: Vector[Event])

  // --- parsing -------------------------------------------------------------
  private val TitleRe = """(?i)^title\s*:\s*(.*)$""".r
  private val DeclRe  = """(?i)^(?:actor|participant)\s+(\S+?)(?:\s+as\s+(.+))?\s*$""".r
  private val NoteRe  = """(?i)^note\s+over\s+(.+?)\s*:\s*(.*)$""".r
  private val MsgRe   = """^(\S+)\s*(-->|->)\s*(\S+)\s*:\s*(.*)$""".r

  private def unquote(s: String): String =
    val t = s.trim
    if t.length >= 2 && ((t.head == '"' && t.last == '"') || (t.head == '\'' && t.last == '\''))
    then t.substring(1, t.length - 1) else t

  /** Parse a sequence-diagram spec. Lifelines are collected in first-seen order; unknown lines warn (stderr). */
  def parse(spec: String): Diagram =
    var title: Option[String] = None
    val decls  = scala.collection.mutable.LinkedHashMap.empty[String, String] // id -> label, insertion-ordered
    val events = Vector.newBuilder[Event]
    def ensure(id: String): Unit = if !decls.contains(id) then decls(id) = id
    for raw <- spec.linesIterator do
      val line = raw.trim
      if line.isEmpty || line.startsWith("#") || line.startsWith("//") then () // comment / blank
      else line match
        case TitleRe(t)          => title = Some(t.trim)
        case DeclRe(id, label)   => decls(id) = Option(label).map(unquote).getOrElse(decls.getOrElse(id, id))
        case NoteRe(ids, text)   =>
          val over = ids.split(",").iterator.map(_.trim).filter(_.nonEmpty).toList
          over.foreach(ensure)
          events += Event.Note(over, text.trim)
        case MsgRe(from, arr, to, text) =>
          ensure(from); ensure(to)
          events += Event.Msg(from, to, text.trim, arr == "-->")
        case other => System.err.println(s"svg: ignoring unrecognized line: $other")
    Diagram(title, decls.iterator.map((id, lab) => Lifeline(id, lab)).toVector, events.result())

  // --- rendering -----------------------------------------------------------
  private def esc(s: String): String =
    s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

  /** Compact number: drop a trailing .0 so the SVG source stays readable. */
  private def fmt(d: Double): String =
    val r = math.round(d * 10) / 10.0
    if r == math.rint(r) then r.toLong.toString else r.toString

  // font size + estimated average glyph width (no font metrics available; a generous monospace-ish estimate)
  private val Fs = 13.0
  private val Cw = 0.62 * Fs

  private def arrowhead(x: Double, y: Double, dir: Int, open: Boolean): String =
    val b = x - dir * 9 // base of the head, 9px behind the tip
    if open then // return / async: an open "V"
      s"""  <path class="msg" fill="none" d="M ${fmt(b)} ${fmt(y - 4)} L ${fmt(x)} ${fmt(y)} L ${fmt(b)} ${fmt(y + 4)}"/>\n"""
    else // call: a filled triangle
      s"""  <path class="head" stroke="none" d="M ${fmt(x)} ${fmt(y)} L ${fmt(b)} ${fmt(y - 4)} L ${fmt(b)} ${fmt(y + 4)} Z"/>\n"""

  private def svgHeader(w: Double, h: Double): String =
    s"""<svg xmlns="http://www.w3.org/2000/svg" width="${fmt(w)}" height="${fmt(h)}" viewBox="0 0 ${fmt(w)} ${fmt(h)}" font-family="ui-sans-serif, system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif">
  <style>
    :root { --fg:#1b1b2b; --box:#eef0fb; --boxline:#9aa0d0; --life:#c3c3d6; --line:#5a5f86; --notebg:#fff6d8; --noteline:#e0c86a; }
    @media (prefers-color-scheme: dark) {
      :root { --fg:#e6e6f0; --box:#2a2d44; --boxline:#5a5f86; --life:#44465e; --line:#a6abd8; --notebg:#403a23; --noteline:#8a7a3a; }
    }
    text { fill: var(--fg); }
    .lbl { font-size: ${fmt(Fs)}px; }
    .title { font-size: 16px; font-weight: 600; }
    .box { fill: var(--box); stroke: var(--boxline); stroke-width: 1; }
    .note { fill: var(--notebg); stroke: var(--noteline); stroke-width: 1; }
    .life { stroke: var(--life); stroke-width: 1; stroke-dasharray: 4 4; }
    .msg { stroke: var(--line); stroke-width: 1.4; }
    .ret { stroke: var(--line); stroke-width: 1.2; stroke-dasharray: 6 4; }
    .head { fill: var(--line); }
  </style>
"""

  /** Render a parsed diagram to a complete, self-contained SVG document string. */
  def render(d: Diagram): String =
    val padX = 14.0; val marginX = 20.0; val headH = 34.0; val topPad = 14.0
    val gap = 46.0; val noteH = 34.0; val titleFs = 16.0
    def textW(s: String): Double = s.length * Cw
    val lls = d.lifelines
    val n = lls.length
    def headW(i: Int): Double = math.max(90.0, textW(lls(i).label) + 2 * padX)
    val maxHeadW = (0 until n).map(headW).maxOption.getOrElse(90.0)
    val maxMsgW  = d.events.collect { case Event.Msg(_, _, t, _) => textW(t) }.maxOption.getOrElse(0.0)
    val colW = math.max(160.0, math.max(maxHeadW + 24, maxMsgW + 30))
    def cx(i: Int): Double = marginX + colW / 2 + i * colW
    val idIndex = lls.iterator.map(_.id).zipWithIndex.toMap
    val titleH  = if d.title.isDefined then titleFs + 12 else 0.0
    val headTop = topPad + titleH
    val firstY  = headTop + headH + 30

    val body = StringBuilder()
    var y = firstY
    for ev <- d.events do ev match
      case Event.Msg(from, to, text, dashed) =>
        val i = idIndex(from); val j = idIndex(to)
        val cls = if dashed then "ret" else "msg"
        if i == j then // self-message: a small loop to the right of the lifeline
          val x = cx(i); val loopW = math.min(colW * 0.4, 66.0)
          body ++= s"""  <path class="$cls" fill="none" d="M ${fmt(x)} ${fmt(y)} L ${fmt(x + loopW)} ${fmt(y)} L ${fmt(x + loopW)} ${fmt(y + 16)} L ${fmt(x + 6)} ${fmt(y + 16)}"/>\n"""
          body ++= arrowhead(x + 6, y + 16, -1, dashed)
          body ++= s"""  <text class="lbl" x="${fmt(x + loopW + 8)}" y="${fmt(y + 2)}" text-anchor="start">${esc(text)}</text>\n"""
          y += gap
        else
          val x1 = cx(i); val x2 = cx(j)
          val dir = if x2 > x1 then 1 else -1
          body ++= s"""  <line class="$cls" x1="${fmt(x1)}" y1="${fmt(y)}" x2="${fmt(x2 - dir * 1)}" y2="${fmt(y)}"/>\n"""
          body ++= arrowhead(x2, y, dir, dashed)
          body ++= s"""  <text class="lbl" x="${fmt((x1 + x2) / 2)}" y="${fmt(y - 7)}" text-anchor="middle">${esc(text)}</text>\n"""
          y += gap
      case Event.Note(over, text) =>
        val xs = over.flatMap(idIndex.get).map(cx)
        if xs.nonEmpty then
          val w = math.max(textW(text) + 2 * padX, (xs.max - xs.min) + 90)
          val midx = (xs.min + xs.max) / 2
          body ++= s"""  <rect class="note" x="${fmt(midx - w / 2)}" y="${fmt(y)}" width="${fmt(w)}" height="${fmt(noteH)}" rx="3"/>\n"""
          body ++= s"""  <text class="lbl" x="${fmt(midx)}" y="${fmt(y + noteH / 2)}" text-anchor="middle" dominant-baseline="central">${esc(text)}</text>\n"""
          y += noteH + 10

    val footTop = y + 6
    val totalW  = marginX * 2 + n * colW
    val totalH  = footTop + headH + topPad

    val lifelineSvg = StringBuilder()
    for i <- 0 until n do
      val x = cx(i); val hw = headW(i); val lbl = esc(lls(i).label)
      lifelineSvg ++= s"""  <line class="life" x1="${fmt(x)}" y1="${fmt(headTop + headH)}" x2="${fmt(x)}" y2="${fmt(footTop)}"/>\n"""
      for by <- List(headTop, footTop) do // head box (top) and foot box (bottom), same label
        lifelineSvg ++= s"""  <rect class="box" x="${fmt(x - hw / 2)}" y="${fmt(by)}" width="${fmt(hw)}" height="${fmt(headH)}" rx="6"/>\n"""
        lifelineSvg ++= s"""  <text class="lbl" x="${fmt(x)}" y="${fmt(by + headH / 2)}" text-anchor="middle" dominant-baseline="central">$lbl</text>\n"""

    val titleSvg = d.title.map(t =>
      s"""  <text class="title" x="${fmt(totalW / 2)}" y="${fmt(topPad + titleFs - 2)}" text-anchor="middle">${esc(t)}</text>\n"""
    ).getOrElse("")

    svgHeader(totalW, totalH) + titleSvg + lifelineSvg.toString + body.toString + "</svg>\n"

  // --- CLI -----------------------------------------------------------------
  private def isSeqMode(m: String): Boolean =
    val s = m.toLowerCase
    s == "sequence" || s == "seq" || s == "--sequence-diagram" || s == "-s"

  private def usage(): Unit =
    println(
      """usage: svg sequence <in.txt> [out.svg]   render a textual sequence-diagram spec to SVG (no out → stdout)
        |       svg --sequence-diagram <in.txt> [out.svg]   (alias)
        |
        |spec lines:  title: <t> | actor <Id> [as <label>] | <A> -> <B>: <msg> | <A> --> <B>: <msg> | note over <A>[,<B>]: <t>""".stripMargin)

  def dispatch(args: List[String]): Unit =
    args match
      case mode :: in :: rest if isSeqMode(mode) =>
        val spec = Files.readString(Path.of(in))
        val diagram = parse(spec)
        val svg = render(diagram)
        val nMsg = diagram.events.count(_.isInstanceOf[Event.Msg])
        rest.headOption match
          case Some(out) =>
            Files.writeString(Path.of(out), svg)
            println(s"svg: wrote sequence diagram (${diagram.lifelines.size} lifelines, $nMsg messages) to $out")
          case None => print(svg)
      case _ =>
        usage()
        sys.exit(2)

@main def renderSvgDiagram(args: String*): Unit = Svg.dispatch(args.toList)
