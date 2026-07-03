# Course-driven development: MSc RE students as an open-source community pipeline

> **Status: IDEA — wild but big, unvetted (BR, 2026-07-03).** A strategic/organizational idea, parked in
> `research/` (not the PRD — it is not a committed requirement). Spans **reqT + genscalator + a new app + BR's
> course**, so it may later move to reqT, a course repo, or a shared community doc. Captured here because two of
> the three teams target genscalator and reqT directly.

## The idea
In **BR's MSc-level Requirements Engineering course**, let students **choose to work in one of three teams**:
1. **reqT team** — RE *tooling* (the requirements-modelling language/tool).
2. **genscalator team** — Agentic SE *tooling* (the typed-tools + agent-collaboration toolbox).
3. **app team** — the **non-meta** level: an application students **use themselves and understand** — e.g. a
   course-management system, or a course chat.

**Core thesis (BR):** *students become part of our open-source community around reqT and genscalator.* The
course is a **contributor pipeline** — students work on **real, living OSS** with real stakeholders, not toy
projects, and some stay in the community after the course ends.

## Why this is more than a nice class project (value-add framing)
- **A pedagogical ladder across three abstraction levels.** reqT is **meta-meta** (a *language for
  requirements*); genscalator is **meta** (*tooling for how software is agentically built*); the app is the
  **object level** (*software students actually use*). Students get to do RE at **all three** — requirements
  *for a requirements language*, *for agentic tooling*, and *for a concrete app they are users of* — and can
  see how the levels differ. Rare to teach RE at more than the object level.
- **A closed-loop requirements supply chain inside the course.** The **app team are real users/stakeholders**
  who generate **genuine requirements**; the **reqT team** tools the RE process that captures and models them;
  the **genscalator team** tools the agentic building of them. The three teams form a small **value chain /
  ecosystem**, and the **interfaces between teams become an RE lesson in themselves** (inter-team contracts,
  traceability, change negotiation — the hard parts of RE, experienced not lectured).
- **Action Research at classroom scale.** This is `METHODOLOGY.md`'s AR/DSR loop with a cohort: students are
  **co-developers** of the artifacts, with a real human stakeholder (BR) and a real community. Dogfooding, not
  simulation.
- **Solves OSS sustainability (`Goal: contributeOpenSource`).** A **renewable stream of contributors** who
  already know the codebase — the perennial small-OSS problem (no contributors) turned into a feature of the
  course. Each cohort can build on the last (continuity across editions).
- **The agentic angle closes on itself.** The genscalator team **dogfoods agentic SE** on reqT / genscalator /
  the app; the app itself could be **built agentically**, so students learn **human↔agent collaboration**
  first-hand — the whole genscalator thesis, taught by doing. Agent-assisted **onboarding** (genscalator
  eating its own dog food) could cut the real cost below.

## Open questions / risks (to think through)
- **Team balance & choice** — what if everyone picks the app (concrete, familiar) and nobody picks reqT? Need
  incentives / caps / rotation.
- **Onboarding cost** — real OSS codebases are steep for a one-semester course; mitigations = agent-assisted
  onboarding, scoped "good first issue" backlogs per team, the app team as the gentle on-ramp.
- **Grading fairness** across very different team types (tooling vs app; contribution-to-existing vs
  greenfield) — needs a rubric that rewards RE *process*, not just LOC shipped.
- **IP / licensing / consent** — students contributing to public OSS need clear licensing + opt-in; grading
  must not *require* a public contribution (a student may decline).
- **The app choice** — course-management vs course-chat vs other; pick something students **self-use and
  already understand** so requirements elicitation starts from lived experience (BR's instinct).
- **Coordination** — three interdependent teams in one course is ambitious; the inter-team contract is both the
  risk and the richest learning.

## Relation to other notes
- `PRD.md` — `Goal: contributeOpenSource`; the app is effectively a new *stakeholder* (like
  `agentHarnessProvider`) whose needs drive reqT + genscalator.
- `METHODOLOGY.md` — AR/DSR; this scales the researcher-pair to a cohort of co-developers.
- `human-state-and-joint-zone.md` / the blog — students living the human↔agent collaboration the toolbox is
  about.
