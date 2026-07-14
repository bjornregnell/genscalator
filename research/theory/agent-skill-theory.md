# Toward an agent-skill theory — a question framework (DRAFT seed)

**Status:** agent-authored DRAFT, 2026-07-14, for BR to develop. This poses the *questions* a theory should
answer; it does not yet answer them. Written in the light of recent events (the cold-start dormant-skill
regressions of 2026-07-13). Model producing this draft: Opus 4.8 (1M) — skill behaviour is model-dependent, so
the theory itself must be model-attributable (see §6). Ties SM069 (skill-set theory + audit), SM070 (detect),
SM077 (warm/load).

## 0. Why a theory (scope)

A "skill" (a `SKILL.md` the harness may inject) is one of several channels that shape how an agent acts: it sits
alongside **memory files**, the **resume-prompt / summary**, **hooks**, the **base-model prior**, and the live
**conversation**. Recent events show these channels interact in non-obvious, sometimes failing ways. A theory
should let us *predict* when a skill will actually change behaviour, *measure* whether it did, and *design* a
skill SET that is effective and efficient. Without it we tune skills by folklore and discover outages only by
watching the agent regress.

## 1. The grounding events (the specimens this must explain)

Concrete, dated observations a theory has to account for:

1. **Lazy firing → cold-start regression.** On 2026-07-13, with `avoid-guard-stall` *installed and active*, the
   agent still emitted brittle bash (`find … 2>/dev/null`, `ls … | head`) in the FIRST bash calls of a fresh
   session. An active skill is dormant until its trigger fires; cold-start moves happen in that gap.
   (`../wr-data/active-skill-still-cold-starts-dormant-reflexes-regress-2026-07-13.md`)
2. **Silent inactivity.** An entire earlier session ran with ALL genscalator skills OFF (plugin not installed);
   no error fired. (`../wr-data/guardrail-skills-silently-inactive-all-session-2026-07-13.md`)
3. **No phenomenology of absence.** The agent cannot *feel* a missing or dormant skill; an inactive skill is
   indistinguishable from the inside from an active one it simply failed to apply. The only external signal is
   behavioural regression. (`../wr-data/agent-has-no-phenomenology-of-absence-2026-07-13.md`)
4. **Capability rides on substrate access, not harness activation.** When skills were off, reading the SKILL.md
   files from disk re-armed the reflexes manually.
   (`../wr-data/capability-rides-on-substrate-access-not-harness-activation-2026-07-13.md`)
5. **Compaction regresses fine-grained reflexes to base-model defaults** unless re-surfaced.
   (`../wr-data/compaction-regresses-fine-grained-reflexes-2026-07-13.md`)

The unifying reading: **a skill's guidance is a learned OVERRIDE of the base-model prior; the override only wins
while it is in salient context. Remove it from salient context and the prior reasserts.** Activation is not
salience.

## 2. The core distinctions the theory must name

- **Active vs in-context (salient).** A skill can be *enabled* yet not *loaded into the working window*. Three
  tiers of decreasing cold-start reliability: (1) resume-prompt/summary = in context; (2) active skill = dormant
  until triggered; (3) memory file = passive recall. The theory needs vocabulary for these.
- **Detect vs load.** SM070's `tt skillcheck` answers *"is the skill active?"*; SM077's warm-cue answers *"are
  its reflexes in my context NOW?"* A dormant-but-active skill passes the first and fails the second.
- **Override vs prior.** A skill's value is measured against what the base model would have done WITHOUT it. A
  skill that merely restates the prior has ~zero override value; the ones that matter are those that flip a
  default (tt over bash, braces-on-long-scope, no-Claude-credit).

## 3. The questions a theory should answer

### A. Ontology — what IS a skill, relative to its neighbours?
- What can a skill do that a memory file / resume-prompt line / hook cannot, and vice versa? What is each
  channel's comparative advantage (persistence, salience-at-cold-start, determinism, cost)?
- When should a given piece of guidance live as a skill vs a doc vs a memory vs a hook vs a tool? (The
  SM067/SM068 "doc not skill" decision is a live instance — what's the rule?)

### B. Activation, salience, and firing
- What exactly triggers a skill to load, and how precise is that trigger? What is the rate of **over-fire**
  (loads when irrelevant, wasting context) and **under-fire** (fails to load when relevant, the cold-start
  case)?
- Is firing purely reactive (keyword/trigger) or can it be proactive (session-start)? Should guardrail-class
  skills fire proactively while task-class skills stay lazy?
- What is the "summoning gap" — the window between session/turn start and first-trigger — and how many
  regressions occur inside it?

### C. Context economy (the reason skills are lazy)
*Developed formally in "The rate–distortion view of skill economy" below — this is the section's centre of gravity.*
- Skills are lazy BECAUSE eagerly loading all of them would blow the context budget. So: what is the marginal
  context cost of a skill (its listing entry + its full text on load), and what is its marginal override value?
- Is there an optimal number/size of skills for a given window budget? A "load the tiny guardrail core, keep the
  rest lazy" policy implies a **value-per-token** ranking — how do we compute it?
- What is the cheapest representation that still flips the default — full SKILL.md, a ~15-line digest, a memory
  line, a hook injection? (Digest-in-a-hook may dominate full-skill-load for guardrails.)

### D. Set-level quality (SM069 proper)
- What makes a skill SET effective (coverage without gaps, right skill at the right time) vs efficient (minimal
  overlap, low context tax, sharp firing)? Can these be measured, not just asserted?
- Consolidate vs split: when does merging two skills improve firing precision, and when does it blur triggers?
- How do we detect coverage gaps (a needed reflex no skill encodes) and redundancy (two skills fighting)?

### E. Observability and verification
- Given no phenomenology of absence, how does the agent (or the human) KNOW a skill is active AND salient AND
  working? What is the minimal instrument? (`skillcheck` is a first cut for *active*; what measures *salient*
  and *working*?)
- Behavioural regression is the only current signal, and the agent can't reliably self-certify it. What
  external/objective measures work — guard-trip counts, tool-choice ratios, a probe suite?

### F. Robustness across the warp (compaction / model change)
- Which channels survive a compaction, and which regress to the base prior? (Empirically: summary + resume-
  prompt survive; memory-only reflexes regress.)
- Skills are model-dependent. Does a skill authored/validated under model X still fire and still override under
  model Y? What is the transfer story across a model warp?

### G. Lifecycle
- Authoring, testing, decay: how is a skill validated (does it actually flip the default in practice, not just
  in theory)? How do we notice a skill that has gone stale or is silently never firing?

## The rate–distortion view of skill economy (developed 2026-07-14)

**Framing — a lens, not a proven isomorphism (flagged as analogy).** Treat a skill as *a lossy code for a
behaviour policy, decoded by a specific model*. Then "how do we size the skill set?" becomes a rate–distortion
problem: **minimise the context tokens spent (rate) subject to keeping the agent's behavioural deviation
(distortion) acceptable** — or dually, minimise distortion under a fixed context budget. The mapping:

| Rate–distortion term | Skill-world meaning |
|---|---|
| Source | the target behaviour policy π\* — the default-flips we want (tt-over-bash, braces-on-long-scope, no-Claude-credit, …) |
| Code / representation | the guidance artifact: full `SKILL.md`, a digest, a memory line, a hook injection |
| Rate | context tokens the code occupies (listing-entry cost + text cost when loaded) |
| Decoder | the model, reading code + prior + context and emitting behaviour (lossy, stochastic, model-specific) |
| Distortion | behavioural deviation from π\* — guard trips / wrong-tool / missed default-flips per opportunity |
| Channel | the finite, shared context window |

The design objective becomes explicit and measurable: for each behaviour, pick the representation on the **knee**
of its rate–distortion curve.

### Four ways agent-skills are NON-classical rate–distortion (the interesting part)

1. **The prior is free side-information — code only the residual.** The base model already follows much of π\*.
   Guidance need only encode the *residual* between the prior's default and π\* (an override). A skill that
   restates a default the model already follows spends rate to encode ~zero information. So the effective source
   is **π\* minus the prior** — the override residual. Rate should track the *override-distance* δ (how often the
   base model errs without the skill), estimable by **ablation** (run without the skill, count regressions). This
   is the formal version of P2.
2. **Rate is scheduled, not fixed — it is expected rate under a firing distribution.** A lazy skill costs its
   full text only when it fires: expected rate = listing_cost + P(fire) × load_cost. Laziness is a rate-reduction
   move — it makes the big cost *conditional*. But under-firing raises distortion (the cold-start summoning gap).
   So *when* you pay rate (proactive at turn zero vs lazy on trigger) is a second, orthogonal knob trading rate
   against distortion via P(fire). This is the formal home for "activation ≠ salience".
3. **Distortion is non-stationary — position- and history-dependent.** The same code yields different distortion
   by where it sits: dormant at turn zero (high), freshly loaded (low), deep in a long or compacted context
   (rising again — reflex regression / rot). Classic rate–distortion assumes a fixed decoder; here the decoder's
   fidelity depends on salience and recency. This is where "cold-start regression" and "compaction regresses
   reflexes" live, and it motivates **re-injection** (re-hydration) as active distortion control.
4. **Codes share a finite channel — rates are coupled, not additive-free.** Because the window is finite and
   shared, spending rate on skill A can push skill B out of salience, *raising B's distortion*. The aggregate is
   NOT a sum of independent codes: **one skill's rate is another skill's distortion.** This is the formal
   statement of "a bloated skill set taxes the window", and it is why the aggregate curve has a knee (P3) — past
   it, added active skills buy ~no distortion reduction and can *increase* total distortion by crowding.

### The lever taxonomy (what the lens licenses you to do, per behaviour)

- (i) **Shrink the source** — encode only the override residual; never spend rate restating the prior.
- (ii) **Compress the code** — a digest instead of full text; accept a little distortion for much less rate.
- (iii) **Schedule the rate** — proactive turn-zero injection for guardrails (kill the summoning-gap distortion)
  vs lazy firing for situational task skills (low expected rate).
- (iv) **Refresh against decay** — re-inject to fight position-dependent distortion (post-compaction re-hydration).
- (v) **Constrain instead of code** — a hook/guard that makes the wrong behaviour *impossible* clamps distortion
  to ~0 by construction. This is not a point on the curve at all: it *modifies the channel* rather than informing
  the decoder. **Inform vs constrain** is the deepest distinction the lens surfaces, and it is exactly why
  "structure over willpower" wins — a `guardcheck` hook takes a behaviour OFF the rate–distortion curve, paying
  in occasional false-positive denials rather than in context tokens.

### The quantities it defines (all buildable from transcripts + `tt guardcheck` + ablation)

- **override-distance** δ(behaviour) = base-model error rate WITHOUT the skill (measured by ablation);
- **code rate** r(artifact) = listing + expected-load tokens;
- **behavioural distortion** D = error rate per opportunity WITH the code, bucketed by context position;
- **value-per-token = Δδ ⁄ r** — the ranking function §C asked for, now defined: rank behaviours × representations
  by distortion-reduction per token, fund the top of the list, leave the tail lazy or unencoded.

### The empirical object

For ONE behaviour, sweep representations {nothing → memory line → digest → full skill → hook} and plot achieved
distortion vs rate → its rate–distortion curve; the **knee** is the recommended representation. For the whole
SET, sweep active-set size and plot aggregate distortion vs aggregate rate → the knee is the recommended set size
(P3), and the crowding coupling (wrinkle 4) predicts the up-tick past it.

### Honest caveats (where the analogy strains)

The decoder is stochastic and non-stationary, so there is no clean fixed R(D); the distortion measure is
behavioural and only partly formalisable (which deviations count, and how weighted?); δ and D are model-specific,
so every curve is indexed by model. The lens earns its keep by making the *questions and quantities* crisp and
measurable — not as a theorem to invoke.

## 4. Candidate propositions (sharp, falsifiable — to test, not assert)

- **P1 (salience beats activation).** Injecting a guardrail digest at turn zero eliminates more cold-start
  regressions than merely having the skill active. *Test:* A/B the SessionStart digest vs plain active-skill,
  count guard trips in the first N tool calls.
- **P2 (override value is what counts).** A skill's behavioural effect is proportional to how far its guidance
  departs from the base prior, not to its length or detail. *Test:* correlate override-distance with measured
  behaviour change.
- **P3 (economy has a knee).** Beyond some number/size of active skills, added coverage costs more context than
  the override value it returns. *Test:* vary skill-set size, measure task quality vs context tax.
- **P4 (guardrails want proactive firing, tasks want lazy).** Splitting skills by firing mode (proactive
  guardrail core vs reactive task skills) beats a uniform policy. *Test:* compare regression + context cost
  across the two regimes.
- **P5 (constrain beats inform where the behaviour is clampable).** For a behaviour that can be structurally
  blocked (brittle bash, Claude-credit), a hook/guard achieves lower session distortion at lower context rate
  than ANY informing code (skill/digest/memory). *Test:* compare guard-trip / leak rate AND context cost, hook
  vs skill-only, for a clampable behaviour. (The rate–distortion statement of "structure over willpower".)
- **P6 (rate crowding — codes share the channel).** Adding active skills past the aggregate knee raises the
  distortion of SOME behaviours by crowding them out of salience, even though each added skill lowers its own.
  *Test:* track per-behaviour distortion as the active set grows; look for the up-tick predicted by wrinkle 4.

## 5. How to study this (method)

This is empirically testable with the tools we already have: guard-trip counts (`tt guardcheck`), tool-choice
ratios and cold-start probes from session transcripts (`tt wr`), the in-session-experiment harness (real work as
the vehicle, the agent as both operator and subject — so measure objectively via commits/tests/transcript, not
the agent's self-report). See `skills/research-methods` for choosing experiment vs case-study, and
`skills/in-session-experiment` for running a controlled probe live. Each proposition in §4 is a candidate study.

## 6. Housekeeping

- **Model attribution required** — every observation feeding this theory must be tagged with the frontier model
  that produced it (behaviour is model-dependent). This draft: Opus 4.8 (1M).
- **Ties:** SM069 (this doc is its natural home / seed), SM070 (`tt skillcheck` = the *detect* instrument),
  SM077 (the *warm/load* lever), the compaction "reflex re-hydration" candidate, the WR specimens in §1,
  `skills/research-methods`, `skills/in-session-experiment`.
- **Open:** is the deliverable ultimately a theory NOTE (blog 006 "agent-psyche" material?) or a preregistered
  STUDY? Decide once the questions above are prioritised.
