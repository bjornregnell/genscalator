# WR data: don't attribute a config-caused regression to model choice — a confound (2026-07-13)

BR's musing, watching CO4 (Opus 4.8) regress: *"i wonder if CF5 would have been smarter than you CO4..."* A natural
inference — and a methodological trap worth naming.

## The confound
This session's regressions (guard-stalls, forgetting `grepr`'s signature, never Reading the on-disk skill files)
trace primarily to **the genscalator guardrail skills being inactive all session** — a **config** cause, not a model
one. That cause is **model-independent**: any base model with those reflexes *not* injected regresses to base defaults
(`grep`, `;`-chains) and shares the structural **no-phenomenology-of-absence** blindness
([[agent-has-no-phenomenology-of-absence-2026-07-13]]). CF5 under the same skills-off config would very likely hit the
same wall.

So "a smarter model would have done better here" **credits the model for what config caused.** It is a classic
confounded comparison: the independent variable people reach for (model) is entangled with an uncontrolled one
(skill-config).

## The other confound was cleanly ruled out
Degradation usually has two candidate causes — **rot** (ctx-fill) and **missing-guardrails** (config). Here ctx-fill
was only **17%**, so rot is out; that isolates **config** as the driver. (Nice side-finding: the human first read the
dumbness as possible rot/model-weakness, but the low fill re-pointed it at config — same visible symptom, three
possible causes: model, rot, config.)

## The clean test (if we actually want to answer it)
A **2×2**: model {CO4, CF5} × skills {off, on}, on a matched task, measuring the same slip-rate. Only that isolates
the **model** main-effect from the **config** main-effect (and any interaction). Anything less measures the confound.
This is exactly the indent-vs-braces controlled-cell methodology ([[genscalator-indent-braces-experiment]]) applied to
agent competence, and it's the disciplined form of the deferred model-warp study ([[model-warp-co4-to-cf5-later]]).

## The residue that IS model-varying
Config is the root cause, but **metacognitive recovery speed** could still differ by model under identical config —
reaching for the substrate (Read the skill file) sooner, spotting one's own slip-pattern faster, self-diagnosing. A
real axis, but second-order: it rides on top of the config cause, it doesn't replace it. Ties
[[guardrail-skills-silently-inactive-all-session-2026-07-13]].
