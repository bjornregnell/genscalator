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
