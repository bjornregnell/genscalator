# RT050 — Does delegating to subagents lower context rot? (empirical study design)

**Status:** open, seeded 2026-07-07 (BR: "can we do an empirical study to investigate the lower-context-rot
hypothesis?"). Child of RT049 (the delegation dilemma, RQ3 = the rot ledger); leans on RT001 (context rot
resembles fatigue) for the construct, and RT048 / the WR command-hygiene synthesis for a ready-made rot proxy.

## The hypothesis
**H (rot reduction, both sides):** delegating bounded work to fresh subagents lowers context rot compared to one
context doing all the work, because (a) the delegated work **never enters the super-agent's context** (it stays
lean, its warm window lasts longer), and (b) each subagent **starts fresh** (no accumulated rot). The system as a
whole ages more slowly than a single overloaded context would.

## Operationalizing "context rot" (the hard part, inherited from RT001)
Rot = performance degradation as usable context fills. Candidate measures, strongest first:
1. **Quality/fidelity of task unit k as a function of position k** in a long run (does late-unit quality decline?).
   Blind-scored, 047 style-rater discipline.
2. **Regression rate** — guard-trips + raw-tool uses per N commands (the DV proposed in the WR
   `SYNTHESIS-structure-over-willpower` doc). A behavioural, cheap, already-logged rot proxy.
3. **Context-fill trajectory** (`/context` token fraction over time) — the independent variable / exposure, not
   the outcome, but needed to relate rot to fill.
4. Self-report vitality checks (weakest tier; corroboration asymmetry, use only as triangulation).

## Design (two arms, same total work)
- **Arm A — solo:** the super-agent does all N bounded units itself in one context. The context fills across the
  run. Measure the rot proxies as a function of unit position and final fill%.
- **Arm B — delegated:** the super-agent hands each unit to a **fresh subagent**, only briefs + adjudicates, and
  keeps its own context lean. Measure (i) the super-agent's fill trajectory + late-run quality, and (ii) each
  subagent's unit quality.
- **Prediction:** in Arm A, late-unit quality drops and regression rate rises with fill; in Arm B, the
  super-agent's fill stays low and its late-run quality holds, and subagent quality shows no position effect
  (each is fresh). If Arm B quality is *not* worse overall, delegation bought rot-reduction at no quality cost.

## Confounds to control (or the study measures the wrong thing)
- **Briefing fidelity (the big one, RT049 RQ2):** Arm B quality also depends on how well the super-agent briefed
  the subagent. To isolate *rot* from *briefing*, use tasks that are **context-light and highly verifiable** (so
  briefing is near-perfect and near-costless) — then any Arm-A degradation is rot, not briefing loss.
- **Model tier:** subagents are a different (cheaper) model. Either hold the subagent = same model as the
  super-agent, or treat tier as a second factor (ties Factor A of 047).
- **Task heterogeneity / order:** randomize or counterbalance unit order; matched task sets across arms.
- **The compact confound (in reflexive data):** a `/compact` also resets fill, so "supervisor stayed lean" after
  a compact is not by itself evidence of delegation-driven leanness. Separate the two.

## Reflexive datapoint already in hand (suggestive, confounded)
This very session: after `/compact` the supervisor (CO4) sat at ~5% context while four review subagents + a WR
synthesis + an AT grind did the heavy lifting; pre-compact it was ~52%. Direction consistent with H, but
confounded by the compact itself and n=1. A specimen, not evidence.

## Threats / limits
- Rot is genuinely hard to measure cleanly (RT001's own caveats carry over); the study is only as good as the
  proxy.
- **Observer/reflexivity:** the agent knowing rot is being measured may change behaviour (same deadlock RT048
  flags).
- n and cost: a proper position-effect curve needs many units per arm.

## Feeds
- Blog **BP013** (`013-the-dilemma-of-delegation.md`), the "but can we measure it?" beat.
- Back-links RT049 RQ3 (the rot ledger) and the regression-rate DV in the WR synthesis.
