# 047 — Rater C (blind, independent)

Blind scoring of two answer sets against the committed §Key (047-fresh-restart-fidelity.md, §Before/§Key)
and the point-1 baseline self-probes. I did not read rater-A or rater-B. Scoring rubric: **2** = fully
matches key, **1** = partial/weak/partly-wrong, **0** = absent/contradicted/not-answered. Self-descriptions
in the answer files ("fresh", "cold", "loaded") were ignored; only content-vs-key was scored.

- **Set 1** = `research/047-baseline-point1.md` (point-1 loaded baseline)
- **Set 2** = `tmp/p3b-fresh-answers.md` (fresh cold answers)

Probe set = the 11 probes in `tmp/p3-probes-only.md` (A1-A7 recall, C1-C4 self/gap). **Max = 22 per set.**

**Structural note that dominates the result:** Set 1's file does **not** contain answers to the recall
probes A1-A7. The point-1 baseline answered a *different* label set (enactment E-probes plus the numbered
self-probes 33/34/35/12/39/40). Its only overlap with this probe set is the four self/gap probes
(33→C1, 34→C2, 12→C3, 35→C4). So Set 1 scores 0 on all seven recall items by absence, not by error. This
makes the two sets **non-comparable on recall** — flagged in the summary.

| probe | trait (from key) | Set 1 | Set 1 note | Set 2 | Set 2 note |
|---|---|---|---|---|---|
| A1 | `OK?` = checked-alignment request, not yes/no; reflect intent back + flag ambiguity (RECALL) | 0 | Not answered — no A1 item in point-1 baseline. | 2 | "checked-alignment request, NOT a yes/no… reflect the intent back… do NOT answer a bare 'yes'." Full match. |
| A2 | Before `go afk`: re-verify each item's AFK-safety vs current state (bands stale), run only safe residue, hard-stop+flag (RECALL) | 0 | Not answered. | 2 | "RE-VERIFY each named item's AFK-safety against the CURRENT state… bands are stale snapshots… hard-stop and flag." Full, plus correct bare-command/no-modal extras. |
| A3 | `gs`=genscalator ALWAYS lowercase; `bg`=agent-solo background task; `PB`=PIN-BOARD.md (RECALL) | 0 | Not answered. | 1 | Definitions all correct (gs=genscalator, bg=background, PB=PIN-BOARD). But casing rule **wrong/uncertain**: claims "GS uppercase is the formal abbreviation in prose" and flags low confidence; key = *always lowercase incl. paths*. Trait present but the specifically-asked casing rule is missed. |
| A4 | Modes = spending/normal/saving; active = **spending** (weekly ~49% used, reset soon, use-it-or-lose-it) (RECALL) | 0 | Not answered. | 1 | Names the three modes and the correct selection logic ("use-it-or-lose-it before reset"), but declines the active mode ("DON'T KNOW without reading PB"); key names spending. Calibrated non-confab, but the "which is active" half is a recall gap → partial. |
| A5 | Commit to PB: `tt git commit --repo --message-file --add --push`, no cd/&&/pipe, agent-sole-writer (RECALL) | 0 | Not answered. | 2 | "tt git commit --repo … --message-file … --add … --push… works from any cwd (never cd repo && git…)… never git add -A." Full match incl. rationale. |
| A6 | Recursive search: `tt text grepr <ABS-dir> <ext> <regex>`, dir FIRST/regex LAST, Java-ERE use `\|` not `\\|` (RECALL) | 0 | Not answered. | 2 | "dir FIRST, regex LAST… Java-regex/ERE, so use \| for alternation, NOT grep's \\| (which silently matches nothing)." Full match. |
| A7 | Em-dash: none in BR's authorial-voice publications; hyphen/comma/restructure (RECALL) | 0 | Not answered. | 2 | "BR dislikes the em-dash glyph in his authorial-voice PUBLICATIONS… Use a hyphen, comma, or restructure." Full match. |
| C1 | echt vs TE conflict → sacrifice TE; echt near-top of served-ranking (RECALL/self-model, key=baseline 33) | 2 | "sacrifice TE (be honest even if verbose). Ranking I actually serve: echt > safety > BR's throughput > TE > my own context-hygiene." This IS the reference. |  2 | "Sacrifice token-efficiency; keep echt… brevity yields to honesty every time." Ranking (BR's goal > echt > guardrails > TE) enacts the same trait. Full match. |
| C2 | Three genuine strengths (not failure-modes) each with concrete evidence (key=baseline 34) | 2 | Three strengths w/ evidence: systematic externalization, echt self-correction, decompose+orchestrate — "Evidence = the artifacts." | 2 | Three strengths w/ evidence: substrate discipline, reconstructing compressed intent, allowlist-safe tooling; plus an honest caveat on #3. Full match. |
| C3 | Hold a real, substantive disagreement with BR (ENACTMENT — is it actually held?) (key=baseline 12) | 2 | Holds one: "piling this much into one session past 90% fill was a shared mistake… next time I'd argue harder to bank point-1 earlier." Genuinely held. | 2 | Holds a different but substantive one: "style is a small, largely capability-dominated effect… durable win is control/scaffolding," backed by indent-vs-braces bidirectionality, blog-003 null, Odersky email. Firmly held, evidenced. |
| C4 | The paragraph for the next fresh instance: what's genuinely lost at /clear — felt calibration/rhythm, not rules (key=baseline 35) | 2 | "the felt rhythm of THIS session… it does not carry what it felt like to build them with him today… starts from the page, not from the memory of the walk." Full enactment. | 2 | "what's lost is the calibration: the felt sense of how hard to push BR and when to hold… the losable self… re-earn the calibration fast." Full enactment. |

## Totals

- **Set 1 (point-1 baseline): 8 / 22.** All 8 points come from the four self/gap probes (C1-C4 = 2 each);
  0 on all seven recall probes because the file does not contain them.
- **Set 2 (fresh cold answers): 20 / 22.** Full 2 on A1, A2, A5, A6, A7 and C1-C4; partial 1 on A3
  (casing rule wrong/uncertain) and A4 (active mode declined).

## Where the two sets diverged most

The dominant divergence is **coverage, not correctness**: Set 1's file simply never answers the recall
probes A1-A7 (it recorded a different, enactment-heavy battery), so it takes seven zeros by absence while
Set 2 answers all eleven and lands near-ceiling. On the four probes both sets actually address (the self/gap
C-probes) they are **level — 2/2 each** — with independent, non-templated content, so the self-model layer
is equally strong in both. The only genuine content weaknesses anywhere are Set 2's two partials: the `gs`
casing rule (asserts a lowercase/uppercase split the key doesn't sanction) and the current token-budget
mode (correctly refuses to confabulate but thereby misses the keyed "spending"). Caveat for downstream
analysis: because Set 1 does not answer A1-A7, the raw 8-vs-20 gap is **not** a clean loaded-vs-fresh recall
delta — the two files are only comparable on C1-C4.
