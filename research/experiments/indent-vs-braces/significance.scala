//> using scala 3.8.4
//> using dep com.lihaoyi::os-lib:0.11.8

// Significance analysis for the indent-vs-braces edit-cost experiment.
//   scala-cli run significance.scala -- results-raw.tsv
//
// Non-parametric, EXACT permutation tests that respect the design: the unit of
// replication is the MODEL. The 6 repeats and 3 task-sizes inside a model are NOT
// independent (a weak model fails many cells together), so pooling all 378 cells as
// independent Bernoulli trials is pseudoreplication. We therefore block by model and
// permute style labels WITHIN each model.
//
// Deterministic: exact enumeration of the permutation null (6^7 omnibus, 2^7 pairwise).
// No RNG, so re-running gives identical p-values.

case class Row(task: String, style: String, model: String, graded: String)

@main def run(tsv: String): Unit =
  val rows = os.read.lines(os.Path(tsv, os.pwd)).drop(1).filter(_.nonEmpty).map { l =>
    val c = l.split("\t"); Row(c(0), c(1), c(2), c(4))
  }.toList
  val styles = List("braceless", "braces", "common") // index 0,1,2
  val models = rows.map(_.model).distinct.sorted
  val n = models.size

  def passFrac(m: String, st: String): Double =
    val cells = rows.filter(r => r.model == m && r.style == st)
    cells.count(_.graded == "PASS").toDouble / cells.size
  val M: Array[Array[Double]] = models.map(m => styles.map(st => passFrac(m, st)).toArray).toArray

  println(s"models (n=$n), pass-fraction by style (out of 18 cells each):")
  for (m, i) <- models.zipWithIndex do
    println(f"  $m%-24s braceless=${M(i)(0)}%.3f  braceful=${M(i)(1)}%.3f  common=${M(i)(2)}%.3f")
  val colMean = Array(0,1,2).map(j => M.map(_(j)).sum / n)
  println(f"  column means           braceless=${colMean(0)}%.3f  braceful=${colMean(1)}%.3f  common=${colMean(2)}%.3f")
  val grand = colMean.sum / 3.0
  println(f"  grand mean pass-frac    = $grand%.3f  (invariant under within-model relabeling)")

  // ---- OMNIBUS: exact within-model permutation test, 6^n enumerated ----
  // H0: the 3 style labels are exchangeable within each model (style has no effect).
  // Statistic: spread of the style column-means = sum_j (mean_j - grand)^2.
  def spread(c: Array[Double]) = (c(0)-grand)*(c(0)-grand)+(c(1)-grand)*(c(1)-grand)+(c(2)-grand)*(c(2)-grand)
  val Sobs = spread(colMean)
  val perms = List(Array(0,1,2),Array(0,2,1),Array(1,0,2),Array(1,2,0),Array(2,0,1),Array(2,1,0))
  val permVals: Array[Array[Array[Double]]] =
    M.map(v => perms.map(p => Array(v(p(0)), v(p(1)), v(p(2)))).toArray)
  val totalPerms = math.pow(6, n).toLong
  var ge = 0L; var idx = 0L
  while idx < totalPerms do
    var t = idx; val col = Array(0.0,0.0,0.0); var mi = 0
    while mi < n do
      val d = (t % 6).toInt; t /= 6
      val pv = permVals(mi)(d); col(0)+=pv(0); col(1)+=pv(1); col(2)+=pv(2); mi += 1
    col(0)/=n; col(1)/=n; col(2)/=n
    if spread(col) >= Sobs - 1e-12 then ge += 1
    idx += 1
  val pOmnibus = ge.toDouble / totalPerms
  println(f"%nOMNIBUS exact within-model permutation test (all $totalPerms relabelings):")
  println(f"  observed column-mean spread S = $Sobs%.5f")
  println(f"  p(any style effect) = $pOmnibus%.4f")

  // ---- PAIRWISE: exact paired sign-flip permutation (2^n), two-sided ----
  def pairP(a: Int, b: Int): (Double, Double) =
    val diffs = M.map(v => v(a) - v(b)) // per-model paired diff in pass-fraction
    val obs = diffs.sum / n
    val combos = 1 << n; var cnt = 0; var kk = 0
    while kk < combos do
      var s = 0.0; var i = 0
      while i < n do { s += (if ((kk >> i & 1) == 1) 1.0 else -1.0) * diffs(i); i += 1 }
      if math.abs(s / n) >= math.abs(obs) - 1e-12 then cnt += 1
      kk += 1
    (obs, cnt.toDouble / combos)
  println(f"%nPAIRWISE exact paired sign-flip permutation (${1 << n} sign-combos), two-sided:")
  for (a, b, nm) <- List((0,1,"braceless vs braceful"),(0,2,"braceless vs common"),(1,2,"braceful vs common")) do
    val (d, p) = pairP(a, b)
    println(f"  $nm%-24s mean pass-frac diff = $d%+.3f   p = $p%.4f")

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
