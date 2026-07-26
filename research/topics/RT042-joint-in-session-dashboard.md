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

## Architecture — how to FEED the dashboard (BR hypothesis 2026-07-05 + agent WDYT)
**BR's sketch:** a local **"dumb" ollama agent** actively monitors (a) a **tee-ed terminal** ("double-watched
terminal") and (b) the **Anthropic web-GUI Settings→Usage modal** (screenshots etc.); smart CO4 feeds data/screenshots
to the dumb agent, which extracts metrics for `tt ascii --dashboard`. **BR hypothesis:** hard to write by hand / with
Claude (may need **image recognition**), so a dumb agent may be the pragmatic bridge — BUT a **deterministic** monitor,
if achievable, is strictly better than a dumb agent.

**Agent WDYT — agree on the principle, challenge the premise that vision is needed. Decompose by SOURCE:**
- **Tee-ed terminal = pure TEXT → deterministic NOW, no agent, no vision.** `script`/`tee` the terminal to a file; a
  sidecar tails + parses it. This half is trivially deterministic; a dumb agent here only adds flakiness.
- **Anthropic Usage modal = the hard part, but vision is the LAST resort, not the first.** Cheapest→hardest:
  1. **JSON endpoint** — the modal renders from data; inspect DevTools → Network for the XHR/JSON it fetches; if it
     exists, hit THAT with the session cookie (fully deterministic, trivial, no vision). *Most likely the real answer.*
  2. **DOM scrape** via headless browser (Playwright) with the session cookie — deterministic text from the rendered
     DOM, still no vision.
  3. **Screenshot + OCR / vision (the dumb-agent path)** — only if 1 and 2 are blocked.
- **So "may need image recognition" is probably FALSE:** terminal is text; the modal is almost certainly JSON-backed.
  Vision is a fallback for genuinely vision-only sources, not the default.

**Why determinism matters MORE here (sharpening BR's principle):** the payload is **numbers**. A dumb agent that
misreads "42%" as "24%" corrupts the dashboard **silently** — hallucinated metrics are worse than no metrics. If a
vision fallback is ever used, **guard it** (range checks, monotonic-% sanity, cross-source agreement).

**One structural flip:** don't put **CO4 in the monitoring hot loop** ("CO4 feeds screenshots to the dumb agent") — that
**spends the very budget we're measuring**, and CO4 can't self-measure anyway. Run the monitor as an **independent
sidecar** (deterministic tool, or a local ollama agent as fallback) OUTSIDE CO4: it watches the terminal tee + polls the
usage source and writes a **metrics file**; `tt ascii --dashboard` renders that file; CO4 just **reads the cheap
rendered dashboard**. Clean split: **producer (sidecar monitor) vs renderer (`tt ascii --dashboard`)**.

**Security:** a monitor holding the Anthropic session cookie / scraping the console = **credential handling** — keep it
local, never commit/leak the cookie, treat as sensitive.

**Escalation ladder (the plan):** deterministic sources first (terminal tee → parse; usage JSON → parse); reserve the
dumb-agent/vision path only for sources with no deterministic access. Ollama (local, already available per
`muntabot-bilingual-ollama`) is the fallback engine iff vision is ever truly required.

**★ BR RATIFIED (2026-07-05, after consideration):** BR agrees with **all of points 1–5 + Net**. So this is the
**decided design direction**, not just a proposal: **deterministic-first** (terminal `tee` → parse; usage **JSON
endpoint** → **DOM scrape** → vision only as a guarded last resort), an **independent sidecar producer OUTSIDE CO4**,
`tt ascii --dashboard` as the pure **renderer**, and the session cookie treated as **credential-handling** (local, never
committed). The dumb ollama agent is retained only as a fallback engine we expect not to need. **Next concrete step
(cheap, human-present):** BR opens the Usage modal with **DevTools → Network** and reports the endpoint/JSON it fetches
— that single check decides whether any non-deterministic path is needed at all.

## Related
- substrate-candidate 6 (token-usage introspection), `006-smart-zone-ceiling`, `007-token-budget-awareness`,
  `039-can-we-give-agent-introspection-wall-clock`, `041-token-speed-degradation-with-context-fill`,
  `harness-ux.md` (can't read `/context` while queued — a statusline would fix that too), `tt ascii` (the renderer).
