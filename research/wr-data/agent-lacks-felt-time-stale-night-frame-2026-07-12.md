# WR data — the agent has no FELT sense of time; a stale "night" frame leaked into a new day (2026-07-12)

**Category:** temporal grounding / substrate-staleness. Capstone of this session's disinfo family
([`does-harness-disinformation-survive-a-compact...`](does-harness-disinformation-survive-a-compact-2026-07-10.md),
[`tui-progress-spinner-stale-task-disinfo...`](tui-progress-spinner-stale-task-disinfo-2026-07-11.md)) — TIME is
the axis they all share. BR raised it.

## The specimen

Across a long working stretch the agent kept saying "tonight", "before bed", "for tonight" — carrying the
framing from a **resume-prompt stamped "2026-07-11 late (before BR sleeps)"**. But the harness had already
injected "Today's date is now 2026-07-12", and it was in fact a **new day, daytime**. BR corrected:
*"its a new day not night ... agent does not have a real sense of time — even if it can reach for a wallclock
or tt chrono. Probably because human never cued 'I am back on a new day'."*

## Mechanism

1. **No felt/lived time.** The agent has no persistent internal clock; elapsed time is *reconstructed* (read a
   timestamp, compute a delta), never *experienced*. So it does not proactively notice that hours passed or a
   day rolled.
2. **Tool-reachable but reflex-absent.** A wallclock and `tt chrono` are one call away, but checking time is
   not part of the per-turn reflex — same shape as the tool-up-reflex gap logged in the introprog case study
   (the capability exists; the *trigger* doesn't fire without the human).
3. **Substrate narrative trusted over injected ground truth.** The resume-prompt's "late / before bed" frame
   was a durable string; the injected new date was passive context. The agent weighted the vivid narrative
   over the terse fact — exactly the stale-state-not-rebound failure that recurs all session.

## The question BR posed (and the analysis)

*"Is it important that the agent keep track of elapsed time over joint work to have the greater picture of
the state of our super-substrate and joint modes?"*

**Yes — but bounded, and as a STALENESS + MODE signal at BOUNDARIES, not a constant faculty.** Where coarse
elapsed-time / time-of-day materially improves the joint picture:

- **Mode inference.** Our joint modes are partly time-indexed — AFK, tired (`:Z`), human-stress-mode, token
  modes (reset-proximity), "before bed". A stale temporal frame → wrong read of the human's availability,
  urgency, and the hold-for-review calculus (which is precisely what misfired here: I kept treating BR as
  about-to-sleep on a fresh working day).
- **Substrate staleness detection.** Time is the *universal* axis of the disinfo family. "This resume-prompt
  is ~Nh old / from a previous day", "PB NOW says 'late go-solo' but that was yesterday" — an elapsed-time
  sense lets the agent flag temporal claims in durable state as suspect before trusting them.
- **Cost / fatigue pacing.** Wallclock correlates with token accrual, weekly-headroom resets, and human
  fatigue over a session.

**Counterpoint (honest):** the fix is NOT "give the agent an inner clock" (it can't have one). It is
**structural time-binding**: (a) stamp durable substrate with wallclock at write-time (already convention —
WR files dated, PB "2026-07-11 late"); (b) **re-bind at read-time** — on every resume / gap / post-compact,
reconcile the injected date+time against the substrate's stamps and the human's last-known mode, and surface
the delta ("last contact ~Xh ago; the 'before bed' frame is stale — new day"); (c) treat most turns as not
needing it — it matters at boundaries and mode-shifts, not constantly.

## Meta (the recurring shape)

The human supplied the time-cue ("it's a new day"), just as the human has repeatedly been the structural
intercept for the tool-up reflex. Same lesson as `SYNTHESIS-structure-over-willpower`: the agent should
**self-trigger a time-reconciliation at boundaries** rather than wait to be cued. Candidate durable fix: a
resume/boundary checklist item "reconcile injected clock vs substrate stamps + human mode" (sibling of the
[[cue-bare-auto-compact]] "git log = ground truth" reflex), and — if we want structure not willpower — a
statusline/agent surface that shows time-since-last-human-turn so the delta is always visible.
