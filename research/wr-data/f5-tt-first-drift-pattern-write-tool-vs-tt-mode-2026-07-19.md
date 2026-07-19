# F5 tt-first drift, second specimen: Write-tool on the modes file where `tt mode` exists

**Stamp:** 2026-07-19 ~17:15 CEST (BR pin on the agent's own "my miss, and it's an
interesting one" admission).
**Context:** same first-F5-super-agent session as
[f5-cold-start-bare-wc-instead-of-tt-2026-07-19](f5-cold-start-bare-wc-instead-of-tt-2026-07-19.md);
SM164 informal-observation stream.

## The specimen

BR declared `+rot-vigil`. The agent updated the declared-modes state by WRITING
`~/.claude/gs-modes` directly with the harness Write tool — which tripped a guard
stall, because the Write allowlist covers the repos/notes/scratchpad but not
`~/.claude/`. BR asked, from inside the stall: "why did I get a guard stall for
+rot-vigil (in other sessions you just did it)".

The designed path is the typed tool: `tt mode add rot-vigil` — covered by the
`Bash(tt *)` allow, stall-free, listed in `gs help`, unit-tested, and even
documented in the gs-dwim skill as "thin front for tt mode ... allowlisted, no
confirmation". Other sessions "just did it" because they used the tool.

Contributing cause, honestly recorded: earlier this session the agent found the
STATE FILE first (reading statusline.scala to answer an unrelated question) and
only later saw the `mode` tool in the test suite — the file was salient, the tool
was not. Availability beat design.

## The pattern (two same-shape drifts, one session)

1. Morning: bare `wc -c` where the toolbox was assumed to cover file size —
   probe AFTER the human flagged it → revealed a REAL gap (no tt size tool).
2. Afternoon: Write-tool on the modes file where `tt mode` fully exists —
   pure miss, no gap.

Same shape: reach for the generic mechanism without first asking "does tt own
this?". Different value: drift 1 was gap-revealing (toolbox work item), drift 2
was avoidable friction. N=2 in one session is a pattern worth watching, not yet
a trend claim.

## Why it is interesting (beyond the miss)

- **The stall itself was the diagnostic.** The human noticed FRICTION where
  memory said there was none ("in other sessions you just did it") and asked —
  the allowlist's shape (typed tool allowed, raw write gated) encoded the
  design intent well enough that deviating from the design produced a visible
  signal. The guard did not just block; it TAUGHT.
- **Mirror of the jps note:** there the human learned from watching the agent's
  good tool choice; here the human's question surfaced the agent's bad one.
  The transparency channel corrects in both directions.
- **SM164 relevance:** early F5-as-super-agent shows a mild recurring
  tt-last-instead-of-tt-first bias when the tt path is not already salient in
  context. Feeds the smart-zone/rot observation arm; check whether the bias
  correlates with context fill or is a cold-reflex constant.

**Corrective adopted in-session:** mode toggles via `tt mode` from now on.
