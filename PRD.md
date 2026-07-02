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

### Release v0.1.0

(this will be the first release, all whats-to-come-in-this-release genscalator reqts in reqT-lang go here)

* Feature: reqTParser has
  * Gist: write a typed tool for parsing reqT-lang reqts
  * Spec: A `tt parsereqt` tool that parses the reqT-lang markdown subset (this PRD's ENT/REL/ATTR bullet grammar) into a typed model, validates it (well-formed ids in camelCase, known ENT/REL/ATTR or a clear "unknown term" error), and answers queries the agent needs: list goals/features, trace `requires`/`refines`, find orphans, and check every `BadGoal` is `mitigates`-linked by some `Feature`. Be inspired by / reuse the reqT-lang Scala parser (`05-MarkdownParser.scala`) rather than re-inventing the grammar; typed args per `research/tt-typed-args.md`; one-line friendly errors, never a stack trace.
  * Spec: **Baseline / ground truth** — feed THIS `PRD.md` to the *current* reqT-lang parser and treat the model it yields as the acceptance fixture. Where our subset diverges from what the parser accepts, resolve each divergence as EITHER a PRD fix OR a proposed improvement to the reqT-lang repo (see the review note below).
  * requires: Function: reqtLangGrammar
  * verifies: Assumption: prdParsesUnderReqTLang
* Comment: reqT-lang parser review DONE → filed as reqT/reqT-lang#15 (strict/lint mode + source positions + id-handling); see research/reqt-lang-review.md. Working model: the vendored parser is the `tt parsereqt` tool.

* Goal: verifiedTypedTools
* Feature: typedToolsTestSuite has
  * Design: testTooling has
    * Gist: use [mUnit](https://docs.scala-lang.org/toolkit/testing-intro.html) for test suites
    * Why: its a curated part of the Scala toolbox
* Comment: (agent) suggest linking these — `Feature: typedToolsTestSuite verifies Goal: verifiedTypedTools`, and `verifiedTypedTools helps safeGeneration + jointHumanAgentProductivity`. Rationale: typed tools are only HALF the safety story — static types catch TYPE errors, mUnit tests catch LOGIC errors; a tool without tests is unverified. Bonus: the suites double as the AGENT's own self-verification / regression signal before it ships a tool change (inference-time-learning.md §7). mUnit is the right pick (Scala Toolkit, zero-config with scala-cli via `//> using test.dep org.scala-lang::munit`). Prune the links if undesired.

* Feature: outputShapingFlags has
  * Gist: typed tools absorb the common output-shaping shell pipes (head/tail/wc/sort, grep -C) as native flags, so the agent never needs a downstream pipe to shape a tool's output.
  * Spec: Output shaping is a first-class flag, not a shell pipe: `--limit N` (head), `--tail N` (tail), `--count` (wc -l; already in text/files), `--sort <key>` (sort). These operate on the tool's output LINE STREAM, so they belong ONCE in the shared dispatch edge (`tt.main`, applied to the tool's `compute: Iterator[String]` result — see the monolith/client design), and every tool inherits them uniformly; tool-specific shaping like `--context N` (grep -C surrounding lines) lives in the relevant tool (e.g. `text grepr`).
  * Why: the recurring reason the agent reaches for `| head/tail/wc/sort` or raw `grep -C` is a MISSING tool flag — absorbing these into the tools removes a whole class of shell-scaffolding AT THE SOURCE (a structural fix, not exhortation), which is also why it belongs in the shared edge rather than per-tool. Evidence: repeated `| head`, `| wc`, `grep -C` reach-for-shell events in `research/wr-data/` (WR-TOOL).
* Feature: outputShapingFlags helps Goal: tokenEfficiency
* Feature: outputShapingFlags helps Goal: safeGeneration
  * Comment: fewer piped/compound shell commands = fewer confirmation-guard trips and less scaffolding to review; the shaping flags compose with the `--eager`/stream decision already planned for the edge.

* Feature: configInArgsNotEnv has
  * Gist: configuration comes from explicit command-line args/flags (or a discovered config FILE, v0.2.0), NOT ambient environment variables.
  * Spec: A tool's behaviour is determined by its argv + defaults, not by hidden env state. Prefer an explicit `--tools <dir>` / positional arg / `-D` property over reading `SOME_ENV`. Rationale: env vars are ambient — they persist across calls, leak to child processes, and are INVISIBLE to the static confirmation-guard (which reasons only about the literal command), so env-configured behaviour is unauditable and non-reproducible from the command alone.
  * Spec: DELIBERATE EXCEPTION — human TRUST BOUNDARIES stay env (or a human-owned file), NOT agent args. The verify tool's executable allowlist `TT_VERIFY_ALLOW` is env-set precisely so the agent CANNOT widen it via an agent-authored flag; there the whole point is that the config sits OUTSIDE the agent's arg surface. Rule of thumb: agent-relevant config → args/config-file; human-only authorization → env/human-file.
  * Why: WR-REGRESS — the agent passed the tools dir via a `TT_TOOLS` env var to run the test suite instead of an explicit arg (see research/wr-data).
* Feature: configInArgsNotEnv helps Goal: safeGeneration
* Feature: configInArgsNotEnv helps Goal: tokenEfficiency

### Release v0.2.0

* Feature: ttConfigFile has
  * Gist: a simple, discovered project config file for STABLE tt settings (defaults, tool dir) — replacing ambient env for non-per-invocation config; complements Feature: configInArgsNotEnv.
  * Spec: A `tt.conf` at the repo/toolbox root, discovered by walking UP from the cwd (self-locating like the `tt` launcher; its LOCATION marks the root, so `tools/` resolves relative to it). Precedence: explicit arg/flag > config file > built-in default. Ordinary config never comes from ambient env (env stays only for human trust boundaries, per configInArgsNotEnv).
  * Spec: FORMAT — a minimal `key = value` line format with `#` comments, parsed by a small zero-dep helper in `lib.scala` (~10 lines). NOT YAML/TOML: those need a parser dependency and carry spec/footgun complexity (YAML indentation + type-coercion "Norway problem") that a handful of keys do not justify — against the lean-deps + statically-analyzable ethos. (java.util.Properties is a zero-dep fallback if we would rather not hand-write a parser, but a controlled `key = value` dogfoods the "typed tool parses a simple text format" pattern, cf. reqT-lang.) Example: `tools = ./tools`, `default.eager = false`.
  * requires: Feature: configInArgsNotEnv
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
* Feature: ttConfigFile helps Goal: tokenEfficiency
  * Comment: a discovered config file is auditable (a readable file in the repo) and statically analyzable, unlike ambient env, and removes the need to re-pass stable settings on every invocation.

## PAST

Here are requirements that are either implemented or cancelled. Move requirements from FUTURE to PAST as the move on.

### IMPLEMENTED

### Release v0.0.1

(there will be no v0.0.1 this is just to show how headings are use)

### CANCELLED
