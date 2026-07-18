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

## Ties
[[blixten-box-flaky]] (commit+push every unit; the box is fragile) · [[dependency-preference-cascade]] (JDK-first
is what tilts a→GraalVM) · SM147 (a safe `tt rm`/`tt move` is another toolbox-self-sufficiency step) · the
`SECURITY-MODEL.md` guard asymmetry (a frozen hook = a frozen gate: reliability of the gate is a security
property, since the human loses the ability to deny). Source finding: `04f1e39`.
