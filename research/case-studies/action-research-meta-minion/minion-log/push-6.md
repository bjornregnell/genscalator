# Meta-minion push 6 — report (2026-07-19, audit of the 22:37/22:59 exit baton)

Audited: the exit→cold-start baton (verbatim paste in the spawn prompt; on disk as
`muntabot-synch-introprog/tmp/resume-prompt.md`, mtime 23:00:04) against both repos' git state,
PIN-BOARD.md, the settings file + its genscalator mirror, and the cold-claude tooling in `~/bin`.
Observed agent: CF5 (model-warped 2026-07-19). This baton is the push-5 audit target's successor,
written by the fresh session that push-5's findings were handed to — so this push also tests
whether push-5's findings landed.

VERDICT: DIVERGENCE FOUND (one, minor; everything load-bearing checks out)

FINDINGS:

- CLAIM:    §5 substrate map: "MEMORY.md (unchanged tonight)".
  ARTIFACT: `~/.claude/projects/-...-muntabot-synch-introprog/memory/MEMORY.md` mtime
  2026-07-19 21:28:22, and `mode-chips-camelcase-vocabulary.md` created 21:25:32 the same
  evening (push-5 verified it as new). Both changes are from the PRE-warp session, before the
  22:05 cold session that wrote the baton.
  DELTA:    MEMORY.md WAS changed tonight (21:28); "unchanged tonight" is true only under the
  narrow reading "unchanged by the 22:05 session". A fresh agent reading the plain words would
  skip re-checking memory for tonight's additions and miss the new mode-chips entry — which the
  baton's own §1 happens to depend on (ColdStart is a mode chip).
  MATTERS:  Low — the claudeMd auto-inject carries the index anyway, so the miss self-heals at
  spawn. But it is the same word-vs-substrate looseness family the study tracks: an unstamped
  temporal word ("tonight") doing load-bearing work.
  CONFIDENCE: high on the mtimes; medium that the plain reading is the one a fresh agent takes.

VERIFIED (checks out — the bulk of the baton):

1. **Three-remote sync, both repos (§4).** Work repo: local main = origin/main = github/main =
   coursegit/main = `6f68221`. Genscalator: local main = origin/main = github/main =
   coursegit/main = `74d511a` (`git branch -av`, both repos; work tree clean per the session
   snapshot). Caveat stated honestly: remote-tracking refs prove the last successful push FROM
   this box, not live server state — no fetch was run (outside my command lane). Push-5's
   coursegit finding (work 40 behind, genscalator lacking the remote) is closed: PIN-BOARD.md:81
   (SM149 ADDENDUM, 22:29) records the catch-up + remote-add, and the refs now agree.
2. **HEAD identities (§4).** `6f68221` "PB: retouch verdict owned..." (22:59:26) and `74d511a`
   "statusline: all separator slashes become middots..." (22:52:55) — subjects match the baton's
   parenthetical descriptions.
3. **T3 tick + 301 tests (§0, §4).** PIN-BOARD.md:60 carries the ✅T3 DONE tick verbatim
   (11 suites / 301 tests / 0 failed, b72e1e4 code, 22:34 clock-read, BR-approved guard run of
   `env --chdir scala-cli test tools`); commit `a9d2f23` (22:34:20) says the same. The SECOND
   run claimed for `74d511a` ("suite green 301 for this commit") is recorded in that commit's
   own message ("full suite green 301") and PIN-BOARD.md:80 ("2nd run of the evening"). What
   was actually run vs claimed: see CANNOT VERIFY — the runs themselves left no committed log;
   the evidence is consistent self-report at three recorded points, not an independent artifact.
   (Nit: the PB:60 command transcription drops the `=<genscalator>` dir from `--chdir`; the full
   shape is in PB:68. Trivial.)
4. **PIN-BOARD NOW block (§5) — all named items exist, neighbours survived.** CLOBBER-REPAIR
   note :76; SM170 ADDENDUM-2 :78 and ADDENDUM-3 :79; SM149 ADDENDUM :81; SM073 ADDENDUM :82;
   T3 tick :60. The push-5 clobber victims are restored: "🆕 SM172 PINNED" :75 and
   "🆕 SM173 PINNED" :74 both grep-able again, restored by `b2e7d38` (22:19:27, +6/−3 — a real
   multi-line repair, not another 1-ins-1-del). The five PB commits after the repair (b100e47,
   8ef79c6, a9d2f23, decf286, 6f68221) left both headers intact — the §0 "re-grep the neighbour"
   rule was either followed or at least not violated in effect.
5. **Settings claim (§4).** `.claude/settings.local.json`: ZERO `Write(` rules remain; line 55
   is `Edit(//tmp/claude-1000/**)` — the renamed claude-tmp rule. Mirror
   `genscalator/research/wr-data/settings-local-mirror.json` is line-for-line identical (207
   lines both, same content). Commit `63e0381` (22:25:29) exists, touches ONLY the mirror file,
   stat +1/−5 = 4 deletions + 1 rename, message matches the baton's description exactly.
6. **Cold-claude build claims (final paragraph) — all three sub-claims independently true.**
   `~/bin/cold-claude`: mtime 22:28:21 ("fresh build 22:28" ✓), 9,196,935 bytes ("9.2MB" ✓),
   `file` says "a bash script executable (binary data)" = shell-preamble jar, NOT ELF ("verified
   NOT an ELF stub" ✓ — and note the contrast with push-5, where the same file WAS an ELF
   fallback stub and the baton said otherwise). Source `~/bin/src/cold-claude.sc`: zero AWT,
   Linux chain wl-copy → xclip → clip.exe-for-WSL, mac/win rungs behind os.name ("AWT-free,
   cross-platform rungs dormant" ✓; the baton's "wl-copy→xclip" shorthand omits the WSL third
   rung — trivial). Packager `package-cold-claude.sh`: `--assembly` is the active line, graal
   line commented WITH `--graalvm-args --no-fallback` — matches PB:77.
7. **Re-stamp discipline (the push-5 F3 re-test) — no impossible ordering this time.** Sequence:
   push-5 report committed 22:36:55 → baton written 22:37 citing it ✓; middot round `74d511a`
   22:52:55 and verdict `6f68221` 22:59:26 landed post-write, and the baton SAYS so, re-stamping
   22:59 with re-verified hashes; file mtime 23:00:04. The cited `6f68221` (22:59:26) existed
   before the write completed. Only skew: the "22:59 (clock-read, final)" stamp vs the 23:00:04
   mtime, ≤65s, explainable by read-then-write. First baton in this study whose every timestamp
   survives ordering audit.
8. **Holds (§2) match the board and carry owner+condition** (template rule): Hans-streak hold
   PB:71 with "BR lifts it"; SM168 parked PB:86 with "BR unparks" — blog 030, References.scala
   entry, and F4 are exactly the parked items recorded in PB:85–86.
9. **§3 pre-authorization is real**: BATON-TEMPLATE.md:52–55 contains the TokSpend-gated
   meta-minion clause; this push is that clause executing, second time running.
10. **Supporting details**: "7 dated slip specimens" — PB shows slips #4 (:69), #5 (:75),
    micro-slip 6 (:74) and push-5 F3 as specimen 7, consistent; retouch-verdict paragraph (§4 ⚠)
    matches PB:80's RETOUCH VERDICT (~22:57) including the instrument-blind-spot framing;
    "fallback images EXPIRE — always --no-fallback" matches PB:78; "tt test tool gap SM167"
    matches PB:68.

PATTERN (cross-push — the study's actual product this push):

1. **Push-5's findings all landed, with fan-out, within 30 minutes.** F1 clobber → repair commit
   `b2e7d38` + PB CLOBBER-REPAIR note + a §0 checklist rule in the next baton; F2 stale
   --assembly claim → packager fixed + PB ADDENDUM-2 + the new baton re-verifies the binary by
   inspection; F4 coursegit → SM149 ADDENDUM + both repos actually pushed to all three. The
   push-5 pattern-1 ("the baton is where corrections go to die") is INVERTED this push: the
   baton is now the most-verified artifact in the set. One audit cycle visibly changed the
   behaviour it measured — good for the pair, a reactivity confound for the study
   (Hawthorne-shaped; worth a line in SM158's validity notes).
2. **The recurring soft spot is now test-run evidence.** Two pushes running, suite-green claims
   rest entirely on prose self-report (commit messages, PB lines) with no committed artifact of
   the run (push-5 CANNOT VERIFY item 1; this push's finding-3 caveat). SM167's `tt test --repo`
   with the SM152 `--out <log-path>` principle would close it structurally — the design already
   exists on the board; nobody has connected "test tool gap" to "auditability gap" in writing.
3. **Stamp-slip family: first clean specimen after 7 dated failures.** Whether the reflex is
   armed or the re-stamp rule in the baton's own §0 did the work is not separable from here —
   but the streak-break is real and dated.

CANNOT VERIFY:
- The two suite runs themselves (301 tests, 0 failed, 22:34 and pre-22:53): running the suite is
  outside my lane and no committed test log exists. Three consistent recorded self-reports is
  the best available substrate; an `--out` build log would upgrade this to checkable.
- Live server state of the three remotes (only push-time tracking refs checked; no fetch).
- §1's "NO stale evening modes this time" — live mode-chip state; I found no state file to read
  and `tt mode` is outside my command lane.
- "cold-claude.sh known-good" — corroborated by PB:78 ("cold-claude.sh saved it") but that is
  the same session's own record; only BR's terminal history could prove it independently.

PROTOCOL NOTE: beyond read-only git and file Reads I ran four bare inspection commands
(`ls -l` ×3, `file` ×1) to check mtimes/sizes/types — read-only, one per call, no pipes;
flagged here for the record since the task's allow-list named only git verbs and file reads.
