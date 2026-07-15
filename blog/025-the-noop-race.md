# The noop race: when a Scala tool should leave the JVM

> **Status: SCAFFOLD 2026-07-14 (agent-drafted from a live benchmarking session; BR to revoice before publish).**
> A data-driven look at Scala startup time across the JVM, Scala Native, and GraalVM native-image, with one
> counterintuitive result for performance-minded developers.
> **Audience:** performance-minded Scala and JVM developers; anyone choosing a compilation target for a
> command-line tool; people who assume GraalVM native-image is always the fastest native option.
> **[SCAFFOLD, agent-drafted, grounded in `research/wr-data/approval-wake-launcher-startup-bench-2026-07-14.md`.
> A no-op prestudy, not a rigorous profile. BR revoices before publish.]**
> **BR considering (2026-07-15): make this a SHORT STAND-ALONE, non-genscalator-specific post.** The
> SN-beats-GraalVM result is general perf-dev interest; if standalone, trim the genscalator-toolbox application
> (the `tt` JVM-startup tax) to a brief aside or a link, and let "The noop race" stand as a general Scala
> startup piece. Fits the punchier title.

## The punchline

For a small, fast-starting command-line tool, **Scala Native beats GraalVM native-image** on *both* startup time
and binary size, even though GraalVM native-image is usually assumed the gold standard for native JVM binaries.
In our no-op measurements: Scala Native started in about **1.9 ms** from a **1.64 MiB** binary; GraalVM
native-image took about **3 ms** from a **12.25 MiB** one. GraalVM native-image's real advantage is not raw
leanness; it is that it can keep the Java dependencies Scala Native cannot use.

## Why we were measuring startup at all

genscalator (our own tooling project) runs its command-line tools on the JVM through `scala-cli`. Every
invocation of a tiny tool like "list these files" or "print the time" pays the JVM's startup cost, and it adds
up: those calls take roughly half a second each, most of which is not the work, it is the virtual machine waking
up. So the question arose: for the hot, frequently-invoked tools, should we compile to a native binary and skip
the JVM startup entirely? And if so, which native route?

We ran a quick prestudy: a no-op program (a `main` that does nothing and exits) built five ways, and timed 200
to 300 startups of each. A no-op isolates pure startup, with no real work to confound it. It is a prestudy, not
a rigorous profile (that would want flamegraphs and real workloads; see the end).

## The numbers

Median startup of a no-op, on one Linux box:

| target                     | median startup | binary size          | needs a runtime? |
|----------------------------|----------------|----------------------|------------------|
| C                          | ~0.76 ms       | 15.8 KiB             | no               |
| bash                       | ~1.59 ms       | 125 B (a script)     | bash             |
| Scala Native (no-GC)       | ~1.85 ms       | 1.64 MiB             | no               |
| GraalVM native-image       | ~3.05 ms       | 12.25 MiB            | no               |
| Scala on the JVM           | ~131 ms        | 230 KiB launcher     | a JVM            |

(Scala Native was built `release-fast` with the garbage collector set to `none`, which its own documentation
recommends for short-running command-line programs that allocate a bounded amount and exit. GraalVM
native-image was built with `--no-fallback` and confirmed to be a genuine native image, not a JVM-launcher
fallback.)

## The two findings

**1. The JVM startup cliff.** Plain Scala on the JVM starts in about **131 ms**; every native option starts in
**1 to 3 ms**. That is a 70 to 86 times gap, and it is a fixed tax paid on *every* invocation. For a tool run
once, 131 ms is invisible. For a tool a script calls a thousand times in a loop, it is two minutes of pure
virtual-machine warmup doing no work. (In daily use our tools pay closer to half a second, because the runner
adds a build-check layer on top of the JVM boot; packaging removes that layer, going native removes the JVM boot
as well.) This cliff is the whole reason to consider a native target.

**2. Scala Native beats GraalVM native-image.** This is the surprise. Both are native, both start in
milliseconds, but Scala Native is faster (1.85 versus 3.05 ms) *and* dramatically smaller (1.64 versus 12.25
MiB). The common assumption that GraalVM native-image is the fastest way to get a native JVM-language binary
does not hold here. What GraalVM native-image buys you instead is compatibility: it compiles ordinary JVM
bytecode, so your existing Java and Scala libraries come along (with some reflection configuration). Scala
Native cannot use Java dependencies at all; its libraries must be published for Scala Native specifically. So
the choice is not "which is faster" but "do you need the Java ecosystem":

- **Leanest, fastest start, pure or Scala-Native-published deps only:** Scala Native.
- **Fast-ish start but you must keep a Java dependency:** GraalVM native-image (and accept the fatter binary).
- **Rare or long-running calls, or you want peak warmed-up throughput:** just stay on the JVM. Native's win is
  startup, not steady-state compute (there is no just-in-time compiler in a native image).

## The so-what: fit-to-task, with a number on it

The lesson is not "native is better." Most tools should stay on the JVM: it is the simplest path and it carries
the whole ecosystem. Native compilation earns its place only where the startup tax is actually felt, a small,
short-lived, frequently-invoked tool, and even then it costs you a build step, a toolchain (a C or LLVM
compiler), a binary artifact to ship, and, for Scala Native, the loss of Java dependencies. Reach for it where
the millisecond shows up in the answer, and nowhere else. In our own project this points at native-compiling a
handful of hot, dependency-light tools, one decision at a time, not flipping a switch for the whole toolbox.

## Honest limits

This is a prestudy on a no-op, on one machine, warm cache. A rigorous version would profile the startup path
(flamegraphs, `perf`), benchmark tools doing their real work rather than nothing, and test cold-cache and
under-load conditions. The rankings are robust enough to guide the decision; the exact milliseconds are not
gospel. Full method and raw numbers are in the linked research note.

## Further Reading

[TODO, verify each link resolves and is on-topic before shipping, per the blog link rule:]
- Scala Native, Garbage Collector settings (the None GC recommendation for short-running CLIs):
  https://scala-native.org/en/stable/user/runtime.html
- Scala Native build modes (release-fast, release-size, release-full):
  https://scala-native.org/en/stable/user/sbt.html
- scala-cli, packaging as GraalVM native images:
  https://scala-cli.virtuslab.org/docs/cookbooks/package/native-images/
- GraalVM native-image reference (fallback images, reflection configuration)
- The genscalator research note grounding this post (internal): `research/wr-data/approval-wake-launcher-startup-bench-2026-07-14.md`
