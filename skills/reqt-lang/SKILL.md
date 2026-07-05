---
name: reqt-lang
description: How to read and write reqT-lang requirements — the ENT/REL/ATTR markdown-subset used in genscalator's PRD.md and any reqT model. Trigger whenever editing PRD.md, authoring/querying a reqT-lang model, or reasoning about requirement structure or traceability. STUB — populate per the plan below before relying on it for anything beyond the core rule.
---

# reqT-lang skill (STUB — to be populated)

reqT-lang is the **parseable requirements markdown-subset** genscalator uses for its PRD (ENT = entities, REL = relations, ATTR = attributes). The agent authors AND queries it; the vendored parser (`tools/reqt-vendored/`, run via `tt parsereqt`) is the ground truth.

## Core rule (verified): parse, don't guess
When (re)structuring reqT-lang, confirm with the REAL parser instead of trusting a mental model of the grammar:
- `tt parsereqt parse FILE` → prints the `Model(...)` so you can see how entities/relations/attributes actually nested.
- `tt parsereqt lint FILE` → flags unknown-concept fall-throughs (typos, un-mapped terms).

This is not optional caution — it caught a real, silent bug: **a relation listed under a `has` block (e.g. `* requires: Feature: X`) does NOT become a relation — the parser turns it into inert `StrAttr(Text, ...)` and the relation is lost.** Write a relation as its own top-level clause: an `ENT REL` line whose indented children are the target(s). A relation binds ONE OR MORE sub-elements via indentation — exactly like `has` binds many attrs/entities (verified: `* Feature: cfg requires` over indented `* Feature: dep1` + `* Feature: dep2` → `Requires,Model(Ent(Feature,dep1),Ent(Feature,dep2))`). The `* Feature: A requires Feature: B` inline form is just the single-target shorthand. `lint` does NOT catch the under-`has` bug (its concept check is `^[A-Z]`; relation keywords are lowercase).

## To populate (PLAN — not yet done)
1. **Move the syntax rules** now parked in `PRD.md` META ("reqT-lang syntax rules the agent MUST follow — empirically verified via `tt parsereqt`") into this skill; leave the PRD pointing here.
2. **The META vocabulary** from `PRD.md`: the minimal ENT/REL/ATTR subset (Goal/Feature/Function/Stakeholder; has/requires/helps/hurts/verifies; Spec/Gist/Why/Prio/Comment/Idea) and the **MAP-not-FORK** mapping to reqT's KAOS/i*/GRL vocabulary (anti-goal → Goal-we-Hurt; mitigates → Hurts; verifies → Verifies; Rationale → Why; Metric → Target/Quality).
3. **The reqT-lang metamodel** from `tools/reqt-vendored/02-meta-model.scala` — the authoritative ENT/REL/ATTR definitions and their informal-semantics strings (e.g. `Idea` = "A concept or thought, potentially interesting").
4. **Worked examples** (short vs multi-line relation forms; `has` nesting; inline `Ent has Attr`) each paired with its verified `Model(...)` output.
5. **The strict/lint gap** (lowercase relation-under-`has` slips past `lint`) → reqT/reqT-lang#15; see `research/015-reqt-lang-review.md`.

Related: [`../scala-style/SKILL.md`](../scala-style/SKILL.md) (how to WRITE a tool), [`../../PRD.md`](../../PRD.md) (the live model + META), the `tt parsereqt` tool.
