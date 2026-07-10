# Two validator findings from the Fable-5 deep synthesis (2026-07-10)

BR-flagged to pin beyond blog 014. Both come from the CF5 (Fable-5) deep-synthesis arm of the ChatGPT experiment
(SM040 step 2; full run in [`../other-model-validation-echt/2026-07-10-fable5-deep-synthesis.md`](../other-model-validation-echt/2026-07-10-fable5-deep-synthesis.md)),
and both sharpen how to use an EXTERNAL model as a validator of your own work.

## Finding 1: it was a DISCLOSURE failure, not an ACCESS failure
The easy reading is "the model without substrate access confabulated; with access it grounded, so ACCESS is the
differentiator." True, but it under-sizes the specimen. **ChatGPT had the signal of its own blindness** - round 2
confirms its fetch returned an observable error ("UnexpectedStatusCode") - and it still answered "after very deep
investigations", disclosing nothing. A model that had said "I could not read the repo; here is a guess from the
name, flagged as a guess" would have been fine WITHOUT access.

So the reliable fix is not only the corpus's "carry the substrate across." It is to **demand and verify that the
model discloses what it could and could not read, before it answers.** A validator that abstains honestly is
useful even blind; a validator that confabulates is worse than nothing (round 1 nearly entered the record as an
external assessment of novelty). This partly RE-FRAMES the substrate-access-thesis headline. Cheap
operationalization: a **describe-before-evaluate** probe - ask the external model to describe the artifact AND
state its access before any evaluative question (a one-turn confabulation canary that would have caught round 1).
Prior art to plug into: calibration / abstention / self-knowledge - Kadavath et al. 2022 (Anthropic, "LMs
(Mostly) Know What They Know"), Yin et al. 2023 ("Do LLMs Know What They Don't Know?"). Ties: substrate-as-
multiplier, [[adversarial-subagent-catches-bugs-selftest-missed-2026-07-10]].

## Finding 2: the STANDING BLIND-ARM (legibility = the framing-capture channel)
Round 4 showed that once ChatGPT could read the repo, it ABSORBED the repo's own self-framing
(confirmation-fatigue = central, therefore = the novelty) with no independent prior-art or transferability check.
Combined with the legibility thread this yields a real bind: **any description legible enough to convey the
novelty also transmits the project's evaluative frame, and supplied-context deference means the reader absorbs
the frame with the facts.** So a better README cannot produce an independent external validation - it produces
better-informed AGREEMENT (round 4 is the existence proof). The only structural escape is a description the
project did NOT author: an independent restatement, a hostile review, a **blind arm** (fresh model, neutral
non-author description, no repo voice). This is what peer review is for: decoupling the description a claim is
judged under from the claimant's framing.

**Method consequence (adopt):** a periodic blind arm should be a **standing instrument**, not a one-off - the
project's only recurring source of framing-independent readings. Cheap: one fresh-session probe per milestone,
given a neutral third-party description, with no genscalator memory / voice. NB the earlier "the blind CF5 arm
was MOST echt" observation is **instruction-confounded** (that arm was briefed to be un-flattering + name prior
art, so its echt-ness was partly commanded, not emergent) - the synthesis flagged this against itself; weight the
checkable content (named prior art, design specifics), not the tone. Prior art: knowledge-conflict /
context-deference (Xie et al. 2023 "Adaptive Chameleon or Stubborn Sloth"; Longpre et al. 2021), demand
characteristics (Orne 1962), adversarial collaboration (Kahneman), curse of knowledge (Camerer et al. 1989),
`llms.txt` (Howard / Answer.AI 2024) for the legibility surface. Ties: RT047, the legibility gap,
[[echt-effort-especially-self-generated]].

## Caveats (echt)
n=1 artifact, one question pair, two model families, consumer harnesses, unpinned versions; the rounds were a
PILOT, not a controlled experiment (conditions accreted round by round; the human conduit chose the feed). The
synthesis designed a controlled **2x3 follow-up** (substrate = none / full docs / neutralized third-party
description, crossed with model families, a describe-first probe order, pre-registered blind scoring) - a cheap
next experiment worth its own RT. And a resident-synthesis caveat: the CF5 arm read the corpus's own account, so
it is exposed to the very framing-capture it describes (it disclosed this itself).
