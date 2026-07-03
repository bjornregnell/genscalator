# Proposal: one sole `@main` dispatcher + typed streaming tools + a native `tt` binary

Status: **PROPOSED, not started** — needs a joint BR+agent decision on sequencing before any code moves.
Raised by BR 2026-07-03 during the object-scoping refactor (which is the stepping stone below).

## Where we are now (after the 2026-07-03 refactor)
Each `tools/<name>.scala` is `object <Name> { def dispatch(args: String*) }` plus a thin top-level
`@main def <descriptive>(args) = <Name>.dispatch(args*)`. `tt` (a bash launcher) runs ONE file per call via
`scala-cli run tools/<name>.scala`. Helpers are scoped in the object, so the whole tree now compiles as one
target without top-level-name collisions (the bug that prompted this).

## The proposal (BR)
1. **One sole `@main`** — a single Scala dispatcher that routes `tt <tool> <args>` to the right tool, instead
   of N per-file `@main`s. Tools become non-`@main` typed methods: `def goodToolName(typedArg): TypedOutput`.
2. **Typed core / thin adapter** — each tool's logic is a pure typed function; the dispatcher owns arg
   parsing, IO, and the verdict line. (Already true for `htmltext`: `stripHtml(html): String` + a CLI shell.)
3. **Stream through `Iterator`/`Iterable`** where it pays — `def tool(lines: Iterator[String]):
   Iterator[String]` so tools compose in-process (pipe tool→tool, no shell) and stay memory-bounded.
4. **Replace the `tt` bash script with Scala** — "everything tt bash does, a Scala program can do better."

## Agent assessment (tradeoffs — the honest version)

**1 + 2 (single dispatcher, typed tools): yes.** Right end-state; the objects from the stepping-stone are
exactly its shape (dispatcher calls `Box.dispatch`, `Forge.dispatch`, …). Kills the top-level-`@main`
collision class entirely (one `@main`). Makes each tool a testable pure function + a Safe-mode-trackable
boundary (aligns with skills/scala-style §3). Per-tool arg parsing stays (box has models/df/gpu/pull; forge
has 4 verbs) — just organized as object methods behind the dispatcher.

**3 (streaming): yes, selectively.** `Iterator[String]` cores win for large-input, composable tools
(`log`, `text`/grepr) — bounded memory + in-process piping. But network tools (`web`, `forge`) and
whole-text tools (regex spanning lines, LaTeX-log analysis) don't fit a line iterator; forcing it adds
ceremony for no gain. Adopt where it pays; not a universal contract.

**4 (kill the bash): yes — but the replacement is a COMPILED/NATIVE `tt`, not a `scala-cli run`.** Two
reasons the bash exists: (a) it's a bootstrap/latency shim; (b) it's the allowlist anchor — `Bash(tt box *)`
is ONE statically-analyzable literal, whereas `Bash(scala-cli run *)` is a far broader hole ("run any code at
any path"). So the right move is `scala-cli package --native` (or Scala Native) → a real `tt` binary: no
bash, no JVM cold-start, and `Bash(tt *)` / `Bash(tt box *)` allowlisting is preserved (prefix match on the
whole command line). Bonus: a great Scala Native dogfood.

## The one real tradeoff to decide (the crux)
Today `tt typo` (pure, zero deps) compiles only `typo.scala` + nothing — tiny, instant. A single unified
project **couples all deps into one classpath**: every invocation drags `requests` + `ujson` + `os-lib` +
the whole `reqt-vendored` tree, even `tt typo`. Cold-compile of that union is slow, and per-tool minimalism
is lost. **The unified design only fully pays off once packaged to a native binary** (compile once, startup
cost gone). So the *sequence* matters:

- Unify → **then** package native.  ✅ dev-loop stays sane, startup gets faster.
- Unify but stay on `scala-cli run`. ❌ dev-loop gets *heavier* (every edit recompiles the heavy union).

Open sub-questions: does `reqt-vendored` (heavy, pins the classpath) belong in the same binary, or should it
stay a separately-invoked tool? Does the dispatcher need per-tool lazy dep loading, or does native packaging
make that moot? Native target: `scala-cli package --native` (GraalVM) vs Scala Native — which builds cleanly
on BR's box and keeps the JVM-and-Native roadmap (skills/scala-style §1) open?

## Recommended sequencing
1. ✅ **Done (2026-07-03):** object-scoping stepping stone — committed, compiles clean.
2. **Joint decision (this doc):** confirm the direction + pick the native target + settle the
   dep-union/`reqt-vendored` question BEFORE writing the dispatcher.
3. Then: add `tools/tt.scala` — one `@main` + a `Map[String, Array[String] => Unit]` registry over the tool
   objects; drop the per-file `@main`s.
4. Then: `scala-cli package --native` → `tt` binary; retire (or thin to a one-line shim) the bash `tt`.
5. Then, incrementally: migrate tool cores to typed `Iterator`-streaming functions where it pays.
