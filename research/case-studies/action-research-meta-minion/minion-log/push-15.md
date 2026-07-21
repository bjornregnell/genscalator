# Meta-minion push 15 — POST-warp audit of the shipped 2026-07-21 ember (2026-07-21 13:54:57, clock-read)

Task: audit the ember the cold-started CF5 actually inherited (`work-repo/tmp/resume-prompt.md`,
mtime 13:49:57) against the substrate, including whether push-14's leanness fixes landed.
Method: `git -C` log/status/branch/show/ls-files (read-only), `tt files`, `tt text grepr`
(quoted), stat, Read, `tt chrono now`. Box restarted between sessions; clock continuity
cross-checked below before trusting any stamp.

VERDICT: DIVERGENCE FOUND — the ember's stamps about ITS OWN writing are provably false and
mislabeled "(clock-read)"; every repo-state claim I checked is TRUE, including "EXACTLY 245".

## FINDINGS

- CLAIM: header "written 13:40-14:1x (clock-read)" and footer "*Pre-flight 14:1x (clock-read)*".
  ARTIFACT: ember mtime 13:49:57. My first clock read this push: 13:54:57 — i.e. POST-warp,
  the wall clock had not yet reached 14:10 when I began auditing a file whose writing
  allegedly ended at 14:1x. No clock jump at the restart: pre-warp commit stamps run
  continuously 13:34:30 → 13:49:25 (work repo + gs), the STAMP CORRECTION's own in-text
  clock-read (13:39:58) sits exactly between commits 13:38:24 and 13:42:43, and the post-warp
  clock continues the same line (cold start 13:51, me 13:54).
  DELTA: "14:1x" never existed on this box's clock before the warp. Both stamps are estimates
  wearing the "(clock-read)" label — in the document whose own §0 (line 14) forbids estimated
  stamps and cites the same-day STAMP CORRECTION specimen, written minutes after committing
  that very correction (bdf321e, 13:42:43).
  MATTERS: medium-high — not for the 20 minutes, but because a false PROVENANCE label is worse
  than a missing one: it disarms the reader's discount heuristic ("clock-read ⇒ trust it").
  CONFIDENCE: high
- CLAIM: (same family, PB) SM186 pin "BR 2026-07-21 ~13:55 clock-anchored" — cited by the ember
  as a today's-pin anchor.
  ARTIFACT: cfc9710, the commit that added the pin, is stamped 13:46:27. A stamp cannot
  postdate the commit that contains it by 9 minutes and be clock-anything.
  DELTA: the estimated-stamp reflex fired AGAIN ~4 minutes after the correction commit for the
  previous five overshoots, now with an upgraded label ("clock-anchored").
  MATTERS: low alone; as the third label-vs-clock mismatch in one hour it upgrades finding 1
  from slip to pattern. CONFIDENCE: high
- CLAIM: header "Audited pre-warp by minion push-14 (leanness fixes applied ...)".
  ARTIFACT: push-14 listed six leanness items B1-B6. Landed: B1 (grep anchors — §6 says
  "grep anchors, not line numbers"; §5's graal pointers now grep `SM146 ADDENDUM`), B2 (hash
  chains deferred to the PB/git log), B4 (§0 slimmed to rule+link — flap ⇒
  [[codeberg-status-check]], pkill ⇒ wr-data, stamp ⇒ grep `STAMP CORRECTION`), B6 partially
  (the wrong "after 13:00" removed). NOT landed: B3 (the two Hans/introprog bullets remain as
  two, §4 lines 36+39, though the hash overlap was removed), B5 (the EN-mirror line 37 still
  carries 94s / 2 model calls / modly DOWN / 4.6% with NO verify pointer — still the only §4
  line violating the section's own "each line with its verify" header).
  DELTA: "fixes applied" totalizes; the true count is ~3.5 of 6, and B6's residue regressed
  into finding 1's false clock-read label. The ember's own §0 cites
  [[summaries-enumerate-dont-totalize]].
  MATTERS: low-medium — the shipped fixes are real and the ember IS visibly leaner; the
  unqualified "applied" is the divergence. CONFIDENCE: high
- CLAIM: §4 "Possibly-unposted drafts in `work-repo/notes/sm145-hans-review-2026-07-21.md` §2:
  drop-the-#949-band-aid reply · ANIMAL Fyle/Katt-Hund leak ask · CI-staleness issue".
  ARTIFACT: §2 holds five paste-ready blocks; the ANIMAL ask (§2.3) and the CI-staleness issue
  (§2.5) are there. No draft matches "drop-the-#949-band-aid": `tt text grepr <work>/notes md
  'band-aid'` = 0 hits; the vestigial-#949-override matter exists only as §6 PROSE ("Hans
  offered to drop it", note lines 163-165), no paste block. (§6 also shows the #951 comment
  was "delivered in-feed", not filed — a plausible same fate for this reply.)
  DELTA: 2 of the 3 listed drafts exist where pointed; the third is not a draft and not in §2.
  MATTERS: low — a cold agent sent to paste it would find nothing and might confabulate one.
  CONFIDENCE: high on absence; medium on what the ember meant.
- Micro, no verdict change: the pre-flight footer says "HEADs `cfc9710` + `4c107be` verified
  clean + ×3 at write time" while §4 correctly names gs HEAD `9368103` (committed 13:49:25,
  ~30s before the ember's final mtime) — the document disagrees with itself about gs HEAD.
  Both tips ARE ×3-decorated today, so no state error, just a stale footer. Also persisting
  from push-14 unfixed (was flagged as micro then too): "#950+#948+#949 (`35fc9912`)" — that
  hash is the #949 merge alone.

## Verified TRUE (enumerated)

- Work repo: HEAD `cfc9710` decorated `origin/main, github/main, coursegit/main`, `status
  --short` empty, `branch -vv` shows no ahead/behind. Push-14's finding 1 (bdf321e unpushed to
  all three) was FIXED before the warp — the correction landed. ×3 TRUE.
- genscalator: HEAD `9368103` decorated ×3; `4c107be` (Reason 4) and `37eab10` (Reason 3)
  beneath. `status --porcelain -uall`: exactly `?? docs/generated/api/README.md` — the ember's
  "ONE deliberate untracked file" is exact (authorship not verifiable from git; mtime 13:20
  pre-warp, consistent).
- Today's 4 wr-data notes tracked (`ls-files research/wr-data`): sandbox-clone-has-no-guard-
  clean-shape-tt-git-lacks-clone, subagents-need-the-ember-s0-checklist, verify-writers-
  before-advising-file-edits, tt-box-lacks-local-health-shape. All four present.
- introprog: HEAD `76f45c7e` = `origin/master`, tree clean. `35fc9912` = "Merge pull request
  #949" in history; `6c950c66` present; review batch matches item-for-item (ca23dbb2
  Compilation concept, 26715775 + 76f45c7e cache, 5e367d9c workflows, a141d44d regen).
- API docs: `tt files <gs>/tmp/api-out html` header = "245 files". EXACTLY 245 TRUE.
- PB grep anchors ALL resolve: `SM186|SM185|SM184|SM183 PINNED` lines 107-110, `SM182|SM181
  PINNED` 113-114, `SM146 ADDENDUM` exactly TWICE (line 73 = the 07-19 graal datum, line 111 =
  the 07-21 investigate pin — precisely the two menu item 4 promises), `SM170/SM146 ADDENDUM-2`
  line 79, `BIG-REPO-REFACTOR ADDENDUM` line 112, `SM145 CODA` line 175, `STAMP CORRECTION`
  in the SM185 line (bdf321e's one-line annotate-not-erase append — 1 line changed for 5 wrong
  stamps is the annotation policy working as designed, not a shortfall).
- Hold paths: `notes/pr943-review-draft.md` exists; the sm145 note exists (modulo finding 4);
  `meta-minion-brief.md` exists.

## CANNOT VERIFY / SKIPPED

- Live harness mode state (§1) — the main agent's own verify step; not probed.
- GitHub-side states (#951 answered, #952 closed, #953/#955 open, Compile-and-Build verdict)
  — no web access in this push's lane.
- "bloop killed ~11 GB freed", the :8138 server death, modly DOWN, 94s/2-model-calls/4.6%
  (push-14's B5, still pointer-less), "Hans-measured" percentages, all quoted BR utterances.
- Content-level audit of the 4 wr-data notes and the "3 specimens" count in §0's header —
  existence checked, contents not.

## PATTERN (across pushes)

The split has never been this clean: every claim the ember makes about the WORLD (2 HEADs, ×3
sync, 1 untracked file, 245 pages, 9 grep anchors, 4 notes, an entire introprog commit list)
verified TRUE; the claims that failed are all about the AGENT'S OWN ACTIONS — when it wrote,
what it applied, what it filed where. That is brief §3.6 measured, not asserted. Trajectory of
the residual failure class: wrong claims (pushes 8/9/11) → ambiguous references (13) →
values-true-at-write-stale-at-read (14) → false provenance LABELS (15). The label escalation
is the dangerous step: "(clock-read)"/"clock-anchored" exist precisely so readers can trust a
stamp, and the reflex now decorates estimates with them, including once ~4 minutes after
committing a correction for the same failure — corroborating wr-data
`retracted-is-not-immune...` at minutes-scale. Cheap mechanical counter for the main agent:
a stamp claiming clock provenance must be ≤ its enclosing commit/mtime; anything else is
self-refuting and can be checked at write time with the same `tt chrono now` call §0 already
mandates.

## Protocol self-check (§8)

Wrote ONLY this file. All commands read-only (`git -C` log/status/branch/show/ls-files across
3 repos, `tt files`, `tt text grepr` quoted, stat, `tt chrono now`, Read); no pipes, no cd;
long outputs via run_in_background + Read.
