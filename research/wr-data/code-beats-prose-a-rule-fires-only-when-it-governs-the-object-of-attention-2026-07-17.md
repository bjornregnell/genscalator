# "Code beats prose" — a rule fires only when it governs the OBJECT OF ATTENTION, never a MEANS

**2026-07-17, afternoon.** **The tagline is BR's**, coined live: *"tag line **'code beats prose'** (same logic as to
why comments in code are bad if code can tell it without them)"*. This note is the argument behind it, the
qualifier that keeps it true, and the day of episodes it rests on.

## BR's question, which is the one worth answering

> *"why? just random behavior or habit reflexes that we cant overcome or is it just that we havent found the right
> persistent substrate?"*

Three hypotheses, asked after the agent violated [[prefer-inrepo-tmp-over-slash-tmp]] **with the memory loaded**.
**Answer: (c) — but "substrate" is the wrong lever, and the data says why.**

## The evidence: one day, a clean split

⚠️ **Adjudicated on BEHAVIOUR, not introspection** (standing confabulation caveat — the agent cannot see its own
retrieval and does not claim to).

**FAILED, every one with the memory loaded:**

| the rule | the memory | what happened |
|---|---|---|
| in-repo scratch, not `/tmp` | [[prefer-inrepo-tmp-over-slash-tmp]] | wrote to `/tmp` **twice** (own probes + the minion's sandbox) |
| warm the minion / give it its tools | [[warm-delegated-subagents-lack-caller-skills]] | shipped a **prohibition-only** brief; the minion flailed |
| the agent cannot count asks | **its own retraction, 4h old** | wrote *"measured: ~8 guard stalls"* into a pin |
| the baton is a claim, not a fact | **the baton's own banner** | read it, then believed the file 20 min later |
| BR owns `afk` | the baton, explicit | cleared `afk` unilaterally |
| use `tt git` / Read+Write, not raw shell | [[commit-via-tt-git-not-raw-cd-git]] | reached for raw `cp` |

**ARMED, same agent, same day:**

| the check | why it fired |
|---|---|
| *"I know what the minion is stuck on"* → **false** | **diffed 28 `tool_use` ids vs 26 `tool_result` ids** — a tool call |
| *"~8 stalls"* → **false** | **had to enumerate the pastes** to write them up — a mechanical act |
| *"I built it in a sandbox"* (in BR's voice) → **false** | **re-read the draft** — a mechanical act |
| a whole damning PR verdict → **false** | **ran the code** (a probe) instead of reasoning from source |
| the `gh pr *` blanket-allow → **flagged unprompted** | a **judgment** call, in an already-deliberative moment |

## ⭐ The discriminator — and it CORRECTS our own prior explanation

`SECURITY-MODEL.md` §3.3 blames **rot**: an in-agent fix works only if *"loaded AND hot at the instant of action"*.
**That is incomplete, and the counter-example is in this repo:**

> **BR's no-em-dash rule is purely MECHANICAL, is never "hot", and arms reliably** — the agent wrote a whole blog
> draft in BR's voice without one, unprompted, on the same day it failed six other mechanical rules.

**So heat is not the variable.** The variable is *what the rule is about*:

> ### **A rule fires when it governs THE OBJECT OF ATTENTION. It does not fire when it governs a MEANS.**

When the agent typed `cp`, attention was on **preserve the file**; the command was **instrumental, beneath notice** —
the way you do not notice which fingers you type with. Same for `/tmp` (attending to *run a probe*, not *where files
live*) and the minion brief (attending to *get it building*, not *what does it need*). **A rule about an incidental
action never enters the moment it is meant to govern.** The em-dash rule is different only because **the prose IS the
object**.

⇒ **Not (a) random:** the split is far too consistent. ⇒ **Not (b) unbeatable:** the identical agent caught four real
errors that day, reliably, whenever the check had somewhere to execute. ⇒ **(c), but no amount of better storage
helps: the knowledge is not missing, it is ARRIVING LATE TO A RACE IT CANNOT WIN.** Retrieval is slower than the
reflex, and the reflex owns the moment.

> ### ⇒ **The lever is not better storage. It is REMOVING THE RACE: make the wrong move UNAVAILABLE, not forbidden.**

## ⭐ BR's tagline, and why the analogy is exact

**A comment is a CLAIM about what the code does; the code is the MECHANISM.** When they disagree the code wins, and
the comment **rots silently**, because nothing forces it to stay true. **Swap the nouns:** a memory / skill /
briefing is a **claim** about what the agent does; the **tool** is the mechanism. Same rot, same winner, same silence.

⚠️ **Worse for agents than for comments** — which strengthens it: a stale comment at least gets **read** when someone
reads the code. **A stale rule about an instrumental action is never consulted at all.** Not misleading. **Absent.**

### ⛔ The qualifier — without it the tagline says something false

Unqualified, *"code beats prose"* reads as **"delete the memories"**, which the evidence refutes. The qualifier is
**BR's own rule**, from `CLAUDE.md`: *"Only write a code comment to state a constraint the code itself can't show."*

| | ❌ delete it, build the mechanism | ✅ keep it — no mechanism can hold it |
|---|---|---|
| **code** | a comment restating the code | *why* this constant; what breaks if you change it |
| **agents** | a rule a tool could enforce (which command, which path, which dir) | judgment, values, taste, threat reasoning |

⇒ **Prose loses precisely where a MECHANISM IS POSSIBLE, and prose is all there is where one is not — which is
exactly where it works.** *(Both directions evidenced: the no-em-dash rule never fails; `SECURITY-MODEL.md` is prose
and irreplaceable.)*

**⭐ And the two tables are the same table.** *Instrumental action* ⇔ *a tool could enforce it*. *Object of
attention* ⇔ *no tool can*. **That is not a coincidence — it is why the analogy holds**: you can only mechanise what
the agent is not attending to, because attention is exactly what a mechanism cannot supply.

## 📌 The specimen is the security model itself

`SECURITY-MODEL.md` §2.3 asserted: *"8 stalls, every one a MED… **the budget argument is now arithmetic, not
rhetoric**."* **That was PROSE ABOUT A MEASUREMENT.** It was false (the 8 were BR's pastes), it **rotted into a
standing falsehood**, and it sat in the repo's most load-bearing doc for a day — **found only because BR asked for
something else in that file.**

⭐ **Had `tt stalls` existed, that number would have been CODE — and code cannot rot into a false claim; it either
runs or it does not.** ⇒ **§2.3 IS a comment that outran its code, in the document that preaches against exactly
that.** *(And the fan-out failed too: the retraction note listed **four homes** and missed this one — see
[[retracted-is-not-immune-i-remade-sm129s-claim-4h-after-retracting-it-2026-07-17]].)*

## ⇒ Actionable: the deny is the lever, and it has a precondition

**A `deny` is the lever because it IS code, not prose:** it fires at the instant of action, reaches the **agent**
(not the human's screen), and is **recorded**. The machinery already exists — `guardcheck.scala` already hands the
check's `fix` string to the agent on a deny.

**⛔ BUT:**

> **A deny must name a tool that EXISTS. A deny without a provision is a prohibition-only briefing implemented in
> the guard — the agent will flail against it exactly as the minion flailed against a rule list with no tools.**

⇒ **ORDER: build the lane, THEN close the road.** SM137 (`tt git` read-verbs, `tt forge`) is a **prerequisite**, not
a companion.
⚠️ **And the deny is expensive when wrong** — 2 false-positive denies in 15 min on read-only commands, **with no
override**. So **measure the false-positive rate first** (SM129 re-spec) before adding any.

## 🔬 The prediction, on record

> **Rules about INSTRUMENTAL actions will never arm from any substrate, however well written. Rules about the
> OBJECT OF ATTENTION can.**
> **If a future agent catches itself reaching for a raw command BY REMEMBERING A NOTE, this is FALSIFIED.**

⚠️ **Note the reflexive trap, and it is not a joke: THIS NOTE IS PROSE ABOUT WHY PROSE DOES NOT WORK.** By its own
thesis it will not arm the agent that reads it. **It is not written to arm anyone — it is written to justify
building the mechanism.** If it is ever *cited* as the reason a slip did not happen, that is the falsification.

## Honest limits

- **One day, one agent, one model.** The split is consistent across ~11 episodes, but it is **an argument, not a
  measurement**. No control, no blinding, and the agent is the subject.
- **"Object of attention" is not operationalised.** It is a post-hoc classification made by the same agent, and it
  is **exactly the kind of unfalsifiable-from-inside story this corpus warns about**. It earns its keep only because
  it **predicts the em-dash counter-example that the rot story gets wrong** — that is one successful prediction, not
  a theory.
- ⛔ **NOT claimed: that memories are useless.** Claimed narrowly: **memories governing instrumental actions failed
  6/6 on 2026-07-17, and a memory governing the object of attention succeeded.**
- **The failures are selected by salience.** We noticed the ones that bit. There may be instrumental rules that arm
  silently every day and are invisible for that reason. **Not measured; a real threat to the split.**

## Ties

[[retracted-is-not-immune-i-remade-sm129s-claim-4h-after-retracting-it-2026-07-17]] ·
[[brief-the-tool-lane-not-the-rule-list-a-prohibition-only-brief-armed-nothing-2026-07-17]] (**the sibling: the
same finding, found in delegation an hour earlier**) · `the-note-to-my-post-warp-self-reached-only-the-human-2026-07-17.md`
(the banner that armed nothing) · `spontaneous-pinning-is-a-state-not-a-trait-2026-07-17.md` (**superseded in part:
"heat arms CONCEPTUAL not MECHANICAL" is the rougher version of this note's attention/means split**) ·
`prohibition-does-not-arm-the-reflex-use-a-hex-escape-2026-07-16.md` · **`SECURITY-MODEL.md` §3.3.1** (the
operational rule) · **§3.5** (*"only removing the reach works"* — the same conclusion from the path angle;
**two independent routes to one rule**) · **PB SM137** (build the lane) · **SM138** (the delegation half) ·
[[prefer-inrepo-tmp-over-slash-tmp]] · [[commit-via-tt-git-not-raw-cd-git]].
