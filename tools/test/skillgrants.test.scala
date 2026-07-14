//> using scala 3.8.4
//> using dep org.scalameta::munit::1.3.3

// Unit tests for skillgrants' paren-aware grant tokenizer (the one bit of real logic). Co-located under tools/
// (test scope extends the MAIN scope, so `Skillgrants` is in scope without a `//> using file`). Runs in-process.
//   scala-cli test tools

class SkillgrantsSuite extends munit.FunSuite:

  test("splits plain space-separated grants") {
    assertEquals(Skillgrants.splitGrants("Read Bash(tt text *)"), List("Read", "Bash(tt text *)"))
  }

  test("preserves spaces INSIDE parentheses (a grant is one token)") {
    val gs = Skillgrants.splitGrants("Bash(scala-cli run *) Bash(scalex *)")
    assertEquals(gs, List("Bash(scala-cli run *)", "Bash(scalex *)"))
  }

  test("a single grant with internal spaces stays one token") {
    assertEquals(Skillgrants.splitGrants("Bash(git -C *)"), List("Bash(git -C *)"))
  }

  test("collapses runs of whitespace and trims, at depth 0") {
    assertEquals(Skillgrants.splitGrants("  Read   Bash(tt files *)  "), List("Read", "Bash(tt files *)"))
  }

  test("empty value yields no grants") {
    assertEquals(Skillgrants.splitGrants(""), Nil)
    assertEquals(Skillgrants.splitGrants("   "), Nil)
  }

  test("real-world scala-code-review line round-trips to 6 grants") {
    val line = "Bash(scala-cli test *) Bash(scala-cli compile *) Bash(scala-cli run *) " +
      "Bash(git -C *) Bash(tt git *) Bash(tt text grepr *)"
    val gs = Skillgrants.splitGrants(line)
    assertEquals(gs.size, 6)
    assertEquals(gs.head, "Bash(scala-cli test *)")
    assert(gs.contains("Bash(git -C *)"))
  }
