# SM031 - `tt find`: a typed, safe file-find / prune leaf

**Status:** INVESTIGATION - report before building (BR pin 2026-07-08, from a live
guard specimen). No code yet.

## Why this exists (the thesis in miniature)
Raw `find ... -delete` **cannot be prefix-allowlisted**: `find` is a general
file-executor (arbitrary `-exec`, arbitrary predicates, `-delete` anywhere), so the
guard blocking `Bash(find:*)` was exactly right - allowlisting it would hand the
agent a blank file-mutation shell. That is the genscalator thesis in one specimen:
a *general executor* forces a fatigue-approval on every use because it can never be
safely blanket-allowed; a tool with **declared narrow semantics** can be audited
once and then allowlisted. `tt find` is the typed replacement for the safe subset
of `find` the agent actually needs.

WR lineage: `harness-ux.md:257` (three `tt files` glued with `echo` dividers),
`genscalator-self-dev.md:231` ("what is xargs? tool candidate?" - list-and-filter
filenames). Both are the same gap: *enumerate + filter paths under a root* with no
blank shell.

## Scope (what the agent actually needs)
1. **find (read-only):** enumerate files under a root, filtered by name/glob/ext,
   optionally by depth. Print paths (one per line, stable sort). This is the 90%
   case (list figures, find `*.md`, locate a tool) and is **trivially safe**.
2. **prune (write, guarded):** delete a matched set, but ONLY within a declared
   confinement dir, dry-run by default. This is the risky half and carries the
   whole safety burden.

NB: for the concrete case that spawned this (ssg copying 30 stale figures) the
real fix was **reference-aware ssg** (SM029, done) - the generator prunes its own
output. `tt find --prune` is the GENERAL tool for the next such case, not a
retrofit of that one.

## Design (pure JDK, no dep - dependency-cascade tier 1)
- Engine: `java.nio.file.Files.walk` / `Files.find` + a `PathMatcher`
  (`glob:**/*.md`). Zero dependency; walks lazily; symlink handling explicit
  (`FileVisitOption` - default do NOT follow symlinks, so a symlinked dir can't
  smuggle the walk outside the root).
- Shape (proposal): `tt find <root> [--name <glob>] [--ext <e>] [--type f|d]
  [--max-depth N]` prints matches. `tt find <root> --name <glob> --prune
  [--confine <dir>] [--yes]` deletes.
- **Safety model for `--prune` (mirrors `mirror.sc`'s rmrf guard):**
  - `--confine <dir>` is REQUIRED for prune; every target must
    `normalize().startsWith(confine.normalize())` or the whole run aborts (no
    partial deletes). Prevents `../` escape and absolute-path smuggling.
  - **Dry-run by default:** prints "would delete N files" + the list; actual
    deletion needs an explicit `--yes` (or `--apply`). So an un-flagged prune is
    always a no-op preview.
  - Refuse to prune the confinement root itself, refuse `/`, refuse `$HOME`, refuse
    a git-repo root (presence of `.git`) unless the confine dir is deeper.
  - No `-exec`, no shell-out, no arbitrary predicate - the tool exposes ONLY
    name/ext/type/depth. That bounded surface is what makes it allowlistable.

## Allowlisting payoff
`tt find <root> ...` (read-only) is allowlist-safe today under the existing
`Bash(tt *)` grant - it can only enumerate + print, never mutate. The `--prune
--yes` form is the one place a human might want a narrower allow entry (or leave it
prompting); everything short of `--yes` is a safe preview. So the tool splits the
old un-allowlistable `find` into a **safe-always read half** and a
**guarded-explicit write half** - the general pattern of the whole toolbox.

## Recommendation
BUILD as a tt-leaf (item-D family), read-half first:
1. `tt find <root> [--name/--ext/--type/--max-depth]` - pure, tested (matcher +
   sort + depth), immediately useful, ships alone.
2. `tt find ... --prune --confine <dir>` (dry-run default) + `--yes` - a second
   PR once the read half is in; the confinement guard + dry-run get their own
   tests (escape attempt aborts; no `--yes` mutates nothing).
Composes with: SM026 (`tt table` consumes file lists), ssg's figure handling,
the general "enumerate paths" need across the toolbox. No external dep; no
dispatcher-unpark needed (clean leaf, like `tt serv`/`tt ssg`).
