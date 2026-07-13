# The compaction wake-me-up poll (the Compact sleep remedy)

A tiny personal hook that fixes **Compact sleep** (see `foundations.md`): a `/compact` takes long, so
mid-flow the human wanders off, and nothing calls them back because the agent stays dormant after the
compaction finishes until the human next types. Two costs stack: the away-interval becomes dead idle
time bolted onto the compaction, and the interruption cools the very flow the compact dance meant to
preserve.

## What it does

A `Pre`/`PostCompact` hook pair calling `compact-wake.sh`:

- **PreCompact** stamps the compaction **start** to `~/.claude/compact-timing.log`.
- **PostCompact** stamps the **end**, and (only when opted in) fires a **critical desktop notification**
  (which pierces Do-Not-Disturb) plus a **chime**, the instant the fresh context is ready, so a wandered-off
  human is called straight back. The notice carries a timestamp so an away human sees when it fired.

Because PRE and POST are stamped by the harness itself (not by a human around the compact), their delta is
the **pure summariser run** with zero human latency in it. That is the clean measurement the by-hand
`tmp/compact-chrono-stamps.md` protocol could never give, and the way to finally test "is a compact slower
the fuller the context is?".

## Setup (personal, per machine)

1. Copy `compact-wake.sh` (next to this doc) to `~/.claude/compact-wake.sh` and make it executable
   (`chmod +x`).
2. Merge these hooks into `~/.claude/settings.json` (both `manual` and `auto` so it fires on a hand-run
   `/compact` and on an auto-compaction):

   ```json
   "hooks": {
     "PreCompact": [
       { "matcher": "manual", "hooks": [{ "type": "command", "command": "bash /home/YOU/.claude/compact-wake.sh pre" }] },
       { "matcher": "auto",   "hooks": [{ "type": "command", "command": "bash /home/YOU/.claude/compact-wake.sh pre" }] }
     ],
     "PostCompact": [
       { "matcher": "manual", "hooks": [{ "type": "command", "command": "bash /home/YOU/.claude/compact-wake.sh post" }] },
       { "matcher": "auto",   "hooks": [{ "type": "command", "command": "bash /home/YOU/.claude/compact-wake.sh post" }] }
     ]
   }
   ```

   If `settings.json` did not exist at session start, open `/hooks` once (or restart) so the settings
   watcher picks the hooks up.
3. Opt in to the notification with **`gs compact notify on`** (silent by default). `gs compact notify off`
   silences it again; bare `gs compact notify` reports the state. The toggle just creates or removes the
   sentinel `~/.claude/compact-notify.enabled` that `compact-wake.sh` reads, so no settings edit happens at
   toggle time.

## Requirements

Desktop Linux with `notify-send` and a sound player (`canberra-gtk-play`, or swap in `paplay`). The script
is fail-soft: with no display or no audio it silently skips the notice and still writes the timing log.

## Why opt-in and off by default

The notice is intrusive by design (a critical popup that bypasses Do-Not-Disturb, plus a chime). That is
exactly right for a wake-me-up, and exactly wrong to impose on someone who did not ask for it. So the noisy
half is gated on the sentinel; the silent timing log always runs.

## Open question (for BR)

Promote this to a **genscalator-shipped plugin hook** (auto-available when the plugin is enabled, still
sentinel-gated and off by default), or keep it as this documented personal install? Shipping it auto-wires
a compaction hook for every plugin user, which is a UX and consent call, not just a packaging one. Parked
here until decided.
