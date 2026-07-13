# The status-line clock: how it actually behaves (SM063)

Empirical documentation of the `genscalator: HH:MM:SS` clock on line 1 of the status line — when it updates,
when it freezes, and how to make it live. (Confirmed via claude-code-guide + live observation, 2026-07-13.)

## It is EVENT-driven, not timer-driven
Claude Code re-invokes the `statusLine` command on **events**, not on a wall-clock timer:
- after each new assistant message,
- after `/compact`,
- on permission-mode / vim-mode changes,
- debounced to ~**300ms** during active use.

**At idle** (sitting at the prompt, no activity) it stops re-invoking, so the displayed content — including the
clock — **freezes at the last-render time**. So `genscalator: 16:37:39` really means *"time of the last event"*,
not *"now"*. It unfreezes the instant there is activity (your next message is an event).

**Precise statement:** frozen at *idle*, not merely *between renders* — because **each human message is an
event** that re-renders, during an active back-and-forth the clock tracks near-real-time at every exchange; it
freezes only in a *true* idle gap.

## Making it live (the fix)
Add a **`refreshInterval`** field (minimum **1 second**) to the statusLine settings:
```json
"statusLine": { "type": "command", "command": "tt statusline --mode-line", "refreshInterval": 1 }
```
Then the command re-runs even at idle → a live clock. **Cost:** it spawns the statusline subprocess every
second while idle (small, but nonzero). Human-approved settings change; not self-applied.

## Cross-check: the clock and the "Wrangling…" spinner are consistent
The spinner shows **live elapsed** since the current turn started; the clock shows **absolute wall-time at the
last render**. So `gs-clock − spinner-elapsed` should recover a **constant turn-start** — and it does
(three samples landed within ~18s of each other). Two independent time sources triangulating the same timeline
= a mini reproducibility check; at each render the clock is honest (near-current), so the staleness is confined
to the idle gap, not active use.

## The design question (open — SM062)
Given the freeze IS the mutual-idle stall made visible, the honest options are three, not two:
- **frozen-and-lying** (current default): a stopped clock that *looks* live → disinformation.
- **always-ticking** (`refreshInterval: 1`): honest "now", but it *erases* the signal that we are at a stall.
- **stopped-and-honest** (candidate): let it stop, but render it AS stopped (dim / ⏸ / "paused HH:MM") so the
  stop becomes an awareness cue. Decide jointly (SM062).

## Related: the auto-compact threshold is UNDOCUMENTED
Auto-compact is on by default (`autoCompactEnabled`; off via `DISABLE_AUTO_COMPACT=1`) and fires when context
"approaches the limit" — **no published %**. The 90% "Context is 90% full" message is a separate *alert*, not
the trigger. Observed ~90-95%; the status line's `--auto-compact` default (92) is a tunable guess.

Sources: `research/wr-data/instruments-must-not-mimic-harness-disinformation-2026-07-13.md` (frozen clock +
temporal cross-check + design target). Ties: SM062, SM063, the mode line, `docs/statusline-manual.md`.
