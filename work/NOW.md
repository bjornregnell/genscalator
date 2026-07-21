# NOW — the tracked present of genscalator development

> **What this file is.** The current state of in-flight work, committed so its history and
> stamps are git's (a stamp here can never lie about its time — the commit log is the
> provenance). Agent-maintained, human-steered; updated at every consistent-state commit.
> Rule: only what has no better home — git knows the history, the issue files know the
> backlog, this file knows the *seam between them*. If this file's last commit is old,
> distrust it and say so. Sibling-to-be: `SOLO-MENU.md` (deliberately separate, so a
> context-rot-aware reader can wear blinders: one narrow file per question).

*As of 2026-07-21 19:54 (commit-stamped by this file's own git log):*

## Just landed (today)

* v0.9.1 "alpha-readiness, step 1" released; update pipeline verified end to end.
* Big repo refactor executed and CLOSED as `reqts/issues/closed/issue-002` (reqts/ real,
  issues insourced, inbox retired, deploy/ gathers transport, todo/ gone).
* README/HUMANS lean-deep split done: BR cut the README (`1e70c5e`), HUMANS.md built out with
  the structure guide + terminology + goals (`7c3c6bc`), CONTRIBUTING gained the in-repo
  issues section that issue-000 promises; README 3.2/3.3 relocated to HUMANS (`9eaa091`).
* GitLab mirror revived + Issue Zero on all three forges; `work/NOW.md` born (SM192 pilot);
  eyes-on-the-ball research designed (`research/055`) with blog 031 drafted for BR's revoice.

## In flight

* Post-warp session live (cold start 19:46 verified clean). Meta-minion push-17 audited the
  first lean ember + this file: zero estimated stamps (first clean push since 13), two real
  findings (a dropped BR-gate, this file's first staleness specimen) — both fixed in this
  commit; report at `research/case-studies/action-research-meta-minion/minion-log/push-17.md`.
* The gitlab-in-push-set open decision is under live co-design (agent recommendation on the
  table: join as plain fast-forward push, mirror.sc stays the only force path); BR's call.

## Next up (decided, unstarted)

* README section on plugin-update slash-magic (marketplace name gotcha, reload, version
  check) — JOINT: agent drafts, BR calls placement (SM190).
* Versioning-scheme semantics: what plugin version, tags and `tt update` each track —
  JOINT (SM189).
* deployttapi step 2: deploy the generated API docs to bjornregnell.se — BR-GATED outward
  step (dry-run first); the go is the "API-docs commit/deploy call" open decision below.
* Blog 031 revoice + the RT055 study go/no-go (both BR).

## Open decisions (human's call pending)

* Does the gitlab mirror join the every-unit push routine, or stay `deploy/mirror.sc`-driven?
* Scaladoc palette override via the design language: feasible-cheap, worth-it undecided.
* API-docs commit/deploy call after inspection at the local server (port 8138 while it runs).
