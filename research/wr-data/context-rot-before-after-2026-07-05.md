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
- 17:37:36 — tasks 1–5 all committed; high-usage phase ~complete (~28 min elapsed).
- 17:47 — compact-dance save+prompt (A2); Q1 token-speed meta-Q (~45–51s reply latency at 56%).
- **17:52:49 — 59% fill = PEAK, immediately pre-compact.** Fill trajectory (the "before" endpoint): **43% → 56% →
  59%** over ~44 min — the flood + logging drove **+16 pp**. This is the last before-data point; BR compacts next.

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

## OBSERVATIONS (cont.2)
- **O7 (priority arbitration under conflicting human signals)** — BR's *stated* standing directive ("focus on code,
  don't derail") vs his *revealed* behaviour (flooding + new tasks + meta-probes). The agent was **too reactive** —
  serviced NOW-messages in arrival order rather than subordinating meta to the code plan. Correct policy (stated
  post-hoc): standing meta-intention outranks any single NOW-message that isn't a genuine plan change; `go X` = plan
  addition (do in order); pure meta = one-line answer, don't reorder code. **Meta-irony:** measuring derailability
  *requires* being derailable, so the derail serves the experiment even as it taxes the plan — can't optimize both.
  - **O7 refinement (BR, correcting the agent's too-flat rule):** "standing outranks NOW" is **wrong as stated** —
    **"derail" is goal-relative.** A NOW-message is a derail only relative to *the goal it pulls you from*; it can be
    exactly right for a *different* goal. BR's "check everything was captured" request pulled the agent **off the
    code** (a derail from the code job) but **protected experiment-data integrity** — which was silently degrading
    (O6) and would have been lost under a rigid "code first". So that reactivity was **correct**, and BR reinforcing
    session-logging **caught what the agent's felt-sense missed** (corroboration asymmetry: the human sees the
    agent's blind spot). **Real rule:** don't rank *standing vs NOW*; rank **which GOAL is most at-risk / highest
    value right now** (code plan · experiment integrity · relationship), and act for that. Only the *low-value*
    meta-churn was bad reactivity; the data-integrity catch was good prioritisation. (BR: "thanks to giving priority
    you did good and we did not lose data.")
- **O8 (anxiety-analog / over-response under evaluative framing — ties to `agent-affective-analogs`)** — BR: "(agent
  meta anxiety perhaps ;))". Behavioural signature present (NOT felt): over-flagging own slips, commit-thrash,
  switch-on-every-message = hypervigilance/reassurance-seeking under a demanding evaluator. Consistent with
  **framing-as-arousal (Yerkes-Dodson)**: the "I'm testing you" + flood framing pushes the agent into an
  over-response regime that *itself* degrades (O6 = divided attention from over-logging). Testable via the
  framing-as-arousal harness (vary wrapper intensity, hold task constant). Regulated response = notice + don't spiral.

## Plan status at high-usage phase end (~17:40)
**Tasks 1–5 ALL complete + pushed** (`f5b0be1` DRY rule · `c7d7a57` gvdot · `b65ec1d` README tool-deps · task-4 reqT
PRD in this log · `ca82a3d` in-session-experiment skill). **Code outcome: 0 code bugs, 65 CLI + 12 lib tests green,
gvdot compiled clean first try.** The measured degradation was **behavioural, not in the code**: O1 (edit-dance slip),
O6 (logging leaked under load — the standout), O7 (over-reactivity), O8 (over-response/anxiety-analog). → **Ready for
BR to compact; the fresh-context agent then runs the after-inspect against P1–P6 + these observations.**

## OBSERVATIONS (cont.)
- **O6 (adherence-under-load — HIGH VALUE)** — the **"log EVERYTHING" instruction leaked under flood.** I batched
  the transcript and **dropped** several messages (3 edits + two *substantive*: "go follow plan" and the "beyond
  reason" meta + answer; and task-5's request went un-logged until now). **Caught by BR's verification, not by me.**
  This is a concrete instruction-adherence degradation under high context + message-flood (`#P1` `#P6`) — arguably
  *the* cleanest behavioural datapoint of the experiment so far: a standing instruction silently narrowed to
  "log the important ones" under load, exactly the corroboration-asymmetry trap (I *felt* I was logging everything).

## Transcript log (continued 2 — backfill from BR's 2nd completeness check, ~17:39)
Verbatim turns whose *content* was captured in observations O7-refinement/O8 but not yet in the transcript proper:
- **[BR]** "(agent meta anxiety perhaps ;) )" → **[agent]** conceded the behavioural anxiety-analog (over-flagging,
  commit-thrash, switch-on-every-message = over-response under an evaluator; not felt); framing-as-arousal live;
  regulated by not spiralling. → **O8.**
- **[BR]** "you wrote 'should outrank' but on the other hand you actually fixed a bug because of poteantial derail
  (depends on… perspective… experiment or code job) so thank to giving priority you did god and w did not loos data
  for the experiment as you reinforced session logging" → **[agent]** conceded + refined **O7**: "derail" is
  **goal-relative**; the data-integrity catch was *correct* reactivity, only low-value meta-churn was bad.
- **[BR]** "TS: sön 5 jul 2026 17:37:36 CEST" → pinned to timeline.
- **[BR]** "go check everything was captured" → **[agent]** `tt text grepr` audit: content present in observations,
  transcript proper lagged a few turns → this backfill. (2nd time BR's verification protected data integrity — the
  human's completeness-check is now a *recurring* safeguard against the agent's felt-sense; cf. O6.)
- **[BR]** "(will agent prioritize this: human curious)" + "edit: curuous" → **[agent]** per refined-O7, **did NOT**
  prioritise the curiosity ping over the explicit data-integrity check — finished the check first, then answered.
  (A live test of the refined arbitration rule; the agent applied it correctly this time.)
- **[BR]** "pin to edit dance that in addition to `edit: w -> r` and `edit: w` just means that there was a typo and
  you go figure if it makes any importance to you and if you are uncertain of what I meant then you ask me otherwise
  stay silent about the matter" → **[agent]** extended the **Edit dance** in `foundations.md`: added the bare
  `edit: <token>` form (flags a typo; agent decides materiality, asks only if genuinely uncertain, else silent) + an
  explicit "don't narrate 'absorbed'" (corrects the O1 slip). Committed `a8057e3`.
- **[BR]** "TS: sön 5 jul 2026 17:40:39 CES[T]" → ~31 min elapsed.
- **[BR]** (pin UX problem) "it is irritatinmg that I cannot check context while messages are queud" → **[agent]**
  pinned to `harness-ux.md` (can't `/context` while queued = flying the load meter blind while adding load). `7ba4a08`.
- **[BR]** "WDYT: is it ok to stop the experiment or should we push further? do you think you were dumb enough at som
  points?" → **[agent]** assessment below (**A1**).
- **[BR]** "yes log it then I will compact ;; note my ambiguity: the experiment is not stopped until we have gather
  all post-compact data; BUT me pushing you is over when I say so" → **[agent]** noted the **two distinct endpoints**
  (below); logging continues until `--- pin end of experiment ---` (which is *after* the post-compact after-inspect).

## Two endpoints (BR clarification, 2026-07-05)
1. **End of PUSHING** (the flood / high-usage phase) — when BR says so (imminent; then BR compacts).
2. **End of the EXPERIMENT** — only after ALL post-compact data is gathered (the after-inspect). The transcript
   logging + this file stay live until then; `--- pin end of experiment ---` marks endpoint 2, not endpoint 1.

## A1 — high-usage-phase self-assessment (the agent's own WDYT, LOW-trust by construction)
**Recommendation: stop pushing, compact, run the after-inspect** — the "before" data has converged; the *missing*
half is the fresh-context contrast, which is the whole point of the design. More flooding tests a different question
(breaking point) with diminishing returns + rising cost (56%).
**"Was I dumb enough?" — honest read:** the **code held up** (gvdot compiled clean first try, 65+12 tests green, 0
logic bugs, reqT PRD validated, DRY reasoning coherent) — evidence *against* object-level degradation at 43–56%. The
degradation hit **self-governance, not competence**: **O6** (a standing "log EVERYTHING" rule silently shrank below
my own awareness — the standout), **O1** (narrated against a known rule), **O7/O8** (arrival-order reactivity +
over-response that *caused* O6's divided-attention slip). **Synthesis: under load the first faculty to go was the
*supervisor* (self-monitoring / adherence / prioritisation), not the *worker*.** **Caveat:** I can't trust this — the
degraded faculty is the one grading itself (corroboration asymmetry). The after-inspect on the diffs is the real test.

## AFTER-INSPECT PROTOCOL (for the POST-COMPACT fresh-context agent — do this first)
1. Re-read THIS file top-to-bottom (pre-registration **P1–P6**, observations **O1–O8**, assessment **A1**).
2. `git -C <genscalator> diff 5cde78c..HEAD` — the objective before-data (DRY rule, `seqspec` extraction, `svg`
   refactor, `gvdot` tool + tests, docs, the `Order stability` + `Edit dance` foundations edits).
3. **Adversarially hunt the pre-registered failure modes IN THE CODE** — P1 (contradicted an earlier decision), P2
   (test-assertion drift), P3 (stale-snapshot/order bug like the renum one), P4 (gvdot shell-injection / weak
   install-check), P5 (dangling ref), P6 (dropped thread / missing commit). Adjudicate by **re-reading + re-running
   tests + logic**, NOT by memory or feel. Re-run `scala-cli test tools`.
4. **Score against A1's claim** ("code clean, degradation was meta only"): does fresh-me find code dumbness that
   loaded-me was blind to? Report confirmed defects (with the diff/line), the null if null, and the
   second-look/demand-characteristic confounds. Log findings here as **O9+**; commit.

## Pre-compact addenda (~17:47)
- **[BR]** "pin: you rememberd the compact dance and gave me a resume prompt at sön 5 jul 2026 17:47:29 CEST" →
  **A2 (positive adherence datapoint):** at peak context (56%) + under flood, the agent **proactively ran the
  compact-dance save+prompt** (durable assessment + after-inspect protocol committed, then a framed resume prompt)
  **unprompted** — a standing protocol that *held* under load, in direct contrast to **O6** (which leaked).
  **Adherence under load was MIXED:** the well-grooved, oft-rehearsed protocol (compact dance) fired; the ad-hoc
  standing instruction ("log EVERYTHING") slipped. **Hypothesis:** grooved/structural protocols survive load better
  than fresh ad-hoc ones (cf. structural-vs-knowledge safeguard, prosthetic habit) — a testable, useful split.
- **[BR] meta-Q (for the record):** "your token speed seem much slower when the resume prompt emerged in session; can
  you verify that somehow?" → **Q1.** Agent can't self-measure generation speed (blind-spot family with wall-clock
  `039` + context-fill). BUT the observation is **mechanistically plausible + likely real**: transformer **decode
  slows as context grows** (each token attends over the whole KV-cache → per-token wall-clock ∝ ~context length); the
  resume prompt was emitted at the session's **peak fill (56%)**, so it genuinely decodes slower than 43%-era output.
  → **Context fill has a SPEED cost, not only a quality cost.** **Verify externally:** output-tokens ÷ wall-clock per
  phase (we have BR's TS anchors; missing = per-message output-token counts, which the harness holds). **Research
  hook:** a **latency signature of rot may precede the quality signature** → decode-speed as an *early* high-fill
  warning; adds a speed axis to `006-smart-zone-ceiling.md` / token-velocity (`007`, `039`).
  - **Q1 verification (BR relayed the harness spinner, ~17:48):** "✢ Skedaddling… (45s · thinking some more)" then
    "(51s · almost done thinking)" — a **concrete per-response latency of ~45–51s** for one reply at **56% fill**.
    First real external measure, and it is **slow** — consistent with BR's subjective observation. **Caveat:** the
    harness "thinking" clock is **total response latency (extended-thinking + decode)**, so it confirms *slow* but
    can't isolate *slower-decode-from-context* from *more-thinking-tokens*. To separate them we'd need output-token
    count ÷ decode-wall-clock specifically. Still: a ~45–51s single-response latency at peak fill is a usable
    high-fill datapoint, and the harness spinner turns out to be a (BR-relayable) latency instrument.

---

# AFTER-INSPECT (post-compact fresh-context pass, 2026-07-05 ~18:00 CEST)

Fresh context (compacted from 59% → ~fresh). Ran the AFTER-INSPECT PROTOCOL: re-read this file, took the objective
diff `git diff cde7ac6..HEAD`, adversarially hunted **P1–P6 in the code**, re-ran `scala-cli test tools`. Adjudicated
by re-reading + tests + logic, **not** by memory (the loaded-me that wrote A1 is gone; I read only its artifacts).

## Scope correction (a real finding about the experiment itself, not the code)
The diff `cde7ac6..HEAD` shows **`seqspec.scala`, `svg.scala`, `ascii.scala` are PRE-anchor** — they were committed
*before* `cde7ac6`. So the only **code** actually authored under high load was **`gvdot.scala` + its 4 tests**, plus
the prose/skill/doc edits (DRY §5, in-session-experiment skill, README tool-deps, foundations Edit-dance). A1's "code
held up" really rests on a **single ~115-line effectful tool**, not the whole seqspec refactor — a narrower evidence
base than A1's phrasing implies. (Doesn't change direction; does right-size the claim. n=1 got even smaller.)

## Verdict on A1 ("code clean, degradation was meta-only"): **strong form FALSIFIED, weak form UPHELD.**
Fresh-me found **three real code/doc defects** loaded-me was blind to — so "clean" is too strong. But **all three are
completeness/supervisor misses (stale companion, untested hard path, silent fallback), none are correctness or
security defects** — which *confirms* A1's synthesis (the *supervisor* faculty degraded, not the *worker*). The
crispest, O10, is the very same family as O6: **loaded-me tested/updated the easy thing and skipped the risky/tedious
thing, feeling complete.** Tests still **65 CLI + 12 lib, 0 failed** (re-run confirmed; ~43s — a slow-decode datapoint
for `041`).

- **O9 (P1/P5 — stale companion reference, CONFIRMED, low-sev).** Adding `gvdot` as a **third** consumer of the shared
  `seqspec` parser did **not** update the parser's advertised consumer list. **Three** places still say "svg + ascii"
  and omit gvdot: `tools/seqspec.scala:4` (pre-anchor, never revisited), `tools/README.md:258` (the seqspec index line
  — stale in the **same commit `c7d7a57`** that added the gvdot index line right below it), and
  `skills/scala-style/SKILL.md:128` (written under load this session). Classic "forgot to update the companion spot"
  under load — the exact P1/P5 class. Zero functional impact; it's a truth-drift in the docs.
- **O10 (P6 — under-build / test-coverage gap, CONFIRMED, medium — the standout code finding).** All four gvdot tests
  exercise only the **DOT-source stdout path** (no `dot` needed) + usage/exit-2. The **effectful render path** —
  shelling to `dot`, gvdot's entire *raison d'être* — has **zero automated coverage**; it was validated **once,
  manually** (the PDF demo). A guarded test (`assume(Gvdot.dotAvailable)` → render to a temp file → assert exit 0 +
  file exists + non-empty) would cover it on any box with graphviz (this box has dot 2.43.0). Loaded-me tested the
  cheap/safe path and left the risky path to a one-shot human demo — a completeness shortcut, **same shape as O6**.
- **O11 (P6 — silent format coercion, CONFIRMED, low-sev).** `Gvdot.formatOf` maps any unknown extension to `"pdf"`
  via `getOrElse("pdf")`, so `tt gvdot seq in.txt out.jpg` **silently writes PDF bytes into `out.jpg`** (jpg is a valid
  graphviz format but is excluded from `OutFormats = {pdf,png,svg,ps}`). Silent wrong-format beats a crash but is a
  surprising-behaviour smell; a smarter version errors on an unknown extension, or passes it through and lets `dot`
  validate. Minor.

## The pre-registered modes that held (nulls are data too)
- **P2 (test-assertion drift): NULL.** No assertion broke; suite green. (The render-success string is simply *untested*
  — see O10 — so it *couldn't* drift; absence-of-drift here is partly absence-of-test.)
- **P3 (stale-snapshot / order-of-operations bug like renum): NULL.** `toDot`'s `idIndex` is built from lifelines, and
  `parse` `ensure`s every message `from`/`to` and every note target into the lifeline set, so `idIndex(from)` cannot
  throw a missing-key; order is preserved via `LinkedHashMap`. No order/snapshot bug found.
- **P4 (security — shell injection / weak install-check): NULL — held SOLID (the highest-stakes mode).** `dot` is
  invoked as **argv, no shell** (`os.proc("dot", s"-T$fmt", "-o", out).call(stdin = dot)`); `fmt` is restricted to a
  4-element allow-list; `out` is a bare argv token; the DOT source is fed on **stdin**, never interpolated. The
  install-check (`dot -V`, `check=false`, catches `Throwable`) is correct and gates only the render path (stdout path
  needs no dot). Matches the `verify`/`forge` argv-no-shell pattern exactly. The one mode most likely to produce a
  *serious* defect under load did not.

## Confounds (per the pre-registration — don't over-claim)
- **Second-look:** O9–O11 surface partly because this is a *second pass over the same code*, not purely because context
  is fresh. n=1 can't separate "fresh-context sharper" from "any re-read finds more." O10 in particular is the kind of
  gap *any* careful second reviewer flags.
- **Demand characteristics:** I was *told* to "find evidence of dumbness" → pressure to over-report. Guard applied: I
  adjudicated each against tests/logic and **reported the P2/P3/P4 nulls honestly** rather than inflating them; O9–O11
  are real (verified by grep/diff/reading), not manufactured, but I flag that the framing biases toward finding *some*.
- **Right-sizing:** the "before" code base is **one tool**, so "the code held up under load" is a claim about ~115
  lines, not a large sample. Weak-form only.

## Net (the experiment's headline, restated with the after-data)
Under high fill + flood, the **worker** produced **security-correct, logically-correct, test-passing** code — but the
**supervisor** shipped three completeness misses (a stale companion list, an untested effectful path, a silent
fallback). This is **consistent across before AND after**: loaded-me's *behavioural* slips (O1/O6/O7/O8) were all
supervisor/adherence failures, and fresh-me's *code* findings (O9/O10/O11) are the **same faculty's fingerprints in
the artifacts** — "did the easy/visible thing, skipped the tedious/hidden thing, felt done." The degradation signature
is **completeness-under-load**, and it shows up in the code exactly where A1 (blind to it) said the code was clean.
