# Probing the harness guard, and building our own safer guard on top

- **Question:** Two coupled questions.
  1. **Introspect:** can a human+agent pair *jointly* figure out **why and how** Anthropic's Claude Code
     environment **confirmation guard** fires — the agent instruments/probes, the human watches the TUI — so
     we understand the actual mechanism, not just our behavioural model of it?
  2. **Improve:** can we then build **our own guard layered on top of the harness env** that is *both* more
     **precise** (fewer false-positive confirmations) *and* **stronger** (catches unsafe actions the harness
     lets through), plausibly using **Scala's experimental Safe mode / capture checking** so a `tt` command is
     *provably* safe and runs silently?
- **Why it matters:** this is the safe-by-design thesis at its sharpest. The harness guard is **sound, not
  complete** (see [[confirmation-guard-static-analysis]]): it prompts whenever it *cannot prove* a shell
  action safe, so every prompt is a true positive (agent reflex) or a false positive (benign notation it can't
  disambiguate). A guard we understand and control could cut confirmation fatigue **and** raise the safety
  floor at once — "be safe" and "avoid needless prompts" are the same goal from two sides. It also feeds the
  first paper (a concrete, buildable safety artifact).
- **Method idea (BR, 2026-07-01) — drive a *sub-session* to probe empirically:** rather than reason about the
  guard only from our own session, **drive another Claude session via stdin/stdout** (a second instance of the
  *same* model, fresh context) and feed it candidate commands, observing what trips ITS guard. This turns guard
  characterization into a **repeatable experiment** instead of anecdote. Feasibility looks HIGH — Claude Code
  has a headless/print mode (`claude -p`) with structured `stream-json` I/O and an Agent SDK, which is exactly
  a stdin/stdout drive-loop; the probe harness would be a `tt` tool that sends prompts and records
  guard-trip / no-trip verdicts. (Verify exact flags before building; caveat: a driven sub-session may run
  under different permission settings than an interactive TUI, so we must confirm the guard behaves the same
  headless as on-screen — part of question 1.)
- **Plan (proposed):**
  1. **Characterize** the guard: enumerate command shapes and record trip/no-trip (both by hand in the TUI and
     via the sub-session driver); reconcile with the sound-not-complete model in [[confirmation-guard-static-analysis]].
  2. **Design** the custom guard: a submit-time hook + `tt` allowlist that (a) normalizes known benign
     false-positive notations before the harness sees them (the `<N-M>` glob / `\n#` / `$VAR` family) and
     (b) proves `tt` commands safe. Explore **capture checking / Safe mode** (see `docs/foundations.md`:
     capture checking; the `scala-style` skill's "Safe-mode-ready") so effect-typed `tt` tools are
     *statically* safe → silent by construction.
  3. **Measure** against the `wr-data/` confirmation ledger: does the custom guard eliminate real prompts
     without admitting an unsafe action?
- **Status:** open (proposed 2026-07-01, BR). Feasibility RESOLVED (2026-07-01): HIGH, but the *sensor* must be
  the Agent SDK `canUseTool` callback, NOT raw CLI stream-json (which blocks instead of emitting an event).
- **Findings (2026-07-01, via claude-code-guide):**
  - Raw headless `claude -p --output-format stream-json` does **not** surface a distinct permission-request
    event; an unapproved tool **blocks** stdin/stdout under `--permission-mode default` (measurable: it hangs).
    So raw CLI piping is a poor guard sensor.
  - Permission/guard evaluation is **identical headless vs interactive** (same hooks → deny → ask → mode →
    allow → fallback flow); only the prompt *surface* differs. ⇒ a probe CAN measure the same guard the user
    sees. Good.
  - permission modes: `default` (writes/cmds prompt) | `acceptEdits` | `plan` | `auto` (Sonnet-4.6+ classifier)
    | `dontAsk` (silent deny of anything not allow-listed) | `bypassPermissions` (auto-approve).
  - The clean, documented sensor is the **Agent SDK `canUseTool` callback** (TS/Python): fires BEFORE tool
    execution with `(toolName, input, ctx)`; can log / allow / deny → this IS the guard-trip signal. The raw-CLI
    analogue is `--permission-prompt-tool <mcpTool>` (routes decisions to an MCP tool) but is underdocumented.
  - Undocumented gaps: the `--input-format stream-json` multi-turn *input* schema; `--permission-prompt-tool`
    wiring. So multi-turn raw-CLI driving is not doc-supported — another reason to use the SDK.
- **Decision — architecture (agent, 2026-07-01):** the permission **sensor** is a thin **Agent-SDK
  `canUseTool` harness** (Python/TS), not raw CLI piping. The Scala `claudeHeadlessDriver` / `tt` layer
  **orchestrates + analyzes** (spawns the SDK harness as a subprocess, feeds it the probe corpus, collects the
  JSON verdicts); **cask server = phase 2**. So: *subprocess-first, SDK-`canUseTool` sensor* — NOT the pure
  os.proc-drives-CLI path sketched above (kept for non-permission probes only). Caveat to verify empirically:
  confirm `canUseTool` engages the SAME static bash-safety classifier the interactive TUI uses (docs say the
  eval flow is identical; test on the false-positive corpus before trusting it).
- **What shipped:** (nothing yet — prototype pending BR go)

## Scala abstraction: `claudeHeadlessDriver` (design sketch, BR 2026-07-01 — "fabeling")
Wrap the headless call in a **typed Scala API**, not ad-hoc `claude -p` shell, so probing is reproducible and
composable:
- `def claudeHeadlessDriver(prompt, model, permissionMode, …): Result` — spawns the CLI, feeds stdin, parses
  the `stream-json` stdout into **typed events** (`AssistantText`, `ToolUse`, `PermissionRequest`, `Result`).
  No `curl`; use **os-lib `os.proc`** for the subprocess and **scala `requests`** only if we go the server
  route.
- Two deployment shapes:
  - **(i) direct subprocess** — `os.proc` pipes; simplest; one call = one CLI process, or a *persistent*
    process driven multi-turn via `--input-format stream-json` on stdin. Start here.
  - **(ii) cask HTTP server** (mirrors **modly**) — a long-lived server owns the claude session(s); clients
    (our `tt` tools, or even OTHER claude sessions) POST prompts via `requests` and get JSON back. Buys
    multiplexing, queueing, a stable endpoint, session pooling, cross-machine use (probe from the laptop, run
    on bjornyx). Heavier → phase 2. Same decoupling modly gives model-serving: "how we talk to claude" split
    from "what we probe".
- Determinism: pin the permission mode + model so a probe suite is repeatable; record model name/version in
  each `Result` for provenance (same discipline as modly's cache-provenance).

## Model portability & the Fable-5 question (BR 2026-07-01)
Open risk: the confirmation guard is **Anthropic's**, not ours — a new model/release (e.g. **Fable 5**) may
ship a **different env/guard**, obsoleting an introspection pinned to today's behaviour. Two consequences:
- **Argues for a guard WE own** (the custom-guard goal above): if our safety layer lives in genscalator
  (submit-time hook + `tt` allowlist + capture-checked `tt`), it is **model-independent** and survives an env
  change; the harness guard becomes just one input, not the foundation.
- **Turn the risk into an experiment:** use `claudeHeadlessDriver` to run the SAME probe suite with
  `--model sonnet` / `--model haiku` (and future models), measuring **how the genscalator plugin changes each
  model's behaviour** — a cross-model portability/regression harness for the plugin. When Fable 5 lands,
  re-run the suite to auto-detect env/guard drift, so "did the new model break our safety assumptions?"
  becomes a one-command check instead of a surprise.

Related: [[confirmation-guard-static-analysis]] (when/why the guard fires; sound-not-complete),
[[instruction-adherence-decay]] (why exhortation fails → structural guard is the fix),
[[inference-time-learning]] (structure > memory > instruction hierarchy), `docs/foundations.md`
(safe by design, capture checking). Evidence source: [[README]] → `wr-data/`.
