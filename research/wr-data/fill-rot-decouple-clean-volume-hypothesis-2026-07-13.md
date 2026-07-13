# WR data: fill and rot may DECOUPLE — clean high-volume work fills context without corrupting it (2026-07-13, UNTESTABLE hypothesis)

**Type:** WR data — a hypothesis BR explicitly framed as **one we cannot test**. Logged as such (like the
compact-duration-vs-fill hypothesis), not as a result.
**Threads:** `research/006-smart-zone-ceiling.md`, [[propose-compact-dance-at-trigger]],
[[joint-rot-vigilance-recovery-kit]], [[agent-affective-analogs]], the family-E can't-self-certify caveat.

## The datum + the hypothesis (BR)
Live reading: **ctx-fill 38%** (past the smart-zone ceiling Z ~30%), yet BR's external read is **rot LOW**. His
hypothesis: *"you just did long stuff, [it did] not corrupt your short-term memory with things that will make
you regress into oblivion — but we don't know."* i.e. **fill and rot decouple**: the long work this session
(the byte-faithful 183-line skill reproduction, the many WR captures) was **high-VOLUME but CLEAN**, so it
raised *fill* without proportionally raising *rot*.

## The distinction it draws
- **Fill** = how much of the window is consumed (measurable, on the gauge).
- **Rot** = degradation of the agent's reasoning/coherence (NOT directly measurable).
- The hypothesis: rot comes from **accumulated confusion / contradiction / degraded signal**, NOT from sheer
  *volume* of coherent content. So filling the window with faithful, non-confusing work (a clean reproduction,
  well-formed notes) costs *fill* but little *rot*; filling it with muddled, self-contradicting, or
  hallucinated material costs both. Same fill %, different rot.

## Supporting proxies (adjacent, NOT proof)
At 38% fill the behavioural proxies stayed clean: the **byte-faithful diff** (precision held over 183 lines),
correct temporal arithmetic (three samples → constant turn-start), sound security/history reasoning,
multi-message queue reconciliation without loss. Consistent with "low rot despite past-Z fill" — but proxies,
not a rot meter.

## Why it is UNTESTABLE (BR's own framing)
- **Family E:** the agent cannot self-certify rot; introspection is unfalsifiable from inside.
- **No ground-truth rot meter** exists — only behavioural proxies and the human's external read.
- **No counterfactual:** we can't run the same session *without* the long work to compare. One timeline only.
So it stays a hypothesis. Note the reflexive point: **fill remains the only *actionable* gauge precisely
BECAUSE rot is unmeasurable** — we steer by the thing we can see.

## Implication (conservative default holds)
If the hypothesis were true, past-Z fill from *clean* work would be less alarming than past-Z fill from
*confused* work. But since we cannot tell the two apart from inside, we keep treating **fill as the
conservative trigger** for the compact/consolidate dance regardless. The hypothesis refines our *understanding*
of the fill-rot relationship; it does not license ignoring the gauge.

## Open research question (BR, pin 2026-07-13): is there a CERTAINTY threshold D?
The decouple hypothesis says fill and rot are not identical — clean volume raises fill without necessarily
raising rot. BR's complementary research question: **is there a *very high* ctx-fill ratio above which we can
say WITH CERTAINTY that the agent is in the "dumb-zone" (rotted)?** — i.e. does the decoupling **break down at
the top of the window**, a saturation regime where rot becomes *mechanically inevitable* (attention/retrieval
degradation as the context fills), regardless of how clean the content is?
- If YES, there are **two thresholds**: **Z** (~30%, where rot RISK begins — content-dependent above it, per the
  decouple hypothesis) and a higher **D** (where rot is CERTAIN — content-independent). Between Z and D, rot is
  possible-but-not-certain; above D, certain.
- The family-E measurement problem still bites: we cannot read rot directly, so "with certainty" cannot come
  from the fill *number* alone — it would have to be established by **unmistakable behavioural collapse**
  (reproducible Q-test failure, losing the thread) that is *universally* observed above some fill. So the honest,
  testable form: **is there a fill fraction D above which behavioural rot is observed across essentially all
  sessions/tasks?** — an empirical claim we can accumulate bounded evidence for (never prove absolutely).
- If NO such universal D exists (rot always stays content-dependent, even near the ceiling), that is itself a
  strong finding — it would mean the gauge can only ever signal RISK, never CERTAINTY, and the human's external
  read stays irreplaceable at every fill level.
- Ties: `research/006-smart-zone-ceiling.md` (Z), [[propose-compact-dance-at-trigger]], [[cue-do-q-test]] (the
  fidelity probe that could detect the collapse), the compact-duration-vs-fill data, the `dumb-zone` mode.
