# Settings mirror (SM073)

A tracked MIRROR of the Claude Code permission settings governing the agent in the
private work repo (`genscalator-work/.claude/settings.local.json` — the repo was named
`muntabot-synch-introprog` until 2026-07-24). The live
file is HUMAN-approved config and stays out of any public repo history except through
this deliberate mirror, refreshed by the agent whenever an approved change lands
(memory rule: settings-local-mirror). Git history here IS the audit trail — no
retro-editing, each refresh is one commit.

Why it lives in research/: the allowlist is primary Workflow-Research data — it shows
which command shapes earned standing trust, which stayed human-gated, and how that
boundary moved over time (ties the hardening dance, guardcheck, and the
never-blanket-allow rules).

- `settings.local.json` — the current mirror (see git log for the change trail).

⚠ **The trail starts BEFORE this directory.** The mirror lived at `research/wr-data/settings-local-mirror.json`
until 2026-07-19; this directory took over on 2026-07-21 (SM073). The older file was deleted on 2026-07-26
rather than moved here, because git retains its history and a frozen snapshot sitting beside a live file
invites someone to read the stale one. **So the audit trail is two paths, not one** — to replay it whole:

```
git log -p -- research/settings-mirror/settings.local.json research/wr-data/settings-local-mirror.json
```

⚠ Note also that the file is deliberately NOT dated in its name: it is a LIVING mirror, continuously synced,
whose value is the history rather than any one snapshot. A date would describe it as a point-in-time artifact,
which is exactly what it is not.
