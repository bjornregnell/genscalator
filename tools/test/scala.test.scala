//> using scala 3.8.4
//> using dep org.scalameta::munit::1.3.3

// Unit tests for scala.scala's PURE core `ScalaTool.plan` — the safety-critical part: it builds the exact
// scala-cli argv with no filesystem or process effect, so the invariants that let `tt scala` retire the
// blanket `Bash(scala-cli *)` allow (no `-e` eval, no arbitrary-flag passthrough, dir-scoped) are pinned
// here. The effectful dispatch (dir-exists check + os.proc) is deliberately not run.

class ScalaPlanSuite extends munit.FunSuite:

  test("test/compile/run build scala-cli <verb> <dir> --server=false"):
    assertEquals(ScalaTool.plan(Seq("test", "/abs/d")).map(_.argv),
      Right(Seq("scala-cli", "test", "/abs/d", "--server=false")))
    assertEquals(ScalaTool.plan(Seq("compile", "/abs/d")).map(_.argv),
      Right(Seq("scala-cli", "compile", "/abs/d", "--server=false")))
    assertEquals(ScalaTool.plan(Seq("run", "/abs/d")).map(_.argv),
      Right(Seq("scala-cli", "run", "/abs/d", "--server=false")))

  test("package-js builds the --power package --js argv and requires -o"):
    assertEquals(ScalaTool.plan(Seq("package-js", "/d", "-o", "main.js")).map(_.argv),
      Right(Seq("scala-cli", "--power", "package", "/d", "--js", "-o", "main.js", "-f", "--server=false")))
    assert(ScalaTool.plan(Seq("package-js", "/d")).isLeft)

  test("--prop k=v becomes --java-prop k=v (repeatable), bad prop rejected"):
    assertEquals(ScalaTool.plan(Seq("test", "/d", "--prop", "tt.tools=/d")).map(_.argv),
      Right(Seq("scala-cli", "test", "/d", "--server=false", "--java-prop", "tt.tools=/d")))
    assertEquals(ScalaTool.plan(Seq("test", "/d", "--prop", "a=1", "--prop", "b=2")).map(_.argv),
      Right(Seq("scala-cli", "test", "/d", "--server=false", "--java-prop", "a=1", "--java-prop", "b=2")))
    assert(ScalaTool.plan(Seq("test", "/d", "--prop", "noequals")).isLeft)

  test("SAFETY: a smuggled scala-cli flag is rejected as an unknown option (no passthrough)"):
    assert(ScalaTool.plan(Seq("test", "/d", "-e", "println(1)")).isLeft)
    assert(ScalaTool.plan(Seq("run", "/d", "--js")).isLeft)
    assert(ScalaTool.plan(Seq("compile", "/d", "--server=true")).isLeft)

  test("unknown verb, missing dir, empty args are rejected"):
    assert(ScalaTool.plan(Seq("eval", "/d")).isLeft)
    assert(ScalaTool.plan(Seq("test")).isLeft)
    assert(ScalaTool.plan(Seq.empty).isLeft)

  test("-o is only valid for package-js"):
    assert(ScalaTool.plan(Seq("test", "/d", "-o", "x.js")).isLeft)

  test("SAFETY: a <dir> or -o value starting with '-' is rejected (flag injection via the path)"):
    assert(ScalaTool.plan(Seq("test", "-e")).isLeft)
    assert(ScalaTool.plan(Seq("run", "--execute")).isLeft)
    assert(ScalaTool.plan(Seq("package-js", "/d", "-o", "-f")).isLeft)
