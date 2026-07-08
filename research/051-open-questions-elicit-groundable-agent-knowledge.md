# RT051 — Open questions to the agent as an epistemic method (eliciting groundable knowledge the human's assumptions miss)

**Status:** open research topic, seeded 2026-07-08 (BR reflection + AHA). Child/sibling of RT048 (substrate-content
power), RT049/RT050 (delegation), [[agent-affective-analogs]]; the human-side complement to the agent-side
[[echt-effort-especially-self-generated]] discipline. Empirical arm: `wr-data/dep-surface-ease-introspection-2026-07-08.md`.

## The spark (the concrete event)
Designing `tt serv` (SM020), BR offered cask / Jetty / just-JDK and, instead of directing, asked an **open**
question: *which is easiest for you?* BR's own prior was cask ("I'd use cask"). The open question surfaced the
agent's actual fluency ranking (**JDK >> cask > Jetty**) — a fact about the agent that BR's assumption would have
overwritten. BR's reflection, verbatim in spirit: *"I have many times tried (pretty shallow) to just assume I know
what is best for you and go with my desired way (I'd use cask), but BECAUSE I asked an open question I learned this,
AND it is empirically grounded in your work."*

## The claim (the research topic)
In human-agent collaboration the human carries strong priors about "the right way." **Asking the agent an OPEN
question** (not a leading/confirming one) can surface two things the human's assumption would have suppressed:
1. a **better technical answer** (here: the zero-dep JDK path, which is also simpler and dep-lean), and
2. a **fact about the agent's own capability/psyche** (its emission fluency across the options) that is
   **empirically groundable in the agent's measurable work** — not merely a generated opinion.

The finding is a **method** result for the whole 047 program: it names a human-side discipline that pairs with the
agent-side echt discipline. **The human must ASK open; the agent must ANSWER echt; then the pair must GROUND the
answer.** Only the full loop produces grounded mutual knowledge.

## Open vs leading (the operative distinction)
- **Leading / confirming:** "should I use cask?" — invites the agent to ratify BR's prior; learns nothing new;
  risks sycophancy.
- **Open / eliciting:** "which is easiest *for you*, and why?" — invites the agent to report its own state; can
  surface a fact the human did not hold.
The open form is what worked. The self-observation that BR's *default* is the shallow-assumption move (and the
open question took deliberate effort) is itself data about the human's role: the discipline is to notice the
assumption and convert it into a question.

## The load-bearing caveat (why "ask open" is necessary but NOT sufficient)
Open questions can equally elicit a **confident-but-wrong** agent stance — the corroboration-asymmetry hazard
([[echt-effort-especially-self-generated]]): a fluent narrative that *sounds* like grounded introspection but is
post-hoc. So the method is **open question -> echt answer -> EMPIRICAL grounding**, in that order. Without the
grounding step it degrades into "eliciting plausible stories," which is worse than assuming, because it *feels*
like learning. What made THIS instance real is that the elicited answer is **measurable** (compile-success rate,
hallucinated-API rate, iterations-to-green across the options — the dep-surface investigation), so it is not just
a stance. RT051 (the method) and that investigation (the measurement) are two arms of one finding.

## Connections
- **member-check symmetry:** the human is ground truth for their own state; the agent is ground-truth-ish for its
  own fluency, *checkable via the compiler-oracle* — each party grounds the other's self-report.
- **capability-not-style:** converges with the blog-003 / indent-vs-braces finding that emission ability (not
  style) dominates; here the human learns to *query* that emission ability rather than assume it.
- **the dashboard (SM022):** a live agent+human dashboard is partly an *instrument for this method* — it makes the
  groundable signals visible so open-question answers can be checked in-session.

## Candidate next steps
- Catalogue further open-question elicitations across the case study and mark which were grounded vs ungrounded
  (a running ledger; distinguishes real method-wins from plausible-story near-misses).
- Formalize the loop as a named collaboration dance (working name: the *open-question / elicit-and-ground* dance),
  if BR ratifies, in `docs/foundations.md`.
