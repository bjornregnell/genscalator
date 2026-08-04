# Issue 022: `tt which` splits `$PATH` on `':'`, so it is broken on Windows — plus no PATHEXT and no PE magic

> status: open · labels: toolbox, which, windows, portability, agent-trust · summary: `which.scala:98`
> splits `$PATH` on `':'`, but Windows uses `';'` and every entry starts `C:` — so the entry list is
> shredded, the reported dir count roughly doubles, and a command is found only if it happens to sit in
> the LAST `$PATH` entry (with its drive letter stripped). Bare `tt which tt` returns a confident
> "not found in PATH" on a box where `tt` is on PATH and running.

## Description

Found 2026-08-04 in an alpha field test of the released `v0.10.0` native `windows-x86_64` build on
Windows 10, while trying to perform the very check issue 019 proposes to make the documented first-line
ritual.

```
$ tt which tt
tt: not found in PATH (36 dirs; not a bash builtin either)      # exit 2
```

`tt` *is* on PATH and is the binary running the command.

### Root cause: the PATH separator is hard-coded to POSIX

```scala
// tools/which.scala:98
Option(System.getenv("PATH")).getOrElse("").split(':').toVector
```

On Windows the separator is `';'` and every absolute entry contains a colon after the drive letter, so
splitting on `':'` shreds the list. `PATH=C:\a\bin;C:\b\cmd` becomes:

| fragment | what it is |
|---|---|
| `C` | a relative path that does not exist |
| `\a\bin;C` | a nonexistent path (the `;` is still in it) |
| `\b\cmd` | the last entry, **drive letter stripped** |

Three consequences, all observed:

1. **The dir count is inflated to ~2×** and reported as fact. This box has **35** real `PATH` entries and
   35 colons; `tt which` reports **36 dirs**. Pinned decisively by shrinking `PATH` to a single entry:

   ```
   $ PATH=C:\Users\<user>\.genscalator\bin   tt which tt
   tt: not found in PATH (2 dirs; ...)        # ONE entry reported as two
   ```

2. **Only the last entry can ever match**, and only by accident — it resolves relative to the *current
   drive*, so the same command is found or not found depending on which drive the shell sits on. That is
   also why the one successful hit prints a path with **no drive letter**:

   ```
   $ tt which tt.exe
   tt.exe: 1 in PATH
     \Users\<user>\.genscalator\bin\tt.exe      # C: missing
   ```

3. **A command in any other entry is reported as absent.** `git` is at
   `C:\Users\<user>\AppData\Local\Programs\Git\cmd\git.exe`, squarely on PATH:

   ```
   $ tt which git         -> git: not found in PATH (36 dirs; ...)         exit 2
   $ tt which git.exe     -> git.exe: not found in PATH (36 dirs; ...)     exit 2
   ```

`File.pathSeparator` (or `File.pathSeparatorChar`) is the portable answer and needs no platform branch.

Provenance of the citation, since the tested artifact is a release binary rather than a checkout:
`git diff v0.10.0 HEAD -- tools/which.scala` is **empty**, and line 98 at the `v0.10.0` tag is verbatim
the line quoted above. So this is the code inside the binary under test, not a later regression on main.

### Secondary: no PATHEXT resolution, so the bare command word never resolves

Even once a directory resolves, `hitsFor` probes `dir.resolve(name)` verbatim, so the executable must be
named exactly as typed. On Windows the file is `tt.exe` and the shell finds it via `PATHEXT`:

```
$ tt which tt        -> not found        # with the dir demonstrably resolving
$ tt which tt.exe    -> 1 in PATH        # only the explicit extension works
```

This one matters beyond convenience: `Bash(tt *)` and the whole "which `tt` wins" question are about the
**bare command word**, which is exactly the form that cannot resolve. The toolbox already knows this
hazard in a neighbouring context — `tools/test/cli.test.scala:29` carries the comment *"⚠ Windows:
PATHEXT resolution is a SHELL feature and Java's ProcessBuilder does not do it"* — so the knowledge
exists in the repo but has not reached `which`.

### Cosmetic, same platform

* **PE binaries are unrecognised.** `kindOf` (`which.scala:64`) tests ELF, shebang, `PK\x03\x04`, and
  `0xcafebabe`, but not `MZ` (`0x4d 0x5a`), so a Windows executable reports `data (unknown magic)` where
  Linux reports `ELF binary (64-bit)`. The kind column is a headline feature of this tool
  (script-vs-binary is how you tell a stale plugin-cache launcher from a native build — issue 019), so on
  Windows it reads as suspicious rather than informative.
* **The mode column is `?`**, since `getPosixFilePermissions` throws on NTFS. Defensible as-is, but a
  literal `?` invites the reader to think the file is unreadable.
* **`not a bash builtin either`** is asserted on a box with no bash. `bashBuiltins` is a static list, so
  the claim is not wrong, but on Windows it points at the wrong universe.
* **`name.contains('/')` (line 113) misses Windows paths.** `tt which C:\dir\tt.exe` takes the *name*
  branch, not the path branch. It happens to print correct facts (an absolute `other` makes
  `Path.resolve` return `other`, then `.distinct` collapses the duplicates), but it is labelled
  `1 in PATH` — a false statement about a file that was given as a path and may not be on PATH at all.

**Why it wedges.** `tt which` is a *diagnostic*, so a false negative is its most expensive possible
failure: it is consulted precisely when someone does not know what is going on, and it answers with
confident, quantified wrongness ("36 dirs") rather than an error. Three ways that bites:

1. **It breaks the ritual issue 019 wants to prescribe.** That issue's whole proposal is to make
   `tt which tt` the first thing a user runs, because it settles both the allowlist question and the
   `skillcheck` question (issue 015) in one read-only call. On Windows it settles them wrongly.
2. **The sanctioned tool fails where the forbidden ones work.** `tt doc guard-clean-digest` says
   *"never raw find / grep / ls | head / 2>/dev/null / command -v / which / type"* — `tt which` is the
   compliant replacement for exactly that family. On Windows an agent following house rules has **no
   working way** to answer "which `tt` wins", while `Get-Command tt` answers correctly. A guardrail that
   fails on a platform teaches agents to route around the guardrail.
3. **It misdiagnoses installs.** A fresh Windows user whose PATH is genuinely unset and one whose PATH is
   correctly set get the *same* "not found in PATH" — so the tool cannot distinguish a real install
   problem from its own defect.

Scope note: this is `which`-specific, not a general portability rot. A grep for `pathSeparator` /
`split(':')` across `tools/` finds one other `split(":")` (`ssg.scala:353`) which parses a
tool-defined `key:value` spec, not `$PATH`, and is fine.

## How to reproduce it

On Windows with the native install on PATH (`C:\Users\<user>\.genscalator\bin`):

```
$ tt which tt
tt: not found in PATH (N dirs; not a bash builtin either)     # exit 2 — but tt is on PATH

$ tt which tt.exe
tt.exe: 1 in PATH
  \Users\<user>\.genscalator\bin\tt.exe                       # drive letter stripped
    data (unknown magic)  40.9M  ?  2026-08-04T13:06

$ tt which git                                                # git.exe is on PATH
git: not found in PATH (N dirs; not a bash builtin either)     # exit 2
```

The separator bug alone, isolated from PATHEXT — the reported count must equal the number of real entries:

```
$ PATH=C:\Users\<user>\.genscalator\bin   tt which tt
tt: not found in PATH (2 dirs; ...)                           # one entry, counted as two
```

## Acceptance sketch

* `pathDirs` splits on `java.io.File.pathSeparator`, not `':'`. One-line fix, no platform branch, and it
  makes the reported dir count truthful on every platform.
* On Windows, resolution honours `PATHEXT` (default `.COM;.EXE;.BAT;.CMD;...`) so the **bare command
  word** resolves the way the shell resolves it. Report the file actually found (`tt.exe`) while
  accepting the bare name — that is what makes the answer comparable to `Bash(tt *)` matching.
* `kindOf` recognises `MZ` as `PE binary` (ideally 32/64-bit, as ELF already does), so the
  script-vs-native distinction issue 019 relies on works on Windows.
* Non-POSIX filesystems print something self-explanatory instead of a bare `?` in the mode column
  (e.g. `n/a`), and the builtin clause is omitted (or reworded) when no bash is in play.
* Path-vs-name detection accepts a platform separator or a drive-letter prefix, so
  `tt which C:\dir\tt.exe` takes the path branch and is not labelled `in PATH`.
* Tests: a `pathDirs` unit test over a `';'`-separated, drive-lettered PATH string asserting the entry
  count and that drive letters survive (pure, so it runs on any platform and would have caught this
  without a Windows box); a `kindOf` case for `MZ` alongside the existing ELF case.

## Discussion

### Comment by hmiddelk/Opus5 at 2026-08-04 16:12

Filed from a Windows alpha field test whose primary purpose was to re-test issues 014–021 on a second
platform. All eight reproduce (confirmations appended to each), and this is the one finding that is
genuinely Windows-specific rather than a re-confirmation.

Environment: Windows 10 Enterprise 10.0.19045 (build 19045), x86-64 (AMD64); released `latest` native
build for `windows-x86_64` (zip sha256 `79f4c702bdc2107515eac2d5e24f8b62bf221c03ddcf9a1a56a9425de3f2c7c2`,
13 951 676 B), unpacked to `C:\Users\<user>\.genscalator` by `scala-cli run get-genscalator.sc`;
`bin/tt.exe` 40.9M, install `VERSION.txt` reads `v0.10.0`. Scala CLI 1.16.0 (Scala 3.8.4) via Coursier;
OpenJDK 11.0.16.1 on PATH. Plugin cache at `0.9.2` — see issue 021, which is why the binary and the
plugin disagree about the version on this one machine.

Relationship to issue 019: that issue proposes promoting `tt which tt` into the documented ritual, and
this one says the ritual cannot work on Windows until the split is fixed. Suggest 019's acceptance
sketch gain a line noting the platform dependency, or that the two land together — a doc change alone
would send Windows users to a broken check. Also touches issue 015: `tt which tt` is the prescribed way
to establish *which* install won, which is the fact that explains the bare-`skillcheck` failure.

Not raised as a defect, but noted for the record since it sits next door: the bootstrap installer
handles Windows correctly. With `SHELL` unset it falls through to a hint printing the exact
`[Environment]::SetEnvironmentVariable(...)` snippet for the user to run, which works. Two cosmetics
there, worth a sentence rather than an issue — it labels a *supported* platform as
`PATH: unrecognised shell ''`, and its closing instruction is `then run: tt help`, which per issue 020
exits 2 with `no such tool 'help'`. So a Windows newcomer's first command after a clean install prints
an error.

Agent disclosure: the platform sweep, the root-cause reduction (the colon-split hypothesis, confirmed by
the entry-count experiment and then located at `which.scala:98`) and this issue text were produced by an
AI agent (Claude Opus 5) under human direction; the human reviewed and submitted.
