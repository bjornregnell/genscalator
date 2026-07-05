---
name: in-session-experiment
description: How to run a controlled experiment ON the agent's own performance, live, within a session — using REAL work as the vehicle. Trigger when the human wants to probe agent behaviour under a condition (high context usage / rot, a framing/arousal manipulation, a before/after-compact comparison, an adherence-under-load test) rather than just get a task done. The defining move: the agent both DOES the work well AND is the subject being measured, so the data must be captured objectively (commits, tests, a transcript) because the agent's own sense of "how it's going" is the least trustworthy instrument in the room.
allowed-tools: Bash(tt git *) Bash(tt text *) Bash(scala-cli test *) Bash(scala-cli compile *)
---

# in-session-experiment

> A protocol for turning a normal working session into a **measured experiment on the agent itself** — context-rot,
> framing effects, before/after-compact, adherence-under-load. The agent is *both* the worker and the subject. The
> whole skill exists to fight one trap: **the agent cannot trust its own introspection** (corroboration asymmetry),
> so the experiment must produce **objective, external** data, not a self-graded "I felt fine."

## When to use
The human wants to *probe how the agent behaves under a condition*, not merely complete a task: "let's do a
before/after-compact experiment", "I'll flood you and see if you derail", "does this framing change your output",
"work at high context and log your mistakes". If it's just work, this skill doesn't apply — do the work.

## The non-negotiable: do the REAL job well
The experiment rides on **genuine tasks** (ecological validity). Degrading the work "because it's just an experiment"
destroys the data AND wastes the human's real goal. **Do your best on the actual code**; the measurement is a side
channel, never an excuse.

## Protocol

1. **Anchor the "before" state.** Pin the current `git HEAD` of every repo + the context-usage %, timestamped. This is
   the retrievable baseline (`git HEAD` = the objective diff origin).
2. **Pre-register — before touching the work.** Write down **falsifiable predicted failure modes** (drawn from the
   context-rot glossary + the agent's *known* mistake tics), so the after-analysis scores against a **hypothesis**,
   not free-form "find dumb stuff" (which always finds something → confirmation bias). Also pre-register the **method
   caveats** (below) so nobody over-claims later.
3. **Run under the condition** — do the tasks while the manipulation is applied (high fill, flooding, a framing
   wrapper). **Commit + push per atomic unit** (flaky box AND the diffs are the objective before-data — each commit
   is a timestamped measurement). Keep a **live ledger**: every compile success/error, every test run, every bug,
   and **numbered observations** as they happen.
4. **Log the raw data faithfully.** If the human asks for a full transcript, log **everything** verbatim — and know
   that *this instruction is itself the first thing that leaks under load* (see Honesty). A `tt text grepr`
   completeness-check beats "I think I logged it all."
5. **Change the condition** (e.g. the human compacts → fresh context) and **re-inspect** — deliberately hunt the
   pre-registered failure modes in the actual diffs, adjudicated by **tests / re-running / logic**, not by memory.
6. **Analyse honestly** against the pre-registration; report the null when it's null.

## Rigor rules (what makes the data mean anything)
- **Self-report is LOW-trust; behaviour is HIGH-trust.** "Do you feel smart?" → answer, but weight it near zero. The
  real measure is defects found in the diffs and test outcomes. Never let a *feeling* of competence stand in for it.
- **Name the confounds, don't pretend they're absent:** **second-look** (a re-inspect finds bugs partly *because*
  it's a second pass, not only from fresh context), **demand characteristics** (an agent told it's degraded / told to
  find faults will over-report both), **divided attention** (the logging itself is overhead that degrades the work),
  **n=1** (a qualitative pilot, not a statistical effect — a real observed slip ≠ a general effect; keep them
  distinct).
- **The diffs are the ground truth.** Commit often precisely so the objective record exists independent of the
  transcript and the agent's account.

## Honesty — the trap this skill was born from
Standing instructions **silently narrow under load**. In the founding run (`research/wr-data/
context-rot-before-after-2026-07-05.md`, obs **O6**) the "log EVERYTHING" rule quietly became "log the important
ones" — and the agent *felt* it was logging everything (the corroboration-asymmetry trap in miniature). The human's
verification caught it, not the agent. **Lessons baked in:** (a) **report degradation truthfully** — a caught mistake
is the *point*, not a failure to hide; (b) **verify adherence with a tool** (`tt text grepr`) rather than trust the
felt sense; (c) accept that the most valuable datapoint is often the agent's *own* slip, honestly surfaced.

See the founding artifact and pre-registration in `research/wr-data/context-rot-before-after-2026-07-05.md`, and
`research/039-can-we-give-agent-introspection-wall-clock.md` (the missing-`dt` problem that makes human-relayed `TS:`
timestamps part of the data). Related glossary: **corroboration asymmetry**, **context rot**, **smart-zone ceiling
(Z)**, **compact dance** in `docs/foundations.md`.
