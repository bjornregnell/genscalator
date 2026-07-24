# Raw-git reflex regressed where tt gitinfo already covers it (2026-07-24)

**Specimen.** To check whether BR's README commits were pushed, the agent ran raw
`git -C <repo> rev-list --left-right --count origin/main...main` — when `tt gitinfo <repo>`
already reports `sync: N ahead, M behind upstream` (and takes `--remote <name>` for a specific
mirror). BR caught it: "could/should have been tt git?". It was NOT a tooling gap — the typed tool
exists and the agent had USED it earlier the same session (at cold-start verification). A pure
reflex regression.

**Why it matters / the pattern.** This is the third raw-shell slip in one multi-thread session, all
same family: (1) `git log … | head` tripped the guard (SM217 note), (2) a compound `;`-chain + a
`2>/dev/null` tripped it, (3) this raw `git rev-list` for sync state. The trigger was NOT a missing
tool but ATTENTION under load: many interleaved threads (forge SM207, model-warp pins, the contributor
rewrite, README edits) pushed the typed-tool reflex below salience even though the digest was loaded
at cold start. Consistent with [[active-skill-still-cold-starts-dormant-reflexes-regress]] and the
day's theme that reflexes must live in a TABLE, not willpower.

**Distinction (real gap vs reflex miss).** Genuinely missing typed shapes remain: commit-log SEARCH
by grep/author/co-author trailer (SM217), `tt forge contributors` (SM217), clone/bundle/init plumbing
(SM218). Sync/ahead-behind is NOT one of them — `tt gitinfo` has it. So the fix here is salience, not
a new tool.

**Fix applied (make it stick, table not recall).**
- Added a `tt gitinfo` row to the GIT section of `docs/guard-clean-digest.txt` (the cold-start reflex
  table loaded via `tt doc guard-clean-digest` / `gs warm`): for STATE/SYNC use `tt gitinfo`, never
  raw `git status` / `git rev-list` / `git log --oneline`; and it documents the SM217 log-search gap
  (keep bare, never `| head`).
- This WR specimen.
- A feedback memory so it surfaces at future cold starts.

Ties [[use-tt-grepr-not-raw-grep]], SM211 (tt which born from a sibling cold-start slip), SM217,
the avoid-guard-stall skill.
