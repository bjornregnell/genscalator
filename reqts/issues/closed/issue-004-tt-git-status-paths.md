# Issue 004: `tt gitinfo` reports a change COUNT but no paths, so precise staging has no typed shape

> status: closed 2026-08-19, fixed by `e4d1284` · labels: toolbox, git, safety · summary: `tt gitinfo` says "10 uncommitted
> change(s)" without saying WHICH, and `tt git commit --add <p>` requires naming exact paths.
> The one shape that closes the gap does not exist, so the agent reaches for a bare
> `git status --short` at exactly the moment it is deciding what to write to history.

## Description

`tt git commit` is deliberately additive: it stages only the paths given with `--add`, which is
what keeps the no-clobber rule enforceable (never stage a file a human is holding). But deciding
*which* paths are mine requires a list of what changed, and no typed verb produces one:

* `tt gitinfo <repo>` prints `state: 10 uncommitted change(s)` — a count, no paths. Its `--help`
  is explicit that it covers branch, state, sync and recent log.
* `tt git` owns the write subset (commit/push/pull/fetch) and has no status verb.

So the agent falls back to a bare `git status --short`. That is not a reflex slip: the digest's
rule is "never raw git status for STATE/SYNC", and gitinfo genuinely covers state and sync. This
is a different need with no shape at all, which makes it a FLAG-THE-GAP case rather than a
regression.

## Why it is a safety issue, not a convenience one

Specimen, 2026-07-27, in this repo. `tt gitinfo` reported 10 uncommitted changes. Nine were the
agent's. The tenth was `reqts/ROADMAP.md`, which BR was editing at that moment under an explicit
edit baton. The list is what caught it. Had the count been trusted and the staging been broad
(`git add -A`, or `--add reqts/`), the agent would have committed a human's in-progress work under
its own commit message — silently, and with a plausible-looking result.

The gap therefore sits precisely where the cost of being wrong is highest: the step that decides
what enters history. A count invites a guess; a list makes it checkable.

## Acceptance sketch

* A read-only typed shape that lists changed paths with their status, e.g. `tt gitinfo <repo>
  --files`, or a `tt git status <repo>` that stays in the non-mutating half.
* Output is one path per line with a status marker, so it composes with `--add` without parsing
  ceremony.
* Distinguishes staged / unstaged / untracked, since "untracked" is the case where a broad add
  does the most damage.
* `tt gitinfo --help` and the guard-clean digest both point at it, so the bare-`git status` reach
  stops being the only available move.

## Discussion

### Comment by bjornregnell/CO5 at 2026-07-27

Filed on BR's go, straight after the near-miss above. Related: the digest's tripwire rule (reaching
for a raw shape IS the signal that a typed verb is missing), and the same class of gap for commit-log
search by grep or trailer, which also still has no typed shape.
