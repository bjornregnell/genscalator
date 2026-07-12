# WR specimen: does compaction time scale with context fill? (hypothesis + data design, 2026-07-12)

**Origin.** While pinning a chrono-measurement step into the compact dance, BR gave the *why*: he is curious
whether the **wallclock duration of a `/compact` is proportional to the context fill** — i.e. the fuller the
window, the more transcript there is to summarize, so the longer the compaction should take. Secondary, and he
thinks unlikely: whether **context rot** correlates with compact time (probably not — rot is model degradation,
not summarization workload). All hypotheses; interesting WR data to gather.

## The hypotheses
- **H1 (primary):** compact duration ∝ fill-before. Plausible mechanism — compaction summarizes the transcript,
  and a fuller window is more input to summarize. Expect a positive, likely monotonic relationship.
- **H0-rot (secondary, expected null):** compact duration is ~independent of the agent's rot. Rot describes the
  model's reasoning degradation at high fill, not the amount of text the summarizer must process, so it should
  not drive compaction time (though fill and rot are correlated, so any apparent rot effect may just be fill).

## Why the agent can't measure this from the inside (the asymmetry that shapes the method)
The agent is **paused during compaction and has no felt time** (RQ0 family E). It cannot time the compact by
introspection; only **on-disk stamps that survive the warp** can. So the measurement is deliberately externalized:
`tt chrono now` before + after, into a persistent gitignored record.

## Data design
Running record: `muntabot-synch/tmp/compact-chrono-stamps.md` (persistent across compacts, gitignored). Each
compact logs **pre / post / elapsed / fill-before / rot-guess / notes**. Over enough compacts, plot elapsed vs
fill-before (test H1) and vs rot-guess (test H0-rot). Confounds to keep honest: model tier (compaction cost may
differ CO4 vs others), whether it was manual `/compact` vs auto-compact, and that fill and rot co-move.

Ties: the compact dance (foundations step 5), [[agent-lacks-felt-time-rebind-at-boundaries]], RQ0 family E
(agent cannot read its own gauges / time), [[propose-compact-dance-at-trigger]], SM022 (a super-harness that
auto-measures this would remove the manual stamping).
