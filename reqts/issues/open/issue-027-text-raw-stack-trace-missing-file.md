# Issue 027: `tt text` prints a raw Java stack trace when the file does not exist

> status: open · labels: toolbox, text, polish, good-first-issue · summary: a twelve-frame
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
