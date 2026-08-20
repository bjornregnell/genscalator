# Issue 040: the uninstall fallback list drifts from the shipped zip, so a pre-manifest uninstall leaves `reqts/` behind and blames the user for it

> status: open 2026-08-20 · labels: installer, uninstall, release, alpha · measured against: v0.10.2
> (from `.genscalator/VERSION.txt`; `tt --version` postdates this release, see issue 028) · summary:
> `--uninstall`'s well-known-paths fallback lists `skills`/`tools`/`plugins`, which the release
> workflow deliberately does NOT ship, and omits `reqts`, which it deliberately DOES — so
> uninstalling a pre-manifest install leaves `reqts/PRD.md` on the box and then reports the leftover
> as a file "this uninstaller did not put there", which is false, and is the invisible-dirt state
> issue 039 was written to kill.

## Description

Found while running issue 039's install → uninstall → reinstall loop on a Linux box carrying a
pre-039 install (a stale `get-genscalator.sc`, so no `INSTALL-MANIFEST.txt`). The uninstall took the
fallback path, as designed, and under-cleaned.

`get-genscalator.sc:223`:

```scala
val wellKnown = Vector("bin", "docs", "skills", "tools", "plugins", "VERSION.txt")
```

What `.github/workflows/native-release.yml:169-175` actually stages into the zip:

```
bin/            docs/            reqts/PRD.md            VERSION.txt
```

Set against set:

| entry | in the shipped zip | in the fallback list | effect |
| --- | --- | --- | --- |
| `bin`, `docs`, `VERSION.txt` | yes | yes | correct |
| `reqts` | **yes** | **no** | **under-cleans — file survives** |
| `skills`, `tools` | no (excluded on purpose) | yes | dead entry, filtered by `Files.exists` |
| `plugins` | no (never a top-level dir) | yes | dead entry, filtered by `Files.exists` |

The dead entries are harmless: `readManifest`'s fallback filters on `Files.exists`, so naming a path
that was never shipped costs nothing. The omission is not harmless.

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

* **Fix the list**: add `reqts`; drop `plugins` (never shipped) and, if D3/D4 are settled, `skills`
  and `tools` too. Only the `reqts` addition is correctness — the removals are hygiene, so the list
  stops implying a payload shape the project has decided against.
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
