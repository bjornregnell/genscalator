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
