# Meta-minion push 8 — report (2026-07-20 15:04, clock-read)

Task: CONSISTENCY REVIEW of the baton → warp-ember rename sweep (genscalator `707fd54`; work repo
`0b4e861`/`5d7897a`), not a handover audit. Reviewed against the decision + sweep record
`research/wr-data/rename-baton-to-warp-ember-2026-07-20.md` and the sanctioned raw-keeps-baton list
supplied in the push prompt. Method: `tt text grepr` over both repos, ext md/txt/scala, patterns
`baton`/`Baton`/`BATON` plus old-path patterns; every hit read in context where classification was
not obvious; file existence verified by Read (error = absent).

VERDICT: DIVERGENCE FOUND — 1 real defect (a live pin pointing at a dead path), 2 stale-term
leftovers in living tool code, plus a sweep-record completeness gap. Everything else clean, with
honest NULLs below.

## Findings (most severe first)

- **F1 — LIVE REMINDER POINTS AT A DEAD PATH (half-updated line).**
  `muntabot-synch-introprog/PIN-BOARD.md:113` — the 👤 REMINDER pin (live, addressed to BR, "review
  + revoice ...") was updated in its FIRST half (blog path now
  `genscalator/blog/030-what-is-a-good-warp-ember.md`, with a "renamed from -baton 2026-07-20"
  annotation) but its SECOND half still says "and the pre-hoc seed `work/BATON-TEMPLATE.md`
  (deliverable 1)". `work/BATON-TEMPLATE.md` does not exist (Read: file does not exist; only
  BATON-EXAMPLE.md got a tombstone). A live action pin BR is expected to act on names deliverable 1
  by a path that 404s. Classification: STALE + broken pointer. CONFIDENCE: high.

- **F2 — USER-FACING TOOL HELP STILL SAYS "baton".**
  `genscalator/tools/mode.scala:34` — the `tt mode` usage text printed to users:
  `ColdStart  SmartZone  (the baton declares -RotVigil +ColdStart +SmartZone upon a warp)`.
  This is a living, compile-checked tool surface, shown on every `tt mode` help invocation. Not in
  the sanctioned raw list, and NOT mentioned in the sweep record's touched OR not-touched lists —
  overlooked, not deliberately kept. Fix requires an edit + the tt-mode auto-rebuild. CONFIDENCE: high.

- **F3 — CODE COMMENT STILL SAYS "the baton's warp declaration".**
  `genscalator/tools/statusline.scala:320` — comment above the ColdStart/SmartZone entries in
  `knownModeColors`. Comment-only (no rendered output), same overlooked class as F2. Minor.
  CONFIDENCE: high.

- **F4 — SWEEP-RECORD COMPLETENESS GAP (story vs substrate).**
  `research/wr-data/rename-baton-to-warp-ember-2026-07-20.md:35-44` claims "living docs updated"
  including "work-repo PB active pins", and its NOT-touched list names wr-data, minion-log,
  sm-investigations, the png, and BR's cold-claude.sc. The record accounts for neither the F1
  half-missed pin line nor the F2/F3 `tools/*.scala` surface (in either list). The sweep record
  slightly overstates its own coverage — the exact pattern this study exists to log. MATTERS:
  low as prose, but it is the checkable-claim-unchecked family. CONFIDENCE: high.

- **F5 — OBSERVATION (borderline, no fix demanded): dead old-path mention inside a history-quoting
  pin.** `muntabot-synch-introprog/PIN-BOARD.md:69` — the SM168 pin's same-day 07-19 addendum says
  "the RQs live in blog stub `genscalator/blog/030-what-is-a-good-warp-baton.md`" (path now dead);
  the SAME bullet's later "📮 RENAMED 2026-07-20" note corrects it. Defensible under the
  pin-texts-that-quote-history carve-out and annotate-never-erase, but a reader following the path
  at that point lands nowhere; a two-word inline "(now -ember)" would fix navigation without
  rewriting history. CONFIDENCE: high on the facts, low on whether it needs action.

- **F6 — OBSERVATION (classified sanctioned): `muntabot-synch-introprog/notes/pr943-review-draft.md:10`
  says "the same gap flagged for the baton". The file is a frozen, retraction-annotated historical
  draft of a pasted PR reply — raw-record in nature though `notes/` is not on the sanctioned list.
  Left as-is is coherent with raw-stays-raw. CONFIDENCE: medium (judgment call).

## Sanctioned-hit count

**173** hit lines classified sanctioned (counting unit: distinct file:line grep hits, patterns
baton/Baton/BATON case-distinct then merged per line, ext md+txt+scala, both repos; ±3 tolerance
where grepr wrapped very long lines). Breakdown: raw records (wr-data, minion-log, case-study log,
sm-investigations, both repos' tmp/ scratch incl. commit-msg files and the pre-rename
`tmp/resume-prompt.md` ember, PB history + history-quoting pin texts) ≈ 148; deliberate
naming-history annotations in living docs (EMBER-TEMPLATE header, EMBER-EXAMPLE naming note +
VERBATIM markers, BATON-EXAMPLE tombstone, blog 030 naming-confession/changelog/figure notes,
foundations glossary naming history) ≈ 25. Stale (flagged above): 3 (PB:113, mode.scala:34,
statusline.scala:320).

## Honest NULLs

- **Link integrity — CLEAN except F1/F5.** `work/EMBER-TEMPLATE.md`, `work/EMBER-EXAMPLE.md`,
  `blog/030-what-is-a-good-warp-ember.md` all exist on disk (read). `work/BATON-TEMPLATE.md` and
  `blog/030-what-is-a-good-warp-baton.md` confirmed absent. All other references to the old names
  are in raw records or describe the rename itself (`SM147-safe-tt-rm-and-move.md:110` narrates the
  rms; minion-log pushes 4/5/6 are history). The tombstone is a valid inbound-link target.
- **Glossary — CLEAN.** `docs/foundations.md:82` A→Z contains "Ember (warp ember; formerly baton)"
  correctly alphabetized under E (between "Edit vs clarification cues" and "Extrinsic-volatile
  plasticity"); no standalone "Baton" index entry on the line. Body entry exists at
  `docs/foundations.md:426-457` with the naming history and the raw-stays-raw pointer.
- **New-file internal consistency — CLEAN.** EMBER-TEMPLATE.md links blog 030 (line 14) and
  EMBER-EXAMPLE.md (line 16); EMBER-EXAMPLE.md carries the naming note (lines 9-13) and both
  VERBATIM markers (lines 26, 68) with the specimen verbatim between them; the tombstone links
  EMBER-EXAMPLE.md (lines 1, 6) and the rename rationale.
- **Memories — MATCH.** The record's "2 memories" claim checks out: exactly two memory files carry
  the rename annotation (`memory/mode-chips-camelcase-vocabulary.md:20`,
  `memory/exit-resume-dance.md:56`); no other memory file mentions baton.
- **Term usage — CONSISTENT.** Prose noun form is "warp ember" / "ember" throughout; the hyphenated
  "warp-ember" appears only attributively ("warp-ember specimen", blog 030:153; "Warp-ember
  declaration", exit-resume-dance memory) — standard compound-adjective hyphenation, not drift.
  README.md:12 uses the full explained form. work-repo txt/scala: zero stale hits (caveat: the
  work repo may simply contain few/no scala files; the scala grep is proven live on genscalator,
  where it found F2/F3).

## Pattern (across pushes)

Same family as push-5/6: the sweep's PROSE record is confident and nearly complete, and the misses
are exactly the surfaces outside the object of attention during the sweep — a second target in an
already-edited line (F1: the line WAS touched, half of it), and a non-md file type (F2/F3: the
sweep grep evidently ran on md, not scala). "Swept" was declared per-file-list, not per-grep-null.
A rename sweep's definition-of-done should be an empty stale-grep over ALL living extensions, not a
list of files touched.
