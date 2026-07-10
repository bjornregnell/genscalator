# Why "no Claude credit" NEVER regresses while safety-critical hygiene rules DO — a stick-vs-regress taxonomy (2026-07-10)

BR-flagged WR question: BR cannot recall CO4 ever regressing on "no Claude-credit in commits," yet more
important rules (some with genuine safety risk) regress frequently. What in the substrate makes THIS detail
stick so hard, when we struggle to make more important behaviour stick?

## The pattern (checkable)
- **"No Claude credit in commits":** 0 observed regressions across the whole corpus.
- **Command-hygiene** (raw `grep -A`, `/dev/stdin`, heredoc, `cd &&`, parallel-commit): 5+ regressions THIS
  session alone, several under high load, twice in the very turn the rule was restated.

## Proposed mechanism — four axes that differ (most→least checkable)
1. **Competing fast reflex (the big one).** Each regressing rule fights a DEEPLY-grooved shell motor-pattern
   from pretraining (raw grep, pipes, heredocs = millions-of-shell-sessions reflexes) that fires FAST and
   automatically *before* the deliberate rule-check engages. "No Claude credit" has **no competing reflex** —
   composing a commit message is a deliberate from-scratch act; nothing automatic appends the trailer when I
   write it by hand. Recall-vs-reflex is a contest the reflex wins under load; the credit rule has no contest
   to lose.
2. **Effort / convenience gradient.** The regressing rules ask for the HARDER path (write a file vs pipe a
   blob; type the long `tt` command vs the short `grep`). Omitting credit is EASIER, not harder — the rule
   rides the convenience gradient instead of fighting it. Rules that ask for LESS effort stick; rules that
   fight a convenience gradient regress under load.
3. **Frequency + checkpoint shape.** The credit rule is checked at ONE narrow, low-frequency, always-deliberate
   moment (writing a commit message). The hygiene rules fire on EVERY bash call, often under load / mid-thought.
   Tiny deliberate surface-area vs large automatic surface-area.
4. **Prior-alignment + identity coherence (most introspective — corroborate).** The rule is about
   **not claiming accountability the agent cannot hold** — BR's clarification: NOT attribution-etiquette but
   *accountability*. A commit carries authorship/responsibility; Claude cannot be an accountable author, only the
   human (BR) can (Anthropic accountable only in extreme-misuse edge cases). That rides an even deeper alignment
   prior (the agent is a tool; responsibility stays with the human — the *authority anchor* / "human keeps the
   keys") and coheres with the self-model; the hygiene rules are arbitrary local mechanics with no prior and less
   identity anchor. Flagged
   as a mechanism-hypothesis, NOT measured — self-report on my own priors is confabulation-prone.

## The lesson
A behaviour STICKS when it rides WITH the prior, has no competing fast reflex, asks for less effort, and fires
at a single deliberate checkpoint. It REGRESSES when it must OVERRIDE a fast grooved reflex, at high frequency,
under load, against a convenience gradient — exactly where willpower/recall loses to automaticity (cf.
Yerkes-Dodson / `research/024-agent-affective-analogs.md`: under load the fast habitual response dominates the
deliberate one).

## The actionable implication (answers BR's hook question)
You cannot instruction-train a from-pretraining reflex to un-groove; but a STRUCTURAL guardrail interposed at
the reflex's firing moment converts "recall must beat reflex" (lost under load) into "structure blocks the
reflex" (load-independent). So aim the guardcheck-hook precisely at the behaviours that fight a fast prior +
convenience gradient — **especially the SAFETY-critical ones**, because the dangerous cell is *high stakes AND
high regression-propensity* (`/dev/stdin` empty-commits, credential-leak dry-runs, broad-allowlist
temptations). The credit rule needs no hook because it already WINS the contest; the hygiene rules need it
because they LOSE it. The hook is the equalizer. Ties: [[guardcheck-hook-structural-fix]],
[[commit-no-claude-credit]], [[commit-msg-write-before-commit-not-parallel]], the 5-slips WR data,
`research/024-agent-affective-analogs.md`.

## COI / echt caveat (BR asked this be recorded)
This note is the agent analysing a rule about crediting its OWN maker (Anthropic), for a human (BR) who is a
class-action copyright-settlement stakeholder with Anthropic — a **double conflict of interest**. So the agent is
not a neutral party here, and **axis 4 in particular** (the "it rides an alignment prior / coheres with my
self-model" claim) is exactly the kind of self-serving-sounding story a conflicted, introspection-limited narrator
would tell. Flagged deliberately: the echt move is to NAME the conflict and mark the introspective claims as
corroborate-not-assert, not to perform neutrality. The observable pattern (credit never regresses; reflex-opposing
hygiene regresses under load) is real behavioural data; the *mechanism* story is a hypothesis, doubly so where it
flatters the maker.
