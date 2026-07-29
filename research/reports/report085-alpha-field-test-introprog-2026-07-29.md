# Report 085: alpha field test of the `tt` toolbox on a third-party Scala repo (2026-07-29)

- **Question:** how does the released alpha toolbox hold up when swept tool-by-tool over a large
  third-party Scala repo that knows nothing about genscalator?
- **Why it matters:** field reports are the point of the alpha cut, and an outside human+agent pair
  working an unfamiliar repo probes the project-agnosticism claim directly — nothing here was built
  with this repo in mind.
- **Status:** shipped as issues 014–021.
- **What shipped:** eight in-repo issues (below). This report is the *provenance* record — the issue
  files are canonical for the findings themselves, and are deliberately not restated here.

## Environment

| | |
|---|---|
| Date | 2026-07-29 |
| Platform | Ubuntu 24.04.2 LTS (Noble Numbat), `VERSION_ID=24.04` |
| Kernel | Linux 6.8.0-136-generic, x86-64 |
| JDK | OpenJDK 21 |
| `tt` under test | released alpha native build at `~/.genscalator/bin/tt` (ELF, 42.3M, built 2026-07-29T12:21) |
| Shadowed on PATH | the plugin-cache launcher (bash script) — see issue 019 |
| Target repo | `lunduniversity/introprog` — Scala + sbt + markdown/LaTeX course repo, clean checkout |

Repo shape, measured with the tools themselves (so also a smoke test): 505 `.scala` (including
generated output under `target/`), 35 `.md`, 7 `.html`, 1671 directories, 21 top-level dirs.

Version caveat: the release advertises `0.9.2` in every in-repo carrier, so the build under test could
only be identified by provenance rather than a version string. That is issue 021, and it is the reason
the other seven issues cite a binary mtime.

## Method

Systematic sweep: invoke each tool against a real target in the repo, classify as
works / rough / broken / missing-verb, and capture the exact command and output for a reduction.
Effectful and outward-facing tools were held back deliberately.

Worth recording which findings came from where, because it says something about method: 014–017 and
019–020 surfaced from **enumerating the surface**, while 018 surfaced from **real use** — digesting an
actual `tt sbt` build log, which is exactly the situation that tool exists for. A help-text sweep would
never have found it. Evidence that a surface sweep and a use-it-for-real pass find different defect
classes, and that an alpha wants both.

## Findings

Pointers only — the issue files are the single source of truth, so this table does not restate them and
cannot drift from them.

| issue | severity | one-line |
|---|---|---|
| 014 | defect | `tt find --max-depth` off by one for `--type d`; the `--help` example is wrong |
| 015 | defect | `tt skillcheck` exits 2 on a native install with no sibling `skills/` |
| 016 | gap | `tt htmltext` has no output cap, and the guard rules forbid piping to `head` |
| 017 | gap | `tt files` / `tt find` cannot exclude build output |
| 018 | gap | `tt log`'s verdict has no positive signal |
| 019 | docs | tell users to check which `tt` wins on PATH |
| 020 | polish | `tt help` alias, `tt log` naming hint, `tt find` missing from the guard-clean digest |
| 021 | release | the `v0.10.0` tag declares `0.9.2` and has no CHANGELOG section |

## Coverage — what was NOT tested

Exercised with a real invocation and/or `--help`: **26 of 45** tools — `bloop` `box` `chrono` `doc`
`env` `files` `find` `gitinfo` `guardcheck` `htmltext` `json` `limit` `links` `log` `md-fmt` `mode`
`sbt` `scala` `session` `skillcheck` `statusline` `text` `typo` `update` `verify` `which`

Of those, `chrono` `md-fmt` `scala` `session` `typo` were **help-only**, no real invocation. `limit`
ran but had no declared limits, so its display path is untested.

Not tested (19): `ascii` `forge` `git` `gvdot` `hangover` `harden` `memory` `newtool` `parsereqt`
`prd` `serv` `skillgrants` `ssg` `sub` `svg` `tsv` `web` `wr` `zip`

Reasons: effectful or outward-facing (`forge` `ssg` `serv` `zip` `harden` `git` write paths, and `web`,
which makes network requests) were held back; the dev-substrate tools (`prd` `parsereqt` `wr` `memory`
`hangover` `skillgrants`) have no substrate in a third-party repo; the rendering tools (`ascii` `svg`
`gvdot` `tsv`) had no natural target.

Highest-value remaining gap: `tt scala test|compile` against a scala-cli sub-project, which exercises a
different driver (`--server=false`) than the sbt path that was tested.

## What worked well

Recorded because a report that lists only complaints misleads about the state of the toolbox.

| tool | verdict |
|---|---|
| `tt which` | Best in show. Full PATH resolution with shadowing, symlink chains, ELF-vs-script, size and mtime — surfaced the entire native-vs-plugin-cache situation in one call, which is what issue 019 is about. |
| `tt sbt` | Passed the hardest real test: `--dir <repo> compile` → exit 0 in 57s, 2 Scala sources compiled (Scala 3.8.3) across two sub-projects. Directory via `ProcessBuilder.directory()` with no shell `cd`, args and exit code passed through, closing audit line. Coexisted with a running sbt/Metals server by dropping to batch mode. |
| `tt guardcheck` | Caught both the banned `grep -rn` reflex (MED) and the interpreter-reach NOTE class, each with a concrete fix line. The severity split — HIGH/MED decide, NOTE only advises — is well judged. |
| `tt verify` | Turned a prose finding into an executable assertion. No-shell argv plus human-only `TT_VERIFY_ALLOW` widening (deliberately no flag) is a genuinely good safety design. |

Also solid, without incident: `links check` (correct — see the retraction below), `text grepr` (the
`--any` flag is a thoughtful fix for a real guard-stall trap), `box health`, `gitinfo`, `json pretty`
(the "it sorts keys, never write it back over a human's file" caveat is exactly right), and
`skillcheck --active` once pointed at the right dir (12/12 active, no silent outage).

## Corrections made during the sweep

Two claims were wrong and were retracted before filing, recorded so the rest can be trusted:

- `tt links check` was suspected of under-scanning at 42 files; verification showed it correct
  (35 `.md` + 7 `.html` = exactly 42).
- The toolbox was counted as 42 tools from the usage line; it is 45. The coverage denominator above is
  the corrected one.

## Duplicate check

Searched the existing set before filing; no overlap. 003 covers the native *rebuild ritual*, not what
the built install tree contains (distinct from 015). 011 wants an ignore mechanism for `links check` —
thematically adjacent to 017, cross-referenced there. 012 covers the release *workflow's* tag input and
`VERSION.txt` stamping — adjacent to 021, which is scoped to what the tagged tree declares about
itself; 021 says explicitly that the two may be merged.

## Notes for the next field test

- Two typed verbs were missing for this workflow: branch creation and commit amend are absent from
  `tt git` (which deliberately excludes the destructive verbs, but these two are not destructive), so
  raw `git` was needed for both. Not filed as an issue pending a view on whether they belong.
- The version-metadata gap (021) degraded every other report by removing the denominator. Fixing it
  first would make subsequent alpha reports cheaper to triage.

---

Reported by hmiddelk. An AI agent (Claude Opus 5) ran the sweep, reduced the reproductions and drafted
the issue text under human direction; the human reviewed the findings, verified issue numbering and the
link gate, and submitted.
