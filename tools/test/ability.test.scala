//> using file ../project.scala
//> using dep org.scalameta::munit::1.3.4
//> using dep com.lihaoyi::os-lib:0.11.8

// The gate for the description projection (issue 041, Phase 1). Two halves, and the second is the
// one that matters.
//
// Reading tools/README.md is cheap and catches a hand-edited heading. But the drift this fixes lived
// on the BAD-ARGUMENTS path — the `case _ =>` block — which no test had ever run. The only test that
// looked at descriptions at all (cli.test.scala:2404) runs `--help` across 8 of the 46 verbs and
// asserts SHAPE, `out.contains(s"tt $tool —")`, never content; two of the six divergences (`harden -`
// and `wr -`, ASCII hyphen and no purity marker) would have failed even that assertion, had it
// reached them. So every projected verb is EXECUTED here on both paths, and the tagline is compared
// for exact content.
//
// Invocations go through the single dispatcher rather than per-file `scala-cli run tools/<t>.scala`:
// one build unit for all twelve instead of six, and it is the path that actually ships (the native
// binary). The per-file path for these tools is already covered by CliSuite.

class AbilitySuite extends munit.FunSuite:

  // Twelve subprocesses; DispatchSuite's note applies (a COLD scala-cli compile blows munit's 30s
  // default, and the failure reads as a tool bug rather than a build-tool cost).
  override def munitTimeout = scala.concurrent.duration.Duration(600, "s")

  // Deliberately restates the locate logic (test independence over DRY, scala-style §5).
  private lazy val toolsDir: os.Path =
    sys.props.get("tt.tools").map(os.Path(_)).filter(d => os.exists(d / "tt")).getOrElse:
      Iterator.iterate(os.pwd)(_ / os.up).take(8)
        .find(d => os.exists(d / "tools" / "tt")).map(_ / "tools")
        .getOrElse(throw IllegalStateException(s"cannot locate tools/ (pass -Dtt.tools=<dir>); cwd=${os.pwd}"))

  // scala-cli.bat on Windows: ProcessBuilder does no PATHEXT resolution (see cli.test.scala).
  private val isWindows = System.getProperty("os.name", "").toLowerCase.contains("win")
  private val ScalaCli  = if isWindows then "scala-cli.bat" else "scala-cli"

  // Parity mode (opt-in), the same contract CliSuite and DispatchSuite use.
  private lazy val nativeBin: Option[os.Path] =
    sys.props.get("tt.native.bin").map(os.Path(_, os.pwd)).filter(os.exists)

  /** Run `tt <verb> <args...>` through the dispatcher; return (exit, stdout, stderr). */
  private def run(verb: String, args: String*): (Int, String, String) =
    val r = nativeBin match
      case Some(bin) =>
        os.proc(bin.toString, verb, args).call(check = false, stdout = os.Pipe, stderr = os.Pipe)
      case None =>
        os.proc(ScalaCli, "run", toolsDir.toString, "--main-class", "dispatchTypedTools", "--", verb, args)
          .call(check = false, stdout = os.Pipe, stderr = os.Pipe)
    (r.exitCode, r.out.text().replace("\r\n", "\n").trim, r.err.text().replace("\r\n", "\n").trim)

  private def firstLine(s: String): String = s.linesIterator.nextOption.getOrElse("")

  test("Phase 1 projects exactly the six verbs with proven drift (issue 041 decision, 2026-08-25)") {
    // Pinned, so that widening the projection is a deliberate edit with a reviewable diff rather than
    // a side effect of touching a tool — the sequencing condition the decision attached to its yes.
    assertEquals(ProjectedAbilities.all.map(_.verb),
      Vector("guardcheck", "harden", "log", "text", "typo", "wr"))
  }

  test("every projected verb is one the dispatcher actually serves, and appears once") {
    for d <- ProjectedAbilities.all do
      assert(clue(Dispatch.verbs).contains(d.verb),
        s"'${d.verb}' declares an ability but is not a verb tt serves")
    assertEquals(ProjectedAbilities.all.map(_.verb).distinct.size, ProjectedAbilities.all.size)
    // declFor is the lookup a future renderer (a protocol surface, the allowlist docs) would use.
    assertEquals(ProjectedAbilities.declFor("text").map(_.verb), Some("text"))
    assertEquals(ProjectedAbilities.declFor("no-such-verb"), None)
  }

  test("tools/README.md headings are RENDERED from the declaration, not hand-written") {
    val lines = os.read.lines(toolsDir / "README.md")
    for d <- ProjectedAbilities.all do
      val own = lines.filter(_.startsWith(s"### ${d.verb} —"))
      assertEquals(own.size, 1, s"expected exactly one '### ${d.verb} —' heading, got ${own.size}")
      assertEquals(own.head, d.readmeHeading,
        s"tools/README.md's heading for '${d.verb}' is hand-edited. Edit the declaration in " +
          s"${d.verb}.scala instead, or paste the rendered line back:\n  ${d.readmeHeading}")
  }

  test("--help and the case _ => usage block open with the SAME declared tagline") {
    // The whole point, asserted as an equality rather than a containment: one string where there were
    // two. Before this, all six verbs described themselves differently across these two paths, and
    // four of them dropped the PURE/EFFECTFUL classification from the second.
    for d <- ProjectedAbilities.all do
      val (helpCode, helpOut, _) = run(d.verb, "--help")
      assertEquals(helpCode, 0, s"${d.verb} --help should exit 0")
      assertEquals(firstLine(helpOut), d.tagline, s"${d.verb} --help tagline is not the declared one")

      // No arguments = the bad-arguments path, i.e. the `case _ =>` block. Usage is OUTPUT here, so
      // it goes to stdout even on the tools that exit 2 (log additionally explains itself on stderr).
      val (_, usageOut, _) = run(d.verb)
      assertEquals(firstLine(usageOut), d.tagline,
        s"${d.verb}'s bad-arguments usage block does not open with the declared tagline")
  }

  test("the effect class renders a shouted safety claim and a prose form, for every shape") {
    import Ability.Effect.*
    assertEquals(Pure(readOnly = false).marker(shout = true), "PURE")
    assertEquals(Pure(readOnly = false).marker(shout = false), "pure")
    assertEquals(Pure(readOnly = true).marker(shout = true), "PURE, read-only")
    assertEquals(Pure(readOnly = true).marker(shout = false), "pure, read-only")
    // The two shapes no verb declares YET, exercised so the renderer cannot rot before it is used.
    // md-fmt and sub are the verbs these are for, once the projection widens past Phase 1.
    assertEquals(PureByDefault("`--write` is the one guarded effect").marker(shout = true),
      "PURE by default; `--write` is the one guarded effect")
    assertEquals(Effectful("", previewDefault = false).marker(shout = true), "EFFECTFUL")
    assertEquals(Effectful("", previewDefault = true).marker(shout = true),
      "EFFECTFUL; PREVIEW BY DEFAULT")
    assertEquals(Effectful("writes into the out-dir", previewDefault = false).marker(shout = false),
      "effectful: writes into the out-dir")
  }

  test("a declaration carries no purity marker of its own — appending it is the renderer's job") {
    // Guards the one way a declaration could re-introduce the drift it removes: a summary that states
    // the classification in prose would render it twice, and the two copies could then disagree.
    for d <- ProjectedAbilities.all do
      val s = d.summary.toLowerCase
      assert(!s.contains("pure") && !s.contains("effectful") && !s.contains("read-only"),
        s"'${d.verb}' states its effect class inside the summary ('${d.summary}') — that is what " +
          "`effect` is for; the renderer appends it.")
      assert(!d.summary.contains("("), s"'${d.verb}' summary should not carry a parenthetical: ${d.summary}")
  }
