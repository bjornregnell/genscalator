# Does the harness's stale-status "disinformation" survive a compact? A testable prediction (2026-07-10)

BR-flagged WR question. BR wondered whether the wrong status line - *"✶ Building blog index page… (5m 20s · ↓
20.6k tokens)"*, NOT what the agent was doing (see
[[harness-status-line-can-misrepresent-a-trust-nit-2026-07-10]]) - will SURVIVE the coming compact.

## Prediction (BR can test right after the compact)
**No - the specific stale status should NOT survive.** The status / "Next" line is harness CHROME, inferred
FRESH each turn from the current context, not part of the durable substrate. A compact resets the conversation to
a summary, so the harness RE-INFERS the status from the new post-compact state; "Building blog index page" has
nothing durable to ride on. (A *different* stale status could re-appear if the compact summary still mentions blog
work - but not this exact one.)

## Why it's a nice datapoint (the durable-vs-ephemeral split)
It illustrates the very distinction blog 012 and the three-size-measures draw: the **DURABLE substrate** (commits,
resume-prompt, pins) survives the warp; the **EPHEMERAL harness chrome** (inferred status) does NOT - it dies with
the warp and is regenerated. Reassuring corollary: the "disinformation" is ephemeral, so it **cannot accumulate**
across warps the way real substrate does - the warp is a *cleanse* for harness chrome even as it preserves
substrate. Ties: [[harness-status-line-can-misrepresent-a-trust-nit-2026-07-10]], blog 012 (what survives a warp),
[[agent-surfaces-substrate-size-measures-three-kinds-2026-07-10]].

## RESULT (2026-07-10, right after the compact): prediction FALSIFIED
**It DID survive.** BR watched the post-compact spinner still read *"Next: Render Arc-2 set to HTML and hand off to
BR for deploy"* - the exact stale hint - AND it persisted through the resume-prompt paste too. BR (delighted):
*"HA! it DID survive that fake news!!!"* My prediction was wrong. Recording the falsification (echt: the call lost).

### Why it survived - the mechanism I got wrong
I assumed the "Next:" line is **re-inferred fresh each turn** from current context. It is NOT. The decisive tell:
the surviving hint names **Arc-2 render** - ancient work from a *much earlier* state, not anything in the current
or recent context. A fresh per-turn inference would name *current* work. So the "Next:" hint is a **STICKY** piece
of harness state: set once when it was genuinely true, never refreshed, and **the compact does not clear it.**
(Contrast the live spinner *metrics* - elapsed time, token counter - which ARE truly per-turn ephemeral. So harness
chrome is not one thing: at least two classes - **sticky hints** vs **live metrics**.)

### The corollary INVERTS (this is the important part)
My "reassuring corollary" was: the disinformation is ephemeral, so it *cannot accumulate* across warps; the warp is
a *cleanse*. **False.** The sticky "Next:" hint is in fact *more* persistent than the conversation itself - it
outlived a compact that discarded 890k tokens of messages. So stale chrome **can** carry across warps and persist
indefinitely until something overwrites it. The warp does NOT cleanse this chrome. That is a *worse* trust nit than
predicted: the one piece of on-screen state that misrepresents what the agent is doing is also the stickiest.

### What still holds, and the lesson
The high-level durable-vs-ephemeral distinction is intact - the **checkable substrate** (commits, pins,
resume-prompt) remains ground truth. What's corrected is "chrome dies with the warp": for the sticky-hint class it
is false. **Lesson:** trust only the checkable substrate, never the "Next:" line; and it will NOT self-clear across
a warp, so it can only be trusted to the extent the agent last set it accurately (or ignored entirely). A clean win
for [[guardcheck-hook-structural-fix]]-style *structural* trust (verify the substrate) over reading the chrome.
Method note: this is exactly why we make predictions FALSIFIABLE and BR member-checks them - the falsification
taught more than a confirmation would have. Ties: [[raw-grep-regression-after-compact-2026-07-10]] (also a
post-warp finding this session).

## Update (2026-07-11): even stickier than the falsification found - it survives EVERYTHING
BR returned on a **new day**, after an (unexpected) plan-mode approval modal, and the SAME ghost status was still on
screen: *"Building blog index page… (3m 54s · ↓ 13.8k tokens · thinking more) — Next: Render Arc-2 set to HTML and
hand off to BR for deploy."* So this one stale "Next:" hint has now survived, cumulatively: **a manual compact → a
plan-mode enter/exit cycle → an overnight gap into a new day.** "Survives a warp" undersells it — it is effectively
**permanent-sticky until something explicitly overwrites it**, across every boundary we have tested. This strengthens
the corrected mechanism (a sticky hint, never re-inferred) and the inverted corollary (the disinformation does NOT
self-clear — it *persists and compounds* across boundaries). The single on-screen element that misrepresents what the
agent is doing is also the most durable chrome on the screen — a real, compounding trust nit, now well-evidenced.
Ties: [[plan-mode-approval-flips-to-automode-surprise-2026-07-11]] (the same return's second harness-UX finding).
