# Do researchers escalate by (fierce) competition?

> **Status: drafted 2026-07-10.** **Author: Björn Regnell.** A study idea: pit agent "researchers" in competition
> against collaboration on a realistic task - writing research funding applications - and see which mode produces
> the better proposals.
> **Audience:** researchers, research-policy folk, and anyone curious whether competition or collaboration gets
> better science, tested with a reproducible agent fleet. No prior context assumed.

<!-- AGENT-DRAFT STUB (2026-07-10): captures BR's brainstormed study idea; scaffold + TODO for BR to develop. Em-dashes kept out. -->

[scaffold: the question. Research culture leans hard on competition - for grants, for priority, for prestige.
Does fierce competition actually escalate researchers to better work, or does collaboration do better? This post
proposes a way to probe it with an agent fleet, where both modes run under identical conditions and can be
compared.]

## The study idea

TODO: draft the meat of this study design.

- **The task.** A fleet run where agents generate realistic **research funding applications** into ASE (agentic
  software engineering).
- **The review.** The applications are assessed by a real human **retired funding-application reviewer** (BR knows
  a couple from the Swedish agencies **VR** and **VINNOVA**), who anonymously scores them along two axes:
  **VR-biased "research excellence"** and **VINNOVA-biased "innovation / commercialisation opportunity."**
- **THE TWIST (the independent variable).** Two social modes:
  - **Competition mode** - agents work in **isolation**: keep your proposal secret, try to beat the others.
  - **Collaboration mode** - agents **share and peer-review** each other's drafts: we help each other, and it
    counts as a win if at least one of the group gets funded.
- **Reproducibility.** The whole generation is **idempotent and reproducible** (a seeded, re-runnable fleet
  harness), so the competition-vs-collaboration comparison is a controlled contrast, not an anecdote.
- **Honest scope.** The real human-review step may not be feasible to pull off; even if it falls through, the
  **generated data from the run is worth publishing** on its own - the proposals, the two modes, the process.

## Why this is interesting (to develop)
- It turns a research-culture question (competition vs collaboration) into a **cheap, reproducible experiment** you
  can actually run.
- It dogfoods the fleet + the ASE angle: agents writing ASE grant proposals, reviewed on real funding criteria.
- Even without the human reviewer, the run yields a novel **dataset**: agent-generated funding proposals under two
  social modes.

## TODO (BR revoice)
- Develop the design + metrics: what "escalate" means, how to score, how to blind.
- Line up the retired VR / VINNOVA reviewer(s); decide feasibility.
- Decide the publishable fallback if human review falls through (the generated corpus + the design).
