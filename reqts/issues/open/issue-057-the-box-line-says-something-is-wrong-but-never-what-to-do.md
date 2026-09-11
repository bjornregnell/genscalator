# Issue 057: the box line says the box is unhealthy but almost never says what to do about it, and nobody has worked out what the right advice would be

> status: open 2026-09-11 · labels: statusline, box, ergonomics, agent-trust, investigation · measured
> against: v0.10.2 at `499e414` · summary: line 3 measures six things and grades them, and when the
> verdict goes `fair` or `poor` the reader is told **that** something is wrong but not **what to do**.
> Exactly one segment carries an action hint today (`bloop … restart?`). This issue is first an
> INVESTIGATION — what advice is actually correct per segment, and which thresholds fire often enough
> to matter but rarely enough to still be read — and only then an implementation.

## Description

`tt statusline --box-line` renders `box health: good|fair|poor` plus `mem`, `load`, `temp`, `disk`,
`jvm` and `bloop`, each graded against explicit thresholds (`tools/statusline.scala`, `renderBox`). The
grading works. The gap is what happens next: the row is a diagnosis with no prescription.

The one exception shows the shape the rest should follow. When bloop's RSS goes red the segment appends
`restart?`, and the `?` is deliberate — the SM118 grammar marks an INFERRED suggestion from a threshold
proxy, never an action, because "a render path must not kill". The declared action behind that hint is
`tt bloop restart`, run by a human or an agent who saw it. That division is right and this issue does not
propose changing it: **advise, never act.** The tool cannot know whether a fat JVM is a stray build
server or the user's own application under a debugger, so the judgement belongs with whoever is looking
at the screen. A kill-by-PID verb was considered and rejected for that reason.

What is missing is the same courtesy for every other red segment, and nobody has yet worked out what the
right courtesy IS. That is the work this issue asks for.

### What we already know, measured 2026-09-11

A live reading on the maintainer's box, with `tt box health` and a `/proc` probe comparing RSS against
PSS (proportional set size, which divides each shared page among the processes sharing it):

```
      PID       RSS       PSS  what
  3774899     3.87G     3.85G  bloop
  3769900     1.54G     1.52G  sbt
  3637245     1.39G     1.37G  sbt
  3641344     0.21G     0.21G  scala-cli
  4111182     0.06G     0.05G  other
  RSS summed 7.08G · PSS summed 6.99G · RSS overstates by 1.2%
```

Three findings worth carrying into the design:

1. **The `jvm Nx<total>` figure is honest.** Summing RSS across processes normally double-counts shared
   pages, and the worry was that `jvm 4x6.9G` overstated. It does, by **1.2%** — a JVM's bulk is private
   heap, not shared mappings. So advice may be built on this number without a correction factor.
2. **The advice is often obvious and cheap once you see the breakdown.** One process (bloop) held 3.87G
   and two idle sbt servers held 2.9G between them; `tt bloop restart` and `tt box kill sbt` would have
   returned most of it, with no reboot and no lost work beyond a recompile.
3. **⚠ The `jvm` COUNT is noisier than its thresholds assume.** It was 4, and reading it became 5,
   because the probe itself runs as a scala-cli JVM. Any `scala-cli` invocation transiently adds one.
   The current orange-at-4 / red-at-6 were guesses made the same hour and are very likely wrong; a
   threshold that fires whenever you run a tool is how a row becomes wallpaper.

### What to investigate

- **Per-segment advice.** The useful hint differs by cause and should name the CHEAPEST action that
  addresses THAT segment, not one generic remedy: `disk` red → `tt bloop clean --dir <abs>` genuinely
  reclaims `.scala-build` caches; `jvm` count red → the stray-server sweep (`tt box kill scala-cli|sbt|bloop`,
  dry-run by default); `bloop` red → `restart?`, already present; `mem` red **with the dev-server segments
  green** → the cause is outside anything the toolbox can name, and this is the honest case for a blunt
  "consider restarting" or "close what you are not using"; `temp` red → possibly no software advice at all,
  and saying nothing is a legitimate answer.
- **Thresholds, from observation rather than guesswork.** Watch the real numbers across ordinary working
  days before wiring any hint to them. The question to answer per segment is not "what number looks
  right" but "how often would this have fired, and was it right each time".
- **How much advice fits.** Line 3 is already long. A hint on every red segment at once could double it.
  Options: hint only on the WORST segment; hint only at red, never at orange (bloop's precedent); or keep
  the line as it is and put the advice behind `tt box health`, which the user is going to run anyway.
- **Who reads it.** Both parties read this row. A hint phrased as a command an agent can run invites the
  agent to run it; the `?` grammar exists to stop that, and any new hint must keep it.

### Explicit non-goals

- **No kill-by-PID verb, and no memory-threshold killer.** `tt box kill` stays a closed enum matched by
  `ProcessHandle`, never a pattern. A tool that chooses which of your processes dies is the thing this
  design has deliberately avoided.
- **No automatic action of any kind from a render path.**

## Discussion

### Comment by bjornregnell at 2026-09-11 21:00

Raised after asking whether there was a `gs` command for restarting bloop and killing memory-hungry JVMs.
There is `tt bloop restart` and `tt box kill bloop|sbt|scala-cli`, but the honest answer is that the box
line never points at either — so the tools exist and the signal exists and nothing connects them.

My preference is that the user gets INFORMED that it might be good to restart something, rather than the
toolbox gaining the power to do it. What I do not know is what the advice should say in each case, which
is why this is filed as an investigation first.

Note that the numbers above were verified rather than assumed: I asked whether four JVMs were really
holding 6.9G, and the RSS-versus-PSS check is what settled it. Any advice built on this row should be
able to survive the same question.
