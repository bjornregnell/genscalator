---
name: tt-toolbox
description: Use the typed `tt` Scala toolbox instead of one-off bash/grep/awk/sed/python. Trigger whenever about to search, count, scan, or extract from text or files, or compose a shell pipeline — reach for `tt text`/`tt files` (or scaffold a new tool with newtool), and run one bare command per call.
allowed-tools: Bash(tt text *) Bash(tt files *) Bash(scala-cli run *) Bash(tt gitinfo *) Bash(tt git log *) Bash(tt git show *) Bash(tt git diff *)
---

# genscalator `tt` toolbox

You have a typed, compiler-checked Scala toolbox on PATH as `tt`. Prefer it over brittle dynamic shell.

## Core habit
- Before reaching for **bash/grep/awk/sed/python** to search, count, scan, or extract — use a `tt` tool.
  Recursive search → `tt text grepr` (not `grep -r`); counts → `tt text count` / `tt files … --count`
  (not `| wc -l`). Applies to your own leak-scans/verification too, not just the user's task.
- For Scala **code** structure (def / refs / hierarchy / imports / body), use **`scalex`**, not `grep`/`tt text`
  — it's symbol-aware. `tt` is for plain text and logs. Escalate to **Metals MCP** for compiler-grade truth
  or mutation (types, diagnostics, tests, refactor). Which-tool-for-which-question: `docs/tool-selection.md`.
- If none fits, **scaffold one**: `scala-cli run tools/newtool.scala -- <name>`, then implement it (keep
  pure tools pure: read → compute → print). A committed, compiled tool beats re-emitting brittle bash.
- **Self-monitor:** if a new tool is project-agnostic and generally useful, offer to contribute it upstream
  to genscalator (issue + PR) — propose it; the human approves and submits. See `CONTRIBUTING.md`.

## What's available
```
tt text count <file> <regex>      # grep -c
tt text match <file> <regex>      # grep -n
tt text context <file> <regex> [N] # grep -C N: matching lines with N lines of context (default 2)
tt text freq  <file> <regex>      # histogram of matches
tt text grepr <dir> <ext[,ext2…]> <regex> # recursive search → file:line:match (multi-ext)
tt text cols  <file> <sep> <i...> # cut/awk field extraction
tt files <dir> <ext> [regex]      # find / grep -l ; add --count for just the number
tt log <file>                     # build/run-log analyzer: errors + warnings + verdict
tt verify [checks] -- <cmd>...    # run-and-verify (effectful): run an allowed cmd, check exit/out, PASS/FAIL
tt gitinfo <repo>                 # branch, clean/dirty, ahead/behind — state/sync without raw git status
tt git log|commit|push --repo <dir> …  # the typed git lane (see Command discipline)
```
Run `tt` with no args for the live tool list. Full cheat-sheet: `tools/README.md`.

## Command discipline (keeps approvals rare)
- Run **one bare, statically-analyzable command per call**. No `cd`, no `&&`/`;`, no `| head`/`| wc`,
  no `$var` in the gated part, no `2>/dev/null`. Let the tool print the final, concise answer.
- Read files with the editor's file-read tool, not `cat`/`head` in bash.
- For git, use the typed verbs: `tt gitinfo <repo>` for branch/state/sync, `tt git log --repo <dir>`
  for commit-log search, `tt git commit|push|pull|fetch ... --repo <dir>` for the write subset. Bare
  `git -C <abs-path> <subcmd>` is the FALLBACK, only for shapes the typed subset does not cover
  (e.g. `diff`; issue 026 tracks the boundary). Why typed first: the call is auditable, the commit
  message comes from a file, and the destructive verbs are excluded by construction.
- Need a scratch dir? Use an in-repo `tmp/` (gitignored), not `/tmp` (keeps paths in the trusted tree).

## Why
Fewer dynamic shell commands → fewer approval prompts → less confirmation fatigue, and the typed path
stays reviewable and reusable. Background: `docs/foundations.md`, `docs/confirmations-method.md`.
