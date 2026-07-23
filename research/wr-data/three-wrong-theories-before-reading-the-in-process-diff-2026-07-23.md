# Three wrong theories before reading the in-process diff (2026-07-23)

**Episode:** `scala-cli test tools` showed 123/163 CliSuite + 2/4 DispatchSuite failures.
Final cause: the agent ran the suite from the muntabot work-repo cwd without
`--java-prop tt.tools=...`, so the suite's documented cwd walk-up resolved a STALE 6-file
`tools/` copy living in that work repo (files, newtool, log, lib, verify, text) and tested
it: old `text` reproduced the pre-fix latin-1 mojibake (class A, tool ran), and the 27
missing tools died instantly with empty output (class B, the 40 ms deaths). The fix was one
documented property. Suite immediately green after: 163/163 + 4/4 (17:0x).

**The three wrong theories, in order, each acted on:**
1. **Build-server contention** (a sandbox minion was compiling) — plausible, refuted by an
   idle-box rerun with identical counts. Cost: one delayed conclusion.
2. **Mid-suite backend death** (~40 spawns then everything dies) — refuted by scattered
   passes late in the suite.
3. **Stale bloop project state after a toolchain update** — "confirmed" by suggestive
   evidence (two coexisting bloop servers, a JVM-25-vs-21 observation, mojibake matching an
   old bug) and acted on: two `tt bloop restart` kills, a failed `scala-cli clean` (it logs
   stacktraces INTO the directory it deletes — real tool bug, noted), and a manual
   `rm -rf tools/.scala-build`. All unnecessary surgery on a healthy box; the freed 9.2 GB
   server was just honest recompiling. Refuted by identical failures on the zero-cache rerun.

**What actually cracked it:** finally READING the in-process DispatchSuite diff, which
listed the expected verb set as exactly 5 verbs — naming the wrong-toolsDir cause directly.
It had been in every failing run's output from the start.

**Lessons (cross-refs to existing patterns):**
- **In-process failures outrank subprocess failures as evidence.** The subprocess failures
  hid their stderr behind assertions; the in-process diff printed the answer verbatim.
  Read the highest-signal failure FIRST, not the most numerous
  (`an-audit-finds-only-what-it-has-a-theory-for-2026-07-16`).
- **Deterministic identical counts across load conditions falsify load theories at once.**
  The agent noted the identical counts and still spent a theory on state corruption.
- **The failure site's own documentation beats theorizing.** cli.test.scala's header
  (lines 10-13) states the tt.tools rule; the agent had READ those lines earlier the same
  session (to learn the run() shape) and still missed the operative sentence under momentum
  ([[rot-vigil-guard-mechanical-precision-first]]).
- **State surgery needs a specific supported prediction, not a matching vibe.** Both bloop
  kills and the cache wipe were pattern-matched, not predicted-and-tested
  ([[never-blanket-allow-destructive-commands]] spirit; the harness system-prompt rule says
  exactly this).

**Open follow-ups:** (1) the stale `tools/` copy in the work repo is a standing landmine —
BR to decide delete/rename; (2) proposed hardening: CliSuite prints its resolved toolsDir
once to stderr, and could fail fast when the dir holds fewer tools than the dispatch table
expects — one line each, makes this whole class of failure self-announcing; (3) `scala-cli
clean` self-defeat (stacktrace-into-deleted-dir) is upstream-reportable; (4) proposed
`tt bloop clean` subcommand (BR, in-guard, same episode) so cache surgery has a typed shape.
