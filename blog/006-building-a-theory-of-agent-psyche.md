# 006 — Building a theory of agent psyche

**Status: STUB.**

> **The 004→008 arc** (map in 004). **You are here: the Method.** *Backwards cliffhanger:* the method only makes
> sense on top of a claim about what "learning" even *is* for a frozen-weight agent. → Next (007): the theory —
> bedrock.

- **TODO: mine WR data on how the agent thinks — its reflexes and habits.** From `research/wr-data/` and
  `research/RAW-DATA.md`, harvest the recurring *behavioural* patterns (not one-off bugs). Seeds already observed:
  - **Reflexes** — reaching for bash (`ls`/`cat`/`grep`/`printf`/`echo`-glued compounds, `$(…)` substitution) instead
    of a typed tool, even when the tool exists; TAB/pre-prompt completion laziness bias.
  - **Habits (learned or drifting)** — over-response / over-delivery bias; think-time perception gap; instruction-adherence
    decay over a long context; commit-message metachar tripwires. (See `instruction-adherence-decay.md`,
    `agent-affective-analogs.md`, `token-budget-awareness.md`.)
  - Frame each as: trigger → reflex → cost → durable cure (usually a typed affordance or a "dance").

- **TODO: fold in our definitions from foundations, on the fly, and explain them.** As the essay uses a term, inline
  its genscalator definition (link to the foundations/glossary) so a reader meets each concept where it bites —
  candidates: over-response bias, framing-as-arousal, fill vs rot, the smart zone / joint zone, WR data, the dances.
  (Anchor-point-for-skimmers style: disambiguate each term at first landing.) TODO: cross-link the actual foundations
  doc once its home is fixed.

- **TODO: explain our WR-data method for probing agent psyche.** Document the method itself (see `research/METHODOLOGY.md`):
  the human logs friction/behaviour events *live* during real runs ("WR data: …"), verbatim excerpt + a labelled
  reflection, appended (never retro-edited — `research/RAW-DATA.md` is append-only, a changed mind is *new* data).
  Why it works as a psyche probe: it captures the agent's behaviour *in situ* under real task pressure, with a
  human-in-the-loop observer, rather than in an artificial eval — naturalistic observation for an artificial mind.
  Note the testable bridge: `agent-affective-analogs.md` proposes over-response ≈ human stress and framing-as-arousal
  (Yerkes–Dodson), operationalisable in the indent-vs-braces harness (vary wrapper intensity, hold task constant).

Related: `research/human-state-and-joint-zone.md`, `research/inference-time-learning.md`; blogs `004` (UX), `005`
(dances). Method honesty: preregister, report the null (cf. the framing-as-arousal prereg).
