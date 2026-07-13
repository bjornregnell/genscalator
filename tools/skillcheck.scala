//> using scala 3.8.4
//> using jvm 21

// skillcheck — verify the genscalator SKILL SET is present, and catch the "silent skill outage" where a skill
// exists on disk but the harness never activated it (the plugin was not installed/enabled, or a fresh window
// has not loaded it). The agent CANNOT feel a missing skill — there is no phenomenology of absence, no error
// (WR: agent-has-no-phenomenology-of-absence, guardrail-skills-silently-inactive) — so the only defence is an
// EXPLICIT, checkable manifest. The expected set is DERIVED from disk (each skills/<name>/SKILL.md = one
// expected skill), so it never drifts from what the plugin actually ships. Pure, read-only.
//
//   tt skillcheck                       list the expected skill set (from skills/*/SKILL.md) + how to reconcile
//   tt skillcheck --active <a> <b> ...  diff expected vs the ACTIVE set (what /skills reports): warn (exit 1)
//                                       on any expected-but-NOT-active; also flag active-but-unexpected
//   tt skillcheck --skills <dir> ...    override the skills dir (default <tools>/../skills, via -Dtt.tools)
//
// The harness `/skills` list can only be produced by the harness, not a tool — so the session-start reflex is:
// run `tt skillcheck` to see what SHOULD be active, run `/skills`, and (optionally) feed the active names back
// as `--active ...` to get a machine-checked diff. Ties SM070.
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*

private val SkillcheckHelp: String =
  """tt skillcheck — verify the genscalator skill set is present (pure, read-only)
    |
    |An agent cannot FEEL a missing skill: an inactive skill is indistinguishable from the inside from an
    |active one it simply failed to apply. The only signal is behavioural regression, which the agent also
    |cannot reliably self-certify. So make the expected set an EXPLICIT manifest and check it. The expected
    |set is derived from disk (each skills/<name>/SKILL.md), so it never drifts from what the plugin ships.
    |
    |Usage:
    |  skillcheck                       list the expected skills + how to reconcile against /skills
    |  skillcheck --active <a> <b> ...  diff expected vs the ACTIVE set (what /skills reports); exit 1 if any
    |                                   expected skill is NOT active; also flags active-but-unexpected skills
    |  skillcheck --skills <dir> ...    override the skills dir (default <tools>/../skills, via -Dtt.tools)
    |
    |Session-start reflex (the harness /skills list can only come from the harness, not a tool):
    |  1. tt skillcheck            # what SHOULD be active
    |  2. /skills                  # what IS active (harness command)
    |  3. tt skillcheck --active <the names /skills listed>   # machine-checked diff; exit 1 = a silent outage
    |
    |Full reference: tools/README.md""".stripMargin

/** Locate the tools dir (cwd-independent): the -Dtt.tools property the `tt` launcher passes, else walk up
  * from the cwd for a `tools/tt`. Mirrors doc.scala + the test suite. */
private def toolsDir(): Option[Path] =
  sys.props.get("tt.tools").map(Path.of(_)).filter(d => Files.exists(d.resolve("tt")))
    .orElse:
      Iterator.iterate(Path.of("").toAbsolutePath)(p => p.getParent).takeWhile(_ != null).take(8)
        .find(d => Files.exists(d.resolve("tools").resolve("tt"))).map(_.resolve("tools"))

/** The expected skills = subdirs of skillsDir that directly contain a SKILL.md, sorted by name. */
private def expectedSkills(skillsDir: Path): Vector[String] =
  if !Files.isDirectory(skillsDir) then Vector.empty
  else
    Files.list(skillsDir).iterator.asScala
      .filter(Files.isDirectory(_))
      .filter(d => Files.isRegularFile(d.resolve("SKILL.md")))
      .map(_.getFileName.toString)
      .toVector.sorted

@main def skillcheck(args: String*): Unit =
  if args.contains("--help") || args.contains("-h") then { println(SkillcheckHelp); sys.exit(0) }
  val a = args.toList
  // consume --skills <dir> (flag + value) by index
  val skillsIdx = a.indexOf("--skills")
  val consumed = if skillsIdx >= 0 then Set(skillsIdx, skillsIdx + 1) else Set.empty[Int]
  val skillsDir: Path =
    (if skillsIdx >= 0 && skillsIdx + 1 < a.size then Some(Path.of(a(skillsIdx + 1))) else None)
      .orElse(toolsDir().map(_.getParent.resolve("skills")))
      .getOrElse:
        Console.err.println("skillcheck: cannot locate skills/ (pass --skills <dir>, or set -Dtt.tools=<dir>)")
        sys.exit(2)
  if !Files.isDirectory(skillsDir) then
    Console.err.println(s"skillcheck: not a skills directory: $skillsDir")
    sys.exit(2)
  val expected = expectedSkills(skillsDir)
  if expected.isEmpty then
    Console.err.println(s"skillcheck: no skills found under $skillsDir (each expected skill needs a SKILL.md)")
    sys.exit(2)

  val activeIdx = a.indexOf("--active")
  if activeIdx < 0 then
    // list mode: what SHOULD be active + the reconciliation reflex
    println(s"expected genscalator skills (${expected.size}, from $skillsDir):")
    expected.foreach(n => println(s"  $n"))
    println("")
    println("Reconcile: run /skills; every name above must be listed. A missing one = a SILENT skill outage")
    println("(the agent cannot feel it). Then: tt skillcheck --active <the names /skills listed>")
  else
    // diff mode: active = tokens after --active that are not flags and were not consumed by --skills
    val active =
      a.zipWithIndex.drop(activeIdx + 1)
        .collect { case (t, i) if !consumed(i) && !t.startsWith("--") => t }
        .toVector.distinct
    val missing = expected.filterNot(active.contains)
    val extra = active.filterNot(expected.contains)
    if missing.isEmpty then
      println(s"OK: all ${expected.size} expected genscalator skills are active.")
      if extra.nonEmpty then
        println(s"info: ${extra.size} active but not in the genscalator set: ${extra.mkString(", ")}")
      sys.exit(0)
    else
      Console.err.println(s"WARNING: ${missing.size} expected skill(s) NOT active — a silent skill outage:")
      missing.foreach(n => Console.err.println(s"  - $n"))
      if extra.nonEmpty then
        Console.err.println(s"info: ${extra.size} active but not expected: ${extra.mkString(", ")}")
      Console.err.println("Fix: install/enable the genscalator plugin (or /reload-plugins), then re-check.")
      sys.exit(1)
