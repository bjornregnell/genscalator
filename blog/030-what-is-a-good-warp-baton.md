# What is a good warp baton?

<!-- Slug/number 030. STUB, not drafted. Seed: BR 2026-07-19, same day the real reboot baton was published as
     work/BATON-EXAMPLE.md and pin SM168 opened the baton-quality investigation. This stub's job: hold the
     research questions until the joint-reasoning pass and (maybe) the subagent experiment produce answers. -->

> **Status: STUB 2026-07-19 (research questions drafted by the agent; nothing else written; BR has not revoiced).**
> **Audience:** TBD - likely people running long-lived agent collaborations who face the same handover problem;
> agentic-SE practitioners; readers of the exit-resume material in 005.

## The idea in one paragraph (agent scaffold, for BR to revoice)

A **warp** is our word for the jump from one agent instance to the next: the context window is cleared (a model
upgrade, a machine reboot, a fresh session) and everything the old instance held in its head is gone. A **baton**
is the small file the outgoing instance writes for the incoming one, named after the relay-race stick: the runner
changes, the race continues. The trick that makes a baton work is that it is deliberately a *pointer to durable
truth, not the truth itself*: the real state lives in committed files and memory, and the baton tells the fresh
agent where to look, what reflexes to re-install, and what NOT to start. We published a real one, verbatim, in
[BATON-EXAMPLE.md](../work/BATON-EXAMPLE.md): it carried an actual machine-reboot handover, and the fresh agent
reconstructed the working state from it with zero human re-explanation. That one worked. But what makes a baton
*good*? That is an empirical question, and this post is where we will answer it.

[figure: screenshot of the real cold start - the fresh agent's first minutes reconstructing from the baton
(mode cleared, pushes verified, the held queue left untouched). BR has the session feed; capture before it scrolls away.]

## Draft research questions

<!-- Agent-drafted 2026-07-19 per BR's ask; these seed the SM168 joint-reasoning pass. Trim, merge, or cut. -->

- **RQ1 (content):** What must a baton contain, and what must it leave out? Candidate ingredients from the one
  specimen we have: a re-install list for reflexes that regress at turn zero (forbidden-to-allowed pairs), holds
  stated together with who lifts them, checkable numbers (commit hashes, counts) instead of adjectives, and a map
  of where durable truth lives. Which of these actually carry the handover, and which are ritual?
- **RQ2 (measurement):** How do we measure baton quality objectively? We already have a candidate instrument: the
  fresh-restart fidelity probes (a quiz the fresh agent answers cold, scored against a key it cannot see). The
  behavioural version may be stronger: count the cold-start actions done right, and count the human
  re-explanations needed. Agent self-report is the one instrument we rule out in advance.
- **RQ3 (ordering):** Does order inside the baton matter? Our specimen puts the reflex re-install list first, on
  the theory that turn-zero regressions must be caught before the first tool call. Is that measurable, for
  instance by swapping section order in variants and watching early mistakes?
- **RQ4 (size):** Is there an optimal length? Too short and reflexes or holds are lost; too long and the baton
  competes with the substrate it is supposed to point at, invites unbounded reading, and rots like any duplicated
  prose. Where is the knee of that curve?
- **RQ5 (the experiment):** Can sub-agent experiments answer RQ1-RQ4 cheaply? Design sketch: give fresh
  sub-agents the same reconstruction task but different baton variants (full, shuffled, stripped of the checklist,
  stripped of numbers, none at all), score them blind with the fidelity probes. This reuses harness pieces we
  already have, and a no-baton control group tells us how much the baton is worth at all.
- **RQ6 (the honest limit):** What can no baton carry? A fresh instance holds every inherited fact at the same
  strength; the outgoing instance knew which facts it had verified itself and which it was merely told. Is that
  lost weighting real and does it show up in behaviour, or is it a romantic story the agent tells about itself?
  (Speculation until measured; the agent's own account of this is exactly the kind of introspection we treat as
  quoted data, not as evidence.)
- **RQ7 (generality):** How much of a good baton transfers to another human-agent pair or another project, and
  how much is pair-specific vocabulary? A baton full of local shorthand was written for *our* fresh agent; a
  transferable template would need to separate the pattern from the private language.

## TODOs

- **TODO:** run the SM168 joint-reasoning pass over RQ1-RQ7; trim to the few that earn the experiment.
- **TODO:** decide whether the sub-agent experiment (RQ5) runs, and on which harness.
- **TODO:** ground the "it worked" claim with the concrete cold-start episode (one home for the facts; this post
  points, the specimen file holds).
- **TODO:** real figure per the placeholder above; a table of baton ingredients vs failure-they-prevent could be
  a good near-figure once RQ1 settles.
- **TODO (BR):** revoice; decide if "warp baton" is the term to lock at first deploy (coined terms stay mutable
  until then).
