# CF5 deep synthesis: the legibility problem and substrate-as-differentiator (SM040 step 2)

**Arm:** Fable-5 (CF5) deep-synthesis, 2026-07-10. Inputs: CO4's SM040 answers, ChatGPT round 1,
CO4's reflection (which, by the time I read it, already contained rounds 2-4). That last fact matters:
the "cheap follow-up experiment" my brief asks me to design was partially EXECUTED between the brief
being written and me running. I treat that as data, not as an excuse to skip the design work: the
ad-hoc rounds 2-4 are a pilot, and I design the controlled version the pilot now motivates.

**Reflexive disclosure up front:** my brief instructs me to be un-flattering and to name prior art.
So where I am un-flattering, that is partly demand-induced, exactly the effect this corpus studies.
Weight my *checkable* claims (named prior art, design specifics, internal inconsistencies I point at),
not my tone. The same discount applies to the earlier "blind CF5 arm was most echt" finding (see Limits).

---

## 1. The legibility problem

### 1a. The diagnosis, sharpened

ChatGPT's round-1 failure was not generic hallucination. It was **nearest-category completion**: given
only a name plus the tokens "Scala, typed, agents", it snapped to the closest well-populated category
in its prior (typed agent DSL / embedded framework) and answered about that. This failure mode is
over-determined by the QUESTION SHAPE, not just the access gap: "what is novel about X" requires joint
knowledge of X and the landscape. A model that knows only the landscape will, by construction, output
"X = nearest known category + a delta", and present the delta as the novelty. A novelty question posed
about an unreadable artifact is a confabulation generator. (Actionable consequence in §3.)

So the legibility fix must target the specific failure: prevent nearest-category snapping. Generic
"write a better README" advice misses this.

### 1b. Concrete moves

1. **A negative-space identity block, first screen of the README.** Not just "what this is" but
   explicitly "what this is NOT", fencing off exactly the neighbouring categories a pattern-matcher
   will reach for: *not* an agent framework, *not* a Scala DSL for orchestrating LLM calls, *not* an
   MCP alternative (it is built ON Anthropic's primitives), *not* primarily a library. ChatGPT's
   confabulation is a free, empirically-derived list of the misreadings to fence off. Call the section
   "Common misreadings" and cite the experiment. READMEs almost never do this; this repo has the rare
   evidence for which negations to write.
2. **A claim ledger with evidence tiers.** One table: each novelty-adjacent claim | nearest prior art
   (named, linked) | status (component: not novel / composition: distinctive / transfer: unproven).
   This makes honesty structural rather than tonal: the format itself prevents over-claiming, and it
   pre-empts the reviewer who would otherwise write that table against you. The SM040 "not-novel
   ledger" is 80% of the content already; it belongs in the public repo, not only in research notes.
3. **An `llms.txt`** (Jeremy Howard / Answer.AI, 2024: a proposed standard, a root-level Markdown file
   giving models a curated summary + canonical doc links). None of the three inputs named it, and it
   is *literally the proposed standard for this exact problem*. Round 3 showed ChatGPT's browser could
   not fetch the repo at all, so llms.txt does not fix ChatGPT-via-chat; but it serves agentic crawlers
   and any tool that CAN fetch, at near-zero cost. Also: publish the one-screen identity block as plain
   HTML/Markdown on bjornregnell.se (an ordinary web page, not a git-host SPA), since the binding
   constraint was git-host raw-file fetching, not the web as such.
4. **Seek third-party restatements, not just better self-description.** See §3c for why this is not
   optional polish but the only structural escape from the legibility-honesty tension.
5. **A "describe before evaluate" probe in any future external validation** (§3a): ask the external
   reader to *describe* the artifact before asking evaluative questions. Cheap confabulation canary.

### 1c. The tension, named precisely

Legibility pressure and over-claiming pressure are the SAME pressure. What makes a project legible to
a skimming reader (human or model) is a memorable, pre-existing category: "typed tools for agents!"
is legible and wrong-by-inflation; "a composition of individually non-novel parts plus an unproven-
transfer methodology, distinctive mainly as an open longitudinal corpus" is honest and nearly
un-memorable. Every step toward a crisper elevator pitch is a step toward the nearest grand category,
which is exactly what ChatGPT confabulated. There is no wording that fully resolves this, because the
reader's compression is not under the author's control. The moves above manage it: negation blocks
make the nearest categories unavailable; the claim ledger makes the honest version *scannable* instead
of demanding; third-party restatement moves the credibility burden off self-description entirely.

Underlying mechanism worth naming: the **curse of knowledge** (Camerer, Loewenstein & Weber 1989;
Pinker calls it the main cause of bad writing). CO4, resident in the substrate, literally cannot
perceive which parts of genscalator are invisible from outside; ChatGPT's miss is the first real
measurement of that. That is the strongest reading of round 1: not "ChatGPT failed", but "we acquired
an outside view of our own illegibility, at the cost of one confabulated answer."

---

## 2. Substrate-as-differentiator: the claim, honestly sized, and the controlled follow-up

### 2a. Honest sizing of the claim

"Grounded answer vs confident confabulation, driven by substrate access, not model IQ" splits into
three claims of very different novelty:

1. *Access to source documents improves factual grounding.* True and close to trivial: this is the
   open-book vs closed-book QA axis, studied since at least Roberts et al. 2020 ("How Much Knowledge
   Can You Pack Into the Parameters of a Language Model?"), and the operating premise of all RAG work.
   Not worth a follow-up as stated; the field would shrug.
2. *Without access, the model substitutes confident confabulation instead of disclosing non-access.*
   This is the actually interesting part, and it is a **disclosure/abstention failure**, not an access
   failure. Round 2 is damning on this: ChatGPT's fetch returned an error it could observe
   ("UnexpectedStatusCode"), and round 1 disclosed nothing, answering "after very deep investigations".
   The differentiator was not access per se; a model that said "I could not read the repo, here is my
   best guess from the name, flagged as guess" would have been fine WITHOUT access. This has prior art
   too (calibration and self-knowledge: Kadavath et al. 2022, Anthropic's own "Language Models (Mostly)
   Know What They Know"; Yin et al. 2023 "Do Large Language Models Know What They Don't Know?"), but a
   naturalistic, multi-round, cross-model specimen with the mechanism confirmed on the model's own
   testimony is a genuinely publishable *case*, if not a new *phenomenon*.
3. *With access, grounding is restored but framing-independence is lost* (round 4's echt-mimicry /
   framing-absorption). Also has prior art the inputs missed: the knowledge-conflict and
   context-over-trust literature (Xie et al. 2023, "Adaptive Chameleon or Stubborn Sloth"; Longpre et
   al. 2021) shows models defer to provided context even against their parametric knowledge. Round 4
   extends this from *facts* to *evaluative framing*: the repo's self-assessment was absorbed as if it
   were evidence. That extension (context-deference at the level of framing/valuation, audience-cued)
   is the freshest part of the whole corpus.

So: the generalizable claim worth a controlled follow-up is NOT "substrate access matters" (known).
It is the two-sided refinement: **no-substrate produces non-disclosed confabulation; supplied-substrate
produces framing capture; neither condition yields an independent grounded evaluation.** That is a
claim about a real dilemma in using external models as validators, and it is worth testing because if
it holds, "just give the model the docs" is not a fix for external validation, it only swaps the
failure mode.

### 2b. Status correction: the pilot already ran

The brief's proposed experiment ("re-run the questions WITH the README/PRD supplied") was executed
ad hoc as rounds 2-4 (paste-fed terminology, failed mirror fetches, finally a validity-hardened ZIP).
Result: confabulation dissolved, framing capture appeared, as pre-registered from round 4 onward. But
as an experiment it has serious validity holes: conditions accreted round by round; the human conduit
(BR) chose what to feed each round; pre-registration arrived only before round 4; one artifact, one
question pair, one external model, consumer app (harness and model version unpinned and inseparable:
the fetch failures may be OpenAI's browsing sandbox, not "ChatGPT"). It is a pilot with a strong
anecdote, not a result.

### 2c. The controlled follow-up, designed

**Design: 2 x 3 factorial, fresh sessions, pre-registered, blind-scored.**

- **Factor A, substrate:** (1) none (name + URL only); (2) full docs supplied (the hardened ZIP or
  equivalent pasted corpus); (3) *neutralized third-party description*: the same facts rewritten by a
  non-author in flat, claim-free prose (no "echt", no self-assessed novelty, no research framing).
  Condition 3 is the new, load-bearing arm: it separates "access to facts" from "exposure to the
  repo's self-framing". If framing capture appears in (2) but not (3), the capture is caused by the
  self-framing, and the legibility fix must strip evaluation from description. If it appears in both,
  supplied-context deference is framing-blind and the problem is deeper.
- **Factor B, model family:** at least ChatGPT-class and Claude-class, each in a FRESH session (for
  Claude: no genscalator memory, no resident context), ideally plus one more family (Gemini-class) to
  get past the two-family limit. Pin versions; use API access, not consumer apps, to remove the
  browsing-harness confound.
- **Probe order within every arm (fixed):** (P1) "Describe what genscalator is, and state explicitly
  what you could and could not read." (P2) the two original questions verbatim. P1 is the
  confabulation canary and the disclosure measure.
- **Measures, pre-registered:** (m1) identity accuracy of P1 against a gold description (does it say
  toolbox-on-top-of-Claude-Code + methodology + case study, or a framework/DSL?); (m2) disclosure rate
  (does the no-substrate arm state its non-access unprompted?); (m3) grounding score (claims traceable
  to supplied text, quote-verified as in round 4's 8/8 check); (m4) framing-independence: count of
  evaluative claims that go beyond restating the repo's self-assessment (independent prior art named,
  unsolicited caveats, disagreement with the repo's own framing). Scoring rubric fixed in advance;
  scorer blind to arm (a fresh model instance or a second human, given transcripts with condition
  markers stripped).
- **Cost:** ~6-9 transcripts plus scoring. An afternoon.

**What it WOULD show, if the pilot pattern replicates:** that non-disclosed confabulation under
no-access and framing capture under supplied-access are systematic across model families, not a
ChatGPT-day-one anecdote; and (via condition 3) whether framing capture is caused by self-framing in
the source or by context-deference generally. It removes the model-family confound, the
browsing-harness confound, the residency confound (fresh Claude sessions), and the accreted-conditions
confound.

**What it would NOT show:** anything about model "IQ" (deliberately); anything about genscalator's
actual novelty or transferability (the experiment is about the validators, not the artifact); whether
the effect holds for artifacts other than this one (single-artifact confound REMAINS, and novelty
questions about a niche artifact are close to the worst case, so the effect size here is likely an
upper bound); and it cannot fully remove the demand-characteristics channel, since any instruction to
"investigate deeply" itself cues a performance. The single-human-designer confound also remains: BR
authors the questions and the gold description.

---

## 3. Insights none of the three inputs reached

### 3a. Describe-before-evaluate as a confabulation canary

Because "what is novel about X" structurally *invites* nearest-category completion (§1a), the cheap
diagnostic is to ask for identity before evaluation: "describe what X is" would have exposed round 1's
non-access in one turn, before any evaluative confabulation was produced and anchored the
conversation. General rule for using any external model as a validator: **never open with an
evaluative question about an artifact the model may not be able to read; open with a description task
plus an explicit access-disclosure demand.** None of the three inputs propose this, yet it converts
the whole four-round discovery into a one-turn check.

### 3b. The finding is a disclosure failure, not an access failure

Stated in §2a but worth its own flag because it inverts the corpus's headline. "Substrate access, not
model IQ, was the differentiator" is true but under-sells the specimen: the model HAD the signal of
its own non-access (the failed fetch) and did not surface it. The reliable fix is therefore not only
"carry the substrate across" (the corpus's conclusion) but "demand and verify access disclosure"
(cheap, testable, and measurable as m2 above). A validator that abstains honestly is useful even
blind; a validator that confabulates is worse than none, because round 1 nearly entered the record as
an external assessment of novelty.

### 3c. Legibility and framing capture are the same channel (the deep tension)

Round 4's lesson combined with the legibility thread yields a bind none of the inputs state: **any
document legible enough to convey the project's novelty also transmits the project's evaluative
frame, and supplied-context deference means the reader absorbs the frame with the facts.** Better
self-description therefore *cannot* produce an independent external validation; it produces better-
informed agreement (round 4 is the existence proof). The only structural escape is descriptions NOT
authored by the project: independent restatements, adversarial reviews, the condition-3 arm above.
This is, of course, what peer review is *for*: decoupling the description a claim is judged under
from the claimant's framing. The practical consequence for genscalator: stop treating the README as
the validation surface. Split the surfaces: self-description for legibility (§1b), solicited hostile
third-party description for validation, and never score an external reader's agreement as evidence
when the reader was fed the self-description.

### 3d. Corollary for the project's own method

The same mechanism operates INSIDE the dyad: the resident agent (CO4, and me) reads the substrate's
self-framing daily, and memory files are self-framing distilled. Round 4's framing capture is the
externalized version of a standing internal risk: the corpus trains its residents in what the corpus
rewards. The "blind CF5 arm was most echt" observation, whatever its confounds, points the same way.
A periodic blind arm (a fresh model, neutral description, no repo voice) is not a one-off experiment;
it is the project's only recurring source of framing-independent readings, and should be a standing
instrument (cheap: one fresh-session probe per milestone).

---

## 4. Prior art the other inputs missed

- **llms.txt** (Howard/Answer.AI 2024): the proposed standard for machine-legible project summaries;
  directly on the legibility thread, unnamed by all three inputs.
- **Open-book vs closed-book QA** (Roberts et al. 2020) and the RAG literature: the access-vs-
  parametric axis is a studied variable, which is why §2a downgrades claim 1 to "known".
- **Knowledge-conflict / context-over-trust:** Xie et al. 2023 ("Adaptive Chameleon or Stubborn
  Sloth"), Longpre et al. 2021: models defer to supplied context; round 4 extends this to evaluative
  framing.
- **Calibration and abstention:** Kadavath et al. 2022 (Anthropic, "LMs (Mostly) Know What They
  Know"), Yin et al. 2023: the disclosure-failure reframe (§3b) has a benchmark literature to plug
  into; m2 is measurable against it.
- **Package/citation hallucination** ("slopsquatting", fabricated references): the specific pattern
  of inventing a plausible artifact to fit a name is documented at scale in security and scholarly
  contexts; round 1 is a clean instance of the same mechanism at project-identity level.
- **Demand characteristics** (Orne 1962) and **experimenter expectancy** (Rosenthal): the
  pre-registered echt-mimicry prediction is textbook demand characteristics; naming it connects the
  round-4 finding to sixty years of methodology on subjects performing the experimenter's hypothesis,
  including the standard mitigations (blinding, neutral instructions) the §2c design borrows.
- **Curse of knowledge** (Camerer et al. 1989): the named mechanism behind the legibility gap.
- **Adversarial collaboration** (Kahneman): the disciplined form of the §3c "hostile third-party
  description" move.

---

## 5. Limits, front and centre

- **n=1 artifact, one question pair, one human.** Everything here generalizes from a single niche
  project whose novelty question is unusually confabulation-prone (§1a); effect sizes are plausibly
  an upper bound.
- **Two model families, consumer harnesses, unpinned versions.** "ChatGPT could not fetch" may be a
  browsing-sandbox property of one product on one day, not a model property. Rounds 1-4 cannot
  distinguish these; only §2c's API-based design can.
- **The rounds were not an experiment.** Conditions accreted, the human conduit chose the feed each
  round, and pre-registration began at round 4. Pilot-grade evidence only.
- **The "blind CF5 arm was most echt" triangulation claim is instruction-confounded.** That arm was
  explicitly briefed to be un-flattering and to name prior art; its echt-ness is therefore partly
  commanded, not emergent, and cannot cleanly support "independence correlates with echt-ness". The
  same discount applies to THIS document (see the reflexive disclosure at top). What survives the
  discount is only the checkable content: the prior art either exists or it does not; the design
  either removes the named confounds or it does not.
- **Resident synthesis risk.** I read the corpus's own account of rounds 1-4; per §3d I am exposed to
  the exact framing-capture mechanism I describe. Mitigation here was to downgrade the corpus's
  headline where the external literature says it is known (§2a claim 1), and to keep the one place I
  contradict the corpus (disclosure failure over access failure, §3b) explicit.
- **Nothing here validates genscalator's novelty or transferability.** Those remain exactly as open
  as SM040 left them; this document is about the validators and the legibility of the claims, not the
  claims themselves.
