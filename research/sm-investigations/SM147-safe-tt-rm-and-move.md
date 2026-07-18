# SM147 — a safe, no-clobber `tt rm` / `tt move`

**Status: DESIGN (agent-drafted AFK 2026-07-18).** No build here — the tool, and especially whether the agent
may run it un-prompted (vs `ask`), is **BR-gated** (a security call). This note is the interface + safety model
to react to.

## Why (the gap, grounded)

The agent can **create and edit** files freely (Write/Edit) but has **no sanctioned way to delete or move**
one. Surfaced 2026-07-18: raw `git rm` / `rm` are **deny-listed** in `.claude/settings.local.json` (a deliberate
prior-session hardening — the deny held even against BR's verbal OK), and `tt git` exposes only
`commit / pull / fetch / show` (git.scala:88-91) — **no `rm`, no `mv` by design**. So a badly-named file (e.g.
the mis-numbered blog stub that day) can only be removed by **BR** in his own terminal. That is a real capability
asymmetry: create is cheap, delete is impossible. WR:
`wr-data/agent-can-create-files-but-has-no-sanctioned-path-to-delete-one-2026-07-18.md`.

The fix is **not** to loosen the deny-list (the blanket raw op *should* stay blocked). It is to give the toolbox
a **bounded, typed** delete/move that matches `SECURITY-MODEL` §3.5: the allowlist holds **syntax** (`tt rm
<path>` is one analyzable literal), the **semantics** (scope, no-clobber, no-traversal, git-awareness) live
**inside the tool** — never in an allowlist pattern.

## Interface (sketch)

```
tt rm   <path>                 # remove ONE file, path-scoped to the repo it lives in
tt move <src> <dst>            # move/rename ONE file, refuse-clobber
```

- **One explicit path per call.** No globs, no recursive `-r`, no variadic list — matches the `tt` ethos of one
  literal, statically-analyzable command (tools/tt:2-4). Deleting a directory tree is out of scope by design.
- Flags kept minimal; `--repo <abs>` to disambiguate like `tt git` if cwd is unclear.

## Safety model (the whole point of the tool)

1. **Path-scope — refuse to escape the repo.** Resolve the canonical path; reject if it is not under the repo
   root, if it contains `..` traversal, or if it targets `.git/` or the repo root itself. (Mirrors the tt
   launcher's own `no '/', no '..'` guardrail, tt:22-23.)
2. **No-clobber (`move`).** `tt move` **refuses if `dst` exists.** Overwrite is a *separate, explicit* gesture,
   never the default ([[no-clobber-human-owned-files]]). A `--force` is a design fork below, not assumed.
3. **Git-aware.** If the path is **tracked**, use `git rm` / `git mv` semantics (stage the change, keep history
   clean, leave it committable) rather than a raw filesystem unlink. If **untracked**, a plain fs remove/move.
4. **The genuinely dangerous case = untracked delete.** A tracked delete is recoverable from git history; an
   **untracked** `rm` is **unrecoverable**. Two candidate mitigations (design fork for BR):
   - **(A) refuse untracked deletes** by default (require an explicit `--force`), or
   - **(B) move-to-trash**: relocate to a repo-local `.trash/` (gitignored) instead of `unlink` — always
     recoverable, at the cost of some clutter + a later sweep.
   Recommendation: **(B) for `rm`** (recoverable-by-default is the safer floor), with tracked files still going
   through `git rm` so they show up as a normal staged deletion.
5. **Refuse human-owned files the agent did not author** stays a *policy* on top ([[no-clobber-human-owned-files]]);
   the tool can't know authorship, so this is the caller's discipline, not a tool check — noted so it is not
   forgotten.

## The permission question (BR's call — do not pre-decide)

Even bounded, delete is destructive ([[never-blanket-allow-destructive-commands]]). Options, safest first:
- **`ask` always** — `tt rm`/`tt move` surface a permission prompt every time (the tool is *convenience +
  analyzability*, not un-attended authority). Safest; the agent still can't delete behind the human's back.
- **allowlist only a narrow scope** — e.g. `tt rm tmp/*` agent-solo, everything else `ask`. Needs the scope to
  be expressible; risky to get right.
- **full allowlist** — not recommended; re-creates the capability the deny-list intentionally removed.
Default recommendation: **`ask`**, with trash-by-default so an approved delete is still reversible.

## What it must NOT do
- No recursive directory removal; no glob expansion; no variadic paths.
- Never touch `.git/`, the repo root, or paths outside the repo.
- Never silently overwrite on `move`.
- Not a replacement for `git rm` in a commit flow — `tt rm` stages the deletion; the commit still goes through
  `tt git commit`.

## Ties
SM146 (toolbox self-sufficiency — the sibling gap: native tools; this one: delete/move) · SM073 (this **retires a
denied raw destructive op**, so it is an input to the settings-vs-`SECURITY-MODEL` audit) · `SECURITY-MODEL`
§3.5 (syntax-in-allowlist / semantics-in-tool) · [[no-clobber-human-owned-files]] ·
[[never-blanket-allow-destructive-commands]]. This was the old `broad-allowlist-aversion` `tt move`/`tt copy`
candidate, now concretely motivated.
