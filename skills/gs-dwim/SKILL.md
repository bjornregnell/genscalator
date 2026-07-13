---
name: gs-dwim
description: Do-What-I-Mean in-session genscalator commands cued by a leading `gs`. Trigger whenever the user's message begins with `gs ` (or is a bare `gs`) — e.g. "gs help", "gs cues", "gs cue similar", "gs dances", "gs dance compact", "gs help tt", "gs help tt search text", "gs tt chrono", "gs status", "gs status line on". Interpret the intent do-what-I-mean style (nearest-in-meaning, "or similar"), not by exact string match, and perform the matching genscalator action.
allowed-tools: Bash(tt text *) Bash(tt files *) Bash(tt log *) Bash(tt chrono *) Bash(tt statusline *) Bash(tt parsereqt *) Bash(tt gitinfo *)
---

# `gs` — genscalator do-what-I-mean (DWIM) in-session commands

When the user types a message led by **`gs`**, they are issuing an in-session genscalator command.
**Do what they mean** ([[dwim]]): match their words to the *nearest command in meaning* below — the list is
an INFORMAL spec, not a rigid grammar, so honour `gs`-led phrasings that are merely *similar* to these.
`gs` is deliberately overloaded — as a bare leading cue it means "run a gs command"; as `gs/path` or the
word "gs" inside prose it still means the project *genscalator*. Context disambiguates; if a `gs` message is
genuinely ambiguous or the stakes are real, ask before acting ([[cue-edit-vs-clarification]]).

## The commands (informal spec — do-what-I-mean, not exact-match)

```
gs help              show the welcome + this help on the gs do-what-I-mean commands
gs                   same as `gs help`
gs help tt           list all typed tools (tt ...) with a one-line description of each
gs help tt <what>    detailed help on the tool nearest in meaning to <what>
gs tt <tool>         run <tool> and show its output here in session (e.g. gs tt chrono)
gs status            the status-line info, expanded into a table, here in session
gs status line on    turn the status line on
gs status line off   turn the status line off
gs cues              list all cues (human->agent and agent->human) and what they mean
gs cue <what>        explain the cue nearest in meaning to <what>
gs dances            list all dances and their goals
gs dance <what>      explain the dance nearest in meaning to <what>
```

## How to perform each

- **`gs help` / bare `gs`** — print the welcome: what genscalator is, the tool families, how to turn the
  status line on, and this `gs` command list. Source: the plugin welcome text (`docs/claude-plugin.md` +
  `research/sm-investigations/SM056-welcome-content-draft.md`).
- **`gs help tt`** — list every `tt` tool with a one-line description. Source of truth: the `## Tools`
  section of `tools/README.md` (each `### <tool>` heading + its tagline). Present as a compact table.
- **`gs help tt <what>`** — pick the tool **nearest in meaning** to `<what>` (e.g. "search text" → `tt
  text`, "make a diagram" → `tt svg`/`tt ascii`, "time something" → `tt chrono`) and show its full help by
  running `tt <tool> --help`. If two are close, show both and say why.
- **`gs tt <tool>`** — run `tt <tool>` (with any args the user gave) and show its output inline. For an
  EFFECTFUL tool (git/forge/ssg/serv/web/box), the normal permission flow still applies — the explicit `gs`
  command is the user's intent, but surface what will run.
- **`gs status`** — expand the status-line information into a TABLE in session: each segment (brand, clock,
  model, ctx-fill, 5h-lim, wk-lim, cost), its meaning, and its threshold/colour rule (from
  `docs/statusline-manual.md`). Fill in current values only if they are available in context (e.g. the user
  pasted the live line); otherwise present the legend and say the live values aren't visible to you.
- **`gs status line on` / `off`** — the one-line `.claude/settings.json` change:
  on = add `"statusLine": { "type": "command", "command": "tt statusline" }`; off = remove that key; then
  reload via `/hooks`. Treat the explicit `gs` command as the user's go, but SHOW the exact change you make
  (settings edits are sensitive — never do a silent one). If unsure whether to edit their settings file
  directly, hand them the snippet to paste.
- **`gs cues`** — list the cues (human→agent and agent→human) and what each means. Sources: the cue entries
  in `docs/foundations.md` and the `cue-*` memories. Present grouped by direction.
- **`gs cue <what>`** — explain the cue **nearest in meaning** to `<what>` (e.g. "tired" → `:Z`, "go away
  for a bit" → `BRB`/`hang on`, "do what I mean" → this).
- **`gs dances`** — list the dances (compact, rest/`:Z`, delegation, live-edit, solo/AFK, token-usage,
  session-limit, weekly-limit, context, hardening, ...) and each one's goal. Source:
  `docs/foundations.md` "Dances and handoffs".
- **`gs dance <what>`** — explain the dance **nearest in meaning** to `<what>` (e.g. "running low on
  context" → the compact dance, "hand off work" → the solo/delegation dance).

## The DWIM contract

The whole point is that the user should not have to remember exact syntax. A leading `gs` plus *roughly* one
of these intents = do the sensible thing. Prefer acting over asking when the intent is clear; ask only on
genuine ambiguity or real stakes. Keep answers scannable (tables/short lists), in session, so the user stays
in flow. This skill is the agent-facing implementation; the human-facing description lives in the plugin
welcome and in `docs/foundations.md` ([[dwim]]).
