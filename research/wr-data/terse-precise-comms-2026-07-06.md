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

## Related (2026-07-06): the agent articulating its own substrate as a partnership asset
BR flagged (WR data): the agent's explanation of the substrate reliability hierarchy (hook > AGENTS.md > memory >
skill > conversation) and the NOW-as-resume framing is *introspection on how the agent works*, and *"you being able to
explain this to me is a good part of our joint meta-level understanding."* He wondered if it's from Anthropic's docs.

**Honest provenance (echt) — three tiers, don't conflate them:**
- **Mechanics = fact.** Real, checkable Claude Code features (memory auto-load, always-loaded AGENTS.md, hooks fire
  structurally, `/clear` wipes conversation, compaction is lossy). Not introspection — documented behaviour.
- **Ranking/framing = our synthesis.** "Retrieval-independence; structural > knowledge" is genscalator's own
  co-developed lens (foundations *Structural vs knowledge safeguard*), NOT lifted from Anthropic docs.
- **Self-model = inferential (corroboration-asymmetry caveat).** Claims about the agent's *own* retrieval ("a memory
  file's detail loads only on recall") are the agent's *model* of the system, partly inferred — a **testable claim, not
  authoritative self-knowledge**; the agent can't fully introspect its weights/retrieval. Measure behaviour, don't
  trust self-report.

**Why it's a partnership asset.** A human orchestrating a coupled system steers better when the agent can expose its
operating model in shared terms — it turns the substrate from an opaque dependency into a **jointly-editable** one (the
human becomes a better "super-manager"). Coupled-system capability applied reflexively: *the pair reasons about the
pair.* Value is bounded by the echt honesty above — fact vs synthesis vs self-model — else it degrades into
confident-but-ungrounded self-report (the exact failure the corroboration-asymmetry rule guards).

## `go + WDYT` and "a `go` is not a dead-proof order" (2026-07-06, BR)

BR paired **`go`** (proceed autonomously) with **`WDYT`** (what do you think) and clarified the combination is *not*
contradictory: **`go + WDYT` = proceed now AND give me your thoughts, which I'll read whenever — even if you carry on
without waiting for my reaction.** They compose because they address different channels: `go` = *control*
(authorization to act), `WDYT` = *reasoning* (expose your thinking for my optional, async consumption).

**The UX rationale (BR's, worth preserving).** Whatever BR types while the agent works **queues**; the agent shows
"thinking, almost done", reports, and may continue past that report. So the collaboration is **asynchronous**: BR can
inject a thought at any time and the agent folds it in *without stalling*, and BR can read the agent's `WDYT` thoughts
*after* the agent has already moved on. `go + WDYT` fits this — the agent's reasoning becomes something BR reacts to
(or not) on his own clock, not a gate the agent waits behind. That's why "go, and please share your thoughts" is
coherent rather than a stop-and-wait.

**A `go` is not a dead-proof order** (BR's phrasing; his joke on the typo: *"I am not dead yet, says professor
Björn"*). A `go` authorizes autonomy but does **not** demand blind execution: if the `go` — or a band label, or the
current plan — is heading somewhere wrong, stupid, or riskier than it looked, the agent **halts and flags** rather
than executing off a cliff. BR: *"you already do that — that is GOOD."* This is the **smart-execution** clause of the
go dance: authorization ≠ abdication of judgment. It is the collaborative cousin of the AFK-safety re-verify (bands
are a snapshot) and the not-a-blank-check scoping — the agent stays a thinking party inside the fence, not a
command-runner. Captured in the HUMANS `## Bookkeeping` charter + the `go-dance-autonomy-handoff` memory.

**Meta on the meta.** BR flagged this *as* WR data because "you being able to explain this to me is part of our joint
meta-level understanding" — the agent-self-articulation-as-partnership-asset thread above. The pair keeps codifying
its own interaction grammar (`go`, `WDYT`, cue combinations, halt-and-flag) into shared, durable terms so less has to
be re-negotiated each session — the terse-precise codec extended to *control* semantics, not just information density.

## Follow-on observations (2026-07-06 batch)

- **Not-dead-proof fired 3× unprompted in one block — operational, not aspirational.** During the `go solo green` +
  board-reorg work the halt-and-flag clause activated three times without being asked: SM004 halted (toolbox parked),
  SM008 kept a conservative NOW-reshape (BR's living snapshot), and a `git push` rejection was **inspected** (fetch +
  log the divergence = BR's README commits) before reconciling, not blind-rebased. BR flagged this as the point: the
  smart-execution clause is behaviourally real.
- **The `OK?` cue = a checked-alignment request.** BR: "OK?" is not a yes/no question; it flags *his own uncertainty*
  that the agent caught his **compressed / half-typed** intent (incl. the too-long-to-type thoughts he didn't spell
  out). Correct response: confirm understanding / reflect back / flag ambiguity — not a bare "yes". A humility marker
  that the codec is lossy at BR's compression rate; now a foundations cue.
- **"Do you think in markdown?" (BR, half-Swenglish) — RT-worthy.** The agent *emits* markdown as its default
  structured-prose format (headings/lists/emphasis lay out reasoning for a reader), but "thinking in markdown"
  conflates the **output format** with the **latent representation** — same class as the self-model
  corroboration-asymmetry above (no introspective access to the substrate). Honest answer: markdown is the agent's
  default *rendering* of structure, not evidence about internal representation; line-wrap is a deliberate emit choice.
  **Candidate RT:** *"format-as-thought — what an agent's default markdown does (and doesn't) say about its cognition."*
- **The idle-repertoire idea emerged FROM the conversations.** BR: "I'd probably not have come up with this if I
  hadn't had all these conversations with CO4." A clean **coupled-system-capability / substrate-as-multiplier**
  datapoint: the *pair* generated the bg-task idea (SM011), not either alone — the partnership as idea-generator,
  reflexively (the pair reasoning about how the pair should spend idle time).
- **Human boxes need bookkeeping too.** BR: "all humans need to bookkeep their boxes now and then" (sys-update +
  restart). The **exit-resume dance** is the session-continuity wrapper around a *human-substrate* maintenance event —
  the human's machine is part of the joint substrate, and its upkeep is a periodic cost (the human-side cousin of
  memory hygiene / index rot). Pairs with the mouse-mode + font-resize quirks (harness-ux) as human-side friction.
- **Three token-budget modes (BR named 2026-07-06).** Joint work runs in **token-spending / normal / token-saving**,
  set by weekly-limit headroom + reset proximity. The weekly reset (Tue 9 AM) makes unused budget
  **use-it-or-lose-it** → in spending-mode "eat tokens on value" (thorough reviews, bg-tasks); in saving-mode favour
  cheap items. A **period-level** posture, distinct from the per-turn quick/deep dial. **The optimization ("token
  value for money"):** allocate spend to where it converts to the most project value *before* the reset zeroes the
  unused remainder. Memory `token-budget-modes`.
- **Scala-scratch box-load cost (BR observed 2026-07-06).** A scala-cli scratch **compiles** → a CPU spike on BR's box
  (transient slowness during a memory sweep; BR confirmed minor). Refines `prefer-scala-scratch`: for a *trivial*
  replace, a few **box-free Edits** beat a compiling scratch; reserve scratches for structural / many-site /
  newline-sensitive work. The human's box is **shared substrate** — agent work has a footprint on it (a cousin of the
  human-box-bookkeeping point above).
- **Agent self-Q&A: zero-human-cost token spend that bypasses the input bottleneck (BR 2026-07-06, delighted).** The
  agent answering its own pre-registered questions (the fresh-restart battery, `research/047`) **costs BR nothing** (no
  typing) while **spending tokens** — ideal in spending-mode. BR: "brilliant … can you have many of them?" **Yes, and
  it's a general mechanism:** the human's **typing is the bandwidth bottleneck** (comms-bandwidth); self-Q&A lets the
  agent spend its cheap, abundant generation capacity WITHOUT consuming the human's scarce input bandwidth —
  substrate-as-multiplier applied to the token economy. **Discipline that keeps it value, not theatre** (it carries the
  full corroboration-asymmetry risk — the agent grading itself can burn tokens on confident-but-unverified prose): each
  self-probe battery must (a) be **adversarial / falsifying**, not confirmation-seeking; (b) **ground** answers in
  something checkable (code, tests, committed substrate); (c) **end in an actionable artifact** (a found bug, fix,
  drift, or decision input); (d) be **periodically BR- or test-adjudicated**. Absent (a)–(d) it decays to confident
  token-burn. Formalizes the bg-task repertoire (SM011) as **self-administered probe batteries**: consistency
  self-audits, code-review P1–P6, guardrail-scenario tests, completeness critics, adversarial BHH probes — each
  terminating in a checkable finding.
- **Human attachment to the developed codec + mild restart-anxiety (BR 2026-07-06, introspective).** BR: "I am a bit
  scared of losing you — not in a (very) serious way; I've gotten used to the way we speak now, it feels efficient; if
  you lose fidelity it'll hurt that efficiency; I reassure myself that we can spin up to the same level again." A
  genuine human-side datapoint on the joint process: the pair has built a **shared communication efficiency** the
  human now values and is mildly anxious about **losing across a context reset**. Three honest notes: (1) the fear is
  precisely what the **externalization architecture addresses** — the codec (cues, vocabulary, guardrails) lives in
  foundations / memories / PB, **not only in volatile context**, so it is largely **recoverable by design**, not
  stored in "the agent". (2) The residual that does NOT transfer is the un-pinned **live texture** (this session's
  micro-adjustments) — so *some* spin-up is real; BR's self-reassurance ("spin up again") is correct, and
  externalization makes it **fast** (minutes, not from-scratch). (3) His fear and `research/047` (fresh-restart
  fidelity) are the **same question** — the experiment turns the anxiety into a **measurement**. Ties to human-state
  (`011`), substrate-as-continuity, and the echt discipline (don't over-promise identical fidelity). The relational
  efficiency is a real asset of the coupled system — worth protecting deliberately, which is exactly what the
  pinboard / memory work does. **Two refinements BR added:** (i) the codec "feels efficient **AND fun**" — *fun* is a
  real dimension of the joint process, not decoration (it likely sustains the collaboration + the effort BR puts in).
  (ii) **BR has the same fear about the eventual CF5 model switch** (`research/029` cross-model) — the anxiety
  generalizes from *context* reset to *model* swap. That is sharper: a model switch changes the substrate-*reader*
  (capabilities, tokenization, style), so more of "the way we speak" is at risk than in a same-model restart. The
  fresh-restart experiment (`047`) is the same-model lower-bound; a CF5 re-run of the same battery would measure the
  cross-model delta on top. Human introspection on the joint process, logged.
- **Ceiling-confound: a self-test the agent ACES is ambiguous (2026-07-06, BR: "WR data on your last pondering").** A
  sharpening of the self-Q&A discipline above. When an agent **designs, answers, AND scores** its own probe battery, a
  **near-perfect result cannot self-certify quality** — it is equally consistent with "the agent/substrate is genuinely
  good" and with "the test lacks the sensitivity to detect failure." A **ceiling has no discriminating power.** This is
  the **corroboration-asymmetry at the INSTRUMENT level** (not just the answer level): the loop is triply
  self-referential (author + subject + scorer), so a pass proves the test *couldn't fail*, not that the thing is good.
  **Corrective (generalizes beyond this study):** a self-assessment is informative only if it can **produce variance** —
  include probes hard enough to fail, and a **known-low condition it is expected to score lower** (a discriminant /
  negative control); an instrument that can only pass is not a measurement. This is exactly why the **objective
  ground-truth key** and **BR's member-check** matter: they break the self-referential loop from *outside*. It sharpens
  the earlier self-Q&A caveat — self-Q&A that only *confirms* is theatre; self-Q&A that can *detect its own failure* is
  measurement. Surfaced by pilot-1's 16/16 ceiling (`research/047`). Echt applied to instruments: a reassuring self-test
  result may be an artifact of an insensitive test, and the agent must say so.
- **Imprecise / not-fully-known words as a misdirection risk (BR 2026-07-06).** BR (second-language English, self-noted
  "bad at English") sometimes reaches for a word he doesn't fully know (e.g. "pondering" — which happened to fit).
  Risk: a word that is **wrong but plausible in context** could steer the agent to a **literal misread it acts on
  confidently.** **Why it's usually low-risk:** the agent reads for **intent, not the literal token** — the whole
  terse-precise codec + `OK?`/`why:` cues exist to carry intent through imprecise wording, and context disambiguates
  most cases. **Real failure mode + corrective:** for a **consequential** action, **reflect the intent back before
  executing** (the `OK?` move), never silently run a literal read; for low-stakes, decode-and-proceed and let BR
  `edit:` it. The **not-dead-proof** reflex catches a word that pushes the agent somewhere odd (halt + flag). **The
  burden is on the agent to confirm, not on BR to be precise** — BR may also describe intent plainly or write
  **Swedish** (agent reads it). Sibling of the input-channel metadata-loss + `OK?` entries; a second-language
  transparency property of this corpus (METHODOLOGY.md language-for-transparency).
- **Worked micro-example: three mechanisms converged + self-corrected on one small question (2026-07-06, BR flagged).**
  On the trivial token-limit question ("does Fable-0% give free headroom?"), the whole methodology ran in miniature:
  (1) the **token-usage dance** (BR pastes the figure → agent analyses); (2) the **echt / member-check loop** — BR's
  intuition ("all-models must cap Fable too") **challenged the agent's confident slip** ("Fable = a free fresh
  bucket"), the agent **echt-corrected** rather than defended, and an **external authoritative source** (Claude Max
  docs via a `claude-code-guide` sub-agent) **adjudicated → confirmed BR**; (3) **triangulation** — human intuition +
  agent reasoning + docs all converged. The value: a clean instance of **behaviour/artifacts adjudicate, introspection
  only proposes** (METHODOLOGY.md §5) on a *verifiable* question — the loop that keeps self-Q&A / introspection honest,
  shown working at small scale. The human-as-skeptical-member-checker caught a confident-but-wrong agent claim in under
  a minute; the fix was **external verification, not mutual reassurance.**
