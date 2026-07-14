# Waking up after a usage-limit head-hit — what survives a hard block (WR)

**Date:** 2026-07-14. **Model:** Opus 4.8 (1M); harness facts via a `claude-code-guide` consult (docs-cited).
**SM082.** Research + report; the actual wiring is JOINT (BR's hand). Ties the compact + exit-resume dances,
[[go-dance-autonomy-handoff]], SM077's SessionStart hook, the resume-prompt substrate.

## The problem (observed)

On a prior solo run, when the account hit a usage limit (the 5-hour rolling or the weekly cap) MID-run, the agent
went **idle at the limit hit and stayed idle until BR spoke** — even though solo work was still queued. We want:
after the limit RESETS, the queued work auto-resumes with no human poke.

## Harness truth (Claude Code docs, via the consult)

- **A limit hit BLOCKS the session until reset** (`errors.md`): "Claude Code blocks further requests until
  reset." Official recovery is manual — wait for reset, switch to an available model, or upgrade. **No documented
  auto-resume-at-reset pattern exists.**
- **`/loop` is session-bound** (`commands.md`): it "continues while the session stays open"; it does not persist
  across `/clear` or new sessions without a manual restart. Docs are silent on a limit hit specifically, but a
  loop depends on the session staying responsive between iterations → **inference: the loop stops when the
  session blocks and must be manually restarted post-reset.** The session state survives (`/resume`), the loop
  orchestration does not.
- **In-session self-scheduled wakeups** (a `ScheduleWakeup`-style timer): docs are silent. **Inference: unlikely
  to fire during a hard block** — the session is idle/blocked and the harness probably does not execute in-session
  timers in that state. Do not rely on it.
- **SessionStart hooks** fire only when a session is (manually) resumed — **not automatically at reset.**
- **Scheduled cloud routines** (`/schedule`, `routines.md`) run out-of-session on Anthropic infra — BUT
  (documented) **they draw down the SAME account usage limits** (5h + weekly) and additionally have a **daily
  cap** on runs per account. "When a routine hits the daily cap or your subscription usage limit ... additional
  runs are rejected until the window resets" (metered overage runs only if usage credits are enabled).

## The consequence

Nothing runs **during** a hard block. Every in-session mechanism (the loop, an in-session timer, the current
session) is frozen; the only out-of-session actor — a cloud routine — **shares the account cap** and so is
*also* rejected until reset. So there is **no mechanism that resumes work while the account is over its limit**,
by design. Auto-resume can only happen **after** the window resets.

## The viable design (auto-resume AFTER reset)

Given the above, the only pattern that works is an **out-of-session cloud routine that fires at/after the reset**
and re-launches the queued work:

- **A periodic self-healing routine (recommended).** Pre-schedule (BEFORE a solo run) a cloud routine that runs,
  say, hourly and: (1) checks a sentinel / the resume-prompt for unfinished solo work; (2) if present, launches
  it. During a block its scheduled runs are simply **rejected (harmless)**; the **first run after the window
  resets succeeds** and resumes the work. Self-healing polling — no need to predict the exact reset time.
  - *Constraint:* the **daily run cap** bounds polling frequency — don't poll too often, or the caps interact.
  - *Constraint:* it consumes normal usage, so scope what it relaunches.
- **A one-shot routine timed at the predicted reset** is the alternative if polling is undesirable: schedule a
  single run a few minutes after the 5h/weekly window is expected to reset. The consult flags this as
  *theoretically* sound but **not documented as a tested recovery pattern**, and it requires knowing the reset
  time — fragile vs the self-healing poller.
- **Do NOT rely on metered overage.** Routines can keep running past the cap only with usage credits (paid
  overage). Given BR self-funds and is cost-conscious ([[br-funds-claude-privately]]), auto-spending overage to
  beat the limit is the wrong default — leave it OFF; accept the wait-for-reset.

## What the routine needs to resume correctly

The routine is a fresh out-of-session actor, so it needs the **resume substrate** to know what to do: a
resume-prompt / pin-board `## NOW` describing the queued solo work and the anti-regression checklist (the same
substrate the exit-resume and compact dances rely on). This couples SM082 to the existing resume-prompt
discipline — the wake mechanism is only as good as the substrate it reads. It should also re-run `gs skills` /
`gs warm` at start (the cold-start reflexes are as absent for a routine-launched session as for any fresh one).

## Honest status + open (BR's hand)

- **No clean auto-wake exists**; the account cap is shared even by cloud routines, so the best achievable is
  **auto-resume shortly after reset** via a pre-scheduled self-healing routine — not resumption during the block.
- **Wiring is JOINT** (a routine/cron + a sentinel/resume-prompt contract + the settings): BR's hand.
- **Undetermined / untested:** whether an in-session `ScheduleWakeup` truly dies under a block (docs silent), and
  whether the "schedule at reset" one-shot is reliable at the window edge. Worth an isolated test.
- **Distinct from SM077's SessionStart hook** (cold-start reflex injection) — but they share the "a
  freshly-launched session must re-warm" need, so a routine-launched resume should invoke the same warm digest.
