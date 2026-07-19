# Getting started

[← Back to the manual home](index.html)

## Prerequisites

- [scala-cli](https://scala-cli.virtuslab.org/install) and a JDK (to run the tools)
- `git` (to clone the repo and for the safe `tt git` helper)
- **Platforms:** Linux, macOS, and WSL — anywhere `bash` and `scala-cli` run. On native Windows,
  use WSL or Git Bash.

## 1. Install the Claude Code plugin

In Claude Code, run:

```
/plugin marketplace add bjornregnell/genscalator
/plugin install genscalator@bjornregnell
/reload-plugins
```

The short form installs from the GitHub mirror (kept in sync on every commit). The canonical
Codeberg repo works too: `/plugin marketplace add https://codeberg.org/bjornregnell/genscalator.git`

Then verify with **`/skills`** — you should see `tt-toolbox`, `scala-style`, and the rest of the
set. If they do not show up yet, restart Claude Code and check again.

An active skill is *lazy*: it loads when its trigger fires, not necessarily at the first turn. To
confirm the set is present at any time, run:

```
gs skills
```

which lists the expected skills (derived from the plugin on disk) so you can reconcile them against
`/skills`.

## 2. Say hello to the do-what-I-mean commands

Type this in chat:

```
gs
```

You will get help on the `gs` commands. A leading `gs` plus *roughly* one of the listed intents is
enough — the agent matches the nearest command in meaning. A few to try:

- `gs help tt` — list the typed tools and what each does
- `gs status` — the status-line information, expanded into a table
- `gs cues` — the cues, human to agent and agent to human, and what they mean
- `gs where` — a short current-state snapshot (pin board, resume prompt, recent git log)

## 3. Let the typed tools run without a prompt

When the harness asks for permission, you can allow according to its suggestions. For a more
complete and precise allowlist, run:

```
gs allow
```

and the agent will set up the recommended two-tier allowlist for the current repo (shown to you;
you approve). See **[The recommended allowlist](allowlist.html)** for the tiers, the deny-list, and
the principles. You can also edit `.claude/settings.local.json` directly, for example:

```
{ "permissions": { "allow": ["Bash(tt *)", "Bash(scala-cli *)"] } }
```

## 4. Generate something

Seed a complete, runnable Scala web app into a directory of your choice:

```
gs new app todo ./my-app
```

Then drive it, extend it, and preview it with `tt serv`. From here, the rest of the manual home
page points at the concepts, the tool selection guide, and the status line.

[← Back to the manual home](index.html)
