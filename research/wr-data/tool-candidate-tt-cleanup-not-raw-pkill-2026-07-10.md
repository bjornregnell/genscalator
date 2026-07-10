# Tool candidate: end-of-session process/server cleanup should be a typed `tt` tool, not raw pkill (2026-07-10)

BR-flagged ("should be a tt **box** tool" - i.e. a subcommand of the existing `tt box` host/box-management tool, `box.scala`). Stopping local `tt serv` web
previews and killing lingering scala-cli / Bloop JVMs is a **recurring end-of-session cleanup**, done ad hoc today
(`scala-cli bloop exit` + a would-be `pkill`). The genscalator thesis applies: don't reach for raw `pkill` / `kill`
(general executors, un-allowlistable, dangerous - they can kill *anything*); build a **narrow, audited `tt` cleanup
leaf**.

## Candidate
A **`tt box` subcommand** (e.g. `tt box clean [host]` / `tt box stop`), extending the existing box-management tool
(`box.scala` - host-pinned remote ops on blixten / bjornyx), that: (a) stops all `tt serv` previews this project
started (tracked + scoped, never arbitrary PIDs), (b) runs `scala-cli bloop exit`, (c) reports what it stopped and
roughly how much it freed. **DECLARED narrow semantics** (only genscalator-spawned scala-cli/serv JVMs) =
allowlist-safe, unlike `pkill`. It fits `tt box` because that tool already owns "operate on a named box".

## The Bloop-restart catch (operationally important)
Any `tt` / scala-cli command **re-spawns the Bloop build server**, so a cleanup (tool or manual) must be the
**LAST action** - running it and then a `tt git commit` undoes it. Observed today: a commit printed "Starting
compilation server" seconds after `scala-cli bloop exit`. A `tt` cleanup tool should say so, and ideally be the
documented final step of a session.

Ties: OD02 (`tt copy`/`tt move` shape - same "narrow typed tool over raw shell" pattern), SM031 (`tt find` -
sibling tt-leaf), [[never-allowlist-interpreters]], the safe-by-design thesis, [[blixten-box-flaky]] (memory
hygiene motivates it), SM039 (the cost/TE angle - clean box for cheaper runs).
