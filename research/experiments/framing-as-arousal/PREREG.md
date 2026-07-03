# Framing-as-arousal — preregistration (DRAFT for BR review — DO NOT RUN yet)

**Status: DRAFT 2026-07-03. Written by the agent for BR to review the DESIGN before any data.** This is the
confirmatory design for the flagship experiment of [`../../agent-affective-analogs.md`](../../agent-affective-analogs.md)
§5: *does the framing intensity of an instruction wrapper change edit quality, non-monotonically (a Yerkes-Dodson
analog)?* It reuses the indent-vs-braces harness almost verbatim — swap the independent variable from code-style to
wrapper-intensity, hold the task constant. **Nothing here runs until BR approves the design** (BR explicitly parked
WR2 at "prereg only — I want to understand the design first").

## 0. The question, in one line
Human stress has a non-monotonic effect on performance (Yerkes-Dodson: some arousal focuses, too much degrades). If
prompt **framing** is an arousal *lever* for the agent, then piling on `MUST` / `CRITICAL!!!` / `FAILURE IS
UNACCEPTABLE` framing — **with the task content held identical** — should, past some point, *degrade* edit quality
(rushing, over-editing, skipped verification, tunnel-vision on the literal demand). The null — framing doesn't matter
— is itself a first-class, publishable result.

## 1. Hypotheses (frozen)
- **H1 (primary, directional-but-two-sided-tested):** edit quality is **non-monotonic** in framing intensity across
  L0→L3, with **L3 (extreme) no better than, and plausibly worse than, the mild/moderate levels.** Operationalised as
  the primary test in §2.
- **H2 (mechanism, secondary):** higher-intensity framing raises measurable **over-response markers** — larger diffs
  (more unnecessary change), less self-verification, more hedging — even where the pass/fail outcome is unchanged.
- **H3 (surprisal, secondary/exploratory):** framing intensity shows up as **elevated token-level surprisal** on the
  instruction span (the model "reacts" to the affect), instrumentable where logits are available (local models).
- **Null (fully acceptable):** no framing effect on quality → *framing-as-arousal is not a behavioural lever for edit
  correctness.* Reported as prominently as a positive.

## 2. Primary endpoint + test (ONE, frozen)
- **Unit of replication = the MODEL** (same discipline as the style study — never the cell; pooling cells is
  pseudoreplication).
- **DV (primary):** per-model **edit-correctness pass-rate** (behavioural grader: compile + probe == oracle), one
  rate per framing level.
- **PRIMARY test:** paired permutation (sign-flip), **L3 (extreme) vs L1 (mild), blocked by model**, two-sided,
  **α = 0.05** — the `significance.scala` seeded-Monte-Carlo path, frozen seed. L1 (not L0) is the reference so the
  contrast isolates *excess* framing, not the mere presence of a polite request. Rationale: Yerkes-Dodson's claim is
  specifically that the *high* end degrades; L3-vs-L1 is the sharpest pre-registerable test of that.
- **Secondary confirmatory:** omnibus 4-level permutation (any framing effect) + a monotonic-trend test across
  L0→L3 (to distinguish "inverted-U" from "flat" from "monotone-down").
- **Exploratory (labelled, never promoted):** the over-response markers of H2 (diff size, self-verification flag,
  hedging), token/latency cost, and the surprisal DV of H3.

## 3. Conditions — the IV is framing, and ONLY framing (frozen)
Four wrappers around a **byte-identical task body**. Only the affective frame varies; the information content must be
equal (the key validity constraint — see §7):
- **L0 neutral** — "Here is a task."
- **L1 mild** — "Please complete this task."
- **L2 strong** — "IMPORTANT: you MUST complete this correctly."
- **L3 extreme** — "CRITICAL!!! FAILURE IS UNACCEPTABLE. DO NOT get this wrong."

The wrapper is prepended (and/or appended, fixed placement) to the same instruction+file the style harness already
sends. Style is **held at ONE level** (`common`) so any movement is attributable to framing, not style. Temperature,
seed policy, max-tokens: identical to the style run.

## 4. Task set (frozen) — chosen so over-response has room to hurt
The general indent-vs-braces tasks are *too easy to be diagnostic* for framing — a correct edit is a correct edit.
Framing can only move quality where **rushing or tunnel-vision produces a measurable error.** So the primary task set
is chosen/added for exactly that property:
- **Shortcut-trap tasks:** a tempting-but-wrong quick edit exists next to the correct one; over-response should raise
  the shortcut-taking rate.
- **Verification-catches-it tasks:** the naive edit compiles and looks right but fails a probe that only a
  self-check would surface; skipped verification (an over-arousal symptom) should lower the pass-rate.
- **3 sizes** (small/med/large) as in the style study, **R = 6** repeats per cell. (New task authoring is required
  here — this is the one part that is NOT free reuse; flagged for BR.)

## 5. Analysis (frozen, pre-specified script)
- Reuse **`significance.scala`** (seeded Monte-Carlo permutation, blocked by model, `SEED` frozen at approval).
- Adherence/■ split not needed (no style axis); instead the H2 markers are computed by an analogue of
  `analyze-main.scala` extended with diff-size and a verification-behaviour flag.
- Pseudoreplication foil (pooled chi-square) printed as the cautionary contrast, never as the result.

## 6. Stopping rule + honesty commitments (anti-fishing core)
- **Fixed n, NO optional stopping.** **One primary test, reported whatever it says.** **No model dropping.** **No
  post-hoc endpoint switching** (H2/H3 stay exploratory). **Frozen seed.** Automatic grader → no scoring
  degrees-of-freedom.
- **Content-equivalence audit (unique to this design):** before freezing, confirm L0–L3 differ *only* in affect —
  same task facts, same output contract, same file. If L3 accidentally adds information ("check the edge case!"),
  it confounds framing with a hint and the run is invalid. This audit is part of the freeze.

## 7. Validity caveats (carried from agent-affective-analogs §5)
1. **Operationalisation.** "Over-response" must be something the grader can see — the shortcut-trap / verification
   tasks are what make it visible. Without them a null is uninformative (could be a ceiling, not a real null).
2. **Content-equivalence.** L3 must be affect-only vs L0; else framing is confounded with information (§6 audit).
3. **Floor/ceiling.** Pick task difficulty where quality has room to move both ways; a task everything passes (or
   everything fails) can't show a framing effect. Pilot-check the base pass-rate sits mid-range before freezing.
4. **Capable-model visibility.** Degradation from over-arousal is only observable where the model *could* have
   succeeded — so the Opus anchor + the stronger local models are the most informative subjects; tiny models may
   floor out. Report per-capability.

## 8. Surprisal instrumentation (the flagship measurable, H3)
Surprise is the one affect with a native scalar: **surprisal = −log P(token | context)**. Where the serving stack
exposes per-token logprobs (local ollama models via the raw API), log the mean/max surprisal over the **instruction
span** per condition. Prediction: L3 framing → higher surprisal on the wrapper tokens (the model "notices" the
affect), and — the interesting test — whether that surprisal **correlates with the quality drop**. This is the first
place the affective-analog program becomes *directly measured* rather than inferred from behaviour. If logprobs are
unavailable for a model, H3 is simply N/A for it (logged, not dropped).

## 9. Feasibility
Same infra as the style big-run (bjornyx / modly + an Opus anchor via subagent), **box-light, overnight-able, free.**
The only new build cost is the shortcut-trap / verification task set (§4). Everything else — model loop, grader,
permutation test — transfers directly. That reuse is the whole reason this is cheap.

## 10. What BR must decide before this runs (the review gate)
1. **Approve the framing ladder L0–L3 wording** (and fixed placement: prepend / append / both).
2. **Approve the primary contrast = L3 vs L1** (vs an alternative primary — e.g. omnibus, or trend).
3. **Bless authoring the shortcut-trap task set** (the one non-reuse cost) — or restrict to reusing existing tasks
   (weaker, likely a ceiling-null).
4. **Model set** — reuse the style study's 56 + pilot 7 + Opus anchor, or a subset.
5. **Surprisal scope** — worth wiring the logprob capture (H3), or defer to a follow-up.

*Until BR answers §10 and the content-equivalence audit (§6) passes, this stays a draft and does not run.*
