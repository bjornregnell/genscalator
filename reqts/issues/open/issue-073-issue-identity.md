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
3. **G3 File-name order is inception order, by day.** Sorting the issue files by name (plain
   lexicographic, as `ls` and every file browser do) lists them in the order they were conceived, to
   the day; issues conceived on the same day sort by slug. This keeps "what came first" readable
   without opening files, as the numbered scheme does today.
4. **G4 Human-meaningful.** An ID says roughly what the issue is about, so a reference in prose or a
   commit message can be understood without looking it up.
5. **G5 Legacy coexistence.** Issues 000 to 073 keep their numbers (IDs are never reused or
   renumbered). Old and new IDs live side by side, and tools and links handle both.
6. **G6 Tool-minted.** A `tt issue` verb mints the ID from the clock, so neither a human nor an agent
   composes one by hand.

## Decision (2026-10-11)

**`issue-YYMMDD-slug.md`**, e.g. `issue-261011-issue-identity.md`, cited as `261011-issue-identity`.

* **ID = UTC date + slug.** `YYMMDD` is the UTC date the issue was conceived, minted by the tool. The
  slug is 1 to 4 kebab-case words, matching the repo's other file names, and is frozen at inception
  even if the title changes later.
* **No time of day, no sequence number, no initials.** A clash needs the same UTC day and the same slug,
  and it is loud, never silent: `tt issue new` refuses locally because the file exists, and two
  branches adding the same path conflict at merge, where renaming one slug fixes it. A per-day
  sequence number (`01`) would bring back a small counter, which G1 rules out. Initials make the ID
  longer and repeat what the preamble and git already record.
* **Same-day order is by slug** (G3 as amended). Few issues are conceived on the same day, so this is
  an acceptable approximation, and simpler than a time component.
* **UTC, not local time,** so that one clock orders every collaborator's issues. It only differs from
  the box's local date near midnight, but it costs nothing when the tool mints it. Note: `tt chrono` has no
  UTC option (it silently ignores an unknown `--utc` and prints local time, which was first mistaken
  for a broken conversion), so the minting code computes the UTC date itself with `java.time`.
* **Numeric date, not month names:** `okt` would sort `apr` < `aug` < `dec`, and is Swedish in a repo
  whose public text is English. `YYMMDD` also matches the session names `tt session` already derives.
* **Cite the full ID**, date and slug. The bare date is not unique, so a citation always carries the
  slug, which is what makes it readable (G4).
* **Cutover: 073 is the last numbered issue.** Legacy files start with `0`, new ones with `2`, so plain
  sorting gives two blocks, legacy first (G5), with no special case in any tool.
* **`tt issue list` shows each issue's age**, derived from the ID. Nice to have, not a goal.
* **Out of scope:** the forge's own issue numbers (Issue Zero on GitHub), and the single-writer counters
  in the closed work repo (SM, WR, RT). Docs should say so, so that nobody migrates them by mistake.

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

### Comment by bjornregnell at 2026-10-11 — DECISION

Decided as recorded in `## Decision` above. The first candidate here had a time of day (`HHMM`) in the
ID; I dropped it for simplicity. The slug makes same-day clashes unlikely, a clash is loud when it
happens, and ordering same-day issues by slug is good enough given how few there are. G3 is amended to
say ordering is by day. UTC date, minted by `tt issue new`. Implementation follows in one PR: the tool
change and the doc updates together, with tests for mixed legacy and new sorting, a same-day same-slug
refusal, a same-day different-slug success, and a UTC date on a box whose local zone is not UTC.

Agent disclosure: recorded by an AI agent (Claude Opus 5.5) from my decisions in the same session.
