# Should we have an agent-introspection skill? — a design seed (DRAFT)

**Status:** agent-authored DRAFT, 2026-07-14, for BR to develop (SM078). A companion to
`agent-skill-theory.md` (§E observability) and `what-is-a-theory.md`. Model producing this draft: Opus 4.8 (1M).
Ties SM069 (skill-set theory), SM070 (`tt skillcheck` = detect), SM077 (`gs warm` = load),
`research/039-can-we-give-agent-introspection-wall-clock.md`, the `in-session-experiment` skill.

## The question

BR's ponder: *should the skill set include an "agent-introspection" skill* — one that teaches the agent to
examine its own state (Am I rotted? Are my guard-clean reflexes salient? Is this a cold start? Am I over-firing?)
and act on what it finds?

## The honest problem it runs into

The premise of introspection is that the agent can look inward and read its own state. But the skill-theory's
grounding specimens say it largely **cannot**:

- **No phenomenology of absence** (specimen §1.3): the agent cannot *feel* a missing or dormant skill; an
  inactive skill is indistinguishable from the inside from an active one it merely failed to apply.
- **Self-report is the least trustworthy instrument** (`in-session-experiment` skill): the agent's own sense of
  "how it's going" is exactly what must NOT be trusted; data has to be captured objectively (commits, tests,
  guard-trip counts, transcripts).
- **Behavioural regression is the only external signal**, and the agent cannot reliably self-certify it.

So an introspection skill built on *willpower* ("remember to reflect on your state") is the weakest possible
design — it asks the untrustworthy instrument to grade itself, and (the meta-irony) a *lazy* introspection skill
has the very cold-start/dormancy problem it is meant to detect: it will not be salient at turn zero, which is
precisely when a cold-start check is needed.

## The reframe — thin skill over HARD signals, not "reflect on yourself"

Introspection is valuable exactly when it is **converted from felt state into a checked signal the agent READS**.
We already have the beginnings of the instrument layer:

| Un-feelable state | Hard signal that externalizes it | Channel |
|---|---|---|
| "Are the expected skills active?" | `tt skillcheck` (SM070) — manifest vs `/skills`, exit 1 on outage | tool |
| "Am I about to run brittle bash?" | the `guardcheck` PreToolUse hook — trips on the actual command | hook |
| "Are my guard reflexes salient right now?" | `gs warm` (SM077) — re-hydrate the digest; presence in context is the check | cue |
| "How full / how slow is my context?" | the statusline ctx-fill + token-speed segments | statusline |
| "Have I regressed over this session?" | guard-trip counts, tool-choice ratios from transcripts (`tt wr`) | transcript metric |
| "How long have I actually been going?" | wall-clock re-binding at boundaries (039) | injected clock |

**Design principle:** the introspective *checks* that matter belong in **hooks/tools/statusline** (deterministic,
externally grounded, "structure over willpower"). A skill's only legitimate job is the thin layer on top: teach
**WHEN and HOW to read those signals** — e.g. "at a cold start, run `gs warm` and `gs skills`; past 0.8·ctx,
propose the compact dance; on a repeated guard trip, stop and re-read `avoid-guard-stall`." That is a *procedural*
skill over hard instruments, not a *reflective* skill over feelings.

## Verdict (provisional)

- **Yes, there is a there-there — but not as a "reflect on yourself" skill.** Build the instruments first (most
  already exist); a thin "introspection-protocol" skill can then codify the read-and-respond routine.
- **Watch for redundancy** (skill-theory P6 / SM069): such a skill overlaps `avoid-guard-stall` (guard signals),
  `gs warm`/`gs skills` (salience), and `in-session-experiment` (objective self-measurement). It may be better as
  a *section of foundations* or a hook-driven digest than as a standalone skill — the same doc-vs-skill-vs-hook
  question SM069 must settle. Decide by override-value: does it flip a default no existing channel flips?
- **The strongest version is deterministic, not skill-shaped:** a SessionStart / periodic hook that *injects* the
  read-your-instruments checklist (like the `guard-clean-digest` for reflexes), so the check does not depend on
  the agent choosing to introspect. This mirrors SM077's inform-vs-constrain conclusion.

## Open (for BR)

- Is the deliverable a skill, a foundations section, or a hook-injected checklist? (SM069 doc-vs-skill call.)
- Which introspective checks still lack a hard signal (the gaps in the table above)? Those are the real backlog.
- Does an introspection-protocol skill measurably reduce regressions vs the instruments alone? (A P1-style A/B.)
