# Changelog

All notable changes to genscalator. Versions follow the git tags (`vX.Y.Z`); the `version` field in
`.claude-plugin/plugin.json` + `marketplace.json` and the version line in `AGENTS.md` track the same number.

Updating genscalator is a **human-reviewed** step — see [`docs/updating.md`](docs/updating.md). Skim this
file before adopting a new version: it changes the agent's operating rules, so review beats blind pull.

## v0.9.2 — 2026-07-24 — native fast path, typed scala/which, statusline diet
Speced in reqT-lang BEFORE the cut for once (see `reqts/PRD.md`, re-engineered from the real
`v0.9.1..HEAD` range) — the first release where FUTURE → PAST is a same-day move, not archaeology.

**New tools + capabilities:**
- **Native fast path (tt-graalify), DEFAULT-ON** — the `tt` launcher runs a GraalVM native-image dispatcher
  binary (~10–30 ms per call vs ~600 ms JVM startup) and falls back to scala-cli when the binary is stale
  (degrades to slow, never to wrong). Rebuild only via the `deploy/buildnative.sc` ritual: build → the FULL
  CLI-contract suite THROUGH the candidate (parity mode) → atomic swap. See `docs/native.md`.
- **`tt scala`** — typed driver over scala-cli (`test`/`compile`/`run`/`package-js` on a DIRECTORY target;
  no `-e` eval, no arbitrary script path, no flag passthrough) so the blanket `Bash(scala-cli *)` interpreter
  allow can be deleted from settings; per-verb allowlistable. Paired with the new SECURITY-MODEL section
  "When the tool's job is to run code".
- **`tt which`** — typed read-only "what is this command?": every `$PATH` hit in order (shadowing flagged),
  the symlink chain, magic-byte kind (ELF / script+shebang / jar / text), size/mode/mtime, bash-builtin
  honesty — absorbing the `command -v`/`which`/`type`/`file`/`readlink -f` raw-shell reflex family in one
  call that never executes the target.
- **`tt forge`** issue / PR / branch-protection READ verbs + a GitHub dialect.
- **`serverless-spa-seed`** — client-only Scala.js + Laminar todo seed (scala-cli, localStorage), the
  zero-server sibling of the crud seed.

**Statusline:**
- **Space diet under a two-glue ruling**: `·` glues a label to a state/level (`silent·3s`, `ctx·41%`),
  `↑` stays exclusively the output-flow marker (`rot?↑120k·tot↑180k`, now welded by a dim middot); the
  limit block welds into one unit under a shape-mirroring legend — `lim·%·res·5h·30%·2h|w·14%·3d`; `wk`→`w`,
  bare `$N` cost, countdowns largest-unit-only.
- **Marker-gated raw capture** (`~/.claude/gs-statusline-dump-on`) — the recall-free way to confirm the CC
  feed's fields against a real invocation. First use verified CC 2.1.218 sends only `five_hour`+`seven_day`.
- **Future-proof extra limit windows** — if a CC version ever adds a per-model weekly window it renders
  automatically with a compacted label (`f5·77%·3d`), test-pinned via simulation.

**Testing:** CLI-suite native **parity mode** (`-Dtt.native.bin`) gates the native swap; fail-fast on a
stale/partial tools dir; suite green (CliSuite 171).

**Repo shape:** `reqts/` born with insourced issues (issue-000), `HUMANS.md` built out as the extended
README, `deploy/` gathers transport, `work/NOW.md` as the tracked present.

## v0.9.1 — 2026-07-21 — snapshot release *(entry backfilled 2026-07-24)*
A plugin-snapshot release cut mid-stream (the changelog entry lagged; this is the honest backfill).
Carried the quote-aware guardcheck, skills/docs accretion, and the wr-data/research growth of 2026-07-12
→ 07-21; ground truth is the git range `v0.9.0..v0.9.1`.

## v0.9.0 — 2026-07-11 — the toolbox + onboarding release
The biggest release yet: **~14 new `tt` tools**, a **web-app seed skill**, guardcheck's auto-firing **hook mode**, and
a large docs/research/blog accretion. Operating rules touched (the `L → Z` rename, new dances, agentic-SE/RE terms) →
version bump. Human review pending (see [`HUMANS.md`](HUMANS.md)). *(reqT-lang `PRD.md` was re-engineered to specify
these as forward-looking requirements — honestly post-hoc mined from the substrate, as a worked Agentic-RE example.)*

**New tools (since v0.8.0):**
- **`tt harden`** — Layer-1 deterministic secret scanner (`repo`/`egress`): signature regexes + a Shannon-entropy
  gate + sensitive-filename detection; **redacted** output (never prints a secret); exit 1 on candidates for semantic
  (Layer-2) triage. Pure JDK; deliberately no bare-blob detection (false-positive control). (SM042)
- **`tt statusline`** — formats the Claude Code `statusLine` stdin JSON into one line (model · $ · context% · rate
  limits · reset); every segment independently guarded so it degrades gracefully and never breaks the prompt. Ends the
  `/cost` / `/usage` paste step of the token-usage dance. (SM039)
- **`tt wr stamp`** — retrofit the real timestamp of an utterance/event from the session `.jsonl` transcripts;
  **`--human`** filters to genuinely human-typed prose (drops tool_result echoes / meta / slash-command wrappers). (SM044)
- **`tt gitinfo`** — read-only git status/overview + remote sync-check (retires raw `git status`/`log`/`ls-remote`).
- **`tt git`** — typed **safe** git helper: add/commit/push + ff-only pull/fetch, commit message from a FILE; no
  reset/rebase/force/rm/clean.
- **`tt web`** — safe read-only HTTP GET (no credential headers, size-capped, optional `--host` allowlist), retiring
  the dual-use `curl` reflex. **`tt forge`** — typed Forgejo/Gitea (Codeberg) client: read releases/tags + an
  env-token `release-create` (token never via a flag, only a human-set env var).
- **`tt ssg`** — hand-rolled static-site generator (GFM subset → self-contained HTML; footnotes + Scala highlighting).
  **`tt serv`** — loopback-only static-file preview server. **`tt md-fmt`** — markdown-aware line reflow
  (structure-preserving, idempotent). **`tt box`** — host-pinned safe remote-ops (fixed verbs, no shell passthrough).
- The three sequence-diagram tools **`tt svg`** / **`tt ascii`** / **`tt gvdot`** (one shared spec parser) — detailed
  below.

**Enhancements:**
- **`tt guardcheck` gained a PreToolUse hook mode** + four new command-hygiene checks (`/dev/stdin`, heredoc,
  here-string, `grep -A/-B/-C`) — the structural half of the confirmation-guard prosthetic (SM007c). *(Wiring the
  settings hook itself stays a human step.)*
- **`tt ssg`** footnotes + Scala syntax highlighting; **`tt parsereqt`** lint hardening.

**New skill — `crud-web-app-seed`:**
- Seeds a complete, runnable Scala web app into a directory of the user's choice: a shared datamodel + a tiny
  dependency-free JSON codec, a **JDK-only** HTTP server, and a **Scala.js + Laminar** client — plus a reqT-lang
  `PRD.md` (with `verifies`-traceability) and a beginner test suite. Build + `sbt test` verified (sbt 1.x, Scala
  3.9.0-RC1). A concrete newcomer on-ramp; see the README's *Try it* section.

**Docs / foundations:**
- **Agentic software engineering / agentic RE** glossary terms (align + cite emerging prior art, not coin); the
  **plan-mode / p-word** convention; the evidence-timestamp pin-dance clause; the acronym-amortization rule + adopted
  Swedish-loanword set in the `blog-assistant` skill.

**Research / blog:** a large accretion — the ChatGPT cross-model experiment (SM040) → blog 014; the TE-efficiency,
usage/cost, and secret-scan investigations (SM039/042/044/045); many wr-data findings (cost snapshots, harness-UX /
trust nits, command-hygiene regressions, sub-agent confabulation, the surf-AFK lesson); blog stubs 015–019; the Bill
Venners "Quality from GenAI" convergence note (SM046). See `research/README.md` + `blog/`.

---

Detail from the 2026-06-30 → 2026-07-05 window (docs/research + the sequence-diagram tools):
- **New tool `tt gvdot --sequence-diagram`** — the graphviz sibling: renders the **same** spec by generating **DOT**
  and shelling to **`dot`** (auto-layout → pdf/png/svg). Effectful driver; **needs graphviz** (`sudo apt install
  graphviz`) for the render path, else prints DOT source. Safe: `dot` run as **argv, no shell**, DOT fed on stdin
  (spec can't inject). 4 CLI tests (DOT-gen path). Docs: https://graphviz.org/ , `dot -h` , `man dot`.
- **New tool `tt ascii --sequence-diagram`** — the plaintext sibling of `tt svg`: renders the **same** spec (grammar
  now shared in `tools/seqspec.scala`, a no-`@main` helper both tools `//> using file`) to a **good-looking
  monospace/box-drawing diagram** for terminals, PR/commit comments, and plaintext reports. Default Unicode
  box-drawing (`│ ─ ┌ ┐ ┬ ┴ ┼ ▶ ◀`); **`--pure`** for strict 7-bit ASCII. 5 CLI-contract tests. (`svg` refactored
  onto the shared parser — no behaviour change; its tests still pass.)
- **New tool `tt svg --sequence-diagram` (aka `tt svg sequence <in.txt> [out.svg]`)** — renders a tiny textual
  sequence-diagram spec (PlantUML/mermaid-flavoured: `actor`, `A -> B: msg`, `A --> B: reply`, `note over`) to a
  **self-contained SVG** for blogs and human-facing reports. **Theme:** default `auto` (adapts via
  `prefers-color-scheme`) or a fixed **`--light` / `--dark`** palette for predictable embedding. **Background:**
  opaque + theme-coloured by default (transparent SVG bg renders badly in Markdown), **`--transparent`** to drop it.
  Pure (JDK-only, no dep); 12 CLI-contract tests incl. a well-formed-XML parse check. Design rationale (why a bespoke spec, not
  reqT-lang: reqT is conceptually a *bag* — order not semantic, though reqT-lang preserves source order; a sequence's
  order *is* its meaning) in
  [`research/037-svg-sequence-diagram-tool.md`](research/037-svg-sequence-diagram-tool.md). First real figure:
  [`blog/figures/seq-compact-dance.svg`](blog/figures/seq-compact-dance.svg) (candidate for blog 005).
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
