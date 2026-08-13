# Issue 031: `tt files` reports `0 files` and exits 0 for a root that does not exist

> status: open · labels: toolbox, files, find, agent-trust, correctness · summary: a missing root is
> indistinguishable from an empty result, so a typo in a path reads as a true negative. `tt find` on
> the same input correctly exits 2, so the sibling walkers disagree.

## Description

Found 2026-08-13 while verifying issue 027 (which is about the opposite failure, a raw stack trace where a
clean message belongs). The two are the same design question answered inconsistently in adjacent code.

```
$ tt files /no/such/dir scala
0 files
$ echo exit=$?
exit=0

$ tt find /no/such/dir --type f
find: no such path: /no/such/dir
$ echo exit=$?
exit=2
```

`Lib.walkPruned`'s `visitFileFailed` returns `CONTINUE` (`lib.scala:188-189`), which is right for an
unreadable file encountered part-way through a walk, and wrong for the walk's root. The result is that
`tt files` cannot distinguish "this directory contains no matching files" from "this directory does not
exist".

**Why this ranks above the stack trace in issue 027.** A trace is loud, ugly and immediately understood as
a caller error. This is a **wrong answer that looks like a right one**. The failure mode is a search that
silently covers nothing: a mistyped path, a stale absolute path after a repo move, or a `--exclude` that
happens to be a directory name yields `0 files`, and both a human and an agent will read that as evidence
of absence. In an agent session it is worse than a crash, because a confident negative propagates into the
next decision, and nothing in the output invites a second look.

It also directly undercuts `tt files`'s stated job. The tool is the recommended replacement for `grep -rl`
in the guard-clean digest, so an agent that has been taught to reach for it inherits this failure mode
exactly where the alternative (raw `grep`) would have said `No such file or directory`.

**The fix is already written, one file away.** `tt find` validates its root and exits 2 with
`find: no such path: <path>`. The disagreement between the two walkers is the bug, and `find` is the side
that is right.

## How to reproduce it

```
$ tt files /no/such/dir scala          # 0 files, exit 0
$ tt files /no/such/dir md             # 0 files, exit 0
$ tt find  /no/such/dir --type f       # find: no such path, exit 2
```

Also worth checking with a root that exists but is a regular file rather than a directory, and with a root
that exists but is unreadable, since those are the other two shapes of the same question.

## Acceptance sketch

* `tt files` validates its root before walking and fails the way `tt find` already does: a one-line
  message on stderr and exit 2, with the same wording as `find` so the two verbs cannot drift again.
* A distinct message for "exists but is not a directory", matching the treatment issue 027 asks for on the
  read path.
* `visitFileFailed` keeps returning `CONTINUE` for entries encountered *during* the walk. The change is
  about the root only, and the distinction is worth a comment in the code, since the current behaviour is
  correct for the case it was written for.
* A test per shape in `tools/test/cli.test.scala`, alongside the existing `find` model
  (`cli.test.scala:451`), asserting exit 2 and the message. The current behaviour is exactly the kind a
  green suite never notices, because `0 files` is a perfectly plausible assertion result.
* Worth a sweep at the same time for any other caller of `Lib.walkPruned` that inherits the same root
  handling.

## Discussion

### Comment by bjornregnell/Opus5 at 2026-08-13 16:02

Filed from the maintainer review of PR 3, credited to report 087: found by the review agent verifying
issue 027, not by the field test itself, but it belongs to that report's batch and would not have been
found without it.

Triage: grouped with 027 for the v0.10.3 wave, since both are the same question (what does a file-taking
verb do when the path is wrong) and fixing them together is what stops the answers diverging a third time.
Reproduced independently in-session before filing.

Agent disclosure: found and drafted by an AI agent (Claude Opus 5) in session with the maintainer, who
reviewed it.
