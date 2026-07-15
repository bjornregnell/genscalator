# 050 — genscalator settings: `configureAllTheThings` (design note / mini-PRD)

*SM115. Status: DESIGN (no build). Author: agent, safe-solo 2026-07-15. Grounds: `tools/statusline.scala`,
`tools/mode.scala`, `tools/forge.scala`, `tools/verify.scala`, `tools/update.scala`, `docs/allowlist.md`, the
compact/approval hook scripts, SM064 (mode colour), SM088/SM105 (hook wiring), the harness-ux notification-copy
finding. Ties PRD `Goal: noCacophony`, `Feature: configureAllTheThings`.*

> **BR's seed:** *"a user who wants to tweak all the things needs a genscalator config. Do we need our own `gs`
> settings file? What goes in it? Probably everything that COULD be configured (and thus should, so we would)."*
> Plus (2026-07-15): *selective notification control* — bing-bings are potentially irritating, so the human must
> be able to turn each KIND on or off individually, not all-or-nothing.

## 1. The problem — config exists, but scattered across five mechanisms

genscalator is already configurable; the knobs just live in five different places, none of them a single file a
user can open and see "here is everything you can tune". Grounded inventory of what's tunable *today*:

| Knob | Where it lives now | Mechanism |
|---|---|---|
| statusline thresholds (`--warn`, `--ctx-warn`, `--dumb-zone`, `--auto-compact`) + segment colours | baked into the `statusLine` command string in `.claude/settings.json` | **CLI flags** (`statusline.scala:205-219`); defaults hardcoded (`dumbZone=75`, `autoCompact=92`) |
| mode labels + per-label colours | `~/.claude/gs-modes` (state) + a colour map | **state file** + hardcoded map (`statusline.scala:136`) |
| forge token / extra hosts | `GENSCALATOR_CODEBERG_TOKEN` (etc.), `TT_FORGE_HOSTS` | **env vars** (`forge.scala:85,90`) |
| verify allowlist extension | `TT_VERIFY_ALLOW` | **env var** (`verify.scala:41`) |
| compact bing-bing on/off | `~/.claude/compact-notify.enabled` | **sentinel file** (hook reads it) |
| update-check throttle stamp | `~/.cache/genscalator/last-update-check` | **stamp file** (`update.scala:40`) |
| allowlist preferences | `docs/allowlist.md` (canonical) merged into `.claude/settings.local.json` | **doc + merge** (`gs allow`) |
| grepr line-truncation (140) + default context N | — | **hardcoded**, not yet exposed |

Five mechanisms (flags-in-a-command-string, env vars, sentinel files, a state file, hardcoded constants). The cost
is not that any one is wrong — each fits its tool — but that there is **no single, discoverable surface** and no
consistent precedence story. `configureAllTheThings` = give the power-user one optional file, without breaking the
sane-defaults-so-you-never-need-it posture.

## 2. Decision: one optional `gs` settings file, tools still take flags/env

**Recommend: one file, read once, layered UNDER the existing per-tool inputs — not a replacement for them.** A
single `genscalator.json` (project-local, next to `.claude/`) that a tool consults for defaults, while flags and
env still win at call time. This keeps every tool independently runnable (a flag always overrides) and makes the
file **purely additive**: delete it and nothing breaks. Rejected alternative — per-tool config files — reproduces
the scattering problem the task exists to solve.

**North star (BR): the file stays OPTIONAL.** Every knob has a sane default; the file only *overrides*. A user who
never opens it gets today's behaviour.

## 3. Schema — start with JSON, keep a typed Scala reader

Three candidates: **JSON**, **reqT-lang**, **a typed Scala config**. Recommendation:

- **On disk: JSON.** It is what Claude Code's own config uses (familiar to the user), trivially hand-editable, and
  needs no genscalator-specific parser knowledge to read. reqT-lang is genscalator's language for *requirements /
  models* (a bag of elements), not really a key→value settings shape; using it here would be a category stretch.
- **In code: a typed Scala config object**, read once at the boundary (the LibJVM-style thin wrapper), so tools see
  `Settings.dumbZone: Double` not raw JSON. This is where **Iron** earns its place (scala-style §1): refine the
  genuinely-constrained fields at the parse boundary — `dumbZone: Int :| Interval.Closed[0,100]`,
  a colour as a constrained code — so a bad value in the file fails *loudly at load* with a clear message, not
  silently at render. (Do NOT refine long free-text fields — the compiler-macro StackOverflow hazard, research 031.)

So: JSON as the interchange, a typed+refined Scala view as the API. A malformed file degrades gracefully (fall back
to defaults + warn once), never a hard crash — same posture as `tt update`.

## 4. Boundary vs Claude Code's `settings.local.json` — do NOT overlap

Clean split, stated as a rule:

- **Claude Code owns:** permissions/allowlist, hooks, statusLine *wiring*, model, MCP servers — anything the
  *harness* consumes. genscalator must not duplicate these; `gs allow` / `gs init` already *write into* CC's file
  (human-gated, shown) rather than shadowing it. That pattern stays.
- **genscalator owns:** the *values genscalator's own tools* read — statusline thresholds/colours, mode colours,
  notification kinds, grepr defaults, ssg template/out-dir, the SM112 native-compile tool-selection. These are
  genscalator-specific knobs the harness knows nothing about.

Litmus test for "which file?": *does the harness read it, or does a `tt` tool read it?* Harness → CC settings
(genscalator only ever writes it shown+gated). `tt` tool → `genscalator.json`.

## 5. Precedence — defaults → file → env → flags (most specific wins)

```
hardcoded defaults  <  genscalator.json  <  environment variable  <  explicit CLI flag
```

A CLI flag is the most specific expression of intent (this call, right now) and wins; the file is the persistent
project default; env sits between (per-shell/session). This matches the existing tools: today a `--dumb-zone` flag
already overrides the hardcoded default; the file simply inserts a new layer between them. Keep it this one obvious
order everywhere so a user never has to wonder why a value "didn't take".

## 6. Selective notification control — a typed `notifications` enum (the concrete win)

BR's sharpest sub-goal: bing-bings are irritating, so each **kind** must toggle **individually** (generalising the
all-or-nothing `gs compact notify on/off`). Model the kinds as a **typed enum**, each a toggle in settings:

```scala
enum NotificationKind:
  case CompactDone     // a /compact finished (the existing bing-bing)
  case ApprovalNeeded  // the harness is blocked on a human approval (SM105 approval-wake)
  case Idle            // turn ended / agent idle ("Claude Code is idle" — the fixed copy)
  case Error           // a tool/build error worth surfacing
  case MinionDone      // a delegated sub-agent finished
```

Two payoffs from one enum:
1. **Selective quieting (PRD `Goal: noCacophony`):** `notifications: { compactDone: on, idle: off, ... }` — the user
   silences the kinds they find noisy and keeps the ones they want.
2. **Cause-labelling:** the enum *names the trigger*, which is exactly the fix for the too-generic notification copy.
   The harness-ux finding this session (the "needs you" confabulation saga) showed a notice that fails to say *why*
   it fired is worse than useless. A kind-labelled notice reads "genscalator: compaction done" / "genscalator:
   approval needed", not a bare "Claude Code needs you". (This dovetails with `genscalator-notifications-branded-colon`.)

### ✅ Feasibility — RESOLVED: the hook DOES distinguish the kind

The gating question — *does the Notification hook pass enough context to distinguish the kinds?* — is answered
**yes**. Per a `claude-code-guide` check against the hooks reference (`code.claude.com/docs/en/hooks.md`,
2026-07-15), the Notification hook's stdin JSON carries a top-level **`notification_type`** field (alongside the
common `session_id` / `transcript_path` / `cwd` / `hook_event_name`, plus `message` / `title` / `details`), with
**8 documented values**:

`permission_prompt` (blocked on a tool approval) · `idle_prompt` (turn ended, awaiting input) · `auth_success` ·
`elicitation_dialog` / `elicitation_complete` / `elicitation_response` (MCP forms) · `agent_needs_input` /
`agent_completed` (background-session events).

So a hook can branch on `notification_type` — the per-kind enum is directly implementable against harness-originated
kinds too, not just genscalator's own. Our enum maps cleanly onto the harness set (`ApprovalNeeded` ⇐
`permission_prompt`, `Idle` ⇐ `idle_prompt`, `MinionDone` ⇐ `agent_completed`, ...) plus genscalator-internal kinds
(`CompactDone`) the harness has no event for.

> ⚠ **Echt caveat:** this is a sub-agent doc-fetch finding, not a fact I confirmed against a live hook payload.
> It is materially more reliable than recall (it cites the doc), but before building the enum, confirm the exact
> field name + value set against a real Notification-hook invocation (the same discipline the statusline schema got).
> (This session's confabulation saga is why the caveat stays.)

## 7. The editor — a DWIM `gs config` / `gs set X Y`

To keep with "the human never remembers exact syntax", a DWIM front:

- `gs config` — show the effective settings (defaults + file, with each value's *source* labelled, like `git config
  --show-origin`), so the user sees what's set and by which layer.
- `gs set <knob> <value>` — write one key into `genscalator.json` (create it if absent), shown, idempotent — a
  sensitive-ish edit but of genscalator's *own* file, so lighter-gated than a CC-settings edit.
- `gs config edit` — open the file.

Synonyms absorbed by DWIM (crib SM113): `gs settings`, `gs configure`, `turn idle notifications off` →
`gs set notifications.idle off`.

## 8. Recommendation + open questions for BR

**Recommended shape:** one optional project-local `genscalator.json`; JSON on disk, a typed+Iron-refined Scala view
in code; layered `defaults < file < env < flags`; a strict harness-vs-genscalator boundary; the `notifications`
enum as the first concrete section; a DWIM `gs config`/`gs set` editor. Build in slices — start with the
`notifications` enum + `gs config` (the highest-value, most-requested part), grow the file as knobs migrate in.

**Open (need BR's call, not agent's):**
1. **File location + name** — `genscalator.json` at project root? `.genscalator/config.json`? Inside `.claude/`
   (co-located but risks blurring the boundary of §4)?
2. **Scope** — project-local only, or also a user-level `~/.config/genscalator/` layer (like the update stamp)?
   Precedence would then be `defaults < user-level < project < env < flags`.
3. **Migration** — leave today's env vars / sentinel / state-file mechanisms in place (additive), or migrate them
   into the file over time? (The compact sentinel and the update stamp are *runtime state*, not *settings* — they
   probably should NOT move into a hand-edited config; keep runtime-state files separate from the settings file.)
4. **The notification feasibility check** — worth a `claude-code-guide` dig now, or defer until the enum is built?

**Not in scope here (own tasks):** the actual build (own-tooling, BR-gated), and the SM112 native tool-selection
which this file would *store* but SM112 *decides*.
