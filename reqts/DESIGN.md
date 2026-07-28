# DESIGN.md - design decisions and their reasons

Why this file exists: `PRD.md` states **what genscalator is for** (stakeholders, goals, features,
targets, in reqT-lang). Design-level material, **how it is built and why it was built that way**, had
been accumulating there too, and the PRD is getting large. This document is the home for the second
kind. A reader who wants to know "why is it done like this?" should find the answer here.

Each record states the decision, the reason, the evidence it rests on, and what follows from it. A
decision with no evidence behind it is written as a decision, not as a finding.

> **Status: stub.** Started 2026-07-26 with the record below. Most or some of the design-level content
> now in `PRD.md` is to be migrated here; that migration is pending and is tracked on the pinboard.

## D1 - the Scala version is single-sourced in `tools/project.scala`

**Decision.** The toolbox states its Scala version exactly once, in `tools/project.scala`. Every tool
file and test file carries `//> using file project.scala` instead of naming a version. Bumping the
version is one edit.

**Context.** Before this, all 48 tool files and 22 test files named their own version, so the
3.8.4 to 3.9.0-RC4 bump had to touch 78 sites across the repo. The file name follows scala-cli's own
convention for project-wide using-directives, so the role is legible to any Scala reader.

**Why the tools include it explicitly, rather than relying on the conventional pickup.** The `tt`
launcher has two paths: the native binary, and a scala-cli fallback used whenever that binary is stale.
The fallback runs **one tool file**, `scala-cli run tools/<tool>.scala`, and that build unit does not
contain the rest of the directory. scala-cli's automatic `project.scala` pickup applies to
directory builds, not to a single-file input, so without an explicit include the version does not
reach the tool.

**Evidence, established live rather than assumed.** Two experiments on this box, both against the real
launcher path:

- Pointing `project.scala` at a different version changed what a single-file tool run compiled with, so
  the include really is what selects the version.
- Removing the include from one tool while leaving `project.scala` in place made that tool compile on
  **3.8.4**, which is scala-cli 1.15's own default. This is the important half: a missing include does
  not fail, it silently compiles the tool on the wrong compiler. `ScalaVersionSuite` exists because the
  failure mode is silent.

**Consequence: the mainless helpers deliberately do not include it.** `lib`, `seqspec`, `boxstats`,
`minijson`, `limitstore`, `secrets` and `mdparse` are themselves included by tools. scala-cli does not
support **chaining** `using file`: the second hop is dropped, with a warning on every build, and that
warning additionally breaks the CliSuite contract that tools emit empty stderr. They inherit the
version from whichever tool includes them, so nothing is lost. `ScalaVersionSuite` asserts the rule in
both directions: every launcher-runnable tool includes the file, no mainless helper does.

**Out of scope, still self-pinned.** The four `deploy/*.sc` scripts, and the propagated tool subset in
the closed work repo. Both are separate build units; neither is reached by this include.

**Related decision: the fallback stays per-file.** Compiling the whole `tools/` directory on every
fallback invocation would remove the need for the includes entirely, and the machinery already exists
(`scala-cli run <toolsdir> --main-class dispatchTypedTools`, exercised by DispatchSuite). It is
deliberately not done. The fallback runs precisely when `tools/` has just been edited, which is when a
file in it is most likely to be broken; per-file keeps a broken tool from taking down every other tool,
preserving the "degrade to slow, never to wrong" property that the parity-gated native build exists to
protect. The cost accepted in exchange is a per-tool build cache instead of one shared one.

## D2 - two audiences, two install shapes, and no third one

**Decision.** genscalator installs two ways and only two. A **user** gets a self-contained native binary
under `GENSCALATOR_HOME` (default `~/.genscalator`) via `get-genscalator.sc`. A **contributor** forks,
clones, and runs `tools/tt` from the checkout. There is no source-only install, no `--with-sources`
flag, and no separate hackable tree.

**Context: what the bash launcher is actually for.** `tools/tt` resolves the tools dir, chooses between
the native binary and scala-cli, guards against a stale binary, and logs timings. Only the staleness
guard is load-bearing, and it exists for exactly one situation: somebody edited `tools/*.scala` and the
binary is now older than the source. A user who installs a release binary never does that. The launcher
is therefore a development-time artifact, not part of the product.

**Evidence.** `tools/tt` sets its runner to scala-cli by default and falls through with the comment "no
binary at all = not graalified yet: silently use scala-cli". So the toolbox is fully functional with no
native binary at all. Native is a startup-latency optimisation, roughly 20 ms against 600 ms, and the
launcher's real job is choosing between the two, which only means anything while sources are changing.

**Consequence: Windows stops being blocked on the launcher.** `tools/tt` is bash and shells out to GNU
`find`, neither of which a Windows box has by default. That was read as a blocker for Windows testers.
Under this decision it is not: a Windows user runs `tt.exe` directly and never sees the launcher. The
POSIX assumptions remain a contributor-experience problem, which is a much smaller and later one.

**Consequence: git is a contributor dependency only.** `tt update` today resolves the repo root and runs
a git ahead/behind check. A user install has no clone, and may have no `git` binary either. So `tt update`
needs a second mode that compares `VERSION.txt` against the latest published release over HTTPS. This is on
the critical path, not polish, because plugin update-awareness is built on it (see D4).

**Rejected alternative: ship sources to users, protected somehow.** Considered marking a shipped
`tools/` read-only, or naming it something that does not read as a build tree, so that a user editing it
could not silently invalidate the binary. Rejected once the premise collapsed: see D3, no user needs
`tools/` at all. Also rejected a `--with-sources` installer flag, on the grounds that a contributor does
not want sources, they want the clone, with history, branches and the ability to open a pull request. A
source drop without git would be both unhackable and uncontributable.

## D3 - the user install is one self-contained archive per platform

**Decision.** Six release assets, `genscalator-<os>-<arch>.zip`, each containing `bin/tt` (`tt.exe` on
Windows), `docs/`, `reqts/PRD.md` and `VERSION.txt`. The installer verifies a published SHA-256, aborts on
mismatch, extracts into `GENSCALATOR_HOME`, and puts `~/.genscalator/bin` on PATH. No symlink.

**No `tools/*.scala` in the archive, which is the opposite of the obvious assumption.** Checked
2026-07-27: five verbs resolve a directory at runtime, `doc`, `prd`, `skillcheck`, `skillgrants` and
`update`, and **not one of them reads `tools/`**. Each uses it only as a landmark to find the repo root
and then reads a sibling: `docs/`, `skills/`, `reqts/PRD.md`, or the root itself as a git repo. The
install therefore ships the data those verbs read, never the sources. This is recorded because the
assumption was made twice in one morning before anyone checked it.

**Consequence in `lib.scala`: `rootDir()`.** `toolsDir()` finds the root by looking for `tools/tt`, the
bash launcher, which a user install does not have. `rootDir()` resolves `GENSCALATOR_HOME`, then the
clone, then `~/.genscalator`. **The order is the point.** Putting the default install location ahead of
the clone would mean a contributor editing a checkout silently gets their installed copy's docs and
skills instead of the ones they are editing, which is the stale-shadow failure this project keeps
finding in other guises.

**Why zip rather than tar.gz.** The JDK reads zip with `java.util.zip` and no dependency at all, so the
installer needs neither a library nor a subprocess. That is tier one of the dependency cascade. The cost
is that zip does not carry the executable bit, so the installer must `chmod +x` after extracting, and
must guard against zip-slip.

**Why one archive per platform rather than one binary plus one shared data bundle.** The data is a few
hundred kilobytes against a 41 MB binary, so deduplicating it saves nothing measurable while doubling
the download and verification logic. Self-contained also makes the install atomic.

**Windows on ARM is published but experimental.** The runner is the newest and least proven, and x86-64
Windows is already the hard leg because native-image needs the MSVC toolchain. It ships anyway so that
an unusual-platform tester can try it and report, with `fail-fast: false` so a red leg cannot mask the
other five. It must be labelled experimental in the release notes: publishing an asset is an implicit
promise of support, and a tester who does not know otherwise files disappointment instead of a bug.

**Code signing, and an assumption that is not yet evidence.** There will be no Apple Developer account
and so no notarisation. The reasoning this rests on is that `com.apple.quarantine` is attached by GUI
downloaders rather than by programmatic HTTP, so an installer-fetched binary should not meet Gatekeeper
at all, and that Apple Silicon's signature requirement is satisfied by the ad-hoc signature the linker
applies at build time. **Both halves are unverified.** They must be tested on a real Mac before the
alpha depends on them. If either is wrong, the fallback is documenting `xattr -d com.apple.quarantine`,
which is ugly but free.

## D4 - the plugin and the toolbox ship separately and check each other

**Decision.** The Claude Code plugin owns skills and slash commands. The installer owns the binary,
`docs/` and `reqts/PRD.md`. Neither ships the other's payload. Both carry the same version line, and
each warns when the other is behind, comparing **major and minor only** and ignoring the patch.

**Context: there are already two copies.** The plugin ships its own tree, including a `bin/tt`, under
`~/.claude/plugins/cache/bjornregnell/genscalator/<version>/`. Shipping `skills/` in the installer as
well would put two copies of the same skills on one box, free to drift. That is the same failure class
as the memory snapshot that sat at 14 files against 148 live while the copy convention was being
followed, and it fails the same way: silently, and persuasively.

**Why major and minor only.** Strict equality cries wolf. A skills-only patch on the plugin, v0.10.1
against a toolbox on v0.10.0, is perfectly compatible but unequal, and a warning that fires when nothing
is wrong is a warning people learn to ignore. Comparing major and minor keeps the shared version line
and still catches the skew that actually breaks things, which is a skill referring to a verb that the
installed toolbox does not have.

**Where the check lives.** In `tt update`, which is already the "am I behind" verb and is already
throttled through `--throttle <hours>`. Extending it to read both versions reuses machinery that exists
rather than adding a second nag. The plugin calls it at session start and the human sees at most one
line a day.

**Detect and inform, never install.** The plugin points at `get-genscalator.sc`; it does not run it. A
plugin that silently downloads and places a 41 MB executable, inside an agent session where the human
may not be watching, is precisely the pattern genscalator argues against. The same rule governs the
installer's own Claude Code check: if Claude Code is absent it says so and links the download, because
`tt` is a useful typed CLI on its own and must not present the agent integration as a prerequisite.

**Deferred.** Whether the same integration is worth building for opencode, Codex and similar harnesses
is an open question, not a decision.

## D5 - one dispatcher owns the verb table, and a test owns the dispatcher

**Decision.** The toolbox has exactly one entry point. `tools/dispatch.scala` maps every verb to its entry
function in a single table, and `@main dispatchTypedTools` is the native image's only main class. The
per-file `@main`s are deliberately KEPT rather than deleted, because they are the scala-cli fallback path.

**Why one table.** Before this, `tt <tool>` meant "run the file named `<tool>.scala`", so the set of verbs
was whatever happened to be on disk. That is unnameable: nothing could state what the toolbox offers, and
nothing could be compiled against it. The table makes the verb set a value, so `usage` derives from it
instead of restating it, and a native image has something to route.

**The load-bearing part is the test, not the table.** A hand-maintained table drifts from the file set the
moment someone adds a tool and forgets a line, and it drifts SILENTLY: the new tool still runs under
scala-cli, so nothing looks broken until the native binary is asked for a verb it has never heard of.
`DispatchSuite` asserts the table covers exactly the files carrying a top-level `@main`, currently 43, so
the drift fails a test instead of surfacing as a missing verb weeks later. Same shape as D1: the mechanism
exists because the failure it prevents is quiet.

**Scope, stated honestly.** Only the structural seam shipped, in v0.9.2. The design note this came from,
`tools/DESIGN-single-dispatcher.md`, describes four seams; the other three, typed arguments in, typed
results out, and streaming, are still a plan. Tools take `String*` today. That note carries the plan; this
record carries only what was decided and built. Its status header claimed "not started" for three weeks
after the dispatcher shipped, which is worth recording as its own small lesson: a status line is a claim,
and an uncorrected one misleads exactly the reader who trusts the document most.

## D6 - the release pipeline pins every action it depends on

**Decision.** `.github/workflows/native-release.yml` pins each third-party action to a release tag, not to
a moving branch. `VirtusLab/scala-cli-setup@v1`, `coursier/cache-action@v6`, `actions/checkout@v4`,
`ilammy/msvc-dev-cmd@v1`. The upstream scala-cli documentation shows `scala-cli-setup@main`; this project
deliberately does not follow it there.

**Why.** This workflow does not run tests, it produces the binaries a person downloads and executes. A
moving branch means the toolchain that built today's asset is not the toolchain that builds tomorrow's,
so a green run stops being evidence about the artifact a user actually has. It is the same argument the
install path already makes in D2 and D3: a reviewable, version-pinned thing the human reads before
running, never a blind fetch of whatever is current. Pinning inside CI and arguing for pinning in the
installer would otherwise be inconsistent.

**The cost, stated.** Pinning means upstream fixes do not arrive on their own; a human bumps the tag. That
is the intended trade. The failure mode of a stale pin is visible and boring; the failure mode of a moving
branch is an artifact that changed for reasons nobody recorded.

**Evidence that `@main` would not have helped anyway.** The one leg where the setup action fails is
`windows-aarch64`, which 404s. Checked against VirtusLab's published release assets on 2026-07-27: there
is `scala-cli-x86_64-pc-win32` in `.msi`, `.zip` and `-sdk.zip`, and no `aarch64-pc-win32` build of any
kind. No action version conjures an asset upstream does not publish, so that leg stays `experimental`
until it does.

**What the cache action is and is not.** `coursier/cache-action@v6` caches the coursier cache, which holds
the JVM, the compiler artifacts and the GraalVM that scala-cli fetches for native-image - the bulk of a
cold leg, paid six times per run. It is a speed decision and correctness-neutral by construction: a cold
cache builds the same binary, more slowly. It is recorded here only so that a future reader does not
mistake it for something the build depends on for correctness and hesitate to remove it.

## D7 - how `tt update --native` gets its code, and how it replaces a running binary

**Status: BOTH DECIDED by BR 2026-07-27, after being raised as open. The analysis below is kept because
it is why the answers are what they are; the decisions are stated at the end of each part.**

The entry was written while deliberately stopping short of both, because each is an architecture call
whose failure mode lands on a user's machine and neither could be verified from this developer box.

Everything else that verb needs now exists and is proven end to end: `Lib.releasePlatform` resolves the
asset name (and returns None rather than guessing for the two unpublished platforms),
`tt forge release-download --verify` fetches and checksums, `tt zip check` validates every CRC, and
`tt zip extract --exec` writes a tree whose `bin/tt` actually runs — verified by running it. What is left
is precisely these two questions.

**D7a - how does `tt update --native` reach the download code?** The verb needs "fetch the latest
release's asset for this platform", which is exactly `tt forge release-download`, whose helpers are all
`private` inside `Forge`. Three options, none obviously right: (1) promote a single public entry point on
`Forge` and call it in-process, which couples `update` to `forge` but keeps one HTTP path; (2) shell out
to `tt forge release-download`, which keeps the tools decoupled but makes a toolbox verb depend on a
subprocess reach of exactly the kind this project argues against; (3) duplicate the request code, which is
the SM247 sibling-miss trap and should be rejected outright.

✅ **DECIDED: a NEW SHARED MODULE that both tools include — which is NONE OF THE THREE options above**,
and in particular not the public-entry-point one this entry was leaning toward.
⚠ *This paragraph read "option (3)" until 2026-07-27, which pointed at the very option the list rejects
outright ("duplicate the request code"). The prose always described the shared module, so nothing was
built wrong, but the label would have sent a reader implementing it straight into the SM247 trap. Fixed
by naming the decision instead of numbering it — a decision that outgrew its own option list should stop
borrowing that list's numbering.* The reasoning that changed it: the toolbox's dependency graph is
FLAT today — 45 tools, none calling another's code, all sharing only `lib.scala` — and option (1) would
have spent that property to save a file. A shared module keeps neither tool dependent on the other and
gives the shared code one narrow documented API, which is how `lib.scala` and `reqt-vendored` already
work. ⚠ It cannot live in `lib.scala` itself: that file is deliberately JDK-only so pure text tools
compile fast, while download-and-verify needs `requests` and `os-lib`. So it is a new file (working name
`releaselib.scala`) carrying those deps, and `forge` moves its download/verify internals into it rather
than exposing them. More churn now, no coupling debt later.

**D7b - how is a RUNNING binary replaced?** This is the sharp one. `tt update --native` installs over
`GENSCALATOR_HOME` (default `~/.genscalator`), and the file it replaces may be the very binary executing
the update. Writing through it with `Files.copy(REPLACE_EXISTING)` truncates the inode in place and can
corrupt the running process. The POSIX answer is an ATOMIC rename, which unlinks the old inode and leaves
the running process holding it safely. **Windows cannot replace a running executable at all** — the
standard trick is to rename the running `tt.exe` aside (permitted) and move the new one in, then clean up
the old on next start. ⚠ **That Windows path cannot be tested from here**, and Windows is now a PROVEN
distribution target, so shipping an unverified self-replace there risks leaving a tester with no working
`tt` at all — the worst possible outcome for a verb whose purpose is keeping `tt` current. Options:
implement POSIX-atomic and refuse on Windows with a clear message (honest, incomplete, and bad for a
platform we just made green); implement both and mark the Windows path experimental until someone runs it
on a real box; or stage the new tree and have the human perform the final move.

⭐ **A candidate answer to D7b that needs NO platform branch, found while writing this entry and worth
checking before anyone accepts the options above.** The Windows-specific difficulty is replacing a running
executable, but Windows *does* permit RENAMING one. So the sequence "move the current binary aside, then
move the new one into place" — two `Files.move(..., ATOMIC_MOVE)` calls — is correct on BOTH families for
different reasons: on POSIX because rename unlinks the old inode while the running process keeps it, and
on Windows because renaming a live image is allowed even though overwriting it is not. That is ONE code
path, testable on Linux, and correct on Windows by construction of the same primitive rather than by a
branch nobody can exercise. The only genuinely Windows-shaped residue is deleting the leftover `tt.old`,
which can be a best-effort sweep on next start and whose failure is cosmetic.
⚠ Stated as a CANDIDATE, not a decision: it rests on the claim that Windows permits renaming a running
image, which is true to the best of this author's knowledge and has NOT been verified on a Windows box.
Verify that one fact and D7b likely stops being a fork.

✅ **DECIDED for D7b: VERIFY ON WINDOWS CI FIRST, then implement the single branch-free path.** The
candidate above is adopted *conditionally* rather than on reasoning alone, which is the whole point of the
decision: the `windows-latest` leg already runs the full toolbox suite, so a test that launches a small
executable and renames it while running settles the question on real Windows for the cost of one CI
round-trip. If it passes, rename-aside ships as ONE path for every platform and this stops being a fork.
If it fails, we learn that here instead of from a tester with no working `tt`.

✅✅ **VERIFIED 2026-07-27, and the claim HOLDS. The candidate is now the design, not a candidate.**
`RunningBinaryRenameSuite` (`tools/test/rename.test.scala`) ran inside the buildnative parity pass on
BOTH families and passed:
- **Linux**, locally: `0 failed, 0 ignored, 1 total`.
- **Windows**, CI run **30301424616**, job **90095042530** (`windows-latest`, 5m48s, run SUCCESS):
  `RunningBinaryRenameSuite finished: 0 failed, 0 ignored, 1 total`.
⚠ **The `0 ignored` is the load-bearing number, not the `0 failed`.** The test is guarded on
`-Dtt.native.bin`, so a skip would have printed `1 ignored` and the JOB would still be green — a passing
job proves nothing on its own here. The log line was read directly rather than inferred from the job
status, which is the only way this evidence means anything.
⇒ So: renaming a RUNNING executable succeeds on Windows, the process survives it, and the vacated path
accepts a new binary. `tt update --native` gets ONE branch-free swap path for every platform genscalator
ships, and the residual Windows-shaped work is only the best-effort cleanup of a leftover `tt.old`, whose
failure is cosmetic.

⇒ **The order of work this fixed:** (1) write the rename-a-running-executable test; (2) let CI answer it
on Windows; (3) only then implement the swap. Steps 1 and 2 are DONE; step 3 is what remains. Note what this makes the test: not a regression test for
code that exists, but an EXPERIMENT whose result selects the design — so it must stay in the suite
afterwards, because the design it selected depends on its claim remaining true.

**Why both were written down rather than chosen unilaterally.** A wrong answer here does not fail a test,
it bricks an install. Both questions wanted the human who owns the distribution decision, and one of them
wanted a Windows machine — or, as it turned out, one verified fact instead of a machine.

## D7c - the SECOND shared module D7a did not see coming

**Status: DECIDED and BUILT 2026-07-28, under the rule BR restated that day ("tools depend on lib and not
on each other").** D7a scoped one shared module, `releaselib.scala`, for download-and-verify. Implementing
it surfaced a sibling immediately: `tt update --native` must also UNPACK what it downloaded, and the
extractor — including the whole zip-slip containment guard — lived inside `tt zip`, a TOOL. So the same
argument applied a second time, to code that is if anything more dangerous.

⇒ **`ziplib.scala`**, carrying `entriesOf`, `failures`, `resolveEntry`, `realParentInside`, the zip-bomb
cap, and extraction split into `planExtraction` (adjudicate every entry, throw if any fails) and `extract`
(write an adjudicated plan). `zip.scala` keeps flags, preview rendering and exit codes, and forwards.

**The property that makes this safe rather than merely tidy:** ZipSuite's hostile-entry tests were written
against `Zip.resolveEntry` and still call it — through a forwarder — so they now guard the code
`tt update --native` actually runs. Had `update` grown its own extractor instead, those 15 tests would
have kept passing while covering only one of the two copies. That is the SM247 sibling-miss, and it is the
specific failure this split prevents.

⚠ **It deliberately does NOT live in `lib.scala`**, even though it is JDK-only and would compile there.
`lib.scala` is included by every pure text tool; putting the toolbox's most destructive capability in the
file that `tt text` includes would widen a lot of blast radii to save a file. Same reasoning as
`releaselib.scala`, arrived at from the opposite direction (that one was excluded from `lib.scala` for its
DEPS, this one for its BLAST RADIUS).

⇒ **D7's step 3 is now implemented** — `tt update --native` exists, previews by default, and applies with
`--write`. ~~⚠ It has NOT been run end to end.~~ *(True when written; superseded the same day — see below.
Left visible rather than deleted, because the supervised-first-run discipline is the reason the record
reads this way.)* What was verified at that point: the suite green, with the pure parts unit-tested (the
asset glob, and the rule that staging and retired must be SIBLINGS of the install — a staging dir inside
the directory being renamed would move with it and the second rename would target a vanished path).

✅✅ **RUN END TO END 2026-07-28, BR present, on linux-x86_64. D7 IS NOW CLOSED.** Against the real draft
release `v0.10.0` (8 assets, CI run 30352401422), in this order:
- `tt update --native --tag v0.10.0` (**preview**) — downloaded `genscalator-linux-x86_64.zip`
  (14,739,513 B) and its `.sha256`, reported `ok … verified 1/1 payload(s)`, planned **36 files /
  44,784,074 B, every CRC32 valid**, printed the swap it would perform, and **removed its staging dir**
  (verified absent afterwards). Nothing else written.
- `… --write` with **no install present** — created `~/.genscalator`, and `~/.genscalator/bin/tt chrono
  now` RAN. ⭐ That is the `--exec 'bin/*'` guard proving itself: `java.util.zip` restores no permission
  bits, so without it this is the exit-126 wall a tester hits on their first command.
- `… --write` a SECOND time, now **with an install present** — the run reported `install: … (v0.10.0)`,
  so this exercised the half the first run could not: **move-aside THEN move-in**, the two-rename swap of
  D7b. No `-old-`/`-new-` siblings survived, and the binary still ran.

⚠ **What is still NOT proven, stated so nobody reads "end to end" as "everywhere":** the swap of a
*running* binary on Windows (that rests on `RunningBinaryRenameSuite`, which is real evidence but a
different path), the two other published platforms (same code path, different arch), and the case where
the binary being replaced IS the one executing — here `tt` resolved from the git clone, not from
`~/.genscalator`.

⇒ **The remaining alpha work on this item is zero code.** What is left is publishing a release the
default (no `--tag`) path can see: `/releases/latest` excludes BOTH drafts and prereleases, so an alpha
published as a prerelease leaves a tester's plain `tt update --native` finding nothing.
