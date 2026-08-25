# Issue 041: a verb's purity classification is hand-maintained in three carriers with nothing relating them, so four verbs already ship a description that omits it

> status: open 2026-08-20 · labels: toolbox, design, docs, mcp · measured against: v0.10.2 (from
> `.genscalator/VERSION.txt`) · summary: every `tt` verb's description — including its
> PURE/EFFECTFUL classification, the most safety-relevant fact about it — is written by hand in
> three places: `tools/README.md`'s tagline, the tool's own `--help` string, and the `case _ =>`
> usage block it prints on bad arguments. Nothing relates them, and they have already diverged: of
> the six verbs that carry all three, **all six disagree and four omit the purity marker from the
> third**. The project already owns the better pattern (`tt skillcheck` DERIVES its manifest from
> disk) but applies it to skills only. The general ask is a capability projected from one source,
> of which the purity classification is the sharpest instance rather than the whole of it. Prior
> art for the shape: soundness PR #1833.

## Description

genscalator runs three different strategies against carrier drift, and the most important carrier
gets the weakest one.

| strategy | instance | status |
| --- | --- | --- |
| **derive** — one source, rendered downstream | `tools/skillcheck.scala:9` — "The expected set is DERIVED from disk (each `skills/<name>/SKILL.md` = one expected skill), so it never drifts from what the plugin actually ships" | structural |
| **assert agreement** between duplicated carriers | `tools/test/version.test.scala:73` (issue 036) — `CONTRIBUTING.md` and `reqts/issues/README.md` must state the same version, plus a negative test that no other root file re-declares it | gated |
| **nothing** | each verb's three hand-written descriptions: `tools/README.md`'s `### <tool> — <tagline>` (the source of truth `gs help tt` reads, per `skills/gs-dwim/SKILL.md:36-37`), the tool's own `<Tool>Help` string, and the `case _ =>` usage block printed on bad arguments | **ungated, and already drifted in 6 of 6** |

Nothing in `tools/test/` ties a tagline to its tool. Drift in row three is detectable only by a
human noticing, which is the same failure mode as issues 034, 036 and 040 — except that here it has
stopped being a risk and become a fact, in every verb that carries all three carriers.

Issue 040 supplies the cleanest citation for the class, and it is not in the toolbox at all:
`get-genscalator.sc:223` holds the uninstaller's payload vector, and `:265` hand-types the same list
again inside a `println`, 42 lines away. One file, two copies of one fact, no test relating them — so
a fix to the first that misses the second leaves the message describing a payload the code no longer
uses. That the pattern recurs outside `tools/` is the argument that this is a project-level habit
rather than a toolbox-local untidiness.

`docs/gs-help.txt` is **not** one of the carriers, though an earlier draft of this issue said it
was. It is 44 lines describing `gs` do-what-I-mean commands; it mentions the toolbox on six of them
(`:6`, `:7`, `:8`, `:9`, `:32`, `:42`) and every one of those describes a `gs` command rather than a
`tt` verb. `skills/gs-dwim/SKILL.md:36-37` names `tools/README.md`'s `## Tools` section as the
source `gs help tt` reads. Correction recorded rather than erased, because the miscount is what hid
the real third carrier.

### The worst instance: the purity classification

`tools/README.md`'s taglines do not merely describe, they **classify**:

```
### text — typed grep/awk/cut/uniq replacement (PURE)
### sub — typed search-and-replace across files (EFFECTFUL; PREVIEW BY DEFAULT)
### md-fmt — markdown-aware line reflow to a target width (PURE by default; `--write` is the one guarded effect)
### zip — read-only zip inspection + guarded extract (JDK-only; EFFECTFUL only with `--write`)
```

That PURE/EFFECTFUL/guarded distinction is the single most safety-relevant fact about a verb. It
decides whether the verb belongs in the allowlist, whether it needs a preview default, and — the
moment the toolbox is exposed over any protocol — what a caller is permitted to invoke unattended.
Today it lives in prose, in a markdown heading, with no link to the code it describes and no test
asserting the two agree. A verb that gains a write path and does not get its tagline updated is
silently misfiled as safe.

That is not hypothetical. Six verbs carry all three carriers, and **all six disagree across them**:

| verb | `tools/README.md` tagline | `--help` tagline | `case _ =>` usage string |
| --- | --- | --- | --- |
| `guardcheck` | flag guard-trip / banned-reflex patterns **(PURE)** | flag shell / commit-message patterns that trip the guard or are banned reflexes | flag shell/commit-message patterns … — **no purity marker** |
| `harden` | Layer-1 deterministic secret scanner **(PURE, read-only)** | Layer-1 deterministic secret scanner (candidates for Layer-2 triage) | `harden -` … — ASCII hyphen, **no purity marker** |
| `log` | build/run-log analyzer **(PURE)** | build/run-log analyzer (pure) | analyze a build/run log (pure) |
| `text` | typed grep/awk/cut/uniq replacement **(PURE)** | typed grep/awk/cut/uniq replacement (pure) | `texttool` — typed grep/awk replacement (pure) |
| `typo` | keyboard-aware typo classifier **(PURE)** | keyboard-aware typo classifier (Swedish QWERTY; pure) | … (Swedish QWERTY; for the fatigue gauge) — **no purity marker** |
| `wr` | Workflow-Research utilities for the WR corpus itself **(PURE, read-only)** | Workflow-Research utilities (tooling for the WR corpus itself) | `wr -` Workflow-Research utilities — ASCII hyphen, **no purity marker** |

**Four of the six — `guardcheck`, `harden`, `typo`, `wr` — drop the purity classification from the
third carrier entirely.** `text` renames the tool to `texttool`, which is not a name anyone types,
and loses `cut/uniq`. Two of the six use an ASCII hyphen where the other carriers use an em dash.

The reason all six survived is the same for each: the `case _ =>` block is the bad-arguments path,
and the only test that looks at descriptions at all (`tools/test/cli.test.scala:2404-2413`) runs
`--help`. It covers 8 of the 46 `### ` entries in `tools/README.md`, and asserts only shape —
`out.contains(s"tt $tool —")` plus `"Full reference:"`, never content. `harden -` and `wr -` would
fail even that shape assertion if it reached them; it does not.

A floor, not a total: these six were found by sweeping `tools/*.scala` for a `println("""…` block
whose first line reads as a tagline. A verb that states its usage in some other shape would not
appear here.

### The two derivations are separable

The chain worth building is:

```
typed Scala method  ->  semantic capability  ->  machine-readable description  ->  model/tool protocol
```

but it contains **two derivations with very different costs**, and conflating them is what has kept
the cheap one from happening:

1. **Description projection** — semantic capability rendered into every human-facing surface
   (`--help`, the README tagline, the `gs help tt` table). Needs **no** type-level work: an
   annotation or a declared field on each verb, read by a tool. This alone moves row three of the
   table above to row one.
2. **Schema projection** — parameter *types* rendered into a machine-readable schema (JSON Schema
   for an MCP tool or a model ability). Needs a typed layer beneath today's
   `@main def x(args: String*): Unit`, which is the expensive half.

Only (2) is blocked on the typed refactor. (1) is available now, and it is where the current drift
lives.

## Prior art: soundness PR #1833

https://github.com/propensive/soundness/pull/1833 (merged) adds `sibylline`, a typed LLM client. The
part that matters here is not the client. It is that a method annotated `@ability` / `@about` has
its description and its JSON schema **derived**, and that — per the PR's own release notes — "the
`@about` annotation is shared with synesthesia, so one description serves a method exposed both as
an MCP tool and as a model ability."

One annotation, two protocols. The transferable claim is therefore: **the protocol is a rendering
target, not the source of truth.** And it is evidence rather than assertion because it is
demonstrated across two consumers in working code (72 offline tests), not argued in a design
document.

**This issue does NOT propose adopting soundness.** Two independent reasons, recorded so the
question is not re-opened as if it were open:

* **It is a toolchain commitment, not a dependency.** `build.mill` force-versions every artifact to
  the **propensive/scala fork** (`SOUNDNESS_SCALA_VERSION`, default `3.9.0-RC5-p14`) plus
  `-experimental`. The toolbox pins upstream `3.9.0-RC4` in `tools/project.scala:1`. So the fork is
  a patch series on **RC5**, one RC ahead of this project's pin — not, as an earlier draft of this
  issue claimed, the same RC4 tree. The conclusion is unchanged and in fact simpler: the two are
  artifact-incompatible because they are different compilers, without needing any argument about
  temporal alignment. genscalator's install story is "scala-cli + a JDK, it fetches the rest"; a
  forked compiler is not that. (Adding a library would be unremarkable — README §5.1 already lists
  `os-lib`, `ujson`, `requests`, `munit`.)
* **`sibylline` points the wrong way for the current role.** It is a *client*: the Scala program
  holds the session and `Toolkit` runs the call/execute/report loop. Under Claude Code that loop
  already exists and the harness owns it; `tt` is the callee. `sibylline` would only be the right
  dependency if a `tt` verb should itself drive a model, which is a different product.

The design idea survives both objections, because it needs nothing from soundness but the shape.

## How to reproduce it

The drift is present, so this reproduces the inconsistency itself rather than the absence of a gate.
Take `text`, whose three carriers can be read in two commands:

```bash
# 1. carrier one — the README tagline, classification included:
tt text match tools/README.md '^### text '
#    => ### text — typed grep/awk/cut/uniq replacement (PURE)

# 2. carriers two and three, both inside the same file, 131 lines apart:
tt text match tools/text.scala 'typed grep'
#    => :22   tt text — typed grep/awk/cut/uniq replacement (pure)
#    => :153  texttool — typed grep/awk replacement (pure)
#       the second renames the tool and loses cut/uniq.

# 3. nothing relates them:
tt files tools/test scala 'tools/README'
#    => 1 file (links.test.scala), and its hit is a COMMENT about link parsing,
#       not an assertion. No test relates a tagline to the tool it describes.
```

For the purity half, `guardcheck`, `harden`, `typo` and `wr` read the same way and are worse: the
third carrier omits the classification rather than merely rewording it.

Then, for the gate that is still missing: change a tool's `--help` wording, or give a PURE verb a
`--write` path, and run the suite. It stays green with the tagline stale — including a purity
classification that is now wrong.

## Acceptance sketch

* **Phase 1 — one declared capability per verb, projected.** Each verb declares its own summary and
  effect class next to its code (the natural home is the `object <Tool>` that already holds the pure
  core, e.g. `object Chrono` behind `chrono.scala:131`'s thin `@main`). A tool renders that set into
  the human-facing surfaces. Whether `tools/README.md`'s taglines become generated, or stay
  hand-written but gated by a test in the shape of `version.test.scala`, is the open call —
  generated is the stronger form and the one this issue argues for. The `case _ =>` usage block is
  the cheapest place to start: for `text` it and the `<Tool>Help` val are in the same file, so the
  first increment is one string where there are currently two, with no cross-file machinery.
* **Extend the test that already exists rather than starting from nothing.**
  `tools/test/cli.test.scala:2404-2413` already runs `--help` across 8 tools and asserts a tagline
  shape. Widening it to all verbs, to the `case _ =>` path, and to content rather than shape is a
  smaller change than a new test file, and it is what would have caught all six divergences.
* **The effect class becomes a value, not an adjective in a heading.** A small enum (`Pure`,
  `PureByDefault(guardedEffect)`, `Effectful(previewDefault)`) that the allowlist documentation and
  any future protocol surface can both consume. This is the payload that makes the projection worth
  more than tidiness.
* **Phase 2 — schema projection, only when the typed layer exists.** With per-verb typed entry
  points beneath the `String*` boundary, the same declaration renders a JSON schema. That is the
  enabler for `Feature: mcpServer` (`reqts/PRD.md:411`, UNSCHEDULED) — and, for free, for an
  `@ability`-shaped surface if the project ever hosts its own model loop.
* **Do not build a second manifest.** The whole point is that this replaces hand-maintained
  carriers; a projection that becomes a fourth place to keep in sync has failed. `tt skillcheck` is
  the reference for what success reads like.
* Out of scope: any LLM client, any dependency on soundness, any change to the `tt` CLI's
  user-facing behaviour. Phase 1 is a refactor plus a generator; the verbs behave identically.

## Discussion

### Comment by hmiddelk at 2026-08-20 11:20

Raised by hmiddelk from soundness PR #1833, framed deliberately against the agent's first reading of
it. The agent's initial answer collapsed a dependency question into a design question — arguing that
because MCP is the roadmap item (`PRD.md:411`), `sibylline` was "the wrong half" of soundness.
hmiddelk's correction is the frame this issue is written in: *"Sibylline is the wrong architectural
dependency for genscalator's current role. Its capability-description/`@ability` machinery may
nevertheless be valuable as evidence for how a capability projection should work."* Rejecting a
dependency and rejecting the evidence it carries are separate acts, and the second does not follow
from the first.

One consequence for issue **040**, filed earlier the same day: its acceptance sketch asks for a CI
assertion that the uninstaller's well-known-paths list agrees with the release workflow's staged
payload — strategy two in the table above. Under this frame the better fix is strategy one: derive
the list from the staging step so agreement is structural. Same bug, stronger fix, visible only from
this framing. Worth settling here before 040's fix lands, since shipping the assertion first makes
the derivation harder to justify later.

Agent disclosure: drafted by an AI agent (Claude Opus 5) in session with hmiddelk, from hmiddelk's
reframing of PR #1833. The agent verified: the three description carriers and the absence of any
test relating them; `skillcheck.scala`'s derive-from-disk comment; `version.test.scala`'s
carrier-agreement tests; the purity classifications in `tools/README.md`; soundness's
forked-compiler pin in `build.mill`; and the `@about`-shared-with-synesthesia claim in #1833's
release notes. NOT verified: that synesthesia is specifically an MCP *server* module (taken from
#1833's wording), and whether soundness compiles or native-images under genscalator's toolchain —
untested, and moot unless the dependency question reopens.

### Comment by hmiddelk at 2026-08-24 20:50

Recording the open call from bjornregnell's review of PR #5, so it is settled here rather than in a
thread. Verbatim:

> On the substance: I lean toward derive over assert for 040, so your Discussion note there is
> well taken. Whether Phase 1 is worth doing ahead of the typed layer is a call I want to make
> separately — reply here and I will settle it in the issue rather than the thread.

**The answer this issue argues for: yes, Phase 1 ahead of the typed layer, and in the derive form
rather than the assert form.** Four reasons, in descending order of how much they depend on
judgement:

1. **The drift is present, not latent, and it is six verbs deep.** `guardcheck`, `harden`, `log`,
   `text`, `typo` and `wr` each carry a third description on the `case _ =>` bad-arguments path, and
   all six disagree with their own `--help` tagline and their own `tools/README.md` tagline. **Four
   of the six — `guardcheck`, `harden`, `typo`, `wr` — drop the purity classification from that
   third string entirely.** So the safety-marker argument above is not a forecast; it has instances.
   (Floor, not a total: the six were found by sweeping `tools/*.scala` for a `println("""…` block
   whose first line reads as a tagline. A verb stating its usage in another shape would not appear.)
2. **Phase 1 needs no type-level work, so the typed layer is not on its critical path.** All three
   carriers per verb can render from one declared capability on the `object <Tool>` that already
   holds the pure core. Nothing here waits on a typed layer beneath `args: String*`.
3. **Deriving is what closes this specific hole; asserting is not.** An agreement test has to be
   *told* which strings are carriers, and whoever writes it will enumerate the ones they can think
   of — which is the same act that produced the drift. `cli.test.scala:2404-2413` is the proof:
   it runs `--help` across 8 of the 46 `###` entries and asserts `out.contains(s"tt $tool —")`,
   shape only, and it has never touched the `case _ =>` path where all six divergences live. Two of
   the six (`harden -`, `wr -`) would fail even that shape assertion if it reached them. A
   projection makes a carrier a carrier by construction.
4. **Phase 1 is not throwaway if Phase 2 slips.** The typed layer changes what a capability
   *contains* (a parameter schema); it does not change where the capability lives. Phase 1 builds
   the home, Phase 2 fills it.

The cheapest first cut is smaller than the sketch above implies: two of the three carriers sit in
the **same file** — `text.scala:22` and `text.scala:153` are 131 lines apart — so the first
increment needs no cross-file machinery at all, only one string where there are currently two.

One constraint carried over from the acceptance sketch, because it is the way this fails: if the
projection becomes a fourth place to keep in sync, it has failed. `tt skillcheck` is the reference
for what success reads like.

Agent disclosure: drafted by an AI agent (Claude Opus 5) in session with hmiddelk. The agent
verified, against `main` at `8060b2d`: the three carrier strings for each of the six verbs named
above and their disagreements; that four of the six omit any purity marker; that `text.scala:153`
and its five siblings are on the `case _ =>` path rather than the `--help` path; the 8-tool,
shape-only assertion at `cli.test.scala:2404-2413`; and the count of 46 `###` entries in
`tools/README.md`. NOT verified: that six is the complete set of third carriers (see the sweep
caveat above), and nothing here was run — the divergences were read, not reproduced.

### Comment by bjornregnell at 2026-08-25 20:36 — DECISION

Settling the call I reserved on PR #5, in the issue as promised.

**Yes: Phase 1 goes ahead of the typed layer, in the derive form.** hmiddelk's four reasons above
carry it, and I am ratifying them rather than restating them. Two things to add, one that
strengthens the case and one that bounds the work.

**The detection caveat is itself the argument for derive, and it is stronger than the four reasons
put it.** The six verbs were found by sweeping for a `println("""…` block whose first line reads as
a tagline, so six is a floor and nobody knows the total. Now notice what an assert-agreement test
would have to do: be *told* which strings are carriers — the same enumeration, with the same blind
spot, performed by the same kind of sweep. An agreement gate built today would be built against
exactly the six we happen to have found, and would certify the rest as clean. A projection has no
enumeration step at all: a verb renders its description from its declaration or it has no
description, so the unfound tail stops existing rather than staying unfound. **The fact that we
cannot bound the drift is the reason to derive it away instead of asserting against it.**

**But sequence it, and do not touch 46 verbs in one change.** Start with the six that have proven
drift — `guardcheck`, `harden`, `log`, `text`, `typo`, `wr` — and take `text` first, since two of
its three carriers sit in one file 131 lines apart and the increment needs no cross-file machinery.
That gives a countable that must move (six verbs disagreeing across three carriers, down to zero)
before any general mechanism is committed to, which is the shape I want on a refactor this wide. A
46-verb mechanical diff arriving in one PR is not reviewable at the level this repo reviews things,
and the design would be getting ratified by its own diff size.

The sketch's constraint stands unchanged and is the acceptance condition I care most about: if the
projection becomes a fourth place to keep in sync, it has failed. `tt skillcheck` is the reference.

**Phase 2 is not decided here.** It stays gated on the typed layer beneath `args: String*` and on
`Feature: mcpServer` remaining UNSCHEDULED in the PRD. Nothing in this decision schedules it, and
Phase 1 must be worth shipping on its own — which, per reason four above, it is.

Agent disclosure: an AI agent (Claude Opus 5) drafted this comment from my ruling and added the
detection-caveat argument and the sequencing condition, which I reviewed and adopted; the decision
and its scope are mine.
