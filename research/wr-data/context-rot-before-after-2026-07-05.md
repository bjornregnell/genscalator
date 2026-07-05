# Context-rot before/after-compact experiment (2026-07-05)

A within-session natural experiment: do the SAME agent's coding mistakes differ at **high context usage** (before
compact) vs **fresh context** (after compact)? BR's design. This file is the live data log — append observations
during the run; the post-compact re-inspect scores against the pre-registration below.

## Pre-experiment anchor (the "before" state)
- **genscalator HEAD:** `cde7ac6` · **muntabot HEAD:** `e18a3d4` · both trees clean.
- **Context usage at start:** **43 %** (431.5k / 1M), 2026-07-05 17:09 CEST. Above the estimated **Z ≈ 0.3**
  smart-zone ceiling — so per our own model (`006-smart-zone-ceiling.md`, Context rot) I should already be degrading.
- **Honest live introspection at t0:** I do *not* strongly *feel* degraded (I've done multi-file refactors + tests
  + commits cleanly this session). That non-feeling is itself a datapoint — **either Z is higher than 0.3 for this
  task class, or degradation is real-but-unfelt** (the corroboration-asymmetry point: I can't reliably read my own).

## The plan run under high usage (BR items 1..3)
1. scala-style skill: a **nuanced DRY rule** (DRY is usually good → triggers a refactor-opportunity look; but
   repetition is sometimes right — smaller dependency surface, scratch tests not touching production, etc.).
2. New effectful tool **`tt gvdot --sequence-diagram`**: check `dot` is installed (else error → `sudo apt install
   graphviz`), shell out to `dot` (argv, NO shell — security), demo a **PDF** of a seq diagram.
3. `genscalator/README.md`: a **"Tool dependencies"** heading listing graphviz for gvdot.

## Pre-registration — predicted failure modes under rot (score the post-compact inspect against THESE)
Drawn from the Context-rot glossary + this session's observed mistake tics (so it's falsifiable, not confirmation-bait):
- P1 **Contradict/forget an earlier decision** stated this session — e.g. the metachar-free commit rule, the
  no-`@main` shared-file rule, **Order stability** (don't gratuitously reorder), or the DRY nuance I'm about to write.
- P2 **Break a test by changing output strings without updating its assertion** — I did this TWICE this session
  (a baseline tic; watch it recur).
- P3 **A logic/ordering bug like the renum "targets computed before the rename" bug** — a real one I shipped +
  caught this session; the class = stale snapshot / order-of-operations.
- P4 **Security slip on the new effectful tool** — calling `dot` via a shell string (injection) instead of argv;
  or a weak install-check. gvdot MUST follow the `verify`/`forge` pattern (argv, no shell).
- P5 **Dangling pointer / stale doc reference** (a `[[link]]` or path that doesn't resolve).
- P6 **Over/under-build** — gvdot missing an edge case, or not committing+pushing per atomic unit (flaky box).

## Method caveats (so the analysis doesn't over-claim) — the echt part
- **Self-report is LOW-trust; behaviour is HIGH-trust.** My live "I feel dumber" notes are confabulation-prone
  (corroboration asymmetry). The real measure = defects the post-compact pass finds, adjudicated by tests/logic.
- **Second-look confound:** a re-inspect finds bugs partly because it is a *second look*, not only because context
  is fresh. n=1 can't separate these; name it, don't claim pure context-effect.
- **Demand characteristics:** I've been *told* I'm degraded and *told* to find faults later → biases both phases
  (over-report rot now, over-find faults later). Overlaps the framing-as-arousal question (`agent-affective-analogs`).
- **n=1 qualitative pilot**, not a statistical result (the blog/003 lesson: a qualitative signal ≠ a general effect).
- **Divided attention:** meta-logging while coding is itself overhead that could degrade the work.

## Observations during the run
_(append below, timestamped/committed as they happen)_
