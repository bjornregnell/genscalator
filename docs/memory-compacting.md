# Compacting the agent memory index (MEMORY.md) — the safe procedure (SM068)

The agent's memory **index** (`MEMORY.md`, one line per memory) has a harness **read limit** (~24.4KB); a
PostToolUse hook nudges compaction as it approaches. This is the **agent-authored** compaction (a Read + Write
the agent controls and can inspect) — NOT the harness `/compact` of the transcript (which the agent does not
control and is paused during). Because the agent is the AUTHOR, **the outcome is method-contingent**: a careful
method loses ~nothing; a careless one loses recall. This doc is the careful method.

## When to run it
- The MEMORY.md-size hook asks (approaching the read limit), or
- Proactively when the index has grown and headroom is low.

## The algorithm (agent-invented, transparent, safe)
```
read MEMORY.md (the index of one-line hooks)
for each entry:
    keep the [name](file.md) link EXACTLY          # NEVER drop a pointer
    rewrite the hook -> the shortest phrase that still SIGNALS the memory's topic
        # cut redundant words / examples / parentheticals; keep the trigger + the rule
drop or merge an entry ONLY if its topic file is genuinely dead
write MEMORY.md back
invariant: total size under the read limit, WITH HEADROOM; topic-file CONTENT untouched
```

## Why it is safe (what is and isn't at risk)
- **Index vs content.** MEMORY.md is only an INDEX; the memory CONTENT lives in the per-topic files, which the
  compaction does **not** touch. Shortening a hook loses **no content** — the hook is a summary; the file is the
  source of truth (a lost/mangled index line can be regenerated from its file).
- **The real risks, ranked, and their mitigations:**
  1. **Dropping an entry** → orphans its topic file so recall never surfaces it = effectively lost. → **Keep
     every entry.**
  2. **Over-tightening a hook** → the recall system matches on the hook; a hook that no longer signals its topic
     stops being RECALLED when it should (a discoverability loss). → Keep each hook specific enough to trigger.
  3. **Transcription slips** in the full rewrite. → Care + the topic files as the durable backstop.
- **The counter-risk that JUSTIFIES compacting:** if MEMORY.md exceeds the read limit, the WHOLE index becomes
  unreadable → the recall system sees no hooks → **total recall failure**. So compaction trades a small
  recall-precision risk for avoiding a total-index-read failure. Not compacting is the bigger risk.

## Empirical angle (SM068, if we study it)
- **Efficiency** = bytes saved per pass (target: well under the read limit, with headroom for growth).
- **Effectiveness** = recall precision PRESERVED — do the shortened hooks still surface the right memories? (A
  before/after recall-probe set would measure this.)

Source + risk analysis: `research/wr-data/memory-index-compaction-read-limit-and-loss-risk-2026-07-13.md`.
Ties: SM068, the substrate-over-mechanism thesis, SM069 (skill vs doc — this stays a doc for now).
