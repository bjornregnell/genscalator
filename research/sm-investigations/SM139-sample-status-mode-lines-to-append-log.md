# SM139 — sampling the status + mode lines into an append-only research log

**Status: INVESTIGATION (agent-drafted AFK 2026-07-18).** ⛔ **This is a CANDIDATE, not a build order** — the
SM134 "find more tools is a quota" gate applies. No tool built here; this answers the design questions and the
feasibility gate the pin demands, so BR can decide whether it earns its keep.

## Why it matters (restating the pin's own case)

Status **line 1** (`ctx-fill`, `rot?`, `tot`, `silent Ns`, limits, cost) is the **only self-report in this system
that is not confabulation** — it is instrumented, not introspected. A time series of it is the closest thing to
objective psyche telemetry, and the natural DV for the rot-vs-heat / Yerkes-Dodson question
([[agent-affective-analogs]]) and for `research/029`'s cross-model comparison (its missing baseline instrument).
**Today it is kept only by accident** — the sole surviving pre-compact sample exists because BR pasted it into the
feed. The sampler industrialises what the human now does by hand, only when he remembers.

## (e) FEASIBILITY GATE — verified FIRST (echt, against source)

Checked what `tt statusline` can actually READ before spec'ing anything (SM118's rule):
- From the harness **stdin JSON**: `model.display_name`/`id`, `cost.total_cost_usd`, `context_window.used_
  percentage`, and `transcript_path` (statusline.scala:15, 231, 270, 464).
- From the **transcript JSONL** (via `transcript_path`): per-line `timestamp`, `type`, `message.content`, and
  `subtype:"compact_boundary"` → the tool derives token totals, last-activity time, and compact seams
  (statusline.scala:126-136). This is the `tok:` gauge + hangover logic (SM117).
- The **mode line** reads the `~/.claude/gs-modes` state file (mode.scala).

**Conclusion: the sample content needs NO new data access.** Everything the pin wants to log is *already* what
`tt statusline` + `tt mode` compute. The missing piece is a **capture trigger + an append sink**, not
instrumentation. The sampler inherits *exactly* statusline's readable envelope (SM118) — it cannot promise a
field statusline can't already show, which is precisely the guarantee the gate asks for.

## (a) TRIGGER — piggyback the per-turn statusline call; NO daemon

The decisive realisation: **the statusline command is already invoked by the harness every turn.** So the
"poller" already exists and is already trusted — we don't add an always-on process (this box is memory-fragile,
[[blixten-box-flaky]]). Three event sources, all pre-existing:
1. **Per-turn (statusline itself):** have `tt statusline` keep a tiny `last-sample` state (last band of `ctx-fill`
   / `rot`, last mode set) and **append one telemetry row only when something crosses a band or a mode changes** —
   sample-on-CHANGE, exactly BR's "space-efficient non-destructive compression." No clock, no daemon.
2. **Compact seam (`compact-wake.sh`):** already fires at `startup|resume|clear|compact` — append a sample there
   too, so the pre/post-compact line (the one BR hand-saves) is captured automatically. This is the highest-value
   single sample.
3. **Mode change (`tt mode add/rm`):** mode.scala already writes the state file on change — it can append a
   telemetry row in the same breath, capturing the transition natively (and its *when*).

## (b) RATE — transitions, not ticks

Interesting events are transitions (mode change, compact, `ctx-fill`/`rot` crossing a band), not every turn.
Sampling on change is both the honest signal and the compression BR asked for. **Honest gap:** a mid-session
`ctx-fill` band-crossing has no harness event of its own — but since statusline runs every turn, the *append-on-
crossing* logic in trigger (1) covers it without a poller. Continuous ticks are explicitly NOT wanted.

## (c) HOME — append-only

An append-only log under `research/wr-data/` (or a dedicated `research/telemetry/`), [[raw-data-append-only]]
applies: **never retro-edit**. Format: one TSV row per sample — `iso-timestamp \t event(turn|compact|mode) \t
model \t ctx_fill% \t rot \t tokens \t cost \t active-modes` — so `tt text` can later histogram it. TSV over prose
so it stays mineable and space-cheap.

## (d) MODE-LINE PROVENANCE — a free partial answer to SM134 #3

SM134 #3: the mode line renders STATE but never who/when/why. A *log* of mode changes recovers the **when** for
free (and, if `tt mode add/rm` records the actor, part of the **who**). So SM139 is also a down-payment on the
provenance gap — worth noting the two are related, not duplicate.

## Recommendation (for BR to weigh against the quota)

**If** it clears the "earns its keep" bar: the cheapest real version is **(2) alone** — append one sample at the
`compact-wake.sh` seam — which already captures the single most valuable datum (the pre/post-compact line) with
**zero new always-on code** and no new tool, just a few lines in the existing hook writing a TSV row via the
existing `tt` readers. Add (1)/(3) later if the transition series proves useful. Do **not** start with a daemon.

## Ties
SM134 #3 (mode-line provenance) · SM118 (statusline readable envelope — the feasibility gate) · SM117 (the
`tok:`/rot gauges this would sample) · [[agent-affective-analogs]] · [[token-budget-modes]] ·
[[raw-data-append-only]] · `research/029` (the telemetry is its missing baseline instrument).
