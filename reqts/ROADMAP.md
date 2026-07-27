# ROADMAP.md - what lands in which version

A short, version-by-version bullet list, so the release picture does not have to be reconstructed from
`PRD.md`. The PRD states goals and requirements; `DESIGN.md` states how and why things are built; this
file states **when**. Detail and open questions live in those documents, not here.

> **Status: stub.** Started 2026-07-26. The version-planning material now in `PRD.md` is to be migrated
> here; that migration is pending.

## v0.9.2 - RELEASED 2026-07-24

- Single dispatcher: the whole toolbox ships as one native image, one entry point, `tt <tool> <args>`
  unchanged.
- Native fast path is default-on in the launcher, with a stale-binary check that falls back to
  scala-cli, so a stale binary degrades to slow and never to wrong.
- Toolbox on Scala 3.9.0-RC4, and the version is now stated once, in `tools/project.scala` (see
  `DESIGN.md` D1).
- New tools: `tt env` (typed environment reads, no whole-environment verb by design), `tt sub` (typed
  search and replace, preview by default), `tt git --remote` and `tt git push` (one unit to a whole
  mirror set in one call).

## v0.9.3 - OPEN QUESTION: does this release exist at all?

Investigate what is actually left for one more release before the alpha, and decide whether it earns
being a release. The alternative is to skip it and go straight to v0.10.0.

The question to answer, not to assume: is there anything shipped since v0.9.2 that a user would want
BEFORE the alpha, and that is not itself alpha work? If everything on the list is either alpha-blocking
or invisible to a tester, then v0.9.3 is ceremony and the honest move is to skip it. Deliberately NOT
in this release either way: the reqT-lang round-trip work, which moved out (see v0.10.1).

**Evidence gathered 2026-07-27, decision still BR's.** ⚠ Scope caveat: read from a `--since 2026-07-24`
log window (95 commits), which is a SUPERSET of post-tag work because v0.9.2 was tagged at 11:17 that
day — so the list below is what a reader should verify, not a certified post-tag set. The answer looks
like **yes, there is real user-facing material, and it is not merely alpha work**:

- **Three hard failures a WINDOWS tester hits on v0.9.2**, all fixed since: `tt sbt --dir` and
  `tt bloop clean --dir` rejected every path a Windows user could type (`e74e184`, `19b311e`), and
  `tt env has PATH` reported ABSENT on every Windows box because `sys.env` is case-sensitive and the
  key there is `Path` (`6bab0d5`), plus the broader charset/line-ending/path pass (`641fe77`). This
  bears directly on the alpha gate, which is *a tester on their own machine* — so it is arguably
  alpha-blocking rather than separate from it.
- **Two `tt forge` bugs that made the tool wrong rather than merely limited:** `releases` and `tags`
  accepted `--gh` and then silently ignored it (`00aba1d`), and `releases` crashed outright on any repo
  with a draft release (`27d88c0`).
- **New verbs a user can see:** `tt links`, `tt memory`, `tt tsv`, `tt json`, `tt sbt`,
  `tt bloop clean`, `tt git push --tags`, and `tt zip` + `tt forge release-download` /
  `release-delete`. (`tt env`, `tt sub` and `tt git push` are NOT in this set — the v0.9.2 section
  above already claims them, which is the kind of double-count this caveat exists to prevent.)

⇒ So the honest reading is that v0.9.3 would **not** be ceremony. The open sub-question is whether the
Windows fixes are better framed as v0.9.3 or folded into the alpha, since they serve the alpha's own
gate.

## v0.10.0 - alpha, coming next

**Gate:** a tester on their own machine can install and run the toolbox without a wedge. Everything else is polish or velocity.

Blocking:

- ~~Cross-platform native build matrix, macOS and Windows.~~ **DONE 2026-07-27** (this text said "have
  never been built", which stopped being true that day). Run 30287100766 was the first fully green
  `native-release`: **linux-x86_64, linux-aarch64, macos-aarch64, windows-x86_64**, each binary proved
  by the full CLI-contract suite run THROUGH it. The route taken was the candidate one, GitHub Actions
  with scala-cli fetching its own GraalVM. Two deliberate exclusions, both recorded in the workflow
  itself: `windows-aarch64` ships EXPERIMENTAL and currently fails, because VirtusLab publishes no
  `aarch64-pc-win32` scala-cli build; and `macos-13` (Intel) was REMOVED rather than marked
  experimental, because it never failed, it never STARTED — a perpetually-queued leg holds `publish`
  hostage via `needs: build`, and `continue-on-error` does not fix queuing. Run 30292345970 then
  demonstrated that distinction empirically: `windows-aarch64` FAILED and `publish` ran anyway.
- Decide how alpha is distributed: release-asset binaries, or build on the tester's box.
  **Still BR's decision, but no longer un-evidenced:** the release-asset path was proven end to end on
  2026-07-27 — `publish` executed for the first time ever (6s), attaching 4 platform zips + 4 sha256,
  and a full round trip was verified from the tester's side: download, sha256 match, then every CRC32
  in the archive validated (36 entries, 44,287,305 B) with `bin/tt` and a correct `VERSION.txt` inside.
- `tt update --native`, explicitly gated on the build matrix being green on both platforms first.
  **The gate is now open, and a SECOND dependency surfaced 2026-07-27 that this line did not know
  about:** the pieces it needs are download (`tt forge release-download`, built) and checksum verify
  (`--verify`, built) and **extraction, which does not exist and was deliberately not built.** A zip
  entry name can contain `../` or an absolute path (zip-slip), so a safe extractor needs a
  path-containment guard designed on purpose with its own tests. ⇒ **`tt zip extract` is the real next
  blocker, and it is a security design task, not plumbing.** It also writes executables onto a user's
  machine, so it wants a human at the wheel.
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

## v0.10.1 - reqT-lang round trip, and the road to de-vendoring

Deliberately AFTER the alpha, not before it. The alpha gate is that a tester can install and run
without a wedge; round-tripping touches none of that, so putting it earlier would insert work no
tester can see between us and the alpha.

* Goal: nonDestructiveRoundTrip has
  * Gist: parsing then unparsing a document never loses anything a human wrote
  * Spec: Two properties, and the weaker one alone is not enough. LOSSLESS means unparse(parse(x)) equals x byte for byte. IDEMPOTENT means norm(norm(x)) equals norm(x), where norm is unparse after parse, so the result reaches a fixed point. A parser can be perfectly idempotent and still destructive: destroy on the first pass, then stay stable forever. The name of this goal is therefore the strong property, not the weak one.
  * Why: a document a human also edits must survive being read and written by a tool. Losing an emphasis marker once is data loss even if it never happens again. Measured 2026-07-27, today's round trip is neither: it consumes one emphasis marker per pass, so the damage compounds. See reqts/issues/open/issue-005.
* Target: proseSurvivesByteForByte verifies Goal: nonDestructiveRoundTrip
* Target: renderedFragmentsReachAFixedPoint verifies Goal: nonDestructiveRoundTrip

The two targets are won by different means, which is the useful part: prose losslessly, by preserving
the original bytes of non-reqT spans rather than by perfecting the renderer; fragments idempotently,
because rendered reqT legitimately normalises and that is fine so long as it stabilises after one pass.

Also in scope, because the cascade is the real constraint:

- Keep the existing rendering functions and ADD the preserving ones under a different name, so the
  change is additive for everyone downstream. Name them for the guarantee (`toMarkdownPreserving`) or
  for what they do (`applyTo(original)`), NOT for idempotence, which is the weaker property again.
- Note the signature cannot be a drop-in: a `Model` does not carry the original bytes, so either the
  parser adds source spans to the model, or the new function takes the document as an argument. The
  source-span route is the bigger change and the more enabling one, since a fragments mode and
  line-accurate lint errors both need it.
- Write the migration note for the reqT Swing desktop tool: what its API consumption must change, and
  what it may keep. Publishing the fix without that note is what makes a cascade hurt.

Then de-vendor `tools/reqt-vendored/` and depend on a released reqT-lang, before the beta.

## Future

Tentative plan after alpha release.

### v0.11.0 - beta

* Feature: SubstrateReArchitecture has
  * Gist: avoid substrate strain
  * Why: a substrate that grows without bound is eagerly read at every cold start, which raises context fill and with it the risk of rot. Measured 2026-07-27: PIN-BOARD.md is 4300 lines, up from the 4096 that first surfaced the strain, and MEMORY.md has already hit its read limit once.
  * Spec: Split genscalator/work into a HUMAN side (readable, no markdown blobs) and an AGENT side written in a restricted markdown subset that gives handles to hold. Then move history OFF the eagerly-read surface, into work/agent/history and work/human/history, so the warp ember reads only a bounded CURRENT surface and history becomes opt-in. Keep a hard size bound on the current surface, plus an explicit pointer saying how to reach history, so that lazy does not become lost.
  * Comment: Decided direction 2026-07-19. This was originally one item covering both issue intake and the substrate itself; the intake half shipped as reqts/issues, and this is what remains. Bounding the cold-start read by construction rather than by discipline also removes a separately-observed failure where a session hangs while eagerly reading an unbounded surface, so two problems close together. Deliberately NOT alpha-blocking, but worth pulling forward if the drag starts to hurt.

- investigation of hangs and regressions from captured data, to harden tt tools
- the meta-minion study, can/should we offer a meta-minion skill for continuos echt-checking in TokSpend mode or similar?  
- bloop upstream work (can we contribute with reproducable bloop memory hogging; we have native builds but when contributors work on tools it fall back to slow scala-cli/bloop mem-hogging stuff)
- the toolbox and infrastructure wishlist.
- semantic versioning scheme
- Tool safety flags: `--safe-mode`, `--sandboxed`, `--audit`.
- Capture-checking Safe-mode proof of concept, so pure tools are safe by default and purity stops being
  a convention the reader has to trust.
- prepare for use in other harnesses not only Claude Code, investigate and decide scope of beta release on tailored support for harnesses and editors (pros/cons/tradeoffs) preferring open source targets, candidates:
  * codex
  * open code
  * vs code
  * kilo
  * cursor
  * ...

- Cross-tool packaging: an MCP server, so the tools are first-class in Codex and opencode too. The Claude
  Code plugin already ships; see the plugin section of the README.
- ...

### v1.0.0 Personal Agentic Software Engineering with genscalator

- TODO: define alfa, beta, milestones M1, ... before RC1, ...


### v2.0.0 genscalator super-harness abstracting over guards and harnesses

- TODO: define alfa, beta, milestones M1, ... before RC1, ...


### v3.0.0 Team-level Agentic Software Engineering with genscalator

- TODO: define alfa, beta, milestones M1, ... before RC1, ...
