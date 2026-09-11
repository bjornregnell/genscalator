# Issue 054: `payloadsync --staged` with no directory argument silently falls back to the weaker check and exits 0, so the release gate can become a non-gate without anyone noticing

> status: open 2026-09-11 · labels: release, installer, deploy, gate, correctness · measured against:
> v0.10.2 at `1dffcb3` (the merge of PR #13, which introduced the file) · summary: `deploy/payloadsync.sc:43`
> reads `--staged` with `argv.sliding(2).collectFirst { case "--staged" :: d :: _ => … }`, which yields
> `None` when `--staged` is the last token. Control then falls into the `case None` branch at `:73`,
> which compares the committed region against the workflow parser, prints `payload layout OK` and exits
> **0** — without ever listing the staged tree. The strongest of the two gates degrades into the weaker
> one on a malformed invocation, and reports success while doing it.

## Description

PR #13 added two deliberately independent gates. The `--staged` gate (`payloadsync.sc:55-71`) lists the
real staging tree with `Files.list` and compares it against what the shipped uninstaller will claim; the
default gate (`:73-92`) re-derives the layout from the workflow text and compares it against the committed
region. The argument for having both is that they share no code, so a bug in the workflow parser cannot
make both agree — this is written out at `tools/payloadlib.scala:90-94`.

That argument holds only while the caller actually reaches the gate it asked for. It does not:

```scala
val staged  = argv.sliding(2).collectFirst { case "--staged" :: d :: _ => Path.of(d) }
```

`sliding(2)` over `List("--staged")` yields a single window of length 1, and over
`List("--write", "--staged")` a single window whose head is `--write`. Neither matches the pattern, so
`staged` is `None` and `:73` runs instead. Nothing warns. The exit code is 0 and stdout reads
`payload layout OK`.

`--root` at `:44` has the same shape with `.getOrElse(Path.of("."))`, so a dropped argument there
silently means "the current directory" instead of the intended root. That one is less dangerous, because
`:48-49` dies if the expected files are not found beneath it, but it is the same latent defect and should
be fixed in the same pass.

**Why this matters more than a normal argument-parsing nit.** `.github/workflows/native-release.yml:178-179`
is the only caller that passes `--staged`, and it sits between the staging step and the archiving step
precisely so that a release cannot ship an uninstaller that disagrees with what it packs. If a future edit
to that line drops or mis-quotes the directory — a refactor, a matrix variable that expands to empty, a
YAML folding accident — the release still goes green, and it goes green having never looked at the tree it
is about to ship. A gate that cannot fail is worse than no gate, because it is believed.

One honest mitigation: the two branches print different text (`(staged tree == get-genscalator.sc)` versus
`(committed region == regeneration)`), so the degradation is visible to somebody reading the log closely.
Nobody reads a green CI log closely. That is the whole point of the issue.

## How to reproduce it

From the repo root, with a tree whose committed region already matches the workflow:

```
scala-cli run deploy/payloadsync.sc -- --staged
```

Observed: exit 0, `payload layout OK: … (committed region == regeneration)`. The staged tree is never
listed, and no message says that `--staged` was ignored.

Contrast with the correct invocation, which does list the tree:

```
scala-cli run deploy/payloadsync.sc -- --staged <dir>
```

## Discussion

### Comment by bjornregnell at 2026-09-11 15:30

Found while reviewing PR #13 before merging it (agent-assisted review; the flag-parsing path was read
rather than executed, then confirmed by running both invocations). I merged #13 as it stands, because the
defect is narrow, the gate is a net improvement over what was there before, and holding a good change
hostage to a follow-up is how follow-ups stop happening. This is that follow-up.

Sketch of the fix, for whoever takes it: treat a flag that expects a value as an error when the value is
absent, rather than as an absent flag. Something like scanning `argv` positionally and calling `die` when
`--staged` or `--root` is the last token or is followed by another `--…` token. `die` already exists at
`:37` and exits 2, which is the behaviour CI needs.

The acceptance condition I care about is that it fails, not that it parses: a test that invokes the script
with `--staged` and no argument and asserts a **non-zero** exit. A test that only checks the happy path
would have passed against this defect on the day it was written.

While in the file, `Files.list(dir)` at `:59` is never closed. The idiom elsewhere in the repo is
`Using.resource(Files.walk(...))` (see `get-genscalator.sc:244,293`). Same pass, one line.
