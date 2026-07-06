# 047 — Plan critique + adjudication (adversarial pass, 2026-07-06)

An adversarial critic sub-agent (read-only) reviewed `047-PLAN.md` + `047-instrument.md` + the study log, steering clear of threats the study already handles, and returned 15 ranked improvements. Below: the critic's point (condensed), **my echt adjudication**, and **STATUS**.

**Status legend:** ✅ APPLIED (clear honesty/labeling/structure fix, already edited or trivially editable) · 🟡 PARTIAL (partly addressed, residual is a design call) · 🔵 FOR-BR (a genuine design decision — I will not re-architect the study solo).

## Tier 1 — threats to the central claims

**1. "Triangulation" is overclaimed.** The two arms measure *different constructs on different populations* (identity: a fresh Claude reconstructs the BR-self from substrate; coding: assorted code-LLMs reproduce a Scala style they were handed). "Facts carry, texture leaks" surfacing in both is **conceptual/pattern replication across media**, not triangulation of one construct → the "defeats mono-method bias" credit isn't earned.
→ **Adjudication: VALID, and echt-important.** I over-claimed. **STATUS ✅ APPLY:** reframe "methodological triangulation / mono-method-bias defeat" → "conceptual replication across two media (CSR §5.3.3 pattern-matching)"; keep the genuine strength (same predicted pattern, second medium), drop the borrowed credit.

**2. Circular style scoring** (CO4 defines the style, is a top subject, AND judges → self-preference bias; "gold examples" mitigation too thin).
→ **Adjudication: VALID.** **STATUS ✅ APPLIED** (commit d1c1f60): blind-to-condition review; mechanical linter/AST scoring for parseable checkpoints; LLM-judge only for subjective qualities; self-preference control (CO4-rates-CO4 vs blind-human baseline).

**3. Full condition tests instruction-following, not reconstruction** (the prompt hands the model the conventions → high full-score = following a just-given checklist = the RECITATION the identity arm worked to get past).
→ **Adjudication: VALID, the sharpest point.** **STATUS 🟡 PARTIAL:** the naturalism note (incomplete PRDs push the model to *infer* unstated details, beyond transcription) partly answers it. Residual **🔵 FOR-BR:** to make it a true *reconstruction* test, the our-style signal should come from something the model must RECONSTRUCT (real committed-code exemplars, or a cold agent locating conventions in the substrate) — not a handed checklist that mirrors the scoring key. Recommend: add a condition that supplies real code samples but withholds the explicit checklist; label the checklist-supplied full condition the "instruction-following ceiling."

**4. "Reader capability" (Factor A) is a confounded bundle + collides with Factor B on substrate sizing.** The 6 model points differ in params, family, tuning AND context-window; and feeding "an excerpt sized to the smallest model's window" either starves capable models (understating carry) or varies excerpt-size by model (confounding B).
→ **Adjudication: VALID, important.** **STATUS ✅ APPLY (labeling)** + **🔵 FOR-BR (sizing):** demote Factor A from "capability" (latent) to "**model**" (a labeled bundle); reserve the *ladder/monotonicity* claim for the **`qwen2.5` 0.5/1.5/3/7b sub-ladder** (family fixed) only. FOR-BR: resolve the excerpt-sizing collision — either fix at the min window for ALL models and log that capable models are under-fed, OR measure substrate-size as a covariate. State the choice.

**5. Stats over-promise for deterministic single-observation cells.** temp0+seed → one answer/cell, zero within-cell variance → a monotone trend over 6 deterministic points has **no inferential test** (only description); **Cronbach α is misapplied** (no multiple respondents/condition); variance then lives only in the Claude fleet (a different population from the ladder).
→ **Adjudication: VALID.** **STATUS 🔵 FOR-BR:** choose (a) run **k>1 seeds at temp>0** per cell → within-cell distribution → **Jonckheere–Terpstra** (test for *ordered* alternatives) on the sub-ladder; or (b) restrict the ladder to **descriptive pattern-matching**, delete inferential language. **Drop α** (keep only if multiple respondents/condition); **keep κ** (rater agreement). Soften "reproducible" → "best-effort deterministic" (temp0 ollama isn't bitwise-guaranteed across GPU/version).

## Tier 2 — sensitivity & feasibility

**6. Floor-saturation** — the un-named mirror of the ceiling threat: if full×mid cells also floor (tasks too hard), no gradient, equally non-discriminating.
→ **VALID. STATUS ✅ APPLY:** make the feasibility gate ALSO a **difficulty-calibration gate** — pilot a few cells, confirm full×mid lands in the sensitive band (~40-70%), add a mid-difficulty style item if C2-C5 saturate.

**7. PRD battery unrealistic for the small end + does NOT hold the capability factor** (a full-stack ScalaJS/Laminar/Jetty app one-shot by a 0.5b → floor; PRDs are routed to capable models only, so the PRD arm has no ladder — contradicting "both tiers hold both factors").
→ **VALID. STATUS ✅ APPLY:** state plainly — **micro-tasks (C1-C5) carry the capability ladder; PRDs carry substrate-completeness + ecological validity at the capable tier.** Fix the "both tiers hold both factors" sentence. (Consider simplifying PRD1's web stack to a testable core, or accept it as a capable-tier ecological probe.)

**8. No per-generation timeout / retry / failure policy for the unattended queue** — a hung ollama call, an infinite-loop generated program, or a never-returning compile stalls the whole overnight run (box already memory-pressured).
→ **VALID, highest concrete overnight-death risk. STATUS ✅ APPLY:** specify hard timeouts per generation and per `scala-cli` compile/test (kill + record `TIMEOUT`); max-retries then `FAILED`; orchestrator scores FAILED/TIMEOUT as 0 with a reason code and continues. Add a timeout-injection test to the feasibility gate.

**9. The identity ladder may itself be non-discriminating** (auto-scoreable identity items are RECALL = already ceiling, plus SELF/GAP = no objective key); so the "retires threats #1/#2" claim rests largely on the **coding arm**, not the identity ladder.
→ **VALID. STATUS ✅ APPLY:** say so — objective sensitivity comes from the coding arm; the identity ladder contributes recall-legibility (ceiling-prone) + qualitative gap-divergence (human-scored, small-n). Don't credit the identity ladder alone with retiring the ceiling threat.

**10. Substrate-excerpt curation is an unspecified researcher degree-of-freedom** (a curator who sees the probes can inflate fidelity).
→ **VALID. STATUS ✅ APPLY:** pre-register the excerpt-selection rule before seeing scores — use the **actual resident core** (`MEMORY.md` + PB, the genuine ~15k cold-boot index the study already measures) as the fixed "full" excerpt, no hand-curation. Tightens the ecological link to the real cold-start.

## Tier 3 — honesty / integration / structure

**11. Pre-register the confirmation/falsification thresholds** (what counts as "flat" and "leaks") — else any pattern narrates as confirmation. → **VALID. ✅ APPLY:** write a decision rule into §6 before the run (e.g. correctness Δ < X, style Δ > Y, control ≥ Z below full).

**12. Specify how the two arms combine into one RQ-level answer** (no integration decision rule; given #1 it's the weakest joint). → **VALID. ✅ APPLY:** add an integration sub-section — joint prediction, confirming pattern in EACH arm, and the disconfirmer (e.g. "if coding correctness *also* drops with capability, the facts/texture split is not medium-general"). Frame as replication, not pooled measurement.

**13. Enactment arm (Arm 5) is orthogonal to the ladder + under-specified scoring.** → **VALID. ✅ APPLY:** present enactment as a **separate binary survival check across the cold boundary** (its own construct), not a rung of the grid; specify who adjudicates "fired" + the behavioral criterion; blind if possible.

**14. Relabel RQ3 "substrate legibility to weak readers" consistently** (currently drifts back to "portability of identity"). → **VALID. ✅ APPLY:** rename RQ3 + the Results heading everywhere; keep the ollama-legibility tier distinct from the Claude cold-start tier.

**15. Structural inconsistencies:** probe count (§3.4 "K×M" vs instrument's fixed 40); "both tiers hold both factors" (#7); different model sets per arm ⇒ capability axis NOT shared across arms (reinforces #1/#7); "object held constant" vs "excerpt sized to smallest window" (#4). → **VALID. ✅ APPLY:** reconcile each.

## Net + plan for applying

The core design (escape ceiling saturation via known-groups capability×substrate + an objective coding response variable) is **sound and a genuine upgrade** — the fixes are honesty/rigor, not a teardown. **Highest-impact:** #1 (stop saying triangulation), #2 (de-circularize scoring — DONE), #3 (full-condition = instruction-following), #4 (capability confound + sizing), #5 (align stats). **Overnight-death risks:** #8 (timeouts), #6/#7 (floor/PRD difficulty).

**Application plan:** the ✅ items are honesty/labeling/structure edits I will apply in a careful revision pass. The 🔵 items (#3 reconstruction condition, #4 sizing, #5 reps-vs-descriptive) are genuine design decisions for BR — raise at the review/surf session. #2 already applied.
