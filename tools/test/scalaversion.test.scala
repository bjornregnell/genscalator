//> using file ../project.scala
//> using dep org.scalameta::munit::1.3.3
//> using dep com.lihaoyi::os-lib:0.11.8

// Tests for the single-sourced Scala version (project.scala). Why this needs a drift-proof at all:
// the `tt` launcher's scala-cli fallback runs ONE tool file, a build unit that does NOT contain the rest
// of the directory — so each tool carries the version through `//> using file`, and the moment one tool
// re-declares a version of its own, two tools can silently build on two different compilers while every
// whole-directory build still looks green. These tests fail instead.

class ScalaVersionSuite extends munit.FunSuite:

  // Deliberately restates the locate logic (test independence over DRY, scala-style §5).
  private lazy val toolsDir: os.Path =
    sys.props.get("tt.tools").map(os.Path(_)).filter(d => os.exists(d / "tt")).getOrElse:
      Iterator.iterate(os.pwd)(_ / os.up).take(8)
        .find(d => os.exists(d / "tools" / "tt")).map(_ / "tools")
        .getOrElse(throw IllegalStateException(s"cannot locate tools/ (pass -Dtt.tools=<dir>); cwd=${os.pwd}"))

  /** Every checked-in Scala source under tools/, skipping generated dirs (.scala-build et al). */
  private def sources: Seq[os.Path] =
    os.walk(toolsDir, skip = p => p.last.startsWith(".")).filter(p => os.isFile(p) && p.ext == "scala")

  private def declaresVersion(p: os.Path): Boolean =
    os.read.lines(p).exists(_.trim.startsWith("//> using scala "))

  private def includesVersionFile(p: os.Path): Boolean =
    os.read.lines(p).exists(l => l.trim == "//> using file project.scala" || l.trim == "//> using file ../project.scala")

  test("the Scala version is declared in exactly one file") {
    assertEquals(sources.filter(declaresVersion).map(_.last).toList, List("project.scala"))
  }

  test("every launcher-runnable tool carries the version include") {
    val mains = os.list(toolsDir)
      .filter(p => os.isFile(p) && p.ext == "scala")
      .filter(p => os.read.lines(p).exists(_.startsWith("@main ")))
    assert(clue(mains).nonEmpty)
    assertEquals(mains.filterNot(includesVersionFile).map(_.last).toList, Nil)
  }

  test("mainless helpers must NOT carry the include — scala-cli cannot chain 'using file'") {
    // A file that is itself INCLUDED by a tool would chain the include one hop further, which scala-cli
    // does not support: it drops the source and warns on stderr for every build. That warning also breaks
    // CliSuite's empty-stderr contract, so this assert catches the cause instead of the symptom.
    val mainless = os.list(toolsDir)
      .filter(p => os.isFile(p) && p.ext == "scala")
      .filterNot(p => os.read.lines(p).exists(_.startsWith("@main ")))
    assert(clue(mainless).nonEmpty)
    assertEquals(mainless.filter(includesVersionFile).map(_.last).toList, Nil)
  }

  test("the version file is directives only — including it adds a version and nothing else") {
    val code = os.read.lines(toolsDir / "project.scala").filterNot(l => l.trim.isEmpty || l.trim.startsWith("//"))
    assertEquals(code.toList, Nil)
  }

  test("the newtool template single-sources too, so generated tools inherit it") {
    val template = os.read.lines(toolsDir / "template.scala.txt").map(_.trim)
    assert(clue(template).contains("//> using file project.scala"))
    assertEquals(template.filter(_.startsWith("//> using scala ")).toList, Nil)
  }
