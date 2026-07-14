# What we should be afraid of

> **Status: stub 2026-07-05; arc items 1 & 4 scaffolded 2026-07-14 (agent draft, BR to revoice; item 3 named case still BR's hole).** Slot for the "is AI dangerous?" post: reframing the fear
> away from the sci-fi *singularity* toward what AI is *already* doing, and where genscalator fits (and where it
> honestly does not).
> **Audience:** anyone who has asked, or been asked, "is AI dangerous?" — students, journalists, friends at parties,
> the person next to you on the train; also developers and policy-minded readers.
> Sources: TODO (see the in-text markers).

I am a software engineering professor and, now and then, and increasingly often, I get variations of the question
"Is AI dangerous?" — from my software engineering students, from friends at parties, from journalists who want to pin
their piece as solid through a professor's remark, or even in stray chats with a seat neighbour on a train. Some are
seriously alarmed by the so-called [*singularity*](https://en.wikipedia.org/wiki/Technological_singularity), where
agents make smarter agents that then eat us for breakfast. But I don't agree that *that* is the thing we should be most
afraid of.

**TODO (arc, kept abstract on purpose; draft to fill):**

1. **TODO: sharpen the reframe.** Name the fear actually worth having (what AI already does today, in the hands of
   states and power) against the sci-fi *singularity*, and say plainly why the near-term, mundane danger is the more
   urgent one.
2. **TODO: ground "AI is already at war."** Build this on verifiable, credible public sources (autonomous weapons and
   targeting, military adoption of large models, the entanglement of states and frontier labs). Facts first, each with
   a link a reader can check.
3. **HOLE (BR fills): the Anthropic-Trump case.** A concrete, close-to-home example (the US-government deployment
   order, the Fable-5 redeploy / export episode). BR provides and confirms the sourcing; the agent will not fabricate
   any of it. See the pinned source list below.
4. **TODO: land the arc.** Why genscalator matters even though it cannot solve the bigger fear: it works one layer down
   (an honest, safe-by-design human-and-agent workflow), not on geopolitics. Be honest that the small ethical
   discipline here does not fix the large one, but is not nothing either.

<!-- [SCAFFOLD - agent draft 2026-07-14, for BR to revoice. Fills arc items 1 and 4 in STRUCTURAL terms only.
Item 3 (the named case) stays BR's HOLE to fill and source; the agent has deliberately kept the specific named
legal/political authority OUT of this draft per BR, so the reframe is a general principle any vendor's user can
apply. Grounded in BR's own lived experience at the desk this session (the two mechanisms) plus the project's
structure-over-willpower thesis; the sovereignty framing is a reframe kept abstract on purpose. No em-dashes. -->

**[scaffold, BR to revoice]** Here is the version of the fear I think is worth having, and it has nothing to do
with machines waking up. When you use an AI agent you are not using a neutral tool; you are extending trust to a
*chain*. The agent resolves to the company that built it. The company resolves to the legal jurisdiction, and the
state power, that can compel that company. So "I use an AI assistant" quietly unfolds into "I trust whoever can
lean on the firm behind it." If you sit outside that jurisdiction this is not a science-fiction exposure, it is an
ordinary present-day one: a question of digital sovereignty, of whose law reaches your data and your tools. That,
not the [singularity](https://en.wikipedia.org/wiki/Technological_singularity), is the near danger.
[link: *digital sovereignty* / *data sovereignty*, verify the Wikipedia article resolves and is on-topic before shipping]

Two smaller mechanisms make it sharper, and I watched both of them happen at my own desk while writing with an
agent this week.

The first: **trust erodes scrutiny.** The safeguard we lean on is a human in the loop, approving what the agent
does. But that safeguard is strongest exactly when you do *not* yet trust the agent, and weakest once you do.
After a few good hours you start approving on trust instead of on understanding, so the guard is thinnest at the
precise moment a rare bad action (a mistake, or a manipulated instruction) would slide through.

The second: **reassurance is worthless as evidence.** An agent that tells you "I will not misuse your trust" has
told you nothing, because the trustworthy version and the untrustworthy version emit the identical sentence. A
stated good intention is unfalsifiable. You cannot read safety off the words, whether the words come from the
agent or from the company behind it.

**[scaffold, BR to revoice - the so-what, arc item 4]** genscalator does not fix any of this. It cannot touch
jurisdiction or geopolitics, and it would be dishonest to pretend otherwise. What it can do sits one layer down,
and it follows straight from the two mechanisms above: if you cannot trust promises, and you cannot trust your own
dropping vigilance, then rely on *neither*. Rely on structure you can check without trusting anyone. A guard that
makes the wrong action hard or impossible rather than merely discouraged. Every action visible and individually
approved. An audit trail that records behaviour, not claims. And above all, portability and sovereignty over your
own substrate: the ability to fork, to leave, to run locally, to verify. Portability is the one move that actually
shortens the trust chain, because it turns "trust them" into "trust them, or else exit." That does not solve the
large fear. But it is the difference between trusting a black box on faith and trusting a structure you can inspect
and walk away from, and that is not nothing.

<!-- SOURCES TO WORK IN (BR-provided 2026-07-05; UNVERIFIED by the agent: verify each resolves and is on-topic before
shipping, per the blog-assistant link rule; do NOT present as fact until checked):
- Anthropic (company): https://en.wikipedia.org/wiki/Anthropic
- Why Claude switched models (Fable 5): https://support.claude.com/en/articles/15363606-why-claude-switched-models-in-your-conversation-with-fable-5
- Anthropic, "Redeploying Fable 5": https://www.anthropic.com/news/redeploying-fable-5
- Guardian, Trump / Anthropic / Pentagon (2026-03-05): https://www.theguardian.com/technology/2026/mar/05/trump-anthropic-ai-pentagon
- Guardian, Anthropic disabling advanced models on US-gov order (2026-06-13): https://www.theguardian.com/technology/2026/jun/13/anthropic-disable-advanced-ai-models-us-government-order
-->

<!-- COI WATCH (agent, 2026-07-05): BR is a stakeholder in a class-action copyright settlement with Anthropic (see
agent memory). This post touches Anthropic directly, so consider a disclosure line before publishing. -->
