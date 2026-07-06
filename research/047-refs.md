# 047 — Reference summaries (read-before-cite)

Reason-1 (need-to-know) references for the study + blog. **Wikipedia-depth refs** fetched via the REST summary API by a disposable subagent fleet (context-economy: the researcher's context stays clean). **Honest rule (from the summarizers):** cite Wikipedia only for what it *actually says*; several study-specific framings (analytic-vs-statistical generalization, researcher-as-instrument, enactment-beats-self-report) are OUR application — attribute those to the methods books we hold (Runeson & Höst, Yin, Wohlin), not to Wikipedia.

**Surf guard audit (2026-07-06):** `tt web get` is blanket-allowlisted regardless of host (wikipedia, arxiv, scholar all fetch clean, no prompt) → **HTML/text surfing** of any target is guard-free for solo/AFK. **Caveat (BR):** this does NOT cover **PDF downloads** — a paper PDF from Google Scholar (a) redirects into arbitrary publisher/repo domains and (b) is **binary**, and `tt web get` prints text with no file-output, so it cannot save/read a PDF. PDFs need `curl -o <file>` + Read (the logged **tt-web-file-output gap**), which is NOT the blanket-allowlisted path. **Practical:** for the few papers we actually need, prefer **arxiv direct** (`arxiv.org/pdf/<id>`) + `curl -o` + Read; and the arxiv **abstract** (via `tt web get`) already covers blog-depth read-before-cite (e.g. Lost-in-the-Middle, whose abstract page fetched clean).

**Seminal academic (the read-before-cite core):** the two method books are already in hand; the one paper to actually read is **Lost in the Middle** (below). RAG/Lewis 2020 optional.

---

## A. LLM context / memory / degradation

**Context window** — https://en.wikipedia.org/wiki/Context_window
- The max tokenized input an LLM can hold at once (tokens, not words); anything outside is invisible unless summarized/retrieved/re-supplied. Sizes have grown from thousands to (mid-2020s) hundreds of thousands / millions.
- *Backs:* the context window is a **bounded** finite resource → grounds the substrate / resident-core framing (only what fits is "resident").
- *Caveat:* the bound is model-dependent and moving — don't cite a fixed figure.

**Retrieval-augmented generation (RAG)** — https://en.wikipedia.org/wiki/Retrieval-augmented_generation
- A technique letting LLMs retrieve + incorporate external documents at query time to supplement training data (domain-specific / updated info). Proposed 2020, now widely adopted.
- *Backs:* the "externalize to substrate, answer from an external store not weights" reframe (the blog's central move).
- *Caveat:* RAG is retrieval-at-inference; our substrate is broader (memory + PB + glossary + git) — cite for the general principle.

**Lost in the Middle: How Language Models Use Long Contexts** (Liu et al., 2023) — https://arxiv.org/abs/2307.03172
- Authors: Nelson F. Liu, Kevin Lin, John Hewitt, Ashwin Paranjape, Michele Bevilacqua, Fabio Petroni, Percy Liang (TACL 2023). **Finding** (abstract, via the arxiv API): LM performance on long-context retrieval/QA **degrades significantly when the relevant info is in the MIDDLE** — highest when it is at the beginning or end (a **U-shaped positional** effect), even for explicitly long-context models.
- *Backs:* the empirical anchor that "context rot / lost-in-the-middle" is real and measured. Full text as **HTML/LaTeX-source** at `arxiv.org/html/2307.03172` (text, easier than the binary PDF, per BR).

## B. Psychometrics

**Cronbach's alpha** — https://en.wikipedia.org/wiki/Cronbach's_alpha
- A reliability coefficient for the **internal consistency** of a set of items (how closely they measure one construct); very widely used, widely warned-against if applied uncritically.
- *Backs:* the rationale for **multiple probe variations per dimension** + checking they cohere.
- *Caveat:* the lead does NOT give thresholds (≥0.7 etc.) — don't attribute those here; α assumes tau-equivalence + rises with item count, so a high α is not proof of one reliable dimension. (Also see critique #5 — α is misapplied to single-respondent deterministic cells; use with care.)

**Cohen's kappa** — https://en.wikipedia.org/wiki/Cohen%27s_kappa
- A chance-corrected statistic for **inter-rater reliability** on categorical data (more robust than raw percent-agreement); ranges -1 to 1.
- *Backs:* our ≥2-rater blind scoring (agreement beyond chance).
- *Caveat:* the Landis-Koch benchmark bands are NOT in the lead — don't attribute cutoffs to Wikipedia; κ is prevalence-sensitive (the "kappa paradox").

**Social-desirability bias** (note: hyphenated title) — https://en.wikipedia.org/wiki/Social-desirability_bias
- A response bias: respondents answer to look favorable (over-report "good", under-report "bad"); the article calls it "a serious problem with conducting research with self-reports."
- *Backs:* the punchline motivating **enactment over self-report** (measure what's done, not claimed).
- *Caveat:* framed for survey self-reports; the "enactment beats self-report" leap is OUR inference.

## C. Methodology

**Case study** — https://en.wikipedia.org/wiki/Case_study
- An in-depth examination of one bounded case in its real-world context; established across fields.
- *Backs:* the single-case framing is a legitimate research form.
- *Caveat:* the lead does NOT cover analytic-vs-statistical generalization — attribute that to Yin / Runeson & Höst, not Wikipedia.

**Action research** — https://en.wikipedia.org/wiki/Action_research
- Combines action + inquiry via critical reflection (Lewin, 1944/1946: a spiral of plan → act → fact-find).
- *Backs:* intervening in one's own practice is a *named methodology*, not a defect.
- *Caveat:* the lead frames it for social-science transformative change + doesn't discuss reactivity-disclosure — that framing is ours.

**Reflexivity (social theory)** — https://en.wikipedia.org/wiki/Reflexivity_(social_theory)
- Circular cause-and-effect in belief structures; effects loop back onto the reflexive agent.
- *Backs:* the researcher-as-instrument threat (studying your own collaboration feeds back onto it).
- *Caveat:* the lead is broad/abstract + doesn't spell out the qualitative-methods "researcher-as-instrument" usage — that's our application.

## D. Identity / attachment (the blog's human hook)

**ELIZA effect** — https://en.wikipedia.org/wiki/ELIZA_effect
- The tendency to project human traits onto rudimentary programs (named for Weizenbaum's 1966 ELIZA; users believed in its understanding despite being told its limits).
- *Backs:* a named, documented human tendency to attach to / anthropomorphize conversational systems — grounds the honest attachment caution. COI-adjacent (AI/Claude blogging).
- *Caveat:* classically about *rudimentary* programs — applying it to modern LLMs extends the 1966 sense.

**Ship of Theseus** — https://en.wikipedia.org/wiki/Ship_of_Theseus
- The classic identity paradox: is an object the same after all its parts are replaced?
- *Backs:* the identity-continuity-across-a-reset hook, with philosophical pedigree.
- *Caveat:* it's about *gradual* replacement of a persisting object, not an instantaneous reset — frame as allusion, not strict equivalence.

## E. Cognitive analogy (ANALOGY-ONLY)

**Yerkes-Dodson law** — https://en.wikipedia.org/wiki/Yerkes%E2%80%93Dodson_law
- An inverted-U relationship between arousal and performance (Yerkes & Dodson, 1908): performance rises with arousal to an optimum, then degrades.
- *Backs:* the curve-shape our demand-framing / over-response analogy borrows.
- *Caveat (MUST accompany the cite):* **analogy, not mechanism** — an LLM has no literal arousal. Secondary: even in humans the "law" status is thin (the 1908 paper was lightly cited for decades).
