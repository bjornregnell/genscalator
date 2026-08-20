# Issue 042: `--uninstall` never names the version it is removing, on either path, while `VERSION.txt` sits in the set of files it is about to delete

> status: open 2026-08-20 · labels: installer, uninstall, version, alpha · measured against: v0.10.2
> (from `.genscalator/VERSION.txt`) · summary: the uninstaller prints `version: latest` on the
> manifest path and `version: unknown` on the fallback path; on both, the installed release is
> stated in `VERSION.txt` in the install root — a file the uninstaller lists, removes, and never
> reads. So the one operation that must identify what it is deleting destroys the evidence rather
> than reporting it.

## Description

`--uninstall` prints a `version:` line, and on a v0.10.2 install neither code path ever puts
`v0.10.2` in it.

**Manifest path** (a post-039 install, `INSTALL-MANIFEST.txt` present):

```
genscalator uninstall  (PREVIEW: nothing will be removed)
  install:  /home/hans/.genscalator
  version:  latest
  would remove: 31 file(s)
    VERSION.txt
    bin/tt
```

**Fallback path** (a pre-039 install, no manifest) — `get-genscalator.sc:232`:

```scala
Manifest(wellKnown, None, "unknown", fallback = true)
```

prints:

```
  version:  unknown
```

**Ground truth, on disk in both cases:**

```bash
$ cat ~/.genscalator/VERSION.txt
v0.10.2
```

### Two different bugs with one symptom

* **Manifest path — the manifest records the request, not the resolution.** `writeManifest` is
  called with the `--tag` flag's `Option[String]` and stores `tag.getOrElse("latest")`. On the
  overwhelmingly common invocation (no `--tag`), that writes the literal string `latest`, which
  names *the policy used to pick a release*, not the release picked. `installed-tag: latest` is true
  and useless: it will still say `latest` on a box installed a year ago.
* **Fallback path — the tag is hardcoded to `"unknown"`** while `VERSION.txt` is listed in the very
  same expression's well-known set two lines earlier (`get-genscalator.sc:223`), so the uninstaller
  has already established the file is there.

The script also knows what that file is for. `get-genscalator.sc:359`:

```scala
// The archive carries the CI-stamped tag in VERSION.txt and unzip REPLACE_EXISTING has already
```

and 363-364 write it as a fallback when the archive did not carry one, so on any install produced by
this script the file exists and states the release.

### Why this is worse than a cosmetic label

The uninstaller **deletes `VERSION.txt`** as part of the removal — it is item one in the
`would remove` list above. So the operation destroys the record it declined to read. Once `--force`
has run, the version that was on the box is unrecoverable from the box; the only surviving statement
about it is the `version: latest` line the tool printed, which is wrong.

That matters for the loop this exists to serve. Issue 039 built `--uninstall` so that install → test
→ uninstall → reinstall would be reachable, and its whole motivation is **comparing versions**: an
alpha tester runs the loop precisely to move between them. An uninstaller that cannot say which
version it just removed breaks the bookkeeping of the loop it was written for. And issue 028 already
established that identifying an artifact is a live problem in this project ("you cannot ask the
binary what it is") — the uninstaller is the one place where the answer is guaranteed to be on disk,
and it is thrown away.

On the fallback path there is an extra cost: `version: unknown` prints directly beneath the
`⚠ NO INSTALL-MANIFEST.txt found` warning, so a tester reads two lines that together say *nothing
here is knowable* — when the version, at least, is.

## How to reproduce it

```bash
# manifest path
scala-cli run get-genscalator.sc                      # any current script; writes INSTALL-MANIFEST.txt
cat ~/.genscalator/VERSION.txt                        # => v0.10.2
grep installed-tag ~/.genscalator/INSTALL-MANIFEST.txt # => installed-tag: latest
scala-cli run get-genscalator.sc -- --uninstall        # => version:  latest        (preview, removes nothing)

# fallback path
rm ~/.genscalator/INSTALL-MANIFEST.txt
scala-cli run get-genscalator.sc -- --uninstall        # => version:  unknown
```

Expected in both: `version: v0.10.2`, the value in `VERSION.txt`.

Note the preview is read-only, so both halves can be checked without removing anything.

## Acceptance sketch

* **Read `VERSION.txt` on both paths, and prefer it.** It is the CI-stamped fact from the archive,
  it is present on every install this script has ever made, and it needs no network and no manifest.
  The fallback path gains a real answer; the manifest path gains a cross-check.
* **Record the resolved release in the manifest, not the request.** `installed-tag: latest` should
  be the resolved tag. Either read `VERSION.txt` back after unpacking, or thread through the tag the
  download actually resolved to. Keep the requested value too if it is useful
  (`requested-tag: latest` + `installed-tag: v0.10.2`), but the field an uninstaller reads must name
  a release.
* **Say so when it genuinely cannot tell.** If `VERSION.txt` is absent and the manifest is absent,
  then `unknown` is honest and should stay — the defect is printing it while holding the answer, not
  the existence of the word.
* **Print the version before deleting the file that states it**, and consider echoing it in the
  closing line, so a tester's scrollback records which version left the box. This is the whole
  value: the message survives, the file does not.
* Anchor a test on it: a scratch-HOME round trip asserting the `version:` line matches
  `VERSION.txt`'s contents, on both the manifest and the no-manifest path. Cheap, and it pins the
  one thing that is easy to regress.
* Out of scope: `tt --version` (issue 028) and anything about *which* files the fallback removes
  (issue 040). This issue is only about the uninstaller misreporting a version it can read.

## Discussion

### Comment by hmiddelk at 2026-08-20 15:35

Split out of issue 040 at hmiddelk's request. It surfaced while arguing 040's fix strategy — the
fallback's hardcoded `"unknown"` was noticed two lines from the `VERSION.txt` entry in its own
well-known list — but it is independent of 040 in both cause and fix: 040 is about *which files* the
fallback removes, this is about *what the tool says* it is removing, and neither fix implies the
other. Filed separately so 040's list question does not have to carry it.

Worth recording that the manifest half was found only by checking, not by reasoning. The 040 comment
that prompted this issue asserted the defect was specific to the fallback path, on the strength of
having read line 232. Running the preview against a manifest-bearing install showed
`version: latest` — the same symptom from an unrelated cause, on the path that was assumed correct.
The fallback is the louder failure; the manifest path is the one that will still be wrong after the
fallback is fixed.

Agent disclosure: drafted by an AI agent (Claude Opus 5) in session with hmiddelk. Verified on one
Linux box against v0.10.2: both preview outputs quoted above, `VERSION.txt`'s contents, the
`installed-tag: latest` line in a manifest written by the current script, and lines 223, 232, 359
and 363-364 of `get-genscalator.sc`. Not verified: macOS or Windows, any release other than v0.10.2,
or an install made with an explicit `--tag` (where `tag.getOrElse` would record the requested tag
and the manifest half of this issue may not reproduce).
