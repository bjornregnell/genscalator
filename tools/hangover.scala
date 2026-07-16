//> using scala 3.8.4
//> using jvm 21

// hangover — detect a just-ended "agent blackout" by the wall-clock GAP at a session boundary (SM121).
// The agent cannot perceive a guard stall / long idle / compact / box crash from INSIDE (no running observer
// during the pause; no marker after). On resume it can only feel the HANGOVER: read the transcript, compare
// NOW to the last conversational record's timestamp, and flag a gap that dwarfs normal execution time. It
// detects THAT the agent was out, not the CAUSE (idle vs stall vs long command vs crash all look alike from a
// gap alone) — but a `compact_boundary` among the recent records names the "out" as a compact.
//
// Pure read -> compute -> print a one-line verdict, no shell post-processing. The SURFACE wiring (a statusline
// segment, a SessionStart / warm hook, `gs warm`) is deliberately NOT here — that is the joint SM121 co-design;
// this file is the reusable, testable CORE. Grounded by research/sm-investigations/SM128-SM121-transcript-...md
// (conversational records carry ISO-8601 timestamps; the boundary is `{type:system, subtype:compact_boundary}`).
//   tt hangover <transcript.jsonl>                     report the resume-gap verdict (now = system clock)
//   tt hangover <transcript.jsonl> --now-ms N          fixed "now" as epoch-ms (deterministic tests)
//   tt hangover <transcript.jsonl> --threshold-sec N   a gap >= N seconds counts as a hangover (default 900)
import java.nio.file.{Files, Path}

object HangoverTool:
  /** A transcript record we care about: its wall-clock instant and whether it is a compact boundary. */
  case class Rec(epochMs: Long, isCompactBoundary: Boolean)
  /** The resume-gap finding. atCompactBoundary = a compact boundary sits among the most-recent records. */
  case class Verdict(gapSec: Long, isHangover: Boolean, atCompactBoundary: Boolean)

  /** Cheap top-level string-field extractor — we need only `timestamp` / `subtype`, so a full JSON parse (or
    * MiniJson) would be overkill here; a per-line regex is enough and keeps this tool dependency-free. PURE. */
  private def strField(line: String, key: String): Option[String] =
    ("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"").r.findFirstMatchIn(line).map(_.group(1))

  /** ISO-8601 (e.g. "2026-07-16T11:07:58.421Z") -> epoch-ms; None if unparseable. PURE. */
  def parseInstantMs(iso: String): Option[Long] =
    scala.util.Try(java.time.Instant.parse(iso).toEpochMilli).toOption

  /** Parse transcript lines into timestamped records, in file order; lines without a timestamp are skipped
    * (many infra record types carry none — see the probe note). PURE.
    * DRY: `statusline.TranscriptStats` also scans the transcript (for tokens/chars); when SM128 lands, factor a
    * shared transcript reader rather than growing two scanners. */
  def parse(lines: Seq[String]): Vector[Rec] =
    lines.iterator.flatMap { l =>
      strField(l, "timestamp").flatMap(parseInstantMs).map { ms =>
        Rec(ms, strField(l, "subtype").contains("compact_boundary"))
      }
    }.toVector

  /** Compare `nowMs` to the last timestamped record: the resume gap. A gap >= `thresholdSec` is a hangover.
    * atCompactBoundary looks at the last few records (the boundary is followed by the post-compact prompt, so
    * it is not strictly last). None if there are no timestamped records to compare against. PURE. */
  def detect(recs: Vector[Rec], nowMs: Long, thresholdSec: Long): Option[Verdict] =
    recs.lastOption.map { last =>
      val gapSec = math.max(0L, (nowMs - last.epochMs) / 1000L)
      Verdict(gapSec, gapSec >= thresholdSec, recs.takeRight(3).exists(_.isCompactBoundary))
    }

  /** Seconds -> a compact human gap, largest unit first: "11h 38m", "5m 3s", "42s". PURE. */
  def formatGap(sec: Long): String =
    val h = sec / 3600; val m = (sec % 3600) / 60; val s = sec % 60
    if h > 0 then f"${h}h ${m}m" else if m > 0 then f"${m}m ${s}s" else f"${s}s"

  /** The final one-line verdict string for a parsed transcript. PURE (given nowMs). */
  def report(lines: Seq[String], nowMs: Long, thresholdSec: Long): String =
    detect(parse(lines), nowMs, thresholdSec) match
      case None => "hangover: no timestamped records to compare (nothing to say)"
      case Some(v) if v.isHangover =>
        val cause =
          if v.atCompactBoundary then " (a compact boundary is at the seam, likely a compact)"
          else " (cause unknown: idle / guard stall / long command / crash all look alike from the gap)"
        s"hangover: ~${formatGap(v.gapSec)} since last activity, you were likely out$cause"
      case Some(v) => s"no hangover: last activity ${formatGap(v.gapSec)} ago (under threshold)"

  private val Help: String =
    """tt hangover — detect a just-ended agent blackout by the resume-gap (SM121)
      |
      |The agent cannot perceive a guard stall / long idle / compact / crash from inside — it can only feel
      |the HANGOVER on resume. This reads the transcript, compares NOW to the last conversational record's
      |timestamp, and flags a gap that dwarfs normal execution time. It detects THAT you were out, not the
      |cause; a compact boundary at the seam names the out as a compact.
      |
      |Usage:
      |  hangover <transcript.jsonl>                    verdict, now = system clock
      |  hangover <transcript.jsonl> --now-ms N         fixed now as epoch-ms (deterministic tests)
      |  hangover <transcript.jsonl> --threshold-sec N  gap >= N seconds is a hangover (default 900 = 15 min)
      |
      |Prints one line: a hangover with the gap + a cause note, or "no hangover". Surface wiring (statusline /
      |warm hook) is deliberately separate; this is the reusable core.
      |
      |Full reference: tools/README.md""".stripMargin

  def dispatch(args: List[String]): Int =
    if args.contains("--help") || args.contains("-h") then { println(Help); return 0 }
    def flagVal(name: String): Option[String] =
      val i = args.indexOf(name); if i >= 0 && i + 1 < args.size then Some(args(i + 1)) else None
    val flagNames = Set("--now-ms", "--threshold-sec")
    val consumed = args.zipWithIndex.flatMap { case (t, i) =>
      if flagNames(t) then Seq(i, i + 1) else Nil
    }.toSet
    val positional = args.zipWithIndex.collect { case (t, i) if !consumed(i) && !t.startsWith("--") => t }
    positional.headOption match
      case None =>
        Console.err.println("hangover: usage: tt hangover <transcript.jsonl> [--now-ms N] [--threshold-sec N]")
        2
      case Some(p) =>
        val path = Path.of(p)
        if !Files.isRegularFile(path) then
          Console.err.println(s"hangover: not a readable file: $p")
          2
        else
          val nowMs = flagVal("--now-ms").flatMap(_.toLongOption).getOrElse(System.currentTimeMillis())
          val thresholdSec = flagVal("--threshold-sec").flatMap(_.toLongOption).getOrElse(900L)
          val lines = String(Files.readAllBytes(path), "UTF-8").linesIterator.filter(_.trim.nonEmpty).toVector
          println(report(lines, nowMs, thresholdSec))
          0

@main def hangoverDetect(args: String*): Unit =
  sys.exit(HangoverTool.dispatch(args.toList))
