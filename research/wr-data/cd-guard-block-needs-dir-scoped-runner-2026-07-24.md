# Toolbox-gap specimen: the guard blocks `cd`, exposing the missing dir-scoped runner (2026-07-24)

Another specimen in the running pattern where REAL work surfaces a `tt`/scratch-tool candidate — a
missing typed shape forces a raw command, which the guard then (correctly) blocks, leaving a dead-end
until the shape is built. Sibling specimens: SM217 (`tt git log` / `tt forge contributors` gaps forced
raw `git log … | head`, which tripped the guard), SM218 (history-rewrite reached for `pip install
git-filter-repo`, guard-blocked).

## The event
Fixing the introprog `build.sbt` CI break, BR asked the agent to `sbt --client` the English build. The
agent's shell cwd is pinned to the session's working repo (`genscalator-work`); the target build is in a
different clone (`.../lunduniversity/introprog`). To run `sbt --client` there the agent needs that cwd —
but:
- `sbt` has no `-C`/`--dir` flag (unlike `git -C`).
- A bare `cd /abs/introprog` was **blocked by guardcheck**: `[HIGH] cd + compound: use git -C <abs>
  for git; pass absolute paths; never cd-then-chain`.

So there was no allowlist-clean way to run the command — a genuine dead-end, not a reflex slip. The
agent could not fulfil the request and had to hand it back to the human (who was already in an sbt
session in that dir).

## Why the guard is RIGHT here (not a false positive)
Blocking `cd` prevents the cd-then-chain anti-pattern the guard-clean discipline forbids. The gap is not
the guard being wrong; it is the TOOLBOX lacking a typed way to run a command in a chosen directory —
exactly the gap `tt git --repo <dir>` already fills for git (it sets the dir via the git process, no
shell `cd`).

## The candidate (SM226)
A narrow, dir-scoped external-command runner, e.g. `tt sbt --dir <abs> <task>`, that sets the working
directory via JDK `ProcessBuilder.directory(dir)` — no shell `cd`, sidestepping the block by
construction and staying reviewable. NOT a general `tt run --dir -- <anycmd>` (that is the
interpreter-style escape-hatch the guard resists, [[never-allowlist-interpreters]]); start specific,
generalize only on a second case ([[contribute-tool]]).

## The pattern this is a datum for
Toolbox gaps are discovered by doing real work, and they announce themselves as guard blocks / raw-shape
reaches, not as abstract design gaps. The guard is a discovery instrument as much as a safety one: each
block that is NOT a reflex slip is a signpost to a missing typed shape. Count so far this week:
SM217 (×2 verbs), SM218, SM226. Ties `tt git --repo` (the pattern to mirror),
[[genscalator-toolbox-single-dispatcher]], [[guardcheck-hook-structural-fix]], the avoid-guard-stall skill.
