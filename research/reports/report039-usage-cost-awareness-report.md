# SM039 — usage/cost awareness for human + agent: investigation report + recommendation

**Status:** investigate→report (SM039 is report-only; nothing built or configured). Author: agent solo, post-compact
AFK run, 2026-07-10. Grounded in (a) an on-box probe of the actual session transcript, (b) a documentation lookup of
the Claude Code statusline schema + ccusage's method (both cited below).

## TL;DR recommendation (one screen)
1. **Build a `tt statusline` leaf tool + have BR wire one settings line.** This is the high-value, low-effort win.
   Claude Code pipes the statusline command a JSON on stdin that **already contains** `cost.total_cost_usd`,
   `context_window.used_percentage`, and `rate_limits.five_hour/seven_day.used_percentage`. So a small tool that
   formats that JSON gives BR **session $ + context% + weekly quota%, live, every turn** — no compute, no dependency,
   on-box. This **structurally removes the human-paste step** of the token-usage dance (no more pasting `/cost` /
   `/usage` / `/context`) and directly answers the "can I trust the harness?" nit: the number is always on screen,
   sourced from CC itself. Split like guardcheck: **agent writes `tt statusline` (Part A); BR adds the one
   `"statusLine"` line to settings.json (Part B, human-gated).**
2. **Do NOT hand-roll a `tt usage` cost calculator for the live number** — the statusline already has it, and a
   from-scratch JSONL sum is **4–12× wrong** (proven below). A `tt usage` is only worth building for **historical /
   cross-session** analysis (what ccusage does), and only if that need is real (e.g. SM045). If built: **inspired-by,
   not depend-on** ccusage — port its validated counting + price table into pure-JDK, and **calibrate to `/cost`**
   before trusting a single number.
3. **Dance impact:** the token-usage dance collapses from "human pastes /cost, agent reads it" to "glance at the
   statusline." [[token-budget-modes]] (weekly-headroom read) becomes a glance, not a paste-and-ask.

## Evidence 1 — the statusline already carries everything (doc lookup)
Claude Code invokes a configured `statusLine` command and pipes it a JSON object on stdin
(https://code.claude.com/docs/en/statusline.md). Confirmed fields relevant here:
- `model.id`, `model.display_name`
- **`cost.total_cost_usd`** — the session $ (the number BR pastes from `/cost`)
- **`context_window.used_percentage` / `remaining_percentage`**, plus `total_input_tokens`, `total_output_tokens`,
  and a `current_usage` breakdown (input/output/cache_creation/cache_read) — the `/context` figure
- **`rate_limits.five_hour.used_percentage`, `rate_limits.seven_day.used_percentage`, `resets_at`** — the `/usage`
  quota (Claude subscription Pro/Max only; BR is on a subscription, so applies)

**Implication:** everything BR currently pastes manually is handed to a statusline for free, once per turn. The
statusline needs to *format*, not *compute*. This is why recommendation #1 is a formatter, not a calculator.

## Evidence 2 — a hand-rolled on-box cost sum is badly wrong (my probe)
I probed the actual current session transcript (`240e00c3…jsonl`, 31.9k lines) to test the "read the JSONL
ourselves" idea (a `probe-usage.scala`, pure ujson):
- The usage data **is** present and per-model: **11,051** assistant entries carry `message.usage`
  (input/output/cache_read/cache_creation) + `message.model`. So on-box reading is feasible.
- **Naive sum → ~$9,661** vs the real `/cost` of ~$797 (≈12×).
- **Trap A — duplicate entries:** deduping by `message.id` dropped **6,824 of 11,051** entries (62% are repeats —
  streaming / multi-content-block turns re-log the same `usage`). After dedup: 11,051 → **4,227** distinct calls,
  and the estimate fell to **~$3,382**.
- **Trap B — price table + residual (~4× still high):** the remainder is the price table (cache-read dominates the
  token volume — billions of cache-read tokens — so the cache-read rate is the whole ballgame) and possibly counting
  sub-agent sidechain entries. *Cause of the residual is unverified* — the honest point is that **you cannot trust a
  hand-rolled sum; it must be calibrated against `/cost`.**
- **Conclusion:** the hard part of a usage tool is not reading the file — it's the *validated counting* (dedup +
  billable fields + correct, current price table). ccusage has already solved and validated this.

## Evidence 3 — ccusage is purely local (doc lookup)
ccusage (github.com/ryoppippi/ccusage) parses `~/.claude/projects/**/*.jsonl`, sums the four per-message token fields
× a (hardcoded or custom) price table, **offline-capable, no Anthropic API call**. It is exactly the approach my
probe prototyped — which is why my probe's traps *are* the problems ccusage already solved. And `/cost` itself is a
**local** estimate (token counts × pricing, may differ from the real bill); only the `/usage` **quota %** is
genuinely server-only (account-wide, cross-product) — though, per Evidence 1, CC fetches it server-side and hands it
to the statusline anyway.

## The inspire-vs-depend decision ([[dependency-preference-cascade]])
- **For the LIVE gauge:** neither inspire nor depend on ccusage — the **statusline already has the numbers**. Build
  `tt statusline` (formatter). Zero dep, on-box, data-sovereign.
- **For HISTORICAL analysis (optional, later):** **inspired-by** ccusage (port its counting + price table into a
  pure-JDK `tt usage`), **not depend-on** (a Node tool + its update surface). Rationale: BR funds privately +
  values data-sovereignty ([[br-funds-claude-privately]]); the counting is a bounded, portable algorithm; but a
  naive reimplementation ships false confidence, so **calibrate to `/cost` as an acceptance test** before trusting it.
  Only build if the retrospective need materializes (SM045 TE work is the likely trigger).

## How the dances simplify
- **Token-usage dance:** today = human runs `/cost` (or `/context`, `/usage`) and pastes the figure; agent reads it.
  With `tt statusline` = the figure is **always on screen**; the paste step disappears. The dance becomes "read the
  statusline," and the agent can be told to *trust the statusline as the live source* (it is CC's own number).
- **[[token-budget-modes]]:** the mode pick (spending / normal / saving) keys off weekly headroom + reset proximity
  — both now glanceable (`rate_limits.seven_day.used_percentage` + `resets_at`), so mode selection stops needing a
  paste.
- **The trust nit** (BR: "can I trust the harness?" re the misleading "Next:" line — see
  [[harness-status-line-can-misrepresent-a-trust-nit-2026-07-10]]): a statusline sourced from CC's own cost/context
  JSON is the *trustworthy* counter that sits next to the untrustworthy inferred-status chrome. It doesn't fix the
  "Next:" line, but it gives BR a number he *can* trust in the same visual region.

## Proposed `tt statusline` (sketch, for a later build — NOT built here)
A leaf tool reading the statusline JSON on stdin and printing one line, e.g.:
`opus4.8 · $12.34 · ctx 41% · wk 14% (resets 3d)`. Pure JDK + ujson (already a toolbox dep). Handles missing fields
gracefully (rate_limits absent for non-subscription; cost absent early). Allowlist-clean (a `tt` leaf). BR then adds
`"statusLine": { "type": "command", "command": "tt statusline" }` to settings.json (his hand).

## Open questions for BR
1. **Green-light `tt statusline`?** (agent builds the leaf; you wire the one settings line). This is the concrete
   ask — the rest is analysis.
2. **What to show** on the line (order/fields): `$` + context% + weekly% + reset — add model? session-5h%? a
   value/loose-ends signal (RT052)?
3. **`tt usage` (historical):** defer until a real retrospective need (SM045)? Or is per-day/per-project cost
   tracking wanted now?

Ties: SM039, SM022 (`agentMetricsPanel` dashboard), SM013 (JSONL transcript), RT052 (token-stress),
[[token-budget-modes]], [[cost-snapshot-2026-07-10-usd774-opus-context-reread]], [[dependency-preference-cascade]],
[[br-funds-claude-privately]].
