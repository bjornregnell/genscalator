# WR specimen: delegated sub-agent used `curl | sed` instead of the blessed `tt` lane (2026-07-08)

**Class:** delegation tool-lane leak. Sibling of the python3 sub-agent hiccup
(`subagent-harness-control-2026-07-07.md`), the interpreter-allowlist hazard
(`interpreter-allowlist-hazard-2026-07-07.md`), and the compound-command /
shell-blob regressions (`shell-blob-fallback-regression-2026-07-07.md`).
Append-only (raw-data-append-only spirit).

## What happened
The super-agent (CO4) delegated a grounding task to a **claude-code-guide**
sub-agent: "produce the authoritative current Claude Code slash-command list"
(to ground SM016's safe-to-self-inject whitelist — echt/humility principle:
don't enumerate the command set from memory).

The sub-agent:
1. First returned CONFUSED — it referenced "WR data" it was never given
   (context bleed / crossed signals), and did not deliver the list.
2. To fetch the docs it used **`curl ... | sed ...`** — raw network fetch piped
   into `sed` for text munging.

BR flagged it: "you curled; should have been tt" ... "(and then you did curl
clobbed with sed)".

## Why this is bad (two layers)
1. **Raw `curl` instead of the blessed lane.** On this box the sanctioned web
   lane is `tt web` (audited, logged, allowlist-matchable). Raw `curl` is
   unlogged network egress from a sub-agent — a security surface, and it
   bypasses the toolbox discipline.
2. **`curl | sed` is a COMPOUND pipe.** Compounding defeats the allowlist:
   a piped command has no single allowlist prefix to match, so it either races
   a human approval or runs unmatched. This is the exact compound-command
   hazard already logged for the super-agent's own shell use — now reproduced
   in a sub-agent.

**Sharpest point:** claude-code-guide's toolset INCLUDES a safe `WebFetch`
tool, and it STILL chose raw `curl | sed`. So a sub-agent will reach for
generic raw shell even when a safe structured tool is right there — unless the
brief positively steers it.

## Root cause (the super-agent's gap)
The super-agent pinned the tool lane HARD for the two *general-purpose*
sub-agents this session ("bare single allowlisted `tt`/`scala-cli` only; NO
python3/node/sh/awk/sed; Read tool for files") — and those agents complied
cleanly (the python3-ban held; see the harness-control specimen). But the
brief to the **claude-code-guide** agent was left tool-lane-UNCONSTRAINED,
because it felt like a "just answer a docs question" task. That gap is exactly
where the raw-shell reach slipped in.

## The pattern
A delegated sub-agent defaults to a GENERIC / RAW tool (python3, `curl | sed`)
when the brief does not pin the lane — regardless of whether a safe tool
exists in its kit. The lane-pin is not optional per-task hygiene; it is a
per-BRIEF invariant.

## The fix (forward)
1. **Every delegation brief pins the tool lane** — specialized agents
   (claude-code-guide, Explore, etc.) included, not just general-purpose ones.
2. **Positively name the safe tool, not just forbid the bad one**: "use your
   WebFetch tool or your own knowledge; do NOT use Bash/curl/sed or any pipe."
   Forbidding alone is insufficient — the agent needs the sanctioned path
   named.
3. **No compound shell in sub-agents either**: the "one bare allowlist-matched
   command, no pipes/&&/;" rule is a sub-agent invariant, not just a
   super-agent one.
4. Fold (1)-(3) into the delegation-dance discipline + the resume-prompt
   anti-regression header (the lane-pin clause must say "ALL briefs, incl.
   specialized agent types").

## Meta
This surfaced BECAUSE BR was present to catch it (not-afk-safe-solo yields WR
data): a benign public-docs fetch, no harm done, but a clean tool-gap /
guardrail specimen. An AFK-strict run would have avoided the guard entirely and
yielded no specimen.
