# wr-data/ — Workflow Research (WR) data

**WR** = **Workflow Research**. This data captures, from *real working sessions*, every moment an agent's
action triggered a **confirmation / approval prompt** that looks like a **candidate for elimination by
building a new typed `tt` tool** (safe-by-design).

This is the **raw evidence behind genscalator's core thesis**: confirmation fatigue (CF) is driven by
specific, recurring dynamic-shell actions, and each one is a candidate to replace with a safe, compiled
tool so the prompt never needs to appear again. Background:
[`../../docs/confirmations-method.md`](../../docs/confirmations-method.md) and
[`../../docs/foundations.md`](../../docs/foundations.md) (CF, safe by design).

## What to record — one entry per confirmation event
A working session appends an entry **when an action it just took rendered a confirmation** that could
plausibly be designed away by a tool. Suggested fields:

| field | meaning |
|---|---|
| `when` | date (the session stamps it — sessions can't generate dates blindly, so write it explicitly) |
| `context` | task / project the agent was doing (e.g. introprog/autotranslate) |
| `action` | what the agent was trying to do |
| `command` | the actual command / tool call that needed approval |
| `why-prompted` | what made it require confirmation (dynamic shell, `/tmp` path, pipe, `&&`, raw grep, `$var`, …) |
| `candidate-tool` | the typed `tt` tool that could make this safe-by-design — sketch a name + what it does |
| `status` | `idea` / `proposed` / `built` (link the tool / PR) |

Keep it lightweight — a markdown table row or a short block per entry. A **structured / columnar** form is
welcome (e.g. a TSV), so a `tt text freq` / `tt text cols` tool can later histogram the *most common
confirmation causes* — turning the WR log itself into input for prioritising which tool to build next.

## Files
- [`introprog-autotranslate.md`](introprog-autotranslate.md) — WR entries from the introprog/autotranslate
  case study (its own working session writes here).
- [`genscalator-self-dev.md`](genscalator-self-dev.md) — WR entries from developing genscalator itself
  (dogfooding: building the toolbox surfaces the friction it exists to remove).
- [`settings-local-mirror.json`](settings-local-mirror.json) — a **committed mirror of the live
  `.claude/settings.local.json`** permission allowlist (which is gitignored, so its evolution would otherwise
  leave no trace). See below.

## The permission-allowlist mirror (`settings-local-mirror.json`) — data + update rule
The live `.claude/settings.local.json` in the working repo holds the **permission allowlist** — the exact set
of tool calls the human has trusted enough to run without a confirmation prompt. That file is **gitignored**
(local, machine-specific), so the *evolution of the human↔agent trust boundary* — arguably the single most
important WR signal (each added rule = a friction the human chose to design away or trust) — would otherwise be
invisible to the research. So we **mirror it here** and let **git history of this one file** be the dataset:
each commit is a snapshot of the trust boundary at a point in time; `git log -p settings-local-mirror.json`
replays how the allowlist grew (and why, via commit messages).

**Update rule (for the working session / agent):**
1. **The human still approves all real settings changes.** The agent does NOT widen `.claude/settings.local.json`
   on its own initiative — a change lands either via the human clicking *"don't ask again"* on a prompt, or via
   an explicit human request to add a rule (e.g. the scoped `rm -f .../tmp/*` rules BR asked for on 2026-07-02).
2. **Whenever `.claude/settings.local.json` changes (agent-made-with-approval OR human-made), sync this mirror**
   to match it byte-for-byte and commit, with a message noting *what rule changed and why*. The mirror is pure
   research data (no approval needed to update the mirror itself — only the underlying settings need approval).
3. If the agent notices drift (mirror ≠ live file) at any checkpoint, reconcile: copy live → mirror, commit.
This keeps the "friction → trusted-rule" end of the CF thesis measurable, closing the loop the `candidate-tool`
/ `status: built` columns above open. Related agent-facing rule persisted in memory (`settings-local-mirror`).

## Note
This is research **data**, not daily-use context — the non-interference rule in
[`../README.md`](../README.md) applies. Consult it when *investigating* or *proposing a contribution*
(see the contribution-mode exception there).
