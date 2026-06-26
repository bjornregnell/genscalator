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
| **compiler-grade truth or any mutation** — inferred types, real compile errors, run tests, refactor (Scalafix), format, dependency / build queries | **Metals MCP** | runs the presentation compiler + build server; reach here when scalex's source-level view isn't enough — see below |
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

## Why Metals MCP — and where it differs from scalex

scalex is source-level (AST + name resolution), **read-only**, sub-second. Metals MCP runs the
**presentation compiler + build server**, which buys a tier scalex structurally cannot reach:

- **Compiler-grade truth** — `compile-file` / `compile-module` / `compile-full` give *real diagnostics*;
  `inspect` / `get-docs` / `get-usages` resolve *through the compiler* (inferred types, implicits,
  cross-module), so fewer false hits than AST matching on tricky code.
- **Run code** — `test` (whole suites or a single method).
- **Mutate / refactor** — `generate-scalafix-rule`, `run-scalafix-rule`, `format-file`.
- **Build & deps** — `import-build`, `list-modules`, `find-dep` (Coursier).

The cost: it needs a **build import + compile + indexing** before it can answer, so it's heavier and
slower to first response than scalex. The overlap (`glob-search`, `get-usages`, `get-source`, `inspect`)
is exactly where scalex wins on speed — so prefer scalex there and escalate to Metals only when you need
what the compiler knows.

Metals exposes its tools over an **MCP server (HTTP / stdio)**, *not* a bash command — so it's governed by
your MCP tool permissions, not the `tt` "one bare command" allowlist. (It's also a concrete instance of
the cross-tool MCP path on the genscalator roadmap.) Setup:
https://scalameta.org/metals/docs/features/mcp/.

### Safety split (matters for the threat model)
Not all Metals tools are the same safety class — grant them accordingly:

- **Read-only** (`glob-search`, `typed-glob-search`, `inspect`, `get-docs`, `get-usages`, `get-source`,
  `list-modules`, `find-dep`, `compile-*`) — low-stakes; safe to allow broadly.
- **Effectful / mutating** (`test`, `run-scalafix-rule`, `generate-scalafix-rule`, `format-file`,
  `import-build`) — these **execute code or change files**. `generate-scalafix-rule` in particular is an
  arbitrary-code surface. Treat them like the effectful-driver tool class: **do not blanket always-allow**;
  approve per-use or per-narrow-scope. (See [`foundations.md`](foundations.md) — BHH BadGoals.)

## Escalation ladder (the rule)

1. **text / logs / non-Scala** → `tt text` / `tt files` (or a one-off `grep`).
2. **Scala *structure*, read-only** → `scalex` — the default for code navigation. No build server, <400 ms warm.
3. **compiler-grade truth or any mutation** → **Metals MCP** — inferred types, real diagnostics, run tests,
   refactor, format, dep/build queries. Heavier (build import + compile + index), so reach here only when
   step 2 genuinely can't answer.

**Tie-break:** *read & fast → scalex; needs the compiler, or changes something → Metals.* Reach down the
ladder only when the cheaper tool above genuinely can't answer the question.
