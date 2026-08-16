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

**Which `tt` does the rule match?** `Bash(tt *)` matches the bare command word only. If you run a
checkout's launcher by absolute path (e.g. `/home/you/genscalator/bin/tt`), the rule does NOT match
and every call prompts, even though it is the same tool (and often newer than a plugin-cache `tt`
that runs silently). Either put the checkout's `bin/` on your PATH so the command word is `tt`, or
add a path-scoped rule in the spirit of principle 2:
```
      "Bash(/ABSOLUTE/PATH/TO/YOUR/CHECKOUT/bin/tt *)"
```
(Found in the first alpha field test, 2026-07-28: the stale 0.9.1 plugin-cache `tt` ran silently
while the up-to-date checkout's `bin/tt` prompted, so the quiet path was the stale one.)

**First, establish WHICH `tt` wins.** Before trusting any of the above, run the read-only check:

```
tt which tt
```

It shows every `tt` on PATH — winner first, shadowed entries flagged — with ELF-vs-script and build
time in one read. Confirm the winner is the install you intend, and that you invoke it as the bare
word `tt` so `Bash(tt *)` applies. Three arrangements have field evidence, and which one you are in
changes both the rule you need and what works:

1. **Stale plugin-cache `tt` wins** (2026-07-28): quiet but OLD — the case described above.
2. **Native `~/.genscalator/bin/tt` wins** (2026-07-29): quiet AND current, but the native install
   tree ships no `skills/` by design, so bare `tt skillcheck` exits 2 there — recoverable from the
   error message itself since v0.10.1 (issue 015).
3. **A `~/.local/bin` symlink to a checkout's launcher wins**: quiet, current, full checkout — benign.

On Windows this check requires v0.10.1+ (issue 022: older `tt which` split PATH on `:` and reported
a false "not found").

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
