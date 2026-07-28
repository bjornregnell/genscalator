//> using file project.scala
//> using file sessionstore.scala
//> using jvm 21

// session — name THIS session, so parallel sessions are tellable apart at a glance (SM208/SM259).
//   tt session                 print the display name: YYMMDD-HHhMMm[-MyName]
//   tt session <name words>    set the human name part (spaces allowed)
//   tt session --clear         remove the human name (the timestamp part always remains)
// The timestamp is ALWAYS present and FIRST (BR's format): the age signal survives naming, duplicate
// human names cannot collide, and the string is filesystem-safe by construction (no colon) — though
// the display name is NEVER a path component; the store is keyed on the opaque harness session id
// (env CLAUDE_CODE_SESSION_ID), which is useless as a name but perfect as a key. State lives in
// ~/.claude/gs-sessions/<id>/ via sessionstore.scala; the statusline renders the name inverted after
// the `gs session:` label. This tool owns only the NAME; chips live in `mode`.
import java.nio.file.Path

object SessionTool:
  val Help: String =
    """tt session — name THIS session, so parallel sessions are tellable apart (v0.10.0)
      |
      |Usage:
      |  session                     print the display name: YYMMDD-HHhMMm[-MyName]
      |  session <name words...>     set the human name part (free text, spaces allowed;
      |                              newlines/control characters rejected; max 120 chars)
      |  session --clear             remove the human name (the timestamp part remains)
      |  session --sessions-root <d> override the store root (config-in-args, for tests)
      |  session --id <id>           override the session id (for tests; default env CLAUDE_CODE_SESSION_ID)
      |  session --now-ms <ms>       fixed clock (for deterministic tests)
      |
      |The timestamp part is ALWAYS present and FIRST — the age signal survives naming and two
      |sessions named the same can never be confused. Outside a harness session (no session id)
      |there is nothing to name: the tool says so and exits 1.
      |
      |Examples:
      |  tt session alpha prep       # this session now renders as e.g. 260728-15h42m-alpha prep
      |  tt session                  # what is this session called?
      |  tt session --clear          # back to the bare timestamp
      |
      |Chips/modes are a separate field: see `tt mode`. Full reference: tools/README.md""".stripMargin

  def dispatch(args: List[String]): Int =
    if args.contains("--help") || args.contains("-h") then { println(Help); return 0 }
    def flagVal(name: String): Option[String] =
      val i = args.indexOf(name); if i >= 0 && i + 1 < args.size then Some(args(i + 1)) else None
    val root  = flagVal("--sessions-root").map(Path.of(_)).getOrElse(SessionStore.defaultRoot)
    val nowMs = flagVal("--now-ms").flatMap(_.toLongOption).getOrElse(System.currentTimeMillis())
    val sid   = flagVal("--id").orElse(SessionStore.sessionId)
    val consumed =
      List("--sessions-root", "--id", "--now-ms").flatMap { n =>
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
          case Nil =>
            val started = SessionStore.readStarted(root, id).getOrElse(nowMs)
            println(SessionStore.displayName(started, SessionStore.readName(root, id)))
            0
          case "--clear" :: Nil =>
            SessionStore.clearName(root, id)
            val started = SessionStore.readStarted(root, id).getOrElse(nowMs)
            println(SessionStore.displayName(started, None))
            0
          case words if words.nonEmpty && !words.exists(_.startsWith("--")) =>
            val name = words.mkString(" ").trim
            if !SessionStore.validName(name) then
              Console.err.println(
                "session: invalid name — free text is fine (spaces allowed) but control characters are not, max 120 chars")
              2
            else
              SessionStore.writeName(root, id, name, nowMs)
              SessionStore.prune(root, nowMs)
              val started = SessionStore.readStarted(root, id).getOrElse(nowMs)
              println(SessionStore.displayName(started, Some(name)))
              0
          case other =>
            Console.err.println(s"session: unexpected arguments '${other.mkString(" ")}' — see --help")
            2

@main def sessionName(args: String*): Unit = sys.exit(SessionTool.dispatch(args.toList))
