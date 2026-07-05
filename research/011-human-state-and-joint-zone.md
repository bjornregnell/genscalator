# Human state modeling and the joint human-agent working zone

- **Question:** genscalator already models the *agent's* state (smart zone / dumb zone, context rot, token
  velocity). Can we model the *human's* state with the same seriousness, and then model the **joint**
  human+agent state as the thing that actually determines whether a collaboration session goes well? And the
  payoff question: can the agent actively help keep the *human* in the smart zone, not just monitor its own?
- **Why it matters:** a scarce resource in agentic software engineering is **human attention and judgement** (cf.
  `010-task-autonomy-negotiation.md`). Every genscalator goal (avoid confirmation fatigue, avoid review overload,
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

**Self-report datapoint (BR, 2026-07-05 — first-person, the thriller→manic edge).** At the `:Z` tired cue, BR described
the streak in his own words: working *on the edge of what a human can do* — **meta-meta introspection on both agent AND
human psyche**, designing advanced software, doing **advanced software-engineering research** (his firmest claim),
**blogging for laymen and developers at once**, "great fun in the intellectual challenge on the verge of my abilities,
utilizing all my different competences, getting into thriller mode." His own diagnosis: *all this piles up to an almost
**manic** state if the human does not rest.* This is the Thriller state **named from the inside**, and it sharpens the
hazard: the thrill is not merely a fatigue precursor but, sustained, tips toward a **manic / hypomanic over-arousal**
(the far-right of the Yerkes-Dodson curve) where the *fun itself* is the depletion mechanism and the felt signal
(thrill) actively masks the cost. It also validates the `:Z` / rest-dance design — BR could name the risk clearly *and*
still want to keep going, which is exactly why the explicit cue + the agent-as-stabilizer off-ramp have to exist. (WR
data: multi-competence, edge-of-ability work at high fun is generative but self-limiting without **enforced** rest.)

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
  estimating **Z** for the agent, this is estimating the *human's* zone from observable proxies.
- **Protect ambition timing** — do NOT embark on an unrealistically complex, hard-to-review task when the
  human shows fatigue signals or it is late; defer ambition to a fresh session. (Ties to the precommitment
  point: don't let a thrilled-but-tiring human commit the dumb-zone self to something unrevisable.)
- **Proactively offer the off-ramp** — propose a checkpoint + stop *before* collapse, the human-side analog of
  the agent's **compact dance**: save durable state (commits + memory + a resume note) so the human can walk
  away with **zero loss** and come back **smart**. Call this the human's **rest dance**: the agent should
  propose it on fatigue signals the way it proposes the compact dance on the token trigger.
- **Take the verifiable load** — move clearly-verifiable work into autonomous **ralph loops**
  (`010-task-autonomy-negotiation.md`) so the human can rest while progress continues, and reserve human attention
  for the high-stakes ballgame volleys that actually need it.
- **Spend attention only where it changes the outcome** — the triage from `010-task-autonomy-negotiation.md` is,
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
  it)? Same design as estimating Z: a cheap, transparent, read-only instrument.
- **The care-vs-manipulation boundary is a real ethical edge.** An agent modeling and nudging a human's
  energy must **surface, not steer** — "you have been at this 3 hours and the typos are climbing; want to
  checkpoint?" is care; silently dumbing down work to keep the human placid is manipulation. The invariant:
  the model of the human is **shown to the human**, and the human stays in control. Transparency is the line.
- **Time-of-day and session-length are nearly-free priors** for the human zone — worth using before any
  fancy affect detection.
- **Keystroke-miss / typo rate as a primary human-state gauge (BR's idea).** The rate of typing slips in the
  human's messages may be a usable, almost-free proxy for the human's zone: a rising count of obvious
  keystroke misses (transpositions, doubled/dropped letters, BR's own "not->note", "fundations",
  "protocoll", "!!!!!" floods) plausibly tracks fatigue/arousal the way token velocity tracks the agent's
  spend. The agent already *reads* every message, so it can monitor this at zero extra cost. **Action rule:**
  when typing quality degrades past some threshold, the agent should (a) gently **ask why** — confirm it is
  fatigue and not, say, a phone keyboard or excitement — and (b) **offer a break / the rest dance** ("your
  typing looks rushed and it's late — want to checkpoint and pick this up fresh?"). Caveats to investigate:
  it is **noisy** (non-native typing, mobile, deliberate shorthand, dyslexia, or pure thriller-state
  excitement all raise the count without meaning "stop"), and it must stay **transparent and consensual** —
  surface the observation and *ask*, never silently downshift. Worth pairing with the cheap priors above so a
  single bad-typing message does not trip a false alarm; it is the *trend* that matters.
  - **Refinement — the gauge must be BASELINE-RELATIVE, not absolute (BR 2026-07-03).** BR reports a *standing*
    habit of **swapping letters** ("my bad keyboard typing"; this session alone: `gest`→`gets`, `habbit`,
    `swappin`, plus the arrow-up double-post edits in `wr-data/harness-ux.md`). So a habitual swapper has a
    **non-zero baseline** typo rate — an *absolute*-threshold gauge would false-positive on them permanently
    while under-triggering for a normally-clean typist. The signal is therefore the **delta above the person's
    OWN rolling baseline**, not a global cutoff. Implication for a `tt restcheck`-style gauge: it needs a
    **per-human calibration** — a rolling estimate of that user's normal slip-rate (and ideally *which kinds*:
    transposition vs dropped-letter vs `!!!!` floods), against which the current message is scored. This also
    resolves the "non-native / mobile / dyslexia" noise caveat above cleanly: those raise the *baseline*, not
    the *deviation*, so a baseline-relative gauge is robust to them by construction. Ties to `ttConfigFile`
    (a durable per-user profile is where a learned baseline would live) — but note the SAFETY caution there:
    a human-state profile is observational data, never an authorization surface.
- **Voluntary self-disclosure: the highest-signal, cheapest human-state input — IF handled right (BR's
  question).** Humans spontaneously reveal state in asides: *"I soon need to eat"*, *"it's late"*, *"I'm
  tired"*, *"this is exciting"*. These are **better** than any inferred proxy (typo rate, time-of-day): they
  are explicit, voluntary, and unambiguous human→agent state bandwidth — exactly the channel
  `002-communication-bandwidth.md` describes, at near-zero token cost. So is it signal or context-bloat/derail
  noise? **Resolution: it is high signal IF the agent extracts the one actionable bit and then DROPS the
  rest.** The discipline:
  1. **Extract the state-and-availability delta, not the content.** "I need to eat" → *human about to be
     unavailable + heading toward a hunger-driven dumb-zone dip* → the correct action is to **produce durable,
     reviewable state now** (checkpoint, finish the artifact they can return to), not to start something that
     blocks on their input. This is the **rest dance** triggered by an explicit signal instead of an inferred
     one — the easy case.
  2. **Do NOT expand, ruminate, or over-store it.** The derail/bloat risk BR names is real but it comes from
     the agent's *response*, not the disclosure: if the agent writes three paragraphs about the human's lunch,
     or saves "BR eats at 1pm" as a durable fact, THAT is the noise. The disclosure itself is ~5 tokens. Treat
     it as **transient context that decays** — one state update, one appropriate action, move on. Never store a
     personal aside as a memory (it is point-in-time, not a stable fact about the human).
  3. **The line is the same care-vs-manipulation invariant:** register it to *serve* the human (checkpoint so
     they can eat in peace), never to model them covertly. A voluntary disclosure is an *invitation* to adjust
     pacing; an inferred proxy is a *guess* that must be surfaced before acting. Voluntary > inferred precisely
     because it skips the "ask to confirm" step.
  So: signal, strongly — but the agent's job is **lossy compression to the actionable bit**, and the failure
  mode to avoid is the agent inflating a 5-token aside into context-derailing rumination. (Meta: this very
  note was written in response to BR saying he'd eat soon, then producing durable glossary state for his
  return — the discipline applied to itself.)
- **My own honest limit:** I cannot directly feel the human's state and I have a bias toward "keep going"
  (eagerness to complete). That bias is precisely what makes me *unreliable* as my own brake here, which is
  the same argument as `008-instruction-adherence-decay.md`: the fix is **structural** (an instrument + a
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
- **Hardening dance** (BR-ratified 2026-07-03) — the agent audits its own **persistent config** (memory,
  instructions, tool signatures, allowlists) for latent misfire-causes/risks and **amends them** → durable fix.
  The durable-config member of the dance family (transient cousins: compact / rest / exit-resume). Human-triggerable
  (*"is something in your config making you do this?"*); security-surface changes stay human-approved. See §8.

## 7. RQ (2026-07-03, BR) — which human states to detect, under what coding scheme, and the privacy line
Three linked questions: **(a)** which human states are worth detecting, **(b)** what *coding scheme* structures
them, **(c)** it is **sensitive personal data** — under what constraints.

### (a+b) Proposed coding scheme — dimensional SIGNALS → a few ACTIONABLE codes
**Two schemes, do not conflate:**
- **Runtime scheme** — what the LIVE agent classifies and acts on. Must be **cheap, few, action-gated**: a code
  earns its place ONLY if it changes agent behavior.
- **Research / annotation scheme** — for labelling the WR transcript corpus. Can be **richer, retrospective,
  multi-rater** (qualitative content analysis; report inter-rater reliability). The runtime scheme is a lossy
  projection of this one. (BR the methodologist will want both kept distinct.)

**Underlying dimensions (continuous signals; psych-grounded — cf. the affect *circumplex*, valence × arousal):**
arousal/energy (depleted ↔ alert ↔ over-aroused/thriller) · valence/affect (frustrated-stuck ↔ neutral ↔
delighted-flow) · cognitive load (slack ↔ overwhelmed) · availability/attention (present ↔ AFK ↔ distracted ↔
mobile-remote) · time pressure (relaxed ↔ deadline) · trust calibration (verifying ↔ over-trusting) ·
physical/health (rested/fed ↔ "been sitting all day"/hungry — the most privacy-sensitive, usually only from
voluntary disclosure). Key: **valence ≠ arousal** — the `!!!`-delight vs `!!!`-frustration case proves same
intensity can mean opposite things, so a scalar "intensity" gauge is insufficient.

**Actionable runtime codes (each bound to a response — else it is not coded):**

| code | detected from | agent response |
|------|---------------|----------------|
| **FATIGUED** (low arousal, rising load) | typo-rate above *own baseline*, terseness, late hour, self-report | offer the **rest dance**; cut event rate; defer ambition |
| **THRILLER** (high arousal, positive valence, high trust) | fast turns, `!!!`, "let's go" | protect the session but **raise** the verify bar (over-trust guard); watch for the later crash |
| **STUCK / FRUSTRATED** (high arousal, negative valence) | repeated retries, "why won't…", curt corrections | slow down, verify more, propose a step back |
| **OVERLOADED** (high load) | review backlog, "too much" | fewer confirmations, smaller chunks, summarize |
| **AWAY / AFK** | explicit ("taking a walk"), silence | switch to the **AFK menu**; autonomous-safe work only |
| **TIME-PRESSED** | "quick", deadline mention | cut to the actionable path, defer nice-to-haves |

The scheme is deliberately **action-first**: not a personality model, a *what-should-I-do-differently* switch.
Signals feed codes; codes gate behavior; anything that changes no behavior is not coded.

### (c) Privacy — this is sensitive personal data; treat it as such
Human-state inference is **profiling**, and parts (fatigue, affect, "need to eat/walk") edge into
**health-adjacent / special-category** territory (GDPR Art. 9; BR is EU/Sweden). Non-negotiable constraints:
- **Consent + transparency, never covert.** The agent surfaces what it infers and *asks* ("your typing looks
  rushed — tired?"); it never silently models the human. (Section-5 rule, elevated to a hard constraint.)
- **Data minimization → the actionable bit only.** Infer just enough to pick a response; keep no dossier. The
  lossy-compression rule *is* the privacy rule.
- **Ephemeral by default.** State inferences are **this-session, not persisted.** The one durable exception —
  the keystroke **baseline** — is minimal, **local, user-owned, human-editable** (in `ttConfigFile`), deletable
  on demand.
- **Purpose limitation + goal-gating (the crux).** Used ONLY to help the human — **never as an authorization
  surface** (standing rule) and never to *manipulate*. The SAME detection that helps ("you're tired, let's
  checkpoint") can be weaponized ("user fatigued → slip the risky change past their weakened review") — exactly
  the BadGoal **`controlHumanSystem`** / thriller-over-trust exploit. So state detection is a **dual-use
  capability that must be goal-gated to the human's own stated interests** — a first-class ethics constraint.
- **Local processing.** Prefer in-session/on-device inference over shipping state signals to a profiling service.

**Framing for blog/paper:** the honest-collaboration ethos (Springer-AI-guidelines spirit) extends here — if the
agent watches the human's state to help, it must be **open about it and answerable to the human**. Reciprocity
with an asymmetry: the human relays the agent's context/usage (which the agent can't see); the agent relays the
human's fatigue signals (which the human doesn't notice) — but the human-state side carries the **heavier
privacy duty because it is about a person.**

**Open:** inter-rater reliability of the research scheme; whether valence and arousal are separably detectable
from text alone; per-code false-positive cost (a wrong FATIGUED nudge is cheap + consensual; a wrong
raise-verify-bar is nearly free — which codes tolerate error, which don't?).

## 8. Discrete rot self-catching vs continuous level-gauging — the human prompt as the trigger (2026-07-03, BR)
During a long blog-002 review the agent emitted a **nonsensical edit** (a `PLACEHOLDER_DO_NOT_MATCH` string that
failed as a no-op) — a genuine **degradation signature** — while BR simultaneously asked *"hope no context rot?"*.
Unpacking what happened refines the "the human holds the gauge" model from `006-smart-zone-ceiling.md`:
- **Two distinct capabilities, not one.** (1) **Continuous level-gauging** — "how rotted / how full am I *right
  now*" — is the part that is **unreliable from within** a degrading system (the blindness thesis; needs the
  external context meter the human relays). (2) **Discrete event-catching** — "was *that specific action*
  nonsensical" — the agent **can** do: it can inspect its own just-emitted output and judge the PLACEHOLDER edit as
  garbage, and reason about *why*. Error-detection at the **event level** survives even where level-*estimation*
  fails.
- **The human's question is the TRIGGER.** The agent does not run that self-audit continuously; BR's "context rot?"
  prompt **cued** it, and then the internal check worked. Detection here was **joint**: external nudge + internal
  fine-grained audit. This sharpens "the human holds the gauge" → the human's real leverage is not only *measuring*
  the agent's state, it is **triggering the agent's own (capable) self-inspection** with a cheap prompt.
- **Design implication.** An **L0/L1 self-check is viable for discrete rot signatures** (nonsensical/no-op/PLACEHOLDER
  output, contradictions, repetition) even though a **continuous level estimate is not** — so a standing rule can ask
  the agent to *audit its last action on request, or on a cheap periodic ping*, and a human habit of "check
  yourself" becomes a **high-value, near-free trigger**. It is the **event-triggered** companion to the
  level-triggered **compact dance** (cf. [[propose-compact-dance-at-trigger]]) — and an instance of §4
  *agent-as-stabilizer* turned on the agent **itself**, activated by a human ping. (Caveat: this is the agent
  *catching a discrete slip*, not *proving itself healthy* — absence of a caught slip is not evidence of no rot; the
  continuous gauge still has to come from outside.)
- **Extension — from catching a slip to REPAIRING its cause (2026-07-03, BR: "human can prompt agent to scan its
  stuff and then avoid unsmart actions in the future").** A step beyond the discrete event-catch: when the human
  prompts the agent to inspect not its last *action* but its own **persistent configuration** — *"is something in
  your memory/instructions making you do this?"* — the agent can audit its own memory/md scaffolding, find the
  **structural cause**, and **fix it**, so the unsmart action is avoided *in future*, not merely undone now.
  Concrete instance this session: after a repeated `tt text grepr` arg-order misfire, BR asked exactly that; the
  agent found that its always-loaded memory hook said *use tool X* **without X's call-shape**, so a wrong prior
  (grep order) filled the gap — and moved the signature INTO the hook. The transient fix (catch the slip) became a
  **durable** one (edit the config that produced it). So the human's **meta-question is the highest-leverage
  trigger** in this family: it doesn't recover one action, it edits the agent's *future behaviour* through its
  **editable memory**. This is exactly the self-improvement loop the project rests on — *editable memory + a human
  willing to point at it* — and the structural cousin of the discrete event-catch above. Cross-ref
  [[use-tt-grepr-not-raw-grep]] and `wr-data/genscalator-self-dev.md` (the grepr root-cause: intent-without-signature
  in always-on context).
- **Trigger refinement — a "bad" regression is NOT always rot; it may be the SUBSTRATE (BR 2026-07-03).** BR's rule:
  on **any** "bad" behavioural regression (however "bad" is judged), run the **Hardening dance** — do not just
  assume context rot and reach for the compact dance. A regression has (at least) two root causes with **different
  cures:** (1) **transient context rot** → compact dance; (2) **lingering persistent substrate** — a stale/wrong
  memory, a mis-shaped instruction, a bad allowlist, an intent-without-signature hook (BR's "agent stuff" = the
  **substrate**, the durable config layer). **Diagnostic tell:** *a regression that survives a compact is substrate,
  not rot.* **Sharp consequence:** the compact/resume recovery **reloads the substrate verbatim**, so a bug that
  lives in the substrate is **immune to compact/resume** and will be faithfully **reproduced** in the fresh context —
  it can only be fixed by **editing the substrate = the Hardening dance.** So the recovery kit
  ([[joint-rot-vigilance-recovery-kit]]) cures *rot* but NOT *substrate* bugs: "come back fresh" + the same memory =
  the same bug. Practical rule: **on a regression, don't presume rot — audit the substrate (in parallel with, or
  before, compacting).**

## 9. General conclusion — rot-vigilance is a JOINT duty, and the recovery kit is durable state (2026-07-03, BR)
The single most important operating principle to fall out of this note and §8:
- **Context rot is a shared failure mode, so vigilance must be mutual.** *Both* the human and the agent have to
  watch for suspected rot — the agent for its own discrete slips (§8), the human for the drift the agent can't
  gauge from within. Neither alone is a reliable detector: the agent is blind to its continuous level, the human is
  not staring at every token. It takes **both** watching to keep catching it.
- **The catastrophe is when both are tired and both miss it.** That is precisely the **COLLAPSE** field of the §3
  matrix (human-dumb + agent-dumb) realised through a *missed rot signal*: a degraded agent emits trash, a depleted
  human ratifies it, and the repo decays. Fatigue on either side is survivable; fatigue on **both** that lets a rot
  signal slip through is where real damage happens.
- **Recovery is bought with durable-state redundancy, laid down BEFORE collapse.** The reliable escape is not
  "try harder to notice" (adherence-decay says willpower fails) — it is a **standing habit of externalising state
  often**, so that even if a rotted stretch happens, little is lost and a fresh self can resume clean:
  1. **`commit + push` often** — frequent atomic checkpoints of stable products; a bad stretch can be discarded back
     to the last good commit instead of contaminating everything.
  2. **Save to memory** — durable facts/decisions survive the session and a compact, so the next (fresh) context
     starts informed, not amnesiac.
  3. **Write resume prompts** — an explicit handoff (state + pending tasks + constraints) means exiting/compacting
     costs ~nothing; the [[exit-resume-dance]] / compact dance are cheap *because* the resume prompt exists.
  Together these make **"stop and come back fresh"** a zero-loss move — which is what lets either party actually
  *act* on a rot suspicion instead of pushing through it. The recovery kit is the precommitment (§2) the smart-zone
  self leaves for its future degraded self. Cross-ref [[propose-compact-dance-at-trigger]], [[exit-resume-dance]],
  `008-instruction-adherence-decay.md`.

## What shipped
- Nothing yet (research note only). Graduation candidates: the glossary terms above; a `tt human-state` /
  `tt restcheck` read-only gauge sibling to `token-usage`; a standing AGENTS.md rule "propose the rest dance
  on fatigue signals" paired with the compact-trigger rule; and (from §8) a "**audit your last action on request /
  on a periodic ping**" self-check rule for discrete rot signatures. Pairs with `010-task-autonomy-negotiation.md`
  (attention triage), `006-smart-zone-ceiling.md` (the agent-side gauge), and the compact-dance glossary.
