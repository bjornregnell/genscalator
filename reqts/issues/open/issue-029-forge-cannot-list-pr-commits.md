# Issue 029: `tt forge` cannot list a PR's commits, so a rule we wrote down cannot be checked inside the lane

> status: open · labels: toolbox, forge, contributing, agent-trust · summary: `tt forge` has four
> read verbs for pull requests but none that lists commits, so the `CONTRIBUTING.md` pre-merge check
> for assistant-credit trailers can only be done with raw `gh`. Found while merging PR 3.

## Description

Found 2026-08-13 while merging PR 3 (issues 024-028 and report 087).

`CONTRIBUTING.md:63` states a house rule for contributors: *"No assistant credit in commits. Do not add
`Co-Authored-By: <assistant>` trailers or 'Generated with ...'"*. Verifying that rule before a merge means
reading the branch's commit messages. `tt forge` cannot do it:

```
forge prs      <owner>/<repo>            list PRs
forge pr       <owner>/<repo> <n>        merge state + body
forge pr-files <owner>/<repo> <n>        changed files: status, +/-, path
forge pr-diff  <owner>/<repo> <n>        raw unified diff
```

All four are read verbs, and none of them shows commits. The diff shows what changed, not who claimed
credit for it or in what message. So the check was done with raw `gh pr view 3 --json commits`, which
returns the whole commit array as unfiltered JSON.

**Why this is worth a verb rather than a shrug.** The tripwire rule (issue 004) says reaching for a raw
shape is the signal that a typed verb is missing. Here the reach was forced by our own contribution
policy: the repository states a rule, and the toolbox cannot check it. A rule that can only be enforced
outside the lane is a rule the lane does not carry, and in an agent session it is the rule most likely to
be skipped, because skipping it is invisible.

It also fits the existing family exactly. `pr-files` and `pr-diff` already exist for review purposes and
this is the same shape with the same arguments, so the gap reads as an omission rather than a decision.

## How to reproduce it

```
$ tt forge                                   # usage: no pr-commits verb
$ tt forge pr-commits bjornregnell/genscalator 3 --gh
```

## Acceptance sketch

* `tt forge pr-commits <owner>/<repo> <n> [--gh | --url BASE] [--limit N]`, printing one line per commit:
  short sha, author name and email, ISO date, and the headline, matching the tab-separated shape the
  other `forge` list verbs already use.
* A `--trailers` flag (or trailer output by default) that surfaces `Co-Authored-By:` and similar message
  trailers, since those are the reason the verb is wanted. Printing the full body of every commit would
  bury the signal.
* Ideally a one-line summary the caller can act on, in the spirit of `tt log`'s verdict line, e.g.
  `29 commits, 0 assistant-credit trailers` versus a named list of the offenders. That turns the
  `CONTRIBUTING.md` rule into something a maintainer can check in one allowlisted call.
* Read-only, like the rest of the `pr-*` family.

## Discussion

### Comment by bjornregnell/Opus5 at 2026-08-13 16:02

Filed at the merge of PR 3, together with issue 030. Both were found the same way: by trying to complete
a maintainer workflow inside the lane and failing at the last two steps.

Sequencing note. This one is a small, purely additive read verb in an existing family, so it can ship
independently of 030 and of the `tt git` work in issue 026. Of the three it is the one with a policy
argument behind it rather than only ergonomics, which is why it is filed first.

Agent disclosure: found and drafted by an AI agent (Claude Opus 5) in session with the maintainer, who
reviewed it.
