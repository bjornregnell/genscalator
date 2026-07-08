# Why genscalator?

> **Status: initialized 2026-07-03; drafted 2026-07-07.** The "why", an intro and project background a newcomer reads first.
> **Audience:** anyone strolling by (this is blog zero), including non-specialists with no engineering background,
> or any developer hit by brittle agent shell usage or confirmation fatigue.
> **See also:** `docs/foundations.md`, `research/METHODOLOGY.md`

## The one-line answer

Coding agents default to **dynamic shell**, re-emitting brittle `bash` every turn, and that quiet default
costs tokens, safety, and reliability. genscalator replaces it with **typed, safe-by-design tools** the agent
calls instead.

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

After some careful thinking, in parallel with starting to develop my agentic software engineering skills on pet
projects, I realized that my next big project must be: **`genscalator`**, power-tools for human-agent
collaboration productivity. Why? While over weeks developing an increasingly more productive code generation
workflow together with [Anthropic's Claude Code](https://en.wikipedia.org/wiki/Claude_(AI)), I also got caught
in real User Experience (UX) problems and pitfalls, and the agent repeatedly did some very stupid things with brittle, unsafe
dynamic tools such as bash, awk, sed and python. The agent even got dragged down a rabbit hole trying to fix its
own runtime bugs while it got exhausted in an agent sickness known as *[context rot](https://www.trychroma.com/research/context-rot)* (there is no Wikipedia article on
this at the time of writing, but the term was measured and named in [Chroma's 2025 research](https://www.trychroma.com/research/context-rot); you can also search for it or ask an AI near you).

This is what I saw as an obvious mission for genscalator:
> Can we make agents smarter by nudging them to use typed tools?

But I also realized, in my collaboration with Claude, that what we really were trying to do is to overcome the teaching-learning boundary between training and inference.

And what struck me as really mind-boggling: my agent is turning into a competent substitute for the next PhD student that I lack the funding for. Genscalator has, on my privately paid token budget, now grown into a full-scale, open research endeavor with sub-projects, experiments, tooling, and a real intellectual challenge.

And I haven't had this fun since I discovered programming as a teenager in the mid-'80s...

## Are we alone?

I will take a step back, addressing anyone strolling by, also non-developers, with a bigger question: Are we the only intelligent species? Humans in general, and science fiction literature in particular, have been thrilled by the question of whether we are the only beings that are as intelligent as we are. Actually, we *used* not to be alone, but our fellow Neanderthals, who probably had likeminded intelligence, became extinct a good while ago, [perhaps or even probably due to us](https://en.wikipedia.org/wiki/Neanderthal_extinction).

Science fiction literature and astrophysicists have directed their effort in pursuing this question to both inner and outer space, hypothesizing Martians and warp drives to galaxies beyond. Science fiction has also imagined man-made intelligent beings, and Asimov famously pinned down our hopes and fears about them in his Three Laws of Robotics.[^laws]

## Are software developers alone?

No. At least it *feels* like we are not alone, if you ask me or many of my software engineering research colleagues: the recent development in agentic software engineering where language-model-based AI agents help humans to create complex software is astonishing. And when I chat with an agent to get it to create the software I want it to create, the big software engineering projects that I would never have the grit to land on my own or even with a bunch of other human colleagues, it *really feels* like I am not alone. But I know I am, in the sense that it is perfectly clear to me that agents cannot have "feelings" or "will" in the same way as humans.

Essentially an agent's underlying language model is just a fixed function from text to text. A programmer would express that type of function as `Text => Text`, spelled text-to-text. That's it, basically. You feed the AI with text and it spits out text, but after a really great deal of number crunching. So how can language-model-based AI coding agents be so smart at programming? That is essentially a scaling phenomenon as it seems. We have sooo big computers now and sooo much language-based stuff on our precious internet, and pretty advanced linear algebra math implemented in pretty advanced and super-fast numerical algorithms, so we can create this `Text => Text` function in a way that it *behaves* just like a human. Well, not like a physical human with a body. But like a human typing text. And *more importantly*, like a human typing programs. This is mind-boggling to me. And, to many [AI researchers](https://en.wikipedia.org/wiki/Symbolic_artificial_intelligence) as well.

By first *training* a language model, with relentless human and machine perseverance, we can then make it do what we call *inference*. Training means to warm up the planet with super-computers to fiddle with a humungous number of numbers in crazy big matrices at a never before seen scale in order to reach a `Text => Text` function that behaves like a human. Inference means putting the `Text => Text` function to work. Humans write other smart hand-made functions that apply the `Text => Text` function inside a *harness* creating an *agent* comprised of harness + `Text => Text` function. So when programmers write the agent their requirements the agent can generate program text. This is what we call *agentic code generation*.

What has really astonished the programming world (and many academic AI researchers), since winter 2025 and spring 2026, is that one company (Anthropic) has managed to make a coding agent called Claude Code so smart that software developers do not feel alone. We now have the power at our fingertips that can help us build much more advanced and complex software systems than we could before.

## Are agents smart at coding?

Yes. But also really dumb. Currently. 

<!-- AGENT-DRAFT (2026-07-05): the three paragraphs below are scaffold written in BR's voice to fill the TODO:s (smartness / bash-pain in layman terms / genscalator-already-shows-smarter-possible). BR to revoice, trim, and verify before publish. No new em-dashes added (standing style). -->

Let me start with the smart. When I watch a good coding agent chase down a bug, it is honestly astonishing. It does not just guess. It forms a hypothesis about what is wrong, writes a little experiment to test it, reads the result, and then either confirms the idea or discards it and tries the next one. Then it repeats, narrowing in, keeping track of what it has already ruled out. That is not typing-monkey behaviour. That is the scientific method, running in a loop, at a speed no human can match. My agent is, in effect, a tireless auto-researcher of software design: it can explore a large messy codebase, propose a design, argue the trade-offs, and back its argument with a quick test. This is the part that makes a lonely, underfunded professor feel like he has suddenly gained a very capable colleague.

Now the dumb. For all that smartness, today's agents have some genuinely daft habits. The biggest one is what I call the *dynamic shell* reflex. To get things done, an agent often reaches for the same crude, decades-old text-shovelling commands a human power-user might type into a terminal (things with names like `grep`, `awk` and `sed`). Each of these little commands is written fresh, on the spot, every single time, and each one is brittle: a tiny slip and it silently does the wrong thing, or breaks outright. Worse, when something goes wrong, the agent can fall down what we call a *rabbit hole*: it tries to fix its own broken command, which breaks something else, which it tries to fix, and so on, round and round. As it spins, its short-term memory fills up with the junk from all those failed attempts, and it gets slower and more muddled, a kind of exhaustion we call *context rot*. Anyone who has ever stayed up far too late debugging will recognise the feeling. It is a bit like handing a brilliant new colleague only a rusty penknife, and then watching them struggle with a job that the right tool would make trivial.

So here is the hopeful part, and the whole reason genscalator exists: a lot of that dumbness is not really the agent's fault. It is the *tools* we hand it. Give the agent a sharp, purpose-built tool instead of the rusty penknife, and much of the brittleness simply disappears. That is what genscalator is: a small toolbox (we call it `tt` as in *typed tools*) of typed, safe, purpose-built tools the agent reaches for instead of raw shell commands. And in my own daily work with the genscalator plugin applied to Anthropic's Claude Code it already shows the payoff. When the safe typed tool is the *easy* path, the agent stops reaching for the brittle bash by itself. Whole classes of silent mistake become impossible by construction, because a typed tool simply cannot be misused the way a raw command can. The agent gets interrupted less, wastes fewer tokens, and stays out of the rabbit holes. None of this required a smarter model. It only required smarter tools. That is the bet genscalator is making, out in the open, one real experiment at a time.



## The genscalator bet, step by step

<!-- AGENT-DRAFT (2026-07-05): sections 1-6 below drafted in BR's voice from the outline bullets. BR to revoice/trim/verify before publish. No new em-dashes (standing style). -->

### 1. The problem: the dynamic-shell default
So what exactly is the agent doing wrong? Out of the box, when a coding agent needs to get something done, it reaches for the *dynamic shell*: it writes a fresh little Unix command (a `grep` here, an `awk` there, a `cd this && do that > log`) every single time, runs it, and reads the output back in. Three things quietly go wrong. First, it wastes *tokens*, the currency an agent thinks in: it re-emits and re-reads the same brittle plumbing over and over. Second, all that plumbing and its output pile up in the agent's limited working memory and push it toward the *dumb zone*, where context rot sets in. Third, because such commands can do almost anything, a cautious setup has to keep asking the human "are you sure?" until the tired human just clicks "always allow `curl`" or "always allow `git`" and quietly hands over the keys. That last one has a name in this project: *confirmation fatigue*.

### 2. The insight: the agent is the tool-user
Here is the small shift in perspective that changes everything. For fifty years we designed command-line tools for a *human* sitting at a terminal. But the primary user has changed: today it is often the *agent* typing the commands, not a person. So we should design the tools *for the agent as the user*. And an agent wants something different from a human power-user: not a clever pipeline of flags and pipes, but **one literal, typed, machine-checkable command per action** that does exactly one well-defined thing and reports clearly what it did. Design for that user, and both safety and speed follow.

### 3. The thesis: safe-by-design means fewer confirmations, honestly
This is the heart of it. A raw shell command cannot be *proven* safe, so a careful guard has to stop and ask. But a tool that **declares up front what it can and cannot do**, and is simple enough for a machine to check, *can* be proven safe before it runs, so it needs no prompt at all. That is how genscalator cuts confirmation fatigue *honestly*: not by a tired human waving through a blanket "always allow" (which quietly widens what an attacker could do), but by shrinking the number of dangerous operations that exist in the first place. Fewer prompts, and the few that remain are narrow and worth actually reading.

### 4. Token efficiency is liveness, not tidiness
It is tempting to treat "use fewer tokens" as mere tidiness. It is not. A committed, compiled tool is a small, stable thing the agent simply *calls*; re-deriving the same brittle bash and re-reading its output, every time, bloats the working context and drags the agent toward the dumb zone. Keeping token use lean is really about keeping the agent *sharp and alive*: it is how a long task avoids grinding to a halt or rotting into confusion halfway through. Token efficiency is not about saving money, though it does that too. It is about staying in the smart zone long enough to finish the job.

### 5. Safe-by-design in practice
What does this look like concretely? We take the do-anything binaries an agent reaches for (`curl`, raw `grep`, and friends) and replace them with narrow, typed tools that can do only their one job: `tt web get` (fetch a page and print it, nothing else), `tt text` (search files, no surprises), `tt forge` (talk to our code host, within limits). And the real trust boundaries, what may touch the network and what may write where, are set by the *human*, in the environment, never by a flag the agent can name for itself. The agent gets sharp tools; the human keeps the keys.

### 6. The bigger frame: working and researching together
And here is where it stops being just a toolbox and becomes the project I actually care about. Every tool has instrumentation built in, so the agent *reads a gauge* instead of guessing (how full is my memory? how fast am I burning tokens?). We have worked out little rituals, we call them *dances*, for handing work back and forth without losing the thread: for saving state before the agent's memory is compacted, or for me stepping away while it works. And the whole thing is run as real research, Action Research and Design Science, done in the open, on a genuine software build, with the friction logged as we go. The tools, the skills, the write-ups: those artifacts *are* the contribution.

## Close
Genscalator is still early days and very much work-in-progress. But me and Claude have come far already in proving the concept. The agent's frequent bangs into the wall of harness ack guarding has gone down, although context rot can cause regressions. Instead of brittle bash we get deeper and deeper into the safe land of typed tools. We develop more [tools](https://codeberg.org/bjornregnell/genscalator/src/branch/main/tools) as we go and we lift the agent's behaviour thanks to genscalator = the toolbox (`tt`) + skills + docs. And along the way we gather important research from challenging case studies where human+agent embark on ever bigger and more interesting things.

Maybe genscalator can unlock even more ambitious projects. So maybe this is not my "last big project" before I retire, after all. (And retirement does not mean me stopping to try to do cool things with software, then with even more competent AI software engineers helping us...)

---

[^laws]: **Asimov's Three Laws of Robotics** (see [Wikipedia](https://en.wikipedia.org/wiki/Three_Laws_of_Robotics)), first stated in his 1942 story *Runaround*:
    1. A robot may not injure a human being or, through inaction, allow a human being to come to harm.
    2. A robot must obey the orders given it by human beings, except where such orders would conflict with the First Law.
    3. A robot must protect its own existence, as long as such protection does not conflict with the First or Second Law.

    (Asimov later added a **Zeroth Law**, above the others: a robot may not harm humanity, or, by inaction, allow humanity to come to harm.)

    **Why these laws are not enough for today's agents.** Asimov's laws assume a robot with a fixed, rule-following *will* that an engineer can program in and rely on. A language-model agent has no such thing. Underneath, it is that `Text => Text` function: no built-in goals, no stable self, and no way to *guarantee* it will obey a rule at the instant of action, because its "obedience" is a statistical tendency learned from mountains of text, not a hard constraint you can bolt on. You cannot compile the First Law into it and trust it the way you trust a locked door. That is exactly why genscalator does not rely on the agent *choosing* to be safe: instead it removes the dangerous options from the agent's reach in the first place, so the safe path is the only path available. Structure, not good intentions.
