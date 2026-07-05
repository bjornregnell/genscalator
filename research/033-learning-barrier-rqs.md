# Research — learning-barrier RQs + the containment thesis (keeper, 2026-07-04)

**Status:** open, keeper RQs from a walk-thinking exchange (BR 2026-07-04, Opus 4.8). BR concurred with the reframes
below. Ties to blog 007 (learning beyond the inference barrier), 006 (introspection-is-not-self-control), 005 (dances),
and [[cross-model-psyche-comparison]] (the Fable-5 sub-question + the harness-coupling method).

## Main RQ (post-AT, highest priority)
Will the **frontier stack** (Fable 5 + newer harness + accumulated substrate) do **better at inference-time learning** —
enough to reach **practically-significant** safety + productivity gains via the genscalator approach — than the current
Opus-4.8 + current-stack baseline?
- **3-way-confound warning.** "Fable 5 + new harness + new substrate" bundles **three** treatments; if the stack does
  better we can't attribute it. **Decompose:** hold harness+substrate fixed across the model cut (pin CLI, change only
  `--model` — see [[cross-model-psyche-comparison]] harness-coupling note), and upgrade the harness as a **separate,
  later, measured** step.

## Sub-RQ 1 — utilization vs architecture ("is it just us not being smart enough?")
Split the performance ceiling into two halves with **different owners and different fixes**:
- **Utilization failures** — fixable by being smarter. E.g. the 2026-07-04 allowlist-defeat: the guard existed, the
  agent spoke in a shape (`cd … && tt …`) it couldn't match. Genuinely improvable.
- **Architectural failures** — NOT fixed by smartness. **Key datapoint (2026-07-04):** the agent committed the blog post
  cataloguing the bash-reflex, then re-fired the reflex ~4 min later — full knowledge, maximum salience, still
  regressed. This **proves the ceiling is not purely utilization**: a gap that survives complete self-knowledge needs a
  structural guard or a different model, not more cleverness.
- We **cannot estimate the ratio** without the frozen-baseline discipline (→ before the Fable switch).

## Sub-RQ 2 — containment, not taming (BR conceded the reframe)
BR's original frame: *"is taming (all/most) reflexes and habits a hard requirement?"* **Reframed + agreed: taming is the
WRONG requirement.** Introspective self-control is unreliable (006; re-demonstrated 07-04). The thesis is **containment**:
make the harmful shape **impossible** (typed tool, hook, allowlist) so the reflex persists **harmlessly** — the only
available shape is safe. **Reflex-rate need not reach zero.**
- **The real hard requirement = structural COVERAGE:** for every *high-cost* reflex, a guard that removes the bad
  affordance. It is **countable** (guard-coverage of the top-N costly reflexes), **model-agnostic** (→ invest before the
  Fable switch), and it is the thing that **accumulates across sessions**.

## Sub-RQ 3 — the barrier isn't binary; operationalise the "break" as a gradient (BR agreed)
"Breaking the barrier" is catchy but **misleading** — the model still doesn't learn (frozen weights). Better verb:
**route around / relocate.** Learning is relocated from the weights into the **joint agent+substrate system**.
- **Operationalisation:** does a guard/dance/memory adopted in session N **measurably reduce its target failure in
  session N+1**? Under frozen weights, *learning IS the substrate delta that changes behaviour.*
- **Success metric:** the substrate accrues guards **faster than reflexes + rot erode them** — **net guard-accumulation
  > 0**, and large enough to be **practically** (not merely statistically) significant (the *Experimentation in SE*
  distinction).
- **Fable-5 sub-question:** does a stronger model **lower the required guard-coverage** (self-corrects more → fewer
  structural guards for the same safety)? Measurable as **guard-coverage-at-fixed-safety**, per model.

## "Have we already achieved inference-time learning?" — YES, to a meaningful extent (BR asked; agreed)
Not weight-learning (frozen). But the **joint system** (agent + human + externalised substrate) demonstrably improves
across time: memories, dances, guards, and the **pin-dance** offloads change future behaviour. **Each pin is
a micro-act of inference-time learning** — the atomic write-op of substrate-mediated learning. So the "inference
barrier" is not *broken*; it is **bypassed**, by moving the *locus* of learning out of the model and into the
environment. This is precisely why "breaking the barrier" mis-describes it — and it reframes the title question of 007.

## Naming — RESOLVED (2026-07-04): the split into `note` + `pin`
The durable write-op is the **pin dance** (cue `pin:`). It was briefly named *etch*, but *etch* is a near-anagram of our
core quality term *echt* — a confusability trap — so it was renamed to **pin** (which also won the embodied cue-word
typing test on ergonomics; see `wr-data/harness-ux.md`). The overloaded "note:" was split off into the **note dance**
(cue `note:` = notice / keep-fluent-this-conversation + pin-candidate) — the attention stage that *feeds* the pin. So
inference-time learning has a two-cue front end mapping onto the two-stage memory model: `note:` (encode) → `pin:`
(consolidate). See [[cue-note-vs-pin]] and `docs/foundations.md`.

Links: [[cross-model-psyche-comparison]], blog 005/006/007, [[hardening-dance]], [[joint-rot-vigilance-recovery-kit]],
[[no-interrupting-modals-during-flow]].
