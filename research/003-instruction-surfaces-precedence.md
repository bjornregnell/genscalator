# Instruction surfaces — how they compose, conflict, and rank

- **Question:** An agent's behavior is shaped by **many** instruction/context surfaces at once —
  global "Instructions for Claude", `AGENTS.md`, `CLAUDE.md`, subdir `CLAUDE.md`, `SKILL.md` bodies,
  `MEMORY.md` + recalled memory files, harness/system prompt, `settings.json`, output styles. **How do they
  compose?** When two say *contradictory* things, **which wins**? Which are **always-on** (paid every task)
  vs **lazy** (loaded on demand)? Where is the **redundancy** that wastes context and **drifts** when one
  copy is updated and others aren't?
- **Why it matters:** this is the **governance layer** of the whole workflow. Get the precedence wrong and
  the agent silently follows a stale or lower-priority rule; get the *always-on set* bloated and every task
  pays a context-rot tax (cf. *smart zone*, `007-token-budget-awareness.md`). A clear, documented model is what
  lets a human put each rule in **exactly one** right place and trust it fires.
- **Status:** open (priors below; needs empirical confirmation per surface + per agent vendor).

## What I already know (priors — to verify, Claude Code-centric)
Rough **authority order** (highest wins on direct conflict), and **load timing**:

1. **Harness / system prompt** — highest. Hard constraints + tool contract. Always-on, not user-editable.
2. **Enterprise-managed policy** (managed settings) — org-level, above the user. Always-on where present.
3. **Project `AGENTS.md` / `CLAUDE.md`** (repo root) — the project contract. Loaded at session start for the
   working dir; the harness explicitly treats it as **"OVERRIDE any default behavior, follow exactly."**
   Subdir `CLAUDE.md` refines and loads **lazily** when files under that dir are touched (more-specific =
   more local; refines rather than fights the root).
4. **User global "Instructions for Claude"** (Settings → General) — cross-project defaults. Always-on,
   **every** session. Lower than a project file that contradicts it, but broader in reach. (See
   `004-instructions-for-claude.md`.)
5. **Recalled memory** (`MEMORY.md` index → memory files) — **background context, NOT user instructions.**
   The index is always-on (one line each); individual memories surface on relevance. Advisory/lower
   authority — and **may be stale** (reflects when written; verify file/flag names still exist before acting).
6. **`SKILL.md` bodies** — **lazy / progressive disclosure**: only name+description sit in context until the
   skill is invoked; on invocation the body becomes active task instructions. Cheapest by default; the model
   to imitate.
7. **`settings.json` / output styles** — config + presentation, not behavioral instructions per se; shape
   tool perms and format, orthogonal to the above.

**Always-on tax** (paid every task): system + enterprise + root AGENTS/CLAUDE + global instructions +
MEMORY.md index. **Lazy** (paid on use): subdir CLAUDE.md, SKILL.md bodies, individual memory files,
research/. Keeping behavior in the **lazy** tier is the TE win.

### Known failure modes
- **Duplication drift** — the same rule in global instructions AND CLAUDE.md AND memory: triple context cost,
  and when one copy changes the others silently disagree. Each rule wants **one home**.
- **`AGENTS.md` vs `CLAUDE.md`** — two names for "the project contract." `AGENTS.md` is the cross-vendor
  standard; `CLAUDE.md` is what Claude Code reads natively. Having both invites divergence; pick one as source
  of truth (symlink / include the other) — an open genscalator decision.
- **Silent precedence** — the agent rarely *announces* which surface a behavior came from, so a human can't
  see why a global rule lost to a project file (or to a stale memory). No introspection of "which instruction
  fired."
- **Memory mistaken for instruction** — recalled memory is *background*, but a confidently-worded memory can
  get followed like a command (esp. when stale). The authority gap (5 vs 3/4) is easy to forget.
- **Lazy-load assumptions** — assuming a subdir `CLAUDE.md` or skill is loaded when it is **not** (until its
  dir is touched / skill invoked) → the agent acts without a rule the human believed was active.

## Open directions
- **Confirm the order empirically** per surface (esp. global-instructions vs root-CLAUDE.md on a deliberate
  contradiction) and per vendor (Claude vs Codex vs open frameworks — the portability goal).
- A **placement decision rule**: "global vs AGENTS.md vs subdir vs skill vs memory?" one-screen checklist
  (pairs with `004-instructions-for-claude.md`'s global-vs-local split).
- **Provenance/introspection:** can the agent report *which* surface a given behavior traces to (debugging
  precedence + drift)? Cousin of the context-fill blind spot (`006-smart-zone-ceiling.md`).
- A **redundancy linter** — detect the same directive living in >1 surface.
- Decide the genscalator **`AGENTS.md`/`CLAUDE.md` canonicalization** (one source of truth).

## What shipped
- Nothing yet — note opened 2026-06-30. Companion to `004-instructions-for-claude.md` (that one is the *global
  field* in isolation; this one is *all surfaces together* + their ranking).
