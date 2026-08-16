# Issue 032: the in-repo issue workflow has no typed verb, so closing an issue is hand-executed prose

> status: closed 2026-08-16, fixed by `7add972` · labels: toolbox, reqts, process, agent-trust · summary: `reqts/issues/README.md`
> defines closing as "rewrite the status line, then `git mv` from `open/` to `closed/`", and nothing
> in the toolbox does it. Six issues were closed by hand in the v0.10.2 sweep, with a bare `mv` per
> file, because `tt git`'s safe subset has no move and there is no issue verb.

## Description

Found 2026-08-13 during the v0.10.2 close sweep, immediately after issues 029 and 030 named the same
class of gap on the forge side. Three name-the-gap specimens in one session is itself the signal.

The repository defines its own issue workflow in prose, in `reqts/issues/README.md`:

* closing an issue is "`git mv` its file from `open/` to `closed/`" (line 21), because the directory
  **is** the open-or-closed state (line 20);
* the preamble's `> status:` line must agree with the directory and carries the finer state (lines 22-23);
* the next free number is "highest existing (across `open/` AND `closed/`) plus one" (lines 13-14),
  never reused;
* the file name is `issue-NNN-short-snake-case-name.md` (line 11).

Every one of those is a rule a tool could enforce and a human or agent currently has to remember. In this
sweep the sequence per issue was: read the status line, hand-edit it to `closed <date>, fixed by <hash>`,
then `mv` the file, then stage both paths so git records the rename. Six times. The `mv` is the least
interesting part: it is the **status-line rewrite that can silently disagree with the directory**, which
is the one invariant the README explicitly says must never break.

**Why a typed verb rather than a documented ritual.** `tt git`'s safe subset excludes move by design, and
correctly so, which means the documented workflow cannot be performed inside the lane at all. So the
toolbox pushes the caller to a raw shape for one of the repository's most routine operations, which is
issue 004's tripwire firing on our own process. The number rule is the sharper half: "next free is the
highest across both directories plus one" is exactly the computation a contributor gets wrong, and
`CONTRIBUTING.md:49` already anticipates the collision ("if two PRs claim the same NNN, the later one
renumbers"). A one-line answer to "what is the next free number" would prevent the renumbering rather
than adjudicate it.

## How to reproduce it

```
$ tt                      # 45 tools, no issue verb, no move verb
$ tt git                  # safe subset: add/commit/push/pull/fetch/show/log, no mv
```

Then try to close an issue without leaving the lane. There is no way.

## Acceptance sketch

A single verb named after the noun, not the file operation:

* `tt issue next [--repo <dir>]` prints the next free NNN, computed across `open/` and `closed/` exactly
  as the README defines it. Smallest, most-used, and useful to contributors as well as maintainers.
* `tt issue close <NNN> --fixed-by <ref> [--date <YYYY-MM-DD>]` rewrites the `> status:` preamble and
  moves the file, as one operation, so the two can never disagree. Preview by default with `--yes` to
  actuate, following the pattern `tt forge release-delete` established.
* `tt issue list [--state open|closed|all]` for the read side, one line per issue: number, state, labels,
  summary.
* It must refuse to close an issue whose file is not where the directory says it is, and it must not
  invent a date. Take the timestamp from the same source `tt chrono` uses.

Design question for whoever picks this up: whether the move primitive should also exist on its own (a
`tt files move`, or a preview-default `tt git mv`). The argument against is verb sprawl for a shape that
is only ever used here. The argument for is that `git mv` preserves rename detection in a way a bare `mv`
plus two `--add` paths only approximates. Recommend deciding it while implementing `tt issue close`
rather than in advance.

## Discussion

### Comment by bjornregnell/Opus5 at 2026-08-13 16:02

Filed during the v0.10.2 close sweep, from the maintainer noticing the raw `mv` reach in-flight, exactly
as the same session's issues 029 and 030 were noticed. Recording the pattern because it is the useful
part: all three gaps were invisible while reading the toolbox and obvious the moment someone tried to
complete a maintainer workflow end to end inside the lane. Report 087 makes the general form of this point
about the effectful and outward lane being the untested one, and these three are its first instances.

Sequencing: `tt issue next` is trivial and can ship alone. `tt issue close` should wait for the move
question above to be settled. Grouped with 029 and 030 for the v0.10.3 wave as the process-tooling batch.

Agent disclosure: found and drafted by an AI agent (Claude Opus 5) in session with the maintainer, who
caught the raw reach and asked for the verb.
