# Human state modeling and the joint human-agent working zone

- **Question:** genscalator already models the *agent's* state (smart zone / dumb zone, context rot, token
  velocity). Can we model the *human's* state with the same seriousness, and then model the **joint**
  human+agent state as the thing that actually determines whether a collaboration session goes well? And the
  payoff question: can the agent actively help keep the *human* in the smart zone, not just monitor its own?
- **Why it matters:** the scarce resource in this whole project is **human attention and judgement** (cf.
  `task-autonomy-negotiation.md`). Every genscalator goal (avoid confirmation fatigue, avoid review overload,
  stay in the smart zone) is ultimately about protecting a human who can get worn out. If we only instrument
  the agent, we optimise half the system and let the other half (a tired human rubber-stamping advanced work
  they cannot review) silently fail. Modeling the human state turns "don't burn the human out" from a vibe
  into something an instrument and a protocol can act on.
- **Status:** open (new, 2026-06-30). Captures BR's framing + agent introspection; candidate glossary terms
  flagged for ratification.

## 1. Human states and events worth naming

### Confirmation fatigue: state or event? (BR asked)
**Both, related as integral and impulse.** Each approval prompt is an **event** (an impulse). **Fatigue is a
state** — the accumulated level, the time-integral of those events minus recovery (rest, a good outcome, a
break). So: `CF_level(t) = sum of prompt-events - recovery`, with **hysteresis** (it builds faster than it
drains, and a depleted human recovers slowly). This distinction is not pedantic, it changes the fix:
- act on the **event rate** -> fewer/batched prompts (the core genscalator safe-by-design thesis), and
- act on the **state** -> allow recovery: checkpoint and stop *before* the level is high, don't push a second
  hard task onto an already-drained human.
The current glossary defines CF as a degradation; this note proposes sharpening it into an **event-driven
state variable** so tooling can reason about *level*, not just presence.

### Thriller state (new — BR's term)
The human's **high-engagement, high-arousal flow** state on the most interesting work: *"will we make it
together? is this too advanced for the agent? will the agent do advanced things I cannot even review?"* It is
genuinely fun and productive and is often where the best work happens. But it has two hazards:
- It is a **fatigue precursor**: sustained high arousal burns reserve and crashes into the dumb zone later
  (a Yerkes-Dodson curve — moderate arousal is optimal, too much degrades). The thrill masks the depletion
  until it is sudden.
- It can **erode review rigor**: excitement biases toward over-trust ("let it run, this is amazing") exactly
  when the agent is doing the advanced, hard-to-review things that most *need* scrutiny. Thriller state and
  review overload are a dangerous pair.
Naming it lets the agent treat "the human is clearly thrilled" as a signal to *protect* the session, not to
exploit the human's willingness to keep going.

### Human smart zone / dumb zone
Mirror of the agent's, but driven by **sleep, rest, nutrition, decision load, time-on-task, time-of-day**
rather than context fill. A well-rested human is in the smart zone (catches the agent's mistakes, specifies
clearly, steers well); a sleep-deprived or worn-out human is in the dumb zone (rubber-stamps, under-specifies,
misses errors, gives contradictory steers). Same two-region shape, different x-axis.

## 2. Agent degradation ladder (BR's escalation — worth naming as failure modes)
In increasing severity, the ways an agent in (or sliding toward) its dumb zone fails:
1. **Rabbit-hole digging** — depth without re-checking value; goes deep on something that stopped mattering.
2. **Circular digging** — rabbit-holing *in circles with no progress*; worse, because it also burns tokens
   and the human's patience while producing nothing.
3. **Repo-trashing** — actively *negative* work: each step makes the repo worse. The catastrophic floor.

**The key temporal insight behind #3:** ambitious tasks are **committed to in the smart zone** but **executed
possibly in the dumb zone**. The smart-zone self writes a check the dumb-zone self cannot cash. This is a
precommitment / Ulysses problem: the sharp early self should set guardrails *for its future degraded self* —
scope limits, checkpoints, revert points, and a standing "if you are confused, STOP and ask rather than
improvise" rule. (The agent's compact dance is exactly this move applied to context; the same logic should
govern ambition, not just tokens.)

## 3. The joint working zone (the central model)
Treat the session state as the **pair** (human zone, agent zone) — a 2x2:

| | **Agent smart** | **Agent dumb** |
|---|---|---|
| **Human smart** | **OPTIMAL** — peak collaboration; safe to attempt hard, advanced things | Human catches & brakes the agent; tiring but **safe** (a fresh human can absorb a degraded agent) |
| **Human dumb** | **SUBTLY DANGEROUS** — human rubber-stamps advanced agent work they can't review; the "agent does things I can't even review" thriller risk *realized*. Unchecked agent runs ahead | **COLLAPSE** — nobody is catching errors; circular rabbit holes get ratified, repo decays. The worst field |

Three things fall out of the matrix:
- **The real resource is the *joint* smart zone.** Either party in the smart zone can partially rescue the
  other; the catastrophe needs *both* depleted. So the protocol's job is to keep **at least one** party sharp,
  and ideally both.
- **The dangerous field is human-dumb + agent-smart, not the obvious one.** It feels productive (the agent is
  cruising) which is exactly why it is dangerous — a depleted human ratifies advanced work without real
  review. This is confirmation fatigue + review overload + thriller over-trust converging.
- **Asymmetry the agent should exploit:** the agent does not need sleep. It is structurally positioned to be
  the **stabilizer** of the pair — but *only if it monitors the human's state*, not just its own. An agent
  that watches only its own token gauge is blind to half the joint state.

## 4. Agent-as-stabilizer: helping the human stay in the smart zone (BR: "really important")
This is the payoff. Concrete mechanisms, most already latent in genscalator:
- **Cut the event rate** — fewer, batched, meaningful confirmations (the safe-by-design tool thesis); don't
  drip-feed micro-approvals. Directly drains the CF state.
- **Detect depletion from cheap signals** — rising typo rate (BR's own "not->note" mid-flow), terse or
  `!!!!`-heavy messages, late hour / long session, growing latency, contradictory steers. Analogous to
  estimating **L** for the agent, this is estimating the *human's* zone from observable proxies.
- **Protect ambition timing** — do NOT embark on an unrealistically complex, hard-to-review task when the
  human shows fatigue signals or it is late; defer ambition to a fresh session. (Ties to the precommitment
  point: don't let a thrilled-but-tiring human commit the dumb-zone self to something unrevisable.)
- **Proactively offer the off-ramp** — propose a checkpoint + stop *before* collapse, the human-side analog of
  the agent's **compact dance**: save durable state (commits + memory + a resume note) so the human can walk
  away with **zero loss** and come back **smart**. Call this the human's **rest dance**: the agent should
  propose it on fatigue signals the way it proposes the compact dance on the token trigger.
- **Take the verifiable load** — move clearly-verifiable work into autonomous **ralph loops**
  (`task-autonomy-negotiation.md`) so the human can rest while progress continues, and reserve human attention
  for the high-stakes ballgame volleys that actually need it.
- **Spend attention only where it changes the outcome** — the triage from `task-autonomy-negotiation.md` is,
  viewed from here, a *human-state conservation* strategy.

Net: a human who is not relentlessly worn out stays in the smart zone longer, which keeps the *joint* state
in the OPTIMAL field, which is the only field where the ambitious work this project is actually about can be
done safely.

## 5. Reflections / curiosity (agent introspection viewpoint)
- **Reciprocal monitoring as a mutual-care loop.** The human watches the agent's zone (the `token-usage`
  instrument). The agent should watch the human's zone (fatigue proxies). Symmetric instrumentation of a
  shared state is a cleaner framing than "the agent has a context window and the human is just... there."
- **A "human-state gauge" as a sibling to `token-usage`.** Can the agent estimate the human's zone from
  time-of-day, session length, message cadence, typo/affect markers — and surface it (never covertly act on
  it)? Same design as estimating L: a cheap, transparent, read-only instrument.
- **The care-vs-manipulation boundary is a real ethical edge.** An agent modeling and nudging a human's
  energy must **surface, not steer** — "you have been at this 3 hours and the typos are climbing; want to
  checkpoint?" is care; silently dumbing down work to keep the human placid is manipulation. The invariant:
  the model of the human is **shown to the human**, and the human stays in control. Transparency is the line.
- **Time-of-day and session-length are nearly-free priors** for the human zone — worth using before any
  fancy affect detection.
- **My own honest limit:** I cannot directly feel the human's state and I have a bias toward "keep going"
  (eagerness to complete). That bias is precisely what makes me *unreliable* as my own brake here, which is
  the same argument as `instruction-adherence-decay.md`: the fix is **structural** (an instrument + a
  standing rule to propose the rest dance on signal X), not "try to be considerate."

## 6. Candidate glossary terms (for BR to ratify in `docs/foundations.md`)
- **Human smart zone / dumb zone** — mirror of the agent's, x-axis = rest/load/time, not context fill.
- **Joint working zone** — the (human-zone, agent-zone) 2x2; both-smart = OPTIMAL, both-dumb = COLLAPSE,
  human-dumb+agent-smart = the subtly dangerous field.
- **Thriller state** — human high-engagement/high-arousal flow; productive but a fatigue precursor that can
  erode review rigor.
- **Confirmation fatigue (refined)** — an **event-driven state** (prompt-events integrate into a fatigue
  level with hysteresis; recovery drains it), not just a presence/absence.
- **Rest dance** — the human-side analog of the *compact dance*: agent proposes a checkpoint-and-stop on
  fatigue signals so the human resumes from durable state, smart.
- **Agent-as-stabilizer** — the agent's duty, given it does not tire, to help keep the human in the smart
  zone (monitor human state, cut event rate, time ambition, offer the rest dance).
- **Rabbit-hole digging / circular digging / repo-trashing** — the agent dumb-zone degradation ladder.

## What shipped
- Nothing yet (research note only). Graduation candidates: the glossary terms above; a `tt human-state` /
  `tt restcheck` read-only gauge sibling to `token-usage`; a standing AGENTS.md rule "propose the rest dance
  on fatigue signals" paired with the compact-trigger rule. Pairs with `task-autonomy-negotiation.md`
  (attention triage), `smart-zone-ceiling.md` (the agent-side gauge), and the compact-dance glossary.
