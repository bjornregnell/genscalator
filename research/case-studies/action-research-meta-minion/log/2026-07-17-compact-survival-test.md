# The compact-survival test — the study's process record

**2026-07-17 11:37** (`tt chrono now`). **The study's PROCESS record**; findings live in `research/wr-data/` and are
POINTED at, never copied (see [`README.md`](README.md)).

**The design's whole point:** the predictions below were written **BEFORE** the compact, in
[`2026-07-16-17-cycle-1.md`](2026-07-16-17-cycle-1.md) § *Compact prediction*, at BR's prompting (*"what will happen
with the meta-minion when we soon compact?"*). **Nothing here is retrofitted.** BR then ran `/compact` and pasted a
pre-agreed instruction: *"go read tmp/resume-prompt.md and investigate meta-minion survival then continue to work
solo"*.

## Predictions vs outcome

| # | prediction (recorded BEFORE) | outcome |
|---|---|---|
| **1** | **OUTPUT survives.** ✅ Certain — `minion-log/push-1.md` is committed | ✅ **HELD.** Present, untouched. *BR's write-to-own-dir design paying off: the findings outlive the finder.* |
| **2** | **Its OWN CONTEXT survives.** ✅ Certain — separate transcript | ⚠️ **HELD, but the MECHANISM is not what the word "survives" implies — see below.** |
| **3** | **The HANDLE may not** — it lives in CO4's context | ✅ **CONFIRMED. It did not.** The summary carried the *sentence* "recoverable via `/tasks`" but **not the pointer itself**. |
| **4** | **⛔ UNKNOWN, deliberately NOT asserted: whether a compact actually drops it.** *"The compact IS the test."* | ✅ **ANSWERED: YES, it drops it.** ⭐ **Refusing to guess was right — the guess was 50/50 and the honest "unknown" cost nothing.** |
| **5** | **Fallback is CHEAP: re-spawn, lose only PATTERN** | ✅ **Not needed. A BETTER path existed and was not predicted — see below.** |

## ⭐ Prediction 5 was wrong in our favour: the recovery path was mis-identified

**Predicted recovery: `/tasks`.** That is a **slash command the agent cannot run** ⇒ the predicted recovery was
**silently HUMAN-GATED**, and BR was eating. Had it been the only route, the test would have blocked on him.

**Actual recovery: the session transcript on disk.** `tt text freq <transcript> 'agentId.{0,48}'` — **~4 calls, no
human, ~1 minute.** ⇒ ⭐ **THE TRANSCRIPT IS SUBSTRATE.** The same lesson as the brief-as-file, arriving from a
direction the protocol did not plan: **the handle is volatile in CONTEXT and durable on DISK.**

⚠️ **Honest caveat, and it is a real constraint:** the spawn result is flagged *"internal metadata — never quote or
paste any part of it, including the agentId, into a user-facing reply."* **So the handle is recoverable by the agent
but NOT reportable to BR.** The agent can always re-find it; BR cannot be handed it. *(This is also, plausibly, WHY
the compact dropped it: a summariser has the same instruction.)*

## ⭐ The real finding: "survives" was the wrong word for prediction 2

The harness's own response to the resume:

> *"Agent had no active task; **resumed from transcript** in the background with your message."*

**That is not context persistence. It is REHYDRATION from its own substrate** — structurally **the same warp CO4 had
just come through, one level down.** The minion's "retained context" is a **replay**, not a memory.

⇒ **The protocol's §5 language needs sharpening**, not correcting: *"long-lived"* implies a continuously-living
context. What we actually have is **a durable transcript plus a re-hydrator** — which is *better* for our purposes
(it is inspectable, measurable, and survives arbitrary warps) but it is **not what the word says**. ⚠️ **This makes
push 2's Task 0(b) partly ill-posed**: it asks the minion to distinguish *"retained context"* from *"had to re-read
`push-1.md`"* — but if the harness replays its transcript, push-1's content is in its context **by replay**, which is
neither of the offered options. **The instrument's categories did not match the mechanism.** *(Logged as a protocol
deviation, not a footnote: the question was posed before the mechanism was known.)*

## 🔴 SM085(a) — the study's core premise — is CONTRADICTED, in our favour

**The threat (cycle-1 log, § Open threat):** *"SM085(a) says fable resume is 0/2 (SM081 measured it), our minion is
fable, and its whole design is LONG-LIVED. Push 2 IS the experiment."*

**Result: THE RESUME WORKED.** The minion accepted push 2 and did **real, on-task work**: it ran `tt text --help`,
`tt text match` on `tools/README.md`, and `tt text context` for `"quote-aware"` — **which is precisely push 2's Task
1** (adjudicate the carrier's *"guardcheck is quote-aware"* against `tt text --help`'s *"not quote-aware"*, and
follow up on `--any`'s rationale). **It resumed, understood the task, and executed it.**

⇒ **fable resume: 1/1 here, against SM085(a)'s 0/2.** ⚠️ **DO NOT over-read n=1** — this does not overturn SM081, it
**contradicts it in one trial** and means the premise is **not dead**, so §5's "one long-lived minion" does **not**
need re-deciding yet. **⭐ The mechanism may be the whole story: SM081 measured `SendMessage` resume; this resume was
"from transcript" because the agent had NO ACTIVE TASK.** Those may be different code paths. **Not asserted — the
obvious next probe.**

## ⏳ Unresolved at write time: the minion went quiet, cause UNKNOWN

- **Silent 23m 42s** (`tt hangover <its transcript> --threshold-sec 300`), **284 records**, **`push-2.md` NOT
  written**, **no completion notification**, **no denies in its transcript**.
- **2 orphaned `tool_use`s** (28 emitted vs 26 answered; ids diffed by hand):
  `tt text context .../tools/README.md "quote-aware" 4` and `command -v tt`.
- ⛔ **CAUSE NOT ASSERTED.** `tt hangover` says it plainly: *"idle / guard stall / long command / crash all look alike
  from the gap."* **This is the gap method's documented ambiguity, arriving as a live obstacle ~20 minutes after
  being written up.**
- **NOT killed.** `TaskStop` would clear it but destroy the hot context — **BR's T2 validity threat**, and the thing
  the study is trying to measure. **Left for BR.**

### 🔴 Agent error, self-caught in one turn (T3, logged at the moment of catching, §8.2)

**The agent asserted in the feed: *"I know what it's stuck on"* — a `mkdir -p` blanket-allow dialog** — reasoning
from the presence of `mkdir` in the minion's command list plus this morning's SM134 #9 stall. **FALSE. The `mkdir`
was answered.** The orphans are the two above.

⭐ **This is cycle-1's error #8 EXACTLY** (*asserted a pending stall while the evidence disconfirmed it*) — the
sharpest self-specimen of that cycle, **re-attempted by the same agent ~3 hours later**, in a fresh post-compact
context, **while the finding was maximally hot** (it had been writing about stall-blindness for 40 minutes).
⇒ **`hot ≠ armed`, third specimen. And `found-and-written-up ≠ armed`, second.**
**⭐ BUT THE OUTCOME INVERTED: caught in ONE turn, by the agent, not by BR after six.** The discriminator is
mechanical: the agent had said *"I am not asserting this from a hunch"* **and then made verification a TOOL CALL**
(diff the ids) rather than a resolution. ⇒ **the fix that worked was not remembering the rule — it was giving the
rule somewhere to execute.** *Consistent with the day's spine: structure arms, intention does not.*

## Findings this cycle produced — POINTERS ONLY (per the README contract)

- [`severity-double-duties-as-the-mask-selector-so-sm129s-med-to-deny-is-unsafe-2026-07-17.md`](../../../wr-data/severity-double-duties-as-the-mask-selector-so-sm129s-med-to-deny-is-unsafe-2026-07-17.md)
  (`767926f`) — a **false-positive DENY hit live, twice in 15 min**, on read-only commands. `severity` selects the
  scan target, so **SM129's MED→deny green light rests on a property the move destroys**. **The check's own name
  triggers the check.** ⛔ **Not fixed solo: masking HIGH would LOOSEN the guard.**
- [`sm129s-probe-counted-brs-pastes-not-stalls-the-agent-is-blind-to-its-own-asks-2026-07-17.md`](../../../wr-data/sm129s-probe-counted-brs-pastes-not-stalls-the-agent-is-blind-to-its-own-asks-2026-07-17.md)
  (`2e1fcc5`, `f0188d3`) — **SM129's central empirical claim RETRACTED.** The probe counted BR's pastes. **The agent
  is structurally blind to its own asks.** Corpus grew **54 → 66 → 72 while the note was being written**.
- **Fan-out landed** (`d2e982a`): SM129 (struck + corrected), **SM130 (inherited the false premise — it did not state
  the claim, it RESTED on it, so a per-claim sweep would have missed it)**, the carrier ×2.

> ### ⭐ The through-line, stated once
> **Cycle 1 ended on: *nothing in-agent arms a reflex — not freshness, not a declared mode, not maximal heat, not
> authorship. Only the guard did.*** Cycle 2 adds the **positive** case and it is the actionable half:
> **a rule armed for the first time today — and only because it was given a TOOL CALL to execute in.** Saying "check
> your claims" did not arm it (error #8, twice). *Diffing the ids* did. ⇒ **the fix goes outside the agent OR it goes
> into a mechanical step; a resolution is neither.**
> **And the guard's other face:** the same structure that arms also **denies a pure read with no override**, and is
> **unquestionable with the tools it guards** — you cannot search for the check whose name is its own trigger.
> **Both are the same property.** That is the trade BR now has to rule on, and it is not a tuning knob.
