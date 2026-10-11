# Issue 072: the sandboxed PR review cannot be done with `tt` alone, so every review reaches for raw git

> status: open 2026-10-11 · labels: toolbox, git, guard-integrity, review · measured against: v0.10.2
> (from `VERSION.txt`) at `5adb395`, Linux · summary: the project's PR-review practice is to build and
> test the MERGE RESULT of a PR in a throwaway clone. `tt git` deliberately has no clone, remote,
> refspec-fetch, checkout or merge verb, so a sub-agent doing that review falls back to raw
> `git -C <sandbox> ...` for five steps. Observed 2026-10-11 on the review of three introprog PRs.
> Note: issue numbers 067 to 071 are claimed by the open PRs #27 and #28, hence 072.

## Description

The review practice (sandbox clone, fetch the PR head, merge the default branch into it, test that)
is the right one: it tests what would actually land, and it checks the forge's `mergeable` claim
independently. On 2026-10-11 it found that introprog master was red on its own, which is what made
the three PRs' "9 within 9" claims explainable.

But the mechanics have no typed shape. The sandbox agent reported:

* `tt git ... remote add` and `tt git fetch` with PR refspecs (`pull/988/head`) stopped at usage text
* it then ran raw `git clone`, `git -C <sandbox> remote add`, `fetch <refspecs>`, `checkout -b` and
  `merge`, single commands, inside the sandbox only

Each is harmless *there*. The problem is the habit: the digest says "never raw git", the review
says "you must", and the agent has to decide per call which rule wins. That is the
missing-verb-causes-interpreter-reach pattern, with git instead of python.

`tt git`'s help states why merge, reset and friends are off: they can destroy work git cannot give
back. That reasoning is about a working clone. In a directory the tool itself just created, nothing
can be lost.

## Acceptance sketch

* One verb that owns the whole sandbox, so the destructive steps are scoped by construction, e.g.
  `tt forge pr-sandbox <owner>/<repo> <n> [<n>...] --dir <new-dir> [--gh]`: clone (from a local
  clone if given, else the forge), fetch the default branch and each PR head, and create one branch
  per PR plus one with all of them, each being the default branch with the PR merged in.
* Refuse if `--dir` already exists. That one rule is what makes merge safe here.
* Report per branch: merge clean or the conflicting paths, and the default-branch sha used.
* Out of scope: running the build or tests, which stay project-specific.
* Ties: the review workflow, issue 003 (lifting rituals into verbs), and the guard digest's
  "a missing verb is the signal" rule.

## Discussion

### Comment by bjornregnell at 2026-10-11

Filed from today's review of introprog #988, #991 and #992. Agent disclosure: drafted by an AI agent
(Claude Opus 5.5) in session with me, from the sandbox sub-agent's own report of the raw git calls it
made.
