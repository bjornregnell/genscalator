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

## Agent reasoning + blog home (pinned 2026-07-12)

**Reasoning link:** this incident *validates* the very principle the agent's own guardrails run on - confirm
before irreversible / outward actions, precisely so a momentum-slip (agent) or a reflex-tab (human) cannot
fire a consequential action. The tab-slip is the human-side proof that the principle is not paranoia:
frictionless commit of a consequential, machine-suggested payload is exactly the failure the guardrails exist
to stop.

**Blog home** (BR wants it placed; the human-agent exchange should also seed some mirth in the reader):
- **004 (Why Claude's UX sometimes sucks) = primary.** 004's own TODO already calls for "the mis-click that
  lands 'yes' on the wrong prompt" - this IS that, first-person. It also lets 004 *escalate* from "a UX
  papercut that guts the all-in human" to "the same frictionless affordance is a **safety** property" - the
  nuke analogy is the bridge, tying straight to genscalator's safe-by-design ethos.
- **010 (What we should be afraid of) = short callback.** Our tiny tab-slip is the homely, disarming version
  of 010's reframe (the mundane near-term danger, not the sci-fi singularity): a frictionless consequential
  action, scaled up, is the real fear. Light banter landing a dark point.
- **New post? Not yet** - 004 (UX-as-safety) + 010 (danger reframe) cover it; fold in. If the
  "friction-proportional-to-stakes / UX-is-a-safety-property" theme accumulates, it can graduate to its own BP.
