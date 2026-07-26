# v0.9.0 tools — allow / ask / deny triage (SM041 subset; per-tool release-hygiene)

Per the finding that we should **triage `allow`/`deny`/`ask` per new tool** (see
`research/wr-data/effectful-verb-ask-overrides-auto-authorization-layers-2026-07-11.md`). The v0.9.0 `tt` surface
classified by effect. Propose-only — the settings edit is BR's hand.

**Current settings model:** `Bash(tt *)` blanket-**allows** every `tt` tool, with per-verb **ask** overrides for the
consequential ones (today only `Bash(tt forge release-create *)`), and a **deny** list catching raw destructive git
(`git rm`/`reset --hard`/`clean -f`/`push --force`).

## Principle
- **read / pure / local-write** → stay under the blanket `Bash(tt *)` allow (no ask). Safe by construction — no
  outward or destructive effect.
- **effectful + OUTWARD / cross-host / consequential** → an **ask** override (a human confirms).
- **destructive** → **deny** (already handled by the deny list).

## Per-tool (new in v0.9.0)
| tool(s) | effect | classification |
|---|---|---|
| harden · statusline · wr stamp · gitinfo · md-fmt | read-only / prints | **allow** (blanket) |
| ssg · serv | write local HTML / loopback-only server | **allow** (local-only, low consequence) |
| svg / ascii / gvdot | generate a diagram (stdout or a local file) | **allow** |
| web (get) | read-only HTTP GET, size-capped, no credentials | **allow** |
| git (commit/push) | outward push — BUT safe-by-design (no force/reset/rm; message-from-file) | **allow** (asking on every commit would defeat the low-friction goal; the deny list catches force-push) |
| **forge release-create** | creates a PUBLIC release | **ask** ✓ (already there) |
| **forge release-edit** | edits a PUBLIC release | **ASK — ADD** (see below) |
| box | host-pinned remote ops (some effectful: e.g. kill remote processes) | **ask — CONSIDER** (cross-host + effectful; but fixed-verb, host-pinned, BR's own box → lower risk; BR's call) |

## Recommended settings change — add to the `ask` array
```json
"ask": [
  "Bash(tt forge release-create *)",
  "Bash(tt forge release-edit *)"
]
```
(optionally also `"Bash(tt box *)"` if you want cross-host ops confirmed.)

## Why release-edit is the clear one
`tt forge release-edit` is a new **effectful, OUTWARD** verb — it PATCHes a **public** release. It belongs with
`release-create` in the `ask` list, not silently under the blanket `Bash(tt *)` allow. Live evidence: when it fixed
the v0.9.0 release body today it ran **silently** (no confirm), *unlike* `release-create` which asked — that exact gap
is what this triage closes. This is the per-tool `allow`/`ask`/`deny` hygiene the effectful-verb-ask finding calls
for, applied to the v0.9.0 surface. Ties: SM041 (the full allowlist deep-mine, of which this is the new-tool subset),
the guardcheck Part B hook (a complementary structural guard on *command shape*), [[hardening-dance]].
