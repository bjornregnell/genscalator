# Auto-compact fires near ~95% context fill; the warp is a ~40x lossy compression of message-history (2026-07-10 17:03)

BR-flagged WR datum, empirically measured this session at a real warp boundary. BR deliberately did NOT
`/compact` manually; he waited to trigger the **auto-compact** and captured `/context` on both sides.

## The measurement (checkable, from the two `/context` pastes)
- **Pre-compact (auto-compact trigger point):** total 982.7k / 1M = **98%**; **Messages: 955.2k = 95.5%**;
  Free space 17k = 1.7%. So the auto-compact fired once context reached ~95-98% fill (near-full) - the
  message-history alone was at **95.5%**.
- **Post-compact:** total 51k / 1M = **5%**; Messages **23.5k**; Free space 949k = 94.9%.
- **Reclaimed:** ~932k tokens. The **955k of message-history distilled to ~23.5k of summary = a ~40x lossy
  compression.** That ratio IS the measured size of "what a compact discards" (session-texture) versus "what the
  summary keeps".

## Why it matters (ties to the substrate thesis)
- Confirms the **three-size-measures** split ([[agent-surfaces-substrate-size-measures-three-kinds-2026-07-10]])
  empirically: the on-disk transcript `.jsonl` (~157MB+) is UNTOUCHED by the compact; only **context-fill**
  reset. Disk-size and context-fill move independently.
- The ~23.5k summary is a lossy sketch; the reason work continues seamlessly is the externalized **substrate**
  (resume-prompt + committed memories/foundations/blog). A measured instance of the thesis: **externalized
  substrate does the preserving; the compact summary is just enough to re-anchor to it.** The context-dump
  (`tmp/session-snapshot-2026-07-10-153514.md`) was written precisely to hold the texture the 40x compression drops.

## Calibration footnote
The 15:35 context-dump self-reported "~76% fill"; harvest-hot-context mode then rode it up to the 95.5%/98%
auto-trigger. Felt and measured AGREED at 76% (unlike the earlier 35% over-read) - the climb to 98% was genuine
extra harvesting, not a mis-read. Threshold caveat: one observation - the auto-compact trigger sits somewhere
around 95%+ message-fill; not proven to be an exact constant. Ties: the felt-vs-measured pacing data, blog 012
(the "gravity well"), [[raw-data-append-only]].
