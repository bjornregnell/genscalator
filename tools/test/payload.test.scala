//> using file ../project.scala
//> using dep org.scalameta::munit::1.3.4
//> using dep com.lihaoyi::os-lib:0.11.8

// Tests for the payload layout the uninstaller's pre-manifest fallback removes (issue 040).
//
// The failure this suite exists to prevent is the one that shipped: `get-genscalator.sc` held a
// hand-typed copy of the layout, nothing related it to the staging step, and it had drifted in BOTH
// directions — missing `reqts` (a leftover the tool then blamed on the user) and claiming `skills`,
// `tools`, `plugins` (a user's own files walked and deleted). Neither direction was caught by
// anything, because the literal was the only statement of the layout in the file.
//
// So there are two kinds of test here, and both are needed:
//   1. DRIFT — the committed region equals a regeneration from the workflow. Cheap, pure, and it is
//      what makes the list a build product rather than a literal with a comment above it.
//   2. BEHAVIOUR — a real scratch-HOME round trip through the actual script, asserting what does and
//      does not survive `--uninstall --force`. The drift test would pass a script whose walk logic
//      was broken; only running it shows the file is gone.
// The round trip is the assertion BR asked for on issue 040, and the over-clean case is the one that
// had never been reproduced anywhere until 2026-08-26 — by hand, on Linux, which is why it is a test
// now instead of a paragraph in an issue.
//
// Deliberately restates the root-locate logic rather than sharing it (scala-style §5: a test that
// re-states its expectation is checking the production path against an independent statement).
class PayloadLayoutSuite extends munit.FunSuite:

  private lazy val root: os.Path =
    sys.props.get("tt.tools").map(os.Path(_)).filter(d => os.exists(d / "tt")).map(_ / os.up).getOrElse:
      Iterator.iterate(os.pwd)(_ / os.up).take(8)
        .find(d => os.exists(d / "tools" / "tt"))
        .getOrElse(throw IllegalStateException(s"cannot locate the repo root (pass -Dtt.tools=<dir>); cwd=${os.pwd}"))

  private lazy val installer = root / "get-genscalator.sc"
  private lazy val workflow  = root / ".github" / "workflows" / "native-release.yml"

  // The expected layout, WRITTEN OUT rather than computed. If this line and the workflow ever
  // disagree, one of them is a deliberate release-payload change and a human should see this test
  // fail while making it — that is the point of stating it twice.
  private val expected = Vector("VERSION.txt", "bin", "docs", "reqts")

  test("the staging step stages exactly the four known top-level entries") {
    assertEquals(agenttools.PayloadLib.stagedTopLevel(os.read(workflow)), expected)
  }

  test("the committed region in get-genscalator.sc IS a regeneration from the staging step") {
    // The drift gate. `deploy/payloadsync.sc -- --write` is what makes this pass again.
    val committed = agenttools.PayloadLib.regionOf(os.read(installer))
    assertEquals(committed, Some(agenttools.PayloadLib.render(expected)))
  }

  test("the region reads back as the same entries by the release gate's own path") {
    // native-release.yml compares the staged tree to entriesOf(region), never to the workflow parser,
    // so that read-back path needs its own assertion or the release gate rests on untested code.
    val committed = agenttools.PayloadLib.regionOf(os.read(installer)).getOrElse(fail("no region"))
    assertEquals(agenttools.PayloadLib.entriesOf(committed), expected)
  }

  test("prose about staging/ does not vote in the generated list") {
    val yaml = """|          # copies staging/imaginary for the sake of an example
                  |          cp -r docs staging/docs
                  |          echo x > staging/VERSION.txt""".stripMargin
    assertEquals(agenttools.PayloadLib.stagedTopLevel(yaml), Vector("VERSION.txt", "docs"))
  }

  test("drift is reported in both directions, named as leftover vs over-clean") {
    val d = agenttools.PayloadLib.drift("x", expected, Vector("bin", "docs", "tools")).getOrElse(fail("no drift"))
    assert(clue(d).contains("MISSING"))
    assert(clue(d).contains("reqts"))
    assert(clue(d).contains("SURPLUS"))
    assert(clue(d).contains("tools"))
    assertEquals(agenttools.PayloadLib.drift("x", expected, expected), None)
  }

  // ---- the round trip -------------------------------------------------------------------------
  // Runs the REAL script against a scratch install root. No network and no release download: the
  // fixture is the payload's SHAPE, which is all the fallback reads. `--home` keeps it inside the
  // temp dir, and uninstall never touches a shell rc (it prints instructions instead).

  private def fixture(dir: os.Path, extra: Seq[os.RelPath] = Nil): Int =
    val payload = Vector(
      os.rel / "bin" / "tt",
      os.rel / "docs" / "foundations.md",
      os.rel / "docs" / "tool-selection.md",
      os.rel / "reqts" / "PRD.md",
      os.rel / "VERSION.txt",
    )
    for p <- payload do os.write.over(dir / p, "x", createFolders = true)
    for p <- extra do os.write.over(dir / p, "mine", createFolders = true)
    payload.size

  private def uninstall(home: os.Path, force: Boolean): os.CommandResult =
    val flags = Seq("--uninstall", "--home", home.toString) ++ Option.when(force)("--force")
    os.proc("scala-cli", "run", installer.toString, "--", flags).call(cwd = root, check = false)

  test("fallback round trip: a pre-manifest install is removed completely, root and all") {
    // Before this fix the same fixture left reqts/PRD.md behind and printed `kept:` about it.
    val home = os.temp.dir(prefix = "gs-payload-clean-")
    val n    = fixture(home)
    val out  = uninstall(home, force = true)
    assertEquals(out.exitCode, 0, clue(out.err.text()))
    assert(clue(out.out.text()).contains(s"removing: $n file(s)"))
    assert(!os.exists(home), s"install root survived: ${if os.exists(home) then os.walk(home) else Nil}")
  }

  test("fallback round trip: a file the installer never wrote SURVIVES --force") {
    // The over-clean, which is the data-loss half. `tools/` is a real directory in the REPO, which is
    // exactly why it looked plausible in the old hand-typed list — but it is not in the payload, so a
    // user's own file under it must not be reachable by the uninstaller.
    val home  = os.temp.dir(prefix = "gs-payload-mine-")
    val mine  = os.rel / "tools" / "scratch.txt"
    val n     = fixture(home, extra = Seq(mine, os.rel / "skills" / "my-skill.md", os.rel / "notes.md"))
    val out   = uninstall(home, force = true)
    assertEquals(out.exitCode, 0, clue(out.err.text()))
    assert(clue(out.out.text()).contains(s"removing: $n file(s)"), "the user's files must not be counted")
    assert(os.exists(home / mine), "the user's own tools/scratch.txt was destroyed")
    assert(os.exists(home / "skills" / "my-skill.md"), "the user's own skills/ file was destroyed")
    assert(os.exists(home / "notes.md"), "a plain file at the install root was destroyed")
    assert(os.exists(home), "the install root must survive when it still holds the user's files")
    TestFs.removeAllForce(home)
  }

  test("the preview says out loud that a claimed directory is emptied, files and all") {
    // Pins the warning text. The old wording promised the opposite ("Anything you put in <home>
    // yourself is NOT listed and NOT touched") two lines above listing the user's file for removal;
    // that sentence is what made the over-clean invisible to anyone reading the output.
    val home = os.temp.dir(prefix = "gs-payload-warn-")
    fixture(home)
    val out = uninstall(home, force = false).out.text()
    assert(clue(out).contains("PREVIEW"))
    assert(clue(out).contains(expected.mkString(", ")), "the warning must render from PayloadTopLevel")
    assert(clue(out).contains("including files you put there yourself"))
    assert(os.exists(home / "VERSION.txt"), "preview must not remove anything")
    TestFs.removeAllForce(home)
  }
