//> using scala 3.8.4
//> using jvm 21
//> using dep com.lihaoyi::ujson:4.4.3

// statusline — format the Claude Code `statusLine` stdin JSON into ONE compact line (SM039).
// Claude Code pipes a JSON object to the configured statusLine command's stdin each turn; this reads it and prints:
//   genscalator:  14:23:07  O4.8 (1M ctx)  ctx-fill: 41%  5h-lim: 30%  wk-lim: 14% resets: 3d  cost: $12
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
// `--warn N` sets the usage-limit warn threshold (default 80): a 5h/wk limit at/above it turns its % AND its
// reset countdown RED (the ambient slice of the SM022b usage-limit WARNING). `--ctx-warn N` sets the context-
// fill dumb-zone threshold (default 30 = the smart-zone ceiling Z): ctx-fill reds at/above it and oranges at
// the compact-dance trigger 0.8*Z. Configure in the settings command string, e.g.
// "command": "tt statusline --warn 85 --ctx-warn 28".
object StatuslineTool: // NB not "Statusline" — that collides case-only with the `statusLine` @main on case-insensitive FS
  def pct(v: Double): String = s"${v.round.toInt}%"

  // --- ANSI colour (SM039 polish). Each SEGMENT is wrapped as a WHOLE so its plain text stays a substring
  // (the tests match on `contains("ctx-fill: 41%")` etc.), and a bad/empty stdin still yields "" with no codes.
  // 256-colour; on a terminal without 256-colour support the sequences degrade to plain text.
  val ESC: Char = 27.toChar // the ANSI escape byte (0x1B); explicit to avoid any \u-escape ambiguity
  def sgr(code: String, s: String): String = s"${ESC}[${code}m${s}${ESC}[0m"
  val sep: String = "  " // two spaces between parts (the middot was dropped to save horizontal space)
  val Red: String = "38;5;203" // shared "danger" red (the gauge cap AND the usage-limit warn colour)
  /** Gauge colour by level: a distinct healthy hue per part, escalating to orange >= 70% and red >= 90%. */
  def gauge(p: Double, healthy: String): String =
    if p >= 90 then Red else if p >= 70 then "38;5;214" else healthy
  /** For a usage LIMIT: RED at/above the configurable warn threshold (default 80%), else the normal gauge.
   *  This is the ambient slice of the usage-limit WARNING (SM022b): a limit past the threshold turns red so
   *  an approaching cap is seen before it blocks — and its reset countdown reddens with it (see render). */
  def limGauge(p: Double, warn: Double, healthy: String): String =
    if p >= warn then Red else gauge(p, healthy)
  /** For CONTEXT FILL: reds at ctxWarn — the smart-zone ceiling Z, i.e. the point of risking the "dumb zone"
   *  (context rot) — and oranges at the compact-dance trigger 0.8*Z. Tied to the compact-dance math, NOT the
   *  generic 90% cap: a context window well below any hard limit is already rot-risky, so it must warn early. */
  def ctxGauge(p: Double, ctxWarn: Double): String =
    if p >= ctxWarn then Red else if p >= 0.8 * ctxWarn then "38;5;214" else "38;5;114"

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
  def render(json: String, nowMs: Long, warn: Double = 80, ctxWarn: Double = 30): String =
    val o = try ujson.read(json).obj catch case _: Throwable => return ""
    val segs = scala.collection.mutable.ArrayBuffer[String]()
    segs += sgr("1;38;5;42", "genscalator:") // brand prefix (BR: prepend "genscalator:")
    segs += sgr("38;5;250", clock(nowMs)) // leading wall clock (light grey)
    o.get("model").flatMap(_.objOpt).foreach: m =>
      m.get("display_name").orElse(m.get("id")).flatMap(_.strOpt).foreach(n => segs += sgr("38;5;45", shortModel(n))) // un-bold so the bold genscalator: prefix is the only bold thing
    o.get("context_window").flatMap(_.objOpt).flatMap(_.get("used_percentage")).flatMap(_.numOpt)
      .foreach(p => segs += sgr(ctxGauge(p, ctxWarn), s"ctx-fill: ${pct(p)}")) // green base; reds at the dumb-zone threshold (Z)
    val rl = o.get("rate_limits").flatMap(_.objOpt)
    rl.flatMap(_.get("five_hour")).flatMap(_.objOpt).foreach: h5 =>
      val usedP  = h5.get("used_percentage").flatMap(_.numOpt)
      val warned = usedP.exists(_ >= warn) // at/above the warn threshold: colour BOTH the % and its reset red
      val used   = usedP.map(p => sgr(limGauge(p, warn, "38;5;176"), s"5h-lim: ${pct(p)}"))
      val reset  = h5.get("resets_at").flatMap(_.numOpt).map(r => sgr(if warned then Red else "38;5;245", s"resets: ${relResetFine(r.toLong, nowMs)}"))
      (used, reset) match
        case (Some(u), Some(r)) => segs += s"$u $r"
        case (Some(u), None)    => segs += u
        case (None, Some(r))    => segs += sgr("38;5;245", "5h-lim ") + r
        case _                  =>
    rl.flatMap(_.get("seven_day")).flatMap(_.objOpt).foreach: w =>
      val usedP  = w.get("used_percentage").flatMap(_.numOpt)
      val warned = usedP.exists(_ >= warn) // at/above the warn threshold: colour BOTH the % and its reset red
      val used   = usedP.map(p => sgr(limGauge(p, warn, "38;5;174"), s"wk-lim: ${pct(p)}")) // rosy-red base
      val reset  = w.get("resets_at").flatMap(_.numOpt).map(r => sgr(if warned then Red else "38;5;245", s"resets: ${relReset(r.toLong, nowMs)}"))
      (used, reset) match
        case (Some(u), Some(r)) => segs += s"$u $r"
        case (Some(u), None)    => segs += u
        case (None, Some(r))    => segs += sgr("38;5;245", "wk-lim ") + r
        case _                  =>
    // cost LAST (least interesting on a fixed monthly plan) + blue, un-graded (no threshold meaning here)
    o.get("cost").flatMap(_.objOpt).flatMap(_.get("total_cost_usd")).flatMap(_.numOpt)
      .foreach(c => segs += sgr("38;5;39", s"cost: $$${c.toLong}")) // whole dollars, TRUNCATED (cents are noise on a fixed monthly plan; saves horiz space; never overstates)
    segs.mkString(sep)

  // ---------- mode line (v0.10.0): a second line labelling the joint state-of-mind ----------
  // A "mode" is a label stuck on the shared human<->agent state; MANY can be active at once. Auto-derived
  // modes come from the same status JSON this tool already reads; declared modes come from a state file (read
  // by the @main). Each label renders REVERSE-video + bold in its own colour, joined by a plain " && "
  // (non-inverted, non-bold), prefixed "genscalator:". Toggled INDEPENDENTLY of the status line above.

  /** Curated colours for well-known modes; an unknown label gets a stable colour by hash. */
  val knownModeColors: Map[String, String] = Map(
    "token-spending" -> "38;5;214", "token-saving" -> "38;5;42", "high-context" -> "38;5;208",
    "dumb-zone" -> "38;5;203", "hot-harvest" -> "38;5;215", "solo" -> "38;5;75",
    "human-stress" -> "38;5;203", "rot-vigilance" -> "38;5;220", "racing" -> "38;5;170",
    "limit-near" -> "38;5;203", "delegation" -> "38;5;111"
  )
  def modeColor(label: String): String =
    knownModeColors.getOrElse(label, {
      val palette = Vector("38;5;170", "38;5;114", "38;5;180", "38;5;75", "38;5;215", "38;5;150", "38;5;210", "38;5;111")
      palette(math.floorMod(label.hashCode, palette.size))
    })
  /** One mode label: reverse-video (7) + bold (1) + its colour, padded to read as a chip. PURE. */
  def renderMode(label: String): String = sgr(s"7;1;${modeColor(label)}", s" $label ")
  /** The mode line: brand prefix + active modes (each reverse+bold, own colour) joined by a plain " && ". PURE.
    * No active modes -> a dim placeholder, so the line is still recognisable as the (empty) mode line. */
  def renderModes(modes: Seq[String]): String =
    val brand = sgr("1;38;5;42", "genscalator:")
    if modes.isEmpty then s"$brand ${sgr("38;5;245", "(no active modes)")}"
    else s"$brand ${modes.map(renderMode).mkString(" && ")}"
  private val Help: String =
    """tt statusline — format Claude Code's statusLine JSON into one compact coloured line
      |
      |Claude Code pipes a JSON object to the configured statusLine command's stdin each
      |turn; this tool reads it and prints one ANSI-coloured line. Every segment is
      |independently guarded: a field absent from the JSON simply omits its segment, and a
      |bad/empty stdin prints an empty line (exit 0) — it never crashes the prompt.
      |
      |Segments (left to right):
      |  genscalator:        brand prefix
      |  HH:MM:SS            local wall clock
      |  O4.8 (1M ctx)       abbreviated model name (Opus/Sonnet/Fable/Haiku -> O/S/F/H)
      |  ctx-fill: N%        context-window fill; orange at the compact-dance trigger
      |                      (0.8*Z), red at Z = the dumb-zone threshold (--ctx-warn)
      |  5h-lim / wk-lim     usage limits (Claude Pro/Max only) + reset countdown; both
      |                      the % and its countdown turn RED at/above --warn
      |  cost: $N            total cost in whole dollars, last (least interesting on a
      |                      fixed monthly plan)
      |
      |Usage:
      |  statusline [flags]             read the JSON from stdin (how Claude Code calls it)
      |  statusline '<json>' [flags]    JSON as a positional arg (for deterministic tests)
      |Flags:
      |  --warn N            usage-limit warn threshold in % (default 80)
      |  --ctx-warn N        context-fill dumb-zone threshold Z in % (default 30)
      |  --now-ms N          fixed "now" as epoch-ms (for deterministic tests)
      |  --mode-line         ALSO emit the mode line (line 2: the declared joint state-of-mind,
      |                      read from the state file `tt mode` writes)
      |  --no-status         suppress line 1 (e.g. show ONLY the mode line)
      |  --modes-file F      the declared-modes state file (default ~/.claude/gs-modes)
      |
      |Wire it up (human-gated settings step) in .claude/settings.json:
      |  "statusLine": { "type": "command", "command": "tt statusline --warn 85 --ctx-warn 28" }
      |
      |Full reference: docs/statusline-manual.md""".stripMargin

  /** Read the declared-modes state file (one label per line); empty if absent/unreadable. Kept in sync with
    * what `tt mode` writes — it is the recorded joint state-of-mind the mode line renders. */
  def readModes(file: java.nio.file.Path): Seq[String] =
    try
      if java.nio.file.Files.isRegularFile(file) then
        String(java.nio.file.Files.readAllBytes(file), "UTF-8").linesIterator.map(_.trim).filter(_.nonEmpty).toVector.distinct
      else Seq.empty
    catch case _: Throwable => Seq.empty
  def defaultModesFile: java.nio.file.Path =
    java.nio.file.Path.of(sys.props.getOrElse("user.home", "."), ".claude", "gs-modes")

  def dispatch(args: List[String]): Int =
    if args.contains("--help") || args.contains("-h") then { println(Help); return 0 }
    var nowMs   = System.currentTimeMillis()
    var warn    = 80.0 // usage-limit warn threshold (%); set via `--warn N` in the settings command string
    var ctxWarn = 30.0 // context-fill dumb-zone threshold (%, the smart-zone ceiling Z); set via `--ctx-warn N`
    var modeLine  = false // --mode-line: also emit the mode line (line 2)
    var noStatus  = false // --no-status: suppress line 1 (e.g. to show ONLY the mode line)
    var modesFile = defaultModesFile
    val pos = scala.collection.mutable.ArrayBuffer[String]()
    val a = args.toVector
    var i = 0
    while i < a.length do
      a(i) match
        case "--now-ms"     if i + 1 < a.length => nowMs     = a(i + 1).toLongOption.getOrElse(nowMs); i += 2
        case "--warn"       if i + 1 < a.length => warn      = a(i + 1).toDoubleOption.getOrElse(warn); i += 2
        case "--ctx-warn"   if i + 1 < a.length => ctxWarn   = a(i + 1).toDoubleOption.getOrElse(ctxWarn); i += 2
        case "--modes-file" if i + 1 < a.length => modesFile = java.nio.file.Path.of(a(i + 1)); i += 2
        case "--mode-line"                      => modeLine  = true; i += 1
        case "--no-status"                      => noStatus  = true; i += 1
        case other                              => pos += other; i += 1
    val json = pos.headOption.getOrElse(scala.io.Source.stdin.mkString)
    // Each println is a SEPARATE status row (Claude Code renders multi-line statuslines).
    if !noStatus then println(render(json, nowMs, warn, ctxWarn))
    if modeLine then println(renderModes(readModes(modesFile)))
    0

@main def statusLine(args: String*): Unit = sys.exit(StatuslineTool.dispatch(args.toList))
