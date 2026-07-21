# Approval-prompt arrow overshoot — the third menu-slip class (a UX HAZARD)

2026-07-21 16:53:35 clock-read. Live specimen minutes earlier: BR, mid-typing, fired the
v0.9.1 `tt forge release-create` (ask-gated BY DESIGN) and MEANT to approve — but his hands
were still in typing flow, an arrow key repeated past the intended row, and Enter landed on
NO. The agent's tool call came back rejected; BR: "oops did i press NO?"; re-fired, approved,
release created. Cost today: one retry, ~30 seconds.

BR's reading, verbatim in-feed (with his star-correction upgrading the term):
"i was typing and by mistake arrowed too far; this is also a UX quirk (this time it was a NO
but next time it could be BLANKET ALLOW) aaaaargh" — then "*UX quirk; UX hazard".

## Why it is a distinct class
The 004-blog menu-slip family now has three members, one per input pathway:
1. **Mouse**: the raise-window click that lands on always-allow (2026-07-07 near-miss).
2. **Focus-steal**: a prompt appearing mid-sentence eats typed characters as menu choices.
3. **Arrow overshoot (NEW)**: the human is AT the menu, WANTS to answer, and the selection
   walks one row past the intent because typing-flow momentum repeats an arrow key.
Class 3 is nastier than it looks: it defeats the "the human was present and deciding" story
completely — presence, intent, and attention were ALL satisfied, and the outcome was still a
dice roll between adjacent rows.

## The asymmetry (BR's point)
Today the overshoot landed on NO: benign, recoverable, one retry. The SAME motor slip on a
guard prompt lands on the always-allow row: a standing policy change. The menu prices a
one-time no and a permanent grant at the same cost, one keystroke, adjacent rows. Friction a
twitch can pass or fail is not a decision point.

## Harness-side ask (joins 004's ask list)
Permanent/policy options should not be reachable by the same single-keystroke motion as
one-time options: separate them spatially, or require a distinct confirm (type a word, hold a
key), or at minimum make the row visually heavy. The 004 rule "never click always" needs a
keyboard sibling: an overshoot-proof always-row.

Blog: beat drafted into media/blog/004 (AGENT-DRAFT, BR to revoice). Ties the-permission-layer
note, [[never-blanket-allow-destructive-commands]], [[cue-guard-stall]].
