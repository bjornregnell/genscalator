# Guardcheck POSITIVE: it blocked a real agent slip (compound command), 2026-07-24 20:59

Most WR guard specimens record FRICTION — the guard stalling a benign command in front of a
present or absent human. This is the complementary evidence the corpus was thin on: the
guardcheck PreToolUse hook doing exactly its job, catching a genuine bad command the agent
emitted by reflex — not a contrived test, a real slip caught in the wild.

## The specimen
Deep in a long session (SM217 tool build + an introprog investigation), the agent (CF5) ran,
by reflex, to list two directories at once:

    tt files /home/bjornr/git/hub/bjornregnell ""  2>/dev/null; tt files /home/bjornr/git ""

The guardcheck hook blocked it with two findings:

    [HIGH] ; command chain: split into separate bare commands, one per call
    [MED]  stderr suppression (2>/dev/null): let the tool self-report to a file and Read it; tolerate benign stderr

Both are correct and actionable. The agent split it into two bare commands and proceeded — no
stall of a legitimate command, no wrong result shipped. The block prevented exactly the two
anti-patterns the guard-clean discipline forbids: a compound `;`-chain, and stderr suppression.

## Why it matters
The WR guard corpus skews toward FRICTION cases (benign commands that stalled), which argue for
TUNING the guard down. This specimen is the counterweight: on a real slip, the hook fired
correctly and cheaply, and the agent self-corrected in one step. It is direct evidence the hook's
true-positive rate is not zero — the friction buys real safety, not only cost.

The context strengthens it: the slip happened deep in a long, switch-heavy session — the exact rot
condition where the guard-clean reflexes fade ([[rot-vigil-guard-mechanical-precision-first]],
[[prohibition-does-not-arm-the-reflex-use-a-hex-escape]]). That is precisely when a STRUCTURAL
guard beats willpower: the hook caught what the faded reflex let through. A prohibition in a skill
did not arm the reflex; the PreToolUse mechanism did.

Ties `research/021-guardcheck-hook-proposal.md` (the hook design), `tools/guardcheck.scala` (the
implementation), the avoid-guard-stall skill (the reflexes it enforces),
[[guardcheck-hook-structural-fix]], [[guard-against-forced-confirmations]].
