//> using file ../project.scala
//> using jvm 21
//> using dep org.scalameta::munit::1.3.4

// Tests for tt sub (SM232). The property that matters most is the SAFETY one: preview must not write.
// Everything else is about not corrupting a file while rewriting it — line endings, a missing final
// newline, literal mode, and never descending into a generated build dir.
class SubToolSuite extends munit.FunSuite:
  import SubTool.*

  test("rewrite reports the changed lines with before and after") {
    val (out, changes) = rewrite("a\nb\na\n", "a".r, "z")
    assertEquals(out, "z\nb\nz\n")
    assertEquals(changes.map(_.line), Vector(1, 3))
    assertEquals(changes.head.before, "a")
    assertEquals(changes.head.after, "z")
  }

  test("rewrite leaves a non-matching file byte-identical") {
    val src = "nothing to see\nhere\n"
    val (out, changes) = rewrite(src, "absent".r, "x")
    assertEquals(out, src)
    assert(changes.isEmpty)
  }

  test("rewrite preserves a missing final newline") {
    val (out, _) = rewrite("a\nb", "b".r, "c")
    assertEquals(out, "a\nc")
  }

  test("rewrite preserves CRLF separators") {
    val (out, _) = rewrite("a\r\nb\r\n", "a".r, "z")
    assertEquals(out, "z\r\nb\r\n")
  }

  test("anchors bind per line, not per file") {
    val (out, changes) = rewrite("x\nx\n", "^x$".r, "y")
    assertEquals(out, "y\ny\n")
    assertEquals(changes.size, 2)
  }

  test("capture-group backrefs work in the replacement") {
    val (out, _) = rewrite("scala 3.8.4\n", raw"scala (\d+)\.(\d+)".r, "scala $1.9")
    assertEquals(out, "scala 3.9.4\n")
  }

  test("literal mode disables regex in the pattern AND backrefs in the replacement") {
    val (re, repl) = compile("a.c", "cost $1", literal = true)
    val (out, changes) = rewrite("a.c and abc\n", re, repl)
    assertEquals(out, "cost $1 and abc\n", "a.c must match literally, not as a wildcard")
    assertEquals(changes.size, 1)
  }

  test("generated build dirs are skipped so a bulk rewrite cannot corrupt a build cache") {
    assert(isSkipped(java.nio.file.Path.of("/r/tools/.scala-build/x/Main.scala")))
    assert(isSkipped(java.nio.file.Path.of("/r/proj/target/gen.scala")))
    assert(!isSkipped(java.nio.file.Path.of("/r/tools/text.scala")))
  }

  // The safety property: without --write the tool is a pure reporter.
  test("preview does NOT touch the file; --write does") {
    val work = os.temp.dir(prefix = "ttsub-")
    try
      val f = work / "a.txt"
      os.write(f, "colour\n")
      substituteText("file", f.toString, "colour", "color")
      assertEquals(os.read(f), "colour\n", "preview must not write")
      substituteText("file", f.toString, "colour", "color", "--write")
      assertEquals(os.read(f), "color\n", "--write must apply the change")
    finally TestFs.removeAllForce(work)
  }

  test("tree mode rewrites only the named extensions, and only with --write") {
    val work = os.temp.dir(prefix = "ttsub-")
    try
      os.write(work / "a.scala", "val v = 1\n")
      os.write(work / "b.md", "val v = 1\n")
      os.makeDir.all(work / "target")
      os.write(work / "target" / "c.scala", "val v = 1\n")

      substituteText("tree", work.toString, ".scala", "val v = 1", "val v = 2")
      assertEquals(os.read(work / "a.scala"), "val v = 1\n", "preview must not write")

      substituteText("tree", work.toString, ".scala", "val v = 1", "val v = 2", "--write")
      assertEquals(os.read(work / "a.scala"), "val v = 2\n")
      assertEquals(os.read(work / "b.md"), "val v = 1\n", "other extensions untouched")
      assertEquals(os.read(work / "target" / "c.scala"), "val v = 1\n", "build dir untouched")
    finally TestFs.removeAllForce(work)
  }
