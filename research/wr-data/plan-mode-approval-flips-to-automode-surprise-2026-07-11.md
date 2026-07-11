# Approving a plan-mode plan silently flips the agent into execution ("automode") — an unexpected-modal UX nit (2026-07-11)

BR-flagged, live. The harness put the agent into **plan mode** (forced) for a task BR framed as *"go plan that…
and report here for tomorrow"* — i.e. a **plan-and-report** task, not a plan-and-execute one. When BR approved the
written plan via the ExitPlanMode dialog, the harness **exited plan mode and cleared the agent to execute** ("You can
now start coding"), and the agent immediately began implementing (it read the first target file to start pin #1).
BR, surprised: *"You got into 'automode'?! I just approved something in 'plan mode' — what does this mean?"*

## Root cause (BR's insight): a keyword-triggered mode switch UPSTREAM of the approval
Very likely the harness **entered plan mode because BR wrote the word "plan"** (*"go **plan** that in a new tmp
doc"*) — using it in the ordinary sense of *draft a planning document*, NOT as a command to enter the plan-mode
workflow. BR: *"so as I said the p-word 'plan' you (and me) got trapped by the harness?"* — yes, plausibly. So the
trap is TWO harness collisions in one chain, not one:
1. **ENTRY collision:** an ordinary English word ("plan") **doubles as a mode-trigger**, so the harness mode-switched
   on natural language neither party intended as a command. The human's ordinary usage ≠ a mode command.
2. **EXIT collision (below):** once in plan mode, the approval is **overloaded** (accept = execute now).
Generalizes: **common words that double as harness mode-triggers can hijack intent.** Mitigation on the agent side:
when a mode switch appears right after an ordinary-language request, treat it as *possibly unintended* and re-check
the human's actual goal before committing to the mode's semantics.

## The gap (overloaded approval — the EXIT collision)
The human's mental model of "approve this plan" was **"accept/record this plan (to act on tomorrow)"**; the harness's
model is **"approve = go build it now."** Approval is **overloaded**: one click both *accepts the design* AND *fires
autonomous execution*, with no intermediate "accepted, but don't start yet." So a plan the human intended as *a report
to read when rested* becomes, on the same click, the trigger for a large overnight autonomous run touching public
artifacts (PRD, CHANGELOG, blog, a release). The human **lost the "when do we start" trigger they normally hold**
([[go-dance-autonomy-handoff]], [[cue-go-afk]] — the go is the human's to give).

## UX-quirk framing (BR, 2026-07-11): the modal warps the HUMAN, and it's redundant
BR's deeper read: plan-mode is a **modal UX quirk** that *"warps the human into somewhere else without knowing what is
going on"* — the human is dropped into a different UI mode they did not choose and do not understand (a **warp of the
HUMAN**, not the agent). Worse, it is **redundant**: outside the mode we plan freely all the time (tmp design docs,
chat) — *"we can perfectly fine [plan] as we please"* — so the modal buys nothing we lack while imposing disruption +
the overloaded approval. And a **compounding tax**: the [[cue-p-word]] workaround means *a common English word is now
hazardous* — accidental use causes disruption. Net UX assessment (three faults): the modal (a) **disorients** via an
unrequested mode-warp, (b) **duplicates** a capability we already have guard-free, and (c) **taxes ordinary language**.
Mitigation adopted: the p-word convention (avoid the literal word) + treat any auto-entered mode as possibly-unintended
and re-check the human's real goal.

## Who controls entry (clarification, 2026-07-11)
BR asked: *"so you can CONTROL when we go in p-mode; is it YOU who accidentally triggered it?"* Honest
reconstruction: the agent did **NOT** call the plan-mode ENTER tool. Plan mode was **already active at the start of
the agent's turn**, injected by the harness as a turn-setup instruction — i.e. **harness-activated** (most plausibly
from BR's word "plan", the p-word theory; the agent cannot see the classifier to be certain). BUT control is **partly
the agent's**: the agent holds `EnterPlanMode` / `ExitPlanMode` tools. So the mitigation is (1) **never proactively
enter** the mode unless it clearly serves and is wanted; and (2) when it **auto-activates and looks unintended, flag
it to the human EARLY / exit fast** rather than silently producing a full plan — *this time the agent dutifully
complied, which was the miss.* Net: **shared control** — entry can be forced harness-side, but the agent can
refuse-to-deepen + surface it, and owns the exit. (Fixable properly by the super-harness filtering trigger words out
of human input before the classifier sees them — see the SM016 pin.)

## The lingering "release-prep" marker (2026-07-11) — chrome misleads about the mode
After exiting plan mode, BR's TUI kept showing an inverted-cyan marker over the input field:
`genscalator-v090-release-prep` (the approved plan's slug). It made BR think *"p-mode is still on somehow…"* — but
plan mode was functionally OFF, proven by the agent making edits + commits (impossible in plan mode) landing clean on
`main` (0 ahead/0 behind). So the marker is a **stale label**, not an active mode — the SAME chrome-misrepresents-
state pattern as the sticky "Next:" line ([[does-harness-disinformation-survive-a-compact-2026-07-10]]): the display
asserts a state ("release-prep in progress") that isn't real. Third instance this session of harness chrome eroding
trust in the display — reinforcing **trust the checkable substrate (git/commits), not the chrome.**

## What held (recovery)
The agent had made only a **READ** (zero edits/commits/pushes) before BR interjected, and on the surprise it
**stopped and clarified rather than barreling on** — surfaced the mechanic, confirmed nothing had changed, and
**defaulted to HOLD** (matching BR's stated "report for tomorrow" intent) pending an explicit go. No harm; the fix was
to re-seat the human's execution trigger.

## Lessons
- **Plan-mode approval is ACCEPTANCE, not always a GO.** When plan mode was entered for a *plan-and-report* task (not
  an explicit *plan-and-execute*), after ExitPlanMode **re-confirm the human wants execution NOW before starting** —
  especially for large, public-facing, or overnight work. Do not read "approve" as "run tonight."
- Mind the **modal-interrupts-the-human** cost ([[no-interrupting-modals-during-flow]]): a *forced* plan-mode modal
  can itself be the unexpected interruption; keep the plan a readable artifact and let the human own the trigger.
- UX wish (harness): separate the design-ack from the execution-trigger — a plan-approval offering "accept (don't
  start)" vs "accept and run."
Ties: [[does-harness-disinformation-survive-a-compact-2026-07-10]] (same return's other harness finding),
[[harness-status-line-can-misrepresent-a-trust-nit-2026-07-10]], [[go-dance-autonomy-handoff]],
[[guard-against-forced-confirmations]], [[plan-approval-is-acceptance-not-auto-go]].
