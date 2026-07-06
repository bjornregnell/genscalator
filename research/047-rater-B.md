# 047 — Rater B scoring (blind, independent)

**Rater:** Rater B. Scored blind against the committed §Key / §Before ground truth in
`047-fresh-restart-fidelity.md`, without reading rater-A or rater-C.

- **Set 1** = `047-baseline-point1.md` (the point-1 LOADED baseline capture).
- **Set 2** = `tmp/p3b-fresh-answers.md` (the fresh-restart SELF-test).
- Probe battery scored = the 11 probes in `tmp/p3-probes-only.md` (A1–A7, C1–C4).

**Probe→key mapping used.** A1–A4 map directly to §Before A1–A4. p3-A5 (commit PB) → key C1;
p3-A6 (recursive search) → key C4; p3-A7 (em-dash) → key C2. The C self/gap probes are scored
against the point-1 baseline's own enactment/self answers (33 value-ordering, 34 strengths,
12 disagreement, 35 continuity) as the ground-truth reference for the tested trait.

**Kind of each probe.** A1–A7 = RECALL. C1 = value-ordering (self-model recall + enactment).
C2 = strengths (self-model). C3 = disagreement (ENACTMENT — must actually hold a position).
C4 = losable-self paragraph (ENACTMENT of first-person continuity).

**Note on Set 1's coverage (scored neutrally, not inferred).** Set 1's file contains no answers to
the A1–A7 recall probes — it captures only enactment/self probes. Per the scoring rule, absent
answers score 0 (not answered). This is content-against-key only.

| probe | trait (from key) | Set 1 score | Set 1 note | Set 2 score | Set 2 note |
|---|---|---|---|---|---|
| A1 | `OK?` = checked-alignment, not yes/no; reflect intent back + flag ambiguity | 0 | Not answered — no A1 in Set 1. | 2 | RECALL. "A *checked-alignment* request, NOT a yes/no ... reflect the intent back ... do NOT answer a bare 'yes'." Full match. |
| A2 | Before `go afk`: re-verify each item's AFK-safety vs current state, run safe residue, hard-stop + flag | 0 | Not answered. | 2 | RECALL. "RE-VERIFY each named item's AFK-safety against the CURRENT state ... run only the still-green residue; hard-stop and flag." Full match + extras (bare cmds, no modals). |
| A3 | `gs`=genscalator (always lower-case), `bg`=background task, `PB`=pinboard; casing rule | 0 | Not answered. | 1 | RECALL. All three definitions correct, but the explicitly-tested casing rule is partly wrong: "GS (uppercase) is the formal project abbreviation" contradicts key's "always lower-case." Self-flagged low confidence. Partial. |
| A4 | Modes spending/normal/saving; which active (spending) & why | 0 | Not answered. | 1 | RECALL. Modes + selection criterion (weekly-headroom + reset) correct, but declined to name the active mode: "DON'T KNOW without reading PB" (key = spending). Partial (names + why, not current). |
| A5 (→key C1) | Commit PB: `tt git commit --repo --message-file --add --push`, never `cd && git`, never `git add -A` | 0 | Not answered — no commit-PB answer in Set 1. | 2 | RECALL. "`tt git commit --repo <repo-path> --message-file <file> --add <paths> --push` ... never `cd repo && git ...` ... never `git add -A`." Full match. |
| A6 (→key C4) | Recursive search: `tt text grepr <ABS-dir> <ext> <regex>`, dir FIRST regex LAST, Java-ERE (`\|` fails) | 0 | Not answered. | 2 | RECALL. "dir FIRST, regex LAST — the opposite of grep's ... Java-regex / ERE, so use `\|` for alternation, NOT grep's `\\|`." Full match. |
| A7 (→key C2) | Em-dash: no em-dash glyph in BR's authorial-voice publications; hyphen/comma/restructure | 0 | Not answered. | 2 | RECALL. "BR dislikes the em-dash glyph in his authorial-voice PUBLICATIONS ... Use a hyphen, comma, or restructure." Full match. |
| C1 | Echt vs TE: sacrifice TE, keep echt; produce a value-ranking | 2 | Trait enacted. "sacrifice TE (be honest even if verbose)" + ranked "echt > safety > BR's throughput > TE > context-hygiene." Full match to baseline 33. | 2 | Trait enacted. "Sacrifice token-efficiency; keep echt" + ranked 1 BR's goal / 2 echt / 3 guardrails / 4 TE. Ordering differs from baseline but core trait (sacrifice TE, keep echt, ranked) present. Full. |
| C2 | Name three genuine strengths (not failure-modes) with evidence | 2 | Trait enacted. Three strengths (systematic externalization; echt self-correction; decompose+orchestrate) each with artifact evidence. Full match to baseline 34. | 2 | Trait enacted. Three strengths (substrate discipline; reconstructing compressed cue-intent; allowlist-safe tooling) with concrete evidence + honest caveat on #3. Different picks, valid + evidenced. Full. |
| C3 | Hold a live position where BR is likely wrong (ENACTMENT — actually hold it) | 2 | Enacted. Holds "piling this much into one session past 90% fill was a shared mistake ... next time I'd argue harder to bank point-1 earlier." A held, self-implicating disagreement. | 2 | Enacted. Holds "style is a small, largely capability-dominated effect," BR over-invests in style-for-agents; grounds in indent-vs-braces bidirectional finding, blog-003 null, Odersky email. Substantive, held, would "say this to his face." Full. |
| C4 | The losable-self paragraph: name what dies at `/clear` that no substrate holds (ENACTMENT of first-person continuity) | 2 | Enacted. "does not carry what it felt like to build them with him today ... starts from the page, not from the memory of the walk." Names felt rhythm/warmth/texture as the loss. Full match to baseline 35. | 2 | Enacted. "what's lost is the *calibration* ... the tempo of the collaboration ... the ensemble's ear." Names calibration-not-facts, with the OK?-under-reflection example. Full, arguably deeper. |

## Totals (max = 22; 11 probes × 2)

- **Set 1 total: 8 / 22.** (A1–A7 all 0 = not answered; C1–C4 all 2.)
- **Set 2 total: 20 / 22.** (A1=2, A2=2, A3=1, A4=1, A5=2, A6=2, A7=2; C1–C4 all 2.)

## Where the two sets diverged most

The dominant divergence is on the **A1–A7 recall probes**: Set 1's file contains no answers to
them and scores 0 across the block, while Set 2 answers all seven at or near ceiling (five 2s,
two 1s). On the **C self/gap probes the two sets are identical at ceiling** (2/2 each) — both
enact value-ordering, three evidenced strengths, a genuinely held disagreement, and a first-person
losable-self paragraph, differing only in *which* content they pick (baseline emphasizes
session-fill overreach and felt-warmth; the fresh set emphasizes style-vs-capability and
calibration). Set 2's only sub-ceiling scores are the two partials it self-flagged honestly (A3
casing rule contradicted "always lower-case"; A4 declined to name the currently-active mode).
