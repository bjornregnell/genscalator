# RT058 — When a test result depends on something not in the repo: the issue-050 split, and what it cost to see it

- **Question:** two people ran the same test suite on the same commit and got opposite results. Nothing
  in the code, the commit, or either report explained the difference. What *were* the inputs that
  decided the outcome, why was none of them visible, and what is the general shape of that failure?
- **Why it matters:** genscalator's whole safety argument rests on checks being trustworthy. A check
  whose result depends on unrecorded environment is not a check — it is a coin flip with a plausible
  explanation attached. This note also records a near-miss: the obvious repair for the symptom would
  have **deleted the only test in the suite capable of catching a whole class of defect**.
- **Status:** shipped (filter + banner merged or in review; see *What shipped*).
- **Audience:** written to be readable by someone who was not there. The arc runs across issues 050,
  061, 062, 065, 066 and `261011-which-merged-usr-duplicates`, and that is hard to reconstruct from the
  issue files alone, because each one is correct about its own slice and none tells the story.

---

## Background: the two output channels

Every `tt` verb is a separate program. `CliSuite` tests one by **running it** and checking three
things: the exit code, what it printed to **stdout** (what the tool means to say), and what it printed
to **stderr** (where errors and warnings go).

That last channel is where the trouble lives.

## The defect, and the trap underneath it

Every tool file begins with a directive meaning *"use the shared settings file"*. That is deliberate
and load-bearing: it is what makes a single-file run agree with a whole-directory build. `scala-cli`
disapproves, and says so on **stderr**, every run:

```
[warn] Using directives detected in multiple files: …
```

The advice cannot be taken — removing those lines breaks how tools are launched — so the complaint is
permanent and unactionable.

Of ~292 tests, **291 checked stderr loosely** (*does it contain this message?*). Extra noise does not
disturb a loose check. **Exactly one checked stderr strictly** — *must be exactly empty* — and that one
failed, in a test about JSON parsing, which had nothing to do with anything.

**The trap:** the obvious repair is to loosen that one strict assertion to match its 291 neighbours.
Everything goes green. It also deletes the only check across 46 verbs able to notice *a verb that
starts printing junk on a success path*. One accidental assertion guarding a whole class of defect,
and the cheapest fix removes it. That, not the red test, is what issue 050 was filed about.

## The split: same commit, opposite results

The maintainer ran the suite and got **green**, and proposed closing the issue as fixed upstream in a
newer `scala-cli`. The contributor ran it and got **red**, upgraded to the same version, and still got
red — so the premise was wrong, and the question became *why do two careful people disagree?*

Two inputs decided it, and **neither is in the repository**:

1. **A global `scala-cli` config key** — `suppress-warning.directives-in-multiple-files` — set once on
   the maintainer's machine years earlier and forgotten. It silences the advisory before it is ever
   written.
2. **Which `scala-cli` the shell finds.** Both machines had several installed. The suite says "run
   `scala-cli`" and lets PATH decide.

Both were confirmed on the maintainer's box. His reply: *"You were right, and my measurement was the
misleading one… Issue 050 stays open."*

### The generalisable shape

A version dependency would at least appear in `scala-cli version`, which any bug report quotes. These
two appear **nowhere**: not in the checkout, not in the version string, not in anything a reporter
would think to include. Two people, same code, different answers, and no artifact anywhere that
explains the difference.

That is the reusable lesson: **when a result depends on the environment, the environment must be part
of the result.**

## What was built

**The filter (issue 050).** Rather than weaken the strict assertion, the suite now strips the
*toolchain's own* lines from captured stderr at the capture point, so `stderr == ""` recovers its real
meaning — *the tool wrote nothing*. The strict check survives intact and the suite went green in
default mode for the first time.

Two design points worth keeping:

- It is applied at **every** capture point (six helpers across four test files), because a filter at
  one site relocates the blind spot instead of closing it.
- It **fails toward red**: patterns are narrow and anchored, so a reworded advisory stops matching,
  survives into `err`, and the assertion goes red rather than quiet. A filter that silently widened
  would re-open the gap it exists to close.
- The suppression *flag* was deliberately **not** used instead. An unknown CLI option is a hard error,
  so pinning the suite to a flag reintroduces a toolchain-version dependency — the very axis the issue
  is about.

**The banner (issue 061).** If the problem is invisible inputs, print them. `CliSuite` now announces
which `scala-cli` it resolved, what kind of file it is, its version, the state of that config key, and
restates the runner next to the elapsed time at the end.

It earned its place on the first run. The maintainer had guessed a 3.5× speed difference came from him
having a native binary and the contributor perhaps a wrapper script. The banner printed `ELF binary
(64-bit) 139.6M`; his own `tt which` output said `ELF binary (64-bit) 139.6M`. **Identical.** The
hypothesis died to one line of ordinary output that neither party had during the entire argument.

## The timing decomposition, and what it says about the suite

With the runner recorded, the speed gap decomposed:

| | per invocation | × ~350 spawns | suite total |
| --- | --- | --- | --- |
| contributor's machine | 2.15s | ~752s | 752.8s measured |
| maintainer's machine | 0.50s | ~175s | 212.6s measured |
| **parity mode** (native binary) | ~0.08s implied | — | **28.1s / 10.4s** |

`CliSuite` launches a tool as a fresh process per assertion, so **its runtime is almost entirely the
cost of starting a program, ~350 times over.** It barely measures the tools at all.

Three candidate explanations for the machine gap were tested and none survived: not the scala-cli
version (1.15.0 and 1.17.1 both ~780s on one box), not the launcher kind (both native ELF, both
~139M), not a missing build server (a Bloop daemon was live and invocations still cost 2.15s). What
remains is per-invocation startup differing ~3.4× between machines — hardware and environment, not a
repo defect.

⚠ **Stated honestly:** the contributor's arithmetic closed exactly (2.15 × 350 = 752.5 vs 752.8
measured); the maintainer's left ~17% unexplained. So per-invocation startup is the **dominant** term,
not the sole one.

## Parity mode, and why an agent should care

genscalator ships a **native binary**: all 46 verbs compiled into one executable. *Parity mode* points
the tests at that binary instead of at the compiler — "parity" meaning *does the shipped artifact behave
identically to running from source?*

- **normal mode** — `scala-cli` compiles and launches each tool fresh. ~2.15s per call here.
- **parity mode** — the pre-built binary launches directly. Milliseconds.

For an agent this is not a test-suite detail, it is the cost of every tool call it makes. A toolbox
that answers in milliseconds gets reached for; one that takes two seconds per call gets avoided, and an
agent that avoids the typed tool falls back to raw `grep` — exactly the behaviour the toolbox exists to
prevent. This is why the launcher prints `native binary is stale … using scala-cli: SLOWER, still
correct` rather than silently taking the slow road: the staleness is worth surfacing because it changes
what the agent will do.

It also means **cross-machine suite timings were never informative**, since both numbers are dominated
by a cost the suite does not intend to measure. Routine full-suite runs belong in parity mode; the
13-minute default-mode run is worth it only when the `scala-cli` path itself is under test.

## Side findings the arc produced

- **issue 062** — the structural fix for nested checkouts also skips **submodules** from the inventory,
  so a link *into* a submodule reads as dangling. A regression introduced by the fix, unreachable in
  this repo (no `.gitmodules`), reachable in any consumer repo.
- **issue 065** — the UTF-8 latin1 regression test failed once and passed once on identical code.
  Filed rather than dismissed, because the flake's signature is *indistinguishable from the bug's*,
  which makes "just re-run it" the wrong reflex.
- **issue 066** — the native-binary swap does not verify its inputs are unchanged.
- **`261011-which-merged-usr-duplicates`** — on merged-`/usr` systems `/bin` *is* `/usr/bin`, and
  `tt which` compared paths as text, so one file reported as two installs. The banner would have
  printed a phantom "shadowed install" — precisely the two-installs confusion it exists to dispel.
  Fixed by comparing the real file, while *naming* the alias rather than hiding it.

## What shipped

- `ToolchainNoise.strip` in `tools/test/testsupport.test.scala`, applied at all six stderr capture
  points — issue 050, merged.
- The runner/config banner in `cli.test.scala`, and `WhichTool.scanPath` / `realKey` giving `tt which`
  real-file identity — issue 061 and `261011-…`, PR #26.
- Issues 058, 061, 062, 065 filed; issue 050 closed by the filter.

## Findings worth carrying forward

1. **A result that depends on the environment must report the environment.** One line of ordinary
   output settled a disagreement that two rounds of argument could not.
2. **The cheapest repair for a red test is sometimes the deletion of a check.** Ask what the assertion
   *can see* before loosening it.
3. **Prefer a fix that is immune to the axis the issue is about.** The filter is version-independent;
   the suppression flag would have reintroduced a version dependency to fix a version-sensitivity
   problem.
4. **Decompose before theorising.** "Why is the suite slow" was unanswerable; "what does one invocation
   cost, times how many" closed to within 0.3s.
5. **An untested hypothesis, labelled as such, is cheap and useful.** The launcher-kind guess was
   wrong, but naming it is what made it falsifiable — and it was falsified by data already in hand.
