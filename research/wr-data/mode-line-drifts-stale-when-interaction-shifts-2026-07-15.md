# WR data: the declared-mode line drifts STALE when the interaction shifts gradually (2026-07-15)

**Type:** WR data — joint-state / status-line specimen. BR flagged it: *"we forgot to remove the afk solo modes
as we were chatting along."*
**Threads:** [[status-plus-mode-line-prototype-2026-07-13]], [[joint-escalation-statusline-codesign-2026-07-12]],
[[joint-rot-vigilance-recovery-kit]], [[cue-we-are-racing]].

## The specimen
The mode line (line 2 of the genscalator status line) is a **manually declared joint state**
(`token-spending & rot-vigilance & afk & solo`). BR began an AFK safe-solo run overnight, then over the morning
**popped in repeatedly and gradually became fully interactive** (co-designing the noop race, tooling, the
README figure, for hours). Nobody ever ran `tt mode rm afk` / `rm solo`, so the line kept declaring **afk +
solo** while the reality was **present + interactive**. BR noticed it (partly *because* the stale modes were
frozen into the status-line screenshot we had just committed into the public README). Corrected to
`token-spending & rot-vigilance & racing`.

## Why it drifts
The mode is a **declared** state, changed only by an explicit command. That is correct for a sharp transition
("go afk" flips it), but a **gradual** shift has no single trigger moment — BR sliding from AFK to interactive
happened over a dozen small pop-ins, none of which felt like "the moment to update the mode." So the manual
declaration lags the fluid reality. The same property that makes the mode line trustworthy (it says exactly
what was declared, not a guess) is what lets it go stale (a declaration nobody refreshed).

## The point / candidates
- **Shared vigilance miss:** the agent could have noticed too — BR clearly present and sending interactive
  turns for hours is prima facie "not afk, not solo." The agent should **proactively flag/propose the drop**
  ("you've been interactive a while — drop afk/solo?") rather than leave a stale declaration standing. Add to
  the joint-rot-vigilance kit: watch for mode-vs-behaviour contradiction.
- **BR's steer (2026-07-15), the operative rule:** in NORMAL operation the agent SHOULD actively **fix** an
  inferred-wrong mode, not merely nudge — BR trusts the agent to correct obvious mode-vs-behaviour drift (present
  + interactive for hours ⇒ clear afk/solo). The **exception** is a *deliberate pose*: setting modes for a demo
  screenshot ("**show-off mode**"), where the line is for display, not tracking reality — there the agent must
  NOT auto-correct. Elegant mechanism: make **"show-off" itself a declared mode that SUSPENDS
  mode-auto-correction** — a mode that governs mode-maintenance. So the rule is: **fix drift by default; back off
  when a "hands-off" mode (show-off) is declared.** (This very session hit both halves: the agent left afk/solo
  stale for hours = should-have-fixed; then BR deliberately re-posed afk/solo for a daytime re-shoot = must-not-
  touch. The "show-off" mode cleanly disambiguates the two.)
- **Point-in-time capture caveat:** a status-line screenshot freezes the modes at snapshot time — fine as a
  demo (the README figure's `afk & solo` were true at 02:25 during the solo run), but a reminder that the line
  is a live, maintenance-requiring surface, not a static fact.
