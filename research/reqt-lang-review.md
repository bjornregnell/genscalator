# reqT-lang review — parser + how genscalator should use the meta-model

Agent review (2026-07-01) of the reqT-lang parser and meta-model, requested by BR (reqT BDFL). Clone reviewed:
`/home/bjornr/git/hub/reqT/reqT-lang` (`src/main/scala/05-MarkdownParser.scala`, `02-meta-model.scala`,
`03-model-GENERATED.scala`). Proposed improvements may be committed upstream to https://github.com/reqT/reqT-lang.

## Headline: MAP, don't FORK

My first-pass PRD "proposed extensions" (BadGoal, Metric, mitigates, conflictsWith, verifies, refines, Rationale…)
were largely **reinventing concepts reqT already has** — and reqT's are the more standard RE (KAOS / i* / GRL)
semantics. We should express genscalator's requirements in reqT's **existing** vocabulary, which keeps the subset
small, avoids a fork, and inherits mature meaning. Mapping (mine → reqT existing, with reqT's own doc-string):

| I proposed | reqT existing | reqT's meaning (`02-meta-model.scala`) |
|---|---|---|
| `mitigates` | **`Hurts`** (GoalRel) | "Negative influence. A goal hinders another goal." → `Feature hurts Goal:controlHumanSystem` |
| (positive contribution) | **`Helps`** (GoalRel) | "Positive influence. A goal supports the fulfillment of another goal." |
| `conflictsWith` | **`Hurts`** (mutual) / `Excludes` | as above |
| `verifies` | **`Verifies`** ✅ | "Gives evidence of correctness. A test verifies the implementation of a feature." |
| `Rationale` | **`Why`** ✅ (StrAttr) | the rationale attribute |
| `Metric` | **`Target`** / **`Quality`** (+ `Min`/`Max`/`Value` IntAttrs, the Quper model) | Target = "A desired quality level or quality goal." |
| `BadGoal` | **`Barrier`** OR a `Goal` owned by an adversarial stakeholder | Barrier = "Something that makes it difficult to achieve a goal." |
| `Risk`, `Term`, `Gist`, `Example`, `Spec`, `Prio` | **all already exist** ✅ | — |

**Key modeling insight:** we don't need a `BadGoal` concept at all. A BHH "bad goal" is just a **`Goal` owned by the
`bhh` stakeholder that our Features `Hurt`** — clean i*/GRL anti-goal modeling with zero new concepts:
```
* Stakeholder: bhh has Goal: controlHumanSystem
* Feature: safeByDesignTooling hurts Goal: controlHumanSystem
* Feature: safeByDesignTooling verifies Goal: safeGeneration   (or a Target/Quality verifies it)
```
So the safe-by-design traceability I wanted lands entirely in existing reqT relations (`hurts`, `verifies`).

## Genuinely missing (worth a real decision, not auto-add)
- **Explicit anti-goal / adversary-goal ENT.** reqT has `Barrier` (obstacle) but no first-class "attacker's goal."
  Two clean options, both concept-free: (a) model as `Barrier`; (b) model as a `Goal` under the adversary
  stakeholder that we `Hurt`. I lean (b). Only if BR wants the adversarial framing to be *visually* first-class
  is an `Antigoal`/`BadGoal` EntType worth adding upstream. **RE-modeling judgment call for BR (the expert).**
- **genscalator product entities** (`Tool`, `Capability`, `Skill`). reqT has `Component`, `Module`, `Service`,
  `Function`, `Feature`. A tt tool ≈ `Component`/`Function`; a skill ≈ `Function`/`Component`. Either map to those
  or, since these are genscalator-domain nouns, propose a small domain vocabulary upstream. Map first; add only if
  the mapping loses meaning.
- **`Status`** — no direct attr; the FUTURE/PAST headings already encode lifecycle (and `Deprecated` exists). No
  new concept needed.

## Parser review (`05-MarkdownParser.scala`) — clean, with three improvement candidates
Strengths: dependency-free recursive-descent over indentation levels; nesting via `level(baseLevel)`; compact
(~200 lines); **graceful degradation** — unknown terms become `Text` (line 188) so authoring never crashes.

1. **Strict / lint mode (top recommendation, and the reqTParser enabler).** The graceful `case _ => Text` fallback
   means a **typo** (`Feautre: x has`) or an **un-mapped term** (`BadGoal: y`) *silently vanishes into a Text
   attribute* — data loss with no signal. For agent+human authoring of an 800-page-scale corpus that's dangerous.
   Propose an opt-in strict mode that WARNS on a capitalized-colon token that isn't a known concept: *"unknown
   concept 'BadGoal' at line N — did you mean Goal? (else it becomes Text)"*. This (a) surfaces typos, (b) is
   exactly the **divergence signal** when we feed `PRD.md` to the parser as ground truth, and (c) powers the
   `reqTParser` feature's validation. Same fail-fast-at-the-boundary principle as `tt-typed-args.md`.
2. **Source positions.** Elems carry no line numbers, so lint/editor errors can't point at a location. Optionally
   thread line indices through `parseLines` into a position on each Elem.
3. **Id handling (lines 124–150) is the intricate/fragile part.** It allows **spaces in ids** (`id = "$idStart
   $idExtra"`), plus empty-id and id-is-a-concept-name edge cases. This conflicts with the PRD's camelCase-id
   convention and could yield surprising ids (trailing space at line 148). Worth: property tests, and a decision —
   enforce single-token camelCase ids, or bless multi-word ids explicitly. Likely a subtle-bug locus.

## Recommendations
1. **Revise the PRD** to use reqT's existing vocabulary (`hurts`/`helps`/`verifies`/`why`/`target`/`quality`/
   `barrier`/`risk`/`term`); drop the invented `mitigates`/`conflictsWith`/`Metric`/`Rationale`/`BadGoal`; model
   anti-goals as adversary-owned Goals we `hurt`. Shrinks "proposed extensions" to ~one open question (explicit
   antigoal ENT?) + the Tool/Capability domain nouns. *(Needs the PRD handover back from BR.)*
2. **Upstream contribution to reqT-lang:** the **strict/lint mode** is the highest-value, lowest-risk parser
   improvement and directly serves the `reqTParser` feature. Prototype in the clone; PR if BR likes it.
3. **Baseline test:** feed this repo's `PRD.md` to the current parser; every element that lands as `Text` is
   either a term to map (per the table) or a real gap — the strict mode automates finding them.

## Status
Open (2026-07-01). What shipped: nothing yet (review note). Next: on BR's word, revise the PRD to the mapped
vocabulary and prototype the parser strict mode in the reqT-lang clone.
