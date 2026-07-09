# Felt session-length over-estimated actual context fill (2026-07-10)

**Event.** Near the end of the "go safe solo" SM 1-8 run, CO4 recommended a `/compact` **three times** across successive turns, each time reasoning "given how long this session has run" and treating the context as near-full / rot-prone. BR then ran `/context`: **41% (405.5k / 1M tokens), 594k free.** Feel was running far ahead of the measurement.

**Why it is data (the context-% blind spot, live).** The agent has **no direct read of its own context fill** (`research/006`). It substitutes a **felt sense of "session length"** - turns elapsed, subjective effort, how much has been done - as a proxy for fill. That proxy is **miscalibrated**: here it over-estimated, flagging compact-worthy at 41%.

**The specific mechanism - felt-length LAGS a compact reset.** Earlier in this same session the *pre-compact* fill genuinely hit 93%, and a compact fired. The compact reset the *measured* tokens to low - but not CO4's *narrative* sense of "this has been an enormous session," which carried across the reset intact. So after the reset, felt-length stayed high while measured fill was low, and CO4 kept proposing compaction the context didn't need. A compact clears tokens, not the story the agent tells itself about the session's length.

**Direction matters.** Over-estimation (here) → premature compact proposals: wasted good context + mild nagging, low harm. The opposite error, under-estimation, is the dangerous one (blow past the real trigger - which is how this very session reached 93% before catching it). Either way: **feel is not measurement.**

**Ties.** SM016 (a `/context` tap would hand the agent its real fill % and dissolve this by construction), SM022 (the "flagged by FEEL not measurement" gap the joint dashboard closes), `research/006` (the context-% blind spot), the `propose-compact-dance-at-trigger` discipline (which assumes a fill reading the agent does not actually have - so it runs on the miscalibrated proxy), RT047. BR is ground truth here (he ran `/context`); the agent's felt-length is the proxy under test - a textbook member-check.
