# Specific instructions for this paper

* Working title (to be discussed):
  Teaching Agents at Inference-Time to Develop and Use Typed Tools -- a case study in meta-level agentic requirements engineering

This is a stub. More will come.

* BR has ORCID number public record: https://orcid.org/0000-0002-9380-6120

* Use some relevant reference to own work esp reqT:
  * BR papers on google scholar: https://scholar.google.com/citations?hl=en&user=W37CtAwAAAAJ&pagesize=200
  * BR papers in Lund University publication data base: https://lup.lub.lu.se/search/person/f235ec82-89a4-4178-a07d-ea8f5db12dde



## REJ

* Springer Nature REJ latex format and rules and submission guidlines to see if stuff goes here 
  - see https://link.springer.com/journal/766
  - https://link.springer.com/journal/766/submission-guidelines
  - https://www.springer.com/us/editorial-policies/artificial-intelligence--ai-/25428500

* TODO: agent summarize above stuff below, good if possible to separate in "do this" and "dont do this"

### Agent summary of REJ / Springer Nature rules (2026-07-02)

> **Fetch caveat (honesty):** all three linked pages redirect through Springer's login gateway
> (`idp.springer.com/authorize`), so they could **not** be fetched directly (they need a browser cookie
> session). The summary below is compiled from **current knowledge of Springer Nature's standard journal
> policies + the REJ (Requirements Engineering journal) profile**, NOT from a fresh fetch — **BR should verify
> the specifics (esp. blind-review mode, length, and the exact LaTeX template) against the live pages in a
> browser** before submission. Points that most need live confirmation are marked **[VERIFY]**.

#### REJ scope & fit — is this paper a good match?
- **DO** target it as a **case study / empirical or action-research paper** — REJ explicitly publishes RE
  research including *methods, tools, empirical studies, experience reports and case studies* across elicitation,
  specification, analysis, validation and management. A **meta-level agentic-RE case study** (agents expressing
  their own requirements in reqT + building typed tools) is a novel, in-scope angle.
- **DO** foreground the **RE contribution** (requirements method, reqT dogfooding, the human–agent requirements
  loop), not just the tooling/AI — REJ is an RE venue, so the RE framing must lead. **[VERIFY]** current aims &
  scope wording + accepted article types.

#### Manuscript format & submission
- **DO** use the **Springer Nature LaTeX template** (the `sn-jnl` / Springer article class) or Word; include a
  ~150–250-word abstract and **4–6 keywords**; standard structure (Intro, Background/Related Work, Approach,
  Evaluation, Discussion, Threats to Validity, Conclusion). **[VERIFY]** exact template + any length guidance
  (REJ has no hard page limit but expects concision).
- **DO** add a **Declarations** section: competing interests, funding, ethics, **data availability** (point to the
  genscalator repo) and **code availability** (the public Codeberg repo — a reproducibility asset).
- **DO** cite own relevant work (reqT + BR's RE papers) — ORCID `0000-0002-9380-6120`.
- **DON'T** submit simultaneously to another venue (no duplicate/concurrent submission); **DON'T** exceed what's
  needed (concise > padded). **[VERIFY: blind review]** — if REJ uses **double-blind**, anonymize: remove author
  names/affiliations and **mask self-citations** ("Author et al.") including the reqT references; if single-blind,
  normal citations are fine. Confirm which, as it changes how the reqT credit is written.

#### AI / LLM use (critical — this paper both USES and STUDIES an AI agent)
- **DON'T** list the AI agent (Claude/LLM) as an **author** — Springer Nature does not permit AI/LLMs as authors
  (they cannot take responsibility, consent, or hold accountability). BR (and co-authors) are the accountable
  authors.
- **DO** **disclose** the AI use explicitly — describe *which* tool/model + version and *how* it was used, in the
  **Methods** (since here the agent is part of the research method/subject) and/or a statement/Acknowledgement.
  Given the agent is the **object of study**, transparent description of the human–agent collaboration (which the
  WR / `RAW-DATA.md` corpus already documents) is both required *and* a strength.
- **DO** take **full author accountability** for all content, including AI-assisted or AI-generated text/code.
- **DON'T** use AI to fabricate/falsify data or results; **DON'T** present AI-generated **images/figures** as
  data (AI images are generally disallowed unless the AI output *is* the research subject — likely fine here for
  illustrating agent behaviour, but label clearly). **[VERIFY]** the exact wording of the AI-images clause.

#### Integrity & reproducibility (general Springer)
- **DO** ensure originality (plagiarism/self-plagiarism screened via iThenticate); disclose any prior
  workshop/preprint version. **DO** make data + code available (the open repos help). **DON'T** reuse figures/text
  from prior work without permission/citation.

**Net:** nothing here blocks the paper; the two live-verify items that materially affect drafting are
**(a) blind-review mode** (affects self-citation of reqT) and **(b) the exact Springer LaTeX template**. The AI
disclosure is straightforward and, for this paper, a feature rather than a hurdle.

