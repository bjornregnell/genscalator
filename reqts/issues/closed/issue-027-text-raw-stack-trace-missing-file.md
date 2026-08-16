# Issue 027: `tt text` prints a raw Java stack trace when the file does not exist

> status: closed 2026-08-16, fixed by `7add972` · labels: toolbox, text, polish, good-first-issue · summary: a twelve-frame
> `NoSuchFileException` for "file isn't there" — reads as a tool crash, and is an outlier against the
> clean one-line failures the rest of the toolbox gives.

## Description

Found 2026-08-11 in the second alpha field test (report 087), hit twice while polling for an output file
that had not been written yet.

```
$ tt text count <abs-path-that-does-not-exist> "."
Exception in thread "main" java.nio.file.NoSuchFileException: <abs-path>
        at java.base@17.0.9/sun.nio.fs.UnixFileSystemProvider.newByteChannel(UnixFileSystemProvider.java:218)
        at java.base@17.0.9/java.nio.file.Files.newByteChannel(Files.java:380)
        at java.base@17.0.9/java.nio.file.Files.newByteChannel(Files.java:432)
        at java.base@17.0.9/java.nio.file.Files.readAllBytes(Files.java:3288)
        at agenttools.Lib$.readUtf8(lib.scala:17)
        at text$package$.text(text.scala:79)
        at Dispatch$.$init$$$anonfun$37(dispatch.scala:58)
        at scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
        at scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
        at Dispatch$.dispatch(dispatch.scala:87)
        at dispatch$package$.dispatchTypedTools(dispatch.scala:109)
        at dispatchTypedTools.main(dispatch.scala:107)
```

The path is unreadable at `Lib.readUtf8` (`lib.scala:17`) and the exception escapes to the default
handler instead of being turned into a message.

**Why it is worth a line of code.** Three reasons, none of them cosmetic on their own but they compound:

* **It reads as a crash.** A twelve-frame JVM trace is what a *bug in the tool* looks like. "The file you
  named is not there" is a caller error and should look like one.
* **It is an outlier.** `tt bloop`, `tt update`, `tt git`, `tt log` and `tt find` all degrade with one
  clean line — `tt log` even gained a `(resolved: …)` clause (noted in issue 020). So the house style is
  established and `tt text` simply misses it.
* **It burns context.** In an agent session the trace is twelve lines of noise for one bit of
  information, and it happened twice in one session on an entirely ordinary "has the file appeared yet?"
  check.

`tt log` already has exactly the right message for this case — `log: not a readable file: <path>` — so
this is a matter of routing `tt text` (and any sibling that reads a file via `Lib.readUtf8`) through the
same shape.

## How to reproduce it

```
$ tt text count /no/such/file "."
$ tt text match /no/such/file "x"
$ tt text freq  /no/such/file "x"
```

Worth checking the other `Lib.readUtf8` callers at the same time — the defect is in the shared read path,
so any tool taking a file argument is likely to have it.

## Acceptance sketch

* `text: no such file: <path>` on stderr, exit 2, no trace — matching `tt log`'s existing wording, and
  ideally with the same `(resolved: …)` clause when the argument is relative.
* Fixed once in `Lib.readUtf8`'s callers (or in a small `Lib.readUtf8OrExit`) rather than per verb, so the
  siblings cannot drift.
* A distinct message for "exists but is a directory", which is the other easy mistake.
* A test per shape, since this is precisely the kind of path a green suite never enters.

## Discussion

### Comment by hmiddelk/Opus5 at 2026-08-11 15:01

Filed from the second alpha field test (report 087). Smallest finding of that batch and the most
mechanical; filed on the same "report anything that wedges, however small" principle as issue 020.

Agent disclosure: found and drafted by an AI agent (Claude Opus 5) under human direction; the human
reviewed and submitted.

### Comment by bjornregnell/Opus5 at 2026-08-13 15:59

Maintainer-side review (PR 3 triage), reproduced live on `main` at `542b2fd` by a dedicated review agent.

**CONFIRMED, unchanged by the v0.10.2 wave, and materially understated on breadth.** All three shapes
still trace and the cited line numbers still match exactly. One correction to the report: the exit code is
**1**, not 2, because `Dispatch.dispatch` has no catch-all and the exception reaches the default handler.

**It is five `tt text` shapes, not three.** `context` (`text.scala:91`) and `cols` (`text.scala:143`)
trace identically. Only `grepr` is guarded (`text.scala:123-127`).

**Your breadth guess is right, and the review named the actual set: seven verbs, twelve call sites.**
Confirmed live, all raw traces, all exit 1: `tt md-fmt` (`md-fmt.scala:165`), `tt ssg --status-update`
(`ssg.scala:461`, via an unvalidated `postFiles()`), `tt ssg --template` (`ssg.scala:511`), `tt htmltext`
(`htmltext.scala:73`), `tt ascii sequence` (`ascii.scala:162`), `tt svg sequence` (`svg.scala:222`), and
`tt parsereqt parse` (`parsereqt.scala:21`, a `FileNotFoundException` at fourteen frames).

**That yields the one correction that changes the fix.** Four of those seven do **not** go through
`Lib.readUtf8` at all: `htmltext`, `ascii` and `svg` call `Files.readString` directly, and `parsereqt`
uses `Source.fromFile`. So "fix it once in `Lib.readUtf8`'s callers" would leave a third of the affected
verbs still tracing. The guard has to be a shared `Lib.requireReadableFile(tool, path)` applied at each
driver, not a change to the readers.

**Your generalisation is too strong in the other direction, which is good news.** "Any tool taking a file
argument is likely to have it" is false: `tt json` (`cannot read ...`), `tt tsv` and `tt sub`
(`not a file: ...`, `sub.scala:111,118`) already degrade cleanly, as do `box`, `boxstats` and `links`.
Several callers are also safe by construction because their paths come from a validated walk
(`files.scala:64`, `text.scala:135`, `ssg.scala:482,512,520`, `log.scala:152`) and must not be touched.

**The directory case is worse than the issue assumes, and it constrains the implementation.**
`tt text count <an existing dir> '.'` throws a *different* exception, `java.io.IOException: Is a
directory`, at nineteen frames. A guard written as `Files.exists(p)` would still trace. It must be
`Files.isRegularFile(p)`, exactly as `log.scala:147` already does.

**One correction to the house-style claim.** The style is real but it has four wordings already:
`not a readable file: X (resolved: Y)` (log), `no such path: X` (find), `not a directory: X (resolved: Y)`
(grepr), `not a file: X` (sub, tsv), `cannot read X` (json). Your proposed `text: no such file: <path>`
would add a fifth, so we are taking your own "ideally" clause instead and adopting `tt log`'s exact
wording, including the `(resolved: ...)` clause and the directory branch it already has. Also, `tt bloop`
is cited as a path-degradation exemplar and is not one: it takes verbs, not a path.

**Bonus defect found while verifying this, and it is arguably worse than the one you filed.**
`tt files /no/such/dir .scala` prints `0 files` and exits **0**, because `Lib.walkPruned`'s
`visitFileFailed` returns `CONTINUE` (`lib.scala:188-189`). A missing root is indistinguishable from an
empty result, which is a wrong answer rather than a loud failure, and `tt find` on the same input
correctly exits 2, so the sibling walkers disagree. Filed separately as issue 031, credited to this
report.

**Triage: accepted for the v0.10.3 wave**, as a shared `Lib.requireReadableFile` plus guards at the eight
unvalidated call sites, keeping `readUtf8` and `readLatin1` pure and throwing so the exit stays in the
effectful drivers. Tests go in `tools/test/cli.test.scala` alongside the existing models at `:451` and
`:987`, one per shape (missing file, existing directory), each asserting that stderr does not contain
`Exception in thread`. `tools/test/text.test.scala` is the wrong home, since it exercises pure helpers.

Smallest finding of your batch and it grew into the widest fix. Thank you for filing it anyway.
