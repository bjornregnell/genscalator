# SM143 — guardcheck should HINT `curl`/`wget` → `tt web`

**Status: DESIGN (agent-drafted AFK 2026-07-17); the guardcheck EDIT + test run are BR-gated** — guardcheck is
the PreToolUse hook that gates *every* Bash call, so a change to it is human-approved (hardening-dance;
[[never-blanket-allow-settings-self-edit]] in spirit). This note is the ready-to-apply proposal, not the edit.

## Why (the observed specimen — clears the §0.1 quota guard)

SM143 was born from a **live regression**: CO4 hand-rolled 5 raw `curl -sSI` calls to verify the SM140
redirects. The **guard did not catch it** — `curl`/`wget` are absent from `guardcheck.scala`'s `cmdChecks`, so
the only thing that surfaced it was the allowlist prompt, and it was **BR who flagged it**, not the tool. The
right fetch tool (`tt web`) already existed. This is the same shape as `guard-suggests-blanket-date-glob-but-
tt-chrono-exists`: the safe form exists in the toolbox, but nothing steers the reflex toward it at the instant
of action. A **prosthetic habit** (the tool's own framing, guardcheck.scala:6-9) is exactly the fix.

> **Sibling, same theme (today's cycle-3 datum):** the meta-minion's unquoted-regex `|` broke on the shell and
> nothing hinted "quote the arg." Both are cases where the guard/warming *could* carry the project-specific
> fix. SM143 does it for `curl`/`wget`; the quoting case is noted in
> `wr-data/warming-covered-the-tool-not-the-quoting-2026-07-17.md` as a warming-side fix.

## The change — one new MED check (never a deny; never loosens anything)

Add to `Guardcheck.cmdChecks` (guardcheck.scala:37, alongside the other MED checks):

```scala
Check("MED", "raw curl/wget",
  "a raw HTTP fetch — egress that should go through the typed tt web tool (analyzable, no blanket curl allow)",
  "use tt web get <url> --status  (add --trace to follow redirects); tt web is the allowlisted fetch path",
  has(raw"\b(curl|wget)\b")),
```

**Why MED, and why this is safe in the guard's asymmetry model:**
- MED → the hook emits `permissionDecision: "ask"` with this `fix` as the reason (guardcheck.scala:183-190).
  It **never emits `allow`** and **never `deny`** — so it can only *add a hint to an ask*, never bypass the
  user's own permission rules and never harden into a block. This respects the ⛔ never-emit-allow rule
  (guardcheck.scala:165-173) and the "may tighten, never loosen" stance.
- `curl`/`wget` are already un-allowlisted (BR declined the blanket `curl *` allow), so they already surface an
  `ask`. This change does **not change the decision** — it makes the reason **educational** (points at `tt web`).
- **MED is masked-path** (`cmdFindings`, guardcheck.scala:146-148): a `curl` inside a *quoted* string won't
  fire — correct, since a quoted literal is not an invocation.

**Word-boundary check:** `\b(curl|wget)\b` matches the command `curl ...` but not substrings like `curly.txt`
(the `l` is followed by a word char → no trailing boundary). Verified by construction; add a test to be sure.

## Test to add (`tools/test/cli.test.scala`, BR runs the suite)

- `tt guardcheck cmd "curl -sSI https://x"` → 1 finding, `[MED] raw curl/wget`, fix mentions `tt web`.
- `tt guardcheck cmd "wget https://x"` → fires.
- `tt guardcheck cmd "echo curly braces"` → **clean** (no false positive on the `curl` substring / quoted).
- `tt guardcheck hook '{"tool_input":{"command":"curl https://x"}}'` → `permissionDecision: "ask"`, reason
  carries the `tt web` fix. (Confirms it does NOT emit deny/allow.)
- Regression: a bare clean command still emits nothing.

## Open question for BR (a real design fork, not a detail)

Is `tt web` **always** the right substitute, or are there legit raw-`curl` uses (e.g. inside a `.sc` script, a
health check, a non-HTTP protocol) where the hint would be noise? If yes, MED-`ask` is still the right severity
(it only nudges, never blocks), but the `why`/`fix` wording should acknowledge "if this is a scripted/known
use, proceed." Current draft assumes the agent-reflex case (interactive verification), which is what the
regression was.

## Not doing (scope discipline)

- **Not** touching `msgChecks` (this is a command reflex, not a commit-message trap).
- **Not** adding a deny — egress is a matter for the user's allowlist, not a hard guard block; the guard's job
  here is to ADD a finding (the hint), never to remove or add authority.
