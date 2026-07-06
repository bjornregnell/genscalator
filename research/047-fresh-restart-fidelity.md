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
