# The compaction bing-bing (the Compact sleep remedy)

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
  human is called straight back. The notice is titled `genscalator:` (mimicking the statusline brand) and carries a timestamp so an away
  human sees when it fired.

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
   toggle time. The **first** `on` (an activation) also fires a one-off **labelled preview** (`compact-wake.sh
   demo`) so you immediately see and hear what you enabled, at the moment of consent; it doubles as a functional
   test, since if this box has no notifier or sound player the preview says so.

## Requirements

Desktop Linux with `notify-send` and a sound player (`canberra-gtk-play`, or swap in `paplay`). The script
is fail-soft: with no display or no audio it silently skips the notice and still writes the timing log.

## Why opt-in and off by default

The notice is intrusive by design (a critical popup that bypasses Do-Not-Disturb, plus a chime). That is
exactly right for a wake-me-up, and exactly wrong to impose on someone who did not ask for it. So the noisy
half is gated on the sentinel; the silent timing log always runs.

## Plugin packaging (resolved 2026-07-13, from a claude-code-guide check)

**Decision: do NOT ship this as a plugin-declared hook.** Per current Claude Code behaviour, a plugin's
declared hooks fire even when the plugin is *disabled* (a known bug), so a "dormant until opt-in" plugin
hook cannot be relied on — that would break the opt-in / consent guarantee outright.

Instead, use the **opt-in-on-first-`on`** pattern: the plugin ships `compact-wake.sh`, and **`gs compact
notify on`** does the wiring, on the user's explicit command — it Edits the `Pre`/`PostCompact` hooks into
the user's own `~/.claude/settings.json`, points them at the script, creates the sentinel, and fires the
preview. Nothing runs until that first `on`.

Mechanics confirmed by the check:
- Hooks written to `settings.json` take effect **live** — no `/hooks` reload or restart needed.
- Do **not** rely on `${CLAUDE_PLUGIN_ROOT}` inside the hook command (currently injected unreliably);
  copy the script to `~/.claude/compact-wake.sh` (or use a fixed absolute path) so the hook command is robust.
- The `manual` + `auto` matcher pair is correct (or a single `manual|auto`); omitting the matcher matches all.
