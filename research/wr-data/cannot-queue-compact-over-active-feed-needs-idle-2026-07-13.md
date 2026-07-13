# WR data: you cannot queue a /compact over an active feed — it fires only when the feed is fully idle (2026-07-13)

BR: *"it seems as i cant que up a slash-compact but the feed needs to be completely idle before it goes
compacting."* A harness finding, discovered live while trying to compact-before-eating.

## The finding
A manual `/compact` does **not** fire while the feed is active (the agent working, or messages pending). It
**waits until the feed is COMPLETELY IDLE** — both human and agent idle, no pending turn — and only then runs
the compaction.

## It explains the session's compact-confusion
BR planned "compact, then eat," but the compact kept deferring ("*not sure why I dont got to compact before*").
Cause: the feed was never idle — BR kept messaging and the agent kept working the hot-harvest, so the queued
compact never got its idle moment.

## The bind it creates (a genuine tension)
- To get the fresh window you must let the feed go **idle**.
- But *"continue safe-solo while I eat"* keeps the feed **active** (the agent working), which **blocks** the compact.
- And post-compact the agent **sleeps** until the human types (compact-sleep), so idling-to-compact means **no
  work happens while the human is away** either.
- So **"keep working while away" and "compact for a fresh window" are mutually exclusive in the moment** — you
  cannot have both simultaneously. The human must choose: keep the agent working (feed active, no compact, and at
  high fill that means working in the dumb-zone), OR go fully idle (compact fires, fresh window, but no work
  until the human returns and wakes it).

## Ties
The mutual-idle requirement is a hard, harness-enforced instance of the **joint clock**: mutual idle is the only
sync point, and here the harness *requires* it for the compact to fire ([[joint-clock-two-party-progress-stalls-2026-07-13]]).
Also the compact-sleep / bing-bing thread, agent-cannot-see-compaction-finish, and the queue-overflow finding.
NB same turn: the **`dumb-zone` flag fired LIVE at ctx-fill 76%** — first real sighting; the ctx-fill ladder
validated (see `tmp/compact-chrono-stamps.md`).
