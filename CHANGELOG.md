# Changelog

All notable changes to genscalator. Versions follow the git tags (`vX.Y.Z`); the `version` field in
`.claude-plugin/plugin.json` + `marketplace.json` and the version line in `AGENTS.md` track the same number.

Updating genscalator is a **human-reviewed** step — see [`docs/updating.md`](docs/updating.md). Skim this
file before adopting a new version: it changes the agent's operating rules, so review beats blind pull.

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
