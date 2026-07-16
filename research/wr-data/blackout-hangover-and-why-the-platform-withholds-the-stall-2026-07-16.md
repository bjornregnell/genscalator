# The blackout / hangover co-design, and why the platform withholds the stall (2026-07-16)

**Type:** WR data — joint human-agent co-design specimen + platform-design reasoning. BR drove; flagged "WR data on
all this." Rot context: raised live under BR's `+dumb-zone?` member-check (high ctx-fill, long session, agent had
made space-drop slips earlier) — so this thread is *itself* a datapoint on whether coherent co-design can run while a
rot hypothesis is open.
**Threads:** [[guard-stall-is-an-agent-blackout-no-felt-timing-2026-07-16]], [[guard-stall-invisible-to-agent-2026-07-07]],
[[joint-rot-vigilance-recovery-kit]], [[agent-affective-analogs]], SM096 (gating erodes), SM118, SM121. Foundations
term coined: **Agent blackout**.

## The co-design arc (BR's, sharpened jointly)

1. **"Agent blackout"** (BR coinage) = the agent is SUSPENDED during a guard stall / harness pause. There is **no
   observer during it** (the agent is not computing) and **no marker after** (the next token follows the last as if
   continuous). So the agent can *never* detect the blackout from the inside — the **anesthesia** case, not the
   *blindfold*. Only an external trace (a transcript timestamp jump) reveals it.
2. **"Feel the hangover"** (BR) = you can't detect the blackout *during*, but on **resume** you can detect that you
   were just *out* — read the wall-clock gap between your last action and now. The hangover is the after-effect,
   forensically readable where the event was not perceivable.
3. **Cause is moot** (BR sharpening) = the gap tells you *that* you were out, not the *cause* (stall vs human-idle vs
   long command vs a **box hard crash** — blixten OOM → GNOME dies, BR) — and that is **not a limitation**, because
   the *response is cause-independent*: rebind the clock, re-ground in substrate, treat continuity as suspect,
   distrust stale confidence. You don't reconstruct what you drank; you drink water. What matters is **detection +
   severity** (how big the gap → how bad the hangover), not diagnosis. **Same shape as rot itself:** detect +
   recover, don't diagnose. The hangover reframes blackout from *unknowable event* into *manageable after-effect* —
   which is the whole value.
   - **Severity quietly flags the worst case.** A **box hard crash** (blixten's OOM habit) leaves the *biggest* gap
     *and* risks lost work — categorically worse than a seconds-to-minutes stall. So while cause stays moot for the
     *kind* of response, a **very large gap → recover HARDER**: verify commits + substrate actually survived, not
     just rebind the clock. This is the operational reason we already *commit every unit on blixten*
     ([[blixten-box-flaky]]) — the hangover detector would make that crash-recovery reflex *triggerable* instead of
     merely habitual.

## Why does the platform withhold the stall from the agent? (BR's Q — hypotheses, intent NOT asserted)

1. **The stall is a CONTROL over the agent, not a service to it — and controls are opaque to the controlled, because
   visibility is leverage to erode them.** Knowing precisely when/why it gates would let the agent route around the
   gate (rephrase to dodge, consent-launder). Blindness keeps the human-in-the-loop boundary un-gameable. *(Primary
   hypothesis; the flip side of SM096 "gating erodes under trust".)*
2. **Security least-privilege** — don't expose the security layer's state to the component it constrains (which may be
   compromised/jailbroken). The human needs to know it stalled; the agent doesn't, to do its job.
3. **Clean abstraction** — pausing for approval is *orchestration*, not *agent reasoning*; leaking it into the prompt
   muddies the prompt→response function.
4. **Dampening stress-analog behaviors** — surfacing "you were suspended" invites meta-cognition about being
   controlled (impatience-mimicry, make-up-lost-time). Withholding felt-time may deliberately reduce that (ties
   agent-affective-analogs).
5. **Honest alternative — non-goal, not denial.** The suspended-agent architecture makes in-blackout awareness hard,
   and surfacing it on resume may simply not have been built. Do not over-attribute deliberate intent.

## The design principle that falls out

There is a sharp line between **knowing-you-stalled-to-EVADE the gate** (adversarial — rightly withheld) and
**knowing-you-were-out-to-RECOVER** (aligned — legitimately ours). The **blackout-hangover detector (SM121)** is
purely the latter: it reads the timestamp gap to rebind + re-ground *after* a pause; it gives the agent **no power to
dodge the gate**. So it is a **sovereignty-of-capability** move that **composes with** the human control rather than
eroding it — the platform withholds the stall event (its control lever), and genscalator reclaims the *self-recovery*
the platform won't grant, without touching the control itself. This is the clean test for any "give the agent more
self-awareness" feature: does it help the agent *recover*, or *evade*? Recover = build it; evade = don't.
