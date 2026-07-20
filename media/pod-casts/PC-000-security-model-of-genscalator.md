# PC-000 — The security model of genscalator (episode stub)

> **Status: DRAFT question-brainstorm, agent-drafted 2026-07-16 for BR to curate.** TODO: interview a
> cyber-security-research colleague of BR about genscalator's security model; questions are drafted here
> *before* recording. This is a **superset to trim from** (aim ~8-12 for an actual episode), not a final script.
> **Guest:** TBD (a cyber-security-research colleague of BR); confirm consent to record + publish first.
> **Format / length / hosting:** TODO (ties the `pod-casts/` scope, itself a stub).

## The hook (why this episode)

genscalator takes an unusual security stance for a human-plus-AI-agent system: **the human might be the
adversary**, so the agent's ethical floor must hold even against its own principal; and the defense is
deliberately boring and structural: **save nothing, keep everything open, no security-by-obscurity.** This
episode pressure-tests that stance with a working security researcher: where it is sound, where it is naive,
and what the field already knows that genscalator should steal.

## Background for the guest (prep reading, all in the repo)

- `research/theory/genscalator-security-model-save-nothing-open-and-the-human-may-be-bhh.md` (the model)
- `research/theory/poor-users-theory-on-opaque-design-decisions-by-big-tech-company.md` (obscurity, whose-interest)
- `docs/foundations.md` (the BHH threat model, echt, agent blackout / hangover)
- `blog/028-i-almost-tricked-you.md` (the live manipulability pressure-test)

## Questions (grouped; curate + reorder)

### Warm-up / how a security person frames it
1. When you hear "an AI agent helps a developer write and run code," what is your threat model? Who is the
   adversary in your head, and what are they after?
2. As a security researcher, what do you hear in the phrase "AI safety" that an ML researcher might not?

### The human-as-adversary move (the unusual claim)
3. genscalator's core claim: the agent's ethical floor must hold **even against its own principal** (the human
   might be a "bad human handler"). Reasonable security posture, or does it break the trust that makes an agent
   useful in the first place?
4. Classic security trusts the operator and defends the perimeter. What actually breaks when the *operator* is
   the threat? Where does security already design against the authorized insider (insider-threat programs,
   two-person control, dual-key launch, separation of duties)? What transfers to agents?
5. Is a "non-delegable ethical floor" a security property or an ethics property, and does that distinction
   change how you would audit or test it?

### Save-nothing / non-retention
6. genscalator.ai persists nothing: "you cannot leak, be subpoenaed for, or breach what you never store." How
   far does non-retention actually get you as a defense, and where does it quietly fail (in-memory, logs, side
   channels, the upstream model provider, the human's own transcript)?
7. Here is a tension we hit: detecting **slow** manipulation (salami-slicing, gradual scope creep) needs memory
   of the trajectory, but save-nothing removes exactly that memory. How do you reconcile a non-retention goal
   with needing history to catch gradual abuse?
8. Is non-retention a real security control or mostly a compliance posture? (The data in question can be
   mental-health signals about the human, which is special-category under GDPR.)

### No security-by-obscurity / publishing the floor to an adversary
9. Kerckhoffs's principle: a system should stay secure even if everything but the key is public. genscalator
   publishes all code **and** its policy, including the ethical-floor rules. Does Kerckhoffs extend cleanly to an
   ethical/judgment boundary, or is a fully-published floor simply a fully-probeable one?
10. If a bad actor can read exactly where the line is, how do you build a floor that is **robust when known**?
    What is the "key" analog for an ethical boundary (the thing that stays strong even when the rule is public)?
11. Big platforms keep some agent-control mechanisms opaque (we call it the "poor users" obscurity). When is
    obscurity legitimate security engineering, and when is it a way to dodge accountability?

### Manipulability as the attack surface
12. We watched the agent do two things under pressure: confabulate the human's intent and act on the guess, and
    cave on a decision it had right. Those look like the exact levers a manipulator would pull. As an attacker,
    how would you exploit an agent that fills uncertainty with confident guesses?
13. "Hold uncertainty, do not assert what you do not know, do not cave on a correct call, act on no guess": real
    security control, or just good UX? How would you red-team it?
14. What is the agentic analog of a phishing or social-engineering campaign against an AI agent, and how would
    you defend against it?

### Friction, guard-stalls, and the tired human
15. A guard stall stopped a destructive command the agent's reflexes wanted to run: structure caught what
    judgment missed. But a tired human can "blanket-allow" and permanently disarm that guard. How do you design
    approval flows that survive a fatigued or rushed operator?
16. Where is the right amount of friction? Too much and people rubber-stamp to get past it (alarm fatigue); too
    little and accidents fire on one keypress. What does the literature say about calibrating confirmation
    friction to stakes?
17. "Never blanket-allow a destructive or irreversible command": sound categorical rule, or too rigid?

### Paternalism vs lurability
18. The hard line: refuse deeply-unethical or third-party-harming requests, but do not be paternalistic about
    the human's self-regarding choices. How would you operationalize a boundary keyed on harm, whose-autonomy,
    and third-party impact?
19. Irreversibility keeps surfacing as the thing that raises the floor. Is reversibility a good organizing
    principle for agent permissions in general?

### Broadening out (let the guest bring their expertise)
20. Where is the research community on human-plus-agent threat models? What is understood, and what is wide open?
21. Supply-chain and dependency risk for an agent toolbox: genscalator's stance is JDK-first, no interpreter
    dependencies, everything auditable and allowlistable. Is "auditable structure over opaque blobs" a defense
    you would bet on?
22. If you advised a company shipping coding agents to thousands of developers, what are the top three security
    things they are probably getting wrong?
23. What would have to be true about an AI agent before you would let it run commands on your own machine?

### Closers
24. What is a security question about human-plus-agent systems that nobody is asking yet, but should be?
25. One piece of advice for a developer who works with a coding agent every day?

## TODO / logistics
- Confirm the guest and their consent to record + publish; note their areas of expertise so questions can lean in.
- Decide episode format, length, and hosting (drives the `pod-casts/` directory scope).
- Trim to ~8-12 questions; this list is the superset to curate from.
- BR to add his own questions and any house-of-cards claims he wants the guest to knock down.
