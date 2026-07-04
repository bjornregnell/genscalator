# BIG RUN — operational log (append-only; NOT the frozen design)

Operational observations during execution. The design is frozen in `BIG-RUN-PREREG.md`; this file records
*what happened when we ran it*, so nothing here changes hypotheses/tests — it only aids honest analysis.

## 2026-07-03 — Phase 1 pulls

- Pulling the 56-model frozen list onto bjornyx via `pull-driver.scala`, disk floor 80 G. All OK so far;
  disk 523 G → dropping ~1–4 G per model as expected.
- **Tag-aliasing flag (integrity):** `gemma3:4b` pulled in **0 s** — a strong signal it shares all layers
  with an already-present model, almost certainly the pilot's `gemma3:latest` (gemma3's default IS the 4B).
  **Action at analysis:** dedup the confirmatory set by ollama **model ID** (the content hash from
  `ollama list`), NOT by tag string, and drop/flag any confirmatory tag whose ID equals a pilot-7 model's
  ID — otherwise the "disjoint, out-of-sample" guarantee is silently violated. Candidate aliases to check:
  `gemma3:4b` vs pilot `gemma3:latest`; any `:latest` tags that resolve to a pilot ID. Report the deduped n.
- This is honest bookkeeping, not a design change: the frozen list stays as committed; the analysis simply
  reports the *effective* disjoint n after ID-dedup, and lists which tags collapsed.

## 2026-07-03 — Phase 2 sweep: model-server contention window (transparency, no data change)

- **What happened:** while the sweep ran, a parallel task (the introprog autotranslator's `--all`/`--dump-overrides`
  regens, part of the overnight AT work) made ollama calls to the **same** `bjornyx.local:8080` model server for
  ~15 min (roughly around sweep cells **745–765**, model `codegemma:2b`). GPU was at **95% util / 66°C / 2194 MiB**
  during this — compute-saturated, so extra requests queued (one autotranslator `--all` visibly hung and was
  killed).
- **Integrity check on NORESP:** the sweep grades a no-model-response cell as `FAIL_NORESP`. Total NORESP so far =
  **4 / 764 (~0.5%)**: cells **309, 324** (`qwen2:0.5b`) occurred **before any contention** (natural base rate for
  tiny models), and **758, 764** (`codegemma:2b`) fall inside the contention window. The two recent ones are fully
  consistent with the pre-existing ~0.5% base rate → **no evidence of material contamination**, but they *could* be
  marginally contention-influenced.
- **Action = NONE to the data.** Per the frozen protocol (no optional stopping, no cell dropping/re-running), a
  NORESP is a legitimate FAIL outcome and stays as recorded. This entry is the transparent caveat so analysis can
  sensitivity-check `codegemma:2b`'s cells 758/764 if it matters (it almost certainly won't — 2 cells, model-blocked
  design, and within base rate). **Mitigation going forward:** no more model-server-competing jobs while the sweep
  runs (the AT track that caused it is now complete). Lesson: a single shared model server means concurrent
  model-using jobs contend on the GPU — schedule them disjoint from a frozen experiment run.

## 2026-07-04 — Phase 2 sweep: externally killed at cell 946, resumed (no data change)

- **What happened:** the background sweep task was **externally stopped** after ~4 h at **cell 946 / 3024 (~31%)**
  — NOT a crash: the output tail shows clean cells (939–946, `llama3.2:3b`) right up to the stop, no
  error/OOM/signal, so this was the harness culling a long-lived background task, not a sweep failure. By then the
  throughput had fully recovered past the slow `codegemma:2b` block (cells 808→946 = +138 in ~30 min).
- **Data preserved:** all **946 cells** were already in `results-bigrun.tsv` (the runner appends per-cell, line
  116). Backed up verbatim to `results-bigrun-partial-946.bak` before touching anything.
- **Recovery = resume, not restart.** Added an **additive resume-skip** to `sweep-main.scala`: it reads the
  `(task, style, model, run)` keys already in the output TSV into a `done` set and **skips them** in the loop, so a
  relaunch **append-continues exactly the missing cells** — no duplicates, no re-running. Relaunched with the SAME
  args (`6 @models-frozen.txt results-bigrun.tsv`); it printed `resume: 946 cells already done … skipping those`
  and immediately resumed at `llama3.2:3b`'s remaining cells (verified: tsv 947→953, new cells only).
- **Why this does NOT touch the frozen design:** the resume changes no model list, no seed, no task/style set, and
  drops/duplicates no cell. The 946 completed cells are the same data an uninterrupted run would hold; the remaining
  cells run the same frozen models fresh. Generations are non-deterministic (temp), but that variance is what R = 6
  captures, and resuming does not choose *which* conditions run — so there is no selection/optional-stopping bias,
  and the model-blocked analysis is unaffected. If the kill recurs, re-relaunching is idempotent (the skip-set just
  grows) — the run completes in chunks. The `.bak` is kept as a safety net until the run finishes.

## 2026-07-04 — Phase 2 sweep: COMPLETE + analysed (no data change)

- **Run complete.** The resumed sweep append-continued to the end with no further kills; `results-bigrun.tsv` holds
  the full frozen matrix (see `BIG-RUN-PREREG.md` for the design). The mid-run checkpoint
  `results-bigrun-partial-946.bak` is retained as the safety net.
- **ID-dedup applied at analysis** (per the Phase-1 tag-aliasing caveat above): the confirmatory set was deduped by
  ollama model **ID** (content hash), not tag string, and any confirmatory tag collapsing onto a pilot model's ID
  (e.g. `gemma3:4b` ≡ pilot `gemma3:latest`) was dropped, so the reported effective *disjoint* n is honest. The
  deduped n and the collapse list are in `RESULTS.md`.
- **Result = a clean, preregistered NULL.** The confirmatory analysis (seeded permutation test, blocked by model;
  `significance.scala`) found **no significant effect of brace style on small-model edit-success** (omnibus
  p ≈ 0.59). The model dominates; the pilot's directional signal did **not** replicate out-of-sample. Full numbers,
  the per-model table, and caveats live in `RESULTS.md` ("Confirmatory big-run"); the write-up is `blog/003`.
- **Protocol integrity held end-to-end.** No optional stopping, no cell dropping/re-running — the only interventions
  were the idempotent resume-skip (recorded above) and ID-dedup at analysis, neither of which selects conditions or
  alters the frozen tests. **This closes the BIG RUN operational log.**
