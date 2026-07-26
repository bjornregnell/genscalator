# 044 - Reconciling the HUMANS.md workflow (one pinboard, drop the inbox)

*Proposal, 2026-07-05 (BR pin). For BR to ratify. Supersedes the two-variant split recorded as OD5. Siblings:
`009-shared-file-editing-protocol.md` (the clobber problem that birthed the inbox), `010-task-autonomy-negotiation.md`,
`docs/foundations.md` (dances), the closed-repo `muntabot-synch-introprog/HUMANS.md`.*

## The inconsistency BR flagged
Two divergent HUMANS.md workflows exist:
- **Closed repo** (`muntabot-synch-introprog/HUMANS.md`): a **single** file, **agent-administered** pinboard. The
  session **feed is the inbox** (no separate inbox file); the agent auto-pins and keeps it current, BR reads + decides.
  Works well - BR handed over edit control and just consumes.
- **Open repo** (`genscalator/HUMANS.md` + `HUMANS.inbox.md`): a **file-partition** (Opt A from `009`). The agent
  appends proposals to the agent-owned, append-only `HUMANS.inbox.md`; the human harvests accepted items into the
  human-owned `HUMANS.md ## TODO`, then deletes them from the inbox. **Two** files.

BR's stated preference: **one** HUMANS.md that acts as a human-todo pinboard which the **agent helps administer**, and
**drop the inbox if possible** - noting the open repo already has **Forge issues** as a natural inbox, and the agent
can read them via `tt forge`.

## Why the inbox existed (so we don't lose the safety)
The inbox solved the **clobber problem** (`009`): if agent and human both edit one HUMANS.md, the agent's disk write
can silently overwrite the human's unsaved editor buffer. The file-partition gave the agent its own write channel so it
**never touches** the human-owned file. So any single-file design must preserve that safety by another means.

The real variable is **ownership**: closed = agent-owned pinboard (human reads); open = human-owned TODO (community
curates), which is why the open repo needed a separate agent proposal channel.

## Proposal - one agent-administered pinboard per repo; retire `HUMANS.inbox.md`
**1. One HUMANS.md, section-partitioned (not file-partitioned).** Replace the *file* partition with a *section*
partition inside the single file, with explicit ownership per section:
- `## NOW` / `## OPEN DECISIONS` / `## AFK MENU` - **agent-maintained** (agent edits freely, targeted edits).
- `## TODO` - **the human's pinboard.** The agent may **only APPEND** new `- [ ] (agent) ...` proposal lines at the
  bottom; it **never reorders, edits, or checks off** the human's existing items. (This is the inbox's job, done
  in-file by a tagged append.)
- `## DONE` / archive - human moves items here; agent may append dated completion notes it made.

**2. Drop `HUMANS.inbox.md`.** Its sole purpose - a safe agent->human proposal channel - is replaced by the tagged
`- [ ] (agent)` append to `## TODO`. One less file, one less harvest-and-delete step.

**3. Keep an inbox concept, but use the RIGHT inbox per context (this is the actual reconciliation):**
- **Closed repo** (private working repo, no issue tracker): the **session feed** is the inbox. Agent pins into
  HUMANS.md continuously. (Already how it works.)
- **Open repo** (community-facing): **Forge issues** are the inbox of record. Substantive agent proposals become
  **Forge issues** (needs a small `tt forge issue-create` capability - see Open questions); the agent reads open
  issues via `tt forge` to stay in sync; HUMANS.md stays a **lean, human-curated** pinboard that can *reference* issue
  numbers. Issues bring labels, assignment, and close-state that a flat inbox file lacks.

So: **one HUMANS.md everywhere (agent-administered, section-owned), the separate `HUMANS.inbox.md` retired, and the
"inbox" realized by the medium that fits each repo** - feed for closed, Forge issues for open.

## How the clobber safety is preserved (without the inbox)
- Agent uses **targeted section edits**, **never `git add -A`** (stage only agent-authored paths), and **only appends**
  to `## TODO` - it never rewrites the human's lines. (Same discipline the closed repo already runs safely; memory
  `no-clobber-human-owned-files`.)
- If the human is **actively editing** HUMANS.md, the agent coordinates (append-only to its own sections, or holds on
  an explicit "hands off"). The concurrent-unsaved-buffer edge is the residual risk `009` named; it is rare for a
  read-mostly pinboard and is the price of dropping the file partition. Modern editors' disk-vs-memory diff (BR's Opt-C
  observation in `009`) usually surfaces the conflict rather than losing data.

## Migration (if ratified)
1. Add a `## TODO` section to the open `genscalator/HUMANS.md`; fold the current `HUMANS.inbox.md` items in (they were
   already pre-digested with harvest actions in the closed pinboard).
2. File the still-open inbox items that deserve tracking as **Forge issues**; delete `HUMANS.inbox.md`.
3. Document the section-ownership convention at the top of each HUMANS.md (who-edits-what).
4. Update `009`/foundations to record: file-partition retired in favor of section-partition + per-repo inbox medium.

## Open questions for BR
- **`tt forge issue-create`?** Forge currently does whoami/releases/tags (read) + release-create. Filing agent
  proposals as issues needs an issue-create verb (same token-from-env + trusted-host guards as release-create). Build
  it, or keep agent proposals as `- [ ] (agent)` TODO appends only and let BR file issues manually?
- **Pure human TODO vs a thin agent-proposals sub-section?** Append `(agent)` items directly into `## TODO`, or keep a
  `### Agent proposals (harvest up)` sub-section so the human's hand-curated list stays visually separate?
- **Concurrent-edit stance:** is append-only-to-own-sections + no-`git add -A` enough for you to trust the agent in the
  single file, or do you want a lightweight lock (e.g. a `HUMANS.md` "agent: hands off" marker line you can set)?
