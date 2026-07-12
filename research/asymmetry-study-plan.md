# Plan: mining the asymmetry study (for blog 021)

> **Status: DRAFT for BR review. This is the PLAN only. No data mining or coding runs until BR
> approves and the coding scheme is frozen.** Agent-drafted from knowledge of the CSR book;
> BR (co-author) is ground truth on the methodology. Points marked **[CHECK]** need BR's eye.

## 0. What this plan is (and is not)

It operationalizes blog 021's **RQ1** (what are the differences and similarities between human and
agent) and **RQ2** (does mutual introspection on them improve the joint work) into a repeatable
coding study over our existing workflow-research (WR) corpus. It defines the design, the data
selection, a **pre-hoc coding scheme**, the coding procedure, the validity treatment, and the
human-audit. It does **not** do any coding yet.

**Writing stance (BR 2026-07-12):** the *study* stays methodologically honest, but the *account* (blog 021)
is **accessible, not academic** - we drop the formal case-study-protocol / checklist scaffolding and the
esoteric validity vocabulary, and speak plainly to a developer reader, while staying true to the underlying
rigor. Rigor underneath, plain words on top.

## 1. Research design (CSR framing)

- **Type:** a single-case, longitudinal, **action-research** case study. The researchers are
  participants in the case by design, not outside observers. (CSR is explicit that case study is a
  *flexible* design and that researcher involvement is legitimate if handled openly. **[CHECK]** you
  frame action research vs case study more precisely than I will here.)
- **The case:** the BR + Claude collaboration on genscalator over the logged period.
- **Unit of analysis:** a **WR specimen** = one coherent logged observation or event. Segmentation
  rule: usually one `wr-data/` file section = one specimen; a file with several dated sub-events is
  split per event. Borderline cases are logged for the audit, not silently decided.
- **Logic:** **abductive** - a small set of a-priori categories seeded from RQ1/RQ2, plus room for
  **emergent** categories via constant comparison. The a-priori set is *frozen* before mining; the
  emergent set is append-only and versioned.
- **CSR process steps** we follow: (1) design [this plan], (2) prepare for collection [scheme +
  manifest], (3) collect evidence [already largely done via WR logging], (4) analysis [code ->
  pattern -> abstraction], (5) report [blog 021]. **[CHECK]** step names vs the book.

## 2. Data: our WR-data dance *is* the collection; this study is *selection*

The evidence already exists, and it has a method behind it: our ongoing **WR-data dance** - the practice
of logging a workflow-research specimen the moment a friction, a difference, or an introspection-payoff
shows up *during the real work*. That continuous, participant-embedded logging **is** the data-collection
method (mostly 3rd-degree / archival, plus 1st-degree participant logs, in CSR's degrees-of-data-collection
terms **[CHECK]**). So this study's job is **selection + coding**, not fresh collection.

Because the dance is *ongoing* (the corpus keeps growing as we work), this coding pass fixes a **cutoff**:
we freeze the corpus as of an agreed date and code that snapshot, so the analysis is not a moving target.
Specimens logged after the cutoff feed a later pass.

**Corpora (with triangulation across sources):**

| source | role | degree |
|--------|------|--------|
| `research/wr-data/*.md` | primary - the deliberate WR specimens | 1st/3rd |
| `RAW-DATA.md` | append-only raw excerpts (verbatim moments) | 3rd |
| `PIN-BOARD.md` (HISTORY) | decisions, tasks, mode-shifts | 3rd |
| `git log` across repos | ground-truth timeline / corroboration | 3rd |
| `memory/*.md` | distilled lessons (secondary, corroborating) | 3rd |

**Inclusion criterion:** an item qualifies if it bears on (i) a human-agent **difference**, (ii) a
**similarity**, or (iii) **evidence that introspection** (by either party) into psyche changed the
workflow or a work product - **including where it did NOT help or misfired** (negative/null cases are
in scope, deliberately, to blunt confirmation bias).

**Exclusion:** purely mechanical tooling notes with no psyche/introspection content. Borderline ->
include and flag.

**Sampling:** aim for a **census** of `wr-data/` (it is curated and finite), with targeted pulls from
RAW-DATA / PB / git for triangulation. Exact counts come from a **manifest pass** (step 3.1), which is
inventory only, no coding.

## 3. Pre-hoc coding scheme (v0 - to be frozen after BR review)

**Primary categories (deductive seed):**

- **A. Differences** (human vs agent). Seed subcodes from existing WR: affect (felt vs shaped),
  time perception, continuity / memory (lived vs substrate-external), embodiment, precision,
  duration-distortion mechanism.
- **B. Similarities.** Seed subcodes: both distort felt duration, both helped by structure-over-
  willpower, rot/fatigue analogs, both benefit from member-checking.
- **C. Positive-effect evidence** (bears on RQ2). Subcodes: *whose* introspection (human / agent /
  joint) x *what changed* (behaviour / tooling / substrate / decision) x *outcome* (software quality /
  super-substrate quality / speed / trust).
- **D. Negative / null.** Introspection that did NOT help, misfired, or made things worse. **Kept and
  weighted equally** - it is the guard against confirmation bias: a study that only hunts for wins will always
  find them, so the RQ2 benefit claim is credible only if we also count the misses.

**Per-specimen metadata (coded alongside):** date; who flagged it (human / agent); work-product type
(**software** vs **persistent super-substrate**); directionality (who introspected about whom); source
anchor (file + line/section).

**Emergent categories:** captured in an append-only `emergent-codes` list, each tagged with the
specimen that birthed it. The scheme is revised in **versioned** steps (v0 -> v1 ...); every bump is
dated and justified (constant comparison, audit trail).

## 4. Coding procedure

1. **Manifest pass** - enumerate the corpus, count and list specimens, no coding. Output: a manifest.
2. **Freeze v0 scheme** - only after BR approves this plan.
3. **Code each specimen** -> (categories, subcodes, metadata, a one-line rationale, source anchor),
   written to an **append-only coding log** (`research/asymmetry/coding.tsv` or `.md`).
4. **Capture emergent codes** as they arise; version the scheme.
5. **Human-audit** (section 6).
6. **Analyse -> abstract -> report** into blog 021 (section 5).

## 5. Analysis plan

- **Pattern / frequency** across categories: how much is difference vs similarity; where does
  positive-effect evidence cluster; how many negative/null.
- **Exemplar synthesis:** the strongest specimen(s) per category, quoted, with source anchors (chain
  of evidence).
- **RQ2 table:** whose introspection -> what changed -> outcome, so the benefit claim is inspectable
  rather than asserted.
- **Abstraction:** distil to the handful of findings that "matter" -> the blog's results/conclusions.

## 6. Human-audit / member-check (a real check, not a rubber-stamp)

- BR independently codes a **random 20% sample** of the census against the frozen scheme, **blind** to the
  agent's codes, with a small **floor** (about a dozen specimens) so a small corpus still gets a real check
  rather than two items. (The exact count is fixed after the manifest pass counts the corpus.)
- Compute a plain **percent-agreement per category**, and - more telling - look at *where* and *why* we
  diverge. We keep it plain (percent agreement), not a formal statistic, per the accessible stance.
- Divergences resolved by discussion; a category that will not hold up to a human read is revised or
  dropped.
- **Report the agreement number in blog 021** - it is what makes RQ2 credible.
- BR also **member-checks the final findings** (ground truth on his own interiority; the anthropomorphism
  caution - the agent never asserts the human's inner state as known).

## 7. "Can we trust this?" - validity kept honest, spoken plainly

**Stance (BR 2026-07-12):** the four standard validity categories in the table below - *construct, internal,
external, reliability* - are too esoteric for a developer audience, so we do NOT use those names in the public
account. The blog keeps the plain heading **"Can we trust this?"** and everyday words. But we keep the four
categories as a **private completeness checklist** whenever we review that account, so we never quietly drop a
real threat. The table is that internal checklist; the blog is its plain-language translation.

| type | threat here | mitigation |
|------|-------------|------------|
| **Construct** | vague categories; the agent's own framing shapes what is "seen" | frozen pre-hoc scheme with per-code definitions; BR reviews the scheme before mining |
| **Internal** (causal: introspection -> better work) | single observational case; correlation not cause | treat RQ2 evidence as associational + exemplar-based, NOT proof; actively seek category-D counter-examples |
| **External** (generalizability) | one human, one agent, one project | claim **analytical / transferable** generalization only, stated plainly; not statistical |
| **Reliability** (would another coder agree?) | **the big one** - agent is coder AND participant | human-audit inter-coder check; append-only raw data; a transparent coding log others can inspect; a chain of evidence from claim back to specimen |

- **Reflexivity (action research):** researcher-as-participant is by design; we disclose the agent's
  double role openly (this is the "Can we trust this?" section of blog 021). The human-audit is the guard
  that keeps the self-coding honest.

## 8. Artifacts & where they live

- Home (**BR approved**): a new **`research/asymmetry/`** dir (manifest, frozen scheme, append-only coding
  log, emergent-codes list, analysis notes).
- Optionally a `research/NNN.md` study file with a running **Study log**.

## 9. Decisions (resolved with BR, 2026-07-12)

1. **Human-audit:** a random **20%** of the census, blind, floor about a dozen specimens; plain percent-agreement.
2. **Artifact home:** a new **`research/asymmetry/`** dir.
3. **Category D (negative/null):** kept and **weighted equally** (the confirmation-bias guard).
4. **Form:** **accessible, not academic** - no formal case-study protocol / checklist scaffolding.
5. **Validity vocabulary:** the four CSR categories are an **internal checklist only**; the public account uses
   the plain **"Can we trust this?"** framing and never names construct / internal / external / reliability.

## 10. Scope guard

No mining or coding happens under this document. Mining begins only after BR approves the plan and the
v0 scheme is frozen. This file is the plan; the mountain stays unclimbed until you say climb.
