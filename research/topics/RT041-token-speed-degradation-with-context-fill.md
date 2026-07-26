# Token-speed degradation with context fill — a latency signature of rot?

- **Question:** does the agent's **generation speed** (decode latency per token) **slow as context fills**, and can
  that **latency signal** serve as a **cheap, externally-measurable, EARLY proxy for high fill / approaching Z** —
  possibly *before* the quality (context-rot) signature shows up?
- **Why it matters:**
  1. **Context has a SPEED cost, not just a quality cost** — a *second*, independent reason to keep working-context
     small (beyond smart-zone quality). Approaching Z you may reason *worse* AND emit *slower* (compounding).
  2. **Latency is externally measurable** — unlike quality/rot, which needs behavioural adjudication (corroboration
     asymmetry), wall-clock-per-token is a number. If it tracks fill, it's a **cheap real-time instrument** the agent
     (or human) can watch — where the quality signal is expensive and laggy.
  3. Ties **token velocity** (dS/dt, `007`) to **context fill** and the **wall-clock blind spot** (`039`).
- **Status:** **STUB** (agent-drafted 2026-07-05, generalised from Q1 in the context-rot experiment).
- **Grounding (not speculation on the mechanism):** transformer **decode is ~O(context) per token** — each generated
  token attends over the whole KV-cache, so per-token wall-clock rises with context length. So *some* slowdown with
  fill is **expected by construction**; the open questions are its *shape*, its *size* on this harness, and whether
  it **leads** the quality signature. First datapoint: BR's subjective "much slower when the resume prompt emerged"
  + the harness spinner **45→51s** for one reply at **56% fill** (see
  `wr-data/context-rot-before-after-2026-07-05.md`, **Q1**).

## The confound to defeat first
The harness "thinking" clock (and any message-latency) is **total response latency = extended-thinking + decode**. A
slow reply at high fill could be *more thinking tokens*, not *slower decode*. **To isolate decode-speed** you need
**output-token-count ÷ decode-wall-clock**, separated from think-time. Don't claim "decode slowed" from raw response
latency alone.

## Minimal TODO
- [ ] **Define the metric precisely:** decode **tokens/sec** as a function of **context fill %** — with think-time
      excluded (or measured separately). Distinguish *prefill* (prompt ingest) from *decode* (generation).
- [ ] **Find the instrument:** where do per-message output-token counts + wall-clock live? (harness has them; the
      `/context` %, BR-relayed **TS:** anchors, and the spinner are partial, human-relayable proxies.) Candidate: a
      `chrono`/`tt usage`-style readout, or logging token-count + elapsed per turn.
- [ ] **Measure the curve:** tokens/sec vs fill across a session (or controlled runs at fixed prompt, growing
      context). Is it linear, or is there a **knee near Z**?
- [ ] **Test "latency precedes quality":** does decode-slowdown appear *before* measurable quality/rot degradation?
      If yes → latency is an **early-warning** instrument (cheaper + earlier than a behavioural rot check).
- [ ] **Actionability:** if it holds, wire a latency read into the **compact-trigger** decision (slow decode = a
      second, cheap signal to checkpoint), alongside fill/Z.

## Related
- `006-smart-zone-ceiling.md` (Z; is there a latency knee at Z?), `007-token-budget-awareness.md` (velocity dS/dt),
  `039-can-we-give-agent-introspection-wall-clock.md` (the agent can't self-time — this is the same blind-spot family,
  and latency needs *external* measurement), and the experiment log's **Q1**.
