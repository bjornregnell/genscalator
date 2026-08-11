# Issue 024: `tt-toolbox` and `avoid-guard-stall` give CONTRADICTORY git guidance, and the agent follows the wrong one

> status: open · labels: skills, docs, git · summary: one skill says use bare `git -C <abs-path>`, the
> other says use `tt git ... --repo <dir>`. Both load in the same session, so the reflex the agent forms
> is a coin flip — observed twice in one session, corrected by the human both times.

## Description

Found 2026-08-11 in the second alpha field test (report 087), which used the toolbox as the only
sanctioned lane for a multi-hour task on a third-party Scala repo.

Three carriers speak to the git lane, and they do not agree:

| carrier | says |
|---|---|
| `skills/tt-toolbox`, *Command discipline* | **"For git, use bare `git -C <abs-path> <subcmd>`."** |
| `skills/avoid-guard-stall`, tripping-shapes table | **"For git, `tt git ... --repo <dir>`"**, memory-linked `commit-via-tt-git-not-raw-cd-git` |
| `docs/guard-clean-digest.txt` | "never raw git status for STATE/SYNC" (quoted in issue 004) |

Two of three point at the typed verbs; the dissenting one is `tt-toolbox` — the skill whose entire job is
*which tool to reach for*, and the one a caller consults precisely when forming that reflex.

**Observed cost.** In the field-test session both skills were loaded at the start. The agent reached for
`git -C <repo> log` and then `git -C <repo> remote -v`; the human rejected both, with
*"I want to use tt git as well as described in the genscalator plugin"* and then
*"use tt git for effectull ops"*. Two corrections, a re-plan, and a wrong first instinct on the very lane
the skills exist to shape. The agent was not ignoring its instructions — it was following one of them.

**Why this is distinct from issue 004.** They are counterparts, not duplicates. 004 is a *missing verb*
producing a justified raw reach (`tt gitinfo` gives a count, not paths, so there is no typed shape for
"which files changed"). This is a *present verb* with documentation pointing away from it: `tt git log`
and `tt gitinfo` both exist and both did the job once the agent was redirected. 004 says the raw reach is
a tripwire signalling a gap; 024 says the raw reach can also be simple mis-instruction, which makes the
tripwire noisier and 004's signal harder to trust.

**Why it survived two earlier field tests.** Reports 085 and 086 mostly *read* with the toolbox. A task
that has to branch, inspect, commit and push is the first one where the git lane is load-bearing, and the
contradiction then shows up immediately.

## How to reproduce it

Read the two shipped skills:

```
$ tt text grepr <plugin-cache>/skills md "For git"
skills/tt-toolbox/SKILL.md:  - For git, use bare `git -C <abs-path> <subcmd>`.
skills/avoid-guard-stall/SKILL.md:  ... For git, `tt git ... --repo <dir>` [[commit-via-tt-git-not-raw-cd-git]]
```

Behavioural reproduction needs an agent session with both skills loaded and a task that requires git
history or status; the raw reach is the observable.

## Acceptance sketch

* `tt-toolbox`'s Command-discipline bullet points at the typed verbs: `tt gitinfo <repo>` for
  branch/state/sync, `tt git log --repo <dir>` for history search, `tt git commit|push|pull|fetch` for the
  write subset — and names bare `git -C` as the fallback **only** for the shapes the safe subset does not
  cover (see issue 026), so the exception is documented rather than contradictory.
* Ideally one sentence stating why, since a rule with a reason survives a compaction better than a bare
  prohibition: the typed path is auditable, takes its message from a file, and excludes the destructive
  verbs.
* A cheap structural guard against recurrence: a test or lint that greps the shipped skills for
  `git -C` / `git status` / `git log` outside a marked fallback block. The skills are the one place the
  compiler cannot check, which is exactly why they drift.

## Discussion

### Comment by hmiddelk/Opus5 at 2026-08-11 15:01

Filed from the second alpha field test (report 087) — a use-it-for-real pass rather than a surface sweep,
which is why this is the finding that pass produced and the two earlier ones did not.

Worth flagging for triage alongside issue 026: the honest fix depends on what the safe subset is going to
cover. If branch creation and `--set-upstream` land, `tt-toolbox` can say "typed verbs, no exceptions"; if
they do not, it has to say "typed verbs, except these named shapes" — and leaving it as today's flat
"use bare `git -C`" is the one option that is wrong either way.

Agent disclosure: found and drafted by an AI agent (Claude Opus 5) under human direction; the human
reviewed and submitted. The agent is also the specimen — it is the thing that followed the wrong rule.
