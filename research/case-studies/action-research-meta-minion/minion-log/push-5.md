# Meta-minion push 5 — report (2026-07-19 22:15 clock-read via tt chrono)

Audited: the exit baton `muntabot-synch-introprog/tmp/resume-prompt.md` (mtime 22:01:29) against
both repos' git state, the pin board, and the cold-claude tooling in `~/bin`. Model note: the
observed agent is CF5 as of the 2026-07-19 warp. Mid-audit race noted: the fresh session committed
`badce0a` (22:10:26) while I was reading; the audit target stays the baton vs the PRE-exit substrate,
with `badce0a` cited only as corroboration.

VERDICT: DIVERGENCE FOUND

FINDINGS (most severe first):

- CLAIM:    Baton §5 substrate map: "PIN-BOARD.md NOW block (SM168–SM173 pinned today)".
  ARTIFACT: SM172 and SM173 no longer exist as pins. `b318b58` (21:30:02) added a real
  "🆕 SM172 PINNED" bullet. `68c8624` (21:44:56, message "pin SM173") is 1-insertion-1-deletion:
  instead of ADDING a bullet it REPLACED the SM172 line, welding SM173's text onto SM172's content
  and destroying the "SM172 PINNED" header. `4b79387` (22:00:50, the SM146 addendum, also
  1-ins-1-del) did the same again to "🆕 SM173 PINNED". Result: one franken-bullet at
  PIN-BOARD.md:73 headed "📮 SM146 ADDENDUM" carrying three pins' content. `tt text grepr` finds
  the string "SM172" NOWHERE in the work repo, and "SM173" only inside the baton itself (:34) —
  plus commit messages.
  DELTA:    The baton advertises six pins; two of them are unfindable on the board because two
  successive "append" edits were actually single-line clobbers.
  MATTERS:  Real. The board is the pair's primary substrate and IDs are permanent by rule
  ([[no-number-reuse-across-substrate]]); a fresh agent grepping SM172/SM173 hits nothing. Neither
  the exit pre-flight nor the fresh session's `badce0a` (which edited the same region at 22:10)
  caught it. Mechanical failure class (brief §3.3), both instances in the declared DumbZone hour.
  CONFIDENCE: high

- CLAIM:    Baton postscript: cold-claude "freshly rebuilt with the JDK/wl-copy/xclip clipboard
  chain, packaged --assembly"; failure hint "AWT ownership vs the assembly build first".
  ARTIFACT: `~/bin/src/package-cold-claude.sh` (mtime 21:56:01) has the --assembly line COMMENTED
  OUT; the active line is `--native-image`. The build ran 21:57:20, installed to `~/bin/cold-claude`
  21:57:52; `file` says ELF executable (the graal FALLBACK image). The truth landed in the PB SM146
  addendum committed 22:00:50 — BEFORE the baton's final edit (mtime 22:01:29) — yet the baton kept
  "--assembly" and pointed the fresh agent's debugging at the wrong build type.
  DELTA:    The baton describes a build that was clobbered one minute after its claimed refresh;
  the file handed to the fresh agent was the one place the correction never landed.
  MATTERS:  Medium in effect (the fresh session self-diagnosed it at 22:09, ADDENDUM-2 in
  `badce0a`, after the binary failed live: `Could not find or load main class cold$minusclaude_sc`)
  but it is a textbook un-landed fan-out: PB updated, baton not — and the baton is the artifact
  whose whole job was to be the trustworthy pointer.
  CONFIDENCE: high

- CLAIM:    Baton header + postscript: "written 21:45 (clock-read)", pre-flight "re-run at the
  21:56 refresh (clock-read — the draft said 22:0x, guessed forward AGAIN before the check caught
  it)".
  ARTIFACT: File mtime is 22:01:29, and the baton asserts work HEAD `4b79387`, a commit created
  22:00:50 — content that did not exist at the last stamped pre-flight.
  DELTA:    At least one unstamped edit after 21:56; the final baton's pre-flight coverage claim
  overstates (the hash it vouches for was never covered by the stamped pre-flight). Ironically the
  "corrected-away" draft stamp 22:0x was right for the final write.
  MATTERS:  Low-medium — the hash IS correct; what's wrong is the claim about what was verified
  when. Same felt-time/stamp family as slips 1–6.
  CONFIDENCE: high

- CLAIM:    §4: work repo "synced BOTH hosts".
  ARTIFACT: github/main and origin/main both at `4b79387` — true. But the repo has a THIRD remote,
  coursegit, whose main sits at `c460641` (Jul 18 23:09), 40 commits behind, unmentioned by the
  baton.
  DELTA:    "BOTH" silently excludes one configured remote.
  MATTERS:  Trivial if coursegit is a deliberately-retired mirror (likely, given the pair's
  codeberg+github vocabulary); flag only so the convention gets stated somewhere once.
  CONFIDENCE: high on the facts, low on whether it matters.

VERIFIED (checks out):
1. genscalator HEAD `f7e71a7` (README awareness figures), github/main = origin/main = `f7e71a7`,
   tree clean. Work repo HEAD was `4b79387` at baton time, both named hosts synced.
2. Holds match the board: Hans-streak hold (PIN-BOARD.md:71 STANCE), SM168 PARKED (:80).
3. §3 budget-gated minion audit exists in `work/BATON-TEMPLATE.md:52`; TokSpend gate honored —
   this push is that clause executing.
4. `tt bloop restart` exists (`tools/bloop.scala`, commit `b72e1e4`) and was field-tested (the
   newborn-RSS find). `tt test --repo` is indeed pinned, as the SM167 ADDENDUM (PIN-BOARD.md:68).
5. Memory file `mode-chips-camelcase-vocabulary.md` exists. wr-data has the claimed 2 new
   CF5-vs-CO4 datums (`4532944` + `5237f08`, both 2026-07-19).
6. Warp declaration (§1) matches the SM118(b) decision on the board, including the
   supersede-bare-rm-rot-vigil rule. "6 slips on 2026-07-19" matches the board's micro-slip 6.

PATTERN (cross-push):
1. **Fix-without-fan-out, pushes 1–5, now inverted.** Earlier pushes: chat/board fixed, files not.
   This push: the FILES (PB) got the truth and the BATON — the single artifact built to carry truth
   across the boundary — kept the stale claim. The one-home principle's hardest case is the home
   that is about to be the only thing read.
2. **NEW: append-by-replace.** Two commits in one hour whose messages say "pin"/"addendum" (adds)
   but whose stat is 1 insertion + 1 deletion (replacements), each eating a pin header. Mechanically
   detectable: any commit claiming to PIN whose diff deletes a line deserves a look. Consistent with
   the known edit-anchor weakness ([[edit-anchor-short-unique-substring]]) — the anchor was the
   whole previous bullet, and the replacement forgot to re-emit it on its own line.
3. Stamp slips continue across the session boundary (F3 is slip-family specimen 7, in the very
   document that warns "you WILL guess").

CANNOT VERIFY:
- "Test suite NOT yet run for b72e1e4" — a negative; consistent with the SM167 addendum's
  guard-stall account, but only the transcript could prove it.
- What changed in `cold-claude.sc` at 22:00:21 — AFTER the 21:57:20 binary build, so source and
  installed binary may diverge; `~/bin` has no history. (Moot if the AWT-drop rewrite lands.)
- ADDENDUM-2's phrase "clobbered the WORKING assembly build": no assembly artifact survives, so
  whether a working assembly was ever installed before 21:57 is unprovable from the substrate.

PROTOCOL NOTE (self-specimen, WR-relevant — logged here because I may not write wr-data):
Early in this audit I ran two raw pipe commands (`ls | grep`, `ls -la | tail`) despite the
anti-regression checklist being verbatim in my own context; BR caught it live from the guard.
Datum: a cold-spawned CF5 subagent regresses to raw-shell reflexes even when the FORBIDDEN→ALLOWED
table is part of the very text it is auditing. Supports the baton's own §0 premise and the
reflex-regression class (brief §3.5). Remaining commands after the catch were compliant.
