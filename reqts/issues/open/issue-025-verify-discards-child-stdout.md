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

### Comment by bjornregnell/Opus5 at 2026-08-13 15:59

Maintainer-side review (PR 3 triage), verified against `main` at `542b2fd` by a dedicated review agent.

**Accepted, with one claim corrected and two additions that make the case stronger.**

**Corrected: "the child's own output is never echoed" is false as written.** On the FAIL path,
`verify.scala:126-131` prints `--- last output ---` plus the last 20 lines of stdout, and a
`--- stderr ---` section when stderr is non-empty. So the real contract is "observable only after death,
and only if it died wrong". The long run that **passes** is the maximally blind case, which is your
ergonomics complaint exactly, but the flat claim is refutable and the narrower one is stronger.

**Confirmed on the core.** `verify.scala:107` calls the child with `stdout = os.Pipe, stderr = os.Pipe`,
and verify's first byte of its own output is `verify.scala:115`, after `.call` has returned and the text
has been drained. Your `run_in_background` analysis is right and does not even need os-lib internals:
verify's stdout receives nothing at all until the child exits, so there is nothing for the harness to
capture. The allowlist is as you quote it (`verify.scala:19`: `Set("scala-cli", "tt", "scalex")`).

**One attribution to fix.** The guard rules forbid two of the three workarounds you list, not all three:
`guardcheck.scala:91-93` catches `| head|tail|wc` and `:185-187` catches `>`, but there is **no check that
matches `tee`**, verified live (`tt guardcheck cmd "... | tee /tmp/x.log"` returns clean). The harness
permission prompt still gates it, so the workaround is blocked in practice, just not by guardcheck.

**Addition that raises this above ergonomics.** `skills/avoid-guard-stall/SKILL.md:38-39` prescribes
`run_in_background` as *the* sanctioned substitute for both `| tail` and `> file`, and
`guardcheck.scala:187` tells the caller to "use the tool's file-sink flag or run_in_background".
`tt verify` is the one tool where that documented escape hatch silently fails and which has no file-sink
flag. The failure mode is therefore reachable by following our own documentation correctly, which is a
doc/tool contradiction rather than a missing convenience.

**Second addition: there is no `--timeout` either.** `verify.scala:107` passes no timeout, so os-lib's
infinite default applies, while `box.scala:93`, `git.scala:130` and `git.scala:306` all pass one. Blind
and unbounded is a worse pairing than blind alone, and a genuinely wedged child hangs verify forever with
no output at all. We will bundle `--timeout N` into the same change; it is three lines and has none of
the traps the tee half has.

**Trap for whoever implements it, because the obvious fix is silently broken.** Swapping `os.Pipe` for
`os.Inherit` (copying `scala.scala:105`) streams perfectly and empties `result.out`, so every `--out` and
`--err` check would report "missing" and the tool would FAIL everything that passes. The fix has to use
line-callback sinks that both print and accumulate. Also: suppress the `--- last output ---` replay when
teeing, and expect stdout/stderr interleaving not to reproduce the child's true ordering, since the
callbacks run on separate reader threads.

**Scoping note that narrows your own honest note further.** `tt scala` (`scala.scala:105`) and `tt sbt`
(`sbt.scala:82`) already stream with `os.Inherit`. If either killed run was a scala-cli project build or
test, `tt scala test <dir>` would have shown live output. So the gap is specifically "a long run that
needs assertions", not "a long run".

**Triage: accepted for the v0.10.3 wave as `--tee` plus `--timeout`.** We agree with your severity read:
ergonomics on a long-run path, not a defect in verify's contract, which held up well under real load.
`--out-file` and the unbounded in-memory capture (`result.out.text()` materialises a 40-minute log whole)
are deliberately left out of that change, because a ring buffer would break `--out` substring checks that
span the cut. That tension wants its own issue rather than a rushed flag.
