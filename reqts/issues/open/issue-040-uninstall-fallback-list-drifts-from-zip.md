# Issue 040: the uninstall fallback list drifts from the shipped zip, so a pre-manifest uninstall leaves `reqts/` behind and blames the user for it

> status: open 2026-08-20 · labels: installer, uninstall, release, alpha · measured against: v0.10.2
> (from `.genscalator/VERSION.txt`; `tt --version` postdates this release, see issue 028) · summary:
> `--uninstall`'s well-known-paths fallback lists `skills`/`tools`/`plugins`, which the release
> workflow deliberately does NOT ship, and omits `reqts`, which it deliberately DOES. The literal is
> therefore wrong in **both** directions. It **under-cleans**: uninstalling a pre-manifest install
> leaves `reqts/PRD.md` on the box and then reports the leftover as a file "this uninstaller did not
> put there", which is false, and is the invisible-dirt state issue 039 was written to kill. And it
> **over-cleans**: `:217-219` promises the fallback "only claims the directories this installer has
> ever created", but no release ever shipped `skills`, `tools` or `plugins`, so a user who made
> `~/.genscalator/tools/` themselves loses every file in it on `--uninstall --force` — the exact loss
> that sentence exists to rule out.

## Description

Found while running issue 039's install → uninstall → reinstall loop on a Linux box carrying a
pre-039 install (a stale `get-genscalator.sc`, so no `INSTALL-MANIFEST.txt`). The uninstall took the
fallback path, as designed, and under-cleaned.

`get-genscalator.sc:223`:

```scala
val wellKnown = Vector("bin", "docs", "skills", "tools", "plugins", "VERSION.txt")
```

What the release workflow actually puts in the zip, from two separate steps —
`.github/workflows/native-release.yml:155` writes the binary (`--out "staging/bin/tt${{ matrix.exe }}"`),
and the staging step at `:171-176` adds the rest:

```
bin/            docs/            reqts/PRD.md            VERSION.txt
```

Set against set:

| entry | in the shipped zip | in the fallback list | effect |
| --- | --- | --- | --- |
| `bin`, `docs`, `VERSION.txt` | yes | yes | correct |
| `reqts` | **yes** | **no** | **under-cleans — file survives** |
| `skills`, `tools` | no (excluded on purpose) | yes | **over-cleans — claims a directory only the user can have created** |
| `plugins` | no (never a top-level dir) | yes | **over-cleans — same** |

**Both directions are correctness, not one.** An earlier draft of this issue said the surplus entries
were harmless on the grounds that the fallback filters on `Files.exists`, so naming a never-shipped
path costs nothing. That is wrong, and it is wrong in the only case that matters. `:223-231` resolves
each entry, filters on `Files.exists`, and for anything that *is* a directory walks it and collects
**every regular file** beneath it. The filter therefore saves you exactly when the entry is absent —
which is the case where naming it was already free. When the entry is present, it can only be present
because the user created it, and the walk claims all of it.

Set against the contract three lines above, at `:217-219`:

> The fallback is deliberately narrow: it only claims the directories this installer has ever created,
> never the whole install root, so a user who put something of their own in there does not lose it.

`skills`, `tools` and `plugins` are not directories this installer has ever created — nothing in
`get-genscalator.sc` creates them, and their only two appearances in the file are the vector at `:223`
and the warning string at `:265`. So the literal contradicts its own stated contract as written,
before any user's box is considered. The repo already records one instance: closed issue 015 exists
because `~/.genscalator/skills` is **absent** on a native install (`skillcheck: not a skills
directory: /home/<user>/.genscalator/skills`).

Concretely: a user who keeps their own scratch scala in `~/.genscalator/tools/` loses all of it to
`--uninstall --force`, silently, and the `kept:` line will not mention it because the directory is
gone. That is a data-loss path, and it ranks above the leftover `reqts/PRD.md` this issue was filed
for.

### Why the message makes it worse than a plain miss

Because a file survives, the `if Files.exists(home)` branch fires and reports the leftover as the
user's own. A tester reading `it holds files this uninstaller did not put there` has been told, in
so many words, *not* to look — the box is dirty and the tool has vouched for it. The next install
lands on a state no real newcomer has, "mostly works", and issue 039's central argument plays out
verbatim:

> the dirt is invisible — the second install will mostly work, which is worse than failing, because
> it tests a state no real newcomer has.

039 built the manifest to end exactly this, and on the manifest path it does. The fallback path —
039's own provision for installs that predate the manifest, i.e. **every alpha tester's box** —
reintroduces it.

### Root cause: the list was written from the repo layout, not the staged layout

`skills`, `tools` and `reqts` all read as plausible top-level names to anyone looking at a checkout
(`skills/` and `tools/` are real repo directories; `plugins/` is not, and looks like a guess). But
the uninstaller does not operate on a checkout — it operates on the unpacked zip, and the workflow
is explicit that the two differ:

```yaml
# NO tools/*.scala: checked 2026-07-27, not one runtime verb reads it (DESIGN D3).
# NO skills/: the Claude Code plugin owns those, and shipping a second copy here is the
# drift failure D4 exists to prevent.
```

So the fallback list is a hand-maintained second copy of the payload layout, kept in a different
file from the one that defines it — the carrier-staleness class of issues 034 and 036. Sharper
still: the list names `skills` **because** it was copied from the repo, and the workflow line
excluding `skills` cites "the drift failure D4 exists to prevent". The fallback is an instance of
the drift the line immediately above it warns against.

This is also why 039's acceptance sketch did not catch it. That sketch asks for

> a scripted round trip in a scratch HOME asserting the manifest removes everything it created

— the **manifest** path. The fallback path has no equivalent assertion, and it is the path whose
correctness rests on a literal that nothing checks.

## How to reproduce it

Any install made by a `get-genscalator.sc` from before 039 will do; the point is only that
`INSTALL-MANIFEST.txt` is absent. On a box with no genscalator installed:

```bash
# 1. install with a PRE-039 script (no manifest is written)
scala-cli run <pre-039>/get-genscalator.sc
#    => "unpacked: 30 file(s) into ~/.genscalator", and no "manifest:" line

# 2. uninstall with a CURRENT script, fetched fresh as designed
curl -fsSLO https://raw.githubusercontent.com/bjornregnell/genscalator/main/get-genscalator.sc
scala-cli run get-genscalator.sc -- --uninstall --force

# 3. look
find ~/.genscalator -type f
```

Observed at step 2 — note `30` unpacked at step 1 against `29` removed here:

```
  ⚠ NO INSTALL-MANIFEST.txt found — this install predates manifests, [...]
  removing: 29 file(s)
  kept:     /home/hans/.genscalator still exists — it holds files this uninstaller did not put there
```

Observed at step 3:

```
/home/hans/.genscalator/reqts/PRD.md
```

Expected: the install root is empty and removed, as it is when a manifest is present. Equivalently,
an install and an uninstall in the same session should not disagree about the file count.

Shortcut for anyone without a pre-039 script: install with a current one,
`rm ~/.genscalator/INSTALL-MANIFEST.txt`, then uninstall — the fallback triggers on the file's
absence.

## Acceptance sketch

* **Fix the list, in both directions, and treat both as correctness**: add `reqts`; drop `plugins`,
  `skills` and `tools`. The removals are not hygiene — they are what stops the fallback claiming
  directories only the user can have created, which is what `:217-219` promises it will not do. The
  D3/D4 question does not gate them either, because no release has ever staged any of the three (see
  the release-history check in the Discussion below), so removing them cannot under-clean any install
  that exists.
* **Render the warning from the same vector.** `:265` prints `WELL-KNOWN PATHS (bin, docs, skills,
  tools, plugins, VERSION.txt)` as a hand-typed string, 42 lines from the vector it paraphrases. A fix
  to `:223` that misses `:265` leaves the warning describing a payload the code no longer uses — the
  same carrier-staleness this issue is about, one file away from itself. It is also the cleanest
  citation available for issue 041: the drift has a second copy inside the very file that documents
  it.
* **Make drift fail the build, not the uninstall.** The list's job is to describe the staged
  payload, so CI should assert exactly that: after the staging step, every top-level entry in
  `staging/` appears in the fallback vector. A release that adds a payload directory without
  touching the list should go red. Without this the divergence returns the next time staging
  changes, and the next discoverer is again a tester holding a dirty box.
* **Round-trip the fallback, not just the manifest**: extend 039's scratch-HOME round trip with a
  case that deletes `INSTALL-MANIFEST.txt` before uninstalling and asserts the install root ends up
  empty. That one case is what would have caught this.
* **Do not let `kept:` overclaim.** The branch should distinguish "entries I did not recognise" from
  "your files". On the fallback path it cannot know the difference, so it should say the weaker,
  true thing — unrecognised entries remain, *and* the fallback may be incomplete — rather than
  asserting a provenance it has no record of. The `⚠ NO INSTALL-MANIFEST.txt` warning is already
  printed above; the `kept:` line contradicts it by sounding certain.
* Out of scope: any change to what the release stages. This is about the uninstaller's model of the
  payload disagreeing with the payload, and the fix belongs on the uninstaller's side.

## Discussion

### Comment by hmiddelk at 2026-08-20 10:50

Reported from a real v0.10.2 field test on Linux, not from reading the code: the box carried a
pre-039 install, `--uninstall --force` reported success, and `reqts/PRD.md` was still there
afterwards. The `30 unpacked` against `29 removed` is what made the miss visible at all — without
those two numbers in one session's scrollback, the `kept:` line reads as normal and the leftover is
never found. Which is an argument for the count assertion in the acceptance sketch: the signal
existed here only by accident of having installed minutes earlier.

Not fixed here; this PR adds the issue only. The one-word fix (`+ "reqts"`) is not the interesting
part and lands better with the CI assertion that keeps the list honest, since shipping the fix alone
recreates the same silent-drift setup for whoever changes staging next.

Agent disclosure: drafted by an AI agent (Claude Opus 5) in session with hmiddelk, from a failure
the human hit while exercising issue 039's loop. The agent verified the divergence against
`native-release.yml`'s staging step and against the `INSTALL-MANIFEST.txt` of a post-039 install on
the same box. Not verified: behaviour on macOS or Windows, or against any release other than
v0.10.2.

### Comment by hmiddelk at 2026-08-20 15:20

**The acceptance sketch above asks for the weaker of two fixes.** Issue 041 names the distinction:
this repo already runs both strategies against carrier drift — *assert* that two hand-maintained
carriers agree (`version.test.scala:73`) versus *derive* one from the other so agreement is
structural (`skillcheck.scala:9`, "so it never drifts"). Bullet two above ("CI should assert ...
every top-level entry in `staging/` appears in the fallback vector") is the assert form. The derive
form is available and stronger: make the literal a **build product of the staging step** —
generated, committed, and CI-verified against a regeneration — so the staging step becomes the
single source of the payload layout and a new payload directory cannot be forgotten, only
re-generated.

**But deriving is not as clean here as it first looks, and the reason is 039's own obligation.** A
list generated from *current* staging describes the *current* payload, while the fallback exists
precisely to uninstall an *older* one. A directory that shipped in 0.9.x and was dropped since would
be absent from a freshly generated list and silently left behind — the same class of miss as this
issue, arriving from the opposite direction. So the generated list has to be **append-only** (a
union across every payload ever shipped, entries added and never removed), which also settles the
hygiene half of bullet one: `skills`/`tools`/`plugins` should be *kept* if they were ever staged,
and only `plugins` — never a top-level dir in any release — is genuinely spurious. Worth deciding
before the one-word fix lands, because "drop the unused entries" and "never remove an entry" are
opposite instructions and the current sketch implies the first.

**A cheaper route to exactness, and a defect it exposes.** The uninstaller does not actually need to
guess the payload for a pre-manifest install, because the installed tag is sitting on disk. Line 232
hardcodes it:

```scala
Manifest(wellKnown, None, "unknown", fallback = true)
```

while line 223 lists `VERSION.txt` in the very same expression's well-known set, and line 359's own
comment states "the archive carries the CI-stamped tag in `VERSION.txt`". So the fallback path
prints `version: unknown` while holding the file that answers the question — which is why the
reproduction above shows `version: unknown` on a box whose `VERSION.txt` read `v0.10.2`. Reading it
would let the fallback name the tag it is removing, and *optionally* fetch that tag's zip and use
its entry list as an exact manifest.

That last step is a judgement call, not a recommendation: a 14.8 MB download to uninstall, and it
fails offline, which is a poor trade for a cleanup operation. **The version misreport is a defect
regardless**, independent of both the derive question and the fetch idea — the uninstaller should
say which version it is removing when the answer is on disk. Small, self-contained, and a better
first commit than the one-word list fix.

Agent disclosure: drafted by an AI agent (Claude Opus 5) in session with hmiddelk, following
hmiddelk's framing in issue 041. Verified: lines 223, 232 and 359 of the current
`get-genscalator.sc`; that `VERSION.txt` is staged by `native-release.yml` and was present in the
v0.10.2 payload on the reporting box. Not verified: which directories earlier releases staged (the
append-only claim above is an argument for checking release history, not a record of it).

### Comment by hmiddelk at 2026-08-20 15:33

The version misreport described in the comment above is now **issue 042**, and it turned out to be
wider than that comment says. Two corrections, so this log does not mislead whoever picks up either
issue:

* **It is not specific to the fallback path.** The comment scoped it there on the strength of line
  232's hardcoded `"unknown"`. Running `--uninstall` against a *manifest-bearing* install prints
  `version: latest`, because `writeManifest` stores `tag.getOrElse("latest")` — the requested tag
  rather than the resolved release. Same symptom, unrelated cause, on the path assumed correct. The
  fallback is the louder failure; the manifest path is the one still wrong after the fallback is
  fixed.
* **It is not a candidate first commit for this issue.** Being present on both paths makes it
  independent of the fallback list entirely, so it belongs to 042 and should not be bundled here.

What remains in scope for 040 is unchanged: which files the fallback removes, and whether that list
is asserted against the staged payload or derived from it. The `kept:` overclaim in the acceptance
sketch also stays here, since it is caused by the leftover rather than by the version label.

### Comment by hmiddelk at 2026-08-24 21:10

Three corrections and one confirmation, all from checking rather than reasoning. The first retracts
the central argument of the 15:20 comment above.

**1. The append-only requirement does not exist. I checked the release history and it is empty.**

The 15:20 comment closes by admitting the claim was unverified — "an argument for checking release
history, not a record of it". Checked now, against the workflow's full history:

* Seven revisions of the staging step exist. **Every one stages exactly `docs/`, `reqts/PRD.md` and
  `VERSION.txt`**, with `bin/` arriving separately from the build step at `:155`. No revision has
  ever staged `skills/`, `tools/` or `plugins/`.
* Native releases begin at **v0.10.0**. The workflow's first revision is `1cc7d2f`, 2026-07-27, one
  day earlier — so there is no pre-v0.10.0 native payload for the fallback to be pointed at.
* **v0.10.0, v0.10.1 and v0.10.2 staged an identical set**, and there is no v0.10.3 or later.
* The single variant anywhere in the history is `1cc7d2f` writing `staging/VERSION` without the
  `.txt`, and it never shipped: v0.10.0 already wrote `VERSION.txt`.

So the union of top-level entries across **every payload the fallback can ever meet** is
`{bin, docs, reqts, VERSION.txt}` — exactly the current staging set. Three consequences:

* "Drop the unused entries" and "never remove an entry" are not opposite instructions here, because
  the entries to drop were never in any payload. The 15:20 comment presented that as a live tension
  and there is none. The real tension is the one the acceptance sketch now states: the surplus
  entries are an over-clean, so removing them is correctness rather than hygiene.
* Deriving the list from *current* staging is sufficient, not merely convenient, because the union
  has not moved across three releases. The objection that a generated list would silently drop a
  directory an older payload shipped describes no release that exists.
* What survives of the instinct is narrower and worth keeping: the derived list should be
  **generated, committed, and CI-verified against a regeneration**, so that a future *removal* from
  staging shows up as a diff a human reads rather than a silent narrowing. That is the useful half
  without the false premise, and it is compatible with rendering `:265` from the same vector.

**2. The `native-release.yml` citation was wrong.** The Description said `:169-175`. Correct is
`:171-176` — `:169` is `shell: bash` and `:170` is `run: |`, and `:175-176` is the two-line "NO
skills/" comment that `:169-175` truncated mid-sentence. Corrected above, along with the fact that
`bin/` is not in that block at all: it comes from `:155`. Four staged entries were cited to one range
that produces three of them.

**3. The 30-unpacked / 29-removed numbers can be derived, and they hold.** They were reported from
one session's scrollback. From the tree at `v0.10.2`: `docs/` is 27 files, plus `bin/tt`,
`reqts/PRD.md` and `VERSION.txt` = **30**; the fallback removes everything but `reqts/PRD.md` = **29**.
So both numbers are forced by the payload rather than merely consistent with the code, and the
count-disagreement assertion the sketch asks for has a known-correct expectation to assert against.

**4. Partly closing the Windows gap.** The Description says the behaviour is unverified off Linux.
The *payload shape* is now verified on Windows 10: a native install there holds `bin`, `docs`,
`reqts`, `VERSION.txt` (plus `INSTALL-MANIFEST.txt`), so `reqts/` is in the payload on that platform
too and the omission is platform-independent. Still **not** verified on Windows: the fallback
uninstall itself, and therefore the leftover and the `kept:` message. Narrowed rather than removed.

Agent disclosure: drafted by an AI agent (Claude Opus 5) in session with hmiddelk, prompted by
bjornregnell's review of PR #4. The agent verified against `main` at `8060b2d` and the repo's tag
history: the seven staging-step revisions and what each stages; the v0.10.0/v0.10.1/v0.10.2 payload
sets; that `1cc7d2f`'s `staging/VERSION` never shipped; the `:171-176` line boundaries; the 27-file
`docs/` count at `v0.10.2`; that nothing in `get-genscalator.sc` creates `skills`/`tools`/`plugins`;
and the install-root contents on the reporting Windows box. NOT verified: the fallback uninstall on
Windows or macOS, and the over-clean data loss was read from `:223-231` rather than reproduced —
no directory was actually destroyed to confirm it.
