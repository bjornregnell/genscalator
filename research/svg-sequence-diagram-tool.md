# `tt svg --sequence-diagram` — a textual-spec to SVG renderer

Status: **shipped v1** (2026-07-05). Tool: [`../tools/svg.scala`](../tools/svg.scala); tests in
[`../test/cli.test.scala`](../test/cli.test.scala) (7 CLI-contract cases). BR's idea (2026-07-05): a `tt svg *`
tool focused on `--sequence-diagram`, input a textual representation, output "nice SVG to be used in blogs and
also in other reports to human", useful "in advanced debugging or when we talk about design decisions when we
code together".

## Why a bespoke spec, and NOT reqT-lang

BR asked to *try reqT-lang for the input, and if reqT concepts are lacking, put what I need into a Spec in a
standard format I decide*. They are lacking, in a specific and instructive way:

- **reqT models a SET.** A reqT model is an unordered collection of entities, relations, and attributes — a
  graph. `Feature: a requires Feature: b` is a *fact that holds*, with no "before / after".
- **A sequence diagram is a LIST in time.** Its whole meaning is the **order**: message 1, then 2, then 3, along
  per-actor lifelines, possibly with activations and loops. Order is the payload, not decoration.
- Encoding a sequence in reqT's set model would either **lose the ordering** or smuggle it in via an index
  attribute (`order: 1`, `order: 2`), which is a temporal model bolted onto a non-temporal language — the worst
  of both: reqT syntax without reqT semantics, and a reader who can't see the flow.

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
and **theme-aware** (light default plus a `prefers-color-scheme: dark` override).

## Design choices

- **No dependency.** Pure string generation over the JDK (`java.nio`), like `htmltext` — no graphics library, no
  headless browser. Text metrics are *estimated* (≈0.62·fontSize per glyph); good enough for box sizing, and it
  keeps the tool a cold-start-cheap pure tool.
- **Explicit-polygon arrowheads**, not SVG `<marker>` — markers with `currentColor` are finicky across renderers;
  a computed triangle (filled, for a call) or open "V" (for a return) is robust.
- **CSS-variable palette** with a dark-mode media query, so one file looks right on a light blog and a dark one.
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
