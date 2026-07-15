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
| C                          | ~0.74 ms       | 15.8 KiB             | no               |
| Rust                       | ~1.04 ms       | 362 KiB stripped / 12.6 MiB unstripped | no |
| Go                         | ~1.04 ms       | 1.31 MiB             | no               |
| bash                       | ~1.59 ms       | 125 B (a script)     | bash             |
| Scala Native (no-GC)       | ~1.85 ms       | 1.64 MiB             | no               |
| GraalVM native-image       | ~3.05 ms       | 12.25 MiB            | no               |
| python3 (interpreted)      | ~11.6 ms       | source only          | python3          |
| Node / JavaScript          | ~26 ms         | source only          | node             |
| Scala on the JVM           | ~142 ms        | 230 KiB launcher     | a JVM            |

(Scala Native was built `release-fast` with the garbage collector set to `none`, which its own documentation
recommends for short-running command-line programs that allocate a bounded amount and exit. GraalVM
native-image was built with `--no-fallback` and confirmed to be a genuine native image, not a JVM-launcher
fallback.)

## The findings

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

**3. Go beats Scala Native, and it explains *why*.** Go starts in ~1 ms, nearly as fast as C, *despite having a
garbage collector*, and it beats Scala Native's ~1.8 ms by almost 2x, even though both are compiled languages
with a GC and similarly-sized binaries (Go 1.3 MiB, Scala Native 1.6 MiB). So Scala Native's floor is not "the
GC" and not "being a compiled GC language" (Go disproves both); it is Scala Native's *own* runtime and standard
library bootstrap. And Rust lands right beside Go (~1 ms) in a lean 362 KiB binary (once stripped, versus a
misleading 12.6 MiB unstripped), giving C-class startup *with* memory safety: the answer to
this post's "C is brittle" aside. You can have the metal without the fragility.

## The other axis: compile time

Startup is only half the developer-experience story. The other half is how long you wait to *get* the binary
(the edit-compile-run loop):

| language | build time | startup |
|---|---|---|
| bash / python3 / Node | none | 1.7 / 11.6 / 26 ms |
| C | ~0.05 s | 0.8 ms |
| Go | ~0.04 s (warm) | ~1.0 ms |
| Rust | ~0.2 s | ~1.0 ms |
| Scala on the JVM | ~12 s | ~142 ms |
| Scala Native | ~23 s | ~1.8 ms |
| GraalVM native-image | ~40 s | ~3 ms |

Now the tradeoff is visible on two axes, and four regimes fall out:
- **Fast-both:** C, Rust, and Go (fast build, ~1 ms start). C is the ideal if you can live with its
  brittleness; Rust gives the same speed with memory safety; Go adds the fastest build of all (a 36 ms warm
  rebuild).
- **Zero build, moderate start:** the interpreters and bash, an instant loop with startup in the tens of ms.
- **Slow build, fast start:** Scala Native and GraalVM native-image. You pay a *20 to 40 second build* for a
  near-native startup: wonderful for something you ship once and run a million times, painful for a tool you
  rebuild every few minutes, where the build cost (not the runtime) dominates your day.
- **Medium build, slow start:** the JVM, worst on startup but with no native-compile step and a JIT that pays
  back on long-running throughput.

And one more thing worth staring at: **the interpreters beat the JVM on startup.** python3 (~12 ms) and Node
(~26 ms) start an order of magnitude faster than Scala on the JVM (~142 ms), even though the JVM is "compiled"
and they are not. "Compiled versus interpreted" does not predict startup; the runtime's fixed initialization
cost does, and the JVM's is large.

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

## Appendix: installing the languages on Ubuntu

**[SCAFFOLD, agent-drafted; BR to verify each command before publish, per the blog link/command rule.]**

Everything here was measured on an Ubuntu 24.04-class box, with: gcc 13.3, clang 18.1, Scala 3.8.4 / Scala
Native 0.5.12 / GraalVM CE 17 (all via scala-cli), OpenJDK 21, Python 3.12, Node 22. Prefer the distro packages
below; the vendor `curl | sh` installers are noted only as alternatives, since a distro package is easier to
audit and to keep updated.

Already on a stock Ubuntu: **bash** and usually **python3** (check with `bash --version` / `python3 --version`).

```bash
# C (gcc)
sudo apt install build-essential

# Python 3
sudo apt install python3

# Node.js (JavaScript) — distro version may lag; for a specific major use NodeSource or nvm
sudo apt install nodejs

# Go
sudo apt install golang-go

# Rust — for the newest toolchain use rustup (rustup.rs), a curl-to-shell installer
sudo apt install rustc cargo

# Java (the JVM)
sudo apt install openjdk-21-jdk
```

**Scala, Scala Native, and GraalVM native-image** all run through one tool, **scala-cli**:

- Install scala-cli (see the current installer at <https://scala-cli.virtuslab.org/install>; on Ubuntu it also
  ships as a `.deb`, or via Coursier: `cs install scala-cli`).
- **Scala Native** additionally needs the LLVM toolchain:
  ```bash
  sudo apt install clang libstdc++-12-dev
  ```
- **GraalVM native-image** needs no separate install: `scala-cli --power package --native-image …`
  auto-downloads a GraalVM distribution via Coursier on first use (it only needs a C toolchain, i.e.
  `build-essential` above).

The exact builds used in this post:

```bash
gcc -O2 noop.c -o noop-c
go build -o noop-go noop.go
rustc -O noop.rs -o noop-rs
scala-cli --power package noop-native.scala -o noop-native -f                             # Scala Native
scala-cli --power package --native-image noop-jvm.scala -o noop-graal -f --graalvm-args --no-fallback
scala-cli --power package noop-jvm.scala -o noop-jvm -f                                    # JVM bootstrap
```

Interpreted runs need no build: `python3 noop.py`, `node noop.js`, `bash noop.sh`.

