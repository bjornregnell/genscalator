# WR data — TUI progress spinner shows DISINFORMATION from a stale in_progress task (2026-07-11)

**Category:** harness disinformation / progress-signal fidelity. Sibling of
[`does-harness-disinformation-survive-a-compact-2026-07-10.md`](does-harness-disinformation-survive-a-compact-2026-07-10.md)
(stale "Next:" line) and [`harness-status-line-can-misrepresent-a-trust-nit-2026-07-10.md`](harness-status-line-can-misrepresent-a-trust-nit-2026-07-10.md).
BR flagged it in-session: *"WR data: still some disinformation in the TUI progress animation"*.

## The specimen (BR pasted the live spinner)

```
✢ Building blog index page… (13m 44s · ↓ 43.1k tokens · thinking some more)
◼ Build blog index page for the published set
◼ Repro host-fix: parameterize modly host via MODLY_URL env
◻ Render Arc-2 set to HTML and hand off to BR for deploy
◻ Item-D tt-leaf tools (git overview + latin1 fix first)
◻ SM045 prep: draft ~10 fix-to-green pilot tasks + harness spec … +2 pending, 2 completed
```

The spinner headline read **"Building blog index page… (13m 44s)"**. The agent was NOT building a blog
index page at any point in this session — it was mid-way through reviewing introprog PR #940 (an AT job BR
had just handed over). "Build blog index page for the published set" was **task #5, left `in_progress` by an
EARLIER work-arc** (a prior session's blog pipeline) and never touched again.

## Mechanism (why the disinfo appeared)

The TUI derives the spinner's activity label from the **activeForm of the oldest `in_progress` task** in the
task list. Two failure links compounded:

1. **Agent-side:** a task (#5) from a superseded arc was left `in_progress` across session/arc boundaries.
   When this session's real work started (SM051 build, then the AT PR review), the agent created NEW tasks
   but did not first reconcile the OLD in_progress one. So the list held two+ `in_progress` entries, the
   oldest of which was semantically dead.
2. **Harness-side:** the spinner picks the oldest `in_progress` task to name the current activity, with no
   binding to what the agent is *actually* doing this turn. A dead task therefore drives a live, confident
   label ("Building blog index page… 13m 44s"), and the elapsed timer makes it look like sustained real work.

Net effect: a **confident, specific, wrong** status — the worst kind of disinfo, because specificity reads as
truth. The 13m44s + token counter lend it false credibility.

## Root cause & the general lesson

Same shape as the "stale Next: line" and "statusline trust nit" specimens: **a persistent UI element is fed
by state that is not re-bound to ground truth each turn.** The task list is agent-owned state that outlives
an arc; the spinner trusts it verbatim. Fidelity requires either (a) the agent closing/deleting stale
in_progress tasks at arc boundaries (discipline — weak, per the instruction-adherence-decay thesis), or (b)
the harness binding the spinner label to the actual current tool activity, not to a possibly-stale task
record (structure — durable).

## Resolution (BR's call)

BR: *"we will leave it so and hope it goes away next time we exit session for a box update — not today, maybe
tomorrow."* So the display is left as-is; a session exit/restart will clear the stale task and the spinner.
The agent did the minimal honest bookkeeping (moved #5 off `in_progress`, marked the real current task
`in_progress`) but did NOT chase the harness display further, per BR.

**genscalator angle:** progress/status surfaces (spinner, statusline, "Next:" line) are a recurring
disinfo family. The durable fix is structural — bind each surface to freshly-computed ground truth per turn,
never to un-reconciled persistent agent state. Discipline candidate: reconcile the task list (close dead
in_progress tasks) at every arc/session boundary, same reflex as refreshing PB NOW + the resume-prompt.
