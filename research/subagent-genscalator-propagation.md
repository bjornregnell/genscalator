# How to make sub-agents use genscalator (tt + skills + methodology)

- **Question:** when a main agent **delegates** work to a spawned sub-agent (Claude Code Task/Agent tool), does
  that sub-agent inherit genscalator's capabilities — the `tt` tools, the skills (`scala-style`, `reqt-lang`),
  the methodology, the permission allowlist — so delegated work is still safe-by-design and convention-following?
  If not, how do we make it so?
- **Why it matters:** genscalator's whole value is encoded partly in **CLI tools** (`tt`) and partly in **skills +
  methodology** (how to work: no raw grep, args-not-env, the guard-avoidance ruleset, scala-style). If delegation
  silently drops half of that, then multi-agent workflows (which we increasingly use — e.g. the WR-harvest
  sub-agents) run *outside* the methodology, and the "agents co-maintain the habits they run under" thesis
  ([`scala-style-evolution.md`](scala-style-evolution.md)) breaks at the delegation boundary.
- **Status:** open (2026-07-02, prompted by BR after observing sub-agent harvests).

## Findings so far (documented + observed 2026-07-02)
Confirmed via the Claude Code guide + direct observation this session:

| Capability | Inherited by a spawned sub-agent? |
|---|---|
| **Plugin CLI tools** (`tt`, `scala-cli`) | **Yes, effectively** — on PATH / absolute path, runnable via the Bash tool (if Bash is in the sub-agent's tool set). Observed: harvest sub-agents ran `scala-cli …/RawData.scala` with **zero prompts**. |
| **Skills** (`scala-style`, `reqt-lang`) | **No, not by default.** Skill *content* loads only if listed in the agent definition's `skills` field. The Skill tool works if in `tools`, but the guidance isn't auto-injected. |
| **Permission allowlist** (`permissions.allow`) | **Docs: not inherited by default** (sub-agents get tool lists, not permission rules). **Observed: allowlisted Bash ran without prompts** — so the interactive Task-tool behavior appears to differ from the documented SDK `AgentDefinition` model, OR `permissionMode` covered it. **Open discrepancy to pin down.** |
| **Plugin CLAUDE.md / slash commands / MCP tools** | Not documented as automatic. **Project** CLAUDE.md *is* loaded (via `settingSources`). |
| **Memories** | No. |

**Net:** a naive sub-agent gets the *tools* (so it CAN run `tt`) but not the *methodology* (so it won't KNOW to,
and won't apply scala-style/reqt-lang/guard-avoidance). Delegated work drifts outside the conventions unless the
main agent **hands it exact commands** (what we currently do) or the sub-agent is **pre-configured**.

## The methodology-propagation gap (the real research point)
This is a **WR finding**: genscalator's methodology, encoded in skills, does **not** cross the delegation boundary
by default. So today's mitigation — the main agent inlines exact commands — works for *mechanical* delegation but
NOT for *judgment* delegation (where you want the sub-agent to reason under scala-style / the guard rules). As
multi-agent workflows scale, the fraction of work done "outside the methodology" grows unless we close this.

## Options to make sub-agents genscalator-aware (to evaluate)
1. **Ship a genscalator sub-agent definition** (`.claude/agents/genscalator-worker.md` or SDK `AgentDefinition`)
   that pre-attaches `skills: ["scala-style","reqt-lang"]`, the `tt`-relevant tools, and the guard-avoidance
   ruleset. Then `subagent_type: genscalator-worker` gets the methodology for free. **Most promising.**
2. **Inline the guidance** in the delegation prompt (cheap, but re-typed each time and easy to forget — the same
   substrate-#1-doesn't-persist problem).
3. **A `tt`-only discipline:** make the tools so safe-by-design that a sub-agent using them *can't* violate the
   methodology even without the skills (safe-mode/capture-checking angle — cross-ref
   [`harness-guard-probe-and-custom-guard.md`](harness-guard-probe-and-custom-guard.md)). Structural, strongest,
   most work.
4. **A `tt agent-brief` command** that emits the current methodology as a prompt-injectable brief, so any
   delegation can prepend it deterministically.

## Next steps
- Pin down the permission-allowlist discrepancy (docs vs observed) — it changes how much config option 1 needs.
- Prototype option 1 (a genscalator worker sub-agent definition) and measure: does a sub-agent given only a task
  (no inlined commands) then correctly reach for `tt text grepr` / avoid raw grep / apply scala-style?
- Relates to [`instruction-surfaces-precedence.md`](instruction-surfaces-precedence.md) (which surface carries the
  methodology to a sub-agent?) and the CF thesis (delegated work must not widen the attack surface either).
