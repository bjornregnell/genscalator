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
