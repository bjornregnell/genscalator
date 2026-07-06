# 047 — Overnight run: operational state + budget clock

Durable operational file for the solo overnight run. The overnight agent READS this to pace itself.
BR is AFK during the run and cannot clear a harness guard or read the quota panel for me — so all pacing
is by **wallclock vs the timestamps below** (I cannot see the usage UI from code).

## Budget clock (timestamped 2026-07-06)

- **Anchor NOW:** 2026-07-06 **21:11:11 CEST** (+0200) — epoch **1783365071**.
- **Weekly limits at anchor:** All-models **57% used**; **Fable 0% used** (separate bucket); session 7% used.
- **WEEKLY RESET (approx):** 2026-07-07 **~09:03 CEST** — epoch **1783407791** (anchor + 11h52m, from a panel
  reading ~3 min stale, so treat as ~09:00-09:10; do not cut it fine).
- **Safe "definitely refreshed" threshold:** epoch **1783408691** (reset + 15 min margin ≈ 09:18 CEST).

## Pacing strategy (two phases by wallclock)

Check with `date '+%s'` and compare to the thresholds. I cannot verify the quota number (no UI access);
wallclock-past-reset is the proxy BR authorized for "headroom refreshed."

- **Phase A — BEFORE reset (`now_epoch < 1783407791`, before ~09:03 CEST):**
  All-models bucket is 57% spent (≈43% left) and shared by Opus/Sonnet/Haiku. **Conserve it.** Do the heavy
  work on the FREE arms: **ollama on bjornyx** (zero API cost, GPU/CPU-bound) + **Fable** (0% used, its own
  bucket). Use Opus only for essential judgment that can't wait.
- **Phase B — AFTER reset (`now_epoch >= 1783408691`, after ~09:18 CEST):**
  All-models refreshed → run the **Opus-heavy** analysis / scoring / synthesis / writing freely.

Rationale: sequence the token-hungry Claude-fleet analysis to land AFTER the weekly reset, so it draws on a
fresh bucket; keep the pre-reset hours on free/local compute (ollama) and the untapped Fable quota.

## Notes
- Session limit (7%, resets ~01:34 CEST / every few hours) is NOT the binding constraint — the weekly all-models
  bucket is. Session resets often and cheaply.
- If the run is still going past ~09:18 CEST, all subsequent heavy work is on the fresh weekly bucket.
