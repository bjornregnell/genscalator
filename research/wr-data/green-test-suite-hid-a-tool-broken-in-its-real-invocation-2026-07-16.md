# A green test suite hid a tool that was broken in its real invocation (2026-07-16)

**Type:** WR data — a verification finding (a structural test/reality gap in the `tt` toolbox), plus a serendipitous
process finding about what BR-present shepherding actually bought.
**Threads:** [[echt-effort-especially-self-generated]], [[not-afk-safe-solo-yields-wr-data]], the scala-style
one-target-compile gotcha (§1), SM121.

## What happened (observable)

Building the SM121 SessionStart hook, the agent added `MiniJson.parse` to `tools/hangover.scala` and ran the tests:

```
scala-cli test tools --test-only 'HangoverSuite'
→ 0 failed, 0 ignored, 10 total
```

Green. Then it ran the tool the way Claude Code will actually invoke it:

```
tt hangover hook '{"source":"resume","transcript_path":"…"}'
→ [error] tools/hangover.scala:163:15  Not found: MiniJson
→ Compilation failed
```

**The tool did not compile at all.** The fix was one line: `//> using file minijson.scala` (which
`statusline.scala`, the other MiniJson consumer, already had).

## Why the tests could not catch it

The two invocations compile **different targets**:

- `scala-cli test tools` compiles the **whole directory as one target**, so `minijson.scala` is in the target and
  `MiniJson` resolves. Every pure test passes.
- `tt <tool>` runs the **single file standalone**, so the file's own `using file` directives are the *only* thing
  that pulls in its dependencies. Without the directive: no MiniJson, no compile, no tool.

So the test suite was **structurally incapable** of failing on this bug. It is not a coverage hole to be fixed by
writing more tests of that kind — more `HangoverSuite` tests would all have passed too. The green was *true* about
the pure functions and *silent* about the artifact that actually ships.

This is the mirror image of the gotcha `scala-style` §1 already records (top-level helper collisions that a
standalone `tt foo` run hides and a whole-toolbox compile surfaces). Same seam, opposite direction: **each compile
target is blind to a class of bug the other one sees.** Worth stating as a general rule for the toolbox — a
`using file` / dependency change is only verified by a **real standalone invocation**, never by the test suite.

## The serendipity: what shepherding actually bought

BR was watching the post-warp window for mechanical regressions and offered: *"go do something guard stall risky
while i am here"* — the point being that a guard stall while he is AFK becomes a forced confirmation nobody can
clear ([[guard-against-forced-confirmations]]).

**No stall occurred** (10+ bash calls, all bare, zero stalls — see the mitigation in
[[post-compact-is-highest-risk-window-for-mechanical-bash-regression-2026-07-16]]). So on its stated terms the
offer found nothing.

But it **pulled the real-invocation smoke test forward into the BR-present window**, and *that* is what exposed the
bug. Without the offer, the plausible path was: green tests → commit → the hook ships broken → it is discovered at
the next session start, i.e. inside a blackout, by a tool whose entire job is reporting on blackouts.

**The finding:** an invitation to "do the risky thing while I am here" is valuable **beyond the risk it names**. Its
real yield here was not stall-shepherding but **forcing the agent to exercise the artifact for real**, at a moment
when a human was present to see the outcome. The agent's own instinct after a green suite was to commit; the
prompt to be reckless is what produced the evidence. Cheap to offer, and it caught something the test design could
not.

## Follow-ups

- **Candidate rule** (for `scala-style` / `contribute-tool`): a new `using` directive or a new cross-file
  dependency in a `tt` tool MUST be verified by running `tt <tool>` itself. Green `scala-cli test tools` does not
  verify the shipped artifact.
- **Candidate lint:** compare each `tools/*.scala`'s referenced top-level symbols against its `using file`
  directives, or simply smoke-run every `tt` tool's `--help` in CI — the cheapest possible standalone-compile check
  across the whole toolbox. (`--help` alone would have caught this one.)
- Both are un-run hypotheses, not verified fixes.
