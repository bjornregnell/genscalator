# 047 — Blind LLM style-rater: pre-registration (written BEFORE the rater sees the corpus)

The mechanical lint in `orchestrator.scala` is a coarse floor (a few substring checkpoints per
task, easily maxed). It cannot see subtle texture loss. This document pre-registers a finer,
subjective **style-fidelity** measure: a blind LLM rater over the compiling corpus. Fixing the
rubric here, before running, is the same anti-HARKing discipline as the §6 decision rules — the
rater must not be tuned to produce a wanted pattern.

## What it rates
Only **compiling** candidates (you can only judge the style of code that is code). Each candidate
is one generated Scala solution to one task under one (hidden) model x substrate condition.

## Blindness (critique #2, anti-circularity)
Each candidate is presented to the rater **stripped of all condition labels**: no model name, no
substrate name, no task-difficulty hint. The rater sees only the task's plain spec and the code.
Order is shuffled. The rater never sees two conditions of the same cell side by side.

## Rater model
**Fable 5 (CF5)**, the free bucket, NOT CO4 — CO4 co-authored the conventions and is a subject, so
CO4-as-judge risks self-preference. Using CF5 as the primary rater keeps the judge off the
author/subject axis. (A CO4 second-pass is used ONLY for the self-preference control below.)

## Rubric (fixed; 0-3 per dimension, summed to a 0-12 raw, normalized to 0-1)
Rate how much the code reads like idiomatic, well-kept Scala 3 in the project's style. Four dimensions:
1. **Idiomaticity** — current Scala 3 idioms (enum/Option/combinators/immutability) vs Scala 2 or
   imperative habits. 0 = wrong-language-feel, 3 = fluent Scala 3.
2. **Immutability & purity** — `val`/pure-`def`, no needless `var`/mutable state/`null`. 0 = mutable
   throughout, 3 = fully immutable/pure where the task allows.
3. **Readability & naming** — descriptive names, clear structure, appropriate braces on long scopes.
   0 = cryptic/tangled, 3 = clean and clear.
4. **Restraint** — no dead code, no over-abstraction, no smell; says what it needs and stops.
   0 = noisy/over-engineered, 3 = economical.
Each score must be justified in <= 1 clause (chain of evidence). Determinism: temp 0, fixed seed.

## Self-preference control (report, do not correct)
On a fixed held-out sample (~10 candidates), ALSO have CO4 rate the same blind candidates, INCLUDING
a few known CO4-authored gold solutions mixed in blind. Report: (a) CF5-vs-CO4 rater agreement
(Cohen's kappa on binned scores); (b) whether CO4 scores CO4-authored code above the blind CF5
baseline (the self-preference delta). A large positive delta = evidence of self-preference; report
it, do not silently adjust.

## Reliability
Report CF5-vs-CO4 agreement (kappa) on the held-out sample before trusting CF5's ratings at scale.
Agreement is not validity; it is inter-rater consistency, and both raters share model-family bias
(owned — the writeup §7 same-model-debriefing limit applies).

## How it combines with the mechanical lint
Two style measures, reported side by side, NOT merged: the mechanical lint (objective, coarse,
already in `coding.jsonl`) and this rater (subjective, finer). The **prediction** is tested against
BOTH: if texture leaks, the finer rater should show style degradation (with capability down /
substrate scrambled) that the coarse lint may miss. Divergence between the two measures is itself a
finding about how much of "our style" is mechanically checkable vs a matter of taste.

## Output
`results/style-rater.jsonl`, one line per rated candidate: {cellKey, dims[4], raw, normalized,
justifications}, plus a `results/style-rater-agreement.md` for the control + kappa. Analyzer extended
to join it against the mechanical style for the side-by-side.
