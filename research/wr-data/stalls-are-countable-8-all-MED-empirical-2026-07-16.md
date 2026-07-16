# Stalls ARE countable: 8 in a full session, every one a MED — the first hard NUMBERS on the guard (2026-07-16)

**Type:** WR data — **empirical measurement**, not a design argument. BR: *"we have empirical evidence!"* This note
exists because the finding is a **number**, and almost everything else in the guard/stall thread is reasoning.
**Threads:** [[co-design-specimen-human-falsified-five-agent-premises-in-one-hour-2026-07-16]] (the design thread this
settles), [[prohibition-does-not-arm-the-reflex-use-a-hex-escape-2026-07-16]], SM129 (stall budget), SM130 (canary),
SM121 (hangover — the method it made unnecessary), `SECURITY-MODEL.md` §2.3.

## The question

BR, after reframing stalls as a **budget** (*"EVERY guard stall is a risk as I might be dumb and blanket allow
anything!"*): *"can we reliably count (or at least estimate) the number of stalls this session? (related to hangover
detection i guess...)"*

## The agent's prediction — WRONG, and worth keeping

The agent predicted **"estimable, not countable"**: a `deny` would leave a trace, but an **ask-then-APPROVED** (the
fatigue case that actually matters) would leave *"only a `tool_use`→`tool_result` time gap"*, since a stall injects
human decision latency where execution is milliseconds — confounded by genuinely slow commands. It proposed
borrowing the **hangover/gap** method (SM121).

It also said the right thing about how to settle it: **"probe, don't theorise — the transcript holds a real
specimen."** That is the only reason the prediction got tested instead of shipped.

## What the probe found

Stalls leave a **distinctive, greppable trace**. No gap-inference needed:

```
requires confirmation for this command:\r [SEVERITY] <check name>: <fix text>
```

**Result — the whole 2026-07-16 session** (one transcript, spanning the compact, i.e. a full day of heavy work):

| count | cause | severity | kind |
|---|---|---|---|
| **6** | `pipe to head/tail/wc` | MED | **TRUE positive** — the agent's pipe reflex |
| **2** | `output redirect (>)` | MED | **FALSE positive** — a `>` inside a quoted arg |
| **8** | **total — every one a MED** | | |

## Why this matters: the budget argument became ARITHMETIC

The stall-budget case was, until now, entirely a *reasoning* chain. It is now a calculation:

- The **2 false positives** are **already dead** — `eb0cd14` (quote-aware MED checks) removed that class.
- The **6 true positives** would, under **MED→`deny`** (SM129 §2.2), have been handed to the **agent**
  (*"use `--limit`"*), who retries correctly. `deny` spends **no human attention**.
- **⇒ Together they take BR's 8 stalls to ZERO. Eight removed chances to blanket-allow.**

Since **every stall risks a blanket-allow**, and a blanket-allow **permanently disarms** the guard for that shape,
that is 8 draws on the catastrophic outcome, removed — measured rather than argued.

## Echt caveats (the number is real; do not over-read it)

1. **"All 8 are MED" is partly BY CONSTRUCTION, not evidence.** HIGH findings `deny` rather than `ask`, so they emit
   **no** "requires confirmation" message and **cannot appear in this search**. The supported claim is narrower:
   **every stall BR *experienced* was a MED.** (Which is still exactly SM129's target — but it is not "no HIGH ever
   fired".)
2. **These are RECORDS, not verified-distinct EVENTS.** Double-logging is unchecked; 6 pipe records may be fewer
   than 6 distinct stalls.
3. **Self-pollution confound** — a *loose* count returned **33** vs the real **8**. Big enough to deserve its own
   section: see [The corpus is self-polluting](#the-corpus-is-self-polluting-the-methodological-finding) below.
4. **Whole-transcript scope:** the transcript spans compacts, so this is a *day*, not a *context window*.

## The corpus is self-polluting (the methodological finding)

**Measured, not argued:** counting the loose phrase `"requires confirmation"` in the transcript returns **33**.
The real number of stalls is **8**. **~76% of the hits are not stalls — they are us, talking about stalls.**

### What the other 25 are

From the histogram of what follows the phrase (`tt text freq … "requires confirmation(.{0,70})"`), the non-stall
hits are, verbatim from the data:

- **The agent's own research prose** — e.g. `: [MED] output redirect (>)\n```\n\nThe `>` was inside a **quoted
  rege…` (×4). That is *this very thread*: the wr-data notes, the quoted fix-texts, the design argument.
- **The agent's own analysis of stall counts** — e.g. `\" modal BR actually saw**.\nSo the agent experienced ~2-3
  guard inter…`. The agent counting stalls, in prose, becomes a stall-shaped hit.
- **Tool-call descriptions** — `\"","description":"Count stall confirmation messages"`. Even the *probe's own
  description field* matched. **The act of measuring left a hit in the thing being measured.**
- **Truncated summary records** — `…","leafUuid":"…"`, the harness's own compaction artefacts.

### Why it happens: the transcript is both the workspace AND the data

This is not an observer effect in the physics sense — observing does not change the stalls. It is simpler and
more awkward: **the record of the work and the record of the research-about-the-work are the same file.** Every
message the agent writes analysing stalls lands in the corpus a stall-counter later reads. The instrument cannot
tell a stall from a sentence about a stall, because both are text in the same JSONL.

**Any project whose method is "the pair studies itself via the transcript" has this by construction.** That is
genscalator's method.

### The recursion — PREDICTED, then MEASURED, and the prediction was WRONG

**The agent's claim was:** the discriminator is now written down in this note, so *"the moment this file is read
into a conversation, the discriminator itself enters the corpus and stops discriminating… there is no phrase-based
discriminator that survives being documented."* Tidy, alarming, and **false** — so it was tested.

**The live experiment (this session, ~20 minutes apart, no new stalls in between):**

| instrument | before writing this note | after writing it | Δ |
|---|---|---|---|
| **loose** phrase count (`"requires confirmation"`) | **33** | **70** | **+112%** |
| **structural** count (`:\r [SEVERITY]` shape) | **8** | **8** | **0** |
| **actual stalls** | 8 | 8 | 0 |

**The loose metric more than doubled from research activity alone.** Signal fell from 8/33 ≈ 24% to 8/70 ≈ 11%:
*"studying it harder makes it harder to study"* — no longer a hypothesis. **The structural metric did not move**,
even though this note names its pattern 11 times.

### Why the structural one survived — NOT the reason the agent gave

Histogram of the bytes right after the phrase:

| bytes | count | what |
|---|---|---|
| `\\r [SEVERITY]` | **11** | **the agent's prose** — note the **DOUBLE** backslash |
| `\r [MED] pipe` | 6 | real stalls |
| `\r [MED] outpu` | 2 | real stalls |
| `.r \\[([A-Z]+)` | 2+1 | the agent's regex, in tool-call descriptions |

**The discriminator is an invisible control character, not visible text.** The harness writes a **real carriage
return**, which JSON encodes as `\r` (2 bytes: `\`, `r`). When the agent *writes about* it, it types a **literal
backslash**, which JSON encodes as `\\r` (3 bytes). The pattern `…command:.r ` matches the former and **cannot**
match the latter. **Writing about the discriminator automatically escapes it.**

Same reason the agent's *first* regex failed: it was built from **BR's paste of the TUI string**, where the CR is
rendered away (`: [MED]`, a plain space). **Neither a human's paste nor an agent's prose can reproduce the raw
control byte** — they both launder it.

**So the general rule is VINDICATED but the mechanism was misdiagnosed:** *measure structure the agent cannot
author* — realised here at the **byte** level (a control char), not the record level the agent proposed. **And we
got it by luck, not design.** Do not mistake this for a solved problem:

- **It is fragile.** Anything that lands a *raw* CR in that position pollutes — e.g. pasting a real terminal capture
  rather than retyped text, or a tool that echoes the record verbatim.
- **The claim that survives** is narrower: *a **visible-phrase** discriminator dies when documented; an **invisible
  control-byte** one survives, because documenting it escapes it.* That is a fact about JSON escaping, not a
  principle to rely on.
- **Build the robust fix anyway** (below): filter by record type. The accident bought us a correct number *today*.

### The bias has a DIRECTION, and it is the dangerous one

The pollution is **not** random noise — it **inflates**, and it inflates **in proportion to how much attention we
pay the phenomenon**:

- Signal-to-noise **degrades as a function of research effort**. Today: 8/33 ≈ 24% signal. Every further note,
  commit message, and design discussion makes it worse. **Studying it harder makes it harder to study.**
- A naive instrument would therefore report stalls **rising** while they are actually **falling** (they are: the
  false-positive class died with `eb0cd14`).
- **This is acutely dangerous for SM130.** A **blanket-allow canary** built on loose text matching would be fed by
  *our own discussion of blanket-allows* — it would cry wolf **because we studied it**, and a canary that cries
  wolf recreates the alert-fatigue it exists to detect. **A metric that alarms in proportion to the attention paid
  to it is worse than no metric**, because it looks like evidence.

### What is safe and what is not

| metric kind | polluted? | why |
|---|---|---|
| **numeric/structural fields** — `output_tokens` (SM128), ISO timestamps + `compact_boundary` (SM121) | ✅ **safe** | the agent cannot accidentally *emit* a token count or a boundary record by writing prose about them |
| **text-matching over the whole transcript** — a naive `tt stalls`, any phrase-counted metric | ❌ **vulnerable** | prose about X is indistinguishable from X |

Note this **retroactively vindicates** SM128 and SM121: both read *structural* fields, so neither is polluted by
the (extensive) prose written about them. That was luck, not design.

### The mitigation: filter by RECORD, then match

Do not grep the file. **Scope to the record type/field where the harness writes the trace, then match inside it.**
The stall trace lives in a specific record kind; the agent's prose lives in `assistant`/`user` message records.
Filtering those out first removes the entire pollution class **and is immune to the recursion**, because the agent
cannot author a record of the harness's type no matter what it writes.

**→ Design requirement for `tt stalls` (SM129):** parse the JSONL and select by **record type first**; never
phrase-count the raw file, and **do not lean on the CR accident** — it is a byte-level fluke that a single pasted
terminal capture could break. **Regression test, already available:** the instrument must return **8** while a loose
count returns **70+ and climbing**, *and it must keep returning 8 as this thread grows*. That divergence is a free,
self-maintaining test fixture — the corpus pollutes itself a little more every time we discuss it.

### The general rule

**When the workspace is the corpus, any metric matching CONTENT measures the research as well as the phenomenon.
Measure STRUCTURE the agent cannot author.**

**Corollary (measured, and narrower than the agent first claimed):** a **visible-phrase** discriminator dies when
documented; an **invisible control-byte** one survives — because writing *about* a control byte escapes it, and
neither prose nor a human's paste can reproduce the raw byte. Real, but a **fact about JSON escaping**, not a
principle to build on.

**Why this matters beyond stalls:** it retroactively explains why SM128 (`output_tokens`) and SM121 (timestamps,
`compact_boundary`) are **immune** — they read fields the agent cannot emit by writing prose — while any future
text-matched metric is not. **The direction of the bias is the dangerous one:** pollution *inflates*, in proportion
to attention, so a naive instrument reports the phenomenon **rising** exactly when the team is working hardest on
**fixing** it. For **SM130's canary** that is disqualifying: a canary fed by our own discussion of blanket-allows
would alarm **because we studied it**, and a canary that cries wolf recreates the alert-fatigue it exists to
detect. **A metric that alarms in proportion to the attention paid to it is worse than no metric, because it looks
like evidence.**

## Method note: the data corrected the regex

The agent's first pattern (`command: \[([A-Z]+)\]`, built from the format **BR pasted from the TUI**) matched
**nothing** — the on-disk format carries a `\r` (`command:\r [MED]`). The TUI rendering and the transcript record
**differ**. Recovered by asking the data what it looked like (`tt text freq` over `requires confirmation(.{0,70})`)
instead of guessing again. **A human's paste of a UI string is not the wire format.**

## Follow-ups

- **Build `tt stalls <transcript>`** (SM129): count + histogram by cause/severity. **You cannot manage a budget you
  cannot count**, and it is the prerequisite for the **blanket-allow canary** (SM130), which must watch approval
  *patterns over time*.
- Verify records ↔ events 1:1.
- **The gap method (SM121/hangover) was not needed here** — worth remembering before reaching for the sophisticated
  instrument: the crude one had a clean signal. The agent proposed the clever method first.
