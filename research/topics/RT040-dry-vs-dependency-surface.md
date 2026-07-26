# DRY vs dependency surface — when repetition is the right call

Status: **note** (2026-07-05, BR + agent). Companion to `skills/scala-style` §5 (the rule) and
`038-tt-shared-helper-file-pattern.md` (the *mechanism* for sharing when you do choose DRY).

## Question

**DRY (Don't Repeat Yourself) is the default — but when is a bit of repetition actually the better call?** genscalator
preaches typed reuse (one greppable, compiler-checked definition), yet BR flagged (2026-07-05) that repetition is
*sometimes* easier and more suitable — "decrease dependency surface, do a scratch test without touching production
code," etc. This note pins the tradeoff so "always DRY" doesn't become dogma.

## The default still holds: notice repetition → investigate a refactor

Duplicated logic **drifts out of sync**. Two "identical" parsers that quietly diverge is a **false-echt correctness
risk** (behaviour that *looks* the same but isn't). So real repetition should **trigger** a look at extracting the
shared thing into one typed, tested unit. **Live good case:** the sequence-diagram spec parser was extracted into
`tools/seqspec.scala` and shared by `svg` + `ascii` — the grammar lives in **one** place, so a future spec feature
lands in both tools and they *cannot* disagree. That is DRY earning its keep.

## Why DRY is a heuristic, not a law

Extracting a shared unit is **not free: it creates a dependency**, and coupling has its own cost. The saved lines can
be outweighed by the coupling they buy.

- **Dependency / coupling surface.** A shared helper couples *every* consumer to it: a change made to satisfy one
  can break another, and now they must be co-released and co-reasoned-about. `svg` and `ascii` sharing `seqspec` was
  worth it because they render **the same spec** — a *single decision* that genuinely should change in lockstep. Two
  tools that merely *look* similar are a different story.
- **Coincidental vs knowledge duplication** *(the load-bearing distinction).* DRY is really about not duplicating a
  **piece of knowledge / a decision**, not about textual similarity. Two code fragments that are identical **today
  but for unrelated reasons** (coincidental duplication) will want to change *independently* — merging them couples
  two things that aren't actually one, and the next change to one warps the other. Share **knowledge** duplication;
  leave **coincidental** duplication alone.
- **Test independence.** A test that re-states a small expected value or a tiny bit of logic **inline** is *more*
  trustworthy than one that **imports the production code** to compute its own expectation — the latter checks the
  code against *itself* (tautology). Deliberate test↔production duplication is a **feature**: it gives an
  independent oracle. (genscalator's CLI-contract tests already lean this way — they assert literal expected output,
  not "whatever the tool computes".)
- **Scratch / one-off code.** Sharing buys a scratch program nothing lasting — only an import and a rebuild
  dependency on production. Let it repeat and stay decoupled.
- **The abstraction isn't clear yet.** Premature extraction locks in the **wrong seam**, and *the wrong abstraction
  is costlier to undo than duplication* (you have to un-abstract every call site, often after they've grown apart).
  Rule of three: tolerate two copies; extract when the real boundary is obvious.

## Decision procedure

1. **Notice** the repetition (don't ignore it).
2. **Ask: is it the same *decision/knowledge*, or coincidental similarity?** Only the former is a DRY candidate.
3. If same-decision, **usually extract** (per §5 + the 038 shared-helper mechanism) — one typed source.
4. Otherwise **weigh duplication cost vs dependency/coupling cost**, and if you keep the repetition, **leave a
   one-line *why*** so the next reader knows it was a choice, not an oversight.

Neither blind DRY nor blind copy-paste. The genscalator posture (pragmatic, conscious, local tradeoffs) applied to
reuse itself.

## Related
- `skills/scala-style/SKILL.md` §5 (the actionable rule), `038-tt-shared-helper-file-pattern.md` (how to share when
  you do), the **echt / false-echt** concept in `docs/foundations.md` (why silently-diverging copies are a trust
  risk), and `036-references-refactor-plan.md` (a pending split-for-reuse decision that this lens applies to).
