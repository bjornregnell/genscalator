# Issue 062: `links check` skips a submodule from the INVENTORY as well as the scan, so a legitimate link from the parent repo into it reads as dangling

> status: open 2026-09-19 · labels: toolbox, links, false-positive, project-agnostic · measured
> against: v0.10.2 (from `VERSION.txt`) at `b410903` plus PR #18's branch, Scala 3.9.0, Linux ·
> summary: issue 053's fix skips any directory holding a `.git` entry, which is right for a nested
> worktree or clone. A **submodule** also holds one, as a file — but unlike those, its path is
> *tracked content of the parent repo* and a legitimate link target. `Links.skipDir` gates the scan
> and the inventory together, and the inventory's `SKIP_SUBTREE` never records the directory, so
> `[lib](vendor/child/README.md)` in the parent resolves to nothing and is reported dangling. Same
> false-positive shape as the `.claude` denylist entry removed in PR #18, on content that is tracked.
> Not reachable in this repo today — there is no `.gitmodules` — but `tt links` is required to be
> project-agnostic.

## Description

PR #18 (issue 053) replaced a denylist entry with a structural rule: a directory holding a `.git`
entry is the root of its own checkout, so the walk stops there. `tools/links.scala`:

```scala
def holdsGitEntry(dir: Path): Boolean = Files.exists(dir.resolve(".git"))

def skipDir(dir: Path, root: Path): Boolean =
  !dir.equals(root) && (skipDirs(…) || holdsGitEntry(dir))
```

One decision, deliberately shared by both walkers, because they are compared against each other and a
directory scanned but not inventoried reports every link in it as dangling. That sharing is correct
for the cases it was written for. A **submodule** is the case it was not.

**Three kinds of directory hold a `.git` entry, and they are not alike:**

| kind | `.git` is | tracked in the parent? | its files are… |
| --- | --- | --- | --- |
| nested worktree | file → `…/.git/worktrees/<name>` | no | a second copy of the parent's own files |
| nested clone | directory | no | a foreign repo that happens to sit here |
| **submodule** | file → `…/.git/modules/<path>` | **yes** — the gitlink, plus `.gitmodules` | foreign content the parent **deliberately references** |

Measured 2026-09-19 by building both shapes locally:

```
submodule  vendor/child/.git  ->  gitdir: ../../.git/modules/vendor/child
worktree   wt/.git            ->  gitdir: /home/hans/genscalator/.git/worktrees/wt

$ git -C parent ls-files vendor/child .gitmodules
.gitmodules
vendor/child
```

For the first two, skipping both scan and inventory is right: nothing in the parent should link into
them, and their internal links are another repo's problem. For a submodule only **half** of that is
right. Its links are still another repo's problem — but its *paths exist and are legitimately linkable
from the parent*, which is the entire point of adding one.

### The failure

`Links.inventory`'s visitor adds a directory to `dirs` only on the non-skipped branch:

```scala
if skipDir(d, root) then SKIP_SUBTREE
else { if !d.equals(root) then dirs += root.relativize(d).toString; CONTINUE }
```

So a skipped submodule is absent from both `files` and `dirs`, and `check`'s resolver —
`files(r) || dirs(r) || generatedFrom(r).exists(files)` — cannot resolve anything under it. A parent
`README.md` containing `[the library](vendor/child/README.md)`, pointing at a file that exists on disk
and is tracked by the project as a whole, is reported dangling.

That is the same defect BR rejected the `.claude` denylist entry for on PR #18: *a valid link into a
skipped directory becomes a false positive.* The denylist version was worse in one respect — it also
hid real breaks inside `.claude/` — and better in another: `.claude/` is scratch, while a submodule is
tracked content the repo chose to depend on.

**Not reachable here today.** `git ls-files .gitmodules` is empty in genscalator, so no submodule
exists and nothing regresses. This is filed because `tt links` is required to be project-agnostic
(`CONTRIBUTING.md` line 34) and submodules are ordinary in the repos it is
meant to serve — the identical argument that removed the `.claude` entry.

## How to reproduce it

```bash
# build a parent repo with a real submodule
mkdir -p /tmp/smtest/child /tmp/smtest/parent
cd /tmp/smtest/child  && git init -q . && echo "# child" > README.md && git add -A && git commit -qm init
cd /tmp/smtest/parent && git init -q . && git add -A && git commit -qm init
git -c protocol.file.allow=always submodule add -q /tmp/smtest/child vendor/child

# the parent links INTO the submodule — a normal thing to do
echo '[the library](vendor/child/README.md)' > /tmp/smtest/parent/README.md

# run the pre-fix and post-fix checkers against that one fixture
scala-cli run tools/links.scala -- check /tmp/smtest/parent          # main
#    => links check: 0 dangling of 1 local link(s) in 3 file(s)

scala-cli run <pr18>/tools/links.scala -- check /tmp/smtest/parent   # issue 053's branch
#    => README.md -> vendor/child/README.md
#    => links check: 1 dangling of 1 local link(s) in 2 file(s)
```

Measured 2026-09-19 on Linux, on one fixture, running both versions against it in turn.

**This is a regression introduced by PR #18, not a pre-existing gap**, and the pair above is what
establishes it. `main` has no `holdsGitEntry`: it skips by *name*, and a submodule at `vendor/child` is
not a name on `skipDirs`, so its files are inventoried and the parent's link resolves. The structural
rule is what makes the submodule invisible. The file count falling from **3 to 2** is the submodule's
`README.md` leaving the scan and the inventory together; the dangling line is the parent's link to it.

Nothing in genscalator is affected — `git ls-files .gitmodules` is empty here, so there is no submodule
to lose — which is why BR asked for this as a follow-up rather than a change to PR #18. The regression
is real but unreachable in this repo, and reachable in any consumer repo that uses submodules.

## Acceptance sketch

* **Split the one decision into two.** `skipDir` currently answers "should the walk stop here?" for
  both walkers. The two questions are different: *do I read this file's links?* (scan) and *does this
  path exist for resolution?* (inventory). A submodule is no-to-the-first, yes-to-the-second. PR #18's
  argument for sharing one decision still holds for everything else, and the fix should keep the
  shared default and carve out exactly this case rather than unpick it.
* **Identify submodules from `.gitmodules`, not from the `gitdir:` target.** Both are available purely
  and both work, but `.gitmodules` at the scanned root is *tracked, declared content* naming exactly
  which paths are submodules, whereas `…/.git/modules/…` versus `…/.git/worktrees/…` is an
  implementation detail of git's private layout. Reading one checked-in file keeps `links` a
  read → compute → print tool with no git binary — the same constraint that ruled out
  `git check-ignore` in issue 053 — and it fails safe: a repo with no `.gitmodules` behaves exactly as
  it does today.
* **Decide how deep the inventory should go, and say so.** Recording only the submodule *directory*
  makes `[lib](vendor/child)` resolve but leaves `[lib](vendor/child/README.md)` dangling, which is
  the more common citation. Recording its files too fixes both at the cost of walking a tree whose
  contents are not this repo's. My suggestion is to **inventory the submodule's files but never scan
  them**, because the asymmetry is exactly the one the issue is about — existence is the parent's
  business, link-correctness is not.
* **A fixture test, not a member assertion.** The lesson from PR #18: the rule is about what the walk
  does, so it needs a real tree. `LinksSuite` already builds nested worktrees and clones in a temp dir
  (`nestedCheckout`); a submodule fixture is the same shape with a `.gitmodules` file, and the
  assertion is that the link into it resolves while its own broken links are not reported.
* **Out of scope:** validating links *inside* a submodule. Those belong to the other repo and should be
  checked by pointing `links check` at it directly, which already works.

## Discussion

### Comment by hmiddelk at 2026-09-19 13:50

Filed at BR's request on PR #18, where he spotted it while reviewing the structural `.git`-entry fix
and asked for it as a follow-up with a suggestion for how the scan and the inventory should split.

His framing is the one I would keep: this is the same shape he made me remove from the denylist, and
it lands on content that is tracked and legitimately linked to. The structural rule is still right —
it is the *sharing* of one decision between two different questions that over-reaches, and only for
the one kind of nested checkout that the parent repo deliberately depends on.

On the two ways to recognise a submodule: I checked both and they work, but I would not use the
`gitdir:` path. `…/.git/modules/…` is git's private layout, and a tool whose whole design point is
not shelling out to git should not be reading git's internals either. `.gitmodules` is a tracked file
the project itself declares, which is the same kind of source `links` already trusts for
`.links.ignore`.

Agent disclosure: drafted by an AI agent (Claude Opus 5) in session with me, and reviewed by me, from
BR's review note on PR #18. Verified BY RUNNING: `git submodule add` and `git worktree add` into a temp
tree, reading both `.git` files (contents quoted above) and `git ls-files` on the parent;
`git ls-files .gitmodules` in genscalator (empty, so nothing here regresses); and the full
reproduction, with `main`'s checker and PR #18's run in turn against one fixture — 0 dangling of 1 in
3 files against 1 dangling of 1 in 2 files. Verified BY READING: `Links.skipDir` and the `inventory`
visitor's skip branch, which is where the directory fails to be recorded.

NOT verified: macOS or Windows; whether `to` and `reach` want the same treatment as `check`, which was
not investigated; and the deeper-nesting cases — a submodule inside a submodule, or one whose own
`.gitmodules` matters — none of which the one-level fixture exercises.
