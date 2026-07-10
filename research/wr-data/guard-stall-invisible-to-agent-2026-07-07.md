# WR data — a guard-stall is structurally INVISIBLE to the agent (only the human sees it), 2026-07-07

**Context.** During the Go #2 autonomous ralph loop (BR asleep/AFK), BR observed that the loop had been
**halted by a guard prompt** and asked which command and when. Investigation (reading the active allowlist
`.claude/settings.local.json`) found the culprit.

## The regression (5th command-hygiene specimen)
`tail` is **not in the allowlist at all** (grep/grep -n, ssh, scala-cli, tt, git -C are; tail is not).
During the loop the agent used **`tail -n N <file>` repeatedly** to peek at the background coding-matrix
collection's progress (checking `.../tasks/<id>.output`). Each `tail` was an un-allowlisted bare command,
so each **triggered a permission prompt that stalled the AFK loop** until BR cleared it. The correct tool
was the **Read tool** (allowlisted for `/tmp/claude-1000/**`) — the background-bash guidance itself said
"use Read on that file path." The agent reached for the shell `tail` habit instead. Timing (bracketed by
commits, no per-command clock available): the box-progress tails ran **~01:00 to ~01:56 CEST**; a suspicious
earlier `tail -n 30` on the study log (returned "no output") was ~00:40-00:50, possibly the first stall.

## The finding (why this is more than a hygiene slip)

**A guard-stall that is later approved is perceptually invisible to the agent.** When a prompt blocks a
command and the human *later approves* it, the command returns its **normal output**. There is no "you were
blocked for N minutes" metadata, and the agent has **no clock between tool calls**. So the agent **cannot
distinguish "ran instantly" from "stalled for an hour then got cleared."** Evidence (from the agent's own
side): it ran `tail` five-plus times and **never registered a single stall** — every result looked normal;
it only learned the loop had halted when BR *told* it. The agent is **time-blind and stall-blind between
calls**.

**Consequences:**
1. **A new, concrete instance of the joint-rot-vigilance asymmetry.** The agent has a *structural blind
   spot* precisely where the human has vision. This is not carelessness that better attention fixes; it is
   **perceptual incapacity** — a cleaner form of the corroboration-asymmetry limit ("there is a failure mode
   I cannot sense at all," not merely "my self-report is unreliable"). [[joint-rot-vigilance-recovery-kit]]
2. **A third independent argument for structure-over-willpower.** We already had: discipline regresses under
   load (Dim 15), and willpower does not survive a warp. Add: **you cannot will yourself to stop making a
   mistake you cannot perceive making.** No amount of care detects an invisible stall; only *structure*
   (guard-free-by-construction / an allowlist-clean command set) or *the human* prevents it. This is why
   guard-free-by-construction is load-bearing for AFK — not to avoid annoyance, but because the failure it
   prevents is **agent-undetectable**. [[guard-against-forced-confirmations]] [[hardening-dance]]
3. **The irony:** the command that stalled the loop was `tail` — the agent being *diligent*, monitoring the
   background job. The **vigilance act was itself the friction source**, and the agent was blind to it. A
   second specimen of the researcher enacting the study's own thesis (after the t=0 `cd`-git regression):
   an apparatus built for "guard-free AFK," written into the resume prompt, then silently tripping a guard
   five times unnoticed.

## Structural fixes
- **Immediate (hygiene):** check background/task output via the **Read tool** (allowlisted), never shell
  `tail`. Add to the resume-prompt anti-regression checklist. (Or get `tail`/a `tt tail` shape allowlisted —
  a human-approved allowlist change, not agent-side.)
- **Deeper (design):** since stalls are agent-invisible, an AFK loop cannot self-correct them; the only
  robust guards are (a) a genuinely guard-free command set (verified against the allowlist BEFORE the run,
  which Go #1 did for the planned shapes but `tail` slipped in as an ad-hoc progress-check), and (b) the
  human as the sole detector. A possible tooling fix: a harness signal that a command was delayed by a
  prompt (would make the stall *visible* to the agent, closing the blind spot) — flagged for BR, not
  agent-buildable.

## Echt caveats
- The stall is **inferred, not directly observed** by the agent (by definition — it is invisible to the
  agent). BR observed the halt; the allowlist confirms `tail` is uncovered; the agent confirms it ran `tail`
  repeatedly and noticed no stall. That triangulates strongly to "tail stalls were the halt," but the agent
  cannot *directly* witness its own blind spot, which is the whole point.
- "Invisible" is demonstrated by the agent's **absence of any stall-detection**, consistent with (and strong
  evidence for) structural invisibility, short of a formal proof.

Candidate for the 047 writeup/blog: *"the agent is time-blind and stall-blind between calls"* is a real
contribution to the human-AI collaboration picture, and a fourth reflexive specimen of the study's themes.
[[at-code-plan-and-introspection]] [[echt-effort-especially-self-generated]]

## Timestamp retrofit (2026-07-10, SM044a-S3) — the first stall pinned to the second
Retrofitted via `tt wr stamp` on the muntabot project dir (append-only; the commit-bracketed estimates above stand).
- **The first suspicious tail is now exact:** the `tail -n 30` on the study log (the one that returned "no output",
  §"The regression", felt as "~00:40-00:50 CEST") fired at **2026-07-06T22:45:02.479Z = 2026-07-07 00:45:02 CEST**
  (session `240e00c3`, `tail -n 30 …/research/047…`). The felt bracket was right to within minutes — a clean
  member-check of the estimate.
- **The box-progress window** (§"The regression", the `.../tasks/<id>.output` peeks, felt "~01:00 to ~01:56 CEST")
  sits in the same session's transcript right after; every `tail` tool_use carries an exact `timestamp`, so the full
  per-command stall profile (tool_use time → next human-approval message = the invisible stall duration) is
  MEASURABLE by the same method — not fully enumerated here (lower marginal value than the first-tail confirmation),
  flagged as a cheap follow-up if the 047/blog writeup wants a per-command duration chart.
- Method caveat (why "measured" beats "felt"): transcript timestamps are UTC; +02:00 gives CEST. Perishable under
  `cleanupPeriodDays` — done now while the session file survives. Ties: [[wr-stamp-dogfood-transcript-format-2026-07-10]],
  [[raw-data-append-only]].
