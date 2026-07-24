# The `tt statusline` bar — how to read it, act on it, and turn it on or off

> A one-line, always-visible gauge of your Claude Code session: what model you are on, how full the
> context window is, how close you are to your usage limits, and the running cost. It replaces the
> `/cost` paste-dance with a live readout, and its leading `genscalator` label doubles as proof that
> the genscalator plugin plus statusline are actually wired up and live.

Claude Code pipes a small JSON object to the configured `statusLine` command on each conversation event
(not on a wall-clock timer, and throttled). `tt statusline` reads that JSON and prints one compact,
colour-coded line. Every segment is independently guarded: a field the JSON does not carry simply omits
its segment, so the bar degrades gracefully across CC versions and subscription tiers and never crashes
your prompt.

## What a full line looks like

```
genscalator 14:23:07 silent·3s  f5·1M  ctx·41%  rot?↑120k  tot↑180k  lim·res·5h·30%·4h|w·14%·3d  $12
```

Segments are separated by two spaces, in a fixed left-to-right order. If the line is wider than your
terminal, CC truncates it at the right edge (it does not wrap), so the least-important segment — cost —
falls off first by design.

**The space diet (BR, 2026-07-24) and the two glue glyphs.** Labels were shortened (`ctx-fill`→`ctx`,
`lim·reset`→`lim·res`, `wk`→`w`, `cost $N`→`$N`), countdowns are largest-unit-only, clock+silent fused
into one segment, and the lim block welded into one unit with `|` between windows. Two glyphs carry
KIND: **`·` glues a label to a state/level** (`silent·3s`, `ctx·41%`, `f5·1M`, `w·14%·3d`) while
**`↑` glues a label to an output-FLOW count** (`rot?↑120k`, `tot↑180k`) — so the glue itself tells you
whether you are reading a level or a flow.

## Reading each segment, and what to do about it

| Segment | Colour | Means | What to do |
|---|---|---|---|
| **`genscalator`** | bold green | The plugin plus statusline are active. If this prefix is **absent**, genscalator is not wired up. | Nothing — it is your at-a-glance "am I live?" indicator. |
| **`14:23:07`** | light grey | Local wall clock, HH:MM:SS. **It freezes while the agent is working and ticks again when control returns to you.** | Use the freeze/tick as a turn signal: ticking clock = your move in the ballgame; frozen = the agent has the ball. |
| **`silent·3s`** | dim grey | Feed inactivity: now minus the last timestamped transcript record. COUNTED, not inferred — no threshold, no colour, no alarm; its subject is the FEED, not a person. Rides one space after the clock (they are both time facts). NB a running command writes no transcript record, so agent-busy time counts as silence. | Nothing — a readout of how long since anything landed in the feed. |
| **`f5·1M`** | cyan | The model, abbreviated (`Opus 4.8` → `o4.8`, `Fable 5` → `f5`, `Sonnet 5` → `s5`, `Haiku 4.5` → `h4.5`; the family letter is lower-case so `o` does not read as a zero), with the context-window size as a `·1M` suffix (middot, not `/`: a capacity tag, not a ratio) — taken from the measured `context_window.context_window_size` field, falling back to a size spelled in the display name. | Confirm you are on the model (and window) you intend. |
| **`ctx·41%`** | green (→ orange at the compact trigger ~24%, **red at the dumb-zone ceiling ~30%**) | How full the context window is (label dieted from `ctx-fill`; the FILL sense is unchanged). | This is the rot axis. Orange means you have crossed the **compact-dance trigger** (~0.8·Z) — start consolidating; **red means you are at the smart-zone ceiling Z and risking the dumb zone** (context rot) — do the compact dance now (commit, then compact) rather than letting it auto-compact mid-thought. Note this reds *early* (~30% of a 1M window), not near 100% — a full-but-rotting window is the danger, not a technically-full one. |
| **`rot?↑120k  tot↑180k`** | graded (rot?) / dim (tot) | Agent output tokens SINCE the last warp/compact (`rot?↑` — the current-window rot proxy; the `?` marks it inferred, the `↑` marks output-FLOW) and for the whole session (`tot↑`; dropped on a narrow terminal). | Read `rot?↑` as "how hard has the agent worked this window" — a second rot axis besides `ctx-fill`; see the do-not-reconcile note below the grading section. |
| **`lim·res·`** | dim grey | Shared legend for the usage-limit block, middot-WELDED to its first cluster. **Its middots mirror the value middots** — each cluster reads `window·used%·countdown`. Shown only when at least one limit is present. The whole block is one visual unit: gray `|` between windows, each window its own hue. | Nothing — it is the column header telling you how to read the clusters. |
| **`5h·30%·4h`** | purple (→ orange ≥ 70%, **red at/above the warn threshold, default 80%**); the **whole cluster** — the % and its reset — shares the one hue | Your rolling 5-hour session limit: **% used** `·` **countdown** (largest unit only) to reset. The % anchors the middle: the window label reads before it, the countdown after (both may end in `h`). The reset half shows only if CC sends `rate_limits.five_hour.resets_at`; with no %, just the window + reset show, ungraded. | When it reddens you are near the session cap — ease off, checkpoint, or wrap up. The reset reddens **with** its limit (same cluster colour), so cap-and-relief read as one unmistakable block. |
| **`w·14%·3d`** | rosy (→ orange ≥ 70%, **red at/above the warn threshold**); whole cluster shares the hue | Your weekly (7-day) limit (label dieted from `wk`): **% used** `·` **coarse countdown** to reset. Any EXTRA `rate_limits` window a future CC version adds (e.g. a per-model weekly) joins the block automatically with a compacted label, e.g. `f5·77%·3d` — as of CC 2.1.218 the feed carries only the two windows, verified live 2026-07-24. | The slow-moving budget. Reddens near the weekly cap; a distant reset then means pace the rest of the week. |
| **`$12`** | blue | Notional API-equivalent cost of this session, in whole dollars (cents dropped to save space): **what the tokens this conversation has burned would cost if billed at pay-as-you-go API rates.** It is cumulative across the session (and survives a compact), and is *not* tied to your monthly billing period. | On a fixed monthly plan this is **not** a real charge — it is a token-consumption meter, the least interesting number, which is why it is placed last and drops off first on a narrow terminal. |

### The gauge grading (the three limit/fill segments)

`ctx` and the limit clusters (`5h`, `w`) each start in their own healthy hue and escalate:

- **healthy** — below 70%: the segment's base colour (green / purple / rosy).
- **orange** — 70% or above: getting full; start planning.
- **red** — act now (compact, ease off, or expect a limit soon).

The **red** trigger differs by segment, because "danger" means something different for each:

- **`ctx`** is graded to the **compact-dance math**, not a near-full window:
  - **orange** at the **compact trigger** ≈ 0.8·Z (default ~24%) — start consolidating toward a compact.
  - **red** at the **smart-zone ceiling Z** (default ~30%) — you are risking the **dumb zone** (context rot);
    do the compact dance now. It reds *early* on purpose: a context window at 30% of 1M is already rot-risky
    long before it is technically full, so waiting for 90% would be far too late.
  - Configure with **`--ctx-warn N`** (default 30). Orange automatically tracks 0.8·N.
- **The `5h` and `w` clusters** are graded to the **usage-limit warn threshold**:
  - **orange** at 70%, **red** at the **warn threshold (default 80%)** — and when a limit reddens **its reset
    countdown reddens with it**, so an approaching cap is unmistakable at a glance. This is the ambient
    early-warning slice of the usage-limit WARNING requirement.
  - Configure with **`--warn N`** (default 80). Applies to both limit gauges.

**Putting both in the settings command string:** e.g. `"command": "tt statusline --warn 85 --ctx-warn 28"`.
The two thresholds are independent — the usage-limit warn (a subscription-budget signal) and the ctx-fill
dumb-zone threshold (a rot signal) mean different things and default to different values (80 vs 30).

**`ctx` vs `rot?↑` / `tot↑` — different quantities, do not reconcile them.** `ctx` is window
**occupancy** (a percentage: how full the context window is right now). `rot?↑` and `tot↑` are cumulative
**agent output tokens generated** (a count: `rot?↑` since the last warp/compact, `tot↑` for the whole
session) — the `↑` marks them as output-*flow*, not occupancy. A flow-count and an occupancy-level are
decoupled: you can generate a large `rot?↑` while `ctx-fill` stays low, or fill the window with one big
paste at near-zero `rot?↑`. So `4%` and `2k` are not two views of one number and never add up — read
`ctx` for rot-risk, `rot?↑` for how hard the agent has been working this window. (Aside: `rot?↑` equals
`tot↑` exactly when there has been no compact since the session started — a free "no warp yet" signal.)

## Line 2 — the mode line (`--mode-line`)

An optional second row for the **declared joint state-of-mind**: short CamelCase labels (`TokSpend`, `RotVigil`,
`Afk`, ...; CamelCase so labels map 1:1 onto the planned `enum ModeChips` case names) that you or the agent add with `tt mode add <label>` (shorthand `+<label>`) and remove with
`tt mode rm <label>` (`-<label>`). The contract: everything on this row was **declared by someone** —
measured things live on line 1 and line 3, so the surface itself encodes the provenance. Renders as a
`gs mode set` prefix plus one colour chip per active mode.

## Line 3 — the box line (`--box-line`)

An optional third row of **measured box health**, read directly from `/proc` and `/sys` on each update
(file reads only, no subprocess). Linux-only by data source; on any other platform it silently does not
print — nothing breaks.

```
box huffing  mem 45%·14.1G·31.2G  load 64%·5.1avg·8cores  temp 63C  disk 78%·110Gfree  jvm 4x5.1G  bloop 5.0G
```

| Segment | Means | Grading |
|---|---|---|
| **`box healthy` / `box huffing` / `box swamped`** | The lead verdict: the WORST severity across the segments, computed from the same thresholds that colour them — not a new inference, just the colour semantics lifted into the name. (Each name is exactly 11 characters, so the three row-leads — `genscalator`, `gs mode set`, `box healthy` — align.) | green / orange / red = flips exactly when a segment leaves green |
| **`mem 45%·14.1G·31.2G`** | Memory used and total; the leading % is the exact number the colour grades on. "Used" = total − available, the kernel's reclaimable-aware figure (matches `free`'s *available*, not *free*). | orange ≥ 70%, red ≥ 90% |
| **`load 64%·5.1avg·8cores`** | The 1-minute load average over the core count: "5.1 cores' worth of demand on 8 cores". Load measures **demand** (tasks running or waiting, including disk-wait), not CPU busy-time — hence the label `load`, not `cpu`. A true cpu% would need a two-sample delta (SM165). | orange ≥ 70%, red ≥ 90% |
| **`temp 63C`** | The hottest thermal zone in °C (the fan story). | orange ≥ 70, red ≥ 85 |
| **`disk 78%·110Gfree`** | Root filesystem: the leading % is space USED (what the colour grades on); the absolute is space FREE (what you act on). | orange ≥ 80%, red ≥ 90% (disks run fuller than mem) |
| **`jvm 4x5.1G`** | Running JVM count × their combined RSS. Informational (dim, ungraded) — JVMs are the heavy processes on a dev box, but their weight already counts inside `mem`. | ungraded |
| **`bloop 5.0G`** | Shown only when a bloop compile daemon is running (matched by cmdline substring): its RSS. The known wedge-and-drain villain gets its own chip so its regrowth is visible early. | orange ≥ 2G, red ≥ 6G |

All thresholds are first-cut guesses (bloop's calibrated on a lived 10.4 GB specimen); they live in
`tools/statusline.scala` — tune against reality.

## Turning it on and off

**On** — add one line to `.claude/settings.json` (top level; merge alongside your existing keys, do not
replace the file):

```json
"statusLine": { "type": "command", "command": "tt statusline" }
```

Append `--mode-line` and/or `--box-line` to the command string to also render lines 2 and 3, e.g.
`"command": "tt statusline --mode-line --box-line"`. The three lines toggle independently, so you budget
your own vertical space.

Then reload with `/hooks` (or restart the session). The bar appears at the bottom of your prompt.

**Off** — remove that `"statusLine"` key from `.claude/settings.json` and reload with `/hooks` (or
restart). There is no runtime toggle; presence of the key is the switch.

## When a segment is missing

Nothing is wrong — the tool only prints a segment when its field is present in the JSON CC sent:

- The `rate_limits.*` clusters (`5h`, `w`, welded under the `lim·res·` legend) are a Claude Pro/Max
  feature; on other tiers they simply do not appear. A per-model weekly limit (the one `/usage` shows,
  e.g. Fable) is NOT in the feed as of CC 2.1.218 — the day CC adds it, it joins the block automatically.
- A completely empty or non-JSON stdin prints a **blank line** (exit 0) — it will never break your prompt.
- The leading clock is only added once the JSON parses, so a blank line really is blank (no stray clock).

## Where this lives

The tool is `tools/statusline.scala` (`tt statusline`). It reads stdin by default, and for deterministic
tests also accepts the JSON as a positional argument plus `--now-ms N` to pin the clock. See the toolbox
test suite for the exact fields consumed and the rendering assertions.

**Raw capture (what is CC actually sending?):** touch `~/.claude/gs-statusline-dump-on` and the next
render tees its raw stdin JSON to `~/.claude/gs-statusline-last.json`; remove the marker to stop. This is
the recall-free way to confirm the fields against a real invocation when a CC version changes the feed.
