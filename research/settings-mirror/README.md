# Settings mirror (SM073)

A tracked MIRROR of the Claude Code permission settings governing the agent in the
private work repo (`muntabot-synch-introprog/.claude/settings.local.json`). The live
file is HUMAN-approved config and stays out of any public repo history except through
this deliberate mirror, refreshed by the agent whenever an approved change lands
(memory rule: settings-local-mirror). Git history here IS the audit trail — no
retro-editing, each refresh is one commit.

Why it lives in research/: the allowlist is primary Workflow-Research data — it shows
which command shapes earned standing trust, which stayed human-gated, and how that
boundary moved over time (ties the hardening dance, guardcheck, and the
never-blanket-allow rules).

- `settings.local.json` — the current mirror (see git log for the change trail).
