# Issue 064: the session chip shows `dir: ?` until a writer happens to run, because `dir` has no fallback while `started` beside it has one

> status: open 2026-09-23 · labels: statusline, session, agent-ergonomics, asymmetry · measured
> against: v0.10.2 (from `VERSION.txt`) at `b410903`, Scala 3.9.0, Linux · summary: the `cwd` stamp is
> written by WRITERS only (`tt mode add|rm` at `mode.scala:122`, `tt session` at `session.scala:154`
> and `:256`), and statusline is read-mostly by contract and never stamps. `statusline.scala:720` reads
> `dirLabel(readCwd(...)).getOrElse("?")` with **no fallback**, so in a session that has only run
> read-only commands the chip has no directory. Eleven lines above, `started` **does** fall back, via
> the stdin JSON's `transcript_path` creation time (`:709-716`). The harness sends the directory in
> that same JSON — confirmed from a captured payload: top-level `cwd` and `workspace.current_dir`.
> So the fix is a fallback chain on a value the tool already has in scope.

## Description

The mode line's session chip renders `dir: <basename>  started: <stamp>`. Both halves are derived
from the session store, but only one of them survives the store being empty.

**`started` has a three-step chain** (`statusline.scala:709-716`):

```scala
val started = SessionStore.readStarted(sessionsRoot, id)
  .orElse:
    try MiniJson.parse(json).flatMap(_.obj).flatMap(_.get("transcript_path")).flatMap(_.str)
      .map(java.nio.file.Path.of(_)).filter(java.nio.file.Files.isRegularFile(_))
      .map(p => java.nio.file.Files.readAttributes(p, classOf[…BasicFileAttributes])
        .creationTime().toMillis)
    catch case _: Throwable => None
  .getOrElse(nowMs)
```

store → stdin JSON → the clock. It always produces something.

**`dir` has one step** (`statusline.scala:720`):

```scala
SessionStore.dirLabel(SessionStore.readCwd(sessionsRoot, id)).getOrElse("?")
```

store → `?`.

### Why the store is often empty

`SessionStore.ensureCwd` says so in its own docstring (`sessionstore.scala:213-215`):

> Stamp the working directory once, like `started` — **writers only**. Without it a later session
> cannot tell whether an entry belongs to ITS directory when hunting orphans after an id re-mint.

Its only callers are writers: `mode.scala:122` (`tt mode add|rm`) and `session.scala:154`, `:256`.
Statusline never calls it, correctly. So a session that has run only read-only verbs has no stamp,
and the chip reads `dir: ?` for as long as that lasts. Nothing is broken; the chip simply cannot
answer a question the harness answered on stdin.

**The directory IS in the stdin JSON.** Verified against a payload captured by the repo's own
documented mechanism (`docs/statusline-manual.md:147-148`: touch `~/.claude/gs-statusline-dump-on`
and the next render tees raw stdin to `~/.claude/gs-statusline-last.json`):

| key | value in the captured payload |
| --- | --- |
| `cwd` | `…/muntabot-synch-introprog` |
| `workspace.current_dir` | `…/muntabot-synch-introprog` (identical) |

`workspace` also carries `project_dir`, `repo` and `added_dirs`.

**No tool reads any of them today.** A search for `current_dir|workspace` across `tools/*.scala`
returns **zero** hits, so this would be the first consumer of that part of the payload.

### Why it is worth a number rather than a quiet patch

**It is an asymmetry between two values rendered by the same expression, three lines apart**, where
one was given a fallback and the other was not. That is the shape issues 041 and 055 are about, on a
different axis: not drift between carriers, but an inconsistent completeness rule between two fields
of one record.

**It misleads exactly at cold start.** `dir: ?` appears when a session has done nothing but read,
which is the beginning of most sessions, and the chip is the surface an agent uses to confirm which
tree it is working in. Answering `?` when the harness stated the directory on stdin is a
self-inflicted blind spot.

## How to reproduce it

```bash
# 1. the dir has no fallback; the stamp is the only source
tt text match tools/statusline.scala 'readStarted|readCwd|dirLabel'
#    => :709  val started = SessionStore.readStarted(sessionsRoot, id)      <- has .orElse below
#    => :720  SessionStore.dirLabel(SessionStore.readCwd(...)).getOrElse("?")  <- no .orElse

# 2. only writers stamp it
tt text grepr <abs>/tools .scala 'ensureCwd'
#    => mode.scala:122, session.scala:154, session.scala:256, and the definition

# 3. the harness does send the directory
tt json keys ~/.claude/gs-statusline-last.json
#    => … cwd … workspace …
tt json keys ~/.claude/gs-statusline-last.json workspace
#    => added_dirs, current_dir, project_dir, repo

# 4. nothing reads it yet
tt text grepr <abs>/tools .scala 'current_dir|workspace'
#    => (no matches)
```

Measured 2026-09-23 on Linux at `b410903`. Steps 1-4 were run and their output is quoted.

⚠ **Two limits on the evidence, both worth stating.**

**The `dir: ?` symptom was not reproduced live in the measuring session.** That session ran
`tt mode add` as its first action, which is a writer, so the stamp existed from then on and the chip
read `dir: genscalator-work`. That is corroboration of the mechanism rather than a sighting of the
defect: the stamp appeared exactly when a writer ran. The original report of `dir: ?` came from a
different session that had run only read-only commands.

**The captured payload is dated 2026-07-24, roughly two months old.** A fresh capture was not taken,
because the dump marker has to exist *before* a render and renders happen on the harness's schedule
rather than on demand. So `cwd` and `workspace.current_dir` are confirmed to have been sent, not
confirmed to be sent by the currently installed Claude Code. Anyone implementing this should re-arm
the marker and re-check, which costs one `touch` and one render.

## Acceptance sketch

* **Give `dir` the same chain shape `started` already has**, in this precedence order:
  1. the stamped `cwd` (unchanged, and it must keep winning — orphan recovery matches on it, per
     `ensureCwd`'s docstring, so the stamp is authoritative wherever it exists);
  2. the stdin JSON, `workspace.current_dir` then top-level `cwd`;
  3. `?` only when all are absent.

  `dirLabel` is already `Option[String] => Option[String]` and PURE (`sessionstore.scala:60-63`), so
  this composes as `dirLabel(readCwd(...).orElse(jsonDir))` without touching it. And `json` is
  already parsed for the `started` fallback at `:711`, so there is **no new plumbing** — the value is
  in scope on the adjacent line.

* **Prefer the JSON over the process's `user.dir`/`pwd`.** The JSON is the harness's statement of the
  session directory; the statusline process's working directory is a launch detail and can differ.
  Not using `pwd` is part of the fix, not an omission.

* **Record why `current_dir` and not the neighbours.** `workspace` also offers `project_dir` and
  `repo`, and either would render a plausible basename. `current_dir` is the right pick because the
  chip answers "which directory is this session in", which is also what the stamp it falls back from
  means; `project_dir` and `repo` answer different questions and would make the fallback disagree
  with the stamp it replaces. Worth stating so the choice is deliberate rather than the first key
  that fit.

* **Keep statusline read-only. Do NOT stamp `cwd` from statusline.** Writers stay the only stampers.
  Stamping here would be the tempting shortcut and would break the contract the docstring relies on:
  the stamp's value for orphan hunting is that it marks entries a *writer* touched.

* **On dimming the fallback value: there is no existing convention to mirror.** The proposal suggests
  rendering an unstamped dir dimmed, "mirroring how inferred values are marked". `started` is also
  inferred whenever it falls back, and it is rendered through `friendlyStamp` with no marker at all
  (`:721`). So this would *create* the convention rather than follow one, and it would be
  inconsistent to mark the new fallback while leaving the existing one unmarked. Either mark both or
  neither; my suggestion is neither, and to keep the diff to the chain.

* **Tests** (the four from the proposal, which are the right four — each fixes one link of the chain):
  - no store dir + JSON with `workspace.current_dir` → chip shows that basename
  - no store dir + JSON with only `cwd` → basename of `cwd`
  - stamped `cwd` present + a *different* JSON dir → **the stamped one wins** (this is the one that
    protects orphan recovery, so it is the test that must not be dropped)
  - neither → `dir: ?`, today's behaviour

  `SessionCliSuite` and `SessionStoreSuite` already exist as hosts (20 and 9 tests, both green at
  `b410903`).

* **Out of scope:** the rest of the payload. `session_name` is also sent on stdin while
  `SessionStore.readName` reads it from the store, which is plausibly the same asymmetry a second
  time, and is deliberately not argued here — unmeasured, so one instance is a floor, the same caveat
  issues 041 and 055 carry.

## Discussion

### Comment by bjornregnell at 2026-09-23 15:05

Reported from another session, where the chip read `dir: ?` after a run of read-only commands only.

Filed with the analysis verified rather than taken on trust, and two things came out of checking that
are worth keeping. The claim that the harness sends the directory is now **measured** from a real
captured payload rather than assumed, using the dump mechanism the statusline manual already
documents — and that also showed `workspace` carries `project_dir` and `repo`, which is why the
sketch now says *why* `current_dir` is the right one instead of just naming it. Against that, the
suggestion to dim the fallback turned out to have no precedent to mirror: `started` falls back
silently today, so dimming would invent a convention rather than follow one.

Agent disclosure: the observation, the asymmetry diagnosis and the proposed fallback chain are from a
Claude Code session of mine; another agent (Claude Opus 5) verified them and wrote this file. Verified
BY READING `statusline.scala:709-722`, `sessionstore.scala:60-63`, `:203-222`, and every `ensureCwd`
caller. Verified BY RUNNING the four reproduction steps, including `tt json keys` against the captured
payload. NOT verified, and flagged in the reproduction section: the `dir: ?` symptom in the measuring
session (a writer ran first, so the stamp existed), and whether the currently installed Claude Code
still sends those fields — the captured payload is from 2026-07-24.
