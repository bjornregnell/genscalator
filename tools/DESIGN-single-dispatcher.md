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

## Decided contract + discussion (2026-07-03, BR + agent)

BR steered the shape concretely (and noted we've likely gone down this track before and lost it — hence this
record + a memory so it doesn't evaporate again). **Agreed, parked for a scheduled session; NOT started —
tonight the tools stay in their committed object-scoped state so `tt git` etc. keep working.**

**The principle (BR).** *"Tools should be functions from input to output and not print but report in their
output — the functional-programming style of passing values rather than side-effects when possible."* So:

- **Each tool = a pure typed function**, no `println`/`sys.exit` in its body. It **reports via its return
  value**, not via stdout.
  ```scala
  // lib.scala (shared contract)
  case class ToolError(msg: String, code: Int = 2)
  trait ToolResult:                    // structured, testable; renders itself for the dispatcher
    def render: Iterator[String]
  ```
  Example (BR's worked case — `newtool`): rename `NewTool.dispatch` (which today returns `Unit` and prints)
  to a descriptive typed function, no IO:
  ```scala
  def scaffoldNewTypedTool(name: String): Either[ToolError, Scaffolded]   // Scaffolded extends ToolResult
  ```
- **One sole `@main`, in `tt.scala`**, owns ALL IO. It calls the tool, checks the `Either`, renders or
  reports the error, sets the exit code — the single effect boundary:
  ```scala
  @main def typedToolDispatcher(args: String*): Unit =
    val res: Either[ToolError, ToolResult] = args.toList match
      case "newtool" :: name :: Nil => NewTool.scaffoldNewTypedTool(name)
      case "typo"    :: rest        => Typo.classifyCli(rest)
      // …one case per tool → its typed function…
      case tool :: _ => Left(ToolError(s"unknown tool '$tool'", 2))
      case Nil       => Left(ToolError(usageText, 2))
    res match
      case Right(r)               => r.render.foreach(println)
      case Left(ToolError(m, c))  => System.err.println(m); if c != 0 then sys.exit(c)
  ```
- **Per-file `@main`s are deleted.** That's why this is coupled to the launcher: with no per-file `@main`,
  `tt <tool>` must route through `tt.scala`'s sole `@main`, which pulls all tools + their deps into one
  target (the dep-union tradeoff above).

**Pure vs effectful nuance ("when possible").** Pure tools (typo, text, log, htmltext, guardcheck) fit this
perfectly — no effects, just `input → Either[ToolError, ToolResult]`. Effectful **drivers** (box ssh, forge
POST, git commit, verify run-a-command, chrono/newtool file-writes, web fetch) still perform their effect
internally — but they too **return** the outcome as `Either[ToolError, ToolResult]` and defer the final
stdout + exit code to the dispatcher, instead of scattering `println`/`sys.exit`. (Inline stderr audit lines
— forge's `[audit]`, box's progress — stay in the driver; only the *result* stdout centralizes.)

**Safe incremental path (so nothing breaks mid-migration).** Keep bash `tt` routing per-file for unmigrated
tools; route a small `MIGRATED="newtool typo …"` set through `scala-cli run tools/tt.scala -- <tool> <args>`.
Move a tool's name into `MIGRATED` as you convert it. `tt git` and the rest keep working the whole time; when
`MIGRATED` covers everything, the per-file branch dies and bash is replaced by the native binary.

## MUST CONSOLIDATE WITH: `../research/tt-typed-args.md` (the arg-parsing half)

BR flagged (2026-07-03) that we'd **both forgotten** the earlier research note
[`../research/tt-typed-args.md`](../research/tt-typed-args.md) (2026-07-01) — and it's not a separate topic, it's
the **other half of the same architecture**. This doc decides the tool's *structure* (one `@main`, tools return
`Either[ToolError, ToolResult]`, IO in the dispatcher); that note decides how a tool's *arguments* are taken —
today every tool hand-parses stringly `args: String*` with `indexOf`/`toIntOption`, and it argues for a **tt-owned
typed-arg layer**: declare typed params → auto-parse + fail-fast **one-line friendly** errors + first-class
flags/subcommands + reusable validators (`intRange`, `existingFile`, `oneOf(enum)`, `FROM..TO`) + the
`--safe-mode`/`--sandboxed`/`--audit` declarations as typed options. (Scala 3 `@main` typed params fit only simple
leaf tools — positional basic types, no flags/subcommands — so tt's needs already exceed them.)

**They converge — the consolidated shape.** In the single-dispatcher world the **dispatcher owns parsing**, which
is exactly where the typed-arg layer belongs: `tt.scala` parses+validates argv into a tool's **typed** input (not
`String*`) and routes, so a tool becomes `def goodName(typedArgs): Either[ToolError, ToolResult]` — typed *in* AND
typed *out*, IO and parsing both centralized, tool body pure. So the "best way forward" is **one design**, not two:
single-dispatcher (structure) + typed-arg layer (input) + `Either[ToolError, ToolResult]` (output) + native binary
(packaging) + `Iterator` streaming (where it pays). **Morning task:** re-read `tt-typed-args.md` alongside this doc
and settle the consolidated architecture before writing `tt.scala` (both share the dispatcher as the seam).
