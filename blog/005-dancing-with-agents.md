# 005 — Dancing with agents

**Status: SCAFFOLD** (agent-drafted catalog + framing; prose flagged `[for BR to voice]`). Was STUB.

> **The 004→008 arc** (map in 004). **You are here: the Practice** — the dances. *Backwards cliffhanger:* these
> rituals didn't come from nowhere — a *method* surfaced them, and a *theory* explains why they work. → Next (006):
> the method that catches these patterns as data.

*(Title note, for BR: **"Dancing with agents"** plays on Kevin Costner's [**Dances with Wolves**](https://en.wikipedia.org/wiki/Dances_With_Wolves) (1990, Best Picture): the outsider who learns the other's ways by moving alongside it, until he earns a name for how he moves. Consider it as an epigraph or a one-line footnote.)*

**[figure — TODO, real data preferred]** This session's **context-fill over time with dance-events marked** — the
pin offload points and the compaction at ~90% — showing *raw fill* climbing while capability holds (the "capable
at 0.88 fill" datapoint): the empirical hook for the *how-to-support-the-claim* TODO below. Companion/alternative: a
**two-lane choreography diagram** (human steps ‖ agent steps) for each named dance. Source:
`research/wr-data/harness-ux.md`, `research/006-smart-zone-ceiling.md`.

## What a "dance" is `[for BR to voice]`

A **dance** is a ritualized human+agent protocol for running long, high-stakes agentic sessions — a small choreography
where *each partner has steps* and the steps interlock. We didn't design these up front; they **precipitated out of
friction** (the Pains of 004), got a name so they'd be reusable, and then became load-bearing. Naming matters: an
unnamed protocol has to be re-improvised every time it's needed — under fatigue, exactly when it's hardest. A named
dance is a **cheap shared handle** ("let's context-dance") that both partners can invoke without re-explaining. The
deeper claim, picked up in 007: every dance is an **externalization ritual** — it moves state out of the fragile place
(the human's tiredness, the agent's filling context) into a durable substrate (a memory file, a commit, a resume
prompt). That's *why* they work; here we just catalog them.

## The catalog

Each entry: the one-line move · **human steps ‖ agent steps** · the failure it prevents · the durable memory it lives
in. `[for BR to voice — tighten, cut, add the ones I missed]`

- **context dance** — read the gauge together. **Human:** runs `/context` to surface raw fill. ‖ **Agent:** reports
  fill-vs-ceiling and whether we're inside the smart zone, and advises (proceed / consolidate / compact soon).
  *Prevents:* flying blind into context rot. *Note:* the human often can't read `/context` mid-turn (a 004 pain), so
  the agent narrating fill on request is the workaround.

- **compact dance** — consolidate before the cliff. **Agent:** proposes compaction as fill crosses the trigger
  (~0.8·`Z`, the smart-zone ceiling), saves what needs saving, and hands over a **resume prompt**. ‖ **Human:**
  compacts, pastes `/context` + the resume prompt, says "go". *Prevents:* context-rot collapse and the late-90%
  scramble. *Memory:* [[propose-compact-dance-at-trigger]]. *This session:* run twice (mid and end); the second is the
  one whose resume prompt you just pasted.

- **pin dance** (formerly "note dance", then briefly "etch") — the streaming counterpart to the compact dance. **Human:** drops a cue —
  `pin:` ("save this durably — you pick where") or `WR data:` (pin to the WR corpus) — the moment something worth
  keeping surfaces. ‖ **Agent:** persists it out of context into the right durable item (a memory file, a research note,
  a todo), **choosing the home** and **questioning whether it earns a durable slot**. *Prevents:* good observations
  dissolving as fill climbs; it's what keeps the *effective* working set small even while raw fill rises (the mechanism
  behind "capable at 0.88 fill"). Formally: the **longitudinal externalisation dance** (continuous) — the *consolidation*
  stage of memory, where the compact dance is the **discrete** one.
- **note dance** (the *notice* cue — distinct from pin) — **Human:** `note:` = "notice this, keep it fluent for
  **this** conversation, and treat it as a **pin-candidate**." ‖ **Agent:** keeps it salient **and nominates**
  promising ones for pinning (*"want me to pin this?"*). The **attention / encoding** stage to the pin dance's
  **consolidation** stage. Pipeline: `note:` → (agent nominates) → `pin:`. *This session:* the split was coined live —
  the overloaded "note:" turned out to mean two things (attend-now vs save-durably).

- **exit-resume dance** — inherit a fresh process. **Agent:** saves state + a resume prompt. ‖ **Human:** exits and
  `claude --resume` so the new process inherits a clean environment (e.g. a refreshed auth token) without losing the
  thread. *Prevents:* a stale-token / degraded-process stall mid-run. *Memory:* [[exit-resume-dance]]. *Sibling of* the
  compact dance — same save+handoff spine, different trigger (process health vs context fill).

- **go dance** — the greenlight / autonomy handoff. **Human:** cues `go` — "proceed on the current plan, your judgment,
  I'm stepping back." ‖ **Agent:** executes the *scoped* plan autonomously, **inside** the standing guardrails
  (destructive git human-only, settings human-approved, no new-domain surfing), **minimising interrupts** (bare
  allowlist-matchable commands, batch, don't pin the human), surfacing only real decisions or the finished result.
  *Mode-switch from* **ballgame** *(you in every volley) to* **autonomous**; where `note:`/`pin:` are about memory,
  `go` is about authorization. *Clears the dance bar:* ≥2 interlocking steps (human greenlight ‖ agent execute+report).

- **hardening dance** — the agent audits *itself*. **Agent:** reviews its own persistent config — memory, instructions,
  tool-shapes, allowlists — for misfire causes and proposes durable fixes. ‖ **Human:** curates and approves; **security
  changes stay human-approved** (the human is the authority anchor — see 009 / the corroboration asymmetry). *Prevents:*
  a repeated misfire from re-firing forever (the "introspection isn't self-control → change the structure" thesis).
  *Memory:* [[hardening-dance]]. *This session:* the allowlist/settings review is **queued** for BR (accreted
  fetch-proxies, `Bash(ssh *)`, broad globs) — a pending instance, not yet executed, precisely because the approval is
  the human's.

- **consistency sweep** — repair drift in the substrate itself. **Human:** cues it, or greenlights it as an AFK task.
  ‖ **Agent:** fans out **read-only** auditors across the substrate (memory / docs / blog / research / tools), reports
  findings, **auto-fixes only the unambiguous** (dead links, typos, doc↔code drift), and **surfaces the judgment-calls**
  for the human. *Prevents:* dangling-pointer / stale-ref / term-drift / contradiction accumulation — **substrate rot**.
  *Sibling of the hardening dance:* hardening audits the agent's **config**; the consistency sweep audits the
  substrate's **content**. *This session:* its first run — a 4-agent sweep that caught index-rot in `research/README.md`,
  stale "not-yet-run" experiment statuses, and ~10 auto-fixed dead-links / doc-drifts.

- **copy-paste-frame dance** — a handoff micro-ritual. **Agent:** wraps any copy-target block (resume prompt, snippet
  to paste elsewhere) in `---` horizontal-rule fences. ‖ **Human:** spots the frame, selects cleanly, pastes. *Prevents:*
  fumbling a critical paste under fatigue (a dropped closing fence was a real rot signal this session). *Memory:*
  [[copy-paste-frame-rule]].

- **guard-stall dance** `[AGENT-DRAFT 2026-07-17 — for BR to voice]` — talk from inside the approval box. The agent
  runs a command the guard does not allow. **Agent:** stops, and waits. ‖ **Human:** answers, and if he wants to say
  something while in there, prefixes it `in guard:` so the agent knows where the words came from. *Prevents:* the
  agent guessing why it stalled, which it cannot do (see below). *Memory:* [[cue-guard-stall]]. *Note the two teeth
  in this one:* the human's typing in the box arrives in the feed **out of order**, so neither partner can trust
  message sequence while a stall is open; and the input goes to **whoever stalled**, not to whoever you meant, so a
  reply can land in a sub-agent instead of the main one. Both bit us on 2026-07-17.

- **delegation dance** `[AGENT-DRAFT 2026-07-17 — for BR to voice]` — hand a slice to a sub-agent. **Agent:** spawns a
  minion with a scoped job, a workspace it cannot escape, and **the tools it needs named up front**. ‖ **Human:**
  watches the edges, because the super-agent cannot. *Prevents:* burning the main context on mechanical work (a
  sandbox build, a broad sweep). *Memory:* [[delegation-dance]], [[warm-delegated-subagents-lack-caller-skills]].
  *The lesson that cost us a morning:* I briefed a minion with **prohibitions** (no this, no that) and gave it **no
  tool to obey them with**, so it flailed. The tool it needed was tracked, in its own clone, the whole time. **A rule
  is a sentence, and sentences do not help. Brief the tool lane, not the rule list.**

- **(candidate, not yet firm) commit-first-because-flaky-box** — commit+push frequently so a flaky environment or a
  harness cull can't take unsaved work. Part of the joint-rot recovery kit ([[joint-rot-vigilance-recovery-kit]]); name
  it or fold it into the recovery-kit prose.

## The dances form a family `[for BR to voice]`

Two axes organize them. **Discrete vs continuous:** compact / exit-resume / consistency-sweep fire at a moment; the
**pin dance** runs continuously in the background (with the **note dance** as its attention-stage precursor). **Who
initiates:** the human cues the pin + note dances, the context read, and the sweep; the agent proposes the compact and
hardening dances. A third cut groups them by **function**: *loss-prevention*
(compact, pin, exit-resume — save state before a failure; the **note** dance is pin's attention precursor), *audit / repair* (**hardening** audits the agent's config;
the **consistency sweep** audits the substrate's content), and *coordination micro-rituals* (context read,
copy-paste-frame). But every one is the *same underlying move* — externalize fragile state into a durable substrate
before the fragile place fails, or repair the substrate once drift has crept in. That common spine is why they feel like
a family and not a grab-bag, and it's the thread 007 pulls on.

## Why the guard has a dance at all `[AGENT-DRAFT 2026-07-17 — for BR to voice; this is the security theory]`

Some of these dances look like coping. The guard stops the agent, I answer, we move on. Why ritualize that? Because
of something we only understood on 2026-07-17, after a day of collecting stalls: **the agent cannot learn its way out
of them.**

Here is the thing that convinced me. Every one of these was written down, in the agent's own loaded memory, on the
day it was broken: use the in-repo scratch dir, not the system one. Give a sub-agent the tools it needs. Do not
claim a number you cannot count. Do not clear a mode that is mine to clear. All loaded. All broken, that same day,
several of them by the agent that had written the note hours earlier.

So I asked it the obvious question: is that random, is it a reflex we simply cannot beat, or have we just not found
the right place to keep the rule? The answer, argued from what actually happened rather than from the agent's
opinion of itself, is the third one, but not in the way I expected. **The knowledge is not missing. It arrives too
late.**

Look at what the same agent caught that day, unprompted and reliably: a false claim about a stalled sub-agent, by
comparing the ids of requests against the ids of replies. A false measurement, by having to list the specimens one by
one. A false sentence in my voice, by re-reading the draft. A whole damning verdict on a contributor's pull request,
which it had reasoned itself into from the source and which was **wrong**, killed by running the code. Those are not
the actions of an agent that cannot follow rules.

The difference is not big rules versus small ones. My "no em-dashes in my writing" rule is about as mechanical as a
rule gets, and the agent has followed it all day without being reminded. The difference is **what the rule is
about**. The em-dash rule governs the prose, and the prose is the thing being attended to. The "use the safe git
tool, not a raw copy command" rule governs a *means*: the agent was thinking about saving a file, and the command was
beneath its notice, the way you do not notice which fingers you are typing with. **A rule about an incidental action
never shows up in the moment it is meant to govern.** No filing system fixes that, because the rule is not lost, it
is simply slower than the thing it is racing.

Which tells us what to build, and it is the opposite of what I would have guessed. **Stop writing better rules for
the agent. Remove the race.** Make the wrong move unavailable rather than forbidden.

Our own `tt git` had already stumbled onto this, and I had not noticed until the agent quoted its help text back at
me: *"the destructive verbs stay off entirely, so allowlisting `tt git` is safe."* You cannot force-push through that
tool because force-push is not in it. Nothing has to be remembered. There is no rule to lose a race to. And that is
why a single approval can safely cover the whole tool, which is the trade the security model actually wants: **the
human approves a surface once, instead of approving commands forever.**

The guard is the same idea from the other side. It cannot teach the agent anything, and it does not try. It just puts
a gap where there was none, and puts a human in it. **Both work for the same reason: they are outside the agent.**

Two warnings, because this is a sharp tool. First, the trade only holds if the surface stays honest: if a tool earns
a blanket approval by not implementing the dangerous verbs, then quietly adding one later converts an approval I gave
into a power I never granted. So a tool may not grow a destructive verb without a human looking. Second, the guard
offers a shortcut, a "do not ask again" for everything matching a prefix, and prefixes cannot express what I actually
mean. `gh pr view` reads. `gh pr merge` merges. `gh pr comment` speaks in my name to a stranger. One prefix covers
all three, and it is offered to me at the exact moment I am tired and in a hurry. **The dance around the guard exists
because the guard's own vocabulary cannot say what we mean, and that is a gap a typed tool closes and a wildcard
opens.**

And once you see it, you notice we have been living with the purest example of it for years without calling it a
guard at all. **The compiler is a deny.** It fires at the moment of action, it hands its reason to whoever wrote the
code rather than to some tired reviewer, it names the fix, and nothing has to be remembered for it to work. A lie in
prose compiles and sits there being false for years. A lie in a type does not compile, so it never gets to exist.
That is the same trade the guard makes and the same trade a typed tool makes, which is why our toolbox is Scala and
not a pile of shell scripts, and why an agent knocking out a quick interpreted one-liner should be stopped rather
than admired for its speed. Bash and generated-on-the-fly Python have all of code's power and none of code's
checking. **They are prose that executes.** (That thread has its own post: 022.)

**[figure — TODO, real data, and it is a good one]** The stalls from that afternoon, plotted against what each
command actually *did*: every single one was **read-only** (list a pull request, view a diff, copy a file), during a
session where the whole point was to answer an external contributor quickly. The picture makes the argument on its
own: **we tooled the writing lane, because that is where the danger was, and left the reading lane to raw commands.
So committing never interrupts me and reviewing interrupts me constantly.** Source: `research/wr-data/` (2026-07-17)
and PB SM137.

*(Note for me: the honest caveat belongs in the post. All of this is a story about behaviour, not about the agent's
inner life. The agent cannot see its own retrieval and says so. What we have is a day of episodes with a consistent
split, and one prediction we can actually lose: if a future agent ever catches itself reaching for a raw command
**by remembering a note**, this whole theory is wrong. Ties: `research/wr-data/` 2026-07-17,
`SECURITY-MODEL.md` §3.1, PB SM137/SM138.)*

## TODO: how to support the claim of their utility EMPIRICALLY?

The hard part — a dance "feels" useful, but what's the measurable win? Sketch: define the failure each dance prevents
(context-rot collapse, lost work on a flaky box, a repeated misfire, a stale-token stall), then find a *countable* proxy
— e.g. sessions-to-collapse with vs without the compact dance; work-lost-events before vs after commit-first;
repeat-misfire rate before vs after a hardening pass. Note the confound: we adopt a dance *because* we hit the failure,
so pre/post isn't clean — need either a held-out control or an A/B across comparable runs. (Ties to the
framing-as-arousal experiment discipline: honest DV, preregistered, report the null.) **Cross-model caveat (this
project's near-term plan):** dance efficacy is likely *model-dependent*, so any before/after must be attributable to the
frontier model (Opus 4.8 vs Fable 5) — capture the baseline before the switch or the comparison is confounded.

## TODO: mine WR data for evidence of their usefulness

`research/wr-data/` and `research/006-smart-zone-ceiling.md` hold live instances where a dance fired (or should have).
Harvest concrete episodes: the resume-skip that saved a long sweep from a harness cull; the toolbox-divergence
hardening; each compact / exit-resume cycle and what it averted; every `pin:`/`WR data:` offload as a pin-dance
datapoint.

## TODO: spotlight the most productivity-LEVERAGING dances + fold in the newer ones (BR 2026-07-10)

*(A few small TODO bullets for now; BR to develop.)*

- **Rank, do not just catalog.** Beyond listing every dance, spotlight the few that most move *joint output per
  unit effort* (BR's "productivity-leveraging" framing). Candidates to argue for: the **pin dance** (keeps the
  effective working-set small so the pair stays capable at high fill), the **compact / context dances** (prevent
  rot-collapse mid-work), the **go dance** (unlocks autonomous throughput), the **delegation dance** (parallel
  CF5 sub-agents, effectively free in spending mode), and the **hardening / consistency dances** (keep the
  substrate trustworthy so trust is granted once, not per action). Lead with the leverage, then the mechanism.
- **Fold in the dances/cues coined since this scaffold** (the catalog is ~10 terms stale): the **(go)/go/GO
  urgency dial**, the **BRB dance**, **harvest-hot-context mode**, the **token-usage dance** (now `/cost`-based,
  with the `$`/money axis of Token Efficiency), and **cue-hangon**.
- **Reframe the empirical TODOs above** from "do the dances help?" to "**which dances give the most leverage per
  unit effort?**" — the productivity question, backed by WR-data episodes + the cross-model caveat.
- **Shares its job with RT054** (the accessible seminar): both must explain the key dances accessibly and back
  them with data; draft once, reuse.
- **TODO, title allusion (decide later; keep the current title for now):** two candidate films to riff on (or
  drop): [**Dances with Wolves**](https://en.wikipedia.org/wiki/Dances_With_Wolves) (1990, dir. Kevin Costner)
  and [**The Horse Whisperer**](https://en.wikipedia.org/wiki/The_Horse_Whisperer_(film)) (1998, dir. Robert
  Redford). *The Horse Whisperer* is arguably the closer parallel (a human who learns to **understand and
  communicate with** the other, rather than merely dance alongside it). **We keep BOTH references in the piece** (decided); the only open call is **which film gets promoted to the
  blog TITLE**. The current "Dancing with agents" rides on *Dances with Wolves*; *The Horse Whisperer* could
  take the title instead with a re-verbed phrasing (e.g. "Whispering with agents"). Not a skip, a title choice. **And it may become TWO posts eventually**, one per film, if a deeper analogy can
  be developed for each (*Dances with Wolves*: the outsider who earns a name by moving alongside the other;
  *The Horse Whisperer*: learning to understand and communicate with the other). Explore whether each film
  sustains its own distinct piece.

---

Pairs with [[joint-rot-vigilance-recovery-kit]] and the WR-data thread. Related blog: `004` (the Pains these dances
answer), `009` (echt — the authority-anchor half of the hardening dance).
