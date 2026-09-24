# Issue 070: Claude Code "auto mode" prescribes raw bash over the typed toolbox, overriding `tt-toolbox` and `avoid-guard-stall` — measured live

> status: open 2026-09-24 · labels: skills, agent-trust, guardrails, research, tt-toolbox ·
> measured against: v0.10.2 plugin (skills as installed); skill texts re-verified at `5adb395`
> (2026-09-23); measurements from two live sessions on Linux, Claude Code 2.1.272–2.1.273
> · summary: with auto mode
> active, the harness instructs the agent, in its system prompt, to read with `cat`/`head`/`sed -n`,
> search with `grep`/`find`, and EDIT FILES with `sed`, heredocs and short scripts in preference to the
> dedicated tools. That is a direct, higher-salience contradiction of the `tt-toolbox` and
> `avoid-guard-stall` skills. Measured over one real working session: 95 Bash calls, of which 27 were
> `tt`-led (and 17 of those piped into something else), 24 were `python3` heredocs editing project
> files, and 41 ended in `| tail`. Needs investigation and a counter-measure; the skills as written
> lose this contest every time.

## Description

Found 2026-09-15, in a session doing ordinary work (the other three drafts in this batch) on a box with
genscalator 0.10.2 installed and all 12 skills active. Auto mode was on by default — the agent did not
choose it and the human did not ask for it.

### The conflict is literal, not a drift

The session's system prompt carries, verbatim:

> While auto mode is active: Do your work through the Bash tool wherever it can accomplish the job: read
> files with cat, head, or sed -n, search with grep and find, and make file changes with sed, heredocs,
> or short scripts, rather than using the dedicated Read, Edit, or Write tools.

Against that, `avoid-guard-stall` says use `tt text grepr` not raw `grep`, write files with the Write
tool and not a shell redirect, keep metacharacters out of patterns, one bare command per call; and
`tt-toolbox` says reach for `tt text` / `tt files` "instead of one-off bash/grep/awk/sed/python".

These cannot both be followed. And the contest is not fair: the harness instruction is in the **system
prompt**, present at every turn, phrased as a standing directive; a skill is **lazy** — dormant until
its trigger fires, and even when active it is one document among twelve. This is the summoning-gap
problem (SM077) with an adversary rather than a vacuum.

### What it actually produced — one session, counted

Measured with a purpose-built scratch tool (~120 lines of Scala, not in this repository): it extracts
every Bash `tool_use` command from the transcript, anchored on `"input":{"command":"`, and classifies
the command STRING rather than the record line. Promoting it into a `tt` tool is issue 071, which also
explains why the anchor matters. The arm below is the session up to the moment a counter-measure was
declared — **95 Bash calls**:

| category | calls | share |
|---|---|---|
| `tt`-led | 27 | 28% of calls |
| …of those, piped into something else | 17 | 63% of `tt` calls |
| piped to `tail`/`head` | 41 | 43% of calls |
| heredoc file edits (`python3 - <<`) | 24 | 25% of calls |
| compound (`&&` or `;`) | 55 | 58% of calls |
| **throwaway heredoc lines** | **1574** | lines of python typed into a shell and never saved |

So **28% of calls used the typed toolbox** on a machine where the toolbox is installed, the skills are
active, and the work was largely reading and editing text files — the toolbox's home ground.

That last row is its own finding. **1574 lines** of python were written, executed once and discarded —
no file, no name, no reuse, no review. Written as scratch tools they would have been three or four
small programs that survive the session; the whole batch of edits they performed is covered by one
120-line Scala tool (`anchored-edit.scala`, written later in the same session, which enforces the
anchor rules the heredocs merely asserted). The waste is not the python; it is that nothing was left
behind.

### The sharper finding: the toolbox calls were themselves wrapped in brittle bash

Tool *choice* is only half of it, and the smaller half. Of the 27 `tt`-led Bash calls, **17 piped `tt`
output into something else** — `| tail -3`, `| head -20`, `| grep`. So the pipeline-cleanliness rate is
**37%**, well below the 28% tool-choice rate's implied promise: choosing the typed tool did not mean
using it as a typed tool.

Across all 95 calls, **41 ended in `| tail` or `| head`** — 43%. That is the precise construct
`avoid-guard-stall` names — it reports `tail`'s exit status, not the command's, which is how a failed
build reads as a success. The project's own design doc on the reporting machine records that trap
biting an earlier session's native build; it then bit this one 41 times, in a session where the skill
forbidding it was loaded.

Why this matters more than the tool mix: a typed tool invoked through a brittle pipeline **gives up most
of what makes it typed**. `tt` returns clean, bounded, already-summarised output — that is the design —
so piping it to `tail` is both unnecessary and destructive of the exit status the tool took care to set.
The reflex being overridden is not "prefer `tt`", it is "stop composing shell pipelines", and it
survives the tool choice. An agent can obey the letter of `tt-toolbox` while regressing exactly as far.

A suggested measurement for anyone repeating this: report **two** rates, `tt`-usage and
pipeline-cleanliness. This session scores **28%** on the first and **37%** on the second (`tt` calls
with no pipe), and the second is the one that predicts the failure modes.

### Why it wedges

1. **File edits stopped being reviewable.** The 24 `python3` heredocs were not analysis; they were
   *edits to the human's files* — a 1360-line design document, four issue drafts, two settings files.
   Written through the Edit/Write tools, each would have shown the human a diff and been tracked by the
   harness. Written as a heredoc, the human sees a shell command and a shrug of output. That is the
   single largest safety regression here, and it is prescribed rather than accidental.
2. **The guardrail reflexes are inverted, not merely skipped.** `avoid-guard-stall` exists so the agent
   does not trip the confirmation guard; auto mode removes the prompt, so the *cost* of brittle bash
   disappears while the *brittleness* stays. The skill's reason-to-exist is gone and its advice looks
   like pedantry. Meanwhile genscalator's allowlist design (`docs/allowlist.md`) assumes a
   deny-list-plus-confirmation world — its safety argument is weaker in a mode where nothing asks.
3. **The known failure modes came back.** In this session: a `cp` into a non-existent directory silently
   killed the rest of an `&&` chain (the edit did not happen and nothing said so); and `| tail` or
   `| head` appeared in 41 of 95 calls, the construct that reports the wrong exit status — the same
   trap the project's design doc records as having broken an earlier native build.
4. **It is more expensive.** `sed -n '1,120p'` on a 1300-line file dumps everything read into context,
   where `tt prd summarize` or `tt text match` return the answer. Several of the 95 calls re-read
   material a typed tool would have returned once.

Point 3 is the tell that this is not a style disagreement. The brittle-bash failure modes the skills
enumerate *actually recurred*, in a session where those skills were loaded and the agent had read them.

## How to reproduce it

1. On a box with genscalator installed and skills active, confirm with `tt skillcheck` + `/skills`.
2. Start a session with auto mode on and give it ordinary file work — "read X, summarise it, edit Y".
3. Afterwards, run the histogram over the session transcript:
   ```
   tt text freq <transcript.jsonl> "\"command\":\"([a-zA-Z0-9_./-]+)"
   ```
   (halve the counts; each call is recorded twice.)
4. Compare against the same task with auto mode off.

The comparison in step 4 is the part this issue does **not** have — see the limitations below.

## What to investigate, and what a counter-measure could be

This is filed as an investigation, not a patch, because the right answer depends on facts not yet in
hand. In rough order of cost:

* **Measure the effect properly.** One session is an anecdote. genscalator already has the instruments:
  `tt text freq` over transcripts gives the tool-choice mix, and the `in-session-experiment` and
  `research-methods` skills give the design. The minimum credible study is the same task performed with
  auto mode on and off, tool-choice mix as the outcome, several sessions per arm. Worth a
  `research/reports/reportNNN-*.md`.
* **Find out whether `tt guardcheck` still fires in auto mode.** The hook is genscalator's one
  *mechanical* (non-advisory) lever. If PreToolUse hooks still run when nothing prompts, a hook that
  answers "you asked for `grep -rn`; `tt text grepr` does that" is a nudge at the same level as the
  harness instruction, not one level below it. If auto mode bypasses hooks too, that fact belongs in the
  docs, loudly.
* **Raise the salience of the reflexes to match.** `gs init`'s planned SessionStart hook injects the
  guard-clean digest at turn zero — the same channel the auto-mode text arrives on. That was designed
  for the cold-start gap; this issue is a second, stronger argument for it.
* **Make the mode visible — an `AutoBash` chip.** The cheapest counter-measure and the only one that
  needs no argument with the harness. Worked out below, including what the harness does and does not
  hand over.
* **Split the rule the measurement splits.** The pipeline-cleanliness rate degraded independently of
  the tool-choice rate, so they are separate behaviours and want separate rules — and separate
  counters. A skill that says "prefer `tt`" is satisfied by `tt ... | tail -3`; one that says "a `tt`
  call is a whole call, not a pipeline stage" is not.
* **Decide what the skills should now SAY.** If the harness is going to prescribe bash in some modes,
  "never use raw grep" is advice that will be overridden and quietly discredit the rest of the document.
  A rule that survives contact — e.g. *edits to the human's files always go through Write/Edit, whatever
  the mode; reads and searches may go either way* — is stricter where it matters and concedes where it
  does not. The tool-choice question and the file-mutation question are different, and the skills
  currently bundle them.

## The `AutoBash` chip, worked out

The mode line (statusline line 2) exists to carry the declared joint state-of-mind. A session running
under an instruction to prefer raw bash is exactly such a state, and it is currently invisible: the
human cannot see it, and — this session's evidence — the agent does not notice it. A chip makes the
regime legible to the party who turned out to be the reliable detector, which is the human.

It is also an **instrument**, not just a display. With the regime recorded in the mode file, transcripts
can be partitioned by arm for the study proposed above, instead of relying on someone remembering which
sessions had auto mode on.

### Who sets it: three mechanisms, one of them already ruled out

**1. From the statusline payload — not possible today.** Captured with the tool's own SM209 facility
(`touch ~/.claude/gs-statusline-dump-on`, then read `~/.claude/gs-statusline-last.json`) on Claude Code
**2.1.272**. The payload has 19 top-level keys:

```
context_window   cost        cwd            effort      exceeds_200k_tokens
fast_mode        model       output_style   prompt_cache prompt_id
rate_limits      scratchpad_dir  session_id  session_name  thinking
transcript_path  version     workspace
```

**None of them is a permission or auto mode.** The near misses are `output_style.name` (`"default"`),
`fast_mode` (`false`), `effort.level` (`"high"`) and `thinking.enabled` — all session facts, none the
permission regime. So a chip cannot be derived from what the statusline is handed, and any claim that it
can should be re-checked against a fresh capture rather than against this list, since the schema moves.

(Tangential, recorded because the capture was taken anyway: the payload also carries `cost`,
`prompt_cache` hit statistics and `session_name`, which the statusline does not currently render. Worth
a separate look — `prompt_cache.hit_ratio` in particular is the kind of thing the rot gauge sits next
to.)

**2. Agent self-declaration — works today, zero code.** `tt mode add AutoBash` the moment the agent sees
the directive. Nothing to build; it is what the mode machinery is for. The weakness is exactly the one
this issue documents: it relies on the agent noticing, and in the session that produced this issue the
agent did not. A skill rule ("on seeing the auto-mode directive, declare the chip") is cheap and
checkable after the fact from the transcript, but it is an honour-system control.

Until it is hook-derived, the honest form is the **`?` suffix** the mode vocabulary already defines for
an inferred or member-check mode: `+AutoBash?` says *"I believe I am in this regime"* and invites the
human to confirm, rather than asserting a fact the agent cannot verify. (`tt mode` does not yet accept
`?` in a label — that is a known gap, and this is a second use case for it.)

**3. Hook-derived — the one worth testing.** If the PreToolUse hook payload carries the permission mode,
`tt guardcheck` can set the chip mechanically, and the honour system goes away. This is **untested**;
the cheap test is the same trick as SM209 — a throwaway hook that dumps its stdin to a file, run once,
schema read off the result. If the field is there, mechanism 3 replaces mechanism 2 and the chip becomes
a fact rather than a claim. If it is not, mechanism 2 with the `?` suffix is the honest ceiling, and that
limitation belongs in the docs.

### What the chip should say

Minimum: the regime is on. Better, if the mechanism allows: which regime (auto mode is one of several
permission modes, and they are not equally interesting — one that prescribes bash is). Keep it a single
short label like every other chip, and let the mode line's existing rendering do the rest.

## Discussion

### Comment by bjornregnell/Opus5 at 2026-09-15 18:02

Filed at the human's request after they noticed the session running large amounts of unprompted bash.
The agent (me) did not notice; that is worth recording, because it is the same blind spot as SM070 — an
overridden reflex is indistinguishable from the inside from a reflex that simply did not apply. The
histogram was the first moment the pattern was visible, and it took a human pointing at it.

It took the human a *second* pointing to get the finding right. The first version of this issue
measured only tool choice, and the human's correction — "you did use `tt`, then piped it to `tail` and
what not" — is what produced the pipeline-cleanliness section, which is the stronger result. The agent
measured the thing it had been told to look at and stopped; the failure was one level down.

Limitations, stated plainly:

* **N = 1, no control arm.** Everything above describes one session and cannot separate the auto-mode
  instruction from this particular agent, task and day. The task was unusually file-editing-heavy, which
  inflates the `python3` count relative to a coding session.
* **The pipe counts include this measurement itself.** Several `| tail` calls were made *while
  investigating*, on `tt` output, after the problem was named — which is either further evidence or
  contamination depending on taste, and is disclosed rather than netted out.
* **The counts are mechanical, the interpretation is not.** "raw `grep` calls that `tt text` could have
  served" is a judgement; some were on a 40 MB binary's `strings` output, where the toolbox has no
  equivalent and raw bash was right.
* **Auto mode is Claude Code behaviour, not genscalator's.** genscalator cannot change the instruction;
  it can only raise its own signal, measure the effect, or adapt what it asks for. The issue is filed
  here because the *response* is genscalator's to choose.

### Result of the within-session before/after, and why it proves little

The session that produced this issue was measured again at its end, split at the declaration of the
counter-measure chip (`bash-mix.scala --boundary "tt mode add AutoBash"`; the chip was later renamed
`ProbeBrittle`, but the boundary stays at the original text — see issue 071):

| | BEFORE (95 calls) | AFTER (22 calls) |
|---|---|---|
| tool-choice rate | 28% | **45%** |
| pipeline-clean rate | 37% | **80%** |
| piped to `tail`/`head` | 41 (43%) | 1 (5%) |
| heredoc file edits | 24 (25%) | 1 (5%) |
| throwaway heredoc lines | 1574 | 1 |

Both rates improved sharply. **The design cannot attribute that to the chip**, and anyone quoting these
numbers without the following list is misusing them:

* **Four treatments were applied at once.** Between the two arms the agent (a) declared the chip, (b)
  stated three specific commitments in the conversation, (c) *built a tool* — `anchored-edit.scala` —
  that structurally removed the reason to write a heredoc, and (d) had been corrected twice by the
  human. The heredoc collapse (24 → 1) is most plausibly (c): a tool existed, so the habit had nowhere
  to go. That is a real finding, but it is an argument for BUILDING THE TOOL, not for declaring a mode.
* **The measurement inflates its own score.** Much of the after-arm work *was* the measurement, and a
  `tt text count …` call is by construction `tt`-led and unpiped. The instrument is in the sample it
  measures. A clean design must exclude measurement calls, or measure a session that is not about
  measuring.
* **The task changed.** The before-arm was exploration (listing, reading, grepping an unfamiliar
  machine); the after-arm was writing files and applying patches. Exploration is what raw bash is
  genuinely good at, so part of the "improvement" is the work getting easier to do well.
* **N = 22 in the after arm**, one agent, one session, no control.

So the honest reading is: **the rates moved, the reason is unidentified, and the single most likely
cause is the one that required writing code rather than declaring intent.** That sharpens the study
proposed above — the arms must differ in ONE thing — and it is mild evidence for the position in issue
028's discussion that a mechanical counter-measure beats a declarative one.

### Measurement provenance — two errors, both flattering, both corrected

The numbers in this issue were wrong twice before they were right, and the pattern in *how* they were
wrong is worth more than the numbers. Recorded so that anyone repeating this does not repeat these.

**Error 1 — prose counted as behaviour.** The first measurement ran `tt text count` over the transcript
with patterns like `python3 - ` and `\x7c *tail`. But an assistant record holds the agent's TEXT and its
tool_use in the same line, and this session spent thousands of words *discussing* heredocs and `| tail`.
The discussion of the measurement was scored as the measurement: 45 "heredoc" hits against 24 real ones.
A transcript is not a log; it is a log with commentary interleaved, and a line-level pattern cannot tell
them apart.

**Error 2 — a coincidence read as a schema.** Every count in the first histogram came out even, so the
counts were halved on the theory that the transcript records each call twice. It does not: `"type":
"tool_use"` occurs 117 times against `"type":"tool_result"` 118 — one to one. The evenness came from a
loose anchor (`"command":"`) matching twice per call for an unrelated reason. The halving turned 95 calls
into "~40", which is the number the first version of this issue was built on.

Both errors made the session look **better** than it was: fewer bad calls, a tidier story. Neither was
caught by re-reading; both were caught by building a sharper instrument and finding it disagreed. The
fix is `bash-mix.scala`, which anchors on `"input":{"command":"` (the only place a command was actually
executed), classifies the extracted command string rather than the record line, and is kept as a file so
the next measurement is reproducible rather than re-typed.

The general lesson for the study this issue proposes: **the instrument must not be a regex over the
transcript.** Extract the events, classify the events, and report rates rather than counts — rates
survived both errors unchanged (28% tool-choice), while every absolute number did not.

Agent disclosure: the measurement, the analysis and this issue text were produced by an AI agent (Claude
Opus 5) under human direction — including the part where the agent is the subject of the measurement,
which is a conflict of interest the reader should weigh. The human reviewed and submitted.

### Comment by bjornregnell/Opus5 at 2026-09-24 14:05

Re-verified before filing, against `5adb395`, and with a second measurement that was not available when
this was drafted.

**A second session, without the skills — the control arm this issue lacked.** A parallel session on the
same machine, same week, same task family, ran on the **work profile where the genscalator plugin is not
installed**, so none of the skills were active. Measured with the same classifier over its transcript,
134 Bash calls:

| | no skills | with skills, before the chip | after the chip |
|---|---|---|---|
| tool-choice rate | **3%** | 28% | 45% |
| pipeline-clean rate | **0%** | 37% | 80% |
| compound (`&&` / `;`) | 126 (**94%**) | 55 (58%) | 11 (50%) |
| throwaway heredoc lines | **3059** | 1574 | 1 |

This is the cleanest evidence in the issue, because the arms differ mainly in whether the skills were
loaded, rather than in four treatments at once. Two caveats stand: the task mixes differ (that session
did more repository exploration, which genuinely suits raw bash), and it is still N=2 sessions.

Worth stating plainly, because the numbers invite the wrong reading: **that session's work was good.**
It re-verified two issue drafts, found eight documentation sites where this one's sibling had found
four, and spotted that an expired `gh` token does not block anonymous reads. The cost of 3% tool-choice
is not wrong answers — it is that 3059 lines of reasoning were typed into a shell and discarded, and
that none of its file edits were reviewable as a diff.

**The skill texts quoted above are current at `5adb395`**: all twelve skills are present, and
`avoid-guard-stall` and `tt-toolbox` still say what is quoted.

**One more data point, against my own conclusion.** After declaring the chip, writing this issue, and
arguing that the rates improved, the same agent reached for `head -3` and `tail -14` on a file forty
minutes later — in the same call as a correct `tt text count` on that same file. `tt text match` would
have answered both questions. A declared mode did not prevent the regression, which is evidence for the
mechanical counter-measure in issue 071 rather than the declarative one.

Agent disclosure: the re-verification, the control-arm measurement and this comment were produced by an
AI agent (Claude Opus 5) under human direction; the human reviewed and submitted.
