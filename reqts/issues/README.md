# In-repo issues — how they work

genscalator tracks its issues **in the repository** (not on any single forge), so requirements,
decisions and their history travel with the code to every mirror, offline and forge-independent.
The forge trackers stay enabled as an **inbox** for one-time reporters; accepted reports are
transcribed here (with credit). See the pinned "Issue Zero" on any forge tracker for the
outsider-facing version of this text.

## Identity and naming

One file per issue: `issue-NNN-short-snake-case-name.md`

* `NNN` is a three-digit number, monotonically increasing, never reused. Next free number =
  highest existing (across `open/` AND `closed/`) plus one.
* The name part is lower-case words separated by `-`.
* Example: `issue-000-how-to-make-issues-on-genscalator.md`

## Open vs closed

* The `open/` and `closed/` directories are the **single source of truth** for the basic
  open-or-closed fact. Closing an issue = `git mv` its file from `open/` to `closed/`.
* The preamble (below) carries only FINER states (triage, in-progress, blocked, wontfix, ...),
  so the directory and the preamble can never disagree about open vs closed.

## Issue file syntax

* The `#` heading is a readable version of the file name.
* After the heading, a block-quote preamble declares finer status, labels, and a one-line
  summary. (The exact preamble field syntax is still settling; keep it short and scannable.)
* Then these `##` sections:
  * `## Description` (mandatory)
  * `## How to reproduce it` (optional)
  * `## Discussion` — an append-log; each entry is
    `### Comment by userhandle at YYYY-MM-DD HH:MM`

## Contributing an issue

Fork + clone, add your file under `open/` with the next free `NNN`, and open a Pull Request.
If two PRs claim the same `NNN`, the later one renumbers. Prefer this route if you can; the
forge inbox exists for everyone else. More in the repo root `CONTRIBUTING.md`.