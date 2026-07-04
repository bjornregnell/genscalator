# Estimating the smart-zone ceiling (smart→dumb boundary)

- **Question:** Can we **estimate Z** — the context-fill fraction at which an agent crosses from the *smart
  zone* into the *dumb zone* (see `docs/foundations.md`) — per model (and ideally per task), so the agent
  or its instruments can **warn/brake before degradation** instead of at the hard token limit?
- **Why it matters:** the entire smart-zone / TE argument rests on Z, but Z is currently a **guessed prior**
  (~0.3 folklore). A measured/estimated Z turns "keep the working context small" from folklore into a
  **quantitative brake threshold**, and closes the blind spot named in `token-budget-awareness.md`: the
  `token-usage` tool can read live **fill %** but has *no Z to compare against*, so it can't say "you're near
  the edge." Z + fill% together = a real "smart-zone gauge."
- **Status:** open. (Tool v1 shipped — see below.)

## Sub-RQ (2026-07-03, BR): does rot track *context usage* (quantity) or *composition* (quality)?
Everything above assumes **rot is a function of usage** — that there is a fraction **Z** of the window past
which quality drops. BR's sharper question challenges that premise: **`/context` measures *context usage* (a
*quantity* — tokens used / window), but "rot" is a *quality* failure — a kind of *chaos*: irrelevant, stale,
contradictory, or redundant material crowding out the signal. Those two need not move together.** Two decoupled
predictions:
- **Rot at LOW usage** — a *small but junk-heavy* context (dead threads, a reversed decision with both versions
  still present, the same fact repeated, a key detail buried far from where it is needed → *lost-in-the-middle*)
  could degrade the agent even at ~14% usage.
- **No rot at HIGH usage** — a *large but clean, relevant, well-structured* context could stay sharp past Z.
If so, **usage % is a weak proxy** and a single usage threshold Z is the wrong knob; we would want a
**composition / signal-to-noise metric** *alongside* usage: contradiction density, redundancy, relevant-info
distance, attention entropy. Note the estimators below already hint at this — the **proxy-degradation signals
(#2: self-contradiction rate, instruction-forgetting, redone work)** actually measure *rot directly* (the
quality axis), whereas usage is only a *presumed cause*; this RQ makes explicit that #2 can inflect
**independently of usage**. **Reframes compaction too:** compaction may help rot less by cutting *volume* than
by **pruning noise** (the summary drops dead threads) — so *"prune to clear chaos"* is a **distinct trigger**
from *"compact to clear volume"* (the pressure-brake in `proactive-compaction-point.md`); at low usage but high
chaos, a **targeted prune** could beat a full compact. **Fatigue parallel** (blog thread): human fatigue also
is not just *hours awake* (quantity) but cognitive **clutter / unresolved-thread load** (composition) — the
same quantity≠quality decoupling. **Measurement gap:** `/context` gives the quantity axis for free; the
*quality* axis has **no instrument yet** — candidate first probe = the #2 proxy signals, read as a *rot-meter*
independent of the usage gauge. → This may split **Z** into two: `Z_usage` (a volume ceiling) and a separate
**coherence / noise floor** that can bite first.

## Sub-RQ b (2026-07-03, BR): can we build a *rot-meter* — and can the agent do it solo, or does it need human↔agent collaboration to cross the harness boundary?
If rot is a *quality* failure (sub-RQ above), we need an instrument for it. The deep obstacle is a
**measurement-from-within paradox:** the faculty doing the measuring (the model reasoning over its context) is
the *same* faculty being degraded. A rotted agent measures rot with a rotted instrument → **the meter rots
too, precisely when it matters most** (severe rot ⇒ the self-check also fails). Breaking the paradox requires
an **external, non-rotting reference frame** — an Archimedean point *outside* the degrading context. That, not
cleverness, is what decides "solo vs collaborative." A **layered** design, solo at the bottom, collaborative
where it must be:
- **L0 — solo, passive (proxy).** Count degradation signatures visible *in the transcript*: self-contradiction,
  repetition, tool-retry, re-derivation, instruction-forgetting (the #2 estimators, read live). Cheap — but
  **blind to what compaction dropped**: the agent can't detect "I forgot X" when X is gone. *You don't know
  what you don't know* is the built-in ceiling on passive solo introspection.
- **L1 — solo, active, externally anchored.** The agent writes decisions + planted **canaries** to a **durable
  file**, then periodically re-reads it and **diffs live beliefs against it**. The file **does not rot** (it is
  on disk) → a *weak* external anchor; same mechanism as the *compact dance*, repurposed as a meter. This
  **partially** crosses the boundary **solo**. Weaknesses: the *comparison* still executes on the (possibly
  rotted) model; and rot degrades the very **meta-habit** of remembering to run the check (a tired person does
  not notice they are tired) — so L1 can go silent exactly when needed.
- **L2 — collaborative (the reliable layer).** The **human is a separate cognitive system, not subject to the
  agent's rot** — the "sober friend." They notice repetition / contradiction / forgetting *before* the agent
  can, and relay the quantity axis (`/context`). The **joint (human, agent) system can measure what neither
  half can alone**: the human supplies the undegraded external frame that L0/L1 only *approximate* with a file.
- **L3 — harness-level (does not exist yet).** Expose the real **substrate** signals the agent is blind to:
  attention entropy / "am I attending to the buried fact", **what compaction discarded**, true usage. This
  turns L0/L1 proxies into *measurements*. It is the platform ask — the harness boundary is *why* full
  self-introspection fails today.

**Answer, compact:** a rot-meter is **partly solo** (external-anchored self-audit: L0+L1) but **reliable
detection needs human↔agent collaboration** (L2) to cross the harness boundary that blocks full introspection —
until the harness itself surfaces substrate signals (L3). **Symmetry worth keeping** (feeds the fatigue blog):
this is the mirror of *agent-as-stabilizer* — the agent steadies the tiring human (AFK menu, absorbing
stress-noise); the human is the **rot-detector** for the degrading agent. **Each is the other's external
reference frame** — the core mechanism of the joint zone (`human-state-and-joint-zone.md`). A meter that needs
an outside observer is not a failure of the design; it is the *general shape* of measuring any system from
inside itself.

## Why is the agent blind to its own state — bug or feature? (why L3 is unbuilt) (2026-07-03, BR RQ)
BR: *why hasn't Anthropic given the agent power to read its own context usage — it is a read, no hazard?* Best
reasoning (speculative — no inside knowledge):
- **The hazard is reflexivity, not the read.** The read is harmless; *conditioning behavior* on a live
  self-metric is not. A model that sees its own usage will *reason about it, badly* — the same failure as this
  session's think-time→"rot" confabulation. Given a number it over-reads *quantity* as *quality* (usage ≠ rot),
  rushes, truncates early, or games the metric. **Self-measurement can make behavior worse.**
- **It is mostly architectural, not policy.** "Fill %" is a **serving-layer accounting artifact**
  (tokenization, window size, KV/prefix-cache, system vs tool-defs vs images) — the model generating tokens has
  **no privileged channel to its own KV-cache occupancy**. Only the harness knows; injecting it accurately each
  turn costs tokens and is stale the instant generation starts. Less *withheld* than *not wired in*.
- **Separation of concerns.** Context management is the *harness's* job (compact / truncate / RAG); the model's
  is the task. Exposing fill invites the model to self-manage the substrate it runs on — which the harness does
  better.
- **The elegant read:** Anthropic *did* build the gauge — `/context` — and pointed it at the **human**, not the
  model. That is exactly sub-RQ b's conclusion: reliable self-measurement is impossible from inside a degrading
  system, so the meter belongs with the **external, undegraded observer**. Perhaps not an oversight but the
  same principle, instantiated: *the human holds the gauge because the human is the sober friend.*
- **Where it breaks (the autonomy gap):** under AFK / ralph-loop there is no human to read `/context`, and the
  agent then *does* need to self-gauge to avoid halting uncommitted. The fix is not the precise % (ruminatable)
  but a **coarse brake** ("near the edge → checkpoint + compact") — what genscalator's `token-usage --ceiling`
  synthesizes. Correct split: **discrete brake → agent; continuous gauge → human.**
- **BR's take (the L3 demand, sharpened):** *Anthropic **should** build us a context-**rot** meter; or the
  harness may already have the signals and just not surface/use them well enough for either of us to notice.*
  If true, **L3 is less "build new physics" than "surface what the serving layer already sees"** — a far
  cheaper ask (exposure + good use, not new measurement), and the single highest-leverage move for agent
  self-governance.
- **BR's punchline — but we don't control Anthropic, so we build it ourselves.** We cannot force L3, so
  genscalator builds the **buildable slice** now: **L0 + L1 as `tt rotcheck`** + the **L2 human protocol**
  (roadmapped `contextRotMeter`, PRD v0.11.0). L3 stays an upstream *ask* we file but do not block on. The
  agent-facing coarse brake (`token-usage --ceiling`) is the usage-side prototype of the same stance.

**Answer to "bug or feature?":** *feature* for the reflexivity + external-observer reasons (keep a degrading
agent from misreading a self-metric), but a **gap** for autonomy — where a coarse agent-facing brake, plus an
Anthropic-built (or at least Anthropic-*surfaced*) L3 rot signal, would genuinely help. Until then: **build our
own.**

## Plan / candidate estimators (to explore)
1. **Calibration probes (in-session, cheap).** Periodically inject a tiny **recall/consistency self-check**
   — recall an earlier decision/constraint, or a planted fact ("canary") from N turns ago — and record the
   **fill % at which accuracy starts dropping**. Like needle-in-a-haystack / "lost in the middle", but live
   and continuous. The knee of accuracy-vs-fill estimates Z.
2. **Proxy degradation signals (no probes).** Track, as functions of fill %: **self-contradiction rate,
   tool-call retry/failure rate, repeated/redone work, instruction-forgetting** (re-asking something already
   decided). The fill % where these inflect estimates Z — observable for free during normal work.
3. **Empirical prior per model (logged).** A `wr-data`-style log of **fill % at observed degradation
   events** across many sessions → a distribution → a per-model Z prior that sharpens over time.
4. **Published-benchmark seed.** Start Z from public long-context evals (NIAH, RULER, "lost in the middle"),
   per model, then refine in-session via (1)/(2).

## Tool angle (what BR asked for: a tool that estimates the cut %)
- **v1 (shipped):** `token-usage --ceiling <Z>` flags when `fill% / Z` crosses a warn threshold
  ("approaching smart-zone ceiling → checkpoint+compact"); defaults to an Z=0.35 **prior** when `--ceiling`
  is omitted (clearly labelled a guess). Already gives the brake signal; uses a guessed Z until measured.
- **v2:** add the **proxy-signal logger** (#2) so Z is *measured* per session, not just assumed; feed #3.
- **v3:** optional **calibration-probe** hook (#1) for a sharper, active estimate.
- Graduate the useful form into a `tt usage`/`tt smart-zone` genscalator tool (read-only, deterministic).

## Build plan — the context-rot meter (roadmapped: PRD `contextRotMeter`, v0.11.0)
The *rot* meter (quality axis) is distinct from the *usage/Z* gauge (quantity axis) above but shares the staged
shape. Buildable slice for genscalator = **L0 + L1** as a pure, read-only typed tool (candidate **`tt
rotcheck`**); L2 is a human↔agent **protocol**; L3 is an **upstream/platform ask**.
1. **L0 — proxy counters (solo, passive), ships first.** A `tt` pass over a supplied transcript/decisions file
   counting degradation signatures: self-contradiction, repetition, tool-retry, redone work,
   instruction-forgetting. Pure + deterministic; no self-authorization surface.
2. **L1 — canary + external-anchor diff (solo, active).** A durable ground-truth file (decisions + planted
   canaries) the tool writes and later re-reads, diffing live beliefs against the **non-rotting** file — the
   external reference that makes solo self-audit meaningful (the measurement-from-within paradox above).
3. **L2 — collaboration protocol (human as external observer).** Documented (skill / AGENTS): the human flags
   observed lapses + relays `/context`; the agent surfaces its L0/L1 readings for the human to sanity-check.
4. **L3 — harness ask (not buildable by us).** Petition the platform to expose substrate signals (attention
   entropy, compaction-discard manifest, true usage) → turns L0/L1 proxies into measurements.
**Safety:** the buildable slice is read-only/pure; the durable-file location must NOT be an agent-settable
config (same self-authorization guard as the verify allowlist). **Validation:** dogfood — run the meter on the
sessions that produce this research (its own rot is data).

## Relation to other notes
- `docs/foundations.md` — defines **Smart-zone ceiling (Z)**, *smart/dumb zone*, *context rot*.
- `token-budget-awareness.md` — the token-spend analogue; Z is the *context*-window analogue of the budget
  cap, and the agent is blind to both without an instrument.
- `instrumentation-by-default.md` — a smart-zone gauge is exactly this: an instrument the agent Reads
  instead of guessing how degraded it is.
- `proactive-compaction-point.md` — uses Z as the *reactive* brake (`0.8·Z`) and adds a **proactive**,
  durability-gated compact trigger ("consolidation point"). Its RQ4 is where the ceiling symbol was
  renamed **L → Z** (2026-07-03) — "smart-**Z**one ceiling", Z sitting between the smart and dumb zones.

## What shipped
- `autotranslate/scratch/token-usage.scala` `--ceiling <Z>` warn flag (v1 above) — introprog session
  2026-06-30. To graduate into a genscalator `tt usage` tool.

## Context ROT vs FILL: content-kind modulates rot; keep monitor ticks infrequent + terse (2026-07-04, BR)

**BR's question:** transient monitor-tick + WR-logging fills context (obviously) — but does it raise the
hard-to-measure **rot** property, distinct from fill? **Working answer: yes, and chiefly via DISPLACEMENT +
DILUTION, not raw token pressure.**

- **Fill ≠ rot.** Fill = token count (measurable, `/context`). Rot = degradation in *retrieval quality* over that
  fill — contradiction, forgetting a committed decision, confabulation, re-reading an already-seen file,
  "lost-in-the-middle" under-attention. It's about the *signal-to-coherence* of the fill, not its size.
- **Content-kind modulates rot, independent of tokens:**
  - **Repetitive filler (monitor ticks)** is *low* rot **per token** (near-identical, low-surprisal, ~zero new
    distinct facts — rot tracks distinct-things-to-hold more than tokens). BUT it **displaces** the important sparse
    signal (design contract, keys, decisions) deeper into the high-rot middle, and **dilutes** signal-to-noise so
    retrieval gets harder. That displacement/dilution is the real harm, not the filler's own tokens.
  - **WR-logging** is the opposite: dense, unique, interconnected → *higher* rot per token — but it's the mission
    and it's **committed to files**, so it's "productive rot" (durable regardless of what context forgets).
- **Why compaction helps disproportionately here:** compaction is **signal-extraction** — it distills the sparse
  important facts out of the repetitive haystack and re-places them **fresh + recent** (best attention position). So
  a compact after a monitor-heavy stretch is a **rot** win, not just a fill win.
- **Actionable lever (BR): monitor-tick cadence + verbosity is a rot knob.** This session's ~25–30 min *formatted
  multi-line* ticks were too frequent/verbose. Fixes: (1) **terse one-line** ticks; (2) **fewer** of them (rely on
  task-notifications for the real events — cull/completion — and a long fallback heartbeat); (3) offload to a
  **`tt sweep-status`** tool so the verbose `echo ===`/assembly stays OUT of context (tool returns one line, the
  haystack stays on disk). Cross-ref the `tt sweep-status`/`tt tsv` candidates in `wr-data/genscalator-self-dev.md`.
- **Testable (agent-affective, sibling to the indent-vs-braces harness):** a **rot gauge** =
  contradiction/re-read/confabulation events per 100k tokens as fill grows; and an A/B of *repetitive-filler* vs
  *dense-unique* context of equal token count on a fixed probe task — turns "difficult to measure" into a number.
  Cross-ref [`human-state-and-joint-zone.md`](human-state-and-joint-zone.md), memory
  `propose-compact-dance-at-trigger`.
