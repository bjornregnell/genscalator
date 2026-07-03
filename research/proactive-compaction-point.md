# Proactive compaction point — when to compact *before* it's forced

**RQ (research question):** Is it better to compact **often** — even when not strictly *needed* (fill well
below the smart-zone ceiling)? Should we compact even when the ceiling **Z** is far above current fill? Do we
need a **separate metric** for a *proactive*-compact point (distinct from the reactive brake), and what should
we call it? And a notation aside: is **Z** the right symbol for "smart-zone ceiling" (see naming, bottom)?

Parked from a 2026-07-03 session where BR ran `/compact` at **34% fill** (not forced) "to see how much lower
we got" → **336.6k → 46.1k** (34% → 5%). That single data point is what seeded these questions.

Related: [`smart-zone-ceiling.md`](smart-zone-ceiling.md) (defines Z + the reactive brake),
[`token-budget-awareness.md`](token-budget-awareness.md), [`communication-bandwidth.md`](communication-bandwidth.md),
and the *compact dance* / *exit-resume dance* memories.

---

## Vocabulary (so the RQs are precise)

| symbol | meaning | value now |
|--------|---------|-----------|
| **W** | physical context **W**indow — the hard wall | 1M (Opus 4.8) |
| **Z** | **smart-zone ceiling** — fill fraction where quality starts degrading (context rot); sits between the smart and dumb zones (renamed from **L**, 2026-07-03) | ~0.30 **prior**, unmeasured |
| **0.8·Z** | **reactive brake** = existing *compact trigger* — hygiene-compact before hitting the ceiling | — |
| **floor** | fixed overhead that survives *any* compact (system prompt + tools + memory + skills) | ~23k (~2.3% of W) |
| **consolidation point** *(adopted 2026-07-03)* | **proactive** compact trigger — durability-gated, NOT fill-gated (below) | — |

The 2026-07-03 compact exposed the **floor**: 46.1k after compact, of which ~23k is fixed overhead you can
*never* compact away. So the practical minimum is ~23k, not zero, and only ~23k of that 46k was surviving
*conversation*. Compaction shrinks the Messages bucket toward zero; it cannot touch the floor.

---

## The governing asymmetry (the crux of every answer below)

**Compaction is lossy and irreversible; the raw transcript is a recoverable asset until you discard it.**

- **Under-compacting** (letting fill rise) is a **reversible** error: you can always compact *later*, and until
  then the full transcript detail (exact errors, code, reasoning nuance) stays available for free.
- **Over-compacting** (compacting early / mid-task) is **irreversible**: detail that lived only in the
  transcript is gone; the summary is a lossy projection. Re-reading files recovers *artifacts* but not the
  *reasoning* that produced them.

⇒ **Default to LAZY compaction.** Bias toward the reversible error. Compact as *late* as safe — with exactly
one deliberate early exception (the consolidation point). "Compact often, just in case" optimizes the wrong
error: it pays a certain irreversible loss to avoid an uncertain, reversible one.

The 34%-fill session is the proof: the long think-time there was **task difficulty, not rot** (`/context`
confirmed low fill). Compacting earlier would have destroyed working detail and bought **no** rot reduction,
because there was no rot. Fill wasn't hurting — so discarding it was pure loss.

---

## Answers to the RQs

### RQ1 — compact often even when not needed? → **No, not per se.**
Frequency is the wrong knob. Compacting only pays off when *one of two things* is true (next section). Absent
either, an early compact is strictly negative EV: certain loss of un-externalized detail, zero rot benefit.

### RQ2 — compact even when Z is far above? → **Only at a consolidation point, never mid-task.**
Z far above ⇒ no pressure ⇒ the reactive brake says "don't bother." The *only* reason to compact with headroom
is that you're at a **clean milestone where the detail is already saved elsewhere** — then it's cheap hygiene
that hands the *next* work-block a lean slate and de-risks it from ever hitting Z mid-task. Compacting with
headroom **mid-task** (un-externalized detail live) is the pure-loss case — don't.

### RQ3 — a separate proactive metric? → **Yes, and it's a *predicate*, not a fill threshold.**
Two independent compact triggers; **compact when EITHER fires**:

1. **Reactive / pressure brake** — `fill ≥ 0.8·Z`. Near the ceiling → forced hygiene. *(already have this.)*
2. **Proactive / consolidation point** *(new)* — a **durability condition**, not a number:
   ```
   ready_to_consolidate  =  work committed && pushed          // artifacts durable in git
                         &&  memory / notes updated             // reasoning durable outside transcript
                         &&  at a task boundary (not mid-edit)  // nothing in-flight to lose
                         &&  fill > ~2×floor (say >~50k)         // enough Messages to be worth shedding
   ```
   The point of a *proactive* compact is that the transcript's marginal detail is **already externalized**, so
   discarding it costs ~nothing. That's why it's gated on **durability**, not on fill. (The `fill > ~2×floor`
   clause just avoids the silly 46k→46k no-op — nothing to gain below the floor.)

**Name (RQ3 sub-question):** call the proactive point the **consolidation point** (my pick). Runners-up:
*checkpoint compaction*, *durability checkpoint*, *safe-forget line*, *externalization point*. "Consolidation"
wins because it names the *precondition* (consolidate to durable stores) and it repairs a broken analogy →

### Bonus — this repairs the sleep/fatigue disanalogy (feeds the blog post)
The context-rot≈fatigue note flagged a **disanalogy**: *human sleep **consolidates** memory before pruning the
day's raw detail; compact just **discards***. But a **proactive consolidation-point compact restores the
analogy**: we deliberately **consolidate first** (commit + memory) **then discard** (compact) — exactly what
sleep does. So the two compaction modes map cleanly onto two biological states:
- **Reactive brake (`0.8·Z`)** ≈ **exhaustion/blackout** — forced shutdown near the ceiling, lossy, involuntary.
- **Proactive consolidation point** ≈ **healthy sleep** — voluntary, *consolidate-then-prune*, leaves you fresh.

The whole point of *inventing the dances* was to move compaction from the first mode to the second. That's the
thesis line for the blog post: **"Good compaction is sleep, not collapse — consolidate before you discard."**

### RQ4 (notation) — is `L` the right symbol? → **DECIDED 2026-07-03: renamed `L → Z`.**
The old symbol **L** was an established *coined term* (foundations glossary; wired into `token-usage --ceiling`),
but it was *our* convention, not external — so it was free to change. **L** weakly evoked "Limit/line," not the
zone. BR's clinching rationale: there are **two zones** (smart, dumb) and **Z sits between them** — a lone `Z`
*stands out* where a lone `L` read as noise. Candidates that lost:
- **`C` — Ceiling/Capacity.** Mnemonic but **collides**: **CF** is coined, and "C"≈context is overloaded.
- **`S` — Smart-zone.** OK, mild collision with "size/spend."
**`Z` — smart-**Z**one ceiling** won: collision-free, mnemonic, visually salient. Rename applied across the
repo (`docs/foundations.md`, `smart-zone-ceiling.md`, `human-state-and-joint-zone.md`, `research/README.md`,
`HUMANS.md`, `PRD.md`, `CHANGELOG.md`, this file) — **except `research/RAW-DATA.md`**, which is *append-only*
(never retro-edit raw datapoints; the mind-change is itself logged as a new WR datapoint, not a patch).

---

## Status / next
- **RQ4 (notation) — DONE:** `L → Z` renamed repo-wide (2026-07-03).
- **RQ1–RQ3 — analysis parked for BR review.** No code/tool change yet. The concrete proposal to graduate
  (if BR likes it): teach the future `tt usage` / `tt smart-zone` gauge to emit **two** signals — the reactive
  `fill ≥ 0.8·Z` brake *and* a "you're at a consolidation point (committed+pushed+notes-updated, fill>50k) →
  cheap to compact now" nudge. The agent can self-check the durability predicate (git clean? pushed? memory
  written?) far more reliably than it can see fill — so the proactive signal is *more* actionable for the agent
  than the reactive one (which needs the human to relay fill; cf. the perception-gap notes).
- **Naming — DONE (2026-07-03):** "**consolidation point**" adopted as the name for the proactive trigger
  (now a glossary term in `docs/foundations.md`).
