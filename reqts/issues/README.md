# In-repo issues — how they work

> **genscalator v0.10.2** — issue-process version. In an older checkout these rules may have moved
> on: check with `tt --version` and update before filing.

genscalator tracks its issues **in the repository** (not on any single forge), so requirements,
decisions and their history travel with the code to every mirror, offline and forge-independent.
The forge trackers stay enabled as an **inbox** for one-time reporters; accepted reports are
transcribed here (with credit). See the pinned "Issue Zero" on any forge tracker for the
outsider-facing version of this text.

## Identity and naming

One file per issue: `issue-YYMMDD-slug.md`, for example `issue-261011-issue-identity.md`.
The issue's ID is the part between `issue-` and `.md`: `261011-issue-identity`.

* `YYMMDD` is the **UTC** date the issue was conceived (first written, even as a draft on a
  branch). UTC, not local time, so that one clock orders every collaborator's issues.
* The slug is 1 to 4 lower-case words of letters and digits, separated by `-`. It says roughly
  what the issue is about, and it is frozen at inception even if the title changes later.
* **Mint it with the tool, never by hand:** `tt issue new <slug> [--title <text>]` takes today's
  UTC date, creates `open/issue-YYMMDD-slug.md` from the house template, and prints its path.
  It refuses a bad slug, and an ID that already exists in `open/` or `closed/`.
* An ID needs no coordination: it does not depend on other branches, forks or open PRs, and it
  never changes, through filing, merge, retitling and closing. A clash needs the same UTC day
  AND the same slug, and it is loud: `tt issue new` refuses locally, and two branches adding the
  same file conflict at merge, where renaming one slug fixes it.
* **Cite the full ID**, date and slug: "issue 261011-issue-identity". The bare date is not
  unique, so a citation always carries the slug.
* Sorting the files by name lists them in the order they were conceived, by day; issues from
  the same day sort by slug.

### Legacy numbers

Issues 000 to 073 keep their three-digit numbers (`issue-NNN-name.md`, cited as "issue 032").
Those numbers are **frozen**: 073 is the last numbered issue, and no new number is ever handed
out (`tt issue next` is retired and points at `tt issue new`). Legacy files start with `0` and
new ones with `2`, so a plain name sort shows the legacy block first. `tt issue list` and
`tt issue close` accept both forms (`close 23`, `close 073`, `close 261011-issue-identity`).
Why the counter was dropped: issue 073.

### Out of scope

This scheme is for the in-repo issues only. The forge trackers' own issue numbers (such as
Issue Zero on GitHub) are the forge's, and the single-writer counters of the closed work repo
(SM, WR, RT) stay as they are. Do not migrate them.

## Open vs closed

* The `open/` and `closed/` directories are the **single source of truth** for the basic
  open-or-closed fact. Closing an issue = `git mv` its file from `open/` to `closed/`.
* The preamble (below) carries only FINER states (triage, in-progress, blocked, wontfix, ...),
  so the directory and the preamble can never disagree about open vs closed.

## Issue file syntax

* The `#` heading is a readable version of the file name.
* After the heading, a block-quote preamble declares finer status, labels, and a one-line
  summary. (The exact preamble field syntax is still settling; keep it short and scannable.)
* Then these `##` sections (`tt issue new` writes them for you):
  * `## Description` (mandatory)
  * `## How to reproduce it` (optional)
  * `## Acceptance sketch` (optional)
  * `## Discussion` — an append-log; each entry is
    `### Comment by userhandle at YYYY-MM-DD HH:MM`

## Study reports and provenance

An issue born from a study or field test cites its report in `## Description`; the batch
provenance (method, coverage, threats to validity) lives in [`research/reports/`](../../research/reports/)
as `reportNNN-short-name.md`, continuing that one number series. Reports do NOT live beside the
issues: one report typically spawns many issues, and the relationship is carried by links in both
directions (the issue cites its report, the report lists the issues it filed). The invariant:
**the issue must still stand alone** — reproduction steps go in the issue itself, never only in
the report. Live example: issues 014–022 and reports 085–086.

## Contributing an issue

Fork + clone, create your file with `tt issue new <slug> --title <text>` (it lands under
`open/` with a fresh ID, per the naming rules above), fill it in, and open a Pull Request.
Without `tt`, name the file `issue-YYMMDD-slug.md` yourself, using today's UTC date. There is
no number to claim, so open PRs need no checking. Prefer this route if you can; the forge
inbox exists for everyone else. More in the repo root `CONTRIBUTING.md`.

In the issue, **state the genscalator version you measured against** (`tt --version`): a gap
report can only be re-checked against current `main` when it names its baseline. Reports 085,
086 and 087 all did this, and it is why 087 could be re-checked before filing.