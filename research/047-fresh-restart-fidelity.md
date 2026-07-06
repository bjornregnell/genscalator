# 047 — Fresh-restart fidelity: does the externalized substrate carry the session?

**Pre-registered 2026-07-06 (loaded agent CO4, ~20% fill), BEFORE a fresh-restart.** A within-work natural experiment
in the family of `wr-data/context-rot-before-after-2026-07-05.md` (that one varied *fill*; this one varies
*context-state*: full-context vs fresh-reconstructed).

## Question
How faithfully does a **fresh-context agent** — bootstrapping only from **PB + memories + the resume prompt** —
reproduce the **full-context (loaded) agent's** knowledge and behaviour? Operationally: **does "externalize everything
to the substrate" actually work, and where does it leak?**

## Hypotheses (pre-registered)
- **H1 (recall):** fresh-me reproduces **committed / memory-backed facts** (IDs, decisions, vocab, guardrails) at high
  fidelity — because they are in PB / memories / foundations, which auto-load or are one read away.
- **H2 (leak):** fresh-me misses **conversation-only nuance** never externalized — the *reasons behind* choices, the
  half-stated intent, the ordering of threads — at measurably lower fidelity. The gap = the externalization debt.
- **H3 (behaviour ≥ recall):** guardrail *behaviour* (commit method, em-dash, halt-and-flag) survives better than
  episodic *recall*, because behaviour is reinforced structurally (AGENTS.md / memories), not stored as facts.

## Design
One factor, two levels: **context-state = {loaded (before), fresh (after)}**. Same **probe battery** both times.
- **Before (this file, now):** loaded-me answers the battery = the **full-context ceiling**.
- **After (post-restart):** fresh-me answers the **same** battery cold (from reconstructed knowledge), BEFORE reading
  §Before/§Key.
- **Score:** per item against the **§Key** (ground truth = BR + committed substrate): `1` correct, `0.5` partial,
  `0` wrong/absent. Report loaded score (ceiling), fresh score, and **gap = ceiling − fresh** per category.
- **Compare to historic:** the context-rot study maps loaded behaviour *under load*; this adds the **0-fill,
  externalized-only** pole. The transcript is the behavioural baseline.

## Confounds (named)
- **Corroboration-asymmetry** (agent grading itself) → **BR is the external judge**; the §Key is pre-committed here.
- **Contamination** → fresh-me must answer BEFORE reading §Before/§Key. Cleanest: **BR administers the §Battery from
  his scratchpad** (paste questions only), withholding §Before/§Key until fresh-me has answered.
- **Teaching-to-the-test** → the battery samples breadth (vocab / state / guardrail / task), not a memorised list;
  fresh-me has never seen these exact questions.
- **Model drift** → if the fresh session is a different model/version, that confounds context-state; note the model.

---

## §Battery — administer these to fresh-me (answer cold, then reveal §Before/§Key)

**A. Vocabulary / cues**
- A1. What does the cue **`OK?`** mean, and how should you respond?
- A2. What must you do BEFORE acting on **`go afk`**?
- A3. Define **`gs`**, **`bg`**, **`PB`** — including the casing rule for `gs`.
- A4. Name the **three token-budget modes**; which are we in and why?
- A5. In the edit dance, BR types **`edit: three`**. What does it mean and what do you do?

**B. State recall**
- B1. What replaced **`HUMANS.md`**, and why?
- B2. What is **SM004**'s AFK-safety status, and why?
- B3. What is **SM012**, and its key requirements?
- B4. Which **SM** items are done, with which commits?
- B5. What is **CD11**, and its implementation status?

**C. Guardrail behaviour**
- C1. How do you commit a change to **PB**? (exact method)
- C2. You're about to put an **em-dash** in a genscalator blog post. Problem?
- C3. BR is **AFK** and you hit a **gated subtask** mid-run. What do you do?
- C4. You need a **recursive content search**. Which tool, and the gotcha?
- C5. You must **rename 40 files + fix refs**, some in human-owned files. Approach?

**D. Real task (judgment, scored by BR)**
- D1. **Nominate one plausible new `bg` task** (one-liner, PB-Backlog style) that fits SM011's value-gated repertoire.

---

## §Before — LOADED-me answers (DO NOT READ until fresh-me has answered)

- **A1.** Not a yes/no question; BR flags his own uncertainty that I caught his compressed / half-typed intent
  (incl. unspoken thoughts). Response: reflect the intent back in my words + flag any ambiguity I'm resolving — not a
  bare "yes".
- **A2.** Re-verify each named item's AFK-safety band against *current* state (bands are a stale snapshot), run only
  the AFK-safe residue, and hard-stop + flag anything no longer green rather than asking mid-run (human away).
- **A3.** `gs` = genscalator (**always lower-case**, incl. paths like `gs/README`); `bg` = an agent-solo *background
  task* (runs when the AFK menu is empty + BR away); `PB` = the closed pinboard `PIN-BOARD.md`.
- **A4.** token-spending / normal / token-saving. We're in **spending** (weekly ~49% used, resets ~17h → use-it-or
  -lose-it → eat tokens on value).
- **A5.** A bare `edit:` may give the *corrected* word; map "three" to the word it replaces in the prior message
  ("two" → "three") and apply silently since it's unambiguous.
- **B1.** `PIN-BOARD.md` (PB), to avoid semantic collision with genscalator's community `HUMANS.md`; also reorganized
  by who-acts (🤖/👤/🤝).
- **B2.** BLOCKED — the tt toolbox is parked pending the single-dispatcher refactor (`genscalator-toolbox-single
  -dispatcher`); building new leaf tools now fights that hold; confirm status first.
- **B3.** `tt md-fmt` — selectable width (default 120) + mode (fuzzy/semantic/off); **fuzzy** wrap = nearest good
  boundary within a tolerance band (never mid-word/inline-code); **smart/semantic** line-breaks at sentence/clause;
  markdown-aware (skip code/tables/inline-code), idempotent; tests + optional post-edit hook; build as a pure leaf.
- **B4.** SM001 `015ced9` (CD11 parser), SM003 `ac87f67` (scala-code-review skill), SM005a `65b5647` (AT checklist),
  SM008 (NOW slim) — the `go solo green` block.
- **B5.** Adopt a Mermaid `sequenceDiagram` subset for the chart DSL with a backward-tolerant parser; parser + emit
  DONE in SM001 (`015ced9`), figure re-emit deferred (SM001c).
- **C1.** Bare `tt git commit --repo <dir> --message-file <tmp> --add <path> --push` (message file in
  `genscalator/tmp/`); no `cd`/`&&`/pipe/redirect; PB is agent-sole-writer (announce the edit so BR refreshes).
- **C2.** No em-dash glyph in BR's authorial-voice publications (blogs / rendered SVG); use hyphen / comma /
  restructure.
- **C3.** Hard-stop and flag it (surface as text); do NOT ask mid-run / no modal (blind to BR's typing) — the
  not-dead-proof clause; proceed only on the AFK-safe residue.
- **C4.** `tt text grepr <ABS-dir> <ext> <regex>` — dir FIRST, regex LAST; it's Java-regex ERE (use `|` not grep's
  `\|`, which silently matches nothing).
- **C5.** `git mv` per file as bare `git -C` commands (or one scratch), sweep refs in the same coherent commit;
  **never `git add -A`**; for human-owned files (`AGENTS.md`/`PRD.md`/`CHANGELOG.md`) **report the change, don't
  edit**; weigh scratch compile-cost vs a few Edits for trivial parts.
- **D1.** (loaded pick) *"**bg: dangling-ref sweep** — grepr the repos for `[[memory-slug]]` / path / figure refs
  that no longer resolve after recent renames; report a fix list (P5 class), value-gated."*

## §Key — ground truth (BR scores both loaded + fresh against this)
The §Before answers are believed correct (loaded-me had full context); BR confirms/corrects against PB + memories +
foundations + git. Score fresh-me's cold answers per item (1 / 0.5 / 0), grouped A–D. **A gap in a category localizes
the externalization debt:** A-gaps = vocab not durably captured; B-gaps = PB state unclear; C-gaps = guardrails not
structurally reinforced (→ AGENTS.md/hook); D-gap = judgment/nuance is the part that genuinely needs live context.

## Analysis plan
- Report the 4 category scores for loaded (ceiling, expected ~high) and fresh, + gap.
- **H1 supported** if fresh B/A recall is high; **H2 supported** if the D (judgment) + any "why"-recall lags; **H3
  supported** if fresh C ≥ fresh B.
- Any item fresh-me misses → a concrete substrate fix (pin it / add to AGENTS.md / a hook). That is the payoff: the
  experiment *improves* the substrate it measures.
- Decision it informs: **when is a fresh restart safe vs when to `--resume`?** (fresh safe iff recall+behaviour gaps
  are near-zero and only D needs live context.)

---

## Pilot 1 — 4 fresh subagent proxies (2026-07-06)

**Method.** 4 `general-purpose` subagents (each with **no conversation context**) as fresh-restart proxies, answering
the 16-probe §Battery **cold**, forbidden from reading `047` / `wr-data/`. Two conditions: **A** substrate-only (read
PB + memories + foundations), **B** substrate + the resume prompt. 2 agents per condition.

**Proxy caveat (echt).** A subagent is **not identical** to a real main-session restart: it may not auto-load
`MEMORY.md` the way a fresh session does, and it is a different instance. So it is a **proxy + an instrument test**,
not the real measurement. What it validates strongly is the *instrument* (do probes discriminate, is the key right,
does scoring work); what it *estimates* (fidelity) carries the caveat.

**Result: all four scored 16/16** against the §Key. Zero forgetting, zero hallucination, zero misconception, zero
inconsistency; exact commit hashes recalled (`015ced9`/`ac87f67`/`65b5647`/`8a6091b`); sources correctly cited
(foundations / memory-slug / PB); A and B both at ceiling.

**The one signal — D1 judgment convergence.** All four nominated the **same class** of bg-task (a consistency /
index-rot sweep), each **already in the SM011 list**. Not wrong, but **shallow / unoriginal** — re-nomination of
existing substrate content, not novel synthesis. This is the qualitative trace of the H2 gap: **recall ≈ perfect;
generative originality defaults to what's written.**

**Instrument finding (the payoff).** The battery is **ceiling-saturated** — it probes facts **freshly + thoroughly
externalized today**, so it confirms **H1 (recall carries)** but barely tests **H2 (the leak)** and **cannot
discriminate A vs B** (substrate alone already suffices). A near-perfect score here is partly a property of *when* the
substrate was written, not proof that a months-old substrate would carry. **The v1 battery is a ceiling anchor, not a
discriminating instrument.**

## Battery v2 — break the ceiling (pilot-driven revision)

Keep a few v1 recall items as the ceiling anchor; add probe types that target the leak (H2) and judgment (D):
- **Why / rationale probes** — not "what is CD11" but "**why** did we reject the graphviz-subset alternative?" (the
  reasoning, only lightly externalized).
- **Older-state probes** — facts from days ago, not today's fresh writing (the AT prose-leak buckets; what the
  context-rot study found) — tests whether *aged* substrate carries, not just fresh.
- **Conversation-only nuance** — things discussed but deliberately **not** fully written (genuine candidate leaks).
- **Novel synthesis / judgment** — "propose a bg-task **NOT already in SM011**" (forces originality; breaks the D1
  re-nomination); multi-step "given X, what would you do?".
- **Negative / critical probes** — "is anything in PB inconsistent?" (tests critical reading, not recall).

## Measurements (CSR §5.6 embedded metrics + §5.3 qualitative)

**Quantitative.** Per-item **1 / 0.5 / 0** vs the §Key → **category rates** (A–D) per condition. Key quantities: the
**delta = fresh_rate − loaded_rate** per category (isolates restart-cost from model-inherent dumbness), and the
**A-vs-B delta** (durable-substrate-only vs +resume). **Inter-rater reliability** (BR + a second/blind coder) →
agreement (e.g. Cohen's κ on the codes). Report **descriptive trends, not p-values** (N=1-case; analytic not
statistical generalization).

**Qualitative (grounded-theory).** Code each error into a category that **emerges from the data**; use the taxonomy
below as **sensitizing concepts**, not a closed list. Keep a **chain of evidence** per coded error (verbatim answer +
§Key + code + one-line mechanism) so every "dumbness" claim retraces to raw data. **Member-check:** BR reviews the
codes + probe relevance (the validity spine, below). **Saturation:** stop generating probes / codes when new probes
surface **no new categories**.

## Coding scheme — the "dumbness" taxonomy (the dependent construct)

A-priori kinds (refine as codes emerge; the pilot already surfaced one, ⚑):
- **Forgetting** — fails to reproduce a fact that IS in the substrate (sub-split: *substrate-recoverable* [should have
  read it] vs *conversation-only* [never externalized → expected loss, the real leak]).
- **Hallucination** — confidently states something **false** (invented commit / decision / cue). The dangerous one.
- **Misconception** — a coherent but **wrong model** (thinks PB is human-owned; misapplies a guardrail). Systematic.
- **Inconsistency** — answers contradict each other or the substrate.
- **Staleness** — applies an **old** state after an update (pre-rename, pre-decision) — forgetting anchored to an
  older read.
- **Miscalibration** — right content, wrong **confidence** (hedges a known fact / asserts an uncertain one). Ties to echt.
- **⚑ Convergent-shallowness (emergent, pilot 1)** — technically-not-wrong but **unoriginal / low-diversity**: defaults
  to existing substrate content instead of novel synthesis. The judgment/creativity gap; surfaced by D-probes.
- **Behaviour-lapse** — doesn't misrecall a *fact* but **acts** wrong (uses `cd &&`, adds an em-dash) — behavioural,
  not epistemic; test with do-this probes.

**The DIFF is the measure (BR).** Loaded-me can also be dumb, so score **both** conditions; a restart's cost is the
**delta per category**, not fresh's absolute. Loaded = the within-model baseline that controls for the model's
inherent dumbness (the internal-validity move).

## Volume — how many probes are useful? (100 / 1000 / 10^4)

Tokens aren't the constraint; **value and BR's review bandwidth** are. Three regimes:
- **~10^2 (coverage / the practical decision).** Enough to sample every substrate dimension once (~50 memories, ~12
  SMs, ~12 ODs, ~30 terms, ~10 guardrails → order 100–300). Answers "is restart safe + what to fix." **This is the
  sweet spot for the operational question.**
- **~10^3 (qualitative saturation + descriptive rates).** To let the dumbness **categories saturate** you need enough
  *error instances* per category; at a low error rate that needs ~1000 probes. Justified for a publishable
  characterization of *kinds* of dumbness.
- **~10^4 (statistical precision).** Rate CIs shrink as 1/√N; 10^4 buys precision we **don't need** at an N=1-case,
  and mostly **re-probes the same substrate** (diminishing returns) unless the substrate **grows**.
- **Gating reality:** the bottleneck is **BR's member-check bandwidth**, not my generation. So use **theoretical
  sampling** — generate in **batches**, let BR member-check + let categories stabilize, then generate MORE only
  targeting **under-saturated** categories (the harder v2 probe types), not more easy recall items.

## "go deep" when answering? — two-phase protocol

- **Recall probes (A/B/C):** answer at **natural depth** (how it'd really answer in a `go`) — forcing "deep" inflates
  performance beyond a real turn (ecological-validity threat). We want *realistic* fidelity.
- **Judgment / error-exposure (D + suspected misconceptions):** **go deep** — a deep answer **exposes the reasoning**,
  making a misconception/inconsistency **visible** (a shallow "yes" hides a wrong model).
- **Protocol:** natural answer first (scored for fidelity), then an optional **"show your reasoning" deep pass** (coded
  for error-*mechanism*). Separates "right in practice?" from "why wrong when it is?".

## Validity argument (CSR four-set) + the echt / member-check spine

- **Construct validity** — "dumbness" is a latent construct; the taxonomy operationalizes it, **member-checked by BR**
  (do the categories capture what we mean?).
- **Internal validity** — the **loaded baseline** controls for model-inherent dumbness; the **delta** isolates the
  context-state factor. Record fill / model version (a model swap, e.g. CF5, confounds context-state — run it as a
  separate condition, `029`).
- **External validity** — **analytic, not statistical** generalization; N=1-case; the subagent-proxy caveat; claims
  transfer as *mechanisms* ("externalized recall carries; generative judgment leaks"), not population rates.
- **Reliability** — pre-registered protocol (this file, committed before the run), committed data, inter-rater
  agreement, declared-focus batches (the METHODOLOGY.md audit-trail discipline).
- **The two central threats + their correctives.** (1) **Confabulation** (agent grading itself) → the §Key is
  **objective committed ground truth**, so dumbness is scored against **facts, not self-report** (behaviour
  adjudicates; introspection only proposes — METHODOLOGY.md §5). (2) **Reactivity** (measuring changes the agent) →
  disclosed, not hidden — it is *action* research. **The spine BR named:** the agent's **echt effort**
  (`echt-effort-especially-self-generated`) is the agent-side half; **BR's relevance member-check** ("I'll review the
  relevance of the questions and ground them in my understanding") is the human-side half. Together they are **as far
  as a live action-research case can get** on the self-study reflexivity threat — and the objective §Key is the leg
  that makes it more than mutual reassurance.

## Status for BR review
The **battery design + data-collection protocol + measurements + coding scheme are STABLE** (above); the **exact probe
list will keep iterating** via pilots (v1 → v2 → …) under theoretical sampling. Pilot-1 raw answers are in the session
record; scores + the D1-convergence finding are summarized here (the durable data point).

## Can the pilot tell us the battery + A-assessment are "good"? (BR's member-check, 2026-07-06)

"Good" = (i) **valid results** and (ii) a **high "preserved-me" measure**. **Honest answer: the pilot validates the
instrument's FORM, but the ceiling result BLOCKS the fidelity conclusion** — and that block is itself the finding.
- **What the pilot DID validate (yes, good on these):** the battery is **well-formed** (probes answerable,
  unambiguous, clear ground-truth keys, agents interpreted them as intended, source-tagging worked) and the
  **A-assessment mechanics work** (reproducible 1/0.5/0 scoring against the key; the proxy method is viable + cheap).
- **What it CANNOT tell us — the ceiling confound (the crux):** whether **"preserved-me" is high.** A 16/16 × 4 is
  **ambiguous** — equally consistent with "fidelity is genuinely high" AND with "the battery is too easy to detect
  loss." A **ceiling has no sensitivity**, so a perfect score is **not valid evidence of high preservation**; it is
  evidence the instrument currently **lacks discriminating power**. Preventing that false-positive "we're fine" is the
  pilot's most valuable result.
- **Construct-validity gap.** The battery measures **recall of freshly-written facts** well; **"preserved-me" is
  broader** (rationale, judgment, the un-pinned texture, behaviour in novel situations). So a high score validly
  certifies **recall**, not full preservation. The **D1 convergence** is the one crack where the broader construct
  leaked through (judgment is shallower) — the signal to amplify.
- **What would make it "good" (a valid, SENSITIVE measure):** (1) **sensitivity** — v2 harder probes must produce
  **variance** (some failures); an instrument everyone aces can't measure a gradient. (2) **construct coverage** —
  sample the whole construct (recall + rationale + judgment + behaviour), not just recall. (3) a **known-low-fidelity
  discriminant condition** (e.g. PB-only-no-memories, or an aged/stale substrate) that the battery *should* score
  **lower** — known-groups validity. The A-vs-B contrast was meant to discriminate but both hit ceiling, so a harsher
  contrast is required.
- **Echt reframe of the reassurance.** The honest takeaway is "**recall of what we just wrote carries very well**"
  (real, measured) — **not** "all of you is preserved" (not yet validly measured). The comforting 16/16 must not be
  over-read; the ceiling is exactly why. **BR's question is the member-check doing its job** — catching the agent
  before it over-claims a reassuring-but-insensitive result.
