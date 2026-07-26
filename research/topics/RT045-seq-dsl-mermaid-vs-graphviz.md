# 045 — Should the sequence-diagram DSL mirror a Mermaid subset?

- **Question:** our sequence-diagram DSL (`seqspec.scala`, shared by `tt svg` / `tt ascii` / `tt gvdot`) is currently
  *bespoke*. Should its authoring syntax instead be a **subset of Mermaid's `sequenceDiagram` language** — so a spec
  file is *also* valid Mermaid and renders anywhere Mermaid does — or is a "graphviz-subset" the better thing to
  mirror? **Deliverable: a quick tradeoff + recommendation.** (Solo-menu item, from BR.)
- **Why it matters:** DSL choice is a portability + review-overload lever. If specs are valid Mermaid, they render in
  GitHub/GitLab/VS Code/Obsidian *for free*, and BR/readers already know the syntax — while our own renderers stay
  value-add (offline, themeable, opaque background, ascii, graphviz). It also decides how much bespoke grammar we own
  and must maintain.
- **Plan (constraint from BR):** compare a **syntax subset only** — do **NOT** install or call Mermaid.js. Weigh
  Mermaid-subset vs graphviz-subset vs status-quo-bespoke against: portability, authoring ergonomics, migration cost
  of the existing parser + the committed `.txt` figures, and the "one grammar we control" cost.
- **Status:** open (investigation drafted 2026-07-06; awaiting BR steer — this changes the DSL + existing figures, so
  it is a decision, not a solo job).

## Current DSL (ground truth, `seqspec.scala`)
```
title: <text>                     optional
actor|participant <Id> [as <label>]
<A> -> <B>: <message>             solid arrow  (call)
<A> --> <B>: <message>            dashed arrow (return/async)
note over <A>[, <B>]: <text>
# and // comments; blank lines ignored; lifelines auto-created first-seen
```

## The three options

### A. Mermaid `sequenceDiagram` subset
Mermaid's sequence sublanguage is close to ours but **not identical**:
- **Arrows differ in the load-bearing spot.** Mermaid `->` = solid line **no arrowhead**; the arrowhead form is
  `->>` (solid) / `-->>` (dashed). Ours uses `->` / `-->` *with* arrowheads. So "become a Mermaid subset" = **migrate
  our arrow tokens** to `->>` / `-->>` (and re-emit the committed `.txt` figures).
- **Declarations match:** `participant X as Label` / `actor X` are shared verbatim.
- **Notes match:** `Note over A,B: text` (Mermaid also has `Note left of` / `Note right of`).
- **Title differs:** Mermaid takes the diagram title from **YAML frontmatter** (`---\ntitle: ...\n---`) or an
  `autonumber`-style directive, not a `title:` line. A subset would either drop titles or adopt the frontmatter form.
- **Wrapper line:** Mermaid needs a leading `sequenceDiagram` keyword. A subset parser would accept (and ignore) it;
  our emitters would add it so output is drop-in Mermaid.
- **Rich blocks we don't have:** `loop` / `alt` / `opt` / `par` / activation (`+`/`-`). Out of subset scope for now,
  but adopting Mermaid's spelling leaves a **compatible growth path** if we ever want them.
- **Pro:** zero-lib portability (renders in GitHub, GitLab, VS Code, Obsidian, mermaid.live), familiar syntax, a
  superset target we can grow into. **Con:** one-time migration of arrows + title + figures; we no longer fully own
  the grammar (Mermaid's evolution constrains us, though a *subset* insulates us).

### B. Graphviz "subset"
Graphviz **DOT is a graph language, not a sequence language** — there is no standard sequence-diagram sublanguage to
mirror. `tt gvdot` *renders* our spec **to** DOT (rank/edge tricks); it is a **backend**, not an authoring surface.
"Author in graphviz" would mean hand-writing verbose DOT with manual ranking — worse ergonomics, no portability
benefit, and it re-introduces exactly the low-level noise the DSL exists to hide. **Verdict: not a real authoring
option;** graphviz stays a render target, not the DSL we mirror.

### C. Status quo (bespoke)
Keep our own minimal grammar. **Pro:** zero migration, full control, already tested. **Con:** specs render *only*
through our tools; no free ecosystem rendering; readers must learn one more micro-syntax.

## Preliminary finding / recommendation (for BR)
The meaningful choice is **A (Mermaid-subset) vs C (bespoke)** — "graphviz-subset" collapses into "keep bespoke,
render via DOT". **Lean: A, a Mermaid-compatible subset**, because portability is a real payoff (specs become
first-class in every Markdown host BR already uses) and Mermaid is a *superset* we can grow into without repainting
ourselves into a corner — while our renderers keep their differentiators (offline, themeable, opaque-bg, ascii,
graphviz). The cost is bounded and one-time: change two arrow tokens in `seqspec.scala`, decide title handling
(frontmatter vs keep a tolerant `title:`), accept+ignore a leading `sequenceDiagram`, and re-emit the committed
`.txt` figures. A **backward-tolerant parser** (accept both `->` and `->>` during transition) would make the
migration incremental and non-breaking.

Ties to `037-svg-sequence-diagram-tool.md` (why the spec is bespoke, not reqT-lang) and `046-reqt-lang-md-diagram-sources.md`
(a *different* axis — the file *container*, not the sequence *syntax*; the two decisions compose).

## What shipped
Nothing yet — investigation note only; the DSL + figures are unchanged pending BR's decision.
