# NOW — the tracked present of genscalator development

> **What this file is.** The current state of in-flight work, committed so its history and
> stamps are git's (a stamp here can never lie about its time — the commit log is the
> provenance). Agent-maintained, human-steered; updated at every consistent-state commit.
> Rule: only what has no better home — git knows the history, the issue files know the
> backlog, this file knows the *seam between them*. If this file's last commit is old,
> distrust it and say so. Sibling-to-be: `SOLO-MENU.md` (deliberately separate, so a
> context-rot-aware reader can wear blinders: one narrow file per question).

*As of 2026-07-24 18:02 (commit-stamped by this file's own git log):*

## Just landed (2026-07-24, the transfer-and-v0.9.2 day)

* **v0.9.2 RELEASED**: tag + GitHub release object live (native fast path, tt scala,
  tt which, statusline space diet + two-glue + lim-block weld, raw capture, future-proof
  f5 rendering). CHANGELOG + PRD PAST carry v0.9.2 (first same-day FUTURE→PAST section).
* **Repo transfer (github now PRIMARY)**: new clones under `~/git/hub/bjornregnell/` —
  open `genscalator` + closed work repo renamed `genscalator-work`; the old `~/git/berg/`
  clones are RETIRED. Mirror remotes fanned out on both new clones (work: gitlab +
  coursegit; open: gitlab + coursegit + codeberg-as-batched-mirror), all verified reachable.
* **Codeberg batch PUSHED** (14:39, its SSH recovered after refusing all day): main
  fast-forwarded to current + tag v0.9.2. Forge-side v0.9.2 release object on codeberg
  still pending (tt forge release-create, BR's call).
* **statusline f5 SOLVED via declared route**: `tt limit set f5 <pct>` shows `f5·~<pct>%·3d`
  (human-declared `~`, live countdown, auto-drop past reset; `gs f5 NN` updates). The live value
  is the statusline / `tt limit`, never narrated here (it moves through the day — a fixed number
  in this file only goes stale). Measured route impossible in CC 2.1.218 — upstream feature
  request drafted (BR files). Live declaration expires Tue Jul 28 ~09:00. Screenshot
  `docs/img/status-line-2.png` committed (e456712).
* **SM202 hold CLEARED**: sysadmins confirmed 80+443 already open on bjornix; Forgejo
  test now blocked only on BR-led local setup (ufw, nginx+TLS, binary). bjornix protocol
  unchanged: production box, every command BR-announced + BR-guarded.
* **SM215/SM216 pinned**: post-warp solo task (hmiddelk introprog scan + sandboxed-minion
  PR review) · `tt init` spec amended (init also creates in-repo `tmp/` + idempotent
  gitignore entry).
* **SM207 SHIPPED** (ebc0388): `tt forge release-create` speaks three dialects
  (Gitea/Codeberg default · `--gh` GitHub · `--gl` GitLab), CliSuite 179/0 with the new
  arg-contract tests. Unblocks the gitlab v0.9.2 release object (LIVE create = BR, token).
* **SM220 FIXED**: the live `tt` launcher was re-pointed from the retired berg clone to
  the hub clone and the native binary rebuilt from hub; verified at the 18:00 cold start
  (launcher symlink → hub, `gs-status-legend` served, chrono footer 0.009s).
* **Doc/reflex hardening**: guard-clean digest gained the `tt gitinfo` row (8aaca4f),
  `gs-status-legend` doc added (07d8766), tools/README missing-tools section filled
  (43e8a2d), RT056 model-warp study committed.

## In flight

* Nothing mid-flight — the 2026-07-24 cold-start boot units (mirror remotes, codeberg
  batch, this reconcile) are committed.

## Next up (decided, unstarted)

* The pre-authorized solo queue, RE-PRIORITIZED by BR 2026-07-24 (ember §4; drain order):
  - (1) **SM215** hmiddelk introprog scan + sandboxed-minion PR review (outward = BR).
  - (2) **SM219** pre-baked render-ready docs (extend `gs-status-legend`; gs-dwim cats it).
  - (3) **SM217** `tt git` log-search (grep/author/trailer) + `tt forge contributors` READ verbs.
  - (4) `tt web` surfaces the error CLASS (refused/timeout/dns).
  - (5) **SM218** promote the validated commit-message filter to a separate effectful tool
    OUTSIDE `tt git` (non-allowlistable; mandatory backup/dry-run/audit + `tt git bundle`).
  - (6) tt bloop clean (path-pinned .scala-build removal; destructive RUN = BR review).
* SM203 `tt init` Stage 1: solo IF BR ratifies the design (spec AMENDED by SM216).
* Awaiting BR: SM196 release-all design · SM197 tier decision · SM201 option-B
  checklist · SM204 seed browser eyeball + `gs new spa` wiring · SM206 SECURITY-MODEL
  run-code section · SM073 settings edit (retire blanket scala-cli allow).
* README plugin-update slash-magic section (SM190, JOINT) · versioning semantics (SM189,
  JOINT) · deployttapi step 2 deploy (BR-gated) · blog 031 revoice + RT055 go (BR).

## Open decisions (human's call pending)

* Codeberg e.V. membership (BR: probably yes) + when/whether to execute the SM201-B move.
* bjornix routing RESOLVED in substance (sysadmins: 80+443 already open → Route B
  canonical is available); remaining = BR-led local setup, see SM202 addenda on the PB.
* Scaladoc palette override (SM187: feasible-cheap; worth-it = BR's eye).
* API-docs commit/deploy call (SM182:6, after BR inspection).
* SM195 avahi vs /etc/hosts for modly.local + BR's ssh config.
* Plugin release timing (post-0.9.1 tree carries the native story; plugin users get the
  new launcher only at the next release — SM189 versioning feeds this).
