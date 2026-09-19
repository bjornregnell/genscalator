# Issue 053: `links check` scans a nested git worktree, so a second copy of the repo doubles every count and invents six dangling links

> status: open 2026-08-29 · labels: toolbox, links, ci, agent-ergonomics, false-positive · measured
> against: v0.10.2 (from `tt --version`: git checkout, bash launcher, Linux) at `c51a728`, scala-cli
> 1.15.0, Scala 3.9.0-RC4 · summary: `links.scala:137` skips a **named list** of build/scratch
> directories and treats every other dot-directory as repo content, deliberately, because
> `.claude-plugin/` is. But `.claude/` is neither: it is gitignored harness scratch, and it can hold a
> nested git **worktree** at `.claude/worktrees/<branch>` — a second checkout of this whole repo. The
> checker descends into it. Measured with one worktree present: **6 dangling of 698 links in 668
> files, exit 1**, against **0 of 349 in 334** with it skipped. All six "dangling" links are the
> worktree's copies of links already ignored by design, unrecognised because the ignore rules key on
> repo-relative paths. The fix is included; the denylist shape that allowed it is the part worth
> discussing, and after review the fix is **structural** rather than a name on the list — see the
> Discussion.

## Description

Before this change, `links.scala:134-138` on `main` (the comment plus the declaration) stated its own
rule and the reason for it:

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
in 668 files — exactly twice 349 and 334 — so the number stops being a property of the repo and becomes
a property of the checkout.

**It is exactly the shape of issue 051.** The skip list is a denylist, and a denylist only ever covers
what someone has already been surprised by. `out/` was added after it was measured to distort the count
(the comment at `links.test.scala:69-73` records that: 289 files → 297); `.claude/` is the next entry,
found the same way. The general statement — *do not scan what git is ignoring* — would retire the class.

## How to reproduce it

```bash
# 1. baseline, no worktree
tt links check "$PWD"
#    => links check: 0 dangling of 349 local link(s) in 334 file(s) (+6 ignored by design)

# 2. add a nested worktree, as Claude Code's isolation does
git worktree add --detach .claude/worktrees/test-053 HEAD

# 3. the same repo, now with a second copy of itself inside it
tt links check "$PWD"
#    => .claude/worktrees/test-053/docs/manual-src/index.md -> foundations.html      (and five more)
#    => links check: 6 dangling of 698 local link(s) in 668 file(s) (+6 ignored by design)   exit 1

# 4. clean up
git worktree remove .claude/worktrees/test-053
```

Measured 2026-08-29 on Linux, v0.10.2 at `c51a728`, and **re-measured 2026-09-14** at `60d4d59` when the
fix was made structural. Steps 1–4 were run. Step 3's two outcomes were obtained on **one** repo state,
by running the pre-fix code and the fixed source in turn: **6 dangling of 698 in 668 files, exit 1**
versus **0 of 349 in 334, exit 0**. The re-measurement used a checkout of `main` parked outside the tree
(`git worktree add /tmp/gs-main main`) as the pre-fix binary, pointed at the same root — `links check`
takes its root as an argument, so both versions can be run against one unchanged tree.

The doubling is exact: 698 = 2 × 349 links, 668 = 2 × 334 files. Earlier drafts of this issue quoted 333
and 666 for the same run; those were wrong and are corrected here.

## Acceptance sketch

* **The structural fix, included in this change** (revised after review — the narrow fix that shipped
  in the first draft of this PR is recorded below as rejected). Skip any directory holding a `.git`
  entry, **file or directory**, since a worktree's and a submodule's `.git` is a file with a `gitdir:`
  pointer while a plain clone's is a directory: `Links.holdsGitEntry` tests for the entry's existence,
  not its type. It detects a nested checkout *structurally*, needs no git binary, and keeps `links` a
  read → compute → print tool that still works on the non-repo trees it is deliberately pointed at
  (`links check out/`). One decision, `Links.skipDir` (`links.scala:168`), now serves both the scan
  (`:308`) and the inventory (`:327`), which is load-bearing: the two are compared against each other,
  so a directory scanned but not inventoried would report every link in it as dangling.
* **The scanned root is exempt.** A repo root holds `.git`, and `links check` is pointed at one as a
  matter of course; without the exemption the canonical invocation would scan nothing and pass with 0
  dangling of 0 links, which is the most expensive way for this tool to be wrong. Asserted.
* **Rejected: add `.claude` to `skipDirs`.** This is what the first draft did, and it is wrong because
  `tt links` is project-agnostic (`links.scala:4`, CONTRIBUTING.md line 34) while `.claude/` being
  scratch is a fact about *this* repo. In a repo that tracks it — committing the agent and skill
  markdown under `.claude/` is common practice — a name on the denylist fails in **both directions at
  once**: a genuine broken link inside `.claude/` goes unreported because the file is never scanned, and
  a valid link *into* `.claude/` is reported dangling because the target is never inventoried. Both are
  now fixture tests.
* **What the structural rule closes, measured.** The denylist entry only ever covered the one directory
  someone had already been surprised by; a worktree created anywhere else walked straight past it. With
  the structural rule, a worktree at `tmp-wt-elsewhere/` — not `.claude`, not on `skipDirs` — is skipped
  too. Verified 2026-09-14 by creating one and re-running: still 0 of 349 in 334.
* **Assert the property, not the member.** Done, and it is why the narrow fix's blind spot was
  catchable: a `skipDirs(".claude")` assertion is a member check that cannot see whether the walk
  descends. The three added tests build real trees in a temp dir and run the effectful `scanDir` /
  `inventory` over them — the same widening argument issue 050 makes about its one exhaustive stderr
  assertion.
* **Still open, and stated rather than glossed.** `holdsGitEntry` costs one filesystem probe per
  directory (unmeasured, and not visibly different in the ~5 s repo scan). A nested checkout with no
  `.git` entry at all — an exported tarball of another repo, say — is still walked, but that is
  indistinguishable from repo content by any structural test.
* **Not adopted: `git check-ignore`.** "Do not scan what git ignores" would also close the class, but it
  makes a pure tool shell out to git and changes its behaviour on non-repo trees. The structural test
  gets the same result without either cost.
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
baseline; the failure with a nested worktree recreated deliberately (exit 1, all six named); the fix on
that same state; that `git ls-files .claude` is empty, so nothing tracked is being skipped; and
`git worktree list` before and after each step. It also ran `LinksSuite` (0 failed of 30) and confirmed
`skipDirs` had exactly one consumer — `links.scala` itself — so that suite was the whole blast radius.
It read `links.scala:134-143`, `:284`, `:304` and the existing `skipDirs` test. NOT verified: the rest
of the suite (not run in that round; `CliSuite` carries the unrelated known failure of issue 050 and
865 s of scala-cli time); behaviour on macOS or Windows; and the structural alternatives, which were
described but neither implemented nor measured. ⚠ The file counts quoted in this round were inconsistent
(333 vs 334, 666 vs 668) and are superseded by the re-measurement in the 2026-09-14 comment below.

### Comment by bjornregnell at 2026-09-11 13:13 — review of PR #18

The denylist entry cannot land, and the reason is one I had not weighed: `tt links` is project-agnostic,
so `.claude/` being scratch is a fact about this repo and not about the tool. In a repo that tracks
`.claude/` the entry fails in both directions at once, demonstrated with a fixture — a `README.md`
linking to `.claude/agents/worker.md`, which links to a file that does not exist:

* before the fix: `1 dangling of 3 local link(s) in 2 file(s)`, correctly naming
  `.claude/agents/worker.md -> ../../does-not-exist.md`
* after the fix: `1 dangling of 1 local link(s) in 1 file(s)`, naming `README.md ->
  .claude/agents/worker.md`, which resolves perfectly on disk

So: swap it for the structural `.git`-entry check this issue's own acceptance sketch already named. Also
noted that the added test is a member assertion on `skipDirs` and so cannot catch that regression, and
that the preamble quoted 333 files where the commit says 334 and cited `:137-138` where pre-fix
`skipDirs` is `:137` only.

### Comment by hmiddelk at 2026-09-14 20:00

Taken as directed; the structural check is in and the denylist entry is out. Three things worth recording
beyond the swap itself.

**The maintainer's fixture is now a test, both directions in one assertion.** `LinksSuite` gained three
fixture tests (33 total, 0 failed) that build real trees in a temp dir: a nested worktree (`.git` as a
file) and a nested clone (`.git` as a directory) are both skipped; the scanned root is *not* skipped even
though it holds `.git`; and the tracked-`.claude/` tree reports exactly the one real break and does not
report the valid link into `.claude/`. The last one mirrors `check`'s dangling rule from the public
helpers, because that rule lives inline in the `@main` and has no callable seam — noted in the test, and
a seam for it is a fair thing to want later.

**The skip decision is now shared rather than duplicated.** Both walkers called `skipDirs` separately
before. They are compared against each other, so the failure mode of them disagreeing is every link in
the affected directory reported dangling — the same false positive as this issue, from the other side.
One `skipDir` removes that possibility structurally.

**The class really does close, and the numbers are now consistent.** Re-measured on one unchanged tree
with `main` parked outside it as the pre-fix binary: 6 of 698 in 668 → 0 of 349 in 334, with the doubling
exact (698 = 2 × 349, 668 = 2 × 334). That also settles the 333/334 and 666/668 disagreement the reviewer
caught: the earlier numbers were wrong, and the corrected ones are in the preamble, the repro and the
CHANGELOG. A worktree at `tmp-wt-elsewhere/` is skipped too, which the denylist entry could never have
managed.

Agent disclosure for this round: the swap, the tests and the re-measurement were done by an AI agent
(Claude Opus 5) in session with me. Verified BY RUNNING: `LinksSuite` (0 failed of 33); the full repro
with a worktree at `.claude/worktrees/test-053` and again at `tmp-wt-elsewhere/`; the pre-fix/post-fix
pair on one tree; `git worktree list` and `git status` after cleanup, so no worktree was left behind.
NOT verified: the rest of the suite (`CliSuite` carries issue 050's unrelated known failure and ~865 s of
scala-cli time); macOS or Windows; and the per-directory cost of the extra filesystem probe, which was
not measured.

### Comment by bjornregnell at 2026-09-18 14:21 — second review of PR #18

The structural rule is what I wanted, and the fixture holds both ways: on the new code the real break
inside `.claude/` is reported and the valid link into it is left alone; on `60d4d59` the false positive
comes back exactly as before. Both nested shapes hold, `LinksSuite` is 0 failed of 33, and the extra
`Files.exists` per directory is not measurable at this size.

Three things before it goes in. The scaladoc on `holdsGitEntry` still says "349 links in 334 files
became 699 in 667" — a **third** pair, different from both the corrected 698/668 and the superseded
666/668, and the only numbers left in the shipped code. It needs another rebase; `main` has moved 18
commits and `CHANGELOG.md` conflicts again, adjacency only. And the canonical figure has drifted under
us: `main` is at 341 files now rather than 334, though the link count is still 349 — worth quoting the
count you actually measure at the point it lands.

One thing for a follow-up rather than this PR: a submodule's `.git` is a file, so `holdsGitEntry` skips
it, and the inventory then skips it *without recording the directory*, so a link from the parent repo
into a submodule comes back as a false dangling. Same shape I asked you to remove from the denylist,
landing on content that is tracked. Nothing here is affected as we have no `.gitmodules`, but
`tt links` is meant to be project-agnostic, so please open it as its own issue with your suggestion for
how the scan and the inventory should split.

### Comment by hmiddelk at 2026-09-19 14:10 — all three done, and the numbers re-taken on the landing tree

**The third pair is gone, and the scaladoc now states the invariant rather than a sample.** You were
right that it was the only numbers left in shipped code, and right that they were a third variant.
Rather than correct them to a fourth, the comment now says what does not drift — a descending scan
reads every file **twice** — and presents the counts as an explicitly dated sample. Re-measured on the
rebased tree, 2026-09-19, one worktree present:

```
before  6 dangling of 698 local links in 686 files   exit 1
after   0 dangling of 349 local links in 343 files   exit 0
```

698 = 2 × 349 and 686 = 2 × 343, so the doubling still holds exactly. The CHANGELOG carries the same
landing figures and names 334 as the earlier measurement rather than silently replacing it.

**On 341 versus 343: they are the same measurement on different bases, and nothing is wrong.** You read
341 and I read 343, which looked like a disagreement until I counted the tracked `.md`/`.html` files
at each commit:

```
fb23f71  340     (main when you reviewed, 14:21)
7d7e70b  342     (the 3.9.0 bump, 17:17 — +2)
b410903  342     (main now)
```

Your review predates the bump by three hours, so your base was `fb23f71` at 340, plus this branch's one
added file — the issue itself — giving **341**. Mine is `b410903` at 342 plus the same one file, giving
**343**. The two files between them are `issue-059-…md` and `issue-060-…md`, added by `7d7e70b`. On both
trees `links check`'s file count equals the tracked count exactly, so nothing untracked or generated is
inflating either number.

That is worth recording rather than just reconciling: **the canonical figure is a property of the
commit, not of the repo**, and this repo quotes it in commit messages. Two people measuring the same
branch correctly, three hours apart, got different right answers. It is the same lesson as the scaladoc
above — the doubling is the invariant, the count is a dated sample.

**The dated `334` references elsewhere in this file are deliberately left as written**, for the reason
the 2026-09-14 comment gives about superseded rounds: they record what a specific run on a specific
tree produced, and rewriting them to today's count would make them false for their own date.

**Rebased, not squashed** — two commits, onto `b410903`. The `CHANGELOG.md` conflict was adjacency
only, both of us adding a bullet in the same section, plus the second commit replacing this branch's
own earlier entry. Nothing of yours was dropped in the resolution.

**Issue 062 is filed** for the submodule gap, with the scan/inventory split you asked for. Two things I
found while writing it that sharpen your note. First, it is a **regression introduced by this PR**
rather than a latent gap — `main` skips by *name*, and `vendor/child` is not on `skipDirs`, so today
the link resolves; measured on one fixture, `main` gives 0 dangling of 1 in 3 files and this branch
gives 1 dangling of 1 in 2 files, with the count falling because the submodule's file leaves scan and
inventory together. Second, on how to recognise a submodule: its `.git` reads
`gitdir: ../../.git/modules/<path>` where a worktree's reads `gitdir: <abs>/.git/worktrees/<name>`, so
the `gitdir:` target would work — but I suggest keying on `.gitmodules` instead, because that is a
tracked file the project declares, while `.git/modules` is git's private layout, and a tool that
refuses to shell out to git should not read its internals either.

Agent disclosure: the scaladoc rewrite, the re-measurement, the rebase and issue 062 were done by an AI
agent (Claude Opus 5) in session with me, and reviewed by me. Verified BY RUNNING on the rebased tree:
the before/after pair above, with a worktree created and removed and `git worktree list` checked after
cleanup; `tt links check`; the tracked-file counts at the four commits quoted above; and, for issue
062, both checker versions against one submodule fixture. NOT verified: macOS or Windows; the rest of
the suite; and the per-directory cost of the extra `Files.exists`, which you measured as not noticeable
at this size but which neither of us has timed.
