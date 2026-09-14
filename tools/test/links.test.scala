//> using file ../project.scala
//> using dep org.scalameta::munit::1.3.4
//> using dep com.lihaoyi::os-lib:0.11.8

// Tests for links.scala. The parsing helpers are pure, so they are tested directly; the value of doing
// so is that every case below is a shape this repo actually contains, not an invented one. The three
// reference shapes (markdown, html attribute, bare path in prose) each get a case, and so does the
// prefix form (research/052) that would otherwise be the silent miss during a migration.
//
// The issue-053 cases at the end are the exception, and deliberately so: the thing under test there is
// whether the WALK descends, which no assertion about a pure helper can see. They build a real tree in
// a temp dir (os-lib, as RunningBinaryRenameSuite does) and run the effectful `scanDir` / `inventory`
// over it.

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

  // Code-span blindness: 6 of the 19 "dangling" links measured on 2026-07-28 were links being TALKED
  // ABOUT, not links. Every case below is a real line from this repo.
  test("a link inside a FENCED code block is documentation, not a link") {
    val t = "intro\n```\n    keep the [name](file.md) link EXACTLY   # NEVER drop a pointer\n```\nafter"
    assertEquals(Links.linkTargets(t), Vector.empty)
  }

  test("a link inside INLINE backticks is documentation, not a link") {
    // research/topics/RT030-ssg-scoping.md, describing how figures are cited
    val t = "**Figures:** hand-authored **SVG** referenced as `![alt](figures/x.svg)`, plus one PNG."
    assertEquals(Links.linkTargets(t), Vector.empty)
    // a log QUOTING the dangling links it reports must not re-report them
    assertEquals(Links.linkTargets("now-dangling links `[HUMANS.inbox.md](HUMANS.inbox.md)` on line 56"), Vector.empty)
  }

  test("stripping code must not swallow the REAL links around it") {
    val t = "see [real](docs/a.md)\n```\n[fake](docs/b.md)\n```\nand `[also fake](docs/c.md)` and [real2](docs/d.md)"
    assertEquals(Links.linkTargets(t), Vector("docs/a.md", "docs/d.md"))
  }

  test("stripCode preserves the LINE COUNT so line-numbered reporting stays honest") {
    val t = "a\n```\nb\nc\n```\nd"
    assertEquals(Links.stripCode(t).split("\n", -1).length, t.split("\n", -1).length)
  }

  test("an UNMATCHED backtick strips nothing — ambiguity fails toward flagging") {
    // a lone backtick (an apostrophe-ish typo) must not blank the rest of the line and hide a real link
    assertEquals(Links.linkTargets("a ` stray tick then [real](docs/x.md)"), Vector("docs/x.md"))
  }

  test("HTML is scanned RAW: a backtick is not a code delimiter there, so an href cannot be hidden") {
    val t = """text ` <a href="docs/x.md">y</a> ` more"""
    assertEquals(Links.linkTargets(t, stripCodeSpans = false), Vector("docs/x.md"))
    // and with markdown rules it WOULD be stripped — which is exactly why callers pass the file type
    assertEquals(Links.linkTargets(t, stripCodeSpans = true), Vector.empty)
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
    // `out/` is the assembled SITE (deploy/buildsite.sc) and `.scalex/` is a symbol-index cache. Both are
    // derived and gitignored, so scanning them double-counts generated copies of the sources: measured
    // 2026-07-28, `out/` alone pushed the repo scan from 289 files to 297 and would have distorted the
    // CI gate. ⚠ The SITE is still checkable - deliberately, by pointing `links check` AT out/, which is
    // a different question (do the published pages resolve?) from the repo check.
    assert(Links.skipDirs("out"), "the assembled site is derived; scanning it double-counts the sources")
    assert(Links.skipDirs(".scalex"))
    // `.claude/` is deliberately NOT here. It is harness scratch in THIS repo, but `tt links` is
    // project-agnostic (links.scala:4, CONTRIBUTING.md line 34) and repos that COMMIT
    // `.claude/agents/*.md` are common — a name on this list would hide their broken links and invent
    // dangling ones for links pointing in (asserted below). What actually needs skipping is a nested
    // checkout, and that is detected structurally by `holdsGitEntry` (issue 053).
    assert(!Links.skipDirs(".claude"), "a repo may TRACK .claude/; use the structural .git-entry check")
  }

  // --- issue 053: a nested checkout is a second copy of the repo, and must not be scanned.
  //
  // These are FIXTURE tests, not member assertions on `skipDirs`. That is the point: the rule under test
  // is "does the walk descend?", and a member check cannot see a regression in the walk. Both walkers
  // (`scanDir`, `inventory`) share one decision (`skipDir`), and both are asserted, because the check
  // compares their outputs — a directory scanned but NOT inventoried reports every link in it as
  // dangling, which is the same false positive from the other side.

  /** A directory that presents as its own checkout. `worktree = true` writes `.git` as a FILE holding a
    * `gitdir:` pointer, which is what `git worktree add` and a submodule produce; `false` writes it as a
    * directory, which is what a plain clone produces. Both shapes must be detected, so both are built. */
  private def nestedCheckout(at: os.Path, worktree: Boolean): Unit =
    os.makeDir.all(at)
    if worktree then os.write(at / ".git", s"gitdir: /elsewhere/.git/worktrees/${at.last}\n")
    else os.makeDir.all(at / ".git")

  test("issue 053: the scan does not descend into a nested git worktree") {
    val root = os.temp.dir(prefix = "links053-")
    try
      os.write(root / "README.md", "[doc](docs/x.md)\n")
      os.makeDir.all(root / "docs")
      os.write(root / "docs" / "x.md", "# x\n")
      // exactly where Claude Code's worktree isolation puts one: a full second checkout of this repo
      nestedCheckout(root / ".claude" / "worktrees" / "wt", worktree = true)
      os.write(root / ".claude" / "worktrees" / "wt" / "README.md", "[doc](docs/x.md)\n")
      // and a plain nested clone, whose `.git` is a directory rather than a file
      nestedCheckout(root / "vendor" / "clone", worktree = false)
      os.write(root / "vendor" / "clone" / "README.md", "[gone](nope.md)\n")

      val scanned = Links.scanDir(root.toNIO, Vector(".md")).map(_._1.replace('\\', '/')).toSet
      assertEquals(scanned, Set("README.md", "docs/x.md"),
        "the walk descended into a nested checkout: every file in it is a second copy of a repo file")

      val (files, dirs) = Links.inventory(root.toNIO)
      val rel = files.map(_.replace('\\', '/')) ++ dirs.map(_.replace('\\', '/'))
      assert(!rel.exists(_.startsWith(".claude/worktrees/wt/")),
        s"the worktree was inventoried: ${rel.filter(_.startsWith(".claude/worktrees/wt/"))}")
      assert(!rel.exists(_.startsWith("vendor/clone/")),
        s"the nested clone was inventoried: ${rel.filter(_.startsWith("vendor/clone/"))}")
      // the directory that HOLDS the checkout is still repo content and still walked — only the
      // checkout itself is cut, so `.claude/` and `vendor/` do not vanish from the inventory
      assert(dirs.map(_.replace('\\', '/')).contains(".claude/worktrees"),
        "the skip cut too high: the parent of a nested checkout is ordinary repo content")
    finally os.remove.all(root)
  }

  test("issue 053: the scanned ROOT is never skipped, even though a checkout root holds `.git`") {
    // `links check` is pointed AT a repo root as a matter of course, and at a worktree when that is what
    // you are in. If the rule did not exempt the root, the canonical invocation would scan nothing at
    // all — a 0-dangling PASS over 0 files, the most expensive way for this tool to be wrong.
    val root = os.temp.dir(prefix = "links053root-")
    try
      nestedCheckout(root, worktree = true)
      os.write(root / "README.md", "[gone](nope.md)\n")
      assertEquals(Links.scanDir(root.toNIO, Vector(".md")).map(_._1).toSet, Set("README.md"))
      assert(!Links.skipDir(root.toNIO, root.toNIO), "the root skipped itself")
      assert(Links.skipDir((root / ".git").toNIO, root.toNIO), "the root's own .git is still skipped")
    finally os.remove.all(root)
  }

  test("issue 053: a repo that TRACKS .claude/ is still checked — both directions") {
    // The regression a `skipDirs(".claude")` entry caused, reported on PR #18. Committing
    // `.claude/agents/*.md` is common practice, and a denylist entry failed in BOTH directions at once:
    //   - a genuine broken link INSIDE .claude/ became invisible (the file was never scanned)
    //   - a valid link INTO .claude/ became a false positive (the target was never inventoried)
    // Asserted as the outcome `check` prints, not as a property of the skip list.
    val root = os.temp.dir(prefix = "links053claude-")
    try
      os.write(root / "README.md", "[the worker](.claude/agents/worker.md)\n")
      os.makeDir.all(root / ".claude" / "agents")
      os.write(root / ".claude" / "agents" / "worker.md", "[missing](../../does-not-exist.md)\n")

      val docs = Links.scanDir(root.toNIO, Vector(".md"))
      val (files, dirs) = Links.inventory(root.toNIO)
      assertEquals(docs.size, 2, "a tracked .claude/ was not scanned, so its broken links are invisible")

      // Mirrors links.scala:384-399 — `check`'s dangling rule lives inline in the @main, so the
      // resolution is reproduced here from the same public helpers rather than left unasserted.
      def resolves(r: String) = files(r) || dirs(r) || Links.generatedFrom(r).exists(files)
      val dangling = (for
        (rel0, text) <- docs
        rel = rel0.replace('\\', '/')
        t <- Links.linkTargets(text, rel.endsWith(".md")) if !Links.isExternal(t)
        n = Links.normalizeTarget(t) if n.nonEmpty
        if !Links.resolve(n, rel).map(_.replace('\\', '/')).exists(resolves)
      yield (rel, t)).toSet

      assertEquals(dangling, Set(".claude/agents/worker.md" -> "../../does-not-exist.md"),
        "exactly the one real break, named by its own path: not the valid link INTO .claude/")
    finally os.remove.all(root)
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
    // prose writes a directory with a trailing slash; the inventory holds it without one
    assertEquals(Links.normalizeTarget("research/experiments/indent-vs-braces/"), "research/experiments/indent-vs-braces")
  }

  test("a directory cited with a trailing slash resolves, and keeps its contents") {
    // the real citation: skills/research-methods writes `research/experiments/indent-vs-braces/`
    val known = Set("research/experiments/indent-vs-braces/probe.scala")
    val dirs = Set("research", "research/experiments", "research/experiments/indent-vs-braces")
    val token = Links.normalizeTarget("research/experiments/indent-vs-braces/")
    assertEquals(Links.referents(token, known, dirs),
      Set("research/experiments/indent-vs-braces", "research/experiments/indent-vs-braces/probe.scala"))
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

  test("a NESTED directory citation names an artifact, so it keeps the contents") {
    val known = Set("research/experiments/ivb/probe.scala", "research/experiments/ivb/task.md", "research/x.md")
    val dirs = Set("research", "research/experiments", "research/experiments/ivb")
    val got = Links.referents("research/experiments/ivb", known, dirs)
    assert(clue(got).contains("research/experiments/ivb/probe.scala"))
    assert(clue(got).contains("research/experiments/ivb/task.md"))
    assert(!clue(got).contains("research/x.md"), "only the cited directory's own contents")
  }

  test("a GROUPING directory citation names a location, so it does NOT keep its contents") {
    // 27 files cite `research/wr-data/` as "the logs"; expanding that kept all 218 and made the
    // migration a no-op. Two components is a grouping, three is an artifact.
    val known = Set("research/wr-data/a.md", "research/wr-data/b.md")
    val dirs = Set("research", "research/wr-data")
    assertEquals(Links.referents("research/wr-data", known, dirs), Set("research/wr-data"))
  }

  test("a TOP-LEVEL directory citation names a location, so it does NOT keep everything") {
    // HUMANS.md links bare `research/` as a repo-map entry; expanding that would make a migration a no-op
    val known = Set("research/a.md", "research/b.md")
    val dirs = Set("research")
    assertEquals(Links.referents("research", known, dirs), Set("research"))
  }

  // --- leaves: reachable but non-propagating -------------------------------------------------------
  // The real case, 2026-07-26: `minion-log/push-17.md` is a frozen record of a past audit. It MENTIONS
  // blog 031, which mentions blog 001, so both drafts were kept three hops from anything alive. A
  // mention in an archive is history, not a dependency.

  test("a leaf matches itself and anything beneath it, and nothing else") {
    val leaves = Vector("research/case-studies/action-research-meta-minion/minion-log")
    assert(Links.isLeaf("research/case-studies/action-research-meta-minion/minion-log", leaves))
    assert(Links.isLeaf("research/case-studies/action-research-meta-minion/minion-log/push-17.md", leaves))
    assert(!Links.isLeaf("research/case-studies/action-research-meta-minion/long-lived-meta-minion.md", leaves))
  }

  test("a leaf prefix must end at a path boundary, so a sibling with a longer name is not swallowed") {
    // `research/wr-data-closed` must not be caught by a `research/wr-data` leaf: prefix matching without
    // the separator is the classic way a path rule quietly captures a neighbour.
    val leaves = Vector("research/wr-data")
    assert(Links.isLeaf("research/wr-data/x.md", leaves))
    assert(!Links.isLeaf("research/wr-data-closed/x.md", leaves))
  }

  test("no leaves means nothing is a leaf — the flag is opt-in and cannot change an existing run") {
    assert(!Links.isLeaf("research/wr-data/x.md", Vector.empty))
  }

  // --- the check ignore file (issue-011): by-design dangling links, each recorded WITH its why ----
  // The two real cases behind the mechanism: docs/manual-src pages cite the .html that only `tt ssg`
  // generates, and the serverless-spa-seed template cites the main.js its build step produces.

  test("parseIgnores: an entry is from -> target with a mandatory reason; comments and blanks skip") {
    val t = """|# known by-design dangling links
               |
               |docs/manual-src/index.md -> allowlist.html  # generated into docs/generated by tt ssg
               |skills/serverless-spa-seed/template/index.html -> main.js  # produced by the seed's build
               |""".stripMargin
    assertEquals(Links.parseIgnores(t), Right(Vector(
      Links.Ignored("docs/manual-src/index.md", "allowlist.html", "generated into docs/generated by tt ssg"),
      Links.Ignored("skills/serverless-spa-seed/template/index.html", "main.js", "produced by the seed's build"))))
  }

  test("parseIgnores: an entry WITHOUT a reason is rejected — an exemption is documentation, not silence") {
    assert(clue(Links.parseIgnores("docs/x.md -> y.html\n")).isLeft)
  }

  test("parseIgnores: EVERY bad line is named, not just the first, so one fix-up pass suffices") {
    Links.parseIgnores("just-a-path.md\ndocs/x.md -> y.html\n") match
      case Left(errs) =>
        assert(clue(errs).contains("line 1"))
        assert(clue(errs).contains("line 2"))
      case Right(v) => fail(s"expected Left, got $v")
  }

  test("parseIgnores: a #fragment inside the target survives — the reason marker binds after the arrow") {
    assertEquals(Links.parseIgnores("docs/x.md -> y.html#sec  # generated\n"),
      Right(Vector(Links.Ignored("docs/x.md", "y.html#sec", "generated"))))
  }

  test("excuse: exact pair match only — a NEW dangling link beside an excused one still fails") {
    val ig = Vector(Links.Ignored("docs/manual-src/index.md", "allowlist.html", "generated by tt ssg"))
    assert(Links.excuse("docs/manual-src/index.md", "allowlist.html", ig).isDefined)
    assert(Links.excuse("docs/manual-src/index.md", "other.html", ig).isEmpty)
    assert(Links.excuse("docs/other.md", "allowlist.html", ig).isEmpty)
  }

  test("excuse: a Windows-separator from still matches the checked-in forward-slash entry") {
    val ig = Vector(Links.Ignored("docs/manual-src/index.md", "allowlist.html", "generated by tt ssg"))
    assert(Links.excuse("docs\\manual-src\\index.md", "allowlist.html", ig).isDefined)
  }
