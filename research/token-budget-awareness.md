# Token-budget awareness across multi-level limits

- **Question:** How can an agent become aware of its **token velocity** (its spend *rate*, dS/dt — see the
  `docs/foundations.md` glossary) relative to **multiple nested limits** — the per-turn context window, and
  provider **session / daily / weekly** rate caps — and *pace* itself accordingly, so it never burns a hard
  cap on brittle/inefficient work and **halts mid-task**?
- **Why it matters:** This is the consequence layer under the whole WR thesis. The `wr-data` friction
  (dynamic shell, re-polling empty stdout, re-deriving aggregations, re-running brittle pipelines) isn't
  just slow — it *spends real tokens*, and it tends to show up as **rising token acceleration** (d²S/dt²:
  each re-poll/re-derive longer than the last). An agent unaware of its **token velocity/acceleration** can
  exhaust a **weekly** cap and then **stop in a broken/uncommitted state** — strictly worse than doing less,
  more carefully. The introspective bet of this note: an agent that *watches its own velocity and
  acceleration* (a speedometer + tachometer for spend) can brake before the budget governs it. Token
  efficiency stops being a nicety and becomes *liveness*.
- **Status:** open — prompted by a real AT session (2026-06-30) where the human had to report usage
  manually because the agent had no way to read it.

## Findings (so far)

### The limits are nested and have different failure modes
1. **Context window (per turn):** soft-degrades into the *dumb zone* (see `docs/foundations.md`) — quality
   drops, but you can compact/continue. Already addressed by token-efficiency discipline.
2. **Session / daily / weekly rate caps (provider):** a **hard halt**. When hit, work stops — possibly
   between a commit and a push, or mid-edit. This is the dangerous one and the least visible.

### The agent currently cannot see the meter
Confirmed in the AT case: there is **no tool exposing the usage meter / remaining budget** to the agent.
Awareness can only come from (a) the **human reporting** it ("1% weekly used, resets Tue"), (b) the
**harness surfacing** a budget signal, or (c) the agent **reasoning about relative cost** (it knows a
20-row table or a re-poll loop is expensive, even without a number). Today only (a) and a weak (c) exist.

### Why this is a genscalator question, not just a model question
genscalator's two pillars already *reduce waste* so the budget buys real work:
- **safe-by-design `tt` tools** (the `wr-data` thesis) remove the dynamic-shell spend + the confirmation
  stalls;
- **instrument-by-default** (`instrumentation-by-default.md`) removes the re-poll / re-derive spend.

But both are *micro* (make each action cheap). Missing is a **macro / governance** layer: *knowing the
budget and pacing to it.* That's this investigation.

## Open design directions (to explore / propose)
1. **A budget instrument (speedometer + tachometer)** — the token analogue of `progress.txt`: a readable
   signal of *remaining* budget at each level (turn / session / week + reset time) **plus the derivatives**
   — **token velocity** (dS/dt) and **token acceleration** (d²S/dt²). Remaining-budget alone is a fuel gauge;
   velocity tells you *time-to-empty at current rate*, and acceleration is the early alarm that something has
   gone brittle/runaway *before* the gauge moves much. If the harness can surface these, the agent *Reads*
   them and adapts; if not, a convention for the human to drop a remaining-budget figure into a known file,
   from which the agent can estimate velocity itself across turns.
2. **A cost reflex** — before an expensive or brittle step, prefer the cheap typed path. Largely the same
   muscle as the `tt`/instrument work, but framed as "don't spend tokens you can't afford," not just "be
   tidy."
3. **Budget-tiered behavior** — ample budget → thorough (extra verification, exploration); tight budget →
   conservative: **finish-and-checkpoint over speculative work**, commit+push *before* risky steps, prefer
   one good attempt over iterate-many.
4. **Halt-avoidance invariant** — as budget tightens, *never leave work in a broken or uncommitted state.*
   Checkpoint (commit+push) early and often so a hard halt is recoverable, never destructive. (In the AT
   session this was done by reflex — commit+push after every small batch — which is exactly the right shape
   when a weekly cap could fire at any time.)

## Relation to other notes
- `wr-data/` — the friction events that *waste* the budget (the thing to eliminate).
- `instrumentation-by-default.md` — make each tool cheap to use (spend less per action).
- **This note** — the missing layer: *know* the budget across nested limits and *pace* to it, so efficiency
  becomes halt-avoidance, not just speed.

## What shipped
Nothing yet — open question. Candidate first step: a tiny convention/tool for surfacing a remaining-budget
signal the agent can Read, plus a documented "budget-tiered + checkpoint-early" behavior in the agent
contract / a skill.
