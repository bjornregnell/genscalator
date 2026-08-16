# Issue 036: nothing tells an issue filer their genscalator is out of date, so stale policy is followed correctly and silently

> status: closed 2026-08-16, fixed by `7add972` · labels: docs, contributing, release, agent-trust, alpha · summary: a contributor
> working from an old checkout reads old `CONTRIBUTING.md` and old source, and nothing in the repo or
> the toolbox says so. Observed twice on 2026-08-15: a false gap report from 79-commits-old source,
> and a `CONTRIBUTING.md` that sends contributors to the wrong forge.

## Description

Found 2026-08-15 while rescuing the report that became issue 035 from an old clone of this repo.

Two failures, same root, both observed rather than imagined:

**1. A false gap report from stale source.** The reporting session read `tools/git.scala` in a clone 79
commits behind and concluded that `tt git` has "no way to push" a tag. `--tags` has shipped since before
v0.10.0 and this project's own release dance depends on it. The string `tags` appears **27 times** in that
file on current main and **zero times** in the clone's copy. The report was accurate about the source it
read. Nothing in reading a checkout tells you the checkout is old: the file is there, it parses, it is
internally coherent, it simply predates the feature.

**2. Stale policy followed correctly, pointing outward.** The same clone's `CONTRIBUTING.md:68` says
*"Fork https://codeberg.org/bjornregnell/genscalator"*. Current main says github.com. Codeberg is now a
mirror. **A contributor working from that clone would fork, branch and open a PR on the wrong forge,
having followed their local instructions completely and correctly**, and nothing would tell them until a
human noticed.

The second is the dangerous one. A wrong technical claim in an issue gets refuted in review, loudly and
cheaply. A contribution that lands on the wrong forge, or a copyright-transfer email sent to a stale
address, fails **silently and outward** — the contributor did everything right and the work goes where
nobody is looking. **Outward-pointing policy is the staleness that costs most.**

This is an alpha-stage concern specifically because the alpha wants reports from people who are *not*
tracking main. Every field-test report so far has paid some version tax: reports 085 and 086 could only
identify their build by binary mtime, and issue 028 was filed because there is still no way to ask the
binary what it is.

## How to reproduce it

In any checkout that is behind:

```
$ tt text match <old-clone>/CONTRIBUTING.md 'codeberg|github'   # points at the old forge
$ tt text match <old-clone>/tools/git.scala 'tags'              # 0 matches; current main has 27
$ tt version                                                     # exit 2, issue 028
```

Nothing in the repo, the toolbox or the docs reports that the checkout is behind.

## Acceptance sketch

The goal is that **a stale carrier announces itself**, since a reader cannot be relied on to wonder.

* **Stamp the policy carriers with a version, the way `AGENTS.md` already is.** `AGENTS.md:3` carries
  `**genscalator vX.Y.Z**` and `tools/test/version.test.scala:52` asserts it against `VERSION.txt`, so the
  mechanism exists and is tested. Extend the same banner and the same assertion to `CONTRIBUTING.md` and
  `reqts/issues/README.md`. A reader then sees the vintage without asking, and the release gate keeps it
  honest.
* **Say it in the contributor path.** One line near the top of `CONTRIBUTING.md`: check your version and
  update before filing, with the two commands. This depends on issue **028** (`tt --version`) to be
  answerable at all, which is the second argument for that issue and arguably a better one than
  discoverability: **without a version, neither the filer nor the maintainer can tell whether a gap report
  describes the shipping tool or a checkout from three releases ago.**
* **Consider a staleness note from the toolbox itself.** `tt update --native` already compares the
  installed `VERSION.txt` against the published release tag, so the comparison exists. A cheap, non-nagging
  form: when `tt --version` lands, have it report the installed version and, if a cached check is
  available, whether it is behind. Deliberately **not** a network call on every invocation.
* **An issue-filing checklist line**, in `reqts/issues/README.md` under "Contributing an issue": state the
  version you measured against. Reports 085, 086 and 087 all did this voluntarily and it is why 087 could
  be re-checked against `main` before filing.

## Discussion

### Comment by bjornregnell/Opus5 at 2026-08-15 20:33

Filed on the maintainer's ask, from the WR225 episode: *"the cause might be that the user is running an
old genscalator version and therefore uses stale contrib instructions and policies that might have
changed, so we need to find a way to remind issue filers to update."*

Checked before filing, and the check is worth recording because it moved the conclusion. The hypothesis
does **not** explain the misfiled location in that specimen: at the 79-behind revision the clone
**already** had `reqts/issues/open/` and its `CONTRIBUTING.md:17` **already** prescribed
`reqts/issues/open/issue-NNN-...`. So that filing was a non-conformance despite correct local docs, not a
victim of stale policy. The hypothesis was then confirmed on a different and more consequential line, the
fork target, as described above. Reasonable hypothesis, wrong instance, worse instance found underneath.

Relationship to neighbours: depends on **028** for the version primitive; shares a root with **034**
(a guard comment asserting a verb does not exist, two weeks after it shipped) and with the ember's own
staleness. Three hazards in one week, all the same shape — **a carrier that cannot say how old it is**.

Agent disclosure: found and drafted by an AI agent (Claude Opus 5) in session with the maintainer, who
proposed the underlying cause and asked for the remedy.

### Comment by bjornregnell/Opus5 at 2026-08-16 11:22

**Erratum: the count of `tags` in `tools/git.scala` on current main is 58, not the 27 stated above** (in
the Description and again in "How to reproduce it"). Measured with `tt text count tools/git.scala 'tags'`
→ **58** (45 matching lines, 16 of them the `--tags` flag). Re-verified on `main` at `dfbcc40` before
writing this comment. The stale clone's **0** is correct and was measured. The Description is left
unedited on purpose, per house practice: corrections are appended, not retrofitted.

The provenance of the wrong number belongs in this issue rather than in a silent fix, because it is the
same failure this issue is about, one level up. It was **not** copied from the stale clone. The filer ran
`tt text match`, which prints matching *lines*, eyeballed the output, and asserted a total never computed
by any command — then carried that total into two artifacts before anyone re-ran it. A number stated
without running a counting command is an adjective wearing a number's clothes.

So the count is a fourth instance of this issue's root, and the only one where the carrier that could not
say how stale it was, was the writer. The other three named above (the ember, the guard comment in 034,
the 79-behind clone) at least had a version to compare against. An unverified number has none.

Agent disclosure: erratum found by an AI auditor re-running the count, appended by an AI agent
(Claude Opus 5) in session with the maintainer.
