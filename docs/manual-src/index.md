# genscalator manual

**Power tools for agents: smarter, safer, faster.**

Genscalator is a toolbox and workflow for coding agents that replaces the brittle
`bash`/`grep`/`awk`/`python` reflex with **typed, compiler-checked, reusable Scala tools**.
Your agent picks the right tool and gives it the right arguments; the Scala compiler catches
mistakes before they run; and a tiny launcher (`tt`) makes every tool a single,
statically-analyzable command that a narrow permission allowlist can trust.

The tools and workflow are *language-agnostic* — you can generate and manage code in **any
language**. When you generate **Scala**, the bundled Scala skills (`scala-style`,
`scala-code-review`, `reqt-lang`) add extra help.

> This manual is generated from markdown with genscalator's own `tt ssg`, and previewed with
> `tt serv` — a small dogfood of the toolbox documenting itself. Regenerate it with the command
> at the foot of this page.

## Start here

- **[Getting started](getting-started.html)** — install the plugin, verify the skills, run your
  first `gs` command, set up the allowlist.

## The two surfaces

Genscalator gives the agent (and you) two ways in:

1. **Typed `tt` tools** in the terminal — `tt text`, `tt files`, `tt ssg`, `tt statusline`, and
   the rest. One bare command each, so a permission allowlist can trust them by shape.
2. **`gs` do-what-I-mean commands** in Claude Code chat — lead a message with `gs` and the agent
   acts on your intent, matched to the nearest command in meaning (not exact syntax). Try `gs help`.

## Reference

- **[Concepts and glossary](foundations.html)** — the canonical definitions: the cues, the dances,
  the modes, and the ideas behind the workflow (confirmation fatigue, safe-by-design, do-what-I-mean).
- **[Which tool answers which question](tool-selection.html)** — pick the right tool for a task.
- **[The recommended allowlist](allowlist.html)** — the two-tier permission set, the deny-list, and
  the principles; set it up in one repo with `gs allow`.
- **[The status line and mode line](statusline-manual.html)** — the live `model · $ · ctx% · wk%`
  gauge and the declared joint state-of-mind.

## Verify your setup

The agent cannot *feel* a missing skill, so check explicitly:

```
gs skills          # or: tt skillcheck
```

lists the skills that should be active; run `/skills` and confirm each is listed.

## Regenerate this manual

From the genscalator repo root:

```
tt ssg --out docs/generated docs/manual-src/index.md docs/manual-src/getting-started.md \
  docs/foundations.md docs/tool-selection.md docs/allowlist.md docs/statusline-manual.md
tt serv docs/generated      # then open the printed URL
```
