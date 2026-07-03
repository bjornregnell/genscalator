# Why genscalator?

> **Status: initialized 2026-07-03 (outline).** Slot reserved for the project's foundational "why" — the intro
> a newcomer reads first. Outline below; to be drafted.
> **Audience:** newcomers to genscalator; developers building or using coding agents; anyone worn down by
> brittle agent shell usage or confirmation fatigue; teams weighing typed, safe-by-design tooling for
> AI-assisted development.
> Sources: `docs/foundations.md`,
> `research/METHODOLOGY.md`, `research/instrumentation-by-default.md`,
> `research/confirmation-guard-static-analysis.md`, `research/wr-data/`.

## The one-line answer
Coding agents default to **dynamic shell** — re-emitting brittle `bash` every turn — and that quiet default
costs tokens, safety, and reliability. genscalator replaces it with **typed, safe-by-design tools** the agent
calls instead. *(to draft)*

## Outline

### 1. The problem — the dynamic-shell default
- Agents reflexively reach for `grep`/`awk`/`sed`/`cd && … > log`; each call is re-emitted, re-read, brittle.
- Costs: wasted tokens (TE), context bloat that pushes toward the **dumb zone** (context rot), and
  **confirmation fatigue** — blanket allowlists like `curl *` / `git *` that can't be *proven* safe.

### 2. The insight — the agent is the tool-user
- Design tools *for the agent as the primary user*, not for a human at a terminal: one literal, typed,
  statically-analyzable command per action.

### 3. The thesis — safe-by-design → fewer confirmations, honestly
- A tool that **declares its effects** and is **statically analyzable** can be *proven* safe by the
  confirmation guard, so it runs without a prompt. We cut CF **without** the unsafe "always allow" a fatigued
  human reaches for.

### 4. Token efficiency as liveness, not tidiness
- A committed, compiled tool beats re-emitting bash every time; self-pacing keeps the working context in the
  **smart zone**. TE stops being a nicety and becomes halt-avoidance.

### 5. Safe-by-design in practice
- Replace dual-use binaries (`curl`, raw `grep`) with narrow typed tools (`tt web get`, `tt text`, `tt forge`);
  trust boundaries come only from **human-set env**, never agent-nameable flags.

### 6. The bigger frame — human↔agent collaboration + research
- Instrumentation-by-default (the agent *Reads* a gauge instead of guessing); the dances (compact /
  consolidation); the whole effort is Action Research + Design Science on a real build — the artifacts *are*
  the contribution.

## Close
genscalator = the toolbox (`tt`) + skills + docs distilled from doing real work this way. *(to draft)*
