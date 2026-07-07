# Capture Checking (CC) for agent safety — a deep-dive + proof-of-concept

**What this is.** A worked study of Scala 3 **capture checking** as a *provable*
guard for the SM016 "tap/inject middle layer" crux, and more broadly for
genscalator's safe-by-design goal. It pairs a reading of the Odersky et al paper
with **our own runnable PoCs** on the Scala nightly compiler, each *predicted
then verified*. Written for both BR and the agent to read; the `poc*.scala`
files are the proof-of-concept and usage examples.

**Provenance.**
- Paper: *Tracking Capabilities for Safer Agents* (published at CAIS '26 as
  *Securing Agents With Tracked Capabilities*), Martin Odersky, Yaoyu Zhao,
  Yichen Xu, Oliver Bračevac, Cao Nguyen Pham (EPFL). `arxiv.org/abs/2603.00991`
  (v2, 7 May 2026). Open-source MCP server: `github.com/lampepfl/tacit`. System
  name **TACIT** = *Tracked Agent Capabilities In Types*.
- Toolchain used here: **Scala `3.10.0-RC1-bin-20260707-a4dab1a-NIGHTLY`**, via
  `scala-cli` with `//> using scala 3.nightly`. CC is experimental and a moving
  target; version-pin your findings.
- Current CC docs: `nightly.scala-lang.org/docs/reference/experimental/capture-checking/`.

---

## Part 1 — The paper in one screen

**Thesis: make the *medium* safe, not the *model* trustworthy.** Instead of the
agent issuing raw tool calls (JSON), it expresses its intentions as **Scala 3 +
capture-checking code**. The type checker then makes entire classes of failure
*impossible by construction*, regardless of whether the code was written by a
careful engineer or a prompt-injected / hallucinating model. "Tracked
capabilities can do for agent safety what type safety has done for software
reliability: not eliminate all bugs, but make entire classes of failures
impossible by construction."

**A capability** is an ordinary program value "of interest" (a file handle, a
permission token, a mutable store) that grants an effect through its methods.
Capture checking **tracks, in the type**, which capabilities a value retains. So
the type system can express "this function may touch the file system" or, more
powerfully, "this function is **pure** — it retains *no* capability."

**Three requirements** the execution language must meet:
1. **Capability safety** — capabilities cannot be forged or forgotten
   (object-capability model: authority only by holding a reference).
2. **Capability completeness** — capabilities regulate *all* safety-relevant
   effects, so the agent touches the world only through granted capabilities.
3. **Local purity** — the type system can require a sub-computation to use only
   a prescribed (possibly empty) set of capabilities. This is what stops leaks:
   classified data is processed via a `map` that accepts only pure functions, so
   it *cannot* be exfiltrated.

**Safe mode** (`import language.experimental.safe`) is the extension that makes
requirement 1 real: full Scala has escape hatches (unchecked casts, reflection,
`caps.unsafe`, `@unchecked`) that could "forget"/forge capabilities. Safe mode
forbids them in untrusted agent code, so the guarantees hold even against a
hostile author.

**The TACIT architecture (two layers).** An MCP server any agent (Claude Code,
OpenCode, Copilot) connects to unmodified. The agent emits Scala snippets as
tool-call arguments; three components process them:
1. **Scala 3 compiler** — validates and type-checks the snippet with capture
   checking + safe mode enabled. Only code that type-checks proceeds.
2. **Scala REPL** — a local REPL instance executes the compiled code and manages
   state across turns (stateless or named stateful sessions), then returns the
   result to the agent.
3. **Capability-safe library** — a small typed API (file system, process exec,
   network) that is the **sole gateway** to the real world; referenced by both
   the compiler (for checking) and the REPL (for execution).

Info-flow control uses a `Classified[T]` wrapper: files under classified paths
return `Classified[String]`; its `map` takes only pure functions; its `toString`
is redacted to `"Classified(****)"`; a runtime split gives a **normal output
channel** (what the untrusted cloud model sees) and a **secure output channel**
(what the human sees), so classified plaintext never enters the model's context.
A `chat(prompt, Classified[String]): Classified[String]` lets a *trusted* LLM
reason over secrets while the result stays wrapped.

**Results.** In "classified" mode both tested models hit **100% security** on a
purpose-built adversarial benchmark (0 leaks across 131 trials each), while task
utility stayed high (99.2% / 90.0%). Writing typed capability-safe Scala did
**not** degrade agentic performance vs raw tool-calling (τ²-bench, SWE-bench
Lite); compile-retry overhead was 0.32–7.93% of snippets, mean 1.06–1.39
retries. **Non-goals:** correctness (hallucination/logic bugs remain — safety is
orthogonal to correctness), side channels (timing/termination), and external
commands (which escape the Scala boundary, degrading to the underlying allowlist
— combine with sandboxing for defense in depth).

---

## Part 2 — Our PoCs (predicted, then verified on nightly)

Each file states a prediction in its header; here is what the compiler actually
did. The "reject" files are **meant to fail compilation** — that failure *is* the
proof the guard fires.

| File | Intent | Predicted | Observed |
|---|---|---|---|
| `poc1-basics.scala` | track a capability with `^` | compiles + runs | ✅ compiles + runs |
| `poc2-local-purity.scala` | a leak via a captured capability is rejected | **compile error** | ✅ rejected: `capability leak cannot flow into capture set {}` |
| `poc3-crux-inject.scala` | only whitelisted slash-commands injectable | compiles + runs | ✅ prints `/compact /context` |
| `poc3-crux-reject.scala` | a *pure* tap handler cannot inject | **compile error** | ✅ rejected: `capability inject cannot flow into capture set {}` |
| `poc4-safe-mode-reject.scala` | safe mode blocks a forging cast | **compile error** | ✅ `Cannot use asInstanceOf in safe mode` |

**The two load-bearing compiler messages (verbatim):**

`poc2` (local purity — a classified value cannot be leaked through a
side-effecting closure):
```
Found:    (s: String) ->{leak} String
Required: String -> String
Note that capability `leak` cannot flow into capture set {}.
```

`poc3-crux-reject` (the SM016 crux — a tap handler required to be pure provably
cannot drive an injection):
```
Found:    (cs: Classified[String]) ->{inject} Unit
Required: Classified[String] -> Unit
Note that capability `inject` cannot flow into capture set {}.
```

**Current-API notes learned by trial** (the moving-target tax, for next time):
- `caps.Capability` is now a **sealed** trait — you *cannot* extend it from user
  code (`Cannot extend sealed trait Capability in a different source file`). The
  extendable marker is **`caps.SharedCapability`**. A class extending it is a
  capability, and its type `T` implicitly expands to `T^`.
- `T^` is shorthand for `T^{any}`; the **root capability is now `any`** (renamed
  from `cap`). Capture sets are written `T^{c1, c2}`.
- **Pure vs impure functions:** `A -> B` is pure (empty capture set); `A => B`
  is `A ->{any} B` (may capture anything). `A ->{c} B` captures exactly `c`.
  (Context functions mirror this: `?->` vs `?=>`.) Our `map[U](op: T -> U)` uses
  the pure arrow, which is *why* the leak is rejected.
- **Safe mode is real and merged** in this nightly: `import
  language.experimental.safe` compiles and enforces the safe subset. It rejects
  global `println` (`tagged @rejectSafe`) and `asInstanceOf` (`Cannot use
  asInstanceOf in safe mode`), closing the forge/forget escape hatches.
- **Separation checking** exists as its own CC topic (capabilities not
  interfering) — relevant later for exclusive/mutable capabilities.

---

## Part 3 — How CC solves the SM016 crux

**The crux (from SM016).** A tap/inject middle layer that lets the super-agent
drive the harness TUI (self-`/compact`, read its own `/context`) is a
**self-escalation channel**: if the agent can inject arbitrary keystrokes it
could approve its own permission prompts (e.g. click "allow `python3 *`") or type
`/allow`. A hand-maintained whitelist is fragile. We want a *provable* guard.

**CC gives it, via two independent, composable guarantees:**

1. **API shape (poc3a).** Model the inject channel as a capability whose typed
   API exposes *only* a closed `enum SlashCommand { Compact, Context }` and
   `def sendSlashCommand(cmd: SlashCommand)`. There is deliberately **no**
   `sendRaw(String)` and **no** `approveCurrentPrompt()`. The dangerous
   affordances *do not exist* to be misused — the capability's type *is* the
   security surface.

2. **Capture checking (poc3b).** Require the middle layer's "tap handler" (the
   code that observes BR's keystrokes) to be a **pure** `Classified[String] ->
   Unit`. Capture checking then proves that *no* tap handler can hold the
   `Injector` capability: an attempt gives type `Classified[String] ->{inject}
   Unit`, which does not conform to the required pure arrow. So a tap handler
   **provably cannot drive an injection** — enforced by the compiler, not by
   review or a runtime check.

Add **safe mode** (poc4) so an adversarial snippet cannot cast/reflect its way to
forging an `Injector` it was never granted, and add `Classified` so tapped input
is redacted on the model-facing channel. The self-escalation channel is then
closed **by construction**. That is the prize SM016 hoped for.

**Honest caveats / limits (do not oversell):**
- The guarantee applies to **code that goes through the compiler + safe mode**.
  It constrains the *injection logic* to be written as safe-mode Scala driven by
  the TACIT-style two-layer harness; it is not a magic property of a raw
  tmux/pty byte stream. The middle layer must be *built this way* to earn it.
- **External commands escape the boundary** (paper non-goal): the moment
  injection actually shells out to the real terminal, safety degrades to the
  underlying allowlist. So CC complements — does not replace — the settings
  allowlist and sandboxing (defense in depth).
- **Not correctness.** CC stops unsafe *effects*, not wrong *decisions*. The
  agent could still choose a bad (but safe) slash-command.
- CC is **experimental + moving** (see the API-drift notes above). A production
  design must pin a version and budget for churn.

---

## Part 4 — Why this matters beyond SM016 (genscalator / `tt`)

TACIT's thesis *is* genscalator's thesis, sharpened: the whole repo exists to
replace "approve dense bash" with "safe, compiled Scala tools." CC is the
type-level mechanism that could let `tt` tools carry **provable** effect bounds:
- A `tt` leaf tool could take capabilities (`FileSystem^` scoped to a root,
  `Network^` scoped to hosts) so its *type* documents and *enforces* its blast
  radius — the `requestFileSystem/requestExecPermission/requestNetwork` pattern.
- `Classified[T]` + local purity is a real answer to the leak-scanning we do by
  hand (the grepr leak sweeps): make exfiltration a *type error*.
- The two-layer "agent submits typed code, compiler gates it" design is a
  candidate architecture for a future safe `tt` execution surface.

---

## Part 5 — How to run

```
cd research/experiments/capture-checking
scala-cli run poc1-basics.scala           # compiles + runs
scala-cli run poc3-crux-inject.scala      # compiles + runs (prints /compact /context)
scala-cli compile poc2-local-purity.scala      # EXPECTED compile error (the proof)
scala-cli compile poc3-crux-reject.scala       # EXPECTED compile error (the proof)
scala-cli compile poc4-safe-mode-reject.scala  # EXPECTED compile error (the proof)
```
First run downloads the nightly toolchain (a few minutes); later runs are fast.

---

## Part 6 — Open questions / next steps

- **Read the paper appendices** (H: full `FileSystem` API surface, real agent
  compiler-error examples, system prompts; D: `@assumeSafe`; A: CC intro; C: a
  worked two-turn session; G: CaMeL vs TACIT comparison).
- **Try the real TACIT MCP server** (`github.com/lampepfl/tacit`) to see the
  preamble/REPL wiring end-to-end.
- **Do NOT read the scala3 compiler source to "understand CC."** The impl lives
  in `github.com/scala/scala3` (branches: `github.com/scala/scala3/branches/all`,
  where experimental CC drifts) — a huge, decades-deep codebase and a rabbit
  hole. Per memory `agent-cant-internalize-huge-codebases`: distill via the
  paper + docs and learn from own compiler trials (as done here); note these
  links only for a *targeted symbol lookup* if ever strictly needed.
- **Build a fuller `Injector` harness PoC**: scoped `requestInject(allowed:
  Set[SlashCommand])`, a `Classified`-tapped input channel, and a stateful
  multi-turn session — the minimal SM016 kernel.
- **Separation / stateful capabilities** for a mutable terminal buffer.
- **Whether current LLMs emit safe-mode Scala reliably** for our own tasks (the
  paper says yes for theirs; measure on `tt`-shaped tasks).
