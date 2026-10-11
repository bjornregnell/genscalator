# Issue 261011-which-merged-usr-duplicates: tt which reports one file twice on merged-/usr systems, so a shadowed install can be phantom

> status: open 2026-10-11 · labels: toolbox, which, linux, silent-wrong · measured against: v0.10.2
> (from `VERSION.txt`) at `c5f1043`, native binary, Linux (Ubuntu, merged /usr) · summary: on a
> merged-/usr system `/bin` is a symlink to `/usr/bin`, so when both are on PATH, `tt which` lists the
> same file twice, the second as "(shadowed)". It reads as two installs. The same lookup feeds the
> runner banner from PR #26 (issue 061), which would print a phantom "+1 shadowed" for any
> distro-packaged tool, the exact two-installs confusion the banner exists to clear up.

## Description

```
$ tt which time
time: 2 in PATH (first wins)
  /usr/bin/time
    ELF binary (64-bit)  26.5K  rwxr-xr-x  2024-04-08T16:21
  /bin/time (shadowed)
    ELF binary (64-bit)  26.5K  rwxr-xr-x  2024-04-08T16:21
```

One file, reported as two. `WhichTool.hitsFor` (`tools/which.scala:118-124`) collects the hits per PATH
directory and de-duplicates with `.distinct` on the UNRESOLVED paths, and `pathDirs` (`:108-109`) maps
PATH entries with `Path.of` and no normalisation through symlinks. `/bin/time` and `/usr/bin/time` are
different strings for the same file, so both survive.

Most Linux distributions now ship merged /usr (Ubuntu, Debian, Fedora, Arch), and typical PATHs list
both `/usr/bin` and `/bin`. So every tool installed by the distro's package manager shows a phantom
shadow. The cases that matter are the ones where the shadow count is the evidence: the issue-050 /
issue-061 investigation was about WHICH of two scala-cli installs won, and PR #26's banner prints the
shadow count for exactly that reason.

Found during the sandbox review of PR #26 on 2026-10-11; confirmed on this box with the output above.

## How to reproduce it

On a merged-/usr Linux box with both `/usr/bin` and `/bin` on PATH:

```
tt which time      # any binary the distro installed in /usr/bin
```

## Acceptance sketch

* De-duplicate hits by the REAL file (`toRealPath`, or `Files.isSameFile`), not by the path string,
  while still DISPLAYING the path PATH resolution would use (the first hit).
* Do not lose information: when two PATH entries reach one file, say so in one line (for example
  `also reachable as /bin/time (same file)`) rather than silently dropping it. Hiding the alias is
  better than a phantom install, but naming it is better still.
* A real second install (a different file) must still be reported as shadowed. Test both: a symlinked
  directory pair in a temp dir yields ONE install, two distinct files yield two.
* PR #26's banner inherits the fix through `hitsFor`; no change needed there.
* Keep `realPath` failures (a dangling link, a permission error) from aborting the lookup: fall back
  to the unresolved path for that hit.

## Discussion

### Comment by bjornregnell at 2026-10-11

Found by the sandbox sub-agent reviewing PR #26 and confirmed live with `tt which time`. Filed as its
own issue because the root cause is in `tt which`, not in the banner, and fixing it there fixes both.

Agent disclosure: confirmed and drafted by an AI agent (Claude Opus 5.5) in session with me; reviewed by me.
