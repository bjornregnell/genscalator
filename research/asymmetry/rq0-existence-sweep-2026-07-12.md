# RQ0 existence sweep - result (PARTIAL, 2026-07-12)

**Status: PARTIAL.** The sweep completed its surface phase, but verification and synthesis were cut off when we
hit the Max-5x SESSION limit (resets 9pm Europe/Stockholm). What survived already answers RQ0.

## Verdict (partial, positive)
**Existence is demonstrated.** Before the limit cut in, independent CF5 verifier agents confirmed **20 asymmetry
instances as REAL** (verbatim-grounded quotes in their source files), spanning a wide set of asymmetry types.
Twenty independently-verified real instances is a solid existence proof for RQ0. The other 201 surfaced candidates
are unverified only because the run was interrupted, not rejected.

## Run stats
- Corpus: **76** wr-data specimens.
- Sweep: **8** CF5 (Fable 5) agents fanned out over slices, surfacing **221 candidate** instances (each with an
  exact verbatim quote anchor).
- Verify: **68** independent CF5 agents (one per file), adversarial + default-reject; **16 completed** before the
  session limit, **62 failed** with "session limit". Of the completed: **20 verified real**.
- Synthesize: failed on the limit (no auto-dedup/typing yet).

## Method (for the post)
Three machine layers by fresh-context CF5 agents that did NOT write the specimens (bias reduction), plus BR's human
echt-check on top:
1. **Surface** (8 agents): read the corpus, extract concrete asymmetry instances with exact quotes.
2. **Independent verify** (per-file agents): strict; grounded only if the quote is verbatim in the file AND
   genuinely evidences the asymmetry (not an over-read); default-reject.
3. (pending) **Synthesize**: dedup + type + count.
4. (pending) **BR echt spot-check**.

## Asymmetry types among the 20 verified (existence spans many kinds)
embodiment (typing-comfort as a selection criterion) - habit-inertia (human reflex still reached the old word
minutes after a rename) - agent-fatigue-analog (context rot as an agent "sickness"; end-of-session slips) -
memory-vs-habit (recalled != enacted for fresh sub-agents and the super-agent) - joint-vigilance (slips caught
human-side) - affect (the panic-writes as the human's felt distress the agent did not have) - introspection-limits
(agent self-monitoring is a post-hoc detector, not a pre-hoc preventer) - turn-taking/bandwidth (accepting more
work past the agent's own proposed stop is the risk) - memory-continuity (agent history lives in external on-disk
structure).

## Raw + resume
- Full result (all 20 verified + the 221 candidates): the workflow output + `journal.jsonl` under
  `.../subagents/workflows/wf_c4647a6f-107/`.
- **To complete after the reset:** resume the workflow (cached agents replay; only the 62 failed verify agents +
  the synth re-run), then BR does the echt spot-check. Script: `wf_c4647a6f-107`.
