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
