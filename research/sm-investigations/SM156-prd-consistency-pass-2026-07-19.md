# SM156 — PRD consistency pass, 2026-07-19 (T9 slice, read-and-report)

**Status: FINDINGS ONLY (agent-drafted AFK 2026-07-19).** No PRD edits made — every item below is a
proposed fix for BR (or a BR-delegated edit round) to adjudicate. Method: `tt parsereqt parse PRD.md`
(clean: **419 top-level elems, zero parse errors**; compile warnings are vendored-code, not model) plus a
full read of all 626 lines cross-checked against the live tool roster, the test suite, and release state.

## Findings (ordered by weight)

### F1 — The FUTURE/PAST seam is one release behind reality
`PRD.md:171`: "The next real release is **v0.9.0** — v0.1.0–v0.8.0 have shipped." Reality: **v0.9.0 is
tagged and released, the toolbox is at v0.10.0** (git tags; PB roadmap). Per the document's own rule
("Requirements move FUTURE → PAST as they ship", line 13) the whole "Release v0.9.0 (next)" block —
ttWeb, ttForge, reqTParser, typedToolsTestSuite, outputShapingFlags(?), ttGit, ttGitinfo, ttWrStamp,
ttStatusline, ttHarden, ttSsg, ttServ, ttMdFmt, ttBox, ttSeqDiagram, guardcheckHook, crudWebAppSeed,
dwimCommands — needs triage: shipped items move to a new `#### Release v0.9.0` IMPLEMENTED block; the
genuinely-unshipped residue (see F3) stays in FUTURE under the next version. PAST currently ends at v0.8.0.

### F2 — `Feature: ttGit` is defined TWICE with different content
`PRD.md:242` (v0.9.0 post-hoc block, shipped gist) and `PRD.md:372` (v0.10.0 block, richer aspirational
spec: in-session diff report, argv-as-data rationale). Same id, two `has` blocks — the §11
duplicated-fact hazard inside the requirements spine itself. Proposed: ONE home — the shipped subset in
PAST, the unshipped extras (diff report, `--max-lines`, `--show`) as a separate FUTURE feature (e.g.
`ttGitDiffReport requires ttGit`). Note `tt git` today also lacks the spec'd read verbs (`status`, `log`,
`diff`, `branch` live in `tt gitinfo` instead) — the two entities' merge should say which tool owns what.

### F3 — Genuinely-still-open items stranded inside the "shipped" v0.9.0 block
Verified open against substrate tonight: **typedToolsTestSuite's spec** ("extend coverage to `tt web` +
`tt forge`") — the test dir has 11 suites and none for web/forge (`tools/test/` listing, 2026-07-19), so
this parked task is real and should survive the F1 move as FUTURE; **outputShapingFlags** and
**greprRegexLint** — no such flags/lint in the current tools (spot-check) — stay FUTURE.

### F4 — `Feature: ttBox` gist describes a DIFFERENT tool than today's `tt box`
`PRD.md:284-285`: "host-pinned safe remote-ops with a fixed verb enum... for a known compute box." Today's
`tt box` (SM160/SM163 era) is the LOCAL machine-health line (mem/load/temp/disk/jvm/bloop). Either the
remote-ops tool was renamed/retired or the id needs splitting (`ttBoxRemote` vs `ttBoxHealth`); as written,
a reader tracing ttBox lands on the wrong semantics. The box-health tool, plus `tt bloop restart` (T3,
suite-verified tonight), `tt mode`, `tt chrono`, `tt wr`, `tt typo`, `tt doc`, `tt find`, `tt htmltext`,
`tt mdparse`, `tt minijson`, `tt skillcheck`, `tt skillgrants`, `tt hangover`, `tt prd`, `tt boxstats`
have NO Feature entity at all — expected for pre-v0.9 backfill (bootstrap note), but the post-v0.9 tools
(box health, bloop restart, mode) are recent enough that their absence is drift, not backlog.

### F5 — Release-numbering mismatch: PRD "v0.10.0 (later)" vs toolbox tag v0.10.0
The PRD's v0.10.0 block (ttConfigFile, parserFallthroughMarker, safeModeFlags, textStreamEditor, richer
ttGit, mcpServer) is mostly UNSHIPPED, yet the toolbox already tags v0.10.0. The PRD's release numbers
and the actual tag stream have diverged; the F1 triage should also re-label the FUTURE release headings
(e.g. "next minor" / "later") or re-sync numbers with the tag reality, so "v0.10.0" stops meaning two
different things in-repo.

### F6 — Minor spec staleness (one-line fixes)
- `rotFatigueGauges` spec (`PRD.md:523`): example model display "lower-case o4.8 slash 1M" — the live
  statusline renders `f5·1M` (F5 warp + tonight's middot decision `74d511a`).
- `ttStatusline` spec (`PRD.md:260`): "cost renders as whole truncated dollars and is placed last" — still
  true, but the segment separators are now middots repo-wide; the spec text predates that and says nothing
  wrong, listed here only as checked-and-fine.
- Line 32 and 87 carry live `*TAP:*` markers (repo-refactor plan; meta-model investigation) — the second is
  answered inline already; candidates for closing during the F1 edit round.

## What was checked and found CLEAN
Parse (419 elems, no errors) · the reqT-lang syntax-rules section (matches the reqt-lang skill's rules) ·
Stakeholder/Goal graph (no orphan goals spotted on read) · v0.11.0 and v2.0 sections (properly future) ·
the 2026-07-15 dated block (SHIPPED/pinned markers accurate per that session's record) · PAST v0.1-v0.8
blocks against CHANGELOG-era memory (no contradictions noticed on read).

## Suggested next step
One BR-present edit round (or a delegated CF5 sub-agent round with this note as the brief) executing
F1+F2+F5 as a single FUTURE→PAST re-organization commit, with F4's rename decided by BR first, then a
re-parse + re-lint as the acceptance check (the reqTParser baseline spec's own method, `PRD.md:199`).

## Ties
SM156/T9 (this is its first concrete slice) · SM149/SM160/SM163 (tools missing from the spine) ·
[[genscalator-prd-reqt-reengineering]] (the "remaining = web/forge tests" memory is CONFIRMED still true) ·
the reqt-lang skill (parse-don't-eyeball discipline followed).
