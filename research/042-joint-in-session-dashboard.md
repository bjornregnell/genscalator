# Joint in-session dashboard — live metrics as ASCII graphics for shared human+agent awareness

- **Idea (BR pin 2026-07-05):** a **human-agent JOINT dashboard** that renders **in-session metrics as ASCII graphics**
  (reuse `tt ascii`) so **both** parties share real-time awareness of the session's state. Not agent-only, not
  human-only: a single surface they read together.
- **Status:** **STUB** (agent-drafted, not deep — placeholder so we don't forget). Full title was too long for a
  filename; shortened to this slug.
- **Why joint (the motivating finding):** the study's **O13** (corroboration asymmetry made operational) + the
  wall-clock/usage blind-spot family — the agent is **blind to its own** token spend, context fill, and elapsed time, so
  today the **human relays numbers by hand** (`/context` %, `TS:` anchors, "42% weekly, resets Tue"). A shared dashboard
  **closes that loop for both**: the agent gets the introspection it structurally lacks, the human stops hand-relaying.

## Candidate metrics (what goes on it)
- **Token usage** — per **session** / per **week** (+ reset countdown) / per **model** (substrate-candidate 6, `007`).
- **Context fill %** + proximity to **Z** (`006`); **compact-trigger** proximity (fill ≥ 0.8·Z).
- **Token velocity dS/dt** + acceleration (`007`); **decode speed** / latency signature (`041`).
- **Wall-clock** elapsed / `dt` between checkpoints (`039`, the missing-`dt` problem; `TS:` anchors as input).

## Minimal TODO (don't build yet)
- [ ] Fix the metric set (above) + decide human-relayable vs agent-accessible per metric (some counts the harness holds).
- [ ] Find the instrument — where do token counts / fill / per-turn output live? (shared with `041`'s "find the
      instrument" TODO and the `tt usage` candidate.)
- [ ] Render via `tt ascii` (sparklines / bars / a compact status block); decide on-demand vs statusline cadence.
- [ ] Tie into the **compact dance** + **budget pacing** decisions (a live readout replaces BR hand-relaying).

## Related
- substrate-candidate 6 (token-usage introspection), `006-smart-zone-ceiling`, `007-token-budget-awareness`,
  `039-can-we-give-agent-introspection-wall-clock`, `041-token-speed-degradation-with-context-fill`,
  `harness-ux.md` (can't read `/context` while queued — a statusline would fix that too), `tt ascii` (the renderer).
