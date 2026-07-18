# Very long thinking latency after a ~3h silence, likely a stale local session (2026-07-18)

**BR, morning of 2026-07-18 (~11:08 CEST).** After an overnight safe-solo drill (the SECURITY-MODEL rewrite
etc.), on a trivial cue ("looks good! ... start co-design of web page"), the agent's thinking ran **>= 8
minutes**, and several tool calls returned **"Tool result missing due to internal error."**

## ✅ RESOLVED 2026-07-18 ~11:54 — root cause was a WEDGED BLOOP DAEMON, not a stale Claude session

The "stale session / exit-resume" hypothesis below was **wrong** (kept per [[keep-the-ball-game-retract-by-annotating]]; the sections after this banner are the live investigation trajectory, some of it falsified — this banner is authoritative). Live joint diagnosis with BR established:

- **Text turns stayed fast (~2s model churn); every `Bash`/`tt` tool call hung for minutes.** ⇒ the failure was the **subprocess/scala-cli lane, not the model** and not the Claude Code session. `tt chrono now` hung in BR's **own terminal** too, proving it was machine-level, not harness-level.
- **Not memory:** `free -h` showed **11 Gi available, 0 B swap used**. OOM ruled out (against the blixten prior).
- **The wedge was the local `bloop` compile daemon.** `pkill -9 -f BloopServer` released it. The first fresh `tt chrono` surfaced the smoking gun: bloop-rifle writes a launcher to `/tmp/start-bloop*.sh` and it vanished (`sh: 0: cannot open … No such file`) — a corrupt bloop-rifle state accreted over the overnight run + 3h idle. (**Not** a disk-space issue: `df -h` showed `/` at 54% with 195 Gi free and no separate `/tmp` mount, so `/tmp` had ample space — the launcher vanished because bloop-rifle itself was wedged, not for lack of room. Disk was the third red herring after memory and exit-resume.) After the kill, a fresh bloop cold-started; `tt chrono` returned in **8.5s cold, then 0.63s warm**, in both BR's shell and the Claude Code tool lane.

**Why one wedged daemon froze the ENTIRE tool lane + the status line at once:** every `Bash` tool fires the guardcheck `PreToolUse` hook, which is a **per-call `scala-cli`** (→ bloop); the status line render is `scala-cli` too. So **bloop's health is a single point of failure for all tooling.** ⇒ **DESIGN FINDING (SM-worthy): the guardcheck hook should be a precompiled/native binary or a warm daemon with a hard timeout, so a wedged build layer cannot freeze every command.** Reinforces the earlier "hook needs a timeout" note.

**The instruments lied, consistently** (a sibling of guard-stall-invisibility, one layer up): the spinner relabeled `Hashing`/`Cascading`/`thinking some more` and **undercounted elapsed time** (it clocks model work, not subprocess-wait); the status-line clock froze at 03:11 because **its own render is a blocked `scala-cli`**. Both are structurally blind to subprocess stalls.

**Fix recipe (for next time):** `pkill -9 -f BloopServer` → `rm -f /tmp/start-bloop*.sh` if the launcher error persists → re-run any `tt` command to cold-start a fresh bloop. **Memory, exit-resume, and reboot were all red herrings.**

## The observation (status line, verbatim from BR)

```
genscalator  03:11:25  silent 3h  o4.8/1M  ctx-fill 36%  rot?↑640k  tot↑1.9M
```
- The **status-line clock is frozen at 03:11:25** while the real time is ~11:08 CEST. A frozen clock signals
  a turn in progress ([[statusline-clock-freeze-signals-ballgame-turn]]); frozen for ~8 hours is not a normal
  turn.
- `rot?↑640k` = a very large since-warp OUTPUT (the overnight drill); `tot↑1.9M` lifetime; `ctx-fill 36%`.

## Diagnosis

- **claude.ai reports Anthropic healthy today** (status.claude.com "All Systems Operational", no Jul-18
  incidents; yesterday's Fable-5 incident resolved 19:43 UTC). ⇒ **the hang is LOCAL**, not a platform outage.
- The **"internal error" on tool results** supports a stalled local session / streaming stall over a merely
  slow-reasoning agent. claude.ai's note that "stale sessions after the Fable-5 incident needed a relaunch to
  restore access" fits.
- **On the "hangover" question:** the agent has **no felt time** ([[agent-lacks-felt-time]]), so it cannot
  perceive a 3h silence as fatigue. Two candidate causes to keep distinct:
  1. **A local/stream stall** (claude.ai's read; the internal-errors support it) — an environment problem,
     fixed by relaunch.
  2. **Fill/output-driven latency** — lag is stall-dominated and worsens with load
     ([[instrument-deadlock-and-throughput]] family); 640k output since warp is heavy. But **8 minutes on a
     trivial cue exceeds** what fill alone explains, so (1) is the likelier primary cause.

## Remedy (agreed with claude.ai's advice)

**The exit-resume dance** ([[exit-resume-dance]]): Esc to interrupt, exit, `claude --resume` / `--continue`
for a fresh process, token, and stream. Sibling of the compact dance; the right move when the *process/stream*
is stale rather than the *context* being full.

## Ties
[[exit-resume-dance]] · [[agent-lacks-felt-time]] · [[agent-lacks-felt-time-rebind-at-boundaries]] ·
[[statusline-clock-freeze-signals-ballgame-turn]] · [[joint-rot-vigilance-recovery-kit]] · the throughput /
fill-latency wr-data notes.
