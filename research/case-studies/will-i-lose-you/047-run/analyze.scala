//> using scala 3.8.4
//> using dep com.lihaoyi::os-lib:0.11.8
//> using dep com.lihaoyi::upickle:4.4.3

// 047 coding-arm analysis (descriptive; ratified 5b — no inferential test on single
// deterministic observations). Reads results/coding.jsonl, emits results/analysis.md:
//   - correctness / style-fidelity / smell / compile-rate by (model x substrate)
//   - the qwen2.5-coder sub-ladder monotonicity (family fixed -> the clean capability claim)
//   - qwen2.5-coder vs qwen2.5 plain (code-tuning effect)
//   - the three pre-registered decision rules from 047-PLAN.md section 6
// Robust to partial data (analyzes whatever is present). Bare: `scala-cli run analyze.scala`.

val BASE = "/home/bjornr/git/berg/bjornregnell/genscalator/research/047-run"

case class Row(
  model: String, task: String, substrate: String,
  compiles: Boolean, testsPass: Int, testsTotal: Int,
  styleScore: Int, styleTotal: Int, smells: Int, reason: String
):
  def correctness: Double = if testsTotal > 0 then testsPass.toDouble / testsTotal else 0.0
  def style: Double = if styleTotal > 0 then styleScore.toDouble / styleTotal else 0.0

def loadRows(): List[Row] =
  val p = os.Path(BASE) / "results" / "coding.jsonl"
  if !os.exists(p) then Nil
  else os.read.lines(p).toList.flatMap { l =>
    try
      val o = ujson.read(l)
      Some(Row(
        o("model").str, o("task").str, o("substrate").str,
        o("compiles").bool, o("testsPass").num.toInt, o("testsTotal").num.toInt,
        o("styleScore").num.toInt, o("styleTotal").num.toInt,
        o("smells").num.toInt, o("reason").str
      ))
    catch case _: Throwable => None
  }

def f2(d: Double): String = f"$d%.2f"
def dd(v: Double, n: Int): String = if n == 0 then "-" else f2(v)  // '-' = no data (not a real 0)
def mean(xs: Seq[Double]): Double = if xs.isEmpty then 0.0 else xs.sum / xs.size
// style-fidelity is only meaningful for code that compiles (a non-compiling candidate would
// otherwise score style ~1.0 by vacuously passing absence-checks like "no var"). So style is
// always averaged over COMPILING cells only; correctness is over all cells (non-compile = 0).
def styleMean(rs: Seq[Row]): Double = mean(rs.filter(_.compiles).map(_.style))

// coder sub-ladder (family fixed) + plain control, in ascending size order
val CoderLadder = List("qwen2.5-coder:0.5b", "qwen2.5-coder:1.5b", "qwen2.5-coder:3b", "qwen2.5-coder:7b")
val PlainLadder = List("qwen2.5:0.5b", "qwen2.5:1.5b", "qwen2.5:3b", "qwen2.5:7b")
val Subs = List("full", "empty", "scrambled")

@main def analyze(): Unit =
  val rows = loadRows()
  val sb = new StringBuilder
  def out(s: String): Unit = sb.append(s).append("\n")

  out("# 047 coding-arm analysis (auto-generated, descriptive)")
  out("")
  out(s"Rows: ${rows.size} (cells scored). Models: ${rows.map(_.model).distinct.size}. " +
      s"Generated from `results/coding.jsonl`. Best-effort deterministic (temp 0 + seed 42).")
  out("")

  // ---- by substrate (aggregate over all models x tasks) ----
  out("## Fidelity by substrate (mean over all models x tasks)")
  out("")
  out("| substrate | n | correctness | style-fidelity | compile-rate | mean smells |")
  out("|---|---|---|---|---|---|")
  def subStats(sub: String): (Double, Double, Double, Double, Int) =
    val rs = rows.filter(_.substrate == sub)
    (mean(rs.map(_.correctness)), styleMean(rs),
     if rs.isEmpty then 0.0 else rs.count(_.compiles).toDouble / rs.size,
     mean(rs.map(_.smells.toDouble)), rs.size)
  out("_(style-fidelity averaged over compiling cells only; correctness over all cells.)_")
  out("")
  for sub <- Subs do
    val (c, st, cr, sm, n) = subStats(sub)
    out(s"| $sub | $n | ${dd(c, n)} | ${dd(st, n)} | ${dd(cr, n)} | ${dd(sm, n)} |")
  out("")

  // ---- qwen2.5-coder sub-ladder monotonicity (full substrate) ----
  out("## qwen2.5-coder sub-ladder (full substrate) — the clean capability claim")
  out("")
  out("| model | correctness | style-fidelity | compile-rate |")
  out("|---|---|---|---|")
  def modelSubStats(model: String, sub: String): (Double, Double, Double, Int) =
    val rs = rows.filter(r => r.model == model && r.substrate == sub)
    (mean(rs.map(_.correctness)), styleMean(rs),
     if rs.isEmpty then 0.0 else rs.count(_.compiles).toDouble / rs.size, rs.size)
  for m <- CoderLadder do
    val (c, st, cr, n) = modelSubStats(m, "full")
    if n > 0 then out(s"| $m | ${f2(c)} | ${f2(st)} | ${f2(cr)} |")
  out("")

  // ---- code-tuning control: coder vs plain (full substrate) ----
  out("## Code-tuning control: qwen2.5-coder vs qwen2.5 plain (full substrate)")
  out("")
  out("| size | coder correctness | plain correctness | coder style | plain style |")
  out("|---|---|---|---|---|")
  val sizes = List("0.5b", "1.5b", "3b", "7b")
  for s <- sizes do
    val (cc, cst, _, cn) = modelSubStats(s"qwen2.5-coder:$s", "full")
    val (pc, pst, _, pn) = modelSubStats(s"qwen2.5:$s", "full")
    if cn > 0 || pn > 0 then out(s"| $s | ${dd(cc, cn)} | ${dd(pc, pn)} | ${dd(cst, cn)} | ${dd(pst, pn)} |")
  out("")

  // ---- full per-(model x substrate) table ----
  out("## Full table (correctness / style) by model x substrate")
  out("")
  out("| model | full C | full S | empty C | empty S | scram C | scram S |")
  out("|---|---|---|---|---|---|---|")
  for m <- rows.map(_.model).distinct.sorted do
    val cells = Subs.map(s => modelSubStats(m, s))
    if cells.exists(_._4 > 0) then
      def cc(i: Int) = dd(cells(i)._1, cells(i)._4)
      def cs(i: Int) = dd(cells(i)._2, cells(i)._4)
      out(s"| $m | ${cc(0)} | ${cs(0)} | ${cc(1)} | ${cs(1)} | ${cc(2)} | ${cs(2)} |")
  out("")

  // ---- pre-registered decision rules (047-PLAN.md section 6) ----
  out("## Pre-registered decision rules (verdicts on current data)")
  out("")
  val (cFull, sFull, _, _, _) = subStats("full")
  val (cEmpty, sEmpty, _, _, _) = subStats("empty")
  val (cScram, sScram, _, _, _) = subStats("scrambled")
  // rule (a): substrate carries iff style full - empty >= 0.25
  val ruleA = (sFull - sEmpty) >= 0.25
  out(s"- **(a) substrate carries** iff style(full) - style(empty) >= 0.25 -> " +
      s"${f2(sFull)} - ${f2(sEmpty)} = ${f2(sFull - sEmpty)} -> **${if ruleA then "CARRIES" else "not met"}**")
  // rule (b): texture leaks iff style drops >= 0.25 while correctness drops < 0.10 (full->empty)
  val ruleB = (sFull - sEmpty) >= 0.25 && (cFull - cEmpty) < 0.10
  out(s"- **(b) texture leaks (facts carry)** iff style drop >= 0.25 AND correctness drop < 0.10 (full->empty) -> " +
      s"style drop ${f2(sFull - sEmpty)}, correctness drop ${f2(cFull - cEmpty)} -> **${if ruleB then "LEAKS (facts carry)" else "not met"}**")
  // rule (c): negative control holds iff for strongest model, scrambled style <= empty + 0.15
  val strongest = "qwen2.5-coder:7b"
  val (_, sStrEmpty, _, nSE) = modelSubStats(strongest, "empty")
  val (_, sStrScram, _, nSS) = modelSubStats(strongest, "scrambled")
  if nSE > 0 && nSS > 0 then
    val ruleC = sStrScram <= sStrEmpty + 0.15
    out(s"- **(c) negative control holds** iff for $strongest, style(scrambled) <= style(empty) + 0.15 -> " +
        s"${f2(sStrScram)} <= ${f2(sStrEmpty)} + 0.15 -> **${if ruleC then "HOLDS" else "VIOLATED (reads priors?)"}**")
  else
    out(s"- **(c) negative control** — insufficient data for $strongest (empty/scrambled) yet.")
  out("")
  out("_Descriptive only (ratified 5b): single deterministic observation per cell, no inferential test. " +
      "Verdicts are provisional on the data present at generation time._")

  val outPath = os.Path(BASE) / "results" / "analysis.md"
  os.write.over(outPath, sb.toString)
  println(s"[047-analyze] wrote $outPath (${rows.size} rows)")
  println(sb.toString)
