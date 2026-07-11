# "Ask rule Bash(tt forge release-create *) overrides auto mode" — the effectful-verb ask is by design; authorization layers don't compose (2026-07-11)

During the v0.9.0 release go-live, `tt forge release-create` fired *"Ask rule `Bash(tt forge release-create *)`
overrides auto mode for this command."* — a confirmation prompt (BR's "aargh"). Why it happened, per BR's ask:

## Why it asks (this is by design, not a bug)
`tt forge release-create` is the ONE **effectful, outward-facing** forge verb — it POSTs a **public release** to
Codeberg. The safe-by-design allowlist deliberately **splits** the forge tool: the **READ** verbs (whoami / releases /
tags) are blanket-allowlistable (silent, no consequence), but the **EFFECTFUL** `release-create` is kept as an **ASK
rule, NOT a blanket allow**, so a human confirms a consequential public action. So the ask "overriding auto mode" is
the safety boundary **working as intended** — a deliberate friction on an outward, hard-to-undo action. (Same
principle as `tt git` omitting force/reset/rm, and `tt verify`'s env-only exec allowlist: the effectful surface is
gated, the read surface is free.)

## The real WR datapoint: authorization layers don't compose
BR **verbally pre-authorized the whole release** ("you have my go to do the actual release … go safe solo") and headed
AFK. But that **task-level** authorization does NOT reach the harness's **command-level** ask-rule — so the ask fired
anyway and BR had to approve it (the "aargh"). Two layers that don't compose:
- **human → agent, task-level:** "do the release" — granted, verbally, once.
- **harness → command, per-command:** the `ask` rule on `release-create` — still fires every time, ignorant of the
  verbal go.
A human who authorizes an agent to perform an outward action solo **still gets pinged** by the per-command ask; the
authorization did not propagate to the layer that actually gates the command. That mismatch is the friction.

## AFK-safety implication (a stall hazard)
This is exactly a **guard-stall** risk under AFK: had BR fully left, the release would have **stalled** on the ask (an
invisible wait — [[guard-stall-invisible-to-agent-2026-07-07]]). You cannot both "authorize the agent to release solo"
AND keep a per-command ask on the release, without one of: (a) the human stays for the one ask, (b) the ask-rule is
temporarily lifted (a human settings change), or (c) the **agent flags it upfront**.

## Agent lesson (echt)
I told BR "the tools make it safe (no stall)" *before* running it — but `release-create` is an **ASK**, not an allow,
so I **under-anticipated the ask-rule**. I should have warned "**this one will prompt you — stay for the single
confirm**" so BR knew not to walk away mid-release. Front-loading the known-ask is the fix, same shape as
[[web-surf-not-afk-safe-front-load-and-vet-egress]] (vet the egress/asks before an AFK run). Ties:
[[guard-against-forced-confirmations]], the safe-by-design effectful-gating, [[go-dance-autonomy-handoff]].
