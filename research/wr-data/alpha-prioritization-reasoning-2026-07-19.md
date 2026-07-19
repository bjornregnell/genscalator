# Alpha-prioritization co-design: reasoning on the record (2026-07-19)

**Type:** WR specimen — human↔agent priority co-design; substrate-strain surfacing as a finding.

## Verbatim (the trigger)

BR, from memory + breakfast ideas, dumped **ten open threads** (T1–T10, explicitly "not in priority order"):
introprog contributor issues (T1), the peculiar non-bloop session hang (T2), build bloop-fix + push-all (T3),
native tt tools (T4), report bloop upstream (T5), security-model web + theory sync (T6), move blogs into media
(T7), unify the graphical language + a logo family (T8), re-engineer PRD + consistency audit (T9), and issue
insourcing + a pinboard re-architecture into `genscalator/work` with a human side and an agent side (T10). The ask:
*"go check the pinboard to see what i have forgotten and pin new stuff... and make a prioritized list of which open
threads that are most important from the overall goal of getting genscalator ready for alpha testing. WDYT?"*

## The reasoning (labelled reflection)

- **Reframe from task-list to tester-journey.** The organizing question was not "which tasks are biggest" but
  *"what does an external alpha tester meet, and in what order, on the v0.10.0 alpha-test-ready milestone?"* That
  produced the P0–P3 frame: **P0** reliability/platform · **P1** value-prop + onboarding · **P2** coherence/polish
  · **P3** internal velocity + ecosystem.
- **Load-bearing insight:** *the alpha gate is reliability, and reliability = native tt tools.* A tester on their
  own machine must not hit a bloop wedge on day one; everything else is polish or velocity. This collapsed a
  ten-way "everything matters" into a spine (T3/T4) with the rest ranked by first-encounter.
- **A decision under uncertainty, resolved by maturity + platform reach:** graal-for-all native-image for alpha
  (over the SN/graal split), because it maximizes the Windows chance, is JDK-complete, and SN is still pre-1.0. The
  faster/smaller Scala-Native split was *deferred*, not dropped — a "ship uniform, optimize later" call.
- **The elegant cross-link (the best move of the session):** BR's "bury history from eager-to-lazy reach" idea
  (T10/SM157) turns out to *structurally solve* the hang (T2/SM153): if the resume/baton reads only a bounded
  *current* surface and history is opt-in in `agent/history` + `human/history`, the cold-start read is **bounded by
  construction** — "bound the cold-start read as architecture, not willpower." Two separately-raised threads (a
  substrate redesign and a scary hang) turned out to be one fix.
- **Two hang hypotheses, one fix:** (1) an unbounded eager-dig through substrate on a bare resume; (2) the
  substrate has simply outgrown even a 1M agent's *usable* window (Z ≪ 1M), so a bare resume thrashes/rots. Both
  point at the same remedy: a bounded cold-start read + a smaller current surface.

## Why this is WR data

- **The substrate strained *while we used it*, and that became evidence, not just friction:** mid-prioritization,
  the PB was 4096 lines and `MEMORY.md` hit its read-limit — the same scale ceiling hypothesis-2 names. The tool
  hitting its own limits *during the task about the tool* is a substrate-as-multiplier specimen with a ceiling.
- **The co-design shape:** the human supplies an unordered idea-dump + the domain goal (alpha); the agent supplies
  structure (a priority frame), surfaces tensions (graal vs SN), flags forgotten items (SM076, SM060, version
  staleness), and holds the pin numbers until the human nods; the human decides. A repeatable division of labor for
  roadmap-shaped work.
- **Anti-rabbit-hole discipline:** the hang investigation (SM153) was pinned *with* an inferred session hash for
  later resume, but explicitly **not chased now** — labelled a time-sink, and likely obviated by the structural
  fix. Deferring a tempting investigation is itself a priority-reasoning move worth recording.
