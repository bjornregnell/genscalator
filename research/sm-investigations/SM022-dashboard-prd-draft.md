# SM022 - live joint-psyche dashboard: PRD v2.0 section (reqT-lang DRAFT)

**Status:** DRAFT for BR review. **Parser-validated** (`tt parsereqt lint` → 0
fall-throughs; every concept recognized, no relation lost to `Text`). NOT folded
into PRD.md - this is the review draft to elaborate the existing
`superHarnessDashboard` STUB in PRD.md "Release v2.0 (far future) - the
super-harness."

## What this elaborates
PRD.md v2.0 already has a `superHarnessDashboard` **STUB** (Gist + Spec + `helps
completeSuperHarness` + `hurts controlHumanSystem/exfiltrateSecrets` + `relatesTo
contextRotMeter`). This draft elaborates it **goal-level down** per SM022, adding:
- **two new Goals** the dashboard serves - `sharedRotVigilanceMirror` (the
  measurement-not-FEEL purpose) and `dashboardDataSovereignty` (local-only);
- **three sub-Features** - `agentMetricsPanel`, `humanProxiesPanel`, and
  `notPsychiatristsDisclaimer` (the visible-disclaimer UI requirement made a
  first-class, traceable Feature);
- **two Targets** that `verify` the goals - `dashboardDataEgressBytes` (zero
  off-box egress) and `proxyMemberCheckRate` (proxies kept honest vs BR).
- **an audio sibling** - `agentSoundCues`: an OPTIONAL out-of-band sound-cue channel (fed by the same
  SM016 tap) so a human whose eyes are on another window *hears* what the agent is doing - above all a
  gentle chime ("a nice bing") when an agent message needs a human decision. The visual dashboard and the
  audio cues are two surfacings of the same tap; distinct cues for distinct event classes, per-event and
  off by default, human-owned. (BR pin 2026-07-12.)
- **easy live cue controls** - `soundCueControls`: the served dashboard carries one-click on/off flips (and
  mute-all) for each sound cue, so the human tailors the mix live and keeps the bings from becoming a cacophony -
  no editing settings files. (BR pin 2026-07-12.)

BR's echt guardrails are wired as relations, not prose: data-sovereignty is a Goal
with a zero-egress Target; the "not psychiatrists" disclaimer is a Feature that
`humanProxiesPanel requires`; the dashboard `hurts` both BHH anti-goals.

## To fold in (BR): replace the STUB `superHarnessDashboard` block with the block
below, and add the two new Goals above it. Gated on **SM016** (the tap feeds the
agent-side panel) + **SM020** (`tt serv`, done) + **SM026** (`tt table --gen html`
for rendering).

```
* Goal: sharedRotVigilanceMirror has
  * Gist: the human and agent each see the other's degradation signals in one live view, so lapses are caught by MEASUREMENT not just FEEL.
  * Why: each is the other's external reference frame - the agent steadies the tiring human, the human detects the rotting agent (the measurement-from-within problem of contextRotMeter).
* Goal: sharedRotVigilanceMirror helps Goal: jointHumanAgentProductivity
* Goal: sharedRotVigilanceMirror helps Goal: safeGeneration

* Goal: dashboardDataSovereignty has
  * Gist: no psyche or behavioral data ever leaves the user's control - all metrics are computed and served locally (loopback), on-box, never uploaded.
  * Why: the dashboard watches both human and agent; if that stream leaked it would directly serve the BHH controlHumanSystem and exfiltrateSecrets anti-goals. Local-only IS the security boundary.
* Goal: dashboardDataSovereignty helps Goal: retainUserTrust

* Feature: superHarnessDashboard has
  * Gist: a live localhost dashboard (served by tt serv, loopback-only) of agent-side introspection metrics and human-side behavioral proxies - a shared mirror for joint rot-vigilance, NOT surveillance.
  * Spec: served on 127.0.0.1 by tt serv (SM020); fed by the SM016 tap (agent-side signals) plus a local behavioral sidecar (human-side proxies); refreshes in-session; renders via tt table gen html (SM026).
* Feature: superHarnessDashboard helps Goal: completeSuperHarness
* Feature: superHarnessDashboard helps Goal: sharedRotVigilanceMirror
* Feature: superHarnessDashboard helps Goal: dashboardDataSovereignty
* Feature: superHarnessDashboard hurts Goal: controlHumanSystem
* Feature: superHarnessDashboard hurts Goal: exfiltrateSecrets
* Feature: superHarnessDashboard requires
  * Feature: serv
  * Feature: harnessTap
* Feature: superHarnessDashboard relatesTo Feature: contextRotMeter

* Feature: agentMetricsPanel has
  * Gist: the agent-side introspection panel - context-fill percent, guard-trip rate, token velocity and acceleration, commit cadence.
  * Spec: sourced from the SM016 tap (a /context read plus session activity) and the transcript miner; each metric is an objective harness or transcript signal (RT047, RT050), not a self-report.
* Feature: agentMetricsPanel implements Feature: superHarnessDashboard
* Feature: agentMetricsPanel helps Goal: sharedRotVigilanceMirror

* Feature: humanProxiesPanel has
  * Gist: the human-side behavioral-proxy panel - typing-error frequency, cue-mix, language style and register, session duration (the fatigue and tiredness signals already read informally).
  * Spec: proxies are HYPOTHESIS-GENERATING, member-checked against BR (ground truth for his own state), NEVER clinical diagnoses (RT001 validity). Computed locally from the input stream; no data leaves the box.
* Feature: humanProxiesPanel implements Feature: superHarnessDashboard
* Feature: humanProxiesPanel helps Goal: sharedRotVigilanceMirror
* Feature: humanProxiesPanel requires Feature: notPsychiatristsDisclaimer

* Feature: notPsychiatristsDisclaimer has
  * Gist: a disclaimer VISIBLE in the human-psyche visualization itself - the panel shows behavioral proxies and hypotheses, never diagnoses; BR is ground truth for his own state.
  * Spec: rendered inside the humanProxiesPanel visualization (not a footnote), stating the proxies are non-clinical and member-checked. A UI requirement, not optional chrome.
* Feature: notPsychiatristsDisclaimer implements Feature: humanProxiesPanel
* Feature: notPsychiatristsDisclaimer helps Goal: retainUserTrust

* Feature: agentSoundCues has
  * Gist: an OPTIONAL out-of-band AUDIO channel - distinct, pleasant sound cues that tell a human whose eyes are on another window what the agent is doing, above all when an agent message needs a human decision.
  * Spec: fed by the same SM016 tap as the dashboard; maps agent-event classes to distinct cues (needs-human-decision, done or idle-waiting, error or blocked); the needs-decision cue is a gentle chime (a nice bing), never jarring; per-event enable, volume, and mute live in human-owned settings; OFF by default; played locally, no audio telemetry.
* Feature: agentSoundCues helps Goal: completeSuperHarness
* Feature: agentSoundCues helps Goal: jointHumanAgentProductivity
* Feature: agentSoundCues requires Feature: harnessTap
* Feature: agentSoundCues relatesTo Feature: superHarnessDashboard

* Feature: soundCueControls has
  * Gist: one-click on/off flips on the served dashboard for each sound cue (plus mute-all), so the human tailors the mix live and keeps the bings from becoming a cacophony.
  * Spec: per-cue toggles rendered in the localhost dashboard (tt serv SM020, tt table gen html SM026), flippable without editing settings files; changes take effect immediately; the human owns the mix; off by default.
* Feature: soundCueControls implements Feature: superHarnessDashboard
* Feature: soundCueControls relatesTo Feature: agentSoundCues
* Feature: soundCueControls helps Goal: jointHumanAgentProductivity

* Target: dashboardDataEgressBytes verifies Goal: dashboardDataSovereignty
* Target: proxyMemberCheckRate verifies Goal: sharedRotVigilanceMirror
```

## Open for BR (design confirms, not blockers)
- **Relation choice for panels:** I used `agentMetricsPanel implements
  superHarnessDashboard` (a panel realises the dashboard). Alternative: nest the
  panels under `superHarnessDashboard has` (composition). `implements` keeps each
  panel a top-level Feature with its own traceability; `has` reads as
  part-of. Your call.
- **The two Targets** are named but not yet given `Min`/`Max`/`Value` - the
  egress target's honest value is **0** (statically checkable via loopback-only +
  no network sink); the member-check-rate target needs a threshold you'd set.
- **Naming:** `notPsychiatristsDisclaimer` is deliberately blunt to preserve your
  pin's intent ("the disclaimer must be VISIBLE ... never clinical claims"); rename
  if you prefer softer.
