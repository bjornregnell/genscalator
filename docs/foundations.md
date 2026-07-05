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

**A→Z (Ctrl-F):** AFK dance · agent (CO4 / CF5) · AT · Authority anchor · Ballgame · BR · Comms shorthand · Communication bandwidth · Compact dance · Compact trigger · Confirmation fatigue (CF) · Consistency dance · Consolidation point · Context rot · Context usage / fill · Corroboration asymmetry · Coupled-system capability · Dangling pointer · echt / äkthet · Edit dance · Extrinsic-volatile plasticity · Go dance · Habit · Hardening dance · Index rot · Memory hygiene · Note dance · Order stability · Pin dance · Pinboard · Prosthetic habit · Quick / deep cues · Ralph loop · Reach (access horizon) · Reflex · Rest dance · Review overload · Safe by design · Smart zone / dumb zone · Smart-zone ceiling (Z) · Solo-safe · SSG · Structural vs knowledge safeguard · Substrate · Substrate-as-multiplier · Thriller state · Token acceleration · Token efficiency (TE) · Token velocity · WR

### Roles and cases
*BR and the agent are the **roles** (stakeholders, above). **WR** is the research program; **AT** and **SSG** are its **cases / units of analysis** — the object-level projects during which workflow data is collected. (Terminology per* Case Study Research in Software Engineering: Guidelines and Examples*, Runeson, Höst, Rainer & Regnell, Wiley 2012, §3.2.3 "Cases and Units of Analyses" — of which BR is a co-author.)*
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
- **Comms shorthand (human↔agent)** — a shared vocabulary of standard chat/dev **acronyms** both roles emit
  and parse *without expansion*, a direct **communication-bandwidth** + **TE** lever: fewer tokens (and less
  human typing) carry the same intent, in both directions. Distinct from the project's *coined* terms
  (**CF**, **TE**, **Z**, **WR**, **AT**, **BHH**, **BadGoal**, **ralph loop** …) which name domain concepts —
  this entry is generic conversational glue. Both sides may use these freely; when a token is genuinely
  ambiguous in context, expand it once. Common set:
  - *Presence / status:* **BRB** be right back · **AFK** away from keyboard · **OOO** out of office ·
    **EOD** end of day · **ETA** estimated time of arrival · **WIP** work in progress · **RN** right now ·
    **TODO** to do (left) · **NM** never mind.
  - *Opinion / agreement:* **WDYT** what do you think · **IMO / IMHO** in my (humble) opinion ·
    **LGTM** looks good to me · **SGTM** sounds good to me · **ACK / NACK** acknowledged / rejected ·
    **+1 / -1** agree / disagree · **FWIW** for what it's worth · **OTOH** on the other hand · **NBD** no big deal.
  - *Meta / reference:* **TL;DR** short summary · **FYI** for your information · **ICYMI** in case you missed it ·
    **PTAL** please take a look · **AFAICT** as far as I can tell · **IIRC** if I recall correctly ·
    **AFAIK** as far as I know · **WRT** with respect to · **WDYM** what do you mean · **IDK** I don't know ·
    **RQ** research question · **N/A** not applicable · **e.g. / i.e.** for example / that is.
  - *Dev-flavored:* **PR** pull request · **MR** merge request · **RC** release candidate · **repro** reproduce ·
    **rebase / squash** git ops · **YAGNI** you aren't gonna need it · **DRY** don't repeat yourself.
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
  [as in *proper fraction*].) See `skills/blog-assistant`, blog README "Authorship & voice", and
  `research/027-steering-doc-design-tension.md`.
### Context rot and the smart zone
- **Token efficiency (TE)** — achieving a task with fewer model tokens (input + output). A committed,
  compiled tool beats re-emitting brittle bash every time. **Two distinct pressures, usually aligned but
  not always:** (a) **$cost** — total tokens billed; (b) **smart-zone** — keeping *working* context small
  so quality stays high (see *smart zone / dumb zone*). (b) is the one that bites silently. genscalator tools serve
  both: a tool is a small stable thing the agent *calls*, vs re-deriving bash + re-reading its output, which
  bloats context.
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
### Dances and handoffs
*Human↔agent protocols — each has **≥2 interlocking steps** (≥1 human, ≥1 agent), else it's just a cue. The **compact trigger** and **consolidation point** below are the timing rules for the compact dance.*
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

  Initiated when fill nears the **smart-zone ceiling (Z)** (read it off a `token-usage`-style instrument),
  *before* **context rot** sets in. **Safe-recovery invariant:** the truth lives in **committed files +
  memory**, never only in the chat — so even a total context loss (crash, cap halt, a dropped thread) recovers
  by reading the resume note; the pasted prompt is a *convenience*, the durable artifacts the *guarantee*. A
  **human↔agent** protocol (steps 1–2 agent, 3–4 human), the cousin of a **ralph loop**'s checkpoint+compact
  but human-triggered at a chat boundary. Initiate at the **compact trigger** (next).
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
- **AFK dance** — the human↔agent protocol for handing off **autonomy for a bounded window** to work a **solo block**,
  turning that stretch into delegated progress. The human is often **away from keyboard** ("**AFK**") but need not be —
  they may be **present-but-busy** on other work and still want the agent running solo on a menu. *(Because of that,
  renaming this the **solo dance** is proposed — see OD9; "AFK" then names the strict **mode** where the human is
  absent so no prompt may race them.)* Built around an **AFK/solo menu** (its artifact): the standing list of scoped
  candidate tasks.
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

  **What makes a good AFK job** (the menu's admission test): **autonomous** (no mid-run human decision) · **low
  forced-confirmation risk** (no build-pipeline glue / new-domain web / `~/.claude` writes — see
  [[guard-against-forced-confirmations]]) · **reversible & non-outward-facing** (no publish/release) ·
  **verifiable after the fact** · **bounded**. Items failing these are labelled *needs-human* or *outward-facing*
  and stay off the autonomous set.

  **Always-stocked-menu invariant:** the agent **continuously nominates** AFK-suitable-but-not-now tasks into the
  menu *during active sessions* (a `pin` into the AFK-menu artifact), so a ready menu is at the human's fingertips
  the moment they trigger the dance — the human should never have to ask "what could you do?" from a blank slate.
  Sibling of the *compact / rest* dances (which hand off **context**); the AFK dance hands off **autonomy** (cf.
  *task-autonomy negotiation*, *ballgame* ↔ *ralph loop*). The menu lives in the human's **pinboard**; the
  delegation UX itself is a WR study subject.
- **Solo-safe** — a property of an AFK task: the agent can complete it **entirely alone, with zero
  forced-confirmation risk and no irreversibility**. Concretely: only `Read` + `Edit`/`Write` inside allowlisted
  repos + **bare allowlist-matchable** `tt` / `git -C` / `scala-cli` calls (no pipes/redirects/`&&`, no
  build-pipeline glue, no new-domain web fetch, no writes that would prompt), **reversible** (git-tracked,
  non-outward-facing — no publish/release), and **verifiable after the fact**. The **strict subset** of the AFK
  dance's *good-AFK-job* test that *additionally* guarantees **no prompt can race the human while they're away**
  (the AARGH lesson — [[guard-against-forced-confirmations]]). "Give me a solo-safe menu" = filter the AFK menu to
  exactly these; the residue (build glue, web to new domains, settings, publish) is *needs-human* / *outward-facing*.
- **Edit dance** — the human↔agent protocol for **correcting a just-sent message** without derailing the turn.
  Cause: a harness input-race — pressing ↑ to edit an already-Entered message is **too late** once the agent has
  begun processing it, so the correction posts as a **new** message (a double-post).
  - **Human step:** don't fight the race — **add a new message**, and for a simple typo send a terse `edit:` note.
    Two forms, both valid: the **full `edit: wrong -> right`** (explicit fix), OR a **bare `edit: <token>`** (just the
    mistyped word, or even a fragment) — which only **flags that a typo happened** and leaves it to the agent to work
    out what was meant and whether it matters.
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
- **Pin dance** (formerly "note dance", then briefly "etch"; formally the *longitudinal externalisation* dance) — the
  **continuous** human↔agent ritual that runs *across a whole session*, the streaming counterpart to the discrete
  **compact dance**:
  - **Human step:** cue `pin:` ("save this durably — you pick where unless I say") or `WR data:` (pin to the WR corpus).
  - **Agent step:** persist that content out of the volatile context (substrate #1) into **durable committed items**
    (memory, `wr-data/`, notes, blog, this glossary) — **choosing the home** and **questioning whether it earns a
    durable slot**, moving each insight *down the substrate hierarchy as it arises*.

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
  `note:`/`pin:` govern *memory*, `go` governs *control / authorization*. Variants: "go [AFK] menu" = `go` over a
  menu of scoped tasks; **`go X`** = a scoped go-verb (small curated set — see `research/035-go-verb-vocabulary.md`).
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
- **Rest dance** — a human↔agent dance for **conserving the human**. The explicit human trigger is the cue **`:Z`**
  ("I am **getting** tired" — a *warning*, **not** asleep; a **precursor to the AFK dance**). Chosen for **minimal
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
  state, open decisions, and the **AFK menu**. Two variants seen in this work: a **private single-human** pinboard
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
### Autonomy and safety
- **Authority anchor (human)** — the human as the **non-delegable authority** for decisions the agent cannot
  self-corroborate: permission / security curation, "always-allow" grants, and final verdicts on the agent's own
  claims. The **security twin of the corroboration asymmetry** — the agent proposes / generates, the human authorizes.
  Weakened by the approval-race (see `research/wr-data/harness-ux.md`, the AARGH episode).
- **Ballgame** — the **collaborative** pole of task-autonomy: the human is **in every volley** (each agent step gets a
  human response), opposite the autonomous **ralph loop**. A spectrum, not a binary; the mode is chosen per task (safe +
  self-verifiable → ralph; needs judgment / taste or touches shared source broadly → ballgame). Mis-triage is a
  *confirmation-fatigue* risk (toward ballgame) or a *safety* risk (toward ralph). See
  `research/010-task-autonomy-negotiation.md`.
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
