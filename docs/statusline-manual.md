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
genscalator:  14:23:07  O4.8 (1M ctx)  ctx-fill: 41%  5h-lim: 30%  wk-lim: 14% resets: 3d  cost: $12.34
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
| **`ctx-fill: 41%`** | green (→ orange ≥ 70%, red ≥ 90%) | How full the context window is. | This is the rot axis. As it climbs toward the smart-zone ceiling (roughly 24–30% of a 1M window is where the compact trigger sits in this workflow), plan a **compact dance** — consolidate, commit, then compact — rather than letting it auto-compact mid-thought. |
| **`5h-lim: 30%`** | purple (→ orange/red by the same thresholds) | Fraction of your rolling 5-hour session limit used. | If it is high and climbing, ease off or wrap up before you get blocked. |
| **`resets: 2h34m`** | dim grey | Fine countdown (hours + minutes) until the 5-hour window resets. Shown only if CC sends `rate_limits.five_hour.resets_at`. | Tells you how long until the session budget refreshes. |
| **`wk-lim: 14%`** | rosy red (→ orange/red by threshold) | Fraction of your weekly (7-day) limit used. | The slow-moving budget. High + a distant reset = pace the week. |
| **`resets: 3d`** | dim grey | Coarse countdown (m / h / d) until the weekly window resets. | When the weekly budget refreshes. |
| **`cost: $12.34`** | blue | Notional API-equivalent cost of the session. | On a fixed monthly plan this is **not** a real charge — it is the least interesting number, which is why it is placed last and drops off first on a narrow terminal. |

### The gauge grading (the three limit/fill segments)

`ctx-fill`, `5h-lim`, and `wk-lim` each start in their own healthy hue and escalate:

- **healthy** — below 70%: the segment's base colour (green / purple / rosy).
- **orange** — 70% or above: getting full; start planning.
- **red** — 90% or above: act now (compact, ease off, or expect a limit soon).

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
