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
needs a second mode that compares `VERSION` against the latest published release over HTTPS. This is on
the critical path, not polish, because plugin update-awareness is built on it (see D4).

**Rejected alternative: ship sources to users, protected somehow.** Considered marking a shipped
`tools/` read-only, or naming it something that does not read as a build tree, so that a user editing it
could not silently invalidate the binary. Rejected once the premise collapsed: see D3, no user needs
`tools/` at all. Also rejected a `--with-sources` installer flag, on the grounds that a contributor does
not want sources, they want the clone, with history, branches and the ability to open a pull request. A
source drop without git would be both unhackable and uncontributable.

## D3 - the user install is one self-contained archive per platform

**Decision.** Six release assets, `genscalator-<os>-<arch>.zip`, each containing `bin/tt` (`tt.exe` on
Windows), `docs/`, `reqts/PRD.md` and `VERSION`. The installer verifies a published SHA-256, aborts on
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
