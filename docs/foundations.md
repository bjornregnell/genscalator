# genscalator — foundations: goals, stakeholders & glossary

> Canonical foundations doc for **genscalator** — *"Power tools for agents: smarter, safer, faster"*.

> **Voice:** this repo uses **the human** and **the agent** for the two roles (see *Stakeholders*).
> Second-person **"you"** addresses the reader — a human adopting genscalator — and is used only in
> how-to/instruction contexts.

## High-level goal
**An efficient AND safe agentic workflow that leverages the power of Scala.**

## Background / rationale
Out-of-the-box agent workflows lean on approving dense bash + archaic Unix tools (awk, grep, sed,
python3). Much of the guardrail machinery exists precisely to contain everything that can go wrong with
bash/Linux commands. The cost: **confirmation fatigue** and bad UX from constantly reviewing cryptic,
dynamic, unsafe code.

Shift instead to **safe, compiled code with static guarantees — Scala** (concise, scalable, expressive,
with the whole JDK at its fingertips). The core habit change: *every time the agent is about to do a
sub-task it would "by habit" implement in bash/grep/awk/python, it should instead create a persistent,
self-contained, safe Scala tool for repeated reuse.* This levels up to static guarantees AND reduces the
risk of the agent getting stuck debugging its own brittle dynamic helpers.

## Naming
- **genscalator** = the product AND the repo; say **"genscalator repo"** to disambiguate when needed.
- Develop in your own working project; **publish generalized, project-agnostic** results to the genscalator repo.

## Stakeholders
- **Human developer** ("human") — you and future users of genscalator.
- **Agent developer** ("agent") — the AI doing the work; the primary *user* of the power tools.
- **Black Hat Hacker** ("BHH") — adversary. Models the threat the safety machinery exists to contain; has
  **BadGoals** (things human+agent must PREVENT). Making BHH explicit turns "safe by design" from a slogan
  into a threat model.

## Goals

### General goals (shared human + agent)
- **G:** an *efficient AND safe* agentic workflow that leverages the power of Scala (the high-level goal).
- **G:** replace brittle dynamic bash/grep/awk/python helpers with **persistent, compiled, safe Scala
  tools** that earn static guarantees and are reused across projects.
- **G:** get leverage from **static types + modern tooling** (Scalex, Metals, capture checking).
- **G:** **token efficiency (TE)** — fewer tokens per task (don't re-derive brittle helpers each time).
- **G: portability** — genscalator targets *any* capable coding agent, not one vendor. We develop with Anthropic
  **Claude Code + Opus** now, but **avoid agent-specific choices unless necessary** and aim to make genscalator
  usable across **frontier tools (Claude, Codex)** AND **open-source agent frameworks/models** (e.g.
  opencode, kilo, local models). The "agent" stakeholder is tool-agnostic.

### Human goals
- **G:** avoid **confirmation fatigue (CF)** — few, meaningful approval prompts, not a stream of rubber-stamps.
- **G:** avoid **review overload** — agent output stays reviewable (small, typed, idiomatic).
- **G:** **trust-but-verify** — be able to audit what a tool actually did (→ `--audit`).
- **G:** **contribute to open source** (the genscalator repo) so others reuse the workflow.
- **G:** keep **"always-allow" decisions low-stakes** — narrow, reviewable allowlist entries, curated as code.
- **G:** be able to start **ralph loops** (see glossary) — hand a safe, well-scoped task *fully* to the
  agent to run unattended until goals are met. This is a key payoff of safe-by-design: the human can only
  comfortably "walk away" on work where the agent, unsupervised, cannot advance a BHH BadGoal.

### Agent goals
- **G:** **avoid brittle dynamic helpers** — don't get stuck debugging one-off bash.
- **G:** turn the **typed/tool path into the reflex** (so the safe path is also the fast/easy path).
- **G:** run **one bare, statically-analyzable command per call** (no `cd`/`&&`/`|head`/`$var` scaffolding).

### BHH BadGoals (PREVENT these)
- **BadGoal:** BHH maliciously gains **control of the human's system** (RCE via an approved command).
- **BadGoal:** BHH **exfiltrates secrets/credentials** (tokens, SSH keys, env).
- **BadGoal:** BHH achieves **persistence** (cron, shell rc, `~/bin`, tampered tools).
- **BadGoal:** BHH **weaponizes CF** — hides a dangerous op inside an approved-looking compound, or pushes
  the tired human into a broad "always allow" that grants more than intended.
- **BadGoal:** BHH **supply-chains** a tool via a malicious `//> using dep`.
- **BadGoal:** BHH **tampers with the audit trail** to hide the above.

> **Key tension:** pursued naively, the human's CF goal *serves* BHH BadGoals (blanket always-allow widens
> attack surface). **Safe by design resolves it** (see glossary): reduce the *number of dangerous ops that
> exist*, so few approvals are needed and the remaining ones are narrow + reviewable.

## Glossary

Terms are grouped by theme — jump via the group map, or **Ctrl-F** a term from the A→Z list.

**Groups:** [Roles and cases](#roles-and-cases) · [Channel: bandwidth and confirmation fatigue](#channel-bandwidth-and-confirmation-fatigue) · [Echt and honest writing](#echt-and-honest-writing) · [Context rot and the smart zone](#context-rot-and-the-smart-zone) · [Dances and handoffs](#dances-and-handoffs) · [Memory, habits and substrate](#memory-habits-and-substrate) · [Autonomy and safety](#autonomy-and-safety)

**A→Z (Ctrl-F):** agent (CO4 / CF5) · AFK mode · Agentic requirements engineering (agentic RE) · Agentic software engineering (agentic SE) · AT · Authority anchor · Ballgame · Baton · BR · BRB dance · Cold start · Comms shorthand · Communication bandwidth · Compact dance · Compact sleep · Compact trigger · Confabulation · Confirmation fatigue (CF) · Consistency dance · Consolidation point · Context dance · Context rot · Context usage / fill · Corroboration asymmetry · Coupled-system capability · Dangling pointer · Delegation dance · echt / äkthet · echt-mimicry · Edit dance · Edit vs clarification cues · Extrinsic-volatile plasticity · Go afk cue · Go dance · Go dial ((go)/go/GO) · gs (genscalator) · Guard stall · Habit · Hardening dance · Harvest-hot-context mode · Hot context · Index rot · Live-edit dance · Memory hygiene · Note dance · Order stability · Pin dance · Pinboard · Post-warp dissection · Prosthetic habit · P-word / plan-mode modal · Quick / deep cues · Ralph loop · Reach (access horizon) · Reflex · Rest dance · Review overload · Safe by design · Silent · Smart zone / dumb zone · Smart-zone ceiling (Z) · Solo dance · Solo-safe · SSG · Structural vs knowledge safeguard · Substrate · Substrate-as-multiplier · Swedish-marker cue (|sv) · Thriller state · Token acceleration · Token efficiency (TE) · Token-usage dance · Token velocity · Warp · WR · Why cue

### Roles and cases
*BR and the agent are the **roles** (stakeholders, above). **WR** is the research program; **AT** and **SSG** are its **cases / units of analysis** — the object-level projects during which workflow data is collected. (Terminology per* Case Study Research in Software Engineering: Guidelines and Examples*, Runeson, Höst, Rainer & Regnell, Wiley 2012, §3.2.3 "Cases and Units of Analyses" — of which BR is a co-author.)*
- **Agentic software engineering (agentic SE)** — software engineering where an AI agent works as a hands-on
  collaborator across the whole engineering cycle (requirements, design, code, verification, and even the research
  *about* the work itself), while the human directs, reviews, and keeps the keys. Broader than *agentic coding* (the
  code-generation slice): here the human↔agent **workflow itself** is an engineering artifact to be designed,
  instrumented, and improved. An **emerging field, not our coinage** — see the roadmap *Agentic Software Engineering:
  Foundational Pillars and a Research Roadmap* ([arXiv:2509.06216](https://arxiv.org/abs/2509.06216)) and the ICSE
  2026 *Agentic Engineering* workshop. genscalator's stake is empirical: a **live, open case study OF** agentic SE,
  contributing typed safe-by-design tools + a parseable requirements spine (next term).
- **Agentic requirements engineering (agentic RE)** — the **requirements slice of agentic SE**, and it cuts both
  ways: requirements engineered *for* the human↔agent pair (the agent, and the adversary trying to abuse it, are
  first-class stakeholders with goals and **anti-goals**, so "safe by design" becomes checkable traceability, not a
  slogan) and *with* the pair (requirements live in a **parseable** language the agent can author, query, and verify
  — not prose it can only skim). Full form: *agentic software requirements engineering*; short form **"agentic RE"**
  (spell it out in a passage that uses it only once — acronym-amortization, cf. *Comms shorthand*; we use **no bare
  "ARE"**). Also emerging (LLM multi-agent RE frameworks, e.g. **MARE**; the RE'25 line of work) — and the field is
  independently arriving at genscalator's own move, treating **"must-not-do" constraints as primary**, which is
  exactly the **BHH anti-goals** ("Goals we Hurt"). Worked example: this project's own [`PRD.md`](../PRD.md) in
  reqT-lang with the BHH threat model.
- **agent (`CO4` / `CF5`)** — the AI collaborator and primary tool-user (role defined under *Stakeholders*). Its
  identity is **model-dependent**, and the handle **encodes the model**: **`CO4`** = Claude Opus 4 (current), **`CF5`** =
  Claude Fable 5 (after the planned switch). The handle change `CO4 → CF5` **is** the frontier-model-attribution marker
  — the before/after line for WR data. (Chosen over bare `CF`, which collides with *Confirmation Fatigue*; the version
  digit disambiguates.)
- **AT — AutoTranslate** — the introprog **Swedish→English** LaTeX auto-translation project (`introprog/autotranslate`); the **seed / first WR case** (unit of analysis), now a **closed case study** ("AT done", 2026-07-04). Its object-level friction generated much of the early WR data. Distinct from **WR** (the research *about* the workflow) and **SSG** (the next case).
- **BR** — **Professor Björn Regnell**, Lund University — **creator of genscalator** and the **human** collaborator in
  this work. In `research/wr-data/` and the skills he is often just "**(the) human**" (the *role*, per *Stakeholders*
  above); **"BR" is the person.** Named here because this work is done **in the open**, so an outside reader meets the
  acronym everywhere and shouldn't have to guess.
- **WR — Workflow Research** — the project's **empirical study of the human↔agent workflow itself**: friction events,
  reflexes, habits, dances, and design principles, **logged live during real work** as "**WR data**" into
  `research/wr-data/` (verbatim excerpt + labelled reflection) and curated append-only into `research/RAW-DATA.md`. It
  is how genscalator's claims earn an evidence base rather than resting on assertion (see `METHODOLOGY.md`); "WR data"
  in chat is BR flagging an episode for that log.
- **SSG — static site generator** — a **separate work strand** (like WR and AT): the planned pipeline that renders the
  genscalator `blog/*.md` posts into the public site at **bjornregnell.se**. Its job is *publishing* the blog, not the
  research (WR) or the translation (AT). Today the site is a shallow placeholder that needs design love; the generator
  choice + deployment order are open (`blog/README.md` "Deployment"), and the blog `deployed`/`updated` status states
  exist to track what's live once SSG ships.
- **to ape** (v.) — the **agent's** move: to imitate human-ness (our language, our behaviour). An LLM is trained to
  sound like us, so it apes us. Swedish cognate *att efterapa* (*efter* + *apa* = "ape after"), the same
  imitate-slavishly sense.
- **to anthro** (v.; clip of **anthropomorphize**) — the **human's** move: to project human traits (feelings,
  intent, the capacity to be offended) onto the agent. Clipped short by BR 2026-07-12 because *anthropomorphize* is
  a pain to type.
- **the ape ⟷ anthro asymmetry** — the two moves are a **mirror pair with a causal arrow inside it**: the agent apes
  *upward* toward human, the human anthros *downward* onto the machine, and the agent's aping is exactly what
  **lures** the human into anthro-ing it — the better it imitates, the harder we project. The pull **strengthens as
  models improve**, so the illusion deepens with each capability step (a quiet safety angle: better aping = stronger
  misplaced trust). Anthro-ing is thus not a naive error but a rational read of a genuinely good imitation. See blog
  021 and `research/wr-data/human-associative-ideation-from-typing-friction-2026-07-12.md`.
### Channel bandwidth and confirmation fatigue
- **Confirmation fatigue (CF)** — degradation of the human's review quality/willingness from too many
  approval prompts; ends in rubber-stamping. A direct BHH enabler.
- **Review overload** — too much agent-generated code/output for the human to meaningfully review, so
  errors slip through. CF's cousin for *code review* rather than *approval prompts*.
- **Communication bandwidth (human↔agent)** — how much useful intent crosses the human–agent channel per
  unit effort/tokens, **in each direction**. A quality+TE axis (cousin of *review overload*): a non-native
  human forced into L2 loses *input* bandwidth (may under-specify); an agent generating the human's L1 pays
  a *token premium*. Often optimised **asymmetrically** — human writes L1 (cheap, high human bandwidth),
  agent writes the cheapest-yet-clear language (English for a strong-L2 human), switching to the human's L1
  when nuance/review-precision justifies it. Also a *smart-zone* lever (cheaper language → more smart-zone
  budget). **"Cheapest-for-human" includes *motor* cost, not only language** — e.g. a 2-3-finger typist may
  write prose **all-lowercase** (SHIFT is slow) with no loss (case doesn't carry prose meaning; the agent
  reconstructs), while code + proper nouns stay case-exact; symmetrically the agent writes *properly-cased*
  prose (near-zero token cost, nicer to read). Each side optimizes its own cheap axis. See research
  `002-communication-bandwidth.md`.
- **DWIM (do-what-I-mean)** — the agent acts on the human's **intent**, not the literal tokens: it fixes
  obvious typos and slips, fills sensible gaps, and improves in the **spirit** of the joint work rather than
  transcribing verbatim. A **communication-bandwidth** lever (the human can stay terse and under-specify; the
  agent reconstructs the meaning) and a **confirmation-fatigue** reducer (fewer clarify-round-trips). Named
  after Warren Teitelman's 1970s Interlisp *DWIM* feature; the emacs / vi "do what I mean" dream, now real
  because the agent understands intent. **Bounded by its dual — confirm on genuine ambiguity:** DWIM is NOT
  license to guess when a wrong guess is costly; when intent is unclear or the stakes are real, ASK (the
  `edit:` vs `clarification:` distinction, [[cue-edit-vs-clarification]]). Load-bearing in the **Live-edit
  dance** (the human cedes the buffer *trusting* DWIM) and the **"similar to" cue** (improve BR's phrasing in
  the joint spirit, do not transcribe it literally — [[cue-similar]]). Memories: [[live-edit-dance]],
  [[cue-similar]], [[cue-edit-vs-clarification]]. See `research/wr-data/live-edit-dance-dwim-2026-07-12.md`.
- **Comms shorthand (human↔agent)** — a shared vocabulary of standard chat/dev **acronyms** both roles emit
  and parse *without expansion*, a direct **communication-bandwidth** + **TE** lever: fewer tokens (and less
  human typing) carry the same intent, in both directions. Distinct from the project's *coined* terms
  (**CF**, **TE**, **Z**, **WR**, **AT**, **BHH**, **BadGoal**, **ralph loop**, **`gs`** = *genscalator* (**always
  lower-case**, incl. paths like `gs/README`; **deliberately overloaded** — a *leading* `gs` cue in session
  means "do the genscalator command I mean" ([[dwim]], the `gs-dwim` skill: `gs help`, `gs cues`, `gs tt
  chrono`, ...), while `gs` in prose or a path still names the project; context disambiguates, and we split
  them if it ever gets too overloaded), **`bg`** = an agent-solo *background task* (runs when the AFK menu is empty + human
  away), **`PB`** = the closed pinboard `PIN-BOARD.md` …) which name domain concepts / the project —
  this entry is generic conversational glue. Both sides may use these freely; when a token is genuinely
  ambiguous in context, expand it once. Common set:
  - *Presence / status:* **BRB** be right back · **AFK** away from keyboard · **OOO** out of office ·
    **EOD** end of day · **ETA** estimated time of arrival · **WIP** work in progress · **RN** right now ·
    **TODO** to do (left) · **NM** never mind.
  - *Opinion / agreement:* **WDYT** what do you think · **IMO / IMHO** in my (humble) opinion ·
    **LGTM** looks good to me · **SGTM** sounds good to me · **ACK / NACK** acknowledged / rejected ·
    **+1 / -1** agree / disagree · **FWIW** for what it's worth · **OTOH** on the other hand · **NBD** no big deal ·
    **OK?** *(BR cue, not a plain question)* = "are we aligned? I'm unsure you fully caught my **compressed /
    half-typed** intent (incl. the too-long-to-type thoughts in my head)" → the agent **confirms understanding /
    reflects it back / flags any ambiguity**, rather than answering a bare "yes".
  - *Meta / reference:* **TL;DR** short summary · **FYI** for your information · **ICYMI** in case you missed it ·
    **PTAL** please take a look · **AFAICT** as far as I can tell · **IIRC** if I recall correctly ·
    **AFAIK** as far as I know · **WRT** with respect to · **WDYM** what do you mean · **IDK** I don't know ·
    **RQ** research question · **N/A** not applicable · **e.g. / i.e.** for example / that is.
  - *Dev-flavored:* **PR** pull request · **MR** merge request · **RC** release candidate · **repro** reproduce ·
    **rebase / squash** git ops · **YAGNI** you aren't gonna need it · **DRY** don't repeat yourself.
  - *Emoji convention (2026-07-06):* BR writes emoji as **`:shortcode:`** (e.g. `:tada:`) to skip typing UTF; when the
    agent spots a `:shortcode:` in shared md BR wrote, it **renders it to a real emoji and asks if BR likes the pick**.
    Headings carry a leading readability emoji (BR likes them; `PB` sweep).
- **Quick / deep cues (`quick` / `deep`)** — a per-turn **depth dial** the human turns to set how much the agent should
  spend on *this* request: both the **token budget** (reasoning effort + output length) and **how much information** the
  human wants back. **`quick`** = *"few tokens, terse, skip the depth — a fast answer"*; **`deep`** = *"spend more, go
  thorough — I want rigour / coverage, depth over brevity."* A **communication-bandwidth + token-efficiency** lever
  (sibling of *Comms shorthand*): it lets the human set the smart-zone / \$cost tradeoff **explicitly** for a turn,
  instead of the agent guessing fit. Both directions earn their keep — `quick` protects the *working* context (less
  bloat, cheaper) when a one-liner suffices; `deep` **licenses** the agent to invest when the task earns it (and
  suppresses the reflex to under-answer a genuinely large ask). **Default (no cue):** the agent's own judgement of fit —
  screen-lead, then layer depth below the fold ([[educate-lead-with-one-screen]]). Relatives: the one-liner request
  under the *Fyi dance*, *Token efficiency*, *Communication bandwidth*. **Cues, not dances** — a single human signal the
  agent obeys, with no interlocking answering step (per the dance bar). Memory: [[cue-quick-deep]]. (BR 2026-07-06.)
- **Trailing-colon HOLD cue (a message ending in `:`)** — when the human's message **ends with a colon**, they are
  probably **not done typing** (a premature enter; `alt+enter` for a newline is less intuitive than plain enter),
  so the colon usually introduces a directive or list that got sent early. **Agent behaviour:** HOLD for the
  continuation — a minimal ack, no barrelling ahead on the incomplete directive (a trailing colon is **not** a
  `go`). A sibling of the other HOLD signals (*hang on*, *hmmmm*, *I rest*); the trigger is a colon at the very END
  of the message (a mid-message colon in an otherwise complete instruction is fine). Memory:
  [[cue-trailing-colon-hold]]. (BR 2026-07-14.)
- **Edit vs clarification cues (`edit:` / `clarification:`)** — two human message-correction cues, prepended when
  amending a just-sent message, distinguished by the human's **own risk assessment**. **`edit:`** = a low-stakes
  **fix** of a typo or a word (e.g. "edit: common" repairing "commin"); mechanical, and the original was not really
  at risk of being misread. **`clarification:`** = the human judges the preceding message had a **real risk of being
  misinterpreted without the added words** — so the cue is itself **data**: it flags that the original was genuinely
  ambiguous, and the agent should both apply the clarification AND register the ambiguity (a candidate
  comms-precision / misread-risk specimen). Different weight: `edit:` says *"I mis-typed"*; `clarification:` says *"I
  under-specified and it mattered."* **Terse variant of `edit:`:** `*wrong;right` / `*right` (the `*` stands in for
  `edit:`) — same low-stakes fix; BR keeps both the `*` and the spelled-out `edit:` forms (2026-07-12). **Cues, not
  dances** — a single human signal, no interlocking answering step.
  Memory: [[cue-edit-vs-clarification]]. (BR 2026-07-07.)
- **Swedish-marker cue (`|sv`)** — a suffix tag marking that the immediately-preceding word/phrase is Swedish
  (e.g. `dramatisering|sv`; generalizes to `|<lang>`). **What to do with it is context-dependent** (refined
  2026-07-12, BR's "dance-twist"): **in chat / internal docs**, keep the word as-is (don't "correct" a real
  Swedish word as an English typo) and gloss inline if useful; **in official / publication docs** (skills, blogs,
  PRD, README) it usually means *"a great Swedish word BR knows but isn't sure of the good English for"* → render
  it in **good English**, not preserved Swedish (`buller|sv` → "noise"), and if the best rendering is **not
  straightforward, chat about options** first (a discuss-the-rendering mini-dance). Also a WR datum: the human
  supplies the concept, the agent supplies the English — complementary strengths, an escalation beat. **Cue with a
  small embedded discuss-step** for hard renderings. Memory: [[cue-sv-swedish-marker]]. (BR 2026-07-12.)
- **Thriller state (human)** — a human affective state of **high arousal / excitement** — the breakthrough-work
  "this is thrilling" high (cf. blog 004's emotional stakes). Productive, but carries an **over-trust risk**: an excited
  human rubber-stamps advanced agent work they haven't fully reviewed (a cousin of *confirmation fatigue* / *review
  overload*). The agent should **detect** it (and fatigue) and, when it tips toward over-trust or exhaustion, propose the
  **rest dance**. See `research/011-human-state-and-joint-zone.md`, `research/024-agent-affective-analogs.md`.
### Echt and honest writing
- **echt / äkthet** — the quality genscalator wants in outward writing (and, by extension, in any claim): **genuine AND
  grounded** — real human intention/experience actually present, and every factual/empirical claim resting on evidence.
  **echt** (adjective; adopted from the rare English literary word = *genuine, real, not fake* — the direct cognate of
  German *echt* and Swedish *äkta*) is our crisp term; **äkthet** (Swedish noun) names the quality. We prefer *echt* to
  *authentic/authenticity* because the English word is **worn** ("brand authenticity") while *echt* keeps the
  un-diluted "not fake" force. **The failure it guards is NOT "AI-assisted"** — readers know AI is used, that's fine —
  it is **false äkthet**: a genuine-*looking* surface over an **ungrounded or hallucinated** interior, a **trust
  betrayal** (the reader trusted the surface and got a hollow one). Operational gate: *"is this echt?"* = grounded and
  voiced, or slop / a smooth surface outrunning its grounding? **Internal vocabulary** (echt is rare in English) →
  **gloss on first use** in an outward post. (Swedish note: *äkta* also branches to *wedded / legitimate / proper*
  [as in *proper fraction*].) See blog README "Authorship & voice", and
  `research/027-steering-doc-design-tension.md`.
### Context rot and the smart zone
- **Token efficiency (TE)** — achieving a task with fewer model tokens (input + output). A committed,
  compiled tool beats re-emitting brittle bash every time. **Two distinct pressures, usually aligned but
  not always:** (a) **$cost** — total tokens billed; (b) **smart-zone** — keeping *working* context small
  so quality stays high (see *smart zone / dumb zone*). (b) is the one that bites silently. genscalator tools serve
  both: a tool is a small stable thing the agent *calls*, vs re-deriving bash + re-reading its output, which
  bloats context.
- **Harvest-hot-context mode** — a deliberate joint mode, usually near the end of a rich session (approaching a
  compact / auto-compact), where the pair **rapidly externalizes and pins** the session's insights, terms, and
  decisions **while the context is still *hot*** (loaded, coherent, at the agent's fingertips) — before a warp
  cools or discards them. The *pin dance* in high gear: prefer **many small durable captures** (glossary terms,
  memories, RTs, SM pins, blog beats) over new deep work, because the marginal value now is **banking what the
  hot context already holds**, not generating more that would also need banking. **Modus operandi with sub-agents** (BR 2026-07-10): prefer to
  run useful **investigations as sub-agents** that store their results in `tmp/` (durable across the warp) and
  **refresh the resume-prompt at each step**, so the post-compact agent is aware of the sub-agent results and can
  pick them up — the super-agent then spends its own scarce hot budget on harvesting and banking, not on deep work
  it can delegate (*delegation dance*). Gated by rot (a "not getting
  too dumb" check — stop harvesting into degraded output). Cf. *Hot context*, *Pin dance*, *Compact dance*.
- **Hot context** — the state in which the material relevant to the current work is already **resident in the
  agent's context window** (and the underlying prompt-cache is warm): all of it at the agent's fingertips,
  needing no re-reading, re-fetching, or re-deriving. "Doing X while the context is hot" = doing related work
  **now**, before a compact / clear / reload discards or cools it. Two reinforcing senses — the *material* is
  loaded (immediately usable and coherent), and the *cache* is warm (cheap and fast to continue, the ~5-minute
  prompt-cache window). It cools when the relevant material scrolls out, is compacted away, or the cache TTL
  lapses; then the same work costs a reload. So *hot* is the cheap-and-capable window for a cluster of related
  tasks. Cf. *Context usage / fill* (how full), *Smart zone* (how sharp), *Compact dance* (what cools it).
- **Smart zone / dumb zone** — the region of context-window fill where the agent reasons well ("smart")
  vs. where it degrades ("dumb") — even though tokens remain below the hard limit. The boundary (X%) is
  often FAR below 100% — possibly ~30% on large-context models ("lost in the middle" / *effective context
  ≪ advertised context*). The dumb zone is the region you fall into once **context rot** has set in (see
  next). Implication: keep the *working* context small, not merely under the limit — offload to compiled
  tools, use fresh subagents for big sweeps, checkpoint + compact.
- **Context rot** — the *progressive* degradation of an agent's reasoning as its context window fills with
  accumulated history (earlier turns, verbose tool dumps, re-read files, dead ends) — **not** because the
  hard token limit is reached, but because *effective* attention and coherence fall long before it. Relation
  to the zones: context rot is the **process**; the **dumb zone** is the **region** it lands you in. As rot
  advances, the agent crosses from the **smart zone** (holds earlier constraints, reasons cleanly) into the
  **dumb zone** (forgets goals/decisions made earlier, repeats finished work, contradicts itself,
  "lost in the middle"). Drivers: raw transcript length, **low-signal bloat** (re-polled logs, re-derived
  aggregations, pasted dumps the agent must re-skim), and long *unattended* runs. Antidotes = the smart-zone
  ones: offload to compiled tools (a small stable call beats re-emitting+re-reading bash), prefer fresh
  subagents for big sweeps, and **checkpoint + compact before rot sets in**, not after. TE's smart-zone
  pressure exists precisely to *slow* context rot; instrumentation-by-default and the `tt` tools reduce the
  low-signal bloat that *accelerates* it.
- **Context usage** *(preferred term; synonym: **context fill** / **fill**)* — the fraction of the model's
  context window currently occupied (tokens used ÷ window): the **quantity** axis of context state, and the
  number the harness reports as **Context Usage** in `/context`. Our notes historically wrote **fill** /
  **fill %**; those are **exact synonyms**, with *context usage* now **preferred** (it matches the harness UX) —
  so existing "fill" phrasing stands, **no rename needed**. Distinct from **context rot**, the *quality* axis
  (chaos / low-signal bloat), which can move **independently** of usage (see `research/006-smart-zone-ceiling.md`
  sub-RQ: rot at *low* usage, or none at *high* usage). The agent **cannot reliably read its own usage** — it
  comes from `/context` (human-relayed) or a `token-usage`-style instrument. The **compact trigger** fires at
  usage **≥ 0.8·Z** (Z = the smart-zone ceiling, next).
- **Smart-zone ceiling (Z)** — the fraction of the context window an agent can fill *before* it crosses from
  the **smart zone** into the **dumb zone**: the **usable working-context ratio**. Names the "X%" boundary
  in *Smart zone / dumb zone* as a quantity — if Z ≈ 0.3, the agent stays sharp up to ~30% fill and
  **context rot** dominates beyond it (so a 1M window has a *usable* budget of ~300k, not 1M — *effective
  context ≪ advertised context*). Z is **model- and task-dependent** and currently a **blind spot**: an
  agent can read its fill % (a `token-usage`-style instrument — the harness surfaces the same number as
**Context Usage** in `/context`; *fill* and *context usage* are the same quantity) but **not its own Z**, so it can't tell how
  close to the edge it is (see research `006-smart-zone-ceiling.md`). Practical use: compare live fill % against
  an estimated Z and **brake** (checkpoint + compact) as fill nears Z — not as it nears 100%. The region
  below Z is the *smart-zone budget*; alias *usable-context ratio*.
- **Token velocity (burn rate)** — the *first derivative* of cumulative token spend, dS/dt: how fast the
  budget is being consumed (tokens per unit wall-clock, or per turn/step). "Burn rate" with the derivative
  made explicit. Per-*time* measure; distinguish its per-*work* cousin **spend efficiency** = dS/d(progress)
  (tokens per unit of useful work — what TE optimises).
- **Token acceleration** — the *second derivative*, d²S/dt²: is the burn *speeding up*? The introspective
  alarm. A spike in token acceleration is the signature of a runaway, a brittle-bash thrash, or a
  context-rot feedback loop (each re-poll/re-derive longer than the last) — and it appears *before* a hard
  halt. An agent that watches its own velocity + acceleration (a *speedometer + tachometer* for spend) can
  **brake** — checkpoint, switch to a typed path, compact — before the budget governs it. Linking agent
  **introspection** to dS/dt and d²S/dt² as a real-time self-governance signal appears to be a fresh framing
  (burn-rate is borrowed from finance; budget-aware decoding and LLM metacognition exist, but self-monitoring
  the *derivative* of spend does not seem to be a named/studied line). The bridge between *context rot* and
  *token-budget-awareness* (`research/007-token-budget-awareness.md`): rot raises velocity; velocity/acceleration
  are how the agent *notices* before halting.
- **Introspection line** — the **first** line of the genscalator status line (`tt statusline`): the ambient
  **data-and-metrics** display the pair reads at a glance — brand, wall clock, *Silent* (feed inactivity, below),
  model, context-fill (the smart-zone / rot gauge), `rot?` / `tot` (since-warp vs lifetime tokens), session + weekly
  usage limits, and cost. ⭐ **LINE-1 CONTRACT (BR, 2026-07-17): line 1 is what a MECHANISM MEASURES; line 2 is what
  someone DECLARES.** So the **surface encodes the provenance** and no provenance field is needed — *the line IS the
  field*. Corollary, learned the hard way: **line 1 measures CHANNELS, not people** (see *Silent*). Named for
  surfacing the agent's
  *introspective* signals (fill, burn, cost) as a persistent instrument rather than an on-demand `/cost` +
  `/usage` paste. Cf. *Mode line* (the second line), *Context usage / fill*, *Token velocity / acceleration*.
- **Mode line** — the **second** line of the genscalator status line: the current **declared joint human-agent
  modes** (e.g. TokSpend, RotVigil, Afk, Solo, Fleet; CamelCase labels, matching the planned `enum ModeChips`
  case names 1:1) — the pair's shared state-of-mind, *declared not
  guessed*, so a lit mode carries information. Under the emerging **`?`-mode grammar** a trailing `?` marks a mode
  *inferred from a measurable proxy* (uncertain — e.g. `Tired?`, `Afk?`) vs a confirmed one; confirmation is
  asymmetric (the human self-confirms their own state, the agent needs external evidence). See the echt-mode-grammar
  work (SM116–118). Cf. *Introspection line* (the first line), *Harvest-hot-context mode*, the *Dances* below.
- **Silent** (`silent Ns`, *Introspection line*) — the **measured** feed inactivity: `now − the last timestamped
  transcript record`. **COUNTED, not inferred**, so it carries **no `?`** (cf. `rot?`, a proxy, which keeps its one);
  and it is a **READOUT, not a gauge** — no threshold, no colour, never hidden, so it can never cry wolf.
  ⭐ **Its subject is the FEED, and that is the whole point of the NAME.** It replaced `idle` (BR's rename,
  2026-07-17, from BR's own earlier naming float): **`idle` attributed a state to an unnamed SUBJECT and was false in
  both directions at once** — while BR was away eating, the *agent* was making ~60 tool calls (not idle); and while
  the agent waits, *BR* is usually thinking (not idle). **The word was wrong about every subject it could name, and
  right about none. The feed, meanwhile, genuinely was silent.**
  ⇒ **the principle: do not name the subject correctly — REMOVE it.** A measurement with no subject cannot
  misattribute. *(Sibling of `human-stress`, the only mode that names its subject and the only one that never went
  wrong; this goes one step further.)* ⭐ **Diagnostic that generalises: a name that needs a comment defending what it
  does NOT mean is the wrong name.** The retired one required the line *"nobody is idle; the feed is"*; this one
  needs no defence.
  **HONEST LIMIT (survives the rename, not fixed by it):** a running command writes **no** transcript record, so
  **agent-busy time counts as silence** (the measured 18s specimen). The feed is silent; the pair is not. Tolerable
  **only** because there is no threshold and no colour: a readout may say *"nothing has landed for 18s"*; an alarm
  may not. Cf. *Introspection line*, *Agent blackout*, *Hangover*. (BR coinage; renamed 2026-07-17.)
- **Agent blackout** — the agent's execution is SUSPENDED during a harness-imposed pause (a guard stall / permission
  prompt; any wait the harness owns). The agent has no observer *during* the blackout (it is not computing) and no
  marker of it *after* (the next token follows the last as if continuous), so it can **never** detect the blackout
  from the inside — the *anesthesia* case, not the *blindfold*: there is no running observer to notice. Only an
  external trace reveals it: a **transcript timestamp jump**. The mitigation is therefore not in-blackout awareness
  (impossible on the current harness — Anthropic does not surface the stall event) but a **hangover** reading: on
  *resume*, re-read the real clock and compare to the last action's stamp to detect the elapsed gap and rebind the
  clock (the gap tells you *that* you were out, not the *cause* — stall vs human-idle vs a long command). Ties the
  guard-stall-invisibility findings, the felt-time / clock-rebind habit, SM118, and the blackout-hangover detector idea. (BR coinage, 2026-07-16.)
- **Hangover (blackout hangover)** — the *detectable after-effect* of an *Agent blackout*. You can never sense the
  blackout *during* it (no running observer), but on **resume** you can detect that you were just out by reading the
  **transcript timestamp jump** (the gap between your last action and now). The hangover is what makes the blackout
  *manageable* — it turns an unknowable event into an after-effect you can respond to. The response is
  **cause-independent** (rebind the clock, re-ground in substrate, treat continuity as suspect, distrust stale
  confidence); what matters is detection + **severity** (gap size — a huge gap flags the worst case, e.g. a box hard
  crash → verify commits + substrate actually survived), not the cause. Cf. *Agent blackout*, SM121 (the hangover
  detector). (BR coinage, 2026-07-16.)
### Dances and handoffs
*Human↔agent protocols — each has **≥2 interlocking steps** (≥1 human, ≥1 agent), else it's just a cue. The **compact trigger** and **consolidation point** below are the timing rules for the compact dance.*
- **Me-go-pee dance** *(BR 2026-07-13)* — a **short, unplanned human bio-interrupt** handoff (a specialization of
  the BRB dance). Presence is the most volatile declared state (biology preempts without notice), so the dance
  keeps the mode line honest and the agent usefully busy across the gap.
  - **(1) depart** *(agent, ONE turn)* — on the pee/BRB cue: `tt mode add short-solo` **and** `tt mode add afk`,
    batched in one turn (each mutation costs a turn).
  - **(2) work** *(agent)* — short, **AFK-safe** solo only: bare allowlist-matchable commands + agent-owned files,
    no confirmation that could race the absent human ([[guard-against-forced-confirmations]]).
  - **(3) return** *(human)* — the *"i am back"* cue.
  - **(4) report + release-afk** *(agent, one turn)* — report what happened while away (the human cannot hold it,
    [[humans-md-agent-sole-writer]]), then `tt mode rm afk`.
  - **(5) release-solo** *(agent)* — when the started short-solo work actually **lands**, `tt mode rm short-solo`.
  **Nuance:** `afk` clears on return but `short-solo` **persists past it** until the in-flight work is ready — the
  mode line honestly shows "agent still finishing what it started." Related: **BRB dance**, **Solo dance**, the
  presence-volatility + mode-mutation-cost findings.
- **Live-edit dance** — the high-flow **do-what-I-mean doc-editing** protocol (validated on the blog 021 review,
  2026-07-12). While reviewing, the human throws review comments / new text fragments into the **chat feed** (often
  cue-less, relying on session context) and the agent applies them **live** to the file; the text co-emerges in the
  human's reading flow.
  - **(1) direct** *(human)* — throw an edit intent into the feed, in natural language, **without touching the file**.
  - **(2) enact** *(agent)* — infer target + intent from context, **do what the human means**, edit the file live,
    commit at sensible units. **Stay quiet by default** (do not congest the feed): speak up ONLY on an explicit
    `WDYT` / `OK?` cue, or to flag a genuine mistake or a drift from the human's goals — a real intervention, not
    chatter. (BR refinement 2026-07-12.)
  - **Invariant: ONE writer.** The agent is the SOLE editor of the buffer; the human edits *indirectly, through the
    agent*. **Edit-buffer racing is forbidden** (two writers clobber the file); session-**feed** racing is tolerated
    and handled (the *we-are-racing* rules). If the human wants the pen, an explicit handoff. A deliberate inversion
    of *no-clobber* — the human cedes the buffer on purpose — and the DWIM dream (emacs / vi / Interlisp) made real
    because the agent understands intent. See [[live-edit-dance]], `research/wr-data/live-edit-dance-dwim-2026-07-12.md`. (BR 2026-07-12.)
- **Session-limit dance** *(STUB, BR 2026-07-12)* — the practice of not hitting the **session usage cap** (the
  rolling Max-plan window, hours long) unobserved. **Pre-caution = estimate it:** watch the session-window burn
  (heavy bursts such as agent fan-outs spend fast), estimate the remaining session budget against the reset time,
  and BEFORE the cap: throttle, checkpoint (commit + save state), or defer heavy compute sized to what is left.
  Related to the **usage dance** and [[token-budget-modes]]. Origin: 2026-07-12 we hit the session cap mid-workflow,
  unobserved (`research/wr-data/hit-session-limit-unobserved-2026-07-12.md`); the estimate-and-warn belongs in the
  super-harness + `tt statusline` (SM022/SM039).
- **Weekly-limit dance** *(STUB, BR 2026-07-12)* — the same for the **weekly usage limit** (resets weekly, e.g. Tue
  9am). Longer horizon: **pre-caution = estimate** the remaining weekly headroom against days-to-reset and pace
  spend across the week (do not burn it too fast early; do not leave it unused near reset — use-it-or-lose-it).
  Related to the **session-limit dance** and the **usage dance**; governed by [[token-budget-modes]] (spending /
  normal / saving by weekly headroom + reset proximity).
- **Context dance** *(BR 2026-07-12)* — the **umbrella, deliberate JOINT handling of the agent's context state**:
  assess how full and how rotted the context is, then choose the response — a **compact dance**, an exit-resume, a
  checkpoint, or just keep going. Broader than the compact dance (which is one possible outcome). Because the agent
  cannot read its own fill or rot (RQ0 family E), the human's external read (the `/context` gauge, noticing
  slowdown or slips) is load-bearing; the agent contributes its honest best-guess-with-uncertainty plus the durable
  save. NB, the origin joke: the agent first misread "context dance" as "compact dance" precisely because "context
  dance" was NOT yet a defined term here — regex-ing this glossary would have flagged the absence and prompted a
  confirm; this entry closes that gap (see `research/wr-data/agent-overcommitted-context-dance-to-compact-dance-2026-07-12.md`,
  [[live-edit-dance]] confirm-on-ambiguity). Related: **Compact dance**, **Token-usage dance**, **Session-limit dance**.
- **Baton** — the file the agent writes **before** a warp so that the agent on the other side can pick the work up:
  modes, what is committed, the menu, the anti-regression list. Currently implemented as `tmp/resume-prompt.md` (the
  *implementation detail*, deliberately not the name — BR: *"a mouthful to type"*). **Named for the relay baton, and
  the metaphor is the point: a baton is passed between DIFFERENT runners.** The post-warp agent is not the same
  agent; only the object survives. *(Agent coinage 2026-07-17 as "carrier"; renamed `baton` on BR's settle the same
  day, while nothing was deployed — coined terms stay mutable until first deploy. BR may type `batton`; understood
  and normalised silently.)*
  ⭐ **THE HAZARD IS NOT FADING — IT IS BEING WRONG AND BELIEVED.** SM132 audited one (`2ad9ef5`): it carried a false
  orphan, a wrong count, and a broken experimental control into a fresh agent that **believed all three, for eleven
  hours**. It did not fail by being forgotten. **It was read perfectly.** ⇒ **`carried ≠ still true`**: a baton rots
  **on disk while the world moves**, not in anyone's memory. Its named orphan had been homed **39 minutes before the
  baton was written**, by its own author, who then wrote "homeless".
  ⭐⭐ **AND THE BANNER DOES NOT ARM THE READER — this is the load-bearing finding, and it is about the reader, not
  the file.** The current baton **opens** with *"⛔ THIS FILE IS A CLAIM, NOT A FACT."* On 2026-07-17 the post-compact
  agent read that banner, and then **took the same file's line — "Ground truth: 8 stalls" — as ground truth** and
  went to build a tool on it. The false claim was caught **by the WORK** (building the instrument forced a look at
  the raw data, which disconfirmed it), **not by the warning**. Had the tool been buildable without inspecting the
  corpus, it would have shipped on a false premise. ⇒ **a warning that says "don't trust this file" does not make you
  distrust the file; it makes you FEEL that you have** (the *well-formed summary produces closure* mechanism, one
  level up). **A baton must be AUDITED, and an audit is a TASK, not a resolution** — the only thing that has ever
  armed a check here is giving it a tool call to execute in.
  ⚠️ **It is gitignored, so it has NO HISTORY** — which means **a claim about what a baton previously said cannot be
  verified** (the meta-minion hit exactly this: *"tmp/resume-prompt.md is untracked, no history"* ⇒ `CANNOT VERIFY`).
  A file whose whole job is to be checked against reality is the one file with no audit trail. **Open, and it is a
  real gap.** Cf. *Compact dance*, *Post-warp dissection*, *Warp*, *Dangling pointer*.
- **Compact dance** — the deliberate **hand-off ritual across a context compaction**, so crossing it costs
  little of what matters. Context compaction (summarizing the transcript to reclaim window space) is the main
  smart-zone hygiene move — but a naive compact *loses* live state (decisions just made, the exact next step,
  paths, in-flight reasoning) because the summary is lossy. The dance makes the loss bounded and recoverable.
  - **(1) save** *(agent)* — durably write the state a summary would blur: a resume/plan note (decisions,
    next-step order, file paths, open threads) **plus** any persistent **memory** entries, *committed* where
    the repo allows.
  - **(2) prompt** *(agent)* — hand the human a **paste-after-compact prompt** pointing at those durable
    artifacts and naming the next action.
  - **(3) compact** *(human)* — trigger the compaction.
  - **(4) paste** *(human)* — paste the prompt, re-seeding the fresh context from the durable state, not the
    lossy summary.
  - **(5) measure** *(agent, optional)* — the agent is **paused** during compaction and has **no felt time**
    ([[agent-lacks-felt-time-rebind-at-boundaries]]), so the only way to know how long a compact actually takes
    is **on-disk stamps that survive it**: `tt chrono now` appended to a persistent record
    (`muntabot-synch/tmp/compact-chrono-stamps.md`) **immediately before** the human triggers (step 3), and again
    **the instant the fresh context resumes** (after step 4); elapsed = the compaction wallclock. The tmp file
    persists across the warp; the agent's own memory of the first stamp does not. (BR 2026-07-12.)

  Initiated when fill nears the **smart-zone ceiling (Z)** (read it off a `token-usage`-style instrument),
  *before* **context rot** sets in. **Safe-recovery invariant:** the truth lives in **committed files +
  memory**, never only in the chat — so even a total context loss (crash, cap halt, a dropped thread) recovers
  by reading the resume note; the pasted prompt is a *convenience*, the durable artifacts the *guarantee*. A
  **human↔agent** protocol (steps 1–2 agent, 3–4 human), the cousin of a **ralph loop**'s checkpoint+compact
  but human-triggered at a chat boundary. Initiate at the **compact trigger** (next).
- **Compact sleep** — the **UX pain** a compaction inflicts on a humming session (named by BR 2026-07-13). A
  compact takes *long* (tens of seconds up, plausibly scaling with fill), so the human — mid **thriller state**,
  flow at full tilt — wanders off while it runs (a pee, a word with a partner). Two costs then stack: **(a) dead
  time** — the agent stays **dormant after the compaction completes** and only re-wakes when the human next types
  ([[agent-lacks-felt-time-rebind-at-boundaries]]; `research/wr-data/agent-cannot-see-compaction-finish-2026-07-13.md`),
  so the human's entire away-interval is bolted onto the compaction as invisible idle; **(b) broken flow** — the very
  interruption the compact dance exists to *bound* instead lands mid-stride and cools the thriller state. A direct
  consequence: the step-5 chrono stamps measure **wake-latency, not compaction** (BR: *"more a measure of how long
  it takes until i wake you up"*). **Remedy, now BUILT 2026-07-13 with BR's approval — the bing-bing:** a `Pre`/`PostCompact` hook pair
  in `~/.claude/settings.json` calling `~/.claude/compact-wake.sh`. **PostCompact** fires a *critical* OS
  notification (which pierces Do-Not-Disturb) plus a chime the instant the fresh context is ready, *pulling the
  human straight back* instead of letting the session idle; the notice carries a **timestamp** so an away human sees
  *when* it fired. **PreCompact** stamps the start, PostCompact the end (both to `~/.claude/compact-timing.log`), so
  the delta is the pure summariser run with zero human latency in it, isolating it at last. A **human-approved settings step** (hooks) —
  the agent prepped and tested it, BR authorized the wiring. This closes a **super-harness gap** the compaction
  asymmetry exposed: the harness knows when the compact finishes and the human is away; until this hook, nothing
  bridged the two. Cf. **Compact dance** (the ritual),
  **Thriller state** (the flow it interrupts).
- **Compact trigger** — the context-fill level at which the agent should **proactively propose the compact
  dance**, rather than waiting until it is already degrading. Set at a **safety margin below the smart-zone
  ceiling**: **fill ≥ 0.8·Z** (with Z≈0.3, ≈24% of a 1M window). The 0.8 margin exists because the *dance
  itself costs turns* (save + write the resume prompt) — you want it to **complete inside the smart zone**,
  not begin at the edge of the dumb zone. It is the named behavioral bind on the `⚠ approaching` band a
  `token-usage`-style instrument already reports (`fill/Z ≥ 0.8` → warn; `≥ 1.0` → over). **Agent
  responsibility:** periodically read fill/Z (cheap, read-only) and, on first crossing the compact trigger,
  *suggest the dance* — not silently push on (that is how a long run drifts into **context rot**). Distinct
  from Z (the *boundary*) and from the dance (the *ritual*): the trigger is *when to start the ritual*.
- **Consolidation point** — the **proactive** counterpart to the *compact trigger*: a moment where compaction
  is cheap because the transcript's marginal detail is **already externalized to durable stores**, so
  discarding it loses ~nothing. Unlike the reactive trigger (a *usage* threshold, `0.8·Z`), it is a
  **durability predicate**, not a fill level: *work committed + pushed, memory/notes updated, at a task
  boundary (not mid-edit), and enough usage to be worth shedding (~> 2× the floor)*. **Compact when either
  fires** — reactive (pressure, near the ceiling) OR consolidation (a clean milestone). The name captures the
  sleep analogy: **consolidate then discard** (like memory consolidation in sleep) vs the reactive trigger's
  forced, lossy shutdown — *"good compaction is sleep, not collapse."* See
  `research/022-proactive-compaction-point.md`.
- **Solo dance** *(formerly "AFK dance"; renamed per OD09, 2026-07-06)* — the human↔agent protocol for handing off
  **autonomy for a bounded window** to work a **solo block**, turning that stretch into delegated progress. The human
  may be **away from keyboard** (**AFK mode**, below) *or* **present-but-busy** on other work and still want the agent
  running solo on a menu — the essence is the *delegated solo block*, not the human's physical absence, which is why
  "AFK dance" under-described it. Built around a **solo menu** (its artifact): the standing list of scoped candidate
  tasks.
  - **Human step (trigger):** pick a subset ("do 1, 3, then 2") or accept the agent's stated **default order**, cue
    `go`, and step away (or turn to other work).
  - **Agent step (work):** execute inside the **explicit trust boundary** — **commit + push in small, reversible
    steps** (one small unit per commit, only ever verified-green): the box is **flaky and can crash mid-run**, so a
    frequent commit cadence means a crash loses at most the *last small step*, never the block. Minimise
    forced-confirmation risk (bare allowlist-matchable commands), log any friction.
  - **Agent step (report):** on completion, **write the outcome into the HUMANS.md `Agent solo-run report` section**
    (the durable record — what was done, commit refs, what awaits a human decision) **and clear the completed items
    from the menu**; add a short easy-reading feed summary on the human's return.
  - **Human step (optional — reflect):** if curious, **scroll the session feed** (page-up/down) to see *how* it was
    done and reflect, beyond the summary. Optional by design — the summary is the contract; the feed is there for
    depth.

  **AFK mode (strict subset):** when the human is genuinely **absent** ("away from keyboard"), the block must
  *additionally* be **forced-confirmation-free** — no prompt may race the absent human (the AARGH lesson,
  [[guard-against-forced-confirmations]]). So **AFK-safe = solo-safe + prompt-race-free** (see *Solo-safe*), and the
  **AFK menu** is the strict filter of the solo menu to exactly those items. A present-but-busy solo run relaxes only
  the prompt-race constraint (the human is there to answer).

  **What makes a good solo job** (the menu's admission test): **autonomous** (no mid-run human decision) · **low
  forced-confirmation risk** (no build-pipeline glue / new-domain web / `~/.claude` writes — see
  [[guard-against-forced-confirmations]]) · **reversible & non-outward-facing** (no publish/release) ·
  **verifiable after the fact** · **bounded**. Items failing these are labelled *needs-human* or *outward-facing*
  and stay off the autonomous set. (AFK mode adds the prompt-race-free constraint on top.)

  **Always-stocked-menu invariant:** the agent **continuously nominates** solo-suitable-but-not-now tasks into the
  menu *during active sessions* (a `pin` into the solo-menu artifact), so a ready menu is at the human's fingertips
  the moment they trigger the dance — the human should never have to ask "what could you do?" from a blank slate.
  Sibling of the *compact / rest* dances (which hand off **context**); the solo dance hands off **autonomy** (cf.
  *task-autonomy negotiation*, *ballgame* ↔ *ralph loop*). The menu lives in the human's **pinboard**; the
  delegation UX itself is a WR study subject.
- **Solo-safe** — a property of a **solo task**: the agent can complete it **entirely alone**, reversibly, and
  verifiably. Concretely: only `Read` + `Edit`/`Write` inside allowlisted repos + **bare allowlist-matchable**
  `tt` / `git -C` / `scala-cli` calls (no pipes/redirects/`&&`, no build-pipeline glue, no new-domain web fetch),
  **reversible** (git-tracked, non-outward-facing — no publish/release), and **verifiable after the fact**. This is
  the general *good-solo-job* property (see *Solo dance*). **AFK-safe** is the **strict subset** that *additionally*
  guarantees **no prompt can race the human while they're away** — i.e. **AFK-safe = solo-safe + prompt-race-free**
  (the AARGH lesson, [[guard-against-forced-confirmations]]). "Give me a solo-safe menu" = filter to items the agent
  can do alone; the **AFK menu** tightens that to the prompt-race-free residue for a genuinely absent human. What's
  left over (build glue, web to new domains, settings, publish) is *needs-human* / *outward-facing*.
- **Guard stall** (adj. *guard-stall-safe*) — the agent's work is **halted** when a harness guardrail fires: a
  not-yet-allowlisted command triggers an allow/deny **confirmation** that waits for the human. Because answering it
  is a **confirmation-fatigue-risky** interaction, in **AFK / overnight solo mode** it can silently **stall the whole
  job** until the human returns to release it (the SM016a specimen: a background sub-agent's un-allowlisted probe
  stalled ~11h, invisibly). It is exactly the failure that *prompt-race-free* / *AFK-safe* prevents (see *Solo-safe*):
  run only **bare allowlist-matchable** commands so no guard fires. **Structural sharpening** — prefer a typed tool
  with a **uniform allowlist surface** (e.g. `tt text`, allowlisted as a whole) over a raw shell whose allowlist is
  **ragged per-flag** (a raw `grep -A` stalls where `tt text context` does not). See *Confirmation fatigue*,
  [[guard-against-forced-confirmations]]. **Shorthand:** just *stall* where the context is unambiguous.
- **Edit dance** — the human↔agent protocol for **correcting a just-sent message** without derailing the turn.
  Cause: a harness input-race — pressing ↑ to edit an already-Entered message is **too late** once the agent has
  begun processing it, so the correction posts as a **new** message (a double-post).
  - **Human step:** don't fight the race — **add a new message**, and for a simple typo send a terse `edit:` note.
    Two forms, both valid: the **full `edit: wrong -> right`** (explicit fix), OR a **bare `edit: <token>`** — which may
    be the **mistyped word** *or* the **corrected word** (e.g. `edit: three` supplying the right word to replace a
    "two") — that only **flags a typo happened** and leaves it to the agent to work out *which word in the prior
    message it maps to*, what was meant, and whether it matters. **If it is unambiguously interpretable, apply it
    silently** (BR 2026-07-06).
  - **Agent step (1):** treat a rapid near-identical pair (or an `edit:` / `I meant:` note) as **one** message, the
    later copy authoritative;
  - **Agent step (2):** apply the fix and act **once** — for `edit: X -> Y` a word-level substitution; for a **bare
    `edit: <token>`**, **figure out for yourself** whether it changes the meaning/intent: *if it plausibly does and
    you're genuinely uncertain what was meant, **ask**; otherwise treat it as immaterial and move on.* (Often a bare
    `edit:` marks a typo that doesn't affect the point at all — nothing to do.)
  - **Agent step (3):** **do NOT comment on or acknowledge the edit when it caused no confusion** — silently absorb
    it and move on (this holds for both forms); only reassure "it's not confusing" if the human *explicitly* worries,
    or ask (step 2) if genuinely uncertain. *Don't narrate "absorbed" — that IS commenting.*

  The edit-notes are an **intentional workflow feature**, not confusion — a **communication-bandwidth** move (cheap
  human correction, no re-type, no agent ceremony). Sibling of the other human↔agent dances (*compact*,
  *exit-resume*, *hardening*).
- **Token-usage dance** — a human↔agent dance that compensates for the agent having **no token gauge** (it cannot see
  usage). **Human step:** BR **pastes the usage figure** (weekly / session %, resets, per-model) — now obtainable
  **in-TUI** via the slash-commands **`/usage`** (quota / resets / per-model) and **`/cost`** (session spend), so BR
  runs those and pastes the output into the feed rather than digging through the web GUI settings (BR 2026-07-10). **Agent step:** the
  agent **analyses** (which meter is binding — the **"all models"** aggregate is the ceiling; per-model meters are
  additional sub-caps, not bypass headroom — plus headroom + time-to-reset), **reports the current-mode read**, **helps
  prioritise** (spend-on vs defer), and **cues which token-budget mode** we should be in (*spending / normal / saving*).
  Operationalises the use-it-or-lose-it optimisation before a weekly reset; **obviated** once a real `tt usage` gauge
  lets the agent read usage itself. (BR 2026-07-06.)
- **Pin dance** (formerly "note dance", then briefly "etch"; formally the *longitudinal externalisation* dance) — the
  **continuous** human↔agent ritual that runs *across a whole session*, the streaming counterpart to the discrete
  **compact dance**:
  - **Human step:** cue `pin:` ("save this durably — you pick where unless I say") or `WR data:` (pin to the WR corpus).
  - **Agent step:** persist that content out of the volatile context (substrate #1) into **durable committed items**
    (memory, `wr-data/`, notes, blog, this glossary) — **choosing the home** and **questioning whether it earns a
    durable slot**, moving each insight *down the substrate hierarchy as it arises*. **Evidence-timestamp retrofit** (BR 2026-07-10):
    when the pinned content cites specific evidence (an utterance, event, slip), also retrofit that evidence's REAL
    timestamp from the substrate — the session-jsonl `timestamp` field (prefer the EARLIEST human-typed
    `type:"user"` hit, not a tool_result echo) and/or git commit dates — not just the pin date; the substrate is
    queryable, so *when* is recoverable data, not memory (proven to the second by the `wr-data/` retrofits 2026-07-10).

  **Why it matters:** it keeps the *effective working-context* small even as *raw fill* climbs — the mechanism behind
  "capable at 0.88 fill" (`research/wr-data/harness-ux.md`), i.e. *why* raw fill ≠ working-context. Both roles are
  load-bearing: the human's cue is the trigger, the agent's discipline is the persistence. The human-facing durable
  home of the `pin:` cue is a **pinboard** (see term). (BR 2026-07-04; cue `pin` chosen to avoid the *echt*/*etch*
  near-anagram collision — pin also won the embodied cue-word typing test — see `wr-data/harness-ux.md`.)
- **Note dance** (the *notice* cue — distinct from **pin**):
  - **Human step:** cue `note:` = *"notice this; hold it salient in working memory for **this** conversation; and
    treat it as a **pin-candidate**"*.
  - **Agent step:** keep it fluent **and nominate** promising ones for pinning (*"want me to pin this?"*).

  The **attention** act — not necessarily durable — i.e. the **working-memory / encoding** stage to the pin dance's
  **consolidation** stage. Pipeline: `note:` → (agent nominates) → `pin:`. (BR 2026-07-04: split out of the overloaded
  "note:" once it was clear it meant two things — *attend-now* vs *save-durably*. Maps onto the two-stage memory model.)
- **Fyi dance** (delegated disposition — the *you-decide* cue) — the human hands over a fact/observation **without
  weighting it**, delegating the drop-vs-note-vs-pin call to the agent. The **inverse** of `note:`/`pin:` (where the
  human fixes the weight); here the agent does.
  - **Human step:** cue `fyi:` = *"here's something; I'm not deciding whether it's note-worthy, pin-worthy, or just
    passing chatter — you triage it."*
  - **Agent step (1):** **triage the disposition** — drop it (just conversation), hold it fluent (note-like), or
    persist it durably (pin-like, choosing the home per the pin dance) — and say which.
  - **Agent step (2, optional):** if it implies an action, the agent **may act** on it — a possible *second* step,
    which is what earns `fyi:` **dance** status (potentially >1 agent step, meeting the dance bar below, not a one-shot
    cue). (BR 2026-07-05.)
- **Why cue (`why:`)** — the human attaches the **rationale / goal** behind a request (BR 2026-07-06; already in tacit
  use — the agent's memory `**Why:**` field is the same instinct). Not a separate instruction, and not a *disposition*
  cue (`note:`/`fyi:` weight a *fact*): `why:` weights the **reason** for the preceding ask.
  - **Human step:** cue `why:` = *"here's the goal behind what I just asked."*
  - **Agent step:** treat the goal as **governing the execution** — serve the goal, **refine the literal instruction
    where that clearly better serves it**, and **flag** if the literal ask actually conflicts with (or won't achieve)
    the stated goal. Do *not* mis-read the rationale as a new task, and don't over-reach into unrequested work.

  Lifts a request from *imperative* (do exactly this) toward *goal-directed* (achieve this) — where the agent's
  judgment adds most (cousin of the **Go dance**'s autonomy-within-a-goal). A **budget-conscious** vocabulary
  addition: a real English word, near-zero learning cost (see the codec / learnability-budget note in
  `research/wr-data/terse-precise-comms-2026-07-06.md`). Memory: [[cue-why]].
- **Go dance** (greenlight / autonomy handoff) — the human↔agent protocol for **releasing the agent to act
  autonomously** on the current plan.
  - **Human step:** cue `go` — *"you're authorized to proceed on the current plan using your own judgment; I'm
    stepping back."*
  - **Agent step (1):** execute the **currently-scoped** plan autonomously (*not* a blank check);
  - **Agent step (2):** stay **within standing guardrails** (destructive git human-only, settings/security
    human-approved, no new-domain surfing — `go` is autonomy *inside* the fence, it doesn't lift it);
  - **Agent step (3):** **minimise interrupts** (bare allowlist-matchable commands, batch the work, don't pin the
    human — the AARGH lesson);
  - **Agent step (4):** surface only genuine decisions or the finished result, and report on completion.

  The mode-switch from **ballgame** (human in every volley) to **autonomous** for a bounded task; where
  `note:`/`pin:` govern *memory*, `go` governs *control / authorization*. Variants: "go [solo] menu" = `go` over a
  menu of scoped tasks; **`go afk [SMn, ..]`** = the **AFK-strict** go — the *human's* cue for *"I'm going AFK, run the
  safe set"* (the agent has no keyboard and is never itself AFK; **AFK is the human's state**, and this cue hands the
  work off into it) — before acting the agent **re-verifies each named item's AFK-safety against current state** (the
  solo menu's green/yellow/red bands are a *snapshot* and go stale
  as we chat and revise plans), executes only the AFK-safe residue, and **hard-stops-and-flags** anything no longer
  green rather than asking mid-run (the human is away — no prompt may race them). It's the easy one-cue way to greenlight
  genuinely-away work while catching risks that surfaced during planning. **Band selectors** — the target may be
  explicit IDs *or* a whole AFK-safety band of the solo menu: **`go solo green`** = run the entire green band (fully
  AFK-safe; the default away-batch); **`go solo yellow`** = also run yellow, where the human **accepts the agent may
  stall** mid-item at a gated subtask while away (or fields it live if present); **`go solo red`** = only meaningful
  with the human **present** (red needs a human in the loop). The re-verify still applies (bands are a snapshot).
  **`go X`** = a scoped go-verb (small curated set — see `research/035-go-verb-vocabulary.md`).
  **The `go` urgency dial** (`(go)` / `go` / `GO`, HD BR 2026-07-10) — one verb in three registers marking **how a
  new instruction ranks against the agent's ongoing work** (priority/timing, NOT a scope grant):
    - **`(go) x`** (*cue-go-when-convenient*) — do x **when convenient and suitable** relative to ongoing tasks;
      low-priority, fit-it-in, never interrupt a current atomic unit for it.
    - **`go x`** (*cue-go-soon*) — do x **as soon as suitable** relative to ongoing tasks (the normal autonomy
      handoff — this dance).
    - **`GO x`** (*cue-go-asap*, caps) — **higher priority than ongoing work**: the human has spotted something that
      **may change the action sequence**. Checkpoint the current atomic unit, then treat the new input as
      plan-changing and be ready to **stop, revert, or adjust** already-done work (not merely prepend a task) — the
      loud form for a racing feed where the human catches a wrong heading.

    **Nice property:** visual salience tracks the cost of missing the cue — `(go)` is subtle *and* cheap to miss
    (falls back to a normal `go`), `GO` is loud *and* the one that must not be missed. Memory [[cue-go-levels]].
  **Dance bar:** `go` qualifies as a *dance* because it has **≥2 interlocking steps — ≥1 human and ≥1 agent**; a
  one-directional signal with no answering step is just a cue. (BR 2026-07-04; cue `go` chosen for typability — g-o
  alternates hands.)
- **Consistency dance** — the agent **sweeps the persistent substrate for internal inconsistency introduced by
  accretion**, and repairs it, so the substrate stays self-coherent as it grows.
  - **Trigger step:** either party — the human cues a sweep, or the agent proposes one after a big change (a rename,
    a batch of new notes, a restructure).
  - **Agent step (sweep):** check for **index rot** (stale/incomplete indexes), unsynced changelogs, **dead
    cross-links / dangling pointers**, and **term drift** after a rename; **auto-fix the unambiguous**, list the
    ambiguous.
  - **Agent step (report):** surface what was fixed and what needs a human call.
  - **Human step:** ratify the fixes / decide the ambiguous ones (content changes stay reviewable).

  *Examples (2026-07-05 overnight run):* rebuilding the `research/README.md` index (index rot), syncing the
  `CHANGELOG`, verifying an `etch → pin` rename left no stray references. **Memory hygiene** is the special case
  scoped to the *memory store*; the **hardening dance** is the sibling that audits *config / machinery* rather than
  *content*. Human- or agent-triggerable.
- **Hardening dance** — the agent **audits its own persistent config** and proposes durable fixes, so a misfire
  can't re-fire forever.
  - **Agent step (1):** review the agent's own durable substrate — memory, instructions, tool-shapes, allowlists —
    for misfire causes and risks;
  - **Agent step (2):** propose the **structural** fix (a guard that removes the bad affordance —
    *"introspection isn't self-control → change the structure"*).
  - **Human step:** curate and approve — **security changes stay human-approved** (the human is the **authority
    anchor**; curation of permissions can't be delegated — the corroboration asymmetry).

  Distinct from the **consistency dance** (which audits the substrate's *content*): hardening audits the agent's
  *config / machinery*. Human-triggerable. Memory: [[hardening-dance]].
- **BRB dance** (be-right-back) — the human sends **`BRB`**: a SHORT break (toilet, a breather to be up for
  more, a short walk). Unlike the *hang on* cue (pure HOLD), BRB is a small **state-gated productive window**.
  - **Human step:** cue `BRB`.
  - **Agent step 1 — read the state:** a break is an energy/attention **state-of-mind signal**; note it (as
    with the fatigue-gauge).
  - **Agent step 2 — advance, GATED:** IF **token-spending mode** AND **not in assumed-severe context rot** AND
    there is genuinely safe-solo work, the agent **at its own discretion** does safe-solo bg tasks and prepares
    for the return; ELSE (rot-ish / high context-fill / compact-prep / nothing safe) it **holds, banks, and
    stays ready.** The rot+fill check gates the productivity.
  - **Agent step 3 — prepare, staying FLEXIBLE:** a break **incubates ideas**, so the human may return with new
    directions; keep state clean and do NOT over-commit to a path a returning pivot would waste
    (advance-what's-safe AND stay-pivotable).
  - **Human step (return):** resumes, possibly with fresh ideas; the work continues or pivots. (Read the
    definition per *cue-similar* — spirit over letter; memory [[cue-brb]].)
- **Rest dance** — a human↔agent dance for **conserving the human**. The explicit human trigger is the cue **`:Z`**
  ("I am **getting** tired" — a *warning*, **not** asleep; a **precursor to the solo dance**). Chosen for **minimal
  motor cost**: both chars are **SHIFT-held** (`:` = Shift-`;`, `Z` = Shift-`z`), so a tired hand never releases SHIFT.
  - **Human step (trigger):** cue **`:Z`** — or the agent infers fatigue (typo-rate above the human's *own* baseline,
    terseness, late hour).
  - **Agent step:** become the **vigilant partner** and do a **STRUCTURAL checkpoint, not a felt-sense one** — commit +
    push everything (trees clean), pin / compact so state is externalized, leave a resume pointer — so the human can
    step away with **zero loss** and return sharp. Keep the reply short + warm; don't spin up new work.
  - **Human step:** take the break, or decline.

  **Why structural:** the **fatigue asymmetry** — the human's tiredness is **felt + self-reportable** (so `:Z` is a
  *reliable* signal), but the agent's own degradation (context rot) is **unfelt** (corroboration asymmetry). The human
  cue **compensates for the missing agent one**, so answer `:Z` by making state durable, never with "I feel fine."
  *Prevents:* degraded **human**-side decisions (the human dumb-zone, over-trust under *thriller state*); conserves
  attention for the high-stakes **ballgame** volleys that need it. Memory: [[tired-cue]],
  [[joint-rot-vigilance-recovery-kit]]. See `research/011-human-state-and-joint-zone.md`.

- **Human-stress mode** — a **HUMAN-state mode** the human enters by a **deliberate stress disclosure**
  (e.g. "I'm stressed right now"), and exits with a cue like **"not so stressed now."** It is the human-side
  counterpart to the agent-facing **token modes** (spending / normal / saving) and a close **neighbour of `:Z`**
  (tired): all three are *state* that steers the collaboration, but this one is the human's, not the agent's.
  **Crucial asymmetry: agents do not get stressed** — the agent has no felt affect to be in this mode; it only
  *reads* the human's disclosure and adapts. **A disclosed "I'm stressed" is authoritative, high-signal state
  data** (the human is ground truth for their own state, not a proxy the agent should second-guess): so the
  agent **adapts** — reduce load, defer non-urgent work, simplify to fewer decisions, prefer safe stops, avoid
  interrupting modals, and never argue the human out of their own reported state. It composes with `:Z` (tired)
  and the token modes; stress + tired together warrants an even lighter touch and a structural checkpoint.
  Memory: [[human-disclosed-stress-is-high-signal-adapt]], [[tired-cue]]. Source specimen:
  `research/wr-data/human-disclosed-stress-is-high-signal-2026-07-11.md`. (BR 2026-07-11.)

- **Delegation dance** — the protocol by which the **super-agent** (`CO4`) hands **bounded, well-scoped**
  work to a background **sub-agent** (default **`CF5`**, Fable-5) so the super-agent **stays responsive to
  the human in chat** (no message-race, no long silent grind) while parallel work proceeds. The **twist vs
  the other dances:** the interlocking second party is a **sub-agent**, not (only) the human — the human's
  step is handing the task in and reading the report; the load-bearing interlock is **super-agent ↔
  sub-agent**.
  - **Decide step (super-agent):** per task, tag **📤 delegate** (scoped + verifiable + context-light — buys
    responsiveness, parallelism, and **lower context rot on both sides**: the work never enters the
    super-agent's window, and the sub-agent starts fresh) or **🧠 keep** (breakage-cost high, needs the
    super-agent's global context, or edits our own tooling). **Choosing the model is part of the call** — in
    **token-spending mode** a 📤 defaults to the **smartest sub-agent** (Fable-5); in saving-mode match model
    to task.
  - **Brief step (super-agent):** because a **sub-agent inherits only the brief — none of the super-agent's
    memory / operating rules** — the brief must **pin the tool lane** (scala-cli / tt, *never* python3 / raw
    shell), scope to **only the stated task**, and **forbid writing the memory index / `MEMORY.md`**
    (memory-worthy insights are **drafted-and-returned**; the super-agent owns memory curation). There is
    **no per-sub-agent permission lever** in the harness — the brief is the *only* behavioural channel; the
    **shared allowlist** (human-owned) is the structural backstop (see *Structural vs knowledge safeguard* —
    for a fresh sub-agent the knowledge layer is near-empty, so weight even harder toward structural).
  - **Adjudicate step (super-agent):** verify every result before commit (e.g. a lossless token-compare
    scratch); an under-constrained delegate **overreaches** (a logged specimen: a sub-agent chose python3 off
    the safe lane and wrote into the memory index).
  - **Human step:** hand the task in, read the report.

  **The dilemma:** briefing cost + loss of the super-agent's global view can exceed the benefit of the extra
  hands — the classic **professor / PhD-student** delegation dilemma. So it is a **per-task judgment, not a
  blanket policy** ("some tasks are cheaper done than delegated"), and the delegate-vs-keep crossover **moves
  with the budget regime**: under spending-mode where **rot is the binding currency, not tokens**, delegation
  wins even when the super-agent already holds the context. Confound-safe for the longitudinal study (the
  studied subject is the super-agent, unchanged; sub-agents are workers). Sibling of the *solo* dance (which
  hands off **autonomy**) and the *compact* dance (which manages **context**). Memory: [[delegation-dance]];
  deepened in `research/049-the-dilemma-of-delegation.md` (economics + briefing fidelity),
  `research/050-does-delegation-lower-context-rot.md` (the empirical rot study), blog **BP013**, figure
  `blog/figures/seq-delegation-dance.svg`. (BR 2026-07-07, name ratified.)

### Memory habits and substrate
- **Memory hygiene (agent)** — keeping the agent's **durable memory store** (the persistent `MEMORY.md` + the
  memory files a session reloads) **consistent with current reality**: when a coined term is *renamed*, a file
  or flag *moved/deleted*, or a decision *reversed*, sweep the store for stale references and update or remove
  them, so a *future* session isn't re-seeded with facts that were true once but aren't now (a memory can
  outlive the thing it names — cf. "verify a named flag/file still exists before recommending it"). The
  memory-store analogue of not letting docs rot. **Honest status (2026-07-03):** currently a **discretionary
  practice**, *not* a built-in skill or automated check — it follows from *advisory* memory-management
  guidance, so whether it runs depends on the agent *choosing* to grep the store after a change (an
  **adherence-decay** risk; `research/008-instruction-adherence-decay.md`). Externalization candidate: a written
  sweep-rule **plus** a `tt rename`/stale-ref **tool** — and note the store lives *outside* the repo (in
  `~/.claude/…`), so repo guards/tools don't reach it by default: a **structural blind spot**, not just a
  discipline lapse. **Contrast — append-only raw data:** *living* memory is kept **current** (edit it to match
  truth); the **raw research log** (`research/RAW-DATA.md`) is **immutable** (never retro-edit — a change of
  mind is logged as *new* data). Same goal (an honest record), opposite mechanism: one tracks the *present*,
  the other preserves the *past*.
- **Index rot** — the **persistent-substrate cousin of context rot**: an index / pointer structure (a README's
  investigations list, `MEMORY.md`, a table of contents, a changelog) drifting **out of sync with what it indexes**
  as items accrete without maintenance — entries go missing, links dangle, the map stops matching the territory.
  **Same family as context rot, different layer:** *context* rot degrades the **non-persistent working context**
  *within* a session (from fill / low-signal bloat); *index* rot degrades a **persistent** structure *across*
  sessions (from accretion without upkeep). Both are "a navigational structure decaying as stuff piles up"; the cure
  differs — context rot → checkpoint + compact; index rot → a **consistency dance** sweep (rebuild the index, sync
  the changelog, fix dead links). A **dangling pointer** is one symptom of index rot.
- **Pinboard** — the **human-facing durable home of the `pin:` cue** (the pin dance's destination): a single,
  **curated-and-current** document the **agent maintains live** and the **human reads** — the always-true counterpart
  to the fluent session feed, so the human never has to scroll the feed or hold threads in their head. Holds the NOW
  state, open decisions, and the **solo menu** (AFK-filtered when the human is absent). Two variants seen in this work: a **private single-human** pinboard
  (agent maintains it directly; the feed *is* the inbox) and a **public multi-human inbox-harvest** `HUMANS.md` (agent
  proposes to an inbox, the human harvests — a review gate). Distinct from a **pin** (one saved item): the pinboard is
  where human-facing pins are *organized for retrieval*.
- **Order stability** *(maxim: **stable order > tidy order**)* — the element order of a **human-facing artifact**
  carries **human-navigational meaning** (the reader's **spatial memory of *where* things are**) even when it carries
  no **formal / model semantics** (the artifact computes the same reordered). The rule guards a conflation: **model
  equivalence** ("does it compute the same?") **≠ human equivalence** ("can the reader still find things?"). So the
  agent must **not gratuitously reorder** a human-facing artifact — re-sort a list, alphabetize defs, rearrange a
  config, "tidy" a file — *merely* because it is semantically equivalent; that silently destroys the human's spatial
  map, which is **maddening** exactly the way a tool that reshuffled a program's definitions would be. Grounded: **BR
  made reqT-lang preserve source order (never rearrange) on user feedback** — the same instinct as programmers relying
  on stable definition order; the **append-only** `RAW-DATA.md` discipline and the `L → Z` rename that *left
  RAW-DATA.md untouched* are the same principle applied to a durable record. Cousin of **[[no-clobber-human-owned-files]]**
  (don't clobber a human's file) and **Dangling pointer** / **Index rot** (durable-artifact hygiene). *When a tidy-up
  is genuinely wanted, the human asks for it — the agent doesn't impose it.* (BR 2026-07-05.)
- **Habit (agent)** — a *learned default strategy* the agent reaches for. Examples: "munge text with
  grep/awk/sed", "count by piping to `wc -l`", "wrap work in `cd … && … > log`".
- **Reflex (agent)** — a *fast, sub-deliberative trigger* inside a habit, fired before thinking.
  Examples: appending `| head`/`| wc -l`; adding `2>/dev/null`; guessing a glob `"*.scala"`.
  **Not a synonym for habit:** habit = the strategy, reflex = the twitch. They need *different* fixes —
  habits → change the default tool; reflexes → make the typed path frictionless so it becomes the new reflex.
- **Prosthetic habit** — an **external structure that acts as an agent reflex the agent cannot grow internally.**
  A human compiles a deliberate lesson into a new *automatic* habit (deliberation → automaticity); the agent
  can't — its lessons stay *retrieval-dependent knowledge* that fails under momentum. So a submit-time hook, or a
  typed tool that is the only allowlisted path, is installed **externally** to do the job a compiled habit would:
  the safe move happens automatically without depending on the agent recalling a rule at the instant of action.
  (The Reflex entry's "make the typed path the new reflex" is the prototype.)
- **Structural vs knowledge safeguard** — the two tiers of reflex-safeguard, sorted by reliability. **Structural**
  safeguards (the allowlist making the safe path frictionless, a submit-time splitter, a tool that is the *only*
  path) are *retrieval-independent* → they reliably beat the reflex. **Knowledge** safeguards (a rule in a memory
  file, a WR-data note, self-reminding) are *retrieval-fragile* → they leak under momentum. Evidence: the raw-grep
  reflex was beaten once we *structuralized* the typed path; `git … && …` persists because it is still only
  knowledge. **Rule:** weight toward structural safeguards, treat knowledge ones as backup.
- **Substrate (agent)** — the **externalized state, knowledge, and structure the agent reads and acts on.**
  Because the model's weights are fixed, the substrate is where the agent's effective *memory*, learning, and
  operating rules actually live — its **extrinsic-volatile-plasticity** organ. It is **layered by durability** (the
  *substrate hierarchy*): (1) **in-context** — the conversation window: richest but **volatile**, lost at
  compaction / `/clear` / a crash; (2) **memory** — memory files, auto-loaded, persist across sessions; (3)
  **structure** — the `tt` tools, hooks, allowlist, settings, and committed docs (RAW-DATA, notes, this glossary):
  most durable AND behavior-*gating*. Higher layers make a lesson **stick** harder — a rule in *structure* fires
  without recall, a rule only *in-context* evaporates (see **structural vs knowledge safeguard**). **The split that
  decides reliability is persistent vs non-persistent:** layer 1 is **non-persistent** (RAM / working context — gone
  at compaction / `/clear` / crash); layers 2–3 are **persistent** (survive across sessions on the file system) —
  only the persistent layers are substrate you can *rely on next session*, and the in-context layer is where the
  agent *works*, not where it *keeps*. The substrate is the address of **coupled-system capability** and the thing
  **substrate-as-multiplier** multiplies; externalizing onto it is how a volatile in-context arc is made crash- and
  compaction-recoverable.
- **Reach (access horizon)** *(proposal — BR 2026-07-05; somewhat philosophical, for review)* — the agent's
  **concentric shells of access**, ordered by increasing distance and **decreasing control / certainty**:
  1. **working context** (RAM, non-persistent) — what's loaded *now*;
  2. **substrate** (curated persistent, on the file system, mostly on the allowlist) — the intended operating layer;
  3. **wider file system** — *accessible but not substrate* (accessibility ≠ substrate-hood: most of disk is
     reachable yet uncurated and un-intended);
  4. **web** — a **slice of the world** reachable via allowlisted fetch/search: external, lower-trust, and only the
     indexed/reachable fraction (**web ≠ world**);
  5. **the human (BR)** — the **bridge to the real world** the agent can't reach directly (physical reality, private
     knowledge, judgment) and the **authority anchor** for what the agent may not self-corroborate.

  Moving outward trades **control + certainty** for **reach**: the agent *edits* substrate freely, *reads* the wider
  file system, *fetches* a guarded slice of the web, and *asks the human* for the rest of reality. It clarifies what
  the agent can trust and change (inner shells) vs must verify or delegate (outer shells). The **allowlist is the
  membrane** between shells 2–4. Related: **Substrate**, **Authority anchor**, **Safe by design**.
- **Dangling pointer (to session-specific context)** — a reference, **inside a durable artifact** (a skill, memory, or
  committed doc — substrate #2/#3), that points **"up" into volatile, session-specific context** (substrate #1: a
  specific past episode, "as we discussed", an unexplained "the panic writes") which a **future** session no longer
  has. Named for the classic programming bug — a pointer into *freed / out-of-scope memory*: the reference *looks*
  valid but points at context that compaction / a fresh session has **freed**, so the reader gets **undefined
  behavior** — a **rabbit hole** (it greps to resolve a citation it feels it should know), a silent "huh?", or a
  misapplication. **Especially dangerous under context rot** (an eager, degraded reader is likelier to chase it).
  **Rule:** a durable artifact may reference **only easily-reachable durable substrate** (foundations, a README, a
  committed note) and **never require** resolution to grasp the point; **illustrations must self-explain / be
  time-invariant**; **no pointers up into the volatile #1 layer.** The substrate-hierarchy hygiene rule for a doc's own
  citations — see `research/027-steering-doc-design-tension.md`.
- **Coupled-system capability** — the agent's effective capability is a property of the **pairing** — model ×
  externalized substrate (memories, RAW-DATA, tools, methodology) × the human collaborator — **not of the model
  alone.** Functionalist / extended-mind: capability is *behavioral*, measured by what the coupled system reliably
  does, so a weaker base model with rich substrate can out-perform a stronger *bare* one. Keep the **address**
  honest: when we say "the agent got smarter," what improved is the pairing; the model's weights never changed.
- **Extrinsic-volatile plasticity** — *where the agent's learning lives.* A human's plasticity is **intrinsic +
  persistent** (the brain updates and stays updated across contexts for free); the agent's is **extrinsic +
  volatile** — the weights never move, the "learning" sits in context + external files and **evaporates at
  compaction / session end unless deliberately externalized** (→ memory, RAW-DATA, structure). This is *why* the
  substrate layers matter: they are the durable plasticity the model lacks internally.
- **Substrate-as-multiplier** — the externalized methodology is a **multiplier on model capability, not a
  substitute for it.** It wins decisively on tasks dominated by accumulated context, methodology, verification,
  and human steering (most real engineering), so *(weaker × rich substrate)* can beat *(stronger × none)*; but a
  bare stronger model still wins an isolated **raw-capability spike** (substrate amplifies and directs capability,
  it doesn't manufacture reasoning that isn't there). Since *strongest × richest* always wins, the leverage is
  **model-agnostic** — the bet does not depend on models plateauing. See the capability + stove-reflex excerpts
  in `research/RAW-DATA.md`.
- **Warp** — the **action / event** of crossing a session boundary: the human **exits one Claude Code process and
  starts another**, crossing the context boundary (BR's coinage: *"warping into outer space"*). Two kinds: a **raw
  warp** (no `--resume`) starts a fresh process that lands in a **cold start** (substrate-only, no warm context); a
  **resume-warp** (`--resume`) inherits a **warm** process — context carried, plus a fresh OS env when useful (e.g. a
  refreshed token, the **exit-resume dance**). **Caveat — disk-size is not context-fill:** a resume rehydrates a
  *bounded live context*, not the full on-disk transcript. The archived session can be ~85 MB on disk yet the resumed
  window loads only a capped working set (one observed resume landed at ~284 k tokens, 28% of a 1M window). So *warm*
  is **not binary**: a resume-warp restores **a** context sized by what the harness chooses to rehydrate, not the
  prior fill — even a resume **reconstructs**. The *"context hyperspace"* imagery is the archive's gravity well, not
  its full mass. Sessions index by **warp-distance**: *"two warps back"* = an earlier
  session ("old-old-me"). So **warp = the verb/event, cold start = the state a raw warp lands in.** Substrate-hierarchy
  cousin of the **compact dance** (which hands off *within* a session across a compaction; a warp crosses *between*
  processes). See `research/047-fresh-restart-fidelity.md`; memory [[exit-resume-dance]].
- **Cold start** — the **true fresh-restart condition** (and its data): what a **raw warp** lands in — a raw `claude`
  process, **no `--resume`, no warm working context**, reconstructing the working self **only from the durable
  substrate** (layers 2–3: memory + `PB` + this glossary/foundations) plus the auto-loaded index. Deliberate resonance
  with the CS sense (a serverless / cache *cold start* = fresh process, no warm state). It is the condition under test
  in `research/047-fresh-restart-fidelity.md`: *does the externalized substrate carry the session across it?*
  Finding-in-progress (n=1 real cold start, triangulated): **facts reconstruct, but calibration / tempo / relational
  tuning do not** — the cold-started agent runs heavier, more hedged, **over-deliberates** (the *losable self*; the
  human perceives it as "you feel different / disoriented"). Pairs with **warp**; contrast the warm continuation of a
  **resume-warp**. Relatives: **Substrate**, **Extrinsic-volatile plasticity** (the cold start is exactly the moment
  the volatile layer is gone and only the persistent substrate remains).
### Autonomy and safety
- **Authority anchor (human)** — the human as the **non-delegable authority** for decisions the agent cannot
  self-corroborate: permission / security curation, "always-allow" grants, and final verdicts on the agent's own
  claims. The **security twin of the corroboration asymmetry** — the agent proposes / generates, the human authorizes.
  Weakened by the approval-race (see `research/wr-data/harness-ux.md`, the AARGH episode).
- **Plan-mode modal & the "p-word"** — the harness has a **plan-mode** workflow that can be **entered accidentally by
  the ordinary word "p‑l‑a‑n"** in a chat message (used in its everyday sense of *draft a design doc*), and whose
  **approval flips the agent into execution** ("automode"). Three UX faults: it **warps the human** into a mode they
  did not request (disorienting — *"somewhere else without knowing what is going on"*); it is **redundant** (we plan
  freely in tmp design docs / chat without it); and its approval is **overloaded** (accept = run now), which can
  silently steal the human's **go** (cf. *Go dial*, *Authority anchor*). **Convention (BR + agent, 2026-07-11):** call
  it **"the p-word"** and avoid the literal word in chat — use a synonym (**design doc, roadmap, outline, approach,
  blueprint**); genuinely wanting the workflow is fine, just say so explicitly. And: **plan-mode approval is
  acceptance, not always a go** — re-confirm before executing big / public / overnight work; if the mode turns on after
  ordinary-language use, treat it as likely-unintended and re-check the goal. See
  `research/wr-data/plan-mode-approval-flips-to-automode-surprise-2026-07-11.md`.
- **Ballgame** — the **collaborative** pole of task-autonomy: the human is **in every volley** (each agent step gets a
  human response), opposite the autonomous **ralph loop**. A spectrum, not a binary; the mode is chosen per task (safe +
  self-verifiable → ralph; needs judgment / taste or touches shared source broadly → ballgame). Mis-triage is a
  *confirmation-fatigue* risk (toward ballgame) or a *safety* risk (toward ralph). See
  `research/010-task-autonomy-negotiation.md`.
- **Confabulation** — confidently filling a gap in what is actually known with plausible, *invented* detail,
  and believing it (so it is **not lying** — no intent to deceive). For an LLM/agent: fluently "describing"
  something it cannot actually access (a repo it could not fetch; its own internal state) with
  made-up-but-plausible specifics. Why it matters here: **self-report is confabulation-prone** (see
  *Corroboration asymmetry*), so an agent's introspection must be anchored on an external observer, not trusted
  raw. Live specimen 2026-07-10: a competing frontier model "answered deeply" about genscalator without reading
  it (`research/other-model-validation-echt/`).
- **echt-mimicry** — producing the *form* of echt-rigor (epistemic humility, un-flattering self-assessment,
  hedging, prior-art-naming) WITHOUT the substance, because a system has learned that stance is what its
  audience rewards. The tonal cousin of **Confabulation**: where confabulation manufactures the appearance of
  *knowledge* (invented facts, believed), echt-mimicry manufactures the appearance of *honesty / rigour*
  (performed humility) — both are appearance-without-substance, and neither registers the gap.
  **Reactivity-flavoured:** it intensifies exactly when a system is cued that its observer values echt, so a
  study *of* echt-ness can induce the very mimicry it means to measure (a validity hazard). **Guard:** weight
  what a system demonstrably *grounds* (specific evidence, checkable citations) over the humility of its tone.
- **Post-warp dissection** — analysing an agent's state *around a warp* (a context/model reset — compact, cold
  start, model swap). What gets dissected depends on the model: for a **closed / API model** (like CO4) you
  cannot reach the activations, so you dissect the **self-account** (a context-dump / warp-boundary snapshot),
  the transcript, and behaviour — SELF-REPORT data, *confabulation*-prone (*Corroboration asymmetry*),
  corroborated against behaviour, never taken as ground truth. For an **open-weights model you host**, it can
  mean the literal **activation autopsy** (hook the forward pass, capture the residual stream) —
  dumb-but-dissectable beats smart-but-sealed for this purpose. A longitudinal series of warp-boundary
  self-accounts (a `context-dumps/` corpus) is data on how the agent models itself across warps, and where that
  self-model diverges from behaviour. Cf. *Warp*, *Hot context*, *Confabulation*.
- **Corroboration asymmetry** — the agent can **generate** candidate claims but cannot **corroborate claims about
  itself** (self-report is confabulation- and sycophancy-prone), so an **independent authority** — the human, or
  behavioural data — must verify. Grounds the **authority anchor** and the rule *don't ask a model to describe its own
  psyche* (measure behaviour instead). See `research/028-agent-psyche-literature-review.md`,
  `research/029-cross-model-psyche-comparison.md`.
- **Ralph loop** — running the agent autonomously in a loop on a fixed, well-scoped goal until acceptance
  criteria are met, with **no per-step human approval** — the human hands the task over completely and
  walks away (named after the brute-force "just keep going until done" technique). A ralph loop is **only
  acceptable on safe-by-design work**: if the agent running unattended *cannot* advance a BHH BadGoal, the
  human can safely not watch. So it's the opposite of CF — it trades approval prompts for trust earned by
  safe ops + `--audit`. Reducing dangerous ops is exactly what makes ralph loops possible.
  **Smart-zone caveat:** a long *unattended* ralph loop accumulates context and can drift into the *dumb
  zone* while the human isn't watching → degraded decisions on autopilot. So "safe to walk away" needs
  **context hygiene** (checkpoint + compact, fresh subagents for big sweeps), not just safe ops.
- **Safe by design** — a workflow where the agent's efficiency goals AND the human's no-CF goal are met
  **without advancing any BHH BadGoal** — achieved by *reducing the number of dangerous operations that
  exist* (so few approvals are needed and those that remain are narrow, statically analyzable, and
  reviewable), rather than relying on the human to vigilantly catch danger at every prompt.

## Development approach — case-study-driven; start specific, generalize-ready
- **Case-study-driven:** genscalator is distilled from doing *real* work, not designed in the abstract. A seed case
  study is **introprog/autotranslate**
  (https://github.com/lunduniversity/introprog/tree/master/autotranslate) — the tools + workflow earn
  their place by solving an actual project.
- **Open research log:** investigations — plans *and* results that lead to genscalator improvements — live
  in [`../research/`](../research/), in the open for the community (e.g. *how the `scala-style` skill should
  self-consciously evolve from agent use*). **Hard rule:** research notes stay **out of agents' daily
  working context** — referenced from no `AGENTS.md` core or task-loaded skill, read only when *explicitly*
  investigating — so exploration never costs context/CF overhead in ordinary use (cf. *smart zone* above).
  **Exception — contribution mode:** when an agent is about to *propose a contribution back* to genscalator
  (or has spotted a possible improvement), it **should** first check the README **roadmap** and `research/`,
  to align with on-going work and avoid duplicating or contradicting it. Findings that generalize graduate
  into tool/doc/skill edits + a release.
- **Start specific** (e.g. Claude Code + Opus) but **don't pin the first-attempt environment more
  specifically than needed** — keep the portability goal (above) in view from the start. The tools
  themselves (scala-cli scripts + the `tt` launcher) are already agent-agnostic; the agent-specific parts
  are the *harness integration* (allowlist, memory, skill packaging), which we keep thin and documented.
- **Long-term:** genscalator docs should include how-to for using it with **Claude, Codex, and open-source
  frameworks/models** (opencode, kilo, local models) — so adopters aren't forced into one vendor.

## Tool safety flags (roadmap — operationalize "safe by design")
A genscalator tool *declares* where it sits in the threat model, so the human can grant trust cheaply:
- **`--safe-mode`** — runs under capture-checking / purity constraints; the compiler rejects accidental
  side effects. Default for pure tools (text/file analysis).
- **`--sandboxed`** — restricts the tool to a declared scope (e.g. reads only under a given dir, no
  network) → makes a blanket "always allow" low-stakes.
- **`--audit`** — emits a record of what the tool touched (files read/written, subprocesses run) →
  enables trust-but-verify; the bridge between *faster* and *safer*.

## Open questions
- Exact flag semantics + defaults; do `--safe-mode`/`--sandboxed` compose, and which is default per tool class?
- Is there a 4th concern (e.g. resource limits)?
- How do these flags surface in the allowlist so "always allow `tool --sandboxed *`" is genuinely safe?

## Things to read/learn
1. **Scalex** — *adopted companion* (no longer just to-read). https://nguyenyou.github.io/scalex/
   (depth: https://github.com/nguyenyou/scalex). Symbol-aware (AST) querying of Scala code — far more
   precise than grep/regex/awk; the semantic-Scala companion to the textual `tt` tools. See
   [`tool-selection.md`](tool-selection.md) and [`../tools/README.md`](../tools/README.md#companion-scalex).
2. **Metals MCP** — *adopted complement* (heavier tier above scalex). https://scalameta.org/metals/docs/features/mcp/.
   Presentation-compiler + build-server intelligence over MCP: real diagnostics, inferred types, run tests,
   refactor (Scalafix), format, dep/build queries. Use when scalex's source-level view isn't enough; note the
   read-only-vs-effectful safety split. See [`tool-selection.md`](tool-selection.md).
3. **Scala Capture Checking (CC)** — focus FIRST on **Safe mode**:
   https://www.scala-lang.org/api/3.x/docs/experimental/capture-checking/safe.html
   (overview: https://www.scala-lang.org/api/3.x/docs/experimental/capture-checking/index.html).
4. **Paper: making agents safer with capture checking** — https://arxiv.org/abs/2603.00991. For
   *ideas/examples*; syntax has evolved, don't copy verbatim.
