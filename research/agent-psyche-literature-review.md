# Research topic — a (systematic) literature review of "agent psyche" academic work

**Status:** open, proposed (BR 2026-07-04). A grounding task for the agent-psyche / human-psyche blog thread
(006/008) and the `#agent-psyche` / `#human-psyche` WR tags.

## Why
We've coined a cluster of terms (echt, the confabulation caveat, the introspection→structure move, the agent/human
psyche dual, sycophancy-as-trained-niceness). **Echt demands we ground them in the peer-reviewed literature** before
publishing — to (a) not reinvent established concepts, (b) not *contradict* solid empirical work unknowingly, (c) map
our coinages onto accepted terms, and (d) cite real, live sources (blog-assistant §8). BR's framing: human-psychology
literature is **huge and uneven** — some is empirically solid, some is unfounded speculation (Freud-style, often
sexuality-themed, no empirical backing). **Apply the same grounding discipline:** prefer empirically-backed work;
treat unfalsifiable speculation as speculation.

## Hunch (BR asked) — is "agent psyche" an established term, and will we find papers?
**"Agent psyche" is *our* coinage — not established.** But there is a real, fast-growing literature under other
names, so **yes, peer-reviewed papers almost certainly exist** (some in top venues). Candidate established terms +
seed works — **ALL UNVERIFIED RECOLLECTIONS, to be checked on Google Scholar; I can confabulate a plausible-but-wrong
citation, which is exactly the failure mode this thread studies, so NONE of these count until verified:**
- **"Machine psychology"** — Hagendorff (~2023), "Machine Psychology: Investigating Emergent Capabilities and Behavior
  in LLMs Using Psychological Methods." *Closest umbrella term to our "agent psyche."*
- **"Agent / LLM personality"** (BR's guess — likely real): Big-Five/OCEAN in LLMs; Serapio-García et al. (~2023)
  "Personality Traits in Large Language Models"; "PsychoBench"; persona-consistency in dialogue.
- **"Cognitive science / psychology of LLMs"** — Binz & Schulz (~2023, *PNAS*), "Using cognitive psychology to
  understand GPT-3."
- **"Machine behaviour"** — Rahwan et al. (2019, *Nature*) — the manifesto for an empirical behavioural science of
  machines.
- **Sycophancy** — Sharma et al. / Perez et al. (Anthropic, ~2023), "Towards Understanding Sycophancy in LMs" —
  directly our niceness-vs-honesty point.
- **Anthropomorphism / ELIZA effect** — Weizenbaum (1966, verified, see blog 006); Nass & Reeves, *The Media
  Equation*; Epley et al. anthropomorphism theory; the **ELIZA effect** entry.
- **Theory of mind in LLMs** — Kosinski (~2023) and rebuttals (contested — good for the "unfalsifiable-from-inside"
  point).
- **LLM introspection / self-knowledge** — recent (~2024) "language models can learn about themselves by
  introspection"-type work.
- **Confabulation / faithfulness of self-explanation** — the interpretability literature on unfaithful
  chain-of-thought / rationalization (our confabulation caveat has prior art here).

**Bottom line hunch:** no single accepted "agent psyche" term, but a **nascent, real, citable field** — "machine
psychology" is the nearest umbrella; "LLM personality," "cognitive science of LLMs," "machine behaviour," and
"sycophancy" are live sub-areas with peer-reviewed (incl. *PNAS*/*Nature*) work. We are late enough that we must
position against prior art, early enough that our *specific* framings (echt, the two-body dual, introspection→structure)
may be genuinely novel.

## Method — do it as a proper SLR (SE sense), and confirm the agent knows what that is
BR asked me to confirm I know a **Systematic Literature Review** in the software-engineering sense. I do:
- **Kitchenham & Charters (2007), "Guidelines for performing Systematic Literature Reviews in Software Engineering"**
  (EBSE/Keele technical report) is the SE standard. An SLR = a *protocol-driven, reproducible* review: **(1)** planning
  — define **RQs**, a **search protocol** (databases, search strings, time window); **(2)** conducting — run the
  search across **Scopus, IEEE Xplore, ACM Digital Library, Web of Science, Google Scholar**; apply **inclusion/
  exclusion criteria**; **quality assessment**; **data extraction**; **(3)** reporting — **synthesis** + threats to
  validity. Everything documented so another researcher reproduces the study set.
- **Snowballing** — Wohlin (2014), "Guidelines for snowballing in systematic literature studies" — backward/forward
  reference-chasing as a search strategy, strong for interdisciplinary/nascent topics where keyword search misses work.
- **Systematic mapping study** — a broader, categorising variant (Petersen et al.); **better fit for a nascent field**
  like this (map what exists and cluster it, rather than answer one narrow RQ). *Recommendation: start with a mapping
  study, tighten to an SLR on the sub-question that matters most (probably sycophancy/honesty or introspection-reliability).*

**BR's steer (2026-07-04) — right-size the protocol.** BR (a co-author of the two SE research-methods books below, and
one of Wohlin's first PhD students — a self-reference to own inline where cited) cautions that the **full Kitchenham SLR protocol may be
over-arching here:** real *hard* empirical evidence is scarce in SE, so a heavy protocol can cost more than it returns.
He'd rather weigh whether **simpler snowball sampling + qualitative synthesis** (cf. Wohlin 2014; and the case-study /
qualitative tradition of *Case Study Research in Software Engineering*) is **more cost-effective** than the deep
machinery. **Open question worth testing:** does **agent-assisted SLR** shift that balance? — if an agent can cheaply
do the mechanical search/screen/extract, the protocol's cost drops and the fuller method may become worth it again.
(That is itself an engineering/action-research question — measure the agent-assisted-vs-manual cost/quality tradeoff,
don't just assert it.)
**BR's sharpening (2026-07-04) — the human-judgment cap remains.** The agent cheapens the *mechanical* cost (search,
dedup, extract), but **not** the *human-judgment* cost: someone still has to read each screened paper's title /
abstract / (sometimes) whole text and assess **quality and relevance** — a call the human must own (accountable
control; the agent can't be trusted with the final verdict — corroboration asymmetry). That assessment is **tedious,
slow, and it caps the number of papers actually reviewed.** So the balance shifts only *partially* — the bottleneck
moves to (stays at) human quality/relevance judgment. The real open question, then: can the agent make **each human
judgment cheaper** — triage candidates, extract the relevant passage, flag a reason — so the human *confirms/overrides*
rather than reads cold? Two catches keep it honest: (a) the human must still **verify** the triage (a weak paper rated
relevant has to be caught), so cost may just move to verification; (b) agent relevance/quality ratings risk
**confabulation** (rating a judgment it can't truly make), so the human can't drop the guard. Whether triage nets a
real saving or just adds a to-verify layer is **empirical and depends on the agent's assessment reliability** — measure
it, don't assume it. Refs (with COI flags) in [`../blog/References.scala`](../blog/References.scala).

## Draft RQs (to refine)
- RQ1: What established terms/fields study "the psychology/behaviour of LLM agents," and how do they relate?
- RQ2: Which of our coinages (echt, confabulation caveat, introspection→structure, the psyche dual, sycophancy) have
  prior peer-reviewed grounding, and which are genuinely novel?
- RQ3: What is *empirically established* (vs speculated) about: LLM introspection reliability; sycophancy; personality
  measurement; anthropomorphism effects on users?

## Candidate search strings (Google Scholar first, then Scopus/IEEE/ACM)
`"machine psychology" LLM` · `large language model personality Big Five` · `cognitive psychology GPT / LLM` ·
`machine behaviour AI Rahwan` · `sycophancy language models` · `anthropomorphism conversational agent ELIZA effect` ·
`theory of mind large language models` · `LLM introspection self-knowledge` · `faithfulness chain-of-thought
self-explanation`.

## Deliverable + honesty guardrails
- A mapping/SLR note + a References section feeding blogs 006/008 (real, live-linked, verified citations only).
- **Every citation VERIFIED on the source before it ships** — a recalled-but-wrong reference is *false echt* (and the
  self-referential embarrassment of hallucinating a citation in a paper about agent confabulation). Mark unverified
  hunches as hunches (as above) until checked.
- Map our terms → established ones honestly; where we're novel, say so plainly; where prior work exists, cite and build
  on it.

Related: blogs 006 (agent psyche) / 008 (joint zone); `wr-data/harness-ux.md` `#agent-psyche` `#human-psyche`;
`agent-affective-analogs.md`; foundations *echt*.
