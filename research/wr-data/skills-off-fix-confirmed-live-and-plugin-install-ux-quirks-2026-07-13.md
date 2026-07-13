# WR data: the skills-off fix confirmed LIVE mid-session + plugin-install UX quirks (2026-07-13)

After diagnosing all-session that the genscalator skills were INACTIVE (the root cause of the session's
regressions), BR installed the plugin mid-session (deliberately BEFORE the warp) to fix it — dogfooding the
README's own just-written install/activation instructions. Both the confirmation and the UX quirks are WR data.

## The live confirmation (the fix works)
Sequence BR ran: `/plugin marketplace add https://codeberg.org/bjornregnell/genscalator.git` →
`/plugin install genscalator@bjornregnell` (**user** scope) → `/reload-plugins`. Result: the **9 genscalator
skills went ACTIVE in the CURRENT (high-context) window**, visible in the agent's available-skills listing,
namespaced `genscalator:<skill>` (avoid-guard-stall, tt-toolbox, scala-style, scala-code-review, gs-dwim,
contribute-tool, reqt-lang, in-session-experiment, crud-web-app-seed). Plus `br-blog-ass` (from the muntabot
`.claude/skills/`).

Confirms:
1. **The root-cause diagnosis was correct** — skills off → all the regressions
   ([[guardrail-skills-silently-inactive-all-session]]); turning them on is the fix.
2. **The fix works MID-SESSION**, not only at a fresh start — `/reload-plugins` activates the plugin's skills in a
   running (even high-context, "rotten") window.
3. **Install persists across a warp** — the plugin is now installed/enabled, so the post-warp fresh process
   auto-loads the skills at startup, killing the "skills-off persists across the warp" risk (the reason we
   installed BEFORE the warp). Ties [[verify-skills-active-at-session-start]].
4. **The README's install/activation path is validated** (marketplace add → install → reload-plugins) — a real
   dogfood of the SM059 alpha-tester onboarding, and it worked.

## The UX quirks (the success was under-reported)
Activation SUCCEEDED, but the Claude Code feedback signals were MISLEADING — a new user could conclude it failed:
- **`/reload-plugins` reported "0 skills"** ("Reloaded: 1 plugin · 0 skills · 6 agents · 0 hooks · ...") while **9
  skills actually loaded.** The count is wrong/misleading (likely counts only top-level `.claude/skills/`, not
  plugin-bundled skills).
- **`/skills` reported "No changes"** (twice) even though the skill set went from absent to present. Another
  "nothing happened" signal when something did.
- Only the agent's internal available-skills listing (which the USER does not directly see) confirmed the 9 were
  live. So the user-facing feedback UNDER-reported the success.
- **`/plugin install` prompted a scope choice** (project vs user) — a decision point a newcomer may not know how to
  answer (we advised **user** scope for cross-repo personal use).
- **Unexpected: "6 agents"** — the genscalator plugin ships 6 agents too, not just skills; not in our mental model
  nor the README's "what you get".
- GOOD UX (contrast): `/plugin install` printed "✓ Installed genscalator. Run /reload-plugins to apply." — clearly
  stating install ≠ active plus the next step.

## Implication
For the SM059 alpha-test onboarding, the misleading "0 skills" / "No changes" feedback is a real confusion risk. The
README/welcome should tell testers to verify with **`/skills`** showing the `genscalator:` set (and warn the reload
counter may under-report). Ties the disinfo / misleading-instrument thread and SM074 (mode/feature discoverability).
