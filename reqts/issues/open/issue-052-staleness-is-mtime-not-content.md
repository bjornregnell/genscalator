# Issue 052: the native fast path is disarmed by mtime, not by change, so a checkout round trip makes a byte-identical binary "stale"

> status: open 2026-08-29 · labels: toolbox, native, launcher, carrier-staleness, agent-ergonomics ·
> measured against: v0.10.2 (from `tt --version`: git checkout, bash launcher, Linux) at `c51a728`,
> scala-cli 1.15.0, Scala 3.9.0-RC4 · summary: `tools/tt:63` decides staleness with
> `find "$TOOLS" -name '*.scala' -newer "$bin"` — **mtime only, content never consulted**. Git rewrites
> the mtime of every file it changes, so any *excursion and return* (switch branches away and back,
> `git stash` + pop, rebase, bisect, or checking out an older revision to read it) leaves `tools/`
> byte-identical while 61 files now stamp newer than the binary. Measured: `tt session` on the exact
> tree the binary was built from, working tree clean, **4.881 s through the fallback against 0.037 s
> native — ~130×** — plus the issue-050 advisory re-emitted on stderr for every subsequent call. The
> remedy the message offers is a full rebuild ritual (build → 292-test parity → swap), minutes of work
> for a binary that needed nothing.

## Description

**This is not an argument against the conservatism.** `docs/native.md:54-55` states the rule and its
reason: *"Staleness = source mtime newer than binary; conservative by design (any edit disarms the fast
path until a rebuild)"*, and `tools/tt:55-57` says the same — *"a stale binary would silently run old
tool behaviour, so staleness falls back to scala-cli with a stderr note (degrades to slow, never to
wrong)"*. That goal is right and should survive any fix.

The defect is in the **detector**. Mtime is a proxy for "the source changed", and git breaks the proxy
in the false-positive direction: it writes mtimes when it materialises a file, so a file can end up
byte-identical to what the binary was built from while carrying a fresh timestamp. The check
(`tools/tt:63`) cannot tell the two apart:

```bash
if [[ -z "$(find "$TOOLS" -name '*.scala' -newer "$bin" -print -quit)" ]]; then
```

### The trigger is narrower than "any git operation", and still routine

Git only rewrites files whose content differs *at that moment*, so switching between two branches with
identical `tools/` does **not** disarm the fast path — this was measured, not assumed (see the perf-log
evidence below, where a branch switch left the fast path intact). What trips it is an **excursion and
return**: the outbound checkout rewrites the files, the return checkout rewrites them back, and the
content ends where it started while every mtime is new.

That covers a lot of ordinary work: reading an older revision and coming back, `git stash` then pop, a
rebase, a bisect, or — as here — cutting a branch from a stale `main` and recutting it from the right
base. None of these change a line of tool source, and all of them cost the toolbox its fast path until
someone spends minutes rebuilding.

### The cost, measured

Same command, same tree, same machine, one round trip apart:

```
tt session   0.037 s   native fast path            (before any checkout)
tt session   4.881 s   scala-cli fallback          (after; tools/ byte-identical to the build tree)
```

The binary was **content-current throughout**: run directly it answers the `issue` verb that the
user's PATH install lacked (`tmp/tt-native … issue next` → a correct next number), so nothing was
actually out of date.

`tmp/tt-perf.tsv`, which the launcher writes itself, records the transition as it happened:

```
2026-08-29 18:52:03  session  37     ← native, AFTER a branch switch (identical tools/: fast path kept)
2026-08-29 18:55:38  issue    39     ← native
2026-08-29 18:58:04  text     6049   ← fallback, after an excursion to a 103-commit-older revision
```

Typical native calls sit at **34–70 ms**; every call after the round trip pays seconds. In one session
that turned a ~40 ms reflex into a 2–8 s one across dozens of calls.

### Why a false positive is worse than a true one

Two multipliers, both already recorded in this repo.

**The remedy is expensive and unnecessary.** The message names `scala-cli run deploy/buildnative.sc`,
which is the full ritual — build, parity run of the whole 292-test suite, atomic swap. Minutes, to fix
nothing. A human who follows the advice loses that time; one who doesn't keeps paying the fallback.

**The message has already caused a worse outcome than the slowdown.** `tools/tt:66-69` records it
(SM252): read as a warning *about the toolbox*, this line "led an agent to conclude `tt is unusable`
from one slow call and fall back to raw grep for an hour". The wording was then rewritten to lead with
reassurance, which was the right repair for the wording — but it does not lower the **rate**, and a
detector that fires on checkout round trips fires far more often than one that fires on real edits.
Every occurrence is another chance for that misreading, and the whole point of the toolbox is that an
agent reaches for `tt` instead of raw `grep`.

**It also multiplies unrelated stderr noise.** Every fallback call runs scala-cli, and every per-file
scala-cli run emits the "Using directives detected in multiple files" advisory of issue 050. So one
false staleness verdict decorates every subsequent command with a second warning the repo has already
decided it cannot act on.

## How to reproduce it

No rebuild required — a marker file stands in for the binary's timestamp:

```bash
# 0. clean tree, on any branch
git status --porcelain                        # => empty

# 1. a reference mtime, standing in for a freshly built binary
touch tmp/marker
find tools -name '*.scala' -newer tmp/marker | wc -l      # => 0   (nothing newer)

# 2. an excursion and return — content ends exactly where it started
git checkout <an-older-revision>
git checkout <your-branch>                    # back where you started
git status --porcelain                        # => empty: byte-identical to step 0

# 3. the detector now says "stale" for 61 files that did not change
find tools -name '*.scala' -newer tmp/marker | wc -l      # => 61
```

Measured 2026-08-29 on Linux, v0.10.2 at `c51a728`, ext4. Steps 0–3 were run — the marker was written
with an editor rather than `touch`, only its mtime matters — and the count went **0 → 61** with
`git status --porcelain` empty on both sides. The timing contrast above was obtained separately, on
the exact branch the binary was built from (`tools/` identical to the build tree, working tree clean),
where `tt session` still reported stale and took 4.881 s.

## Acceptance sketch

* **Keep "degrade to slow, never to wrong". Change what is compared.** Stamp a content hash of
  `tools/**/*.scala` at build time and compare hashes, not timestamps. `deploy/buildnative.sc:186-187`
  is the natural home: it already performs the atomic swap, so it can write the stamp beside the binary
  in the same step. Absent or unreadable stamp ⇒ behave exactly as today, so the safe direction is
  preserved and old binaries keep working.
* **Probably keep mtime as a pre-filter.** Hash comparison on every launch is on the critical path of a
  launcher whose own overhead is ~26 ms (`docs/native.md:54`). The cheap composition: if no file is
  newer, fast path (one `find`, as today); only when mtime says "maybe" pay for the hash. The common
  case is unchanged and the false positive is caught by the second check.
* **Measure before choosing.** Whether hashing 61 files fits the launcher's budget is not something
  this issue establishes, and it decides between "hash always" and "hash only on suspicion". A bash
  implementation may not be the right tool; the dispatcher could expose a verb that answers it.
* **Say which case it is.** A message can then distinguish "your tool source really differs from the
  binary" from "cannot tell, falling back to be safe" — different facts, and only the first is worth a
  rebuild.
* **The rule is documented; the docs move with the code.** `docs/native.md:49` and `:54-55` state the
  mtime rule explicitly, so a change here is a docs change too.
* **Out of scope:** the fallback behaviour and the message wording. Falling back is correct, and SM252
  already tuned the wording (`tools/tt:66-69`) — this issue is about how often it is triggered
  needlessly, not about what it says when it is right.

## Discussion

### Comment by hmiddelk at 2026-08-29 19:07

Found by tripping it, and then by getting the diagnosis wrong first, which is worth recording because
it is the same trap a maintainer would meet. I had rebuilt the binary at 15:25 in another terminal. An
agent session later reported the toolbox as stale and advised me to rebuild — a second full ritual —
when the binary was current and the verdict was an artefact of the agent's own branch switching.

The agent's first causal story was also too strong: it said "any branch switch trips it". The perf log
the launcher writes itself refuted that — calls at 18:52–18:55 ran native at 34–70 ms *after* a branch
switch, because that switch changed no surviving `tools/` file. The fallback began only after an
excursion to a 103-commit-older revision and back. The narrower claim is the one in the Description,
and it is the interesting one: the trigger is a round trip, not a switch.

Agent disclosure: drafted by an AI agent (Claude Opus 5) in session with me, from a defect it caused
itself and then mis-diagnosed, and reviewed by me. The agent verified BY RUNNING: the 0 → 61 marker
experiment with `git status --porcelain` empty on both sides; the 4.881 s vs 0.037 s contrast on the
build-tree branch; that the binary is content-current (running it directly answers the `issue` verb);
and the perf-log timeline quoted above. It read `tools/tt:53-74`, `docs/native.md:49,54-55` and
`deploy/buildnative.sc:186-187`. NOT verified: that hashing fits the launcher's time budget (the
measurement the sketch asks for, deliberately not guessed); that `git stash`, rebase and bisect trip it
(reasoned from the same mechanism — only the checkout round trip was run); anything on macOS or
Windows, where git's mtime behaviour and filesystem timestamp granularity may differ; and anything
about a native *install* rather than a checkout, which has no `tools/` directory
(`tools/tt:36-37`) and so cannot reach this check at all.
