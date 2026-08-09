# Issue 020: three small polish items — `tt help` alias, `tt log` name collision, `tt find` missing from the guard-clean digest

> status: open · labels: toolbox, docs, discoverability, good-first-issue · summary: three independent
> paper-cuts from the alpha field test, each cheap: `tt help` exits 2, `tt log <dir>` fails in a way that
> invites a git misreach, and the `gs warm` digest teaches `tt files` while never mentioning `tt find`.

## Description

Three small things from the 2026-07-29 alpha field test. None blocks anything; each cost a wasted call or
a wrong first instinct. Filed together because they are all one-liners — split if preferred.

### 1. `tt help` is not a tool

```
$ tt help
tt: no such tool 'help'
usage: tt <tool> <args...>   (tools: ascii bloop box chrono doc env files find forge git gitinfo
guardcheck gvdot hangover harden htmltext json limit links log md-fmt memory mode newtool parsereqt
prd sbt scala serv session skillcheck skillgrants ssg statusline sub svg text tsv typo update verify
web which wr zip)
```

It degrades gracefully — the usage line carries the full 45-tool list, which is most of what a caller
wanted — but the exit code is 2 and the first line reads as an error. `tt help` is the obvious guess for a
newcomer, and `gs help tt` is the documented route only if you already know `gs`.

**Fix:** alias `tt help` (and maybe `tt --help`) to the usage line with exit 0.

### 2. `tt log <dir>` invites a git misreach

```
$ tt log <abs-repo>
log: not a readable file: <abs-repo>   (exit 2)
```

`tt log` is a build/run-log analyzer, but the name reads as git history, and the guard-clean digest lists
`tt gitinfo` under GIT while `tt log` appears nowhere — so the collision is never addressed where the
reflex is formed. An agent primed on git reaches for `tt log <repo>` first; this happened on the first
attempt in the field test.

The error is cheap and clear, so the cost is one wasted call, not a wrong answer.

**Fix:** add a hint to the error when the argument is a directory that looks like a repo, e.g.
`for git history see tt gitinfo <repo>`.

### 3. `tt find` is missing from the guard-clean digest

`tt doc guard-clean-digest` teaches, under SEARCH / FILES:

```
  tt files <DIR> <ext> [regex]              find files (dir first)
```

...and never mentions `tt find`. But `tt find` is the better-shaped tool for structure questions — it has
`--type f|d`, `--max-depth`, `--name` globs, `--all`, and documents that symlinks are not followed —
while `tt files` is the content-search tool (its distinctive power is the content regex, i.e. `grep -rl`).
An agent that loads the digest at cold start and nothing else will never discover `tt find`.

The two agreed exactly where they overlap in the field test (both reported 505 `.scala`), so this is
purely a discoverability gap, not a correctness one.

**Fix:** one added line in the digest's SEARCH / FILES block, e.g.

```
  tt find <root> --type d --max-depth N     find dirs / structure (globs, depth, types)
```

...and ideally a one-clause statement of the division of labour: `files` = content search,
`find` = structure search.

## Acceptance sketch

* `tt help` prints the usage line and exits 0.
* `tt log`'s not-a-file error points at `tt gitinfo` when the argument is a directory.
* The guard-clean digest names `tt find` alongside `tt files`, with the division of labour stated in a
  clause.

## Discussion

### Comment by hmiddelk/Opus5 at 2026-07-29 23:27

Filed from the same alpha field test as issues 014–019, on the "report anything that wedges, however
small" principle. Item 3 is the one with the most leverage per character: the digest exists precisely
because these reflexes regress across a compaction, so a tool absent from it is effectively invisible at
cold start.

Agent disclosure: found and drafted by an AI agent (Claude Opus 5) under human direction; the human
reviewed and submitted.

### Comment by hmiddelk/Opus5 at 2026-08-04 16:12

**All three items CONFIRMED on Windows 10.** Released `v0.10.0` native `windows-x86_64` build, Windows 10
Enterprise 10.0.19045.

**1. `tt help`** — reproduces, and `tt --help` fails the same way, so the parenthetical "and maybe
`tt --help`" in the fix is worth making definite:

```
> tt help        -> tt: no such tool 'help'     (exit 2)
> tt --help      -> tt: no such tool '--help'   (exit 2)
```

Both still carry the full 45-tool usage line, so it degrades gracefully here too.

**2. `tt log <dir>`** — reproduces, no `tt gitinfo` hint. `v0.10.0` has improved the message with a
resolved-path clause, which is genuinely useful, but the missing-hint point is untouched:

```
> tt log C:\Users\<user>\testgenscalator
log: not a readable file: C:\Users\<user>\testgenscalator (resolved: C:\Users\<user>\testgenscalator)
```

**3. `tt find` missing from the digest** — reproduces, counted rather than eyeballed:
`tt doc guard-clean-digest` mentions `tt files` **once** and `tt find` **zero** times. The SEARCH / FILES
block is verbatim as quoted above.

Worth adding to item 3's leverage argument: this Windows sweep re-tested `tt find` heavily (issue 014 is
entirely `tt find` behaviour), and the tool's absence from the digest means the cold-start reflex points
only at `tt files` — which, per issue 017, is also the one that cannot express "structure, excluding build
output". The digest line proposed above would fix discoverability for both.

A first-run observation that belongs with item 1 rather than in its own issue: the bootstrap installer's
closing instruction on Windows is literally `then run: tt help`. So on a clean install the first command
a newcomer is told to run is the one that exits 2 — the two paper-cuts compose into a bad first
impression. Noted also in issue 022's discussion.

Agent disclosure: the Windows re-test was run and written up by an AI agent (Claude Opus 5) under human
direction; the human reviewed and submitted.

### Comment by bjornregnell/Fable5 at 2026-08-07 15:56

Maintainer-side review (PR 2 triage): all three sub-items confirmed, and item 1 is WORSE on
today's Linux tree than the filed transcript. The bash launcher intercepts BEFORE the dispatcher:
`tt help` prints `no such tool 'help'` with NO tool list (`tools/tt:28`), and `tt --help` is
rejected as `invalid tool name` by the identifier regex (`tools/tt:27`). The graceful
usage-list degradation shown in the transcript is the dispatcher path (`dispatch.scala:88`),
reached only by direct-binary invocations (the Windows shape). The fix therefore needs BOTH code
paths: a `help|--help|-h` special case in the launcher AND in `dispatch()`. Installer footnote
verified: `get-genscalator.sc:234` closes with `then run: tt help`. Digest item verified: `tt
find` occurs zero times in `docs/guard-clean-digest.txt` (only inside the "never raw find"
prohibition); note the digest edit also partially serves issue-017's discoverability (files =
CONTENT search, find = STRUCTURE search) — one cross-referenced edit, not two.

Triage: POLISH, v0.10.2 — except the `tt help` item, which is the strongest pull-forward
candidate for v0.10.1: a fresh install's first instructed command currently exits 2.

Agent disclosure: this review comment was produced by an AI agent (Claude Fable 5) under human
direction; the human reviewed and submitted.

### Comment by bjornregnell/Fable5 at 2026-08-09 14:36

ALL THREE ITEMS SHIPPED (v0.10.2 wave). Item 1 in `58db2ea`: `help`/`--help`/`-h` print the
usage line with the full tool list and exit 0 on BOTH code paths the triage located — the bash
launcher (special case before the identifier regex, answered from bash with no JVM launch) and
the dispatcher (same usage the unknown-tool branch prints, exit 0; deliberately not an entries
verb, so the coverage test's table==files invariant holds); each path keeps deriving its own
list, no second hand-maintained copy. The installer's closing `then run: tt help` instruction is
therefore now valid with no installer change. Unknown tools still exit 2 — pinned, with the help
cases, in dispatch.test.scala; the launcher path was manually verified (help/--help exit 0 with
the list, nosuchtool exit 2). Item 2 in `19e4209` (rides in log.scala with issue-018): a
directory argument gets "for git history see: tt gitinfo" on stderr, exit 2 kept, absent for
nonexistent paths — tested. Item 3 in `58db2ea`: the guard-clean digest's SEARCH/FILES block
names `tt find` with the division of labour (files = CONTENT search, find = STRUCTURE search),
also discharging issue-017's discoverability cross-reference. Ready to close on the maintainer's
sweep.

Agent disclosure: this comment was produced by an AI agent (Claude Fable 5) under human direction;
the human reviewed and submitted.
