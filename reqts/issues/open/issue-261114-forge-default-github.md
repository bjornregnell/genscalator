# Issue 261114-forge-default-github: tt forge defaults to Codeberg, which genscalator no longer uses: make GitHub the default

> status: open 2026-11-14 · labels: tools, forge, ux · measured against: genscalator main at `18f2887`
> · summary: with no dialect flag, `tt forge` talks to Codeberg (Gitea/Forgejo). genscalator dropped Codeberg
> (issue 261114-drop-codeberg-mirror), and its own release checklist now has to add `--gh` to every call. Make
> GitHub the default dialect; keep Gitea/Forgejo reachable with an explicit flag or `--url`.

## Description

`tools/forge.scala:53` sets `DefaultBase = "https://codeberg.org"`, and every verb uses the Gitea/Forgejo dialect
unless `--gh` or `--gl` is given. The token chain for the default dialect is `GENSCALATOR_CODEBERG_TOKEN`,
`CODEBERG_TOKEN`, `FORGE_TOKEN` (`tools/releaselib.scala:97`), and the trusted-host set is `codeberg.org`
(`tools/releaselib.scala:178`).

Since 2026-11-14 genscalator has no Codeberg repo, and the issue that removed it had to rewrite `docs/RELEASING.md`
steps 8, 10 and 12 to add `--gh`. A call that forgets the flag now asks Codeberg about a repo that does not exist
and gets a 404, which reads like a missing release rather than the wrong host. For a release command
(`release-create`), the default is the effectful path, so a wrong default matters more than for a read.

Most users of the toolbox are on GitHub, and the Claude Code plugin marketplace resolves `owner/repo` to github.com,
so GitHub is also the more useful default for others.

## Acceptance sketch

- With no dialect flag, every verb uses the GitHub dialect (api.github.com, the GitHub token chain).
- `--gh` keeps working as a no-op, so existing scripts and docs do not break.
- Gitea/Forgejo stays reachable: a new explicit flag (e.g. `--gitea`), and `--url BASE` without `--gl` keeps
  meaning "a Gitea forge at BASE". The Codeberg default base remains for that dialect only.
- Verbs that exist only for Gitea or only for GitHub say so plainly when called in the wrong dialect, as
  `contributors` already does.
- Help text (`tools/forge.scala:9`, `:80-85`, `:170`, `:191-207`) and `tools/README.md` say GitHub is the default.
- Tests: `tools/test/cli.test.scala:2998` assumes the Gitea default and needs updating; add one test that a bare
  `tt forge releases o/r` builds a GitHub URL, and one that `--gitea` (or `--url`) builds a Gitea URL.
- `docs/RELEASING.md` can then drop its `--gh` flags (optional; they stay valid).
- CHANGELOG notes the changed default as a behaviour change.

## Discussion

Not in scope: removing Gitea/Forgejo support. It is a general feature and other projects may use it.
