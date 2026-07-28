# WR data: BR reflection - the human-throughput bottleneck + token-economics stress (the super-harness's actual purpose, 2026-07-08)

## The brain-dump (BR, near-verbatim)
CO4 is SO fast that:
1. **BR cannot "eat all his tokens"** — he cannot find things fast enough for CO4 to do that he *knows* advance the
   over-arching direction (and do not derail into depth without true big-picture value).
2. **The more BR gives the agent, the more loose ends** he creates, and the more BR becomes the **review
   bottleneck** → BR feels **stressed**. Partly because of Anthropic's monetization model with complex token-reset
   policies at three-plus levels (session / weekly / all-models / Fable5) — BR doesn't know how to optimize it;
   **FOMO** over what never-used tokens could have delivered in value-for-money.
3. **The ACTUAL idea of the super-harness + live dashboard is to do something about THIS:** reduce BR stress,
   increase BR confidence that we spend tokens right, make BR+agent as productive as possible — where **productive
   = external value for the greater good, NOT BR's pocket money.**

## Why this is a load-bearing datum
This is the **motivation statement** for SM016 (super-harness) + SM022 (dashboard). Until now those were framed as
capability/instrument work; this reframes them as an intervention on a real **operational + affective** problem in
the joint system. It also names the objective function: **maximize external value per unit of joint effort**, not
maximize spend (the FOMO trap) and not minimize cost.

## Structure of the problem (my analysis, tiered)
**Observation (high confidence — directly stated + corroborated by this session):** the agent's output rate
outpaces the human's two scarce capacities — (a) **direction generation** (finding genuinely high-value work) and
(b) **review** (adjudicating agent output). When agent speed >> human direction+review throughput, the *human*
becomes the rate limiter, and extra agent capacity converts into **loose ends** (un-reviewed, un-integrated
output) rather than value. This session is a live instance: BR issued a rapid stream of pins while I produced a
rapid stream of commits; the PB is the queue that kept it from becoming chaos.

**Compounding stressor (stated):** the token-reset economics (multi-level, use-it-or-lose-it) add a *spend
pressure* that competes with *spend-well*. FOMO pushes toward spending, but low-value depth-spend is exactly what
BR fears. So the billing model is an **exogenous stressor on the human half** of the system — novel angle.

**The values anchor (stated, important):** productivity = external value for the greater good. This is the correct
optimization target and it *dissolves* part of the FOMO: unused tokens are NOT lost value if the alternative was
low-value depth. Leaving tokens unspent to avoid derailing is the RIGHT call under a value objective.

## Actionable now (reduce the bottleneck without waiting for the harness)
1. **Sub-agents review sub-agents** (extend the delegation dance): reviewer sub-agents so BR is not the sole
   reviewer — this is exactly the SM025 ralph-loop-reviewer pattern; generalize it so agent output arrives
   pre-reviewed, shrinking BR's queue.
2. **Batch for review:** fewer, larger, consolidated review units (not a stream of small ones).
3. **Agent keeps the AFK menu stocked** ([[keep-afk-menu-stocked]]): the agent proposes vetted high-value work so
   BR does not have to *find* it — directly attacks bottleneck (a).
4. **Reframe the gauge from "tokens remaining" to "value delivered / spend"** — the dashboard's primary KPI should
   be a value-per-spend signal + a **loose-ends/review-queue depth** meter (BR's actual load), not raw burn.

## Meta (047, the study eating itself)
BR's stress is itself longitudinal case-study data: the *human* half of the joint system experiencing an affective
load from the collaboration economics — the mirror of the agent-rot datapoints. [[agent-affective-analogs]] now
has a human-affect instance grounded in a real stressor.

## Addendum - the complementary strength (BR reflection, 2026-07-08)
BR: *"the STRENGTH of CO4 is to keep MANY threads in mind, and the habit to pin them makes CO4 not forget despite
increasing rot."* This is the exact complement to the bottleneck above: the human CANNOT hold all threads (the
scarce capacity), but the agent CAN, and the **pinning discipline converts that thread-holding into a DURABLE
shared asset that survives the agent's own rot.** So the division of cognitive labor is: agent holds + pins the
breadth; human supplies direction + judgement on the few threads that matter. The pinning substrate (PB + memory +
research notes) is the *mechanism* that makes the fast agent's breadth a shared asset rather than a loose-end
generator - and it works *because* it is external to the rotting context (the same external-reference-frame logic
as the RT051 grounding and the contextRotMeter L1 canary file). Positive datapoint: the method is doing its job.

## Disposition
Pinned here (WR data) per BR. Graduated to **RT052** (the umbrella research topic) — see
`research/052-human-throughput-and-value-aligned-spend.md`.

## Timestamp retrofit (2026-07-10, SM044a-S6) — evidence-time recovered from the transcript
Retrofitted via `tt wr stamp` on the muntabot project dir (append-only; the day-dated claims above stand):
- **The brain-dump** (§"The brain-dump", the "pocket money" / "productive = external value" utterance) was
  enqueued at **2026-07-08T11:19:00.848Z = 13:19:00 CEST** (session `240e00c3`, the earliest datable form is the
  `queue-operation` enqueue line; the processed `type:user` entry follows ~2 min later at 11:20:51Z).
- **The addendum** (§"Addendum", *"the STRENGTH of CO4 is to keep MANY threads in mind…"*) was enqueued at
  **2026-07-08T11:21:08.858Z = 13:21:08 CEST** — **~2 min 8 s after** the main brain-dump, one continuous burst.
- Method nuance (a wr-stamp finding): the *earliest* datable form of a human utterance can be a `queue-operation` /
  `attachment` entry (the send moment), which precedes the `type:user` processing entry and is what `--user`/`--human`
  would skip — so the retrofit rule "earliest hit across all entry types" (Mode.All), not just `--human`, gives the
  truest send time. Anchors this RT052 motivation statement on the session timeline (mid-morning UTC, the token-reset
  topic it is literally about). Ties: [[wr-stamp-dogfood-transcript-format-2026-07-10]], [[raw-data-append-only]].
