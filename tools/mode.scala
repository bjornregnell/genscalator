//> using scala 3.8.4
//> using jvm 21

// mode — record the DECLARED modes of the joint human<->agent state-of-mind (v0.10.0). A "mode" is a label
// stuck on the shared state; MANY can be active at once, and BOTH the human and the agent may declare or clear
// them — it is a joint, mutually-visible channel. Declaring a mode = adding a label to the recorded state
// (a small state file); the statusline's mode line renders whatever is active here. This tool owns ONLY the
// state (add/remove/list); rendering it lives in `statusline`.
//   tt mode                   list the active modes (one per line)
//   tt mode add <label>       declare <label> active (idempotent)
//   tt mode rm <label>        clear <label>
//   tt mode clear             clear all modes
//   tt mode --file <f> ...    override the state file (default ~/.claude/gs-modes; config-in-ARGS, for tests)
// State: one label per line in ~/.claude/gs-modes (created on first `on`, order-preserving, de-duplicated).
// Labels are bare tokens [A-Za-z0-9._-]+ (no spaces, no paths) so they render cleanly and pass around safely.
import java.nio.file.{Files, Path}

private val ModeHelp: String =
  """tt mode — record the declared modes of the joint state-of-mind (v0.10.0)
    |
    |A mode is a label on the shared human<->agent state; many can be active at once, and both the human
    |and the agent may declare or clear them. Declaring = adding a label to the recorded state; the
    |statusline's mode line renders whatever is active. This tool owns only the state.
    |
    |Usage:
    |  mode                   list the active modes (one per line)
    |  mode add <label>       declare <label> active (idempotent)
    |  mode rm <label>        clear <label>
    |  mode clear             clear all modes
    |  mode --file <f> ...    override the state file (default ~/.claude/gs-modes)
    |
    |Labels are bare tokens [A-Za-z0-9._-]+ (no spaces / paths). Examples of modes:
    |  TokSpend  TokenSaving  HotHarvest  HighContext  Solo  HumanStress  RotVigil  Racing
    |  ColdStart  SmartZone  (the baton declares -RotVigil +ColdStart +SmartZone upon a warp)
    |Labels are CamelCase so they map 1:1 onto the planned `enum ModeChips` case names.
    |
    |Examples:
    |  tt mode add hot-harvest       # agent or human declares hot-harvest mode
    |  tt mode rm hot-harvest        # clear it
    |  tt mode                       # list what is active
    |
    |Full reference: tools/README.md""".stripMargin

private def defaultStateFile(): Path =
  Path.of(sys.props.getOrElse("user.home", "."), ".claude", "gs-modes")

@main def mode(args: String*): Unit =
  if args.contains("--help") || args.contains("-h") then { println(ModeHelp); sys.exit(0) }
  val a = args.toList
  val fileIdx = a.indexOf("--file")
  val file: Path =
    if fileIdx >= 0 && fileIdx + 1 < a.size then Path.of(a(fileIdx + 1)) else defaultStateFile()
  def read(): Vector[String] =
    if Files.isRegularFile(file) then
      String(Files.readAllBytes(file), "UTF-8").linesIterator.map(_.trim).filter(_.nonEmpty).toVector.distinct
    else Vector.empty
  def write(modes: Seq[String]): Unit =
    Option(file.getParent).foreach(Files.createDirectories(_))
    val body = if modes.isEmpty then "" else modes.mkString("\n") + "\n"
    Files.write(file, body.getBytes("UTF-8"))
  def valid(s: String): Boolean = s.matches("[A-Za-z0-9._-]+")
  // positional args = everything except the --file flag and its value
  val consumed = if fileIdx >= 0 then Set(fileIdx, fileIdx + 1) else Set.empty[Int]
  val rest = a.zipWithIndex.collect { case (t, i) if !consumed(i) => t }
  rest match
    case Nil =>
      val cur = read()
      if cur.isEmpty then println("(no active modes)") else cur.foreach(println)
    case "clear" :: Nil =>
      write(Seq.empty)
    case "add" :: label :: Nil =>
      if !valid(label) then
        Console.err.println(s"mode: invalid label '$label' (use bare [A-Za-z0-9._-], no spaces or paths)")
        sys.exit(2)
      val cur = read()
      if !cur.contains(label) then write(cur :+ label)
    case "rm" :: label :: Nil =>
      write(read().filterNot(_ == label))
    case _ =>
      Console.err.println("mode: usage: tt mode [ add <label> | rm <label> | clear ]   (bare: list)")
      sys.exit(2)
