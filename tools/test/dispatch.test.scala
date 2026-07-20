//> using scala 3.8.4
//> using dep org.scalameta::munit::1.3.3
//> using dep com.lihaoyi::os-lib:0.11.8

// Tests for the single dispatcher (dispatch.scala). The coverage test is the drift-proof: the verb
// table must match EXACTLY the set of tools/*.scala files with a top-level @main (minus dispatch
// itself), so adding or removing a tool without touching the table fails the suite. The subprocess
// test exercises the real single-entry-point contract the native image will ship with.

class DispatchSuite extends munit.FunSuite:

  // Deliberately restates the locate logic (test independence over DRY, scala-style §5).
  private lazy val toolsDir: os.Path =
    sys.props.get("tt.tools").map(os.Path(_)).filter(d => os.exists(d / "tt")).getOrElse:
      Iterator.iterate(os.pwd)(_ / os.up).take(8)
        .find(d => os.exists(d / "tools" / "tt")).map(_ / "tools")
        .getOrElse(throw IllegalStateException(s"cannot locate tools/ (pass -Dtt.tools=<dir>); cwd=${os.pwd}"))

  test("verb table covers exactly the tool files with a top-level @main") {
    val stems = os.list(toolsDir)
      .filter(p => os.isFile(p) && p.ext == "scala")
      .filter(p => os.read.lines(p).exists(_.startsWith("@main ")))
      .map(_.baseName)
      .filterNot(_ == "dispatch")
      .toSet
    assertEquals(Dispatch.verbs.toSet, stems)
  }

  test("verbs are unique and usage lists every verb") {
    assertEquals(Dispatch.verbs.distinct, Dispatch.verbs)
    Dispatch.verbs.foreach(v => assert(clue(Dispatch.usage).contains(v)))
  }

  test("entryFor: known verb yields an entry, unknown yields none") {
    assert(Dispatch.entryFor("text").isDefined)
    assert(Dispatch.entryFor("no-such-tool").isEmpty)
  }

  test("subprocess golden: text count through the single entry point") {
    val f = os.temp(contents = "foo\nbar\nfoo baz foo\n", suffix = ".txt")
    try
      val r = os.proc("scala-cli", "run", toolsDir.toString,
          "--main-class", "dispatchTypedTools", "--", "text", "count", f.toString, "foo")
        .call(check = false, stdout = os.Pipe, stderr = os.Pipe)
      assertEquals(r.exitCode, 0)
      assertEquals(r.out.text().trim, "3")
    finally os.remove(f)
  }
