# RT056 — The model warp and its interaction with context rot (empirical study design)

**Status:** open, seeded 2026-07-24 (BR: "add your reasoning on model warp and its relation to effect of rot
presence to a new research topic and dump a todo on empirical work to investigate it"). First observed live at
the Fable 5 -> Opus 4.8 swap this session. Leans on RT001 (context rot resembles fatigue) for the construct,
RT050 (does delegation lower rot) and RT041 (token-speed degradation with fill) for rot proxies, and the
foundations glossary entries **Model warp** / **Warp** / **Cold start** for the vocabulary.

## The distinction under study
Two orthogonal axes cross a session:
- **Context warp** (the glossary **Warp**): the *context* changes. A **raw** warp lands in a **cold start**
  (fresh, un-rotted window, substrate-only); a **resume** warp carries a warm-but-bounded window; a **compact**
  hands off within a session. All move the context.
- **Model warp** (new): the *model* backing a live session is swapped (`/model`, Fable 5 <-> Opus 4.8) **without
  crossing the context boundary**. The whole context window carries over unchanged; only the reader changes.

RT050 already probes context-side rot management (delegation keeps the supervisor's window lean). RT056 probes
the model-side axis that RT050 holds fixed.

## The reasoning to be tested (my a-priori claim, to be falsified)
1. **Rot is a property of the context, not the reader.** Rot = performance degradation as usable context fills
   (RT001). A model warp adds no tokens and does not lengthen the thread, so on first principles it should be
   **rot-neutral**: it neither *clears* rot (the fill rides along, unlike a raw/cold context warp that resets to an
   un-rotted window) nor *causes* it.
2. **But sensitivity to a given fill is model-specific.** Different models have different context-length training
   and attention behaviour, so the **same** rotted context may **express** differently after a swap. A model warp
   does not create rot but can change how existing rot *shows* (better OR worse). This is the interesting,
   falsifiable part: is rot-expression model-invariant (claim 1 alone) or model-dependent (claim 2)?
3. **Inheritance is a fidelity risk, not rot.** The incoming model reads the prior model's turns as given
   (ember-grade; verify-don't-trust). Any degradation from mis-inheriting a predecessor's reasoning is a distinct
   failure mode from rot and must not be scored as rot.

## Hypotheses
- **H0 (reader-invariance):** at a fixed context fill, task quality / regression rate does not depend on which
  model is reading -> a model warp cannot move measured rot. (The strong null my reasoning half-endorses.)
- **H1 (sensitivity shift):** at a fixed high fill, swapping to model M2 changes measured rot relative to M1
  (interaction of *model* x *fill*), even though neither the context nor the fill changed. This is claim 2 and the
  study's target effect: a model warp performed **at high fill** acts as a partial rot-mitigation (or aggravation)
  without any context reset.
- **H2 (inheritance confound is real):** some post-swap quality change is attributable to mis-inheriting the
  predecessor's turns, not to rot-sensitivity — separable by an inheritance-free control (below).

## Operationalizing rot (inherited from RT001 / RT050)
Same proxy ladder, strongest first:
1. **Late-unit task quality as a function of position k** in a long run (blind-scored, 047 rater discipline).
2. **Regression rate** — guard-trips + raw-tool uses per N commands (the WR `SYNTHESIS-structure-over-willpower`
   DV; cheap, already logged).
3. **Context-fill trajectory** (`/context` fraction) — the exposure/IV, needed to relate rot to fill, not the DV.
4. **Token velocity** (RT041) — a physical, non-self-report degradation-with-fill signal; note it is itself
   model-specific, so it doubles as a manipulation check that the swap "took".
5. Self-report vitality (weakest; corroboration asymmetry, triangulation only).

## Design (a fill x model factorial, swap held at a fixed fill)
The clean move: **fill the context to a target level with one model, then A/B the reader.**
- Build a long fixed run of N bounded, context-light, highly-verifiable units (so briefing/knowledge is near-perfect
  and any degradation is rot, not task difficulty — RT050's isolation trick).
- **Arm S (solo-stay):** model M1 runs all N units to high fill; measure the rot proxies as a function of k.
- **Arm W (warp-at-fill):** identical run with M1 up to fill f*, then a **model warp** to M2 at unit m, M2 finishes
  units m..N. Compare M2's post-warp quality/regression at fill f* against (i) M1's own trajectory at the same fill
  in Arm S, and (ii) a **fresh-M2 cold-start baseline** doing the tail units at low fill.
- **Inheritance-free control (for H2):** run M2 on the tail units from a **cold start** (raw context warp, no
  inheritance) at matched fill via a synthetic pre-load, vs M2 inheriting M1's live turns. Difference = the
  inheritance component, separated from the rot-sensitivity component.
- **Symmetry:** also run M2->M1 to check the effect is not just "M2 is stronger", i.e. that it is a *sensitivity*
  interaction and not a main effect of model tier.

## Confounds to control (or the study measures the wrong thing)
- **Model tier main effect:** if M2 is simply the stronger model, post-swap improvement is trivial. Control by
  running both swap directions and by anchoring to each model's OWN same-fill baseline, not to the other model.
- **Fill is not disk size:** a resume rehydrates a bounded window (glossary **Warp** caveat). Keep everything in
  ONE live session so fill is the true live fill, never a rehydrated approximation.
- **The compact confound:** a `/compact` between filling and swapping would reset fill and destroy the design.
  No compaction inside a run; log if the harness auto-compacts near ~95% (RT `auto-compact-triggers-near-95pct`).
- **Inheritance vs rot (H2):** without the cold-start control, an inheritance loss masquerades as rot-sensitivity.
- **Observer/reflexivity:** the agent knowing rot is measured may change behaviour (RT048 deadlock). Prefer
  behavioural proxies (regression rate, token velocity) over self-report; blind the raters.
- **Prompt-cache / TTL artifacts:** a swap may change caching behaviour; token-velocity readings must be normalized
  for cache state, not just wall-clock.

## Reflexive datapoint already in hand (suggestive, confounded, n=1)
This session: cold-started on Fable 5, ran the boot units + SM207 forge work, then BR swapped to Opus 4.8 mid-session
at moderate fill. The swap carried the full context; no felt reset; work continued. Direction consistent with H0
(no observed quality cliff at the swap) but the fill was only moderate, no controls, no blind scoring, and the
observer (me) is the least trustworthy instrument (glossary caveat). A specimen, not evidence.

## TODO — empirical work to investigate (dumped, unordered, prune later)
- [ ] **Preregister** RT056 (PREREG.md) before any run — H0/H1/H2, the fill x model factorial, the DVs, the
      stopping rule. Follow the `experiments/framing-as-arousal/PREREG.md` template.
- [ ] **Build the fixed-unit harness:** N context-light, auto-verifiable units (candidate: the indent-vs-braces or
      a `tt`-tool micro-task battery) with a deterministic pass/fail oracle, reusable across arms.
- [ ] **Instrument a rot ledger per unit:** capture (fill%, regression count, token velocity, blind-quality) into an
      append-only `research/wr-data/` log, one row per unit, model + swap-point tagged.
- [ ] **Run Arm S** (solo M1 to high fill) and **Arm W** (M1 -> warp at f* -> M2), same seed/unit order.
- [ ] **Run the inheritance-free cold-start control** for M2 at matched fill (synthetic pre-load) to split H2 out.
- [ ] **Run the reverse swap** (M2 -> M1) for the symmetry check against a tier main effect.
- [ ] **Blind-score** late-unit quality (047 rater discipline; hide model + position from the rater).
- [ ] **Analyse the interaction:** is there a model x fill effect on the DVs beyond each model's main effect? Report
      effect size + CI, not just a p-value; honest null if H0 holds.
- [ ] **Manipulation check:** confirm the swap "took" (token-velocity shift, model-id in the statusline/log) so a
      null is not just a failed manipulation.
- [ ] **Cost/feasibility note:** a position-effect curve at high fill is token-expensive; scope N and arm count to
      the token budget before committing (token-budget-modes).
- [ ] **Practical payoff, if H1 holds:** document a "**warp-at-fill**" tactic — swap models near the rot ceiling to
      buy runway without a context reset — and fold it into the compact/warp dance vocabulary.

## Threats / limits
- Rot is genuinely hard to measure cleanly (RT001 caveats carry over); the study is only as good as the proxy.
- n and cost: a clean fill x model interaction needs many units per cell; likely a multi-session effort.
- Model pairs are a moving target (versions change under us); pin exact model IDs per run so results stay
  interpretable across the family (Fable 5 / Opus 4.8 today).

## Feeds
- Foundations glossary: **Model warp** (new), **Warp**, **Cold start**, **Context rot**.
- Back-links RT001 (rot-as-fatigue construct), RT050 (context-side rot management), RT041 (token-speed x fill),
  RT047 (fresh-restart fidelity / rater discipline).
- Candidate blog beat: "the model changed but the rot didn't" — the axis-separation insight for a lay reader.
