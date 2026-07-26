# Task-autonomy negotiation — ralph-loop vs collaborative ballgame

- **Question:** How should human + agent decide, **per task**, where it sits on the autonomy spectrum — from
  a hand-it-over **ralph-loop** (little/no human check) to an active side-by-side **ballgame** (human in every
  volley)? Can this triage be made **explicit and repeatable** (the agent proposes a mode, the human
  confirms/overrides) instead of ad hoc — and what signals decide it?
- **Why it matters:** **human attention is the scarcest resource in the loop.** Mis-triage is costly in both
  directions: treating a high-stakes task as a ralph-loop risks unreviewed bad outcomes (and hands a BHH room
  to advance a BadGoal unsupervised); treating a low-stakes, reversible, self-verifiable task as a ballgame
  **wastes the human's attention and manufactures confirmation fatigue.** Getting the mode right per task is a
  first-class efficiency + safety lever — the *scheduling* layer above the individual safe-by-design tools.
- **Status:** open (spectrum + triage signals drafted; the explicit-negotiation protocol is the new idea).

## The spectrum
```
ralph-loop  ┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄  ballgame
hand over + checkpoint + walk away      turn-by-turn human volleys
(autonomous)        (most real tasks: checkpointed          (collaborative)
                     autonomy with human gates at
                     the few genuine decision points)
```
Most work is **not** at either pole — it's *checkpointed autonomy*: the agent runs a lane unattended but
**stops at the few points that need a human** (a naming choice, an irreversible step, an ambiguous spec). The
craft is locating those points so the human is pulled in **only** there.

## Triage signals — what pushes a task toward autonomous vs collaborative
| Signal | → autonomous (ralph-loop) | → collaborative (ballgame) |
|---|---|---|
| **Verifiability** (the decisive one) | agent can objectively self-check **green** (compiles, tests pass, build stays green, leak% drops) | success hinges on taste/quality only a human can judge |
| **Stakes / blast radius / reversibility** | low-stakes, narrow, easily reverted | high-stakes, wide blast radius, hard to undo |
| **Human-judgment need** | none — mechanical, well-specified | naming, design trade-offs, domain knowledge, pedagogy |
| **Safe-by-design** | a BHH running it unsupervised **cannot** advance a BadGoal | unsupervised run could be weaponized |
| **Novelty / uncertainty** | well-trodden, established vein | exploratory, ambiguous, first-of-kind |

The first row dominates: **a task the agent can objectively verify needs little human attention regardless of
size.** Verifiability is what converts "big" into "still a ralph-loop." (This is why instrumentation — a
build-green check, a leak% metric — *expands the autonomous lane*: it manufactures verifiability.)

## The negotiation is itself a human↔agent protocol
Who decides the mode? The efficient pattern: **the agent triages and proposes** ("X is low-stakes +
self-verifiable → I'll ralph-loop it and checkpoint; Y needs your taste → let's ballgame"), the **human
confirms or overrides.** This is a communication act (cf. `002-communication-bandwidth.md`) and a CF-reducer: the
human spends one cheap decision ("yes, autonomous") instead of N per-step approvals.
- **Worked example (this very session, 2026-06-30):** BR said *"pick the one that needs least of my attention
  for now — I'm reviewing genscalator."* That is an explicit triage delegation: reserve human attention for
  the high-judgment task (reviewing genscalator research — taste/naming/correctness) and hand the agent the
  self-verifiable lane. The agent's job is then to **pick the genuinely ralph-loopable task** and not quietly
  drag the human into a ballgame.
- **Worked example (AT plan mapped onto the spectrum):**
  - *Appendix/solutions prose-leak grind* → **ralph-loop lane**: reversible, build-green-gated + leak%-metered
    (high verifiability), no naming calls. Agent runs it, checkpoints each batch.
  - *B0 automated code-scaffold pass* → **ballgame**: touches shared source broadly, design decisions, wants
    human review before shipping.
  - *Kojo / code-identifier translation* → **human-input-gated**: blocked on BR's glossary (domain naming);
    cannot proceed autonomously without advancing a quality risk.

## Open directions
- A lightweight **per-task autonomy rating** the agent self-computes (verifiability × stakes × judgment-need)
  and surfaces with its proposed mode.
- A **handoff/checkpoint protocol** for the autonomous lane (what to checkpoint, when to pull the human back,
  how to report on return) — pairs with the **compact dance** for long unattended runs.
- **Measure** human-attention spend per task class — does explicit triage cut it without raising error rate?
- **Mode drift:** a ralph-loop that hits an unforeseen decision point must *escalate to ballgame*, not guess.
  How does the agent detect "this just became a judgment call"?

## Relationships
- **Ralph loop** (glossary) is the autonomous pole; this note generalizes it into a *spectrum + triage*.
- **Confirmation fatigue / review overload** — mis-triage toward ballgame is a CF source; toward ralph-loop is
  a review-gap source. **Safe by design** is what makes the ralph-loop pole *safe to choose*.
- **Smart-zone / compact dance** — a long unattended ralph-loop drifts into the dumb zone, so autonomy needs
  context hygiene, not just safe ops (the `ralph loop` glossary caveat).
- **Instruction-adherence-decay** — autonomy assumes the agent won't regress to unsafe reflexes while
  unsupervised; the structural fixes (tool+allowlist+hook) are what make walking away tolerable.

## What shipped
- Nothing yet — note opened 2026-06-30, prompted by BR framing the choice between a low-attention AT task and
  a high-judgment genscalator review as exactly this kind of negotiation. **Candidate to graduate** into an
  AGENTS.md guideline ("triage each task's mode; propose, don't assume") + a HUMANS.md handoff convention.
