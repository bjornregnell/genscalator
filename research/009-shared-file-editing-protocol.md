# Non-destructive shared-file editing (human and agent on the same file)

- **Question:** When a human and an agent both edit the same file (the motivating case: `HUMANS.md`, which
  the agent appends to and the human curates), how do we stop the agent from clobbering the human's work?
  Which coordination protocol has the **lowest total friction**, counting both clobber risk and the ongoing
  human effort the protocol imposes?
- **Why it matters:** the HUMANS.md / AGENTS.md pair is meant to be a clean, reusable collaboration artifact
  (a candidate genscalator plugin feature). It only works if concurrent edits are safe. But "safe" and
  "low-effort" pull in different directions, so the right protocol is not obvious; picking wrong either risks
  lost work or quietly taxes the human on every cycle (a confirmation-fatigue cousin).
- **Status:** open. Opt A adopted for genscalator HUMANS.md as a starting point (2026-06-30); the churn-vs-safety
  trade-off below is explicitly flagged for future evaluation.

## The core problem (and the insight that constrains it)
The agent writes the **on-disk** file; the human's editor holds **unsaved buffer state the agent cannot see**.
So an in-progress human edit is undetectable from disk alone. Detection-from-disk cannot be the foundation;
convention or structure has to be.

**Key insight: section-level zones inside one file do NOT help.** A text editor saves the **whole file from
its buffer**, so when the human saves, their buffer overwrites *any* agent change made *anywhere* in the file
after the buffer was opened, including a separate "agent zone". Therefore a safe partition must be at the
**file level**, not the section level. This single fact rules out the tempting "just give the agent its own
heading in HUMANS.md" approach.

## The options and their trade-offs
| Opt | Mechanism | Safety | Human churn | Other cost |
|---|---|---|---|---|
| **A: file-level inbox** | agent writes only `HUMANS.inbox.md`; human harvests into HUMANS.md, then deletes from inbox | **Highest** - agent never touches HUMANS.md, so zero clobber by construction | **Highest** - every cycle the human moves items across files (a manual harvest step that may get tiring) | one extra plumbing file; slight delay before items appear in the curated list |
| **B: commit channel** | agent proposes via CHANGELOG `## Unreleased` + commit messages; human harvests at review time | High - agent never edits HUMANS.md | Medium - human still harvests, but from a file they already read | couples two concerns into CHANGELOG; proposals mixed with release notes |
| **C: two-writer + handshake + editor safeguard** | agent appends to HUMANS.md, but only when the human has no uncommitted/unsaved state; rely on the editor's disk-vs-buffer diff to catch races | Medium - depends on the editor safeguard + discipline | **Lowest** - no harvest step; items land directly in the curated list | needs an explicit "go"/quiet-state convention; residual race if the editor lacks the safeguard |

## The editor-safeguard angle (BR's point - argues for C)
Modern editors (VS Code, JetBrains, etc.) **diff disk against the in-memory buffer** and, when the file
changed on disk under an unsaved buffer, **warn ("file changed on disk") and offer to reload/merge** rather
than silently overwriting. If that safeguard is reliable, then Opt C's clobber risk is largely handled **by
the editor itself**, and Opt C becomes attractive because it imposes **the least human churn**: the agent
appends directly to HUMANS.md, the human never moves items between files, and a genuine race surfaces as a
visible editor prompt instead of silent loss.

The counter-considerations to investigate:
- The safeguard is **editor-dependent**; a human editing via a tool without it (a plain `>` redirect, a
  basic editor, a web textarea) gets no protection. The protocol's safety would then vary by the human's tools.
- The safeguard catches a **saved-file race**, but the resolution (reload vs keep-buffer vs merge) is a manual
  decision pushed onto the human at an awkward moment; that is its own small friction, just rarer than A's
  per-cycle harvest.
- It still needs a **quiet-state convention** so the agent does not append in the middle of an edit session.

## The real question: lowest *total* friction, not just lowest risk
Opt A maximizes safety but may **tire the human** with constant cross-file moving (the thing BR flagged).
Opt C minimizes per-cycle effort and leans on editor safeguards, but trades guaranteed safety for
mostly-safe + editor-dependent. The honest answer is that the best choice **depends on edit frequency and the
human's editor**: high-frequency curation with a safeguard-equipped editor favours C; rare, high-stakes
curation or heterogeneous tooling favours A. This wants **measuring**, not guessing.

## Open directions (future research)
- **Measure harvest fatigue** under Opt A: how many cross-file moves per week before the human resents it?
- **Test the editor safeguard** empirically across the editors humans actually use here; how reliable, and is
  the reload/merge prompt low- or high-friction in practice?
- A **hybrid**: agent appends directly (C) for low-stakes items, but routes high-stakes or large edits through
  the inbox (A). Mode chosen by the same triage as `010-task-autonomy-negotiation.md`.
- **Tooling that removes A's churn:** a `tt harvest` command that moves inbox items into HUMANS.md TODO in one
  call, making A nearly as low-effort as C while keeping its safety. This may dominate both.
- Generalize beyond HUMANS.md to any human-owned shared file (AGENTS.md, CLAUDE.md, config).

## Observed instance (2026-07-01): the shaky PRD handover — live evidence for a low-ceremony C
The agent and BR used an EXPLICIT handover on `PRD.md` ("I won't touch it until you say so" / "PRD is yours
again"). It went shaky: the agent handed PRD back after a revision, then **re-edited it minutes later** (a `tt
parsereqt lint` run caught a `Rationale→Why` miss) **without re-taking the handshake** — a real protocol slip. No
work was lost only because BR happened not to be editing. Notably the slip occurred because a **multi-step agent
task (go 1 → go 2) spanned the handover boundary**: the agent released the lock after step 1, then a later step
needed the file again.

**BR's decision:** it's OK for the handover to be a bit **fuzzy**. Rationale: (a) the editor's disk-vs-buffer
warning is the real safety net (Opt C's core), so a slip surfaces as a visible prompt, not silent loss; (b) both
humans and agents forget, so a hard lock would be constantly violated anyway; (c) **periodic handshakes** ("you
have it" / "back to you") are enough to resync. So the working protocol for genscalator's shared *research / PRD*
files (distinct from HUMANS.md) is a **low-ceremony optimistic C**: edit freely, signal handovers as soft cues,
lean on the editor safeguard, re-handshake on doubt. This is empirical support for C over strict locking when the
human uses a safeguard-equipped editor. Caveat learned: an EXPLICIT handover must be **re-affirmed, not assumed
to persist** across multi-step agent work — and per `011-human-state-and-joint-zone.md`, the **agent** should be the
one to re-handshake when resuming edits (it is the tireless partner; the human is the one who forgets).

## What shipped
- **Opt A adopted for genscalator HUMANS.md (2026-06-30):** `HUMANS.inbox.md` created (agent-owned,
  append-only); HUMANS.md intro + How-it-works updated so the agent never writes HUMANS.md. Chosen as the
  safe default to start; the churn evaluation above (and the possible swing to C or a hybrid) is left open.
- Companion hygiene now in the agent's memory: never `git add -A`, stage only agent-authored paths;
  pre-write `git status` guard on shared files. See `008-instruction-adherence-decay.md` (structure beats
  exhortation) and `010-task-autonomy-negotiation.md` (mode triage).
