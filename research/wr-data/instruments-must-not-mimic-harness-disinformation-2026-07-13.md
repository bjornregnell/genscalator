# WR data: awareness instruments must NOT mimic harness disinformation (2026-07-13)

**Type:** WR data — a DIFFICULT design tradeoff, flagged by BR as *"critical to awareness to get it right"*.
**Status:** CAPTURED; the design decision is deferred to **SM062** (BR: *"too difficult to decide now"*).
**Threads:** [[joint-clock-two-party-progress-stalls-2026-07-13]], the disinfo-survives-compact WR line,
[[echt-effort-especially-self-generated]], [[status-plus-mode-line-prototype-2026-07-13]] (declared-not-derived),
[[cue-bare-auto-compact]], blog 004 (why Claude UX sometimes sucks), the awareness/observability strand (blog 020).

## The tradeoff (BR's words)
*"we discovered a very difficult thing; a difficult tradeoff; we dont want to mimic harness disinformation."*

We are building **ambient awareness instruments** — the status line, the mode line, the compact-chrono, the
bing-bing. Their whole value is telling the truth about joint state so the human can steer. **But any
persistent display that is not continuously refreshed will LIE the moment the underlying state changes and the
display does not.** A stale display that still *looks* live is **disinformation** — it asserts something false
as current. So our instruments risk becoming instances of the very harness pain we criticize.

## The harness patterns we must not replicate
The Claude Code harness already does this, and it is a known UX pain (blog 004):
- **The sticky `Next:` line.** Live specimen handed in 2026-07-13 mid-session: the spinner showed
  *"Next: Build blog index page for the published set"* while we were deep in clock/awareness/WR work — a
  **stale predicted-next-step shown as the current plan**. (This is the disinfo-survives-compact finding: the
  stale `Next:` is sticky and survives the warp.)
- **The injected clock / "felt time".** The agent has no felt time ([[agent-lacks-felt-time-rebind-at-boundaries]]);
  an injected timestamp can be stale relative to now.
- **Frozen status fields at idle** (see below).

## The live instance that crystallised it: the frozen status-line clock
Mid-conversation the status-line clock **froze at `15:55:21`** and BR flagged *"stuck on genscalator: 15:55:21"*.
Cause (confirmed via claude-code-guide): the `statusLine` command is **event-driven, not timed** — re-invoked
after each assistant message, after `/compact`, on mode/vim changes, debounced ~300ms during activity — and
**goes quiet at idle**. So the clock shows *last-activity* time but *looks like* now = disinformation, and it
freezes at exactly the mutual-idle stall of [[joint-clock-two-party-progress-stalls-2026-07-13]].

### This instance IS fixable (but the fix has a cost — the tradeoff in miniature)
The `statusLine` object takes a **`refreshInterval`** field (minimum **1 second**); with `"refreshInterval": 1`
the command re-runs even at idle, so the clock ticks live. **Cost:** it spawns the statusline subprocess every
second while idle (a small but nonzero resource + a subprocess-per-second). So even the *fixable* case is a
**honesty-vs-cost** tradeoff. For a clock field, honesty wins and the cost is small — **candidate: set
`refreshInterval: 1`** (a settings change → human-approved, mirror per [[settings-local-mirror]]; NOT
self-applied).

### The design TARGET (SM062, joint): the clock-stop as an HONEST awareness cue — not just "always tick"
BR's key refinement: *"based on how we ACTUALLY want the clock stop to work as awareness-supporting cue after
thinking hard the both of us."* So the goal is **not** simply to paper over the freeze with `refreshInterval: 1`
(which *hides* the mutual-idle stall). There is a **third option** beyond "frozen-and-lying" and
"always-ticking":
- **frozen-and-lying** (current default): stopped clock *looks* live → disinformation.
- **always-ticking** (`refreshInterval: 1`): honest "now", but it **erases a real signal** — that we are at the
  quiescence stall.
- **stopped-and-honest** (the candidate target): let the clock stop, but **render it AS stopped** — dim/greyed,
  a ⏸ marker, or reframed "paused HH:MM (idle)" — so the very act of stopping becomes an *awareness-supporting
  cue*: you can SEE the joint clock has stopped at mutual idle ([[joint-clock-two-party-progress-stalls-2026-07-13]]).
  This turns the disinformation liability into an honest indicator of the two-party stall.
A live clock and a stall-indicator are not mutually exclusive — e.g. a ticking clock during activity that
switches to a visibly-paused state at idle. The exact rendering is for SM062 to decide jointly. The principle:
**do not hide state and do not fake state — show state, including "we are stalled", truthfully.**

## Why it is genuinely DIFFICULT (not just "keep it fresh")
1. **Some staleness is inherent, some is fixable.** The clock is fixable (`refreshInterval`). The *mode line*
   is DECLARED-not-derived — it is only as fresh as the last human/agent declaration, so a forgotten `rm`
   leaves a **false chip** (we hit this live: the stale `racing`/`high-context`/`rot-vigilance` chips survived
   a compaction and had to be cleared). No interval fixes a declared value going stale — only **active
   hygiene** does.
2. **The richer/more persistent the instrument, the bigger its disinformation surface.** Persistence is the
   feature AND the hazard.
3. **The mitigations themselves cost or clutter:** continuous refresh (resource), staleness markers
   (dim/greyed/"as of HH:MM" framing — honest but noisier), reframing a field from "now" to "last tick"
   (honest but less punchy), or simply *not showing* a field that can silently go stale.

## Candidate design principles (for SM062 to decide)
- **Never present a stale value AS current.** If a field can go stale, either refresh it, mark it stale, or
  reframe it honestly (e.g. the clock as "last tick", not "now").
- **Declared state needs active-hygiene discipline** (and ideally automation): a mode chip left on is a lie
  about the joint state-of-mind. The racing removal-trigger work ([[cue-we-are-racing]]) is exactly this kind
  of hygiene; generalise it to every declared mode.
- **Prefer honest-but-humble over impressive-but-lying.** An instrument that quietly goes wrong is worse than
  no instrument, because it is *trusted*. This is [[echt-effort-especially-self-generated]] applied to UI.
- **Don't out-source the judgement to the harness's bad habits** — we criticise the sticky `Next:`; we must
  not ship our own sticky `Next:`.

## Positive dogfood (getting it right, same session)
BR, seeing the empty mode line render `genscalator: clear: no active mode labels`: *"no disinfo there ;) very
good dogfood."* The empty-state placeholder **truthfully reports the absence** — it says "clear", it does not
fake active modes. That is the principle passing its own test in the live UI: an honest empty state, not a
misleading one. The contrast with the frozen clock and the sticky `Next:` is the whole point — same instrument
family, one field honest, others not (yet).

## Why it matters (BR)
The mode/status/awareness instruments are the observability spine of the whole collaboration (the asymmetry the
status line closes: the agent cannot self-read fill/rot, the human steers from the display). If that spine
**misinforms**, it actively misleads the steering — worse than silence. Hence *"critical to awareness to get it
right"*, and hence SM062 (the mode add/rm/declaration model must be designed under this constraint).

## Refinement from live dogfooding (2026-07-13): staleness is FIELD-TYPE-dependent
Dogfooding the mode line live the same session surfaced a distinction that **partly resolves** the difficulty:

**Two kinds of instrument field have different staleness semantics.**
- **Measured / derived fields** (the clock, ctx-fill, cost) track a world that **moves on**. The instant the
  world changes and the display does not, they go stale → disinfo. Fix = **refresh** (`refreshInterval`),
  mark-stale, or honest reframing ("last tick").
- **Declared fields** (the mode chips) hold a value that **does not change on its own**. A declared chip stays
  **true** until someone re-declares it. It cannot go stale by the world moving — only by a human/agent
  forgetting to update it. Fix = **active hygiene** (discipline/automation to clear/update declarations; the
  `racing` removal-trigger is exactly this — [[cue-we-are-racing]]).

**Corollary:** the disinfo RISK — and its FIX — is field-type-dependent. A frozen clock is disinfo (time moved
under a frozen number); a persistent mode chip at idle is NOT disinfo (the declaration is still true). Measured
→ refresh; declared → hygiene. Different failure modes, different fixes. **This taxonomy should anchor SM062.**

**Live dogfood confirmations (same session).**
- Declared-state renders promptly and correctly: BR — *"genscalator: token-spending — it appeared instantly
  when you ate the msg. GOOD!"* The chip updates on the next tick (the same event-driven render as the clock);
  because declared state does not drift, prompt-render + persist is exactly right for it.
- The frozen clock, twice, live: BR — *"stuck on genscalator: 15:55:21"* and *"clock is not ticking while i
  type this"* — the measured field going stale in real time, the taxonomy's other half, felt directly.

**A parallel echt move (same session): the title-humility edit.** BR retitled blog 004 *"(and how we'd fix
it)"* → *"(and how genscalator tries to fix SOME of it)"*. Same principle one level up: **do not overclaim.** An
instrument must not overclaim state; a post must not overclaim its fixes. Echt register applied to the title,
mirroring the disinfo constraint on the UI. Ties: [[echt-effort-especially-self-generated]],
[[publications-match-br-register]].

## The archetypal volatile declared field: human presence ("i am here")
BR, wryly, same session: *"br said 'i am here' — BR suddenly needed to pee; that can happen..."* He had opened
the task with *"(jointly as needed, i am here)"* and then, within seconds, went AFK. A perfect live specimen:
**human presence is a DECLARED state (like a mode chip), and the most volatile one there is.** It can go stale
**instantly and involuntarily** — biology (a pee), a word to a spouse, a doorbell — and the human usually
*cannot or will not re-declare in the moment* the state changes.

So within the field-type taxonomy above, presence is a declared field whose staleness is driven not by "forgot
to update" but by a **sudden involuntary state-change**. Its half-life is **short and unpredictable** ("that
can happen"). Consequences:
- The agent must hold *"i am here"* **loosely** — it is a point-in-time *claim*, not a durable fact. Never
  assume continued presence from an earlier declaration.
- The hygiene must be **cue-driven and cheap**, because the human won't do bookkeeping mid-interrupt: `afk`
  added on leaving, cleared on the return cue *"i am back"* ([[cue-brb]], [[cue-go-afk]]). The mode line tracks
  the volatility **only if** those cues fire — and the gap between the bio-interrupt and the cue is an
  irreducible staleness window.
- This is *why* the bing-bing and the afk-mode exist at all: presence cannot be assumed, so the system is built
  to detect departure/return rather than trust a standing "here". It is also the human side of the joint clock
  ([[joint-clock-two-party-progress-stalls-2026-07-13]]): a bio-interrupt is an **unplanned quiescence** — the
  human's clock stops without notice.
- Human-factors truth, stated plainly: the human is a *body*; interrupts are involuntary and unannounced. A
  robust collaboration is built to tolerate that, not to assume a declared "here" stays true.

**→ Candidate: a "me go pee" dance (BR, 2026-07-13 — PLACEHOLDER, BR to elaborate; noted not designed).** BR:
*"that's why we need a (me go pee)-dance ... more coming."* A protocol for the human's sudden bio-interrupt:
graceful entry (declare `afk`, agent switches to safe-solo / prep-only), the return cue (*"i am back"* → `rm
afk`), and the bing-bing/return-handling. The afk-mode add/rm and the bing-bing are already the pieces; the
dance would name and formalise the whole entry→away→return loop. Deferred until BR says more.
