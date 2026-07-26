//> using file ../project.scala
//> using dep org.scalameta::munit::1.3.3

// Tests for links.scala. The parsing helpers are pure, so they are tested directly; the value of doing
// so is that every case below is a shape this repo actually contains, not an invented one. The three
// reference shapes (markdown, html attribute, bare path in prose) each get a case, and so does the
// prefix form (research/052) that would otherwise be the silent miss during a migration.

class LinksSuite extends munit.FunSuite:

  test("markdown links and images yield their targets") {
    val t = "see [the doc](docs/foundations.md) and ![fig](media/blog/figures/x.svg)"
    assertEquals(Links.linkTargets(t), Vector("docs/foundations.md", "media/blog/figures/x.svg"))
  }

  test("a bare word is not a link target — documentation that SHOWS the syntax must not self-trip") {
    // tools/README.md documents [text](target) and href="url"; a naive parser reports both as broken.
    assertEquals(Links.linkTargets("""see [text](target) and href="url" """), Vector.empty)
    assertEquals(Links.linkTargets("[real](docs/x.md)"), Vector("docs/x.md"))
  }

  test("an html target counts as present when its markdown source sits beside it") {
    // the site generator produces the .html at render time, so the repo holds only the .md
    assertEquals(Links.generatedFrom("blog/000-why.html"), Some("blog/000-why.md"))
    assertEquals(Links.generatedFrom("docs/x.md"), None)
  }

  test("build caches are skipped, but real dot-directories are repo content") {
    assert(Links.skipDirs(".scala-build"))
    assert(Links.skipDirs("tmp"))
    assert(!Links.skipDirs(".claude-plugin"), "a live plugin manifest dir is not a build cache")
  }

  test("html href and src count as links") {
    val t = """<link rel="stylesheet" href="/genscalator/graphical-profile/design.css">
              |<script src='design.js' defer></script>""".stripMargin
    assertEquals(Links.linkTargets(t), Vector("/genscalator/graphical-profile/design.css", "design.js"))
  }

  test("external targets and bare anchors are not local links") {
    assert(Links.isExternal("https://example.com/x"))
    assert(Links.isExternal("http://example.com"))
    assert(Links.isExternal("mailto:a@b.c"))
    assert(Links.isExternal("#section"))
    // a site-absolute asset path is NOT a repo path: checking it here would cry wolf on every page
    assert(Links.isExternal("/genscalator/graphical-profile/design.css"))
    assert(!Links.isExternal("docs/foundations.md"))
  }

  test("a target loses its fragment, query and trailing sentence punctuation") {
    assertEquals(Links.normalizeTarget("docs/x.md#anchor"), "docs/x.md")
    assertEquals(Links.normalizeTarget("docs/x.md?v=2"), "docs/x.md")
    assertEquals(Links.normalizeTarget("docs/x.md,"), "docs/x.md")
    assertEquals(Links.normalizeTarget("docs/x.md)."), "docs/x.md")
  }

  test("prose path tokens survive backticks, quotes and parens") {
    // This is the shape most skills use to cite research files; missing it was the whole point.
    val t = "see `research/008-instruction-adherence-decay.md` and (media/blog/index.md), not http://x/y.md"
    val got = Links.pathTokens(t)
    assert(clue(got).contains("research/008-instruction-adherence-decay.md"))
    assert(clue(got).contains("media/blog/index.md"))
    assert(!got.exists(_.startsWith("http")))
  }

  test("a bare sibling filename is a path token too — missing it would list a cited file as movable") {
    val got = Links.pathTokens("folded into `031-references-summary-enum-design.md` last week")
    assert(clue(got).contains("031-references-summary-enum-design.md"))
    // precision is restored by the caller keeping only tokens that RESOLVE, so these are harmless here
    assert(clue(Links.pathTokens("call sys.exit and check 3.9.0-RC4")).nonEmpty)
  }

  test("resolve is relative to the citing file's directory, and .. cannot escape the repo") {
    assertEquals(Links.resolve("lib.scala", "tools/text.scala"), Some("tools/lib.scala"))
    assertEquals(Links.resolve("../project.scala", "tools/test/x.test.scala"), Some("tools/project.scala"))
    assertEquals(Links.resolve("../../outside.md", "docs/x.md"), None)
  }

  test("a dir/prefix citation resolves to every file with that prefix — the generous direction") {
    val known = Set("research/052-scala-native-jdk-gaps.md", "research/029-cross-model.md", "docs/x.md")
    val dirs = Set("research", "docs")
    assertEquals(Links.referents("research/052", known, dirs), Set("research/052-scala-native-jdk-gaps.md"))
    assertEquals(Links.referents("docs/x.md", known, dirs), Set("docs/x.md"))
    assertEquals(Links.referents("research/999", known, dirs), Set.empty[String])
  }

  test("a citation of a directory counts as referencing the directory") {
    val known = Set("research/wr-data/a.md")
    val dirs = Set("research", "research/wr-data")
    assertEquals(Links.referents("research/wr-data", known, dirs), Set("research/wr-data"))
  }
