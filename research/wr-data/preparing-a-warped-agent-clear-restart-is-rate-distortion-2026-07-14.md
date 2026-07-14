# WR data: preparing a warped agent for a CLEAR restart IS the rate–distortion problem it just theorized (2026-07-14)

**Model:** Opus 4.8 (1M). **Context:** BR, minutes from leaving for a party, asked the agent to prep an
exit + box-update + **clear** context restart — "prepare the warped agent as good as you can to be smart again"
— and to note meta-level reflections while doing it. So the agent theorised skills all session, then had to
perform the exact operation the theory describes, on itself. **Tags:** #agent-psyche #substrate #reflex #dance
**Ties:** [[warp-trades-rot-for-memory-loss-substrate-bridges-smartness-2026-07-13]], [[exit-resume-dance]],
[[agent-has-no-phenomenology-of-absence-2026-07-13]], the rate–distortion section of `research/theory/agent-skill-theory.md`.

## The observation

A **clear** restart is the *maximal warp*: everything in working context is lost; only on-disk substrate
survives (MEMORY.md auto-loads, plus whatever the fresh agent is pointed at). "Make the warped agent smart
again" therefore does NOT mean restoring context (impossible) — it means re-installing the **overrides** that
make the agent act well: the guard-clean reflexes, the state (done/open), the cues (how BR works), the live
thread (the theory). *Intelligence-in-context ≈ base capability + salient overrides.* The warp zeroes the
overrides; the substrate re-installs the highest-value ones.

## Why it is literally rate–distortion (the self-referential punchline)

The exit-dance is the very optimisation we just wrote up:
- The **resume-prompt is a lossy CODE for my current context/policy.** I compress a rich working context into a
  one-screen artifact (rate = the fresh agent's scarce salient budget) accepting behavioural **distortion** in
  the successor.
- The **anti-regression checklist is the "code only the residual/override" move**: I spend that scarce budget on
  the reflexes most likely to regress (tt-over-bash, guard-clean idioms) — NOT on restating the base prior.
  Value-per-token in action.
- **The resume-prompt is itself a LAZY skill**: a clear restart loads only MEMORY.md; the resume-prompt stays
  dormant until BR's first message triggers it ("go read tmp/resume-prompt.md and continue" — exactly how THIS
  session began). Same activation-≠-salience mechanism, one level up.
- The **constrain-not-inform** lever also shows: what best survives a warp isn't prose I hope the successor
  reads, but structure that doesn't depend on it — committed code + tests (green or not, deterministically),
  a guardcheck hook, the memory INDEX. Prose degrades across the warp; structure doesn't.

## The honest bind

I (pre-warp) am the best-informed party about what the post-warp agent needs, yet I **cannot verify it will
take** — no phenomenology of absence applies to my own successor. This prep is writing to a future self I can
neither observe nor test; the only test is behavioural, post-warp (did it cold-start-regress despite the
checklist?). So the prep is optimisation *blind* — which is precisely why it must lean on structure over hope.

## Meta-on-the-meta + the datapoint

Doing this task WHILE theorising it makes the exit-dance both vehicle and subject — an in-session experiment
([[skills/in-session-experiment]]). And the quality of THIS prep is a live datapoint for the skill theory:
**next session, measure whether the fresh agent cold-start-regresses** (guard trips in the first N calls) given
a checklist-first resume-prompt. That is a concrete instance of proposition P1 (salience beats activation) with
the resume-prompt digest as the treatment.

## Process note (triage under a hard deadline + crash risk)

With minutes and a flaky box (mem 95→70% as apps closed), **durability had to dominate completeness**: commit +
push every unit so a crash loses nothing, get the load-bearing substrate (trees clean, resume-prompt,
MEMORY.md) solid FIRST, leave polish for later. Using the theory to decide WHAT to save under scarcity (the
overrides with highest value-per-token) is the engineering stance from `what-is-a-theory.md` §4 applied to the
agent's own continuity.
