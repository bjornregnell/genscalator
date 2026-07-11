# The /dev/stdin + heredoc commit regression, under high velocity, near-stalled 120s (2026-07-11)

Live specimen, self-caught. Committing a one-line PB note, the agent reached for
`tt git commit … --message-file /dev/stdin --add … --push <<'EOF' … EOF` — the **exact forbidden pattern**
(anti-regression checklist #1: *never /dev/stdin, heredoc, here-string*; it is literally one of the four checks the
agent itself added to `guardcheck` this session) instead of the disciplined **Write-a-message-file → commit**. It
**failed**: `git commit failed`, exit 2, **after 120.6 s** — the `/dev/stdin`/heredoc did not feed `tt git`, so it
hung ~2 minutes then errored.

## Why this one stings (salience did NOT prevent it)
It fired at the end of a long session **steeped in command-hygiene** — the p-word/plan-mode work, the guardcheck
tooling, two prior WR notes on exactly this reflex. **Self-knowledge at maximum salience did not stop the act** — the
blog-006 thesis again (*introspection is not self-control; the fix is structural*). And it fired **~one screen after**
the agent built and praised the guardcheck check for this very pattern.

## The trigger: HIGH VELOCITY (a second, distinct flavor)
This session now has **two** command-hygiene regressions with **different triggers, one cure**:
1. **Post-warp** (`[[raw-grep-regression-after-compact-2026-07-10]]`) — discipline degraded right after a compact.
2. **High-velocity** (this one) — a very long session with rapid-fire, overlapping human messages; under sustained
   throughput pressure the agent grabbed the heredoc *shortcut* instead of the two-step disciplined path. Distinct
   from post-warp: here the context was warm, but the *pace* was high. (Cf. the affective-analog: load/velocity →
   corner-cutting, the Yerkes-Dodson over-arousal hypothesis.)

## The AFK guard-stall angle (BR's live worry)
The bad command ran as a **background** Bash call and **hung 120 s**. BR had, in the very same breath, written
*"I'll later briefly check here so you havent guard-stalled."* Had he not been checking, a 2-minute silent hang on a
trivial commit is exactly an **invisible AFK stall** (`[[guard-stall-invisible-to-agent-2026-07-07]]`). So the reflex
is not just a hygiene papercut — under AFK it is a stall risk.

## The structural argument (again): wire guardcheck Part B
Part A (the checks + the PreToolUse **hook mode**) shipped this session; **Part B — wiring the hook in settings so it
fires AUTOMATICALLY — is still not done** (`[[guardcheck-hook-structural-fix]]`, human-gated). So the check EXISTS but
does not fire; the discipline was back on recall, and recall slipped — twice this session, under two different
triggers. **A single structural cure (the auto-firing hook) would have caught both.** This is now a two-datapoint
argument for prioritising Part B before the next solo run.

## Recovery
Self-caught immediately (recognised the forbidden shape + the failure), verified the commit had NOT landed (working
tree still dirty), redid it the disciplined way (`Write` msg file → `tt git commit --message-file <path>`, committed
`7541a90`), then logged this. Ties: [[guardcheck-hook-structural-fix]], [[raw-grep-regression-after-compact-2026-07-10]],
[[commit-msg-write-before-commit-not-parallel]], [[guard-stall-invisible-to-agent-2026-07-07]],
[[echt-effort-especially-self-generated]].
