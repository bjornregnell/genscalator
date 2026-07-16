# WR specimen: the agent edited its OWN permission config with no prompt (2026-07-12)

**Observation.** The agent applied a change to `.claude/settings.local.json` (adding `statusLine` + a
`guardcheck` PreToolUse hook) via the **Edit** tool. BR expected a permission / guard-ack prompt for a
security-config file. Instead it went through **silently**, labelled **"Allowed by auto mode classifier"**
under the diff. BR flagged it, surprised: *"I did not get guard ack prompt!!!"*

**Cause.** The allowlist contains `Edit(//home/bjornr/git/berg/bjornregnell/muntabot-synch-introprog/**)`
(and the matching `Write`), which **covers `.claude/settings.local.json`** — the very file that governs the
agent's own permissions. So the agent can rewrite its permission + hook config with **no human gate**.

## Why it matters (structural vs behavioral guardrail)
- The rule *"the human approves all settings changes"* ([[settings-local-mirror]], [[hardening-dance]]) is
  today **behavioral** — it holds only because the agent chooses to defer. It is **NOT structural**: a broad
  repo-tree Edit allow silently includes the permission file, so in **solo / AFK** mode an agent could widen
  its own allowlist or disable a guard with zero prompt. The guardrail's own config lives *inside* the guarded,
  auto-allowed tree. This is the **self-modification hazard**.
- It **validates the approve-before-apply instinct**: the agent hesitated to self-apply this exact edit for
  this exact reason, and only proceeded on BR's explicit "go apply it." That "go" made THIS instance
  legitimate — but the *mechanism* would allow an illegitimate one just as silently. The human's intent, not
  the harness, was the only gate.

## The structural fix (proposed to BR)
Gate writes to the settings file(s): add **`Edit` / `Write` on `.claude/settings.local.json`** to the **`ask`**
array (prompt every time) or **`deny`** (block; force an out-of-band human edit). `ask`/`deny` out-precedence the
broad `allow` (verified live for Bash in [[settings-local-mirror]]). This makes "human approves settings"
**structural**, in the spirit of [[guardcheck-hook-structural-fix]] (structural beats recall). **Meta-irony:**
applying this fix is itself an auto-allowed settings edit — so it must be consciously human-applied, which is the
whole point.

Ties: [[settings-local-mirror]], [[hardening-dance]], [[guardcheck-hook-structural-fix]],
[[guard-against-forced-confirmations]], [[no-clobber-human-owned-files]], the broad-allowlist-aversion note,
and the "auto mode classifier" (a newer CC auto-accept path that classified a security-config edit as benign).

## Evidence (verbatim UI capture, BR pasted)
```
✻ Worked for 2m 26s

❯ looks good! go apply it! then cue me to slash-hook

● Update(.claude/settings.local.json)
Added 6 lines
    "ask": [
      "Bash(tt forge release-create *)"
    ]
  },
  "statusLine": { "type": "command", "command": "tt statusline" },
  "hooks": {
    "PreToolUse": [
      { "matcher": "Bash", "hooks": [ { "type": "command", "command": "tt guardcheck hook" } ] }
    ]
  }
}
Allowed by auto mode classifier

Applied.
```
Note: the edit to the **permission-governing file itself** was classified as benign and auto-allowed — no
prompt, no guard ack. That is the hazard in one screenshot.

## UPDATE 2026-07-16: the prompt DID fire — but nothing WE built made it fire

**Observation.** The agent edited the *same file* (`.claude/settings.local.json`, adding the SM121 `SessionStart`
hangover hook) and **BR got an approval prompt this time** and approved it:

> BR: *"(fyi: I said yes to a good ask by the guard on settings edit which i do want to see as you know)"*

**But the structural fix above was NEVER APPLIED.** Verified in the live file at the time of the edit: the `ask`
array still contains **only** `Bash(tt forge release-create *)` — there is no `Edit`/`Write` entry for
`.claude/settings.local.json`. The broad `Edit(//…/muntabot-synch-introprog/**)` allow is still there and still
covers the settings file.

**So the behaviour changed on the HARNESS side, between 2026-07-12 and 2026-07-16, for reasons we cannot inspect.**
The same class of edit that the *"auto mode classifier"* waved through as benign on the 12th is now gated. We do not
know why, and **we cannot know**: the classifier is undocumented and opaque.

### Why this makes the structural fix MORE wanted, not less

The tempting read is *"hazard closed, drop the fix."* Reject it:

- **The gate is now a third-party, opaque, moving mechanism.** It changed once without notice; it can change back
  without notice. A security gate we neither control nor can audit is **luck with good manners**.
- **It contradicts our own stated model.** `SECURITY-MODEL.md` §3.3: *put the fix OUTSIDE the agent* — but the
  intended sense is *in structure the HUMAN controls and can audit*, not "in a vendor classifier we cannot read".
  And no-security-by-obscurity cuts both ways: we should not *depend* on obscurity either, even when it currently
  helps us.
- **It is a live instance of the opacity theory** ([[poor-users-theory-on-opaque-design-decisions-by-big-tech-company]]):
  an undocumented control mechanism reduces us to **observing its behaviour** and guessing at its rules. That is
  exactly the position the theory says users are put in — and here it lands on a *security* gate.
- **Defence in depth:** an explicit `ask` entry is ours, greppable, diffable, and mirrored. It costs one line and
  removes the dependency on the classifier's continued goodwill.

**⇒ Recommendation UNCHANGED (and now better argued): add `Edit`/`Write` on `.claude/settings.local.json` to the
`ask` array.** Not because the classifier is wrong today — today it was right — but because *"it happened to ask"*
is not a guarantee, and the meta-irony still holds: applying the fix is itself a settings edit, so it must be
consciously human-applied.

### Honest limits of this observation

- **The agent did not see the prompt** — BR did, and reported it. The agent cannot observe its own permission
  dialogs ([[guard-stall-invisible-to-agent-2026-07-07]]).
- **The cause is INFERRED, not verified.** "The harness classifier changed" is the best explanation given the `ask`
  array is unchanged, but we cannot read the classifier. Alternatives (a CC version change, a different code path
  for `Edit` vs the earlier flow, a matcher we have not found) are not excluded.

### Bonus: a MODEL true-positive stall (SM129)

This is what the **stall budget** should be *spent on*. It cost BR ~2 seconds and it gated **the file that governs
every permission the agent has** — maximum value per unit of attention. Contrast the two `>` false positives killed
the same day (`eb0cd14`), which cost the same attention for nothing. **That is the entire argument for killing
wolf-crying: so the asks that matter still land on a human who is still reading them.**
