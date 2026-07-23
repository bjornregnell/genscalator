# NOW — the tracked present of genscalator development

> **What this file is.** The current state of in-flight work, committed so its history and
> stamps are git's (a stamp here can never lie about its time — the commit log is the
> provenance). Agent-maintained, human-steered; updated at every consistent-state commit.
> Rule: only what has no better home — git knows the history, the issue files know the
> backlog, this file knows the *seam between them*. If this file's last commit is old,
> distrust it and say so. Sibling-to-be: `SOLO-MENU.md` (deliberately separate, so a
> context-rot-aware reader can wear blinders: one narrow file per question).

*As of 2026-07-23 21:07 (commit-stamped by this file's own git log):*

## Just landed (2026-07-23, the graalify-and-forge day)

* **tt-graalify COMPLETE at default-on**: native-image `tt` built (39 MB), parity-proven
  (317 tests/13 suites/0 fail THROUGH the binary), rebuild ritual `deploy/buildnative.sc`
  PROVEN green (build 91 s, parity 31 s, atomic swap), default FLIPPED on BR's "flip it"
  (plain `tt` = 0.01-0.10 s; `TT_NATIVE=0` opts out; absent binary silent, stale binary
  warns). README §3.4 + §1 bullet + ToC; `docs/native.md` truth-passed; issue-003 pins
  `tt update --native`, GATED on win+mac ritual runs.
* **`tt forge` grew the read surface**: issues/prs/issue/pr/protection verbs, GitHub
  dialect via `--gh` (fixed-host token rule), README §4.2 token setup (gh reuse, Codeberg
  docs link, GitLab = honestly nothing).
* **introprog/Hans arc CLOSED**: #956+#957 sandbox-verified against live master and
  MERGED (BR), `Example-compile-gate` now a REQUIRED check on master, review comments
  posted as issue #958 (assigned hmiddelk), Hans added to contributors.
* **SM201 pinned**: Codeberg ToU 2(1)7 RATIFIED (358-144, blog 2026-07-23, gradual
  enforcement); genscalator realistically no-longer-welcome tier; options doc in work repo.
* **SM202 pinned + prepped**: self-hosted Forgejo on bjornix.cs.lth.se — install plan,
  ufw + listener recon done (443 closed, 8080-family open, 8099 free, TSM backup lead),
  443-ask EMAILED to sysadmins. bjornix protocol: production box, every command
  BR-announced + BR-guarded.

## In flight

* Nothing mid-flight — the 2026-07-23→24 BR-present solo session's units are committed.

## Next up (decided, unstarted)

* The ember §3 queue + SM205, after the 2026-07-23→24 BR-present solo session:
  - **CliSuite self-announce + fail-fast — DONE** (06a6bd4): `beforeAll` announces the resolved
    tools dir and aborts with ONE clear message on a stale/partial dir (kills the ~123-phantom-
    fail class). Verified both ways; green in native parity too.
  - **SM205 `tt scala` — BUILT, SHIPPED, NATIVE** (c7500fe; native rebuilt via
    `buildnative.sc --root <genscalator>`, parity-green, 39 MB, `tt scala` live at 0.005s). A
    typed scala-cli driver (dir-scoped, no `-e` eval, no flag passthrough). Retires the blanket
    `scala-cli *` allow — the settings edit (delete it, add narrow `tt scala` allows) is BR's,
    via SM073.
  - REMAIN (ember items 1,3,4): tt web error classes · tt forge --gl · tt bloop clean —
    unblocked now that `tt scala test` exists.
  - SM203 `tt init` bootstrap design — investigate/design still open (AFK-safe).
* Awaiting BR: SM196 ratify (done 2026-07-22 c4b6f53) · SM197 tier decision · SM201 option-B
  checklist · SM204 seed browser eyeball + `gs new spa` wiring · SM206 SECURITY-MODEL review.
* README plugin-update slash-magic section (SM190, JOINT) · versioning semantics (SM189,
  JOINT) · deployttapi step 2 deploy (BR-gated) · blog 031 revoice + RT055 go (BR).

## Open decisions (human's call pending)

* Codeberg e.V. membership (BR: probably yes) + when/whether to execute the SM201-B move.
* bjornix Route A (port 8099, no firewall change) vs Route B (443, canonical) — resolves
  on the sysadmins' reply to BR's email.
* Scaladoc palette override (SM187: feasible-cheap; worth-it = BR's eye).
* API-docs commit/deploy call (SM182:6, after BR inspection).
* SM195 avahi vs /etc/hosts for modly.local + BR's ssh config.
* Plugin release timing (post-0.9.1 tree carries the native story; plugin users get the
  new launcher only at the next release — SM189 versioning feeds this).
