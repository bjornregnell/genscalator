//> using scala 3.8.4
//> using dep com.lihaoyi::os-lib:0.11.8

// analyze — aggregate results-raw.tsv into error-rate + diff-locality tables (per model×regime, per task×regime,
// failure-type split). Prints markdown to stdout (drop into RESULTS.md with narrative + caveats).
//   scala-cli run analyze.scala
val Root = os.Path("/home/bjornr/git/berg/bjornregnell/genscalator/research/experiments/indent-vs-braces")

case class Row(task: String, regime: String, model: String, run: Int, graded: String, outTok: Int, diff: Int)

@main def analyze(): Unit =
  val rows = os.read.lines(Root / "results-raw.tsv").drop(1).filter(_.trim.nonEmpty).map { l =>
    val a = l.split("\t"); Row(a(0), a(1), a(2), a(3).toInt, a(4), a(5).toInt, a(6).toInt)
  }
  def rate(rs: Seq[Row]): String =
    if rs.isEmpty then "-" else
      val fails = rs.count(_.graded != "PASS"); f"${100.0 * fails / rs.size}%.0f%% (${fails}/${rs.size})"
  def meanDiff(rs: Seq[Row]): String =
    val p = rs.filter(r => r.graded == "PASS" && r.diff >= 0)
    if p.isEmpty then "-" else f"${p.map(_.diff).sum.toDouble / p.size}%.1f"

  val regimes = List("braceless", "braces", "common")
  val models = rows.map(_.model).distinct.sorted
  val tasks = rows.map(_.task).distinct.sorted
  val bar = "|" + ("---|" * (regimes.size + 1))
  val sb = StringBuilder()

  sb ++= s"_n = ${rows.size} cells; ${models.size} models × ${tasks.size} tasks × ${regimes.size} regimes._\n\n"
  sb ++= "### Error-rate by model × regime (fails/attempts)\n\n"
  sb ++= "| model | " + regimes.mkString(" | ") + " |\n" + bar + "\n"
  for m <- models do
    sb ++= s"| $m | " + regimes.map(rg => rate(rows.filter(r => r.model == m && r.regime == rg))).mkString(" | ") + " |\n"
  sb ++= "\n### Error-rate by task (size) × regime\n\n"
  sb ++= "| task | " + regimes.mkString(" | ") + " |\n" + bar + "\n"
  for t <- tasks do
    sb ++= s"| $t | " + regimes.map(rg => rate(rows.filter(r => r.task == t && r.regime == rg))).mkString(" | ") + " |\n"
  sb ++= "\n### Failure-type split by regime\n\n"
  for rg <- regimes do
    val rs = rows.filter(_.regime == rg)
    sb ++= s"- **$rg**: ${rs.count(_.graded == "PASS")} pass, ${rs.count(_.graded == "FAIL_COMPILE")} compile-fail, ${rs.count(_.graded == "FAIL_MISSCOPE")} misscope, ${rs.count(r => r.graded.startsWith("FAIL_N") || r.graded == "FAIL_ERROR" || r.graded == "FAIL_TIMEOUT")} infra (n=${rs.size})\n"
  sb ++= "\n### Mean diff-lines (PASS only) by model × regime\n\n"
  sb ++= "| model | " + regimes.mkString(" | ") + " |\n" + bar + "\n"
  for m <- models do
    sb ++= s"| $m | " + regimes.map(rg => meanDiff(rows.filter(r => r.model == m && r.regime == rg))).mkString(" | ") + " |\n"
  println(sb.toString)
