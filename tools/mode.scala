//> using file project.scala
//> using file sessionstore.scala
//> using jvm 21

// mode — record the DECLARED modes of the joint human<->agent state-of-mind (v0.10.0). A "mode" is a label
// stuck on the shared state; MANY can be active at once, and BOTH the human and the agent may declare or clear
// them — it is a joint, mutually-visible channel. Declaring a mode = adding a label to the recorded state
// (a small state file); the statusline's mode line renders whatever is active here. This tool owns ONLY the
// state (add/remove/list); rendering it lives in `statusline`; the session NAME lives in `session`.
//   tt mode                   list the active modes (one per line; session chips + machine chips)
//   tt mode add <label>       declare <label> active (idempotent)
//   tt mode rm <label>        clear <label>
//   tt mode clear             clear this SESSION's modes (bare shell: clears the global file)
//   tt mode --file <f> ...    override: single-file mode on <f>, no session scoping (for tests)
//   tt mode --sessions-root <d> ...   override the session-store root (for tests)
// SCOPING (SM208, BR-ratified 2026-07-24; made UNIFORM on his call 2026-07-28): ALL modes are
// PER-SESSION, keyed on env CLAUDE_CODE_SESSION_ID — a chip declared in one terminal must never
// leak into another. Even the token-budget chips scope per session: the SHARED FACT (weekly account
// headroom) lives in `tt limit`'s machine store; the CHIP is this session's spend POLICY, and
// policy differs per session under one wallet (line-1-measured / line-2-declared). Without a
// session id (a bare shell), everything falls back to the global file (~/.claude/gs-modes),
// exactly the pre-scoping behavior; chips left there render in every session until removed.
// Labels are bare tokens [A-Za-z0-9._-]+ (no spaces, no paths) so they render cleanly and pass around safely.
import java.nio.file.Path

private val ModeHelp: String =
  """tt mode — record the declared modes of the joint state-of-mind (v0.10.0)
    |
    |A mode is a label on the shared human<->agent state; many can be active at once, and both the human
    |and the agent may declare or clear them. Declaring = adding a label to the recorded state; the
    |statusline's mode line renders whatever is active. This tool owns only the state.
    |
    |Usage:
    |  mode                   list the active modes (session chips + machine chips, one per line)
    |  mode add <label>       declare <label> active (idempotent)
    |  mode rm <label>        clear <label> (from whichever store holds it)
    |  mode clear             clear this SESSION's modes (in a bare shell with no session id,
    |                         clears the global file)
    |  mode --file <f> ...    single-file mode on <f>, no session scoping (config-in-args, for tests)
    |  mode --sessions-root <d> ...  override the session-store root (for tests)
    |  mode --global-file <g> ...    override the machine store, scoping stays active (for tests)
    |  mode --id <id> ...     fix the session id (for tests; default env CLAUDE_CODE_SESSION_ID)
    |  mode --cwd <d> ...     override the working directory used for orphan matching (for tests)
    |
    |SCOPING (SM208): ALL modes are PER-SESSION, keyed on the harness session id
    |(env CLAUDE_CODE_SESSION_ID), so parallel sessions cannot flip each other's chips — including
    |the token-budget chips: the shared FACT (account headroom) lives in `tt limit`, the CHIP is
    |this session's spend policy. No session id (a bare shell) -> the global file
    |(~/.claude/gs-modes), as before; chips left there show in every session until removed.
    |Session state: ~/.claude/gs-sessions/<id>/.
    |
    |If a list finds NO state under this session's key while recent (<48h) orphaned state for the
    |SAME directory exists (the harness re-minted the session id, e.g. a bg/fg round trip), ONE
    |hint line goes to stderr; stdout is unchanged. Recover with `tt session adopt`.
    |
    |Labels are bare tokens [A-Za-z0-9._-]+ (no spaces / paths). Examples of modes:
    |  TokSpend  TokenSaving  HotHarvest  HighContext  Solo  HumanStress  RotVigil  Racing
    |  ColdStart  SmartZone  (the warp ember declares -RotVigil +ColdStart +SmartZone upon a warp)
    |Labels are CamelCase so they map 1:1 onto the planned `enum ModeChips` case names.
    |
    |Examples:
    |  tt mode add HotHarvest        # agent or human declares HotHarvest mode (this session only)
    |  tt mode rm HotHarvest         # clear it
    |  tt mode                       # list what is active here
    |
    |The session NAME is a separate field with its own verb: see `tt session`.
    |Full reference: tools/README.md""".stripMargin

private def defaultStateFile(): Path =
  Path.of(sys.props.getOrElse("user.home", "."), ".claude", "gs-modes")

@main def mode(args: String*): Unit =
  if args.contains("--help") || args.contains("-h") then { println(ModeHelp); sys.exit(0) }
  val a = args.toList
  def flagVal(name: String): Option[String] =
    val i = a.indexOf(name); if i >= 0 && i + 1 < a.size then Some(a(i + 1)) else None
  val fileOverride = flagVal("--file").map(Path.of(_))
  val sessionsRoot = flagVal("--sessions-root").map(Path.of(_)).getOrElse(SessionStore.defaultRoot)
  val globalFile   = fileOverride
    .orElse(flagVal("--global-file").map(Path.of(_)))
    .getOrElse(defaultStateFile())
  // --file = single-file mode with NO scoping: the pre-SM208 behavior, kept as the test surface and
  // the explicit escape hatch. --global-file overrides only the MACHINE store while scoping stays
  // active, and --id fixes the session id (both config-in-args, for tests). Session scoping engages
  // on the default store with a live id.
  val sid: Option[String] =
    if fileOverride.isDefined then None
    else flagVal("--id").filter(_.matches("[A-Za-z0-9-]+")).orElse(SessionStore.sessionId)
  val cwd = flagVal("--cwd").getOrElse(sys.props.getOrElse("user.dir", "."))
  def readF(file: Path): Vector[String] = SessionStore.readChips(file)
  def writeF(file: Path, modes: Seq[String]): Unit = SessionStore.writeChips(file, modes)
  def valid(s: String): Boolean = s.matches("[A-Za-z0-9._-]+")
  val consumedIdx =
    List("--file", "--global-file", "--sessions-root", "--id", "--cwd").flatMap { n =>
      val i = a.indexOf(n); if i >= 0 then List(i, i + 1) else Nil
    }.toSet
  val rest = a.zipWithIndex.collect { case (t, i) if !consumedIdx(i) => t }
  def sessionModesFile: Option[Path] = sid.map(id => SessionStore.modesFile(sessionsRoot, id))
  rest match
    case Nil =>
      // Machine chips first (they are the shared truth), then this session's chips.
      val sessionChips = sessionModesFile.map(readF).getOrElse(Vector.empty)
      val cur = (readF(globalFile) ++ sessionChips).distinct
      // issue-023: when the key is FULLY empty but a recent same-directory orphan exists (the
      // harness re-minted the id), SessionStore.orphanHint yields ONE line — stderr only, so
      // stdout stays byte-identical for anything that parses the list.
      for id <- sid do
        SessionStore.orphanHint(sessionsRoot, id, cwd, System.currentTimeMillis())
          .foreach(Console.err.println)
      if cur.isEmpty then println("(no active modes)") else cur.foreach(println)
    case "clear" :: Nil =>
      sessionModesFile match
        case Some(f) => writeF(f, Seq.empty) // budget chips in the machine store deliberately survive
        case None    => writeF(globalFile, Seq.empty)
    case "add" :: label :: Nil =>
      if !valid(label) then
        Console.err.println(s"mode: invalid label '$label' (use bare [A-Za-z0-9._-], no spaces or paths)")
        sys.exit(2)
      val target = sessionModesFile match
        case Some(f) =>
          SessionStore.ensureStarted(sessionsRoot, sid.get, System.currentTimeMillis())
          SessionStore.ensureCwd(sessionsRoot, sid.get, cwd)
          SessionStore.prune(sessionsRoot, System.currentTimeMillis())
          f
        case None => globalFile
      val cur = readF(target)
      if !cur.contains(label) then writeF(target, cur :+ label)
    case "rm" :: label :: Nil =>
      // Remove from whichever store holds it — both is safe and answers the caller's intent.
      writeF(globalFile, readF(globalFile).filterNot(_ == label))
      sessionModesFile.foreach(f => writeF(f, readF(f).filterNot(_ == label)))
    case _ =>
      Console.err.println("mode: usage: tt mode [ add <label> | rm <label> | clear ]   (bare: list)")
      sys.exit(2)
