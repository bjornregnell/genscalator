# Experiment: braces vs significant indentation — agent edit-cost & error-rate

**Status:** harness SKETCH (design + seed task + results template). Not yet run at scale. Framed as *future
work* for the first paper (per BR 2026-07-02) — a self-contained experiment that could be its own paper.

**Parent context:** [`../../001-scala-style-evolution.md`](../../001-scala-style-evolution.md) (the investigation + thesis) and
[`../../017-scala-style-recommendations.md`](../../017-scala-style-recommendations.md) (the Odersky/Regnell/Kerr common-style
note this tests against). Seed evidence (n=1): a real significant-indentation edit bug the agent committed on
2026-07-02 (wrap-a-block-in-`else` → mis-indent → mis-scope → compile fail → repair cycle).

## Hypothesis
For an AGENT's dominant workload (**editing** existing code, not greenfield generation), block-structure edits
are **cheaper and safer with braces** than with significant indentation, because braces make such edits *local
and whitespace-robust* while indentation makes them *global and whitespace-fragile*. Concretely:

- **H1 (error-rate):** first-attempt edit-error rate (compile-fail OR silent mis-scope) is **lower** under
  braces-everywhere and common-style than under braceless-everywhere, and the gap **grows with block size**.
- **H2 (token-cost):** expected tokens-to-correct (edit output + resulting diff + any repair cycles) is **lower**
  under braces/common-style for wrap/extract/add-branch edits, despite braceless's smaller raw character count.
- **H3 (sweet spot):** **common-style** (braces on long/blank-line scopes, braceless on short) ≈ braces-everywhere
  on H1/H2 while staying near braceless on raw surface tokens — i.e. it dominates on expected-cost.

## Independent variable: three code styles
Each task's *before* file is rendered in three equivalent forms:
1. **braceless** — significant indentation everywhere, no optional braces, `end` markers avoided.
2. **braces** — braces on every multi-line block.
3. **common-style** — the Odersky/Regnell/Kerr rule: braces around long scopes (those containing blank lines),
   braceless for short scopes, closing keywords (`else`/`do`/`yield`/`case`/`catch`) as end markers.

## Task corpus (`tasks/NNN-<name>/`)
Each task is a *structural edit* — the class where indentation is load-bearing. Per task dir:
- `before.<style>.scala` — the starting file in each of the 3 styles (semantically identical).
- `instruction.md` — the edit to perform, phrased style-neutrally (e.g. "wrap the dispatch loop body in an
  `else` branch guarded by `cond`").
- `after.<style>.scala` — a known-correct target in each style (the oracle for grading).
- `notes.md` — why this task is interesting; which style it stresses.

Seed edit-task families (the ones where whitespace bites):
- **wrap-in-block** — enclose an existing N-line block in a new `if`/`else`/`while`/`try` (the seed bug).
- **extract-scope** — pull a run of statements into a new nested scope.
- **add-branch** — add an `else if`/`case` to an existing conditional/match.
- **reindent-after-rename** — a rename that changes a block's nesting depth.
- **merge/split-blocks** — join two sibling blocks or split one, across a blank line.
Vary **block size** (5 / 15 / 40 lines) as a covariate to test the "gap grows with size" prediction.

## Measurement protocol (per task × style)
1. Give a fresh agent the `before.<style>.scala` + style-neutral `instruction.md` (+ a fixed style directive
   matching the style). One Edit-tool attempt.
2. **Compile** the result (scala-cli). Record: compiled? (bool).
3. **Semantic grade** vs `after.<style>.scala`: compiled AND semantically equal to the oracle (normalizing
   whitespace/braces per style) → PASS; compiled-but-wrong-scope (silent mis-scope) → the dangerous FAIL;
   compile-fail → loud FAIL.
4. **Token accounting:** edit-output tokens, resulting `git diff` line count (hunk size), and — if the first
   attempt failed — the repair-cycle tokens to reach PASS (cap at K attempts).
5. Repeat R times per (task, style) for a rate (agents are stochastic; whitespace errors especially so).

## Metrics
- **edit-error-rate** = fails / attempts (split loud vs silent-mis-scope — the silent one is the real hazard).
- **expected-tokens-to-correct** = mean(output + diff + repair) over R runs.
- **diff-locality** = changed-line count / semantically-changed-line count (braceless re-indents inflate this).
- Report each **vs block size** to test H1's "gap grows with size."

## Confounds & honesty
- **Model/prompt bias:** the style directive must be equally clear per style; pilot to calibrate. n=1 anecdote
  (the seed bug) is *suggestive, not evidence* — the whole point of the harness is to replace it with a rate.
- **Oracle bias:** grading needs a whitespace/brace-normalizing comparator so a *correct* answer in any style
  passes; build it before running.
- **Self-subject bias:** the agent designing the harness should not also be the sole subject; run across sessions.
- Log any bound/cap ([[../../METHODOLOGY.md]] no-silent-truncation rule).

## How to run (once built)
`tasks/` + a `runner` (TBD — a `tt` tool or a workflow) that loops (task × style × R), invokes an agent per
cell, compiles + grades, and appends rows to `RESULTS.md`. See `RESULTS.md` for the row schema.

## Why it may be paper-worthy on its own
It measures a crisp, novel claim — *tooling substrate (agent editing) should inform language surface syntax,
not just human taste* — with a reproducible protocol. Complements the human-ergonomics framing of the
common-style note with an agent-ergonomics axis.
