# WR data: `claude --resume` reloads the full context (NO rot reset); a FRESH session + substrate is the real warp (2026-07-13)

Confirmed LIVE during a warp attempt: at ctx-fill **78% (dumb-zone)**, BR exited and ran
`claude --resume <session-id>`. Result: **still 78% dumb-zone**, and the stale `Next:` status-line disinfo **still
showing**. `--resume` reloaded the FULL transcript, so nothing reset — neither the context-rot nor the stale field.

## The finding
- **`claude --resume <id>` = CONTINUITY, not a warp.** It reloads the full session transcript, so context-ROT
  persists (still dumb-zone) and even stale derived state (the `Next:` field) survives. It gives a fresh
  **process** (env/config/token) but does nothing for rot.
- **The real rot-reset "warp" = a FRESH `claude` session** (no `--resume`, no `--continue`) → **blank context** →
  then point it at the substrate ("go read `tmp/resume-prompt.md`"). The substrate (resume-prompt + PB + memories +
  committed work) reconstitutes the fresh agent's orientation from zero.
- This makes the earlier warp thesis testable and its cost EXPLICIT: a fresh session has ONLY the substrate, so
  **post-warp smartness = f(substrate quality)** with no transcript fallback. `--resume` has no memory loss but also
  no rot reset — **the two effects are coupled: you cannot reset rot without losing the transcript.**

## The taxonomy — three operating points
| Boundary | Context | Rot | Process / env | "Memory" loss |
|---|---|---|---|---|
| `claude --resume <id>` | full transcript reloaded | **persists** | fresh | none |
| `/compact` | lossy in-place summary | reduced | same | partial (the summary) |
| fresh `claude` + substrate | **blank, rebuilt from substrate** | **reset** | fresh | total (substrate-only) |

The human picks by need: **continuity** (`--resume`), **partial reset** (`/compact`), **full rot-reset**
(fresh + substrate).

## Disinfo corroboration
The stale `Next: Build blog index page` field surviving the `--resume` is a second, independent confirmation that
`--resume` clears nothing — and that the long-running stale-`Next:` disinfo only clears on a fresh session. Ties the
disinfo / misleading-instrument thread and the declared-not-derived mode-line work.

## Background agents persist too ("gang of four")
`--resume` also reloaded the session's **4 background agents** — the CF5 renumber subagents + claude-code-guide
agents spawned this session (all completed or killed) — the status line still showed "← 4 agents" after the resume.
A THIRD confirmation that `--resume` reloads the FULL session state (transcript + stale status fields +
background-agent references), not just the chat. A fresh session shows zero agents. (Completed records here, not
live leaks — but a fresh session drops them.)

## Implication
Exit-resume-dance memory updated: `--resume` for env/process continuity; **FRESH session + substrate for a rot
reset**. The resume-prompt's quality is the SOLE determinant of a good fresh-session warp. Ties
[[warp-trades-rot-for-memory-loss-substrate-bridges-smartness-2026-07-13]] and the [[exit-resume-dance]].
