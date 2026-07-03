//> using scala 3.8.4
//> using dep com.lihaoyi::os-lib:0.11.8

// Significance analysis for the indent-vs-braces edit-cost experiment.
//   scala-cli run significance.scala -- results-raw.tsv     (pilot, 7 models — exact + MC agree)
//   scala-cli run significance.scala -- results-main.tsv    (BIG run, ~50 models — MC path)
//
// Non-parametric permutation tests that respect the design: the unit of replication is
// the MODEL. The 6 repeats and 3 task-sizes inside a model are NOT independent (a weak
// model fails many cells together), so pooling all cells as independent Bernoulli trials
// is pseudoreplication. We block by model and permute style labels WITHIN each model.
//
// TWO paths, chosen by sample size n:
//   * EXACT enumeration of the permutation null (6^n omnibus, 2^n paired) when n is small
//     enough to enumerate — deterministic, RNG-free.
//   * Seeded MONTE-CARLO permutation (fixed SEED, R relabelings) when n is too large to
//     enumerate — deterministic GIVEN THE SEED. This is the BIG-run path (6^50 is not
//     enumerable). Preregistered in BIG-RUN-PREREG.md §5.
// At small n we print BOTH so the MC estimate self-validates against the exact truth.
//
// Header-aware: detects the style column (named "style" or "regime") and the "graded"
// column by name, so it reads both the pilot TSV and the main-experiment TSV unchanged.

// ---- FROZEN Monte-Carlo parameters (preregistered — do not change after data) ----
val SEED: Long = 20260703L
val R: Int     = 100000  // MC relabelings per test

// Enumerate exactly only when the null is small enough to walk in full.
val ExactOmnibusMaxN = 10 // 6^10 = 60.5M
val ExactPairedMaxN  = 24 // 2^24 = 16.8M per pair

case class Row(task: String, style: String, model: String, graded: String)

@main def run(tsv: String): Unit =
  val lines = os.read.lines(os.Path(tsv, os.pwd)).filter(_.nonEmpty)
  val header = lines.head.split("\t").toVector
  def col(names: String*): Int =
    names.iterator.map(header.indexOf).find(_ >= 0).getOrElse(
      sys.error(s"none of ${names.mkString("/")} in header: ${header.mkString(",")}"))
  val (ti, si, mi, gi) = (col("task"), col("style", "regime"), col("model"), col("graded"))
  val rows = lines.drop(1).map { l => val c = l.split("\t"); Row(c(ti), c(si), c(mi), c(gi)) }.toList

  val styles = List("braceless", "braces", "common") // index 0,1,2
  val models = rows.map(_.model).distinct.sorted
  val n = models.size

  def passFrac(m: String, st: String): Double =
    val cells = rows.filter(r => r.model == m && r.style == st)
    if cells.isEmpty then 0.0 else cells.count(_.graded == "PASS").toDouble / cells.size
  val M: Array[Array[Double]] = models.map(m => styles.map(st => passFrac(m, st)).toArray).toArray

  println(s"source: $tsv   |   models (n=$n), pass-fraction by style:")
  for (m, i) <- models.zipWithIndex do
    println(f"  $m%-24s braceless=${M(i)(0)}%.3f  braceful=${M(i)(1)}%.3f  common=${M(i)(2)}%.3f")
  val colMean = Array(0,1,2).map(j => M.map(_(j)).sum / n)
  println(f"  column means           braceless=${colMean(0)}%.3f  braceful=${colMean(1)}%.3f  common=${colMean(2)}%.3f")
  val grand = colMean.sum / 3.0
  println(f"  grand mean pass-frac    = $grand%.3f  (invariant under within-model relabeling)")

  // One deterministic RNG for all Monte-Carlo draws (seeded once → reproducible).
  val rng = new scala.util.Random(SEED)
  val perms = Array(Array(0,1,2),Array(0,2,1),Array(1,0,2),Array(1,2,0),Array(2,0,1),Array(2,1,0))
  def spread(c: Array[Double]) = (c(0)-grand)*(c(0)-grand)+(c(1)-grand)*(c(1)-grand)+(c(2)-grand)*(c(2)-grand)
  val Sobs = spread(colMean)

  // ---- OMNIBUS: within-model permutation test (style labels exchangeable within model) ----
  // Statistic: spread of the style column-means = sum_j (mean_j - grand)^2.
  def omnibusExact: Double =
    val permVals = M.map(v => perms.map(p => Array(v(p(0)), v(p(1)), v(p(2)))))
    val total = math.pow(6, n).toLong
    var ge = 0L; var idx = 0L
    while idx < total do
      var t = idx; val c = Array(0.0,0.0,0.0); var k = 0
      while k < n do { val d = (t % 6).toInt; t /= 6; val pv = permVals(k)(d); c(0)+=pv(0); c(1)+=pv(1); c(2)+=pv(2); k += 1 }
      c(0)/=n; c(1)/=n; c(2)/=n
      if spread(c) >= Sobs - 1e-12 then ge += 1
      idx += 1
    ge.toDouble / total
  def omnibusMC: Double =
    var ge = 0L; var d = 0
    while d < R do
      val c = Array(0.0,0.0,0.0); var k = 0
      while k < n do { val p = perms(rng.nextInt(6)); val v = M(k); c(0)+=v(p(0)); c(1)+=v(p(1)); c(2)+=v(p(2)); k += 1 }
      c(0)/=n; c(1)/=n; c(2)/=n
      if spread(c) >= Sobs - 1e-12 then ge += 1
      d += 1
    (1.0 + ge) / (R + 1.0) // +1 correction: observed labeling counts as one draw

  println(f"%nOMNIBUS within-model permutation test (H0: style has no effect):")
  println(f"  observed column-mean spread S = $Sobs%.5f")
  if n <= ExactOmnibusMaxN then println(f"  p(exact, ${math.pow(6,n).toLong} relabelings)  = ${omnibusExact}%.4f")
  else                          println(f"  p(exact) = —  (6^$n not enumerable)")
  println(f"  p(Monte-Carlo, seed=$SEED, R=$R) = ${omnibusMC}%.4f")

  // ---- PAIRWISE: paired sign-flip permutation, two-sided ----
  def pairExact(a: Int, b: Int): (Double, Double) =
    val diffs = M.map(v => v(a) - v(b)); val obs = diffs.sum / n
    val combos = 1L << n; var cnt = 0L; var kk = 0L
    while kk < combos do
      var s = 0.0; var i = 0
      while i < n do { s += (if ((kk >> i & 1) == 1) 1.0 else -1.0) * diffs(i); i += 1 }
      if math.abs(s / n) >= math.abs(obs) - 1e-12 then cnt += 1
      kk += 1
    (obs, cnt.toDouble / combos)
  def pairMC(a: Int, b: Int): (Double, Double) =
    val diffs = M.map(v => v(a) - v(b)); val obs = diffs.sum / n
    var cnt = 0L; var d = 0
    while d < R do
      var s = 0.0; var i = 0
      while i < n do { s += (if (rng.nextBoolean()) 1.0 else -1.0) * diffs(i); i += 1 }
      if math.abs(s / n) >= math.abs(obs) - 1e-12 then cnt += 1
      d += 1
    (obs, (1.0 + cnt) / (R + 1.0))

  println(f"%nPAIRWISE paired sign-flip permutation, two-sided:")
  for (a, b, nm) <- List((0,1,"braceless vs braceful"),(0,2,"braceless vs common"),(1,2,"braceful vs common")) do
    val (d, pMC) = pairMC(a, b)
    val exactStr = if n <= ExactPairedMaxN then f"exact ${pairExact(a,b)._2}%.4f" else "exact —"
    println(f"  $nm%-24s mean diff = $d%+.3f   $exactStr   MC(seed=$SEED) ${pMC}%.4f")

  // ---- Friedman test, asymptotic chi-square (df = k-1 = 2; survival = exp(-x/2)) ----
  def ranksOf(v: Array[Double]): Array[Double] =
    val ord = Array(0,1,2).sortBy(v(_)) // ascending pass-frac
    val r = Array(0.0,0.0,0.0); var i = 0
    while i < 3 do
      var j = i
      while j+1 < 3 && v(ord(j+1)) == v(ord(i)) do j += 1
      val avg = ((i+1)+(j+1))/2.0; var kk = i
      while kk <= j do { r(ord(kk)) = avg; kk += 1 }
      i = j + 1
    r
  val Rj = Array(0.0,0.0,0.0)
  M.foreach { v => val r = ranksOf(v); Rj(0)+=r(0); Rj(1)+=r(1); Rj(2)+=r(2) }
  val k = 3
  val chiF = 12.0/(n*k*(k+1)) * Rj.map(x => x*x).sum - 3*n*(k+1)
  println(f"%nFriedman test (rank styles within each model): chi2 = $chiF%.3f (df=2), p ~ ${math.exp(-chiF/2)}%.4f")
  println(f"  rank sums: braceless=${Rj(0)}%.1f  braceful=${Rj(1)}%.1f  common=${Rj(2)}%.1f (lower = better)")

  // ---- FOIL: naive pooled chi-square over all cells (PSEUDOREPLICATION — do NOT trust) ----
  val N = rows.size
  val overall = rows.count(_.graded == "PASS").toDouble / N
  var chiP = 0.0
  for st <- styles do
    val cs = rows.filter(_.style == st); val nst = cs.size.toDouble
    val op = cs.count(_.graded == "PASS").toDouble
    val ep = nst*overall; val ef = nst*(1-overall)
    chiP += (op-ep)*(op-ep)/ep + ((nst-op)-ef)*((nst-op)-ef)/ef
  println(f"%nFOIL — naive pooled chi-square, all $N cells as independent (PSEUDOREPLICATION):")
  println(f"  chi2 = $chiP%.2f (df=2), p ~ ${math.exp(-chiP/2)}%.2e  <- overstated; ignores model clustering")
