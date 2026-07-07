//> using scala 3.8.4
//> using dep com.lihaoyi::os-lib:0.11.8
//> using dep com.lihaoyi::upickle:4.4.3

// Joins the blind LLM style-rater scores to the private key and answers the ceiling question:
// does the FINER rater show substrate/model style structure that the coarse mechanical lint
// flattened? Reads results/style-scores.jsonl ({id, rater, d:[4]}) + results/style-key.jsonl.
// Emits results/style-rater-analysis.md. Bare: `scala-cli run style_analyze.scala`.

val BASE = "/home/bjornr/git/berg/bjornregnell/genscalator/research/047-run"
val Subs = List("full", "empty", "scrambled")

def f2(d: Double): String = f"$d%.2f"
def mean(xs: Seq[Double]): Double = if xs.isEmpty then 0.0 else xs.sum / xs.size

@main def styleAnalyze(): Unit =
  def readRater(fn: String, rater: String): List[(Int, String, Vector[Double])] =
    val p = os.Path(BASE) / "results" / fn
    if !os.exists(p) then Nil
    else os.read.lines(p).toList.flatMap { l =>
      val parts = l.trim.split("\\|")
      if parts.length == 5 then
        try Some((parts(0).toInt, rater, Vector(parts(1), parts(2), parts(3), parts(4)).map(_.toDouble)))
        catch case _: Throwable => None
      else None
    }
  val scores = readRater("raterA.txt", "A") ++ readRater("raterB.txt", "B")
  val key = os.read.lines(os.Path(BASE) / "results" / "style-key.jsonl").toList.flatMap { l =>
    try
      val o = ujson.read(l)
      Some(o("id").num.toInt -> (o("model").str, o("substrate").str, o("task").str, o("lintStyle").num))
    catch case _: Throwable => None
  }.toMap

  // finer style per (id, rater) = sum of 4 dims / 12, normalized 0-1
  def finer(dims: Vector[Double]): Double = dims.sum / 12.0
  // per id, average finer across raters present
  val byId = scores.groupBy(_._1)
  val finerById: Map[Int, Double] = byId.map { (id, rs) => id -> mean(rs.map(t => finer(t._3))) }

  val sb = new StringBuilder
  def out(s: String): Unit = sb.append(s).append("\n")
  out("# 047 blind LLM style-rater analysis (the mechanical-lint ceiling test)")
  out("")
  out(s"Candidates rated: ${finerById.size}. Raters: ${scores.map(_._2).distinct.sorted.mkString(", ")}. " +
      s"Finer style = mean over raters of (sum of 4 dims / 12). Compared against the coarse mechanical lint.")
  out("")

  // ---- finer style vs lint style, by substrate ----
  out("## Finer style vs mechanical lint, by substrate")
  out("")
  out("| substrate | n | FINER style | lint style | delta (finer - lint) |")
  out("|---|---|---|---|---|")
  for sub <- Subs do
    val ids = key.filter(_._2._2 == sub).keys.filter(finerById.contains).toList
    if ids.nonEmpty then
      val finerM = mean(ids.map(finerById))
      val lintM = mean(ids.map(id => key(id)._4))
      out(s"| $sub | ${ids.size} | ${f2(finerM)} | ${f2(lintM)} | ${f2(finerM - lintM)} |")
  out("")

  // ---- finer style by model x substrate ----
  out("## Finer style by model x substrate")
  out("")
  out("| model | full | empty | scrambled |")
  out("|---|---|---|---|")
  val models = key.values.map(_._1).toList.distinct.sorted
  for m <- models do
    def cell(sub: String): String =
      val ids = key.filter(kv => kv._2._1 == m && kv._2._2 == sub).keys.filter(finerById.contains).toList
      if ids.isEmpty then "-" else f2(mean(ids.map(finerById)))
    out(s"| $m | ${cell("full")} | ${cell("empty")} | ${cell("scrambled")} |")
  out("")

  // ---- dimension-level breakdown by substrate (which texture dimension leaks?) ----
  out("## Finer style by DIMENSION x substrate (mean 0-3; which dimension moves?)")
  out("")
  out("| substrate | idiomaticity | immutability | readability | restraint |")
  out("|---|---|---|---|---|")
  for sub <- Subs do
    val ids = key.filter(_._2._2 == sub).keys.filter(byId.contains).toList
    if ids.nonEmpty then
      def dimMean(k: Int): Double = mean(ids.flatMap(id => byId(id).map(_._3(k))))
      out(s"| $sub | ${f2(dimMean(0))} | ${f2(dimMean(1))} | ${f2(dimMean(2))} | ${f2(dimMean(3))} |")
  out("")

  // ---- inter-rater agreement (reliability) ----
  out("## Inter-rater agreement (reliability)")
  out("")
  val bothIds = byId.filter(_._2.map(_._2).distinct.size >= 2).keys.toList
  if bothIds.nonEmpty then
    // pair A vs B on shared ids; per-dimension exact agreement + mean abs diff on 0-12 total + Pearson r
    val pairs = bothIds.flatMap { id =>
      val rs = byId(id)
      for a <- rs.find(_._2 == "A"); b <- rs.find(_._2 == "B") yield (a._3, b._3)
    }
    val dimAgree = (0 to 3).map { k =>
      val ag = pairs.count { case (a, b) => a(k) == b(k) }.toDouble / pairs.size
      f"${List("idiom", "immut", "read", "restraint")(k)}=${ag}%.2f"
    }.mkString(", ")
    val totalsA = pairs.map(_._1.sum)
    val totalsB = pairs.map(_._2.sum)
    val absDiff = mean(pairs.map { case (a, b) => math.abs(a.sum - b.sum) })
    val mA = mean(totalsA); val mB = mean(totalsB)
    val cov = mean(totalsA.zip(totalsB).map { case (x, y) => (x - mA) * (y - mB) })
    val sdA = math.sqrt(mean(totalsA.map(x => (x - mA) * (x - mA))))
    val sdB = math.sqrt(mean(totalsB.map(y => (y - mB) * (y - mB))))
    val r = if sdA == 0 || sdB == 0 then 0.0 else cov / (sdA * sdB)
    out(s"- Candidates rated by BOTH raters: ${pairs.size}")
    out(s"- Per-dimension exact-agreement rate: $dimAgree")
    out(s"- Mean absolute difference on the 0-12 total: ${f2(absDiff)}")
    out(s"- Pearson r on the 0-12 totals: ${f2(r)}")
    out("")
    out("_(Reliability is agreement, not validity; both raters are CF5 and share model-family bias.)_")
  else out("- Only one rater present; no inter-rater statistic.")
  out("")
  out("_Descriptive; LLM rater not bitwise-deterministic (Agent tool does not expose temp/seed) — best-effort._")

  os.write.over(os.Path(BASE) / "results" / "style-rater-analysis.md", sb.toString)
  println(s"[style_analyze] wrote style-rater-analysis.md (${finerById.size} candidates)")
  println(sb.toString)
