# WR data — harness UX (human↔agent input channel)

**WR = Workflow Research** (see [`README.md`](README.md)). This file logs friction on the **harness itself** —
the human↔agent *communication channel* — as distinct from [`genscalator-self-dev.md`](genscalator-self-dev.md)
(dynamic-shell / tool friction). These are things genscalator cannot fix (we don't own the harness), but they
are real costs on joint productivity and belong in the WR corpus as upstream asks + agent-side mitigations.
Cross-ref [`../communication-bandwidth.md`](../communication-bandwidth.md) (channel bandwidth),
[`../human-state-and-joint-zone.md`](../human-state-and-joint-zone.md) (perception gap / relayed signals).

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
human mis-attributes it. Cross-ref [`../human-state-and-joint-zone.md`](../human-state-and-joint-zone.md)
(perception gap / relayed signals) and the context-usage-blindness thread in `smart-zone-ceiling.md`.

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
[`../human-state-and-joint-zone.md`](../human-state-and-joint-zone.md) (perception/attention gaps under long runs).

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
`grep`/file-open, not a scroll or a re-ask — the [inference-time-learning](../inference-time-learning.md) substrate
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
