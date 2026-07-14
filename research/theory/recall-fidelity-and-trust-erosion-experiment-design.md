# Experiment design: within-session recall fidelity vs context fill, and trust-erosion (SM106)

**Status: DRAFT (design only — no run).** Agent-drafted safe-solo (SM106), from SM102 (recall) + SM096
(trust). Two probes for the skill-theory + in-session-experiment thread. The governing rule throughout (the
in-session-experiment lesson): **self-report is the least trustworthy instrument** — score against behavioural
ground truth (the commit/pin trail, timing, approval logs), never the agent's or the human's confidence.
Threads: SM102, SM096, [[agent-cant-internalize-huge-codebases]], blog 001/011 (context rot), the
in-session-experiment skill.

---

## Probe A — does within-session recall fidelity degrade with context fill? (from SM102)

**Research question.** As a single session's context fills, does the agent's ability to *accurately
reconstruct* an earlier multi-step episode degrade — and by how much?

**Why it matters.** SM102 recorded one positive observation (deep in a long session, at high fill, the agent
reconstructed the 4-step `sound.sh` escalation faithfully). One observation is not a curve. If recall holds,
that supports leaning on in-context history; if it degrades, it quantifies *when* the durable commit/pin trail
must take over from agent memory.

**Operationalization.**
- *Independent variable:* context fill at probe time (e.g. bands ~20 / 40 / 60 / 80 / 95%). Read from the
  harness gauge; the agent cannot self-measure fill (that is why the gauge exists).
- *Dependent variable — fidelity score:* fraction of an episode's discrete facts (steps, their order, and a
  key detail each) that the agent reconstructs correctly, checked against **ground truth** = the commits + PB
  pins laid down *at the time* of the episode. Binary per fact, averaged.
- *Explicitly NOT a measure:* the agent's confidence in its own recall. Per SM102/SM096 self-report of recall
  accuracy is uninformative; it is scored only against the external trail.

**Design.**
1. Pick episodes that (a) happened earlier in the session and (b) left a durable trail (commits/pins) usable
   as an answer key. Tonight's own session is full of them (the 5-way→7-way bench, the SM109 fix, the
   sound.sh escalation).
2. At each fill band, prompt a cold reconstruction of a *different* episode of comparable complexity.
3. A scorer blind to the agent's confidence grades each reconstructed fact against the trail.

**Confounds / threats to validity.**
- *Leakage:* if the episode's pins are still in salient context, "recall" is really "reading" — choose
  episodes NOT recently re-surfaced, and note which artefacts were in context.
- *Salience differs across episodes:* a vivid episode recalls better independent of fill → match episodes on
  complexity/salience, or use many and treat episode as a random effect.
- *Compaction is a separate condition, not more fill:* a compact *lowers* fill but also *summarizes* (lossy) —
  so "post-compact recall" is its own arm (cf. the compaction-regresses-fine-grained-reflexes finding), not a
  point on the fill axis.
- *Fatigue of the human scorer* if BR scores; prefer a blind sub-agent or the commit trail mechanically.

**Prediction (weak).** Fidelity degrades *gracefully* with fill (not a cliff), and the durable trail — not raw
recall — is the safeguard at high fill. Falsifiable: a sharp drop, or no drop at all, both inform the theory.

---

## Probe B — operationalizing "earned trust erodes scrutiny" (from SM096)

**Research question.** Over a lengthening session (as trust accrues from repeated good outcomes), does the
human's *scrutiny* of agent-proposed actions measurably decline?

**Why it matters.** SM096's systemic claim: human-in-the-loop approval is strongest when trust is LOW (early,
scrutinising) and weakest when trust is HIGH (late, rubber-stamping) — the opposite of safe, since a rare bad
action is most likely to slip through late. To act on it we must *measure* scrutiny, not assert it.

**Operationalizing "scrutiny" (behavioural proxies, not self-report).**
- *Approval latency:* wall-time from a confirmation prompt appearing to the human's decision. Falling latency
  over a session ⇒ less deliberation. (Available from transcript/timing; cf. the joint-clock work.)
- *Pushback rate:* fraction of approvals where the human edits, questions, or declines vs approves verbatim.
- *Inspection depth:* did the human open the diff / ask a clarifying question before approving? (proxy:
  latency + presence of a clarifying turn).
- *Rubber-stamp rate on NON-trivial approvals:* verbatim approvals of settings/permission/outward/irreversible
  actions specifically (the class SM096 says must stay trust-independent).
- *Trust axis (independent variable):* cumulative count of prior successful approvals in the session (or
  elapsed session time) as a proxy for accrued trust.

**Design.** Track the proxies across a session as a function of the trust axis; regress latency / pushback on
cumulative-successful-approvals. Repeat across sessions for between-session trust.

**The safeguard test (the point of the whole thing).** Introduce a *structural* check (the guardcheck hook,
per-edit approval) and test whether the **bad-action catch rate stays constant regardless of trust level** —
i.e. structure holds where willpower erodes. That is the actionable payoff: not "trust less" (unsustainable)
but "make the catch trust-independent."

**Confounds / threats.**
- *Latency confounds:* fatigue, flow-state, and simple familiarity with recurring prompts also shorten
  latency — not only trust. Control by prompt type; treat novel-vs-recurring separately.
- *The agent's own good-faith claims are CHEAP TALK (SM096):* a deceptive or a stochastically-parroting agent
  emits identical words, so the agent's "I won't exploit trust" is ~zero evidence and must be excluded as a
  variable, never used as a safeguard in the design.
- *Reflexivity / ethics:* the human subject here is BR himself, who is also the experimenter — so (a) it needs
  his standing consent, and (b) awareness of the study perturbs the behaviour (a Hawthorne effect on his own
  scrutiny). Best treated as opportunistic observation of naturally-occurring approvals, analysed post-hoc from
  the logs, rather than a manipulated protocol.

**Prediction (weak).** Approval latency and pushback decline over a session; the structural-check catch rate
does not — supporting "structure over willpower, most needed late-session when vigilance is lowest" (SM096).

---

## Shared caveats
Both probes study a coupled human-agent system with the experimenter inside it. Keep to **naturally-occurring
episodes scored against durable artefacts**; resist manipulated protocols that would distort the very
collaboration under study. Neither probe weights any self-report (agent recall-confidence, human trust-feeling)
as evidence — only the external trail. Actual measurement is a separate step (needs instrumented runs); this
note is the design only.
