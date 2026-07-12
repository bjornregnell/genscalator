# WR specimen: hit the session limit unobserved (2026-07-12)

**Event.** After a long, hard-pushing session (many commits, a live-edit-dance marathon on blog 021, then a
78-agent RQ0 workflow), we hit the **Max-5x SESSION limit** mid-workflow: "You've hit your session limit, resets
9pm". The RQ0 sweep's verification + synthesis phases died on it (62 of 78 agents failed). Weekly budget was fine
(37% all-models, 22% Fable) - it was the SESSION window we maxed, largely from the parallel CF5 agent burst.

**BR's point (verbatim-ish):** "we finally reached the limit after pushing HARD ... THE PROBLEM: we didn't observe
it coming. That's a requirement for the super-harness and maybe the status line?"

## The requirement it yields
**The super-harness (and the status line, SM039) should WARN of an APPROACHING usage limit, not just report it
after the fact.** A limit-approaching gauge: session-window %, weekly %, and a heads-up as we near the session cap,
so we can throttle, checkpoint, or defer heavy compute (a big agent fan-out) BEFORE it stalls mid-work. Same family
as the smart-zone / rot gauges: measure-and-warn before the wall, not report at the wall.

## Why it matters (asymmetry + safety)
- **Asymmetry:** neither party watched the burn. The human cannot see the meter mid-flow; the main-loop agent does
  not reliably see the session-limit approach either; and a fanned-out agent swarm has no shared budget view - so
  the congestion was invisible to both. A shared budget gauge is exactly the super-harness's job (the joint
  awareness the dashboard and statusline serve).
- **Cost of the miss:** a heavy workflow stalled mid-run (recoverable via resume, but wasteful). A pre-warning
  would have let us size the fan-out to the remaining budget.

Ties: SM022 (super-harness dashboard - add a usage/limit panel), SM039 ([[prefer-modly-over-raw-ollama-track-improvements]] neighbour; `tt statusline` - add a session + weekly limit gauge with an approaching-limit warning),
[[token-budget-modes]], the smart-zone / rot gauges. TODO: reqT-formalize as a super-harness Feature in SM022/PRD
after the reset.
