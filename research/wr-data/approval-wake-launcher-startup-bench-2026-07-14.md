# WR data: bash vs C vs Scala Native — launcher startup micro-benchmark (2026-07-14)

**Status: PRESTUDY** — a quick no-op wall-clock micro-benchmark, enough to decide the launcher-language question
for `approval-wake`, NOT a rigorous performance study. A thorough version (flamegraphs, `perf`/`strace` of the
startup path, real workloads instead of a no-op) is Future Work (below).
**Type:** engineering experiment. **Origin:** BR's idea — implement the "performance-critical" approval-wake
launcher in pure C AND Scala Native (LLVM → native binary) and benchmark startup against the bash version.
**Decoupled from** the bing-bing 30s-lag investigation ([[harness-ux]] Notification addendum): the lag is an
audio-path/screen-lock issue, NOT a launcher-language one — so this experiment stands on its own (settle the
launcher cost empirically + dogfood Scala Native), it is *not* a fix for the lag.
**Threads:** [[prefer-scala-scratch-over-bash]], [[dependency-preference-cascade]], the SM105 bash-not-Scala
design guideline, blog 022 (brittle-bash → beautiful-Scala — this is the *counter*-nuance: when NOT to reach).

## Question
Is bash's interpreter the bottleneck for a launcher hook? BR's hypothesis: "bash is still interpreted so C
should be faster, and Scala Native compiled to a binary via LLVM would be almost as lean and mean [as C]."

## Method
Three do-nothing launchers, each measuring PURE startup (fork + exec + run + reap of a binary that immediately
exits) — no downstream work, so nothing confounds the language's launch cost. 300 timed runs each after 3
warmups; child stdout/stderr discarded. Harness = a JVM Scala program timing `ProcessBuilder(...).waitFor()`
with `System.nanoTime()`. Box: this dev machine (Ubuntu, clang 18.1.3, gcc 13.3.0, Scala 3.8.4). Scala Native
0.5.12, `nativeMode release-fast`, Immix GC, multithreading auto-disabled.

Sources (all in `tmp/nativebench/`, gitignored — embedded here for reproducibility):

```bash
# noop.sh
#!/usr/bin/env bash
exit 0
```
```c
/* noop.c */
int main(void) { return 0; }
```
```scala
// noop-native.scala  (baseline: release-fast + default Immix GC)
//> using scala 3.8.4
//> using platform scala-native
//> using nativeMode release-fast          // -O2 + LTO; the SN-documented mode for startup latency
@main def noopNative(): Unit = ()
// variants tested (same @main body): + `//> using nativeGc none`  (no-GC, per SN's own "short-running CLI"
// recommendation); and `//> using nativeMode release-size` (-Oz) + `nativeGc none` (minimal-image attempt).
```

Build commands:
```
gcc -O2 noop.c -o noop-c
scala-cli --power package noop-native.scala         -o noop-native          -f   # ~28s (incl first SN fetch)
scala-cli --power package noop-native-nogc.scala    -o noop-native-nogc     -f   # ~23s (cached)
scala-cli --power package noop-native-relsize.scala -o noop-native-relsize  -f   # ~23s (cached)
```
Bench harness: `bench.scala` (JVM) — `ProcessBuilder(exe).redirectOutput(DISCARD).redirectError(DISCARD)`,
timed with nanoTime, 3 warmups + N timed runs, prints min/median/mean/max ms. Run:
`scala-cli run bench.scala -- 300 <exe> ...`.

## Results (seven-way; SN rows 300 runs, JVM/GraalVM rows 200 runs; ms)

| launcher                          | min    | median  | mean    | max     | binary size                    |
|-----------------------------------|--------|---------|---------|---------|--------------------------------|
| C (`noop-c`)                      | 0.664  | 0.758   | 0.783   | 1.455   | 15.8 KiB (15776 B)             |
| bash (`noop.sh`)                  | 1.445  | 1.587   | 1.693   | 2.765   | 125 B                          |
| SN release-fast + **none GC**     | 1.579  | 1.851   | 1.947   | 4.288   | 1.64 MiB (1720328 B)           |
| SN release-fast + Immix           | 1.584  | 1.914   | 2.013   | 4.363   | 1.67 MiB (1752672 B)           |
| SN release-size + none GC         | 1.574  | 1.827   | 1.915   | 4.159   | 1.64 MiB (1724024 B)           |
| **GraalVM native-image**          | 2.469  | 3.049   | 3.205   | 7.164   | **12.25 MiB** (12848896 B)     |
| **JVM (bootstrap launcher)**      | 103.3  | 130.7   | 131.6   | 174.4   | 230 KiB (235764 B) + needs JVM |
| **python3 3.12 (interpreted)**    | 9.795  | 11.610  | 12.297  | 30.188  | source only, needs python3     |
| **node 22 / JavaScript (interp)** | 20.544 | 26.253  | 26.986  | 42.269  | source only, needs node        |

(python3/node added 2026-07-15; interpreted no-ops run via shebang-less `python3 noop.py` / `node noop.js`,
same harness. TypeScript is not a distinct runtime point — it compiles to JS and runs on node, so its startup ≡
node's; its distinguishing cost is compile time (`tsc`). Go, Rust, Bun, Deno not installed on this box —
candidates for BR to add: Go and Rust would test the "compiled-with-GC starts C-fast" (Go) and "C-fast AND
memory-safe" (Rust) questions.)

A packaged JVM bootstrap measures **JVM program startup** (~131 ms). A real `tt` call is ~500 ms because
`scala-cli run` adds a build-check/resolution layer *on top* of this JVM startup — so two separate costs stack
in daily use (packaging to a bootstrap removes the scala-cli layer; native removes the JVM startup too).
GraalVM native-image was confirmed a **genuine** native image (built `--no-fallback`; not a JVM-launcher
fallback). GraalVM CE 17, Serial GC; SN 0.5.12, release-fast, Immix/None GC.

## Compile / build time (the other axis, added 2026-07-15)
Startup is only half the developer-experience story; build time is the other half (the edit-compile-run loop).

| target | build time | notes |
|---|---|---|
| bash | 0 | interpreted, no build |
| python3 | 0 | interpreted (negligible bytecode) |
| node / JavaScript | 0 | JIT, no separate build |
| C (`gcc -O2`) | **~0.05 s** | measured (52 ms) |
| JVM Scala (`scala-cli package`, bootstrap) | ~12 s | cold; incremental is faster |
| Scala Native (`release-fast`, cached) | ~23 s | ~28 s first time incl. artifact fetch |
| GraalVM native-image | ~40–50 s | native-image step alone ~37 s |

**The two-axis tradeoff (the sharpened fit-to-task lesson).** Plotting startup vs build time, four regimes:
- **Fast-both:** C (0.05 s build, 0.8 ms start) — ideal, but brittle (the post's whole point).
- **Zero build, moderate start:** bash / python3 / node — instant edit-run loop, startup 1.7–26 ms.
- **Slow build, fast start:** Scala Native (23 s → 1.8 ms), GraalVM (40 s → 3.2 ms) — you pay a **20–40 s build**
  for near-native startup. Good for ship-once-run-many; **bad for a tool you rebuild often**, because the build
  cost dominates the dev loop. So native-compiling a `tt` tool only pays for a **stable, hot, frequently-INVOKED
  (not frequently-rebuilt)** tool — a sharper conclusion than startup alone gave.
- **Medium build, slow start:** JVM Scala (12 s → 142 ms) — worst on startup, but no native-compile step and JIT
  throughput after warmup.

**Bonus finding:** the interpreters BEAT the JVM on startup — python3 ~12 ms and node ~26 ms vs the JVM's
~142 ms — despite the JVM being "compiled." The JVM's runtime-load + bytecode-verify + JIT-machinery startup
tax exceeds a scripting interpreter's cold start by 5–12×. "Compiled vs interpreted" does not predict startup;
the *runtime's* fixed init cost does.

## Findings
1. **C is fastest — ~2.2× bash — but by only ~0.9 ms** (0.77 vs 1.69 ms median). BR's "C faster" confirmed:
   just the dynamic loader + libc init, no interpreter.
2. **No Scala Native config beats bash-tier, and none comes near C.** All SN variants cluster 1.7–1.9 ms
   (~2.3× C). This is a **fixed floor set by SN runtime init**, and it barely moves:
   - **`nativeGc none`** helps a little (1.91 → 1.72 ms, now dead-even with bash) — vindicating BR's point that
     a GC is pure tax for a no-op (SN's own docs recommend None GC for "short-running command-line
     applications"). But it does **not** reach C: the residual cost is runtime bootstrap, not the GC.
   - **`nativeMode release-size` / `release-full`** cannot help startup: SN's docs say release-fast *is* the
     startup mode and release-full is "for long-running applications where startup matters less." Confirmed:
     release-size was no faster (1.83 ms) and, for a no-op, produced no smaller binary.
3. **Binary size has a hard ~1.64 MiB floor too.** GC-none shed only ~2% (32 KB); release-size was actually
   ~4 KB *larger*. The 1.64 MiB is the linked-in SN runtime/stdlib — there is no user code to optimize away,
   so the runtime *is* the binary. SN is not "lean" for a trivial program on either axis (startup or size).
4. **The whole launcher spread is ~1.2 ms.** Against the measured wake latency (≈2 s baseline, ≈4 s, ≈30 s
   screen-locked), the launcher language is **3–4 orders of magnitude below the bottleneck.** Launcher choice
   is noise.

## Findings — the `tt`-tool target axis (JVM vs native)
The JVM and GraalVM rows answer a *different* question than the launcher one: not "which launcher for a hook"
but "which compilation target for a `tt` tool that pays startup on every call."
5. **The JVM cliff: ~131 ms program startup vs ~1–3 ms for every native option — a ~70–86× gap.** THIS is the
   tax that motivates native-compiling hot, short-lived `tt` tools. And it stacks: a real `tt` call is ~500 ms
   because `scala-cli run` adds its own build-check layer *above* the 131 ms JVM boot. Native-compiling removes
   both.
6. **Scala Native beats GraalVM native-image — on BOTH startup and size.** SN 1.85 ms / 1.64 MiB vs GraalVM
   3.05 ms / **12.25 MiB**. This is counterintuitive (GraalVM native-image is often assumed the gold standard),
   but for a lean, fast-starting CLI, **SN is the better native target.** GraalVM native-image's real advantage
   is *keeping JVM-ecosystem Java deps* (which SN cannot use), not being leanest or fastest to start. So the
   target choice is not "native vs JVM" but a three-way: JVM (deps + throughput, slow start) / SN (leanest
   start, no Java deps) / GraalVM native-image (fast-ish start + keeps Java deps, but fat). Feeds the new
   `scala-platform` skill.

## Design conclusion (empirical, replaces the asserted version in SM105)
**Keep `approval-wake` as the 3-line bash.** Not because bash is fast — it is 2× slower than C — but because
that ~0.9 ms is utter noise against the audio-path latency, and bash keeps the hook a reviewable one-screen
script with zero build step and no 1.64 MiB binary to maintain. This is the [[dependency-preference-cascade]]
and fit-to-task rule proven with numbers: reach for the compiled/native tool where *throughput* or *reviewable
logic* pays (`sound.sc`), not for a trivial latency-insensitive fork-and-exit hook.

**BR's synthesis (the moral, in his words):** *"we can never beat C … C is brittle … Scala is good."* The
experiment earns all three: C is unbeatable on raw startup (loader + libc, nothing above the metal) — but you
buy that ~1 ms with C's brittleness (memory-unsafety, UB, manual everything). For this class of task the speed
is **unspendable** (invisible under the audio path) while the brittleness is **real**. So the rule is not "C
wins" but **don't pay in brittleness for a speed you can't spend.** The genuine variable is safety-vs-brittleness;
raw speed is a red herring wherever it lands below the true bottleneck. (Blog 022 matter: "brittle-bash →
beautiful-Scala," extended with *brittle-C* on the same axis.)

**The real latency lever (untested here) is version (B):** link **libcanberra in-process** (C `-lcanberra`, or
Scala Native FFI) to eliminate the `canberra-gtk-play` fork+exec entirely. THAT could move wake latency; the
launcher language cannot. Left as follow-up — and note BR's screen-lock/sink-resume theory suggests even (B)
may be dominated by PulseAudio sink resume, so measure the sink-resume cost before investing in FFI.

## Caveats / threats to validity
- Measures the JVM harness's fork+exec overhead as a constant across all three (fair for *comparison*, but the
  absolute ms includes harness cost, not a pure kernel exec time).
- release-fast, not release-full; release-full would likely *not* change startup (it targets runtime perf).
- Single box, warm cache; no cold-cache or under-load conditions tested (those are the audio-lag conditions,
  separately logged).

## Future work (the rigorous version — deferred, and it is an ordeal)
This prestudy already burned three Scala Native native-compile minion runs (~28s + ~23s + ~23s, each a full
LLVM link/optimize/codegen) just to compare a *no-op*. A genuinely thorough performance study is a much bigger
ordeal, hence deferred:
- **Flamegraphs / `perf` / `strace`** of the actual startup path, to attribute SN's ~1.7 ms floor to specific
  init (segment load, runtime bootstrap, dynamic linking) rather than inferring "runtime init" from the gap.
- **Real workload, not a no-op** — the fair test for "lean and mean" is a launcher that does the launcher's
  real job (resolve a device, spawn and play a sound), where SN's compiled compute could actually matter, not an
  empty `main`.
- **Version (B): in-process libcanberra** — C `-lcanberra` and SN FFI, to measure eliminating the
  `canberra-gtk-play` fork+exec. This is the only lever that could move *wake* latency; the launcher language
  cannot. But measure the PulseAudio **sink-resume** cost first (BR's screen-lock theory) — if resume dominates,
  even (B) is moot.
- **Cold-cache and under-load conditions**, matching the real bing-lag scenarios rather than a warm idle box.
- Startup-latency statistics done properly (a tool like hyperfine, outlier handling), not a hand-rolled harness.
