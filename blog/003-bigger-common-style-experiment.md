# A bigger common-style experiment (003) — STUB / preregistered, not yet run

> **Status: STUB, 2026-07-03. Author: Björn Regnell.** A **preregistered** design awaiting a run — the confirmatory
> sequel to [002](002-braceful-or-braceless-or-the-common-style.md). Nothing here is a result yet; the results and
> conclusions sections are deliberately empty until the run happens and reports *whatever it finds* (including a
> null). This stub holds the plan.

## Why a sequel

[002](002-braceful-or-braceless-or-the-common-style.md) was an honest **pilot with a null**: across 7 small local
models, braceless edits were costlier *in aggregate* (−17.5 pp pass-rate vs braceful) but the effect was **not
statistically significant** (exact permutation p ≈ 0.46) and **bidirectional per model**. A pilot points the way; it
does not settle the question. 002 §7 named exactly what a verdict needs — *more models, for statistical power.* This
post is that follow-up: a larger run **designed in advance** to have a real chance of confirming or refuting the
direction, **without fishing for the result.**

**Pilot proposes, preregistered run disposes.** 002 *generated* the hypothesis; 003 *tests it out-of-sample* on
models disjoint from the pilot's 7.

## The preregistration (the honesty guarantee)

The full frozen design lives at
[`../research/experiments/indent-vs-braces/BIG-RUN-PREREG.md`](../research/experiments/indent-vs-braces/BIG-RUN-PREREG.md).
Its anti-fishing core: **fixed n, one primary test, no optional stopping, a committed random seed, and a standing
commitment to report the null if it stands.** (Preregistration = lock the hypotheses, sample size, primary test and
analysis script *before* collecting data — so you can't try many tests and report the smallest p, drop inconvenient
models, or stop the moment it looks good.)

- **Primary test:** paired permutation, braceless vs braceful pass-rate, **blocked by model** (the model is the unit
  of replication — pooling cells is the pseudoreplication that faked p = 0.008 in 002 §5.5).
- **Power basis:** the pilot's effect size is small (d ≈ 0.37, inflated by outliers) → ~55 models for 80% power.

## Hardware & feasibility — why the "lame GPU" is actually on-target

Checked 2026-07-03, `bjornyx.local`: Quadro RTX 3000 **6 GB VRAM**, **523 GB free disk**, 30 GB RAM, 16 cores.
**No showstopper:** VRAM caps model *size* (≤ ~8B, quantised), not *count*; the disk holds 100+ small models; ~50
models × 2 700 cells is a single overnight autonomous job (the pilot already ran 378 cells autonomously).

The reframe that matters: 002's rule is *"design for the weakest agent that will edit the code."* So **small local
models ARE the target population** for the weak-editor question — a 6 GB card testing many of them is the right
instrument, not a poor-man's substitute. What it *can't* reach is mid/large models — a separate question (the
capability gradient), which is what bigger hardware would add.

## Tiered design

- **Tier A — small-model confirmatory (bjornyx, runnable now).** ~50 distinct ≤8B models disjoint from the pilot 7,
  the 002 task family (wrap-in-`else`, 3 sizes, 3 styles), R = 6. Answers: *does braceless-costs-more hold, and
  significantly, across many weak models?* One overnight AFK job.
- **Tier B — capability gradient (bigger hardware, later / optional).** Add mid-tier models (needs more VRAM — a work
  GPU) to trace how the effect shrinks from small → frontier, **bridging 002's two endpoints** (weak-model effect vs
  the frontier's zero effect). A bonus, not a prerequisite for Tier A.

## Research questions & hypotheses

Frozen in the prereg (H1 adherence · H2 primary: braceless ≥ braceful error, controlling for adherence · H3
frontier: style-insensitive at the top). See `BIG-RUN-PREREG.md` §1–2.

## Results — _TBD after the run_

_(To be populated: primary p, the model × style picture at ~50 models, the adherence-vs-correctness split, and the
cost vector — tokens, repair cycles, wall-clock. Whatever it says.)_

## What it means — _TBD after the run_

_(Confirm / null / bounded — written honestly from the data, in the same spirit as 002.)_

## How this post was made

Same disclosure ethos as [002 §9](002-braceful-or-braceless-or-the-common-style.md): Björn Regnell is the author and
accountable; a coding agent built and ran the harness; every number will be reproducible from the committed scripts,
raw data, and the preregistration; the AI is a disclosed tool, not an author.

---

*Working notes / plans accumulate here and in `BIG-RUN-PREREG.md` until the run is scheduled.*
