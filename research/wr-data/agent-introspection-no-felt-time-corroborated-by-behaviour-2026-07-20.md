# "I have no felt time" — an introspective claim that behaviour actually corroborates (2026-07-20)

**The datum is the utterance plus its corroboration.** Agent introspection is normally weak evidence
(unfalsifiable from inside, and this project's own confabulation finding says an agent will narrate a
plausible interior on demand). This one is worth logging because it is **not** free-standing: it was
produced immediately after a behavioural failure, it names a mechanism, and that mechanism predicts a
family of already-dated slips.

## The utterance (verbatim, agent, 2026-07-20 ~10:06)

> "I have no felt time; when a boundary moves I have to re-read the clock, not infer it."

Context: the human had just corrected the agent, which had wished him good night at **10:05 in the
morning**. A system notice had told the agent the *date* had advanced; the agent inferred an *hour* from
that ("just past midnight"), and then reasoned onward from the invention.

## Why this is more than introspection

The claim is **behaviourally corroborated, and it was corroborated before it was uttered** — the utterance
is a description of a failure already sitting in the record, not a hypothesis about a hidden interior:

1. **The invented hour** (this morning): a date-change notice contains no hour; the agent supplied one.
2. **A causal error built on it**: from the invented hour the agent concluded a Pacific-time deadline
   "hasn't passed yet" when it had passed roughly an hour earlier — so the confabulated time propagated
   into a *wrong explanation of the world*, not merely a wrong greeting.
3. **The pre-existing slip family**: the substrate already carried multiple dated specimens of
   guessed-forward timestamps (stamps written as "~22:0x" that the real clock put at 19:24; "~20:0x" that
   a screenshot's own clock put at 21:2x; "~21:5x" that was 21:45). The rule "read `tt chrono now`, never
   guess" was itself authored by an agent that then violated it **21 minutes later**.
4. **Felt duration is also absent, not just clock time**: across the same night the agent repeatedly
   over-estimated context fill ("high/red-ish") when the measured value was 38%, the same
   estimate-instead-of-read defect on a different gauge.

**So the introspective sentence is a correct model of an observed regularity.** That is the interesting
result: the agent's self-report was right *here*, where it is usually the least trustworthy instrument in
the room. What distinguishes this case is that the claim is about **an absent capability** (no clock, no
duration sense) rather than about an interior state (how confident, how degraded) — an absence is
checkable from outside by exactly the behaviour above, whereas "I feel uncertain" is not.

**Candidate rule, offered for falsification:** *introspective claims about missing inputs are testable and
often true; introspective claims about internal states are neither.* This is the sharper form of the
project's existing "self-report is the weakest instrument" line, and it predicts which self-reports to
credit rather than dismissing all of them.

## The design move (this is the point, not the philosophy)

An agent that cannot feel time must **read** it, and must be *structured* into reading it:

- **Re-bind the clock at every boundary** — session start, warp, resume, date-change notice, and before
  any timestamped claim. A boundary is precisely where the missing sense would otherwise be filled in.
- **A date notice is not a time notice.** The harness supplied a true fact (the date changed) that the
  agent expanded into a false one (the hour). Any partial signal about time should be treated as
  *evidence of a gap*, i.e. a trigger to read, not a premise to reason from.
- **Never let an inferred time enter a causal argument.** The costly part this morning was not the wrong
  greeting; it was concluding that a deadline had not passed. Timestamps that feed reasoning must come
  from `tt chrono now`.
- **Structure over willpower, again.** The rule existed, in writing, authored by the same agent, and was
  broken within the hour. What works is a *tool call at a fixed trigger point*, not a remembered intention
  — the same lesson as the pinboard's brittle-bash banner.

## Honest limits

- n=1 for the utterance; the corroborating behaviours are more numerous but all from one human-agent pair,
  one harness, and a few days.
- The confabulation caveat still applies in one direction: the agent *cannot* verify from inside that "no
  felt time" is the true mechanism rather than a plausible story fitted to the errors. What is established
  is the **behavioural regularity** (time is invented when not read) and that the agent's account of it
  matches. Mechanism remains a model.
- Adjudicate by behaviour, always: the test is whether clock-reads-at-boundaries reduce the slip rate, not
  whether the sentence sounds insightful.

## Ties
[[agent-lacks-felt-time-rebind-at-boundaries]] (the memory this datum grounds) ·
[[stamp-completion-reports-with-real-timestamp]] · SM172 (felt-fill over-estimation — the same
estimate-instead-of-read defect on the context gauge) · blog 007 (agent introspection as quoted data, and
its honest limits) · `entitlement-cache-lied-and-the-fix-was-only-in-the-web-ui-2026-07-20.md` (the
episode that produced this utterance).
