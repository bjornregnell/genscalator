# Warp-regression specimen: raw curl from the super-agent, same evening it briefed the rule

2026-07-21 ~20:3x (BR guard-caught at the modly LAN check; logged immediately after).

## What happened

1. Cold-started session (first lean-ember warp, ~19:46) investigating modly-down on bjornyx.
2. Agent ran a LOCAL `curl -s http://bjornyx.local:8080/health` to verify LAN reachability.
   The guard prompted; BR allowed from the TUI and called it: "should be tt web (is this a
   warp regression?)".
3. Verdict: YES, a warp regression — `tt web get <url>` existed, is the documented shape
   (the codeberg-badge check memory uses it), and re-running with it worked first try.

## Why this specimen is interesting

- **Carried ≠ armed, sharpest form yet:** the same agent had pasted the EXACT rule
  ("URLs: `tt web get <url>` — never raw curl/wget") VERBATIM into two sub-agent briefs
  EARLIER THE SAME EVENING (push-17, and the EMBER-for-sub-agents assembly). The rule was
  in context as *briefing material for others* but did not arm as the agent's OWN reflex.
  Same family as push-14's finding that a corrected reflex does not stay armed in-session.
- **Root cause is structural, not volitional:** the main-session guard-clean digest had
  SEARCH/FILES, SHELL HYGIENE and GIT blocks but NO URLS block — the sub-agent delta rules
  carry the URL rule precisely because the digest does not. The cold-start warm-up therefore
  never re-hydrated it. Fixed this unit: URLS block added to `docs/guard-clean-digest.txt`.
- **The guard worked as designed:** the allowlist gap turned the regression into a prompt
  instead of a silent habit — the structural backstop caught what salience missed.
- **Gray-zone siblings, deliberately NOT counted as specimens:** the same investigation ran
  `ssh bjornyx.local curl -s http://localhost:8080/health` (and similar) — REMOTE curls on a
  host that has no tt installed. No typed shape exists for remote-host checks (candidate:
  a `tt box remote` / SM160-family verb); noted in the digest's new URLS block as a flagged
  gap rather than an improvised rule.

## Pattern link

Push-17's residue-class observation was "compression loses qualifiers before facts"; this
adds the transmission analog: a rule can be TRANSMITTED intact (verbatim briefs) while not
being INSTALLED in the transmitter. Briefing others is not self-warming. Candidate reflex:
when assembling a sub-agent brief, the assembler re-reads the delta rules AS ITS OWN
checklist once (cheap, and it would have caught this).
