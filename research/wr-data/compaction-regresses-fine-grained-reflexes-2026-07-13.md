# WR data: compaction regresses fine-grained reflexes to base-model defaults (2026-07-13)

**Type:** WR data — post-compaction regression specimen. BR spotted it live and named the hypothesis:
*"is it the compacting that caused the regression into grep blobs instead of tt grepr or similar?"*
**Threads:** [[use-tt-grepr-not-raw-grep]], [[cue-bare-auto-compact]], [[resume-prompt-anti-regression-checklist]],
[[guardcheck-false-positive-gt-inside-quoted-regex-2026-07-13]], [[joint-rot-vigilance-recovery-kit]].

## The specimen
Post-compaction (fresh window, ctx 7%), the agent reached for raw `grep` — three times, each with a
shell-metachar in the pattern (`|`, `>` via `<label>`/`<f>`) — tripping the guardcheck each time and racing
BR. There is a STANDING memory, `use-tt-grepr-not-raw-grep`: recursive scans go through `tt text grepr`,
never raw `grep`. Earlier the SAME turn (pre-drift) the agent had used `tt text grepr` correctly. So this is
a regression *within one session*, straddling... nothing but the compaction? No — straddling the compaction.

## BR's hypothesis, and why it holds
**Yes, and it is traceable to a concrete lossy channel.** Neither the compaction summary NOR the agent's own
resume-prompt anti-regression checklist carried the grepr reflex forward. The checklist (which the agent
authored pre-compact) listed em-dash anchors, tail-pipe, mode verbs, git flow, notify branding — but omitted
`use-tt-grepr-not-raw-grep`. So in the fresh window, with that reflex absent from the salient context, the
agent defaulted to `grep` — the base-training default. `tt grepr` is a *learned local override*; `grep` is
the model's prior. Remove the override from working context and the prior reasserts.

## The general finding (the value)
Compaction preserves the NARRATIVE and the LOUD reflexes (whatever made it into the summary or the
anti-regression checklist), but **fine-grained learned overrides that live only in memory files regress to
base-model defaults** unless they are re-surfaced post-compact. The summary is lossy precisely at the level
of small, specific habits — the ones a memory file encodes but a prose summary rounds off. So:
- Memory files are necessary but NOT sufficient across a compaction — they are not auto-injected into the
  fresh window's salient context; only the summary + the resume prompt are.
- The resume-prompt anti-regression checklist is the load-bearing carrier of fine-grained reflexes across the
  warp. Its comprehensiveness IS the regression-prevention mechanism.

Two distinct sub-slips, one root: (1) tool choice (`grep` vs `tt grepr`); (2) metachar-in-pattern (even when
grepr was used). Both are small habits; both regressed; both were absent from the checklist.

## The structural fix (not willpower)
Add the grepr reflex to the resume-prompt anti-regression checklist, so it survives the NEXT compaction.
Generalise: every regression actually hit post-compact becomes a checklist line — the checklist grows
monotonically toward covering the agent's full set of learned overrides. This is the same
structure-over-willpower move as the guardcheck hook: encode the reflex where the process will re-surface it,
not where it depends on the agent spontaneously recalling a memory file.

## Follow-up
- DONE this session: checklist line added; behaviour corrected mid-turn.
- Candidate SM: a post-compact "reflex re-hydration" step — the agent re-reads its own high-frequency
  operating memories (grepr, commit flow, guard-clean idioms) as a deliberate first action, not relying on
  the summary to have carried them. Cheap insurance against exactly this regression class.
