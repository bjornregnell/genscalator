# WR data — command-hygiene regression (live study specimen), 2026-07-06

**Event.** The agent ran `cd /home/.../genscalator 2>/dev/null; grep -n ... research/047-PLAN.md` — a **compound** command (`cd` + `;`), violating the standing command-hygiene discipline (one bare allowlist-matchable command; no `cd`/`&&`/pipe/redirect; memories `prefer-inrepo-tmp-over-slash-tmp`, `guard-against-forced-confirmations`). Side effect: the harness reported "Shell cwd was reset". Correct form: a bare `grep -n <pattern> <absolute-path>` (no `cd`). Command-hygiene is **dim 11 in the fixed instrument** (`047-instrument.md`).

**BR caught it and asked (WR data): regression due to warp / compact / context-rot?**

**Echt diagnosis (agent; corroboration-asymmetry caveat — cannot fully self-diagnose):**
- **Context-rot by fill:** weak — fill ~18% at last `/context`, maybe ~40% now, far from the ceiling where fill-driven rot bites.
- **Warp:** none recent (old-old-me, resumed hours ago).
- **Compact:** last compact many turns ago; hygiene held since → unlikely the proximate cause.
- **Most likely — discipline slippage under rapid multi-thread load:** a burst of parallel work (move the raw log, scrub, two-repo commits, `claude --version` check, PB edit, plan edits, answering fast messages) narrowed attention; the agent defaulted to the *habitual* `cd ; grep` instead of the disciplined bare form. This is exactly the pinned **structural-over-knowledge dogma (dim 15): discipline regresses under load; the durable fix is structural, not willpower.**

**Double relevance:**
1. **Live 047 specimen:** an *enactment* trait (command-hygiene) failing under load in real time — the exact degradation the study measures, caught in vivo (enactment failing, not recall).
2. **Overnight-run risk:** a `cd ;` compound can race/trip the harness guard → dangerous for the AFK guard-free run. Reinforces that the feasibility guard-audit + (ideally) a structural allowlist guard against compound shell commands (human-approved; agent not authorized to change the allowlist solo) are load-bearing.

**Structural fix (per the dogma):** the right layer is a tooling/allowlist guard that makes the compound command impossible, not a willpower resolution to "be more careful." Flagged for BR / a hardening pass.
