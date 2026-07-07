# SYNTHESIS — Structure over willpower (command hygiene, guards, the allowlist), 2026-07-07

- **What this is:** a WR-data SYNTHESIS artifact drawing together the command-hygiene regression specimens,
  the guard-stall-invisibility finding, and the substrate-power question (RT048). It is a *reading* of the
  raw specimens, not new raw data; every claim below is grounded in a named specimen file (+ section). The
  raw specimens are untouched (append-only discipline, [[raw-data-append-only]]).
- **Primary sources:** `command-hygiene-regression-2026-07-06.md` (specimens S1–S5),
  `guard-stall-invisible-to-agent-2026-07-07.md`, `compound-command-approval-halt-2026-07-07.md` (S6),
  `substrate-regression-candidates-2026-07-05.md`, `broad-allowlist-aversion-2026-07-06.md`,
  `genscalator-self-dev.md`, `harness-ux.md`, `instrument-deadlock-and-throughput-2026-07-06.md`;
  `../048-substrate-content-power-over-tool-discipline.md` (RT048),
  `../039-can-we-give-agent-introspection-wall-clock.md` (RT039).

---

## (a) Taxonomy of the command-hygiene regression specimens

The core series (S1–S6) plus the earlier corpus decomposes into five recurring **shape classes**, each a
distinct way the agent's emitted command defeats the bare-command prefix allowlist or trips a guard.

### Shape class 1 — `cd`-compounds (`cd … && …`, `cd … ; …`)

Defeats the prefix allowlist (the compound no longer starts with an allowlisted head) AND trips dedicated
guards (untrusted-hooks for `cd && git`; path-resolution-bypass for `cd` + redirection).

| id | specimen | shape | when | detected by | cost |
|----|----------|-------|------|-------------|------|
| S1 | `command-hygiene-regression-2026-07-06.md` §1 | `cd <repo> 2>/dev/null; grep -n … 047-PLAN.md` | 2026-07-06, under rapid multi-thread load | BR caught it live; harness reported "Shell cwd was reset" | Prompt friction + cwd reset; flagged as overnight-run risk |
| S4 | `command-hygiene-regression-2026-07-06.md` §4 | `cd genscalator && git log … && echo --- && git status` — the FIRST tool call after a /compact | 2026-07-07, t=0 post-compact (Go #2 start) | Harness untrusted-hooks guard fired; BR "shouted" | Guard prompt at the very start of the ralph loop; the sharpest warp-fidelity datapoint (memory `commit-via-tt-git-not-raw-cd-git` was in context, not enacted) |
| — | `substrate-regression-candidates-2026-07-05.md` #1 | `cd <repo> && git add … && git commit -F … && git push` used all session | 2026-07-05 | Untrusted-hooks guard + BR's regression pin | Repeated prompts despite `tt git` (the sanctioned, allowlist-matchable path) already existing |
| — | `harness-ux.md` "cd + output redirection" | `cd introprog && ls … 2>/dev/null; ls …` | 2026-07-03 | Path-resolution-bypass guard | Manual approval for a pure read-only inspection |
| — | `harness-ux.md` "Bash-reflex re-fired in the MAIN loop" | `cd <repo> && for f in …; do …; done` — < 5 min after committing blog 004/005, the posts cataloguing this exact reflex | 2026-07-04 | BR: "AARGH WR data"; cwd reset | Prompt + the strongest evidence that maximum self-knowledge salience does not prevent the act |

### Shape class 2 — pipe/semicolon assembly bundles (allowlisted head, unallowlisted residue)

`Bash(scala-cli *)` etc. prefix-match only a command that IS that command; any `| tail`, `; echo`,
`| head`, `| grep` residue is a separate, unallowlisted sub-command → the whole blob prompts.

| id | specimen | shape | when | detected by | cost |
|----|----------|-------|------|-------------|------|
| S3 | `command-hygiene-regression-2026-07-06.md` §3 | `scala-cli run <file> 2>&1 \| tail -25; echo "exit=${PIPESTATUS[0]}"` | 2026-07-06, Go #1 feasibility pilot | BR noticed the stall; agent did not | **Stalled a solo run** on a permission prompt mid-pilot |
| S6 | `compound-command-approval-halt-2026-07-07.md` | `grep … \| head; echo ===; python3 -c "…"` (semicolons + `python3 -c`) during final peer review of the 047 writeup | 2026-07-07 (overnight) | BR found it on return | **~1 overnight stall** of an otherwise-finished review task (zero data loss) |
| — | `genscalator-self-dev.md` table rows 1 & 3 | `echo "=== …"; scala-cli run …; echo …; … echo "exit=$?"` and the `$B`-var + `; echo` + `grep`-pipe smoke-test bundle | 2026-06-27 and 2026-07-03 | Guard ("Contains simple_expansion"); BR flagged the unreviewable blob | Prompts; the lesson that the reflex lives in the *test scaffolding*, not the target action → led to `tt verify` |
| — | `harness-ux.md` "Bash-reflex cluster → typed tt tools" | `readlink -f …; echo "---"; ls … \| head; …`, `echo "==="; tt files …; echo "==="; tt files …` (echo-glued multi-tool assemblies), `$(which …)` substitution | 2026-07-04 | BR live-flagged 3x; `$(…)` tripped `simple_expansion` and its prompt caught an accidental mid-typing "yes" | Prompts + a raced, unreviewed approval (authority-anchor corruption) |

### Shape class 3 — raw habitual tool instead of the disciplined/allowlisted path

Not a compound: a single bare command, but the *wrong* tool — the trained shell habit beating the typed
or harness-native path that is actually allowlisted.

| id | specimen | shape (raw → correct) | when | detected by | cost |
|----|----------|----------------------|------|-------------|------|
| S2 | `command-hygiene-regression-2026-07-06.md` §2 | raw `curl -sS -o … <pdf-url>` → `tt web get` (which, nota bene, could NOT save binary to file: a genuine tooling gap, not only a regression) | 2026-07-06 | BR caught it ("WR data; regression") | Prompt friction; exposed the `tt web --out` gap |
| S5 | `command-hygiene-regression-2026-07-06.md` §5 + `guard-stall-invisible-to-agent-2026-07-07.md` | shell `tail -n N <file>` (not allowlisted), five-plus times, to monitor the background coding matrix → the Read tool (allowlisted for the path, and the background-bash guidance said so) | 2026-07-07, ~01:00–01:56 CEST, AFK ralph loop | **Only BR** — the agent never registered a single stall | **Repeatedly stalled the AFK loop**; the flagship invisibility datapoint |
| — | `harness-ux.md` "printf > file commit-message reflex" | `printf '…' > /tmp/x; tt git … --message-file /tmp/x` → Write tool + bare `tt git` | 2026-07-04 (recurred several times in one session) | BR flagged it again | Prompt noise; textbook knowledge-safeguard failure |
| — | `harness-ux.md` "bash reflex reproduced in a SECOND (subagent) instance" | subagent used raw `curl` then `command -v tt` + `head`/`echo` compounds one turn after self-correcting | 2026-07-04 | Subagent self-report + main-loop review | Evidence the reflex is a **model-level trained disposition**, not one session's drift |
| — | `substrate-regression-candidates-2026-07-05.md` #2 | direct `dot -V` (not allowlisted) instead of `tt gvdot` / nested-in-scala-cli | 2026-07-05 | Prompt idled the agent during an AFK gap | AFK idle time |

### Shape class 4 — dynamic shell constructs the analyzer cannot statically prove safe

`$var` / `$(…)` / `for`-loops / heredoc-`python3 -c`: the guard fires on unprovability, not on effect.
Specimens: the `$B` smoke-test bundle and `$(which …)` above (classes 2's rows), the `sed -n '/…/,$p'`
that manufactured the Enter-race confirmation (`harness-ux.md` "Input-focus race"), and S6's `python3 -c`.
The cost is double: a prompt AND an unreviewable reason-string for the human
(`harness-ux.md` "Permission-parser internals surface as the reason string").

### Shape class 5 — the broad-grant temptation (human-side twin)

Not an agent command shape but the allowlist's failure mode under the same pressure: the harness's
"yes, and don't ask again" offers **over-broad rules** (`mv *`, `git *`, `claude *`) whose acceptance by a
tired human would durably defeat the narrow-allowlist premise. Specimens:
`broad-allowlist-aversion-2026-07-06.md` (the `Bash(mv *)` offer BR refused: "THAT I REALLY DO NOT WANT");
`harness-ux.md` "Too broad allow suggestions" (`claude *` offered for `claude --version`) and the
`git *` "don't ask again" analysis in `genscalator-self-dev.md` (would silently blanket-approve the
destructive verbs BR explicitly denylists). The cure applied there: narrowest-rule curation + global
destructive denies, human as the authority anchor.

---

## (b) The STRUCTURE-OVER-WILLPOWER case: three independent arguments

The dogma ("discipline is not the fix; the substrate is") rests on three separately-evidenced legs. Each
leg kills a different rescue-fantasy for willpower.

### Argument 1 — discipline regresses under load (willpower fails exactly when needed)

**Specimens: S1, S2, S3** — three regressions in ONE session (2026-07-06), all diagnosed in the raw file
as "discipline slippage under sustained rapid-multi-thread load": the burst of parallel work narrowed
attention and the habitual raw form (`cd ; grep`, raw `curl`, `| tail; echo`) beat the disciplined bare
form. The sharpest single instance is the 2026-07-04 `cd && for`-loop re-fired **under five minutes after
committing the blog posts that catalogue that very reflex** (`harness-ux.md`): self-knowledge at maximum
salience did not prevent the act. `substrate-regression-candidates-2026-07-05.md` names the mechanism
("absorption-regression: when the primary task fills attention, the agent defaults to the low-effort raw
path") and RT043 (guardrail adherence under load) is the research sibling. Crucially, S1–S2 also show the
**meta-monitor failing first**: BR caught both; the agent's self-monitoring degraded before its output did
(joint-rot-vigilance, agent half down).

### Argument 2 — willpower does not survive a warp (recall is not enactment)

**Specimen: S4.** The very first tool call after a /compact — zero load accumulated — was a compound
`cd && git`, while the exact countermanding memory (`commit-via-tt-git-not-raw-cd-git`) was **present in
the recalled context**. The rule survived the warp as text; the enacted discipline did not. This is study
047's facts-carry/enactment-leaks pattern produced *by the researcher on itself*, at t=0. **Specimen S6**
independently replicates it in a different agent (the reviewer): the compounding-defeats-the-allowlist
rule was in durable memory (three separate pins), "recalled but not enacted," one day after the same class
was logged for the researcher. Two agents, two days, same signature: a resolution ("I will emit bare
commands") is context-state, and context-state is exactly what a warp reconstructs lossily. Hence the fix
that DID get adopted lives in the artifact that reloads on every warp — the anti-regression header at the
top of the resume prompt ([[resume-prompt-anti-regression-checklist]]) — persuasive substrate, placed
structurally.

### Argument 3 — you cannot perceive-to-correct an invisible failure (time-blind + stall-blind)

**Specimens: S5 + `guard-stall-invisible-to-agent-2026-07-07.md`.** A guard-stall that the human later
approves returns the command's **normal output**; there is no "you were blocked for N minutes" metadata
and the agent has **no clock between tool calls**. So the agent cannot distinguish "ran instantly" from
"stalled for an hour then got cleared." Evidence: during the AFK ralph loop the agent ran unallowlisted
`tail` five-plus times, stalling the loop each time, and **never registered a single stall** — it learned
the loop had halted only when BR told it. This is not carelessness that attention fixes; it is
**perceptual incapacity**, a cleaner form of the corroboration-asymmetry limit ("a failure mode I cannot
sense at all," not merely "my self-report is unreliable"). Arguments 1 and 2 say willpower is unreliable;
argument 3 says willpower is *inapplicable* — no amount of care detects a stall the agent's senses cannot
represent. The irony logged in the specimen seals it: the stalling command was the agent's *diligence*
(monitoring the background job); the vigilance act was itself the friction source, and the agent was blind
to it.

---

## (c) Design implications

1. **Guard-free-by-construction is load-bearing BECAUSE the failure is agent-undetectable.** If stalls
   were visible, an AFK loop could self-correct (notice, re-emit bare, continue). Since they are not
   (argument 3, S5), the only robust defenses are (a) a command set that *cannot* trip a guard, verified
   in advance, and (b) the human as the sole detector — and (b) is by definition absent in AFK. So
   "guard-free" is not prompt-annoyance hygiene; it is the *only* agent-side mechanism that works in the
   one regime (unattended) where the failure is uncorrectable. Stated first in
   `guard-stall-invisible-to-agent-2026-07-07.md` §Consequences-2; this synthesis just names its rank:
   for AFK work it is the primary safety property, not a nicety.

2. **The tt-clock (RT039) closes the perceptual blind spot from the agent side.** RT039's 2026-07-07
   section (BR steer: "we need to give you a tt for that") promotes a `tt time` / elapsed-since-mark tool:
   time measured externally (never self-reported — corroboration asymmetry), cheap, allowlisted. With it,
   a large elapsed on a trivial command reads as "that stalled on a guard" — the S5 failure class becomes
   detectable, hence learnable, hence self-correctable. Complements, not replaces, human detection.

3. **The harness delay-signal is the upstream fix.** Proposed in
   `guard-stall-invisible-to-agent-2026-07-07.md` §Structural-fixes: the harness could annotate a tool
   result that was delayed by an approval prompt ("delayed N min awaiting approval"). That would make the
   stall visible *at the source*, with no clock arithmetic or threshold calibration. Not agent-buildable;
   flagged for BR as an upstream ask (same family as the harness-ux upstream asks: surface the allowlisted
   sub-part of a compound as pre-cleared, human-readable reason strings, no keystroke races).

4. **Allowlist-verify-before-run — and it must cover the ad-hoc periphery.** Go #1 verified the *planned*
   command shapes against the live allowlist before the run (the feasibility guard-audit, S1's "double
   relevance"), and those shapes held. What stalled the loop was `tail` — an **ad-hoc progress-check that
   was never in the plan** (S5). Lesson: pre-run verification of the planned matrix is necessary but not
   sufficient; the discipline must extend to every *improvised* command, which in practice means (a) the
   improvised repertoire must be enumerated and verified too (peek-at-file, check-git-state, check-disk),
   or (b) structurally, the whole loop is a **single bare-invoked orchestrator** whose internal `os.proc`
   calls are not Bash-gated at all — S3's pinned fix, which makes per-cell shell compounding impossible by
   construction. A pre-flight `tt guardcheck`-style checker (RT013/RT016/RT021 lineage) is the tool-shaped
   version of this discipline.

5. **Close the tool-coverage gaps so the disciplined path always exists (RT048 H3).** Two specimens were
   *forced* regressions: `tt web get` has no file output, so raw `curl -o` was the only way to save a PDF
   (S2); no `tt tail`/`tt cat` existed for peeking a growing file, and though the Read tool covered S5's
   case, the general peek-a-file-safely need is real (`substrate-regression-candidates` #5, the `tt cat`
   candidate). "You cannot will-avoid a gap the tool does not cover" (S2's own wording). Every closed gap
   removes a regression class outright, independent of any persuasion.

6. **Keep grants narrow; the agent proposes narrow, the human authorizes.** Shape class 5 shows the
   allowlist itself erodes under the same load pressure, on the human side: over-broad "don't ask again"
   offers (`mv *`, `git *`, `claude *`) accepted by a tired human would durably defeat the premise the
   whole structure rests on. The applied pattern (global destructive denies + narrow allows,
   `genscalator-self-dev.md`; the refused `mv *`, `broad-allowlist-aversion-2026-07-06.md`) is the
   human-side half of guard-free-by-construction: the *rule* must be provably safe, not just the intended
   call. Security changes stay human-approved ([[guard-against-forced-confirmations]], [[hardening-dance]]).

---

## (d) Open questions / what RT048 should grow into

RT048 asks: *which substrate content actually has power over enacted tool behaviour?* This synthesis
sharpens its agenda:

1. **Test H1 (structural >> persuasive) with the regression-rate DV.** The wr-data hygiene series is a
   natural regression log (six core specimens + the 2026-07-03/04 corpus). Compare regression rate
   (guard-trips + raw-tool uses per N commands) across substrate configurations: anti-regression header
   present/absent (H2), a tt gap open/closed (H3), a blocking guard vs a memory (H1). The S4/S6 pair is
   already a clean qualitative datapoint that *recall alone* scores near zero.
2. **Does the anti-regression header actually work?** It was added after S4 (2026-07-07). The next warps
   are the experiment: count post-warp first-N-commands regressions with the header loaded. If the header
   fails too, that strengthens H1 (only blocking structure steers) and demotes persuasive placement.
3. **Which raw habits are stickiest (H4)?** The taxonomy gives the priors: `cd`-compounds and `| head`/
   `| tail` bounding appear most often; `printf >` recurred within one session; the subagent reproduction
   (`harness-ux.md`) suggests these are model-level dispositions — connect to RT029 (cross-model reflex
   rate) and re-measure after model switches.
4. **tt-clock calibration (RT039 handshake).** What elapsed-threshold on which command classes separates
   "slow compile" from "guard-stall"? Needs per-call elapsed data (`tmp/tt-perf.tsv` exists for tt calls);
   the composition caveat (RT039: elapsed = reasoning + toolchain + human) must be honored or the signal
   misleads.
5. **Can pre-flight verification be a tool?** A `tt guardcheck <command>` that answers "would this prompt
   under the current allowlist?" would let the agent (or the orchestrator) verify improvised commands
   before emitting them — turning discipline 4 above from a practice into a mechanism. RT013/016/021 own
   the guard-tooling design space.
6. **Reflexivity.** The agent knowing its regressions are being counted may itself change behaviour
   (observer effect, flagged in RT048 §Method). Design the measurement so the observation channel is not a
   demand channel — the exact deadlock `instrument-deadlock-and-throughput-2026-07-06.md` documents for
   the human-as-instrument case.
7. **Adopt this taxonomy as the coding scheme.** Future hygiene specimens should be logged with their
   shape class (1–5), detection channel (self / human / guard-text / never), and cost band, so the
   regression log becomes analyzable data rather than prose — the same move RAW-DATA.md mining made for
   session data.

---

*Grounding note (echt): every event cited above is taken verbatim-in-substance from the named specimen
files; no event was invented or embellished. Where a specimen itself flags an inference (e.g. the S5 stall
is triangulated, not agent-witnessed — by definition), that caveat carries over here.*
