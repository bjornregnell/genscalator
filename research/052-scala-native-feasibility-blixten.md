# 052 — Scala Native feasibility probe on blixten (SM112)

*2026-07-15, box: blixten. Author: agent, BR present (OOM-prone box → findings committed promptly). Feeds
[[051-gs-native-tool-selection-and-consent]]. Toolchain found already present: **clang 18.1.3**, **scala-cli
1.15.0**, **Scala Native 0.5.12** (auto-fetched). No install was needed — so the consent/install UX did not fire.*

## Method

`scala-cli --power package --native <tool>.scala -o <bin> -f` (debug mode, to keep memory light on blixten), three
targets of rising `java.*` surface: a **noop**, **`typo`** (pure char logic), **`statusline`** (the hot target, now
pure-JDK after the ujson de-dep). Run each resulting binary to confirm it executes.

## Results

| target | links? | runs? | binary | build (debug) | note |
|---|---|---|---|---|---|
| **noop** | ✅ | ✅ (exit 0) | **1.64 MiB** | ~17.7 s (release-fast) | matches the blog-025 SN noop figure exactly |
| **`typo`** | ✅ | ✅ (`classify teh the` → `transposition`, = JVM) | — | ~14 s | **ports AS-IS**, zero code changes |
| **`statusline`** | ⛔ | — | — | failed at link (~3.4 s) | **8 unreachable symbols, ALL `java.time.*`** |

`statusline`'s failure is narrow and precise: `java.time.{ZoneId, Instant, ZonedDateTime, LocalTime,
format.DateTimeFormatter}` — **every one traced to a single method, `clock()`** (`statusline.scala:91-92`, the
HH:MM:SS wall clock). SN 0.5.12 has **not ported `java.time`**. Everything else linked fine: the `java.nio.file`
mode-file read, `scala.io.Source`, and the hand-rolled `MiniJson`. So statusline is **one function away** from porting.

(Side note: the statusline link log also showed `java.nio.charset.spi.CharsetProvider → NoProviders` as *info*, not
an error — worth watching when porting tools that decode Latin-1/UTF-8, a possible second gap after `java.time`.
Every SN build here also emits a harmless `ld` "executable stack / missing .note.GNU-stack" warning.)

## The finding — portability is gated at LINK time by `java.*`, not just by declared deps

This **refines the `research/051` derived-default**: reading a tool's `//> using dep` (pure JDK → SN candidate) is
**necessary but not sufficient**. A pure-JDK tool can still fail to link if it touches an **unported `java.*` API** —
here `java.time`. The gate is *reachability at link time*, discovered only by attempting the SN compile.

Good news: the SN linker error is **precise and machine-readable** — it names the exact unreachable symbols AND the
methods that reference them. So `gs native` can parse it and act, rather than just failing.

## Implications for `gs native`

1. **Add a link-probe step.** Derive the candidate target from deps (051), then *attempt* the SN compile. On
   unreachable-symbol errors, **classify**: `java.time` → a known, small port; anything else → investigate or fall
   back. Then **fall back gracefully** (GraalVM native-image keeps the full JDK, or stay JVM) for that tool.
2. **`java.time` is a known SN gap → a small port pattern.** For `statusline`'s clock, two SN-compatible routes:
   (a) POSIX `localtime`/`strftime` via `scala.scalanative.posix.time` (correct local tz), or (b) pure-arithmetic
   UTC from epoch-ms (trivial, but UTC-only). This is the kind of per-tool port `gs native` would either apply or
   flag. It also argues for keeping time-formatting behind a thin seam so a native build can swap the impl.
3. **Consent UX confirmed cheap on a ready box.** clang was already present, so no install prompt fired — the
   "detect → suggest install (+ link the SN homepage per-platform instructions)" path only triggers when the
   toolchain is missing. The README's lean-prereq promise holds: a user with clang needs nothing more.
4. **Startup win not re-measured** — blog 025 already established it (~1.9 ms SN vs the ~0.5 s JVM `tt` tax); this
   probe was about *portability*, and it shows the win is reachable for the hot tools once the `java.time` seam is
   handled (`typo` already reachable today; `statusline` after a one-function port).

**Next (BR-gated):** port `statusline.clock()` off `java.time` (posix or arithmetic behind a seam), re-compile to
confirm the full hot tool goes native, then wire the launcher's stateless per-call dispatch (051 §7). Artifacts of
this probe live in the session scratchpad, not the repo.
