# Proposal (DRAFT, needs BR approval): a guardcheck PreToolUse hook

- **Goal:** turn `tt guardcheck` from a *knowledge-tier* safeguard (a tool the agent must remember to run) into a
  *structural* one (auto-invoked at the decision point) — the **prosthetic habit** made real, and the fix for the
  **agent-perception gap** (the agent can't see the env-guard messages; a hook can inject them back).
- **Status:** DRAFT for BR review. NOT activated — hooks/settings changes are human-approved (settings-mirror rule).

## The idea
A **PreToolUse hook** on the `Bash` matcher runs a guard-check on the *proposed* command *before* it executes,
and **injects the findings back into the agent's context** (`hookSpecificOutput.additionalContext`). So the agent
finally *sees* the reason a command would trip the guard (which today only the human sees), and can self-correct
to the safe form — or the hook blocks the trip outright.

## Two escalation levels (recommend starting at L1)
- **L1 — advisory (recommended first).** On a finding, inject the guardcheck report as `additionalContext` (agent
  sees "your command has an `&&` chain → split it") and let the command proceed (or fall to the normal prompt).
  Zero lock-out risk; pure perception-gain. Measures: does seeing the finding reduce the agent's relapse rate?
- **L2 — blocking.** On a HIGH finding, return `permissionDecision: "deny"` (or `"ask"`) with the reason, so the
  reflex is *structurally prevented*. Higher power, but risks false-positive lock-outs — adopt only after L1 shows
  guardcheck's precision is high enough.

## Sketch (L1, PreToolUse / Bash)
```json
{
  "hooks": {
    "PreToolUse": [{
      "matcher": "Bash",
      "hooks": [{ "type": "command",
        "command": "<extract .tool_input.command, run guardcheck cmd on it, emit additionalContext if findings>" }]
    }]
  }
}
```
The command extracts `.tool_input.command` (jq), runs the guard-check, and if there are findings emits
`{"hookSpecificOutput":{"hookEventName":"PreToolUse","additionalContext":"guardcheck: <report>"}}`.

## The real design problem: LATENCY
Running the JVM `tt guardcheck` (scala-cli) on **every** Bash command adds ~1–2s JVM startup *per command* — an
unacceptable tax. Three ways out (in preference order):
1. **Resident guardcheck** — the `--client`/Unix-socket server from the tt-monolith plan
   (`notes/plan-tt-monolith-client.md`): the hook talks to an already-warm guardcheck, sub-ms. **Best; ties the
   hook to the monolith work.**
2. **Fast pre-filter in the hook** — a cheap regex gate (pure `jq`/shell, no JVM) that recognises the obvious
   patterns (`&&`, `;`, `$(`, backtick, line-leading `#`) and only *those* inject a warning; escalate to the full
   guardcheck lazily. Downside: duplicates guardcheck's logic in shell (guardcheck stays the tested reference).
3. **Accept the latency** — simplest, but the per-command tax is likely too high for interactive use.

## Recommendation
Build **L1 + a fast pre-filter (option 2)** as the first shippable version — it closes the perception gap for the
high-frequency reflexes (`&&`/`;`/`$(`/line-leading-`#`) with no JVM tax and no lock-out risk — and treat the
**resident guardcheck (option 1)** as the clean end-state once the tt monolith `--client` server exists. `tt
guardcheck` (already shipped + tested) is the **spec** the pre-filter must match, so the two can't diverge
silently. BR approves before any hook is written into `.claude/settings*.json`.
```
