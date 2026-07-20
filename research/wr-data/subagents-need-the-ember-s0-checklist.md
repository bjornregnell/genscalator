# Sub-agents need the ember's §0 anti-regression checklist

2026-07-20 22:20 (clock-read). BR observation during the SM145 cold-start
session ("WR data: sub-agent need §0 of ember").

## Observation
The ember's §0 (the forbidden→allowed checklist: tt text grepr not raw grep,
tt web get not curl, Write tool not redirects, tt git not cd-and-git, quoting
rules, English, timestamps read not guessed) exists because reflexes regress
at turn zero. A spawned subagent IS a turn-zero context — the same regression
applies, but tonight's two subagent briefs (push-13 minion, SM145 PR sandbox)
each carried a hand-written PARAPHRASE of §0 rather than the checklist itself.

## Why paraphrase is the risk
Prior specimen warming-covered-the-tool-not-the-quoting: a hand-warmed minion
got the tool choice right but broke on regex quoting — the paraphrase covered
what the author happened to think of. A canonical, reused §0 block covers what
the corpus has already learned, including the fresh specimens §0 accretes
(the one-ext-only tt files trap, the leading-# comment trap).

## Implication / candidate fix
Treat §0 as a reusable WARMING BLOCK, not ember-internal text:
- the ember template's §0 doubles as the standard subagent-brief preamble
  (paste wholesale, not paraphrased), or
- `gs warm` / the guard-clean digest becomes the single canonical source both
  embers and subagent briefs import from.
Tonight's sandbox-clone guard stall (see
sandbox-clone-has-no-guard-clean-shape-tt-git-lacks-clone) is the complementary
case: warming cannot fix a MISSING shape — so the pair of specimens splits the
failure space cleanly: missing shapes need new tt tools; existing shapes need
canonical (non-paraphrased) warming.

## Confounds
Neither of tonight's subagents demonstrably misfired from a paraphrase gap
(the clone stall was structural); this note is BR's forward-looking read plus
the prior warming specimen, not a fresh misfire.

## ADDENDUM 22:25 (clock-read) — the fresh misfire arrived minutes later
The confound above is now RESOLVED in favour of BR's read: the SM145 sandbox
subagent's own completion report honestly logs that two of its early searches
regressed to raw recursive grep (one with a "| head" pipe) and tripped the
guard; BR approved from the guard TUI. Cause fits the paraphrase-gap model
exactly: the caller's brief warmed the git-lane and egress constraints
carefully but never mentioned the search-tool lane — the author-paraphrase
covered what the author was worried about (repo safety), not what §0 knows.
A verbatim §0 block in the brief would have named tt text grepr and the
no-pipe rule. Specimen count for canonical-warming: 2 (quoting 2026-07-17,
search-lane 2026-07-20).

## ADDENDUM-2 ~22:40 — pipe-in-tt-arg, third specimen, same agent
BR (from the guard TUI, during his AFK wind-down): the same sandbox subagent
later put a pipe inside a tt tool argument ("pipe clobbery in tt tool arg") —
the exact §0 pattern-quoting rule (no (a|b) alternation, metachars out of
patterns) that the 2026-07-17 minion also broke. Notable: my follow-up brief
to this agent EXPLICITLY said "use tt text grepr not raw grep, no pipes to
head/tail/wc" — it obeyed the tool-choice line yet still tripped on quoting,
i.e. even a partial paraphrase that names the failure class adjacently does
not transfer the sibling rule. Verbatim §0 (which states the quoting rule
separately) remains the fix candidate. Specimen count: 3, two lanes
(tool-choice, pattern-quoting), one evening.
