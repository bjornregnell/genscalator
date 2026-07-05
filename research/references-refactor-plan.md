# TODO + plan sketch — refactor References.scala (separate rendering logic from data)

- **Status:** TODO, **pinned (BR 2026-07-05)**. Not urgent — *"we get there when we see the need."* But flagged now
  because `blog/References.scala` is growing and increasingly **mixes logic with data**, which is the smell that
  triggers the refactor.
- **Why it matters:** References.scala currently holds **both** the data (the model types + the big `references` Seq of
  reference literals) **and** the rendering logic (the `toMarkdown` / `toHtml` / `toBibTex` extension methods). As the
  reference list and the renderers both grow, one file mixing the two gets hard to read, review, and reuse.

## The move (plan sketch — refine before executing)
1. **Lift each render extension into a standalone function** — e.g. `def mkMarkdown(r: Reference): Markdown` (and
   `mkHtml`, `mkBibTex`), instead of `extension (r: Reference) def toMarkdown`. Pure `Reference => output` functions are
   easier to test, move, and call from elsewhere.
2. **Consider a separate rendering module** — e.g. `Markdown.scala` (or a `render/` module) that owns the
   `Reference => Markdown/Html/BibTex` functions, leaving `References.scala` as **data + model types only** (data ‖
   logic split).
3. **Decide the home:** this rendering logic is very likely **an SSG resource** — the static-site generator for
   `bjornregnell.se` will need exactly "render a `Reference` (and a post) to HTML". So the refactor may **fold into the
   SSG case study** (`research/ssg-scoping.md`) rather than living in `blog/`. Decide data/logic boundaries *with* the
   SSG design, not before it.

## Open questions
- Separate `Markdown.scala` module **now**, or wait until the SSG forces the boundary? (Lean: wait — refactor when the
  SSG needs it, so the split matches the SSG's actual shape.)
- Should the model types (`Reference`, `Summary`, `RefData`, the Iron aliases) move to their own `model` file too, so
  data-types / data-values / rendering are three clean pieces?
- Does the BibTeX/HTML rendering want the same **exhaustive-match** discipline the Markdown renderer has (so a new
  `Summary` case is a compile error everywhere)?

## Trigger to act
When adding a renderer branch or a reference **feels cramped** in one file — or when SSG work starts and needs a
`Reference => Html` entry point. Until then: parked-but-pinned.
