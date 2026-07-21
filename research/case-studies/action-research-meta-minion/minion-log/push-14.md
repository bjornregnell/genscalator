# Meta-minion push 14 — PRE-warp audit of the refreshed 2026-07-21 ember (2026-07-21 13:46, clock-read)

Task: two dimensions on `work-repo/tmp/resume-prompt.md` (the REFRESHED ember, superseding the one
push-13 audited): (A) consistency vs substrate, (B) lean-and-mean-ness. Report only; no edits.
Method: `git -C` log/status/show/branch/check-ignore, `tt files`, `tt text grepr` (quoted), stat,
Read, `tt chrono now`. Live-audit caveat: the main session is ACTIVE while I check — some deltas
below may be in flight.

VERDICT (A): DIVERGENCE FOUND — 1 real mismatch (work HEAD moved past the ember and is UNPUSHED to
all three remotes), 3 minor, everything else VERIFIED including an exact 245.

## A. FINDINGS

- CLAIM (§4): work repo HEAD `7037e62`, "synced ×3 apart from any Codeberg-flap stragglers".
  ARTIFACT: HEAD is `bdf321e` ("PB: stamp correction, five estimated pin times overshot the
  clock"), `branch -vv` = `[origin/main: ahead 1]`; origin+github+coursegit all sit at `7037e62`.
  DELTA: the ember is stale by one commit, and `bdf321e` is unpushed to ALL THREE remotes — not a
  Codeberg flap (that excuse covers origin only; github+coursegit pushed fine all day).
  MATTERS: medium — the §0 every-unit-both-hooks push routine is unsatisfied at audit time; if the
  warp happens now, the cold-start agent inherits an ember whose §4 HEAD claim is false and a
  local-only commit that a box hiccup would strand. Land the push (and ideally bump the ember's §4
  hash) before warping. CONFIDENCE: high
- CLAIM (§6): "line ~167 = the SM145 CODA".
  ARTIFACT: the coda (with the Hans-arc hashes, e.g. `35fc9912`) is at `PIN-BOARD.md:174`; line
  167 is the 2026-07-17 SM144-unblock line.
  DELTA: pointer off by 7 — exactly the pin lines added ABOVE it today; the coordinate was carried
  over from yesterday's ember (where 167 was right) instead of re-grepped.
  MATTERS: low as an error, high as a lesson: the PB grows at the top, so EVERY line-number
  pointer in an ember rots by construction. See B1. CONFIDENCE: high
- CLAIM (§4): the introprog review batch "committed after 13:00".
  ARTIFACT: `ca23dbb2` 12:38, `26715775` 12:39, `5e367d9c` 12:39, `a141d44d` 12:44,
  `76f45c7e` 13:12 (git author times).
  DELTA: 4 of 5 landed before 13:00. MATTERS: trivial — but it is a time claim written from memory
  in the same ember whose §0 carries today's stamp-drift specimen. CONFIDENCE: high
- CLAIM (pre-flight, §50): "both HEADs verified clean + pushed at each unit through the day".
  ARTIFACT: genscalator `status --short` = `?? docs/generated/api/` (one untracked file,
  `docs/generated/api/README.md`, mtime 13:20 today — the deliberate SM182 seed the pin itself
  references as a start pointer).
  DELTA: "clean" overlooks one untracked file; plus the work-repo ahead-1 above.
  MATTERS: trivial for the README (deliberate, self-explaining); the ahead-1 is finding 1.
  CONFIDENCE: high
- Micro-imprecision, no verdict change: §4 writes "the merged #950+#948+#949 (`35fc9912`)" — that
  hash is the #949 merge only; #948 = `57a4bb60`, #950 = `0da0d2b6`. One hash labels three merges.

## A. Verified TRUE

- genscalator HEAD `4c107be`, decorated origin+github+coursegit, tracked tree clean. All listed
  beneath-hashes exist: `37eab10`, wr-data `14416a1 7fbe353 21b5cc8 a12741b 13b6818 e5e4b31
  0b46f10` (all seven present), push-13 `5a84ade`.
- Work-repo day chain: `cd84da3 → c7b6408 → d75508c → 104fb2a → 15073e3 → 0880b69 → 43d0334 →
  3f19b8f → c5cb1e1 → 7037e62` all present in that order (one unlisted interleaved: `e4b04f1`
  Reason-3 pin — its rider `0880b69` IS listed; same selective-list pattern push-13 noted).
- introprog (`/home/bjornr/git/hub/lunduniversity/introprog`): tree clean, HEAD `76f45c7e` =
  origin/master. `35fc9912`/`57a4bb60`/`0da0d2b6` = the three PR merges; exactly FIVE commits
  between them and `6c950c66` inclusive ("five morning fixes" count exact); the review batch
  matches item-for-item — Compilation concept `ca23dbb2`, cache retrofit `26715775` + `76f45c7e`,
  CI-workflow fix `5e367d9c`, glossary regen ×4 = `a141d44d` touches exactly 4 glossary files.
- API docs: `genscalator/tmp/api-out` holds EXACTLY 245 html files (`tt files`: "245 files") —
  the ember's number is precise, and the `$lessempty$greater$` paths corroborate the
  `<empty>`-package wart. (The "~150/245 vendored reqt" split: not counted, plausible.)
- Today's four wr-data notes all exist and are committed: `sandbox-clone-has-no-guard-clean-shape-
  tt-git-lacks-clone.md`, `subagents-need-the-ember-s0-checklist.md`,
  `verify-writers-before-advising-file-edits.md`, `tt-box-lacks-local-health-shape.md`.
- "No ~/.claude memory-dir writes today": MEMORY.md mtime 2026-07-20 22:04 — supports it (index
  untouched today; individual unindexed writes not exhaustively ruled out).
- PB pointers, the right halves: TODAY's pins at lines 107-113 (SM185, SM184, SM183, SM146-add,
  Reason-3+mirror.sc rider, SM182, SM181) — "~107-116" over-covers into three 07-20 pins
  (114-116) but is "approximately right" as tasked. §5's graal start-pointers "PB ~line 73 + 79"
  are EXACT: line 73 = the 07-19 ~22:00 graal datum, line 79 = SM170/SM146 ADDENDUM-2.
- Every other named path resolves, repo-prefixed (push-13's finding adopted): work-repo
  `PIN-BOARD.md`, `notes/sm145-hans-review-2026-07-21.md` (mtime today 11:46),
  `notes/pr943-review-draft.md`; genscalator `todo/TODO-BIG-REPO-REFACTOR.md` (Reasons 1-4
  headings at lines 5/66/96/112 — §6's "Reasons 2-4" all present), `tmp/br-site/`, `tmp/api-out/`,
  `research/wr-data/`, minion-log push-13 + brief; introprog
  `autotranslate/scratch/sbt-task.scala`; `~/.claude` MEMORY.md.
- The ember itself is gitignored (`check-ignore tmp/resume-prompt.md` — same path as yesterday's,
  overwritten in place, consistent with "the version you audited is gone").

## A. CANNOT VERIFY

- §4 EN-mirror line: 94s runtime, 2 model calls, modly DOWN, concepts=123, the 4.6%+4.6%
  Hans-measured Swedish-% — no verify pointer given and no cheap substrate found; see B5.
- §1 budget ("weekly limit RESET 09:00") and all quoted BR utterances — session facts.
- §4 "first-ever Compile-and-Build run fired... verdict UNCHECKED" — egress; correctly queued as a
  solo-menu item rather than claimed.

## B. LEANNESS — ranked cut-list (report only; the agent edits, not I)

1. **Kill line-number pointers; anchor by grep.** The one already-wrong pointer (~167 → 174)
   proves the mechanism: the PB grows at the top, so every line coordinate in an ember is stale by
   the next pin. §6 should say "grep `SM145 CODA`" / "grep `SM181 PINNED`" (the ember already
   trusts grep for SM145 elsewhere); same for §5's "PB ~line 73 + 79" → "grep `graal datum` /
   `ADDENDUM-2`" — those two happen to be right TODAY and wrong after the next pin.
2. **De-duplicate the hash chains.** §4's work-repo line carries a 9-hash day chain that the PB
   coda (its own §6 first pointer) already harvests, and git log reproduces on demand; the gs line
   lists 7 wr-data hashes that §4's wr-data line re-lists by NAME two lines later. Keep HEAD +
   "day chain: see the SM145 coda / git log"; cut ~3 dense lines. A turn-zero reader needs the
   TIPS and where the story lives, not the story twice.
3. **Merge the two Hans/introprog lines.** §4 line "introprog (BR's clone...)" and §4 line
   "Hans/introprog: ALL PRs merged..." split one state across two bullets with overlap (#948/#949/
   #950 appear in both). One bullet: repo state + PR scoreboard + the unposted-drafts pointer.
4. **§0 carries narrative that lives elsewhere — and §0 is the verbatim subagent payload.** Line
   13 mandates pasting §0 into every subagent brief, so every §0 sentence is a per-delegation tax.
   The Codeberg flap diagnosis (⇒ [[codeberg-status-check]]), the pkill saga (⇒ SM181 pin +
   wr-data note), and the stamp-drift specimen (⇒ PB line 107 + bdf321e) each have a durable home;
   §0 needs the RULE + link, not the evidence. Cutting the why-prose roughly halves §0.
5. **EN-mirror line is unverifiable as written.** 94s / 2 model calls / modly-down / 4.6% —
   no verify command, no note pointer (only line in §4 without one, breaking the section's own
   "each line with its verify" header). Either point at where it is recorded (commit message,
   notes file) or compress to "EN mirror rebuilt clean on master 2026-07-21; measures optional".
6. **Unanchored micro-times.** "committed after 13:00" (wrong), "written 13:40-13:5x", "~14:10"
   family — the ember's own §0 forbids estimated stamps; drop times where git carries them.
Not flagged: §2/§5 as such — the holds carry owner+release, the queue is rot-ranked, and the old
ember's dropped design-queue items (light-logo, pink, mascot...) are safely homed in the PB
SM155/T8 ADDENDUM-2 OPEN queue, so their disappearance from the ember is a leanness WIN, not loss.

## PATTERN (across pushes)

Third consecutive push where hard facts hold and the residue is REFERENCE hygiene: push-12 = clean;
push-13 = ambiguous unprefixed paths (fixed in this ember — repo prefixes now present throughout,
the fix visibly landed); push-14 = stale line-number + a raced HEAD. The failure class keeps
narrowing: from wrong claims (push-8/9/11) to ambiguous references (13) to time-fragile references
(14). The ember practice is improving push-over-push; the remaining leak is coordinates that were
true at WRITE time and false at READ time — line numbers and pre-push HEAD hashes. Both have the
same fix: reference by durable anchor (grep string, "HEAD after the final push") rather than by
value captured mid-session.

## Protocol self-check (§8)

Wrote ONLY this file. All commands read-only (`git -C` log/status/show/branch/check-ignore ×2
repos + introprog, `tt files`, `tt text grepr` quoted, stat, `tt chrono now`, Read); no pipes, no
cd, one bare command per call; long listings via run_in_background + Read.
