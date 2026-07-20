# Meta-minion push 11 — audit of the SM142 media/ migration (2026-07-20 19:56, clock-read)

Task: check the agent's SM142 account against genscalator `b1f29f2` + `96a92c5` and work-repo `d15c819`.
Method: `git -C` show/log/status/branch, Read, `tt text grepr` (quoted patterns), `tt web get` — never the
agent's account. Cross-push context: push-9 (pre-warp ember audit), push-10 (post-warp, clean).

VERDICT: DIVERGENCE FOUND — the mechanics are solid (moves, links, refs, remotes all verified TRUE);
the divergences are all in the ACCOUNT's "only/exactly/matches-the-plan" framing, not in the work.

FINDINGS:

- CLAIM:    "the ONLY functional code change is `deploy/deployblog.sc` `blogDir = \"media/blog\"`"
  (also in the PB DONE annotation as "the plan's ONE functional change").
  ARTIFACT: `b1f29f2` also changes `media/design-language/DesignLang.scala:1323`:
  `"../../blog/_template.html" -> "../blog/_template.html"` in the outputs vector — this changes where
  the generator WRITES a file. That is a functional code change by any reading, and the account itself
  reports it in a separate bullet.
  DELTA:    The "only functional change" phrase is inherited verbatim from the plan
  (`todo/TODO-BIG-REPO-REFACTOR.md:38-39`, written 2026-07-17, BEFORE the DesignLang blog template
  existed 2026-07-20) and repeated without updating it against what was actually executed. The account
  contradicts itself: one bullet says "only", another bullet reports the second functional edit.
  MATTERS:  minor — the work is correct; but a later reader auditing "what could have changed behavior"
  would be told one site when there are two. Stale-plan language carried forward as if current.
  CONFIDENCE: high

- CLAIM:    "every executed item matches the plan section" (also commit `96a92c5`: "Every executed item
  matches the plan").
  ARTIFACT: The plan says `docs/foundations.md: no path-links into blog. Clean.` (plan line 45) — but the
  execution had to fix 3 blog-path sites in `docs/foundations.md`. The DesignLang output-path edit is
  likewise in no plan item. Both are correct deviations forced by work that landed AFTER the plan was
  written (design-language day, 2026-07-20).
  DELTA:    "Matches the plan" is true in the direction that matters (nothing planned was skipped) but
  false as stated: at least two executed items exist that the plan says would not be needed. The
  post-hoc verification pass did catch plan items the first commit missed (research links, issues
  pointers — credit where due), yet reported "matches" instead of "matches, plus the plan was stale in
  two spots".
  MATTERS:  minor — same family as F1: the plan's staleness is silently absorbed rather than named.
  CONFIDENCE: high

- CLAIM:    "doc refs updated in `docs/foundations.md` (2 sites)".
  ARTIFACT: The `b1f29f2` diff of foundations.md changes 3 lines, each with one blog-path rewrite:
  the SSG glossary entry (`blog/*.md` line + `blog/README.md` line) and the delegation-dance figure line
  (`blog/figures/seq-delegation-dance.svg`).
  DELTA:    3 path sites, not 2. (Defensible only if "site" meant glossary ENTRY — 2 entries — but the
  same account uses "site" to mean individual occurrence everywhere else: "16 href sites".)
  MATTERS:  trivial — all 3 were fixed; only the count in the account is off.
  CONFIDENCE: medium (the entry-vs-occurrence ambiguity)

## Verified TRUE (the load-bearing claims all check out)

- `b1f29f2` stat: 83 files, full `{blog => media/blog}` and `{pod-casts => media/pod-casts}` rename
  detection, untouched posts at 0 changes; `deployblog.sc => deploy/deployblog.sc` rename with the header
  usage lines now `scala-cli run deploy/deployblog.sc` and blogDir = "media/blog" (everything else in
  that diff is comments/usage text — within deployblog the one-functional-edit claim holds).
- History preserved: `log --follow media/blog/000-why-genscalator.md` reaches `2e625d9`, `df5f156`. TRUE.
- Link sweep END STATE clean: quoted-pattern greps over `media/blog` find zero remaining one-level
  `](../X` hrefs, zero one-level backticked `../X` texts, zero `../../../`. The rewriter
  `tmp/bump-blog-links.scala` exists with the claimed pre-asserts (no pre-existing `../../`, totals
  16+6); claimed per-file counts (0+3 / 3+3 / 6+0 / 4+0 / 3+0) sum to 16+6 and match the per-file
  changed-line counts in the stat. Triple-consistent.
- `96a92c5` touches exactly the 5 claimed files: 3 issues READMEs → `todo/TODO-BIG-REPO-REFACTOR.md`,
  research 028 References link and 037 figures link both bumped to `media/blog`. TRUE.
- Spot-checks: `README.md:155` one line, stale `https://bjornregnell.se/blog` retargeted to
  `/genscalator/blog` ✓; `docs/gs-registry.md` 1 site ✓; `work/EMBER-TEMPLATE.md` 1 site ✓;
  `tools/ssg.scala` 3 usage examples → `media/blog` ✓; work-repo `SKILL.md` exactly 2 absolute paths
  (lines 86, 150 area) ✓; PB SM142 DONE annotation ADDED above the pin (annotate-not-erase honored),
  process note about the compressed read-together step present, "Audited by meta-minion push-11"
  pre-announced ✓.
- References self-containment: `package blog` only in `media/blog/References.scala` +
  `References.test.scala`; zero mentions of References anywhere in `tools/*.scala`. TRUE.
- Repo state: both trees clean; genscalator origin/github/coursegit all exactly at `96a92c5`; work repo
  all three exactly at `d15c819`. The three-host push claim TRUE at tip position.
- No-redeploy: live `https://bjornregnell.se/genscalator/blog/` serves the generated template (000 + 002
  in the index — 002 live from its earlier promote, PB records it as `updated` awaiting BR's call).
  Consistent with "live site not touched by the migration".

## PATTERN (across pushes)

Third consecutive specimen of prose-quietly-exceeding-substrate (push-8 sweep-record gaps, push-9 F1
TIP/ACG widening, now the stale plan's "only functional edit" + "matches the plan" carried forward as
current fact). The mechanical layer keeps checking out perfectly; the drift lives one level up, in
summary sentences with totalizing words ("only", "exactly", "every ... matches"). Those words are where
to look next push.

CANNOT VERIFY:
- The test runs (305 tools tests, 8 References tests green) and the `tt ssg --status deployed` /
  deployblog `--dry-run` outputs — would require executing build/render, outside my read-only lane.
  Indirect support: clean tree + `_template.html` moved with 0 changes is consistent with a post-move
  regen whose output was unchanged.
- "Zero pre-existing `../../`" pre-state directly — accepted indirectly: the rewriter hard-asserts it
  before writing, it ran (per-file counts printed match the diffs), and the post-state has no `../../../`.

## Protocol self-check (§8)

Wrote ONLY this file. Note for the record: my first grepr batch went out with unquoted patterns and got
shell-mangled (a false triple-level "hit" that was any-char matching); caught and re-run quoted before
drawing conclusions. genscalator `status --short` after this write shows only
`.../minion-log/push-11.md`; work repo stays clean.
