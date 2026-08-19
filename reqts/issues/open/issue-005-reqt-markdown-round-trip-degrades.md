# Issue 005: reqT-lang's markdown round trip is not idempotent, and each pass destroys more

> status: open · labels: reqt-lang, upstream-candidate, data-loss · summary:
> `Model.toMarkdown(MarkdownParser.parseModel(x))` is neither equal to `x` nor stable under
> repetition. Emphasis markers are eaten one per pass, so the damage COMPOUNDS. Any tool that
> renders a parsed model back over a human-edited file corrupts it a little more each run,
> silently, because the result still parses.

## Description

Two different properties are worth separating:

* **identity** — `toMarkdown(parseModel(x)) == x`, byte for byte. The strong property, and what a
  file that humans also edit actually needs.
* **idempotence** — with `norm = toMarkdown ∘ parseModel`, `norm(norm(x)) == norm(x)`. The weak
  property: the first pass may reformat, but it reaches a fixed point and stays there. This is what
  makes a formatter safe to re-run.

Measured 2026-07-27 against the vendored reqT-lang, on two real files in this repo:

| file | identity | idempotent | lines before → after |
|---|---|---|---|
| `reqts/ROADMAP.md` | no | **no** | 92 → 112 |
| `reqts/PRD.md` | no | **no** | 685 → 711 |

Identity failing is unsurprising. **Idempotence failing is the defect**, because it means there is no
fixed point: the output of pass N is not the output of pass N+1, and the difference is loss.

## The compounding, on one real line

`reqts/PRD.md` line 3, across two passes:

```
original : **agent capabilities** — skills, plugins…   *is-a* Capability
pass 1   : **agent capabilities*  — skills, plugins…   *is-a* Capability
pass 2   : **agent capabilities*  — skills, plugins…   *is-a  Capability
```

One emphasis marker consumed per pass. The root cause looks like prose lines being lexed for `*` as a
list or emphasis marker and one being consumed, rather than the span being preserved verbatim.
Indentation goes the same way: `  - Single dispatcher …` loses its leading spaces on pass 2. Line
counts also grow, so lines are being split as well as stripped.

## Why it matters here

Nothing in genscalator renders models back over files today, so no data has been lost. But it blocks
a family of obvious next steps: a reqT formatter, a `tt` verb that edits `PRD.md` structurally, or any
"parse, modify, write back" tool. Each would corrupt prose progressively while leaving the model
parseable, which is the quiet-failure shape this project treats as the dangerous one.

It also bounds a design idea recorded separately: markdown documents carrying reqT fragments. Round
tripping those is only safe if prose spans are preserved byte for byte rather than re-rendered.

## Acceptance sketch

* `norm(norm(x)) == norm(x)` for every file in `reqts/`, as a property test, not a spot check.
* Prose that contains no reqT construct survives a round trip **byte for byte**. Preserving the
  original span is a cheaper and more honest route to this than making the prose renderer perfect.
* Emphasis markers, inline code and indentation are never consumed by a pass.
* Where identity genuinely cannot hold, the tool says so rather than writing silently.

## Discussion

### Comment by bjornregnell/India at 2026-08-19

**Item 1 landed: the property is now a test** (`tools/test/reqt-roundtrip.test.scala`), marked
expected-to-fail so the suite stays green while the defect stands and turns red the day it is fixed.
Zero cascade, as this issue's own ordering argues.

Two corrections to the report above, both from re-measuring rather than re-reading:

1. **A third file drifts.** The non-idempotent set is `DESIGN.md`, `PRD.md`, `ROADMAP.md` — the table
   above measured only the latter two.
2. **The first drift is INDENTATION, not emphasis.** In `DESIGN.md` at rendered line 34 of pass 1:

   ```
   pass 1:   - Pointing `project.scala` at a different version changed what a single-file tool run …
   pass 2: - Pointing `project.scala` at a different version changed what a single-file tool run …
   ```

   Two leading spaces consumed. That matches this issue's own aside ("Indentation goes the same way")
   but puts it FIRST in the corpus rather than as a secondary symptom.

⚠ **And a correction to how the emphasis claim should be read.** A characterisation test asserting that
a bold span does not survive a round trip was written from this issue's table and **failed**: in
isolation, `* **agent capabilities** are the point` round-trips **byte for byte intact**. The
emphasis-eating in `PRD.md` is real, but it is CONTEXT-DEPENDENT — surrounding prose, line length, or a
second emphasis span on the same line — and no minimal reproducing specimen has been isolated. Nothing
in this issue is retracted; the caution is that "one emphasis marker consumed per pass" is a description
of one observed line, not a rule that holds for every bold span. Isolating the actual trigger belongs to
item 3, with the fix.

Agent disclosure: measured and written by an AI agent (Claude Opus 5) in session with the maintainer.

### Comment by bjornregnell/CO5 at 2026-07-27

Found while sketching how reqT-lang could parse ordinary markdown with reqT fragments scattered in it.
The probe was a throwaway script that parsed, rendered, re-parsed and re-rendered, then diffed; it is
easy to recreate from the table above and was not kept.

Upstream candidate. `tools/reqt-vendored/` is a pristine copy of reqT-lang's `src/main/scala`, kept
diff-clean precisely so verified fixes can go upstream as PRs, and BR's stated plan is to drop the
vendored copy after the alpha release and depend on a new reqT-lang release before the beta.

**The cascade is the real constraint, and it sorts the work.** reqT-lang is a dependency of other
repos under the reqT organisation, notably the Swing desktop tool, so a change here becomes release
and documentation work over there. That is the stated reason the copy was in-sourced in the first
place (see the header of `tools/parsereqt.scala`). It does NOT block contributing, but it means
contributing in order of how much it forces on downstream:

1. **A failing property test — zero cascade.** It changes no behaviour and nothing downstream depends
   on it. It converts this report from a claim into something the build states. Best first move.
2. **Additive API, opt-in — near-zero cascade.** A fragments mode, source positions, byte-preserved
   prose spans. Downstream keeps calling what it calls today and sees no change.
3. **Fixing the emphasis-eating itself — needs coordination.** It changes what `toMarkdown` emits, so
   any downstream that renders models to markdown gets different output, and its docs and screenshots
   may move with it. Worth doing, worth timing with a desktop release rather than dropping on them.

Since dropping the vendored copy requires reqT-lang to carry everything genscalator needs, item 2 is
the one that actually gates de-vendoring, and item 1 is the cheapest way to start the conversation
upstream with evidence attached.
