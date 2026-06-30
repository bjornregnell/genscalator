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
  budget). See research `communication-bandwidth.md`.
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
- **Smart-zone ceiling (L)** — the fraction of the context window an agent can fill *before* it crosses from
  the **smart zone** into the **dumb zone**: the **usable working-context ratio**. Names the "X%" boundary
  in *Smart zone / dumb zone* as a quantity — if L ≈ 0.3, the agent stays sharp up to ~30% fill and
  **context rot** dominates beyond it (so a 1M window has a *usable* budget of ~300k, not 1M — *effective
  context ≪ advertised context*). L is **model- and task-dependent** and currently a **blind spot**: an
  agent can read its fill % (a `token-usage`-style instrument) but **not its own L**, so it can't tell how
  close to the edge it is (see research `smart-zone-ceiling.md`). Practical use: compare live fill % against
  an estimated L and **brake** (checkpoint + compact) as fill nears L — not as it nears 100%. The region
  below L is the *smart-zone budget*; alias *usable-context ratio*.
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
  *token-budget-awareness* (`research/token-budget-awareness.md`): rot raises velocity; velocity/acceleration
  are how the agent *notices* before halting.
- **Compact dance** — the deliberate **hand-off ritual across a context compaction**, so crossing it costs
  little of what matters. Context compaction (summarizing the transcript to reclaim window space) is the main
  smart-zone hygiene move — but a naive compact *loses* live state (decisions just made, the exact next step,
  paths, in-flight reasoning) because the summary is lossy. The dance makes the loss bounded and recoverable.
  **Four steps:** **(1) save** — the agent **durably writes** the state a summary would blur: a resume/plan
  note (decisions, next-step order, file paths, open threads) **plus** any persistent **memory** entries,
  *committed* where the repo allows; **(2) prompt** — the agent hands the human a **paste-after-compact
  prompt** that points at those durable artifacts and names the next action; **(3) compact** — the *human*
  triggers the compaction; **(4) paste** — the human pastes the prompt, re-seeding the fresh context from the
  durable state, not from the lossy summary. Initiated when fill nears the **smart-zone ceiling (L)** (read it
  off a `token-usage`-style instrument), *before* **context rot** sets in. **Safe-recovery invariant:** the
  truth lives in **committed files + memory**, never only in the chat — so even a total context loss (crash,
  cap halt, a summary that drops a thread) recovers by reading the resume note. The pasted prompt is a
  *convenience*; the durable artifacts are the *guarantee*. Steps 1–2 are the agent's, 3–4 the human's: it is
  a **human↔agent** protocol (cf. *communication bandwidth*), and the cousin of a **ralph loop**'s
  checkpoint+compact — but human-triggered at a chat boundary rather than autonomous. Initiate at the
  **compact trigger** (next).
- **Compact trigger** — the context-fill level at which the agent should **proactively propose the compact
  dance**, rather than waiting until it is already degrading. Set at a **safety margin below the smart-zone
  ceiling**: **fill ≥ 0.8·L** (with L≈0.3, ≈24% of a 1M window). The 0.8 margin exists because the *dance
  itself costs turns* (save + write the resume prompt) — you want it to **complete inside the smart zone**,
  not begin at the edge of the dumb zone. It is the named behavioral bind on the `⚠ approaching` band a
  `token-usage`-style instrument already reports (`fill/L ≥ 0.8` → warn; `≥ 1.0` → over). **Agent
  responsibility:** periodically read fill/L (cheap, read-only) and, on first crossing the compact trigger,
  *suggest the dance* — not silently push on (that is how a long run drifts into **context rot**). Distinct
  from L (the *boundary*) and from the dance (the *ritual*): the trigger is *when to start the ritual*.
- **Habit (agent)** — a *learned default strategy* the agent reaches for. Examples: "munge text with
  grep/awk/sed", "count by piping to `wc -l`", "wrap work in `cd … && … > log`".
- **Reflex (agent)** — a *fast, sub-deliberative trigger* inside a habit, fired before thinking.
  Examples: appending `| head`/`| wc -l`; adding `2>/dev/null`; guessing a glob `"*.scala"`.
  **Not a synonym for habit:** habit = the strategy, reflex = the twitch. They need *different* fixes —
  habits → change the default tool; reflexes → make the typed path frictionless so it becomes the new reflex.
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
