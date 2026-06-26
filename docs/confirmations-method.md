# Confirmations log — method & template

A lightweight practice for driving down **confirmation fatigue (CF)**: track when a manual approval
prompt popped up, what the agent tried to do, the likely trigger, and the **safe-by-design fix** that
would remove the need to confirm. Goal: converge toward only the *rare, genuinely-important*
confirmations, and learn the recurring patterns.

This doc is the **methodology + template**. Keep your own filled-in log privately in your working
project — raw rows often contain local paths, hostnames, and project internals that don't belong in a
public repo.

## How we capture (given a limitation)
The agent generally **cannot detect** post-hoc whether a specific tool call required approval (auto- vs
user-approved results can look identical to it). So:
- **Agent** flags commands it *predicts* are confirmation-prone (compound bash, non-allowlisted tools
  like `grep -r`/`awk`, `/tmp` paths, brand-new commands) and logs them.
- **Human** notes which actually prompted ("I had to confirm this").
- Together, add the **fix** (an allowlist entry, or — better — replace the habit with a typed,
  allowlisted tool).

## Log template
Copy this table into your private log and add one row per noteworthy confirmation:

| # | What the agent tried | Likely trigger | Safe-by-design fix |
|---|----------------------|----------------|--------------------|
| 1 | _bare description of the command/intent_ | _why it likely prompted (e.g. compound `&&`, non-allowlisted helper, `$var`, `/tmp` path)_ | _the fix: narrow allowlist entry, or a typed tool that removes the habit_ |

## UX risk: the "Yes, and always allow …" option
A confirmation prompt's **"Yes, and always allow …"** option is itself a safety hazard under fatigue:
- An always-allow can quietly grant something broad (e.g. a `~/bin` write) — exactly the kind of
  outside-the-project grant you do NOT want to approve unseen.
- The deeper problem: **deciding "always?" on every prompt is its own cognitive load**, and a tired
  mis-click can permanently widen the allowlist and open unsafety by mistake.
- Yet you *do* sometimes want "always allow" for genuinely safe, repeated things — so "never click
  always" isn't the answer either.

**Goal:** reach a state where "always" decisions are **easy and low-stakes**. Mitigations:
- Prefer **narrow, precise allowlist entries** (per-subcommand, e.g. `Bash(tt text *)`, not `Bash(tt *)`)
  so an "always" — or a curated settings edit — is tightly scoped and reviewable.
- **Curate the allowlist file deliberately** (reviewed commits) instead of via in-the-moment "always"
  clicks; treat the allowlist as reviewed code, not an accident of clicking.
- Default to **"just this once"**; reserve "always" for things already vetted in the allowlist file.
- Agent habit: **echo/dry-run** what a new command will do before running; never propose an action that
  writes outside the project without flagging it explicitly.
- Open: can broad/`$HOME`-touching always-grants be visually distinguished or blocked?

## Recurring patterns (the big one)
**The agent's ingrained bash-composition habit is the dominant trigger.** The constant across most
entries: wrapping work in `echo … ; cmd | head/wc/sed ; cmd2` with redirects/variables. Each addition
(`|`, `;`, `echo`, `$var`, `2>/dev/null`, a non-allowlisted helper) makes the call non-analyzable → prompt.
- **Fix 1 (discipline):** run **one bare allowlisted command per tool call**; no pipes/compound/echo.
- **Fix 2 (tools):** make tools produce the *final* concise answer (counts, summaries, formatted output)
  so there's nothing left to `| head/wc/sed` (e.g. a `files` tool that replaces `find|wc`, and `--count`
  modes).
- **Fix 3 (reading):** use the editor's **file-read tool** for files, never `cat`/`head` in bash.
- **Habit reversion** under time pressure (grep/find for "just a quick thing") re-introduces the trigger;
  the toolbox must make the typed path the *easy* path.

### A standing git pattern that avoids prompts
Even when `git *` is allowlisted, a `cd … && git add … && git commit … && git log` compound is
non-analyzable → prompt. Instead use **bare `git -C <abs-path> <subcmd>`** — no `cd`, no `&&` — one
invocation per call (e.g. `git -C <path> commit -m "…" <file>`, then `git -C <path> push`).
