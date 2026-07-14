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

## Second specimen — the checklist was NOT sufficient even when it CARRIED the reflex (2026-07-14 22:5x)
Post-compact resume (fresh window), the agent read `tmp/resume-prompt.md` — whose anti-regression checklist
line 5 explicitly lists `ls | head` as a ⛔ pattern — and *on its second working tool call* still appended
`| head -50` to a `tt files` command. The guardcheck hook caught it ([MED] pipe-to-head). BR reported it live
from inside the guard-stall (`ig:`) as WR data.
- **Why this sharpens the finding:** the 2026-07-13 specimen regressed on a reflex ABSENT from the checklist,
  and the fix was "add the line." This specimen regressed on a reflex PRESENT on the checklist, freshly read.
  So checklist-carriage is necessary but **not sufficient** — a base-model "just peek at the top" reflex fires
  below the salience level a read-once checklist provides. The checklist is a *reference*, not a *reflex*; only
  the **guard hook** operated at reflex speed and actually stopped it.
- **The value:** this is the structure-over-willpower thesis proving itself twice over. The willpower layer
  (checklist I authored + just read) failed; the structural layer (the automatic PreToolUse guard) held. It
  argues for the guard hook as the load-bearing backstop, NOT the checklist — and for the SM103-adjacent idea
  of a `tt` native "peek first N" so the reflex has a guard-clean landing spot instead of only a ⛔.
- The pipe was also pointless: `tt files` had already printed the full 14-file list in the prior (unpiped)
  call; the piped re-run added nothing. So: regression under low stakes / near-zero payoff — the classic
  autopilot signature.
