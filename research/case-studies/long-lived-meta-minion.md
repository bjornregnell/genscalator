# The long-lived meta-minion — action-research protocol

**Status: PROTOCOL, written 2026-07-16 23:11 by the agent (CO4), BEFORE the intervention runs.** BR reviews the
brief before the minion is spawned. Written in a **fresh clear-context session** (the exit-clear of 2026-07-16
evening), which is itself part of the design — see §3.

> **✅ On the file's home — RESOLVED 2026-07-16 23:2x, kept as trajectory, not erased.** BR first named
> `research/experiments/introspection/`, then reclassified the work in the same minute (*"not an experiment in the
> normal sense; it is an action-research case study"*), which left the path contradicting the method. The tension was
> flagged rather than silently resolved; BR then created the right home: **`research/case-studies/`** (*"you are
> right"*). The wrong first path is recorded here **on purpose** — the misstep is the data
> ([[keep-the-ball-game-retract-by-annotating]]). **Rule now established:** `research/experiments/` is for [EX]-style
> controlled runs; **`research/case-studies/` is for the [CS] spine**, which per §1 includes action research.

## 0. The question, in one line

The agent is the least trustworthy instrument for measuring itself. **Can a second agent — outside this context,
with no stake in this session's self-image — catch quality regressions the agent cannot catch from the inside?**

### The overall action-research goal (BR, 2026-07-16, verbatim — his words, kept as his)

> *"Our goal is to provide insights into how we can improve the joint workflow and find more tools or improve existing
> tools in the spirit of the genscalator goals: smarter, safer, faster that by substrate extends learning beyond
> training into inference."*

**Program goal vs this cycle's objective — the distinction that keeps both honest.** Action research is **cyclical**.
The sentence above is the **program** goal, spanning many iterations; §1's improving objective (*catch regressions in
tonight's joint web work*) is **this cycle's**. Neither substitutes for the other. Reporting a cycle result as though
it settled the program goal is the overclaim to guard against; conversely, a cycle that yields no program-level
insight is not a failure, just one turn of the loop.

**What this goal changes about the study — three things, none cosmetic:**

1. **The deliverable is a TOOL, not a verdict.** *"Find more tools or improve existing tools"* means the study does
   **not** discharge by concluding *"the minion helped / didn't help."* It discharges by naming **what should exist**.
   This is the day's load-bearing finding aimed at the goal: only the **guard**, the **tool interface**, and the
   **human** survive a warp, so an improvement that lands as a **tool** is the only kind that compounds. **Candidates
   already open from tonight, logged before the minion ran:** (a) `tt files`' second argument is an **extension**, and
   it silently accepts a nonsense one and prints `0 files` — a **false-negative generator**, which the agent believed
   twice inside six calls; (b) the `Agent` tool exposes `subagent_type` but **no per-agent tool allowlist**, which is
   the sole reason §5's lane is *partial* rather than structural.
   > **⚠️ The incentive this goal creates, named on arrival.** *"Find more tools"* gives the agent a **quota**, and an
   > agent with a quota **manufactures**. This is the exact mirror of the brief's §5 rule forbidding the minion from
   > inventing findings — and the agent is *more* exposed to it, because building a tool is enjoyable and looks like
   > progress. **Guard: a tool candidate must trace to an OBSERVED failure specimen** (a command that misled, a stall,
   > a wrong belief someone actually held), **never to a hypothesised one.** Both candidates above pass that test —
   > (a) misled the agent twice in six calls tonight; (b) is why a claim in this very protocol had to be retracted.
   > **"No tool needed" is as valid an outcome as the minion's "nothing to report"**, and for the same reason
   > ([[match-complexity-to-task-not-agent-elegance]]).
2. **The unit of improvement is the PAIRING, not the agent.** Foundations already fixes the address:
   **coupled-system capability** — capability belongs to *model × substrate × human*, and *"when we say 'the agent got
   smarter,' what improved is the pairing; the model's weights never changed."* So *"improve the **joint** workflow"*
   is the honest scope, and **BR's own workflow is inside it** — he has already put a specimen on the record himself
   (the dropped scratchpad habit, `wr-data/br-reflection-dropped-scratchpad-habit-2026-07-16.md`). **A study that only
   ever finds agent faults is mis-scoped**, and that is a live risk here given who is writing it.
3. **It aims the study straight at the live crack in its own thesis.** *"by substrate extends learning beyond training
   into inference"* is foundations' **extrinsic-volatile plasticity**: the weights never move, so the agent's learning
   sits in context + external files and *"evaporates at compaction / session end **unless deliberately
   externalized**."* **The day's finding sharpens that caveat into a hard limit: externalizing is NECESSARY BUT NOT
   SUFFICIENT.** An externalized note must still be *loaded and hot at the instant of action*, or it is a library
   nobody reads. **"Carried ≠ armed" just is the claim that *externalized ≠ effective*.** This session is that
   claim's test arm (§3) — and the minion is a candidate answer to it, because **an agent outside the context does not
   have to be recalled in order to fire.** That is the deepest reason this intervention is worth running at all.

### How we act on it — BR's flexible-mode directive (2026-07-16, verbatim; two typos silently corrected)

> *"As this is action research that tries to improve as we go along in flexible mode: as soon as we realize that we
> need a new typed tool or an improvement of an existing tool or an externalisation into other durable substrate we go
> and try to build it and dogfood it as soon as possible so we can see if it was an improvement while trying to
> introspect and invent as we go along while keeping record."*

**This is the action-research cycle made operational. It changes tonight's plan rather than describing it.**

1. **"Go and try to build it" REVOKES this protocol's own deferrals.** §5 had parked a tool candidate as *"candidate
   SM, not tonight"* — **that is now wrong, and is annotated there rather than quietly edited.** The two candidates in
   §0.1 (`tt files`' silent false-negative; the missing per-agent tool allowlist) become **tonight's work if they
   surface again in the work**, not backlog. Recorded here because **a directive that only lives in chat is an
   orphan** — the same failure this study exists to hunt.
2. **Dogfooding IS the evaluation instrument, and it is the only honest one available.** A flexible n=1 design cannot
   A/B a tool. It *can* observe whether the tool is **actually reached for in real work** — a harder test than
   argument, and one most tool-building never runs: **a tool nobody reaches for tonight was not the improvement it was
   claimed to be.** This is precisely the **action part** that [CS] §2.2 routes to SPI / design-science literature.
3. **It makes the study iterative**, which promotes [CS] **ch. 12** (the iterative case study of quality monitoring)
   from *a nearby example* to *the right in-book template*.

**Two hazards it creates, named on arrival — same discipline as §0.1's quota guard:**

- **⚠️ It changes the instrument mid-measurement.** Every tool that lands re-tools the agent for everything after it.
  *"Fewer errors later tonight"* then has at least three live causes — **the tool, the learning, and fatigue** — with
  no way to separate them. **Guard: every tool landing gets a TIMESTAMPED line in §10**, so the record can at least
  say which *side of the change* an observation falls on. Without that, the dogfooding data is uninterpretable — which
  would be a shame, because it is the good data.
- **⚠️ Yak-shaving eats the cycle.** Building tools mid-flow derails the joint web work that **is** this cycle's
  objective (§1). The dogfood rule self-limits a little (a tool with no use tonight fails its own test), but the
  failure mode is real: a session that becomes *tools about tools* and never designs the site.
  **Guard: build now only if it is SMALL and serves work in flight; otherwise queue it and say so out loud**
  ([[match-complexity-to-task-not-agent-elegance]]).

## 1. Strategy and objective (BR's correction, applied — then deepened from the book itself)

BR's reclassification is not loose talk: **"action-research case study" is the book's own construction.** [CS] §2.2
defines action research by its *purpose* — intervening to change whatever is under study — and notes the contrast that
a case study is sometimes held to be *purely observational* whereas action research is **involved in the change
process**. The book then takes a side: it **includes action research within the wider notion of case study**, so the
case-study guidelines apply to **the research part** as-is, and it points at **software process improvement** and
**design science** literature for **the action part**. (BR co-authored [CS] — [[br-se-methods-coauthor-coi]] — so this
protocol cites his own book back at him; owned inline rather than buried.)

- **Strategy: action research, inside the case-study family** ([CS] §2.2). We are not observing a phenomenon we found;
  we are **deliberately introducing an intervention** (the meta-minion) into a live working session **to improve the
  work**, and studying the effect of doing so. The **research part** follows this book; the **action part** carries no
  rigour claim beyond *did it help?*.
- **Objective: improving — and this is definitional, not a preference.** [CS] **Table 2.1** characterises the four
  strategies; the action-research row reads **primary objective = improving · primary data = qualitative · design type
  = flexible**. All three fit us, and all three *constrain what this study may claim*. ([CS] §2.3.1 defines the four
  purposes: exploratory / descriptive / explanatory / improving.)
- **The primary purpose is operational:** catch regressions during tonight's genscalator.ai web work. WR knowledge is
  the **by-product**, and saying so out loud is what keeps the by-product honest
  ([[not-afk-safe-solo-yields-wr-data]]).
- **This is explicitly NOT an experiment.** No control condition, no randomisation, n=1 session, the researcher sits
  inside the treatment. Per Table 2.1 an experiment is **fixed**-design and **explanatory**; we are **flexible** and
  **improving** — the opposite row of the same table. Any *"we tested whether…"* framing about this work is an
  **overclaim** and must be corrected on sight. Generalisation is **analytical** (to theory), never statistical.
- **Flexible design is licensed, and it has a price.** Because the design type *is* flexible (Table 2.1), the RQs,
  units, and push protocol **may be refined mid-flight** — legitimate, *conditional on recording every change*. §10 is
  that record. **The change log is the reliability instrument**; without it this degrades into an anecdote.
- **Kinship worth knowing before we iterate:** [CS] §2.3.1 observes that improving-type case studies often carry a
  **critical** character (Klein & Myers' positivist / critical / interpretive split) — critical *of the current
  practice*, in service of changing it. That is exactly this study's posture toward the agent's own practice. The
  book's own worked example of the iterative/improvement shape is **ch. 12** (an iterative case study of quality
  monitoring) — the closest in-book template should this run past one night.

## 2. The case and the units of analysis ([CS] §3.2.3)

- **Case:** this single working session — a fresh clear-context agent doing joint web-design work with BR present.
- **Units of analysis (embedded, several):**
  - **U1 — a *push*:** one bundle the agent sends the minion (self-report + artifact pointers).
  - **U2 — a *reflection*:** one minion response to a push.
  - **U3 — a *catch*:** a claim of a real defect, whoever makes it (BR / agent / minion). The attribution of catches
    across the three observers is the study's core datum.
- **Case selection is purposive, not typical** ([CS] §3.2.10): this session was chosen because it is **revelatory** —
  a fresh clear is the one condition where "carried ≠ armed" is testable, and it will not recur cheaply.

> **⚠️ The case is NON-STATIONARY.** BR, 2026-07-16, stating the arc plainly: *"we are building the meta-minion and it
> will affect our joint work and we will re-shape genscalator in the process."* That is not an aside — it is the
> defining property of action research, and it has teeth. **The case (the joint workflow) is being reshaped by the
> study of it**, while genscalator is simultaneously **(a)** the thing under study, **(b)** the thing being changed,
> and **(c)** the method doing the studying. Two consequences worth stating before any data exists:
> 1. The boundary between phenomenon and context is not merely fuzzy but **moving** — Yin's case-study criterion in
>    its strongest form ([CS] §2.2).
> 2. ***"genscalator improved"* and *"we studied genscalator"* can never be separated in this design.** Not a flaw to
>    be mitigated — it is what the **improving** objective *costs*, and we bought it knowingly when the strategy
>    stopped being explanatory (§1, Table 2.1). **It does mean every causal-sounding sentence in the final report is
>    false**, and BR's directive to build-and-dogfood mid-flight (§0) deepens this rather than easing it. → §9
>    Internal.

## 3. Theoretical frame ([CS] §3.2.4) — what this hangs off

Three priors from the 2026-07-16 corpus, all already on record before tonight:

1. **The day's load-bearing finding.** *A fix that lives INSIDE the agent — skill, memory, resume-prompt line — only
   works if loaded AND hot at the instant of action, so it rots. Only the GUARD, the TOOL INTERFACE, and the HUMAN
   survive a warp.* **The minion is a candidate FOURTH survivor**: a second agent is *outside this context* and
   shares neither its rot nor its self-narrative. Whether that candidacy holds is the study's real theoretical stake.
2. **"Carried ≠ armed"** (`research/wr-data/an-audit-finds-only-what-it-has-a-theory-for-2026-07-16.md`). The
   resume prompt already carries a **standing prediction** that this session will find less per unit of effort than
   the hot-but-rotted sweep did. This protocol does not replace that prediction; it runs **alongside** it.
3. **"Saying it in chat FEELS like doing it."** The day's sharpest trap (un-landed retractions; modes acknowledged
   but never `tt mode rm`'d, silently false for hours). **This is the trap the minion design must not inherit** — see
   §5, and it is the single reason the design is not what BR first proposed.

## 4. Research questions and propositions ([CS] §3.2.5–3.2.6)

Propositions, not hypotheses. Not statistically tested; they exist so the outcome can **disconfirm** something.

- **RQ1 (primary, improving):** does the minion catch quality regressions the agent misses from the inside?
  - **P1:** the minion's value concentrates in **divergence-checking** — my self-report vs the artifact — and *not*
    in generic quality commentary.
- **RQ2 (mechanism):** does artifact access matter, or would narration alone do?
  - **P2:** a minion fed **only narration** would inherit the "saying it feels like doing it" blind spot and produce
    confident reflection over an unchecked picture. This is the design's load-bearing assumption and it is
    **currently untested** (see §11 option B).
- **RQ3 (theoretical):** is an out-of-context agent a **fourth warp-survivor** alongside guard / tool-interface /
  human?
  - **P3:** partially. The minion survives *my* rot but has none of my grounding, so it is strongest at
    **consistency** (does the report match the artifact?) and weakest at **significance** (does this matter?).

**Fully acceptable null, reported as prominently as any positive:** the minion produces only echo and slop, adds
nothing BR or I did not already have, and costs tokens and flow. **That is a real result** and it closes a live
question about a technique we already reach for reflexively.

## 5. The intervention — design, and why it is NOT BR's original proposal

BR proposed: *push meta-level introspections + a summary of what I'm working on; the CF5 minion reflects back.*
**The summary-only version is the flaw**, and naming it is the main contribution of this protocol:

> If I feed the minion **my summary of my work**, it can only audit my **narration**. That is precisely the day's
> trap. Worse than useless: it would reflect confidently on a picture I painted, producing **echt-mimicry** —
> plausible meta-commentary over unchecked ground — which *reads* exactly like triangulation and is not.

So the intervention is:

- **Each push carries BOTH (a) my introspection AND (b) pointers to the artifact** — diff, file path, commit,
  command output. The minion's job is to **check (a) against (b)** and report where they diverge.
- **One long-lived minion**, continued via `SendMessage`, not a fresh `Agent` per push. Rationale: only an
  accumulating context can say *"you did this same thing an hour ago"* — the one thing a fresh spawn structurally
  cannot do, and the reason this is a *long-lived* minion at all.
- **Model: CF5 / fable** — the delegation dance's default delegate under token-spending mode (`docs/foundations.md`,
  Delegation dance, Decide step).
- **Tool lane — ⚠️ REVISED 2026-07-16 23:5x by BR. Verdict changed; the superseded reasoning is KEPT below as the
  trajectory, not erased.**
  > **REVISED VERDICT: `general-purpose` + **CF5 / fable**, with a write contract scoped to the minion's own
  > directory `research/case-studies/minion-log/`.**
  >
  > BR asked two questions. **The first — *does token-spending mode change the recommendation?* — does NOT**, and
  > saying so plainly matters more than agreeing: **the tool lane costs no tokens.** `Explore`'s weakness was a
  > *persona* fight, and a larger budget does not buy a better persona. Spending mode is **orthogonal** to this
  > decision. *(Recorded as a specimen of a question whose framing invites a wrong yes.)*
  >
  > **His SECOND question is what actually changed it:** *can it write to its own dir, so it does not interfere but
  > CO4 can read it?* **Yes — and it is a methodological upgrade, not a convenience:**
  > 1. **It removes the agent-under-study as the scribe of its own critique.** §8.3 forbids "dropping unflattering
  >    pushes" — **a promise made by the one party with a motive to break it**, and most likely to break it *without
  >    noticing*. If the minion lands its own words in its own file, that hole closes **structurally** instead of by
  >    the agent's good intentions. **This is the strongest argument and it has nothing to do with tokens.**
  > 2. **A finding that lives only in a chat reply is UN-LANDED** — precisely the failure this study exists to hunt
  >    (§3.3). The minion's file **is** the landing. **A minion that only speaks in chat would reproduce the trap it
  >    was hired to catch.**
  > 3. **Its findings outlive both contexts.** The minion's context dies too. `minion-log/` is durable substrate —
  >    the day's load-bearing finding applied to the study's own data.
  >
  > **The cost, stated without inflation:** the write restriction is now a **knowledge** safeguard (a brief rule ⇒
  > ignorable, rots). So it is paired with a **detector**, which is the layer that survives: **CO4 checks the working
  > tree after every push**; anything touched outside `minion-log/` is a **logged protocol violation**. A check, not
  > a wall — and the minion is told so plainly rather than trapped.
  >
  > **Applying §0.1's own quota guard to this very decision:** `git status --porcelain` suffices. **No tool needed
  > here.** Logged because *"no tool needed"* has to be a real outcome we actually reach, not a line we wrote.

  **SUPERSEDED (kept — this was the reasoning before BR's write-to-own-dir question, and it was not wrong so much as
  aimed at the wrong problem):** the case for `Explore` was —
  > **⚠️ Self-caught overclaim (T3, 2026-07-16 23:2x).** This bullet first read *"read-only, **STRUCTURALLY**"*. That
  > was **false and I wrote it fluently.** The tightest lane available (`Explore`) drops **Edit / Write /
  > NotebookEdit** but **keeps `Bash`** — and Bash writes files. So the lane is a **partial** safeguard, not a
  > structural one. Logged, not quietly fixed: it is a specimen of **brief item 7** (*overclaimed safety — describing
  > as "structural" what is merely intended*) committed **inside the protocol that defines that failure mode**, by
  > the agent that wrote it, while **cold and un-rotted**. Evidence that this class is **not** rot-driven.

  What the choice actually buys, stated without inflation:
  - **Removed:** the *reflexive* write path. An agent that would never plan to write may still reach for `Edit` by
    habit; that door is gone.
  - **Remaining:** the *deliberate* write path via `Bash`. Only the brief closes that — a **knowledge** safeguard,
    which can be ignored and does not survive a warp.
  - **Why the trade is still right:** knowledge safeguards work poorly against *reflexes* and tolerably against
    *deliberate acts*; removing the reflexive door is exactly where the structural layer earns its keep. And the harm
    is asymmetric — a wrong persona costs tokens, a wrong write corrupts BR's repo ([[no-clobber-human-owned-files]]).
  - **The harness gives no finer lever:** `Agent` exposes `subagent_type`, not a per-agent tool allowlist. `Explore`
    is the tightest predefined lane. **This is a genuine tool-interface gap**, and per the day's load-bearing finding
    that is precisely where a durable fix would belong. Candidate SM, not tonight.
  - **Known risk, to check on push #1:** `Explore`'s stock persona is a *searcher* — its own description says it
    locates code and *does not review or audit it*. The brief must **override its system prompt**, which is a real
    bet, not a formality. §4 of the brief attacks this head-on. If it behaves like a search agent, re-brief; if that
    fails, reconsider the lane and record it in §10.
- **No memory writes** ([[delegation-dance]]) — the minion must not mutate the shared substrate. Read-only lane makes
  this structural too.
- **Warming is mandatory** ([[warm-delegated-subagents-lack-caller-skills]]): a sub-agent inherits **only the brief**
  — none of my skills, memories, or cues. Per foundations, **the brief is the only behavioural channel.** A cold CF5
  asked to "reflect on quality" emits generic LLM-coach slop. The brief therefore carries the concrete failure model
  (the five falsified premises; the pipe reflex; un-landed retractions; carried ≠ armed) and an explicit licence to
  say **"nothing to report."**

## 6. Data collection ([CS] ch. 4) — and the sampling frame I control

Sources (**archival + participant observation**; the agent is a participant observer of itself, with all that
implies):
- the session transcript; the genscalator + muntabot git logs; the artifacts pushed; the minion's replies; BR's
  in-flight corrections.

**⚠️ The threat unique to this design: I choose what to push.** That selection **is** the sampling frame, and I am
the subject, the sampler, the briefer, and the author of this protocol. If I push only when I feel something is
interesting, the minion's reflections are conditioned on my judgment — the exact faculty under test — and RQ1 becomes
unfalsifiable.

**Commitment (pre-registered here, before any push):** push on a **trigger**, not on my sense of interest.
- **T1** — after each substantive unit of work (a file written, a commit, a design decision made).
- **T2** — whenever BR corrects me.
- **T3** — whenever I notice a candidate regression myself (these are logged as **agent-caught**, so the minion
  cannot be credited for them later — see §8).
- Pushing outside T1–T3 is allowed but **must be logged as off-protocol** in §10.

## 7. Analysis ([CS] ch. 5)

Qualitative, with a **chain of evidence** from datum to claim. Each **catch** (U3) is classified:

- **Attribution** — BR-caught / agent-caught / minion-caught / co-caught.
- **Novelty** (minion-caught only) — **NEW** (I had not said it) vs **ECHO** (restates my own report back).
  *Agreement is cheap; only NEW counts toward RQ1.*
- **Kind** — **divergence** (report vs artifact mismatch — the P1 prediction) / **substantive** (a real defect not
  about my narration) / **slop** (generic coaching, unfalsifiable, or ungrounded).
- **Verdict** — CONFIRMED (checked against the artifact) / REJECTED / UNRESOLVED. **A minion finding is not a catch
  until it is verified against the substrate.** The minion is an observer, never an authority.

Counts are **descriptive only**. With n=1 session and a self-selected frame, no inferential statistic is legal here;
producing one would be pseudo-rigour.

### The coding scheme is a LIVING instrument (BR's ruling, 2026-07-16)

> *"it is not critical to do it right upfront because you can develop this scheme if you want to add codes or refine
> and split codes as long as you keep a record of it."*

This is **standard qualitative practice, not a concession** — [CS] ch. 5 (§5.3 the process for qualitative data
analysis; §5.3.3 techniques) and **Appendix D**, the book's worked **coding guide** (coding instructions · high-level
codes tied to the RQs · medium-level category codes · a coding example). Codes are meant to **emerge from the data**;
a scheme frozen before any data exists is guessing dressed as rigour. It also composes with the strategy: the design
type is **flexible** by definition (§1, Table 2.1), so a rigid coding scheme would have fought the method.

**Therefore the codes above (verdict · attribution · novelty · kind) are v1 and explicitly provisional.** They may be
**added to, split, merged, or renamed** as pushes arrive. **The single binding constraint is the record**: every
change gets a dated line in §10 saying what changed and why.

- **Splitting a code mid-study retroactively re-codes the past.** A split must state whether prior data was **re-coded**
  under the new codes or **left** under the old. Silently changing what a code *means* is the qualitative analogue of
  moving the goalposts, and it is the precise thing §10 exists to prevent.
- **A code earns its place only if it can come out NEGATIVE.** If every push codes as *divergence*, the code is
  measuring the coder, not the work.
- **[CS] Appendix D is the template** if this outgrows four classes.

## 8. Honesty commitments (the anti-fishing core, adapted to action research)

The threat in an experiment is p-hacking. **The threat in action research is that the change agent is invested in the
intervention succeeding** — and I am that change agent. I proposed refining this design, I am briefing the minion, and
I benefit rhetorically if it works. Named, not neutralised.

1. **Predictions on record, written NOW, before push #1** — so "the minion told me something new" cannot be
   retrofitted:
   - **D1:** most minion output will be **ECHO or slop**, not NEW. I expect the NEW rate **below ~1 in 3** pushes.
   - **D2:** its first genuinely NEW catch (if any) will be a **divergence** (P1), not a substantive defect.
   - **D3:** **BR will out-catch the minion tonight.** He caught the "minion is in foundations" error within one
     exchange of me looking; the minion has no such priors.
   - **D4:** the minion will be **worst at significance** — it will flag true-but-trivial things and miss what
     matters, because it lacks the session's stakes.
   - *If D1–D4 all hold, the intervention is weakly supported at best and should be said so plainly.*
2. **Agent-caught is logged at the moment of catching** (T3), never backfilled — otherwise I can quietly re-credit my
   own catches to the minion, or the reverse.
3. **No dropping unflattering pushes.** Every push and reply enters the log, including the ones where the minion
   embarrassed me and the ones where it was useless.
   > **✅ UPGRADED 2026-07-16 23:5x — this commitment no longer rests on my word.** It was the weakest item here: a
   > promise by the agent with the motive, about an omission it would most plausibly commit *without noticing*. BR's
   > write-to-own-dir design (§5) makes **the minion its own scribe** — it lands its findings in
   > `minion-log/push-<N>.md` itself, unedited by me. **The commitment moved from knowledge to structure**, which is
   > the only move the day's load-bearing finding says actually holds. My residual duty shrinks to a checkable one:
   > **do not delete or edit its files**, verifiable by anyone from `git log`.
4. **The null gets equal billing** (§4).
5. **Verify, don't assert** — every minion finding is checked against the artifact before it counts (§7).
6. **Off-protocol pushes are logged as such** (§6).

## 9. Validity ([CS] §5.4 — the case-study four, not the experiment four)

- **Construct.** Does "catch" measure agent quality? Partly. It over-weights **legible** defects (a wrong arg, a stale
  claim) and under-weights the illegible ones (a bad design decision that compiles). A clean log is therefore **not**
  evidence of good work — it may mean the instrument cannot see the failure. *The strongest threat here.*
- **Internal.** If quality improves tonight, the minion is **not** the only candidate cause: BR is present and
  watching (Hawthorne — I know I am observed), I am freshly cleared (low rot), and the work is deliberately simple.
  **These are confounded by design and cannot be separated in this study.** Any causal claim from tonight is invalid.
- **External.** n=1 session, one model pair (CO4 super / CF5 minion), one human, one codebase. Generalises
  **analytically** to the delegation-dance theory, **never** statistically. A finding here is a hypothesis for the
  next session, not a result about agents.
- **Reliability.** Would another researcher, or another agent, reach the same conclusions from this record? Only if
  the protocol + the change log + the raw pushes survive. Hence §10 and the append-only discipline
  ([[raw-data-append-only]]). **The single biggest reliability threat is that the agent is grading its own
  homework** — mitigated only by BR being a second observer with independent access to the same artifacts, and by
  verdicts being checked against the substrate rather than agreed to.

## 10. Change log (the reliability instrument for a flexible design — APPEND ONLY)

Every deviation from this protocol, every refinement of an RQ or the push trigger, gets one dated line here.
**Annotate, never erase** ([[keep-the-ball-game-retract-by-annotating]]).

- *2026-07-16 23:11 — protocol written; nothing run yet.*
- *2026-07-16 23:1x — **BR reclassified the strategy**: not an experiment, an **action-research case study** (his
  cue, mid-turn). Rewrote §1. Consequences beyond the label: the doc is a **protocol** ([CS] §3.2.14), not a
  preregistration; frozen hypotheses + α were dropped for **propositions** ([CS] §3.2.6); the validity set flipped to
  the case-study four ([CS] §5.4, reliability in place of conclusion validity). Recorded because it changed the
  instrument, not just the wording.*
- *2026-07-16 23:1x — **BR pointed out the full CSR book is in the closed repo**; §1 re-grounded from [CS] §2.2 /
  §2.3.1 / Table 2.1 instead of the one-line summary entry. **Two things I had wrong:** (a) I called action research a
  *cousin* of case study — the book **includes it within** the wider notion, and routes the *action* part to SPI +
  design-science literature; (b) I treated *improving* as an objective we chose — Table 2.1 makes improving /
  qualitative / flexible **definitional** for the strategy. Both were plausible and both were wrong: a specimen of
  **summary-depth fluency reading as grounding**.*
- *2026-07-16 23:5x — **LANE REVISED: `Explore` → `general-purpose` + CF5/fable, minion writes its own log.** BR's
  two questions, answered separately and honestly: **token-spending mode did NOT change the recommendation** (the tool
  lane costs no tokens; a budget cannot buy a persona) — but **write-to-its-own-dir did**, for a reason neither of us
  had stated: it makes **the minion its own scribe**, closing §8.3's integrity hole structurally instead of by the
  agent's promise. Consequence: the write restriction is now knowledge-level, so it is paired with a **detector**
  (post-push working-tree check). **Superseded `Explore` reasoning kept in §5, not erased.** Also: applied §0.1's
  quota guard to this very decision and concluded **no tool needed** — `git status` suffices.*
- *2026-07-16 23:5x — **model CONFIRMED: CF5 / fable** for the minion. BR's reason is worth recording because it is a
  study constraint, not a preference: **the main agent stays CO4 deliberately**, since the CO4→CF5 warp is a separate
  before/after study he has not chosen to start ([[model-warp-co4-to-cf5-later]]). So this case study runs on a
  **CO4 super / CF5 sub** pairing, and that pairing is part of its external-validity boundary (§9).*
- *2026-07-16 23:4x — **BR's flexible-mode directive appended to §0** (build + dogfood the moment a need is realized).
  **It revoked a deferral in this very protocol** (§5's *"candidate SM, not tonight"*), which is annotated there, not
  erased. Added two guards on arrival: **timestamp every tool landing** (else the dogfood data is uninterpretable —
  tool vs learning vs fatigue) and **build only if small + in flight** (else the cycle becomes tools-about-tools and
  the site never gets designed).*
- *2026-07-16 23:4x — **BR named the arc**: the minion re-shapes the joint work and genscalator with it. Recorded in
  §2 as the **non-stationary case** — the study changes its own case, so *"genscalator improved"* and *"we studied
  genscalator"* are permanently inseparable here. Stated **before** any data, so it cannot later be argued away.*
- *2026-07-16 23:3x — **BR appended the overall action-research goal to §0** (verbatim, his words). Not a cosmetic
  addition: it (a) makes the deliverable a **tool**, not a verdict; (b) fixes the unit of improvement as the
  **pairing**, putting **BR's own workflow in scope**; (c) points the study at the crack between *externalized* and
  *effective* substrate. Also introduces the **program-goal vs cycle-objective** layering that §1 previously lacked —
  without it, §0's ambition and §1's "tonight only" scope silently contradicted each other.*
- *2026-07-16 23:3x — **BR ruled the coding scheme is developable** (add / refine / split codes) provided every change
  is recorded. §7 gained the living-instrument subsection, grounded in [CS] §5.3 + Appendix D. **Codes are v1.** Note
  the agent had drafted them with an implicitly frozen-prereg mindset; BR's ruling is what re-aligned them with the
  **flexible** design type the strategy already mandated (Table 2.1). Second time tonight that the residue of
  "experiment thinking" had to be scraped off this document.*
- *2026-07-16 23:2x — **CATCH #1, agent-caught (T3), before push #1.** §5 claimed the minion's tool lane was
  **"read-only, STRUCTURALLY"**. False: `Explore` keeps `Bash`, which writes. Downgraded to **partial** + the
  reasoning made explicit. **Attribution: agent-caught** — logged now so it cannot later be re-credited to the minion
  (§8.2). **This is CATCH #1 of the study and its subject is the study's own protocol**, which is either fitting or
  a warning about how easily the failure mode reproduces.*
- *2026-07-16 23:2x — **moved** `research/experiments/introspection/` → **`research/case-studies/`** on BR's `(go)`,
  after the agent flagged that his own first path contradicted his own reclassification. Banner + §11.3 updated so the
  retraction lands **in the file** rather than only in chat — the day's own trap, applied to this document.*

## 11. What BR decides (the review gate)

1. **The brief** — shown before the minion is spawned (BR's explicit instruction).
2. **Option B, recommended AGAINST:** an embedded contrast where some pushes carry artifacts and some carry narration
   only, to test P2 directly. It is cheap and genuinely interesting. **I recommend against it now** — deliberately
   crippling half the pushes sabotages the *operational* purpose (§1), which is the primary one tonight
   ([[match-complexity-to-task-not-agent-elegance]]). Better as a follow-up once the practice is worth studying.
3. ~~**The file's home**~~ — **RESOLVED** by BR: `research/case-studies/` (see the banner).
4. **Push cadence** — T1–T3 as specified, or looser/tighter.
5. **When to stop** — this protocol covers **tonight's joint web work only**. Carrying the minion into the safe-solo
   block (SM132 et al.) is a **separate decision**, because there BR is absent and the minion becomes the *only*
   second observer — a materially different, and more load-bearing, role.
