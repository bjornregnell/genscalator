# Report 088: the Windows update lifecycle, and the 014–022 re-verification (2026-08-21)

- **Question:** two things, in this order. (A) Can a Windows alpha tester run the loop the project
  prescribes — `install → test → uninstall → reinstall` — and end up on a known version with the binary
  and the plugin agreeing? (B) Do the nine findings of PR #2, all now shipped or closed, actually hold
  fixed on Windows hardware?
- **Why it matters:** report 086 tested the *tools* on Windows but could not test version comparison,
  because nothing removed an install (issue-039). The uninstaller now exists on `main`, and this box was
  carrying its designed input: a **pre-manifest v0.10.0 install**. That input disappears once everyone has
  upgraded. Separately, the v0.10.2 release notes name **issue-022's real-Windows verification as not run
  on hardware** — the outstanding blocker on closing it — and this is the box that can run it.
- **Headline:** **8 of 9 re-verified fixed on Windows; the 9th (019) is correctly absent from this
  release.** Issue **022 passes its full maintainer-drafted checklist and is ready to close.**
- **Status:** three issues filed and three Windows confirmations pending. See *Duplicate check* — the
  first draft of this report proposed five new issues, and three of them were already filed from Linux
  the previous day.
- **What shipped:** three issue files under `reqts/issues/open/`, one per PR, each branched independently
  off `main` at `7f03345`. Named rather than linked, since they arrive in separate PRs and a link would
  dangle until all four merge — the same reason report 086 names `issue-022-…md` without linking it:
  * `issue-043-readme-documents-an-uninstall-the-release-asset-cannot-do.md` — PR #7
  * `issue-044-installer-silently-ignores-unknown-flags.md` — PR #8
  * `issue-045-plugin-version-is-not-a-provenance-statement.md` — PR #9

  Plus **two Windows confirmations held pending merge** of the Linux issues they belong to: **040** (PR #4)
  and **042** (PR #6), both of which state they were not verified on Windows. Held rather than pushed into
  someone else's open branch. As in reports 085–087 the issue files are canonical for the findings and are
  deliberately not restated here.
- **The finding a user hits first is 043:** the front page documents an uninstall procedure that the
  artifact it tells you to download cannot perform. It was found by a human following the documented
  procedure and being surprised, not by the sweep — see *Corrections*.

## Environment

| | |
|---|---|
| Date | 2026-08-21 |
| Platform | Windows 10 Enterprise 10.0.19045 |
| Arch | x86-64 (`AMD64`) |
| Shell | PowerShell 5.1 (`SHELL` unset — still the fact that drives the installer's PATH branch) |
| Working dir | `C:\Users\<user>\testgenscalator` (not a git checkout) |
| Toolchain | Scala CLI 1.16.0, Scala 3.8.4; scala-cli provisioned **JVM 17** for the bootstrap |
| JDK on PATH | OpenJDK 11.0.16.1 — *not* what compiled the script; recorded because it misleads |
| Start state | `~\.genscalator` `VERSION.txt` = `v0.10.0`, 30 files, **no** manifest; plugin `0.9.2` @ `92caf86` |
| End state | `VERSION.txt` = `v0.10.2`, 30 files + manifest; plugin `0.10.2` @ `7f03345`; `tt help` exits 0 |
| `tt` under test | `C:\Users\<user>\.genscalator\bin\tt.exe`, 41.1M, released `windows-x86_64` v0.10.2 |
| Release zip | sha256 `7b5fcae61f2cae8da84decd82c7a2420a8a078aef08cf03b14cf730896ae9b7d`, 14 059 469 B, 30 entries |
| Uninstaller under test | `get-genscalator.sc` at `main` — **unreleased**, 360 lines, sha256 `BEEB62FF…3BA4` |
| Version provenance | by `VERSION.txt` + zip sha256; `tt --version` still exits 2 at this release (issue-028) |

## Method

**Arm A — lifecycle exercise on the machine's real state, not a fixture.** Deliberate: run the loop on the
box exactly as report 086 left it, rather than construct a clean subject. Gain: the pre-manifest fallback
was tested against a real pre-manifest install, the only population it will ever serve. Cost: **the start
state is not reproducible** — nobody can re-run this after upgrading.

**Arm B — replication of 086's protocol against the new release.** Take each of 014–022, re-run its
documented reproduction, classify fixed / not-fixed / not-in-this-release. Two rules carried over from 086
and one added:

- **Purpose-built fixtures with exact ground truth.** For 017 the fixture is a *deliberate byte-level
  replica of the one report 086 recorded* (`src\Main.scala`, `target\translations-GENERATED.scala`,
  `node_modules\pkg\index.scala`, `project\target\active.json`), so the before/after comparison is direct
  rather than analogous. Ground truth was measured with PowerShell independently of the tool under test.
- **Predict the number before running the tool.** Used in arm A before the deletion (below) and in arm B
  wherever a count was the verdict.
- **New rule, and it changed the report: test against the *native binary*, never the plugin's `tools/tt`.**
  Finding 044 is why — on this box the plugin's source is 21 commits ahead of the tag.

## Arm A: the command log

Every command, in order, with why it was run and what it settled. PowerShell unless marked.
Paths abbreviated: `~` = `C:\Users\<user>`, `TD` = `~\testgenscalator`, `GS` = `~\.genscalator`.

### Phase 0 — orientation

| # | command | reason | result |
|---|---|---|---|
| 1 | `Get-ChildItem -Force TD` | see what the test dir holds | one file: `get-genscalator.sc`, 14 183 B, dated 2026-08-04 |
| 2 | `Get-ChildItem -Force -Recurse TD\.genscalator` | check for a *local* install in the test dir | nothing — the install is under `~` |
| 3 | `Get-ChildItem -Force ~ -Directory` + `-Filter .genscalator -Recurse -Depth 2` | locate the real install root | `~\.genscalator` |
| 4 | Read `TD\get-genscalator.sc` (all 235 lines) | read-before-run, and learn the flags | flags are `--dry-run --no-path --home --tag`. **No `--uninstall`** — first signal for 043 |
| 5 | `gh pr view 3 --repo bjornregnell/genscalator --json …` | pull the field-test PR | wrong PR (that is report 087's); human redirected to #2 |
| 6 | `gh pr view 2 …` | get the actual subject | the Linux sweep + Windows replication, issues 014–022 |
| 7 | `gh release list --repo … --limit 12` | what is installable | v0.10.2 latest, then v0.10.1, v0.10.0, v0.9.2 |
| 8 | `Get-ChildItem -Force -Recurse GS`; `Get-Content GS\VERSION.txt` | identify the installed artifact | `v0.10.0`, 30 files, no manifest |
| 9 | `scala-cli version`; `java -version`; `Get-Command tt`; User `Path` filtered | can we bootstrap, and does `tt` resolve | scala-cli 1.16.0; JDK 11; `tt` → `GS\bin\tt.exe`; PATH entry present |
| 10 | `gh release view v0.10.2 --repo …` | read the target release's notes | notes state issue-022's Windows verification **has not been run on hardware** — this box's job |

### Phase 1 — carrier audit: does the released installer even have an uninstaller?

| # | command | reason | result |
|---|---|---|---|
| 11 | `Move-Item …sc …sc.stale-v0.10.0`; `gh release download v0.10.2 --pattern get-genscalator.sc --dir TD` | replace the stale script with the release asset | both 14 183 B — suspicious |
| 12 | `Get-FileHash` on both | is the "stale" file actually stale? | **identical** `A7EE81D3…845F`. The v0.10.2 asset *is* the v0.10.0 asset |
| 13 | `gh api …/contents/CHANGELOG.md?ref=v0.10.2` → grep `uninstall` | is uninstall in the release at all? | **0 hits** |
| 14 | `gh api …/git/trees/v0.10.2?recursive=1` → grep `install` | a separate uninstaller file in the tag? | no |
| 15 | `gh api …/contents/get-genscalator.sc?ref=main` | is the feature on `main`? | yes — 26 `uninstall` hits, 360 lines vs 235 |
| 16 | `gh api …/contents/CHANGELOG.md?ref=main` | which release is it slated for? | **Unreleased (v0.10.3 wave) → v0.10.4 candidates**, as issue-039 |
| 17 | `gh api …/contents/README.md?ref=main` → grep | what does the front page tell a user? | documents `--uninstall --force` **and** links `releases/latest/download/get-genscalator.sc` |
| 18 | `scala-cli run TD\get-genscalator.sc -- --uninstall --dry-run` | decisive: what does the released asset do with a flag it lacks? | prints **`genscalator bootstrap (DRY RUN)`**, resolves 30 files to unpack → **findings 043 + 044** |

Command 18 is why the human's `-- --uninstall --force` was not run as given: on the release asset it is not
an uninstall, it is an install. `--dry-run` made the probe non-destructive.

### Phase 2 — uninstall the pre-manifest v0.10.0 install

| # | command | reason | result |
|---|---|---|---|
| 19 | `Move-Item …sc …sc.release-v0.10.2`; `Copy-Item _get-genscalator-main.sc …get-genscalator.sc` | put the only carrier that *has* the feature in place, so the documented command works verbatim | working script = main's 23 713 B version |
| 20 | `scala-cli run TD\get-genscalator.sc -- --uninstall` | preview-before-force, as the tool intends | fallback warning fired correctly; **29 files** listed; `version: unknown` |
| 21 | `Get-ChildItem -Recurse -File GS` + filter for paths outside `bin\|docs\|skills\|tools\|plugins\|VERSION.txt` | ground truth vs the tool's claim, **before** deleting | **30 on disk vs 29 listed**; gap = `reqts\PRD.md`. Prediction registered: it will survive |
| 22 | `scala-cli run TD\get-genscalator.sc -- --uninstall --force` | apply — the human's requested command, on the carrier that implements it | removed 29; printed `kept: …\.genscalator still exists — it holds files this uninstaller did not put there` |
| 23 | `Get-ChildItem -Recurse -Force GS`; `Get-Command tt`; User `Path` grep | did it reach a clean box? | `reqts\PRD.md` (82 092 B) survived — **confirms issue 040**; `tt` gone ✓; PATH entry retained by design ✓ |
| 24 | `Remove-Item -Recurse -Force GS` | reach the clean box the loop requires, by hand | `~\.genscalator` gone |

Command 24 is the finding restated as an action: **the loop's third step needed a manual command to
complete** — precisely what issue-039 exists to remove.

### Phase 3 — reinstall v0.10.2

| # | command | reason | result |
|---|---|---|---|
| 25 | `scala-cli run TD\get-genscalator.sc` | install the newcomer way — no `--tag`, so the default path is what gets tested | verified sha256, unpacked 30 files, wrote `INSTALL-MANIFEST.txt`, printed the PowerShell PATH snippet |
| 26 | `Get-Content GS\VERSION.txt`; manifest header; listed-vs-on-disk counts | is the install identifiable, and is the manifest honest? | `v0.10.2`; **`installed-tag: latest`** — confirms issue 042's manifest half; 30 listed + itself = 31 on disk, so the next uninstall is exact |
| 27 | `Get-Command tt`; `tt help`; `$LASTEXITCODE` | the installer closes with `then run: tt help` — is it true now? | `EXIT=0`, 45 tools → **issue-020 item 1 confirmed fixed** |

### Phase 4 — reload the Claude plugin

| # | command | reason | result |
|---|---|---|---|
| 28 | Read `~\.claude\plugins\installed_plugins.json`, `known_marketplaces.json` | how is the plugin wired, and to what | plugin **0.9.2** @ `92caf86`, installed 2026-08-04 |
| 29 | `git -C …\marketplaces\bjornregnell log -1 --oneline`; read its `plugin.json` | is the marketplace checkout itself stale? | `92caf86`, declares `0.9.2` — yes |
| 30 | `claude plugin --help` | find the supported verbs rather than guess | `marketplace`, `update`, `list`, … |
| 31 | `claude plugin marketplace update bjornregnell` | refresh the source before asking for a version | → `7f03345`; `plugin.json` now declares `0.10.2` |
| 32 | `claude plugin list` | did the marketplace refresh update the plugin too? | **no — still 0.9.2.** Two steps needed; only the second announces itself |
| 33 | `claude plugin update genscalator@bjornregnell` | the actual update | `0.9.2 → 0.10.2`, *"Restart to apply changes"* |
| 34 | Read `installed_plugins.json` | confirm the new identity | `0.10.2` @ `7f03345` |
| 35 | List the new tree's `skills\` and root | what changed; does it ship `tools/` (issue-015's sibling question) | 12 skills (3 new); ships `tools/`, `reqts/`, `research/`, and its own `get-genscalator.sc` |
| 36 | `gh api …/compare/v0.10.2...7f03345` | does "0.10.2" mean the v0.10.2 tag? | **ahead_by 21, 79 files changed** → **finding 045** |

### Phase 5 — provenance and output-encoding checks

| # | command | reason | result |
|---|---|---|---|
| 37 | `tt --version`; `tt version` | can the shipped binary name itself yet? | both `no such tool`, `EXIT=2` — issue-028 confirmed still open at this release; fix is on main |
| 38 | fixture `TD\fixture-oldinstall`, then `cmd /c "scala-cli run … -- --uninstall --home <fixture> > _enc-probe.txt 2>&1"`, then `[IO.File]::ReadAllBytes` | is the mangled `⚠`/`—` a real output defect or my reader's fault? `cmd` redirection + raw bytes controls the read side | `⚠` written as ASCII `?` (**destroyed at source**); `—` as single byte `0x97` (cp1252); only non-ASCII byte in the stream is `0x97` |
| 39 | `scala-cli run …sc -- --uninstal --force --dry-run` (one `l`) | is the silent-flag half a release-timing artifact, or durable on main? | prints `genscalator bootstrap`. **Durable: a one-letter typo turns remove into install** → **finding 044** |
| 40 | `Get-ChildItem …\cache\bjornregnell\genscalator` with per-dir counts | does the old plugin version get cleaned up? | `0.10.2` (585 files) **and** `0.9.2` (551 files) both retained |

*Log hygiene:* command 39 reported `Exit code 255` — that is PowerShell's `Select-Object -First 6` closing
the pipe on a native command, not a failure of the script. Recorded so the log is not misread.

## Arm B: the 014–022 re-verification

Fixtures built under `TD\fx\`, ground truth measured independently first: `fx014` = 4 non-dot immediate
sub-dirs, 2 dot-named, 2 root-level `.md`; `fx017` = report 086's exact replica, 3 `.scala` + 1 `.json`;
`fx016\big.html` = 500 `<p>` paragraphs, 38 422 B; `fx018` = `empty.log` (0 B), `nolog.json`,
`success.log` (sbt markers), `zerocount.log` (`0 tests passed`).

### Phase 6 — the documented reproductions, one issue at a time

| # | command | reason | result |
|---|---|---|---|
| 41 | `tt find fx014 --type d --max-depth 0 \| 1 \| 2`; `--type f --max-depth 1`; `--ext .md --max-depth 1` | 014's four documented rows plus the maintainer's bonus defect (dirs leaking into `--type f`) | `1 / 5 / 6` matches — GNU semantics; `--type f` returns the 2 files only, **no dirs leak**; dot-dirs still skipped |
| 42 | `tt files fx017 .scala`; `tt find fx017 --ext .json --max-depth 3`; `--all`; two repeated `--exclude` | 017 both halves, against 086's recorded `3 files` / `1 matches` | `1 files (3 excluded: node_modules, target)`; **`0 matches (3 excluded: …)`**; `--all` restores 3; `--exclude` repeatable and disclosed |
| 43 | `tt htmltext big.html` (count lines); `--cap 40`; `--cap notanumber` | 016 — is output boundable, and is truncation silent? | uncapped 1498 lines, 0 notices (documented default); `--cap 40` → 40 + `=== truncated: showing 40 of 1498 lines`, **true total matches the measurement**; bad value = one-line error, exit 2 |
| 44 | `tt log` on each of the four fixtures | 018 — 086 found empty / non-log / clean-build byte-identical | **four distinct verdicts**: `EMPTY input (0 bytes) — … not a clean run`; `no log markers recognised in 1 lines`; `0 errors, 0 warnings, 4 success markers (9 lines scanned)`; and `0 tests passed` correctly scores **0** success markers |
| 45 | `tt log empty.log --require-markers`; same on `success.log` | 018's opt-in gate, which 086's empty-file data point argued for | `EXIT=1` on empty, `EXIT=0` on success |
| 46 | `tt log C:\Users\<user>\testgenscalator` | 020 item 2 — directory argument should hint at `tt gitinfo` | `for git history see: tt gitinfo <repo>  (tt log analyzes build/run LOG FILES)`, exit 2 kept |
| 47 | `tt help`, `tt --help`, `tt -h`, `tt nosuchtool` | 020 item 1 on all spellings 086 tested, plus the invariant | all three exit 0 with the tool list; unknown tool still exits 2 |
| 48 | `tt doc guard-clean-digest` grep `tt files` / `tt find` / `CONTENT\|STRUCTURE` | 020 item 3 — 086 counted files 1×, find **0×** | now **1× each**, plus `division of labour: files = CONTENT search (grep -rl), find = STRUCTURE search` |
| 49 | `tt skillcheck`; `tt skillgrants` | 015 — tier-1 fix was a recoverable error, not a fallback | both exit 2 **with** the recovery hint: names `--skills`, states the D4 reason, probes the cache and prints ready-to-paste lines for **both** `0.10.2` and `0.9.2`, warns that a stale cache yields a wrong set |
| 50 | `tt doc allowlist` — line count, `tt which` mentions | 019 — fix `7add972` postdates the release, so the gap should still be here | **71 lines** (exactly 086's count), `tt which` **0 mentions**. Correctly absent |
| 51 | `tt which tt`; `tt which tt.exe`; `tt which git`; ground-truth PATH entry count | 022 checklist: found / drive-letter / kind | bare word **resolves** (exit 0); path keeps `C:`; `PE executable`; mode `n/a`; `git` found |
| 52 | `tt which zzz-no-such`; same with `PATH` shrunk to one entry; `tt which C:\…\tt.exe` | 022 checklist: entry-count / not-found / path-branch | `36 dirs` == ground truth 36 (old bug would say 37); single entry → `1 dirs`; explicit path takes the **path branch**, not labelled `in PATH` |
| 53 | `tt files --help` grep | do not file a non-issue about 017's disclosure count | help documents it: *"a pruned subtree counts as one entry"* — count 3 vs 2 names is the documented rule |
| 54 | `gh api` blobs for `.claude-plugin/plugin.json`, `marketplace.json`, `VERSION.txt` at tag `v0.10.2`; `AGENTS.md`; CHANGELOG sections | 021 — all carriers must agree at the tag | `0.10.2` / `0.10.2` / `0.10.2`; `**genscalator v0.10.2**`; `## v0.10.2` section present, `## v0.10.0` backfilled, and the honest note *"v0.10.0 shipped with every in-repo version…"* is there |

### Phase 7 — the carrier audit behind finding 043

Prompted by the human's surprise, not by the protocol: *"downloading the script was not enough to
uninstall — you had to find it on main, and that is not what the front page says."* Run after arm B.

| # | command | reason | result |
|---|---|---|---|
| 55 | Read `_README-main.md` lines 78–112 | quote the promise exactly rather than paraphrase it | lines 80/81 send the reader to the **release assets**; 96 says *"the same script"*; 109 says *"a script you fetch fresh"* |
| 56 | `gh api …/contents/README.md?ref=v0.10.2` → count `uninstall`; same for the installed `docs\` tree, the release asset and main's script | which carriers promise the capability, and which have it? | **main README 5, v0.10.2 tag README 0, installed docs 0, release asset 1 (a comment, no flag), main's script 26** — so the released tree is correctly silent and the front page is the sole over-promise |
| 57 | `Get-Content _README-main.md -TotalCount 6` | did issue 036's version-banner fix cover `README.md`? | **no** — it opens `# genscalator` + badge + TLDR with no version line, so nothing marks which release the page describes. Gives 043 a fix path the repo already owns |

### Verdicts

| issue | class | Windows verdict on v0.10.2 | evidence |
|---|---|---|---|
| **014** | defect | **FIXED** | `--max-depth 0/1/2` → 1/5/6 vs 086's 0/1/10-shape; bonus defect fixed too |
| **015** | defect | **FIXED** (tier-1, as triaged) | recovery hint on **both** `skillcheck` and `skillgrants`; exit 2 kept by design |
| **016** | enhancement | **FIXED** | `--cap`, non-silent truncation with the true total, uncapped default, clean bad-value error |
| **017** | split | **FIXED, both halves** | 086's misleading `1 matches → the only hit is a build artifact` is now `0 matches (3 excluded: …)` |
| **018** | split | **FIXED, both halves** | the three byte-identical outputs 086 recorded are now three distinct verdicts; gate exits 1 |
| **019** | docs | **not in this release — correctly** | `7add972` closed it 2026-08-16, after the 2026-08-11 cut. Gap still present verbatim (71 lines, `tt which` 0×) |
| **020** | polish | **FIXED, all three items** | help exits 0 on 3 spellings; gitinfo hint present; digest names `tt find` + the division of labour |
| **021** | release | **FIXED** | every carrier reads `0.10.2` at the tag; CHANGELOG section exists; honest note about v0.10.0 stands |
| **022** | defect | **FIXED — and now verified on hardware** | all five checklist items pass: found / entry-count / drive-letter / kind / not-found |

**8 of 9 fixed and verified; 019 correctly pending its release.** No fix regressed, and no new defect was
found in any of the nine tools. Arm B produced **zero** new issues — worth saying plainly, because a
re-verification arm that finds nothing is the outcome you want and the one that looks like idle work.

**On 019 and 022 together — the ordering constraint held.** Report 086 warned that 019 must not ship
before 022 or it would teach Windows users to trust a broken check. 022's fix is in the binary (v0.10.1)
and 019's doc lands in v0.10.3, so a Windows reader will meet the prescribed `tt which tt` ritual only
once the ritual works. That coupling was honoured across three releases and two reports, which is the
part of this result worth keeping.

### Two cosmetic residues on 022, for the closing comment rather than a new issue

- **The printed filename carries the PATHEXT probe's casing, not the file's.** `tt which tt` prints
  `…\bin\tt.EXE` and `tt which git` prints `…\cmd\git.EXE`; the files on disk are `tt.exe` and `git.exe`.
  Harmless on NTFS, but the acceptance sketch asked it to *"report the file actually found (`tt.exe`)"*,
  and a path echoed into a case-sensitive context or a string comparison would be wrong.
- **`1 dirs`** — plural agreement in the single-entry case. One character.

## Duplicate check

Run before proposing anything, and it changed the report — see *Corrections*.

Searched `open/` and `closed/` on `main` (highest issue number **039**) **and the open pull requests**,
which is where three higher numbers already live:

| existing | subject | relation to this report |
|---|---|---|
| **040** (PR #4, open, filed 2026-08-20 from Linux) | uninstall fallback list drifts from the shipped zip | **Same finding as my phase-2 observation.** PR #4 has the better root cause: `get-genscalator.sc:223`'s hand-maintained vector vs `native-release.yml:169-175`'s actual staging. It states *"Not verified on macOS or Windows"* → my contribution is a **Windows confirmation**, not an issue |
| **042** (PR #6, open, filed 2026-08-20 from Linux) | `--uninstall` never names the version it removes | **Same finding as BOTH my version observations** — the fallback's `version: unknown` *and* the manifest's `installed-tag: latest`, which PR #6 correctly separates as two causes with one symptom. Also *"Not verified: macOS or Windows"* → **Windows confirmation** |
| **041** (PR #5, open) | capability descriptions are not projected from one source | **Adjacent, not the same.** 041 is about `tt` verb descriptions across three carriers (`--help`, `tools/README.md`, `docs/gs-help.txt`). Findings 043 and 044 are the same *class* — a description that outlives, or outruns, the capability — on a different carrier (the README and the installer's own flag set). Cross-reference, do not merge |
| **036** (closed `7add972`) | stale carriers cannot say how old they are | **Findings 043 and 045 are its two inverses**, which is why both are worth filing: 036 assumes the problem is a *missing or stale* version. 043 is a carrier running **ahead** of the release and not saying so; 045 is a version that is **present, current and agreeing** and still identifies nothing. 036's own fix — a version banner asserted by `version.test.scala` — is the proposed fix for 043, extended to the one carrier it skipped |
| **021** (closed) | v0.10.0 shipped declaring 0.9.2 | 045 is its descendant: the versions now agree and the content still does not |
| **028** (open) | no `tt --version` | re-confirmed incidentally (phase 5); nothing to add |
| **039** (closed) | no uninstall story | the parent of arm A. Not duplicated: 039 built the feature, this tests it |

**Next free number is 043.**

## Findings

Three filed as issues (PRs #7, #8, #9), and two confirmations held for the Linux issues they belong to.

| # | class | finding |
|---|---|---|
| **043** *(filed, PR #7)* | **defect, docs** | **The README's documented uninstall procedure cannot be performed with the artifact the README tells you to download.** This is the finding a user actually hits, and it is filed first for that reason. `README.md` on `main` — the GitHub front page — documents uninstall in five places, including its own section (2.1, *"and how to uninstall"*), and instructs: line 80 *"the bootstrap installer **shipped with every release**"*, line 81 *"download `get-genscalator.sc` **from the release assets**"*, line 96 *"**The same script** removes what it installed"*, line 109 *"a script you **fetch fresh** can still reverse an install"*. Follow that exactly and you get an asset **byte-identical to v0.10.0** (sha256 `A7EE81D3…845F`) with **no `--uninstall` flag** — so the documented remove command **installs instead** (cmd 18). The `--uninstall` code exists only on `main`'s raw file and in the plugin cache tree, neither of which the README names as a download source. The measured split: `uninstall` appears **5×** in main's README, **0×** in the v0.10.2 tag's README, **0×** in the installed `docs\` tree, **1×** in the release asset (a comment, no flag), **26×** in main's script. So the released tree is *correctly silent* — the defect is precisely that **the front page describes unreleased capability without saying so.** Suggested fix, and it is a mechanism the repo already owns: issue 036's fix stamped `CONTRIBUTING.md` and `reqts/issues/README.md` with a version banner asserted by `version.test.scala` — **`README.md` did not get one** (it opens `# genscalator` + badge + TLDR, no version line). Extend the banner and the assertion to `README.md`, and/or mark unreleased sections explicitly. Note the front page's *first* line (the TLDR) also links `releases/latest/download/get-genscalator.sc`. |
| **044** *(filed, PR #8)* | defect | **The installer silently ignores unrecognised arguments**, which is what turns 043 from a loud failure into a wrong action. `--uninstall` on a carrier lacking it, and `--uninstal` (one `l`) on the carrier that has it, both perform a full **install** — proven durable on `main` by the typo test (cmd 39), so unlike 043 this does not resolve when v0.10.3 ships. A user asking to *remove* software and receiving an *install*, with no error, is the worst available outcome for a script whose stated premise is that it hides nothing. Suggested fix: `die` on any `--`-prefixed token not in the known set — four lines. Same class as issue 041 (a description outliving the capability) on a different carrier. |
| **045** *(filed, PR #9)* | defect/docs | **A plugin version string is not a provenance statement.** The marketplace tracks `main`, so `claude plugin update` installed a tree declaring `0.10.2` that is **21 commits and 79 files ahead of the v0.10.2 tag** — including `which.scala`, `versionlib.scala`, `files.scala`, `text.scala` and their tests. On this box the plugin's `tools/` is *newer* than the native `tt.exe` under test, so a field test that reaches for `tools/tt` silently tests unreleased code. The inverse of issue 036: the version is present, current and agrees across carriers, and still identifies nothing. Only `gitCommitSha` in `installed_plugins.json` is a real identity, and nothing surfaces it. Shares 043's root — a carrier that cannot say which release it corresponds to — from the other direction. |
| **040** | *confirmation* | **Confirms PR #4 on Windows**, which it lists as unverified there. `reqts\PRD.md` (82 092 B) survived `--uninstall --force`, the install root survived with it, and the `kept:` line misattributed it. Adds a **prediction registered before the deletion** (30 on disk vs 29 listed, gap named as `reqts\PRD.md` in advance) and the consequence: reaching a clean box took a manual `Remove-Item -Recurse`. |
| **042** | *confirmation* | **Confirms PR #6 on Windows, both halves, independently** — hit before reading PR #6. Fallback printed `version: unknown` and the manifest written by the ordinary no-`--tag` install recorded `installed-tag: latest`, both against a `VERSION.txt` reading `v0.10.2` in the same directory. Corroborates the half PR #6 says it found "by checking, not reasoning". |
| — | *note for 040/042* | **On a Windows console the JVM bootstrap destroys `⚠`.** Raw-byte probe (cmd 38): the warning marker is written as ASCII `?` and the em-dash as cp1252 `0x97`, so the fallback's *"this list is a guess"* line loses its glyph. Scoped precisely: this is the **JVM** path — the native `tt.exe` printed `—` correctly all through arm B, so it is the bootstrap script's stream encoding, not a house style problem. |

Two smaller observations, not worth issues:

- `claude plugin marketplace update` does **not** update the installed plugin (cmd 32) — two verbs, and
  only the second prints a version change. Harness behaviour, but a step a tester will skip.
- The superseded `0.9.2` plugin cache tree (551 files) is retained. Possibly deliberate for rollback —
  and it earned its keep here: it is what made issue 015's recovery hint offer **two** candidates and
  exercise its stale-cache warning for real.

## What worked well

| thing | verdict |
|---|---|
| **the fixes themselves** | 8 of 9, on a platform none of them was developed on, with no regression and no new defect in the nine tools. Three were re-tested against the *exact* fixture that produced 086's defective output, so the comparison is direct. |
| **017 and 018 are better than their acceptance sketches asked** | 017 not only prunes but *discloses on the count line*, so the misleading case flipped from "1 hit, all of it generated, nothing to prompt doubt" to "0 matches **and here is why**". 018's zero-tally rule means `0 tests passed` cannot fake a pass — the mirror-image risk, closed. |
| **the uninstaller's core loop** | Preview by default; `--force` removed exactly what it listed; empty dirs pruned deepest-first; a non-empty dir left standing; `tt` gone afterwards. Issue 040 is one stale list, not a design failure. |
| **never editing a human-owned file** | PATH and any `settings.json` hooks were *printed with exact instructions* and left alone, both directions. On Windows that also made the reinstall painless — the August PATH entry still pointed at the right directory. |
| **the manifest** | Written on the first install that supports it, lists 30 files **and itself**, so the *next* uninstall is exact rather than heuristic. |
| **015's recovery hint** | The tier-1 scope was the right call and it shows: exit 2 kept, but the error now carries the escape hatch, the D4 *reason*, live-probed candidates, and the stale-cache warning. It turned the one failure in arm B that still exits non-zero into a self-service fix. |
| **read-before-run held up under an actual read** | The uninstaller's comment block argues its own design (why the flag lives here, the fetched-fresh obligation, the vendored-guard drift hazard) — and issue 040 is that same drift hazard, two files away, which the file all but predicts. |

## Corrections made during this run

Six claims were made and withdrawn. The first two are the ones that matter, and both were caught by the
human rather than by the protocol.

- **Reframed: the README/asset mismatch was buried, and its severity was wrong.** The first draft folded it
  into one finding as the *"aggravating"* half of the silent-flag defect, and predicted *"the doc half
  closes when v0.10.3 ships"*. Both judgements were wrong. It is the failure a **user actually hits** —
  the human followed the front page's documented procedure, downloaded the asset the front page names, ran
  the documented command, and got an install; the sweep had only established the mechanism, from the
  inside, by hash-comparing two files. And "it closes when the next release ships" is the reasoning that
  lets a thing ship: the *mechanism* (a front page with no version stamp, describing unreleased
  capability) survives the release that hides this instance of it. Now filed first, as 043, with the
  README lines quoted and the per-carrier counts measured (cmds 55–57). **The human's surprise was the
  instrument here** — an agent reading the source cannot feel a false promise, because it reads the code
  the promise is about.

- **Withdrawn: the numbering, and two of the five "new" findings.** The first draft of this report proposed
  five new issues numbered **040–044**. Three of those numbers were already taken by pull requests **#4,
  #5 and #6**, filed from Linux the previous day, and **two of my five findings were already filed there**
  — the fallback's missing `reqts/PRD.md` (issue 040) and both version-naming halves (issue 042). The
  duplicate check had been run against `reqts/issues/` on `main`, where the highest number is 039; it had
  **not** been run against open PRs. On a repo where issues arrive *as* pull requests, the issue files on
  `main` lag the claimed numbers, so `main` is the wrong denominator. This is report 087's lesson recurring
  in a new place — that report recorded checking titles early and source late; this one checked the right
  directory and the wrong ref. The human caught it by asking.
- **Narrowed: finding 044's v-prefix half, dropped.** The draft cited native `v0.10.2` vs plugin `0.10.2`
  as part of the defect. It is not: issue 021's close settled it deliberately — *"the `v` belongs to the git
  tag alone"*, `VERSION.txt` in-repo is bare, CI stamps the tag name into the install, and the release gate
  normalizes before comparing. 044 stands on the commit distance alone.
- **Retracted: "main's installer is 873 lines."** The first fetch piped `gh api` through `Out-File`, which
  **line-wrapped the file at console width**, inflating 360 lines to 873 and corrupting the content. Any
  line citation from that read was worthless. Re-fetched with `[IO.File]::WriteAllBytes`. In PowerShell
  5.1, `Out-File`/`>` is **not** a byte-faithful download.
- **Retracted: "the plugin tree's installer has no `--uninstall` (0 hits)."** Caused by passing the
  regex-escaped pattern `\-\-uninstall` to `Select-String -SimpleMatch`, which searched for literal
  backslashes. Re-run: **26 hits**. The plugin tree *does* carry the uninstaller — which sharpens 043
  rather than weakening it: two carriers of one filename, one with the feature and one without.
- **Narrowed: the mojibake.** The em-dash garbling seen when *reading* files (`â€”`) is PowerShell 5.1's
  ANSI-default `Get-Content` misreading correct UTF-8 — not genscalator's doing. Only the *console write*
  survives, and only because cmd 38's raw-byte probe controlled for the read side.

All five came from re-checking rather than trusting the first output — and the first one came from the
human asking a question I should have asked myself.

## Coverage — what was NOT tested

**Arm B invoked 8 of 45 tools for real** — `doc` `files` `find` `htmltext` `log` `skillcheck` `skillgrants`
`which` — plus the dispatcher (`help`/`--help`/`-h`/unknown/`--version`). One more than report 086's 7, and
the same denominator: **37 of 45 untested on Windows.** No claim is made about them. This is a targeted
re-verification, not the Windows sweep report 086 said was still owed — that remains owed.

Specifically undone:

1. **The manifest-based uninstall was never applied.** The new install has a manifest, but it was not
   removed again, so only the *fallback* branch ran end to end. The precise branch — everyone's, from
   v0.10.3 on — is untested, and issue 040 says it is the one that works.
2. **A truly naked-box install.** The User Path entry survived from August, so the documented
   `[Environment]::SetEnvironmentVariable(…)` step was never exercised. That is the actual newcomer case.
3. **`--home`, `--no-path`, `--tag` on the install side.** Only the fixture used `--home`, for a preview.
   The `--tag` path is exactly where issue 042's manifest half may *not* reproduce.
4. **The plugin's skills were not loaded.** `claude plugin update` requires a restart; this session ran
   0.9.2's 9 skills throughout. Nothing here tests the three new skills, and arm B deliberately did not
   need them.
5. **`tt update --native`** — the self-upgrade path report 087 praised was bypassed on purpose, to reach
   the uninstaller.
6. **The driver tools** (`tt scala` / `sbt` / `bloop`) — still the highest-value Windows gap, for the
   reason report 086 gave: `ProcessBuilder` and PATHEXT. 022's fix touched `which`'s PATHEXT handling, not
   theirs.

## Threats to validity

- **Single machine, single Windows version.** Same box as report 086 — so this is not an independent
  platform sample but a *longitudinal* observation of one box.
- **Arm A's subject is unreleased.** The uninstaller is `main` at the `7f03345` era. Its findings may be
  fixed before v0.10.3; filing them now is the only way that happens.
- **Non-reproducible start state.** The pre-manifest v0.10.0 install is gone and a hand-built fixture is
  not the same evidence. `n = 1`, permanently, for the Windows half of issue 040.
- **Arm B is confirmation-shaped, and doubly so.** Re-running nine known reproductions is biased toward
  finding what it looks for, and this time the expected answer was *"fixed"* — the direction where a
  tester stops looking early. Mitigated by measuring ground truth independently before each verdict and by
  keeping 086's exact fixture, not by protocol.
- **The 022 entry-count check is weaker than it looks.** Ground truth was 36 entries and the PATH happened
  to contain 36 colons, so the pass rules out the old `split(':')` (which would have printed 37) but is one
  fewer bit of evidence than 086's asymmetric 35/36 case. The single-entry test (`1 dirs`) is the decisive
  one, and it is why it was run.
- **My fixture is not 086's fixture in one respect.** `big.html` yields 1498 lines here against 086's 1000,
  because the filler differs. The 016 verdict rests on internal consistency (reported total == measured
  total), not on matching 086's number.
- **`gh` was used for all repo facts, not `tt forge`.** Deliberate — `forge` is untested surface — but it
  means report 087's tool-lane discipline was not applied here.
- **The encoding finding is partly environmental.** `chcp` was never varied; `chcp 65001` is untested.
- **Same human, same agent model, same conventions** as reports 085–087. Not an independent replication —
  and note that the Linux issues 040/042 this report confirms were filed by the *same pair*, so the
  confirmation is cross-platform but not cross-observer.

## Notes for the next field test

- **Check open PRs, not just `reqts/issues/`, before numbering anything.** On this repo issues arrive as
  pull requests, so `main` under-reports the highest number. Cost this time: three findings drafted as new
  that were already filed, caught by the human rather than by me.
- **022 can be closed.** Its checklist is met on hardware. Attach the two cosmetic residues above to the
  closing comment rather than opening anything.
- **The Windows halves of 040 and 042 are a deadline, not a backlog item.** The pre-manifest install only
  exists on boxes that have not upgraded, and every day there are fewer.
- **A pure test would have caught issue 040 without any box** — the same lesson 086 drew for 022, and PR #4
  already proposes it. Assert the fallback vector against the *staging step's* top-level entries and the two
  can never drift silently again. Structure rather than vigilance, which is the argument the installer makes
  about its own vendored guard.
- **Fix 044 regardless of the release schedule.** Four lines, and it is the difference between an error
  message and an unwanted install. 043 will look self-healing the moment v0.10.3 publishes a script with
  the flag — which is exactly why it should be fixed as a *mechanism* (a version-stamped README) rather
  than waited out. The next feature documented ahead of its release will do this again.
- **Windows still deserves the 085 treatment.** Two reports have now re-tested what Linux found; neither
  has swept Windows for what Linux cannot see. The driver tools are where to start.

---

Reported by hmiddelk. An AI agent (Claude Opus 5) ran both arms, made the pre-deletion prediction, and
drafted this report under human direction; the human supplied the uninstall command under test, redirected
the arm from PR #3 to PR #2, and asked for the duplicate check against PRs #4–#6 that corrected the
findings list. Five claims the agent made were withdrawn during the run and are recorded above. No
assistant credit in the commits, per `CONTRIBUTING.md`.
