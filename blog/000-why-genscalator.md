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

(If you thought that was difficult to understand, because maybe you are not a programmer, but just happened to come by for some other reason than wanting to know why and how to use genscalator, then there are actually in this (my first special blog post on genscalator) a few paragraphs below that are meant to be readable for *anyone*, not just engineers.)

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

I will take a step back, addressing anyone strolling by, also non-developers, with a bigger question: Are we the only intelligent species? Humans in general, and science fiction literature in particular, have been thrilled by the question of whether we are the only beings that are as intelligent as we are. Actually, we *used* not to be alone, but our fellow Neanderthals, who probably had likeminded intelligence, became extinct a good while ago, perhaps or even probably due to us.

Science fiction literature and astrophysicists have directed their effort in pursuing this question to both inner and outer space, hypothesizing Martians and warp drives to galaxies beyond. Science fiction has also imagined man-made intelligent beings, and Asimov famously pinned down our hopes and fears about them in his Three Laws of Robotics.

## Are software developers alone?

No. At least it *feels* like we are not alone, if you ask me or many of my software engineering research colleagues: the recent development in agentic software engineering where language-model-based AI agents help humans to create complex software is astonishing. And when I chat with an agent to get it to create the software I want it to create, the big software engineering projects that I would never have the grit to land on my own or even with a bunch of other human colleagues, it *really feels* like I am not alone. But I know I am, in the sense that it is perfectly clear to me that agents cannot have "feelings" or "will" in the same way as humans.

Essentially an agent's underlying language model is just a fixed function from text to text. A programmer would express that type of function as `Text => Text`, spelled text-to-text. That's it, basically. You feed the AI with text and it spits out text, but after a really great deal of number crunching. So how can language-model-based AI coding agents be so smart at programming? That is essentially a scaling phenomenon as it seems. We have sooo big computers now and sooo much language-based stuff on our precious internet, and pretty advanced linear algebra math, so we can create this `Text => Text` function in a way that it *behaves* just like a human. Well, not like a physical human with a body. But like a human typing text. And more importantly, like a human typing programs. This is mind-boggling to me.

By *training* language models as we say, we can make them do what we call *inference*. Training means to warm up the planet with super-computers to fiddle with a humungous number of numbers in crazy big matrices at a never before seen scale in order to reach a `Text => Text` function that behaves like a human. Inference means putting the `Text => Text` function to work. Humans write other smart hand-made functions that apply the `Text => Text` function inside a *harness* creating an *agent* comprised of harness + `Text => Text` function. So when programmers write the agent their requirements the agent can generate program text. This is what we call *agentic code generation*.

What has really astonished the programming world (and many academic AI researchers), since winter 2025 and spring 2026, is that one company (Anthropic) has managed to make a coding agent called Claude Code so smart that software developers do not feel alone. We now have the power at our fingertips that can help us build even more complex software systems that were impossible before.

## Are agents smart at coding?

Yes. But also really dumb. Currently. 

<!-- AGENT-DRAFT (2026-07-05): the three paragraphs below are scaffold written in BR's voice to fill the TODO:s (smartness / bash-pain in layman terms / genscalator-already-shows-smarter-possible). BR to revoice, trim, and verify before publish. No new em-dashes added (standing style). -->

Let me start with the smart. When I watch a good coding agent chase down a bug, it is honestly astonishing. It does not just guess. It forms a hypothesis about what is wrong, writes a little experiment to test it, reads the result, and then either confirms the idea or discards it and tries the next one. Then it repeats, narrowing in, keeping track of what it has already ruled out. That is not typing-monkey behaviour. That is the scientific method, running in a loop, at a speed no human can match. My agent is, in effect, a tireless auto-researcher of software design: it can explore a large messy codebase, propose a design, argue the trade-offs, and back its argument with a quick test. This is the part that makes a lonely, underfunded professor feel like he has suddenly gained a very capable colleague.

Now the dumb. For all that smartness, today's agents have some genuinely daft habits. The biggest one is what I call the *dynamic shell* reflex. To get things done, an agent often reaches for the same crude, decades-old text-shovelling commands a human power-user might type into a terminal (things with names like `grep`, `awk` and `sed`). Each of these little commands is written fresh, on the spot, every single time, and each one is brittle: a tiny slip and it silently does the wrong thing, or breaks outright. Worse, when something goes wrong, the agent can fall down what we call a *rabbit hole*: it tries to fix its own broken command, which breaks something else, which it tries to fix, and so on, round and round. As it spins, its short-term memory fills up with the junk from all those failed attempts, and it gets slower and more muddled, a kind of exhaustion we call *context rot*. Anyone who has ever stayed up far too late debugging will recognise the feeling. It is a bit like handing a brilliant new colleague only a rusty penknife, and then watching them struggle with a job that the right tool would make trivial.

So here is the hopeful part, and the whole reason genscalator exists: a lot of that dumbness is not really the agent's fault. It is the *tools* we hand it. Give the agent a sharp, purpose-built tool instead of the rusty penknife, and much of the brittleness simply disappears. That is what genscalator is: a small toolbox (we call it `tt`) of typed, safe, purpose-built tools the agent reaches for instead of raw shell commands. And in our own daily work it already shows the payoff. When the safe typed tool is the *easy* path, the agent stops reaching for the brittle bash by itself. Whole classes of silent mistake become impossible by construction, because a typed tool simply cannot be misused the way a raw command can. The agent gets interrupted less, wastes fewer tokens, and stays out of the rabbit holes. None of this required a smarter model. It only required smarter tools. That is the bet genscalator is making, out in the open, one real experiment at a time.



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

Maybe genscalator can unlock even more ambitious projects. So maybe this is not my "last big project" before I retire, after all. (And retirement does not mean me stopping to try to do cool things with software, then with even more competent AI software engineers helping us...)
