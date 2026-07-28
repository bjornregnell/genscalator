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
