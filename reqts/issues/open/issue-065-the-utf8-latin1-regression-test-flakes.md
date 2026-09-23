# Issue 065: the UTF-8 latin1 regression test failed once and passed once on identical code, so the test guarding a real encoding bug is not trustworthy

> status: open 2026-09-23 · labels: tests, flaky, toolchain, encoding · measured against: v0.10.2
> (from `VERSION.txt`) at `1dba5b1` plus the issue-050 filter branch, scala-cli 1.17.1, Scala 3.9.0,
> Linux · summary: `cli.test.scala:274` — *"text count reads UTF-8 (Swedish å/ä/ö not mangled) —
> regression for the latin1 bug"* — failed with `expected 1, obtained 0` on one run and passed on the
> next, with **no change to the code between them**. It is the regression test for a real encoding
> defect, so a flake there is worse than a flake elsewhere: the run that matters is the one where it
> passes for the wrong reason. Not reproduced deliberately; this issue records the observation and the
> eliminations, not a diagnosis.

## Description

```scala
test("text count reads UTF-8 (Swedish å/ä/ö not mangled) — regression for the latin1 bug") {
  val f = os.temp(contents = "Björn\nRegnell\n", suffix = ".txt")
  try
    val (code, out, _) = run("text", "count", f.toString, "ö")
    assertEquals(code, 0)
    assertEquals(out, "1") // read as latin1 the UTF-8 ö byte-pair would NOT match the char 'ö' → 0
  finally os.remove(f)
}
```

Observed 2026-09-23 while building the issue-050 stderr filter, across three full `CliSuite` runs on
the same machine within a few hours:

| run | tree | `cli.test.scala:274` | `:2462` (issue 050) |
| --- | --- | --- | --- |
| 1 | `main` @ `1dba5b1`, unmodified | pass | FAIL (expected) |
| 2 | issue-050 filter branch, warm caches | **FAIL** — `expected 1, obtained 0` | pass |
| 3 | issue-050 filter branch, after `tt bloop clean` | pass | pass |

Runs 2 and 3 are **the same commit**. The only deliberate difference is that the build caches were
cleared between them.

### Why it is not the change that was in flight

The branch under test only touched captured **stderr** — it added `ToolchainNoise.strip` and routed
`err` through it in three suites. `:274` asserts on `out`. That is an argument, not a proof, so it was
checked rather than assumed:

* **The source literals are byte-identical to `main`'s.** `Björn` carries `303 266` (`c3 b6`, UTF-8
  `ö`) in both, so nothing re-encoded the test file.
* **A faithful JVM reproduction returns `1`.** A script doing exactly what the test does —
  `os.temp(contents = "Björn\nRegnell\n")`, then `os.proc` the tool with `"ö"` as an argument — reports
  `exit=0 out=[1]` and file bytes `42 6a c3 b6 72 6e …`.
* **The JVM's encodings are the same on both scala-cli versions.** `defaultCharset`, `file.encoding`,
  `sun.jnu.encoding` and `native.encoding` are all `UTF-8` under 1.15.0 and 1.17.1.
* **The diff is stdout-neutral**, touching only the third element of the returned triple.

So the mechanism by which the branch could have caused it is not visible, and the failure did not
survive a re-run.

### The leading hypothesis, stated as a hypothesis

A **stale build unit**. The branch added a `//> using dep` line to `tools/test/testsupport.test.scala`,
which re-hashes that build unit; run 2 was the first compile across that boundary, and run 3 followed a
`tt bloop clean`. That would make it an artifact of incremental compilation rather than of the code.

This is **not established**. Two things changed between runs 2 and 3 — the cache was cleared *and* the
suite was run again — so the recovery cannot be attributed to either. A single pass does not
distinguish "fixed by cleaning" from "passed by chance".

### Why this is worth a number rather than a footnote

**It is the test for a defect that actually happened.** The comment on `:274` records what it is for:
read as latin1, the UTF-8 `ö` byte pair would not match the char `ö` and the count would be 0 —
which is precisely the failure that was observed. A test that can produce its own failure signature
*without* the bug being present cannot distinguish the bug from noise, and the cost lands the day
someone reintroduces latin1 reading and this test is the one that was supposed to catch it.

**A flake in a regression test degrades quietly.** The rational response to a test that fails once and
passes on re-run is to re-run it. That habit is exactly what would carry a real encoding regression
through.

**It sits next to a sibling with the same shape.** `cli.test.scala:277`, *"text match preserves UTF-8
Swedish characters in its output (no mojibake)"*, builds its fixture the same way and passes a
non-ASCII argument the same way. It did not fail in any of the three runs, but nothing established
here says it could not.

## How to reproduce it

Not reproduced on demand — that is the gap. What is known:

```bash
# the failing observation, run 2 above (not reliably reproducible)
scala-cli test tools --test-only 'CliSuite' --java-prop tt.tools="$PWD/tools"
#    => ==> X CliSuite.text count reads UTF-8 (Swedish å/ä/ö not mangled)
#       cli.test.scala:274  expected 1, obtained 0

# the isolated path, which has never failed
#   os.temp(contents = "Björn\nRegnell\n"), then os.proc(scala-cli run tools/text.scala -- count <f> "ö")
#    => exit=0 out=[1], file bytes 42 6a c3 b6 72 6e 0a ...
```

Anyone chasing this should start by trying to make it fail again, since every elimination above rests
on a single red observation.

## Acceptance sketch

* **First, characterise it — do not fix it.** One red run is not enough to act on. Run `CliSuite`
  repeatedly on an unchanged tree and record how often `:274` fails; a flake that never recurs in
  twenty runs is a different problem from one that recurs in three.
* **Test the stale-unit hypothesis directly, one variable at a time.** Re-hash the test build unit (a
  trivial `using` change) *without* cleaning, and see whether the first run after it fails. That
  separates "incremental compilation artifact" from "coincidence", which runs 2 and 3 could not.
* **Make the failure self-diagnosing if it survives.** `expected 1, obtained 0` says the count was
  wrong and nothing about *why*. Printing the fixture's bytes and the received argument on failure
  would distinguish a mangled file from a mangled argv at the moment it happens, instead of requiring
  the reproduction that is currently missing.
* **Check the sibling.** `cli.test.scala:277` has the same fixture and argument shape. Whatever
  characterisation is done for `:274` should cover it, or state why it is exempt.
* **Out of scope:** the issue-050 filter, which is on its own branch and was cleared above; and the
  `tt text` UTF-8 behaviour itself, which is correct in every isolated run performed here.

## Discussion

### Comment by hmiddelk at 2026-09-23 18:45

Filed because the honest thing to do with a one-off red is to record it rather than let a passing
re-run erase it. The issue-050 work needed a control run anyway, and that control is what makes this
recordable: the same test passed on unmodified `main` in the same session, so there is a baseline.

I want to be clear about the limits of what is here. I did **not** reproduce the failure. Everything in
the eliminations section rules out a specific mechanism, and none of it explains what actually
happened. The stale-build-unit hypothesis is the one I would test first, but I changed two variables
between the failing and passing runs and so cannot claim it.

What made me file it rather than dismiss it is the comment on the test itself. It exists because reading
latin1 produces exactly `0` where `1` is expected — so the flake's signature is indistinguishable from
the bug's. That is the property that makes a re-run the wrong reflex, and it is worth someone knowing
before they meet it.

Agent disclosure: observed and drafted by an AI agent (Claude Opus 5) in session with me, and reviewed
by me, while it was building the issue-050 filter. Verified BY RUNNING: the three `CliSuite` runs
tabulated above, including the control on unmodified `main`; an isolated JVM reproduction of the test's
exact path; and the encoding-property probe under both scala-cli 1.15.0 and 1.17.1. Verified BY
READING: the test at `cli.test.scala:269-276`, its sibling at `:277`, and the byte content of the
source literals on both trees. NOT verified: the cause; whether the failure recurs at all; whether the
sibling test shares the exposure; and anything on macOS or Windows.
