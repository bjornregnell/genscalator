//> using scala 3.8.4
//> using dep org.scalameta::munit::1.3.3
//> using dep com.lihaoyi::os-lib:0.11.8

// Unit tests for the shared pure helpers in lib.scala (agenttools.Lib). Co-located under tools/ (test scope):
// `lib.scala` is on the toolbox's MAIN scope, which test scope extends — so no `//> using file` is needed (adding
// one would double-compile lib.scala and clash). These import Lib directly (it has no @main, so no clash with the
// per-tool @mains) and run in-process — fast + hermetic.
//   run from the genscalator root:  scala-cli test tools
import agenttools.Lib

class LibSuite extends munit.FunSuite:

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
