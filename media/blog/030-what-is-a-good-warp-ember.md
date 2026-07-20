# What is a good warp ember?

<!-- Slug/number 030. Agent scaffold in BR's voice per the blog-assist skill; BR has NOT revoiced yet.
     Sources: SM168 joint pass 2026-07-19 (PB), work/EMBER-EXAMPLE.md, the P3b fidelity self-tests,
     research/wr-data 07-19 entries. Renamed baton -> warp ember 2026-07-20 (see wr-data note).
     Working notes at the bottom. -->

> **Status: drafted 2026-07-19 (SCAFFOLD - full draft written by the agent in BR's voice, every sentence
> awaiting BR's rewrite/approval; nothing here is published prose yet).**
> **Audience:** people running long-lived human-agent collaborations who face the handover problem;
> agentic software engineering practitioners; anyone curious how a research programme gets scoped down
> to one honest little experiment.

## Banking the fire

I work daily with an AI agent that has no long-term memory. (This blog is part of **genscalator**, my
project on making such human-agent collaboration safe and evidence-based; you need no more background
than that to read this post.) The agent's working memory is its *context window*: the finite text
buffer holding our whole conversation and everything it has read. Every so often that window is
cleared and I get a fresh instance: a model upgrade, a machine reboot, or a deliberate fresh start to
escape *context rot* (the well-documented quality slide as the window fills up). We call that jump a
**warp**. Everything the old instance held in its head is gone.

What survives is what we wrote down. And the trick we use at every warp is older than software: it is
what a blacksmith does at closing time. The smith does not keep the forge roaring overnight; the fire
is *banked* - ash raked over the coals so that a small ember survives until morning, when it is blown
back into flame. At every warp the outgoing instance writes a **warp ember**: a small file the
incoming one blows back into working context. The fire dies; the ember crosses.

(A naming confession: we first called this file a *baton*, after the relay race. Then Swedish caught
up with us - *batong* is what the police hit people with - and both English senses point wrong too, a
conductor's stick or a truncheon. The smithy metaphor was sitting in our own vocabulary all along: the
toolbox already has a tool called `tt forge`. Renamed, before any experiments had run.)

The one rule we are most sure about: an ember is a *pointer to durable truth, not the truth itself*.
The real state lives in committed files and memory notes; the ember tells the fresh agent where to
look, which reflexes to re-install, and what NOT to start doing on its own.

## The specimen that worked

<img src="../media/img/baton-example1.png" alt="Terminal screenshot: the fresh agent's session opening on the real warp ember - the pointer-not-truth header and the anti-regression checklist" width="900">

*Above: a real warp ember, exactly as a fresh agent received it after a machine reboot - from before
the rename, so its banner still says "baton". First the warning that it is a pointer, not the truth;
then, before anything else, a checklist of reflexes that regress at turn zero (the very first moments
of a fresh session, before the agent has read anything).*

We published this one verbatim in [EMBER-EXAMPLE.md](../../work/EMBER-EXAMPLE.md), because it worked:
the fresh agent reconstructed the working state with zero re-explanation from me. It cleared a stale
mode flag (a status label on the shared status display we both read), verified that yesterday's git
pushes had actually landed before trusting them, and correctly did NOT start a work queue I had put
on hold.

That is one success. One. Which brings us to the actual question.

## What we think makes it good (so far: reasoning, not evidence)

The agent and I did a joint pass over the design. Here is where we landed, and I want to be honest
about the epistemic status: this is two collaborators reasoning themselves into agreement, not
measurement. Our best current answers:

| what the ember carries | the failure it prevents |
|---|---|
| **a reflex checklist, first** - forbidden habit, then the allowed one | turn-zero regression: a fresh agent falls back to base habits before it has read anything else |
| **holds, each with an owner** - what not to start, and who lifts it | an eager fresh agent resuming work a human deliberately paused |
| **pre-authorized actions** - what it may do before anyone speaks | wasted round-trips, and its opposite: unauthorized initiative |
| **numbers, not adjectives** - commit hashes, counts, percentages | inherited claims that cannot be checked against git and disk |
| **verify-mandates** - each claim it will act on says how to check it | over-trust of inherited state (the ember says "verify before trusting", and means it) |
| **a substrate map, last** - where durable truth lives (the *substrate*: the written-down layer that survives warps - committed files, notes, logs) | the ember itself swelling into a copy of the truth |

Two design tensions came out of the pass. First: should the ember restate any state at all, or only
point? Every restated fact is a future stale fact - a claim living in two places drifts quietly false
in one of them, and nobody knows which. We have been burned by exactly that kind of prose rot before (see [022](022-brittle-bash-to-beautiful-scala.md) for the code-beats-prose version
of this lesson). Our tentative call: at most a three-line state summary, and each line carries its own
verify-mandate. Second: how much does each ingredient actually matter? We ranked them by gut feeling
from one lived episode. That is exactly the kind of claim that should embarrass an empiricist.

## Too many questions at once

Drafting this post, the research questions multiplied: what content, what order, what size, does the
type of warp matter (a fresh start knows it knows nothing, but an agent that survived a context
compaction carries a lossy summary that *feels* like memory and may over-trust it), can any of this be
measured on sub-agents (short-lived helper agents a main agent can spawn), what can no ember ever
carry, does it transfer to other pairs, should there be an ember-writing skill. Nine questions, each
with sub-questions and hypotheses. (A *compaction*, for the newcomer: when the window nears full, the
harness squeezes the conversation into a summary to free space - the agent survives, but its memory
of the session becomes that summary.)

There is a strong temptation to design one grand study that answers everything. In earlier work of
ours - the textbook on experimentation in software engineering that I co-authored - we spent whole
chapters warning against exactly this: pile every factor into one study and you confound all the
things, and learn nothing cleanly. The discipline is boring and correct: a SERIES of small
experiments, one factor at a time, everything else held fixed.

And there is a second, more personal reason to insist on measurement here. When the agent and I
finished the joint pass, agreeing happily with each other, I caught myself and said:

> "we confabulate into agreement :)"

Two reasoners who like each other's conclusions are not evidence. *Confabulation* is a term borrowed
from psychology: confidently filling a gap with plausible invented content, without any awareness of
inventing it. Applied to language models it means fluent, reasonable-sounding claims with nothing
underneath; applied to a human-agent pair it means something sneakier - each side polishing the
other's guess until it feels like established fact. The methods literature calls that a validity
threat; my co-authors would just call it skipping the experiment.

## So: a seed, and one small experiment

The honest conclusion of this post is therefore not "here is what a good ember looks like". It is two
much smaller deliverables:

**1. A pre-hoc seed** (pre-hoc: written down *before* any measurement, the opposite of rationalizing
afterwards). Our best-reasoned ember template - the table above, ordered checklist-first,
with the three-line verified state summary - written down as the thing to beat. It is a seed, not a
result: [EMBER-TEMPLATE.md](../../work/EMBER-TEMPLATE.md), living in the repo next to the real specimen.
This post deliberately does not duplicate its details, so it cannot drift out of date here.

**2. A pilot A/B test** (two groups, exactly one difference between them). One factor, the most
primitive one: *ember versus no ember*. Fresh agent
instances get the same reconstruction task on a frozen snapshot of a real repository; half receive the
seed ember, half only the durable substrate (the committed files and notes both groups share). We
measure behaviour, not self-report: cold-start actions done correctly, forbidden-habit relapses,
recall probes scored against a key by scorers who do not know which group they are grading. A pilot in the textbook sense: its job is as much to shake
down the instruments (does the task discriminate? do the metrics move?) as to estimate the effect. If
the total-ember effect is small, the finer questions are moot; if it is large, its size becomes the
yardstick that makes every later per-ingredient experiment interpretable. The next experiment in the
series then isolates the ingredient we believe in most, the reflex checklist.

## What no ember can carry

One finding from our informal fidelity probes deserves its own paragraph. We quizzed fresh agent
instances on our shared working vocabulary, twice, under different conditions. Both instances failed
the *same* probes: the items our written substrate simply does not cover. An ember cannot fix a gap in
the substrate it points to. Fidelity across a warp is bounded by what was ever written down.

And below that bound sits something an ember compensates for but cannot restore. Asked what is
genuinely lost at each context clear, the agent wrote (quoted as data, not as my view):

> "What dies at /clear is the weighting, not the facts. [...] My successor will re-read the same pins
> [entries on our shared pin board of decisions] and hold them all at uniform strength - flat, like a
> photograph of a relief map."

Whether that description is introspective truth or well-formed confabulation is itself one of the
things we cannot know from the inside - which is precisely why the template's answer is mechanical:
mandate re-verification of every claim the next instance will act on, and let the texture regrow
through work. The pilot will tell us whether even that much is measurable.

## Further Reading

- **Experimentation in Software Engineering** - Wohlin, Runeson, Höst, Ohlsson, Regnell, Wesslén
  (Springer). Our own book (I am a co-author); the series-of-experiments discipline and the validity
  vocabulary used above. *(Link via the typed bibliography before publish.)*
- [EMBER-EXAMPLE.md](../../work/EMBER-EXAMPLE.md) - the real, working warp-ember specimen, verbatim,
  with annotations of its design moves (pre-rename, so the specimen itself says "baton").
- [005 - Dancing with agents](005-dancing-with-agents.md) - the exit-resume dance this post's warp
  handover belongs to.
- [007 - Learning beyond the inference barrier](007-learning-beyond-the-inference-barrier.md) - why
  durable substrate is the only thing that crosses a warp at all.
- [011 - How dumb did the agent get?](011-how-dumb-did-the-agent-get.md) - an earlier, narrated
  experiment on the compact boundary; the warp-type taxonomy of RQ9 starts there.

<!-- ============ WORKING NOTES (agent), trim before publish ============
The nine RQs in full detail (RQ1-RQ9 with sub-questions) were drafted in this file's stub version;
they now live condensed in "Too many questions at once". Full versions: git history of this file +
the SM168 pin on the PB. Decisions this draft encodes (BR 2026-07-19, in-feed): series-of-experiments
framing; pilot = ember vs no-ember; tension A = three-line state summary WITH per-line verify-mandates
(BR: "but i am not sure"); BR quote "we confabulate into agreement :)" verbatim, ratified by use.
Renamed baton -> warp ember 2026-07-20 (BR call; batong connotation + smithy fit); file renamed from
030-what-is-a-good-warp-baton.md; the naming-confession paragraph is new and awaits BR's revoice like
everything else. Raw records (wr-data, minion logs, the specimen's verbatim section) keep "baton".
TODO before publish:
- BR revoices everything; check plain-word rule; check no em-dash glyph slipped in.
- Add the ESE book to blog/References.scala (Verified, with link) and cite from there; verify the
  three internal blog cross-links still resolve after any renumbering.
- Figure check: baton-example1.png in place (filename kept, pre-rename screenshot); consider a second
  real figure when the pilot runs (metric chart per the dataviz skill).
- Pilot preregistration: hypotheses, metrics, N, scoring key - as a research/ note, linked here.
============ END WORKING NOTES ============ -->
