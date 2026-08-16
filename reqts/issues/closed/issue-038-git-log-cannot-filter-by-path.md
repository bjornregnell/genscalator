# Issue 038: `tt git log` cannot filter by path, so "did anything touch this file" has no typed answer

> status: closed 2026-08-16, fixed by `7add972` · labels: tools, git, enhancement · summary: `tt git log` has grep/author/committer/
> since/limit filters but no path filter, so the question "which commits touched this file or
> directory" cannot be asked through the typed lane. An auditor needing exactly that answer on
> 2026-08-16 had to settle for reading commit subjects, and honestly flagged its own conclusion as
> weaker than it wanted to be.

## Description

Found 2026-08-16 by a read-only audit agent verifying a handover claim: "file X exists and no commit
after `<sha>` touches it". The first half is a `tt find` call. The second half has no typed shape.

`tools/git.scala` gives `log` these filters (`git.scala:326-344`): `--repo`, `--grep`, `--co-author`,
`--author`, `--committer`, `--since`, `--limit`. All of them filter on commit *metadata*. None filters
on *what the commit changed*. Raw git answers this with a pathspec (`git log -- <path>`), which is
among the most common read-only git questions there is: which commits touched this file, when was this
directory last modified, did anything land here since the release.

The auditor's fallback was to run `tt git log --limit 6` and read the five commit *subjects*, reasoning
that none of them sounded like an edit to the file in question. That is a judgement about prose, not a
measurement, and the audit report had to carry a caveat saying so. The same session had just confirmed
(issue 036's erratum) what a conclusion eyeballed from output is worth compared to one computed by a
command.

The alternatives are all worse:

* **Raw `git log -- <path>`** — the exact reflex `tt git log` was built to retire (SM217, noted at
  `git.scala:321-325`), uncapped and needing a `| head` that trips the guard.
* **`tt git show --ref <ref> --path <relpath>` per commit** — reads one file at one ref; answering
  "which commits touched it" this way means one call per candidate commit, a loop standing in for a
  filter.
* **Not checking** — which is what "verified by commit subjects" quietly rounds to.

## How to reproduce it

```
$ tt git log --repo <repo> --grep 'anything'          # works, metadata filter
$ tt git log --repo <repo> --path notes/some-file.md  # fails: unexpected/incomplete argument '--path'
```

There is no typed way to ask which commits changed a given path.

## Acceptance sketch

* **A `--path <relpath>` flag on `tt git log`**, repeatable like `--grep`, appended to the git argument
  vector after a `--` separator (`git log ... -- <path> [<path>...]`). The separator matters: it keeps a
  path that looks like a ref or a flag from being parsed as one.
* Everything else stays as is: same capped, tab-formatted, one-line-per-commit output; same read-only
  contract; `Bash(tt git log *)` stays allowlist-safe. This is a filter, not a new verb.
* Paths are relative to the repo root, matching `tt git show --path`'s existing convention, and a path
  matching nothing yields the existing `(no matching commits)` rather than an error — git itself does
  not distinguish "path never existed" from "no commits in range touched it" without extra work, and
  the filter should not pretend to.
* Test sketch: in a scratch repo, commit twice touching file A then once touching file B;
  `--path A` returns two rows, `--path B` one, `--path A --path B` three, and combining
  `--path A --grep <pattern>` intersects as expected.

## Discussion

### Comment by bjornregnell/Opus5 at 2026-08-16 11:34

Filed on the maintainer's word, from the cold-start audit of 2026-08-16. Fits the v0.10.3 wave's
read-path family (027, 031): all three are cases where the read lane exists but under-answers, pushing
a user toward a raw shape or a weaker conclusion. Smallest of the three — the parser already handles
repeatable flags (`--grep`), so this is one `case` line, the `--` separator, and a test.

Agent disclosure: gap found by a read-only AI audit agent (Claude Fable 5) that hit it live and flagged
its own workaround as a caveat; drafted by an AI agent (Claude Fable 5) in session with the maintainer,
who ordered the filing.
