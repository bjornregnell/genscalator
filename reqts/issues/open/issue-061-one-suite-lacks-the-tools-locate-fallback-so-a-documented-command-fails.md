# Issue 061: ten suites resolve `tools/` by hand, one lost the fallback, and a command `tools/README.md` documents fails two tests on a clean checkout

> status: open 2026-09-19 · labels: tests, docs, drift, false-positive · measured against: v0.10.2
> (from `VERSION.txt`) at `b410903`, scala-cli 1.15.0, Scala 3.9.0, Linux · summary: `tools/README.md`
> offers `scala-cli test tools` from the repo root as an alternative to the
> `--prop tt.tools=<abs>/tools` form. It is not equivalent: `GitHelpCoverageSuite` fails **2 of 2**
> under it with `cannot locate tools/ (pass -Dtt.tools=<dir>)`. Ten test files resolve `tools/` or the
> repo root from the `tt.tools` property, in **four hand-written variants**; nine fall back to walking
> up from the cwd and `tools/test/git.test.scala:358-359` does not. The lone copy that lost the
> fallback is the drift; the documented command that exercises it is how it reaches a newcomer.

## Description

`tools/README.md:33-34` states two ways to run the suite, as equals:

> Run the whole toolbox + tests with **`tt scala test <abs>/tools --prop tt.tools=<abs>/tools`** (the
> allowlist-clean driver form, correct from any cwd) — or `scala-cli test tools` from the repo root.

The second does not work. On a clean checkout at the repo root:

```
GitHelpCoverageSuite:
  ==> X every verb tt git dispatches has a usage line in its own --help
      java.lang.IllegalStateException: cannot locate tools/ (pass -Dtt.tools=<dir>)
  ==> X the safety paragraph does not deny a verb the tool actually dispatches
      java.lang.IllegalStateException: cannot locate tools/ (pass -Dtt.tools=<dir>)
Test run GitHelpCoverageSuite finished: 2 failed, 0 ignored, 2 total
```

Every other suite in the same run locates `tools/` without help, which is what makes this read as a
defect in the tool under test rather than as a missing flag.

### The census: ten copies, four variants, one without a fallback

The resolution rule is restated in each suite that needs it. Enumerated at `b410903`:

| variant | files | shape |
| --- | --- | --- |
| **A** — `tools/`, validated, walk 8 | `cli.test.scala:18`, `dispatch.test.scala:20`, `ability.test.scala:28`, `scalaversion.test.scala:15` | property → `.filter(os.exists(d / "tt"))` → `Iterator.iterate(os.pwd)(_ / os.up).take(8)` |
| **B** — repo root, validated, walk 8 | `payload.test.scala:28`, `version.test.scala:25`, `parsereqt.test.scala:17`, `reqt-roundtrip.test.scala:29` | as A, then `.map(_ / os.up)` |
| **C** — `tools/`, unvalidated, walk 6 | `session.test.scala:118` | `os.Path(_, os.pwd)` (tolerates a relative value), **no** `.filter`, `take(6)` |
| **D** — `tools/`, unvalidated, **no fallback** | `git.test.scala:358-359` | `sys.props.get("tt.tools").map(os.Path(_)).getOrElse(throw …)` |

> **Baseline note, added on merge.** The preamble declares `b410903`, where `ability.test.scala` did
> not yet exist, so at filing time the census read 9 copies and 8 fallbacks on `main` and the table
> above described `main` plus PR #14's branch. #14 has since merged, so the table now describes `main`
> exactly: **10 copies, 9 with the fallback**, re-measured after the merge. The numbers were right about
> where the repo was going; only the stated baseline lagged.

Variant **D** is the defect. It is the only one of the ten that cannot resolve `tools/` without the
property, and it is also the only one that never checks the property points at a real tools directory —
so a *wrong* `-Dtt.tools` gets past it and fails later inside `os.read` with a worse message than the
clear one it throws when the property is absent entirely.

Variant **C** is not broken but is a third spelling: 6 levels instead of 8, and relative-tolerant where
the others demand absolute. Nobody decided that; it is what independent authorship produces.

The closest thing to a specification is a comment, not code — `cli.test.scala:10-13` describes the
property-then-walk contract for its own file. Nine files implement something close to it and one does
not, with nothing relating them.

### A second unrecorded input: which `scala-cli` the suite actually runs

Found while investigating issue 050, and it belongs here because it is the same shape — an input the
suite resolves from the environment, per file, with nothing recording what it resolved.

`cli.test.scala:33`, `dispatch.test.scala:47` and `session.test.scala:124` each define:

```scala
private val ScalaCli = if isWindows then "scala-cli.bat" else "scala-cli"
```

A bare name, resolved through PATH, and every assertion in those suites is made against subprocesses
spawned that way. **There is nothing wrong with the mechanism** — that is how PATH works, and the
Windows branch is there for a documented reason (`buildnative.sc:154` explains that neither Git Bash
nor `ProcessBuilder` does PATHEXT resolution). The gap is that **nothing records which binary won.**

On this machine there are two, and the suite silently picks one:

```
/usr/local/bin/scala-cli                      1.15.0   (first on PATH)
~/.local/share/coursier/bin/scala-cli         1.17.1
```

Measured consequence, from the issue-050 work: invoking the test harness by absolute path to one
version leaves every asserted subprocess on the *other*, so a run that looks like a version comparison
is not one. The two are indistinguishable from the output, because the output never names either.

**The precedent for the fix is already in the same file, eleven lines away.** `cli.test.scala:93`
prints the resolved tools directory, and `:96-98` refuses to run when it looks wrong, with the
diagnosis spelled out: *"Likely the wrong dir resolved via cwd walk-up — pass `-Dtt.tools=<abs
tools>`."* The suite already believes that a resolved input should be announced and guarded. It does
this for `toolsDir` and says nothing about the binary it is about to execute several hundred times.

### Why this is worth a number

**A documented command fails, and blames the wrong thing.** The two failures name `tt git`'s help
coverage. Someone who just cloned the repo, ran the command the README gave them, and saw two red tests
against a tool they have not touched has every reason to think their checkout is broken or that `main`
is red. The true cause — a missing `--java-prop` — is named in the exception text but not in the
document that told them what to run.

**Nothing gates it.** Issue 050 records that no CI workflow runs the suite: `.github/workflows/` holds
`links-check.yml` and `native-release.yml`, and neither invokes `scala-cli test tools`. The
release-shaped run (`deploy/buildnative.sc`) passes `--java-prop tt.tools=…` explicitly, so the path
that is exercised deliberately is the one that works, and the path a contributor takes is the one that
fails. That is the same asymmetry issue 050 documents for a different reason, in the same suite run.

**The duplication is deliberate, and this is the first measured cost of it.**
`ability.test.scala:26` states the policy in as many words: "Deliberately restates the locate logic
(test independence over DRY, scala-style §5)." That is a real position and this issue does not argue
against it generally. But the project has already made an exception to it, in the same directory, for
the same reason: `tools/test/testsupport.test.scala` holds `object TestFs`, and its docstring explains
that duplicated cleanup made a Windows bug "read as 14 unrelated git failures rather than as one
cleanup bug". Locate is now in precisely that position — one bug, presenting as two failures in an
unrelated suite. The question is whether locate joins `removeAllForce` as a shared helper or stays
restated ten times.

## How to reproduce it

```bash
# 1. the documented alternative, from the repo root — 2 failures
scala-cli test tools --test-only 'GitHelpCoverageSuite'
#    => cannot locate tools/ (pass -Dtt.tools=<dir>)
#    => GitHelpCoverageSuite finished: 2 failed, 0 ignored, 2 total

# 2. the same suite with the property — green
scala-cli test tools --test-only 'GitHelpCoverageSuite' --java-prop tt.tools="$PWD/tools"
#    => GitHelpCoverageSuite finished: 0 failed, 0 ignored, 2 total

# 3. the census: who reads the property, and who falls back
tt text grepr tools/test .scala 'tt.tools'          # => 10 files
tt text grepr tools/test .scala 'Iterator.iterate'  # => 9 files; git.test.scala is absent

# 4. what the README promises
tt text match tools/README.md 'scala-cli test tools'
```

Measured 2026-09-19 on Linux at `b410903`, scala-cli 1.15.0, Scala 3.9.0. Steps 1–4 were run and
their output is quoted or summarised above.

⚠ One measurement that misled and is worth recording, because it will mislead the next person too. A
tree whose `.scala-build` caches were written by Scala 3.9.0-RC4 does not simply recompile after the
3.9.0 bump (`7d7e70b`): it fails with `error while loading` for **every** `@main` in the toolbox,
around forty lines of them, then `Compilation failed`. Nothing in that output names the cache.
`tt bloop clean --dir <abs> --yes` clears it (3 directories, ~0.1G here). That is unrelated to this
issue, but it happened in the same session and produced a failure that also pointed at the wrong
thing.

## Acceptance sketch

* **The one-line repair: give `git.test.scala` the fallback its nine neighbours have**, and the
  `.filter(os.exists(d / "tt"))` guard with it, so a wrong property is rejected at the point it is
  read rather than inside `os.read`. This makes the documented command work and is the minimum.
* **The class question, which is the part worth deciding:** ten hand-written copies of one resolution
  rule, already in four spellings, with no test relating them. A shared `TestFs.toolsDir` /
  `TestFs.repoRoot` would end it, and `object TestFs` already exists for exactly this kind of
  cross-cutting helper — so this is not proposing a new exception to "test independence over DRY", it
  is asking whether locate belongs with `removeAllForce` on the existing exception. The argument that
  it does is that both failures have the same signature: one bug presenting as several failures in
  suites that are not at fault.
* **Or gate it instead of sharing it.** If the duplication should stand, the cheap alternative is a
  meta-test: every file in `tools/test/` that reads `sys.props.get("tt.tools")` must also contain the
  walk-up fallback. That keeps ten copies and makes the tenth impossible to omit. It is weaker than
  sharing — it pins the shape, not the behaviour, and would not have caught variant **C**'s `take(6)`.
* **Fix `tools/README.md:33-34` either way.** Whatever happens to the code, the document currently
  offers a command that does not work. If the fallback lands, the sentence becomes true and can stay;
  if it does not, the bare `scala-cli test tools` form should be removed rather than left as a trap.
  These are separable and the README half should not wait on the design question.
* **Extend the existing banner to name the `scala-cli` it resolved**, and its version. `cli.test.scala:93`
  already prints the tools directory for exactly this reason; adding the binary is one line in a
  `println` the suite already emits, and it costs nothing at runtime. It does not need to *guard*
  anything — the point is only that a run's own output should state the two inputs that determine its
  result, so a pasted failure is interpretable without asking the reporter what their PATH was. This
  is worth doing even if the locate question below is settled the other way, and it is the half that
  matters to issue 050, where a green run and a red run differed on inputs neither output recorded.
* **Out of scope:** the `-Dtt.tools` mechanism itself. Passing the tools directory as an explicit
  argument rather than discovering it is a PRD position (`configInArgsNotEnv`, cited at
  `cli.test.scala:10`), and the fallback exists to make interactive use bearable, not to replace it.
  Nothing here argues against either.

## Discussion

### Comment by hmiddelk at 2026-09-19 13:05

Found by running `scala-cli test tools` while verifying an unrelated branch, getting two failures in a
suite that branch does not touch, and checking whether they were mine. They were not — they reproduce
on `main`.

What makes this more than a missing flag is the census. I expected one file to be missing the fallback
and found that the rule is written out ten times in four different ways, which means the tenth copy
was not *forgotten* so much as never related to the other nine. `tt git`'s help coverage suite is the
one that happens to expose it, and it has nothing to do with `tt git`.

I have not proposed a preference between sharing and gating, because `ability.test.scala:26` states
the no-DRY position deliberately and it is not mine to overturn. But I do think `TestFs` changes the
shape of that decision: the exception already exists, in the same directory, and the docstring that
justifies it describes this exact failure mode one platform over.

Agent disclosure: found and drafted by an AI agent (Claude Opus 5) in session with me, and reviewed by
me. Verified BY RUNNING: the suite with and without `--java-prop tt.tools` (2 failed and 0 failed), and
the two census searches. Verified BY READING: each of the ten locate implementations, to classify them
into the four variants in the table. NOT verified: behaviour on macOS or Windows; whether any suite
outside `tools/test/` resolves the same property; and whether `take(6)` in variant **C** can actually
fail for a real checkout depth, which is stated as an inconsistency rather than as a defect.

### Comment by hmiddelk at 2026-09-19 15:40 — the PATH input, added

Added the second-input section above. It arrived from the issue-050 investigation rather than from
looking for it: while establishing whether scala-cli 1.17.1 changes that issue's failure, a run that
appeared to be a version comparison turned out to have the harness on one version and every asserted
subprocess on the other, because `ScalaCli` is a bare name and PATH decided. Two installs on one
machine, and nothing in the output naming either.

I want to be careful about what is and is not being claimed, because the mechanism is not a bug.
Resolving a command through PATH is how the shell works, the Windows `.bat` branch exists for a
documented reason, and none of the three files is doing anything unusual. The claim is narrower: the
suite's result is a function of inputs the suite does not report, and this is the second of them.
`-Dtt.tools` decides *which tree* it reads and PATH decides *which binary* it executes; the first is
announced at `cli.test.scala:93`, the second is not announced at all.

That is why the suggested fix is a `println` rather than a guard. There is no wrong answer to police —
any scala-cli on PATH is a legitimate thing to test against. What is missing is the record, and the
cost of its absence is measured in issue 050, where a green run and a red run of the same suite on the
same commit differed only on environment that neither run's output described.

Agent disclosure: found by an AI agent (Claude Opus 5) in session with me, as a by-product of the
issue-050 measurements, and reviewed by me. Verified BY READING `cli.test.scala:32-33`, `:93`,
`:96-98`, `dispatch.test.scala:47`, `session.test.scala:124` and `buildnative.sc:154-158`; and BY
RUNNING `which scala-cli` and a version call against both installs on this machine. NOT verified:
whether the two installs actually diverge on any assertion in the suite — the issue-050 runs came out
the same on both, so the exposure here is unrecorded provenance rather than a demonstrated behavioural
difference.

### Comment by hmiddelk at 2026-09-24 12:10 — the banner is implemented, and its first run retired an open hypothesis

`CliSuite` now prints the runner beside the tools directory it already printed, and restates it next
to the elapsed time when the suite ends:

```
[CliSuite] tools dir: /home/hans/genscalator/tools — 46 @main tool files, dispatch table expects 46
[CliSuite] runner:    /usr/local/bin/scala-cli  v1.17.1  ELF binary (64-bit)  139.6M  rwxrwxr-x  2026-09-19T13:21  (+2 shadowed on PATH)
[CliSuite] 752.8s elapsed, through /usr/local/bin/scala-cli  v1.17.1  ELF binary (64-bit)  139.6M  ...
```

Two lines rather than one because they answer different questions: the opening line records the inputs
before anything runs and survives an aborted suite, and the closing line is the pairing BR asked for on
issue 050 — *"recording, next to the timing, which binary won and what kind it is"*. A pasted tail now
carries all three.

Resolution reuses `WhichTool` — `pathDirs`, `hitsFor`, `factsOf` — rather than restating PATH lookup.
The toolbox already owns dir-order-then-PATHEXT resolution, and a second implementation inside a test
would be exactly the drift this issue is about. It is a `println` and not a guard: any scala-cli on
PATH is legitimate to test against, so there is no wrong answer to police.

**It retired an open hypothesis on its first run.** BR suggested the 220s-versus-780s gap between our
machines might be launcher startup — his winner a native binary, mine possibly a script launcher —
and labelled it a guess. The banner prints `ELF binary (64-bit) 139.6M`, and his own `tt which` output
reports `ELF binary (64-bit) 139.6M`. **Same kind, same size.** The hypothesis is dead, and the fact
that kills it is one line of an ordinary run's output that neither of us had during the whole exchange.

### Then the gap decomposed, and the suite turned out to have no mystery in it

Measured on an idle machine, warm, three times: a single `scala-cli run tools/json.scala -- --help`
costs **2.15s** (2.15 / 2.21 / 2.08). `cli.test.scala` holds **347** `run(` / `runStdin(` / `runIn(`
sites, about 344 net of the three helper definitions, with a few inside loops:

| | per invocation | × ~350 | measured |
| --- | --- | --- | --- |
| scala-cli, this machine | **2.15s** | ~752s | **752.8s** |
| scala-cli, BR's machine | ~0.63s implied | — | 220s |
| native binary (parity mode) | ~0.08s implied | — | 28.1s |

2.15 × 350 = 752.5 against a measured 752.8. **The suite's entire wall-clock is per-invocation launcher
startup multiplied by the number of spawns**, and nothing else contributes materially.

So all three candidate explanations for the machine gap are now measured and none survives:

* **not the version** — 1.15.0 and 1.17.1 both land near 780s here
* **not the launcher kind** — both winners are 139.6M native ELF
* **not a missing build server** — a Bloop daemon is running here, warm, and an invocation still costs
  2.15s

What is left is per-invocation scala-cli startup differing ~3.4× between two machines, amplified 350
times. That is a property of the hardware and environment rather than a defect in this repo, and I do
not think it is worth chasing further.

**The consequence worth keeping is sharper than the puzzle.** `CliSuite`'s duration measures *launcher
startup*, not the tools it is testing. Parity mode runs the same 292 tests in 28.1s because the native
binary starts in milliseconds — a 27× difference that dwarfs any machine-to-machine variation. Two
things follow. Comparing suite wall-clock across machines was never going to be informative, because
both numbers are dominated by a cost the suite does not intend to measure. And anyone running the full
suite routinely should be doing it in parity mode; the default-mode run is worth its 13 minutes only
when the scala-cli path itself is what is under test.

Agent disclosure: implemented and measured by an AI agent (Claude Opus 5) in session with me, and
reviewed by me. Verified BY RUNNING: `CliSuite` 292/292 with the banner in place (752.8s); three warm
single invocations; the spawn-site counts; and a check that a Bloop daemon was live. The decomposition
and the conclusion that the gap is not worth chasing are the agent's; BR's launcher-kind hypothesis was
retired against his own posted `tt which` output rather than against anything new from his machine.
