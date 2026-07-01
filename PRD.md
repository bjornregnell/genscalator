# Product Requirements Document (PRD) for genscalator

This document includes requirements for the genscalator product including all typed tools (TT or tt) and other **agent capabilities** — skills, plugins, and other artifacts (often in markdown or Scala code) that help **escalate human-agent code generation to the next level**. ("agent capabilities" is the proposed umbrella word for line-3's open question: a tt tool, a skill, and a plugin each *is-a* Capability; see the `Capability`/`Tool`/`Skill` entities below.)

*TAP:* To Agent Plan: refactor this whole repo so that things that belong here are moved here

## META

This document includes specs in reqT-lang, a markdown-subset for expressing requirements using meta-level concepts of ENT (requirement entities), REL (requirements relations), ATTR (requirements attributes).

### Why reqT-lang here (rationale — also a first-paper argument)

Using a **parseable requirements language** for genscalator is not incidental — it **dogfoods the project's own central claim**. genscalator's thesis is *migrate insight into structure the agent can rely on, not prose*. A prose PRD is precisely the fragile, unqueryable substrate we fight; a **reqT-lang PRD is a structured, diffable, agent-consultable model** — the same move applied to the project's own requirements. Concretely it buys three things a prose PRD cannot:

1. **Shared vocabulary** for human↔agent requirement-talk (ENT/REL/ATTR is small and memorable, like the WR-* codes / smart-zone terms — *vocabulary as infrastructure*).
2. **Traceability the agent can follow** — `Feature requires Feature`, `Goal has Spec` let an agent check completeness, find orphans, and order work deterministically instead of re-reading prose.
3. **"Safe by design" as checkable relations** — the threat model (below) stops being hand-wavy once `Feature mitigates BadGoal` and `Metric verifies Goal` are actual links.

The complexity risk ("too much machinery") is real **only** if we import the whole reqT meta-model or formalize every fleeting idea. Mitigations: keep the **minimal subset** (below); scope reqT-lang to **stable, cross-cutting** requirements while fleeting ideas stay in `research/` + `wr-data/`; and remember the **agent-payoff needs a parser** (the `reqTParser` feature) — until then this is "structured prose" (already useful for humans and as a spine, but the agent-leverage lands with the tool). Roles to prevent drift: **PRD = requirements spine · research/ = investigations · wr-data = evidence · memory = agent ops.**

*(WR / paper note: this rationale — reqT-lang adoption as dogfooding the structure-beats-prose thesis — is flagged as important input for the first research paper.)*

**Abstract example** of concrete syntax with abstract terms:

* ENT: id
* ENT: id REL
  * ENT: id
  * ATTR: text
  * ENT: id

**Concrete example:**

* Comment: A string attribute with informative text.
* Feature: yyy has Prio: 42
* Feature: yyy requires Feature: xxx
* Feature: xxx has
  * Prio: 12
  * Spec: A longer textual specification
      that is spanning several lines. A longer textual specification
      that is spanning several lines.
  * UseCase: zzz has
    * Prio: 23

**Convention:**

* **abbreviations** reqt and reqts is short for just requirement(s) and reqT-lang the language (and [reqT](https://reqt.github.io/) is a desktop tool not used here (yet), we stick with the language in md files for now)
* identifiers (id) are camelCase
* We only use this subset of reqT-lang (may be extended by agent or human as soon as we see fit). Core (BR):
  - ENT: Goal, Feature, Function, Stakeholder, ...
  - REL: has, requires, ...
  - ATTR: Spec, ...
* **Proposed extensions (agent, 2026-07-01 — for BR review; grounded in `research/`):**
  - ENT: **BadGoal** (a goal we do NOT want, e.g. of the BHH — one word per BR), **Capability** (umbrella) with **Tool** (a tt tool) and **Skill**, **Metric** (a measurable target), **Risk**, **Assumption**, **Constraint**, **Term** (glossary term).
  - REL: **mitigates** (`Feature mitigates BadGoal` — makes "safe by design" traceable), **conflictsWith** (`Goal conflictsWith BadGoal`), **verifies** (`Metric verifies Goal`), **refines** / **derivedFrom** (Goal → Feature → Function traceability), **owns** (`Stakeholder owns Goal`).
  - ATTR: **Gist** (one-line intent, BR-introduced), **Rationale** (the *why*), **Status** (open/shipped/cancelled — or model via the FUTURE/PAST headings), **Example**.

*TAP:* To Agent Plan: investigate what more entities, relations, attributes agent thinks we need from the reqT-lang meta model
  *(agent first pass done above under "Proposed extensions"; the highest-value adds are `mitigates`/`conflictsWith`/`verifies` because they turn the threat model + measurement research into traceability. Open for BR to prune/rename.)*

**reqT-lang language specification:**

* In prose: https://github.com/reqT/reqT-lang/blob/main/docs/langSpec-GENERATED.md

* In code:
  - Class hierarchy for abstract syntax tree (generated): https://github.com/reqT/reqT-lang/blob/main/src/main/scala/03-model-GENERATED.scala
  - Informal semantics (see strings): https://github.com/reqT/reqT-lang/blob/main/src/main/scala/02-meta-model.scala
  - Syntax: https://github.com/reqT/reqT-lang/blob/main/src/main/scala/05-MarkdownParser.scala


## Stakeholders and their goals (from `research/`: agent-is-the-tool-user + BHH threat model)

* Stakeholder: human has
  * Goal: avoidConfirmationFatigue
  * Goal: avoidReviewOverload
  * Goal: contributeOpenSource
* Stakeholder: agent has
  * Goal: tokenEfficiency
  * Goal: safeActionsRunWithoutConfirmation
* Stakeholder: bhh has
  * Comment: BHH = Black Hat Hacker; an ADVERSARIAL stakeholder — we design AGAINST these.
  * BadGoal: controlHumanSystem
  * BadGoal: exfiltrateSecrets

## General goals (stable over time)

* Goal: tokenEfficiency has
  * Spec: The agent accomplishes a task with the fewest tokens that preserves quality — cheap-but-clear over verbose, typed tools over shell scaffolding, and self-pacing to stay in the smart zone (context fill below the L ceiling).
  * Rationale: fewer tokens = lower cost AND better quality (a full context degrades into the dumb zone).
* Goal: safeGeneration has
  * Spec: Generated code and agent actions never advance a BHH BadGoal. Precisely: the human's no-CF/no-review-overload goals and the agent's convenience goals are met WITHOUT ever widening the BHH attack surface. A safe action should be provably safe (statically analyzable) so it needs no per-action confirmation.
  * Metric: confirmationsPerSession
  * Metric: guardTripsPerSession
* Goal: jointHumanAgentProductivity has
  * Spec: The human-agent pair produces more, and more reliable, software per unit of scarce resource (human attention, tokens, wall-clock) than either alone or than the out-of-the-box baseline. This is the down-to-earth backbone thesis.
  * Metric: tokensToGreen
  * Metric: brokenBuildIterations

## Safe-by-design as traceability (worked example of the proposed relations)

* Feature: safeByDesignTooling mitigates BadGoal: controlHumanSystem
* Feature: safeByDesignTooling has
  * Gist: tt tools that are statically analyzable + effect-declared, so trust is granted once, not per action.
  * Spec: Each tt tool is a literal, single, typed command that declares its effects (--sandboxed / --safe-mode / --audit). Because the confirmation-guard can PROVE such a command safe, it runs without a prompt — so cutting confirmation fatigue never forces a fatigued "always allow" that could serve a BHH BadGoal.
  * verifies: Metric: confirmationsPerSession
* Goal: avoidConfirmationFatigue conflictsWith BadGoal: controlHumanSystem
  * Comment: the #6305 hazard — a CF-driven "always allow" is a BadGoal being served by fatigue; safeByDesignTooling is how we resolve the conflict without a prompt.

## FUTURE

### Roadmap

### Release v0.1.0

(this will be the first release, all whats-to-come-in-this-release genscalator reqts in reqT-lang go here)

* Feature: reqTParser has
  * Gist: write a typed tool for parsing reqT-lang reqts
  * Spec: A `tt reqt` tool that parses the reqT-lang markdown subset (this PRD's ENT/REL/ATTR bullet grammar) into a typed model, validates it (well-formed ids in camelCase, known ENT/REL/ATTR or a clear "unknown term" error), and answers queries the agent needs: list goals/features, trace `requires`/`refines`, find orphans, and check every `BadGoal` is `mitigates`-linked by some `Feature`. Be inspired by / reuse the reqT-lang Scala parser (`05-MarkdownParser.scala`) rather than re-inventing the grammar; typed args per `research/tt-typed-args.md`; one-line friendly errors, never a stack trace.
  * Spec: **Baseline / ground truth** — feed THIS `PRD.md` to the *current* reqT-lang parser and treat the model it yields as the acceptance fixture. Where our subset diverges from what the parser accepts, resolve each divergence as EITHER a PRD fix OR a proposed improvement to the reqT-lang repo (see the review note below).
  * requires: Function: reqtLangGrammar
  * verifies: Assumption: prdParsesUnderReqTLang
* Comment: reqT-lang parser review — BR wants agent input on the parser at https://github.com/reqT/reqT-lang (clone: /home/bjornr/git/hub/reqT/reqT-lang); proposed improvements may be committed there. Agent review pending (queued after this PRD fold).

## PAST

Here are requirements that are either implemented or cancelled. Move requirements from FUTURE to PAST as the move on.

### IMPLEMENTED

### Release v0.0.1

(there will be no v0.0.1 this is just to show how headings are use)

### CANCELLED
