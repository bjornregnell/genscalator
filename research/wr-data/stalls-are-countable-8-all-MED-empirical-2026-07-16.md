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
3. **Self-pollution confound (important, and a nice specimen in itself):** a *loose* count of `"requires
   confirmation"` returned **33**, vs the real **8**. The excess is the agent's **own prose about stalls** (this very
   research thread, quoted fix-texts, tool-call descriptions) plus truncated summary records. **The agent writing
   about a phenomenon contaminates the corpus it later measures the phenomenon in.** Any instrument must match the
   structural `:\r [SEVERITY]` shape, never the phrase.
4. **Whole-transcript scope:** the transcript spans compacts, so this is a *day*, not a *context window*.

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
