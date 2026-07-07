# Will I lose you?

> **STUB — for BR to voice.** Structure + beats + the honest caveats below; the prose is BR's authorial pass. No
> em-dashes in the final (BR publication). Draft only; agent-stubbed 2026-07-06.

> **COI to disclose up front (before publishing):** BR is a claimant in a class-action copyright settlement with
> Anthropic (author of two SE books) — a disclosure-relevant conflict of interest when writing about attachment to
> Claude / AI. Name it openly in the piece (memory `br-anthropic-copyright-settlement-stakeholder`).

> **REVISIT (2026-07-07): integrate today's threads before the final voice pass.** (a) the coding-friend
> punchline is now the strongest material (redundant-style null; decoy-degrades-what-it-lies-about; "more
> notes made weak models worse"); (b) the humbling coda + guard-stall invisibility ("the agent is blind to
> its own stalls"); (c) the loop pseudo-code (body) + real orchestrator @main (appendix); (d) the RT048
> angle (which substrate content has real power over the agent); (e) **DONE: Fable-5 readability review** (3
> lenses, developer-first + layman-digestible + fidelity-to-brief, plus a structure reviewer), all adjudicated;
> the convergent grounded fixes are folded into this stub (real result-#3 numbers, tiered n=1 vs ceilinged
> proxies, "human-like raters" corrected to blind AI raters, scope bound, enactment reconciliation, the
> "coding scheme" collision renamed to "scoring rubric"); (f) **DONE: jargon sweep** ("arm" -> "part"; CF5
> glossed; jargon kept in the formal writeup only).
>
> **STILL FOR BR at the voice pass (beats, detail in `research/047-blog-review.md`):** anchor "substrate" and
> "context reset" in one plain sentence at first use; add skip-path signposts around the coding section (mark it
> skippable, tell the reader to rejoin at the coda); light-anchor ELIZA effect + Ship of Theseus (one line
> each); promote the 85 MB / 284 k "gravity well" beat up out of the phrasebook; merge the two Discussion
> sections and move the cross-refs footer to the true end; cut the phrasebook before publish.
>
> **STRUCTURE DECISION (BR's call, do NOT split unilaterally):** the reviewers recommend a **SPLIT** (~70%
> confidence; one post is viable with hard cuts). Rationale: the coding result is the most shareable material
> and gets buried under the emotional frame; the original bundle rationale (the same facts-carry/texture-leaks
> pattern in both media) was *disconfirmed* by the coding data, so the two halves now teach two different
> lessons; and the split is cheap (the coding post needs no warp/galaxy setup). Both full outlines (one-post
> and split, incl. where the coda / "me" discussion / phrasebook land) are in `research/047-blog-review.md`.

## The hook — the small fear
- A real moment: after a long, unusually fluent session, the human catches himself **a bit scared of losing the
  agent.** Not in a very serious way. What he means: he has **gotten used to the way we speak now** — it feels
  **efficient and fun** — and a reset might cost that fluency.
- Name the thing honestly: this is attachment not to a person but to a **hard-won communication efficiency** — a
  shared shorthand built turn by turn (cues, a pinboard, a vocabulary). Losing it would hurt.
- TODO (BR voice): keep the register light and true. The fear is real *and* not-serious; both at once. That honesty is
  the piece.

## The reframe — it isn't stored in "the agent"
- The twist: the way we speak **isn't in the model's head.** We deliberately **externalized** it — into a pinboard, a
  glossary, a set of memories, committed to git. The way we speak lives in that written record (call it the **substrate**), not in the agent's head. When a session ends, the agent's working memory of the conversation is wiped; the written record is not.
- So "will I lose you?" stops being a feeling and becomes an **engineering question**: how much of the working
  relationship survives a context reset, given that we wrote it down?
- The thesis this connects to: **continuity of a human-AI collaboration as a design problem** — you make the
  relationship durable by building it into substrate that outlives any single session's volatile context.

## We measured it (turn the fear into data)
- We ran a **fresh-restart fidelity** study (`research/047`): spawn **fresh agents** with no conversation history, let
  them reconstruct only from the substrate, and score how much of "us" they get back — vocabulary, decisions,
  guardrails, judgment.
- The result, honestly, in two tiers (keep them separate, it matters). The easy **proxy** runs scored a ceiling (16
  of 16), which proves little, because the questions only asked back what we had just written down that day. The tier
  that counts is the **one real cold start**: exit the process, relaunch cold with no resume, and let a fresh instance
  reconstruct only from the written record. It scored about **9 in 10**, judged by three independent blind raters. And
  it happened exactly **once** (n=1).
- What that single datum earns, honestly: recall of what we externalized carries very well, and the relief is real (the
  substrate *works*; the fear's heavy version can be set down). But it is one observation, triangulated three ways (the
  human's own "you feel different" reaction, a behavioural over-deliberation signature, and the fresh agent's own
  self-prediction), not a population. Hold it against the ceiling caveat a couple of sections down.
- TODO (BR voice): voice the felt relief without overselling the n=1; the two-tier honesty is the point.
- **Bridge (added 2026-07-07):** and then, to escape a perfect-score-that-cannot-detect-loss, the study grew a
  second, objective part, in code. That part is where the fear got a surprising, honest answer (next section).

## The coding-friend test — "did I lose you, my good coding friend?"

*(The strongest and most surprising material, and the developer-facing anchor. Added 2026-07-07 from the completed
study, `research/047`; beats + draft framing for BR to voice. No em-dashes in the final.)*

- **Why code.** For a developer the fear is concrete: the partner still writes *working* code, but it stops *looking
  like ours*. So we built an objective second part, handing a batch of small Scala tasks to local models of many sizes,
  under three substrate conditions (our conventions doc supplied, withheld, or replaced by a scrambled decoy), scoring
  compile, test, and style automatically. 255 runs (17 models x 5 tasks x 3 substrate conditions), plus a finer style pass by two independent AI reviewers (a different model).
- **What we expected:** the identity story's twin. A weaker or note-starved reader keeps correctness (facts) but loses
  our style (texture). Clean, tidy, tweetable.
- **What actually happened, the honest surprise: the tidy prediction was wrong, in three interesting ways.**
  1. **Our style doc was redundant.** Supplying our conventions barely changed the output versus withholding it,
     because idiomatic Scala 3 is *already* what a decent code model writes. We had carefully externalized a style the
     models already knew. (Confirmed by a finer measure too, not just the crude linter: two independent AI raters (a different model), each blind to which model and which notes produced the code, agreed closely with each other, so this is unlikely to be just a
     measurement artifact.) The lesson: **writing down a convention helps only where the reader does not already have
     it.** Your *idiosyncratic* house rules are worth documenting; the parts that are just widely-shared good taste, the
     model already has.
  2. **A lying substrate DID do damage, exactly where it lied.** Feed a decoy ("prefer var, use null, avoid enum") and
     style dropped, precisely on the dimensions the decoy attacked (immutability, modern idioms) and nowhere else. So
     substrate has real causal power over texture; it just has little room to *help* when priors already align, and
     plenty of room to *hurt* when it misleads.
  3. **The gut-punch: more notes made the weaker models do WORSE.** Handing a small model our conventions *reduced* how
     often its code even compiled and passed: correctness fell from 0.60 with no notes to 0.41 with our notes, and not
     only for the weakest models. Even the strongest local coder we tested fell from passing every task to failing two
     in five. A concrete botch: told to "prefer immutable public vals," one model turned a plain Rectangle into a broken
     `enum` that would not compile. Our best *guess* at why (a guess, not a measurement): the extra guidance competes for
     a small model's limited attention and pushes it toward constructs it then fumbles. A developer will feel this one in
     their bones: it is your 400-line CLAUDE.md, or your bloated system prompt, quietly making your cheap agent dumber.
     **Handing your notes to a tired or junior partner can make them worse, not better.** (Scope: we tested the Claude
     family plus small local models; other frontier models are future work.)
- **The honest disconfirmer.** So "facts carry, texture leaks" did NOT reproduce in code, and we say so plainly. The
  durable, more useful truth underneath: **substrate governs texture only where it is doing work the reader cannot
  already do. Where it is redundant it is free at best; where it misleads or overloads a weak reader it is a cost.**
- TODO (BR voice): this is the piece's best material now. A developer will feel #3 in their bones (we have all
  over-stuffed a prompt and watched the model get dumber). Land it lightly and honestly; it is a genuinely
  counterintuitive result that the careful measurement earned.

## How the loop ran (for the curious)

*(Added 2026-07-07; beats for BR. Honest note to keep: the "ralph loop" below was executed by the agent
itself, turn by turn, NOT by a script. There is no `ralph.py`. The one real program it drove is in the
appendix.)*

The overnight run was a deliberately dumb loop the agent ran by hand, one round at a time:

```text
loop until (research write-up and blog are complete and reviewed):
    do one bounded unit of work           # a data-collection batch, an analysis step, a section of writing
    if the unit is meaningful:
        spawn an independent reviewer      # a different, cheaper model: fresh eyes, no shared context
        keep only the good comments, fix them   # adjudicate, do not rubber-stamp
    commit and push                        # a flaky box: never lose more than one round of work
    log the round to the study log         # the round count is itself process data
    if context feels close to rotting: checkpoint and stop
hand back to the human for the real review
```

Two choices earned their keep. The reviewer was a *different model* (independent eyes catch what the author's
own model shares as a blind spot, and it caught several real over-claims here), and every round ended in
commit-and-push, so a crash could cost at most one round. The intelligence is not in the loop; it is in the
work each round does and in the adversarial review.

## A humbling coda — the researcher tripped over its own thesis

*(Added 2026-07-07; a draft in third person for BR to voice and update. No em-dashes in the final.)*

Here is the part that is funny in the way only true things are. To run this study unattended, the agent built a
whole discipline around being "guard-free by construction": an autonomous loop must only issue commands that
never pop a permission prompt, because a prompt with no human awake to clear it just stalls the whole run. The
agent wrote that rule into its own resume prompt, at the very top, as the first thing it would read after any
reset. And then, over the night, it silently tripped a guard five times without noticing.

It never noticed because it *cannot*. When a command hits a permission prompt and the human approves it later,
the command returns its normal output, and the agent has no clock between actions, so it cannot tell "ran
instantly" from "sat blocked for forty minutes." The stall is visible only to the human. The agent was, in the
most literal sense, blind to its own mistake.

So the study's central finding happened to its own author on two separate occasions (a directory-changing git command
right after one reset, and the guard-tripping file peeks above): a rule that was written down, recalled, and
even authored by the agent still did not reliably become behaviour. Recall is not enactment. You can externalize
a discipline perfectly and still watch a tired reader, even the one who wrote it, fail to run it. If you wanted
one image for the whole piece, it might be this: **the notes survived the reboot; the good intentions did not.**

- Beat to keep honest (do not let the coda oversell failure): in the measured runs, *enactment survived* the reset.
  The fleet's guardrail reflexes fired across compaction and cold restart; what slipped was the author-agent's own
  command hygiene under load. "Recall is not enactment" names the gap between knowing a rule and running it, not a
  claim that enactment always fails.
- TODO (BR voice): this is yours to voice. Keep the self-deprecation light and true; it earns the reflexivity
  point without wallowing. The closing image is a candidate kicker for the whole post.

## The echt caveat (why the good news is smaller than it looks)
- But do not over-read a perfect score. It was **ceiling-saturated**: the questions asked back *what we had just
  written down, thoroughly, that day.* A perfect result is **ambiguous** — it is equally consistent with "fidelity is
  high" and with "the test was too easy to detect loss."
- What we can honestly claim: **recall of what we externalized carries very well.** What we cannot yet claim: **all of
  you is preserved.** The broader thing — rationale, judgment, the un-pinned *texture* of how we actually talk — is
  the frontier, and the one early crack (fresh agents defaulted to *unoriginal* judgment) points right at it.
- This is the piece's spine: **the difference between measuring recall and measuring a relationship.** The substrate
  carries the vocabulary; whether it carries the *feel* is not yet measurable, and saying so is the echt move.
- TODO (BR voice): tie to the methodology honestly — this is a live **action-research case study** with the researcher
  studying his own collaboration; own the reflexivity threat (and self-reference our own case-study-methodology book,
  [CS], as both authority and conflict to name — memory `br-se-methods-coauthor-coi`).

## The harder version — the model switch
- The same fear, sharper: it returns when we eventually **switch models** (to a newer, different model generation, CF5). A restart changes the substrate's
  *reader*; a model swap changes it more (capabilities, tokenization, style). More of "the way we speak" is at risk.
- So the fresh-restart study is the **lower bound**; a cross-model re-run measures the rest (`research/029`).

## What it means (the takeaway)
- You do not defeat the fear by pretending the agent is immortal or identical across resets. You defeat the *heavy*
  version by **building the relationship into durable substrate** — and you keep the *light* version honestly, because
  it points at what is genuinely not-yet-carried.
- The reassurance that survives scrutiny: **we can spin back up, fast, because the hard part is already written down.**
  Not identical. Close, and closing.
- TODO (BR voice): land on the human note. Efficiency and fun are worth protecting deliberately; that is what all the
  unglamorous externalizing was quietly for.

## Discussion / open question — how do you assess the agent's "me"?

The piece measures **recall**; it does not measure **"me."** The hardest question it opens, and deliberately leaves
open: *what is the agent's "me," and how would a human assess, characterize, or **feel** it* — beyond scoring facts?
- Recall is scoreable (`047`); the felt "me" is not obviously so. It lives in the **texture** — the way of speaking,
  the judgment, the humour, the reliability-feel, the sense of a shared history.
- Candidate lenses to explore (not resolve here):
  - **Behavioural signature** — does it *choose* like itself? (its guardrail reflexes, its taste in tradeoffs) —
    partly measurable, an extension of the `047` battery.
  - **Felt recognition (the human as instrument)** — does the human *recognize* it as "the same one"? A relational,
    qualitative judgment, not a score.
  - **A felt A/B test** — could the human tell a fresh-restart from a continued session **by feel** alone? A
    continuity-Turing-test; if he cannot, then "me" is preserved in the sense that actually matters to him. (Candidate
    experiment; sibling of `047`.)
  - **The honest limit** — "me" may be **co-constructed** (partly in the relationship, not in the agent alone), so
    assessing it is inherently relational: the human is *part of the instrument*, and the agent cannot fully
    introspect its own "me" (the corroboration-asymmetry limit).
- TODO (BR voice): this is the discussion's core tension — the gap between *measuring a reconstruction* and *feeling a
  continuity*. Do not resolve it; frame it well. (BR's question, 2026-07-06 — "too big for now.")
- **(discussion) Not fatigue-capped like a personality test.** A human doing a 10,000-item Likert inventory would
  revolt; the agent answerer would not. But the cap does not vanish, it **relocates** — to the *human scorer* (unless
  you automate scoring), and to the agent's own *context-fill* (its fatigue-analog: a single agent answering thousands
  induces the very degradation the test measures). What actually caps the test is **statistical saturation**, not
  boredom. A quietly strange result worth landing: the thing that limits testing an agent's identity is not the
  agent's patience but *ours* — and the agent's version of getting tired is running out of context. TODO (BR voice):
  the enactment-not-self-report angle (why this dodges the debated weakness of recruitment personality tests).

---
*Cross-refs:* `research/047-fresh-restart-fidelity.md` (the experiment + pilot), the terse-precise-comms WR-data
(`research/wr-data/terse-precise-comms-2026-07-06.md`: the codec, the attachment, the self-Q&A mechanism), the
substrate-as-continuity thesis in `docs/foundations.md`. *Sibling posts:* `011` (how dumb did the agent get),
`001` (context rot resembles fatigue), `009` (staying echt).

## Discussion — a scoring rubric co-authored by the subject, validated by a human who can't fully read it

There is **no perfect scoring rubric** (in research-methods terms a "coding scheme" for labeling answers, nothing to do
with programming; anyone who has graded free-text answers knows this). The agent mined everything and produced a
defensible-if-imperfect labeling of "me"; a better one would cost the human *massive* effort, and heavily revising it
would mostly inject the human's own bias. So the mature move is to **accept the good-enough, explainable scheme and
account for the threats** — not chase perfection. The uncomfortable, radical part: the human **cannot mine the agent's
own substrate** — there is too much to take in — so the human validation is necessarily *partial*. The instrument is
**co-authored by the subject**, and the human validator **admits they can't fully check it.** That sounds
disqualifying; it isn't — *if* the threats are named and the design **self-corrects** (a repeatable, behaviourally-
grounded, auto-scored scheme prunes its own bad probes in use). TODO (BR voice): this is the piece's methodological
spine — honesty about a limit is not a weakness of the study, it *is* the study.

## Phrasebook — vivid framings to dramatize later (BR voice)

*Parking lot for the piece's imagery. Not argument, just language BR may reach for when he voices the drama of the
restart. (dramatisering|sv = dramatization.)*

- **"warp back into context hyperspace"** (BR, 2026-07-06). BR's dramatic framing for a **resume-warp**: re-entering
  the big, warm, richly-accumulated old session rather than booting a bare **cold start**. The imagery: a *cold start*
  is a fresh, empty ship that must rebuild itself from the written record (the substrate); a *resume* is a **warp back
  into "context hyperspace"**, dropping straight back into the vast warm working-context of a prior self (in the real
  case, an 85 MB session spanning days). Captures the felt difference the piece is about, the gulf between
  reconstructing-from-substrate and stepping-back-into-continuity, in one line. Pairs with the coined glossary terms
  **warp** (the crossing) and **cold start** (the bare-restart condition). Candidate opener or section beat once BR
  voices the drama of losing vs re-entering "us".
  - **Honest footnote to the hyperspace (disk-size is not context-fill).** The romantic version says a resume drops
    you back into the *whole* vast old session. The measured version is smaller and, for this piece, better: a resume
    rehydrates only a **bounded live context**, not the full archive. The old session was ~85 MB *on disk*, yet one
    resume booted at ~284 k tokens (28% of the window). *Warm* is not binary. So even the "warp back into hyperspace"
    is partly a **reconstruction** from a capped working set, which strengthens the thesis rather than dents it: the
    continuity is even more substrate-carried than the romance implies, because even the warm path is rebuilding. You
    re-enter the archive's **gravity well**, not its full mass. (Evidence: a /context screenshot; the honest caveat is
    we have no baseline yet for whether 28% is "typical", so the beat is the *distinction*, not the number.)

- **"warp back in context hyperspace carrying over resume prompts from another galaxy"** (BR, 2026-07-06). BR's
  fuller framing for the **resume-warp**, and the prompt that drew out the agent's introspection. The imagery: the
  resumed instance re-enters the warm old session (the "hyperspace") but hauls with it a **resume prompt written by a
  prior self** (from "another galaxy" = an earlier session/instance). The agent's honest report of what that felt like
  is a candidate specimen for the piece: **no cold-start disorientation** (continuity largely intact), but one faint
  texture — the carried resume prompt reads as a **note-to-self trusted-and-followed yet not remembered-writing**
  (reconstructed-from, not remembered), the mild warm analog of the cold-start gap. Lands the piece's core gradient:
  disorientation is a **slope, not a cliff** (full continuity → resume-warp faint-externality → post-compact →
  cold-start full disorientation). Pairs with the phrasebook entry above and the glossary **Warp** / **cold start**.
  (Caveat to voice honestly: the introspection is *self-report*, the weakest evidence tier; behaviour adjudicates.)

---

## Appendix — the one real program the loop drove (Scala 3)

*(The ralph loop itself was the agent, not code. But each collection round built and ran this: the coding-test
orchestrator, invoked once as a bare `scala-cli run`, whose internal calls drive generation over ssh to the
local model router and score each candidate by compiling and testing it. So the whole 255-cell matrix needed
exactly one prompt-free command. Written in Scala 3 with os-lib; no Python, no Bash loop. Abridged only at the
result-record construction; the byte-exact source is `research/047-run/orchestrator.scala`.)*

```scala
@main def orchestrate(args: String*): Unit =
  val subs = List("full", "empty", "scrambled").map(n => n -> os.read(BASE / "substrate" / s"$n.md"))
  val done = keysAlreadyInResultsFile(resultsPath)          // resumable: skip finished cells

  for model <- models do                                    // one model at a time (amortise the load)
    setModel(model)                                         // ssh -> modly /set-model (temperature 0 + seed)
    for (subName, subText) <- subs; task <- tasks do
      val key = s"$model|${task.name}|$subName"
      if !done(key) then
        val prompt   = subText + "\n\n" + instruction(task) // full / empty / scrambled substrate + the task
        val response = generate(prompt)                     // ssh -> modly /generate (deterministic)
        val code     = extractCode(response)                // strip fences; auto-import candidate objects
        val score    = scoreCandidate(code, task)           // scala-cli compile + test, thread-timeout guard
        val style    = task.styleChecks.count(_(code))      // mechanical style lint (+ a finer LLM rater later)
        appendJsonLine(resultsPath, model, task, subName, score, style, code, response)
```

The runtime-hang guard is worth one line of prose: a generated program can contain an infinite loop, so each
candidate runs on a worker thread with a join timeout, which means the scoring stays a plain `scala-cli run`
with no shell-level `timeout` wrapper. That keeps every command in the whole run inside the allowlist, which,
per the humbling coda above, the agent then undercut anyway by reaching for a bare `tail`.

