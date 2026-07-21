# scala-cli --server=false: the bloop escape hatch (measured, verdict calibrated)

2026-07-21 18:00 (clock-reads 17:59:32-18:00:24). BR asked, on seeing it work: "is this a
breakthrough finding?" Answer after measurement: not a default-path breakthrough, but a
first-class ESCAPE HATCH plus a strong launcher-design candidate.

## The finding
During SM191 (gitlab mirror), the bloop vanished-launcher wedge RECURRED (the 07-18 signature:
`sh: 0: cannot open /tmp/start-bloop*.sh: No such file`): two mirror.sc runs failed at bloop
SPAWN even though the script was cache-hot and unchanged, and `tt bloop restart` (which killed a
6.7G daemon) did not cure the respawn. `scala-cli run --server=false mirror.sc` ran CLEAN on the
first try: no daemon in the path at all. The wedge blocks even no-compile runs; server=false
removes the dependency entirely.

## The measurement (coarse, confounded, still decisive)
Two cache-hot `scala-cli run --server=false chrono.scala` probes, bounded by feed clock-reads:
each landed in the 5-10s range (bounds include agent turn latency, so real cost is toward the
low end). Warm-bloop `tt chrono now` baseline: ~0.6s. A 10x-ish per-call tax ⇒ NOT viable as
the hot-path default (tt text/statusline run constantly). Instrument honesty: inter-call gaps
include model latency; a proper paired benchmark would tighten this, but the order of magnitude
already answers the design question.

## Verdicts, enumerated
1. **Escape hatch (use TODAY)**: when bloop wedges, `scala-cli run --server=false <script>` is
   the zero-dependency bypass — no kill dance, no human, works mid-wedge. Goes in the
   avoid-guard-stall/tt lore next time the wedge bites.
2. **Not the interim no-bloop default**: the latency tax is 10x on the hot path; SM146 native
   remains the real endgame.
3. **Launcher-design candidate (SM181/SM146 family, the promising part)**: `tools/tt` could
   auto-retry with `--server=false` when the normal run fails with the bloop-spawn signature —
   the wedge would then cost one slow call instead of a stalled session. Fix lives in the
   TOOL'S INTERFACE (survives warps, needs no recall) per avoid-guard-stall's where-fixes-live
   table. BR-gated ship (tt is the security-critical launcher).
4. **Recurrence datum**: the vanished-launcher wedge is now a REPEAT (2026-07-18, 2026-07-21,
   twice today post-reboot) — whatever eats /tmp/start-bloop*.sh is alive on this box; the
   SM146 motivation compounds.

Ties SM146, SM181, SM191, [[blixten-box-flaky]], wr-data `tt-box-lacks-local-health-shape`.
