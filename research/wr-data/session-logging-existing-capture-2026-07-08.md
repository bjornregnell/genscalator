# SM013a: what session-logging ALREADY exists (map before building)

The quick-win half of SM013: before proposing a session-log tool/toggle, map what the harness + our tooling
already capture, so we do not rebuild it. Finding in one line: **the raw record is already solved end to end;
the only real gaps are what the transcript structurally OMITS, which a new logger cannot fix anyway.**

## What already exists

| Capability | Where / how | State |
|---|---|---|
| **Per-session transcript** | The harness auto-writes every turn (user + assistant + tool calls/results) to `~/.claude/projects/<project-slug>/<session-uuid>.jsonl`, one file per session, mode `0600` (private). Can be large (this session's is ~157 MB). | Built-in, ON by default |
| **Primary data source** | `research/METHODOLOGY.md` treats the `.jsonl` as the primary objective record; `RAW-DATA.md` excerpts are mined verbatim from it; turn indices `#N` are the jsonl's user+assistant ordering. | Documented, in use |
| **Retention control** | `cleanupPeriodDays` (default 30) auto-deletes old transcripts; `--no-session-persistence` disables transcript writes entirely. | Settings, human-owned |
| **Read / extract tool** | A first-cut **`tt transcript <session-id | --recent N>`** reader exists (per `wr-data/harness-ux.md`, 2026-07-04) — reads/extracts from the jsonl so the agent does not hand-roll a path-extraction blob. | Built (first cut) |
| **Sidecar artifacts** | A per-session `<uuid>-toc.txt` (table of contents) and a per-session subdir sit alongside the jsonl. | Present, harness-produced |
| **Transcript miner (counts)** | A tool-choice / token-velocity miner over the jsonl is proposed in METHODOLOGY (WR-TOOL) and partly prototyped (subagent-transcript miner found 27 items in one run). | Partly built / proposed |

## What the `.jsonl` structurally OMITS (the real gap)
From the wr-data corpus, the transcript is **not** a complete record of context:
- **Harness-injected content is excluded** — `system-reminder` blocks, recalled memories, and (critically)
  **guard reasons/outcomes are NOT in the jsonl** (`introprog-autotranslate.md` explicitly asks the harness to
  log guard reasons so the friction corpus is minable). So true context load > logged content.
- **Flush-lag** — the latest live turn lags the jsonl flush, so the most recent turn is not yet readable
  (the reason self-reflection happens in chat first, then gets mined later).

## Implication for SM013 / SM016 (the recommendation preview)
- **Do NOT build a new raw session logger** — the jsonl + `cleanupPeriodDays` + `tt transcript` already cover
  capture, retention, and read-back. That would be reinventing a built-in.
- The residual value splits cleanly:
  1. **Auto-curation** (a human-readable running log of chat + decisions + WR flags): this is the RAW-DATA/wr-data
     manual workflow; a tool could auto-capture it (flagged as O6/O12 in `substrate-regression-candidates`), but
     it is a convenience over the jsonl, not new capture.
  2. **Capturing what the jsonl misses** (harness injections + guard reasons): this needs the **SM016 tap**
     (a layer that can read the injected stream), NOT a `tt`-side logger — `tt` cannot see content the harness
     never wrote to disk.
- So SM013's "build-a-log-tool" half **folds into SM016** (for the omitted content) and into a small optional
  auto-curator (for convenience). Its independent quick-win (this map + documenting the flags) is done here.

## Disposition
Closes SM013a. Feeds SM013 (recommendation), SM016 (the tap sees what the jsonl omits), and the transcript-miner
line in METHODOLOGY. No settings change proposed (retention flags are human-owned).
