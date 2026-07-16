//> using scala 3.8.4
//> using jvm 21
//> using file minijson.scala

// hangover — detect a just-ended "agent blackout" by the wall-clock GAP at a session boundary (SM121).
// The agent cannot perceive a guard stall / long idle / compact / box crash from INSIDE (no running observer
// during the pause; no marker after). On resume it can only feel the HANGOVER: read the transcript, compare
// NOW to the last conversational record's timestamp, and flag a gap that dwarfs normal execution time. It
// detects THAT the agent was out, not the CAUSE (idle vs stall vs long command vs crash all look alike from a
// gap alone) — but a `compact_boundary` among the recent records names the "out" as a compact.
//
// Pure read -> compute -> print a one-line verdict, no shell post-processing. Grounded by
// research/sm-investigations/SM128-SM121-transcript-...md (conversational records carry ISO-8601 timestamps;
// the boundary is `{type:system, subtype:compact_boundary}`).
//
// SURFACE (BR's SM121 decision: option (b)) = the `hook` subcommand, wired as a Claude Code **SessionStart**
// hook. That event fires on all four boundaries — startup / resume / clear / compact — and hands the hook a
// `transcript_path` plus a `source` naming WHICH boundary it was. `source` is the piece the gap alone can
// never supply, so the hook says "you were out ~11h, the session was resumed" where the bare tool can only
// say "you were out ~11h, cause unknown". The hook JSON is read HERE (like `tt guardcheck hook`) rather than
// by a bash shim: no untyped, untested glue between Claude Code and the verdict.
//   tt hangover <transcript.jsonl>                     report the resume-gap verdict (now = system clock)
//   tt hangover <transcript.jsonl> --now-ms N          fixed "now" as epoch-ms (deterministic tests)
//   tt hangover <transcript.jsonl> --threshold-sec N   a gap >= N seconds counts as a hangover (default 900)
//   tt hangover hook [<json>]                          SessionStart hook: stdin (or arg) JSON -> a named line
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

  /** Claude Code's SessionStart `source` -> what the boundary WAS. This is the hook's whole advantage over the
    * bare tool: a gap alone cannot tell a resume from a crash from an idle, but `source` names the seam.
    * HONEST LIMIT (kept in the wording): `source` names the boundary, NOT the whole gap — a resume seam says
    * nothing about how long the human sat idle before exiting, so we say "at the seam", never "because of". PURE. */
  def causeOf(source: Option[String]): String = source match
    case Some("resume")  => "the session was resumed (exit/restart at the seam)"
    case Some("compact") => "a compact ran at the seam"
    case Some("clear")   => "the context was cleared at the seam"
    case Some("startup") => "a fresh session started at the seam"
    case _               => "cause unknown (idle / guard stall / long command / crash look alike from the gap)"

  /** The SessionStart hook's line: the resume-gap verdict NAMED by the boundary source. Empty string = print
    * nothing — no hangover, or a fresh transcript with no records. Silence is the point: this text is injected
    * into the agent's context on EVERY session start, so it must earn its tokens by only speaking when the
    * agent actually was out. PURE given `lines` + `nowMs`. */
  def hookReport(source: Option[String], lines: Seq[String], nowMs: Long, thresholdSec: Long): String =
    detect(parse(lines), nowMs, thresholdSec) match
      case Some(v) if v.isHangover =>
        s"hangover: ~${formatGap(v.gapSec)} since your last activity — ${causeOf(source)}."
      case _ => ""

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
      |  hangover hook [<json>]                        Claude Code SessionStart hook: reads the hook JSON on
      |                                                stdin (or as an arg, for testing) and prints a hangover
      |                                                line NAMED by `source` (startup/resume/clear/compact),
      |                                                or nothing when you were not out. Always exits 0.
      |
      |Prints one line: a hangover with the gap + a cause note, or "no hangover". The `hook` surface is silent
      |unless there IS a hangover — its output is injected into the agent's context on every session start.
      |
      |Wiring the hook (human-gated, see docs/hangover-hook.md):
      |  "hooks": { "SessionStart": [ { "hooks": [ { "type": "command", "command": "tt hangover hook" } ] } ] }
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
    val nowMs = flagVal("--now-ms").flatMap(_.toLongOption).getOrElse(System.currentTimeMillis())
    val thresholdSec = flagVal("--threshold-sec").flatMap(_.toLongOption).getOrElse(900L)
    positional match
      case "hook" :: rest => hook(rest, nowMs, thresholdSec)
      case Nil =>
        Console.err.println("hangover: usage: tt hangover <transcript.jsonl> [--now-ms N] [--threshold-sec N]")
        Console.err.println("hangover:        tt hangover hook [<json>]   (Claude Code SessionStart hook)")
        2
      case p :: _ =>
        val path = Path.of(p)
        if !Files.isRegularFile(path) then
          Console.err.println(s"hangover: not a readable file: $p")
          2
        else
          println(report(readLines(path), nowMs, thresholdSec))
          0

  /** Read a transcript's non-blank lines. EFFECTFUL (file read). */
  private def readLines(path: Path): Vector[String] =
    String(Files.readAllBytes(path), "UTF-8").linesIterator.filter(_.trim.nonEmpty).toVector

  /** SessionStart hook: take Claude Code's hook JSON (stdin, or an arg for tests), pull `source` +
    * `transcript_path`, and print the named hangover line — nothing when the agent was not out.
    * FAIL-SOFT BY CONSTRUCTION: any malformed JSON / missing / unreadable transcript yields silence, and it
    * always exits 0. A SessionStart hook runs before the agent can do anything, so a throw or a non-zero exit
    * here would greet the human with an error at the very moment they start working — this tool is never
    * worth breaking a session start over. EFFECTFUL (stdin + file read); the decision is in `hookReport`. */
  private def hook(rest: List[String], nowMs: Long, thresholdSec: Long): Int =
    try
      val json = if rest.nonEmpty then rest.mkString(" ") else scala.io.Source.stdin.mkString
      val o = MiniJson.parse(json)
      val source = o.flatMap(_.field("source")).flatMap(_.str)
      val lines = o.flatMap(_.field("transcript_path")).flatMap(_.str)
        .map(Path.of(_)).filter(Files.isRegularFile(_)).map(readLines).getOrElse(Vector.empty)
      val out = hookReport(source, lines, nowMs, thresholdSec)
      if out.nonEmpty then println(out)
    catch case _: Throwable => ()
    0

@main def hangoverDetect(args: String*): Unit =
  sys.exit(HangoverTool.dispatch(args.toList))
