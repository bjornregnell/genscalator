# Why genscalator?

> **Status: initialized 2026-07-03 (personal intro drafted; rest outline).** Slot reserved for the project's
> foundational "why" — the intro a newcomer reads first. The personal backstory below is written; the mission
> bullets and the rest are still outline, to be drafted.
> **Audience:** anyone strolling by (this is blog number zero); newcomers to genscalator; developers building or using coding agents; anyone worn down by
> brittle agent shell usage or confirmation fatigue; teams weighing typed, safe-by-design tooling for
> AI-assisted development.
> Sources: `docs/foundations.md`,
> `research/METHODOLOGY.md`, `research/instrumentation-by-default.md`,
> `research/confirmation-guard-static-analysis.md`, `research/wr-data/`.

## The one-line answer

Coding agents default to **dynamic shell** — re-emitting brittle `bash` every turn — and that quiet default
costs tokens, safety, and reliability. genscalator replaces it with **typed, safe-by-design tools** the agent
calls instead. *(to draft)*

(If you thoght that was difficult to understand, because maybe you are not a programmer, but just happend to come by for some other reason than wanting to know why and how to use gensacalator, then there is actually in this (my first special blog post on genscalator) a paragraphs below that are ment to be readable for *anyone*, not just engineers.)

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

## Are we alone?

I will take a step back, addressing anyone strolling by, also non-developers, with a bigger question: Are we the only intelligent speicies? Humans in general and science fiction literature in particular, have been thrilled about the question if we are the only beings that are as intelligent as we are. Actually, we *used* not to be alone, but our fellow neanderthals that probably had likeminded intelligence, became extinct a good while ago, perhpas or even probably due to us. 

Science fiction literature and astro physicists have directed their effort in persuing this question to both inner and outer space, hopthesizing martians and warp drives to galaxies beyond. Science fiction has also imagined man-made intelligent beings and Asimov has pinned the 

## Are software developers alone?

No. Atleast it *feels* like we are not alone, if you ask me or many of my software engineering research colleagues: the recent development in agentic software engineering where language-model-based AI agents help humans to create complex software is astonishing. And when I chat with an agent to get it do create the software I want it to create, the big software engineering projects that I would never have the grit to land on my own or even with a bunch of other human colleagues, it *really feels* like I am not alone. But I know I am, in the sense that it is perfectly clear to me that agents cannot have "feelings" or "will" in the same way as humans.

Essentially an agent's underlying language model is just a fixed function form text to text. A programmer would express that type of function as `Text => Text`, spelled text-to-text. That's it, basically. You feed the AI with text and it spits out text, but after a really great deal of number crunching. So how can language-model-based AI coding agents be so smart at programming? That is essentially a scaling phenomena as it seems. We have sooo big computer now and sooo much language-based stuff on our precious internet, and pretty advanced linear algebra math, so we can create this `Text => Text` function in a way that it *behaves* just like a huma. Well not like a phisical human with a body. But like a human typing text. An more importantly, like a human typing programs. This is mind-boggling to me. 

By *training* language models as we say, we can make them do what we call *inference*. Training means to warm up the planet with super-computers to fiddle with a humungous number of numbers in crazy big matrices at a never before seen scale in order to reach a `Text => Text` function that behaves like a human. Inference mean putting the `Text => Text` function to work. Humans write other smart hand-made functions that apply the `Text => Text` function inside a *harness* creating an *agent* comprised of harness + `Text => Text` function. So when programmers write the agent their requirements the agent can generate program text. This is what we call *agentic code generation*.

What has really astinished the programming world (and many academic AI researchers), since winter 2025 and spring 2026, is that one company (Anthropic) has managed to make a coding agent called Claude Code so smart that software develpers does not feel alone. We now have the power at our fingertips that can help us build even more complex software systems that was impossible before.

## Are agents smart at coding?

Yes. But also really dumb. Currently. 

TODO: write about all the smartness, like astonishing debugging capability with super smart hypothesis creating, tracking, testing, its really an auto-researcher of software design. etc etc

TODO: write in laymen terms about bash pain, rabbit holes, etc etc etc.

TODO: write how genscalator alreade has shown that it is possible to be smarter

TODO by agent: can you draft above TODO:s in the same kind of voice as humans previous text? but before that: fact check and report while go fix grammar+typos



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
