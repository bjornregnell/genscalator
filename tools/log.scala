//> using file project.scala
//> using jvm 21
//> using file lib.scala
//> using file ability.scala

// log — analyze a build/run log: count + show errors and warnings across common ecosystems
// (compiler/build, test runners, runtime leveled logs, CI, package managers, LaTeX). PURE: reads, computes, prints.
//   scala-cli run tools/log.scala -- [summary|errors|warnings] <file> [--error RE]... [--warn RE]... [--no-defaults] [--require-markers] [--cap N]
import agenttools.Lib

// Helpers (the default marker sets, hits/show, the Cfg type + parser, usage) scoped in this object so their
// generic names (usage/parse/hits/show) don't collide with other tools when the toolbox compiles together.
// Only the @main entry is top-level. See skills/scala-style.
object Log {

  /** What this verb is, declared ONCE and projected into every surface that describes it (issue 041).
    * All three of this verb's carriers kept the purity marker but reworded the description; the point
    * is that there is now nothing left to reword. */
  val ability: Ability.Decl = Ability.pure("log", "build/run-log analyzer")

  // Curated default markers — TARGETED so tally lines ("0 errors", "no warnings") don't false-positive.
  // Each compiles SEPARATELY, so a custom pattern's inline (?i) can't leak into these.
  private val defaultErr = Vector(
    """^! """,                                  // LaTeX error
    """(?i)\berror[\[:]""",                      // compiler/scala-cli/clang "error:" and rust "error[E0382]"
    """\[error\]""",                            // sbt/bloop
    """\bERROR\b""", """\bFATAL\b""", """\bCRITICAL\b""", // ALL-CAPS leveled logs
    """Exception\b""",                          // JVM
    """Traceback \(most recent call last\)""",  // Python
    """^panic:""",                              // Go
    """npm ERR!""",                             // npm
    """##\[error\]""",                          // GitHub Actions annotation
    """\bFAIL(?:ED|URE)?\b""",                  // test runners / CI (ALL-CAPS, so "0 failures" prose won't hit)
    """(?i)"?(?:level|severity)"?\s*[=:]\s*"?(?:error|fatal|crit)""" // logfmt / JSON structured logs
  )
  private val defaultWarn = Vector(
    """(?i)\bwarn(?:ing)?\b""",                 // warning:, [warn], WARN, npm WARN, level=warn (SINGULAR — "warnings" tally won't hit)
    """(?i)^(?:over|under)full""",              // LaTeX boxes
    """(?i)"?(?:level|severity)"?\s*[=:]\s*"?warn""" // structured logs
  )
  // Positive success/activity markers (issue-018): the mirror image of the tally-line rule above —
  // wherever a count appears it must be NON-ZERO ([1-9]\d*), so "0 passed" / "Passed 0" can't fake a pass.
  private val defaultSuccess = Vector(
    """\[success\]""",                          // sbt/bloop success tag
    """(?i)\bcompiling [1-9]\d* .*\bsources?\b""", // sbt "compiling 2 Scala sources" (non-zero: compiling nothing is not activity)
    """(?i)^\s*(?:\[info\]\s*)?compiled\b""",   // scala-cli "Compiled project", bloop "Compiled foo (123ms)" — line-anchored so prose "not compiled" won't hit
    """\bAll tests passed\b""",                 // scalatest
    """\b[1-9]\d* (?:tests? )?passed\b""",      // pytest "5 passed in 0.1s", generic "3 tests passed"
    """\bPassed [1-9]\d*\b""",                  // sbt test summary ", Passed 5" ("Passed: Total ..." has a colon; "Passed 0" won't hit)
    """^\s*(?:\[info\]\s*)?\+ .*\d\.\d+s$""",   // munit green per-test line "  + MySuite.test 0.01s" — duration-anchored so diff "+" lines won't hit
    """\bBUILD SUCCESS(?:FUL)?\b"""             // maven / gradle (ALL-CAPS phrase, so prose won't hit)
  )

  private def hits(text: String, res: Vector[scala.util.matching.Regex]): Vector[(Int, String)] =
    if res.isEmpty then Vector.empty
    else
      text.linesIterator.zipWithIndex
        .collect { case (l, i) if res.exists(_.findFirstIn(l).isDefined) => (i + 1, l.trim) }
        .toVector

  private def show(label: String, hs: Vector[(Int, String)], cap: Int): Unit =
    println(s"=== $label: ${hs.size}")
    for (n, l) <- hs.take(cap) do println(f"$n%6d: ${l.take(140)}")
    if hs.size > cap then println(s"  … ${hs.size - cap} more (use --cap N or read the file directly)")

  private case class Cfg(cmd: String, file: String, errs: Vector[String], warns: Vector[String], noDefaults: Boolean, requireMarkers: Boolean, cap: Int)

  private def parse(args: List[String]): Either[String, Cfg] =
    @annotation.tailrec
    def go(rest: List[String], pos: Vector[String], es: Vector[String], ws: Vector[String], nd: Boolean, rm: Boolean, cap: Int): Either[String, Cfg] =
      rest match
        case Nil =>
          pos.toList match
            case file :: Nil => Right(Cfg("summary", file, es, ws, nd, rm, cap))
            case cmd :: file :: Nil if Set("summary", "errors", "warnings")(cmd) => Right(Cfg(cmd, file, es, ws, nd, rm, cap))
            case _ => Left("expected: [summary|errors|warnings] <file>")
        case "--error" :: re :: t => go(t, pos, es :+ re, ws, nd, rm, cap)
        case "--warn" :: re :: t  => go(t, pos, es, ws :+ re, nd, rm, cap)
        case "--no-defaults" :: t => go(t, pos, es, ws, true, rm, cap)
        case "--require-markers" :: t => go(t, pos, es, ws, nd, true, cap)
        case "--cap" :: n :: t =>
          n.toIntOption match
            case Some(v) if v >= 0 => go(t, pos, es, ws, nd, rm, v)
            case _ => Left(s"--cap needs a non-negative integer, got '$n'")
        case ("--error" | "--warn" | "--cap") :: Nil => Left("a flag is missing its argument")
        case other :: t => go(t, pos :+ other, es, ws, nd, rm, cap)
    go(args, Vector.empty, Vector.empty, Vector.empty, false, false, 50)

  private val Help: String =
    ability.tagline + """
      |
      |Counts and shows errors and warnings in a log file, with a one-line verdict. The summary
      |also counts recognised SUCCESS markers (sbt [success], compiling/compiled, tests-passed
      |lines, BUILD SUCCESS), so a genuinely clean run is distinguishable from a file with no
      |log markers at all. Curated default markers cover compiler/build output, test runners
      |and CI, runtime leveled logs (plain, logfmt, JSON), Python tracebacks, Go panics, npm,
      |and LaTeX — targeted so tally lines like "0 errors", "no warnings" or "0 passed" don't
      |false-positive. Extend or replace them per log.
      |
      |Usage:
      |  log <file>                     summary (default): counts + lines + verdict
      |  log summary <file>             same, explicit
      |  log errors <file>              only the error bucket
      |  log warnings <file>            only the warning bucket
      |
      |Flags:
      |  --error <regex>                add an error pattern (repeatable)
      |  --warn <regex>                 add a warning pattern (repeatable)
      |  --no-defaults                  use ONLY the supplied patterns (skip the curated markers)
      |  --require-markers              exit 1 when NO markers of any kind (error, warning, success)
      |                                 are found — lets an unattended run gate on a truncated,
      |                                 empty, or non-log input instead of taking it as clean
      |  --cap <n>                      max lines shown per bucket (default 50)
      |
      |Notes:
      |  Each pattern compiles separately, so an inline (?i) in one cannot leak into the others.
      |  The file is read as Latin-1 (some logs, e.g. LaTeX, are not valid UTF-8).
      |
      |Examples:
      |  tt log build.log                                # curated defaults — the 90% case
      |  tt log errors run.log --cap 200                 # just errors, show more lines
      |  tt log app.log --error 'MYAPP-FATAL'            # defaults plus my app's own marker
      |  tt log weird.log --no-defaults --error 'BOOM:'  # only my pattern
      |  tt log build.log --require-markers              # gate: exit 1 if nothing was recognised
      |
      |Full reference: tools/README.md""".stripMargin

  private def usage(): Unit =
    println(ability.tagline + """
      |  log [summary|errors|warnings] <file>   summary = counts + lines + verdict (default)
      |  --error <regex>   add an error pattern   (repeatable)
      |  --warn  <regex>   add a warning pattern  (repeatable)
      |  --no-defaults     use ONLY supplied patterns (skip the curated markers)
      |  --require-markers exit 1 when no markers of ANY kind (error/warning/success) are found
      |  --cap <n>         max lines shown per bucket (default 50)
      |curated markers span compiler/build, test runners, runtime leveled logs, CI, npm, LaTeX,
      |plus success markers ([success], compiled, tests passed, BUILD SUCCESS) — targeted so
      |"0 errors"/"no warnings"/"0 passed" tallies don't false-positive.""".stripMargin)

  def dispatch(args: String*): Unit =
    if args.contains("--help") || args.contains("-h") then { println(Help); sys.exit(0) }
    parse(args.toList) match
      case Left(msg) =>
        System.err.println(s"log: $msg")
        usage()
        sys.exit(2)
      case Right(cfg) =>
        val errRes = (if cfg.noDefaults then Vector.empty else defaultErr) ++ cfg.errs
        val warnRes = (if cfg.noDefaults then Vector.empty else defaultWarn) ++ cfg.warns
        if errRes.isEmpty && warnRes.isEmpty then
          System.err.println("log: --no-defaults given but no --error/--warn patterns — nothing to match")
          sys.exit(2)
        val path = java.nio.file.Path.of(cfg.file)
        if !java.nio.file.Files.isRegularFile(path) then
          System.err.println(s"log: not a readable file: ${cfg.file} (resolved: ${path.toAbsolutePath})")
          if java.nio.file.Files.isDirectory(path) then // issue-020: the name invites a git misreach on repo dirs
            System.err.println("for git history see: tt gitinfo <repo>  (tt log analyzes build/run LOG FILES)")
          sys.exit(2)
        val text = Lib.readLatin1(cfg.file) // Latin-1: logs (e.g. LaTeX) aren't always valid UTF-8; ASCII markers unaffected
        val succRes = if cfg.noDefaults then Vector.empty else defaultSuccess
        val errs = hits(text, errRes.map(_.r))
        val warns = hits(text, warnRes.map(_.r))
        val succs = hits(text, succRes.map(_.r))
        cfg.cmd match
          case "errors"   => show("errors", errs, cfg.cap)
          case "warnings" => show("warnings", warns, cfg.cap)
          case _ =>
            show("errors", errs, cfg.cap)
            show("warnings", warns, cfg.cap)
            println(s"=== success markers: ${succs.size}")
            // The verdict must not report the ABSENCE of bad news as good news (issue-018): an empty
            // file and a marker-less file are called out, so they cannot masquerade as a clean run,
            // while success markers upgrade a zero-hit run to genuinely clean-looking.
            // Both zero-hit verdicts keep the additive "0 errors, 0 warnings" counts substring (tests pin it).
            val lines = text.linesIterator.size
            if text.isEmpty then
              println("=== verdict: EMPTY input (0 bytes) — nothing was scanned, this is not a clean run")
            else if errs.isEmpty && warns.isEmpty && succs.nonEmpty then
              println(s"=== verdict: 0 errors, 0 warnings, ${succs.size} success markers ($lines lines scanned)")
            else if errs.isEmpty && warns.isEmpty then
              println(s"=== verdict: 0 errors, 0 warnings — but no log markers recognised in $lines lines (is this a log?)")
            else
              println(s"=== verdict: ${errs.size} errors, ${warns.size} warnings ($lines lines scanned)")
        // --require-markers: gate for unattended runs — a truncated/empty/non-log input has no markers
        // of ANY kind and must not pass silently. Applies to all subcommands (empty input included).
        if cfg.requireMarkers && errs.isEmpty && warns.isEmpty && succs.isEmpty then
          sys.exit(1)
}

@main def logAnalyze(args: String*): Unit = Log.dispatch(args*)
