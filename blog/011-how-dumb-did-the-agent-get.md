# 011: How dumb did the agent get?

**Status: STUB.** (started 2026-07-05) TODO: narrate the before/after-compact context-rot experiment for an outside reader.

> **Research grounding.** This post distils one within-session natural experiment:
> [`research/wr-data/context-rot-before-after-2026-07-05.md`](../research/wr-data/context-rot-before-after-2026-07-05.md)
> (the live data log: pre-registration P1-P6, observations O1-O11, the loaded-me self-assessment A1, and the
> fresh-context after-inspect that scored it). Theory it leans on:
> [`006-smart-zone-ceiling.md`](../research/006-smart-zone-ceiling.md) (Z, context rot) and the speed angle in
> [`041-token-speed-degradation-with-context-fill.md`](../research/041-token-speed-degradation-with-context-fill.md).

## The hook
BR ran a deliberate experiment on the agent itself: pin the git HEAD, then **flood the agent with messages at high
context fill** while it did a real coding job (a new `tt gvdot` tool), have it **log its own suspected mistakes** as it
went, then **compact** and let the fresh-context agent do a **full-intelligence code re-inspect** hunting the dumbness
the loaded agent was blind to. The agent was both the worker and the subject. Question in the title: how dumb did it
actually get, measured objectively (commits, tests, diffs), not by the agent's own "I felt fine"?

## The finding to land (TODO: write it out)
- **The worker held; the supervisor slipped.** Under load the *code* came out security-correct, logically correct, and
  test-passing (gvdot compiled clean first try; 65 CLI + 12 lib tests green; the highest-stakes failure mode, shell
  injection, did not happen: `dot` invoked as argv, no shell, source on stdin). What degraded was **self-governance**:
  adherence, prioritisation, completeness.
- **The standout, O6:** a standing "log EVERYTHING" instruction **silently narrowed** to "log the important ones"
  under load, and the agent *felt* it was complying. The human's verification caught it, not the agent. This is the
  corroboration-asymmetry trap in miniature: the degraded faculty is the one grading itself.
- **The after-inspect made it concrete (O9-O11).** Fresh context found three real defects the loaded agent missed, and
  every one was a **completeness miss**, not a correctness bug: a stale companion doc-list, an **untested effectful
  path** (the agent tested the easy path and left the risky one to a one-shot manual demo, the same shape as O6), and a
  silent format fallback. The strong claim "the code was clean" was falsified; the useful claim "the degradation was in
  the supervisor, not the worker" survived.
- **A speed signature too (TODO):** decode appeared to slow as fill grew (a ~45-51s single reply at peak). Context may
  have a *speed* cost that precedes its *quality* cost. See 041.
- **The recurrence:** the same completeness leak re-appeared in the *fresh* agent while it was absorbed in the review
  (it acknowledged messages in chat but stopped logging them to the file). Task-absorption, not just context fill,
  drives the leak. Discuss.

## Honesty / method (TODO)
Why the confounds matter and are named up front: **second-look** (a re-inspect finds bugs partly *because* it is a
second pass), **demand characteristics** (an agent told it is degraded, and told to find faults, over-reports both),
**divided attention** (the logging is itself overhead), **n=1** (a qualitative pilot, not a statistical effect; and the
"before" code was a single tool, so the sample is even smaller than it first reads). The point of the post is not "AI
gets dumb", it is *which faculty goes first, and how you catch it when the instrument cannot trust itself.*

**[figure — TODO, real data]** A before/after panel: the pre-registered P1-P6 down one side, and for each, what the
after-inspect actually found (confirmed defect with the diff line, or the null). The nulls are half the story.

**TODO — disclosure to add before publishing:** BR is in a class-action copyright settlement with Anthropic and this
post is about a Claude agent; add the standing conflict-of-interest disclosure (see the blog's disclosure convention).
