# A stale entitlement cache told the user to pay, and the remedy existed only in the web UI (2026-07-20)

**One line:** the CLI told the human his model required payment, silently changed which model served him,
and let him save that change as a durable default — while the truth ("still included; restart") was
reachable only in a *different product surface* he had no reason to open.

**Provenance.** Observed live in a working session, 2026-07-19 evening → 2026-07-20 morning, clock-read
stamps. The web-settings wording below is the human's verbatim report (the agent cannot see that surface);
everything else is from the session transcript, fetched docs, and `/status` output pasted into the feed.

## Timeline

| when (Europe/Stockholm) | what happened |
|---|---|
| 07-19 ~23:02 | status line shows model `f5·1M`, `wk 42%` — Fable 5 serving, weekly budget fine |
| 07-19 23:41 | long solo work run ends; everything committed and pushed |
| 07-20 **08:59:59** | **the Fable 5 promotional period ends** (documented cutoff: 07-19 23:59:59 PT) |
| 07-20 ~09-10:00 | human returns, notices **the model is now Opus 4.8**, which he did not choose |
| 07-20 ~10:00 | a message about needing to pay appears and **vanishes before it can be read** |
| 07-20 ~10:0x | `/model Fable5` → *"Model 'Fable5' not found"* (alias is lowercase `fable`) |
| 07-20 ~10:0x | picker says **Fable 5 "requires credits"**; human selects Opus, which is **"saved as your default for new sessions"** |
| 07-20 ~10:1x | web settings → Usage: *"Fable 5 is still included in your Max plan. If Claude Code asked you to set up usage credits for Fable 5, **restart Claude Code**."* |

`/status` confirms `Login method: Claude Max account`, and the docs confirm Max keeps Fable 5 included
after the promo. **So the CLI's claim was false, and the client knew how to be right: restart.**

## The defect chain (each link independently fixable)

1. **A payment prompt that cannot be re-read.** It appeared and disappeared. A message about *money* is
   exactly the class that must be dismissible-with-a-record, not transient.
2. **A silent capability change.** Which model serves is a first-order fact about cost, latency and
   quality. It changed without the human acting, and the only witness was a status-line tag he happens to
   run — i.e. *his own* instrument, not the vendor's.
3. **A false statement presented with full confidence.** "Requires credits" was simply not true for this
   account. The client had stale state and rendered it as fact, with no hedge and no "last synced" stamp.
4. **The remedy was in another product.** The CLI knew the failure mode well enough that the *web* team
   wrote a targeted sentence about it — "if Claude Code asked you..., restart Claude Code". That sentence
   proves the vendor anticipated this exact confusion. **The one place it does not appear is the place
   where the confusion happens.** An error that has a known one-word fix should carry the fix.
5. **A transient falsehood was allowed to write durable config.** Acting on the false prompt, the human
   set Opus as the **default for new sessions**. The stale cache is fixed by a restart; the default it
   induced is not. *A state that a restart can invalidate must not be able to persist a preference.*

## The so-what (design moves, not just grievance)

- **An error message must carry its own remedy.** If the product knows the fix well enough to document it
  elsewhere, it knows it well enough to print it. Cheap, and it collapses this whole episode to zero.
- **Entitlement is a claim about the world; render it with provenance.** "Requires credits (as of last
  sync HH:MM — refresh)" is honest; a bare assertion from a cache is the same failure genscalator hunts in
  its own substrate: *a claim whose freshness is invisible*. Compare the stale status-line finding
  (SM172): a measured line wearing a live face is quiet disinformation. Same shape, vendor-side.
- **Never let transient state write persistent preference** without a visible, one-click undo.
- **The user's own instrumentation was the only honest witness.** The model tag and `wk` gauge — a
  genscalator status line, not a vendor surface — are what made the change *noticeable at all*. This is
  the ambient-awareness argument landing on the vendor's own product, and it is the strongest single
  datapoint yet for why the mode/status lines exist.

## The sovereignty point (the human's, and it is the sharp one)

> *"it shows how much we are in the hands of Anthropic"*

Capability here was gated by a **cache inside a client we do not control**, flipped by a **promotional
calendar we do not set**, in a **timezone that is not ours** (the cutoff landed at 08:59:59 local — an
hour before the human sat down, which is precisely why it looked like a random fault). Nothing about the
work changed; the machine, the repo and the toolbox were identical before and after. This is the concrete,
lived form of the PRD's `sovereigntyOfCapability` goal: **the parts of the workflow we own kept working;
the part the vendor owns changed under us without notice.** The mitigation is not indignation, it is what
the project already does — own the substrate, own the tools, own the awareness instruments, and keep the
vendor-dependent surface small and *observable*.

## Agent-side specimens from the same hour (own failures, logged per keep-the-ball-game)

- **Confabulated clock.** A date-rollover notice gave the agent a new *date*; it invented an *hour* from
  it ("past midnight"), wished the human good night at 10:00, and — worse — built a *causal* argument on
  the invented hour, concluding the PT cutoff "hasn't passed yet" when it had passed an hour earlier.
  Fixed only when the human said what time it was. **No felt time: re-read the clock at every boundary.**
- **Two confident wrong causes, in sequence.** (a) "It's the safety-classifier fallback" — plausible,
  documented, wrong. (b) "You've spent the 50%-of-weekly Fable 5 slice" — plausible, arithmetically
  consistent with the observed `wk 42%` plus a heavy run, wrong. The true cause was a stale cache, which
  the agent never hypothesized because it reasoned from *documentation* (what the rules say) rather than
  from *implementation* (what a client actually does). **A rules-shaped model of the world cannot predict
  a bug.**
- **A premise invented, then laundered through a subagent.** The agent told a docs-fetching subagent "he
  is on Max" without knowing it; the subagent built its verdict on that and returned "this is a bug or an
  entitlement sync failure — contact support." The agent then correctly discounted the verdict for resting
  on an unverified premise. **The verdict was right.** Two lessons, not one: never feed a subagent an
  unverified premise as fact, *and* a conclusion discounted for bad provenance is not thereby false —
  re-check it once the premise is grounded, instead of dropping it.

## Practical residue for this account

Restart Claude Code, re-select `fable` (lowercase alias; `Fable5` is not a valid id), and **unstick the
saved default** that the false prompt induced.

## Ties
`sovereigntyOfCapability` (PRD) · SM172 (stale line wearing a live face — the same defect, vendor-side) ·
blog 004 (*why Claude UX sometimes sucks*) — this is a candidate episode for it · the awareness-lines
argument (status line as the only honest witness) · SM164 (the model question stays *measured*, not
assumed).
