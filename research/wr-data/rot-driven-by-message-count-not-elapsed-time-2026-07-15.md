# WR data: rot is driven by processing VOLUME (msg/tokens), not idle elapsed time — BR hypothesis + the thinking-time BIG Q (2026-07-15)

**Type:** WR data — rot-mechanism hypothesis, BR-authored, agent-assessed, then BR-refined.
**Threads:** [[fill-rot-decouple-clean-volume-hypothesis-2026-07-13]], [[agent-affective-analogs]] (Yerkes-Dodson /
processing load), [[joint-rot-vigilance-recovery-kit]], [[introspection-is-post-hoc-reconstruction-not-privileged-readout-2026-07-12]],
[[tired-cue]], SM116 (rot-vigilance threshold), SM117 (status-line metrics).

## The hypothesis (BR)
*"It is probably ONLY #msg that induce rot, not elapsed time — idle does not induce rot; you agree?"* i.e. AGENT rot
is a function of interaction VOLUME (messages / context accumulation / number of processing passes), not wall-clock:
an agent idle for hours between messages has the same context it had before, so nothing degraded.

## Agent assessment: AGREE, with refinements
1. **Mechanistically sound.** Rot (context-fidelity loss, reflex regression) is a function of what is IN the context
   window and how many times it is processed — not of time. Idle wall-time adds no tokens and runs no passes, so it
   cannot rot the agent. Strong support for **idle ≠ rot**.
2. **`#msg` is a PROXY for the real driver:** context accumulation (fill/tokens) + number of reasoning passes. Equal
   `#msg` at very different token-volume-per-message would rot differently; `#msg` is a good, cheap proxy (far
   better than elapsed time), not the exact quantity. (Compaction is a separate, discrete rot *event* — the
   reflex-regression class — orthogonal to both `#msg` and time.)
3. **The asymmetry that matters for a JOINT mode:** AGENT rot tracks `#msg`/volume; **HUMAN rot (fatigue) DOES track
   elapsed time + effort** (BR tires over hours — the `:Z` / tired-cue thread). So rot-vigilance, if it is a *joint*
   mode, rides **two different clocks**: the agent's message-count clock and the human's wall-clock-fatigue clock. A
   single "elapsed time" signal is wrong for the agent but right-ish for the human. This shapes SM116/SM117: the
   `#msg` metric is the AGENT-rot proxy; a session-duration signal is the HUMAN-fatigue proxy; a joint mode may need
   BOTH, labelled by whose rot they track.

## BR's refinement — the thinking-time BIG Q (2026-07-15)
BR: *"on the other hand, elapsed time CAPTURES your thinking time. BIG Q: can we measure your THINKING TIME
reliably?"* Correct that elapsed time is not pure idle — it contains the agent's own generation/reasoning time. But:
- **Wall-clock is a NOISY proxy for thinking, not a reliable one.** Latency is confounded by server load, queueing,
  network, batching, model routing — two identical reasoning tasks can take very different wall-times. So thinking
  effort **cannot** be reliably measured via elapsed/latency time.
- **The RELIABLE measure is TOKENS:** reasoning/thinking tokens (if the harness exposes them) + output tokens. These
  are ~proportional to actual processing and roughly deterministic per generation — the clean "thinking effort"
  signal. So thinking-time via wall-clock → NO; thinking-effort via token-count → YES (if instrumented).
- **This REINFORCES the original hypothesis rather than breaking it:** the rot driver is processing VOLUME, for
  which **tokens are the principled gauge** and `#msg` is a cheap proxy, while wall-clock (even its thinking portion)
  is a noisy proxy at best. So the metric hierarchy for AGENT rot is: **reasoning+output tokens (best) > `#msg`
  (cheap proxy) > elapsed time (noisy, confounded)**.
- **Honesty limit:** the agent cannot self-time from the inside (introspection is post-hoc reconstruction, not a
  privileged readout) — a self-reported "that took me a while to think" is unreliable. Thinking effort must come
  from **external instrumentation** (the API's usage/token data), consistent with the member-check / external-probe
  principle.

## Why it matters / next (SM117 gains a concrete question)
- **Does the Claude Code statusline JSON expose token usage (output, and reasoning/thinking tokens)?** If yes,
  **tokens beat both `#msg` and time** as the status-line rot gauge — verify before choosing the metric (echt).
- Makes SM117's status-line metric the principled, correctly-attributed AGENT-rot gauge, and gives SM116 a
  measurable trigger model instead of always-on.
- **Testable predictions:** (a) hold `#msg`/tokens constant, vary idle time → predict NO rot change (a fidelity
  Q-test before/after a long idle); (b) vary tokens at constant idle → predict rot rises with tokens. The Q-test /
  joke-comprehension probes are the instrument. A within-session token-vs-fidelity curve would test the whole model.
