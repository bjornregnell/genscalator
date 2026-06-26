# research/ — open investigation log

genscalator is a **research project**: the tools, skills, and docs are distilled from real investigations
done in the open. This folder is where those investigations live — plans *and* results — so the community
can see what's being explored, what we learned, and how it fed back into genscalator.

## The one rule: research must not interfere with daily use
Research notes are for **humans and for agents that are *explicitly* doing research** — never loaded into
an agent's working context during ordinary tool use.

- **No file here is referenced from `AGENTS.md` (core contract) or from a skill body that loads during a
  task.** Pulling research prose into the working context would bloat it and risk the *dumb zone* (see
  [`../docs/foundations.md`](../docs/foundations.md) glossary: *smart zone / dumb zone*, *token efficiency*).
- An agent consults `research/` only when the human asks it to investigate, not while doing the user's task.
- **Exception — contribution mode:** when an agent is about to *propose a contribution back* to genscalator
  (or has spotted a possible improvement), it should first check the README **roadmap** and `research/` to
  align with on-going work and avoid duplicating or contradicting it. See [`../CONTRIBUTING.md`](../CONTRIBUTING.md).

## How an investigation works
1. One file per investigation: `research/<topic>.md`, using the template below.
2. Findings that generalize **graduate** into edits to `tools/` / `docs/` / `skills/` + a release; the
   investigation file records *what shipped* so the trail stays auditable.
3. Keep entries lightweight — this is a lab notebook, not a paper.

### Template
```
# <Investigation title>
- **Question:** what are we trying to learn?
- **Why it matters:** which genscalator goal/tradeoff this serves.
- **Plan:** how we'll investigate.
- **Status:** open / in progress / shipped / parked.
- **Findings:** what we learned (grows over time).
- **What shipped:** concrete changes to genscalator, with version.
```

## Investigations
- [`scala-style-evolution.md`](scala-style-evolution.md) — how should the `scala-style` skill
  self-consciously evolve, guided by agents using it, without drifting or bloating?

## Data
- [`wr-data/`](wr-data/) — **WR** (Workflow Research) log of confirmation/approval events from real
  sessions that are candidates for elimination by a new safe-by-design `tt` tool. The raw evidence behind
  the confirmation-fatigue thesis; feeds *which tool to build next*.
