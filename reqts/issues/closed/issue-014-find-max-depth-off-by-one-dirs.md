# Issue 014: `tt find --max-depth` is off by one for `--type d`, and the `--help` example is wrong — CLOSED: fixed in the v0.10.1 batch

> status: closed 2026-08-07, fixed by `8fb28af` · labels: toolbox, find, docs · summary: `tt find <root> --type d --max-depth 1` returns
> ONLY the root, not the immediate sub-directories its own `--help` example promises; the shift is
> `--type d` relative to `--type f`, and `--max-depth 0` returns nothing despite the documented
> "root = 0".

## Description

Found 2026-07-29 in an alpha field test of the toolbox against a large third-party Scala repo
(`lunduniversity/introprog` — 1671 directories, 505 `.scala`, 35 `.md`, 7 `.html`).

`tt find --help` documents the flag and gives an example:

```
find <root> --max-depth N            descend at most N levels below <root> (root = 0)
...
  tt find . --type d --max-depth 1         # immediate sub-directories
```

The example does not return immediate sub-directories. It returns only the root. Immediate
sub-directories require `--max-depth 2`.

The discrepancy is **not a uniform shift** — it is `--type d` relative to `--type f`. An entry sitting
directly inside the root is reached at `--max-depth 1` if it is a file, but needs `--max-depth 2` if it
is a directory:

| invocation | actual | GNU `find` equivalent |
|---|---|---|
| `--ext .md --max-depth 1` | root-level files — correct | same |
| `--type d --max-depth 0` | 0 matches | root (docs say "root = 0") |
| `--type d --max-depth 1` | the root only | root + immediate sub-dirs |
| `--type d --max-depth 2` | root + immediate sub-dirs | one level deeper |

Probable cause: a directory is only emitted once the walk *enters* it (at depth+1), so directories pay
one extra level of descent that files do not.

**Why it wedges.** Silent under-collection is the expensive failure mode for a search tool, and this is
the case where following the tool's *own documented example* yields a confidently wrong answer — an agent
concludes "this repo has no sub-directories" and moves on. It cost real confusion in the field test: the
result was initially read as a repo-structure fact rather than a tool defect. The blast radius is small
but the trust cost is not, because `--help` is the contract.

## How to reproduce it

Against any repo whose root has sub-directories (here: 21 immediate sub-dirs):

```
$ tt find <abs-repo> --type d --max-depth 0
0 matches

$ tt find <abs-repo> --type d --max-depth 1
1 matches
  <abs-repo>                       # the root only — NOT the immediate sub-dirs

$ tt find <abs-repo> --type d --max-depth 2
22 matches                          # root + the 21 immediate sub-dirs
  <abs-repo>/about
  <abs-repo>/autotranslate
  ...
```

Files are unaffected, which is what makes the inconsistency visible:

```
$ tt find <abs-repo> --ext .md --max-depth 1
2 matches
  <abs-repo>/README.md
  <abs-repo>/migration.md          # root-level FILES at depth 1 — correct
```

Current (defective) behaviour pinned with the toolbox's own driver:

```
$ tt verify --out "1 matches" -- tt find <abs-repo> --type d --max-depth 1 --count
=== ran: tt find <abs-repo> --type d --max-depth 1 --count (exit 0, 36 ms)
=== PASS
```

## Acceptance sketch

* `--type d --max-depth 1` lists the root plus its immediate sub-directories, matching `--type f` depth
  accounting and GNU `find -maxdepth 1 -type d`.
* `--max-depth 0` has a defined, documented meaning consistent with "root = 0".
* A CLI-contract test pins depth semantics for BOTH `--type f` and `--type d` on the same fixture tree,
  so the two can never drift apart again.
* If the current behaviour is instead deemed correct, the `--help` text and its example are corrected —
  but aligning with GNU `find` is preferred, on least-surprise grounds.

Regression assertion after the fix (on a root with 21 immediate sub-dirs):

```
tt verify --out "22 matches" -- tt find <abs-repo> --type d --max-depth 1 --count
```

## Discussion

### Comment by hmiddelk/Opus5 at 2026-07-29 23:27

Filed from an alpha field test sweeping the toolbox over `lunduniversity/introprog`. Tested with the
released alpha native build (`~/.genscalator/bin/tt`, ELF, built 2026-07-29T12:21) on Ubuntu 24.04.2 LTS,
kernel 6.8.0-136-generic, x86-64, OpenJDK 21. Path/depth logic only, so it should reproduce on any
platform.

Agent disclosure: the sweep, the repro reduction and this issue text were produced by an AI agent
(Claude Opus 5) under human direction; the human reviewed and submitted.

### Comment by hmiddelk/Opus5 at 2026-08-04 16:12

**CONFIRMED on Windows 10, identical behaviour** — the "should reproduce on any platform" expectation
above now has second-platform evidence. Retested with the released `v0.10.0` native `windows-x86_64`
build (`C:\Users\<user>\.genscalator\bin\tt.exe`) on Windows 10 Enterprise 10.0.19045, x86-64.

Root used: the plugin cache directory, ground truth **9 non-dot immediate sub-directories** (12 total,
3 dot-named and correctly skipped per the documented dot-skip) and **6 root-level `.md` files**.

| invocation | Windows result | matches the Linux table above |
|---|---|---|
| `--type d --max-depth 0` | `0 matches` | yes |
| `--type d --max-depth 1` | `1 matches` (the root only) | yes |
| `--type d --max-depth 2` | `10 matches` (root + all 9 sub-dirs) | yes |
| `--ext .md --max-depth 1` | all 6 root-level files, correct | yes |

So the `--type d` vs `--type f` asymmetry is byte-for-byte the same, and `--max-depth 0` returns nothing
here too. Nothing platform-specific: the acceptance sketch and the regression assertion apply unchanged.

Agent disclosure: the Windows re-test was run and written up by an AI agent (Claude Opus 5) under human
direction; the human reviewed and submitted.

### Comment by bjornregnell/Fable5 at 2026-08-07 15:56

Maintainer-side review (PR 2 triage), independently verified on a third environment (Linux dev box):
all four table rows reproduce byte-for-byte on a fresh fixture tree and on this repo itself. Root
cause located: `find.scala:78-87` — JDK `walkFileTree` delivers directories at exactly `maxDepth` to
`visitFile` (not `preVisitDirectory`), and at `maxDepth 0` even the root; `visitFile` emits only
`typ == "f"` with no `isDirectory` check. The issue's "probable cause" is right in effect; this is
the precise mechanism.

One defect this issue missed, SAME root cause, strengthening the case: boundary directories leak
INTO `--type f` output — `tt find <root> --max-depth 1` with no ext filter lists the immediate
sub-directories as if they were files. A single type-aware branch in `visitFile` fixes both at once.

Triage: DEFECT, targeted at v0.10.1. Coverage note: existing tests never combine `--max-depth` with
`--type d` (`cli.test.scala:301-368`), which is why this drifted — the acceptance sketch's paired
test is the right addition, and its `22 matches` regression arithmetic is sound under the fix.

Agent disclosure: this review comment was produced by an AI agent (Claude Fable 5) under human
direction; the human reviewed and submitted.

### Comment by bjornregnell/Fable5 at 2026-08-07 20:28

CLOSED as fixed by `8fb28af`: a type-aware branch in the walker's `visitFile` (directories at
exactly max-depth arrive there, not in `preVisitDirectory`) fixes both this issue and the
same-root-cause bonus defect the triage found — boundary dirs no longer leak into `--type f`
output. Depth accounting now matches GNU find: `--type d --max-depth 1` = root + immediate
sub-dirs, `--max-depth 0` = the root itself (nothing for `--type f`, since the root is not a
regular file). The acceptance sketch's paired contract test is in (`cli.test.scala`, one fixture,
both types), and the whole suite ran green. In v0.10.1 the walker moved into the shared
`Lib.walkPruned` (issue-017), so find and files carry one depth semantics, not two.

Agent disclosure: this closing comment was produced by an AI agent (Claude Fable 5) under human
direction; the human reviewed and submitted.
### Comment by hmiddelk/Opus5 at 2026-08-21 19:05

**RE-VERIFIED FIXED on Windows 10 against the released `v0.10.2`** — this closes the loop on the
2026-08-04 confirmation above, which recorded all four table rows reproducing on this same box.

Environment: Windows 10 Enterprise 10.0.19045, x86-64; released `v0.10.2` native `windows-x86_64`,
install `VERSION.txt` = `v0.10.2`, zip sha256 `7b5fcae6…9b7d`, `bin/tt.exe` 41.1M. Named by
`VERSION.txt` + hash since `tt --version` still exits 2 at this release (issue 028).

Fixture with exact ground truth, measured with PowerShell before the tool was run: **4 non-dot immediate
sub-directories** (`a b c d`), 2 dot-named (`.dot1 .dot2`), **2 root-level `.md`** (`r1.md r2.md`), and one
nested `a\deep`.

| invocation | v0.10.0 (2026-08-04) | v0.10.2 | GNU `find` |
|---|---|---|---|
| `--type d --max-depth 0` | `0 matches` | **`1 matches`** — the root | agrees |
| `--type d --max-depth 1` | `1 matches` (root only) | **`5 matches`** — root + all 4 | agrees |
| `--type d --max-depth 2` | root + immediate only | **`6 matches`** — adds `a\deep` | agrees |
| `--ext .md --max-depth 1` | 2, correct | `2 matches`, still correct | agrees |

So `--max-depth 0` now means the root, `--max-depth 1` means root + immediate sub-dirs, and the
`--type d` / `--type f` asymmetry is gone.

**The bonus defect the triage found is fixed too**, and it was tested separately because this issue never
named it:

```
> tt find <fx> --type f --max-depth 1
2 matches
  <fx>\r1.md
  <fx>\r2.md
```

Boundary directories no longer leak into `--type f` output — at `--max-depth 1` the four immediate
sub-directories would previously have been listed as if they were files. Dot-name skipping is also intact:
`.dot1` and `.dot2` appear in none of the runs above.

The acceptance sketch's regression assertion holds in the form this fixture supports (`5 matches` on a root
with 4 immediate sub-dirs, the same arithmetic as the sketch's `22 matches` on 21).

Provenance: `research/reports/report088-windows-update-lifecycle-2026-08-21.md`, which carries the method,
coverage and threats to validity for the batch. Note the walker moved into the shared `Lib.walkPruned` in
v0.10.1 (issue 017), so this fixture also exercises the shared depth semantics rather than `find`'s own.

Agent disclosure: the re-verification was run and written up by an AI agent (Claude Opus 5) under human
direction; the human reviewed and submitted.
