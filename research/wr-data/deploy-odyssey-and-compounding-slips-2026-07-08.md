# WR data: the first-deploy odyssey + command-compounding regressions (2026-07-08)

A long live session that took the genscalator blog from "renders locally" to "one command from live on
bjornregnell.se", plus a clutch of agent self-regressions caught along the way. Two threads.

## Thread A: tool-gap specimens surfaced by doing a real SFTP deploy
Setting up the *first ever* deploy to one.com (bjornregnell.se, Beginner plan, SFTP-only) surfaced a chain of
friction that pure local work never would. Each is a demand for a typed tool or a piece of knowledge that was
missing:

1. **No `tt deploy` existed** — we hand-built `deployblog.sc` (scala-cli + lftp, creds from `~/.netrc`, nothing on
   argv). Candidate for a future typed `tt deploy` once the shape stabilises.
2. **one.com redesigned their site** — the help docs the agent found (via WebSearch) did not match the live UI,
   and `help.one.com` now 403s automated fetches (WebFetch blocked). The human reading the panel live was the only
   reliable source. Lesson: for fast-moving vendor UIs, the human-in-the-loop IS the tool.
3. **SSH "too many authentication failures"** — the ssh client offered all of BR's existing keys first, each
   counting against the server's MaxAuthTries, so the password never got a fair try. Fix: force password-only auth
   (`-o PubkeyAuthentication=no -o PreferredAuthentications=password`). Non-obvious; cost several round-trips.
4. **lftp + `.netrc`** — lftp only reads the password from `.netrc` when the USERNAME is in the URL
   (`sftp://user@host`), not for a bare host. Another non-obvious quirk. (deployblog.sc sidesteps it by parsing
   `.netrc` itself and passing creds on lftp's stdin.)
5. **Landing dir was NOT `httpd.www`** — SFTP login lands in the account home; the real web root is `webroots/www/`
   (BR had to explore interactively to find it). The docs' generic "you land in httpd.www" was wrong for this account.
6. **ssg over-copied figures** — rendering any post copied the WHOLE `blog/figures/` dir (30 files for other draft
   posts), so a dry-run showed 33 files to upload instead of 6. BR caught it ("we dont want the whole blog dir with
   all draft etc"). Root-fixed same session: ssg is now reference-aware (copies only linked figures, self-prunes).

Takeaway: a real outward-facing task (deploy) is a *dense* generator of tool-gap and missing-knowledge data that
inward local work does not produce. The human-present (not AFK) mode is what let this data surface at all
(see [[not-afk-safe-solo-yields-wr-data]]).

## Thread B: command-compounding regressions (shell-blob lineage, instances 6-7)
Sibling of `shell-blob-fallback-regression-2026-07-07.md` (instances 1-5). Two more this session, both a DIFFERENT
flavour than the earlier heredoc/`/dev/stdin` blobs: **compounding** (multiple commands joined with `&&`/`;`/`|`),
which the anti-regression checklist explicitly forbids because it defeats the allowlist prefix-match:

- **Instance 6:** `cd /…/genscalator && scala-cli test tools …` — used `cd &&` on roughly the 5th tool call
  post-warp, despite the checklist. The bare abs-path form (`scala-cli test /abs/path/tools …`) was available and
  used correctly for the rest of the session after self-catch.
- **Instance 7:** `find … -delete ; echo … ; grep … | sort | uniq -c` — a THREE-command pipe/semicolon blob.
  **BR + the guard both caught it**: the guard refused `find … -delete` under a `Bash(find:*)` prefix rule
  ("executes commands or modifies files — cannot be auto-allowed"). This is the genscalator thesis in miniature:
  a general executor cannot be safely prefix-allowlisted; a *typed* tool with declared narrow semantics can
  (spawned SM031 `tt find`).

Pattern confirmed again: the regressions cluster at momentum/fatigue moments in a long session, and they are
"recalled != enacted" (the rule is in the reloaded header AND obeyed dozens of times, yet slipped). Corroborates
the [[joint-rot-vigilance-recovery-kit]] and the rot-suspicion note. Both slips were **caught** (self + BR + guard)
and produced no bad artifact — the layered defence (checklist + guard + human) held even though the first layer
(recall) leaked.

## Disposition
Feeds RT052 (throughput/friction), SM016/SM022 (the instrument), SM031 (`tt find`), a future `tt deploy`, and the
rot-vigilance line. [[shell-blob-fallback-regression-2026-07-07]] [[guard-against-forced-confirmations]]
