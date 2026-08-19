# Issue 009: `tt forge contributors` — a read verb for who the forge thinks contributed

> status: closed 2026-08-19, fixed by `7add972` · labels: toolbox, forge · summary: `tt forge` speaks issues/prs/releases/tags but
> cannot read a repo's CONTRIBUTOR list, so verifying "who does the forge credit" needs `tt web
> get` on the raw API or a browser.

## Description

GitHub `GET /repos/<o>/<r>/contributors` (Gitea/GitLab analogs) answers a question that comes up
in contributor management and attribution checks. The verb folds naturally into the existing
read-verb dispatch and dialect routing (`--gh | --gl | --url BASE`).

## Acceptance sketch

* `tt forge contributors <owner>/<repo> [--gh | --gl | --url BASE]`
* Prints `login⇥contributions⇥type`, one row per contributor.
* Read-only; same anonymous-with-token-fallback behavior as the other forge reads.

## Discussion

### Comment by bjornregnell/Charlie at 2026-07-28

Filed on BR's go, from the v0.10.1 mining sweep; sibling of issue-008 — the same 2026-07-24
investigation needed both.
