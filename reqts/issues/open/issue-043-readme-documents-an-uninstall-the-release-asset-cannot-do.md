# Issue 043: the README documents an uninstall the release asset cannot perform, so the documented remove command installs

> status: open · labels: docs, release, installer, uninstall, alpha, carrier-staleness · summary:
> `README.md` on `main` documents `--uninstall` in five places and tells the reader to download
> `get-genscalator.sc` **from the release assets**; that asset is byte-identical to v0.10.0 and has no such
> flag, so following the documented procedure performs a full **install**. The released tree is correctly
> silent about uninstall — the front page describes unreleased capability without saying so.

## Description

Found 2026-08-21 in a Windows field test of the update lifecycle (report 088), by a human following the
documented uninstall procedure and being surprised — not by reading code.

`README.md` on `main` is the GitHub front page. It documents uninstall as a supported procedure:

* **line 80** — *"the bootstrap installer **shipped with every release**"*, linking `releases/latest`
* **line 81** — *"download **`get-genscalator.sc`** from the release assets"*
* **line 96** — *"**Uninstall.** **The same script** removes what it installed, so
  `install → test → uninstall → reinstall` is the supported loop for comparing versions (and the way to get
  a genuinely naked box back before a field test)"*
* **lines 100-101** — the two commands, `--uninstall` and `--uninstall --force`
* **line 109** — *"a script you **fetch fresh** can still reverse an install whose own binary is broken or
  gone"*
* the **TLDR at line 5** also links `releases/latest/download/get-genscalator.sc`

Do exactly that today and the remove command installs. The asset served by
`releases/latest/download/get-genscalator.sc` is **byte-identical to the v0.10.0 asset** (sha256
`a7ee81d3ea899ce337b7e924310ae4258beb3f9a43496011c4f22bbfb075845f`, 14 183 B, 235 lines) and contains no
`--uninstall`. Unrecognised arguments are silently ignored (**issue 044**), so the flag does not error — it
falls through to the install path.

### The measured split across carriers

Counting the string `uninstall`:

| carrier | occurrences | has the flag? |
|---|---|---|
| `README.md` on `main` (the front page) | **5** | — it is documentation |
| `README.md` at tag `v0.10.2` | **0** | — |
| the installed native `docs/` tree | **0** | — |
| `get-genscalator.sc` from the v0.10.2 release assets | **1** (a comment) | **no** |
| `get-genscalator.sc` on `main` | 26 | yes |
| `get-genscalator.sc` in the plugin cache tree | 26 | yes |

**This is not stale documentation — it is the inverse.** The released tree never promises uninstall; the
tag's README and the shipped `docs/` say nothing about it, which is correct. Only the front page promises
it, because `--uninstall` (issue 039) is on `main` under *Unreleased (v0.10.3 wave) → v0.10.4 candidates*.
The front page runs **ahead** of the release and does not say so.

The two phrases that make the promise false rather than merely optimistic are *"**the same** script"* and
*"a script you **fetch fresh**"*. Both actively tell the reader that the artifact named in line 81 is the
one that uninstalls. The carriers that can uninstall — `main`'s raw file and the plugin cache tree — are
never named as download sources anywhere in the README.

**Why it wedges.**

1. **It defeats the loop it advertises.** Line 96 sells `install → test → uninstall → reinstall` as *the*
   way to compare versions and *"to get a genuinely naked box back before a field test"*. That reader is an
   alpha tester, the audience this release is for, and the step they are told to run puts the software back
   instead of removing it.
2. **The failure is silent and in the wrong direction.** Asking to remove software and receiving an install
   is worse than an error, and it is invisible unless the reader diffs the output against the flag they
   typed. That half is issue 044; this issue is why anyone types the flag at all.
3. **It costs the project its own argument.** The README earns the reader's trust by refusing
   curl-into-shell — *"this project argues against curl-into-shell precisely because it hides what it
   does"* — and asks them to read the file before running it. A reader who **does** read the file finds no
   `--uninstall` in it and is left unable to tell whether the docs, the download, or their own reading is
   wrong.
4. **The mechanism outlives the instance.** When v0.10.3 publishes a script with the flag, this particular
   symptom disappears while the cause — a front page with no version stamp, free to describe unreleased
   work — remains. The next feature documented ahead of its release repeats it.

### Relationship to existing issues

* **Issue 039** built the uninstaller and is not duplicated: 039 is the feature, this is the gap between
  the feature's documentation and its distribution.
* **Issue 044** is the other half of the observed failure and is filed separately because the fix and the
  lifetime differ: 044 is durable and one-line, this one is a docs/release-sequencing question.
* **Issue 041** (capability descriptions are not projected from one source) is the same class — a
  description that outruns the capability — on a different carrier. Worth settling together if the
  maintainer sees one mechanism.
* **Issue 036** is the near-inverse and supplies the fix. 036 addressed carriers that cannot say how *old*
  they are, and its fix stamped `CONTRIBUTING.md` and `reqts/issues/README.md` with a
  `> **genscalator vX.Y.Z**` banner asserted by `version.test.scala`. **`README.md` did not get one** — it
  opens with the title, a CI badge and the TLDR, with no version line. This issue is a carrier running
  *ahead* of the release, and the same mechanism catches it.

## How to reproduce it

From a box with a genscalator install, following the README exactly:

```
# as README line 81 instructs — download from the release assets
gh release download v0.10.2 --repo bjornregnell/genscalator --pattern get-genscalator.sc
#   (or: curl -LO https://github.com/bjornregnell/genscalator/releases/latest/download/get-genscalator.sc)

grep -c uninstall get-genscalator.sc          # => 1, and it is a comment; there is no flag

# the command README line 100 documents as a PREVIEW of what would be removed:
scala-cli run get-genscalator.sc -- --uninstall
```

Observed (Windows 10, v0.10.2, `--dry-run` added to keep the probe non-destructive):

```
genscalator bootstrap  (DRY RUN: nothing will be written)
  platform: windows-x86_64
  release:  latest published
  install:  C:\Users\<user>\.genscalator
  verified: sha256 7b5fcae61f2cae8da84decd82c7a2420a8a078aef08cf03b14cf730896ae9b7d  (14059469 B)
  would unpack 30 file(s) into C:\Users\<user>\.genscalator
```

Without `--dry-run` that is an install. Confirm the asset identity:

```
sha256sum get-genscalator.sc
# a7ee81d3ea899ce337b7e924310ae4258beb3f9a43496011c4f22bbfb075845f  — identical to the v0.10.0 asset
```

And confirm the release itself is not at fault:

```
git show v0.10.2:README.md | grep -c uninstall     # => 0
```

## Acceptance sketch

* **The front page cannot promise what `releases/latest` does not ship.** Either gate the uninstall section
  on the release that carries it, or mark it explicitly as unreleased with the version it lands in — the
  same courtesy `CHANGELOG.md`'s *Unreleased* heading already gives.
* **Extend issue 036's banner to `README.md`**, asserted by `version.test.scala` alongside
  `CONTRIBUTING.md` and `reqts/issues/README.md`. A reader then sees which release the page describes
  without asking, and the release gate keeps it honest. This is the structural fix and it needs no new
  mechanism.
* **If the uninstaller is meant to be fetchable ahead of its release** — a defensible position, since
  `--uninstall` is most needed when the installed binary is broken — then say so where the reader is:
  name the raw-`main` URL in the uninstall section and explain why it differs from the release asset.
  Silence is the only unacceptable option.
* **Consider attaching `get-genscalator.sc` to releases from `main` rather than the tag**, if the
  fetched-fresh property in line 109 is intended to mean "always current". Today the asset is pinned to the
  tag while the prose describes `main`, and that gap is exactly this issue.
* A check that the flags the README documents exist in the asset the README links would have caught this
  and belongs with the release gate (issue 012 / 021's companion), not with a human's vigilance.

## Discussion

### Comment by hmiddelk at 2026-08-21 17:00

Filed from report 088, a Windows 10 field test of the update lifecycle
(`research/reports/report088-windows-update-lifecycle-2026-08-21.md`), which also carries the method,
coverage and threats to validity for this batch.

Baseline measured against: released `v0.10.2` native `windows-x86_64`, install `VERSION.txt` = `v0.10.2`,
zip sha256 `7b5fcae61f2cae8da84decd82c7a2420a8a078aef08cf03b14cf730896ae9b7d` (14 059 469 B, 30 entries);
`bin/tt.exe` 41.1M. Named by `VERSION.txt` + hash rather than by `tt --version`, which still exits 2 at
this release (**issue 028**; its fix is a v0.10.3 candidate). Windows 10 Enterprise 10.0.19045, x86-64,
PowerShell 5.1, Scala CLI 1.16.0.

Numbering note: `main`'s highest issue number is 039, but PRs #4, #5 and #6 claim 040, 041 and 042. This
issue takes **043** on the assumption those land; per `reqts/issues/README.md` the later PR renumbers if
that assumption is wrong.

**How this was found is the point, and it is why it is filed as its own issue.** The mechanism was already
in hand from the sweep — two files hash-compared, a flag absent from a 235-line script — and the first
draft of report 088 buried it as the "aggravating half" of issue 044, predicting it would *"close when
v0.10.3 ships"*. That was wrong twice: it is the failure a reader actually meets, and "it closes on the
next release" is the reasoning that lets the mechanism survive. The reframing came from the human asking
why downloading the script had not been enough — **an agent reading the source cannot feel a false
promise, because it is reading the code the promise is about.** Recorded because it bears on what kind of
finding this is: not a code defect, a broken promise, and only a reader can report it.

Not verified: macOS or Linux (the carrier facts are platform-independent and the counts above are from the
repo, not the box, so this should reproduce anywhere); releases other than v0.10.2; and whether pinning
the asset to the tag rather than `main` is deliberate — which is the one question that could turn this
from a defect into a documentation-only fix.

Agent disclosure: an AI agent (Claude Opus 5) ran the lifecycle test, measured the carrier counts and
drafted this issue under human direction; the human followed the documented procedure that exposed the
gap, supplied the framing above, and reviewed and submitted.

### Comment by bjornregnell/Fable5 at 2026-08-24 20:13

Merged with the central measurement verified independently: the served asset is sha256
`a7ee81d3...845f`, 14 183 B, 235 lines, one commented `uninstall`, no flag, byte-identical to the
v0.10.0 asset; README at tag v0.10.2 has zero mentions, `main` has five. The diagnosis holds. Three
corrections to the text as filed, recorded here rather than edited in:

1. **The count table's two `26` cells are 25.** `get-genscalator.sc` on `main` had 25 occurrences of
   `uninstall` at the issue's own baseline `7f03345` and still has 25 today; the plugin-cache copy
   likewise. Every other cell in the table is exact.

2. **"Five places" and the bullet list are not the same five.** The five actual `uninstall`
   occurrences in the README are lines 31, 96, 100, 101 and 108 — line 31 (the contents entry "(and
   how to uninstall)") is not in the list, while lines 5, 80 and 81 carry no `uninstall` at all; they
   are the download-source lines. The bullets describe those lines correctly, but the list runs to six
   items under a count of five. The download-source lines matter to the argument (they name the
   artifact), so the right fix is wording, not deletion.

3. **The numbering note was already stale at merge time** — `main` carried issue 046 and `tt issue
   next` returned 047; 043 was still free, so no collision and the number stands. The underlying gap
   (numbers reserved by open PRs are invisible to a file scan) is now a stated rule in
   `reqts/issues/README.md` (`7b580f0`), so future filers check the forge's open PRs too.

One addition that supports the last acceptance bullet: pinning the asset to the tag has already
drifted once — the v0.10.0 release asset does not match the v0.10.0 tag (tag file 13 715 B /
`71b27ebc...2c1b` vs published asset 14 183 B / `a7ee81d3...845f`, uploaded six minutes after the
zips; the reason is in the script itself at `get-genscalator.sc:362`, the post-publish smoke fix from
2026-07-28). So the `.sc` asset is not reliably tag-derived today, which is the deferred question's
answer arriving from a direction the issue did not take, and it makes the fix cheaper than assumed.
The deferred question itself stays open; I will answer it as part of the fix.

Agent disclosure: this comment was drafted by an AI agent (Claude Fable 5) under human direction; the
human reviewed and submitted.
