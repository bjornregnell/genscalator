# WR specimen: a human-run /context reaches the agent as text (2026-07-12)

**Observation.** BR ran `/context`; its output was surfaced into the agent's turn as a `<local-command-stdout>`
block. The agent SAW the full thing (92% fill, category breakdown, the autocompact warning). BR was surprised:
"you SAW my slash output?" - he had not assumed the output reached the agent.

## Two implications
1. **Partial patch to the agent's context blind-spot (RQ0 family E).** The agent cannot read its own fill, but a
   human-run `/context` DOES reach it as text - so the agent can KNOW its fill WHEN the human surfaces it. It still
   cannot INITIATE the read. The super-harness gap: make the read agent-initiable / automatic (the usage + context
   panel; SM022/SM039). This is "the human feeds the agent the gauge it cannot read" made concrete.
2. **Transparency: slash-command outputs land in the agent's context.** Not private to the human's terminal. Here
   benign (stats), but worth knowing the channel exists - a slash output the human thought was local IS shared with
   the agent.

## Origin (honesty beat)
The agent sloppily wrote "92% - noted", which sounded like a self-reading; BR probed; the mechanism was surfaced
honestly (the agent READ the number from the piped output, it did not MEASURE it). A small honesty clarification
that yielded a real UX / harness datum.

Ties: RQ0 family E (agent cannot read own gauges), [[hit-session-limit-unobserved-2026-07-12]] (the usage-warning
requirement), the super-harness usage panel (SM022/SM039), the post-hoc-introspection / honesty theme.
