//> using file project.scala
//> using file sessionstore.scala
//> using jvm 21

// session — name THIS session, so parallel sessions are tellable apart at a glance (SM208/SM259).
//   tt session                 print the display name: YYMMDD-HHhMMm[-MyName]
//   tt session <name words>    set the human name part (spaces allowed)
//   tt session --clear         remove the human name (the timestamp part always remains)
//   tt session adopt [<id>]    re-attach orphaned state after a harness session-id re-mint (issue-023)
//   tt session list            list sessions recorded for THIS directory (pure read; alias: ls)
// The timestamp is ALWAYS present and FIRST (BR's format): the age signal survives naming, duplicate
// human names cannot collide, and the string is filesystem-safe by construction (no colon) — though
// the display name is NEVER a path component; the store is keyed on the opaque harness session id
// (env CLAUDE_CODE_SESSION_ID), which is useless as a name but perfect as a key. State lives in
// ~/.claude/gs-sessions/<id>/ via sessionstore.scala; the statusline renders the name inverted after
// the `gs session:` label. This tool owns only the NAME; chips live in `mode`.
//
// issue-023: the harness id is unique but NOT stable — a bg/fg round trip re-mints it, orphaning
// name + chips under the old key while reads of the new key find silent emptiness. Recovery is the
// explicit `adopt` verb plus a one-line stderr hint on empty-state reads; the shared scan/select/
// hint logic lives in sessionstore.scala (SessionStore's orphan-recovery section), used by both this
// tool and `tt mode`. Auto-adopt is decided OUT of scope, and with SEVERAL candidates adopt refuses
// to pick: two live sessions in one directory is exactly where an auto-pick would union ANOTHER
// LIVE SESSION's chips into this one — the user must choose with `adopt <id>`.
//
// issue-037: any bare word used to be a SETTER, so `tt session list` (the obvious roster query)
// silently RENAMED the live session. Read-shaped lone words are now RESERVED like adopt is:
// list/ls run the roster read; show/status/current/get/name print the display name — none can ever
// set a name. The setter itself announces `session: renamed <old> -> <new>` on stderr, so a write
// can no longer pass as a read; stdout stays byte-stable for everything that parses it.
import java.nio.file.{Files, Path}

object SessionTool:
  val Help: String =
    """tt session — name THIS session, so parallel sessions are tellable apart (v0.10.0)
      |
      |Usage:
      |  session                     print the display name: YYMMDD-HHhMMm[-MyName]
      |  session <name words...>     set the human name part (free text, spaces allowed;
      |                              newlines/control characters rejected; max 120 chars)
      |  session list                list every session recorded for THIS directory, newest first,
      |                              the current one starred — a pure read; `ls` is an alias
      |  session --clear             remove the human name (the timestamp part remains)
      |  session adopt               re-attach ORPHANED state to this session: when the harness
      |                              re-mints the session id (e.g. a background/foreground round
      |                              trip), the old key's name + chips look cleared while the state
      |                              sits orphaned under the old key. With exactly ONE orphan
      |                              recorded for the SAME working directory, adopt copies it under
      |                              the current key and reports what was adopted (name, chips,
      |                              age). With SEVERAL candidates NOTHING is adopted — one may be
      |                              another LIVE session in this directory — they are listed
      |                              (newest first) and you pick with `adopt <id>`; exit 2. No
      |                              orphan: says so and exits 2. Adoption is always explicit —
      |                              there is no auto-adopt.
      |  session adopt <id>          adopt exactly the candidate with store id <id> (ids are shown
      |                              by a bare `adopt`); an id that is not an adoptable candidate
      |                              for this directory is an error naming the valid ones, exit 2.
      |  session --sessions-root <d> override the store root (config-in-args, for tests)
      |  session --id <id>           override the session id (for tests; default env CLAUDE_CODE_SESSION_ID)
      |  session --cwd <dir>         override the working directory used for orphan matching (for tests)
      |  session --now-ms <ms>       fixed clock (for deterministic tests)
      |
      |The timestamp part is ALWAYS present and FIRST — the age signal survives naming and two
      |sessions named the same can never be confused. Outside a harness session (no session id)
      |there is nothing to name: the tool says so and exits 1.
      |
      |`adopt` and the READ words list/ls/show/status/current/get/name are RESERVED: a lone word
      |spelled that way in any capitalization is the verb, never a name — a query must NEVER write
      |(issue-037: `tt session list` used to silently rename the session). Multi-word names are
      |unaffected. Setting a name announces itself on stderr (`session: renamed <old> -> <new>`),
      |so a write can never be mistaken for a read; stdout stays the display name, byte-stable.
      |
      |When an empty-state read (`tt session` or `tt mode`) finds recent (<48h) orphaned state for
      |this directory, ONE hint line goes to stderr pointing at `tt session adopt`; stdout stays
      |exactly as before, so nothing that parses it can break.
      |
      |Examples:
      |  tt session alpha prep       # this session now renders as e.g. 260728-15h42m-alpha prep
      |  tt session                  # what is this session called?
      |  tt session list             # which sessions has this directory seen? (never a setter)
      |  tt session --clear          # back to the bare timestamp
      |  tt session adopt            # chips/name vanished after a bg/fg? re-attach the orphan
      |
      |Chips/modes are a separate field: see `tt mode`. Full reference: tools/README.md""".stripMargin

  /** One candidate as listed to the human: id first (it is what `adopt <id>` takes). */
  private def candLine(o: SessionStore.Orphan, nowMs: Long): String =
    val disp = SessionStore.displayName(o.startedMs.getOrElse(o.mtimeMs), o.name)
    val chipsPart = if o.chips.isEmpty then "no chips" else "chips: " + o.chips.mkString(" ")
    s"  ${o.id}  $disp ($chipsPart; ${SessionStore.ageStr(nowMs - o.mtimeMs)} old)"

  // issue-037: lone words that must NEVER become a name — every plausible spelling of a read.
  // RosterWords run the roster; the rest print the current name exactly like a bare `tt session`.
  private val RosterWords: Set[String] = Set("list", "ls")
  private val ReadWords: Set[String]   = RosterWords ++ Set("show", "status", "current", "get", "name")

  /** The bare read: orphan hint (stderr only) + display name. Shared by the reserved read words. */
  private def printName(root: Path, id: String, cwd: String, nowMs: Long): Int =
    SessionStore.orphanHint(root, id, cwd, nowMs).foreach(Console.err.println) // stderr ONLY
    val started = SessionStore.readStarted(root, id).getOrElse(nowMs)
    println(SessionStore.displayName(started, SessionStore.readName(root, id)))
    0

  /** issue-037 roster: every recorded session for THIS directory, newest first — the same data
    * adopt enumerates when it lists candidates, plus the current session. A PURE read: it scans
    * and prints, and deliberately writes NOTHING (no prune, no cwd stamp). */
  private def roster(root: Path, id: String, cwd: String, nowMs: Long): Int =
    val entries = SessionStore.scanStore(root)
      .filter(o => o.id == id || o.cwd.contains(cwd))
      .sortBy(-_.mtimeMs)
    if entries.isEmpty then println(s"no sessions recorded for $cwd")
    else
      println(s"sessions for $cwd (newest first; * marks this session):")
      entries.foreach(o => println((if o.id == id then "*" else " ") + candLine(o, nowMs)))
    0

  /** Copy ONE chosen orphan's state under the current key and report it. */
  private def adoptOne(root: Path, id: String, cwd: String, nowMs: Long,
      best: SessionStore.Orphan): Int =
    // Chips: UNION, orphan's first — chips declared AFTER the id re-mint are live declarations
    // and must survive adoption (the field timeline in issue-023 shows exactly that split).
    val curChips = SessionStore.readChips(SessionStore.modesFile(root, id))
    SessionStore.writeChips(SessionStore.modesFile(root, id), (best.chips ++ curChips).distinct)
    // started: the orphan's (earlier) stamp OVERWRITES — adoption claims continuity, so the
    // age signal should show the true session start, not the post-re-mint first write.
    val started = best.startedMs.orElse(SessionStore.readStarted(root, id)).getOrElse(nowMs)
    Files.writeString(SessionStore.startedFile(root, id), started.toString + "\n")
    // Name: the orphan's, unless the human already named the NEW key (newest explicit choice wins).
    if SessionStore.readName(root, id).isEmpty then
      best.name.foreach(n => SessionStore.writeName(root, id, n, nowMs))
    SessionStore.ensureCwd(root, id, cwd)
    val disp = SessionStore.displayName(started, SessionStore.readName(root, id))
    val chipsPart = if best.chips.isEmpty then "no chips" else "chips: " + best.chips.mkString(" ")
    println(s"adopted: $disp ($chipsPart; ${SessionStore.ageStr(nowMs - best.mtimeMs)} old; from id ${best.id})")
    0

  private def adopt(root: Path, id: String, cwd: String, nowMs: Long, chosen: Option[String]): Int =
    // No age cap on candidates: adoption is an explicit human act on a named candidate; the 48h
    // cap exists to keep the unsolicited HINT from nagging, not to second-guess a deliberate
    // recovery. Prune first so long-dead entries cannot surface as candidates.
    SessionStore.prune(root, nowMs)
    val cands = SessionStore.selectOrphans(SessionStore.scanStore(root), id, cwd, nowMs, maxAgeMs = Long.MaxValue)
    chosen match
      case Some(pick) =>
        cands.find(_.id == pick) match
          case Some(o) => adoptOne(root, id, cwd, nowMs, o)
          case None =>
            Console.err.println(s"session adopt: '$pick' is not an adoptable orphan for this directory")
            if cands.nonEmpty then
              Console.err.println("valid candidates:")
              cands.foreach(o => Console.err.println(candLine(o, nowMs)))
            2
      case None =>
        cands match
          case Vector() =>
            Console.err.println(
              "session adopt: no orphaned session state found for this directory — nothing to adopt")
            2
          case Vector(one) => adoptOne(root, id, cwd, nowMs, one)
          case many =>
            // NEVER auto-pick among several (issue-023 acceptance): one candidate may be another
            // LIVE session in this directory, and adopting it would union ITS chips into this one.
            Console.err.println(
              "session adopt: several orphan candidates for this directory — one may be another LIVE session, so nothing was adopted; pick one with: tt session adopt <id>")
            many.foreach(o => Console.err.println(candLine(o, nowMs)))
            2

  def dispatch(args: List[String]): Int =
    if args.contains("--help") || args.contains("-h") then { println(Help); return 0 }
    def flagVal(name: String): Option[String] =
      val i = args.indexOf(name); if i >= 0 && i + 1 < args.size then Some(args(i + 1)) else None
    val root  = flagVal("--sessions-root").map(Path.of(_)).getOrElse(SessionStore.defaultRoot)
    val nowMs = flagVal("--now-ms").flatMap(_.toLongOption).getOrElse(System.currentTimeMillis())
    // --id is validated like the env id in SessionStore.sessionId (and like mode.scala does):
    // the id becomes a directory name, and an unvalidated override could escape the store root.
    val sid   = flagVal("--id").filter(_.matches("[A-Za-z0-9-]+")).orElse(SessionStore.sessionId)
    val cwd   = flagVal("--cwd").getOrElse(sys.props.getOrElse("user.dir", "."))
    val consumed =
      List("--sessions-root", "--id", "--cwd", "--now-ms").flatMap { n =>
        val i = args.indexOf(n); if i >= 0 then List(i, i + 1) else Nil
      }.toSet
    val rest = args.zipWithIndex.collect { case (t, i) if !consumed(i) => t }
    sid match
      case None =>
        Console.err.println(
          "session: no session id (env CLAUDE_CODE_SESSION_ID is unset) — outside a harness session there is nothing to name")
        1
      case Some(id) =>
        rest match
          case Nil => printName(root, id, cwd, nowMs)
          case "--clear" :: Nil =>
            SessionStore.clearName(root, id)
            val started = SessionStore.readStarted(root, id).getOrElse(nowMs)
            println(SessionStore.displayName(started, None))
            0
          // A LONE adopt matches in ANY capitalization: during the recovery flow a case-typo must
          // hit the verb, not silently NAME the session "Adopt". Multi-word names are unaffected;
          // the targeted form is exact-lowercase `adopt <id>` with an id validated like --id.
          case verb :: Nil if verb.equalsIgnoreCase("adopt") =>
            adopt(root, id, cwd, nowMs, chosen = None)
          case "adopt" :: pick :: Nil if pick.matches("[A-Za-z0-9-]+") =>
            adopt(root, id, cwd, nowMs, chosen = Some(pick))
          case "adopt" :: bad =>
            Console.err.println(s"session adopt: usage: tt session adopt [<id>] — got '${bad.mkString(" ")}'")
            2
          // issue-037: READ-shaped words are RESERVED — `tt session list` used to silently NAME
          // the session "list". The adopt precedent applies: a LONE word in any capitalization is
          // the verb; exact-lowercase with arguments is a usage error; multi-word names in other
          // capitalizations stay allowed, and the cold-start `tt session <Name>` flow is untouched.
          case verb :: Nil if ReadWords(verb.toLowerCase) =>
            if RosterWords(verb.toLowerCase) then roster(root, id, cwd, nowMs)
            else
              Console.err.println(
                s"session: '$verb' is a reserved READ word — it never sets a name; the current name follows (to list sessions: tt session list)")
              printName(root, id, cwd, nowMs)
          case verb :: bad if ReadWords(verb) =>
            Console.err.println(
              s"session: usage: tt session $verb — takes no arguments; got '${bad.mkString(" ")}'")
            2
          case words if words.nonEmpty && !words.exists(_.startsWith("--")) =>
            val name = words.mkString(" ").trim
            if !SessionStore.validName(name) then
              Console.err.println(
                "session: invalid name — free text is fine (spaces allowed) but control characters are not, max 120 chars")
              2
            else
              // issue-037: a WRITE must announce itself. The audit line goes to STDERR so the
              // parsed stdout (the display name, as ever) stays byte-stable for existing callers.
              val oldName    = SessionStore.readName(root, id)
              val oldStarted = SessionStore.readStarted(root, id)
              SessionStore.writeName(root, id, name, nowMs)
              SessionStore.ensureCwd(root, id, cwd)
              SessionStore.prune(root, nowMs)
              val started = SessionStore.readStarted(root, id).getOrElse(nowMs)
              val newDisp = SessionStore.displayName(started, Some(name))
              val oldDisp = SessionStore.displayName(oldStarted.getOrElse(started), oldName)
              val act     = if oldName.isDefined then "renamed" else "named"
              Console.err.println(s"session: $act $oldDisp -> $newDisp")
              println(newDisp)
              0
          case other =>
            Console.err.println(s"session: unexpected arguments '${other.mkString(" ")}' — see --help")
            2

@main def sessionName(args: String*): Unit = sys.exit(SessionTool.dispatch(args.toList))
