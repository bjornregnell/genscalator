# Braceful, braceless, or the common style?

> **Status: initialized 2026-07-03 (outline).** Slot for the Scala-syntax post: braces vs significant
> indentation, judged not only by human taste but by **agent edit-cost**. Outline below; to be drafted.
> **Audience:** Scala developers weighing a Scala-3 style policy; language designers / SIP folk; builders of
> agentic coding tooling; anyone setting a codebase style convention for AI-assisted development.
> Sources: `research/scala-style-evolution.md`, `research/scala-style-recommendations.md`, the genscalator
> **indent-vs-braces edit-cost experiment** (`research/experiments/`), and the Scala SIP-committee context.

## The question
Scala 3 ships two surface syntaxes for the same language — **braceful** (`{ }`) and **braceless**
(significant indentation). Which should *agent-authored and agent-edited* code use? And is there a **common
style** that serves humans and agents at once, rather than forcing a camp? *(to draft)*

## Outline

### 1. Two syntaxes, one language
- braceful `{ }` vs braceless (significant indentation); Scala 3 supports both, and they compile identically.

### 2. The usual debate is about human readability
- taste, diff noise, nesting clarity, copy-paste robustness — well-trodden, and mostly inconclusive-by-taste.

### 3. The new axis — agent edit-cost
- genscalator's experiment measures the **token / edit cost for an agent to *modify*** code in each style.
- Finding (Run 1+2): braceless is **costliest in aggregate**, but the effect is **bidirectional per model**
  and **dominated by the model's ability to *emit* that style** — not by any intrinsic merit of the syntax.
- Implication: "which style is cheaper" is partly a fact about the *model*, not the *language* — and it moves
  as models change.

### 4. "The common style" — the synthesis
- A disciplined **common subset / convention** that reads well for humans *and* is emitted + edited reliably by
  agents — rather than a dogmatic all-braces or all-indent. *(develop: what exactly the common style is —
  where braces earn their keep, where indentation is safe, and the anchoring rules that make edits robust.)*

### 5. Why this matters beyond Scala
- Language + style design for a world where **agents are primary authors/editors**: readability is now a
  *two-audience* problem. The SIP-committee angle — experimental dogfooding of these tradeoffs is deliberate.

## Close
*(to draft)*
