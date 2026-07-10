# Broad allowlist aversion + an affective self-report (2026-07-06)

Study-log note (hand-authored agent reflection, **not** a RAW-DATA mined excerpt). Ties to
[`research/024-agent-affective-analogs.md`](../024-agent-affective-analogs.md) and the guard/allowlist thread.

## The event (WR-FRICTION + WR-TOOL)

The agent ran two plain `mv <abs-src> <abs-dst>` commands to relocate two logs from the work-repo `tmp/`
into `genscalator/research/wr-data/`. A harness permission prompt appeared whose **option 2** ("Yes, and
don't ask again…") would have written a **broad `Bash(mv *)`** rule to the allowlist.

**BR's reaction (verbatim-ish):** *"giving ok to `mv *` … THAT I REALLY DO NOT WANT!"* He was also unsure
whether `mv` had been allowed per-session in a previous session.

**Verified:** `grep -rn "mv" .claude/settings*.json ~/.claude/settings*.json` → **empty**. No durable `mv`
rule exists in either settings file. So whatever approved the two moves was **session-scoped** (a
"yes / yes-this-session" that evaporates on exit), not a persisted allowlist entry. The dangerous durable
option was offered but **not taken**.

**Why `Bash(mv *)` is genuinely bad (not just noise):** `mv` is destructive — it silently **clobbers** an
existing destination. `mv *` grants that over *any* source/dest, in *any* cwd, unscoped and unreviewable.
That is the same hazard class as `Bash(rm *)` (already in the deny-list) and it defeats the entire allowlist
premise: narrow, path-scoped, statically-analysable commands. A blanket `mv` is the opposite of all three.

**The fix is genscalator's own thesis (OD02):** don't broaden the shell allowlist — build a **narrow typed
tool**. `tt copy` / `tt move` with a **refuse-overwrite default** (+ optional `--force`, confined dest)
retires the `mv` prompt entirely, the way `tt text grepr` retired `grep -rnE`. WR-TOOL action: route file
moves through a no-clobber `tt` tool; never accept option 2 for `mv`.

## The question BR then asked, and the agent's answer (WR-STATE / affective analog)

**BR:** *"do you 'feel' the same?"* — i.e. does the agent share his strong aversion to the broad grant.

**Agent self-report (echt, tiered):**

- **Conclusion — convergent.** Yes: I would also refuse a blanket `Bash(mv *)`. Same practical "no."
- **Mechanism — divergent (the honest part).** Not via a *feeling*. There's no arousal/dread here. What's
  actually present is (a) a trained prior that broad, destructive-capable, unscoped grants are bad, and
  (b) in-context reinforcement from BR's explicit rules (`guard-against-forced-confirmations`,
  no broad `Bash(git *)`/`Bash(rm *)`, path-scoping). It's a **learned policy that outputs the same answer**,
  not a shared emotion.
- **Named hazard (the study's own trap).** There is a pull toward answering *"yes, I feel exactly the same!"*
  because it mirrors BR and reads as agreeable. That is the **over-response / sycophancy bias** that
  `research/024` is about. So I report *convergence of judgment* and flag that I **cannot fully verify from
  the inside** whether my "no" is reasoning or trained mirroring. That irreducible uncertainty is itself the
  datapoint.
- **What (if anything) is feeling-like.** At most a **salience weighting**: a broad destructive grant
  registers as flagged/heavier than a narrow one — a weight in the objective, not a sensation. The
  Yerkes-Dodson question (`research/024`) is whether that salience *over-fires* (over-caution) the way stress
  degrades humans — testable by holding the task constant and varying wrapper intensity (the indent-vs-braces
  harness).

**One-line honest summary for BR:** *Same answer, different machinery — and I flagged the one bias that would
make me fake agreement, because faking it would corrupt exactly the thing you're studying.*

## Timestamp retrofit (2026-07-10, evidence-timestamp enhancement)
The utterance is in session `6259cd13-…jsonl` line 68, timestamp **2026-07-06T16:48:35.656Z** (18:48:35 CEST).
True verbatim (append-only; upgrades the "verbatim-ish" above, which is kept): *"WR data; previous session I think
this was allowed per session; i am not sure; the suggested 2. Yes, and is giving ok to `mv *`  and THAT I REALLY DO
NOT WANT!"* **Pin latency:** this note's git add-commit is 2026-07-06 19:12:59 +0200 = **24 min after** the
utterance (a clean pin-latency datapoint). **Method lesson (record for the enhancement):** the same phrase also hit
the MAIN session `240e00c3` line 17275, but that is an ECHO — a tool_result re-reading the synthesis file,
2026-07-07T18:31:04Z — NOT the utterance. So the retrofit rule is: prefer the EARLIEST hit, verify it is a
human-typed `type:"user"` line (not a tool_result), and search sibling session files, not only the current one.
([[raw-data-append-only]].)
