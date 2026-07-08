//> using scala 3.8.4
//> using dep org.scalameta::munit::1.3.3

// Unit tests for the ssg HTML renderer (ssg.scala). The rendering (inline + blocks + renderPage) is PURE, so it
// is tested in-process here; the @main file I/O is not. Covers the fiddly inline cases that blog 002 actually
// exercises (intraword underscores like FAIL_COMPILE, nested bold+italic, code-span protection, image-before-link,
// autolinks, url escaping) plus block rendering (headings, lists, tables, fences, blockquotes) and page assembly.
//   run from the genscalator root:  scala-cli test tools

class SsgSuite extends munit.FunSuite:
  import Ssg.*

  test("escape handles & < >") {
    assertEquals(escape("a < b & c > d"), "a &lt; b &amp; c &gt; d")
  }

  // --- inline emphasis ---
  test("inline: bold, star-italic, underscore-italic") {
    assertEquals(renderInline("**bold** and *it* and _em_"), "<strong>bold</strong> and <em>it</em> and <em>em</em>")
  }
  test("inline: intraword underscores are NOT italicised (FAIL_COMPILE hazard)") {
    assertEquals(renderInline("**FAIL_COMPILE** and **FAIL_MISSCOPE**"),
      "<strong>FAIL_COMPILE</strong> and <strong>FAIL_MISSCOPE</strong>")
  }
  test("inline: nested bold + underscore-italic") {
    assertEquals(renderInline("**_not_**"), "<strong><em>not</em></strong>")
  }
  test("inline: bold containing star-italic (nested emphasis, blog 002 pattern)") {
    assertEquals(renderInline("**a *b* c**"), "<strong>a <em>b</em> c</strong>")
  }
  test("inline: a code span inside a link resolves (nested placeholder)") {
    assertEquals(renderInline("[`code`](u)"), """<a href="u"><code>code</code></a>""")
  }

  // --- inline code / links / images / autolinks (stashed, escaped, protected) ---
  test("inline: code span is escaped and its insides are not emphasised") {
    assertEquals(renderInline("`a **b** <c>`"), "<code>a **b** &lt;c&gt;</code>")
  }
  test("inline: link, and & in the url is escaped") {
    assertEquals(renderInline("[t](http://x?a=1&b=2)"), """<a href="http://x?a=1&amp;b=2">t</a>""")
  }
  test("inline: autolink") {
    assertEquals(renderInline("<https://a.b/c>"), """<a href="https://a.b/c">https://a.b/c</a>""")
  }
  test("inline: image is matched before link semantics") {
    assertEquals(renderInline("![alt](figures/f.svg)"), """<img src="figures/f.svg" alt="alt">""")
  }

  // --- intra-site link rewriting (.md -> .html) ---
  test("siteHref rewrites a relative .md link to .html, preserving any #fragment") {
    assertEquals(siteHref("003-bigger.md"), "003-bigger.html")
    assertEquals(siteHref("002-style.md#55-could-this-just-be-chance"), "002-style.html#55-could-this-just-be-chance")
    assertEquals(siteHref("../research/experiments/x/README.md"), "../research/experiments/x/README.html")
  }
  test("siteHref leaves absolute URLs, mailto, pure anchors, and non-md paths untouched") {
    assertEquals(siteHref("https://a.b/c.md"), "https://a.b/c.md")
    assertEquals(siteHref("mailto:x@y.z"), "mailto:x@y.z")
    assertEquals(siteHref("#section"), "#section")
    assertEquals(siteHref("figures/f.svg"), "figures/f.svg")
  }
  test("inline: a relative .md link renders with an .html href") {
    assertEquals(renderInline("[next](003-bigger.md)"), """<a href="003-bigger.html">next</a>""")
  }

  // --- headings ---
  test("headingParts counts hashes and caps at 6") {
    assertEquals(headingParts("# Title"), (1, "Title"))
    assertEquals(headingParts("### Sub"), (3, "Sub"))
    assertEquals(headingParts("####### deep"), (6, "deep"))
  }

  // --- blocks ---
  test("renderBlocks: heading, paragraph with emphasis, hr") {
    val out = renderBlocks(MdParse.parse("# H\n\npara *x*\n\n---"))
    assert(clue(out).contains("<h1>H</h1>"))
    assert(out.contains("<p>para <em>x</em></p>"))
    assert(out.contains("<hr>"))
  }
  test("renderBlocks: bullet list and ordered list") {
    val ul = renderBlocks(MdParse.parse("- a\n- b"))
    assert(clue(ul).contains("<ul>") && ul.contains("<li>a</li>") && ul.contains("<li>b</li>") && ul.contains("</ul>"))
    val ol = renderBlocks(MdParse.parse("1. a\n2. b"))
    assert(clue(ol).contains("<ol>") && ol.contains("<li>a</li>"))
  }
  test("renderBlocks: blockquote wraps inline") {
    val out = renderBlocks(MdParse.parse("> quoted **b**"))
    assert(clue(out).contains("<blockquote>") && out.contains("<p>quoted <strong>b</strong></p>"))
  }
  test("renderBlocks: code fence gets a language class and is escaped, not reflowed") {
    val out = renderBlocks(MdParse.parse("```scala\nval x = a < b\n```"))
    assert(clue(out).contains("""<pre><code class="language-scala">"""))
    assert(out.contains("val x = a &lt; b"))
  }
  test("renderBlocks: GFM table with header row") {
    val out = renderBlocks(MdParse.parse("| a | b |\n|---|---|\n| 1 | 2 |"))
    assert(clue(out).contains("<thead>"))
    assert(out.contains("<th>a</th>"))
    assert(out.contains("<td>1</td>"))
  }

  // --- heading ids + table of contents ---
  test("slugify matches GitHub-style anchors (drops punctuation, spaces to hyphens)") {
    assertEquals(slugify("5.5 Could this just be chance?"), "55-could-this-just-be-chance")
    assertEquals(slugify("1. Two syntaxes, one language"), "1-two-syntaxes-one-language")
  }
  test("headingSlugs de-duplicates GitHub-style (-1, -2)") {
    assertEquals(headingSlugs(MdParse.parse("## Foo\n\n## Foo")), Vector("foo", "foo-1"))
  }
  test("renderBlocks adds an id to h2 but not h1") {
    val out = renderBlocks(MdParse.parse("# Title\n\n## Section one"))
    assert(clue(out).contains("<h1>Title</h1>"))
    assert(out.contains("""<h2 id="section-one">Section one</h2>"""))
  }
  test("buildToc lists h2/h3 as anchor links; empty when fewer than two") {
    val toc = buildToc(MdParse.parse("## A\n\n### B\n\n## C"))
    assert(clue(toc).contains("""<a href="#a">A</a>"""))
    assert(toc.contains("""<a href="#b">B</a>"""))
    assert(toc.contains("toc-sub"))
    assertEquals(buildToc(MdParse.parse("## Only one")), "")
  }

  // --- page assembly ---
  test("renderPage fills TITLE from the first h1 and the CONTENT slot") {
    val page = renderPage("# My Post\n\nhello", "<title>{{TITLE}}</title>|{{CONTENT}}")
    assert(clue(page).contains("<title>My Post</title>"))
    assert(page.contains("<h1>My Post</h1>"))
    assert(page.contains("<p>hello</p>"))
  }
  test("renderPage defaults the title to Untitled when there is no h1") {
    assert(renderPage("no heading", "<title>{{TITLE}}</title>{{CONTENT}}").contains("<title>Untitled</title>"))
  }

  // --- status preamble (SM032) ---
  test("currentStatus reads the LAST verb of the status history") {
    assertEquals(currentStatus("# T\n\n> **Status: initialized 2026-07-03; drafted 2026-07-07.** intro"), Some("drafted"))
    assertEquals(currentStatus("> **Status: drafted 2026-07-03.** more"), Some("drafted"))
  }
  test("currentStatus is None without a status preamble") {
    assertEquals(currentStatus("# T\n\njust prose"), None)
  }
  test("currentStatus stops at the first ** (does not swallow a following bold span)") {
    assertEquals(currentStatus("> **Status: drafted 2026-07-03.** **Author: BR.**"), Some("drafted"))
  }
  test("promoteStatus appends a transition when the current status matches `from`") {
    val md = "# T\n\n> **Status: initialized 2026-07-03; drafted 2026-07-07.** intro"
    assertEquals(promoteStatus(md, "drafted", "published", "2026-07-08"),
      Some("# T\n\n> **Status: initialized 2026-07-03; drafted 2026-07-07; published 2026-07-08.** intro"))
  }
  test("promoteStatus returns None when the current status does not match `from`") {
    assertEquals(promoteStatus("> **Status: drafted 2026-07-03.**", "published", "deployed", "2026-07-08"), None)
  }
  test("promoteStatus then currentStatus reflects the new status (round-trip, case-insensitive from)") {
    val up = promoteStatus("> **Status: drafted 2026-07-03.**", "Drafted", "published", "2026-07-08").get
    assertEquals(currentStatus(up), Some("published"))
    assertEquals(promoteStatus(up, "published", "deployed", "2026-07-09").map(currentStatus), Some(Some("deployed")))
  }
  test("renderBlocks strips the internal Status span from a preamble blockquote, keeping the rest") {
    val out = renderBlocks(MdParse.parse("> **Status: drafted 2026-07-03; published 2026-07-08.** **Audience:** readers"))
    assert(!clue(out).contains("Status"))
    assert(out.contains("<strong>Audience:</strong> readers"))
  }
  test("renderBlocks drops a blockquote that is ONLY a status span (nothing left to show)") {
    assertEquals(renderBlocks(MdParse.parse("> **Status: drafted 2026-07-03.**")).trim, "")
  }
