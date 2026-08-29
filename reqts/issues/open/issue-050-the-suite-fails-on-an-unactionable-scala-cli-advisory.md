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

It is also **default-mode only**. In parity mode (`-Dtt.native.bin=<binary>`), `run` invokes the
binary directly (`cli.test.scala:54-56`) and scala-cli never runs, so no advisory is emitted and
`2454` passes. Read from the code, not run: no native binary was built to confirm this.

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
