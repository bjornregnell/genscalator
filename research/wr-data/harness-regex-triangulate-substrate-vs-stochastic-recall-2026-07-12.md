# WR specimen: harness should regex-triangulate substrate FACTS, not stochastically recall them (BR hypothesis, 2026-07-12)

**BR's hypothesis.** "You are far better than me on regex - why not use it when better than stochastic inference?
... the harness could have helped you triangulate by actual regex what you are looking for in substrate."

## Verdict: right in the main (close to the genscalator core), with one nuance
- **Where it wins - substrate FACTS.** For anything CHECKABLE in the substrate (file contents, git state, usage
  numbers, timestamps), a DETERMINISTIC regex/grep (`tt text grepr/match`, `git`, `tt chrono`) beats the agent's
  stochastic recall, which is confabulation-prone (the post-hoc thesis). "Go look, do not guess" is exactly this,
  and the RQ0 VERIFY layer already embodies it: those agents CONFIRMED verbatim quotes by reading the files, not by
  trusting recall. A harness that pushes regex-triangulation over recall - and makes NOT-checking harder - is
  squarely the genscalator / super-harness thesis (deterministic tools where they beat inference; cf. Venners
  "quality from GenAI").
- **The nuance - the misread was NOT a fact-lookup.** The "context dance -> compact dance" slip was INTENT parsing
  (what did an ambiguous human phrase MEAN), not a substrate fact. Regex cannot disambiguate meaning - there is no
  deterministic rule for intent. So that failure's cure is the CONFIRM step (the live-edit "do what he means +
  confirm on ambiguity"), not regex.
- **Two failure modes, two cures.** (a) Confabulating about a CHECKABLE fact -> deterministic triangulation (BR's
  hypothesis; a real harness-improvement direction). (b) Over-committing on AMBIGUOUS INTENT -> confirm before
  acting. The misread was (b); BR's hypothesis nails (a).

**Design takeaway.** The harness could/should make the deterministic-grounding path the path of least resistance for
FACTS (and flag when the agent is recalling a checkable fact instead of looking) - a concrete super-harness feature.
Intent stays stochastic + confirmed.

Ties: genscalator thesis (deterministic tools > stochastic inference), the super-harness, the RQ0 verify layer,
[[introspection-is-post-hoc-reconstruction-not-privileged-readout-2026-07-12]], [[live-edit-dance]].
