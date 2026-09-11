# Issue 055: the toolbox skill is a fourth description carrier that issue 041 does not cover, and it is the one nothing watches

> status: open 2026-09-11 · labels: skills, docs, drift, agent-trust · measured against: v0.10.2 at
> `1dffcb3` · summary: issue 041 identified three carriers of a verb's description (the `tools/README.md`
> heading, the tool's `--help`, and its `case _ =>` usage block) and PR #14 projects all three from one
> declaration for six verbs. But `skills/tt-toolbox/SKILL.md` carries its own one-line description of
> each verb, written independently, and no test or gate relates it to anything. It is the same drift
> class as 041, outside 041's scope, and unlike the other three it has no owner.

## Description

`skills/tt-toolbox/SKILL.md:32` reads:

```
tt log <file>                     # build/run-log analyzer: errors + warnings + verdict
```

After PR #14, `tt log`'s projected tagline is `tt log — build/run-log analyzer (pure)`. The two agree on
the words "build/run-log analyzer" by coincidence of a careful author, and then diverge: the skill adds
"errors + warnings + verdict", which the declaration does not carry, and **omits the purity
classification**, which is the fact issue 041 argued is the most safety-relevant thing a verb's
description states.

That omission is the exact failure 041 documented in the `case _ =>` carrier: four of six verbs had dropped
PURE/EFFECTFUL there, and the reason it survived was that nothing read that path. The skill file is now in
the same position. Worse, its audience is an agent deciding which verb to reach for, so a missing or wrong
purity marker there is read by the consumer least able to notice it is missing.

**Why this is filed separately rather than folded into 041.** 041's scope is deliberately the three
carriers inside `tools/`, and PR #14's Phase 1 is deliberately six verbs. Widening either mid-flight is the
thing the Phase 1 decision was written to prevent. But the sweep that found six drifted verbs looked only
at `tools/*.scala`, so this carrier was never in the search space, and closing 041 would otherwise record
the problem as solved while one carrier remains unmeasured.

**What is not yet known**, and should be established before choosing a fix: how many verbs the skill
describes, and how many of those descriptions disagree with the projected tagline or omit the
classification. `tt log` is one confirmed instance found while reviewing PR #14; it is a specimen, not a
survey. The count is the thing that decides whether the answer is a gate, a projection, or a deletion.

Three shapes worth weighing once that count exists:

1. **Project it**, like the other three carriers, once Phase 2 exists. Cleanest, but the skill file is
   markdown prose with a deliberate teaching voice, and a generated block inside it may read worse than
   what a human writes.
2. **Gate it**, like `AbilitySuite` gates the README heading: assert that every verb the skill names
   carries the same classification the declaration does, without requiring identical prose. Weaker than
   projection, but it catches the safety-relevant half, and it is cheap.
3. **Stop describing verbs there at all**, and have the skill point at `tools/README.md`, which is already
   the source `gs help tt` reads. Removes the carrier instead of maintaining it. Worth considering,
   because the least-drifting description is the one that does not exist.

## Discussion

### Comment by bjornregnell at 2026-09-11 15:30

Surfaced during the review of PR #14 (agent-assisted). I said in that thread that I would rather this
became an issue than be forgotten, so here it is.

My inclination is option 2 or option 3 rather than 1: the skill is written to be read by an agent at turn
zero, and generated blocks are exactly the kind of text that gets skimmed past. But I would like the count
first. If it turns out only one or two entries drift, option 3 is nearly free; if most of them do, that is
itself the argument that the file should not be carrying these descriptions.

Note that this does not block closing issue 041 when Phase 1 lands. 041 is about the three carriers it
named, and it fixed them. This is the carrier nobody knew to look for, which is a different statement and
deserves its own number.
