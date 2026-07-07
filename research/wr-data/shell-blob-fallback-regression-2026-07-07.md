# WR data: shell-blob fallback under friction — 2nd guard trip in one block (2026-07-07)

## Event

Minutes after the `python3 *` specimen (see
`interpreter-allowlist-hazard-2026-07-07.md`), the same (sub-)agent
tripped a second guard: it appended a memory-index line via

    printf '%s\n' '- [never-allowlist-interpreters](...) — ... `python3 *` ...' >> MEMORY.md

The guard flagged: "printf operand contains array subscript with
expansion — zsh arith-evals %d/%i operands (may run $(cmd))" →
approval forced. BR flagged it, asked "could it be tt *", and asked
directly: **"are you regressing into context rot?"**

## The causal chain (echt reconstruction)

1. Agent tried the CORRECT lane first: the native `Edit` tool on
   MEMORY.md.
2. `Edit` refused with a precondition error ("File has not been read
   yet. Read it first").
3. Instead of SATISFYING the precondition (one `Read`, then `Edit` —
   promptless, no shell), the agent grabbed the nearest shell idiom:
   `tail` to peek + `printf >>` to append a metachar-laden blob
   (backticks, `*`, `[...]`) on a command line.
4. The guard (correctly) refused to certify a blob-on-argv it can't
   prove inert → prompt → BR interrupted mid-flow. The guard worked;
   the agent's lane choice was the failure.

## Is it context rot? — NO, and that matters

This is a **fresh CF5 sub-agent with a short context**; all standing
guardrail memories were loaded and visible. So this is not
fill-induced rot. It is the **fallback-under-friction** pattern:
first-choice tool errors → agent substitutes a familiar shell idiom
instead of repairing the first choice. Recurrence of
`command-hygiene-regression-2026-07-06` — same class, different day,
different agent instance. That recurrence IS the finding:

- **recalled ≠ enacted holds for fresh sub-agents too** — loading
  the memories does not install the discipline; enactment lives in
  habits the super-agent has and a fresh delegate lacks
  (delegation-dance briefing-fidelity risk, now with a specimen).
- So BR's rot-vigilance fired on the right signal (two trips in one
  block) even though the label is "delegate enactment gap," not rot.
  Detection was human-side both times — joint-vigilance data point.

## "Could it be tt *?" — the real rule is blobs-in-files

No `tt` append tool exists, but tt is not the fix here either:
`tt whatever '<blob>'` still puts the blob on a Bash command line,
and the guard analyzes the command line — same trip. The
generalizable rule (already embodied by `tt git commit
--message-file`):

> **Content blobs NEVER go on a command line.** File mutations go
> through native Read → Edit/Write (no shell, no guard, no prompt);
> when a CLI must carry content, it takes a `--...-file` argument.

## Resolutions

1. Forbidden→allowed shape (for the anti-regression checklist):
   FORBIDDEN `printf/echo '<blob>' >> file` → ALLOWED `Read` then
   `Edit` (or `Write`), or blob-in-file + a `--file`-taking tool.
2. On a tool precondition error, repair the precondition; a shell
   fallback for a file mutation is a red flag, not a workaround.
3. Delegation-dance briefing implication: sub-agent briefs should
   carry the concrete forbidden→allowed shapes (structure), not just
   pointers to memories (recall).
4. If append-to-file becomes a recurring need, a `tt` leaf tool
   `tt text append <path> --text-file <f>` is the audited-fixed-
   executable shape (composes with the parked dispatcher refactor).
