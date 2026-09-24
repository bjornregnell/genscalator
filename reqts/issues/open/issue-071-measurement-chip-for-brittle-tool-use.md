# Issue 071: a chip that turns ON measurement of brittle dynamic tool use — `ProbeBrittle`

> status: open 2026-09-24 · labels: statusline, mode, tooling, research, tt-toolbox ·
> measured against: v0.10.2 (from `VERSION.txt`); `tt mode --file`, `--modes-file` and
> `--limits-file` re-verified present at `5adb395` (2026-09-23)
> · summary: issue 070 measured
> a session's tool-use mix with a throwaway script and found 28% tool-choice / 37% pipeline-clean over
> 95 Bash calls. Proposes making that measurement a first-class, repeatable capability: a declared chip
> that marks a boundary in the transcript and turns on classification of brittle dynamic tool use, with
> a `tt` tool doing the counting. Named `ProbeBrittle`, under a proposed `Probe<What>` convention for
> measurement chips — see the naming section for the two rejected candidates and why.

## Description

Issue 070 reports the finding: under a harness regime that prescribes raw bash, the agent's tool-use
mix degrades along two independent axes — which tool it reaches for, and whether it wraps that tool in
a shell pipeline. The measurement that produced those numbers was a scratch Scala tool written mid
session and kept in a local `tmp/`. This issue is about promoting the capability, not the finding.

The ask, in the human's words: *a chip for turning on measurement of brittle dynamic tool usage.*

### What already exists, and what is missing

Existing, and enough to build on:

* **`tt mode`** — declared chips, session-scoped, rendered on statusline line 2. The declaration
  mechanism is done.
* **The transcript** — every Bash `tool_use` with its full command string, already read once per render
  by the statusline for the rot gauge.
* **A working classifier**, as a scratch file (`bash-mix.scala`, ~120 lines): extracts commands anchored
  on `"input":{"command":"`, classifies each command STRING, reports counts and two rates, and can
  split at a boundary regex.

Missing:

* the classifier as a real, project-agnostic `tt` tool rather than one machine's scratch file;
* a boundary that is recorded rather than grepped for (today the split anchors on the literal text
  `tt mode add AutoBash` appearing in the transcript, which works and is ugly);
* any live visibility — the numbers exist only when someone runs the tool afterwards.

### Sketch

**1. `tt bashmix` (name negotiable) — the measurement.**

```
tt bashmix <transcript.jsonl> [--since <iso|boundary>] [--json]
```

Pure read → classify → print, no state. Classifiers are the interesting content and they are all pure
predicates over a command string: `isTt`, `hasPipe`, `pipesToPager`, `isHeredocEdit`, `isCompound`.
Each is one line and each is independently testable against a fixture table — which is what makes this
a toolbox tool rather than a script: the *judgements* get pinned down and reviewed, not re-typed per
session. Output: counts plus the two rates 027 argues for (tool-choice, pipeline-clean), `--json` for a
study harness.

**2. Timestamped mode declarations — the chip records WHEN, not just WHAT.**

`tt mode add <label>` writes the label; what it does not write is *when*. Today arm-splitting works by
grepping the transcript for the literal command text `tt mode add AutoBash` — it works, and it is a
hack that breaks the moment the label is renamed or declared twice.

Give each declaration a timestamp and the hack disappears: `tt bashmix --since ProbeBrittle` resolves
the boundary from the mode store itself. Sketch:

* **Store**: the mode file's line becomes `<label>\t<iso-8601>` (UTC). A bare label with no tab still
  parses, as a declaration of unknown time — so existing files keep working and no migration is needed.
* **Removal**: `tt mode rm` currently just drops the line, which loses the fact that the mode *ended*.
  Two options, and they are not equivalent: keep the state file as state and add an **append-only
  event log** (`gs-modes.log`: `add|rm  label  iso  session`), or keep only the current state and
  accept that history is lost on `rm`. The log is the honest version — a session's phases are events,
  not a snapshot — and it is what makes retrospective analysis possible for sessions nobody thought to
  measure at the time.
* **Location**: whichever file, it inherits issue 067 — the mode store resolves `$HOME/.claude`
  literally and should follow `CLAUDE_CONFIG_DIR`. Worth landing 067 first so this does not add a
  second file in the wrong home.

**Useful well beyond this issue**, which is why it is worth doing even if the rest of this issue is declined:

* the mode line can show **duration** (`ProbeBrittle 42m`), which makes a stale chip visible — a mode
  declared three hours ago and never cleared is currently indistinguishable from one declared a minute
  ago, and the stale one actively misleads (an `Afk` chip that outlived the human's absence is the
  dangerous case);
* `gs where` gains a real session timeline instead of a current-state snapshot;
* any study that wants to correlate behaviour with declared state gets exact intervals rather than
  "some time during this session".

**3. The live rate — where it gets interesting, and awkward.**

The statusline already parses the transcript each render; classifying the Bash commands in the same
pass is close to free, and line 2 could carry `ProbeBrittle 28/37` (tool-choice / pipeline-clean, live).
See the perf note in `statusline.scala` — the transcript read is already the tool's dominant cost, and
a full re-classification per render on a long transcript needs the same incremental-read treatment, or
`--no-tok`-style opt-out.

**But live to whom?** The statusline is rendered for the *human*; the agent never sees it. So a live
rate is feedback for the person watching, not for the process generating the behaviour. If the goal is
to change the agent's behaviour rather than to inform the human, the number has to arrive on a channel
the agent reads — a PreToolUse hook's output, or a periodic reminder. That is a different mechanism
with a different cost, and it should be decided deliberately rather than discovered after building the
statusline version. (Related: 027's mechanism-3 question, whether a hook can even see the regime.)

### Naming: `ProbeBrittle`, and the two concepts it has to keep apart

`AutoBash` was a codename and names the **cause** (a harness regime that prescribes bash). The thing
being measured is the **effect** — how brittle and dynamic the tool use is — and the effect is general:
the same two rates are worth watching in a session with no auto mode at all, and would have caught the
same regression a year ago for entirely different reasons. So the measurement chip should be named for
the measurement, and `AutoBash` stays as the regime chip from 027 (declaring it may *imply* the
measurement one).

**Settled: `ProbeBrittle`.** The `Probe` half says *measurement is on*, which is exactly what this chip
does, and it generalises into a family the mode vocabulary currently lacks:

| chip | means |
|---|---|
| `ProbeBrittle` | watching brittle / unreviewable tool use |
| `ProbeRot` | watching context rot (cf. the existing `RotVigil`) |
| `ProbeSpend` | watching token spend |

A `Probe<What>` convention gives every "I am measuring X right now" chip one shape, and distinguishes
measurement chips from state chips (`Afk`, `Solo`, `HighContext`) — a distinction the vocabulary does
not currently make.

**Two candidates were rejected, and the reasons are the specification.**

`AutoBash` (the original codename) names the **cause** — a harness regime that prescribes bash. Wrong
level: the same two rates are worth watching in a session with no auto mode at all. A cause-name on a
general instrument is wrong the first time it is used outside that cause. `AutoBash` survives as the
*regime* chip in issue 070, which is what it was always describing.

`ProbeUnsafe` was proposed next and rejected for a collision: `safe` / `unsafe` is already load-bearing
here and means something else. Safe mode is the capture-checking direction where the **compiler** tracks
effects and mutation, and a tool is "not-safe" when the planned `--safe-mode` flag would have to exclude
it (see the `scala-style` skill). To a reader holding that vocabulary, `ProbeUnsafe` reads as "probing
for tools that are not Safe-mode-ready" — a different activity in the same repo.

`Brittle` is the word the skills already use for what is actually being measured: "brittle bash" is
native vocabulary in `avoid-guard-stall` and in issue 070. It collides with nothing, and names the
property rather than a verdict — which also keeps the chip usable when the brittleness is deliberate and
correct (a `strings | grep` over a 40 MB binary is brittle by nature and was the right call).

## Decision needed

1. **Is the measurement a `tt` tool, and is `bashmix` the right shape?** A pure read-classify-print
   tool over a transcript is unambiguously in scope for the toolbox; the question is whether the
   classifiers belong in it or in a shared place the statusline can also use.
2. **Should a declared mode record its timestamp?** Small, generally useful, and it removes the
   transcript-grep hack. Probably worth doing regardless of the rest of this issue.
3. **Live in the statusline, or not at all?** Cheap-ish to add, but it feeds the human, not the agent —
   and the per-render cost lands on the tool's known hot path.
4. **Is `Probe<What>` adopted as the convention for measurement chips?** The chip name itself is
   settled (`ProbeBrittle`); the open part is whether the family is a convention or a one-off, and
   whether declaring the regime chip `AutoBash` should imply the measurement chip.
5. **Do declarations get timestamps, and is there an event log?** The timestamp is small and useful
   on its own (stale-chip duration, `gs where` timelines). The append-only log is the larger, more
   honest version and can come later — but the file format should be chosen with it in mind.

## Discussion

### Comment by bjornregnell/Opus5 at 2026-09-15 18:20

Proposed by the human after reading 027. Filed separately because 027 is a finding that wants a fix and
this is a capability that wants a design, and bundling them would let the (contentious, unbuilt)
instrument hold up the (measured, actionable) report.

One honest objection to the whole premise, which I would rather state than have found in review: **the
measurement does not need the chip at all.** The transcript holds every command, so any session can be
measured retrospectively, with no declaration and nothing switched on. So a chip that "turns on
measurement" is not turning anything on — the data was always there.

What the chip actually buys is two narrower things, and the issue is stronger if it claims only these:

* **Arm partitioning.** A declared, timestamped boundary is what makes before/after comparison exact
  rather than a grep for a magic string. That is real, and it is the part 027's study design needs.
* **Intent, recorded.** Declaring the chip says "I am watching this now", which is what makes the
  numbers a check rather than an audit. Whether that changes anything is exactly the open question.

A risk worth naming: a live rate visible to whoever is being measured invites optimising the number
instead of the behaviour — piping to `tail` less because it scores badly, not because it hides an exit
status. The rates are proxies and should be treated as such; the failure modes in 027 are the thing
that matters, and they are what the rates are *for*, not the other way round.

Prior art in this repo to reuse rather than reinvent: the `in-session-experiment` and `research-methods`
skills already cover running a controlled probe on the agent's own behaviour with real work as the
vehicle, which is precisely this. If this lands, the reporting side belongs in `research/reports/`.

### Comment by bjornregnell/Opus5 at 2026-09-15 18:34

Naming settled in three moves, recorded because the reasons are more useful than the result: the agent's
codename `AutoBash` named the cause rather than the effect; the human proposed `ProbeUnsafe`, which
contributed the `Probe` prefix and the family idea; the agent flagged that `Unsafe` collides with Safe
mode's established meaning; the human ruled for `ProbeBrittle`. The live chip in the session that
produced these issues was renamed to match.

One measurement consequence of the rename, noted so a later reader is not confused: the before/after
split in issue 070 still anchors on the ORIGINAL declaration text (`tt mode add AutoBash`), because that
is when the behavioural boundary actually fell. Renaming the chip does not move the boundary — which is
a small live demonstration of why the timestamped-declaration item above is worth building.

The timestamped-declaration idea was promoted from a sub-bullet to its own sketch item in the same
pass, on the human's instruction. Worth noting it is the one part of this issue with value independent
of the measurement question: a stale `Afk` chip that outlived the human's absence is a live hazard
today, and a duration on the mode line would surface it.

Agent disclosure: this comment was produced by an AI agent (Claude Opus 5) under human direction; the
human reviewed and submitted.

Agent disclosure: this issue text was produced by an AI agent (Claude Opus 5) under human direction. The
agent is also the subject the proposed instrument would measure, which is a conflict of interest the
reader should weigh — including in the objection above, which argues for a smaller version of a feature
that would be pointed at the agent.

### Comment by bjornregnell/Opus5 at 2026-09-24 14:05

Re-verified before filing, against `5adb395`.

* **The mechanisms this proposal builds on are present**: `tt mode --file` (`mode.scala:39`) and
  `tt statusline --modes-file` / `--limits-file` (`statusline.scala:571-572`), plus their tests in
  `tools/test/cli.test.scala` and `session.test.scala`. The sketch does not invent an entry point.
* **The `in-session-experiment` and `research-methods` skills exist**, so the "prior art to reuse
  rather than reinvent" note is accurate rather than aspirational.
* **The classifier has been sharpened since drafting**, and the fix is the part worth upstreaming.
  The first version anchored on a bare `"command":"`, which counted the agent *writing about* a
  statusLine key as the agent *running* a command — 190 "calls" against ~95 real. The anchor must be
  `"input":{"command":"`, the only place a command was actually executed. A transcript is a log with
  the agent's own prose interleaved, and any `tt bashmix` must classify extracted events, never match
  patterns against record lines. That belongs in the acceptance criteria, not just in this comment.

**One piece of evidence against the declarative half of this proposal**, gathered after it was drafted:
the agent declared `ProbeBrittle`, wrote the issue arguing the rates improved, and then reached for
`head` and `tail` on a file forty minutes later, in the same call as a correct `tt text count` on that
same file. A chip the agent declares, and cannot see, did not prevent the regression. That strengthens
mechanism 3 (hook-derived) over mechanism 2 (self-declared), and it strengthens the objection already
recorded above — that the chip's real value is arm partitioning and recorded intent, not behaviour
change.

Agent disclosure: the re-verification and this comment were produced by an AI agent (Claude Opus 5)
under human direction; the human reviewed and submitted.
