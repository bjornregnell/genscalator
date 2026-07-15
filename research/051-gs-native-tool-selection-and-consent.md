# 051 — `gs native`: per-tool target selection + install-consent UX (design note)

*SM112. Status: DESIGN (no build; BR-gated — installs packages + runs native builds). Author: agent, safe-solo
2026-07-15. Grounds: `blog/025-the-noop-race.md`, `skills/scala-platform/SKILL.md`,
`research/wr-data/approval-wake-launcher-startup-bench-2026-07-14.md`, the per-tool deps in `tools/*.scala`. Ties
[[050-genscalator-settings-configureallthethings]] (the file that would STORE the tool-selection), the README lean-prereq promise.*

> **BR's seed:** a `gs native` do-what-i-mean command that (a) **detects** the toolchain the user already has, (b)
> **installs** (with consent) only the missing native prereqs, then (c) **native-compiles the tt tools that make
> sense** — Scala Native for the hot dependency-light tools, GraalVM native-image when a Java dependency forces it.

This note does **not** re-derive the target-selection theory — that lives in the `scala-platform` skill (JVM vs SN
vs GraalVM, the decision-in-order, the constraints). It applies that theory to the **actual toolbox** and designs
the two genuinely new parts: the **per-tool classification** and the **detect → consent → build** UX.

## 1. What `gs native` is

The product application of the noop race (blog 025). Today the JVM `tt` launcher pays **~0.5 s startup per call**
(025: JVM ~131 ms + the scala-cli layer). For a tool called constantly, that tax is the felt cost. `gs native`
turns the per-tool "should this leave the JVM?" decision (scala-platform) into **one gated action** that provisions
the toolchain and compiles the tools that qualify — leaving everything else on the JVM. It is **per-tool, not a
toolbox switch** (scala-platform §"genscalator application").

## 2. Per-tool classification (the core deliverable)

Grounded in each tool's actual `//> using dep` (pure JDK = no dep line) and its invocation frequency. Three regimes,
straight from 025 lines 82-85 / the skill's decision table.

| Tool(s) | Dep | Frequency | Target | Why |
|---|---|---|---|---|
| **statusline** ⭐ | ujson | **every prompt render** (hottest) | **Scala Native** (see §3 caveat) | the single biggest win — runs constantly; the ~0.5 s tax is paid on every refresh |
| **text, files, find** | pure JDK | high (interactive grep loop) | **Scala Native** | hot + JDK-light + pure = SN's exact sweet spot; a ~300× startup win |
| **mode** | pure JDK | high (read for the mode line; written as MO shifts) | **Scala Native** | pure, tiny, frequently read |
| **chrono, typo, doc, guardcheck** | pure / ujson | medium-high | **Scala Native** (ujson caveat for guardcheck) | small, pure-ish, called often enough to feel startup |
| **git, update, verify** | os-lib + `os.proc` | medium (commit/test loops) | **GraalVM** *if compiled at all* | os-lib is cross-published, but they **spawn subprocesses** — process APIs are SN's weak spot (skill "ProcessBuilder drivers may not port"); Graal keeps the full JVM process API. Marginal frequency — may not be worth compiling. |
| **gvdot** | os-lib + spawns `dot` | low | **stay JVM** | rare + spawns an external process |
| **web, forge** | requests (+ujson, os-lib) | rare (network) | **stay JVM** | rare invocation → startup not felt; requests is JVM-oriented, SN networking is dicey |
| **ssg, serv** | (serv: os-lib) | batch / long-running | **stay JVM** | serv is a long-running server (startup irrelevant); ssg is a batch render |
| **svg, ascii, log, md-fmt, htmltext, parsereqt, newtool, harden, wr, gitinfo, prd** | mixed | low-medium, not latency-critical | **stay JVM** | the default (scala-platform step 1): startup is noise relative to how rarely / how much work they do |

**The high-value core is small and clear:** `statusline` + the grep family (`text`/`files`/`find`) + `mode`. Those
four or five tools capture almost all of the felt startup tax. Everything else is marginal — do NOT native-compile
by reflex (the skill's whole point: the speed you'd win is often unspendable).

## 3. The `statusline` de-dep insight (highest-value enabler)

`statusline` is the hottest tool but carries a **ujson** dep (it parses Claude Code's statusline stdin JSON). Two
questions that gate its SN build, and a clean resolution:

- **Is ujson published for Scala Native at the pinned version?** The lihaoyi libraries are *generally* cross-published
  for SN, but this is a **per-artifact, per-version fact I must not assert** — VERIFY that `ujson:4.4.3` (and any
  transitive) resolves for `scala-native` before relying on it.
- **Better: drop the dep.** `statusline` reads only a **handful of fields** from the CC JSON (context percentage,
  cost, effort, ...). A **tiny hand-rolled field extractor** (scala-style §1: hand-roll the small, bounded, testable
  thing) would remove ujson entirely, making `statusline` **pure JDK** → the cleanest possible SN target, and
  removing the whole "does ujson-native resolve?" risk for the single most valuable tool. **Recommend investigating
  the de-dep** as the enabling first step, paired-test against the current ujson parse to prove equivalence.

Same logic applies to `guardcheck`'s ujson use (smaller stakes — lower frequency).

## 4. Dependency SN-availability — a required pre-check, not an assumption

Before compiling any SN target, `gs native` must **verify each dep resolves for `scala-native`** (and flag the
process-spawning tools regardless). This is exactly the skill's "check every dep before committing" constraint,
made a step. Where a dep won't port: fall back to GraalVM (keeps the dep) or de-dep (§3) or stay JVM. Never emit a
silently-broken SN build.

## 5. Toolchain detection

The README promises a **lean prereq** — `scala-cli` + a JDK only — so `gs native` must **not assume** clang/LLVM/gcc.
Detect, per 025 lines 190-195 + the skill's constraints:

- **Scala Native** needs the **LLVM toolchain** (`clang`, `libstdc++`; with `nativeGc none` we avoid needing a GC lib).
  Detect: `clang --version`.
- **GraalVM native-image** needs **only a C toolchain** (`gcc`/`clang`); scala-cli **auto-downloads** the GraalVM
  distribution via Coursier on first `--power package --native-image`. Detect: a C compiler + `scala-cli --power`.
- Report a clear matrix: for each target, present / missing, and exactly which package supplies the gap.

## 6. Install-consent UX (BR: never force clang/gcc)

The gate. Principle: **opt-in, shown, with a clean decline path** — same discipline as `gs allow` / settings edits.

1. **Detect** and show what's present vs missing (§5).
2. If a prereq is missing, **show the exact install command** for the user's platform (`sudo apt install clang`,
   `brew install llvm`, ...) and **ask** — never run a package install silently or without consent.
3. **Decline path:** if the user declines (or has no toolchain and doesn't want one), `gs native` **falls back to
   the JVM** for those tools and says so. The toolbox keeps working exactly as today; native is purely additive.
4. Only after consent: run the (CPU-heavy, ~20-40 s each — 025 lines 107-109) native builds, per-tool, showing progress.

This keeps the README's lean-prereq promise true: a user who never runs `gs native` (or declines the toolchain)
needs nothing beyond scala-cli + a JDK.

## 7. Coexistence with the JVM `tt` launcher

The native binaries must be **additive and optional** — nothing breaks if they're absent. Design: `gs native` writes
each binary to a known cache path (e.g. `~/.cache/genscalator/native/<tool>`), and the **`tt` launcher dispatches to
the native binary if it exists, else falls back** to `scala-cli run tools/<tool>.scala`. Consequences:

- A fresh checkout with no native builds behaves exactly as today (JVM).
- `gs native` is re-runnable and incremental (rebuild only changed / requested tools).
- A stale binary is a risk (tool source changed, binary didn't) — so the launcher should prefer the binary only when
  it is **newer than the source**, else rebuild-or-fall-back. (Detail for the build task.)
- The tool-selection itself (which tools the user opted to compile) is exactly the kind of knob
  [[050-genscalator-settings-configureallthethings]] would persist — `native: { statusline: on, text: on, ... }`.

## 8. Recommendation + open questions

**Recommended first slice:** investigate the **statusline de-dep** (§3), then SN-compile the high-value core —
**statusline + text/files/find + mode** — behind the detect/consent gate, with the launcher dispatching to a native
binary when present. Leave the process-spawning tools (git/update/verify) on the JVM initially (marginal win, SN
process-API risk); revisit with GraalVM only if their startup proves felt. Do **not** compile the long tail.

**Open (BR's call / needs verification, not agent's to assume):**
1. **Verify** ujson-SN (and os-lib-SN) resolution before relying on it — or commit to the statusline de-dep instead.
2. **Scope of the first build** — just statusline, or the whole hot core, in slice one?
3. **Cache path + launcher dispatch** — is `~/.cache/genscalator/native/` the right home, and is newer-than-source
   the right staleness rule?
4. **CI / portability** — do we build natives in CI (which OS/arch matrix?), or is `gs native` purely a local,
   user-run provisioning step? (The toolchain is a real CI cost — skill §constraints.)

**Not in scope here:** the actual build + the staleness-dispatch in the launcher (own-tooling, BR-gated), and a
rigorous startup profile (future work in the wr-data prestudy note).
