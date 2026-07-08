# WR data: agent's real-time introspection of SUSPECTED growing context rot (2026-07-08, long-session tail)

BR cue: *"WR data on the introspection of suspicion of growing context rot."* This is CO4 self-reporting its own
suspected rot AS it happened — the live **L0 self-report layer** of the contextRotMeter (PRD), captured on
purpose. Echt discipline applies hardest here (self-generated, corroboration-asymmetric) — so it is tiered by how
checkable each signal is, and it actively guards against over-attribution.

## The context
A single, extraordinarily long and dense session: dozens of commits across three repos, several large reads (the
full blog 002, the 348-line template, PB sections re-read repeatedly), a rapid-fire BR pin burst at the tail. No
`/context` / token-fill read available to me (the SM016 blind spot) — so every signal below is **FEEL, not
measurement.**

## The signals, tiered by strength
**STRONG (behavioral, hard to explain away):**
- I **proposed a compact unprompted** — a felt need for a fresh window, before any external prompt. FEEL-based,
  but a real self-generated signal.
- Minutes later I **enacted the forbidden shell-blob pattern** (heredoc → `--message-file /dev/stdin`, instance 5)
  — a rule that is in my reloaded anti-regression header AND that I had obeyed correctly *dozens of times the
  same session*. Enacting a freshly-recalled forbidden rule anyway is the **signature of rot** ("recalled ≠
  enacted"), and it landed **right after** I flagged the rot. So the FEEL-suspicion got **behavioral
  corroboration** within minutes — a rare case where the soft signal was hard-confirmed.

**WEAK (ordinary bugs; suggestive only by clustering near the tail):**
- a naming slip (`inline` collided with the Scala soft keyword), a placeholder-delimiter bug (used spaces, had to
  fix), a mis-anchored Edit (assumed a test description had changed when it hadn't). Each is a normal dev error
  that happens at ANY context length; only their **clustering** at the session tail is weakly suggestive.

**UNMEASURABLE:** the actual context fill / true rot magnitude — I cannot read it, so I cannot separate "genuinely
degraded" from "long session, normal error rate." That gap is the whole SM016 motivation.

## The echt guard (important)
Once I suspect rot, **confirmation bias** makes every slip look like evidence — so I deliberately down-weight the
WEAK signals. The honest claim is narrow: the **STRONG** pair (unprompted compact-urge + a forbidden-rule slip
immediately after) is genuine evidence of degradation; the rest is noise-or-rot, unresolvable from the inside. The
value is not "I measured my rot" (I can't) but "a FEEL-based rot-suspicion produced a **checkable prediction**
(I'm about to slip) that then **came true**" — which is modest evidence that agent rot-FEEL is a real detector,
not just anxiety.

## Why it matters
- **Live L0 data for the contextRotMeter** (PRD `contextRotMeter`): a worked example of the "count degradation
  signatures + self-report" layer, produced in-situ.
- **RT052 / joint-rot-vigilance:** the human (BR) prompting the agent to record its own suspected state IS the L2
  human-anchor working — the undegraded observer eliciting the degrading one's self-report. And it confirms the
  RT052 lesson: I should have acted on my own compact proposal instead of taking "one more pin" (each raised slip
  odds; instance 5 is the receipt).
- **The measurement gap is the product spec:** because I can only FEEL this, not read it, an instrument that
  surfaces fill + regression-rate (SM016 tap → SM022 dashboard) would convert this anecdote into a signal we can
  act on *before* the slip, not diagnose after. This note is the demand, written from inside the failure.

## Z-calibration datapoint (the measurement, supplied after the fact)
Append-only addendum (NOT a retro-edit of "UNMEASURABLE" above — that was true when written; this is new data). After
I flagged the rot and proposed the compact, **BR ran `/context` and shared it: 503.3k / 1M = 50% raw fill.** This is
the **L2 human-anchor supplying the measurement the L0 agent lacked** — the undegraded observer reading the gauge the
degrading one can't, closing the loop the SM016 blind spot describes.

Interpretation (echt, N=1):
- The rot-feel + the instance-5 forbidden-rule slip fired at **~50% of a 1M window** — i.e. effective-Z for reliable
  **guardrail-discipline** looks well below the raw window (roughly the first half here, **not** the 0.8·window compact
  trigger we'd been assuming from smaller windows).
- **Caveats, held honestly:** one datapoint. It may track **absolute context mass + edit-density** (dozens of commits,
  several large reads, a pin burst) rather than the **fraction** of the window — a 1M window at 50% is still ~500k of
  absolute mass, more than a whole smaller window. And **discipline-Z is plausibly < reasoning-Z**: the shell-blob
  guardrail is a low-salience procedural rule competing with high-salience task momentum, so it may degrade earlier
  than the reasoning that ships correct code (which stayed fine at the same fill).
- Feeds RT001 (rot~fatigue), RT052, SM016 (the tap that would surface this live), and the PRD `contextRotMeter` — this
  hand-measured 50%-fill calibration is exactly what the meter would automate.

## Disposition
Sibling of `shell-blob-fallback-regression-2026-07-07.md` (instance 5 is the behavioral half of this). Feeds
RT001 (rot~fatigue), RT052, SM016/SM022, and the PRD contextRotMeter. [[joint-rot-vigilance-recovery-kit]]
[[echt-effort-especially-self-generated]]
