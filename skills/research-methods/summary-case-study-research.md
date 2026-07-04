# [CS] Case Study Research in Software Engineering — working summary

> **Our own distillation, not the book.** This is an original, from-scratch summary of the *ideas and structure* of
> the book, written to serve as a fast public reference for the genscalator/WR work. It reproduces **no** text, tables,
> or figures from the book. For depth, read the copyrighted original (BR's personal copy lives in the closed repo; buy
> links below).

**Book:** *Case Study Research in Software Engineering: Guidelines and Examples* — Per Runeson, Martin Höst, Austen
Rainer & **Björn Regnell**, Wiley/IEEE Computer Society, 2012. DOI **10.1002/9781118181034**
(<https://onlinelibrary.wiley.com/doi/book/10.1002/9781118181034>).

> **Self-reference (echt).** This book is **co-authored by BR** — when citing it in any genscalator writing, own that
> inline ("our own case-study methodology book"), as both a strength (first-hand authority) and a conflict of interest
> to name openly. BR was one of Claes Wohlin's early PhD students; the same research-methods lineage runs through [EX].

---

## Why this book exists

Software engineering is done by people, in organizations, with tools and processes that you usually **cannot fully
control or isolate**. That rules out a clean controlled experiment for most interesting SE questions. The **case study**
is the strategy for exactly this situation: studying a **contemporary phenomenon in its real-life context**, especially
when the boundary between phenomenon and context is fuzzy. The book adapts the general social-science case-study
tradition (Yin, Stake, Eisenhardt, Runeson-Höst's own guidelines) specifically for SE, and pushes hard on one theme:
a case study is **empirical research, not an anecdote** — its credibility comes from a *designed protocol*, a
*traceable chain of evidence*, and an *explicit validity argument*, not from the story being interesting.

**When to reach for it (vs the alternatives):**
- **Case study** — flexible design, a real phenomenon you can't manipulate, mostly qualitative + mixed data, aiming for
  *understanding* and *analytical generalization* (to theory), not statistical generalization to a population.
- **Experiment** — you *can* control and manipulate a factor and want a causal, quantitative claim → see [EX].
- **Survey** — sample a population to generalize statistically.
- **Action research** — you deliberately *intervene to improve* and study the effect; a close cousin of the case study
  where the researcher is a change agent.

## The five process steps (the spine of the book)

The book frames a case study as a five-step process; the chapters map onto these:
1. **Design** — define objective, cases, units of analysis, theory, RQs, and write the *protocol*.
2. **Preparation for data collection** — instruments, sources, protocol detail.
3. **Collecting evidence** — from multiple sources (triangulation).
4. **Analysis of collected data** — mostly qualitative, chain of evidence.
5. **Reporting** — audience-appropriate, with the validity argument.

The recurring insight: these are **iterative and interleaved**, not a waterfall — flexible design means the RQs and
even the units of analysis can be refined as understanding grows, *as long as you record the changes* (that record is
what protects reliability).

## Case study design (ch. 3 — the part we lean on most)

Design elements (§3.2.x), each of which you should be able to state explicitly:
- **Rationale & objective** (§3.2.1–3.2.2) — why this study, what kind of objective (exploratory / descriptive /
  explanatory / improving).
- **The case and the unit(s) of analysis** (§3.2.3) — *the single most useful distinction in the book.* The **case** is
  the phenomenon-in-context; the **unit(s) of analysis** are what you actually examine within it, and they **change with
  the RQ**. A study can be **holistic** (one unit) or **embedded** (several units inside one case), and **single-** or
  **multiple-case**. Getting this layer right is what keeps a sprawling qualitative study analyzable.
- **Theoretical frame** (§3.2.4) — the lens/prior theory the study builds on or tests.
- **Research questions** (§3.2.5) and **propositions/hypotheses** (§3.2.6) — case studies *can* carry propositions;
  they just aren't statistically tested.
- **Case selection** (§3.2.10) — purposive, not random: you pick cases for what they can teach (typical, extreme,
  critical, revelatory), which is why generalization is *analytical*, not statistical.
- **The protocol** (§3.2.14) — write it down: objective, procedures, instruments, and the plan for analysis. The
  protocol is what makes the study *auditable and repeatable in spirit*, and it is the main reliability instrument.
- **Legal & ethical** (§3.3) — informed consent, confidentiality, handling of sensitive company data.

## Collecting evidence (ch. 4)

- **A taxonomy of data sources** (§4.2), spanning **degree of interaction** (direct methods like interviews and
  observation, where the researcher perturbs the setting, vs independent/archival methods that don't) and **degree of
  control**.
- **Interviews** (§4.3) — structured / semi-structured / unstructured; the funnel model; the workhorse of qualitative
  SE data.
- **Focus groups** (§4.4), **observation** (§4.5, incl. participant observation and think-aloud), **archival data**
  (§4.6 — documents, logs, repositories), and **metrics** (§4.7 — quantitative sources embedded in a qualitative
  study).
- **Triangulation** is the through-line: combine sources (data / observer / methodological / theory triangulation) so a
  claim rests on more than one leg.

*Our WR mapping:* the genscalator/WR data is mostly **archival + observation** — the `research/wr-data/` logs,
`RAW-DATA.md`, and the session transcripts are the primary sources; the researcher is also a participant, which is a
named bias to manage, not hide.

## Analysis & validity (ch. 5 — the credibility chapter)

- **Qualitative analysis** (§5.3) — coding, building explanations, keeping a **chain of evidence** from raw data →
  interpretation → conclusion so a reader can retrace every claim.
- **Validity** (§5.4) — the four-set to argue in *every* case study:
  - **Construct validity** — do the measures/observations actually capture the concept you claim (do interviewees and
    researcher mean the same thing by the terms)?
  - **Internal validity** — for causal/explanatory claims, are there uncontrolled factors confounding the relationship?
  - **External validity** — to what extent can findings be generalized (analytically, to theory / similar contexts)?
  - **Reliability** — would another researcher, following your protocol, reach the same result? (This is where the
    experiment's *conclusion validity* slot is instead filled by *reliability*.)
- **Improving validity** (§5.5) — member checking, triangulation, prolonged involvement, peer debriefing, an audit
  trail. **Quantitative analysis within a case study** (§5.6) for the embedded-metrics parts.

## Reporting (ch. 6) & scaling up (ch. 7–8)

- **Reporting** (ch. 6) — tailor to the audience; include enough of the protocol and chain of evidence that the
  validity argument is checkable; be honest about threats.
- **Longitudinal & multiple-case** guidance (ch. 7) and **synthesis of evidence across studies** (ch. 8, incl. §8.3) —
  how to combine cases and studies without overclaiming.

---

## How genscalator/WR uses this book

- **WR is a case study, not an experiment.** It is a **longitudinal, multiple-case study of the human↔agent workflow**;
  **AT, SSG, and genscalator's own self-development are the cases / units of analysis** (see
  `docs/foundations.md` "Roles and cases"). Longitudinal + multiple-case guidance: ch. 7.
- **We owe an explicit validity argument** in the [CS] four-set (construct / internal / external / **reliability**) —
  reliability especially, because the researcher is also a participant. Our protocol = the preregistrations, the
  append-only `RAW-DATA.md`, and the committed research notes; that audit trail *is* the reliability instrument.
- **Analytical, not statistical, generalization.** An n=1 qualitative observation (e.g. the braceless mis-scope bug) is
  a *real finding about a mechanism*; it does not become a population-level effect. Keeping that distinction honest is a
  recurring theme in the blog (see blog/003).
- Embedded experiments (like `research/experiments/indent-vs-braces/`) sit *inside* the case study — triangulation
  across quantitative and qualitative legs (§2.3.3, §5.6).

## When to read the real thing

This summary is an index, not a substitute. Read the copyrighted original when you need the actual guidance depth —
e.g. the exact design checklist of §3.2, the data-source taxonomy of ch. 4, or the validity-improvement tactics of
§5.5. BR has the book; ask him to pull the full text of a section when depth is needed. **Never** paste the book's
text/tables/figures into this public repo — cite the *section*, not the passage.
