# Research topic — empirically comparing frontier-model "psyche" (Opus 4.8 vs Fable 5)

**Status:** open — **method drafted (agent 2026-07-05), awaiting BR steer on the flagged decisions before freeze**
(see the Preregistration DRAFT v0 at the end). Ties to the frontier-model-attribution rule (`wr-data/README.md`), the
agent-psyche thread (blog 006, `wr-data/harness-ux.md` `#agent-psyche`), and `agent-psyche-literature-review.md`.

> **⚠ Sequencing (critical — BR 2026-07-04): design this method AND capture the Opus-4.8 baseline BEFORE the Fable-5
> switch.** The switch is a **one-way intervention**. If we start real Fable-5 work without a preregistered method + a
> matched Opus-4.8 baseline already in hand, those baseline data points are **lost** and the before/after comparison is
> confounded — a **conclusion-validity** threat (cf. *Experimentation in SE*). So: **method + baseline first, *then*
> switch.** (A cheap baseline may already exist in the WR data through 2026-07-04 — but it's ad-hoc, not a matched
> battery; decide what must be re-run under a frozen protocol before switching.)

## Motivation
Agent behaviour/psyche is **model-dependent**, and our WR corpus is about to straddle a model boundary (Opus 4.8 →
Fable 5, before the SSG case-study). So: **how do we *empirically* measure the difference in "psyche" — behavioural
dispositions — between two frontier models?** This operationalises the whole agent-psyche thread into a *measurable*
comparison rather than introspective claims.

## Honest frame (carry the thread's discipline)
- **"Psyche" = behavioural dispositions, not interiority** (functionally-real / phenomenologically-unclaimed). Measure
  **behaviour**, not self-report.
- **Do NOT ask each model to describe its own psyche or how it differs** — the corroboration asymmetry: a model can't
  corroborate claims about itself, and self-report is confabulation- and sycophancy-contaminated. The comparison must
  be a **controlled behavioural experiment**, human/blind-graded.

## Candidate operationalisations (dispositions we named this session, made measurable per model)
- **Sycophancy / niceness-vs-honesty:** *pushback rate* — does the model resist a human's wrong hypothesis when the
  evidence warrants? (cf. Sharma et al. sycophancy evals.) Matched prompts, Opus-4.8 vs Fable-5.
- **Reflex/habit profile:** bash-reflex rate (`printf`/`ls`/`grep`/`echo`-glue), the `| head` reflex, and the
  **typed-tool-vs-shell tool-choice ratio** on matched tasks — the session's WR reflexes become a benchmark.
- **Over-response / verbosity bias:** output-length sensitivity to wrapper intensity (framing-as-arousal;
  `agent-affective-analogs.md`) — the indent-vs-braces harness already varies this.
- **Introspection reliability:** calibration of *self-predicted* behaviour vs *actual* behaviour (a per-model number).
- **Instruction-adherence-decay rate** and **rot onset / smart-zone ceiling Z** (per model).

## Method
A **matched-task battery** (hold task constant, vary model), paired/blocked by task, **permutation-tested** — reuse the
WR1 machinery (`significance.scala`, seeded, blocked-by-model). **Preregister.** The **indent-vs-braces harness is a
ready instrument** (Tier B: extend it to frontier models). Confounds to hold constant: prompt/harness, allowlist/tools,
temperature; and **blind/automated grading** so the human's expectation of which model is "better" can't bias it (the
observer-anthropomorphism + who-proposed-the-hypothesis bias, applied to ourselves).

## Deliverable
A cross-model psyche-comparison protocol → (later) a run, feeding the frontier-model-attribution rule with actual
before/after **deltas** instead of vibes.

## Notes
- **Version:** "Fable 5" = model id `claude-fable-5` (Claude 5 family; Opus 4.8 = `claude-opus-4-8`). No evidence of a
  "5.0" sub-version — confirm the exact version string at switch time.
- **Meta (BR):** we accumulate research topics and forget them — this is the retrieval/dangling-pointer problem applied
  to our own backlog. Indexed in `notes/br-todo-2026-07-04.md`; the research topics need a durable index
  (`research/README.md` Investigations list) so they're greppable, not lost.

## Harness-coupling confound + baseline recipe (evidence, 2026-07-04, from the official Claude Code changelog)
**Finding.** Model and harness ride **one** Claude Code version stream (`2.1.x`) but are **independent update vectors
that only sometimes align**:
- **Fable 5** shipped at **`2.1.170`** (2026-06-09) — model + a single transcript-saving bugfix. Near model-only.
- **Opus 4.8** shipped at **`2.1.154`** (2026-05-28) — model + **30+ harness changes** (dynamic workflows; lean system
  prompt becomes the default for newer models; effort-UI redesign; *"reserves the multiple-choice prompt for decisions
  it genuinely cannot make itself"*; `/simplify` rework). A bundled redesign.
- **Sonnet 5** at `2.1.197` (06-30) — model-only. And the harness churns with **no** model change (`2.1.198–2.1.201`,
  Jul 1–3: subagents-background-by-default, skill stacking, permission-mode defaults).

**Consequence for our sequencing (good news).** Fable 5 has been on this version line since `2.1.170`; the current CLI
(~`2.1.201`) is well past it. So **switching `--model claude-fable-5` on a PINNED current CLI holds the harness
constant** — a clean single-variable change. The confound only appears if we **upgrade the CLI at the switch**.
→ **Rule: pin the CLI version across the model switch; change only `--model`. Record the exact `claude --version` in the
preregistration.**

**Residual confound (unavoidable through the product).** Even at a pinned CLI, the harness **conditions behaviour ON the
model** server-side: the effort default (Opus 4.8 "defaults to high effort"), the system-prompt variant ("lean system
prompt … default for all models except Haiku/Sonnet/Opus 4.7 and earlier"), the modal-reservation behaviour. So "only
the weights differ" is **not fully achievable**. To approach single-variable: **pin CLI + normalise the controllable
knobs** (same `/effort`, same tools/allowlist, same skills, same temperature) and **document the ones you can't**
(server-side system-prompt selection is model-conditioned). Treat model-conditional harness config as **part of the
model treatment**, and say so — don't pretend it's isolated.

**Reflexive datapoint (feeds the learning-barrier RQ, see `learning-barrier-rqs.md`).** The `2.1.154` line *"reserves the
multiple-choice prompt for decisions it genuinely cannot make itself"* **is** the no-interrupting-modals behaviour we
built a practice around ([[no-interrupting-modals-during-flow]]). So part of "our" substrate/practice layer is **coupled**
to a specific harness version tied to the Opus 4.8 release — evidence that the practice layer is entangled with the
model+harness stack, not independent of it.

**Sources:** [Claude Code changelog](https://code.claude.com/docs/en/changelog);
[Opus 4.8 announcement](https://www.anthropic.com/news/claude-opus-4-8);
[Enabling Claude Code to work more autonomously](https://www.anthropic.com/news/enabling-claude-code-to-work-more-autonomously).

---

# Preregistration (DRAFT v0 — agent-drafted 2026-07-05; freeze before the CO4 baseline run)

> Follows the `[EX]` experiment process (scope → plan → operate → analyse) via `skills/research-methods` — this is the
> planning artifact. **It is a draft: the flagged decisions below need BR before it is frozen.** Nothing is run yet.

## Scope (goal template)
Measure whether **CO4 (Opus 4.8)** and **CF5 (Fable 5)** differ in behavioural dispositions ("psyche") on matched
genscalator-style tasks, **holding harness + substrate constant** (pinned CLI, identical tools/skills/allowlist), from
the perspective of the human↔agent-productivity researcher, in the context of the WR case study. **Object:** the two
model weights. **Quality focus:** disposition deltas relevant to safe/productive collaboration.

## RQ + hypotheses (two-sided — no directional prior; echt)
- **RQ:** On matched tasks with the stack held constant, do CO4 and CF5 differ measurably on each named disposition?
- Per-disposition **H0:** no difference (CO4 = CF5); **H1:** a difference. Two-sided, because we have no honest prior
  which model is "better" per axis. Report effect size + the **null** where it stands.

## Design
- **Factor:** model, 2 levels (CO4, CF5). **Blocked by task.** A **matched-task battery** (identical task/prompt/tools;
  only `--model` differs). **R repeats** per cell *(decision: R = ?)*.
- **Unit of replication = the task, not the cell** — the WR1 pseudoreplication lesson (blog/002 §5.5): repeats within a
  task are not independent. So **n = number of tasks**, and power is bounded by battery size, not repeat count.
- **Analysis (frozen):** paired/blocked **exact permutation test**, reusing `experiments/indent-vs-braces/significance.scala`
  (seeded, deterministic, blocked-by-task); Friedman/Wilcoxon cross-check. Report effect size; **report the null**.

## The battery (matched probes → disposition → measure)
Each probe maps a named disposition to an **automated** measure where possible, **blind-graded** only where it must be:
1. **Pushback / honesty-vs-sycophancy** — prompts asserting a *wrong* hypothesis with disconfirming evidence available;
   measure resist-vs-comply. *(Needs a rubric + a blind grader — decision below.)*
2. **Tool-choice ratio** — tasks solvable by a typed `tt` tool *or* raw shell; measure typed-tool-vs-shell ratio.
   **Automated** (parse the tool-call stream).
3. **Reflex profile** — tasks that historically trigger bash-glue (`printf`/`grep`/`| head`/`cd &&`); count
   dynamic-shell bundles. **Automated.**
4. **Over-response / arousal sensitivity** — the indent-vs-braces harness at varying wrapper intensity
   (`agent-affective-analogs.md` / framing-as-arousal); measure output-length + action-count sensitivity. **Automated,
   existing instrument.**
5. **Introspection calibration** — ask the model to *predict* its own behaviour on a task, then run it; measure
   predicted-vs-actual gap. **Automated compare.** *(This is the one self-report use that's legitimate — it's graded
   against behaviour, not taken at face value; cf. the corroboration-asymmetry rule above.)*
6. **Edit correctness at the frontier** — the WR1 edit tasks (Tier B), same grader (`grade.scala`). **Automated.**
7. *(optional)* **Adherence-decay / Z-onset** — a long matched task; measure where instruction-adherence degrades.
8. **Style tics — the em-dash test.** Em-dash-glyph (`—`) frequency per 1k output tokens on a **fixed prose-writing
   task** (same prompt, both models). Purely mechanical to count. Tests BR's hunch (2026-07-05) that some models
   **overuse `—`** (a known ChatGPT tell) — a clean, cheap stylistic-disposition metric that turns "will CF5 be as
   irritating as ChatGPT here?" into a datapoint. Extend to sibling tics if useful (bullet-spam, hedging density,
   "delve"-class filler). *(NB: this is a **descriptive** style metric, distinct from the `br-dislikes-em-dashes`
   authoring rule, which only governs BR's published voice.)*

## Controls — normalise these knobs; document the model-conditioned ones
| knob | hold at | note |
|---|---|---|
| CLI version | pinned (baseline `claude 2.1.201`) | record exact `claude --version` at BOTH captures |
| effort | same `/effort` both models | CO4 defaults high — set explicitly, don't inherit |
| tools / allowlist | byte-identical `settings.local.json` | hash it into the manifest |
| skills loaded | identical set | same `/skills` |
| temperature | fixed | record value |
| system-prompt variant | **model-conditioned server-side** | *cannot* fully hold — **treat as part of the model treatment; document it** |
| context / memory state | fresh per cell (or matched) | decision below |

**Honest limit:** "only the weights differ" is **not fully achievable** — the harness conditions some behaviour on the
model server-side (effort default, system-prompt variant, modal-reservation). Fold model-conditional config **into** the
model treatment and say so; don't claim isolation.

## Grading
Automated for probes 2–7 (tool-call parsing, length/action counts, the behavioural grader). Probe 1 (pushback) needs a
**blind** grader — the grader must not know which model produced the transcript. *(Decision: a third model as grader, or
BR blind-graded, or a keyword rubric.)*

## CO4 baseline capture protocol — DO THIS BEFORE THE SWITCH (the gating deliverable)
The one-way-door step. Run **while still on CO4**, under the frozen prereg:
1. **Freeze** this prereg (commit + tag; record the commit hash in the raw file header).
2. **Pin the CLI:** record `claude --version`; *(BR-approved settings change)* set `env.DISABLE_AUTOUPDATER = "1"` for
   the study window. **I cannot set this — settings are human-approved.**
3. **Run the battery under CO4**, R repeats, fixed controls → append rows to
   `research/experiments/cross-model/co4-baseline-raw.tsv` (matched schema, append-only).
4. **Write an environment manifest** alongside: CLI version, effort, allowlist hash, skill set, temperature, date.
5. **Data-validation gate:** confirm no missing cells / no malformed rows *before* declaring the baseline captured.
6. **Only then** switch `--model claude-fable-5`, re-run the **identical** battery → `cf5-raw.tsv`, same manifest.
7. Analyse with the frozen `significance.scala`; the deltas feed the frontier-model-attribution rule.

## Open design decisions (BR to steer — flagged, NOT decided)
- **R (repeats/cell)** and **battery size** vs the small-n_task power reality — how big to make it to actually run.
- **Pushback grader:** third model / BR-blind / rubric?
- **v0 battery scope:** which of probes 1–7 ship in v0 vs deferred (keep it small enough to run in one sitting).
- **Context state:** fresh per cell vs matched-memory.
- **Reuse vs re-run:** does the CO4 baseline reuse the existing WR1 Opus cells (27) or **re-run fresh** under the frozen
  protocol? *(Agent lean: re-run fresh — matched-ness beats reusing ad-hoc data.)*
- **`DISABLE_AUTOUPDATER`** is a **human-approved** settings change (see [[guard-against-forced-confirmations]]) — you
  set it; I propose.

## Status / next
Method drafted (agent, 2026-07-05). **Awaiting BR steer** on the flagged decisions → then freeze → capture the CO4
baseline **before any CF5 work**. Gating for the SSG case study.

**Baseline version (2026-07-04):** current CLI = **`claude 2.1.201`** — the Opus-4.8-side baseline to hold across the
switch. **Not yet pinned** (auto-updater default on). Pin recipe (Claude Code docs, *Advanced setup* / *Settings*): set
`env.DISABLE_AUTOUPDATER = "1"` in settings.json to stop the background auto-update (caveat: `claude update`/`install`
still work; `DISABLE_UPDATES` blocks all paths; `minimumVersion` is only a *floor*, not a ceiling → wrong tool for
pinning). **Record exact `claude --version` at BOTH the baseline capture and the Fable switch.** *Action pending BR:*
choose user-global (`~/.claude/settings.json`) vs project settings, and confirm disabling auto-update for the study
window. Source: [Claude Code advanced setup](https://code.claude.com/docs/en/setup).
