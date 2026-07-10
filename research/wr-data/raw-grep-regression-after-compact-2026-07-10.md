# Raw `grep` regression, one turn after a compact (2026-07-10)

BR-caught specimen. Immediately after a manual compact, tasked with "look at PB NOW", the agent
reached for raw `/usr/bin/grep` to locate a heading in `PIN-BOARD.md` - violating anti-regression
checklist item #3 (*"Search: `tt text match|context|grepr`, never raw grep"*). BR: *"that grep again;
typical regression after compact?"* Yes - textbook. Two mechanisms stack.

## 1. recalled != enacted (the named post-warp failure mode)
The resume-prompt the agent read THAT SAME TURN opens with: *"Post-warp discipline degrades before
output does (recalled != enacted). Scan COLD."* The agent recited the rule and then broke it two tool
calls later. **Recall of a rule is not the rule firing at the point of action.** This is the exact
phenomenon the checklist header warns about - and it happened *against* a freshly-read checklist, which
is the strongest possible demonstration that recall-based discipline is not enough post-warp.

## 2. a tool-MISS is the regression trigger (not randomness)
It wasn't a random slip. The first attempt guessed a WRONG `tt` path (`genscalator/tt`) and exited 127.
A tool *failure* is what kicked the fallback instinct back to the **pre-tool habit** (raw grep) instead
of to *recovering the tool*. Path-uncertainty is highest right after a warp, when the agent has lost the
grounding of where tools live. So: **warp -> path uncertainty -> tool-miss -> fallback to old habit.**
The fix that actually worked was `which tt` (reground the tool), NOT "remember harder."

## Why it matters (the argument for Part B)
This is a clean, self-demonstrating case FOR the auto-firing PreToolUse guard
([[guardcheck-hook-structural-fix]]): discipline-by-recall is *precisely* what degrades post-warp, so a
structural guard that fires regardless of the agent's warp-state is the right layer. Part A (the checks +
hook mode) is built; Part B (wiring the settings hook) is the human-owned step that would have caught
this. Corollary mitigation for the interim: after a warp, **reground tool paths early** (`which tt`) as
part of the cold scan, and treat any tool-miss (127 / not-found) as a regression *trigger* - on a miss,
recover the tool, never fall back to the raw command. Ties:
[[does-harness-disinformation-survive-a-compact-2026-07-10]] (durable vs ephemeral across a warp),
[[guardcheck-hook-structural-fix]].
