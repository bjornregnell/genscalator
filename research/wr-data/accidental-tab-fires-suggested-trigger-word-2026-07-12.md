# WR data — an accidental TAB-accept fired a machine-suggested trigger word; UX brittleness (2026-07-12)

**Category:** human-agent communication-channel brittleness / accidental consequential action / stakes-vs-
friction. Ties [[cue-p-word]], [[guard-against-forced-confirmations]], and the confirm-before-irreversible
principle. BR flagged it.

## The incident

The harness's **pre-prompt suggestion** (the greyed-out autocomplete) contained the plan-mode trigger word.
BR pressed **TAB** — a single, very low-friction key that accepts the suggestion — and thereby *accidentally
fired the trigger word* he had not meant to type. BR: *"the p-word was in the pre-prompt suggested by the
harness so I accidentally fired it away with that tasty tab key that is so easy to press."* (Clarification:
those suggestions are the **harness's** autocomplete, not text the agent emits — so the human fired
machine-suggested content, unintentionally.)

## Mechanism — three hazards stacked

1. **Frictionless commit.** A consequential input (a word that can flip the harness into plan-mode) was
   committed by ONE cheap keystroke, with no confirmation.
2. **Machine-suggested payload.** The fired text was not even the human's own — it was auto-suggested. So the
   human accepted a *machine's* proposal by reflex.
3. **A hidden trigger.** The word carries a mode-changing side effect the human was not thinking about at the
   moment of the tab. Reflex + suggestion + hidden effect = accidental action.

## The thesis (BR's nuke analogy)

*"This is a UX incident that shows how brittle our communication can get — not a big deal for us, but a big
deal for human kind if I just had (hypothetically) let the bombers go nuking folks."* The failure **class** is
scale-invariant: a high-consequence action placed behind a **low-friction, easily-mis-fired affordance** is a
catastrophe waiting for the wrong context. Here the payload was a harmless mode word; swap in an irreversible
real-world command and the same one-tab reflex is a disaster. **Design principle:** friction must be
**proportional to stakes** — confirmation, a deliberate second act, or an undo window for anything
consequential; never a single cheap keystroke, *especially* when the payload is machine-suggested.

## Symmetry (a blog-021 datapoint)

This is the **human side** of the same accidental-action hazard the guardrails watch on the **agent side**
(the agent misfiring a consequential command under momentum). Both parties can mis-fire through low-friction
affordances; both need structure, not vigilance, to prevent it. The communication channel between human and
agent is not only *lossy* (the laughter/affect asymmetry) but *injection-prone* — autocomplete, tab-accept,
and trigger words can fire signals neither party intended. Good material for blog 021 (the channel is brittle
in both directions) and for genscalator's guard design (do not put consequential ops behind frictionless
accepts).

Ties: [[cue-p-word]], [[guard-against-forced-confirmations]], [[no-interrupting-modals-during-flow]], blog 021,
`harness-ux.md`.
