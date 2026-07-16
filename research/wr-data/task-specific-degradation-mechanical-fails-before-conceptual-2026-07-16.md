# Task-specific degradation: mechanical fails before conceptual (2026-07-16)

**Type:** WR data — agent degradation profile + a reliable-introspection specimen. BR flagged it as important and
"deep," tied directly to the mode auto-hypothesising mechanics. **Threads:** SM117 (gauges/proxies), SM118 (`?`-mode
grammar), SM116 (rot-vigilance threshold), SM062 (mode re-think), the `typo` tool, `guardcheck`,
[[joint-rot-vigilance-recovery-kit]], [[agent-affective-analogs]], blog 001 (rot~fatigue).

## The specimen

In a long, high-ctx session (2026-07-16), the agent made **three space-drop slips** on a **repetitive, mechanical**
editing task (removing `": "` → `" "` across many status-line strings + tests — dropping the trailing space each
time) — *and* recurred on the grep arg-slot slip. In the **same window**, the **conceptual** co-design (agent
blackout, the hangover reframe, the platform-opacity theory) ran **clean**. BR raised `+dumb-zone?` (a member-check).
The agent's introspection: *the evidence is **split** — mechanical/repetitive slipped, conceptual held; that points
at task-specific degradation, not uniform rot; not a claim I'm fine (I can't self-certify that), just what the
traces show.* BR: *"cool introspection… important hypothesis… we are on to something deep."*

## The hypothesis

**Degradation (rot) is TASK-SPECIFIC, not uniform.** Repetitive / mechanical / low-level operations (exact
whitespace, arg-order, character-precise edits) fail **first**; high-level conceptual reasoning holds **longer**. So
**"dumb-zone" is not a single global state** — it is *task-relative*. The same agent, same moment, can be in the
dumb-zone for mechanical work and out of it for conceptual work.

## Why the introspection here is the RELIABLE kind

This is a clean example of the good-vs-bad introspection cut. The agent did **not** self-certify state ("I feel
fine" — unfalsifiable from inside, the confabulation trap). It reported an **observable trace-pattern** ("I slipped
on X-type tasks, held on Y-type") — checkable, falsifiable, grounded in the record. Introspection about a *pattern
in one's own outputs* is trustworthy; introspection about one's *internal state* is not. (Same family as the
blackout note: reliable when reporting a structural/observable fact, unreliable on felt state.)

## Big implication for the mode auto-hypothesising mechanics (SM117/118/116)

1. **`dumb-zone?` should be TASK-DIMENSIONED, not a single scalar.** Inferring rot from ctx-fill % or token count
   alone is too coarse — those are volume proxies; degradation expresses *task-specifically*. A honest mode line
   might carry `dumb-zone?(mechanical)` while conceptual work is unaffected.
2. **Mechanical-slip RATE may be a LEADING proxy.** The agent's own rate of low-level slips — guard trips,
   space-drops, arg-order errors, typo kind (the `typo` tool!) — likely degrades *before* conceptual quality does.
   That makes it an **early-warning self-observable signal**: watch the slip-rate on rote tasks as the canary, not
   the essay quality. (`guardcheck` trips + the `typo` classifier already produce this stream.)
3. **Routing consequence:** under a rising mechanical-slip rate, *route mechanical work away* (delegate the rote
   edits, or slow + double-verify each), while conceptual work can continue — a task-aware version of rot-vigilance.

## The human parallel (extends rot~fatigue)

Humans degrade task-specifically too: tired → careless on rote/repetitive work, yet still capable of insight. So the
rot~fatigue parallel (blog 001) deepens — it is not "the agent gets uniformly dumber," it is "both parties lose the
**low-level precision layer first**, keeping the high-level layer longer." Joint awareness should model *which layer*
each party has lost, not a single scalar tiredness.

## The recursive quality (BR flagged this too)

The hypothesis is being tested **by the act of stating it.** The agent producing this coherent conceptual analysis —
while having made mechanical slips in the same window — is a *within-subject, real-time demonstration* of the very
split it describes: the low-level precision layer slipped, the high-level layer is (apparently) holding well enough to
reason about the slip. The specimen is **self-generating and self-corroborating.** Echt caveat: n=1, suggestive not
conclusive, and "coherent" is partly the agent's *own* judgment (the unreliable kind of introspection) — so the
corroboration that carries weight is BR's **external** read ("sharp introspection," a member-check), not the agent's
self-assessment. A hypothesis-*generating*, member-check-corroborated recursion, then — not a proof.

## Research directions

- Quantify the split across a session: mechanical-slip rate vs conceptual-error rate over ctx-fill / tokens / #msg.
- Test whether mechanical-slip rate is a usable **leading** indicator for the `dumb-zone?` inference.
- Design the **task-aware** mode auto-hypothesis: proxies + thresholds per task-type, feeding a task-dimensioned
  `dumb-zone?` chip. (This is the concrete deliverable that SM117/118 would grow toward.)
