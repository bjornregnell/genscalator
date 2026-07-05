//> using scala 3.8.4
//> using jvm 21
//> using file seqspec.scala

// svg — render a small textual diagram spec to a self-contained, theme-aware SVG for blogs & reports.
// Focus mode: --sequence-diagram (aka `sequence`). PURE: reads spec text, computes SVG, prints (or writes a file).
//   tt svg sequence <in.txt> [out.svg]              (no out → prints the SVG to stdout)
//   tt svg --sequence-diagram <in.txt> [out.svg]    (alias for `sequence`)
//
// Why a bespoke spec and NOT reqT-lang: reqT is conceptually a *bag* of requirements — element order is not
// semantic (though reqT-lang's parser does preserve source order), and there is no message/interaction concept. A
// sequence diagram is fundamentally ORDERED in time (message 1, then 2, then 3) along per-actor lifelines, where the
// order IS the meaning — so it wants a notation where order is semantic. Hence a tiny purpose-built spec,
// deliberately PlantUML/mermaid-flavoured so it is a de-facto standard readers already know. (Design rationale +
// BR's ordering correction: research/037-svg-sequence-diagram-tool.md.)
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
import SeqSpec.* // shared spec model (Diagram, Event, Lifeline) + parse — see seqspec.scala

// Helpers live INSIDE this object so top-level names don't collide with the other tools when the whole tools/
// tree compiles as one unit (scala-cli compile tools / the Scala MCP). Only the @main entry is top-level.
object Svg:

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

  private val LightVars = "--bg:#ffffff; --fg:#1b1b2b; --box:#eef0fb; --boxline:#9aa0d0; --life:#c3c3d6; --line:#5a5f86; --notebg:#fff6d8; --noteline:#e0c86a;"
  private val DarkVars  = "--bg:#1b1b24; --fg:#e6e6f0; --box:#2a2d44; --boxline:#5a5f86; --life:#44465e; --line:#a6abd8; --notebg:#403a23; --noteline:#8a7a3a;"

  /** CSS for the chosen theme. "light"/"dark" hardcode ONE palette — predictable in ANY embedding context (an SSG
    * page whose theme differs from the OS, a PDF export, an image viewer). "auto" adapts to the viewer via
    * prefers-color-scheme — convenient, but it tracks the OS setting, NOT the host page's theme, so it can mismatch
    * a light page on a dark OS. That mismatch is exactly why the tailored modes exist. */
  private def palette(theme: String): String = theme match
    case "light" => s":root { $LightVars }"
    case "dark"  => s":root { $DarkVars }"
    case _       => s":root { $LightVars }\n    @media (prefers-color-scheme: dark) { :root { $DarkVars } }"

  private def svgHeader(w: Double, h: Double, theme: String): String =
    s"""<svg xmlns="http://www.w3.org/2000/svg" width="${fmt(w)}" height="${fmt(h)}" viewBox="0 0 ${fmt(w)} ${fmt(h)}" font-family="ui-sans-serif, system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif">
  <style>
    ${palette(theme)}
    text { fill: var(--fg); }
    .canvas { fill: var(--bg); }
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

  /** Render a parsed diagram to a complete, self-contained SVG document string. theme = "auto" | "light" | "dark".
    * transparent = omit the background rect (default is an OPAQUE, theme-coloured background — transparent SVG
    * backgrounds often render badly in Markdown/GitHub). */
  def render(d: Diagram, theme: String, transparent: Boolean): String =
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
          y += noteH + 28 // clear the box bottom AND the next message's label (which sits ~17px above its arrow)

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

    val canvasSvg = // opaque theme-coloured background by default; --transparent omits it
      if transparent then "" else s"""  <rect class="canvas" x="0" y="0" width="${fmt(totalW)}" height="${fmt(totalH)}"/>\n"""
    svgHeader(totalW, totalH, theme) + canvasSvg + titleSvg + lifelineSvg.toString + body.toString + "</svg>\n"

  // --- CLI -----------------------------------------------------------------
  private def isSeqMode(m: String): Boolean =
    val s = m.toLowerCase
    s == "sequence" || s == "seq" || s == "--sequence-diagram" || s == "-s"

  /** Theme from flags: --dark/--dark-mode, --light/--light-mode, else "auto" (prefers-color-scheme adaptive). */
  private def themeOf(flags: List[String]): String =
    val f = flags.map(_.toLowerCase)
    if f.exists(x => x == "--dark" || x == "--dark-mode") then "dark"
    else if f.exists(x => x == "--light" || x == "--light-mode") then "light"
    else "auto"

  /** --transparent (aka --transparent-bg / --no-bg) → transparent background; default is an OPAQUE theme background. */
  private def transparentOf(flags: List[String]): Boolean =
    flags.map(_.toLowerCase).exists(x => x == "--transparent" || x == "--transparent-bg" || x == "--no-bg")

  private def usage(): Unit =
    println(
      """usage: svg sequence <in.txt> [out.svg] [--light|--dark] [--transparent]   spec → SVG (no out → stdout)
        |       svg --sequence-diagram <in.txt> [out.svg]                          (alias for `sequence`)
        |
        |theme:  (default) auto = adapts to the viewer via prefers-color-scheme; --light / --dark = a fixed, tailored
        |        palette (predictable when embedded in a page/PDF whose theme may differ from the OS setting)
        |bg:     default is an OPAQUE, theme-coloured background (transparent SVG bg often renders badly in Markdown);
        |        --transparent (aka --no-bg) drops it
        |spec lines:  title: <t> | actor <Id> [as <label>] | <A> -> <B>: <msg> | <A> --> <B>: <msg> | note over <A>[,<B>]: <t>""".stripMargin)

  def dispatch(args: List[String]): Unit =
    args match
      case mode :: tail if isSeqMode(mode) =>
        val (flags, pos) = tail.partition(_.startsWith("--"))
        val theme = themeOf(flags)
        val transparent = transparentOf(flags)
        pos match
          case in :: rest =>
            val diagram = parse(Files.readString(Path.of(in)))
            val svg = render(diagram, theme, transparent)
            val nMsg = diagram.events.count(_.isInstanceOf[Event.Msg])
            val bg = if transparent then "transparent" else "opaque"
            rest.headOption match
              case Some(out) =>
                Files.writeString(Path.of(out), svg)
                println(s"svg: wrote $theme/$bg sequence diagram (${diagram.lifelines.size} lifelines, $nMsg messages) to $out")
              case None => print(svg)
          case Nil => usage(); sys.exit(2)
      case _ =>
        usage()
        sys.exit(2)

@main def renderSvgDiagram(args: String*): Unit = Svg.dispatch(args.toList)
