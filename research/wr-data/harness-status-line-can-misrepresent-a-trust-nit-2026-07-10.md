# The harness status line can misrepresent what the agent is doing - a UX nit that touches TRUST (2026-07-10)

BR-flagged. The harness's live status line showed *"Building blog index page... Next: Render Arc-2 set to HTML
and hand off to BR for deploy"* while the agent was actually doing TE / cost / blog-stub work - a STALE inference
latched onto an old thread (the blog-publishing arc, hours earlier). CO4 noted "that's not what I'm actually
doing now"; BR: "very interesting. a UX nit (or perhaps even a pain - can I trust the harness???)".

## The point
The harness's inferred status / "Next" line is a LOSSY, generated layer that can go STALE or wrong in a long
session. At best a cosmetic nit; at worst it **erodes trust in the harness**: if the status line misrepresents
what the agent is doing, the human is left wondering what ELSE the chrome might be getting wrong. Trust-in-the-tool
is load-bearing for a human delegating + reviewing under confirmation-fatigue - a display that quietly lies spends
that trust.

## The framing (on-theme)
This is the confabulation / echt axis one tier UP - the **HARNESS confabulating a status**: a plausible,
confident surface ("here's what I'm doing next") that is not grounded in the actual current action. Same lesson,
different layer: **trust what is checkable, distrust the plausible-but-inferred surface.**

## The resolution (where to anchor trust)
Anchor trust on the CHECKABLE substrate, never the inferred chrome: the actual tool calls, the commits (`git
log`), the resume-prompt, the agent's own messages - all verifiable. The status / "Next" line is a convenience
hint, NOT a source of truth; treat it as such.

## Actionable (harness-ux, report-upstream candidate)
The "Next" inference should DECAY / clear when stale, or be tied to the ACTUAL current tool call rather than an
old thread. A stale status is a small but real trust leak - a harness-UX item in the same family as the
FleetView-warp pain trace and the guard-invisibility finding. Ties: `harness-ux.md`, [[fleetview-warp-panic-writes-2026-07-04]],
[[guard-stall-invisible-to-agent-2026-07-07]] (if present), confirmation-fatigue, the "trust the substrate, not
the surface" principle.
