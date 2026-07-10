# BR needs a PRIVATE scratchpad to avoid message-races (and not forget), kept opaque to the agent (2026-07-10)

BR-flagged WR datum. When posting a lot, BR needs his own **private scratchpad** to manage his input to the
agent. It resolves a three-way tension:
- **Avoid message-races.** Posting many rapid messages floods the feed and RACES the agent's in-progress
  responses (the [[harness-double-post-edit-race]] / racing-feed hazard). A private buffer lets BR **queue**
  things and post them when the agent is ready, decoupling his *capture rate* from his *post rate*.
- **Don't forget.** Ideas surface faster than they can be raised one-at-a-time; the scratchpad holds them so
  none are lost (he moves items below a "done ---" divider after pasting - his own bookkeeping).
- **But keep it opaque to the agent.** The scratchpad is BR's *chaotic private working notes*; he does NOT want
  the agent to see it or be confused by it. So it is human-side only: the agent sees only the **curated paste**,
  never the raw buffer.

**Design reading:** this is the human-side analog of the agent's substrate. Just as the agent externalizes
state into durable substrate to stay capable, the human externalizes his input-queue into a private scratchpad
to (a) not forget and (b) gate the flow so it does not race the agent. The pair-level rule: the human buffers +
curates on his side; the agent holds on cues ([[cue-hangon]], [[cue-brb]]) on its side; together they keep the
turn-taking clean under a high-throughput, high-excitement feed. Ties: [[br-personal-scratchpad]] (the actual
file), the racing-feed hazard, `harness-ux.md`, and the thriller-mode throughput this session.
