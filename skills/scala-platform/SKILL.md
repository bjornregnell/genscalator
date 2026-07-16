---
name: scala-platform
description: How to choose a Scala COMPILATION TARGET / platform — JVM vs Scala Native vs GraalVM native-image — for a tool or app. Trigger whenever startup latency, binary size, native performance, or deployment shape matters for a Scala program: deciding whether to native-compile a hot `tt` tool, weighing JVM startup against a native binary, or hitting a dependency/toolchain constraint of a native target. This is the WHICH-target decision (deployment axis); it is distinct from `scala-style` (HOW to write the code) and `contribute-tool` (HOW to add a tool to the toolbox). Grounded in the genscalator launcher/startup benchmark.
---

# Choosing a Scala compilation target

> **In one breath:** most Scala runs on the **JVM** — full ecosystem, best warmed-up throughput, but a heavy
> (~half-second) startup you pay *every invocation*. When that startup dominates (a small, short-lived,
> frequently-invoked CLI — the `tt`-tool shape), compile to a **native binary**: **Scala Native** (Scala→LLVM,
> the leanest startup) or **GraalVM native-image** (JVM-bytecode→native, keeps the JVM ecosystem). The choice
> is a **conscious, local tradeoff** — startup vs throughput vs portability vs dependency freedom — not a
> default to "native = better". Sibling skills: `scala-style` = HOW to write it; `contribute-tool` = HOW to add
> it; **this** = WHICH target to compile it to.

This skill exists because "rewrite it in a faster language/target" is a reflex that is often **wrong**: the
speed you would win is frequently *unspendable* (below the real bottleneck) while the cost you inherit
(brittleness, a build step, a lost ecosystem, a fragile toolchain) is *real*. Decide with data and fit-to-task.

## The three targets

| target | startup (no-op, this box) | binary | ecosystem / deps | best for |
|--------|---------------------------|--------|------------------|----------|
| **JVM** (default) | **~131 ms** program boot (**~500 ms** per `tt` call: JVM + scala-cli layer) | 230 KiB launcher, needs a JVM | **all** Java + Scala libs; JIT throughput | the default: rare or long-running invocations; anything needing Java deps; throughput after warmup |
| **Scala Native** | **~1.85 ms** (release-fast, None-GC) | **~1.64 MiB** floor | **no Java deps** (libs must be published for SN); partial `java.*` | hot, short-lived, frequently-invoked CLIs that are JDK-light and pure — the leanest, fastest-starting native |
| **GraalVM native-image** | **~3.05 ms** (AOT, JVM→native) | **~12.25 MiB** (fat) | **Java deps OK** with reflection config; no JIT | native startup while KEEPING JVM-ecosystem deps you can't drop |

(Numbers are the genscalator prestudy — a *no-op* micro-benchmark, enough to rank the targets, not a rigorous
profile; see `research/wr-data/approval-wake-launcher-startup-bench-2026-07-14.md`. For reference: C = ~0.76 ms /
15.8 KiB, bash = ~1.59 ms.)

**Surprise worth knowing: Scala Native beats GraalVM native-image on BOTH startup (~1.85 vs ~3.05 ms) AND size
(~1.64 vs ~12.25 MiB).** GraalVM native-image is often assumed the gold native standard, but for a lean
fast-starting CLI, SN wins. GraalVM's real edge is *keeping Java deps SN cannot use* — so choose it only when a
Java dependency forces your hand, not for raw leanness.

## The decision, in order
1. **Default to the JVM.** Simplest, full ecosystem, best throughput once warm. If the tool is invoked rarely,
   or runs long enough that ~0.5 s startup is noise, **stop here** — do not native-compile. Most `tt` tools
   are fine on the JVM.
2. **Does JVM startup actually dominate?** Only native-compile when the tool is **short-lived AND invoked
   often** so the ~0.5 s startup is the felt cost (the `tt`-tool shape: `files`, `text`, `chrono`, `find`).
   If yes:
   - **JDK-light and no Java deps** → **Scala Native.** Leanest startup (~1.7 ms, a ~300× win over JVM), and
     short-lived CLI is SN's own documented sweet spot (use **`nativeGc none`** — a no-freeing GC is ideal when
     the process allocates a bounded amount and exits; the OS reclaims on death).
   - **Blocked only by a SMALL `java.*` gap (not a real dependency)?** → **hand-roll the gap and KEEP Scala
     Native.** Do not escalate to Graal for a missing stdlib corner. `java.time` is the canonical case: SN
     0.5.12 has not ported it (`research/052`), yet an ISO-8601→epoch-ms conversion or a local wall clock is a
     small, bounded, testable slice — the hand-roll sweet spot (`scala-style` §1). **Worked examples:**
     `tools/statusline.scala`'s `clock` and `isoToEpochMs`; both exist *purely* to keep the hot native target
     compiling. **Test the hand-roll against the JDK as an independent oracle** (the test runs on the JVM and
     may use `java.time` freely — only the *tool* is constrained). **Cost to weigh honestly:** you own that
     code forever, so hand-roll only what is genuinely small; a fiddly/decades-deep domain (a full date-time
     library, a TZ database, crypto) is NOT this branch — that is the next one.
   - **Needs Java deps, reflection-heavy libs, or a JDK gap too big/risky to hand-roll** → **GraalVM
     native-image.** It AOT-compiles JVM bytecode, so the ecosystem comes along — at the cost of reflection
     configuration and a heavier build. **This is the right escape hatch when startup matters but the JDK gap
     is not worth owning**: ~3.05 ms startup (still ~165× better than the JVM) and full `java.*`, paid for in
     binary size (~12.25 MiB vs SN's ~1.64) and build time. Reach for it because a dependency *forces* it —
     never for leanness (SN wins that outright, see the table above).
3. **Throughput-bound, long-running compute?** Prefer the **JVM** (the JIT wins after warmup) or SN
   `release-full`. Native's advantage is *startup*, not steady-state compute.
4. **Trivial, latency-insensitive, fire-and-forget** (a hook that just execs something)? **Do not compile at
   all — use bash.** Even C's ~1 ms edge is invisible if the real work is downstream; a reviewable one-screen
   script beats a binary + build step. (This is why `approval-wake.sh` stays bash — proven, not asserted.)

## Constraints and gotchas (the reasons "native" is not free)
- **Scala Native cannot use Java dependencies.** Libraries must be published *for Scala Native*; `java.*`
  support is partial. Check every dep before committing. JDK-heavy code (`java.nio` tree walks, `ProcessBuilder`
  drivers) may not port unchanged.
  - **The gap bites LATE, which is what makes it expensive.** A `java.time` call compiles fine, passes every
    JVM test, and only breaks at the *native build* — far from the line that caused it. So the check belongs at
    **write** time, per file, in `scala-style` §1: *is this file a native target? then is this API ported?* The
    constraint is **per-file, not global** — `tools/statusline.scala` must avoid `java.time`; `tools/hangover.scala`
    (a JVM-only hook) uses it correctly. Duplicating a small helper across that boundary is deliberate, not sloppy.
- **GraalVM's fallback-image trap.** If native-image hits reflection it can silently emit a *fallback image*
  that is really just a JVM launcher — a fake "native" binary with JVM startup. **Always build with
  `--no-fallback`** so it errors instead of lying, and add reflection config where needed.
- **Both native targets need a C/LLVM toolchain** (clang for SN; a C compiler + a fetched GraalVM for
  native-image, which scala-cli auto-downloads via coursier) and have **slow builds** (native-image especially).
  That toolchain is a portability and CI cost.
- **Binary size.** Scala Native has a ~1.64 MiB *floor* for even a no-op (the linked-in runtime/stdlib);
  neither GC choice nor `release-size` shrinks it meaningfully. A native tool is an artifact to build, ship,
  and maintain — weigh that against the JVM's zero-artifact `scala-cli` run.
- **No JIT in native.** Steady-state hot loops can be *slower* than a warmed JVM; native trades peak throughput
  for instant startup.

## Build commands (per-call approved — not auto-granted)
This skill grants no tools: native builds are occasional and CPU-heavy, so keep them per-call approved.
- Scala Native: `//> using platform scala-native`, `//> using nativeMode release-fast`, `//> using nativeGc none`,
  then `scala-cli --power package <file> -o <bin> -f`.
- GraalVM native-image: `scala-cli --power package --native-image <file> -o <bin> -f --graalvm-args --no-fallback`.

## genscalator application
The `tt` toolbox pays JVM startup on **every** call (~0.5 s, most of a `tt files`/`tt chrono` wall-time). For
the **hot, JDK-light, pure** tools this is the prime candidate for a native build — a per-tool decision, not a
toolbox-wide switch, because the JDK-heavy tools (`reqt-vendored`, `ProcessBuilder` drivers) and any
Java-dep-using tools do not port for free. The prestudy that grounds all of the above:
`research/wr-data/approval-wake-launcher-startup-bench-2026-07-14.md` (bash vs C vs SN vs GraalVM vs JVM).
Rigorous profiling (flamegraphs, real workloads) is future work there.
