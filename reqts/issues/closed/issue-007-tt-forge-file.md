# Issue 007: `tt forge file` — fetch ONE repo file's contents, the remote sibling of `tt git show`

> status: closed 2026-08-19, fixed by `e4d1284` · labels: toolbox, forge · summary: no verb reads repo FILE contents from a forge
> (`release-download` does only assets), and `tt web get` NEVER sends credentials by design — so a
> single file in a private repo forces a raw curl-with-token or a full clone.

## Description

The gap surfaced while an agent parsed one file from a private repo: the typed toolbox offers no
shape for "get me `<path>` from `<owner>/<repo>` at `<ref>`". The workaround was a shallow clone
plus `tt git show`, which works but downloads a repository to read one file.

## Acceptance sketch

* `tt forge file <owner>/<repo> <path> [--ref R] [--out F] [--gh | --url BASE]`
* GitHub: `GET /repos/o/r/contents/<path>` with `Accept: application/vnd.github.raw`; Gitea via
  the raw endpoint; `--ref` defaults to the default branch.
* Token from the SAME fixed-env + gh-auth-fallback machinery as the other forge verbs, trusted
  hosts only. This is a READ that carries a CREDENTIAL, so the typed single-file shape IS the
  safety argument versus the raw reflex it prevents.
* Size-capped like `tt web` (`--max-bytes`); `--out` writes a file, else stdout.

## Discussion

### Comment by bjornregnell/Charlie at 2026-07-28

Filed on BR's go, from the v0.10.1 mining sweep. Specced 2026-07-28 when the gap was correctly
NAMED instead of improvised around — the pattern working as intended.
