# tt-graalify: the native-image `tt` (status + recipe)

> **What this is.** The working notes for compiling the whole `tt` toolbox into ONE
> GraalVM native-image binary — why, how, what is proven, and what remains before
> `gs native` (the consent-gated provisioning DWIM, PRD SM112) can ship it to users.
> Status as of 2026-07-23 evening: **built, parity-proven and DEFAULT-ON on linux-x64
> in this repo's launcher** (`TT_NATIVE=0` opts out; a checkout without a built binary
> silently uses scala-cli as before). Plugin users get the new launcher at the next
> plugin release — the 0.9.1 cache still ships the pre-native `tt`.

## Why

* **Startup**: measured 6-9 ms per tool call vs ~0.5 s warm JVM (60-80x); the golden
  CLI suite drops from 118 s to 3.5 s. Sub-agents issuing dozens of small `tt` calls
  feel this most.
* **No build server**: a native binary never touches bloop/scala-cli at run time —
  the whole class of daemon-wedge and compile-contention failures disappears (see
  wr-data: the 123-phantom-failure episode, the 10 GB bloop specimens).
* **Windows/alpha**: Graal keeps the full JDK surface (java.time, java.net.http...),
  unlike Scala Native — the decided direction for alpha (PB 2026-07-19: graal-for-all).

## The proven recipe (linux-x64, 2026-07-23)

```
scala-cli --power package --native-image <repo>/tools \
  --main-class dispatchTypedTools -o <repo>/tmp/tt-native \
  -- --no-fallback --enable-url-protocols=https,http -J-Xmx6g
```

* Entry point: `tools/dispatch.scala` (the single dispatcher; contract `tt-native <tool> <args...>`).
* Measured: image generation 1 m 39 s, peak RSS 3.3 GB (cap the heap; check free mem
  first — 6 GB floor), binary 39.5 MB (19.3 code + 18.1 heap). GraalVM CE 17.0.9 via
  scala-cli's own fetch; `--no-fallback` is load-bearing (a fallback image would fake
  the win). 2,438 types auto-registered for reflection by ScalaFeature; no manual
  reflection config was needed.
* **Parity evidence**: the CLI-contract suite run with BOTH
  `--java-prop tt.tools=<repo>/tools` and `--java-prop tt.native.bin=<repo>/tmp/tt-native`
  → 317 tests / 13 suites / 0 failures (2026-07-23; CliSuite 163/163 through the binary).
  Re-run this after EVERY rebuild — it is the golden identical-behaviour net.

## Using it today (DEFAULT-ON since 2026-07-23; `TT_NATIVE=0` opts out)

```
tt <tool> <args...>              # native when fresh, scala-cli otherwise
TT_NATIVE=0 tt <tool> <args...>  # force the JVM path
```

The launcher (`tools/tt`) prefers the binary when `TT_NATIVE` is unset or `1`, the binary exists
(default `tmp/tt-native`, override `TT_NATIVE_BIN`), and **no `tools/**.scala` is newer
than it**. Fallback behaviour: a missing binary is SILENT (not graalified yet = the
pre-native default experience); a STALE binary prints a one-line stderr note naming the
refresh command. Live-verified through
the launcher 2026-07-23 (BR): `time TT_NATIVE=1 tt chrono now` → 0.032 s real end-to-end
(tool 0.006 s; the bash launcher itself is now the dominant ~26 ms). Staleness = source
mtime newer than binary; conservative by design (any edit disarms the fast path until a
rebuild).

## What remains toward default-on and `gs native` (SM112)

1. **Default-flip: DONE 2026-07-23** (BR's "flip it"): native-when-fresh is the default,
   `TT_NATIVE=0` the opt-out.
2. **Rebuild ritual: `deploy/buildnative.sc`** — build to `tmp/tt-native.next`, run the
   full suite THROUGH the candidate (parity mode), atomic swap only on green; build
   failure changes nothing, parity failure keeps the candidate for inspection and the
   live binary untouched. Free-memory floor 6 GB. Run BR-present from the repo root:
   `scala-cli run deploy/buildnative.sc`. *(Script written 2026-07-23; first proving
   run pending — until it has gone green once, treat the ritual as unproven.)*
3. **Platform matrix**: macOS + Windows binaries (CI build matrix — the alpha-tester
   long pole named in the SM146 distance report).
4. **`gs native` DWIM** (PRD SM112): detect toolchain (gcc, free mem), consent-gated
   build/install, never forcing native on anyone; suggests, not surprises.
5. **Distribution question**: ship binaries per release (forge release assets — SM196
   release-all would carry them) vs build-on-box. Undecided.

*Ties: SM146 (native = no-bloop endgame), SM112/PRD provisioning, SM196, the scala-platform
skill (JVM vs SN vs Graal decision guide), CliSuite parity mode (tools/test/cli.test.scala).*
