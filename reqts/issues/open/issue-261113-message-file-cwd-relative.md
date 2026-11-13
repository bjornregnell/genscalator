# Issue 261113-message-file-cwd-relative: tt git resolves --message-file and --out against the cwd, not --repo, while --add and the help examples are repo-relative

> status: open 2026-11-13 · labels: tools, git, ux · measured against: genscalator v0.10.3, main at `d8c7a83`
> on Linux · summary: in `tt git commit`, `--add` paths are relative to `--repo` (git runs there), but
> `--message-file` is resolved against the caller's cwd. The help example `--repo /abs/repo --message-file
> tmp/msg.txt` and the house rule "write the message into the repo's gitignored tmp/" both read as repo-relative,
> so an agent working from another directory gets "message file not found". `tt git show --out` has the same split.

## Description

`tools/git.scala:224` resolves the message file with `os.Path(msgStr, os.pwd)`, while `--add` paths are passed to
`git add` run inside the repo (`tools/git.scala:229`), i.e. they are repo-relative. One command, two different
bases for relative paths.

Everything around the flag points the reader at the repo-relative reading:

- the help examples (`tools/git.scala:152-153`): `tt git commit --repo /abs/repo --message-file tmp/msg.txt --add src/app.scala --push`;
- `tt issue`'s help (`tools/issue.scala:342`): `tt git commit --repo <dir> --message-file tmp/msg.txt ...`;
- the session-start digest: "write the message file FIRST, into the repo's gitignored tmp/".

The guard makes it worse: a bare `cd` is blocked, so an agent is routinely in a *different* cwd from `--repo`.
That is the case `--repo` exists for, and the case where a cwd-relative message path breaks.

Field case, 2026-11-13: the session's cwd was a different project. The agent wrote the message to
`<genscalator>/tmp/msg-….txt`, then ran the command exactly in the help's shape:

    tt git commit --repo /home/…/genscalator --message-file tmp/msg-issue-261113-paste-slash-copy-dance.txt --add reqts/issues/open/… --push

and got `git: message file not found: <cwd>/tmp/msg-….txt`. Passing an absolute path worked (commit `d8c7a83`).
The error at least prints the resolved path, which made the cause quick to see; nothing was committed wrongly.

The same split exists in `tt git show --out` (`tools/git.scala:425`, `os.Path(o, os.pwd)`), whose help example is
`--out tmp/old-readme.md` next to a repo-relative `--path`.

## How to reproduce it

1. From a cwd that is NOT the repo, write a message to `<repo>/tmp/msg.txt`.
2. `tt git commit --repo <repo> --message-file tmp/msg.txt --add <some file>`
3. Result: `message file not found: <cwd>/tmp/msg.txt`. With `--message-file <repo>/tmp/msg.txt` it commits.

## Acceptance sketch

One rule for every relative path a `tt git` verb takes, stated in the help. The suggested rule:

- A relative `--message-file` (and `show --out`) resolves against `--repo`, like `--add` and `--path`. That matches
  every example and the tmp/ convention.
- To avoid silently changing behaviour for callers who relied on cwd: if the repo-relative path does not exist but
  the cwd-relative one does, use it and print a one-line note, or fail with both candidate paths named. Either is
  fine, as long as it is never a silent guess.
- The help line for `--message-file` says what a relative path is relative to.
- A test in `tools/test/git.test.scala` that commits with a relative `--message-file` from a cwd different from `--repo`.

Alternative, if cwd-relative is kept on purpose: change the help examples and `tt issue`'s hint to absolute paths
(`--message-file /abs/repo/tmp/msg.txt`) and say "relative paths are relative to the cwd, not --repo" in the help.

## Discussion
