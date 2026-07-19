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

---

## Follow-up datum (BR, same evening, appended): the harness confound

**Datum (BR):** BR realizes the new smartness of CF5 might also be harness updates adapted to the new
model; difficult to know, as it is secret sauce of Anthropic. When we go into the CO4/CF5 comparison
experiment we should have a hypothesis on this.

**Unpacking.** "The model got smarter" and "the harness got retuned for the model" are confounded from
the outside: Claude Code ships model-adapted scaffolding (system prompts, tool descriptions, agent
plumbing) whose changes are not publicly itemized, and the CF5 warp coincided with harness upgrades
(v2.1.215 at the warp). Any felt delta is the SUM of model + harness-fit + their interaction.

**Consequence for the SM164 A/B design:**
- The subagent A/B runs both models under the SAME harness version, which *controls* the harness
  variable for the comparison itself — good.
- BUT the shared harness is presumably tuned FOR CF5, so CO4 may run below its own-era form
  (harness-model fit penalty). The A/B then measures "CO4-in-CF5's-harness vs CF5-in-its-harness",
  not "CO4-at-its-best vs CF5-at-its-best". State this as an explicit threat to construct validity.
- Pre-registered hypotheses to carry into the design: **H-model** (capability delta exists under an
  identical harness), **H-fit** (part of the observed delta is harness-model fit, testable in part by
  pinning an older Claude Code version for CO4 runs, if version pinning proves feasible).

