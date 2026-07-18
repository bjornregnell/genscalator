# The agent can create files freely but has no sanctioned path to DELETE one (2026-07-18)

**Event.** Making a blog stub, the agent first wrote it under a placeholder number
(`blog/0xy-blacksmiths-also-bootstrap.md`), then, on BR's call, re-created it correctly numbered
(`blog/029-...`). To finish the rename it needed to **delete the stale, badly-numbered copy** (`0xy-...`). It
could not:

- raw `git -C <repo> rm blog/0xy-...` was **hard-denied by the harness — twice, including AFTER BR explicitly
  granted verbal permission** (*"you have my permission to remove"*);
- `tt git` **deliberately excludes `rm`** — it is on the "not on the tool, by design" list with
  `reset`/`rebase`/`merge`/`--force`/`clean`, which is exactly what makes `tt git` safe to allowlist;
- plain `rm` would hit the same deny.

**Grounded (BR's hypothesis, confirmed by reading the config).** This is a **standing, deliberate policy, not
a sudden or mysterious loss** (BR's own "suddenly" was wrong). `.claude/settings.local.json`'s `deny` list
explicitly carries `Bash(git rm *)`, `Bash(git -C * rm *)`, `Bash(git reset --hard*)`, `Bash(git push
--force*)`, and `Bash(git clean -f*)` — a destructive-command deny-list added in a **prior session's
hardening**. It is *surgical*, not blanket: `rm -f .../tmp/*` is explicitly allowed for scratch cleanup; only
the dangerous shapes are denied. So the capability was never "lost" — it was deliberately never granted.

⇒ **the agent has NO sanctioned path to delete a tracked file.** Only the human, from their own terminal, can.

## The finding: a CREATE/DELETE capability asymmetry

The agent **creates and edits files freely** (Write/Edit, `tt git add`) but **cannot delete any file at all.**
This is not a tuning accident, it is the destructive-command guard working as designed. But it has a cost the
guard's framing hides: it **blocks legitimate cleanup too** — removing a duplicate or mis-named file **the
agent itself created.** The agent can make the mess and cannot clean it up; the human must. (Noted without
complaint: the asymmetry is a deliberate, defensible safety choice — but it *is* an asymmetry, and it has a
friction cost worth naming.)

## Two ties

1. **Security model, confirmed live.** The structural deny **held even against the principal's explicit verbal
   permission.** That is the `SECURITY-MODEL.md` symmetric-vigilance point in action: a destructive op does not
   become allowed because the human said so in chat; it would take an actual config change. Good (safety), and
   friction (a legitimate op blocked). A clean specimen of "the guard does not bend to chat-level say-so, even
   from the principal."
2. **A known tool gap, freshly motivated.** `broad-allowlist-aversion-2026-07-06.md` already proposed a
   no-clobber **`tt move` / `tt copy`** typed tool ("build a narrow typed tool, don't broaden the shell
   allowlist"). A scoped, refuse-clobber, git-aware **`tt rm` / `tt move`** would close exactly this gap: the
   toolbox owns the op, so neither a broadened allowlist nor a raw destructive command is ever needed.

## Resolution / next

- **Now:** the human runs `git rm` in their own terminal; the agent then commits `029`.
- **Candidate SM:** a safe `tt rm` / `tt move` (path-scoped to the repo, refuse-overwrite, git-aware delete)
  so the toolbox is self-sufficient for the ops it currently forces onto the human or a denied raw command.
  Fits beside SM146 (native tools / toolbox self-sufficiency).

Ties: [[no-clobber-human-owned-files]] · `SECURITY-MODEL.md` · `broad-allowlist-aversion-2026-07-06.md` · SM146.
