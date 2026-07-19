# CF5 vs CO4, mode-chip hygiene — a subjective observation (BR, 2026-07-19)

**Datum (BR, verbatim intent):** BR thinks (subjectively) that CF5 seems smarter than CO4 was in
handling mode chip declarations; CO4 often forgot them and left them dangling.

**Context.** Recorded the evening of CF5 day-2 (the first post-warp session), during heavy mode-system
work: the CamelCase chip rename, the ColdStart/SmartZone additions, and a staged-screenshot episode in
which six chips were declared as props and then restored to the true state (`TokSpend, SmartZone`)
immediately after the staging plan was abandoned. The restore was agent-initiated (the declared line
must not lie), which is plausibly the concrete behaviour behind today's impression.

**Classification.** This belongs to the pre-registered "BR-subjective-CF5-sharper" informal stream
(SM164): subjective, single-observer, collected DURING use, explicitly NOT the controlled measurement.
It is a hypothesis-feeder for the SM164 subagent A/B, not evidence of the delta.

**Confounds, named honestly:**
1. **Expectation/novelty bias** — BR expects the newer model to be sharper; day-2 glow.
2. **Instrument change** — the mode system itself matured today (fewer/cleaner chips, CamelCase,
   alignment); better hygiene may partly be a better instrument, not a smarter agent.
3. **Salience** — chips were the day's working topic, so the agent's attention on them was task-driven;
   CO4's dangling chips may have occurred while chips were background state, a harder condition.
4. **Memory asymmetry** — CO4's failures accumulated over weeks; CF5 has had two days.

**Toward an objective version (SM164 candidate metric):** dangling-chip episodes are countable after
the fact — a `tt mode add` in the transcript whose matching `rm` never comes before session end (or
whose mode provably no longer held). The state file keeps no history, but the transcripts record every
`tt mode` call. A per-session "chips declared vs chips cleared-or-still-true" ratio would turn this
impression into a measured before/after, CO4 transcripts included.
