# Issue 048: `tt links check` on Windows reports nearly every link dangling, because the inventory is built with backslashes while resolution is forward-slash string arithmetic

> status: open · labels: toolbox, links, ssg, windows, silent-wrong · measured against: v0.10.2 (native binary) ·
> summary: `scanDir` and `inventory` build repo-relative keys with `root.relativize(f).toString`,
> which is backslash-separated on Windows, while `resolve`, `referents` and `pathTokens` all split
> and join on `'/'`. So every resolved target misses the inventory and `check` reports 309 dangling
> of 348 links on a tree whose CI is green. The file already knows: `excuse` (`links.scala:186`)
> normalizes `\` to `/` with a comment saying exactly why — the separator fix landed at one of the
> five sites that need it, and the suite's single Windows test pins that one site. `to` and `reach`
> are affected the same way. A toolbox-wide sweep (Discussion, 2026-08-25) found one further
> instance — `ssg.scala:550`, where the same mismatch decides a `Files.delete` — and four sites
> that are safe for reasons worth writing down.

## Description

Reported by @hmiddelk from a field run of the released **v0.10.2 native binary on Windows**
(recorded at the end of his review reply on PR #5): `tt links check` prints **309 dangling of 348
local links**, with backslash-separated paths in the `from` column, on a checkout whose CI gate is
green. He did not chase it and flagged it as looking like a path-resolution problem in the shipped
binary rather than a repo problem. It is, and the mechanism is one mismatch applied five times.

**The two halves disagree about what a repo-relative path is.**

The inventory half uses `java.nio.file` and gets the platform separator:

* `links.scala:281` — `scanDir`: `val rel = root.relativize(f).toString`
* `links.scala:299`, `:301` — `inventory`: the same call for both `dirs` and `files`

On Windows those produce `docs\native.md`, and that string becomes the key in the `files` and `dirs`
sets and the `from` field of every `Ref`.

The resolution half is pure string arithmetic on forward slashes, by construction:

* `links.scala:222-223` — `resolve`: `fromFile.split('/')` and `repoRel.split('/')`
* `links.scala:228` — `resolve`: `.mkString("/")`
* `links.scala:244`, `:246-251` — `referents`: `token.count(_ == '/')`, `lastIndexOf('/')`,
  `startsWith(dir + "/")`
* `links.scala:215` — `pathTokens`: keeps a prose token only if it `contains('/')`

So on Windows a link `[native](native.md)` inside `docs\native.md` resolves like this:
`fromFile.split('/')` finds no separator, yields the single element `docs\native.md`,
`.dropRight(1)` empties it, and the target resolves to `native.md`. The `resolves` predicate at
`:355-356` then asks `files("native.md")`, and `files` holds `docs\native.md`. Miss. Dangling.

Every relative link in any subdirectory fails this way, which is why the count is 309 of 348 rather
than a handful. The 39 that survive are the ones whose resolution happens to need no directory
context.

### The file already diagnosed this, and fixed it in one place

This is not an unconsidered platform gap. `links.scala:184-187`:

```scala
/** ... `from` is compared with forward slashes so a scan on Windows still matches the checked-in
  * entries. PURE. */
def excuse(from: String, target: String, ignores: Vector[Ignored]): Option[Ignored] =
  val f = from.replace('\\', '/')
```

The comment states the exact defect class this issue reports — a Windows scan produces backslash
keys that will not match forward-slash data — and normalizes for it. But it normalizes only the
`from` field, only inside the exemption lookup, which is the one code path where a mismatch would
merely fail to *excuse* a link rather than fail to *resolve* one. The five sites listed above are
untouched.

`tools/test/links.test.scala` records the same shape: of 30 tests, exactly one is
separator-aware — `:229`, *"excuse: a Windows-separator from still matches the checked-in
forward-slash entry"*, asserting on the literal `"docs\\manual-src\\index.md"`. So the suite pins
the single function that is correct and says nothing about the five that are not.

This is the pattern issues 040 and 042 are both about, arriving a third time: a defect diagnosed in
a comment, fixed for one field, and left standing everywhere else in the same file. Here it is
sharper than either, because the surviving sites are the ones that decide the tool's answer.

### Scope: all three verbs, not just `check`

`edges` (`:339-351`) — which backs both `to` and `reach` — calls `resolve`, `referents` and
`pathTokens`, and matches their forward-slash output against the same backslash `files`/`dirs` sets.
So on Windows `links to` reports no referents for anything in a subdirectory, and `links reach
--unreachable` reports essentially the whole tree as movable. That second one is the dangerous
reading: the verb exists to answer *"may I delete this?"*, and on Windows it currently answers yes
for almost everything.

### Why CI is green and the gate did not catch it

The `links check` gate runs on Linux, where `relativize().toString` already yields forward slashes
and both halves agree. The defect is unreachable on POSIX — the same shape as issue 047, where the
PATHEXT branch is empty off Windows. Nothing here is wrong on the platform the gate runs on.

### A second instance, same cause, in `tt ssg` — and this one deletes

`ssg.scala:550`, in the figure-pruning step:

```scala
if !referenced.contains(outFigures.relativize(p).toString) then Files.delete(p)
```

`referenced` is populated at `:521` from `"(?:src|href)=\"figures/([^\"]+)\"".r` — the capture is
whatever the rendered HTML wrote after `figures/`, which is always forward-slash. The left side of
the comparison is `relativize().toString`, which is backslash-separated on Windows. Same mismatch,
opposite consequence: `check` reports a wrong answer, this **deletes the file**.

The sequence makes it worse than a stale-prune. `:541-546` copies each referenced figure *in*
(`srcFigures.resolve(rel)` handles a forward-slash `rel` correctly on Windows, so the copy
succeeds), and then `:547-551` walks the output and deletes everything not in `referenced`. On
Windows a nested figure is therefore copied in and deleted again in the same run, and the deploy
set silently ships without it.

**Currently latent, and deliberately so far from unreachable.** Every figure in this repo today is
flat (`media/blog/figures/` has no subdirectories), and a flat name has no separator, so the
comparison matches on Windows too. But nested figures are *intended* to work — `:545`'s
`Files.createDirectories(dst.getParent)` exists for no other reason — so this is a gap waiting on
the first `figures/charts/bar.svg`, not a shape the design rules out. Nothing is lost from the
source tree either way: the deletion is confined to the generated out-dir.

### Four sites that are safe, and the rule that separates them

The sweep covered every `relativize` in `tools/` plus the path-string comparisons around them. The
discriminator is one line: **a `relativize().toString` is only a defect when it is compared against
a string that came from somewhere else** — parsed text, a regex capture, a literal. Compared
against another `relativize().toString`, or kept in the `Path` domain, it is correct on every
platform.

| site | why it is safe |
| --- | --- |
| `ssg.scala:368` — `dst.resolve(src.relativize(p))` | never leaves the `Path` domain |
| `ssg.scala:532` — `p.getFileName.toString` vs `written` | both sides are bare filenames, no separator |
| `lib.scala:200`, `:208` — `excludedBy(root.relativize(dir), …)` | matches a `Path` with `fs.getPathMatcher("glob:…")`, which is filesystem-aware and resolves `/` against the platform separator itself. **This is the pattern the other sites should have used** |
| `memory.scala:70` — `listFiles` | both sides of every comparison in `plan` come from this one function, so live and snapshot keys agree on any platform |
| `sub.scala:60` — `path.iterator` … `SkipDirs.contains(p.toString)` | `Path.iterator` yields name elements, which are separator-free by construction |

Writing the safe ones down is the point: four of the five are safe *by accident of shape* rather
than by a stated rule, and `memory.scala` in particular is one refactor away from breaking — the
moment either side of that comparison stops coming from `listFiles`, it joins this issue.

## How to reproduce it

On Windows, against any checkout of this repo:

```
> tt links check C:\path\to\genscalator
docs\manual-src\index.md -> getting-started.md
docs\manual-src\index.md -> allowlist.html
...
links check: 309 dangling of 348 local link(s) in NN file(s)
```

Expected: `0 dangling`, matching what the same command reports on Linux against the same commit.

The backslashes in the `from` column are the visible tell, and they are also what makes the output
non-comparable with the checked-in `.links.ignore`, whose entries are written with forward slashes.

On POSIX the defect is unreachable, so a fix must leave Linux and macOS byte-identical.

## Acceptance sketch

* **Normalize at the boundary, once, rather than at each consumer.** `scanDir` and `inventory` are
  the only three places a `Path` becomes a `String` (`:281`, `:299`, `:301`). Appending
  `.replace('\\', '/')` at those three sites makes every downstream key forward-slash on every
  platform, and the five forward-slash-assuming sites become correct without being touched. Fixing
  the consumers individually instead would be the fifth repetition of the mistake this issue is
  about.
* **Keep `excuse`'s own normalization.** After the boundary fix it is redundant, but it is
  defensive, it is free, and its comment and test are the record of how this was found. Removing it
  would delete the documentation of the defect class.
* **Fix `ssg.scala:550` by staying in the `Path` domain, not by normalizing a string.** The
  boundary trick does not apply there — `referenced` is a regex capture, not an inventory — so the
  right shape is the one `lib.scala` already uses: build `referenced.map(outFigures.resolve)` as a
  `Set[Path]` once and test membership on `p` directly. `Path.resolve` reads a forward-slash `rel`
  correctly on every platform, and `Path` equality is the filesystem's own, so the comparison stops
  depending on how either side spells a separator. A `.replace('\\', '/')` at that line would also
  work and would be the wrong lesson.
* **A path compared against text from outside the filesystem must be compared as a `Path`.** That
  is the rule the sweep's four safe sites keep and the two broken ones do not, and it belongs
  somewhere every tool author sees it — `tools/README.md` or the `scala-style` skill — because it
  is what would have stopped `ssg.scala:550` and what `memory.scala:70` currently satisfies only by
  accident of both sides sharing one helper.
* **Say which convention is canonical, in one place.** The repo-relative key is a forward-slash
  string on all platforms; anything that constructs one owes that guarantee. A line in the header
  comment next to the existing two KNOWN LIMIT notes is the natural home, since a future verb that
  inventories the tree will otherwise reintroduce this.
* **Test it as a pure unit, not as a Windows run.** The boundary is the only effectful part; the
  assertion that matters is that `resolve`, `referents` and `pathTokens` agree with the inventory's
  key shape. A test constructing backslash keys and asserting resolution still lands — the shape of
  the existing `:229` test, widened past `excuse` — runs green on Linux and would have failed
  before this fix. The live Windows run belongs on the next hardware sweep's checklist rather than
  in the suite.
* **Re-run `tt links check` on Windows against the fix** and confirm `0 dangling`, matching Linux
  on the same commit. That is the countable that must move: 309 to 0, not merely "looks right".
* Out of scope: any change to what counts as a reference, to the three reference shapes, or to the
  `.links.ignore` format. This is a key-normalization defect only.

## Discussion

### Comment by bjornregnell/Opus5 at 2026-08-25 20:10

Filed from @hmiddelk's field observation on PR #5, which supplied the symptom, the platform, the
counts and the backslash tell. The mechanism here was not inherited from that report: it was read
from `links.scala` at `main` after the PR merged, following the same rule 047 was filed under.

The `excuse`-fixed-it-once finding is the part worth keeping. @hmiddelk read it as "a real
path-resolution problem in the shipped binary rather than a repo problem", which is right, but the
repo half is the more interesting one: the project had already met this defect, written down why it
happens, fixed the one site it was standing on, and shipped a test that locks in that single site.
Three issues in this batch — 040, 042 and now 048 — are the same failure of a fix stopping at the
first instance, which is an argument for 041's derive-over-assert conclusion reaching past
descriptions.

Not verified: nothing here was run on Windows. The 309/348 counts are @hmiddelk's, unreproduced;
what I checked is that the five forward-slash sites and the three `relativize().toString` sites are
as cited at `main`, that `excuse` is the only normalizing site, and that `links.test.scala:229` is
the only separator-aware test of the suite's 30. The claim that exactly 39 links survive is an
inference from his total rather than a measurement, and the specific dangling lines in the
reproduction above are illustrative, not a transcript.

Agent disclosure: an AI agent (Claude Opus 5) located the mechanism and drafted this issue under
human direction; the human reviewed and submitted.

### Comment by bjornregnell/Opus5 at 2026-08-25 20:36

Swept the rest of the toolbox for this defect class rather than leaving the issue at one tool,
since a fix stopping at its first site is what 040, 042 and this issue all report. Eight
`relativize` sites exist in `tools/`; the sweep also picked up the path-string comparisons around
them. Results are in the two sections above: one further defect (`ssg.scala:550`, latent today but
destructive when it fires), four safe sites, and a one-line discriminator that separates them.

Two things the sweep changed about this issue. First, it is no longer a `links` issue — the label
set and the acceptance sketch now carry `ssg`, and the sketch says explicitly that the two need
*different* fixes, because `ssg`'s comparison has no inventory to normalize at a boundary. Second,
`lib.scala:200` turns out to already do this correctly with a filesystem-aware `PathMatcher`, so
the repo contains its own worked example — which makes this a case of the right pattern existing
in-tree and not being reached for, rather than a pattern nobody had found yet. That is the same
finding as `excuse` normalizing once, one level up.

Not verified: nothing was run on Windows, here or for the `ssg` half. The `ssg` defect is read from
`:521`, `:541-551` and the flat-figures state of `media/blog/figures`; I did not construct a nested
figure and render it, which is the cheap experiment that would settle it and belongs in the fix's
test rather than in this issue.

Agent disclosure: an AI agent (Claude Opus 5) ran the sweep and drafted this comment under human
direction; the human reviewed and submitted.
