# Issue 024: `tt-toolbox` and `avoid-guard-stall` give CONTRADICTORY git guidance, and the agent follows the wrong one

> status: closed 2026-08-16, fixed by `7add972` · labels: skills, docs, git · summary: one skill says use bare `git -C <abs-path>`, the
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

### Comment by bjornregnell/Opus5 at 2026-08-13 15:59

Maintainer-side review (PR 3 triage), verified against `main` at `542b2fd` by a dedicated review agent.

**CONFIRMED, and the spread is wider than filed: nine carriers, not three.** Both quoted lines are
verbatim correct (`skills/tt-toolbox/SKILL.md:41`, `skills/avoid-guard-stall/SKILL.md:40`). Six further
carriers repeat the `git -C` advice, and two of them outrank both skills:

* `AGENTS.md:39` says it too. That is the operating contract, the file a vendored copy carries into other
  repos, version-pinned and test-guarded (`tools/test/version.test.scala:52`). It never mentions
  `tt git` or `tt gitinfo`.
* `tools/guardcheck.scala:76` is the guard's own fix text: *"use `git -C <abs>` for git; pass absolute
  paths; never cd-then-chain"*. By `avoid-guard-stall`'s own "where the fix must live" table, the guard is
  the one carrier that survives a warp, so it teaches the raw shape at the exact instant of failure.
* `tools/test/cli.test.scala:1038-1041` is a passing test asserting that `guardcheck cmd
  "git -C /tmp/x status"` is clean. The suite certifies as guard-clean the very command the digest names
  as the reflex slip.
* Also: `docs/confirmations-method.md:59-62` (a whole section prescribing it, with `git -C <path> commit
  -m "..."` as the worked example), `docs/allowlist.md:49,57`, `docs/foundations.md:595`, and
  `skills/scala-code-review/SKILL.md:4,21,26`. The last is legitimate, since `tt git` has no `diff` verb.

**One correction to the framing, which sharpens rather than weakens it.** This is not a logical
contradiction. Read in context, both lines forbid `cd && git`, and they differ only in the replacement
they name: `avoid-guard-stall:40` sits in the `cd` row and answers "how do I run git in another directory
without `cd`". The precise defect is that `tt-toolbox:41` is unconditional and names no typed verb, so it
reads as blanket permission covering `status` and `log`, where the digest does forbid the raw shape. "Coin
flip" overstates it, "one carrier omits the typed default" is exact, and the observed cost stands either
way.

**The reconciled wording already exists in-tree.** `docs/EMBER-EXAMPLE.md:70` carries the
typed-first-plus-marked-fallback shape the acceptance sketch asks for, so the fix is largely a copy edit
toward our own exemplar rather than new drafting.

**A coupling worth naming before the lint is written.** The most common raw-git call is `git status`,
which issue 004 explicitly licenses as a justified reach and `cli.test.scala:1039` certifies as clean. A
blanket grep would flag exactly that, plus the three legitimate `scala-code-review` mentions. The lint has
to assert "every raw-git mention names the typed verb first or sits in a marked fallback block", not "no
raw git", and it is cheapest after 004 lands a typed status shape.

**Triage: accepted, split three ways.** The five documentation carriers ship in the v0.10.3 wave. The two
`tools/*.scala` carriers (the guardcheck fix text, and the test literal changed to a shape with no typed
equivalent such as `git -C <abs> diff`) ship with them, because a doc-only fix is precisely the class the
guard-stall skill says rots. The lint waits on 004; its natural home is
`tools/test/version.test.scala`, the only suite that already lints real repo carriers for cross-file
agreement.

Two adjacent findings from the same review, recorded here so they are not lost: `tools/guardcheck.scala:150`
still claims `git log --grep` is "explicitly sanctioned, commit-log search has no typed verb (SM217)",
which stopped being true when `tt git log` shipped on 2026-07-28, so the guard is now the stale carrier;
and `skills/tt-toolbox`'s own `allowed-tools` (line 4) grants no git at all, so obeying its advice under a
Tier-1 allowlist produces the very prompt the skill exists to prevent.

Thank you for this one. It is the finding a surface sweep structurally cannot produce, and the point that
mis-instruction adds noise to 004's tripwire is the part that changed how we are sequencing the fix.
