# Brief the TOOL LANE, not the RULE LIST — a prohibition-only sub-agent brief armed nothing

**2026-07-17, afternoon.** **BR watched this one live and named it**, which is why it exists: *"wr data on the env
bash clobbery when sub-agent is trying to run sbt; it does seem to lack knowledge on how to do it; how to fix that
is a good question"*.

## What happened

The super-agent spawned a sandbox sub-agent to build + test introprog PR #943. The brief contained a **HARD RULES**
section, and rule 5 was, verbatim:

> *"**BASH COMMAND STYLE — CRITICAL.** A command guard will DENY you and waste the human's time if you get this
> wrong: Run **BARE** commands only. **NO `cd X && Y`**, **NO `;` chains**, **NO pipes `|`**, **NO redirects `>`**."*

**The minion then flailed trying to run `sbt`**, because `sbt` needs a working directory and every obvious way to
give it one was forbidden. BR watched it happen.

## ⭐ The diagnosis is not "it lacked knowledge". Two things were wrong, and the second is the finding.

### 1. The knowledge was MIS-HOMED (the easy half)

`introprog/autotranslate/scratch/at.scala` exists **for exactly this problem**. Its own header:

> *"Thin driver: run the autotranslate sbt project with arbitrary args, from the introprog root (**no cd**)."*

It takes the root as an **argument** and sets `cwd` internally. It is **TRACKED**, so it was sitting **inside the
minion's own sandbox clone the entire time it was flailing.**

**But its CONTRACT** — *this exists, use it, here is the calling shape* — **lives in
`muntabot-synch-introprog/notes/at-next-session-handoff.md`: the CLOSED work repo.** An agent cloned into
**introprog** has **no path to that file** and no reason to suspect it exists.

> ### **⇒ Substrate must live in the repo where the WORK happens, not in the OBSERVER's notebook.**

⭐ **This is SM132's audience-relative-home rule hitting a home we never enumerated: A FUTURE AGENT INSIDE SOMEONE
ELSE'S REPO.** Our home list is scoped to *our* substrate. introprog is another house, and we keep the map to it at
our place. **Sibling shape:** SM133 says a *retraction* must reach every home. This says a **capability** must too —
and capabilities have *more* homes than claims do, because anyone who works in the repo needs them, including the
external contributor (hmiddelk has this exact problem and even less access to our notes).

### 2. 🔴 The brief was PROHIBITIONS with NO TOOL TO COMPLY WITH THEM (the real finding)

The super-agent told the minion **what not to do** (`no cd`, `no pipes`, `no &&`) and **gave it nothing to do
instead**. It had `at.scala` in its hands and did not know it. It had `tt git` available and was told to use raw
`git`.

> ### **A rule is a SENTENCE. Sentences arm nothing. The only lever that works is giving the check somewhere to EXECUTE.**

⭐⭐ **THIS IS THE DAY'S OWN THROUGH-LINE, BITING ITS AUTHOR IN A NEW PLACE.** The finding — *nothing in-agent arms a
reflex; the only lever found is a TOOL CALL* — was written by this agent, hours earlier, and it wrote a
**prohibition-only brief** anyway. **The fix was never "brief harder". It was: hand it `at.scala` and `tt git`.**

> ### ⇒ **DELEGATION COROLLARY: brief the TOOL LANE, not the RULE LIST.**
> A constraint you cannot execute is a trap. **If you forbid a path, you owe the agent the path you left open** —
> named, with its calling shape. Otherwise the prohibition just converts a stall into a flail.

## ⚠️ The part that indicts the memory system itself

[[warm-delegated-subagents-lack-caller-skills]] **already says this** — *sub-agents don't carry the caller's skills;
warm the minion / constrain its tool-lane*. It is **in the index, loaded every session**. The agent **read it, and
shipped a prohibition-only brief anyway.**

⇒ **another `found-and-written-up ≠ armed` specimen — and this one is about the very memory that warns of it.**
⚠️ **Note what DID transfer from that memory: the "constrain its tool-lane" half became a list of BANS.** The "warm
the minion" half (give it what it needs) **did not.** ⇒ **a memory that contains both a prohibition and a
provision, applied under load, yields the prohibition.** *Candidate mechanism, n=1: prohibitions are cheaper to
emit — you can forbid without knowing the domain, but you cannot provision without knowing it.* **Unfalsifiable
from inside; flagged as a story about one observation.**

## The evidence that the fix is real (and its limit)

After BR's cue, the super-agent sent the minion `at.scala`'s calling shape. ⚠️ **At the time of writing, the
outcome is NOT YET KNOWN** — the minion had not reported back. **This note therefore does NOT claim the fix worked.**
It claims only what was observed: **the brief was prohibition-only, the tool existed and was unreachable-in-practice,
and BR had to catch it.** *(Follow-up: did the minion succeed after being handed the tool? That is the actual test,
and it belongs in this note's ties when it lands.)*

## ⇒ Actionable

1. **`autotranslate/scratch/README.md` IN introprog** (PB SM138) — calling shapes for `at.scala` + the sibling
   helpers, the AT flags, the absolute-path/no-`cd` convention. **Serves the minion AND hmiddelk.**
   ⚠️ **Scope-check: split what belongs to introprog (public, contributor-facing) from OUR workflow framing (closed).
   Do not leak WR vocabulary into a course repo.**
2. **A delegation-brief shape**: every prohibition line must be paired with the **sanctioned tool** for that need.
   *"No raw `git`"* is incomplete; *"no raw `git` — use `tt git`, here is its surface"* is a lane.
   ⚠️ **Caveat, and it is this study's own lesson: writing THIS DOWN is also a sentence.** Per the theory it should
   not arm either. **The armed version is a briefing TEMPLATE the spawner fills in — a slot, not a maxim.**
3. **Check the sibling case:** the same brief told the minion to use raw `git clone` / `checkout` / `log` / `diff`.
   **`tt git` has none of those** (see SM137 — the review lane is untooled), so on that axis the prohibition was
   not merely unhelpful, it was **impossible to satisfy**.

## Honest limits

- **n=1**, single minion, single task, and the model differs from the super-agent's (fable vs CO4) — **the flail
  may be model-specific and this is NOT controlled.**
- **BR is the only reason this was observed at all.** The super-agent **cannot see its minion's stalls** (same
  blindness as its own — see `sm129s-probe-counted-brs-pastes-not-stalls…`). ⇒ **without a human watching, a
  flailing minion looks identical to a slow one.** ⭐ That is arguably the sharper finding here and it is
  **untested**: the super-agent had no instrument that would have told it.
- ⛔ **NOT claimed: that prohibitions are useless.** The guard rules were *correct* and the minion (as far as is
  known) did not trip the guard. **Claimed narrowly: prohibition WITHOUT provision converts a stall into a flail,
  which is not obviously better.**

## Ties

[[warm-delegated-subagents-lack-caller-skills]] (**the memory that said this and did not arm**) ·
[[delegation-dance]] · [[introprog-build-and-sync]] · [[agent-cant-internalize-huge-codebases]] (**docs + probes —
this is the docs half, and it was missing**) · [[prefer-inrepo-tmp-over-slash-tmp]] · **PB SM138** (the build) ·
**PB SM137** (the review lane is untooled — why half the prohibitions were unsatisfiable) · **SM132** (home is
audience-relative) · **SM133** (fan-out) · `the-note-to-my-post-warp-self-reached-only-the-human-2026-07-17.md`
(**same author, same day, same shape: a sentence aimed at an agent that could not act on it**).
