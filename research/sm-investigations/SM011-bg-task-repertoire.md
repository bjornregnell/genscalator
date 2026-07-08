# SM011 - The agent's background-task (bg-task) repertoire

*Deliverable for SM011 (agent-solo, ungated). Defines the durable repertoire of safe, token-efficient tasks the agent runs on its own when the AFK menu is empty and BR is away, plus the cadence and the safety gate. Foundation that SM014 (auto-bg) and the settings-hardening bg task consume.*

## 1. What a bg-task IS (and is not)

Two distinct kinds of work live under the 🤖 (agent-solo) banner:

- **SM candidates** - BR-triggered, sit on the PIN-BOARD menu with `[ ]` tickboxes, banded by AFK-safety; BR picks one by number or band and cues `go`. The agent does not start them unprompted.
- **bg-tasks** (this doc) - **agent-initiated**, run to keep bringing value to the genscalator goal during genuine idle time (AFK menu empty, BR away). No human picks them; the agent selects from this repertoire under the safety gate below.

There is **no token-usage gauge yet** (assume none until a real `tt usage` tool exists - see Backlog). The consequence is the operating rule: **cheap-first, value-gated** - never spend more than the work is worth, and prefer the cheapest useful task.

## 2. The repertoire (cheapest first)

Each entry: what it does · why it has value · rough cost · AFK-safety notes.

1. **Substrate consistency sweeps** - scan PIN-BOARD / MEMORY.md / README for internal drift (stale rows, dead pointers, IDs referenced but undefined, duplicated facts). *Value:* the substrate is the human's and the agent's shared memory prosthesis; drift there is expensive later. *Cost:* cheap (reads). *Safe:* read-only; propose fixes, apply only the unambiguous agent-owned ones.
2. **Memory / index-rot checks** - verify every `MEMORY.md` index line points to a real file, flag stale or contradicted memories, catch duplicates. *Value:* directly fights the memory-loss failure mode (cf. `blixten-box-flaky`, logged after CO4 forgot a pinned fact). *Cost:* cheap. *Safe:* read; index edits are careful + agent-owned.
3. **Box health checks** - disk free, git sync status (unpushed commits / dirty trees across the repos), and - given `blixten-box-flaky` - a memory-pressure / health glance. *Value:* catches an unpushed commit or a filling disk before it bites; the flaky box makes this non-trivial. *Cost:* cheap. *Safe:* read-only status commands (bare, allowlisted).
4. **Shipped-artifact health** - link-audit the live blog, re-run the tool test suites, confirm the deploy set still renders clean. *Value:* regressions surface without waiting for BR. *Cost:* cheap-to-medium (test runs are small). *Safe:* read/verify; no deploy.
5. **PB stocking** - keep the three PIN-BOARD categories (agent-solo / BR-solo / together) stocked with plausible, well-scoped TODOs so the menu is never empty when BR returns. *Value:* the standing "keep-the-AFK-menu-stocked" habit. *Cost:* cheap. *Safe:* agent-owned board writes.
6. **WR-data mining for `tt` tool candidates** - scan the friction logs (`research/wr-data/`) for recurring guard-trips / tool-gaps that a typed `tt` leaf could close. *Value:* feeds the item-D tool pipeline from real evidence. *Cost:* medium (reading + synthesis). *Safe:* read + a proposal note.
7. **Code review, most-important-code-first** (the SM003 `scala-code-review` skill) - adversarially review the highest-value shipped code. *Value:* real, but **strictly value-gated** (review value must exceed tokens spent). *Cost:* medium-to-expensive. *Safe:* read-only.
8. **Safe refactorings during BR-AFK** - fearless-refactor only where a **paired test** + the compiler + metals guard the change. *Value:* real but the riskiest entry. *Cost:* medium. *Safe ONLY IF:* a test pins behaviour, the change is committed per unit, and compiles stay **small** (blixten OOM-crashes under memory pressure - no large/parallel builds).

## 3. Cadence

With no token gauge: **run the cheap sweeps (1-5) first and often**; escalate to the expensive entries (6-8) only when the cheap sweeps are clean AND the value is clear AND the token posture is spending-mode. A future `tt usage` gauge (Backlog) would let this be budget-aware instead of heuristic; until then, cheapest-useful-first is the discipline.

## 4. The AFK-safety gate (every bg-task must pass ALL)

- **Prompt-race-free** - bare, allowlist-matchable commands only; nothing that can raise a confirmation prompt that steals BR's input focus while he is (or might be) typing.
- **Commit + push each completed unit** - and on the flaky box (`blixten-box-flaky`) never leave a half-done state; a hard crash is unrecoverable until BR power-cycles.
- **OOM-safe** - small compiles, no large concurrent sub-agent fleets; the box crashes under memory pressure.
- **Reversible / read-heavy** - no live deploys, no edits to BR-authored files, no settings/allowlist changes, no unreviewed memory-index rewrites.
- **Value >= cost** - the whole point; if unsure, skip and stock a TODO instead.

## 5. Ties

Consumed by **SM014** (auto-bg = the gated, auto-triggered version of this repertoire) and the settings-hardening bg task. Composes with **SM015** (sub-agent fleets - a bg-task may fan out, within the OOM ceiling), the **token-budget-modes** (spending / normal / saving gate the expensive entries), the standing **keep-the-AFK-menu-stocked** habit, and **`blixten-box-flaky`** (the OOM + commit-per-unit constraints above). Sharpened later by a real `tt usage` gauge.
