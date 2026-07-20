# Meta-minion push 13 — audit of the pre-warp cold-start ember (2026-07-20 22:15, clock-read)

Task: adversarially check the 22:07-22:12 handover ember's factual claims (HEADs, sync, commit
lists, path resolution, memory writes, one live spot-check) against disk + git. NOT a re-audit of
the evening arc — push-12 covered that; this audits the handover DOCUMENT.
Method: `git -C` log/status/show/check-ignore, `tt files`, `tt text grepr` (quoted), Read,
`tt web get` (1 fetch), `tt chrono now` — never the ember's own say-so.

VERDICT: DIVERGENCE FOUND — one path-ambiguity finding (low stakes, already propagated once);
every other checkable claim VERIFIED.

## FINDINGS

- CLAIM: Ember §0 deploy bullet — "Stage exact files: tmp/stage-morph.scala (4 landing/security
  files → tmp/deploy-root), tmp/stage-file.scala (one file → tmp/deploy-root-brsite)" — bare
  `tmp/` paths with no repo prefix, in a bullet whose other paths are `<gs>/...`.
  ARTIFACT: Both scripts exist ONLY in genscalator: `<gs>/tmp/stage-morph.scala` and
  `<gs>/tmp/stage-file.scala` (`tt files <gs>/tmp scala`). The work repo's `tmp/` has neither
  (`tt files <work>/tmp scala`: 5 files, none of them).
  DELTA: The ember's bare `tmp/` is ambiguous between the two repos; the correct resolution is
  `<gs>/tmp/`. Evidence the ambiguity bites: my own spawn prompt (written by the main agent FROM
  this ember) resolved it to "in the work repo" — wrong repo, first reader, same evening.
  MATTERS: Low — a cold-start agent would find them in seconds. But an ember exists precisely to
  make paths unambiguous at turn zero, and this one already produced a propagated misreading.
  CONFIDENCE: high

## Verified TRUE

- **(a) Work repo HEAD + sync + clean.** `git -C <work> log --oneline --decorate -5`: HEAD
  `77d7b26` decorated `origin/main, github/main, coursegit/main` — synced to all three exactly.
  `status --short`: empty. The ember's "clean apart from THIS gitignored ember" checks out
  end-to-end: the ember lives at `<work>/tmp/resume-prompt.md` (its lines 23/25/31/34/44 match the
  pasted text) and `git check-ignore tmp/resume-prompt.md` confirms it is gitignored. TRUE.
- **(b) genscalator HEAD + sync + clean.** HEAD `fdb1782` decorated `origin/main, origin/HEAD,
  github/main, coursegit/main`; `status --short` empty. TRUE.
- **(c) Commit lists.** Work repo: `cc2170d`, `9cfa154`, `43bcd22`, `d3a0bb2` sit immediately
  beneath `77d7b26` in exactly that order — contiguous and exhaustive. TRUE. genscalator: the four
  named (`80ac91a`, `a0d2cca`, `546d1f0`, `898eaf2`) all exist beneath `fdb1782` in the ember's
  relative order, with two unlisted commits interleaved (`4762f2e` wordmark-seam, `b084b43`
  push-11-corrections annotate). A selective list, not a false one — but note the asymmetry: the
  work-repo list is contiguous-complete, the gs list silently elides two commits with no "among
  others" marker. Consistent with the substrate; not a divergence. TRUE (selective).
- **(d) Paths resolve.**
  - PIN-BOARD.md SM145 pin: `PIN-BOARD.md:167` holds both "🆕 SM145 PINNED (BR 2026-07-17)" and
    the "📌 SM145 ADDENDUM (BR 2026-07-20, ...) SCOPE REFINED + declared SAFE-SOLO" marker — the
    ember's "grep SM145 ADDENDUM" instruction works (matched by `tt text grepr`). The addendum's
    content matches the ember's compressed contract (sweep issues+PRs, diff vs BR's answers,
    sandboxed mergeability review, paste-ready drafts, post nothing). `9cfa154` is indeed the
    commit that wrote it (1-line change to PIN-BOARD.md). TRUE.
  - `<work>/notes/pr943-review-draft.md` exists. TRUE.
  - `<gs>/deploy/deployblog.sc`, `<gs>/media/design-language/DesignLang.scala`,
    `<gs>/media/blog/_template.html` all exist. TRUE.
  - `tmp/stage-morph.scala` + `tmp/stage-file.scala`: exist, in `<gs>/tmp/` — see FINDINGS for the
    ambiguity.
- **(e) The three memories.** `summaries-enumerate-dont-totalize.md`,
  `deploy-staged-files-never-mirror-media-whole.md`, `live-css-edits-need-hard-reload.md` all exist
  in the live agent-memory dir
  (`~/.claude/projects/-home-...-muntabot-synch-introprog/memory/`) with index lines at
  `MEMORY.md:141-143`. TRUE. Caller-phrasing note (not an ember error): my spawn prompt located
  them "under the work repo's memory/ dir" — the work repo DOES have a `memory/` dir, but it is a
  stale 14-file snapshot that contains NONE of the three; the live memory dir is the `~/.claude`
  one. The ember itself names no location, so the ember's claim is TRUE as written; the spawn
  prompt's location was wrong, and the stale in-repo `memory/` snapshot is a lurking
  wrong-turn for any future path-ambiguous reference to "the memory dir".
- **(f) Live spot-check.** `tt web get https://bjornregnell.se/`: the page carries
  `<a href="mailto:bjorn.regnell@cs.lth.se">bjorn.regnell@cs.lth.se</a>` (fetch line 268). The
  ember's "email now bjorn.regnell@cs.lth.se" is live-TRUE. (1 fetch, at budget.)

## PATTERN (across pushes)

Two pushes running, the account-register inversion push-12 logged holds: the ember pre-declares its
own audit ("trust the minion-log over this file's self-assessment"), carries per-line verify
commands, and every hash/state claim survived checking. The one finding is not a wrong FACT but an
under-specified PATH — and the interesting part is the propagation: the ambiguity reproduced in the
very first document derived from the ember (my spawn prompt), in two places (stage scripts' repo;
"work repo's memory/ dir" where a stale same-named dir exists). New shape worth watching: the
failure mode is migrating from wrong-claims (push-8/9/11 era) to ambiguous-references that stay
technically-true while steering a cold reader to the wrong repo. Cheap fix at the source: embers
always prefix `tmp/` and `memory/` with `<gs>/` or `<work>/` (or `~/.claude`).

## CANNOT VERIFY

- §1 usage claim ("BR reported 79% of the weekly Fable limit ~21:00") — a report of a human
  utterance; no substrate to check it against.
- The ember's timestamps "22:07-22:12 (clock-read)" — plausible (push-12 headed 22:06, my spawn
  ran ~22:13) but the writing moments themselves left no artifact.
- "LIVE + verified" for the surfaces beyond the root page (genscalator landing/security/blog/
  design-language) — would cost 4+ more fetches; push-12 verified the root page + style.css
  line-identical at 22:06 and I re-confirmed the root page at 22:14. The remaining surfaces rest
  on push-12's commit-level checks, not a fresh fetch.
- §2 holds and §5 queue — intent, out of scope per the tasking (path-existence checked where named:
  pr943 draft exists; `<gs>/tmp/br-site/` not checked, deletion is the offered action).

## Protocol self-check (§8)

Wrote ONLY this file. Both trees clean at audit start; work repo untouched. All commands read-only
(`git -C` log/status/show/check-ignore, `tt files`, `tt text grepr` quoted, `tt web get` ×1,
`tt chrono now`, Read).
