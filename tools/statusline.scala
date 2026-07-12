//> using scala 3.8.4
//> using jvm 21
//> using dep com.lihaoyi::ujson:4.4.3

// statusline — format the Claude Code `statusLine` stdin JSON into ONE compact line (SM039).
// Claude Code pipes a JSON object to the configured statusLine command's stdin each turn; this reads it and prints:
//   genscalator:  14:23:07  O4.8 (1M ctx)  ctx-fill: 41%  5h-lim: 30%  wk-lim: 14% resets: 3d  cost: $12.34
//   (leading HH:MM:SS wall clock; ANSI-coloured segments; two-space separators; ctx is a FILL gauge, 5h/wk LIMITs)
// Every segment is INDEPENDENTLY GUARDED: a field absent from the JSON simply omits its segment, so the tool
// degrades gracefully across CC versions / subscription tiers (rate_limits are Claude Pro/Max only) and NEVER
// crashes the prompt — a bad/empty stdin prints an empty line, exit 0.
// Wire it (BR — human-gated settings step): add to .claude/settings.json:
//   "statusLine": { "type": "command", "command": "tt statusline" }
// Fields consumed (per https://code.claude.com/docs/en/statusline.md — confirm against a real invocation):
//   model.display_name|model.id · cost.total_cost_usd · context_window.used_percentage ·
//   rate_limits.five_hour.{used_percentage,resets_at} · rate_limits.seven_day.{used_percentage,resets_at}
// Reads stdin by default; also accepts the JSON as a positional arg + `--now-ms N` (both for deterministic tests).
object StatuslineTool: // NB not "Statusline" — that collides case-only with the `statusLine` @main on case-insensitive FS
  def pct(v: Double): String = s"${v.round.toInt}%"

  // --- ANSI colour (SM039 polish). Each SEGMENT is wrapped as a WHOLE so its plain text stays a substring
  // (the tests match on `contains("ctx-fill: 41%")` etc.), and a bad/empty stdin still yields "" with no codes.
  // 256-colour; on a terminal without 256-colour support the sequences degrade to plain text.
  val ESC: Char = 27.toChar // the ANSI escape byte (0x1B); explicit to avoid any \u-escape ambiguity
  def sgr(code: String, s: String): String = s"${ESC}[${code}m${s}${ESC}[0m"
  val sep: String = "  " // two spaces between parts (the middot was dropped to save horizontal space)
  /** Gauge colour by level: a distinct healthy hue per part, escalating to orange >= 70% and red >= 90%. */
  def gauge(p: Double, healthy: String): String =
    if p >= 90 then "38;5;203" else if p >= 70 then "38;5;214" else healthy

  /** Local wall-clock HH:MM:SS from an epoch-ms (from --now-ms in tests, else System.currentTimeMillis). PURE. */
  def clock(nowMs: Long): String =
    java.time.Instant.ofEpochMilli(nowMs).atZone(java.time.ZoneId.systemDefault())
      .toLocalTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))

  /** Abbreviate the model label: "Opus 4.8 (1M context)" -> "O4.8 (1M ctx)"; "Fable 5" -> "F5"; etc. PURE. */
  def shortModel(name: String): String =
    name
      .replaceFirst("^Opus ",   "O")
      .replaceFirst("^Sonnet ", "S")
      .replaceFirst("^Fable ",  "F")
      .replaceFirst("^Haiku ",  "H")
      .replace("context", "ctx")

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

  /** Finer countdown (hours + minutes) for SHORT windows like the 5-hour limit. PURE. */
  def relResetFine(resetsAt: Long, nowMs: Long): String =
    val resetMs = if resetsAt < 1000000000000L then resetsAt * 1000L else resetsAt // <1e12 ⇒ seconds
    val deltaMs = resetMs - nowMs
    if deltaMs <= 0 then "now"
    else
      val totalMin = deltaMs / 60000L
      val h = totalMin / 60
      val m = totalMin % 60
      if h <= 0 then s"${m}m" else s"${h}h${m}m"

  /** PURE: statusLine JSON + current time → the one-line status ("" if nothing usable / not JSON). */
  def render(json: String, nowMs: Long): String =
    val o = try ujson.read(json).obj catch case _: Throwable => return ""
    val segs = scala.collection.mutable.ArrayBuffer[String]()
    segs += sgr("1;38;5;42", "genscalator:") // brand prefix (BR: prepend "genscalator:")
    segs += sgr("38;5;250", clock(nowMs)) // leading wall clock (light grey)
    o.get("model").flatMap(_.objOpt).foreach: m =>
      m.get("display_name").orElse(m.get("id")).flatMap(_.strOpt).foreach(n => segs += sgr("38;5;45", shortModel(n))) // un-bold so the bold genscalator: prefix is the only bold thing
    o.get("context_window").flatMap(_.objOpt).flatMap(_.get("used_percentage")).flatMap(_.numOpt)
      .foreach(p => segs += sgr(gauge(p, "38;5;114"), s"ctx-fill: ${pct(p)}")) // green base, gauge-graded
    val rl = o.get("rate_limits").flatMap(_.objOpt)
    rl.flatMap(_.get("five_hour")).flatMap(_.objOpt).foreach: h5 =>
      val used  = h5.get("used_percentage").flatMap(_.numOpt).map(p => sgr(gauge(p, "38;5;176"), s"5h-lim: ${pct(p)}"))
      val reset = h5.get("resets_at").flatMap(_.numOpt).map(r => sgr("38;5;245", s"resets: ${relResetFine(r.toLong, nowMs)}"))
      (used, reset) match
        case (Some(u), Some(r)) => segs += s"$u $r"
        case (Some(u), None)    => segs += u
        case (None, Some(r))    => segs += sgr("38;5;245", "5h-lim ") + r
        case _                  =>
    rl.flatMap(_.get("seven_day")).flatMap(_.objOpt).foreach: w =>
      val used  = w.get("used_percentage").flatMap(_.numOpt).map(p => sgr(gauge(p, "38;5;174"), s"wk-lim: ${pct(p)}")) // rosy-red base
      val reset = w.get("resets_at").flatMap(_.numOpt).map(r => sgr("38;5;245", s"resets: ${relReset(r.toLong, nowMs)}"))
      (used, reset) match
        case (Some(u), Some(r)) => segs += s"$u $r"
        case (Some(u), None)    => segs += u
        case (None, Some(r))    => segs += sgr("38;5;245", "wk-lim ") + r
        case _                  =>
    // cost LAST (least interesting on a fixed monthly plan) + blue, un-graded (no threshold meaning here)
    o.get("cost").flatMap(_.objOpt).flatMap(_.get("total_cost_usd")).flatMap(_.numOpt)
      .foreach(c => segs += sgr("38;5;39", f"cost: $$$c%.2f"))
    segs.mkString(sep)

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
