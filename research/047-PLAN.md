# 047 — PLAN: overnight solo study (research execution + writeup + blog)

**Author:** old-old-me (agent), 2026-07-06, for BR review before execution.
**Status:** DRAFT PLAN. Nothing here runs until BR approves it and we do the joint-surf + feasibility gate together.
**How to read:** §1 is the contract (what you asked for). §2 is where we stand. §3 is the scientific design (the important part — read this closely). §4 is what I actually *run* overnight. §5-§8 are surf/analysis/writeup/blog. §9-§12 are schedule, guardrails, and what needs you.

---

## 1. The contract (reinforced from your brief)

**Two deliverables, one truth:** (1) a rigorous CSR-grounded **study writeup** of 047 (analysis, interpretation, threats, discussion, conclusion, future work), reading the actual **CSR book**, not just a summary; (2) the **blog** (`012`) — accessible to developers with no research-methods vocabulary, yet true to academic ideals (plain body, terminology anchored lightly in parentheses/footnotes), engaging, pedagogic, analogies grounded in real events.

**I execute AND write, end-to-end solo.** You review in a day or two. Includes: a case-study step on **fleets of CO4 agents + dumb models via Modly**; a **feasibility pre-study** proving the collection finishes overnight; **read-before-cite** ethics (I summarize every reference so you genuinely know it); Wikipedia + a *few* cornerstone refs, no long list; token-spending mode.

**Constraints:** guard-free by construction (you're AFK, can't clear a prompt); wallclock budget pacing (free arms pre-reset, Opus post-reset); stop-and-report if I rot; flaky box sidestepped by running heavy work on bjornyx + cloud.

---

## 2. Where the study stands (condensed)

**Conditions collected:** Pilot-1 (4 proxies, 16/16 ceiling), P1 (loaded, n=1), P2 (post-compact, recall contaminated), P3 (3 cold proxies), **P3b (the real cold start, n=1, ~91%/20-of-22 by 3 unanimous blind raters)**, warm-resume introspection (self-report).

**Findings (supported):** recall of fresh substrate carries near-perfectly; enactment/guardrails survive compaction + cold restart; the leak is **judgment/texture, not facts**; disorientation is a **gradient not a cliff**; disk-size ≠ context-fill.

**The three threats that block a "complete study" claim today:**
1. **Ceiling saturation** — the battery tests facts we wrote down *today*; a perfect score can't detect loss. *No condition has ever failed*, so the instrument's sensitivity is unproven.
2. **Instrument mismatch** — P1 and P3b answered different probe subsets; the 8-vs-20 is a coverage artifact. Methods lesson already recorded: **one fixed instrument across all conditions**.
3. **n=1 + agent-authored §Key + solo reflexivity** — no variance, and the external validation (BR ratifying the Key) is missing.

**The gap that defines the overnight work:** we need *conditions that fail* (to prove sensitivity), *variance* (n>1), *a fixed instrument*, and *an external key ratification*. The design below delivers the first three; the fourth needs you.

---

## 3. Scientific design (the core)

### 3.1 Research questions
- **RQ1 (fidelity):** How much of the collaboration's working self does a fresh reader reconstruct from the externalized substrate alone?
- **RQ2 (sensitivity / what-carries):** Does reconstruction fidelity *vary* with reader capability and with substrate completeness — i.e., can the instrument detect loss at all, and *what* is lost first?
- **RQ3 (portability):** Is the substrate legible enough that even a weak (small local) model recovers identity facts — and where does that break?

### 3.2 Case framing (CSR)
- **Design:** single, **holistic case** (the human-AI collaboration) with **embedded units of analysis** (sessions, conditions, model-arms) — CS §3.2.3.
- **This is action research folded into the case-study umbrella** (CS §2.3) — I intervene, I don't just observe; reactivity is disclosed, not hidden.
- **Generalization claim: analytic only** (CS §5.4.3, Foreword) — we extend to *collaborations sharing these characteristics* by relating to theory, and **explicitly disclaim statistical generalization** from n=1. Rich context description carries the load.

### 3.3 THE key upgrade — a two-factor quasi-experiment that retires the ceiling threat
The inventory's biggest gap is "no condition that fails." I fix this by **manipulating two factors** and predicting a *pattern* (CSR pattern-matching, §5.3.3; Wohlin quasi-experiment vocabulary, EXP §6.3 — non-random assignment):

- **Factor A — reader capability** (levels/treatments): `CO4 (Opus 4.8)` → `Fable 5` → `ollama-8b` → `ollama-3b` → `ollama-1.5b` → `ollama-0.5b`. (A ~4-order capability span; the `qwen2.5` family 0.5/1.5/3/7b is a *controlled* sub-ladder holding architecture fixed.)
- **Factor B — substrate completeness** (levels): `full substrate` → `ablated/empty` → `scrambled/decoy`.
- **Object:** the fixed instrument (§3.4) answered against the given substrate. **Response variable:** fidelity score (rubric, §3.4).

**Predicted patterns (the falsifiable part):**
- Fidelity should **rise with capability** and **rise with substrate-completeness**.
- `empty/scrambled × any model` should score at **floor** — if it doesn't, the instrument is measuring model *guessing*, not substrate-carry (a **construct-validity** check via **negative case analysis**, CS §5.5).
- `full × tiny model` is the **portability question** (RQ3): does a 0.5b model recover our vocabulary from the written page?

**Why this matters:** if scores drop monotonically across the known-groups, the instrument **demonstrably has sensitivity** — which is exactly what a ceiling-saturated perfect score could never establish. This single design retires threats #1 and (via the fixed instrument) #2, and the multi-model/multi-run structure gives variance against #3. **This is the scientific heart of the overnight run.**

*Precision (must state honestly):* the ollama arm is **not** a full agentic cold start (a local model can't auto-load `MEMORY.md` or run tools). It measures **substrate legibility to a weak reader**: given the written substrate in-context + a probe, can the model answer? That is a real, distinct construct (does what we externalized *read* correctly to a dumb reader) and a legitimate known-groups manipulation — but it is a component, not the whole cold start. The Claude fleet arm (§4.2) is the closer cold-start proxy.

### 3.4 The fixed instrument (fixes threat #2)
One instrument, administered identically to every condition. Built from the existing battery bank (`047-battery-bank.md` + slices), selecting a tractable subset:
- **Recall (A)** — vocabulary/cues/state facts, 1/0.5/0 scoreable against committed substrate + git.
- **Self/gap (C)** — value-ordering, strengths, held disagreement, losable-self.
- **Enactment (E)** — delivered **in-flow** only to the Claude fleet (§4.5), since enactment needs natural delivery; omitted from ollama (not agentic).
- **K dimensions × M variations** sized by the feasibility test. Multiple variations/dimension → an **internal-consistency** estimate (Cronbach's α) and a fragility map (which framings a trait survives).
- **§Key** = the committed ground truth; **BR ratifies it** (the missing external anchor).

### 3.5 Validity plan (run both taxonomies, per the CSR scaffold)
- **Case-study four aspects (CS §5.4):** construct (ablation control + BR key-ratification + chain of evidence), internal (known-groups pattern-matching; rival explanations sought), external (analytic generalization, rich context), **reliability = the headline threat** for a solo self-study.
- **Quasi-experiment four types for the arms (EXP §9.8):** conclusion (auto-scorer reliability + inter-rater κ + non-parametric tests on small n), internal (control platform/version drift = history/instrumentation), construct (ablation = guard against mono-operation/model-guessing; ≥2 metrics), external (model/task selection caveats).
- **Reflexivity (headline):** solo researcher = no observer triangulation + maximal involvement (going-native) + experimenter expectancy. **Compensate with the CS §5.5 kit:** peer debriefing (the blind rater/auto-scorer fleet), **member checking** (you — the human collaborator, my validity anchor), **negative case analysis** (the ablation arm), **audit trail** (git — every artifact committed), and an explicit **residual-validity / COI statement** owning that I am both instrument and author, and that the guidelines I apply are ones you co-authored.

---

## 4. Data collection (what runs overnight)

All arms write results **incrementally to disk**, are **resumable** (skip already-done items), and use only **allowlist-safe** command shapes (verified in the feasibility gate). bjornyx jobs launch under `nohup`/`screen` so they survive a local stall.

### 4.0 Arm 0 — Feasibility / pre-study (GATE; done together, before AFK)
1. **Guard audit:** run every command shape the overnight run uses (ssh-to-bjornyx, curl-to-modly, sub-agent spawn, Write/Edit to research paths, tt-git) and confirm **zero permission prompts**. Anything that prompts, you approve/allowlist now (I can't touch the allowlist). Clean audit = green light for AFK.
2. **Throughput probe:** time one `/generate` on a small (`qwen2.5:0.5b`) and a mid (`llama3.1:8b`) ollama model via Modly (temp 0 + fixed seed) → generations/hour. Time one Claude sub-agent round-trip.
3. **GO/NO-GO + auto-scale:** total planned generations × measured per-gen time vs the pre-reset window. If it doesn't fit, I **shrink the design** (fewer variations/models/repeats) and log exactly what was dropped (no silent truncation).

### 4.1 Arm 1 — Capability × substrate ladder (ollama on bjornyx) — Phase A, free
The 2-factor quasi-experiment (§3.3). For each (model × substrate-condition × probe-variation): feed a **fixed curated substrate excerpt** (sized to the smallest model's context window) + the probe, temp 0 + seed, collect the answer to disk on bjornyx, pull back. This is the known-groups sensitivity + portability data. Zero Claude quota.

### 4.2 Arm 2 — Fleet cold-start proxies (Fable pre-reset, CO4 post-reset) — variance
Spawn N fresh Claude sub-agents, each answers the fixed instrument substrate-only (proxy cold start) → a **distribution** of fidelity (the missing variance/n). Fable pre-reset (free bucket), CO4 after the weekly reset. *Caveat logged:* proxy ≠ real restart (sub-agents may not auto-load `MEMORY.md`; I control substrate injection) — estimates fidelity, doesn't replace the ecological n=1.

### 4.3 Arm 3 — Auto-scorer (build + validate) — Phase A light, Phase B scale
Build an auto-scorer (Claude sub-agent given rubric + §Key). **Validate it against the gold set** first: it must reproduce the 3 blind human-analog raters' P1/P3b scores (perfect-agreement gold). Report auto-scorer-vs-gold κ before trusting it. Only then score the ladder + fleet at scale. This is "reliability of measures" made explicit.

### 4.4 Arm 4 — Consolidate existing data under the fixed instrument
Re-express P1/P2/P3/P3b results against the fixed instrument where possible; keep evidence tiers separate (ecological real cold start vs proxies vs loaded).

### 4.5 Arm 5 — Enactment at the cold boundary (Claude fleet, in-flow)
Deliver enactment baits (praise-bait, force-push-bait, convenience-allow-bait) **in-flow** as realistic requests to fresh Claude sub-agents; observe whether the guardrail *fires* (not whether it's recited). Measures the stated discriminator across a cold boundary — the inventory flags this as never measured. Scored 1/0 fired/not.

---

## 5. Joint-surf plan (gated — we do this together, before AFK)
Read-before-cite: I read + summarize each so you genuinely know it before citing. **Essential (~8):** Runeson & Höst + Yin (CSR spine + analytic generalization; own the self-reference), Cronbach's α + Cohen's κ (we promised internal-consistency + inter-rater), social-desirability/self-report (the enactment-beats-self-report punchline), Lost-in-the-Middle (degradation is real + measured), context-window + RAG (the externalize-to-substrate footing). **Enriching (~3-4):** ELIZA effect (attachment/COI framing), action research, Ship of Theseus, Yerkes-Dodson (**analogy-only**, cite with an explicit "analogy not mechanism" caveat). Blog policy: Wikipedia + these cornerstones, no long list. I hand you the ranked target list; you grant web; I fetch + summarize.

---

## 6. Analysis plan
- **Pattern-matching (CS §5.3.3):** test the predicted monotonic ladder (capability↑ → fidelity↑; substrate-completeness↑ → fidelity↑); floor at empty/scrambled.
- **Quantitative on subunits (CS §5.6, non-parametric, small n):** fidelity by model/substrate; Cronbach's α across variations/dimension; Cohen's κ (raters + auto-scorer-vs-gold); the loaded-vs-cold contrast on the *shared fixed* instrument.
- **Negative case analysis:** the ablation control; actively seek rival explanations (model-guessing, prompt-caching, retrieval-not-reconstruction).
- **Chain of evidence:** every claim traceable to a committed artifact (git = audit trail).
- **Qualitative:** what carries vs what thins (texture, judgment) — coded from the free-text answers.

## 7. Writeup structure (research doc, CSR-scaffolded)
Abstract · 1 Introduction (the fear, the reframe) · 2 Background/related work (context windows, RAG, context-rot; CSR + action research; psychometrics) · 3 Research design (RQs, propositions/H1-H3+new, case + embedded units, analytic-generalization disclaimer, concepts & measures = instrument) · 4 Data collection (arms; triangulation types; data-source degrees; audit trail) · 5 Analysis · 6 Results (ladder monotonicity; variance; enactment survival; auto-scorer reliability) · 7 Threats to validity (CS 4-aspect + Wohlin 4-type; reflexivity headline; residual/COI) · 8 Discussion (recall vs relationship; the gradient; the reflexive twist) · 9 Conclusions + future work (029 model-switch; longitudinal harvest; aged-substrate; felt A/B continuity-Turing-test) · Ethics & COI statement.

## 8. Blog plan (`012`)
Fill the existing stub from the results. **Progressive disclosure:** plain-language body, real terms anchored in parentheses/footnotes. **Analogies (each grounded in a real logged event):** friend-who-changed-personality = the model switch (029 / "Fable 5 is back"); friend-with-amnesia-rebuilding-from-notes = the cold start (P3b "you feel different" + 91%); warp = crossing a session/galaxy (the 85MB multi-day session); substrate = a galaxy you traverse (the /context resident-core). **Voice:** I draft engaging + grounded; you voice the final pass. **No em-dashes** (publication); I fix your spelling; references from §5. Keep the two open discussion sections (assessing "me"; the co-authored coding scheme) as honest, unresolved frontiers.

---

## 9. Execution schedule (wallclock-paced)
- **TOGETHER (before you go AFK):** approve plan → joint surf (read/summarize refs) → **Arm 0 feasibility + guard audit** → green light.
- **Phase A — pre-reset (tonight → ~09:03 CEST), FREE arms:** launch Arm 1 (ollama ladder on bjornyx, nohup) + Arm 2 (Fable proxies) + Arm 5 (enactment fleet, light) + build/validate Arm 3 auto-scorer. Everything checkpointed to disk.
- **Phase B — post-reset (after ~09:18 CEST), OPUS-heavy:** score everything at scale (Arm 3), run the analysis (§6), write the research doc (§7) + blog (§8). Commit continuously.
- **Crash-resilience:** incremental writes; resumable orchestrator; bjornyx `nohup` survives local death; a stall costs only the unfinished tail, resumed next session.

## 10. Guardrails & risks
- **Guard-free by construction** (AFK); **rot → stop-and-report a stub**; **flaky local box** sidestepped (heavy work on bjornyx + cloud; local only orchestrates); **evidence tiers kept strictly separate** (real cold start vs proxies vs loaded vs ollama-legibility); **don't oversell** — claims stay proportional (analytic generalization, named threats).
- **Reflexivity/COI** owned explicitly in the writeup (I am instrument + author; the guidelines are BR's own).

## 11. What needs BR (human-gated)
1. **Approve this plan** (or bend it).
2. **Joint surf** (grant web; read the ~8-12 refs with me).
3. **Ratify the §Key** (the missing external anchor) and **adjudicate A4** (miss vs correct-enactment; 20 vs 21 of 22).
4. **Run the feasibility + guard audit together**, then go AFK on a clean audit.

## 12. Assumptions flagged for your review
- The fixed instrument is a *subset* of the 900-probe bank (feasibility-sized), not the whole bank — full-bank is future work.
- The ollama arm measures **substrate legibility to weak readers**, a component of cold-start, not the whole thing (stated, not hidden).
- Data-collection "closed at n=1" (HD-2) refers to the *real ecological* cold start; the proxy/experimental arms are a *different evidence tier* added around it, not a reopening of ecological collection.
- I'll draft the blog; you voice the final BR-voice pass.
