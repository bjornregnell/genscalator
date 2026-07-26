# RT048 — How different kinds of substrate content affect the agent's ability to use genscalator reliably (without regressing into "bash hell")

- **Status:** STUB (agent-drafted 2026-07-07, from BR's steer + the command-hygiene regression series + the
  guard-stall-invisibility finding). Needs BR steer on which mechanism to test first.
- **Author framing (BR, 2026-07-07):** *"steer you by the parts of substrate that has big power over you";*
  *"how different kind of substrate content affect your ability to use genscalator reliably without
  regressing into bash hell."*

## The research question

Which **substrate content**, and in which **placement/form**, most powerfully steers the agent toward the
disciplined genscalator `tt` tools (and the bare-allowlist-clean command set) and **away from raw-bash
habits** (`cd`, `tail`, `grep`-instead-of-`tt`, raw `curl`) that stall on guards, trip the untrusted-hooks
guard, or defeat the allowlist? Put sharply: **what substrate has "big power" over my enacted tool
behaviour, versus substrate I recall but do not enact?**

## Why this matters (the evidence base is already here)

The Go #1/#2 loop produced **five command-hygiene regressions** in a few days
(`wr-data/command-hygiene-regression-2026-07-06.md`, specimens 1-5) plus the **guard-stall-invisibility**
finding (`wr-data/guard-stall-invisible-to-agent-2026-07-07.md`). The pattern across them:
- **Recall does not equal enactment** — the relevant rule was often *in recalled memory* yet not enacted
  (the t=0 post-compact `cd`-git slip had `commit-via-tt-git-not-raw-cd-git` in context).
- **Willpower does not fix it** — discipline regresses under load (RT043), does not survive a warp, and
  (guard-stall) **cannot even correct a mistake the agent cannot perceive**.
- So the live question is **substrate design**: since neither recall nor willpower reliably steers tool
  behaviour, *which substrate content actually does?* This is the meta-level of 047 (does the agent's own
  substrate carry the agent's own discipline? So far: imperfectly) and the actionable sibling of the
  structure-over-willpower dogma.

## Sub-questions / hypotheses

1. **Structural >> persuasive substrate (H1).** Substrate that *blocks* a bad path (the allowlist denying a
   compound command; a `tt` tool being the *only* available path) should steer far more reliably than
   substrate that *asks* the agent to behave (a memory, a checklist). Prediction: guard-free-by-construction
   (structural) beats the anti-regression header (persuasive), and both beat bare recall.
2. **Salience/placement matters (H2).** A rule at the **top of the resume prompt, reloaded every warp**
   (the anti-regression header) should steer better than the same rule as a one-line memory-index entry.
   Testable: regression rate with vs without the header.
3. **Tool-coverage gaps force regressions (H3).** The agent regresses to raw bash partly where the
   disciplined path *does not exist* (no `tt tail`/`tt cat` for peeking a file → raw `tail`; no `tt web`
   file-output → raw `curl -o`). You cannot will-avoid a gap the tool does not cover. Prediction: closing
   the tt gaps (below) removes those regressions outright, independent of any persuasion.
4. **Prior-strength of the raw habit (H4).** `grep`/`tail`/`cd` are deeply trained defaults; the tt
   equivalents are thin substrate. Which raw habits are stickiest, and does salience or structure beat the
   prior?

## Candidate interventions (rank + prototype)

- **Close the tt gaps (H3, cheapest, highest-certainty):** add safe file-peek tools (`tt tail`/`tt head`/
  `tt cat` or fold into `tt text`) so background/log peeking never needs raw `tail`; a `tt time` clock
  (RT039); confirm every common bash need has a `tt`/allowlisted path. Make the disciplined path *always
  available* so the agent is never forced into bash hell.
- **The anti-regression checklist as high-salience steering substrate (H2):** already added to the top of
  the resume prompt; measure whether it lowers the regression rate.
- **Structural guard (H1):** an allowlist/hook that makes compound and un-covered commands *impossible*
  (blocks, not asks) — human-approved (agent not authorized to change the allowlist solo). RT013/RT016/
  RT021 are the guard-tooling siblings.

## Method (candidate)

Observational data already exists (the wr-data hygiene series = a natural regression log). A cleaner test:
vary the substrate configuration (header present/absent; a tt gap open/closed) across comparable
autonomous spans and measure the **regression rate** (guard-trips + raw-tool uses per N commands). Sibling
harness to the indent-vs-braces edit-cost experiment, but the DV is *tool-discipline reliability*, not edit
cost. Beware the reflexivity/observer effect: the agent knowing it is measured may itself change behaviour.

## Related
- Data: `wr-data/command-hygiene-regression-2026-07-06.md` (5 specimens),
  `wr-data/guard-stall-invisible-to-agent-2026-07-07.md`, `wr-data/broad-allowlist-aversion-2026-07-06.md`.
- Siblings: RT039 (wall-clock / `tt` clock), RT043 (guardrail adherence under load), RT003 (instruction
  surfaces precedence), RT013/016/021 (guard tooling), RT027 (steering-doc design tension).
- Themes: 047 (substrate carry, at the meta-level of the agent's own discipline), structure-over-willpower,
  guard-free-by-construction, [[genscalator-toolbox-single-dispatcher]], [[hardening-dance]].
