# HUMANS.inbox.md - agent->human proposal inbox

**Agent-owned, append-only.** This is the agent's write channel to the human, the structural half of the
HUMANS.md / AGENTS.md collaboration protocol (Opt A: file-level partition, so the agent never touches the
human-owned HUMANS.md and can't clobber a live editor buffer).

**How it works**
- The **agent** appends new review-worthy items / proposals at the bottom (never edits or reorders existing
  items, never writes HUMANS.md).
- The **human** harvests: move an item into HUMANS.md `## TODO` (tagging `HD:`/`TAP:` as needed), then delete
  it from here. When this file is empty, you have harvested everything.
- Each item: a `- [ ]` line so it pastes straight into the TODO. Keep entries short; detail lives in the
  referenced file / CHANGELOG.

---

## Inbox (harvest into HUMANS.md TODO, then delete)

- [ ] `research/task-autonomy-negotiation.md` - NEW note (your ralph-loop vs collaborative-ballgame idea).
      Verifiability is the deciding triage signal; agent proposes the mode, human confirms. Candidate to
      graduate into an AGENTS.md "triage each task's mode" guideline + a HUMANS.md handoff convention. Skim + steer.

- [ ] `research/shared-file-editing-protocol.md` - NEW note (the non-destructive editing problem we just hit).
      Captures Opt A/B/C trade-offs, the whole-file-buffer insight, and your point that modern editors'
      disk-vs-memory diff may make Opt C lowest-churn. Open question flagged for future research. Skim + steer.

- [ ] FYI (no action): `research/instructions-for-claude.md` example updated with the `No em-dashes` standing
      style line (committed 711217e) for you to copy back to the web-GUI instructions.
