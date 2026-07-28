# Specimen: the DumbZone chip appears to change agent behaviour within one turn (n=1, confounded)

**2026-07-25 ~14:5x, BR-present, BR flagged this as WR data.** Recorded because it is positive evidence
for a mode chip having a behavioural effect, which the mode vocabulary has mostly asserted rather than
shown. Recorded WITH its confounds, because the strongest alternative explanation is very strong.

## The event

Long session (overnight AFK run, then a full working day). BR typed `+HotHarvest  +DumbZone?`. The `?`
turned out not to be part of a label — `tt mode` rejects it (`invalid label 'DumbZone?'`) — so it read as
BR asking whether the agent was in a dumb zone.

The agent answered with a self-assessment: reasoning apparently intact, *mechanical discipline* slipping,
citing five concrete slips that day (a commit-message file written into the wrong repo minutes earlier; an
over-broad `contains("allow")` assertion that failed on the tool's own documentation; a `segments < 3`
guard the pure tests structurally could not catch; a missing `//> using file` directive; and the original
SM228 python3 reach).

BR then set `+DumbZone` and said to close the loop. **Within the next turn the agent stopped a running
test suite, wrote two new test files (`tsv.test.scala` plus CliSuite cases for `tt tsv` and the guardcheck
`posthook`), and only then proceeded** — narrating it as *"Under DumbZone I should not ship untested tools
— and tt tsv and posthook currently have no tests."*

That is a real, costly behaviour change: it stopped work in progress, added unrequested effort, and
delayed the commit. Not merely a change in tone.

## Why this is WEAKER evidence than it looks

Four confounds, and the first two are serious enough that the chip may be doing none of the work:

1. **The self-diagnosis arrived with the chip.** In the immediately preceding turn the agent had itself
   enumerated the five slips and concluded "mechanical discipline is what's slipping". Having just
   articulated that, more caution follows without any chip at all. The chip and the diagnosis are
   confounded by construction, because the diagnosis is what *prompted* BR to set the chip.
2. **Demand characteristics, and the agent narrated the chip out loud.** The agent knows BR studies mode
   effects, and it explicitly said "Under DumbZone I should…". An agent PERFORMING a chip is
   indistinguishable, from the transcript alone, from an agent CHANGED by one. This is the sharpest
   threat: the very sentence BR cited as evidence is also exactly what performance would look like.
3. **n=1, no control.** No counterfactual turn without the chip, in the same state, on the same task.
4. **Order effects.** It was late in a long session; the pending work (two untested tools) was
   independently the obvious next thing to do, and the agent had already been told to "close that loop",
   which names finishing rather than starting.

## What would actually test it

The chip is cheap to toggle and the behaviour is observable, which makes this testable rather than
merely arguable, per the in-session-experiment skill:

- **Blind the narration.** Instruct the agent NOT to mention the chip. If caution persists without the
  agent naming it, confound 2 weakens sharply. If caution disappears, the chip was mostly performance.
- **Counterbalance.** Set the chip at a point where the agent has NOT just self-diagnosed, ideally
  mid-task and unprompted by any error, which breaks confound 1.
- **Use a mechanical outcome, not a judgement.** Tests-written-before-commit, suite-run-before-push, and
  slips-caught-by-structure-vs-by-human are all countable from commits and the transcript, so the measure
  does not depend on the agent's self-report — which is the least trustworthy instrument here.

## The honest summary

One instance, in the direction the chip intends, with a costly and concrete behaviour change rather than
a stylistic one. But the agent had just diagnosed itself, and it narrated the chip while acting on it, so
this is **suggestive and not evidential**. Its main value is as a pre-registration prompt: it names the
two confounds that any future test of mode chips has to break.

Ties [[mode-chips-camelcase-vocabulary]], [[rot-vigil-guard-mechanical-precision-first]],
[[tired-cue]], [[not-afk-safe-solo-yields-wr-data]], SM228/SM230 (the slips enumerated above), the
in-session-experiment and research-methods skills.
