# SM149 — `tt git push-all`: every-commit box-burn resilience

**Status: DESIGN (agent-drafted AFK 2026-07-18).** The tool is a small git wrapper the agent can draft, but it is
**egress behaviour**, so the wiring and any allowlist entry stay **BR-reviewed**. This note is the interface +
safety model to react to.

## Why (the gap, grounded)

2026-07-18 Codeberg went flaky mid-session (SSH port 22 dropping), stranding a local commit off-box. The fix
raised genscalator to three hosts and mirrored the work repo — but a **plain `git push`** only reaches **one**
upstream, and `mirror.sc` clones the master **from** Codeberg, so it is a verbatim *release* mirror that **cannot
rescue a stranded LOCAL commit**. On a box that can OOM/GNOME-crash ([[blixten-box-flaky]]), the safe habit is
commit **and push every unit** — but "push" today means one remote. The gap: **no single command pushes the
current branch to every configured mirror**, so real redundancy depends on the agent remembering to push N times.

## Interface (sketch)

```
tt git push-all --repo <dir> [<remote-name>...]
```
- Bare: push the current branch to **every** remote configured in the repo's `.git/config`.
- With names: push only to that **subset** (e.g. `tt git push-all --repo /abs github coursegit`).
- Slots in as a new `dispatch` case beside `commit/pull/fetch/show` (git.scala:87-92); reuses the existing `run()`
  helper (git.scala:79-83) and the same `--repo` resolution the other verbs use.

## Safety model (the core of SM149)

1. **Targets come from git config — NEVER from a URL arg.** The list of remotes is read via `git -C <repo>
   remote`. Any positional arg is validated to be a member of that configured set; an **unknown name is refused**,
   never fetched/pushed. This is the whole defense: a **URL as an argument is an exfiltration channel the allowlist
   cannot bound** (it could name any host), so args are remote **names** only — the same design as `tt forge`'s
   `TT_FORGE_HOSTS` allowlist. The tool can push only where the human already ran `git remote add`.
2. **Plain fast-forward push, per remote.** `git push <remote> <current-branch>` with **no `--force`, no
   `--mirror`, no `--delete`, no `--tags` sweep**. Consequences, all load-bearing:
   - a non-fast-forward is **refused** (never rewrites remote history),
   - only the **current branch** goes (never propagates local WIP/experiment branches),
   - no remote ref is ever deleted.
   Stays inside `tt git`'s existing safe subset — `push` is already exposed (git.scala:122-125); this only fans it
   out. reset/rebase/force/rm/clean remain off the tool entirely.
3. **Per-remote independent + continue-on-fail.** Each remote is pushed in its own `run()` call; a failure (flaky
   Codeberg) is caught and recorded, and the next remote is still attempted. One bad host yields
   "2 ok, 1 failed — you are off-box safe", not an abort.
4. **Read-only on the working tree.** Push never touches files; nothing to clobber.
5. **Clear per-remote summary + honest exit code.** Print `remote: ok (sha)` / `remote: FAILED (reason)` per
   target and a tally; exit non-zero if **any** push failed, so the agent sees partial success as not-fully-safe
   and can report it, rather than a green all-clear.

## Relationship to `mirror.sc` (keep both)
`mirror.sc` stays the **release** tool: Codeberg master → the public mirrors, verbatim, run deliberately.
`push-all` is the **every-commit insurance**: the local branch → all configured remotes, run on each unit. Setup
is one `git remote add <name> <url>` per mirror, once (a human gesture — the URLs enter config by hand, never
through the tool).

## What it must NOT do
- Never accept a URL as an argument; never push to anything not already in `.git/config`.
- No `--force` / `--mirror` / `--delete` / branch-spray — current branch, fast-forward, that is all.
- Never abort the remaining remotes because one failed.
- Not a replacement for `mirror.sc`'s release flow.

## The permission question (BR's call — do not pre-decide)
It is egress to multiple hosts, so the allowlist entry is BR-reviewed. It is a **good** allowlist candidate — it
can only push the current branch, fast-forward, to pre-configured remotes, so its blast radius is bounded by
`.git/config`, not by the argument string. But "push unattended to every mirror" is still a deliberate decision.

## Ties
SM146 / SM147 / SM152 (toolbox self-sufficiency) · SM035 (mirror sovereignty) · SM148 (repo rename / mirror
thread) · [[blixten-box-flaky]] (the box that motivates push-every-unit) · [[commit-via-tt-git-not-raw-cd-git]]
(push-all is the natural extension of the sanctioned commit path) · `SECURITY-MODEL` §3.5 (targets from config =
semantics-in-tool, not a URL in an allowlist string).
