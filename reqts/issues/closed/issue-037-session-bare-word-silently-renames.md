# Issue 037: `tt session list` silently RENAMES the session, because any bare word is taken as the new name

> status: closed 2026-08-16, fixed by `7add972` · labels: toolbox, session, agent-trust, safety · summary: `tt session` has no read
> subcommand, and `session.scala:172` treats any bare non-flag word as a name to set. So the obvious
> way to ask "what sessions are there" performs a write instead. Observed live 2026-08-15.

## Description

Found 2026-08-15 by a read-only audit agent, which was explicitly briefed to write nothing. It ran
`tt session list` to check a roster and **silently renamed the live session** from
`260813-15h38m-Golf` to `260813-15h38m-list`. It noticed, restored the name with `tt session Golf`, and
disclosed the misfire at the top of its report. A less careful caller, or one that never re-read the
name, would not have noticed at all.

The dispatch at `session.scala:152-185` special-cases exactly three shapes: `--clear` (`:157`), `adopt`
(`:165-167`), and `--help` (`:131`). Everything else falls to `:172`:

```scala
case words if words.nonEmpty && !words.exists(_.startsWith("--")) =>
```

Any bare word or words become the new name. `list`, `show`, `status`, `ls`, `get`, `name` and `current`
are therefore all setters, and every one of them is a plausible first guess for a read.

**Why this is more than a papercut.** Three properties compound:

* **It is silent and it is a write.** The rest of the toolbox is built so that effectful shapes announce
  themselves and previews come first (`tt forge release-delete` previews by default, `tt sub` previews
  before writing). This one writes on what reads like a query, with no confirmation and no audit line
  distinguishing "you set a name" from "here is your name".
* **The damage is invisible in the same call.** The tool prints the new name, which looks exactly like a
  successful read. `260813-15h38m-list` is indistinguishable at a glance from a legitimate session name.
* **It corrupts the thing sessions are for.** Per-session mode chips and state are keyed by session, and
  issue 023 (closed in v0.10.2) was specifically about state being orphaned when the key changes. Renaming
  by accident is another way to reach that same broken state, from the other direction.

Adjacent, and part of the same gap: **there is no roster at all.** `tt session` prints only the current
name, and there is no `tt sessionstore list`. The audit agent was trying to verify a list of used session
names that exists in exactly one place, a prose line in a handover document. That is what sent it looking
for a read verb in the first place.

## How to reproduce it

⚠ **This reproduction renames your live session.** Do it in a throwaway session, or be ready to restore
the name.

```
$ tt session                 # prints the current name, a genuine read
$ tt session list            # NO ERROR: silently sets the name to "<stamp>-list"
$ tt session <realname>      # restore
```

## Acceptance sketch

* **A read subcommand, and reserve the obvious synonyms.** `tt session list` (and `show` / `status` /
  `current` / `ls` / `get`) must never set a name. Either make them read verbs or refuse them with a
  message naming the setter form, in the spirit of the case-insensitive `adopt` reservation already at
  `:165` — that precedent exists precisely because a recovery-flow typo must not do the wrong thing.
* **Make the setter explicit, or make it announce itself.** Options, maintainer's choice: require
  `tt session --set <name>` or `tt session name <name>`, keeping the bare form as a deprecated alias; or
  keep the bare form and have it print an unmistakable audit line, e.g.
  `session: renamed <old> -> <new>`, so a write can never be mistaken for a read. The second is cheaper
  and preserves every existing call site.
* **A roster verb**, which is the underlying need: list known sessions for this directory with their
  stamps and last-seen times, the same data `adopt` already enumerates when it offers candidates
  (`:117-122`). `adopt` can already list; nothing else can.
* **Reject names that look like verbs** as a backstop, so a future subcommand cannot be shadowed by a
  legitimate-looking name.
* Tests for each shape in `tools/test/session-cli.test.scala`, asserting that a read verb leaves the
  stored name unchanged. The current suite has 13 tests and none of them covers "a word that should not
  have been a name".

## Discussion

### Comment by bjornregnell/Opus5 at 2026-08-15 20:5x

Filed at session exit, from the exit-side ember audit. Confirmed from the source rather than reproduced,
deliberately: reproducing it would have renamed the filing session, which is the whole complaint.

Worth recording as a small specimen in its own right: **a read-only brief was broken by a tool whose read
shape does not exist.** The agent did nothing careless. It reached for the most natural query form, and
the tool's argument grammar turned it into a write. This is the same family as issue 031 (`tt files`
returning `0 files` and exit 0 for a missing root) — the toolbox answering a question the caller did not
ask, plausibly enough that nothing looks wrong.

Triage: the audit-line half is the cheap, high-value fix and could ship on its own. The roster verb is the
real feature and is worth pairing with issue 023's `adopt` work, since `adopt` already computes the list.

Agent disclosure: found by an AI agent (Claude Opus 5) acting as an exit-side auditor, verified from source
and drafted by an AI agent in session with the maintainer, who reviewed it.
