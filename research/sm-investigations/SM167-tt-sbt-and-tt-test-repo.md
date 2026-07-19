# SM167 — `tt sbt --repo` and `tt test --repo`: directory-as-argument build invocation

**Status: DESIGN (agent-drafted AFK 2026-07-19).** No build here — spec to react to. Wiring and the
allowlist line are BR-gated.

## Why (the gap, grounded)

Two dated specimens, both 2026-07-19:

1. Compiling introprog needed `env --chdir=<introprog> sbt --client <task>` because bare `cd` is
   guard-blocked and sbt keys on cwd — BR pinned SM167 from inside that guard-stall.
2. Running the toolbox test suite needed `env --chdir=<genscalator> scala-cli test tools` TWICE, guard-stalling
   BR both times (PB SM167 ADDENDUM). Honest classification from the pin: **not agent drift — a tool gap.** No
   `tt` shape exists for build/test invocation, so the raw shape gets improvised repeatedly.

The principle is the `tt git --repo` precedent: **dir-as-arg beats cd.** A bare, uniform, allowlistable
command shape with no `env`/`cd` tricks, cwd-independent, statically analyzable.

There is also an **auditability motive** (meta-minion pushes 5–7, recurring finding): every "suite green
N tests" claim in the substrate is prose self-report — no committed test log exists. A `tt test` with a
durable `--out` log turns test evidence from narration into substrate, closing that gap structurally.

## Interface (sketch)

```
tt test <repo-abs> [--out <log-relpath>] [--args <passthrough>...]
tt sbt  <repo-abs> <task> [--out <log-relpath>]
```

- `tt test` runs the repo's test command (for genscalator: `scala-cli test tools`; make the command
  discoverable/configurable per repo — candidate: a one-line `test-command` entry in a repo-local config —
  rather than hardwired).
- `tt sbt` runs one named sbt task via `sbt --client` semantics in the given repo.
- `--out` defaults ON, to a timestamped file under the repo's `tmp/` (e.g. `tmp/test-2026-07-19-2330.log`) —
  the SM152 generalized principle: durable logs in-repo, not harness /tmp. Print the tail summary (suites,
  tests, failed) to stdout; the full log lives in the file, referencable by commit messages and auditable by
  the meta-minion.

## Honest bound (say it in the tool's help)

sbt tasks and test suites are **arbitrary code**. This wrapper bounds the **invocation shape only**, not the
semantics — weaker than `tt git`'s curated verb subset. The value is: no `cd`, no `env --chdir`, one bare
allowlist-matchable literal, plus durable evidence via `--out`. It does NOT make running tests "safe"; the
guard/allowlist decision for `tt test *` is a separate, explicit BR call (same fork as SM147's permission
section: `ask` vs narrow allow).

## Family and cousins

- SM160 typed-system-surface family (`tt box` etc.).
- Consider a general `tt run --dir <abs> <cmd...>` cousin — deliberately NOT specced here: it would be an
  arbitrary-command escape hatch wearing a tt costume, which the allowlist should not bless. Keep the verbs
  named and enumerable (`test`, `sbt`), one honest bound each.
- Baton rule (already ratified in the SM167 addendum): batons carry "no raw env/build-tool invocations; if
  no tt shape exists, FLAG the gap instead of improvising the shape repeatedly."

## Ties
SM160 (family) · SM152 (`--out` durable-log principle) · SM146c/T3 (`tt bloop restart` — the wedge these
invocations hit) · meta-minion pushes 5–7 (test-evidence auditability) · [[commit-via-tt-git-not-raw-cd-git]].
