# WR data: the agent's guardrail skills were SILENTLY INACTIVE all session — root cause of the felt regression (2026-07-13)

BR's flag: *"BR thinks agent is regressing even on the genscalator big goals.. (???)"* — a meta-intuition, floated
with uncertainty. It turned out to be exactly right, with a concrete mechanism.

## The symptom
Repeated **avoid-guard-stall regressions**: `;`-command-chains, `2>/dev/null` stderr-suppression, `| tail` pipes —
the exact idioms the `avoid-guard-stall` skill exists to prevent, tripped MULTIPLE times this session despite the
agent "having" that skill. Plus a general style/scope drift the human sensed but couldn't yet name.

## The root cause (confirmed via `/skills`)
`/skills` reported **"No skills found"** — ZERO project/user skills active this whole session. Not just genscalator's;
nothing.
- **genscalator's 10 skills** (`scala-style`, `tt-toolbox`, `avoid-guard-stall`, `scala-code-review`,
  `contribute-tool`, `gs-dwim`, `reqt-lang`, `research-methods`, `crud-web-app-seed`, `in-session-experiment`) live
  in **`genscalator/skills/`** = the **plugin layout**, not `<cwd>/.claude/skills/`. Plugin-layout skills load only
  when the plugin is **installed/enabled** (`enabledPlugins`), which genscalator was NOT in this session.
- **`br-blog-ass`** IS in `<cwd>/.claude/skills/` (cwd = muntabot-synch), but that dir was **created this session**;
  the skill watcher only picks up skill dirs **present at session start**, so it needs a **restart** to activate.

Net: **every genscalator edit this session happened with genscalator's own coding + guard rails switched off.** The
guard-stalls were the observable tip; the drift was the rest.

## The human-in-the-loop win (the interesting part)
**The agent cannot *feel* a missing skill.** There is no phenomenology of absence, no error, no notice — an inactive
skill is indistinguishable from the inside from an active one the agent simply failed to apply. The only signal is
**behavioral regression**, which the agent also can't reliably self-certify (family E). The HUMAN closed the loop: he
read the behavioral signature (repeated guard-stalls + scope/style drift), formed a meta-hypothesis ("are relevant
skills even active?"), and had the agent run `/skills` — surfacing a root cause the agent had never thought to check.
This is a clean instance of the external read being load-bearing: the human's out-of-band intuition caught a silent
infrastructure outage the agent was structurally blind to.

## Generalization + fix
- **An agent's guardrail skills can be silently inactive.** No error fires. Needs a **startup self-check** — the
  resume-prompt / SessionStart should VERIFY the expected skills are active (e.g. assert `/skills` lists the ones the
  work depends on), not assume. Ties [[SM069]] (skill-set audit) and the [[hardening-dance]] (agent audits its own
  config for misfire causes).
- **Reflexes-in-memory are not a substitute for an active skill.** The avoid-guard-stall reflex lived in memory +
  the resume-prompt, yet still regressed — because a memory line is passive recall, while an active skill is
  injected guidance. This is why the skill being OFF mattered so much.
- **Fix (foundation-first, human's hand):** (1) restart → activates `br-blog-ass`; (2) enable genscalator as a
  plugin → activates the 10; (3) only THEN resume genscalator code, with rails on. Building more before that = more
  blind drift.
- **Contributing driver:** the session had also drifted into self-referential observability plumbing (mode-line →
  clock-freeze → compact-timing hook → json-escaping-for-that-hook), crowding out the actual big goal (v0.9.0/v0.10.0
  release + reqT-PRD). Instruments-to-watch-the-agent displaced product work — a second, orthogonal regression the
  same intuition flagged.
