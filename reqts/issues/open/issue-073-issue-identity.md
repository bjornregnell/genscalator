# Issue 073: issue identity — make in-repo issue IDs unique without coordination, stable from inception, and sorted by inception

> status: open 2026-10-11 · labels: meta, issues, process, docs, toolbox · measured against: v0.10.2
> (from `VERSION.txt`) at `6a905b8`, Linux · summary: in-repo issues are numbered by a global
> counter (highest across `open/` and `closed/`, plus one). That counter cannot see drafts or unmerged
> PRs, so numbers get renumbered at filing and the old numbers survive inside issue texts as wrong
> cross-references. This meta issue states the goals for a new identity scheme before any is chosen.
> It was itself meant to be 072, which an issue filed earlier the same day had already taken.

## Description

### The churn, observed

All within two weeks:

* **Drafts renumbered at filing.** Issues 068, 070 and 071 were drafted as 026, 027 and 028. Filing
  renumbered the files but not the references inside them, so the texts cited the wrong issues: 071
  said "027" in seven places, and main's 027 is an unrelated closed issue about a stack trace. Found by
  review on 2026-10-11, fixed by hand in PR #28.
* **Numbers held by unmerged PRs are invisible.** `tt issue next` scans the local `open/` and `closed/`.
  With PRs #27 and #28 open, it would have handed out 067, which those PRs already use.
  `reqts/issues/README.md` warns about this, and the warning is all there is: the check is manual, a
  forge API call. Issue 072 avoided the clash only because the agent knew of the open PRs.
* **This issue.** The maintainer asked for "issue-072-issue-identity"; 072 had been filed earlier
  the same day. With two or more people (or agents) filing in parallel, a single counter needs coordination
  it does not have.

### Why a counter is the wrong shape here

A monotonic counter is fine with one writer (the closed work repo's PIN-BOARD counters stay as they
are). This repo has several writers, each working on a branch, and an issue's identity is needed from
the moment it is conceived, long before it reaches `main`. The counter is only known at merge time, so
every issue drafted on a branch carries a provisional number, and provisional numbers leak.

## Goals (to be met by whatever scheme is chosen)

1. **G1 Uniqueness without coordination.** Any writer, on any branch or fork, offline, can mint an ID
   that will not collide with any other writer's, without consulting the forge, other branches or open
   PRs.
2. **G2 Stable from inception.** An issue gets its ID when it is first written (as a draft, on a
   branch) and keeps it forever: through filing, merge, review, retitling and closing. No step ever
   renumbers an issue, so a reference written on day one stays correct.
3. **G3 File-name order is inception order.** Sorting the issue files by name (plain lexicographic, as
   `ls` and every file browser do) lists them in the order they were conceived. This keeps "what came
   first" readable without opening files, as the numbered scheme does today.
4. **G4 Human-meaningful.** An ID says roughly what the issue is about, so a reference in prose or a
   commit message can be understood without looking it up.
5. **G5 Legacy coexistence.** Issues 000 to 073 keep their numbers (IDs are never reused or
   renumbered). Old and new IDs live side by side, and tools and links handle both.
6. **G6 Tool-minted.** A `tt issue` verb mints the ID from the clock, so neither a human nor an agent
   composes one by hand.

## A candidate, for discussion (not decided)

`issue-YYMMDD-HHMM-slug.md`, e.g. `issue-261011-1830-issue-identity.md`, cited as `261011-1830-issue-identity`:

* G1: date, time and slug together collide only if two writers start the same topic in the same minute.
* G2: minted at inception, never changed; the slug stays frozen even if the title changes.
* G3: `YYMMDD-HHMM` sorts lexicographically in time order. Numeric, not month names (`okt` would sort
  `apr` < `aug` < `dec`, and is Swedish in a repo whose public text is English).
* G4: the slug, 1 to 4 kebab-case words, matching the repo's other file names.
* G5: legacy files start with three digits, new ones with six, so the two forms sort as two blocks,
  legacy first.
* An earlier idea used a per-day sequence number (`01`). That reintroduces a small counter and a small
  collision window, which G1 rules out; the time does the same job with no coordination.

Open questions: is minute resolution enough, or should the writer's initials be appended; does
`HHMM` use UTC or local time (UTC avoids a two-hour ambiguity across collaborators); and should
`tt issue list` show the age of an issue, which the ID now carries.

## What would change

* `reqts/issues/README.md`, `CONTRIBUTING.md` and issue 000: the numbering rule and how to cite.
* `tt issue`: `next` replaced or joined by `new <slug>`, which mints the ID; `list` and `close`
  accept both forms.
* `docs/EMBER-TEMPLATE.md`: the "next free IDs" hint for issues becomes unnecessary.
* The toolbox skill text that mentions `tt issue next`.

## Discussion

### Comment by bjornregnell at 2026-10-11

Proposed after a day in which issue numbers caused rework three times (the 026/027/028 draft numbers in
PR #28, the 067 near-miss, and this issue's own 072). Goals first; the scheme above is a candidate to
argue with, not a decision.

Agent disclosure: drafted by an AI agent (Claude Opus 5.5) in session with me, from my proposal and its
review in the same session; reviewed by me.
