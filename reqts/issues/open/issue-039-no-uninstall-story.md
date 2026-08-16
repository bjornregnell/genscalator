# Issue 039: there is no uninstall story, so "test from a clean box" is not reachable twice

> status: open · labels: installer, docs, release, windows, alpha · summary: nothing removes what
> the installer put on a box — no `--uninstall`, no README section — so a tester cannot return to
> the naked state that alpha field-testing (and the planned real-Windows day) depends on, and every
> "clean install" after the first is silently not one.

## Description

Raised by the maintainer 2026-08-16, planning the real-Windows day on a naked box: *"we need to
develop an uninstall story in README and the mechanisms to remove the local installed stuff, this
is esp important if a user wants to test a new version of genscalator from a clean 'naked' box
state."*

The gap has two halves:

**1. No mechanism.** `get-genscalator.sc` installs; nothing uninstalls. A tester who wants to
re-run the newcomer path from zero must hand-reverse the install from memory: find the install
dir, find what the PATH gained, find what the guard hook added to settings. Every step they miss
makes the next "clean install" a dirty one, and the dirt is invisible — the second install will
mostly work, which is worse than failing, because it tests a state no real newcomer has.

**2. No story.** The README walks a user in but never out. For an alpha explicitly soliciting
field tests (reports 085-087), the way OUT is part of the test harness: install → test →
uninstall → reinstall-new-version is the loop every version-comparing tester runs, and today the
loop's third step does not exist.

## Design decision: an option on `get-genscalator.sc`, not a second script

Maintainer preference, agent concurring — `tt uninstall` or a separate `uninstall-genscalator.sc`
both lose to a `get-genscalator.sc --uninstall` flag:

* **One carrier for the layout knowledge.** The script that installed things is the artifact that
  knows where they are. A separate uninstall script duplicates the install-layout knowledge into a
  second carrier that rots independently — the same carrier-staleness class as issues 034/036.
* **No added download weight** (the maintainer's argument): the uninstaller travels inside the
  thing every installer-user already fetches.
* **The bootstrap works in both directions.** The script is fetched fresh, so uninstall is always
  available even after the install dir is gone — including for reversing a BROKEN install, which a
  `tt uninstall` verb could never do.
* Not `tt uninstall`: `tt` excludes destructive verbs BY DESIGN (that is what makes it
  allowlistable); the lifecycle script is the right home for the one destructive operation the
  project needs, behind a preview.

The fetched-fresh property carries an obligation: a NEWER `get-genscalator.sc` must be able to
uninstall an OLDER install. Assuming the current layout is not enough.

## Acceptance sketch

* **The installer writes a manifest at install time** — a plain list of every file/dir it created
  and every mutation it made (PATH entry, hook registration), stored in the install root.
  `--uninstall` reads the manifest and removes exactly what it lists; a well-known-paths fallback
  covers pre-manifest installs (and says so).
* **Preview-default, like every destructive shape in this project:** `--uninstall` PRINTS what it
  would remove and exits; actually deleting takes an explicit second flag (e.g.
  `--uninstall --force`), mirroring the release-delete pattern.
* **Print, do not edit, human-owned files:** PATH lines in shell rc files and guard-hook entries
  in `settings.json` are reported with exact remove-this instructions, never auto-edited — the
  installer may have added them, but the files belong to the human and may have been hand-tuned
  since.
* **A README "Uninstall" section**, short, next to the install section: the one command, what it
  removes, what it deliberately leaves for your hands (the settings/rc lines), and that
  install → uninstall → reinstall is the supported version-testing loop.
* **The round trip is a test:** on the planned real-Windows naked-box day, run
  install → uninstall → verify-naked → reinstall as a checklist item; on CI or locally, a scripted
  round trip in a scratch HOME asserting the manifest removes everything it created (dir empty,
  PATH report emitted).
* Out of scope, stated to prevent creep: uninstall never touches user repos, user work, or
  anything absent from the manifest/fallback list.

## Discussion

### Comment by bjornregnell/Opus5 at 2026-08-16 11:59

Filed on the maintainer's word during v0.10.3 wave planning; the maintainer proposed the need and
the flag-on-installer shape, the agent added the manifest mechanism, the preview default, and the
print-don't-edit boundary. Natural sequencing: lands well with the version family (028/036),
since "what version am I removing/reinstalling" and "what version is this box on" are the same
newcomer conversation — but it is installer work, not tools/*.scala, so it does not ride the
wave's single native-rebuild constraint.

Agent disclosure: drafted by an AI agent (Claude Fable 5) in session with the maintainer, from
the maintainer's stated need and design preference.
