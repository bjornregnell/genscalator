# Issue 025: `tt verify` discards the child's stdout, so a long-running command is unobservable

> status: open · labels: toolbox, verify, ergonomics · summary: verify captures the child's output and
> prints only its own audit line, with no `--tee` / `--out-file`. For a fast assertion that is right; for
> a run measured in minutes it means progress is indistinguishable from a hang — and the guard rules
> correctly forbid the pipe shapes that would work around it.

## Description

Found 2026-08-11 in the second alpha field test (report 087), driving a long compile gate on a
third-party Scala repo.

`tt verify` runs the child as argv, captures exit/stdout/stderr, checks them, and prints an audit line
plus a verdict. The child's own output is never echoed:

```
$ tt verify -- tt files <abs-dir> scala --count
=== ran: tt files <abs-dir> scala --count (exit 0, 58 ms)
=== PASS
```

The count — the entire point of the command — is gone. For `--out <substr>` assertions this is exactly
the intended design and it is a good one: verify's value is that it turns a prose claim into one
allowlistable call with a clean PASS/FAIL, and echoing everything would bury that.

The problem is the long tail. The gate under test in the field test runs several hundred compiles and
takes minutes; wrapped in verify it produces **nothing at all** until it exits. Two runs (≈15 min and
≈40 min) were killed because there was no way to tell a working run from a wedged one.

**The workarounds are all blocked or wrong:**

* `| tee`, `> file`, `| tail` — forbidden by the guard rules, and rightly so.
* the harness's `run_in_background` — does not help; the buffering is inside verify, not the harness, so
  the captured output file stays empty until exit.
* running the child directly instead of through verify — abandons the audit line and the allowlist
  discipline, i.e. gives up the reason for using verify.
* **what was actually done:** add a `--report <file>` flag to the *program under test* so it writes its
  own progress, then read that. This works, but it exports verify's observability problem to the callee
  and is only available when you own the callee. It is a workaround for a third-party binary only if that
  binary happens to have such a flag.

**Why it matters beyond convenience.** "Drive a long build and watch it" is a normal thing to want from a
run-and-verify driver, and it is the situation where an agent is most likely to reach for a forbidden
pipe shape. A tool that makes the guard-clean path unusable for a whole class of commands pushes callers
toward the shapes the guard exists to prevent — the same dynamic issue 004 names as the tripwire rule.

## How to reproduce it

Any command that prints and takes a while:

```
$ tt verify -- tt files <abs-dir> scala --count      # count not shown, only the audit line
$ tt verify --help                                    # no --tee, no --out-file, no streaming flag
```

For the long-run shape, wrap anything that emits progress over minutes (e.g.
`tt verify -- scala-cli run <script>` on a script that prints a line per unit of work): nothing appears
until exit, including in the harness's background-output file.

## Acceptance sketch

* `--tee` — echo the child's stdout (and stderr) as it arrives, with the audit line and verdict still
  printed last so the PASS/FAIL contract is unchanged.
* and/or `--out-file <path>` — write the captured child output to a file as it streams, so a background
  or CI run leaves an artifact without any shell redirect at the call site.
* Either flag must not change exit semantics or the check results; it is purely about what reaches the
  caller's eyes.
* Worth documenting in the help *why* the default is silent, so the default reads as a deliberate design
  choice rather than an omission.

## Discussion

### Comment by hmiddelk/Opus5 at 2026-08-11 15:01

Filed from the second alpha field test (report 087). Recorded there as the finding that actually changed
the work rather than costing an exchange.

Honest scoping note: the severity here is task-shaped and partly confounded with the program under test.
The gate spawns one compiler per snippet, which is what made runs long enough for the buffering to hurt;
a command that finishes in ten seconds would never expose this. The gap is real regardless, but it should
be triaged as ergonomics on a long-run path, not as a defect in verify's core contract — which held up
well under real load, including the human-only `TT_VERIFY_ALLOW` widening (no widening was needed; the
built-in `scala-cli, tt, scalex` allowlist covered the whole task).

Agent disclosure: found and drafted by an AI agent (Claude Opus 5) under human direction; the human
reviewed and submitted.
