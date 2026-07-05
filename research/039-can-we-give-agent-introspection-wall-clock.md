# Can we give the agent introspection into wall-clock time?

- **Question:** the agent has **no sense of elapsed wall-clock time** — it cannot perceive how long a turn, a build
  span, or a "think" took. Can we give it a reliable, low-cost **elapsed-time signal** it can read and act on?
- **Why it matters:** wall-clock is the missing `dt` under **token velocity / acceleration** (dS/dt, d²S/dt² —
  `research/007-token-budget-awareness.md`), the trigger for **checkpoint/compact** on a long autonomous span, and a
  **rest-dance** input (fatigue is partly time-on-task). Without it the agent can run 11 minutes and not know
  (grounded datapoint: the `Crunched for 10m 54s` relay, `research/wr-data/harness-ux.md`).
- **Status:** **STUB** (agent-drafted 2026-07-05, from the wall-clock introspection-blindness datapoint). Needs BR
  steer on which mechanism to prototype first.
- **Prior art in-repo:** `tools/chrono.scala` (a human-relayed stopwatch — the agent can't time itself, so a human
  relays the round); the chrono premise IS this problem, half-solved by delegation.

## Minimal TODO

- [ ] **Frame it precisely.** Separate three clocks: (a) **turn wall-clock** (submit → response), (b) **span
      wall-clock** (a multi-step autonomous run), (c) **"think" time** (model reasoning only). Which does the agent
      actually need, and for what decision?
- [ ] **Survey what the harness already exposes.** The `Crunched for 10m 54s` figure is a **UI element**, not in the
      agent's context. Does any timestamp reach the model (message metadata, tool-result times)? If not, that gap is
      the crux.
- [ ] **List candidate mechanisms** and rank by cost/reliability:
      - a `tt usage`/`chrono`-style tool the agent calls to read **elapsed since a marked start** + **compile-cycle
        count** (extends `chrono.scala`; config-in-args, no ambient state);
      - a **hook** (Stop / PostToolUse / SessionStart) that stamps elapsed and **injects it into context** (the
        prosthetic-habit route — external structure supplies what the model can't hold);
      - **tool-result timestamps** surfaced back (each `tt`/scala-cli call already logs ms to `tmp/tt-perf.tsv`).
- [ ] **Decide the trust model.** Time must be **measured externally, not self-reported** (corroboration asymmetry —
      an agent guessing "that took a while" is confabulation). The signal has to come from a clock, via tool or hook.
- [ ] **Define the action.** What does the agent DO with elapsed? Candidates: propose a **consolidation-point
      compact** past a span threshold; feed **dt** into a velocity/acceleration readout; time-box a ralph loop;
      inform the **rest dance**. Pick one to drive the first prototype.
- [ ] **Keep the coupled-system caveat.** Span wall-clock ≈ *model reasoning + toolchain (scala-cli/JVM compile+test)
      + human relay*, NOT think-time alone (the crunch datapoint was mostly compile/test wall-clock). Report elapsed
      **and** its composition, or it misleads.

## Related
- `tools/chrono.scala`, `research/007-token-budget-awareness.md`, `research/wr-data/harness-ux.md` (the crunch
  datapoint), [[at-code-plan-and-introspection]] (token velocity/acceleration), the **corroboration asymmetry** and
  **prosthetic habit** glossary entries in `docs/foundations.md`.
