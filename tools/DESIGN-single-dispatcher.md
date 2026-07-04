# Design: one sole `@main` dispatcher + typed args in / typed result out + streaming + a native `tt` binary

Status: **PLAN (consolidated 2026-07-04), not started.** This is now the *single* design note for the tt-toolbox
refactor — it absorbs the former arg-parsing note `../research/tt-typed-args.md` (left as a pointer stub). Needs a
joint BR+agent go-ahead on sequencing before any code moves. Raised by BR 2026-07-03 during the object-scoping
refactor (the stepping stone below); typed contract + streaming type settled 2026-07-04.

## Where we are now (after the 2026-07-03 stepping-stone refactor)
Each `tools/<name>.scala` is `object <Name> { def dispatch(args: String*) }` plus a thin top-level
`@main def <descriptive>(args) = <Name>.dispatch(args*)`. `tt` (a bash launcher) runs ONE file per call via
`scala-cli run tools/<name>.scala`. Helpers are scoped in the object, so the whole tree now compiles as one target
without top-level-name collisions (the bug that prompted this). The objects are *already the shape* the dispatcher
needs — `tt.scala` will call `Box.…`, `Forge.…`, one case per tool.

## The consolidated architecture — ONE design, four seams
Earlier we (twice) split this across two notes and lost the thread. It is **one** architecture with four seams,
all meeting at the dispatcher:

1. **Structure** — one sole `@main` in `tt.scala` routes `tt <tool> <args>` to the right tool; per-file `@main`s
   are deleted. Tools become non-`@main` typed functions.
2. **Input (typed args in)** — the dispatcher owns parsing: it turns raw argv into a tool's **typed** input via a
   tt-owned typed-arg layer (validators, flags, subcommands, one-line friendly errors). Not `String*`.
3. **Output (typed result out)** — a tool **returns** its outcome as a typed value (`enum ToolResult`) and never
   `println`/`sys.exit`s. The dispatcher is the single IO boundary: it renders the result to stdout, routes
   errors to stderr, sets the exit code.
4. **Packaging** — replace the bash `tt` with a **native `tt` binary** (`scala-cli package --native`), so the
   dep-union cost (below) is paid once at build time, not per invocation, and `Bash(tt …)` allowlisting survives.

BR's guiding principle (verbatim): *"tools should be functions from input to output and not print but report in
their output — the FP style of passing values rather than side-effects when possible."* Seams 2+3 are that
principle made concrete: **typed in, typed out, IO centralized in the one dispatcher.**

## The typed contract (settled 2026-07-04)

BR asked: instead of `Either[ToolError, ToolResult]`, design our own `enum ToolResult` (enum? case class? trait?),
maybe a `ToolArgs`, and stream results through something better-typed than `Iterable[String]`. Settled shape:

### Output — `enum ToolResult` (replaces the public `Either`)
```scala
// lib.scala (shared contract)

case class ToolError(msg: String, code: Int = 2)   // one-line friendly msg; code is the process exit code

trait Rendered:                                     // the success payload: structured, testable, lazy-rendering
  def render: LazyList[String]                      // the ONLY place output *shape* lives (see streaming §)

enum ToolResult:
  case Ok(out: Rendered)                            // success
  case Warn(out: Rendered, notes: List[String])     // success + non-fatal warnings (notes -> stderr)
  case Partial(out: Rendered, err: ToolError)       // streamed some output, then failed
  case Err(err: ToolError)                          // failure, no payload

  def stdout: LazyList[String] = this match
    case Ok(o) | Warn(o, _) | Partial(o, _) => o.render
    case Err(_)                             => LazyList.empty
  def stderr: List[String] = this match
    case Warn(_, ns)   => ns
    case Partial(_, e) => List(e.msg)
    case Err(e)        => List(e.msg)
    case Ok(_)         => Nil
  def exitCode: Int = this match
    case Ok(_) | Warn(_, _) => 0
    case Partial(_, e)      => e.code
    case Err(e)             => e.code
```

**Why an `enum`, not a bare `Either` or a `trait`:** `Either[ToolError, Rendered]` only models two states; real tools
have four (clean success, success-with-warnings, streamed-then-failed, hard failure). A Scala 3 `enum` is the right
tool for a *closed* sum with per-case payloads, and it carries its own `stdout`/`stderr`/`exitCode` methods so the
dispatcher stays a dumb, uniform sink. Room to grow (e.g. a `Progress` case for long jobs) without touching call
sites. `Rendered` stays a **trait** because the payload is open — every tool has its own result record
(`Scaffolded`, `GrepHits`, `ModelList`, …) that `extends Rendered` and knows how to render itself.

**`Either` is NOT banished — it is demoted to an internal working type.** Inside a tool, chaining parse → validate →
compute is most ergonomic as `Either[ToolError, A]` in a `for`-comprehension. Tools use `Either` privately and
convert to `ToolResult` only at the return boundary (`.fold(ToolResult.Err(_), r => ToolResult.Ok(r))`). So: **public
contract = `enum ToolResult`; internal short-circuiting = `Either`.** Best of both.

### Input — `RawArgs` + per-tool typed args + a shared combinator layer
There is no single concrete `ToolArgs` type that fits every tool (box has subcommands; git has `--add` +
`--message-file`; typo is positional). So "typed args" is **three things**, not one:
- **`RawArgs`** — the shared currency the dispatcher hands each tool: the tool name + `Vector[String]` argv +
  helpers. This is what the registry is keyed on (`Map[String, RawArgs => ToolResult]`).
- **Per-tool typed record** — each tool declares its own `case class FooArgs(...)` (typed, validated fields).
- **The tt typed-arg combinator layer** (the bridge, absorbed from `tt-typed-args.md`) — reusable readers/validators
  that parse a `RawArgs` slice into `Either[ToolError, FooArgs]` with **one-line friendly** errors.

So a tool is two functions the dispatcher fuses:
```scala
object Foo:
  case class Args(count: Int, path: os.Path, mode: Mode)          // typed record
  def parse(raw: RawArgs): Either[ToolError, Args] =              // built from the combinator layer
    for
      count <- raw.int("count", intRange(1, 100))
      path  <- raw.opt("--file", existingFile)
      mode  <- raw.sub(oneOf(Mode.values))
    yield Args(count, path, mode)
  def run(a: Args): ToolResult = …                               // pure-ish; effectful drivers still RETURN typed
  // dispatcher registers:  "foo" -> (raw => parse(raw).fold(ToolResult.Err(_), run))
```

### The dispatcher — the one `@main`, the single IO boundary
```scala
// tt.scala
@main def typedToolDispatcher(args: String*): Unit =
  val raw = RawArgs.from(args)
  val res: ToolResult = registry.get(raw.tool) match
    case Some(tool) => tool(raw)
    case None       => ToolResult.Err(ToolError(usageText, 2))
  res.stdout.foreach(println)                       // the ONLY println in the whole toolbox
  res.stderr.foreach(System.err.println)
  if res.exitCode != 0 then sys.exit(res.exitCode)
```
`registry: Map[String, RawArgs => ToolResult]`. Effectful **drivers** (box ssh, forge POST, git commit, verify,
file-writers, web fetch) still perform their effect internally — but they too **return** `ToolResult` and defer the
final stdout + exit to the dispatcher. Inline stderr *audit* lines (forge `[audit]`, box progress) stay in the
driver; only the *result* stdout centralizes.

## The typed-arg layer (consolidated from `tt-typed-args.md`)
Every tool today hand-parses stringly `args: String*` with `indexOf`/`toIntOption`. Scala 3's `@main` typed params
auto-parse basic types from argv and fail fast — **but only** for a fixed list of *positional, basic-typed* params:
no flags (`--write`), no subcommands (`tt text grepr`), no ranges (`FROM..TO`), no semantic validation (path exists,
enum value), no domain error hints. tt's tools already exceed that (e.g. `RawData` needs `--jsonl PATH`, `--grep RE`,
`--role`, and a deliberately `..`-spelled `FROM..TO` range that dodges the zsh numeric-glob guard). So:

- **Use `@main` typed params only for a genuinely simple leaf tool** (positional basic types) — zero boilerplate there.
- **For everything with flags/subcommands/validation, use the tt typed-arg layer**: declare typed params → auto-parse
  → fail-fast **one-line friendly** error (never a stack trace) → auto-usage. Reusable validators: `intRange`,
  `existingFile`, `oneOf(enum)`, `fromTo`. First-class flags, named/optional args, subcommands. The
  `--safe-mode`/`--sandboxed`/`--audit` declarations become typed options here too, so a static confirmation-guard can
  prove an invocation safe.
- Where basic-type parsing helps, the layer may reuse Scala's `CommandLineParser.FromString` internally, but tt owns
  the flag/usage/validation surface. **Errors must be one line and friendly** (cf. the `tt text grepr`
  `NoSuchFileException` hardening in `wr-data/`).

This graduates into `skills/scala-style` once prototyped: *use `@main` typed params for simple leaf tools; for
anything richer use the tt typed-arg layer; validate at the boundary; one-line friendly errors, never a stack trace.*

## Streaming type — research + recommendation (2026-07-04)

BR: *"`Iterable[String]` is low ambition; `Iterable[ToolResult]` is more typed; Scala has `LazyList` — look it up and
decide LazyList vs Iterable on performance."* Streaming shows up at **two levels**: (a) a tool streaming its output
**lines** (`Rendered.render: ?[String]`), and (b) the dispatcher/pipe streaming a sequence of **results** tool→tool
(`?[ToolResult] => ?[ToolResult]`, the efficient-piping goal). Same axes apply to both. Findings (nightly docs):

| Type | Lazy? | Memoizes? | Re-traversable? | Hold-the-head leak? | Cost profile |
|------|-------|-----------|-----------------|---------------------|--------------|
| **`List`/`Vector`** (strict) | no — fully realized | n/a (all in memory) | yes | n/a | simplest; fine for small bounded output |
| **`Iterator`** | yes | no | **no — single-use** | no (nothing retained) | lowest overhead; the classic one-shot pipe |
| **`View`** | yes | **no — recomputes each traversal** | yes (but recomputes) | **no** | pure value, zero retention; *recompute* is the cost |
| **`LazyList`** | yes (`#::`) | **yes — caches computed elements** | yes (cheap, cached) | **YES** | cleanest construction/pattern-match; memo cost + leak risk |

Doc anchors: *"A LazyList is like a list except that its elements are computed lazily … Only those elements requested
are computed."* — *"All collections except lazy lists and views are strict."* — and the crux contrast, **"Unlike
LazyList, which caches computed elements, views remain pure proxies without intermediate storage."** The LazyList
footgun: because it memoizes from the head, **any long-lived reference to the head retains the entire realized
prefix** → space leak / OOM on large or infinite streams. The rule is "consume it as a stream; never bind the head to
a val that outlives the traversal." Views avoid the leak (they retain nothing) but *recompute* on each pass, and are
*"very confusing if the delayed operations have side effects"* — so `render` must be pure.

**Recommendation (settles BR's lean, with the honest caveat):**
- **`Rendered.render: LazyList[String]`** for the payload API — BR's lean, and right *here*: it's a **pure value**
  (direct-style, testable, re-traversable in unit tests), and `#::` gives the cleanest construction and pattern
  matching. The leak **cannot materialize at the framework boundary** because the dispatcher consumes it as
  `res.stdout.foreach(println)` and **never holds the head** (nothing binds the `LazyList` to a surviving val, so GC
  reclaims the prefix as the loop advances). Memoization is a small, bounded cost for a write-once sink and buys
  cheap test re-traversal.
- **`Iterator[String]` for unbounded/huge tool *cores*** that genuinely pipe line→line (`log` scans, `text`/grepr over
  a big tree). There, single-use + zero retention is exactly right and memoization would be pure waste; the core
  produces an `Iterator`, and `render` can wrap it (`LazyList.from(it)`) only if the output is bounded.
- **`View` is the dark horse worth a prototype** for the **pipe** level (`View[ToolResult]`): it gives LazyList's
  *value semantics* (a pure, re-traversable value — nicer than a stateful one-shot `Iterator`) **without** the
  hold-the-head leak, at the cost of recomputation on a second pass the dispatcher won't do anyway. If a prototype
  confirms the ergonomics, `View[ToolResult]` may be the better pipe currency than `LazyList[ToolResult]`.
- **Never `Iterable[String]`** as the contract — too weak (says nothing about laziness/one-shot) and exactly the "low
  ambition" BR flagged. The typed `LazyList[String]` / `Iterator[String]` / `View[ToolResult]` choices above all
  dominate it.

Net: **`LazyList[String]` render + `Iterator` cores + `View` as the pipe candidate**, all under the discipline "never
hold the head" — which the next section makes the *compiler's* job.

## The dep-union / native-packaging crux (the one real tradeoff to decide)
Today `tt typo` (pure, zero deps) compiles only `typo.scala` — tiny, instant. A single unified project **couples all
deps into one classpath**: every invocation drags `requests` + `ujson` + `os-lib` + the whole `reqt-vendored` tree,
even `tt typo`. Cold-compile of that union is slow, and per-tool minimalism is lost. **The unified design only fully
pays off once packaged to a native binary** (compile once, startup cost gone). So sequence matters:
- Unify → **then** package native. ✅ dev-loop stays sane, startup gets faster.
- Unify but stay on `scala-cli run`. ❌ dev-loop gets *heavier* (every edit recompiles the heavy union).

Why the bash `tt` exists (and why the replacement must be native, not `scala-cli run`): (a) it's a bootstrap/latency
shim; (b) it's the **allowlist anchor** — `Bash(tt box *)` is ONE statically-analyzable literal, whereas
`Bash(scala-cli run *)` is a far broader hole ("run any code at any path"). `scala-cli package --native` → a real
`tt` binary keeps `Bash(tt *)`/`Bash(tt box *)` allowlisting intact (prefix match on the whole command line), kills
JVM cold-start, and is a great Scala Native dogfood.

Open sub-questions: does `reqt-vendored` (heavy, pins the classpath) belong in the same binary, or stay a
separately-invoked tool? Does the dispatcher need per-tool lazy dep loading, or does native packaging make that moot?
Native target: `scala-cli package --native` (GraalVM) vs Scala Native — which builds cleanly on BR's box and keeps the
JVM-and-Native roadmap (skills/scala-style §1) open?

## Hardening to fold in — `tt git` commits are not atomic to `--add` (BR-observed 2026-07-03)
`git.scala` stages the `--add` paths then runs `git commit -F <msg>` with **no pathspec**, so it commits **everything
staged**, not just what was `--add`'d. BR had a one-word blog-002 edit already staged; it rode into a blog-003-titled
commit (`a91f764`) — harmless there, but the same class as the 2fc896b mislabel: a commit whose contents exceed its
message. **Fix:** when `--add` paths are given, scope the commit — `git commit -F <msg> -- <adds...>` — so tt git
commits are **atomic to exactly the requested paths** and can never sweep in unrelated staged changes. (When no
`--add` is given — the "stage separately" mode — keep committing the staged set; that's the caller's explicit choice.)
Small, worth doing as part of the git.scala rewrite.

## Safe incremental migration path (nothing breaks mid-migration)
Keep bash `tt` routing per-file for unmigrated tools; route a small `MIGRATED="newtool typo …"` set through
`scala-cli run tools/tt.scala -- <tool> <args>`. Move a tool's name into `MIGRATED` as you convert it. `tt git` and the
rest keep working the whole time; when `MIGRATED` covers everything, the per-file branch dies and bash is replaced by
the native binary.

## Recommended sequencing
1. ✅ **Done (2026-07-03):** object-scoping stepping stone — committed, compiles clean.
2. ✅ **Done (2026-07-04):** this consolidated plan — contract (`enum ToolResult`, `Rendered`, `RawArgs` + typed-arg
   layer) + streaming recommendation (`LazyList` render / `Iterator` cores / `View` pipe candidate) + CC future work.
3. **Joint go-ahead (BR):** confirm the direction + pick the native target + settle the dep-union/`reqt-vendored`
   question BEFORE writing `tt.scala`.
4. Then: add `tools/lib.scala` (the contract types) + `tools/tt.scala` (one `@main` + the `RawArgs => ToolResult`
   registry); migrate `newtool` + `typo` first (pure, easy); drop their per-file `@main`s.
5. Then: prototype the typed-arg combinator layer on ONE richer tool (`text` subcommands) — compare `@main`-typed vs
   the clean-room parser on boilerplate AND **error-message quality** (the review-facing metric).
6. Then: `scala-cli package --native` → `tt` binary; retire (or thin to a one-line shim) the bash `tt`.
7. Then, incrementally: migrate tool cores to typed streaming (`Iterator` cores, `LazyList` render) where it pays;
   prototype `View[ToolResult]` at the pipe level.

## Future work / second iteration — Capture Checking makes "never hold the head" the compiler's job
The streaming API's one real footgun is discipline-based: **holding the head of a `LazyList` retains the whole
realized prefix** (memory leak), and a lazy `render`/pipe could also silently **capture an escaping capability** (a
file handle, a mutable buffer, an ssh session) that outlives its safe scope. **Capture Checking (CC)** — experimental
Scala 3 — turns exactly these runtime footguns into **compile-time type errors**. This is genscalator's OWN,
**direct-style** path to the Safe-mode vision (skills/scala-style §3): the safety comes from the *compiler*, **not**
from kyo's effect system ([[kyo-ai-inspiration]], ruled out — no monadic runtime).

**How CC works (from the nightly basics page).** A *capturing type* is written `T^{c1, c2, …}` — `T` plus the set of
capabilities it may access. `T^` = captures anything; `A -> B` = pure function (captures nothing); `A ->{c} B` =
captures only `c`; `A => B` = impure. Capabilities are types marked with `SharedCapability` (e.g. `FileSystem^`,
`CanThrow[E]^`). The checker computes each closure's capture set and **rejects a value whose captures escape their
scope.**

**The docs' own LazyList example is exactly our case.** A lazy `map` is typed:
```scala
trait LzyList[+A]:
  def map[B](f: A => B): LzyList[B]^{this, f}     // the result CAPTURES this and f
```
So this compiles (strict `List` — effect runs and completes inside the scope):
```scala
val xs = usingLogFile { f => List(1, 2, 3).map { x => f.write(x); x * x } }
```
…but the lazy version is a **type error**, because the returned `LzyList` captures `f` (the log file), which escapes
`usingLogFile`'s scope — the checker flags *"capability `f` cannot be included in outer capture set."* A structure that
would secretly retain a closed file (or an un-GC'd head) **won't compile.**

**What this buys our API (2nd iteration, once CC is less experimental):**
- Annotate `Rendered.render: LazyList[String]^{}` — a pure render that captures **nothing**. A `render` that
  accidentally closes over a file handle, an ssh session, or a mutable buffer becomes a **compile error**, not a
  latent leak.
- Annotate the dispatcher boundary so a tool **cannot leak a capability into its `ToolResult`** — the effectful driver
  must complete its effect *before* returning; a result that still holds the capability won't type-check. That is
  `--safe-mode` enforced by the compiler instead of by convention.
- The "never hold the head" rule stops being a discipline-you-must-remember: a held head that would retain the prefix
  shows up as an escaping capture. CC-safe `LazyList` becomes *provably* leak-free, so we can lean into the value
  semantics without the caveat.

Positioned as a second iteration: adopt CC once it stabilizes; until then the discipline (dispatcher consumes
head-not-held; `render` pure) holds the line. Part of the standing future-research thread on leveraging experimental
CC for safety. CC basics + the safe-LazyList example:
https://nightly.scala-lang.org/docs/reference/experimental/capture-checking/basics.html

## Tool candidates logged for the reopened toolbox
- **`tt tsv`** — typed TSV slice/select/count (kills chained `cut`/`awk` over result tables).
- **`tt sweep-status`** — assemble a long-job progress line (row count vs target, current model/cell, ETA) so the
  verbose status stays OUT of the agent's context (the monitor-tick rot knob).
- **`tt which <tool>`** — typed toolbox introspection: where a tool resolves, which toolbox(es) exist, symlink vs repo
  clone. Kills the `readlink; echo; ls; $(which …)` reflex compound (WR data 2026-07-04); the `$(…)` in that reflex
  trips the `simple_expansion` confirmation guard, so a typed call is safer too.
- **`tt files` already exists** (os-lib list/stat/walk) — the recurring `ls`/`cat`/`grep | head` reflex is a *discipline*
  gap, not a missing tool; consider widening `files` to be the obvious reach for shell-introspection questions.

---
*Consolidates and supersedes `../research/tt-typed-args.md` (now a pointer stub). This is the single tt-refactor plan.*
