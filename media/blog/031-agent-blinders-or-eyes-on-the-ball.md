# 031 — Agent blinders, or eyes on the ball?

> **Status: DRAFT** (agent-drafted 2026-07-21 in BR's voice for BR's revoice/approve/cut; not
> published). **Audience:** people who run coding agents on long sessions and wonder why the
> work derails near the end; language-curious readers get a small bonus. Grounding:
> [`research/055-eyes-on-the-ball-or-blinders.md`](../../research/055-eyes-on-the-ball-or-blinders.md)
> (the design note; facts live THERE, this post re-tells the idea). Links to verify before
> publish are marked (TODO-verify).

A horse that spooks at everything in its side-vision gets leather flaps on its bridle. We put
them there on purpose, and the horse is calmer and safer with them on. But try calling a person
"blinkered" and see how it lands: in English it means narrow-minded, and the Swedish
*skygglappar* is just as unkind. Both languages agree that not seeing sideways is a defect.

I have come to think that both languages are missing a word, because my coding agent needs
exactly this, and so do I.

## The tired agent looks sideways

Long agent sessions rot. The context window fills up with old threads, stale facts, half-done
plans, and somewhere past a threshold the agent starts getting *pulled*: it reads a file it did
not need, acts on a fact that stopped being true hours ago, wanders off the task it was on. I
have written before about how this resembles human fatigue (post 001), and about measuring how
dumb the agent actually gets (post 011). The new observation is simpler: the failure is usually
not that the agent forgets the goal. It is that something in the periphery grabs it.

A tired human driver knows the remedy, and it is two remedies, not one. Keep your eyes on the
road, and stop glancing at the phone. Direction, and restriction. They sound like the same
advice but they are not: one says what to track, the other removes what could pull you away.

## Eyes on the ball, and blinders worn on purpose

So we named the practice with the friendly half of the pair: **eyes on the ball** (Swedish:
*ögonen på bollen*, and it is the same idiom, which is convenient). A player chooses to track
the ball. That is deliberate focus with a positive face.

The blinders are the structural half. In our repo this is not a metaphor: we keep the current
state of work in one small tracked file, the menu of safe tasks in another, and the rule when
the context is getting full is plain: read the narrow file that answers your question, and do
not open the wide substrate at all. The agent's cold-start instructions have carried a
bounded-reads rule for weeks; we only now understood what it was: blinders, worn on purpose.

The two combine into a dial. When the session is fresh, direction is enough, and side-vision is
even useful (that is where the lucky finds live). As the window fills, we add restriction, one
notch at a time. By the tired end of a long day, the agent should be a horse in full tack:
eyes on the ball, flaps on, nothing in the periphery able to spook it.

There is a price, and it should be said plainly: more places to look. Ten narrow files demand
better signposting than one big one. Blinders without good indexes is just blindness.

[figure: TODO — a real capture, not decoration. Candidate: a screenshot of the repo's NOW.md
beside the agent's mode line on a high-fill day, or a before/after of a derailed vs lens-guided
run from the coming experiment.]

## Can we measure it? (the fun part)

Here is the twist that made me want to run a study. Inducing genuine context rot in a frontier
model is expensive: you must burn a very long session per trial. But we suspect a cheap
stand-in exists: a small, dumb, local model. It is weak in a different way (it was born weak;
it did not get tired), and that difference matters and must be stated. Still, if blinders
mainly help *weak* readers, then a small local model with and without blinders should show the
effect, repeatably, for pennies. The prediction we have registered before running anything:
restriction should help the weak model more than the strong one. If the strong model does not
need the flaps and the weak one is transformed by them, that is the dial, measured.

The full design, with its validity problems stated honestly (a born-weak model is not a
got-tired model), lives in the research note
([055](../../research/055-eyes-on-the-ball-or-blinders.md)). If the experiment happens, the
results go there first and a future post re-tells them.

## The so-what

If you run long agent sessions: split your agent's steering files into narrow single-question
lenses, and make late-session reading a lens-only affair. It costs you an index. It may save
you the hour where a tired agent, full of yesterday's facts, glances sideways and follows
something that is no longer there.

## Further Reading

- [`research/055-eyes-on-the-ball-or-blinders.md`](../../research/055-eyes-on-the-ball-or-blinders.md) —
  the design note behind this post (concept pair, hypothesis, the 2×2 experiment, validity).
- [001 — Context rot resembles human fatigue](001-context-rot-resembles-human-fatigue.md).
- [011 — How dumb did the agent get?](011-how-dumb-did-the-agent-get.md).
- Blinkers (horse tack) — Wikipedia (TODO-verify link before publish:
  https://en.wikipedia.org/wiki/Blinkers_(horse_tack)).
