# Issue 034: `guardcheck` still says `git log --grep` has no typed verb, two weeks after `tt git log` shipped

> status: closed 2026-08-16, fixed by `7add972` · labels: toolbox, guardcheck, docs, agent-trust · summary: `guardcheck.scala:150-151`
> justifies its raw-reach anchor by claiming commit-log search "still has no typed verb (SM217)".
> `tt git log` shipped 2026-07-28. The guard is the one carrier that survives a warp, so a stale
> claim there is the most expensive kind.

## Description

Found 2026-08-13 by the review agent verifying issue 024, and confirmed in-lane before filing.

`guardcheck.scala:150-151`, inside the comment justifying the leading-anchor design of the raw-reach
check:

```scala
//   - `git log --grep=<x>` is EXPLICITLY sanctioned — commit-log search still has no typed verb
//     (SM217) — and never appears first;
```

`tt git log --repo <dir> [--grep P] [--co-author P] [--author P] [--committer P] [--since D] [--limit N]`
has existed since 2026-07-28. The guard-clean digest already records this exact staleness being corrected
in its own text ("this line said 'no typed shape yet' until 2026-07-28; the verb existed and the digest
was stale"). The same correction never reached `guardcheck.scala`, so the claim has simply moved from one
carrier to another.

**Why this is more than a stale comment.** Three reasons, and they compound:

* **The guard is the carrier of last resort.** By `skills/avoid-guard-stall`'s own "where the fix must
  live" table, the guard is the only carrier that survives a warp or a compaction. A rule that rots in a
  skill gets re-armed from the digest at cold start; a rule that rots in the guard is what an agent reads
  at the moment of failure, with no other context loaded.
* **It licenses the exact reach the toolbox has retired.** The comment is not idle prose: it explains why
  the check deliberately does not fire on `git log --grep`. An agent reading it learns that raw
  commit-log search is sanctioned, which was true and is not.
* **It is the same defect class as issue 024**, filed by the contributor in the same batch: documentation
  pointing away from a verb that exists. 024 covers the skills and `AGENTS.md`; this is the code-side
  instance, and 024's fix scope already names it. Filed separately so the guard fix is not blocked behind
  024's lint design, which is itself gated on issue 004.

## How to reproduce it

```
$ tt text match /abs/genscalator/tools/guardcheck.scala 'SM217'
$ tt git log --repo /abs/genscalator --grep 'issue-012'    # the verb the comment says does not exist
```

## Acceptance sketch

* Rewrite `guardcheck.scala:150-151` to state the current position: `tt git log --repo <dir> --grep <p>`
  is the typed shape, and say why the check still does not fire on a raw `git log --grep` if that remains
  the intended behaviour (the anchor reasoning at `:139-155` may well stand on its own without the false
  premise).
* Decide, while there, whether raw `git log --grep` should now trip a NOTE-tier check, given that the
  typed verb exists. Tighten-never-loosen argues yes, but NOTE tier only, so nothing can stall.
* Sweep the rest of `guardcheck.scala`'s comments for other claims of the form "X still has no typed
  verb". The file reasons about the toolbox and will drift again as verbs land. Two candidates already
  known: this one, and the `git -C` fix text at `:76` that issue 024 covers.
* Cheap structural guard worth considering with 024's lint: any comment asserting that a verb does not
  exist is checkable against the dispatcher's own tool table.

## Discussion

### Comment by bjornregnell/Opus5 at 2026-08-15 20:20

Filed from the PR 3 review round. Found while verifying issue 024's carrier count, recorded in 024's
review comment, and promoted to its own issue on the maintainer's instruction so it can ship with the
guard fix rather than waiting on 024's lint.

The pattern is worth naming for triage: this is the second time this exact claim has gone stale in a
carrier, the digest being the first. A fact that has rotted twice in two places is a fact that wants a
test, not a third correction.

Agent disclosure: found by an AI agent (Claude Opus 5) during the PR 3 review, confirmed and drafted in
session with the maintainer, who reviewed it.
