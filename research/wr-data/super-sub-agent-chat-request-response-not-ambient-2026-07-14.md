# Super-agent ↔ sub-agent "chat" is request-response, not ambient — and the resume path failed live (WR)

**Date:** 2026-07-14. **Models:** super = Opus 4.8 (1M); sub-agents = fable (CF5). **SM081.** Grounded in a
LIVE run (the SM080 book-consultation), not theory. Ties [[delegation-dance]], [[cue-use-fleet]], SM079
(asymmetry), the ape⟷anthro frame.

## BR's question

Can the super-agent CHAT with sub-agents the way the human chats with the super-agent — i.e. an ongoing,
multi-turn, context-preserving exchange — or is it something narrower?

## What I actually did (the vehicle)

Running SM080, I spawned two background CF5 sub-agents as "book-expert consultants" (one on *Experimentation in
SE*, one on *Case Study Research in SE*), each with a scoped brief AND an explicit instruction to "stay available
for follow-ups as the design firms up." Each read its book and returned a rich, cited digest. I then tried to run
a **second round**: a follow-up question to each via `SendMessage(to: <agentId>)`, per the spawn result's own
instruction ("Use SendMessage with to: '<agentId>' ... to continue this agent").

## The finding: the second round FAILED

Both follow-ups returned:

> could not be resumed: **No transcript found for agent ID: <id>**

So the "keep them alive as consultants I query as the design firms up" pattern — the exact thing SM081's
deliverable set out to design — **failed for these two *completed* CF5/fable sub-agents**.

**But then it WORKED (1 of 3).** A third completed background sub-agent — the `claude-code-guide` consult
(default model, spawned later in the same run) — **resumed cleanly** via `SendMessage` minutes after it
completed ("had no active task; resumed from transcript in the background"). So resume-after-completion is NOT
uniformly broken: it succeeded once and failed twice in the same session, failing only for the two fable experts.
**Empirical verdict: documented, but INCONSISTENT in practice** — worked 1/3 live. See "Harness truth" below.

## Interpretation — the asymmetries vs human↔super

Even setting the failure aside, the channel is structurally NOT like human↔super:

1. **Request-response, not ambient.** The super initiates every exchange. A sub cannot proactively ping the
   super mid-task; only its final result returns (plus a completion notification). Contrast: the super surfaces
   to the human at will, any time. The human↔super channel is bidirectionally initiable; super↔sub is not.
2. **The super receives a DIGEST, not the sub's context.** Each expert internally consumed ~90k tokens reading
   its book; only a few KB of cited digest returned to my window. The sub's working context is not shared back.
   Within a session, by contrast, the human never loses the super's context.
3. **The multi-turn channel was unavailable post-completion (this run).** What the docs imply is a resumable
   "continue this agent" chat was, empirically, a single turn per consultant.

## The deeper point: a chat-like protocol may be UNDESIRABLE here anyway

The resume-failure cost far less than expected — because the fire-and-forget shape is arguably the RIGHT one for
this use. The whole VALUE of the fleet (per [[delegation-dance]]) was big-in / small-out: each expert absorbed
~90k tokens and returned a compact digest, keeping the bulk OUT of the super's window and thereby NOT rotting the
super. A fully chat-like, many-round consultation would repeatedly pull sub-context back into the super's window
and **re-rot the super** — the exact harm delegation exists to prevent. So:

- **"Fire-and-forget request-response with a digest return" is the correct default** for consultation-style
  delegation. The persistent-chat ideal is most tempting exactly when it is most dangerous (iterative refinement
  = repeated context pull-back into the super).
- The asymmetry mirrors SM079: what the super needs FROM a sub (a compact digest) ≠ what the human needs from
  the super (a live, ambient, co-design partner). Same ape⟷anthro asymmetry, different pairing.

## Design implications (the consultant-fleet workflow, SM081 deliverable)

1. **Front-load all questions into the initial brief.** Because the resume path is unreliable post-completion
   and re-rounds re-rot the super, put every question you can foresee into the FIRST brief (one rich round),
   rather than planning follow-ups. Today's briefs did this partly; do it more.
2. **If a follow-up is truly needed, spawn a FRESH expert** with just that question (it re-reads the relevant
   chapter — cheap on a CF5) rather than relying on resume. This keeps the super's window clean and sidesteps the
   resume-failure entirely.
3. **Treat the fleet as a DIGEST pump, not a chat partner.** This is the honest capability and the desirable one:
   it validates [[delegation-dance]]'s big-in/small-out logic rather than contradicting it.
4. **Reserve true multi-turn agent chat** for still-running / named-teammate topologies (pending harness-truth
   confirmation) and for cases where follow-ups are cheap and the super genuinely needs iterative back-and-forth
   — accepting the re-rot cost consciously.

## Harness truth (from claude-code-guide, docs-cited)

- **Resume-after-completion IS documented and supported.** Per Claude Code `sub-agents.md`: *a completed
  subagent that receives a `SendMessage` auto-resumes in the background without a new `Agent` invocation.* It
  works for background subagents AND named ones; it does NOT require agent-teams to be enabled (teams just add
  structured message types). The only documented block is a MANUAL stop (via `/tasks` `x` or SDK `stop_task`,
  v2.1.191+), which is permanent until you type into the transcript.
- **So my "No transcript found" was an anomaly, not the designed behaviour.** The docs are silent on that exact
  error; the guide infers a session-context/ID issue.
- **And it is genuinely inconsistent live:** in this one session, resume **failed 0/2 for the CF5/fable book
  experts** ("No transcript found") but **succeeded 2/2 for the default-model `claude-code-guide` sub-agent**
  (resumed cleanly twice). The difference is unexplained — candidate causes: model/agentType (fable vs default),
  or a timing/persistence difference in how the async fable agents' transcripts were registered. **Undetermined;
  worth an isolated repro.**
- **Design consequence:** because the channel is documented-but-flaky (worked 1/3 of the completed agents here),
  do not DEPEND on it — front-load questions (implication #1) and spawn-fresh on real need (#2) remain the safe
  defaults regardless of the resume semantics.

## Side-finding: a delegated sub-agent tripped the guardcheck hook (and the hook caught it)

While researching, the `claude-code-guide` sub-agent (CF5/fable) **regressed to brittle bash** — a piped `grep`
that tripped the `guardcheck` PreToolUse hook — then self-recovered ("I regressed into a piped grep without
thinking ... let me answer without triggering that hook again") and still returned a correct, cited answer. Two
lessons, both reinforcing the skill-theory:

1. **Sub-agents do NOT carry the genscalator guard-clean skills** (those are the caller's plugin context, not the
   sub's), so cheap delegated minions **will** regress to base-model brittle bash — the same cold-start /
   dormant-skill mechanism (`agent-skill-theory.md` §1) but at the sub level, where the reflexes were never
   present at all.
2. **The `guardcheck` HOOK clamped it anyway** — a clean live corroboration of **P5 (constrain-beats-inform)**: no
   informing skill was in the sub's context, yet the structural hook took the wrong behaviour off the table. The
   sub had willpower-only recovery; the hook is what actually held.
3. **Actionable (ties SM077):** when delegating, **"warm" the minion** — pass the `guard-clean-digest` (or a
   tool-lane constraint) into the sub's brief so it doesn't regress. Delegation should carry the reflexes down,
   not assume them. And prefer to **constrain the sub's tool-lane** ([[delegation-dance]]) so a regression can't
   even reach the shell.

## Bottom line

**No — the super does not "chat" with sub-agents the way the human chats with the super.** In this live run it
was one-shot request-response (the documented resume path failed post-completion), the channel is super-initiated
and non-ambient, and only a digest — not context — flows back. And that narrower shape is the one we should
*want* for delegation, because a chattier protocol would re-rot the super it is meant to protect.
