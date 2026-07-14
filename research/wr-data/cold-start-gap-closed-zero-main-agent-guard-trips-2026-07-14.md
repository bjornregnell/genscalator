# Cold-start gap closed: zero MAIN-agent guard trips across a long session (WR)

**Date:** 2026-07-14 (evening). **Model:** main = Opus 4.8 (1M). **Context fill at time of note:** ~21%.
Positive datapoint, contrasts the 2026-07-13 cold-start regressions. Ties skill-theory **P1** (salience beats
activation), SM077 (`gs warm`), and the specimen
`active-skill-still-cold-starts-dormant-reflexes-regress-2026-07-13.md`.

## Observation

Across this session's multi-hour AFK solo run — many `tt` / `git` / file calls, ~a dozen commits — the **main
agent had ZERO `guardcheck` trips**. The guard-clean reflexes (tt over raw find/grep, one bare command per call,
`run_in_command` background not pipes, Write not redirect, `tt git` not `cd && git`) held **from turn zero**
through the whole run. The only guard trip in the entire session was inside a **CF5 sub-agent** (documented in
`super-sub-agent-chat-request-response-not-ambient-2026-07-14.md`), not the main agent.

## Contrast (the thing this is evidence about)

On 2026-07-13, a fresh/clear session emitted **two brittle-bash guard trips in the FIRST bash calls** despite the
skills being active — the "summoning gap": an active-but-dormant skill is not salient at turn zero. **This
session had none.** So the cold-start summoning gap was closed here.

## What was different (candidate causes — CONFOUNDED, not isolated)

1. **Turn-zero salience via the resume-prompt.** This session opened by reading `tmp/resume-prompt.md`, whose
   FIRST block is the anti-regression forbidden→allowed checklist — so the reflexes were *in salient context* at
   turn zero, before the first bash call. This is exactly the P1 intervention (salience injection), delivered
   manually via the resume-prompt rather than by a SessionStart hook.
2. **Continuous re-exercise.** `rot-vigilance` was declared and the whole run was `tt`-based, so the reflexes were
   used constantly — maintained salience (the refresh / anti-decay lever), never allowed to go dormant.
3. **The wall-sticker banner** was added early (BR's ask) at the top of the pin board — a persistent visible
   reminder.

## Interpretation

A **positive corroboration of P1**: a turn-zero salience injection (here the resume-prompt checklist) closes the
cold-start regression the 2026-07-13 specimen showed. It also supports the **SM077 `gs warm` thesis** — the fix
is getting the reflexes salient at turn zero; the resume-prompt did that manually this session, and a hook/warm-
cue would automate it.

## Honest caveats (echt — what this is NOT evidence of)

- **N=1, observational, no ablation.** Three interventions fired at once (resume-prompt checklist + continuous
  re-exercise + banner); this run cannot isolate which mattered. That is precisely what the P1 A/B in
  `skill-theory-study-design.md` §2 is for.
- **Low-moderate fill (~21%).** This tests the **cold-start / summoning-gap** regression, NOT the high-fill /
  post-compaction **position-dependent distortion** (D rising with context position, wrinkle 3). "No regression
  in a long while" here means *across wall-clock and many calls at low-moderate fill* — the high-context and
  post-compaction stress case remains **untested this session**.
- **Salience was continuously refreshed by use.** A genuinely idle stretch followed by a lone bash call (dormancy
  without re-exercise) would be the harder test; this run did not create that condition.

## Bottom line

The turn-zero anti-regression checklist appears to have closed the cold-start gap that bit us on 2026-07-13
(P1-consistent, low-fill), with the honest caveat that the high-fill/post-compaction distortion is a different,
still-open question.
