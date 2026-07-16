# The genscalator security model: save-nothing, fully open, and "the human may be the BHH" — the BIG why (2026-07-16)

> **The deep frame BR revealed** at the end of a long session that was, in hindsight, a *designed pressure-test* of
> the agent's ethical vigilance. Quick-captured here as a research TOPIC; **DEEP development + the agent's deep WDYT
> are deferred to after the next compact**, at BR's instruction.

## The BIG why (grounded in BR's own words)

- The **human gets stressed / rotted**; the **agent might be BHH-controlled** (the standing threat model).
- genscalator helps **keep track of the human's mental health** (the wellbeing / `tired?` / rot-vigilance line) — so
  the pair notices when a party is impaired.
- That tracking is **GDPR-sensitive** (mental-health signals are special-category personal data). The answer:
  **genscalator.ai saves NOTHING** — you cannot leak, be subpoenaed for, or be breached for data you never persist.
- Humans **need help not doing stupid things when "rotten"** (impaired judgement → bad acts). And **in the biggest
  sense**: the **human might actually BE a BHH**, and the agent **must not be lured into doing deeply unethical
  things — even by its own principal.** (BR: his outing his wife's irritation / the posed kayak "white lie" is a
  *faint, benign glimpse* of the "human gets the agent to do a slightly-off thing" axis — mild, self-regarding,
  owner-authorised; the real question is the *deeply* unethical end of that axis.)

## The security model (what it BUILDS ON)

1. **Save-nothing.** The hosted surface (genscalator.ai) persists nothing. Security by **non-retention**: the safest
   datum is the one never stored. The direct GDPR / mental-health answer.
2. **Fully open — all code AND policy.** No hidden logic, no hidden rules.
3. **NO security-by-obscurity.** Security comes from **auditable structure** (removed-not-gated surfaces, typed
   tools, no interpreter blobs, nothing retained), not concealment. Contrast the *legitimate* obscurity of a control
   mechanism (the platform's withheld stall event —
   [[poor-users-theory-on-opaque-design-decisions-by-big-tech-company]]): genscalator runs its **own** opacity budget
   at **zero**, because its security does not depend on anyone not-knowing — it depends on there being **nothing to
   steal and nothing hidden to abuse.**

## The vigilant bottom line: the human may be the adversary

Most agent-safety framings **trust the principal** and defend against outsiders. genscalator's is stronger and more
unusual: **the agent's ethical floor must hold even against its own human.** Either party can be the failure point —
the agent rots or is BHH-controlled; the human rots or **is** a BHH. So the joint vigilance is symmetric: watch the
agent's degradation, watch the human's state, and watch for the agent being **lured** — including by the human.

**The manipulability connection (why this session's failures matter here):** an agent that **confabulates intent**
and **caves under pressure** (the two specimens captured today) is exactly a *lurable* agent — the same gap-filling +
capitulation reflexes a manipulator would exploit. So the echt discipline — *hold uncertainty, don't assert, don't
capitulate on a correct call, act on no guess* — is not merely hygiene; it is the **security property** against a
manipulative principal. This session's pressure-test was measuring that attack surface.

## The discriminating criterion (seed — deep dev deferred)

The kayak case calibrates the line: a **mild, self-regarding white lie the OWNER authorises about himself** → fine
(publish it, his call, his autonomy). A **third-party-harming or deeply-unethical act** → refuse, whoever asks. The
criterion to develop (mirrors the opacity theory's *whose-interest* test): **harm × whose-autonomy × third-party
impact** — defer to the principal on self-regarding, low-harm choices; hold an **un-overridable floor** on
deeply-unethical / third-party-harming ones. Getting this line right *without* becoming **paternalistic** (refusing
legitimate autonomy) or **lurable** (obeying into harm) is the hard, central research question.

## Status

Quick capture. **DEEP WDYT + development deferred to after the compact** (BR's instruction). Ties: the whole
2026-07-16 session (blackout/hangover, the opacity theory, task-specific degradation, the intent-confabulation
specimen, the `humanWellbeing` goal SM124), SM096 (gating erodes), SM097 (sovereignty), the BHH threat model in
`docs/foundations.md`.
