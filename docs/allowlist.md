# genscalator recommended allowlist

The safe-by-design payoff (typed tools run without a confirmation prompt each time) only lands if your
Claude Code permissions let the `tt` tools run silently while keeping raw shell and destructive ops gated.
This doc is the single source of truth for the recommended `.claude/settings.local.json` permissions.

- `gs help allow` prints this doc (no changes made).
- `gs allow` applies it to the CURRENT repo: it fills in this repo's absolute path for the scoped rules,
  MERGES the block into `.claude/settings.local.json` (it adds to your existing permissions, it does not
  overwrite them), SHOWS you the exact change, and you approve it before it applies. It is never done silently.

## The two tiers

Pick the tier you want; `gs allow` defaults to Tier 1 and adds Tier 2 only if you ask for it.

**Tier 1 — safe defaults (recommended for everyone).** The typed tools run silently; the unsafe raw
equivalents (`grep -rnE`, pipe-chains) still prompt.
```
{
  "permissions": {
    "allow": [
      "Bash(tt *)",
      "Bash(scala-cli *)"
    ],
    "deny": [
      "Bash(rm -rf *)",
      "Bash(git push --force *)",
      "Bash(git reset --hard *)"
    ]
  }
}
```
Add `"Bash(scalex *)"` to `allow` if you use the scalex companion.

**Tier 2 — autonomous (opt in consciously).** Adds path-scoped `git` and scratch-`rm` for THIS repo, so the
agent can commit and clean its own `tmp/` without prompting. `gs allow` fills in the absolute path for you.
```
      "Bash(git -C /ABSOLUTE/PATH/TO/YOUR/REPO *)",
      "Bash(rm -f /ABSOLUTE/PATH/TO/YOUR/REPO/tmp/*)"
```

## Principles (why the block looks like this)

1. **Allow the typed tools, not raw shell.** `Bash(tt *)` + `Bash(scala-cli *)` make the safe,
   statically-analyzable tools silent; the unsafe raw equivalents still prompt.
2. **Scope by absolute path, never broad verbs.** `Bash(git -C /abs/repo *)` per repo, never `Bash(git *)`;
   `Bash(rm -f /abs/repo/tmp/*)` for gitignored scratch, never `Bash(rm *)`. Path-scoping keeps the blast
   radius legible.
3. **Keep destructive and catastrophic gated.** `push --force`, `reset --hard`, `rm -rf` stay denied even
   when you want low friction, so a low-friction setup still cannot foot-gun.
4. **Config in args, not env.** Nothing here relies on ambient environment variables; the allowlist is
   auditable precisely because the commands are literal.
5. **Grow the allowlist deliberately, and record why.** Each "do not ask again" you grant is a trust-boundary
   decision worth capturing.

## Notes

- This doc is the source of truth; the README "Recommended Claude Code settings" section points here (do not
  duplicate the block in two places that can drift).
- Full background and open questions: `research/018-recommended-plugin-settings.md`.
