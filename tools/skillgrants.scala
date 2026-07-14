//> using scala 3.8.4
//> using jvm 21

// skillgrants — print what a skill GRANTS: its `allowed-tools:` frontmatter, made legible for INFORMED CONSENT
// (SM100). When the harness loads a skill it silently widens the auto-approved tool set by that skill's
// allowed-tools — but the human is never shown, at grant time, WHICH tools a skill opens; they'd have to open
// the SKILL.md and read the YAML themselves. This tool is that read: name a skill (or list them all) and see
// the exact permissions it grants, one per line, so consent is informed rather than blind. Pure, read-only.
//
//   tt skillgrants                    audit the WHOLE skill set: every skill + its grants + the union
//   tt skillgrants <name>             show one skill's grants (e.g. tt skillgrants scala-style)
//   tt skillgrants --skills <dir>     override the skills dir (default <tools>/../skills, via -Dtt.tools)
//
// A skill with no `allowed-tools:` grants nothing beyond the session defaults — reported as such, not skipped,
// so "grants nothing" is a visible, checkable state rather than an absence you have to infer. Ties SM100/SM103.
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*

type SkillName = String
type Grant     = String // one allowed-tools entry, e.g. "Read" or "Bash(scala-cli run *)"

// @main name is descriptive + globally unique (the toolbox compiles as one unit); helpers live in the object
// so nothing (toolsDir, splitGrants, …) collides with skillcheck.scala / doc.scala at a whole-toolbox compile.
@main def printSkillGrants(args: String*): Unit = Skillgrants.dispatch(args)

object Skillgrants:
  val Help: String =
    """tt skillgrants — print what a skill GRANTS (its allowed-tools), for informed consent (pure, read-only)
      |
      |Loading a skill silently widens the auto-approved tool set by that skill's `allowed-tools:` — but the
      |human is never shown, at grant time, WHICH tools it opens. This tool is that read.
      |
      |Usage:
      |  skillgrants                    audit the WHOLE set: every skill, its grants, and the union across all
      |  skillgrants <name>             show one skill's grants (e.g. skillgrants scala-style)
      |  skillgrants --skills <dir>     override the skills dir (default <tools>/../skills, via -Dtt.tools)
      |
      |A skill with no allowed-tools grants nothing beyond session defaults — reported explicitly, not skipped.
      |
      |Full reference: tools/README.md""".stripMargin

  /** Locate the tools dir (cwd-independent): the -Dtt.tools property the `tt` launcher passes, else walk up
    * from the cwd for a `tools/tt`. Mirrors skillcheck.scala + doc.scala. */
  def toolsDir(): Option[Path] =
    sys.props.get("tt.tools").map(Path.of(_)).filter(d => Files.exists(d.resolve("tt")))
      .orElse:
        Iterator.iterate(Path.of("").toAbsolutePath)(p => p.getParent).takeWhile(_ != null).take(8)
          .find(d => Files.exists(d.resolve("tools").resolve("tt"))).map(_.resolve("tools"))

  /** Skills = subdirs of skillsDir that directly contain a SKILL.md, sorted by name. */
  def skillDirs(skillsDir: Path): Vector[SkillName] =
    if !Files.isDirectory(skillsDir) then Vector.empty
    else
      Files.list(skillsDir).iterator.asScala
        .filter(Files.isDirectory(_))
        .filter(d => Files.isRegularFile(d.resolve("SKILL.md")))
        .map(_.getFileName.toString)
        .toVector.sorted

  /** The raw `allowed-tools:` value from a SKILL.md's YAML frontmatter (the first --- … --- block), if present. */
  def allowedToolsValue(skillMd: Path): Option[String] =
    val lines = Files.readAllLines(skillMd).asScala.toList
    if !lines.headOption.map(_.trim).contains("---") then None
    else
      val frontmatter = lines.drop(1).takeWhile(_.trim != "---")
      frontmatter.find(_.trim.startsWith("allowed-tools:"))
        .map(_.trim.stripPrefix("allowed-tools:").trim)
        .filter(_.nonEmpty)

  /** Split an allowed-tools value into individual grants. Top-level tokens are whitespace-separated, but a
    * grant may carry internal spaces inside parentheses (`Bash(scala-cli run *)`), so split on whitespace
    * only at paren-depth 0. */
  def splitGrants(value: String): List[Grant] =
    val out = scala.collection.mutable.ArrayBuffer.empty[Grant]
    val cur = new StringBuilder
    var depth = 0
    for ch <- value do
      ch match
        case '(' => depth += 1; cur += ch
        case ')' => depth = math.max(0, depth - 1); cur += ch
        case c if c.isWhitespace && depth == 0 =>
          if cur.nonEmpty then { out += cur.toString; cur.clear() }
        case c => cur += c
    if cur.nonEmpty then out += cur.toString
    out.toList

  def grantsOf(skillsDir: Path, name: SkillName): List[Grant] =
    allowedToolsValue(skillsDir.resolve(name).resolve("SKILL.md")).map(splitGrants).getOrElse(Nil)

  def printOne(name: SkillName, grants: List[Grant]): Unit =
    if grants.isEmpty then println(s"$name — grants nothing (no allowed-tools frontmatter)")
    else
      println(s"$name — grants ${grants.size} tool permission(s):")
      grants.foreach(g => println(s"  $g"))

  def dispatch(args: Seq[String]): Unit =
    if args.contains("--help") || args.contains("-h") then { println(Help); sys.exit(0) }
    val a = args.toList
    val skillsIdx = a.indexOf("--skills")
    val consumed = if skillsIdx >= 0 then Set(skillsIdx, skillsIdx + 1) else Set.empty[Int]
    val skillsDir: Path =
      (if skillsIdx >= 0 && skillsIdx + 1 < a.size then Some(Path.of(a(skillsIdx + 1))) else None)
        .orElse(toolsDir().map(_.getParent.resolve("skills")))
        .getOrElse:
          Console.err.println("skillgrants: cannot locate skills/ (pass --skills <dir>, or set -Dtt.tools=<dir>)")
          sys.exit(2)
    if !Files.isDirectory(skillsDir) then
      Console.err.println(s"skillgrants: not a skills directory: $skillsDir")
      sys.exit(2)

    // positional skill name = first token that is neither a flag nor a consumed flag-value
    val name = a.zipWithIndex.collectFirst { case (t, i) if !consumed(i) && !t.startsWith("--") => t }

    name match
      case Some(n) =>
        val skillMd = skillsDir.resolve(n).resolve("SKILL.md")
        if !Files.isRegularFile(skillMd) then
          Console.err.println(s"skillgrants: no such skill '$n' under $skillsDir (need $n/SKILL.md)")
          sys.exit(2)
        printOne(n, grantsOf(skillsDir, n))
      case None =>
        val skills = skillDirs(skillsDir)
        if skills.isEmpty then
          Console.err.println(s"skillgrants: no skills found under $skillsDir (each skill needs a SKILL.md)")
          sys.exit(2)
        println(s"skill grants under $skillsDir (${skills.size} skills):")
        println("")
        val perSkill = skills.map(n => n -> grantsOf(skillsDir, n))
        perSkill.foreach { case (n, gs) => printOne(n, gs); println("") }
        val union = perSkill.flatMap(_._2).distinct.sorted
        println(s"=== union: ${union.size} distinct tool permission(s) granted across the set:")
        union.foreach(g => println(s"  $g"))
