# Issue 012: `native-release.yml`'s free-text tag input serves double duty and is never validated

> status: open · labels: ci, release, safety · summary: a manual dispatch's `tag` input is BOTH
> the `VERSION.txt` stamp and the upload target for `gh release upload --clobber` — one typo
> mis-stamps every binary AND clobbers assets on whatever release the typo names.

## Description

On a `release` event the workflow derives everything from the event and is safe. On a manual
`workflow_dispatch`, `inputs.tag` is free text: it is echoed into `staging/VERSION.txt` at build
time and used as the `--clobber` upload target at publish time, with no check that it names an
existing release or a real tag. The v0.10.0 post-publish investigation (2026-07-28) had to read
job logs to establish what a dispatch had actually stamped — the input's value is otherwise
invisible after the fact.

## Acceptance sketch

* Validate `inputs.tag` early in the run: it must name an existing release (or the run fails
  before building), so a typo dies in seconds instead of publishing.
* Derive the `VERSION.txt` stamp from the release the run actually attaches to, not from the raw
  input — one source of truth instead of two uses of one string.
* Blank input keeps its current meaning (build only, attach nothing).

## Discussion

### Comment by bjornregnell/Charlie at 2026-07-28

Filed on BR's go, from the v0.10.1 mining sweep; the open rider from the v0.10.0 version-stamp
investigation.

### Comment by bjornregnell/Fable5 at 2026-08-09 14:36

SHIPPED in `50cb1d0` (v0.10.2 wave), implemented in the gate job that issue-021's cut introduced —
one gate serving both issues, as that review proposed. The free-text tag input is now checked
BEFORE anything downstream trusts it, in an order where each check assumes only what the previous
one proved: shape (v + dotted numerics + optional suffix; garbage and injection text die with the
expected form named), existence (asked of the REMOTE via `git ls-remote --exit-code`, deliberately
fetch-independent — a well-shaped typo dies in seconds instead of `--clobber`-ing assets on
whatever release it names), then the issue-021 carrier match. Hardening beyond the sketch: the tag
reaches the script as an env var, never template-expanded into the run block, so a crafted input
is inert data rather than shell source; release-event tags flow through the same checks. Blank tag
keeps meaning build-only. One recorded nuance: "must name an existing release" is implemented as
"existing git tag" — a real tag lacking a release would still die at the upload step, after
building; acceptable because the carrier check means at most one tag can pass on a given checkout.
Unverified in CI until the next dispatch or release run fires the gate; ready to close after that
first live firing, on the maintainer's sweep.

Agent disclosure: this comment was produced by an AI agent (Claude Fable 5) under human direction;
the human reviewed and submitted.
