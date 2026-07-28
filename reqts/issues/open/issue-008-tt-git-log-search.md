# Issue 008: a typed commit-log SEARCH verb — grepping history currently has no shape

> status: open · labels: toolbox, git · summary: there is no typed shape for "grep the commit
> log" (message/author/committer/date, or the `Co-Authored-By:` trailer), so the agent falls back
> to raw `git log --grep` — and the first live attempt tripped the guard with a `| head` clobber,
> the exact reflex the guard-clean digest forbids.

## Description

`tt gitinfo` covers branch, state, sync, and recent log; `tt git` owns the write subset. Neither
can answer "which commits mention X" or "what did this co-author land". The guard-clean digest
today carries an explicit carve-out telling agents to keep this a bare `git log --grep`, which is
a documented hole in the typed surface rather than a solution.

## Acceptance sketch

* A pure read verb, e.g. `tt git log <repo> [--grep RE] [--author RE] [--committer RE]
  [--since D] [--co-author RE] [--max N]`.
* `--co-author` greps the `Co-Authored-By:` trailers — the thing GitHub attributes contributors
  from, and the query that motivated this issue.
* Tab-separated `sha⇥author⇥subject` out, capped by the tool itself so no `| head` is ever
  needed; bare-allowlistable read-only shape.
* The guard-clean digest drops its carve-out and points here instead.

## Discussion

### Comment by bjornregnell/Charlie at 2026-07-28

Filed on BR's go, from the v0.10.1 mining sweep; the specimen investigation (2026-07-24) needed
exactly this twice.
