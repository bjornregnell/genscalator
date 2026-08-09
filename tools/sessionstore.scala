// (no directives at all: mainless helper — inherits everything from its includer; see project.scala)

// sessionstore — the SHARED, MAINLESS store logic behind per-SESSION mode scoping (SM208):
// `tt mode` (chip writer), `tt session` (name writer) and `tt statusline` (reader/renderer).
// Split out like limitstore.scala: a tool file with a @main cannot be `using file`-included by
// another tool.
//
// WHY (SM208, ratified by BR 2026-07-24): the one global ~/.claude/gs-modes file is a CORRECTNESS
// bug with concurrent sessions — a chip flipped in one terminal leaks into every other, and the
// mode line is what an agent reads to decide how autonomously to act. The principle: MODES ARE NOT
// GLOBAL — every workflow mode is per-session, WITHOUT exception (BR 2026-07-28, superseding the
// same-pin budget-chip carve-out: the shared FACT of account headroom lives in `tt limit`'s machine
// store; a budget CHIP is this session's spend POLICY, and policy differs per session).
//
// KEY: the harness session id (env CLAUDE_CODE_SESSION_ID, also `session_id` in the statusline's
// stdin JSON). It is opaque and useless as a NAME, but a key only has to be unique and stable
// (BR's objection and its resolution, SM208). No session id (a bare shell) -> the global file,
// exactly today's behavior.
//
// STORE: ~/.claude/gs-sessions/<session-id>/ with four tiny files —
//   modes    one chip label per line (the same format as the global file)
//   name     the human-chosen session name, ONE line (spaces allowed; control chars rejected)
//   started  epoch-ms stamped when the dir is first created by a WRITER (never by statusline)
//   cwd      the working directory, ONE line, stamped once by writers (issue-023): the id key is
//            unique but NOT stable (a harness bg/fg round trip re-mints it), and orphan recovery
//            must match entries to THIS directory — see the orphan-recovery section below.
// THE PARTS ARE STORED, NEVER THE JOIN (SM259 rider): `YYMMDD-HHhMMm-MyName` is concatenated at
// RENDER time only, because names contain hyphens and the join cannot be split back. The display
// name is never a path component; the id is the key.

object SessionStore:
  import java.nio.file.{Files, Path}

  def sessionId: Option[String] =
    sys.env.get("CLAUDE_CODE_SESSION_ID").map(_.trim).filter(_.nonEmpty)
      .filter(_.matches("[A-Za-z0-9-]+")) // defensive: the id becomes a directory name

  def defaultRoot: Path =
    Path.of(sys.props.getOrElse("user.home", "."), ".claude", "gs-sessions")

  /** A session NAME is free text for a human: spaces ALLOWED, control characters (incl. newline —
    * the store is one-value-per-line) REJECTED. The chip validator RELAXED, not reused (SM259). */
  def validName(s: String): Boolean =
    s.nonEmpty && s.length <= 120 && !s.exists(_.isControl)

  /** The always-present default name part: `YYMMDD-HHhMMm` (BR's format, third iteration): fixed
    * width, chronologically sortable, filesystem-safe by construction (no colon). PURE. */
  def defaultName(startedAtMs: Long, zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): String =
    java.time.format.DateTimeFormatter.ofPattern("yyMMdd-HH'h'mm'm'").withZone(zone)
      .format(java.time.Instant.ofEpochMilli(startedAtMs))

  /** The rendered display name: timestamp ALWAYS present and FIRST; the human name is a SUFFIX.
    * The age signal survives naming, and duplicate human names cannot collide. PURE. */
  def displayName(startedAtMs: Long, name: Option[String],
      zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): String =
    defaultName(startedAtMs, zone) + name.map("-" + _).getOrElse("")

  def dir(root: Path, id: String): Path        = root.resolve(id)
  def modesFile(root: Path, id: String): Path  = dir(root, id).resolve("modes")
  def nameFile(root: Path, id: String): Path   = dir(root, id).resolve("name")
  def startedFile(root: Path, id: String): Path = dir(root, id).resolve("started")

  /** Chip lines, tolerant of an absent file — the SAME parse the global file has always had. */
  def readChips(file: Path): Vector[String] =
    try
      if Files.isRegularFile(file) then
        String(Files.readAllBytes(file), "UTF-8").linesIterator.map(_.trim).filter(_.nonEmpty).toVector.distinct
      else Vector.empty
    catch case _: Throwable => Vector.empty

  def writeChips(file: Path, chips: Seq[String]): Unit =
    Option(file.getParent).foreach(Files.createDirectories(_))
    val body = if chips.isEmpty then "" else chips.mkString("\n") + "\n"
    Files.write(file, body.getBytes("UTF-8"))

  def readName(root: Path, id: String): Option[String] =
    try
      val f = nameFile(root, id)
      if Files.isRegularFile(f) then
        String(Files.readAllBytes(f), "UTF-8").linesIterator.nextOption().map(_.trim).filter(_.nonEmpty)
      else None
    catch case _: Throwable => None

  def readStarted(root: Path, id: String): Option[Long] =
    try
      val f = startedFile(root, id)
      if Files.isRegularFile(f) then String(Files.readAllBytes(f), "UTF-8").trim.toLongOption else None
    catch case _: Throwable => None

  /** Stamp `started` if absent. Called by WRITERS (tt mode / tt session) when they first touch a
    * session — deliberately never by statusline, whose contract is read-mostly. */
  def ensureStarted(root: Path, id: String, nowMs: Long): Long =
    readStarted(root, id).getOrElse:
      val f = startedFile(root, id)
      Option(f.getParent).foreach(Files.createDirectories(_))
      Files.writeString(f, nowMs.toString + "\n")
      nowMs

  def writeName(root: Path, id: String, name: String, nowMs: Long): Unit =
    require(validName(name), s"invalid session name")
    ensureStarted(root, id, nowMs)
    Files.writeString(nameFile(root, id), name + "\n")

  def clearName(root: Path, id: String): Unit =
    try Files.deleteIfExists(nameFile(root, id)) catch case _: Throwable => ()

  /** Opportunistic GC: drop session dirs untouched for `olderThanDays`. The store is not colocated
    * with the harness's own session files, so it does not get GC for free; this keeps it bounded.
    * Best-effort and silent — a GC failure must never break a mode write. */
  def prune(root: Path, nowMs: Long, olderThanDays: Int = 14): Unit =
    try
      if Files.isDirectory(root) then
        val cutoff = nowMs - olderThanDays.toLong * 86400_000L
        val ds = Files.list(root)
        try
          ds.iterator.forEachRemaining: d =>
            try
              if Files.isDirectory(d) && Files.getLastModifiedTime(d).toMillis < cutoff then
                val fs = Files.list(d)
                try fs.iterator.forEachRemaining(f => Files.deleteIfExists(f)) finally fs.close()
                Files.deleteIfExists(d)
            catch case _: Throwable => ()
        finally ds.close()
    catch case _: Throwable => ()

  // ---- issue-023: orphan recovery (harness session-id re-mint, e.g. a bg/fg round trip) ----
  // The id key is unique but NOT stable: when the harness re-mints it, name + chips sit orphaned
  // under the old key while the new key reads empty. Recovery = the explicit `tt session adopt`
  // verb plus a one-line stderr hint on empty-state reads; `tt mode` and `tt session` SHARE the
  // single copy below (this file is the mainless home both include — a @main tool cannot be
  // file-included by the other). Matching needs the working directory, which the id-only key never
  // recorded — so writers stamp the `cwd` file (once, like `started`); entries from before that
  // stamp have no cwd on record and are deliberately invisible here, because adopting across
  // directories would attach another project's chips.

  /** One store entry as scanned for orphan hunting. `cwd` is None for entries written before cwd
    * stamping existed; `mtimeMs` is the newest write anywhere in the entry's dir. */
  final case class Orphan(id: String, name: Option[String], chips: Vector[String],
      startedMs: Option[Long], cwd: Option[String], mtimeMs: Long)

  /** Hint age cap. 48h covers the realistic gap between a bg/fg id re-mint and the next read (the
    * same day or the day after); older same-directory state is more likely a FINISHED session than
    * an orphan of this one, and a hint that fires on every read forever would be tuned out. The
    * explicit `adopt` verb applies NO cap (the human decides); the 14-day GC above bounds all. */
  val HintMaxAgeMs: Long = 48L * 3_600_000L

  /** Candidate selection (same directory, newest first, age-capped): PURE so the adopt/hint
    * semantics are unit-testable without a harness (issue-023 acceptance). An entry qualifies only
    * if it RECORDED the same working directory — a missing cwd (pre-stamp entry) never matches. */
  def selectOrphans(cands: Vector[Orphan], currentId: String, currentCwd: String,
      nowMs: Long, maxAgeMs: Long): Vector[Orphan] =
    cands.filter(c =>
        c.id != currentId
          && c.cwd.contains(currentCwd)
          && (c.chips.nonEmpty || c.name.nonEmpty)
          && (nowMs - c.mtimeMs) <= maxAgeMs)
      .sortBy(-_.mtimeMs)

  /** Human age like `7m` / `3h` / `2d`, floored; negative (clock skew) clamps to 0m. PURE. */
  def ageStr(ageMs: Long): String =
    val m = ageMs.max(0L) / 60_000L
    if m < 60 then s"${m}m" else if m < 48 * 60 then s"${m / 60}h" else s"${m / (24 * 60)}d"

  def cwdFile(root: Path, id: String): Path = dir(root, id).resolve("cwd")

  def readCwd(root: Path, id: String): Option[String] =
    try
      val f = cwdFile(root, id)
      if Files.isRegularFile(f) then
        String(Files.readAllBytes(f), "UTF-8").linesIterator.nextOption().map(_.trim).filter(_.nonEmpty)
      else None
    catch case _: Throwable => None

  /** Stamp the working directory once, like `started` — writers only. Without it a later session
    * cannot tell whether an entry belongs to ITS directory when hunting orphans after an id
    * re-mint. Best-effort: a stamp failure must never break a name/chip write. */
  def ensureCwd(root: Path, id: String, cwd: String): Unit =
    try
      if readCwd(root, id).isEmpty then
        val f = cwdFile(root, id)
        Option(f.getParent).foreach(Files.createDirectories(_))
        Files.writeString(f, cwd + "\n")
    catch case _: Throwable => ()

  /** IO scan of every store entry into Orphan values; the selection itself stays pure above.
    * Best-effort and silent like prune — a scan failure must never break a read. */
  def scanStore(root: Path): Vector[Orphan] =
    try
      if !Files.isDirectory(root) then Vector.empty
      else
        val buf = Vector.newBuilder[Orphan]
        val ds = Files.list(root)
        try
          ds.iterator.forEachRemaining: d =>
            try
              if Files.isDirectory(d) then
                val id = d.getFileName.toString
                var mt = Files.getLastModifiedTime(d).toMillis
                val fs = Files.list(d)
                try
                  fs.iterator.forEachRemaining: f =>
                    try mt = mt.max(Files.getLastModifiedTime(f).toMillis) catch case _: Throwable => ()
                finally fs.close()
                buf += Orphan(id, readName(root, id), readChips(modesFile(root, id)),
                  readStarted(root, id), readCwd(root, id), mt)
            catch case _: Throwable => ()
        finally ds.close()
        buf.result()
    catch case _: Throwable => Vector.empty

  /** The one-line orphan hint. PURE render; stderr-only at the call sites, because stdout of
    * `tt session` / `tt mode` is parsed (statusline and agents) and must stay byte-stable. */
  def hintLine(best: Orphan, nowMs: Long): String =
    val disp = displayName(best.startedMs.getOrElse(best.mtimeMs), best.name)
    val chipsPart = if best.chips.isEmpty then "no chips" else "chips " + best.chips.mkString(" ")
    s"hint: orphaned session state for this directory exists ($disp, $chipsPart, ${ageStr(nowMs - best.mtimeMs)} old) — adopt with: tt session adopt"

  /** Is the current key EMPTY (no name, no chips, not even a started stamp)? The hint fires only
    * then — partial re-declaration under the new key means the human is already recovering. */
  def keyIsEmpty(root: Path, id: String): Boolean =
    readName(root, id).isEmpty
      && readStarted(root, id).isEmpty
      && readChips(modesFile(root, id)).isEmpty

  /** The whole empty-state hint in one call, shared verbatim by `tt mode` (list) and `tt session`
    * (print): Some(line) only when the current key is fully empty AND a recent same-directory
    * orphan exists. Callers print to STDERR only. */
  def orphanHint(root: Path, id: String, cwd: String, nowMs: Long): Option[String] =
    if keyIsEmpty(root, id) then
      selectOrphans(scanStore(root), id, cwd, nowMs, HintMaxAgeMs).headOption.map(hintLine(_, nowMs))
    else None
