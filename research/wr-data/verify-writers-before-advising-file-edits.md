# Verify writers before advising file edits (naming conventions are a hint, not proof)

2026-07-21 11:53 (clock-read). Human-catch specimen from the introprog
kompilera-warning investigation; BR is the catcher.

## What happened
Investigating an sbt build warning (duplicate concept in quiz/quiz-concepts.tsv),
the agent advised "delete line 42" — a hand-edit to a file that turned out to be
GENERATED (quiz/Main.scala:62 writes it from QuizData.scala). BR challenged
("BUT isnt quiz-concepts.tsv generated??"); a writer-grep confirmed him in one
command. The follow-up dig also overturned the diagnosis itself: the two
kompilera definitions are deliberate cross-week quiz content (w01 abstract, w02
concrete), correctly canonicalized by the glossary generator — nothing was stale.

## The agent-side lesson (verification order)
The agent had cited the repo's "-generated" naming convention as evidence the
file was source ("no -generated suffix, so hand-maintained") — using the
convention as PROOF instead of a hint. The writer-grep that settled it was
one cheap command available BEFORE advising. Rule: before advising any file
edit, grep for the file's writers (save/write/filename in code); a naming
convention ranks below a writer search in evidence strength.

## The repo-side root cause + fix-forward
BR: "i should have named it -generated.tsv" — the convention exists precisely
so names can carry generated-ness, and this one file predated/escaped it,
breaking the chain for both humans and agents. BR filed
https://github.com/lunduniversity/introprog/issues/953 to rename it per the
convention. Both fixes stand and compose: repo names generated files so the
hint is reliable; agent still verifies writers so an unreliable hint cannot
mislead.

## Wider read
Second retraction-by-annotation in the same task file (after the tsv advice
correction) — the keep-the-ball-game norm made the wrong path cheap to
document and the human challenge cheap to honour. The specimen pairs with
[[summaries-enumerate-dont-totalize]]-style inherited-claim risks: here the
inherited claim was the agent's OWN earlier inference, re-served as fact.
