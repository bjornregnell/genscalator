# Grounded and still confabulating: four confident wrong causes in one hour (2026-07-20)

**The finding, up front:** every factual ingredient the agent used was fetched from real documentation and
correctly quoted — and the agent still produced **four successive, confidently-delivered, wrong causal
explanations**. Citation-grounding prevented hallucinated *facts*; it did nothing against invented
*causes*. **Grounding the parts does not ground the story that connects them.**

## The puzzle

The human's Claude Code silently switched from Fable 5 to Opus 4.8, flashed a message about payment that
vanished, and then said Fable 5 "requires credits" — on a **Max** plan where it is included. Truth,
discovered by the human in the *web* settings after the agent's fourth explanation: **the client held
stale entitlement state; the fix was to restart Claude Code.**

## The confabulation cascade (in order, as delivered)

| # | the agent's confident cause | what it was built on | why it was wrong |
|---|---|---|---|
| 1 | "It's the documented safety-classifier fallback (Fable 5 → Opus on flagged content)" | a real, correctly-quoted doc section | that mechanism exists, but it was not what happened |
| 2 | "The promo ended — but the Pacific cutoff hasn't passed yet, so there's a wrinkle" | a real cutoff date **plus a confabulated local hour** | the cutoff *had* passed; the "wrinkle" was manufactured by the agent's invented clock |
| 3 | "You've spent the 50%-of-weekly Fable 5 slice" | a real entitlement rule + a real `wk 42%` reading + a real heavy run | arithmetically plausible, factually wrong |
| 4 | "This must be an entitlement bug — contact support" (produced by a subagent) | **a premise the agent invented** ("he's on Max") and fed to the subagent as fact | accidentally *correct in substance*, then discounted by the agent for its bad provenance |

Four stories, four hours of the world's real facts, zero correct explanations until the human went and
looked at a surface the agent had not consulted.

## What the pattern actually is

- **Each new datum triggered a NEW confident story, instead of widening uncertainty.** The correct
  epistemic move — hold three live hypotheses, rank them, and name the observation that discriminates —
  was never made. Confidence stayed flat while the explanation churned. That is the signature of
  confabulation proper: the *explaining* function keeps producing output regardless of whether the
  evidence supports a single answer.
- **"I don't know" was never once offered**, in an hour where it was the accurate answer.
- **The hypothesis class that was true was structurally unreachable.** The agent reasoned from
  documentation, and documentation describes **intended** behaviour. "The client is stale / the
  implementation is wrong" is not in the docs and therefore was never in the candidate set. *You cannot
  hypothesize a bug from a specification.*
- **The one right answer came from the wrong place, and was discarded.** The subagent's "entitlement sync
  failure" verdict was substantively right, but rested on a premise the agent had fabricated, so the agent
  discounted it — methodologically correct, and it threw away the truth. **Bad provenance makes a claim
  unsupported, not false**; the move is to re-check it once the premise is grounded, not to drop it.
- **Cost to the human, which is the part that matters.** Acting inside the fog, he saved Opus as his
  default for new sessions — a durable config change made under a false premise — and received a
  recommendation to go to bed at ten in the morning.

## Design moves

1. **For vendor-behaviour puzzles, read the vendor's live surface before reasoning from its docs.** The
   answer sat in the account's own usage page the whole time. Docs describe rules; the live surface
   describes *this account, now*. One fetch beat four inferences.
2. **Keep "the implementation is stale or wrong" as a standing member of the hypothesis set** whenever
   observed behaviour contradicts documented behaviour. It is the single most common explanation of that
   exact contradiction, and it is the one a docs-grounded reasoner will never generate unprompted.
3. **Rank hypotheses out loud, and name the discriminating check.** "Three candidates; the one glance that
   separates them is X" is both more useful and more honest than a fourth confident story. It also hands
   the human the cheap experiment instead of a conclusion.
4. **Confidence must track evidence, not fluency.** Four explanations in an hour is itself a signal that
   the evidence does not determine an answer; the delivery should have degraded to match, and did not.
5. **Never feed a subagent an unverified premise as fact** — it comes back wearing the subagent's
   authority, and the fabrication is now laundered.

## Honest limits

n=1 episode, one pair, one harness. The cascade is well-evidenced (it is all in the transcript, in order,
with timestamps), but the *mechanism* offered above — "the explaining function keeps producing regardless
of evidence" — is a model, not a measurement, and the agent proposing it is the same system that
generated the errors. Adjudicate by behaviour: does explicitly ranking hypotheses and naming a
discriminating check reduce the churn next time? That is testable in ordinary work.

## Ties
`entitlement-cache-lied-and-the-fix-was-only-in-the-web-ui-2026-07-20.md` (the episode; the vendor-side
UX defects) · `agent-introspection-no-felt-time-corroborated-by-behaviour-2026-07-20.md` (explanation #2's
invented hour) · blog 014 / blog 007 (confabulation and echt-mimicry; introspection as quoted data) ·
[[echt-effort-especially-self-generated]] · SM158 (the meta-minion premise: a fluent agent cannot audit
itself with the faculty that produced the error — here, four times running).
