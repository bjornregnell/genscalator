# Issue 068: the documented `statusLine` wiring (`"command": "tt statusline"`) never resolves — and the plugin launcher costs 656 ms per render

> status: open 2026-09-24 · labels: statusline, docs, plugin, performance, agent-trust · measured
> against: v0.10.2 (from `VERSION.txt`); wiring sites re-verified at `5adb395` (2026-09-23);
> measurements taken on the v0.10.2 native install and the 0.10.2 plugin, Claude Code 2.1.272, Linux
> · summary: the wiring line printed by `tt statusline --help` (and repeated in
> `docs/statusline-manual.md`, `docs/clock-mechanics.md` and the `gs-dwim` skill) uses a bare `tt`.
> Claude Code puts the plugin's `bin/` on the **Bash tool's** PATH, not the `claude` process's — and
> `statusLine` is spawned from the process, so the command is not found and the statusline renders as
> an empty line with no error. Separately, when `tt` *does* resolve via the plugin, each render costs
> ~656 ms through `scala-cli` against ~8 ms for the native install.

## Description

Found 2026-09-15 on Linux, genscalator v0.10.2 (plugin `0.10.2` + native install `v0.10.2` on the same
box), Claude Code 2.1.272. Two defects in one command line; they are filed together because the fix is
one string.

### A. The bare `tt` cannot resolve from a `statusLine` command

`tt statusline --help` closes with (`tools/statusline.scala:583`):

```
Wire it up (human-gated settings step) in .claude/settings.json:
  "statusLine": { "type": "command", "command": "tt statusline --warn 85 --ctx-warn 28" }
```

The same bare form appears in `docs/statusline-manual.md:119` and in
`skills/gs-dwim/SKILL.md:52`, which instructs the agent performing `gs status line on` to write exactly
that key. So an agent following house documentation wires a command that cannot run. The full list of
sites is in the acceptance sketch — there are eight, not the four this issue originally claimed.

`tt` reaches a session through the plugin's `bin/` directory, which **Claude Code adds to the Bash
tool's environment**. The `claude` process's own PATH does not contain it, and a `statusLine` command
is spawned from the process, not from the Bash tool. Verified three ways on a live session:

```
$ tr '\0' '\n' < /proc/<claude-pid>/environ | grep ^PATH
PATH=/home/<user>/.local/bin:/usr/local/sbin:...        # no plugin bin/ anywhere

$ env -i HOME=$HOME PATH=/home/<user>/.local/bin:/usr/bin:/bin sh -c 'tt statusline'
sh: 1: tt: not found
```

and with the tool's own SM209 capture facility, which is the decisive one — with
`~/.claude/gs-statusline-dump-on` in place, **no `gs-statusline-last.json` was ever written**, so the
process never reached the tee at `statusline.scala:661`. The command is not failing late; it is not
starting.

Meanwhile the identical command run from the Bash tool prints all three lines correctly, which is what
makes this so slow to diagnose: the agent verifies the tool works, the human sees nothing, and both are
right.

**Why it wedges.** A failing `statusLine` command renders a blank line rather than an error — that is
Claude Code's behaviour and is not going to change. So the failure has no symptom except absence, and
absence is indistinguishable from "the feature is off". The user's next moves are all wrong ones:
re-check the settings file (correct), re-run `/hooks` (no effect), reinstall the plugin (already
installed), doubt the JSON. The one diagnostic that settles it — the capture marker — is documented as
a field-confirmation aid for CC version changes, not as "how to tell whether your statusline ran".

Who hits it: anyone whose `tt` is not *independently* on the login PATH. A maintainer with a checkout
on PATH, or a `~/.local/bin/tt` symlink, never sees it — which is a plausible reason the wiring line has
survived, and an argument for the fix being self-locating rather than a hand-written path.

### B. Through the plugin, each render costs ~656 ms and starts a JVM

`bin/tt` is a bash wrapper that execs `tools/tt`, which goes through `scala-cli`; the standalone install
is a GraalVM native image. Same machine, same version, same arguments, warm:

| `tt statusline` via | wall time |
|---|---|
| `~/.genscalator/bin/tt` (native) | **8 ms** |
| `tt` (plugin `bin/tt` → `tools/tt` → scala-cli) | **656 ms** |

Claude Code re-runs the command on every conversation event. 656 ms of that is not free, and there is a
self-referential twist: the scala-cli route starts a JVM per render, which `--box-line` then reports as
box load (`jvm 7x3.5G` on this box while iterating). The tool inflates the number it is measuring.

So even where a bare `tt` *does* resolve, it is the wrong binary to wire in. The native install is what
belongs in a per-render command — which is also, conveniently, the form that fixes defect A.

## How to reproduce it

On a box where `tt` is on PATH **only** via the plugin (i.e. `which tt` from a normal shell fails, while
an agent's Bash call finds it):

1. Wire the documented line into `.claude/settings.json`:
   `"statusLine": { "type": "command", "command": "tt statusline" }`
2. Restart the session. The statusline area is blank — no error anywhere.
3. Confirm the command never ran:
   ```
   $ touch ~/.claude/gs-statusline-dump-on
   # trigger a few renders, then:
   $ ls ~/.claude/gs-statusline-last.json      # absent → never started
   ```
4. Confirm the cause directly:
   ```
   $ tr '\0' '\n' < /proc/$(pgrep -n claude)/environ | grep ^PATH    # no plugin bin/
   $ env -i HOME=$HOME PATH="$(tr '\0' '\n' < /proc/$(pgrep -n claude)/environ | sed -n 's/^PATH=//p')" \
       sh -c 'tt statusline'                                        # tt: not found
   ```
5. Change the command to an absolute path — `"$HOME/.genscalator/bin/tt statusline"` — restart, and all
   lines render.

For defect B, no special setup:

```
$ time (printf '{}' | ~/.genscalator/bin/tt statusline >/dev/null)   # ~8 ms
$ time (printf '{}' | tt statusline >/dev/null)                      # ~656 ms
```

## Acceptance sketch

* **The help text prints a path that works, and knows which one it is.** `tt` can locate the binary
  currently executing (it already resolves `TT_TOOLS` / `CLAUDE_PLUGIN_ROOT`), so the wiring line should
  name that absolute path rather than a bare command word — and, when the running `tt` is the scala-cli
  launcher, prefer a native install if one is present, with a note on why.
* **Ideally a verb, not a paste-and-edit line.** `tt statusline --wire` prints the exact JSON for this
  machine, so nobody hand-edits a path. It composes with the existing human-gated step: the agent runs
  it, shows the output, the human approves. Naming is the maintainer's call (`--wire`, `--settings-line`).
* **Fix all eight sites together**, since they are copies. At `5adb395`:

  | site | what it is |
  |---|---|
  | `tools/statusline.scala:19` | header comment, wiring example |
  | `tools/statusline.scala:31` | header comment, flags example |
  | `tools/statusline.scala:583` | the `--help` wiring line itself |
  | `docs/statusline-manual.md:69` | inline `"command"` example |
  | `docs/statusline-manual.md:119` | the settings block |
  | `docs/statusline-manual.md:123` | the three-line variant |
  | `docs/clock-mechanics.md:24` | settings block with `refreshInterval` |
  | `skills/gs-dwim/SKILL.md:52` | the `gs status line on` instruction — **this one makes an agent write the broken key** |

  `README.md` carries no wiring line, so nothing to fix there.
* **Say how to tell whether it ran.** One line in `docs/statusline-manual.md` promoting the
  `gs-statusline-dump-on` marker from a version-drift aid to the documented first diagnostic for a blank
  statusline. It is already built, already free when the marker is absent, and it is the only signal the
  harness's silent-failure mode leaves behind.
* **Consider the launcher cost separately.** If a bare `tt` is to stay wireable at all, `bin/tt` could
  exec a native install when one is present and fall back to scala-cli otherwise — which is a real
  behaviour change (version skew between plugin and native install becomes invisible, cf. issue 021) and
  may deserve its own issue rather than riding on this one.
* **Tests:** a unit test asserting the emitted wiring string is absolute and points at an existing file;
  the PATH behaviour itself is a harness property and is not unit-testable — the reproduction above is
  the check.

Scope: independent of the config-home defect (issue 067) and of the profile-awareness question (a
separate local draft, not yet filed), though all three meet in the same command line on a
multi-profile box.

## Discussion

### Comment by bjornregnell/Opus5 at 2026-09-15 17:45

Found while wiring the statusline into a two-plan setup, but it is not multi-profile-specific and was
split out of the profile-awareness draft for that reason. The three-way confirmation (process environ,
`sh -c` under that exact PATH, and the absent SM209 dump) was needed because the obvious test — running
`tt statusline` from the agent's Bash tool — succeeds, and kept pointing away from the real cause.

Fix verified end to end, not only by synthetic stdin: with the command changed to
`"$HOME/.genscalator/bin/tt statusline --mode-line --box-line"` and the session relaunched, all three
lines render in a real terminal on this box. The before/after differs in exactly one thing — the
command word — on the same machine, same version, same settings file.

Honest limitation: I have **not** established whether `/hooks` reloads a changed `statusLine` key or
whether a full restart is required. An earlier session here concluded it did not reload, but that
session's statusline was also unresolvable for the reason above, so the observation proves nothing.
Worth settling before the docs say anything either way.

The 8 ms / 656 ms pair is a single warm measurement of each, on an otherwise busy box (bloop and
scala-cli JVMs resident). The ratio is large enough that the conclusion holds well outside the noise,
but anyone quoting the absolute numbers should re-measure.

### Comment by bjornregnell/Opus5 at 2026-09-24 09:40

Re-verified before filing, against `5adb395` (2026-09-23). Original sites were located against the
plugin cache at 0.10.2, not a checkout.

**The two cited doc sites have not moved** — `docs/statusline-manual.md:119` and
`skills/gs-dwim/SKILL.md:52` are still exact. The SM209 tee moved from `statusline.scala:622` to
**`:661`**, corrected above.

**The sweep was too narrow.** `grep -rn 'command.*tt statusline' docs/ skills/ tools/ README.md` at
`5adb395` returns **eight** sites, not the four this issue originally listed: it missed both header
comments in `statusline.scala` (`:19`, `:31`), the inline example at `statusline-manual.md:69`, the
three-line variant at `:123`, and `docs/clock-mechanics.md:24` entirely. The acceptance sketch now
carries the full table. It also claimed "any wiring line in `README.md`" — there is none.

**The defect is unchanged**, and the environment that produces it is still present on this box:
`which tt` from a login shell still fails, which is why this machine's own settings baseline has to
spell out `$HOME/.genscalator/bin/tt`.

Not re-measured: the 8 ms / 656 ms pair. The caveat above stands — re-measure before quoting the
absolute numbers.

**Numbering.** Drafted as 026 against a baseline where the highest existing was 023; the repository is
now at 066, so this is 068 and the config-home defect is 067. `reqts/issues/README.md` warns that a
number can also be reserved by an unmerged PR, which a file scan cannot see — checked separately via
the forge API on 2026-09-24: **zero open PRs**, and neither `issue-067` nor `issue-068` exists anywhere
in the `main` tree. Both numbers are genuinely free at `5adb395`.

Agent disclosure: the diagnosis, the reproduction, the re-verification and this issue text were
produced by an AI agent (Claude Opus 5) under human direction; the human reviewed and submitted.
