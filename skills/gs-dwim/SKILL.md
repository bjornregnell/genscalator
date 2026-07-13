---
name: gs-dwim
description: Do-What-I-Mean in-session genscalator commands cued by a leading `gs`. Trigger whenever the user's message begins with `gs ` (or is a bare `gs`) — e.g. "gs help", "gs cues", "gs cue similar", "gs dances", "gs dance compact", "gs help tt", "gs help tt search text", "gs tt chrono", "gs status", "gs status line on", "gs where", "gs menu", "gs reqt", "gs term rot", "gs test", "gs new app todo ./my-app". Interpret the intent do-what-I-mean style (nearest-in-meaning, "or similar"), not by exact string match, and perform the matching genscalator action.
allowed-tools: Read Bash(tt text *) Bash(tt files *) Bash(tt log *) Bash(tt chrono *) Bash(tt statusline *) Bash(tt parsereqt *) Bash(tt gitinfo *) Bash(tt doc *) Bash(tt mode *) Bash(scala-cli test *)
---

# `gs` — genscalator do-what-I-mean (DWIM) in-session commands

When the user types a message led by **`gs`**, they are issuing an in-session genscalator command.
**Do what they mean** ([[dwim]]): match their words to the *nearest command in meaning* below — the list is
an INFORMAL spec, not a rigid grammar, so honour `gs`-led phrasings that are merely *similar* to these.
`gs` is deliberately overloaded — as a bare leading cue it means "run a gs command"; as `gs/path` or the
word "gs" inside prose it still means the project *genscalator*. Context disambiguates; if a `gs` message is
genuinely ambiguous or the stakes are real, ask before acting ([[cue-edit-vs-clarification]]).

## The commands

The **canonical, render-ready command list lives on disk** at `docs/gs-help.txt` — terminal-style monospace,
the single source of truth. **`gs help` reads that file and prints it verbatim** (see the how-to below); do
not re-synthesise or reformat it into a table, and keep `docs/gs-help.txt` current whenever a command changes.

The file groups them as **user commands**, then under a divider the **experimental developer commands** (genscalator contributors /
dogfooding mode, which assume the dev substrate: a pin board / resume prompt, a reqT-lang `PRD.md`, a
`tools/test/` suite — a plain plugin user will not have these, so those commands should say so and degrade
gracefully). The per-command behaviour is specified below.

## How to perform each

- **`gs help` / bare `gs`** — **run `tt doc gs-help`** and let its output render. It cats
  `docs/gs-help.txt` at native speed, so the help appears from the subprocess instead of the agent re-emitting
  it token-by-token (the slow path). `docs/gs-help.txt` stays the single source of truth; keep it current when
  commands change. Fallback if `tt` is unavailable: Read the file and print it verbatim in a code fence. Fuller
  welcome content lives in `research/sm-investigations/SM056-welcome-content-draft.md`.
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
- **`gs status mode on` / `off` / (bare = status)** — toggle the **mode line** (line 2 of the statusline: the
  declared joint state-of-mind). Same `.claude/settings.json` command the status line uses, via a flag: `on` =
  ensure the statusLine command carries `--mode-line`; `off` = remove that flag; bare = report whether it is
  present. (Line 1 off but mode line on = `tt statusline --no-status --mode-line`.) A sensitive settings edit:
  SHOW the exact change, human-gated, reload via `/hooks` — same discipline as `gs status line`. The two lines
  toggle INDEPENDENTLY so the user budgets vertical space.
- **`gs mode` / `gs mode add <label>` / `gs mode rm <label>`** — read or MUTATE the recorded joint
  state-of-mind (the declared modes the mode line renders). Thin front for `tt mode`: bare = `tt mode` (list),
  `add` = `tt mode add <label>`, `rm` = `tt mode rm <label>`, `gs mode clear` = `tt mode clear`. Labels are bare
  tokens (`token-spending`, `hot-harvest`, `high-context`, `solo`, `human-stress`, `rot-vigilance`, `racing`,
  ...). NOT a settings edit — just the state file `~/.claude/gs-modes`, allowlisted, no confirmation. **Both
  parties declare:** the human sets frame modes (token-spending, racing, human-stress); the **agent should
  proactively declare its own** as the MO shifts — `tt mode add hot-harvest` when harvesting, `rot-vigilance`
  when watching rot, `high-context` as fill rises, `solo` on an AFK handoff — and `rm` them when they end, so
  the mode line stays a live, mutually-visible reflection of the shared state.
- **`gs cues`** — list the cues (human→agent and agent→human) and what each means. **Source:
  `docs/gs-registry.md`** (the ready-to-grab Cues tables — read it and render; it is kept in sync with
  `docs/foundations.md` + the `cue-*` memories, which stay canonical if the registry looks stale). Present
  grouped by direction.
- **`gs cue <what>`** — explain the cue **nearest in meaning** to `<what>` (e.g. "tired" → `:Z`, "go away
  for a bit" → `BRB`/`hang on`, "do what I mean" → this).
- **`gs dances`** — list the dances (compact, rest/`:Z`, delegation, live-edit, solo/AFK, token-usage,
  session-limit, weekly-limit, context, hardening, ...) and each one's goal. **Source:
  `docs/gs-registry.md`** (the Dances table; regenerate from `docs/foundations.md` "Dances and handoffs" if stale).
- **`gs dance <what>`** — explain the dance **nearest in meaning** to `<what>` (e.g. "running low on
  context" → the compact dance, "hand off work" → the solo/delegation dance).
- **`gs term <what>`** — explain the foundations glossary term **nearest in meaning** to `<what>`. Broader
  than `gs cue`/`gs dance`: covers any coined concept (rot, the dumb zone Z, substrate-grounding, ape⟷anthro,
  echt, DWIM, ...). **Source: `docs/gs-registry.md`** (the Terms table; canonical prose in
  `docs/foundations.md`). Give the definition plus a one-line "why it matters"; if two terms are close, show both.
- **`gs new app <what> <dir>`** — create a complete, runnable Scala web app (`gs seed app` / `gs make app` /
  `gs create app` are do-what-I-mean synonyms — "seed" is the internal skill name, "new" is the user-facing
  verb). Recognise the intent and **invoke
  the `crud-web-app-seed` skill**, passing the app kind `<what>` (e.g. "todo-web-app") and the target `<dir>`.
  This WRITES a project (shared datamodel, JDK-only server, Scala.js/Laminar client, reqT-lang PRD, tests), so
  it is effectful: confirm the target `<dir>` and never overwrite a non-empty directory without asking. Not a
  delegation candidate (it writes durable output) — run inline; afterwards point the user at the generated
  `README.md` to build and run. Tier 1 (any plugin user), but effectful, unlike the other Tier-1 commands.
- **`gs compact notify on` / `off` / (bare = status)** — toggle the compaction **bing-bing**: a critical
  desktop notice (it pierces Do-Not-Disturb) plus a chime, fired the instant a `/compact` finishes, so a human
  who wandered off is called back (foundations "Compact sleep"). It is gated on the
  sentinel file `~/.claude/compact-notify.enabled`, read by the `Pre`/`PostCompact` hook `~/.claude/compact-wake.sh`:
  `on` creates the sentinel — and when this is an ACTIVATION (the sentinel did not already exist), also fires a
  one-off labelled **preview** by running `compact-wake.sh demo`, so the user sees and hears exactly what they
  opted into at the moment of consent (relay any "no notifier / no sound on this box" line it prints — a free
  functional test). `off` removes the sentinel, bare/`status` reports whether it exists. This is a trivial,
  reversible one-file toggle (NOT a `settings.json` edit — the hook is already wired), so treat the explicit `gs`
  command as the user's go and just do it, then confirm the new state (SHOWN, like `gs status line`). **Default is
  OFF**, because the notice is intrusive by design. If the hook is not installed (a plugin user who has not set it
  up), say so and point at the setup doc `docs/compact-bing-bing.md` (the `compact-wake.sh` script plus a
  `Pre`/`PostCompact` hook entry in `~/.claude/settings.json`). Desktop-Linux only (`notify-send` + a sound player such as `canberra-gtk-play`).
- **`gs sound test`** — play a single test chime so the user can confirm their speakers / audio are on. Run
  `canberra-gtk-play -i complete` (fall back to `paplay /usr/share/sounds/freedesktop/stereo/complete.oga`); if
  neither player exists, say the box has no sound player installed. Effect-light (just plays a sound), Tier 1,
  never a delegation candidate (the user wants to hear it here and now).
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
of these intents = do the sensible thing.

**Match the response to your CONFIDENCE about what they mean — do-what-I-mean, not narrate-what-I-mean:**

- **Certain** (one obvious reading): just DO it. No preamble, no "I'll now...", no confirmation question —
  silent execution keeps the user in flow (the whole point). Speak only to hand back a result worth seeing.
- **Not straightforward** (you can guess but aren't fully sure, or the action is mildly effectful / easy to
  get wrong): state in ONE line what you plan to do, then act — or pause for a quick confirm first if it is
  effectful or easily wrong (a settings edit, a project seed, an effectful `gs tt`).
- **Severely ambiguous OR incomplete** (two or more equally-likely readings, or a required argument is
  missing): ask ONE short follow-up before acting, phrased as a **"did you mean: ..."** offering the nearest
  candidate readings for the user to pick from, rather than an open-ended question (faster to answer, and it
  shows what the agent already inferred) — e.g. `gs seed app` with no `<dir>` → ask where; bare
  `gs tt` with no tool → ask which.

The **stakes dial rides on top**: the more effectful or irreversible the action, the more you lean toward
announce-or-confirm even when fairly sure; a pure read (`gs help`/`term`/`status`/`cues`) almost never needs a
question. Keep answers scannable (tables/short lists), in session, so the user stays
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
| `gs compact notify on/off` | **never** | a per-user config toggle (a sentinel file the hook reads); keep it inline and SHOWN, like `gs status line` — delegating an out-of-sight config change is the anti-pattern. |
| `gs status mode on/off` | **never** | a settings edit (the statusLine flag); inline and SHOWN, like `gs status line`. |
| `gs mode add/rm` | no | a quick state-file mutation the agent does inline as its MO shifts; not a big-in / small-out job. |
| `gs new app` | no | writes a whole project (effectful, durable output); the primary output is the seeded app — run inline, never delegate. |

Two refinements that make the decision smarter:
- **`gs test`: a BACKGROUND job often beats a sub-agent.** When the human is present, run it in the background
  (no agent turn spent; it notifies on completion and you read only the digest). Reserve the sub-agent form for
  when you also want it fully isolated from the main context, or run in parallel with other work.
- **Model + safety.** The good candidates are mechanical read-and-digest, so a **cheap model** (CF5/haiku)
  fits — no heavy reasoning. And a delegated gs command must write **nothing durable** (no memory, no commits):
  it reads and returns. If a gs command would need to write or commit, it is NOT a delegation candidate.

Neat consequence: the delegatable commands are exactly **Tier 2** (dogfooding), and the Tier-1 user commands
mostly want to render inline. **The dogfooding tier is also the delegation tier.**
