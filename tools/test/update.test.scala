//> using scala 3.8.4
//> using dep org.scalameta::munit::1.3.3

// Unit tests for update's ahead/behind parser (the one bit of pure logic; the git effects stay in the driver).
// Co-located under tools/ (test scope extends the MAIN scope, so `Update` is in scope without a `//> using file`).
//   scala-cli test tools

class UpdateSuite extends munit.FunSuite:

  test("tab-separated ahead/behind parses to the pair") {
    assertEquals(Update.parseAheadBehind("2\t5"), (ahead = 2, behind = 5))
  }

  test("space-separated also parses (git may vary whitespace)") {
    assertEquals(Update.parseAheadBehind("0 3"), (ahead = 0, behind = 3))
  }

  test("up to date is (0, 0)") {
    assertEquals(Update.parseAheadBehind("0\t0"), (ahead = 0, behind = 0))
  }

  test("ahead only (local commits not pushed)") {
    assertEquals(Update.parseAheadBehind("4\t0"), (ahead = 4, behind = 0))
  }

  test("empty output defaults to (0, 0), never throws") {
    assertEquals(Update.parseAheadBehind(""), (ahead = 0, behind = 0))
  }

  test("a single malformed token defaults to (0, 0)") {
    assertEquals(Update.parseAheadBehind("garbage"), (ahead = 0, behind = 0))
  }

  test("non-numeric tokens fall back to 0 each") {
    assertEquals(Update.parseAheadBehind("x\ty"), (ahead = 0, behind = 0))
  }
