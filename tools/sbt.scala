//> using file project.scala
//> using file lib.scala
//> using jvm 21
//> using dep com.lihaoyi::os-lib:0.11.8

// sbt — run `sbt` in an EXPLICIT directory (EFFECTFUL: runs sbt as a subprocess). This is the
// dir-scoped runner that closes a real dead-end: the guardcheck hook blocks even a bare `cd`
// (correctly — it prevents cd-then-chain), and sbt has no `-C`-style flag, so an agent whose cwd is
// pinned to the session repo could not build an sbt project living somewhere else.
//
// The fix mirrors what `tt git --repo <dir>` already does for git: the working directory becomes a
// TYPED ARGUMENT set via ProcessBuilder.directory() (os-lib's `cwd`), not a shell `cd`. There is no
// shell anywhere in the path — argv goes straight to ProcessBuilder, so ; | && $() globs are inert.
//
//   tt sbt --dir <abs-dir> [sbt-args...]     run sbt in <abs-dir>, e.g. --client pdfCompendiumEn
//
// SPECIFIC, NOT GENERAL, on purpose (SM226 ratified): a `tt run --dir <abs> -- <any-cmd>` would BE
// the run-anything-anywhere escape hatch the guard exists to resist. This tool names one program.
//
// Honest about its limit, exactly like `tt scala`: sbt runs a project's own build code, so this is
// arbitrary code execution and stays SHOWN-GATED — no blanket `Bash(tt sbt *)` allow belongs in
// settings. What the tool removes is the surplus: the dir is validated and explicit, the program is
// fixed, and the call is reviewable in one line.
//
// The pure core `plan` validates args and builds the argv with NO filesystem or process effect; the
// effectful `dispatch` checks the directory really is an sbt build and runs it there.

object SbtTool {

  final case class Plan(dir: String, argv: Seq[String])

  val Help: String =
    """tt sbt — run sbt in an explicit directory (no shell cd)
      |
      |The working directory is a typed argument set via ProcessBuilder.directory(), never a shell
      |`cd`. No shell is involved, so shell metacharacters in arguments are inert.
      |
      |Usage:
      |  tt sbt --dir <abs-dir> [sbt-args...]
      |
      |Examples:
      |  tt sbt --dir /home/me/proj compile
      |  tt sbt --dir /home/me/proj --client pdfCompendiumEn
      |
      |Rules:
      |  --dir must come FIRST and must be an ABSOLUTE path to an existing sbt build (a directory
      |  holding build.sbt or project/). Everything after it is passed through to sbt untouched.
      |
      |Running sbt runs the project's own build code — keep this shown-gated, not allowlisted.
      |
      |Exit: passes sbt's exit code through (0 = success); 2 = usage / bad directory.""".stripMargin

  /** PURE: validate args and build the sbt argv. No filesystem, no process. */
  def plan(args: Seq[String]): Either[String, Plan] =
    args.toList match
      case Nil            => Left("usage: tt sbt --dir <abs-dir> [sbt-args...]  (tt sbt --help)")
      case "--dir" :: Nil => Left("--dir needs an absolute directory argument")
      case "--dir" :: dir :: rest =>
        if !agenttools.Lib.isAbsolutePath(dir) then
          Left(s"--dir must be an ABSOLUTE path (got '$dir'); the working directory is explicit by design")
        else Right(Plan(dir, "sbt" +: rest))
      case first :: _ =>
        Left(s"first argument must be --dir (got '$first'); the working directory is explicit by design")

  def dispatch(args: String*): Unit =
    if args.contains("--help") || args.contains("-h") then
      println(Help); sys.exit(0)
    plan(args) match
      case Left(msg) =>
        System.err.println(s"tt sbt: $msg")
        sys.exit(2)
      case Right(p) =>
        val dirPath = os.Path(p.dir)
        if !os.exists(dirPath) || !os.isDir(dirPath) then
          System.err.println(s"tt sbt: not a directory: ${p.dir}")
          sys.exit(2)
        if !os.exists(dirPath / "build.sbt") && !os.isDir(dirPath / "project") then
          System.err.println(s"tt sbt: not an sbt build (no build.sbt and no project/): ${p.dir}")
          sys.exit(2)
        val t0 = System.nanoTime()
        val result =
          try os.proc(p.argv).call(check = false, stdout = os.Inherit, stderr = os.Inherit, cwd = dirPath)
          catch
            case e: Throwable =>
              System.err.println(s"tt sbt: failed to run sbt: ${e.getMessage}")
              sys.exit(2)
        val ms = (System.nanoTime() - t0) / 1000000
        println(s"=== tt sbt: ${p.argv.mkString(" ")} in ${p.dir} (exit ${result.exitCode}, $ms ms)")
        sys.exit(result.exitCode)
}

@main def sbtInDir(args: String*): Unit = SbtTool.dispatch(args*)
