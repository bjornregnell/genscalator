# Issue 066: the rebuild ritual swaps a binary without checking its inputs are still the ones it built from, so a `tools/` change during the build silently ships a stale binary

> status: open 2026-09-23 · labels: toolbox, native, deploy, guard-integrity, silent-wrong · measured
> against: v0.10.2 (from `VERSION.txt`) at `2c625ca`, Scala 3.9.0, Linux · summary:
> `deploy/buildnative.sc` builds a candidate, runs the full suite through it, and swaps on exit 0
> (`:186-187`). Nothing re-checks, at the swap, that `tools/**.scala` is still what the build read.
> If those sources change while the build runs — a merge landing, or anyone editing a tool — the swap
> installs a binary the launcher will immediately call stale (`tools/tt:63`), and the only signal is a
> warning on the next `tt` call. Observed 2026-09-23: a PR touching `tools/links.scala` merged during a
> rebuild, the swap reported SWAPPED, and the next `tt` call printed the staleness warning. Cost: one
> wasted 106-second build, and a window in which the "parity-proven" binary was not.

## Description

The ritual is three steps and the discipline is already right: build a candidate, run the **whole**
suite through it, and swap only on exit 0. `deploy/buildnative.sc`:

* `:172-175` deletes any old candidate and builds the new one
* `:182-184` runs the suite with `-Dtt.native.bin=<candidate>`
* `:186-187` `Files.move(nextBin, liveBin, ATOMIC_MOVE, REPLACE_EXISTING)`
* `:196` prints `SWAPPED : … now IS the parity-proven candidate`

The swap is atomic with respect to *readers* of the binary. It is not guarded with respect to
*writers of the sources*. Between `:175` and `:187` there is a window of roughly 100 seconds plus the
suite duration, and nothing observes whether `tools/` moved inside it.

**The claim printed at `:196` is what goes wrong.** "Parity-proven" is true of the tree as it was at
build start. After a source change it is no longer true of the tree on disk, and the line says it
unconditionally.

### The launcher already computes exactly the predicate that is missing

`tools/tt:63`:

```bash
if [[ -z "$(find "$TOOLS" -name '*.scala' -newer "$bin" -print -quit)" ]]; then
```

and `:55` states why it exists: *"a stale binary would silently run old tool behaviour, so staleness"*
matters. On the failing side it prints (`:70`):

```
tt: native binary is stale (tools/ edited since build) - using scala-cli: SLOWER, still correct.
```

So the project already has one definition of "is this binary fresh". The rebuild ritual does not
consult it at the moment it declares a binary good.

### Observed instance, 2026-09-23

A `tools/`-touching PR (issue 053's links fix) was merged while `buildnative.sc` was mid-build. The
build completed exit 0, the full suite passed through the candidate, and the swap reported SWAPPED.
The very next `tt` call printed the staleness warning, because `tools/links.scala` was now newer than
the binary just installed.

Two things worth separating. The binary was **not wrong** — it was a correct build of an older tree,
and the launcher's fallback kept behaviour correct. What was wrong is that **the ritual reported
success for a state that no longer held**, and the discrepancy surfaced only by accident on the next
unrelated command.

A second, quieter instance in the same session: `LinksSuite` reported **30** tests in that build and
**33** immediately afterwards on the merged tree. The suite count in the verdict described the old
tree too, which is how the staleness was first noticed.

### Why this is worth a number rather than a note

**Because the note already exists and did not cover it.** `docs/EMBER-TEMPLATE.md:117-122` already
classifies binary staleness as a guard-integrity risk and instructs "rebuild BEFORE a long batch".
That instruction was followed. The gap is not "forgot to rebuild", it is "rebuilt correctly, and then
the inputs moved" — a case the prose does not describe, so adding more prose to the same paragraph is
the weakest available fix. (A one-line pointer is being added there anyway, as a stopgap until this
lands, because the ember is what a cold start actually reads.)

**Because the swap is the only place it can be caught.** A local git hook cannot see a merge performed
on the forge. CI does not build the native binary outside a release. Nothing between the merge and the
next `tt` call is in a position to notice.

**Because it is the repository's recurring shape.** A check that exists but is not consulted at the
decisive point. Issue 050's advisory reached an assertion because no capture point filtered it; the
override-orphan gate runs before the cache gate and hid its number for three days; and here the
freshness predicate exists in the launcher and is absent from the swap.

## How to reproduce it

```bash
# 1. start the ritual
scala-cli run deploy/buildnative.sc -- --root <repo>

# 2. while it runs, change any tool source (a merge does this too)
#    e.g. touch tools/links.scala

# 3. the ritual still reports success
#    => buildnative: VERDICT ... SWAPPED : ... now IS the parity-proven candidate

# 4. the very next tt call disagrees
tt gitinfo <repo>
#    => tt: native binary is stale (tools/ edited since build) - using scala-cli: ...
```

Observed on Linux at `2c625ca` by the merge described above, not by the `touch` shortcut; step 2 is
written as `touch` because it is the minimal trigger, and that form is NOT the one that was run.

## Acceptance sketch

* **Reuse the launcher's predicate; do not invent a second one.** The check at the swap should be the
  same comparison `tools/tt:63` makes: is any `tools/**.scala` newer than the binary about to become
  live. An earlier draft of this proposal suggested hashing the input files instead, on the grounds
  that mtimes are fragile. That is now judged **wrong for this case**: a content hash would introduce a
  second, competing definition of freshness, and two carriers of one rule drifting apart is the exact
  failure family issues 041, 055 and 061 are about. One predicate, consulted in two places, beats two
  predicates that agree today.
* **Fail closed, and no override flag.** If the sources moved, refuse the swap, name the files that
  changed, keep the candidate for inspection (the code already does this for the parity failure at
  `:167`), and say re-run. A `--force` would invite exactly the override that makes a silently-stale
  binary possible, which the ember classifies as guard-integrity rather than convenience. A re-run
  costs about 106 seconds, measured.
* **The false positive is acceptable and should be stated.** A checkout that rewrites mtimes without
  changing content would trigger a needless refusal. That is the safe direction to fail, and it is
  cheaper than the alternative of a wrong SWAPPED line.
* **Correct the verdict's wording too.** `:196` asserts the binary "IS the parity-proven candidate".
  With this check in place that becomes true again; without it the line should not be unconditional.
  Worth also printing the tree state the build read, so the verdict describes a specific input rather
  than an implied present tense — the same argument issue 061 makes for printing the resolved
  `scala-cli` and the tools directory.
* **Interaction with issue 052, which is the reason the "reuse one predicate" choice is right.**
  052 is *"staleness is mtime not content"* — it proposes changing how `tools/tt:63` decides. If this
  issue reuses that predicate rather than defining its own, then whatever 052 settles on, the swap
  guard inherits it for free and the two can never disagree. If instead this issue hashed content
  independently, 052 landing would leave two definitions of freshness in the tree, which is the
  drift family issues 041, 055 and 061 all describe. So 052 does not block 066, and 066 should not
  anticipate 052's answer.
* **Interaction with issue 003**, which proposes lifting the whole build/parity/swap ritual out of
  `deploy/buildnative.sc` into a `tt` verb. This guard belongs to the ritual rather than to the
  script, so it should move with it. Cheaper to add here now and carry it across than to remember it
  during that lift.
* **Out of scope:** preventing the merge. It happens on the forge, not here, and no local mechanism
  can block it. This issue is detection at the swap, which is the only achievable point. Also out of
  scope: making the ritual re-run itself automatically on detection. Refusing loudly is enough, and an
  automatic retry could loop while a long merge sequence lands.

## Discussion

### Comment by bjornregnell at 2026-09-23 23:58

Filed from a live instance rather than from review: I merged a `tools/`-touching PR while a rebuild was
running, and the ritual told me it had installed a parity-proven binary that the next command
immediately called stale.

The framing I first reached for was a discipline rule, "do not merge during a rebuild". I do not think
that is the right shape. Any edit during the build has the same effect, so a merge-specific rule would
miss the ordinary case of someone editing a tool while a build runs, and it would put the burden on
memory rather than on structure. The swap already refuses to install a binary that fails the suite;
refusing to install one whose inputs have moved is the same kind of guard.

Agent disclosure: hit and diagnosed by an AI agent (Claude Opus 5) in session with me, and written by
it; the instance is one it caused and reported rather than one it was asked to look for. Verified BY
READING `deploy/buildnative.sc:167-196` and `tools/tt:55-70`. Verified BY RUNNING the two rebuilds
whose verdicts and differing `LinksSuite` counts (30 then 33) are the evidence above. NOT verified: the
`touch`-based reproduction as written, which is the minimal trigger rather than the observed one; and
whether any `deploy/` script other than `buildnative.sc` has the same unguarded-swap shape.
