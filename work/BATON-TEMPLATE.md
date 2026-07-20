# BATON-TEMPLATE: the pre-hoc seed

**Status: pre-hoc seed, v0 (2026-07-19); v0.1 maintenance 2026-07-20** — operational updates from
live guard-stall specimens plus the ColdStart-clearing revert. Still PRE-EXPERIMENT: no pilot has
run; these refinements move the thing-to-beat, they are not results. This is the best-*reasoned* baton template from the SM168
joint pass - written down BEFORE any measurement, as the thing for the pilot experiment to beat (see
blog post [030](../blog/030-what-is-a-good-warp-baton.md)). It is a seed, not a result: the pilot
(baton vs no baton) and the follow-up experiments (per-ingredient) may change any part of it. The
real specimen it generalizes: [BATON-EXAMPLE.md](BATON-EXAMPLE.md).

## The rules the template encodes

1. **Pointer, not truth.** Durable truth lives in committed files and notes; the baton points. The
   only prose state allowed is the three-line summary of §4, and each of its lines must say how to
   verify itself. *(This pointer said §5 until minion push-4 F3 caught the off-by-one.)*
2. **Verify-mandate rule.** Every inherited claim the fresh agent will ACT on carries its own
   check-instruction (a hash to compare, a command to run, a file to read). A fresh agent holds all
   inherited facts at uniform strength; the mandates substitute for the lost knowing-which-was-checked.
3. **Numbers over adjectives.** Commit hashes, counts, percentages - checkable against git and disk.
4. **One screen.** Size is a duplication budget, not a token budget: if a section grows, relocate
   content to the substrate and point.
5. **Exit order.** Commit and push ALL work first, then write the baton, then exit. A baton written
   before the final commits points at a moving target.
6. **English**, even if the working chat is not: the reader may be any future agent.

## The skeleton (section order is part of the design)

```markdown
⛔ THIS IS A BATON: a pointer to durable truth, not the truth. Verify before
trusting; read only what you need (bounded reads).

# Baton — <warp type: cold start | compact | model switch | reboot>, written <real timestamp> by <writer>

## 0. Anti-regression checklist — READ FIRST (reflexes regress at turn zero)
FORBIDDEN → ALLOWED:
- <raw habit the base model will reach for> → <the pair's tool/shape that replaces it>
- ... (one line per reflex; keep the full list — this section has priority over all others
  because it must act before the agent's first tool call)
- If a needed tool shape does not exist: FLAG THE GAP, do not improvise the raw shape.
- BEFORE flagging a gap, check whether a guard-free shape already exists in the substrate
  (specimen 2026-07-20: the test suite's absdir+java-prop form was documented inside the
  test file all along, while an env-chdir improvisation stalled the human three times).
- A freshly built executable or never-before-run command shape is UNVETTED: stall-free
  history does not transfer to it; smoke it human-present or park it (specimen 2026-07-20:
  new native-binary smokes stalled minutes after an "everything left is stall-free" claim).
- No regex alternation `(a|b)` in inline shell patterns — it reads as regex but executes as
  shell; run two plain single-pattern greps instead (two live slips 2026-07-20).

## 1. Warp declaration (mode line)
- cold start: declare `-RotVigil +ColdStart +SmartZone`
  (fresh = un-rotted but un-calibrated and demonstrably at low fill; ColdStart clearing is
  the HUMAN's call — the hygiene-done self-clear stance was tried and reverted 2026-07-20;
  the clear-condition is an open SM118 question)
- compact survivor instead: consider `+Hangover`; distrust what you think you remember —
  the verify-mandates below apply DOUBLE to you.

## 2. Holds — do NOT start these
- <held work item> — WHY it is held; WHO lifts it and on what condition.
- ... (a hold without an owner+condition is a defect)

## 3. Pre-authorized actions (do at cold start, report in-feed)
- <action> — pre-authorized by <human>, verify by <check>.
- IF the budget mode says spending (TokSpend lit): spawn the meta-minion to audit the handover —
  the last session's final commits against this baton's claims. It is the one verifier outside
  both the dying and the newborn context ("carried ≠ armed": baton content must be read to act,
  a spawned watcher fires regardless). If not in spending mode: ask before spawning.
- ...

## 4. State summary — MAX THREE LINES, each with its verify-mandate
- <claim of state> (verify: <command/hash/file>)
- <claim of state> (verify: <command/hash/file>)
- <claim of state> (verify: <command/hash/file>)

## 5. Substrate map (priority order)
<board/roadmap file> · <memory index> · <research/data dir> · <this file's own queue section if any>
```

## Pre-flight checklist (mechanical; a future baton-prep skill can enforce it)

Before exiting, the writer checks:
- [ ] every path in the baton resolves on disk;
- [ ] every commit hash named exists in the repo it is claimed in;
- [ ] every hold names its owner and release condition;
- [ ] every state line and pre-authorized action carries a verify-mandate;
- [ ] all work is committed AND pushed (both remotes if the primary flaps);
- [ ] the file fits on roughly one screen;
- [ ] timestamps are read from a clock, never guessed;
- [ ] the baton was (re)written or re-stamped at THIS exit — a baton reused from an earlier
      warp is stale by construction (specimen 2026-07-20: a cold start received the previous
      evening's baton, missing a whole morning session of pins);
- [ ] the FORBIDDEN→ALLOWED lines reflect the NEWEST stall data — retire lines proven false
      (specimen: "test suite guard-stalls, BR-present only" outlived its truth by a day).

## Open questions this seed does not settle

Carried by the research programme (blog 030): whether the checklist earns its top slot (experiment
2), whether the three-line summary beats pure pointers (experiment 3), how the template should vary
by warp type, what a no-baton control reveals about the whole idea (the pilot), and whether the
cold-start meta-minion audit earns its tokens (a later arm: baton+minion vs baton alone).
