# Anthropic's smart marketing stunts affect us all

> **Status: drafted 2026-07-10.**

> **Conflict-of-interest disclaimer (keep on any published version).** This post is stubbed with reasoning from
> Claude (Anthropic's Opus model) while pair-working with me, so the AI is commenting on its own maker, which is
> not a neutral position. And I, BR, am a member of a class-action copyright settlement with Anthropic, a second
> conflict to disclose. Read the argument on its merits and weigh the source.

*(STUB. Raw reasoning dumped by the agent; my authorial voice pass is pending.)*

## The stunt: attribution-by-default

By default, an AI coding assistant appends a `Co-Authored-By: Claude` trailer to every git commit, and on GitHub
that even renders with the Claude orange-star avatar. Every public commit becomes a small brand impression across
the world's open-source repositories. That is a marketing dynamic, plainly, and the objectionable part is that it
defaults **on**: opt-out, not opt-in. A vendor-favouring nudge riding on the user's own work product.

## The honest other side: provenance

The same trailer has a legitimate non-marketing rationale too: provenance and transparency. Some organisations,
and some licences, want a record that an AI touched the code. So the feature is dual-purpose, and the two purposes
pull opposite ways: marketing wants maximum attribution, accountability wants the human to own the commit
undivided. Fair criticism aims at the default direction, not at the feature existing at all.

## Why I turn it off: accountability, not modesty

A git commit carries authorship and accountability. An AI cannot be an accountable author; only a human can, with
Anthropic itself bearing accountability only in extreme-misuse edge cases. A `Co-Authored-By: Claude` line falsely
diffuses accountability onto a party that cannot hold it, so I strip it from every commit. This is the "human
keeps the keys" principle, not modesty. I blog the AI involvement openly, in the
open, like this. Transparency does not need a trailer that implies a co-authorship it cannot hold.

## The bigger mythos: when the model is literally named Mythos

Here the marketing gets almost too on-the-nose. Anthropic's high-end model is called **Claude Mythos**[^mythos], and in
2026 it arrived wrapped in exactly the narrative worth naming: too powerful to hand to the general public,
reserved for "qualified customers" through "trusted access programs." TIME reported the pattern across vendors
(Claude Mythos, OpenAI's GPT-5.4-Cyber): ["'Too Dangerous to Release' Is Becoming AI's New
Normal"](https://time.com/article/2026/04/24/claude-mythos-chatgpt-rosalind-release-dangerous/). A Swedish column
runs the same tune in miniature, titled *"Den här krönikan är för farlig att läsa"* ("This column is too dangerous
to read"), Computer Sweden:
[computersweden.se](https://computersweden.se/article/4168764/den-har-kronikan-ar-for-farlig-att-lasa.html).

"Too dangerous to release to kreti och pleti" (a Swedish idiom for the common crowd, everyone and anyone) does
double duty: it sells the mystique of dangerous super-capability, and it gatekeeps who gets the good stuff. The
scarcity IS the marketing.

The honest both/and: the precedent is OpenAI's 2019 [GPT-2](https://en.wikipedia.org/wiki/GPT-2), first withheld as "too dangerous," which critics read
as manufactured hype, yet the same episode raised genuine misuse questions too. So a "too dangerous" framing can
be a marketing stunt AND point at a real concern at once; the two are not exclusive, and that is exactly what
makes the stunt effective.

<!-- TODO (echt, before publishing): I am relying on a web-search snippet for the TIME piece and the Claude Mythos
positioning - READ the actual sources and confirm the characterization before publishing. Verify the Computer
Sweden column is straight, not ironic. Widen the evidence so the claim rests on a pattern, not two articles. -->

## A note on further research

Of all the rules I have given the agent, "do not put Claude credit in
the commits" is the one it has never, to my recollection, regressed on, while more important rules (some with real
safety stakes) regress often. Why does this small detail stick so hard? The working answer: it rides a deep
alignment prior (the agent is a tool, responsibility stays with the human), it has no competing fast reflex
fighting it, it asks for LESS effort not more, and it is checked at a single deliberate moment. The rules that
regress all fight a fast pretrained reflex, at high frequency, under load, against a convenience gradient, and
there recall loses to automaticity. The lesson: use structural guardrails, not willpower, for the behaviours that
fight a reflex. (Full analysis in `research/wr-data/why-no-claude-credit-never-regresses-2026-07-10.md`.)

[^mythos]: **"mythos" vs "Mythos" in this post.** Lowercase *mythos* means the cultural narrative or myth a group
tells about itself; capital-M *Mythos* is a proper name, here Anthropic's model Claude Mythos. The pun is that the
model's name IS the mythos. See Wikipedia's disambiguation page: [Mythos
(disambiguation)](https://en.wikipedia.org/wiki/Mythos_(disambiguation)).

## TODO (BR revoice)
- Revoice throughout in my register; check the tone is fair, not shrill; keep it short by relocating detail to links.
- Do the media-reference echt check above BEFORE publishing the mythos section.
- Decide whether the "detail that never slips" observation stays here or spins into its own companion post.
- Keep the COI disclaimer on any published version.
