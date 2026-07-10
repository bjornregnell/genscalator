# Cost snapshot: this engagement = $774, Opus-dominated, driven by high-context re-read (2026-07-10)

BR-flagged, pinned to the **$-cost axis of Token Efficiency (SM039)**. A `/cost` + `/context` read near the end of
a ~4-day engagement, for the money-axis of TE ([[br-funds-claude-privately]] - BR pays privately).

## The snapshot
- **Total: $774.28** (API 23h 41m; wall 4d 0h 37m). Code changes: 19,791 lines added, 2,976 removed.
- **By model:** Opus **$724.79 (94%)** [557.6k in, 5.2m out, **951.5m cache-read**, 12.0m cache-write] · Fable
  **$48.54 (6%)** [165.7k in, 319.5k out, 6.9m cache-read] · Haiku **$0.95 (~0%)** [459.2k in, 24.5k out, 13 web
  searches].
- **Fill at read:** 79% (766.5k message tokens). Budgets: session 69%, weekly all-models 13%, Fable 9%.
- **Drivers (the tool's own diagnosis, last 24h):** 100% subagent-heavy sessions · 100% sessions 8h+ · **90% of
  usage at >150k context.**

## The TE reading
1. **The SUPER-AGENT is ~all the cost.** Opus (me) = 94%; the Fable + Haiku sub-agents together = 6%. So the cost
   lever is the super-agent's turns times context, NOT the sub-agents. Delegation is cheap; the resident Opus
   context is what's expensive.
2. **Cache-read dominates the token volume: 951.5m cache-read** vs 5.2m output. This IS the ">150k context is
   expensive even cached" effect - every turn re-reads the whole large cached context. **Cost tracks RAW
   context-fill, re-read each turn.**
3. **Tension with the substrate thesis:** the pin-dance keeps the *effective working-context* small (capability),
   but does NOT shrink the *raw fill* that drives cache-read cost. **Compacting is the only thing that resets the
   raw fill** (and thus the per-turn re-read cost). So capability is bought by externalisation; COST is cut by
   compacting - two different levers (cf. the three-size-measures: cost tracks context-fill, not durable-substrate
   size).
4. **Cheapest cost levers, in order:** (a) **compact more often** (reset the >150k re-read - the single biggest
   lever); (b) **shorter sessions**; (c) **cheaper model for simple sub-agents** (Haiku, not Fable/Opus - tonight's
   term-surveys and verifiers could have been Haiku; the tool flagged exactly this).
5. **Money reality:** $774 of BR's private money for ~4 days - real cost-consciousness; the biggest saving is
   PROCEDURAL (compact/clear cadence), not capability-cutting.

## Delegation strategy: double-race Haiku vs Fable-5 (BR idea, 2026-07-10)
BR: when delegating to Haiku (cheap), consider **double-race-delegation** - run the SAME task on BOTH Fable-5 AND
Haiku in parallel and compare. Agent WDYT:
- **As CALIBRATION: valuable.** Racing the two on a task-class reveals whether the cheap tier suffices - build a
  map of Haiku-sufficient vs needs-Fable tasks (genuine TE data: when can you drop a tier?). It is the
  triangulation / independence principle (the ChatGPT experiment; the adversarial-subagent finding) applied at the
  sub-agent tier.
- **As a cheap ENSEMBLE:** two independent cheap models AGREEING = a confidence signal (trust it); DIVERGING =
  escalate to Fable/Opus. For verifiable tasks (schema'd surveys, fact-checks) this is a near-free confidence
  check (Haiku ~$0, Fable cheap).
- **NOT a routine steady-state saver:** running BOTH every time is 2x sub-agent cost for no saving once
  calibrated. The SAVING move is Haiku ALONE on calibrated-simple tasks; double-race is the calibration investment
  + the confidence tool, run while learning or when confidence matters.
- **Caveat:** for HARD tasks neither cheap tier suffices (both fail) - then Opus. Double-race is for the
  simple/verifiable class. Net: adopt it as a calibration + ensemble move (not always-on); absolute sub-agent cost
  is trivial, so the experiment is near-free. Ties: [[delegation-dance]], RT029 (cross-model), the triangulation
  principle.

Ties: SM039, [[token-budget-modes]], [[propose-compact-dance-at-trigger]] (compacting = the cost lever),
[[agent-surfaces-substrate-size-measures-three-kinds-2026-07-10]] (cost tracks context-fill),
[[br-funds-claude-privately]].
