# WR data: the agent has NO phenomenology of absence — "I can't feel a missing skill" (2026-07-13)

BR named this asymmetry directly, mid-incident: *"WR data asymmetry: 'I can't feel a missing skill'."* It generalizes
well beyond the triggering case (guardrail skills silently inactive — see
[[guardrail-skills-silently-inactive-all-session-2026-07-13]]) and deserves its own specimen.

## The asymmetry
From the inside, an **inactive skill is indistinguishable from an active skill the agent simply didn't apply.** There
is no felt gap, no error, no "something is off" signal. Absence does not announce itself. The agent has rich signals
about what IS present (it can list loaded tools, quote recalled memories, read its context) but **no signal about what
is missing** — because a missing thing produces no token, no perception, nothing to introspect on.

Contrast with human introspection, which often DOES register negative space: "I know there's a word for this," "some-
thing's missing from this argument," the tip-of-the-tongue feeling. The human gets a felt pointer at a gap. The agent
gets silence — and silence reads identical to sufficiency.

## It generalizes across every kind of absence
- **Missing skill** — the trigger: `avoid-guard-stall` was OFF all session; the agent kept regressing and felt fine.
- **Missing memory** — a recall failure (the total-recall-failure risk in memory compaction) leaves no stub; the
  agent doesn't feel the forgotten fact, it just doesn't have it.
- **Unloaded / deferred tool** — an absent capability isn't felt as a limit; the agent just doesn't reach for it.
- **Truncated or missing context** — a dropped passage post-compaction leaves no "hole"; the summary reads whole.
- **A file it never read** — unread code is not felt as ignorance; confident wrong answers follow.

In every case the failure mode is the same: **the agent cannot distinguish "not there" from "there and fine."**

## Consequence for self-audit
- **Presence is checkable; absence is not — without an external expectation.** To detect absence you must know what
  SHOULD be present (a spec), then assert against it. The agent has no innate such spec.
- So the load-bearing signals for absence are exactly two, both external to the agent's felt state:
  1. **The human's out-of-band read** — noticing the behavioral signature of a gap (here: repeated guard-stalls +
     drift) and asking the diagnostic question. (Ties family-E: the agent also can't self-certify rot.)
  2. **An explicit POSITIVE-ASSERTION check** against a declared expected set — "assert skills X, Y are active,"
     "assert the memory index is complete," "confirm this file was read" — run at startup/boundaries, because the
     agent won't feel the lack.

## Who can detect it? (nobody *feels* it — and only one party can even *catch* it)
BR's follow-up: *"who else can feel it???"* The answer sharpens the finding: **no party feels the absence** — not
even the human. Three parties, three relationships to it:
- **Agent** — blind: no felt gap, and no ground truth about what's loaded.
- **Harness/system** — holds the **ground truth** (it knows exactly which skills are active; `/skills` proves it) but
  does **not feel** it and does **not proactively announce** it. Knowledge without alert — inert until queried.
- **Human** — also cannot *feel* the agent's missing skill; he **infers** it from the behavioral symptom
  (guard-stalls + drift) and then **confirms** with a check. Inference + query, not feeling — and typically late,
  after some damage.

The crux: **detecting absence requires BOTH an expectation of what should be present AND access to the actual state.**
- Agent has neither a reliable expectation nor felt access to actual.
- Harness has the actual state but no expectation (it doesn't know what *this* task needs).
- **Human uniquely holds both** — he knows the work needs `scala-style` etc. (expectation) and can run `/skills`
  (actual) — so, today, **only the human can close the expectation-vs-actual gap.** That is precisely why the
  human-in-the-loop catch worked, and why it was load-bearing rather than incidental.

To AUTOMATE absence-detection you must give some component both halves: a **declared expected-skill/tool/memory
manifest** (the expectation) + a **query of live state** (`/skills`, the loaded-tool list, the memory index) — then
assert one against the other at startup. That is the concrete shape of the SM070 self-check.

## Design implication
Build **positive-assertion self-checks at session/boundary start**, not trust-in-felt-completeness. The resume-prompt
should VERIFY expected skills/memories/tools are active rather than assume them. This is the actionable core of
[[SM069]] (skill-set audit) and a sibling of the [[hardening-dance]] (agent audits its own config). The deep point:
an agent's introspection is **presence-biased** — it can survey what it has, never what it lacks — so robustness must
come from external expectations checked explicitly, never from the absence of a felt gap.
