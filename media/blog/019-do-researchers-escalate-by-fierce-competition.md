# Do researchers escalate by (fierce) competition?

> **Status: drafted 2026-07-10.** **Author: Björn Regnell.** A study idea: pit agent "researchers" in competition
> against collaboration on a realistic task - writing research funding applications - and see which mode produces
> the better proposals.
> **Audience:** researchers, research-policy folk, and anyone curious whether competition or collaboration gets
> better science, tested with a reproducible agent fleet. No prior context assumed.

<!-- AGENT-DRAFT (revoiced 2026-07-12): the framing prose is now drafted in BR's voice; the study design below is
BR's own material, lightly kept. Awaiting his final pass on the design + metrics. Em-dashes kept out. -->

Research culture runs on competition. We compete for grants, for priority, for who got there first, for prestige.
The assumption underneath all of it is that competition sharpens us, that the pressure of rivals escalates
researchers to better work. But is that actually true? Or would we do better science by helping each other more?
It is an old question, and normally an unanswerable one, because you cannot run the same research group twice under
two different social rules and compare the results. But with a fleet of agent "researchers" you can. Same task,
same conditions, two social modes, held side by side. Here is one way to probe it.

## The study idea

The shape is simple: generate the same kind of work under two social rules, then have a real expert judge the
output blind. Concretely -

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

## TODO (BR final pass + real-world calls)
- Framing prose is drafted (agent revoice); the design bullets are your own material kept intact - **read and
  make the whole thing yours.**
- **Yours to develop:** the design + metrics - what "escalate" means, how to score, how to blind.
- **Yours to line up:** the retired VR / VINNOVA reviewer(s); decide feasibility.
- **Yours to decide:** the publishable fallback if human review falls through (the generated corpus + the design).
