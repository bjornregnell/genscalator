# WR data — how the `tt git pull --ff-only` tool gap surfaced (use-driven), 2026-07-07

**The ask (BR):** trace WHY we found the need for a new `tt git` option (`pull --ff-only` / `fetch`) and log it.

## The discovery chain
1. BR edited a README **on another box** (in fact via the GitHub web GUI) and asked CO4 to *"go pull (using
   `tt git`) my updates."*
2. CO4 found **`tt git` has no `pull`**: its safe subset is deliberately **add/commit/push only** (the usage/error
   surfaced it). CO4 fell back to the already-allowlisted `git -C <dir> pull` (clean fast-forward) and **flagged the
   gap** ("tt git can't pull").
3. BR asked (WDYT) whether we were *missing a `tt git` option or needed a new tool*.
4. CO4's verdict: not a new tool, but a worthwhile consistency add, a **safe `tt git pull --ff-only`** (+ read-only
   `fetch`). BR: *"go add --ff-only."* Implemented + tested + live (`6511cf9`).

## Why we found it (the causal analysis)
- **Use-driven, not audit-driven.** The gap was invisible until a **real workflow need** hit it: a **multi-box /
  web-GUI edit** that then had to sync into the local clone exercised a git operation (`pull`) the typed tool never
  covered. This is the same shape as the **`tt web get` file-output gap** (S2 in
  `command-hygiene-regression-2026-07-06.md`): *you cannot will-avoid a gap the tool does not cover* — the missing
  verb forces the raw-command fallback, which works but is off the disciplined typed path.
- **A design-tension the need exposed.** `tt git`'s safe subset was drawn **intentionally minimal**
  (add/commit/push; no pull/reset/rebase/merge/force) so `Bash(tt git *)` could never be a data-loss vector. That
  boundary was **slightly tighter than real multi-box use needs.** The resolution kept the safety intent by adding
  only the **narrowest still-safe verb**: `pull --ff-only` (never merges, runs merge hooks, or leaves conflicts, it
  fast-forwards or fails loudly) plus a read-only `fetch`.
- **This collaboration is inherently multi-box.** BR works across machines + the GitHub web GUI, so **sync (pull /
  fetch) is a recurring need**, which is exactly why the missing verb was felt here and not in a single-box flow.

## Lesson (feeds RT048 / RT049 + the structure-over-willpower synthesis)
Tool-coverage gaps surface through **real use, not inspection**; closing each with the **narrowest safe verb**
removes a friction/regression class outright (here: reaching for raw `git -C pull` instead of the typed tool),
independent of any discipline. Same family as design-implication 5 in
`SYNTHESIS-structure-over-willpower-2026-07-07.md` ("close the tt coverage gaps so the disciplined path always
exists"). [[commit-via-tt-git-not-raw-cd-git]] [[genscalator-toolbox-single-dispatcher]]
