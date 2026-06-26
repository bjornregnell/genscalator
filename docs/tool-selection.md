# Tool selection — pick the right tool for the question

> Which tool answers a given question best — so the agent reaches for the precise one first, instead of
> defaulting to grep and following up three times. Right tool → more precise answer, fewer follow-up
> calls, fewer approval prompts.

The reflex to fix: *"search anything"* → `grep`. Grep is a line/byte matcher; it does not know what a
symbol *is*. For Scala **code structure** there is a tool that does — use it, and the answer comes back
right the first time.

## The guide

| The question is about… | Reach for | Why |
|---|---|---|
| plain text, logs, config, non-Scala files — "is this string anywhere?" | **`tt text` / `tt files`** (or a one-off `grep` for a single literal) | typed, no shell scaffolding, one allowlisted command |
| **Scala *structure*** — where is this defined, who calls it, what extends it, show me the body, resolve this import | **`scalex`** | symbol-aware, fast, structured output — see below |
| **true compiler semantics** — inferred types, diagnostics / compile errors, run tests, rename/refactor | **Metals MCP** | needs a build import + compile; use when source-level structure isn't enough |
| reading a file | the editor's **file-read tool** | never `cat`/`head`/`tail` |
| executing effectful or untrusted code | a capability-constrained Scala scratch tool / OS sandbox | never raw bash |

## Why `scalex` for Scala code

`grep` finds the *string* `process`; `scalex` finds the *method* `process` — its definition, its callers,
its overrides, the type it lives on. "Grep, but it understands Scala's AST." It parses with Scalameta and
caches per git OID, so there is no build server to wait on (~2–5 s cold index, **<400 ms** warm). That
precision is exactly what cuts follow-up calls: one `refs Foo.bar --count` replaces a grep, a manual
filter of false hits, and a second grep to categorize them.

Core commands: `explain <Sym>` (definition + scaladoc + members + impls), `def`, `refs <Sym> --count`
(categorized impact), `hierarchy <Sym>`, `imports <Sym>` (resolves wildcard `import pkg.*`),
`body <method> --in <Type>`, `batch` (several queries, one index load). Filters:
`--kind / --path / --no-tests / --exact / --max-output`.

`scalex` is a separate upstream project (a GraalVM-native CLI + its own Claude Code plugin), **recommended
and integrated by genscalator, not bundled**. Install and command reference:
[`tools/README.md`](../tools/README.md#companion-scalex) → *Companion: scalex*. Upstream:
https://github.com/nguyenyou/scalex.

## Rule of thumb

Text and logs → `tt`. Scala *code* → `scalex`. Compiler-grade truth → Metals. Reach down the list only
when the cheaper tool above genuinely can't answer the question.
