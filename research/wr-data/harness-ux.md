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
