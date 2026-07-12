# SM056 — genscalator WELCOME + `/genscalator` help: content draft

**Status:** DRAFT, content only. AFK-safe part (the message text below) is done. **HELD for a BR-present /
egress turn:** the plugin *mechanism* — whether a Claude Code plugin can auto-print a welcome on install /
activation, or via a `SessionStart` hook, or whether a slash command is the only reliable channel. Do NOT
guess plugin internals; confirm via **claude-code-guide** before wiring. Below is written to work either
way: the same body serves as a first-run welcome and as the `/genscalator` command output.

Ties: SM021 (plugin community-launch readiness), `docs/claude-plugin.md`, `docs/statusline-manual.md`.

---

## A. The `/genscalator` help command (on-demand, the safe/idiomatic channel)

Plugins can ship slash commands — that is the confirmed-idiomatic surface, so this is the primary
deliverable. Proposed command file (path to confirm against CC plugin command conventions, likely
`commands/genscalator.md` under the plugin root):

> **genscalator** — a typed Scala 3 toolbox plus habits for safer, calmer agent work.
>
> **Am I active?** If your status line starts with a bold green **`genscalator:`**, the plugin and status
> line are live. (No status line yet? See "Turn on the status line" below.)
>
> **What you get — the `tt` toolbox** (run `tt <tool> ...`; one bare command per call, no shell glue):
> - **Search & text** — `tt text` (typed grep/awk/cut), `tt files` (typed find), `tt log` (build/run-log
>   analyzer), `tt md-fmt` (markdown-aware reflow), `tt typo`, `tt htmltext`.
> - **Scala & dev flow** — `tt verify` (run-and-check, no shell), `tt newtool` (scaffold a pure tool),
>   `tt chrono` (stopwatch), `tt guardcheck` (flag banned command shapes), `tt parsereqt` (parse reqT).
> - **Diagrams** — `tt svg`, `tt ascii`, `tt gvdot` (one spec, three renderers).
> - **Web & site** — `tt web` (read-only GET), `tt serv` (loopback preview server), `tt ssg` (markdown to
>   static site).
> - **Git & forge** — `tt git` (safe commit/push helper), `tt forge` (Codeberg/Gitea client).
> - **Security & session** — `tt harden` (secret scanner), `tt box`, `tt wr` (research stamps),
>   `tt statusline` (the status bar below).
>
> **The skills** (load themselves when relevant):
> - **tt-toolbox** — prefer `tt` over raw bash/grep/awk; one bare command per call.
> - **scala-style** — how to *write* a tool: direct style, state-safe, safe-mode-where-possible.
> - **contribute-tool** — how to generalize a scratch tool and propose it upstream.
>
> **Turn on the status line** — a live one-line gauge (model, context fill, usage limits, cost). Add to
> `.claude/settings.json` (merge, do not replace):
> `"statusLine": { "type": "command", "command": "tt statusline" }` then reload with `/hooks`. Reading it:
> `docs/statusline-manual.md`.
>
> **Full command reference:** `tools/README.md`. **Install / allowlist / the scalex companion:**
> `docs/claude-plugin.md`. **Needs on your machine:** `scala-cli` + a JDK (plugins can't install deps).

## B. The first-run WELCOME (on install / activation) — SAME body, shorter lead

If (and only if) claude-code-guide confirms a plugin can print on activation (SessionStart hook or a
plugin welcome field), show a **short** version so it is not intrusive — a one-screen lead that points at
the full `/genscalator`:

> **genscalator is active.** A typed Scala 3 `tt` toolbox + habits for safer, calmer agent work. The bold
> green **`genscalator:`** on your status line is your at-a-glance "I'm live" indicator (turn the status
> line on with one line in `.claude/settings.json` — run `/genscalator` for how). Type **`/genscalator`**
> any time to see what you can do; full reference in `tools/README.md`. Needs `scala-cli` + a JDK.

**Intrusiveness caveat (for the mechanism decision):** auto-printing on every session start risks noise.
Preferred if supported: print **once** on first activation (or on install), not every SessionStart. If the
only reliable channel is the slash command, ship just section A and skip the auto-print entirely — the
`genscalator:` status-line prefix already carries the passive "am I active?" discoverability.

## C. Open questions for claude-code-guide (the held, non-AFK half)

1. Can a Claude Code plugin display a message on **install** or **first activation** (not every session)?
   Is there a plugin manifest field for it, or is a `SessionStart` hook the only route?
2. What is the correct path/format for a plugin-shipped **slash command** (`commands/genscalator.md`? front
   matter? naming so it surfaces as `/genscalator`)?
3. If a `SessionStart` hook is used, how to make it fire **once** rather than on every start (state file in
   the plugin root? a marker in the user's config)?
4. Does the command output render markdown, and is there a length/format constraint to respect?

Answer these, then wire section A (and optionally B) into the plugin — a BR-present, egress-allowed step.
