# An audit finds only what it has a THEORY for — and this one ran at rot 710k (2026-07-16)

**Type:** WR data — a **method** finding about audits/sweeps, captured at BR's request because *this* consistency
sweep behaved differently from previous ones and he wants to know why.
**Threads:** [[two-error-classes-rot-vs-structural-and-what-a-clear-buys-2026-07-16]] (the pinning trap + the
taxonomy this refines), [[co-design-specimen-human-falsified-five-agent-premises-in-one-hour-2026-07-16]],
SM132 (the audit this predicts the performance of), SM133.

## BR's observation (the trigger)

> *"my previous session's cue to do consistency sweep i dont think rendered this kind of introspection on problems
> and you found some real ones so i think this is important to learn from in the future"*

**The same cue** ("do a consistency sweep") produced **skimming** before and **five real defects** this time.
The cue did not change. So what did?

## What it found (concrete, so the claim is checkable)

In `tmp/resume-prompt.md` — **the clear-carrier**, i.e. the fresh agent's entire inheritance:
1. *"`solo` is probably STALE — BR never said `-solo`, his to drop"* — **flatly false**: he said it; the agent
   removed it. The carrier contradicted reality.
2. Mode list read `solo, tok-spend`; actual state `tok-spend`.
3. The `hangover?` item still framed as *"10s is below the floor — propose a fix"* when **the fix was already
   shipped** (`5080764`, chip→60s) ⇒ **the fix itself was an ORPHAN**, living only in a commit message.
4. SM range `SM001–SM132` after SM133 existed.
5. PB `NOW` still claimed *"SM121 NOT yet wired"* and listed the hook as next-solo.

Plus **two gaps worse than the stale lines**: `tmp/afk-solo-queue.md` — the **only** home of the web design notes,
the deploy mechanism and its guardrails — was **pointed at by nothing** (gitignored: it survives a clear but is
invisible); and the **standing deploy permission** had no durable home at all.

## The hypothesis: an audit finds only what it has a THEORY for

**A generic "check consistency" has no failure model.** With nothing specific to hunt, the search space is
unbounded, so the search degenerates into **reading for plausibility** — and a well-written stale document reads
*perfectly plausible*. That is why previous sweeps returned "looks fine": **they were not wrong, they were empty.**

This sweep had, freshly built in the preceding hour, a **sharp and specific failure model**:
- claims **propagate to multiple homes**, corrections do not (**fan-out**);
- **orphans** live in commit messages;
- **"saying it in chat FEELS like doing it"** (the stale mode file; the un-landed retraction).

So the sweep was not "look for problems". It was **"hunt THIS shape"** — bounded, and therefore findable. Every one
of the five defects is an instance of the model: #1/#2/#5 are *saying-it-feels-like-doing-it*; #3 is a textbook
**orphan**; the two gaps are **fan-out** failures (a claim that reached one home and no other).

**⇒ Generalisation: an audit without a failure model is theatre.** It produces the *feeling* of having checked.
The reusable move is to **arrive with the shape you are hunting**, in writing, before opening the file. *(Cheap
corollary: prefer a checklist of KNOWN failure shapes over "review this carefully" — and grow the checklist from
specimens, not imagination.)*

## ⚠️ The uncomfortable implication — this CUTS AGAINST tonight's own plan

**The sweep ran at `rot? ~710k`, well past the red line.** By the day's own rot logic it should have been sloppy.
It was the most productive audit of the day. Why: **it is CONCEPTUAL work, and conceptual holds longer than
mechanical** (the day's taxonomy). What it needed was not a *fresh context* — it needed a **HOT THEORY**, and the
theory was at its sharpest precisely *because* the session had just spent an hour building it.

**This refines BR's own pinning-trap insight** (*"if we only pin when we're about to clear, we pin while rotted"*).
Both are true, and they pull in **opposite directions**:

| the work | degrades with | so do it |
|---|---|---|
| **mechanical** (relocate text, edit anchors, move blocks) | **rot** | **FRESH** (post-clear) |
| **conceptual** (recognise the failure shape at all) | **theory going cold** | **while the theory is HOT** (pre-clear) |

**⇒ The rule is not "audit while fresh". It is: MATCH THE WORK TO WHAT DEGRADES.** Tonight's sweep was correctly
done at high rot *because it was conceptual*; the archiving of stale NOW beats was correctly **deferred** at the
same moment *because it is mechanical block-moving*. (That deferral was made explicitly, for this reason.)

## ★ A testable prediction (for SM132, and it is falsifiable)

**SM132's audit is planned for AFTER the clear, "while fresh".** But post-clear the failure model will be
**carried, not hot** — the fresh agent will have the resume-prompt's *summary* of the theory, and *"carried ≠
armed"* is this session's most-evidenced claim (5 mechanical regressions against a documented rule; one under an
ACTIVE `+rot-vigil`).

**PREDICTION: the post-clear SM132 audit will find LESS per unit of effort than tonight's sweep did, despite lower
rot** — because the binding constraint is theory-heat, not context-freshness.

**This is a real experiment with a control**: tonight's sweep = *hot theory, high rot* → **5 defects + 2 gaps** in
one pass. The post-clear audit = *cold-carried theory, low rot* → count it. **If the prediction holds, it is
evidence that the resume-prompt cannot transmit a working failure model** — which would be a significant, and
uncomfortable, result about the whole substrate strategy. **If it fails** (the fresh agent finds as much or more),
that is evidence a well-written carrier CAN arm a conceptual reflex — which would be genuinely good news, and would
partly rehabilitate in-agent fixes.

**Confound, stated up front:** tonight's sweep had already *removed* the defects it found, so the post-clear audit
faces a **cleaner corpus** and would find fewer defects for reasons having nothing to do with theory-heat. To keep
it fair, judge on the **51 commit messages** (untouched, and the same corpus for both) rather than on the carrier.

## The reusable practice

- **Before an audit, write down the shapes you are hunting.** No shapes ⇒ do not call it an audit.
- **Sweep immediately after building the model, while it is hot** — do not "save it for when I'm fresh".
- **Split the audit by degradation class:** find (conceptual, hot) and fix (mechanical, fresh) are different jobs
  and can be done at different times.
- **Point at every carrier.** A file nothing links to is invisible even when it survives (`afk-solo-queue.md`).
