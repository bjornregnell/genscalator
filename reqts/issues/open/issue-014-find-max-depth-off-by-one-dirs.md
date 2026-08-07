# Issue 014: `tt find --max-depth` is off by one for `--type d`, and the `--help` example is wrong

> status: open · labels: toolbox, find, docs · summary: `tt find <root> --type d --max-depth 1` returns
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
