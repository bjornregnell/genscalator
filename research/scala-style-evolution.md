# Self-conscious evolution of the `scala-style` skill

- **Question:** the [`scala-style`](../skills/scala-style/SKILL.md) skill encodes *hard, non-black-and-white
  tradeoffs* (safety ↔ token-efficiency ↔ performance; immutability vs. a justified local `var`; deps vs.
  purity). How should the skill **evolve from real use** — refined by the agents applying it when they hit
  a case its guidance doesn't cover — so it keeps aligning with genscalator's goals, *without* drifting,
  bloating, or contradicting itself?
- **Why it matters:** a static style guide goes stale; an agent-edited one risks drift. We want a
  **disciplined feedback loop** where friction in real use becomes a small, reviewed improvement — the
  same "propose, human approves, ship" pattern genscalator already uses for tools, applied to the skill
  itself. This is also a probe of a bigger question: *can agents safely co-maintain the very habits they
  run under?*

- **Plan (open — not started):**
  1. **Capture friction, don't auto-edit.** When an agent finds the skill's guidance insufficient or
     self-contradictory mid-task, it should *note the case* (and proceed pragmatically), not silently
     rewrite the skill — that would violate the "research doesn't interfere with daily use" rule.
  2. Collect those cases here (or via issues) as small, concrete tradeoff examples.
  3. Agent **proposes** a minimal skill edit + rationale tied to a foundations goal; **human reviews and
     ships** (the existing contribution discipline — see [`../CONTRIBUTING.md`](../CONTRIBUTING.md)).
  4. Track whether each change actually improved agent + human outcomes (clearer? fewer bad tradeoffs?
     smaller review burden?), so the skill earns its edits rather than accreting them.
  - **Open design questions:** what's the lightest "friction log" mechanism? How do we keep the skill
    *short* as cases accumulate (curate, don't append)? When does Safe-mode capture-checking let us replace
    prose guidance with compiler-enforced rules (cross-link the safety-flags roadmap)?

- **Status:** open — stated as a direction; no experiments run yet.
- **Findings:** _(none yet)_
- **What shipped:** _(nothing yet — this file states the intent)_
