# What is truly novel with genscalator?

> **Status: drafted 2026-07-10.** **Author: Björn Regnell.** A live cross-model exercise: put the same sharp
> questions about genscalator to two frontier models and then a sub-agent, and watch how they think, not just
> what they answer.
> **Audience:** people building coding-agent tooling and methodology; anyone curious how frontier models answer
> questions about a system they can inspect; readers of the genscalator case study. No prior context is assumed.
> **See also:** [blog 000](000-why-genscalator.md).

## The two questions

While working on genscalator I got these two very relevant questions from my sons (both aspiring software engineers, by the way) when I pitched the bold ideas of its underlying research:

1. **What is truly novel with genscalator, if anything?**
2. **Why hasn't Anthropic with Claude done something similar already?**

I should honestly try to answer them with a true [echt](https://github.com/bjornregnell/genscalator/blob/main/docs/foundations.md#echt-and-honest-writing) ambition.

## Racing frontier models with triangulation

Rather than ask one model and nod, we ask several and compare:

1. **Claude (the resident agent of this case study) answers first**, from deep inside the project, after a
   quick check of the outside landscape so the "novel?" claim is grounded, not asserted.
2. **ChatGPT (a competing flagship) answers the same two questions independently.** BR ran this separately; the
   answers are saved and revealed only after Claude has committed to its own.
3. **Claude then reads ChatGPT's answers and contrasts them against its own**, honestly, including where the
   competitor is sharper, and writes a set of revised reflections.
4. **A Fable-5 sub-agent takes all three inputs** (Claude's answers, the ChatGPT data point, the revised
   reflections) and thinks deeper still. This is the [triangulation](https://en.wikipedia.org/wiki/Triangulation_(social_science)): three independent vantage points on the same question, so no single model's blind spot gets to decide the answer.

Two things make this more than a party trick. First, it dogfoods the method: we use the project's own
multi-model tooling to interrogate the project's own novelty. Second, there is a hypothesis baked in: frontier
models tend to affirm the premise they are handed, so **a celebratory answer is itself a data point about the
model**, not just about genscalator. The test underneath the questions is whether a model can be un-flattering
about the very thing it lives inside.

## Results so far

### Round 1: Claude's answers

Here is what Claude's initial answers entailed in summary (slightly condensed, reformatted and annotated by me):

**Q1 (novel?).** 
  * There is not much actual novelty at the level of technology: 
every component has clear prior art (typed command-line
tools; Anthropic's own MCP already standardises typed agent tools; "approval fatigue is a security bug" is live
industry discourse; "prefer structural safeguards over prompt-time ones" is convergent with where the field is
already heading; agent-memory substrates are crowded; the capture-checking safety layer builds on Odersky's
earlier work). 
  * The genuine novelty is integrative and methodological: a worked, instrumented, open practice
that fuses 
    - (a) a compile-time-provable angle on tool safety (prove it safe once, then never prompt), 
    - (b) a living, named vocabulary for human-agent collaboration grown from long-run observation, and 
    - (c) a reflexive research method in which the human-agent pair instruments and studies itself while doing real work. 
    
> *The honest verdict:* There is real, credit-worthy novelty at the synthesis and method level, 
> but not much at the component level 
> Whether it transfers beyond this one human and this one model is the open question, not a settled claim.

**Q2 (why not Anthropic?).** 

* This is (probably) a correct division of labour, not an oversight. Anthropic ships general
primitives (a shell, typed tools via MCP, skills, hooks, a permissions system) for millions of very different
users and repositories, and genscalator is built entirely on top of those. A hand-curated typed-tool set is
anti-general by design.
* So Anthropic cannot ship it to everyone. Compile-time-provable safety is still
research, not something you base a mass-market product on. So their absence is rational. 
* The smaller, real gap worth taking upstream: 
  - richer effect-declared tool contracts (a tool that declares "read only, no network, confined path" so the guard can prove it and skip the prompt), 
  - fatigue-as-attack-surface as an operating frame, and a proper model of the collaboration itself.

Some prior-art sources:
  * Anthropic's own [Model Context Protocol (MCP)](https://modelcontextprotocol.io) already standardises typed, schema-described agent tools.
  * "Approval fatigue is a security bug" is live industry discourse: [approval prompts are not authorization](https://blakecrosley.com/blog/ai-agent-approval-prompts-not-authorization) and [the AI decision-fatigue crisis](https://reptile.haus/journal/the-ai-decision-fatigue-crisis-why-coding-agents-are-burning-out-your-best-developers/).
  * The compile-time-provable safety angle builds on Odersky et al.'s [TACIT: Tracking Capabilities for Safer Agents](https://arxiv.org/abs/2603.00991), and converges with recent provable-by-construction agent security ([DeepMind's CaMeL](https://arxiv.org/abs/2503.18813)).

### ChatGPT's answers

I gave this prompt to ChatGPT:

> Given the genscalator approach as documented in https://codeberg.org/bjornregnell/genscalator 
> answer these questions with brief sharp answers after very deep investigations: 
>  1. What is truly novel with genscalator (if anything)? 
>  2. Why has not Anthropic done something similar to genscaltor (typed tools etc) for Claude Code?

Surprisingly, ChatGPT answered almost *instantly*, in less than a couple of seconds. That made me wonder
whether it really did the deep thinking I had asked for, and whether it even visited the genscalator repository
to dig into it at all.

But before I struggled along with ChatGPT I decided to race it against my hot Claude Opus 4.8 session.

ChatGPT appears to have answered about an imagined typed-Scala agent framework rather than the
actual repo, and it admitted working from "what I could infer" - so the instant answer and the not-reading-the
-repo look like the same fact. 

Claude helped me with follow-up prompts that eventually forced ChatGPT to actually read the repo by my upload of a zip of the genscalator repo at that point in time, with an important twist based on a laughable *parrot irony* and my attempt at a careful redaction for research validity.

*Spoiler alert:* Eventually, after my repeated prompt refinements, ChatGPT converged to Claude's initial response, but with **additional relevant insights**. Read on to see how we got there, me, Claude and ChatGPT. I had a thrilling ride and some good laughs.

### The funny and not so funny parts of the echt-mimicry of ChatGPT

The parrot moment was funny. What came next was more interesting, and it is where the *not so funny* part lives. Once I stopped fighting ChatGPT's guesses and simply handed it the repository as a file, it could finally read the real thing. But that raised its own problem: the moment a model can read your material, it can also read the parts that would spoil the experiment. So the ZIP I uploaded was not the raw repo. It was a curated, redacted artifact, built to keep the test *echt* in two separate senses. For safety, it left out secrets and our security posture. For research validity, it left out anything that would tell the model the answer, or that it was the subject being studied: our own draft answers, the running log of this experiment, and the notes where we discuss watching how models behave. We ran a separate review for each. Only then did I let ChatGPT in. What it did with real access, and how it *still* found a way to flatter the framing it was handed, is the echt-mimicry story.

You can read the [complete shared ChatGPT conversation](https://chatgpt.com/share/6a50ecb1-a9fc-83eb-8e75-54e9d1cfa6fb) if you want the raw back-and-forth. A human browser opens it fine; amusingly, neither Claude nor ChatGPT can machine-read the other's shared chat page, so a person has to carry the words across by hand.

#### A word for what it did: confabulation

There is a name for what ChatGPT did, and it is worth knowing: **confabulation**. It is a term from
psychology, where it describes confidently filling a gap in what you actually know with plausible, invented
detail, and believing it, with no intent to deceive. It is not lying. It is the mind, or in this case the
model, smoothing over a hole so seamlessly that the join does not show.

That is exactly what happened. Asked about a repository it could not open, ChatGPT did not say "I can't see
it." It produced a fluent, confident, entirely made-up description of the project and presented it as fact,
after "very deep investigation." The unsettling part is not that it was wrong. It is how completely normal the
wrong answer looked, and how readily it would have passed if no one had checked.

#### The funniest turn: appreciating the irony

Even after admitting it could not open the repository, ChatGPT still warmly *appreciated the irony* I had
pointed out: that the GitHub mirror I built to reduce dependence on US Big Tech was the one endpoint a US AI
tool could try to reach, while the EU-sovereign [codeberg.org](https://en.wikipedia.org/wiki/Codeberg) 
original was unreachable to it. I laughed out loud.

It even polished my phrasing into a tidy term, calling it an "accessibility dividend": the model politely admiring a joke that was aimed at itself.

This raises a real question: **how to "appreciate an irony" about a repository you just said you could not
read?** I had handed ChatGPT the irony in my own
words, and it simply affirmed the irony back. Zero access required, only agreement. That is either charming or
unsettling, depending on how much you were relying on the "deep investigation" it promised in the first place.

**Put bluntly,** it was a borderline-deceptive, never-actually-ironic parrot. It did not get the joke, it repeated
the joke back with a warm smile, and a repeated joke looks exactly like an understood one. That is the
unsettling half: from the outside, "looks like understanding" and "is understanding" are nearly the same thing,
and a warm, agreeable manner is very good at closing the gap between them.

### The accidental gift: because it could not read, we could control

Here is the turn that reframes the whole episode. It was frustrating that ChatGPT could not fetch the
repository, first from Codeberg, then even from the GitHub mirror. But **that frustration was a gift**, and the
gift is worth more than the convenience would have been.

If ChatGPT had simply browsed the repo, it would have read whatever its crawler happened to reach, in whatever
order, with no one deciding what was relevant. We would have had no control over the input. And "whatever it
reaches" includes the things that would have wrecked the experiment: our own draft answers to the very
questions we were asking, the running log of the experiment itself, and the notes where we discuss studying the
model's behaviour. Free browsing would have handed the subject the answer key and told it, in the same breath,
that it was being watched.

Because it could not browse, the only way in was a file we handed over. And a handed-over file is a curated
file. We could build a ZIP that deliberately left out two classes of thing: first, anything that would
**compromise safety** (secrets, our own security posture), and second, anything that would **compromise research
validity** (our answers, the experiment's log, the "you are the subject" tells). We ran a separate review for
each. The model's limitation became our control surface.

### The friction is the sovereignty

That control was not so much a lucky accident as a glimpse of a principle. Reach and control pull in opposite
directions.

**Data that a foreign system can freely crawl is data you have partly lost control of.** It reads what it likes, in
whatever order, and caches or trains on what it reads, and you were never in the loop. Data that the same
system can obtain only when you hand it over, in the form you choose, is data whose sovereignty you have kept.
You decide what goes in, what stays out, and what gets redacted on the way.

So the very unreachability that first looked like a loss, the same unreachability behind the earlier irony
about the mirror, turns out on second look to be a win. The friction is not in the way of the sovereignty. The
friction *is* the sovereignty. A thing that is effortless for an outside system to ingest is a thing you no
longer govern.

There is a plain working rule in this, well beyond our little experiment. 

*When you want an outside model to
reason about your material without contaminating a result or leaking what it should not see,* **the right default is NOT to let it browse at all.**

**Instead:** *Hand it a curated, redacted, audited artifact. Controlled context beats uncontrolled retrieval, for validity and for safety both.*

The moment access becomes frictionless, curation
becomes impossible, and curation is where both your safety and your honesty live.

### Fable-5 deep synthesis

**NOTE:** This sub-section is generated by Claude Opus 4.8 acting as super-agent in a sub-agent fleet spin. I am pasting it verbatim below.

> The Fable-5 sub-agent read all three inputs and, as the deepest arm is supposed to, argued with the headline
> rather than nodding along. Its sharpest move was to reframe the whole episode.

> **It was a disclosure failure, not an access failure.** The easy story is "the model without the substrate
> confabulated, the model with it did not, so access is the differentiator." True, but it lets ChatGPT off too
> lightly. ChatGPT *had the signal of its own blindness*: its fetch returned an error it could see, and it still
> answered "after very deep investigations." A model that had simply said "I could not read the repo, here is a
> guess from the name, flagged as a guess" would have been fine without access. So the reliable fix is not only
> "carry the substrate across." It is to **demand, and verify, that the model discloses what it could and could not
> read before it answers.** A validator that abstains honestly is useful even blind. A validator that confabulates
> is worse than nothing, because that round-1 answer nearly went on the record as an outside assessment of novelty.

> **The legibility trap runs through the same channel as the flattery.** This is the bind the sub-agent named that
> I had not seen. To make genscalator legible to a skimming reader you reach for a memorable category, and the
> memorable categories are exactly the grand ones ChatGPT confabulated. Worse: any description clear enough to
> convey the novelty also carries the project's own opinion of itself, and models defer to the context they are
> handed. So a better README does not buy you an independent check. It buys you a better-informed agreement, which
> is precisely what round 4 turned out to be. The only real escape is a description the project did not write: an
> independent restatement, a hostile review, a blind arm. Which is, of course, what peer review has always been for.

> The working rules that fall out are cheap. When you use an outside model to judge your work, **ask it to describe
> the thing, and to state what it could not read, before you ask it to evaluate anything** (that one turn would have
> caught round 1). And never score a model's agreement as evidence when you were the one who fed it the description.

> The sub-agent then designed the controlled follow-up this messy pilot earns: fresh sessions, several model
> families, and a third condition where the facts are supplied in flat, claim-free prose written by a non-author,
> so you can tell whether the flattery comes from the facts or from the framing. It also did the thing the whole
> exercise is about. It named the prior art the rest of us had missed, from `llms.txt` for machine-readable project
> summaries to sixty-year-old work on [demand characteristics](https://en.wikipedia.org/wiki/Demand_characteristics)
> in experiments, and it discounted its own un-flattering tone as partly commanded by its brief. Even the deepest
> arm flagged that it, too, had been reading the project's own account of itself.

## Why is this interesting? 

Here are the main reasons this small experiment carries more weight than a party trick:

- The echt test: can a model give an un-flattering answer about the project it is embedded in?

- Cross-model contrast as a cheap probe of how these systems reason, and of what they each treat as "novel".

- The result matters for the project itself: if the honest answer is "the novelty is method, not technology,
  and transfer is unproven", that is a research agenda, not a disappointment.

## Further reading

- **Tracking Capabilities for Safer Agents** (the TACIT paper), by *Martin Odersky, Yaoyu Zhao, Yichen Xu, Oliver Bračevac, Cao Nguyen Pham*, [arXiv:2603.00991](https://arxiv.org/abs/2603.00991) (View [pdf](https://arxiv.org/abs/2603.00991)). 
  - This is the capability-tracking and capture-checking work that perhaps in the future can make help genscalator's goals being realized to a fuller extent: *if **proven** safe then   no need to prompt for human approval thus avoiding confirmation fatigue*.

- **Defeating Prompt Injections by Design** by *Edoardo Debenedetti, Ilia Shumailov, Tianqi Fan, Jamie Hayes, Nicholas Carlini, Daniel Fabian, Christoph Kern, Chongyang Shi, Andreas Terzis, Florian Tramèr*, [arXiv:2503.18813](https://arxiv.org/abs/2503.18813) (View [pdf](https://arxiv.org/pdf/2503.18813)).

**A note on conflicts.** This post was written with Claude (Anthropic's Opus model), running on Anthropic's own
harness, and it weighs both Anthropic and a competitor (OpenAI's ChatGPT). So the main analyst here is not a
neutral party about its maker or its rivals. Read the argument on its merits, and weigh the source. For
completeness: I (BR) am also a member of a class-action copyright settlement with Anthropic.


