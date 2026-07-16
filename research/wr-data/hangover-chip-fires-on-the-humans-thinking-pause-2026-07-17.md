# The `hangover?` chip reported the HUMAN's thinking pause as the AGENT's state

**2026-07-17 00:13 (`tt chrono now`).** Live specimen, BR + CO4, ~75 min into a fresh clear-context session.
**Sibling of [`nobody-dropped-the-hangover-chip-2026-07-16.md`](nobody-dropped-the-hangover-chip-2026-07-16.md)**
(same chip, ~90 minutes earlier, opposite error). Requested by BR as WR data. **SM121 · SM134 candidate #5 · safe-solo
menu item 5 (the calibration job).**

## The event, in order

1. BR pauses ~1 minute to compose a message.
2. **The mode line lights `hangover? 1m`** (BR, verbatim: *"THERE WAS a ` hangover? 1m ` present in the mode line
   when i typed my -hangover"*).
3. BR reads it as *the agent may be hungover*, and types **`-hangover`**, with his reasoning attached:
   *"(why: i think you have cleared you thought by now, but thats just my hunch - but you **do** good reasoning in my
   view and it fits with genscalator etc)"*
4. **He presses enter. The chip vanishes** — before the agent acts, before the agent even thinks. BR: *"oops it
   disappeard just when i pressed enter without you thinking... hmmm"*
5. BR diagnoses it himself: *"aha the auto-hangover experienced a gap (but it was a false blackout and that can
   happen we know that)"*
6. `tt mode` → `tok-spend`. **There was never a `hangover` mode to remove.** The agent ran nothing.

## The mechanism (closed, source-grounded)

`statusline.scala`: the gap is `now − last timestamped transcript record` (:123); `hangoverChip` shows when
`gapSec >= showSec`, and `hangoverSec = 60L` (:383).

⇒ **BR's ~1-minute composition pause crossed the 60s threshold, so the chip lit. Pressing enter wrote a new
transcript record, the gap reset to ~0, and the chip cleared mid-keystroke.**

> ## ⇒ THE FINDING: the chip measured the HUMAN's pause and rendered it as the AGENT's state.
> Nothing about the agent changed at any point in the sequence. **The only thing that moved was how long BR had been
> thinking.**

## Why "we know that" is only half true — there are TWO noise floors, and only one was measured

BR: *"it was a false blackout and that can happen **we know that**."* **True, but the known floor is the wrong
floor.** What was measured on 2026-07-16 (and is recorded in `statusline.scala` :378-382) is the
**command-execution** floor: *"an 18s gap was just a command running"* ⇒ 10s is below it ⇒ **60s was chosen as "the
first value above the floor."**

**But that calibration only ever considered gaps the AGENT creates.** There is a second, much larger noise source
that was never in the sample:

| noise source | typical gap | in the 60s calibration? |
|---|---|---|
| a command running (agent busy) | **~18s** (measured) | ✅ yes — 60s clears it |
| **the human thinking / composing** | **~1-3 min** (this specimen: **1m**) | ❌ **NO** |
| a real blackout (session ended) | minutes to hours | ✅ the intended signal |

⭐ **60s sits BETWEEN the two floors: above the agent's, below the human's.** So the chip is **structurally
guaranteed** to fire every time BR stops to think for a minute — which, in a co-design session, is constantly. **This
is not a tuning nit; the threshold was fitted to a sample that excluded the dominant noise source.**

## The deeper error: a hangover CANNOT RE-APPEAR

**Even a perfect threshold would not fix this, and that is the important part.**

**A hangover is a property of a session BOUNDARY** — set once, by a blackout, and **decaying as the agent warms up**.
The metaphor is exact and it is load-bearing: *you do not become hungover again by pausing to think.* **You cannot
re-acquire a hangover by being idle.**

But the chip **re-fires on any 60s of idle, indefinitely, forever.** ⇒ **It is an IDLE TIMER wearing a blackout's
name.** The `?` honestly signals uncertainty about the *inference*, but the underlying quantity is simply not the
one the name promises.

**This re-frames the hook/chip split.** The current explanation (`statusline.scala` :378-382, and the resume prompt)
is that they *"have DIFFERENT noise profiles."* **That is true but it undersells it — they measure different
CONSTRUCTS:**

| | fires when | what it can actually know |
|---|---|---|
| **the HOOK** (`tt hangover hook`, 10s) | **SessionStart only** | ✅ **a true blackout** — a SessionStart *is* the boundary, so the gap it sees is real |
| **the CHIP** (mode line, 60s) | **every turn** | ❌ only *"time since last activity"* — it **cannot** distinguish blackout / command / human-thought |

⇒ **The hook has the right construct and needed no threshold defence. The chip has the wrong one, and no threshold
can rescue it** — 5 minutes would merely make the false positives rarer and more confusing.

## A false positive is a BUG, not a margin — the principle is already ours

This is a **direct transfer of a principle BR established the same day**, when he falsified the agent's claim that
quote-aware guardchecking was a "security loosening": **a false positive is a BUG, not a margin — the agent mistook
imprecision for protection.** Identically here: a chip that cries hangover during ordinary human thought is not
being *cautious*. **It is wrong**, and it spends the scarcest resource in the room — BR's attention — on
reconciliation work he then has to overrule.

**And it is wrong in BOTH directions.** By design it clears after **60s of activity**, while tonight's *behavioural*
hangover — **three argument-shape errors in the agent's first six tool calls**, including twice believing a
false `0 files` — persisted well past that. ⇒ **The chip cleared while the agent was demonstrably still fumbling,
and lit while the agent was reasoning fine.** *(Exact overlap not instrumented; the design's 60s clear is far shorter
than the observed recovery, which is the claim being made — not a measured coincidence.)*

## ⭐ The human out-performed the instrument — and that is the load-bearing observation

**BR overruled the chip using his own read of the agent's reasoning quality** (*"you **do** good reasoning in my view
and it fits with genscalator etc"*) — and **he was right**. The chip measured a **time gap**; BR measured **the
thing the chip is a proxy for**, directly, and beat it.

**This is the day's load-bearing finding paying out exactly as predicted:** *only the guard, the tool interface, and
the HUMAN survive a warp.* Here **the tool interface was wrong and the human caught it.** The proxy is cheap and
fires constantly; the human is expensive, accurate, and — per his own hedge (*"but thats just my hunch"*) —
appropriately uncertain in a way the chip's confident `1m` is not.

⚠️ **But note what it cost:** BR had to *notice* the chip, *doubt* it, *reason* about the agent's state, and *act*
to clear it — and the thing he acted on **did not exist** (`tt mode` → `tok-spend`). **A false positive does not
cost zero. It costs a human decision.**

## Candidate fix (SM134 #5) — NOT built; BR is tired (`:Z`) and this touches a live instrument

**Clears the §0.1 anti-quota gate:** two observed specimens 90 minutes apart, same chip, opposite errors.

- **Sketch:** the chip renders the **SessionStart gap** — the value **the hook already computes**, frozen at session
  start and **decaying with work done** — and **never re-fires on idle**. A hangover is set at the boundary and
  wears off; it does not come back.
- **This may DELETE code rather than add it**, which is the best kind of tool candidate.
- **Open (the real work, needs BR):** what should the decay be a function of? Wall-clock is the obvious answer and is
  probably wrong — tonight's behavioural recovery tracked **turns worked**, not minutes elapsed. **Unresolved, and it
  is the interesting question.**
- ⛔ **NOT built tonight, deliberately.** The statusline has live callers, `:Z` is declared, and per the agent's own
  standing rule *edits to an existing tool need a real invocation test and a named blast radius*. **Touching the
  measuring instrument at 00:13 with a tired reviewer is precisely the move this note argues against.**

## Cross-links

`nobody-dropped-the-hangover-chip-2026-07-16.md` (**the sibling — read together: at 23:47 the chip vanished and BR
credited the agent; at 00:13 it appeared and BR credited the agent again. Same chip, same missing affordance —
NO PROVENANCE — twice in 90 minutes**) · SM121 (`tt hangover`) · SM134 (candidates #3 provenance, #5 this) ·
`research/case-studies/long-lived-meta-minion.md` §0.2 (**the pairing is the unit — this specimen is the tool's
fault, not either party's**) · [[joint-rot-vigilance-recovery-kit]] · [[tired-cue]].
