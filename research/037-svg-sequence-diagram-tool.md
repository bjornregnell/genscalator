# `tt svg --sequence-diagram` — a textual-spec to SVG renderer

Status: **shipped v1** (2026-07-05). Tool: [`../tools/svg.scala`](../tools/svg.scala); tests in
[`../tools/test/cli.test.scala`](../tools/test/cli.test.scala) (7 CLI-contract cases). BR's idea (2026-07-05): a `tt svg *`
tool focused on `--sequence-diagram`, input a textual representation, output "nice SVG to be used in blogs and
also in other reports to human", useful "in advanced debugging or when we talk about design decisions when we
code together".

## Why a bespoke spec, and NOT reqT-lang

BR asked to *try reqT-lang for the input, and if reqT concepts are lacking, put what I need into a Spec in a
standard format I decide*. They are lacking, in a specific and instructive way:

- **reqT is conceptually a *bag* of requirements.** A reqT model is a collection of entities, relations, and
  attributes whose **order is not semantic** — `Feature: a requires Feature: b` is a *fact that holds*, with no
  "before / after" *in the formal model*. *(Correction, BR 2026-07-05: reqT-lang's **parser preserves source order
  and never rearranges** elements — BR added this on **user feedback**. And a further nuance (BR): that order **is
  meaningful — to the human, not to the model**. It carries no *formal / model* semantics (the requirements bag
  computes the same either way), yet it is genuine **spatial-navigation memory** for the reader — exactly like the
  order of **definitions in a program**: coders remember *where* a thing is, and a tool that reshuffled defs would
  be maddening. So "order is not semantic" means "**not part of the formal model**", NOT "meaningless".)*
- **A sequence diagram is a LIST in time.** Its whole meaning is the **order**: message 1, then 2, then 3, along
  per-actor lifelines, possibly with activations and loops. Order is the payload, not decoration — and there is a
  first-class notion of a **message / interaction** between two lifelines that reqT simply doesn't have.
- So the mismatch is **not** that reqT would *lose* the order (it wouldn't — it preserves source order). It is that
  you'd be leaning on an ordering the **model** gives no *formal* meaning to (even though the human reads meaning
  into it), with **no message concept**, so nothing downstream could rely on "message 3 follows message 2" *as
  model semantics*. Smuggling it in via an index attribute
  (`order: 1`, `order: 2`) is a temporal model bolted onto a non-temporal language — reqT syntax without reqT
  semantics.

So the spec is a **tiny purpose-built DSL**, deliberately **PlantUML / mermaid-flavoured** — a de-facto standard
that readers (and models) already know, so there is nothing new to learn. This is the honest version of "a std
format you decide": pick the one the world already converged on for *this* shape of data.

(Generalisable rule worth keeping: **match the notation's algebra to the data's algebra.** reqT is the right
tool for requirement *structure*; it is the wrong tool for *temporal order*. Reaching for the in-house language
everywhere is a hammer-nail trap.)

## The spec (v1)

One statement per line; blank lines and `#` / `//` comments ignored:

```
title: <text>                     optional, centred at the top
actor <Id> [as <label>]           declare a lifeline (also: participant); label may be "quoted"
<A> -> <B>: <message>             solid arrow, filled head   (a call / synchronous message)
<A> --> <B>: <message>            dashed arrow, open head     (a return / reply / async)
note over <A>[, <B>]: <text>      a note box over one or two lifelines
```

Lifelines not pre-declared are auto-created in first-seen order. A self-message (`A -> A`) draws a loop. Output
is a **self-contained** SVG (inline `<style>`, no external refs — safe to inline in an SSG page or an artifact)
and **theme-aware**: default `auto` (light plus a `prefers-color-scheme: dark` override), or a fixed `--light` /
`--dark` palette for predictable embedding (see Design choices).

## Sibling: `tt ascii` (shared spec)

`tt ascii --sequence-diagram` renders the **same spec** to a monospace/box-drawing diagram (terminals, PR/commit
comments, plaintext reports). The grammar + parser were extracted into `tools/seqspec.scala` (a no-`@main` shared
helper, like `lib.scala`) so `svg` and `ascii` share ONE definition of the spec — add a spec feature once, both
tools get it. `ascii` defaults to Unicode box-drawing glyphs for looks, with `--pure` for strict 7-bit ASCII.

## Design choices

- **No dependency.** Pure string generation over the JDK (`java.nio`), like `htmltext` — no graphics library, no
  headless browser. Text metrics are *estimated* (≈0.62·fontSize per glyph); good enough for box sizing, and it
  keeps the tool a cold-start-cheap pure tool.
- **Explicit-polygon arrowheads**, not SVG `<marker>` — markers with `currentColor` are finicky across renderers;
  a computed triangle (filled, for a call) or open "V" (for a return) is robust.
- **Three theme modes** (BR's call, 2026-07-05): default **`auto`** = a CSS-variable palette with a
  `prefers-color-scheme: dark` media query (one file adapts to the viewer); **`--light`** / **`--dark`** = a *fixed*
  palette, no media query. The tailored modes exist because `auto` tracks the **OS** setting, not the **host page's**
  theme, so an auto SVG can mismatch a light SSG page on a dark-OS machine (or a PDF export, or an image viewer).
  BR's hunch — "hard to be dark/light generic" — is right: for a page that *commits* to a theme, generate the
  matching variant; keep `auto` for standalone viewing. Cheap to offer both, so we do.
- **Opaque background by default** (BR, 2026-07-05), `--transparent` to opt out. The background is a full-canvas
  `<rect>` filled with a per-theme `--bg` var (white for light, near-black for dark; adaptive under auto). Default
  opaque because a *transparent* SVG background often renders badly — a Markdown/GitHub viewer or a local SVG app
  shows its own checkerboard/dark canvas *through* the diagram (BR hit exactly this). Transparent is still one flag
  away for when the embedding page's own background is wanted.
- **XML-escaped** every label (`& < > "`), locked by a well-formed-XML test that parses the output with
  `DocumentBuilderFactory` — a defence against an unescaped `&` in a message silently breaking the SVG.

## Use cases

1. **Blog figures** (blog-assistant §7 wants *real* figures): render an actual protocol — the compact dance, the
   note→pin pipeline, a confirmation-guard round — as a crisp diagram instead of a wall of prose. First real
   artifact: [`../blog/figures/seq-compact-dance.svg`](../blog/figures/seq-compact-dance.svg) (source spec
   alongside it, so it is regenerable). Candidate figure for blog 005 (*dancing with agents*).
2. **Design discussions while coding together** — sketch a proposed message flow in a few lines, render, look at
   it, argue about it. Cheaper than a whiteboard and it version-controls.
3. **Advanced debugging** — reconstruct an observed call/return sequence (who called whom, what came back) into a
   diagram to reason about an ordering bug or an unexpected round-trip.

## Roadmap (not built yet — YAGNI until a real spec needs it)

- Activation bars (lifeline "busy" rectangles) — needs an activate/deactivate or auto-nesting rule.
- `alt` / `loop` / `opt` combined fragments (boxed groups).
- Other `tt svg` modes behind the same dispatcher (`--flow`, `--state`) if a real need shows up — the tool is
  named `svg` with a mode arg precisely to leave that room, but v1 ships only `--sequence-diagram`.
- Wrap-long-labels (currently a very long message widens the whole column). Fine for now; revisit if it bites.

## Upstream: contribute the generators back to reqT (BR, 2026-07-05)

**Eventually contribute this SVG generation (and the HTML/text generation, e.g. `htmltext`) back to reqT** once it
is well-tested. reqT already ships `GraphvizGen`, `HtmlGen`, and `LatexGen` (see `tools/reqt-vendored/05-*.scala`),
so an SVG diagram generator is a natural sibling upstream. **Gate: only contribute a *well-tested* artifact** — the
CLI-contract + well-formed-XML tests are the start of earning that. Note the design boundary this tool already
established (reqT is conceptually a *bag* — requirement order isn't semantic, even though reqT-lang preserves source
order; a sequence's order IS its meaning) — an upstream contribution should either add an ordered/interaction
concept to reqT or keep the sequence spec as a companion notation, not lean on reqT's incidental element order. Track with the reqT-lang relationship in [`015-reqt-lang-review.md`](015-reqt-lang-review.md).
