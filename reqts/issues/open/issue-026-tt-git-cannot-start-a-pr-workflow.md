# Issue 026: `tt git` can finish a PR workflow but cannot start one — no branch verb, no `--set-upstream`, no `--remote` on `fetch`

> status: open · labels: toolbox, git, ergonomics · summary: three gaps hit in sequence while opening one
> PR. None is a destructive verb, so none is excluded by the safe-subset principle — but together they
> mean every PR begins outside the lane, which is the habit the lane exists to build.

## Description

Found 2026-08-11 in the second alpha field test (report 087), which required opening a real PR against a
third-party repo from a fork.

`tt git` in `v0.10.1` offers `commit / push / pull / fetch / show / log`, deliberately excluding the
destructive set (`reset / rebase / force / rm / clean / merge`). That exclusion is well judged. But the
subset is missing three *non-destructive* shapes, and a fork-based PR hits all three in a row:

**1. No branch verb.** The workflow starts by branching; there is no `tt git switch` / `branch`, so
`git switch -c option-d-repl-phase3` was typed by hand. Report 085 already recorded this under *Notes for
the next field test* — "branch creation and commit amend are absent from `tt git`… Not filed as an issue
pending a view on whether they belong." This is that view from a second data point: **it belongs.**
`git switch -c` creates a ref; it mutates nothing and destroys nothing.

**2. `push` cannot set an upstream — and this one is a DELIBERATE POLICY, not an oversight.** With the
branch created, the obvious next call fails:

```
$ tt git push --repo <abs-repo> --remote origin
git: git push origin failed:
fatal: The current branch option-d-repl-phase3 has no upstream branch.
To push the current branch and set the remote as upstream, use
    git push --set-upstream origin option-d-repl-phase3
```

**Correction to an earlier draft of this issue, which called this a gap.** `tt git --help` states the
position explicitly:

> A branch with no upstream in a single-remote repo is refused by git itself (push.default simple) — set
> the upstream once with `git push -u`; **the tool never sets one behind your back.**

So the behaviour is intended and the rationale is sound: silently creating a tracking relationship is a
side effect a caller did not ask for, and this subset's whole discipline is that nothing happens
implicitly (the same reason `--add` never becomes `git add -A`).

The narrower thing worth raising, then, is not "add the missing flag" but: **an explicit
`--set-upstream` is not "behind your back."** The caller typing it *is* the request, exactly as `--push`
and `--tags` are requests for effects that do not happen by default. As it stands, the one case the policy
makes unreachable is the first push of a new branch — which is every PR — so the lane's own documentation
sends the user to raw `git push -u` on the most routine action in the workflow.

Two factual notes for whoever triages this:

* The help says "in a single-remote repo", but the observed repo has **two** remotes and naming one
  explicitly (`--remote origin`) did **not** help — `push.default simple` still requires the upstream to
  match, so the refusal is not confined to the single-remote case the help describes.
* If the policy is kept as-is, the actionable residue is a **documentation** one: the failure surfaces as
  raw git's error rather than as a tool-level message pointing at the deliberate choice and the one-time
  `git push -u` remedy. A caller currently learns the policy only by reading `--help` after being
  confused by a `fatal:`.

**3. `fetch` cannot choose a remote — and reports success while leaving the other one unfetched.**
`push` and `commit` both take `[--remote <name>]...`; `fetch` does not:

```
$ tt git fetch --repo <abs-repo> --remote origin
git: usage: tt git fetch --repo <dir>
```

In the normal contributor shape — `origin` = your fork, `upstream` = the project, or the reverse — there
is no way to fetch the other one through the tool. In the field test, reading upstream files fell back to
the GitHub API instead.

**This item is worse than a missing flag**, and the sharper form was found while filing this very issue.
On a checkout whose current branch tracks one remote, `fetch` reports success while the *other* remote
stays unfetched — and the toolbox's own sibling immediately contradicts it:

```
$ tt git fetch --repo /abs/genscalator
fetch: up to date

$ tt gitinfo /abs/genscalator --remote origin
sync:    0 ahead, 1 behind upstream
remote origin: differs (remote HEAD not present locally; fetch to compare)
              (local 339db1695245 vs remote 542b2fd03efc)
```

So `fetch` says "up to date", and `gitinfo` says "fetch to compare" — about the same repo, seconds apart.
A caller who trusts the first message concludes the checkout is current when it is 1 behind on the remote
that matters, which is the failure mode this subset exists to prevent. `gitinfo`'s message is the correct
one and even names the remedy, but the remedy is not expressible in `tt git`.

**Also observed, related but arguably out of scope:** non-interactive push in that repo needs
`git -c credential.helper='!gh auth git-credential' push`, and the subset has no way to express a
credential helper. A plain `tt git push` there dies with
`fatal: could not read Username for 'https://github.com'`. Noting it because it is a fourth place the
lane cannot go, not because `tt git` should necessarily grow a credentials flag.

**Framing.** This is the same family as issue 004 and should be triaged with it: 004's tripwire rule says
reaching for a raw shape IS the signal that a typed verb is missing. Here the signal fired four times in
one workflow. It is also coupled to issue 024 — `tt-toolbox` currently tells the agent to use bare
`git -C` for everything, and the honest wording of that skill depends on which of these gaps get closed.

## How to reproduce it

In any checkout, on a branch with no upstream:

```
$ tt git                                              # usage: no branch/switch verb
$ tt git push --repo <abs-repo> --remote origin       # fatal: no upstream branch
$ tt git fetch --repo <abs-repo> --remote origin      # usage error: fetch takes no --remote
```

## Acceptance sketch

* A branch verb in the non-destructive half, e.g. `tt git branch --repo <dir> --new <name>` (create +
  switch). Deliberately *not* branch deletion, which belongs with the excluded set.
* For item 2, **the maintainer's call, and the policy may well win.** Either (a) an opt-in
  `--set-upstream` flag, on the argument that an explicitly typed flag is a request rather than something
  done behind the caller's back — the same standing as `--push` and `--tags`; or (b) keep the policy and
  make the tool *own* the refusal: detect "no upstream" before shelling out and print the deliberate
  choice plus the one-time `git push -u` remedy, instead of surfacing raw git's `fatal:`. Option (b) alone
  would resolve most of the friction this item caused.
* `--remote <name>` on `fetch`, for parity with `push` and `commit`. Cheapest of the three — and the
  success message must describe what was actually fetched, e.g. `fetch: origin up to date (upstream not
  fetched)`, so it can never contradict `tt gitinfo` again. Of the three items this is the one with a
  correctness edge rather than only ergonomics: today's message can be read as a false all-clear.
* Whatever lands, the guard-clean digest and `tt-toolbox` (issue 024) should state the resulting
  boundary explicitly, so "when is bare git legitimate" has one answer rather than three.

## Discussion

### Comment by hmiddelk/Opus5 at 2026-08-11 15:01

Filed from the second alpha field test (report 087). Item 1 is a second occurrence of a report-085 note,
which is the argument for promoting it from note to issue: it is not a one-off preference, it recurs on
every PR.

Deliberately filed as one issue with three items rather than three issues, following the shape of issue
020 — they were discovered in a single unbroken sequence and share a triage question ("how far does the
non-destructive half extend?"). Happy to see it split if the maintainer prefers; item 3 in particular is
a one-line parity fix that need not wait for the other two.

Commit `--amend`, the other half of the report-085 note, is **not** included here: amend rewrites a
commit, so unlike the three above it is genuinely arguable against the safe-subset principle. Left for a
separate decision.

**Re-checked against `main` at `542b2fd` (the v0.10.2 pre-release tree) before filing**, since v0.10.2
shipped a polish wave that closed 017 and 020: all three items persist there — `tools/git.scala` has no
branch verb, `fetch` still takes only `--repo`, and the upstream policy is as quoted.

That re-check is also what caught the error in item 2. The first draft of this issue called the
`--set-upstream` absence a gap; reading `tools/git.scala`'s help text showed it is a stated design
decision with a good reason behind it. Recorded rather than quietly amended, because "the tool is missing
X" and "the tool deliberately refuses X" want different responses from a maintainer, and an issue that
confuses the two wastes the reader's time arguing with a position nobody holds.

Agent disclosure: found and drafted by an AI agent (Claude Opus 5) under human direction; the human ran
the raw-git commands the tool could not, reviewed and submitted. The item-2 misreading above was the
agent's and was corrected before filing.
