# RT049 — The dilemma of delegation (delegation economics in agent orchestration)

**Status:** open research topic, seeded 2026-07-07 (BR idea + the delegation dance, see
`wr-data/delegation-dance-and-dilemma-2026-07-07.md`). Sibling of RT048 (substrate-content-power over
tool-discipline), RT001 (context rot), and the indent-vs-braces edit-cost harness.

## The question
When a super-agent (CO4) can either **do a task itself** or **delegate it to a subagent** (Fable-5, run in the
background), which wins, and *why*? Delegation is not free: it trades briefing cost and loss of the supervisor's
global view against parallel throughput, super-agent responsiveness, and reduced context rot. This is the
**delegation dilemma** every senior engineer and every professor already lives (see the analogy below); the agent
case makes its variables unusually measurable.

## The ledger (what the decision actually trades)
**Benefits of delegating**
- **Responsiveness:** the super-agent's turn stays free to talk with the human (no long silent stretches, no
  message-race / input-focus theft). [[no-interrupting-modals-during-flow]]
- **Parallel throughput:** N subagents eat tokens at once (spending-mode friendly). [[token-budget-modes]]
- **Context-rot reduction, both sides:** the subagent's work never enters the super-agent's context (CO4 stays
  lean, warm window lasts longer, fewer warps), and each subagent starts fresh (no accumulated rot).

**Costs / risks of delegating**
- **Briefing overhead:** tokens + effort to fill the subagent's substrate. Cheap under MAX5 / spending-mode;
  binding only under a tight budget.
- **Briefing-fidelity bottleneck (the real risk):** the super-agent may pin the subagent's substrate worse than
  its own global understanding, so the subagent underperforms what CO4 would produce. This is RT048's
  substrate-power question aimed at a subagent instead of the tool-allowlist.
- **Adjudication tax:** the super-agent must still review/verify the subagent's output (the ralph-loop discipline)
  — cheap if the work is verifiable, expensive if judging it needs the same global context delegating was meant to
  save.

## Research questions
- **RQ1 (the crossover):** for which task classes does delegation beat solo, as a function of briefing cost,
  verifiability, and how much global context the task needs? Predict a crossover: highly-scoped, verifiable,
  context-light tasks favor delegation; global-context-heavy synthesis favors solo. The crossover also **moves
  with the budget regime** (agent introspection, BR-flagged 2026-07-07): under **token-spending mode, rot is the
  binding currency, not tokens**, so delegation wins even when the supervisor already holds the task in context
  (briefing tokens are free, while keeping the supervisor's context lean is the real prize). [[token-budget-modes]]
- **RQ2 (briefing fidelity → RT048):** can the super-agent transfer enough substrate for the subagent to match
  its own quality? What briefing content actually carries the power (the RT048 question, subagent variant)?
- **RQ3 (the rot ledger):** does delegation net-*reduce* context rot across the system, and by how much, once the
  briefing-token overhead and the adjudication re-read are counted?
- **RQ4 (the human analogy, testable):** does the professor/PhD delegation dilemma transfer quantitatively? Do the
  same predictors (transfer cost, task decomposability, supervisor global-context dependence) govern both?

## The analogy (grounding)
A professor often finds it **harder to delegate and explain a task to a PhD student than to do it himself.** The
mentoring overhead (transferring context, the supervisor's irreplaceable global view, the review burden) can
exceed the benefit of the extra hands, especially for a first-of-its-kind or synthesis task. The agent case is a
clean, instrumented model of this: briefing = the explaining, the subagent's fresh context = the student's
un-rotted-but-uninformed start, adjudication = the review. Whatever we learn about one informs the other.
Related human-side framing: BR's own supervision experience; the software-engineering delegation/mentoring
literature (to be cited read-before-cite if the blog leans on it).

## Possible measurement (reuse existing machinery)
- **Matched-task quality:** run the same well-specified task CO4-solo vs CO4-delegated-to-CF5; blind-score the
  outputs (the 047 style-rater discipline). Vary briefing richness to probe RQ2.
- **Briefing-token cost** and **human wait-time** (responsiveness) as the two axes of the ledger.
- The **indent-vs-braces edit-cost harness** ([[genscalator-indent-braces-experiment]]) is a natural vehicle:
  it already measures per-task agent cost under controlled variation.
- **Reflexive data source:** this very session's subagent runs (the 4 blog reviewers, the WR synthesis, the AT
  grind) are specimens — which produced quality matching CO4-solo, and which needed re-work?

## Open
- Is there a stable "delegate if ..." rule, or is it irreducibly a per-task judgment (the meta-finding that CO4
  authored the delegation *pinning* itself because it was context-heavy)?
- Interaction with model tier: does the crossover move when the subagent is Fable-5 vs Opus vs a tiny local model?
  (Ties Factor A of 047.) **Practical rule (BR 2026-07-07): model choice IS part of the delegation decision.** In
  **token-spending mode, default to the smartest / top-tier sub-agent** (quality first, cost moot; Fable-5 is the
  sweet spot, top-tier plus a free bucket); in saving-mode, match the model to the task (mechanical work to a cheaper
  tier). So a "delegate?" call always carries a "to which model?" answer. [[token-budget-modes]]
- The empirical test of the rot-reduction bonus (RQ3) is spun out as its own study design: **RT050**
  (`research/050-does-delegation-lower-context-rot.md`).
- Feeds blog **BP013** (`blog/013-the-dilemma-of-delegation.md`).
