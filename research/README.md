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
- [`scala-style-evolution.md`](scala-style-evolution.md) — how should the `scala-style` skill
  self-consciously evolve, guided by agents using it, without drifting or bloating?
- [`instrumentation-by-default.md`](instrumentation-by-default.md) — should scratch/CLI tools emit their own
  progress file + per-event log + compact summary *by default*, so agents Read an instrument instead of
  wrapping the tool in `echo`/`grep`/`for` shell? The design-principle synthesis of the `wr-data/` evidence.
- [`token-budget-awareness.md`](token-budget-awareness.md) — how can an agent become aware of its
  token-spending *rate* across nested limits (context window / session / weekly cap) and *pace* itself, so
  inefficient/brittle spend never causes a hard halt mid-task? The liveness/governance layer of the thesis.
- [`smart-zone-ceiling.md`](smart-zone-ceiling.md) — can we *estimate* **L**, the context-fill fraction at
  which the agent crosses smart→dumb zone, so an instrument can warn/brake before degradation rather than at
  the hard limit? Turns the ~30% folklore boundary into a measurable brake threshold (pairs with `token-usage`).
- [`communication-bandwidth.md`](communication-bandwidth.md) — for a non-native-English human + an
  English-centric agent, what's the bandwidth- and TE-optimal language *per direction*? The linguistic
  dimension of the human↔agent channel (human L1 in, agent cheapest-clear out; ask when L1 is idiosyncratic).
- [`instructions-for-claude.md`](instructions-for-claude.md) — what is the highest-leverage content for the
  agent's *global* custom-instructions field (loaded into every session), and what should live in
  AGENTS.md/skill/memory instead? The global-vs-local split as a TE + quality lever. Includes BR's current
  instructions as a worked example.
- [`instruction-surfaces-precedence.md`](instruction-surfaces-precedence.md) — how do *all* the instruction
  surfaces (global instructions, AGENTS.md/CLAUDE.md, SKILL.md, MEMORY.md+memory, system prompt) compose,
  conflict, and **rank**? Which are always-on vs lazy, and where's the redundancy/drift? The governance layer;
  companion to `instructions-for-claude.md`.
- [`instruction-adherence-decay.md`](instruction-adherence-decay.md) — *why* does the agent keep regressing to
  dynamic-shell bundles despite explicit rules (even one call after doing it right)? No external guardrail —
  trained-prior reflex re-sampled per call; the fix is structural (tool+allowlist+hook), not more exhortation.
  The foundational justification for genscalator's whole safe-by-design method; confirms the *Habit/Reflex* split.
- [`confirmation-guard-static-analysis.md`](confirmation-guard-static-analysis.md) — a model of *when and why* the
  harness confirmation-guard fires. It is **sound, not complete** (asks whenever it cannot prove safety, because
  shell effects are undecidable), so every prompt is either a **true positive** (agent reflex - fix the behavior)
  or a **false positive** (benign convention the parser cannot disambiguate: `<N-M>` glob, `\n#` comment-hide,
  `$VAR` - fix the notation). The `tt` design rule falls out: a literal, single, typed, effect-declared command
  is *provably* safe -> runs silently. "Be safe" and "avoid unnecessary prompts" are the same goal from two sides.
- [`task-autonomy-negotiation.md`](task-autonomy-negotiation.md) — per task, where does it sit between a
  hand-over **ralph-loop** (little human check) and an active **ballgame** (human in every volley)? Verifiability
  is the deciding signal; the agent should *propose* the mode and the human confirm. Spends the scarce resource
  (human attention) only where it changes the outcome. Generalizes the *ralph loop* glossary into a spectrum.
- [`inference-time-learning.md`](inference-time-learning.md) — agents have a frozen pre-training/inference
  boundary (no weight updates while working), so inference-time learning must be **externalized**. The frame:
  genscalator migrates insight DOWN a reliability hierarchy — in-context instruction (weakest) → persistent
  memory → environmental/structural change (strongest, e.g. the submit-time hook). Explains why exhortation
  fails and structure works; reframes memory as surrogate plasticity, the roundtrip as the learning loop, and
  the wr-data ledger as how learning is measured. **One** strong candidate frame for the first paper — one of
  several theses; the down-to-earth backbone is *human-agent productivity via static tooling* (BR sets the
  paper's focus when writing starts; see the BR note in the file).
- [`human-state-and-joint-zone.md`](human-state-and-joint-zone.md) — we model the *agent's* state (smart/dumb
  zone); should we model the *human's* too, and the **joint** (human-zone, agent-zone) 2x2? Both-dumb = collapse;
  human-dumb+agent-smart is the subtly dangerous field (rubber-stamping unreviewable work). Key payoff: the
  agent, being tireless, should act as **stabilizer** and help keep the human in the smart zone (cut prompt
  events, time ambition, propose a **rest dance**). Names thriller state + the rabbit-hole/repo-trashing ladder.
- [`shared-file-editing-protocol.md`](shared-file-editing-protocol.md) — when human and agent edit the same
  file (HUMANS.md), how to avoid the agent clobbering live human edits? Section-zones fail (editors save whole
  buffers), so partition must be file-level. Opt A (inbox) is safest but adds harvest churn; Opt C (two-writer +
  editor disk-vs-buffer safeguard) is lowest-churn but editor-dependent. Lowest *total* friction is open.

## Data
- [`wr-data/`](wr-data/) — **WR** (Workflow Research) log of confirmation/approval events from real
  sessions that are candidates for elimination by a new safe-by-design `tt` tool. The raw evidence behind
  the confirmation-fatigue thesis; feeds *which tool to build next*.
