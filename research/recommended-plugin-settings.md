# Recommended Claude settings for genscalator plugin users

- **Question:** a human who installs the genscalator plugin wants the `tt` tools + the safe-by-design workflow to
  run with **minimal confirmation friction** but **without widening the attack surface**. Which
  `.claude/settings.local.json` permission rules should we *recommend* out of the box? What is the principled set
  — and how do we teach the human to grow it safely as they work?
- **Why it matters:** the plugin's payoff (no-CF, safe-by-design) only lands if the user's settings let the typed
  tools run silently. But settings are exactly where a user can accidentally over-grant (a broad `Bash(git *)` or
  `Bash(rm *)`) and undo the safety story. So "recommended settings" is itself a **safe-by-design deliverable**,
  not an afterthought. Grounded in BR's real settings + the allowlist we grew this session (mirrored, with its
  git history, in [`wr-data/settings-local-mirror.json`](wr-data/settings-local-mirror.json)).
- **Status:** open (2026-07-02). Initial principles below; the concrete recommended block is drafted in the
  repo README ("Claude Code settings") as a first cut.

## Principles (from what we did + BR's settings)
1. **Allow the typed tools, not raw shell.** Recommend `Bash(tt *)` (or per-subcommand `Bash(tt text *)` etc.) and
   `Bash(scala-cli *)` — so the safe, statically-analyzable tools run silently, and the *unsafe* raw equivalents
   (`grep -rnE`, pipe-chains) still prompt. This is the CF thesis operationalized in config.
2. **Scope by absolute path, never broad verbs.** `Bash(git -C /abs/repo *)` per repo, NOT `Bash(git *)`;
   `Bash(rm -f /abs/repo/tmp/*)` for gitignored scratch, NEVER `Bash(rm *)`. Path-scoping keeps the blast radius
   legible (the AFK-git-loosening + scoped-rm-tmp we did are the worked examples).
3. **Keep destructive + catastrophic gated.** `push --force`, `reset --hard`, `rm -rf`, and anything outside the
   trusted repo tree should stay in `ask`/`deny` even for a user who wants low friction. (Candidate: ship a
   recommended `deny` list too.)
4. **Config in args, not env** — nothing here should rely on ambient env vars ([[tt-typed-args.md]],
   PRD `configInArgsNotEnv`); the allowlist is auditable precisely because commands are literal.
5. **Grow the allowlist deliberately, and record why.** Each "don't ask again" the user grants is a trust-boundary
   decision worth capturing (this is the settings-mirror idea — [[settings-local-mirror]] — that a user could
   optionally adopt to see their own allowlist evolve).

## Open questions
- **What is the *minimal* recommended block** that makes the plugin pleasant without over-granting? (Draft in
  README; needs pruning to essentials — a user shouldn't copy BR's whole 80-line project-specific list.)
- **Tiers?** e.g. a "safe defaults" tier (tt + scala-cli read-only-ish) vs an "autonomous" tier (adds scoped
  git add/commit/push) the user opts into consciously — mirrors the present-vs-AFK autonomy we tuned live.
- **Can the plugin ship a recommended settings *fragment*** the user merges, rather than prose to hand-copy?
  (A `tt init-settings` that prints a scoped block for the user's repo paths — safe-by-construction scaffolding.)
- **Per-repo path templating:** the scoping needs the user's absolute repo paths; how to generate those without
  the user hand-editing (and fat-fingering a too-broad glob)?
- **Deny-list recommendation:** should the plugin recommend explicit `deny` rules (force-push, hard-reset,
  rm-rf) so low-friction users still can't foot-gun? Likely yes.

## Relation to other threads
Directly serves [`confirmation-guard-static-analysis.md`](confirmation-guard-static-analysis.md) (§4: a `tt`
command should be provable-safe → silent) and the BHH threat model (never trade safety for a quieter prompt).
The sub-agent propagation question ([`subagent-genscalator-propagation.md`](subagent-genscalator-propagation.md))
is the delegation-side sibling: settings are how the *human* grants trust; sub-agent config is how *that trust
plus the methodology* reaches delegated work.
