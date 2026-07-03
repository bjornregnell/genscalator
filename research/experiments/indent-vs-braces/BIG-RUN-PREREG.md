# BIG RUN — preregistration (indent-vs-braces edit-cost, confirmatory) — DRAFT for BR approval

**Status: DRAFT 2026-07-03. FREEZE + COMMIT this before any run.** This document *is* the anti-fishing guarantee:
the design below is fixed in advance; the overnight job executes it **once** and reports whatever it finds —
including a null.

## 0. Why this run
The 7-model pilot (`RESULTS.md`, blog 002 §5.5) was **hypothesis-generating** and returned a **null**: braceless
costlier in aggregate (−17.5 pp pass-rate vs braceful) but omnibus permutation **p ≈ 0.46**, effect *bidirectional*
per model. This run is the **confirmatory** follow-up: test the pilot's hypothesis **out-of-sample** on many more
models, with enough replication to have a real chance at significance — pre-committed to report the null if it
stands. It executes blog 002 §7's "more models, across vendors, for power."

## 1. Hypotheses (frozen)
- **H1 (adherence):** style adherence depends on model × style (some models cannot produce some styles).
- **H2 (PRIMARY):** controlling for adherence, braceless edits are at least as error-prone as braceful; aggregate
  per-model pass-rate braceless < braceful.
- **H3 (frontier, secondary):** a frontier anchor (Opus 4.8) shows ~ceiling adherence + style-insensitive correctness.

## 2. Primary endpoint + test (ONE, frozen)
- **Unit of replication = the MODEL** (never the cell — pooling cells is the pseudoreplication that faked p=0.008).
- **DV:** per-model pass-rate (PASS ÷ attempts) per style.
- **PRIMARY test:** exact/Monte-Carlo **paired permutation (sign-flip), braceless vs braceful pass-rate, blocked by
  model**, two-sided, **α = 0.05** — the `significance.scala` test, on the confirmatory sample.
- **Secondary confirmatory:** omnibus 3-style permutation + Friedman.
- **Exploratory (labelled, never promoted):** per-size, per-family, per-edit-kind, token & wall-clock cost.

## 3. Sample (freeze exact list on approval)
- **Confirmatory set = NEW models DISJOINT from the pilot 7**, so the hypothesis is tested out-of-sample. Target
  **n ≈ 50 new models**, all ≤ ~8B / ≤ ~5.5 GB (q4) to fit 6 GB VRAM.
- **Candidate pool** (verify `ollama pull` availability, then freeze ~50): qwen2.5 {0.5b,1.5b}, qwen2.5-coder
  {0.5b,1.5b,3b}, gemma3 {1b,4b}, gemma2:2b, llama3.2 {1b,3b}, llama3.1:8b, phi3:3.8b, phi3.5, mistral:7b, codegemma
  {2b,7b}, deepseek-coder {1.3b,6.7b}, deepseek-r1 distills {1.5b,7b,8b}, starcoder2 {3b,7b}, granite-code {3b,8b},
  granite3 {2b,8b}, stablelm2:1.6b, tinyllama, smollm2, orca-mini:3b, yi:6b, internlm2, openchat, neural-chat,
  hermes3:8b, wizardlm2:7b, codeqwen, falcon3, olmo2, exaone, nemotron-mini, command-r7b, dolphin variants…
  Prefer **family/vendor diversity** for external validity; near-duplicate finetunes are allowed as distinct
  replication units.
- **Pilot 7 re-run too** (pooled n≈55) but reported **SECONDARY** (contaminated by hypothesis generation).
- **Honest ceiling:** the usable ≤8B universe is ~40–60 models — i.e. the 6 GB card sits right at the ~55 the power
  calc wants. If the pullable count falls short, we **report at the achieved n and flag under-power**; we do **not**
  crank R to compensate (that would be pseudoreplication).

## 4. Tasks + repeats (frozen)
- **PRIMARY:** the SAME task family as the pilot — wrap-in-`else`, 3 sizes (small/med/large), 3 styles — a direct
  out-of-sample confirmation.
- **EXPLORATORY (time permitting / 2nd night):** +1–2 new edit kinds (extract-helper, add-`case`) for
  generalization only; not in the primary test.
- **R = 6** repeats per cell (matches pilot; stabilizes each model's estimate). *R does not buy between-model power —
  models do.*

## 5. Analysis (frozen, pre-specified script)
- At n ≈ 50, `6^n` is not enumerable → pre-commit to **Monte-Carlo permutation with a FIXED committed seed**
  (100 000 relabellings; deterministic given the seed) for both omnibus and paired tests. **Update
  `significance.scala` to the Monte-Carlo path and commit it BEFORE the run.**
- Adherence split (`analyze.scala`) for H1. Pseudoreplication foil (pooled chi-square) reported ONLY as the
  cautionary contrast, never as the result.
- **Committed seed:** _(set here at freeze time, e.g. `SEED = 20260703`)._

## 6. Stopping rule + honesty commitments (the anti-fishing core)
- **Fixed n, NO optional stopping** — we never add models until p<0.05 then stop.
- **One primary test, reported whatever it says** — a null is reported as prominently as a positive.
- **No model dropping** — every pulled model that runs is in the analysis (can't-emit-a-style models → RQ1 data).
- **No post-hoc endpoint switching** — exploratory results stay flagged, never promoted to "the finding."
- **Frozen seed** for the Monte-Carlo permutation. Automatic behavioural grader → no scoring degrees-of-freedom.

## 7. Overnight AFK execution plan
- **Phase 0 — HUMAN GATE (fast):** BR approves + we commit this frozen doc. The *only* human step before compute; it
  IS the honesty guarantee.
- **Phase 1 — AFK setup:** `ollama pull` the ~48 new models (skip-and-log failures, never halt). ~150 GB,
  network-bound (mins–hours by bandwidth); disk fine (523 GB free).
- **Phase 2 — AFK compute:** sweep **model-OUTER** (each model loads once → runs all its 3×3×6 = 54 cells →
  unloads; ~50 loads total, not thousands) → generate + grade + append `results-raw.tsv` (crash-safe/resumable).
  ~50 × 54 = **2 700 primary cells**. Est **~8–14 h** (small models, load-amortised) → one long night; exploratory
  edit-kinds spill to a 2nd night.
- **Phase 3 — AFK:** run the pre-specified (seeded) `significance.scala` on the frozen TSV → primary p + secondary +
  exploratory.
- **Phase 4 — HUMAN (morning):** read the honest result; write up confirm-or-null.

## 8. Hardware feasibility — bjornyx.local (checked 2026-07-03)
- GPU **Quadro RTX 3000, 6 GB VRAM** (idle 6 MiB) → caps model **size** (≤~8B q4), not count.
- Disk **523 GB free** of 741 GB → 55 models (~150–165 GB) fit with ~3× headroom.
- RAM **30 GB** (25 free) → recently-used models cache in RAM → faster reloads.
- **16 CPU cores** → per-cell scala grading parallelisable.
- **Verdict:** disk/RAM/CPU are **not** the bottleneck; VRAM caps size not count; the true ceiling is how many
  genuinely-distinct ≤8B models exist (~40–60) — which **brackets the ~55 the power calc wants.** Feasible as an
  overnight AFK job, sitting at the honest *edge* of being able to reach significance — the informative regime.

## 9. Power basis (honest)
- Pilot effect: braceless−braceful mean **−17.5 pp**, between-model **SD ≈ 48 pp** → **d ≈ 0.37** (small,
  outlier-inflated by aya/gemma3).
- Paired test, 80% power at d = 0.37, α = 0.05 → **n ≈ 55 models**.
- If the larger sample's effect is *more consistent* (smaller SD) than the outlier-heavy pilot, fewer suffice; if
  genuinely inconsistent, even 55 under-powers → we **report the null**. Both are real answers — which is the whole
  point of doing it honestly.

## 10. AFK-menu fit — verdict
**Yes.** Phases 1–3 are a *scaled pilot* (the pilot already ran 378 cells autonomously overnight) → fully
autonomous, crash-safe, box-light on VRAM. The **only** non-AFK step is Phase 0 (approve the frozen prereg), which
is fast and is exactly what keeps it honest. Menu item shape: *"BIG confirmatory run — pull ~48 models → sweep
model-outer (2 700 cells) → seeded permutation test → report (incl. null)."* The one rule the AFK job must obey:
**run the full frozen n and report whatever p comes — never stop early on significance.**
