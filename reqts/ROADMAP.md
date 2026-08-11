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

## v0.9.3 - SKIPPED. Decided by BR 2026-07-27; this release does not exist

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

✅ **DECIDED 2026-07-27 by BR: SKIP v0.9.3, go straight to v0.10.0.** Recorded with the tension intact,
because the decision went AGAINST the evidence above rather than following it, and a later reader
should not have to reconstruct why. The evidence said the material is real; BR's call was that real
material does not by itself earn a release when the sub-question above resolves the other way — the
Windows fixes serve the alpha's own gate, so they ARE alpha content rather than a separate shipment.
**Nothing on the list is dropped: the list stays because it is now the answer to "what does the alpha
ship beyond its blocking items", which is a question the release notes will have to answer anyway.**
⚠ The cost BR accepted explicitly: a Windows tester's three hard failures now ship no sooner than the
alpha, and the alpha is gated behind `tt zip extract`, a security design task (see v0.10.0 below).

## v0.10.0 - alpha — SHIPPED, published 2026-07-28

✅ **PUBLISHED 2026-07-28 (19:26Z) as a real, non-prerelease release**: 4 platform zips + 4 sha256 +
`get-genscalator.sc`, all built from the day's final commits and verified by a post-publish smoke
(installer against a scratch home, then `tt update --native` answering "already up to date" — after
the smoke caught and fixed a bootstrap version-stamp defect, `8c7f02e`; full record in DESIGN.md D7c).
The section below is kept as it stood at the gate, with shipped items marked.

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
- ~~Decide how alpha is distributed: release-asset binaries, or build on the tester's box.~~
  ✅ **DECIDED 2026-07-27 by BR: release-asset binaries for the four PROVEN platforms**
  (`linux-x86_64`, `linux-aarch64`, `macos-aarch64`, `windows-x86_64`), **with building from source
  DOCUMENTED as the supported route for the two that are not proven** (Intel macOS, and
  `windows-aarch64` while no `aarch64-pc-win32` scala-cli build exists upstream). So an unusual-platform
  tester is slower, never unsupported. This is what the workflow comments already assumed; it is now a
  decision rather than an assumption. ⚠ **It is also what makes extraction necessary** — see the
  `tt update --native` item below, whose blocker exists only because this went the assets way.
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
  ✅ **DECIDED 2026-07-27 by BR: BUILD `tt zip extract` with a containment guard.** The alternative
  offered and declined was to skip extraction entirely and have `tt update --native` download, verify,
  then PRINT the install command for the human to run — which is what `tt update` already does today
  ("It updates nothing itself... the human is the actuator") and would have added no zip-slip surface.
  BR chose the real extractor, accepting a security design task on the alpha's critical path in
  exchange for a genuine one-command update. **Guard rules the extractor must enforce, and each wants
  a hostile-entry test rather than a comment claiming it holds:** reject any entry whose RESOLVED
  target escapes the destination directory; reject absolute paths and Windows drive letters; reject
  symlink entries; and apply a stated overwrite policy rather than an accidental one.
  🔨 **BUILT 2026-07-28, and the shape of the work was not what this item predicted.** `tt zip extract`
  landed as forecast, but implementing the verb surfaced a dependency NEITHER this line nor DESIGN.md's
  D7a saw: both halves it needs — download-and-verify, and extraction — lived inside OTHER TOOLS
  (`tt forge`, `tt zip`), and tools depend on shared libs rather than on each other. So the real work was
  two extractions into shared modules, `releaselib.scala` and `ziplib.scala`, before a line of the verb
  could be written. Recorded as **D7c** in DESIGN.md. The payoff is that ZipSuite's 15 hostile-entry tests
  now guard the code the self-updater actually runs, instead of one of two copies of it.
  ~~⚠ NOT YET RUN END TO END, and that is deliberate rather than unfinished.~~ ✅ **RUN END TO END
  2026-07-28, BR present** — preview, fresh-install, and install-over-install (the two-rename swap)
  all exercised against the real release; DESIGN.md D7c carries the full record and closes D7. What
  had been verified before that run: the full suite green, and the pure parts unit-tested (the asset
  glob, and the rule that staging and retired must be SIBLINGS of the install).
- `gs native`, consent-gated provisioning, depends on the same.

Also in scope for alpha:

- Onboarding smoothness: install and getting-started path, and `tt init` / `gs init` ~~(designed, not
  built)~~ *(stale when the alpha shipped: `gs init` and `gs allow` are BUILT in the gs-dwim skill —
  per-project onboarding walking allowlist, status line, and hooks, human-gated — and the README was
  rebuilt installer-first with the release-frozen script URL, 2026-07-28)*. Includes a one-command install of genscalator plus its companions, scalex and the Metals MCP,
  for newcomers who want everything at once. It must be a reviewable, version-pinned script the human
  reads before running, never a blind curl-into-shell pipe: that opaque pattern is the exact
  confirmation-fatigue and remote-execution risk genscalator argues against.
- ~~Write the credentials-and-tokens section of `SECURITY-MODEL.md`, which is currently an explicit TODO
  in a document testers read.~~ **WRITTEN 2026-07-27.** The section is no longer a placeholder: it leads
  with the policy in one paragraph, names what the policy trades away, closes point 4's open question,
  and gives point 5 its preference plus the two legitimate exceptions (CI, and a forge with no helper)
  and the one illegitimate one (an rc-file export for interactive convenience).
  ✅ **POLICY DECIDED 2026-07-27 by BR: MOMENTARY-FIRST.** A credential helper consulted at the point of
  use is the preferred route; an exported token in a shell rc file is DISCOURAGED, because it is
  continuously readable by every child process for the life of the shell, while a token fetched at the
  moment of use exists briefly inside one audited tool and self-heals on re-auth. This ratifies where
  the code already went (`4cfe96b`, and the `gh auth token` fallback of 2026-07-25) rather than
  inventing a stance.
  ⚠ **The section must ARGUE this position explicitly, not merely state it, and the reason is a
  coherence problem a reader will otherwise spot unaided:** the accepted cost is that an agent can
  self-authorize whenever the human is already logged in to `gh` — observed live on 2026-07-27, where
  `tt forge` printed "no token in env; obtained one from `gh auth token`" on essentially every call,
  including while BR was AFK. Read beside a self-updating native installer (the decision two items up),
  both choices trade human ACTUATION for audit-plus-brevity-of-exposure. That is a defensible stance
  and it is now the project's de facto one, so the document must name **audit and short exposure as the
  control**, instead of leaving a reader to infer that human-in-the-loop was quietly abandoned. The
  alternative BR declined was human-gated-only: tokens solely from fixed human-set env names, no helper
  fallback, so an agent could never obtain a credential it was not handed.
- PRD consistency pass: make it describe what was actually built.
- Public-surface hygiene, including moving research and unpublished media that reads as cruft to the
  closed work repo, with a link-consistency sweep afterwards.
  📮 **SM250 folds in HERE rather than as a new blocking item, because it is the missing instrument for
  the sweep this line already asks for.** Measured 2026-07-27: the PUBLIC repo has **19 dangling of 363
  local links across 290 files**, and the tester-facing ones are real defects rather than tidiness —
  `skills/serverless-spa-seed/` cites a missing `main.js` in THREE places (a seed a newcomer runs),
  `docs/manual-src/getting-started.md` → `allowlist.html`, and the published post
  `media/blog/030-what-is-a-good-warp-ember.md` → `../media/img/baton-example1.png` (also SM244's rotted
  filename, still saying *baton*).
  ⚠ **The order matters and is counter-intuitive: GATE FIRST, WIDEN SECOND.** All 19 are plain
  markdown/html links, so the NARROW matcher already finds every one — `tt links check` carries an exit
  code built for gating and is in NO workflow (`native-release.yml` is the only one). *(Progress
  2026-07-28: down to 6 dangling of 347, all six in the by-design set below; the remaining half of
  this line is filed as issue-011, and the lint sibling as issue-010.)* So the cheap win is
  wiring it in and fixing the 19; the `--include-prose` widening comes after, because a better matcher on
  a check nobody runs just produces more findings nobody reads. When it lands it must stay OFF the CI exit
  code, so the gate keeps the precision that makes it gateable.
  ⚠ Judgement required, not a blind fix: the `docs/manual-src/*.md → *.html` set may be BY DESIGN, since
  those `.md` sources cite `.html` siblings that exist only after `tt ssg` generates them. If so the
  answer is an ignore rule or a generation-order note — never edit a link into a lie.

## v0.10.1 - RELEASED 2026-08-07 - the alpha's bug harvest; the round-trip headline did NOT ship

Deliberately AFTER the alpha, not before it. The alpha gate is that a tester can install and run
without a wedge; round-tripping touches none of that, so putting it earlier would insert work no
tester can see between us and the alpha.

Besides the round-trip headline (issue-005), the alpha's own field experience minted a set of
smaller items, each filed as an in-repo issue on 2026-07-28: forge asset replace (issue-006),
`tt forge file` (issue-007), ~~commit-log search (issue-008)~~ *(closed same day: `tt git log`
already existed; the stale digest that hid it is fixed)*, `tt forge contributors` (issue-009),
reqt-lint fenced-skip (issue-010), links-check exceptions + CI gate (issue-011), release-workflow
tag-input validation (issue-012), and the munit bump (issue-013, a good first contribution). See
`reqts/issues/open/`.

Hans's alpha field test (PR 2, merged 2026-08-07; provenance in reports 085-086) added a second
batch, issues 014-022. Maintainer triage (2026-08-07, recorded in each issue's Discussion) placed
six in THIS release as defects — find's depth boundary (issue-014), skillcheck on a native install
(issue-015), the `tt files` pruning half of issue-017, the minimal-verdict half of issue-018, the
version-carrier consistency + release gate (issue-021), and `tt which` on Windows (issue-022) —
and the rest in the v0.10.2 polish pool (016, the enhancement halves of 017/018, 019, 020).
Ordering constraint: issue-022's fix lands before or with issue-019's doc change, never after.
Same-day field find against the new per-session state feature: harness bg/fg orphans mode chips
and session name (issue-023, 2026-08-07) — a defect-pool candidate for this release.
Suggested fix order from the triage: 014, 022, 018, 015, then 017's pruning half, with 021 LAST —
021 is the release-cut work itself (bump the five version carriers, write the CHANGELOG section,
gate on tag == carriers), so everything else must be in before it runs.

**What v0.10.1 actually shipped** (tag `v0.10.1` on `a61f839`, 2026-08-07): the six defects above,
and nothing else. The round-trip headline below (issue-005) and the de-vendoring did NOT ship in it
and are NOT yet re-scheduled — the section keeps them here rather than silently moving them, because
where they land is a maintainer decision, not a bookkeeping one. The polish pool moved on to v0.10.2,
which now has its own section below.

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

## v0.10.2 - RELEASED 2026-08-11 - the polish pool, and the release gate proven

The six items the v0.10.1 triage parked as polish rather than defects, landed as one wave on
2026-08-09 and cut on 2026-08-11: the enhancement halves of issue-017 (curated skip-set, repeatable
`--exclude`, disclosed exclusions, `--all` meaning everything) and issue-018 (success-marker taxonomy,
`--require-markers`), issue-016 (`tt htmltext --cap`), issue-020 (all three polish items, including a
`tt help` that exits 0), issue-023 (`tt session adopt` for state orphaned by a harness bg/fg round
trip), and issue-012 (release-workflow tag-input validation, implemented in the gate job issue-021's
cut introduced). Per-item detail lives in each issue's Discussion and in `CHANGELOG.md`; the
implementation commits are `58db2ea` `19e4209` `68e3545` `53edb60` `456f038` `50cb1d0`, with contract
tests in `9e9f3f8` and the doc sweep in `cb660d4`.

Deliberately NOT in this release, and moved to v0.10.3 rather than dropped:

- **issue-022's real-Windows verification.** The `tt which` fix shipped in v0.10.1 and is unit-tested
  with `';'` on every platform, but the checklist in 022's Discussion has not been run on real
  hardware. BR's call, 2026-08-11: ship v0.10.2 with what is proven and do the Windows day for
  v0.10.3. The issue stays open, and the ordering constraint below still holds.
- **issue-019 (allowlist doc: which `tt` wins).** Its blocking constraint — 022's fix must land before
  or with it — is satisfied by code, but the doc would be asserting Windows behaviour nobody has yet
  watched work. It travels with the Windows day.
- **issue-011 (links-check ignore rules + the CI gate).** Held on purpose: 017's enhancement half
  deferred the gitignore question here, so links-check, `tt files` and `tt find` can settle ONE ignore
  dialect together rather than three.

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
