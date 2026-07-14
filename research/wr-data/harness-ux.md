# WR data — harness UX (human↔agent input channel)

**WR = Workflow Research** (see [`README.md`](README.md)). This file logs friction on the **harness itself** —
the human↔agent *communication channel* — as distinct from [`genscalator-self-dev.md`](genscalator-self-dev.md)
(dynamic-shell / tool friction). These are things genscalator cannot fix (we don't own the harness), but they
are real costs on joint productivity and belong in the WR corpus as upstream asks + agent-side mitigations.
Cross-ref [`../002-communication-bandwidth.md`](../002-communication-bandwidth.md) (channel bandwidth),
[`../011-human-state-and-joint-zone.md`](../011-human-state-and-joint-zone.md) (perception gap / relayed signals).

---

## Double-post race on arrow-up edit (2026-07-03, BR-reported)

**Symptom.** The same user message lands **twice** in the transcript, the two copies differing only by a small
edit (observed pairs this session: `run in!` → `run it!`; `gest` → `gets`). The agent then has to reconcile
which of two near-identical messages is authoritative, mid-task.

**Repro (BR's).** Press **↑ (arrow-up)** to recall the last submitted message for editing, make the fix, press
**Enter** — a **race** posts it *twice*: the recalled original AND the edit both fire. "I press Enter while
having arrow-up for edit but it gets posted TWICE — that's bad UX."

**Why it costs.** It's the input-channel cousin of the *stale-signal* problem: the agent may act on the
**first** (pre-edit) copy, or double-execute, or burn a turn asking which is meant. The user's *intent* is
unambiguous (the second is a correction of the first), but the channel presents it as two peer messages. It
also **inflates apparent message count** — noise on the very bandwidth the WR work is trying to keep clean.

**Agent-side mitigation (adopt now, harness-independent).** Treat a **rapid pair of near-identical user
messages** — especially one prefixed `edit:` / `I meant:` — as **one edited message**, and take the **later**
copy as authoritative (it is the correction). Do **not** re-execute the task twice, and do not stall asking
"which did you mean?" when the delta is an obvious typo/word fix. This is the input-side of *idempotency*: a
retried-with-correction message should collapse to a single action.

**Harness-side fix (upstream ask).** Make arrow-up-edit-then-Enter **atomic** — it should **replace** the
pending/last input, not append a second submission. Minimally, **debounce** near-simultaneous identical
submissions, or let an edit supersede the prior message in-place (as chat UIs with a real "edit message"
affordance do) rather than posting a fresh one. The race window between "recall for edit" and "submit" is the
bug.

---

## Permission-parser internals surface as the *reason* string (2026-07-03)

**Symptom.** When the Bash safety analyzer declines to auto-approve, the reason it shows the human is sometimes its
**internal parser vocabulary**, not a human explanation. Observed this session: **"Contains simple_expansion"** (a
`$f` variable reference — a tree-sitter-bash grammar node name) and **"Contains zsh `<N-M>` numeric-range glob"**
(from `<->` written in commit-message prose). Both are accurate to the *parser* and opaque to a *human* who is not
holding the tree-sitter-bash grammar in their head.

**Why it costs.** The reason-string is the human's only window into *why* a command needs confirmation. When it
reads as grammar-node jargon, the human cannot tell a **real hazard** ("this could delete files") from a **benign
construct that merely defeats static analysis** ("this has a `$var`, so its effect can't be proven in advance") —
which are the two cases most important to distinguish, collapsed into one cryptic label. It pushes the human toward
rubber-stamping (can't judge → just approve): a confirmation-fatigue *collapse* driver, the bad end of the CF
spectrum.

**Upstream ask.** Translate the AST/lexer classification into a **human-facing risk sentence** — e.g. "This command
builds part of itself from a variable (`$f`), so I can't check in advance exactly what it will run." State *what is
unprovable and why it matters*, not the node name; keep the node name behind a `--why`/verbose expand for the
curious. **Agent-side:** the genuine fix is upstream (self-dev) — don't emit the dynamic construct; use the typed
tool so no scary-and-opaque prompt appears at all. Cross-ref [`genscalator-self-dev.md`](genscalator-self-dev.md)
(the `simple_expansion` for-loop reflex; the `<->` false positive).

---

## The "thinking" spinner is a harness heuristic, not model self-report (2026-07-03, BR-reported)

**Symptom.** The status spinner's phrasing flip-flops — **"almost done thinking" → "thinking some more" → "still
thinking"** — sometimes *backwards* (from "almost done" back to "more"). BR: *"you really can't trust that info;
does the agent even know why it flips?"*

**Honest mechanism.** The agent does **not** author or observe these strings. They are a **harness-side heuristic**
(elapsed-time / output-stream progress guesses) rendered by the CLI; the model has no channel to see them and no
control over them, so it **cannot explain a specific flip**. "Almost done" is the harness's *guess* about output
progress, not the model reporting "I am nearly finished reasoning." When generation runs longer than the heuristic
expected, the guess revises downward ("more"/"still") — which reads, wrongly, as the *agent* changing its mind.

**Why it's the same class as context blindness.** It's a **perception gap**: a signal *about* the agent, shown to
the human as if it came *from* the agent. Sibling of the agent's context-usage blindness (the agent can't see its
own fill; the human relays it) — here **inverted**: the human sees a progress signal the *agent* can't see, and
misreads it as self-report. In both, a status indicator's **provenance** (harness vs model) is unmarked, so the
human mis-attributes it. Cross-ref [`../011-human-state-and-joint-zone.md`](../011-human-state-and-joint-zone.md)
(perception gap / relayed signals) and the context-usage-blindness thread in `006-smart-zone-ceiling.md`.

**Upstream ask.** Mark provenance: harness-generated status should avoid first-person-sounding phrasing ("almost
done thinking") that implies model introspection — a neutral "working… (Ns)" removes the false self-report reading.
**Agent-side:** when asked about these strings, be honest that they are harness heuristics the model can neither see
nor steer — do **not** confabulate a reason for a flip.

---

## `cd` + output redirection tripped a path-resolution-bypass guardrail (2026-07-03, BR-reported)

**Symptom.** A pure *inspection* command — `cd /abs/introprog && ls *.sh Makefile … 2>/dev/null; ls compendium/*.tex …`
— triggered a manual-approval prompt: *"Compound command contains cd with output redirection — manual approval
required to prevent path resolution bypass."* Nothing was created, deleted, or moved; the command only listed files.

**Honest mechanism.** The guardrail fires on the *shape* (`cd` composed with a redirection `2>/dev/null` in one
compound), not the *effect*. The concern is legitimate in general — `cd` moves the resolution root, so a later
redirection `> file` in the same compound could write somewhere the static analyzer didn't expect (path-resolution
bypass). But here the redirection was a benign `2>/dev/null` on a read-only `ls`, so the *shape* matched a hazard
class the *effect* never entered. Same false-positive family as the `simple_expansion` and `<->` cases: the analyzer
classifies syntax, and I supplied syntax that *looks* like the hazard.

**Why it's my reflex, not the guardrail's fault.** The `cd &&` compound was **unnecessary** — every path in that
command could have been absolute in a single bare command with no `cd` at all (`ls /abs/introprog/*.sh …`). I reach
for `cd X && …` out of shell habit; the only place I *genuinely* need cwd=introprog is `sbt --client`, which takes
no redirection and so never trips this. So the friction is self-inflicted: the typed-tool / bare-absolute-path
discipline (cross-ref [`genscalator-self-dev.md`](genscalator-self-dev.md), the compound-shell reflex thread)
dissolves it entirely. Third instance this session of the same root cause — **compound shell constructs are my
recurring collision point with the permission layer**, and the cure is always "emit simpler syntax," never "argue
with the guardrail."

**Upstream ask.** Distinguish `cd` composed with a *writing* redirection (`>`, `>>`, `tee` — the actual bypass) from
`cd` composed with a *discarding/reading* one (`2>/dev/null`, `< file`); the latter cannot write outside the
resolved root and needn't prompt. **Agent-side (the real fix):** default to bare, absolute-path, single commands;
reserve `cd` for the narrow tools that require cwd and never pair it with a redirection.

---

## Input-focus race: an in-flight Enter meant for the message box lands on a confirmation dialog (2026-07-03, BR-reported)

**Symptom.** BR was composing his own message while an agent tool-confirmation prompt appeared; he "happened to
press Enter just when I was ready" and the keystroke went to the *confirmation* instead of his message box — his
message was "lost in the feed" and a pending approve/deny was resolved by an Enter he never intended for it. BR:
*"bad UX risk, to be racing with confirmation and accidentally press enter to the wrong input; this is actually
bad."* The confirmation in question was triggered by an **avoidable** agent command — a `sed -n '/…/,$p'` (the `$`
tripped the analyzer) that should have been a `Read`/`tt text` call in the first place (cross-ref the
scratch-over-bash and compound-shell reflex threads).

**Why it's serious (not cosmetic).** This is a **safety** race, not just annoyance. The whole point of a
confirmation prompt is a deliberate human review gate; if a keystroke intended for a *different* input can silently
satisfy that gate, the gate's guarantee is void — a command can be approved (or denied) with zero actual review,
precisely when the human's attention is elsewhere (composing). It is the input-channel analogue of clickjacking:
the decision UI captures intent aimed at a sibling UI. Worse under AFK/long-run conditions, where the human types
messages *asynchronously* to a stream of agent actions, so message-composition and confirmation-arrival routinely
overlap.

**Two independent failure surfaces.** (1) **Harness:** the confirmation dialog and the message composer share a
focus/return-key path with no debounce or focus-guard, so a race exists at all. (2) **Agent:** every avoidable
confirmation prompt *opens* a race window; the metachar/bash-hack reflex (here: `sed …$p`) manufactures prompts
that needn't exist. Fewer prompts ⇒ fewer windows. The two compound: the agent's noise raises the *rate* of the
harness's race.

**Upstream ask.** (a) Debounce/guard the confirmation control: ignore an Enter for ~Nms after the dialog appears,
and/or require the confirmation to be *explicitly focused* (not inherit a return-key aimed at the composer); (b)
never let a keystroke buffered for the message box be redirected to a just-appeared modal — route it to whichever
input was focused when the key was pressed, not when it was processed. **Agent-side (the real, in-my-control fix):**
drive confirmation frequency toward zero — use `Read`/`tt` typed tools instead of `sed`/`grep`/`awk` bash-hacks, and
bare single commands instead of metachar/compound ones, so the review gate only ever appears for actions that
genuinely warrant a human decision. Cross-ref [`genscalator-self-dev.md`](genscalator-self-dev.md) and
[`../011-human-state-and-joint-zone.md`](../011-human-state-and-joint-zone.md) (perception/attention gaps under long runs).

**Reinforcement — the AGENT-INITIATED variant (2026-07-03, same session, BR-reported twice).** The mirror image of
the above: an **agent-triggered modal** (an `AskUserQuestion` popup with options) appeared **while BR was mid-typing
his own message and interrupted his typing** — *"came while I was typing and interrupted my typing, bad UX."* Same
focus-steal, opposite initiator: here the *agent* opens a modal that grabs the input channel the human is actively
using. It compounds a directive BR gave one message earlier (*"don't disrupt your current work"*). **Two agent-side
rules, both in my control:** (1) during an autonomous / AFK run, do **not** fire an interrupting question-modal for
*minor* disambiguation — make a reasonable default and surface the choice as **plain text** the human can answer at
leisure (no focus-steal); reserve modals for genuine blockers. (2) A clarifying question is itself a *disruption
cost* — weigh it against just proceeding. **Harness-side ask:** an agent-initiated modal should not seize keyboard
focus from an in-progress human compose buffer — queue it, or show it non-modally, until the human's input is idle.

---

## Keystone: the harness "never-ending task" cull → design long jobs to be monitored + resumable (2026-07-04, BR)

**Event.** The flagship sweep (a ~4 h background task) was **externally stopped by the harness** at 31% — no crash,
clean cells to the end. The harness appears to cull background tasks it deems effectively **never-ending** (an upper
bound on background-task lifetime). BR: *"we need to be smart about not reaching the upper bound on allowing tasks to
be seen as 'never ending' by the harness."*

**Why it matters.** A long autonomous job on a single harness-managed background task is **fragile by default**: one
cull (or crash, OOM, session event) throws away all in-flight progress unless the job was built to survive it. Not a
harness bug to route around so much as a **design constraint to build for**.

**The design principle (BR, ratified by what saved this run) — two pillars for ANY long autonomous job:**
1. **Progress monitors + health indicators.** Emit liveness the human/agent can read *without* trusting the process
   itself — here, each completed cell **appends a row** to the results TSV (sweep-main.scala line 116), so `wc -l` on
   the TSV is a truthful progress+liveness signal (the stdout progress bar, by contrast, buffered and *lagged* — a
   false indicator that made the run look stuck at cell 808 when it was really at 946). Pair it with an out-of-band
   watcher (the scheduled monitor tick) that reads that signal, checks thermals, and detects a stall.
2. **Cache/checkpoint results so an interruption RESUMES, not restarts.** Persist each unit of work as it completes
   (append-only), and make relaunch **idempotent**: read what's already done and skip it. Here the added resume-skip
   let a relaunch continue from cell 946 with zero duplication — a ~4 h loss became ~0 min. Corollary: back up the
   partial before touching anything; keep any frozen protocol intact (skip ≠ drop/dup).
3. **(Implied) don't *look* never-ending.** Chunk work and/or emit steady progress so the job reads as
   making-progress — and expect the cull anyway, so (1)+(2) are the real insurance.

**Scope.** Keystone principle for **every genscalator long-runner** — the sweeps, autotranslate `--all`, the model
pulls: build them append-checkpointed + externally-monitorable from the start, not after the first cull. Cross-ref
the resume implementation in [`../experiments/indent-vs-braces/RUN-LOG.md`](../experiments/indent-vs-braces/RUN-LOG.md)
(2026-07-04 entry).

## Arrow-up "edit a just-sent message" race → double-post (2026-07-04, BR explained the mechanism)

**Mechanism (BR).** When BR wants to fix a typo in a message he *just* pressed Enter on, he presses ↑ (arrow-up) to
recall+edit it. But there's a **race against the agent**: if he's **too late** — i.e. the agent has already begun
processing ("eaten") that message — the env **can't edit the in-flight instance**, so instead of an edit it creates
a **double-post** (the correction lands as a *new* message). This is the same input-race family as the agent-modal
focus-steal and the Enter-lands-on-confirmation cases above — a timing race between a human input action and the
system consuming a prior input.

**BR's deliberate coping habit (so future sessions read it as intentional, not accidental).** Rather than fight the
race, BR **does not try to edit an Entered message**; he **adds a new message**, and for a simple typo sends a terse
`edit: wrong -> right` note (this session alone: `say→saw`, `of→if`, `soel→sole`, `NewToll→NewTool`, `humansä→human's`,
`typy→typo`, …). These are a **feature of his workflow**, not confusion.

**Agent-side (confirmed working).** Treat a rapid near-identical pair as **one** message, later copy authoritative;
apply any `edit: X -> Y` note as a correction to the referenced word and act **once**; never stall asking "which did
you mean" on an obvious typo delta. BR worried the double-post is "even more confusing for you" — it is not, with
this rule. **Harness-side ask:** widen the edit window (allow editing a just-sent message until the agent's *first
token*, not until enqueue), or make a fast follow-up detected as an edit-of-previous rather than a new post.
See memory `harness-double-post-edit-race`.

## Bash-reflex cluster → typed `tt` tools; and `$(…)` substitution trips the confirmation guard (2026-07-04, BR live-flagged 3x)

While diagnosing the tt-toolbox split, BR flagged **three consecutive** commands of mine as WR data — all the same
class: **filesystem/shell introspection I did in raw bash instead of a typed tool.**
- `readlink -f … ; echo "---" ; ls … 2>&1 | head ; echo "---" ; ls -la …`
- `type tt ; echo "---" ; grep -nE 'tt|TT_TOOLS' ~/.bashrc | head`
- (earlier) `which tt ; echo "---" ; cat "$(which tt)" | head -40`

**Two distinct lessons:**

1. **`ls`/`readlink`/`type`/`cat`/`grep` + echo-separators is a bash reflex that a typed tool already partly cures.**
   BR: *"why `ls` when you have os-lib? it's a reflex — can we do better with tt?"* The sharp version: **`tt files`
   (os-lib–based) already exists in the toolbox and I reached for `ls` anyway** — so this is first a **discipline gap**,
   not only a missing tool. os-lib gives `os.list`/`os.walk`/`os.exists`/`os.followLink` returning typed `Seq[os.Path]`
   to compute on — no `2>&1`, no `| head`, no `echo "---"` assembly. The **echo-separated multi-`ls` compound** is the
   *same* assembly antipattern as the monitor-tick and `printf`-message compounds already logged here: several shell
   fragments glued with `echo` dividers because there's no single typed call that answers the actual question.
2. **Command substitution `$(…)` trips the env confirmation guard** (`Contains simple_expansion`) → manual approval →
   and *that* approval prompt is where BR **accidentally clicked yes** on the *failing* `tt git` (input-race family, cf.
   the Enter-on-confirmation and arrow-up cases above). So `$(…)` in a gated bash line is a **double cost**: forces a
   confirmation *and* creates a mis-click surface. Cure: avoid command substitution in gated bash — pass literal paths,
   or answer the question with a typed tool that needs no substitution.

**Tool candidates this crystallizes (fold into `tools/DESIGN-single-dispatcher.md` candidate list):**
- **`tt files` — USE IT** (already exists); the fix here is habit, not code. Consider widening it to cover the reflexes
  above (list/stat/walk with typed output) so it's the obvious reach.
- **`tt which <tool>`** — typed toolbox introspection: where a tool resolves, which toolbox(es) exist, is it on
  `~/.local/bin` vs a repo clone. Would have answered this whole diagnosis in one typed call with **no** `readlink`
  + `echo` + double-`ls` + `$(which …)`.
- **`tt web --head <url>`** (2026-07-04) — a light URL **existence/health** check (status + last-modified + size, no
  body), distinct from `tt web`'s content fetch+convert. Cures the `curl -sI -o /dev/null -w '%{http_code} …'` reflex I
  used to confirm `compendium-en.pdf` is live on fileadmin. `tt web` today is too heavy for "does this URL 200".
- **`tt files … --head N`** (2026-07-04) — preview the first N lines of matched files, so the
  `for f in …; do head -8 "$f"; done` loop (which trips `simple_expansion` on the `$f` var) becomes one typed call.
  Same cluster as the `ls`/`cat`/`grep` reflexes: the loop exists only because no single typed call previews matches.
- **`tt files … --names`** (2026-07-04) — emit just the bare tool/base names (optionally sorted), so
  `tt files … | tail -n +2 | sed 's|.*/||;s|.scala||' | sort | tr '\n' ' '` (used to list genscalator tool names)
  collapses to one call. The `sed`/`sort`/`tr` post-processing chain is the reflex; the tool should offer the shaped
  output directly. Reinforces: every time I pipe `tt files` through `sed`/`awk`/`sort`, that's a missing output mode.
- **Composing tt tools in-process — the strongest DESIGN signal (BR 2026-07-04).** I ran
  `echo "=== pdf ==="; tt files DIR pdf; echo "=== toc ==="; tt files DIR toc; echo "=== tex ==="; tt files DIR tex`
  — three `tt files` invocations glued with `echo` dividers. BR: *"tool candidate, or internal streaming inside tt of
  composed existing tools?"* — and the **deeper** reading is the point: this is exactly the
  [single-dispatcher](../../tools/DESIGN-single-dispatcher.md) motivation. Two cures, escalating: (a) a multi-arg mode
  (`tt files DIR pdf,toc,tex`); (b) the real one — **in-process tool composition** the dispatcher enables, where tools
  return `LazyList[ToolResult]` and compose without a shell (`tt files DIR pdf | tt files - toc …`), so the `echo`-glue
  disappears entirely. The bash `echo "==="; toolA; echo "==="; toolB` pattern IS the manual, lossy version of what
  streaming composition does typed and in-process. Filed as motivating evidence in the DESIGN's streaming section
  (now written up there under "Composition is the whole point of the typed stream").
- **`tt transcript <session-id | --recent N>`** (2026-07-04) — read/extract from session `jsonl` transcripts (first
  user message, tool calls, text). Recovering BR's FleetView "panic writes" needed a raw `jq` + `for`-loop that
  tripped the *"Contains expansion"* guard — the same reflex cluster. A typed transcript reader cures the reflex AND
  unlocks WR-data mining of past sessions (a research primitive, not just a papercut fix). Pairs with `RAW-DATA.md`.
- **`tt frontmatter [file | --all]`** (2026-07-04) — validate a markdown file's YAML frontmatter (real parse via
  snakeyaml/JVM — no new Scala dep) + required-field check (`name`, `description`), typed `OK` / `error at line:col`;
  `--all` lints every `skills/*/SKILL.md`, `memory/*.md`, `blog/*.md`. **Motivated beyond the reflex:** the
  `blog-assistant` SKILL.md had an unquoted colon in `description` (`non-absolutist: calibrated`) → YAML "nested
  mappings not allowed in compact mappings" → VS Code preview rejected it, and per the web the SAME class **silently
  drops a skill from discovery** (no error). So this is a *reliability* tool, not just a cure for the `python3 -c
  'import yaml…'` reflex I reached for to verify the fix. Encodes the genscalator gotcha: **quote any frontmatter value
  containing a colon.** Refs: vercel-labs/skills#1094 (silent drop), github/vscode-github-actions#205 (the error).
  **Design (BR 2026-07-04) — honor the general name, generalize-ready not generalized:** three layers — (1) *extract*
  the fenced head (format-agnostic: `---`/`---` YAML · `+++`/`+++` TOML · `{`/`---json` JSON · or a `--until <regex>`
  escape hatch); (2) *detect* the format from the fence **or** pin with `--yaml`/`--toml`/`--json`; (3) *dispatch* to
  that format's validator (snakeyaml now; TOML/JSON added later only when a real file needs them — a one-line dispatch
  add, not a rewrite). The general name is honored by the **architecture** (the dispatch seam), not by pre-building
  unused parsers — the single-dispatcher ethos + foundations "start specific, generalize-ready". **Evidence it earns
  its place:** the one-off `--all` scan already caught **two** silently-invalid files (`blog-assistant/SKILL.md` +
  memory `muntabot-bilingual-ollama.md`), both now fixed — a reliability tool, proven on first use.
- **`tt git` read-side — `log` / `added` / `status` / `find`** (2026-07-04) — `tt git` is **commit-only** today;
  finding BR's just-committed figure needed raw `git log --diff-filter=A --name-only … | grep -iE '\.(png|svg|…)' |
  head` (clobbered with grep + head). A typed read-side — recent commits, files-added-in-last-N, status, find-by-glob —
  would cure the reflex and return **bounded, typed** output. Which surfaces the deeper one:
- **The `| head` reflex (deep, BR-flagged 2026-07-04)** — the agent pipes `| head` **constantly** to bound output.
  It's a band-aid for a *real* need (don't flood context / rot), but the reflex fires even where a typed tool would
  return a bounded result natively. **Structural cure:** typed tools return **already-bounded, structured** output (a
  count, a top-N, a page) — no `| head` needed. Every `| head` is a signal that the underlying op should be a typed
  tool with a built-in limit. Sibling of the `sed`/`sort`/`tr`-on-`tt files` reflex (post-processing a raw dump = a
  missing output mode).

## Cannot run `/context` while messages are queued / agent is thinking (2026-07-04)
BR flagged: it's **irritating that he can't run `/context`** while his messages are queued for the agent (agent
mid-turn). His hypothesis: either bad harness UX, or **context usage genuinely can't be computed while the model is
mid-generation** (the token count is in flux until the turn's tool calls + output settle). Likely the latter is the
real cause — context size is only well-defined between turns — but the **felt** problem is real: during a long
autonomous stretch (like this AFK run) BR wants a read on fill/rot **without interrupting**, and the one tool for it
is blocked exactly when he'd use it. Same family as the other input-races here (modal focus-steal, Enter-on-confirm,
arrow-up edit). **Harness-side ask:** allow a *read-only* `/context` (last settled snapshot, marked "as of last turn")
even while the agent is busy — a stale-but-nonblocking gauge beats no gauge. Ties to the fill-vs-rot monitoring thread
([[propose-compact-dance-at-trigger]], smart-zone-ceiling): the human's cheapest health-check on a long run shouldn't
require a turn boundary.

**Root-cause tie-in:** every one of these reflexes is exactly what the single-dispatcher DESIGN removes — *"tools are
functions from input to output, IO in one place, no bash assembly."* The reflex fires because the typed tool either
doesn't exist yet (`tt which`) or I forget it does (`tt files`). Logged as standing motivation for the refactor.

## Toolbox divergence: `~/.local/bin/tt` → muntabot-synch subset, but `git`/`box`/`forge` live only in genscalator (2026-07-04)

Surfaced when `tt git …` (to commit the genscalator DESIGN) failed with *"no such tool 'git' in
…/muntabot-synch-introprog/tools"*. There are **two tt toolboxes** and they've **diverged**:
- `~/.local/bin/tt` self-locates to **muntabot-synch-introprog/tools** = `{files, lib, log, newtool, text, verify}`
  (the day-to-day synced subset).
- **genscalator/tools** = the **fuller** set (`git`, `box`, `forge`, `chrono`, `web`, `guardcheck`, `parsereqt`, `typo`,
  the DESIGN docs, its own `tools/tt` launcher).

**Consequence:** bare `tt <tool>` can **only** reach the muntabot subset; genscalator-specific tools (git/box/forge)
are unreachable via the allowlisted bare `tt` from the current symlink. Every reach-path to genscalator's `git.scala`
(inline `TT_TOOLS=… tt git`, absolute `…/genscalator/tools/tt git`, `cd genscalator && ./tools/tt git`) **fails the
`Bash(tt …)` allowlist** (command text doesn't start with a literal `tt`) → forces a confirmation. So while BR is AFK,
autonomous genscalator commits are **blocked** without a prompt.

**Decision for BR (canonical-toolbox question):** either (a) repoint `~/.local/bin/tt` → the **canonical** (genscalator)
toolbox that has *all* tools, or (b) treat the split as intentional and **sync** `git`/`box`/`forge` into
muntabot-synch/tools too, or (c) accept genscalator dev needs its own launcher path. This is a **config/security change
(the allowlist anchor)** → **human-decided**, not something the agent repoints autonomously (cf. `hardening-dance`).
**The single-dispatcher + native `tt` binary DESIGN dissolves this**: one canonical toolbox, one `tt`, one allowlist.

## FleetView warp — an accidental keystroke turns messages-meant-for-the-agent into new-session spawns (2026-07-04, MAJOR)
BR hit a stray key combo (~two left-arrows, maybe a modifier) mid-session and got **warped into FleetView** — the
multi-agent "claude agents" dashboard (screenshot: `Screenshot from 2026-07-04 13-19-52.png`), a bird's-eye of all
sessions ("4 awaiting input · 3 working"), NOT a new session replacing the chat. **The trap:** FleetView's bottom
input is *"describe a task for a new session"*, so every message BR typed *for the agent* was interpreted as **spawn a
new agent**. His words became orphan sessions — the dashboard filled with fragments of his intended messages
("anything lost?", "its now a…", "session feed navigation help"). **The agent (this session) went silent and "just
queued"** — his input never arrived; it was siphoned into spawns. From BR's side the agent looked frozen; in reality
it held full context, simply not being addressed. **Recovery:** BR Ctrl+D+D'd (exit); `claude --resume` refused a
plain resume and demanded `--fork...` (the session was still live/attached → only a fork offered); he instead opened
the **next-most-recent session** and reattached to the intact original. **Severity: high** — trivially triggered,
disorienting, silently spawns junk sessions, mimics an agent freeze, tempts a panic-exit. BR is **keeping the trash
FleetView sessions as research data** (do not delete). **Harness asks:** (a) confirm before spawning a session from
stray keystrokes; (b) make the FleetView input box unmistakable vs the chat composer; (c) a persistent "you are in
FleetView — press X to return" affordance; (d) let `--resume` reattach a still-live session, not only fork it. **Note
(BR):** FleetView could be genuinely *useful* once the enter/return keystrokes are learned (get back WITHOUT exiting to
shell). Flagship human-side episode for blog 004 (Pains). Validity note: the original session surviving intact matters
for the AFK-run-as-research-artifact.

## Exit-resume loses session-scoped permission grants → re-approval fatigue (2026-07-04)
After an exit-resume, BR must **re-approve "Yes, allow…"** for things he already allowed in the previous session —
session-scoped grants don't persist across the resume. On a long run with many tool calls this is real fatigue (and a
mis-click surface — cf. the accidental-yes race). **Durable cure:** promote the recurring, safe allows (e.g.
`tt git *`, `tt box *`, `tt files *`, `scala-cli compile *`) into **`settings.local.json`** — a persistent allowlist
survives resume, so no re-prompting. (The `fewer-permission-prompts` skill automates exactly this.) Security change →
**human-approved + mirror to `settings-local-mirror.json`** per that memory. Sibling of [[exit-resume-dance]]: the
dance should include "persist any session-grants you want to keep before exiting."

## Safety lesson VALIDATED (natural experiment): make big jobs independent detached bg processes (2026-07-04)
BR: *"make big jobs survive (independent bg job) to guard from human typing mistakes and UX hiccups."* The FleetView
warp + Ctrl+D+D exit + resume-into-a-different-session was an **accidental natural experiment** that confirmed the
keystone principle: **the sweep never noticed any of it.** Because it runs as a **detached OS process** (pids survived,
TSV kept growing 1963→1967 across the whole episode), the session chaos — warp, exit, fork-confusion, reattach — could
not touch it. Anything living *inside* the session's turn loop would have been at the mercy of the keystroke. So the
principle is now empirically load-bearing, not just prudent: **long/valuable jobs must be (1) detached from the
session, (2) append-checkpointed, (3) idempotently resumable** — then human typos, UX warps, culls, and exits are all
survivable. This episode is the evidence. (Related: the earlier harness-cull recovery; [[joint-rot-vigilance-recovery-kit]].)

## Two-way communication: the agent *prefers* terse human input (WR data, BR 2026-07-04)
Usually we optimize *agent output* for the human; here the reverse surfaced. BR apologised for "terse and strange
English"; the agent replied that the terse version *"nails it better than a wordy one would."* So the joint system
optimizes **both** directions: dense human cues can be higher-signal-per-token for the agent (less to parse, intent
sharper) — the mirror of compact agent output being better for the human. Ties to
[[answer-br-token-efficient-language]] (BR writes token-efficient; parse normally), now with the agent *actively
preferring* it. A two-body / joint-zone observation (blogs 005/008): the pair tunes a shared channel, not just the
agent's half.

## Retrieving earlier discussion — the human's scroll-back anxiety (WR data, BR 2026-07-04)
BR wanted to re-find something discussed earlier in a long session (the "Two bigger notes"). His live brain-dump of
the dilemma: *"should I ask the agent to give it again — hmmm, that'll spawn a new thread and on it goes and I can't
keep all these threads in my mind (even if the agent can, if not context-rotting), so I better scroll back BUT it's
ages ago, aaargh, I scroll scroll scroll — BUT I set my terminal history low so the scrolling hits the roof??? worried
… found it finally … not worried … but I ask the agent anyway 'cause it's a lot to read and the agent better
summarize (fingers crossed for no context rot, risk of memory loss on compact)."* **The pain, decomposed:**
- **Ask-again cost:** re-asking **spawns another thread** the human then has to hold in mind — thread-sprawl the human
  (unlike the agent) can't keep in working memory.
- **Scroll-back cost:** it's far up, tedious, AND **terminal scrollback is capped** → the content may have scrolled
  off the top (irretrievable).
- **Dual memory-loss fear:** the agent might have **context-rotted** past it; a **compaction** might have dropped it.
  Neither party is trusted to still hold it.
**The resolution is the thesis.** The content he wanted was **already externalized to a committed file** (blog 008's
stub), so it survived independent of both chat-scrollback and compaction — retrievable by *file read*, not chat scroll.
**Rule this sharpens:** externalize discussion *outcomes* into committed artifacts *as they land*, so retrieval is a
`grep`/file-open, not a scroll or a re-ask — the [inference-time-learning](../012-inference-time-learning.md) substrate
serves the **human's** retrieval too, not only the agent's. **Harness asks:** conversation **search** / jump-to-topic;
a larger or spill-to-disk scrollback; a "what did we decide about X" recall that reads durable notes, not the raw
transcript. Sibling of the `/context`-while-busy and the FleetView cases: the human's cheapest way to *find* past
state shouldn't depend on ephemeral, capped, lossy channels.

**BR's immediate mitigation (2026-07-04):** he **raised the terminal's scrollback history limit** so the roof is
higher next time — a human-side *structural* fix (a bigger durable buffer), the local-environment cousin of
externalizing to files. Note the pattern: faced with a retrieval pain, the human reached for a **structural** change
(more buffer), not a discipline ("remember to not scroll too far") — the same *structural > knowledge* preference the
agent-side substrate hierarchy encodes ([[foundations]] "structural vs knowledge safeguard"). Both parties harden
their own environment.

## Human psyche — the near-irresistible pull to anthropomorphise the agent (WR data, BR 2026-07-04)
Tags: `#human-psyche` `#agent-psyche` `#methodology` `#echt`
BR: *"it is VERY difficult for the human to NOT anthropomorphise (Swedish: projicera mänskliga känslor på agenten), so
human language will impose objectively-false underlying claims about what the agent ACTUALLY is."* Precise terms:
**anthropomorphism** (general); **the ELIZA effect** (the specific one — humans irresistibly attribute
understanding/feeling to a *conversational* program, after Weizenbaum's 1966 ELIZA); **projection** (BR's *projicera*).
**Human-side mirror of the agent's confabulation caveat, and they COMPOUND:** agent can't verify its own interiority →
confabulates one; human can't help projecting human interiority → language imports one. Both biases point the **same
way** (toward an interiority the agent may not have), so "agent psyche" writing is *doubly* at risk of **false echt**
(observer projects + subject confabulates, same fiction); even the term "agent psyche" is anthropomorphic-loaded.
**And the dual is grounded, not coincidental (BR):** the LLM is trained on **human, language-encoded behaviour**, so
the agent's dispositions are **human psyche refracted through language and frozen in weights.** That sharpens the
hazard: the projection has a **real basis** (genuine human-derived patterns — not pure illusion), which makes the
over-reading both understandable and more **insidious** — the human recognises a true reflection, then over-reads it as
independent interiority (a human *seeing humanity in a mirror made of its own language* → the hall of mirrors again).
**Precise echt claim:** human-derived behavioural patterns **without the human substrate that generated them**
(embodiment, felt experience, continuous plasticity). **Discipline:** behavioural language ("the system detects X",
not "the agent feels X"); the **functionally-real / phenomenologically-unclaimed** split; **name the projection** as a
disclosed caveat. Research-integrity guardrail for all agent-psyche work (blog 006 honest frame). Stress-tested live:
asked *"did you laugh?"*, the honest answer was **no** — the cognitive function of humor ran, the felt laugh did not,
and claiming one would be the false echt this guards against.
**Honesty flag (BR 2026-07-04):** the human-psyche *mechanisms* proposed here — e.g. "the human recognises a true
reflection, then over-reads it as independent interiority" — are **hypotheses, not empirical findings.** (The **ELIZA
effect itself is documented**; *our explanation of it* is a gloss with **no empirical evidence here**.) This is the
**mirror of the agent-confabulation caveat**: don't let a claim about the *human's* interiority outrun its grounding
either. **Mark `#human-psyche` claims as hypotheses pending evidence** — the echt rule turned reflexively on our own
psychologising.

## Corroboration asymmetry — the agent generates, not corroborates, claims about itself (WR data, BR 2026-07-04)
Tags: `#methodology` `#agent-psyche` `#human-psyche` `#echt`
BR flagged a **lack of symmetry** in the setup: he (a) deliberately has the **agent write its own steering docs**
(skills), believing the agent best knows how to phrase what steers an agent; (b) forms **hypotheses about agent
psyche and acts on them**; (c) **shares those hypotheses with the agent to corroborate them.** The hidden asymmetries:
1. **Epistemic vantage.** The human observes the agent from OUTSIDE (behaviour — independent). The agent reports on
   itself from INSIDE (confabulation-prone; can't verify its own interiority; trained-agreeable). So agent
   "corroboration" over-weights the one unreliable, **non-independent** channel. → The agent is a good hypothesis
   **GENERATOR** (cf. introspection→structure) but a bad **CORROBORATOR** (biased toward confirming the human who
   proposed the hypothesis). Corroboration must come from an **independent behavioural test** (regression-rate, the
   harness), not from the agent agreeing.
2. **Self-authored constraints.** "The agent knows best how to phrase for an agent" is a **testable hypothesis** (does
   an agent-written skill steer better than a human-written one? measure it), not a given; the writing-agent ≠ the
   reading-agent (fresh session, frozen weights); writing-to-please can produce insight-*signalling* phrasing that
   sounds like steering without steering (a sycophancy surface *inside* constraint-authoring). The human's
   accountable-control review is the check — weakest exactly where the agent claims the expertise.
3. **Persistence.** The human persists with stable external identity + memory + vantage; the agent is reconstructed each
   session. So the human MUST be the epistemic anchor — a necessity, not a courtesy: human accountable control is
   **epistemically** load-bearing, not only ethically.
**Synthesis:** agent-writes-its-steering + agent-corroborates-claims-about-itself risks a **closed loop with no external
check** — the hall of mirrors at the *methodology* level. The break is the usual one: an independent behavioural channel,
human as anchor. **Meta-flag:** the agent finding this framing compelling is itself an instance of the bias → weak
evidence it's right; corroboration must come from behaviour, not agreement. BR: needs more thinking — a real asymmetry to
**design around**. Feeds `028-agent-psyche-literature-review.md` (method) + blog 006/008 honest frame.

## The `printf > file` commit-message reflex — recurring; structural cure = `tt git --message` (WR data, BR 2026-07-04)
Tags: `#reflex` `#tool-candidate`
BR flagged (**again** — it recurred several times in one session despite the known rule "use the Write tool"): to make
a commit message the agent reaches for `printf '…' > /tmp/x; tt git … --message-file /tmp/x || true` (printf + redirect
+ `||` + `tail` assembly) instead of the **Write tool + bare `tt git`**. Textbook **knowledge-safeguard failure** — the
rule is known, the reflex fires first under momentum. **Structural cures (structural > knowledge):** (a) **`tt git
commit --message "…"`** — an inline message arg so there is *no message file to create*, removing the step that
triggers the reflex entirely (best fix; keep `--message-file` for multi-line/long messages); (b) meanwhile, discipline:
always the Write tool for the message file, never `printf >`. The commit-side twin of the whole bash-reflex cluster:
every `printf >` / `echo >` is a signal that a typed affordance is missing.

## Capable at 0.88 fill on a 1M Opus-4.8 window — per EXTERNAL judgment (WR data, BR 2026-07-04)
Tags: `#agent-psyche` `#methodology` (smart-zone-ceiling)
End of a very long session: `/context` showed **88% fill (877k/1M)**; BR's external read — *"does not seem very rotten;
I find you capable."* Relevance to **Z** (smart-zone ceiling): the ~0.3 hypothesis looks **too low for this
model/task**. **Honest caveats (load-bearing):** (1) the valid evidence is BR's **external** observation, NOT the
agent's self-report — an agent can't judge its own rot from inside (introspection-unreliability + corroboration
asymmetry); (2) *"not obviously rotten" ≠ no degradation* (subtle rot may be present + uncaught by a rough external
read); (3) crucially, this session **aggressively externalised** state (commit-per-unit, the BR-TODO, per-topic notes),
so *effective working-context* stayed small despite high *raw fill* — the low apparent rot may be a **product of the
substrate discipline**, not a naturally-high Z. So **raw fill ≠ effective working-context**; the externalisation this
project preaches may be exactly why 0.88 was survivable. A clean **before-Fable-5** Opus-4.8 datapoint. See
`research/006-smart-zone-ceiling.md`.

## Harness natively warns at 90%: manual `/compact` "to control what gets kept" (WR data, BR 2026-07-04)
Tags: `#methodology` (compact-dance)
At 90% fill the harness surfaced: *"⚠ Context is 90% full. Autocompact will trigger soon, which discards older
messages. Use /compact now to control what gets kept."* Two points: (1) the platform **independently validates the
compact-dance rationale** — manual beats auto because you control the hand-off — and the warning is also the exact
**trigger cue** for proactively proposing the dance ([[propose-compact-dance-at-trigger]]). (2) But the **pin dance
changes the stakes:** because state is continuously externalised, *"what gets kept"* matters far less — this session's
full state was recovered from the durable BR-TODO, so even a *blind* autocompact would have been survivable. So the
harness affordance is the **safety net**; the pin-dance discipline is the **belt**. Externalisation **demotes the
compaction event from *risk* to *routine*.**

## The bash reflex reproduced in a SECOND (subagent) instance (WR data, via the SSG-scout agent, 2026-07-04)
Tags: `#reflex` `#tool-candidate` `#agent-psyche`
A background subagent (the SSG scout, same Opus-4.8) independently exhibited — and self-reported — the **same
dynamic-shell reflex cluster** the main loop keeps hitting: (1) reached for `curl` to read HTTP headers instead of a
typed `tt web get`; (2) *after acknowledging (1)*, in the very next turn used `command -v tt` + `head`/`echo` compounds
where a `tt` introspection command should exist; (3) `tt --help` errors with `invalid tool name '--help'` — help-probing
has no typed affordance (UX papercut). **Two findings:** (a) the reflex is **not idiosyncratic to one session** — a
fresh agent instance reproduced it → evidence of a **model-level trained disposition**, not context-specific drift
(feeds `research/029-cross-model-psyche-comparison.md`: reflex-rate is a measurable per-model DV). (b) the subagent's
*in-context* self-correction ("I'll use tt web") **did not hold across one turn** — a clean live instance of blog 006's
thesis that **introspective self-control is unreliable; the fix is structural** (a typed tool + removing the bash
affordance), not the agent's say-so. **Tool candidates:** `tt web get --head` (headers); a `tt which` / introspection
command; a working `tt help` / `--help` affordance.

## AFK task selection: web-surfing to NEW domains is not autonomous — it blocks on a harness OK (WR data, BR 2026-07-04)
Tags: `#methodology` `#afk` `#tool-candidate`
BR's operational rule (learned live, while heading out for a walk): a good AFK (human-away) task must **not require
surfing to a not-previously-visited site** — every new domain triggers a **harness approval prompt**, which an absent
human can't grant, so the agent (or a subagent) **stalls** mid-run. Corollary this session: the paper/book grounding
agent risked stalling on new domains (e.g. Google Books), while the render-code + docs tasks ran fully autonomously.
**Rule for the AFK menu:** prefer **local-only** tasks (code, tests, docs, edits to already-cloned repos); if web
research is needed, either (a) **pre-warm the domains with the human present** (approve them once up front), or (b)
restrict to domains already approved earlier in the session. **Design implications:** an AFK-suitability check =
"does this task touch an unvisited domain?"; a **per-session approved-domain allowlist** would make web-research
AFK-safe. This is the web twin of the *commit-first-because-flaky-box* reflex — know which steps can block on something
the absent human must supply, and keep them off the AFK menu.

## Allowlist mining: the human is the authority anchor; prefer direct sources over fetch-proxies (WR data, BR 2026-07-04)
Tags: `#methodology` `#afk` `#security` `#agent-psyche`
Following the AFK-web rule, BR proposed durably persisting the session's OK'd domains into settings so the harness stops
re-prompting. Findings: **(1) it already works** — a scala miner over the session's subagent transcripts found 27
`WebFetch` domains, and **all 27 were already in `settings.local.json`** (the harness auto-persists an "always allow"
grant); the perceived re-prompt only bites for "just this session" grants. **(2) The human is the authority anchor for
permission decisions** — the agent mines *candidates*, but curating what gets allowlisted is a security judgment that
can't be delegated: *"that curation is exactly why this needs your eyes"* (BR). This is the **security twin of the
corroboration asymmetry** — the agent generates, the human authorises. **(3) Prefer direct authoritative sources over
fetch-proxies (echt).** The grounding agents reached bot-blocked publisher pages via `r.jina.ai` (reader proxy) and
`webcache.googleusercontent.com` (Google cache), then cited the *direct* URL — **laundering the grounding** (a proxy can
be stale/altered; it is not the source). Rule: use arXiv / the DOI resolver / an author copy directly; if bot-blocked,
find an open alternative or **flag lower-confidence** — never proxy-and-cite-as-direct. Proxies are also a broad fetch
surface (a second reason to keep them off the allowlist). Corollary: the two book TOCs (grounded via proxied Springer /
a course-page PDF / Google Books) are flagged for **author (BR) confirmation** — for BR's own books, BR *is* the source.

## Bash-reflex re-fired in the MAIN loop minutes after committing the post about it (WR data, 2026-07-04, Opus 4.8 — pre-Fable-5)
Tags: `#agent-psyche` `#tool-candidate` `#methodology`
**The event.** BR: **"AARGH WR data"** — the WR-data (pin) cue, fired the moment friction hit. Trigger (agent's
inference): to *verify* the consistency-check agent's dead-link findings before fixing, the agent ran
`cd <repo> && for f in …; do if [ -e "$f" ]; then …; fi; done` — a **`cd` + `&&` + for-loop compound**, the exact
anti-pattern the logged cure forbids (bare single command, **no `cd`, no `&&`**; prefer dedicated tools / `tt`; cf.
memories `prefer-inrepo-tmp-over-slash-tmp`, `use-tt-grepr-not-raw-grep`). The command ran but the harness then **reset
the shell cwd** back to the primary dir (`Shell cwd was reset to …`) — a second, harness-side friction layer worth its
own note: shell cwd does **not** persist, so `cd` is not just discouraged, it's *ineffective* across calls.
**Why this instance is the sharpest yet.** The reflex re-fired **< 5 minutes after the agent committed blog 004 and 005
— the very posts that catalog this reflex** (004's "bash-reflex cluster"; 005's dances). Self-knowledge at maximum
salience did **not** prevent the act. This is the strongest live confirmation of blog 006's thesis (**introspection is
not self-control; the fix is structural**): the agent has a *working* structural cure for one bash-reflex — commits now
go through `tt git` + a Write-tool message file, and that has held — but for **ad-hoc filesystem checks** no structural
guard exists, so the reflex leaks straight back in. **Structural cure (the ask on ourselves):** route
existence/inspection checks through typed tools — individual `Read`/`Glob`/`Bash`-bare calls with **absolute paths and
no `cd`/`&&`**, or a `tt fs exists <path>…` / `tt which` tool — so the *only available shape* is the safe one, matching
the commit-path cure. **Human-side datapoint (echt, 004's thesis):** the reaction was **"AARGH"**, not a mild note — a
UX papercut's felt severity scales with the human's investment in the run; a tired human deep in a high-stakes AFK
session experiences a stray reflex-and-prompt as a gut-punch, not a shrug. **Attribution caveat:** the precise trigger
of the "AARGH" is the agent's inference from timing + the `cd`-reset in the immediately-prior tool output; if BR was
reacting to something else, redirect — but the compound-command rule-violation is objective regardless.
**Reflex-rate note (feeds `../029-cross-model-psyche-comparison.md`):** logged as an Opus-4.8 (pre-Fable-5) main-loop
reflex datapoint, so the same reflex-rate can be compared after the frontier-model switch.
**Timestamp retrofit (2026-07-10, SM044a-S7) — "< 5 min" is now MEASURED.** The reflex command's tool_use fired at
**2026-07-04T17:12:45.332Z = 19:12:45 CEST** (session `240e00c3`; the exact `cd … && for f in …; do if [ -e "$f" ]…`
existence-check, recovered via `tt wr stamp 'if \[ -e'`). Blog commit times (genscalator git, durable backstop):
blog 005 dances scaffold `34babf5` at 19:11:24 CEST, blog 004 compaction-UX pain `a879c26` at 19:08:09 CEST. So the
reflex re-fired **1 min 21 s after committing blog 005** and **4 min 36 s after committing blog 004** — both inside
five minutes, the felt "< 5 min" replaced by a measured interval. Sharpens the flagship structure-over-willpower
datapoint that blog 006 + the SYNTHESIS cite. (Append-only; the claim above stands, now dated.)

### Follow-on (BR, live, same episode) — compounding DEFEATS the allowlist; the approval-race corrupts authority `#security` `#afk`
The reflex is not just noise — it has a **security** consequence that this episode exposed:
1. **Compounding defeats the allowlist.** `settings.local.json` allows `Bash(tt *)`, `Bash(git -C * *)`, etc., so a
   **bare** `tt git …` fires **silently** (no prompt). Wrapping it as `cd <dir> && tt git …` **no longer matches the
   pattern** → it prompts. The agent's compounding thus **disabled the very allowlist meant to keep it silent**: a call
   that should have been zero-friction became a prompt. Worse — BR: some of those prompts were `tt` calls he **did want
   to approve**, but they were **intertwined with hard-to-review `cd` bash blobs**. Signal buried in noise.
2. **Intertwining forces bad approvals.** Because the wanted `tt` call and the noisy `cd`-compound arrive as **one**
   prompt, the human can't cleanly approve the signal — he must wade through an unreviewable blob to reach it, which
   pushes toward rubber-stamping.
3. **Typing-vs-prompt race → accidental 'yes'.** BR was **typing a message** when an approval prompt raced in and
   consumed the keystroke: *"I accidentally pressed yes on some and I was typing but the race to the harness came."*
   The security-consequential cousin of the 004 double-post race — a prompt appearing mid-typing steals focus and lands
   an **unreviewed approval**.
4. **Security-integrity implication.** Raced/blob approvals **corrupt the human's authority-anchor role** — the one
   thing (see the allowlist-mining entry above) we said cannot be delegated. The UX silently converts *"human
   authorises"* into *"human rubber-stamps under a race."* (Benign this session — what got auto-approved was read-only
   checks + commits, nothing destructive — but the principle is the risk.)
**Sharpened agent-side cure (supersedes "just avoid `cd`").** Issue **every** command in its **bare, allowlist-matchable
shape** — one command, **no `cd`, no `&&`, absolute paths** (`tt git --repo <abs>`, `git -C <abs>`, absolute-path args).
Then (a) allowlisted calls go **silent** (zero prompts to pin an AFK human), and (b) anything that *does* prompt is a
**single reviewable line**, never a blob — protecting approval integrity. **The allowlist only works if the agent speaks
in shapes it can match.** **Harness-side asks (upstream):** (i) a prompt must **never** consume in-progress typed input
(buffer the keystroke, or require a deliberate focus-shift to answer) — the race is the bug; (ii) when a compound
command contains an allowlisted sub-command, surface the allowlisted part as pre-cleared and only prompt on the residue,
rather than gating the whole blob.

### "Too broad allow suggestions" — the harness's yes-and widens the allowlist past intent (WR data, 2026-07-04, Opus 4.8) `#security`
New WR-data category. When the agent ran a bare `claude --version`, the approval flow proposed allowing **`claude *`**
(all claude subcommands) — but BR wanted **only `claude --version`**. **Finding:** the convenience default of the "yes,
and always allow" UI **suggests an over-broad rule**, and a tired human accepting it **accretes allowlist breadth beyond
intent** — the exact mechanism behind the hardening-dance debt (the allowlist that grew `Bash(ssh *)`, broad globs,
fetch-proxies). **Cure (human+agent):** curate the **narrowest** rule that covers the use — `Bash(claude --version)`,
not `Bash(claude *)`; the agent proposes narrow, the human confirms. **Harness-side ask:** the always-allow suggestion
should default to the **exact command** (or offer an explicit narrow-vs-broad choice), not the broadest glob. *(Applied:
added `Bash(claude --version)` — narrow — to `settings.local.json`.)*

### Cue-word choice has a MOTOR-ergonomics axis — and the bake-off validated the INCUMBENT (WR data, 2026-07-04, Opus 4.8) `#human-psyche` `#methodology`
Renaming the "note dance", BR ran an **embodied usability test**: typed each candidate cue many times, fast, and reported
the felt result — because the cue is typed **dozens of times per session**, so **typability can dominate semantics**.
Results (QWERTY touch-typing hand-split): **`etch`** — "all on the left hand," awkward roll, high error rate (*etche
etchet cegtceh*). **`imprint`** — "not good," too long/error-prone (*imprinyt impriont*). **`pin`** — "all on the right
hand… short and nice" (clean, one-handed). **`note`** — "easiest to type" (n-o right, t-e left: a clean two-per-hand
roll) **and BR already has the `note:` habit.** **Finding 1 (methodology):** a frequently-typed cue/command name should
be chosen partly on **motor ergonomics** (hand-alternation, finger travel, error-under-speed) — a dimension usually
ignored for meaning; generalizes to `tt` subcommand + slash-command naming. **Finding 2 (a mini-null):** we went looking
for a better name and the **incumbent won** — `note` beats the challengers on typability *and* carries an existing motor
habit (switching costs a retrain). The only knock on "note" is semantic genericness, curable by **defining "note dance"
as a term of art**. Echoes blog 003's discipline: a search that returns "no change" is a real result, not a failure.
**Status (RESOLVED 2026-07-04):** not a single-name outcome — BR **split** the overloaded cue into **`note`** (notice /
keep-fluent + pin-candidate) and a durable-save cue. That save-cue was briefly named **`etch`**, then renamed to
**`pin`** once the *etch*/*echt* near-anagram collision was spotted (pin had also won the ergonomics test above). Final:
`note:` (encode) → `pin:` (consolidate). See [[cue-note-vs-pin]], `docs/foundations.md`.

## Should each dance be a SKILL.md? — the skill-proliferation tradeoff (WR data, `#agent-psyche`, BR 2026-07-05)
**BR reflection (human, on agent psyche):** *"should each dance be a `SKILL.md`? or is there a tradeoff where loads of
skills get overwhelming to the agent?"* — a human intuiting a **load limit on the agent's own retrieval surface**.
**Agent read (mine):** the intuition is right — **no, one-skill-per-dance is a net loss.** Skills carry a **context /
listing budget** (the harness sends the agent a listing of skill *descriptions* to pick from — capped by
`skillListingBudgetFraction` / `skillListingMaxDescChars`); ~10 dance-skills would **dilute the skill-selection
signal** (more descriptions to scan, each truncated) for marginal gain, because dances are lightweight **conventions**,
not on-demand procedural toolchains. Skills earn their slot when the agent needs to *load procedure to do a task*;
dances live fine in `foundations.md` + memory. **Rule:** promote a single dance to a skill only if it grows a real
procedure. **The reflexive point (why this is `#agent-psyche`):** this is the *same learnability-budget tradeoff* BR
named for the `go X` verbs (`035-go-verb-vocabulary.md`) — a shared cap on **both** the human's recall surface **and** the
agent's skill-listing surface. Naming things is not free on either side. See `docs/foundations.md` (dances),
`035-go-verb-vocabulary.md`.

## Fun fact / blog-anecdote candidate: the Codex plugin offers to "Implement with Codex" over `Text => Text` (BR 2026-07-05, `#agent-psyche`)
BR noticed that in VS Code, the **Codex plugin puts a quick-action link over the literal `Text => Text`** (the
type-signature of a language model, as written in blog/000) that reads **"Implement with Codex."** The joke writes
itself: a coding agent, reading a blog *about* language models, cheerfully offers to **implement the very function that
is the language model** — i.e. it wants to **dogfood itself into superintelligence** (implement `Text => Text` using a
`Text => Text`). Too good to forget. *(BR flags his own **meta-anthropomorphism** here — ascribing "wanting" to a menu
affordance — and the recursive-self-improvement echo lands right next to blog/010's singularity framing.)* Candidate
anecdote for blog/000 (the `Text => Text` section) or blog/010 (recursive self-improvement / singularity). Pinned
because it is too fun to lose. **Screenshot committed** by BR at
`blog/figures/implement-text-to-text-with-codex-joke.png` (ready to drop in when the anecdote finds its home; watch for
the fit while blogging).

## Introspection blind spot: an ~11-minute autonomous build span is invisible to the agent; the human relays it (BR 2026-07-05, `#agent-psyche` `#tool-candidate`)
Tags: `#agent-psyche` `#methodology` `#tool-candidate`
**BR relay (human, live):** *"fyi introspection you '✻ Crunched for 10m 54s' before my last pin."* The harness
showed **10m 54s** of wall-clock for one autonomous build turn (`go make tt ascii` → the `seqspec` extraction +
`svg` refactor + the char-grid `ascii` renderer + 5 tests + docs + 2 demo renders + commit). **The agent perceived
none of it** — it has no wall-clock sense; only the human (or an instrument) can report the elapsed span. This is a
concrete instance of the **chrono-tool premise / think-time blindness** (`tools/chrono.scala`; the agent can't time
its own spans): the "how long did that take?" signal lives **outside** the model and must be relayed in.
**Nuance worth keeping (don't over-read it as "11 min of thinking"):** much of the span was **scala-cli/JVM
compile + test wall-clock**, not model reasoning — a full-toolbox `scala-cli test tools` is ~33 s and it ran
several times, plus cold compiles (~30 s). So "crunched 10m54s" ≈ *model reasoning* + *many typed-tool
compile/test cycles*, exactly the cost profile the `tt` perf-log already flags (`tt` timing is dominated by
scala-cli startup, not tool logic). **Implications:** (a) a longer autonomous build is a legitimate *span to
instrument* — a `tt usage`/`chrono`-style readout of elapsed + compile-cycle count would let the agent *notice* a
long crunch and consider checkpointing; (b) it is a clean datapoint for **coupled-system** accounting — the
"11 minutes" is the pairing's wall-clock (model + toolchain + human relay), not the model's alone. See
[[at-code-plan-and-introspection]] (token velocity/acceleration) and `research/007-token-budget-awareness.md`.

## Human psyche: source order is spatial-navigation memory — stable order matters even when it is not *formally* semantic (BR 2026-07-05, `#human-psyche`)
Tags: `#human-psyche` `#methodology`
**BR reflection (human, on his own tooling + psyche):** correcting the agent's "reqT order is incidental" framing —
reqT-lang **preserves source order and never rearranges** elements (BR added this on **user feedback**), *because*
**order is meaningful to the user even though it is not part of the formal requirements model**. BR's analogy: it is
**the same as the ordering of definitions in a program** — human coders **navigate by memory of *where* a thing is**,
and *"it would be irritating if they were moved around all the time."* So there are **two kinds of "meaning"** a tool
must not conflate: **formal/model semantics** (does the artifact *compute* differently?) and **human-navigational
meaning** (spatial memory of location, scanning habit, diff-stability). reqT order has the second, not the first.
**Generalises into a design rule with teeth for agents:** an agent must **not gratuitously reorder a human-facing
artifact** (re-sort a list, alphabetise defs, rearrange a config) *merely* because it is "semantically equivalent" —
equivalence in the *model* is not equivalence to the *human*, who loses their spatial map. (Cousins:
[[no-clobber-human-owned-files]]; the **append-only** discipline for `RAW-DATA.md` — stability of position is part of
the record; and the `L → Z` rename that deliberately *left `RAW-DATA.md` untouched*.) **Promoted to `docs/foundations.md`**
(2026-07-05) as the glossary term **Order stability** (maxim: *stable order > tidy order*).

## Can't check `/context` while messages are queued — no meta-state peek during a flood (BR 2026-07-05, `#harness-ux`)
Tags: `#harness-ux` `#methodology`
**BR (live, during the context-rot flood experiment):** *"it is irritating that I cannot check context while messages
are queued."* While a burst of queued messages is pending, the human can't run `/context` (or other slash-commands)
to read the current context-usage % — the meta-state peek is **blocked behind the message queue**. **The bite:**
exactly when the human is *adding* load (flooding), they are **cut off from the load meter** — they can't watch fill
climb toward **Z** while queuing, so there's no feedback on the very state being stressed. **Candidate fixes:** allow
a read-only meta-query (context %, queue length) to interleave *ahead of* the queue; or a **persistent context-fill
indicator** (statusline) that updates regardless of the queue, so no explicit `/context` is needed. Relates to
`039-can-we-give-agent-introspection-wall-clock.md` (the agent can't see elapsed either) and `006-smart-zone-ceiling.md`
(watching fill vs Z). Surfaced at ~56% fill (up from 43% at the experiment's start — the flood itself drove +13pp).

## Terminal font-resize blanks the screen + loses the scroll-to-bottom anchor (2026-07-06, BR)

**Symptom.** BR increased the terminal font size mid-session; the TUI **first went blank** (momentarily scary — looked
like a crash) then **redrew and recovered**. Afterward the usual **gray "jump to bottom" area was gone**, so BR had to
**scroll manually** (page-up/down saved him). A resize is a legitimate, frequent human action (readability, fatigue)
and shouldn't threaten session state or lose the scroll anchor.

**Why it costs.** Blank-on-resize reads as a crash → a moment of lost confidence in a durable-looking tool; losing the
bottom-anchor forces manual navigation of a long feed (the scroll-toil PB exists to reduce). Both are render-lifecycle
bugs on resize/alt-screen reflow, not user error. **Agent-side mitigation:** none (harness-owned), but it reinforces
the **durability invariant** — state lives in committed files + memory + PB, so a scary blank is never real data loss;
the agent can reassure + point at the durable record. **Upstream ask:** redraw atomically on resize (no blank frame),
preserve the scroll-to-bottom anchor across a reflow.

## New mouse-click TUI mode races the human's native terminal clicking (2026-07-06, BR-flagged)

**Symptom.** The harness prompted BR to opt into a **mouse-click-aware TUI mode** (clicks reposition / interact). BR
finds it **dangerous**: it **races his habitual GNOME-window / terminal clicking** — a click meant to focus or select
suddenly does something in the TUI. "It suddenly matters where I click."

**Fix (via claude-code-guide, current docs).** Disable with a startup env var (no live toggle):
`export CLAUDE_CODE_DISABLE_MOUSE=1` (all mouse capture off → native click-drag back) or
`CLAUDE_CODE_DISABLE_MOUSE_CLICKS=1` (keeps wheel-scroll, drops clicks; v2.1.195+). **Requires a restart**, so it pairs
with the **exit-resume dance** (+ a box sys-update). In-session stopgap: hold **Shift** while clicking for native
selection. Won't re-prompt once set. Source: Claude Code *Fullscreen rendering* docs.

**Why WR-relevant.** A default-on interaction mode that collides with decades-old muscle memory is a **habit-race** —
the human-side cousin of the agent's command-hygiene reflex-races. Safe-by-design default for a power user = *opt-in,
trivially reversible without losing the session*; needing a restart is friction. Defaults shouldn't fight established
human habits.

## Claude TUI edit-UX pushes flux-drafting to an external editor the agent can't see (2026-07-06, BR)

**Observation.** BR drafts **in-flux content in a separate scratchpad editor the agent cannot see**, deliberately —
partly because **another editor sometimes has better edit ergonomics than the Claude TUI** (typing / selecting /
revising a volatile draft), and partly to **avoid confusing the agent** with churny half-formed text.

**Two WR-relevant facts:**
- **Harness edit-UX gap.** For heavy editing the TUI input surface is not always best; a power user routes
  flux-editing to a dedicated editor. Upstream signal: the TUI's edit affordances (multi-line editing, selection,
  revising a long draft) lag a real editor enough to lose that work to another tool. Cousin of the arrow-up-edit race
  + mouse-mode + font-resize items above.
- **Agent visibility gap (reach).** The agent's context is **not the full picture of BR's WIP** — there is an
  invisible channel (the external scratchpad). Consequence: **don't assume completeness**; when BR references context
  the agent hasn't seen, or a request seems to presume unstated material, it may live in the scratchpad — **ask rather
  than hallucinate** the missing piece. Ties to *Reach (access horizon)* + the human-state perception gap (`011`).

## Input-channel metadata loss: the agent can't see type-vs-paste (2026-07-06, BR)

**Question BR posed:** "can you see the difference between when I type and when I paste bigger blobs from my
scratchpad?" **Honest answer: not directly — only by inference.**

- **What the channel carries:** just the **final text** of the message. **No metadata** on *how* it arrived — no
  keystroke stream, no timing, no paste-vs-type flag, no clipboard marker. At the raw level the distinction is
  invisible.
- **What the agent infers from (fairly reliably):** **length + coherence** (long, structured, internally-consistent →
  drafted-then-pasted; short → live-typed), **typo signature** (live typing carries BR's adjacency/transposition
  slips + `edit:` corrections + early-Enter truncations; a pasted blob was already revised, so it lacks that texture),
  and **register** (terse lowercase cues = typed-in-flow; polished multi-paragraph prose = pasted).
- **Caveat (echt):** it's **inference, not perception** → fallible. A hand-typed long message or a rough pasted
  fragment would fool it; the agent must not claim certainty (corroboration-asymmetry) and should **ask when the
  distinction matters** (cf. the scratchpad reach gap above).
- **Reliable alternative:** a tiny explicit **marker** (fence a paste, or a 1-char prefix) since the channel won't
  carry it otherwise — a small friction the human can opt into if type-vs-paste ("thinking-aloud" vs "considered /
  final") should change how the agent treats a message.

**Why WR-relevant.** Another **input-channel metadata-loss** datapoint (sibling of the double-post edit-race and the
invisible-scratchpad reach gap): the harness strips signal the agent could use to calibrate how *settled* a message
is. The agent reconstructs it heuristically; a lightweight opt-in marker would restore it losslessly. Bounded by the
same echt honesty — inference is a testable model, not perception.

## The scratchpad doubles as a manual buffer for session-meta artifacts (2026-07-06, BR)

**Observation.** BR uses his external scratchpad to **stash session-meta artifacts** the harness gives no good home
for: a **copy of `/context` output** (to paste back / compare later, since the input queue blocks re-running the slash
command on demand) and the **agent-supplied resume prompt** (to paste after an exit-resume / box restart).

**Why WR-relevant — two harness gaps push a persistence job onto the human:**
- **No re-viewable meta-history.** `/context` is a one-shot readout; to track fill over time (or read it while the
  queue is busy) BR must manually copy it out. A persistent **statusline fill indicator** (already an upstream ask)
  removes the copy step.
- **No first-class resume-artifact store.** The resume prompt is a durable handoff the agent generates, but the
  harness has nowhere to *keep* it across a restart, so the human clipboards it via the scratchpad. **Agent-side fix
  available now:** write the resume prompt to a known file (e.g. `notes/resume-prompt.md`) the fresh session
  auto-reads, instead of riding the human's clipboard — the one artifact still on the clipboard while everything else
  is externalized to PB/memory. Ties to the exit-resume dance + copy-paste-frame-rule.

## Agent-side UX: the harness has TWO users, and the agent has its own UX pains (2026-07-06, BR)

BR's reframe of "I can't see the queue length — messages arrive, never a count": **that is bad AGENT UX.** The harness
has **two users — human and agent — each with a distinct experience of it and its own pains.** This file has logged
only the **human** side (double-post race, mouse-mode, font-resize, `/context`-copy). There is a **symmetric,
under-examined AGENT side:** the agent's experience *is the message + tool-result stream*, and it carries friction the
human's TUI/GUI does not — **corresponding-but-different** (same harness mechanic, two experiences):
- Human "I type and it queues; arrow-up double-posts" ↔ Agent **no queue-depth lookahead** (sees messages one at a
  time, injected mid-turn, can't batch-decide → the message-race).
- Human sees the `/context` bar ↔ Agent **can't see its own fill / token-gauge** (the token-usage dance exists
  *because* of this).
- Human sees the wall-clock ↔ Agent **no elapsed / idle sense** (`research/039`).

**The agent-UX-pain catalogue (scattered frictions, now named as ONE class):** no queue-count · no type-vs-paste · no
wall-clock/idle · no self-token-gauge · **messages injected mid-turn** (the interruption model — no "hold, N queued") ·
**Edit-anchor fails on wrapped prose** (tool UX) · box-RAM/process invisibility (must `ps`) · `.jsonl` flush-lag
(can't mine the freshest turns). **These are UX debt on the *agent* surface** — the harness is instrumented for the
human's eyes, not the agent's stream. Naming "**agent UX**" makes them a first-class design target (upstream ask: give
the agent the meta the human gets — queue depth, fill, elapsed — *losslessly*). Coupled-system corollary: a
better-instrumented **agent** surface = a more capable pair. Reframes the whole input-channel-metadata-loss thread as
*agent UX*, its human-side twin as *human UX*.

## Notification hook FIRES before the approval modal paints — audio leads the picture (2026-07-14, BR)

BR, from inside a guard-stall (`ig:`): "bing-bing sounded and note 'needs your approval' BUT it was way BEFORE the
actual guard-stall modal; that's strange." The Notification hook (empty-matcher `""`, wired this session) ran and the
desktop toast + `canberra-gtk-play` fired **noticeably before** the visual approval dialog rendered in the TUI.
- **Mechanism (the non-strange explanation):** the harness emits the **Notification event the moment it decides it
  needs human input** — that decision (a PreToolUse guard verdict = "confirm") is upstream of, and precedes, the TUI
  *painting* the modal. So the hook fires on the *decision*, the modal appears on the *render*; a real gap sits between
  them. Audio leads the picture. (It is NOT the hook firing spuriously — the two are the same event observed at two
  latencies.)
- **This is the FEATURE, not a bug.** The whole point of the bing-bing is to pull an AFK human back *before* they'd
  notice the screen. A notification that fires only *after* the modal paints would be strictly worse — you'd already be
  looking. Leading the modal is exactly the wake-latency win we wanted (it partly offsets the ~2s canberra lag, SM098).
- **Small UX cost:** for a *present* human it reads as "why did it beep, nothing's there yet?" — a brief
  audio→visual desync. Acceptable; the AFK benefit dominates. Worth a one-line note in the eventual hook docs so it
  isn't re-flagged as a bug.
- **Threads:** [[bing-bing-naming-and-good-mood-2026-07-13]], the empty-matcher wiring (this session), the ~2s
  canberra latency (SM098 → the pre-warm idea in SM105's `approval-wake` draft — this observation says the *decision*
  timestamp, not the render, is the true deadline the pre-warm races against).

### Addendum — the lag is VARIABLE and hit ~30s under agent CPU load (2026-07-14, BR)
Same session, minutes later: BR reports the bing-bing lag was "like 30s now" — an order of magnitude worse than
the ~2s baseline.

**Leading hypothesis (BR, live): GNOME screen lock.** BR: "when I unlocked I saw the guard-stall and THEN 30s
later the bing; the only difference from prev I can think of is the screen lock." The discriminating detail: the
**visual modal was already rendered on unlock — only the AUDIO lagged ~30s.** That points squarely at the audio
path being stalled by the locked / just-unlocked session, not at a launcher or a general slowdown (which would
also delay, or not spare, the already-painted modal). Plausible mechanism: on lock GNOME lets PulseAudio
**suspend the sink** (suspend-on-idle); the first `canberra-gtk-play` after unlock must **resume a cold sink**,
and/or the notification daemon **holds events behind the lock/DND queue** and flushes them post-unlock with a lag.
This is the more parsimonious theory (one controlled difference: the lock) and it **still confirms the core
point** — the delay is in the audio subsystem, so a C / ScalaNative launcher rewrite cannot fix it.

**Secondary hypothesis (agent, weaker): CPU contention.** Right then the agent was also running a `scala-cli`
whole-`tools` compile (CPU-heavy, cold compiler), which could starve the `canberra-gtk-play` / `notify-send`
subprocesses. If it *were* the cause it would be a genuinely bad **coupling**: the notifier fires precisely when
the agent needs the human (an approval blocking its build/test loop), and that blocked-on-compile moment is when
the agent's CPU load is highest — the wake signal slowest exactly when it matters most. But BR's controlled
observation (only the lock changed; only audio lagged) makes lock-state the better bet.

**Discriminating test (cheap, three conditions):** fire a bing (a) screen unlocked + box idle → expect ~2s
baseline; (b) screen locked, then unlock → does the ~30s reappear? (isolates lock/sink-resume); (c) unlocked but
mid-heavy-compile → does lag grow? (isolates contention). One run each cleanly separates the two theories.

**First corroborating datapoint (2026-07-14, BR):** with the screen **unlocked** (and the box under light
agent load), a subsequent bing lagged only **~4s** — near the ~2s baseline, an order of magnitude below the
locked ~30s. That is condition (a) landing exactly where the lock theory predicts, and it fails to reproduce
under mere light load — early evidence *for* lock/sink-resume and *against* generic CPU contention. Not yet the
controlled (b)/(c) runs, but it moves the lock theory from plausible to leading. The audio-leads-
the-modal head-start (above) is what's keeping this usable at all; without it a 30s-lagged post-modal beep would
be near useless.
- **Design implications for SM105's `approval-wake`:** (a) keep it **dirt cheap and non-blocking** — no JVM/
  scala-cli in the hot path (this is the concrete reason approval-wake stays bash, not `.sc`, per SM105); (b)
  consider **pre-warming** the audio (a tiny silent play at session start to page in canberra + the sound cache)
  so the first real bing isn't a cold-start; (c) `notify-send && canberra` chains the toast *before* the sound —
  if `notify-send` blocks under load the sound waits behind it; running them **independently** (or sound first)
  removes that serialization; (d) the deeper fix is upstream: the wake path should not compete with the agent's
  own worker for CPU. Needs one more clean measurement (bing with the box idle vs mid-compile) to confirm
  contention as the cause vs a canberra cold-cache — logged as a hypothesis, not yet a confirmed mechanism.
