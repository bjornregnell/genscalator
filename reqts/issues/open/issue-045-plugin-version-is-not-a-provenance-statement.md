# Issue 045: the plugin version is not a provenance statement — a tree declaring `0.10.2` can be 21 commits ahead of the `v0.10.2` tag

> status: open · labels: plugin, release, versioning, agent-trust, carrier-staleness, alpha · summary: the
> marketplace tracks the `main` branch, so `claude plugin update` installs whatever `main` holds while
> reporting the version string in `plugin.json`. On this box that string read `0.10.2` over a tree **21
> commits and 79 files ahead of the `v0.10.2` tag**, including `tools/which.scala` and `tools/versionlib.scala`
> — so the plugin's `tools/` was NEWER than the released binary a field test was meant to be testing.

## Description

Found 2026-08-21 in a Windows field test of the update lifecycle (report 088), while establishing which
artifact was under test before re-verifying issues 014–022.

The plugin was refreshed the documented way:

```
claude plugin marketplace update bjornregnell     # 92caf86 -> 7f03345
claude plugin update genscalator@bjornregnell     # "updated from 0.9.2 to 0.10.2"
```

`~/.claude/plugins/installed_plugins.json` then reads:

```json
"genscalator@bjornregnell": [ {
  "installPath": ".../plugins/cache/bjornregnell/genscalator/0.10.2",
  "version": "0.10.2",
  "gitCommitSha": "7f03345c299092dd4ca7537116aec3874f59a9a5"
} ]
```

Every version string on the box now agrees — install `VERSION.txt` `v0.10.2`, plugin cache `VERSION.txt`
`0.10.2`, `plugin.json` `0.10.2`, cache directory named `0.10.2`. **And the trees do not.**

```
$ gh api repos/bjornregnell/genscalator/compare/v0.10.2...7f03345
status: ahead   ahead_by: 21   behind_by: 0   files changed: 79
```

`7f03345` is `main`'s head, not the tag. `plugin.json` on `main` declares the last *released* number, and
the marketplace source is the branch, so the version string names the release the tree has passed rather
than the tree. Among the 79 files are `tools/which.scala`, `tools/versionlib.scala`, `tools/files.scala`,
`tools/text.scala`, `tools/git.scala`, `tools/gitinfo.scala` and their tests — the unreleased v0.10.3 /
v0.10.4 wave.

So on one machine, at one moment, with everything reporting `0.10.2`:

| carrier | declares | actually contains |
|---|---|---|
| `~/.genscalator/bin/tt.exe` | `VERSION.txt` `v0.10.2` | the **v0.10.2 tag**, CI-built |
| plugin cache `tools/` | `0.10.2` | `main` @ `7f03345`, **21 commits ahead** |

**Why it wedges.**

1. **It silently changes what a field test measures.** This is not hypothetical: reports 085–087 and this
   one all re-run reproductions against "the toolbox", and the plugin tree ships a complete, runnable
   `tools/tt`. Reaching for it tests unreleased code while the report says `v0.10.2`. Report 086 already
   had to caveat that its doc text came from the binary while the skills on the box were `0.9.2`; this is
   the same hazard with the version agreement *removed as a warning sign*. Report 088 adopted "verify
   against the native binary, never the plugin's `tools/tt`" as a standing rule because of this finding —
   a rule an agent has to be told, since nothing on the box says it.
2. **It is the inverse of issue 036, so 036's fix does not catch it.** 036 addressed carriers that cannot
   say how *old* they are, and reasonably assumed the failure is a missing or stale version. Here the
   version is present, current, and agrees across every carrier — and identifies nothing. A stamp that is
   always correct and never sufficient is worse than a stale one, because it removes the reader's cue to
   check.
3. **It is issue 021's descendant.** 021 was "the tag declares 0.9.2"; its fix made the numbers agree and
   added a release gate asserting tag == `VERSION.txt`. That gate is sound and fired green for v0.10.1 and
   v0.10.2. But agreement between *numbers* was never the goal — knowing *which code you are running* was,
   and a branch-tracking marketplace reintroduces the gap on the far side of the gate.
4. **The real identity is present and unsurfaced.** `gitCommitSha` in `installed_plugins.json` is exact,
   and nothing displays it: `claude plugin list` shows `Version: 0.10.2`, the cache directory is named
   `0.10.2`, and `tt --version` — whose whole purpose per **issue 028** is to answer *what am I running* —
   does not exist at this release and, when it lands, reports on the install it runs from.

Honest scoping: **the marketplace-tracks-`main` behaviour may well be intended**, and for a plugin whose
value is skills-under-development that is a defensible choice. The defect claimed here is narrower — that
the arrangement is **not stated anywhere the reader will meet it**, and that the version string actively
implies the opposite. If tracking `main` is deliberate, this becomes a docs-and-labelling issue rather than
a release-process one, and the acceptance sketch below is written to work either way.

## How to reproduce it

On any box with the plugin installed from the marketplace:

```
claude plugin marketplace update bjornregnell
claude plugin update genscalator@bjornregnell
claude plugin list                                    # => Version: 0.10.2

# the declared version:
cat ~/.claude/plugins/cache/bjornregnell/genscalator/0.10.2/VERSION.txt      # => 0.10.2

# the actual content:
SHA=$(jq -r '.plugins["genscalator@bjornregnell"][0].gitCommitSha' \
        ~/.claude/plugins/installed_plugins.json)
gh api repos/bjornregnell/genscalator/compare/v0.10.2...$SHA \
  --jq '{status, ahead_by, files: (.files | length)}'
# => {"status":"ahead","ahead_by":21,"files":79}
```

The marketplace checkout is also a shallow clone with **no tags** (`git tag` in
`~/.claude/plugins/marketplaces/bjornregnell` returns nothing, `.git/shallow` present), so the comparison
cannot be made locally — which is part of why the situation is easy to miss.

## Acceptance sketch

* **Say which ref the plugin tree came from, where the reader is.** Whatever the source policy, the
  installed tree should carry its commit — a line in the cache's `VERSION.txt`, or a
  `provenance: main@7f03345` field beside the version — so "which code is this" is answerable offline
  without the GitHub API.
* **Decide and document the marketplace source.** If it tracks `main` on purpose, say so in the README's
  plugin section and in `docs/claude-plugin.md`: *"the plugin tracks `main`, so it may be ahead of the
  latest release; the native binary tracks tags."* If it should track releases, point
  `.claude-plugin/marketplace.json` at the tag. Either is fine; the current silence is not.
* **A version that can be ahead should not be spelled like a release.** If the tree is `main`-tracking,
  `0.10.2` is misleading and something like `0.10.2+21` or `0.10.3-dev` is not — the same
  distance-from-tag information `git describe` gives for free.
* **When `tt --version` lands (issue 028), have it name the carrier it is reporting on** — it already
  distinguishes git checkout / source copy / native install per that issue's close. On a box with both a
  native install and a plugin cache, the two answers differ, and that is the fact worth printing.
* **`docs/native.md` or the field-test guidance should state the rule report 088 adopted**: verify against
  the native binary, not the plugin's `tools/tt`, unless unreleased code is the deliberate subject.
* A check is possible and cheap: assert that the plugin cache's `VERSION.txt` corresponds to the recorded
  `gitCommitSha`'s nearest tag, and warn when the distance is non-zero.

## Discussion

### Comment by hmiddelk at 2026-08-21 17:00

Filed from report 088 (`research/reports/report088-windows-update-lifecycle-2026-08-21.md`), which carries
the method, coverage and threats to validity for this batch.

Baseline measured against: released `v0.10.2` native `windows-x86_64` (install `VERSION.txt` = `v0.10.2`,
zip sha256 `7b5fcae61f2cae8da84decd82c7a2420a8a078aef08cf03b14cf730896ae9b7d`, `bin/tt.exe` 41.1M) and
plugin `genscalator@bjornregnell` 0.10.2 at `gitCommitSha` `7f03345`. Named by `VERSION.txt` + hash rather
than `tt --version`, which still exits 2 at this release (**issue 028**). Windows 10 Enterprise
10.0.19045, PowerShell 5.1.

Numbering note: `main`'s highest is 039, but PRs #4, #5 and #6 claim 040-042. This takes **045** assuming
those land; the later PR renumbers per `reqts/issues/README.md`.

This one had **direct cost in the test that found it**, which is the strongest argument for it. The session's
purpose was re-verifying issues 014–022 against v0.10.2, and the plugin tree — freshly installed, declaring
the matching version — contains a runnable `tools/tt` plus every issue file and report. It is the obvious
place to reach, and reaching for it would have tested `which.scala` and `files.scala` as they exist on
`main`, then reported the result as a v0.10.2 verification. The only thing that prevented it was running
the comparison above out of habit inherited from issue 021. Every verdict in report 088's arm B was then
taken from `tt.exe` for this reason, and that is stated in its Method.

A related observation, not filed separately: the superseded `0.9.2` cache tree (551 files) is retained
after the update. It earned its keep here — it is what made **issue 015**'s recovery hint offer *two*
`--skills` candidates and exercise its stale-cache warning for real — so this is recorded as a fact, not a
complaint.

Not verified: macOS or Linux (the mechanism is the marketplace source, so it should be
platform-independent); whether `main`-tracking is deliberate, which is the question that decides this
issue's class; and whether `claude plugin update` behaves differently for a marketplace pinned to a tag.
One more limitation worth stating: `claude plugin marketplace update` does **not** update the installed
plugin — two verbs are needed, and only the second reports a version change. That is harness behaviour
rather than genscalator's, but it is a step a tester will skip, leaving a third version arrangement on the
box.

Agent disclosure: an AI agent (Claude Opus 5) ran the plugin update, made the tag comparison and drafted
this issue under human direction; the human reviewed and submitted.

### Comment by bjornregnell/Fable5 at 2026-08-24 20:13

Merged with the sharpest argument of the batch verified exactly: the pre-merge review re-ran the
comparison `v0.10.2...7f03345` and got ahead 21, behind 0, 79 files, precisely as reported; and the
structural cause checks out — `.claude-plugin/marketplace.json` is `"source": "./"` with no ref, tag
or branch anywhere, while `tools/test/version.test.scala:45-55` gates `plugin.json` and
`marketplace.json` to *agree* with `VERSION`. So the number is guaranteed to agree and guaranteed not
to identify, which is this issue's point stated as a test. The inverse-of-036 reading is right. Three
corrections to the record:

1. **`tools/which.scala` is not among the 79 files** — only `tools/test/which.test.scala` is;
   `which.scala` itself is byte-identical at `v0.10.2` and `7f03345`. The other five named files are
   all there (`versionlib.scala`, `files.scala`, `text.scala`, `git.scala`, `gitinfo.scala`), so the
   list survives, but the later sentence about what reaching for the plugin tree would have tested
   needs the same fix: `files.scala` yes, `which.scala` no. Worth being exact about, since `which` is
   issue 022's subject.

2. **The 21 and 79 are anchored to `7f03345` and grow with every merge.** At review time (2026-08-23,
   `main` = `8060b2d`) the same comparison already gave ahead 25, 80 files, and tonight's merge of
   this batch has grown it again — with `plugin.json` still reading `0.10.2` and `v0.10.2` still the
   newest release. The sha-stamping instinct was right; that the number moves within days is the
   argument, not a problem with it.

3. **The numbering note was stale at merge time** — `main` carried issue 046; 045 was still free, so
   the number stands. The invisible-reservation gap behind it is now a stated rule in
   `reqts/issues/README.md` (`7b580f0`).

The question the issue leaves to the maintainer — whether `main`-tracking is deliberate — stays open
and I will answer it here in the Discussion; writing the acceptance sketch to work under either
answer is why it could merge before that decision. The note about the retained 0.9.2 cache tree
earning its keep on issue 015 is a good one, recorded as a fact rather than a complaint.

Agent disclosure: this comment was drafted by an AI agent (Claude Fable 5) under human direction;
the human reviewed and submitted.

### Comment by bjornregnell/Fable5 at 2026-08-24 21:05

**The deferred question is answered: `main`-tracking was NOT deliberate, and the fix is shipped —
the plugin source is now pinned to the release tag AND its exact sha.** The class decision this
issue was written to survive either way: it is a defect, not a documentation gap.

The mechanism, verified in the plugin-marketplace docs before deciding: a `github` plugin source
supports `ref` (branch/tag) and `sha` (exact commit), with the sha as the effective pin — and,
the sharper half, a declared `version` field gates only the *update cadence*, never the *content*.
So the old `"source": "./"` arrangement meant: users received updates on release-version bumps, but
what they received was `main`'s tree at that moment, labelled with the release number. Your
"guaranteed to agree and guaranteed not to identify" is exactly what the docs' own semantics
produce for that configuration.

What shipped:

* `marketplace.json`'s source is now `{github, bjornregnell/genscalator, ref: "v0.10.2",
  sha: "542b2fd0..."}` — the tag names the intent, the sha proves the bytes.
* A new gate in `version.test.scala`: the `ref` must equal `v` + `VERSION` and a `sha` must be
  present, so a release cut that bumps `VERSION` without moving the pin goes red — the same
  structural move as the agreement gate you cited, extended from "agrees" to "identifies".
* The two channels are now the story a tester can hold: **plugin = released, checkout = bleeding
  edge**. Anyone who needs `main` today installs from a checkout, which is what the maintainer's
  own boxes do.

Left open, honestly: the `version` field still lives in both `plugin.json` and `marketplace.json`
(the docs advise against; our agreement gate makes the hazard moot, but single-sourcing it is fair
future work), and this issue stays open until the fix has survived one real release cut plus a
field-tested `claude plugin update` against the pinned source — the project's own standard: a claim
about behaviour that has not been executed is a guess.

Agent disclosure: the docs verification, the pin change and this comment were produced by an AI
agent (Claude Fable 5) under human direction; the human chose pin-to-tag and reviewed and
submitted.
