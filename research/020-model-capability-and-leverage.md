# Model capability × genscalator leverage — and holding the model constant

- **Question:** how does a **more capable** base model leverage the genscalator tools + methodology compared to a
  **less capable** one? Is the leverage a constant multiplier, does it diminish (a stronger model needs the
  scaffolding less), or does it grow (a stronger model *uses* the tools better)? And per dimension — safety,
  token-efficiency, productivity — does the answer differ?
- **Why it matters:** it's the empirical core of the **substrate-as-multiplier** claim (foundations glossary). If
  we can characterize the interaction, we know *when* genscalator pays off most, and whether the bet stays robust
  as models improve. It's also a **paper-2 / Discussion** thread, not the REJ first-paper focus.
- **Status:** open (2026-07-02, BR).

## Methodological decision (BR 2026-07-02): hold the model constant during WR-data collection
For the **first paper's** WR-data collection we **stay on Opus 4.8** and do **not** switch to Fable 5 / Mythos.
A model change mid-investigation is a **confounding variable**: the very things we measure (shell-reflex relapse
rate, guard-trip frequency, how well the typed path is adopted, meta-introspection) are *themselves*
model-dependent, so a swap contaminates every before/after comparison. Therefore:
- **Decide the model explicitly** and record it (this note). Opus 4.8 is the fixed model for the study window.
- **A model change is an EVENT to log**, not a silent drift — when we do switch, note the date/turn and treat it
  as a deliberate A/B boundary, observing effects on both sides.
- This mirrors the sticky-fallback concern from the earlier Fable-5 thread (a silent model swap = silent
  capability drift): for research validity the model must be a *known, controlled* variable.

## Hypotheses to test (when we do vary the model)
- **Safety leverage is likely capability-INDEPENDENT.** The allowlist + guard + safe-by-design constrain *any*
  model equally — a hook that splits `&&` helps a weak and a strong model the same, and the BHH attack surface
  doesn't shrink just because the model is smarter. So safety is where genscalator's value is most model-robust.
- **Token-efficiency / productivity leverage is likely capability-DEPENDENT, and possibly non-monotonic:**
  - A **less capable** model may depend on the tools to *reach* a quality bar it couldn't hit with raw shell
    (the tools do work it can't) — high leverage, but bounded by the model's ceiling.
  - A **more capable** model may need fewer *knowledge* safeguards (better instruction-following, fewer reflexes)
    yet extract MORE from the *generative* side — composing tools, building better new tools, expressing
    requirements in reqT-lang more precisely. So leverage could shift from "rescue" to "amplification."
  - Net multiplier could therefore *dip then rise*, or stay flat, depending on which sub-capability dominates.
- **Reflex profile is model-dependent.** A stronger model may relapse less on the banned shell reflexes, changing
  the WR-data distribution — which is exactly why the model must be fixed while we characterize the baseline.
- **The two safeguard tiers may split by capability** (ties to [[foundations]] "structural vs knowledge safeguard"): *structural*
  safeguards (hooks, allowlist) help all models equally; *knowledge* safeguards matter more for weaker models
  (stronger ones self-correct). If so, the structural-safeguard investment is the model-agnostic one to prioritize.

## How we'd measure it (future, paper-2)
Run a fixed task battery (the AT grind, the tt-tool-building tasks, a reqT authoring task) under two models with
identical genscalator substrate, and compare: task success, tokens-to-correct, guard-trip / relapse counts,
number+quality of tools built, safety-envelope violations (should be ~0 for both if safe-by-design holds).
Controlling substrate constant isolates the model as the independent variable — the mirror image of this note's
decision (hold the model constant to isolate the substrate). Relates to
[`019-subagent-genscalator-propagation.md`](019-subagent-genscalator-propagation.md) (a sub-agent is a capability/
context knob too) and the planned Java-vs-Scala token-efficiency experiment (`README` roadmap).
