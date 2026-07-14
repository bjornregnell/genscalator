# What is a theory? — a working note (DRAFT seed)

**Status:** agent-authored DRAFT, 2026-07-14, for BR to develop. A companion to `agent-skill-theory.md` that
grounds the meta-level: what *we* mean by "theory", and the rubric we will judge our own theories against. It
leans deliberately on BR's own methods framing — *Experimentation in Software Engineering* [EX] and *Case Study
Research in Software Engineering* [CS] — surfaced via `skills/research-methods` (BR is a co-author, so the
vocabulary here is his: the four validity threats, analytical vs statistical generalization, "theory §3.3").

## 1. What a theory is (and is not)

A **theory** is a set of related **constructs** plus **propositions** stating relationships among them, that
**explains** and **predicts** a class of phenomena within a stated **scope**. It is not a single fact, not a
model of one case, and not a bare taxonomy (though constructs + a taxonomy are ingredients).

Its parts:
- **constructs** — the concepts (e.g. *gap*, *salience*, *firing*);
- **propositions** — claimed relationships among constructs, ideally **causal**;
- **scope / boundary conditions** — where it applies and where it does not;
- **operationalization** — the bridge to observables, so it can be measured and tested.

In SE these usually take the form of **variance theories** (X causes Y, ceteris paribus) or **process
theories** (a sequence of events producing an outcome).

## 2. What we ask of a theory — the three powers

### Explanatory power
Accounts for **why** the phenomena occur — offers a **mechanism**, not just a correlation. Judged by *scope*
(how much it explains), *parsimony* (how little it assumes), and *coherence*.

### Predictive power
States what **will** be observed under given conditions — crucially including **novel** predictions not used to
build it. Prediction is the sterner test. Note explanation and prediction can come apart: you can predict
without explaining (a black-box forecast) and explain without precise prediction (a mechanism in a noisy
system). A strong theory does both.

### Causal relationships
The strongest propositions say X **causes** Y, not merely co-occurs. Causality needs (a) association, (b)
temporal precedence, (c) no confounding — which is exactly why **internal validity** is the pivotal threat for
causal claims, and why **controlled manipulation** (an experiment) gives the strongest causal leverage.
Establishing causality is what licenses **intervention** (§4).

## 3. Qualitative vs quantitative — two complementary modes, not a hierarchy

- **Qualitative** — constructs, mechanisms, process; the "how / why"; rich context. Built via case study,
  observation, coding, keeping a **chain of evidence**; generalizes **analytically to theory**. Strong for
  **theory-building** and explanation; weak on precise magnitude.
- **Quantitative** — measured variables, magnitudes, statistical relationships; the "how much / how often".
  Built via experiment and measurement; generalizes **statistically to a population** (given sampling). Strong
  for **theory-testing** and prediction; weak at discovering new constructs.

The healthy cycle: **qualitative to FORM** the theory (find constructs, propose mechanisms) → **quantitative to
TEST** it (operationalize, measure, try to falsify) → back to qualitative when anomalies appear. Mixed methods
and **triangulation** strengthen both. **Construct validity** is the bridge in either mode: the
operationalization (treatment *and* metric) must actually capture the theoretical construct.

## 4. The engineering perspective — theory in service of improving the world

Science seeks to **understand**; engineering seeks to **improve**. In a design discipline (SE, and our agent
work) a theory earns its keep by informing **interventions** that make outcomes better — not only by being true.

The engineer combines **three** sources: (1) **validated theory** (what generalizes), (2) **own experience +
tacit knowledge** (what the theory does not yet cover), (3) the **specific context** at hand. Theory is a lever,
not a substitute for judgment: it narrows the search and explains **why** a design works, so success transfers
rather than being luck. This is the **improving / prescriptive** stance (cf. the [CS] case-study objective
*improving*; design science): a good theory yields **design principles** — *to get outcome Y, do X, because
mechanism M* — that a practitioner applies and adapts.

Crucially, using theory to improve the world is **also how you test it**: an intervention that behaves as
predicted corroborates; one that fails feeds the theory back (the practitioner as experimenter). This is our own
MO — real work is the vehicle, the **intervention IS the study** (`skills/in-session-experiment`).

Honesty guard: **experience without theory does not generalize** (folklore); **theory without experience is
brittle in context**. Hold both.

## 5. What is a GOOD theory?

- **Falsifiable (Popper).** It **forbids** something — makes predictions that could be shown *wrong*. A claim
  compatible with every possible observation explains nothing. The sharper the prohibitions, the more testable
  and the more informative.
- **Validatable through real-world studies.** Falsifiability must be **practical**, not merely logical: the
  constructs must be **operationalizable** and the predictions **checkable** by feasible empirical studies
  (experiments, case studies) under an explicit **validity argument** — construct / internal / external /
  conclusion (or **reliability** for case studies). *A theory you cannot design a study to test is not yet a
  scientific theory* — it is a **framework or a metaphor**, and should be named as such, honestly.
- **Other marks:** *parsimony* (Occam); a stated *scope* and *boundary conditions*; *generality vs precision*
  (a genuine trade-off, not a free lunch); *internal coherence*; *fecundity* (it generates new questions and
  predictions).
- **Corroboration is not proof.** Passing a test raises confidence but never verifies; one sound
  disconfirmation outweighs many confirmations. So a good theory is one we keep **trying to break**, reporting
  the **nulls** honestly (a preregistered null is a real result).
- **Provisional and improvable.** A theory is the best current account, revised as evidence accrues — the same
  growth loop as the engineering stance in §4.

## 6. Applying the rubric to our agent-skill theory (the point of this note)

A quick self-audit of `agent-skill-theory.md` against §5:

- **Falsifiable?** In intent, yes — P1–P6 are stated to be breakable. Good.
- **Validatable?** This is the crux. It hinges on whether **gap** and **drift** are operationalizable and
  measurable by real studies (ablation, `tt guardcheck` trip-counts, transcript
  metrics). If gap and drift are measurable → a scientific theory-in-progress. If not → honestly a **lens / metaphor**
  (which the draft already flags).
- **Explanatory?** Yes — the *override-vs-prior* mechanism explains the specimens. **Predictive?** Partially —
  P1–P6 make novel predictions.
- **Causal?** The propositions *claim* causal levers (salience → fewer regressions). Establishing them needs
  **internal validity** = controlled manipulation (the SessionStart-digest A/B of P1), not observation alone.
- **Qual vs quant?** We are **theory-building** from qualitative WR specimens now; the next move is
  **quantitative testing**.
- **Engineering?** The payoff is **design principles** (guardrail digest at turn zero; *constrain-not-inform*
  for clampable behaviours) that improve the agent — and applying them is also how we test them.

**Honest status of the agent-skill theory:** a well-formed **lens** with falsifiable propositions, pending the
operationalization of gap and drift to graduate into an empirically validated theory. Naming that gap is itself the
§5 discipline in action.

## Housekeeping / ties

- Companion to `agent-skill-theory.md`; grounds the meta-level and supplies the goodness rubric.
- Leans on BR's own [EX] / [CS] via `skills/research-methods` (the four validity threats; analytical vs
  statistical generalization; theory feeds studies, §3.3).
- Ties SM069, `skills/in-session-experiment` (intervention-as-study), the WR method, and the "structure over
  willpower" / constrain-vs-inform thread.
