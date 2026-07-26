# The dir-scoped run: our own guidance taught a shape the permission layer cannot analyse (2026-07-26)

**The finding, up front:** to regenerate a generator that resolves its output path relative to the
current directory, the agent ran `env --chdir=<abs> scala-cli run …`. That is not a lapse — it is
**exactly the shape the warp ember teaches**, in its anti-regression checklist, as the sanctioned
replacement for a guard-blocked `cd`. The permission layer then flagged it: *"env with --chdir flag
cannot be statically analyzed."* Both are right. **The substrate contained two rules pointing in
opposite directions, and the agent followed the one that had been loaded into its context most
recently and most imperatively.**

## The contradiction, verbatim from the two documents

| source | what it says |
|---|---|
| the warp ember, §0 anti-regression checklist | compound `cd &&` *"AND even a bare `cd` are guard-blocked; to run a command in another dir use `env --chdir=<abs> <cmd>` (proven this era; the guard blocks `cd`)"* |
| `tt doc guard-clean-digest`, DIRECTORIES | lists `tt sbt --dir` and `tt git --repo`, then: *"other programs have no dir-scoped shape yet — **flag the gap, do not reach for `cd`**"* |

The digest says *flag the gap*. The ember says *here is the shape*. The ember wins in practice, because
it is read first at turn zero, it is phrased as a FORBIDDEN-to-ALLOWED mapping, and it carries the
reassurance "proven this era" — a claim that was **inherited rather than re-verified**, and that this
episode contradicts.

## Why the permission layer is right, and the ember wrong

`env` is a **general program executor**. `env VAR=x --chdir=/anywhere anyprogram anyargs` can launch
anything, in any directory, with a modified environment. An allowlist cannot bound what actually runs,
which is the same property that makes allowlisting an interpreter unacceptable in this project. So
routinely reaching for `env --chdir` **re-opens through the back door precisely what blocking `cd`
closed**: it converts an analysable command into an opaque one, and it does so while feeling like
compliance, because the literal token `cd` never appears.

That is the uncomfortable part. The rule was obeyed at the level of the *string* and broken at the level
of the *property the rule protects*.

## The uncovered noun, and the reach it predicted

The noun is **"run a program in another directory"**. It is covered for exactly two programs, `tt git
--repo` and `tt sbt --dir`, and uncovered in general. RT056 predicts that an uncovered noun with real
traffic produces a raw reach; this is one, and it went unflagged by every syntax check because the
command has no `cd`, no pipe, no redirect and no substitution.

## The fix is at the callee, not the caller — and the repo already contains the proof

The generator declares its own dependence on the working directory:

    val next = Path.of("media", "graphical-profile") // run from the repo root

That comment IS the defect. A tool that resolves paths against the current directory makes the caller
responsible for controlling the current directory, which is the thing the caller is not allowed to do.

Its sibling in the same repo already solves it correctly: `deploy/buildnative.sc` takes **`--root <abs>`**,
and has been invoked all day from a *different repository's* working directory with no `cd`, no `env`,
and no guard friction whatsoever. Same problem, same repo, one script solved it and the other did not.

**So the primary fix is a `--root` flag (or a self-locating walk-up, as `lib.scala` does for the tools
dir) on every script that is "run from the repo root".** That removes the need for a dir-scoped runner
entirely, rather than making the dangerous runner more convenient.

## The design conclusion worth keeping

The tempting answer to "should this be `tt scala`?" is *yes, add `tt scala run --dir <abs>
--main-class X`* — and that is probably worth having, since `tt scala run` today takes a project
directory but offers neither a working-directory argument nor a `--main-class` passthrough, so it
genuinely could not express this call.

But the **general** version of that answer is wrong. A `tt run --dir <abs> <any program>` verb would
recreate `env` under a friendlier name and inherit exactly its unanalysability. The pattern that works
is the one already emerging: **per-tool directory arguments on bounded verbs** (`tt git --repo`, `tt sbt
--dir`, and a proposed `tt scala --dir`), each of which can only launch one known program, **plus
callee-side `--root` flags** so that most scripts never need one. Bounded surfaces keep commands
analysable; a general dir-scoped executor cannot.

## Actions this specimen argues for

1. **Fix the ember template**: `env --chdir` should be listed as a shape to avoid, not as the allowed
   replacement. Its "proven this era" endorsement is now falsified.
2. **Give `DesignLang.scala` a `--root` flag**, matching `buildnative.sc`.
3. **Consider `--dir` and `--main-class` on `tt scala run`**; deliberately do NOT build a general
   dir-scoped program runner.

## Threats and limits

- Single specimen, and the agent is reporting its own reach, which is the least trustworthy narrator;
  what is checkable is the command that ran and the two documents quoted above.
- The permission layer's message is the only evidence that the shape is treated as unanalysable here;
  whether it is *denied* or merely *flagged* depends on configuration not inspected during this episode.
- It is not established how often the general noun actually occurs. Two covered programs and one
  uncovered case is thin evidence for building anything; the `--root` fix costs least and should come
  first regardless.
