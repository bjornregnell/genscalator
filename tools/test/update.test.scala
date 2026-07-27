//> using file ../project.scala
//> using dep org.scalameta::munit::1.3.3
//> using dep com.lihaoyi::os-lib:0.11.8

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

  // ---- --native: the pure parts, so the swap's structural rules are checked without a filesystem ----

  test("the asset glob names this platform's zip") {
    assertEquals(Update.assetPattern("linux-x86_64"), "genscalator-linux-x86_64.zip*")
  }

  test("the asset glob matches BOTH the payload and its sibling checksum") {
    // One glob, deliberately: fetching the zip while silently missing the .sha256 would leave the
    // self-updater with nothing to verify against, which it must refuse rather than skip.
    val g = Update.assetPattern("macos-aarch64")
    assert(agenttools.Lib.globMatches(g, "genscalator-macos-aarch64.zip"))
    assert(agenttools.Lib.globMatches(g, "genscalator-macos-aarch64.zip.sha256"))
  }

  test("the asset glob does NOT match another platform's asset") {
    val g = Update.assetPattern("linux-aarch64")
    assert(!agenttools.Lib.globMatches(g, "genscalator-linux-x86_64.zip"))
    assert(!agenttools.Lib.globMatches(g, "genscalator-windows-x86_64.zip"))
  }

  test("staging and retired are SIBLINGS of the install, never children") {
    // Structural, not cosmetic: the swap renames the install directory itself, so a staging dir INSIDE it
    // would be carried along by that rename and the second move would target a path that no longer exists.
    val home                = os.Path("/opt/genscalator")
    val (staging, retired)  = Update.swapPaths(home, 1234L)
    assertEquals(staging / os.up, home / os.up)
    assertEquals(retired / os.up, home / os.up)
    assert(!staging.startsWith(home), clue = s"staging $staging must not be inside $home")
    assert(!retired.startsWith(home), clue = s"retired $retired must not be inside $home")
  }

  test("staging and retired are distinct from each other and from the install") {
    val home               = os.Path("/opt/genscalator")
    val (staging, retired) = Update.swapPaths(home, 1234L)
    assertNotEquals(staging, retired)
    assertNotEquals(staging, home)
    assertNotEquals(retired, home)
  }

  test("distinct runs get distinct staging paths, so a retry cannot collide with a leftover") {
    val home = os.Path("/opt/genscalator")
    assertNotEquals(Update.swapPaths(home, 1L).staging, Update.swapPaths(home, 2L).staging)
  }
