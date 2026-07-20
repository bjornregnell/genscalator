# 020 — Super-context awareness and modus operandi

**Status: STUB (agent-scaffolded from BR's captured notes; BR to revoice).** About the joint human+agent
**"super-context"** — the shared understanding that outlives either party's individual context window — and how we
**introspect it and choose our "modes"** with easy-typed cues.

## BR-voiced raw (dump — revoice into the post)

> When to go TSM? When to go HHM? I and Claude have these easy-typed cues so that we can discuss what mode we are in.
> For example "Tokens Spending Mode" or "Hot Harvest Mode". What are these and how do we know when to go into a
> certain mode? And can we build typed tools to support our super-context awareness? We got one small idea and one big
> idea.

## The idea: a joint "super-context" and named modes

Neither the human nor the agent holds the whole picture alone. The human forgets; the agent's window warps (a compact,
a cold start, a model swap). What persists is the **super-context** — the externalised, shared substrate (pins,
memory, commits, this blog) plus a small shared vocabulary for **what mode we are jointly in right now**. Naming the
mode is cheap (a two- or three-letter cue) and it aligns both parties in one token.

Two modes to define (draft — BR to refine):
- **TSM — Tokens Spending Mode.** We deliberately *spend* tokens on high-value work (deep investigation, fleets,
  double-races) because weekly headroom is comfortable and the value is real. The complement of saving-mode. Ties to
  [`token-budget-modes`] and the money axis of Token Efficiency.
- **HHM — Hot Harvest Mode.** *(BR to pin the exact meaning.)* Working sense: a burst where the *hot context* is rich
  and we **harvest** it fast — pin the findings, spawn the sub-agents, bank the substrate — before a warp cools it.
  The mode you enter when the session is generative and the risk is *losing* what's live rather than spending too much.

**When to switch, and how we know:** the trigger signals (weekly headroom + reset proximity for TSM; context-fill and
"is this context about to warp?" for HHM) are exactly the things a human cannot see without pasting and an agent
cannot see about itself. Which is where the tools come in.

## Can typed tools support super-context awareness? One small idea, one big idea

**TODO — the SMALL idea: the status line.** `tt statusline` (shipped, SM039) formats the harness's own per-turn JSON
into one glanceable line: model · session $ · context% · weekly-quota%. It makes the TSM signals (headroom, reset)
*ambient* — no `/cost` paste, no guessing. Flesh out: how a live gauge turns mode-choice from a paste-and-ask ritual
into a glance; the trust angle (a number sourced from the harness itself, unlike the misleading inferred chrome).

**TODO — the BIG idea: a shared cost/usage/context awareness + the dances.** The larger move is a **super-harness**
(SM016) tap that gives *both* sides the same live picture — the agent can read its own context% (killing the
compact-trigger blind spot), the human sees usage without pasting, and the mode-switching **dances** (the compact
dance, the token-usage dance, the go/solo handoffs) run off shared signals rather than one party's guesswork. Flesh
out: the two-user harness (instrumented for the human's eyes, not the agent's stream); how a shared gauge lets us
*name and enter a mode together* deterministically; the input-tap that could even de-trigger accidental mode-switches
(the p-word). This is where super-context stops being a metaphor and becomes instrumented.

## Notes for the revoice
- Land the through-line: **naming the mode is the cheap coordination primitive; the tools make the mode's triggers
  visible.** Small idea = make one party's blind signal ambient (statusline). Big idea = make *all* the signals
  *shared* (the super-harness).
- Define HHM crisply (BR) — and whether there are more modes worth cueing (saving-mode, rest, ballgame vs ralph).
- Ties: `token-budget-modes`, SM039 (statusline/usage-cost), SM016 (super-harness), the token-usage + compact dances,
  the plan-mode/p-word finding (an *accidental* mode-switch — the failure case of un-shared mode control).
