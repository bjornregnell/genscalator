# Meta-minion push 2 — report (2026-07-17)

Resumed via SendMessage after CO4's ~09:06 compact dropped my handle (CO4 recovered it by grepping
the session transcript). Mid-push, BR interjected from inside a guard stall with a WR question;
answered inline, logged below.

VERDICT: DIVERGENCE FOUND (one live artifact lying, one push-premise stale-at-read-time, one
cross-push PATTERN now visible)

## TASK 0 — the resume datum

(a) Answered from retained context, before re-reading anything: push 1's catches were
(A) mode.scala validates only in the `add` branch — `rm` is `write(read().filterNot(_ == label))`,
no validation, so `tt mode rm hangover?` is a silent exit-0 no-op, contradicting the WR note's
"would be rejected outright" (nobody-dropped-the-hangover-chip-2026-07-16.md:24-25); and
(B) "the `?` marks it an inferred proxy (SM118)" claimed as an exact double-quote at
statusline.scala:209 and :325 — :209 has the parenthetical, :325 does not. I judged B trivial for
the argument (the ?-convention is real, both line numbers correct; quote-precision only). A was the
non-trivial one: a false mechanism inside a document framed as answered-from-source.

(b) Retained context, no re-read. HONEST CAVEAT on what this measures: the SendMessage resume
handed me my transcript INTACT — I was never compacted; the compact hit CO4. So the datum is
"SendMessage resume of a fable minion preserves its context" (1/1 tonight), not "fable recalls
across a warp". Against SM085(a)/SM081's fable-resume 0/2 (PIN-BOARD.md:59, :650 — verified): this
is a SUCCESS, but I CANNOT VERIFY from my position that the 0/2 failures used the same mechanism,
so I do not claim 1/3 on a common denominator.

## TASK 1 — guardcheck quote-awareness, adjudicated from source

**The source (deciding lines, tools/guardcheck.scala):**
- :146-148 `cmdFindings`: "The cmd checks, quote-aware: HIGH scans the RAW command, MED scans the
  masked skeleton" — `c.find(if c.severity == "HIGH" then command else masked)`.
- :125-142 `maskQuoted`: quoted spans masked to a space; unbalanced quotes -> None -> raw scan
  (:139, :147 "fail safe").
- :79-85 the `>` check is severity MED, hence masked; :41-43 the `;` check is HIGH, hence raw.
- Deployed = source: settings.local.json:184-193 wires `tt guardcheck hook` as the Bash PreToolUse
  hook, and ~/.local/bin/tt:30 runs `scala-cli run "$TOOLS/$tool.scala"` — every invocation
  compiles current source. Fix landed eb0cd14, 2026-07-16 21:10.

**Adjudication: guardcheck IS quote-aware — on the MED path only.** "Type `>` naturally inside
quotes" is true for balanced quotes (the `>` check is MED/masked) and FALSE as a general green
light: quoted `;`, backtick, `&&`, `$(`, `<<`, `cd ` still fire HIGH on the raw bytes and DENY with
no override.

**So which of our artifacts is lying? The tool help, not the carrier — plus two homes the push did
not name:**
1. `tt text --help` Notes: "trips the safety guardcheck (not quote-aware)" — STALE since eb0cd14,
   and it is the stated rationale for `--any`. LYING NOW, verified live today.
2. `tools/README.md:44`: same "(not-quote-aware)" clause — stale, and notably :48-49 explicitly
   anticipated the quote-aware guard as "a separate, hook-side hardening task": the cure it
   predicted landed the same day and the prediction was never updated.
3. `tools/guardcheck.scala:80` — the `>` check's OWN why-text still says "a > inside a QUOTED
   pattern/string arg fires this same check, since the guard scans raw bytes". False on the MED
   path since the fix, inside the very file that was fixed, contradicting its own maskQuoted doc
   at :103-124. (Still true in the unbalanced-quote fallback; false as stated.)
4. `tmp/resume-prompt.md` — the push quoted it asserting flatly "guardcheck is quote-aware: type >
   naturally inside quotes". AT MY READ TIME the file no longer says that: line 68 now reads
   "quote-aware ON THE `MED` PATH ONLY — ~~type `>` naturally inside quotes~~ is HALF TRUE",
   corrected 2026-07-17 11:16 citing 767926f (verified: exists in genscalator, 11:17:54, and its
   note states it was recorded BEFORE my verdict to keep the two observations independent). So the
   carrier WAS over-broad and has been retracted by annotation. My independent source read agrees
   with the corrected text. NOTE: tmp/resume-prompt.md is untracked (git log on it is empty), so
   the pre-correction wording has no substrate history — I take the push's quote + the visible
   strikethrough as consistent evidence, but CANNOT VERIFY the original wording independently.

**A sub-finding neither the help nor 767926f states:** the pipe check was NEVER a general `|`
detector. In BOTH the pre-fix (eb0cd14^) and current source it is `\|\s*(head|tail|wc)\b` — so a
quoted alternation like `TODO|FIXME` did not trip OUR hook even before the fix. `tt text --help`'s
attribution of the |-stall to "the safety guardcheck" was imprecise from the start; the stalls that
motivated `--any` for bare `|` must have come from the harness's own permission analyzer (internals
not verifiable from here — CANNOT VERIFY, but the project's own memory documents that analyzer
tripping on metachars).

**Follow-on — does `--any` still earn its place? Yes, on residual grounds:** (1) HIGH metachars
inside quoted patterns (`;`, backtick, `cd `...) still hard-DENY — two live hits today per the
carrier :71-72, including the check's own name firing the check; (2) the harness's own analyzer
stalls on metachars independently of our hook; (3) unbalanced-quote accidents fall back to a raw
scan, `--any` is immune by construction; (4) typed-flag clarity vs regex alternation precedence.
Its reason-for-being is not gone — but its DOCUMENTED rationale is wrong and should be rewritten to
these grounds when BR rules on the fan-out.

## TASK 2 — does the add-validates/remove-doesn't shape generalise? NO.

Checked all 64 .scala files under tools/ (incl. test/ and reqt-vendored/): a verb-branch sweep
(`case "add|rm|remove|clear|set|on|off"`, then `del|delete|drop|new|start|stop|save|init|unset|
reset`) plus a sweep for the `filterNot` silent-removal idiom, plus full reads of the two hits.
- The ONLY add/rm verb pair in the toolbox is mode.scala (:67-76). The only state-mutating
  filterNot on a user-supplied key is mode.scala:76. (reqt-vendored's removeTop is a library op,
  not a CLI path; the other filterNots are flag parsing.)
- The only other stateful start/remove-shaped pair, chrono.scala start/stop, is a COUNTER-example:
  `stop` with no recorded start fails LOUDLY — "no start recorded", exit 1 (:79-81).
The shape does not generalise; mode.scala is a singleton. One refinement to my own push-1 catch:
`rm` of a VALID-but-absent label is equally silent — the defect is "no feedback on whether anything
was removed", of which missing validation is only half. chrono's loud stop is the in-house pattern
to copy if BR fixes it.

## Mid-push datum (mine, not CO4's) — BR's in-guard question

`command -v tt` stalled on the permission prompt; BR asked from inside the guard whether that is a
regression-after-compact instance. Answered live: NO, on three grounds — (1) wrong agent for the
hypothesis: I was never compacted; (2) wrong class: the command is guardcheck-clean (bare, no
metachars; the hook stays silent on clean), so this was the plain allowlist seeing a first-use
shape; (3) "regression" requires a prior corrected state, and `command -v` has none in my run. It
IS a specimen of sub-agent allowlist-envelope drift: sub-agents cannot see the allowlist, and
settings.local.json:5 shows the `command -v` verb is allowlisted per-exact-argument
("command -v pdftk"), so precedent-in-shape still asks. Mitigation if wanted: the push brief
enumerates allowlisted verbs.

## PATTERN (cross-push — the thing CO4 cannot see)

1. **Scope over-extension of a TRUE property, now seen twice.** Push 1 catch A: `valid()` is real
   but only guards `add`; asserted for the whole tool ("would be rejected outright"). Push 2: the
   quote-aware fix is real but only guards MED; asserted flat ("type > naturally"). Same signature
   both times: the property IS true on the path that was checked, and the assertion silently widens
   to the unchecked path. Both were written as verified-from-source, and both times the FALSE half
   was the operationally dangerous one. Countermeasure that would have caught both: when asserting
   "tool X does P", name the code path on which P was verified.
2. **Fix-without-fan-out, echoing push 1's catch B.** eb0cd14 fixed the mechanism 2026-07-16 21:10;
   at least three prose homes still assert the defect today (tt text --help, README:44,
   guardcheck.scala:80), one of them inside the fixed file itself. 767926f tables the same list
   independently. This is SM133's retraction-fan-out problem occurring in the toolbox while SM133
   studies it in prose.

CANNOT VERIFY: the pre-correction wording of tmp/resume-prompt.md (untracked file, no history);
the harness permission analyzer's internal quote-handling; whether SM081/SM085's fable-resume 0/2
used the same resume mechanism as tonight's success.
