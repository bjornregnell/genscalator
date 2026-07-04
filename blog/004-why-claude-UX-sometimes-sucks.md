# 004 — Why Claude's UX sometimes sucks (and how we'd fix it)

**Status: STUB.** TODO: mine WR data for UX problems.

> **The 004→008 arc.** Read in order **004 → 005 → 006 → 007**, then **008** (synthesis). Together they descend a
> stack — **Pains** (004) → **Practice** (005) → **Method** (006) → **Theory** (007): the Theory-Method-Practice-Pains
> chain, read bottom-up. *Backwards cliffhanger:* the thing you just felt has a deeper cause one post down.
> **You are here: the Pains.** → Next (005): the *practice* we grew to cope with them — the "dances".

**TODO: include the HUMAN's personal experience on the pain side.** These pains are *joint*, not just agent-observed
papercuts — write BR's own felt frustration in the first person: the mis-click that lands "yes" on the wrong prompt;
losing a carefully-typed message to the double-post race; the irritation of not being able to read `/context` while
the agent is mid-turn; watching the agent reach for raw bash for the Nth time. The human's lived friction is half the
data, and the more relatable half.

Source material to mine (Workflow-Research "WR data", logged live during real agentic runs):
- `research/wr-data/harness-ux.md` — the primary log. Recurring themes already captured there:
  - **Input races** — arrow-up "edit a just-sent message" → double-post; Enter landing on a confirmation prompt;
    agent-fired modals stealing the human's typing focus; `$(…)` command-substitution tripping the confirmation guard
    then a mis-click on the prompt.
  - **Can't read the gauge when you need it** — `/context` blocked while messages are queued / the agent is mid-turn,
    exactly when a human on a long AFK run wants a fill/rot read without interrupting.
  - **Bash-reflex cluster** — the agent reaches for `ls`/`cat`/`grep`/`echo`-glued compounds instead of typed tools;
    each is a small UX-and-safety papercut (noise, confirmation prompts, lossy composition).
- `research/smart-zone-ceiling.md` — fill-vs-rot, monitor-tick cadence as a rot knob.
- Related memories: harness-double-post-edit-race, no-interrupting-modals-during-flow, propose-compact-dance-at-trigger.

Angle (TODO firm up): these aren't random bugs — most are **one family**: *timing/observability races between a human
action and the system consuming a prior input*, plus *missing typed affordances* that push work into bash. For each,
pair the felt problem with a concrete harness-side ask (widen the edit window; read-only `/context`; a nonblocking
health gauge; typed tools that remove the reflex).

Pairs with the tt-toolbox DESIGN (`tools/DESIGN-single-dispatcher.md`) on the tooling half.
