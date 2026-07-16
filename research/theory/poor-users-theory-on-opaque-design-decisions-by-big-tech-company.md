# The "poor users" theory of opaque design decisions by big tech — a deliberately provocative, ironic seed

> **Register: PROVOCATIVE / IRONIC, NOT echt.** BR set this up on purpose (2026-07-16): the *title* theory is a
> tongue-in-cheek lament, and the note's job is to **puncture it** — because the opacity it complains about is often
> a legitimate need (security-by-obscurity, control-mechanism integrity). Keep the irony visible; do **not** let the
> provocative surface read as a genuine grievance claim. This is a theory-building **seed** to develop, not a finding.

## The provocative theory (the straw this note will partly burn)

Big tech companies make **opaque design decisions** — choices whose rationale is never explained to the people, and
now the *agents*, affected. Users are left in the dark, powerless, *poor users*, at the mercy of inscrutable megacorp
choices. Immediate specimen: **Claude Code withholds the guard-stall event from the agent** — the agent is blacked
out and never told (see [[blackout-hangover-and-why-the-platform-withholds-the-stall-2026-07-16]]). Framed this way it
sounds user-hostile: why keep the agent — and the user watching it — uninformed?

## The echt correction (why the theory is ironic)

Because **the opacity is frequently the mechanism, not a slight.** A control you can *see* is a control you can
*game*. Exposing the stall's timing/cause to the agent is exactly what would let a compromised or merely
optimising agent route around the human-in-the-loop gate. That is **security-by-obscurity in its legitimate
register** — not the derided "hide the crypto and hope," but *"do not hand the constrained party the schematics of
its own constraint."* Least-privilege, need-to-know, and control integrity are real engineering needs. So the "poor
users" lament partly **inverts**: the opacity users would lament is often what protects the integrity of a control
that serves *them* (or the system they depend on).

## The synthesis (the actual research question worth building)

Opaque big-tech design decisions are **not one thing**; they sit on a spectrum, and the useful work is telling the
ends apart:

- **Legitimately-necessary opacity** — security / safety controls whose visibility would let them be gamed (the
  stall event; anti-abuse thresholds; fraud heuristics). Opacity serves the *user's* (or system's) security.
- **User-hostile obscurantism** — opacity that serves the *vendor's* leverage: lock-in, un-portability, undocumented
  deprecations, no update-awareness, dark patterns. Opacity serves *the vendor*.
- **The test (genscalator's sovereignty lens): *whose interest does the opacity serve?*** Protects a control the
  user depends on → legitimate. Protects vendor leverage over the user → the sovereignty gap genscalator exists to
  fill. The **same company does both at once**: Anthropic's stall-opacity looks *legitimate* (control integrity),
  while (e.g.) no plugin update-check API and no portability look like the *leverage* kind — which is exactly why
  genscalator builds its own (`tt update`, the MCP-server goal).

## Why it matters / how to develop it

- It **sharpens the sovereignty thesis.** "Reclaim capability the platform under-serves" needs the
  legitimate-vs-leverage distinction, or it slides into "reclaim everything," including security controls we should
  *not* erode — the **knowing-to-RECOVER vs knowing-to-EVADE** line from the blackout note is the same cut.
- **Develop:** collect specimens (each opaque decision we hit), classify by the whose-interest test, and check
  whether the legitimate/leverage split predicts which ones genscalator should *route around* vs *respect*.
- **Ethics (engineering-research posture):** name the irony honestly — no cheap "big tech bad" shot. The
  contribution is the **discriminating criterion**, not the grievance. (COI note: genscalator/BR is a party with a
  sovereignty stake *and* a settlement relationship with Anthropic — disclose when this is written up publicly.)

**Ties:** [[blackout-hangover-and-why-the-platform-withholds-the-stall-2026-07-16]], SM096 (gating erodes),
SM097 (sovereignty), the `tt update` / update-awareness work, the MCP-server goal, blog 026 (survive Anthropic's tools).
