# SM045 — TE-efficiency pilot: DRAFT plan (for BR review BEFORE running)

**Status:** DRAFT for review. Nothing here is executed yet — SM045 is explicitly "draft a plan, report before
running." This doc is the report. Author: agent solo (post-compact AFK run, 2026-07-10).

## 1. Goal (one sentence)
Turn Token Efficiency from a vibe into a **measured surface** — quality × wall-time × token-$ per task-class across
model tiers — on ONE verifiable task-class, so we can find the **"lagom fleet"**: the cheapest tier (or composition)
that clears the task's **quality floor**. This pilot is the method rehearsal for a later bigger experiment.

## 2. Why this task-class (the pilot's single verifiable class)
The quality signal must be **auto-gradeable** (no human in the scoring loop — that would reintroduce the very
bottleneck TE studies, and it would make the pilot non-reproducible). Of BR's three candidates (schema'd survey,
fact-check, "code compiles + passes tests"), I propose:

- **PRIMARY: "fix-to-green" — planted-bug repair on small Scala functions.** Each item = a self-contained Scala
  function with a planted bug + a munit test file that fails on the bug and passes when fixed. Quality = **fraction
  of the item's tests passing after the model's patch** (0..1), graded by actually running `scala-cli test`. Real
  (this is what the delegation dance *actually* asks sub-agents to do), objectively gradeable, and **difficulty-
  tunable** (bug subtlety: a wrong operator → a wrong algorithm → a subtle off-by-one / edge case). Difficulty range
  is what makes the knee visible: cheap tiers should clear easy items and fail hard ones.
- **FALLBACK (if fix-to-green is too flaky to grade cleanly across tiers): schema'd extraction with a gold key.**
  E.g. "extract every public def signature in this file as JSON"; grade by set-equality against a gold answer. Pure,
  deterministic grading, zero test-harness flakiness — but less representative of real fleet work. Keep in reserve.

**One class only** for the pilot (BR's scope). Representativeness across classes is the *next* experiment's job; we
flag it as a threat, not solve it here.

## 3. The suite (fixed, versioned, idempotent)
- **~10 items, graded difficulty** (say 3 easy / 4 medium / 3 hard), each: `input.scala` (buggy) + `test.scala`
  (gold tests) + `meta.json` (difficulty label, one-line description). Frozen in
  `research/experiments/te-lagom-fleet/suite/` so every run scores the *same* inputs.
- Items authored by the agent, **spot-checked by BR** (a bad suite invalidates everything — this is the one place
  human review is load-bearing, and it's cheap: 10 small functions).

## 4. Tiers (the pilot's independent variable)
- **Haiku 4.5** (`claude-haiku-4-5-20251001`) — the cheap end.
- **Fable-5** (`claude-fable-5`) — the mid tier.
- **Opus 4.8** (`claude-opus-4-8`) — the quality baseline / ceiling.
- **(Deferred to the bigger experiment)** a **local modly tier** on `bjornyx.local:8080` (deterministic, temp0+seed)
  to extend the range *downward* and map the cheap end — BR framed local models as the later round, so the pilot
  stays on the 3 API tiers. Noted so the harness is built tier-agnostic from day one.

Each tier gets the **same prompt** (a fixed "fix this so the tests pass, return only the patched file" template).
Threat: a single prompt may suit one tier's idiom better than another's (prompt-sensitivity) — mitigated by keeping
the prompt minimal + identical, and flagged as a limitation.

## 5. The harness (reproducible, idempotent, logs everything)
A **scala-cli measurement program** (`te-pilot.scala`), NOT the Workflow tool — measurement wants determinism and a
clean token/time log, not orchestration. Contract, per `(tier, item, rep)`:
1. Fresh context (no cache carryover between runs — cache state would confound $).
2. Call the model API (the `claude-api` skill's endpoint) with the fixed template + the item's buggy file.
3. **Time** the call (wall-clock, ms).
4. Capture **token usage from the API response** (`usage.input_tokens`, `output_tokens`, cache read/write) — the
   real billed quantities, not an estimate.
5. Write the model's patched file to a scratch dir; **grade** by running `scala-cli test` on it → fraction passing.
6. Append one row to `results.tsv`: `tier item difficulty rep quality wall_ms in_tok out_tok cache_r cache_w
   cost_usd seed`.
- **Reps: 3–5 per (tier, item)** — API calls at temperature 0 are low-variance but NOT deterministic, so we measure
  a distribution (mean quality + variance), not a point. Seeds/params logged for re-run.
- **Idempotent:** fixed suite + logged seeds + append-only TSV → re-running reproduces the table (modulo API
  nondeterminism, which the reps quantify). This is the blog-019 "reproducible fleet" property BR asked for.
- **Cost per row** = tokens × per-tier published $/Mtok (a small pinned price table; the cost-snapshot already has
  the three tiers' relative economics).

## 6. Metrics + the TE formula (and why the scalar isn't the whole story)
Per tier, aggregate over items × reps:
- **Q** = mean quality ∈ [0,1] (fraction of tests passing).
- **C** = mean $ per task.
- **T** = mean wall-time per task.
- **BR's scalar: TE = Q / (C × T)** — quality per (cost × time). Useful as a single headline number.

**But the scalar alone is misleading** (it rewards fast+cheap even below a usable quality floor). The *actionable*
analysis is the **lagom-fleet decision rule**, two-stage:
1. **Floor filter:** keep only tiers with **Q ≥ floor** (pilot default floor = 0.9; a knob to sweep).
2. **Minimize** cost (or cost×time) among the survivors → that tier is the **lagom fleet** for this class+floor.

Report BOTH: the TE scalar (headline) AND the **Q-vs-C Pareto frontier** with the **knee** marked (where buying more
$ stops buying Q). The knee + the floor is the real deliverable; the scalar is the soundbite.

## 7. Bonus arm — the double-race ensemble (from the cost-snapshot reasoning)
Cheap: also score **Haiku+Fable run in parallel, agreement-gated** (both patch → if their patches agree / both pass,
accept; else escalate). Question: does the 2-cheap-model ensemble reach the quality floor at **less cost than one
Opus call**? If yes, the "lagom fleet" is literally a *fleet*, not a single tier — which is the whole naming point.
This is the calibration-value of the double-race made measurable ([[cost-snapshot-2026-07-10-usd774-opus-context-reread]]).

## 8. Threats to validity (echt)
- **API nondeterminism** → reps + variance, not point estimates.
- **Grader validity** → the gold tests ARE the quality definition; a weak test suite inflates cheap tiers. BR
  spot-check of the suite is the guard.
- **Single task-class** → the knee is class-specific; do NOT generalize "Haiku suffices" beyond fix-to-green. Stated
  loudly in any writeup.
- **Prompt-sensitivity across tiers** (§4).
- **Price drift / cache effects** → fresh context per run + a pinned, dated price table.
- **Small N** → pilot is a method rehearsal; the bigger experiment scales items, classes, tiers (incl. modly).

## 9. Cost estimate for the pilot itself (TE-conscious, BR pays privately)
~10 items × 3 tiers × 4 reps = **~120 model calls** + the double-race arm (~40 more) ≈ **~160 calls**, each a small
single-file task. Haiku ≈ ~$0, Fable cheap, **Opus is the cost** (as always — the cost-snapshot's 94% finding). Rough
order: **low single-digit dollars**, dominated by the Opus baseline. Cheap enough that the experiment is near-free;
the expensive resource is *authoring + reviewing the suite*, not running it.

## 10. Deliverables
1. This plan (done). 2. The frozen suite (agent-authored, BR spot-checked). 3. `te-pilot.scala` harness + `results.tsv`.
4. A short findings note: the Q/C/T table, the Pareto+knee, the lagom-fleet verdict for fix-to-green at floor 0.9,
   and the double-race result. Feeds **SM039** ($-axis), **RT029** (cross-model), and blog 019 (sibling experiment).

## 11. Open questions for BR (blocking the run)
1. **Task-class:** OK with **fix-to-green** as primary (schema'd-extraction as fallback)? Or prefer the pure-grading
   extraction class for a cleaner (if less representative) first cut?
2. **Quality floor:** 0.9 the right default to sweep around, or a different bar?
3. **Include the double-race arm** in the pilot, or keep the pilot single-tier and add the ensemble in round 2?
4. **Suite authoring:** agent drafts ~10 items for your spot-check — good? (This is the one human-in-loop step.)
5. **modly/local tier:** confirmed deferred to the bigger experiment, not the pilot?

Ties: SM045, SM039, [[cost-snapshot-2026-07-10-usd774-opus-context-reread]], [[cue-use-fleet]], RT029, blog 019.
