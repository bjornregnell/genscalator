# Estimating the smart-zone ceiling (smart→dumb boundary)

- **Question:** Can we **estimate Z** — the context-fill fraction at which an agent crosses from the *smart
  zone* into the *dumb zone* (see `docs/foundations.md`) — per model (and ideally per task), so the agent
  or its instruments can **warn/brake before degradation** instead of at the hard token limit?
- **Why it matters:** the entire smart-zone / TE argument rests on Z, but Z is currently a **guessed prior**
  (~0.3 folklore). A measured/estimated Z turns "keep the working context small" from folklore into a
  **quantitative brake threshold**, and closes the blind spot named in `token-budget-awareness.md`: the
  `token-usage` tool can read live **fill %** but has *no Z to compare against*, so it can't say "you're near
  the edge." Z + fill% together = a real "smart-zone gauge."
- **Status:** open. (Tool v1 shipped — see below.)

## Plan / candidate estimators (to explore)
1. **Calibration probes (in-session, cheap).** Periodically inject a tiny **recall/consistency self-check**
   — recall an earlier decision/constraint, or a planted fact ("canary") from N turns ago — and record the
   **fill % at which accuracy starts dropping**. Like needle-in-a-haystack / "lost in the middle", but live
   and continuous. The knee of accuracy-vs-fill estimates Z.
2. **Proxy degradation signals (no probes).** Track, as functions of fill %: **self-contradiction rate,
   tool-call retry/failure rate, repeated/redone work, instruction-forgetting** (re-asking something already
   decided). The fill % where these inflect estimates Z — observable for free during normal work.
3. **Empirical prior per model (logged).** A `wr-data`-style log of **fill % at observed degradation
   events** across many sessions → a distribution → a per-model Z prior that sharpens over time.
4. **Published-benchmark seed.** Start Z from public long-context evals (NIAH, RULER, "lost in the middle"),
   per model, then refine in-session via (1)/(2).

## Tool angle (what BR asked for: a tool that estimates the cut %)
- **v1 (shipped):** `token-usage --ceiling <Z>` flags when `fill% / Z` crosses a warn threshold
  ("approaching smart-zone ceiling → checkpoint+compact"); defaults to an Z=0.35 **prior** when `--ceiling`
  is omitted (clearly labelled a guess). Already gives the brake signal; uses a guessed Z until measured.
- **v2:** add the **proxy-signal logger** (#2) so Z is *measured* per session, not just assumed; feed #3.
- **v3:** optional **calibration-probe** hook (#1) for a sharper, active estimate.
- Graduate the useful form into a `tt usage`/`tt smart-zone` genscalator tool (read-only, deterministic).

## Relation to other notes
- `docs/foundations.md` — defines **Smart-zone ceiling (Z)**, *smart/dumb zone*, *context rot*.
- `token-budget-awareness.md` — the token-spend analogue; Z is the *context*-window analogue of the budget
  cap, and the agent is blind to both without an instrument.
- `instrumentation-by-default.md` — a smart-zone gauge is exactly this: an instrument the agent Reads
  instead of guessing how degraded it is.
- `proactive-compaction-point.md` — uses Z as the *reactive* brake (`0.8·Z`) and adds a **proactive**,
  durability-gated compact trigger ("consolidation point"). Its RQ4 is where the ceiling symbol was
  renamed **L → Z** (2026-07-03) — "smart-**Z**one ceiling", Z sitting between the smart and dumb zones.

## What shipped
- `autotranslate/scratch/token-usage.scala` `--ceiling <Z>` warn flag (v1 above) — introprog session
  2026-06-30. To graduate into a genscalator `tt usage` tool.
