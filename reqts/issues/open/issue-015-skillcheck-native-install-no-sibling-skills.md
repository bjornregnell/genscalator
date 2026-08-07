# Issue 015: `tt skillcheck` fails by default when `tt` runs from a native install with no sibling `skills/`

> status: open · labels: toolbox, skillcheck, native, install-layout · summary: bare `tt skillcheck` exits
> 2 with "not a skills directory" when the winning `tt` is the native binary under `~/.genscalator/bin/`,
> because that install tree ships no `skills/` — so the SM070 session-start self-check dies at step 1 on
> exactly the install shape that wins on PATH.

## Description

Found 2026-07-29 in an alpha field test. Bare invocation:

```
$ tt skillcheck
skillcheck: not a skills directory: /home/<user>/.genscalator/skills
(exit 2)
```

`skillcheck` resolves its expected set from `<tools>/../skills`. On this box the winning `tt` is the
native binary at `~/.genscalator/bin/tt`, and that install tree contains only `bin/`, `docs/`, `reqts/` —
no `skills/`, and no `tools/`. The shipped skills live in the plugin cache instead:

```
$ tt find ~/.genscalator --type d --max-depth 2          # (see issue 014 re: the depth arg)
  ~/.genscalator  ~/.genscalator/bin  ~/.genscalator/docs  ~/.genscalator/reqts

$ tt find ~/.claude/plugins/cache/<marketplace>/genscalator/<version> --type d --max-depth 2
  .../bin  .../deploy  .../docs  .../media  .../reqts  .../research  .../skills  .../tools
```

**Why it wedges.** `skillcheck` exists precisely because *an agent cannot feel a missing skill* — an
inactive skill is indistinguishable from the inside from an active one it merely failed to apply, so the
expected set has to be an explicit manifest that gets checked. That self-check is the first step of the
documented session-start reflex, and it fails by default on the install shape that `tt which` shows
winning on PATH. An agent that runs step 1 and gets exit 2 has no manifest, and the most likely
recovery is to skip the check entirely — which is the silent skill outage the tool was built to prevent.

Secondary effect: the same missing `tools/` means the test-suite path cannot resolve from that install
root either, so `gs test` must be pointed explicitly at the plugin cache or a real checkout.

Note this is an **install-shape** defect, not a logic defect in `skillcheck` — the same code works fine
when run from a full checkout. It surfaces only for users on the native install, which is the
recommended fast path.

## How to reproduce it

1. Install/build the native `tt` so that the winning PATH entry is `~/.genscalator/bin/tt`
   (confirm with `tt which tt` — it flags shadowed entries).
2. Run `tt skillcheck` with no arguments → exit 2, "not a skills directory".

The documented escape hatch works, so this is friction rather than loss of function:

```
$ tt skillcheck --skills <plugin-cache>/skills
expected genscalator skills (12, from <plugin-cache>/skills):
  avoid-guard-stall  contribute-tool  crud-web-app-seed  gs-dwim  in-session-experiment
  reqt-lang  research-methods  scala-code-review  scala-platform  scala-style
  serverless-spa-seed  tt-toolbox

$ tt skillcheck --skills <plugin-cache>/skills --active <the 12 names /skills listed>
OK: all 12 expected genscalator skills are active.
```

## Acceptance sketch

* Bare `tt skillcheck` succeeds on a native install: when `<tools>/../skills` is absent, fall back to
  discovering a plugin-cache `skills/` (or another known install root) rather than exiting 2.
* Failing that, the error message names the `--skills` escape hatch AND the probable plugin-cache path,
  so the reflex is recoverable from the error alone without reading the source.
* Consider shipping `skills/` (and `tools/`, for the suite) in the native install tree so the layout is
  self-sufficient.
* A test covers the "install root without a sibling skills dir" case.

## Discussion

### Comment by hmiddelk/Opus5 at 2026-07-29 23:27

Filed from the same alpha field test as issue 014. Environment: Ubuntu 24.04.2 LTS, kernel
6.8.0-136-generic, x86-64, OpenJDK 21; released alpha native build at `~/.genscalator/bin/tt` (ELF,
42.3M, built 2026-07-29T12:21), with the plugin-cache launcher shadowed.

Related but distinct: issue 003 covers promoting the native *rebuild ritual* to `tt update --native`;
this one is about what the native install *tree* contains once built. Ties into the docs point in issue
019 (`tt which tt` as a first-line check), since both failure directions now have field evidence.

Agent disclosure: found and drafted by an AI agent (Claude Opus 5) under human direction; the human
reviewed and submitted.

### Comment by hmiddelk/Opus5 at 2026-08-04 16:12

**CONFIRMED on Windows 10, identical** — same install-shape defect, same exit code, same message modulo
path separators. Released `v0.10.0` native `windows-x86_64` build, Windows 10 Enterprise 10.0.19045:

```
> tt skillcheck
skillcheck: not a skills directory: C:\Users\<user>\.genscalator\skills     (exit 2)
```

The install tree is `bin\ docs\ reqts\` — **no `skills\`, no `tools\`**, exactly as reported on Linux, so
the secondary effect holds too: `gs test` cannot resolve a suite from this root either.

One Windows-specific aggravation for the recovery path. This issue notes that a user who has not
established *which* `tt` won cannot explain the failure, and issue 019 proposes `tt which tt` as that
check — but on Windows `tt which` is itself broken (**issue 022**: it splits `$PATH` on `':'` and does no
PATHEXT resolution, so bare `tt which tt` reports "not found in PATH" even while `tt` is running). So on
Windows the diagnosis chain is broken at both links: `skillcheck` fails, and the tool that would explain
why also fails. Worth weighing when prioritising 022.

The documented escape hatch works here, so it stays friction rather than loss of function — the
`--skills <plugin-cache>\skills` form resolves normally.

Agent disclosure: the Windows re-test was run and written up by an AI agent (Claude Opus 5) under human
direction; the human reviewed and submitted.
