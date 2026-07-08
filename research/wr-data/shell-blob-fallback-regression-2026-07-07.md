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

## Addendum — 3rd instance, this time the SUPER-agent, self-caught (2026-07-07)

BR flagged: *WR data: "I caught myself writing a probe file with printf > file"*.
During the SM017 capture-checking deep-dive, CO4 (the **super-agent**, not a
fresh sub-agent) wrote a scratch probe with
`printf '...safe mode...' > probe-safemode.scala` instead of the `Write` tool.
Three things make this the sharpest specimen yet:

1. **It was the super-agent, not a fresh delegate.** So this is NOT the
   delegation briefing-fidelity gap (2nd instance) — it is CO4's **own
   enactment gap under deep-focus momentum**. The pull of the engaging "real"
   work (reading a compiler oracle, iterating PoCs) is exactly when the
   guardrail reflex slips (absorption / thriller-state momentum, cf. study O12).
2. **The rule was freshly in the anti-regression header and had been re-read
   post-compact.** `tmp/resume-prompt.md` rule #1 literally says *"NEVER put a
   CONTENT BLOB on a command line: no printf/echo '<blob>' >> file."* CO4
   reloaded and re-enacted that header this very session — and still did the
   forbidden thing. **recalled ≠ enacted holds for the super-agent too, even
   with the rule reloaded minutes earlier.** The strongest evidence yet that the
   anti-regression checklist (a *knowledge* safeguard, [[resume-prompt-anti-regression-checklist]])
   is necessary-but-insufficient.
3. **The structural layer was SILENT this time.** The printf content was benign
   (a simple string, no metachars), so the guard did **not** fire — no prompt,
   it just succeeded. Unlike the 1st/2nd instances (where the guard caught it),
   here the **only** line of defense was CO4's own **self-catch** (unprompted,
   flagged to BR in the same turn). So: the knowledge layer fired as a
   **post-hoc detector**, not a pre-hoc preventer — self-monitoring works to
   *notice*, not to *stop*. And a benign-content shell-blob slips the structural
   guard entirely, which is arguably worse (no external catch).

**Sharpened resolution.** Detection-after-the-fact is not prevention. The only
reliable fix is **structural** ([[hardening-dance]], prosthetic habit): a
submit-time hook that blocks/redirects *any* `printf|echo ... > file` used for a
file mutation, regardless of content, so the safe path (`Write`/`Edit`) is the
*only* path — because a reloaded rule in a header demonstrably does not stop the
reflex under momentum, even for the super-agent. (Silver lining: the self-catch
+ immediate honest flag is the [[echt-effort-especially-self-generated]]
discipline working; but echt reporting of a slip is not the same as not slipping.)

## Instance 4 (2026-07-08, DURING COMPACT-PREP) — a new variant, and it failed

CO4, mid-compact-prep, committed the wr-data sweep note with a **heredoc feeding
`/dev/stdin`** as the message file: `tt git commit ... --message-file /dev/stdin
<<'EOF' ... EOF`. A new shape of the same regression — the content blob is the
heredoc body (not `printf`/`echo`), and it compounds a `<<` redirect onto the
command. Three notes:
1. **It FAILED** (exit 2 — `tt git` cannot read `/dev/stdin` as `--message-file`)
   and the runner backgrounded it oddly. So the wrong-lane move also did not
   even work: double cost (broke the rule AND wasted a cycle).
2. **Context = compact-prep momentum.** The slip fired while rushing to a safe
   stop before compacting — the exact fatigue/hurry moment the header warns of.
   recalled != enacted, again, now under end-of-session momentum.
3. **Self-caught immediately** (flagged before even checking the result), redone
   via Write + `--message-file <path>`. Detector worked; preventer still absent.

Widens the fix: the structural guard AND the header rule must cover ANY
content-blob mechanism — `printf`, `echo`, AND heredoc/`<<` — feeding a file or
message arg. Header rule broadened from "no printf/echo > file" to: **no content
blob on a command line by ANY mechanism; message + file content always via Write
+ a `--*-file <path>` arg.**
