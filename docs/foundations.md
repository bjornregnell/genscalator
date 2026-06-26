# genscalator — foundations: goals, stakeholders & glossary

> Canonical foundations doc for **genscalator / GS** — *"Power tools for agents: smarter, safer, faster"*.

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
- **genscalator** / **GS** = the product AND the repo; say **"GS repo"** to disambiguate.
- Develop in your own working project; **publish generalized, project-agnostic** results to the GS repo.

## Stakeholders
- **Human developer** ("human") — you and future users of GS.
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
- **G: portability** — GS targets *any* capable coding agent, not one vendor. We develop with Anthropic
  **Claude Code + Opus** now, but **avoid agent-specific choices unless necessary** and aim to make GS
  usable across **frontier tools (Claude, Codex)** AND **open-source agent frameworks/models** (e.g.
  opencode, kilo, local models). The "agent" stakeholder is tool-agnostic.

### Human goals
- **G:** avoid **confirmation fatigue (CF)** — few, meaningful approval prompts, not a stream of rubber-stamps.
- **G:** avoid **review overload** — agent output stays reviewable (small, typed, idiomatic).
- **G:** **trust-but-verify** — be able to audit what a tool actually did (→ `--audit`).
- **G:** **contribute to open source** (the GS repo) so others reuse the workflow.
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
- **Token efficiency (TE)** — achieving a task with fewer model tokens (input + output). A committed,
  compiled tool beats re-emitting brittle bash every time. **Two distinct pressures, usually aligned but
  not always:** (a) **$cost** — total tokens billed; (b) **smart-zone** — keeping *working* context small
  so quality stays high (see *smart zone / dumb zone*). (b) is the one that bites silently. GS tools serve
  both: a tool is a small stable thing you *call*, vs re-deriving bash + re-reading its output, which
  bloats context.
- **Smart zone / dumb zone** — the region of context-window fill where the agent reasons well ("smart")
  vs. where it degrades ("dumb") — even though tokens remain below the hard limit. The boundary (X%) is
  often FAR below 100% — possibly ~30% on large-context models (aka **context rot** / "lost in the
  middle" / *effective context ≪ advertised context*). Implication: keep the *working* context small, not
  merely under the limit — offload to compiled tools, use fresh subagents for big sweeps, checkpoint + compact.
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
- **Case-study-driven:** GS is distilled from doing *real* work, not designed in the abstract. A seed case
  study is **introprog/autotranslate**
  (https://github.com/lunduniversity/introprog/tree/master/autotranslate) — the tools + workflow earn
  their place by solving an actual project.
- **Start specific** (e.g. Claude Code + Opus) but **don't pin the first-attempt environment more
  specifically than needed** — keep the portability goal (above) in view from the start. The tools
  themselves (scala-cli scripts + the `tt` launcher) are already agent-agnostic; the agent-specific parts
  are the *harness integration* (allowlist, memory, skill packaging), which we keep thin and documented.
- **Long-term:** GS docs should include how-to for using it with **Claude, Codex, and open-source
  frameworks/models** (opencode, kilo, local models) — so adopters aren't forced into one vendor.

## Tool safety flags (roadmap — operationalize "safe by design")
A GS tool *declares* where it sits in the threat model, so the human can grant trust cheaply:
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
1. **Scalex** — https://nguyenyou.github.io/scalex/ (depth: https://github.com/nguyenyou/scalex).
   Type/compiler-semantic querying of Scala code — far more precise than grep/regex/awk.
2. **Scala Capture Checking (CC)** — focus FIRST on **Safe mode**:
   https://www.scala-lang.org/api/3.x/docs/experimental/capture-checking/safe.html
   (overview: https://www.scala-lang.org/api/3.x/docs/experimental/capture-checking/index.html).
3. **Paper: making agents safer with capture checking** — https://arxiv.org/abs/2603.00991. For
   *ideas/examples*; syntax has evolved, don't copy verbatim.
