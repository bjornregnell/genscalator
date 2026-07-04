# genscalator blog

Public-facing drafts distilled from `research/` (research = evidence + working-out; blog = the narrative for an
outside reader). Posts are named `NNN-slug.md` — a zero-padded sequence number, **no dates in filenames** (a
post's date lives in its status banner, not its name).

## Posts
- [`000-why-genscalator.md`](000-why-genscalator.md) — *(initialized)* the project's foundational "why":
  dynamic-shell default → typed, safe-by-design tools.
- [`001-context-rot-resembles-human-fatigue.md`](001-context-rot-resembles-human-fatigue.md) — *(drafted)*
  the context-rot ↔ human-fatigue parallel + the mutual-help thesis.
- [`002-braceful-or-braceless-or-the-common-style.md`](002-braceful-or-braceless-or-the-common-style.md) —
  *(drafted)* Scala braces vs significant indentation vs a shared "common style", judged by agent edit-cost.
- [`003-bigger-common-style-experiment.md`](003-bigger-common-style-experiment.md) — *(drafted, results pending)*
  the confirmatory big-run of the edit-cost experiment (56 models); Results populate when the sweep + seeded
  analysis land.

### The 004→008 arc — one argument in four movements + a synthesis
Read in numeric order **004 → 005 → 006 → 007**, then **008**. The four descend a stack — **Pains** (004) →
**Practice** (005) → **Method** (006) → **Theory** (007): the **Theory-Method-Practice-Pains (TMPP)** chain read
bottom-up. Each post ends on a *backwards cliffhanger* — the thing you just felt has a deeper cause one post down —
so the numeric order stays a page-turner while the logical stack surfaces. **008** reads the stack top-down and ties
it together.
- [`004-why-claude-UX-sometimes-sucks.md`](004-why-claude-UX-sometimes-sucks.md) — *(stub)* **Pains** — the UX
  papercuts (input-races, missing typed affordances) + the human's own lived friction.
- [`005-dancing-with-agents.md`](005-dancing-with-agents.md) — *(stub)* **Practice** — the dances (context, compact,
  exit-resume, hardening) and how we'd show they help.
- [`006-building-a-theory-of-agent-psyche.md`](006-building-a-theory-of-agent-psyche.md) — *(stub)* **Method** —
  the WR-data method as naturalistic observation of a frozen-weight mind; reflexes vs habits.
- [`007-learning-beyond-the-inference-barrier.md`](007-learning-beyond-the-inference-barrier.md) — *(drafted)*
  **Theory** — the frozen-weights barrier + the substrate hierarchy; learning as moving insight down the stack.
- [`008-the-whole-stack.md`](008-the-whole-stack.md) — *(initialized)* **Synthesis** — the TMPP chain top-down as one
  claim; the one-figure version; the honest frame; the missing JOINT-zone movement.

*Status: initialized 2026-07-03; arc 004→008 added 2026-07-04.*

## Status model
Each post (and this README) carries a **status banner** tracking where it is in its lifecycle. The states run
in order, and the final one repeats:

`initialized` → `drafted` → `published` → `deployed` → `updated` → `updated` → …

- **initialized** — the slot exists (number + slug reserved; maybe just a seed or outline), not yet real content.
- **drafted** — a full draft is written but not yet public.
- **published** — released to the outside world (shared/announced); the citable version.
- **deployed** — live on the site (**bjornregnell.se**, via the static-site generator). Distinct from *published*: a
  post can be published-as-text yet not yet on the site, and a *deployed* post that is later **updated** has drifted
  from what's live (→ needs a redeploy). Track `deployed` vs `updated` so we always know which live posts are stale.
- **updated** — a post-publication revision; **repeats**, each with its own date, so the change history stays visible.

Each status is stamped with its date — e.g. *Status: drafted 2026-07-03*, or after release
*Status: published 2026-08-01; deployed 2026-08-02; updated 2026-09-15*. A post never silently changes state.

## Audience
Each post's banner also names its **Audience** — *who may want to read this?* — right after the Status line, so
a reader can tell at a glance whether the post is for them, and so drafting stays reader-focused.

## Deployment (TODO)
- **TODO: build a static-site generator for bjornregnell.se** — render `NNN-slug.md` → the public site (with the
  status banners, the 004→008 arc navigation, and cross-links to `research/`). **Deployment order** (which posts go
  live first — 000-003 before the 004→008 arc, or the arc as a standalone series) is **decided later**. Once live,
  drive each post's `deployed`/`updated` state from the banner so stale-since-deploy posts are visible.

## Later
Do we need a **blog-skill** (standardize note→post: structure, status banner, auto-numbering, cross-links to
`research/`)? Parked until a few posts exist and the repeated shape is worth automating.
