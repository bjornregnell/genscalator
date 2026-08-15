# Issue 033: `guardcheck`'s output-shaping pipe check names three commands and misses `tee`

> status: open · labels: toolbox, guardcheck, docs · summary: the pipe check matches only
> `head|tail|wc`, so `| tee <file>` passes as clean, while our own documentation credits the guard
> with blocking it. Nothing was at risk, but a doc that overstates the guard is worse than one that
> understates it.

## Description

Found 2026-08-13 by the review agent verifying issue 025, and reproduced in-lane before filing.

`guardcheck.scala:91-93` is the output-shaping pipe check:

```scala
Check("MED", "pipe to head/tail/wc",
  "an output-SHAPING pipe a typed tool should absorb as a flag",
  "use the tool's --limit / --tail / --count flag instead of a pipe", has(raw"\|\s*(head|tail|wc)\b")),
```

The pattern is a closed list of three commands. `tee` is a fourth member of the same family and is not in
it, so a pipe to `tee` is reported clean:

```
$ tt guardcheck cmd "tt verify -- scala-cli test tools | tee /home/bjornr/tmp/x.log"
guardcheck [cmd]: clean — no guard-trip / reflex patterns found
```

**Nothing was ever at risk, and this is not a security hole.** The harness permission matcher still
prompts on an unallowlisted `tee`, so the shape is gated in practice. The defect is in what we *claim*.
Issue 025 was filed on the reasonable belief that "the guard rules correctly forbid `| tee`, `> file` and
`| tail`", and two of those three are true. An external contributor read our documentation, believed the
guard covered a shape it does not see, and reported it that way. That is the failure mode worth fixing:
**a guard that is credited with more coverage than it has trains the wrong confidence**, and the whole
point of the NOTE and MED tiers is that a caller can trust what they say.

The redirect check (`guardcheck.scala:185-187`) catches `> file`, so the `tee` case is the only member of
the family with no coverage at all. It is also the one an agent is most likely to reach for precisely when
a long-running command produces no visible progress, which is the situation issue 025 describes.

## How to reproduce it

```
$ tt guardcheck cmd "some-long-command | tee /abs/path/out.log"     # clean
$ tt guardcheck cmd "some-long-command | tail -5"                   # MED, pipe to head/tail/wc
$ tt guardcheck cmd "some-long-command > /abs/path/out.log"         # caught by the redirect check
```

## Acceptance sketch

* Add `tee` to the alternation at `guardcheck.scala:93`, keeping the tier at MED. One token.
* Consider whether the fix line should differ for `tee`: `head`/`tail`/`wc` are answered by a `--limit` or
  `--count` flag, whereas `tee` is answered by `run_in_background` or a file-sink flag, which is the
  advice `guardcheck.scala:187` already gives for `>`. A caller who piped to `tee` wants the output kept,
  not shortened.
* A test alongside the existing guardcheck cases in `tools/test/cli.test.scala`, asserting the MED verdict
  for the `tee` shape.
* Worth one pass over the other closed-list patterns in the same file while here, asking of each whether
  it enumerates a family or a complete set. This one read as complete and was not.

## Discussion

### Comment by bjornregnell/Opus5 at 2026-08-15 20:20

Filed from the PR 3 review round, where it was found as a side effect of checking a claim in issue 025
rather than by looking at the guard. Recorded in 025's review comment at the time and promoted to its own
issue on the maintainer's instruction, since it is a defect in a different tool than the one 025 is about.

Related and deliberately kept separate: issue 025 asks for `tt verify --tee`, which would remove most of
the reason to reach for a shell `tee` in the first place. Fixing this check does not depend on that, and
should not wait for it.

Agent disclosure: found by an AI agent (Claude Opus 5) during the PR 3 review, reproduced and drafted in
session with the maintainer, who reviewed it.
