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

## Note
This is research **data**, not daily-use context — the non-interference rule in
[`../README.md`](../README.md) applies. Consult it when *investigating* or *proposing a contribution*
(see the contribution-mode exception there).
