# 005 — Dancing with agents

**Status: SCAFFOLD** (agent-drafted catalog + framing; prose flagged `[for BR to voice]`). Was STUB.

> **The 004→008 arc** (map in 004). **You are here: the Practice** — the dances. *Backwards cliffhanger:* these
> rituals didn't come from nowhere — a *method* surfaced them, and a *theory* explains why they work. → Next (006):
> the method that catches these patterns as data.

**[figure — TODO, real data preferred]** This session's **context-fill over time with dance-events marked** — the
pin offload points and the compaction at ~90% — showing *raw fill* climbing while capability holds (the "capable
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

- **pin dance** (formerly "note dance", then briefly "etch") — the streaming counterpart to the compact dance. **Human:** drops a cue —
  `pin:` ("save this durably — you pick where") or `WR data:` (pin to the WR corpus) — the moment something worth
  keeping surfaces. ‖ **Agent:** persists it out of context into the right durable item (a memory file, a research note,
  a todo), **choosing the home** and **questioning whether it earns a durable slot**. *Prevents:* good observations
  dissolving as fill climbs; it's what keeps the *effective* working set small even while raw fill rises (the mechanism
  behind "capable at 0.88 fill"). Formally: the **longitudinal externalisation dance** (continuous) — the *consolidation*
  stage of memory, where the compact dance is the **discrete** one.
- **note dance** (the *notice* cue — distinct from pin) — **Human:** `note:` = "notice this, keep it fluent for
  **this** conversation, and treat it as a **pin-candidate**." ‖ **Agent:** keeps it salient **and nominates**
  promising ones for pinning (*"want me to pin this?"*). The **attention / encoding** stage to the pin dance's
  **consolidation** stage. Pipeline: `note:` → (agent nominates) → `pin:`. *This session:* the split was coined live —
  the overloaded "note:" turned out to mean two things (attend-now vs save-durably).

- **exit-resume dance** — inherit a fresh process. **Agent:** saves state + a resume prompt. ‖ **Human:** exits and
  `claude --resume` so the new process inherits a clean environment (e.g. a refreshed auth token) without losing the
  thread. *Prevents:* a stale-token / degraded-process stall mid-run. *Memory:* [[exit-resume-dance]]. *Sibling of* the
  compact dance — same save+handoff spine, different trigger (process health vs context fill).

- **go dance** — the greenlight / autonomy handoff. **Human:** cues `go` — "proceed on the current plan, your judgment,
  I'm stepping back." ‖ **Agent:** executes the *scoped* plan autonomously, **inside** the standing guardrails
  (destructive git human-only, settings human-approved, no new-domain surfing), **minimising interrupts** (bare
  allowlist-matchable commands, batch, don't pin the human), surfacing only real decisions or the finished result.
  *Mode-switch from* **ballgame** *(you in every volley) to* **autonomous**; where `note:`/`pin:` are about memory,
  `go` is about authorization. *Clears the dance bar:* ≥2 interlocking steps (human greenlight ‖ agent execute+report).

- **hardening dance** — the agent audits *itself*. **Agent:** reviews its own persistent config — memory, instructions,
  tool-shapes, allowlists — for misfire causes and proposes durable fixes. ‖ **Human:** curates and approves; **security
  changes stay human-approved** (the human is the authority anchor — see 009 / the corroboration asymmetry). *Prevents:*
  a repeated misfire from re-firing forever (the "introspection isn't self-control → change the structure" thesis).
  *Memory:* [[hardening-dance]]. *This session:* the allowlist/settings review is **queued** for BR (accreted
  fetch-proxies, `Bash(ssh *)`, broad globs) — a pending instance, not yet executed, precisely because the approval is
  the human's.

- **consistency sweep** — repair drift in the substrate itself. **Human:** cues it, or greenlights it as an AFK task.
  ‖ **Agent:** fans out **read-only** auditors across the substrate (memory / docs / blog / research / tools), reports
  findings, **auto-fixes only the unambiguous** (dead links, typos, doc↔code drift), and **surfaces the judgment-calls**
  for the human. *Prevents:* dangling-pointer / stale-ref / term-drift / contradiction accumulation — **substrate rot**.
  *Sibling of the hardening dance:* hardening audits the agent's **config**; the consistency sweep audits the
  substrate's **content**. *This session:* its first run — a 4-agent sweep that caught index-rot in `research/README.md`,
  stale "not-yet-run" experiment statuses, and ~10 auto-fixed dead-links / doc-drifts.

- **copy-paste-frame dance** — a handoff micro-ritual. **Agent:** wraps any copy-target block (resume prompt, snippet
  to paste elsewhere) in `---` horizontal-rule fences. ‖ **Human:** spots the frame, selects cleanly, pastes. *Prevents:*
  fumbling a critical paste under fatigue (a dropped closing fence was a real rot signal this session). *Memory:*
  [[copy-paste-frame-rule]].

- **(candidate, not yet firm) commit-first-because-flaky-box** — commit+push frequently so a flaky environment or a
  harness cull can't take unsaved work. Part of the joint-rot recovery kit ([[joint-rot-vigilance-recovery-kit]]); name
  it or fold it into the recovery-kit prose.

## The dances form a family `[for BR to voice]`

Two axes organize them. **Discrete vs continuous:** compact / exit-resume / consistency-sweep fire at a moment; the
**pin dance** runs continuously in the background (with the **note dance** as its attention-stage precursor). **Who
initiates:** the human cues the pin + note dances, the context read, and the sweep; the agent proposes the compact and
hardening dances. A third cut groups them by **function**: *loss-prevention*
(compact, pin, exit-resume — save state before a failure; the **note** dance is pin's attention precursor), *audit / repair* (**hardening** audits the agent's config;
the **consistency sweep** audits the substrate's content), and *coordination micro-rituals* (context read,
copy-paste-frame). But every one is the *same underlying move* — externalize fragile state into a durable substrate
before the fragile place fails, or repair the substrate once drift has crept in. That common spine is why they feel like
a family and not a grab-bag, and it's the thread 007 pulls on.

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
hardening; each compact / exit-resume cycle and what it averted; every `pin:`/`WR data:` offload as a pin-dance
datapoint.

---

Pairs with [[joint-rot-vigilance-recovery-kit]] and the WR-data thread. Related blog: `004` (the Pains these dances
answer), `009` (echt — the authority-anchor half of the hardening dance).
