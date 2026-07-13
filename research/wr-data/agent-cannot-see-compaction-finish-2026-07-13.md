# WR specimen: the agent cannot see when compaction finishes (2026-07-13)

**Observation (BR, live).** BR triggered `/compact`, then walked away. The compaction finished while he was
gone. The agent did NOT stamp `post:` at that moment — it stayed dormant. Only when BR came back and typed a
message did the harness re-invoke the agent, which then ran its first tool call (`tt chrono now`) and wrote the
`post:` stamp. BR: "there will be a lag in the last chrono as I HAVE TO TRIGGER IT manually... it was first
until I said something that you got awakened by the harness... you cant see when the compacting process is
finished?"

**Correction (BR, same exchange).** This is NOT global invisibility. **The human CAN see compaction finish** —
BR has a compact **progress bar** in his terminal and would have seen it complete had he been looking; he was
away from the desk. So the boundary is **human-visible via an instrument the agent does not have.** The precise
statement is an ASYMMETRY: human-visible (progress bar) / agent-invisible — not "invisible to everyone."

## The mechanism
- The agent is **paused through the compact and stays paused after it completes.** There is no agent-visible
  "compaction finished" event (though the HUMAN sees one — the progress bar).
- The harness re-invokes the agent only when there is something to process. Empirically here that was **BR's
  message**, not the compaction-complete boundary.
- Therefore the hand-stamped `post:` is really **"first moment the agent ran again"** = gated on the human
  returning AND typing. The interval between compaction-finished and that first agent action is the human's
  entire away-time — **unbounded and invisible to the agent.**

## Consequence for the compact-duration measurement
The `elapsed = post - pre` hand-stamp is **not even a clean end-to-end ceiling** (as the chrono-stamps doc had
assumed). It is polluted by:
1. BR reading the "ready to compact" message and triggering `/compact` (pre-side latency), AND
2. **BR's away-interval after compaction finished, before he returned and typed** (post-side latency) — the new,
   larger, uncontrolled term this specimen surfaces.

Concretely: compact 2 hand-measured **11:06** at fill 42% vs compact 1's **6:39** at fill 35%. The naive reading
("higher fill → longer") is **void**: compact 2's 11:06 includes an unknown chunk of BR being away. The
fill-vs-duration hypothesis cannot be tested from hand-stamps at all.

## The fix (already sketched in the chrono-stamps doc — this specimen is the hard motivation)
`PreCompact` + `PostCompact` **command hooks**, each running `tt chrono now` (redirected to the stamps file).
A command hook is a shell command the harness runs at the lifecycle event — it does NOT require the agent to be
awake, and it fires on the true process boundaries. The delta between those two stamps is the **pure summariser
run** with zero human latency and zero dependence on the agent or the human being present. That is the only
instrument that can answer BR's "is compaction time proportional to fill?" question. Human-approved settings
step (hooks) — PROPOSE to BR, do not self-apply.

## Conclusion (BR)
**We need the super-harness to close this gap.** The human already has the instrument (the progress bar); the
super-harness should give the AGENT an equivalent — expose the compaction lifecycle boundaries to the agent
(via PreCompact/PostCompact hooks now, or a first-class agent-readable event later), so the asymmetry collapses
and the agent can both self-measure the compact and know its own lifecycle state without waiting on the human.
This is the same shape as the usage/context panel (SM022/SM039): a gauge the human can see, made agent-readable.

## Ties
Family E (the agent cannot read its own gauges / lifecycle) generalizes here to **the agent cannot observe the
harness's own lifecycle events** — compaction start/finish are opaque to it, just like fill (whereas the human
has a progress bar for the former and `/context` for the latter). The super-harness gap: expose (or hook) the
lifecycle boundaries to the agent, per BR's conclusion above. Related: [[agent-lacks-felt-time-rebind-at-boundaries]] (no felt
time — here, not even a felt *event*), the compact dance (foundations), and the `/context`-reaches-agent
specimen (a slash output DOES reach the agent; a compaction-complete event does NOT — asymmetric harness
visibility).
