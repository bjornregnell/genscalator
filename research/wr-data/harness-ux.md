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
