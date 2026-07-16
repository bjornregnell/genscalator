//> using scala 3.8.4
//> using jvm 21
//> using file minijson.scala

// statusline — format the Claude Code `statusLine` stdin JSON into ONE compact line (SM039).
// Claude Code pipes a JSON object to the configured statusLine command's stdin each turn; this reads it and prints:
//   genscalator  14:23:07  o4.8/1M  ctx-fill 41%  5h-lim 30%  wk-lim 14% reset 3d  cost $12
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
// reset countdown RED (the ambient slice of the SM022b usage-limit WARNING). Context-fill has a 3-step warn
// ladder: `--ctx-warn N` = the smart-zone ceiling Z (default 30; ctx-fill reds at/above, oranges at 0.8*Z);
// `--dumb-zone N` = most-likely-rotted (default 75; bold bright-red + a "dumb-zone" flag); `--auto-compact N`
// = the harness is about to auto-compact (default 92 - a GUESS; the real trigger is UNDOCUMENTED/opaque, the
// 90% warning is a separate alert, observed ~90-95%; bold reverse red +
// "auto-compact!"). Configure in the settings command string, e.g.
// "command": "tt statusline --warn 85 --ctx-warn 28 --dumb-zone 70 --auto-compact 90".
object StatuslineTool: // NB not "Statusline" — that collides case-only with the `statusLine` @main on case-insensitive FS
  def pct(v: Double): String = s"${v.round.toInt}%"

  // --- ANSI colour (SM039 polish). Each SEGMENT is wrapped as a WHOLE so its plain text stays a substring
  // (the tests match on `contains("ctx-fill 41%")` etc.), and a bad/empty stdin still yields "" with no codes.
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
  def ctxGauge(p: Double, ctxWarn: Double, dumbZone: Double, autoCompact: Double): String =
    if p >= autoCompact then "7;1;38;5;196"      // near auto-compact: bold + reverse + bright red (loudest)
    else if p >= dumbZone then "1;38;5;196"       // most likely dumb-zone: bold bright red
    else if p >= ctxWarn then Red                 // past Z: rot-risk red
    else if p >= 0.8 * ctxWarn then "38;5;214"    // compact-dance trigger: orange
    else "38;5;114"                               // healthy green

  /** Compact human-readable token count (SM117): 5008654 -> "5.0M", 178118 -> "178k", 950 -> "950". PURE. */
  def formatTokens(n: Long): String =
    if n >= 1_000_000 then f"${n / 1e6}%.1fM"
    else if n >= 1_000 then s"${n / 1000}k"
    else n.toString
  /** Colour for the cumulative-token rot gauge: green base, orange past `warn`, red past `danger`. The agent's
    * processing-volume rot signal (tokens, not #msg or wall-clock). Thresholds are GUESSES, configurable. PURE. */
  def tokGauge(t: Long, warn: Long, danger: Long): String =
    if t >= danger then Red else if t >= warn then "38;5;214" else "38;5;42"

  /** Cumulative session stats parsed from the Claude Code transcript JSONL (SM117). PURE + guarded: a line that
    * fails to parse or lacks the expected shape is skipped, never throws. Field paths verified against a live
    * transcript 2026-07-15 (see research note / probe):
    *   agentTokens = sum of assistant `message.usage.output_tokens`, EXCLUDING sub-agent sidechains
    *                 (`isSidechain:true`) so the gauge reflects THIS conversation's processing volume.
    *   humanChars  = sum of the LENGTH of user `message.content` when it is a plain STRING — a genuine typed
    *                 prompt; tool-result user records carry an array and are (correctly) excluded.
    *   sinceWarpTokens = like agentTokens but RESET at each {type:system, subtype:compact_boundary} marker
    *                 (SM128) — output tokens since the last warp (compact/clear) = the current-window rot? signal. */
  object TranscriptStats:
    def of(lines: IterableOnce[String]): (agentTokens: Long, humanChars: Long, sinceWarpTokens: Long) =
      var tok = 0L
      var chars = 0L
      var sinceWarp = 0L // SM128: agent output tokens since the last warp (compact_boundary) = the rot? signal
      lines.iterator.foreach: line =>
        try
          val o = MiniJson.parse(line).flatMap(_.obj).get // .get throws on malformed -> caught below -> line skipped
          o.get("type").flatMap(_.str) match
            case Some("assistant") if !o.get("isSidechain").flatMap(_.bool).getOrElse(false) =>
              o.get("message").flatMap(_.obj).flatMap(_.get("usage")).flatMap(_.obj)
                .flatMap(_.get("output_tokens")).flatMap(_.num).foreach: n =>
                  tok += n.toLong
                  sinceWarp += n.toLong
            case Some("user") =>
              o.get("message").flatMap(_.obj).flatMap(_.get("content")).flatMap(_.str)
                .foreach(s => chars += s.length)
            case Some("system") if o.get("subtype").flatMap(_.str).contains("compact_boundary") =>
              sinceWarp = 0L // a warp (compact) resets the current-window rot count (SM128)
            case _ =>
        catch case _: Throwable => () // skip unparseable / unexpected lines — never crash the prompt
      (tok, chars, sinceWarp)

  /** Local wall-clock HH:MM:SS from an epoch-ms (from --now-ms in tests, else System.currentTimeMillis). PURE.
    * Hand-rolled WITHOUT java.time — Scala Native 0.5.12 has not ported java.time (research/052), and this is the
    * one function that blocked statusline from compiling native. Uses java.util.TimeZone for the local, DST-aware
    * offset, then pure arithmetic; `floorMod` guards a pre-epoch nowMs. */
  def clock(nowMs: Long): String =
    val offsetMs = java.util.TimeZone.getDefault.getOffset(nowMs) // local offset incl. DST; no java.time
    val secOfDay = math.floorMod((nowMs + offsetMs) / 1000L, 86400L)
    f"${secOfDay / 3600}%02d:${(secOfDay % 3600) / 60}%02d:${secOfDay % 60}%02d"

  /** Compact model tag (SM117): "Opus 4.8 (1M context)" -> "o4.8/1M"; "Fable 5" -> "f5"; "Sonnet 5" -> "s5";
    * "Haiku 4.5" -> "h4.5". The family initial is LOWER-CASE so "o" does not read as a zero (BR). The context
    * window (e.g. "1M") becomes a "/1M" suffix; absent → just the letter+version. Falls back to the first char
    * for an unrecognised family (e.g. a bare model id). PURE. */
  def shortModel(name: String): String =
    val fam = name.toLowerCase
    val letter =
      if      fam.contains("opus")   then "o"
      else if fam.contains("sonnet") then "s"
      else if fam.contains("fable")  then "f"
      else if fam.contains("haiku")  then "h"
      else name.trim.headOption.map(_.toLower.toString).getOrElse("?")
    val ver = raw"(\d+(?:\.\d+)?)".r.findFirstIn(name).getOrElse("")
    val ctx = raw"\((\d+[MmKk])\b".r.findFirstMatchIn(name).map(_.group(1).toUpperCase)
    if ver.isEmpty then name.trim // no version to compact (e.g. a bare id like "haiku") — keep it recognisable
    else ctx.map(c => s"$letter$ver/$c").getOrElse(s"$letter$ver")

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
  def render(json: String, nowMs: Long, warn: Double = 80, ctxWarn: Double = 30, dumbZone: Double = 75, autoCompact: Double = 92,
             rotTokens: Option[Long] = None, totTokens: Option[Long] = None, showTot: Boolean = true, humanChars: Option[Long] = None,
             tokWarn: Long = 200_000L, tokDanger: Long = 500_000L, tiredChars: Option[Long] = None): String =
    val o = MiniJson.parse(json).flatMap(_.obj) match
      case Some(m) => m
      case None    => return "" // nothing usable / not an object → empty line, exit 0 (never crash the prompt)
    val segs = scala.collection.mutable.ArrayBuffer[String]()
    segs += sgr("1;38;5;42", "genscalator") // brand prefix (BR: prepend "genscalator")
    segs += sgr("38;5;250", clock(nowMs)) // leading wall clock (light grey)
    o.get("model").flatMap(_.obj).foreach: m =>
      m.get("display_name").orElse(m.get("id")).flatMap(_.str).foreach(n => segs += sgr("38;5;45", shortModel(n))) // un-bold so the bold genscalator prefix is the only bold thing
    o.get("context_window").flatMap(_.obj).flatMap(_.get("used_percentage")).flatMap(_.num)
      .foreach(p => segs += sgr(ctxGauge(p, ctxWarn, dumbZone, autoCompact),
        s"ctx-fill ${pct(p)}${if p >= autoCompact then " auto-compact!" else if p >= dumbZone then " dumb-zone" else ""}")) // escalates green->orange->red(Z)->dumb-zone(D)->auto-compact(A)
    // rot?/tot token gauges (SM128): rot? = tokens since the last warp (compact/clear) = the CURRENT-window rot
    // signal, COLOURED by threshold; the `?` marks it an inferred proxy (SM118). tot = cumulative lifetime tokens,
    // dim (context only, no threshold meaning), and DROPPED on a narrow terminal (showTot). rot? is the star.
    rotTokens.foreach(r => segs += sgr(tokGauge(r, tokWarn, tokDanger), s"rot? ${formatTokens(r)}"))
    if showTot then totTokens.foreach(t => segs += sgr("38;5;245", s"tot ${formatTokens(t)}"))
    val rl = o.get("rate_limits").flatMap(_.obj)
    rl.flatMap(_.get("five_hour")).flatMap(_.obj).foreach: h5 =>
      val usedP  = h5.get("used_percentage").flatMap(_.num)
      val warned = usedP.exists(_ >= warn) // at/above the warn threshold: colour BOTH the % and its reset red
      val used   = usedP.map(p => sgr(limGauge(p, warn, "38;5;176"), s"5h-lim ${pct(p)}"))
      val reset  = h5.get("resets_at").flatMap(_.num).map(r => sgr(if warned then Red else "38;5;245", s"reset ${relResetFine(r.toLong, nowMs)}"))
      (used, reset) match
        case (Some(u), Some(r)) => segs += s"$u $r"
        case (Some(u), None)    => segs += u
        case (None, Some(r))    => segs += sgr("38;5;245", "5h-lim ") + r
        case _                  =>
    rl.flatMap(_.get("seven_day")).flatMap(_.obj).foreach: w =>
      val usedP  = w.get("used_percentage").flatMap(_.num)
      val warned = usedP.exists(_ >= warn) // at/above the warn threshold: colour BOTH the % and its reset red
      val used   = usedP.map(p => sgr(limGauge(p, warn, "38;5;174"), s"wk-lim ${pct(p)}")) // rosy-red base
      val reset  = w.get("resets_at").flatMap(_.num).map(r => sgr(if warned then Red else "38;5;245", s"reset ${relReset(r.toLong, nowMs)}"))
      (used, reset) match
        case (Some(u), Some(r)) => segs += s"$u $r"
        case (Some(u), None)    => segs += u
        case (None, Some(r))    => segs += sgr("38;5;245", "wk-lim ") + r
        case _                  =>
    // cost LAST (least interesting on a fixed monthly plan) + blue, un-graded (no threshold meaning here)
    o.get("cost").flatMap(_.obj).flatMap(_.get("total_cost_usd")).flatMap(_.num)
      .foreach(c => segs += sgr("38;5;39", s"cost $$${c.toLong}")) // whole dollars, TRUNCATED (cents are noise on a fixed monthly plan; saves horiz space; never overstates)
    // human-fatigue NUDGE (SM117): the human's char-count is an INTERNAL gauge (showing the raw number can itself
    // stress — BR); only a gentle `tired?` surfaces, and ONLY when a threshold is explicitly set (opt-in, default
    // off). Calm lavender, NEVER red — a nudge, not an alarm. The `?` marks it INFERRED (the agent cannot know the
    // human is tired). NB this display does NOT engage any mode — auto-mode-engagement is JOINT co-design (SM116/SM118).
    (humanChars, tiredChars) match
      case (Some(c), Some(thr)) if c >= thr => segs += sgr("38;5;147", "tired?")
      case _                                =>
    segs.mkString(sep)

  // ---------- mode line (v0.10.0): a second line labelling the joint state-of-mind ----------
  // A "mode" is a label stuck on the shared human<->agent state; MANY can be active at once. Auto-derived
  // modes come from the same status JSON this tool already reads; declared modes come from a state file (read
  // by the @main). Each label renders REVERSE-video + bold in its own colour, joined by a plain " & "
  // (non-inverted, non-bold), prefixed "genscalator". Toggled INDEPENDENTLY of the status line above.

  /** Curated colours for well-known modes; an unknown label gets a stable colour by hash. */
  val knownModeColors: Map[String, String] = Map(
    "tok-spend" -> "38;5;214", "token-saving" -> "38;5;42", "high-context" -> "38;5;208",
    "dumb-zone" -> "38;5;203", "hot-harvest" -> "38;5;215", "solo" -> "38;5;75",
    "human-stress" -> "38;5;203", "rot-vigil" -> "38;5;220", "racing" -> "38;5;170",
    "limit-near" -> "38;5;203", "delegation" -> "38;5;111"
  )
  def modeColor(label: String): String =
    knownModeColors.getOrElse(label, {
      val palette = Vector("38;5;170", "38;5;114", "38;5;180", "38;5;75", "38;5;215", "38;5;150", "38;5;210", "38;5;111")
      palette(math.floorMod(label.hashCode, palette.size))
    })
  /** One mode label: reverse-video (7) + bold (1) + its colour, padded to read as a chip. PURE. */
  def renderMode(label: String): String = sgr(s"7;1;${modeColor(label)}", s" $label ")
  /** SM119: a STABLE render order so a given SET of active modes always renders the same regardless of the
    * +/- add/remove history (the state file records insertion order, which reshuffles the line on every toggle).
    * First-cut canonical priority, grouped by frame; tune freely — it is only a DISPLAY order, no behaviour
    * depends on it. Any label not listed sorts alphabetically AFTER the known ones, so unknown modes stay stable
    * too. A `?`-suffixed inferred mode (SM118) sorts with its confirmed base, just after it. */
  val modeOrder: Vector[String] = Vector(
    "afk", "solo", "delegation", "racing",   // session frame (who/how we are working)
    "human-stress", "tired",                 // human state
    "rot-vigil", "dumb-zone",                // agent vigilance
    "high-context", "limit-near",            // context / limits
    "tok-spend", "token-saving",             // token budget
    "hot-harvest"                            // task
  )
  /** Order `modes` by `modeOrder` (known first, in that order), then alphabetically for the rest. PURE. */
  def sortModes(modes: Seq[String]): Seq[String] =
    val rank = modeOrder.zipWithIndex.toMap
    modes.sortBy: label =>
      val base = label.stripSuffix("?")                 // ?-inferred sorts with its confirmed base (SM118)
      (rank.getOrElse(base, Int.MaxValue), base, label) // known-by-rank, else alphabetical; plain before ?
  /** The mode line: brand prefix + active modes (each reverse+bold, own colour) joined by a plain " & ". PURE.
    * No active modes -> a dim placeholder, so the line is still recognisable as the (empty) mode line. */
  def renderModes(modes: Seq[String]): String =
    val brand = sgr("1;38;5;42", "gs mode set") // line-2 prefix: NOT "genscalator" again (redundant with line 1); doubles as the DWIM verb
    if modes.isEmpty then s"$brand ${sgr("38;5;245", "clear: no active mode labels")}"
    else s"$brand ${sortModes(modes).map(renderMode).mkString(" & ")}"
  private val Help: String =
    """tt statusline — format Claude Code's statusLine JSON into one compact coloured line
      |
      |Claude Code pipes a JSON object to the configured statusLine command's stdin each
      |turn; this tool reads it and prints one ANSI-coloured line. Every segment is
      |independently guarded: a field absent from the JSON simply omits its segment, and a
      |bad/empty stdin prints an empty line (exit 0) — it never crashes the prompt.
      |
      |Segments (left to right):
      |  genscalator         brand prefix
      |  HH:MM:SS            local wall clock
      |  o4.8/1M             abbreviated model (Opus/Sonnet/Fable/Haiku -> o/s/f/h, /ctx suffix)
      |  ctx-fill N%         context-window fill; orange at the compact-dance trigger
      |                      (0.8*Z), red at Z = the dumb-zone threshold (--ctx-warn)
      |  rot? N / tot N      rot? = agent tokens SINCE the last warp (compact/clear) = current-window rot,
      |                      coloured by threshold (the `?` marks it an inferred proxy); tot = cumulative
      |                      lifetime tokens (dim; dropped if the terminal is narrow or --rot-only). Both from
      |                      the transcript; --no-tok skips the read entirely.
      |  5h-lim / wk-lim     usage limits (Claude Pro/Max only) + reset countdown; both
      |                      the % and its countdown turn RED at/above --warn
      |  cost $N             total cost in whole dollars, last (least interesting on a
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
    var ctxWarn = 30.0 // context-fill RISK threshold (%, the smart-zone ceiling Z); set via `--ctx-warn N`
    var dumbZone    = 75.0 // context-fill DUMB-ZONE threshold (%): most likely rotted; set via `--dumb-zone N`
    var autoCompact = 92.0 // context-fill NEAR-AUTO-COMPACT threshold (%): harness about to compact; `--auto-compact N` (real value ~90-95%, a guess)
    var modeLine  = false // --mode-line: also emit the mode line (line 2)
    var noStatus  = false // --no-status: suppress line 1 (e.g. to show ONLY the mode line)
    var modesFile = defaultModesFile
    var tokWarn    = 200_000L // rot? (since-warp) orange threshold (GUESS for one window, configurable via --tok-warn)
    var tokDanger  = 500_000L // rot? (since-warp) red threshold (GUESS for one window, configurable via --tok-danger)
    var tiredChars: Option[Long] = None // human-char `tired?` nudge threshold; None = OFF (opt-in via --tired-chars)
    var noTok      = false // --no-tok: skip the transcript read entirely (no tok gauge)
    var rotOnly    = false // --rot-only: show rot? but DROP the secondary tot gauge (also auto-dropped if narrow)
    val pos = scala.collection.mutable.ArrayBuffer[String]()
    val a = args.toVector
    var i = 0
    while i < a.length do
      a(i) match
        case "--now-ms"     if i + 1 < a.length => nowMs     = a(i + 1).toLongOption.getOrElse(nowMs); i += 2
        case "--warn"       if i + 1 < a.length => warn      = a(i + 1).toDoubleOption.getOrElse(warn); i += 2
        case "--ctx-warn"     if i + 1 < a.length => ctxWarn     = a(i + 1).toDoubleOption.getOrElse(ctxWarn); i += 2
        case "--dumb-zone"    if i + 1 < a.length => dumbZone    = a(i + 1).toDoubleOption.getOrElse(dumbZone); i += 2
        case "--auto-compact" if i + 1 < a.length => autoCompact = a(i + 1).toDoubleOption.getOrElse(autoCompact); i += 2
        case "--modes-file" if i + 1 < a.length => modesFile = java.nio.file.Path.of(a(i + 1)); i += 2
        case "--tok-warn"    if i + 1 < a.length => tokWarn    = a(i + 1).toLongOption.getOrElse(tokWarn); i += 2
        case "--tok-danger"  if i + 1 < a.length => tokDanger  = a(i + 1).toLongOption.getOrElse(tokDanger); i += 2
        case "--tired-chars" if i + 1 < a.length => tiredChars = a(i + 1).toLongOption; i += 2
        case "--no-tok"                         => noTok     = true; i += 1
        case "--rot-only"                       => rotOnly   = true; i += 1
        case "--mode-line"                      => modeLine  = true; i += 1
        case "--no-status"                      => noStatus  = true; i += 1
        case other                              => pos += other; i += 1
    // SM128: drop the secondary `tot` gauge on a narrow terminal. Claude Code sets $COLUMNS before running the
    // statusline (there is NO width field in the JSON — claude-code-guide 2026-07-16); --rot-only forces tot off.
    val cols = Option(System.getenv("COLUMNS")).flatMap(_.toIntOption)
    val showTot = !rotOnly && cols.forall(_ >= 90)
    val json = pos.headOption.getOrElse(scala.io.Source.stdin.mkString)
    // Transcript-derived cumulative stats (SM117): the statusline JSON carries `transcript_path`; parse the JSONL
    // for the agent-token rot gauge (+ the internal human-char fatigue gauge feeding `tired?`). Fully guarded — any
    // failure yields no gauge, never a broken prompt. Reads the transcript on EACH render; --no-tok opts out.
    // PERF: on a large transcript this file read/parse is the tool's dominant cost — a cache/incremental read and
    // the SM112 native build are the follow-ups; --no-tok is the escape hatch meanwhile.
    val stats: Option[(agentTokens: Long, humanChars: Long, sinceWarpTokens: Long)] =
      if noTok then None
      else
        try
          MiniJson.parse(json).flatMap(_.obj).flatMap(_.get("transcript_path")).flatMap(_.str)
            .map(java.nio.file.Path.of(_))
            .filter(java.nio.file.Files.isRegularFile(_))
            .map: p =>
              val src = scala.io.Source.fromFile(p.toFile, "UTF-8")
              try TranscriptStats.of(src.getLines()) finally src.close()
        catch case _: Throwable => None
    // Each println is a SEPARATE status row (Claude Code renders multi-line statuslines).
    if !noStatus then println(render(json, nowMs, warn, ctxWarn, dumbZone, autoCompact,
      rotTokens = stats.map(_.sinceWarpTokens), totTokens = stats.map(_.agentTokens), showTot = showTot,
      humanChars = stats.map(_.humanChars), tokWarn = tokWarn, tokDanger = tokDanger, tiredChars = tiredChars))
    if modeLine then println(renderModes(readModes(modesFile)))
    0

@main def statusLine(args: String*): Unit = sys.exit(StatuslineTool.dispatch(args.toList))
