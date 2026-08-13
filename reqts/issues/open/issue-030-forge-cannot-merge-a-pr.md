# Issue 030: `tt forge` cannot merge a pull request, so every merge leaves the lane

> status: open · labels: toolbox, forge, ergonomics · summary: `tt forge` can read a PR from four
> angles but cannot merge one, so the last step of every contribution is raw `gh pr merge`. Found
> while merging PR 3, alongside issue 029.

## Description

Found 2026-08-13 while merging PR 3 (issues 024-028 and report 087).

The `pr-*` family is read-only: `prs`, `pr`, `pr-files`, `pr-diff`, and with issue 029, `pr-commits`. The
merge itself has no typed shape, so it was done with raw `gh pr merge 3 --merge --subject ... --body ...`.

This is the same family as issue 026 (`tt git` can finish a PR workflow but cannot start one) seen from
the maintainer's side rather than the contributor's: the toolbox covers the middle of the contribution
workflow and neither end. Creating a PR has no verb either, which is worth recording here but is a
separate ask.

**The specific value a typed verb would add, beyond avoiding the raw reach.** `CONTRIBUTING.md:81`
requires that *"the merge commit message names the PR (number + title), so mirrored history carries the
cross-reference"*. That subject line is composed by hand today, from a title the caller has to fetch
separately, which is exactly the kind of clerical step that gets skipped or mistyped under load. A verb
that composes it from the PR metadata makes the convention automatic instead of remembered.

**Precedent for the safety shape already exists in `tt forge`.** `release-delete` previews by default and
requires `--yes` to actuate, and it states plainly what it will not touch. A merge is effectful and
outward-facing in the same way, and should follow the same pattern rather than acting on a bare
invocation.

## How to reproduce it

```
$ tt forge                                    # usage: no pr-merge verb
$ tt forge pr-merge bjornregnell/genscalator 3 --gh
```

## Acceptance sketch

* `tt forge pr-merge <owner>/<repo> <n> [--gh | --url BASE] [--method merge|squash|rebase] [--yes]`.
* **Previews by default**, in the shape `release-delete` established: print the PR number, title, author,
  head to base branches, mergeable state, the changed-file count, and the exact merge subject that would
  be written, then stop unless `--yes` is given.
* Compose the merge subject as `Merge PR #<n>: <title>` by default, satisfying `CONTRIBUTING.md:81`
  without the caller retyping the title, with `--subject` and `--body-file` to override. Take the body
  from a file rather than a flag, matching `tt git commit --message-file`, so prose with shell
  metacharacters never reaches a command line.
* Refuse to actuate when the forge reports the PR as not mergeable, and say which state it is in rather
  than passing the forge's error through raw.
* Do **not** delete the source branch implicitly. That is a separate destructive act and belongs behind
  its own flag, if at all.

## Discussion

### Comment by bjornregnell/Opus5 at 2026-08-13 16:02

Filed at the merge of PR 3, together with issue 029.

Triage note from the maintainer side: this is the more invasive of the two, since it is the first
effectful verb in the `pr-*` family and needs the preview-and-confirm design settled before it is written.
029 is additive and read-only and should not wait for it.

One decision to make when this is scoped: whether `--method` should default to `merge` (a merge commit,
which is what `CONTRIBUTING.md:81`'s naming rule assumes) or whether squash should be offered at all,
given that a squash discards the contributor's commit boundaries and, with them, the per-commit
attribution that issue 029 exists to check.

Agent disclosure: found and drafted by an AI agent (Claude Opus 5) in session with the maintainer, who
reviewed it.
