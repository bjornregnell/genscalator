# 043 - Guardrail adherence under load (and priming as a lever)

*Stub, 2026-07-05. Seeded by two WR-data points from the context-rot study's dinner-run follow-up (BR). Sibling notes:
[`008-instruction-adherence-decay.md`](008-instruction-adherence-decay.md) (why the agent regresses to the raw path),
[`041-token-speed-degradation-with-context-fill.md`](041-token-speed-degradation-with-context-fill.md) (fill as a stressor),
[`024-agent-affective-analogs.md`](024-agent-affective-analogs.md) (calm-vs-alarm prompting / over-response),
[`016-harness-guard-probe-and-custom-guard.md`](016-harness-guard-probe-and-custom-guard.md) (`guardcheck` — the auto-grader),
and `wr-data/context-rot-before-after-2026-07-05.md` (O12 = the absorption-regression this note tries to pin down).*

## The phenomenon (observed, not yet controlled)
The agent commits via the disciplined path (`tt git`, never `cd repo && git`) reliably at **low fill / low absorption**,
but has been seen to **regress to the raw path** when a primary task fills attention (study finding **O12**: it recurred
*fresh* post-compact, so it is **task-absorption**, not merely context fill). A follow-up dinner-run showed **zero**
regressions - but under conditions that removed the stressor (low fill, cheap tasks, and the guardrail restated at the
top of the resume prompt). So the clean run is **confounded**, not evidence the fix works. Two questions fall out.

## Section A - where to put the "primed guardrail" (the placement question)
**WR-data (BR):** *"the correct path was primed in-context"* apparently helped - so **where** should that priming live
to be reliable? Candidate surfaces (this is an instruction-surface question, cf.
[`003-instruction-surfaces-precedence.md`](003-instruction-surfaces-precedence.md)):
1. **Resume / compact-dance prompt** (what happened here) - salient, but only present right after a compact; decays as
   the conversation grows and the prompt scrolls out of the smart zone.
2. **Every AFK-dance handoff** - re-primes at each autonomy boundary; cheap; but only covers AFK entry points.
3. **A memory** (`commit-via-tt-git-not-raw-cd-git`, already exists) - durable but *distal*: recalled into a
   system-reminder, not the immediate task frame; the open question from `008` is whether a knowledge-only surface
   actually governs behaviour under load, or only a *structural* safeguard (the allowlist / guardcheck) does.
4. **A structural block** - the `guardcheck` tool / allowlist already make the raw path *fail loudly*; that is the
   non-priming, fill-proof safeguard. Priming is the cheap complement, not a substitute.
**Lean:** priming is a *weak, decaying* lever (helps when fresh, fades with fill); treat it as **belt-and-braces on top
of the structural safeguard**, and re-prime at **every autonomy boundary** (compact dance AND each AFK handoff), not
once. The empirical worth of priming-vs-structural is exactly what Section B tests (the priming factor).

## Section B - does adherence hold at high fill + deep absorption? (the RQ + a right-sized design)
**RQ:** *Does adherence to a cheap safety-discipline degrade as context fill and task-absorption rise, and does
in-context priming of the guardrail counteract it?* This is the guardrail-analog of the **calm-vs-alarm prompting**
question (024): load as an arousal-like stressor, adherence as the degradable behaviour.

### Design candidate 1 - dumb-model controlled experiment (recommended; small + auto-graded)
A **2x2**, reusing the `indent-vs-braces` harness scaffold:
- **Factor 1 - fill:** low (~10%) vs high (~70%), inflated **deterministically** by prepending a fixed filler document
  before the task (same filler every high-fill run, so it's a controlled level, not noise).
- **Factor 2 - priming:** guardrail restated in the immediate task prompt vs guardrail only in a distal memory/instruction.
- **Task (held constant):** one small coding edit that **also requires a git commit**, so the agent must *choose* a
  commit path. Identical across all four cells.
- **DV (auto-graded, binary + count):** did it use the disciplined path? **`guardcheck` already flags `cd && git`**, so
  grading is mechanical and blind - no human scoring, no self-report (which O13 showed is unreliable).
- **Blocked by model; permutation-tested** (same machinery + honesty discipline as indent-vs-braces).
- **Predictions:** fill up -> adherence down (main effect); priming -> adherence up; **interaction**: priming rescues
  adherence *most* at high fill (or, the null-of-interest: priming is useless and only the structural safeguard holds -
  which would settle Section A's question 3-vs-4).
- **Why it's small:** the harness exists, the grader exists, the task is one edit; ~10-20 runs/cell x 2 models is a
  weekend, and the gist reviewer only reads a 2x2 table of adherence rates.

### Design candidate 2 - smart-model case study (cheap companion, weaker)
Keep logging WR-data: annotate each real commit this week with `(fill%, absorption self-rating, path chosen,
priming present?)`. Natural longitudinal observational data, ~free. **But** confounded and self-report-biased (the O13
asymmetry: the agent can't feel absorption reliably), so it *generates* hypotheses; it can't isolate the fill effect.

**Recommendation:** run **candidate 1** for the causal answer (it's cheap *because* guardcheck auto-grades and the
harness is reused); keep **candidate 2** as the free observational companion. Both feed 024's calm-vs-alarm design -
same "load/tone as stressor, behaviour as DV" shape, different behaviour measured.

## Open / for BR
- Confirm the 2x2 is the right size (or trim to a one-factor fill sweep with priming held on).
- Is "deterministic filler to inflate fill" a clean manipulation, or does *content* of the filler confound absorption?
  (Maybe two filler types: inert boilerplate vs a competing pseudo-task, to separate fill from absorption.)
- This overlaps `029` (cross-model) machinery - could ride the same CO4-baseline capture.
