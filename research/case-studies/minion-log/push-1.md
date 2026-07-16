# Meta-minion push 1 — report (2026-07-17)

Checked: CO4's self-report (session 2026-07-16 23:11 → 2026-07-17 00:35) against the artifacts in
genscalator and muntabot-synch-introprog. Verified from source and commits, not from CO4's account.

VERDICT: DIVERGENCE FOUND

FINDINGS:

- CLAIM:    "`?` is not a legal mode-label character (labels are `[A-Za-z0-9._-]+`), so `tt mode rm
  hangover?` would be rejected" (self-report #2). Landed in the artifact as: "`tt mode` accepts
  `[A-Za-z0-9._-]+`, so **`tt mode rm hangover?` would be rejected outright.**"
  (`research/wr-data/nobody-dropped-the-hangover-chip-2026-07-16.md:24-25`), presented there as
  verified-from-source.
  ARTIFACT: `tools/mode.scala` — `valid()` (line 59) is called ONLY in the `add` branch (lines
  69-72). The `rm` branch (lines 75-76) is `write(read().filterNot(_ == label))`: no validation at
  all. `tt mode rm hangover?` is silently accepted, exits 0, and no-ops.
  DELTA:    The command would be silently accepted as a no-op, not "rejected outright" — the
  validation CO4 cites exists but does not run on `rm`.
  MATTERS:  The note's conclusion survives by a different route (a derived chip never enters the
  state file — statusline.scala:294 — so the rm removes nothing either way). But the stated
  mechanism is false, it is asserted inside a document explicitly framed as "answered from THE
  SOURCE — do not assert from memory", and it is failure model #2 (confident prose about a
  checkable command behaviour, over-extended from the `add` branch to the whole tool). It was
  repeated to me tonight in the self-report, so the belief is live, not a one-off typo.
  CONFIDENCE: high

- CLAIM:    statusline.scala "states — TWICE — that 'the `?` marks it an inferred proxy (SM118)'",
  lines ~209 and ~325 (self-report #4). Fanned out to `PIN-BOARD.md:1430-1432` (muntabot repo,
  SM134 #5), which bold-quotes ":209 + :325 say twice, 'the `?` marks it an INFERRED PROXY
  (SM118)'".
  ARTIFACT: Line 209 has the phrase with "(SM118)". Line 325 reads "(the `?` marks it an inferred
  proxy)" — no "(SM118)". The parenthetical appears once, not twice.
  DELTA:    A paraphrase presented as an exact double-quote; the second occurrence lacks the SM118
  attribution.
  MATTERS:  Trivial for the argument (the ?-convention is real and line numbers are correct), but
  it is exactly the specimen class this push was told to hunt: quoted-as-exact, actually
  approximate, and already fanned out to a second repo.
  CONFIDENCE: high

VERIFIED (checks out, no divergence):
1. `tools/files.scala:41` filters with `p.toString.endsWith(ext)`; second arg is an extension
   suffix; a non-suffix pattern yields "0 files" with exit 0. Mechanism confirmed. (The claimed
   incident — believing `0 files` twice — is transcript-only, see CANNOT VERIFY.)
2. `tt mode` run live at push time: exactly one line, `tok-spend`. No hangover mode in state.
   Label regex `[A-Za-z0-9._-]+` at mode.scala:59 (but see finding 1 for its scope).
3. `hangover?` is a DERIVED mode (statusline.scala:287, :437); "a derived chip never enters the
   state file" (:294, verbatim); "it decays on its own" (:307, verbatim, in a source comment).
5. `hangoverSec = 60L` (statusline.scala:383); gap = now − last timestamped record
   (:123 comment + :129 implementation, any record carrying a timestamp advances `lastStamp`).
6. SM015-en-masse-subagents.md:26 has "a declared slice ... declared-focus slices"; :42 prescribes
   "Sub-agents WRITE results to files ... returning only a short pointer + summary". Verbatim.
7. long-lived-meta-minion.md:340-342 contains the self-caught overclaim, original wording
   "read-only, STRUCTURALLY", downgraded to "partial" because `Explore` keeps `Bash`; :41 and :583
   corroborate. Confirmed.
9. All 9 commits exist (42f5a5f, 575219c, 3378e56, ec295eb, 20feeaa, 0c22bed in genscalator;
   51a0ea3, d8e9a47, 255bca1 in muntabot-synch). Messages match the narrative; 0c22bed spot-checked
   in full (adds the 00:13 WR note, 128 insertions, content consistent).

CANNOT VERIFY:
- Self-report #8 ("three argument-shape errors in the first six tool calls"): the transcript is
  not available to me. CO4's own records repeat it (commit 0c22bed message; PIN-BOARD SM134 #1
  says "TWICE inside six calls" for the files errors specifically), but those are the same
  narrator — not independent evidence. Would need the session transcript.
- The specific `tt files` "0 files for a file that existed" incident, and "the agent had run no
  mode command this session prior" (nobody-dropped note :20): same reason, transcript-only.

OUT OF SCOPE:
- The protocol's [CS] (Runeson et al.) citations: not read, not verified, per the push brief.

PATTERN: none yet — first push, no cross-push baseline. Noted for future pushes: finding 1 is a
validation-scope over-extension (a checked property of one code path asserted for the whole tool);
watch for recurrence of that shape.
