# Issue 041: a verb's capability description is hand-maintained in three carriers instead of projected from one

> status: open 2026-08-20 · labels: toolbox, design, docs, mcp · measured against: v0.10.2 (from
> `.genscalator/VERSION.txt`) · summary: every `tt` verb's description — including its
> PURE/EFFECTFUL classification, the most safety-relevant fact about it — is written by hand in the
> tool's `--help`, in `tools/README.md`'s tagline, and in `docs/gs-help.txt`, with nothing checking
> that the three agree; the project already owns the better pattern (`tt skillcheck` DERIVES its
> manifest from disk) but applies it to skills only. Prior art for the general shape: soundness PR
> #1833.

## Description

genscalator runs three different strategies against carrier drift, and the most important carrier
gets the weakest one.

| strategy | instance | status |
| --- | --- | --- |
| **derive** — one source, rendered downstream | `tools/skillcheck.scala:9` — "The expected set is DERIVED from disk (each `skills/<name>/SKILL.md` = one expected skill), so it never drifts from what the plugin actually ships" | structural |
| **assert agreement** between duplicated carriers | `tools/test/version.test.scala:73` (issue 036) — `CONTRIBUTING.md` and `reqts/issues/README.md` must state the same version, plus a negative test that no other root file re-declares it | gated |
| **nothing** | each verb's hand-written `--help` string; `tools/README.md`'s `### <tool> — <tagline>` (what `gs help tt` reads); `docs/gs-help.txt` (which the gs-dwim skill instructs a human to "keep current whenever a command changes") | **ungated** |

Nothing in `tools/test/` ties a tagline to its tool. Drift in row three is detectable only by a
human noticing, which is the same failure mode as issues 034, 036 and 040.

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
  `-experimental`. The toolbox pins upstream `3.9.0-RC4` in `tools/project.scala`. Note the
  near-miss: the fork's base is the same 3.9.0-RC4 tree, so the two are temporally aligned and
  artifact-incompatible. genscalator's install story is "scala-cli + a JDK, it fetches the rest"; a
  forked compiler is not that. (Adding a library would be unremarkable — README §5.1 already lists
  `os-lib`, `ujson`, `requests`, `munit`.)
* **`sibylline` points the wrong way for the current role.** It is a *client*: the Scala program
  holds the session and `Toolkit` runs the call/execute/report loop. Under Claude Code that loop
  already exists and the harness owns it; `tt` is the callee. `sibylline` would only be the right
  dependency if a `tt` verb should itself drive a model, which is a different product.

The design idea survives both objections, because it needs nothing from soundness but the shape.

## How to reproduce it

The drift is latent rather than currently-failing, so this reproduces the *absence of a gate*, not a
present inconsistency:

```bash
# 1. every verb's tagline, classification included, lives here as prose:
tt text match tools/README.md '^### '

# 2. the same descriptions, written again by hand, inside each tool:
tt text match tools/text.scala 'usage:'

# 3. and a third time, for the gs surface:
tt doc gs-help

# 4. nothing relates them:
tt files tools/test scala 'tools/README'
#    => 1 file (links.test.scala), and its hit is a COMMENT about link parsing,
#       not an assertion. No test relates a tagline to the tool it describes.
```

Then: change a tool's `--help` wording, or give a PURE verb a `--write` path, and run the suite. It
stays green with the tagline stale — including a purity classification that is now wrong.

## Acceptance sketch

* **Phase 1 — one declared capability per verb, projected.** Each verb declares its own summary and
  effect class next to its code (the natural home is the `object <Tool>` that already holds the pure
  core, e.g. `object Chrono` behind `chrono.scala:131`'s thin `@main`). A tool renders that set into
  the human-facing surfaces. Whether `tools/README.md` and `docs/gs-help.txt` become generated, or
  stay hand-written but gated by a test in the shape of `version.test.scala`, is the open call —
  generated is the stronger form and the one this issue argues for.
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
