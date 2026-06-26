# Updating genscalator

genscalator distributes **two kinds of thing**, and updating is different for each:

- **Code** — the `tt` tools (`tools/*.scala`). A normal software artifact.
- **Operating rules** — `AGENTS.md` + the skills. These *are* the agent's modus operandi.

**Golden rule:** updating the operating rules is a **human-reviewed** step. An agent must never silently
pull and adopt new rules for itself — rewriting an agent's own operating instructions from a remote source
is exactly the supply-chain / persistence threat genscalator's model exists to prevent (see
[`foundations.md`](foundations.md), BHH BadGoals). Skim [`../CHANGELOG.md`](../CHANGELOG.md), then update
deliberately.

## Am I current?
- **Single source of truth:** the latest git tag (`vX.Y.Z`) on the genscalator repo.
- **Tracked in:** `.claude-plugin/plugin.json` + `marketplace.json` (`version`), and the version line at the
  top of [`../AGENTS.md`](../AGENTS.md) (so even a vendored, non-Claude setup can read its version).
- Compare your version line against the latest tag / `CHANGELOG.md` heading upstream.

## Mode 1 — installed as a Claude Code plugin
Updating is a **git-pull of the marketplace repo**, human-initiated:
```
/plugin marketplace update bjornregnell    # re-pull the marketplace (catalog + plugin)
/reload-plugins                            # activate — Claude Code prompts for this after an update
```
Notes:
- **Auto-update is OFF by default for third-party marketplaces** (on only for official Anthropic ones), so
  genscalator updates won't land at startup unless you opt in via the `/plugin` UI → Marketplaces →
  *Enable auto-update*. Leaving it off is the safe-by-design default: rule changes are reviewed, not silent.
- There is **no "new version available" notification** — only a "Last updated" date in plugin details. Pull
  when you choose to; check `CHANGELOG.md` first.
- After update you **must `/reload-plugins`** for the new skills / `bin/tt` PATH entry to take effect.

## Mode 2 — vendored (Codex, opencode, or any non-plugin setup)
If you copied `AGENTS.md`, the skills, and `tools/` into your project, treat genscalator as a **vendored
dependency**:
1. Check the upstream tag / `CHANGELOG.md` against your `AGENTS.md` version line.
2. Pull the new `AGENTS.md`, `skills/`, and `tools/` from the genscalator repo at the chosen tag.
3. **Review the diff** (especially `AGENTS.md` + skills — they change how the agent behaves).
4. The agent adopts the new rules simply by **re-reading the updated files next session** — no other step.

## After updating (either mode)
Verify the toolbox still runs, e.g.:
```
tt files src .scala --count
```
If a tool changed, re-check it against `CHANGELOG.md`.
