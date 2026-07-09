# SM013 - session-log-to-disk on/off tool: investigation + recommendation

**Status:** (a) DONE 2026-07-08 (see `wr-data/session-logging-existing-capture-2026-07-08.md`);
(b)/(c) reported here. Investigation only - **recommendation: build no dedicated
logger tool.**

## Recap of (a) - what already exists
Raw capture is solved end to end: the harness auto-writes a private per-session
`~/.claude/projects/<slug>/<uuid>.jsonl` (every user/assistant turn + tool
calls/results), `cleanupPeriodDays` (default 30) controls retention,
`--no-session-persistence` disables writes, and a first-cut `tt transcript` reads
it back. The jsonl is METHODOLOGY.md's primary data source. The only structural
gap: the jsonl **omits** harness-injected content - `system-reminder` blocks,
recalled memories, and (critically) **guard reasons/outcomes** - which no
`tt`-side logger can recover, because `tt` cannot see bytes the harness never wrote
to disk.

## (b) What a "toggle" would actually add - three readings
1. **Control the existing transcript (on/off).** This already IS
   `--no-session-persistence` + `cleanupPeriodDays`. A toggle tool would only wrap
   a **settings change** - and whether the session record persists is an
   **audit-trail / authority-anchor** decision the *human* owns. An agent that can
   switch its own logging off is a hole in the record it is the subject of
   (RT047). So this reading is deliberately NOT a `tt` tool.
2. **A supplementary human-readable log** (chat + decisions + WR flags the agent
   curates). This is exactly the existing manual RAW-DATA / `wr-data/` workflow. A
   tool/hook could auto-append it, but that is a **convenience over the jsonl**,
   not new capture, and it duplicates a discipline that already works.
3. **Export / snapshot-now** of the session to a durable file on demand. Already
   ~90% covered by the existing `tt transcript <id | --recent N>` reader; the
   missing 10% is just writing its output to a chosen file.

## (c) Recommendation - tt-tool vs settings/hook
- **Raw logging: neither tool nor toggle.** Do not rebuild the jsonl. The on/off
  and retention live in **settings (human-owned)**; the disposition is to
  *document* the flags (done in 013a), not to expose an agent-drivable switch.
- **Omitted content (guard reasons + injections): folds into SM016.** This is the
  only genuinely-missing data, and it needs the **tap** (a layer that reads the
  injected stream), not a `tt` logger. Tracked under SM016, not here.
- **Convenience auto-curator: DEFER (low value).** A hook that appends chat +
  decisions to a running log is possible but adds moving parts (hook lifecycle,
  human approval, dedup vs the manual workflow) for marginal gain over the
  discipline we already practice. Revisit only if the manual workflow starts
  leaking under load (the O6/O12 "log everything" regression in
  `substrate-regression-candidates`).
- **Export extension: cheap, opportunistic.** If a durable one-file snapshot is
  ever wanted, add `tt transcript --export <file>` to the existing reader rather
  than a new tool. Not urgent.

## Disposition
SM013 investigated + closed as **no dedicated tool**: the raw-capture half is a
built-in + human-owned settings; the missing-content half is SM016 (the tap); the
convenience/export halves are a deferred optional hook + a trivial `tt transcript`
flag. Any settings change (persistence/retention) stays BR-approved.
