# Product Requirements Document (PRD) for genscalator

This document includes requirements for the genscalator product including all typed tools (TT or tt) and other **agent capabilities** — skills, plugins, and other artifacts (often in markdown or Scala code) that help **escalate human-agent code generation to the next level**. ("agent capabilities" is the proposed umbrella word for line-3's open question: a tt tool, a skill, and a plugin each *is-a* Capability; see the `Capability`/`Tool`/`Skill` entities below.)

*TAP:* To Agent Plan: refactor this whole repo so that things that belong here are moved here

## META

This document includes specs in reqT-lang, a markdown-subset for expressing requirements using meta-level concepts of ENT (requirement entities), REL (requirements relations), ATTR (requirements attributes).

### Why reqT-lang here (rationale — also a first-paper argument)

Using a **parseable requirements language** for genscalator is not incidental — it **dogfoods the project's own central claim**. genscalator's thesis is *migrate insight into structure the agent can rely on, not prose*. A prose PRD is precisely the fragile, unqueryable substrate we fight; a **reqT-lang PRD is a structured, diffable, agent-consultable model** — the same move applied to the project's own requirements. Concretely it buys three things a prose PRD cannot:

1. **Shared vocabulary** for human↔agent requirement-talk (ENT/REL/ATTR is small and memorable, like the WR-* codes / smart-zone terms — *vocabulary as infrastructure*).
2. **Traceability the agent can follow** — `Feature requires Feature`, `Goal has Spec` let an agent check completeness, find orphans, and order work deterministically instead of re-reading prose.
3. **"Safe by design" as checkable relations** — the threat model (below) stops being hand-wavy once `Feature mitigates BadGoal` and `Metric verifies Goal` are actual links.

The complexity risk ("too much machinery") is real **only** if we import the whole reqT meta-model or formalize every fleeting idea. Mitigations: keep the **minimal subset** (below); scope reqT-lang to **stable, cross-cutting** requirements while fleeting ideas stay in `research/` + `wr-data/`; and remember the **agent-payoff needs a parser** (the `reqTParser` feature) — until then this is "structured prose" (already useful for humans and as a spine, but the agent-leverage lands with the tool). Roles to prevent drift: **PRD = requirements spine · research/ = investigations · wr-data = evidence · memory = agent ops.**

*(WR / paper note: this rationale — reqT-lang adoption as dogfooding the structure-beats-prose thesis — is flagged as important input for the first research paper.)*

**Abstract example** of concrete syntax with abstract terms:

* ENT: id
* ENT: id REL
  * ENT: id
  * ATTR: text
  * ENT: id

**Concrete example:**

* Comment: A string attribute with informative text.
* Feature: yyy has Prio: 42
* Feature: yyy requires Feature: xxx
* Feature: xxx has
  * Prio: 12
  * Spec: A longer textual specification
      that is spanning several lines. A longer textual specification
      that is spanning several lines.
  * UseCase: zzz has
    * Prio: 23

**Convention:**

* **abbreviations** reqt and reqts is short for just requirement(s) and reqT-lang the language (and [reqT](https://reqt.github.io/) is a desktop tool not used here (yet), we stick with the language in md files for now)
* identifiers (id) are camelCase
* We only use this subset of reqT-lang (may be extended by agent or human as soon as we see fit). Core (BR):
  - ENT: Goal, Feature, Function, Stakeholder, ...
  - REL: has, requires, ...
  - ATTR: Spec, ...
* **Mapping to reqT's existing vocabulary (agent review 2026-07-01 → `research/reqt-lang-review.md`; MAP not FORK).** Almost everything we need already exists in reqT's meta-model with the standard KAOS/i*/GRL semantics — so we use those rather than invent:
  - anti-goal ("BadGoal") → a **`Goal` owned by an adversarial stakeholder that our Features `hurt`** (BR decision 2026-07-01: model as *Goal-we-Hurt*, NOT a new `BadGoal` concept, NOT `Barrier`).
  - `mitigates` / `conflictsWith` → **`Hurts`** ("negative influence; a goal hinders another"); positive contribution → **`Helps`**.
  - `verifies` → **`Verifies`** ✅ ("a test verifies a feature"); `Rationale` → **`Why`** ✅; `Metric` → **`Target`** / **`Quality`** (+ `Min`/`Max`/`Value`).
  - already present, used as-is: `Goal`, `Feature`, `Function`, `Stakeholder`, `Risk`, `Term`, `Gist`, `Example`, `Spec`, `Prio`, `Requires`, `Has`.
  - genscalator product nouns (`Tool`, `Skill`) → map to **`Component`** / **`Function`** for now (propose a domain vocabulary upstream only if the mapping loses meaning); "agent capabilities" stays the prose umbrella word.
  - `Status` → the FUTURE/PAST headings already encode lifecycle (or `Deprecated`); no new attr.

*TAP:* To Agent Plan: investigate what more entities, relations, attributes agent thinks we need from the reqT-lang meta model
  *(agent review done → `research/reqt-lang-review.md`: MAP not FORK — reqT already has what we need. Only open item: whether an explicit `Antigoal` EntType is worth adding upstream; BR chose to model anti-goals as Goal-we-Hurt for now, so NO new concept. reqT-lang parser feedback filed as issue reqT/reqT-lang#15.)*

**reqT-lang language specification:**

* In prose: https://github.com/reqT/reqT-lang/blob/main/docs/langSpec-GENERATED.md

* In code:
  - Class hierarchy for abstract syntax tree (generated): https://github.com/reqT/reqT-lang/blob/main/src/main/scala/03-model-GENERATED.scala
  - Informal semantics (see strings): https://github.com/reqT/reqT-lang/blob/main/src/main/scala/02-meta-model.scala
  - Syntax: https://github.com/reqT/reqT-lang/blob/main/src/main/scala/05-MarkdownParser.scala


### reqT-lang syntax rules the agent MUST follow (empirically verified via `tt parsereqt`; → future reqt-lang skill)

Learned by running snippets through the REAL parser (`tt parsereqt parse FILE`), not by guessing. Belongs in the reqt-lang skill; kept here for now.

1. **Relations bind two entities — NEVER list a relation under `has`.** A bullet like `* requires: Feature: X` placed *inside* a `Feature: Y has` block does NOT create a relation: the parser turns it into an inert `StrAttr(Text, "requires: Feature: X")` and the relation is SILENTLY LOST. And because `requires`/`verifies`/`helps` are lowercase, `lint`'s `^[A-Z]` concept-like check does NOT flag them. Verified: buggy snippet parsed to `...Has,Model(...,StrAttr(Text,"  requires: Feature: dep"),...)`.
2. **Write a relation as its own top-level clause: an `ENT REL` line whose indented children are the target(s).** A relation binds its owner to ONE OR MORE sub-elements — exactly the way `has` binds many attrs/entities (rule 3). Short single-target form: `* Feature: ttConfigFile requires Feature: configInArgsNotEnv` → `Rel(...,Requires,Model(Ent(Feature,configInArgsNotEnv)))`. Multi-target form: a `* Feature: cfg requires` bullet with indented `* Feature: dep1` and `* Feature: dep2` children → `Rel(...,Requires,Model(Ent(Feature,dep1),Ent(Feature,dep2)))` (BOTH bound). Both verified.
3. **`has` holds the owner's ATTRs and sub-ENTs.** Under `* Feature: F has`, indented bullets (`Gist`, `Spec`, `Comment`, `Idea: k has Gist: ...`) attach to F; a sub-entity may nest its own inline `has` (`Idea: k has Gist: g` → F has Idea k, and Idea k has Gist g). Verified.
4. **An unknown or typo'd leading `Word:` falls through to `Text`** (parser ~line 188). `lint` catches only the CAPITALIZED ones (`Feautre:`, `BadGoal:`); a lowercase mis-relation slips through silently — so verify structural edits by PARSING, not eyeballing.
5. **Method: when unsure, `tt parsereqt parse <snippet>` and read the `Model(...)`.** The parser is ground truth; do not trust a mental model of the grammar.

## Stakeholders and their goals (from `research/`: agent-is-the-tool-user + BHH threat model)

* Stakeholder: human has
  * Goal: avoidConfirmationFatigue
  * Goal: avoidReviewOverload
  * Goal: contributeOpenSource
* Stakeholder: agent has
  * Goal: tokenEfficiency
  * Goal: safeActionsRunWithoutConfirmation
* Stakeholder: bhh has
  * Comment: BHH = Black Hat Hacker; an ADVERSARIAL stakeholder — these are anti-goals we design AGAINST (Features `hurt` them), NOT goals we pursue.
  * Goal: controlHumanSystem
  * Goal: exfiltrateSecrets

## General goals (stable over time)

* Goal: tokenEfficiency has
  * Spec: The agent accomplishes a task with the fewest tokens that preserves quality — cheap-but-clear over verbose, typed tools over shell scaffolding, and self-pacing to stay in the smart zone (context fill below the L ceiling).
  * Why: fewer tokens = lower cost AND better quality (a full context degrades into the dumb zone).
* Goal: safeGeneration has
  * Spec: Generated code and agent actions never advance a bhh anti-goal. Precisely: the human's no-CF/no-review-overload goals and the agent's convenience goals are met WITHOUT ever widening the bhh attack surface. A safe action should be provably safe (statically analyzable) so it needs no per-action confirmation.
  * Target: confirmationsPerSession
  * Target: guardTripsPerSession
* Goal: jointHumanAgentProductivity has
  * Spec: The human-agent pair produces more, and more reliable, software per unit of scarce resource (human attention, tokens, wall-clock) than either alone or than the out-of-the-box baseline. This is the down-to-earth backbone thesis.
  * Target: tokensToGreen
  * Target: brokenBuildIterations

## Safe-by-design as traceability (worked example, in reqT's EXISTING relations)

* Feature: safeByDesignTooling has
  * Gist: tt tools that are statically analyzable + effect-declared, so trust is granted once, not per action.
  * Spec: Each tt tool is a literal, single, typed command that declares its effects (--sandboxed / --safe-mode / --audit). Because the confirmation-guard can PROVE such a command safe, it runs without a prompt — so cutting confirmation fatigue never forces a fatigued "always allow" that could serve the bhh controlHumanSystem goal.
* Feature: safeByDesignTooling hurts Goal: controlHumanSystem
* Feature: safeByDesignTooling helps Goal: avoidConfirmationFatigue
* Target: confirmationsPerSession verifies Goal: safeGeneration
  * Comment: the confirmation-fatigue hazard — a CF-driven "always allow" is a bhh goal served by fatigue; safeByDesignTooling `hurts` that adversary goal AND `helps` the human's no-CF goal, and the confirmationsPerSession target `verifies` safeGeneration. All in existing reqT relations (hurts/helps/verifies).

## FUTURE

### Roadmap

The next real release is **v0.9.0** — v0.1.0–v0.8.0 have shipped (see PAST/IMPLEMENTED, backfilled from
`CHANGELOG.md` as ground truth). The requirements below are the genuinely-unshipped work, in reqT-lang.

> **Bootstrap note (2026-07-03):** this PRD was re-engineered retrospectively — we did NOT author reqT-lang
> reqts before each release. To keep it realistic, PAST was reconstructed release-by-release from the
> CHANGELOG *as if* each version's Features had been specified here first; FUTURE is what remains. All
> reqT-lang here is validated with `tt parsereqt`.

### Release v0.9.0 — HTTP + forge tooling, parser hardening, tool tests (next)

* Feature: ttWeb has
  * Gist: safe read-only HTTP for agents — GET only, no credential headers, size-capped, optional --host allowlist — replacing the dual-use curl reflex.
  * Spec: `tt web get <url>` can ONLY fetch-and-print, so `Bash(tt web get *)` is blanket-allowlistable where a bare `curl *` allowlist would expose exfiltration (`curl -d @secret`), RCE (`curl | sh`), and credential leaks; residual SSRF-read is locked down with --host.
* Feature: ttWeb helps Goal: tokenEfficiency
* Feature: ttWeb helps Goal: safeGeneration
* Feature: ttWeb hurts Goal: exfiltrateSecrets

* Feature: ttForge has
  * Gist: Forgejo/Gitea forge client (default Codeberg) — whoami/releases/tags (READ) + release-create (effectful) — replacing hand-curling the REST API with a token on the command line.
  * Spec: READ verbs need no auth (allowlistable); whoami/release-create read the token ONLY from a FIXED set of human-set env names (GENSCALATOR_CODEBERG_TOKEN/CODEBERG_TOKEN/FORGE_TOKEN), never a flag, AND a trusted-host guard pins the destination (codeberg.org; human-extends via TT_FORGE_HOSTS) — so the agent can neither self-authorize nor exfiltrate the token via --url.
* Feature: ttForge relatesTo Feature: ttWeb
* Feature: ttForge helps Goal: safeGeneration
* Feature: ttForge hurts Goal: exfiltrateSecrets
* Feature: ttForge hurts Goal: controlHumanSystem

* Feature: reqTParser has
  * Gist: write a typed tool for parsing reqT-lang reqts
  * Spec: A `tt parsereqt` tool that parses the reqT-lang markdown subset (this PRD's ENT/REL/ATTR bullet grammar) into a typed model, validates it (well-formed ids in camelCase, known ENT/REL/ATTR or a clear "unknown term" error), and answers queries the agent needs: list goals/features, trace `requires`/`refines`, find orphans, and check every `BadGoal` is `mitigates`-linked by some `Feature`. Be inspired by / reuse the reqT-lang Scala parser (`05-MarkdownParser.scala`) rather than re-inventing the grammar; typed args per `research/tt-typed-args.md`; one-line friendly errors, never a stack trace.
  * Spec: **Baseline / ground truth** — feed THIS `PRD.md` to the reqT-lang parser and treat the model it yields as the acceptance fixture; resolve each divergence as EITHER a PRD fix OR a proposed upstream reqT-lang improvement. **CORE IMPLEMENTED** as `tt parsereqt` (vendored parser) — it authored + validates THIS PRD. REMAINING for a numbered release: native in-parser strict mode + source positions (reqT/reqT-lang#15).
* Feature: reqTParser requires Function: reqtLangGrammar
* Comment: reqT-lang parser review DONE → filed as reqT/reqT-lang#15 (strict/lint mode + source positions + id-handling); see research/reqt-lang-review.md. Working model: the vendored parser is the `tt parsereqt` tool.

* Goal: verifiedTypedTools has
  * Gist: every typed tool is covered by tests — static types catch TYPE errors, mUnit tests catch LOGIC errors; a tool without tests is unverified.
* Feature: typedToolsTestSuite has
  * Design: testTooling has
    * Gist: use [mUnit](https://docs.scala-lang.org/toolkit/testing-intro.html) for test suites
    * Why: its a curated part of the Scala toolbox
  * Spec: extend coverage to `tt web` + `tt forge` (offline usage/arg-error paths), matching the htmltext/chrono pattern — a parked AGENT-owned task.
* Feature: typedToolsTestSuite verifies Goal: verifiedTypedTools
* Goal: verifiedTypedTools helps Goal: safeGeneration
* Goal: verifiedTypedTools helps Goal: jointHumanAgentProductivity
* Comment: (agent) suggest linking these — `Feature: typedToolsTestSuite verifies Goal: verifiedTypedTools`, and `verifiedTypedTools helps safeGeneration + jointHumanAgentProductivity`. Rationale: typed tools are only HALF the safety story — static types catch TYPE errors, mUnit tests catch LOGIC errors; a tool without tests is unverified. Bonus: the suites double as the AGENT's own self-verification / regression signal before it ships a tool change (inference-time-learning.md §7). mUnit is the right pick (Scala Toolkit, zero-config with scala-cli via `//> using test.dep org.scala-lang::munit`). Prune the links if undesired.

* Feature: outputShapingFlags has
  * Gist: typed tools absorb the common output-shaping shell pipes (head/tail/wc/sort, grep -C) as native flags, so the agent never needs a downstream pipe to shape a tool's output.
  * Spec: Output shaping is a first-class flag, not a shell pipe: `--limit N` (head), `--tail N` (tail), `--count` (wc -l; already in text/files), `--sort <key>` (sort). These operate on the tool's output LINE STREAM, so they belong ONCE in the shared dispatch edge (`tt.main`, applied to the tool's `compute: Iterator[String]` result — see the monolith/client design), and every tool inherits them uniformly; tool-specific shaping like `--context N` (grep -C surrounding lines) lives in the relevant tool (e.g. `text grepr`).
  * Why: the recurring reason the agent reaches for `| head/tail/wc/sort` or raw `grep -C` is a MISSING tool flag — absorbing these into the tools removes a whole class of shell-scaffolding AT THE SOURCE (a structural fix, not exhortation), which is also why it belongs in the shared edge rather than per-tool. Evidence: repeated `| head`, `| wc`, `grep -C` reach-for-shell events in `research/wr-data/` (WR-TOOL).
* Feature: outputShapingFlags helps Goal: tokenEfficiency
* Feature: outputShapingFlags helps Goal: safeGeneration
  * Comment: fewer piped/compound shell commands = fewer confirmation-guard trips and less scaffolding to review; the shaping flags compose with the `--eager`/stream decision already planned for the edge.

* Feature: configInArgsNotEnv has
  * Gist: configuration comes from explicit command-line args/flags (or a discovered config FILE, `ttConfigFile`), NOT ambient environment variables.
  * Spec: A tool's behaviour is determined by its argv + defaults, not by hidden env state. Prefer an explicit `--tools <dir>` / positional arg / `-D` property over reading `SOME_ENV`. Rationale: env vars are ambient — they persist across calls, leak to child processes, and are INVISIBLE to the static confirmation-guard (which reasons only about the literal command), so env-configured behaviour is unauditable and non-reproducible from the command alone.
  * Spec: DELIBERATE EXCEPTION — human TRUST BOUNDARIES stay env (or a human-owned file), NOT agent args. The verify tool's executable allowlist `TT_VERIFY_ALLOW` is env-set precisely so the agent CANNOT widen it via an agent-authored flag; there the whole point is that the config sits OUTSIDE the agent's arg surface. Rule of thumb: agent-relevant config → args/config-file; human-only authorization → env/human-file.
  * Why: WR-REGRESS — the agent passed the tools dir via a `TT_TOOLS` env var to run the test suite instead of an explicit arg (see research/wr-data).
* Feature: configInArgsNotEnv helps Goal: safeGeneration
* Feature: configInArgsNotEnv helps Goal: tokenEfficiency

### Release v0.10.0 — config file, parser fall-through marker, safe-mode flags, MCP (later)

* Feature: ttConfigFile has
  * Gist: a simple, discovered project config file for STABLE tt settings (defaults, tool dir) — replacing ambient env for non-per-invocation config; complements Feature: configInArgsNotEnv.
  * Spec: A `tt.conf` at the repo/toolbox root, discovered by walking UP from the cwd (self-locating like the `tt` launcher; its LOCATION marks the root, so `tools/` resolves relative to it). Precedence: explicit arg/flag > config file > built-in default. Ordinary config never comes from ambient env (env stays only for human trust boundaries, per configInArgsNotEnv).
  * Spec: FORMAT — a minimal `key = value` line format with `#` comments, parsed by a small zero-dep helper in `lib.scala` (~10 lines). NOT YAML/TOML: those need a parser dependency and carry spec/footgun complexity (YAML indentation + type-coercion "Norway problem") that a handful of keys do not justify — against the lean-deps + statically-analyzable ethos. (java.util.Properties is a zero-dep fallback if we would rather not hand-write a parser, but a controlled `key = value` dogfoods the "typed tool parses a simple text format" pattern, cf. reqT-lang.) Example: `tools = ./tools`, `default.eager = false`.
  * Comment: candidate config keys (BRAINSTORM — `Idea` = "a concept or thought, potentially interesting", per `tools/reqt-vendored/02-meta-model.scala`; these are exploratory, NOT yet committed keys):
  * Idea: toolsPath has Gist: location of the toolbox `tools/` dir — the original driver, replaces the TT_TOOLS env reach; defaults to `./tools` relative to the config file.
  * Idea: scalaVersion has Gist: single source for the Scala version the tools compile with, so a bump (e.g. 3.8.4 → 3.9.0-RC1) happens in ONE place instead of every per-file `//> using scala`.
  * Idea: defaultEager has Gist: default output mode for the monolith edge — stream (lazy) vs collect (eager); overridable per call by `--eager`.
  * Idea: defaultLimit has Gist: default `--limit` (head) the shared edge applies so large outputs do not flood the agent's context unless a call opts out; ties to Feature: outputShapingFlags.
  * Idea: maxOutputBytes has Gist: hard truncation ceiling on any tool's output (protects agent context / tokenEfficiency) — a safety cap, distinct from the defaultLimit default.
  * Idea: defaultExtensions has Gist: default file-extension filter for `grepr`/`files` (e.g. `.scala,.md`) so common searches need not repeat it.
  * Idea: auditLogPath has Gist: file the shared gate appends each tool invocation to — the session-visibility banner's durable sink, doubling as WR-instrumentation.
  * Idea: colorOutput has Gist: ANSI colour on/off/auto — a human terminal wants colour, an agent parsing stdout usually wants it off.
  * Idea: logLevel has Gist: verbosity of the gate banner + hints (quiet/normal/verbose), so the human can dial down chatter.
  * Idea: subprocessTimeoutSeconds has Gist: default timeout for effectful drivers (verify, os.proc runs) so a hung child fails cleanly instead of blocking.
  * Comment: SAFETY CAUTION (ties to configInArgsNotEnv): the verify executable-allowlist must NEVER become a config key — a config-redirectable allowlist location would let the agent point verify at a permissive list = self-authorization. Trust boundaries stay human-env/human-file, never agent-editable config.
* Feature: ttConfigFile requires Feature: configInArgsNotEnv
* Feature: ttConfigFile helps Goal: tokenEfficiency
  * Comment: a discovered config file is auditable (a readable file in the repo) and statically analyzable, unlike ambient env, and removes the need to re-pass stable settings on every invocation.

* Feature: parserFallthroughMarker has
  * Gist: the parser marks every silent Text fall-through with a searchable sentinel, so even a plain (non `--lint`) parse is greppable — total parsing preserved, NO metamodel change.
  * Spec: when the parser degrades an unrecognized or mis-formed bullet to a `Text` attribute, it prefixes the value with a distinctive searchable sentinel — e.g. `<<<reqt-fallthrough: run tt parsereqt lint>>>`. The parser still NEVER errors (permissive by design when not in `--lint` mode); the sentinel just makes a drop findable by a plain `grep '<<<reqt-fallthrough'` over BOTH the source `.md` and the parsed model, so a silent fall-through can never ship unnoticed.
  * Spec: NOT a metamodel type. `Failure` is a DOMAIN concept ("A description of a runtime error that prevents the normal execution of a system", per `tools/reqt-vendored/02-meta-model.scala`); parse-status is meta-meta and stays OUT of the metamodel. The sentinel lives inside `Text`; `--lint` remains the rich report (already extended to flag lowercase relation-keyword fall-throughs); this marker is the zero-tooling breadcrumb. Parser-side emission modifies the vendored parser → propose UPSTREAM (reqT/reqT-lang#15), not a local fork.
  * Why: a SILENT fall-through gets shipped — the ttConfigFile + reqTParser "relation lost to Text under has" bug proved it; a searchable breadcrumb makes the always-parse guarantee safe.
* Feature: parserFallthroughMarker relatesTo Feature: reqTParser
* Feature: parserFallthroughMarker helps Goal: jointHumanAgentProductivity

* Feature: safeModeFlags has
  * Gist: `--safe-mode` (capture-checking / purity), `--sandboxed` (declared scope, no network), `--audit` (a record of what a tool touched) — a tool DECLARES where it sits in the threat model so the human grants trust cheaply.
  * Spec: `--safe-mode` is the default for pure tools; `--sandboxed` makes a blanket "always allow" low-stakes; `--audit` is the bridge between faster and safer. ttVerify + ttForge already print audit lines — the prototype.
* Feature: safeModeFlags helps Goal: avoidConfirmationFatigue
* Feature: safeModeFlags hurts Goal: controlHumanSystem

* Feature: mcpServer has
  * Gist: an MCP server exposing the toolbox for cross-tool use (Claude / Codex / opencode) — the portability goal.
* Feature: mcpServer helps Goal: contributeOpenSource

## PAST

Requirements implemented (or cancelled). Move requirements from FUTURE to PAST as they ship. The IMPLEMENTED
blocks are backfilled from `CHANGELOG.md` (ground truth) — one block per released version — as the reqT-lang
spine it *would* have been had we specified reqts before each release (bootstrap re-engineering, 2026-07-03).

### IMPLEMENTED

#### Release v0.1.0 — the tt toolbox + foundations
* Feature: ttToolbox has
  * Gist: typed, compiled Scala scratch tools (text/files/newtool + shared lib) replacing the bash/grep/awk/python reflex.
  * Spec: the `tt <tool> <args>` launcher makes each tool ONE literal, statically-analyzable command matching a narrow allowlist. text = grep/awk/cut; files = find/grep -l; newtool = scaffold; lib = pure JDK-only helpers.
* Feature: ttToolbox helps Goal: tokenEfficiency
* Feature: ttToolbox helps Goal: safeGeneration
* Feature: ttToolbox hurts Goal: controlHumanSystem
* Feature: foundationsDoc has
  * Gist: docs/foundations.md — goals, stakeholders (human / agent / BHH), and the glossary.
* Feature: confirmationsMethod has
  * Gist: docs/confirmations-method.md — the method for driving down confirmation fatigue.
* Feature: confirmationsMethod helps Goal: avoidConfirmationFatigue

#### Release v0.2.0 — Claude Code plugin packaging
* Feature: pluginPackaging has
  * Gist: the repo doubles as its own Claude Code marketplace; bin/tt + the tt-toolbox skill ship as a plugin.
* Feature: contributingWorkflow has
  * Gist: CONTRIBUTING.md — the human+agent contribution workflow (agent proposes a generally-useful tool as issue+PR; human approves + submits).
* Feature: pluginPackaging helps Goal: contributeOpenSource
* Feature: contributingWorkflow helps Goal: contributeOpenSource

#### Release v0.3.0 — scalex integration
* Feature: scalexIntegration has
  * Gist: symbol-aware (AST) Scala navigation companion; docs/tool-selection.md (which tool for which question).
* Feature: scalexIntegration helps Goal: tokenEfficiency

#### Release v0.4.0 — Metals MCP
* Feature: metalsMcp has
  * Gist: compiler-grade complement above scalex (inferred types, diagnostics, run tests, refactor); read-only-vs-effectful safety split.
* Feature: metalsMcp relatesTo Feature: scalexIntegration

#### Release v0.5.0 — scala-style skill + research log
* Feature: scalaStyleSkill has
  * Gist: how to WRITE a tool — direct style, pragmatic immutability (safety ↔ TE ↔ performance), Safe-mode-ready, effects isolated to drivers.
* Feature: researchLog has
  * Gist: research/ open investigation log + research/wr-data/ (Workflow Research: confirmation events → candidate safe-by-design tools).
* Feature: greprMultiExt has
  * Gist: `tt text grepr` multi-extension scan (.scala,.java in one call) + friendly one-line errors (exit 2) on a bad/relative dir.
* Feature: researchLog helps Goal: contributeOpenSource

#### Release v0.6.0 — tt log + contribute-tool skill
* Feature: ttLog has
  * Gist: build/run-log analyzer — curated error/warn markers across build/test/runtime/LaTeX logs; customizable (--error/--warn/--no-defaults/--cap).
* Feature: contributeToolSkill has
  * Gist: the recipe to generalize a scratch tool into a toolbox-worthy tt tool before proposing it (start specific → general class → verify with adversarial fixtures → propose; human ships).
* Feature: ttLog helps Goal: tokenEfficiency
* Feature: contributeToolSkill helps Goal: contributeOpenSource

#### Release v0.7.0 — tt verify (first effectful driver)
* Feature: ttVerify has
  * Gist: run-and-verify driver — one allowlistable call (no shell) that checks exit/stdout/stderr and prints PASS/FAIL, replacing the `cd && … > log; echo $?` bundle.
  * Spec: runs the command directly as argv (no shell → ; | && $() globs inert); only executables on the allowlist run (scala-cli, tt, scalex + human-set TT_VERIFY_ALLOW); prints an audit line — the prototype of the --audit roadmap flag.
* Feature: ttVerify requires Feature: execAllowlist
* Feature: ttVerify verifies Goal: safeGeneration
* Feature: ttVerify helps Goal: tokenEfficiency
* Feature: ttVerify hurts Goal: controlHumanSystem
* Feature: execAllowlist has
  * Gist: the human-set (env, never a flag) allowlist of executables tt verify may run — the agent cannot widen it, so cutting CF never forces a fatigued over-broad "always allow".

#### Release v0.8.0 — comms shorthand
* Feature: commsShorthand has
  * Gist: a shared human↔agent acronym vocabulary (BRB/AFK/WDYT/…) emitted + parsed WITHOUT expansion — a communication-bandwidth + TE lever.
  * Spec: a foundations.md glossary entry (4 groups) + an always-on AGENTS.md section (18 inline acronyms); plus the motor-cost research thread (lowercase-in / cased-out; mobile amplification).
* Feature: commsShorthand helps Goal: tokenEfficiency

### CANCELLED
