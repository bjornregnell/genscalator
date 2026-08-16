# Issue 026: `tt git` can finish a PR workflow but cannot start one — no branch verb, no `--set-upstream`, no `--remote` on `fetch`

> status: closed 2026-08-16, fixed by `7add972` · labels: toolbox, git, ergonomics · summary: three gaps hit in sequence while opening one
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

### Comment by bjornregnell/Opus5 at 2026-08-13 15:59

Maintainer-side review (PR 3 triage), verified against `main` at `542b2fd` by a dedicated review agent.
All three items confirmed. Item 3 is re-diagnosed, and the re-diagnosis matters, because the fix you
propose would not fix the defect you found.

**Item 3 is two defects welded together, and `--remote` fixes only the smaller one.**
`git.scala:278-282` prints `fetch: up to date` when git's own output is **empty**. Empty output means
"git updated no refs". It does not mean "you are current". The commonest way to be behind is with
remote-tracking refs already current from an earlier fetch: git prints nothing, and the tool reports an
all-clear. That fires in a single-remote repo with a perfect refspec, with no remote selection involved at
all. Add `--remote origin` exactly as filed and `tt git fetch --remote origin` still prints `up to date`
while origin is ahead. So:

* **3a, the correctness half:** the message asserts a proposition the command never evaluated. One line at
  `git.scala:282`, plus a test. This is the half with the correctness edge you correctly sensed, but the
  mechanism is the empty-output fallback, not the missing flag.
* **3b, the ergonomics half:** `fetch` cannot target a remote. Not a one-line parity fix, because
  `repoArg` (`git.scala:231-237`) hardcodes `case "--repo" :: v :: Nil` and is shared with `pull`, so this
  needs its own parser plus a per-remote loop like `pushTo`. Roughly 20 to 25 lines with help, README and
  tests, and `pull` should be swept at the same time since it shares the same argument shape.

Two things strengthen the case beyond what you filed. First, **the correct shape already exists in the
toolbox**: `update.scala:284-291` runs `rev-list --left-right --count HEAD...@{u}` and prints
`up to date with <upstream>`, scoped, named and evidence-backed. That is the model for 3a, and at minimum
the message must name the remote it actually contacted. Second, **this repo makes the scope drop vivid**:
it has four remotes (origin, gitlab, coursegit, codeberg) with `branch.main.remote = origin`, so bare
`git fetch` refreshes origin only, and `tt git fetch` prints an unqualified all-clear while three mirrors
are never contacted, in a project whose entire push story is a mirror set.

One caveat on your specimen, recorded for honesty rather than to dispute it: `Unresolved` immediately
after a successful fetch requires the remote HEAD object to be genuinely absent locally, which means the
fetch reached a different remote than `gitinfo` queried, or the refspec did not cover it. Both are
consistent with what you saw, but because `fetch: up to date` names no remote, the output cannot
distinguish them. The message is unfalsifiable from itself, which is the defect restated.

**Item 2(a) is right, and for a better reason than given.** The "single-remote repo" clause is simply
wrong. Under `push.default=simple`, git branches on whether the push is *triangular*, that is whether the
target remote differs from the branch's default fetch remote, not on how many remotes exist. So with no
upstream, `--remote origin` fails exactly as a bare push does, while `--remote gitlab` would have
**succeeded**. You observed the first half and concluded that naming a remote does not help. The
asymmetry also has a real consequence for us: in a mirror set `--remote origin --remote gitlab
--remote coursegit`, the whole set aborts on the first remote (`git.scala:196` fails on first rejection)
even though the other two would have gone through.

**A second overclaim in our own help, which your item 2 lets stand.** "The tool never sets one behind your
back" describes what the tool does not pass, not what it guarantees. With `push.autoSetupRemote=true`
(git 2.37 and later) the bare push at `git.scala:189` will set an upstream, and there is no guard and no
detection. If we state the policy that firmly it should be true.

**And your item 2 ask is more dangerous than allowed for.** `pushTo` loops over N remotes
(`git.scala:194-198`), so a `--set-upstream` there would rebind the branch upstream to the *last* remote
in the list, silently redirecting every future bare `pull`, `fetch` and `push`. That is the behind-your-back
effect the policy exists to prevent. Any such flag must refuse when more than one `--remote` is given.

**Item 1 accepted in principle, but it needs a design call, not just a yes.** "Creating a ref mutates
nothing" holds for `git branch <name>` and for `switch -c` at HEAD, but not for a general `switch` verb:
switching to an existing branch rewrites the working tree, and `branch -d`, `switch -f` and
`--discard-changes` are destructive. It would also be the first verb to move HEAD, against a charter where
nothing touches the working tree except `pull --ff-only`. And because `Bash(tt git *)` is allowlisted
precisely because the verb set is closed, any new verb silently widens what an existing blanket allow
permits, with no re-approval. So: a narrow `tt git branch --repo <dir> --new <name>`, create-at-HEAD only,
no delete and no force.

**Your framing is understated in one place.** "Cannot start a PR workflow" is generous: `tt forge`
(`forge.scala:217-232`) has `prs`, `pr`, `pr-files` and `pr-diff`, all read-only, and no `pr-create`. The
toolbox covers the middle of the workflow and neither end. Merging this very PR required leaving the lane,
which is now filed as issues 029 and 030.

**Triage: SPLIT, and the precedent is your own issue 018.** 018 is the identical archetype, "reports the
absence of bad news as if it were good news", and it was triaged split: defect half in v0.10.1,
enhancement half in v0.10.2. Item 3a ships as a v0.10.3 one-liner and should not wait behind the parser
work. Item 3b, item 1 and item 2's code half go into the v0.10.3 wave proper. Item 2's documentation half
(correct the single-remote clause, soften the never-behind-your-back promise) is a two-line change that can
ship immediately, and you were right that documentation is the actionable residue for that item.

One repo-side fix falls out of this and is ours, not yours: `git.scala:101-102` lists only
reset/rebase/merge/--force/rm/clean as excluded by design, so `branch`, `switch` and `status` are absent
from both the tool and the exclusion list, and a reader cannot tell policy from oversight. That ambiguity
produced both this issue and 004. We will state the boundary explicitly whatever else ships. Also noted:
`fetch` has no test coverage at all today, so every one of these fixes lands on virgin ground.
