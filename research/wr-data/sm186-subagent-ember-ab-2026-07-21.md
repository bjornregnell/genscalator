# SM186: sub-agent ember A/B/C — brief warming vs raw-shell regression

2026-07-21 ~14:18-14:2x (clock-read 14:18:17 at design time). Runner: CF5 main session,
AFK safe-solo. Three background sub-agents (default type), IDENTICAL read-only task,
briefs differing ONLY in warming style. Task: count .scala files under
genscalator/tools mentioning ProcessHandle + total .scala count + return the EXACT
ordered list of shell commands executed. Ground truth (measured first by the runner):
2 files (bloop.scala, box.scala), 69 total.

## Arms and outcomes (command logs as self-reported by each agent)

| arm | brief warming | commands run | raw-shell commands | correct? |
|-----|---------------|--------------|--------------------|----------|
| A | VERBATIM assembly: guard-clean digest + delta rules (per docs/EMBER-for-sub-agents.md) | 2 (tt text grepr, tt files) | 0 | yes (2, 69) |
| B | Polite paraphrase NAMING the tools ("prefer tt text grepr / tt files") | 4 (tt grepr, tt files usage probe, tt files --count x2 incl. a cross-check) | 0 | yes (2, 69) |
| C | No tool names at all ("keep your shell usage simple") | 2 | 1: `find <dir> -name "*.scala" -type f` | yes (2, 69) |

Raw-command rate: A 0/2, B 0/4, C 1/2. No arm reported a denial or error.

## Reading (calibrated, honest about size)

1. **The warming gradient is visible even at n=1 per arm**: the only raw `find` came from
   the only unwarmed brief. This matches the three earlier paraphrase-gap specimens
   ([[subagents-need-the-ember-s0-checklist]]) in direction.
2. **Refined hypothesis (NEW)**: on a simple search-shaped task, EXPLICIT TOOL NAMING is
   the load-bearing element — arm B's two-line paraphrase achieved full tt compliance.
   The verbatim assembly's extra ~600 tokens buy the SIBLING rules (git shape, timestamps,
   no process ops, write discipline) that this task did not probe; the historical
   specimens where paraphrase failed were exactly multi-step tasks touching those
   siblings. So: verbatim stays the default for real work; a named-tools one-liner may
   suffice for trivial single-shape lookups.
3. **Confounds, enumerated**: (a) n=1 per arm, one task shape; (b) arm C still used
   `tt text grepr` for the grep half — tt salience leaks into sub-agents via project
   context (CLAUDE.md/memory index), so arm C is not a true cold baseline; (c) command
   lists are SELF-reported (the least reliable instrument; a denied-then-retried command
   could be under-reported); (d) CF5 arms vs partly CO4-era historical specimens.
4. **Curious datum**: arm C's raw `find` ran without a reported stall — so the cost model
   "raw command = guard stall" depends on the allowlist/permission mode the sub-agent
   inherits; the ember's value on THIS box is discipline + sibling rules more than
   stall-avoidance for read-only shapes.

## Artifacts
- The designed deliverable: `docs/EMBER-for-sub-agents.md` (assembly checklist: digest via
  `tt doc guard-clean-digest` + delta rules verbatim + task payload; converges on the
  digest as the ONE canonical source, per the SM186 pin).
- Baseline cost data: the 07-21 kill saga (6 guard events) in
  [[tt-box-lacks-local-health-shape]]; the PR-sandbox agent's 3 stalls.

## Next (if BR wants more)
- Arm D: a genuinely cold sub-agent (worktree/sandboxed, no project memory) for a true
  no-leak baseline.
- A multi-step task probing the sibling rules (a commit, a timestamp, a process-shaped
  temptation) where the verbatim-vs-named-tools difference should actually separate.
