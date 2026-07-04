# tt argument handling — CONSOLIDATED into the single tt-refactor plan

**Status: superseded 2026-07-04.** This note (the arg-parsing *half* of the tt-toolbox refactor — typed params vs a
tt-owned typed-arg layer, flags/subcommands/validators, one-line friendly errors) has been **folded into the single
design note**, because it was never a separate topic — it's the *input seam* of the one-dispatcher architecture (the
dispatcher owns parsing, so tools become typed-IN and typed-OUT).

➡ **See [`../tools/DESIGN-single-dispatcher.md`](../tools/DESIGN-single-dispatcher.md)** — sections *"The typed
contract"* (the `RawArgs` + per-tool typed record + combinator-layer split) and *"The typed-arg layer"* (when `@main`
typed params fit, when to use the tt layer, validators, safe-mode flags, the graduate-into-scala-style rule).

Kept as a stub so old links resolve. Do not add new content here — extend the design note instead.
