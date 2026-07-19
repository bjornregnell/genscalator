# Using genscalator as a Claude Code plugin

This repo doubles as its own **plugin marketplace**, so adopters get the `tt` toolbox on PATH and the
toolbox-habit skill in one step — no manual symlink.

> Plugin packaging is **Claude Code-specific**. The tools themselves are just `scala-cli` scripts and
> work in any agent that can run a shell command; `AGENTS.md` carries the workflow to other tools. The
> portable cross-tool path (Claude / Codex / opencode) is an MCP server — see the README roadmap.

## What the plugin ships
- **`bin/tt`** — added to the Bash tool's PATH while the plugin is enabled, so the agent runs `tt …`
  with no setup. It delegates to the bundled `tools/tt` and sets `TT_TOOLS` to the bundled `tools/`.
- **`skills/tt-toolbox/SKILL.md`** — teaches the habit (prefer `tt` over bash/grep/awk; one bare command
  per call) and loads contextually when relevant.
- **`skills/scala-style/SKILL.md`** — how to *write* a tool when scaffolding one: direct style, state-safe,
  safe-mode-where-possible. Loads when editing/creating Scala tools.
- **`skills/contribute-tool/SKILL.md`** — how to *generalize* a scratch tool into a toolbox-worthy one and
  propose it upstream. Loads when an agent is about to contribute a tool.
- Manifests: `.claude-plugin/plugin.json` (the plugin) and `.claude-plugin/marketplace.json` (the catalog).

Still required on the user's machine (plugins can't install dependencies): **`scala-cli`** and a **JDK**.

## Install
```
/plugin marketplace add bjornregnell/genscalator
/plugin install genscalator@bjornregnell
```
**Why the short form works:**
- The short `owner/repo` form of `/plugin marketplace add` resolves to **github.com**, and the GitHub
  mirror `github.com/bjornregnell/genscalator` is pushed on every commit, so it is always current
  (and github.com has steadier uptime than codeberg.org).
- The canonical Codeberg repo also works, via the full URL:
  `/plugin marketplace add https://codeberg.org/bjornregnell/genscalator.git`. Here the **`.git` suffix**
  tells Claude Code to *clone* the repo (so it finds `.claude-plugin/marketplace.json` inside it) rather
  than treating the URL as a direct link to a hosted `marketplace.json`.

The install token is **`<plugin>@<marketplace>`** — here the plugin `genscalator` (its `name` in
`plugin.json`) from the marketplace `bjornregnell` (its `name` in `marketplace.json`). It is *not* the
`owner/repo` git path.

Then verify the agent can run e.g. `tt files src .scala --count`.

## Recommended allowlist (opt-in, curated)
Plugins do **not** silently grant Bash permissions — and that's deliberate (see
[`confirmations-method.md`](confirmations-method.md): curate the allowlist as reviewed code, not via
in-the-moment "always allow" clicks). The skill pre-approves the narrow `tt` commands while it's active;
to make them always-allowed, add **narrow, per-subcommand** entries to your own `.claude/settings.json`:
```json
{
  "permissions": {
    "allow": ["Bash(tt text *)", "Bash(tt files *)", "Bash(tt log *)", "Bash(tt verify *)", "Bash(scala-cli run *)", "Bash(scalex *)"]
  }
}
```
Keep entries per-subcommand (`Bash(tt text *)`, not `Bash(tt *)`) so each grant stays tightly scoped.
`scalex` is read-only Scala code intelligence (no writes, no build server), so `Bash(scalex *)` is a
low-stakes grant; narrow it per-subcommand (`Bash(scalex explain *)`, `Bash(scalex refs *)`, …) if you
prefer to start tighter.
`tt verify` is *effectful* (it runs commands), but `Bash(tt verify *)` is still safe to blanket-allow
**by design**: it executes only its allowlisted executables (`scala-cli`/`tt`/`scalex` + your
`TT_VERIFY_ALLOW`) directly as argv with **no shell**, so an approved `tt verify *` can't become a general
exec/`bash -c` surface. To widen what it may run, set `TT_VERIFY_ALLOW` in your shell profile (not a flag).

## Companion plugin: scalex
For Scala **code** navigation, genscalator recommends **[scalex](https://github.com/nguyenyou/scalex)**
(symbol-aware "grep for the AST"). It's a *separate* upstream plugin — install it alongside genscalator,
it is not bundled:
```
/plugin marketplace add nguyenyou/scalex
/plugin install scalex@scalex-marketplace
```
Which tool for which question — `tt` (text/logs) vs `scalex` (Scala structure) vs Metals MCP (compiler
semantics): [`tool-selection.md`](tool-selection.md). Command reference: [`../tools/README.md`](../tools/README.md#companion-scalex).

## Optional: a "nudge" hook (roadmap, not shipped)
A `PreToolUse` hook on `Bash` could advise switching to `tt` when it sees a raw `grep`/`awk`/`sed`/`find`
in a command. It's left out of the default plugin to avoid false positives and per-call overhead; wire it
in deliberately if wanted. Sketch (`hooks/hooks.json`, plus a script that reads the tool JSON on stdin):
```json
{
  "hooks": {
    "PreToolUse": [
      { "matcher": "Bash",
        "hooks": [ { "type": "command", "command": "\"${CLAUDE_PLUGIN_ROOT}\"/scripts/nudge-bash.sh" } ] }
    ]
  }
}
```

## Caveats to verify on first install
- **`CLAUDE_PLUGIN_ROOT` in `bin/` scripts** is documented for hooks/MCP; `bin/tt` also falls back to
  resolving `tools/` relative to itself, so it works either way.
- **`allowed-tools` grammar** in `SKILL.md` (space-separated `Bash(tt text *) …`) — confirm it pre-approves
  as expected; adjust to an array if your Claude Code version prefers that.
