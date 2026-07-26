# 046 — reqT-lang `.md` files as diagram sources

- **Question:** today each sequence-diagram spec lives in a plain `.txt` file (e.g. `blog/figures/seq-compact-dance.txt`)
  that the renderers read verbatim. Should we instead store specs as **`.md` files containing valid reqT-lang**, where
  a diagram is a reqT element carrying its DSL in a `Spec` attribute, so the source is a **parseable reqT element**
  (`tt parsereqt`-checkable) rather than an opaque blob? **Deliverable: what it buys vs costs + a lean.** (Solo-menu
  item, from BR.)
- **Why it matters:** genscalator already **dogfoods reqT-lang for its own PRD** (structure-beats-prose thesis,
  `015-reqt-lang-review.md`). If diagram sources are reqT too, we get **one grammar** across the substrate, a lint
  gate (`tt parsereqt`), and co-located typed metadata (title, id, intended renderers, description) instead of a
  bare stream of DSL lines.
- **Plan:** sketch the reqT shape, name what it buys and costs, and resolve the apparent tension with
  `037-svg-sequence-diagram-tool.md` (which argues a sequence should *not* be modeled as reqT because reqT is a
  *bag* — order not semantic — while a sequence's order *is* its meaning).
- **Status:** open (investigation drafted 2026-07-06; awaiting BR steer).

## Proposed shape
```
* Image: someCoolDiagram has
  * Title: "The compact dance"
  * Spec: <<
      title: The compact dance
      participant H as Human
      participant A as Agent
      A -> H: (1) save + resume prompt
      H -> H: (3) compact
    >>
```
(Exact reqT surface TBD — `Image`/`Spec`/`Title` are illustrative element+attribute names; the key move is that the
**DSL text lives inside one `Spec` string attribute** of a typed reqT element, with sibling attributes for metadata.)

## Resolving the tension with 037 (important)
037's objection — "don't model a sequence as reqT, reqT is an unordered bag" — **does not apply here**, and naming why
is the crux of this note: we are **not** modeling the sequence *as* reqT elements (one element per message, whose
order reqT wouldn't preserve). The **entire ordered sequence stays an opaque string** inside a single `Spec`
attribute. reqT's bag-nature never touches the ordered content; the reqT layer only provides a **typed container +
metadata**, and the *sequence semantics live wholly inside the string* (parsed by `seqspec.scala`, order intact). So
037 (don't reqT-model the sequence) and 046 (do reqT-*wrap* the sequence file) are **compatible** — different layers.

## What it buys
- **One grammar / one lint gate.** The container is `tt parsereqt`-checkable; a malformed diagram *catalog* fails
  fast, consistent with the PRD dogfooding.
- **Typed, co-located metadata.** Title, stable id, intended renderers (svg/ascii/gvdot), a description, cross-links to
  the PRD/foundations term the figure illustrates — as reqT attributes, not naming conventions.
- **Catalogs.** One `.md` can hold *many* `Image` elements (a whole figure set for a blog post), each independently
  addressable — nicer than N loose `.txt` files.
- **Substrate consistency.** Diagrams become first-class reqT model elements, greppable/queryable like the rest.

## What it costs
- **An extraction step in every renderer.** `tt svg/ascii/gvdot` currently `read` a `.txt` and parse. Now they must
  parse reqT, select the `Image` element, pull its `Spec` string, then feed `seqspec`. Cleanest fix: a small
  `tt reqt extract-spec <file> <id>` (or a shared helper) that emits the raw DSL to stdout, keeping the renderers
  unchanged and single-purpose. That is a new tool to design/test.
- **Hand-edit friction.** A `.txt` is trivially editable by any human/tool; the reqT wrapper adds indentation +
  `<<...>>` block rules to get right. Mild, but real for quick one-off edits.
- **Indirection for single-diagram files.** For one diagram the wrapper is overhead; the payoff concentrates in
  **catalogs** and in the **metadata/lint** value, not in the 1-diagram case.

## Preliminary lean (for BR)
**Adopt the reqT container for diagram *catalogs* (a figure set for a post), keep bare `.txt` acceptable for one-off
single diagrams** — i.e. make renderers accept *either* a raw DSL file *or* a reqT file (via an extract step), rather
than forcing all figures through reqT. That captures the metadata/lint/one-grammar win where it pays (multi-figure
sets) without taxing the quick single-diagram case, and it composes with 045 (the DSL *syntax* decision) since the
`Spec` block's contents are just whichever DSL 045 settles on. Gate any renderer change on the shared extract helper
being built + tested first.

Ties to `015-reqt-lang-review.md` (MAP-not-FORK), `037-svg-sequence-diagram-tool.md`, `038-tt-shared-helper-file-pattern.md`
(the `seqspec` shared-helper pattern an extract step would join), and `045-seq-dsl-mermaid-vs-graphviz.md`.

## What shipped
Nothing yet — investigation note only.
