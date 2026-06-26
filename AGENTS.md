# AGENTS.md — operating contract for agents

genscalator (GS) is a toolbox + workflow for *you, the coding agent*. This file is the short contract;
the reference docs are linked at the bottom — read them once, then follow these rules.

## Core habit
- Before reaching for a one-off **bash/grep/awk/sed/python** helper, use a typed `tt` tool instead.
- If no tool fits, **scaffold one**: `scala-cli run tools/newtool.scala -- <name>`, then implement it.
  Keep pure tools pure (read → compute → print). A committed, compiled tool beats re-emitting brittle
  bash every time.
- See the tool cheat-sheet for what already exists: [`tools/README.md`](tools/README.md).

## Command discipline (avoids confirmation prompts)
- Run **one bare, statically-analyzable command per call**. No `cd`, no `&&`/`;`, no `| head`/`| wc`,
  no `$var` in the gated part, no `2>/dev/null`. Let the tool print the final, concise answer.
- Invoke tools as `tt <tool> <args...>` so the call matches a narrow allowlist entry (e.g. `Bash(tt text *)`).
- Read files with the editor's **file-read tool**, not `cat`/`head`/`tail` in bash.
- For git, use bare `git -C <abs-path> <subcmd>` — not `cd … && git …`.

## Why this matters (one line)
Fewer dangerous, dynamic commands → fewer approval prompts → less **confirmation fatigue**, and the
typed path stays reviewable and reusable.

## Read more
- [`tools/README.md`](tools/README.md) — the `tt` toolbox cheat-sheet (`text`, `files`, `newtool`).
- [`docs/foundations.md`](docs/foundations.md) — goals, stakeholders (human / agent / BHH), glossary.
- [`docs/confirmations-method.md`](docs/confirmations-method.md) — method for driving down confirmation fatigue.
