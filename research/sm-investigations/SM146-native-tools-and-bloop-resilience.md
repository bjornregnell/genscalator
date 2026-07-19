# SM146 — avoiding tool-lane hiccups: native tools + bloop resilience

**Status: INVESTIGATION / TRADEOFF (agent-drafted AFK 2026-07-18).** No build here — the native-compile
adoption, any new allowlist entry, and the `pkill` packaging are **BR-gated** (build + security calls). This
note scopes the three threads BR pinned and gives a recommendation to react to, not a decision taken.

## The failure this fixes (grounded)

`tools/tt:30` runs every tool as `scala-cli run "$TOOLS/$tool.scala" ...` → a **bloop** compile-server call.
The guardcheck `PreToolUse` hook is itself a tool (`guardcheck.scala`), so it **too** is a per-call `scala-cli`.
On 2026-07-18 one wedged `BloopServer` (its `/tmp/start-bloop*.sh` launcher had vanished: `sh: cannot open … No
such file`) froze the **entire** Bash/`tt` lane for hours — because *both* the tools and the gate that must
approve every Bash call route through the same daemon. Text-only turns stayed fast; anything touching a
subprocess hung. Fix that day: `pkill -9 -f BloopServer` + a cold start (8.5s cold → 0.6s warm). See the PB
2026-07-18 MORNING bullet and `wr-data/long-thinking-latency-...-2026-07-18.md` (`04f1e39`).

**The core structural point:** the compile daemon is a single point of failure sitting under *both* the tools
and the permission gate. Resilience = break that coupling. The three threads attack it at different depths.

## Thread (a) — native-compile the tools: Scala Native vs GraalVM native-image

The toolbox is ~35 single-file `scala-cli` scripts (`tools/*.scala`), deliberately **JDK-leaning** per
[[dependency-preference-cascade]] (JDK-first): `MiniJson` is hand-rolled (ujson de-dep'd, SM112), but several
tools lean hard on the JDK stdlib — `web.scala` (`java.net.http`), `serv.scala` (an HTTP server), `svg.scala` /
the image work (`javax.imageio`, `java.awt`), file walking (`java.nio`). That JDK-dependence is the hinge of
the choice:

| | **GraalVM native-image** | **Scala Native** |
|---|---|---|
| Input | JVM bytecode → AOT native binary | Scala → LLVM → native |
| JDK libs | **Full** (`java.net.http`, `javax.imageio`, `java.awt`, nio all work) | **Partial** — no AWT/ImageIO, HTTP needs a SN lib; big porting cost |
| Reflection/resources | needs `reflect-config`/`resource-config` (some tools use reflection-ish JSON) | limited |
| Startup | ~ms (no JVM, no bloop) | ~ms (no JVM, no bloop) |
| Build cost | heavy (minutes/binary), per-OS/arch | lighter, per-OS/arch, but requires LLVM toolchain |
| Binary size | large (10s of MB) | small |
| Risk to THIS toolbox | low compat risk, high build/config cost | **high compat risk** (JDK libs) |

**Lean:** **GraalVM native-image** is the realistic full-toolbox path — it preserves the JDK-first bet, whereas
Scala Native would force re-implementing JDK-provided pieces (`web`'s HTTP client, ImageIO) tool-by-tool. The
cost is build weight + reflection config, not portability of the *source*.

**But the highest-value target is not the whole toolbox — it is the guardcheck hook (see below).** Native-
compiling all 35 tools is a large program; native-compiling the ONE tool on the permission-critical path is
surgical and removes the worst failure mode first.

### The precompiled-hook angle (the surgical first step)

`guardcheck.scala` is the best native-compile candidate and should be first:
- it runs on **every** Bash call (highest call frequency; its cold/wedged cost is paid constantly);
- it is the **SPOF that froze the gate** — a native guardcheck means a wedged bloop can still hang the *tools*
  but the human can **still approve/deny**, so the session is steerable instead of fully frozen;
- it is **JDK-light** — reads hook JSON on stdin, runs regex `cmdChecks`/`msgChecks`, writes a JSON decision;
  no AWT/ImageIO/HTTP, so it native-images cleanly with minimal reflection config.

Dispatch model that keeps `scala-cli` as the fallback (graceful degradation, no big-bang migration):
`tt` (and the hook wiring) prefer `TOOLS/<tool>` **native binary if present**, else fall back to
`scala-cli run TOOLS/<tool>.scala`. Ship natives incrementally; nothing breaks if a binary is absent.
Portability caveat: native binaries are **per-OS/arch** → a CI build matrix, not a committed artifact (BR is on
linux today, but genscalator aims to be portable).

## Thread (b) — bloop health diagnosis (detect the wedge from outside)

A `tt doctor` / `bloop status` probe that reports WEDGED when: (1) a trivial compile exceeds a short timeout,
(2) no `BloopServer` process is found, or (3) the vanished-launcher symptom recurs (`/tmp/start-bloop*` gone).

**Chicken-and-egg constraint (important):** if this probe is itself a `scala-cli` tool, it **hangs exactly when
bloop is wedged** — useless in the case it exists for. Therefore (b) must **not depend on bloop**: it is a plain
**bash/JDK-direct** script (or native binary). This is a strong reason (b) and (c) live outside the `.scala`
toolbox, or are among the first natives.

## Thread (c) — `tt bloop restart` (package the recovery)

Wrap `pkill -9 -f BloopServer` + a cold-start warm into one command, fired when (b) trips. Same non-bloop-
dependence constraint as (b) → a bash script or native. Killing bloop is **safe/reversible** (it re-spawns on
the next compile). Security note: `pkill -9 -f BloopServer` is a **targeted** kill (not a blanket `pkill`), but
it is still a destructive-command shape — whether the *agent* may run it unattended is a **security call for BR**
([[never-blanket-allow-destructive-commands]]); the safe default is a human-run one-liner the tool prints.

## Recommendation (for BR to react to)

1. **First and cheapest: native-image `guardcheck` only**, with `scala-cli` fallback in the launcher. Biggest
   resilience win (keeps the gate alive under a wedge) for the least porting risk.
2. **Add a bloop-independent `tt doctor` + restart recipe** (bash or native) — detection + the one-liner.
3. **Defer full-toolbox native-image** to a CI matrix program; adopt per-tool where cold-start cost bites.
4. **Scala Native: not recommended** for this toolbox while it stays JDK-first (AWT/ImageIO/HTTP porting cost).

## Addendum 2026-07-19 — T4 graal-for-all: box inventory + executable plan (agent-drafted AFK, no builds run)

T4 (P0, ✅DECIDED graal-for-all for alpha) got its first real datum tonight via the SM170 cold-claude
experiment (PB SM146 ADDENDUM + ADDENDUM-2): GraalVM CE **17.0.9** aborted on `scala.Enumeration` reflection,
then emitted a **fallback image that EXPIRES** (JVM-launching stub carrying build-time temp classpaths — it
died at the next fresh prompt). Standing rule from that datum: **always `--no-fallback`** so a stub fails the
build loud instead of shipping.

### Inventory (read-only, this box, 2026-07-19 ~23:3x)

- **A modern GraalVM is ALREADY cached — no download needed:** coursier arc cache holds GraalVM CE
  distributions for **jdk-17.0.9, jdk-21.0.2, and jdk-25.0.1** (under
  `~/.cache/coursier/arc/https/github.com/graalvm/graalvm-ce-builds/`). The SM146-addendum lesson
  "upgrade GraalVM FIRST before config archaeology" is satisfiable offline: pin scala-cli's
  `--native-image` build to the **25.0.1** distribution instead of whatever default resolved 17.0.9.
- **System JDKs (sdkman):** Temurin 11/17/21/24/25; `current -> 25.0.2-tem`. Plain JVM assembly path healthy.
- **AWT surface of the toolbox is ONE file:** a fresh grep over `tools/` finds `java.awt` only in
  `tools/reqt-vendored/01-Settings.scala:24` (`java.awt.Color` constants for reqt syntax colouring).
  **Correction to this note's Thread (a) table premise:** the 07-18 text says `svg.scala`/image work uses
  `javax.imageio`/`java.awt` — a fresh grep finds **no** `javax.imageio`/`BufferedImage` hits in `tools/`
  at all (svg.scala evidently emits SVG text). So the AWT boss fight is nearly empty: de-AWT
  `01-Settings.scala` by replacing `java.awt.Color` with a tiny RGB case class (the colours are constants),
  and the whole toolbox is headless-clean.

### Plan (order of attack, each step BR-gated at build time)

1. **Pin the GraalVM:** build with the cached CE 25.0.1 (JDK-25-era native-image auto-handles the
   Enumeration/ScalaFeature reflection that killed the 17.0.9 attempt). Diagnose any residue with
   `--no-fallback -H:+ReportExceptionStackTraces`.
2. **guardcheck first** (this note's standing recommendation 1): the gate must survive a bloop wedge;
   fallback to `scala-cli` stays in the launcher.
3. **Hot-loop tools next:** `text`, `files`, `chrono`, `mode`, `statusline`, `git` — the per-turn and
   per-commit path where cold-start cost bites hardest.
4. **De-AWT `reqt-vendored/01-Settings.scala`** (RGB case class), unblocking `parsereqt`/`prd` for the
   same pipeline instead of parking them as "the AWT tools".
5. **Dispatcher question (BR):** the parked single-dispatcher decision ([[genscalator-toolbox-single-dispatcher]],
   one `@main` + tools as pure fns) changes T4's economics — ONE native image for the whole toolbox vs ~32
   separate binaries (build time, disk, upgrade story). T4 is a natural moment to unpark or re-park it
   deliberately; flagged, not pre-decided.
6. **Verification per binary:** the tool's own suite assertions against the NATIVE binary (not the scala-cli
   form), a `file`-check that the artifact is a real ELF (the SM170 stub shipped precisely because nobody
   `file`-checked it), and never installing any artifact built without `--no-fallback`.

### AFK bound tonight

Inventory and plan only — no native-image builds were run (long, resource-heavy, and the build tool-lane is
guard-gated; also per this note's own header: adoption is BR-gated).

## Ties
[[blixten-box-flaky]] (commit+push every unit; the box is fragile) · [[dependency-preference-cascade]] (JDK-first
is what tilts a→GraalVM) · SM147 (a safe `tt rm`/`tt move` is another toolbox-self-sufficiency step) · the
`SECURITY-MODEL.md` guard asymmetry (a frozen hook = a frozen gate: reliability of the gate is a security
property, since the human loses the ability to deny). Source finding: `04f1e39`.
