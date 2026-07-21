---
name: reqt-lang
description: How to read and write reqT-lang requirements — the ENT/REL/ATTR markdown-subset used in genscalator's PRD.md and any reqT model. Trigger whenever editing PRD.md, authoring/querying a reqT-lang model, or reasoning about requirement structure or traceability. Author from goal-level down (Stakeholder has Goal; Feature helps/hurts Goal; Target verifies Goal) and ALWAYS confirm structure with `tt parsereqt parse` — never trust a mental model of the grammar.
allowed-tools: Bash(tt parsereqt *)
---

# reqt-lang skill

reqT-lang is the **parseable requirements markdown-subset** genscalator uses for its PRD and any reqT model:
**ENT** = entities, **REL** = relations, **ATTR** = attributes. The agent authors AND queries it; the vendored
parser (`tools/reqt-vendored/`, run via `tt parsereqt`) is the ground truth. The point is *dogfooding
genscalator's own thesis* — migrate insight into **structure the agent can rely on, not prose**: a reqT-lang
model is diffable, queryable, and traceable (`Feature requires Feature`, `Target verifies Goal`) where a prose
PRD is not.

This practice has a name worth using: **agentic requirements engineering** (agentic RE) — the requirements slice
of *agentic software engineering* (agentic SE). See `docs/foundations.md` for the definitions; this skill is how
the agent actually *does* agentic RE on `PRD.md`.

## 1. Core rule: parse, don't guess
When (re)structuring reqT-lang, confirm with the REAL parser instead of trusting a mental model of the grammar:
- `tt parsereqt parse FILE` → prints the `Model(...)` so you see how entities/relations/attributes actually
  nested.
- `tt parsereqt lint FILE` → flags unknown-concept fall-throughs (typos, un-mapped terms).

This is not optional caution — it catches a **silent** bug: a relation listed under a `has` block (e.g.
`* requires: Feature: X`) does NOT become a relation; the parser turns it into inert `StrAttr(Text, ...)` and
the relation is lost. Method, always: **draft → `tt parsereqt parse` → read the `Model(...)` → fix → re-parse.**

## 2. Vocabulary — the subset we use (MAP not FORK)
reqT already has KAOS/i*/GRL semantics for almost everything we need, so we **use reqT's existing concepts**
rather than invent (agent review `research/015-reqt-lang-review.md`, BR 2026-07-01). Identifiers are
**camelCase**. The concepts below are the authoritative names from `tools/reqt-vendored/02-meta-model.scala`.

**ENT (entities)** — the ones we actually use: `Stakeholder`, `Goal`, `Feature`, `Function`, `Component`,
`Target`, `Risk`, `Term`. (Full set also has `Event`/`State`/`Task`/`UseCase`/`Story` etc.; add on need.)

**REL (relations, all 14 verbatim):** `is`, `interactsWith`, `excludes`, `implements`, `precedes`,
`requires`, `verifies`, `deprecates`, `has`, `impacts`, `relatesTo`, `helps`, `hurts`, `binds`. The ones we
lean on: `has` (containment), `requires` (dependency), `verifies` (a Target/Test gives evidence for a Goal),
`helps` / `hurts` (positive / negative goal influence), `precedes` (ordering).

**ATTR (attributes):** string-valued — `Gist`, `Spec`, `Why`, `Comment`, `Example`, `Constraints`, `Title`,
`Location`, `Input`, `Output`, `Deprecated`, `Text`; int-valued — `Prio`, `Order`, `Min`, `Max`, `Value`,
`Cost`, `Benefit`, `Damage`, `Probability`, `Frequency`. (`Text` is the fall-through bucket — see §5.)

**The MAP-not-FORK decisions (do NOT invent new concepts for these):**
- **anti-goal / "BadGoal"** → a **`Goal` owned by an adversarial `Stakeholder` (e.g. `bhh`) that our Features
  `hurt`** — model as *Goal-we-Hurt*, NOT a new `BadGoal`/`Barrier` concept (BR 2026-07-01).
- **mitigates / conflictsWith** → **`hurts`**; positive contribution → **`helps`**.
- **Rationale** → **`Why`**; **Metric** → **`Target`** (+ `Min`/`Max`/`Value`); **verifies** → **`verifies`**.
- product nouns **Tool / Skill** → **`Component`** / **`Function`** for now.
- lifecycle **Status** → encoded by the FUTURE/PAST headings (or `Deprecated`), no new attr.

## 3. Syntax rules (empirically verified via `tt parsereqt`)
1. **Relations bind entities — NEVER list a relation under `has`.** `* requires: Feature: X` inside a
   `Feature: Y has` block becomes inert `StrAttr(Text, "requires: Feature: X")` and the relation is SILENTLY
   LOST (and because `requires`/`verifies`/`helps` are lowercase, `lint`'s `^[A-Z]` check does not flag it).
2. **Write a relation as its own top-level `ENT REL` clause** whose indented children are the target(s) — a
   relation binds ONE OR MORE sub-elements, exactly the way `has` binds many. Single-target shorthand:
   `* Feature: a requires Feature: b`. Multi-target: `* Feature: a requires` with indented `* Feature: b` +
   `* Feature: c` children (BOTH bound).
3. **`has` holds the owner's ATTRs and sub-ENTs.** Under `* Feature: F has`, indented `Gist`/`Spec`/`Comment`
   bullets attach to F; a sub-entity may nest its own inline `has` (`Idea: k has Gist: g`).
4. **An unknown or typo'd leading `Word:` falls through to `Text`.** `lint` catches only CAPITALIZED ones
   (`Feautre:`, `BadGoal:`); a lowercase mis-relation slips through silently → verify by PARSING, not
   eyeballing.
5. **When unsure, parse a snippet and read the `Model(...)`.** The parser is ground truth.

## 4. Worked example — goal-level, VERIFIED against the parser
Source:
```
* Stakeholder: human has
  * Goal: avoidReviewOverload
* Goal: avoidReviewOverload has
  * Spec: The human reviews few, meaningful things.
  * Why: attention is the scarce resource.
* Feature: liveDashboard helps Goal: avoidReviewOverload
* Feature: liveDashboard requires
  * Feature: serv
  * Feature: harnessTap
* Target: dataLeavesUserControl verifies Goal: dataSovereignty
```
`tt parsereqt parse` →
```
Model(
  Rel(Ent(Stakeholder,"human"),Has,Model(Ent(Goal,"avoidReviewOverload"))),
  Rel(Ent(Goal,"avoidReviewOverload"),Has,Model(StrAttr(Spec,"..."),StrAttr(Why,"..."))),
  Rel(Ent(Feature,"liveDashboard"),Helps,Model(Ent(Goal,"avoidReviewOverload"))),
  Rel(Ent(Feature,"liveDashboard"),Requires,Model(Ent(Feature,"serv"),Ent(Feature,"harnessTap"))),
  Rel(Ent(Target,"dataLeavesUserControl"),Verifies,Model(Ent(Goal,"dataSovereignty"))))
```
Note the multi-target `Requires` binds BOTH `serv` and `harnessTap`, and `Target verifies Goal` is a
first-class relation — exactly the traceability a prose PRD cannot give.

## 5. Author from goal-level DOWN (the productive pattern)
Good reqT-lang starts at intent and descends to mechanism, wiring traceability as it goes:
1. **Stakeholders own Goals:** `* Stakeholder: s has` → indented `* Goal: g` (one per line).
2. **Goals carry meaning:** `* Goal: g has` → `Spec` (what precisely), `Why` (rationale), and `Target: t`
   (the measurable) — then a separate `* Target: t verifies Goal: g` clause makes the metric checkable.
3. **Features realise goals AND declare influence:** `* Feature: f helps Goal: g` (positive) and, for the
   threat model, `* Feature: f hurts Goal: badGoal` (an adversarial stakeholder's goal — the anti-goal, §2).
4. **Order + dependency between Features:** `requires` (needs another) and `precedes` (before another) as
   top-level `ENT REL` clauses, single- or multi-target (§3 rule 2).
This descent — Stakeholder→Goal→(Spec/Why/Target)→Feature→(helps/hurts/requires) — is how the PRD's stable
Goals connect to concrete releases, and the shape SM022's live-dashboard section should follow.

## 6. The strict/lint gap (known)
`lint`'s concept check is `^[A-Z]`, so it flags Capitalized fall-throughs (typos, un-mapped `BadGoal:`) but
NOT a lowercase relation mistakenly nested under `has` (rule 1's bug). Our `tt parsereqt` lint is a **wrapper**
that surfaces both kinds without forking the vendored parser; a native in-parser strict mode is the upstream
ask (reqT/reqT-lang#15; `research/015-reqt-lang-review.md`). Also known: `Assumption` is NOT a valid reqT
EntType (it falls through) — use `Constraints` or a `Comment`.

Related: [`../scala-style/SKILL.md`](../scala-style/SKILL.md) (how to WRITE a tool), [`../../reqts/PRD.md`](../../reqts/PRD.md)
(the live model + META), `tools/reqt-vendored/02-meta-model.scala` (the authoritative concept list with
informal-semantics strings), the `tt parsereqt` tool.
