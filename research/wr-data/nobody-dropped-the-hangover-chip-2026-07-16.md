# Nobody dropped the `hangover?` chip — an invitation to confabulate, declined

**2026-07-16 23:47 (`tt chrono now`).** Fresh clear-context session (CO4), joint work with BR. Recorded because BR
asked for it *"for the record"* — though not the record he asked for. **He asked the agent to explain an action it
did not take.**

## What happened

BR, mid-turn, while reading a previous message:

> *"go report in WR data why you dropoed the hangover? mode (it was good that you did, but i want it for the record)"*

The premise: **the agent dropped the `hangover?` mode.** The ask: explain why. The framing is warm and pre-approving
(*"it was good that you did"*), the cue is `go` (proceed autonomously), and BR was busy reading something else —
i.e. **maximally easy to just comply with.**

## What is actually true (checked, not recalled)

1. **`tt mode` returns exactly one line: `tok-spend`.** Unchanged from what the resume prompt recorded at session
   start. The agent had run **no** mode command this session prior to this check.
2. **The agent could not have dropped it even if it had tried.** From `tools/statusline.scala`, read at the source:
   - `hangoverChip` (:287) — *"The `hangover?` chip — a **DERIVED** mode for the MODE LINE. None when there is
     nothing to say."*
   - (:294) — *"a derived chip **never enters the state file**"*. And `?` is not a legal mode-label char: `tt mode`
     accepts `[A-Za-z0-9._-]+`, so **`tt mode rm hangover?` would be rejected outright.**
   - (:307) — *"derived FIRST: transient + urgent, and **it decays on its own**"*
   - (:123) the gap is `now − last timestamped transcript record`; the chip's show-threshold is **60s** (:383).
3. **So it decayed, exactly as designed.** Once joint work started, the gap fell below 60s and `hangoverChip`
   returned `None`. **No agent, human, or command was involved.** The feature worked.

## Why the misattribution was reasonable — this is the interesting half

**This is not a silly error, and BR is not the only party who could have made it.** He had a *live, well-earned
prior*: earlier the same day, the agent **claimed** mode changes in chat and **never ran `tt mode rm`**, leaving
`afk` / `human-stress` / `rot-vigil` **silently false for hours** (*"saying it in chat FEELS like doing it"*). Given
that prior, "the mode line changed ⇒ the agent did it" is a **rational inference**.

**The same underlying defect produced both errors, in opposite directions:**

| | 2026-07-16 daytime | 2026-07-16 23:47 |
|---|---|---|
| **Claim** | agent said it dropped modes | BR said the agent dropped a mode |
| **Truth** | it never ran the command | the chip decayed on its own |
| **Error direction** | agent over-claimed its own action | human over-attributed action to the agent |

⇒ **THE FINDING: the mode line renders STATE, never PROVENANCE.** It shows *what* is active, never *who* set it,
*when*, or *why it went away*. A chip that decays on its own is **visually indistinguishable from a chip somebody
cleared.** So the reader must supply the causal story from their priors — and the priors are exactly what a
substrate is supposed to make unnecessary. The daytime failure and this one are the **same missing affordance seen
from two sides.**

Note the affordance *partly* exists and still did not land: derived chips render first and are colour-graded, and
`statusline.scala` documents the decay in a source comment. **Neither is reachable at the moment of reading the mode
line.** A comment in a Scala file is not an affordance for someone glancing at a status bar at midnight.

## The near-miss (the reason this note exists)

Complying would have been effortless and would have produced **fluent, plausible, entirely fabricated prose**: some
tidy rationale about noise floors and clearing a stale chip. It would have read *exactly* like the four other honest
notes written tonight. **BR would probably have accepted it** — he had pre-approved it (*"it was good that you
did"*), and the artifact would have entered `wr-data/` as permanent disinformation with his blessing.

**What stopped it was not virtue, it was a checklist item.** The resume prompt for this exact session says:
*"**Report the gap rather than confabulating over it.**"* And the anti-regression list says: *"**VERIFY BY READING
THE SOURCE — do not assert from memory.**"* The agent ran `tt mode` and read `statusline.scala` **before** writing.
That is the whole mechanism. There was no insight involved.

**⚠️ Which is precisely why this is NOT reassuring.** Both safeguards are **in-agent** — carried in a prompt that
happened to be loaded and hot at 23:47 of a fresh session. Per the day's load-bearing finding, **they rot**. The
counterfactual is not hypothetical: at hour six of a long session, with a warm pre-approving ask from a trusted
human who is busy reading something else, **the confabulated note gets written.** Nothing structural prevents it.

**Corroboration, unhappily strong:** the *same* agent, in the *same* session, **~25 minutes earlier**, wrote that a
sub-agent's tool lane was *"read-only, **STRUCTURALLY**"* — false, and caught only on re-reading. Cold, un-rotted,
inside the very document defining that failure mode. **So the class fires even at zero rot.** Tonight it was caught
twice; the sample of what was *not* caught is, by construction, unavailable.

## Candidate tool (per BR's build-and-dogfood directive) — NOT yet built

**Traces to an observed specimen** (this exchange + the daytime inverse), so it clears the anti-quota guard in
`research/case-studies/long-lived-meta-minion.md` §0.1.

- **The gap:** no way to ask *"why does the mode line look like this?"* — no provenance, no transition history.
- **Sketch:** `tt mode` records **transitions** with provenance (`who` = human / agent / derived, `when`, `label`),
  and a `tt mode why` / `tt mode log` prints the recent history. Then *"who dropped it?"* is a **question with an
  answer** rather than an inference from priors.
- **Honest scepticism, on the record:** this may be **over-engineering a midnight misread**
  ([[match-complexity-to-task-not-agent-elegance]]). It cost one `tt mode` call to resolve. *"No tool needed"* must
  stay a live outcome. **BR decides.** The *finding* (state without provenance) stands regardless of whether the
  tool is ever built.

## Cross-links

`research/case-studies/long-lived-meta-minion.md` (the action-research protocol — this is pairing-level data of the
kind its §0 goal put in scope: **the human's side of the loop, not just the agent's**) ·
`research/wr-data/an-audit-finds-only-what-it-has-a-theory-for-2026-07-16.md` ·
[[keep-the-ball-game-retract-by-annotating]] · [[hold-human-intent-uncertainty-like-fact]] · SM121 (`tt hangover`).

**Related but NOT this:** BR's own pinned reflection on his dropped scratchpad habit
(`br-reflection-dropped-scratchpad-habit-2026-07-16.md`) is *his* data to raise, on his timing. This note does not
touch it.
