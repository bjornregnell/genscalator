# Human–agent communication bandwidth (the language-channel question)

- **Question:** For a **non-native-English human** working with an English-centric agent, what is the
  **bandwidth- and TE-optimal language per direction** (human→agent vs agent→human)? When does the human's
  L1 input beat L2 input, and when is the agent's output *in the human's L1* worth its token premium?
- **Why it matters:** human↔agent **communication bandwidth** is a first-class **quality + TE** factor — a
  cousin of *review overload* / CF. A non-native human forced into L2 may **under-specify or mis-phrase
  intent** (the most valuable signal in the whole loop); an agent generating the human's L1 pays a **token
  premium**. This is the *linguistic* dimension of the human–agent channel, and it is **tunable**, not a
  fixed default.
- **Status:** open (a working rule adopted — see *What shipped*).

## Findings / priors
- **Tokenizers favor English** (~1.2–1.5× tokens for Swedish: å/ä/ö + compounds split worse). So *agent*
  output in a non-English language is the **expensive direction**; *human* input in L1 is **cheap** (human
  inputs are short vs agent output + tool/file context — marginal share of total tokens).
- **Agent comprehension is ~language-agnostic** across major languages — the agent understands the human's
  L1 about as well as English. So **human L1 input loses ~no agent comprehension** while **gaining human
  expressive bandwidth** (faster, more precise, less mis-statement of intent).
- ⇒ **The optimum is asymmetric:** **human writes L1, agent writes the cheapest-yet-clear language** (often
  English for a strong-L2 human), and the agent **switches to the human's L1** when review-precision/nuance
  justifies the premium.
- **Risk — idiosyncratic L1:** a human who is *creative* with their L1 (coinage, dialect, playful
  compounds) can lose the agent. Mitigations: human keeps it reasonably **canonical** and **keeps technical
  terms in the canonical language** (English for programming); agent **asks for clarification on ambiguous
  input rather than guessing**.

## Motor cost — a second axis of "cheapest-for-human" (2026-07-03, BR)
The "cheapest-for-human" side of the asymmetry is **not only *language* choice — it includes *physical
keystroke / motor* cost.** BR (2-3-finger typist; SHIFT is disproportionately slow for him) asked whether
he can drop capitalization and type prose **all-lowercase**, using `.` `,` `?` as sentence separators.
- **Answer: yes, no loss.** Agent comprehension does **not** depend on capitalization in prose — sentence
  boundaries and intent reconstruct from context, not case. So lowercasing is another **cheap-for-human,
  cheap-for-agent** move, exactly parallel to L1-input: the human writes in whatever minimizes *their* cost,
  the agent absorbs the (near-zero) reconstruction cost.
- **Carve-out — case-sensitive tokens:** capitalization *does* carry information in a few spots —
  **code** (identifiers/types are case-sensitive: `Fyle` ≠ `fyle`), **proper nouns/acronyms** (`us` vs `US`),
  and the pronoun `i`/`I`. Rule: **prose lowercase is free; code and proper nouns stay case-exact.** The
  agent asks on the rare genuine tie rather than guessing.
- **Reverse direction is *also* asymmetric — and the other way:** proper capitalization in the *agent's*
  output costs it **~nothing** (casing barely moves tokenization; never trades against quality) while making
  prose nicer for the human to read. So the optimum is **human writes lowercase (saves fingers), agent writes
  properly-cased prose (saves nothing, reads better).** Each side optimizes its own cheap axis; neither pays
  for the other's preference.
- **Generalization:** "cheapest-for-human" = min over *all* human costs (language fluency **and** motor
  effort **and** cognitive load), not just token count. The token-optimal channel is agent-side; the
  human-side channel should minimize *human* effort, whatever form that takes.

## Open directions
- A **switch heuristic** for the agent: default to the cheap output language; flip to human-L1 output when
  (a) the human asks, (b) the topic *is* the human's L1 source text/terminology, or (c) a review-precision
  signal fires.
- **Measure** comprehension-error rate per channel — does L1-in actually reduce mis-specification?
- **Context-fill lever:** language choice is also a *smart-zone* lever — a cheaper output language leaves
  more **smart-zone budget**. See `[[smart-zone-ceiling]]`, `[[token-budget-awareness]]`.

## What shipped
- A working collaboration rule (introprog session 2026-06-30): the human (BR) writes sv/en freely (en for
  programming terms / non-canonical sv); the agent answers in the **token-efficient** language (English
  default) and **asks for clarification on idiosyncratic Swedish** rather than guessing. Captured in the
  agent's memory; **candidate to graduate into a genscalator skill / AGENTS.md guideline** on the language
  channel.
