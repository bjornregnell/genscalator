# HUMANS.md — the humans' review queue

The counterpart to [`AGENTS.md`](AGENTS.md): that file is *for the agent(s)*, this one is *for the human(s)* — a
to-do list of **human** tasks, above all **reviewing agent-authored changes** before they land. Together the
`AGENTS.md` ↔ `HUMANS.md` ↔ `HUMANS.inbox.md` trio is a small, reusable **protocol for human–agent collaboration** — and
genscalator uses its own copy as a live example. New contributors: this is the pattern; copy it into your own repo.

> **New here?** Start with [`CONTRIBUTING.md`](CONTRIBUTING.md) (how to propose tools + the agent-proposes/human-approves
> ethos), then skim [`docs/foundations.md`](docs/foundations.md) (goals, glossary) and [`AGENTS.md`](AGENTS.md).

## The protocol (how this file works)

- **Agent → human channel is the inbox, never this file.** The agent **never writes `HUMANS.md` directly** (so it can't
  clobber your live edits). It appends proposals to [`HUMANS.inbox.md`](HUMANS.inbox.md) (append-only, agent-owned). You
  **harvest**: move an item into `## TODO` below, then delete it from the inbox. **You alone own `## TODO` and
  `## DONE`.**
- **Tick as you go.** When a whole subsection is done, move it to `## DONE` (or delete it if it no longer matters for
  memory/changelog). Empty `## TODO` = you're caught up.
- **Two tags you can add when harvesting or ticking:**
  - **`HD:`** *(Human Decision)* — prefix a decision only you can make, so the record shows *what* was decided and *why*.
  - **`TAP:`** *(To Agent — Plan)* — a job for the agent; when you next ask the agent to read `HUMANS.md`, it folds all
    `TAP:` items into its work plan and checks with you on prioritization.
- **The historical record lives in [`CHANGELOG.md`](CHANGELOG.md)** (`## Unreleased`). This file is only the
  *what-humans-still-owe* checklist; the agent keeps `CHANGELOG.md` consistent when a change affects it.
- **Docs/research** the agent may commit + push freely once agreed. **Tool/version changes follow the human-reviewed
  release flow** ([`docs/updating.md`](docs/updating.md)) — never auto-pushed; they wait for human approval.

## A related variant (for the curious)

This inbox-harvest form is the **safe, multi-human** variant: the agent proposes, humans own the list, nothing lands
unreviewed. A **higher-trust single-human** variant also exists, where the agent *maintains* the human's pinboard live
(the human reads, the agent pins) and the session feed *is* the inbox — no separate inbox file. Pick the variant that
matches your trust level and team size. genscalator's public repo uses the reviewable inbox-harvest form here.

## TODO

*(Genscalator's live human-review queue. Doubling as a worked example of the protocol above.)*

### Improve the HUMANS.md ↔ AGENTS.md protocol itself
- [ ] **Fold this collaboration protocol into the `/genscalator` plugin** so any repo can adopt the
      `AGENTS.md`/`HUMANS.md`/`HUMANS.inbox.md` trio out of the box. *TAP:* chat about the packaging.
      *(Side note for agents: humans usually type a plain `-`, not `—`/`--` — match that in prose.)*

## DONE

*(Kept as a short worked example of the protocol closing items. Full history is in [`CHANGELOG.md`](CHANGELOG.md).)*

- [x] **`AGENTS.md` vs `CLAUDE.md` canonicalization** — *HD:* `AGENTS.md` is the source of truth (provider-agnostic:
      also works for Codex, opencode, etc.).
- [x] **Emoji rule** — *HD:* dropped the flat ban; scoped guidance instead (no decorative clutter; functional status
      glyphs OK).
- [x] **Language-channel rule placement** — *HD:* the universal version belongs in the global "Instructions for Claude";
      per-project nuances stay in agent memory.

---
*Agent: never write this file directly — append review-worthy proposals to [`HUMANS.inbox.md`](HUMANS.inbox.md)
(append-only). The human harvests into `## TODO`, ticks, and moves completed subsections to `## DONE`. Keep
`CHANGELOG.md` consistent when relevant; stage only agent-authored paths, never `git add -A`.*
