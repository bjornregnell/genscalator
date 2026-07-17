# Three swings on one review claim — only the code-RUNNING ones were accurate. Source-reading gives confidence, not truth.

**2026-07-17, afternoon.** A single review claim (does introprog PR #943 break the English mirror?) was asserted
**three times**, each with full conviction, and **the two accurate verdicts both came from executing code; the two
confident ones from reading it were both wrong.** BR named it: *"three swings, and the only accurate ones came from
running code."*

## The three swings, in order

| # | method | verdict | correct? |
|---|---|---|---|
| **1** | **read the source** (`Main.scala` + `CodeGlossary.scala`) | *"render breaks 24 files, the PR is a regression, '0 model calls' is the symptom of a broken gate"* | ❌ **WRONG, and damning** |
| **2** | **ran a probe** (real `CodeGlossary.render` over the real corpus, no sbt) | *"render mangles 24 files IN ISOLATION — but I can't see if it survives `Code.translate`"* | ✅ **right, and correctly scoped** |
| **2b** | **ran a second probe** (real `Code.swedishish` + real `render`) | *"REFUTED my own swing 1: ~27 of the 45 come out CORRECT; the real bug is PARTIAL coverage, ~13 strings"* | ✅ **right** |
| **3** | **built it end-to-end** (sandbox minion, full `--all`) | *"the ~13 DO ship; model-repair launders one into fluent-wrong English; model calls 9→22 not zero"* | ✅ **right, and went further than the probes could** |

⚠️ **Swing 1 was not a careless read.** It was a *careful* one — it produced the **right hypothesis** (the glossary
is context-scoped, the call site is corpus-wide) and then **the wrong conclusion** (that the damage is fatal and the
PR is a regression), with **total confidence**, and it was **an hour from being pasted at an external contributor in
BR's name.**

## ⭐ The finding, stated once

> ### **Source-reading produces a HYPOTHESIS and CONFIDENCE. It does not produce TRUTH. Only running the code does.**
> And the confidence is the hazard: it is highest exactly when it is least earned, because a fluent read *feels*
> like understanding.

**Mechanism, and it is the same one this corpus keeps finding:** reading the source is a **resolution** ("I
understand what this does"). Running it is a **mechanical act** with an external verdict the agent does not author.
⇒ **the discriminator is not care, expertise, or freshness — it is whether the check has somewhere to EXECUTE.**
This is [[code-beats-prose-a-rule-fires-only-when-it-governs-the-object-of-attention-2026-07-17]] applied to
*belief formation*: a source-read is prose the agent tells itself; a run is code that answers back.

## Why swing 1 was so convincing (the anatomy of the false confidence)

- It **started from a true premise** (the scope mismatch is real).
- It **found real specimens** (`"hej"→"hisan"` genuinely happens under `render`).
- It **assembled them into a story** that was internally coherent and technically fluent.
- **Every step was checkable and checked — except the one that mattered**: whether `Code.translate` runs *after*
  `render` and repairs it. That single unchecked link inverted the verdict. **A chain of true facts with one
  unexamined joint produces a false conclusion that wears all the authority of the true facts.**

⭐ **This is the SAME shape as the meta-minion's push-1 "Catch A" and the SM129 stall count: a FALSE MECHANISM /
FALSE JOINT propping up (or inverting) a story built from true parts.** Third independent specimen of that class in
~24h.

## ✅ The counter-move that worked, twice, and is cheap

**`CodeGlossary` has no dependencies** (`Regex` only). So the probe needed **no sbt, no model, no mirror, no
minion** — `tt git show --out` to extract the file, ~40 lines of Scala to run its real function over the real
corpus, seconds to run. ⇒ **the fastest way to check a claim about a pure function is to RUN THE PURE FUNCTION**,
and the barrier to doing so is usually imagined, not real. **The agent reached for a 122k-token sandbox build
before noticing it could run the function directly in 40 lines.**

## The division of labour that emerged (none substitutes for another)

- **Isolated probe** (swings 2/2b): fastest, killed the false claim, but **structurally blind** to what happens
  downstream of the isolated unit.
- **End-to-end build** (swing 3, the minion): saw the **final output** and the **model-call delta** the probe
  could not, at ~150× the token cost and ~8 min.
- ⇒ **not redundancy — different instruments with different blind spots.** The cheap one refutes fast; the
  expensive one is the only thing that sees the whole pipeline.

## Honest limits

- **n=1 claim, one PR, one reviewer.** It is a clean specimen (four datable swings, verdicts on record) but it is
  **an argument, not a rate.** We do not know how often a source-read is *right* — selection bias: we noticed this
  one because it flipped.
- ⚠️ **Source-reading was NOT useless — it was NECESSARY.** It produced the hypothesis the probes then tested. The
  claim is narrow: **reading is for generating candidates; running is for verdicts.** Do not read this as "never
  read the source."
- **The "confidence is highest when least earned" line is a story about felt confidence**, which is
  unfalsifiable from inside (standing caveat). The **behavioural** fact is only: swing 1 was asserted as a verdict,
  not as a hypothesis, and it was wrong.

## Ties

[[code-beats-prose-a-rule-fires-only-when-it-governs-the-object-of-attention-2026-07-17]] (**belief-formation is
the same as rule-firing: prose the agent tells itself vs code that answers back**) ·
[[retracted-is-not-immune-i-remade-sm129s-claim-4h-after-retracting-it-2026-07-17]] (the false-mechanism class) ·
[[agent-cant-internalize-huge-codebases]] (**distill via probes, not by reading source — this is the sharp form:
even a read you CAN do is worse than a run**) · [[prefer-scala-scratch-over-bash]] (the probe was a Scala scratch
program) · **the meta-minion cycle-2 log** (the end-to-end swing lives there as process) · **PB SM134 #1**
(`--verify-examples` silently no-ops — found in the same session, same "the tool lies quietly" class).
