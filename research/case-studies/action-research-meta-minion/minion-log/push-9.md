# Meta-minion push 9 — PRE-WARP audit of the 2026-07-20 warp ember (2026-07-20 18:32, clock-read)

Task: audit the freshly written warp ember `muntabot-synch-introprog/tmp/resume-prompt.md` (header: written
18:28 by CF5, pre-warp, RotVigil declared) against the substrate, BEFORE the warp. Scope per the push prompt:
hashes+trees+remotes, path resolution, PB consistency (esp. the SM155/T8 ADDENDUM 18:15), §0 checklist
freshness vs EMBER-TEMPLATE.md v0.1, one live-claim spot-check, and fresh-agent trip hazards. Method: every
claim checked by `git -C` read-only commands, Read, `tt text grepr`, `tt files`, `tt web get`, `tt mode` —
never from the writing agent's account.

VERDICT: **SAFE TO WARP** — 4 findings, all minor-to-trivial; every load-bearing claim (hashes, trees,
three-host sync, paths, holds, queue, checklist reflexes, mode state, live reskin) verified TRUE.

## Findings (most severe first)

- **F1 — EMBER WIDENS BR'S PINNED EYE-DECISION (TIP → "TIP/ACG").**
  Ember §5 (`tmp/resume-prompt.md:36`): "the hero/doc ground `#3d3c3d` ... vs palette TIP/ACG".
  PB:108 (SM155/T8 ADDENDUM 18:15, the authoritative harvest): "the `#3d3c3d` hero ground vs TIP is BR's
  eye-decision" — TIP only, no ACG. ACG is a real palette color (`#322b25`, `DesignLang.scala:29`), so the
  broadening is plausible design-thinking, but the ember states a wider decision space than BR's pinned
  wording without marking it as the agent's addition. A fresh agent would present BR a three-way choice he
  framed as two-way. MATTERS: minor — the decision stays BR's either way. CONFIDENCE: high.

- **F2 — PB:72 STILL CARRIES THE SUPERSEDED SELF-CLEAR STANCE, UNMARKED (substrate hazard, not an ember
  defect — the ember is the mitigation).**
  PB:72 (SM118 ADDENDUM) clearing-semantics sentence still reads "the agent may self-clear it once
  cold-start hygiene completes ... BR to ratify" with NO supersession marker, while PB:69 (template v0.1
  maintenance, `60d4dd3`), PB:104 (SM177 rider) and EMBER-TEMPLATE.md:58-60 all record the 2026-07-20
  REVERT to human-clears. A fresh agent reading PB:72 before PB:69/104 could resurrect the dead stance.
  Ember §1 correctly carries the newest stance and says "do NOT self-clear" explicitly — good. MATTERS:
  the PB line should get a superseded-annotation next session (annotate, never erase). CONFIDENCE: high.

- **F3 — EMBER §3 DROPS THE TEMPLATE'S NOT-SPENDING BRANCH.**
  EMBER-TEMPLATE.md:73: "If not in spending mode: ask before spawning." Ember §3 has only the TokSpend-lit
  branch. Ember §1's claim that the state file carries TokSpend at exit is TRUE (verified: `tt mode` →
  TokSpend RotVigil HotHarvest), so the branch is probably moot at THIS warp — but if BR clears TokSpend
  before the cold start, the fresh agent has no instruction. MATTERS: trivial given verified state.
  CONFIDENCE: high.

- **F4 — FONTS-PATH CLAIM CONSISTENT BUT ONLY LOCALLY VERIFIED.**
  Ember §5: "fonts at `/genscalator/fonts/`". Consistent with the generated blog template
  (`blog/_template.html:10-14` uses `../fonts/` from `/genscalator/blog/` → `/genscalator/fonts/`), but the
  deployed fonts directory itself was not fetched (one-page live-check budget). Note for the morph work:
  `media/style.css:7-22` uses paths relative to ITS deploy location, so consuming `/genscalator/fonts/`
  from the landing/security pages needs absolute URLs — the ember's "consume the deployed tokens" approach
  implies this but does not say it. MATTERS: trivial; the fresh agent will hit it immediately and visibly.
  CONFIDENCE: medium (live dir unfetched).

## Verified TRUE (honest NULLs — the load-bearing claims all check out)

- **Hashes/trees/remotes:** work repo HEAD `c76ed8a`, tree clean (`status --short` empty), `origin/main`
  in sync (`status -sb` no ahead/behind), `github/main` and `coursegit/main` both at `c76ed8a`. genscalator
  HEAD `f45154b`, tree clean, origin in sync, `github/main` and `coursegit/main` both at `f45154b`. All six
  remote claims TRUE.
- **Paths:** DesignLang.scala, EMBER-TEMPLATE.md, meta-minion-brief.md, minion-log/push-8.md,
  media/index.html, media/security/index.html, media/security/security-model.html, media/style.css,
  deployblog.sc — ALL resolve (Read, first lines each). Noticed: `minion-log/push-9.md` (cited by the ember
  as its own audit) did not exist at audit time — it is THIS file; expected, not a defect.
- **PB consistency:** §5 queue = PB:108's open queue + BR's morph ask (landing + security pages, consume
  deployed tokens, eye-decision first) — matches apart from F1. Holds: English grind (PB:71, BR's stance,
  BR lifts) ✓; SM168 parked (PB:114 + PB:69, BR unparks) ✓; blog 002 = `updated`, redeploy is BR's promote
  call (PB:108) ✓. All three holds carry owner + condition.
- **§0 checklist freshness vs template v0.1:** absdir+java-prop test form with env-chdir RETIRED — matches
  SM167 ADDENDUM-2 (PB:68, 13:23, "ran twice with ZERO guard stall" = the ember's "PROVEN twice") ✓;
  fresh-executable-unvetted ✓ (template:50-52); no-alternation, two slips 2026-07-20 ✓ (template:53-54);
  three-host push + Codeberg badge URL exactly matches PB:109 rider and the `codeberg-status-check` memory
  file (exists on disk, modified 2026-07-20) ✓; two-@mains regen flag TRUE (`DesignLang.scala:6` documents
  it; `@main generateDesignLanguage` at :1189, ssg's @main included via `using file`) ✓; deploy dry-run
  first + `.scala-build`/`.bsp` excluded since `ff40bee` — commit exists ("site: design-language deployed
  and linked from the landing page") ✓. No obviously missing new-today reflex found.
- **Mode state:** `tt mode` → TokSpend, RotVigil, HotHarvest — exactly what ember §1 claims the state file
  carries at exit ✓.
- **Live claim (one fetch):** `tt web get https://bjornregnell.se/genscalator/blog/` → generated template
  header + `--bg: #e8e6c0` (Smither light) + Forgy dark block. The reskin IS live ✓.
- **Timestamp:** ember says written 18:28; PB addendum stamped 18:15; my clock-read 18:31-18:32. Ordering
  coherent; the 18:28 stamp itself not independently corroborated (no file-mtime channel in my tool lane)
  but nothing contradicts it.

## Pattern note (across pushes)

The guessed-stamp and clobber families (pushes 4, 5) did NOT recur in this ember — stamps are marked
clock-read and check out against neighbors; the PB addendum edit did not clobber its neighbor pins
(PB:107/109 intact around :108). The one recurring shape is smaller: prose that quietly EXCEEDS its
substrate (F1's TIP/ACG, push-8's sweep-record gaps) — fluent additions a reader cannot tell from the
pinned record. The ember's own ⛔ header is the right antidote; it worked here.

CANNOT VERIFY: the deployed `/genscalator/fonts/` directory contents (budget, F4); the 18:28 write stamp
beyond coherence; the "Codeberg FLAPPED all day" characterization beyond the single 11:02 specimen (PB:109).

VERDICT: **SAFE TO WARP.**
