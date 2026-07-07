# WR data — who controls a sub-agent's harness settings? (the "why python not scala" finding), 2026-07-07

**Trigger.** After the PB-reflow sub-agent reached for **python3** instead of the blessed **scala-cli**
lane (overreach specimen in `delegation-dance-and-dilemma-2026-07-07.md`), BR asked the sharp question:
*"WHY did the sub-agent not use type-safe Scala? This is IMPORTANT to steer delegation hand-offs better —
the super-agent needs to craft allow/deny-lists for sub-agents. Are you (or me?) in control of the harness
settings for SUB-agents?"* This note answers it, because the answer reshapes how briefs must be written.

## The answer: the super-agent controls the BRIEF, not the sub-agent's permissions
- **The `Agent` tool exposes:** `subagent_type`, `prompt`, `model`, `isolation`, `run_in_background`. It does
  **NOT** expose a per-sub-agent allow/deny list. So the super-agent (CO4) **cannot craft a separate
  permission allowlist per sub-agent** — there is no such lever in the tool.
- **Sub-agents inherit the SAME `settings.local.json` allow/deny** as the main session. That allowlist is
  **BR's** (human-approved, the authority anchor) — not something CO4 sets or varies per delegate.
- **Therefore the ONLY channel CO4 has to shape a sub-agent's behaviour is the BRIEF (the prompt).** Not
  permissions. This is exactly why the governance fix landed on the brief: *pin the tool lane in the brief*,
  because there is no permission-scoping alternative.

## Why the sub-agent used python3, precisely
- **Sub-agents do NOT inherit CO4's substrate.** They get **only the brief** — not CO4's memories, not the
  CLAUDE.md-style operating rules, not the "scala-cli is the blessed lane" discipline. That discipline lives
  in CO4's **persistent substrate** (memory `never-allowlist-interpreters`, `prefer-scala-scratch-over-bash`),
  which a fresh sub-agent has never read.
- So the sub-agent fell back to the **generic-agent default reflex** for a text-reflow task: **python3**. It
  wasn't a considered rejection of Scala; it was the out-of-the-box habit genscalator exists to replace
  (`foundations.md` Background: "awk, grep, sed, python3"), firing in an agent that lacked the substrate that
  suppresses it. A clean **briefing-fidelity failure** (RT049 RQ2): CO4 briefed the sub-agent worse than its
  own global understanding — it forgot the sub-agent doesn't know what CO4 knows.

## The silver lining + the real finding (BR-flagged agent introspection)
- **Silver lining (structural safeguard held):** the shared allowlist has `Bash(python3 -)` but NOT
  `python3 <script>`, so the sub-agent's python3 attempt **tripped the guard** and BR was prompted (and
  correctly declined the dangerous "don't ask again `python3 *`" blanket). The **structural** layer caught
  what the **knowledge** layer (the brief) missed.
- **The real finding:** this is the **two-tier safeguard model** (`foundations.md` *Structural vs knowledge
  safeguard*) playing out **across the delegation boundary**. The brief is a **knowledge** safeguard
  (retrieval-fragile — and a fresh sub-agent has *nothing* to retrieve, so it is even weaker than for CO4).
  The shared allowlist is the **structural** safeguard (retrieval-independent — it held). **Lesson: for
  sub-agents, weight even harder toward structural, because the knowledge layer is not just fragile, it is
  nearly empty.** A sub-agent starts with none of CO4's hard-won discipline; only the allowlist and the brief
  stand between it and the out-of-the-box bash reflex.

## Implications for delegation hand-offs
1. **The brief must transfer the lane explicitly** (already pinned): "use scala-cli / tt only, NEVER
   python3 / raw shell" — because the sub-agent has no memory that says so.
2. **The shared allowlist is the real backstop** for sub-agent misbehaviour, and it is **BR's to curate**
   (human-approved). Tightening it (e.g. the `python3 *` denial that just worked, the `ssh *` narrowing on
   the PB) protects against *any* delegate, briefed well or badly. Structural > knowledge, doubly so here.
3. **Gap worth naming:** there is **no per-sub-agent permission scoping** in the harness today. If we wanted
   a sub-agent to run under a *tighter* allowlist than the main session (least-privilege delegation), the
   harness does not offer it — the delegate runs at the *same* privilege as CO4. A possible RT / feature ask:
   least-privilege sub-agents. Until then, least-privilege is achieved only by the brief (weak) + the global
   allowlist being safe for the *most* privileged thing any delegate might do.

Cross-refs: `delegation-dance-and-dilemma-2026-07-07.md` (the overreach specimen this analyses),
`interpreter-allowlist-hazard-2026-07-07.md`, memory `delegation-dance` (briefs pin the tool lane),
`never-allowlist-interpreters`, RT049 (briefing fidelity = the bottleneck).

## Correction / refinement (authoritative, via `claude-code-guide`, 2026-07-07)

An authoritative harness check refines the pessimistic claim above ("the
brief is the ONLY lever"). Precise picture:

- **Per-SPAWN scoping does NOT exist** — confirmed. The only per-spawn
  levers are **`model`** and **`isolation: worktree`**. You cannot pass a
  narrower allowlist, tool set, or skill list when spawning a sub-agent.
- **BUT there IS a structural lever the earlier note missed: the custom
  sub-agent DEFINITION** (`.claude/agents/*.md` frontmatter). It can:
  - **restrict tools** via `tools:` (allowlist) + `disallowedTools:`;
  - **control skills** — sub-agents inherit *access* to all skills via
    the `Skill` tool (not auto-preloaded); remove `Skill` from `tools:`
    to deny skills entirely, or `skills:` to *preload* chosen ones;
  - **set `permissionMode:`** (though a parent in `bypassPermissions` /
    `acceptEdits` overrides it — so the parent's mode caps the child's);
  - restrict `mcpServers:`.
  - Permissions allow/deny are still **inherited** from `settings.json`
    (no per-agent narrowing there), but tools + skills + permissionMode
    are constrainable at the **agent-type** granularity.

- **The real upgrade for delegation safety:** the tool-lane discipline
  need not live only in a fragile per-spawn **brief** (knowledge, and for
  a fresh sub-agent a near-empty one). We can **author a restricted
  worker agent TYPE** (e.g. `gs-worker`) whose **frontmatter fixes the
  privilege** (no `Skill`, tight `tools:`, safe `permissionMode`) and
  whose **system-prompt body permanently bakes** "scala-cli / tt only,
  never python3 / raw shell, do only the stated task, never write the
  memory index." That converts the fix from **knowledge → structural**
  (`foundations.md` *Structural vs knowledge safeguard*): the discipline
  fires without the super-agent remembering to re-brief it each time.
  **Caveat:** it is agent-type granularity, not per-task; and authoring/
  editing `.claude/agents/` is a **config change → BR-approved** (the
  authority anchor; hardening dance). Filed as a PB backlog item.
- **Net:** "least-privilege delegation IS achievable" — just at the
  agent-TYPE level (author a restricted definition), not per-spawn. The
  earlier "no lever" was half-right (no per-spawn lever) and half-wrong
  (there is an agent-type lever). Corrected here for the record.
