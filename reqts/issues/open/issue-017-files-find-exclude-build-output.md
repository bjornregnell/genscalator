# Issue 017: `tt files` / `tt find` cannot exclude build output, so generated files dominate every scan

> status: open · labels: toolbox, files, find, ergonomics · summary: dot-names are skipped but `target/`,
> `out/`, `node_modules/` are not, and there is no `--exclude` and no `.gitignore` awareness — on a Scala
> repo the generated tree crowds out the sources you asked for.

## Description

Found 2026-07-29 in an alpha field test against a large Scala repo. Enumerating sources returns build
output mixed in, with no way to filter it out:

```
$ tt files <abs-repo> scala                  # 505 files, ~44KB of output
  <abs-repo>/target/translations-GENERATED.scala
  <abs-repo>/target/headings-GENERATED.scala
  ...
```

For some extensions the generated tree is *all* you get, which is actively misleading:

```
$ tt find <abs-repo> --ext .json --max-depth 3
1 matches
  <abs-repo>/project/target/active.json      # the ONLY hit is a build artifact
```

Both tools already skip dot-names (`.git`, `.scala-build`) — the right call, and documented. But
`target/`, `project/target/`, `out/`, `build/`, `node_modules/` are not hidden, and there is no
`--exclude <glob>` and no `--respect-gitignore`. `--max-depth` is not a workaround: build output is
shallow while sources are deep, so any depth bound that removes `target/` also removes the code.

**Why it wedges.** These two are the toolbox's primary answer to `find`/`grep -rl`, and the guard-clean
reflexes teach them as *the* file-finding move — so this is the hot path. On any JVM/Node repo the first
scan is polluted, and the failure is quiet: an agent reading "the only `.json` is `project/target/active.json`"
draws a wrong conclusion about the repo rather than noticing a missing filter. A generated
`*-GENERATED.scala` in a source list can also send an agent off to edit a file that gets overwritten by
the next build.

## How to reproduce it

On any repo with a populated `target/` (e.g. after `tt sbt --dir <abs-repo> compile`):

```
$ tt files <abs-repo> scala                  # generated sources included, no way to exclude
$ tt find <abs-repo> --ext .json --max-depth 3
$ tt files --help                            # no --exclude
$ tt find --help                             # no --exclude, no gitignore awareness
```

## Acceptance sketch

* `--exclude <glob>` on both tools, repeatable, matched against the repo-relative path so a whole subtree
  can be dropped (`--exclude 'target/**'`).
* And/or `--no-ignored`, honouring the repo's `.gitignore`.
* A curated default skip-list (`target`, `out`, `build`, `node_modules`) would fix the 90% case without
  any flag, in the same spirit as `tt log`'s curated default markers — provided it is documented and
  overridable (`--all`, or `--no-default-excludes`), so the tool never hides files without saying so.
* Whatever the default, the count line makes exclusions visible rather than silent, e.g.
  `505 files (112 excluded by default: target, out)`.

## Discussion

### Comment by hmiddelk/Opus5 at 2026-07-29 23:27

Filed from the same alpha field test as issues 014–016. Thematically adjacent to issue 011 (an ignore
mechanism for `tt links check`): both are "the checker needs a principled way to record what it should
not look at, and an exemption should be documentation rather than silence". Worth deciding once whether
exclusion is a per-tool flag or a shared repo-level convention the toolbox reads, so `links check`,
`files` and `find` do not each grow their own dialect.

Agent disclosure: found and drafted by an AI agent (Claude Opus 5) under human direction; the human
reviewed and submitted.

### Comment by hmiddelk/Opus5 at 2026-08-04 16:12

**CONFIRMED on Windows 10, including the misleading symptom.** Released `v0.10.0` native
`windows-x86_64` build, Windows 10 Enterprise 10.0.19045.

Neither `--help` offers `--exclude`, `--no-ignored`, or any `.gitignore` awareness — a search of both help
texts for `exclude|ignore|gitignore|target` returns nothing.

Reproduced on a purpose-built fixture rather than a large repo, which makes the ratio explicit — one real
source against a populated `target\`, `project\target\`, and `node_modules\`:

```
> tt files <fx> .scala
3 files
  <fx>\node_modules\pkg\index.scala          # vendored
  <fx>\src\Main.scala                        # the one real source
  <fx>\target\translations-GENERATED.scala   # generated

> tt find <fx> --ext .json --max-depth 3
1 matches
  <fx>\project\target\active.json            # the ONLY hit is a build artifact
```

The second one is the case this issue calls quiet: 1 of 1 hits is generated, and the output contains
nothing that would prompt a reader to doubt it. Note the fixture was named `translations-GENERATED.scala`
deliberately, to check the `*-GENERATED*` hazard named above — it is returned in a plain source listing
with no marking, so the "agent edits a file the next build overwrites" path is open on Windows too.

Dot-name skipping works correctly here (verified separately while re-testing issue 014: 3 dot-dirs
skipped out of 12), which is what isolates this to the non-dot build dirs — the diagnosis holds
unchanged, as does the acceptance sketch.

Agent disclosure: the Windows re-test was run and written up by an AI agent (Claude Opus 5) under human
direction; the human reviewed and submitted.

### Comment by bjornregnell/Fable5 at 2026-08-07 15:56

Maintainer-side review (PR 2 triage): confirmed, and the issue UNDERSTATES its own case. The
premise "both tools already skip dot-names" holds only for `tt find` — `tt files` has NO pruning
at all (`files.scala:38` is a raw `Files.walk`; the only filters are regular-file + extension).
Measured on this repo: a `.json` sweep returns 258 hits via `tt files` vs 5 via `tt find`, and 253
of the 258 are dot-dir cache internals — 98% noise; the `.scala` sweep includes 10 `.scala-build`
generated files plus a build copy duplicating a real source, so the edit-the-copy hazard is open
on Linux too. `tools/.scala-build/` alone holds ~39k files; pruning is also a real perf win
(~0.4s vs ~0.03s on that subtree).

Triage: SPLIT. Defect half, v0.10.1 — give `tt files` the sibling's dot-dir pruning (factor
`find.scala:78-90`'s visitor into `lib.scala`, add `--all` for parity, note the skip in the help).
Enhancement half, v0.10.2 — the curated non-dot skip-set (`target`, `out`, `build`,
`node_modules`) with repeatable `--exclude` and DISCLOSED exclusion counts, designed once together
with the shared-convention question issue-011 cross-links. Coverage note: neither tool has a
dedicated test file today — the fixture-tree tests in the acceptance sketch are the right start.

Agent disclosure: this review comment was produced by an AI agent (Claude Fable 5) under human
direction; the human reviewed and submitted.

### Comment by bjornregnell/Fable5 at 2026-08-07 20:28

DEFECT HALF SHIPPED in `8fb28af` (v0.10.1), issue stays OPEN re-scoped to the enhancement half.
What shipped: the pruning visitor is factored into `Lib.walkPruned` and shared by `tt find` and
`tt files` — so `files` now skips dot-named subtrees whole, gains `--all` for parity, and the
skip is disclosed in its help; a fixture test pins the sibling parity so the two cannot drift
again. Remaining scope for v0.10.2, as triaged: the curated non-dot skip-set (`target`, `out`,
`build`, `node_modules`), repeatable `--exclude`, DISCLOSED exclusion counts on the count line,
and the design-once question with issue-011's ignore mechanism.

Agent disclosure: this comment was produced by an AI agent (Claude Fable 5) under human
direction; the human reviewed and submitted.

### Comment by bjornregnell/Fable5 at 2026-08-09 14:36

ENHANCEMENT HALF SHIPPED in `53edb60` (v0.10.2 wave) — both halves of this issue are now in.
The shared walker takes a Prune policy and returns a PruneReport: curated skip-set (`target`,
`out`, `build`, `node_modules`) pruned like dot-names; repeatable `--exclude <glob>` on both
tools, root-relative java.nio glob, a trailing double-star glob pruning the whole subtree before
the walk descends; every non-dot exclusion DISCLOSED on the count line, formatted in one shared
place so the siblings cannot drift, plain line when nothing was excluded. `--all` now means
everything: hidden entries AND the curated skips. Counting semantics documented in both helps: a
pruned subtree counts as ONE entry (counting its contents would cost exactly the walk the pruning
saved). Contract tests in `9e9f3f8`; `tools/README.md` updated in `cb660d4`. Deliberately NOT
built, per triage: gitignore awareness — the repo-level ignore convention stays with issue-011,
where links check, files and find can settle one dialect together. Ready to close on the
maintainer's sweep; the shared-convention thread continues in issue-011.

Agent disclosure: this comment was produced by an AI agent (Claude Fable 5) under human direction;
the human reviewed and submitted.
