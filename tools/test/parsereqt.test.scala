//> using file ../project.scala
//> using file ../reqt-vendored
//> using dep org.scalameta::munit::1.3.4
//> using dep com.lihaoyi::os-lib:0.11.8

// Unit tests for parsereqt.scala's lint wrapper — specifically the fenced-block skip (issue 010).
// The pure half (fencedLines / normalizeBullet / dropFenced) runs without touching a file; the last
// test pins the real reqts/PRD.md at ZERO findings, which is the acceptance criterion that matters:
// a lint reporting the same 5 eternal false positives trains every reader to ignore its number.

import reqt.*

class ParseReqtSuite extends munit.FunSuite:

  // Deliberately restates the locate logic (test independence over DRY, scala-style §5).
  private lazy val root: os.Path =
    sys.props.get("tt.tools").map(os.Path(_)).filter(d => os.exists(d / "tt")).map(_ / os.up).getOrElse:
      Iterator.iterate(os.pwd)(_ / os.up).take(8)
        .find(d => os.exists(d / "tools" / "tt"))
        .getOrElse(throw IllegalStateException(s"cannot locate the repo root (pass -Dtt.tools=<dir>); cwd=${os.pwd}"))

  test("fencedLines collects the CONTENT of a fenced block, not its markers") {
    val src =
      """prose before
        |
        |```
        |* ENT: id
        |* ATTR: text
        |```
        |
        |* Feature: real
        |""".stripMargin
    val fenced = ParseReqt.fencedLines(src)
    assertEquals(fenced, Set("ENT: id", "ATTR: text"))
    assert(!fenced.contains("```"), fenced.toString)
    assert(!fenced.contains("Feature: real"), fenced.toString) // outside the fence, still judged
  }

  test("a ``` inside a ~~~ block is content, not a closing marker") {
    val src =
      """~~~
        |```
        |* ENT: id
        |~~~
        |* Feature: real
        |""".stripMargin
    val fenced = ParseReqt.fencedLines(src)
    assert(fenced.contains("ENT: id"), fenced.toString)
    assert(!fenced.contains("Feature: real"), fenced.toString) // the ~~~ closed the block
  }

  test("an info string and up to three spaces of indent still open a fence") {
    val src =
      """   ```scala
        |* ENT: id
        |   ```
        |""".stripMargin
    assert(ParseReqt.fencedLines(src).contains("ENT: id"))
  }

  test("four spaces is an indented code block, not a fence — so it does not open one") {
    val src =
      """    ```
        |* ENT: id
        |""".stripMargin
    assertEquals(ParseReqt.fencedLines(src), Set.empty[String])
  }

  test("normalizeBullet strips the list marker the parser has already consumed") {
    assertEquals(ParseReqt.normalizeBullet("  * ENT: id  "), "ENT: id")
    assertEquals(ParseReqt.normalizeBullet("- ENT: id"), "ENT: id")
    assertEquals(ParseReqt.normalizeBullet("+ ENT: id"), "ENT: id")
    assertEquals(ParseReqt.normalizeBullet("ENT: id"), "ENT: id")
    assertEquals(ParseReqt.normalizeBullet("*emphasis* not a bullet"), "*emphasis* not a bullet")
  }

  test("dropFenced removes only the findings whose text sits inside a fence") {
    val inFence  = ParseReqt.Finding("ENT: id", "unknown concept 'ENT' kept as Text: ENT: id")
    val outside  = ParseReqt.Finding("Feautre: typo", "unknown concept 'Feautre' kept as Text: Feautre: typo")
    val kept     = ParseReqt.dropFenced(List(inFence, outside), Set("ENT: id"))
    assertEquals(kept, List(outside)) // the real typo survives; the metasyntax does not
  }

  test("an empty fenced block suppresses nothing") {
    assertEquals(ParseReqt.dropFenced(List(ParseReqt.Finding("X: y", "m")), Set.empty), List(ParseReqt.Finding("X: y", "m")))
  }

  test("the skip is LINT-only: fenced bullets still reach the parsed model") {
    val src =
      """```
        |* ENT: id
        |```
        |""".stripMargin
    // the wrapper never edits what the vendored parser sees, so the fenced bullet is still an elem
    // that findings() flags — it is dropFenced, downstream, that decides not to report it
    val raw = ParseReqt.findings(MarkdownParser.parseModel(src))
    assert(raw.exists(_.text.startsWith("ENT: id")), raw.toString)
    assertEquals(ParseReqt.dropFenced(raw, ParseReqt.fencedLines(src)), Nil)
  }

  test("ACCEPTANCE (issue 010): the repo's own PRD.md lints to zero findings") {
    val prd = root / "reqts" / "PRD.md"
    assert(os.exists(prd), s"missing $prd")
    val src  = os.read(prd)
    val all  = ParseReqt.findings(MarkdownParser.parseModel(src))
    val kept = ParseReqt.dropFenced(all, ParseReqt.fencedLines(src))
    assertEquals(
      kept.map(_.message),
      Nil,
      "PRD.md must lint clean — a non-empty list here is a REAL fall-through to fix, not a false positive",
    )
  }
