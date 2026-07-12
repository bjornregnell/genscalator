# WR specimen: the statusline clock freeze is a free turn-taking (ballgame) indicator (2026-07-12)

**Observation.** After adding a leading `HH:MM:SS` wall clock to `tt statusline`, BR noticed it **freezes while the
agent is processing a turn** and **jumps + resumes ticking the instant the turn completes**. Cause: Claude Code
re-invokes the statusline command on render events, and it does **not** re-render mid-generation — so the clock's
liveness tracks CC's render cadence, which pauses during the agent's turn.

**Why BR loved it (his words):** *"the clock stops when you think! that is actually good!"* and *"so I KNOW that I
have control back in our ball game when clock jumps and ticks - THAT IS REALLY GOOD."*

## Why it matters
- A **free, emergent turn-taking / control indicator** that maps directly onto the **ballgame** (whose turn it is,
  foundations glossary): **frozen clock = the AGENT has the ball** (busy); **ticking clock = control is back with the
  HUMAN**. No design work - it fell out of the clock's refresh cadence.
- Closes a real gap: the human otherwise cannot always tell at a glance whether the agent is **still working** or
  **done and waiting**. The frozen/live clock encodes agent-busy vs idle ambiently.
- **Design lesson:** a cheap ambient signal (a ticking clock) can carry turn-state for free; an *unintended* affordance
  that turned out genuinely useful. Candidate to note deliberately (a "busy/idle" or "agent has the ball" indicator is
  exactly what SM022's live dashboard would otherwise build on purpose - here it is for free).
- Also **answers the earlier "does the clock tick?" question** empirically: it ticks when CC re-renders the statusline
  (idle / on activity) and freezes during the agent's turn - so it is NOT a smooth per-second clock, and that is the
  feature, not a bug.

Ties: **Ballgame** (foundations), the statusline (SM039, genscalator `4b82452`), SM022 (live psyche dashboard - a
free agent-busy signal), [[joint-rot-vigilance-recovery-kit]] (shared turn-awareness), the human/agent asymmetry study.
