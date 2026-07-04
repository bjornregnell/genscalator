//> using dep org.scalameta::munit::1.3.3

package blog

import Summary.*

class ReferencesTests extends munit.FunSuite:

  test("every reference renders to non-empty Markdown without throwing"):
    references.foreach: r =>
      assert(r.toMarkdown.nonEmpty, s"empty markdown for: ${r.title}")

  test("toMarkdown renders the ELIZA reference: author, italic title, year, verified badge, and the OtherSummary body"):
    val r  = references.find(_.title.startsWith("ELIZA")).get
    val md = r.toMarkdown
    assert(md.contains("Weizenbaum, J."), md)
    assert(md.contains("*ELIZA"),         md)   // italic title
    assert(md.contains("(1966)"),         md)
    assert(md.contains(" ✓"),             md)   // verified badge
    assert(md.contains("ELIZA effect"),   md)   // rendered from the OtherSummary

  test("a reference without a Summary renders just the head line (no summary sub-bullets)"):
    val noSummary = references.find(_.summary.isEmpty).get
    val md = noSummary.toMarkdown
    assert(md.startsWith("- "), md)
    assert(!md.contains("\n  - "), md)          // no indented summary bullets
