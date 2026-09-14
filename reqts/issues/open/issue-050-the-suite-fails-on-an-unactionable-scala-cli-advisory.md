# Issue 050: the suite fails on a scala-cli advisory the repo is designed not to act on, and the obvious repair would delete its only exhaustive stderr assertion

> status: open 2026-08-29 · labels: tests, toolchain, silent-blindspot · measured against: v0.10.2
> (from `VERSION.txt`) at `c51a728`, scala-cli 1.15.0, Scala 3.9.0-RC4, Linux · summary: every tool
> file carries `//> using file project.scala` deliberately, so scala-cli 1.15.0 prints a
> "Using directives detected in multiple files" advisory to **stderr on every per-file run** — advice
> the repo cannot take, because that include is what makes the launcher's single-file path agree with
> whole-directory builds. `CliSuite` has 292 tests and reads stderr in 39 of them, always by
> `contains`; **exactly one assertion constrains stderr exhaustively** (`cli.test.scala:2454`,
> `assertEquals(run(...), (0, "gs", ""))`) and it is the only one the advisory can break. So the suite
> is red for a reason unrelated to the `json` test it names. The trap is the fix: weakening that line
> to `._2`, like its 40 neighbours, would remove the only check in the suite capable of noticing a
> tool that writes something unexpected to stderr on a success path.

## Description

Two facts, each correct alone, that compose into a permanently red test.

**One.** Every tool file carries `//> using file project.scala` explicitly, and that is load-bearing
rather than incidental. `project.scala:10-14` states why: the `tt` launcher's scala-cli fallback runs
ONE tool file (`scala-cli run tools/<tool>.scala`), and that build unit does not contain the rest of
the directory — the explicit include is what makes the single-file path agree with whole-directory
builds. `ScalaVersionSuite` asserts the rule in both directions (every `@main` tool carries it; no
mainless helper does, because scala-cli cannot chain `using file`).

**Two.** Because directives therefore exist in both the tool file and `project.scala`, scala-cli
1.15.0 writes this to **stderr, on every per-file run, warm cache included**:

```
[warn]  Using directives detected in multiple files:
- tools/json.scala:1:1-3:30
It is recommended to keep them centralized in the <root>/tools/project.scala file.
```

The recommendation cannot be followed. Centralising the *version* in `project.scala` is exactly what
the repo already does — the advisory is about the `using file` lines themselves, and removing those is
what would break the launcher. So the warning is both permanent and unactionable.

### Where it lands, and why only once

`CliSuite`'s `run` helper returns `(exit, stdout, stderr)` and the suite has 292 tests. Stderr is
inspected in 39 of them — and always in the tolerant shape:

```scala
val (code, _, err) = run("doc", "--docs", d.toString, "nope")
assert(clue(err).contains("no such doc"))
```

Containment cannot be broken by an extra line appearing *alongside* the expected text. There are zero
uses of `._3`. **Exactly one assertion in the suite constrains stderr exhaustively** —
`cli.test.scala:2454`:

```scala
assertEquals(run("json", "get", f.toString, "name"), (0, "gs", ""))
```

It is the only whole-tuple comparison against `run` in the file, and it is the only assertion the
advisory can break. Exit code and stdout are both correct; the test fails on the third element. The
failure message names `json get: dot path with a numeric array index prints the scalar unquoted`,
which is not what is broken, and `json` is not the tool at fault.

There is a second, independent stderr source with the same victim: on a **cold** build unit scala-cli
also writes `Compiling project (...)` / `Compiled project (...)` to stderr. So `2454` needs both no
advisory *and* a warm cache to pass. Suppressing the advisory alone leaves it fragile on a fresh
checkout — which is precisely the situation in which someone runs the suite for the first time.

### The part that matters more than the red

The instinct is to weaken `2454` to `._2` so it matches its 40 neighbours. That would make the suite
green and **delete the only exhaustive stderr assertion across 46 verbs and 292 tests.**

Nothing else could then notice a tool that starts writing to stderr on a *success* path. Thirty-nine
containment assertions can detect stderr that is missing something expected; none can detect stderr
that has *gained* something. A verb that grew a spurious warning, a deprecation notice, or a caught
stack trace beside correct stdout and exit 0 would pass all 292 tests and every one of those 39
checks.

That is the same shape as **issue 041** (the description carrier that drifted in six verbs because no
test ran the path it lived on) and **issue 018** (the absence of bad news reported as good news): the
check that would catch a whole class exists exactly once, close to by accident, and the cheapest
repair is the one that removes it. Which is the argument for treating this as a design question about
what the suite can see, rather than as one red test to get past.

### Why nothing has reported it

**No CI workflow runs the suite.** `.github/workflows/` holds two: `links-check.yml`, which runs
`tools/links.scala -- check`, and `native-release.yml`, which builds and uploads the native binaries.
Neither invokes `scala-cli test tools`. So the suite runs only when a human or an agent runs it
locally, and whether this failure appears depends on the contributor's scala-cli version — there is no
gate that would have turned it red for everyone at once.

It is also **default-mode only**, and that is now measured rather than inferred. In parity mode
(`-Dtt.native.bin=<binary>`), `run` invokes the binary directly (`cli.test.scala:54-56`) and
scala-cli never runs, so no advisory reaches stderr and `2454` passes. Confirmed on 2026-08-29 by a
`deploy/buildnative.sc` run, whose parity stage runs the whole suite against the freshly built native
dispatcher: **`CliSuite` 0 failed, 292 total, 26.0s** — against **1 failed, 292 total, 865.7s** for
the same suite through scala-cli, same day, same machine.

That asymmetry makes the gap harder to see rather than easier. The suite's only exhaustive stderr
assertion **passes on the path that ships and fails on the path a contributor uses**: a release build
is parity-proven green, while a fresh checkout's first `scala-cli test tools` is red — and with no CI
gate (below), the green one is the run that gets performed deliberately.

## How to reproduce it

```bash
# 1. The advisory, on a tool nobody has modified, WARM cache, stderr only:
scala-cli run tools/json.scala -- get some.json name 2>&1 1>/dev/null
#    => [warn]  Using directives detected in multiple files:
#       - tools/json.scala:1:1-3:30

# 2. It is not that tool — any tool does it, because they all carry the include:
scala-cli run tools/files.scala -- --help 2>&1 1>/dev/null

# 3. The assertion it breaks is the ONLY exhaustive stderr check in the suite:
tt text count tools/test/cli.test.scala '\._3'                # => 0
tt text match tools/test/cli.test.scala 'assertEquals\(run'   # => :2454 is the only whole-tuple compare

# 4. The suite:
scala-cli test tools
#    => CliSuite: 1 failed, 0 ignored, 292 total
#       ==> X CliSuite.json get: dot path with a numeric array index prints the scalar unquoted
#       munit.ComparisonFailException: cli.test.scala:2454

# 5. The advisory is suppressible, and with a warm unit stderr goes to EXACTLY empty:
scala-cli run --suppress-directives-in-multiple-files-warning tools/json.scala -- get some.json name
#    => first run: stderr still carries "Compiling project (...)" (the flag re-hashed the build unit)
#    => second run: stderr is 0 bytes
```

Measured 2026-08-29 on Linux, scala-cli 1.15.0, Scala 3.9.0-RC4: full suite run, one failure of 292,
at `cli.test.scala:2454`. Steps 1, 2 and 5 were run and their output is quoted above. Step 1 was also
reproduced against a **pristine tree** extracted with `git archive HEAD tools` at `c51a728`, where
`json.scala` and `cli.test.scala` are byte-identical to `main` — so the failure predates and is
independent of any working change.

The parity counterpart was measured the same day, and is the cleanest single contrast:

```bash
# 6. the same suite, same machine, against a freshly built native dispatcher:
scala-cli run deploy/buildnative.sc      # its parity stage sets -Dtt.native.bin and runs the suite
#    => CliSuite: 0 failed, 0 ignored, 292 total   26.0s     (parity, no scala-cli in the loop)
#    vs CliSuite: 1 failed, 0 ignored, 292 total  865.7s     (default, per-file scala-cli)
```

## Acceptance sketch

* **Keep an exhaustive stderr assertion. Do not trade it for green.** Whatever shape the fix takes,
  the suite must still be able to fail when a tool writes something unexpected to stderr on a success
  path. Weakening `2454` to `._2` is the one outcome to avoid, and it is the likely one if this is
  triaged as "flaky test".
* **Filter the toolchain's noise at the one capture point**, beside `normalizeEol` in `run` — a small
  named list of build-tool lines (the advisory, `Compiling project`, `Compiled project`) stripped from
  `err` before it is returned, so that `assertEquals(..., "")` recovers its real meaning: *the tool*
  wrote nothing. This is the option that also covers the cold-build case, which the suppression flag
  does not. If scala-cli's wording later changes, the filter stops matching and the test goes red —
  the safe direction to fail in.
* **Or pass `--suppress-directives-in-multiple-files-warning`** in `run`'s scala-cli invocation.
  Verified above to silence the advisory; verified also to be insufficient alone, because cold builds
  still put `Compiling project` on stderr. Cheap, and composes with the filter.
* **Then widen it.** One exhaustive stderr check over 46 verbs is a floor, not a total. "No verb
  writes anything unexpected to stderr on its success path" is a sweep of the same shape as the
  `--help` sweep, and would pair naturally with issue 041's projection work now that a per-verb
  declaration exists to iterate.
* **Recorded as an observation, not a proposal here:** nothing gates the suite in CI. Whether that
  should change, and whether it belongs in this issue or its own, is a maintainer call — it is noted
  because it is the reason this sat unreported, not because this issue asks for it.
* **Out of scope:** changing the per-tool `//> using file project.scala` convention. That include is
  load-bearing (`project.scala:10-14`, `ScalaVersionSuite`), and taking scala-cli's advice would break
  the launcher's single-file fallback. The advisory is wrong for this repo, not a finding about it.

## Discussion

### Comment by hmiddelk at 2026-08-29 15:07

Found while running the full suite to verify issue 041's fix (PR #14), which is why that PR's body
flags it: a reviewer running the suite will meet one red test that has nothing to do with that change,
and would reasonably suspect the change. Filed separately rather than folded into that PR, because the
interesting half is not the red line — it is that the repair anyone would reach for first deletes the
suite's only exhaustive stderr assertion, and nothing would report the loss.

Agent disclosure: drafted by an AI agent (Claude Opus 5) in session with me, from a failure it hit
while verifying its own change, and reviewed by me. The agent verified BY RUNNING: the advisory on
`json.scala` and on `files.scala` with a warm cache; the full suite (1 failure of 292, at
`cli.test.scala:2454`, with the obtained-vs-expected diff read); the same advisory from a pristine tree
extracted at `c51a728`; that `--suppress-directives-in-multiple-files-warning` exists and silences it,
and that a warm run under that flag leaves stderr at exactly 0 bytes while the first run still carries
`Compiling project`; the stderr-assertion census (39 `contains` bindings, zero `._3`, `2454` the only
whole-tuple comparison); and that neither workflow in `.github/workflows/` runs the suite. NOT
verified: that parity mode passes (read from `cli.test.scala:54-56`; no native binary was built);
anything on macOS or Windows; and anything against scala-cli 1.16.0, which this machine reports as
available but which was not installed or tested.

### Comment by hmiddelk at 2026-08-29 15:37 — parity mode confirmed by measurement

The one claim the comment above labels NOT verified is now verified, and it came for free: I ran
`scala-cli run deploy/buildnative.sc` for unrelated reasons, and its parity stage runs the whole suite
against the binary it just built. Result, same machine and day as the failing run:

```
CliSuite: 0 failed, 0 ignored, 292 total   25.973s    (parity, -Dtt.native.bin set)
CliSuite: 1 failed, 0 ignored, 292 total  865.748s    (default, per-file scala-cli)
```

So `cli.test.scala:2454` passes with the native dispatcher and fails through scala-cli, which is what
the code path predicted: parity mode never invokes scala-cli, so the advisory is never emitted. The
Description is updated accordingly; the earlier comment's "NOT verified" line is left as written,
because the append-only rule means the record should show when the claim was still an inference.

Two things this measurement does **not** establish, stated so the upgrade is not read as wider than it
is. First, the parity run used `-Dtt.tools=<root>/tools` against the main checkout on the issue-040
fix branch, not a pristine `main` — but that branch touches neither `json.scala` nor `cli.test.scala`,
so the contrast holds for this assertion. Second, that tree predates PR #14, so `AbilitySuite` was not
in it and is not covered by this parity result.

The asymmetry is the part worth keeping: the suite's only exhaustive stderr assertion is green on the
path that ships and red on the path a contributor runs. Combined with there being no CI gate, the
green run is the one performed deliberately (a release build) and the red one is the one performed by
whoever just cloned the repo — which is the wrong way round for a signal that is supposed to protect
the release.

Agent disclosure: this comment was drafted by an AI agent (Claude Opus 5) from output of a command I
ran myself, and reviewed by me. The numbers are quoted from that run's console output; the agent ran
nothing new for this comment.

### Comment by hmiddelk at 2026-08-29 18:32 — correcting "for unrelated reasons"

The comment above says I ran `scala-cli run deploy/buildnative.sc` *for unrelated reasons*, which
reads as though I ran it arbitrarily. There was a reason, and it was named in the same session: the
agent reported that the `tt` on my PATH was a **stale install** — its usage listed no `issue` verb,
and `tt --version` answered `tt: no such tool '--version'`, so the binary predated both. The
documented refresh for exactly that state is `scala-cli run deploy/buildnative.sc` (`tools/README.md`,
`docs/native.md`), so I ran it. The parity numbers then arrived as a side effect of its parity stage.

Unrelated to *this issue's subject*, then, but not unmotivated: the chain is stale binary → agent says
so → documented rebuild → parity stage → the measurement. Worth having straight, because it changes
what the measurement is evidence of. It did not fall out of a coincidence; it fell out of the ordinary
staleness-and-refresh lifecycle, which is a path contributors are on routinely — the same population
this issue argues meets the red test. That the release-shaped run is the one that happens to be green
is therefore even less reassuring than the comment above makes it: here it happened only because a
stale carrier forced a rebuild.

It also explains a choice already visible in the preamble: this issue states its baseline as v0.10.2
*from `VERSION.txt`* rather than from `tt --version`, per `reqts/issues/README.md`, because at filing
time `tt --version` could not answer. A carrier that cannot state its own age is issue 036's subject.

Nothing else changes: the numbers, the assertion census and the acceptance sketch stand as written,
and the 15:37 wording is left in place under the append-only rule.

Agent disclosure: drafted by an AI agent (Claude Opus 5) in session with me, from my correction and
from the earlier session's transcript, and reviewed by me. The stale-`tt` symptoms quoted above are
that session's recorded command output; the agent ran no new measurement for this comment.

### Comment by hmiddelk at 2026-09-14 — re-measured on `main`, and the cited line has moved

Both halves of the contrast re-run on **`main` at `fb23f71`**, which is 20 commits on from the `c51a728`
this issue was filed against and has PR #13 (issue 040) merged. Same machine, same day, one tree:

```
CliSuite: 0 failed, 0 ignored, 292 total    28.126s    (parity, -Dtt.native.bin set)
CliSuite: 1 failed, 0 ignored, 292 total   768.011s    (default, per-file scala-cli)
```

**This closes the first of the two limits the 15:37 comment attached to its own numbers.** That
measurement was taken on the issue-040 fix branch rather than a pristine `main`, and the comment said so.
The contrast now holds on `main` itself, so "green on the path that ships, red on the path a contributor
runs" is a statement about the mainline and not about one feature branch.

**It does NOT close the second limit.** That comment also noted its tree predated PR #14, so
`AbilitySuite` was not covered. It still is not: #14 is unmerged, `AbilitySuite` does not appear in this
run's log either, and nothing here says anything about it. Stated rather than left to be assumed away.

**The failing assertion has moved: `cli.test.scala:2454` → `:2462`.** The failure this run reports is at
`:2462`, and that line is `assertEquals(run("json", "get", f.toString, "name"), (0, "gs", ""))` — the
same assertion, displaced by edits above it in the 20 intervening commits.

The **census still holds** at the new location, re-run rather than assumed: 68 `assertEquals(run` sites
in the file, `:2462` the only one comparing the whole tuple (every other takes `._1`, `._2` or
`._2.linesIterator`), and still **zero** uses of `._3`. So the finding is unchanged — one exhaustive
stderr assertion across 46 verbs and 292 tests — and only the coordinate is stale.

⚠ The body of this issue still says `:2454` throughout, and that is **left as written on purpose**. The
preamble anchors itself to `c51a728`, where `:2454` was correct; rewriting it to `:2462` would make it
wrong for its own declared baseline. This comment is the bridge for anyone navigating the file today.
The lesson is small but on-topic: a line-number citation is itself a carrier that drifts, which is why
the assertion is identified above by its text as well as its line.

**The timing gap is stable, which is worth knowing before anyone optimises the wrong thing.** 768s
default against 28.1s parity is 27×; the 2026-08-29 pair was 865.7s against 26.0s, or 33×. Both sides
moved a little and the ratio did not meaningfully change, so the default-mode cost is a property of
running 292 tests through per-file scala-cli rather than something that has been degrading.

**Provenance, and it is the same chain as the comment above.** This did not come from anyone setting out
to measure it either. I told the agent the native binary was stale; the documented refresh is
`deploy/buildnative.sc`; its parity stage ran the suite and produced the green half; the red half was
then run deliberately, because a parity figure with no same-tree counterpart is the weaker half of a
contrast. That is now **twice** that this measurement has arrived out of ordinary
staleness-and-refresh maintenance, which is mild support for the 18:32 comment's argument about which
runs actually get performed: the release-shaped run keeps happening, and the contributor-shaped run
happens only when someone goes looking.

Agent disclosure: measured and drafted by an AI agent (Claude Opus 5) in session with me, and reviewed
by me. Verified BY RUNNING, on `fb23f71`: `deploy/buildnative.sc` end to end (build 444s, parity suite
117s, exit 0, binary swapped) for the parity figure; `scala-cli test tools --test-only CliSuite` for the
default figure, with the failure message and `:2462` read from its output; and the `assertEquals(run` /
`._3` census re-run against the current file. NOT verified: `AbilitySuite` under parity (see above);
macOS or Windows; scala-cli 1.16.0, still neither installed nor tested; and whether the advisory's
wording has changed in any newer scala-cli, which is the thing that would make the acceptance sketch's
filter option fail safe.

### Comment by hmiddelk at 2026-09-14 — Phase 1 adds 12 verb invocations and no exhaustive stderr assertion

Bearing on the sketch's last two bullets, and on the order they have to be done in.

⚠ Every `ability.test.scala` line cited below is on **PR #14's branch at `8c51200`**, not on `main` —
the file does not exist here until that PR merges, and its line numbers may shift before it does. Each
is therefore named by its text as well as its number, for the reason the comment above had to record
(`:2454` moved to `:2462` in 20 commits). The measured stderr figure is independent of #14 and
reproducible on `main` today.

PR #14 adds `AbilitySuite`: 6 tests, 12 verb invocations, and a `run` helper that already returns
`(exit, stdout, stderr)` (`ability.test.scala:49`). It contains **zero** whole-tuple comparisons against
`run` and **zero** uses of `._3` — it only ever reads the exit code and stdout. So the suite still holds
exactly **one** exhaustive stderr assertion, and the "keep it, do not trade it for green" bullet still
has exactly one thing to protect.

**"Then widen it" is now cheaper than when it was written.** That bullet hoped a per-verb sweep would
"pair naturally with issue 041's projection work now that a per-verb declaration exists to iterate."
Three of the four pieces such a sweep needs now exist: `ProjectedAbilities.all` is the iteration source,
`AbilitySuite` is a host that already loops over those verbs on two paths, and its `run` already
captures stderr. What is missing is the assertion itself.

**⚠ But it cannot go in first, and this is the part worth acting on.** `AbilitySuite` is green today
*because* it ignores stderr, not because the stderr is clean. In default mode its `run` shells out to
`scala-cli run tools --main-class dispatchTypedTools` (`:47`), and that invocation writes **4,843 bytes**
of the advisory to stderr — measured directly on 2026-09-14, listing 86 files, the whole-directory form
of exactly the noise this issue is about:

```bash
scala-cli run tools --main-class dispatchTypedTools -- harden --help 2>/tmp/err 1>/dev/null
wc -c < /tmp/err     # => 4843
```

So adding an exhaustive stderr sweep over the projected verbs **today** would not extend the one
assertion to six — it would produce six failures with the same unactionable cause, and twelve once both
description paths are asserted. The sweep multiplies this issue instead of widening coverage.

That gives the sketch a dependency it does not currently state: **the capture-point filter has to land
before the sweep.** Filter the toolchain's lines out of `err` inside `run` first, so that "stderr is
empty" recovers its meaning of *the tool wrote nothing*; only then is iterating it over 6, and later 46,
verbs a coverage win rather than a red-test multiplier. Ordering the sketch's third bullet before its
fourth is the concrete next step this comment is arguing for.

Note the filter would then be needed in **two** `run` helpers, not one — `cli.test.scala`'s and
`ability.test.scala`'s — which restates the deliberate duplication `ability.test.scala:26` flags ("test
independence over DRY, scala-style §5"). Worth deciding once, in this issue, rather than twice by
accident.

Agent disclosure: drafted and measured by an AI agent (Claude Opus 5) in session with me, and reviewed
by me. Verified BY RUNNING: the `scala-cli run tools --main-class dispatchTypedTools` invocation above,
with stderr captured to a file and its size read (4,843 bytes, advisory first line). Verified BY
READING: the `assertEquals(run` and `._3` census of `ability.test.scala`, and its `run` helper at
`:41-49`. NOT verified: that a filter written against this wording would survive a scala-cli upgrade —
which the sketch already names as the safe direction to fail in — and nothing about the 40 unprojected
verbs.

### Comment by hmiddelk at 2026-09-15 — "the one capture point" is six helpers in four files, with three different normalisations

Correcting the comment above, and the sketch's third bullet with it. That bullet says to filter the
toolchain's lines "at the one capture point, beside `normalizeEol` in `run`", and my comment above
refined that to two helpers. Both undercount. Enumerated:

| file | helper that returns stderr to a test | stderr normalisation |
| --- | --- | --- |
| `cli.test.scala` | `run` (`:53`), `runStdin` (`:75`), `runIn` (`:882`) | `normalizeEol` (`:37`) |
| `session.test.scala` | `run` (`:127`) delegating to `runStdin` (`:128`) | `norm` (`:135`) |
| `dispatch.test.scala` | `runDispatcher` (`:63`), returning a raw `os.CommandResult` that callers read with `r.err.text()` (`:88`) | **none** |
| `ability.test.scala` | `run` (`:42`) | inline `.replace("\r\n", "\n").trim` (`:49`) |

**Six helper functions, four files, three distinct normalisation implementations, and one file with no
normalisation at all.** Each was written independently; `cli.test.scala`'s three are three separate
functions in one file, so even a filter placed in that file's `run` would leave `runStdin` and `runIn`
untouched.

Why this matters for the fix rather than being trivia: a filter added at one of these six sites
**relocates the blind spot instead of closing it.** Any exhaustive stderr assertion written through an
unfiltered helper stays red for the same unactionable cause the issue is about, and — worse for a
reviewer — would look like a fresh defect in whichever tool the test happens to name, which is precisely
how `cli.test.scala:2462` misattributes today.

Three consequences for whoever implements the third bullet:

* **`dispatch.test.scala` is the sharp edge.** It hands back a raw `os.CommandResult`, so there is no
  single place inside it to put a filter — the stderr text is read at the call site. Filtering it means
  either changing its return type or filtering at each reader. That is a small design decision, and it
  is invisible if the count is believed to be one.
* **A sweep must not be written against `ability.test.scala` first.** The comment above argues the
  filter has to precede the per-verb sweep; this adds that the sweep's intended host is the helper with
  the weakest normalisation of the four (an inline trim, no shared function), so it needs the filter
  built rather than borrowed.
* **Whether the filter is duplicated or shared is a decision for this issue.** The four helpers are
  deliberately independent — `ability.test.scala:26` says so in as many words about its own restated
  logic, "test independence over DRY, scala-style §5". Under that policy the filter becomes four to six
  copies of a list of scala-cli's log lines, each able to drift from the others; the alternative is one
  shared helper, which is a stated exception to the policy rather than an oversight. Better settled once
  here than six times by accident, and it is exactly the carrier-drift shape issues 041 and 055 are
  about — a filter list copied six times is a manifest.

⚠ `ability.test.scala` does not exist on `main`; its citations are on **PR #14's branch at `8c51200`**
and may shift before that merges. The other three files are byte-identical on `main` and on that branch
(`git diff origin/main...8c51200 --name-only` lists neither `cli.test.scala`, `session.test.scala` nor
`dispatch.test.scala`), so those line numbers hold here.

Agent disclosure: found and drafted by an AI agent (Claude Opus 5) in session with me, and reviewed by
me. It arose from my asking why the rebuild ritual runs the whole suite when only four test files exec
the candidate binary; the agent's answer named those four, and enumerating their stderr handling is what
produced the count above. Verified BY READING each cited line in all four files, and by confirming with
`git diff --name-only` which of them PR #14 modifies. NOT verified: that six is the total — the
enumeration covers helpers that hand stderr back to a test, and a test reading stderr by some other
route would not appear, so six is a floor of the same kind this issue's "exactly one" was measured as.
