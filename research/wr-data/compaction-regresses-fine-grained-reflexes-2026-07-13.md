# WR data: compaction regresses fine-grained reflexes to base-model defaults (2026-07-13)

**Type:** WR data — post-compaction regression specimen. BR spotted it live and named the hypothesis:
*"is it the compacting that caused the regression into grep blobs instead of tt grepr or similar?"*
**Threads:** [[use-tt-grepr-not-raw-grep]], [[cue-bare-auto-compact]], [[resume-prompt-anti-regression-checklist]],
[[guardcheck-false-positive-gt-inside-quoted-regex-2026-07-13]], [[joint-rot-vigilance-recovery-kit]].

## The specimen
Post-compaction (fresh window, ctx 7%), the agent reached for raw `grep` — three times, each with a
shell-metachar in the pattern (`|`, `>` via `<label>`/`<f>`) — tripping the guardcheck each time and racing
BR. There is a STANDING memory, `use-tt-grepr-not-raw-grep`: recursive scans go through `tt text grepr`,
never raw `grep`. Earlier the SAME turn (pre-drift) the agent had used `tt text grepr` correctly. So this is
a regression *within one session*, straddling... nothing but the compaction? No — straddling the compaction.

## BR's hypothesis, and why it holds
**Yes, and it is traceable to a concrete lossy channel.** Neither the compaction summary NOR the agent's own
resume-prompt anti-regression checklist carried the grepr reflex forward. The checklist (which the agent
authored pre-compact) listed em-dash anchors, tail-pipe, mode verbs, git flow, notify branding — but omitted
`use-tt-grepr-not-raw-grep`. So in the fresh window, with that reflex absent from the salient context, the
agent defaulted to `grep` — the base-training default. `tt grepr` is a *learned local override*; `grep` is
the model's prior. Remove the override from working context and the prior reasserts.

## The general finding (the value)
Compaction preserves the NARRATIVE and the LOUD reflexes (whatever made it into the summary or the
anti-regression checklist), but **fine-grained learned overrides that live only in memory files regress to
base-model defaults** unless they are re-surfaced post-compact. The summary is lossy precisely at the level
of small, specific habits — the ones a memory file encodes but a prose summary rounds off. So:
- Memory files are necessary but NOT sufficient across a compaction — they are not auto-injected into the
  fresh window's salient context; only the summary + the resume prompt are.
- The resume-prompt anti-regression checklist is the load-bearing carrier of fine-grained reflexes across the
  warp. Its comprehensiveness IS the regression-prevention mechanism.

Two distinct sub-slips, one root: (1) tool choice (`grep` vs `tt grepr`); (2) metachar-in-pattern (even when
grepr was used). Both are small habits; both regressed; both were absent from the checklist.

## The structural fix (not willpower)
Add the grepr reflex to the resume-prompt anti-regression checklist, so it survives the NEXT compaction.
Generalise: every regression actually hit post-compact becomes a checklist line — the checklist grows
monotonically toward covering the agent's full set of learned overrides. This is the same
structure-over-willpower move as the guardcheck hook: encode the reflex where the process will re-surface it,
not where it depends on the agent spontaneously recalling a memory file.

## Follow-up
- DONE this session: checklist line added; behaviour corrected mid-turn.
- Candidate SM: a post-compact "reflex re-hydration" step — the agent re-reads its own high-frequency
  operating memories (grepr, commit flow, guard-clean idioms) as a deliberate first action, not relying on
  the summary to have carried them. Cheap insurance against exactly this regression class.

## Second specimen — the checklist was NOT sufficient even when it CARRIED the reflex (2026-07-14 22:5x)
Post-compact resume (fresh window), the agent read `tmp/resume-prompt.md` — whose anti-regression checklist
line 5 explicitly lists `ls | head` as a ⛔ pattern — and *on its second working tool call* still appended
`| head -50` to a `tt files` command. The guardcheck hook caught it ([MED] pipe-to-head). BR reported it live
from inside the guard-stall (`ig:`) as WR data.
- **Why this sharpens the finding:** the 2026-07-13 specimen regressed on a reflex ABSENT from the checklist,
  and the fix was "add the line." This specimen regressed on a reflex PRESENT on the checklist, freshly read.
  So checklist-carriage is necessary but **not sufficient** — a base-model "just peek at the top" reflex fires
  below the salience level a read-once checklist provides. The checklist is a *reference*, not a *reflex*; only
  the **guard hook** operated at reflex speed and actually stopped it.
- **The value:** this is the structure-over-willpower thesis proving itself twice over. The willpower layer
  (checklist I authored + just read) failed; the structural layer (the automatic PreToolUse guard) held. It
  argues for the guard hook as the load-bearing backstop, NOT the checklist — and for the SM103-adjacent idea
  of a `tt` native "peek first N" so the reflex has a guard-clean landing spot instead of only a ⛔.
- The pipe was also pointless: `tt files` had already printed the full 14-file list in the prior (unpiped)
  call; the piped re-run added nothing. So: regression under low stakes / near-zero payoff — the classic
  autopilot signature.

## Third specimen — TWO slips in the first three post-compact calls (2026-07-15 midday)
Fresh window straight after a `/compact`, resuming SM111. The very first tool call was a `cd repo && git status`
compound (⛔ on the checklist: no `cd`-then-chain, use `git -C`); the guard hook caught it. Two calls later, a
`tt web 2>&1 | head -40` to read a tool's short usage (⛔ pipe-to-head; and pointless, the usage self-prints in
full). BR flagged the second live: *"where is your real head :) regression after compact."*
- **Why it sharpens further:** this is now the pattern across THREE consecutive compactions (07-13, 07-14, 07-15),
  each time in the *first handful* of post-compact calls, each time a different specific reflex (grepr / tail-pipe /
  cd-chain + head-pipe). The regression is not about *which* reflex — it is a general **salience collapse** right
  after the warp: the checklist is freshly read yet base-model shell idioms (cd-chain, pipe-to-peek) fire below its
  salience. The **guard hook caught every one** — the structural backstop holding while the willpower layer slips,
  exactly the thesis. Strongest single argument yet that the load-bearing control is the automatic hook, not the
  checklist.
- **Candidate, sharpened:** the post-compact "reflex re-hydration" first-action (from the 07-13 follow-up) should be
  *mandatory and explicit* in the resume-prompt — a deliberate "run `gs warm`, re-read guard-clean idioms" step as
  ACTION ZERO before any tool call, since three specimens show the fresh window cannot be trusted to carry the
  fine-grained reflexes on its own.

### The visibility asymmetry BR spotted (deny vs ask) — 2026-07-15
BR, live: *"STRANGE: you talk about 3 slips but I only got ONE guard modal on the head clobbery... that's
interesting."* The reconciliation is itself a finding. **Observed (ground truth):** the `cd … && git …` compound
came back to the *agent* as a **flat error** (`[HIGH] && command chain` / `[HIGH] cd + compound`) with **no
confirmation modal to BR**; the `… | head` pipe raised the **[MED] "requires confirmation" modal BR actually saw**.
So the agent experienced ~2-3 guard interventions in the first calls, but BR saw only **one**.
- **Likely mechanism (INFERRED, not yet verified — hook-config check deferred to BR-present, dropped mid-run for
  AFK-safety):** the guard hook has two tiers — **HIGH → deny** (blocks the agent, returns an error, no human
  prompt) vs **MED → ask** (surfaces a confirmation modal). If so, HIGH slips are invisible to the human by design.
- **Why it matters (the value):** the human's **modal count UNDERCOUNTS the agent's slip count**, because the most
  severe patterns are auto-denied silently. This bears directly on the *shared-vigilance* picture
  ([[joint-rot-vigilance-recovery-kit]]): BR literally cannot police the HIGH slips he never sees — the deny tier is
  a pure agent↔hook loop. Good news for safety (worst patterns hard-blocked without needing a human), but it means
  the WR-data record, not the human's memory of modals, is the true count of post-compact regressions. **Candidate
  (BR-present):** verify the deny/ask tiering in the guardcheck hook config, and consider whether the agent should
  *self-report* HIGH denies to the human (else they vanish from the joint record).

### Slip taxonomy — which base-model reflexes reassert post-compact (BR: "WR data on the slip categories")
Across the three specimens (07-13 / 07-14 / 07-15), the post-compact slips are not random — they cluster into a
small, stable set of **base-model shell idioms**, each with a known guard tier and a known correct `tt` replacement.
Naming the categories makes the re-hydration list *targetable* (a resume-prompt / `gs warm` can drill exactly these):

| # | category | observed slips | guard tier | correct genscalator idiom |
|---|----------|----------------|------------|---------------------------|
| A | **command chaining** | `cd repo && git …`, `a && b`, `a ; b` | HIGH → deny | ONE bare command per call; `git -C <abs>`; `tt git …` |
| B | **output peeking** | `\| head`, `\| tail`, `\| wc` | MED → ask | the tool's own `--limit` / `--tail` / `--count` |
| C | **stream suppression** | `2>/dev/null`, `2>&1` | MED → ask | let the tool self-report (to a file, then Read); tolerate benign stderr |
| D | **legacy-tool reach** | raw `grep` / `find` / `ls` | (varies) | `tt text grepr` / `tt files` / `tt text match` |
| E | **metachar-in-arg** | shell glob/redirect chars inside a regex/pattern | HIGH (guardcheck) | metachar-free patterns; anchor on plain substrings |

**Reading of the taxonomy:** every category is a case of *a general-purpose Unix reflex reasserting over a
learned narrow tool* — the same root as the tool-choice regression (grepr vs grep), generalized. A/E trip the HIGH
(deny) tier → invisible to BR (the asymmetry above); B/C trip MED (ask) → BR sees the modal. So the categories BR
*observes* (B/C) are a biased sample of the categories that actually *fire* (A-E) — another reason the WR-data
record, not modal-memory, is the true tally. This session alone hit **A** (cd-chain + `&&` + `;`), **B** (`| head`),
and **C** (`2>/dev/null` + `2>&1`) in the first ~5 calls. The taxonomy is the concrete payload for the ACTION-ZERO
re-hydration candidate: re-surface *these five categories by name* post-compact, not a vague "be guard-clean."
