# Report 086: Windows 10 replication of the alpha field test findings (2026-08-04)

- **Question:** do the eight findings of report 085 hold on a second platform, and does anything break on
  Windows that Linux could not have shown?
- **Why it matters:** every issue in 014–021 was filed from a single Ubuntu box, and three of them
  (015, 019, 021) are about *install shape* rather than pure logic — exactly the class where one platform
  is not evidence. A replication either promotes those findings from "observed once" to "cross-platform",
  or splits them.
- **Status:** shipped as Windows confirmations appended to issues 014–021, plus one new issue 022.
- **What shipped:** eight `## Discussion` comments (append-only, no existing line touched) and
  `issue-022-tt-which-splits-path-on-colon-windows.md`. As in report 085, the issue files are canonical
  for the findings and are deliberately not restated here.

## Environment

| | |
|---|---|
| Date | 2026-08-04 |
| Platform | Windows 10 Enterprise 10.0.19045 (build 19045) |
| Arch | x86-64 (`AMD64`) |
| Shell | PowerShell 5.1 (`SHELL` unset — relevant to the installer path, see below) |
| `tt` under test | released `latest` native build for `windows-x86_64`, `C:\Users\<user>\.genscalator\bin\tt.exe` (40.9M) |
| Release asset | zip sha256 `79f4c702bdc2107515eac2d5e24f8b62bf221c03ddcf9a1a56a9425de3f2c7c2`, 13 951 676 B, 30 files |
| Install `VERSION.txt` | `v0.10.0` |
| Plugin cache | `0.9.2` (skills + docs) — the binary and the plugin disagree; see issue 021 |
| Installed via | `scala-cli run get-genscalator.sc` (Scala CLI 1.16.0, Scala 3.8.4, Coursier) |
| JDK on PATH | OpenJDK 11.0.16.1 (not used by the native binary; recorded for completeness) |

Because the native binary needs no JVM and no scala-cli at runtime, the toolchain above matters only for
the bootstrap. Worth stating plainly: **the native `windows-x86_64` build runs on Windows** — that is the
precondition for this whole report and it was never in doubt during the sweep.

Version note: identifying the artifact took the sha256 plus `VERSION.txt` rather than a version string,
for the reason issue 021 gives. Every comment filed in this batch therefore names the build by platform
and hash. First attempt at naming it picked up `0.9.2` from the plugin and was wrong.

## Method

**Replication, not a fresh surface sweep** — this is the methodological difference from report 085 and it
sets the scope. The changed factor is the platform; the protocol was: take each of 014–021, re-run its
documented "How to reproduce it" on Windows, and classify as confirmed / not-reproducible / changed.
Only after that did anything exploratory happen, and only in the one place a Windows-shaped failure
appeared (`tt which`).

Two deliberate design choices, each with a cost:

- **Synthetic fixtures instead of a third-party repo.** Report 085 swept `lunduniversity/introprog`; this
  test used purpose-built trees (a `target\` + `project\target\` + `node_modules\` fixture for 017, a
  500-paragraph HTML page for 016, a 0-byte file for 018) plus two real directories for 014. Gain: exact
  ground truth, so a count is a verdict rather than an estimate — 9 non-dot sub-directories, 6 root-level
  `.md`, 1 real source against 2 generated. Cost: **ecological validity**. Nothing here reproduces the
  scale effects of a 1671-directory repo, so this report can confirm 085's findings but cannot claim to
  have looked for new ones in the same conditions.
- **Root-cause reduction was pursued, not just symptom capture.** For `tt which` the four observed
  symptoms were reduced to one hypothesis (`$PATH` split on `':'`), tested by shrinking `PATH` to a single
  entry and predicting the reported count in advance — it printed `2 dirs`, as predicted — and then
  located in source at `which.scala:98`, with `git diff v0.10.0 HEAD -- tools/which.scala` confirming the
  cited line is the one in the tested binary. A prediction made before the observation is worth more than
  four symptoms described after it.

## Findings

Pointers only; the issue files are the single source of truth.

| issue | Windows 10 verdict | note |
|---|---|---|
| 014 | **confirmed, identical** | `--type d` depth shifted by one, `--max-depth 0` empty, files correct |
| 015 | **confirmed, identical** | exit 2, no sibling `skills\` or `tools\` in the install tree |
| 016 | **confirmed** | no cap flag; 500 paragraphs → 1000 lines, no truncation notice |
| 017 | **confirmed** | incl. the misleading case: the only `.json` hit is a build artifact |
| 018 | **confirmed, extended** | a 0-byte file is byte-identical to a clean build (new data point) |
| 019 | **confirmed, with a caveat** | doc gap holds, but the remedy it proposes cannot work on Windows |
| 020 | **confirmed, all three** | `--help` fails like `help`; `tt find` mentioned 0× in the digest |
| 021 | **confirmed, extended** | the two install trees on one machine now disagree (`v0.10.0` vs `0.9.2`) |
| **022** | **new — Windows-only** | `tt which` splits `$PATH` on `':'`; no PATHEXT; no `MZ` magic |

8 of 8 replicated. That is the headline, and it is unsurprising in hindsight: seven of the eight are path
arithmetic, missing flags, doc text, or release metadata — nothing touching a platform API. The
replication earns its keep on the other two counts, by promoting the install-shape findings (015, 019,
021) to cross-platform and by finding 022.

Two findings gained something beyond confirmation, which is the argument for replicating rather than
assuming: **019 became conditional** (its acceptance sketch should not land before 022, or it teaches
Windows users to trust a tool that lies to them), and **021 gained a symptom** (the CI-stamp fix has taken
effect for the native tree, which is what makes the two trees disagree on one machine, and it exposes a
`v`-prefix mismatch that any future version-agreement gate will have to settle).

## Coverage — what was NOT tested

Narrower than report 085 by design, and the honest denominator matters more than the ratio.

Invoked on Windows: **7 of 45** tools — `doc` `files` `find` `htmltext` `log` `skillcheck` `which`.
Plus the dispatcher itself (`tt help` / `tt --help`, issue 020 item 1), which is not a tool.

**Not tested: 38 of 45.** No claim of any kind is made about them on Windows. The gap is much larger than
085's because this was a targeted replication: the tools above are exactly those that 014–021 name.

Highest-value gaps, in priority order for the next Windows pass:

1. **`tt scala` / `tt sbt` / `tt bloop`** — the driver tools. These spawn subprocesses, and
   `tools/test/cli.test.scala:29` and `dispatch.test.scala:48` both carry Windows PATHEXT warnings about
   `ProcessBuilder`, so this is the area where 022's root cause is most likely to have siblings. Not a
   guess worth filing, but the obvious place to look.
2. **The test suite itself** — `gs test` was never run on Windows. scala-cli is now installed on this box,
   so it is feasible; it was skipped because the suite is not part of the released install tree
   (issue 015) and pointing it at the plugin cache tests `0.9.2`, not the `v0.10.0` binary under test.
3. **`tt statusline` / `box`** — the box line is documented Linux-only (`/proc`, `/sys`); confirming it
   degrades silently rather than erroring on Windows is a small, cheap check that was not done.
4. `tt env`, `tt limit`, `tt session`, `tt mode` — the state-file tools, which touch `~` resolution.

Also untested here: everything effectful or outward-facing (`forge` `ssg` `serv` `web` `git` `zip`
`harden`), held back on the same grounds as report 085.

## What worked well

Recorded for the same reason report 085 records it.

| thing | verdict |
|---|---|
| the native install | The whole point of the fast path, and it delivers on Windows: a single self-contained `tt.exe`, no JVM and no scala-cli at runtime, sha256 verified against the published hash before unpacking. |
| `get-genscalator.sc` | Verified the asset hash, unpacked 30 files, and — with `SHELL` unset — printed the exact `[Environment]::SetEnvironmentVariable(...)` snippet a PowerShell user needs. `--dry-run` was honest: re-running it wrote nothing. The one-file, no-dependency, read-before-you-run design held up under an actual read. |
| `tt find` / `tt files` | Correct on Windows apart from the known 014 depth bug: paths rendered with drive letters and backslashes throughout, dot-name skipping exact (3 of 12 skipped), extension matching and the content-regex path both fine. |
| `tt doc` | The pre-baked-paste design (`gs help`, `gs status`, the guard digest) works unchanged on Windows and was the fastest thing in the sweep. |
| `tt log` | The `v0.10.0` not-a-file error has gained a `(resolved: ...)` clause since 085 — a real improvement, and it made the 020 item-2 check unambiguous. |

## Corrections made during the sweep

Two claims were made to the human and then withdrawn before anything was filed. Recorded so the filed
findings can be trusted, and because both corrections changed what would have been reported.

- **Retracted: "the installer fails to set PATH on Windows."** This was reported as a
  Windows-specific installer defect and was the original reason for opening a Windows issue at all. It is
  wrong. Re-running with `--dry-run` showed the installer detects the situation and prints the correct
  PowerShell command for the user to run; `~/.genscalator/bin` was absent from PATH because that printed
  instruction had not been followed, not because the installer failed. What survives is two cosmetics,
  filed as a note inside issue 022 rather than as an issue: it labels a supported platform
  `PATH: unrecognised shell ''`, and its closing instruction is `then run: tt help`, which per issue 020
  exits 2 — so a clean install's first suggested command prints an error.
- **Reduced: four `tt which` faults to one root cause.** The first pass reported the drive-letter
  stripping, the bogus dir count, and the two not-found results as separate symptoms. They are one bug:
  the `':'` split. PATHEXT is genuinely independent; the `MZ` magic gap and the `?` mode column are
  cosmetic. Filing the first version would have described a mess instead of a one-line fix.

Both corrections came from doing the cheap verification rather than trusting a plausible reading — the
installer one from re-running a tool in dry-run mode, the `which` one from predicting a count before
measuring it.

## Duplicate check

Searched `open/` and `closed/` before filing. 022 is the next free number (highest existing 021, across
both directories). No existing issue covers `$PATH` parsing or Windows portability: 003 is the native
*rebuild ritual*, 015 is what the built install tree *contains*, 019 is the *documentation* of the
which-`tt`-wins check — 022 is why that check cannot work on Windows, and is cross-referenced from both
015 and 019. Grep for the same defect class elsewhere in `tools/` found one other `split(":")`
(`ssg.scala:353`), which parses a tool-defined `key:value` spec and is not `$PATH` — recorded in 022 so
the next reader does not have to redo the search.

## Threats to validity

- **Single machine, single Windows version.** Windows 10 Enterprise 19045 only. Windows 11 untested;
  Windows-on-ARM has no published binary at all (the installer says so and refuses, correctly).
- **`n = 1` for the platform claim, still.** This report doubles the platform count from one to two; it
  does not establish general portability. The 38 untested tools are the bulk of the surface.
- **Confirmation-shaped protocol.** Re-running eight known reproductions is a test biased toward finding
  what it is looking for. Nothing here was designed to find Windows defects in the tools 014–021 do not
  mention, and 022 was found only because a re-test walked into it. A Windows sweep in the style of
  report 085 remains undone, and would be a different study.
- **Fixtures over a real repo**, as set out under Method — exact ground truth bought at the cost of
  realism.
- **Doc/digest content read from the `v0.10.0` binary** (`tt doc ...`), but the shipped *skills* on this
  box are `0.9.2`. Where a finding concerns doc text (019, 020 item 3) the binary's copy is the one
  quoted, which is the right one for the artifact under test.
- **Not independent of the original.** Same human, same agent model, same reporting conventions as
  report 085. A replication by a different pair would be worth more than this one on the findings that
  are judgement calls (016, 017, 019 are gaps and docs, not crashes).

## Notes for the next field test

- **Fix 021 first, still.** Report 085 said this; a week later it cost this test the same tax, and every
  comment in this batch had to spend a sentence on provenance instead of a version number.
- **022 before 019.** They are coupled: 019 wants to prescribe a ritual that 022 says is broken on
  Windows. Landing the doc change alone would be actively harmful.
- **A pure test would have caught 022 without a Windows box.** A unit test on `pathDirs` over a
  `';'`-separated, drive-lettered PATH string asserts the entry count and drive-letter survival, and runs
  anywhere. Worth asking of any function that parses an environment variable — the platform-shaped bugs
  reachable by pure tests are the cheap ones to own.
- **Windows deserves the 085 treatment.** A full surface sweep on Windows would probably find more of the
  022 class, and the driver tools are the place to start.

---

Reported by hmiddelk. An AI agent (Claude Opus 5) ran the replication, reduced the `tt which` root cause,
and drafted issue 022 and the eight confirmation comments under human direction; the human reviewed the
findings, verified issue numbering, and submitted. Two claims the agent made were withdrawn before filing
and are recorded above.
