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

  /** ISO-8601 UTC ("2026-07-16T11:07:58.421Z") -> epoch-ms. None if unparseable. PURE.
    *
    * HAND-ROLLED WITHOUT java.time, ON PURPOSE — same reason as `clock` below: Scala Native 0.5.12 has not ported
    * java.time (research/052), and statusline is the hot native target. `HangoverTool.parseInstantMs` does the same
    * job with `java.time.Instant.parse`, which is fine there (a JVM-only hook) but would re-break native HERE.
    * DELIBERATE DUPLICATION (scala-style §5): the two copies differ by the constraint that motivates them; the DRY
    * move is a shared transcript reader, already noted as debt in hangover.scala.
    * Days-from-civil is the standard Hinnant algorithm (era arithmetic; correct across leap years/centuries). */
  def isoToEpochMs(iso: String): Option[Long] =
    def daysFromCivil(y: Int, m: Int, d: Int): Long =
      val yy  = if m <= 2 then y - 1 else y
      val era = (if yy >= 0 then yy else yy - 399) / 400
      val yoe = yy - era * 400                                     // [0, 399]
      val doy = (153 * (if m > 2 then m - 3 else m + 9) + 2) / 5 + d - 1
      val doe = yoe.toLong * 365 + yoe / 4 - yoe / 100 + doy       // [0, 146096]
      era.toLong * 146097L + doe - 719468L                         // shift epoch from 0000-03-01 to 1970-01-01
    scala.util.Try {
      val y  = iso.substring(0, 4).toInt
      val mo = iso.substring(5, 7).toInt
      val d  = iso.substring(8, 10).toInt
      val h  = iso.substring(11, 13).toInt
      val mi = iso.substring(14, 16).toInt
      val s  = iso.substring(17, 19).toInt
      val ms = if iso.length >= 23 && iso.charAt(19) == '.' then iso.substring(20, 23).toInt else 0
      daysFromCivil(y, mo, d) * 86_400_000L + h * 3_600_000L + mi * 60_000L + s * 1000L + ms
    }.toOption

  /** Seconds -> a compact gap for the chip: "11h", "5m", "42s". PURE. */
  def formatGapShort(sec: Long): String =
    if sec >= 3600 then s"${sec / 3600}h" else if sec >= 60 then s"${sec / 60}m" else s"${sec}s"

  // RETIRED 2026-07-17: `hangoverGauge` + `hangoverChip` are GONE, with their thresholds and their blink gate.
  // Not a refactor — the construct was wrong. The chip fused a MEASUREMENT (the transcript gap) with an INFERENCE
  // (the agent is cold), so it rendered the HUMAN's 1-minute thinking pause as the AGENT's state, twice in 90
  // minutes (wr-data: nobody-dropped-the-hangover-chip / hangover-chip-fires-on-the-humans-thinking-pause).
  // BR's fix SPLITS them, and each half loses its `?`:
  //   * the MEASUREMENT -> `silent` on LINE 1 (see render): counted, no threshold, no colour, subject = the FEED.
  //   * the INFERENCE   -> `hangover` on LINE 2: a DECLARED mode, owned by whoever declares it.
  // The graded-blink restraint the gauge encoded (an alarm is a budget, SM129) is not lost — it is MOOT: a readout
  // never alarms, so it can never cry wolf. Thresholds existed only to guess a hangover; nothing guesses now.
  // The whole 10s-vs-60s noise-floor calibration question dies with them. History: `git log tools/statusline.scala`.

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
    def of(lines: IterableOnce[String]): (agentTokens: Long, humanChars: Long, sinceWarpTokens: Long, lastStampMs: Long) =
      var tok = 0L
      var chars = 0L
      var sinceWarp = 0L // SM128: agent output tokens since the last warp (compact_boundary) = the rot? signal
      var lastStamp = 0L // SM121: epoch-ms of the LAST timestamped record -> now - this = the hangover gap
      lines.iterator.foreach: line =>
        try
          val o = MiniJson.parse(line).flatMap(_.obj).get // .get throws on malformed -> caught below -> line skipped
          // SM121: any record carrying a timestamp advances the "last activity" mark (same single scan — no second
          // pass, no second file read; the parse is already paid for here).
          o.get("timestamp").flatMap(_.str).flatMap(isoToEpochMs).foreach(ms => if ms > lastStamp then lastStamp = ms)
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
      (tok, chars, sinceWarp, lastStamp)

  /** Local wall-clock HH:MM:SS from an epoch-ms (from --now-ms in tests, else System.currentTimeMillis). PURE.
    * Hand-rolled WITHOUT java.time — Scala Native 0.5.12 has not ported java.time (research/052), and this is the
    * one function that blocked statusline from compiling native. Uses java.util.TimeZone for the local, DST-aware
    * offset, then pure arithmetic; `floorMod` guards a pre-epoch nowMs. */
  def clock(nowMs: Long): String =
    val offsetMs = java.util.TimeZone.getDefault.getOffset(nowMs) // local offset incl. DST; no java.time
    val secOfDay = math.floorMod((nowMs + offsetMs) / 1000L, 86400L)
    f"${secOfDay / 3600}%02d:${(secOfDay % 3600) / 60}%02d:${secOfDay % 60}%02d"

  /** Compact ctx-window SIZE for the model tag's "/1M" suffix: 1000000 -> "1M", 200000 -> "200k". Round
    * millions stay whole ("1M", not "1.0M" — it is a nameplate capacity, not a measurement of flow). PURE. */
  def formatCtxSize(n: Long): String =
    if n >= 1_000_000 then
      if n % 1_000_000 == 0 then s"${n / 1_000_000}M" else f"${n / 1e6}%.1fM"
    else s"${n / 1000}k"

  /** Compact model tag (SM117): "Opus 4.8 (1M context)" -> "o4.8/1M"; "Fable 5" -> "f5"; "Sonnet 5" -> "s5";
    * "Haiku 4.5" -> "h4.5". The family initial is LOWER-CASE so "o" does not read as a zero (BR). The context
    * window becomes a "/1M" suffix, PREFERRING the MEASURED `context_window.context_window_size` from the
    * status JSON (`ctxSize`) over a "(1M ...)" parsed from the display name. Why (2026-07-19): Opus announced
    * itself as "Opus 4.8 (1M context)" so the name-parse worked, but Fable's display_name is bare "Fable 5"
    * and the suffix silently vanished at the model-warp — a nameplate fact should come from the measured field
    * when the harness provides one (line-1 provenance), with the name-parse kept as the fallback. Falls back
    * to the first char for an unrecognised family (e.g. a bare model id). PURE. */
  def shortModel(name: String, ctxSize: Option[Long] = None): String =
    val fam = name.toLowerCase
    val letter =
      if      fam.contains("opus")   then "o"
      else if fam.contains("sonnet") then "s"
      else if fam.contains("fable")  then "f"
      else if fam.contains("haiku")  then "h"
      else name.trim.headOption.map(_.toLower.toString).getOrElse("?")
    val ver = raw"(\d+(?:\.\d+)?)".r.findFirstIn(name).getOrElse("")
    val ctx = ctxSize.map(formatCtxSize)
      .orElse(raw"\((\d+[MmKk])\b".r.findFirstMatchIn(name).map(_.group(1).toUpperCase))
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
             tokWarn: Long = 200_000L, tokDanger: Long = 500_000L, tiredChars: Option[Long] = None,
             silentSec: Option[Long] = None): String =
    val o = MiniJson.parse(json).flatMap(_.obj) match
      case Some(m) => m
      case None    => return "" // nothing usable / not an object → empty line, exit 0 (never crash the prompt)
    val segs = scala.collection.mutable.ArrayBuffer[String]()
    segs += sgr("1;38;5;42", "genscalator") // brand prefix (BR: prepend "genscalator")
    segs += sgr("38;5;250", clock(nowMs)) // leading wall clock (light grey)
    // `silent` — feed inactivity, riding just after the clock (BR, 2026-07-17). LINE-1 CONTRACT: this row is for what
    // a MECHANISM MEASURES; line 2 is for what someone DECLARES. So the SURFACE encodes the provenance and no
    // provenance field is needed — the line IS the field.
    //
    // NO `?`, deliberately: the `?` marks an INFERRED PROXY (see rot? below). This is COUNTED — `now - the last
    // timestamped transcript record` — both terms exactly known, no inference. `tot` is counted and carries no `?`;
    // so does this. (BR asked whether it could be measured reliably. It can; the `?` was dropped on that basis.)
    //
    // NO thresholds, NO grading, NO alarm — dim, like `tot`. It is a READOUT, not a gauge, and that restraint is
    // what makes it honest: it replaces `hangover?`, which fused a MEASUREMENT (the gap) with an INFERENCE (the
    // agent is cold) and so misattributed the HUMAN's thinking pause as the AGENT's state (wr-data 2026-07-17).
    // The inference now lives on line 2 as a DECLARED `hangover` mode, owned by whoever declares it.
    //
    // Subject: `silent` describes the FEED, and that is the whole point of the NAME (BR renamed it from `idle`
    // 2026-07-17, and the rename is not cosmetic). `idle` attributed a STATE to an unnamed SUBJECT — and got it
    // wrong exactly when it mattered: while BR was away eating, the agent was NOT idle, it was making tool calls;
    // and when the agent waits, BR is often NOT idle, he is thinking. `silent` names the MEASUREMENT instead: the
    // feed has no new records. No subject, so nothing to misattribute. ⭐ The old name needed a comment defending
    // what it did not mean ("nobody is idle; the feed is") — a name that needs a paragraph of defence is the wrong
    // name. This one needs none: the feed IS silent. Same lesson as `human-stress` (the only mode that names its
    // subject and the only one that never went wrong), pushed one step further: do not name the subject correctly,
    // REMOVE it — the feed is the subject. (wr-data: modes-dont-say-whose-state-2026-07-17.)
    //
    // HONEST LIMIT, recorded at the code: a running command writes NO transcript record, so agent-busy time counts
    // as silence (the measured 18s specimen). The feed IS silent, the pair is not. Tolerable ONLY because there is
    // no threshold and no colour: a readout may say "nothing has landed for 18s"; an alarm may not. ⚠️ Note the
    // limit SURVIVES the rename and is not fixed by it — `silent` is honest about the feed, and the feed is still
    // an imperfect proxy for the pair. It just no longer LIES about whose state it is.
    silentSec.foreach(s => segs += sgr("38;5;245", s"silent ${formatGapShort(s)}"))
    // measured ctx-window SIZE (docs: context_window.context_window_size, 200000 default / 1000000 extended) —
    // feeds the model tag's "/1M" suffix so it survives a display_name that omits it (see shortModel).
    val ctxSize = o.get("context_window").flatMap(_.obj).flatMap(_.get("context_window_size")).flatMap(_.num).map(_.toLong)
    o.get("model").flatMap(_.obj).foreach: m =>
      m.get("display_name").orElse(m.get("id")).flatMap(_.str).foreach(n => segs += sgr("38;5;45", shortModel(n, ctxSize))) // un-bold so the bold genscalator prefix is the only bold thing
    o.get("context_window").flatMap(_.obj).flatMap(_.get("used_percentage")).flatMap(_.num)
      .foreach(p => segs += sgr(ctxGauge(p, ctxWarn, dumbZone, autoCompact),
        s"ctx-fill ${pct(p)}${if p >= autoCompact then " auto-compact!" else if p >= dumbZone then " dumb-zone" else ""}")) // escalates green->orange->red(Z)->dumb-zone(D)->auto-compact(A)
    // rot?/tot token gauges (SM128): rot? = tokens since the last warp (compact/clear) = the CURRENT-window rot
    // signal, COLOURED by threshold; the `?` marks it an inferred proxy (SM118). tot = cumulative lifetime tokens,
    // dim (context only, no threshold meaning), and DROPPED on a narrow terminal (showTot). rot? is the star.
    // The `↑` marks these as OUTPUT-FLOW (agent tokens GENERATED), a different KIND of quantity from ctx-fill's
    // window OCCUPANCY (%): a flow-count vs a level, decoupled, never expected to reconcile. The glyph stops a
    // glancer grouping `2k` with `4%` on one axis (wr-data 2026-07-17, confirmed against TranscriptStats).
    rotTokens.foreach(r => segs += sgr(tokGauge(r, tokWarn, tokDanger), s"rot?↑${formatTokens(r)}"))
    if showTot then totTokens.foreach(t => segs += sgr("38;5;245", s"tot↑${formatTokens(t)}"))
    val rl = o.get("rate_limits").flatMap(_.obj)
    // Compact rate-limit cluster (BR 2026-07-17): factor the twice-repeated "lim"/"reset" words into ONE gray
    // legend `lim/reset`, whose slash MIRRORS the value slash `P%/reset` — the legend IS the column header. Each
    // window's whole cluster (`5h P%/reset`, `wk P%/reset`) takes its own gauge colour, so a near-cap limit reds as
    // one solid block (the reset reddens WITH its limit, for free) and all of one window's info shares a hue. Both
    // halves stay independently guarded: a missing % or reset simply drops from the cluster (no orphan slash).
    def limCluster(label: String, base: String, usedP: Option[Double], reset: Option[String]): Option[String] =
      Option.when(usedP.isDefined || reset.isDefined):
        val body = (usedP.map(pct), reset) match
          case (Some(p), Some(r)) => s"$label $p/$r"
          case (Some(p), None)    => s"$label $p"
          case (None, Some(r))    => s"$label $r"    // CC sent no %: show the window + its reset, ungraded
          case (None, None)       => label            // unreachable under the Option.when guard
        sgr(usedP.map(p => limGauge(p, warn, base)).getOrElse("38;5;245"), body)
    val m5  = rl.flatMap(_.get("five_hour")).flatMap(_.obj)
    val mWk = rl.flatMap(_.get("seven_day")).flatMap(_.obj)
    val clusters = List(
      limCluster("5h", "38;5;176", // rolling 5-hour window
        m5.flatMap(_.get("used_percentage")).flatMap(_.num),
        m5.flatMap(_.get("resets_at")).flatMap(_.num).map(r => relResetFine(r.toLong, nowMs))),
      limCluster("wk", "38;5;174", // weekly window, rosy-red base
        mWk.flatMap(_.get("used_percentage")).flatMap(_.num),
        mWk.flatMap(_.get("resets_at")).flatMap(_.num).map(r => relReset(r.toLong, nowMs)))
    ).flatten
    // `lim/reset` legend glued to its first column by ONE space (BR); two-space `sep` between the two clusters.
    if clusters.nonEmpty then segs += sgr("38;5;245", "lim/reset") + " " + clusters.mkString(sep)
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
    "limit-near" -> "38;5;203", "delegation" -> "38;5;111",
    // `hangover` (BR 2026-07-17): DECLARED, never derived — the agent is still warming after a warp/compact and is
    // reading itself hot. Orange echoes the retired chip's "a real break" band, but nothing grades it now: it is
    // on/off because a person judged it, not a number that crossed a line.
    "hangover" -> "38;5;209"
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
    "hangover", "rot-vigil", "dumb-zone",    // agent state + vigilance (hangover LEADS: it is the transient one,
                                             //   and it is the one that most changes how the next few turns go)
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
  /** The mode line: brand prefix + the active DECLARED modes (each reverse+bold, own colour), joined by " & ". PURE.
    * Nothing active -> a dim placeholder, so the line is still recognisable as the (empty) mode line.
    *
    * LINE-2 CONTRACT (BR, 2026-07-17): **everything here was DECLARED by someone** — the agent, the human (`+afk`),
    * or a negotiated `discuss-mode-dance`. Measured things belong on line 1. The split is what makes the SURFACE
    * encode the provenance: on line 1 a mechanism measured it; here, someone said it. No provenance field needed.
    *
    * NOTE THE SIGNATURE: there is deliberately NO `derived` parameter. It had one (SM121's `hangover?` chip rode
    * here), and dropping it makes the declared-only contract STRUCTURAL rather than documentary — you cannot put a
    * derived thing on line 2 because there is nowhere to put it. A comment saying "declared only" would rot; an
    * absent parameter cannot. (The day's load-bearing finding, applied to this function.)
    *
    * `hangover` still exists — as a DECLARED mode, no `?`, owned by whoever declares it. A `?` marks an inferred
    * proxy; a declaration is not a proxy, it is a judgment with an owner. BR usually declares it, because the agent
    * is the unreliable narrator of its own warmth: the rule is that the declarer is whoever can OBSERVE the state,
    * which is sometimes the other party. */
  def renderModes(modes: Seq[String]): String =
    val brand = sgr("1;38;5;42", "gs mode set") // line-2 prefix: NOT "genscalator" again (redundant with line 1); doubles as the DWIM verb
    val chips = sortModes(modes).map(renderMode)
    if chips.isEmpty then s"$brand ${sgr("38;5;245", "clear: no active mode labels")}"
    else s"$brand ${chips.mkString(" & ")}"
  // ---------- box line (SM163): a third MEASURED row — box health at a glance ----------
  // Rationale: this box OOM-crashes GNOME and bloop wedges silently ([[blixten-box-flaky]], SM146/SM150), and
  // the human was watching it with gnome-system-monitor at 2.2GB RSS. This line is a passive readout of the
  // same facts for ~zero marginal cost: a few /proc and /sys FILE READS per tick, NO subprocess spawned.
  // LINE-1 FAMILY (line-1-measured / line-2-declared contract): everything here is MEASURED by a mechanism.
  // The LEAD CHIP is the one aggregate: "box healthy" / "box huffing" / "box swamped" (each exactly
  // "genscalator".length = 11 chars, so the three row-leads align) = the WORST severity across the segments,
  // computed from the SAME thresholds that colour them — a lift of the existing colour semantics into the
  // name, not a new inference (precedent: ctx-fill's "dumb-zone" flag). Inferred proxies would carry `?`;
  // none do yet (`wedge?` detection is SM146b, deferred).
  // Linux-only by data source (/proc, /sys); EVERY reader is guarded, so on any other OS gather() yields None
  // and the line simply does not print — alpha-tester-safe degradation.
  object BoxStats:
    /** One gathered snapshot of the box. PURE data; `renderBox` renders it (testable without /proc). */
    final case class BoxInfo(memUsedKb: Long, memTotalKb: Long, load1: Double, cores: Int,
                             tempC: Option[Int], jvmCount: Int, jvmRssKb: Long, bloopRssKb: Option[Long])
    /** MemTotal + MemAvailable (kB) from /proc/meminfo content. "Used" = total - available (the kernel's own
      * reclaimable-aware figure — matches what `free` calls available, unlike total - free). PURE. */
    def parseMemInfo(s: String): Option[(Long, Long)] =
      def kb(key: String): Option[Long] =
        s.linesIterator.find(_.startsWith(key + ":")).flatMap(_.trim.split("\\s+").lift(1)).flatMap(_.toLongOption)
      for t <- kb("MemTotal"); a <- kb("MemAvailable") yield (t, a)
    /** 1-minute load average from /proc/loadavg content ("3.50 2.10 1.00 2/1234 5678"). PURE. */
    def parseLoadAvg(s: String): Option[Double] =
      s.trim.split("\\s+").headOption.flatMap(_.toDoubleOption)
    /** VmRSS (kB) from a /proc/<pid>/status content. PURE. */
    def parseVmRssKb(status: String): Option[Long] =
      status.linesIterator.find(_.startsWith("VmRSS:")).flatMap(_.trim.split("\\s+").lift(1)).flatMap(_.toLongOption)

    private def readFile(p: java.nio.file.Path): Option[String] =
      try Option.when(java.nio.file.Files.isReadable(p))(String(java.nio.file.Files.readAllBytes(p), "UTF-8"))
      catch case _: Throwable => None
    /** Hottest thermal zone in whole °C (max across /sys/class/thermal/thermal_zone* — the fan story). */
    def readTempC(): Option[Int] =
      try
        val dir = java.nio.file.Path.of("/sys/class/thermal")
        if !java.nio.file.Files.isDirectory(dir) then None
        else
          val zones = scala.jdk.CollectionConverters.IteratorHasAsScala(java.nio.file.Files.list(dir).iterator()).asScala
            .filter(_.getFileName.toString.startsWith("thermal_zone"))
            .flatMap(z => readFile(z.resolve("temp")).flatMap(_.trim.toLongOption)).toVector
          if zones.isEmpty then None else Some((zones.max / 1000).toInt)
      catch case _: Throwable => None
    /** Scan /proc for JVMs: (count, total VmRSS kB, bloop's VmRSS kB if a bloop JVM is present). A process is
      * a JVM iff its comm is "java"; it is BLOOP iff its cmdline contains "bloop" (SUBSTRING heuristic — note
      * `pkill -f BloopServer` missed a live BloopServer 2026-07-19, so the main-class string is NOT reliably in
      * the cmdline; the ~/.cache/bloop classpath entries are). The cmdline is read, never printed — bounded
      * OUTPUT is the SM160 constraint, and this emits at most two numbers. */
    def jvmScan(): (Int, Long, Option[Long]) =
      try
        val procs = scala.jdk.CollectionConverters.IteratorHasAsScala(
          java.nio.file.Files.list(java.nio.file.Path.of("/proc")).iterator()).asScala
          .filter(_.getFileName.toString.forall(_.isDigit)).toVector
        var count = 0; var rss = 0L; var bloopRss = Option.empty[Long]
        procs.foreach: p =>
          if readFile(p.resolve("comm")).map(_.trim).contains("java") then
            val vm = readFile(p.resolve("status")).flatMap(parseVmRssKb).getOrElse(0L)
            count += 1; rss += vm
            if readFile(p.resolve("cmdline")).exists(_.toLowerCase.contains("bloop")) then
              bloopRss = Some(bloopRss.getOrElse(0L) + vm)
        (count, rss, bloopRss)
      catch case _: Throwable => (0, 0L, None)
    /** One effectful gather; None when the box offers no /proc (non-Linux) → the line silently absent. */
    def gather(): Option[BoxInfo] =
      for
        (total, avail) <- readFile(java.nio.file.Path.of("/proc/meminfo")).flatMap(parseMemInfo)
        load           <- readFile(java.nio.file.Path.of("/proc/loadavg")).flatMap(parseLoadAvg)
      yield
        val (jc, jr, br) = jvmScan()
        BoxInfo(total - avail, total, load, Runtime.getRuntime.availableProcessors, readTempC(), jc, jr, br)

  /** The box line. Severity 0/1/2 (green/orange/red) per segment from explicit thresholds; the lead chip is
    * the MAX severity, so the name flips healthy -> huffing -> swamped exactly when a segment leaves green.
    * All thresholds are first-cut GUESSES (mem/load reuse the 70/90 gauge; temp 70/85 °C; bloop 2G/6G from the
    * lived 10.4GB specimen); tune against reality. PURE — testable with a synthetic BoxInfo. */
  def renderBox(b: BoxStats.BoxInfo): String =
    def gb(kb: Long): String = f"${kb / 1048576.0}%.1fG"
    def sev(p: Double): Int = if p >= 90 then 2 else if p >= 70 then 1 else 0
    def colour(s: Int, healthy: String): String = s match
      case 2 => Red
      case 1 => "38;5;214"
      case _ => healthy
    val memPct   = 100.0 * b.memUsedKb / b.memTotalKb
    val loadPct  = 100.0 * b.load1 / b.cores
    val tempSev  = b.tempC.map(t => if t >= 85 then 2 else if t >= 70 then 1 else 0).getOrElse(0)
    val bloopSev = b.bloopRssKb.map(r => if r >= 6L * 1048576 then 2 else if r >= 2L * 1048576 then 1 else 0).getOrElse(0)
    val overall  = List(sev(memPct), sev(loadPct), tempSev, bloopSev).max
    val lead = overall match // 11 chars each, aligning with "genscalator" / "gs mode set" (BR 2026-07-19)
      case 2 => sgr("1;38;5;203", "box swamped")
      case 1 => sgr("1;38;5;214", "box huffing")
      case _ => sgr("1;38;5;114", "box healthy")
    val segs = scala.collection.mutable.ArrayBuffer[String](lead)
    segs += sgr(colour(sev(memPct), "38;5;114"), f"mem ${b.memUsedKb / 1048576.0}%.1f/${gb(b.memTotalKb)}") // one G, on the total
    segs += sgr(colour(sev(loadPct), "38;5;110"), f"load ${b.load1}%.1f/${b.cores}")
    b.tempC.foreach(t => segs += sgr(colour(tempSev, "38;5;114"), s"temp ${t}C"))
    if b.jvmCount > 0 then segs += sgr("38;5;245", s"jvm ${b.jvmCount}x${gb(b.jvmRssKb)}") // dim readout; its weight already counts inside mem
    b.bloopRssKb.foreach(r => segs += sgr(colour(bloopSev, "38;5;245"), s"bloop ${gb(r)}"))
    segs.mkString(sep)

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
      |  silent Ns           feed inactivity: now - the last timestamped transcript record. NO `?`:
      |                      this is COUNTED, not inferred (cf. rot?, which is a proxy and keeps its
      |                      `?`). No threshold, no colour, never hidden — a READOUT, not a gauge.
      |                      Its subject is the FEED, not a person, so it cannot misattribute — and
      |                      the NAME says so: nobody is "idle" (BR was reading a newspaper; the
      |                      agent was making tool calls), the FEED is silent. NB a running command
      |                      writes no record, so agent-busy time counts as silence.
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
      |  --box-line          ALSO emit the box line (line 3, SM163: MEASURED box health from
      |                      /proc + /sys — lead chip "box healthy"/"box huffing"/"box swamped"
      |                      = worst segment severity; mem used/total, 1-min load / cores, max
      |                      thermal temp, JVM count + total RSS, a bloop chip when a bloop JVM
      |                      is present. Linux-only; silently absent elsewhere)
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
    var boxLine   = false // --box-line: also emit the box line (line 3, SM163 — measured box health)
    var noStatus  = false // --no-status: suppress line 1 (e.g. to show ONLY the mode line)
    var modesFile = defaultModesFile
    var tokWarn    = 200_000L // rot? (since-warp) orange threshold (GUESS for one window, configurable via --tok-warn)
    var tokDanger  = 500_000L // rot? (since-warp) red threshold (GUESS for one window, configurable via --tok-danger)
    var tiredChars: Option[Long] = None // human-char `tired?` nudge threshold; None = OFF (opt-in via --tired-chars)
    var noTok      = false // --no-tok: skip the transcript read entirely (no tok gauge)
    var rotOnly    = false // --rot-only: show rot? but DROP the secondary tot gauge (also auto-dropped if narrow)
    // RETIRED 2026-07-17 with the chip: hangoverSec/Warn/Danger (60s/5min/1h) + their --hangover-* flags. `silent`
    // has NO threshold, so there is nothing left to tune. Safe to drop: the live config is `tt statusline
    // --mode-line` and never passed them (checked, not assumed).
    //
    // PRESERVING WHAT THE DELETED COMMENT HELD, because it was a real finding and this was its home: the pair's
    // WORKING NOISE FLOOR is ~60s, not 10s — "an 18s gap was just a command running", so a 10s gate never clears;
    // a compact is ~140s MEASURED (compact-timing.log, 7 pairs: 124-162s). SM132's audit established that this
    // comment was that finding's ENGINEERING home and that it had no RESEARCH home. It has one now:
    // research/wr-data/hangover-chip-fires-on-the-humans-thinking-pause-2026-07-17.md (§ the TWO noise floors —
    // the 60s calibration only ever sampled gaps the AGENT makes, never the human's 1-3min thinking pauses, which
    // is why the chip fired on BR composing a message). The rationale dies with its constant; the measurement does
    // not.
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
        case "--box-line"                       => boxLine   = true; i += 1
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
    val stats: Option[(agentTokens: Long, humanChars: Long, sinceWarpTokens: Long, lastStampMs: Long)] =
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
    // SM121: gap = now - the last timestamped record. Only a POSITIVE gap from a real record counts (lastStampMs 0
    // = no timestamped records = a fresh transcript = nothing to say).
    val gapSec: Option[Long] =
      stats.map(_.lastStampMs).filter(_ > 0L).map(ms => math.max(0L, (nowMs - ms) / 1000L))
    // Each println is a SEPARATE status row (Claude Code renders multi-line statuslines).
    if !noStatus then println(render(json, nowMs, warn, ctxWarn, dumbZone, autoCompact,
      rotTokens = stats.map(_.sinceWarpTokens), totTokens = stats.map(_.agentTokens), showTot = showTot,
      humanChars = stats.map(_.humanChars), tokWarn = tokWarn, tokDanger = tokDanger, tiredChars = tiredChars,
      silentSec = gapSec))
    // LINE 2 is DECLARED-ONLY (BR 2026-07-17): the gap now rides line 1 as `silent`, and there is no derived-chip
    // argument left to pass — see renderModes.
    if modeLine then println(renderModes(readModes(modesFile)))
    // LINE 3 (SM163): measured box health; gather() is None off-Linux so the row is silently absent there.
    if boxLine then BoxStats.gather().foreach(b => println(renderBox(b)))
    0

@main def statusLine(args: String*): Unit = sys.exit(StatuslineTool.dispatch(args.toList))
