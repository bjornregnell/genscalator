---
name: research-methods
description: "Guide for designing, planning, running, and reporting an EMPIRICAL study in the genscalator/WR work — choosing a research strategy (experiment vs case study vs survey vs SLR), case-study design, experiment design, validity analysis, case/subject selection, preregistration, and honest reporting. Trigger when scoping or reviewing any study, defining RQs, arguing validity/threats, planning data collection or analysis, or preparing a methods section. This skill is a fast INDEX + checklists that point into two public summaries of BR's two authoritative books (the copyrighted originals stay in the closed repo — read them directly when depth is needed); it is NOT a substitute for them. Calibrated guidance, not rules."
allowed-tools: Read Bash(tt files *) Bash(tt text *)
---

# research-methods — empirical study design for the WR work

> **Posture (calibrated, per the blog-assistant meta-rule).** This is a working index, not a textbook. When a checklist
> and good judgment conflict, follow the judgment and surface the tension. The **two source books are the authority**;
> this skill just gets you to the right section fast and keeps our practice honest.

> **Self-reference (echt).** Both source books are **co-authored by BR** (own this inline when citing them in any public
> writing — "our own methodology book", not a buried reference). That is a strength (first-hand authority) *and* a
> conflict-of-interest to name openly.

## The two books — and where to point

Use, in order of preference:
1. **Our public summaries** (in this repo, safe to link/cite anywhere) — meaty original distillations, section pointers,
   buy links. **This is the canonical pointer** for any genscalator note, blog post, or `References.scala` entry:
   - **[CS]** → [`summary-case-study-research.md`](summary-case-study-research.md)
   - **[EX]** → [`summary-experimentation.md`](summary-experimentation.md)
2. **The real (copyrighted) books** — for depth beyond the summary. BR owns both; his personal copies live in the
   **closed** repo `muntabot-synch-introprog/books/` (safe to *read* there when depth is needed). Cite the *section*,
   not the passage.

**The books:**
- **[CS]** *Case Study Research in Software Engineering: Guidelines and Examples* — Runeson, Höst, Rainer & Regnell,
  Wiley 2012. DOI 10.1002/9781118181034.
- **[EX]** *Experimentation in Software Engineering* — Wohlin, Runeson, Höst, Ohlsson, Regnell & Wesslén, Springer
  (2nd ed. 2024). DOI 10.1007/978-3-662-69306-3.

## Copyright — hard rule
The books are **publisher-copyrighted**. **Never** reproduce their text, tables, or figures into the **public**
genscalator repo — not in the summaries, not anywhere. The summaries and any genscalator note contain **our own
distillation + section pointers only**, never lifted passages. When a summary isn't deep enough, **Read the real book**
from the closed path (`muntabot-synch-introprog/books/`, BR-only) and cite the *section*, not the text.

## 0. Which strategy? (choose before designing)
Pick the research strategy from the RQ, not from habit ([EX] ch. 2 "Empirical Research", esp. §2.5 the decision-making
structure + §2.6 research-approach comparison; [CS] ch. 2 for the case-study side).
- **Experiment** — you can **control + manipulate** a factor and want a **causal** claim ("does brace-style change
  edit-success?"). Quantitative, hypothesis-testing. → §2 below. *(Our indent-vs-braces run is one.)*
- **Case study** — you study a **phenomenon in its real-life context** you can't fully control ("how does the
  human↔agent workflow actually behave?"). Flexible, mostly qualitative + mixed. → §1 below. *(WR is this; AT/SSG are
  cases.)*
- **Survey** — generalize over a population via sampling ([EX] ch. 5). **SLR / systematic review** — secondary study
  aggregating existing evidence ([EX] ch. 4; [CS] ch. 8.3 synthesis).
- Often **combined** (a case study containing a small embedded experiment; triangulation across sources — [CS] §2.3.3).

## 1. Case study — checklist + pointers ([CS])
Design ([CS] ch. 3, "Elements of the Case Study Design"):
- **Rationale + objective** (§3.2.1–3.2.2) · **Cases and units of analysis** (§3.2.3 — the *case* is the phenomenon in
  context; the *unit(s) of analysis* are what you actually examine, and they **vary with the RQ**) · **Theoretical
  frame** (§3.2.4) · **RQs** (§3.2.5) · **propositions/hypotheses** (§3.2.6) · **case selection** (§3.2.10) · **protocol**
  (§3.2.14 — write it; it's what makes the study replicable) · **legal/ethical** (§3.3).
- **Our mapping:** WR = a **longitudinal, multiple-case** study of the human↔agent workflow; **AT / SSG / genscalator
  self-dev** are the **cases / units of analysis** (see foundations "Roles and cases"). Longitudinal + multiple-case
  guidance: [CS] ch. 7.
Data collection ([CS] ch. 4): data-source types (§4.2), interviews (§4.3), focus groups (§4.4), observation (§4.5),
archival (§4.6), metrics (§4.7). *Our WR data = live archival + observation (the `wr-data/` logs, RAW-DATA).*
Analysis + validity ([CS] ch. 5): qualitative process (§5.3), **validity §5.4** (construct / internal / external /
**reliability**), improving validity §5.5, quantitative §5.6.
Reporting: [CS] ch. 6.

## 2. Experiment — checklist + pointers ([EX])
The experiment process is a pipeline ([EX] Part II):
- **Scoping** (ch. 8) — goal, hypotheses, what's manipulated (treatment) vs measured (response).
- **Planning** (ch. 9) — context, subjects, **design** (incl. **paired/blocked** designs), instrumentation, and
  **validity threats** (this is where the four validity types are argued — plan them *in*, don't bolt on later).
- **Operation** (ch. 10) — preparation, execution, data validation.
- **Analysis & Interpretation** (ch. 11) — descriptive stats, **hypothesis testing** (pick the test for the design +
  scale type — [EX] §3.4 measurement/scale types), effect size, tool support.
- **Presentation & Package** (ch. 12) — enough to **replicate**.
Essentials cutting across: **ethics** ([EX] §3.1), **replication** (§3.2), **theory** (§3.3), **measurement/scale
types** (§3.4 — the scale type constrains the legal statistics).
- **Our worked example:** `research/experiments/indent-vs-braces/` — preregistered, permutation-tested, blocked by
  model, reported the **null**. Reuse its `significance.scala` + BIG-RUN-PREREG.md as a template.

## 3. Validity — the cheat-sheet (argue these EVERY study)
The two books use slightly different four-sets — use the right one for the strategy:
- **Experiment [EX]:** **Conclusion** (stat. power, right test, reliability of treatment) · **Internal** (is the effect
  *caused* by the treatment, not a confound?) · **Construct** (do the metrics measure the concept?) · **External**
  (does it generalize beyond this setting/subjects?).
- **Case study [CS] §5.4:** **Construct** · **Internal** · **External** · **Reliability** (would another researcher get
  the same result? → the protocol + audit trail). *(Case study swaps in* reliability *where the experiment has*
  conclusion*.)*
- **For our cross-model work:** the Fable switch is a one-way intervention → **conclusion + internal validity** hang on
  capturing the CO4 baseline *before* switching and holding harness/substrate constant (see
  `research/029-cross-model-psyche-comparison.md`).

## 4. Discipline we already follow (keep it)
- **Preregister** the DV, hypotheses, and analysis *before* running (guards researcher-df / p-hacking).
- **Report the null** — a preregistered null is a real result (blog/003). Honest DV, no post-hoc story-fitting.
- **Blind/automated grading** where the researcher is also a subject (self-subject bias — our own risk; [CS] flags
  observer bias).
- **Triangulate** ([CS] §2.3.3) and **log raw data append-only** (`research/RAW-DATA.md`) so the chain from data →
  claim is auditable.

## 5. Using the books in practice
- **Start at the summary** ([`summary-case-study-research.md`](summary-case-study-research.md) /
  [`summary-experimentation.md`](summary-experimentation.md)) — it carries the structure + section pointers and is safe
  to link from anywhere public.
- **When the summary isn't deep enough, Read the real book** from the closed path (as we did for §3.2.3). Use `Read`
  with a page range; find a chapter's page from the TOC first. Then **cite section, not text**, and own BR's
  co-authorship inline.
- Keep the summaries' section pointers in sync if a book edition's numbering is checked and differs.

## Cross-links
[`summary-case-study-research.md`](summary-case-study-research.md) · [`summary-experimentation.md`](summary-experimentation.md) ·
`research/029-cross-model-psyche-comparison.md` (the live experiment-design case) · `research/033-learning-barrier-rqs.md` ·
`research/METHODOLOGY.md` (WR's own method) · `research/experiments/` (worked example) · `docs/foundations.md`
("Roles and cases", validity terms) · `skills/blog-assistant` (reporting/echt).
