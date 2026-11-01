# Issue 261101-git-log-date-gaps: tt git log has no --until and prints no dates, so dating a commit takes a bisection

> status: open 2026-11-01 · labels: tools, git, enhancement · measured against: main at `a07c032` ·
> summary: `tt git log` takes `--since` but not `--until`, and prints sha, author and headline with no
> date. "When was commit X made?" therefore has no one-call answer: it takes pairs of `--since` queries
> narrowing in on the day, and the alternative is a raw `git log --format=%ad`, which is exactly the
> reach the typed verb exists to replace.

## Description

`tt git log --repo <dir> [--grep P] [--co-author P] [--author P] [--committer P] [--since D] [--limit N]
[--path <relpath>]...` is the typed commit search. Two gaps, one need ("when did this happen"):

1. **No `--until`.** A date WINDOW cannot be asked for. `tt git log ... --until 2026-09-11` is rejected
   as `unexpected/incomplete argument '--until'`.
2. **No date in the output.** Each line is `<short sha> <author email> <headline>`. Even when the
   right commit is listed, the output does not say when it was made.

Either one alone would have made the case below one call per commit.

**The case (2026-11-01).** Applying the v0.10.3 CHANGELOG draft needed the dates of three commits that
the draft could only infer (`499e414`, `e8778c8`, `fd0e279`). Getting them took eight `tt git log`
calls: pairs like `--since 2026-09-10` and `--since 2026-09-11` with `--grep` or `--path`, reading off
on which side of the boundary each commit fell. It worked, and it found one of the three inferred
dates wrong (`e8778c8` is 2026-08-23, not 08-24), but a bisection by hand is not a reasonable price
for a date.

## How to reproduce it

```
tt git log --repo <genscalator> --since 2026-09-10 --until 2026-09-11
```

Observed: exit 2, `unexpected/incomplete argument '--until'`.

```
tt git log --repo <genscalator> --since 2026-09-10 --limit 3
```

Observed: three lines of sha, email and headline; no dates.

## Acceptance sketch

- `--until D` accepted beside `--since D`, with the same date syntax, passed through to git.
- Each output line carries the commit date (`YYYY-MM-DD` is enough; author date, as `git log` shows
  by default), in a fixed column so the output stays easy to scan.
- A test that a commit outside a `--since`/`--until` window is NOT listed, and one inside is, so the
  window is shown to exclude rather than only to parse.
- Help text and `tools/README.md` updated together.

## Discussion

### Comment by bjornregnell at 2026-11-01 20:19

**A third gap, and the worst of the three: a bare date in `--since` does not mean midnight.** git reads
a date with no time as that date at the CURRENT time of day, so `--since 2026-09-11` run at 19:20
means "since 2026-09-11 19:20". `tt git log` passes the value through unchanged, so it inherits this.

It mattered the same day. The bisection described above read "listed with `--since D`, not listed with
`--since D+1`" as "made on day D", which is only true at midnight. Re-run with explicit times
(`--since '2026-09-11 00:00'`), two of the three dates came out differently: the box line is
2026-09-11, not 09-10, and the CONTRIBUTING checklist is 2026-08-24, not 08-23. The wrong dates went
into the v0.10.3 CHANGELOG and were fixed on main afterwards. A missing flag is visible; this gave
plausible wrong answers.

Reproduce: `tt git log --repo <genscalator> --since 2026-11-01 --path tools/payloadlib.scala`, run
on the evening of 2026-11-01, does not list `3444296`, which was committed that afternoon and touches
that file.

Suggested fix, beside the acceptance sketch above: treat a bare `YYYY-MM-DD` in `--since`/`--until`
as `00:00` local time (and say so in `--help`), and add a test that a commit made earlier on the
`--since` date IS listed.
