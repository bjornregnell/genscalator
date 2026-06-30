# HUMAN.md — BR's review queue

> Your personal to-do list for **reviewing agent-authored genscalator changes**. The agent appends here when
> it has pushed work that needs your judgement (naming, definitions, decisions, what to ship). Tick items as
> you go; delete a section once cleared. The **historical record** of what changed lives in
> [`CHANGELOG.md`](CHANGELOG.md) (`## Unreleased`) — this file is only the *what-you-still-owe* checklist.

## How this works
- Agent commits + pushes docs/research freely (you authorized this 2026-06-30 because you were behind on
  review). Nothing here changes the `tt` tools or a released version — it's docs/research accretion.
- **Tool/version changes still follow the human-reviewed release flow** (`docs/updating.md`) — those are NOT
  auto-pushed; they wait for you.
- When you finish reviewing an item, check it off or delete it. When the file is empty, you're caught up.

## To review — session 2026-06-30 (foundations concepts + research notes)

### A. Ratify glossary concepts (`docs/foundations.md`) — names + definitions are yours to veto
- [ ] **Smart-zone ceiling (L)** — the usable working-context ratio (~0.3) before the dumb zone. (You already
      picked this name over "lucidity ceiling".) Confirm the definition reads right.
- [ ] **Compact dance** — the save→prompt→compact→paste ritual + the *recovery invariant* (truth in committed
      files + memory, the pasted prompt is only convenience). Your term; confirm wording.
- [ ] **Compact trigger** — propose the dance at **fill ≥ 0.8·L**. Your chosen name. OK as the threshold?
- [ ] **Communication bandwidth (human↔agent)** — per-direction language/TE channel (you write L1, I answer
      cheap-clear; ask on idiosyncratic Swedish).
- [ ] **Token velocity / Token acceleration / Context rot** — the dS/dt, d²S/dt², and the rot framing. Confirm
      these are keepers (claim: self-monitoring the *derivative* of spend looks like a fresh framing).

### B. Read the new research notes (lab-notebook depth, no action needed beyond a skim + steer)
- [ ] `research/instructions-for-claude.md` — **contains your current "Instructions for Claude" verbatim** as
      the worked example. Check I captured it correctly. Note: I recorded that you **removed "No emojis"**
      (emojis can raise bandwidth if non-irritating) — confirm that's the intent.
- [ ] `research/instruction-surfaces-precedence.md` — how AGENTS.md/CLAUDE.md/SKILL.md/MEMORY.md/global-instr
      compose + rank. Includes my best-guess **authority order** — sanity-check it against your understanding.
- [ ] `research/smart-zone-ceiling.md`, `research/communication-bandwidth.md`, `research/token-budget-awareness.md`,
      `research/instrumentation-by-default.md` — skim; flag anything off.
- [ ] `research/instruction-adherence-decay.md` — **why I keep regressing to dynamic shell** (your question).
      Argues it's trained-prior reflex (no external guardrail), so the fix is structural (tool+allowlist+hook),
      not more rules. This is the *justification for genscalator's whole method* — worth your read + steer on
      whether to prototype the **submit-time hook** (item D).

### C. Decisions I'm waiting on you for
- [ ] **`AGENTS.md` vs `CLAUDE.md` canonicalization** — pick one as source of truth (symlink/include the
      other) so they can't drift. (Raised in `instruction-surfaces-precedence.md`.)
- [ ] **Emoji rule** — you dropped the flat ban. Want any *scoped* guidance instead ("no decorative clutter;
      functional status glyphs OK"), or leave it unspecified?
- [ ] **Where does the language-channel rule live?** — currently in agent *memory* (per-project). Should the
      universal version go in the *global* "Instructions for Claude" field instead? (See instructions-for-claude.md.)

### D. WR-data → candidate `tt` tools to greenlight (from `research/wr-data/introprog-autotranslate.md`)
These are the most-repeated friction patterns; each is a small typed tool waiting for your go-ahead:
- [ ] **`tt text countr` / batch count+status** — kill the recurring `grepr | wc -l`, `for/if/grep -q`
      aggregation-across-a-set bundles (logged 4×+ — the dominant friction category).
- [ ] **`tt git overview <path>` / `tt repo status`** — one bare call for "state of repo X" (changed files +
      last N commits) to stop the `cd && ls && echo && git log` regressions.
- [ ] **`tt pdf grep <pdf> <regex>`** — "did X render?" checks without `cd | pdftotext | grep | head`.
- [ ] **`gpu-status.scala` / `tt run <scratch>`** — job/GPU health probe + a scratch-runner that strips JVM
      noise (so I stop piping scratch output through `grep -v`).
- [ ] **`token-usage` → graduate to `tt usage`** — the context-fill / velocity / smart-zone-ceiling
      instrument (currently an introprog scratch hardcoding your home path; belongs in the toolbox).
- [ ] **Encoding bug:** `tt text match/count/cols` read latin1 → mangle Swedish å/ä/ö; make them UTF-8 (or
      `--enc`). Pre-existing, still owed.

---
*Agent: keep this file current — append a new dated section when you push review-worthy work; prune cleared
sections so the list reflects only what BR still owes.*
