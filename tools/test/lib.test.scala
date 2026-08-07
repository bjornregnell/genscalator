//> using file ../project.scala
//> using dep org.scalameta::munit::1.3.4
//> using dep com.lihaoyi::os-lib:0.11.8

// Unit tests for the shared pure helpers in lib.scala (agenttools.Lib). Co-located under tools/ (test scope):
// `lib.scala` is on the toolbox's MAIN scope, which test scope extends — so no `//> using file` is needed (adding
// one would double-compile lib.scala and clash). These import Lib directly (it has no @main, so no clash with the
// per-tool @mains) and run in-process — fast + hermetic.
//   run from the genscalator root:  scala-cli test tools
import agenttools.Lib

class LibSuite extends munit.FunSuite:

  // --- releasePlatform ---
  // Pins BR's distribution decision of 2026-07-27 as executable fact rather than prose: assets for the
  // four proven platforms, None (build from source) for the rest.
  test("releasePlatform maps the four PUBLISHED platforms") {
    assertEquals(Lib.releasePlatform("Linux", "amd64"), Some("linux-x86_64"))
    assertEquals(Lib.releasePlatform("Linux", "aarch64"), Some("linux-aarch64"))
    assertEquals(Lib.releasePlatform("Mac OS X", "aarch64"), Some("macos-aarch64"))
    assertEquals(Lib.releasePlatform("Windows 11", "amd64"), Some("windows-x86_64"))
  }
  test("releasePlatform returns None for the two platforms with NO published asset") {
    // Not an oversight: Intel mac never produced an artifact, and windows-aarch64 is experimental and
    // failing. A near-miss guess here would install a binary that cannot run.
    assertEquals(Lib.releasePlatform("Mac OS X", "x86_64"), None)
    assertEquals(Lib.releasePlatform("Windows 11", "aarch64"), None)
  }
  test("releasePlatform is case-insensitive and knows the arch aliases") {
    assertEquals(Lib.releasePlatform("LINUX", "X86_64"), Some("linux-x86_64"))
    assertEquals(Lib.releasePlatform("linux", "arm64"), Some("linux-aarch64"))
    assertEquals(Lib.releasePlatform("Darwin", "arm64"), Some("macos-aarch64"))
  }
  test("releasePlatform refuses an unknown os or arch rather than guessing") {
    assertEquals(Lib.releasePlatform("SunOS", "sparc"), None)
    assertEquals(Lib.releasePlatform("Linux", "riscv64"), None)
    assertEquals(Lib.releasePlatform("", ""), None)
  }

  // --- globMatches ---
  // Shared by `tt forge release-download --pattern` and `tt zip extract --exec`. The second consumer
  // arrived hours after the first, which is the moment a private copy would have been duplicated and
  // then drifted; these tests pin the ONE definition instead.
  test("globMatches: a bare name matches only itself") {
    assert(Lib.globMatches("bin/tt", "bin/tt"))
    assert(!Lib.globMatches("bin/tt", "bin/tt.exe"))
    assert(!Lib.globMatches("bin/tt", "sbin/tt"))
  }
  test("globMatches: a trailing star matches a prefix, including the empty remainder") {
    assert(Lib.globMatches("bin/*", "bin/tt"))
    assert(Lib.globMatches("bin/*", "bin/"))
    assert(!Lib.globMatches("bin/*", "docs/tt"))
  }
  test("globMatches: a star matches across separators, and several stars work") {
    assert(Lib.globMatches("*.zip", "genscalator-linux-x86_64.zip"))
    assert(Lib.globMatches("genscalator-*.zip*", "genscalator-linux-x86_64.zip.sha256"))
    assert(Lib.globMatches("*/*", "a/b"))
  }
  test("globMatches: regex metacharacters in the NAME are literal, not a pattern") {
    // The whole reason the glob is quoted per-segment: a name carrying `.` or `+` must not act as regex.
    assert(!Lib.globMatches("a.c", "abc"))
    assert(Lib.globMatches("a.c", "a.c"))
    assert(Lib.globMatches("v1+2", "v1+2"))
  }

  // --- histogram ---
  test("histogram sorts descending by count and shows keys + counts") {
    val h = Lib.histogram(Map("apple" -> 3, "pear" -> 1))
    assert(clue(h).indexOf("apple") < clue(h).indexOf("pear")) // higher count first
    assert(h.contains("3"))
    assert(h.contains("1"))
    assert(h.contains("#")) // bar drawn
  }
  test("histogram of empty map is empty string") {
    assertEquals(Lib.histogram(Map.empty), "")
  }
  test("histogram caps the bar at 50 hashes") {
    val h = Lib.histogram(Map("x" -> 1000))
    assertEquals(h.count(_ == '#'), 50)
  }

  // --- edit1: differ by EXACTLY one edit ---
  test("edit1 true for one substitution")  { assert(Lib.edit1("cat", "car")) }
  test("edit1 true for one insertion")     { assert(Lib.edit1("cat", "cart")) }
  test("edit1 true for one deletion")      { assert(Lib.edit1("cart", "cat")) }
  test("edit1 false for identical (0 edits, not 1)") { assert(!Lib.edit1("cat", "cat")) }
  test("edit1 false for two substitutions") { assert(!Lib.edit1("cat", "cod")) } // a→o AND t→d
  test("edit1 false for three-way change") { assert(!Lib.edit1("cat", "dog")) }
  test("edit1 false when length differs by more than one") { assert(!Lib.edit1("a", "abc")) }

  // --- readUtf8 / readLatin1 ---
  test("readUtf8 round-trips Swedish åäö") {
    val f = os.temp(contents = "räksmörgås", suffix = ".txt")
    try assertEquals(Lib.readUtf8(f.toString), "räksmörgås")
    finally os.remove(f)
  }
  test("readLatin1 never throws on non-UTF-8 bytes and preserves ASCII markers") {
    val f = os.temp(suffix = ".log")
    os.write.over(f, Array[Byte](0xFF.toByte, '!', ' ', 'x')) // 0xFF is invalid UTF-8
    try
      val s = Lib.readLatin1(f.toString)
      assert(s.contains("! ")) // the "! " error marker survives byte-for-byte
    finally os.remove(f)
  }
