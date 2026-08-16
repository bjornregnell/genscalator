//> using file ../project.scala
//> using dep org.scalameta::munit::1.3.4
//> using dep com.lihaoyi::os-lib:0.11.8

// Unit tests for WhichTool's PURE helpers (issue-022): PATH splitting, PATHEXT parsing, magic-byte
// kinds. The Windows ';'-separator and PATHEXT cases run on ANY platform precisely because the
// helpers are pure — this suite would have caught the colon-split defect without a Windows box.
// WhichTool sits in the empty package (like this file), so no import is needed.
//   run from the genscalator root:  scala-cli test tools

class WhichUnitSuite extends munit.FunSuite:

  // --- splitPathString (the issue-022 headline: ':' shredded drive-lettered Windows PATHs) ---
  test("splitPathString on ';' keeps drive-lettered Windows entries whole") {
    val raw = "C:\\a\\bin;C:\\b\\cmd;C:\\Users\\u\\.genscalator\\bin"
    val got = WhichTool.splitPathString(raw, ';')
    assertEquals(got.size, 3)                          // NOT ~2x: no colon-shredding
    assertEquals(got(0), "C:\\a\\bin")                 // drive letter survives
    assertEquals(got(2), "C:\\Users\\u\\.genscalator\\bin")
  }
  test("splitPathString on ':' keeps POSIX behaviour, skipping empty entries") {
    val got = WhichTool.splitPathString("/usr/bin::/usr/local/bin", ':')
    assertEquals(got, Vector("/usr/bin", "/usr/local/bin"))  // empty entry (= cwd) skipped on purpose
  }
  test("splitPathString on a single entry yields exactly one dir") {
    // The field pin: PATH=C:\Users\u\.genscalator\bin was reported as "2 dirs" by the ':' split.
    assertEquals(WhichTool.splitPathString("C:\\Users\\u\\.genscalator\\bin", ';').size, 1)
  }
  test("splitPathString keeps a UNC entry whole among drive-lettered ones") {
    // Maintainer's precision note on issue-022: UNC entries carry no colon, so any colon-based
    // arithmetic breaks on them — the ';' split must be indifferent to entry shape.
    val got = WhichTool.splitPathString("C:\\a\\bin;\\\\server\\share\\bin;C:\\b", ';')
    assertEquals(got, Vector("C:\\a\\bin", "\\\\server\\share\\bin", "C:\\b"))
  }

  // --- hitsFor: bare word resolves through the ext list, dir order outranking ext order ---
  test("hitsFor probes the verbatim name first, then each ext in order, per directory") {
    val d1 = os.temp.dir(); val d2 = os.temp.dir()
    os.write(d1 / "tool.EXE", "x"); os.write(d2 / "tool", "x")
    for f <- Seq(d1 / "tool.EXE", d2 / "tool") do f.toIO.setExecutable(true, false)
    val hits = WhichTool.hitsFor("tool", Vector(d1.toNIO, d2.toNIO), Vector(".EXE"))
    // d1 has only the ext form, d2 the verbatim form; dir order decides who comes first,
    // and the file ACTUALLY found (tool.EXE) is what gets reported for the bare word.
    assertEquals(hits, Vector((d1 / "tool.EXE").toNIO, (d2 / "tool").toNIO))
  }

  // --- pathExts (Windows PATHEXT; empty elsewhere) ---
  test("pathExts is empty off Windows regardless of the env value") {
    assertEquals(WhichTool.pathExts(Some(".COM;.EXE"), windows = false), Vector.empty)
  }
  test("pathExts parses a set PATHEXT and falls back to the documented default") {
    assertEquals(WhichTool.pathExts(Some(".COM;.EXE;.BAT"), windows = true), Vector(".COM", ".EXE", ".BAT"))
    assertEquals(WhichTool.pathExts(None, windows = true), Vector(".COM", ".EXE", ".BAT", ".CMD"))
    assertEquals(WhichTool.pathExts(Some(""), windows = true), Vector(".COM", ".EXE", ".BAT", ".CMD"))
  }

  // --- kindOf: MZ alongside the existing ELF case ---
  test("kindOf recognises a PE executable by its MZ magic") {
    assertEquals(WhichTool.kindOf(Array('M'.toByte, 'Z'.toByte, 0x90.toByte, 0x00.toByte)), "PE executable")
  }
  test("kindOf still recognises ELF (the sibling case MZ was modelled on)") {
    assert(WhichTool.kindOf(Array(0x7f.toByte, 'E'.toByte, 'L'.toByte, 'F'.toByte, 2.toByte)).startsWith("ELF binary"))
  }

  // --- looksLikePath: '/' everywhere; '\' and drive prefixes only on Windows ---
  test("looksLikePath takes the path branch for '/' and stays name-branch for bare words") {
    assert(WhichTool.looksLikePath("dir/tool"))
    assert(!WhichTool.looksLikePath("tt"))
    // 'C:\dir\tt.exe' is a path only where '\' is a separator; on POSIX '\' is a legal name char.
    assertEquals(WhichTool.looksLikePath("C:\\dir\\tt.exe"), WhichTool.isWindows)
  }
