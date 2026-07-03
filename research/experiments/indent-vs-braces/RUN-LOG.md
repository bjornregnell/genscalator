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
