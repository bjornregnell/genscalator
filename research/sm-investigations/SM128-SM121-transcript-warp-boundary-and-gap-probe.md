# SM128 + SM121 — transcript warp-boundary & timestamp-gap probe (2026-07-16)

**Status:** empirical investigation (read-only). Grounds two pins without deciding them: **SM128** (status-line
`tok` gauge: cumulative vs since-last-warp) and **SM121** (blackout-hangover detector). Method: a read-only scratch
program (`scratchpad/probe-transcript.scala`) over this session's own transcript JSONL (8474 records spanning
2026-07-14 → 2026-07-16). No code changed; this is input to the JOINT mode-indicator revisit.

## What the transcript actually looks like (measured)

- **Records:** 8474. **With a top-level `timestamp`:** 6586 (ISO-8601, e.g. `2026-07-14T11:07:58.421Z`). The
  conversational records (`message` 3112, `user` 1515, `system` 171) carry timestamps; several infra record types
  (`queue-operation`, `mode`, `permission-mode`, `last-prompt`, `ai-title`, …) do **not**. Gap-detection must key on
  the stamped conversational records, not every line.
- **The warp boundary is a FIRST-CLASS, detectable marker:** `{"type":"system","subtype":"compact_boundary"}`. This
  file contains **2** of them (records ~2252 and ~4409), each immediately followed by the post-compact `user` summary
  prompt. There is also a `compact_file_reference` type (6 records). So a compact leaves an **explicit in-file
  marker** — no heuristic needed.
- **The transcript file SPANS compacts.** 8474 records, **48h 9m** wall-clock, **2 compact_boundary** markers, all in
  ONE file. Pre-compact records remain present after the boundary. => `transcript_path` **survives** `/compact`.

## SM128 finding: `tok` currently accumulates ACROSS warps (confirms BR's concern)

`TranscriptStats.of` sums `output_tokens` over **every** line of the file. Because the file spans compacts, the sum
is a **lifetime-of-transcript** figure, not a current-context figure — "tok 6.5M" here would be ~2 days and 2
compacts of tokens. As a **rot** gauge that overcounts: rot lives in the *current* context window and resets at a
warp. **Options (for the JOINT decision, not taken here):**
1. **since-warp** — sum `output_tokens` only for records **after the last `compact_boundary`** in the file. Cheap,
   exact, uses the explicit marker. `/clear` and a brand-new session naturally start a fresh file (tok = 0), so the
   marker-reset handles the compact case and file-freshness handles the rest.
2. **keep cumulative** — if `tok`'s job is *cost/effort over the whole engagement*, cumulative is correct; then the
   ROT signal should be a **separate** since-warp segment.
3. **show both** — e.g. `tok 6.5M (0.4M ↺)` = lifetime + since-warp. Note `ctx-fill %` **already** resets at compact
   (it is a current-window fill), so a since-warp `tok` would agree with `ctx-fill`'s framing while adding the
   absolute-token magnitude `ctx-fill` lacks. Decide `tok`'s JOB first (cost vs rot), then pick.

## SM121 finding: the hangover is detectable; the cause is not (matches the theory)

Largest inter-record gaps between stamped records (seconds): **41865 (~11.6h), 39648 (~11.0h), 17985 (~5.0h), 8393
(~2.3h), 6703, 6013, 1541, 707**. The two ~11h gaps are overnight sleeps; the rest are long idles / breaks. A gap
**far exceeding plausible execution time** is a clean **hangover signal** on resume. As predicted in the blackout WR
note, the gap detects **THAT** the agent was out, **not the cause** (overnight idle vs guard stall vs long command vs
box crash all look the same from the gap alone) — and a `compact_boundary` between the two records tells you the
"out" was a compact specifically. So SM121's boundary check is: on resume, `now - (last stamped agent/conversational
record)`; flag when it dwarfs execution time; if a `compact_boundary` sits at the seam, name it a compact.

## Caveats

- Field extraction in the probe was regex-cheap (top-level `"type"` / `"timestamp"` / `"subtype"`); the
  `compact_boundary` subtype was confirmed by direct record inspection (printed above), not inferred.
- `transcript_path` survival across `/compact` is **confirmed for compact** (same file, inline boundary). `/clear`
  and `claude --resume`-into-fresh were **not** probed here (assumed fresh-file from first principles; verify before
  relying on it for the since-warp reset of those cases).
- Both features share **transcript-boundary parsing** — if built, factor a small shared reader (the `TranscriptStats`
  neighbourhood) rather than duplicating the scan. Ties SM117, SM118.
