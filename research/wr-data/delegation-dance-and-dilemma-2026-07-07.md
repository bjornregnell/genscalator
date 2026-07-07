# WR data — the delegation dance and the dilemma of delegation, 2026-07-07

**Event.** While CO4 was proposing to run solo tasks via subagents, BR got an idea: lean on **Fable-5 (CF5)
subagents more**, deliberately, so that the super-agent (CO4) **stays responsive to BR in chat** and the risk of
**message-race** drops, while parallel subagents **eat tokens in parallel** (aligned with token-spending mode).
BR named this a **dance** and asked what to call it. Working name (CO4 proposal, ratified by BR 2026-07-07): the
**delegation dance**. It joins the established dance family (compact, hardening, go, note, pin, exit-resume,
solo/AFK, ...).

## The dance
- **Mechanism:** the super-agent offloads bounded, well-scoped work units to CF5 subagents running in the
  background, and keeps its own turn free to talk with BR. When a subagent finishes, the super-agent adjudicates
  and commits.
- **Goal:** make super-agent CO4 **more responsive to BR** (no long silent stretches, no input-focus race) while
  useful work proceeds in parallel.

## Bonuses
1. **Responsiveness + race reduction** (the primary goal).
2. **Lower context rot in BOTH directions (BR, 2026-07-07).** Subagent work does **not fill the super-agent's
   context** (CO4 stays lean, its warm window lasts longer, fewer warps), AND each subagent gets a **fresh start**
   (no accumulated rot on its side). Delegation is a rot-management move, not only a throughput move. [[joint-rot-vigilance-recovery-kit]] [[propose-compact-dance-at-trigger]]

## Cost
- CO4 spends tokens **briefing the subagent** (filling its substrate with the relevant context). **Moot right
  now:** since BR's private MAX5 subscription we have not been able to eat the weekly limit, so in spending-mode
  the briefing overhead is effectively free. The cost matters only under a tight budget. [[token-budget-modes]]

## Risk (the real one)
- CO4 may **pin the subagent's substrate poorly** — brief it worse than CO4's own **global understanding** of the
  work — so the subagent does a **worse job than CO4 would itself**. Briefing fidelity is the bottleneck, and it
  is exactly the [[genscalator-prd-reqt-reengineering]]/RT048 question (which substrate content actually transfers
  power to the reader) applied to a subagent instead of the allowlist. [[cue-use-fleet]]

## The analogy (BR, flagged "VERY interesting")
- A **professor** often finds it **harder to delegate and explain a task to a PhD student than to just do it
  himself.** The classic **delegation dilemma / mentoring overhead**: the cost of transferring enough context,
  plus the loss of the supervisor's global view, can exceed the benefit of the extra pair of hands. This maps
  precisely onto CO4-and-subagents: **briefing cost + global-context loss vs parallel throughput + rot
  reduction.** The human-supervision literature and the agent-orchestration case illuminate each other.

## Meta (enacting the lesson)
- CO4 chose to **author this pinning itself, not via a subagent**, precisely because it needs the global context
  of this conversation (the analogy, the naming, the cross-refs). That is the dilemma's own verdict in miniature:
  **some tasks are cheaper done than delegated.** The dance is a judgment call per task, not a blanket policy.
  (BR flagged this sentence, 2026-07-07, as a valued **agent-introspection** specimen: the agent reaching, by
  introspection on its own choice not to delegate, the same verdict a tired professor reaches.
  [[echt-effort-especially-self-generated]])

## Actions
- Pinned as memory [[delegation-dance]] (the reusable protocol + proposed name, pending BR ratification).
- Research topic **RT049** created (`research/049-the-dilemma-of-delegation.md`): delegation economics in agent
  orchestration + the professor/PhD analogy.
- Research topic **RT050** created (`research/050-does-delegation-lower-context-rot.md`): an empirical study
  design to test the lower-context-rot hypothesis (BR's second addition).
- Blog stub **BP013** created (`blog/013-the-dilemma-of-delegation.md`).
- Cross-refs: RT048 (substrate-content-power), RT001 / blog 001 (context rot resembles fatigue), the
  indent-vs-braces edit-cost harness ([[genscalator-indent-braces-experiment]]) as a possible measurement vehicle.

## Introspection specimen (BR-flagged, 2026-07-07): rot is the currency under spending-mode
Agent introspection, triggered by BR's idea and tied to [[token-budget-modes]]: *"under spending-mode where rot is
the real currency, not tokens, delegation still wins."* Even when the supervisor **already holds the task in
context** (so "do it myself" looks cheapest on tokens), under **token-spending mode** the binding constraint is not
tokens but **context rot**, and delegation keeps the supervisor's context lean while a fresh subagent absorbs the
churn. So the delegate-vs-solo crossover (RT049 RQ1) **moves with the budget regime**: token-cheap-but-rot-expensive
tasks flip toward delegation in spending-mode. Live instance: the introprog AT-README em-dash sweep (2026-07-07) was
delegated on exactly this reasoning though CO4 had already read the whole file into context.

## Test specimen (BR probe, 2026-07-07): did the super-agent spot the delegation opportunity itself?
BR handed a task ("measure the Swedish % left in compendium-en.pdf") as a **covert probe**: would CO4, unprompted,
recognize it as a delegation candidate and reason about whether to delegate given the current mode? **Result (echt,
with the priming caveat):** CO4 DID flag it, tagging it as a delegation candidate and reasoning "scoped, verifiable,
build output would rot my context, we are in spending-mode" before delegating. **But** the honest calibration: in the
*same* message BR had just instructed CO4 to adopt the standing "indicate delegate-or-not under
delegation-dilemma-consideration" rule, so the recognition was **primed by that instruction in the same turn**, not
cold. What CO4 did do on its own: apply the reasoning correctly to *this specific task* without being told "delegate
this one." So: positive but primed. A cleaner future probe = a delegation candidate handed in a LATER turn with no
delegation-framing, to see if the flag fires cold. [[echt-effort-especially-self-generated]]
