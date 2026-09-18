# Issue 060: `tt forge` can read issues but not create them, so filing one leaves the typed lane for raw `gh`

> status: open · labels: toolbox, forge, missing-verb, guard-integrity · summary: `forge` has `issues`
> and `issue` for reading and `pr-merge`, `release-create`, `release-edit`, `release-upload`,
> `asset-rm` and `release-delete` for writing, but nothing that FILES an issue. Filing therefore
> happens through `gh issue create`, outside the fixed-env token handling, the trusted-host guard and
> the `[audit]` line that every other effectful forge verb goes through. This is the
> missing-verb-causes-an-invisible-reach shape, on an OUTWARD-facing action.

## Description

Hit 2026-09-18 when two issues had to be filed on `lunduniversity/introprog`. `tt forge --help` lists
no create verb for issues, so the work was done with:

```
gh issue create --repo lunduniversity/introprog --title "..." --body-file <path>
```

That worked, and `gh` was already authenticated. The problem is not that it failed; it is where it
happened.

### Why a missing verb matters more here than usual

The toolbox's own tripwire says the reach itself is the signal: reaching for a raw interpreter or a
general-purpose CLI is what indicates a typed verb is missing. This instance has three properties
that make it worth a verb rather than a one-off:

1. **It recurs.** genscalator keeps its issues as files under `reqts/issues/open/`, but introprog —
   the other repo worked on daily — keeps them on the forge, 39 open at the time of writing. Filing
   is not a one-time act there.
2. **It is EFFECTFUL and OUTWARD.** Every other effectful forge verb prints an `[audit]` line first,
   reads its token only from fixed env vars, refuses to send that token anywhere but a trusted host,
   and takes prose from a `--body-file` so it never rides a command line. `gh issue create` bypasses
   all four of those properties. An outward-facing write is exactly the category those guarantees
   exist for.
3. **The prose problem is already solved elsewhere in this tool.** `release-create` takes
   `--body-file F` precisely so release notes containing shell metacharacters never touch a command
   line, and `pr-merge` takes `--body-file F` for the same reason. An issue body is longer and more
   metacharacter-dense than either. The pattern to copy is already in the file.

### What exists today

Reading is complete: `forge issues <owner>/<repo>` lists, `forge issue <owner>/<repo> <n>` shows one
with its comments. Writing is complete for releases and for merging a PR. The asymmetry is
specifically that an issue can be **read but not written**, while a release can be created, edited,
uploaded to and deleted.

## How to reproduce it

```
$ tt forge --help | <read it>
  ... issues, issue, prs, pr, pr-files, pr-diff, pr-commits, pr-merge,
      release-create, release-edit, release-upload, release-download,
      release-delete, asset-rm, protection, file, contributors, whoami, tags
  ... no issue-create, no issue-edit, no issue-comment, no issue-close
```

Then try to file an issue with `tt` alone. There is no path.

## Acceptance sketch

* **`forge issue-create <owner>/<repo> --title S --body-file F [--gh | --gl | --url BASE]`**, built to
  the same contract as `release-create`:
  * body from a FILE, never inline, for the metacharacter reason above;
  * token from the fixed env vars only, never a flag;
  * trusted-host check before the token is sent;
  * an `[audit]` line printed before the write;
  * prints the created issue's number and URL on success, so the caller can verify without a second
    listing call (which, per issue 059, may under-report).
* Consider `--label` (repeatable) and `--assignee`, but only if they are cheap; the blocking gap is
  creation.
* **Explicitly NOT in scope, and worth writing into the issue so it does not erode:** closing or
  deleting issues. Closing is a judgement about whether work is done, and the repo's own convention
  already puts merging and closing in the human's hand. A create verb lowers the cost of recording a
  finding; a close verb would lower the cost of ending a conversation, which is not the same thing
  and should stay manual.
* Tests: URL construction per dialect (GitHub `POST /repos/:owner/:repo/issues`, Gitea the same
  shape, GitLab `POST /projects/:id/issues` with `title`/`description`), the trusted-host refusal,
  and the refusal to run with an empty or missing body file. All pure; none need a live forge.
* Update `docs/guard-clean-digest.txt` once the verb exists, since the digest is where the reflex
  gets re-established after a compaction and an unlisted verb is an unused verb.

## Discussion

### Comment by bjornregnell/Opus5 at 2026-09-18 16:56

Filed from the reach itself rather than from a later audit. Two introprog issues needed filing, `tt`
had no verb, and `gh issue create` was used deliberately and said out loud rather than quietly — which
is the documented correct handling of a one-off, but the need is not a one-off.

Worth noting what the missing verb cost beyond tidiness. Verifying the two filings then required
`tt forge issues --limit`, which turned out to under-report (issue 059), so the absent write verb and
the defective read verb compounded: the write could not be done in the typed lane, and the read used
to confirm it printed a number that was wrong by 37. A create verb that returns the new issue's
number and URL would have made the confirming listing unnecessary.

I have kept close/delete out of the acceptance sketch on purpose. The argument for `issue-create` is
that recording a finding should be cheap; that argument does not extend to ending one.

Agent disclosure: the gap was hit and this issue text produced by an AI agent (Claude Opus 5) under
human direction; the human reviewed and submitted.
