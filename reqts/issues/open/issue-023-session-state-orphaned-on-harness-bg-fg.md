# Issue 023: per-session mode/session state is orphaned when the harness re-mints the session id (bg/fg round trip)

> status: open · labels: toolbox, session, mode · summary: backgrounding a Claude Code session
> (accidental left-arrow) and returning to it made the harness mint a NEW session id; `tt mode`
> and `tt session` then correctly read the new key and found nothing — all chips and the session
> name appeared cleared, while the real state sat orphaned under the old key. Recovery today is
> manual re-declaration; there is no adoption path and no hint that an orphan exists.

## Description

Found 2026-08-07 in live use, the same day the feature's first field session ran. The v0.10.0
per-session state (mode chips + session name) is keyed on working directory + harness session id —
deliberately, so concurrent sessions in different terminals stop clobbering each other's chips
(the defect this design fixed). The key was chosen as unique and stable. Field observation: it is
unique but NOT stable — a background/foreground round trip in the Claude Code TUI (left arrow to
background, enter to return) handed the continuing conversation a new session id.

Consequence: every `tt mode` / `tt session` read after the return is scoped to the new id and
finds empty state. To the human the mode line silently loses all chips and the session name; to
an agent reading the mode line to calibrate autonomy, a vanished chip is a wrong signal, not a
missing one (a lost `afk` chip misleads in the unsafe direction). The old state is not deleted —
it sits orphaned under the previous key — but nothing surfaces that fact.

Observed timeline (one machine, Linux, v0.10.0-era checkout): chips declared 13:13-15:56 and the
session name were all gone after the ~15:57 bg/fg; chips declared after the return survived; the
freshly minted default name carried the timestamp of the first post-return read (15h59m). That
chip-survival split is the diagnostic signature of a key change, reproducible in minutes.

## How to reproduce it

1. In a Claude Code session, declare state: `tt session MyName`, `tt mode add SomeChip`.
2. Background the session from the TUI (left arrow), then return to it (enter).
3. Run `tt mode` and `tt session`: both read empty/default; the pre-background state is orphaned
   under the old session id's key. (Step 2's id re-mint is harness behavior and may change across
   Claude Code versions; the defect claimed here is that the toolbox has NO recovery path when
   any id change happens, whatever its cause.)

## Acceptance sketch

- On an empty-state read (`tt mode`, `tt session`, and the statusline path) where the SAME
  directory holds recently-touched orphaned state, print ONE hint line naming what was found
  (name, chips, age) and the adopt command — never silent emptiness when an orphan exists.
- An explicit `tt session adopt` verb re-attaches the newest same-directory orphan (name + chips)
  to the current key; with several candidates it lists them and requires a choice. Adoption stays
  human-triggered: two live sessions in one directory is exactly the case where silent adoption
  would attach the wrong session's chips.
- Auto-adopt-when-unambiguous may come later as an opt-in refinement; it is not this issue.
- Candidate selection (same dir, newest, age cap) factored as a pure function with unit tests, so
  the semantics are testable without a harness.
- Re-keying the state on anything other than the harness id is NOT the fix — it resurrects the
  cross-session clobber defect the current design fixed.

## Discussion

### Comment by bjornregnell/Fable5 at 2026-08-07 16:14

Diagnosis note from the finding session: the orphan was confirmed by the chip-survival timeline
alone, no store inspection needed — everything declared before the bg/fg lived under the old key,
everything after under the new, and the default name's timestamp pinned the switch moment. Worth
keeping as the reproduction check because it needs no knowledge of the store layout. The harness
side (a continuing conversation re-minting its session id) may merit an upstream report
separately; this issue deliberately claims only the toolbox's missing recovery path.

Agent disclosure: this issue text and comment were produced by an AI agent (Claude Fable 5) under
human direction from a live incident report by the human; the human reviewed and submitted.

### Comment by bjornregnell/Fable5 at 2026-08-09 14:36

SHIPPED in `456f038` (v0.10.2 wave). The store now stamps a `cwd` file per entry (writers only) —
the ground truth turned out to be that the store keyed on session id ALONE, so same-directory
matching had nothing to match on until this. `tt session adopt`: one same-directory orphan =
adopted (chips unioned so post-re-mint declarations survive, the earlier `started` stamp adopted
so the age signal is restored, the name transferring only onto an unnamed key); SEVERAL candidates
= nothing adopted — one may be another LIVE session in this directory — they are listed newest
first and the user picks with `adopt <id>`, exit 2 until then; a lone word spelled adopt in any
capitalization is the verb, never a session name. Empty-state reads of `tt mode`/`tt session`
print ONE stderr hint when a recent (48h) same-directory orphan exists; stdout stays
byte-identical. Auto-adopt stayed OUT, as decided at filing. The orphan logic lives once, in
sessionstore.scala, shared by both tools.

Two recorded limitations: (a) orphans written before the cwd stamp existed — including this
issue's own field incident — are invisible to adopt; strict matching beats attaching another
project's chips. (b) The acceptance sketch's statusline surface deliberately got NOTHING: the
statusline's stdout is parsed and it has no stderr channel a human reads, so the hint lives on
the mode/session reads and the verb is the recovery path — recorded here as the scope decision.
Ready to close on the maintainer's sweep; the upstream harness report remains a separate thread.

Agent disclosure: this comment was produced by an AI agent (Claude Fable 5) under human direction;
the human reviewed and submitted.
