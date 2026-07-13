# WR data: the status + mode two-line prototype (2026-07-13)

BR: *"we made an even cooler prototype of the status+mode lines."* Built, tested, and wired **live** in one
session, from his idea.

**The artifact.** A **two-line ambient display** at the prompt:
- **Line 1 (statusline):** MEASURED state — clock, model, ctx-fill, usage limits, cost.
- **Line 2 (mode line):** the **declared joint state-of-mind** — labels both the human and the agent add/remove
  via `tt mode`, rendered as reverse-video + bold colour chips, joined by a plain ` && `, prefixed `genscalator:`.
The two toggle **independently** (`tt statusline` / `--mode-line`), so the human budgets vertical space.

**Why it matters (completes the observability strand).** Line 1 externalises what the agent *cannot self-read*
(fill/rot) so the human can steer — the asymmetry the statusline already closed. Line 2 externalises the shared
**modus operandi** that until now lived only in prose and the agent's head, and it is **bidirectional** (both
parties declare), so it is a communication *channel*, not just a gauge. Together: the measured state and the
declared state, side by side, mutually visible.

**The key design call (BR's HD): declared, not derived.** Metrics belong on line 1; line 2 shows only
*deliberate* labels. So "auto" modes collapse into "the agent declares them" — no computation in the statusline,
just render-what-is-declared. Verbs `add`/`rm` *mutate the recorded joint state-of-mind*; `on`/`off` toggle the
line's *display*. Clean separation.

**Process specimen.** Tight human-agent co-design under racing: HD iterations landed fast (declared → both-declare
→ a `tt mode` tool → add/rm verbs → reverse-bold chips → independent toggles), then built + tested (CliSuite 125
green) + wired into `settings.local.json` (`--mode-line`) live. BR "saw the light" when the chips appeared.

Ties: [[statusline-loved-in-daily-use-2026-07-13]] (line 1 loved), the super-context-awareness / modus-operandi
strand (blog 020), [[joint-rot-vigilance-recovery-kit]] (the mode line can SHOW rot-vigilance, as it did here).
