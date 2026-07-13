# WR data: the guard flags `| tail` even when the upstream tool has no native --tail (2026-07-13)

**What happened.** The agent ran `scala-cli test <dir> --java-prop tt.tools=<dir> 2>&1 | tail -40` to see the
end of a long test run. The `PreToolUse:Bash` guard flagged it: *"[MED] pipe to head/tail/wc: use the tool's
--limit / --tail / --count flag instead of a pipe"*, forcing a confirmation (it cost BR a prompt; he logged it).

**The friction.** The guard's advice is right for tools that HAVE a native limiter (`tt text --count`,
`git log -n`, etc.) — a pipe there is a bash-reflex worth removing. But `scala-cli test` has **no** `--tail` /
`--limit` flag; its output is inherently long, and the agent only wanted the pass/fail summary at the end. So
the nudge fired on a case where its own suggested remedy does not exist, and the human paid a confirmation for it.

**The guard-friendly workaround (adopted).** Redirect the full output to an in-repo tmp file and Read the tail:
`scala-cli test <dir> ... > tmp/doctest.out 2>&1`, then Read the file. No pipe, no head/tail, so no guard trip —
and it matches the standing "let the tool self-report to a file and Read it" discipline. Slightly more ceremony
(a file + a Read) than `| tail`, but deterministic and guard-clean.

**Harness-side asks (either, not both):**
- Let the guard **exempt `| tail`/`head`** when the upstream command is one with no native limiter (a small
  allowlist of "inherently verbose, no --tail" commands: `scala-cli test`, `sbt`, `pdflatex`, ...), OR
- Make the **agent default** to redirect-to-file for known-verbose tool runs, so the pipe never appears.

Either removes a confirmation on a false-positive. Ties: `harness-ux.md` (the bash-reflex cluster + guard-trip
class), [[guardcheck-hook-structural-fix]], [[prefer-inrepo-tmp-over-slash-tmp]].

## UPDATE (same session): the redirect was ALSO flagged, a catch-22

The "guard-friendly" redirect above was itself flagged on the very next run: *"[MED] output redirect (>): give
the tool a file-sink flag; do not redirect around it"*. So for a verbose external tool with **no native limiter
AND no file-sink flag** (exactly `scala-cli test`), BOTH bash idioms for capturing just the tail are guarded:
`| tail` and `> file`. The agent is left with: run it **raw** (floods context) or eat a confirmation. A genuine
gap, not a false-positive on one idiom.

**The actually-clean fix: do not use a shell at all.** Run the verbose command with **`run_in_background: true`**
— the harness captures stdout to a task file with **no pipe and no redirect in the command string**, so the
guard sees a bare `scala-cli test ...` and does not trip; the agent reads the captured output on completion.
That is the guard-clean, context-safe way to run any long/verbose tool. (Alternatively, wrap such runs in a `tt`
tool with a native `--out <file>` sink.) Lesson adopted: for a known-verbose run, reach for `run_in_background`,
not a shell pipe or a redirect.
