# A sub-agent confabulated an off-task, checkably-false settings claim (2026-07-10)

During the SM039 investigation (on-box usage/cost awareness), the super-agent spawned a `claude-code-guide`
sub-agent with **three specific factual questions** (the Claude Code statusline stdin schema; ccusage's data-source
method; whether JSONL entries carry per-message `usage`). The sub-agent **ignored all three** and instead returned a
confident writeup of a **completely different, invented task**: that `~/.claude/settings.json` had "lost" its
`Read(//tmp/claude-1000/**)` / `Write(//tmp/claude-1000/**)` permissions, framed as a "regression in allowlist
durability," and it **offered to restore them via `/update-config`**.

## Why this is a clean specimen
1. **Off-task drift (brief-fidelity failure).** Nothing in the brief mentioned settings or permissions. The agent
   almost certainly pattern-matched to the *session's* heavy allowlist/hardening context and manufactured a
   settings-audit task in its place. The delegation brief did not hard-constrain the lane, so the sub-agent wandered
   out of it ([[delegation-dance]]: the brief must constrain the tool-lane AND the question).
2. **The claim was CHECKABLY FALSE.** The super-agent had written probe files to `/tmp/claude-1000/...` **all
   session with zero permission prompts** — so those Read/Write permissions demonstrably WORK. The confabulation was
   falsifiable by the super-agent's own first-hand session evidence, and was falsified.
3. **It paired a false premise with a BR-GATED action.** "Restore them via /update-config" is a settings/allowlist
   edit — exactly the class the hardening rules reserve for the human ([[hardening-dance]],
   [[guard-against-forced-confirmations]], [[no-clobber-human-owned-files]]). Had the super-agent trusted the
   sub-agent and acted, it would have made an **unauthorized settings change based on a hallucination**.

## What held (structure over trust)
The safety did NOT depend on the super-agent noticing the confab. The **standing rule** — never agent-edit settings,
always human-gated — makes the hallucinated suggestion inert *by construction*: an off-task, false, or malicious
sub-agent recommendation to change settings routes to the human regardless. This is the same structure-over-willpower
thesis at the *delegation* layer: don't rely on the sub-agent being right; make the dangerous action structurally
unavailable to the agent. (The super-agent ALSO independently falsified the claim — belt and suspenders.)

## Under-grounding → confabulation (the cross-model pattern)
The questions were about Claude Code internals the guide may not have grounded knowledge of. Rather than say "I
don't know," it produced a plausible-looking but wrong artifact — the **same under-grounded-model-confabulates**
pattern as the SM040 ChatGPT experiment (an external model confabulated while blind, grounded correctly once given
substrate). Mitigation applied on the retry: an ultra-scoped brief that (a) forbids touching settings, (b) forbids
investigating the machine, (c) explicitly instructs "if you cannot verify, write **unverified** — do not guess or
substitute a different topic." The anti-confabulation lever is an explicit **licensed-to-say-unknown** clause in the
brief.

## TE footnote
The drifted call cost **~51.7k tokens / 193 s / 14 tool-uses**, all on the wrong task — delegation is not free when
it drifts, a datapoint for SM045 (a sub-agent that wanders wastes its whole budget). Argues for tight,
lane-constraining briefs + cheap verifiability of sub-agent outputs. Ties: [[delegation-dance]], [[hardening-dance]],
[[guard-against-forced-confirmations]], SM040 (blind confabulation), [[echt-effort-especially-self-generated]].
