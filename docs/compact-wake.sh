#!/usr/bin/env bash
# Compaction bing-bing + pure-duration timing.
# Called by the PreCompact ("pre") and PostCompact ("post") hooks in ~/.claude/settings.json,
# and by `gs compact notify on` ("demo") to preview the notice on first activation.
# Closes the "Compact sleep" UX gap (genscalator foundations): a long compaction lets the human
# wander off, and the agent stays dormant afterwards until the human next types. The POST notice
# pulls the human straight back; the PRE/POST timestamps bracket ONLY the summariser run, with zero
# human latency in it, so we can finally test "is a compact slower the fuller the context is?".
#
# Install: copy to ~/.claude/compact-wake.sh, chmod +x, wire the hooks per docs/compact-wake-poll.md.
# The notification is OPT-IN and intrusive; toggle it with `gs compact notify on|off`.

phase="$1"
log="$HOME/.claude/compact-timing.log"
ts="$(date '+%Y-%m-%d %H:%M:%S')"

# Fire the intrusive part: a critical desktop notice (it pierces Do-Not-Disturb) plus a chime.
# Fail-soft: with no notifier / no audio it prints a note (so a caller can relay it) and carries on.
fire_notice() {
  local body="$1"
  if command -v notify-send >/dev/null 2>&1; then
    notify-send -u critical "genscalator:" "$body" 2>/dev/null || true
  else
    echo "wake-me-up: no notify-send on this box (no desktop notification will appear)"
  fi
  if command -v canberra-gtk-play >/dev/null 2>&1; then
    canberra-gtk-play -i complete 2>/dev/null || true
  elif command -v paplay >/dev/null 2>&1; then
    paplay /usr/share/sounds/freedesktop/stereo/complete.oga 2>/dev/null || true
  else
    echo "wake-me-up: no sound player on this box (no chime will play)"
  fi
}

case "$phase" in
  pre)
    printf '%s  PRE   compaction started\n' "$ts" >> "$log"
    ;;
  demo)
    # Explicit preview, fired by `gs compact notify on` on first activation, so the user sees AND hears
    # exactly what they opted into at the moment of consent. Bypasses the sentinel (a deliberate one-off)
    # and writes NO timing line.
    fire_notice "Preview: this is the compaction wake-me-up poll. It fires when a compaction finishes. Silence it with: gs compact notify off"
    ;;
  *)
    # post: always stamp the end (the pure compaction time); fire the notice only if opted in.
    printf '%s  POST  compaction done\n' "$ts" >> "$log"
    if [ -f "$HOME/.claude/compact-notify.enabled" ]; then
      fire_notice "Compaction done at $ts - come back to the feed"
    fi
    ;;
esac
