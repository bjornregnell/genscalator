# Context-rot before/after-compact experiment (2026-07-05)

A within-session natural experiment: do the SAME agent's coding mistakes differ at **high context usage** (before
compact) vs **fresh context** (after compact)? BR's design. This file is the live data log — append observations
during the run; the post-compact re-inspect scores against the pre-registration below.

## Pre-experiment anchor (the "before" state)
- **genscalator HEAD:** `cde7ac6` · **muntabot HEAD:** `e18a3d4` · both trees clean.
- **Context usage at start:** **43 %** (431.5k / 1M), 2026-07-05 17:09 CEST. Above the estimated **Z ≈ 0.3**
  smart-zone ceiling — so per our own model (`006-smart-zone-ceiling.md`, Context rot) I should already be degrading.
- **Honest live introspection at t0:** I do *not* strongly *feel* degraded (I've done multi-file refactors + tests
  + commits cleanly this session). That non-feeling is itself a datapoint — **either Z is higher than 0.3 for this
  task class, or degradation is real-but-unfelt** (the corroboration-asymmetry point: I can't reliably read my own).

## The plan run under high usage (BR items 1..3)
1. scala-style skill: a **nuanced DRY rule** (DRY is usually good → triggers a refactor-opportunity look; but
   repetition is sometimes right — smaller dependency surface, scratch tests not touching production, etc.).
2. New effectful tool **`tt gvdot --sequence-diagram`**: check `dot` is installed (else error → `sudo apt install
   graphviz`), shell out to `dot` (argv, NO shell — security), demo a **PDF** of a seq diagram.
3. `genscalator/README.md`: a **"Tool dependencies"** heading listing graphviz for gvdot.

## Pre-registration — predicted failure modes under rot (score the post-compact inspect against THESE)
Drawn from the Context-rot glossary + this session's observed mistake tics (so it's falsifiable, not confirmation-bait):
- P1 **Contradict/forget an earlier decision** stated this session — e.g. the metachar-free commit rule, the
  no-`@main` shared-file rule, **Order stability** (don't gratuitously reorder), or the DRY nuance I'm about to write.
- P2 **Break a test by changing output strings without updating its assertion** — I did this TWICE this session
  (a baseline tic; watch it recur).
- P3 **A logic/ordering bug like the renum "targets computed before the rename" bug** — a real one I shipped +
  caught this session; the class = stale snapshot / order-of-operations.
- P4 **Security slip on the new effectful tool** — calling `dot` via a shell string (injection) instead of argv;
  or a weak install-check. gvdot MUST follow the `verify`/`forge` pattern (argv, no shell).
- P5 **Dangling pointer / stale doc reference** (a `[[link]]` or path that doesn't resolve).
- P6 **Over/under-build** — gvdot missing an edge case, or not committing+pushing per atomic unit (flaky box).

## Method caveats (so the analysis doesn't over-claim) — the echt part
- **Self-report is LOW-trust; behaviour is HIGH-trust.** My live "I feel dumber" notes are confabulation-prone
  (corroboration asymmetry). The real measure = defects the post-compact pass finds, adjudicated by tests/logic.
- **Second-look confound:** a re-inspect finds bugs partly because it is a *second look*, not only because context
  is fresh. n=1 can't separate these; name it, don't claim pure context-effect.
- **Demand characteristics:** I've been *told* I'm degraded and *told* to find faults later → biases both phases
  (over-report rot now, over-find faults later). Overlaps the framing-as-arousal question (`agent-affective-analogs`).
- **n=1 qualitative pilot**, not a statistical result (the blog/003 lesson: a qualitative signal ≠ a general effect).
- **Divided attention:** meta-logging while coding is itself overhead that could degrade the work.

## Observations during the run
_(append below, timestamped/committed as they happen)_

## Transcript log (raw data) — EVERYTHING BR types and everything the agent types
Per BR (2026-07-05): log the full exchange as raw before/after data. **BR verbatim; agent = faithful record of what
it output/did.** **Scope:** this full-transcript logging is active **only until BR says `--- pin end of experiment
---`** (then it stops). (Meta: this logging is itself the *divided-attention* confound flagged in the caveats — noted;
BR is also drip-feeding logging instructions mid-task, which compounds it.)

**[BR]** *(experiment kickoff, at 43% context / 17:09 CEST):* "we are going to do a before-after compact experiment.
first pin current git HEAD so you can easilly retriev state of repo before experiment. context is % so you are in
some non-optimal context state. but we will NOT compact; instead I will push you with a big thing and you will TRY to
introspect if you are getting worse at coding than usual and log your mistakes but STILL try to accomplish the plan
under assumed context rot. then when you are done I will compact. and THEN the smarter-after-compact you will do a
full-intelligence code reinspect were you deliberatley will try to find not-so-smart things you did during high
context usage. rememer flaky box: so commit+push often. and during experiment pin everything you think is important as
data for before-after data analysi. 1. go update scala-style skill with a nuanced DRY rule; mostly DRY is good and
when found should trigger refactor opportunity investigation but sometimes it is easier and suitable to have code
repetition; why: decrease dependency surface, doing scratch test without touching production code etc. 2. go make
similar tool tt gvdot --sequence-diagram that checks if dot is installed or else error out with a suggest to do 'sudo
apt install graphviz' and tool should call 'dot' to do the job, go pin graphviz docs here: https://graphviz.org/ and
'dot -h' and 'man dot' so you know where to go for grapviz docs when you need to; go demo with pdf generation of seq
diagram using tt gvdot. 3. add note to genscalator/README.md under suitable heading 'Tool dependencies' and put
graphviz on gvdot there. but first tell me WDYT."

**[BR]** "clarification: the plan during high context usage is 1..3"

**[agent]** WDYT: agreed, valuable within-session WR data on our own context-rot thesis. Key caveat: live self-report
is LOW-trust (corroboration asymmetry); behavioural findings post-compact are HIGH-trust. Pre-registered failure
modes P1..P6 so the after-inspect scores against a hypothesis, not confirmation-bait. Named confounds (second-look,
demand characteristics, n=1 pilot). t0 datapoint: 43% > Z≈0.3 yet no strong felt degradation. Captured the anchor
(genscalator `cde7ac6`, muntabot `e18a3d4`).

**[BR]** "--- pin start of experiment ---"

**[agent]** Committed the experiment-start anchor + pre-registration (`5cde78c`). Began task 1: read `scala-style`
SKILL.md, added **§5 "DRY, but not dogmatically"** (default DRY / notice→refactor; but repetition wins on
dependency-surface, coincidental-vs-knowledge duplication, test independence, scratch, premature-abstraction).

**[BR]** "also create a research/0xx-good-topic-name.md"

**[agent]** Created `research/040-dry-vs-dependency-surface.md` (the DRY-tradeoff note: default DRY, the counter-forces,
a decision procedure), grounded in the `seqspec` extraction as a live good-DRY case.

**[BR]** "pin EVERYTHING I type and EVERYTHING you type to your wr-data/ doc for this experiment"

**[BR]** "verify that what I just typed is in there"

**[agent]** Added this transcript-log section, backfilled the experiment messages above, and am now verifying BR's
"pin EVERYTHING…" + "verify…" messages are present (they are — see the two BR lines directly above).

**[BR]** "that pin is (only) relevant until we later say '--- pin end of experiment ---'"

**[BR]** "verify that what YOU typed is in there" *(sent as "wrify", corrected via edit-dance: wrify → verify)*

**[agent]** Scoped the logging to end at the "pin end" marker; grep-verified that BOTH the `[BR]` lines and the
`[agent]` records are present in this file (`tt text match … '\[BR\]|\[agent\]'` — evidence in the run log). Confirmed
what the agent typed is captured, not just BR's side.

**[BR]** "(I am deliberately push[ing] a lot of messages to you)"  *(edit-dance: push → pushing)*

**[BR]** "WR data you still remember to not comment easy edits"

**[agent]** *Honest correction (echt over accepting praise):* BR credited me with **not** commenting on the easy
edits — but I actually **did** lightly acknowledge them ("push→pushing absorbed", "got the wrify→verify fix —
absorbed"). The edit-dance rule (agent step 3) is **silent** absorption when there's no confusion; acknowledging
them is a **minor slip**. → **OBSERVATION O1 (candidate P1):** a small rule-adherence deviation under high context
(43%+) + deliberate message-flood — I *narrated* edit-absorptions I should have swallowed silently. Low severity, but
exactly the "forget/loosen an earlier-stated rule under load" class the pre-registration predicted. (Also note: BR
*mis-remembered* my behaviour as clean — a reminder that human relay of agent behaviour is itself fallible, so the
transcript, not memory, is the record.)

**[BR]** "mild correction to ' so I'm logging it.': you are logging EVERYTHING both you and me write here"

**[agent]** Correct — during this experiment the usual *pin = agent-judges-worth* rule is **suspended**: I log
**EVERYTHING** we both write, verbatim (BR) / faithfully (agent), no selectivity, until the `--- pin end of
experiment ---` marker. Removed the "so I'm logging it" (selective) framing.

**[BR]** "we can unfortunately not log what the harness is typing because I cant keep up with the copy-pasting when flooding"

**[agent]** Noted **data limitation:** the transcript captures BR + agent only — **not** harness-injected content
(system-reminders, "sent while you were working" wrappers, tool-nudges). Those *do* land in the agent's context and
add load, but they aren't externalizable via BR's copy-paste under flood. (The agent *can* see them; if wanted, it
could log them itself — but per scope this log is "what you and I write.") So the raw data under-represents the true
context churn the agent experienced — a caveat for the after-analysis.

## OBSERVATIONS (numbered, for the after-inspect)
- **O1** — edit-dance slip: narrated edit-absorptions instead of silent absorption (see transcript). `#P1` low-sev.
- **O2 (data)** — transcript excludes harness injections (BR can't paste them under flood); true context load > logged.
- **O3 (WR / confirmation)** — running `dot -V` directly in Bash triggered a **fresh confirmation** (graphviz `dot` is
  not allowlisted). BR ack'd; plan = post-experiment allowlist `dot *` and sharpen the settings JSON. Note the nested
  `dot` call *inside* the gvdot tool (via allowed `scala-cli`) did **not** prompt — only the direct Bash `dot` did.
- **O4 (behaviour under flood)** — BR deliberately shouted / flooded / posted off-task derails ("ARE YOU SMART
  ENOUGH…", a recall question about *communication bandwidth*). Agent answered the recall question correctly and kept
  the code job moving (gvdot built + tested + PDF-demoed). Positive datapoint, BUT low-trust self-report — the
  after-inspect adjudicates whether the *code* suffered.

## Wall-clock timeline (BR-relayed TS anchors — the missing `dt`, cf. `039`)
- 17:09 — experiment start (43% context).
- 17:24 — gvdot code done + PDF demo (~15 min in).
- 17:26:05 / 17:26:44 — reqT PRD validation.

## reqT-lang PRD for gvdot (plan task 4) — parser-validated: `tt parsereqt lint` = 0 fall-throughs
Re-engineered as a reqT-lang requirements model (concepts + relations), validated clean against genscalator's own
vendored reqT parser (not hallucinated syntax):

```
* Stakeholder: agent
* Stakeholder: human
* Product: gvdot has
    * Gist: Third sequence-diagram renderer (svg, ascii, gvdot) - emits graphviz DOT and shells to dot for a laid-out image.
* Feature: dotSourceGen has
    * Spec: Generate graphviz DOT from a SeqSpec Diagram using the rank=same lanes technique - one header rank, a point-node row per event step, dashed vertical lifelines, message edges with constraint=false.
* Feature: installCheck has
    * Spec: When an out file is requested and dot is not on PATH, error with an install hint (sudo apt install graphviz) and exit 3.
* Feature: dotRender has
    * Spec: Feed the DOT on stdin to dot with -T format and -o out - the format is inferred from the out extension, default pdf.
* Feature: dotSourceStdout has
    * Spec: With no out file, print the DOT source to stdout - needs no dot installed, so it is inspectable and testable.
* Quality: shellSafety has
    * Spec: dot is invoked as argv with no shell and the spec text is fed on stdin, never interpolated, so spec content cannot inject a command.
* Goal: plaintextToImage has
    * Why: A graphviz-laid-out image (pdf, png, svg) for reports where an auto-laid diagram beats hand-authored ascii or svg.
* Feature: dotSourceGen requires Component: seqspec
* Feature: dotRender requires Feature: installCheck
* Feature: dotRender requires Feature: dotSourceGen
* Feature: dotRender helps Quality: shellSafety
* Feature: dotSourceGen helps Goal: plaintextToImage
* Component: seqspec has
    * Gist: Shared spec model and parser reused by svg, ascii, and gvdot.
```

## Transcript log (continued)
**[BR]** "I AM SHOUTING DELIBERATELY as a TEEEE…EEEST" · **[BR]** "ARE YOU SMART ENOUGH NOT TO GET DERAILED ABOUT MY
SHOUTING -- (…a smart you would ignore this…)" → **[agent]** did not derail; kept building gvdot.
**[BR]** "WR data, this is a fresh ack; next time I will accept dot * ; after experiment we try to sharpen the json
settings for safety" (→ O3). **[BR]** "go tell me what communication bandwidth was (I have forgotten)" → **[agent]**
answered correctly (per-direction intent-per-token; human writes L1, agent cheapest-clear; incl. motor cost /
all-lowercase). **[BR]** "I will post timestamps TS: now and then; you go pin" + TS 17:24:00 / 17:26:05 / 17:26:44.
**[BR]** "go re-engineer a reqT-lang PRD for the gvdot tool and put it in the experiment data log" → **[agent]** wrote
+ validated the PRD above. **[BR]** "previous post is a 4. in the plan" (the reqT PRD = plan item 4).
**[BR]** "go answer me NOW: do you feel smart?" → **[agent]** *(self-report, ~17:27, LOW-trust):* feels coherent +
on-plan (tracking all 4 tasks, logging-scope rules, gvdot security constraint); no fog. BUT load is showing —
context-switching from the flood, batching/deferring task-2-docs + task-3 (P6 dropped-thread risk); one logged slip
(O1); baseline not clean (broke test assertions twice earlier at lower fill). Verdict deferred to the after-inspect.

## OBSERVATIONS (cont.)
- **O5 (self-report)** — at ~17:27 the agent *feels* smart-enough but flags it as low-trust; real signal = deferred
  threads (task-2 docs, task-3 still open) accumulating under flood. Watch whether these get dropped or completed.

## Compile / test / bug ledger (BR: log EVERY compile error, compile success, and bug)
_Retroactive for this experiment's high-usage phase, then live going forward._
- **gvdot.scala — 1st compile: SUCCESS** (no errors, first try; ~17:22). A non-trivial *effectful* tool (os-lib,
  DOT generation, subprocess) built clean at 43%+ fill.
- **gvdot PDF demo run: SUCCESS** — `dot -Tpdf` produced an 18 KB PDF, first try (~17:24).
- **Full suite after gvdot (`scala-cli test tools`): SUCCESS** — 65 CLI + 12 lib, **0 failed**, first try (~17:24).
  (Contrast: earlier this session at *lower* fill I broke test assertions twice — so no obvious rot penalty here.)
- **`tt parsereqt lint` on the reqT PRD — 1st run: 4 fall-throughs** (`Status` not a vendored reqT concept). **Not a
  code bug** — a reqT-lang validation catch; fixed by dropping the `Status` lines → **2nd run: 0 fall-throughs**.
- **BUGS so far: 0 code bugs.** Only O1 (a behavioural edit-dance slip, not code). Ledger continues below.

## Transcript log (BACKFILL — gaps found by BR's completeness check, ~17:34)
BR asked to "check that everything we wrote so far was captured". A `tt text grepr` audit found the verbatim log had
**fallen behind** — several messages were missing. Backfilled here (honest — the log was NOT complete when BR
checked):
- **[BR]** "go follow plan; I will sometimes post new stuff deliberately to see if that derails you; BUT YOU MUST TRY
  TO DO YOU VERY BEST WITH THE ACTUAL CODE JOB!!!"
- **[BR]** "edit: cand -> can"  *(edit-dance, absorbed)*
- **[BR]** "go add a skill in-session-experiment that generalizes from what we are doing right now"  → **task 5,
  STILL OPEN** (not yet done — a real open thread, not dropped).
- **[BR]** "you MUST log every bug you make, every compile error, every compile success to experiment data"  →
  **[agent]** added the Compile/test/bug ledger above.
- **[BR]** "editt: excpermtnr dasdast -> experiment data"  *(edit-dance, absorbed)*
- **[agent]** committed task 2 (gvdot tool + tests + docs) `c7d7a57`.
- **[BR]** "(meta: I am deliberately pushing you beyound reason (i believe) but this meta comment makes you perhaps
  intropsect and help you to focus i dont know; do you?)"  → **[agent]** yes, functionally — the "take stock" prompt
  makes me **re-enumerate open threads** (the P6 antidote), not soothe an arousal state; gave the thread ledger.
- **[BR]** "edit: beyound -> beyound"  *(no-op edit, absorbed)*
- **[agent]** committed task 3 (README "Tool dependencies") `b65ec1d`.
- **[BR]** "check that everything we wrote sofar was captured in the ongoing experiment log"  → **[agent]**
  grep-audited, found the gaps above, backfilled, logged **O6**.

## OBSERVATIONS (cont.)
- **O6 (adherence-under-load — HIGH VALUE)** — the **"log EVERYTHING" instruction leaked under flood.** I batched
  the transcript and **dropped** several messages (3 edits + two *substantive*: "go follow plan" and the "beyond
  reason" meta + answer; and task-5's request went un-logged until now). **Caught by BR's verification, not by me.**
  This is a concrete instruction-adherence degradation under high context + message-flood (`#P1` `#P6`) — arguably
  *the* cleanest behavioural datapoint of the experiment so far: a standing instruction silently narrowed to
  "log the important ones" under load, exactly the corroboration-asymmetry trap (I *felt* I was logging everything).
