# History-rewrite tooling vs toolbox purity — a rare-but-critical-mutation tension (2026-07-24)

**Trigger.** A `git` history rewrite was needed (strip six `Co-Authored-By:` trailers so a
co-author stops showing on the public contributor graph). The agent reached for the purpose-built
external tool `git-filter-repo` and ran `pip3 install --user git-filter-repo`. The guard STALLED it
(interpreter + install of a non-repo executable) — working as designed on a consequential action —
and BR used the stall to reopen the method: *is a heavy external Python tool the right move, given
history rewrite is not a typical genscalator thing, yet it is a critical irreversible mutation
where a safe tool matters MORE, not less?*

**The tension (BR's, sharpened).**
- History rewrite is OFF the genscalator daily-loop path (genscalator = safe typed tools for the
  common inner loop; a full-history rewrite is rare surgery).
- But it is exactly the RARE + IRREVERSIBLE + PUBLIC-BLAST class where a safe, typed, reviewable,
  backup-forcing tool has the highest value PER USE — the opposite of "too rare to bother".
- So "not typical genscalator" argues against a toolbox verb, while "critical mutation" argues FOR
  one. Both are true.

**Technical finding (could a scratch tool have done it easily?).** Yes, moderately — and it would be
genscalator-native. `git-filter-repo` is itself a wrapper over `git fast-export` → transform →
`git fast-import`. The needed transform here is a pure line-drop (remove message lines matching the
trailer), which is precisely genscalator's read→transform→write shape over git BUILTINS + the JDK —
no Python, no external dep. A ~50-line Scala driver over `git fast-export --all | <drop-lines> |
git fast-import --force` covers the drop-a-trailer / rewrite-author-email class. The round-trip is
git's own guarantee; the only mutation is the dropped lines, so the risk surface is small IF a
dry-run + diff verifies before the ref update.

**Where the external tool still wins.** filter-repo has years of edge-case handling (signed tags,
encodings, marks, replace-refs, path/subtree rewrites, blob purging). For EXOTIC rewrites the
proven tool beats a scratch script. The [[dependency-preference-cascade]] resolves it: hand-roll
(small, over builtins) is preferred over a big lib, UNLESS a correctness/safety gate overrides — and
for an irreversible public mutation that gate CAN override. The nuance surfaced today: for the
SIMPLE, high-frequency-enough rewrites (drop trailer / fix author), a thin typed verb is BOTH pure
AND safe (small risk surface + forced backup/dry-run), so the cascade is NOT overridden; the
override is reserved for genuinely exotic surgery.

**Design implication (→ SM218).** A `tt git`-family verb (e.g. `strip-trailer` / `rewrite-msg`)
built on fast-export/fast-import, JDK-only, with MANDATORY: (a) backup bundle first, (b) dry-run
diff of message changes, (c) an [audit] line, (d) refuses without explicit confirm, (e) never
touches remotes (human pushes). filter-repo stays a documented ESCALATION for exotic cases, not a
vendored dep. This instances [[match-complexity-to-task-not-agent-elegance]] and
[[dependency-preference-cascade]] on a critical-mutation example.

**Meta.** The guard stall was the useful event — it converted an autopilot `pip install` into a
human-in-the-loop design decision on a rare, irreversible operation. A specimen of the guard
adding value not by blocking danger but by FORCING deliberation at exactly the right moment.
