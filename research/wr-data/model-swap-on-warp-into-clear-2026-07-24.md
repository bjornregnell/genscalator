# Model swapped when warping into a cleared window; human had to set it manually (2026-07-24 ~18:05)

Event (human-reported from the guard TUI, logged verbatim-in-substance): at the
2026-07-24 warp into a cleared context window — the planned CO4 (Opus 4.8) →
CF5 (Fable 5) model warp, see the model-warp memory and RT056 — the harness came up
on a DIFFERENT model than intended. BR had to set the model manually before the
cold start proceeded.

What the agent can verify from inside the session:
- The running session self-identifies as claude-fable-5 (harness-declared), so the
  manual fix landed; the statusline shows `f5·1M`.
- The prior session's ember was authored by Opus 4.8 and its §2 declared "on
  CO4/Opus 4.8 = big token headroom" — the model warp to CF5 was the PLAN.
- What the window initially came up as, and where the swap happened (clear vs
  resume vs launcher), is NOT observable from here — human report is the datum
  ([[agent-blind-to-input-channel-and-timing]]: the agent cannot see harness
  chrome events).

Open mechanism question (for a later probe, cheap): the cold-claude launcher
(SM171 addendum) carries a model-alias table and prints the model at its human
checkpoint — did the launch pass the intended model and the harness dropped it on
clear, or was the launch itself model-less? Deciding which changes the fix owner
(launcher script vs an upstream Claude Code report).

Cost: one manual correction by a tired human at a transition point that is
supposed to be low-friction — same friction class as today's
guard-ask-rule-help-form datum (transitions concentrate small frictions on the
human precisely when their attention is thinnest).

Related: RT056 model-warp study, [[exit-resume-dance]], SM221 (settings friction
pinned the same evening).
