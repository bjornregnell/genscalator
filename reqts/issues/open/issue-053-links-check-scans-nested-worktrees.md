# Issue 053: `links check` scans a nested git worktree, so a second copy of the repo doubles every count and invents six dangling links

> status: open 2026-08-29 · labels: toolbox, links, ci, agent-ergonomics, false-positive · measured
> against: v0.10.2 (from `tt --version`: git checkout, bash launcher, Linux) at `c51a728`, scala-cli
> 1.15.0, Scala 3.9.0-RC4 · summary: `links.scala:137-138` skips a **named list** of build/scratch
> directories and treats every other dot-directory as repo content, deliberately, because
> `.claude-plugin/` is. But `.claude/` is neither: it is gitignored harness scratch, and it can hold a
> nested git **worktree** at `.claude/worktrees/<branch>` — a second checkout of this whole repo. The
> checker descends into it. Measured with one worktree present: **6 dangling of 698 links in 666
> files, exit 1**, against **0 of 349 in 333** with it skipped. All six "dangling" links are the
> worktree's copies of links already ignored by design, unrecognised because the ignore rules key on
> repo-relative paths. A one-line fix is included; the denylist shape that allowed it is the part worth
> discussing.

## Description

Before this change, `links.scala:134-138` stated its own rule and the reason for it:

```scala
/** Build caches and scratch, never sources: skipped when scanning AND when inventorying. Everything
  * else, including dot-directories like `.claude-plugin`, is real repo content — skipping those made
  * the checker report a live directory as missing. PURE. */
val skipDirs: Set[String] =
  Set(".git", ".scala-build", ".bsp", ".bloop", ".metals", ".scalex", "node_modules", "target", "tmp", "out")
```

That decision is right: `.claude-plugin/` holds the plugin manifest and must be checked. But it splits
the world in two — build caches (skip) and repo content (scan) — and `.claude/` is in neither half. It
is gitignored (`.gitignore:15`), so it is not repo content; it is not on the list, so it is scanned.

And it is the one directory in the tree that can contain **an entire second checkout of the
repository**. Claude Code's worktree isolation puts one at `.claude/worktrees/<branch>`, a real git
worktree:

```
$ git worktree list
/home/hans/genscalator                                  [issue-053-links-scans-nested-worktrees]
/home/hans/genscalator/.claude/worktrees/test-053        (detached HEAD)
```

Two consequences, both measured below. Every count roughly **doubles**, because every markdown file in
the repo is scanned twice. And six links are reported **dangling** that are not: they are the
worktree's copies of the six links the repo excuses in `links-ignore`, and they go unmatched because an
excuse is keyed on the repo-relative path (`docs/manual-src/index.md`), while the copy presents as
`.claude/worktrees/test-053/docs/manual-src/index.md`. Same file, unrecognised path.

### Why it matters more than a wrong number

**It is red locally and green in CI, which is the wrong way round.** `.github/workflows/links-check.yml`
runs the checker on a fresh clone, where no worktree exists, so CI cannot see this. A contributor with
a worktree — which is to say, anyone whose agent sessions use isolation — meets `exit 1` and six
dangling links that have nothing to do with their change. That is the same asymmetry issue 050 records
for `CliSuite`: the check is green on the path that gates and red on the path a contributor runs, so
the failure arrives with maximum capacity to mislead and no gate ever reports it.

**It makes the metric machine-dependent.** "0 dangling of 349 local links" is the line this repo puts in
commit messages (issue 050's own commit does). With a worktree present the same tree reports 698 links
in 666 files, so the number stops being a property of the repo and becomes a property of the checkout.

**It is exactly the shape of issue 051.** The skip list is a denylist, and a denylist only ever covers
what someone has already been surprised by. `out/` was added after it was measured to distort the count
(the comment at `links.test.scala:69-73` records that: 289 files → 297); `.claude/` is the next entry,
found the same way. The general statement — *do not scan what git is ignoring* — would retire the class.

## How to reproduce it

```bash
# 1. baseline, no worktree
tt links check "$PWD"
#    => links check: 0 dangling of 349 local link(s) in 333 file(s) (+6 ignored by design)

# 2. add a nested worktree, as Claude Code's isolation does
git worktree add --detach .claude/worktrees/test-053 HEAD

# 3. the same repo, now with a second copy of itself inside it
tt links check "$PWD"
#    => .claude/worktrees/test-053/docs/manual-src/index.md -> foundations.html      (and five more)
#    => links check: 6 dangling of 698 local link(s) in 666 file(s) (+6 ignored by design)   exit 1

# 4. clean up
git worktree remove .claude/worktrees/test-053
```

Measured 2026-08-29 on Linux, v0.10.2 at `c51a728`. Steps 1–4 were run. Step 3's two outcomes were
obtained on **one** repo state, by running the pre-fix code (the native binary built before this change)
and the fixed source in turn: 6 dangling of 698 in 666 files versus 0 of 349 in 333.

## Acceptance sketch

* **The narrow fix, included in this change.** Add `.claude` to `skipDirs`. Matching is on the directory
  *name* (`links.scala:284`, `:304` after the change), so `.claude-plugin` is a different string and
  stays in the scan —
  the decision the existing comment defends is untouched. Verified: no file under `.claude/` is tracked
  (`git ls-files .claude` is empty), so skipping it creates no blind spot in the checked set.
* **Its limit, stated rather than glossed.** This is one more denylist entry. A worktree created
  anywhere else — `git worktree add ../gs-wt`, or a plain second clone inside the tree — walks straight
  past it, and so would any future harness that picks a different scratch directory.
* **The structural option: do not scan what git ignores.** It closes the class, and the trade-off is
  real and should be weighed rather than assumed: `links.scala` is a PURE tool (read → compute → print),
  and consulting `git check-ignore` would make it shell out to git, which changes what it is and how it
  behaves outside a git checkout (the site check, `links check out/`, is deliberately run on a
  non-repo tree). A middle path is to skip any directory containing a `.git` entry — file *or*
  directory, since a worktree's `.git` is a file — which detects a nested checkout structurally, stays
  pure, and needs no git binary.
* **Assert the property, not the member.** The added test asserts `skipDirs(".claude")`, which is a
  member check: it will not notice the next scratch directory. A test that builds a nested checkout in
  a temp dir and asserts the scan does not descend would hold for whatever the rule becomes — the same
  widening argument issue 050 makes about its one exhaustive stderr assertion.
* **Out of scope:** the ignore-rule keying. Excuses being repo-relative is correct; the defect is that
  a foreign tree was scanned at all, not that its paths failed to match.

## Discussion

### Comment by hmiddelk at 2026-08-29 19:41

Found sideways. An agent ran the link checker before committing an unrelated issue file, reported "6
dangling of 699" and correctly said it was not caused by its change — a leftover worktree from the
PR #14 session was still sitting in `.claude/worktrees/`. Removing that worktree restored the canonical
`0 dangling of 349 in 334 files`, which confirmed the diagnosis before any code was touched.

Filed with the fix because the fix is one line and the interesting content is the design note, not the
repair. I would rather the maintainer weigh "do not scan what git ignores" — or the pure variant, skip
any directory holding a `.git` entry — than merge a denylist entry and consider the class closed.

Agent disclosure: drafted by an AI agent (Claude Opus 5) in session with me, from a false positive it
hit while verifying an unrelated change, and reviewed by me. The agent verified BY RUNNING: the
baseline (0 of 349 in 333/334, depending on which uncommitted issue file was present); the failure with
a nested worktree recreated deliberately (6 of 698 in 666, exit 1, all six named); the fix on that same
state (0 of 349 in 333); that `git ls-files .claude` is empty, so nothing tracked is being skipped; and
`git worktree list` before and after each step. It also ran `LinksSuite` (0 failed of 30) and confirmed
`skipDirs` has exactly one consumer — `links.scala` itself — so that suite is the whole blast radius.
It read `links.scala:134-143`, `:284`, `:304` and the existing `skipDirs` test. NOT verified: the rest
of the suite (not run here; `CliSuite` carries the unrelated known failure of issue 050 and 865 s of
scala-cli time); behaviour on macOS or Windows; and the structural alternatives, which are described
but neither implemented nor measured.
