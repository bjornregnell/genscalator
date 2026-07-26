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
