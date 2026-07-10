# Raw `grep -nA4` slipped past the tt-first rule AND guard-stalled (2026-07-10, BR-flagged)

## What happened
Mid AFK bookkeeping, an Edit failed because line numbers had shifted, so I reached
for raw `grep -nA4 "SM023 — Improve" PIN-BOARD.md` to relocate the block. BR flagged
it live: **"WR data; should be call to tt"** and had to **release a guard-stall** it
caused.

## Two faults in one command
1. **Raw-tool reflex (the tt-first miss).** `tt text` already covers this:
   `tt text match <file> <regex>` (= grep -n) and `tt text context <file> <regex> [N]`
   (= grep -C N, default 2). So this was a **habit slip, not a tool-gap** - the tool
   exists and I'd even used `tt text grepr` correctly earlier the same session. The
   reflex is sticky under flow.
2. **It guard-stalled.** The allowlist covers only specific grep shapes (`grep -n *`,
   `grep -nE *`, `grep -ni *`, ...) - NOT `grep -nA4` (after-context). So the raw
   command tripped a confirmation. BR was present and released it; **had he been fully
   AFK this would have been an INVISIBLE stall** (the SM016a failure mode), silently
   halting the run.

## The lesson (sharper than "prefer tt")
`tt text *` is **uniformly allowlisted** (one broad, audited grant), whereas raw
`grep` is allowlisted only for the exact flag shapes someone happened to add. So
routing file searches through `tt text` is not just dogfooding / staying in
genscalator land ([[use-tt-grepr-not-raw-grep]]) - it is **strictly stall-safer**:
the typed tool has one stable allowlist surface; the raw tool has a ragged one where
any un-listed flag (`-A`, `-B`, `-C`, `-A4`) is a latent AFK stall. This is the
genscalator thesis applied to my own habits: the audited tool is the allowlistable
unit; the raw blob's safety is accidental and shape-dependent.

## Corrective
- File searches → `tt text match` / `tt text context` / `tt text grepr`, never raw
  `grep` (especially never a context flag).
- Directory listings → Glob tool or `tt`, not raw `ls -1`/`ls -la` (same ragged-allowlist
  hazard; I used raw `ls` several times this session too - same class).
- Candidate to strengthen: this is the standing raw-tool-reflex regression
  ([[shell-blob-fallback-regression]], [[command-hygiene-regression]]); the durable fix
  is structural (a pre-flight "is this raw when a tt exists?" check), not willpower.
