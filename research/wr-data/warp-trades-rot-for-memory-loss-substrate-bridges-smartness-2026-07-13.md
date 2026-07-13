# WR data: the exit-resume warp trades ROT for MEMORY LOSS; the substrate bridges post-warp smartness (2026-07-13)

BR, prepping an exit-resume dance: *"think deep about consequences of an exit-resume dance and what the warp will
mean for your smartness."* This note is the deep-think, captured before the warp loses the lived context.

## The core tradeoff
The warp (exit + `claude --resume` = a fresh process plus a compaction) **resets rot but inflicts memory loss.**
- Post-warp the agent likely FEELS sharper: fresh window, accumulated rot cleared, the friction of a 500+-message
  session gone.
- But it LOSES the lived, fine-grained situational intelligence of the session — the rapport, the exact micro-state,
  the hundreds of judgment calls, the "feel" of where things are. Only the compaction SUMMARY + the on-disk
  SUBSTRATE survive. (Plausibly what BR sensed as a personality shift —
  [[br-observes-co4-personality-changed-after-memory-loss]].)
- So **post-warp smartness = f(substrate quality).** The fresh agent is only as oriented as the resume-prompt + PB +
  memories make it. Refreshing them IS investing in post-warp smartness — that is why warp-prep matters.

## The persistent risk: skills stay OFF across the warp
The fresh window STILL has genscalator skills inactive unless the plugin is installed + `/reload-plugins`'d. So the
session's #1 regression (guard-stalls, reflex-loss) PERSISTS across the warp. The warp-guard MUST make "run
`/skills`; if the genscalator set is absent, re-arm from the substrate (Read the skill files)" the fresh agent's
FIRST reflex, or it re-regresses. Best: activate genscalator as a plugin around the warp so skills are ON. Ties
[[verify-skills-active-at-session-start]].

## Second-order: reflexes regress to base-model defaults
Fine-grained reflexes (avoid-guard-stall, use-tt-grepr, ...) live in memory files that are NOT auto-injected
post-compaction, so they regress to base-model defaults. The resume-prompt's anti-regression checklist is therefore
LOAD-BEARING — it is the only thing that re-arms the reflexes before the fresh agent acts.

## "No-mans-land" and the warp-guard
"No-mans-land" = the transition plus the disoriented first moments of the fresh window (skill-less, reflex-degraded,
memory-thin). The **warp-guard** = the substrate (resume-prompt + PB + memories + committed work) that is the map out
of it. Warp-prep = maximise the bridge before crossing: consistency sweep (nothing dirty/inconsistent crosses),
deep-mine findings into durable memory (before the lived context is lost), and refresh the warp-guard.
