# WR specimen: gs cues / gs dances need a ready-to-grab substrate, not live synthesis (2026-07-13)

**Observation (BR, live).** BR ran `gs cues` then `gs dances`. Both produced good, complete lists — but the
agent had to **think for a while** to synthesize each one from the `cue-*` memories + `docs/foundations.md`,
grouping and glossing on the fly. BR: "you had to think quite a while to produce these lists on cues and
dances; we should have a ready-to-grab thing in substrate."

## The friction
`gs cues` / `gs dances` / `gs term` are currently **synthesis** commands: the agent reads scattered sources
(the coined-term entries in foundations, the ~30 `cue-*` memories) and reconstructs a grouped, glossed list
every time. That is slow and token-costly, and — because it is reconstruction — it is also a **rot / accuracy
surface** (a synthesized list can drift or drop an entry, and the agent cannot self-certify completeness,
family E). It repeats the same work on every invocation with no caching.

## The fix
A **ready-to-grab registry** in substrate that these commands READ and lightly format, instead of
synthesizing. Candidate shape: a generated `docs/gs-registry.md` (or per-kind files: cues / dances / terms),
each entry a one-line meaning + its direction (human->agent / agent->human), ordered and grouped once, kept in
sync with `foundations.md` as the canonical source. Then `gs cues` / `gs dances` / `gs term` become
**read-and-render** (cheap, deterministic, complete) rather than read-many-and-reconstruct. This also makes
them safe **sub-agent / cheap-model** jobs and even inline-instant.

Pinned as **SM058** (safe solo, agent-authored doc). This is the same move as substrate-grounding generally:
precompute the deterministic artifact once, read it thereafter, instead of re-deriving from memory each time.

## Ties
[[cue-gs-dwim-commands]], the gs-dwim delegation policy (a registry makes these commands trivially
delegatable), the substrate-grounding theme (read the artifact, don't reconstruct), family E (the agent can't
certify a synthesized list is complete — a fixed registry can be reviewed once and trusted).
