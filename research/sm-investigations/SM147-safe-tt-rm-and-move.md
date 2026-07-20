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

## Addendum 2026-07-19 — live datums from the .bak cleanup round (PB SM147 ADDENDUM, work-repo `65a745c`)

A real delete task ran end-to-end tonight (three obsolete `media/img/*.bak*` backups) and sharpened three
design points with observed evidence:

1. **Staging is load-bearing, not a nicety.** The "what it must NOT do" section above already said `tt rm`
   stages the deletion — tonight showed the *absence* bites: `tt git commit --add <path>` **cannot stage a
   removal** (it stat-checks the path and errors on the missing file), and `--add <dir>` through the wrapper
   staged nothing either. The only path was a raw `git -C <repo> add -u <dir>` — exactly the improvised-shape
   smell the SM167 addendum names. So: delete = filesystem remove **+ git-aware staging in the same typed verb**,
   and `tt move` likewise (a rename is an add plus a delete).
2. **Report the path's git standing: tracked / untracked / ignored.** The agent inferred "these .bak files are
   tracked" from `git status` *silence* — wrong: `*.bak` is gitignored (`.gitignore:15`), and status silence is
   ambiguous between tracked-clean and ignored. A `tt rm` that *says* which of the three it deleted dissolves
   that inference trap for free (it must resolve the standing anyway to pick unlink vs `git rm`).
3. **Deny-state drift observation (input to SM073, not asserted as fact):** the "Why" section records raw `rm`
   as deny-listed per 2026-07-18; tonight three raw `rm` calls went through the guard lane ask-gated with BR
   present. Whether the deny softened deliberately or drifted is for the SM073 settings-vs-SECURITY-MODEL
   audit to adjudicate — flagged here so the note's own premise gets re-checked there.

## Addendum 2026-07-20 — "legit places" become a gsSettings knob (BR from guard, during the ember rename)

Fresh specimen: the baton→ember rename sweep needed plain file renames (write new + `rm` old), and each
raw `rm` guard-stalled BR — who asked from inside the guard: *"could we have a tool that just removes
stuff in legit places"* and then pointed at the genscalator-settings idea as the natural home for WHERE
those legit places are declared. Design consequence, folded into the feature pair:

1. **The boundedness comes in two layers.** Layer 1 (structural, always on): repo-relative path, no
   `..`/absolute escapes, never `.git/` or the root — the existing "must NOT do" list. Layer 2
   (configurable): a **`rmLegitPlaces`** knob in the gsSettings file (PRD `gsSettings`, SM115) listing
   project-relative directories where deletion is routine (`tmp/`, generated media, scratch dirs). Inside
   a legit place `tt rm` runs in the normal allow lane; outside it, it escalates to `ask` (or refuses,
   per a second knob) — destruction stays one keystroke away exactly where it is NOT routine.
2. **Git standing still decides reversibility** (addendum-1 point 2): tracked deletes are recoverable
   from history and may deserve a laxer lane than untracked ones even inside a legit place; the
   tracked/untracked/ignored report line stays mandatory.
3. **PRD hook:** `gsSettings`' Gist now names the knob ("safe-delete legit places", this addendum); a
   future `Feature: ttRm` should `requires Feature: gsSettings` for layer 2 while layer 1 ships
   config-free.

Rename-sweep context (why the rms happened): `BATON-TEMPLATE.md` → `EMBER-TEMPLATE.md` etc., see
`../wr-data/rename-baton-to-warp-ember-2026-07-20.md`.

## Ties
SM146 (toolbox self-sufficiency — the sibling gap: native tools; this one: delete/move) · SM073 (this **retires a
denied raw destructive op**, so it is an input to the settings-vs-`SECURITY-MODEL` audit) · `SECURITY-MODEL`
§3.5 (syntax-in-allowlist / semantics-in-tool) · [[no-clobber-human-owned-files]] ·
[[never-blanket-allow-destructive-commands]]. This was the old `broad-allowlist-aversion` `tt move`/`tt copy`
candidate, now concretely motivated.
