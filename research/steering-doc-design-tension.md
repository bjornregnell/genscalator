# Steering-doc design tension — salience-under-rot vs nuance/judgment (and how to have both)

**Status:** open, foundational (BR meta-reflection 2026-07-04). Adjacent to `instruction-adherence-decay.md`,
`smart-zone-ceiling.md`, and the `foundations.md` substrate/structural-vs-knowledge material.

## The tension (BR, verbatim gist)
*"Agent context grows and grows; every round feeds everything to the agent; the agent's mind gets clobbered by
clobbered/rotten context; so the agent leans on special things in its substrate — documents with BIG steering power;
the human tends to write those as HARD CONCISE DO/DON'Ts; nuance is lost; the agent gets dumb."*

Two opposing pressures on any high-steering doc (a skill, a memory, an `AGENTS.md` rule):
- **P1 — salience / robustness / cost** → favors **terse, hard DO/DON'Ts**: they cut through a rotting context, are
  unambiguous under momentum, and are cheap in tokens.
- **P2 — judgment / nuance** → favors **calibrated, explained, exception-aware** guidance: it preserves the agent's
  ability to reason about the case in front of it.

Pick wrong in either direction and you lose: **too blunt** → the agent over-applies the rule rigidly and "gets dumb"
(no room for the exception); **too nuanced/verbose** → the rule dilutes, costs context, and is *lower-salience* so a
rotting context drops it (an `instruction-adherence-decay` failure). The naive reading is a forced trade of nuance for
robustness.

## It's a FALSE binary
The dilemma only exists if steering docs are treated as **static context that must survive rot by salience alone.**
Re-engineer the substrate and both hold:

1. **Layer, don't choose (anchor + expansion).** A terse, salient *headline* (the DO/DON'T — survives rot, steers under
   momentum) **plus** the nuance / rationale / exceptions *beneath* it (engaged only when the agent is actually at the
   decision). The blunt part does the cutting-through; the nuance is present when judgment is live. Cf.
   *writing-anchor-points-for-skimmers* (disambiguate at the landing point; detail below).
2. **Structure beats rule.** Where the safe path can be a **tool / allowlist / hook**, *no rule is needed* — the
   blunt-vs-nuanced tradeoff never arises (foundations: *structural > knowledge*). So **shrink the surface that must
   live as a rule** in the first place; the tension only bites what genuinely must be knowledge.
3. **JIT retrieval defeats the premise.** Blunt "wins" only because it survives rot. If the nuanced rule is
   **re-surfaced fresh at the decision point** (just-in-time injection — a hook that shows the relevant guidance exactly
   when the triggering action is attempted), *freshness* supplies salience **without** stripping nuance. The
   "must-be-blunt-to-survive" assumption dies. Cf. `inference-time-learning.md` §4 (the retrieval gap; push memory
   toward substrate #3).
4. **Calibrate rigidity to stakes × reversibility.** Hard DO/DON'T where the action is **irreversible / dangerous** and
   nuance is a *liability* ("never force-push", "destructive git is human-only"); **nuance** where judgment adds value
   and errors are cheap/reversible (style, phrasing, structure). **Uniform bluntness is the real mistake** — match a
   rule's rigidity to the cost of the agent getting it wrong.
5. **Escape-hatch meta-rule (backstop).** State, in the doc itself: *"when a guideline and good judgment genuinely
   conflict, follow the judgment and surface the tension."* Restores judgment atop blunt rules — but it is a *knowledge*
   safeguard (retrieval-fragile), so it's a backstop, not the primary fix.

## References must point to easily-reachable substrate (BR 2026-07-04)
A distinct hazard, surfaced reviewing the `blog-assistant` skill: an aside like `(e.g. the "panic writes")` is a
**dangling pointer to session-specific / ephemeral matter.** A high-steering doc loads **fresh every session**
(substrate #3), so that reference means nothing to a clean context and — worse, under an eager or rotten-context
disposition — triggers a **rabbit hole** (the agent greps to "resolve a citation it feels it should know") or a silent
"huh?". **Reframe (BR): the test is not "is it in scope" but "is it *easily-reachable substrate matter*?"** — can the
reader resolve it from **durable, findable** substrate without a hunt, and is resolving it even *required* to get the point?
- **Illustrations must self-explain (be time-invariant):** "quote the human's own words whole rather than paraphrase"
  needs no lookup; "(e.g. the panic writes)" does.
- **References may point only to durable, findable substrate** (foundations, a README, a committed note) — and must
  **never be *required*** to understand the point; they serve the reader who *chooses* to go deeper.
- **No dangling episodic pointers** to one session's events in a doc that outlives that session.
This is substrate-hierarchy hygiene applied to a doc's own citations: a fresh-loaded (substrate #3) doc must be
**self-contained against substrate #1** (the volatile context it cannot assume). Fixed the instance in `blog-assistant`.

## Already applied
The `skills/blog-assistant/SKILL.md` opens with a "how to read this skill" meta-rule (calibrated-not-absolutist +
surface-the-tension) — an instance of (5) plus the layering ethos of (1). This note is the general form of BR's earlier
steer *"don't write high-steering docs too bluntly/concisely if you want to preserve nuance."*

## Open questions / measurement
- **Testable:** does a *blunt* rule vs a *layered* (headline+nuance) rule change **both** adherence AND
  judgment-quality (appropriate exception-handling)? The indent-vs-braces-style harness could vary rule bluntness while
  holding the task, and score compliance *and* correct-exception behaviour — bluntness likely trades one for the other,
  layering may dominate both.
- Which docs are safe to make blunt (map to stakes × reversibility)? A per-rule rigidity label.
- Can JIT injection be made cheap/general (one hook, not one-per-rule) so nuance survives rot by freshness?

## What shipped
Nothing yet (foundational note). Reframes existing artifacts: `instruction-adherence-decay.md` (why blunt is tempting),
`foundations.md` (structural>knowledge, the substrate hierarchy), the `blog-assistant` skill (first calibrated-doc
instance). Candidate graduation: a `scala-style`/skill-authoring guideline — *layer, structuralize, JIT, calibrate to
stakes* — for writing every future high-steering doc.
