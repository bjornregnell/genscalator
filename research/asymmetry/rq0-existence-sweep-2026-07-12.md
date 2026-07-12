# RQ0 existence sweep - result (COMPLETE, 2026-07-12)

**Status: COMPLETE.** Surface + verification + synthesis all finished after the session cap reset (resumed from
cache; all 78 CF5 agents done, 0 errors).

## Verdict
**RQ0 is answered: the human-agent asymmetry EXISTS and is abundantly documented.** Independent CF5 verifier agents
confirmed **217 asymmetry instances as REAL** (verbatim-grounded quotes) out of 221 surfaced - a **98% grounding
rate** - spanning **8 asymmetry families** across the corpus and 9 calendar days (2026-07-04 to 2026-07-12).
Existence is settled far past threshold. The sturdier sub-claim: **29 independent source documents** each contain
at least one verified asymmetry observation.

## Run stats
- Corpus: **76** wr-data specimens.
- Surface: **8** CF5 (Fable 5) agents -> **221 candidate** instances (each with an exact verbatim quote).
- Independent verify: **68** CF5 agents (one per file), adversarial + default-reject -> **217 verified real** (98%).
- Synthesize: 1 CF5 agent. NOTE: its input was capped at ~60k chars, so it deduped/typed a ~87-instance SAMPLE of
  the 217 (reporting 83 distinct across 29 files). The family STRUCTURE and the VERDICT hold; the per-family counts
  below are a LOWER BOUND - the full verified set is 217. (A full uncapped re-synth is available if exact counts
  are wanted.)

## Method (for the post)
Three machine layers, all by fresh-context CF5 agents that did NOT write the specimens (bias reduction), with BR's
human echt-check on top:
1. **Surface** (8 agents) - read the corpus, extract concrete asymmetry instances with exact quotes.
2. **Independent verify** (per-file agents) - strict; grounded only if the quote is verbatim in the file AND
   genuinely evidences the asymmetry; default-reject.
3. **Synthesize** - dedup + type + count + verdict.
4. (owed) **BR echt spot-check**.

## The 8 asymmetry families (from the synthesis; counts are lower bounds)
- **A. Memory & compute architecture** - agent memory is external, on-disk, lossily compacted, re-read per turn at
  real cost; the human carries state natively. *e.g. "the agent's memory/history lives in external structure".*
- **B. Recall != enactment** - for agents, a rule held in context does not become behaviour; knowledge and
  discipline are separate layers. *e.g. "Recall of a rule is not the rule firing at the point of action".*
- **C. Load / fatigue analogs** - degradation modes structurally like human fatigue, hurry, absorption (rot,
  end-of-session slips, corner-cutting under velocity). *e.g. "held all session and then slipped at the tail".*
- **D. Introspective blind spots, confabulation, imitation** - the agent cannot read its own state gauges and its
  self-report is confabulation-prone. *e.g. "the actual context fill / true rot magnitude - I cannot read it".*
- **E. Felt time, embodiment, perception** - no clock, body, or felt duration; cannot see its own stalls or UI;
  runs both ways (exact timestamp precision the human lacks). *e.g. "a 9-min tool call and a 1-sec one feel identical".*
- **F. Affect & the lossy channel** - the human has real affect (panic, mirth, offense-capacity) the text channel
  drops; the agent's side is absence, not suppression. *e.g. "the agent's laughter is more likely ABSENT than suppressed".*
- **G. Verification, authority, joint vigilance** - the human is the external verifier, ground truth for his own
  state, and the structural gate on dangerous acts. *e.g. "the safety did NOT depend on the super-agent noticing the confab".*
- **H. Complementary division of labour & bandwidth (the upside face)** - human ignition + agent articulation, the
  dyad exceeding either arm. *e.g. "the dyad's output exceeded either arm"; BR's live hypothesis "asymmetry has some real upsides".*

## Honest caveats (the echt part - feeds 021's "Can we trust this?")
1. **Author = subject for much of the corpus.** Most specimens were written by the agent about itself or the dyad.
   The sweep verified the quotes EXIST verbatim, NOT that the claims are TRUE. Agent self-observation is
   confabulation-prone - a limit several specimens themselves document (at least consistent).
2. **Same-day authorship.** Several 2026-07-12 specimens were written the same day as the sweep - high fidelity to
   events, but no settling time or human review yet.
3. **Verifier independence is partial.** Verification was by CF5 agents, the same model family as the (co-)author
   of many specimens.

## Raw + next
- Full result (all 217 verified + the 221 candidates + the full synthesis report): the workflow output +
  `journal.jsonl` under `.../subagents/workflows/wf_c4647a6f-107/`.
- **Next: BR's echt spot-check** - verify a sample of the 217 are genuinely in the files (the human layer on top of
  the CF5 verification), then wire the headline numbers into blog 021's "What did we find?" and its "Can we trust
  this?" (the three caveats above are ready-made).
- Optional: re-run the synthesis uncapped for exact per-family counts.
