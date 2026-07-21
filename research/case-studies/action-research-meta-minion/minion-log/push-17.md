# Meta-minion push 17 — first cold-start audit of the SM192 lean-ember era (2026-07-21 19:49:16–19:52:49, clock-read)

Task: audit (a) the lean warp ember the cold-started super-agent consumed (pasted verbatim in
my payload) and (b) the new tracked-state file `genscalator/work/NOW.md` (last commit `a415170`
2026-07-21 19:15:42 — payload's hash+time claim confirmed exact) against the work-repo
PIN-BOARD (lines 95–139 read) and both repos' git logs. Specific pattern-watch inherited from
pushes 14/15/16: estimated stamps in provenance position, unearned totalizers, and whether the
SM192 division of labour (ember = pointer, NOW.md = seam state, PB = append-only past) holds.
Method: Read, `git -C` log/show/status (read-only), `tt files`, `tt text match`, `tt mode`
(read), `tt chrono now`. Both trees clean at audit time; both HEADs (`9eaa091` gs, `5d8ebb7`
work) decorated `origin/main, github/main, coursegit/main`.

VERDICT: DIVERGENCE FOUND — but small and gate-shaped, not stamp-shaped. The thrice-caught
estimated-stamp reflex produced ZERO specimens in either artifact (first clean push since 13;
the SM192 structural fix worked on its first outing). Two findings: a BR-gate dropped from a
next-up item in both new-era surfaces, and NOW.md already stale against its own freshness rule
at first test.

## FINDINGS

- CLAIM: NOW.md "Next up (decided, unstarted): deployttapi step 2: deploy the generated API
  docs to bjornregnell.se (dry-run first)". The ember's §3 Holds list omits SM188 step 2.
  ARTIFACT: PB SM188 pin (line 112): "Step 2 (LATER, **BR-gated** outward)"; PB SM188/SM186
  ADDENDUM (line 113): "OPEN on SM188: step 2 deploy (BR-gated, dry-run first)". The gate is
  stated twice in the PB and appears in NEITHER new-era surface: NOW.md keeps only "(dry-run
  first)" and files the item under "decided, unstarted", while the same file's "Open decisions
  (human's call pending)" carries "API-docs commit/deploy call after inspection" — the same
  matter listed as both decided-unstarted and pending-human-call.
  DELTA: the one outward-facing BR-gated item lost its gate qualifier exactly where the SM192
  design says a cold agent should read first; NOW.md also mildly contradicts itself about
  whether the deploy is decided or pending.
  MATTERS: medium-low — a cold solo agent reading only "Next up" could start an outward deploy
  the PB gates on BR; mitigations are real (dry-run first, the Open-decisions row, and the
  three-surface habit). The blog-031/RT055 row shows the file KNOWS how to mark gates
  ("(both BR)") — this row just didn't get one. SM189/SM190 are likewise unmarked in NOW.md
  but ARE in the ember holds, so only SM188:2 falls through both.
  CONFIDENCE: high

- CLAIM: NOW.md header: "updated at every consistent-state commit"; body: "README/HUMANS
  lean-deep split done" (as of 19:13).
  ARTIFACT: `9eaa091` (19:42:43, "README sections 3.2 and 3.3 relocate to HUMANS") touched
  HUMANS.md + README.md only — a consistent-state commit 27 minutes after NOW.md's last
  refresh (`a415170`, 19:15:42) that did not update NOW.md, and whose content CONTINUES the
  very split NOW.md declares "done".
  DELTA: the tracked-present file went stale on its first session boundary, against its own
  header rule; "done" was overtaken (or premature) within half an hour.
  MATTERS: low-medium — the designed catch (ember §2.1: "verify freshness ... stale = distrust
  and say so") covers exactly this, and 27 min / 1 commit is a small drift; but the catch only
  fires for a reader who actually compares dates, and this is specimen #1 that the
  every-consistent-commit discipline does not yet self-sustain. Logged so the trajectory is
  visible if it recurs.
  CONFIDENCE: high (on the mechanics; whether `9eaa091` was pre- or post-warp I cannot tell
  from git alone — either way NOW.md was not refreshed at it)

- Micro, no verdict weight: ember §1 "TokSpend rides the state file" is TRUE of the harness
  mode state (`tt mode` → TokSpend, ColdStart, SmartZone — TokSpend survived the warp) but the
  ember's headline says "STATE LIVES IN COMMITTED FILES", and the committed state file
  (NOW.md) carries no modes — a cold reader could hunt TokSpend in the wrong "state file".
  Phrasing nit, not a divergence.

## Verified TRUE (enumerated)

Ember claims checked:
- Stamps: header "19:1x (clock-read 19:13:22 at prep start)" — hedged span + explicit anchor,
  the honest form. The same 19:13:22 clock-read appears in the PB SM184 rider (committed
  `5d8ebb7` 19:15:50; stamp ≤ commit ✓). No stamp in the ember postdates any enclosing commit.
- §2.2 grep anchors ALL resolve in the PB: SM188 PINNED (l.112), SM189 (115), SM190 (118),
  SM191 (120), SM192 (122), SM193 (123) — the promised SM188..SM193 six-of-six — plus RT055
  PINNED (124), CAP-STALL (116), SM191 SUBSTANTIALLY DONE (121), v0.9.1 RELEASED (117).
- §2.3 expectations: both trees clean ✓; both HEADs ×3-decorated (origin+github+coursegit) ✓.
- §3 Holds, each against substrate: SM168 parked (PB l.139) ✓ · SM184/SM185 JOINT (125, 108) ✓
  · SM189/SM190 JOINT (115, 118) ✓ · RT055 "go = BR" (124: "design ratification + go = BR") ✓
  · blog 031 revoice BR (`1aa3187` msg: "marked for his revoice") ✓ · Swedish-% grind HELD
  while Hans active (109: "Hans ACTIVE ⇒ the Swedish-% grind HOLD stands") ✓.
- "next free IDs: SM194+ / RT056+": `tt text match` on the PB → 0 hits for each; highest
  observed SM193 / RT055. EXACT.
- "ColdStart clearing = the HUMAN's call": matches PB SM118/SM177 ADDENDUM (l.111) — the
  auto-clear timer is a PARKED rider, not decided. ✓
- Pointers exist: `meta-minion-brief.md` (read), `docs/EMBER-for-sub-agents.md`,
  `work/NOW.md`. §4's description of MY task matches the payload I received. ✓

NOW.md claims checked:
- "As of 19:13 (commit-stamped)": last commit `a415170` 19:15:42; stamp ≤ commit ✓ — the
  push-15 mechanical counter, now satisfied STRUCTURALLY, exactly as the SM192 pin intends.
- Just landed: v0.9.1 released + pipeline verified (PB l.117, enumerated there) ✓ · refactor
  closed as issue-002 (`reqts/issues/closed/issue-002-big-repo-refactor.md` exists; `bcf2187`)
  ✓ · README cut `1e70c5e` + HUMANS build-out `7c3c6bc` + the commits exist with matching
  messages ✓ · GitLab mirror + issue zero ×3 forges (PB l.121) ✓ · NOW.md born `26c5c53` ✓ ·
  `research/055-eyes-on-the-ball-or-blinders.md` exists ✓ · blog 031 exists at
  `media/blog/031-agent-blinders-or-eyes-on-the-ball.md` (`1aa3187`) ✓.
- Open decisions all trace: gitlab push-set question (PB l.121 REMAINING) ✓ · scaladoc palette
  feasible-cheap/worth-it-undecided (PB l.116) ✓ · api-docs call (PB l.113/129) ✓.
- SOLO-MENU.md correctly described as "sibling-to-be": does not exist yet (work/ holds exactly
  EMBER-TEMPLATE.md, EMBER-EXAMPLE.md, NOW.md). ✓
- Push-16 finding 1 residue FIXED en route: the rebuilt HUMANS.md (`7c3c6bc`) contains exactly
  ONE "inbox" mention (l.20, the new forge-trackers-as-inbox semantics); the four dangling
  `HUMANS.inbox.md` references are gone. NOW.md's "inbox retired" is now fan-out-true.

## CANNOT VERIFY / SKIPPED

- Whether `9eaa091` was committed by the pre-warp or post-warp session (affects finding 2's
  color, not its substance).
- The :8138 server "while it runs" — process state is outside my lane (no process ops), and
  correctly so.
- "BR cut the README" attribution in NOW.md — session fact; the commit exists, the actor split
  (BR vs agent-under-grant) is not in git.
- Ember-time ×3-sync at 19:13 exactly — I verified the CURRENT tips are ×3; the historical
  instant is unrecoverable, and the ember delegates this to a verify anyway.

## PATTERN (across pushes)

1. **The estimated-stamp reflex: caught in pushes 14, 15, 16 — ZERO specimens in push 17.**
   Both audited artifacts carry only clock-read or commit-backed stamps, and NOW.md's design
   makes the push-15 counter ("a stamp must be ≤ its enclosing commit") structural rather than
   volitional. Honest caveat: the fix partly works by REMOVING the surface — the lean ember
   simply makes far fewer self-claims than its ancestors, so there is less room for the reflex
   to fire. The real test is whether PB pins and issue files (where free-text stamps still
   live) stay clean; the SM184 rider's 19:13:22 ≤ 19:15:50 says yes, n=1.
2. **The residue class has moved: from false stamps to dropped GATES.** Push-16's finding 1 was
   an un-flagged fan-out gap; push-17's finding 1 is a BR-gate that survived in the append-only
   PB but fell off during compression into the new mutable surfaces. Compression loses
   qualifiers before it loses facts — every fact in NOW.md checked true; what went missing was
   WHO may act on one of them. If a finding of this class appears in push 18, the lean-era
   templates should grow an explicit owner/gate column for next-up items.
3. The world/self split (pushes 13–16) holds trivially this push: almost nothing here IS a
   self-claim any more. That is the SM192 diet working as intended.

## Protocol self-check (§8)

Wrote ONLY this file. Commands: `git -C` log/show/status (read-only, absolute paths),
`tt files`, `tt text match`, `tt mode` (bare read), `tt chrono now`, Read; one bare command
per call, no pipes, no cd, no process ops, no commits, no memory, no mode changes.
