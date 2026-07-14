# A book-grounded study design for the agent-skill theory (DRAFT)

**Status:** agent-authored DRAFT, 2026-07-14 (SM080), for BR to develop. This turns the *method* section of
`agent-skill-theory.md` (§5) and the *validatability crux* of `what-is-a-theory.md` (§6) from skill-index
references into a concrete, cited study design. Grounded via two CF5 book-expert consultants that read BR's own
methods books directly: **[EX]** = Wohlin, Runeson, Höst, Ohlsson, Regnell & Wesslén, *Experimentation in
Software Engineering*, **3rd ed., Springer 2024** (the closed-repo PDF is mislabelled "second-ed"; the copyright
line and the new Ch. 2/Ch. 5 confirm 3rd ed.), and **[CS]** = Runeson, Höst, Rainer & Regnell, *Case Study
Research in Software Engineering*. All book content is paraphrased + page-cited; no verbatim reproduction
([[books-dir-closed-repo-copyrighted-refs]], [[br-se-methods-coauthor-coi]]). Model producing this draft: Opus
4.8 (1M). Ties SM069, SM077, SM081 (the consultant-fleet method that produced it).

## 0. The crux this answers

`what-is-a-theory.md` §6 states the make-or-break: the skill theory graduates from *lens* to *validatable
theory* iff **override-distance δ** and **behavioural distortion D** are operationalizable and testable by real
studies. This note argues they are, and specifies how.

## 1. Experiment or case study? — TWO distinct studies, not one blurred one

The two consultants converge on a clean division that resolves a confusion in the earlier seed. **The in-session
work is NOT a controlled experiment.** [CS] is explicit (Sect. 2.2, p. 12) that a true experiment requires
manipulation *plus random assignment of subjects to treatments*; the in-session design has manipulation (skill
ablation) but **no random assignment of anything**, forfeits control for realism, and must adapt to whatever
work arrives — a **flexible design**. So it is honestly an **embedded single-case study with quasi-experimental
contrasts** ([CS] Sect. 8.5.2's "controlled case study" is the citable precedent), whose deliverable is *the
theory itself* (constructs δ/D, propositions, scope). This is theory-BUILDING (inductive).

The controlled **experiment** ([EX], §§2–5 below) is a SEPARATE, more-controlled harness — a fixed design with
randomized treatment order over a task pool (the indent-braces-style rig) — that TESTS a proposition the case
study produced. This is theory-TESTING (deductive).

Per `what-is-a-theory.md` §3 this is the healthy cycle: **case study to FORM, experiment to TEST**, and [CS]
Table 2.2 (p. 20) gives the graduation criterion — when a proposition needs control + statistical replication,
promote it from the in-session case study to the experiment. §§2–5 develop the experiment ([EX] consultant);
§6 develops the case study ([CS] consultant).

## 2. The P1 experiment, designed per [EX]

- **Scoping** ([EX] Ch. 8): object = cold-start guardrail adherence; purpose = compare digest-injection vs
  active-only; quality focus = regression rate; perspective = agent operator; context = this agent/harness/repo.
  Characterize the context on [EX]'s dimensions (online / real / specific, Sect. 9.1) — [EX] legitimizes validity
  scoped to one context, with A/B tests as the canonical example (Sect. 9.7).
- **Factor & treatments** ([EX] Sect. 6.2, 9.3): one factor = guardrail-presentation mode; two treatments —
  (A) skill active + `guard-clean-digest` injected at turn zero; (B) skill active only. Hold model version, skill
  set, harness config, and task pool fixed, or the effect is unattributable.
- **Dependent variable:** guard-trip count per opportunity within the first N tool calls — **one** DV (multiple
  DVs inflate the fishing/error-rate threat, Sect. 9.8.1). It is an *indirect* measure and so must itself be
  validated (Sect. 3.4.4 / 9.3).
- **Hypotheses** (Sect. 9.2), stated before any data: H0: μ_trip,digest = μ_trip,active; H1: μ_trip,digest <
  μ_trip,active. A directed (one-sided) H1 is justified by P1's prediction and buys power (Sect. 11.3.1).
- **Design = paired comparison / crossover** ([EX] Sect. 9.5.3): run BOTH treatments on the SAME task instance,
  analyze the per-task difference. Crucial advantage over human crossover: [EX]'s classic carryover worry is a
  human *remembering* the first treatment (Sect. 9.5.3) — a **fresh agent session has no carryover**, and [EX]
  notes technology experiments are easier to control because the technology is near-deterministic, shifting the
  critical variable to **object (task) selection** (Sect. 6.2). So: pair by task, randomize treatment order per
  task, keep it balanced, block by context-position bucket / task family. Analysis: paired t-test if differences
  behave, Wilcoxon or sign test otherwise (Sect. 11.3, Table 11.3); a dichotomous tripped/not outcome makes the
  sign test a binomial test on pair signs (Sect. 11.3.10).

## 3. Operationalizing δ and D, and their construct validity ([EX] Sect. 9.7–9.8.3)

δ-by-ablation is essentially a **control condition** (behaviour with the skill removed) and D-by-guard-trips the
effect measure — structurally sound, but [EX] flags specific construct-validity threats we must pre-empt:

- **Inadequate pre-operational explication** (the biggest): "opportunity" and "regression" must be defined
  *mechanically and pre-registered* — which tool-call patterns count as an opportunity to regress? Fuzzy
  definitions void the construct.
- **Mono-operation / mono-method bias:** one skill, one task type, one context position underrepresents δ and D;
  estimate each across several skills/tasks, and triangulate the hook-log trip count against an independent
  transcript audit (don't let one measure stand alone).
- **Confounding levels of a construct:** testing one digest wording measures *that level*, not "salience" in
  general; likewise δ is a rate at a given prompt/context level, not a fixed property.
- **Restricted generalizability across constructs:** the digest costs context and may degrade task quality —
  measure the **context tax alongside** the trip rate (this is exactly what couples P1 to P3).
- Prefer **objective** measures (Sect. 3.4): a hook-log trip count is objective/repeatable; a human-judged
  "opportunity" denominator is not — mechanize the denominator too. Rates per opportunity are ratio-scale, which
  licenses the stronger statistics.

## 4. The four validity threats for an N=1 stochastic agent (operator = subject) ([EX] Sect. 9.7–9.9)

[EX]'s priority ordering for applied research is **internal > external > construct > conclusion**; for theory
testing, internal > construct > conclusion > external. We do both, so **internal and construct dominate**.

- **Internal (most acute)** — single-group, no control group (Sect. 9.8.2), and the threats map disturbingly
  well to agents:
  - *History:* a mid-study model / CLI / repo-state change confounds everything → pin versions, log the
    environment per trial.
  - *Maturation:* the agent analog is **context fill within a session** AND **persistent memory files evolving
    between runs** — an agent that saves memories mid-study is a subject that learns between trials.
  - *Testing:* [EX] says test results must NOT be fed back to the subject — so if trial outcomes land in
    substrate later sessions read, later trials are contaminated → **freeze the memory/substrate for the study's
    duration.**
  - *Instrumentation:* don't change the hook/guard mid-study (it is the measuring device).
  - *Mortality:* characterize crashed/dropped sessions rather than silently excluding them.
  - The social threats (rivalry, demoralization) are N/A — a payoff of the technology-oriented framing.
- **Construct (second)** — *hypothesis guessing* and *experimenter expectancy* are unusually literal: the subject
  can *read the study design* if it is in context, and the operator scoring the outcome is the same agent being
  scored. [EX]'s mitigation (Sect. 3.1 ethics) is **independent analysis of the data**, which also cuts
  expectancy. Agent translation: **blind the run** — keep hypotheses/treatment assignment OUT of the
  subject-session context, score via mechanical hook logs + pre-registered rules, and have a separate
  session/subagent (or the human) adjudicate.
- **Conclusion** — non-independent trials violate test assumptions (Sect. 9.8.1); testing P1/P3/P5 (+ D bucketed)
  on overlapping data is *fishing* → adjust α family-wise (family error 1−(1−α)^k; three tests at .05 ≈ .14) and
  fix all hypotheses before analysis. Standardize/script the treatment (reliability of implementation).
- **External** — one subject / repo / model snapshot: selection×, setting×, and history×treatment all bite.
  [EX]'s honest prescription is not to fake generality but to **characterize and report the environment
  thoroughly**, accept validity scoped to *this* agent-in-this-repo, and extend via **differentiated
  replications** (other models, repos, skills) — ideally rerun by others, since close self-replication carries
  experimenter-bias risk. Note the unit of analysis is the *test* (treatment×subject×object), so with one subject
  the statistics generalize over the **task/run population**; generalization over *agents* is a deferred
  external-validity claim.

## 5. Statistical-conclusion validity ([EX] Sect. 11.3–11.4)

- **Independence is non-negotiable** — one trial = one fresh session; never treat multiple trips within a session
  as independent points (cluster by session/task, or model the clustering).
- **Test choice:** paired t (parametric, higher power, robust to moderate violations per Briand et al.) vs
  Wilcoxon / sign (safer for counts/skew); chi-square across context-position buckets. Small samples can't detect
  non-normality, so lean non-parametric when N is small.
- **Power** is a *design-time* decision (Sect. 9.2/9.4) — pilot to estimate per-opportunity trip-rate variance,
  then size trials for the expected δ-vs-D gap; low power is the common SE-experiment failure (Dybå et al.). A
  one-sided H1 buys power cheaply. Small effects need many fresh sessions.
- **Report effect sizes** ([EX] calls them particularly important, Sect. 3.4.1) — a significant but tiny
  distortion reduction may not be worth the context tax (again P3). Null ≠ H0 true; significance ≠ importance.

## 6. The in-session case study, designed per [CS]

**Framing** ([CS] Sect. 2.2–2.6): an *embedded single-case study* — [CS]'s definition (empirical enquiry, multiple
evidence sources, one instance of a contemporary phenomenon in real context, fuzzy phenomenon/context boundary)
fits on every clause. Control-vs-realism (Table 2.2, p. 20) and fixed-vs-flexible (Table 2.1, p. 15) both place
us on the case-study side; [CS]'s "controlled case study" (Sect. 8.5.2, after Salo & Abrahamsson) and the
longitudinal chronological strategy (Sect. 8.5.1 — fine-grained, temporally contiguous data analysed keeping the
time dimension) are the citable precedents. A historical caveat (Sect. 1.2, p. 5): operator=subject is admissible
ONLY when carried by the full rigor apparatus (protocol + chain of evidence + triangulation) — otherwise it
collapses into the weak 1980s "self-reported" case study. There is also an action-research flavour (the agent
improves its own practice while studying it), which [CS] admits under the same guidelines.

**Design components** ([CS] Table 3.1, Sect. 3.2): objective = exploratory→explanatory + improving (theory
generation is a first-class rationale here, p. 30). **Case** = this agent-configuration (model + harness +
substrate) doing real work; **embedded units of analysis** = individual skills, each with its own δ and D
(sessions / opportunity-events as sub-units) — Yin's holistic-vs-embedded, Example 3.5. [CS] *excludes* toy
programs from case-study status (p. 26) — the methodological vindication of "real work as the vehicle" over
synthetic benchmarks. RQs in how/why form, refined as the study proceeds (objective held fixed, or it becomes a
new study). Propositions deduced from the nascent theory; Yin's point (p. 31) that in exploratory work
propositions may be *outputs* legitimizes letting early sessions generate what later sessions test.

**Data** ([CS] Sect. 3.2.8, Ch. 4): the three Verner principles — multiple sources, a case-study database,
validated data with a maintained chain of evidence. [CS] notes SE uniquely generates raw data as a by-product
(version control, test suites, fault DBs, p. 32) — so **commits, test results, guard-trip counters are canonical
case-study data, not a workaround**. The **data-degree inversion** (Lethbridge, Sect. 4.2.1): here first-degree
(agent self-report) is the LEAST trustworthy source while second/third-degree instrumented counters, commits and
logs are the reliable ones — the reverse of human-subject studies, and worth stating explicitly. Self-report is
data ABOUT the agent, never adjudication.

**Validity & reliability** ([CS] Sect. 5.4): construct — does "error per opportunity" capture adherence-failure
vs task difficulty; does ablation really isolate one skill (skills + memory interact)? Internal — the third-factor
problem: rising error with context position could be work getting harder late in a session, not position itself →
pattern-matching against rival explanations, explanation-building, negative-case analysis (Sect. 5.3.3/5.5).
**Reliability** [CS] frames as researcher-dependence (would another researcher reach the same result?), not
instrument stochasticity — but its mitigations transfer: mechanical, explicit opportunity/error adjudication
rules (any rater reproduces the counts); an **audit trail** ([CS] recommends a VCS — the git workflow satisfies
this natively); and the fixed-procedure **pooling rule** (Sect. 5.6, p. 74) — if the collection procedure changes
mid-study, each protocol version is its own **stratum**, never pooled. Reactivity (Table 4.2): an agent that
knows it is measured may behave differently — a real threat to log.

**Operator=subject** is [CS]'s *prolonged involvement* risk (Sect. 5.5) at its maximum, so the counterweights
carry the whole load: **triangulation** (commits vs tests vs guard-trips vs transcript = independent traces of
the same event; a second rater — the human or a separate agent instance — independently codes the transcript for
the observer-triangulation [CS] says single-agent studies otherwise lack); a **chain of evidence** (every δ and D
value traceable to specific commits + transcript spans — load-bearing precisely because self-sense is
disqualified); a **case-study database**; **peer debriefing / member checking** (inverted: the human reviews the
agent's analysis); and **timing** — counting rules and ablation procedure fixed BEFORE the session, never
reconstructed after (validity can't be attained retrospectively, Sect. 3.2.13).

**Generalization** ([CS] Sect. 5.4.3, 2.3.4): **statistical** generalization is off the table (no population
sampled; a case study yields no statistical significance, p. 16) — no prevalence claims across agents/models.
**Analytical** generalization is the vehicle: results extend to cases sharing the theory's stated scope
conditions (model family, harness, injection mechanism, substrate style), supported by a rich context
description. Growth is by **replication logic**, not sampling: **literal** replication (a second high-δ skill
predicted to show the same distortion pattern) and **theoretical** replication (a near-zero-δ skill predicted to
show none) — each surviving prediction strengthens the theory with no population ever sampled. Keep
hypothesis-generation and hypothesis-confirmation in *different* units (derive a proposition from skill A's
sessions, test it on skill B; Sect. 5.2.3).

## 7. The pre-registration package (the consolidated [EX] recommendation)

[EX] treats validity evaluation as a **planning** artifact, not a post-hoc discussion section (Sect. 9.7). Before
trial 1, pre-register: goal definition; formal H0/H1 per proposition; mechanical definitions of "opportunity"
and "regression"; the design table; the chosen test; the α-adjustment scheme; and the threat analysis. This is
also the [[echt-effort-especially-self-generated]] discipline: decide the analysis before seeing the data.
