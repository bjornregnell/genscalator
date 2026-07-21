# tt box lacks a local box-health shape (ps regression at the guard)

2026-07-21 ~13:1x. Live specimen, BR-flagged from the guard TUI during the
pre-warp bloop-restart step ("this should be tt box, no? WR data").

## What happened
BR issued `gs bloop restart` (why: see if the box gets less swamped). The agent
checked `tt box` for a blessed shape: its subcommands are models/df/gpu/freegb/
pull — ALL host-pinned REMOTE ops. No local shape exists for the two things the
task needs: a process/memory snapshot (before/after evidence) and the
BloopServer kill itself. The agent fell back to raw `ps -eo rss,pcpu,comm
--sort=-rss` → guard stall; the kill would next need raw `pkill -9 -f
BloopServer` (the documented interim mitigation from the 2026-07-19 box-health
probe) → a second stall.

## Reading
Same failure family as sandbox-clone-has-no-guard-clean-shape-tt-git-lacks-clone
(2026-07-20): a MISSING tt shape structurally forces raw commands regardless of
agent warming. The bloop drain is a RECURRING box condition on this machine
(10.4 GB RSS + ~39% CPU at the 07-19 probe; OOM-crashes GNOME), so the
mitigation dance recurs too — a strong candidate for a typed shape.

## Candidate tt extension (SM-candidate for BR's new-SM round)
- `tt box health` (or `top`): local read-only snapshot — top-N by RSS/CPU,
  free memory, load; allowlistable, gives the before/after evidence for free.
- `tt box bloop kill` (or `restart`): the pinned mitigation as a typed,
  narrowly-scoped op (matches only BloopServer; no shell passthrough), so the
  recurring dance stops stalling the guard.
Fits the dependency cascade (small, JDK-only ProcessHandle listing could even
avoid shelling to ps). Ties SM146 (native tools, no-bloop) as the long fix;
this is the interim ergonomics.
