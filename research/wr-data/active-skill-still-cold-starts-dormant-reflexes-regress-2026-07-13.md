# WR data: an ACTIVE guardrail skill still cold-starts DORMANT — reflexes regress in the first bash calls of a fresh session (2026-07-13)

**Model:** Opus 4.8 (1M). BR caught it live, twice in a row, and named the frame: *"after warping into clear
session"* + *"should be tt tool"* + *":("*.
**Threads:** [[guardrail-skills-silently-inactive-all-session-2026-07-13]],
[[compaction-regresses-fine-grained-reflexes-2026-07-13]],
[[resume-reloads-context-no-rot-reset-fresh-session-is-the-warp-2026-07-13]],
[[capability-rides-on-substrate-access-not-harness-activation-2026-07-13]], [[use-tt-grepr-not-raw-grep]].
**Tags:** #agent-psyche #reflex #tool-candidate #substrate

## The specimen — two guard trips in the first three bash calls
Fresh, CLEAR session (resume-prompt handoff, ctx low). genscalator IS now installed as a plugin, so
`avoid-guard-stall` (and the rest) are **ACTIVE** per `/skills`. Yet:
1. `find . -maxdepth 2 -iname "PIN-BOARD.md" ... 2>/dev/null` → guard trip (`2>/dev/null` stderr-suppression).
   Should have been a `tt files` call, no stderr suppression.
2. `ls -d ../../*/genscalator ... 2>&1 | head` → guard trip (pipe to `head`) + brittle glob. Should have been a
   `tt` tool.

Both are exactly the idioms `avoid-guard-stall` exists to prevent. The skill was ON. It didn't fire in time.

## Why this REFUTES the earlier optimistic fix
The [[guardrail-skills-silently-inactive-all-session-2026-07-13]] note diagnosed a whole session of guard-stalls
as *"the skills are OFF; install the plugin + restart and the rails come on."* True, but **insufficient**. Today
the plugin IS installed, the skill IS active — and the reflexes STILL regressed at cold start. So "skill active"
is not the same as "reflex in context."

The mechanism: **an active skill is LAZY, not PROACTIVE.** It injects its guidance only when its trigger fires
(a matching keyword, or — worse — reactively, *after* a guard already tripped). At a fresh-session cold start,
before any trigger, the skill's content is NOT in the salient context. So the first few tool calls run on the
base-model prior (brittle bash: `find`, `2>/dev/null`, `| head`), precisely the prior the skill exists to
override. The override exists on disk and is "installed" — but it is dormant until summoned, and the regression
happens in the summoning gap.

This is the same shape as the compaction finding ([[compaction-regresses-fine-grained-reflexes-2026-07-13]]):
a learned override that isn't in the salient context loses to the base prior. New wrinkle: **it holds even when
the override is a fully-activated skill, not just a memory line.** Activation ≠ salience. The resume-prompt even
CLAIMED "skills auto-load and re-arm at startup" — that claim is misleading; they load lazily, so the re-arm is
not guaranteed before the first action.

## The general finding (the value)
Three tiers, decreasingly reliable at cold start:
1. **In the resume-prompt / summary** — actually in context, load-bearing.
2. **Active skill** — installed + enabled, but DORMANT until triggered → loses the cold-start race.
3. **Memory file** — passive recall, not auto-injected.
The felt-safe assumption "the skill will catch it" is false for the opening moves of a session. The carrier that
actually works at cold start is tier 1: the reflex must be in the resume-prompt's anti-regression checklist as an
explicit, do-this-first line — not delegated to a skill that hasn't woken up yet.

## Structural fix (not willpower)
- **Resume-prompt anti-regression checklist must carry the guard-clean reflexes explicitly** (tt-files/tt-grepr,
  no `2>/dev/null`, no `| head/tail/wc`, one bare command, Write-tool-for-files) as a FIRST-ACTIONS block — do
  not rely on `avoid-guard-stall` firing in time. This session's resume-prompt had the grepr/tail lines but the
  agent still cold-started on raw `find`; the checklist needs to be *read and internalised as action-zero*, and
  it should name `find`→`tt files` too (that pair was missing).
- **Candidate: a proactive cold-start reflex-rehydration step** (the [[compaction-regresses-fine-grained-reflexes-2026-07-13]]
  follow-up, now with more evidence): the agent's genuine first action in any fresh/warped window is to re-read
  its high-frequency guard-clean idioms deliberately, BEFORE the first bash call — turning tier-2 dormancy into
  tier-1 salience by hand. Cheap insurance against exactly this class.
- **Harness-ask (upstream):** could a skill declare a *session-start injection* (not just trigger-on-keyword), so
  guardrail-class skills are salient from the first turn? That would close the dormancy gap at the platform level.

## Note
Human-in-the-loop stayed load-bearing again: the agent could not feel the dormant-skill gap (no phenomenology of
absence); BR read the behavioural signature (two guard trips) and named the mechanism in real time.
