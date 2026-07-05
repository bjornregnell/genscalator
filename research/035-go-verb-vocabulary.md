# A small, hardened `go X` verb vocabulary (proposal)

- **Question:** should we **harden a small set of `go X` verbs** — scoped autonomy-handoff commands (`go stub`,
  `go sweep`, …) — that name the most frequent transitions, so a one-word cue carries a whole scoped action?
- **Why it matters:** `go` already handoffs autonomy on the *current* plan; a `go X` names *which* scoped action to
  take without spelling it out — a **communication-bandwidth** win. But there's a hard tradeoff (below), so the set
  must stay **small and earned**.
- **Status:** **PROPOSAL** (agent-drafted 2026-07-05), awaiting BR ratify. Pinned for review.

## The tradeoff (BR 2026-07-05 — and it is *general*)
> "There is only so much I can learn quickly, and a bit more by deliberate rehearsal."

Shorthand has a **learnability budget**. Every coined term (a cue, a dance name, a `go X` verb, a comms acronym) spends
some of it. Past the budget the vocabulary becomes **private jargon the human can't recall in the moment** — the
*index-rot failure applied to vocabulary*. So: a **tiny learn-once core**, a **small rehearsal tier**, and a standing
rule to **prune before adding**. This principle is general — it governs all our coined vocabulary, not just `go X`.

## Design rules for admitting a `go X`
1. **Maps to a frequent, distinct transition** in the task-state model (`034-defining-an-agent-task-state-model.md`) — a
   verb that fires rarely doesn't earn a slot.
2. **No overlap with an existing cue.** `pin:` / `note:` / `WR data:` already exist → **no `go pin`** (redundant).
3. **Typable + unambiguous** (short, low motor cost, no collision with another term).
4. **Prune-to-add:** the set has a soft cap; a new verb must displace a weaker one or justify growing the budget.

## Proposed set

### Core tier (learn-once — the ~4 that pay for themselves)
- **`go`** *(base)* — proceed autonomously on the **current scoped plan** (the go dance).
- **`go stub <topic>`** — create a scoped **stub note/file** for a topic (Candidate → Scoped); don't build it out.
- **`go draft <thing>`** — produce a **draft / proposal**; do **NOT** finalize, apply, or publish. (For anything
  outward-facing or hard-to-reverse — you review first.)
- **`go fix`** — **apply** the change we just discussed (the reverse of `go draft`: act, don't just propose).

### Rehearsal tier (learn by use — a few more, each a named dance)
- **`go sweep`** — run a **consistency dance** (index-rot / dead-link / changelog / term-drift sweep, auto-fix the
  unambiguous, report the rest).
- **`go menu`** — refresh / present the **AFK menu** (the always-stocked-menu invariant on demand).
- **`go harden`** — run the **hardening dance** (audit the agent's own config for misfire risks; propose structural
  fixes; you approve security ones).

That's **7 total**. Recommendation: **freeze at ~7**; if an 8th is tempting, prune first.

## Deliberately *not* included (and why)
- `go pin` — `pin:` already does it (rule 2).
- `go compact` / `go rest` — these are **agent-proposed** dances (the agent detects the trigger), not human-issued
  verbs; a `go` prefix would misframe who initiates.
- `go publish` / `go release` — **outward-facing**, must stay an explicit deliberate act, never a one-word cue.

## Next
BR ratifies / prunes the set → then it graduates into a `foundations.md` note ("go-verb vocabulary") + a one-line
memory, and the verbs get aligned 1:1 with the task-state-model transitions. Until ratified, only `go` (bare),
`go stub`, `go draft`, `go fix` are in active use (the ones already used naturally).
