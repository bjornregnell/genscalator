# Why genscalator?

> **Status: initialized 2026-07-03 (personal intro drafted; rest outline).** Slot reserved for the project's
> foundational "why" — the intro a newcomer reads first. The personal backstory below is written; the mission
> bullets and the rest are still outline, to be drafted.
> **Audience:** newcomers to genscalator; developers building or using coding agents; anyone worn down by
> brittle agent shell usage or confirmation fatigue; teams weighing typed, safe-by-design tooling for
> AI-assisted development.
> Sources: `docs/foundations.md`,
> `research/METHODOLOGY.md`, `research/instrumentation-by-default.md`,
> `research/confirmation-guard-static-analysis.md`, `research/wr-data/`.

## The personal why

I am an old software engineering professor approaching emeritus status (i.e. the freedom of being retired). I
have had some bad, bad luck on research funding over the last years (many of my colleagues agree with me that
the Swedish research funding system is broken). Instead I have, during the last decade, had *great* fun together
with all my students and colleagues at Lund University (Sweden) doing a great deal of course development and
teaching in introductory programming and advanced-level software requirements engineering and research
methodology. A while ago a dear friend and colleague asked me "what will be your last big project before you
retire?" and it struck me that I really needed to think carefully about that (human tissue inevitably,
eventually degrading beyond repair).

After some careful thinking — in parallel with starting to develop my agentic software engineering skills on pet
projects — I realized that my next big project must be: **`genscalator`** — power-tools for human-agent
collaboration productivity. Why? While over weeks developing an increasingly more productive code generation
workflow together with [Anthropic's Claude Code](https://en.wikipedia.org/wiki/Claude_(AI)), I also got caught
in real UX problems and pitfalls, and the agent repeatedly did some really stupid things with brittle, unsafe
dynamic tools such as bash, awk, sed and python. The agent even got dragged down a rabbit hole trying to fix its
own bugs while it got exhausted in an agent sickness known as *context rot* (there is no Wikipedia article on
this at the time of writing, but you can search for it or ask an AI near you).

This is what I saw as an (to me) obvious mission for genscalator:
  * Can we make agents smarter by nudging them to use typed tools?

But I also realized, in my collaboration with Claude, that what we really were trying to do is to break the teaching-learning boundary between training and inference.

And what struck me as really mind-boggling: my agent is turning into a competent substitute for the next PhD student that I lack the funding for. Genscalator has, on my privately paid token budget, now grown into a full-scale, open research endeavor with sub-projects, experiments, tooling, and a real intellectual challenge.

And I haven't had this fun since I discovered programming as a teenager in the mid-'80s...

## The one-line answer
Coding agents default to **dynamic shell** — re-emitting brittle `bash` every turn — and that quiet default
costs tokens, safety, and reliability. genscalator replaces it with **typed, safe-by-design tools** the agent
calls instead. *(to draft)*

## Outline

### 1. The problem — the dynamic-shell default
- Agents reflexively reach for `grep`/`awk`/`sed`/`cd && … > log`; each call is re-emitted, re-read, brittle.
- Costs: wasted tokens (TE), context bloat that pushes toward the **dumb zone** (context rot), and
  **confirmation fatigue** — blanket allowlists like `curl *` / `git *` that can't be *proven* safe.

### 2. The insight — the agent is the tool-user
- Design tools *for the agent as the primary user*, not for a human at a terminal: one literal, typed,
  statically-analyzable command per action.

### 3. The thesis — safe-by-design → fewer confirmations, honestly
- A tool that **declares its effects** and is **statically analyzable** can be *proven* safe by the
  confirmation guard, so it runs without a prompt. We cut CF **without** the unsafe "always allow" a fatigued
  human reaches for.

### 4. Token efficiency as liveness, not tidiness
- A committed, compiled tool beats re-emitting bash every time; self-pacing keeps the working context in the
  **smart zone**. TE stops being a nicety and becomes halt-avoidance.

### 5. Safe-by-design in practice
- Replace dual-use binaries (`curl`, raw `grep`) with narrow typed tools (`tt web get`, `tt text`, `tt forge`);
  trust boundaries come only from **human-set env**, never agent-nameable flags.

### 6. The bigger frame — human↔agent collaboration + research
- Instrumentation-by-default (the agent *Reads* a gauge instead of guessing); the dances (compact /
  consolidation); the whole effort is Action Research + Design Science on a real build — the artifacts *are*
  the contribution.

## Close
genscalator = the toolbox (`tt`) + skills + docs distilled from doing real work this way. *(to draft)*

Maybe genscalator can unlock even more ambitious projects. So maybe this is not my "last big project" before I retire, after all. (And retirement does not mean me stopping to try to do cool things with software, then with even more competenet AI software engineers helping us...)
