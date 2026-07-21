# Session-limit datum: 98% at 16:14, reset 16:30

2026-07-21 16:14:28 clock-read. BR relayed the harness banner verbatim as WR data:

"You've used 98% of your session limit · resets 4:30pm (Europe/Stockholm) · /upgrade to
keep using Claude Code"

Context: arrived mid v0.9.1 release preparation, ~16 minutes before reset. The day: a full
post-warp AFK safe-solo queue (SM181 build, SM186 ember + 4 probe arms incl. two extra
sub-agents, push-15 minion, SM188 minion + adjudication, ~15 commits x3 hosts) on TokSpend.
Agent response: session-limit dance — checkpoint everything committable, compress replies,
park the release firing until after reset (it is BR-gated anyway). Nearness of the reset
(minutes) makes this the cheap case of the dance: pause beats squeeze.

## ADDENDUM (2026-07-21 16:36:24 clock-read) — the stall observed; auto-degrade FALSIFIED

BR chose "keep going and see what happens", guessing the harness would auto-degrade CF5 to
CO4. What actually happened (BR's paste of the stall UI, 16:34:34 CEST, trimmed to the verdict
lines):

    You've hit your session limit · resets 4:30pm (Europe/Stockholm)
    What do you want to do?
    > 1. Stop and wait for limit to reset
      2. Upgrade your plan

Findings, enumerated:
1. **No auto-degrade**: a HARD stop with a human choice modal (wait vs upgrade). BR's guess
   falsified; the agent's stated prior (hard refusal or overage prompt at least as likely as a
   silent model swap) matched. The statusline-chip instrument was never needed — the harness
   answered loudly.
2. **The stall raced an absent human**: it fired while BR was at tea (+afk, minutes earlier),
   and only luck (reset already at 16:30) kept the outage to minutes. Same family as
   [[guard-against-forced-confirmations]], but the modal came from the HARNESS, not the guard —
   the agent cannot construct its way around this one.
3. **Dance-rule candidate**: at >= 9x% session limit, a human stepping away should flip the
   agent to pause-and-checkpoint rather than work into the modal; the % and reset time are ON
   the statusline, so the trigger is readable before it fires. (This run had checkpointed
   everything anyway — x3 pushes held; zero work lost.)
4. **Post-reset continuity**: the same session resumed cleanly, background probe results
   intact and consumable; in-flight background tasks survived the stall window.
5. **Stakeholder reaction (verbatim, in-feed 16:3x, cued as WR data)**: "WE MANAGED TO EAT ALL
   TOKENS :tada: F5 was maxxed out :)" — first observed full consumption of a 5h window on this
   substrate, on a deliberate TokSpend day (fresh weekly window, one post-warp solo arc + two
   minion programs + four probe sub-agents). The cap was hit by DESIGN pressure, not runaway
   waste, and BR reads it as a milestone, not an incident.
