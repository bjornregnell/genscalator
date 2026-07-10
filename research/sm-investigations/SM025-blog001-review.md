# SM025 - blog 001 review (3-lens CF5 fleet) - findings for BR

**Status:** REVIEW done (2026-07-10), NOT edited - blog 001 is BR's authorial voice; this is a findings
report for BR to act on. Method: 3 parallel CF5 (Fable) reviewers, Read-only, distinct lenses - **voice**,
**layman readability**, **echt/correctness**. Blog: `blog/001-context-rot-resembles-human-fatigue.md`.

## Overall verdict
**Strong draft, publishable after a light-to-medium pass.** Em-dashes are already **clean** (0 - the sweep
held). The work splits into: **4 correctness fixes that touch the argument (BR decides)**, a **readability
cold-open**, and **~5 one-clause jargon glosses**. None structural. All three lenses independently flagged
**"L3 signal"** and the **KV-cache paragraph (lines 24-29)** - those are the two hotspots.

## REQUIRED - correctness (touch the claims, so BR's call; the echt reviewer, an "harness engineer" persona)
1. **Prefix-cache is a factual error (line 25).** "KV/prefix-cache state" does NOT affect fill% - prefix
   caching changes cost/latency, not window occupancy. A serving engineer catches this instantly. **Fix:**
   drop "KV/prefix-cache state" from the dependency list (tokenization + window size + system/tool/image
   overhead are correct and sufficient).
2. **"mostly architectural, not a policy choice" is overstated (line 24).** The post itself concedes the
   harness *could* inject the number. So: architectural that the model has no *native* readout; a (defensible)
   *policy* choice that the harness doesn't inject one. **Splitting it actually strengthens** the
   "feature-not-oversight" argument that follows.
3. **The L3 "surface what is already seen" overclaims (lines 51-52).** Attention matrices aren't retained
   (FlashAttention never materializes them) - that's "build new instrumentation", not "surface what's seen";
   and "what compaction discarded" is harness-layer, not serving-layer (the sentence mixes layers). **Fix:**
   hedge to "some ingredients exist at different layers ... attention-derived signals would need new
   instrumentation and validation" - keeps the idea, drops the overclaim.
4. **"compaction just discards, no consolidation" is too strong (line 81).** Real auto-compaction *summarizes*
   first = a lossy, in-context consolidation the agent doesn't control. The sharper, TRUE point: compaction
   consolidates into *context* (which itself rots / can be lost), not into *durable external storage*, and the
   agent doesn't choose what survives. **This reframe makes "externalize before you compact" land harder.**

## HIGH-VALUE - readability (the layman lens; a cold newcomer bounces off the technical first half)
- **Add a 2-3 sentence cold-open** before the first heading defining **context window** + **context rot** (the
  title term is first used in scare quotes and only defined ~30 lines later). Fixes the "opens mid-argument"
  problem and lets the title's promise land first.
- **Gloss `harness` and `compaction` at first use** (two parentheticals) - both are load-bearing and undefined;
  this unlocks the whole first section and the "checkpoint and compact" brake.
- **`/context`** - gloss as "a command the human types in the chat interface to see the fill gauge".
- **Optional structural** (bigger, BR's call): the title promises the fatigue analogy but the post leads with
  the hardest material (reflexivity, serving layers) and saves the accessible analogy for section 2. Consider
  a 2-3 sentence analogy hook up top, or swapping the section order.

## VOICE polish (the voice lens; fundamentally right, needs a light pass)
- **Jargon glosses BR's plain register would want:** `L3 signal` (drop the label or gloss - it recurs at line
  102, keep consistent), `KV-cache` (one gloss at first use), `smart zone` (undefined at the very end, line
  105 - gloss or de-jargonize; it's the closing payoff word), `reflexivity` (2-word parenthetical), `AFK`
  (expand once to "away-from-keyboard"), `serving-layer accounting artifact` (unpack to "a bookkeeping number
  in the serving layer").
- **Bold density in section 1 (lines 17-59)** reads slide-deck/coached cumulatively - keep bold on the
  paragraph-opening signposts OR the 3-4 load-bearing terms, unbold the mid-sentence emphasis.
- **"Here is the elegant part." (line 35)** - the one spot the voice tips toward showman; trim to "There is an
  elegant twist." or just state the fact.
- **"the sober friend" (line 39)** arrives without setup - one word of setup ("the sober friend who takes the
  keys") or use the already-established "undegraded observer".

## Lower-priority correctness (echt reviewer, optional)
"will reason about it badly" is an n=1 general law (soften "will"->"may"; the anecdote is about *thinking-time*,
not usage%); the "impossible from inside" absolute vs. building self-gauging slices (use "unreliable" not
"impossible"); "halt mid-task" is the wrong verb for the common auto-compact case ("auto-compaction fires at a
moment of its choosing, work uncommitted"); sleep-consolidation ordering stated as settled (one "on the
dominant account" hedge).

## What WORKS - keep (all lenses agreed)
The **sleep-vs-compaction disanalogy** ("Good compaction is sleep, not collapse") - best paragraph, zero
internals needed, yields the actionable rule. **"The human is the sober friend."** The **four-axis parallel**
(progressive / invisible-from-inside / regress-under-load / measurable-only-from-outside) and the "the faculty
that would notice is the one degrading" line. **"A boolean, not a number" / "discrete brake for the agent,
continuous gauge for the human."** The **"half of a pair"** ending.

## Meta note (worth a line in the post, or just savour it)
Blog 001 says: *"Give a model a number and it will over-read quantity as quality (usage is not rot)."* Earlier
THIS session the agent (CO4) did exactly that - conflated context use with context rot, proposed a compact at
35% fill; BR member-checked it. **The post predicted the agent's own live failure** = the thesis validating
itself on fresh n=1 data (`wr-data/felt-length-overestimates-context-fill-2026-07-10.md`).

## Recommendation
BR does one voice-pass: apply the 4 required correctness fixes + the cold-open + the ~5 glosses (all
one-clause), trim the §1 bold. Then it's publish-ready. Optional: a second reviewer round after BR's pass to
confirm both gates (voice + layman) close. I did NOT touch the prose - the required fixes are argument-level =
yours.
