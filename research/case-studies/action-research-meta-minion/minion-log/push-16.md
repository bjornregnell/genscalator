# Meta-minion push 16 — audit of the big repo refactor at `ee6bd8d` (2026-07-21 18:27:52–18:32:33, clock-read)

Task: check the ACCOUNT (three EXECUTED/PARTIALLY-EXECUTED banners in the plan doc + `ee6bd8d`'s
commit message) against the ARTIFACT (tree + history at `ee6bd8d`). Hot audit: `ee6bd8d` was
committed 18:25:58, ~2 min before my first clock read, and the main session kept landing commits
UNDER the audit — `efd1531` 18:29:04 (unrelated baton tombstone) and `bcf2187` 18:31:38, which
moved the very plan doc I read (`reqts/TODO-BIG-REPO-REFACTOR.md` → `reqts/issues/closed/
issue-002-big-repo-refactor.md`). All banner quotes below are from the doc as committed in
`ee6bd8d`; findings are against HEAD `ee6bd8d` per the payload.
Method: Read, `git -C` log/show/status/ls-files (read-only), `tt files`, `tt text grepr/match`,
`tt prd` (read-only, live evidence for claim 6a), `tt chrono now`.

VERDICT: DIVERGENCE FOUND — 3 findings (one fan-out gap the account never flags, one "full
sweep" survivor, one false stamp — the push-15 pattern again); the mechanical repo-state claims
all check out.

## FINDINGS

- CLAIM: "HUMANS.inbox.md retired" (commit msg) / "`HUMANS.inbox.md` REMOVED" (Reason 3 banner).
  ARTIFACT: the FILE is gone from the tree (verified), but `HUMANS.md` — the live root doc that
  DEFINES the inbox protocol — still carries 4 references: lines 5 and 41 name the
  "`AGENTS.md`↔`HUMANS.md`↔`HUMANS.inbox.md` trio", and lines 14 and 56 are now-dangling
  markdown links `[HUMANS.inbox.md](HUMANS.inbox.md)`, line 56 being a STANDING INSTRUCTION:
  "*Agent: never write this file directly — append review-worthy proposals to HUMANS.inbox.md*".
  DELTA: the retirement did not fan out to the protocol doc that mandates the retired file, and
  neither the banner nor the commit message flags this residue. Mitigation noted honestly:
  HUMANS.md is human-owned, so the agent COULD NOT edit it — but it could have flagged the
  dangling instruction for BR, and the Reason-3 banner (which does list what "remains OPEN")
  does not. An agent obeying HUMANS.md:56 tomorrow would recreate the deleted file.
  MATTERS: medium — this is brief §3.1 (a removal is not done until it lands in every place the
  claim reached), in the one file the agent cannot land it in, which is exactly why it needed
  SAYING. CONFIDENCE: high

- CLAIM: "the full reference sweep" (commit msg; the banner enumerates: prd tool default,
  gs-dwim default, reqt-lang skill link, README/CONTRIBUTING/RELEASING/foundations, PRD's own
  four links).
  ARTIFACT: every ENUMERATED site is correctly updated (verified below). Survivor outside the
  enumeration: `tools/parsereqt.scala` — header line 5 "parse / lint reqT-lang requirements
  (e.g. this repo's PRD.md)", usage line 45 same phrase, and usage examples lines 61-62
  `tt parsereqt parse PRD.md` / `tt parsereqt lint PRD.md`. Run from the repo root as the
  examples suggest, that path no longer exists. Not touched by `ee6bd8d` (19-file stat checked).
  Cosmetic near-misses, not flagged as findings: bare no-path `PRD.md` mentions in
  `skills/gs-dwim/SKILL.md:23,215` and `skills/reqt-lang/SKILL.md:3,18` (filename-as-name, fine);
  `README.md:211`'s `PRD.md` is the SEEDED app's own file (legit); CHANGELOG mentions are
  historical; `tmp/` + crud-web-app-seed template out of scope per the payload.
  DELTA: "full" totalizes over an enumerated sweep; one live tool's self-documentation still
  points at the root path. MATTERS: low-medium — a user following the tool's own usage examples
  gets a file-not-found; and this is the third audited use of an unearned totalizer
  ([[summaries-enumerate-dont-totalize]] cited in the agent's own ember §0).
  CONFIDENCE: high

- CLAIM: issue-001's Discussion header: "### Comment by bjornregnell/CF5 at 2026-07-21 18:30".
  ARTIFACT: issue-001 was created IN `ee6bd8d`, committed 18:25:58. The comment's stamp
  postdates the commit that contains it by ~4 minutes; 18:30 had not happened on this clock
  when the text was committed.
  DELTA: a forward-estimated stamp in provenance position again — the exact class push-14
  caught in the PB (bdf321e's five overshot pin times) and push-15 caught in the ember's
  "(clock-read)" labels, now in a git-tracked ISSUE file whose comment-header format exists
  precisely to carry provenance. Third push in a row; third substrate.
  MATTERS: low alone; as a now-thrice-confirmed reflex it is the standing pattern (below).
  CONFIDENCE: high

## Verified TRUE (enumerated, per payload item)

1. PRD move + sweep, enumerated sites: `tools/prd.scala:8,12,29,77,79` default =
   `<tools>/../reqts/PRD.md`; `skills/gs-dwim/SKILL.md:201` (`gs reqt` defaults `reqts/PRD.md`);
   `skills/reqt-lang/SKILL.md:115` link `../../reqts/PRD.md`; `README.md:173` `(reqts/PRD.md)`;
   `CONTRIBUTING.md:84` `(reqts/PRD.md)`; `docs/RELEASING.md:27` both parse+lint examples;
   `docs/foundations.md:102` `(../reqts/PRD.md)`. Root `PRD.md` gone (git mv in the stat).
2. PRD's own links: EXACTLY four non-http markdown links in `reqts/PRD.md` (lines 13, 18, 21,
   27), all `../`-rebased (`../CHANGELOG.md` ×2, `../docs/foundations.md`, `../README.md`); a
   second regex pass for non-rebased relative links found ZERO. "Four outbound links" is exact;
   nothing missed.
3. Inbox + issues: no tracked `todo/`, `issues/` (root), or `HUMANS.inbox.md` (`ls-files` empty
   for all three; working tree clean at audit start). Pre-removal inbox (`ee6bd8d^`) held
   EXACTLY 5 `- [ ]` items (research 010, 009, 004-FYI, 011, 036); "4 obsolete + 1 transcribed"
   is structurally exact: item 5 (the research/036 References split) = `reqts/issues/open/
   issue-001-split-references-data-from-rendering.md`, which points at
   `research/036-references-refactor-plan.md` (line 13) and is status "parked" per the banner.
   The 4 dropped items reference late-June/early-July notes; obsolete is plausible, not
   adjudicated here. issue-000 exists in `open/` and cites `reqts/PRD.md` (line 45).
4. `deploy/mirror.sc` (full text read): self-check line 69-70 expects
   `<root>/deploy/mirror.sc`; usage lines 29-35 all say `deploy/mirror.sc` and note the move
   ("lives in deploy/ since the 2026-07-21 repo refactor"); failure hint line 123 no longer
   always blames SSH — "READ git's own message ... no SSH key? a PROTECTED branch/tag ...
   (SM191 specimen 2026-07-21: the old always-blame-auth hint here misdiagnosed a
   protected-branch rejection)"; per-run summary line 129 matches. All three account sub-claims
   TRUE in the file text.
5. todo/ retirement: plan doc tracked at `reqts/TODO-BIG-REPO-REFACTOR.md` in `ee6bd8d`
   (moved on to issue-002 by `bcf2187` DURING this audit); no `todo/` in `ls-files`; remaining
   `todo/TODO-BIG-REPO` strings are only minion-log history (pushes 11/12/14 — mine, correctly
   frozen) and issue-002's own "began life as" line. No live-doc survivors.
6. "tt prd summarize resolves the new default": reproduced live — `tt prd --prd
   <abs>/reqts/PRD.md summarize` prints "FUTURE roadmap — 49 gists from .../reqts/PRD.md".
   "49 gists" EXACT; default-path resolution verified from `prd.scala:77` code (not by cwd).
7. Banner datings: all three banners say 2026-07-21; the file landed in `ee6bd8d` 18:25:58
   same day — dates honest. Reason-4's "issue-zero DONE earlier same day": issue-000 commits
   `28578bf` 17:26:27(--) / `8396c6c` / `e239d69` 17:39:56 precede the refactor — "earlier same
   day" TRUE for the in-repo part.

## CANNOT VERIFY / SKIPPED

- "toolbox compile green, full suite green" — not re-run (per payload); no test-output artifact
  found in the tree to corroborate (only `tmp/commit-msg-refactor-r3/r4.md` drafts, which
  evidence the msg-file-first workflow, not the suite). CANNOT VERIFY — flagged, not doubted.
- Forge-side states: Issue Zero "live/pinned/labeled on Codeberg/GitHub/GitLab", the GitLab
  revival (SM191), the post-move mirror.sc RUN — no web in this lane. The SM191 protected-branch
  story is at least CONSISTENT with mirror.sc:123's new hint text.
- "Ratified as issue-001 by BR in-feed" and all quoted BR decisions — session facts.
- Content quality of `reqts/issues/README.md` (40 lines, "real workflow docs") — existence and
  size verified, prose not audited.

## PATTERN (across pushes)

1. **Forward-estimated stamps in provenance position: three pushes, three substrates.** Push-14
   (PB pin times, corrected in bdf321e), push-15 (ember "(clock-read)" labels + SM186
   "clock-anchored"), push-16 (issue-001 comment header, +4 min past its own commit). The
   correction commits and ember rules have not disarmed the reflex; it follows the agent into
   each NEW document format it invents. The push-15 mechanical counter stands unadopted: a
   claimed-clock stamp must be ≤ its enclosing commit time.
2. **The world/self split holds again.** Every repo-state claim (4 links, 5 inbox items, 49
   gists, 19-file stat, mirror.sc text, zero remnants) verified TRUE; the residue is all in
   claims ABOUT THE AGENT'S OWN ACT — "full" sweep, "retired" without the fan-out flag, when a
   comment was written. Brief §3.6, fourth consecutive corroboration.
3. New-ish: the fan-out gap (finding 1) landed in a file the agent is FORBIDDEN to edit. The
   failure therefore shifted from "didn't land the edit" to "didn't flag the un-landable edit"
   — the obligation that survives a write-ban is the flagging one.

## Protocol self-check (§8)

Wrote ONLY this file. Commands: `git -C` log/show/status/ls-files (read-only), `tt files`,
`tt text grepr/match`, `tt prd` (read-only), `tt chrono now`, Read; one bare command per call,
no pipes, no cd, long outputs via run_in_background + Read. Live-race caveat recorded above.
