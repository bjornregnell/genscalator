# Status-line sample: a fresh clear + resume-prompt + one paste reads `ctx-fill 4%` — the SM139 instrument, first captured reading

**2026-07-17, ~18:43.** BR pasted the live status line into the session as WR data, sampled **right after** a `/clear`,
the load of `tmp/resume-prompt.md`, my three STATE `VERIFY:`s, and BR's one paste (`go look at tmp/resume-prompt.md
and await my instruction`). Verbatim:

```
genscalator  18:43:46  silent 1m  o4.8/1M  ctx-fill 4%  rot? 2k  tot 2k  5h-lim 1% reset 4h36m  wk-lim 15% reset 3d  cost $0
```

## Why log it — this is SM139 happening

SM139 is *sample the status/mode line into an append-only research log; the line that saves BR's attention live is the
natural DV for the rot/heat theory over time.* This is the **first deliberate capture of that DV** with a **known
session state attached** (fresh clear, this exact 73-line resume prompt, one paste, three `tt`/`git` VERIFY calls).
That makes it a **calibration anchor**: it is what the instrument reads at the *floor* of a session.

## The floor reading, held as data (not conclusion)

- **`ctx-fill 4%`** on the 1M window (`o4.8/1M`) ⇒ ~40k tokens of loaded context: CLAUDE.md + the ~130-line MEMORY.md
  index + all skill/agent descriptions + tool schemas + the resume prompt + a few tool results. So **"start of a
  cleared session" is not ~0% — the standing substrate (memory index, skills, tools) already costs ~4%.** Useful
  baseline for reasoning about how much headroom a `/clear` actually buys.
- **`rot? 2k` = `tot 2k`** — at the floor, the rot estimate EQUALS the total. Nothing has decayed yet; the two
  counters coincide at t0 and would be expected to diverge as the session runs. Their *divergence over time* is the
  quantity the rot theory predicts — this note fixes the t0 datum they diverge from.
- **`silent 1m`** — 1 min since last activity (matches: I ran VERIFYs, then waited). **`cost $0`** — rounds to zero
  this early. **`5h-lim 1%` (reset 4h36m), `wk-lim 15%` (reset 3d)** — usage headroom; consistent with `tok-spend`
  mode being safe to run in.

## ⚠️ The instrument-legibility snag worth flagging (ties SM134)

**`ctx-fill 4%` (~40k tokens) does NOT reconcile with `tot 2k` / `rot? 2k` (2000).** 2k tokens is ~0.2% of 1M, not
4%. ⇒ **`ctx-fill` and `tot`/`rot?` are measuring different things** (plausibly: `ctx-fill` = whole loaded window;
`tot`/`rot?` = a per-turn or output-token counter, or a windowed subset). A glancer could read all three as "context
size" and mis-add them. **This is a concrete legibility defect for the SM134 status-line defect list** (sibling to
"mode-line provenance" #3 and "whose-state" #6): *the line does not say what unit each number is in, so two fields
that look comparable are not.* Not fixed here — recorded so the fix has a witness.

## Honest limits

- **N=1, single reading**, no time series yet — this is the anchor, not the trend. The value arrives when later
  samples in the SAME session are captured and the rot/tot divergence is plotted.
- **The reconciliation gap is inferred, not confirmed** — I have not read `tt statusline`'s source to prove what
  `tot`/`rot?` count. The claim "different units" is the honest read of the numbers; the exact definitions want a
  code check (SM118 is the feasibility ground for what the line can read).

## Ties

**SM139** (this IS its first datapoint) · **SM134 #3/#6** (status/mode-line defects — add the unit-legibility snag) ·
**SM118** (what `tt statusline` can actually read) ·
[[statusline-glance-replaces-the-slash-cost-dance-a-built-instrument-anthropic-didnt-ship-2026-07-17]] (the UX-win
sibling; this is the same line seen as a research instrument) · [[token-budget-modes]] (the lim/cost half) ·
[[felt-length-overestimates-context-fill]] (why a rendered fill beats a felt one).
