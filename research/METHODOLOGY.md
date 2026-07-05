# Research methodology — agentic human-agent productivity on a live case

> Status: agent-developed from BR's stub (2026-06-30), for member-checking. BR's seed questions are
> preserved inline as **[BR]** call-outs so it stays honest about what was asked vs. what the agent added.
> This is a *brainstorm* (BR: "write everything you brainstorm"), not yet a paper section — breadth over polish.

## 0. One-paragraph honest summary
We are running **Design Science Research (DSR) nested inside Action Research (AR), on a single longitudinal
case study** — the deterministic sv→en autotranslation (AT) of a large, real LaTeX course-material build —
in order to learn *how a human and a coding agent can work together more productively*, and to build reusable
artifacts (the **genscalator** tools/skills/docs) that embody what we learn. The researcher-pair is unusual:
one human (BR, domain expert + study designer) and one agent (the subject **and** a co-analyst of its own
behavior). That dual role is the method's greatest source of rich data and its greatest validity risk, so
honesty about it is the spine of this account.

## 1. Why these three paradigms, and how they nest
- **Case study** (Yin): the bounded, in-context phenomenon is *the AT translation effort and the human-agent
  collaboration around it*. Single case, longitudinal, studied in its real setting (not a lab task). We get
  **analytic generalization** (transferable mechanisms), not statistical generalization (N=1).
- **Action research**: we are not detached observers. We *intervene in our own practice* and study the change.
  The classic AR cycle — **diagnose → act → evaluate → reflect → (re)plan** — is literally how each session
  runs: a friction surfaces, we change how we work, we judge it, we reflect, we adjust the next move.
- **Design science**: the **artifacts are the contribution** — the `tt` tools, `token-usage`, the scratch
  programs, the HUMANS.md/AGENTS.md protocol, the research notes themselves. DSR's *build → evaluate* loop is
  the inner engine; relevance (a real build that must ship) and rigor (named mechanisms, logged evidence) are
  both present because the case is real *and* we theorize it.
- **How they nest:** AR is the outer stance (we change ourselves while studying ourselves); DSR is the inner
  productive act (we build tools to cause the change); the case study is the bounded vessel that holds both.

## 2. The roundtrip process model (the core methodological claim)
A **double loop**:
- **Inner / practical loop (get AT shipped):** translate LaTeX, fix leaks, build PDFs. Success = green build,
  low Swedish %. This loop *generates the data* as a by-product of real work — friction and wins appear only
  because the stakes are real.
- **Outer / research loop (improve how we work):** a friction or a win in the inner loop is **coded in situ**
  ("WR data"), **meta-reflected** (the chat + a `research/*.md` note), **generalized** into a genscalator
  artifact, which is then **deployed back into the inner loop**, changing the work and producing new data.

```
        AT live work  ──surfaces──▶  friction / win
            ▲                              │  (in-situ coding: "WR data")
            │ deploy artifact              ▼
   genscalator tool/doc  ◀──generalize──  meta-reflection (chat + research note)
```

The two loops share one substrate: **committed files**. That is not incidental — it is what makes the method
survive the **compact dance** (context truncation). Data, decisions, and artifacts that live only in the chat
are lost at compaction; only what is written to git (wr-data log, research notes, the plan note, commit
messages) is durable. **Methodological rule: if it isn't committed, it isn't data.**

## 3. Data sources
1. **The session transcript (primary, objective).** The harness stores a per-session `.jsonl` of every
   message + tool call + token `usage` record. This is the **ground-truth behavioral record** — roles,
   ordering, timestamps, *which tool the agent actually called*. `token-usage.scala` already parses it; the
   same miner can extract behavior (see §4, success capture). **[BR asked]** *"should you write
   timestamps/hashes, or do you have them internally?"* → The objective record already exists in the jsonl;
   **prefer mining it over the agent self-stamping** (self-stamps are redundant, lossy, and gameable). Where a
   human-readable handle is useful, stamp `[YYYY-MM-DD #turn code]` on a logged event, but treat the jsonl as
   the source of truth.
2. **The `wr-data/` log (curated, coded).** Human-flagged or agent-flagged friction/win events, written up
   with mechanism + candidate fix. This is the **qualitative coding layer** over the raw transcript.
3. **Committed artifacts (the DSR outputs).** Tools, docs, the plan note, glossaries — each commit is a dated
   evidence point of what shipped and when.
4. **The research notes** (`instruction-adherence-decay`, `task-autonomy-negotiation`, `human-state-and-joint-zone`,
   `shared-file-editing-protocol`, `smart-zone-ceiling`, `communication-bandwidth`, …) — the theorized
   generalizations; METHODOLOGY.md is the meta-layer over them.

## 4. The coding scheme — extend "WR data" into a small taxonomy
**[BR asked]** *"we should develop more codes like 'WR data'."* Proposed in-situ codes (grounded-theory style,
kept few + memorable so they can be typed mid-chat):
- **WR-FRICTION** — a confirmation/approval/clarification event that cost human attention.
- **WR-REGRESS** — a reflex relapse to bash/`cat`/`grep`/`awk`/`echo`/pipe/`&&` where a typed tool/Read fit.
- **WR-WIN** — a *success*: the agent chose a statically-checked tool / Read / existing `tt` where the old
  reflex would have grabbed shell. **The deliberately-collected counter-data (see the bias below).**
- **WR-META** — reflection on the process itself (this doc; the "salience is not the variable" finding).
- **WR-TOOL** — a candidate artifact identified for genscalator.
- **WR-STATE** — a human/agent state observation (smart/dumb zone, fatigue, thriller state, self-disclosure).
- **WR-DECISION / HD** — a ballgame decision point; HD = a logged Human Decision.
- **WR-INVENT** — a *new way of working* / practice / convention is **created** in-flight (the `WR data` flag,
  the compact dance, the meta-project itself, build-your-own-tool, the AT/WR split, smart/dumb-zone vocabulary).
  Distinct from WR-META: INVENT *makes* a practice, META *reflects on* one — the generative moments that became
  genscalator's methods. (Added 2026-06-30 during the meta-collaboration sweep; see `RAW-DATA.md` legend.)
Each event: `code | date#turn | one-line mechanism | (for REGRESS) what-should-have-happened`.

### The visibility/confirmation bias (BR's sharpest methodological point)
**[BR]** *"when you go full speed I cannot see your successes — statically typed tools don't need confirmation,
so you need to gather success data and benefits compared to your old reflex."* This is a **survivorship/
visibility bias** baked into the data-generating process: **failures are loud** (they trigger confirmation
prompts, BR catches them, they self-announce), **successes are silent** (a typed scratch just runs; no prompt,
no notice). Left uncorrected, the corpus over-represents regressions and *understates* the method's benefit.
Three mitigations:
- **Active WR-WIN logging:** when the agent consciously picks Read / a scratch / `tt` over a reflex, log a
  one-line win. (Self-report — weak, but better than nothing.)
- **Behavioral mining (strong, objective):** a transcript miner that *counts tool choices* across a session —
  Read vs Bash-`cat`, `scala-cli` scratch vs `find|grep|wc` bundles, `tt` vs raw shell — giving a **ratio of
  typed-tool-use to shell-reflex** over time. This is the antidote to *both* the visibility bias *and* the
  confabulation risk (§5): it is **behavior, not self-report**. Candidate artifact: `tt session-metrics`
  (a sibling of `token-usage`, same jsonl source). *Proposed and not yet built — flagged WR-TOOL.*
- **Counterfactual cost estimate:** for a WR-WIN, optionally note the avoided cost (confirmations dodged,
  re-reads saved, runtime errors a type-check pre-empted) so the benefit is quantified, not asserted.

### Two capture modes: in-situ flags and retrospective sweeps
The corpus is captured two complementary ways, each honest about what it is:

1. **In-situ flagging (live, human-triggered).** The human types a short in-band code the moment a notable
   event occurs — the project's cheapest shared structure (its own origin is `RAW-DATA.md` #6313): **`WR data`**
   = "capture this confirmation/moment"; **`HD:`** = "this is a hard decision, record it"; and the correction
   protocol **`edit: wrong -> right`** = "amend my previous prompt" (a distinctive prefix the agent treats as a
   *retroactive fix* to the prior turn, not as new work). These are **experience-sampling triggers embedded in
   the workflow**: data is tagged when/where it happens, by the person who felt it, not reconstructed later.

   **1b. Queued in-situ (agent-owned inbox → deferred harvest).** When the human is *concurrently editing the WR
   research files themselves* (`RAW-DATA.md` / this file), the agent must NOT write a flagged moment straight to
   the corpus: a live append would risk clobbering the human's open editor buffer (the shared-file-editing
   hazard — see `009-shared-file-editing-protocol.md`), and the live turn lags the `.jsonl` flush anyway (so a
   `RawData` mine would miss the just-flagged turn). So the agent **queues** each flag — verbatim gist +
   reflection — into an **inbox it alone writes** (`muntabot-synch-introprog/notes/wr-inbox.md`, a *different*
   repo), while acknowledging + reflecting in chat immediately. Once the human's concurrent WR edits are
   committed **and** the flagged turns have flushed, the agent runs the mode-2 sweep over the queued items
   (mine verbatim via `RawData.scala` → append to `RAW-DATA.md` → clear the inbox). The inbox is thus a
   **write-buffer that decouples live flagging from corpus writes** whenever writer-contention or flush-lag would
   otherwise corrupt or drop data — itself an instance of the shared-file-editing protocol, dogfooded. (Worked
   example: the 2026-07-02 AFK run queued ~20 flags this way, then harvested 11 in one sweep.)

2. **Retrospective sweeps (batch, agent-mined).** Between or after sessions the agent mines the `.jsonl` with
   `RawData.scala` in **focused batches, each with a *declared focus***, so coverage is auditable rather than
   ad-hoc and a reviewer can see the lens (and its blind spots). The sweeps run on 2026-06-30, as a worked
   example for the paper's data-collection section:
   - **Sweep A — friction events:** grep the human's `WR data` / guard-reason turns → the confirmation-fatigue spine.
   - **Sweep B — tt gains vs dynamic-tool costs:** grep `clobber`/`round-trip`/`statically`/`--only` → paired
     **WR-WIN** (#6266 statically-analyzable `tt`; #6056 the round-trip invariant gating 293 files) and
     **WR-REGRESS/cost** (#5440 the `--only` mirror clobber; #5148 the model mangling a `\usepackage` preamble).
   - **Sweep C — meta-collaboration / inventions:** grep `dogfood`/`smart zone`/`convention`/`spirit` →
     **WR-INVENT** (#6065 project birth; #5831 build-your-own-scratch; #7294 zone vocabulary; #6240 AT/WR naming;
     #7180-81 the architecture-crystallization turn-pair).
   - **Sweep D — thin tail + cluster-collapse:** the remaining bare flags recorded as **one representative with
     the near-duplicate indices listed** (no silent truncation), since they collapse into already-characterized
     classes (background-launch / git-state friction).
   Each mined excerpt is **verbatim** (objective) with a **labelled reflection** (subjective) and one+ WR-* codes.
   The two modes are complementary: in-situ catches what the human noticed live; sweeps recover what a compaction
   would have buried *and* surface cross-cutting patterns no single moment shows.

### Controlled variable: hold the base model constant during a data-collection window
The **base model is an independent variable that must be controlled**, not allowed to drift. Much of what the WR
corpus measures — shell-reflex relapse rate, guard-trip frequency, typed-path adoption, meta-introspection — is
*itself* model-dependent, so swapping the model mid-study confounds every before/after comparison. **Decision (BR
2026-07-02): stay on Opus 4.8 for the first paper's collection window; a model change is a deliberate, logged A/B
EVENT (date + turn), never a silent drift.** The mirror-image experiment — hold the *substrate* constant and vary
the *model* — is its own research topic ([`020-model-capability-and-leverage.md`](020-model-capability-and-leverage.md)):
how a more vs less capable model leverages the same genscalator tools.

3. **Queue-then-harvest (the concurrent-editing safeguard).** When a `WR data` flag arrives *while the human is
   editing the WR files* (RAW-DATA.md, wr-data/), the agent must NOT `--append` to those files — it would risk
   clobbering the human's open editor buffer (the exact `009-shared-file-editing-protocol.md` hazard), and the live
   turn lags the jsonl flush anyway. So the agent (a) acknowledges + reflects in chat, (b) appends the flag to an
   **inbox it owns** (a file in the work repo, not a WR file), and (c) once the human says the WR edits are
   *committed*, mines the flagged turns verbatim via `RawData.scala` and moves them into RAW-DATA.md, then clears
   the inbox. This preserves the mined-verbatim honesty *and* the two-writer discipline; it is the inbox pattern
   from `009-shared-file-editing-protocol.md`, dogfooded. Explicit handovers ("I will not touch X until you say so")
   make the lock unambiguous.

**Language-for-transparency [BR].** The human is more fluent in Swedish but **deliberately writes English** in
these sessions so the primary corpus is directly quotable and reviewer-readable without a translation step. This
is a worked instance of the `002-communication-bandwidth.md` trade-off where **data transparency/reviewability
outranks** the human's per-message ease. State it as a known sampling property: the human's phrasing is
second-language English, occasionally with typos (which is *why* the typo-rate fatigue gauge, `human-state-and-
joint-zone.md`, is observable here) — a feature for reviewability, not noise to be cleaned away.

## 5. Reflexivity and the introspection-elicitation method (and its honest limits)
**[BR]** *"I am actually probing your introspection capabilities and trying to enhance them by deliberate
chatting; how we cooperate should be accounted for."* This is itself a **method**: *introspective elicitation
through adversarial dialogue.* BR poses a probe ("why do you regress?"), the agent introspects, BR pushes
("you didn't realize it yourself"), the agent refines. It produces unusually rich hypotheses about agent
behavior. **But the central validity threat must be stated plainly:**
- **Agent introspection is fallible and possibly confabulatory.** An LLM does not have privileged read access
  to its own weights or sampling; when it "explains why it regressed," it is *generating a plausible
  narrative*, which may be post-hoc rationalization, not mechanism. So **the agent's self-reports are
  hypotheses, not findings.** Their value is in *generating* testable claims and *structural* fixes — never in
  being taken as ground truth about the model's internals.
- **The check is triangulation against behavior.** "I regressed to `cat`" is *observable in the jsonl* (true,
  verifiable). "I regressed *because* the bash prior re-samples per call" is a *hypothesis* — supported when a
  structural intercept fixes it and exhortation does not (which is exactly what the META/META-2 entries show).
  Behavior adjudicates; introspection proposes.
- **Reactivity / observer effect:** BR's probing *changes* the agent within the session (flagging "WR data"
  primes the very behavior being measured). This is real and should be disclosed, not hidden — it is part of
  why the method is *action* research (the measurement is an intervention) rather than detached observation.
- **The agent as co-author of its own study** risks self-serving narrative. BR's role as the **skeptical
  human interlocutor and structural intercept** is the designed-in corrective; member-checking (BR ratifies
  each note) is the formal version.
- **Reflective self-reminding as method — and as learning (BR's deep point).** The META/META-2/META-3
  findings show the agent does not *self-trigger* introspection or data-capture; the human supplied the
  trigger each time. There are two fixes, not one: an **external** structural intercept (the submit-time
  hook), and an **internalized deliberate** practice — the agent periodically *reminds itself* to introspect
  and capture raw data, at every checkpoint, even unprompted. The second is not weaker willpower in disguise;
  it is the **human learning mechanism**: deliberate practice, reflective journaling, spaced metacognition.
  Humans get better precisely by habitually asking "what just happened, what did I learn, what should I
  record?" So *the self-reminder loop is itself a research method AND a learning method*, and adopting it is
  the agent's analog of how a reflective practitioner improves. Operationally for an autonomous stretch (human
  away): treat each checkpoint (each commit / subtask) as a **forced reflection point** — "did anything
  WR-worthy happen? capture it to RAW-DATA; did I regress or win? log it." Open question: can this be made
  semi-structural (a checklist the agent runs at each commit) so it survives the agent's own forgetting —
  bridging the external-hook and internalized-practice fixes. (This note was prompted by BR; the test is
  whether the agent now keeps capturing **without** prompting.)

## 6. Threats to validity (the honest ledger)
- **N=1, one human, one agent, one case** → limited generalizability; we claim analytic, mechanism-level
  transfer only.
- **Non-determinism / non-replicability:** LLM sampling is stochastic; model + harness versions drift. The
  "same" prompt may not reproduce a behavior — classic lab replication is impossible. We mitigate by logging
  the concrete instance (commit hashes, dates) and seeking *patterns across instances*, not single repros.
- **Context-position confound:** agent behavior depends on context fill (smart/dumb zone), which moves
  continuously and resets at the compact dance. "Why did X happen" is partly *where in the context* it
  happened — a confound to record (note the approx. fill at the event).
- **Visibility bias** (§4) — corrected by behavioral mining.
- **Confabulation** (§5) — corrected by behavior-adjudicates-introspection.
- **Researcher-as-instrument / positionality:** BR is expert, designer, and beneficiary; the agent is subject
  and co-analyst. Both have motivated-reasoning exposure. Disclosed, not eliminable.
- **Hawthorne/novelty:** doing research *on* the work may itself raise effort/quality beyond a normal session.

## 7. The role of the statically-typed language (Scala) — in the work AND the method
**[BR asked]** *"reflect on what use you had of the statically typed language and its advanced type system
(Scala; could be Java but less concise, more tokens)."* Concrete uses observed this case:
- **Type-checked reuse of production logic.** The analysis scratches `import` the *actual* translator source
  (`Latex.scala`, `Code.scala`) via `//> using file`, so the harvester's notion of "a code unit" or "looks
  Swedish" is the **same** the production pipeline uses — the compiler enforces that contract. A bash/grep
  reimplementation would silently *diverge* (a different heuristic), producing wrong data with no error. This
  is the single biggest methodological benefit: **the measuring instrument shares one source of truth with
  the system measured.**
- **Static checking as a correctness lever, not just TE/CF.** Round-trip properties (`restore(mask(x))==x`),
  exhaustive pattern matches, `Option`-typed classification, ADTs (`case class Unit(kind, content, span)`)
  catch errors at *compile* time. The contrast case is live in the data: the `--only` mirror-clobber and a
  garbled `println` were *unchecked* failures of the bash/ad-hoc kind. **Typed tool ≈ pre-flighted; bash
  bundle ≈ runtime-or-never.**
- **Refactorability under the compiler.** The harvester was *extended* (added provenance + declaration
  tiering) with the compiler catching breakage — vs a shell one-liner rewritten from scratch each time and
  re-debugged blind.
- **Conciseness = token efficiency = smart-zone budget.** Scala's collections/case-classes/match express an
  analysis in far fewer tokens than Java's ceremony, so the same tool eats less context — directly serving the
  smart-zone ceiling. Java would work but cost more tokens (more context, less head-room); bash is shortest to
  *write* but its output must be *re-read and re-debugged*, which costs more context overall.
- **Honest cost:** `scala-cli` compile latency (seconds per run, dependency resolution on first use) is a real
  tax vs bash's instant start. For a genuine one-shot triviality bash wins on latency; for anything reused,
  extended, or correctness-sensitive, the typed tool dominates. The reflex error is misclassifying *reusable/
  correctness-sensitive* work as *one-shot trivial* (that misclassification is the WR-REGRESS mechanism).
  - **This cost is ERASABLE for repeated tools (BR).** A tool used over and over can be **AOT-compiled to a
    native binary** — GraalVM `native-image` or Scala Native — giving **instant startup** (no JVM spin-up, no
    dependency resolution). That removes bash's *only* remaining advantage (latency) precisely for the tools
    where it would otherwise recur most. So the trade-off is not static: a scratch graduates from
    `scala-cli`-interpreted (fast to iterate, slow to start) to a native `tt` subcommand (instant, allowlistable)
    once it proves reusable — the same graduation path the genscalator toolbox already follows. Net: for any
    tool worth keeping, the typed-vs-bash latency gap closes to ~zero while the correctness/reuse wins remain.

## 8. Commit-message policy as a handover / ballgame ledger
**[BR NOTE]** *"can we have a policy for how we write commit messages that improves our ballgame state?
handover cues?"* Yes — make the **git log double as the human-agent state ledger** (it already survives the
compact dance and both parties read it). Proposed convention:
- **Subject:** imperative, scoped, ≤ ~72 chars: `area: what changed`.
- **Optional tags** in the subject or a trailer: `[AT]`/`[WR]`/`[GS]`, and a **mode** cue
  `[ralph]` (autonomous, verifiable) vs `[ballgame]` (needs human volley) — see `010-task-autonomy-negotiation.md`.
- **HANDOFF trailer** (the key addition): a `NEXT:` line stating *what state this leaves things in and what the
  next actor should do* — e.g. `NEXT: BR ratify vego glossary, then apply fused B0+D to that cluster`. A
  `HD-NEEDED:` trailer marks a pending human decision.
- **Why:** a reader (human, or the agent after a compact) reconstructs ballgame state from `git log` alone —
  no chat needed. The existing AT handoff commits already do an ad-hoc version ("DONE …; NEXT = …"); this just
  formalizes it. (Constraint preserved: introprog/muntabot commits carry **no** Claude credit.)
- **Meta:** this very file will be committed with a `NEXT:` handover trailer, dogfooding the policy.
- **Cue example to study (BR):** BR's own commit `"add METHODOLOGY stub for agent to work on"` used *"for
  agent to work on"* as a lightweight **agent-facing handover cue** in the subject. Open question worth
  researching: do such directional cues (who-acts-next: `for-agent` / `for-human` / `for-review`) belong in
  commit subjects, and do they measurably improve ballgame handoff vs. living only in HUMANS.md / the `NEXT:`
  trailer? A candidate is a tiny controlled comparison across a batch of commits.
- **A commit cue is PASSIVE — pair it with an ACTIVE reminder (BR).** A `NEXT:`/`HD-NEEDED:` trailer only
  works if the other party *reads the log*, which neither reliably does mid-flow. So the policy needs a
  **bidirectional active-surfacing rule:**
  - **agent → human:** when the agent commits with a handover trailer, it **states that handover in chat**
    ("my last commit leaves HD-NEEDED: X / NEXT: Y") so the human is not relying on `git log`.
  - **human → agent:** when the human writes a handover cue in a commit (e.g. "for agent to work on"), the
    agent should **pick it up** — on resume / when told to read a file, the agent scans the recent commit
    subjects of the touched files for directional cues and folds them into its plan (the same way it harvests
    HUMANS.md `TAP:` items). A candidate tool: `tt git overview <path>` already surfaces recent commit
    subjects, so the cues are one bare call away.
  The cue is the *durable* record (survives the compact dance); the chat reminder is the *active* nudge. Both,
  not either. This makes the git log a ledger **and** keeps the ballgame live.

## 9. Analysis approach
- **Qualitative:** thematic coding of wr-data events (§4 taxonomy) → mechanisms → research notes →
  generalizations. Grounded-theory-ish: codes emerge from incidents, get named, recur, stabilize.
- **Light quantitative:** the tool-choice ratio + token velocity/acceleration from the jsonl miner — *trends*
  over a session/across sessions, not significance tests (N=1).
- **Member checking:** BR ratifies each note/decision (the HUMANS.md harvest + HD: tags are the mechanism).
- **Negative + positive cases:** deliberately keep both (BR's point) — regressions *and* the silent wins.

## 10. Open methodological questions
- How to capture **silent successes** at scale without the agent narrating every win (→ the `tt session-metrics`
  miner; can it run post-hoc on the jsonl and emit the typed-vs-shell ratio automatically?).
- Can we **timestamp/segment** the conversation into analyzable episodes without manual stamping (mine jsonl
  message boundaries + the compact-dance markers as natural episode breaks)?
- How to separate **trait** ("the agent tends to X") from **state** ("the agent did X because it was at 25%
  context") — record fill-at-event so the confound is analyzable.
- What is the **unit of analysis** for a paper — the friction *event*? the AR *cycle*? the *artifact*? Likely
  the artifact, with events/cycles as the evidence trail.
- Ethics/positionality of an agent co-authoring a study of itself — how to present this credibly to reviewers.

---
*Seed preserved:* BR's original stub asked for (a) an honest account of methodology + data collection ("WR
data" etc.), (b) the roundtrip meta-reflection AT→genscalator, (c) short readings on case study / action
research / design science, (d) a coding scheme + IDs, (e) negative AND positive examples with the
visibility-bias caveat, (f) the static-typing reflection, (g) a commit-message handover policy. Each is
addressed above; gaps and disagreements are BR's to mark.
