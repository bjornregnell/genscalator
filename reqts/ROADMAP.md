# ROADMAP.md - what lands in which version

A short, version-by-version bullet list, so the release picture does not have to be reconstructed from
`PRD.md`. The PRD states goals and requirements; `DESIGN.md` states how and why things are built; this
file states **when**. Detail and open questions live in those documents, not here.

> **Status: stub.** Started 2026-07-26. The version-planning material now in `PRD.md` is to be migrated
> here; that migration is pending.

## v0.9.2 - current, in progress on `main`

- Single dispatcher: the whole toolbox ships as one native image, one entry point, `tt <tool> <args>`
  unchanged.
- Native fast path is default-on in the launcher, with a stale-binary check that falls back to
  scala-cli, so a stale binary degrades to slow and never to wrong.
- Toolbox on Scala 3.9.0-RC4, and the version is now stated once, in `tools/project.scala` (see
  `DESIGN.md` D1).
- New tools: `tt env` (typed environment reads, no whole-environment verb by design), `tt sub` (typed
  search and replace, preview by default), `tt git --remote` and `tt git push` (one unit to a whole
  mirror set in one call).

## v0.10.0 - alpha, coming next

**Gate:** a tester on their own machine can install and run the toolbox without a wedge. Everything else is polish or velocity.

Blocking:

- Cross-platform native build matrix, macOS and Windows. Linux x86-64 is alpha-ready today; the other
  two platforms have never been built. Candidate route: GitHub Actions, since scala-cli fetches its own
  GraalVM and needs no separate provisioning on a runner.
- Decide how alpha is distributed: release-asset binaries, or build on the tester's box.
- `tt update --native`, explicitly gated on the build matrix being green on both platforms first.
- `gs native`, consent-gated provisioning, depends on the same.

Also in scope for alpha:

- Onboarding smoothness: install and getting-started path, and `tt init` / `gs init` (designed, not
  built). Includes a one-command install of genscalator plus its companions, scalex and the Metals MCP,
  for newcomers who want everything at once. It must be a reviewable, version-pinned script the human
  reads before running, never a blind curl-into-shell pipe: that opaque pattern is the exact
  confirmation-fatigue and remote-execution risk genscalator argues against.
- Write the credentials-and-tokens section of `SECURITY-MODEL.md`, which is currently an explicit TODO
  in a document testers read.
- PRD consistency pass: make it describe what was actually built.
- Public-surface hygiene, including moving research and unpublished media that reads as cruft to the
  closed work repo, with a link-consistency sweep afterwards.

## Future

Tentative plan after alpha release.

### v0.11.0 - beta

- Substrate re-architecture
- investigation of hangs and regressions from captured data, to harden tt tools
- the meta-minion study, 
- bloop upstream work (can we contribute with reproducable bloop memory hogging)
- the toolbox and infrastructure wishlist.
- semantic versioning scheme
- Tool safety flags: `--safe-mode`, `--sandboxed`, `--audit`.
- Capture-checking Safe-mode proof of concept, so pure tools are safe by default and purity stops being
  a convention the reader has to trust.
- Cross-tool packaging: an MCP server, so the tools are first-class in Codex and opencode too. The Claude
  Code plugin already ships; see the plugin section of the README.
- ...

### v1.0.0 Personal Agentic Software Engineering with genscalator

- TODO: define alfa, beta, milestones M1, ... before RC1, ...


### v2.0.0 genscalator super-harness abstracting over guards and harnesses

- TODO: define alfa, beta, milestones M1, ... before RC1, ...


### v3.0.0 Team-level Agentic Software Engineering with genscalator

- TODO: define alfa, beta, milestones M1, ... before RC1, ...
