# The `tt statusline` bar — how to read it, act on it, and turn it on or off

> A one-line, always-visible gauge of your Claude Code session: what model you are on, how full the
> context window is, how close you are to your usage limits, and the running cost. It replaces the
> `/cost` paste-dance with a live readout, and its leading `genscalator:` label doubles as proof that
> the genscalator plugin plus statusline are actually wired up and live.

Claude Code pipes a small JSON object to the configured `statusLine` command on each conversation event
(not on a wall-clock timer, and throttled). `tt statusline` reads that JSON and prints one compact,
colour-coded line. Every segment is independently guarded: a field the JSON does not carry simply omits
its segment, so the bar degrades gracefully across CC versions and subscription tiers and never crashes
your prompt.

## What a full line looks like

```
genscalator:  14:23:07  O4.8 (1M ctx)  ctx-fill: 41%  5h-lim: 30%  wk-lim: 14% resets: 3d  cost: $12
```

Segments are separated by two spaces, in a fixed left-to-right order. If the line is wider than your
terminal, CC truncates it at the right edge (it does not wrap), so the least-important segment — cost —
falls off first by design.

## Reading each segment, and what to do about it

| Segment | Colour | Means | What to do |
|---|---|---|---|
| **`genscalator:`** | bold green | The plugin plus statusline are active. If this prefix is **absent**, genscalator is not wired up. | Nothing — it is your at-a-glance "am I live?" indicator. |
| **`14:23:07`** | light grey | Local wall clock, HH:MM:SS. **It freezes while the agent is working and ticks again when control returns to you.** | Use the freeze/tick as a turn signal: ticking clock = your move in the ballgame; frozen = the agent has the ball. |
| **`O4.8 (1M ctx)`** | cyan | The model, abbreviated (`Opus 4.8` → `O4.8`, `Sonnet` → `S`, `Fable` → `F`, `Haiku` → `H`) and its context window. | Confirm you are on the model you intend. |
| **`ctx-fill: 41%`** | green (→ orange at the compact trigger ~24%, **red at the dumb-zone ceiling ~30%**) | How full the context window is. | This is the rot axis. Orange means you have crossed the **compact-dance trigger** (~0.8·Z) — start consolidating; **red means you are at the smart-zone ceiling Z and risking the dumb zone** (context rot) — do the compact dance now (commit, then compact) rather than letting it auto-compact mid-thought. Note this reds *early* (~30% of a 1M window), not near 100% — a full-but-rotting window is the danger, not a technically-full one. |
| **`5h-lim: 30%`** | purple (→ orange ≥ 70%, **red at/above the warn threshold, default 80%**) | Fraction of your rolling 5-hour session limit used. | When it reddens you are near the session cap — ease off, checkpoint, or wrap up before you get blocked. |
| **`resets: 2h34m`** | dim grey, **but red when its limit is at/above the warn threshold** | Fine countdown (hours + minutes) until the 5-hour window resets. Shown only if CC sends `rate_limits.five_hour.resets_at`. | A red reset says: the cap is close, and here is how long until relief. |
| **`wk-lim: 14%`** | rosy (→ orange ≥ 70%, **red at/above the warn threshold, default 80%**) | Fraction of your weekly (7-day) limit used. | The slow-moving budget. Reddens near the weekly cap; a distant reset then means pace the rest of the week. |
| **`resets: 3d`** | dim grey, **red when its limit is at/above the warn threshold** | Coarse countdown (m / h / d) until the weekly window resets. | When the weekly budget refreshes. |
| **`cost: $12`** | blue | Notional API-equivalent cost of this session, in whole dollars (cents dropped to save space): **what the tokens this conversation has burned would cost if billed at pay-as-you-go API rates.** It is cumulative across the session (and survives a compact), and is *not* tied to your monthly billing period. | On a fixed monthly plan this is **not** a real charge — it is a token-consumption meter, the least interesting number, which is why it is placed last and drops off first on a narrow terminal. |

### The gauge grading (the three limit/fill segments)

`ctx-fill`, `5h-lim`, and `wk-lim` each start in their own healthy hue and escalate:

- **healthy** — below 70%: the segment's base colour (green / purple / rosy).
- **orange** — 70% or above: getting full; start planning.
- **red** — act now (compact, ease off, or expect a limit soon).

The **red** trigger differs by segment, because "danger" means something different for each:

- **`ctx-fill`** is graded to the **compact-dance math**, not a near-full window:
  - **orange** at the **compact trigger** ≈ 0.8·Z (default ~24%) — start consolidating toward a compact.
  - **red** at the **smart-zone ceiling Z** (default ~30%) — you are risking the **dumb zone** (context rot);
    do the compact dance now. It reds *early* on purpose: a context window at 30% of 1M is already rot-risky
    long before it is technically full, so waiting for 90% would be far too late.
  - Configure with **`--ctx-warn N`** (default 30). Orange automatically tracks 0.8·N.
- **`5h-lim` and `wk-lim`** are graded to the **usage-limit warn threshold**:
  - **orange** at 70%, **red** at the **warn threshold (default 80%)** — and when a limit reddens **its reset
    countdown reddens with it**, so an approaching cap is unmistakable at a glance. This is the ambient
    early-warning slice of the usage-limit WARNING requirement.
  - Configure with **`--warn N`** (default 80). Applies to both limit gauges.

**Putting both in the settings command string:** e.g. `"command": "tt statusline --warn 85 --ctx-warn 28"`.
The two thresholds are independent — the usage-limit warn (a subscription-budget signal) and the ctx-fill
dumb-zone threshold (a rot signal) mean different things and default to different values (80 vs 30).

## Turning it on and off

**On** — add one line to `.claude/settings.json` (top level; merge alongside your existing keys, do not
replace the file):

```json
"statusLine": { "type": "command", "command": "tt statusline" }
```

Then reload with `/hooks` (or restart the session). The bar appears at the bottom of your prompt.

**Off** — remove that `"statusLine"` key from `.claude/settings.json` and reload with `/hooks` (or
restart). There is no runtime toggle; presence of the key is the switch.

## When a segment is missing

Nothing is wrong — the tool only prints a segment when its field is present in the JSON CC sent:

- The `rate_limits.*` segments (`5h-lim`, `wk-lim`, their `resets:` countdowns) are a Claude Pro/Max
  feature; on other tiers they simply do not appear.
- A completely empty or non-JSON stdin prints a **blank line** (exit 0) — it will never break your prompt.
- The leading clock is only added once the JSON parses, so a blank line really is blank (no stray clock).

## Where this lives

The tool is `tools/statusline.scala` (`tt statusline`). It reads stdin by default, and for deterministic
tests also accepts the JSON as a positional argument plus `--now-ms N` to pin the clock. See the toolbox
test suite for the exact fields consumed and the rendering assertions.
