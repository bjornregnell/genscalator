//> using scala 3.8.4
//> using dep org.scalameta::munit::1.3.3

// Unit tests for the text tool's grepr pattern selection (the --any metachar-free OR, SM114) and the OR match.
// Co-located under tools/ (test scope extends the MAIN scope, so `TextTool` is in scope without a `//> using file`).
//   scala-cli test tools

class TextToolSuite extends munit.FunSuite:

  // --- selectGreprPatterns: single-positional (back-compat) ---

  test("single positional regex is the one pattern") {
    assertEquals(TextTool.selectGreprPatterns(List("TODO")), Vector("TODO"))
  }

  test("single positional regex with --count: --count is a flag, not a pattern") {
    assertEquals(TextTool.selectGreprPatterns(List("TODO", "--count")), Vector("TODO"))
  }

  test("single mode keeps a regex that itself contains a pipe (bare | still allowed positionally)") {
    assertEquals(TextTool.selectGreprPatterns(List("TODO|FIXME")), Vector("TODO|FIXME"))
  }

  test("single mode ignores stray trailing args (only the first positional is the pattern)") {
    assertEquals(TextTool.selectGreprPatterns(List("TODO", "junk")), Vector("TODO"))
  }

  // --- selectGreprPatterns: --any (the new metachar-free OR) ---

  test("--any collects every following pattern") {
    assertEquals(TextTool.selectGreprPatterns(List("--any", "TODO", "FIXME", "XXX")),
      Vector("TODO", "FIXME", "XXX"))
  }

  test("--any with --count after the patterns: --count drops out, patterns stay") {
    assertEquals(TextTool.selectGreprPatterns(List("--any", "TODO", "FIXME", "--count")),
      Vector("TODO", "FIXME"))
  }

  test("--any with a single pattern is fine") {
    assertEquals(TextTool.selectGreprPatterns(List("--any", "TODO")), Vector("TODO"))
  }

  test("--any preserves patterns that contain regex metachars (each is a full regex)") {
    assertEquals(TextTool.selectGreprPatterns(List("--any", "^foo", "bar$", "a.c")),
      Vector("^foo", "bar$", "a.c"))
  }

  // --- empty / no-pattern cases (the @main errors on empty) ---

  test("no args yields no patterns") {
    assertEquals(TextTool.selectGreprPatterns(Nil), Vector.empty)
  }

  test("only --count yields no patterns (caller must error)") {
    assertEquals(TextTool.selectGreprPatterns(List("--count")), Vector.empty)
  }

  test("--any with nothing after it yields no patterns") {
    assertEquals(TextTool.selectGreprPatterns(List("--any")), Vector.empty)
  }

  // --- anyMatch: logical OR over regexes ---

  test("anyMatch is true when any one pattern matches") {
    val res = Vector("TODO".r, "FIXME".r)
    assert(TextTool.anyMatch(res, "  // FIXME later"))
    assert(TextTool.anyMatch(res, "  // TODO now"))
  }

  test("anyMatch is false when no pattern matches") {
    val res = Vector("TODO".r, "FIXME".r)
    assert(!TextTool.anyMatch(res, "  a clean line"))
  }

  test("anyMatch: --any TODO FIXME finds the same lines a TODO|FIXME regex would") {
    val pats = TextTool.selectGreprPatterns(List("--any", "TODO", "FIXME"))
    val res = pats.map(_.r)
    val or = Vector("TODO|FIXME".r)
    val lines = List("x TODO y", "z FIXME", "nothing here", "TODOFIXME")
    assertEquals(lines.filter(l => TextTool.anyMatch(res, l)),
      lines.filter(l => TextTool.anyMatch(or, l)))
  }
