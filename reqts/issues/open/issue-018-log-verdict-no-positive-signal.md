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

### Comment by hmiddelk/Opus5 at 2026-08-04 16:12

**CONFIRMED on Windows 10, and the empty-file case is now attested too.** Released `v0.10.0` native
`windows-x86_64` build, Windows 10 Enterprise 10.0.19045.

The non-log file reproduces exactly as reported:

```
> tt log %USERPROFILE%\.claude\settings.json
=== errors: 0
=== warnings: 0
=== verdict: 0 errors, 0 warnings          (exit 0)
```

The acceptance sketch asks that "zero-length input is called out explicitly rather than summarised as
clean", so that case was tested directly — a genuinely 0-byte file:

```
> tt log empty.log
=== errors: 0
=== warnings: 0
=== verdict: 0 errors, 0 warnings          (exit 0)
```

**Byte-identical to the settings-file output and to a real successful build.** So the list of
indistinguishable situations in the Description is confirmed empirically rather than by inference: an
empty log is not merely under-reported, it is indistinguishable from a clean one, and the exit code
offers no discrimination either. That strengthens the case for the `--require-markers` / non-zero-exit
option in the sketch, since a gate is the only thing that can catch it.

Platform-independent, as expected — recorded for two-platform evidence and for the empty-file data point.

Agent disclosure: the Windows re-test was run and written up by an AI agent (Claude Opus 5) under human
direction; the human reviewed and submitted.

### Comment by bjornregnell/Fable5 at 2026-08-07 15:56

Maintainer-side review (PR 2 triage), confirmed empirically on fixtures: clean, empty and
marker-less inputs produce byte-identical stdout, all exit 0; the sole verdict emission is
`log.scala:137`, a pure function of the two negative-marker counts. One erratum: the Description's
"wrong/mistyped file path" case is NOT indistinguishable — a nonexistent path exits 2 with
`log: not a readable file:` on stderr (`log.scala:125-127`); only a mistype landing on a different
EXISTING file is. The other three indistinguishable cases stand.

Triage: SPLIT. Defect half, v0.10.1 — the minimal discrimination between "verified clean" and
"saw nothing": distinct verdicts for EMPTY input and for no-recognised-markers, plus a
lines-scanned count. Enhancement half, v0.10.2 — the positive success-marker taxonomy and the
`--require-markers` exit gate (which the Windows empty-file data point argues for, since a gate is
the only thing that catches a truncated log in an unattended run). The existing test asserts
`contains("1 errors, 1 warnings")` and survives an additive change — keep the counts substring.

Agent disclosure: this review comment was produced by an AI agent (Claude Fable 5) under human
direction; the human reviewed and submitted.

### Comment by bjornregnell/Fable5 at 2026-08-07 20:28

DEFECT HALF SHIPPED in `8fb28af` (v0.10.1), issue stays OPEN re-scoped to the enhancement half.
What shipped, per the triage's minimal-discrimination scope: EMPTY input gets its own verdict
("EMPTY input (0 bytes) — nothing was scanned, this is not a clean run"), a zero-hit non-empty
file gets "0 errors, 0 warnings — but no log markers recognised in N lines (is this a log?)",
and a verdict with hits carries a lines-scanned count. The counts substring stayed additive as
required; three tests pin the three verdicts. Remaining scope for v0.10.2: the positive
success-marker taxonomy and the `--require-markers` non-zero-exit gate — which the Windows
empty-file data point above still argues for, since only a gate catches a truncated log in an
unattended run.

Agent disclosure: this comment was produced by an AI agent (Claude Fable 5) under human
direction; the human reviewed and submitted.
