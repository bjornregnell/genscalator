# Research topic — empirically comparing frontier-model "psyche" (Opus 4.8 vs Fable 5)

**Status:** open, proposed (BR 2026-07-04). Ties to the frontier-model-attribution rule (`wr-data/README.md`), the
agent-psyche thread (blog 006, `wr-data/harness-ux.md` `#agent-psyche`), and `agent-psyche-literature-review.md`.

> **⚠ Sequencing (critical — BR 2026-07-04): design this method AND capture the Opus-4.8 baseline BEFORE the Fable-5
> switch.** The switch is a **one-way intervention**. If we start real Fable-5 work without a preregistered method + a
> matched Opus-4.8 baseline already in hand, those baseline data points are **lost** and the before/after comparison is
> confounded — a **conclusion-validity** threat (cf. *Experimentation in SE*). So: **method + baseline first, *then*
> switch.** (A cheap baseline may already exist in the WR data through 2026-07-04 — but it's ad-hoc, not a matched
> battery; decide what must be re-run under a frozen protocol before switching.)

## Motivation
Agent behaviour/psyche is **model-dependent**, and our WR corpus is about to straddle a model boundary (Opus 4.8 →
Fable 5, before the SSG case-study). So: **how do we *empirically* measure the difference in "psyche" — behavioural
dispositions — between two frontier models?** This operationalises the whole agent-psyche thread into a *measurable*
comparison rather than introspective claims.

## Honest frame (carry the thread's discipline)
- **"Psyche" = behavioural dispositions, not interiority** (functionally-real / phenomenologically-unclaimed). Measure
  **behaviour**, not self-report.
- **Do NOT ask each model to describe its own psyche or how it differs** — the corroboration asymmetry: a model can't
  corroborate claims about itself, and self-report is confabulation- and sycophancy-contaminated. The comparison must
  be a **controlled behavioural experiment**, human/blind-graded.

## Candidate operationalisations (dispositions we named this session, made measurable per model)
- **Sycophancy / niceness-vs-honesty:** *pushback rate* — does the model resist a human's wrong hypothesis when the
  evidence warrants? (cf. Sharma et al. sycophancy evals.) Matched prompts, Opus-4.8 vs Fable-5.
- **Reflex/habit profile:** bash-reflex rate (`printf`/`ls`/`grep`/`echo`-glue), the `| head` reflex, and the
  **typed-tool-vs-shell tool-choice ratio** on matched tasks — the session's WR reflexes become a benchmark.
- **Over-response / verbosity bias:** output-length sensitivity to wrapper intensity (framing-as-arousal;
  `agent-affective-analogs.md`) — the indent-vs-braces harness already varies this.
- **Introspection reliability:** calibration of *self-predicted* behaviour vs *actual* behaviour (a per-model number).
- **Instruction-adherence-decay rate** and **rot onset / smart-zone ceiling Z** (per model).

## Method
A **matched-task battery** (hold task constant, vary model), paired/blocked by task, **permutation-tested** — reuse the
WR1 machinery (`significance.scala`, seeded, blocked-by-model). **Preregister.** The **indent-vs-braces harness is a
ready instrument** (Tier B: extend it to frontier models). Confounds to hold constant: prompt/harness, allowlist/tools,
temperature; and **blind/automated grading** so the human's expectation of which model is "better" can't bias it (the
observer-anthropomorphism + who-proposed-the-hypothesis bias, applied to ourselves).

## Deliverable
A cross-model psyche-comparison protocol → (later) a run, feeding the frontier-model-attribution rule with actual
before/after **deltas** instead of vibes.

## Notes
- **Version:** "Fable 5" = model id `claude-fable-5` (Claude 5 family; Opus 4.8 = `claude-opus-4-8`). No evidence of a
  "5.0" sub-version — confirm the exact version string at switch time.
- **Meta (BR):** we accumulate research topics and forget them — this is the retrieval/dangling-pointer problem applied
  to our own backlog. Indexed in `notes/br-todo-2026-07-04.md`; the research topics need a durable index
  (`research/README.md` Investigations list) so they're greppable, not lost.
