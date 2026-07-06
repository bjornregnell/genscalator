# WR-data — deliberate terse-precise comms via the developed vocabulary (2026-07-06)

**Episode.** During the SM006 numbering-refactor planning, BR flagged (verbatim, lightly cleaned):

> "meta: WR data: I have been trying to use our developed terminology and cues and numbers now to stay very terse and
> precise. why: i am trying to learn how to strike the balance between me having to type less and you getting exactly
> what i mean. WDYT?"

Context: across this session BR compressed high-bandwidth intent into very few keystrokes using the shared substrate
vocabulary — `HD:`, `note:`, `pin:`, `fyi:`, `go`, `:Z`, `edit:`, plus the ID schemes (`OD/CD/SMnnn`, `D1 YES / D5 NO`).
E.g. the entire SM006 ratify arrived as "`D1: YES … D4: YES; D5: … NO`", which expanded unambiguously into five plan
edits.

**Reflection (agent, labelled).**
- **It's the communication-bandwidth thesis in action** (`002-communication-bandwidth`, `Comms shorthand` in
  foundations): the vocabulary is a *compression codec* both sides share, so BR spends few keystrokes and the agent
  decompresses to exact intent. The keystroke saving is real and the ambiguity has stayed low **this** session.
- **Where it works vs risks under-specifying.** Compression is lossless exactly where a term/cue/ID is **already
  pinned** (shared definition). The one failure mode is compressing something **not yet in the shared vocabulary** —
  then the agent must guess. So the strategy's reliability scales with how much has been pinned; the fix is the pin
  pipeline itself (`note:` -> `pin:`): when BR coins a *new* compression on the fly, flag it once so it gets a durable
  definition, after which it is free.
- **Motor-cost axis, not just tokens.** This is the same lever as all-lowercase prose and `:Z`'s SHIFT-held ergonomics
  (`002`, `tired-cue`): optimize BR's *typing* cost while the agent reconstructs. Terse IDs (`SM006`, `CD11`) are a
  strong case — a 5-char handle names a whole plan item.
- **Meta-meta:** BR is *consciously training* the balance (his word: "learning"), which makes the vocabulary a
  co-evolved instrument, not a fixed spec — consistent with the substrate-as-multiplier / co-adaptation framing.

**Net:** a genuine, low-ambiguity bandwidth win; the residual risk is un-pinned novel compressions, mitigated by the
existing note->pin pipeline. Worth watching for a real mis-decompression event (none yet this session) as the
falsifier.

## Risk (BR-raised): the codec can outgrow the human's memory (asymmetric)
BR (2026-07-06): *"note the risk that the vocabulary gets esoteric and big may risk remembrance and ambiguous use by
human (my near-term memory is not as good as yours, and you have all my allowed disk space for your long-term memory so
you probably beat me there too)."*

- **Asymmetric memory is the binding constraint.** The codec has two users with **unequal recall**: the agent's is
  **durable + disk-backed** (memory files, foundations) and effectively unbounded; the human's is **near-term,
  biological, limited**. So as the vocabulary grows large/esoteric, the **human** is the first to mis-remember or
  mis-apply a cue (encode the *wrong* compression) — the mirror of the agent's mis-decode risk, and the **more likely**
  failure, because the weaker-memory party sets the ceiling.
- **Learnability budget = the human's, and it binds.** Same tradeoff already named for the `go X` verbs
  (`035-go-verb-vocabulary`: "bounded by the learnability-budget — prune before adding"). Generalized: **the shared
  vocabulary's size ceiling is the human's recall, not the agent's.** Adding a term carries a *human* memory cost even
  when its agent cost is ~zero — so growth must be paid in the scarce budget, not the abundant one.
- **Mitigations.** (a) **prune before adding** — retire dead cues; (b) keep cues **mnemonic + motor-cheap** (typeable,
  non-colliding — `pin` over `etch`, `:Z` ergonomics); (c) the agent should **expand/offer the definition** of a term
  the human seems to have half-forgotten rather than assume; (d) watch for a **human mis-encode** event (BR uses a cue
  against its pinned def) as the falsifier that the codec has outgrown its budget.
- **Reframe — the asymmetry is also the backstop.** The agent's durable memory isn't only a risk amplifier; it's the
  safety net. BR need not hold the whole vocabulary — he holds the *frequent core* and **outsources the rest to the
  agent on demand** (recall-as-a-service). So the design target is not "small vocabulary" but "**small active set for
  the human + full set recallable via the agent**." (This is itself a nice instance of coupled-system capability:
  the *pair* remembers more than either party.)
