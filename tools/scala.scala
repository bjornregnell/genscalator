//> using scala 3.8.4
//> using jvm 21
//> using dep com.lihaoyi::os-lib:0.11.8

// scala — a typed driver over `scala-cli` for a Scala PROJECT DIRECTORY (EFFECTFUL: runs scala-cli as a
// subprocess). This is what lets a blanket `Bash(scala-cli *)` allow come OUT of settings (SM205).
//
// SAFE BY DESIGN, and honest about its limit. The verb is a FIXED enum, the target is a DIRECTORY (never
// `-e` inline eval, never an arbitrary script path), the argv is built HERE from the verb + validated dir +
// baked safe defaults (`--server=false`, the SM191 no-bloop path) with NO passthrough of arbitrary scala-cli
// flags, and there is no shell (argv via os.proc → ; | && $() globs are inert). It does NOT make running code
// safe — running a project's tests or `@main` IS arbitrary code execution (see SECURITY-MODEL, "When the
// tool's job is to run code"); it removes the SURPLUS a broad allow grants, so each verb is per-verb
// allowlistable (`Bash(tt scala test *)`) while bare `scala-cli` stays off the allowlist.
//
//   tt scala test <dir> [--prop k=v]...          scala-cli test <dir>     (exit 0 = green)
//   tt scala compile <dir> [--prop k=v]...        scala-cli compile <dir>
//   tt scala run <dir> [--prop k=v]...            scala-cli run <dir>      (runs the project @main; tightest)
//   tt scala package-js <dir> -o <out> [--prop k=v]...   link Scala.js to <out>
//
// The pure core `plan` validates args and builds the exact scala-cli argv with NO filesystem or process
// effect, so the safety-critical construction (no eval, no passthrough) is unit-tested; the effectful
// `dispatch` does the dir-exists check and runs it.

object ScalaTool {

  final case class Plan(argv: Seq[String], dir: String)

  private val Verbs = Set("test", "compile", "run", "package-js")

  val Help: String =
    """tt scala — typed driver over scala-cli for a Scala project DIRECTORY
      |
      |Runs scala-cli on a validated directory with no shell and no arbitrary-flag passthrough, so a
      |narrow `Bash(tt scala test *)` can replace a blanket `Bash(scala-cli *)` allow. It does not make
      |running code safe (tests and @main run real code) — it removes the surplus a broad allow grants.
      |
      |Usage:
      |  tt scala test <dir> [--prop k=v]...                run the test suite      (exit 0 = green)
      |  tt scala compile <dir> [--prop k=v]...             compile only
      |  tt scala run <dir> [--prop k=v]...                 run the project @main    (tightest verb)
      |  tt scala package-js <dir> -o <out> [--prop k=v]... link Scala.js to <out>
      |
      |Options:
      |  --prop k=v     pass a JVM system property (becomes --java-prop k=v; repeatable). Handy for the
      |                 toolbox suite: --prop tt.tools=<abs-tools-dir>.
      |  -o <out>       output path (package-js only, required).
      |
      |Baked in every call: --server=false (no bloop daemon). No inline `-e` eval and no script path are
      |accepted — the target is always a directory.
      |
      |Exit: passes scala-cli's exit code through (0 = success); 2 = usage / bad directory.""".stripMargin

  /** PURE: validate args and build the scala-cli argv. No filesystem, no process. */
  def plan(args: Seq[String]): Either[String, Plan] =
    args.toList match
      case Nil => Left("usage: tt scala <test|compile|run|package-js> <dir> [options]  (tt scala --help)")
      case verb :: _ if !Verbs.contains(verb) =>
        Left(s"unknown verb '$verb' (expected: ${Verbs.toVector.sorted.mkString(", ")})")
      case _ :: Nil => Left("missing <dir>")
      case verb :: dir :: opts =>
        if dir.startsWith("-") then
          Left(s"<dir> must not start with '-' (got '$dir'); it is a directory, not a flag")
        else parseOpts(opts).flatMap: (props, out) =>
          val propFlags = props.flatMap(kv => Seq("--java-prop", kv))
          verb match
            case "package-js" =>
              out match
                case Some(o) if o.startsWith("-") => Left(s"-o path must not start with '-' (got '$o')")
                case Some(o) =>
                  Right(Plan(Seq("scala-cli", "--power", "package", dir, "--js", "-o", o, "-f", "--server=false") ++ propFlags, dir))
                case None => Left("package-js needs -o <out>")
            case _ if out.isDefined => Left(s"-o is only valid for package-js, not '$verb'")
            case other => Right(Plan(Seq("scala-cli", other, dir, "--server=false") ++ propFlags, dir))

  private def parseOpts(opts: List[String]): Either[String, (Vector[String], Option[String])] =
    @annotation.tailrec
    def go(rest: List[String], props: Vector[String], out: Option[String]): Either[String, (Vector[String], Option[String])] =
      rest match
        case Nil => Right((props, out))
        case "--prop" :: kv :: t =>
          if kv.contains("=") then go(t, props :+ kv, out) else Left(s"--prop needs k=v, got '$kv'")
        case "--prop" :: Nil => Left("--prop needs a k=v argument")
        case "-o" :: p :: t  => go(t, props, Some(p))
        case "-o" :: Nil     => Left("-o needs a path argument")
        case other :: _      => Left(s"unknown option '$other' (only --prop and -o are accepted)")
    go(opts, Vector.empty, None)

  def dispatch(args: String*): Unit =
    if args.contains("--help") || args.contains("-h") then
      println(Help); sys.exit(0)
    plan(args) match
      case Left(msg) =>
        System.err.println(s"tt scala: $msg")
        sys.exit(2)
      case Right(p) =>
        val dirPath =
          try os.Path(p.dir, os.pwd)
          catch case _: Throwable => os.pwd / p.dir
        if !os.exists(dirPath) || !os.isDir(dirPath) then
          System.err.println(s"tt scala: not a directory: ${p.dir}")
          sys.exit(2)
        val t0 = System.nanoTime()
        val result =
          try os.proc(p.argv).call(check = false, stdout = os.Inherit, stderr = os.Inherit, cwd = os.pwd)
          catch
            case e: Throwable =>
              System.err.println(s"tt scala: failed to run scala-cli: ${e.getMessage}")
              sys.exit(2)
        val ms = (System.nanoTime() - t0) / 1000000
        println(s"=== tt scala: ${p.argv.mkString(" ")} (exit ${result.exitCode}, $ms ms)")
        sys.exit(result.exitCode)
}

@main def scalaProjectDriver(args: String*): Unit = ScalaTool.dispatch(args*)
