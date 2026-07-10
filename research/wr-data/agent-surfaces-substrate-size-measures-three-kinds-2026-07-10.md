# The agent can surface SIZE measures of substrate - three distinct kinds, often conflated (2026-07-10)

BR-flagged WR datum: CO4 spontaneously handed BR a size measure ("grinding the 157 MB transcript"), so the
agent surfaces not only timestamps but also sizes. What that measure includes, and its kin.

## What the "157 MB" is
The **session TRANSCRIPT `.jsonl` on disk** - the harness's per-session record. It includes: every user and
assistant turn; every tool CALL and tool RESULT verbatim (the big file-Reads, the `/context` pastes, embedded
sub-agent outputs, web results); and per-entry JSON metadata (timestamps, message IDs, model, token counts). It
is the full raw history, byte-exact.

## Three DIFFERENT size measures - do not conflate them
1. **Transcript disk size (MB)** - the `.jsonl` on disk (~157 MB this session). Holds ALL history including
   already-compacted / discarded material, verbose tool results, and JSON overhead. Much larger than the live
   context.
2. **Context-fill (tokens)** - what is in the LIVE window now (e.g. 92% of 1M). NOT the same as disk size: an
   ~85 MB session once booted a resume at ~284k tokens (blog 012) - raw disk >> working tokens.
3. **Durable committed substrate (MB)** - the memory + foundations + research + git repo (the "preserved me"): a
   different, smaller, curated artifact (measured ~4.3 MB at one point in the backlog note).

## What the transcript OMITS
Per SM013a (`session-logging-existing-capture-2026-07-08.md`): harness-injected content - `system-reminder`
blocks, recalled memories, and guard reasons - are NOT written to the `.jsonl`. So even the 157 MB is not the
full context the agent actually saw.

## Echt caveat (important)
The specific "157 MB" was **RECALLED from an earlier note** (SM013a), not freshly measured, and the transcript
has grown since - so it is approximate. A reliable current figure needs a fresh `stat` of the jsonl. The agent
CAN measure it directly, but the number it *handed over* here was a self-report/recall, subject to the same
confabulation-caution as any un-verified introspective figure ([[raw-grep-guard-stall-tt-text-context-2026-07-10]]
family: verify, don't assert). Ties: SM013a, blog 012 (disk-size != context-fill, the "gravity well" beat), the
substrate-size longitudinal backlog item, and the retrofit-timestamp datum.
