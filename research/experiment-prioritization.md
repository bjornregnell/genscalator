# Which experiment next? — a prioritization rationale

As joint reasoning unlocks more runnable experiments than we can run, we need a rule for **what to run next.** This
is that rule.

## The north star (BR, 2026-07-03)
> **Run the experiment most likely to build valuable *new* knowledge in relation to the research front of planet
> Earth.**

The load-bearing words are **new** and **in relation to the research front**: the value is **marginal** — knowledge
the world *doesn't already have and isn't about to get anyway*. An experiment that re-derives what a better-resourced
lab will publish next month has ~zero marginal frontier value even if it's locally fun. So the target is **expected
marginal contribution to the global frontier**, not "is this interesting to genscalator."

## Decomposition (so the north star is usable)
It factors into the classic research-prioritization triad — **Importance × Neglectedness × Tractability** — weighted
by our **Differential advantage**, divided by **Cost**, times **Optionality**:

1. **Importance (value-if-true).** How much would a solid answer matter — change a design rule, a theory, a safety
   posture? (Style-cost → AI-era style policy; framing-as-arousal → prompt/harness design *and* safety.)
2. **Neglectedness (= "the research front" test).** Is the question genuinely **open** at the world frontier, or
   already answered / crowded with well-funded groups? This is BR's criterion sharpened: *marginal* value is high
   only where the world isn't already looking. **Duplicating a crowded benchmark = low marginal value for us**, even
   if the local result is clean.
3. **Tractability (P(credible result) under OUR constraints).** Given a 6 GB box, small-n honesty, and an automatic
   grader — how likely are we to produce a **trustworthy** answer (including a *trustworthy null*)? An
   under-powered or confounded design contributes little regardless of importance. (This is where the honest
   power-calc feeds straight in.)
4. **Differential advantage (the multiplier that's really ours).** Do *we* have a vantage few others have? Our
   sharpest edge is the **human+agent reflexive setup** — an agent introspecting *with* a human, in a young field.
   So questions about **agentic collaboration, agent introspection, joint human-agent state, agent affective
   analogs** score high here: high importance × high neglectedness × a vantage almost no one else is positioned for.
   Conversely "which LLM codes best" is crowded → low differential value for us even if locally tempting.
5. **Cost (the denominator).** Knowledge **per unit of scarce resource** — human attention first, then box-time and
   tokens. **Harness reuse slashes cost:** the framing-as-arousal run reuses the indent-vs-braces harness almost
   whole → very high knowledge-per-cost. Box-light + overnight-autonomous + a null-is-informative design → cheap.
6. **Optionality (the multiplier for the future).** Does running it **unlock** further experiments — build reusable
   machinery, open new branches — or dead-end? BR's "we'll step-by-step unlock quite a few" *is* optionality; value
   the branching factor, not just the immediate result.

**The lens (not a literal formula):**
> priority ≈ (Importance × Neglectedness × Tractability × Differential-advantage × Optionality) ÷ Cost

Use it to *rank and argue*, not to compute a false-precision number.

## Honest caveats
- **Assessing the frontier is itself uncertain.** We can misjudge what's already known. **Mitigation:** a quick
  front-scan before committing a run (deep-research / web search: "is this open?") + humility. Don't stake a
  novelty claim we haven't checked.
- **Two value axes — keep them separate and label which is driving a run.** *Frontier value* (new to the world) is
  the north star. *Internal value* (improves genscalator's own design/tooling) is a legitimate but **different**
  reason to run something; when we run for internal value, say so, don't dress it as frontier novelty.
- **Readiness / sequencing ≠ priority.** A slightly lower-priority experiment that is *already preregistered and
  ready* may run **first** if it also **unlocks** the higher one (shared harness). Priority ranks *worth*;
  sequencing also weighs *readiness × unlocking*.

## Live backlog, ranked by the lens (2026-07-03)
1. **Style big-run — Tier A** (`BIG-RUN-PREREG.md`). Importance: mid–high (style policy for AI-edited code).
   Neglectedness: decent (edit-*cost*-by-style for agents is not a crowded benchmark). Tractability: honest (at the
   ~55-model power edge; null is informative). Differential: mid. Cost: low (overnight, box-light). **Optionality:
   high — it builds/【proves the harness that the framing experiment reuses.** → **Run first: highest readiness ×
   unlocking**, even though its pure novelty is below #2.
2. **Framing-as-arousal / Yerkes-Dodson** (`agent-affective-analogs.md` §5). Importance: high (prompt/harness design
   + safety). **Neglectedness: high** (agent affective analogs is a young, thin area). **Differential advantage:
   high** (needs exactly our human+agent reflexive setup). Cost: low (**reuses the Tier-A harness**). → **Likely the
   highest *marginal-frontier* value in the backlog**; run right after Tier A, whose machinery it inherits.
3. **Capability gradient — Tier B** (mid/large models). Importance: mid–high (bridges small→frontier). Tractability:
   gated on bigger hardware (autumn). → Deferred by *tractability/cost*, not worth.
4. **Unlocked-but-unscoped:** more edit-kinds; cross-language; human-state-detection studies; the affective-analog
   *mapping* program (§4b). Park here; re-rank as they sharpen.

## Process
Before committing any run: (1) a quick **frontier scan** (is it open?); (2) score it on the six factors above vs the
current #1; (3) if it wins, **preregister** (the honesty gate) and slot it; (4) prefer the one that also **unlocks**
the most. Re-rank whenever a new experiment is unlocked — the backlog is living.

Pairs with `agent-affective-analogs.md`, `human-state-and-joint-zone.md`, and each experiment's prereg.
