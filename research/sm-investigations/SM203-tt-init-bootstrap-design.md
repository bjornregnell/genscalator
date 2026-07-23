# SM203 — `tt init`: making `tt` the default agent behaviour in a fresh plugin-active repo

2026-07-24 00:07, agent-drafted (safe-solo). Status: DESIGN PROPOSAL for BR's ratification;
nothing here is built. Evidence base: the SM203 pin + its ADDENDUM (crux resolved via
claude-code-guide against the code.claude.com docs, 2026-07-23), the `gs-dwim` skill, the
guard-clean digest, and the SM205 `tt scala` narrow-allow precedent.

## The one-screen summary

BR observed: in a blank repo with the genscalator plugin active, the agent regresses to raw
`git`/`grep` and `tt forge` is not even discoverable. Three surfaces, none carried by "plugin
active" alone: (1) auto-**memory** is project-path-scoped, so the tt-reflex memories don't load
elsewhere; (2) **settings** carry no `Bash(tt *)` allow; (3) **discovery** — the `tt` launcher
resolves its tools dir to its own script-dir, not cwd. The crux is the turn-zero regression:
plugin **skills are lazy**, so the first git action regresses before any skill wakes.

The verified CC facts settle the mechanism: a plugin **cannot** ship an always-on
CLAUDE.md-equivalent (only lazy skills, agents, hooks, MCP, `bin/`), and plugin `settings.json`
carries only an `agent` key — **not** permissions/env. But a plugin **can** register a
**SessionStart hook**, and a **project** `CLAUDE.md` **is** always-on. So the always-on-ness
must come from a file the PROJECT holds, planted by one command, and the plugin's only always-on
lever (the hook) exists to make sure that command gets run. That command is **`tt init`**.

## The design: three parts

### 1. `tt init` — the deterministic installer (new toolbox verb, EFFECTFUL)

`tt init [dir]` (default cwd) scaffolds a repo's Claude Code config so tt is the default:

- **Writes `CLAUDE.md`** (project root, or appends a fenced block if one exists) carrying the
  **tt tool-lane primer** — the always-on reflex arming distilled from the guard-clean digest:
  use `tt git` not raw git, `tt text`/`tt files` not grep/find, `tt web get` not curl,
  `tt scala test|compile|package-js` not raw scala-cli; the dir-first `grepr` signature; one
  bare allowlist-matchable command per call. Kept SHORT (it rides every turn's context).
- **Writes `.claude/settings.json`** with the NARROW `tt` allowlist — `Bash(tt text *)`,
  `Bash(tt files *)`, `Bash(tt git *)`, `Bash(tt chrono *)`, `Bash(tt web *)`, `Bash(tt forge *)`,
  and the SM205 per-verb `Bash(tt scala test|compile|package-js *)` — and NOT the blanket
  `Bash(scala-cli *)` (ties SM205/SM073: the whole point is narrow allows, never the interpreter).
- **Idempotent + no-clobber.** If `CLAUDE.md`/`settings.json` exist, MERGE (append a marked
  `<!-- tt-primer -->…<!-- /tt-primer -->` block; union the allow array) or report "already
  present", never overwrite human content ([[no-clobber-human-owned-files]]). Re-running is a
  no-op. A `--check`/`--dry-run` prints what it WOULD write.
- **Pure core, effectful driver** (scala-style §3): a pure `plan(existing) -> edits` computes the
  merge; the effectful driver reads/writes the files. So the merge logic is unit-testable, like
  `tt scala`'s `plan`.

### 2. `gs init` — the in-session DWIM front

The `gs-dwim` skill already lists `gs init` as a trigger. Wire it to run `tt init` do-what-I-mean
(nearest-in-meaning), so a user (or the agent, on demand) types `gs init` and the repo is armed.
This is the human-invoked path.

### 3. The plugin SessionStart hook — the always-on turn-zero lever

A `hooks/hooks.json` `SessionStart` entry (the one always-on thing a plugin ships) fires at turn
zero in any plugin-active repo. It checks whether the repo is armed (marker: the `tt-primer`
block in `CLAUDE.md`), and if not, emits ONE line into context: *"genscalator plugin active but
this repo isn't armed — run `gs init` to make `tt` the default."* This closes the gap that the
plugin cannot ship the always-on CLAUDE.md itself: the always-on HOOK bootstraps the always-on
CLAUDE.md that `tt init` writes.

- **Nudge, not auto-run.** The hook should NOT silently scaffold files — that mutates the user's
  repo without consent ([[no-interrupting-modals-during-flow]], human-controls-the-work). It
  surfaces the one-command fix; the human runs it. (A future `--auto` opt-in could be a separate
  decision.)

### 4. Discovery — `tt` on PATH + walk-up-from-cwd (the third surface)

- Ship `tt` as the plugin's **`bin/`** executable so it lands on PATH in any plugin-active repo
  (no manual install), fixing "`tt forge` not seen".
- The standing **walk-up-from-cwd** tool-resolution fix (`wr-data/genscalator-self-dev.md`) so
  `tt <tool>` resolves the repo-local (or canonical) tools from any cwd — the same gap that bit
  `buildnative.sc` this session (it resolved the work-repo subset; SM203 in the wild).

## Requirements, each traceable

- R1. **Always-on arming without a per-project commit-by-hand.** Met by: the SessionStart hook
  (always-on, plugin-shipped) nudging `tt init`, which plants the project CLAUDE.md (always-on).
- R2. **Narrow allows, never the interpreter.** `tt init` installs per-verb `tt` allows incl.
  `tt scala` (SM205), never `Bash(scala-cli *)`. Ties SM073.
- R3. **No-clobber idempotency.** Merge into existing CLAUDE.md/settings via marked blocks; never
  overwrite; re-run = no-op; `--dry-run` first.
- R4. **Discovery from any cwd.** `bin/` on PATH + walk-up-from-cwd resolution.
- R5. **Short primer.** The CLAUDE.md block is distilled (rides every turn); link out to the full
  guard-clean digest rather than inlining it.

## What this note deliberately does NOT decide (BR's calls)

1. **CLAUDE.md vs AGENTS.md.** CLAUDE.md is what CC auto-loads; recommend it primary, optionally
   also emit `AGENTS.md` + a `@AGENTS.md` import for cross-tool portability. BR's call whether to
   bother with AGENTS.md now.
2. **Exact primer content + length** — the distilled tool-lane block is voice/scope-sensitive
   (BR authors the reflex canon). Draft = agent, ratify = BR.
3. **Which tt allows are in the default narrow set** — settings policy, human-gated
   ([[never-blanket-allow-settings-self-edit]]); `tt init` proposes, the set is BR-ratified.
4. **Hook nudge wording + whether an opt-in `--auto` scaffold ever exists.**
5. **Verify the exact plugin hook contract + settings keys** against the installed CC version
   before building (the crux facts are the guide's doc reading at one point in time —
   version-dependent).

## Build order if BR ratifies

Stage 1 — `tt init` with `--dry-run` (pure merge `plan` + tests, like `tt scala`), writing the
CLAUDE.md primer + narrow settings, no-clobber. Stage 2 — wire `gs init` to it. Stage 3 — the
plugin `SessionStart` hook (nudge) + `bin/` on PATH + walk-up-from-cwd. Stage 1 is self-contained
and the highest-value slice (a fresh repo becomes armable with one command). Everything that
ships the plugin or edits settings defaults stays BR-gated.

Ties: SM203 (this), SM205 (`tt scala` narrow-allow precedent), SM073 (settings review),
SM189/SM190 (plugin release/version story), the `gs-dwim` skill, the guard-clean digest,
`wr-data/genscalator-self-dev.md` (cross-repo resolution), `wr-data/active-skill-still-cold-starts-dormant-reflexes-regress-2026-07-13.md`.
