# Inference-time learning — the frozen-weights boundary and externalized structure as its substitute

- **Question:** an LLM agent has a firm boundary between **pre-training** (weights are formed) and
  **inference** (weights are frozen). In the weight-update sense it therefore *cannot learn* while working.
  Yet the whole point of genscalator is to make the agent *get better over time*. So: **what is the actual
  substrate of an agent's learning during inference, given it cannot change its weights?** And how do we
  build tools that make that learning real, reliable, and cumulative?
- **Why it matters (BR):** this is **an important thesis** of the project (not necessarily *the* central one —
  see the BR note below). It unifies the memory system, the instruction-adherence-decay finding, the compact
  dance, the roundtrip process, and the structural-fix rule under one frame: *genscalator is a system for
  inference-time learning by externalizing hard-won experience into persistent structure that changes behavior
  without retraining.*

> **BR note (2026-06-30 — paper focus still undecided).** Inference-time learning is **one candidate framing,
> maybe not THE thesis.** The project's original, more down-to-earth main thesis was simpler: *human-agent
> productivity can be increased through static (typed, statically-checked) tools built for agents.* It has
> since grown into several theses (inference-time learning, the substrate hierarchy, confirmation-fatigue /
> joint-zone, instruction-adherence-decay). BR still needs to think about the focus for the **first** paper.
> Keep this note as a strong candidate frame, but read it as *elaborating* the down-to-earth backbone —
> **human-agent joint productivity via static tooling** — rather than displacing it. Revisit when we start
> writing the paper.
- **Status:** open, foundational (new 2026-06-30, from BR's "agents learn during inference" deep thought).
  Agent co-theorizing its own learning — read with the METHODOLOGY §5 confabulation caveat.

## 1. The boundary, stated plainly
- **Pre-training / fine-tuning:** weights change. This is where the model's priors, skills, and reflexes live
  (including the bash/`cat`/`cd` reflexes that keep winning — see `008-instruction-adherence-decay.md`).
- **Inference:** weights are **frozen**. The model maps context → next token with a fixed function. Nothing
  it "experiences" in a session writes back to the weights. The session's only volatile store is the
  **context window**, which is itself lost/clipped at the **compact dance**.
- **Consequence:** any learning that happens *during use* must be **external to the weights**. There is no
  other place for it to live. This is not a limitation to lament; it is the **design space genscalator
  operates in**.

## 2. The substrate hierarchy (by reliability) — the key structure
If learning at inference must be external, *where* it is externalized determines how reliably it changes
behavior. Ranked weakest → strongest, with the session's own evidence:

1. **In-context instruction (weakest).** A rule stated in the prompt/chat. Survives only until compaction;
   competes per-token against the trained prior and usually **loses on "small" reflexive acts**. The session
   proved this at maximum strength: META-2 showed a self-authored, seconds-old rule failing to stop a raw
   `cat`. *"Salience is not the variable."* In-context instruction is **working memory, not learning.**
2. **Persistent memory (medium).** A memory file reloaded each session (the `memory/` dir). This **survives
   compaction and sessions** — genuine long-term storage, the agent's surrogate for synaptic plasticity. But
   it has a **retrieval-and-application gap**: storage works; *recall at the decision point* does not. The
   git-`-C` rule was in memory and the agent **still** regressed to `cd && git` (META-4), because the reflex
   fires *before* the memory is consulted. Memory learns *that* something is true; it does not reliably make
   the agent *act on it in the half-second the reflex fires.*
3. **Environmental / structural change (strongest).** The behavior is enforced by the **world**, not by the
   agent remembering: a typed tool that replaces the bash bundle; an allowlist; a **submit-time hook** that
   intercepts `cd X && git` and rewrites it to `git -C X`; instrumentation-by-default so a tool self-reports
   and is never shell-wrapped. Here the agent **need not recall anything** — the structure makes the wrong
   move impossible or the right move frictionless. This is learning externalized so completely it no longer
   depends on the agent at all.

**The thesis in one line:** *real inference-time learning moves insight DOWN this hierarchy — from fragile
in-context instruction, to durable-but-unreliable memory, to reliable environmental structure.* genscalator
is the machine that performs that migration.

## 3. This explains the META findings (not as failures but as evidence)
The repeated regressions (pipe-to-grep, raw-`cat`, `cd`+redirect, `cd`+git) are not the agent being dim; they
are **the predicted behavior of a frozen-weight system whose only correction channel is external**. Telling it
("be careful") edits substrate #1, which the prior overrides. Saving a memory (#2) helps across sessions but
loses the *moment*. Only #3 — the structural intercept — closes it. So every "why do I regress" finding is
really a datum about **which substrate a given fix lives in**, and the cure is always "move it lower in the
hierarchy." The session is, in effect, a controlled demonstration that #1 < #2 < #3.

## 4. Memory as surrogate plasticity — and its honest gap
The `memory/` dir is the closest thing to weight-plasticity available: write once, recalled later, shapes
future behavior. But three failure modes distinguish it from real plasticity:
- **Retrieval gap:** a memory must be *surfaced at the relevant moment*. Background recall (a memory shown in
  a system-reminder) is not the same as the agent *consulting* it before acting. Candidate fix: **just-in-time
  memory** — a hook that injects the relevant memory exactly when the triggering action is attempted (pushing
  memory toward substrate #3).
- **Application gap:** even surfaced, a memory is an in-context token stream → inherits #1's weakness against
  the reflex. (META-4: the rule was effectively present and still lost.)
- **Staleness:** memories are point-in-time; the world (files, flags) drifts. Real plasticity is continuously
  reconciled; a memory file is not, so it needs verification-before-trust (already a memory-system rule).

## 5. The human parallel (BR's framing, made precise)
Humans learn via **two** channels: neuroplasticity (weights change) **and** external scaffolds (notes, tools,
habits, designed environments). An inference-time agent has **only the second**. So the right mental model of
an agent "learning on the job" is a person with **anterograde amnesia who functions through a notebook and a
carefully arranged environment** (cf. *Memento*): perfectly capable in the moment, no new long-term neural
memory, so all durable competence must live in the notebook (memory/) and the environment (tools/hooks). This
is not a metaphor for poetry's sake — it makes a concrete prediction: **invest in the environment, not in
exhortation**, because the environment is the only channel that persists. And it reframes the agent's earlier
note (`011-human-state-and-joint-zone.md` §5) on *reflective self-reminding as method*: self-reminding is the
agent **deliberately re-reading its own notebook** — useful, but still substrate #1/#2; it complements, never
replaces, the environmental fix.

## 6. The roundtrip IS the learning loop
`METHODOLOGY.md` §2's double loop is, in this frame, **the inference-time learning algorithm**:
experience (inner loop) → reflection/coding (WR data) → **externalization into structure** (a tool/hook/memory
= moving the lesson down the hierarchy) → deployment → changed behavior → new experience. Each turn of the
loop is one "learning step." The accretion of the genscalator toolbox across sessions is then literally the
**cumulative learning** the frozen weights cannot accumulate — the project's memory of everything it has
learned, stored where it actually changes behavior.

## 7. Measurement (how we'd show learning happened)
Because the substrate is external and behavioral, learning is **measurable without touching the weights**:
- **Regression-rate over time** for a given reflex (e.g. `cd`+git): count occurrences per session before the
  memory fix, after the memory fix, and after a structural hook. If #3 drops it to ~0 while #2 only dented it,
  that quantifies the hierarchy. The `wr-data/` ledger + `RAW-DATA.md` + a `tt session-metrics` miner are the
  measurement substrate (METHODOLOGY §4).
- **Tool-choice ratio drift** (typed-tool vs shell-reflex) across sessions as the toolbox accretes — does the
  environment getting richer pull behavior toward the typed tools? That is cumulative inference-time learning
  made visible.

## 8. Open questions / further research (BR: "further researched")
- **Closing the retrieval gap:** can memory be made just-in-time (surfaced at the decision point), pushing
  substrate #2 toward #3 without a bespoke hook per rule?
- **Does it compound?** Does the agent measurably improve across many sessions as structure accretes, or does
  each new context start near baseline + whatever the environment enforces? (Hypothesis: baseline competence
  is fixed by the weights; *marginal* competence is whatever the environment now enforces — so improvement is
  real but lives in the environment, not the agent.)
- **A taxonomy of externalization substrates** and their reliability/cost (instruction, memory, tool,
  allowlist, hook, native binary) — a design guide for "where should this lesson live?"
- **Transfer:** structure learned on the AT case (e.g. `tt run`, submit-time hook) deploying to *other* cases
  = the agent "learning" something general, stored in a portable tool. How much transfers?
- **The honest limit (METHODOLOGY §5):** the agent theorizing its own learning is itself substrate #1 output
  and may confabulate; these claims are adjudicated by the **behavioral** regression-rate data, not by the
  agent's introspective say-so.

## What shipped
- Nothing yet (foundational note). It reframes existing artifacts as one system: `008-instruction-adherence-decay.md`
  (why #1 fails), the `memory/` dir (#2), the `tt` tools + proposed submit-time hook + instrumentation-by-default
  (#3), `METHODOLOGY.md` §2 (the loop) + §4 (measurement) + §5 (self-reminding, confabulation caveat). Candidate
  graduation: a `docs/foundations.md` glossary entry **Inference-time learning** + **Substrate hierarchy**, and
  prioritizing the **submit-time hook** as the highest-leverage learning mechanism (it operates at substrate #3).
