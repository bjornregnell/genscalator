# Does the harness's stale-status "disinformation" survive a compact? A testable prediction (2026-07-10)

BR-flagged WR question. BR wondered whether the wrong status line - *"✶ Building blog index page… (5m 20s · ↓
20.6k tokens)"*, NOT what the agent was doing (see
[[harness-status-line-can-misrepresent-a-trust-nit-2026-07-10]]) - will SURVIVE the coming compact.

## Prediction (BR can test right after the compact)
**No - the specific stale status should NOT survive.** The status / "Next" line is harness CHROME, inferred
FRESH each turn from the current context, not part of the durable substrate. A compact resets the conversation to
a summary, so the harness RE-INFERS the status from the new post-compact state; "Building blog index page" has
nothing durable to ride on. (A *different* stale status could re-appear if the compact summary still mentions blog
work - but not this exact one.)

## Why it's a nice datapoint (the durable-vs-ephemeral split)
It illustrates the very distinction blog 012 and the three-size-measures draw: the **DURABLE substrate** (commits,
resume-prompt, pins) survives the warp; the **EPHEMERAL harness chrome** (inferred status) does NOT - it dies with
the warp and is regenerated. Reassuring corollary: the "disinformation" is ephemeral, so it **cannot accumulate**
across warps the way real substrate does - the warp is a *cleanse* for harness chrome even as it preserves
substrate. Ties: [[harness-status-line-can-misrepresent-a-trust-nit-2026-07-10]], blog 012 (what survives a warp),
[[agent-surfaces-substrate-size-measures-three-kinds-2026-07-10]].
