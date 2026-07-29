# Issue 018: `tt log`'s verdict has no positive signal — a clean build and a non-log file are indistinguishable

> status: open · labels: toolbox, log, agent-trust · summary: `tt log` reports only error/warning counts,
> so a successful 57-second sbt compile and a JSON settings file produce byte-identical output and exit 0;
> "0 errors, 0 warnings" is an unfalsifiable pass.

## Description

Found 2026-07-29 in an alpha field test, while using `tt log` exactly as intended — to digest a large
build log without pulling it into context.

A real, successful sbt compile (2 Scala sources, three `[success]` lines, 57s):

```
$ tt log <sbt-build.log>
=== errors: 0
=== warnings: 0
=== verdict: 0 errors, 0 warnings
```

Correct. But a file that is not a log at all gives the identical output and exit code:

```
$ tt log ~/.claude/settings.json
=== errors: 0
=== warnings: 0
=== verdict: 0 errors, 0 warnings
```

So at least four distinct situations are indistinguishable from the verdict: a genuinely clean build; a
build that compiled nothing; a truncated or empty log; and a wrong/mistyped file path.

**Why it wedges.** `tt log` is the sanctioned way for an agent to check a build without reading the whole
log — the tool exists to be trusted in place of the raw text. That makes a false pass expensive: an agent
that trusts the verdict will report "build clean" for an empty log or a typo in the path, and nothing in
the output contradicts it. In this field test the build was only *known* to be real because the raw log
was read afterwards and showed `compiling 1 Scala source ...`; stopping at the verdict would have been a
green light with no evidence behind it. The current design reports the absence of bad news as if it were
good news.

## How to reproduce it

```
$ tt log <a-successful-build.log>
=== errors: 0
=== warnings: 0
=== verdict: 0 errors, 0 warnings

$ tt log <any-file-with-no-log-markers>     # e.g. a small .json
=== errors: 0
=== warnings: 0
=== verdict: 0 errors, 0 warnings           # identical, exit 0
```

## Acceptance sketch

* The summary carries a positive signal: lines scanned, plus counts of recognised success/activity
  markers (`[success]`, `compiling N source`, test-passed lines) alongside the error/warning buckets.
* A distinct verdict when NO recognised log markers of any kind are present — e.g.
  `=== verdict: no log markers recognised (N lines scanned) — is this a log file?` — so a wrong path or an
  empty file cannot masquerade as a clean run.
* Consider a non-zero exit (or an opt-in `--require-markers`) for the no-markers case, so it can gate.
* Zero-length input is called out explicitly rather than summarised as clean.

## Discussion

### Comment by hmiddelk/Opus5 at 2026-07-29 23:27

Filed from the same alpha field test as issues 014–017, and the only finding that surfaced from *using* a
tool for real work rather than from enumerating the surface — it appeared while digesting the
`tt sbt --dir <repo> compile` log (`tt sbt` itself performed well: correct directory handling with no
shell `cd`, arg and exit-code passthrough, and a `tt verify`-style audit line).

Note the curated default markers are deliberately built so tally lines like "0 errors" do not
false-positive; this issue is the mirror-image risk on the negative side.

Agent disclosure: found and drafted by an AI agent (Claude Opus 5) under human direction; the human
reviewed and submitted.
