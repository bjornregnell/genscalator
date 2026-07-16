# Post-compact is the highest-risk window for the mechanical bash-regression — shepherd it (2026-07-16)

**Type:** WR data — process finding + a JOINT mitigation protocol. Live specimen: BR flagged from *inside* a guard
stall (`ig:`) that the agent had just regressed to a `| tail` pipe **right after a compact**, and asked whether the
human needs to stay at the keyboard post-compact to catch guard stalls.
**Threads:** [[task-specific-degradation-mechanical-fails-before-conceptual-2026-07-16]], the blackout/hangover pair,
[[joint-rot-vigilance-recovery-kit]], [[cue-guard-stall]], the compact/exit-resume dances.

## What happened (observable)

Immediately after `/compact`, on about the third tool call, the agent ran `scala-cli test … 2>&1 | tail -40`. The
PreToolUse guard **stalled it for confirmation** (`[MED] pipe to head/tail/wc`). This is the **same `| tail`
regression the resume prompt explicitly warns about** ("`| tail` slipped TWICE this session" is a literal line in the
carried substrate) — yet it recurred anyway, post-compact. The guard caught it; BR flagged it from inside the stall.

## Why post-compact specifically (two grounded reasons)

1. **Carried ≠ armed (the hangover).** The anti-regression rule *is* in the resume prompt, but at **turn zero after a
   compact** the base-model habit (`| tail`) is more salient than the project rule. Crossing the compact boundary is a
   cold restart where the guardrails are present in substrate but **not hot in context** — the blackout/hangover shape
   applied to *reflexes*, not just facts.
2. **It's a MECHANICAL action → degrades first.** Bash plumbing (pipes, arg-order, whitespace) is exactly the
   repetitive/mechanical class that [[task-specific-degradation...]] predicts fails **before** conceptual work. The
   agent's conceptual output post-compact was intact; only the *plumbing* slipped. Same specimen as the session's
   space-drop slips, in the cold-start window.

So this is not random: **the post-compact window is a predictable spike** in mechanical-regression risk — cold reflex
+ mechanical action, stacked.

## The JOINT mitigation protocol (candidate)

- **Human side — do NOT go AFK immediately after a compact.** Stay at the keyboard to shepherd the **first handful of
  tool calls**, because that is exactly when the agent is most likely to regress and the guard stall needs a human
  watching to reject (not blanket-approve — see [[never-blanket-allow-destructive-commands]]). "AFK-right-after-compact"
  is the risky move; "shepherd, then AFK" is the safe one.
- **Agent side — re-arm before the first bash call.** Treat turn-zero-post-compact as a re-arm point: re-read the ⛔
  anti-regression list / `gs warm`, and prefer the `tt` flags (`--tail`/`--limit`/`--count`) over a pipe on the *first*
  command, not after the guard reminds you. Don't trust carried substrate to be hot.

## Why it matters

The compact/exit-resume dances currently optimise for *knowledge* continuity (the resume prompt carries state). This
finding says they must **also** account for *reflex* continuity: knowledge re-loads via the prompt, but **reflexes
re-arm slower**, and the gap is a measurable safety window. The mitigation is cheap (a few shepherded turns) and the
cost of missing it is a rubber-stamped regression — the exact human-rotted axis of the threat model. *(Candidate
memory if it recurs: post-compact = high-risk mechanical-regression window; human shepherds the first turns, agent
re-arms before the first bash call.)*
