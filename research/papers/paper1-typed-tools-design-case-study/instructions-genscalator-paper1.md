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

> **Source:** extracted from the saved *Submission guidelines — Requirements Engineering* page (BR's Firefox
> "Save Page As", committed in this folder). Confirmed facts below (no longer guesses). The Springer LaTeX
> template is present in [`sn-article-template/`](sn-article-template/).

#### ✅ DO
- **Format: LaTeX, `\documentclass{sn-jnl}`** (Springer Nature template, encouraged) — you have it in
  `sn-article-template/`. Word (`.docx`) is also accepted. **Always submit editable source files** at every
  submission/revision, or the article is **not** reviewed.
- **Abstract 150–250 words**, with no undefined abbreviations or unspecified references.
- **4–6 keywords** (for indexing).
- **Title page (separate, NOT in the anonymized manuscript):** title (concise, informative), author name(s),
  affiliation(s), corresponding author + active email, and the **16-digit ORCID** (BR: `0000-0002-9380-6120`);
  put acknowledgements/disclosures/funding here too.
- **"Statements and Declarations"**: provide the required declarations; **Author Contributions** and **Competing
  Interests** are entered **via the submission-system interface** (the new SNAPP system), not the manuscript —
  only interface-entered info appears in the published version. Keep them at hand. Manuscripts missing required
  declarations are **returned as incomplete**.
- **Document the LLM/agent use in the Methods section** — REQUIRED here. (The exemption for "AI-assisted copy
  editing" covers only readability/grammar fixes to human-written text; it explicitly **excludes** "generative
  editorial work and autonomous content creation" — which is exactly what our agent does, so it must be
  disclosed.) This is a strength: the agent *is* the study, and the WR/`RAW-DATA.md` corpus is the raw evidence.
- **Frame as an RE contribution** (the reqT dogfooding + human–agent requirements loop leads), suited to REJ's
  empirical / case-study / experience-report scope.
- **Get permission** for any reused figures/tables/text passages already published elsewhere.

#### ⛔ DON'T
- **DON'T reveal author identity — REJ is DOUBLE-BLIND (double-anonymous).** Remove all author names,
  affiliations and identifying info from the manuscript **and all accompanying files** (incl. figures/supplementary).
  Crucially: *"Authors should avoid citing their own work in a way that could reveal their identity"* — so the
  **reqT + BR self-citations must be written in a non-identity-revealing way** (neutral third person; avoid "in
  our prior work…"; don't let the citation pattern out you). This is the single biggest drafting constraint.
- **DON'T list the LLM/agent as an author** — LLMs (ChatGPT/Claude) do **not** satisfy authorship criteria,
  because authorship carries accountability that cannot apply to an LLM. Humans remain fully accountable for the
  final text and must agree it reflects their original work.
- **DON'T submit if published before or under consideration elsewhere** (submission implies neither); co-author +
  institutional approval implied.

#### Two previously-open items — now RESOLVED
- **Blind review = DOUBLE-BLIND** → anonymize everything + write the reqT self-citations to not reveal identity.
- **Template = `sn-jnl`** → in `sn-article-template/`.

**Net:** nothing blocks the paper. The load-bearing drafting constraint is the **double-blind self-citation of
reqT**; the AI disclosure (LLM use in Methods, no LLM author, human accountability) is straightforward and, for
this paper, a feature.

