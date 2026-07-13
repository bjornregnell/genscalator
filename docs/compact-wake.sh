#!/usr/bin/env bash
# Compaction wake-me-up poll + pure-duration timing.
# Called by the PreCompact ("pre") and PostCompact ("post") hooks in ~/.claude/settings.json.
# Closes the "Compact sleep" UX gap (genscalator foundations): a long compaction lets the human
# wander off, and the agent stays dormant afterwards until the human next types. The POST notice
# pulls the human straight back; the PRE/POST timestamps bracket ONLY the summariser run, with zero
# human latency in it, so we can finally test "is a compact slower the fuller the context is?".
#
# Install: copy to ~/.claude/compact-wake.sh, chmod +x, and wire the hooks per docs/compact-wake-poll.md.
# The notification is OPT-IN and intrusive; toggle it with `gs compact notify on|off`.

phase="$1"
log="$HOME/.claude/compact-timing.log"
ts="$(date '+%Y-%m-%d %H:%M:%S')"

if [ "$phase" = "pre" ]; then
  printf '%s  PRE   compaction started\n' "$ts" >> "$log"
else
  printf '%s  POST  compaction done\n' "$ts" >> "$log"
  # The wake-me-up poll is OPT-IN, because it is intrusive (a critical notice that pierces Do-Not-Disturb,
  # plus a chime). It fires ONLY when the user has enabled it with `gs compact notify on`, which creates the
  # sentinel below; `gs compact notify off` removes it. The timing stamps above are silent and always run.
  if [ -f "$HOME/.claude/compact-notify.enabled" ]; then
    notify-send -u critical "Claude Code" "Compaction done at $ts - come back to the feed" 2>/dev/null || true
    canberra-gtk-play -i complete 2>/dev/null || true
  fi
fi
