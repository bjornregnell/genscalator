# NOW — the tracked present of genscalator development

> **What this file is.** The current state of in-flight work, committed so its history and
> stamps are git's (a stamp here can never lie about its time — the commit log is the
> provenance). Agent-maintained, human-steered; updated at every consistent-state commit.
> Rule: only what has no better home — git knows the history, the issue files know the
> backlog, this file knows the *seam between them*. If this file's last commit is old,
> distrust it and say so. Sibling-to-be: `SOLO-MENU.md` (deliberately separate, so a
> context-rot-aware reader can wear blinders: one narrow file per question).

*As of 2026-07-22 00:4x (commit-stamped by this file's own git log):*

## Just landed (today)

* v0.9.1 "alpha-readiness, step 1" released; update pipeline verified end to end.
* Big repo refactor executed and CLOSED as `reqts/issues/closed/issue-002` (reqts/ real,
  issues insourced, inbox retired, deploy/ gathers transport, todo/ gone).
* README/HUMANS lean-deep split done: BR cut the README (`1e70c5e`), HUMANS.md built out with
  the structure guide + terminology + goals (`7c3c6bc`), CONTRIBUTING gained the in-repo
  issues section that issue-000 promises; README 3.2/3.3 relocated to HUMANS (`9eaa091`).
* GitLab mirror revived + Issue Zero on all three forges; `work/NOW.md` born (SM192 pilot);
  eyes-on-the-ball research designed (`research/055`) with blog 031 drafted for BR's revoice.
* CD13 (BR HD in-feed ~20:00): gitlab JOINS the every-unit push routine as a plain
  fast-forward push (genscalator set now x4: origin, github, coursegit, gitlab);
  `mirror.sc` demotes to the sole deliberate force/repair path. EMBER-TEMPLATE v0.2 carries
  both this and the push-17 mode-state phrasing fix.

## In flight

* Post-warp session live (cold start 19:46 verified clean). Meta-minion push-17 audited the
  first lean ember + this file: zero estimated stamps (first clean push since 13), two real
  findings (a dropped BR-gate, this file's first staleness specimen) — both fixed in this
  commit; report at `research/case-studies/action-research-meta-minion/minion-log/push-17.md`.
* **Introprog v2026.5 PUBLISHED 2026-07-22 00:07** (tag at master, 6 compendium assets,
  agent-published on BR's explicit go): the whole rendered-review arc (26 findings, ~30
  fixes incl. the fi-glue class three-layer fix and a deglueFi mirror post-pass) lives in the
  work-repo `notes/en-pdf-review-v2026.5-2026-07-21.md` (findings numbered to 27 but
  #9 does not exist, so 26 — push-18 CONFIRMED, the PB pin said 27). UML warts ship as-is by BR's
  call, next-release batch. REMAINS: BR's fileadmin publish, gated on a deck-rebuild round.
* **Post-warp "go 12345" batch — 4 of 5 LANDED this session (BR went +afk +solo ~00:17):**
  (1) ✅ deck rebuild for fileadmin: 19 EN decks (98 s, per-deck Swedish gauges), SV wjava,
  intro SV+EN via the scratch builder — every build exit 0, zero LaTeX errors; **BR's
  fileadmin publish is now UNBLOCKED**. (2) ⏳ meta-minion push-18 launched (audits the
  review doc + evening PB claims, local-substrate-only brief). (3) ✅ SM200 solo core:
  `--cache-only` (read-only cache, no backend resolution, fallbacks-vs-baseline gate) +
  `--prose-leaks-ratchet` (152 measured live = the release-notes residual) implemented and
  gate-tested BOTH ways — **diff UNCOMMITTED in introprog for BR review** (2 sources + 2
  baseline files; found+fixed en route: sys.exit in an unforked sbt run kills the hot sbt
  server — gates now sys.error). NO yaml (JOINT, as pinned). (4) ✅ SM196 design note at
  `research/sm-investigations/SM196-release-all-design.md` (c4b6f53): R1-R7 from the
  11-prompt casefile, 3-stage build, 4 decisions left open for BR. (5) ✅ SM195 drafts in
  work-repo `notes/sm195-modly-generalization-drafts.md` (7f4b945): 3-row CODE inventory,
  frozen-records rule honored, README in paste-ready fences — ⚠ HD datum for BR: an ssh
  alias does NOT resolve `http://modly.local:8080`; the name needs an mDNS (avahi) publish
  on the box. TokSpend was dropped by BR (~00:20, 24% weekly at 6d-to-reset) — session ran
  in saving mode; the ember's TokSpend-gated ember-audit was skipped accordingly.
* AT baton back from Hans (his pause email 2026-07-21); evening's genscalator movement:
  HUMANS tables, guard-clean digest URLS block, CD13 executed, SM194-SM200 + CD13 pinned.

## Next up (decided, unstarted)

* *(added 2026-07-23 16:5x clock-read)* SM201 JOINT: prepare compliance with Codeberg ToU
  2(1)7 (the new no-mostly-generative-AI clause, verified live on their main; governance
  status under investigation). Options doc in the work repo; outward steps BR-only.
* *(added 2026-07-23 16:5x clock-read)* SM202 JOINT: self-hosted forge on bjornix.cs.lth.se
  (SM201 option B concretized; department-IT approval + install = BR, prep = agent).

* README section on plugin-update slash-magic (marketplace name gotcha, reload, version
  check) — JOINT: agent drafts, BR calls placement (SM190).
* Versioning-scheme semantics: what plugin version, tags and `tt update` each track —
  JOINT (SM189).
* deployttapi step 2: deploy the generated API docs to bjornregnell.se — BR-GATED outward
  step (dry-run first); the go is the "API-docs commit/deploy call" open decision below.
* Blog 031 revoice + the RT055 study go/no-go (both BR).

## Open decisions (human's call pending)

* Scaladoc palette override via the design language: feasible-cheap, worth-it undecided.
* API-docs commit/deploy call after inspection at the local server (port 8138 while it runs).
* SM200 introprog diff review (uncommitted, 2 sources + 2 baselines) — note: a live
  `--cache-only` verification run was deliberately NOT done; `mirror()` wipes + re-creates
  slides-en/compendium-en, which would delete the freshly built deck PDFs staged for the
  fileadmin publish. Run it BR-present after the publish (expected fallbacks: 1).
* SM195: avahi-publish vs /etc/hosts for `modly.local` (the drafts note recommends avahi),
  then BR's ssh config + the 3 CODE edits.
