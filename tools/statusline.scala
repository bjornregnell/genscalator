//> using scala 3.8.4
//> using jvm 21
//> using dep com.lihaoyi::ujson:4.4.3

// statusline — format the Claude Code `statusLine` stdin JSON into ONE compact line (SM039).
// Claude Code pipes a JSON object to the configured statusLine command's stdin each turn; this reads it and prints:
//   Opus 4.8 · $12.34 · ctx 41% · 5h 30% · wk 14% (resets 3d)
// Every segment is INDEPENDENTLY GUARDED: a field absent from the JSON simply omits its segment, so the tool
// degrades gracefully across CC versions / subscription tiers (rate_limits are Claude Pro/Max only) and NEVER
// crashes the prompt — a bad/empty stdin prints an empty line, exit 0.
// Wire it (BR — human-gated settings step): add to .claude/settings.json:
//   "statusLine": { "type": "command", "command": "tt statusline" }
// Fields consumed (per https://code.claude.com/docs/en/statusline.md — confirm against a real invocation):
//   model.display_name|model.id · cost.total_cost_usd · context_window.used_percentage ·
//   rate_limits.five_hour.used_percentage · rate_limits.seven_day.{used_percentage,resets_at}
// Reads stdin by default; also accepts the JSON as a positional arg + `--now-ms N` (both for deterministic tests).
object StatuslineTool: // NB not "Statusline" — that collides case-only with the `statusLine` @main on case-insensitive FS
  def pct(v: Double): String = s"${v.round.toInt}%"

  /** Relative "time until reset" from an epoch that may be in SECONDS or MILLISECONDS (auto-detected). PURE. */
  def relReset(resetsAt: Long, nowMs: Long): String =
    val resetMs = if resetsAt < 1000000000000L then resetsAt * 1000L else resetsAt // <1e12 ⇒ seconds
    val deltaMs = resetMs - nowMs
    if deltaMs <= 0 then "now"
    else
      val mins = deltaMs / 60000L
      if mins < 60 then s"${mins}m"
      else if mins < 1440 then s"${mins / 60}h"
      else s"${mins / 1440}d"

  /** PURE: statusLine JSON + current time → the one-line status ("" if nothing usable / not JSON). */
  def render(json: String, nowMs: Long): String =
    val o = try ujson.read(json).obj catch case _: Throwable => return ""
    val segs = scala.collection.mutable.ArrayBuffer[String]()
    o.get("model").flatMap(_.objOpt).foreach: m =>
      m.get("display_name").orElse(m.get("id")).flatMap(_.strOpt).foreach(segs += _)
    o.get("cost").flatMap(_.objOpt).flatMap(_.get("total_cost_usd")).flatMap(_.numOpt)
      .foreach(c => segs += f"$$$c%.2f")
    o.get("context_window").flatMap(_.objOpt).flatMap(_.get("used_percentage")).flatMap(_.numOpt)
      .foreach(p => segs += s"ctx ${pct(p)}")
    val rl = o.get("rate_limits").flatMap(_.objOpt)
    rl.flatMap(_.get("five_hour")).flatMap(_.objOpt).flatMap(_.get("used_percentage")).flatMap(_.numOpt)
      .foreach(p => segs += s"5h ${pct(p)}")
    rl.flatMap(_.get("seven_day")).flatMap(_.objOpt).foreach: w =>
      val used  = w.get("used_percentage").flatMap(_.numOpt).map(p => s"wk ${pct(p)}")
      val reset = w.get("resets_at").flatMap(_.numOpt).map(r => s"resets ${relReset(r.toLong, nowMs)}")
      (used, reset) match
        case (Some(u), Some(r)) => segs += s"$u ($r)"
        case (Some(u), None)    => segs += u
        case (None, Some(r))    => segs += s"wk $r"
        case _                  =>
    segs.mkString(" · ")

  def dispatch(args: List[String]): Int =
    var nowMs = System.currentTimeMillis()
    val pos = scala.collection.mutable.ArrayBuffer[String]()
    val a = args.toVector
    var i = 0
    while i < a.length do
      a(i) match
        case "--now-ms" if i + 1 < a.length => nowMs = a(i + 1).toLongOption.getOrElse(nowMs); i += 2
        case other                          => pos += other; i += 1
    val json = pos.headOption.getOrElse(scala.io.Source.stdin.mkString)
    println(render(json, nowMs))
    0

@main def statusLine(args: String*): Unit = sys.exit(StatuslineTool.dispatch(args.toList))
