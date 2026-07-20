# Sandbox clone has no guard-clean shape: tt git lacks clone (and merge)

2026-07-20 22:18 (clock-read). Live specimen from the SM145 Hans-PR review, cold-start session.

## What happened
The sandboxed PR-review subagent (throwaway-clone workflow) needed
`git clone https://github.com/lunduniversity/introprog.git <scratchpad>` as its
first step. The guardcheck hook stalled on it; BR happened to be at the guard TUI
and approved ("in guard: sub-agent wanted to clone and i approved"). BR then asked
whether `tt git` could have done it guard-clean.

## Check performed
`tt git` usage (read 22:17): safe subset = commit / pull(--ff-only) / fetch / show.
No `clone`. Also no `merge` — and the same review workflow's next step is
merge-simulation (`git merge --no-ff` on throwaway branches), so BOTH sandbox
steps require raw git and will stall the guard.

## Reading
- The stall was STRUCTURAL, not a subagent-warming miss: no guard-clean shape
  exists for the clone step. Warming (per warming-covered-the-tool-not-the-quoting)
  cannot fix a missing tool shape.
- Had BR been AFK, the SM145 review would have hung at step 1 — the exact
  stall-risk the original SM145 pin flagged for egress, now reproduced for clone.
  Generalizes the front-load-before-AFK rule from web fetches to sandbox clones.
- Supports the already-pinned candidate (BR 2026-07-12, in the
  pr-review-sandboxed-subagent memory): a small allowlisted `tt` tool for
  sandbox-clone mechanics (throwaway clone into a scratch dir + PR-head fetch,
  possibly merge-simulate), which would make the whole review workflow
  guard-clean and AFK-viable. This specimen is its first live justification
  from the clone side.

## Confounds
Single occurrence; the guard config could in principle allowlist a narrow
`git clone https://github.com/...` shape instead of a new tt tool — but that
conflicts with the bare-allowlist policy (URL argument varies), whereas a typed
tt tool can constrain destination (scratch-only) and source (https only) by
construction.
