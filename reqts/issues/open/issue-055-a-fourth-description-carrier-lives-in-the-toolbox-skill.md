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

### Comment by hmiddelk at 2026-09-14 — THE SURVEY

The count you asked for. Measured against `main` at `fb23f71`.

**Scope: the skill describes 6 of 46 verbs**, across 11 lines in the `## What's available` block
(`SKILL.md:25-35`). `tools/README.md` carries 46 `### <verb> —` headings, so 40 verbs are not named in
the skill at all. It is a curated list, not a catalogue — which matters for the choice of fix.

| verb | the skill's gloss (`SKILL.md`) | source of truth | classification? |
| --- | --- | --- | --- |
| `text` | six subcommand lines, e.g. `# grep -c` | `typed grep/awk/cut/uniq replacement (PURE)` | ✗ |
| `files` | `find / grep -l ; add --count for just the number` | `typed find / find\|wc / grep -l replacement (PURE)` | ✗ |
| `log` | `build/run-log analyzer: errors + warnings + verdict` | `build/run-log analyzer` **(PURE)** | ✗ |
| `verify` | `run-and-verify (effectful): run an allowed cmd, check exit/out, PASS/FAIL` | `run-and-verify driver (EFFECTFUL)` | **✓** |
| `gitinfo` | `branch, clean/dirty, ahead/behind — state/sync without raw git status` | `typed, READ-ONLY git status/overview (PURE, read-only)` | ✗ |
| `git` | `the typed git lane (see Command discipline)` | `safe git helper: commit-from-file, ff-pull, fetch, read-only show (EFFECTFUL, non-destructive)` | ✗ |

**The counts:**

* **6 of 46** verbs described.
* **6 of 6** worded independently — not one is the tagline verbatim.
* **1 of 6** carries the PURE/EFFECTFUL classification (`verify`, lowercased). **5 of 6 omit it.**
* **2 of 6** are verbs PR #14 projects (`text`, `log`) — and **both omit the marker**, so Phase 1
  landing does not reduce this to a hypothetical.
* `text` is a special case worth separating: the skill has no verb-level description of it at all, only
  six subcommand glosses. So there is nothing there to disagree with a tagline, and a
  compare-the-tagline gate would have to decide whether that counts as a miss. It should: the six
  subcommand lines are precisely where a reader forms their idea of what `text` is.

### Which of the three shapes the count supports

Restating the options from the sketch above, so this reads without scrolling back:

1. **Project it** — generate the skill's descriptions from the declaration, as the other three carriers
   now are, once Phase 2 exists.
2. **Gate it** — assert that every verb the skill names carries the same classification its declaration
   does, without requiring identical prose, the way `AbilitySuite` gates the README heading.
3. **Delete the descriptions** — have the skill point at `tools/README.md` instead of restating it.

**The data supports (2), gate it, and argues specifically against (3), delete.** My inclination before
counting was yours — (2) or (3) — and the count moved me off (3). The descriptions are **not
redundant**: five of the six carry information `tools/README.md`'s heading does not carry at that
granularity — `files`' `--count` hint, `verify`'s PASS/FAIL shape, `git`'s `--repo` flag form, and all
six `text` subcommands visible at one glance. That is teaching text aimed at an agent at turn zero, and
the 6-of-46 ratio shows it was curated rather than transcribed. Deleting it to fix a marker problem
would trade something that works for something that was never broken.

(1), project it, replaces that curated voice with a generated block — the objection you already raised,
and the 6-of-46 ratio strengthens it: a generator would either emit all 46 and bury the curation, or
need its own list of which 6 to emit, which is the enumeration issue 041 set out to remove.

(2), gate it, fixes exactly the broken half at the lowest cost: the prose stays hand-written and the
classification becomes checkable. At **1 of 6 passing**, such a gate would have real work to do on day
one rather than being a tripwire for a hypothetical.

### A second finding, which a classification gate would NOT catch

The skill's prose recommends verbs its own grants exclude. `allowed-tools` (`SKILL.md:4`) grants the
read-only four — `tt git log`, `tt git show`, `tt git diff`, `tt gitinfo` — after the 2026-08-25
narrowing recorded in the CHANGELOG. But `:35` advertises `tt git log|commit|push --repo <dir>`, and
`:44` instructs `tt git commit|push|pull|fetch ... --repo <dir>` as "the write subset". So four verbs
are recommended by a skill that does not grant them, under a section headed **"Command discipline
(keeps approvals rare)"** — the prose routes the agent into a confirmation prompt while explaining how
to avoid them.

That is this issue's drift class (the skill describing the toolbox in its own words) on a different
axis: grants rather than descriptions. It is arguably the more expensive half, because the
classification omissions make the skill *less informative* while this one makes it *actively
misleading* about what will run unattended. Flagged here rather than filed separately, since it is the
same file and the same cause; say the word if it should have its own number.

### Method, and its limits

Read `SKILL.md` in full (52 lines) rather than grepping it, counted the README headings mechanically,
and took the projected set from `ProjectedAbilities`. The six comparisons were then made BY HAND,
because "is this the same claim?" is semantic and 6 rows is well under the size where a tool pays for
itself. NOT verified: whether the same carrier exists in the other shipped skills — this survey covers
`skills/tt-toolbox/` only. If it does, the count above is a floor, exactly as issue 041's six was.

Agent disclosure: surveyed by an AI agent (Claude Opus 5) in session with me, at my request, and
reviewed by me. The agent read the two files, ran the heading count and produced the table; the reading
that gating beats deleting is its argument from the data, which I agree with.
