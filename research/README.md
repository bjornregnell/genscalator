# research/ — open investigation log

genscalator is a **research project**: the tools, skills, and docs are distilled from real investigations
done in the open. This folder is where those investigations live — plans *and* results — so the community
can see what's being explored, what we learned, and how it fed back into genscalator.

## The one rule: research must not interfere with daily use
Research notes are for **humans and for agents that are *explicitly* doing research** — never loaded into
an agent's working context during ordinary tool use.

- **No file here is referenced from `AGENTS.md` (core contract) or from a skill body that loads during a
  task.** Pulling research prose into the working context would bloat it and risk the *dumb zone* (see
  [`../docs/foundations.md`](../docs/foundations.md) glossary: *smart zone / dumb zone*, *token efficiency*).
- An agent consults `research/` only when the human asks it to investigate, not while doing the user's task.
- **Exception — contribution mode:** when an agent is about to *propose a contribution back* to genscalator
  (or has spotted a possible improvement), it should first check the README **roadmap** and `research/` to
  align with on-going work and avoid duplicating or contradicting it. See [`../CONTRIBUTING.md`](../CONTRIBUTING.md).

## How an investigation works
1. One file per investigation: `research/<topic>.md`, using the template below.
2. Findings that generalize **graduate** into edits to `tools/` / `docs/` / `skills/` + a release; the
   investigation file records *what shipped* so the trail stays auditable.
3. Keep entries lightweight — this is a lab notebook, not a paper.

### Template
```
# <Investigation title>
- **Question:** what are we trying to learn?
- **Why it matters:** which genscalator goal/tradeoff this serves.
- **Plan:** how we'll investigate.
- **Status:** open / in progress / shipped / parked.
- **Findings:** what we learned (grows over time).
- **What shipped:** concrete changes to genscalator, with version.
```

## Investigations

*(Grouped by theme for findability; a note may touch several themes. `METHODOLOGY.md` is the umbrella — the
research method the rest of this folder runs under.)*

- [`METHODOLOGY.md`](METHODOLOGY.md) — the research method itself: **Design Science Research nested in Action
  Research on a single longitudinal case study** (the human↔agent workflow), with the agent as both subject and
  co-analyst. The umbrella all the notes below sit under; member-checked from BR's stub.

### Scala style
- [`scala-style-recommendations.md`](scala-style-recommendations.md) — the **common-style note** itself
  (Odersky, Regnell & Kerr, Jan 2026): braceless-vs-braceful as *a matter of degree*, braces on long scopes. The
  source document blog/002 builds on and the experiment sets out to test.
- [`scala-style-evolution.md`](scala-style-evolution.md) — how should the `scala-style` skill self-consciously
  evolve, guided by agents using it, without drifting or bloating?

### Context fill, smart zone, token budget
- [`smart-zone-ceiling.md`](smart-zone-ceiling.md) — can we *estimate* **Z**, the context-fill fraction at which
  the agent crosses smart→dumb zone, so an instrument can brake before degradation rather than at the hard limit?
- [`token-budget-awareness.md`](token-budget-awareness.md) — how can an agent become aware of its token-spending
  *rate* across nested limits and *pace* itself, so brittle spend never causes a hard halt mid-task?
- [`proactive-compaction-point.md`](proactive-compaction-point.md) — should we compact *proactively* (before the
  `0.8·Z` brake), and when? Argues **lazy** compaction with one exception, the **consolidation point** (committed +
  pushed + notes-updated). Where the `L → Z` rename was decided.
- [`communication-bandwidth.md`](communication-bandwidth.md) — for a non-native-English human + an English-centric
  agent, what's the bandwidth- and TE-optimal language *per direction*? (Human L1 in, agent cheapest-clear out.)
- [`steering-doc-design-tension.md`](steering-doc-design-tension.md) — the tension between **terse hard DO/DON'Ts**
  (salient under rot, but nuance-lossy) and **nuanced guidance** (judgment-preserving, but rot-fragile) in any
  high-steering doc; how to get both.

### Instruction surfaces, adherence, inference-time learning
- [`instructions-for-claude.md`](instructions-for-claude.md) — highest-leverage content for the agent's *global*
  custom-instructions field vs what belongs in AGENTS.md/skill/memory. Includes BR's current instructions as a
  worked example.
- [`instruction-surfaces-precedence.md`](instruction-surfaces-precedence.md) — how *all* instruction surfaces
  (global, AGENTS.md/CLAUDE.md, SKILL.md, MEMORY.md, system prompt) compose, conflict, and **rank**. The governance
  layer.
- [`instruction-adherence-decay.md`](instruction-adherence-decay.md) — *why* the agent keeps regressing to
  dynamic-shell bundles despite explicit rules: no external guardrail, trained-prior reflex re-sampled per call →
  the fix is structural, not exhortation. The foundational justification for the whole safe-by-design method.
- [`inference-time-learning.md`](inference-time-learning.md) — the frozen pre-training/inference boundary means
  learning must be **externalized**; genscalator migrates insight DOWN a reliability hierarchy (in-context →
  memory → structural). One strong candidate frame for the first paper.
- [`learning-barrier-rqs.md`](learning-barrier-rqs.md) — the keeper RQs + the **containment thesis**: will the
  frontier stack learn better at inference time; the 3-way confound (model × harness × substrate) and how to
  decompose it. Ties to blog 005/006/007 and the cross-model method.

### Confirmation guard and safe-by-design
- [`confirmation-guard-static-analysis.md`](confirmation-guard-static-analysis.md) — a model of *when and why* the
  harness confirmation-guard fires. **Sound, not complete** → every prompt is a true positive (fix the reflex) or a
  false positive (fix the notation). "Be safe" and "avoid prompts" are one goal from two sides.
- [`harness-guard-probe-and-custom-guard.md`](harness-guard-probe-and-custom-guard.md) — jointly introspect *why/how*
  the guard fires, then build our OWN guard that is more precise and stronger; method drives a headless `claude -p`
  session as a repeatable probe. Extends the static-analysis note from model → artifact.
- [`guardcheck-hook-proposal.md`](guardcheck-hook-proposal.md) — *(DRAFT, needs BR approval)* a **PreToolUse hook**
  that guard-checks a proposed command and injects the finding back into context, closing the agent-perception gap.
  The prosthetic habit made structural. Not activated (hooks/settings are human-approved).
- [`recommended-plugin-settings.md`](recommended-plugin-settings.md) — which `settings.local.json` rules to *ship*
  with the plugin: minimal confirmation friction **without** widening the attack surface. A safe-by-design
  deliverable, not an afterthought.
- [`instrumentation-by-default.md`](instrumentation-by-default.md) — should scratch/CLI tools emit their own
  progress file + per-event log + compact summary *by default*, so agents Read an instrument instead of wrapping the
  tool in shell? The design-principle synthesis of the `wr-data/` evidence.

### Autonomy, human/agent state, collaboration
- [`task-autonomy-negotiation.md`](task-autonomy-negotiation.md) — per task, where does it sit between a hand-over
  **ralph-loop** and an active **ballgame**? Verifiability is the deciding signal; agent proposes the mode, human
  confirms.
- [`human-state-and-joint-zone.md`](human-state-and-joint-zone.md) — model the *human's* smart/dumb zone too, and
  the **joint** 2×2. Both-dumb = collapse; human-dumb+agent-smart is the subtly dangerous field. The agent as
  **stabilizer**; names thriller state + the rest dance.
- [`agent-affective-analogs.md`](agent-affective-analogs.md) — the mirror of the human-state note: do human
  affective constructs have functional **agent analogs**, and does prompt **framing** act as an arousal lever?
  (Testable on the framing-as-arousal experiment.)
- [`shared-file-editing-protocol.md`](shared-file-editing-protocol.md) — when human and agent edit the same file
  (HUMANS.md), how to avoid clobbering live edits? Partition must be file-level; inbox (safest) vs two-writer
  (lowest-churn) trade-off. *(Now embodied in the two HUMANS.md variants.)*
- [`subagent-genscalator-propagation.md`](subagent-genscalator-propagation.md) — does a spawned sub-agent inherit
  genscalator's `tt` tools, skills, methodology, and allowlist — so delegated work stays safe-by-design? If not, how
  to make it so.
- [`defining-an-agent-task-state-model.md`](defining-an-agent-task-state-model.md) — *(stub)* the states an ongoing
  agent task moves through (Candidate → Scoped → Ready → InProgress ↔ Blocked → Verified → Done, + Parked/Dropped),
  with a state diagram — a shared vocabulary under the AFK dance + task-autonomy triage.

### Agent psyche and cross-model method
- [`agent-psyche-literature-review.md`](agent-psyche-literature-review.md) — *(proposed)* a grounding lit review of
  academic "agent psyche" work before publishing our coinages (echt, introspection→structure, psyche dual), to map
  onto accepted terms and not contradict solid empirical work. Feeds blog 006/008.
- [`cross-model-psyche-comparison.md`](cross-model-psyche-comparison.md) — empirically comparing frontier-model
  "psyche" (Opus 4.8 vs Fable 5). **⚠ Gating:** design the method AND capture the Opus-4.8 baseline BEFORE the
  one-way Fable switch, or the before/after is confounded (conclusion validity).
- [`model-capability-and-leverage.md`](model-capability-and-leverage.md) — how a more-capable base model leverages
  the genscalator substrate vs a weaker one: constant multiplier, diminishing, or growing? The empirical core of the
  substrate-as-multiplier claim. Also fixes *hold the model constant* during WR-data collection.

### Tools, design decisions, meta
- [`reqt-lang-review.md`](reqt-lang-review.md) — using reqT-lang for genscalator's own PRD dogfoods the
  structure-beats-prose thesis. Verdict: **MAP not FORK** (reqT already fits); top proposal is an opt-in strict/lint
  mode (filed as reqT-lang#15).
- [`tt-typed-args.md`](tt-typed-args.md) — should `tt` tools adopt Scala `@main` typed params, roll their own typed
  arg layer, or mix? Typed args at the tool boundary = smarter+safer applied to the CLI itself.
- [`references-summary-enum-design.md`](references-summary-enum-design.md) — design-validation of the `Summary` enum
  (Generated/Book/Other) in blog/References.scala: where the schema strained, why the enum beats a refined String, and
  the iron-macro-StackOverflow-on-long-literals finding (→ long prose stays `String`).
- [`references-refactor-plan.md`](references-refactor-plan.md) — *(pinned TODO)* split `References.scala`'s rendering
  logic from its data (`mkMarkdown(r)` etc., likely a separate render module / an SSG resource); parked-but-pinned.
- [`kyo-ai-inspiration.md`](kyo-ai-inspiration.md) — *(to investigate)* what genscalator can mine from **kyo-ai**
  (Scala 3 algebraic-effects LLM/agent module) — typed effects for agent workflows.
- [`experiment-prioritization.md`](experiment-prioritization.md) — the rule for *what experiment to run next*: the
  one most likely to build valuable **new** knowledge relative to the planet's research front (marginal frontier
  value).
- [`go-verb-vocabulary.md`](go-verb-vocabulary.md) — *(proposal)* a small, **hardened** set of `go X` verbs
  (`go stub` / `go draft` / `go fix` / `go sweep` / `go menu` / `go harden`) that name frequent scoped actions,
  bounded by the **learnability-budget** tradeoff (prune before adding).

### Cases and roadmap
- [`ssg-scoping.md`](ssg-scoping.md) — scoping the static-site generator to publish the blog to bjornregnell.se;
  agent-scouted, leaning **Laika**, with a spike as the next step. Input to the SSG WR case study.
- [`course-driven-development.md`](course-driven-development.md) — *(idea)* run BR's MSc RE course as three student
  teams (reqT / genscalator / app) so students become a **contributor pipeline** into the OSS community.

### Experiments
- [`experiments/indent-vs-braces/`](experiments/indent-vs-braces/) — the edit-cost pilot behind blog **002/003**:
  design [`README.md`](experiments/indent-vs-braces/README.md), results [`RESULTS.md`](experiments/indent-vs-braces/RESULTS.md),
  preregistered follow-up [`BIG-RUN-PREREG.md`](experiments/indent-vs-braces/BIG-RUN-PREREG.md), raw
  [`results-raw.tsv`](experiments/indent-vs-braces/results-raw.tsv).
- [`experiments/framing-as-arousal/`](experiments/framing-as-arousal/) — *(preregistered)* WR2: does prompt
  **framing intensity** act as an arousal lever on agent behaviour (Yerkes-Dodson), holding the task constant? The
  runnable test of `agent-affective-analogs.md`.

## Data
- [`wr-data/`](wr-data/) — **WR** (Workflow Research) log of confirmation/approval events from real sessions that
  are candidates for elimination by a new safe-by-design `tt` tool. The raw evidence behind the confirmation-fatigue
  thesis; feeds *which tool to build next*.
- [`RAW-DATA.md`](RAW-DATA.md) — the **append-only** raw ledger (never retro-edited: a changed mind is new data).
  The auditable chain from observation → claim.
- [`substrate-consistency-check-2026-07-04.md`](substrate-consistency-check-2026-07-04.md) — a dated
  agent-run consistency sweep of the substrate (agent-done vs BR-needed); a reusable report shape.
