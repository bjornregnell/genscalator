# SM022b - usage-limit WARNING (estimate-and-warn before the cap): reqT-lang DRAFT

**Status:** DRAFT for BR review, to fold into SM022 (the super-harness dashboard PRD work) and then
PRD.md. NOT folded in yet. Parser-validated (`tt parsereqt parse` + `lint`).

## What this adds and why

On 2026-07-12 we hit the Max-plan SESSION limit mid-workflow, unobserved (62 of 78 RQ0 agents died on
it) - see `research/wr-data/hit-session-limit-unobserved-2026-07-12.md`. BR's requirement, verbatim-ish:
the super-harness (and the status line) should WARN of an APPROACHING usage limit, not just report it
after the fact. This draft reqT-formalizes that as:

- **one new Goal** - `noSurpriseUsageHalt`: the pair never hits a session (5-hour) or weekly usage cap
  unobserved; measure-and-warn before the wall, not report at the wall (same family as the smart-zone /
  rot gauges).
- **one new Feature** - `usageLimitWarning`: estimate session + weekly consumption from the
  `rate_limits` fields the Claude Code statusLine stdin JSON already carries, and warn at a configurable
  threshold (default 80 percent) BEFORE the hard limit - surfaced ambiently in `tt statusline` and as a
  usage panel in the super-harness dashboard.
- **one new Target** - `unwarnedLimitHits`: hard limit hits not preceded by a warning; honest value 0.

This is the requirements form of the **session-limit dance** and **weekly-limit dance** STUBs in
`docs/foundations.md` (both BR 2026-07-12): pre-caution = ESTIMATE the remaining budget against the
reset, then throttle, checkpoint (commit + save state), or defer heavy compute (a big agent fan-out)
sized to what is left. The feature makes the estimate ambient instead of a human paste-and-ask.

## To fold in (BR)

Add the block below to PRD.md alongside the SM022 dashboard block (v2.0 super-harness section). NOTE a
placement option: `usageLimitWarning`'s statusline surfacing needs only the SHIPPED `ttStatusline` (v0.9.0,
SM039), so a thin slice (threshold warning on the line) could land in a NEAR release, with the dashboard
usage panel and burn-rate projection following in v2.0. `agentSoundCues` and `superHarnessDashboard`
relations assume the SM022 draft block is folded in too.

```
* Goal: noSurpriseUsageHalt has
  * Gist: the human-agent pair never hits a session (5-hour) or weekly usage cap unobserved - an approaching limit is warned about in time to throttle, checkpoint (commit + save state), or defer heavy compute sized to the remaining budget.
  * Why: a hard cap mid-workflow stalls work in a possibly uncommitted state and wastes the spend that led up to it; neither party watches the burn unaided (the human cannot see the meter mid-flow, the agent cannot self-read usage, a fanned-out agent swarm has no shared budget view). Origin: research/wr-data/hit-session-limit-unobserved-2026-07-12.md; operationalizes the session-limit and weekly-limit dances in docs/foundations.md.
* Goal: noSurpriseUsageHalt helps Goal: jointHumanAgentProductivity
* Goal: noSurpriseUsageHalt helps Goal: maximizeUsefulAutonomy
* Goal: noSurpriseUsageHalt helps Goal: manageInferenceCost

* Feature: usageLimitWarning has
  * Gist: an estimate-and-warn gauge for the session (5-hour) and weekly usage limits - warns at a configurable threshold BEFORE the hard cap, so the pair can throttle, checkpoint, or defer a heavy fan-out instead of discovering the limit only when blocked.
  * Spec: SOURCE - the Claude Code statusLine stdin JSON already carries rate_limits.five_hour.used_percentage, rate_limits.seven_day.used_percentage, and resets_at (the fields ttStatusline formats); no new telemetry, no computation upstream, all read locally on-box.
  * Spec: WARN - when either used_percentage crosses a configurable threshold (default 80 percent, a candidate ttConfigFile key), surface a prominent warning marker in the tt statusline line and a usage panel entry in the superHarnessDashboard; degrade gracefully when rate_limits is absent (non-subscription).
  * Spec: ESTIMATE - beyond the stateless threshold check, keep a small local on-box history of per-turn readings to estimate the burn rate and project time-to-cap against resets_at, so the warning can say "at this rate the session cap hits BEFORE the reset" and a planned fan-out can be sized to the remaining budget.
  * Why: warning is cheap prevention where the miss is expensive - the 2026-07-12 session-cap hit stalled a 78-agent workflow mid-run; a pre-warning would have let us checkpoint and size the burst to headroom. Measure-and-warn before the wall, not report at the wall.
* Feature: usageLimitWarning helps Goal: noSurpriseUsageHalt
* Feature: usageLimitWarning helps Goal: completeSuperHarness
* Feature: usageLimitWarning helps Goal: manageInferenceCost
* Feature: usageLimitWarning requires Feature: ttStatusline
* Feature: usageLimitWarning relatesTo Feature: superHarnessDashboard
* Feature: usageLimitWarning relatesTo Feature: agentSoundCues
* Feature: usageLimitWarning relatesTo Feature: ttConfigFile

* Target: unwarnedLimitHits has
  * Gist: count of hard usage-limit hits (session or weekly) NOT preceded by a threshold warning; the honest value is zero - every hit was warned about first.
  * Max: 0
* Target: unwarnedLimitHits verifies Goal: noSurpriseUsageHalt
```

## Open for BR (design confirms, not blockers)

- **No `hurts` relations, deliberately.** The feature reads harness-provided local JSON and emits a local
  warning - it adds no new attack surface and no egress, so wiring a bhh `hurts` link would be decorative.
  The data-sovereignty guardrail is inherited from `dashboardDataSovereignty` via `superHarnessDashboard`
  (SM022 draft). Add `hurts` links only if you want the zero-egress claim traceable HERE too.
- **Slice or not:** the `requires Feature: ttStatusline` link plus the placement note above lets you ship
  the ambient threshold warning early (statusline-only) and keep the projection + panel in v2.0. If you
  slice, the Spec WARN and ESTIMATE lines split naturally.
- **Threshold default:** 80 percent mirrors the compact-dance trigger convention (0.8 of Z); pick another
  if session and weekly deserve different defaults (a fast-burning session window may want an earlier
  warning than the slow weekly one).
- **`agentSoundCues` tie:** an approaching-limit event is a natural sound-cue class (a gentle warning
  bing); the `relatesTo` records it - the cue mapping itself stays in the agentSoundCues Spec.
