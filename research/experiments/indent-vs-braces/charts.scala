//> using scala 3.8.4
//> using dep com.lihaoyi::os-lib:0.11.8

// Generate SVG figures for the blog post from the raw experiment data.
//   scala-cli run charts.scala -- results-raw.tsv <outDir>
// Emits: fig-model-style.svg (error-rate by model x style — the bidirectionality headline)
//        fig-size-style.svg  (error-rate by block size x style — the size gradient)
// Pure: reads the TSV, computes fails/attempts, writes SVG. No deps beyond os-lib.

case class Row(task: String, style: String, model: String, graded: String)

val styles = List("braceless", "braces", "common")
val col = Map("braceless" -> "#e15759", "braces" -> "#4e79a7", "common" -> "#59a14f")
// Data keys match the TSV `regime` column ("braces"); the blog prose calls that style "braceful".
// Remap only the RENDERED label so charts read consistently with the text. Identity for all other keys.
def styleLabel(s: String): String = Map("braces" -> "braceful").getOrElse(s, s)

def esc(s: String): String = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
def n(d: Double): String = f"$d%.1f"
def errRate(rs: Seq[Row]): Double =
  if rs.isEmpty then 0.0 else rs.count(_.graded != "PASS").toDouble / rs.size

/** Grouped bar chart: one group per `groups` entry, 3 bars (styles) each, values 0..1. */
def groupedBar(title: String, subtitle: String, groups: List[String],
               value: (String, String) => Double, groupW: Int): String =
  val padL = 54; val padR = 18; val padT = 56; val padB = 108
  val plotH = 230
  val plotW = groupW * groups.size
  // canvas must fit the widest of {plot, title, subtitle} so long subtitles never clip at the viewBox edge
  val textW = math.max(title.length * 9.6, subtitle.length * 6.5)
  val w = math.max(padL + plotW + padR, (padL + textW + padR).toInt)
  val h = padT + plotH + padB
  val sidePad = 9; val barGap = 3
  val barW = (groupW - 2 * sidePad - 2 * barGap) / 3.0
  val sb = new StringBuilder
  sb ++= s"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 $w $h" width="$w" height="$h" font-family="-apple-system,Segoe UI,Roboto,Helvetica,sans-serif">"""
  sb ++= s"""<rect width="$w" height="$h" fill="white"/>"""
  sb ++= s"""<text x="$padL" y="27" font-size="17" font-weight="700" fill="#1a1a1a">${esc(title)}</text>"""
  sb ++= s"""<text x="$padL" y="45" font-size="12" fill="#666">${esc(subtitle)}</text>"""
  for p <- List(0, 25, 50, 75, 100) do
    val y = padT + plotH * (1 - p / 100.0)
    sb ++= s"""<line x1="$padL" y1="${n(y)}" x2="${padL + plotW}" y2="${n(y)}" stroke="#ececec" stroke-width="1"/>"""
    sb ++= s"""<text x="${padL - 7}" y="${n(y + 4)}" font-size="11" fill="#999" text-anchor="end">$p%</text>"""
  for (g, gi) <- groups.zipWithIndex do
    val gx = padL + gi * groupW
    for (st, si) <- styles.zipWithIndex do
      val v = value(g, st)
      val bh = plotH * v
      val x = gx + sidePad + si * (barW + barGap)
      val y = padT + plotH - bh
      sb ++= s"""<rect x="${n(x)}" y="${n(y)}" width="${n(barW)}" height="${n(bh)}" fill="${col(st)}" rx="1.5"/>"""
      sb ++= s"""<text x="${n(x + barW / 2)}" y="${n(y - 3)}" font-size="9" fill="#555" text-anchor="middle">${(v * 100).round}</text>"""
    val lx = gx + groupW / 2.0
    val ly = padT + plotH + 15
    sb ++= s"""<text x="${n(lx)}" y="$ly" font-size="10.5" fill="#333" text-anchor="end" transform="rotate(-32 ${n(lx)} $ly)">${esc(g)}</text>"""
  val ly = padT + plotH + 74
  for (st, si) <- styles.zipWithIndex do
    val lx = padL + si * 118
    sb ++= s"""<rect x="$lx" y="${ly - 11}" width="13" height="13" fill="${col(st)}" rx="2"/>"""
    sb ++= s"""<text x="${lx + 18}" y="$ly" font-size="12.5" fill="#333">${esc(styleLabel(st))}</text>"""
  sb ++= "</svg>\n"
  sb.toString

/** 100%-stacked bar: one bar per group, segments stacked bottom→top; values are proportions summing to ~1. */
def stackedBar(title: String, subtitle: String, groups: List[String],
               segs: List[(String, String)], value: (String, String) => Double, groupW: Int): String =
  val padL = 54; val padR = 18; val padT = 56; val padB = 92
  val plotH = 230
  val plotW = groupW * groups.size
  val textW = math.max(title.length * 9.6, subtitle.length * 6.5)
  val legendW = padL + (segs.size - 1) * 150 + segs.last._1.length * 7 + padR
  val w = math.max(math.max(padL + plotW + padR, (padL + textW + padR).toInt), legendW)
  val h = padT + plotH + padB
  val barW = groupW * 0.46
  val sb = new StringBuilder
  sb ++= s"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 $w $h" width="$w" height="$h" font-family="-apple-system,Segoe UI,Roboto,Helvetica,sans-serif">"""
  sb ++= s"""<rect width="$w" height="$h" fill="white"/>"""
  sb ++= s"""<text x="$padL" y="27" font-size="17" font-weight="700" fill="#1a1a1a">${esc(title)}</text>"""
  sb ++= s"""<text x="$padL" y="45" font-size="12" fill="#666">${esc(subtitle)}</text>"""
  for p <- List(0, 25, 50, 75, 100) do
    val y = padT + plotH * (1 - p / 100.0)
    sb ++= s"""<line x1="$padL" y1="${n(y)}" x2="${padL + plotW}" y2="${n(y)}" stroke="#ececec" stroke-width="1"/>"""
    sb ++= s"""<text x="${padL - 7}" y="${n(y + 4)}" font-size="11" fill="#999" text-anchor="end">$p%</text>"""
  for (g, gi) <- groups.zipWithIndex do
    val cx = padL + gi * groupW + groupW / 2.0
    val x = cx - barW / 2
    var yBottom = (padT + plotH).toDouble
    for (segLabel, color) <- segs do
      val v = value(g, segLabel)
      val segH = plotH * v
      val y = yBottom - segH
      sb ++= s"""<rect x="${n(x)}" y="${n(y)}" width="${n(barW)}" height="${n(segH)}" fill="$color"/>"""
      if v >= 0.05 then
        sb ++= s"""<text x="${n(cx)}" y="${n(y + segH / 2 + 4)}" font-size="11" font-weight="600" fill="#ffffff" text-anchor="middle">${(v * 100).round}%</text>"""
      yBottom = y
    sb ++= s"""<text x="${n(cx)}" y="${padT + plotH + 17}" font-size="12.5" fill="#333" text-anchor="middle">${esc(styleLabel(g))}</text>"""
  val ly = padT + plotH + 62
  for ((seg, si) <- segs.zipWithIndex) do
    val (segLabel, color) = seg
    val lx = padL + si * 150
    sb ++= s"""<rect x="$lx" y="${ly - 11}" width="13" height="13" fill="$color" rx="2"/>"""
    sb ++= s"""<text x="${lx + 18}" y="$ly" font-size="12.5" fill="#333">${esc(segLabel)}</text>"""
  sb ++= "</svg>\n"
  sb.toString

@main def run(tsvPath: String, outDir: String): Unit =
  val rows = os.read.lines(os.Path(tsvPath, os.pwd)).drop(1).filter(_.nonEmpty).map { l =>
    val c = l.split("\t"); Row(c(0), c(1), c(2), c(4))
  }
  val out = os.Path(outDir, os.pwd)
  os.makeDir.all(out)

  // Fig 1 — error rate by model x style (models in a stable order)
  val models = rows.map(_.model).distinct.sorted.toList
  val modelRate = (m: String, st: String) => errRate(rows.filter(r => r.model == m && r.style == st))
  os.write.over(out / "fig-model-style.svg",
    groupedBar("Edit-error rate by model and style",
      "wrap-in-else edit, 7 local models, R=6 — lower is better. Note: the effect flips per model.",
      models, modelRate, groupW = 84))

  // Fig 2 — error rate by block size x style
  val sizeOf = Map("001" -> "small (2 branches)", "002" -> "medium (5 branches)", "003" -> "large (10 branches)")
  def sizeKey(task: String) = sizeOf.getOrElse(task.take(3), task.take(3))
  val sizes = List("small (2 branches)", "medium (5 branches)", "large (10 branches)")
  val sizeRate = (sz: String, st: String) => errRate(rows.filter(r => sizeKey(r.task) == sz && r.style == st))
  os.write.over(out / "fig-size-style.svg",
    groupedBar("Edit-error rate grows with block size",
      "all models pooled — error rises with size under every style; braceless is worst at every size.",
      sizes, sizeRate, groupW = 150))

  // Fig 3 — failure-type composition by style (100%-stacked): pass / silent mis-scope / loud compile error
  val outcomeCol = List("passed" -> "#59a14f", "silent mis-scope" -> "#e8a33d", "compile error" -> "#e15759")
  val gradedOf = Map("passed" -> "PASS", "silent mis-scope" -> "FAIL_MISSCOPE", "compile error" -> "FAIL_COMPILE")
  val styleOutcome = (st: String, seg: String) =>
    val rs = rows.filter(_.style == st)
    if rs.isEmpty then 0.0 else rs.count(_.graded == gradedOf(seg)).toDouble / rs.size
  os.write.over(out / "fig-failure-split.svg",
    stackedBar("What kind of failure?",
      "each style's 126 attempts, split by outcome — pass, silent mis-scope, or loud compile error.",
      styles, outcomeCol, styleOutcome, groupW = 172))

  // Console sanity table (matches RESULTS.md)
  println("model x style error-rate (%):")
  for m <- models do
    println(f"  $m%-20s " + styles.map(st => f"$st=${modelRate(m, st) * 100}%.0f%%").mkString("  "))
  println("size x style error-rate (%):")
  for sz <- sizes do
    println(f"  $sz%-14s " + styles.map(st => f"$st=${sizeRate(sz, st) * 100}%.0f%%").mkString("  "))
  println("failure split by style (of 126 each — pass / silent misscope / loud compile):")
  for st <- styles do
    val rs = rows.filter(_.style == st)
    println(f"  $st%-10s pass=${rs.count(_.graded == "PASS")}  misscope=${rs.count(_.graded == "FAIL_MISSCOPE")}  compile=${rs.count(_.graded == "FAIL_COMPILE")}")
  println(s"wrote 3 SVGs to $out")
