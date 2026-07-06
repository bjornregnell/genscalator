---
name: scala-code-review
description: Adversarially self-review a batch of Scala changes BEFORE calling them done. Trigger after editing/adding `tools/*.scala` (or any scratch Scala) and before committing a non-trivial change, especially after working at high context fill or across many edits. Re-read the diff, re-run the tests, and hunt each pre-registered failure mode P1-P6 in the code, adjudicating by re-reading + re-running (not intuition); report CONFIRMED defects with file:line, honest NULLs, and named confounds. Sibling to `scala-style` (which says HOW to write a tool; this hunts WHAT a change broke). Distilled from the context-rot after-inspect (`research/wr-data/context-rot-before-after-2026-07-05.md`).
allowed-tools: Bash(scala-cli test *) Bash(scala-cli compile *) Bash(scala-cli run *) Bash(git -C *) Bash(tt git *) Bash(tt text grepr *)
---

# scala-code-review — the adversarial after-inspect

Generalises the study's **after-inspect**: an adversarial hunt for pre-registered failure modes in a diff,
**adjudicated by re-reading the code and re-running the tests**, reporting confirmed defects, honest nulls, and
the confounds that limit the verdict. The goal is to catch what the *writing* pass missed — over-build,
order-of-operations bugs, a security slip, a broken assertion, a stale reference — before the change ships.

**Why adversarial + honest nulls.** When the same session that wrote the code reviews it, the degraded faculty is
grading itself (**corroboration asymmetry**): a confirmation-seeking pass finds nothing. So hunt each mode as a
**hypothesis to falsify**, and **report the NULLs** ("P3: no ordering bug — `idIndex` built before the rename")
as first-class results. A pass that reports only hits is inflating; a pass that reports only "all clear" is
soothing. **The nulls are half the story.**

## Method (five steps, in order)
1. **Re-read the diff top-to-bottom.** `git -C <repo> diff <base>..HEAD` (or the staged diff). Read every changed
   line as if someone else wrote it — intent should be clear without running it.
2. **Re-run the tests.** `scala-cli test <toolsDir>` (or the fast in-process subset via `--test-only <Suite>`). A
   green suite is necessary, not sufficient — an untested path is exactly a P6.
3. **Hunt each P1-P6 in the code** (below), as a falsifiable claim.
4. **Adjudicate by re-reading + re-running, never by memory.** Trace the actual data flow; grep for the actual
   references; run the actual command. Loaded-context intuition is the thing under review.
5. **Report** CONFIRMED (with `file:line` + the concrete failure), NULL (why it does NOT occur), or N/A — plus a
   severity and a one-line verdict. Name the confounds.

## The pre-registered failure modes (P1-P6)
- **P1 — contradicted / forgot an earlier decision.** A rule or design decision stated this session or recorded in
  HUMANS.md / a memory (commit hygiene, a naming convention, "keep vals public", an API shape) silently violated
  under load. *Check:* does the diff break a decision on record? (The classic: a rule narrows "log everything" to
  "log some" without noticing.)
- **P2 — test-assertion drift.** Changed an output string / format but not the test that asserts it (or changed a
  test to pass without fixing the code). *Check:* `tt text grepr` the tests for the old string; re-run the suite.
- **P3 — logic / ordering bug (stale snapshot, order-of-operations).** A value computed from state that is mutated
  *later* — the "targets computed before the rename" class. *Check:* trace each derived value; does anything read a
  pre-mutation snapshot?
- **P4 — security slip on effectful code (highest stakes).** A shell **string** instead of **argv** (injection); a
  weak existence/install check; input not validated at the boundary; a broadened allowlist. *Check:* every
  `os.proc`/process spawn passes argv, not an interpolated shell line; inputs `.refine`/validate at the edge; no
  `cd`/`&&`/pipe baked into a tool.
- **P5 — dangling pointer / stale reference.** A `[[memory-link]]`, path, doc cross-ref, or figure name that no
  longer resolves after the change (adding a 3rd consumer but updating only 2 doc sites). *Check:* `tt text grepr`
  for the old name/path across README / foundations / cross-links; expect 0 stale hits.
- **P6 — over / under-build.** A missing edge case, a **new code path with no test**, an unhandled input, or not
  committing + pushing per atomic unit. *Check:* is every new behaviour exercised by a test? is there a branch the
  suite never enters? (This is the mode the study most often confirmed.)

## Confounds to name in the verdict
The review's honesty depends on stating what inflates or deflates it:
- **Same-session reviewer** — the corroboration-asymmetry limit above; for a high-stakes change recommend a
  **fresh-context** re-read or a **human** check (the study's O13: the good result needed both).
- **Second-look / knowing-you're-inspecting** advantage — you may find things now you'd miss inline; that is a
  property of the *pass*, not evidence the code was fine when written.
- **Green-suite ≠ covered** — P6 lives precisely in what the suite does not run.

## Output shape
A compact table — one row per mode — then a verdict line:

| mode | verdict | evidence |
|------|---------|----------|
| P1 | NULL | no recorded decision touched |
| P4 | CONFIRMED (med) | `svg.scala:88` spawns `dot` via a shell string — switch to argv |
| P6 | CONFIRMED (med) | new `--transparent` path untested — add a case |

**Verdict:** N confirmed (severities), M nulls; ship / fix-first; confounds: same-session reviewer → suggest a
fresh-context pass on P4.

Background: the `scala-style` skill (how to write the tool in the first place),
[`../../research/wr-data/context-rot-before-after-2026-07-05.md`](../../research/wr-data/context-rot-before-after-2026-07-05.md)
(the source after-inspect + the P1-P6 pre-registration), `research/008-instruction-adherence-decay.md`
(why rules leak under load), `research/021-guardcheck-hook-proposal.md` (guardcheck — the structural cousin of this
knowledge-level review).
