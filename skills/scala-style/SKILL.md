---
name: scala-style
description: How to write genscalator Scala tools — direct, pragmatic, Safe-mode-ready. Trigger whenever scaffolding or editing a `tools/*.scala` (or any scratch Scala tool). Default to pure read → compute → print and immutable data because that's usually the safest AND cheapest path, but treat safety / token-efficiency / performance as a conscious balance — spend mutation or effects deliberately when they win, keep them local and reviewable, and put real side effects in clearly-marked drivers. Pairs with the tt-toolbox skill (which picks WHICH tool; this says HOW to write one).
allowed-tools: Bash(scala-cli run *) Bash(scala-cli compile *) Bash(scalex *)
---

# genscalator Scala style

> **Genscalator land, in one breath (especially if you're a sub-agent landing cold):** you're writing Scala the
> genscalator way — **direct style** (read → compute → print, immutable by default), **lean** (JDK + the `tt`
> toolbox first, then the dependency cascade in §1), **safe by construction** (Safe-mode / capture-checking
> ready, auditable, allowlistable — no blobs, no interpreter deps), and **pragmatic, not dogmatic** (balance
> safety / token-efficiency / performance consciously and locally, with a one-line *why*). The compiler and the
> next reader — human OR agent — are allies: a tool's effects obvious at a glance, its intent clear without
> running it. Sibling skills: **tt-toolbox** = WHICH tool to run; **this** = HOW to write one;
> **scala-code-review** = the adversarial check before you call it done.

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
- **Preference cascade (HD, BR 2026-07-10), when choosing HOW to build something.** Prefer, in order:
  **(1) JDK / stdlib**, then **(2) hand-roll** if the slice is small, bounded, and paired-test-verifiable,
  then **(3) a small direct-style Java/Scala lib** (no big frameworks, no effect systems, no reflection
  magic), then **(4) whatever ships without security risk or dependency hell**, then **(5) escalate to BR**
  (don't wing it). Each step down adds *surface*: audit cost, security surface, dependency hell, cognitive
  magic.
  - **Crossover (2 vs 3):** hand-roll the *small bounded* thing (a lexer, a CSV state machine); a small
    readable lib beats hand-rolling a *fiddly / decades-deep* domain (the "don't write a Scala compiler"
    line, which is exactly what "a small, well-understood library beats hand-rolling something fiddly" below
    already means).
  - **Security gate (overrides every tier):** never a dependency that needs an interpreter / arbitrary-exec
    runtime (node/python) or is otherwise un-auditable / un-allowlistable.
- **Helpers live inside `object ToolName`; only the `@main` (and any genuinely-public, reusable API) stays
  top-level.** A top-level `private def` still lands in the *package* scope, so when the whole `tools/` (or
  the Metals/BSP workspace) compiles as **one target**, two files that each declare a top-level `usage`
  /`fail`/`run`/`freeGb` collide: *"fail is already defined … overloaded methods must all be defined in the
  same group of toplevel definitions."* Running one file standalone (`tt foo`) hides it; a whole-toolbox
  compile (`scala-cli compile tools`, or the Scala MCP) surfaces it. Fix by scoping: wrap every helper in
  `object ToolName { … }` (brace-delimited — a long scope earns braces), so `fail` becomes `ToolName.fail`
  and pollutes nothing. Put the former `@main` body in an object method (`def dispatch`/`run`) and leave a
  one-line top-level `@main` that delegates: `@main def descriptiveName(args: String*) = ToolName.dispatch(args*)`.
  Top-level is reserved for two things only — the `@main`, and a genuinely *public, reusable* pure function
  (e.g. `stripHtml` in `htmltext.scala`, `Lib.readLatin1`) that legitimately shapes an API; everything else
  is an internal and belongs in the object. Expressions over statements; small named helpers over long blocks.
  **Avoid big frameworks** — one you'd have to reverse-engineer is a token-efficiency sink for the next reader.
- **Name the `@main` for what the tool DOES — and make it globally unique.** `tt` dispatches by *filename*
  (`tt foo` runs `tools/foo.scala`), so the `@main` name is never what you type at the CLI — it's free to be
  a long, descriptive verb-phrase (`requirementsMarkdownParser`, `stripHtml`; not `run`/`main`/`go`). Because
  the whole tree compiles as one target, every `@main` shares one global scope. So: (a) unique across the
  whole toolbox — a generic `run`/`main` WILL collide; (b) descriptive of the tool's job; (c) **must not
  clash with an imported package or type** — a tool that does `import reqt.*` cannot be `@main def reqt`
  (shadows the package); (d) **must differ from its own `object` name by more than case** — `object
  Guardcheck` + `@main guardCheck` generate `.class` files that collide on case-insensitive filesystems
  (macOS/Windows), and the compiler warns *"Generated class … differs only in case."* Pick a distinct
  verb-phrase (`checkGuardPatterns`), not the object word re-cased. A descriptive name satisfies all four for free.
- **Scope imports to where they're used.** Put a one-off `import` in the block/method that needs it (e.g.
  `import scala.jdk.CollectionConverters.*` inside the single function using it), not at the top — narrower
  scope means less name pollution and fewer surprising extensions/implicits in scope, and the reader sees
  the dependency right where it bites. Keep genuinely repo-wide imports at the top.
- Latest **stable** Scala (re-check per project). Reach first for the **JDK + the `tt` toolbox** — they
  cover most needs.
- **Wrap raw JDK APIs in a thin Scala abstraction** (e.g. `LibJVM.scala`) rather than calling
  `java.nio`/`java.net`/`Executors` directly all over the tools. A one-screen wrapper that exposes a Scala
  idiom (named results, `Using` for resources, our own naming) buys three things: one place to refactor, the
  leverage of Scala idioms over Java ergonomics, and a **far easier Scala Native port** (swap the wrapper, not
  every call site). Keep it THIN — wrap only what we actually use; don't build a JDK-shaped framework.
- **Deps are allowed, chosen with care.** A small, well-understood Maven Central library (Scala or Java)
  beats hand-rolling something fiddly. Prefer Scala libs published **for Scala 3** and **for both JVM and
  Native** (keeps the native-compilation roadmap open). Pick libs you can read and explain.
- **Iron for refinement types where it obviously pays off.** When a value has a *real* constraint — a range, non-empty,
  positive, a format (regex) — reach for **[Iron](https://github.com/Iltotore/iron)** (`A :| Constraint`, e.g.
  `Int :| Interval.Closed[1900, 2100]`) instead of a bare `Int`/`String` + a runtime guard: the constraint becomes part
  of the **type**, literals are checked at **compile time** (a bad literal won't compile), and runtime input refines at
  the boundary via `.refine` / `.refineEither` / `.refineOption` (the *validate-at-the-edge* rule, made typed). Iron is
  Scala 3, opaque-type-based (~zero runtime overhead), and cross-platform (JVM + Native). **Worked example:**
  `Year = Int :| Interval.Closed[1900, 2100]` in [`../../blog/References.scala`](../../blog/References.scala). Don't
  over-refine — use it where the constraint is real and the payoff (a whole class of bad values made *unrepresentable*)
  is clear; it is also the planned implementation for tt's typed-arg validators (tt-toolbox DESIGN).
  - **Only where friction is near-zero and gain is high — and NOT on long literals.** The refinement runs a
    **compile-time macro**; on a **long literal constant** (a paragraph of prose refined `NonBlank`) it can
    **StackOverflow the compiler** during inlining — a real crash we hit on `blog/References.scala`'s summary fields
    (see `research/031-references-summary-enum-design.md`). So refine **short, genuinely-constrained** values (`Year`, a
    `Doi`/`Url` shape, a non-empty short name); keep **long free prose as plain `String`** — it also keeps string
    concat / interpolation ergonomic. When unsure whether a field earns a refinement, **start with a `type` alias over
    `String`** (§4): it documents intent now and leaves a cheap path to a validated `case class` *or* an Iron
    constraint later **if it proves worth it** — don't over-commit the constraint up front.
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

## 4. Name your types — aliases now, opaque/case classes when they earn it
- **Give basic types good names with `type`.** A bare `String`/`Int` in a signature says nothing; `type
  ToolName = String` documents intent AND opens a cheap refactor path — swap the alias for a `case class` or
  `opaque type` later and most call sites stand. Reach for a `type` alias whenever a primitive plays a
  specific role (`ToolName`, `Relpath`, `LineNo`).
- **Opaque types when allocation cost really matters.** Scala 3 `opaque type Name = String` gives real
  compile-time type safety at **zero allocation cost** — at runtime the value *is* the underlying `String`/`Int`,
  but the compiler restricts what you can do with it to the operations you expose (enforced by scoping, not by
  a wrapper object). Use them for simple data on a **hot path** where a wrapper `case class` allocation would
  hurt. Off the hot path a `case class` is clearer (named fields, pattern-matching) — prefer it there. Rule of
  thumb: `type` alias for a name; `opaque type` for safety-without-allocation on a hot path; `case class` when
  you want structure and aren't allocation-bound.
- **Prefer NAMED tuples over unnamed ones** in `def` args and return types (Scala 3). `def segment(...):
  (blocks: Seq[String], seps: Seq[String])` self-documents and lets the caller write `r.blocks`; an unnamed
  `(Seq[String], Seq[String])` forces the reader to decode `._1`/`._2`. **This matters MOST when the members
  share a type** (`(Seq[String], Seq[String])`, `(Int, Int)`): with same-typed slots the compiler can't catch
  a swapped order, so an unnamed tuple invites silent order-confusion bugs for humans AND agents — names make
  the order self-checking. Reserve unnamed tuples for local, throwaway pairing. A named tuple is also a
  zero-cost stepping stone to a `case class` if the shape grows.

## 5. DRY, but not dogmatically — repetition is sometimes the right call
**DRY (Don't Repeat Yourself) is the default and usually right.** Duplicated logic drifts out of sync — two
"identical" parsers that quietly diverge is a *false-echt* correctness risk — so **when you notice real repetition,
treat it as a trigger to investigate a refactor:** extract the shared thing into one typed, tested definition (e.g.
`tools/seqspec.scala`, the sequence-diagram parser shared by `svg`, `ascii` + `gvdot`; see the shared-helper-file pattern,
[`../../research/038-tt-shared-helper-file-pattern.md`](../../research/038-tt-shared-helper-file-pattern.md)). One
greppable, compiler-checked source beats copies that silently disagree.

**But DRY is a heuristic, not a law — some repetition is deliberately better.** Extracting a shared unit **creates a
dependency**, and coupling has its own cost. Prefer a little repetition when:
- **Dependency surface matters more than duplication.** A shared helper couples *every* consumer to it — a change
  to satisfy one can break another, and that coupling can outweigh the saved lines. Share code when the two uses are
  the **same decision that must change together**, not merely because they *look* similar (coincidental duplication
  ≠ knowledge duplication). Two short, independently-evolving copies can be cheaper than one abstraction everyone
  must agree on.
- **Tests want independence from production.** A test that re-states a small expected value or tiny bit of logic
  **inline**, instead of importing the production code that computes it, is *more* trustworthy — it checks the
  production path against an **independent** statement rather than against itself (importing the code-under-test to
  build the expectation makes the test tautological). Deliberate test/production duplication is a feature.
- **A scratch / one-off** would gain nothing lasting from sharing, only an import and a rebuild dependency.
- **The right abstraction isn't clear yet.** Premature extraction locks in the wrong seam; two copies now, extract
  once the real boundary is obvious (rule of three). The **wrong** abstraction is costlier to undo than duplication.

**So:** notice repetition → *consider* extracting (usually do) → but weigh **duplication cost vs
dependency/coupling cost**, and leave a one-line *why* when you keep the repetition. Neither blind DRY nor blind
copy-paste. (This mirrors the skill's whole posture: pragmatic, conscious, local tradeoffs — §Intro.)

## 6. Braces vs braceless: the common style
Scala 3 allows both brace and braceless (significant-indentation) syntax. Genscalator follows the **common
style** set out in the open note *"Towards a Common Scala Style Recommendation"* (Martin Odersky, Björn
Regnell, Rex Kerr; 2026-01-07). The rule, verified against that source:

- **Put braces around a *long* scope that is not terminated by a keyword.** A scope is **long** when it
  "contains blank lines which are not already embedded in a nested construct." Braces are unnecessary when a
  closing keyword — `else`, `do`, `yield`, `case`, or `catch` — already terminates the scope (that keyword is
  the end marker).
- **Short scopes stay braceless (colon-indent)**, or take braces where they aid understanding. For short
  class/function bodies the note gives *no* recommendation — `:`-indent or braces, as you prefer.
- **Prefer braces over `end` markers** when a scope is long enough to want a marker.
- **Compensate for fewer braces with blank lines placed by logical structure**, so the vertical rhythm braces
  used to provide survives.
- **Prefer the new control constructs** (`if-then-else`, not `if (...) else`).

This is not brace-dogma; it is where **human legibility and agent-edit-safety coincide.** The note's
agent-code-generation analysis calls braces-on-long-scopes a *"genuine sweet spot — near braces-everywhere on
safety, near braceless on surface tokens":* a brace insertion is O(1) tokens, whereas a braceless edit forces
a line-by-line re-indent and risks a **silent mis-scope** bug (agents are least reliable with whitespace
semantics). In this toolbox: `mirror.sc`'s braced `for`-body (a long scope) beside its one-line `foreach:`
colon form (a short one); the `object ToolName { … }` wrapper of §1 (a long scope earns its braces).
genscalator's own indent-vs-braces experiment (reported in blog post 002) runs the measurable comparison the
note proposes — braceless / braces-everywhere / common style — across edit-error-rate and token cost.

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
- **Runtime-reflection "mystery-meat" frameworks** (the Spring-Boot archetype), where the actual behaviour is
  assembled at runtime by annotation-scanning, classpath magic, and reflective dependency injection, so you
  *can't tell what the program does by reading it*, only by running it and praying. The exact opposite of this
  skill's core value (effects obvious at a glance, intent clear without running): no `@Autowired` séances, no
  invisible bean-wiring, no "it works because a proxy the compiler never saw decided it should." If a
  dependency's behaviour lives in reflection you can't follow at compile time, it's a no-go; reach back up the
  cascade (JDK, hand-roll, or a small lib you can actually *read*).

Background: [`../../docs/foundations.md`](../../docs/foundations.md) (safe by design, capture checking),
[`../../tools/README.md`](../../tools/README.md) (conventions). Picking which tool to run: the
**tt-toolbox** skill.
