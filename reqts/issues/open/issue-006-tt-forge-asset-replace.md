# Issue 006: replacing ONE release asset has no typed shape, and the refusal message points at a verb that does not exist

> status: open · labels: toolbox, forge, release · summary: `tt forge release-upload` refuses
> duplicate asset names by design and its error says "Delete it first, or upload under another
> name" — but no forge verb can delete an asset (`release-delete` removes a whole release). So
> asset REPLACEMENT, an act that genuinely occurs in release maintenance, only exists as a raw
> `gh` call.

## Description

The duplicate-name refusal is correct as a default: silently overwriting a published asset is
exactly the kind of surprise a typed tool should not hand out. The gap is that the refusal's own
advice ("Delete it first") cannot be followed inside the toolbox:

* `tt forge release-upload` attaches ONE file and dies on a duplicate name.
* `tt forge release-delete` deletes an entire release, never a single asset.
* No other verb touches assets.

Specimen, 2026-07-28, this repo. The v0.10.0 post-publish smoke test found `get-genscalator.sc`
overwriting the zip's CI-stamped `VERSION.txt` with the literal word `latest`, which broke
`tt update --native`'s already-up-to-date check (fixed in `8c7f02e`). Shipping that fix required
replacing the `get-genscalator.sc` asset on the already-published release. The typed path
dead-ends as described, so the fallback was one raw `gh release upload --clobber` — the same
shape CI itself runs in `native-release.yml`. The gap was named before the raw call, which makes
this a flag-the-gap case rather than a regression; this issue is the flag.

## Why it is a safety issue, not a convenience one

Replacing an asset on a PUBLISHED release changes bytes a user may already have downloaded and
verified. That is precisely the act that deserves a typed verb with a preview, an audit line, and
a deliberate confirmation — not a raw call whose flags are remembered under time pressure minutes
after a release went live. And an error message that prescribes an impossible action trains the
caller to leave the toolbox at exactly the moment the toolbox should be holding the rails.

## Acceptance sketch

* `tt forge asset-rm <owner>/<repo> <tag> <asset> [--gh | --url BASE]` — preview by default,
  `--yes` to apply, and the louder double-flag for a published release, following the
  `release-delete --yes --allow-published` pattern.
* And/or an explicit `--clobber` on `release-upload`, so replacement is a conscious flag while
  refuse-duplicates stays the default.
* The duplicate-name error message names the verb that actually exists, so its advice can be
  followed without leaving the toolbox.
* Same token machinery and audit stderr line as the other effectful forge verbs.

## Discussion

### Comment by bjornregnell/Charlie at 2026-07-28

Filed on BR's go, straight after the specimen above. Same loop as `release-edit --gh` and
`release-upload` earlier the same week: a raw reach names a missing verb, the verb gets built.
