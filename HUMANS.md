# HUMANS.md — the humans' review queue

Mirror of [`AGENTS.md`](AGENTS.md): that file is for the agent(s), this one is for the **human(s)** —
a to-do list for human tasks such as **reviewing agent-authored changes**. 
* The agent NEVER writes this file directly (so it can't clobber your live edits). Instead it appends proposals to HUMANS.inbox.md (append-only, agent-owned), and you harvest them into the TODO below. This file is human-written only. 
* Tick items as you go; when a whole subsection is completed move it from ##TODO to ##DONE so other humans can see what we have done. 
* Agent should keep CHANGELOG.md consistent if affected. The main **historical record** of what changed in the repo lives in [`CHANGELOG.md`](CHANGELOG.md) (`## Unreleased`) — this file is only the *what-humans-still-owe* checklist.

## How this works
- Agent commits + pushes docs/research freely if agreed with human. 

- Agent->human channel: the agent posts new review items / proposals to HUMANS.inbox.md, never here. You harvest: move an item into TODO (tagging HD:/TAP: as needed), then delete it from the inbox. You alone own TODO and DONE.

- **Tool/version changes still follow the human-reviewed release flow** (`docs/updating.md`) — those are NOT auto-pushed; they wait for human approval.

- When you as a human finish reviewing an item, you to check it off or move a completet subsection to ## DONE or delete it if not relevant anymore for memory, changelog purpuses etc. When the ## TODO section is empty, you're caught up.

- When you tick of items that include requests for human decisions then you prefix your decision with *HD:* (for Human Decision) and you can also provide jobs for the agent marked *TAP:* for To Ageent Plan so when you instruct agent to read your updates in HUMANS.md agent will put those instructions in its work plan automatically and chat with you if prioritization of the plan is needed. 

## TODO

Here are the actual TODO of genscalator, so this document is both a template example of a HUMANS.md file as well as a working document for this repo. Future research may expand how we use HUMANS.md to support efficient agent-human work split and collaboration.

### Work on the HUMANS.md file guidelines
- [ ] **Chat with agent about HUMANS.md to improve guidlines in it** - (side note: humans tend not to do long emdashes but just - so why do agents do -- (but some UTF-8 glyph) all the time???) The HUMANS.md <-> AGENTS.md pair is actually a protocoll for human-agent collaboration. How do we get that into the /genscalator plugin? *TAP:* We should chat about this. 

### To review — session 2026-06-30 (foundations concepts + research notes)

#### A. Ratify glossary concepts (`docs/foundations.md`) — names + definitions are yours to veto
- [ ] **Smart-zone ceiling (Z)** — the usable working-context ratio (~0.3) before the dumb zone. (You already
      picked this name over "lucidity ceiling".) Confirm the definition reads right.
- [ ] **Compact dance** — the save→prompt→compact→paste ritual + the *recovery invariant* (truth in committed
      files + memory, the pasted prompt is only convenience). Your term; confirm wording.
- [ ] **Compact trigger** — propose the dance at **fill ≥ 0.8·Z**. Your chosen name. OK as the threshold?
- [ ] **Communication bandwidth (human↔agent)** — per-direction language/TE channel (you write L1, I answer
      cheap-clear; ask on idiosyncratic Swedish).
- [ ] **Token velocity / Token acceleration / Context rot** — the dS/dt, d²S/dt², and the rot framing. Confirm
      these are keepers (claim: self-monitoring the *derivative* of spend looks like a fresh framing).

#### B. Read the new research notes (lab-notebook depth, no action needed beyond a skim + steer)
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

## DONE

### C. Decisions I'm waiting on you for
- [X] **`AGENTS.md` vs `CLAUDE.md` canonicalization** — pick one as source of truth (symlink/include the other) so they can't drift. (Raised in `instruction-surfaces-precedence.md`.)
  * *HD:* go with AGENTS.md as we want to be agent-provider agnostic and work also for codex and opencode etc.
- [X] **Emoji rule** — you dropped the flat ban. Want any *scoped* guidance instead ("no decorative clutter; functional status glyphs OK"), or leave it unspecified?
  * *HD:* yes *TAP*: agent help me with scoped guidance and include in exa,ple of instruction for claude
- [X] **Where does the language-channel rule live?** — currently in agent *memory* (per-project). Should the universal version go in the *global* "Instructions for Claude" field instead? (See instructions-for-claude.md.) Probably global. *TAP:* Agnet investigate pros and cons of language-channel rule placement options.


---
Agent: never write HUMANS.md directly. Append review-worthy proposals to HUMANS.inbox.md (append-only). The human harvests into TODO, ticks, and moves completed subsections to DONE. Keep CHANGELOG.md consistent when relevant; stage only agent-authored paths, never git add -A.
