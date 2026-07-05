# The shared-helper-file pattern for cross-tool reuse in the `tt` toolbox

Status: **pattern in use** (2026-07-05). Live examples: `tools/lib.scala` (pure helpers) and `tools/seqspec.scala`
(the sequence-diagram spec model + parser shared by `tools/svg.scala` and `tools/ascii.scala`).

## The pattern

When two or more `tt` tools need the **same typed model / parser / helper**, put it in its own file — an object
with **no `@main`** — and have each tool pull it in with `//> using file <helper>.scala`. Define the shared thing
**once**; every tool that includes it gets the same definition. When a feature is added to the shared spec, all
consumers get it for free (no drift between copies).

```
tools/seqspec.scala   object SeqSpec { case class Diagram…; def parse(...) }   // NO @main
tools/svg.scala       //> using file seqspec.scala   import SeqSpec.*          // @main renderSvgDiagram
tools/ascii.scala     //> using file seqspec.scala   import SeqSpec.*          // @main renderAsciiDiagram
```

Concrete payoff (2026-07-05): `svg` and `ascii` render *the same* sequence-diagram spec. The grammar lives in
`SeqSpec.parse` alone, so `ascii` reused it verbatim instead of copy-pasting ~25 lines of regexes that would drift
apart the next time the spec grows.

## Why it works — and the two rules that make it safe

The `tt` toolbox has two execution contexts that a shared file must satisfy at once:

1. **Standalone run** — `tt <tool>` runs `scala-cli run tools/<tool>.scala`, a **single-file** compile. The
   `//> using file seqspec.scala` directive is what makes the shared object visible in that single-file build.
2. **Whole-toolbox compile** — `scala-cli compile tools` / `scala-cli test tools` / the Metals MCP compile the
   **entire `tools/` tree as one unit** (the "compiles-as-one" invariant; see `tools/newtool.scala` header).

Two rules keep a shared file valid in both:

- **RULE 1 — the shared file has NO `@main`.** `tt` invokes a tool with `scala-cli run tools/<tool>.scala` and
  passes **no `--main-class`**. If the included helper also had an `@main`, the single-file run would see two main
  methods and fail with an ambiguity error. So a shared file is a *library* object only (like `lib.scala`). The
  per-tool `@main` stays in the tool file.
- **RULE 2 — include a SAME-SCOPE file; never cross scope.** In the whole-toolbox compile, scala-cli **dedupes**
  a `//> using file X.scala` against a file already in the build **when X is in the same scope** — so `svg.scala`
  (main scope) including `seqspec.scala` (main scope) does *not* double-compile it. Proven pre-existing: `text.scala`
  / `files.scala` / `log.scala` all `//> using file lib.scala` and `scala-cli compile tools` is clean.
  **The dedup does NOT hold across scopes.** A `*.test.scala` file (test scope) that `//> using file`s a main-scope
  file re-compiles it in test scope → **duplicate-definition error**. We hit exactly this: `tools/test/lib.test.scala`
  had to **drop** its `//> using file ../lib.scala` and instead rely on test scope *extending* main scope (see
  `research/README` + the tools test move). Cross-scope sharing = inheritance, not a `using file`.

## When to reach for it

- **≥2 tools need the same typed thing** (a model, a parser, a validator, a formatter). One consumer → just keep it
  inline in that tool.
- The thing is **pure library code** (no entry point of its own). If it needs to *run*, it's a tool, not a helper.
- You want the genscalator DRY / typed-reuse property: one greppable, compiler-checked definition instead of copies
  that silently diverge (a *false-echt* risk for behaviour — two "identical" parsers that aren't).

## Relation to the single-dispatcher refactor

The parked `tt` single-dispatcher refactor (one sole `@main` dispatching to tools-as-pure-functions;
see [[genscalator-toolbox-single-dispatcher]] memory + `tools/DESIGN-single-dispatcher.md`) would change the
mechanics: with one `@main`, RULE 1 dissolves (helpers and tools all become plain functions under the dispatcher)
and RULE 2's scope question is handled by the single build. Until then, this shared-file pattern is the
lightweight way to share code **without** that refactor — and `seqspec.scala` is a clean unit the dispatcher can
absorb later.
