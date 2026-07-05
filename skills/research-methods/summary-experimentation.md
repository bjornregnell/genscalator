# [ESE] Experimentation in Software Engineering — working summary

> **Our own distillation, not the book.** An original, from-scratch summary of the *ideas and structure* of the book,
> written as a fast public reference for the genscalator/WR work. It reproduces **no** text, tables, or figures from the
> book. For depth, read the copyrighted original (BR's personal copy lives in the closed repo; buy links below).

**Book:** *Experimentation in Software Engineering* — Claes Wohlin, Per Runeson, Martin Höst, Magnus C. Ohlsson,
**Björn Regnell** & Anders Wesslén, Springer (2nd ed., Springer 2024). DOI
**10.1007/978-3-662-69306-3** (<https://link.springer.com/book/10.1007/978-3-662-69306-3>).

> **Self-reference (echt).** This book is **co-authored by BR** — own that inline when citing it ("our own
> experimentation textbook"), as both first-hand authority and a conflict of interest to name openly. It is the sibling
> methods text to [CSR]; the same lineage (Wohlin et al.) runs through both.

---

## Why this book exists

To make SE claims **credible**, you often need more than a plausible story — you need a **controlled experiment**: you
*manipulate* one or more factors, *hold others constant or randomize them away*, and *measure* the effect, so a causal
claim survives scrutiny. The book is the practical, end-to-end manual for doing that in software engineering, where the
"subjects" are people writing/reading/maintaining software and the "objects" are programs, specs, and processes. Its
backbone is a **process** (scope → plan → operate → analyze → present) plus a disciplined vocabulary for **measurement**
and **validity threats** so that experiments are comparable and replicable.

**When an experiment is the right strategy:** you can define a **treatment** (the manipulated factor, e.g. brace-style),
apply it to **experimental units** while controlling confounds (ideally via **randomization** and **blocking**), and
**measure a response variable** that operationalizes your concept. If you cannot control/manipulate the factor in
context, you are in **case-study** territory ([CSR]); if you want population estimates, a **survey**; if you aggregate
existing studies, a **systematic review/SLR**.

## Empirical strategies & how to choose (ch. 2)

The book situates the experiment among empirical strategies and gives a **decision structure** for picking one from the
RQ, not from habit (esp. §2.5 the decision-making structure, §2.6 a comparison of research approaches). Experiments buy
you the strongest **internal validity / causal** leverage at the cost of **external validity** (artificial settings,
student subjects); case studies trade the reverse. Often you **combine** them (a small embedded experiment inside a
field case study).

## The measurement backbone (ch. 3 — read this before designing anything)

- **Ethics** (§3.1) — informed consent, no deception without justification, subject welfare; especially sharp when
  subjects are your own students or, in our case, when the researcher is also a subject.
- **Replication** (§3.2) — exact vs conceptual replication; the point of the whole packaging discipline.
- **Theory** (§3.3) — experiments should connect to and feed theory, not float free.
- **Measurement & scale types** (§3.4) — **the constraint that trips people up**: a variable's *scale type*
  (**nominal / ordinal / interval / ratio**) determines which statistics and tests are even *legal*. Choosing a metric
  fixes what analysis you're allowed to do later.
- **GQM (Goal–Question–Metric)** — derive metrics top-down from goals so you measure what matters, not what's easy.

## The experiment process (Part II — the operational core)

A pipeline, each stage a chapter:

1. **Scoping** (ch. 8) — state the **goal** (often in a GQM-style goal template): object of study, purpose, quality
   focus, perspective, context. Fix **hypotheses** and what is manipulated (**treatment/factor**) vs measured
   (**response/dependent variable**).
2. **Planning** (ch. 9) — the design-heavy stage:
   - **Context selection** — students vs professionals, toy vs real, off-line vs in-vivo (each is an external-validity
     tradeoff).
   - **Hypothesis formulation** — a precise **null (H0)** and **alternative (H1)**; you test to *reject H0*.
   - **Variables** — independent (factors + their levels), dependent (response), and **confounding** variables to
     control.
   - **Subjects** — selection and sampling; representativeness.
   - **Experiment design** — the standard designs: **one-factor** (with two or more treatments), **paired** designs,
     **blocked** designs (block on a nuisance factor like subject skill or, for us, **model**), **factorial** designs.
     Randomization, balancing, and blocking are the three tools against confounds.
   - **Instrumentation** — objects, guidelines, measurement instruments prepared *before* running.
   - **Validity evaluation** — argue the **four validity threats here, at planning time** (see below), not as an
     afterthought.
3. **Operation** (ch. 10) — **preparation** (commit subjects, brief them, pilot), **execution** (run it, keep
   conditions constant), **data validation** (sanity-check that the collected data is trustworthy before analysis).
4. **Analysis & interpretation** (ch. 11) — **descriptive statistics** first (plots, central tendency, spread, outlier
   inspection), optional **data-set reduction**, then **hypothesis testing**: pick the test for the **design + scale
   type** (parametric like t-test/ANOVA when assumptions hold; non-parametric like Mann–Whitney / Wilcoxon /
   permutation tests otherwise), report **effect size**, not just a p-value.
5. **Presentation & package** (ch. 12) — report enough (design, raw data, analysis, threats) that others can
   **replicate** and aggregate; this is what turns one experiment into cumulative knowledge.

## The four validity threats (ch. 9 planning; the thing to argue every time)

Prioritize per study — internal/conclusion matter most when the goal is a causal claim; external/construct when you
want it to travel:
- **Conclusion validity** — is there a *real* statistical relationship? Threats: low power, wrong/violated test, fishing
  & the researcher-degrees-of-freedom problem, unreliable measures/treatment implementation. *(Preregistration and a
  fixed analysis plan are the defense.)*
- **Internal validity** — is the relationship **causal**, not due to a confound (history, maturation, selection,
  instrumentation, mortality, learning/order effects)? Randomization and blocking are the countermeasures.
- **Construct validity** — do the operationalizations (treatment *and* metric) actually reflect the theoretical
  construct? (Mono-operation/mono-method bias, evaluation apprehension, experimenter expectancy.)
- **External validity** — do results generalize beyond these subjects/objects/setting? (Student subjects, toy programs,
  artificial time pressure.)

Note the deliberate contrast with [CSR]: a **case study swaps in *reliability*** where the experiment has *conclusion*
validity — because a case study isn't doing statistical inference.

---

## How genscalator/WR uses this book

- **Worked example we already run:** `research/experiments/indent-vs-braces/` — a preregistered experiment, **blocked by
  model**, permutation-tested, that honestly **reported the null**. Its `significance.scala` + `BIG-RUN-PREREG.md` are a
  reusable template for the process above (scope → preregister → operate → permutation test → report effect + null).
- **The cross-model / Fable-switch work is where conclusion + internal validity bite.** The `CO4 → CF5` switch is a
  **one-way intervention**: to make any before/after claim causal we must **capture the CO4 baseline *before* switching**
  and **hold the harness/substrate constant** (pin the CLI version, change only `--model`). See
  `research/029-cross-model-psyche-comparison.md`. Getting this wrong = an uncontrolled confound (model *and* harness moved
  together).
- **Scale type discipline (§3.4)** governs what we may compute from our logs — much WR data is ordinal or count data, so
  non-parametric / permutation tests, not blind t-tests.
- **Report the null, preregister the DV** (§ conclusion validity) — a preregistered null is a real result; it guards
  against post-hoc story-fitting when the researcher is also the subject.

## When to read the real thing

This summary is an index, not a substitute. Read the copyrighted original for the actual depth — the design catalog and
validity checklists of ch. 9, the test-selection guidance of ch. 11, or the scale-type rules of §3.4. BR has the book;
ask him to pull a section's full text when depth is needed. **Never** paste the book's text/tables/figures into this
public repo — cite the *section*, not the passage.
