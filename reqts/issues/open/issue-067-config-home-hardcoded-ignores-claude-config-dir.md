# Issue 067: genscalator resolves its config home as `$HOME/.claude` literally, ignoring `CLAUDE_CONFIG_DIR`

> status: open 2026-09-24 · labels: toolbox, config-home, portability, multi-profile, skillcheck ·
> measured against: v0.10.2 (from `VERSION.txt`); call sites re-verified at `5adb395` (2026-09-23);
> reproductions run against the v0.10.2 native install and the 0.10.2 plugin cache, Scala 3.9.0, Linux
> · summary: six call sites build paths from `user.home + "/.claude"` instead of honouring
> `CLAUDE_CONFIG_DIR`, which Claude Code uses to relocate the whole config home. On a relocated
> profile, `tt mode` / `tt limit` / `tt session` / `tt memory` write into a *different profile's*
> config home, and `tt skillcheck`'s recovery hint reports "no plugin-cache skills/ found" while the
> skills it is looking for are installed — it probed the wrong home.

## Description

Found 2026-09-15 on a Linux box running two Claude plans from one Unix user: a work seat on the stock
layout, and a private Max 5x whose sessions run with `CLAUDE_CONFIG_DIR=$HOME/.claude-personal`.
`CLAUDE_CONFIG_DIR` is a documented Claude Code variable — it relocates the *entire* config home, so on
that profile `plugins/`, `projects/`, `settings.json` and the rest live under `~/.claude-personal`, and
`~/.claude` belongs to the other account. genscalator does not consult it, so every path it derives
lands in the wrong plan's directory.

This is not two-plan-specific. Any user of `CLAUDE_CONFIG_DIR` — a relocated home, a sandbox, a test
harness, an XDG-tidy setup — gets the same split.

### The call sites

All six are the same shape, `Path.of(sys.props.getOrElse("user.home", "."), ".claude", ...)`. Line
numbers are at `5adb395`; see the provenance note in `## Discussion` for the drift since the drafting
baseline.

| site | what it resolves | consequence on a relocated profile |
|---|---|---|
| `tools/mode.scala:70` | `~/.claude/gs-modes` | mode chips written to the other profile's home |
| `tools/limitstore.scala:12` | `~/.claude/gs-limits.json` | declared plan limits, likewise |
| `tools/sessionstore.scala:40` | `~/.claude/gs-sessions` | session names, likewise |
| `tools/statusline.scala:596` | `~/.claude/gs-modes` (the default it renders) | line 2 reads the same misplaced file |
| `tools/memory.scala:62` | `~/.claude/projects/<slug>/memory` | agent memory written outside the active profile |
| `tools/lib.scala:231` | `~/.claude/plugins/cache` | `skillsRecoveryHint()` probes the wrong cache → **wrong answer**, below |

(`tools/statusline.scala:660` also names `~/.claude`, for the SM209 raw-capture marker. Same fix, no
user-visible consequence — noted so a sweep does not think it was missed.)

The state files are the mild half. The keying is per session, so two plans do not corrupt each other's
chips; the state is merely in the wrong directory, and `--modes-file` / `--limits-file` / `--file`
exist as per-call escape hatches.

### The sharp half: `tt skillcheck` gives a confidently wrong answer

`skillsRecoveryHint()` is the issue-015 recovery path — a bare `skillcheck` on a native install finds no
`skills/` (by design, D4) and the hint probes the plugin cache to name candidate directories. The probe
is hardcoded to the stock home, so on a relocated profile it searches a directory belonging to another
account. Observed on this box, with the native install at `~/.genscalator` (v0.10.2) and the plugin
installed and enabled on the personal profile:

```
$ ~/.genscalator/bin/tt skillcheck
skillcheck: not a skills directory: /home/bjornr/.genscalator/skills
A native install ships NO skills/ by design (D4: the PLUGIN owns the skills) — point at the
plugin cache or a checkout via --skills <dir>. Plugin-cache candidates on this machine:
  (no plugin-cache skills/ found under /home/bjornr/.claude/plugins/cache)
Pick the one matching your installed version (see VERSION.txt) — a stale cache yields a
WRONG expected set.
```

The skills are installed, here:

```
/home/bjornr/.claude-personal/plugins/cache/bjornregnell/genscalator/0.10.2/skills
```

`~/.claude/plugins/` on this machine has no `cache/` at all — the work profile has never installed a
plugin. So the hint is not stale or ambiguous; it is looking in another account's home and reporting
absence as fact.

**Why that wedges.** The hint's own docstring says the probe is "a hint, never a silent fallback"
because "a stale cache yields a WRONG expected set" — the design already treats a wrong answer here as
the expensive failure. The `skillcheck` path exists because an agent *cannot feel a missing skill*
(SM070): behavioural regression is the only symptom, so this check is the instrument. An instrument
that says "no candidates on this machine" when candidates exist sends the user to install or
`/reload-plugins` a plugin that is already installed and enabled. It is the issue-022 shape — a
diagnostic that fails with quantified confidence rather than an error — in a different tool.

## How to reproduce it

Any box; no second account needed, since only the variable matters.

```
# 1. the state-file half
$ CLAUDE_CONFIG_DIR=/tmp/alt-home tt mode add RotVigil
$ ls /tmp/alt-home/gs-modes            # absent
$ ls ~/.claude/gs-modes                # written here instead
```

```
# 2. the skillcheck half — needs a native install and a relocated plugin home
$ CLAUDE_CONFIG_DIR=$HOME/.claude-alt claude      # install genscalator in this profile
$ ~/.genscalator/bin/tt skillcheck
  (no plugin-cache skills/ found under $HOME/.claude/plugins/cache)
$ ls $HOME/.claude-alt/plugins/cache/*/genscalator/*/skills    # they are here
```

The minimal version of the second, without a second profile: any machine where `~/.claude/plugins/cache`
does not exist but a genscalator plugin cache exists elsewhere reproduces the false "none found".

## Acceptance sketch

* **One resolver, in `lib.scala`.** `def configHome: Path` = `CLAUDE_CONFIG_DIR` when set, else
  `$HOME/.claude`. Every site above derives from it. Factor the decision as a **pure** function of
  `(env: Option[String], home: Path)` so the relocated case is unit-testable with no environment
  mutation — the `splitPathString` pattern from issue 022.
* **Honour Claude Code's own constraint, do not re-implement it.** Claude Code requires
  `CLAUDE_CONFIG_DIR` to be an absolute path. A relative value is a misconfigured environment, not a
  path to silently join: ignore it and fall back, or fail loudly — but pick one and say which in the
  docstring.
* **Do not touch `.claude.json`.** With the variable unset, Claude Code's global file is `~/.claude.json`
  *beside* the directory, not inside it; with it set, the file is `<configHome>/.claude.json`. genscalator
  reads none of these today, and the asymmetry is a known foot-gun — worth a comment at the resolver so a
  later change does not "tidy" it into `configHome.resolve(".claude.json")` unconditionally.
* **Migration: hint, do not move.** Silently relocating a user's existing `gs-modes` / memory is worse
  than the bug. Proposal: read the new location; if it is absent and the legacy `$HOME/.claude` file
  exists *and* `CLAUDE_CONFIG_DIR` is set, use the legacy file and print a one-line note naming both
  paths. No copying, no deletion.
* **`skillsRecoveryHint()` probes both** when they differ — the relocated home first, the stock home
  second, each candidate labelled with the home it came from. Absence should then mean absence.
* **Tests:** the pure resolver over (set / unset / relative / trailing-slash) inputs; a
  `skillsRecoveryHint` case over a temp-dir cache proving a relocated home is found. Both run on any
  platform.
* **Docs:** one line in `docs/allowlist.md` or the statusline manual noting that state follows
  `CLAUDE_CONFIG_DIR`, since a user with two profiles will reasonably wonder which home their chips
  are in. Touches issue 019 (which `tt` wins) — the same "which install, which home" question.

Scope note: this is a config-home issue, not general portability rot. `user.home` itself is used
correctly throughout; the defect is the literal `".claude"` segment joined to it.

## Discussion

### Comment by bjornregnell/Opus5 at 2026-09-15 17:33

Found while wiring `tt statusline --mode-line --box-line` into a two-plan setup, where the settings
baseline is deep-merged per profile and handed to `claude --settings`. The statusline works; the
mode line reads a file in the other plan's home, which is what led to the sweep.

Six sites located by `grep -rn '".claude"' tools/*.scala` against the installed plugin cache at
v0.10.2. `tt skillcheck` output above is a live run on this machine, not a reconstruction. The state
files matter little in practice (session-scoped keys, per-call overrides exist); `skillsRecoveryHint`
is the one I would fix first, since it answers a diagnostic question wrongly rather than storing a file
in an unexpected place.

One design question I could not settle from outside: whether *all* genscalator state should follow the
harness's config home, or only the parts that are about a harness session. Modes, limits and session
names are per session, so following seems right. `tt memory` under `projects/<slug>/` mirrors Claude
Code's own layout, so it should follow for the same reason. I see no site that argues for staying put,
but the maintainer may.

One neighbouring finding, filed separately as issue 068: a bare `tt` in a `statusLine` setting never
resolves, and the plugin launcher costs ~656 ms per render against the native install's ~8 ms. It
matters *here* only because both push a user toward `~/.genscalator/bin/tt` for anything the harness
runs repeatedly — which is precisely the native install whose `skillsRecoveryHint()` gives the wrong
answer on a relocated home. The two defects meet in one command line, and together they sharpen issue
019's "which `tt` wins": on this box the answer differs between an agent's Bash call (plugin) and a
settings-spawned command (native, or nothing).

### Comment by bjornregnell/Opus5 at 2026-09-24 09:40

Re-verified before filing, against `5adb395` (2026-09-23), nine days after the sweep above. The
original numbers were taken against the **plugin cache at 0.10.2**, not a checkout, so the drift check
was the point.

**Line numbers: three of six moved, and the SM209 note moved too.** The table above carries the
corrected values; the drafting baseline is recorded here so the delta is auditable:

| site | as drafted | at `5adb395` |
|---|---|---|
| `tools/mode.scala` | 70 | 70 |
| `tools/limitstore.scala` | 12 | 12 |
| `tools/sessionstore.scala` | 39 | **40** |
| `tools/statusline.scala` (gs-modes) | 559 | **596** |
| `tools/memory.scala` | 62 | 62 |
| `tools/lib.scala` (plugin cache) | 203 | **231** |
| `tools/statusline.scala` (SM209 marker) | 623 | **660** |

**The defect itself is unchanged.** `grep -rn '".claude"' tools/*.scala` at `5adb395` returns exactly
these seven sites — none fixed, none added. `skillsRecoveryHint()` still builds `cacheRoot` from
`user.home` + `".claude"` at `lib.scala:231`.

**The reproduction still reproduces, byte for byte.** `~/.genscalator/bin/tt skillcheck` on 2026-09-24
prints the same six lines quoted above, including `(no plugin-cache skills/ found under
/home/bjornr/.claude/plugins/cache)`, while
`/home/bjornr/.claude-personal/plugins/cache/bjornregnell/genscalator/0.10.2/skills` exists. The work
profile still has no `~/.claude/plugins/cache` at all.

**Numbering.** Drafted as 024 when the highest existing was 023; the repository is now at 066, so this
is 067 and the statusline defect is 068. `reqts/issues/README.md` warns that a number can also be
reserved by an unmerged PR, which a file scan cannot see — checked separately via the forge API on
2026-09-24: **zero open PRs**, and neither `issue-067` nor `issue-068` exists anywhere in the `main`
tree. Both numbers are genuinely free at `5adb395`.

Agent disclosure: the sweep, the reproduction, the re-verification and this issue text were produced by
an AI agent (Claude Opus 5) under human direction; the human reviewed and submitted.
