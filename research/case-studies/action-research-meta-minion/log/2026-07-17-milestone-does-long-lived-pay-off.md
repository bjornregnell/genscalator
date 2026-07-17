# Milestone assessment — is the LONG-LIVED meta-minion paying off? (2026-07-17)

**BR's question:** *"we are also seeing the long-running meta-minion pay off; no? if so log to our action-research
study."* **Answer: YES — but the honest version separates two things BR's phrasing fuses, and the separation is the
study's own result applied to itself.**

## ⚠️ First, disambiguate — two different minions, two different payoffs

There were **two** fable sub-agents in play on 2026-07-17, and only one is *this study's* subject:

| minion | what it is | what it delivered | which payoff |
|---|---|---|---|
| **the long-lived meta-minion** (push-1, push-2) | THIS study's subject; persists across pushes | the **PATTERN slot** (below) | **payoff of LONG-LIVEDNESS** |
| the PR #943 sandbox minion | a **fresh, unrelated** build-and-test agent | the end-to-end build + model-call counts | payoff of **DELEGATION**, not longevity |

**Fusing them would over-credit the study.** The build minion proves delegation works; it says nothing about
whether *keeping a minion alive across sessions* is worth it. **Only the first column answers BR's question.**

## ✅ The long-lived payoff is REAL, and it is the one thing a fresh minion CANNOT produce

**The `PATTERN` slot delivered** (push-2, verbatim in [`minion-log/push-2.md`](../minion-log/push-2.md)):

> *"**Scope over-extension of a TRUE property, now seen twice.** Push 1: `valid()` real but only guards `add`,
> asserted tool-wide. Push 2: quote-awareness real but MED-only, asserted flat. Same signature both times... both
> written as verified-from-source, both times the FALSE half was the operationally dangerous one. Countermeasure:
> when asserting 'tool X does P', name the code path on which P was verified."*

⭐ **THIS IS THE PAYOFF, and it is structural: a cross-push pattern is invisible to any single-push observer.** Push 1
alone is an anecdote. Push 2 alone is an anecdote. **"Twice, same signature" is a FINDING, and it requires a memory
that spans both** — which is exactly what long-livedness buys and a fresh minion cannot have. **A fresh minion
spawned for push 2 would have caught push 2's instance and had nothing to compare it to.**

## ⭐⭐ The payoff COMPOUNDED the same day — and this is the strong evidence

The pattern the long-lived minion named was then **independently REPLICATED by the fresh PR #943 minion**, which
committed *the exact shape* (measured whole-run model calls, asserted a narrower "0 calls" claim false). ⇒ the
long-lived observer did not just spot a local bug — **it extracted a GENERALISABLE failure mode that then predicted
a different agent's behaviour hours later.** n=2 across two different fable minions, n≥4 counting CO4's own
recurrences. **That is the difference between a bug report and a research finding, and only the long-lived seat
produced it.**

## ⚠️ Honest caveats — do NOT let the payoff inflate

- **The long-lived minion also has a COST that is now documented**: it **writes but does not commit** (cycle-2 §2.1),
  so push-2 sat uncommitted and mis-homed for ~1h. **The payoff is real AND the protocol has a gap.** Both true.
- **Its caution beat CO4's** (it refused to score the fable-resume success it could not verify) — a payoff of the
  *observer* stance, not strictly of longevity, but it is why the seat is trustworthy.
- ⛔ **NOT claimed: that long-lived beats fresh in general.** Claimed narrowly: **for cross-push PATTERN detection,
  long-lived is the only option, and it delivered one real, replicated pattern.** For single-task work (the build),
  fresh was correct and cheaper.
- **n=1 study, 2 pushes.** One pattern, however good, is one pattern. The study continues.

## ⇒ For the study

- **The PATTERN slot is validated as the study's highest-value artifact** — keep it in the push protocol, keep it
  mandatory, keep the "name the code path" countermeasure on the register (noting it **has not yet armed anyone** —
  read four times, reproduced four times; a mechanical countermeasure, so per today's theory it will only arm as a
  tool/lint, not as a briefing line).
- **Open protocol gap (BR's call):** the minion writes but does not commit. Either brief it to commit, or make
  receiving a push a two-step.

## Ties

[[three-swings-only-the-code-running-ones-were-accurate-source-reading-gives-confidence-not-truth-2026-07-17]]
(the fresh minion's payoff, kept separate) · **cycle-2 log** (the full process record; this file is the *milestone
assessment*, it POINTS) · `long-lived-meta-minion.md` (the protocol) · [[code-beats-prose-a-rule-fires-only-when-it-governs-the-object-of-attention-2026-07-17]]
(why the "name the code path" countermeasure will not arm from prose).
