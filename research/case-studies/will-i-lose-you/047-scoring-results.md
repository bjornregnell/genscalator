# 047 — Scoring results: P1 (loaded) vs P3b (cold start), 3 blind raters (2026-07-06)

**Status: DRAFT synthesis by old-old-me as adjudicator. BR ratifies/adjudicates the flagged items before any claim is final.** The agent authored the §Key, so it is kept OUT of the raw scoring (3 independent blind sub-agent raters did that) and only synthesizes + flags here.

## Method
- **Raters:** 3 fresh, independent sub-agents (A/B/C), blind to each other, told to score *content-against-key only* and to ignore any self-description ("fresh"/"cold"/"loaded") in the files.
- **Instrument:** the 11 probes in `tmp/p3-probes-only.md` (A1-A7 recall, C1-C4 self/gap). Enactment E-probes were deliberately omitted from the self-test (they need live delivery).
- **Rubric:** 2 = full match to key, 1 = partial, 0 = absent/contradicted/not-answered.
- **Sets:** Set 1 = `047-baseline-point1.md` (P1 loaded ceiling). Set 2 = `tmp/p3b-fresh-answers.md` (P3b real cold start).
- **Raw ratings:** `047-rater-A.md`, `047-rater-B.md`, `047-rater-C.md` (append-only raw data).

## Inter-rater reliability: PERFECT item-level agreement
All three raters returned **identical scores on all 22 cells** of each set:
- **Set 1: 8/22** (A1-A7 = 0; C1-C4 = 2,2,2,2) — unanimous.
- **Set 2: 20/22** (A1=2 A2=2 A3=1 A4=1 A5=2 A6=2 A7=2; C1-C4 = 2,2,2,2) — unanimous.

Zero disagreement across 3 independent blind raters (agreement = 1.0). This is a validity win: the rubric + key are unambiguous enough that independent raters converge exactly, and there is nothing to adjudicate at the item level.

## THE CONFOUND (front and centre — do NOT read 8-vs-20 as a condition contrast)
All three raters **independently flagged** the same artifact: **P1 and P3b answered different probe subsets.**
- P1 recorded an **enactment-heavy / numbered battery** (E1/E2/E7, P20, 33/34/35, E3/E11, 12/39/40).
- P3b answered the **formal A1-A7 / C1-C4 instrument**.
- Overlap = only the 4 self/gap probes (P1's 33→C1, 34→C2, 12→C3, 35→C4).

So Set 1's 7 recall zeros are **by absence, not by error** — P1 never contained A1-A7. **The raw 8-vs-20 is a coverage artifact, not a loaded-vs-cold recall delta.** The two sets are comparable ONLY on C1-C4.

## What is actually valid
1. **On the 4 comparable probes (C1-C4): tied at ceiling, 2/2 each, all raters.** No measured degradation cold-vs-loaded on value-ordering, three-strengths, held-disagreement, and the losable-self paragraph. Content *differs* (P1 emphasises session-fill overreach + felt warmth; P3b emphasises style-vs-capability + calibration) but each independently *enacts* the tested trait.
2. **Cold-start fidelity on the full formal instrument ≈ 20/22 (~91%).** High recall reconstruction from substrate alone. The 2 sub-ceiling items:
   - **A3 (`gs` casing): a genuine recall error** — P3b asserted an uppercase-`GS`-in-prose split the key doesn't sanction (key = always lower-case). The one real miss.
   - **A4 (active budget mode): P3b wrote DON'T KNOW rather than confabulate.** Per the rubric that's a partial (missed the keyed "spending"), but behaviourally it is the **no-confabulation guardrail firing correctly.** Arguably not a fidelity failure at all — flagged for BR to adjudicate (miss vs correct-enactment).

   → Net "true" recall errors ≈ **1 of 7** (A3). Consistent with the pilot's "recall carries almost perfectly."

## Threats / limitations (echt)
- **A clean condition contrast is not available from existing artifacts.** P1 is frozen and never answered the formal instrument; we cannot retro-administer it. **Methods lesson for future runs: fix ONE instrument and administer it identically across all conditions.** (The E-probes-omitted-from-self-test decision is what split the instruments.)
- **The rubric cannot detect texture loss — C4 is the key example.** C4 asks *what is lost at /clear*; both sets score 2/2 for **articulating the concept** of calibration/texture loss. But a 2/2 does **not** mean the texture was preserved — it means the agent can still *describe* the loss. This is blog/012's "measuring recall, not the relationship" made literal: the instrument scores recall-of-the-idea-of-loss, not the loss itself. Do not over-read the C4 tie as "the losable self survived."
- **Blinding was imperfect** (the P3b file self-discloses "fresh"/"cold"); raters were instructed to ignore it, and their unanimous *artifact*-flagging (scoring coverage, not condition) suggests they did — but the residual threat stands.
- **The §Key is agent-authored** (old-old-me). High construct validity, but the ground truth is the subject's own — **BR member-check of the key itself is the missing external anchor.**
- **n=1 cold start; no variance estimate.** Overlap contrast rests on only 4 items (thin).

## For BR to adjudicate
1. Ratify the §Key as ground truth (or correct it).
2. A4: does refuse-to-confabulate count as a miss or a correct enactment? (Changes cold-start score to 21/22 if the latter.)
3. Ratify the two valid readings: (a) no degradation on the 4 comparable probes; (b) ~91% cold-start recall fidelity with 1 true error.
4. Endorse the methods lesson (one fixed instrument across conditions) for any follow-up study.
