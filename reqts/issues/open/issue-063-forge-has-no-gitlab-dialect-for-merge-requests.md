# Issue 063: `tt forge` speaks GitLab for four verbs but not for merge requests, so listing and merging MRs leaves the typed lane

> status: open 2026-09-23 · labels: toolbox, forge, gitlab, dialect, missing-verb · measured
> against: v0.10.2 (from `VERSION.txt`) at `b410903`, Scala 3.9.0, Linux · summary: `Dialect.GitLab`,
> the GitLab token chain and the `TT_FORGE_GITLAB_HOSTS` trusted-host guard all exist and are used by
> four verbs (`whoami`, `contributors`, `release-create`, `file`). The pull-request family does not
> route `--gl`: `prs` and `pr` reject it with the generic `unknown/incomplete flag '--gl'`, and
> `pr-merge` refuses it with a **deliberate, explained** message. So a repo hosted on GitLab has no
> typed shape for "list open MRs" or "merge this MR", and the work leaves `tt` for `glab` or the web UI.

## Description

`tt forge` is dialect-routed: a `Dialect` enum selects Gitea/Forgejo (default), GitHub (`--gh`) or
GitLab (`--gl`), and the token/host machinery for GitLab is already built. `forge.scala` accepts
`--gl` at four sites (`:307`, `:472`, `:866`, `:1306`) and carries a GitLab trusted-host guard at
`:251` and `:345`.

What is missing is the **merge-request family**. Listing and merging are the verbs a maintainer
actually runs on a mirror, and they are exactly the ones with no GitLab path.

### The current surface, verb by verb

Enumerated from `tt forge --help` at `b410903`. `--gl` is accepted by **4 of 20** verbs:

| accepts `--gl` | rejects `--gl` |
| --- | --- |
| `whoami`, `contributors`, `release-create`, `file` | everything else, including `prs`, `pr`, `pr-merge`, `pr-files`, `pr-commits`, `issues`, `issue` |

**The rejections are not all the same, and the difference matters to this issue.** Two distinct
behaviours:

* **No decision recorded.** `prs` and `pr` have no `--gl` case at all, so the flag falls through to
  the catch-all at the end of their flag loops and prints:

  ```
  forge: unknown/incomplete flag '--gl'
  ```

  That reads as a typo in the flag rather than as an unimplemented dialect, which is the ergonomic
  cost: nothing tells the caller that `--gl` is a real flag this verb does not serve.

* **A decision recorded, and honest about it.** `pr-merge` refuses explicitly (`forge.scala:722-724`):

  ```
  pr-merge is not implemented for GitLab (a merge request is a different API shape there).
    Stated rather than faked — use --gh or the default Gitea/Forgejo dialect.
  ```

  Three release verbs refuse the same way and give a substantive reason (`release-edit` at `:1005`,
  `release-upload` at `:1126`, `asset-rm` at `:1064`: GitLab releases carry *links* to external
  artifacts rather than uploaded assets, so the flags would mean something different there).

**So part of this issue asks to discharge a stated non-implementation rather than to fix an
oversight.** `pr-merge`'s refusal is correct as written, and "a merge request is a different API
shape there" is the actual reason the work is not free. This issue is the argument that the shape is
worth implementing anyway, plus a request that `prs`/`pr` say what `pr-merge` already says while they
wait.

### Why this is worth a number

**genscalator is itself mirrored to GitLab.** `issue-000` lists
`https://gitlab.com/bjornregnell/genscalator`, `issue-035` records the mirror set as origin + gitlab +
coursegit + codeberg, and `reqts/PRD.md:183` already counts `release-create --gh/--gl` as shipped
capability. The project's own sovereignty argument is that no single forge should be load-bearing, and
the PR family is the one place where that currently fails: a contribution arriving on the GitLab
mirror cannot be listed or merged through `tt`.

**It is a guard-integrity matter, the same shape as issue 060.** With no typed verb, the work is done
by reaching for `glab` or raw `curl`, which is the reach issue 060 is about for issue creation. A
missing verb does not stop the task; it relocates it to an interpreter the guard cannot inspect.

**The batch case is where the hand-rolled version gets dangerous.** The motivating task is
"merge the open MRs oldest-first, but only the cleanly mergeable ones". Done by hand that is a loop
over a JSON listing with a merge call inside it, which is precisely the shape that should not be
improvised: `pr-merge`'s existing semantics (preview by default, apply only with `--yes`, refuse an
unmergeable PR, never delete the source branch) are the safety properties a hand-rolled loop drops
first.

## How to reproduce it

```bash
# 1. the listing verb rejects the dialect flag as if it were a typo
tt forge prs bjornregnell/genscalator --gl
#    => forge: unknown/incomplete flag '--gl'          (exit 2)

# 2. the merge verb refuses deliberately, and says why
tt text context tools/forge.scala 'case "--gl" :: _' 3
#    => :722  pr-merge is not implemented for GitLab (a merge request is a different API shape there)

# 3. the dialect, token chain and host guard already exist
tt text match tools/forge.scala 'case "--gl"|TT_FORGE_GITLAB_HOSTS'
#    => :307 :472 :866 :1306 accept it;  :251 :345 the trusted-host guard

# 4. which verbs speak GitLab today
tt forge --help
#    => --gh | --gl on whoami, contributors, release-create, file; --gh | --url BASE elsewhere
```

Measured 2026-09-23 on Linux at `b410903`. Steps 1-4 were run and their output is quoted.

⚠ **The GitLab API specifics in the sketch below are NOT verified.** They come from the filer's
proposal, not from a probe against a live GitLab instance or a reading of GitLab's API reference. No
endpoint, response field or parameter name below has been confirmed by this repo. In particular
`detailed_merge_status`, its value set, and the `merge_status` fallback are stated as the proposal
received them. Anyone implementing this should treat the endpoint table as a starting point to
check, not as a specification — the same caution the release verbs' refusals imply about assuming one
forge's shape carries to another.

## Acceptance sketch

* **`prs --gl`** — `GET /projects/:id/merge_requests?state=opened&order_by=created_at&sort=asc`, with
  `:id` the URL-encoded `owner/repo`. Print iid, created_at, author, source branch, and the merge
  status, keeping the existing one-line-per-PR shape with the head branch in `[brackets]`.
* **`pr --gl`** — `GET /projects/:id/merge_requests/:iid`, rendering merge state + description the way
  the GitHub path does.
* **`pr-merge --gl`** — `PUT /projects/:id/merge_requests/:iid/merge`, **keeping every existing
  safety property**: preview by default, apply only with `--yes`, refuse unless the MR is actually
  mergeable, never remove the source branch (`should_remove_source_branch=false`), `--method squash`
  → `squash=true`, and pass `sha` = the previewed head sha so that a push landing between preview and
  apply makes the merge fail rather than silently merge something unreviewed. That last property is
  the one worth most here and has no equivalent in a hand-rolled call.
* **`pr-files` / `pr-commits --gl`** (lower value) — via `/merge_requests/:iid/changes` and
  `/commits`. `pr-commits` also carries the CONTRIBUTING pre-merge credit check, so it is worth more
  than its size suggests.
* **Token and host: reuse what exists, add nothing.** `GENSCALATOR_GITLAB_TOKEN` / `GITLAB_TOKEN`,
  sent as `PRIVATE-TOKEN`, only to a host in the `TT_FORGE_GITLAB_HOSTS` / `TT_FORGE_HOSTS` set,
  never from a flag. Self-managed instances arrive via `--url BASE`, as `release-create --gl` already
  does for `git.cs.lth.se`.
* **Until it is implemented, make `prs` and `pr` refuse like `pr-merge` does.** This is separable,
  cheap, and worth doing even if the rest is deferred: a `case "--gl" :: _ => die(...)` naming the
  dialect as unimplemented, instead of `unknown/incomplete flag '--gl'`, which misdescribes a real
  flag as a typo. Same pattern as four verbs already in the file.
* **Two gotchas from the proposal, both unverified and both affecting the safety argument:**
  GitLab is said to compute merge status asynchronously, so the status may read `checking` or
  `unchecked` right after a merge and need a brief poll before deciding; and older self-managed
  instances may lack `detailed_merge_status`, wanting a `merge_status` fallback. If the first is
  true it is load-bearing, not cosmetic: a batch merge that reads `checking` as "not mergeable" skips
  work silently, and one that reads it as "mergeable" defeats the refusal.

### Possible follow-up verb, explicitly NOT part of this issue

`tt forge pr-merge-queue <owner>/<repo> --gl [--on-conflict stop|skip] [--yes]`: oldest-first,
re-checking mergeability after **each** merge because an earlier merge can make a later MR conflict,
previewing the whole plan by default. Recorded so the idea is not lost, and kept separate because it
is a new effectful verb with its own safety design rather than a dialect added to an existing one.
Filing it now would also bundle a decision (should `tt` drive a merge queue at all?) with a mechanical
dialect addition.

* **Out of scope:** the other `--gl` gaps. `issues`/`issue` have the same shape and are not argued
  here; the three release verbs' refusals are substantively reasoned and this issue does not reopen
  them. So four-of-twenty is a count of the current surface, not a claim that the remaining sixteen
  all want GitLab.

## Discussion

### Comment by bjornregnell at 2026-09-23 13:52

Filed from my own proposal, under the working name `#add-tt-forge-gitlab-features`. The need is
concrete rather than hypothetical: batch-merging open MRs oldest-first, only where cleanly mergeable,
on a GitLab-hosted repo, which today has no typed shape at all.

Two things I want on the record as the reason this is a number and not a quiet patch. First, that
`pr-merge` **already** refuses GitLab deliberately and says why: I am asking to change a stated
decision, so it should be visible as such rather than look like filling in a blank. Second, that
`prs` and `pr` currently misreport a real flag as an unknown one, which is a small independent defect
worth fixing whatever happens to the rest.

Agent disclosure: the proposal text, the endpoint sketch, the safety requirements for `pr-merge` and
the merge-queue follow-up idea are mine. An AI agent (Claude Opus 5) in session with me filed this
file, and verified BY RUNNING: `tt forge prs … --gl` (the exact error text above), `tt forge --help`
(the four-verbs-of-twenty count), and the two `forge.scala` searches locating the existing
`Dialect.GitLab` routing, the trusted-host guard and the five deliberate `--gl` refusals with their
messages. It did NOT verify any GitLab API detail, and flagged that limit in the reproduction section
rather than presenting my endpoint list as measured, which is the right call: those lines are my
proposal, not this repo's findings.
