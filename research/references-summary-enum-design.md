# Design-validation feedback: the `Summary` enum (blog/References.scala)

BR proposed a structured `Summary` enum (PaperSummary / BookSummary / OtherSummary) "to validate the design (might
evolve)." I implemented it, wrote a render extension + tests, and populated demo summaries. This note reports **where
the schema strained** — the actual deliverable. Verdict up front: **the enum is a clear win over the refined String;**
the strains below are refinements, not reasons to abandon it.

## What worked cleanly
- **`BookSummary(topic, chapterHeadings: Seq[NonBlank])`** — no strain; a book's TOC is genuinely a list.
- **`OtherSummary`** as the escape hatch — *validated by use*: ELIZA (a 1966 system paper) and "Machine behaviour" (a
  Nature manifesto) have no RQ/method/results/validity, so forcing them into PaperSummary would have been false
  structure. Populating them as OtherSummary is exactly why the case must exist.
- **The `RefKind` ↔ `Summary`-case alignment** is natural (Journal/Conference/Preprint → PaperSummary; Book →
  BookSummary; Web/Misc → OtherSummary), so choosing a case is rarely ambiguous.

## Where it strained (decisions for BR)
1. **`abstract` is a Scala reserved word** → I backticked it (`` `abstract` ``). Works, but every construction/pattern
   touches backticks. *Decide:* keep backticks, or rename (`synopsis` / `abstractText`).
2. **NonBlank fields can't be built by `+` or `.stripMargin`** — Iron auto-refines only *literals*, so a long summary
   must be a **single string literal** (no concatenation, no `stripMargin`). Awkward for multi-paragraph text. *Options:*
   (a) relax the fields to plain `String` and validate via a smart constructor that `.refineUnsafe`s the built value;
   (b) accept single-literal ergonomics; (c) a `nb("...")` helper that refines a runtime string. **This is the main
   ergonomic strain.**
3. **PaperSummary's 5 fields assume a controlled/empirical study.** Fine for Binz&Schulz or Sharma; awkward for
   guidelines/position papers. Borderline case: Wohlin's snowballing paper is *guidelines + a replication* — half-fits.
   *Sub-question:* is **`validity`** too SE-specific? Many non-SE papers report no explicit validity/threats section —
   you'd write "authors report none," which is honest but forced.
4. **`researchQuestions` I widened `String → Seq[NonBlank]`** (plural is right; parallels `chapterHeadings`). But an
   **empty Seq is currently allowed** (a paper with no explicit RQs). *Decide:* allow empty, or require ≥1 via
   `Seq[NonBlank] :| MinLength[1]`.
5. **Summaries are content-claims → they carry the same false-echt risk as citations, but are NOT gated.**
   `RefVerification` gates the *citation data*; nothing gates whether a PaperSummary's "results" line is accurate. A
   `Verified` ref can still carry a hallucinated summary. **Biggest open question:** should the summary inherit the
   ref's verification, carry its own marker, or should populating a summary require the same grounding pass the
   citations got? (This session's 2 grounded PaperSummary/BookSummary demos went through a fetch agent precisely to
   avoid confabulating — that discipline should be encoded, not ad-hoc.)
6. **Renderer coupling (expected, noted):** `toMarkdown`/`toHtml`/`toBibTex` don't yet special-case the structured
   summary in BibTeX/HTML the way `toMarkdown` does; and adding a 4th `Summary` case later means updating every renderer
   that matches on it. That's the normal enum tradeoff — exhaustive `match` will flag the unhandled case at compile
   time, which is a feature.

## Concrete recommendation
Keep the enum. Resolve (2) first (it's the daily ergonomic cost) and (5) next (it's the echt-integrity gap). (1)(3)(4)
are small polish calls. If summaries become common, a `summaryVerification` marker (or reusing `RefVerification` at the
summary level) is worth adding so "grounded summary" is a first-class, greppable state — mirroring how `ToDo`/`Verified`
already work for the citation itself.
