# Friction event: compound verification command halted overnight run on approval prompt (2026-07-07)

**Context.** Sub-task: final peer review of `research/047-writeup.md`. While spot-checking writeup numbers against data, the agent issued a compound Bash call (`grep ... | head; echo ===; python3 -c "..."`) to count distinct tasks in `coding.jsonl`.

**What happened.** The compound shape (semicolons + `python3 -c`) defeated the bare-command allowlist and raised "This command requires approval" while BR was away; the run stalled overnight until BR returned and approved.

**Why it matters (recurrence).** This exact failure class is already in durable memory (`prefer-inrepo-tmp-over-slash-tmp`, `guard-against-forced-confirmations`, `prefer-scala-scratch-over-bash`): compounding defeats the allowlist and races approvals. The rule was *recalled but not enacted* — an in-vivo instance of study 047's own facts-carry / enactment-leaks pattern, in the reviewer this time, one day after the same regression class was logged for the researcher agent (`command-hygiene-regression-2026-07-06.md`).

**Cost.** ~1 overnight stall of an otherwise-finished review task; zero data loss (prior verification results had already returned).

**Correct shape that was available.** A scala-cli scratch program (or `tt text grepr`) for the jsonl task-count; bare single commands only during unattended work.
