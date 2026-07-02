//> using scala 3.8.4
//> using jvm 21

// chrono — a simple stopwatch for timing work spans (a human-agent-human round, or any manual span).
// The running start is held in an ephemeral state file; each completed span is appended to a TSV log.
// (Stateful/IO by nature — that's the point — unlike the pure text/typo tools.)
//   tt chrono start [label]   record a start time (label optional)
//   tt chrono stop            report elapsed since the last start + append it to the log
//   tt chrono now             print the current timestamp
import java.nio.file.{Files, Path, StandardOpenOption}
import java.time.{Instant, ZoneId}
import java.time.format.DateTimeFormatter

val stateFile = Path.of(sys.props.getOrElse("tt.chrono.state",
  System.getProperty("java.io.tmpdir") + "/tt-chrono-state.tsv"))
val logFile = Path.of(sys.props.getOrElse("tt.chrono.log",
  "/home/bjornr/git/berg/bjornregnell/genscalator/research/wr-data/chrono-log.tsv"))

/** Format a millisecond duration compactly: "0.42s", "45s", "1m 18s". Pure. */
def fmt(ms: Long): String =
  if ms < 10_000 then f"${ms / 1000.0}%.2fs"
  else
    val s = ms / 1000
    if s < 60 then s"${s}s" else s"${s / 60}m ${s % 60}s"

def iso(ms: Long): String =
  DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(ms))

/** Parse a relayed think-time — "30s", "1m18s", "1m", or bare seconds "90" — into ms. Pure. */
def parseThinkMs(s: String): Long =
  val c = s.trim.toLowerCase
  val min = "(\\d+)\\s*m".r.findFirstMatchIn(c).map(_.group(1).toLong).getOrElse(0L)
  val sec = "(\\d+)\\s*s".r.findFirstMatchIn(c).map(_.group(1).toLong).getOrElse(0L)
  if min == 0 && sec == 0 then
    val digits = c.filter(_.isDigit)
    if digits.isEmpty then 0L else digits.toLong * 1000
  else (min * 60 + sec) * 1000

@main def chrono(args: String*): Unit =
  val now = System.currentTimeMillis()
  args.toList match
    case "start" :: rest =>
      val label = rest.mkString(" ")
      Files.writeString(stateFile, s"$now\t$label")
      println(s"chrono: started at ${iso(now)}${if label.nonEmpty then s"  [$label]" else ""}")
    case "stop" :: rest =>
      if !Files.exists(stateFile) then
        println("chrono: no start recorded (run `tt chrono start` first)")
        sys.exit(1)
      val thinkMs = rest.sliding(2).collectFirst { case Seq("--think", v) => parseThinkMs(v) }.getOrElse(-1L)
      val parts = Files.readString(stateFile).split("\t", 2)
      val start = parts(0).trim.toLong
      val label = if parts.length > 1 then parts(1).trim else ""
      val elapsed = now - start
      if !Files.exists(logFile) then Files.writeString(logFile, "start\tstop\telapsed_ms\tlabel\tthink_ms\n")
      Files.writeString(logFile, s"${iso(start)}\t${iso(now)}\t$elapsed\t$label\t${if thinkMs >= 0 then thinkMs else ""}\n", StandardOpenOption.APPEND)
      Files.deleteIfExists(stateFile)
      println(s"chrono: elapsed ${fmt(elapsed)}${if label.nonEmpty then s"  [$label]" else ""} (logged)")
      if thinkMs >= 0 then
        println(s"        = think ${fmt(thinkMs)} + human ${fmt(math.max(0, elapsed - thinkMs))}")
    case "now" :: _ =>
      println(iso(now))
    case "fmt" :: ms :: _ => // format a duration in ms (debug/test util for the pure formatter)
      println(fmt(ms.toLong))
    case "think" :: v :: _ => // parse a think-duration to ms (debug/test util, inverse of fmt)
      println(parseThinkMs(v).toString)
    case _ =>
      println("usage: chrono start [label] | stop [--think <dur>] | now | fmt <ms> | think <dur>   (spans -> chrono-log.tsv)")
      sys.exit(2)
