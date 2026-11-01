# Issue 261101-statusline-stale-binary-churn: a stale native binary sends the status line through a full scala-cli compile on every refresh

> status: open 2026-11-01 · labels: tools, native, statusline, performance · measured against: main at
> `e110b8f` on Linux · summary: when any `tools/**.scala` is newer than the native binary, the `tt`
> launcher (`tools/tt:63`) routes every call through `scala-cli run`. That is the right trade for a
> command a human or agent runs once. For the status line, which Claude Code re-runs on every refresh,
> it turns one stale file into a continuous JVM compile load and keeps a ~3 GB Bloop server busy. The
> staleness test also counts `tools/test/**`, so editing only a TEST, which cannot change the binary,
> triggers it.

## Description

`tools/tt:59-74` picks the runner: the native binary when it exists and no `*.scala` under `$TOOLS` is
newer than it, else `scala-cli run $TOOLS/<tool>.scala`. The comment states the reason: a stale binary
would silently run old tool behaviour, so staleness "degrades to slow, never to wrong".

Two things make that trade expensive for the status line:

1. **The status line runs constantly.** Each refresh is a `tt statusline` call, so while the binary is
   stale every refresh is a scala-cli compile-and-run instead of a ~10 ms native call. On 2026-11-01,
   with the binary stale, `tt box health` showed `scala-cli run .../tools/statusline.scala` in the
   process list and a Bloop server at 3.0 GB RSS and 13.7% CPU. Bloop had been stopped with
   `scala-cli bloop exit` 1m37s earlier and was already back, restarted by the status line itself. The
   box line then reported the JVM and Bloop as the heavy consumers, which was true, but the cause was the
   stale binary, not anything the human was running.
2. **Test-only edits count as staleness.** `find "$TOOLS" -name '*.scala' -newer "$bin"` includes
   `tools/test/`. The trigger on 2026-11-01 was an edit to `tools/test/payload.test.scala` and
   `tools/test/links.test.scala` only; the binary was in fact current.

For the status line, slightly old behaviour is a far smaller cost than a compile per refresh: it only
formats a line, and a stale-but-fast line is still correct in every way that matters to the reader.

Related: issue 052 (staleness is mtime, not content). That issue is about a touched-but-unchanged file
disarming the fast path; this one is about which files count, and about what the status line should
do when the fast path is disarmed.

## How to reproduce it

1. Build the native binary (`scala-cli run deploy/buildnative.sc`); `tt --version` is fast and quiet.
2. Touch only a test file: edit any `tools/test/*.test.scala`.
3. Watch `tt box health` while the status line refreshes: `scala-cli run .../statusline.scala`
   processes appear, Bloop starts and stays busy, and stopping Bloop does not help because the next
   refresh restarts it.

## Acceptance sketch

- Edits under `tools/test/` do not make the binary stale (the binary is built from the tools, not the
  tests). A test that touches only a test file and asserts the native path is still chosen.
- When the binary IS stale, `tt statusline` keeps using it, with a visible marker in the line (for
  example a `stale` chip), instead of falling back to scala-cli. Other verbs keep today's
  "slow, never wrong" fallback. The status line is the one caller where frequency outweighs freshness.
- The marker tells the human what to do: `scala-cli run deploy/buildnative.sc`.

## Discussion
