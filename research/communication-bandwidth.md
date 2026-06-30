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
