# Changelog

All notable changes to genscalator. Versions follow the git tags (`vX.Y.Z`); the `version` field in
`.claude-plugin/plugin.json` + `marketplace.json` and the version line in `AGENTS.md` track the same number.

Updating genscalator is a **human-reviewed** step — see [`docs/updating.md`](docs/updating.md). Skim this
file before adopting a new version: it changes the agent's operating rules, so review beats blind pull.

## Unreleased — docs + research accretion + `tt svg` (sessions 2026-06-30 → 2026-07-05)
Mostly docs/research; **one new tool** (`tt svg`). Version bump pending. Human review pending (see [`HUMANS.md`](HUMANS.md)).
- **New tool `tt svg --sequence-diagram` (aka `tt svg sequence <in.txt> [out.svg]`)** — renders a tiny textual
  sequence-diagram spec (PlantUML/mermaid-flavoured: `actor`, `A -> B: msg`, `A --> B: reply`, `note over`) to a
  **self-contained, theme-aware SVG** for blogs and human-facing reports. Pure (JDK-only, no dep); 7 CLI-contract
  tests incl. a well-formed-XML parse check. Design rationale (why a bespoke spec, not reqT-lang: reqT models an
  *unordered set*, a sequence is *ordered in time*) in [`research/037-svg-sequence-diagram-tool.md`](research/037-svg-sequence-diagram-tool.md).
  First real figure: [`blog/figures/seq-compact-dance.svg`](blog/figures/seq-compact-dance.svg) (candidate for blog 005).
- **`docs/foundations.md` glossary — new agent-introspection concepts:** **Context rot**, **Token velocity**,
  **Token acceleration**, **Smart-zone ceiling (Z)** (usable working-context ratio before the dumb zone),
  **Communication bandwidth (human↔agent)** (per-direction language/TE channel), **Compact dance** (the
  save→prompt→compact→paste hand-off ritual, with a committed-files-are-the-guarantee recovery invariant),
  **Compact trigger** (propose the dance at fill ≥ 0.8·Z).
- **New research notes:** `005-instrumentation-by-default.md`, `007-token-budget-awareness.md`, `006-smart-zone-ceiling.md`,
  `002-communication-bandwidth.md`, `004-instructions-for-claude.md` (the global custom-instructions field; includes
  BR's current instructions as a worked example), `003-instruction-surfaces-precedence.md` (how AGENTS.md /
  CLAUDE.md / SKILL.md / MEMORY.md / global instructions compose, conflict, and rank). `research/README.md`
  index updated for all.
- **`research/wr-data/introprog-autotranslate.md`** — appended friction events (count/status aggregation gaps,
  GPU/job probe, pipe-to-grep noise suppression, git-commit & repo-overview cd/&&/echo regressions) feeding
  candidate `tt` tools.
- **Human-agent collaboration research notes:** `008-instruction-adherence-decay.md` (why the agent regresses to
  dynamic shell — trained-prior reflex, fix is structural), `010-task-autonomy-negotiation.md` (ralph-loop vs
  ballgame per-task triage by verifiability), `009-shared-file-editing-protocol.md` (non-destructive shared-file
  editing; Opt A/B/C trade-offs; the HUMANS.md/HUMANS.inbox.md split), `011-human-state-and-joint-zone.md` (model
  the human's smart/dumb zone + the joint (human,agent) 2x2; agent-as-stabilizer; thriller state; rest dance).
  Plus the **HUMANS.md + HUMANS.inbox.md** collaboration protocol (Opt A file-level partition).
- **Notation rename `L → Z`** — the smart-zone ceiling symbol is now **Z** (smart-**Z**one ceiling; a lone `Z`
  is visually salient and reads as *sitting between* the smart and dumb zones, where a lone `L` was noise).
  Renamed repo-wide (`docs/foundations.md`, `006-smart-zone-ceiling.md`, `011-human-state-and-joint-zone.md`,
  `research/README.md`, `HUMANS.md`, `PRD.md`, this file) — **except `research/RAW-DATA.md`**, now declared
  **append-only** (a change of mind is logged as new data, never a retro-patch of raw datapoints).
- **New research note** `022-proactive-compaction-point.md` — lazy-vs-proactive compaction; a durability-gated
  *consolidation point* as a second (proactive) compact trigger beside the reactive `fill ≥ 0.8·Z` brake; the
  "compaction should be sleep, not collapse" framing. Plus **RQ** (research question) added to the
  comms-shorthand glossary.
- **Cue split `note:` / `pin:` + the dances family (2026-07-04/05).** The durable-save cue (briefly `etch`,
  renamed to **`pin`** to avoid the *echt*/*etch* near-anagram) is now distinct from `note:` (notice / keep-fluent
  + pin-candidate). New dances defined with a **dance-bar** criterion (≥2 interlocking steps, ≥1 human + ≥1 agent):
  **go** (autonomy handoff), **hardening**, **rest**, **consistency-sweep**. Propagated across `docs/foundations.md`,
  `blog/005-dancing-with-agents.md`, and memory.
- **`docs/foundations.md` reorganized for findability** — a group map + A→Z index + themed subsections; new terms
  (agent **CO4/CF5**, **AT**, authority anchor, ballgame, corroboration asymmetry, thriller state, go / hardening /
  rest dances); "Roles and work strands" reframed to **Roles and cases**, grounded in *Case Study Research in SE*
  §3.2.3.
- **New `research-methods` skill + two public book summaries.** `skills/research-methods/SKILL.md` (strategy
  chooser, case-study + experiment checklists, validity cheat-sheet) points at `summary-case-study-research.md` and
  `summary-experimentation.md` — original, copyright-clean distillations of BR's two co-authored methods books; the
  closed-repo PDFs are the depth fallback only.
- **Open `HUMANS.md` slimmed to a community template.** Reframed as the lean, reusable **inbox-harvest** variant of
  the `AGENTS.md`/`HUMANS.md`/`HUMANS.inbox.md` protocol (agent proposes → human harvests), with a contribute
  pointer; personal working backlog moved off to a private pinboard.
- **`research/README.md` index rebuilt** — every note now listed under thematic subsections, with an **Experiments**
  subsection and `RAW-DATA.md` linked under Data (index-rot fix).
- **More research notes** — agent-psyche literature review + cross-model method, learning-barrier RQs,
  agent-affective-analogs, ssg-scoping, steering-doc-design-tension, guard-probe + guardcheck-hook +
  recommended-plugin-settings, references-summary-enum-design, model-capability-and-leverage,
  experiment-prioritization, and more. See the refreshed `research/README.md` for the full, current index.

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
