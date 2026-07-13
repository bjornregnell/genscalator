---
name: gs-dwim
description: Do-What-I-Mean in-session genscalator commands cued by a leading `gs`. Trigger whenever the user's message begins with `gs ` (or is a bare `gs`) — e.g. "gs help", "gs cues", "gs cue similar", "gs dances", "gs dance compact", "gs help tt", "gs help tt search text", "gs tt chrono", "gs status", "gs status line on", "gs where", "gs menu", "gs reqt", "gs term rot", "gs test", "gs seed app todo ./my-app". Interpret the intent do-what-I-mean style (nearest-in-meaning, "or similar"), not by exact string match, and perform the matching genscalator action.
allowed-tools: Read Bash(tt text *) Bash(tt files *) Bash(tt log *) Bash(tt chrono *) Bash(tt statusline *) Bash(tt parsereqt *) Bash(tt gitinfo *) Bash(scala-cli test *)
---

# `gs` — genscalator do-what-I-mean (DWIM) in-session commands

When the user types a message led by **`gs`**, they are issuing an in-session genscalator command.
**Do what they mean** ([[dwim]]): match their words to the *nearest command in meaning* below — the list is
an INFORMAL spec, not a rigid grammar, so honour `gs`-led phrasings that are merely *similar* to these.
`gs` is deliberately overloaded — as a bare leading cue it means "run a gs command"; as `gs/path` or the
word "gs" inside prose it still means the project *genscalator*. Context disambiguates; if a `gs` message is
genuinely ambiguous or the stakes are real, ask before acting ([[cue-edit-vs-clarification]]).

## The commands (informal spec — do-what-I-mean, not exact-match)

Two tiers. **Tier 1 — anyone with the plugin, in any project** (explore and drive the shipped toolbox and habits):

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
gs term <what>       explain the foundations glossary term nearest in meaning to <what>
gs seed app <what> <dir>  seed a complete runnable Scala web app <what> (e.g. todo-web-app) into <dir>
```

**Tier 2 — genscalator contributors, dogfooding mode** (working ON genscalator, or in the gs research MO).
These assume the gs dev substrate: a pin board / resume prompt, a reqT-lang `PRD.md`, a `tools/test/` suite.
A plain plugin user will not have these; the command should say so and fall back gracefully.

```
gs where             orient: a short current-state snapshot (pin board + resume prompt + recent git log)
gs menu              show the safe solo-task menu (rot-ranked), for a solo/AFK handoff
gs reqt [<file>]     parse + lint a reqT-lang file (default PRD.md); report both results
gs test              run the tt toolbox test suite (handles the tt.tools prop) and report green/red
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
- **`gs term <what>`** — explain the foundations glossary term **nearest in meaning** to `<what>`. Broader
  than `gs cue`/`gs dance`: covers any coined concept (rot, the dumb zone Z, substrate-grounding, ape⟷anthro,
  echt, DWIM, ...). Source: `docs/foundations.md`. Give the definition plus a one-line "why it matters"; if two
  terms are close, show both.
- **`gs seed app <what> <dir>`** — seed a complete, runnable Scala web app. Recognise the intent and **invoke
  the `crud-web-app-seed` skill**, passing the app kind `<what>` (e.g. "todo-web-app") and the target `<dir>`.
  This WRITES a project (shared datamodel, JDK-only server, Scala.js/Laminar client, reqT-lang PRD, tests), so
  it is effectful: confirm the target `<dir>` and never overwrite a non-empty directory without asking. Not a
  delegation candidate (it writes durable output) — run inline; afterwards point the user at the generated
  `README.md` to build and run. Tier 1 (any plugin user), but effectful, unlike the other Tier-1 commands.
**Tier 2 (genscalator contributors / dogfooding mode) — assume the gs dev substrate; degrade gracefully if absent:**

- **`gs where`** — orient: a SHORT current-state snapshot so the user (or a returning agent) re-syncs fast.
  Read from whatever current-state substrate the project keeps — a pin board's `## NOW` section, a
  `tmp/resume-prompt.md`, and the recent `git log` (`tt gitinfo` / `tt log`) — and summarise: what shipped
  recently, what is in flight, what awaits the human. Keep it to a screen; link the sources for detail.
  **Ground it in the files, do not recall.** If the project keeps no such substrate, say so and fall back to
  the recent git log.
- **`gs menu`** — show the safe solo-task menu for a solo/AFK handoff, rot-ranked (safest/cheapest first).
  Source: the pin board's stocked menu if one exists (the `## NOW` safe-vs-not-safe list); otherwise derive
  candidate safe tasks from the current state (agent-authored, read-only, no outward ops). Re-verify each
  item's safety against the CURRENT state before presenting ([[cue-go-afk]]).
- **`gs reqt [<file>]`** — verify a reqT-lang file in one step: run `tt parsereqt parse <file>` then
  `tt parsereqt lint <file>`, and report BOTH (parse errors and unknown-concept fall-throughs). Default
  `<file>` to `PRD.md` if none is given. This is the after-every-reqT-edit check folded into one command.
- **`gs test`** — run the genscalator toolbox test suite and report green/red. Command shape:
  `scala-cli test <repo>/tools --java-prop tt.tools=<repo>/tools` — the `tt.tools` prop is REQUIRED whenever
  the cwd is not the tools dir (a known gotcha); resolve `<repo>` to the genscalator checkout. Report the
  pass/fail counts; on red, surface the first failing suite so the user knows where to look.

## The DWIM contract

The whole point is that the user should not have to remember exact syntax. A leading `gs` plus *roughly* one
of these intents = do the sensible thing. Prefer acting over asking when the intent is clear; ask only on
genuine ambiguity or real stakes. Keep answers scannable (tables/short lists), in session, so the user stays
in flow. This skill is the agent-facing implementation; the human-facing description lives in the plugin
welcome and in `docs/foundations.md` ([[dwim]]).

## Running a gs command as a sub-agent job (delegation policy)

Most gs commands run inline. A few are worth handing to a **sub-agent** (the Agent tool) instead. Delegate
ONLY when **all three** hold — if any fails, run inline:

1. **Big intermediate, small digest** — the command reads or produces a lot of text (many files, a long build
   log) but the user wants only a short conclusion. The sub-agent absorbs the bulk and returns just the
   digest, keeping it OUT of the main context window. This is the whole reason to delegate ([[delegation-dance]]).
2. **Read-only or safely isolated** — no settings edit, no outward/effectful op, nothing that needs the main
   agent's permission flow or the human's sight.
3. **Independent** — the job needs no live back-and-forth with the main context to complete.

| Command | Delegate? | Why |
|---|---|---|
| `gs test` | **yes (best)** | ~60s run, tens of KB of build noise → one line (green/red + first failing suite). Pure mechanical read-and-digest. |
| `gs where` | **yes** | reads pin board + resume prompt + git log → a one-screen snapshot; returns the conclusion, not the file dumps. |
| `gs menu` | **yes** | reads the menu + re-verifies each item's safety → a short ranked list; same big-in / small-out shape. |
| `gs reqt` | marginal | only if the parse dump is huge and you want just the verdict; usually run right after an edit and wanted inline to act on. |
| `gs cues` / `gs dances` | marginal | multi-source read but small output; usually wanted rendered here. |
| `gs help*` / `gs term` / `gs cue` / `gs dance` | no | quick lookups, small output — a sub-agent hop costs more than it saves. |
| `gs tt <tool>` | no | the user wants the tool's output HERE; and for an effectful tool (git/forge/ssg/serv) the permission flow + outward-op discipline MUST stay in the main agent's view — never delegate an effectful run. |
| `gs status line on/off` | **never** | a sensitive settings edit — must be inline, SHOWN to the user, with the `/hooks` handoff; delegating a settings change out of sight is the exact anti-pattern. |
| `gs seed app` | no | writes a whole project (effectful, durable output); the primary output is the seeded app — run inline, never delegate. |

Two refinements that make the decision smarter:
- **`gs test`: a BACKGROUND job often beats a sub-agent.** When the human is present, run it in the background
  (no agent turn spent; it notifies on completion and you read only the digest). Reserve the sub-agent form for
  when you also want it fully isolated from the main context, or run in parallel with other work.
- **Model + safety.** The good candidates are mechanical read-and-digest, so a **cheap model** (CF5/haiku)
  fits — no heavy reasoning. And a delegated gs command must write **nothing durable** (no memory, no commits):
  it reads and returns. If a gs command would need to write or commit, it is NOT a delegation candidate.

Neat consequence: the delegatable commands are exactly **Tier 2** (dogfooding), and the Tier-1 user commands
mostly want to render inline. **The dogfooding tier is also the delegation tier.**
