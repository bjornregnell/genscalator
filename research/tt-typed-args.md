# tt argument handling — Scala `@main` typed params vs a tt-owned typed-arg layer

- **Question:** every `tt` tool today takes **stringly** args (`args: String*` / `os.Args`, hand-parsed with
  `indexOf`/`toIntOption`). Scala 3's `@main` supports **typed** main parameters — basic types are auto-parsed
  from argv and a bad value fails fast with a generated error. Should `tt` adopt `@main` typed params, roll its
  own typed-arg layer, or mix? (BR flagged 2026-07-01.)
- **Why it matters:** validating input **at the tool boundary** = fail-fast with a clear message the human/harness
  sees, instead of a silent mis-parse deeper in. It's the *smarter + safer* pillars applied to the CLI surface
  itself. Same class as the bug the compiler caught in `RawData.valOf` (a malformed one-line body inferred
  `Nothing`) and the `FROM..TO` range parsing — typed boundaries turn "wrong at runtime" into "rejected at the
  edge." Also composes with the safe-by-design flag taxonomy (`--sandboxed`/`--safe-mode`/`--audit`) and static
  analyzability (a literal, well-formed invocation the confirmation-guard can prove safe; see
  [`confirmation-guard-static-analysis.md`](confirmation-guard-static-analysis.md)).

## The feature (docs)
- https://docs.scala-lang.org/scala3/book/methods-main-methods.html
- https://docs.scala-lang.org/scala3/reference/changed-features/main-functions.html

`@main def go(count: Int, name: String, verbose: Boolean)` generates a main that parses argv into the declared
types via the `CommandLineParser.FromString` typeclass; on a parse failure it prints a usage/error and exits
nonzero. **Limits (the "cool but limited" BR noted):**
- Only **basic types** + a trailing **vararg** of them (`Int`, `Long`, `Double`, `Float`, `Boolean`, `String`).
- **Positional only** — no first-class flags (`--write`), named/optional args (beyond param defaults), or
  subcommands (`tt text grepr`).
- Custom types need a hand-written `FromString[T]` given — possible but clunky, and per-arg **error messages are
  generic** (no domain hints like "range must be FROM..TO").

## Where each fits
- **`@main` typed params FIT simple leaf tools** — a fixed list of positional, basic-typed params. There you get
  free parsing + a usage error for **zero boilerplate**; worth using.
- **They do NOT fit tt's richer tools** — flags (`--write`, `--grep RE`, `--only`), subcommands, ranges, semantic
  validation (path exists, enum value, `FROM..TO`), and the safe-by-design flag declarations. Concrete evidence
  already in the repo tooling: `RawData.scala` needs `--jsonl PATH`, `--grep RE`, `--role`, and a **`FROM..TO`
  range** — the range isn't a basic type *and* was deliberately spelled `..` (not `<N-M>`) to dodge the zsh
  numeric-glob guard, a constraint `@main` can't know. So tt's needs already **exceed** the feature.

## Candidate direction
A small **tt-owned typed-arg module** that gives the `@main` benefits — *declare typed params → auto-parse +
fail-fast friendly error + auto-usage* — **with** the flexibility tt needs: flags, named/optional args,
subcommands, and reusable **validators** (`intRange`, `existingFile`, `oneOf(enum)`, `FROM..TO`), plus the
`--safe-mode`/`--sandboxed`/`--audit` declarations as typed options. Where basic-type parsing helps it may reuse
Scala's `FromString`, but tt owns the flag/usage/validation layer. **Errors must be one line and friendly** (not
a stack trace — cf. the `tt text grepr` `NoSuchFileException` hardening in `wr-data/`).

## Candidate for the `scala-style` skill
The resulting guidance — *use `@main` typed params for simple leaf tools; for anything with flags/subcommands
use the tt typed-arg layer; validate at the boundary; keep errors one-line and friendly (never a stack trace)* —
is a natural **style rule to graduate into the `scala-style` skill** (see
[`scala-style-evolution.md`](scala-style-evolution.md)), so every new tt tool gets typed, checked,
well-erroring args by default instead of ad-hoc `String*` parsing. Once the direction is prototyped and chosen,
promote it there as the standard for tool CLIs.

## Open / next
- Prototype both on ONE tt tool (say the `text` subcommands): `@main`-typed vs a from-scratch parser — compare
  boilerplate and **error-message quality** (the review-facing metric). Decide whether `CommandLineParser.
  FromString` is worth reusing internally or a clean-room parser reads better.
- **Status:** open (2026-07-01). **What shipped:** nothing yet (analysis note).

> Indexing: add a one-line entry under `research/README.md`'s Investigations list when the concurrent WR edits
> land (kept out now to avoid clobbering BR's open files).
