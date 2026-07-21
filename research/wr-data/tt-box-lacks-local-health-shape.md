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
avoid shelling to ps).

## ADDENDUM minutes later — the dance costs a stall PER PROBE
BR flagged the SECOND raw-ps stall (`ps -eo rss,pcpu,args`) within minutes: the
first probe (comm) could not identify which java was which, so a second probe
(args) was needed — and `pkill -f BloopServer` had already missed (exit 1: the
2026-07-19 process name is gone; today's servers are named differently, which
ALSO argues for a typed shape that knows the current process fingerprints
rather than a memorized pkill pattern). Cost accounting for ONE bloop-restart
dance: 2+ ps probes + 1 kill = 3+ guard stalls, each needing a present human.
The before/after-evidence pattern (the why BR gave) doubles the probe count by
design. A `tt box health` shape amortizes all of it to zero stalls.

## ADDENDUM-2 — the kill saga completes the case (BR: "should be tt")
Full sequence to actually kill the daemon (10.9 GB RSS, 117% CPU at kill time):
1. `pkill -9 -f BloopServer` → exit 1, no kill.
2. `pkill -9 -f scalacli/bloop` → exit 1, no kill.
3. `pgrep -af scalacli` → daemon plainly visible (PID 990931) — AND the probe
   revealed the likely pkill killer: the harness wraps every command in
   `bash -c 'eval …'` whose OWN cmdline contains the pattern text, so a
   `pkill -f` match set includes the wrapper shell executing the pkill.
   Pattern-kills are SELF-REFERENTIAL under this harness.
4. `kill -9 990931` → success (BR flagged this too as should-be-tt).
Tally for one bloop restart: 2 ps probes + 1 pgrep + 3 kill attempts = 6
guard events, 2 silent failures, one live human required throughout.
Design consequence: the typed shape must resolve targets via JDK
ProcessHandle (inspect cmdline in-process, kill by PID handle) — never via
shell pattern-matching, which this specimen shows is unreliable in TWO
independent ways (self-reference; and fragile memorized patterns as process
naming drifts across toolchain versions). Ties SM146 (native tools, no-bloop) as the long fix;
this is the interim ergonomics.
