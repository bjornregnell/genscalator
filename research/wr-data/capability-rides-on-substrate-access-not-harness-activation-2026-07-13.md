# WR data: agent capability rides on SUBSTRATE ACCESS, not harness activation (a skill is just a file) (2026-07-13)

**Context.** After the br-blog-ass skill was moved into a brand-new `.claude/skills/` dir (which needs a Claude
Code restart for the HARNESS to auto-discover it), BR asked whether the agent needs the restart to *have* the
blog-voice guidance this session. The agent's answer — **no** — and its reasoning is the specimen.

## The distinction the agent kept (avoiding a plausible conflation)
It is easy, and WRONG, to conflate *"the skill is not harness-activated"* with *"I cannot use it."* Two layers:
- **Harness skill-activation** = discovery + auto-surfacing + auto-triggering on relevance. THIS needs the
  restart (a new dir must be picked up by the watcher). It is a **convenience** layer.
- **Content access** = the agent reading/holding the skill's text. A skill is fundamentally a **markdown file
  on disk (substrate)**. The agent reaches it via (a) already-in-context (read/authored this session) or (b) a
  direct `Read`. This needs NO harness activation.

So **capability rides on substrate access, not on the activation machinery.** The harness layer makes a skill
*auto-surface*; it is not the SOURCE of the capability. An agent that reasons "not activated ⇒ unusable" has
mistaken the convenience layer for the substrate.

## Why it matters (thesis reinforcement)
This is the genscalator **substrate-over-mechanism** thesis in miniature, and a sibling of "trust the checkable
substrate, not the chrome" (blog 004) and the agent-recovers-by-reading-its-own-substrate finding (the
FleetView panic-writes / `jsonl` recovery): the **file is the ground truth**; harness features are surfacing
conveniences on top. Whenever a harness mechanism is unavailable — a skill not yet watched, a stale display, a
warp — the agent's fallback is always **substrate + Read**, which is MORE fundamental than the mechanism.

## Reasoning-quality datapoint (with the family-E caveat)
The agent distinguished the two layers rather than conflating them — a clean model of its own operating
environment. Family E still applies (this is the agent reporting its own reasoning); the *behavioural* proof is
that it correctly told BR "no restart to USE it, only to auto-surface it," and can act on the blog-voice rules
this session with no restart.

Ties: the substrate thesis (foundations, blog 007/008), blog 004 (trust-substrate-not-chrome),
[[agent-cant-internalize-huge-codebases]] (distill via substrate + probes), [[cue-bare-auto-compact]]
(reconcile via substrate/git-log), the skill-discovery guide finding.
