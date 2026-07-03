//> using scala 3.8.4
//> using dep com.lihaoyi::os-lib:0.11.8

// pull-driver — pull the FROZEN confirmatory model list (models-frozen.txt) onto the compute box,
// disk-guarded and resumable. Idempotent: `ollama pull` of an already-present model is a fast no-op,
// so re-running after a crash resumes where it left off. Appends every outcome to pull-log.tsv.
//   scala-cli run pull-driver.scala -- [--floor 80] [--host bjornyx.local]
// Honesty: pulls the frozen list in file order; a pull that fails is LOGGED (never silently replaced);
// if free disk drops below --floor GB the driver STOPS pulling (never pushes the box toward full).
import scala.util.Try

val Root = os.Path("/home/bjornr/git/berg/bjornregnell/genscalator/research/experiments/indent-vs-braces")

def ssh(host: String, timeoutMs: Long, remote: String*): (Int, String, String) =
  val argv = Seq("ssh", "-o", "BatchMode=yes", "-o", "ConnectTimeout=8", host) ++ remote
  Try(os.proc(argv).call(check = false, stdout = os.Pipe, stderr = os.Pipe, timeout = timeoutMs)) match
    case scala.util.Success(r) => (r.exitCode, r.out.text().trim, r.err.text().trim)
    case scala.util.Failure(e) => (255, "", e.getMessage)

def freeGb(host: String): Int =
  val (c, out, _) = ssh(host, 60_000, "df", "-BG", "/")
  if c != 0 then -1
  else out.linesIterator.toList.lastOption.map(_.split("\\s+")).filter(_.length >= 4)
        .flatMap(_(3).stripSuffix("G").toIntOption).getOrElse(-1)

@main def pullDriver(args: String*): Unit =
  var floor = 80; var host = "bjornyx.local"
  val it = args.iterator
  while it.hasNext do it.next() match
    case "--floor" if it.hasNext => floor = it.next().toIntOption.getOrElse(floor)
    case "--host"  if it.hasNext => host  = it.next()
    case other                   => System.err.println(s"pull-driver: ignoring arg '$other'")

  val models = os.read.lines(Root / "models-frozen.txt").map(_.trim).filter(_.nonEmpty).toList
  val log = Root / "pull-log.tsv"
  if !os.exists(log) then os.write(log, "model\tstatus\tfree_gb_after\tseconds\n")
  println(s"pull-driver: ${models.size} frozen models -> $host, floor ${floor}G")

  for (m, i) <- models.zipWithIndex do
    val free = freeGb(host)
    if free >= 0 && free < floor then
      val line = s"$m\tSKIP_FLOOR_${free}G\t$free\t0\n"
      os.write.append(log, line)
      println(s"[${i+1}/${models.size}] $m -> STOP: ${free}G < floor ${floor}G — halting pulls")
      sys.exit(0)
    val t0 = System.nanoTime()
    val (code, _, err) = ssh(host, 3_600_000, "ollama", "pull", m)
    val secs = (System.nanoTime() - t0) / 1_000_000_000
    val after = freeGb(host)
    val status = if code == 0 then "OK" else s"FAIL_$code"
    os.write.append(log, s"$m\t$status\t$after\t$secs\n")
    val note = if code == 0 then "" else s"  ($err)".take(120)
    println(s"[${i+1}/${models.size}] $m -> $status  ${after}G free  ${secs}s$note")

  val ok = os.read.lines(log).drop(1).count(_.contains("\tOK\t"))
  println(s"pull-driver: done. OK pulls logged: $ok. See pull-log.tsv.")
