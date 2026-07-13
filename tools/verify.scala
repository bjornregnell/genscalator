//> using scala 3.8.4
//> using jvm 21
//> using dep com.lihaoyi::os-lib:0.11.8

// verify — run-and-verify driver (EFFECTFUL): run an ALLOWED command (NO shell), capture exit/stdout/stderr,
// check against expectations, print an audit line + a PASS/FAIL verdict. Replaces the
// `cd && … > log 2>&1; echo $?` bundle with ONE allowlistable call.
//
// SAFE BY DESIGN: runs the command directly as argv (no shell → `;`, `|`, `&&`, `$()`, globs are inert
// literal args), and only executables on the allowlist. Allowed = { scala-cli, tt, scalex } plus any in
// the HUMAN-set env var TT_VERIFY_ALLOW (comma-separated). The agent CANNOT widen this via a flag — that
// is the whole point; a flag would be agent-authored, not human approval.
//   scala-cli run tools/verify.scala -- [checks] -- <cmd> <args...>
//   checks: --exit N | --out S | --out-re R | --err S | --err-re R   (combinable; ALL must pass; default --exit 0)

// Helpers (parseChecks/allowed/basename, the allow-set + Checks type) scoped in this object so their generic
// names don't collide with other tools when the toolbox compiles together. Only the @main entry is top-level.
object Verify {
  private val builtinAllow = Set("scala-cli", "tt", "scalex")

  private case class Checks(exit: Int, out: Vector[String], outRe: Vector[String], err: Vector[String], errRe: Vector[String])

  private def parseChecks(args: List[String]): Either[String, Checks] =
    @annotation.tailrec
    def go(rest: List[String], c: Checks): Either[String, Checks] =
      rest match
        case Nil => Right(c)
        case "--exit" :: n :: t =>
          n.toIntOption match
            case Some(v) => go(t, c.copy(exit = v))
            case None    => Left(s"--exit needs an integer, got '$n'")
        case "--out" :: s :: t    => go(t, c.copy(out = c.out :+ s))
        case "--out-re" :: r :: t => go(t, c.copy(outRe = c.outRe :+ r))
        case "--err" :: s :: t    => go(t, c.copy(err = c.err :+ s))
        case "--err-re" :: r :: t => go(t, c.copy(errRe = c.errRe :+ r))
        case flag :: Nil if flag.startsWith("--") => Left(s"$flag needs an argument")
        case other :: _ => Left(s"unknown check '$other' (expected --exit/--out/--out-re/--err/--err-re)")
    go(args, Checks(0, Vector.empty, Vector.empty, Vector.empty, Vector.empty))

  private def allowed: Set[String] =
    val extra = sys.env.getOrElse("TT_VERIFY_ALLOW", "").split(",").iterator.map(_.trim).filter(_.nonEmpty).toSet
    builtinAllow ++ extra

  private def basename(s: String): String = s.split('/').filter(_.nonEmpty).lastOption.getOrElse(s)

  private val Help: String =
    """tt verify — run-and-verify driver: run an allowed command, check its output, print PASS/FAIL
      |
      |Runs ONE allowed executable directly as argv (no shell — ; | && $() and globs are inert
      |literal args), captures exit/stdout/stderr, checks them against your expectations, and prints
      |an audit line (argv, exit, ms) plus a PASS/FAIL verdict. Replaces the
      |'cd && ... > log 2>&1; echo $?' bundle with one allowlistable call.
      |
      |Usage:
      |  verify [checks] -- <cmd> <args...>   everything after the FIRST -- is the command to run
      |                                       (a --help/-h AFTER the -- belongs to that command,
      |                                       not to verify)
      |
      |Checks (combinable; ALL must pass; default: --exit 0):
      |  --exit N                        expected exit code (default 0)
      |  --out <substr>                  stdout must contain this substring (repeatable)
      |  --out-re <regex>                stdout must match this regex (repeatable)
      |  --err <substr>                  stderr must contain this substring (repeatable)
      |  --err-re <regex>                stderr must match this regex (repeatable)
      |
      |Allowed executables: scala-cli, tt, scalex — plus any listed in the HUMAN-set env var
      |TT_VERIFY_ALLOW (comma-separated, e.g. export TT_VERIFY_ALLOW=git,make). There is
      |deliberately NO flag to widen this: a flag would be agent-authored, not human approval.
      |
      |Exit: 0 all checks pass, 1 a check failed, 2 usage / disallowed executable / spawn error.
      |
      |Examples:
      |  tt verify -- tt files /abs/src .scala --count
      |  tt verify --exit 0 --out 8 -- scala-cli run tools/text.scala -- grepr /abs .scala x --count
      |  tt verify --exit 2 --out usage -- tt chrono bogus    # assert the failure mode too
      |
      |Full reference: tools/README.md""".stripMargin

  def dispatch(args: String*): Unit =
    // split "[checks] -- <cmd...>" at the FIRST "--"
    val (checkArgs, cmd) = args.toList.span(_ != "--") match
      case (before, _ :: rest) => (before, rest) // "--" present → drop it
      case (before, Nil)       => (before, Nil)  // no "--"
    // help ONLY when asked among verify's OWN args (before the --); after the -- it belongs to <cmd>
    if checkArgs.contains("--help") || checkArgs.contains("-h") then
      println(Help)
      sys.exit(0)
    if cmd.isEmpty then
      System.err.println("verify: usage: verify [checks] -- <cmd> <args...>   (checks: --exit N | --out S | --out-re R | --err S | --err-re R)")
      sys.exit(2)

    val exe = basename(cmd.head)
    if !allowed.contains(exe) then
      System.err.println(
        s"verify: '$exe' is not an allowed executable. Allowed: ${allowed.toVector.sorted.mkString(", ")}.\n" +
          "  To allow more, the HUMAN sets the env var (e.g. export TT_VERIFY_ALLOW=git,make) — not via a flag."
      )
      sys.exit(2)

    parseChecks(checkArgs) match
      case Left(msg) =>
        System.err.println(s"verify: $msg")
        sys.exit(2)
      case Right(checks) =>
        val t0 = System.nanoTime()
        val result =
          try os.proc(cmd).call(check = false, stdout = os.Pipe, stderr = os.Pipe, cwd = os.pwd)
          catch
            case e: Throwable =>
              System.err.println(s"verify: failed to run '${cmd.mkString(" ")}': ${e.getMessage}")
              sys.exit(2)
        val ms = (System.nanoTime() - t0) / 1000000
        val out = result.out.text()
        val err = result.err.text()
        println(s"=== ran: ${cmd.mkString(" ")} (exit ${result.exitCode}, $ms ms)")

        val fails: Vector[String] =
          (if result.exitCode != checks.exit then Vector(s"exit: expected ${checks.exit}, got ${result.exitCode}") else Vector.empty) ++
            checks.out.filterNot(out.contains).map(s => s"""stdout missing "$s"""") ++
            checks.outRe.filterNot(r => r.r.findFirstIn(out).isDefined).map(r => s"stdout doesn't match /$r/") ++
            checks.err.filterNot(err.contains).map(s => s"""stderr missing "$s"""") ++
            checks.errRe.filterNot(r => r.r.findFirstIn(err).isDefined).map(r => s"stderr doesn't match /$r/")

        if fails.isEmpty then println("=== PASS")
        else
          fails.foreach(f => println(s"  ✗ $f"))
          val combined = (out + (if err.nonEmpty then "\n--- stderr ---\n" + err else "")).linesIterator.toVector
          if combined.nonEmpty then
            println("--- last output ---")
            combined.takeRight(20).foreach(println)
          println(s"=== FAIL (${fails.size})")
          sys.exit(1)
}

@main def verifyCommand(args: String*): Unit = Verify.dispatch(args*)
