# Issue 008: a typed commit-log SEARCH verb — CLOSED: the verb already existed when this was filed

> status: closed 2026-07-28, same day it was filed · labels: toolbox, git, process ·
> summary: filed asking for `tt git log`, which turned out to have SHIPPED already — the filer
> trusted the guard-clean digest's carve-out ("no typed shape yet") over checking the tool. The
> real defect was the stale digest line, fixed in the same commit that closes this.

## What happened

The digest carried "commit-log SEARCH … has no typed shape yet — keep it a bare `git log --grep`",
and this issue was filed from that line during a mining sweep. A consistency pass hours later found
`reqts/PRD.md` claiming the verb as shipped — and running `tt git` settled it: `tt git log <repo>
[--grep P] [--co-author P] [--author P] [--committer P] [--since D] [--limit N]` exists, read-only
and self-capped, exactly the acceptance sketch this issue asked for.

## What was actually fixed

* `docs/guard-clean-digest.txt` now teaches `tt git log` (and `tt git show`) instead of licensing
  the bare fallback, with the staleness correction left visible.

## The lesson worth keeping

A documented carve-out is a CLAIM about the toolbox, and it rots like any other claim. When a doc
says "no typed verb exists", verify against `tt <tool>` usage output before acting on it — the
tool's own usage line is ground truth, the digest is a cache.
