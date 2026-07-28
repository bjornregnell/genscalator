# EMBER-TEMPLATE: the pre-hoc seed

*A **warp ember** is what a smith's banked fire is to the morning: at exit the session rakes ash
over the coals; the fresh agent on the other side of the warp blows the ember back into flame.
Formerly called the "baton" — renamed 2026-07-20 (Swedish* batong *= police club; the smithy fit
won: `tt forge`, banking the fire). Raw pre-rename records keep the old word; see
`../research/wr-data/WR181-rename-baton-to-warp-ember-2026-07-20.md`.*

**Status: pre-hoc seed, v0 (2026-07-19); v0.1 maintenance 2026-07-20; v0.2 maintenance
2026-07-21; v0.3 maintenance 2026-07-28** — v0.3: the EXIT-SIDE minion audit becomes a required
step (see the pre-flight checklist), on BR's argument that the ember is written by the session's
most-rotted context and neither party reliably REMEMBERS to order a check at exactly that moment —
so the check must be structural, not remembered. Two specimens preceded the rule (2026-07-28, both
sessions' embers audited by a fresh minion before commit; the first audit caught real mirror lag). — v0.2: mode-state phrasing clarified (meta-minion push-17 nit: modes live in the
harness, not in committed files) and the routine push set grew to include the gitlab mirror
(CD13). v0.1: operational updates from
live guard-stall specimens plus the ColdStart-clearing revert, and the baton→ember rename. Still
PRE-EXPERIMENT: no pilot has run; these refinements move the thing-to-beat, they are not results.
This is the best-*reasoned* ember template from a joint human-agent design pass - written down BEFORE any
measurement, as the thing for the pilot experiment to beat (see blog post
[030](../media/blog/030-what-is-a-good-warp-ember.md)). It is a seed, not a result: the pilot
(ember vs no ember) and the follow-up experiments (per-ingredient) may change any part of it. The
real specimen it generalizes: [EMBER-EXAMPLE.md](EMBER-EXAMPLE.md).

## The rules the template encodes

1. **Pointer, not truth.** Durable truth lives in committed files and notes; the ember points. The
   only prose state allowed is the three-line summary of §4, and each of its lines must say how to
   verify itself. *(This pointer said §5 until minion push-4 F3 caught the off-by-one.)*
2. **Verify-mandate rule.** Every inherited claim the fresh agent will ACT on carries its own
   check-instruction (a hash to compare, a command to run, a file to read). A fresh agent holds all
   inherited facts at uniform strength; the mandates substitute for the lost knowing-which-was-checked.
3. **Numbers over adjectives.** Commit hashes, counts, percentages - checkable against git and disk.
4. **One screen.** Size is a duplication budget, not a token budget: if a section grows, relocate
   content to the substrate and point.
5. **Exit order.** Commit and push ALL work first, then write the ember, then exit. An ember written
   before the final commits points at a moving target.
6. **English**, even if the working chat is not: the reader may be any future agent.
7. **One carrier, one path: write it to `EMBER.md` at the repo root, and nowhere else.** Root, because
   that is where a cold agent actually looks; tracked, because an ember's whole job is to be audited and
   an untracked one yields `CANNOT VERIFY`. **Never leave a second copy.** Learned the hard way on
   2026-07-26: a fresh session was handed a **two-day-old** duplicate from gitignored `tmp/` while the
   real ember sat six minutes old, because the exit dance re-stamped one and not the other — and the
   stale one carried an expired overnight AFK-solo mandate to work unattended. **A stale ember is worse
   than an absent one: absent fails loudly, stale fails silently and persuasively.** ⚠ Root means
   tracked, so this rule is for a **CLOSED** repo; in a public one the ember stays gitignored or absent,
   since it routinely names holds, embargoes, and half-rotated credentials.

## The skeleton (section order is part of the design)

```markdown
⛔ THIS IS A WARP EMBER: a pointer to durable truth, not the truth. Verify before
trusting; read only what you need (bounded reads).

# Ember — <warp type: cold start | compact | model switch | reboot>, written <real timestamp> by <writer>

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
  the clear-condition is still an open question)
- modes live in the HARNESS state (`tt mode`), never in a committed file: budget modes like
  TokSpend persist across the warp there. Verify with `tt mode`; do not hunt modes in
  NOW.md. (Phrasing fixed after meta-minion push-17 caught "rides the state file".)
- compact survivor instead: consider `+Hangover`; distrust what you think you remember —
  the verify-mandates below apply DOUBLE to you.

## 2. Holds — do NOT start these
- <held work item> — WHY it is held; WHO lifts it and on what condition.
- ... (a hold without an owner+condition is a defect)

## 3. Pre-authorized actions (do at cold start, report in-feed)
- <action> — pre-authorized by <human>, verify by <check>.
- IF the budget mode says spending (TokSpend lit): spawn the meta-minion to audit the handover —
  the last session's final commits against this ember's claims. It is the one verifier outside
  both the dying and the newborn context ("carried ≠ armed": ember content must be read to act,
  a spawned watcher fires regardless). If not in spending mode: ask before spawning.
- ...

## 4. State summary — MAX THREE LINES, each with its verify-mandate
- <claim of state> (verify: <command/hash/file>)
- <claim of state> (verify: <command/hash/file>)
- <claim of state> (verify: <command/hash/file>)

## 5. Substrate map (priority order)
<board/roadmap file> · <memory index> · <research/data dir> · <this file's own queue section if any>
```

## Pre-flight checklist (mechanical; a future ember-prep skill can enforce it)

Before exiting, the writer checks:
- [ ] every path in the ember resolves on disk;
- [ ] every commit hash named exists in the repo it is claimed in;
- [ ] every hold names its owner and release condition;
- [ ] every state line and pre-authorized action carries a verify-mandate;
- [ ] all work is committed AND pushed to the FULL routine push set (genscalator: origin
      (github) + gitlab + coursegit every unit, plain fast-forward only — `mirror.sc` stays the
      sole force path; codeberg is a BATCHED mirror, release/sweep only; work repo: origin
      (github) + gitlab + coursegit); a flapping remote is retried next unit, never rewritten around;
- [ ] the file fits on roughly one screen;
- [ ] timestamps are read from a clock, never guessed;
- [ ] the ember was (re)written or re-stamped at THIS exit — an ember reused from an earlier
      warp is stale by construction (specimen 2026-07-20: a cold start received the previous
      evening's ember, missing a whole morning session of pins);
- [ ] the FORBIDDEN→ALLOWED lines reflect the NEWEST stall data — retire lines proven false
      (specimen: "test suite guard-stalls, BR-present only" outlived its truth by a day);
- [ ] **EXIT-SIDE MINION AUDIT (required, v0.3): before committing the ember, spawn a FRESH
      sub-agent to audit it against disk** — every hash in its claimed repo, every path resolving,
      every count re-counted, every verify-mandate actually runnable, and (the check the writer
      cannot do from inside) **no load-bearing claim that resolves to nothing durable** — a
      CANNOT-VERIFY finding means a fact is about to die with the session: commit the missing
      substrate or drop the claim. Fold findings in, then commit. WHY structural rather than
      remembered: the ember is written at the session's HIGHEST fill by its most-rotted context,
      which is precisely when both parties are least likely to remember to order a check. The
      minion is read-only, writes no memory, and gets the bare-allowlist reflexes in its brief
      (a warm minion lacks the caller's skills). This is the exit-side sibling of §3's cold-start
      meta-minion; the ember-prep SKILL that would enforce this whole checklist mechanically is
      still an open build (anticipated above).

## Open questions this seed does not settle

Carried by the research programme (blog 030): whether the checklist earns its top slot (experiment
2), whether the three-line summary beats pure pointers (experiment 3), how the template should vary
by warp type, what a no-ember control reveals about the whole idea (the pilot), and whether the
cold-start meta-minion audit earns its tokens (a later arm: ember+minion vs ember alone).
