# 005 — Dancing with agents

**Status: SCAFFOLD** (agent-drafted catalog + framing; prose flagged `[for BR to voice]`). Was STUB.

> **The 004→008 arc** (map in 004). **You are here: the Practice** — the dances. *Backwards cliffhanger:* these
> rituals didn't come from nowhere — a *method* surfaced them, and a *theory* explains why they work. → Next (006):
> the method that catches these patterns as data.

**[figure — TODO, real data preferred]** This session's **context-fill over time with dance-events marked** — the
note-dance offload points and the compaction at ~90% — showing *raw fill* climbing while capability holds (the "capable
at 0.88 fill" datapoint): the empirical hook for the *how-to-support-the-claim* TODO below. Companion/alternative: a
**two-lane choreography diagram** (human steps ‖ agent steps) for each named dance. Source:
`research/wr-data/harness-ux.md`, `research/smart-zone-ceiling.md`.

## What a "dance" is `[for BR to voice]`

A **dance** is a ritualized human+agent protocol for running long, high-stakes agentic sessions — a small choreography
where *each partner has steps* and the steps interlock. We didn't design these up front; they **precipitated out of
friction** (the Pains of 004), got a name so they'd be reusable, and then became load-bearing. Naming matters: an
unnamed protocol has to be re-improvised every time it's needed — under fatigue, exactly when it's hardest. A named
dance is a **cheap shared handle** ("let's context-dance") that both partners can invoke without re-explaining. The
deeper claim, picked up in 007: every dance is an **externalization ritual** — it moves state out of the fragile place
(the human's tiredness, the agent's filling context) into a durable substrate (a memory file, a commit, a resume
prompt). That's *why* they work; here we just catalog them.

## The catalog

Each entry: the one-line move · **human steps ‖ agent steps** · the failure it prevents · the durable memory it lives
in. `[for BR to voice — tighten, cut, add the ones I missed]`

- **context dance** — read the gauge together. **Human:** runs `/context` to surface raw fill. ‖ **Agent:** reports
  fill-vs-ceiling and whether we're inside the smart zone, and advises (proceed / consolidate / compact soon).
  *Prevents:* flying blind into context rot. *Note:* the human often can't read `/context` mid-turn (a 004 pain), so
  the agent narrating fill on request is the workaround.

- **compact dance** — consolidate before the cliff. **Agent:** proposes compaction as fill crosses the trigger
  (~0.8·`Z`, the smart-zone ceiling), saves what needs saving, and hands over a **resume prompt**. ‖ **Human:**
  compacts, pastes `/context` + the resume prompt, says "go". *Prevents:* context-rot collapse and the late-90%
  scramble. *Memory:* [[propose-compact-dance-at-trigger]]. *This session:* run twice (mid and end); the second is the
  one whose resume prompt you just pasted.

- **note dance** — the streaming counterpart to the compact dance. **Human:** drops a cue — `note:` or `WR data:` —
  the moment something worth keeping surfaces. ‖ **Agent:** offloads it out of context into the right durable item (a
  memory file, a research note, a todo). *Prevents:* good observations dissolving as fill climbs; it's what keeps the
  *effective* working set small even while raw fill rises (the mechanism behind "capable at 0.88 fill"). *This session:*
  named here, and used continuously — most of this session's research notes entered via a `note:`/`WR data:` cue.
  Formally: the **longitudinal externalization dance** (continuous), where the compact dance is the **discrete** one.

- **exit-resume dance** — inherit a fresh process. **Agent:** saves state + a resume prompt. ‖ **Human:** exits and
  `claude --resume` so the new process inherits a clean environment (e.g. a refreshed auth token) without losing the
  thread. *Prevents:* a stale-token / degraded-process stall mid-run. *Memory:* [[exit-resume-dance]]. *Sibling of* the
  compact dance — same save+handoff spine, different trigger (process health vs context fill).

- **hardening dance** — the agent audits *itself*. **Agent:** reviews its own persistent config — memory, instructions,
  tool-shapes, allowlists — for misfire causes and proposes durable fixes. ‖ **Human:** curates and approves; **security
  changes stay human-approved** (the human is the authority anchor — see 009 / the corroboration asymmetry). *Prevents:*
  a repeated misfire from re-firing forever (the "introspection isn't self-control → change the structure" thesis).
  *Memory:* [[hardening-dance]]. *This session:* the allowlist/settings review is **queued** for BR (accreted
  fetch-proxies, `Bash(ssh *)`, broad globs) — a pending instance, not yet executed, precisely because the approval is
  the human's.

- **copy-paste-frame dance** — a handoff micro-ritual. **Agent:** wraps any copy-target block (resume prompt, snippet
  to paste elsewhere) in `---` horizontal-rule fences. ‖ **Human:** spots the frame, selects cleanly, pastes. *Prevents:*
  fumbling a critical paste under fatigue (a dropped closing fence was a real rot signal this session). *Memory:*
  [[copy-paste-frame-rule]].

- **(candidate, not yet firm) commit-first-because-flaky-box** — commit+push frequently so a flaky environment or a
  harness cull can't take unsaved work. Part of the joint-rot recovery kit ([[joint-rot-vigilance-recovery-kit]]); name
  it or fold it into the recovery-kit prose.

## The dances form a family `[for BR to voice]`

Two axes organize them. **Discrete vs continuous:** compact / exit-resume fire at a moment; the note dance runs
continuously in the background. **Who initiates:** the human cues the note dance and the context read; the agent
proposes the compact and hardening dances. But every one is the *same underlying move* — externalize fragile state into
a durable substrate before the fragile place fails. That common spine is why they feel like a family and not a grab-bag,
and it's the thread 007 pulls on.

## TODO: how to support the claim of their utility EMPIRICALLY?

The hard part — a dance "feels" useful, but what's the measurable win? Sketch: define the failure each dance prevents
(context-rot collapse, lost work on a flaky box, a repeated misfire, a stale-token stall), then find a *countable* proxy
— e.g. sessions-to-collapse with vs without the compact dance; work-lost-events before vs after commit-first;
repeat-misfire rate before vs after a hardening pass. Note the confound: we adopt a dance *because* we hit the failure,
so pre/post isn't clean — need either a held-out control or an A/B across comparable runs. (Ties to the
framing-as-arousal experiment discipline: honest DV, preregistered, report the null.) **Cross-model caveat (this
project's near-term plan):** dance efficacy is likely *model-dependent*, so any before/after must be attributable to the
frontier model (Opus 4.8 vs Fable 5) — capture the baseline before the switch or the comparison is confounded.

## TODO: mine WR data for evidence of their usefulness

`research/wr-data/` and `research/smart-zone-ceiling.md` hold live instances where a dance fired (or should have).
Harvest concrete episodes: the resume-skip that saved a long sweep from a harness cull; the toolbox-divergence
hardening; each compact / exit-resume cycle and what it averted; every `note:`/`WR data:` offload as a note-dance
datapoint.

---

Pairs with [[joint-rot-vigilance-recovery-kit]] and the WR-data thread. Related blog: `004` (the Pains these dances
answer), `009` (echt — the authority-anchor half of the hardening dance).
