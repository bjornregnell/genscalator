# Issue 003: promote the rebuild ritual to a typed verb `tt update --native`

> status: open, GATED · labels: native, toolbox, cross-platform · summary: lift
> `deploy/buildnative.sc` (build -> parity -> atomic swap) into the toolbox as
> `tt update --native`, so the ritual gets a typed, allowlistable shape — but only AFTER
> buildnative.sc is proven on BOTH Windows and macOS (BR's gate, set 2026-07-23).

## Description

The tt-graalify rebuild ritual lives in `deploy/buildnative.sc` (2026-07-23): build the
native-image dispatcher into `tmp/tt-native.next`, run the full CLI-contract suite THROUGH
the candidate (the parity property in `tools/test/cli.test.scala`), atomically swap only on
green. Proven on linux-x64 the same day (build 91 s, parity 31 s, 0 failures) and the
launcher's native path is DEFAULT-ON with staleness fallback (`docs/native.md`).

The durable home for the ritual is a typed verb, not a loose script — `tt update --native`
(the `update` tool already owns the is-my-install-current question, so the rebuild step
extends it naturally). Gains: allowlist-friendly single shape, compiler-checked swap logic,
one place for the coming platform variations, and the deploy/ dir stops carrying a
build-not-deploy misfit (discussed in-feed 2026-07-23; root placement was rejected to keep
the root lean per the big-repo-refactor).

**GATE (BR 2026-07-23): do NOT build this verb until `deploy/buildnative.sc` has been
tested green on BOTH Windows and macOS.** Rationale: the verb freezes an interface; the
script is the cheap place to discover platform surprises (GraalVM toolchain presence,
`/proc/meminfo` absence — the script already treats non-Linux memory as unknown-proceed,
path/exec semantics on Windows, whether `-J-Xmx` and the option syntax survive each
platform's native-image). The platform runs belong to the alpha platform-matrix work
(SM146 distance report: tester platforms are the long pole).

## Acceptance sketch

* `tt update --native` performs build -> parity -> atomic swap with the same
  never-swap-unproven guarantee, on linux-x64 + macOS + Windows.
* Failure modes preserved: build failure changes nothing; parity failure keeps the
  candidate for inspection and never touches the live binary; free-memory floor honored
  where measurable.
* `deploy/buildnative.sc` retires (or shrinks to a one-line delegator) when the verb ships.

## Discussion

### Comment by bjornregnell/CF5 at 2026-07-23 21:01

Filed on BR's in-feed pin the evening the ritual went green on linux-x64. Ties: SM112
(`gs native` provisioning would call this verb), SM146 (native = no-bloop endgame),
SM196 (release-all could ship the binaries the verb builds), `docs/native.md` (status +
remaining steps list, which should point here).
