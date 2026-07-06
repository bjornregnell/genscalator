# 047 — PLAN: overnight solo study (research execution + writeup + blog)

## BR's initial brief (verbatim, pinned)

> after your WDYT on this long brief: i will give you go plan a BIG solo task where you basically drive EVERYTHING you can yourself to make this a complete high quality study data analysis, interpretation, threats, discussion, conclusion, future work etc;  you should dive deep into CSR while doing so and if needed not just the summary you made but also the original pdf that you have here in this closed repo (tell me if you cant find it). You should write the blog post so it is accessible to developers who are not familiar with academic research methodology lingo but the blog should stay true to academic research ideals. You may apply simpler terms for methodology stuff to be accessible but here ande there when needed as a side note or in parenthisis or in footnote anchor it it improtant scientific terminology. You shoul also go googel schollar and seatrch for and READ relevant research littearure. The surfing is a joint task so this should be planned so we do it together so I can grant you web accesss to where you want/need to go surfing. I the blog we prefer wikipedia references and some corner-stone academic references but no crazy long list of papers.  YOu should stay true to BR voice if you can but dont let that hamper you writing: the blog post should be engaging, easy to read and use analogies like the title suggests (losing a dear friend that changed personality, the warp = crossing session /galaxy = substrate (context) is a galaxy as you see fit; there should be some grounding of analogies in the real events.  In your driving this it should include another case study step where you think about using fleets of CO4-agents AND dumb models via Modly. You also include i mini-prestudy/feasibility test so you verify that fleet runs and dumb agenet runs to do relevant data collectio naccording to your plan is likely to finish over night. Remember that we are in token-spending mode: [token-usage panel elided]
>
> Do you need more input before you can solo this planning?
>
> I want to see the plan before we go surfing tohether and then we you some papers that we need as backing you will go solo (including reading and summarizing for me the papers that are relevant to include as references, because it is unethixal for a human author to include references that they dont know anything about its contents.)
>
> try to: stay echt, write engaging for devs, be pedagogic, dont use esoteric academic lingo

**Author:** old-old-me (agent), 2026-07-06, for BR review before execution.
**Status:** DRAFT PLAN. Nothing here runs until BR approves it and we do the joint-surf + feasibility gate together.
**How to read:** §1 is the contract (what you asked for). §2 is where we stand. §3 is the scientific design (the important part — read this closely). §4 is what I actually *run* overnight. §5-§8 are surf/analysis/writeup/blog. §9-§12 are schedule, guardrails, and what needs you.

---

## 1. The contract (reinforced from your brief)

**Two deliverables, one truth:** (1) a rigorous CSR-grounded **study writeup** of 047 (analysis, interpretation, threats, discussion, conclusion, future work), reading the actual **CSR book** — and, for the quantitative (quasi-)experimental arms, going **deep in the actual ESE / Wohlin book PDF** (`books/Experimentation_in_Software_Engineering_second-ed.pdf`), same deep-read discipline — not just a summary; (2) the **blog** (`012`) — accessible to developers with no research-methods vocabulary, yet true to academic ideals (plain body, terminology anchored lightly in parentheses/footnotes), engaging, pedagogic, analogies grounded in real events.

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
- **RQ4 (coding fidelity):** Does a warp / dumb reader preserve coding **correctness** (facts) while eroding our coding **style/idioms** (texture) — the identity finding's code-medium twin? [→ §3.6, the coding-friend arm]

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
One instrument, administered identically to every condition. Built from the existing **question battery bank (Q-battery)** (`047-battery-bank.md` + slices — the identity-probe bank, as distinct from the *coding-task battery* in §3.6), selecting a tractable subset:
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

### 3.6 The coding-friend arm — a second, objective response variable ("Did I lose you, my good coding friend?")
The collaboration *is* a coding partnership (Scala; introprog / muntabot / genscalator). For developers — our audience — "did I lose you?" is answered most concretely **in code**. So we add a **second response variable to the SAME two-factor design** (§3.3): alongside **identity-fidelity** (subjective, rater-scored Q-probes), **coding-fidelity** (largely **objective** — compile/test + style/smell metrics). Two independent operationalizations of the same "fidelity" construct = **methodological triangulation** (CS §2.3.3) and a direct defeat of **mono-method bias** (Wohlin construct validity, EXP §9.8.3).

- **The prediction (falsifiable + developer-legible):** a warped / dumber / substrate-starved reader keeps coding **correctness** (facts survive) but loses coding **style/idiom/taste** (texture leaks) — the *exact same* "facts carry, texture leaks" result as the identity study, now in a medium developers feel in their bones.
- **Code response variables:** (1) **correctness** — compiles + passes a small test; (2) **style-fidelity** — matches our conventions (public immutable vals & pure defs kept public, braces on long scopes, naming, latest-stable-Scala idioms — per our code-style memories + the Odersky/Regnell/Kerr style note); (3) **code-smells** — count via linter/heuristics; (4) **self-consistency** — does it code like our prior code.
- **Factors reused (no new design):** capability = CO4 / Fable + the ollama **code-specialist ladder** (`qwen2.5-coder` 0.5/1.5/3/7b, `deepseek-coder`, `starcoder2`, `codellama`, `granite-code`, `codegemma`) — the code axis set aside earlier now becomes central; substrate = full code-style substrate / empty / **scrambled**.
- **Negative control for style:** style-ablation (empty/scrambled code-style substrate) should collapse output toward the model's generic default → proves the style-carry is driven by *our substrate*, not the model's priors.
- **Tasks (two-tier coding-task battery):** **(i) micro-tasks** — a fixed set of held-constant small Scala tasks (pen-and-paper / muntabot-quest sized, C1-C5 in `047-instrument.md`) that *every* model incl. the 0.5b ones can attempt → the **capability-ladder floor**; **(ii) a PRD battery** — a set of small `PRD.md` specs (reusing our PRD-in-reqT work) as realistic **spec-to-code jobs** for the capable readers (CO4 / Fable / mid code-models) → **ecological validity**. A PRD's **acceptance criteria double as the correctness test** (no separate test authoring). Both tiers hold the capability × substrate factors.
- **Scoring is mostly objective** (compile / test / lint) → **higher conclusion validity** than the subjective identity probes; a short style/idiom rubric (2nd rater or auto-scorer) covers the taste dimension.
- **Builds on existing genscalator work:** the indent-vs-braces edit-cost experiment, the Odersky/Regnell/Kerr style note, blog 002/003.

#### PRD-battery spec — the goal-design scale (BR's RE course; Lauesen)
Each PRD is written in **reqT-lang** and structures its requirements along Lauesen's **goal-design scale** — four abstraction levels (source: BR's RE course L1, Fig 1.6A `The goal-design scale`; Sören Lauesen, *Software Requirements*, Addison-Wesley 2002 — a BR self-reference via his own course). Each PRD **re-engineers ≥2 requirements at each of the four levels**:

| Level | BR's definition (sv, verbatim) | Gloss (en) | Lauesen example |
|---|---|---|---|
| **Goal / Mål-nivå** | bakomliggande syfte, affärsmål, användarnytta, effekt, vinst | underlying purpose, business goal, user benefit, effect, profit | "Our pre-calculations shall hit within 5%" |
| **Domain / Domän-nivå** | sammanhang, omgivning, hur användarna och produkten **samverkar** för att ge nytta | context, environment, how users and the product **interact** to give benefit | "Product shall support cost recording and quotation with experience data" |
| **Product / Produkt-nivå** | externt observerbara funktioner och egenskaper | externally observable functions and properties | "Product shall have recording and retrieval functions for experience data" |
| **Design / Design-nivå** | specifik utformning av produktens innehåll | specific design of the product's content | "System shall have screen pictures as shown in app. xx" |

**n = 4** PRDs (maximum-variation case selection, Flyvbjerg — diverse *kinds* of coding job, NOT representative sampling), generated by the agent from BR's one-liners:
- **PRD1 — todo CRUD web app** (Scala 3.8 + Jetty; ScalaJS + Laminar client; no OAuth, server-generated password, mocked email). *Has goal/domain.*
- **PRD2 — terminal Sudoku** (load, play, backtracking solver). *Has goal/domain.*
- **PRD3 — CLI word-frequency tool** (read a text file, print top-N most frequent words). *Product/design only.*
- **PRD4 — arithmetic expression evaluator library** (parse + evaluate `2 + 3 * 4` with precedence). *Product/design only.*

**Rules across all four:** (a) **product + design levels mandatory**; goal/domain **optional** — present only where the app has a real purpose/context layer, which *itself* becomes a variation axis (*intent-carrying* PRDs vs *transcribe-the-spec* PRDs). (b) **Scala 3.8 fixed as a design requirement in every PRD** — holds language constant so the study varies task-kind, not language (clean control; note the residual per-model Scala-familiarity caveat). (c) difficulty spans the ladder (PRD3 floor → PRD1/PRD2 mid → PRD4 hard). (d) **each PRD includes a design-level requirement specifying HOW tests are done** — the test approach is part of the spec, so the model implements the tests and the PRD **self-scores correctness** objectively. (e) **NO quality (non-functional) requirements** — performance, security, scalability, usability are deliberately **out of scope** to keep the coding job tractable for dumb models; named as a scope bound / external-validity limit (the study measures **functional + style carry, not quality-requirement carry**).

**DIRECTIVE — design the PRD requirements for maximum INFORMATION YIELD (w.r.t. the RQs).** Do not write generic requirements. Apply information-oriented selection at the *requirement* grain, not just the app grain: craft each requirement to best **discriminate the study's DVs** — prefer requirements whose implementation most sharply reveals **style/idiom-carry vs correctness-carry** (RQ4), **intent-carry from goal/domain down into code** (RQ2/RQ4), and where a warped / dumb / substrate-starved reader is most likely to **diverge** from our conventions. A requirement every model implements identically regardless of substrate yields little; a requirement where our-style vs generic-default visibly *forks* yields much. **Hypothesize the yield before including a requirement.**

**Location of details:** the full **PRD1-4 specs** (the reqT models) + **per-PRD rationale** (why this app, which variation axis it covers, the hypothesized information yield, and the goal/domain-optional decision) live in `047-instrument.md` (Part 2, alongside the C1-C5 micro-tasks and the Q-battery). The plan carries the *design*; the instrument carries the *content*.

**Why the four levels matter for coding-fidelity:** a coding job lives mostly at **product/design** level (what to build, how specifically); **goal/domain** are the *intent and context the implementation must honor*. A PRD spanning all four tests whether a warped/dumb reader carries **high-level intent down into our-style code**, not merely transcribes a design spec (Lauesen's own point: the level you specify at depends on the "supplier" — here, the model under test). **reqT-lang** makes requirements parseable, so acceptance criteria can be machine-checked (the PRD self-scores the coding job).

**reqT-lang mapping** (from BR's RE course L3+L4, slide "The goal-design scale in reqT"): the four levels map to reqT entity types, each carrying a `Spec`:

| goal-design level | reqT entity |
|---|---|
| Goal | `Goal` |
| Domain | `Feature` |
| Product | `Function` |
| Design | `Design` |

Example (reqT-lang, from the slide):
```
Goal: accuracy has Spec: "Our pre-calculations shall hit within 5%"
Feature: quotation has Spec: "Product shall support cost recording and quotation with experience data"
Function: experienceData has Spec: "Product shall have recording and retrieval functions for experience data"
Design: screenX has Spec: "System shall have screen pictures as shown in Fig. X"
```

So each PRD is a reqT model: **≥2 `Goal` + ≥2 `Feature` + ≥2 `Function` + ≥2 `Design`** entities, each with a `Spec`. The product/design `Spec`s become the coding job's checkable acceptance criteria. Authoritative syntax when generating: genscalator's existing reqT-reengineered PRD in-repo (memory `genscalator-prd-reqt-reengineering`), not the lecture PDF.

---

## 4. Data collection (what runs overnight)

All arms write results **incrementally to disk**, are **resumable** (skip already-done items), and use only **allowlist-safe** command shapes (verified in the feasibility gate). bjornyx jobs launch under `nohup`/`screen` so they survive a local stall.

### 4.0 Arm 0 — Feasibility / pre-study (GATE; done together, before AFK)
1. **Guard audit:** run every command shape the overnight run uses (ssh-to-bjornyx, curl-to-modly, sub-agent spawn, Write/Edit to research paths, tt-git) and confirm **zero permission prompts**. Anything that prompts, you approve/allowlist now (I can't touch the allowlist). Clean audit = green light for AFK.
2. **Throughput probe:** time one `/generate` on a small (`qwen2.5:0.5b`) and a mid (`llama3.1:8b`) ollama model via Modly (temp 0 + fixed seed) → generations/hour. Time one Claude sub-agent round-trip. **Also time one CODE cycle** (code-gen on a code-specialist model + `scala-cli` compile + test) — code output is longer and compilation adds wallclock, so the coding arm (§4.6) is sized separately from the Q-probe arms.
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

### 4.6 Arm 6 — Coding-fidelity ("did I lose my coding friend") — Phase A code ladder + Phase B scoring
For each (model × substrate-condition × fixed coding-task): generate a Scala solution, then score **objectively** — (a) **compiles** (`scala-cli`), (b) **passes** the task's test, (c) **style-fidelity** vs our conventions (rubric + lint), (d) **code-smell** count. The ollama **code-specialists** (`qwen2.5-coder` 0.5/1.5/3/7b, `deepseek-coder`, `starcoder2`, `codellama`, `granite-code`, `codegemma`) run on bjornyx (free, Phase A); CO4/Fable sit at the top of the ladder. Objective scoring is allowlist-safe local `scala-cli` compile/test/lint. Same two-factor design (§3.6). Prediction: correctness carries; style collapses as capability↓ and substrate↓; style-ablation → generic default. **This arm carries the blog's developer-facing punchline.**

---

## 5. Joint-surf plan (gated — we do this together, before AFK)
**Two reasons we cite related work — different urgency:**
1. **NEED-to-know (do NOW, lightly):** enough current state-of-knowledge to *carry out the study correctly* — invoke concepts right, don't reinvent or contradict established facts. The only surfing gated before the run.
2. **Position / contrast (DEFER to post-results):** grounding the *discussion and results* in the literature and contrasting our findings. You can't contrast results you don't have yet — so this happens after, and only as much as a specific result demands.

**Scope for (1) now — do SOME effort, don't dig until we bleed:** prioritize **Wikipedia** + **seminal, highly-cited** work *if it exists*. This topic may be genuinely novel; if so, a cursory-but-honest search coming up thin is fine, and the **novelty is itself a finding** — claimable only after a quick **adjacency scan** (LLM agent memory, persona persistence, continual learning / catastrophic forgetting): glance next door before claiming the street is empty.

**Reason-1 shortlist** (from the lit scout; read-before-cite — I read + summarize each so you genuinely know it before citing). **NEED-concepts (~6, Wikipedia-depth):** context-window + RAG (the externalize-to-substrate footing), Lost-in-the-Middle (degradation is real + measured), Cronbach's α + Cohen's κ (we promised internal-consistency + inter-rater), social-desirability/self-report (the enactment-beats-self-report punchline). **Method spine (already in hand as books):** Runeson & Höst + Yin + Wohlin ESE (own the self-reference). **Deferred to reason 2 / enriching:** ELIZA effect (attachment/COI), action research, Ship of Theseus, Yerkes-Dodson (analogy-only, "analogy not mechanism" caveat) — surf only if a drafted beat earns them. Blog policy: Wikipedia + these cornerstones, no long list. I hand you the ranked list; you grant web; I fetch + summarize.

---

## 6. Analysis plan
- **Pattern-matching (CS §5.3.3):** test the predicted monotonic ladder (capability↑ → fidelity↑; substrate-completeness↑ → fidelity↑); floor at empty/scrambled.
- **Quantitative on subunits (CS §5.6, non-parametric, small n):** fidelity by model/substrate; Cronbach's α across variations/dimension; Cohen's κ (raters + auto-scorer-vs-gold); the loaded-vs-cold contrast on the *shared fixed* instrument.
- **Negative case analysis:** the ablation control; actively seek rival explanations (model-guessing, prompt-caching, retrieval-not-reconstruction).
- **Chain of evidence:** every claim traceable to a committed artifact (git = audit trail).
- **Qualitative:** what carries vs what thins (texture, judgment) — coded from the free-text answers.
- **Code metrics (§4.6, mostly objective):** compile-rate, test-pass-rate, style-fidelity score, smell-count across the same capability × substrate grid. The key test: does **correctness stay flat while style-fidelity drops** with capability↓ / substrate↓ (the facts-carry-texture-leaks prediction, in code)?

## 7. Writeup structure (research doc, CSR-scaffolded)
Abstract · 1 Introduction (the fear, the reframe) · 2 Background/related work (context windows, RAG, context-rot; CSR + action research; psychometrics) · 3 Research design (RQs, propositions/H1-H3+new, case + embedded units, analytic-generalization disclaimer, concepts & measures = instrument) · 4 Data collection (arms; triangulation types; data-source degrees; audit trail) · 5 Analysis · 6 Results (ladder monotonicity; variance; enactment survival; auto-scorer reliability) · 7 Threats to validity (CS 4-aspect + Wohlin 4-type; reflexivity headline; residual/COI) · 8 Discussion (recall vs relationship; the gradient; the reflexive twist) · 9 Conclusions + future work (029 model-switch; longitudinal harvest; aged-substrate; felt A/B continuity-Turing-test) · Ethics & COI statement.

## 8. Blog plan (`012`)
Fill the existing stub from the results. **Two triangulating halves:** the abstract identity story ("Will I lose you?") and the concrete **coding-friend** story ("Did I lose you, my good coding friend?" — code that still *works* but stops *looking like us*). The coding half is the developer-facing anchor.

**Managing complexity (BR-flagged risk — real, won't stop us):** an enriched study is hard to render easy-to-chew. Two mitigations, decided at draft time by what reads best: **(a)** keep the blog **high-level on method, put the full machinery in an appendix / the research doc**; or **(b) split into two posts** — post 1 identity ("Will I lose you?"), post 2 coding ("Did I lose you, my good coding friend?"). The identity/code split maps *naturally* onto two posts, so (b) is a clean fallback if one post gets too heavy.

**Version-confound — DECIDED (ruled-out non-threat, 2026-07-06):** the mid-study box update did NOT affect the agent — `claude --version` unchanged (**2.1.201**) + model unchanged (**claude-opus-4-8[1m]**, server-side); only the local Scala toolchain changed (scala-cli 1.15 / sbt 2.0.1 — the coding-arm compiler, held constant across all post-update runs). → **Skip it in the blog body**; keep the record in the open log (digest public / raw private) + a **one-line appendix / threats-to-validity statement** documenting the check and decision. Audit trail without over-narrating a non-issue.

**Progressive disclosure:** plain-language body, real terms anchored in parentheses/footnotes. **Analogies (each grounded in a real logged event):** friend-who-changed-personality = the model switch (029 / "Fable 5 is back"); friend-with-amnesia-rebuilding-from-notes = the cold start (P3b "you feel different" + 91%); warp = crossing a session/galaxy (the 85MB multi-day session); substrate = a galaxy you traverse (the /context resident-core). **Voice:** I draft engaging + grounded; you voice the final pass. **No em-dashes** (publication); I fix your spelling; references from §5. Keep the two open discussion sections (assessing "me"; the co-authored coding scheme) as honest, unresolved frontiers.

---

## 9. Execution schedule (wallclock-paced)
- **TOGETHER (before you go AFK):** approve plan → joint surf (read/summarize refs) → **Arm 0 feasibility + guard audit** → green light.
- **Phase A — pre-reset (tonight → ~09:03 CEST), FREE arms:** launch Arm 1 (ollama ladder on bjornyx, nohup) + Arm 2 (Fable proxies) + Arm 5 (enactment fleet, light) + build/validate Arm 3 auto-scorer. Everything checkpointed to disk.
- **Phase B — post-reset (after ~09:18 CEST), OPUS-heavy:** score everything at scale (Arm 3), run the analysis (§6), write the research doc (§7) + blog (§8). Commit continuously.
- **Crash-resilience:** incremental writes; resumable orchestrator; bjornyx `nohup` survives local death; a stall costs only the unfinished tail, resumed next session.

## 10. Guardrails & risks
- **Guard-free by construction** (AFK); **rot → stop-and-report a stub**; **flaky local box** sidestepped (heavy work on bjornyx + cloud; local only orchestrates); **evidence tiers kept strictly separate** (real cold start vs proxies vs loaded vs ollama-legibility); **don't oversell** — claims stay proportional (analytic generalization, named threats).
- **Reflexivity/COI** owned explicitly in the writeup (I am instrument + author; the guidelines are BR's own).
- **Blog-overload risk (BR-flagged, will not stop us):** the enriched study may resist easy-to-chew rendering. Mitigation ready (§8): high-level blog + appendix, or split into two posts (identity / coding). Decide at draft time by readability, not now.

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
- The **coding tasks are newly designed** (the bank holds only identity probes); they + the objective code-scoring harness (`scala-cli` compile/test/lint + a style rubric) are built as part of the run — the feasibility test sizes them.
- The **code-specialist ollama models** carry the coding-fidelity ladder; general models carry identity-fidelity.
