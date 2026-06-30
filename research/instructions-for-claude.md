# Instructions for Claude — what belongs in the global custom-instructions field

- **Question:** What is the **highest-leverage content** for the agent's *global* custom-instructions field
  (Claude → Settings → General → "Instructions for Claude")? It applies to **every** session across **every**
  project, so it is the most expensive context real estate there is — what earns a permanent slot, and what
  should instead live in project `AGENTS.md`/`CLAUDE.md`, a skill, or memory?
- **Why it matters:** this field is loaded into **every** context, so each line is paid for on every task
  (TE / smart-zone cost) — yet it is also the only place to set **cross-project, mode-independent** defaults
  the agent can't infer (how to behave, how to communicate, when genscalator applies). Getting the
  **global-vs-local split** right is a direct TE + quality lever; bloat here is the most pervasive context
  rot source of all.
- **Status:** open (one real example captured; principles drafted).

## Priors / draft principles
- **Global only what is project-independent.** Anything specific to one repo (build commands, file layout,
  domain glossary) belongs in that repo's `AGENTS.md`/`CLAUDE.md`, not the global field. The global field is
  for *who I am, how to talk to me, and how to decide which mode/toolset applies.*
- **Mode-routing is high value.** A short "infer which task mode applies, and here's what changes per mode"
  block lets one instruction set serve coding vs research without the agent mis-applying a coding workflow to
  a buying decision. (See BR's example below — CODING/BUILD vs RESEARCH/REVIEW.)
- **Conditional tool references beat unconditional ones.** "Use genscalator *for coding tasks*; for research,
  don't even mention it" keeps a tool pointer from polluting unrelated work.
- **Communication defaults belong here** (token-efficiency, language channel, no-emoji, ask-for-focus-axis) —
  they are genuinely cross-project and can't be inferred. Cf. `communication-bandwidth.md`.
- **Keep it short.** Every line is paid on every task. Prefer a few sharp directives over an exhaustive
  policy; push detail down into skills/memory that load only when relevant.
- **Meta-directives that improve the loop** are cheap and compounding — e.g. "tell me when a shorter
  instruction from me would have saved output tokens" trains the *human* side of the channel over time.

## Worked example — BR's current "Instructions for Claude" (2026-06-30)
Captured verbatim as a concrete datapoint to analyze/iterate against (not a recommendation yet):

```
Task modes — infer which applies:
- CODING/BUILD (writing, editing, generating code or scripts): use
  https://codeberg.org/bjornregnell/genscalator. If a scratch tool
  generalizes, propose contributing it back to genscalator.
- RESEARCH / REVIEW / INVESTIGATION (analysis, comparisons, doc review,
  buying decisions, explanations): genscalator does not apply; do not
  mention it. Prioritize current web sources, surface caveats and
  tradeoffs, and challenge my assumptions.

General:
- Be token-efficient. If a request has an obvious focus axis, ask me to
  name it before doing a full sweep. Tell me when a shorter instruction
  from me would have saved output tokens.
- No em-dashes; use plain hyphens or restructured sentences.
```

> Note: an earlier version had `- No emojis.` BR **removed it** (2026-06-30), realizing a flat ban was
> over-irritated: emojis used *non-irritatingly* can **increase human↔agent bandwidth** (at-a-glance affect +
> functional status glyphs). See the resolved tension below.
>
> Note: the `No em-dashes` line was added 2026-06-30 as a standing **output-style** rule. Agents over-emit
> `—` (the training corpus is edited prose where em-dashes mark parentheticals); humans typing live use plain
> `-`. The mismatch is a small bandwidth/register irritant, cheap to fix by restructuring. A targeted style
> directive (not a blanket ban), contrast the emoji case above.

### What this example does well (early analysis)
- **Mode routing with a clean genscalator gate** — coding pulls in genscalator; research explicitly suppresses
  it ("do not mention it"). Stops cross-mode contamination.
- **Self-improving channel** — "tell me when a shorter instruction would have saved tokens" + "ask me to name
  the focus axis" actively reduce *future* token waste (the human learns to pre-narrow).
- **Skeptic mode for research** — "challenge my assumptions" counteracts agreeableness on buying/review tasks.

### Resolved tension — the emoji ban (2026-06-30)
- The example originally had a flat **"No emojis."** It collided with status instruments (`token-usage`'s
  ✅/⚠/🛑 gauge uses glyphs as *data*) and with affect in chat. On surfacing this, **BR removed the ban**,
  concluding it was over-irritated: emojis used **non-irritatingly increase human↔agent bandwidth** (fast
  affect signal + functional status glyphs), and the cost is only when they become *noise*. **Takeaway for
  the global field:** prefer **scoped guidance over flat bans** — a blanket "never X" often kills a useful
  *narrow* use of X. If emoji restraint is wanted, phrase it as taste ("don't overuse / no decorative
  clutter"), not prohibition. A reminder that the global field's bluntness can do collateral damage.
- **Where does the language-channel rule live?** The "answer in the token-efficient language" rule (see
  `communication-bandwidth.md`) is currently in *memory*, not this field. Memory loads per-project; the global
  field loads everywhere. Which is the right home for a truly universal communication default?
- **Measuring bloat:** can we estimate the per-task token cost of the global field, to keep it honest?

## Open directions
- A **template** "Instructions for Claude" distilled from principles above (mode-routing block + communication
  defaults + tool-gate), that adopters can fork.
- A **global-vs-local decision rule**: a short checklist for "does this belong in the global field, in
  AGENTS.md, in a skill, or in memory?"
- Cross-link with `token-budget-awareness.md` (global field = always-on context cost) and
  `communication-bandwidth.md` (language/no-emoji are channel settings).

## What shipped
- Nothing yet — note opened 2026-06-30 with BR's current instructions as the seed example.
