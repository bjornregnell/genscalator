# SM129's probe counted BR's PASTES, not stalls — the agent is structurally BLIND to its own asks

**2026-07-17 11:25** (`tt chrono now`). Found SOLO while building `tt stalls`, the tool SM129 specifies.
**This retracts SM129's central empirical claim.** The claim was mine, the probe was mine, and BR believed it.

## The claim being retracted (SM129, verbatim)

> **🔬 PROBED 2026-07-16 — stalls are COUNTABLE (the guess "estimable, not countable" was WRONG).** They leave a
> distinctive greppable trace: `requires confirmation for this command:\r [SEVERITY] <check name>`. No
> gap-inference needed. **Whole-session result (one transcript, spanning the compact): 8 stalls, EVERY ONE a
> MED — 6 × `pipe to head/tail/wc` + 2 × `output redirect (>)`.**

⇒ **The original guess ("estimable, not countable") was RIGHT. The probe that overturned it was wrong.**

## What the trace actually is

Structural search for `command:\\r \[MED\]` across **every** transcript in the project (370 files): **28 matches.**
Every single one, inspected with a 240-char backward window, is **BR pasting the dialog into chat**:

```
{"type":"last-prompt","lastPrompt":"ig: after compact regerssion? \"\"\" Bash command\r\r   scala-cli test ...
   2>&1 | tail -40\r   Run tools test suite\r\r Hook PreToolUse:Bash requires confirmation for this command:...   ×11
{"type":"last-prompt","lastPrompt":"WR data: pipe clobbery; head again; where is your real head :) regression
   after compact: \"\"\" Bash command\r \r   tt web 2>&1 | head -40\r ...                                          ×10
{"type":"last-prompt","lastPrompt":"ig: WR data  pipe clobbery: \"\"\" ...                                          ×6
"...## What happened (observable)\n\nThe agent ran, while BR was AFK:\n\n```\ntt text grepr <dir> scala
   \"^//> using file\"\n→ Hook PreToolUse:Bash requires confirmation..."                                            ×4
```

Three sources, **none of them a harness record**:
1. **BR's pasted dialogs** — he copied the stall into chat, mostly cued `WR data` or `ig:` (from inside the stall).
2. **`{"type":"last-prompt"}` records**, which re-store the same prompt **over and over** (623 such records in the
   fixture). ⭐ **The ×11 / ×10 / ×6 multiplicity is the proof**: a real event is not recorded eleven times.
3. **Our own `wr-data/` note** quoting the dialog, read back into the transcript.

## ⭐ The structural reason, and it is the valuable part

**The transcript is the AGENT's record.**

| | goes to | enters the agent's context? | in the transcript? |
|---|---|---|---|
| **`deny`** | **the agent** (the hook's reason is returned as the tool result) | **yes — it must be, or the agent could not retry** | ✅ **recorded, exactly** |
| **`ask`** | **the human's screen** (a terminal dialog) | **no. never.** | ❌ **not recorded, anywhere** |

⇒ **The agent is structurally blind to the stalls it causes.** It cannot see them at the time (no observer runs
during the pause) and it cannot see them afterwards (nothing is written). **The only way a stall enters the
agent's world is if the HUMAN pastes it.** Which is exactly what BR did, eight times, and exactly what the probe
then counted.

**Corroborating evidence (independent of the above):** the fixture's record-type histogram contains **no
permission/stall record type at all** — `message · assistant · user · direct · text · thinking · attachment ·
queue-operation · last-prompt · permission-mode · ai-title · mode · file-history-snapshot · system · update ·
file-history-delta · create · file · unavailable · image`. (`permission-mode` is the *mode* — default/acceptEdits —
re-recorded 622×, not a stall.) **There is no slot for an ask to live in.**

**Today's session confirms it from the other side:** BR hit **2 stalls this morning** (the `mkdir` and the `mv`,
both pinned as SM134 #9). Structural search of today's transcript finds **ZERO**. The two `mkdir` commands ARE
there as ordinary tool calls, with **nothing marking that they stalled**.

## What survives, and what does not

**DOES NOT survive:**
- ❌ *"stalls are COUNTABLE"* — **asks are not countable from the transcript. There is no trace to count.**
- ❌ *"No gap-inference needed"* — **gap inference (SM121) is the ONLY route to asks.** SM129's line *"Ties SM121
  (hangover — the gap method turned out unnecessary here)"* is **backwards**: the gap method was dismissed on the
  strength of this probe, and it is the only instrument left. ⚠️ It is ambiguous by construction (idle / stall /
  long command / human thinking all look alike), which is the `hangover?` chip's noise-floor problem exactly.
- ❌ **The budget arithmetic**: *"Together they take BR's 8 stalls to ZERO — 8 removed chances to blanket-allow."*
  That assumed 8 = **all** stalls. 8 = **all PASTES**.

**DOES survive, and matters:**
- ✅ **The 8 are REAL events.** BR pasted real dialogs he really experienced. The probe found 8 real stalls. It is
  a **valid LOWER BOUND** and a **valid sample of CAUSES** (6 pipe, 2 redirect). It is **not a census**.
- ✅ **The sampling frame is now named**: *stalls BR found annoying or interesting enough to copy into chat.*
  Biased toward the memorable, and toward the period when we were actively studying stalls.
- ✅ **The thesis is UNTOUCHED**: every stall risks a blanket-allow; `deny` is cheap; `ask` is expensive; minimise
  all stalls. **Nothing here weakens the argument for acting.** ⭐ If anything it **strengthens** it: the true
  stall count is **unknown and ≥ 8**, so the fix's value is **unquantified and possibly much larger**. We lost the
  number, not the case.
- ✅ **DENIES are countable, exactly** — see below. SM129 got this backwards too, but in our favour.

## ⇒ The inversion SM129 got exactly wrong

SM129's echt caveat says:

> "all 8 are MED" is partly BY CONSTRUCTION — HIGH *denies* rather than *asks*, emits no "requires confirmation",
> and so **CANNOT appear in this search**

**True as stated, and the conclusion drawn from it is inverted.** Denies are not the invisible class; **they are
the ONLY visible class.** A deny is recorded verbatim, with its command, because the agent must be told:

```json
"content":"[HIGH] && command chain: split into separate bare commands, ONE per Bash call",
"is_error":true,"tool_use_id":"toolu_017FGMRqkJmVVFoaEcG4nHXJ"
```

`tool_use_id` **joins each deny back to the exact command that caused it.**

## ⇒ What `tt stalls` should therefore be (NOT what SM129 specifies)

**Not built yet — the spec has to be re-decided first, and that is BR's call (the pin is his board).**

**Buildable, exact, and worth having:**
1. **Count DENIES** by check name, from `is_error` tool_results whose content starts with a severity token.
2. **Join to the command** via `tool_use_id`.
3. ⭐ **Re-run `Guardcheck.cmdFindings` on that command and report which denies were FALSE POSITIVES** — a finding
   that vanishes under `maskQuoted` was a quoted false positive. **This turns SM129's "precision FIRST" ordering
   constraint from a judgment call into a measured number**, which is exactly what
   [[severity-double-duties-as-the-mask-selector-so-sm129s-med-to-deny-is-unsafe-2026-07-17]] says is missing.
4. **Re-run TODAY's checks over an OLD transcript** ⇒ which historical stalls the current guard would no longer
   fire. This measures a fix's value **retroactively**, e.g. `eb0cd14` vs the 2 redirect asks.
5. **Say loudly that ASKS CANNOT BE COUNTED**, and why. An instrument that silently reports only denies would
   repeat this exact error one level up.

⚠️ **And it must read SUBAGENT transcripts too** (`<session>/subagents/agent-*.jsonl`) — a sub-agent's Bash call
goes through the same hook and stalls the same human. 15 of the 28 matches are in subagent files. **The main
transcript alone undercounts.** (Live right now: the fable meta-minion is running Bash while BR is away.)

## Honest limits

- ⛔ **UNCHECKED: whether the harness logs asks anywhere OUTSIDE the transcript** (a separate log under
  `~/.claude/`). If it does, asks become countable again and this note's "not countable" narrows to "not countable
  *from the transcript*". **Not asserted either way — this is the obvious next probe, and it could partly rescue
  SM129.**
- The `deny` recording is confirmed by **n=2** in one session (yesterday's `cd &&…| tail`, and today's false
  positive). The mechanism (the agent must be told) makes it general, but the JSON shape is the **harness's**, not
  ours, so it can change on any upgrade — the same fragility that makes `requires confirmation` a bad thing to
  match.
- **`"interrupted by user"` × 5** in the fixture is a *different* signal (BR pressing ESC), not a permission
  dialog. Not investigated.

## ⭐ The methodological finding (this is the one to keep)

**The instrument measured the researcher's own note-taking.** BR pasted 8 stalls as WR data → the probe searched
the transcript → found 8 → reported "8 stalls happened". **The number was real. Its referent was not.** The
evidence trail *of studying the phenomenon* was mistaken for the phenomenon.

**Why it was so convincing:** the count was *specific* (8), *decomposable* (6+2), *plausible* (the pipe reflex is
real and well documented), and it **matched a true story we already believed**. It agreed with everything — because
it was derived from our own account of it. ⇒ **a false MECHANISM propping up a TRUE conclusion**, which is exactly
the shape of the meta-minion's push-1 Catch A. **Second specimen of that class in 12 hours, found independently.**

⚠️ **And note WHERE the pollution came from: `WR data:` cues.** The very act of BR flagging stalls as research data
is what planted the corpus the probe later mined. **The more diligently we log a phenomenon, the more the
transcript looks like the phenomenon.** Any future transcript-mining instrument has this problem
([[raw-data-append-only]] keeps RAW-DATA clean; the *transcript* has no such discipline and never will).

## Ties

**SM129** (retracted here — the pin must be corrected, this note is not enough) · **SM130** ⚠️ **DEPENDS ON THE
RETRACTED CLAIM**: it says *"SM129's probe shows stalls ARE countable + attributable by cause, so the canary can
watch the approval series"* — **the canary as conceived may be unbuildable**, since approvals leave no trace; it
would have to watch *denies* (which the human never sees) or *gaps* (ambiguous). **SM130 needs re-deciding, not
re-wording.** · **SM121** (the gap method, wrongly retired — now the only route) · **SM134 #9** (today's 2 stalls,
invisible to the instrument) · [[severity-double-duties-as-the-mask-selector-so-sm129s-med-to-deny-is-unsafe-2026-07-17]]
(the sibling found 10 min earlier: precision is unmeasured AND the green light is wrong) ·
[[keep-the-ball-game-retract-by-annotating]] · **SM133** — this retraction has **at least four homes** (SM129,
SM130, the carrier's menu item 1, this note). **Fan-out is now a live obligation, not a research topic.**
