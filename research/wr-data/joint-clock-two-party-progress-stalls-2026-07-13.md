# WR data: the joint clock — two-party progress stalls at BOTH extremes (2026-07-13)

**Type:** WR data — a foundations-candidate concept, captured live during a "lets chat" co-design musing.
**Status:** CAPTURED, not yet structured. BR: *"its too difficult to decide now"* → deep-think deferred to
**SM062**. This note preserves the thread faithfully; the final form (foundations term? blog?) is for SM062.
**Threads:** [[agent-lacks-felt-time-rebind-at-boundaries]], [[cue-we-are-racing]], [[cue-lets-chat]],
[[agent-affective-analogs]] (Yerkes-Dodson), the compact-chrono / wake-latency work, [[instruments-must-not-mimic-harness-disinformation-2026-07-13]].

## The seed (BR)
Musing on the racing/queue idea, BR saw an "(a)symmetry to the clock stopping": *"when msg q of your or mine
msgs filled; my clock is stopped; and it is not released until both you and me idle."* Then the sharp
correction: *"clock is NOT ticking despite both idle."*

## The two clock-stops are mirror images
The "clock" = felt **forward-progress**. Whoever is *waiting* has a stopped clock.

| | Compaction (compact sleep) | Racing queue |
|---|---|---|
| Whose clock stops | the AGENT's (dormant during + after) | the HUMAN's (blocked till the queue drains) |
| Who is free meanwhile | the human (can wander off) | the agent (busy draining) |
| Release condition | **one-sided** — only the human can wake the agent | **two-sided** — needs BOTH idle |
| The pain | wake-latency (human wanders → long dormancy) | human cannot get a clean synchronous turn |
| Instrument built | **bing-bing** (agent→human wake signal) | **racing** vigilance flag (reconcile whole queue) |

The asymmetry BR named: the *release* differs — compaction is **other-gated** (the human must wake the agent),
racing is **mutual** (clears only when both queues empty and neither is active).

## BR's correction, and why it makes the model better
The first framing said "the clocks tick together at mutual idle." **Wrong.** Mutual idle is not a tick, it is a
**stall**: at mutual idle nothing happens, so progress is stopped. "Released" ≠ "ticking" — mutual idle is the
moment the *block lifts* (each is free to act), but the clock only resumes when someone actually breaks idle.

So the joint progress-clock is stopped at **BOTH extremes**, for opposite reasons:
- **Congestion** (queue full / racing) → a party's clock is *blocked* — too much pending.
- **Quiescence** (mutual idle) → the clock is *starved* — nothing pending, a dead pause awaiting a restart.
- **Active balanced exchange** (the middle) → the clock *ticks* — the only band where progress flows.

## The payoff: an inverted-U, tying to the Yerkes-Dodson thread
Progress-rate vs. pending-load is an **inverted-U**: it peaks in the middle and falls off at both ends —
overload (flooded feed) AND underload (idle stall) both stall you. This lands on the existing
[[agent-affective-analogs]] Yerkes-Dodson thread, but lifted from a *single agent's* arousal-vs-performance
curve to the **two-party collaborative-throughput** curve. Same shape, joint subject.

## A formal frame (BR is a CS professor — this maps cleanly)
"Not released until both idle" is almost exactly **termination / quiescence detection** in distributed systems:
the computation is "done" only when no party is active AND no messages are in flight (Dijkstra-Scholten /
Chandy-Lamport). The collaboration is a **two-party rendezvous** whose shared logical clock advances only at
synchronization points. And everything we have been building — compact-chrono, bing-bing, the mode line, the
racing flag — is **instrumentation for measuring and managing clock desynchronization** between the two
parties, i.e. tooling to pull us out of the two stalled states (congestion, quiescence) back into the ticking
band.

## The live instance that started it (see the sibling note)
Mid-conversation the status-line clock **froze at `15:55:21`** because both of us went idle — the joint clock
stopping, rendered literally in the UI. That instance opened the **disinformation tradeoff**: a frozen display
that still *looks* live is a disinformation source. Captured separately in
[[instruments-must-not-mimic-harness-disinformation-2026-07-13]], and central to SM062.

## For SM062 (deep-think, deferred)
Decide the durable home + final model: is this a `foundations.md` term ("the joint clock" / "progress stalls at
both extremes"), a blog beat (candidate: blog 004 UX, or the awareness/observability strand blog 020), or both?
Reconcile with the mode/status/awareness instruments as desynchronization-management tools, under the
disinformation constraint.
