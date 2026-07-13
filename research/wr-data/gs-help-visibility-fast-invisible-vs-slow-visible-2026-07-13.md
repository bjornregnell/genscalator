# WR data: gs help visibility hiccup — fast-render is invisible, agent-paste is slow (dumb-zone + racing, 2026-07-13)

Surfaced during the live plugin dogfood at **ctx-fill 78% (dumb-zone)** with feeds **racing**.

## The hiccup cluster
- **`gs help` invisible**: `gs help` runs `tt doc gs-help` (fast subprocess cat of `docs/gs-help.txt`), but BR's UI
  HIDES raw Bash tool-output, so he saw NOTHING ("I see NOTHING of that"). The gs-dwim skill's "let the subprocess
  render, don't re-emit token-by-token" optimization **silently fails** for a user who doesn't see tool-output.
- **Agent-paste is slow**: re-emitting the help as agent text IS visible but prints token-by-token (~0.5 s/line —
  BR: "it printed slow, almost one line at 0.5 sec"). So the tradeoff is real: **fast + invisible vs slow + visible.**
- **The cleaner fix is likely a UI toggle, not slow-paste**: if the user makes tool-output VISIBLE, the fast
  `tt doc` render shows and beats the slow paste. The gs-dwim skill was updated to prescribe paste (visible fallback),
  but the better resolution may be user-side visibility, or a hybrid (a short agent line + the fast render).
- Sits alongside the reload "0 skills" / "/skills: No changes" under-reporting
  ([[skills-off-fix-confirmed-live-and-plugin-install-ux-quirks-2026-07-13]]).

## Meta (the load-bearing bit)
These hiccups surfaced AND were slow to resolve **because the agent is in the dumb-zone (78%) while the feed races** —
itself a data point: the dumb-zone degrades the joint human-agent debugging loop (slower agent output, more back-and-forth,
misreads). An argument for warping (exit-resume) BEFORE deep UX-debugging, not during. Ties SM074 (discoverability),
[[cue-we-are-racing]], the warp-trades-rot-for-memory-loss note.
