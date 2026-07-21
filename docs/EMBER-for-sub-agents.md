# EMBER for sub-agents (SM186)

> **What this is.** The standard warming block for every delegated sub-agent brief in genscalator
> land. A sub-agent inherits NONE of the caller's skills, memories, or reflexes — only its brief
> ([[warm-delegated-subagents-lack-caller-skills]], [[delegation-dance]]) — so the guard-clean
> reflexes must ride IN the brief, VERBATIM. Paraphrase loses the sibling rules: three documented
> specimens (wr-data `subagents-need-the-ember-s0-checklist`), plus the 2026-07-21 A/B run
> (wr-data `sm186-subagent-ember-ab`). Print this file with `tt doc EMBER-for-sub-agents`.

## How to assemble a brief (the super-agent's checklist)

1. **Paste the guard-clean digest VERBATIM** — run `tt doc guard-clean-digest` and paste its whole
   output between labelled fences. That digest is the ONE canonical home of the core reflexes
   (search/files, shell hygiene, git); this file deliberately does NOT restate it — converge,
   don't fork.
2. **Paste the delta rules below VERBATIM** (they cover what the digest doesn't, and what only
   matters for a delegated agent).
3. **Add the task payload**: goal, inputs as ABSOLUTE paths, the EXACT output contract, and the
   tool-lane (what the agent may Read/run/Write — default read-only, write-paths enumerated).
4. Wrap every pasted block in PROMINENT labelled fences at BOTH ends ([[copy-paste-frame-rule]]).

## The delta rules (paste verbatim into briefs)

=== SUB-AGENT DELTA RULES — BEGIN ===
- ABSOLUTE paths always — your cwd is not guaranteed to be the repo you work on.
- `tt files` respects .gitignore: ignored files are INVISIBLE to it; stat/Read by absolute path instead.
- URLs: `tt web get <url>` — never raw curl/wget.
- NO process ops (ps/pkill/kill) ever — if a task seems to need one, STOP and report instead.
- Timestamps: read `tt chrono now` per stamp, never estimate.
- Write ONLY the paths your brief enumerates; touch nothing else — no memory, no settings, no
  mode changes, no commits unless the brief grants them explicitly.
- If a command errors or is denied, do NOT retry cosmetic variants — note it and switch to the
  tt shape.
- Report in ENGLISH; enumerate what you checked (never "all"/"every" unless you verified each).
- Your final text IS the return value: raw data per the output contract, no pleasantries.
=== SUB-AGENT DELTA RULES — END ===

## Why verbatim beats paraphrase (the evidence, dated)

- Three paraphrase-gap specimens where a summarized brief lost sibling rules and the sub-agent
  regressed to raw shell (wr-data `subagents-need-the-ember-s0-checklist`, 2026-07-20/21).
- The PR-sandbox review agent: 3 guard stalls in one evening on an under-warmed brief.
- One bloop restart without a typed shape: 6 guard events, 2 silent pkill failures
  (wr-data `tt-box-lacks-local-health-shape`, 2026-07-21) — the cost of every un-warmed reflex
  lands on a present human as a stall.
- A/B run 2026-07-21 (verbatim-assembly arm vs polite-paraphrase arm, identical read-only task):
  see wr-data `sm186-subagent-ember-ab` for command logs and the verdict.

## Scope notes

- This ember is for DELEGATED WORKERS (fresh context, one task, report back). The main-session
  cold-start ember (the warp baton) is a different artifact: it carries session state, holds, and
  a menu; this one carries reflexes + a task contract only.
- Long-lived observer agents with their own protocol (e.g. the meta-minion) keep their protocol
  file as the brief body but STILL get §1+§2 pasted at the top — the meta-minion brief predates
  this file and already demonstrates the pattern.
