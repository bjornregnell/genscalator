# RAW-DATA — curated verbatim excerpts from the live human-agent sessions

**Purpose.** A durable, committed store of *verbatim* chat excerpts (with real timestamps) that matter for the
research, plus the agent's reflections. It exists because the raw context is lost from the **agent's** working
memory at each **compact dance**, taking the *curation* (which moments mattered and why) with it — even though
the raw transcript itself persists in the session `.jsonl`. This file is the curation, made durable in git.

**How it is produced (important for honesty).** Excerpts are **mined verbatim from the session jsonl** by the
typed tool [`RawData.scala`](RawData.scala) — they are NOT retyped by the agent from memory (which would be
lossy and, per [`METHODOLOGY.md`](METHODOLOGY.md) §5, confabulation-prone). The agent adds **reflections** as
clearly-separated `> **Agent reflection:**` annotations. So: *excerpt = objective (mined); reflection =
subjective (labelled).* This is METHODOLOGY §4 (behavioral mining over self-report) applied to data capture.

**Workflow.**
```
scala-cli run research/RawData.scala -- --list --grep "WR data"        # find turn indices
scala-cli run research/RawData.scala -- --dump 6129 6135               # preview verbatim
scala-cli run research/RawData.scala -- --append 6129 6135 --note "…"  # commit it here
```
Turn indices (`#N`) are the jsonl's user+assistant ordering and are STABLE for a given session file; the
`--list` headers note when a turn is a post-compact "session continued" summary (a natural episode boundary).

**Coding** (see METHODOLOGY §4): tag reflections with WR-FRICTION / WR-REGRESS / WR-WIN / WR-META / WR-TOOL /
WR-STATE / WR-DECISION so the file can later be analysed by code.

---

## Excerpt #6159–6159 · 2026-06-26

**[🧑 BR · 17:26:21 · #6159]**

1.  I soon need to eat anyway. AND note in the joint-zone research that humans may reveal things about themselves such as going to eat so agent get an idea oh human state perhaps; or is this just noise that fills the agents context window with not so useful information that derails agent focus; do you get me?


> **Agent reflection:** WR-STATE. Voluntary human self-disclosure of state ('I soon need to eat') = the highest-signal, cheapest human-state input (METHODOLOGY-adjacent; human-state-and-joint-zone.md §5). Correct agent response: compress to the actionable bit (human about to be unavailable + heading to a hunger dip) -> produce durable reviewable state, do not block on human input; do NOT ruminate or store as a memory. BR also turned it into a research question (signal vs context-noise), which is itself the joint reflexive method (METHODOLOGY §5).

---

## Excerpt #5462–5462 · 2026-06-26

**[🧑 BR · 14:43:24 · #5462]**

⟦tool_result⟧
WR data you did a pipe to grep; should use some scala tool


> **Agent reflection:** WR-REGRESS (recovered from BEFORE a compact dance — no longer in agent context; proves the jsonl-mining durability win). Agent piped a scala-cli scratch through grep instead of fixing the scratch to emit clean output. Same reflex family as the later META-2 raw-cat regression: shell post-processing of a typed tool's output. Fix is structural (the tt run / submit-time hook), not exhortation.

---
