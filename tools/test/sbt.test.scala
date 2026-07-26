//> using file ../project.scala
//> using dep org.scalameta::munit::1.3.3

// Unit tests for sbt.scala's PURE argv planner. No sbt process is touched here — what is pinned is
// the safety-critical construction: the working directory is a validated ABSOLUTE argument (never a
// shell cd), the program is always literally "sbt", and everything after --dir is passed through
// untouched. The rejection cases are the point: they are what keeps this a narrow dir-scoped runner
// rather than the run-anything-anywhere escape hatch the guard exists to resist (SM226).

class SbtSuite extends munit.FunSuite:
  import SbtTool.Plan

  test("--dir plus a task plans sbt in that directory") {
    assertEquals(SbtTool.plan(Seq("--dir", "/home/me/proj", "compile")),
      Right(Plan("/home/me/proj", Seq("sbt", "compile"))))
  }

  test("extra args pass through untouched, so --client <task> works") {
    assertEquals(SbtTool.plan(Seq("--dir", "/p", "--client", "pdfCompendiumEn")),
      Right(Plan("/p", Seq("sbt", "--client", "pdfCompendiumEn"))))
  }

  test("--dir alone is legal: a bare interactive sbt in that directory") {
    assertEquals(SbtTool.plan(Seq("--dir", "/p")), Right(Plan("/p", Seq("sbt"))))
  }

  test("the planned program is always literally sbt, never taken from the args") {
    val argv = SbtTool.plan(Seq("--dir", "/p", "rm", "-rf", "/")).toOption.get.argv
    assertEquals(argv.head, "sbt")
  }

  test("a relative --dir is rejected — the working directory must be unambiguous") {
    assert(SbtTool.plan(Seq("--dir", "proj")).isLeft)
    assert(SbtTool.plan(Seq("--dir", "../proj")).isLeft)
    assert(SbtTool.plan(Seq("--dir", "./proj")).isLeft)
  }

  test("a flag where --dir's value belongs is rejected (absolute-path check catches it)") {
    assert(SbtTool.plan(Seq("--dir", "--client")).isLeft)
  }

  test("--dir must come first, so no sbt argument can shadow it") {
    assert(SbtTool.plan(Seq("compile", "--dir", "/p")).isLeft)
    assert(SbtTool.plan(Seq("--client", "--dir", "/p")).isLeft)
  }

  test("missing --dir value and empty args are usage errors, not silent defaults") {
    assert(SbtTool.plan(Seq("--dir")).isLeft)
    assert(SbtTool.plan(Seq.empty).isLeft)
  }

  test("shell metacharacters in a passed-through arg stay inert data, not a second command") {
    val argv = SbtTool.plan(Seq("--dir", "/p", "compile; rm -rf /")).toOption.get.argv
    assertEquals(argv, Seq("sbt", "compile; rm -rf /"))
  }

  test("help text documents the absolute-dir rule and the shown-gated stance") {
    assert(SbtTool.Help.contains("ABSOLUTE"), SbtTool.Help)
    assert(SbtTool.Help.contains("shown-gated"), SbtTool.Help)
  }
