# SM016 - Tap/inject middle layer: design synthesis + allow/deny model (part c)

*The 🧠 capstone of the harness-integration cluster (SM013/014/016/017). Combines the transport survey ([[SM016a-tap-inject-survey]]), the capture-checking safety result (SM017 v1/v2), and the drafted slash-command whitelist (SM016 b-progress) into a recommended design and the allow/deny model. **Recommends a design; enables nothing** - building any real tap/inject layer is a BR-review autonomy + security escalation.*

## 1. The problem, restated

BR's idea: a middle layer between BR's keyboard and the Claude Code TUI that the super-agent can **tap** (read the input/terminal stream) and **inject** into (issue TUI actions it cannot today: `/context`, `/compact`, other slash-commands). Payoff: the agent could run dance steps it currently cannot - self-trigger a compact, **read its own context %** (killing the Z / compact-trigger blind spot), and supply an idle/wall-clock signal for SM014.

The danger is singular and severe: **a component that can inject arbitrary keystrokes into the agent's own harness is a self-escalation channel.** It could approve its own permission prompts, type `/allow`, or emit free text that collapses the authority anchor (the human approves; a layer the agent drives must never approve for itself). The whole design question is how to get the payoff without opening that channel.

## 2. The three-layer design

Separate the concerns that the naive "tmux passthrough" conflates:

**Layer A - Transport (dumb, byte-transparent).** Per the survey: run `claude` under a **thin transparent pty broker** (or tmux pane) that copies bytes untouched in both directions and taps/injects only at the edges. It does NOT parse or reimplement the TUI - that is what makes it survive harness updates (the drift argument). The transport has **no policy**; it is a pipe with two side-channels (a tap mirror and an inject sink).

**Layer B - Safety (the pure-Scala capability harness).** This is the load-bearing layer and the SM017 result. The inject side is NOT "write bytes to the pty" exposed to the agent; it is a **capability-typed `Injector`** whose ONLY operations are members of a closed, human-owned command vocabulary. Crucially (SM017 v1/v2, verified on nightly): **no `sendRaw(bytes)` / `approve()` / `typeText(String)` method exists on the capability at all** - so the agent cannot emit a permission-approval or free text because there is no method that could, not because a runtime check forbids it. The self-escalation channel is closed **by construction** (compile-time), not by a hand-maintained runtime whitelist that could be bypassed. Safe mode (SM017: blocks `asInstanceOf` / global `println`) prevents forging the capability; `Classified` redaction protects anything tapped.

**Layer C - Policy (the `SlashCommand` enum).** The command vocabulary Layer B exposes is the drafted whitelist (`research/experiments/capture-checking/slash-command-whitelist.md`): a **GREEN** kernel (~10 read-only commands + `/compact`) that the `Injector` may emit, and a large **RED** set (everything settings / auth / external / destructive, and *all* permission-prompt responses and free text) that has no corresponding method. The enum is **policy, not constant** (`/model` is RED-for-this-study but GREEN in general) and is **human-owned** - changing it is a BR-approved edit, and the type system walls off the RED set in one auditable place.

## 3. Why this beats the alternatives

- **vs a raw tmux/pty passthrough with a runtime allow-list:** a byte-stream injector *can* physically emit anything; its guard is a string check that a bug or a clever prompt can slip past. The capability design makes the bad emission **not expressible**. This is the SM017 prize and the reason BR ruled out "just tmux" for the action side.
- **vs parsing the TUI:** any layer that scrapes regions or pattern-matches prompts hard-couples to the current TUI and breaks silently on every harness update (the expect trap). Depending on the **stable keyboard command surface** (slash-commands are the harness's de-facto public API) is drift-robust.

## 4. The tap side (read) - separate risks

Tapping BR's keystrokes pre-submit carries a **privacy/consent** dimension distinct from inject. Design: the tap handler that the agent runs must be **provably pure** (SM017: CC rejects `... ->{inject} Unit` where purity is required) - it may observe and mirror-to-log, but cannot itself trigger an inject, so reading can never become acting. Rendered-screen reads (context %, via tmux `capture-pane -e` or a maintained terminal-state model) are **best-effort, clearly labelled, never load-bearing** for a security decision. What BR types is `Classified` by default.

## 5. What it unlocks (the payoff, once enabled)

- **Self-compact** and other GREEN dance steps the agent cannot run today.
- **Read own context %** - closes the Z / compact-trigger / token-usage blind spot (`research/006` + the token-usage dance); the single most-requested introspection signal.
- **Idle/wall-clock signal for SM014** - a tap on the input stream gives real "human silent for X" (better than the transcript-mtime proxy SM014 recommends).
- Feeds the SM022 live psyche dashboard.

## 6. Recommendation + staged rollout (enabling = BR-review)

Recommended design = the three-layer stack above: **dumb transparent transport + a capture-checked capability harness for the action side + the human-owned `SlashCommand` enum as policy.** Do NOT build it as one step. Staged, each rung BR-gated:

1. **Read-only tap first** (lowest risk): run under the transport, mirror streams to a log, expose *only* a `/context` read-back. No inject capability compiled in at all. Proves the transport + the context-% payoff with zero self-escalation surface.
2. **GREEN-inject kernel** (`/compact` + read-only commands) behind the capability harness, on nightly/CC in an isolated tree (never in the production `tt` toolchain, per the SM033 stable/nightly split).
3. **Only then** discuss widening the enum - and every widening is a human-owned edit.

**Enabling any of this is a real autonomy + security escalation and is BR's decision at every rung.** The safety claim rests on CC (a moving target on nightly), so a production version waits on CC stabilising - exactly why SM033/SM017 keep the CC work in an isolated experiment, not in shipped tools.

## 7. Ties

[[SM016a-tap-inject-survey]] (transport), SM017 (the CC capability-harness result + safe mode + `Classified`), the slash-command whitelist (b-progress), SM013 (session-log = the tap's audit trail), SM014 (idle signal), SM022 (dashboard consumer), the token-usage + compact dances (the payoff), `research/006` (the context-% blind spot). Supersedes nothing; completes SM016 parts (b)+(c) at the design level.
