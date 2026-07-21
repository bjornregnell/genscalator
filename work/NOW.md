# NOW — the tracked present of genscalator development

> **What this file is.** The current state of in-flight work, committed so its history and
> stamps are git's (a stamp here can never lie about its time — the commit log is the
> provenance). Agent-maintained, human-steered; updated at every consistent-state commit.
> Rule: only what has no better home — git knows the history, the issue files know the
> backlog, this file knows the *seam between them*. If this file's last commit is old,
> distrust it and say so. Sibling-to-be: `SOLO-MENU.md` (deliberately separate, so a
> context-rot-aware reader can wear blinders: one narrow file per question).

*As of 2026-07-21 18:39 (commit-stamped by this file's own git log):*

## Just landed (today)

* v0.9.1 "alpha-readiness, step 1" released; the repo→marketplace→session update pipeline
  verified end to end (cache carries same-day skill content).
* Big repo refactor: `reqts/` is real (PRD.md + insourced issues, first citizens
  issue-000/001/002), `HUMANS.inbox.md` retired, `deploy/` gathers all outward transport
  (deployblog, deployttapi, mirror.sc, the legacy-redirect htaccess), `todo/` gone.
* The GitLab mirror revived (SSH keys done, protected-branch unblocked, full tag sync);
  Issue Zero posted, labeled and pinned across Codeberg/GitHub/GitLab.

## In flight

* README.md + HUMANS.md lean/extended split — BR's own hand, live right now (includes fixing
  HUMANS.md's references to the retired inbox, per meta-minion push-16).
* This file itself: first pilot of the tracked-NOW design (ember-diet follow-up pending).

## Next up (decided, unstarted)

* README section on plugin-update slash-magic (marketplace name gotcha, reload, version check).
* Versioning-scheme semantics: what plugin version, tags and `tt update` each track.
* deployttapi step 2: deploy the generated API docs to bjornregnell.se (dry-run first).

## Open decisions (human's call pending)

* Does the gitlab mirror join the every-unit push routine, or stay `deploy/mirror.sc`-driven?
* Scaladoc palette override via the design language: feasible-cheap, worth-it undecided.
* API-docs commit/deploy call after inspection at the local server.
