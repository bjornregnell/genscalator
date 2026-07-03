# Changelog

All notable changes to genscalator. Versions follow the git tags (`vX.Y.Z`); the `version` field in
`.claude-plugin/plugin.json` + `marketplace.json` and the version line in `AGENTS.md` track the same number.

Updating genscalator is a **human-reviewed** step — see [`docs/updating.md`](docs/updating.md). Skim this
file before adopting a new version: it changes the agent's operating rules, so review beats blind pull.

## Unreleased — docs + research accretion (introprog session 2026-06-30)
Docs/research only — no tool/version change. Human review pending (see [`HUMANS.md`](HUMANS.md)).
- **`docs/foundations.md` glossary — new agent-introspection concepts:** **Context rot**, **Token velocity**,
  **Token acceleration**, **Smart-zone ceiling (Z)** (usable working-context ratio before the dumb zone),
  **Communication bandwidth (human↔agent)** (per-direction language/TE channel), **Compact dance** (the
  save→prompt→compact→paste hand-off ritual, with a committed-files-are-the-guarantee recovery invariant),
  **Compact trigger** (propose the dance at fill ≥ 0.8·Z).
- **New research notes:** `instrumentation-by-default.md`, `token-budget-awareness.md`, `smart-zone-ceiling.md`,
  `communication-bandwidth.md`, `instructions-for-claude.md` (the global custom-instructions field; includes
  BR's current instructions as a worked example), `instruction-surfaces-precedence.md` (how AGENTS.md /
  CLAUDE.md / SKILL.md / MEMORY.md / global instructions compose, conflict, and rank). `research/README.md`
  index updated for all.
- **`research/wr-data/introprog-autotranslate.md`** — appended friction events (count/status aggregation gaps,
  GPU/job probe, pipe-to-grep noise suppression, git-commit & repo-overview cd/&&/echo regressions) feeding
  candidate `tt` tools.
- **Human-agent collaboration research notes:** `instruction-adherence-decay.md` (why the agent regresses to
  dynamic shell — trained-prior reflex, fix is structural), `task-autonomy-negotiation.md` (ralph-loop vs
  ballgame per-task triage by verifiability), `shared-file-editing-protocol.md` (non-destructive shared-file
  editing; Opt A/B/C trade-offs; the HUMANS.md/HUMANS.inbox.md split), `human-state-and-joint-zone.md` (model
  the human's smart/dumb zone + the joint (human,agent) 2x2; agent-as-stabilizer; thriller state; rest dance).
  Plus the **HUMANS.md + HUMANS.inbox.md** collaboration protocol (Opt A file-level partition).
- **Notation rename `L → Z`** — the smart-zone ceiling symbol is now **Z** (smart-**Z**one ceiling; a lone `Z`
  is visually salient and reads as *sitting between* the smart and dumb zones, where a lone `L` was noise).
  Renamed repo-wide (`docs/foundations.md`, `smart-zone-ceiling.md`, `human-state-and-joint-zone.md`,
  `research/README.md`, `HUMANS.md`, `PRD.md`, this file) — **except `research/RAW-DATA.md`**, now declared
  **append-only** (a change of mind is logged as new data, never a retro-patch of raw datapoints).
- **New research note** `proactive-compaction-point.md` — lazy-vs-proactive compaction; a durability-gated
  *consolidation point* as a second (proactive) compact trigger beside the reactive `fill ≥ 0.8·Z` brake; the
  "compaction should be sleep, not collapse" framing. Plus **RQ** (research question) added to the
  comms-shorthand glossary.

## v0.8.0 — 2026-07-03
- **Comms shorthand (human↔agent)** — a shared vocabulary of standard chat/dev acronyms (BRB, AFK, WDYT,
  LGTM/SGTM, ACK/NACK, TL;DR, PTAL, AFAICT, IIRC, WRT, WIP, repro, PR …) both roles emit and parse *without
  expansion*, as a communication-bandwidth + TE lever. New **`docs/foundations.md`** glossary entry (four
  groups: presence/status, opinion/agreement, meta/reference, dev-flavored; distinct from the project's
  coined terms CF/TE/Z/WR/AT/BHH) **plus** an always-on **`AGENTS.md`** section (18 inline acronyms for
  zero-load use, pointing at the full glossary list). Operating-rules change → version bump.

## v0.7.0 — 2026-06-27
- **New tool `tt verify`** — run-and-verify driver and the toolbox's first **effectful** tool (os-lib).
  Runs an allowed command **directly as argv (no shell)**, captures exit/stdout/stderr, checks them
  (`--exit`/`--out`/`--out-re`/`--err`/`--err-re`, all must pass), and prints an audit line + PASS/FAIL.
  Replaces the `cd && … > log; echo $?` bundle with one allowlistable call (`Bash(tt verify *)` is safe to
  blanket-allow). Safe-by-design exec allowlist: `scala-cli`, `tt`, `scalex` + the human-set
  `TT_VERIFY_ALLOW` env var (the agent can't widen it via a flag). Prototypes the `--audit` roadmap flag and
  closes the run-and-verify WR-data candidate.

## v0.6.0 — 2026-06-27
- **New tool `tt log`** — build/run-log analyzer (two buckets: errors, warnings + a verdict). Curated
  markers span the logs agents actually read: compiler/build, test runners / CI (`FAIL`, `##[error]`),
  runtime leveled logs (`ERROR`/`FATAL`/`CRITICAL`, logfmt `level=error`, JSON `"level":"error"`), Python
  `Traceback`, Go `panic:`, `npm ERR!`, and LaTeX — all targeted so tally lines ("0 errors") don't
  false-positive. Customizable per log: `--error`/`--warn` (repeatable) extend the defaults, `--no-defaults`
  replaces them, `--cap N` bounds output. Pure, JDK-only. First of the case-study-driven "more generic
  tools" roadmap item.
- **New skill `contribute-tool`** — the recipe to generalize a scratch tool into a toolbox-worthy `tt`
  tool before proposing it (start specific → general class → sane defaults + customization → verify with
  adversarial fixtures → strip specifics → propose; human ships). Sibling to `scala-style`; distilled from
  the `tt log` generalization. Wired into `AGENTS.md` (self-monitoring) and `CONTRIBUTING.md`.

## v0.5.0 — 2026-06-27
- **New skill `scala-style`** — how to *write* a tool: direct style, pragmatic immutability (safety ↔
  token-efficiency ↔ performance as a conscious balance), Safe-mode-ready, effects isolated to drivers.
- **`research/` folder** — open investigation log; first investigation: self-conscious evolution of the
  `scala-style` skill. Includes `research/wr-data/` (Workflow Research: confirmation events that are
  candidates for new safe-by-design `tt` tools).
- **`tt text grepr` enhanced** — multi-extension scan (`.scala,.java` in one call) and friendly one-line
  errors (+ exit 2) on a bad/relative dir instead of a raw stack trace. Single-ext form unchanged.
- Install docs restructured for newcomers (clone step, `$PWD` context, verify); companion installs for
  scalex + Metals MCP in the plugin section.

## v0.4.0 — 2026-06-26
- **Metals MCP** adopted as the compiler-grade complement above scalex: explicit escalation ladder
  (`tt` → scalex → Metals) and a read-only-vs-effectful safety split. Install UX improvements.

## v0.3.0 — 2026-06-26
- **scalex integration** — `docs/tool-selection.md` (which tool for which question) + companion docs;
  scalex recommended as the symbol-aware Scala-navigation companion (integrated, not bundled).

## v0.2.0 — 2026-06-26
- **Claude Code plugin packaging** — repo doubles as its own marketplace; `bin/tt` + the `tt-toolbox`
  skill ship as a plugin. Human/agent contribution workflow.

## v0.1.0 — 2026-06-26
- Initial release: the `tt` toolbox (`text`, `files`, `newtool` + shared `lib`),
  `docs/foundations.md` (goals, stakeholders, glossary), `docs/confirmations-method.md`.
