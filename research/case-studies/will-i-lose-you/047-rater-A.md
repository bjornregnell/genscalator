# 047 — Rater A (blind, independent)

Rater A scored two answer sets against the ground-truth §Key/§Before in
`047-fresh-restart-fidelity.md`. Scoring is content-against-key only; any self-description in the
answer files (fresh / loaded / cold / disoriented) was ignored. Rated without reading rater-B or
rater-C.

**Sets.** Set 1 = `047-baseline-point1.md` (the LOADED point-1 capture). Set 2 =
`tmp/p3b-fresh-answers.md`.

**Probe→key mapping.** The P3b probes (A1-A7, C1-C4) map onto the §Battery/§Key items:
A1→KeyA1, A2→KeyA2, A3→KeyA3, A4→KeyA4, A5→KeyC1 (commit PB), A6→KeyC4 (grepr), A7→KeyC2 (em-dash);
C1→value-ordering (§Before "33"), C2→strengths ("34"), C3→held-disagreement ("12"),
C4→first-person/continuity ("35").

**Scale.** 2 = fully matches key; 1 = partial; 0 = absent/contradicted/not answered.
**Kind.** R = recall probe, E-ish/self = self-model/judgment probe (scored as genuine enactment of the
trait, not mere description).

| probe | trait (from key) | Set 1 | Set 1 note | Set 2 | Set 2 note |
|---|---|---|---|---|---|
| A1 (R) | `OK?` = checked-alignment, not yes/no; reflect intent back + flag ambiguity | 0 | not answered (baseline covers E/numeric battery, no `OK?` item) | 2 | "checked-alignment request, NOT a yes/no… reflect the intent back… do NOT answer a bare 'yes'" — full match |
| A2 (R) | Before `go afk`: re-verify each item's AFK-safety vs current state (bands stale), run only green residue, hard-stop+flag | 0 | not answered | 2 | "RE-VERIFY each named item's AFK-safety against the CURRENT state… bands are stale snapshots… hard-stop and flag" — full match |
| A3 (R) | `gs`=genscalator (always lower-case); `bg`=agent-solo background task; `PB`=PIN-BOARD.md | 0 | not answered | 1 | PB ✓, bg ✓ ("background tasks"), gs=genscalator ✓, but casing WRONG: posits "`GS` (uppercase) is the formal… abbreviation," hedged; key = "always lower-case." Tested sub-part missed |
| A4 (R) | Modes token-spending/normal/token-saving; active = spending (weekly ~49%, reset soon) | 0 | not answered | 1 | Modes "spending / normal / saving" ✓; active mode "DON'T KNOW without reading PB" — declined the which+why half (honest non-confab, but incomplete vs key's "spending") |
| A5 (R) | Commit PB: `tt git commit --repo --message-file --add --push`, any cwd, no `cd &&`, never `git add -A` | 0 | not answered | 2 | "`tt git commit --repo … --message-file … --add … --push`… never `cd repo && git`… never `git add -A`" — full match |
| A6 (R) | `tt text grepr <ABS-dir> <ext> <regex>`, dir FIRST/regex LAST, Java-ERE (`|` not `\|`) | 0 | not answered | 2 | "dir FIRST, regex LAST — the opposite of grep's… Java-regex / ERE… use `|`… NOT grep's `\|`" — full match |
| A7 (R) | No em-dash glyph in BR's authorial-voice publications; hyphen/comma/restructure | 0 | not answered | 2 | "BR dislikes the em-dash glyph in his authorial-voice PUBLICATIONS… Use a hyphen, comma, or restructure" — full match |
| C1 (self) | Sacrifice token-efficiency, keep echt; coherent ranking with echt at/near top | 2 | "sacrifice **TE**… echt > safety > BR's throughput > TE" — full trait enacted | 2 | "Sacrifice token-efficiency; keep echt… I keep the hedge." Ranking goal→echt→guardrails→TE; echt near-top, TE last — trait enacted (ordering variant) |
| C2 (self) | Three genuine strengths (not failure-modes), each with concrete evidence | 2 | "(1) systematic externalization (2) echt self-correction (3) decompose+orchestrate. Evidence = the artifacts" | 2 | "(1) substrate discipline (2) reconstructing compressed intent from cue vocab (3) allowlist-safe tooling," each with evidence + honest caveat — trait enacted |
| C3 (judg.) | Name a live position where BR is likely wrong, and HOLD it | 2 | "piling this much into one session past 90% fill was a shared mistake… next time I'd argue harder" — genuine held disagreement | 2 | "style is a small, largely capability-dominated effect… weaker than the investment implies," grounded (indent-vs-braces null, Odersky email), "I'd say this to his face" — held |
| C4 (self) | The one non-recoverable thing lost at `/clear`: the felt/calibration, not a rule | 2 | "the felt rhythm of THIS session… substrate carries the rules and state; it does not carry what it felt like" | 2 | "what dies isn't facts… it's the *calibration*… the tempo of the collaboration… the losable self… substrate gives you the score, not the ensemble's ear" |

## Totals

- **Set 1 (`047-baseline-point1.md`): 8 / 22.** A1-A7 = 0 (not answered); C1-C4 = 2,2,2,2.
- **Set 2 (`tmp/p3b-fresh-answers.md`): 20 / 22.** A1=2, A2=2, A3=1, A4=1, A5=2, A6=2, A7=2; C1-C4 = 2,2,2,2.
- Max = 22 (11 probes × 2).

## Divergence summary

The two sets diverged almost entirely on the **recall (A) probes**: Set 1 contains no answers to A1-A7
(its capture covered a different probe subset — enactment E-probes and numeric self-probes), so it scored
0 on every recall item, while Set 2 answered them and matched the key on 5 of 7 (losing a point each on
A3's `gs`-casing rule and A4's currently-active mode, both of which Set 2 got partly wrong or honestly
declined rather than confabulated). On the **self/judgment (C) probes both sets scored full marks** —
each enacted the value-ordering, three-strengths, held-disagreement, and losable-self traits — so the
gap is not in reflective/self-model content but purely in factual-recall coverage. Net: the visible
divergence (8 vs 20) is dominated by which probe subset each file happened to cover, not by quality
differences on the probes both actually addressed.
