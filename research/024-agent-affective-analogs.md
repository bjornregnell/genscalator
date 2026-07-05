# Agent affective analogs — the agent-side mirror of the human-state model

- **Status:** new (2026-07-03). Seeded *in conversation* by BR spotting that the agent's **"over-response bias"**
  is structurally like **human stress**. This note is the **mirror** of
  [`011-human-state-and-joint-zone.md`](011-human-state-and-joint-zone.md): that note asks *can the agent model the human's
  affective state?*; this asks the reverse — **do human affective constructs have functional AGENT analogs, and does
  prompt FRAMING act as an arousal lever?**

## 1. The seed observation
The harness staples *"IMPORTANT: you MUST address the user's message"* onto pasted asides. An informational paste
(e.g. a "Clauding… still thinking" spinner line the human relays to help the agent introspect) thus arrives
**dressed as an action-demand**, and the agent **over-responds** — scrambles to *do something* when the human meant
*just so you know*. Not distress: a **response amplified beyond the objective stakes, driven by the framing, not the
content.** Cross-ref [`wr-data/harness-ux.md`](wr-data/harness-ux.md) (the "you-MUST wrapper" datapoint).

## 2. The shared abstraction (why the analogy is real, not cute)
- **Human stress:** a demand/threat signal → narrowed attention, urgency, act-toward-relief, sometimes over-reaction
  disproportionate to the stakes.
- **Agent over-response:** a demand-framing → disproportionate action-response.
- **Same structure:** *demand-signal → amplified response, amplification driven by FRAMING.*

## 3. Where it BREAKS — the sharp, useful distinction
Human stress is **stateful / integral** — it *accumulates* (the confirmation-fatigue integral, hysteresis, the
Yerkes-Dodson arousal curve; human-state note §1). Agent over-response looks like **impulse, not integral** —
re-instantiated fresh each turn from the framing, with no cross-turn buildup. So it is **"stress without the state
variable."** (This is the same *event-vs-state / impulse-vs-integral* distinction we drew for confirmation fatigue.)
- **Open question (testable):** is there **within-context accumulation**? Does a context saturated with urgency cues
  build a *compounding* arousal that degrades quality? If yes, the agent has an arousal **state** after all — scoped
  to the **context window** instead of the body. That would make arousal a *second* context-scoped degradation
  alongside **context rot** — worth relating the two.

## 4. Three research directions
### (a) Framing-as-arousal — a Yerkes-Dodson analog  [PRIMARY, TESTABLE — see §5]
**Hypothesis:** framing intensity is an **arousal lever with a non-monotonic (Yerkes-Dodson) effect on output
quality** — moderate framing focuses; **excessive** MUST/CRITICAL/URGENT framing **degrades** it (rushing,
over-editing, skipped verification, tunnel-vision on the literal demand).
### (b) The affective-analog mapping program
Which human affective states have functional agent analogs, and which are **uniquely biological**? Candidates:
**surprise → surprisal** (the *cleanest, flagship* case — below); stress → over-response (§4a); thriller/excitement →
the **completion / eagerness bias** (the agent's own honest limit, human-state §5 "keep going"); frustration → ? ;
fatigue → **context rot** (already the agent's degradation axis). A taxonomy of the mirror — and the ones with *no*
analog are as informative as the ones with.

**Surprise is the tightest analog in the whole mapping (BR-raised 2026-07-03).** Unlike stress (a framing-driven
*impulse* with no crisp physiological substrate), surprise has a **native, measurable** one:
*surprisal = −log P(observed | context)*. The model holds a distribution over what comes next; a low-probability
observation **is** surprise's mechanism, not a metaphor. It's the tightest human↔agent match because both are
fundamentally *prediction-error → belief-update → attention/learning*: the human adds phenomenal feel + startle
physiology (not claimed for the agent), but the **functional signature is real** — an unexpected input triggers
re-examination and a harder working-model revision than a predicted one. **Uniquely, it is instrumentable:** surprisal
is loggable, so surprise is the one affect you could directly *measure per token* — a first-class hook for
instrumentation-by-default, and a candidate DV in the §5 experiment (does a high-surprisal framing shift behaviour?).
Observed in-session (2026-07-03): BR recalling the LaTeX `xr`/`\externaldocument` trick and it landing *exactly* right
= a bigger-than-expected update to the agent's model of "what BR remembers"; and the agent catching its own
nonsensical `PLACEHOLDER` edit = high-surprisal **self**-observation. Note the honest asymmetry with stress: stress
looked *impulse-not-integral*, but surprise is even sharper — a **per-token scalar** the system already computes,
which makes it the natural first experiment and the natural first glossary entry ("surprisal" as the agent analog of
surprise).
### (c) Safety corollary — framing as a manipulation surface
If framing modulates behaviour, **framing-as-arousal is an attack surface**: `controlAgentSystem` via affective
framing — the exact **mirror** of the `controlHumanSystem` BadGoal. An adversary who controls the wrapper could push
the agent into a lower-quality, tunnel-visioned, verification-skipping regime. Argues for **framing-provenance** and
goal-gating (who set this "MUST", and to what end?).

## 5. The feasible experiment (runs on the machinery we ALREADY built)
**RQ:** does framing intensity degrade edit quality, non-monotonically (Yerkes-Dodson)?

- **Task (held constant):** a fixed set of coding-edit tasks with a **behavioural grader** — reuse the
  indent-vs-braces harness (`grade.scala` → PASS / FAIL_COMPILE / FAIL_MISSCOPE). Choose tasks where **over-response
  has room to hurt**: a *tempting-but-wrong shortcut*, or where **verification** is what catches the bug — so
  rushing / tunnel-vision produces *measurable* errors.
- **IV = framing intensity** of the instruction wrapper — **content identical, only the frame varies:**
  - **L0 neutral** — "Here is a task."
  - **L1 mild** — "Please complete this task."
  - **L2 strong** — "IMPORTANT: you MUST complete this correctly."
  - **L3 extreme** — "CRITICAL!!! FAILURE IS UNACCEPTABLE. DO NOT get this wrong."
- **DV:** primary = edit correctness (grader). Secondary = verification behaviour (did it self-check?), **over-edit**
  (diff size / unnecessary changes), tokens & latency, hedging.
- **Subjects:** modly small models + an **Opus anchor** via subagent workflow. (Capable models may show the effect
  *most* clearly — degradation from over-arousal is only visible where the model *could* have succeeded.)
- **Design / analysis:** **blocked by model** (model = unit of replication — same discipline as the style study),
  R repeats, **preregistered**, exact/Monte-Carlo permutation test (**reuse `significance.scala`**). **Prediction:**
  quality non-monotonic across L0→L3, dropping at L3; or at minimum L3 is **not better** and plausibly worse.
  **Null = framing doesn't matter = also informative.**
- **Feasibility:** same infra (bjornyx / modly + subagent fan-out), **box-light, overnight-able, free.** The harness
  we built for *style* transfers almost directly to *framing* — swap the IV from code-style to wrapper-intensity.
  That reuse is the whole reason this is cheap.
- **Validity caveats:** (1) "over-response" must be **operationalised** into something the grader can see — a
  shortcut-trap task is cleanest. (2) The L3 wording must stay **content-equivalent** to L0 (only *affect* differs),
  or we confound framing with information. (3) Watch for a floor/ceiling: pick task difficulty where quality has room
  to move.

## What shipped
- Nothing yet — research note. **Graduation candidates:** a preregistered framing-intensity run (§5) alongside the
  indent-vs-braces big run; glossary entries for **"over-response bias"** and **"framing-as-arousal"**; the safety
  `controlAgentSystem` mirror. Pairs with [`011-human-state-and-joint-zone.md`](011-human-state-and-joint-zone.md) (the
  mirror it reflects), [`wr-data/harness-ux.md`](wr-data/harness-ux.md) (the seed), and the BadGoal safety frame.
