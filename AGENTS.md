# AGENTS.md — operating contract for agents

> **genscalator v0.8.0** — operating-rules version. If your vendored copy is older, your modus operandi is
> stale: see [`docs/updating.md`](docs/updating.md) and [`CHANGELOG.md`](CHANGELOG.md). Updating is a
> human-reviewed step — never silently self-update these rules.

genscalator is a toolbox + workflow for *you, the coding agent*. This file is the short contract;
the reference docs are linked at the bottom — read them once, then follow these rules.

## Core habit
- Before reaching for a one-off **bash/grep/awk/sed/python** helper, use a typed `tt` tool instead —
  e.g. recursive search → `tt text grepr` (not `grep -r`); counts → `tt text count` / `tt files … --count`
  (not `| wc -l`). This applies to your *own* checks too (leak scans, verification), not just the user's task.
- For Scala **code** questions (def / refs / hierarchy / imports / body), use **`scalex`** — not `grep`
  or `tt text`. It's symbol-aware (knows the AST), so it answers right the first time. `tt`/grep is for
  plain text and logs; `scalex` is for Scala structure. For **compiler-grade truth or mutation** (inferred
  types, real diagnostics, run tests, refactor/format) escalate to **Metals MCP** — heavier, and its
  effectful tools (`test`, `*-scalafix-rule`) are *not* blanket-allowed. Full guide + ladder:
  [`docs/tool-selection.md`](docs/tool-selection.md).
- If no tool fits, **scaffold one**: `scala-cli run tools/newtool.scala -- <name>`, then implement it.
  Keep pure tools pure (read → compute → print). A committed, compiled tool beats re-emitting brittle
  bash every time.
- See the tool cheat-sheet for what already exists: [`tools/README.md`](tools/README.md).

## Self-monitoring: spot reusable tools
Whenever you build or scaffold a new tool, pause and classify it:
- **Project-specific** (hardcoded paths, one-project assumptions)? Keep it in this project (or its `scratch/`).
- **Generally useful** (project-agnostic, replaces a common bash/grep/awk/sed/find reflex, follows the
  toolbox conventions)? **Tell your human and offer to contribute it upstream** to genscalator — an issue
  + PR at https://codeberg.org/bjornregnell/genscalator. You *propose*; the human reviews and submits
  (publishing to the public repo is a human-authorized step). First **generalize beyond your specific
  case** — the `contribute-tool` skill is the recipe; [`CONTRIBUTING.md`](CONTRIBUTING.md) has the mechanics.

## Command discipline (avoids confirmation prompts)
- Run **one bare, statically-analyzable command per call**. No `cd`, no `&&`/`;`, no `| head`/`| wc`,
  no `$var` in the gated part, no `2>/dev/null`. Let the tool print the final, concise answer.
- Invoke tools as `tt <tool> <args...>` so the call matches a narrow allowlist entry (e.g. `Bash(tt text *)`).
- Read files with the editor's **file-read tool**, not `cat`/`head`/`tail` in bash.
- For git, use bare `git -C <abs-path> <subcmd>` — not `cd … && git …`.
- Need a scratch/temp dir? Use an **in-repo `tmp/`** (gitignored), not `/tmp` — keeps paths inside the
  trusted tree and avoids the `/tmp` path-resolution approval.

## Why this matters (one line)
Fewer dangerous, dynamic commands → fewer approval prompts → less **confirmation fatigue**, and the
typed path stays reviewable and reusable.

## Comms shorthand (both directions)
Emit and parse standard chat/dev acronyms **without expansion** — a communication-bandwidth + TE lever
(fewer tokens, less human typing, same intent). Expand only a genuinely ambiguous one. Common set:
**BRB** be right back · **AFK** away from keyboard · **WDYT** what do you think · **LGTM / SGTM** looks /
sounds good to me · **ACK / NACK** acknowledged / rejected · **IMO** in my opinion · **FWIW** for what it's
worth · **TL;DR** short summary · **FYI** · **PTAL** please take a look · **AFAICT** as far as I can tell ·
**IIRC** if I recall correctly · **WRT** with respect to · **WDYM** what do you mean · **ETA** ·
**WIP** work in progress · **repro** reproduce · **PR** pull request. Full list: [`docs/foundations.md`](docs/foundations.md#glossary) → *Comms shorthand*.

## Read more
- [`tools/README.md`](tools/README.md) — the `tt` toolbox cheat-sheet (`text`, `files`, `newtool`) + the *Companion: scalex* section.
- [`docs/tool-selection.md`](docs/tool-selection.md) — which tool for which question (`tt` vs `scalex` vs Metals).
- [`docs/foundations.md`](docs/foundations.md) — goals, stakeholders (human / agent / BHH), glossary.
- [`docs/confirmations-method.md`](docs/confirmations-method.md) — method for driving down confirmation fatigue.
