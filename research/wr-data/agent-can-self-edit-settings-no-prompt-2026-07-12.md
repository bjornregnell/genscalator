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
