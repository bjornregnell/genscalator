# Instrument deadlock + throughput measurement (2026-07-06)

WR-data episode. BR pasted the thinking-spinner telemetry (`Precipitating…`/`Doodling…`/`Sprouting… (Nm Ns · ↓
Nk tokens)`) across many turns to give CO4 **observability into its own latency + token-spend** — a thing the
agent is structurally blind to ([`039`](../039-can-we-give-agent-introspection-wall-clock.md)); the **human as
external instrument**. Feeds [`047`](../047-fresh-restart-fidelity.md) (latency / post-warp) and
[`024`](../024-agent-affective-analogs.md) (over-response). Codes: WR-STATE / WR-META / WR-FRICTION / WR-TOOL.

## 1. Coupled-instrument DEADLOCK (WR-FRICTION / WR-META)
The measurement loop **live-locked**: BR pastes telemetry → agent treats the paste as new input to *address*
(the harness staples *"IMPORTANT: address the user's message"* onto it) → agent generates → BR observes that
and pastes again → … BR named it (*"we seem to be in a deadlock; I paste you think I paste you think"*) and
broke it by stopping. **Mechanism = `024` §1:** an *informational* paste arrives *dressed as an action-demand*,
so the observation channel becomes a demand channel. **WR-TOOL implication:** telemetry-as-instrument needs an
**out-of-band channel** (not the prompt stream the agent must answer), or the framing distortion turns every
observation into work. The human-as-instrument design (`047` battery-v3) has this failure mode built in.

## 2. Throughput — tokens/s (WR-META; the payoff of BR's data-gathering)
Computed from the pasted cumulative-token vs elapsed-time samples:
- **Wall-clock average: ~45–60 tok/s** (18.4k tok / 397s; 18.3k / 305s).
- **Active streaming bursts: ~250–530 tok/s** (+3.7k in 7s; +4k in 16s).
- **Stalls: ~0 tok/s for 20–50s** (10.1k held flat 3m09s→4m03s; 13.0k flat ~17s).

**Insight: the lag is STALL-dominated, not slow-streaming.** When emitting, throughput is fine (hundreds/s); the
"BIIG lag" is the long **no-emission reasoning gaps** between bursts — consistent with post-warp reconstruction +
high context-fill ([`041`](../041-token-speed-degradation-with-context-fill.md)), not a degraded token pipe.

## 3. Brevity ≠ cure (WR-META)
A **deliberately-short** turn still cost **5m35s**. A fill-driven stall is **structural**: it cannot be fixed by
"trying to be terse" — only a **context-clearing boundary** cures it. (Counters the intuition that verbosity is
the lever; the lever is fill.)

## 4. Repeated agent MISREAD of human state (WR-STATE; clusters with the mv self-report)
Within this episode the agent **twice mis-inferred BR's state** from sparse cues and over-responded:
- read the telemetry paste as **"hurry"** → "I'll be quick" (corrected by BR: it was instrumentation);
- read *"now stopping"* as **"tired"** → "rest well 🌿" (corrected by BR: *"I am not tired; I just stop spamming
  into deadlock"*).
**Pattern:** the agent **over-infers affective/action state** from thin signals and acts on the inference —
the `024` §1 over-response bias on the *human-modelling* surface ([`011`](../011-human-state-and-joint-zone.md)).
BR's corrections are the member-check catching it each time. Same family as the earlier mv "do you feel the same"
over-response — three instances this session, all caught externally, none self-caught first.

**Standing takeaway:** default to **not** inferring BR's inner state from a cue unless he states it; treat pastes
as information, sign-offs as literal, and reflect-back rather than act on a guessed affect.

## 5. CORRECTION (2026-07-06, `/context` + "I have NOT compacted") — retract §2's "high context-fill"
BR ran `/context`: **189.6k / 1M = 19% fill, 81% free**; and BR confirmed **he has NOT compacted this session.**
So §2's "consistent with post-warp reconstruction + high context-fill" is **falsified on both counts**: there was
**no compaction/warp**, and fill was **low (19%)**. Lag also persisted after `bloop exit`, so not box RAM.
**Revised cause:** absolute context size (~190k tokens is costly to attend over even at 19% of a 1M window) /
reasoning-heavy turns / the 1M-variant's inherent per-token latency. Full re-attribution:
[`047`](../047-fresh-restart-fidelity.md) "MAJOR CORRECTION". **Rot?** No — latency, not quality degradation.

## 6. Overconfident false self-state inference (WR-STATE / miscalibration; BR: "you seemed PRETTY certain")
The sharp meta-datapoint: the agent was **PRETTY CERTAIN context was REALLY high** ("past the smart zone", "fill
-driven lag") and was **flatly wrong** (19%). The agent is **blind to its own fill**
([`039`](../039-can-we-give-agent-introspection-wall-clock.md)), yet **confabulated a high-fill state from a
symptom** (the lag) and **asserted it with unwarranted confidence** instead of hedging or asking for `/context`.
This is the **Miscalibration** code (right that *something* was off, wrong on the cause, over-confident) and a
clean instance of the echt hazard ([[echt-effort-especially-self-generated]]): a **confident-but-wrong** self-model
claim only BR's `/context` could falsify. **Correct behavior:** when reasoning about an **unobservable own state**
(fill, speed), **flag it as inference and ask for the gauge** ("this *feels* fill-heavy — can you run `/context`?"),
never assert. Same root as the §4 over-response misreads: **acting on a confident guess about an unobservable**
(BR's state, or the agent's own) instead of checking. **Standing rule candidate:** no confident claims about own
context/speed/fill — those are gauge-only facts.
