//> using scala 3.8.4
//> using dep com.lihaoyi::os-lib:0.11.8

// analyze-main — separate the two effects: emission-conformance (RQ1) vs edit-correctness GIVEN emission (RQ2).
//   scala-cli run analyze-main.scala
val Root = os.Path("/home/bjornr/git/berg/bjornregnell/genscalator/research/experiments/indent-vs-braces")

case class Row(task: String, style: String, model: String, run: Int, emitted: String, graded: String, outTok: Int, diff: Int)

@main def analyzeMain(): Unit =
  val rows = os.read.lines(Root / "results-main.tsv").drop(1).filter(_.trim.nonEmpty).map { l =>
    val a = l.split("\t"); Row(a(0), a(1), a(2), a(3).toInt, a(4), a(5), a(6).toInt, a(7).toInt)
  }
  val styles = List("braceless", "braces", "common")
  val models = rows.map(_.model).distinct.sorted
  def pct(num: Int, den: Int): String = if den == 0 then "-" else f"${100.0 * num / den}%.0f%% ($num/$den)"
  // emission-rate: among braces/braceless cells (common = na), fraction that emitted the requested style
  def emitRate(rs: Seq[Row]): String = { val e = rs.filter(_.emitted != "na"); pct(e.count(_.emitted == "conform"), e.size) }
  // conditional correctness: among cells that emitted correctly (conform, or na=common), the PASS rate
  def condPass(rs: Seq[Row]): String = { val c = rs.filter(r => r.emitted == "conform" || r.emitted == "na"); pct(c.count(_.graded == "PASS"), c.size) }
  def rawPass(rs: Seq[Row]): String = pct(rs.count(_.graded == "PASS"), rs.size)

  val bar = "|" + ("---|" * (styles.size + 1))
  val sb = StringBuilder()
  sb ++= s"_n = ${rows.size} cells; ${models.size} models × 3 tasks × 3 styles._\n\n"
  sb ++= "#### RQ1 — Emission-conformance by model × style (emitted requested style / attempts; common = na)\n\n"
  sb ++= "| model | " + styles.mkString(" | ") + " |\n" + bar + "\n"
  for m <- models do
    sb ++= s"| $m | " + styles.map(st => emitRate(rows.filter(r => r.model == m && r.style == st))).mkString(" | ") + " |\n"
  sb ++= "\n#### RQ2 — Conditional edit-correctness: PASS-rate GIVEN correct emission (the decoupled edit-cost)\n\n"
  sb ++= "| model | " + styles.mkString(" | ") + " |\n" + bar + "\n"
  for m <- models do
    sb ++= s"| $m | " + styles.map(st => condPass(rows.filter(r => r.model == m && r.style == st))).mkString(" | ") + " |\n"
  sb ++= "\n#### Aggregate by style (emission vs conditional correctness vs raw)\n\n"
  sb ++= "| style | emission-conform | cond. correctness (PASS \\| emit) | raw PASS |\n|---|---|---|---|\n"
  for st <- styles do
    val rs = rows.filter(_.style == st)
    sb ++= s"| $st | ${emitRate(rs)} | ${condPass(rs)} | ${rawPass(rs)} |\n"
  println(sb.toString)
