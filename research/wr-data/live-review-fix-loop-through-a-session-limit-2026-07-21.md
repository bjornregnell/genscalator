# Live review-fix loop: human rendered-eyeball + agent cache-surgery, through a session limit

2026-07-21 evening (~20:55-22:00). Context: introprog v2026.5 release candidate; BR read
the freshly built compendium-en.pdf and threw findings into the feed as he went; the agent
located root causes and fixed them live. WR-relevant observations, enumerated:

1. **The loop shape worked**: human eyeballs RENDERED output (the only surface where some
   defect classes exist), agent traces each finding to substrate (cache row, source line,
   post-pass, stale artifact) and fixes at the root, verifies in the regenerated mirror,
   commits per unit. 22 findings in ~65 min; 14 fixed-and-verified same session, the rest
   located-or-scoped with recovery routes recorded in a committed review doc.
2. **Second session-limit hit of the day (~21:15:16, reset ~15 min)**: BR KEPT REVIEWING
   during the stall and queued 8 findings as one batch message; the queue survived the
   stall intact and processing resumed cleanly post-reset. Datum for the cap-stall dance
   rule: a stall need not stop the HUMAN's half of a joint loop; the feed is a buffer.
3. **Defect classes found by the rendered eyeball that the corpus gauge cannot see**:
   surface gaps (the plan/ table, quiz answer letters, stale never-rebuilt decks),
   masked-token space migrations (should__C1__ __C2__not), semantic inversions with
   perfect grammar ("of type different", answer letter O -> "Zero", moles -> "trolls",
   villebråd -> "weapon"). The last class is the strongest argument for human review of
   MT output: every one was fluent English.
4. **Guard-designed friction observed**: a cache row set to sv==sv identity (to KEEP
   intentional Swedish) was auto-dropped by the pipeline's own staleness guard and
   re-translated - the right long-term fix is a keep-verbatim mechanism, not a cache hack;
   escalated to BR-present work rather than improvised solo (AFK discipline held).
5. **Tool-shape note**: the entire loop ran on bare tt text match/grepr + Edit + one
   9-second deterministic regen per batch - no model calls beyond the known benign unit,
   modly serving throughout (restarted earlier the same evening after being found down).
