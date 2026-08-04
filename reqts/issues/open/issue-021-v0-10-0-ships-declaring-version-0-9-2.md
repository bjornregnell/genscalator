# Issue 021: the `v0.10.0` tag ships with every in-repo version declaration still reading `0.9.2`, and no CHANGELOG section

> status: open · labels: release, docs, versioning, alpha · summary: at the `v0.10.0` tag,
> `plugin.json` and `marketplace.json` both declare `"version": "0.9.2"`; `AGENTS.md`, `VERSION.txt` and
> the newest CHANGELOG section still say v0.9.2 on main six commits later — so an alpha tester cannot tell
> which release they are on, and the release users are told to review has no release notes.

## Description

Found 2026-07-29 while writing up an alpha field test (issues 014–020) and trying to state *which version*
was tested. It turned out not to be answerable from the repo.

`CHANGELOG.md` states the invariant itself:

> Versions follow the git tags (`vX.Y.Z`); the `version` field in `.claude-plugin/plugin.json` +
> `marketplace.json` and the version line in `AGENTS.md` track the same number.

The `v0.10.0` release violates it. Verified **at the tag**:

```
$ git describe --tags HEAD
v0.10.0-6-g92caf86                       # the tag is an ancestor of main, 6 commits back

$ git show v0.10.0:.claude-plugin/plugin.json
  "version": "0.9.2"

$ git show v0.10.0:.claude-plugin/marketplace.json
  "version": "0.9.2"
```

And still on **main**, six commits later:

| carrier | value on main | expected |
|---|---|---|
| `.claude-plugin/plugin.json` | `0.9.2` | `0.10.0` |
| `.claude-plugin/marketplace.json` | `0.9.2` | `0.10.0` |
| `AGENTS.md` operating-rules line | `genscalator v0.9.2` | `v0.10.0` |
| `VERSION.txt` | `0.9.2` | `0.10.0` (or CI-stamped) |
| `CHANGELOG.md` newest released section | `## v0.9.2 — 2026-07-24` | a `## v0.10.0` section |

None of the six commits since the tag is a version bump (`EMBER-TEMPLATE v0.3`, `PRD: greprRegexLint`,
`reqts consistency pass`, `issues 007-013`, `issue-006`, `bootstrap: stop clobbering the zip's CI-stamped
VERSION.txt`), so the numbers were not corrected after publication either.

Meanwhile the *code* knows about v0.10.0 — e.g. `tt session --help` is annotated `(v0.10.0)` — so the gap
is specifically in the declared metadata and the release notes, not in the shipped behaviour.

**Why it wedges.**

1. **A tester cannot name their version.** The plugin advertises `0.9.2` to Claude Code, so `/plugin` shows
   `0.9.2` while the installed release is `v0.10.0`. In this field test the version had to be replaced with
   build provenance ("native binary, built 2026-07-29T12:21") in every issue report — which weakens each
   report, since a maintainer cannot map it to a release.
2. **The release notes users are instructed to read do not exist.** `CHANGELOG.md` says: *"Skim this file
   before adopting a new version: it changes the agent's operating rules, so review beats blind pull."*
   There is no `v0.10.0` section to skim, so the one review step the project asks of its users cannot be
   performed for the current release.
3. **The staleness self-check misfires.** The `AGENTS.md` version line exists so an agent can notice its
   vendored operating rules are older than the repo's ("If your vendored copy is older, your modus operandi
   is..."). With the number frozen at v0.9.2, a genuinely stale v0.9.2 copy looks current.
4. **Alpha feedback loses its denominator.** For a release whose whole purpose is field reports, "which
   version were you on?" is the first triage question, and right now every answer has to be reconstructed
   from binary mtimes.

## How to reproduce it

```
$ git -C <checkout> describe --tags HEAD
$ git -C <checkout> show v0.10.0:.claude-plugin/plugin.json      # "version": "0.9.2"
$ git -C <checkout> show v0.10.0:.claude-plugin/marketplace.json # "version": "0.9.2"
```

Or from a user's side: install the plugin from the marketplace at `v0.10.0` and observe that `/plugin`
reports `0.9.2`.

## Acceptance sketch

* One **single source of truth** for the version is chosen and the rest derive from it. Note `VERSION.txt`
  is CI-stamped at build time (see commit `8c7f02e` and issue 012), so it is plausibly an *output*, not an
  input — worth settling explicitly so the two mechanisms cannot disagree again.
* The in-repo carriers are corrected to match the released tag, and a `## v0.10.0` CHANGELOG section is
  added. If retagging is undesirable, the next release's notes state plainly that v0.10.0 shipped with
  0.9.2 metadata, so the record is honest rather than silently repaired.
* A **release-time gate** refuses to tag or publish when the tag does not match the declared version in
  `plugin.json` / `marketplace.json` / `AGENTS.md`. This is the natural companion to issue 012's
  validation of the workflow's tag input.
* Ideally a locally runnable check (a small `tt` verb or a CI step) asserting all carriers agree, so the
  invariant `CHANGELOG.md` already documents becomes enforced rather than aspirational.

## Discussion

### Comment by hmiddelk/Opus5 at 2026-07-29 23:51

Filed alongside issues 014–020 from the same alpha field test, on the "report anything that wedges,
however small" principle — though this one is arguably not small, since it degrades every other alpha
report by removing the version denominator.

**Relationship to issue 012 (please merge if you judge them one thing).** Issue 012 covers the release
*workflow*: an unvalidated free-text `tag` input that both stamps `VERSION.txt` and picks the
`--clobber` upload target, and it already references a "v0.10.0 version-stamp investigation" from
2026-07-28. This issue is about the *repository content at the tag* — `plugin.json`,
`marketplace.json`, `AGENTS.md` and the missing CHANGELOG section, i.e. what the released tree declares
about itself rather than what CI stamps into the binaries. They may well share one root cause (a release
checklist that does not gate on version agreement), in which case 012's acceptance sketch could absorb
this one; kept separate here because the user-visible symptom (`/plugin` says 0.9.2, no release notes to
skim) is distinct from the workflow safety concern.

Agent disclosure: found and drafted by an AI agent (Claude Opus 5) under human direction; the human
reviewed and submitted.

### Comment by hmiddelk/Opus5 at 2026-08-04 16:12

**CONFIRMED from the user side on a second machine (Windows 10), with one carrier now disagreeing
*within* a single install.** Fresh install on 2026-08-04: `/plugin marketplace add bjornregnell` +
`/plugin install genscalator@bjornregnell`, then `scala-cli run get-genscalator.sc` for the native binary.

| carrier on this machine | value |
|---|---|
| `~\.genscalator\VERSION.txt` (native install, CI-stamped) | **`v0.10.0`** |
| plugin cache directory name | `0.9.2` |
| `.claude-plugin\plugin.json` → `version` | `0.9.2` |
| plugin cache `VERSION.txt` | `0.9.2` |
| `AGENTS.md` operating-rules line | `genscalator v0.9.2` |
| newest released `CHANGELOG.md` section | `## v0.9.2 — 2026-07-24` |

So the user-visible symptom in "How to reproduce it" is confirmed exactly: install from the marketplace at
`v0.10.0` and the plugin reports `0.9.2`.

New data point on the split the acceptance sketch has to settle. The Linux report found `VERSION.txt` on
main reading `0.9.2`; here the *native install's* `VERSION.txt` reads `v0.10.0` — consistent with the
commit noted above (`bootstrap: stop clobbering the zip's CI-stamped VERSION.txt`) having taken effect.
That is the fix working, and it produces a new symptom: **the two trees on one machine now disagree**, the
binary tree saying `v0.10.0` and the plugin tree saying `0.9.2`. It also means the CI-stamped value
carries a leading `v` while `plugin.json` carries a bare number, so whichever direction the single source
of truth is chosen, the `v` prefix needs deciding too or a comparison will read `v0.10.0 != 0.10.0`.

This one had direct cost in this field test, which is the point of the "loses its denominator" argument:
the agent's first attempt to state which version was under test picked up `0.9.2` from the plugin, and the
version had to be reconstructed from `VERSION.txt` plus the release asset's sha256 before any of these
Windows confirmations could be honestly labelled. Every issue comment in this batch therefore names the
build by platform, sha256 and byte count rather than by version alone.

Agent disclosure: the Windows re-test was run and written up by an AI agent (Claude Opus 5) under human
direction; the human reviewed and submitted.
