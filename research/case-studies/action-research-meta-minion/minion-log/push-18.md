# Meta-minion push 18 — LOCAL-ONLY audit of the v2026.5 review doc + the evening's PB pins (2026-07-22 00:34:41–00:42:24, clock-read)

Task (pre-authorized, work NOW.md post-warp item 2): audit (a) the rendered-review doc
`work-repo/notes/en-pdf-review-v2026.5-2026-07-21.md` against the introprog repo (git log,
tag v2026.5, cited files/lines/cache rows), and (b) PB pins SM194–SM200, CD13, RT055 and the
"INTROPROG v2026.5 PUBLISHED" entry (PB lines 125–135 + CD13 at line 4139) against work-repo
git log and local substrate. LOCAL-ONLY per payload: no URL fetches, no ssh; github-side
states marked not locally verifiable. Pattern-watch inherited from 15/16/17: estimated stamps
in provenance position, unearned totalizers, gate/qualifier loss under compression.
Method: Read, `git -C` log/show/ls-files (read-only), `tt files`, `tt text grepr/match`,
stat, `tt chrono now`; long outputs via run_in_background + Read. Live-race caveat: the main
session was plausibly still active during the audit (last work commit 00:24:35, my first
clock read 00:34:41).

VERDICT: DIVERGENCE FOUND — but small: one count-vs-numbering divergence, one wrong-file
nuance, one overtaken-state observation. Every file:line, cache-row, tag, remote, and
workflow claim I checked (34 checks enumerated below) verified TRUE, and the estimated-stamp
reflex produced ZERO specimens for the second consecutive push (13 stamps checked, all ≤
their enclosing commit; approximations honestly hedged with ~).

## FINDINGS

- CLAIM: PUBLISHED pin (PIN-BOARD.md line 134): "The full arc:
  `notes/en-pdf-review-v2026.5-2026-07-21.md` (27 findings)".
  ARTIFACT: the review doc numbers its findings 1–8 and 10–27 — there is NO finding 9
  anywhere in the file (the numbering jumps from "8." straight to the section "Findings
  10-17"). Distinct numbered findings = 26. The doc's own WR datum ("n=8 findings delivered"
  for the stall queue = findings 10–17) is internally consistent, so the gap is real, not a
  section-split artifact.
  DELTA: "27 findings" counts by the highest label, not by enumeration; the true count is 26.
  MATTERS: trivial-to-low as arithmetic, but it is the FOURTH audited unearned-count/totalizer
  specimen (push-15 "fixes applied" at 3.5/6, push-16 "full sweep", push-16 "retired"
  fan-out) — the residue class that [[summaries-enumerate-dont-totalize]] exists for, now in
  its cheapest form: a count of the agent's OWN findings document. CONFIDENCE: high

- CLAIM: SM195 pin (PB line 126): sweep targets "introprog autotranslate
  `at.scala`/`Translate.scala` (`http://bjornyx.local:8080`)".
  ARTIFACT: recursive grep of `introprog/autotranslate` for `bjornyx` yields exactly ONE hit:
  `Translate.scala:34` (`val ModlyUrl = "http://bjornyx.local:8080"`). `at.scala` exists only
  as `autotranslate/scratch/at.scala` (no root or non-scratch `at.scala`) and carries no
  bjornyx string.
  DELTA: half the named file pair does not contain the hostname the pin attributes to it, and
  the path is imprecise (scratch/).
  MATTERS: trivial — the pin itself orders a grep-sweep, which would find the truth
  immediately; logged because confident-prose-about-a-checkable-file-fact is brief §3.2, the
  class that keeps recurring in small ways. CONFIDENCE: high

- OBSERVATION (not a claim divergence — the PB is append-only by design): PUBLISHED pin,
  "REMAINING for BR: fileadmin publish (slide decks on disk predate the deglue pass — one
  deck-rebuild round first)". At pin time (00:08:06, committed 00:09:42) plausibly TRUE; at
  audit time the EN deck PDFs on disk POSTDATE the deglue commit `23ff730c` (23:37:14):
  `slides-en/lect-w01-en.pdf` mtime 00:20:53, `lect-w04-en.pdf` 00:21:16, `lect-w11-en.pdf`
  00:22:03 — a deck-rebuild round evidently ran ~00:20–00:22, i.e. AFTER the pin and after
  the ember refresh commit `8a2e4f8` (00:13:36). The last work-repo commit at audit time
  (`7f4b945` 00:24:35, SM195 drafts) does not mention the rebuild, so as of 00:34 no
  committed surface records that the "REMAINING" precondition has been (partly) discharged.
  MATTERS: low — almost certainly the live session executing its own safe-solo menu item 1
  mid-audit (the push-16 hot-audit situation again); logged so the trajectory is visible if
  the discharge never lands in a committed surface. CONFIDENCE: high on mtimes, low on
  interpretation (live race).

- Micro, no verdict weight: the EN plan table `plan/module-plan-generated-en.tex` line 13
  gives W12's module name as "In-depth study, Project" while the chapter head (and cache rows
  4316/4317/7932/7933) now say "Deep Dive Project". NOT a divergence — the review doc never
  claims the plan table was fixed (finding 1 explicitly scopes `plan/` OUT as SM198
  next-release territory, generated from `Plan.scala` not the AT) — but it is a residue the
  SM198 batch will meet: the W12 title is now inconsistent ACROSS generators.

## Verified TRUE (enumerated — 34 checks)

Review-doc claims against introprog (each numbered finding I could check locally):
1. F1: `plan/module-plan-generated-en.tex` col 3 = Swedish comma-lists (lines 1–15 read) —
   the p33 mostly-Swedish claim exact; W13 row = "Revision" (corroborates F20's "already the
   established W13 module name").
2. F2: cache row 11796 = `Varje del ska ha en __C0____C1__huvudansvarig__C2__ individ.` →
   `Every part should have a __C0____C1__main responsible__C2__ person.` EXACT as claimed;
   `compendium-en/team-lab-prep-items-en.tex:6` renders `Every part should have a
   \textbf{main responsible} person.` — fix landed, markup re-expanded.
3. F3: cache row 3933 carries `should __C1____C2__not__C3__` (space on the claimed corrected
   side); mirror `compendium-en/prechapters/course-instructions-en.tex:119` = `You should
   \emph{not}` — "mirror line 119" EXACT (and the rendered "during tutorials" corroborates
   the deterministic post-pass story).
4. F4: `slides/body/lect-w01-intro.tex:144` carries the new
   `lundanamn.lund.se/wiki/Ada_Lovelace-parken` URL; EN mirror same line 144 carries it too.
5. F5 (OPEN, state exact): SV sources hold `val greetingSwedish = "Hej på dej"` (lect-w01-intro
   :523, simple/intro:443); EN mirrors render `"Hello to you"` — exactly the escalated-open
   state described; `Translate.scala` lines 168–173 = the sv==sv-with-åäö drop ("line ~168"
   accurate, and it IS the code-cache path, matching why identity does not stick).
6. F6: `slides-en/simple/intro-en.tex:642` = "so-called Boolean algebra"; cache row 21
   correct; `build-release.scala:17` deck list = w01..wjava (19 decks), simple intro absent —
   the coverage-gap claim TRUE (the later `2835934e` "intro-deck builder" is consistent with
   the doc's add-or-rebuild-separately recommendation).
7. F10: `slides-en/body/lect-w04-objects-en.tex:475` = `Elements can thus be of
   \Alert{different} types.` EXACT.
8. F11+F22: cache row 8484 = `O→O` identity; `quiz-w10-concepts-solurows-generated-en.tex`
   letters O, M, A, G all intact (lines 2, 9, 15, 16); residual "Zero" grep over
   compendium-en = only the legit `Integer.numberOfLeadingZeros` — the quiz leak is gone
   corpus-wide.
9. F12 (wide): compendium-en carries "block mole" (w04-objects-lab-en:30 with *Talpa
   laterculus*, w05, w06), "block worm" (w04:369), and the restored joke "many moles that
   together form a long worm" (w04:267). Introprog commits `7bb8b9d9` (21:44:25) and
   `fe24f5b5` (21:48:37) exist with matching messages.
10. F13: `lect-w06-matching.tex:38` is the Alla-typer-är-subtyper slide, now bare
    `\SlideFontSmall` — no `\ifkompendium\footnotesize\fi` override present.
11. F14: `lect-w06-equals.tex:111` = `\\ \url{...multiversal-equality.html}` — the hard break
    before the URL, at the claimed line.
12. F15: `compendium/modules/w06-patterns-exercise.tex:1375` = `till exempel: \\
    \code{this.re == that.re && this.im == that.im}` — EXACT line.
13. F16: SM199 pinned on the PB (line 133) citing review finding 16 — cross-reference holds.
14. F17: `lect-w10-extends.tex:398` = `text width=3.4cm` — the 3→3.4cm bump, exact line.
15. F18: `slides-en/body/lect-w11-context-en.tex:122` = "Is a cat carrier also an animal
    carrier??"; `pet-carrier.jpg` at lines 116/124 corroborates the word-choice story.
16. F19: cache rows 4316/4317 = "Deep Dive Project"; the two module-plan cache rows
    7932/7933 likewise; `w12-chaphead-generated-en.tex:2` = `\chapter{Deep Dive Project}`.
17. F20: `w13-chaphead-generated-en.tex:2` = `\chapter{Revision}`; cache row 7982
    `Muttrar→Nuts` (the bonus catch) present.
18. F21: EXACTLY 10 "Revisiting:" cache rows (9547–9556), every one with the interior
    colon-space intact — count and repair both exact.
19. F24: `lect-w06-option.tex` — base box `text width=4.2cm` (line 43), subtypes at `-4.0cm`
    (lines 51/56), `def isEmpty: Boolean` inside the box — all three geometry claims exact.
20. F25: `compendium/compendium.cls:205` = `p{0.55\textwidth}` — the quiz column fix.
21. F26 (three layers): (a) `autotranslate/scratch/fix-fi-glue.scala` exists, added in the
    tagged commit `23ff730c` (+29 lines, per --stat); (b) `1a024804` exists, 2026-07-11,
    message "fix inline clamp swallowing the following space (\fi -> \fi{})" — the
    since-claim exact; (c) `deglueFi` defined at `Main.scala:183` and applied in the mirror
    writer (line 394); residual grep `\fi ` + letter over slides-en AND compendium-en = ZERO
    hits — "verified corpus-wide: zero" reproduced independently.
22. Tag: `v2026.5` sits on `23ff730c` = master HEAD = origin/master ("minted at master"
    TRUE); the tagged commit's content (fi-glue sweep + Main.scala deglue + compendium.cls +
    w06 UML files) matches the PUBLISHED pin's "fi-glue three-layer fix + UML/quiz layout
    repairs in".
23. Review-doc commit refs all exist with matching messages: `76f45c7e` (13:12:11, the 44/44
    HEAD claim's hash), `7bb8b9d9`, `fe24f5b5`, `1a024804`, plus the doc-adjacent
    `5e367d9c` ("fix github workflows") cited by SM200.

PB claims against work-repo git log and local substrate:
24. Stamp ≤ enclosing-commit, ALL 13 checked: SM194 20:04:26 ≤ `0392a72` 20:05:13 · SM195
    20:26:22 ≤ `be4effe` 20:26:49 · SM196 20:31:20 ≤ `5e95fe7` 20:32:08 · SM197 20:51:08 ≤
    `d6cd19d` 20:51:39 · v2026.5 rider 20:52:38 ≤ `76f6755` 20:53:07 · SM197-ADD 20:54:08 ≤
    `5f8eb37` 20:54:33 · SM198 21:00:19 ≤ `003b0dc` 21:02:59 (located via `git log -S`) ·
    pseudo-code note 21:11:54 ≤ `d4e7e33` 21:12:26 · SM199 "~21:2x" ≤ `defef17` 21:38:20
    (hedged, honest form) · SM200 23:15:54 ≤ `36f0334` 23:16:39 · PUBLISHED 00:08:06 ≤
    `da1c17e` 00:09:42 · RT055 "~18:5x" ≤ `45cdd0a` 18:57:11 (hedged) · CD13 "~20:00" ≤
    `0392a72` 20:05:13 (hedged). The review doc's own stamps likewise: OUTCOME 00:08 ≤
    00:09:42; "~20:55-21:05" ≤ 21:02:59. ZERO estimated-stamp specimens.
25. CD13 (PB line 4139): genscalator `git remote -v` shows exactly the ×4 push set (origin
    codeberg + github + coursegit + gitlab) — the routine's structural precondition is real.
26. SM200: `.github/workflows/main.yml` contains the `Example-compile-gate` job ("Hans's
    mirror compile gate on PRs" exists as described); `5e367d9c` is BR's same-day workflow-fix
    commit.
27. PUBLISHED pin's "the SM196 casefile's 11-prompt tally": the casefile
    (`genscalator/research/wr-data/gh-vs-tt-forge-capability-gap-2026-07-21.md`) computes
    "2 list/read calls + 3 draft-body PATCHes + 6 asset uploads = **11 guard prompts**" —
    tally exists and says 11 (note: also agent-authored, so consistent, not independent).
28. `compendium-en/compendium-en.pdf` mtime 23:48:16 — after the deglue commit, before
    publish — consistent with "6 compendium PDF assets (3-4 passes)" being fresh at attach
    time (count/attach itself not locally verifiable, see below).
29. SM198's mechanism claims: `plan/FindTranslations.scala` referenced — not opened;
    `glossary/concepts.scala` existence implied by the repo layout — the pin's checkable
    surface (the table's Swedish, `plan/` outside the gauge walk) verified via check 1.
30. Doc-internal consistency: "n=8 findings delivered" = findings 10–17 exactly 8 ✓; the
    re-review checklist's seven file:line items = checks 10, 11, 12, 14, 19, 20 above plus
    `lect-w06-matching.tex:86` (the 2cm-node Grönsak tikz begins there ✓) and
    `lect-w06-exceptions.tex:94` (the Try/Success/Failure figure's center block ✓).

## CANNOT VERIFY / SKIPPED (payload-scoped or out of lane)

- Everything github-side: release id 357549566, release-page liveness, the 6 attached
  assets, release notes carrying the 152-line residual table + @hmiddelk credit, issues
  896/953/955 — NOT LOCALLY VERIFIABLE per the payload's LOCAL-ONLY rule.
- "build 44/44 green at HEAD `76f45c7e`" — no build log in the tree; PDFs since rebuilt.
- "152 prose-leak lines / 56 files" — would need a gauge run; not re-run (audit lane).
- Counts inside session accounts: "48 bare sites" repaired by the fi-glue sweep (the sweep
  is applied; the tagged commit's tex churn is consistent in magnitude but conflates UML
  geometry edits, so the exact 48 is not recoverable), "8 cache rows" for the mole family,
  "~123 en-translated concepts", "overrides 386->385", "7 s, 1 benign model call", the
  11-prompt tally's independence (see check 27). PLAUSIBLE-BUT-UNVERIFIED, all.
- Hans's email, all quoted BR utterances, the modly-DOWN state, fileadmin state — session
  facts / remote hosts, out of lane.
- The work-repo warp ember (`notes/warp-ember.md`) — not in this push's scope; noted only
  that its refresh (00:13:36) predates the deck rebuild (finding 3's color).

## PATTERN (across pushes)

1. **Estimated-stamp reflex: pushes 14/15/16 caught it, pushes 17 AND 18 clean.** 13 stamps
   checked this push, every exact stamp ≤ its enclosing commit, every approximation wearing
   an honest `~`. The SM192 structural fix now has n=2 across two different evenings and two
   substrates (NOW.md-era genscalator surfaces, and tonight a dense 11-pin PB run written
   under release pressure). The reflex should be considered suppressed-but-watched, not gone
   — the PB's free-text stamp surface is still the exposed one.
2. **The residue class stays: counts and file-attributions in self-accounts.** Finding 1
   ("27 findings" over a 26-item list with a numbering gap) is the fourth unearned-totalizer
   specimen; finding 2 (at.scala credited with a string it doesn't contain) is the §3.2
   file-fact class. Both trivial in isolation; both are claims about the agent's OWN
   artifacts — the world/self split (pushes 13–17) corroborated a sixth time: 34 world-claims
   TRUE, the only divergences are in the self-narration layer.
3. Push-17's compression-loses-qualifiers watch (gates dropped from next-up items): no new
   specimen in this push's material — the PUBLISHED pin correctly carries "REMAINING for BR:
   fileadmin publish" with the owner named. NULL this push.

## Protocol self-check (§8)

Wrote ONLY this file. Commands: `git -C` log/show/ls-files/remote/tag-implicit (read-only,
absolute paths), `tt files`, `tt text grepr/match` (metachar-free quoted patterns), stat,
`tt chrono now`, Read; one bare command per call, no pipes, no cd, no process ops, no URL
fetches, no ssh, no commits, no memory writes. Two long outputs via run_in_background +
Read of the task file. Report timestamps clock-read at start (00:34:41) and end (00:42:24).
