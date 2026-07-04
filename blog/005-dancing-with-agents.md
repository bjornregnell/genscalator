# 005 — Dancing with agents

**Status: STUB.**

- **TODO: explain our "dances"** — the ritualized human+agent protocols we've evolved for running long,
  high-stakes agentic sessions. Each is a small choreography where human and agent each have steps:
  - **context dance** — human runs `/context` to read fill; agent reports fill-vs-ceiling and advises.
  - **compact dance** — agent proposes compaction at the trigger (~0.8·ceiling); human compacts; agent resumes from
    a handoff prompt. (See memory `propose-compact-dance-at-trigger`.)
  - **exit-resume dance** — save state + resume prompt → human exits and `claude --resume` to inherit a fresh process
    env (e.g. a refreshed token). (Memory `exit-resume-dance`.)
  - **hardening dance** — agent audits its own persistent config (memory / instructions / tool-shapes / allowlists)
    for misfire causes and amends them; security changes stay human-approved. (Memory `hardening-dance`.)
  - (candidates to name: the *copy-paste frame* handoff, the *commit-first-because-flaky-box* reflex.)

- **TODO: how to support the claim of their utility EMPIRICALLY?** The hard part — a dance "feels" useful, but what's
  the measurable win? Sketch: define the failure each dance prevents (context-rot collapse, lost work on a flaky box,
  a repeated misfire, a stale token stall), then find a *countable* proxy — e.g. sessions-to-collapse with vs without
  the compact dance; work-lost-events before vs after commit-first; repeat-misfire rate before vs after a hardening
  pass. Note the confound: we adopt a dance *because* we hit the failure, so pre/post isn't clean — need either a
  held-out control or an A/B across comparable runs. (Ties to the framing-as-arousal experiment discipline: honest
  DV, preregistered, report the null.)

- **TODO: mine WR data for evidence of their usefulness** — `research/wr-data/` and `research/smart-zone-ceiling.md`
  hold live instances where a dance fired (or should have). Harvest concrete episodes: the resume-skip that saved a
  ~4h sweep from a harness cull; the toolbox-divergence hardening; each compact/exit-resume cycle and what it averted.

Pairs with `research/joint-rot-vigilance-recovery-kit` (memory) and the WR-data thread. Related blog: `004` (UX).
