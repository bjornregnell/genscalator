# Adversarial sub-agent verification caught bugs the author's own tests missed (2026-07-10)

BR-flagged WR datum, and BR's framing: **"a nice example of joint work."**

## The event (checkable, against the commits)
Building markdown-footnote support for `ssg` (BR: "add ssg footnote support and test it with sub-agents"):
1. The super-agent (CO4) implemented the feature and wrote **6 unit tests**; they passed and the **full toolbox
   suite was green** (43 SsgSuite tests) - by the author's own lights, done and verified.
2. Per BR's instruction, a **Fable sub-agent** was then given an **adversarial** brief: *construct tricky footnote
   inputs and try to BREAK it.* It ran 8 edge-cases and found **2 REAL bugs the author's tests had missed**:
   (a) stacked `[^id]:` definition lines with no blank between them merged (MdParse joins them) - the second def
   swallowed + a phantom "missing footnote"; (b) repeated references to one footnote emitted a **duplicate HTML
   `id`** (invalid HTML).
3. Both fixed + covered with 2 new unit tests; suite green at 45 SsgSuite (`c7ed271` feature, `9b0cbda` fixes).

## The lesson
**Delegated ADVERSARIAL verification beats self-testing alone.** The author tests what they *thought of*; an
independent verifier with a *break-it* goal tests what the author **didn't** think of. Notably this held even
with the **same model family** (Fable sub-agent, CO4 author) - the gain was **independence + an adversarial
objective**, not a smarter model. The blind spot is structural (you can't test the case you didn't imagine), so a
second agent tasked to refute is a cheap, high-yield net. Generalizes the adversarial-verify / refuter-panel
pattern down to routine feature work, not just high-stakes review.

## Why it's "joint work" (BR's point)
The capability was in the **pair, not either agent**: the super-agent's build+confirm plus the sub-agent's
adversarial break caught what neither did alone - **coupled-system capability at the agent-fleet level** (the
sibling of the human↔agent coupled-system finding, and of the ChatGPT-experiment's triangulation where
independence correlated with echt-ness). And the **human closed the loop**: BR's "test it with sub-agents"
instinct is what triggered the adversarial pass - a human process-choice that materially improved the artifact.
Ties: [[delegation-dance]], [[cue-use-fleet]], [[coupled-system-capability]] (if pinned; else foundations
"Coupled-system capability"), the adversarial-verify pattern, `research/024-agent-affective-analogs.md` (echt).
