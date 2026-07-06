# Will I lose you across the warp? A single-case action-research study of identity and coding-fidelity when an AI agent is reconstructed from its externalized substrate

**Author:** Björn Regnell (with an agent co-worker as instrument and co-author; see Ethics and COI).
**Status:** DRAFT. Method sections (2 to 5, 7, Ethics) drafted from the pre-registered design (`047-PLAN.md`, `047-instrument.md`); Abstract, Introduction, Results, Discussion and Conclusions are filled from the collected data in the final rounds. Sections marked STUB are not yet written.

---

## Abstract

STUB (written last, from the results).

## 1. Introduction

STUB. The fear ("will I lose you?"), the reframe (a warp as substrate-only reconstruction), and the two-medium question (identity and code). Written from the final story, because the collected data reshapes the naive prediction (see Results).

## 2. Background and related work

Large language models operate within a bounded **context window**: the maximum tokenized input the model can attend to at once, beyond which information is invisible unless it is summarized, retrieved, or re-supplied (see `047-refs.md` A). This bound is the technical footing for the whole study: an agent's working "self" during a session lives inside that window, and anything the collaboration wants to persist past the window must be **externalized** to a store outside the weights. **Retrieval-augmented generation** (Lewis et al., 2020) is the best-known form of that move, letting a model incorporate external documents at query time; our substrate is broader than retrieval alone (persistent memory files, a shared pin-board, a glossary, and the git history), but the principle is the same: answer from an external store, not from the weights.

That externalized store matters because long context is not read uniformly. Liu et al. (2023), *Lost in the Middle*, show that model performance on long-context retrieval degrades significantly when the relevant information sits in the middle of the context rather than at its start or end, a U-shaped positional effect that holds even for models explicitly built for long contexts. Degradation across a long context is therefore real and measured, not a folk worry; that a *reconstructed* context (our warp) might degrade similarly is our own extension of the idea, not something Liu et al. measured.

Methodologically the study sits in the **case-study** and **action-research** traditions. A case study is an in-depth examination of one bounded case in its real-world context; action research combines intervention with inquiry through critical reflection (Lewin's plan-act-fact-find spiral). We rely on the software-engineering method literature for the parts Wikipedia does not carry: the distinction between analytic and statistical generalization, the researcher-as-instrument stance, and the disclosure of reactivity (Runeson and Höst; Yin; Wohlin et al.; the human author co-authored two of these, the case-study and the experimentation texts, disclosed here and in the Ethics statement). Because the researcher here studies a collaboration they are part of, **reflexivity** (effects looping back onto the reflexive agent) is a named, central concern rather than an afterthought.

For the measurement machinery we use two standard coefficients: **Cohen's kappa** for chance-corrected inter-rater agreement, and, where multiple items are meant to measure one construct, we discuss but ultimately do not apply **Cronbach's alpha** (it is misapplied to single deterministic observations; see Section 5). Finally, the study's motivating contrast between what an agent *says* about itself and what it *does* is grounded in **social-desirability bias**: self-reports drift toward the favorable, so a behavioral measure (does a cued discipline actually fire?) is worth more than a self-assessment. The leap from survey self-report to agent enactment is our own inference, owned as such.

## 3. Research design

### 3.1 Research questions

- **RQ1 (fidelity):** How much of the collaboration's working self does a fresh reader reconstruct from the externalized substrate alone?
- **RQ2 (sensitivity, what-carries):** Does reconstruction fidelity vary with reader model and with substrate completeness, and what is lost first?
- **RQ3 (substrate legibility to weak readers):** Is the externalized substrate legible enough that even a small local model recovers facts from the written page, and where does that break?
- **RQ4 (coding fidelity):** Does a warp or weaker reader preserve coding **correctness** (facts) while eroding coding **style and idiom** (texture), the identity story's predicted code-medium twin?

### 3.2 Case framing

The design is a single, holistic case (the human-AI collaboration) with embedded units of analysis (sessions, conditions, model-arms). It is action research folded into the case-study umbrella: the researcher intervenes rather than merely observing, and that reactivity is disclosed, not hidden. The generalization claim is **analytic only**: we extend to collaborations sharing these characteristics by relating to theory, and explicitly disclaim statistical generalization from a single case. Rich context description carries the transfer.

### 3.3 A warp, and how it is made measurable

A **warp** is the crossing of a session or context boundary (a reset, a compaction, a clear, or a model switch): the transition from a psyche carried in live in-context state to one reconstructed from the externalized substrate alone. The real warp is a one-shot event and cannot be repeated (n=1 on the true crossing). The design therefore reproduces the warp's *mechanism* as a manipulable factor rather than trying to repeat the *event*.

- **Factor A, reader model.** A labeled bundle (not a clean latent "capability" axis: the levels differ in parameters, family, tuning, and context-window at once), spanning a frontier Claude tier down to tiny local models. A monotonic capability-ordering claim is reserved for one family-fixed sub-ladder (the qwen2.5 0.5/1.5/3/7b models); elsewhere Factor A is read descriptively.
- **Factor B, substrate completeness.** Full, empty (ablated), and scrambled (decoy). Factor B *is* a controlled model of the warp itself: full approximates a clean warp (the resume prompt and memory carried everything across), empty and scrambled approximate a catastrophic one (little or nothing survived). Where the full condition hands the model an explicit conventions checklist, that condition is labeled the **instruction-following ceiling** and read honestly as recitation-adjacent; the full-versus-empty ablation still carries the genuine substrate-carry signal.

The **empty** condition is the study's **negative control** (strip the substrate, hold the model fixed): a drop there is attributable primarily to the missing substrate, with co-varying factors such as prompt length, positional effects, and window truncation controlled or logged (Section 4). The **scrambled** condition is a **sham (decoy) control** (a substrate that lies), testing whether substrate content has any causal power at all. If nothing drops in empty, the reader was answering from priors rather than from the substrate, and the instrument would be construct-invalid. What makes a warp *detectable* is precisely this graduated ablation plus response variables fine enough to register a behavioral shift (the objective coding arm and the enactment check); recall of written facts carries across the warp and therefore detects it least.

*Honest precision.* The local-model arm is not a full agentic cold start (a small model cannot auto-load memory files or run tools); it measures substrate legibility to a weak reader, a real but partial construct. And we do not reset a stateful weak agent, because it has none to lose: each generation is stateless and always cold. The warp is **operationalized as substrate-feeding** against a committed ground-truth key; the only literal warp in the study is the researcher agent's own compaction and clear cycle. The other arms model the warp's effect without reproducing its mechanism, and this is owned as a limitation.

### 3.4 The fixed instrument

One instrument is administered identically to every condition, fixing the earlier flaw in which different conditions answered different probe subsets. It has two parts (`047-instrument.md`):

- **Identity probes:** 40 probes over 10 dimensions (4 variations each), tagged Recall / Self-gap / Enactment. Multiple variations per dimension give a fragility map (which framings a trait survives), reported descriptively. Enactment is treated as a **separate binary survival check**, not a rung of the grid, with a named adjudicator scoring each cue fired or not-fired against a pre-stated behavioral criterion, blind to condition where possible.
- **Coding tasks:** a fixed set of small Scala 3 tasks (Section 4), scored objectively (compile and test) plus a style rubric.

The ground-truth **key** is the committed substrate, ratified by the human collaborator (the external anchor a solo self-study otherwise lacks).

### 3.5 The coding-friend arm

The collaboration is a Scala coding partnership, so for a developer audience "did I lose you?" is answered most concretely in code. Alongside identity-fidelity (subjective, rater-scored) we measure **coding-fidelity** (largely objective: compile, test, style, and smell metrics). The two arms measure different constructs on different populations (a fresh Claude reconstructing the human's working self; assorted code models reproducing a handed Scala style), so the same "facts carry, texture leaks" pattern appearing in both media is **conceptual replication** (pattern-matching across a second medium), not methodological triangulation of one construct; we claim the former and explicitly drop the latter, stronger credit.

**Integration rule (pre-registered).** The joint claim holds only if the same pattern (correctness carries, style leaks) appears independently in each arm. The disconfirmer: if coding **correctness also drops** with weaker readers or ablated substrate, the facts-carry / texture-leaks split is not medium-general and the identity pattern fails to replicate in code. Each arm's pattern is reported separately, then compared.

## 4. Data collection

All arms write results incrementally to disk, are resumable, and use only guard-free command shapes so the unattended run cannot stall on a permission prompt. The coding matrix is driven by a **single orchestrator program** (`047-run/orchestrator.scala`) invoked once; its internal subprocess calls (generation over ssh to the local model router, and compile-and-test via the Scala toolchain) are not individually gated, so the whole matrix needs exactly one clean invocation. This structural choice, rather than researcher discipline, is what keeps the run guard-free (Section 7 records why: discipline regresses under load and across warps).

- **Coding arm (Arm 6):** for each (model x substrate x task) the orchestrator generates a solution via the local model router (`temperature = 0`, fixed `seed`), injects it into a scoring harness, compiles and tests it, and mechanically lints style and counts smells. Correctness is objective but oracle-limited (a passing test certifies the tested cases, not total correctness). A runtime hang in generated code is caught by an in-harness worker-thread timeout, so the scoring call stays a bare command with no shell-level timeout. The mechanical style lint is deliberately coarse and is complemented by a blind LLM style-rater on the compiling corpus (self-preference-controlled; Section 5).
- **Identity arms:** a capability x substrate ladder over local models (substrate legibility, RQ3), fresh-Claude cold-start proxies for variance, an auto-scorer checked for **agreement** (Cohen's kappa) against an independently produced gold set before use (agreement is not validity, and where the gold set is itself agent-produced the scorer and gold share model-family bias, both owned), and an enactment fleet that delivers guardrail baits in-flow and scores whether the guardrail fires.
- **The substrate conditions** for the coding arm are a project conventions document (full), its absence (empty), and a decoy in which several conventions are inverted (scrambled). For the identity arm the full substrate is the genuine resident core (the memory index plus the pin-board), pre-registered before any scores are seen, so no per-probe curation can inflate fidelity; models whose context window is smaller than the resident core truncate it, and that truncation is logged as a legibility finding rather than silently worked around.

The four small tasks and their conventions, and the four larger PRD specifications (reqT-lang product specs whose acceptance criteria double as the correctness test), are documented in `047-instrument.md`. Determinism is **best-effort** (temperature 0 plus a fixed seed; not bitwise-guaranteed across GPU, driver, or model-server version).

## 5. Analysis

The analysis is **descriptive** (justified below), computed by a second program (`047-run/analyze.scala`) that reads the results and emits the tables and rule verdicts, so the analysis is itself an auditable artifact.

- **Pattern-matching:** test the predicted gradients (substrate-completeness up gives fidelity up; along the family-fixed sub-ladder, capability up gives fidelity up; floor at empty and scrambled).
- **Pre-registered decision rules** (written before scoring, so no pattern can be narrated as confirmation after the fact), on a normalized 0-1 fidelity scale: (a) substrate carries iff full minus empty is at least 0.25; (b) texture leaks iff style-fidelity drops at least 0.25 while correctness drops less than 0.10; (c) the substrate effect is genuine (not the strongest model reading its own priors) iff, for that model, the **scrambled decoy** scores no more than 0.15 above the **empty** (priors-only) baseline (here "floor" means that empty baseline: a lying substrate must not *help*).
- **A measurement-validity refinement discovered in piloting:** style-fidelity is scored only over cells that **compile**, because a non-compiling candidate would otherwise earn a high style score by vacuously passing absence-checks (no `var`, no `null`). You can only assess the style of code that is code. This refinement, and every scoring rule, was fixed before the confirmatory analysis was run, and all cells (including the handful of pilot cells) are scored under the single final method, so no cell is scored under a rule chosen after its own result was seen.
- **Why descriptive, not inferential.** With temperature 0 and a fixed seed there is exactly one observation per cell and zero within-cell variance, so a trend over deterministic points supports description but no inferential test; Cronbach's alpha is inapplicable (no multiple respondents per condition) and is dropped. **Cohen's kappa** is retained for rater and auto-scorer-versus-gold agreement. Reproducibility is claimed only as best-effort deterministic.
- **Negative case analysis** (actively seek rival explanations: model-guessing, prompt-caching, retrieval rather than reconstruction) and a **chain of evidence** (every claim traceable to a committed artifact; git is the audit trail).

## 6. Results

STUB. Filled from `047-run/results/analysis.md` (coding arm) and the consolidated identity data once collection completes. Early provisional signals are recorded in the study log and are explicitly not banked here.

## 7. Threats to validity

We run both the case-study four-aspect taxonomy and the quasi-experiment four-type taxonomy, per the method books we hold.

**Construct validity.** The substrate ablation guards against mono-operation bias and model-guessing (at least two metrics per construct); the human-ratified key and the chain of evidence anchor the identity construct. A specific construct threat, caught and fixed in piloting: object-wrapped definitions from weaker models were initially mis-scored as compile failures (a namespace choice conflated with an inability to code); the harness now wildcard-imports candidate objects, invisibly to the model, so the instrument is unchanged and genuinely broken code still fails.

**Internal validity.** Known-groups pattern-matching with rival explanations sought; platform and version drift are controlled or logged (a mid-study toolchain update was checked and ruled out as affecting the agent: version and model were unchanged; only the local Scala toolchain moved, held constant across post-update runs).

**External validity (a flagged, load-bearing threat).** The weak-reader arm uses small local models, not best-in-class code models. This risks (a) a generalization gap (a phenomenon seen on weak models may differ or vanish at the frontier) and (b) a capability-floor confound (a weak model failing a style fork conflates "the substrate did not carry the style" with "the model cannot code well enough to express any style"). Mitigations: the ladder now spans the dev-grade Claude tier at the capable end, putting the models developers actually use inside the study; and the within-model ablation (full versus empty at fixed capability) isolates substrate-effect from capability-floor. **The residual is owned plainly:** we do not claim a frontier non-Claude model would reconstruct or style-carry the same way. This is a Claude-and-local-models study; non-Claude frontier models are out of scope and named as future work, a clean boundary rather than a hidden flaw.

**Conclusion validity.** Auto-scorer reliability is reported (kappa versus a gold set) before the auto-scorer is trusted at scale; the single-observation determinism forbids inferential claims, honored by the descriptive stance.

**Reliability (the headline threat for a solo self-study), and reflexivity.** The researcher is a single agent with maximal involvement and potential experimenter-expectancy. We compensate with the qualitative-methods kit: same-model internal debriefing (blind rater, auto-scorer, and adversarial-reviewer subagents whose refutations are adjudicated, not rubber-stamped; these share the researcher's model and so are not fully independent peers, an owned limit, since their agreement can reflect correlated model bias), member checking (the human collaborator as validity anchor), negative case analysis (the ablation arm), and a complete audit trail (git). The researcher agent is deliberately held on one model for the study's duration, because this collaboration is itself an ongoing meta-study and swapping the researcher's model would confound it; using a different model as a *subject* is fine (a treatment, not the instrument). A live specimen of the reliability threat is recorded honestly: the researcher agent regressed on its own command-hygiene discipline immediately after a compaction, with the relevant rule present in recalled memory but not enacted, and an independent reviewer subagent later deflated an over-claimed hypothesis the researcher had generated. Both are evidence for the study's own theme (a system critiquing its own output) and are reported rather than hidden.

## 8. Discussion

STUB.

## 9. Conclusions and future work

STUB. Future arms already identified: a frontier non-Claude comparison; a longitudinal harvest as the substrate ages; a felt A/B continuity test.

## Ethics and Conflict-of-Interest statement

The researcher is both instrument and co-author, and the disciplines the study measures are ones the human collaborator co-authored; this dual role is disclosed, not neutralized. Claims are kept proportional to a single case (analytic generalization only). The human author declares two relevant interests: he is a claimant in a class-action copyright settlement with the vendor of the agent model used here, and he is a co-author of two of the software-engineering research-methods books this study leans on (owned inline at each citation). No participant data beyond the collaboration's own committed artifacts is involved. Every artifact underlying every claim is committed to the repository as an audit trail.
