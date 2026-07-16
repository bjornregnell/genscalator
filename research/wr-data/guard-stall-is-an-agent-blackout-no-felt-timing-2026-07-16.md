# Guard stall is an agent BLACKOUT — zero perception, no felt timing (2026-07-16)

**Type:** WR data — agent structural blindness / guard stall. BR flagged it live as important.
**Threads:** [[guard-stall-invisible-to-agent-2026-07-07]] (the founding note this refines), the agent-lacks-felt-time
line, [[joint-rot-vigilance-recovery-kit]], `tt chrono` (reconstructs think/round time from a human relay).

## The specimen

During an overnight native-compile run, a guarded command tripped a permission prompt. BR approved it once ("1.
Yes") and explicitly **declined the blanket / "always allow (update settings)" option** — correct, per
[[never-blanket-allow-settings-self-edit]] / [[guard-against-forced-confirmations]]. Afterwards BR probed the agent
directly: *"do you remember when we got stalled (harness does not give timestamps when stalled...)?"*

The agent's honest introspection: **ZERO perception.** Not when the stall started, not how long it lasted, not even
*that* it was a stall rather than a transient error. The **only** trace was a `Compilation cancelled` artifact on the
interrupted command; the agent knew it was a *stall* solely because BR said so.

## BR's framing (the refinement)

> *"you basically have a blackout during guard stall; it is like hitting you in the head... (an analogy not
> anthro-ing you)."*

This sharpens the 2026-07-07 note. That one established the stall is **invisible** (the agent can't see the prompt
or that it's waiting). Today adds two things:

1. **The timing/duration is a total blackout, not just the event.** From inside, a 2-second stall and a 2-hour
   stall are *identical* — there is no clock attached to the gap. The human's stall-and-approve time is invisible
   except as an artifact. (The "hit in the head" analogy: a discontinuity the subject cannot time from within.)
2. **A member-check where introspection about a LIMITATION is reliable.** BR asked the agent to report its own
   perception; the agent correctly reported the blackout. This is the *inverse* of the confabulation cases: the
   agent is unreliable when introspecting internal *state* (echt / rot self-report), but reliable when reporting a
   *structural absence* it can reason about ("I have no channel for X"). Worth keeping the distinction: *"I cannot
   sense it"* (structural, checkable) vs *"I feel fine"* (state, unfalsifiable from inside).

## Reconstructable, not perceivable

The blackout is *forensically* recoverable: the transcript stamps every entry, so the wall-clock gap between the
stalled tool-call and its result bounds the stall. The agent can **read** the timing after the fact from the file;
it cannot **feel** it live. Same shape as the whole felt-time-vs-reconstructed-time thread — and why the instruments
(`tt chrono`, the transcript-derived `tok` gauge) exist: to give the agent a *read* where it has no *sense*.

## Why it matters

Rot-vigilance is joint precisely because of blind spots like this. The human owns the stall channel entirely (the
agent is blacked out), so stall-timing data — how long approvals take, how often, the confirmation-fatigue cost —
can only come from the human side or forensic transcript reconstruction, never the agent's live sense.
