# Issue 261115-graalvm-25-unsafe-warning: build the native tt on a maintained GraalVM (25) without the Unsafe warning noise

> status: open 2026-11-15 · labels: native, build, deps · summary: `deploy/buildnative.sc` is pinned to GraalVM
> Community 21.0.2, which gets no more updates. Move to the maintained GraalVM Community 25 line once a binary
> built on it no longer prints the JDK 25 `sun.misc.Unsafe` warnings on every `tt` call.

## Description

scala-cli's default GraalVM for `--native-image` moved to JDK 25 ("25 by default", `--graalvm-java-version`).
A `tt` binary built on GraalVM CE 25.0.2 printed four lines on every call, on stderr ahead of the tool's output:

```
WARNING: A terminally deprecated method in sun.misc.Unsafe has been called
WARNING: sun.misc.Unsafe::objectFieldOffset has been called by scala.runtime.LazyVals$ (file:.../tmp/tt-native)
WARNING: Please consider reporting this to the maintainers of class scala.runtime.LazyVals$
WARNING: sun.misc.Unsafe::objectFieldOffset will be removed in a future release
```

JDK 24+ warns about `sun.misc.Unsafe` memory access (JEP 498), and Scala 3 lazy vals (`scala.runtime.LazyVals$`)
still use it under Scala 3.9.0. Every agent reads that noise in every tool result.

As a stopgap, `deploy/buildnative.sc` pins `--graalvm-jvm-id graalvm-community:21.0.2` (2026-11-15). That build
is quiet, but GraalVM Community gets updates only on its newest line: in coursier's JVM index the 17 line ends
at 17.0.9 and the 21 line at 21.0.2 (January 2024), while 25 continues (25.0.x, then graal-25.1 to 25.5).
A frozen 21.0.2 means no further fixes, mostly relevant to the bundled TLS stack and CA certificates that
`tt web` and `tt forge` use. The 17 line is no alternative: it is frozen too, and `tools/` declares
`//> using jvm 21`.

## How to reproduce it

Remove the `--graalvm-jvm-id` pin from `deploy/buildnative.sc`, rebuild, run any `tt` verb (e.g. `tt chrono now`).

## Acceptance sketch

- `buildnative.sc` pins a GraalVM Community 25 release (an explicit id, never scala-cli's moving default).
- A `tt` call on the resulting binary prints no `sun.misc.Unsafe` warning lines.
- The parity step (the CLI suite through the candidate binary) passes, on CI's platforms too.
- `docs/native.md` names the GraalVM used, and the pin's comment in `buildnative.sc` is updated.

## Discussion

Candidate ways to silence it, none verified yet:
1. The JVM's own switch `--sun-misc-unsafe-memory-access=allow`. Open question: can a native image carry it as a
   built-in default, or does it apply only to the JVM running the builder (`-J...`)?
2. A Scala version whose lazy vals no longer use `sun.misc.Unsafe` (check the Scala 3 changelogs from 3.9 on).
   This removes the cause rather than the message.
3. Filter the lines in the `tools/tt` launcher. Last resort: it hides the symptom and costs a pass over stderr.

Until one of these works, keep the 21.0.2 pin: quiet output beats a newer build that floods every result.
