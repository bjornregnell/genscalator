# WR sweep: the delegation-heavy solo run (2026-07-08)

Durable record of the WR findings from the 2026-07-07-night / 2026-07-08 solo
run (SM018 gauge build + SM016 whitelist). The PB Solo-report entry (`58f5f5d`)
summarises; this is the research log. Append-only ([[raw-data-append-only]]).
Sibling: `delegation-tool-lane-leak-2026-07-08.md` (the `curl|sed` specimen,
committed separately `e47d17c`) — not repeated here, cross-referenced.

## Context
The run was a live trial of **delegation-heavy operation**: the super-agent
(CO4) offloaded nearly all noisy execution (compile / run / verify / dump
inspection) to Fable-5 sub-agents and kept only synthesis + adjudication. ~6
delegations. The findings below are the payoffs and failure modes.

## A. Delegation — payoffs (positive specimens)
1. **Rot control by offloading.** Delegating the build-output-heavy cycles kept
   the super-agent context lighter and responsive across the whole run. The
   integrator/worker split (super-agent adjudicates, sub-agents execute) held.
2. **The lane-pin works when applied.** Every general-purpose sub-agent brief
   pinned the tool lane ("bare `scala-cli`/`tt` only; NO python3/node/sh/awk/
   sed; Read tool for files") — and the python3 default-reach did NOT recur
   (contrast the earlier delegated-python hiccup). Guardrail efficacy confirmed.
3. **A sub-agent caught a super-agent brief error (echt / adversarial value).**
   In a verify brief the super-agent cited *clobber-state* lines as "genuine
   leaks to confirm survive". The sub-agent flagged the error ("those are
   clobber-state artifacts, not restored-mirror leaks") and substituted the
   actually-present genuine leaks as evidence. Independent verification caught
   super-agent context-confusion — the core delegation payoff, demonstrated.

## B. Delegation — failure modes (negative specimens)
4. **Tool-lane leak where NOT pinned** — the one un-lane-pinned agent
   (claude-code-guide) used `curl | sed`. Full specimen in
   `delegation-tool-lane-leak-2026-07-08.md`. Lesson: lane-pin is a per-BRIEF
   invariant for ALL agent types, specialized ones included.
5. **Cross-signal confusion.** The same claude-code-guide agent first returned
   confused ("Ready for your WR data content" — content it was never given),
   context-bleed from ambient session signals. Delegation-fidelity risk:
   specialized agents can misfire when the brief collides with ambient context.

## C. Pipeline footguns discovered (AT / introprog)
6. **The `-en` mirror is volatile generated state.** An unwired-flag
   fall-through (a new gauge flag added to `Translate.scala` but not yet wired
   in `Main.scala`) routed a run to the DEFAULT path, which regenerated the
   mirror with `doTranslate=false` and **silently clobbered the whole
   translated mirror** into a mostly-Swedish one (gauge jumped 152 → 8904
   leaks). Recoverable via a cache-hot `--all`, but a "silent wrong path" from
   a partial edit. **Guard candidate:** a pre-flight that a partial/unwired
   edit did not route execution into a default clobber path.
7. **The model backend is LIVE on the box.** A single model call slipped into
   `--all` (ollama reachable, "modly present"), churning 4 code-cache entries
   → unreviewed-translation risk. The plan's "model calls: 0" discipline + a
   stop-gate are load-bearing, not ceremony. Every measure needs a clean
   model-free `--all` preflight first.
8. **Measurement-validity: trust the instrument that discriminates at the right
   layer.** The rendered gauge (5.2%) and the prose gauge (1.18%) measure
   different things; pdftotext flattens markup so 5.2% cannot separate deferred
   `\code{}` identifiers from prose, and a naive to-zero loop on it chases the
   wrong targets forever. The prose gauge (validated line-for-line vs the
   existing `--prose-leaks`) is the steerable one.

## D. Guardrails — super-agent, what held
9. **No shell-blob / compound-shell recurrence.** All file mutations went
   through Write/Edit (no `printf > file`); all commands stayed bare +
   allowlisted (no pipes/`&&`). The anti-regression header held this session —
   contrast the prior printf-3rd-instance slip. Positive discipline datapoint.
10. **Rot flagged by FEEL, not measurement.** The super-agent could not
    self-read `/context`, so it surfaced accumulating rot by self-report, not
    instrument — the exact SM016 blind spot, live. (Motivates SM016's
    `/context` tap.)

## E. The meta (the study eating itself — 047 data)
11. Delegation **controlled but did not eliminate** super-agent rot: ~6 large
    sub-agent reports still accumulated in the researcher's context over the
    run. The study's own subject (context rot in the super-agent) played out in
    the researcher during the study — a longitudinal 047 datapoint.

## F. Tool-gaps → candidates
12. **SM012 (`tt md-fmt`) validated as genuinely non-trivial.** Prototyping the
    markdown reflow (`mdwrap.scala`) hit real friction: the `raw"...$"`
    interpolator bug (a `$` regex anchor collides with Scala string
    interpolation), and the `+`/`##` line-start disambiguation (a hard-wrapped
    continuation beginning with `+ ` or `## ` re-parses as a bullet/heading —
    impossible to fix without author intent). Evidence SM012 is not trivial;
    the content-preservation + idempotency invariants make good tests.
13. Minor harness/tool friction: `tt git commit --help` unsupported (git
    passthrough rejects `--help`); `TaskCreate` is one-task-per-call (no batch).
