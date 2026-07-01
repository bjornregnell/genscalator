---
name: scala-style
description: How to write genscalator Scala tools — direct, pragmatic, Safe-mode-ready. Trigger whenever scaffolding or editing a `tools/*.scala` (or any scratch Scala tool). Default to pure read → compute → print and immutable data because that's usually the safest AND cheapest path, but treat safety / token-efficiency / performance as a conscious balance — spend mutation or effects deliberately when they win, keep them local and reviewable, and put real side effects in clearly-marked drivers. Pairs with the tt-toolbox skill (which picks WHICH tool; this says HOW to write one).
allowed-tools: Bash(scala-cli run *) Bash(scala-cli compile *) Bash(scalex *)
---

# genscalator Scala style

Write tools so the **compiler catches mistakes** *and* the code stays **easy for both humans and agents to
read, review, and trust** — a tool's effects obvious at a glance, its intent clear without running it. The
compiler and the reader are allies, not a tradeoff. This is **pragmatic, not dogmatic**: you're balancing
three things that usually pull together but sometimes don't —

- **safety** (reviewable, hard to misuse, Safe-mode-ready),
- **token-efficiency** (less code to write/read, less time debugging brittle helpers),
- **performance** (fast enough for the job).

Default to the safe/pure path because it's *usually also the cheapest*. When an axis genuinely conflicts,
make the tradeoff **consciously and locally**, and leave a one-line comment saying why. None of this is
black-and-white.

## 1. Direct style, lean dependencies
- Top-level functions + a single `@main`; expressions over statements; small named helpers over long
  blocks. **Avoid big frameworks** — one you'd have to reverse-engineer is a token-efficiency sink for the
  next reader (human or agent).
- **Name the `@main` for what the tool DOES — and make it globally unique.** `tt` dispatches by *filename*
  (`tt foo` runs `tools/foo.scala`), so the `@main` name is never what you type at the CLI — it's free to be
  a long, descriptive verb-phrase (`requirementsMarkdownParser`, not `run`/`main`/`go`). This is not just
  taste: an IDE (Metals) and any multi-file build compile the whole `tools/` (+ neighbouring `research/`)
  tree as **one target**, so every `@main` shares **one global scope**. Two files with `@main def run` become
  illegal overloads split across files ("run is already defined"). So: (a) unique across the whole toolbox —
  a generic `run`/`main` WILL collide; (b) descriptive of the tool's job; (c) **must not clash with an
  imported package or type** — e.g. a tool that does `import reqt.*` cannot be `@main def reqt` (shadows the
  package). A descriptive name satisfies all three for free.
- **Scope imports to where they're used.** Put a one-off `import` in the block/method that needs it (e.g.
  `import scala.jdk.CollectionConverters.*` inside the single function using it), not at the top — narrower
  scope means less name pollution and fewer surprising extensions/implicits in scope, and the reader sees
  the dependency right where it bites. Keep genuinely repo-wide imports at the top.
- Latest **stable** Scala (re-check per project). Reach first for the **JDK + the `tt` toolbox** — they
  cover most needs.
- **Deps are allowed, chosen with care.** A small, well-understood Maven Central library (Scala or Java)
  beats hand-rolling something fiddly. Prefer Scala libs published **for Scala 3** and **for both JVM and
  Native** (keeps the native-compilation roadmap open). Pick libs you can read and explain.
- **A dep in a *pure* tool needs a purity check.** Investigate what it actually does: if it can be used
  side-effect-free, the tool stays pure. If it forces effects you can't avoid, move that work into an
  effectful driver — or **mark the tool not-safe** so the planned `--safe-mode` flag (see foundations
  roadmap) can exclude it. Never let an effectful dep quietly turn a "pure" tool impure.

## 2. Default to immutability — spend mutation deliberately
- **Reach for `val` and pure transforms (`map`/`filter`/`fold`) first** — they're safe, concise, and read
  well. But this is a default, not a ban.
- A **local `var` or mutable buffer is a legitimate tool**, especially for performance (tight loops, large
  inputs, building a collection once) or when it's simply clearer than a contorted fold. Keep it
  **encapsulated**: local to the function, not escaping, so the mutation isn't observable from outside.
  When you choose it over the functional version, it's a safety↔performance/clarity tradeoff — note it.
- **Avoid mutable state in the API surface** — public `var`, shared/`this`-level mutation, a mutable thing
  handed back to callers. That's the kind that actually bites review and concurrency; the cost there rarely
  pays off.

## 3. Safe-mode-ready (prose now, compiler-checked later)
- A pure tool is **read → compute → print**: args in, result out, a clear verdict (a count, `OK`/error) so
  no bash post-processing is needed. Today purity is a *convention*; keeping to it makes a tool ready for
  capture-checking **Safe mode**, where the **compiler tracks effects and mutation** and enforces the
  boundary for you. So the plan is: be pragmatic now, tighten *programmatically* once Safe mode lands —
  not pile on prose rules.
- **Put real side effects in a separate, clearly-named effectful driver** (write files, run
  `scala-cli`/`pdflatex`, spawn processes via os-lib `os.proc`). The point isn't "effects are bad" — it's
  that a visible effect *boundary* keeps the pure core reviewable and Safe-mode-trackable. Drivers
  root-find (walk up) and stay outside Safe mode.

## Shape
```scala
//> using scala 3.8.4
//> using jvm 21
// pure tool: read → compute → print. @main name = what it DOES, globally unique (see §1), NOT `run`.
@main def grepLatin1(path: String, pat: String): Unit =
  val hits = readLatin1(path).linesIterator.zipWithIndex
    .collect { case (l, i) if l.matches(s".*$pat.*") => s"${i + 1}:$l" }
    .toList
  hits.foreach(println)
  println(s"=== ${hits.size} matches")   // verdict, no | wc needed
```

## Avoid (these rarely pay off)
- **Mutable state in the API surface** — public `var`, shared mutation, a mutable thing escaping its owner.
  (A local, encapsulated `var` is fine — see §2.)
- **Hidden failure** — `try` that swallows errors; a tool that exits "successfully" having done nothing.
- **Effects smuggled into something labelled pure**, or `cd`/`&&`/pipes baked into a tool — it should print
  the final answer itself.
- **Re-emitting brittle bash from inside Scala** — if you need an effect, make it a typed, reusable driver.

Background: [`../../docs/foundations.md`](../../docs/foundations.md) (safe by design, capture checking),
[`../../tools/README.md`](../../tools/README.md) (conventions). Picking which tool to run: the
**tt-toolbox** skill.
