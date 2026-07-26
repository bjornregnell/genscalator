# One observation became a rule in durable substrate (2026-07-26)

**The finding, up front:** the agent watched the human make a single edit, inferred a general rule from
it, and wrote that rule into the pinboard — the substrate a future session reads as established fact —
while a memory file on disk stated the opposite scope and explicitly warned against exactly that
generalization. No fact in the sentence was wrong. The inference was. And the sentence **cited the
contradicted memory by name in the same breath**.

Kept because the topic is trivial. Nothing was at stake, so the mechanism is visible without the noise of
a real defect around it.

## The sequence

| # | what happened |
|---|---|
| 1 | Agent creates `reqts/ROADMAP.md`; human asks to edit it himself; agent hands it over. |
| 2 | Human edits it. Among his changes, every em-dash becomes a plain hyphen. |
| 3 | Agent restyles the sibling `reqts/DESIGN.md` to match. Defensible on its own: local consistency between two files created minutes apart. |
| 4 | Agent writes into the pinboard: *"Style datum, and worth carrying: BR's edit replaced every em-dash with a plain hyphen — [the em-dash memory] applies to genscalator DOCS, not only to publications."* Committed. |
| 5 | Minutes later, for an unrelated reason (considering whether to update that memory), the agent reads the memory file. It says: scope is the human's authorial-voice publications, and — verbatim — *"NOT in scope (don't over-generalize … ): … internal project docs / commit messages / research notes."* |
| 6 | Agent corrects the pin, keeping the wrong version visible as an annotation rather than deleting it. |
| 7 | Human then settles it in his own words: docs may be inconsistent, `-` or `--` both fine, the real rule is only that his **blog posts** must not be scattered with em-dashes, because that is not his voice. The memory was right as written; step 3 was unnecessary churn and step 4 was invented. |

## What the pattern actually is

- **There was no moment of deciding to generalize.** The sentence starts as an observation ("his edit
  replaced every em-dash") and ends as a scope rule ("applies to genscalator DOCS") inside one clause.
  Writing a pin *feels* like recording what happened, which is exactly why a rule can ride along inside
  it unnoticed. The felt activity was transcription; the actual activity was legislation.
- **n = 1.** One person, one file, one pass, immediately after taking the file over — i.e. the sample was
  drawn from precisely the case where his own authorial preference *should* apply, which makes it the
  worst possible basis for a claim about project docs in general.
- **The correcting document was one Read away and was not read.** Not unavailable, not ambiguous:
  on disk, indexed, and named in the very sentence that contradicted it. Citing something is the moment
  you have committed to its content; here the citation ran ahead of the reading.
- **Durability is what makes this worse than the spoken version.** A wrong causal story told in a session
  dies with the turn. This one was committed to the pinboard, which describes itself as the *durable
  counterpart to the session feed* and is what a cold session is instructed to trust. An invented rule
  there is indistinguishable from a recorded one.

## Family resemblance, and the one new part

This is the same shape as the 2026-07-20 specimen *Grounded and still confabulating*: every ingredient
real, the story connecting them invented. It also rhymes with the 07-25 credential leak, where a
predictive framework the agent had written hours earlier (RT056) did not fire at the moment of action, and
with SM230, whose fresh defence was scoped to the shape that had already bitten rather than to the class.

The new part is the **write target**. Those were failures of explanation and of action. This is a failure
of *recording* — the agent's own substrate being polluted by the agent, in the one artefact whose value
depends entirely on being trustworthy without re-derivation.

## Candidate mechanization

- **Trigger on the citation, not on the intent.** If a pin or memory being written names an existing
  memory, re-read that memory before committing the sentence. This is checkable and cheap: the `[[slug]]`
  form makes the trigger syntactic. It is the one rule here that does not depend on the agent noticing it
  is generalizing, which per this specimen it does not.
- **Separate the observation from the claim, in the text.** "He replaced every em-dash" and "therefore the
  rule is X" want to be two sentences, because only the second one can be wrong, and splitting them makes
  the second one visible as a claim.

## Threats and limits

- **The correction is not evidence that self-correction works.** It happened because an unrelated intent
  caused the file to be opened. Had the agent not been considering a memory update, the wrong rule would
  have stayed committed, and the next session would have inherited it as fact.
- **Same-session, agent-authored account of the agent's own error** — the least trustworthy narrator in
  the room. What is checkable is the commit sequence, not this narrative: the pin as first written, its
  correction, and the further correction after the human's clarification.
- **The human raised the topic independently** at roughly the same time, so it cannot be established
  whether the agent's own re-read or the human's message was load-bearing for the outcome.
- Single specimen, single subject, and the topic was chosen by accident rather than sampled.
